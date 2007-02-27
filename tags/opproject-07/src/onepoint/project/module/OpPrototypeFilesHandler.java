/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.ArrayList;
import java.util.HashMap;

public class OpPrototypeFilesHandler implements XNodeHandler {

   public final static String PROTOTYPE_FILES = "prototype-files";

   public Object newNode(XContext context, String name, HashMap attributes) {
      return new ArrayList();
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
      OpPrototypeFile prototype_file = (OpPrototypeFile) child;
      ((ArrayList) node).add(prototype_file);
   }

   public void addNodeContent(XContext context, Object node, String content) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}
