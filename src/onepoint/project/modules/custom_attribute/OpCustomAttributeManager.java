/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.type.CustomType;

import onepoint.persistence.OpObject;

/**
 * @author dfreis
 *
 */
public class OpCustomAttributeManager {

   public static final String SUBTYPE_DELIM = ":";
   private static OpCustomAttributeManager instance = new OpCustomAttributeManager();
   private Map<String, Map<String, OpCustomAttribute>> customAttributes = new HashMap<String, Map<String,OpCustomAttribute>>();
   private Map<String, OpCustomType> customTypes = new HashMap<String, OpCustomType>();
   private Map<Class, Class[]> hirarchyMap = new HashMap<Class, Class[]>();
   
    /**
    * 
OpCustomAttributeManagerypeManager() {
      customTypes = new HashMap<String, Map<String,OpCustomAttribute>>();
   }
   /**
    * @return
    * @pre
    * @post
    */
  public static OpCustomAttributeManager getInstance() {
      return instance;
   }

   public static void registerInstance(OpCustomAttributeManager instance) {
      OpCustomAttributeManager.instance = instance;
   }
   
   public Map<String, OpCustomAttribute> getCustomAttributesMap(Class type, Byte subType) {
      return getCustomAttributesMap(type, subType, null);
   }
   
   public Map<String, OpCustomAttribute> getCustomAttributesMap(Class objectType, Byte subType,String customTypeName) {
      Map<String, OpCustomAttribute> ret;
      Class[] hirarchy = getHirarchy(objectType);
      for (int pos = 0; pos < hirarchy.length; pos++) {
         Class type = hirarchy[pos];
         // first try general map
         String typeName;
         // first try custom map
         if (customTypeName != null) {
            typeName = getTypeName(type.getName(), subType, customTypeName);
            ret = customAttributes.get(typeName);
            if (ret != null) {
               return ret;
            }
         }
         typeName = getTypeName(type.getName(), subType, null);
         ret = customAttributes.get(typeName);
         if (ret != null) {
            return ret;
         }
      }
      return new HashMap<String, OpCustomAttribute>();
   }
   
//   String typeName = getTypeName(type.getName(), subType, null);
//   Map<String, OpCustomAttribute> typeMap = customTypes.get(typeName.toString());

   /**
    * @param name
    * @param subType
    * @param customTypeName
    * @return
    * @pre
    * @post
    */
   private static String getTypeName(String name, Byte subType, String customTypeName) {
      StringBuffer typeName = new StringBuffer(name);
      typeName.append(SUBTYPE_DELIM);
      typeName.append(subType);
      typeName.append(SUBTYPE_DELIM);
      typeName.append(customTypeName);
      return typeName.toString();
   }

   public OpCustomAttribute getCustomAttribute(Class type, String name) {
      return getCustomAttribute(type, null, name);
   }

   public OpCustomAttribute getCustomAttribute(Class type, Byte subType, String name) {
      return getCustomAttribute(type, subType, null, name);      
   }
     
   public OpCustomAttribute getCustomAttribute(Class objectType, Byte subType, String customTypeName, String name) {
      Class[] hirarchy = getHirarchy(objectType);
      if (hirarchy.length == 0) {
         return null;
      }
      int pos = 0;
      Class type = hirarchy[pos];
      while (true) {
         String typeName = getTypeName(type.getName(), subType, customTypeName);
         Map<String, OpCustomAttribute> map = customAttributes.get(typeName);
         if (map != null) {
            OpCustomAttribute attr = map.get(name);
            if (attr != null) {
               return attr;
            }
         }
         if (customTypeName != null) {
            customTypeName = null;
         }
         else if (subType != null) {
            // type stays the same but cut of subType from typeName
            subType = null;
         }
         else {
            pos++;
            if (pos >= hirarchy.length) {
               break;
            }
            type = hirarchy[pos];
         }
      }
      return null;
   }
   
   /**
    * @param objectType
    * @return
    * @pre
    * @post
    */
   private Class[] getHirarchy(Class objectType) {
      Class[] ret = hirarchyMap.get(objectType);
      if (ret != null) {
         return ret;
      }
      LinkedList<Class> classes = new LinkedList<Class>();
      classes.add(objectType);
      while (objectType != OpObject.class) {
         objectType = objectType.getSuperclass();
         classes.add(objectType);
      }
      Class[] toAdd = new Class[classes.size()];
      classes.toArray(toAdd);
      hirarchyMap.put(objectType, toAdd);
      return toAdd;
   }

   protected void clear() {
      customAttributes.clear();
      customTypes.clear();
   }

   /**
    * @param type
    * @param attributeMap
    * @pre
    * @post
    */
   protected void putCustomAttribute(OpCustomType type,
         LinkedHashMap<String, OpCustomAttribute> attributeMap) {
      StringBuffer typeName = new StringBuffer(type.getPrototypeName());
      typeName.append(':');
      typeName.append(type.getSubType());
      typeName.append(':');
      typeName.append(type.getCustomTypeName());
      
      customAttributes.put(typeName.toString(), attributeMap);      
      customTypes.put(typeName.toString(), type);
   }

   /**
    * @param prototypeClass
    * @param subType
    * @param object
    * @return
    * @pre
    * @post
    */
   public OpCustomType getCustomType(Class type, Byte subType,
         String customTypeName) {
      // first get general map
      String typeName = getTypeName(type.getName(), subType, customTypeName);
      return customTypes.get(typeName.toString());
   }

}
