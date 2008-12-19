/**
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.persistence.hibernate;

import java.util.HashMap;
import java.util.Map;

/**
 * This class defines a Hibernate filter wich will be defined at the moment when Hibernate mappings content is generated.
 *
 * @author calin.pavel
 */
public class OpHibernateFilter {
   private String name;
   private String condition;
   private Map<String, String> parameters;

   /**
    * Creates a new filter
    *
    * @param name       filter name
    * @param condition  filtering condition
    * @param parameters filter parameters (name->type)
    */
   public OpHibernateFilter(String name, String condition, Map<String, String> parameters) {
      if (name == null) {
         throw new NullPointerException("Filter name must not be NULL");
      }
      else {
         this.name = name;
      }

      if (condition == null) {
         throw new NullPointerException("Filter condition must not be NULL");
      }
      else {
         this.condition = condition;
      }

      this.parameters = parameters;
   }

   /**
    * Creates a new filter
    *
    * @param name      filter name
    * @param condition filtering condition
    */
   public OpHibernateFilter(String name, String condition) {
      this(name, condition, new HashMap<String, String>());
   }

   /**
    * Returns filter name.
    *
    * @return filter name
    */
   public String getName() {
      return name;
   }

   /**
    * Returns filter condition
    *
    * @return filter condition
    */
   public String getCondition() {
      return condition;
   }

   /**
    * Returns filter parameters.
    *
    * @return map of filter parameters (name->type).
    */
   public Map<String, String> getParameters() {
      return parameters;
   }
}
