/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.applet;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.XExtendedComponent;
import onepoint.express.XView;
import onepoint.express.applet.XExpressApplet;
import onepoint.project.modules.project_planning.components.OpProjectComponentProxy;
import onepoint.project.modules.work.components.OpWorkProxy;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XBinaryClient;
import onepoint.service.XMessage;
import onepoint.util.XBase64;
import onepoint.util.XCalendar;
import onepoint.util.XCookieManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
      XComponent.registerProxy(new OpWorkProxy());
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
      String startForm = getParameter(OpProjectConstants.START_FORM);
      getClient().setVariable(OpProjectConstants.START_FORM, startForm);
      if(startForm != null) {
         parameters.put(OpProjectConstants.START_FORM, startForm);
      }
      return parameters;
   }

   /**
    * @see onepoint.express.XViewer#showStartForm(java.util.Map)
    */
   public void showForm(String location) {
      getDisplay().showForm(location);
   }


   /**
    * @see onepoint.express.XViewer#showStartForm(java.util.Map)
    */
   public void showForm(String location, Map parameters) {
      getDisplay().showForm(location, parameters);
   }

   public void showMainForm(int group, int pos) {
      showMainForm(group, pos, null);
   }
   
   private void showMainForm(int group, int pos, String mainLocation) {
      XComponent dockFrame = XDisplay.findFrame("DockFrame");
      XComponent form = (XComponent)dockFrame.getChild(0);
      XExtendedComponent box = (XExtendedComponent) form.findComponent("NavigationBox");
      box.requestFocus();

      // show first selected item within main frame
      XComponent main_frame = XDisplay.findFrame("MainFrame");
      if (mainLocation != null) {
         main_frame.showForm(mainLocation);
      }
      box.deselectNavigationItems();
      box.selectNavigationItem(group, pos);
      dockFrame.refreshForm();
   }

   /**
    * @see onepoint.express.XViewer#showStartForm(java.util.Map)
    */
   public void showStartForm(Map parameters) {
      String startForm = (String) parameters.get(OpProjectConstants.START_FORM);
      if (startForm != null) {
         getDisplay().showForm(startForm, parameters);
      }
      else {
         if(getClient().getVariable(OpProjectConstants.START_FORM) != null) {
            startForm = (String) getClient().getVariable(OpProjectConstants.START_FORM);
            getDisplay().showForm(startForm, parameters);
         }
         else {
            throw new UnsupportedOperationException("No start form defined");
         }
      }
   }

   /**
    * Automatically log-in the user if the auto-login cookie is set.
    *
    * @return the form to redirect to.
    */
   private boolean autoLogin() {
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
            return true;
         }
      }
      return false;
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



