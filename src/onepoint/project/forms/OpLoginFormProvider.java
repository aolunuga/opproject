/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.XMessage;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceManager;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for the login form.
 *
 * @author horia.chiorean
 */
public class OpLoginFormProvider implements XFormProvider {

   /**
    * Request parameter names.
    */
   private static final String SESSION_EXPIRED = "sessionExpired";
   private static final String ERROR_LABEL_ID = "ErrorLabel";

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      OpProjectSession projectSession = (OpProjectSession) session;
      String localeId = projectSession.getLocale().getID();

      //check the run level
      checkRunLevel(parameters, localeId, form);

      //check whether the session has expired
      checkSessionExpired(parameters, localeId, form);

      //check license
      checkLicense(form, session);
   }

   /**
    * Checks the current status of the license.
    *                                       `
    * @param form    a <code>XComponent(FORM)</code> representing the login form.
    * @param session a <code>XSession</code> representing the server session.
    */
   private void checkLicense(XComponent form, XSession session) {
      XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
      XService service = XServiceManager.getService("LicenseService");
      if (service != null) {
         XMessage response = service.invokeMethod(session, "checkLicense", new XMessage());
         if (response != null) {
            if (response.getError() != null) {
               errorLabel.setText(response.getError().getMessage());
               errorLabel.setVisible(true);
            }
         }
      }
   }

   /**
    * Checks whether the session has expired or not.
    *
    * @param parameters a <code>HashMap</code> of <code>String,Object</code> pairs, representing form parameters.
    * @param localeId   a <code>String</code> representing the id of the current locale.
    * @param form       a <code>XComponent(FORM)</code> representing the login form.
    */
   private void checkSessionExpired(HashMap parameters, String localeId, XComponent form) {
      String sessionExpiredParameter = (String) parameters.get(SESSION_EXPIRED);
      if (sessionExpiredParameter != null) {
         XLocalizer localizer = createLocalizer(localeId, "main.error");
         XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
         errorLabel.setText(localizer.localize("{$SessionExpired}"));
         errorLabel.setVisible(true);
      }
   }

   /**
    * Checks the run level found in the parameters, and if necessary displays a message to the user.
    *
    * @param parameters a <code>HashMap</code> of <code>String,Object</code> pairs, representing form parameters.
    * @param localeId   a <code>String</code> representing the id of the current locale.
    * @param form       a <code>XComponent(FORM)</code> representing the login form.
    */
   private void checkRunLevel(HashMap parameters, String localeId, XComponent form) {
      String runLevelParameter = (String) parameters.get(OpProjectConstants.RUN_LEVEL);
      if (runLevelParameter != null) {
         String resourceMapId = OpInitializer.getErrorMapId();
         if (resourceMapId == null) {
            resourceMapId = "main.levels";
         }
         XLocalizer localizer = createLocalizer(localeId, resourceMapId);

         int runLevel = Integer.valueOf(runLevelParameter).intValue();
         int successRunLevel = OpInitializer.getSuccessRunLevel();
         if (runLevel < successRunLevel) {
            form.findComponent("Login").setEnabled(false);
            form.findComponent("Password").setEnabled(false);
            form.findComponent("okButton").setVisible(false);
            String resourceId = OpInitializer.getErrorId();
            if (resourceId == null) {
               resourceId = "{$" + OpProjectConstants.RUN_LEVEL + runLevelParameter + "}";
            }
            XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
            errorLabel.setText(localizer.localize(resourceId));
            errorLabel.setVisible(true);
         }
      }

   }

   /**
    * Creates a localizer object used by the login form to i18n messages.
    *
    * @param localeId      a <code>String</code> representing the id of a locale.
    * @param resourceMapId a <code>String</code> representing the id of a resource map.
    * @return a <code>XLocalizer</code> object.
    */
   private XLocalizer createLocalizer(String localeId, String resourceMapId) {
      XLocalizer localizer = new XLocalizer();
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(localeId, resourceMapId);
      localizer.setResourceMap(resourceMap);
      return localizer;
   }
}
