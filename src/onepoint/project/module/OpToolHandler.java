/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

public class OpToolHandler implements XNodeHandler {

   public final static String TOOL = "tool";
   public final static String NAME = "name";
   public final static String CAPTION = "caption";
   public final static String ICON = "icon";
   public final static String START_FORM = "start-form";
   public final static String GROUP_REF = "group-ref";
   public final static String SEQUENCE = "sequence";
   public final static String MULTI_USER = "multi-user-only";

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
      return tool;
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {}

   public void addNodeContent(XContext context, Object node, String content) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}
