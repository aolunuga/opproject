/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

import java.util.*;

public class OpModuleRegistry implements Iterable<OpModule> {

   /**
    * This class' logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpModuleRegistry.class);

   /**
    * List of module files
    */
   private List<OpModuleFile> moduleFiles;

   /**
    * Modules (module name,OpModule)
    */
   private Map<String, OpModule> modules;

   /**
    * Creates a new module registry instance.
    */
   public OpModuleRegistry() {
      moduleFiles = new ArrayList<OpModuleFile>();
      modules = new LinkedHashMap<String, OpModule>();
   }

   /**
    * Adds a module file to list of module files
    *
    * @param moduleFile <code>OpModuleFile</code>
    */
   final void addModuleFile(OpModuleFile moduleFile) {
      moduleFiles.add(moduleFile);
   }

   /**
    * Returns the module with the give the key <code>name</code> form the modules map.
    *
    * @param name <code>String</code> the key whose associated module is to be returned.
    * @return <code>OpModule</code> the value asociated for this key or <code>null</code> if no value with this key exists.
    */
   public final OpModule getModule(String name) {
      return modules.get(name);
   }

   /**
    * Returns an iterator over the modules map values (collection of OpModule entries).
    *
    * @return <code>Iterator</code>
    */
   public final Iterator<OpModule> iterator() {
      return modules.values().iterator();
   }

   /**
    * Gets the map of modules.
    * @return a <code>Map(String,OpModule)</code>.
    */
   public Map<String, OpModule> getModules() {
      return modules;
   }

   /**
    * Loads registered moduled from filenames.
    */
   public void loadModules() {
      logger.info("Loading registered modules...");
      Map<String, OpModule> modules = new LinkedHashMap<String, OpModule>();
      for (OpModuleFile moduleFile : moduleFiles) {
         OpModule module = moduleFile.loadModule();
         modules.put(module.getName(), module);
      }
      this.addModules(modules);
      logger.info("Registered modules loaded.");
   }

   /**
    * Adds the registered modules, taking into account their dependencies (dependent modules go after modules they depend)
    * @param unorderedModules a <code>Map(String, OpModule)</code> representing a potentially
    * unordered modules map (as loaded from the registry file).
    */
   private void addModules(Map<String, OpModule> unorderedModules) {
      //order the modules according to dependencies
      List<String> orderedModuleNames = new ArrayList<String>(unorderedModules.keySet());
      boolean needsSorting = true;
      while (needsSorting) {
         needsSorting = false;
         for (OpModule module : unorderedModules.values()) {
            int originalPosition = orderedModuleNames.indexOf(module.getName());
            int newPosition = originalPosition;
            Set<String> dependencies = module.getDependencies();
            for (String moduleName : dependencies) {
               OpModule otherModule = unorderedModules.get(moduleName);
               if (otherModule == null) {
                  throw  OpModuleDependencyException.createMissingDependecyException(moduleName, module.getName());
               }
               if (existsDependency(unorderedModules, otherModule, module)) {
                  throw  OpModuleDependencyException.createCyclincDependecyException(module.getName(), moduleName);
               }
               newPosition  = Math.max(newPosition, orderedModuleNames.indexOf(moduleName));
            }
            //if a swap is done, make sure we start all over again (because of transitive dependencies)
            if (newPosition != originalPosition) {
              Collections.swap(orderedModuleNames, originalPosition, newPosition);
              needsSorting = true;
            }
         }
      }

      //register the modules
      for (String moduleName: orderedModuleNames) {
         OpModule module = unorderedModules.get(moduleName);
         modules.put(moduleName, module);
      }
   }

   /**
    * Checks if there is a depdency between 2 given modules.
    *
    * @param allModules a <code>Map(String, OpModule)</code> representing all the loaded modules.
    * @param module1 an <code>OpModule</code> representing the 1st module.
    * @param module2 an <code>OpModule</code> representing the 2nd module.
    * @return <code>true</code> if there is a dependency between the 2 modules.
    */
   private boolean existsDependency(Map<String, OpModule> allModules, OpModule module1, OpModule module2) {
      if (module1 == module2) {
         return true;
      }
      Set<String> dependencies = module1.getDependencies();
      boolean areDependet = false;
      for (String moduleName : dependencies) {
         OpModule module = allModules.get(moduleName);
         areDependet |= existsDependency(allModules, module, module2);
         //break early
         if (areDependet) {
            return true;
         }
      }
      return areDependet;
   }

   /**
    * Registers all the modules stored in this registry.
    */
   void registerModules() {
      for (OpModule module : modules.values()) {
         module.register();
      }
   }
}

