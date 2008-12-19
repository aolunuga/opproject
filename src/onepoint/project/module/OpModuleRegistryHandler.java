/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

public class OpModuleRegistryHandler implements XNodeHandler {

   public final static String MODULE_REGISTRY = "module-registry";

   public Object newNode(XContext context, String name, HashMap attributes) {
      return new OpModuleRegistry();
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
      ((OpModuleRegistry) node).addModuleFile((OpModuleFile) child);
   }

   public void addNodeContent(XContext context, Object node, String content) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}
