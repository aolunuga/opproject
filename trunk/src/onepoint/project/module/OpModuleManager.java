/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpSource;
import onepoint.persistence.OpSourceManager;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.util.OpEnvironmentManager;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

public final class OpModuleManager {

   public final static String MODULE_REGISTRY_FILE_NAME = "registry.oxr.xml";

   /**
    * This class' logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpModule.class);

   private static OpModuleRegistry moduleRegistry;
   private static OpModuleRegistryLoader opModuleRegistryLoader;

   /**
    * Sets the ModuleRegistryLoader to be used for loading the modules
    *
    * @param moduleLoader module loader
    */
   public static void setModuleRegistryLoader(OpModuleRegistryLoader moduleLoader) {
      opModuleRegistryLoader = moduleLoader;
   }

   /**
    * Read modules registry.
    */
   public static void load() {
      load(MODULE_REGISTRY_FILE_NAME);
   }

   /**
    * Read modules registry.
    *
    * @param registryFileName name of the module registry file.
    */
   public static void load(String registryFileName) {
      if (opModuleRegistryLoader == null) {
         opModuleRegistryLoader = new OpModuleRegistryLoader();
      }
      File opHome = new File(OpEnvironmentManager.getOnePointHome());
      moduleRegistry = opModuleRegistryLoader.loadModuleRegistry(opHome, registryFileName);
      if (moduleRegistry == null) {
         logger.error("The module registry wasn't initialized. No modules will be loaded.");
         return;
      }
      moduleRegistry.registerModules();
      OpBackupManager.getBackupManager().initializeBackupManager();
      //<FIXME author="Horia Chiorean" description="Is this needed ?">
      OpTypeManager.lock();
      //<FIXME>
   }


   public static void start() {
      Collection<OpSource> allSources = OpSourceManager.getAllSources();
      for (OpSource source : allSources) {
         // Invoke start callbacks
         OpProjectSession startupSession = new OpProjectSession(source.getName());
         Iterator<OpModule> modulesIt = moduleRegistry.iterator();
         while (modulesIt.hasNext()) {
            OpModule module = modulesIt.next();
            logger.info("Loading module " + module.getName());
            module.start(startupSession);
         }
         startupSession.close();
      }
   }

   public static void stop() {
      Collection<OpSource> allSources = OpSourceManager.getAllSources();
      for (OpSource source : allSources) {
         // *** Write module-registry?
         OpProjectSession shutdownSession = new OpProjectSession(source.getName());
         Iterator<OpModule> modulesIt = moduleRegistry.iterator();
         while (modulesIt.hasNext()) {
            OpModule module = modulesIt.next();
            module.stop(shutdownSession);
         }
         shutdownSession.close();
      }
   }

   /**
    * Calls the upgrade method for all the registered modules. This usually occurs when a db schema update takes place.
    *
    * @param dbVersion     database version (old one).
    * @param latestVersion an <code>int</code> representing the latest version.
    */
   public static void upgrade(int dbVersion, int latestVersion) {
      Collection<OpSource> allSources = OpSourceManager.getAllSources();
      for (OpSource source : allSources) {
         OpProjectSession session = new OpProjectSession(source.getName());
         session.loadSettings();
         Iterator<OpModule> modulesIt = moduleRegistry.iterator();
         while (modulesIt.hasNext()) {
            OpModule module = modulesIt.next();
            for (int i = dbVersion + 1; i <= latestVersion; i++) {
               String methodName = "upgradeToVersion" + i;
               try {
                  Method m = module.getClass().getMethod(methodName, OpProjectSession.class);
                  logger.info("Invoking " + methodName + " for module " + module.getName());
                  m.invoke(module, session);
               }
               catch (NoSuchMethodException e) {
                  logger.debug("No upgrade method " + methodName + " found for module " + module.getName());
               }
               catch (IllegalAccessException e) {
                  logger.debug("Cannot access upgrade method ", e);
               }
               catch (InvocationTargetException e) {
                  logger.error("Cannot invoke upgrade method " + methodName + " for module " + module.getName(), e);
                  //allow exceptions thrown by upgrade methods to be handled by someone else as well
                  throw new RuntimeException(e.getCause());
               }
            }
         }
         session.close();
      }
   }


   /**
    * Checks the integrity of the modules and fixes the possible module errors.
    */
   public static void checkModules() {
      long currentTime = System.currentTimeMillis();
      Collection<OpSource> allSources = OpSourceManager.getAllSources();
      for (OpSource source : allSources) {
         OpProjectSession session = new OpProjectSession(source.getName());
         session.loadSettings();
         Iterator<OpModule> modulesIt = moduleRegistry.iterator();
         while (modulesIt.hasNext()) {
            OpModule module = modulesIt.next();
            module.check(session);
         }
         session.close();
      }
      logger.info("Total checking time: " + (System.currentTimeMillis() - currentTime) / 1000 + " sec");
   }

   public static OpModuleRegistry getModuleRegistry() {
      return moduleRegistry;
   }
}