/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.xml_rpc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author dfreis
 *
 */
public class OpXMLRPCUtil {

   public static <K, V> void convertMapNullValues(Map<K, V> map, V value) {
      for (Entry<K, V> entry : map.entrySet()) {
         if (entry.getValue() == null) {
            entry.setValue(value);
         }
      }
   }

   public static <K, V> void deleteMapNullValues(Map<K, V> map) {
      Iterator<V> iter = map.values().iterator();
      while (iter.hasNext()) {
         V value = iter.next();
         if (value == null) {
            iter.remove();
         }
      }
   }

}
