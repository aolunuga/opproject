/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.ArrayList;
import java.util.HashMap;

public class OpLanguageKitFilesHandler implements XNodeHandler {

   public final static String LANGUAGE_KIT_FILES = "language-kit-files";

   public Object newNode(XContext context, String name, HashMap attributes) {
      return new ArrayList();
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
      ((ArrayList) node).add(child);
   }

   public void addNodeContent(XContext context, Object node, String content) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}
