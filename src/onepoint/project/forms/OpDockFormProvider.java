/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpTool;
import onepoint.project.module.OpToolGroup;
import onepoint.project.module.OpToolHandler;
import onepoint.project.module.OpToolManager;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

/**
 * Form provider class for the application's navigation dock.
 */
public class OpDockFormProvider implements XFormProvider {

   
   public final static String TOOL_PARAMETER = "tool";

   /**
    * The id of the navigation box
    */
   private final static String NAVIGATION_BOX = "NavigationBox";

   /**
    * Suffix used for i18n the name of tool groups
    */
   private static final String GROUP_MAP_ID_SUFFIX = ".module";

   private static final String MY_TASKS_GROUP_NAME = "my_tasks";
   private static final String PROJECT_COSTS_TOOL_NAME = "project_costs";
   private static final String RESOURCES_TOOL_GROUP_NAME = "resources";
   private static final String REPORTS_TOOL_GROUP_NAME = "reports";
   private static final String PROJECT_TOOL_GROUP_NAME = "projects";
   private static final String CATEGORY_NAME = "category";
   static final String DEFAULT_CATEGORY = "default";

   private final static String FORM_ID = "NavigationBox";


   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      XComponent navigationBox = form.findComponent(NAVIGATION_BOX);
      String category = (String) parameters.get(OpProjectConstants.CATEGORY_NAME);
      
      XComponent categoryComp = form.findComponent(CATEGORY_NAME);
      if (category != null) {
         session.setVariable(OpProjectConstants.CATEGORY_NAME, category);
      }
      else {
         category = (String) session.getVariable(OpProjectConstants.CATEGORY_NAME);
         if (category == null) {
            
            category = DEFAULT_CATEGORY;
            session.setVariable(OpProjectConstants.CATEGORY_NAME, category);            
         }
      }
      categoryComp.setStringValue(category);
      
      //add the tool groups
      Map<String, XComponent> navigationGroupMap = addToolGroups(session, navigationBox, category);

      //add the tools to each tool group.
      addTools(session, navigationGroupMap);
      
      // *** GM: Select active tool if "tool" parameter is set
      String activeToolName = (String) parameters.get(TOOL_PARAMETER);
      if (activeToolName != null) {
         XComponent activeTool = form.findComponent(activeToolName);
         if (activeTool != null) {
            ((XComponent) activeTool.getParent()).setSelectedIndex(activeTool.getIndex());
            activeTool.setSelected(true);
         }
      }

   }

   /**
    * Links the registered tools with the created navigation groups, via navigation item components.
    * @param session a <code>OpProjectSession</code> representing the server session.
    * @param navigationGroupMap a <code>Map</code> of [String, XComponent(NAVIGATION_GROUP)] pairs.
    */
   private void addTools(OpProjectSession session, Map<String, XComponent> navigationGroupMap) {
      OpBroker broker = session.newBroker();
      try {
         List<List<OpTool>> toolLists = prepareToolSet(session, broker);
         for (List<OpTool> toolList : toolLists) {
            for (OpTool tool : toolList) {
               //link the tools
               if (tool.getGroupRef() != null) {
                  XComponent navigationGroup = (XComponent) navigationGroupMap.get(tool.getGroupRef());
                  //navigation group might be null 
                  if (navigationGroup != null) {
                     XComponent navigationItem = createNavigationItem(tool, session);
                     navigationGroup.addChild(navigationItem);
                     if (navigationItem.getSelected()) {
                        navigationGroup.setSelectedIndex(new Integer(navigationItem.getIndex()));
                     }
                  }
               }
            }
         }
      }
      finally {
         broker.close();
      }
   }

   /**
    * Checks the setting that hides manager features and sets(if the setting is set to true)/removes(if the setting
    *    is set to false) the manager level on the tools that need to be hidden/shown from non manager users.
    */
   protected void modifyToolManagerLevel(OpProjectSession session) {
      Boolean hideManagerFeatures = Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.HIDE_MANAGER_FEATURES));

      Iterator<List<OpTool>> toolListsIterator = OpToolManager.getToolLists();
      while (toolListsIterator.hasNext()) {
         List<OpTool> toolList = toolListsIterator.next();
         Iterator<OpTool> toolListIt = toolList.iterator();
         while (toolListIt.hasNext()) {
            OpTool tool = toolListIt.next();
            if (tool.getName().equals(PROJECT_COSTS_TOOL_NAME)) {
               if(hideManagerFeatures) {
                  tool.setLevel(OpUser.MANAGER_USER_LEVEL);
               }
            }
         }
      }
   }

   //<FIXME author="Haizea Florin" description="Maybe the removal of the tools for which the user doesn't have
   //    the appropriate level could be done sooner...">
   /**
    * Remove the tools for which the user does not have the appropriate level.
    *
    * @param session - the <code>OpProjectSession</code> needed to get the user's level
    * @return the list of <code>OpTool</code> objects for which the user has the appropriate level.
    */
   protected List<List<OpTool>> prepareToolSet(OpProjectSession session, OpBroker broker) {
      OpUser user = session.user(broker);
      Byte userLevel = user.getLevel();

      //add multi user manager level rights
      if (OpEnvironmentManager.isMultiUser()) {
         modifyToolManagerLevel(session);
      }

      Iterator<List<OpTool>> toolListsIterator = OpToolManager.getToolLists();
      List<List<OpTool>> toolLists = new ArrayList<List<OpTool>>();
      while (toolListsIterator.hasNext()) {
         List<OpTool> toolList = toolListsIterator.next();
         //make a copy of the tool list so that the users that will log in after the current user
         //have the full list of tools at the log in moment
         List<OpTool> tempList = new ArrayList<OpTool>();
         for(OpTool tool : toolList){
            tempList.add(tool);
         }
         toolLists.add(tempList);
         Iterator<OpTool> toolListIt = tempList.iterator();
         while (toolListIt.hasNext()) {
            OpTool tool = toolListIt.next();
            if (tool.getLevel() != null && tool.getLevel() > userLevel) {
               toolListIt.remove();
            }
            else{
               OpToolGroup group = tool.getGroup();
               if(isToHide(userLevel, group)) {
                  toolListIt.remove();
               }
            }
         }
      }

      return toolLists;
   }
   //<FIXME>

   public static boolean isToHide(Byte userLevel, OpToolGroup group) {
	   return group != null && ((group.getMinLevel() != null && group.getMinLevel() > userLevel) || (group.getHiddenLevels() != null && group.getHiddenLevels().contains(userLevel)));
   }

   /**
    * Creates a navigation item component, from the given tool.
    * @param tool a <code>OpTool</code> instance.
    * @param session a <code>OpProjectSession</code> representing the project session.
    * @return a <code>XComponent(NAVIGATION_ITEM)</code> created from the given tool.
    */
   private XComponent createNavigationItem(OpTool tool, OpProjectSession session) {
      XComponent navigationItem = new XExtendedComponent(XExtendedComponent.NAVIGATION_ITEM);
      navigationItem.setID(tool.getName());
      String resourceMapId = tool.getModule().getName() + GROUP_MAP_ID_SUFFIX;
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), resourceMapId);
      navigationItem.setText(tool.getCaption());
      if (resourceMap != null) {
         XLocalizer localizer = new XLocalizer();
         localizer.setResourceMap(resourceMap);
         navigationItem.setText(localizer.localize(tool.getCaption()));
      }

      // Following code implements pre-configured navigation from the module file
      // (For instance, used in the custom type module)
      // properties ar added as "?key=value&key2=value2..."
      Map<String, String> startParams = tool.getStartParams();
      StringBuffer params = new StringBuffer();
      if (startParams != null && !startParams.isEmpty()) {
         boolean first = true;
         for (Map.Entry<String, String> entry : startParams.entrySet()) {
            if (first) {
               first = false;
               params.append(OpToolHandler.PARAM_DELIM);
            }
            else {
               params.append(OpToolHandler.ARRAY_DELIM);               
            }
            params.append(entry.getKey());
            params.append(OpToolHandler.KEY_VALUE_DELIM);
            params.append(entry.getValue());
         }
      }

      navigationItem.setStringValue(tool.getStartForm()+params.toString());
      navigationItem.setIcon(tool.getIcon());
      navigationItem.setOnButtonPressed(NAVIGATION_BOX + "_onButtonPressed");
      // *** GM: Check this with Didi; probably legacy code
      // ==> Maybe remove SELECTED property of tool as a whole
      // navigationItem.setSelected(tool.isSelected());
      
      return navigationItem;
   }

   /**
    * Adds the application registered tool groups, creates UI components for each and adds them to the navigation box.
    * @param session a <code>OpProjectSession</code> representing an application session.
    * @param navigationBox a <code>XComponent(NAVIGATION_BOX)</code> representing the navigation box.
    * @param category 
    * @return a <code>Map</code> of [String, XComponent(NAVIGATION_GROUP)] pairs.
    */
   private Map<String, XComponent> addToolGroups(OpProjectSession session, XComponent navigationBox, String category) {
      Map navigationGroupMap = new HashMap();
      OpBroker broker = session.newBroker();
      try {
         List<List<OpToolGroup>> groupLists = removeToolGroupsWithHigherLevels(session, broker);

         for (List<OpToolGroup> groupList : groupLists) {
            for (OpToolGroup group : groupList) {
               if (category == null || !category.equals(group.getCategory())) {
                  continue;
               }
               if (group.isAdministratorOnly() && !session.userIsAdministrator()) {
                  continue;
               }
               XComponent navigationGroup = createNavigationGroup(group, session);
               navigationBox.addChild(navigationGroup);
               navigationGroupMap.put(group.getName(), navigationGroup);
            }
         }
      }
      finally {
         broker.close();
      }
      return navigationGroupMap;
   }

   /**
    * Checks the setting that hides manager features and sets(if the setting is set to true)/removes(if the setting
    *    is set to false) the manager level on the tool groups that need to be hidden/shown from non manager users.
    */
   private void modifyToolGroupManagerLevel(OpProjectSession session) {
      Boolean hideManagerFeatures = Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.HIDE_MANAGER_FEATURES));
      Boolean showOnlyMyTasksForContributorUsers = Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.SHOW_ONLY_MYWORK_FOR_CONTRIBUTOR_USERS));

     Iterator<List<OpToolGroup>> groupListsIterator = OpToolManager.getGroupLists();
      while (groupListsIterator.hasNext()) {
         List<OpToolGroup> groupList = groupListsIterator.next();
         Iterator<OpToolGroup> groupListIt = groupList.iterator();
         while (groupListIt.hasNext()) {
            OpToolGroup group = groupListIt.next();
            String name = group.getName();
			if (name.equals(PROJECT_TOOL_GROUP_NAME) || name.equals(RESOURCES_TOOL_GROUP_NAME)) {
				if (showOnlyMyTasksForContributorUsers) {
					group.addHiddenLevel(OpUser.CONTRIBUTOR_USER_LEVEL);
				}
				else {
					group.removeHiddenLevel(OpUser.CONTRIBUTOR_USER_LEVEL);
				}
            }
            if (name.equals(RESOURCES_TOOL_GROUP_NAME) || name.equals(REPORTS_TOOL_GROUP_NAME)) {
               if(hideManagerFeatures){
                  group.setMinLevel(OpUser.MANAGER_USER_LEVEL);
               }
            }
         }
      }
   }

   //<FIXME author="Haizea Florin" description="Maybe the removal of the toolGroups for which the user doesn't have
   //    the appropriate level could be done sooner...">
   /**
    * Remove the tool groups for which the user does not have the appropriate level.
    *
    * @param session - the <code>OpProjectSession</code> needed to get the user's level
    * @return the list of <code>OpToolGroup</code> objects for which the user has the appropriate level.
    */
   private List<List<OpToolGroup>> removeToolGroupsWithHigherLevels(OpProjectSession session, OpBroker broker) {
      OpUser user = session.user(broker);
      Byte userLevel = user.getLevel();

      //add multi user manager level rights
      if (OpEnvironmentManager.isMultiUser()) {
         modifyToolGroupManagerLevel(session);
      }

      Iterator<List<OpToolGroup>> groupListsIterator = OpToolManager.getGroupLists();
      List<List<OpToolGroup>> groupLists = new ArrayList<List<OpToolGroup>>();
      while (groupListsIterator.hasNext()) {
         List<OpToolGroup> groupList = groupListsIterator.next();
         //make a copy of the tool group list so that the users that will log in after the current user
         //have the full list of tool groups at the log in moment
         List<OpToolGroup> tempGroupList = new ArrayList<OpToolGroup>();
         for(OpToolGroup group : groupList){
            tempGroupList.add(group);
         }
         groupLists.add(tempGroupList);
         Iterator<OpToolGroup> groupListIt = tempGroupList.iterator();
         while (groupListIt.hasNext()) {
            OpToolGroup group = groupListIt.next();
            if (isToHide(userLevel, group)) {
               groupListIt.remove();
            }
         }
      }

      return groupLists;
   }
   //<FIXME>

   /**
    * Creates a new navigation group component, containing the data from the given tool group.
    * @param group a <code>OpToolGroup</code> instance.
    * @param session a <code>OpProjectSession</code> representing the project session.
    * @return a <code>XComponent(NAVIGATION_GROUP)</code> component
    */
   private XComponent createNavigationGroup(OpToolGroup group, OpProjectSession session) {
      XComponent dockGroup = new XExtendedComponent(XExtendedComponent.NAVIGATION_GROUP);
      dockGroup.setID(group.getName());
      dockGroup.setStringValue(group.getName());
      String resourceMapId = group.getModule().getName() + GROUP_MAP_ID_SUFFIX;
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(),
            resourceMapId);
      dockGroup.setText(group.getCaption());
      if (resourceMap != null) {
         XLocalizer localizer = new XLocalizer();
         localizer.setResourceMap(resourceMap);
         dockGroup.setText(localizer.localize(group.getCaption()));
      }
      return dockGroup;
   }

}
