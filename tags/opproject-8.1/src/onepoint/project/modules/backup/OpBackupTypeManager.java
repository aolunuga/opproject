/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpType;
import onepoint.project.util.Quadruple;
import onepoint.project.util.Triple;
import onepoint.service.XSizeInputStream;

/**
 * Class that manages the field type mapping used during backup/restore.
 * These mappings are some bi-directional mappings from OpType types to string types.
 *
 * @author horia.chiorean
 */
public final class OpBackupTypeManager {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpBackupTypeManager.class);

   /**
    * A map of [OpType.TYPE, typeString].
    */
   private static final Map<Integer, String> typeStringMap = new HashMap<Integer, String>();

   /**
    * A map of [typeString, OpType.TYPE].
    */
   private static final Map<String, Integer> stringTypeMap = new HashMap<String, Integer>();


   /**
    * A map of [OpType.TYPE, Class] mapping from OpType types of Java types.
    */
   private static final Map<Integer, Class> typePrimitiveJavaTypeMap = new HashMap<Integer, Class>();

   /**
    * A map of [OpType.TYPE, java primitive wrapper class]
    */
   private static final Map<Integer, Class> typeJavaTypeMap = new HashMap<Integer, Class>();

   private static Map<Triple<Class, String, String>, Quadruple<Class, String, Class, String>> methodMap = new HashMap<Triple<Class,String,String>, Quadruple<Class,String,Class, String>>();

   /**
    * Initialize the type maps.
    */
   static {
      String typeName = "Boolean";
      Integer type = new Integer(OpType.BOOLEAN);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typePrimitiveJavaTypeMap.put(type, Boolean.TYPE);
      typeJavaTypeMap.put(type, Boolean.class);

      typeName = "Integer";
      type = new Integer(OpType.INTEGER);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typePrimitiveJavaTypeMap.put(type, Integer.TYPE);
      typeJavaTypeMap.put(type, Integer.class);

      typeName = "Long";
      type = new Integer(OpType.LONG);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typePrimitiveJavaTypeMap.put(type, Long.TYPE);
      typeJavaTypeMap.put(type, Long.class);

      typeName = "String";
      type = new Integer(OpType.STRING);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typePrimitiveJavaTypeMap.put(type, String.class);

      typeName = "Text";
      type = new Integer(OpType.TEXT);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typePrimitiveJavaTypeMap.put(type, String.class);

      typeName = "Date";
      type = new Integer(OpType.DATE);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typePrimitiveJavaTypeMap.put(type, Date.class);

      typeName = "Content";
      type = new Integer(OpType.CONTENT);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typePrimitiveJavaTypeMap.put(type, XSizeInputStream.class);

      typeName = "Byte";
      type = new Integer(OpType.BYTE);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typePrimitiveJavaTypeMap.put(type, Byte.TYPE);
      typeJavaTypeMap.put(type, Byte.class);

      typeName = "Double";
      type = new Integer(OpType.DOUBLE);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typePrimitiveJavaTypeMap.put(type, Double.TYPE);
      typeJavaTypeMap.put(type, Double.class);

      typeName = "Timestamp";
      type = new Integer(OpType.TIMESTAMP);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typePrimitiveJavaTypeMap.put(type, Timestamp.class);
   }

   /**
    * This is a utility class.
    */
   private OpBackupTypeManager() {
   }

   /**
    * Returns the name of a type (string) based on its int type.
    *
    * @param type a <code>int</code> representing a type identifier from OpType.
    * @return a <code>String</code> representing the name of the type.
    */
   static String getTypeString(int type) {
      return (String) typeStringMap.get(new Integer(type));
   }

   /**
    * Returns the name of a type (string) based on its int type.
    *
    * @param typeName a <code>String</code> representing the name of a type.
    * @return an <code>int</code> representing the value of the type from OpType.
    */
   static Integer getType(String typeName) {
      return (Integer) stringTypeMap.get(typeName);
   }

   /**
    * Returns the java class type for the given OpType constant.
    *
    * @param type a <code>int</code> representing an <code>OpType</code> type identifier.
    * @return a  <code>Class</code> representing the corresponding Java type class.
    */
   static Class getPrimitiveJavaType(int type) {
      return (Class) typePrimitiveJavaTypeMap.get(new Integer(type));
   }

   /**
    * Returns the java class type for the given OpType constant.
    *
    * @param type a <code>int</code> representing an <code>OpType</code> type identifier.
    * @return a  <code>Class</code> representing the corresponding Java type class.
    */
   public static Class getJavaType(int type) {
      return (Class) typeJavaTypeMap.get(new Integer(type));
   }

   /**
    * Returns the map of [OpType.TYPE, Class] entries.
    *
    * @return a map from OpType to java class.
    */
   static Map<Integer, Class> getTypeJavaTypeMap() {
      return typeJavaTypeMap;
   }

   /**
    * Returns the map of [OpType.TYPE, Class] entries (containing java primitive types).
    *
    * @return a map from OpType to java class.
    */
   static Map<Integer, Class> getTypePrimitiveJavaTypeMap() {
      return typePrimitiveJavaTypeMap;
   }

   static Object convertParsedValue(int typeId, String valueString, String workingDirectory) {
      switch (typeId) {
         case OpType.BOOLEAN: {
            return Boolean.valueOf(valueString);
         }
         case OpType.INTEGER: {
            return Integer.valueOf(valueString);
         }
         case OpType.LONG: {
            return Long.valueOf(valueString);
         }
         case OpType.STRING: {
            return valueString;
         }
         case OpType.TEXT: {
            return valueString;
         }
         case OpType.DATE: {
            try {
               return new Date(OpBackupManager.DATE_FORMAT.parse(valueString).getTime());
            }
            catch (ParseException e) {
               logger.error("Could not parse date:" + valueString);
            }
            break;
         }
         case OpType.CONTENT: {
            if (valueString != null) {
               String contentPath = workingDirectory + valueString;
               return OpBackupManager.readBinaryFile(contentPath);
            }
            break;
         }
         case OpType.BYTE: {
            return Byte.valueOf(valueString);
         }
         case OpType.DOUBLE: {
            return Double.valueOf(valueString);
         }
         case OpType.TIMESTAMP: {
            try {
               return new Timestamp(OpBackupManager.TIMESTAMP_FORMAT.parse(valueString).getTime());
            }
            catch (ParseException e) {
               logger.error("Could not parse timestamp:" + valueString);
            }
            break;
         }
         default: {
            logger.error("Unsupported type ID " + typeId);
         }
      }
      return null;
   }

   /**
    * @param name
    * @param accesorArgument
    * @return
    * @pre
    * @post
    */
   public static Quadruple<Class, String, Class, String> getMappedMethod(Class oldTarget, String oldMethodName, String oldArgClassName) {
      Triple<Class, String, String> old = new Triple<Class, String, String>(oldTarget, oldMethodName, oldArgClassName);
      return methodMap.get(old);
   }
   
   public static void addMappedMethod(Class oldTarget, String oldMethodName, String oldArgClassName, Class newTarget, String newMethodName, Class newArgClass)  {
      addMappedMethod(oldTarget, oldMethodName, oldArgClassName, newTarget, newMethodName, newArgClass, null);
   }

   /**
    * @param class1
    * @param string
    * @param class2
    * @param class3
    * @param string2
    * @param class4
    * @param string3
    * @throws NoSuchMethodException 
    * @throws SecurityException 
    * @pre
    * @post
    */
   public static void addMappedMethod(Class oldTarget, String oldMethodName, String oldArgClassName, 
                                                                    Class newTarget, String newMethodName, Class newArgClass, 
                                                                    String convertMethodName) {
//      Method method = null;
//      if (convertMethodName != null) {
//         try {
//            method = oldArgClass.getDeclaredMethod(convertMethodName, new Class[0]);
//         }
//         catch (SecurityException exc) {
//            logger.error("method not accessible: "+convertMethodName, exc);
//         }
//         catch (NoSuchMethodException exc) {
//            logger.error("method not found: "+convertMethodName, exc);
//         }
//         method.setAccessible(true);
//      }
      methodMap.put(new Triple<Class, String, String>(oldTarget, oldMethodName, oldArgClassName), 
                                                  new Quadruple<Class, String, Class, String>(newTarget, newMethodName, newArgClass, convertMethodName));
   }
}
