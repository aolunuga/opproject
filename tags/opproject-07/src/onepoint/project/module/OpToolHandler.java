/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

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
      return tool;
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   public void addNodeContent(XContext context, Object node, String content) {
   }

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
   }

}
