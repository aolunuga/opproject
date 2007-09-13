/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.Collection;

/**
 * @author gmesaric
 */
public interface OpQuery {

   // Value positions start at index zero (0)

   public void setLong(int index, long l);

   public void setLong(String name, long l);

   public void setInteger(int index, int i);

   public void setInteger(String name, int i);

   public void setByte(int index, byte b);

   public void setByte(String name, byte b);

   public void setBoolean(int index, boolean b);

   public void setBoolean(String name, boolean b);

   public void setString(int index, String s);

   public void setString(String name, String s);

   public void setDate(int index, java.sql.Date d);

   public void setDate(String name, java.sql.Date d);

   public void setTimestamp(int index, java.util.Date t);

   public void setTimestamp(String name, java.util.Date t);

   public void setDouble(int index, double d);

   public void setDouble(String name, double d);

   public void setCollection(String name, Collection c);

   // Special setter for long-based object IDs
   public void setID(int index, long l);

   public void setParameter(int index, Object o);

   public void setFirstResult(int objectIndex);

   public void setMaxResults(int count);

   public void setFetchSize(int size);
}
