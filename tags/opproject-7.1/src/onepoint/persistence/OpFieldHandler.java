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
         field.setAutoIncrement(Boolean.parseBoolean((String) value));
      }
      value = attributes.get("unique");
      if ((value != null) && (value instanceof String)) {
         field.setUnique(Boolean.parseBoolean((String) value));
      }
      value = attributes.get("unique-key");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         field.setUniqueKey(s);
      }
      value = attributes.get("mandatory");
      if ((value != null) && (value instanceof String)) {
         field.setMandatory(Boolean.parseBoolean((String) value));
      }
      value = attributes.get("ordered");
      if ((value != null) && (value instanceof String)) {
         field.setOrdered(Boolean.parseBoolean((String) value));
      }
      value = attributes.get("indexed");
      if ((value != null) && (value instanceof String)) {
         field.setIndexed(Boolean.parseBoolean((String) value));
      }
      value = attributes.get("column");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         field.setColumn(s);
      }
      value = attributes.get("default");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         field.setDefaultValue(s);
      }
      value = attributes.get("update");
      if ((value != null) && (value instanceof String)) {
         field.setUpdate(Boolean.valueOf((String) value));
      }
      value = attributes.get("insert");
      if ((value != null) && (value instanceof String)) {
         field.setInsert(Boolean.valueOf((String) value));
      }

      return field;
   }

   public void addNodeContent(XContext context, Object node, String content) {
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
   }

}
