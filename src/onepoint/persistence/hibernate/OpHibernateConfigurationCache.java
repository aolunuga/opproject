/**
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.persistence.hibernate;

import org.hibernate.cfg.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * This class function as a cache for Hibernate configurations.
 *
 * @author calin.pavel
 */
public class OpHibernateConfigurationCache {
   private Map<String, Configuration> configurations = new HashMap<String, Configuration>();
   private static OpHibernateConfigurationCache instance;

   /**
    * Returns a singleton instance of <code>OpHibernateConfigurationCache</code>.
    *
    * @return instance
    */
   public static OpHibernateConfigurationCache getInstance() {
      if (instance == null) {
         instance = new OpHibernateConfigurationCache();
      }

      return instance;
   }

   /**
    * Add configurations to cache.
    *
    * @param configurationName name/key for the configuration to be added.
    * @param configuration     configuration to add.
    */
   public void addConfiguration(String configurationName, Configuration configuration) {
      configurations.put(configurationName, configuration);
   }

   /**
    * Returns configuration mapped with a given name or NULL if none can be found.
    *
    * @param configurationName configuration name
    * @return configuration mapped to that name or NULL if none can be found.
    */
   public Configuration getConfiguration(String configurationName) {
      return configurations.get(configurationName);
   }
}
