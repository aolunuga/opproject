/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.*;
import java.sql.*;

import onepoint.service.XSizeInputStream;

/**
 * Hibernate custom type for mapping a blob to an <code>InputStream</code>. This type is needed because hibernate does not support
 * 2nd level caching of blob types.
 *
 * @author horia.chiorean
 */
public class OpBlobUserType implements UserType {

   /**
    * The sql type(s) this type maps to.
    */
   private static final int[] SQL_TYPES = {Types.BLOB};

   /**
    * The data base type
    */
   private static int DATABASE_TYPE;

   /**
    * @see org.hibernate.usertype.UserType#sqlTypes()
    */
   public int[] sqlTypes() {
      return SQL_TYPES;
   }

   /**
    * Sets up the <code>DATABASE_TYPE</code>
    *
    * @param type <code>int</code> representing the data base type
    */
   public static void setDatabaseType(int type) {
      DATABASE_TYPE = type;
   }

   /**
    * Returns the <code>DATABASE_TYPE</code>
    *
    * @return int representing the data base type
    * @see OpHibernateSource
    */
   public static int getDatabaseType() {
      return DATABASE_TYPE;
   }

   /**
    * @see org.hibernate.usertype.UserType#returnedClass()
    */
   public Class returnedClass() {
      return XSizeInputStream.class;
   }

   /**
    * @see org.hibernate.usertype.UserType#equals(Object,Object)
    */
   public boolean equals(Object x, Object y)
        throws HibernateException {
      return (x == y) || (x != null && y != null && x.equals(y));
   }

   /**
    * @see org.hibernate.usertype.UserType#hashCode(Object)
    */
   public int hashCode(Object object)
        throws HibernateException {
      return object.hashCode();
   }

   /**
    * @see org.hibernate.usertype.UserType#nullSafeGet(java.sql.ResultSet,String[],Object)
    */
   public Object nullSafeGet(ResultSet resultSet, String[] strings, Object object)
        throws HibernateException, SQLException {
      InputStream is = resultSet.getBinaryStream(strings[0]);
      if (is == null) {
         is = new ByteArrayInputStream(new byte[0]); // return an empty but not null stream
      }
      return new XSizeInputStream(is, XSizeInputStream.UNKNOW_STREAM_SIZE); // set size as unknown. Should be set to real value ASAP.
   }

   /**
    * @see org.hibernate.usertype.UserType#nullSafeSet(java.sql.PreparedStatement,Object,int)
    */
   public void nullSafeSet(PreparedStatement preparedStatement, Object object, int i)
        throws HibernateException, SQLException {
      if (object == null) {
         preparedStatement.setNull(i, Types.BLOB); // set the null blob type if the stream is provided
      }
      else {
         XSizeInputStream is = (XSizeInputStream) object;
         preparedStatement.setBinaryStream(i, is, (int) is.getSize());
      }
   }

   /**
    * @see org.hibernate.usertype.UserType#deepCopy(Object)
    */
   public Object deepCopy(Object object)
        throws HibernateException {
      return object;
   }

   /**
    * @see org.hibernate.usertype.UserType#isMutable()
    */
   public boolean isMutable() {
      return false;
   }

   /**
    * @see org.hibernate.usertype.UserType#disassemble(Object)
    */
   public Serializable disassemble(Object object)
        throws HibernateException {
      return null; // streams can't be serialized
   }

   /**
    * @see org.hibernate.usertype.UserType#assemble(java.io.Serializable,Object)
    */
   public Object assemble(Serializable serializable, Object object)
        throws HibernateException {
      return null; // streams can't be serialized
   }

   /**
    * @see org.hibernate.usertype.UserType#replace(Object,Object,Object)
    */
   public Object replace(Object object, Object object1, Object object2)
        throws HibernateException {
      return object;
   }
}
