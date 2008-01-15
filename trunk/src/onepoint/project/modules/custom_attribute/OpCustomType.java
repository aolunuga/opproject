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
   private String description;
   private String label;
   private Set<OpCustomAttribute> customAttributes;
   private Byte subType;
   private String customTypeName;
   private boolean deleted = false;

   /**
    * Default constructor
    */
   public OpCustomType() {
   }

   /**
    * Default constructor
    */
   public OpCustomType(Class prototype) {
      this(prototype, null, null);
   }

   /**
    * Default constructor
    */
   public OpCustomType(Class prototype, Byte subType) {
      this(prototype, subType, null);
   }

   public OpCustomType(Class prototype, String customTypeName) {
      this(prototype, null, customTypeName);
   }

   public OpCustomType(Class prototype, Byte subType, String customTypeName) {
      this.prototypeName = prototype.getName();
      this.subType = subType;
      this.customTypeName = customTypeName;
   }

   public String getPrototypeName() {
      return prototypeName;
   }

   public String getCustomTypeName() {
      return customTypeName;
   }

   //   public void getPrototypeName(String name) {
//      prototypeName = name;
//   }
//
   public Class getPrototypeClass() {
      try {
         return Class.forName(prototypeName);
      }
      catch (ClassNotFoundException exc) {
         return null;
      }
   }

   public void addCustomAttribute(OpCustomAttribute customAttribute) {
      if (customAttribute == null) {
         throw new IllegalArgumentException("customAttribut must not be <null>");
      }
      if (customAttributes == null) {
         customAttributes = new HashSet<OpCustomAttribute>();
      }
      customAttributes.add(customAttribute);
   }
   
   public void removeCustomAttribute(OpCustomAttribute customAttribute) {
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

   private void setSubType(Byte subType) {
      this.subType = subType;
   }

   public void setCustomTypeName(String customTypeName) {
      this.customTypeName = customTypeName;
   }

   /**
    * @return
    * @pre
    * @post
    */
   public Byte getSubType() {
      return subType;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getLabel() {
      return label;
   }

   public void setLabel(String label) {
      this.label = label;
   }
   
   public boolean isDeleted() {
      return deleted;
   }

   /**
    * @param b
    * @pre
    * @post
    */
   public void setDeleted(boolean deleted) {
      this.deleted = deleted; 
   }

}
