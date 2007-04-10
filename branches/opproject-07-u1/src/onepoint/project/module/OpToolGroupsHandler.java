/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class OpToolGroupsHandler implements XNodeHandler {

	public final static String TOOL_GROUPS = "tool-groups";

	public Object newNode(XContext context, String name, HashMap attributes) {
		return new ArrayList();
	}

	public void addChildNode(XContext context, Object node, String child_name, Object child) {
		OpToolGroup group = (OpToolGroup)child;
		((ArrayList)node).add(group);
	}

	public void addNodeContent(XContext context, Object node, String content) {}

	public void nodeFinished(XContext context, String name, Object node, Object parent) {
      // Set back-references to modules for all tools
      Iterator groups = ((ArrayList) node).iterator();
      OpToolGroup group = null;
      while (groups.hasNext()) {
         group = (OpToolGroup) groups.next();
         group.setModule((OpModule)parent);
      }
   }

}
