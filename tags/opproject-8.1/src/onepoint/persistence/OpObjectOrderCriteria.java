/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * Class that represents a sort order criteria for an OpObject entity. It is used to allow the sorting of objects based
 * on the name of one of their properties.
 *
 * @author horia.chiorean
 */
public final class OpObjectOrderCriteria implements Comparator {

   /**
    * Constants that represend the sort order.
    */
   public static final Integer ASCENDING = 1;
   public static final Integer DESCENDING = -1;

   /**
    * A constant used for defining an empty order criteria.
    */
   // FIXME: not everything is derived from OpObject any longer...
   // public static final OpObjectOrderCriteria EMPTY_ORDER = new OpObjectOrderCriteria("OpObject", new HashMap());

   /**
    * The name of the object (entity) for which the sort order should apply.
    */
   private Class type = null;

   /**
    * A map of [propertyName, sortOrder] that contains the sorting criterias for the object.
    */
   private SortedMap<String, Integer> orderCriterias = new TreeMap<String, Integer>();

   /**
    * Creates a new sort order criteria for the given object name and a list of criterias.
    *
    * @param type     a <code>String</code> representing the name of the object(entity) for which to create the sort
    *                       order.
    * @param orderCriterias a <code>Map</code> of [String, String] representing [properyName, sortOder] pairs.
    */
   public OpObjectOrderCriteria(Class type, SortedMap<String, Integer> orderCriterias) {
      this.type = type;
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
   public OpObjectOrderCriteria(Class type, String propertyName, int sortOrder) {
      SortedMap<String, Integer> orderCriterias = new TreeMap<String, Integer>();
      orderCriterias.put(propertyName, sortOrder);
      this.type = type;
      this.orderCriterias = orderCriterias;
      validateOrderCriteria();
   }

   /**
    * Gets the name of the object of this order criteria.
    * @return a <code>String</code> representing the name of the object.
    */
   public String getObjectName() {
      return type.getSimpleName();
   }

   /**
    * Generates a Hibernate sort query string, using the optional given alias.
    * @param alias a <code>String</code> representing the name of an alias to use. If it is <code>null</code> then the
    * object name.
    * @return a <code>String</code> representing a sort query.
    */
   public String toHibernateQueryString(String alias) {
      if (type == null || orderCriterias == null || orderCriterias.size() == 0) {
         return "";
      }

      if (alias == null) {
         alias = type.getSimpleName();
      }
      StringBuffer result = new StringBuffer();
      result.append(" order by ");
      Iterator<Entry<String, Integer>> criteriasIterator = orderCriterias.entrySet().iterator();
      while (criteriasIterator.hasNext()) {
         Entry<String, Integer> entry = criteriasIterator.next();
         result.append(alias);
         result.append(".");
         result.append(entry.getKey());
         result.append(" ");
         result.append(entry.getValue() > 0 ? "asc" : "desc");
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
      if (type == null || orderCriterias == null || orderCriterias.size() == 0) {
            return "";
         }

         if (alias == null) {
            alias = type.getSimpleName();
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
      OpPrototype prototype = OpTypeManager.getPrototype(type.getSimpleName());
      if (prototype == null) {
         throw new OpObjectOrderValidationException("The object name " + type + " used in the sort order criteria has not been loaded by the type manager");
      }
      Iterator<Entry<String, Integer>> it = orderCriterias.entrySet().iterator();
      while (it.hasNext()) {
//         String propertyName = (String) it.next();
         Entry<String, Integer> entry = it.next();
         String propertyName = entry.getKey();
         if (propertyName == null || prototype.getMember(propertyName) == null) {
            throw new OpObjectOrderValidationException("Member " + propertyName + " not found for the " + type + " object");
         }
      }
   }
   /* (non-Javadoc)
    * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
    */
   public int compare(Object o1, Object o2) {
      try {
         for (Map.Entry<String, Integer> entry : orderCriterias.entrySet()) {
//          if (entry.getValue() > 0)
            Comparable c1 = (Comparable)o1.getClass().getMethod("get"+entry.getKey()).invoke(o1, new Object[0]);
            Comparable c2 = (Comparable)o2.getClass().getMethod("get"+entry.getKey()).invoke(o2, new Object[0]);
            int comp = c1.compareTo(c2);
            if (entry.getValue() < 0) {
               comp = -comp;
            }
            if (comp != 0) {
               return comp;
            }
         }
      }
      catch (IllegalArgumentException exc) {
         exc.printStackTrace();
      }
      catch (SecurityException exc) {
         exc.printStackTrace();
      }
      catch (IllegalAccessException exc) {
         exc.printStackTrace();
      }
      catch (InvocationTargetException exc) {
         exc.printStackTrace();
      }
      catch (NoSuchMethodException exc) {
         // TODO Auto-generated catch block
         exc.printStackTrace();
      }
      if ((o1 instanceof Comparable) && (o2 instanceof Comparable)) {
    	  return ((Comparable)o1).compareTo(o2);
      }
      return 0;
   }
}

/**
 * Exception class used to inform the user that the constructed object order criteria is invalid.
 */
class OpObjectOrderValidationException extends RuntimeException {
   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   OpObjectOrderValidationException(String message) {
      super(message);
   }
}

