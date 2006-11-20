/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.applet;

import onepoint.express.XComponent;
import onepoint.express.applet.XExpressApplet;
import onepoint.project.modules.project_planning.components.OpProjectComponentProxy;
import onepoint.project.util.OpProjectConstants;

import java.awt.*;
import java.util.HashMap;

/**
 * Applet used for the expander application.
 */
public class OpProjectApplet extends XExpressApplet {

   /**
    * Various applet constants.
    */
   private final static String APPLET_LOADED = "OnePoint Applet already loaded in another browser window. Please close it and try again.";
   private final static String WAR_NAME = "opproject";

   /**
    * Registers project proxies.
    */
   static {
      XComponent.registerProxy(new OpProjectComponentProxy());
   }

   /**
    * @see onepoint.express.applet.XExpressApplet#getAppletPath()
    */
   protected String getAppletPath() {
      String path = super.getAppletPath();
      if (path == null) {
         path = "/" + WAR_NAME + "/service";
      }
      return path;
   }

   /**
    * @see onepoint.express.applet.XExpressApplet#getParameters()
    */
   protected HashMap getParameters() {
      HashMap parameters = super.getParameters();
      String runLevel = getParameter(OpProjectConstants.RUN_LEVEL);
      if (runLevel != null) {
         parameters.put(OpProjectConstants.RUN_LEVEL, runLevel);
      }
      return parameters;
   }

   /**
    * @see XExpressApplet#showStartForm(String, java.util.HashMap)
    */
   protected void showStartForm(String start_form, HashMap parameters) {
      String runLevel = (String) parameters.get(OpProjectConstants.RUN_LEVEL);
      if (runLevel != null && Byte.parseByte(runLevel)== OpProjectConstants.CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL.byteValue()) {
         getDisplay().showForm(OpProjectConstants.CONFIGURATION_FORM);
      }
      else {
         getDisplay().showForm(start_form, parameters);
      }
   }

   /**
    * @see XExpressApplet#paintAlreadyLoaded(java.awt.Graphics, java.awt.Rectangle)
    */
   protected void paintAlreadyLoaded(Graphics g, Rectangle bounds) {
      g.setClip(bounds);
      g.setColor(Color.RED);
      g.drawString(APPLET_LOADED, 10, 10);
   }
}


