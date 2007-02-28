/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class that represents a sort order criteria for an OpObject entity. It is used to allow the sorting of objects based
 * on the name of one of their properties.
 *
 * @author horia.chiorean
 */
public final class OpObjectOrderCriteria {

   /**
    * Constants that represend the sort order.
    */
   public static final String ASCENDING = "asc";
   public static final String DESCENDING = "desc";

   /**
    * A constant used for defining an empty order criteria.
    */
   public static final OpObjectOrderCriteria EMPTY_ORDER = new OpObjectOrderCriteria("OpObject", new HashMap());

   /**
    * The name of the object (entity) for which the sort order should apply.
    */
   private String objectName = null;

   /**
    * A map of [propertyName, sortOrder] that contains the sorting criterias for the object.
    */
   private Map orderCriterias = null;

   /**
    * Creates a new sort order criteria for the given object name and a list of criterias.
    *
    * @param objectName     a <code>String</code> representing the name of the object(entity) for which to create the sort
    *                       order.
    * @param orderCriterias a <code>Map</code> of [String, String] representing [properyName, sortOder] pairs.
    */
   public OpObjectOrderCriteria(String objectName, Map orderCriterias) {
      this.objectName = objectName;
      this.orderCriterias = orderCriterias;
      validateOrderCriteria();
   }

   /**
    * Creates a new sort order criteria for the given object name and a single sort criteria.
    *
    * @param objectName     a <code>String</code> representing the name of the object(entity) for which to create the sort
    *                       order.
    * @param propertyName a <code>String</code> representing the name of the objects property for which the sorting should be done.
    * @param sortOrder a <code>String</code> representing the sort order.
    * @see OpObjectOrderCriteria#ASCENDING
    * @see OpObjectOrderCriteria#DESCENDING
    */
   public OpObjectOrderCriteria(String objectName, String propertyName, String sortOrder) {
      Map orderCriterias = new HashMap(1);
      orderCriterias.put(propertyName, sortOrder);
      this.objectName = objectName;
      this.orderCriterias = orderCriterias;
      validateOrderCriteria();
   }

   /**
    * Gets the name of the object of this order criteria.
    * @return a <code>String</code> representing the name of the object.
    */
   public String getObjectName() {
      return objectName;
   }

   /**
    * Generates a Hibernate sort query string, using the optional given alias.
    * @param alias a <code>String</code> representing the name of an alias to use. If it is <code>null</code> then the
    * object name.
    * @return a <code>String</code> representing a sort query.
    */
   public String toHibernateQueryString(String alias) {
      if (objectName == null || orderCriterias == null || orderCriterias.size() == 0) {
         return "";
      }

      if (alias == null) {
         alias = objectName;
      }
      StringBuffer result = new StringBuffer();
      result.append(" order by ");
      Iterator criteriasIterator = orderCriterias.keySet().iterator();
      while (criteriasIterator.hasNext()) {
         String propertyName = (String) criteriasIterator.next();
         String sortOrder = (String) orderCriterias.get(propertyName);
         result.append(alias);
         result.append(".");
         result.append(propertyName);
         result.append(" ");
         result.append(sortOrder);
         if (criteriasIterator.hasNext()) {
            result.append(", ");
         }
      }
      return result.toString();
   }

   /**
    * Generates a Hibernate query part that may be used in the group by clause of a hibernate query.
    * @param alias a <code>String</code> representing the name of an alias to use. If it is <code>null</code> then the
    * object name.
    * @return a <code>String</code> representing a sort query.
    */
   public String toHibernateGroupByQuery(String alias) {
      if (objectName == null || orderCriterias == null || orderCriterias.size() == 0) {
            return "";
         }

         if (alias == null) {
            alias = objectName;
         }
         StringBuffer result = new StringBuffer();
         Iterator criteriasIterator = orderCriterias.keySet().iterator();
         while (criteriasIterator.hasNext()) {
            String propertyName = (String) criteriasIterator.next();
            result.append(alias);
            result.append(".");
            result.append(propertyName);
            if (criteriasIterator.hasNext()) {
               result.append(", ");
            }
            result.append(" ");
         }
         return result.toString();
   }

   /**
    * Validates an instance of an order criteria, to make sure the object name, properties and sort order are ok.
    * @throws OpObjectOrderValidationException if the validation fails.
    */
   private void validateOrderCriteria()
         throws OpObjectOrderValidationException {
      OpPrototype prototype = OpTypeManager.getPrototype(objectName);
      if (prototype == null) {
         throw new OpObjectOrderValidationException("The object name " + objectName + " used in the sort order criteria has not been loaded by the type manager");
      }
      Iterator it = orderCriterias.keySet().iterator();
      while (it.hasNext()) {
         String propertyName = (String) it.next();
         if (propertyName == null || prototype.getMember(propertyName) == null) {
            throw new OpObjectOrderValidationException("Member " + propertyName + " not found for the " + objectName + " object");
         }
         String sortOrder = (String) orderCriterias.get(propertyName);
         if (sortOrder == null && !sortOrder.equals(ASCENDING) && !sortOrder.equals(DESCENDING)) {
            throw new OpObjectOrderValidationException("Invalid sort oder " + sortOrder);
         }
      }
   }

   /**
    * Exception class used to inform the user that the constructed object order criteria is invalid.
    */
   private class OpObjectOrderValidationException extends RuntimeException {
      private OpObjectOrderValidationException(String message) {
         super(message);
      }
   }
}
