/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.configuration;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

/**
 * Class that handles client launcher configuration nodes
 *
 * @author ovidiu.lupas
 */
public class OpClientLauncherHandler implements XNodeHandler {

   /**
    * Constants that represent configuration nodes .
    */
   public final static String REMOTE_CONFIGURATION = "remote-configuration";
   public final static String HOST = "host";
   public final static String PORT = "port";
   public final static String PATH = "path";
   public final static String SECURE = "secure";

   /**
    * @see XNodeHandler#newNode(onepoint.xml.XContext, String, java.util.HashMap)
    */
   public Object newNode(XContext context, String name, HashMap attributes) {
      if (name.equals(REMOTE_CONFIGURATION)) {
         return OpClientLauncherConfiguration.getInstance();
      }
      return name;
   }

   /**
    * @see XNodeHandler#addNodeContent(onepoint.xml.XContext, Object, String)
    */
   public void addNodeContent(XContext context, Object node, String content) {
      context.setVariable(node.toString(), content);
   }

   /**
    * @see XNodeHandler#addChildNode(onepoint.xml.XContext, Object, String, Object)
    */
   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext, String, Object, Object)
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      if (name.equals(HOST)) {
         ((OpClientLauncherConfiguration) parent).setHost((String) context.getVariable(name));
      }
      if (name.equals(PORT)) {
         int port = Integer.parseInt((String) context.getVariable(name));
         ((OpClientLauncherConfiguration) parent).setPort(port);
      }
      if (name.equals(PATH)) {
         ((OpClientLauncherConfiguration) parent).setPath((String) context.getVariable(name));
      }
      if (name.equals(SECURE)) {
         String secureProtocol = (String) context.getVariable(name);
         ((OpClientLauncherConfiguration) parent).setSecure(Boolean.valueOf(secureProtocol).booleanValue());
      }
   }
}
