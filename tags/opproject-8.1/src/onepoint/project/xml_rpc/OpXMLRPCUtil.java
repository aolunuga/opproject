/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.xml_rpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpMember;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpTypeManager;
import onepoint.project.modules.backup.OpBackupTypeManager;

import org.hibernate.collection.PersistentSet;

/**
 * @author dfreis
 *
 */
public class OpXMLRPCUtil {
   /**
    * This class's logger
    */
   private static final XLog logger = XLogFactory.getLogger(OpXMLRPCUtil.class);
   private static final HashSet<Class> ALLOWED_VALUES;
   public static final String ID_KEY = "_Id";
   public static final String LOCATOR_KEY = "_Locator";
   private static final Class[] COMPOUNDS;
   
   static {
      ALLOWED_VALUES = new HashSet<Class>();
      ALLOWED_VALUES.add(Boolean.class);
      ALLOWED_VALUES.add(Integer.class);
      ALLOWED_VALUES.add(String.class);
      ALLOWED_VALUES.add(Double.class);
      ALLOWED_VALUES.add(Date.class);
      ALLOWED_VALUES.add(byte[].class);
      //ALLOWED_VALUES.add(Map.class);
      //ALLOWED_VALUES.add(List.class);
      
      COMPOUNDS = new Class[] { 
            Map.class,
            List.class,
            PersistentSet.class,
            Set.class
      };
   }

   public static <K, V> void deleteMapNullValues(Map<K, V> map) {
      Iterator<V> iter = map.values().iterator();
      while (iter.hasNext()) {
         V value = iter.next();
         if (value == null) {
            iter.remove();
         }
      }
   }
   
   /**
    * @param ignoremode 
    * @param strings 
    * @param activity
    * @param ret
    * @pre
    * @post
    */
   public static Map convertToXMLRPCMap(OpObject object, boolean ignoremode) {
      return convertToXMLRPCMap(object, new String[]{}, ignoremode);
   }

   /**
    * @param ignoremode 
    * @param activity
    * @param strings
    * @return
    * @pre
    * @post
    */
   public static Map<String, Object> convertToXMLRPCMap(OpObject object, String[] ignore) {
      return convertToXMLRPCMap(object, ignore, true);
   }
   
   /**
    * @param ignoremode 
    * @param activity
    * @param strings
    * @return
    * @pre
    * @post
    */
   public static Map<String, Object> convertToXMLRPCMap(OpObject object, String[] ignore, boolean ignoremode) {
      HashSet<String> ignoredKeys = new HashSet<String>();
      ignoredKeys.addAll(Arrays.asList(ignore));
      if (!ignoremode) { // complete list
         for (int count = 0; count < ignore.length; count++) {
           int pos = 0;
           while (true) {
              int delim = ignore[count].indexOf('.', pos);
              if (delim > 0) {
                 ignoredKeys.add(ignore[count].substring(0, delim));
                 pos = delim+1;
              }
              else {
                 break;
              }
           }
         }
      }
      Map<Object, Integer> visited = new IdentityHashMap<Object, Integer>();
      IntHolder id = new IntHolder(0);
      Map ret = opObjectToXMLRPCMap(object, null, ignoredKeys, ignoremode, visited, id, null);
      return ret;
   }

   
   /**
    * @param ignoremode 
    * @param activity
    * @param strings
    * @return
    * @pre
    * @post
    */
   public static void fillUpFromMap(OpObject object, Map<String, Object> map, String[] ignore, boolean ignoremode) {
      HashSet<String> ignoredKeys = new HashSet<String>();
      ignoredKeys.addAll(Arrays.asList(ignore));

      Map<String, Object> cache = new HashMap<String, Object>();
      fillUpFromMap(object, map, map, ignoredKeys, ignoremode, cache, null);
   }


   /**
    * @param object
    * @param map
    * @param ignoredKeys
    * @param ignoremode
    * @param cache
    * @pre
    * @post
    */
   private static void fillUpFromMap(OpObject opObject, Map<String, Object> map, Map<String, Object> topMap, HashSet<String> ignore, boolean ignoremode, Map<String, Object> cache, String prefix) {
      if (prefix == null) {
         prefix = "";
      }
         
      // FIXME(dfreis Sep 12, 2007 6:42:25 AM) should add some cache here
      OpPrototype prototype = OpTypeManager.getPrototypeByClassName(opObject.getClass().getName());
      Iterator<OpMember> members = prototype.getDeclaredMembers();
      
      while (members.hasNext()) {
         OpMember member = members.next();
         String memberName = member.getName();
         Class memberType = OpBackupTypeManager.getJavaType(member.getTypeID());
         if (ignore.contains(prefix+memberName) == ignoremode) {
            continue;
         }

         Object value = map.get(memberName);
         if (value instanceof String)
         {
            if (((String)value).startsWith("Id_")) {
               value = topMap.get((String)value);
            }  

            if (OpObject.class.isAssignableFrom(memberType)) {
               OpObject objVal = (OpObject) getValue(opObject, memberName);
               if (objVal == null) { // no value set
                  try {
                     Constructor constr = memberType.getDeclaredConstructor(new Class[0]);
                     constr.setAccessible(true);
                     objVal = (OpObject) constr.newInstance(new Object[0]);
                  }
                  catch (InstantiationException exc) {
                     // TODO Auto-generated catch block
                     exc.printStackTrace();
                     continue;
                  }
                  catch (IllegalAccessException exc) {
                     // TODO Auto-generated catch block
                     exc.printStackTrace();
                     continue;
                  }
                  catch (SecurityException exc) {
                     // TODO Auto-generated catch block
                     exc.printStackTrace();
                     continue;
                  }
                  catch (NoSuchMethodException exc) {
                     // TODO Auto-generated catch block
                     exc.printStackTrace();
                     continue;
                  }
                  catch (IllegalArgumentException exc) {
                     // TODO Auto-generated catch block
                     exc.printStackTrace();
                     continue;
                  }
                  catch (InvocationTargetException exc) {
                     // TODO Auto-generated catch block
                     exc.printStackTrace();
                  }
                  setValue(opObject, objVal, memberName, memberType);
               }  

               fillUpFromMap(objVal, (Map)value, topMap, ignore, ignoremode, cache, memberName);
               continue;
            }

            if (Map.class.isAssignableFrom(memberType)){
               setValue(opObject, value, memberName, memberType);
               continue;
            }

            if (Set.class.isAssignableFrom(memberType)){
               setValue(opObject, value, memberName, memberType);
               continue;
            }

            System.err.print("unknown type: "+memberType.getName());
         }
      }
   }
//         Object value = null;
         
         
//         if (value != null) {
//            Object newValue = toXMLRPCObject(value, topMap, ignore, ignoremode, visited, id, prefix+memberName+".");
//            if (newValue != null) {
//               if (isCompound(newValue)) {
//                  String entryId = "Id_"+visited.get(value);
//                  params.put(memberName, entryId);
//                  topMap.put(entryId, newValue);
////                  System.err.println(memberName+" mapped to id "+entryId+" "+((params == topMap) ? "in top map" : prefix));//opObject.getClass().getName()));
//               }
//               else {
//                  params.put(memberName, newValue);
//               }ln(:
//            }
//         }
//      }
//   }

   /**
    * @param opObject
    * @param objVal2 
    * @param memberName
    * @param objVal
    * @param memberType 
    * @pre
    * @post
    */
   private static boolean setValue(OpObject opObject, Object value, String memberName, Class memberType) {
      // try it via method
      Method method = null;
      try {
         method = opObject.getClass().getMethod("set"+memberName, new Class[] {memberType});
      } 
      catch (NoSuchMethodException exc) {
      }
      if (method != null) {
         try {
            method.invoke(opObject, new Object[] { value });
            return true;
         }
         catch (IllegalArgumentException exc) {
            logger.warn(exc);
            return false;
         }
         catch (IllegalAccessException exc) {
            logger.warn(exc);
            return false;
         }
         catch (InvocationTargetException exc) {
            logger.warn(exc);
            return false;
         }
      }
      // try via field
      try {
         opObject.getClass().getField(memberName).set(opObject, value);
         return true;
      } 
      catch (NoSuchFieldException exc) {
         logger.error("no method and no field found for '"+memberName+"'");
         return false;
      }
      catch (IllegalArgumentException exc) {
         logger.error("no method and wrong field found for '"+memberName+"'");
         return false;
      }
      catch (SecurityException exc) {
         logger.error("no method and no access to field found for '"+memberName+"'");
         return false;
      }
      catch (IllegalAccessException exc) {
         logger.error("no method and no access to field found for '"+memberName+"'");
         return false;
      }
   }

   /**
    * @param ignoremode 
    * @param prefix 
    * @param activity
    * @param ret
    * @pre
    * @post
    */
   private static Map opObjectToXMLRPCMap(OpObject opObject, Map<String, Object> topMap, 
         Set<String> ignore, boolean ignoremode, Map<Object, Integer> visited, IntHolder id, String prefix) {
      Map<String, Object> params = new HashMap<String, Object>();
      if (topMap == null) { // add to params per default
         topMap = params;
      }
      if (prefix == null) {
         prefix = "";
      }
      // FIXME(dfreis Sep 12, 2007 6:42:25 AM) should add some cache here
      OpPrototype prototype = OpTypeManager.getPrototypeByClassName(opObject.getClass().getName());
      Iterator<OpMember> members = prototype.getDeclaredMembers();
      
      while (members.hasNext()) {
         OpMember member = members.next();
         String memberName = member.getName();
         if (ignore.contains(prefix+memberName) == ignoremode) {
            logger.info("Ignoring: "+prefix+memberName);
            continue;
         }
         Object value = getValue(opObject, memberName);
         
         if (value != null) {
            Object newValue = toXMLRPCObject(value, topMap, ignore, ignoremode, visited, id, prefix+memberName+".");
            if (newValue != null) {
               if (isCompound(newValue)) {
                  String entryId = "Id_"+visited.get(value);
                  params.put(memberName, entryId);
                  topMap.put(entryId, newValue);
//                  System.err.println(memberName+" mapped to id "+entryId+" "+((params == topMap) ? "in top map" : prefix));//opObject.getClass().getName()));
               }
               else {
                  params.put(memberName, newValue);
               }
            }
         }
      }
      // add default values
      params.put(ID_KEY, Long.toString(opObject.getId()));
      params.put(LOCATOR_KEY, opObject.locator());
      return params;
   }

   /**
    * @param opObject
    * @param memberName
    * @return
    * @pre
    * @post
    */
   private static Object getValue(OpObject opObject, String memberName) {
      Method method = null;
      try {
         method = opObject.getClass().getMethod("get"+memberName, new Class[]{});
      } 
      catch (NoSuchMethodException exc) {
         try {
            method = opObject.getClass().getMethod("is"+memberName, new Class[]{});
         } 
         catch (NoSuchMethodException exc2) {
         }
      }
      Object value = null;
      if (method != null) {
         try {
            value = method.invoke(opObject, new Object[] {});
         }
         catch (IllegalArgumentException exc) {
            method = null;
            logger.warn(exc);
         }
         catch (IllegalAccessException exc) {
            method = null;
            logger.warn(exc);
         }
         catch (InvocationTargetException exc) {
            method = null;
            logger.warn(exc);
         }
      }
      if (method == null) {
         // try via field
         try {
            value = opObject.getClass().getField(memberName).get(opObject);
         } 
         catch (NoSuchFieldException exc) {
            logger.error("no method and no field found for '"+memberName+"'");
         }
         catch (IllegalArgumentException exc) {
            logger.error("no method and wrong field found for '"+memberName+"'");
         }
         catch (SecurityException exc) {
            logger.error("no method and no access to field found for '"+memberName+"'");
         }
         catch (IllegalAccessException exc) {
            logger.error("no method and no access to field found for '"+memberName+"'");
         }
      }
      
      return value;
   }

   /**
    * @param value
    * @return
    * @pre
    * @post
    */
   private static Object toXMLRPCObject(Object value, Map<String, Object> topMap, 
         Set<String> ignore, boolean ignoremode, Map<Object, Integer> visited, IntHolder id, String prefix) {
      if (value instanceof OpObject) {
         // check if already added
         Integer refId = visited.get(value);
         if (refId == null) {
            refId = id.postIncValue();
            visited.put(value, refId);
            return opObjectToXMLRPCMap((OpObject)value, topMap, ignore, ignoremode, visited, id, prefix);
         }
         return "Id_"+refId;
      }
      if (ALLOWED_VALUES.contains(value.getClass())) {
         return value;
      }
         
      if (value instanceof java.sql.Date) {
         return new Date(((java.sql.Date)value).getTime());
      }
      if (value instanceof Byte) {
         return new Integer((Byte)value);
      }
      if (value instanceof Long) {
         return Long.toString((Long)value);
      }
      if (value instanceof PersistentSet) {
         // check if already added
         Integer refId = visited.get(value);
         if (refId == null) {
            visited.put(value, id.postIncValue());
            // copy to list
            return toList(((PersistentSet)value).iterator(), topMap, ignore, ignoremode, visited, id, prefix);
         }
         return "Id_"+refId;
      }
      if (value.getClass().isArray()) {
         // check if already added
         Integer refId = visited.get(value);
         if (refId == null) {
            visited.put(value, id.postIncValue());
            // copy to list
            return toList(Arrays.asList((Object[])value).iterator(), topMap, ignore, ignoremode, visited, id, prefix);
         }
         return "Id_"+refId;
      }

      if (value != null) {
         logger.error("could not convert value '"+value+"' of type '"+value.getClass().getName()+"'");
      }
      return null;
   }

   /**
    * @param id 
    * @param topMap 
    * @param visited 
    * @param ignore 
    * @param name
    * @return
    * @pre
    * @post
    */
   private static List<Object> toList(Iterator<Object> iterator, Map<String, Object> topMap, 
            Set<String> ignore, boolean ignoremode, Map<Object, Integer> visited, IntHolder id, String prefix) {
      LinkedList<Object> ret = new LinkedList<Object>();   
      while (iterator.hasNext()) {
         Object value = iterator.next();
         Object entryValue = toXMLRPCObject(value, topMap, ignore, ignoremode, visited, id, prefix);
         if (entryValue != null) {
            if (isCompound(entryValue)) {
               String entryId = "Id_"+visited.get(value);
               ret.add(entryId);
               topMap.put(entryId, entryValue);
            }
            else {
               ret.add(entryValue);
            }
         }
      }
      return ret;
   }

   /**
    * @param entryValue
    * @return
    * @pre
    * @post
    */
   private static boolean isCompound(Object value) {
      for (int count = 0; count < COMPOUNDS.length; count++) {
         if (COMPOUNDS[count].isAssignableFrom(value.getClass())) {
            return true;
         }
      }
      return false;
   }
}

class IntHolder {
   private int value;

   IntHolder(int value) {
      this.value = value; 
   }
   
   /**
    * @return
    * @pre
    * @post
    */
   public int postIncValue() {
      int ret = value;
      value++;
      return ret;
   }

   void setValue(int value) {
      this.value = value;
   }
   
   int getValue() {
      return value;
   }
}
