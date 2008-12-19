/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import java.io.InputStream;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;

public class OpModuleLoader extends XLoader {

   private static final XLog logger = XLogFactory.getLogger(OpModuleLoader.class);

   public final static XSchema MODULE_SCHEMA = new OpModuleSchema();

   public OpModuleLoader() {
      super(new XDocumentHandler(MODULE_SCHEMA));
   }

   public OpModule loadModule(InputStream input_stream) {
      OpModule module = (OpModule) (loadObject(input_stream, null));
      module.loadParts();
      return module;
   }

   public OpModule loadModule(String moduleFileName) {
      logger.info("Loading module: " + moduleFileName);
      OpModule module = (OpModule) (loadObject(null, moduleFileName, null));
      if (module == null) {
         logger.error("module not found: "+moduleFileName);
      }
      module.loadParts();
      return module;
   }
}