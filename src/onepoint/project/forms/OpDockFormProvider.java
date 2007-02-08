/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpTool;
import onepoint.project.module.OpToolGroup;
import onepoint.project.module.OpToolManager;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
      Iterator toolLists = OpToolManager.getToolLists();
      while (toolLists.hasNext()) {
         List toolList = (List) toolLists.next();
         for (int i = 0; i < toolList.size(); i++) {
            OpTool tool = (OpTool) toolList.get(i);
            //link the tools
            if (tool.getGroupRef() != null) {
               XComponent navigationGroup = (XComponent) navigationGroupMap.get(tool.getGroupRef());
               //navigation group might be null 
               if (navigationGroup != null) {
                  XComponent navigationItem = createNavigationItem(tool, session);
                  navigationGroup.addChild(navigationItem);
               }
            }
         }
      }
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
      Iterator groupLists = OpToolManager.getGroupLists();
      Map navigationGroupMap = new HashMap();

      while (groupLists.hasNext()) {
         List groupList = (List) groupLists.next();
         for (int i = 0; i < groupList.size(); i++) {
            OpToolGroup group = (OpToolGroup) groupList.get(i);
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
