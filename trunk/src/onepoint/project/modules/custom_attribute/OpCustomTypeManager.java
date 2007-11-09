/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import onepoint.persistence.OpObject;

import com.sun.corba.se.pept.transport.ContactInfo;

/**
 * @author dfreis
 *
 */
public class OpCustomTypeManager {

   private static OpCustomTypeManager instance = new OpCustomTypeManager();
   protected Map<String, Map<String, OpCustomAttribute>> customTypes;

    /**
    * 
    */
   public OpCustomTypeManager() {
      customTypes = new HashMap<String, Map<String,OpCustomAttribute>>();
   }
   /**
    * @return
    * @pre
    * @post
    */
   public static OpCustomTypeManager getInstance() {
      return instance;
   }

   public static void registerInstance(OpCustomTypeManager instance) {
      OpCustomTypeManager.instance = instance;
   }
   
   public Map<String, OpCustomAttribute> getCustomAttributesMap(Class type) {
      Map<String, OpCustomAttribute> typeMap = customTypes.get(type.getName());
      if (typeMap == null) {
         return null;
         //typeMap = new HashMap<String, OpCustomAttribute>();
      }
      return Collections.unmodifiableMap(typeMap);
   }
   
   public OpCustomAttribute getCustomAttribute(Class type, String name) {
      while (type != null) {
         Map<String, OpCustomAttribute> map = customTypes.get(type.getName());
         if (map != null) {
            OpCustomAttribute attr = map.get(name);
            if (attr != null) {
               return attr;
            }
         }
         if (type == OpObject.class) {
            return null;
         }
         type = type.getSuperclass();
      }
      return null;
   }
}
