/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.ArrayList;
import java.util.HashMap;

public class OpModuleHandler implements XNodeHandler {

   private static XLog logger = XLogFactory.getLogger(OpModuleHandler.class, true);

   public final static String MODULE = "module";
   public final static String CLASS = "class";
   public final static String NAME = "name";
   public final static String VERSION = "version";
   public final static String CAPTION = "caption";
   public final static String EXTENDS = "extends";

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpModule module = null;
      Object value = attributes.get(CLASS);
      if ((value != null) && (value instanceof String)) {
         try {
            module = (OpModule) (Class.forName((String) value).newInstance());
         }
         catch (Exception e) {
            logger.error("OpModuleHandler.newNode", e);
         }
         // *** More error handling needed (instanceof OpModule etc.)
      }
      value = attributes.get(NAME);
      if ((value != null) && (value instanceof String)) {
         module.setName((String) value);
      }
      value = attributes.get(VERSION);
      if ((value != null) && (value instanceof String)) {
         module.setVersion((String) value);
      }
      value = attributes.get(CAPTION);
      if ((value != null) && (value instanceof String)) {
         module.setCaption((String) value);
      }
      value = attributes.get(EXTENDS);
      if ((value != null) && (value instanceof String)) {
         module.setExtendedModule((String) value);
      }
      return module;
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
      if (child_name == OpPrototypeFilesHandler.PROTOTYPE_FILES)
         ((OpModule) node).setPrototypeFiles((ArrayList) child);
      else if (child_name == OpServiceFilesHandler.SERVICE_FILES)
         ((OpModule) node).setServiceFiles((ArrayList) child);
      else if (child_name == OpLanguageKitFilesHandler.LANGUAGE_KIT_FILES)
         ((OpModule) node).setLanguageKitFiles((ArrayList) child);
      else if (child_name == OpToolsHandler.TOOLS)
         ((OpModule) node).setTools((ArrayList) child);
      else if (child_name == OpToolGroupsHandler.TOOL_GROUPS)
         ((OpModule) node).setGroups((ArrayList) child);
   }

   public void addNodeContent(XContext context, Object node, String content) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}
