/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;

import java.io.InputStream;
import java.util.Iterator;

public class OpModuleRegistryLoader extends XLoader {

   private static XLog logger = XLogFactory.getLogger(OpModuleRegistryLoader.class,true);

   // Does not use resource loader: Might in the future contain licensing information

   public final static XSchema REGISTRY_SCHEMA = new OpRegistrySchema();

   public OpModuleRegistryLoader() {
      super(new XDocumentHandler(REGISTRY_SCHEMA));
      setUseResourceLoader(false);
   }

   public OpModuleRegistry loadModuleRegistry(InputStream input_stream) throws OpModuleException {
      OpModuleRegistry module_registry = (OpModuleRegistry) (loadObject(input_stream, null));
      _loadModules(module_registry);
      return module_registry;
   }

   public OpModuleRegistry loadModuleRegistry(String filename) throws OpModuleException {
      OpModuleRegistry module_registry = (OpModuleRegistry) (loadObject(filename, null));
      _loadModules(module_registry);
      return module_registry;
   }

   protected static void _loadModules(OpModuleRegistry module_registry) throws OpModuleException {
      logger.info("Loading registered modules...");
      Iterator module_files = module_registry.getModuleFiles();
      OpModuleFile module_file = null;
      while (module_files.hasNext()) {
         logger.info("...");
         module_file = (OpModuleFile) (module_files.next());
         module_registry.addModule(module_file.loadModule());
      }
      logger.info("Registered modules loaded.");

   }

}