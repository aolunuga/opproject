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

public class OpModuleLoader extends XLoader {

   private static final XLog logger = XLogFactory.getServerLogger(OpModuleLoader.class);

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
      module.loadParts();
      return module;
   }
}