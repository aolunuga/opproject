/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.HashMap;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

public class OpRelationshipHandler implements XNodeHandler {

   public final static String RELATIONSHIP = "relationship".intern();


   public Object newNode(XContext context, String name, HashMap attributes) {
      OpRelationship relationship = new OpRelationship();
      Object value = attributes.get("name");
      if ((value != null) && (value instanceof String)) {
         relationship.setName((String) value);
      }
      value = attributes.get("type");
      if ((value != null) && (value instanceof String)) {
         relationship.setTypeName((String) value);
      }
      value = attributes.get("collection-type");
      if ((value != null) && (value instanceof String)) {
         relationship.setCollectionTypeName((String) value);
      }
      value = attributes.get("relationship-type");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         if (s.equals("association")) {
            relationship.setRelationshipType(OpRelationship.ASSOCIATION);
         }
         else if (s.equals("aggregation")) {
            relationship.setRelationshipType(OpRelationship.AGGREGATION);
         }
         else if (s.equals("composition")) {
            relationship.setRelationshipType(OpRelationship.COMPOSITION);
         }
      }
      value = attributes.get("back-relationship");
      if ((value != null) && (value instanceof String)) {
         relationship.setBackRelationshipName((String) value);
      }
      value = attributes.get("inverse");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         if (s.equals("true")) {
            relationship.setInverse(true);
         }
         else if (s.equals("false")) {
            relationship.setInverse(false);
         }
      }
      value = attributes.get("recursive");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         if (s.equals("true")) {
            relationship.setRecursive(true);
         }
         else if (s.equals("false")) {
            relationship.setRecursive(false);
         }
      }
     value = attributes.get("cascade");
      if ((value != null) && (value instanceof String)) {
         String s = (String) value;
         if (s.equals(OpRelationship.CASCADE_DELETE) || s.equals(OpRelationship.CASCADE_SAVEUPDATE) || s.equals(OpRelationship.CASCADE_ALL)) {
            relationship.setCascadeMode(s);
         }
      }
      value = attributes.get("fetch");
      if ((value != null) && (value instanceof String)) {
         relationship.setFetch((String) value);
      }
      value = attributes.get("lazy");
      if ((value != null) && (value instanceof String)) {
         relationship.setLazy((String) value);
      }
      value = attributes.get("order-by");
      if ((value != null) && (value instanceof String)) {
         relationship.setOrderBy((String) value);
      }
      value = attributes.get("sort");
      if ((value != null) && (value instanceof String)) {
         relationship.setSort((String) value);
      }
      value = attributes.get("transient");
      if ((value != null) && (value instanceof String)) {
         relationship.setTransient(((String) value).equals("true"));
      }

      return relationship;
   }

   public void addNodeContent(XContext context, Object node, String content) {}

   public void addChildNode(XContext context, Object node, String child_name, Object child) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}
