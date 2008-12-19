/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import onepoint.persistence.OpObject;
import onepoint.persistence.OpSource;
import onepoint.project.OpProjectSession;
import onepoint.service.server.XSession;

/**
 * @author dfreis
 *
 */
public class OpCustomAttributeManager {

   public static final String SUBTYPE_DELIM = ":";
   private static Map<String, OpCustomAttributeManager> instanceMap = new HashMap<String, OpCustomAttributeManager>();
   private static Class<? extends OpCustomAttributeManager> defaultInstance = OpCustomAttributeManager.class;
   private Map<String, Map<String, OpCustomAttribute>> customAttributes = new HashMap<String, Map<String,OpCustomAttribute>>();
   private Map<String, OpCustomType> customTypes = new HashMap<String, OpCustomType>();
   private Map<Class, Class[]> hirarchyMap = new HashMap<Class, Class[]>();
   
    public OpCustomAttributeManager(OpProjectSession session) {
   }

   /**
    * @return
    * @pre
    * @post
    */
  public static OpCustomAttributeManager getInstance() {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      String name = OpSource.DEFAULT_SOURCE_NAME; // to enable repository
      // upgrade...
      if (session != null) {
         name = session.getSourceName();
      }
      OpCustomAttributeManager man = instanceMap.get(name);
      if (man == null) {
         Constructor<? extends OpCustomAttributeManager> constructor;
         try {
            constructor = defaultInstance.getConstructor(OpProjectSession.class);
            man = constructor.newInstance(session);
         } catch (SecurityException e) {
         } catch (NoSuchMethodException e) {
         } catch (IllegalArgumentException e) {
         } catch (InstantiationException e) {
         } catch (IllegalAccessException e) {
         } catch (InvocationTargetException e) {
         }
         if (man == null) {
            man = new OpCustomAttributeManager(session);
         }
      }
      registerInstance(name, man);
      return man;
   } 

   public static void registerInstance(String name, OpCustomAttributeManager instance) {
      instanceMap.put(name, instance);
   }

   public static void registerDefaultInstance(Class<? extends OpCustomAttributeManager> instance) {
      defaultInstance  = instance;
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
      while (objectType != null && objectType != OpObject.class) {
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

   public synchronized void invalidate() {
   }

}
