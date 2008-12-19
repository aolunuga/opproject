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
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserLanguageManager;
import onepoint.project.util.OpHashProvider;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceManager;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.jar.JarFile;
import java.net.URL;
import java.net.JarURLConnection;
import java.io.IOException;

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

   private static final String PREFERENCES_SERVICE_NAME = "PreferencesService";

   private static final String PREFERENCES_ARGUMENT = "preferences";

   private static final String LANGUAGE_KEY = "language";
   private static final String PASSWORD_KEY = "password";
   private static final String PASSWORD_RETYPED_KEY = "passwordRetyped";
   private static final String SHOW_HOURS_KEY = "showHours";
   private static final String IMPORT_EXPORT_LANGUAGE_KEY = "importExportLanguage";
   private static final String IMAGE_FORMAT_KEY = "imageFormat";

   private static final String PREFERENCE_NAME_ARG = "preference";
   private static final String IMAGE_FORMATS_ARG = "imageFormats";

   private static final Map IMAGE_FORMATS;
   private static final XLog logger = XLogFactory.getLogger(OpPreferencesService.class);

   static {
      IMAGE_FORMATS = new HashMap();
      IMAGE_FORMATS.put("gif", "CompuServe GIF");
      IMAGE_FORMATS.put("jpg", "JPEG");
      IMAGE_FORMATS.put("png", "PNG");
   }

   /**
    * Gets the registered instance of this service.
    *
    * @return The registered instance of this service.
    */
   public static OpPreferencesService getService() {
      return (OpPreferencesService) XServiceManager.getService(PREFERENCES_SERVICE_NAME);
   }

   /**
    * Saves the user preferences.
    *
    * @param session a <code>OpProjectSession</code> object representing the current session.
    * @param request a <code>XMessage</code> representing the current request.
    * @return a response in the form of a <code>XMessage</code> object.
    */
   public XMessage savePreferences(OpProjectSession session, XMessage request) {


      HashMap arguments = (HashMap) request.getArgument(PREFERENCES_ARGUMENT);

      String password = (String) arguments.get(PASSWORD_KEY);
      String passwordRetyped = (String) arguments.get(PASSWORD_RETYPED_KEY);

      //start password validation
      XMessage validationResult = validatePasswords(password, passwordRetyped, session);
      if (validationResult.getError() != null) {
         return validationResult;
      }

      OpBroker broker = session.newBroker();
      try {
         OpUser currentUser = session.user(broker);

         OpTransaction tx = broker.newTransaction();

         if (!checkPasswords(password, OpPreferencesFormProvider.PASSWORD_TOKEN)) {
            //update the password
            String hashedPasswrd = (password == null) ? null : new OpHashProvider().calculateHash(password);
            currentUser.setPassword(hashedPasswrd);
            broker.updateObject(currentUser);
         }

         XMessage reply = updateUserPreferences(session, broker, currentUser, arguments);

         tx.commit();
         return reply;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Returns the value of a user preference, specified by name in the request arguments map.
    *
    * @param session a <code>OpProjectSession</code> object representing the current session.
    * @param request a <code>XMessage</code> representing the current request.
    * @return a response in the form of a <code>XMessage</code> object containing the required preference value
    */
   public XMessage getPreference(OpProjectSession session, XMessage request) {
      HashMap arguments = (HashMap) request.getArgument(PREFERENCES_ARGUMENT);
      String preference = (String) arguments.get(PREFERENCE_NAME_ARG);

      XMessage reply = new XMessage();
      reply.setArgument(PREFERENCE_NAME_ARG, getPreference(session, preference));
      return reply;
   }

   /**
    * Returns the value of a user preference.
    *
    * @param session        a <code>OpProjectSession</code> object representing the current session.
    * @param preferenceName the name of the preference whose value is retreived.
    * @return the value of the user preference or null if the preference name is null.
    */
   public String getPreference(OpProjectSession session, String preferenceName) {
      String preferenceValue = null;
      if (preferenceName != null) {
         OpBroker broker = session.newBroker();
         OpUser currentUser = session.user(broker);
         preferenceValue = currentUser.getPreferenceValue(preferenceName);
         broker.close();
      }
      return preferenceValue;
   }

   /**
    * Returns the list of possible image formats.
    *
    * @param session current session
    * @param request client request
    * @return reply to client containing the image formats map
    */
   public XMessage getImageFormatMap(OpProjectSession session, XMessage request) {
      XMessage reply = new XMessage();
      reply.setArgument(IMAGE_FORMATS_ARG, IMAGE_FORMATS);
      return reply;
   }

   /**
    * Updates the user preferences from the given arguments.
    *
    * @param session     Current session
    * @param broker      Current broker instance
    * @param currentUser Session user
    * @param arguments   Request arguments.
    * @return reply message
    */
   protected XMessage updateUserPreferences(OpProjectSession session, OpBroker broker, OpUser currentUser, HashMap arguments) {

      XMessage reply = new XMessage();
      //update the language preference
      String xLocaleId = (String) arguments.get(LANGUAGE_KEY);
      boolean languageChanged = OpUserLanguageManager.updateUserLanguagePreference(broker, currentUser, xLocaleId);
      if (languageChanged) {
         if (!session.getLocale().getID().equals(xLocaleId)) {
            XLocale newLocale = XLocaleManager.findLocale(xLocaleId);
            session.setLocale(newLocale);
            OpSettingsService.getService().configureServerCalendar(session);
            reply.setArgument(OpProjectConstants.CALENDAR, session.getCalendar());
         }
      }

      //update the "show assignment in hours" preference
      saveOrUpdatePreference(broker, currentUser, arguments, SHOW_HOURS_KEY, OpPreference.SHOW_ASSIGNMENT_IN_HOURS);

      //update the "saved image format" preference
      saveOrUpdatePreference(broker, currentUser, arguments, IMAGE_FORMAT_KEY, OpPreference.SAVED_IMAGE_FORMAT);

      //update the "import/export language" preference
      saveOrUpdatePreference(broker, currentUser, arguments, IMPORT_EXPORT_LANGUAGE_KEY, OpPreference.IMPORT_EXPORT_LANGUAGE);

      return reply;
   }

   /**
    * Saves or updates a user preference.
    *
    * @param broker         Current broker instance
    * @param currentUser    Session user
    * @param arguments      Request arguments
    * @param argumentKey    Request argument name representing the user preference to be saved/updated
    * @param preferenceName Name of the user preference to be saved/updated
    */
   protected void saveOrUpdatePreference(OpBroker broker, OpUser currentUser, HashMap arguments, String argumentKey, String preferenceName) {
      String argumentsValue = arguments.get(argumentKey).toString();
      OpPreference preference = currentUser.getPreference(preferenceName);
      if (preference != null) {
         preference.setValue(argumentsValue);
         broker.updateObject(preference);
      }
      else {
         OpPreference newPreference = new OpPreference();
         newPreference.setUser(currentUser);
         newPreference.setName(preferenceName);
         newPreference.setValue(argumentsValue);
         broker.makePersistent(newPreference);
      }
   }

   /**
    * Validates the password that came from the client side.
    *
    * @param password        a <code>String</code> representing the user password.
    * @param passwordRetyped a <code>String</code> representing the confirmal of the user password.
    * @param session         a <code>OpProjectSession</code> representing the current user session.
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
         boolean allowsEmptyPasswords = Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.ALLOW_EMPTY_PASSWORD));
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


   /**
    * Returns a list of supported locales by searching for LocaleData_xx classes from the mpxj jar.
    *
    * @return a List of supported mpxj locales for read/write opperations.
    */
   public static List getSupportedMSProjectLocales() {
      List result = new ArrayList();
      try {
         URL resource = OpPreferencesService.class.getClassLoader().getResource("net/sf/mpxj/mpx/");

         //we assume that the mpxj is available as a jar file resource.
         JarURLConnection conn = (JarURLConnection) resource.openConnection();
         JarFile jfile = conn.getJarFile();
         Enumeration e = jfile.entries();
         while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            String entryname = entry.getName();
            if (entryname.startsWith("net/sf/mpxj/mpx/LocaleData_") && entryname.endsWith(".class")) {
               String className = entryname.replace(".class", "").replace("/", ".");
               try {
                  Class localeClass = Class.forName(className);
                  //check if the locale class extends the expected ListResourceBundle class
                  if (localeClass.getSuperclass() != ListResourceBundle.class) {
                     //not a ListResourceBundle, continue
                     continue;
                  }
               }
               catch (ClassNotFoundException e1) {
                  logger.warn("Unable to instantiate the locale class for " + className);
               }

               String[] strings = className.split("_");
               if (strings.length > 1) {
                  String localeStr = strings[1];
                  Locale locale = new Locale(localeStr);
                  result.add(locale);
               }
            }
         }
      }
      catch (IOException e) {
         logger.warn("Unable to determine the supported locales of mpxj library", e);
      }

      if (result.isEmpty()) {
         logger.warn("Adding internationalized locale en");
         result.add("en");
      }

      return result;
   }

}
