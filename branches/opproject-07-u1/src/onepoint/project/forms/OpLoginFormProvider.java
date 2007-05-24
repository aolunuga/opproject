/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpConnectionManager;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpProjectConstants;
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
   private static final String MYSQL_ENGINE_WARNING_ID = "MySQLEngineWarning";

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      OpProjectSession projectSession = (OpProjectSession) session;

      String localeId = projectSession.getLocale().getID();

      //check the run level
      String errorText = OpInitializer.checkRunLevel(parameters, localeId, "main.levels");
      if (errorText != null) {
         form.findComponent("Login").setEnabled(false);
         form.findComponent("Password").setEnabled(false);
         form.findComponent("okButton").setVisible(false);
         XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
         errorLabel.setText(errorText);
         errorLabel.setVisible(true);
      }
      else {
         //check for an invalid engine type
         String dbConnectionCode = (String) parameters.get(OpProjectConstants.DB_CONNECTION_CODE);
         if (dbConnectionCode != null && Integer.valueOf(dbConnectionCode).intValue() == OpConnectionManager.INVALID_MYSQL_ENGINE) {
            XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
            errorLabel.setText(form.findComponent(MYSQL_ENGINE_WARNING_ID).getStringValue());
            errorLabel.setVisible(true);
         }
      }

      //check whether the session has expired
      checkSessionExpired(parameters, localeId, form);

      //check license
      checkLicense(form, session);

      //mark the session as valid (in case it was invalidated)
      session.validate();
   }

   /**
    * Checks the current status of the license.
    * 
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
         XLocalizer localizer = XLocaleManager.createLocalizer(localeId, "main.error");
         XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
         errorLabel.setText(localizer.localize("{$SessionExpired}"));
         errorLabel.setVisible(true);
      }
   }
}
