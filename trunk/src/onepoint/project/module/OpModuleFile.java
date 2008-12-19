/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.project.util.OpProjectConstants;

public class OpModuleFile {

   private String fileName;

   public final void setFileName(String file_name) {
      this.fileName = file_name;
   }

   public final OpModule loadModule() {
      String module_file_name = OpProjectConstants.PROJECT_PACKAGE + fileName;
      OpModuleLoader opModuleLoader = new OpModuleLoader();
      OpModule module = opModuleLoader.loadModule(module_file_name);
      if (module.getExtendedModule() != null) {
         loadExtendedModules(module, opModuleLoader);
      }
      return module;
   }

   /**
    * Tries to load a hierarchy of modules, starting with the given module.
    * @param module a <code>OpModule</code> representing the start module.
    * @param moduleLoader a <code>OpModuleLoader</code> that will load each parent module.
    * 
    */
   private void loadExtendedModules(OpModule module, OpModuleLoader moduleLoader) {
      OpModule startModule = module;
      while (startModule.getExtendedModule() != null) {
         String extendedModulePath = startModule.getExtendedModule();
         extendedModulePath =  OpProjectConstants.PROJECT_PACKAGE + extendedModulePath;
         OpModule parentModule = moduleLoader.loadModule(extendedModulePath);
         module.extend(parentModule);
         startModule = parentModule;
      }
   }
}