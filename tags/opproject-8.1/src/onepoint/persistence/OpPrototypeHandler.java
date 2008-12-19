/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.HashMap;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

public class OpPrototypeHandler implements XNodeHandler {

   public final static String PROTOTYPE = "prototype".intern();

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpPrototype prototype = new OpPrototype();
      Object value = attributes.get("name");
      if ((value != null) && (value instanceof String)) {
         prototype.setName((String) value);
      }
      value = attributes.get("instance-class");
      if ((value != null) && (value instanceof String)) {
         prototype.setClassName((String) value);
      }
      value = attributes.get("super-type");
      if ((value != null) && (value instanceof String)) {
         prototype.setSuperTypeName((String) value);
      }
      value = attributes.get("implements");
      if ((value != null) && (value instanceof String)) {
          String[] values = ((String) value).split(",");
          for (int pos = 0; pos < values.length; pos++) {
             values[pos] = values[pos].trim();
          }
          prototype.setImplementingNames(values);
      }
      value = attributes.get("type");
      if ((value != null) && (value instanceof String)) {
         if ("interface".equalsIgnoreCase((String)value)) {
            prototype.setInterface(true);
         }
         if ("abstract".equalsIgnoreCase((String)value)) {
            prototype.setAbstract(true);
         }
      }
      value = attributes.get("batch-size");
      if ((value != null) && (value instanceof String)) {
         prototype.setBatchSize(Integer.valueOf((String) value));
      }
      return prototype;
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
      if (child instanceof OpMember) {
         ((OpPrototype) node).addDeclaredMember((OpMember) child);
      }
   }

   public void addNodeContent(XContext context, Object node, String content) {      
   }

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
   }

}
