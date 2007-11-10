/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.util.OpEnvironmentManager;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
      initializeBackupManager();
      //<FIXME author="Horia Chiorean" description="Is this needed ?">
      OpTypeManager.lock();
      //<FIXME>
   }


   /**
    * Initializes the backup manager by registering all the prototypes in correct order.
    */
   private static void initializeBackupManager() {
      List toAddLast = new ArrayList();
      OpPrototype superPrototype = OpTypeManager.getPrototypeByClassName(OpObject.class.getName());
      Iterator it = OpTypeManager.getPrototypes();
      while (it.hasNext()) {
         OpPrototype startPoint = (OpPrototype) it.next();
         if (!(startPoint.getID() == superPrototype.getID())) {
            registerPrototypeForBackup(superPrototype, startPoint, toAddLast);
         }
      }
      it = toAddLast.iterator();
      while (it.hasNext()) {
         OpBackupManager.addPrototype((OpPrototype) it.next());
      }
   }

   /**
    * Registers a prototype with the backup manager, taking into account the prototype's dependencies.
    *
    * @param superPrototype           a <code>OpPrototype</code> representing OpObject's prototype.
    * @param startPoint               a <code>OpPrototype</code> representing a start point in the back-up registration process.
    * @param lastPrototypesToRegister a <code>List</code> which acts as an acumulator and will contain at the end a list
    *                                 of prototypes which will be registered at the end of all the others.
    */
   private static void registerPrototypeForBackup(OpPrototype superPrototype, OpPrototype startPoint, List lastPrototypesToRegister) {
      List dependencies = startPoint.getBackupDependencies();
      for (Object dependency1 : dependencies) {
         OpPrototype dependency = (OpPrototype) dependency1;
         if (dependency.getID() == superPrototype.getID()) {
            lastPrototypesToRegister.add(startPoint);
         }
         else if (!OpBackupManager.hasRegistered(dependency)) {
            registerPrototypeForBackup(superPrototype, dependency, lastPrototypesToRegister);
         }
      }
      if (!startPoint.subTypes().hasNext() && !OpBackupManager.hasRegistered(startPoint)) {
         OpBackupManager.addPrototype(startPoint);
      }
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
   }


   public static OpModuleRegistry getModuleRegistry() {
      return moduleRegistry;
   }
}