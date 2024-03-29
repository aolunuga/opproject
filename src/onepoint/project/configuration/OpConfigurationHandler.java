/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.configuration;

import java.util.HashMap;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

public class OpConfigurationHandler implements XNodeHandler {

   public Object newNode(XContext context, String name, HashMap attributes) {
      return new OpConfiguration();
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   public void addNodeContent(XContext context, Object node, String content) {
   }

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
   }

}
