/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.persistence.OpQuery;
import org.hibernate.Query;

import java.util.Collection;

/**
 * @author gmesaric
 */
public class OpHibernateQuery implements OpQuery {

   private Query _query;

   OpHibernateQuery(Query query) {
      _query = query;
      _query.setCacheable(true);
   }

   public final Query getQuery() {
      return _query;
   }

   public final void setByte(int index, byte b) {
      _query.setByte(index, b);
   }

   public final void setByte(String name, byte b) {
      _query.setByte(name, b);
   }

   public final void setBoolean(int index, boolean b) {
      _query.setBoolean(index, b);
   }
   
   public final void setBoolean(String name, boolean b) {
      _query.setBoolean(name, b);
   }
   
   public final void setInteger(int index, int i) {
      _query.setInteger(index, i);
   }

   public final void setInteger(String name, int i) {
      _query.setInteger(name, i);
   }

   public final void setLong(int index, long l) {
      _query.setLong(index, l);
   }

   public final void setLong(String name, long l) {
      _query.setLong(name, l);
   }

   public final void setString(int index, String s) {
      _query.setString(index, s);
   }
   
   public final void setString(String name, String s) {
      _query.setString(name, s);
   }

   public final void setDate(int index, java.sql.Date d) {
      _query.setDate(index, d);
   }

   public final void setDate(String name, java.sql.Date d) {
      _query.setDate(name, d);
   }

   public final void setTimestamp(int index, java.util.Date d) {
      _query.setDate(index, d);
   }

   public final void setTimestamp(String name, java.util.Date d) {
      _query.setDate(name, d);
   }
   
   public final void setDouble(int index, double d) {
      _query.setDouble(index, d);
   }
   
   public final void setDouble(String name, double d) {
      _query.setDouble(name, d);
   }

   public void setCollection(String name, Collection c) {
      _query.setParameterList(name, c);
   }

   public final void setID(int index, long l) {
      _query.setLong(index, l);
   }

   public void setParameter(int index, Object o) {
      _query.setParameter(index, o);
   }
}
