/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.preferences;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.preferences.forms.OpPreferencesFormProvider;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserLanguageManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.project.util.OpSHA1;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.util.HashMap;

/**
 * Service class that handles all the business operations
 *
 * @author horia.chiorean
 */
public class OpPreferencesService extends OpProjectService {

   /**
    * This service's error map
    */
   private static final OpPreferencesErrorMap ERROR_MAP = new OpPreferencesErrorMap();

   /**
    * Saves the user preferences.
    *
    * @param session a <code>OpProjectSession</code> object representing the current session.
    * @param request a <code>XMessage</code> representing the current request.
    * @return a response in the form of a <code>XMessage</code> object.
    */
   public XMessage savePreferences(OpProjectSession session, XMessage request) {

      XMessage reply = new XMessage();

      HashMap arguments = (HashMap) request.getArgument("preferences");

      String language = (String) arguments.get("language");
      String password = (String) arguments.get("password");
      String passwordRetyped = (String) arguments.get("passwordRetyped");
      Boolean showHours = (Boolean) arguments.get("showHours");

      //start password validation
      XMessage validationResult = validatePasswords(password, passwordRetyped, session);
      if (validationResult.getError() != null) {
         return validationResult;
      }

      OpBroker broker = session.newBroker();
      OpUser currentUser = session.user(broker);

      OpTransaction tx = broker.newTransaction();
      
      //update the language
      boolean languageChanged = OpUserLanguageManager.updateUserLanguagePreference(broker, currentUser, language);
      if (languageChanged) {
         if (!session.getLocale().getID().equals(language)) {
            XLocale newLocale = XLocaleManager.findLocale(language);
            session.setLocale(newLocale);
            OpSettings.configureServerCalendar(session);
            reply.setArgument(OpProjectConstants.CALENDAR, session.getCalendar());
         }
      }

      if (!checkPasswords(password, OpPreferencesFormProvider.PASSWORD_TOKEN)) {
         //update the password
         String hashedPasswrd = (password == null ) ? null : new OpSHA1().calculateHash(password);
         currentUser.setPassword(hashedPasswrd);
         broker.updateObject(currentUser);
      }

      //update the "show assignment in hours" preference
      OpPreference showHoursPref = currentUser.getPreference(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showHoursPref != null) {
         showHoursPref.setValue(showHours.toString());
         broker.updateObject(showHoursPref);
      }
      else {
         OpPreference showHoursPreference = new OpPreference();
         showHoursPreference.setUser(currentUser);
         showHoursPreference.setName(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
         showHoursPreference.setValue(showHours.toString());
         broker.makePersistent(showHoursPreference);
      }

      tx.commit();
      broker.close();

      return reply;
   }

   /**
    * Validates the password that came from the client side.
    * @param password a <code>String</code> representing the user password.
    * @param passwordRetyped a <code>String</code> representing the confirmal of the user password.
    * @param session a <code>OpProjectSession</code> representing the current user session.
    * @return a <code>XMessage</code> representing a possible error message or <code>null</code> if the passwords are valid.
    */
   private XMessage validatePasswords(String password, String passwordRetyped, OpProjectSession session) {
      XMessage response = new XMessage();

      if (checkPasswords(password, OpPreferencesFormProvider.PASSWORD_TOKEN)) {
         if (!checkPasswords(passwordRetyped, null)) {//wrong retyped password
            response.setError(session.newError(ERROR_MAP, OpPreferencesError.PASSWORD_MISSMATCH));
            return response;
         }
      }
      else {
         boolean allowsEmptyPasswords = Boolean.valueOf(OpSettings.get(OpSettings.ALLOW_EMPTY_PASSWORD)).booleanValue();
         if ((password == null || password.length() == 0) && !allowsEmptyPasswords) {
            response.setError(session.newError(ERROR_MAP, OpPreferencesError.EMPTY_PASSWORD));
            return response;
         }
         if (!checkPasswords(password, passwordRetyped)) {//wrong retyped password
            response.setError(session.newError(ERROR_MAP, OpPreferencesError.PASSWORD_MISSMATCH));
            return response;
         }
      }
      return response;
   }

   /**
    * Performs equality checking for the given passwords.
    *
    * @param password1 <code>String</code> first password
    * @param password2 <code>String</code> second password
    * @return boolean flag indicating passwords equality
    */
   private boolean checkPasswords(String password1, String password2) {
      if (password1 != null && password2 != null) {
         return password1.equals(password2);
      }
      if (password1 != null) {
         return password1.equals(password2);
      }
      if (password2 != null) {
         return password2.equals(password1);
      }
      return password1 == password2;
   }
}
