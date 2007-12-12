/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.persistence.hibernate.cache;

import net.sf.ehcache.event.CacheEventListenerFactory;

import java.util.Properties;

/**
 * Listener Factory for specific type of entities.
 *
 * @author florin.haizea
 */
public class OpCacheEventListenerFactory extends CacheEventListenerFactory {

   /**
    * Create an <code>OpCacheEventListener</code> object which monitors all cache operations regarding a specific type
    *    of objects.
    *
    * @param properties - aditional parameters needed for the creation of the listener.
    * @return an <code>OpCacheEventListener</code> object which monitors all cache operations regarding a specific type
    *    of objects.
    */
   @Override   
   public OpCacheEventListener createCacheEventListener(Properties properties) {
      return new OpCacheEventListener();
   }
}
