/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.modules.settings.OpSettings;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;

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
    * @param localeId         a <code>String</code> representing a locale id.
    * @param user a <code>OpUser</code> representing the user for which to fill the data.
    */
   public static void fillLanguageDataSet(XComponent languageDataSet, XComponent languageField, String localeId, 
        OpUser user) {

      //find the user's language
      String currentLanguage = getUserLanguage(user);
      if (currentLanguage == null) {
         currentLanguage = OpSettings.get(OpSettings.USER_LOCALE);
      }

      Iterator it = XLocaleManager.getLocaleMap().keyIterator();
      int selectedIndex = 0;
      while (it.hasNext()) {
         String key = (String) it.next();
         String value = ((XLocale) XLocaleManager.findLocale(key)).getName();
         String choice = XValidator.choice(key, value);
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(choice);
         if (key.equals(currentLanguage)) {
            dataRow.setSelected(true);
            if (languageField != null) {
               languageField.setSelectedIndex(new Integer(selectedIndex));
            }
         }
         languageDataSet.addChild(dataRow);
         selectedIndex ++;
      }
   }

   /**
    * Gets the current user language.
    *
    * @param user a <code>OpUser</code> object.
    * @return a <code>String</code> representing the locale of the current user or <code>null</code> if there is none.
    */
   private static String getUserLanguage(OpUser user) {
      return user.getPreferenceValue(OpPreference.LOCALE);
   }

   /**
    * Updates the language preference for the given user.
    *
    * @param broker      a <code>OpBroker</code> used to perform business operations.
    * @param currentUser a <code>OpUser</code> representing the user for which to perform the operation.
    * @param language    the language code ("en", "de", etc).
    */
   public static void updateUserLanguagePreference(OpBroker broker, OpUser currentUser, String language) {
      boolean langugageChanged = false;

      if (currentUser.getPreferences() != null) {
         //set the language
         Iterator it = currentUser.getPreferences().iterator();
         while (it.hasNext()) {
            OpPreference userPreference = (OpPreference) it.next();
            if (userPreference.getName().equals(OpPreference.LOCALE)) {
               langugageChanged = true;
               userPreference.setValue(language);
               broker.updateObject(userPreference);
               break;
            }
         }
      }

      if (!langugageChanged) {
         OpPreference preference = new OpPreference();
         preference.setName(OpPreference.LOCALE);
         preference.setValue(language);
         preference.setUser(currentUser);
         broker.makePersistent(preference);
      }
   }

}
