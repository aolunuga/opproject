/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.persistence.OpQuery;
import org.hibernate.Query;

import java.util.Collection;

/**
 * @author gmesaric
 */
public class OpHibernateQuery implements OpQuery {

   private Query query;

   OpHibernateQuery(Query query) {
      this.query = query;
      this.query.setCacheable(true);
   }

   public final Query getQuery() {
      return query;
   }

   public final void setByte(int index, byte b) {
      query.setByte(index, b);
   }

   public final void setByte(String name, byte b) {
      query.setByte(name, b);
   }

   public final void setBoolean(int index, boolean b) {
      query.setBoolean(index, b);
   }
   
   public final void setBoolean(String name, boolean b) {
      query.setBoolean(name, b);
   }
   
   public final void setInteger(int index, int i) {
      query.setInteger(index, i);
   }

   public final void setInteger(String name, int i) {
      query.setInteger(name, i);
   }

   public final void setLong(int index, long l) {
      query.setLong(index, l);
   }

   public final void setLong(String name, long l) {
      query.setLong(name, l);
   }

   public final void setString(int index, String s) {
      query.setString(index, s);
   }
   
   public final void setString(String name, String s) {
      query.setString(name, s);
   }

   public final void setDate(int index, java.sql.Date d) {
      query.setDate(index, d);
   }

   public final void setDate(String name, java.sql.Date d) {
      query.setDate(name, d);
   }

   public final void setTimestamp(int index, java.util.Date d) {
      query.setDate(index, d);
   }

   public final void setTimestamp(String name, java.util.Date d) {
      query.setDate(name, d);
   }
   
   public final void setDouble(int index, double d) {
      query.setDouble(index, d);
   }
   
   public final void setDouble(String name, double d) {
      query.setDouble(name, d);
   }

   public void setCollection(String name, Collection c) {
      query.setParameterList(name, c);
   }

   public final void setID(int index, long l) {
      query.setLong(index, l);
   }

   public void setParameter(int index, Object o) {
      query.setParameter(index, o);
   }

   public void setParameter(String name, Object o) {
      query.setParameter(name, o);
   }


   public void setFirstResult(int objectIndex) {
      query.setFirstResult(objectIndex);
   }

   public void setMaxResults(int count) {
      query.setMaxResults(count);
   }
}
