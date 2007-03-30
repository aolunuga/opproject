/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.project.util.OpProjectConstants;

import java.util.Iterator;

public class OpModuleFile {

   private String _file_name;

   public final void setFileName(String file_name) {
      _file_name = file_name;
   }

   public final OpModule loadModule() {
      String module_file_name = OpProjectConstants.PROJECT_PACKAGE + _file_name;
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
         String extendedModulePath = module.getExtendedModule();
         extendedModulePath =  OpProjectConstants.PROJECT_PACKAGE + extendedModulePath;
         OpModule extendedModule = moduleLoader.loadModule(extendedModulePath);
         extendModule(extendedModule, module);
         startModule = extendedModule;
      }
   }

   /**
    * Performs the actual "module extension" operation between 2 modules, by taking attributes from the parent module and adding them
    * to the child module.
    * @param parent an <code>OpModule</code> that represents the base module (that is extended).
    * @param child an <code>OpModule</code> that represents the sub-module (that extends).
    */
   private void extendModule(OpModule parent, OpModule child) {
      //simply take all the prototypes, language kits and service files from the parent.
      child.getPrototypesList().addAll(0, parent.getPrototypesList());
      child.getLanguageKitsList().addAll(0, parent.getLanguageKitsList());
      child.getServicesList().addAll(0, parent.getServicesList());

      //override tools and groups
      Iterator it = parent.getTools();
      while (it.hasNext()) {
         OpTool parentTool = (OpTool) it.next();
         if (!(child.getToolsMap().containsKey(parentTool.getName()))) {
            child.getToolsList().add(parentTool);
         }
      }

      it = parent.getGroups();
      while (it.hasNext()) {
         OpToolGroup parentToolGroup = (OpToolGroup) it.next();
         if (!(child.getGroupsMap().containsKey(parentToolGroup.getName()))) {
            child.getGroupsList().add(parentToolGroup);
         }
      }
   }
}