/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

public class OpFieldHandler implements XNodeHandler {

   public final static String FIELD = "field".intern();

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpField field = new OpField();
      Object value = attributes.get("name");
      if ((value != null) && (value instanceof String)) {
         field.setName((String) value);
      }
      value = attributes.get("type");
      if ((value != null) && (value instanceof String)) {
         field.setTypeName((String) value);
      }
      value = attributes.get("collection-type");
      if ((value != null) && (value instanceof String)) {
         field.setCollectionTypeName((String) value);
      }
      value = attributes.get("auto-increment");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         if (s.equals("true"))
            field.setAutoIncrement(true);
         else if (s.equals("false"))
            field.setAutoIncrement(false);
      }
      value = attributes.get("unique");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         if (s.equals("true"))
            field.setUnique(true);
         else if (s.equals("false"))
            field.setUnique(false);
      }
      value = attributes.get("mandatory");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         if (s.equals("true"))
            field.setMandatory(true);
         else if (s.equals("false"))
            field.setMandatory(false);
      }
      value = attributes.get("ordered");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         if (s.equals("true"))
            field.setOrdered(true);
         else if (s.equals("false"))
            field.setOrdered(false);
      }
      value = attributes.get("indexed");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         if (s.equals("true"))
            field.setIndexed(true);
         else if (s.equals("false"))
            field.setIndexed(false);
      }
      return field;
   }

   public void addNodeContent(XContext context, Object node, String content) {}

   public void addChildNode(XContext context, Object node, String child_name, Object child) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}
