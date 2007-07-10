/**
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

/**
 * This class is resposiblem for creation and management of <code>OpInitializer</code> instances.
 *
 * @author calin.pavel
 */
public class OpInitializerFactory {
   // This class's logger
   private static final XLog logger = XLogFactory.getServerLogger(OpInitializerFactory.class);

   // initializer factory holder (solves synchronization and double-checked-locking idiom issue)
   private static class OpInitializerFactoryHolder {
      static OpInitializerFactory initializerFactory = new OpInitializerFactory();
   }
   
   // Products initializer.
   private OpInitializer initializer;

   /**
    * Creates a new instance.
    */
   private OpInitializerFactory() {
   }

   /**
    * Returns a single instance of OpInitializerFactory
    *
    * @return class instance
    */
   public static OpInitializerFactory getInstance() {
     return OpInitializerFactoryHolder.initializerFactory;
   }

   /**
    * Defines class that will be used as product initializer.
    *
    * @param initializerClass initializer class.
    */
   public void setInitializer(Class initializerClass) {
      if (initializerClass == null) {
         throw new NullPointerException("Could not create initializer, because provided class is NULL");
      }

      try {
         initializer = (OpInitializer) initializerClass.newInstance();
      }
      catch (InstantiationException e) {
         logger.error("OpInitiliazer instantiation failed.", e);
      }
      catch (IllegalAccessException e) {
         logger.error("IllegalAccess during OpInitiliazer instantiation.", e);
      }
   }

   /**
    * Return product initializer.
    *
    * @return an instance of product initializer.
    */
   public OpInitializer getInitializer() {
      if (initializer == null) {
         throw new RuntimeException("Initializer was not defined yet. Please use setInitializer() method first.");
      }

      return initializer;
   }

}
