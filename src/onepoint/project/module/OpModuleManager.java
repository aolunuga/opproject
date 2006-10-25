/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.persistence.OpObject;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.resource.XLanguageKit;
import onepoint.resource.XLocaleManager;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceManager;
import onepoint.util.XEnvironment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class OpModuleManager {

   public final static String MODULE_REGISTRY_FILE_NAME = "registry.oxr.xml";

   private static OpModuleRegistry _module_registry;
   private static OpModuleRegistryLoader opModuleRegistryLoader;

   /**
    * Sets the ModuleRegistryLoader to be used for loading the modules
    *
    * @param moduleLoader
    */
   public static void setModuleRegistryLoader(OpModuleRegistryLoader moduleLoader) {
      opModuleRegistryLoader = moduleLoader;
   }

   public static void load()
        throws OpModuleException {
      // Read module registry
      // *** Exception/error if module-registry has already been read?
      String path = XEnvironment.getVariable(onepoint.project.configuration.OpConfiguration.ONEPOINT_HOME) + "/" + MODULE_REGISTRY_FILE_NAME;
      if (opModuleRegistryLoader == null) {
         opModuleRegistryLoader = new OpModuleRegistryLoader();
      }
      _module_registry = opModuleRegistryLoader.loadModuleRegistry(path);
      if (_module_registry == null) {
         // We assume that this is a newly installed system
         _module_registry = new OpModuleRegistry();
         // *** Add built-in modules here or hard-code completely?
      }
      // Register prototypes and tools for all modules
      Iterator modules = _module_registry.getModules();
      OpModule module = null;
      Iterator prototypes = null;
      OpPrototype prototype = null;
      Iterator services = null;
      XService service = null;
      Iterator language_kits = null;
      XLanguageKit language_kit = null;
      Iterator tools = null;
      OpTool tool = null;
      Iterator groups = null;
      OpToolGroup group = null;
      while (modules.hasNext()) {
         module = (OpModule) (modules.next());
         // Register prototypes
         prototypes = module.getPrototypes();
         while (prototypes.hasNext()) {
            prototype = (OpPrototype) (prototypes.next());
            OpTypeManager.registerPrototype(prototype);
         }
         // Register services
         services = module.getServices();
         while (services.hasNext()) {
            service = (XService) (services.next());
            if (module.getExtendedModule() != null) {
               XServiceManager.registerOverriddingService(service);
            }
            else {
               XServiceManager.registerService(service);
            }
         }
         // Register language kits
         language_kits = module.getLanguageKits();
         while (language_kits.hasNext()) {
            language_kit = (XLanguageKit) (language_kits.next());
            if (module.getExtendedModule() != null) {
               XLocaleManager.registerOverriddingLanguageKit(language_kit, false);
            }
            else {
               XLocaleManager.registerLanguageKit(language_kit);
            }
         }
         // Add tool-groups
         groups = module.getGroups();
         while (groups.hasNext()) {
            group = (OpToolGroup) (groups.next());
            OpToolManager.registerGroup(group);
         }
         // Add tools
         tools = module.getTools();
         while (tools.hasNext()) {
            tool = (OpTool) (tools.next());
            // Resolve group-ref
            if (tool.getGroupRef() != null) {
               tool.setGroup(OpToolManager.getGroup(tool.getGroupRef()));
            }
            OpToolManager.registerTool(tool);
         }
      }
      initializeBackupManager();
      OpTypeManager.lock();
      // Standard installation: Create prototypes
      // *** Dependencies should be handled by programmer (installation-order in
      // module.oxm)
      // ==> Do we need an extra removal-order as well (or reverse installation
      // order)?
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
      Iterator it = dependencies.iterator();
      while (it.hasNext()) {
         OpPrototype dependency = (OpPrototype) it.next();
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

   public static void setup() {
      // Invoke setup callbacks (for setting up a new instance)
      OpProjectSession setupSession = new OpProjectSession();
      Iterator modules = _module_registry.getModules();
      while (modules.hasNext()) {
         OpModule module = (OpModule) (modules.next());
         module.setup(setupSession);
      }
      setupSession.close();
   }

   public static void start() {
      // Invoke start callbacks
      OpProjectSession startupSession = new OpProjectSession();
      Iterator modules = _module_registry.getModules();
      while (modules.hasNext()) {
         OpModule module = (OpModule) (modules.next());
         module.start(startupSession);
      }
      startupSession.close();
   }

   public static void stop() {
      // *** Write module-registry?
      OpProjectSession shutdownSession = new OpProjectSession();
      Iterator modules = _module_registry.getModules();
      while (modules.hasNext()) {
         OpModule module = (OpModule) (modules.next());
         module.stop(shutdownSession);
      }
      shutdownSession.close();
   }

   /**
    * Calls the upgrade method for all the registered modules. This usually occurs when a db schema update takes place.
    *
    * @param dbVersion
    * @see OpModule#upgrade(onepoint.project.OpProjectSession,int)
    */
   public static void upgrade(int dbVersion) {
      OpProjectSession session = new OpProjectSession();
      Iterator modules = _module_registry.getModules();
      while (modules.hasNext()) {
         OpModule module = (OpModule) (modules.next());
         module.upgrade(session, dbVersion);
      }
      session.close();
   }

   // *** Maybe these two should already go into an XModuleService?

   public static void installModule(OpProjectSession session, String module_name) {
      // Register new prototypes and re-lock type manager
      // *** We could maybe work with "run-levels" for locking/unlocking etc.
      OpModule module = _module_registry.getModule(module_name);
      if (module != null) {
         Iterator prototypes = module.getPrototypes();
         OpPrototype prototype = null;
         /*
          * while (prototypes.hasNext()) { prototype = (OpPrototype)
          * (prototypes.next()); OpTypeManager.registerPrototype(prototype); }
          * OpTypeManager.lock();
          */
         // *** ATTENTION: The following is not possible anymore
         // ==> We can only create the whole schema
         // *** Maybe we could provide an "alterSchema()" method for this?
         /*
          * // Create prototypes in database OpBroker broker =
          * session.getBroker(); while (prototypes.hasNext()) { prototype =
          * (OpPrototype) (prototypes.next());
          * broker.createPrototype(prototype.getName()); }
          */
      }
   }

   public static OpModuleRegistry getModuleRegistry() {
      return _module_registry;
   }
}