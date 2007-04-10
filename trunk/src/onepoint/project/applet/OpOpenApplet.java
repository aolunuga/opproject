/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.applet;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.applet.XExpressApplet;
import onepoint.project.modules.project_planning.components.OpProjectComponentProxy;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XBinaryClient;
import onepoint.service.XMessage;
import onepoint.util.XBase64;
import onepoint.util.XCalendar;
import onepoint.util.XCookieManager;

import java.awt.*;
import java.util.HashMap;

/**
 * Applet used for the expander application.
 */
public class OpOpenApplet extends XExpressApplet {

   /**
    * Various applet constants.
    */
   private final static String APPLET_LOADED = "OnePoint Applet already loaded in another browser window. Please close it and try again.";

   /**
    * Default war name (in case the server doesn't send an explicit one).
    */
   private final static String DEFAULT_CONTEXT_PATH = "opproject";

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
         path = "/" + DEFAULT_CONTEXT_PATH + "/service";
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
    * @see XExpressApplet#showStartForm(String,java.util.HashMap)
    */
   protected void showStartForm(String start_form, HashMap parameters) {
      String runLevel = (String) parameters.get(OpProjectConstants.RUN_LEVEL);
      String formLocation = start_form;
      if (runLevel != null && Byte.parseByte(runLevel) == OpProjectConstants.CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL.byteValue()) {
         formLocation = OpProjectConstants.CONFIGURATION_WIZARD_FORM;
      }
      else {
         formLocation = autoLogin(formLocation);
      }
      getDisplay().showForm(formLocation, parameters);
   }

   /**
    * Automatically log-in the user if the auto-login cookie is set.
    *
    * @param formLocation default form to load.
    * @return the form to redirect to.
    */
   private String autoLogin(String formLocation) {
      XBinaryClient client = (XBinaryClient) getClient();
      String value = client.getCookieValue(XCookieManager.AUTO_LOGIN);
      if (value != null) {
         String logindata = XBase64.decodeToString(value);
         XMessage request = new XMessage();
         request.setAction(OpProjectConstants.SIGNON_ACTION);
         // idx of the user/passs separator
         int idxDelim = logindata.indexOf(' ');
         request.setArgument(OpProjectConstants.LOGIN_PARAM, logindata.substring(0, idxDelim));
         // if password is present > 0 chars
         if (logindata.length() - idxDelim > 1) {
            request.setArgument(OpProjectConstants.PASSWORD_PARAM, logindata.substring(idxDelim + 1));
         }
         request.setVariable(OpProjectConstants.CLIENT_TIMEZONE, XCalendar.CLIENT_TIMEZONE);
         // login user
         XMessage response = client.invokeMethod(request);
         // if autenticated, go to start form
         if (response.getError() == null) {
            XCalendar calendar = (XCalendar) client.getVariable(OpProjectConstants.CALENDAR);
            XDisplay.getDefaultDisplay().setCalendar(calendar);
            formLocation = OpProjectConstants.START_FORM;
         }
      }
      return formLocation;
   }

   /**
    * @see XExpressApplet#paintAlreadyLoaded(java.awt.Graphics,java.awt.Rectangle)
    */
   protected void paintAlreadyLoaded(Graphics g, Rectangle bounds) {
      g.setClip(bounds);
      g.setColor(Color.RED);
      g.drawString(APPLET_LOADED, 10, 10);
   }
}



