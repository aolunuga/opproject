/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XSchema;

/**
 * @author gerald
 */
public class OpRegistrySchema extends XSchema {

   public OpRegistrySchema() {
      registerNodeHandler(OpModuleRegistryHandler.MODULE_REGISTRY, new OpModuleRegistryHandler());
      registerNodeHandler(OpModuleFileHandler.MODULE_FILE, new OpModuleFileHandler());
   }

}