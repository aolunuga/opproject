/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

public class OpToolGroupHandler implements XNodeHandler {

   public final static String TOOL_GROUP = "tool-group";
   public final static String NAME = "name";
   public final static String CAPTION = "caption";
   public final static String SEQUENCE = "sequence";
   public final static String ADMINISTRATOR_ONLY = "administrator-only";
   public final static String CATEGORY = "category";
   public final static String LEVEL = "level";

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpToolGroup group = new OpToolGroup();
      Object value = attributes.get(NAME);
      if ((value != null) && (value instanceof String)) {
         group.setName((String) value);
      }
      value = attributes.get(CAPTION);
      if ((value != null) && (value instanceof String)) {
         group.setCaption((String) value);
      }
      value = attributes.get(SEQUENCE);
      if ((value != null) && (value instanceof String)) {
         group.setSequence(Integer.parseInt((String) value));
      }
      value = attributes.get(ADMINISTRATOR_ONLY);
      if ((value != null) && (value instanceof String)) {
         group.setAdministratorOnly(Boolean.valueOf((String) value).booleanValue());
      }
      value = attributes.get(CATEGORY);
      if ((value != null) && (value instanceof String)) {
         group.setCategory((String) value);
      }
      value = attributes.get(LEVEL);
      if((value != null) && (value instanceof String)) {
         if(OpToolHandler.getLevelType((String) value) != null){
            group.setLevel(OpToolHandler.getLevelType((String) value));
         }
      }
      return group;
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {}

   public void addNodeContent(XContext context, Object node, String content) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}
