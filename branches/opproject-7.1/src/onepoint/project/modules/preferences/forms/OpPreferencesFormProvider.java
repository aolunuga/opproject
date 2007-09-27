/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.preferences.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserLanguageManager;
import onepoint.service.server.XSession;

import java.util.HashMap;

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

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      OpBroker broker = session.newBroker();
      OpUser currentUser = session.user(broker);

      fillPreferences(form, currentUser);

      broker.close();
   }

   /**
    * Fills the preferences form with the current user's information.
    *
    * @param form        Preferences form
    * @param currentUser Current user
    */
   protected void fillPreferences(XComponent form, OpUser currentUser) {
      //get the avaiable languages
      XComponent languageDataSet = form.findComponent(USER_LANGUAGE_DATASET_ID);
      XComponent languageChoiceField = form.findComponent(LANGUAGE_CHOICE_ID);

      OpUserLanguageManager.fillLanguageDataSet(languageDataSet, languageChoiceField, currentUser);

      //set up the dummy user password
      XComponent passwordField = form.findComponent(USER_PASSWORD_ID);
      passwordField.setStringValue(PASSWORD_TOKEN);

      //set up the hours view preference
      Boolean showHours = Boolean.valueOf((OpSettingsService.getService().get(OpSettings.SHOW_RESOURCES_IN_HOURS)));
      String showAssignInHoursPref = currentUser.getPreferenceValue(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showAssignInHoursPref != null) {
         showHours = Boolean.valueOf(showAssignInHoursPref);
      }
      form.findComponent(SHOW_HOURS_ID).setBooleanValue(showHours.booleanValue());
   }
}
