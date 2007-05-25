/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class OpToolsHandler implements XNodeHandler {

   public final static String TOOLS = "tools".intern();

   public Object newNode(XContext context, String name, HashMap attributes) {
      return new ArrayList();
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
      OpTool tool = (OpTool) child;
      ((ArrayList) node).add(tool);
   }

   public void addNodeContent(XContext context, Object node, String content) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      // Set back-references to modules for all tools
      Iterator tools = ((ArrayList) node).iterator();
      OpTool tool = null;
      while (tools.hasNext()) {
         tool = (OpTool) tools.next();
         tool.setModule((OpModule) parent);
      }
   }

}
