/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.persistence.hibernate.cache;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import org.hibernate.cache.ReadWriteCache;

import java.util.Map;


/**
 * Listener for cache operations.
 *
 * @author florin.haizea
 */
public class OpCacheEventListener implements CacheEventListener {

   public void notifyElementRemoved(Ehcache cache, Element element)
        throws CacheException {
   }

   public void notifyElementPut(Ehcache cache, Element element)
        throws CacheException {
      //this method is called after the element newly was added in the cache
      //if the object newly added element is an OpContent object, remove it from the cache
      if (isElementAnOpContent(element)) {
         cache.remove(element.getObjectKey());
      }
   }

   public void notifyElementUpdated(Ehcache cache, Element element)
        throws CacheException {
      //this method is called after the element was added in the cache but at the same key another element already existed
      //the previous element IS NOT an OpContent object (it is some other object cached under the
      // key = onepoint.persistence.OpObject + an OpContent id, example: ReadWriteCache.Lock object)
      //but the object newly cached IS an OpContent so we must remove it from the cache
      if (isElementAnOpContent(element)) {
         cache.remove(element.getObjectKey());
      }
   }

   public void notifyElementExpired(Ehcache cache, Element element){
   }

   public void notifyElementEvicted(Ehcache cache, Element element){
   }

   public void notifyRemoveAll(Ehcache cache){
   }

   public void dispose() {          
   }

   @Override
   public Object clone()
        throws CloneNotSupportedException {
      return super.clone();
   }

   /**
    * Checks if the cache <code>Element</code> contains an <code>OpContent</code> object.
    *
    * @param element - the <code>Element</code> which is being searched.
    * @return <code>true</code> if the cache <code>Element</code> passed as parameter contains an <code>OpContent</code>
    *    object or <code>false</code> otherwise.
    */
   private boolean isElementAnOpContent(Element element) {
      if (element.getObjectValue() instanceof ReadWriteCache.Item) {
         ReadWriteCache.Item item = (ReadWriteCache.Item) element.getObjectValue();
         if (item.getValue() instanceof Map) {
            Map map = (Map) item.getValue();
            String classname = (String) map.get("_subclass");
            return "onepoint.project.modules.documents.OpContent".equals(classname);
         }
      }
      return false;
   }
}