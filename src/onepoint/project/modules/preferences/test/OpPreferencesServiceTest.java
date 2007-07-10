/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.preferences.test;

import onepoint.persistence.OpBroker;
import onepoint.project.modules.preferences.OpPreferencesError;
import onepoint.project.modules.preferences.forms.OpPreferencesFormProvider;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.util.*;

/**
 * This class test preferences service methods and form providers.
 *
 * @author lucian.furtos
 */
public class OpPreferencesServiceTest extends OpBaseOpenTestCase {

   private static final String DEFAULT_USER = "tester";
   private static final String DEFAULT_PASSWORD = "pass";
   private static final String NEW_PASSWORD = "new_pass";

   private static final String PREFERENCES = "preferences";
   private static final String LANGUAGE_ARG = "language";
   private static final String PASSWORD_ARG = "password";
   private static final String PASSWORD_RETYPED_ARG = "passwordRetyped";
   private static final String SHOW_HOURS_ARG = "showHours";
   private static final String ENGLISH_LANGUAGE = "en";

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      clean();

      Map userData = OpUserTestDataFactory.createUserData(DEFAULT_USER, DEFAULT_PASSWORD, OpUser.STANDARD_USER_LEVEL);
      userData.put(OpUserService.LANGUAGE, ENGLISH_LANGUAGE);
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = OpTestDataFactory.getUserService().insertUser(session, request);
      assertNoError(response);

      logIn(DEFAULT_USER, DEFAULT_PASSWORD);
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      logOut();
      logIn();

      clean();

      super.tearDown();
   }

   /**
    * Test the saving of new preferences for current user
    *
    * @throws Exception if the opperation fails.
    */
   public void testSavePreferences()
        throws Exception {
      HashMap prefs = new HashMap();
      prefs.put(LANGUAGE_ARG, ENGLISH_LANGUAGE);
      prefs.put(PASSWORD_ARG, NEW_PASSWORD);
      prefs.put(PASSWORD_RETYPED_ARG, NEW_PASSWORD);
      prefs.put(SHOW_HOURS_ARG, Boolean.TRUE);

      XMessage request = new XMessage();
      request.setArgument(PREFERENCES, prefs);

      XMessage response = OpTestDataFactory.getPreferencesService().savePreferences(session, request);
      Boolean refresh = (Boolean) response.getArgument(OpProjectConstants.REFRESH_PARAM);

      assertNoError(response);
      assertNull(refresh);
   }

   /**
    * Test the saving of new preferences for current user when the passwords do not match or are empty
    *
    * @throws Exception if the opperation fails.
    */
   public void testSavePreferencesWrongPassword()
        throws Exception {
      HashMap prefs = new HashMap();
      prefs.put(PASSWORD_ARG, NEW_PASSWORD);
      prefs.put(PASSWORD_RETYPED_ARG, "wrong_pass");
      prefs.put(LANGUAGE_ARG, ENGLISH_LANGUAGE);

      XMessage request = new XMessage();
      request.setArgument(PREFERENCES, prefs);
      XMessage response = OpTestDataFactory.getPreferencesService().savePreferences(session, request);

      assertError(response, OpPreferencesError.PASSWORD_MISSMATCH);

      prefs.clear();
      prefs.put(PASSWORD_ARG, null);
      prefs.put(PASSWORD_RETYPED_ARG, null);
      prefs.put(LANGUAGE_ARG, ENGLISH_LANGUAGE);
      prefs.put(SHOW_HOURS_ARG, Boolean.FALSE);

      request = new XMessage();
      request.setArgument(PREFERENCES, prefs);
      response = OpTestDataFactory.getPreferencesService().savePreferences(session, request);

      assertError(response, OpPreferencesError.EMPTY_PASSWORD);

      prefs.clear();
      prefs.put(PASSWORD_ARG, OpPreferencesFormProvider.PASSWORD_TOKEN);
      prefs.put(PASSWORD_RETYPED_ARG, NEW_PASSWORD);
      prefs.put(LANGUAGE_ARG, ENGLISH_LANGUAGE);
       prefs.put(SHOW_HOURS_ARG, Boolean.FALSE);

      request = new XMessage();
      request.setArgument(PREFERENCES, prefs);
      response = OpTestDataFactory.getPreferencesService().savePreferences(session, request);

      assertError(response, OpPreferencesError.PASSWORD_MISSMATCH);
   }

   /**
    * Test change the language
    *
    * @throws Exception if the opperation fails.
    */
   public void testSavePreferencesChangeLanguage()
        throws Exception {
      HashMap prefs = new HashMap();
      prefs.put(LANGUAGE_ARG, "de");
      prefs.put(PASSWORD_ARG, OpPreferencesFormProvider.PASSWORD_TOKEN);
      prefs.put(PASSWORD_RETYPED_ARG, null);
      prefs.put(SHOW_HOURS_ARG, Boolean.TRUE);

      XMessage request = new XMessage();
      request.setArgument(PREFERENCES, prefs);
      XMessage response = OpTestDataFactory.getPreferencesService().savePreferences(session, request);
      XCalendar calendar = (XCalendar) response.getArgument(OpProjectConstants.CALENDAR);

      assertNoError(response);
      assertNotNull(calendar);
   }

   /**
    * Test create show hour preference
    *
    * @throws Exception if the opperation fails.
    */
   public void testSavePreferencesCreateShowHour()
        throws Exception {
      OpBroker broker = session.newBroker();
      OpUser currentUser = session.user(broker);
      OpPreference showHoursPref = currentUser.getPreference(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showHoursPref != null) {
         broker.deleteObject(showHoursPref);
      }
      broker.close();

      HashMap prefs = new HashMap();
      prefs.put(LANGUAGE_ARG, ENGLISH_LANGUAGE);
      prefs.put(PASSWORD_ARG, OpPreferencesFormProvider.PASSWORD_TOKEN);
      prefs.put(PASSWORD_RETYPED_ARG, null);
      prefs.put(SHOW_HOURS_ARG, Boolean.TRUE);

      XMessage request = new XMessage();
      request.setArgument(PREFERENCES, prefs);
      XMessage response = OpTestDataFactory.getPreferencesService().savePreferences(session, request);

      assertNoError(response);
      broker = session.newBroker();
      currentUser = session.user(broker);
      showHoursPref = currentUser.getPreference(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      assertNotNull(showHoursPref);
      assertEquals(showHoursPref.getValue(), "true");
      broker.close();
   }

   /**
    * Test create show hour preference
    *
    * @throws Exception if the opperation fails.
    */
   public void testSavePreferencesChangeShowHour()
        throws Exception {
      OpBroker broker = session.newBroker();
      OpUser currentUser = session.user(broker);
      OpPreference showHoursPref = currentUser.getPreference(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showHoursPref != null) {
         assertEquals(showHoursPref.getValue(), "false");
      }
      broker.close();

      HashMap prefs = new HashMap();
      prefs.put(LANGUAGE_ARG, ENGLISH_LANGUAGE);
      prefs.put(PASSWORD_ARG, OpPreferencesFormProvider.PASSWORD_TOKEN);
      prefs.put(PASSWORD_RETYPED_ARG, null);
      prefs.put(SHOW_HOURS_ARG, Boolean.TRUE);

      XMessage request = new XMessage();
      request.setArgument(PREFERENCES, prefs);
      XMessage response = OpTestDataFactory.getPreferencesService().savePreferences(session, request);

      assertNoError(response);
      broker = session.newBroker();
      currentUser = session.user(broker);
      showHoursPref = currentUser.getPreference(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      assertNotNull(showHoursPref);
      assertEquals(showHoursPref.getValue(), "true");
      broker.close();
   }

   private void clean() {
      OpUserTestDataFactory usrData = new OpUserTestDataFactory(session);
      ArrayList ids = new ArrayList();
      List users = usrData.getAllUsers();
      for (Iterator iterator = users.iterator(); iterator.hasNext();) {
         OpUser user = (OpUser) iterator.next();
         if (user.getName().equals(OpUser.ADMINISTRATOR_NAME)) {
            continue;
         }
         ids.add(user.locator());
      }
      XMessage request = new XMessage();
      request.setArgument(OpUserService.SUBJECT_IDS, ids);
      OpTestDataFactory.getUserService().deleteSubjects(session, request);
   }   
}
