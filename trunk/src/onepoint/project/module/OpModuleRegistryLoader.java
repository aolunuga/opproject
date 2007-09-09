/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;

import java.io.File;
import java.io.InputStream;

public class OpModuleRegistryLoader extends XLoader {

   public final static XSchema REGISTRY_SCHEMA = new OpRegistrySchema();
   private static final String MODULE_RESOURCE_PATH = "onepoint/project/";
   
   public OpModuleRegistryLoader() {
      super(new XDocumentHandler(REGISTRY_SCHEMA));
      setUseResourceLoader(false);
   }

   public OpModuleRegistry loadModuleRegistry(InputStream input_stream) {
      OpModuleRegistry moduleRegistry = (OpModuleRegistry) (loadObject(input_stream, null));
      moduleRegistry.loadModules();
      return moduleRegistry;
   }

   public OpModuleRegistry loadModuleRegistry(File path, String filename) {
      OpModuleRegistry moduleRegistry = (OpModuleRegistry) (loadObject(path, MODULE_RESOURCE_PATH, filename, null));
      moduleRegistry.loadModules();
      return moduleRegistry;
   }
}