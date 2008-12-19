/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.module;

import onepoint.xml.XNodeHandler;
import onepoint.xml.XContext;

import java.util.HashMap;

/**
 * This class handles language kit path nodes
 *
 * @author lucian.furtos
 */
public class OpLanguageKitPathHandler implements XNodeHandler {

   public static final String LANGUAGE_KIT_PATH = "language-kit-path";

   public final static String PATH = "path";

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpLanguageKitPath language_kit_path = new OpLanguageKitPath();
      Object value = attributes.get(PATH);
      if ((value != null) && (value instanceof String)) {
         language_kit_path.setPath((String) value);
      }
      return language_kit_path;
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   public void addNodeContent(XContext context, Object node, String content) {
   }

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
   }

}
