/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpTool;
import onepoint.project.module.OpToolGroup;
import onepoint.project.module.OpToolManager;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.*;

/**
 * Form provider class for the application's navigation dock.
 */
public class OpDockFormProvider implements XFormProvider {

   /**
    * The id of the navigation box
    */
   private final static String NAVIGATION_BOX = "NavigationBox";

   /**
    * Suffix used for i18n the name of tool groups
    */
   private static final String GROUP_MAP_ID_SUFFIX = ".module";

   private static final String PROJECT_COSTS_TOOL_NAME = "project_costs";
   private static final String RESOURCES_TOOL_GROUP_NAME = "resources";
   private static final String REPORTS_TOOL_GROUP_NAME = "reports";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      XComponent navigationBox = form.findComponent(NAVIGATION_BOX);

      //add the tool groups
      Map navigationGroupMap = addToolGroups(session, navigationBox);

      //add the tools to each tool group.
      addTools(session, navigationGroupMap);
   }

   /**
    * Links the registered tools with the created navigation groups, via navigation item components.
    * @param session a <code>OpProjectSession</code> representing the server session.
    * @param navigationGroupMap a <code>Map</code> of [String, XComponent(NAVIGATION_GROUP)] pairs.
    */
   private void addTools(OpProjectSession session, Map navigationGroupMap) {
      List<List<OpTool>> toolLists = removeToolsWithHigherLevels(session);

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

   /**
    * Checks the setting that hides manager features and sets(if the setting is set to true)/removes(if the setting
    *    is set to false) the manager level on the tools that need to be hidden/shown from non manager users.
    */
   private void modifyToolManagerLevel() {
      Boolean hideManagerFeatures = Boolean.valueOf(OpSettingsService.getService().get(OpSettings.HIDE_MANAGER_FEATURES));

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
               else{
                  tool.setLevel(null);
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
   private List<List<OpTool>> removeToolsWithHigherLevels(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpUser user = session.user(broker);
      Byte userLevel = user.getLevel();

      //add multi user manager level rights
      if (OpEnvironmentManager.isMultiUser()) {
         modifyToolManagerLevel();
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
               if(group != null && tool.getGroup().getLevel() != null && tool.getGroup().getLevel() > userLevel){
                  toolListIt.remove();
               }
            }
         }
      }

      return toolLists;
   }
   //<FIXME>

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
      navigationItem.setStringValue(tool.getStartForm());
      navigationItem.setIcon(tool.getIcon());
      navigationItem.setOnButtonPressed(NAVIGATION_BOX + "_onButtonPressed");
      navigationItem.setSelected(tool.isSelected());

      return navigationItem;
   }

   /**
    * Adds the application registered tool groups, creates UI components for each and adds them to the navigation box.
    * @param session a <code>OpProjectSession</code> representing an application session.
    * @param navigationBox a <code>XComponent(NAVIGATION_BOX)</code> representing the navigation box.
    * @return a <code>Map</code> of [String, XComponent(NAVIGATION_GROUP)] pairs.
    */
   private Map addToolGroups(OpProjectSession session, XComponent navigationBox) {
      Map navigationGroupMap = new HashMap();
      List<List<OpToolGroup>> groupLists = removeToolGroupsWithHigherLevels(session);

       for (List<OpToolGroup> groupList : groupLists) {
         for (OpToolGroup group : groupList) {
            if (group.isAdministratorOnly() && !session.userIsAdministrator()) {
               continue;
            }
            XComponent navigationGroup = createNavigationGroup(group, session);
            navigationBox.addChild(navigationGroup);
            navigationGroupMap.put(group.getName(), navigationGroup);
         }
      }
      return navigationGroupMap;
   }

   /**
    * Checks the setting that hides manager features and sets(if the setting is set to true)/removes(if the setting
    *    is set to false) the manager level on the tool groups that need to be hidden/shown from non manager users.
    */
   private void modifyToolGroupManagerLevel() {
      Boolean hideManagerFeatures = Boolean.valueOf(OpSettingsService.getService().get(OpSettings.HIDE_MANAGER_FEATURES));

     Iterator<List<OpToolGroup>> groupListsIterator = OpToolManager.getGroupLists();
      while (groupListsIterator.hasNext()) {
         List<OpToolGroup> groupList = groupListsIterator.next();
         Iterator<OpToolGroup> groupListIt = groupList.iterator();
         while (groupListIt.hasNext()) {
            OpToolGroup group = groupListIt.next();
            if (group.getName().equals(RESOURCES_TOOL_GROUP_NAME) || group.getName().equals(REPORTS_TOOL_GROUP_NAME)) {
               if(hideManagerFeatures){
                  group.setLevel(OpUser.MANAGER_USER_LEVEL);
               }
               else{
                  group.setLevel(null);
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
   private List<List<OpToolGroup>> removeToolGroupsWithHigherLevels(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpUser user = session.user(broker);
      Byte userLevel = user.getLevel();

      //add multi user manager level rights
      if (OpEnvironmentManager.isMultiUser()) {
         modifyToolGroupManagerLevel();
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
            if (group.getLevel() != null && group.getLevel() > userLevel) {
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
