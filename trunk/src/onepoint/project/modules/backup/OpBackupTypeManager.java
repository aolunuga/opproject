/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.persistence.OpType;
import onepoint.service.XSizeInputStream;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that manages the field type mapping used during backup/restore.
 * These mappings are some bi-directional mappings from OpType types to string types.
 *
 * @author horia.chiorean
 */
public final class OpBackupTypeManager {

   /**
    * A map of [OpType.TYPE, typeString].
    */
   private static final Map typeStringMap = new HashMap();

   /**
    * A map of [typeString, OpType.TYPE].
    */
   private static final Map stringTypeMap = new HashMap();


   /**
    * A map of [OpType.TYPE, Class] mapping from OpType types of Java types.
    */
   private static final Map typeJavaTypeMap = new HashMap();

   /**
    * A map of [OpType.TYPE, java primitive wrapper class]
    */
   private static final Map primitiveJavaTypeMap = new HashMap();

   /**
    * Initialize the type maps.
    */
   static {
      String typeName = "Boolean";
      Integer type = new Integer(OpType.BOOLEAN);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typeJavaTypeMap.put(type, Boolean.TYPE);
      primitiveJavaTypeMap.put(type, Boolean.class);

      typeName = "Integer";
      type = new Integer(OpType.INTEGER);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typeJavaTypeMap.put(type, Integer.TYPE);
      primitiveJavaTypeMap.put(type, Integer.class);

      typeName = "Long";
      type = new Integer(OpType.LONG);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typeJavaTypeMap.put(type, Long.TYPE);
      primitiveJavaTypeMap.put(type, Long.class);

      typeName = "String";
      type = new Integer(OpType.STRING);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typeJavaTypeMap.put(type, String.class);

      typeName = "Text";
      type = new Integer(OpType.TEXT);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typeJavaTypeMap.put(type, String.class);

      typeName = "Date";
      type = new Integer(OpType.DATE);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typeJavaTypeMap.put(type, Date.class);

      typeName = "Content";
      type = new Integer(OpType.CONTENT);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typeJavaTypeMap.put(type, XSizeInputStream.class);

      typeName = "Byte";
      type = new Integer(OpType.BYTE);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typeJavaTypeMap.put(type, Byte.TYPE);
      primitiveJavaTypeMap.put(type, Byte.class);

      typeName = "Double";
      type = new Integer(OpType.DOUBLE);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typeJavaTypeMap.put(type, Double.TYPE);
      primitiveJavaTypeMap.put(type, Double.class);

      typeName = "Timestamp";
      type = new Integer(OpType.TIMESTAMP);
      typeStringMap.put(type, typeName);
      stringTypeMap.put(typeName, type);
      typeJavaTypeMap.put(type, Timestamp.class);
   }

   /**
    * This is a utility class.
    */
   private OpBackupTypeManager() {
   }

   /**
    * Returns the name of a type (string) based on its int type.
    * @param type a <code>int</code> representing a type identifier from OpType.
    * @return a <code>String</code> representing the name of the type.
    */
   static String getTypeString(int type) {
      return (String) typeStringMap.get(new Integer(type));
   }

   /**
    * Returns the name of a type (string) based on its int type.
    * @param typeName a <code>String</code> representing the name of a type.
    * @return an <code>int</code> representing the value of the type from OpType.
    */
   static Integer getType(String typeName) {
      return (Integer) stringTypeMap.get(typeName);
   }

   /**
    * Returns the java class type for the given OpType constant.
    * @param type a <code>int</code> representing an <code>OpType</code> type identifier.
    * @return a  <code>Class</code> representing the corresponding Java type class.
    */
   static Class getJavaType(int type) {
      return (Class) typeJavaTypeMap.get(new Integer(type));
   }

   static Class getJavaPrimitiveType(int type) {
      return (Class) primitiveJavaTypeMap.get(new Integer(type));
   }
}
