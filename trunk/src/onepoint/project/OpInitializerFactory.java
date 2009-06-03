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
   private static final XLog logger = XLogFactory.getLogger(OpInitializerFactory.class);
   private static final Object MUTEX = new Object();

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
   public OpInitializer setInitializer(Class initializerClass) {
      if (initializerClass == null) {
         throw new NullPointerException("Could not create initializer, because provided class is NULL");
      }
      if (initializer != null) {
         if (initializer.getClass() == initializerClass) {
            return initializer;
         }
         // FIXME (dfreis, May 29, 2009) : do what?
         throw new IllegalStateException("initializer already set, cannot be changed to a different type!");
      }
      try {
         initializer = (OpInitializer) initializerClass.newInstance();
         return initializer;
      }
      catch (InstantiationException e) {
         logger.error("OpInitiliazer instantiation failed.", e);
         return null;
      }
      catch (IllegalAccessException e) {
         logger.error("IllegalAccess during OpInitiliazer instantiation.", e);
         return null;
      }
   }
   
   /**
    * Return product initializer.
    * @param productCode 
    *
    * @return an instance of product initializer.
    */
   public OpInitializer getInitializer() {
      if (initializer == null) {
         throw new NullPointerException("Could not create initializer, because provided class is NULL");
      }
      return initializer;
   }
}
