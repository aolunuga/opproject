/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class OpModuleLoader extends XLoader {

   private static final XLog logger = XLogFactory.getServerLogger(OpModuleLoader.class);

   public final static XSchema MODULE_SCHEMA = new OpModuleSchema();

   public OpModuleLoader() {
      super(new XDocumentHandler(MODULE_SCHEMA));
   }

   public OpModule loadModule(InputStream input_stream) {
      OpModule module = (OpModule) (loadObject(input_stream, null));
      _loadModuleParts(module);
      return module;
   }

   public OpModule loadModule(String module_file_name) {
      logger.info("Loading module: " + module_file_name);
      OpModule module = (OpModule) (loadObject(module_file_name, null));
      _loadModuleParts(module);
      return module;
   }

   protected static void _loadModuleParts(OpModule module) {
      // Load prototypes
      logger.info("Loading prototypes of module '" + module.getName() + "'...");
      Iterator prototype_files = module.getPrototypeFiles();
      ArrayList prototypes = new ArrayList();
      OpPrototypeFile prototype_file = null;
      while (prototype_files.hasNext()) {
         prototype_file = (OpPrototypeFile) (prototype_files.next());
         prototypes.add(prototype_file.loadPrototype());
      }
      module.setPrototypes(prototypes);
      logger.info("Prototypes of module '" + module.getName() + "' loaded.");
      // Load services
      logger.info("Loading services of module '" + module.getName() + "'...");
      Iterator service_files = module.getServiceFiles();
      ArrayList services = new ArrayList();
      OpServiceFile service_file = null;
      while (service_files.hasNext()) {
         service_file = (OpServiceFile) (service_files.next());
         services.add(service_file.loadService());
      }
      module.setServices(services);
      logger.info("Services of module '" + module.getName() + "' loaded.");
      // Load language-kits
      logger.info("Loading language-kits of module '" + module.getName() + "'...");
      Iterator language_kit_paths = module.getLanguageKitPaths();
      ArrayList language_kits = new ArrayList();
      OpLanguageKitPath language_kit_path = null;
      while (language_kit_paths.hasNext()) {
         language_kit_path = (OpLanguageKitPath) (language_kit_paths.next());
         language_kits.addAll(language_kit_path.loadLanguageKits());
      }
      module.setLanguageKits(language_kits);
      logger.info("Language-kits of module '" + module.getName() + "' loaded.");
   }

}