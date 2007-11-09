/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;

/**
 * @author dfreis
 *
 */
public class OpCustomType extends OpObject {

   private String prototypeName;
   private Set<OpCustomAttribute> customAttributes;

   /**
    * Default constructor
    */
   public OpCustomType() {
   }

   /**
    * Default constructor
    */
   public OpCustomType(Class prototype) {
      prototypeName = prototype.getName();
   }

   public String getPrototypeName() {
      return prototypeName;
   }

   public void getPrototypeName(String name) {
      prototypeName = name;
   }

   public Class getPrototypeClass() {
      try {
         return Class.forName(prototypeName);
      }
      catch (ClassNotFoundException exc) {
         return null;
      }
   }

   void addCustomAttribute(OpCustomAttribute customAttribute) {
      if (customAttribute == null) {
         throw new IllegalArgumentException("customAttribut must not be <null>");
      }
      if (customAttributes == null) {
         customAttributes = new HashSet<OpCustomAttribute>();
      }
      customAttributes.add(customAttribute);
   }
   
   void removeCustomAttribute(OpCustomAttribute customAttribute) {
      if (customAttribute == null) {
         throw new IllegalArgumentException("customAttribut must not be <null>");
      }
      if (customAttributes == null) {
         return;
      }
      customAttributes.remove(customAttribute);
   }

   public Set<OpCustomAttribute> getCustomAttributes() {
      return customAttributes;
      //      return Collections.unmodifiableSet(customAttributes);
   }

   public void setCustomAttributes(Set<OpCustomAttribute> value) {
      this.customAttributes = value;
   }

   /**
    * @param string
    * @return
    * @pre
    * @post
    */
   public boolean containsAttribute(String string) {
      if (customAttributes == null) {
         return false;
      }
      return customAttributes.contains(string);
   }

   public void setPrototypeName(String prototypeName) {
      this.prototypeName = prototypeName;
   }
   

}
