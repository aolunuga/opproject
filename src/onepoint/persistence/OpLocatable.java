/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.persistence;

/**
 * @author dfreis
 *
 */
public interface OpLocatable extends Comparable<OpLocatable>{
   void setId(long id);

   public abstract long getId();

   public abstract String getSiteId();

   public abstract void setSiteId(String siteId);

   public abstract String locator();
   
}
