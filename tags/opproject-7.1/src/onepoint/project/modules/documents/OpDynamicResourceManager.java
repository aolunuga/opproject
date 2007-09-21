/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.documents;

import onepoint.persistence.OpObject;

import java.util.Iterator;
import java.util.Set;

/**
 * Class responsible for performing various operations on dynamic resources.
 *
 * @author horia.chiorean
 */
public final class OpDynamicResourceManager {

   /**
    * Utility class.
    */
   private OpDynamicResourceManager() {
   }

   /**
    * Gets a locale-specific dynamic resource for the given object.
    * @param localeId a <code>String</code> representing the id of a locale.
    * @param object a <code>OpObject</code> entity.
    * @return a <code>OpDynamicResource</code> that has the given locale, or <code>null</code> if nothing is found.
    */
   public static OpDynamicResource getDynamicResourceForLocale(String localeId, OpObject object) {
      Set dynamicResources = object.getDynamicResources();
      for (Iterator it = dynamicResources.iterator(); it.hasNext();) {
         OpDynamicResource dynamicResource = (OpDynamicResource) it.next();
         if (dynamicResource.getLocale().equals(localeId)) {
            return dynamicResource;
         }
      }
      return null;
   }
}
