/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.preferences.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.preferences.OpPreferencesService;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserLanguageManager;
import onepoint.resource.XLocale;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Form provider class for the user preferences module.
 *
 * @author horia.chiorean
 */
public class OpPreferencesFormProvider implements XFormProvider {

   /**
    * The dummy password token.
    */
   public static final String PASSWORD_TOKEN = "@@@@@@@@@";

   /**
    * Constants used in this form provider.
    */
   private static final String USER_LANGUAGE_DATASET_ID = "UserLanguageDataSet";
   private static final String USER_PASSWORD_ID = "Password";
   private static final String LANGUAGE_CHOICE_ID = "UserLanguage";
   private static final String SHOW_HOURS_ID = "ShowResourceHours";
   private static final String IMPORT_EXPORT_LANGUAGE_DATASET_ID = "ImportExportLanguageDataSet";
   private static final String IMPORT_EXPORT_LANGUAGE_CHOICE_ID = "ImportExportLanguage";

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      OpBroker broker = session.newBroker();
      try {
         OpUser currentUser = session.user(broker);

         fillPreferences(session, form, currentUser);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Fills the preferences form with the current user's information.
    *
    * @param session     the <code>OpProjectSession</code> object
    * @param form        Preferences form
    * @param currentUser Current user
    */
   protected void fillPreferences(OpProjectSession session, XComponent form, OpUser currentUser) {
      //get the available languages
      XComponent languageDataSet = form.findComponent(USER_LANGUAGE_DATASET_ID);
      XComponent languageChoiceField = form.findComponent(LANGUAGE_CHOICE_ID);

      OpUserLanguageManager.fillLanguageDataSet(session, languageDataSet, languageChoiceField, currentUser);

      //set up the dummy user password
      XComponent passwordField = form.findComponent(USER_PASSWORD_ID);
      passwordField.setStringValue(PASSWORD_TOKEN);

      //set up the hours view preference
      Boolean showHours = Boolean.valueOf((OpSettingsService.getService().getStringValue(session, OpSettings.SHOW_RESOURCES_IN_HOURS)));
      String showAssignInHoursPref = currentUser.getPreferenceValue(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showAssignInHoursPref != null) {
         showHours = Boolean.valueOf(showAssignInHoursPref);
      }
      form.findComponent(SHOW_HOURS_ID).setBooleanValue(showHours.booleanValue());

      // fill the import/export language data set
      fillImportExportLanguageDataSet(session, form, currentUser);
   }

   /**
    * Fills and orders the import/export data set
    *
    * @param session     the <code>OpProjectSession</code> object
    * @param form        Preferences form
    * @param currentUser Current user
    */
   private void fillImportExportLanguageDataSet(OpProjectSession session, XComponent form, OpUser currentUser) {
      // obtain the previously saved import/export language
      XLocale importExportLocale = session.getLocale();
      String importExportLanguage = importExportLocale.getLanguage();

      //<FIXME author="Mihai Costin" description="At the moment the locales from MPXJ contain only language (no country)!">
      //if (importExportLocale.getCountry() != null) {
      //   importExportLanguage += "_" + importExportLocale.getCountry();
      //}
      //</FIXME>

      String importExportLanguagePref = currentUser.getPreferenceValue(OpPreference.IMPORT_EXPORT_LANGUAGE);
      if (importExportLanguagePref != null) {
         importExportLanguage = importExportLanguagePref;
      }

      // fill the import/export language data set and select the appropriate data row
      XComponent importExportLanguageChoiceField = form.findComponent(IMPORT_EXPORT_LANGUAGE_CHOICE_ID);
      XComponent importExportDataSet = form.findComponent(IMPORT_EXPORT_LANGUAGE_DATASET_ID);

      List locales = OpPreferencesService.getSupportedMSProjectLocales();
      for (int i = 0; i < locales.size(); i++) {
         XComponent dataRow = importExportDataSet.newDataRow();
         Locale locale = (Locale) locales.get(i);
         dataRow.setStringValue(XValidator.choice(locale.getLanguage(), locale.getDisplayLanguage(locale)));
         importExportDataSet.addDataRow(dataRow);
      }

      // sort languages according to the current locale
      importExportDataSet.sort();

      // select the data row which contains the previously saved preference, or the default one
      for (int i = 0; i < importExportDataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) importExportDataSet.getChild(i);
         if (XValidator.choiceID(dataRow.getStringValue()).equals(importExportLanguage)) {
            dataRow.setSelected(true);
            importExportLanguageChoiceField.setSelectedIndex(i);
            break;
         }
      }
   }
   
}