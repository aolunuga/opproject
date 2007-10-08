/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

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
         /*
          * try { prototype.setInstanceClass(Class.forName((String)value)); } catch (Exception e) {
          * System.err.println(e); }
          */
      }
      value = attributes.get("super-type");
      if ((value != null) && (value instanceof String)) {
         if ((value != null) && (value instanceof String)) {
            prototype.setSuperTypeName((String) value);
         }
      }
      return prototype;
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
      if (child instanceof OpMember) {
         ((OpPrototype) node).addDeclaredMember((OpMember) child);
      }
   }

   public void addNodeContent(XContext context, Object node, String content) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}