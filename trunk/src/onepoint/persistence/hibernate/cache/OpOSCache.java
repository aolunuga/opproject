/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate.cache;

import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.Timestamper;
import org.hibernate.util.PropertiesHelper;

import java.util.Map;

/**
 * Adapter for the OSCache implementation
 *
 * @author horia.chiorean
 */
public class OpOSCache implements Cache {

   /**
    * The OSCache 2.0 cacheAdministrator administrator.
    */
   private static GeneralCacheAdministrator cacheAdministrator = new GeneralCacheAdministrator(OpOSCacheProvider.cacheProperties);

   /**
    * The <tt>OSCache</tt> cacheAdministrator capacity property suffix.
    */
   public static final String OSCACHE_CAPACITY = "cache.capacity";

   /**
    * Cache refresh period.
    */
   private final int refreshPeriod;

   /**
    * Cron expression that can be used for refreshing.
    */
   private final String cron;

   /**
    * The name of the region this cacheAdministrator will apply to.
    */
   private final String regionName;

   /**
    * The name of the region groups this cacheAdministrator will apply to.
    */
   private final String[] regionGroups;

   /**
    * Returns a string associated with the given key.
    */
   private String toString(Object key) {
      return String.valueOf(key) + "." + regionName;
   }

   public OpOSCache(int refreshPeriod, String cron, String region) {
      this.refreshPeriod = refreshPeriod;
      this.cron = cron;
      this.regionName = region;
      this.regionGroups = new String[]{region};

      Integer capacity = PropertiesHelper.getInteger(OSCACHE_CAPACITY, OpOSCacheProvider.getCacheProperties());
      if (capacity != null) {
         cacheAdministrator.setCacheCapacity(capacity.intValue());
      }
   }

   public Object get(Object key)
        throws CacheException {
      try {
         return cacheAdministrator.getFromCache(toString(key), refreshPeriod, cron);
      }
      catch (NeedsRefreshException e) {
         cacheAdministrator.cancelUpdate(toString(key));
         return null;
      }
   }

   public void put(Object key, Object value)
        throws CacheException {
      cacheAdministrator.putInCache(toString(key), value, regionGroups);
   }

   public void remove(Object key)
        throws CacheException {
      cacheAdministrator.flushEntry(toString(key));
   }

   public void clear()
        throws CacheException {
      cacheAdministrator.flushGroup(regionName);
   }

   public void destroy()
        throws CacheException {
      synchronized (cacheAdministrator) {
         cacheAdministrator.destroy();
      }
   }

   public void lock(Object key)
        throws CacheException {
      // local cacheAdministrator, so we use synchronization
   }

   public void unlock(Object key)
        throws CacheException {
      // local cacheAdministrator, so we use synchronization
   }

   public long nextTimestamp() {
      return Timestamper.next();
   }

   public int getTimeout() {
      return Timestamper.ONE_MS * 60000; //ie. 60 seconds
   }

   public Map toMap() {
      throw new UnsupportedOperationException();
   }

   public long getElementCountOnDisk() {
      return -1;
   }

   public long getElementCountInMemory() {
      return -1;
   }

   public long getSizeInMemory() {
      return -1;
   }

   public String getRegionName() {
      return regionName;
   }

   public void update(Object key, Object value)
        throws CacheException {
      put(key, value);
   }

   public Object read(Object key)
        throws CacheException {
      return get(key);
   }
}