/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Arrays;

/**
 * Hibernate custom type for mapping a blob to a byte array. This type is needed because hibernate does not support
 * 2nd level caching of blob types.
 *
 * @author horia.chiorean
 */
public class OpBlobUserType implements UserType {
   /**
    * Logger for this class
    */
   private static final XLog logger = XLogFactory.getLogger(OpBlobUserType.class, true);
   /**
    * The sql type(s) this type maps to.
    */
   private static final int[] SQL_TYPES = {Types.BLOB};

   /**
    * The data base type (default is DERBY)
    */
   private static int DATABASE_TYPE = OpHibernateSource.DERBY;

   /**
    * @see org.hibernate.usertype.UserType#sqlTypes()
    */
   public int[] sqlTypes() {
      return SQL_TYPES;
   }

   /**
    * Sets up the <code>DATABASE_TYPE</code>
    * @param type <code>int</code> representing the data base type
    */
   public static void setDatabaseType(int type){
      DATABASE_TYPE = type;
   }

   /**
    * Returns the <code>DATABASE_TYPE</code>
    * @return int representing the data base type
    * @see OpHibernateSource
    */
   public static int getDatabaseType(){
      return DATABASE_TYPE;
   }
   /**
    * @see org.hibernate.usertype.UserType#returnedClass()
    */
   public Class returnedClass() {
      return byte[].class;
   }

   /**
    * @see org.hibernate.usertype.UserType#equals(Object, Object)
    */
   public boolean equals(Object object, Object object1)
        throws HibernateException {
      if (object == null || object1 == null) {
         return false;
      }
      if (object == object1) {
         return true;
      }
      return Arrays.equals((byte[]) object, (byte[]) object1);
   }

   /**
    * @see org.hibernate.usertype.UserType#hashCode(Object)
    */
   public int hashCode(Object object)
        throws HibernateException {
      return object.hashCode();
   }

   /**
    * @see org.hibernate.usertype.UserType#nullSafeGet(java.sql.ResultSet, String[], Object)
    */
   public Object nullSafeGet(ResultSet resultSet, String[] strings, Object object)
        throws HibernateException, SQLException {
      Blob blob = resultSet.getBlob(strings[0]);
      if (blob == null) {
         return new byte[]{};
      }
      //<FIXME author="Horia Chiorean" description="Possible issue here with large blobs">
      int length = (int) blob.length();
      //<FIXME>
      return blob.getBytes(1, length);
   }

   /**
    * @see org.hibernate.usertype.UserType#nullSafeSet(java.sql.PreparedStatement, Object, int)
    */
   public void nullSafeSet(PreparedStatement preparedStatement, Object object, int i)
        throws HibernateException, SQLException {
      if (object == null) {
         preparedStatement.setNull(i, Types.BLOB);
      }
      else {
         preparedStatement.setBytes(i, (byte[]) object);
      }
   }

   /**
    * @see org.hibernate.usertype.UserType#deepCopy(Object)
    */
   public Object deepCopy(Object object)
        throws HibernateException {
      if (object == null) {
         return null;
      }
      else {
         byte[] original = (byte[]) object;
         byte[] clone = new byte[original.length];
         System.arraycopy(original, 0, clone, 0,  original.length);
         return object;
      }
   }

   /**
    * @see org.hibernate.usertype.UserType#isMutable()
    */
   public boolean isMutable() {
      return true;
   }

   /**
    * @see org.hibernate.usertype.UserType#disassemble(Object)
    */
   public Serializable disassemble(Object object)
        throws HibernateException {
      return (Serializable) deepCopy(object);
   }

   /**
    * @see org.hibernate.usertype.UserType#assemble(java.io.Serializable, Object)
    */
   public Object assemble(Serializable serializable, Object object)
        throws HibernateException {
      return deepCopy(serializable);
   }

   /**
    * @see org.hibernate.usertype.UserType#replace(Object, Object, Object)
    */
   public Object replace(Object object, Object object1, Object object2)
        throws HibernateException {
      return deepCopy(object);
   }
}
