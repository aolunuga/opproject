/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpTool;
import onepoint.project.module.OpToolGroup;
import onepoint.project.module.OpToolManager;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class OpDockFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getLogger(OpDockFormProvider.class,true);

   public final static String TOOL_DOCK = "ToolDock";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      // Populate dock tool bar with registered tools
      // System.err.println("OpDockFormProvider.updateForm(): " + form);
      XComponent tool_dock = form.findComponent(TOOL_DOCK);
      // System.err.println(" tool-bar: " + tool_bar);
      // *** Get tools from tool-registry (iterator?)
      
      // Add tool-groups to dock
      Iterator group_lists = OpToolManager.getGroupLists();
      ArrayList group_list = null;
      OpToolGroup group = null;
      int i = 0;
      XComponent dock_group = null;
      HashMap dock_group_map = new HashMap();
      while (group_lists.hasNext()) {
         group_list = (ArrayList) group_lists.next();
         for (i = 0; i < group_list.size(); i++) {
            group = (OpToolGroup) group_list.get(i);
            System.err.println("***ADDING GROUP " + group.getName());
            // Create new dock group
            dock_group = new XExtendedComponent(XExtendedComponent.NAVIGATION_GROUP);
            dock_group.setID(group.getName());
            dock_group.setStringValue(group.getName());
            dock_group_map.put(group.getName(), dock_group);
            String resource_map_id = group.getModule().getName() + ".module";
            XLanguageResourceMap resource_map = XLocaleManager.findResourceMap(session.getLocale().getID(),
                  resource_map_id);
            dock_group.setText(group.getCaption());
            if (resource_map != null) {
               XLocalizer localizer = new XLocalizer();
               localizer.setResourceMap(resource_map);
               dock_group.setText(localizer.localize(group.getCaption()));
            }
            // dock_group.setOnButtonPressed(TOOL_DOCK + "_onGroupChanged");
            tool_dock.addChild(dock_group);
         }
      }

      // Add tools to dock
      Iterator tool_lists = OpToolManager.getToolLists();
      ArrayList tool_list = null;
      OpTool tool = null;
      XComponent dock_tool = null;
      while (tool_lists.hasNext()) {
         tool_list = (ArrayList) tool_lists.next();
         for (i = 0; i < tool_list.size(); i++) {
            tool = (OpTool) tool_list.get(i);
            System.err.println("***ADDING TOOL " + tool.getName());
            // Create new dock tool
            dock_tool = new XExtendedComponent(XExtendedComponent.NAVIGATION_ITEM);
            dock_tool.setID(tool.getName());
            String resource_map_id = tool.getModule().getName() + ".module";
            XLanguageResourceMap resource_map = XLocaleManager.findResourceMap(session.getLocale().getID(),
                  resource_map_id);
            dock_tool.setText(tool.getCaption());
            if (resource_map != null) {
               XLocalizer localizer = new XLocalizer();
               localizer.setResourceMap(resource_map);
               dock_tool.setText(localizer.localize(tool.getCaption()));
            }
            dock_tool.setStringValue(tool.getStartForm());
            dock_tool.setIcon(tool.getIcon());
            dock_tool.setOnButtonPressed(TOOL_DOCK + "_onButtonPressed");
            
            // FIXME: This should not be hard-coded
            if (tool.getName().equals("projects"))
               dock_tool.setSelected(true);
            
            if (tool.getGroupRef() != null) {
               dock_group = (XComponent) dock_group_map.get(tool.getGroupRef());
               System.err.println("   --> resolving " + tool.getGroupRef());
               dock_group.addChild(dock_tool);
            }
         }
      }

   }

}
