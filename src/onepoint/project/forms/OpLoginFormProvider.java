/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpUserError;
import onepoint.project.modules.user.OpUserErrorMap;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

/**
 * Form provider for the login form.
 *
 * @author horia.chiorean
 */
public class OpLoginFormProvider implements XFormProvider {

   /**
    * Request parameter names.
    */
   protected static final String ERROR_LABEL_ID = "ErrorLabel";
   private static final String SESSION_EXPIRED = "sessionExpired";
   private static final String LOGIN_FIELD = "Login";
   private static final String PASSWORD_FIELD = "Password";
   private static final String OK_BUTTON = "okButton";
   private static final String REMEMBER_CHECK_BOX = "Remember";
   private static final String ERROR_MAP = "main.error";
   private static final String SESSION_EXPIRED_ERROR = "${SessionExpired}";
   protected static final String START_FORM = "StartForm";
   
   private static final String MOTD_FILE_NAME = "motd.txt";
   private static final String MOTD_FIELD = "MOTD";


   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {

      OpProjectSession projectSession = (OpProjectSession) session;
      String localeId = projectSession.getLocale().getID();

      //set the start form
      setStartForm(form);

      //check the run level
      String errorText = OpRunLevelErrorFormProvider.getErrorResourceFromRunLevel(parameters, localeId, "main.levels");
      if (errorText != null) {
         disableFields(form);
         XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
         errorLabel.setText(errorText);
         errorLabel.setVisible(true);
      }
      else {
         //check whether the session has expired
         checkSessionExpired(parameters, localeId, form);

         //check license
         checkLicense(form, projectSession);

         if (!session.getServer().isSiteValid(((OpProjectSession)session).getSourceName())) {
            XLocalizer localizer = XLocaleManager.createLocalizer(localeId, OpUserErrorMap.RESOURCE_MAP_ID);
            XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
            errorLabel.setText(localizer.localize("${"+OpUserError.SITE_IS_INVALID_NAME+"}"));
            errorLabel.setVisible(true);
         }
         
         // display MOTD, if present:
         String path = OpEnvironmentManager.getOnePointHome();
         String motdFile = path + File.separator + MOTD_FILE_NAME;
         
         String motd = "";
         try {
            BufferedReader motdReader = new BufferedReader(new FileReader(motdFile));
            // read only the first line!
            while (motd.length() == 0) {
               motd = motdReader.readLine();
            }
         } catch (FileNotFoundException e) {
         } catch (IOException e) {
         }
         
         if (motd.length() > 0) {
            XComponent motdField = form.findComponent(MOTD_FIELD);
            motdField.setText(motd);
            motdField.setVisible(true);
         }
         //mark the session as valid (in case it was invalidated)
         session.validate();
      }
   }

   /**
    * Checks if the license has errors and/or warnings.
    *
    * @param form    a <code>XComponent(FORM)</code> representing the login form.
    * @param session a <code>XSession</code> representing the server session.
    */
   protected void checkLicense(XComponent form, OpProjectSession session) {
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
         XLocalizer localizer = XLocaleManager.createLocalizer(localeId, ERROR_MAP);
         XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
         errorLabel.setText(localizer.localize(SESSION_EXPIRED_ERROR));
         errorLabel.setVisible(true);
      }
   }

   /**
    *  Disables login fields.
    */
   protected void disableFields(XComponent form) {
      form.findComponent(LOGIN_FIELD).setEnabled(false);
      form.findComponent(PASSWORD_FIELD).setEnabled(false);
      form.findComponent(OK_BUTTON).setVisible(false);
      form.findComponent(REMEMBER_CHECK_BOX).setVisible(false);
   }

   protected void setStartForm(XComponent form) {
      form.findComponent(START_FORM).setStringValue(OpEnvironmentManager.getAutoLoginStartForm());
   }
}
