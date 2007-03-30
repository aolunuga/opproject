/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

public class OpServiceFileHandler implements XNodeHandler {

   public final static String SERVICE_FILE = "service-file";

   public final static String FILE_NAME = "file-name";

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpServiceFile service_file = new OpServiceFile();
      Object value = attributes.get(FILE_NAME);
      if ((value != null) && (value instanceof String)) {
         service_file.setFileName((String) value);
      }
      return service_file;
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {}

   public void addNodeContent(XContext context, Object node, String content) {}

   public void nodeFinished(XContext context, String name, Object node, Object parent) {}

}
