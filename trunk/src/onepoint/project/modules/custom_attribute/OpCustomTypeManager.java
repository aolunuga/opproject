/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import onepoint.persistence.OpObject;

/**
 * @author dfreis
 *
 */
public class OpCustomTypeManager {

   public static final String SUBTYPE_DELIM = ":";
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
   
   public Map<String, OpCustomAttribute> getCustomAttributesMap(Class type, Byte subType) {
      return getCustomAttributesMap(type, subType, null);
   }
   
   public Map<String, OpCustomAttribute> getCustomAttributesMap(Class type, Byte subType, String customTypeName) {
      StringBuffer typeName = new StringBuffer(type.getName());
      typeName.append(SUBTYPE_DELIM);
      typeName.append(subType);
      typeName.append(SUBTYPE_DELIM);
      typeName.append(customTypeName);
      
      Map<String, OpCustomAttribute> typeMap = customTypes.get(typeName.toString());
      if (typeMap == null) {
         return null;
         //typeMap = new HashMap<String, OpCustomAttribute>();
      }
      return Collections.unmodifiableMap(typeMap);
   }
   
   public OpCustomAttribute getCustomAttribute(Class type, String name) {
      return getCustomAttribute(type, null, name);
   }

   public OpCustomAttribute getCustomAttribute(Class type, Byte subType, String name) {
      return getCustomAttribute(type, subType, null, name);      
   }
     
   public OpCustomAttribute getCustomAttribute(Class type, Byte subType, String customTypeName, String name) {
      StringBuffer typeName = new StringBuffer(type.getName());
      typeName.append(SUBTYPE_DELIM);
      typeName.append(subType);
      typeName.append(SUBTYPE_DELIM);
      typeName.append(customTypeName);

      while (type != null) {
         Map<String, OpCustomAttribute> map = customTypes.get(typeName.toString());
         if (map != null) {
            OpCustomAttribute attr = map.get(name);
            if (attr != null) {
               return attr;
            }
         }
         if (type == OpObject.class) {
            return null;
         }
         if (customTypeName != null) {
            customTypeName = null;
         }
         else if (subType != null) {
            // type stays the same but cut of subType from typeName
            subType = null;
         }
         else {
            type = type.getSuperclass();
         }
         typeName = new StringBuffer(type.getName());
         typeName.append(SUBTYPE_DELIM);
         typeName.append(subType);
         typeName.append(SUBTYPE_DELIM);
         typeName.append(customTypeName);
      }
      return null;
   }

}
