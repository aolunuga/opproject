/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.module;

import onepoint.xml.XSchema;

/**
 * @author gerald
 */
public class OpModuleSchema extends XSchema {

   public OpModuleSchema() {
      registerNodeHandler(OpModuleHandler.MODULE, new OpModuleHandler());
      registerNodeHandler(OpPrototypeFilesHandler.PROTOTYPE_FILES, new OpPrototypeFilesHandler());
      registerNodeHandler(OpPrototypeFileHandler.PROTOTYPE_FILE, new OpPrototypeFileHandler());
      registerNodeHandler(OpToolGroupsHandler.TOOL_GROUPS, new OpToolGroupsHandler());
      registerNodeHandler(OpToolGroupHandler.TOOL_GROUP, new OpToolGroupHandler());
      registerNodeHandler(OpToolsHandler.TOOLS, new OpToolsHandler());
      registerNodeHandler(OpToolHandler.TOOL, new OpToolHandler());
      registerNodeHandler(OpServiceFilesHandler.SERVICE_FILES, new OpServiceFilesHandler());
      registerNodeHandler(OpServiceFileHandler.SERVICE_FILE, new OpServiceFileHandler());
      registerNodeHandler(OpLanguageKitPathsHandler.LANGUAGE_KIT_PATHS, new OpLanguageKitPathsHandler());
      registerNodeHandler(OpLanguageKitPathHandler.LANGUAGE_KIT_PATH, new OpLanguageKitPathHandler());
   }

}