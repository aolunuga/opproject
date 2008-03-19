/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.applet;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.XExtendedComponent;
import onepoint.express.applet.XExpressApplet;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.project_planning.components.OpProjectComponentProxy;
import onepoint.project.modules.work.components.OpWorkProxy;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XBinaryClient;
import onepoint.service.XMessage;
import onepoint.util.XBase64;
import onepoint.util.XCalendar;
import onepoint.util.XCookieManager;

import java.awt.*;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Applet used for the expander application.
 */
public class OpOpenApplet extends XExpressApplet {

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getClientLogger(OpOpenApplet.class);

   /**
    * Various applet constants.
    */
   private final static String APPLET_LOADED = "OnePoint Applet already loaded in another browser window. Please close it and try again.";

   /**
    * Default war name (in case the server doesn't send an explicit one).
    */
   private final static String DEFAULT_CONTEXT_PATH = "opproject";

   private String version;

   private Date build;

   /**
    * Registers project proxies.
    */
   static {
      XComponent.registerProxy(new OpProjectComponentProxy());
      XComponent.registerProxy(new OpWorkProxy());      
   }

   /**
    * 
    */
   public OpOpenApplet() {
      super();
      getManifestInfo();
      logger.info(getClass().getName()+" constructed, version: "+(version == null ? "unknown" : version)+
                  " build: "+(build == null ? "unknown" : new SimpleDateFormat("yyyyMMdd").format(build)));
   }
   /**
    * 
    * @pre
    * @post
    */
   private void getManifestInfo() {
      Manifest mf = getManifest();
      if (mf != null) {
         Attributes attr = mf.getAttributes("Implementation");
         if (attr != null) {
            version = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
         }
         attr = mf.getMainAttributes();
         if (attr != null) {
            String buildString = attr.getValue("Build-Date");
            if (buildString != null) {
               SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
               try {
                  build = df.parse(buildString);
               }
               catch (ParseException exc) {
               }
            }
         }
      }
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
      String autoLoginStartForm = getParameter(OpProjectConstants.AUTO_LOGIN_START_FORM);
      getClient().setVariable(OpProjectConstants.START_FORM, startForm);
      getClient().setVariable(OpProjectConstants.AUTO_LOGIN_START_FORM, autoLoginStartForm);
      if (startForm != null) {
         parameters.put(OpProjectConstants.START_FORM, startForm);
      }
      if (autoLoginStartForm != null) {
         parameters.put(OpProjectConstants.AUTO_LOGIN_START_FORM, autoLoginStartForm);
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
      XComponent form = (XComponent) dockFrame.getChild(0);
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
      String runLevel = (String) parameters.get(OpProjectConstants.RUN_LEVEL);

      boolean success = false;
      //if everything is ok determine how to login the user
      if (runLevel != null && Byte.parseByte(runLevel) == OpProjectConstants.SUCCESS_RUN_LEVEL) {
         success = autoLogin();
         if (success) {
            startForm = (String) parameters.get(OpProjectConstants.AUTO_LOGIN_START_FORM);
         }
      }

      if (startForm != null) {
         getDisplay().showForm(startForm, parameters);
      }
      else {
         if (getClient().getVariable(OpProjectConstants.START_FORM) != null) {
            startForm = (String) getClient().getVariable(OpProjectConstants.START_FORM);
            if (success) {
               startForm = (String) getClient().getVariable(OpProjectConstants.AUTO_LOGIN_START_FORM);
            }
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
         // In case that encoded string exceeds XBase64.MAX_LINE_LENGTH length \n char is added to encoded value
         // and as a result value is quoted into cookie. Now we have to remove quotes if present.
         value = value.indexOf('"') != -1 ? value.replaceAll("\"", "") : value;

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

   public void destroy() {
      super.destroy();
      logger.info(getClass().getName()+" destroyed");
   }

   /* (non-Javadoc)
    * @see onepoint.express.applet.XExpressApplet#init()
    */
   public void init() {
      super.init();
      logger.info(getClass().getName()+" initialized");
   }

   /* (non-Javadoc)
    * @see onepoint.express.applet.XExpressApplet#start()
    */
   public void start() {
      super.start();
      logger.info(getClass().getName()+" started");
   }

   /* (non-Javadoc)
    * @see java.applet.Applet#stop()
    */
   public void stop() {
      super.stop();
      logger.info(getClass().getName()+" stopped");
   }
   
   /**
    * Returns the manifest this class is in
    * @return the manifest this class is in
    */
   private static Manifest getManifest() {
      // try reading infos from manifest
      try {
         URL url = OpOpenApplet.class.getResource("");
         JarURLConnection jconn = (JarURLConnection) url.openConnection();
         Manifest mf = jconn.getManifest();
         return mf;
      }
      catch (IOException exc) {
      }
      catch (ClassCastException exc) {
      }
      return null;
   }
}



