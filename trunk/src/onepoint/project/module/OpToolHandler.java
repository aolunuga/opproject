/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;
import onepoint.project.util.OpProjectConstants;

import java.util.HashMap;
import java.util.Map;

public class OpToolHandler implements XNodeHandler {

   final static String TOOL = "tool";

   private final static String NAME = "name";
   private final static String CAPTION = "caption";
   private final static String ICON = "icon";
   private final static String START_FORM = "start-form";
   private final static String GROUP_REF = "group-ref";
   private final static String SEQUENCE = "sequence";
   private final static String MULTI_USER = "multi-user-only";
   private final static String SELECTED = "selected";
   private final static String LEVEL = "level";

   /**
    * The mapping between the user level names and the byte code of each level.
    * The structure of the map is: Key - user level name
    * Value - the level's byte code.
    */
   private static Map<String, Byte> LEVEL_NAME_TYPE_MAP;

   static {
      LEVEL_NAME_TYPE_MAP = new HashMap<String, Byte>();
      LEVEL_NAME_TYPE_MAP.put("Customer", OpProjectConstants.OBSERVER_CUSTOMER_USER_LEVEL);
      LEVEL_NAME_TYPE_MAP.put("Observer", OpProjectConstants.OBSERVER_USER_LEVEL);
      LEVEL_NAME_TYPE_MAP.put("Contributor", OpProjectConstants.CONTRIBUTOR_USER_LEVEL);
      LEVEL_NAME_TYPE_MAP.put("Manager", OpProjectConstants.MANAGER_USER_LEVEL);
   }

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpTool tool = new OpTool();
      Object value = attributes.get(NAME);
      if ((value != null) && (value instanceof String)) {
         tool.setName((String) value);
      }
      value = attributes.get(CAPTION);
      if ((value != null) && (value instanceof String)) {
         tool.setCaption((String) value);
      }
      value = attributes.get(ICON);
      if ((value != null) && (value instanceof String)) {
         tool.setIcon((String) value);
      }
      value = attributes.get(START_FORM);
      if ((value != null) && (value instanceof String)) {
         tool.setStartForm((String) value);
      }
      value = attributes.get(GROUP_REF);
      if ((value != null) && (value instanceof String)) {
         tool.setGroupRef((String) value);
      }
      value = attributes.get(SEQUENCE);
      if ((value != null) && (value instanceof String)) {
         tool.setSequence(Integer.parseInt((String) value));
      }
      value = attributes.get(MULTI_USER);
      if ((value != null) && (value instanceof String)) {
         tool.setMultiUserOnly(Boolean.valueOf((String) value));
      }
      value = attributes.get(SELECTED);
      if ((value != null) && (value instanceof String)) {
         tool.setSelected(Boolean.valueOf((String) value).booleanValue());
      }
      value = attributes.get(LEVEL);
      if((value != null) && (value instanceof String)) {
         if(LEVEL_NAME_TYPE_MAP.containsKey((String) value)){
            tool.setLevel(LEVEL_NAME_TYPE_MAP.get((String) value));
         }
      }
      return tool;
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   public void addNodeContent(XContext context, Object node, String content) {
   }

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
   }

}
