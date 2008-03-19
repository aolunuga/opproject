/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsDataSetFactory;
import onepoint.project.modules.settings.OpSettingsService;

import java.util.Iterator;

/**
 * Class that manages the user languages for the form providers.
 *
 * @author horia.chiorean
 */
public final class OpUserLanguageManager {

   /**
    * Should not be instantiated directly.
    */
   private OpUserLanguageManager() {
   }

   /**
    * Fills the given language dataset component with the available languages.
    *
    * @param languageDataSet a <code>XComponent(DATA_SET)</code> representing a dataset that will hold the user languages.
    * @param languageField   a <code>XComponent(CHOICE_FIELD)</code> representing the choice field where the user selected language
    *                        will be displayed. This may be <code>null</code>.
    * @param user            a <code>OpUser</code> representing the user for which to fill the data.
    */
   public static void fillLanguageDataSet(OpProjectSession session, XComponent languageDataSet, XComponent languageField, OpUser user) {

      //find the user's language
      String currentLocaleId = getUserLocaleId(user);
      if (currentLocaleId == null) {
         currentLocaleId = OpSettingsService.getService().get(session, OpSettings.USER_LOCALE_ID);
      }

      OpSettingsDataSetFactory.fillLanguageDataSet(languageDataSet, languageField, currentLocaleId);
   }

   /**
    * Gets the current user language.
    *
    * @param user a <code>OpUser</code> object.
    * @return a <code>String</code> representing the locale ID of the current user or <code>null</code> if there is none.
    */
   private static String getUserLocaleId(OpUser user) {
      return user.getPreferenceValue(OpPreference.LOCALE_ID);
   }

   /**
    * Updates the language preference for the given user.
    *
    * @param broker      a <code>OpBroker</code> used to perform business operations.
    * @param currentUser a <code>OpUser</code> representing the user for which to perform the operation.
    * @param xLocaleId    the XLocale id
    * @return true if the language was updated (i.e. if the new language preference != old preference)
    */
   public static boolean updateUserLanguagePreference(OpBroker broker, OpUser currentUser, String xLocaleId) {
      boolean languageChanged = false;

      if (currentUser.getPreferences() != null) {
         //set the language
         Iterator it = currentUser.getPreferences().iterator();
         while (it.hasNext()) {
            OpPreference userPreference = (OpPreference) it.next();
            if (userPreference.getName().equals(OpPreference.LOCALE_ID)) {
               languageChanged = true;
               userPreference.setValue(xLocaleId);
               broker.updateObject(userPreference);
               break;
            }
         }
      }

      if (!languageChanged) {
         OpPreference preference = new OpPreference();
         preference.setName(OpPreference.LOCALE_ID);
         preference.setValue(xLocaleId);
         preference.setUser(currentUser);
         languageChanged = true;
         broker.makePersistent(preference);
      }

      return languageChanged;
   }

}
