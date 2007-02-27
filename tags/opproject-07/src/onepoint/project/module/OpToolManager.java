/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

public final class OpToolManager {

   private static TreeMap _tool_lists = new TreeMap(); // Ensure correct order/sequence
   private static HashMap _tool_names = new HashMap();
   private static TreeMap _group_lists = new TreeMap(); // Ensure correct order/sequence
   private static HashMap _group_names = new HashMap();
   
   // *** Problem: Order of tools and how to be sure that groups are "first"
   // ==> Possible solution: "Main" module (project) must be first and defines all groups
   // *** OR: Maybe much better -- use registry file to define AND organize groups
   // ==> Even "assign" tools to groups from registry file (problem: One group per module -- PORTFOLIO!)
   // *** Maybe solvable using an extra "navigation" structure "beside" module-registry itself
   // ==> Note: Could be hard-coded *now*, but part of configuration file in the future
   
   // *** DECISION: Use project-module-first defines all groups for *NOW*
   // ==> Use configuration file approach in the future

   public static void registerTool(OpTool tool) {
      // *** Check for uniqueness of tool name?
      ArrayList tool_list = (ArrayList) _tool_lists.get(new Integer(tool.getSequence()));
      if (tool_list == null) {
         tool_list = new ArrayList();
         _tool_lists.put(new Integer(tool.getSequence()), tool_list);
      }
      tool_list.add(tool);
      _tool_names.put(tool.getName(), tool);
   }

   // *** removeTool(String name)

   public static OpTool getTool(String name) {
      return (OpTool) (_tool_names.get(name));
   }

   public static Iterator getToolLists() {
      return _tool_lists.values().iterator();
   }

   public static void registerGroup(OpToolGroup group) {
      // *** Check for uniqueness of tool name?
      ArrayList group_list = (ArrayList) _group_lists.get(new Integer(group.getSequence()));
      if (group_list == null) {
         group_list = new ArrayList();
         _group_lists.put(new Integer(group.getSequence()), group_list);
      }
      group_list.add(group);
      _group_names.put(group.getName(), group);
   }

   // *** removeGroup(String name)

   public static OpToolGroup getGroup(String name) {
      return (OpToolGroup) (_group_names.get(name));
   }

   public static Iterator getGroupLists() {
      return _group_lists.values().iterator();
   }

}
