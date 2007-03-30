/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

public class OpPrototypeFileHandler implements XNodeHandler {

   public final static String PROTOTYPE_FILE = "prototype-file";

   public final static String FILE_NAME = "file-name";

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpPrototypeFile prototype_file = new OpPrototypeFile();
      Object value = attributes.get(FILE_NAME);
      if ((value != null) && (value instanceof String)) {
         prototype_file.setFileName((String) value);
      }
      return prototype_file;
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {}

   public void addNodeContent(XContext context, Object node, String content) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}
