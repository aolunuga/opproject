/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.documents;

import onepoint.persistence.OpObject;

/**
 * Entity representing a dynamic resource.
 *
 * @author horia.chiorean
 */
public class OpDynamicResource extends OpObject {
   
   public final static String DYNAMIC_RESOURCE = "OpDynamicResource";

   /**
    * The locale of the resource.
    */
   private String locale = null;

   /**
    * The name of the resource.
    */
   private String name = null;

   /**
    * The value of the resource.
    */
   private String value = null;

   /**
    * The object thas is associated with the resource.
    */
   private OpObject object = null;

   /**
    * Gets the resource locale.
    * @return a <code>String</code> representing the user locale.
    */
   public String getLocale() {
      return locale;
   }

   /**
    * Sets the resource locale.
    * @param locale a <code>String</code> representing the user locale.
    */
   public void setLocale(String locale) {
      this.locale = locale;
   }

   /**
    * Gets the resource name.
    * @return a <code>String</code> representing the resource name.
    */
   public String getName() {
      return name;
   }

   /**
    * Sets the resource name.
    * @param name a <code>String</code> representing the name of the resource.
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * Gets the value of the resource.
    * @return a <code>String</code> representing the value of the resource.
    */
   public String getValue() {
      return value;
   }

   /**
    * Sets the value of the resource.
    * @param value a <code>String</code> representing the value of the resource.
    */
   public void setValue(String value) {
      this.value = value;
   }

   /**
    * Gets the object associated with the resource.
    * @return a <code>OpObject</code> representing the object associated with the resource.
    */
   public OpObject getObject() {
      return object;
   }

   /**
    * Sets the object associated with the resource.
    * @param object an <code>OpObject</code> representing the object associated with the resource.
    */
   public void setObject(OpObject object) {
      this.object = object;
   }
}
