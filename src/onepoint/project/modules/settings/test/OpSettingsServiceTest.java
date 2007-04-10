/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.settings.test;

import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsError;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.test.OpBaseTestCase;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.util.HashMap;

/**
 * This class test settings service methods and form providers.
 *
 * @author lucian.furtos
 */
public class OpSettingsServiceTest extends OpBaseTestCase {

   /**
    * Tear down
    */
   public void tearDown()
        throws Exception {
      HashMap prefs = new HashMap();
      prefs.put(OpSettings.USER_LOCALE, "en");
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(OpSettings.CALENDAR_DAY_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(OpSettings.CALENDAR_WEEK_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_HOLIDAYS_LOCATION, OpSettings.CALENDAR_HOLIDAYS_LOCATION_DEFAULT);
      prefs.put(OpSettings.ALLOW_EMPTY_PASSWORD, Boolean.valueOf(OpSettings.ALLOW_EMPTY_PASSWORD_DEFAULT));
      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);
      prefs.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, new Integer(OpSettings.REPORT_REMOVE_TIME_PERIOD_DEFAULT));
      prefs.put(OpSettings.RESOURCE_MAX_AVAILABYLITY, new Double(OpSettings.RESOURCE_MAX_AVAILABYLITY_DEFAULT));
      prefs.put(OpSettings.MILESTONE_CONTROLLING_INTERVAL, new Integer(OpSettings.MILESTONE_CONTROLLING_INTERVAL_DEFALUT));
      prefs.put(OpSettings.SHOW_RESOURCES_IN_HOURS, Boolean.valueOf(OpSettings.SHOW_RESOURCES_IN_HOURS_DEFAULT));

      XMessage request = new XMessage();
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);

      XMessage response = getSettingsService().saveSettings(session, request);
      assertNoError(response);

      super.tearDown();
   }

   /**
    * Test wrong work days
    *
    * @throws Exception if the test fails
    */
   public void testWrongWorkDay()
        throws Exception {
      HashMap prefs = new HashMap();
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);

      XMessage request = new XMessage();
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);

      XMessage response = getSettingsService().saveSettings(session, request);
      assertError(response, OpSettingsError.LAST_WORK_DAY_INCORRECT);
   }

   /**
    * Test wrong day work time
    *
    * @throws Exception if the test fails
    */
   public void testWrongDayWorkTime()
        throws Exception {
      HashMap prefs = new HashMap();
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(-1d));

      XMessage request = new XMessage();
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);

      XMessage response = getSettingsService().saveSettings(session, request);
      assertError(response, OpSettingsError.DAY_WORK_TIME_INCORRECT);

      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(25d));

      request = new XMessage();
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);

      response = getSettingsService().saveSettings(session, request);
      assertError(response, OpSettingsError.DAY_WORK_TIME_INCORRECT);
   }

   /**
    * Test week work time
    *
    * @throws Exception if the test fails
    */
   public void testWeekWorkTime()
        throws Exception {
      HashMap prefs = new HashMap();
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(OpSettings.CALENDAR_DAY_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(35d));
      prefs.put(OpSettings.CALENDAR_HOLIDAYS_LOCATION, OpSettings.CALENDAR_HOLIDAYS_LOCATION_DEFAULT);
      prefs.put(OpSettings.ALLOW_EMPTY_PASSWORD, Boolean.valueOf(OpSettings.ALLOW_EMPTY_PASSWORD_DEFAULT));
      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);
      prefs.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, new Integer(OpSettings.REPORT_REMOVE_TIME_PERIOD_DEFAULT));
      prefs.put(OpSettings.RESOURCE_MAX_AVAILABYLITY, new Double(OpSettings.RESOURCE_MAX_AVAILABYLITY_DEFAULT));
      prefs.put(OpSettings.MILESTONE_CONTROLLING_INTERVAL, new Integer(OpSettings.MILESTONE_CONTROLLING_INTERVAL_DEFALUT));
      prefs.put(OpSettings.SHOW_RESOURCES_IN_HOURS, Boolean.valueOf(OpSettings.SHOW_RESOURCES_IN_HOURS_DEFAULT));

      XMessage request = new XMessage();
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);

      XMessage response = getSettingsService().saveSettings(session, request);
      assertNoError(response);

      XCalendar calendar = (XCalendar) response.getVariables().get(OpProjectConstants.CALENDAR);
      double dwt = calendar.getWorkHoursPerDay();
      assertEquals(7d, dwt, 0);
   }

   /**
    * Test missing or wrong settings
    *
    * @throws Exception if the test fails
    */
   public void testMissingSettings()
        throws Exception {
      HashMap prefs = new HashMap();
      XMessage request = new XMessage();

      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(OpSettings.CALENDAR_DAY_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(OpSettings.CALENDAR_WEEK_WORK_TIME_DEFAULT));

      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, null);
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);
      XMessage response = getSettingsService().saveSettings(session, request);
      assertError(response, OpSettingsError.EMAIL_INCORRECT);

      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(OpSettings.CALENDAR_DAY_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(OpSettings.CALENDAR_WEEK_WORK_TIME_DEFAULT));

      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, "wrong_pattern");
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);
      response = getSettingsService().saveSettings(session, request);
      assertError(response, OpSettingsError.EMAIL_INCORRECT);

      prefs.clear();
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(OpSettings.CALENDAR_DAY_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(OpSettings.CALENDAR_WEEK_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);

      prefs.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, null);
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);
      response = getSettingsService().saveSettings(session, request);
      assertError(response, OpSettingsError.REPORT_REMOVE_TIME_PERIOD_INCORRECT);

      prefs.clear();
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(OpSettings.CALENDAR_DAY_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(OpSettings.CALENDAR_WEEK_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);

      prefs.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, null);
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);
      response = getSettingsService().saveSettings(session, request);
      assertError(response, OpSettingsError.REPORT_REMOVE_TIME_PERIOD_INCORRECT);

      prefs.clear();
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(OpSettings.CALENDAR_DAY_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(OpSettings.CALENDAR_WEEK_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);

      prefs.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, new Integer(-1));
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);
      response = getSettingsService().saveSettings(session, request);
      assertError(response, OpSettingsError.REPORT_REMOVE_TIME_PERIOD_INCORRECT);

      prefs.clear();
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(OpSettings.CALENDAR_DAY_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(OpSettings.CALENDAR_WEEK_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);
      prefs.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, new Integer(OpSettings.REPORT_REMOVE_TIME_PERIOD_DEFAULT));

      prefs.put(OpSettings.RESOURCE_MAX_AVAILABYLITY, null);
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);
      response = getSettingsService().saveSettings(session, request);
      assertError(response, OpSettingsError.RESOURCE_MAX_AVAILABILITY_INCORRECT);

      prefs.clear();
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(OpSettings.CALENDAR_DAY_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(OpSettings.CALENDAR_WEEK_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);
      prefs.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, new Integer(OpSettings.REPORT_REMOVE_TIME_PERIOD_DEFAULT));

      prefs.clear();
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, OpSettings.CALENDAR_FIRST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, OpSettings.CALENDAR_LAST_WORKDAY_DEFAULT);
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(OpSettings.CALENDAR_DAY_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(OpSettings.CALENDAR_WEEK_WORK_TIME_DEFAULT));
      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);
      prefs.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, new Integer(OpSettings.REPORT_REMOVE_TIME_PERIOD_DEFAULT));

      prefs.put(OpSettings.RESOURCE_MAX_AVAILABYLITY, new Double("-1"));
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);
      response = getSettingsService().saveSettings(session, request);
      assertError(response, OpSettingsError.RESOURCE_MAX_AVAILABILITY_INCORRECT);
   }

   /**
    * Test settings retrieval
    *
    * @throws Exception if the test fails
    */
   public void testGetSettings()
        throws Exception {

      String oldLocale = OpSettings.get(OpSettings.USER_LOCALE);
      String newLocale = "en".equals(oldLocale) ? "de" : "en";
      Double maxAvailability = new Double("200");

      HashMap prefs = new HashMap();
      prefs.put(OpSettings.USER_LOCALE, newLocale);
      prefs.put(OpSettings.CALENDAR_FIRST_WORKDAY, Integer.toString(XCalendar.TUESDAY));
      prefs.put(OpSettings.CALENDAR_LAST_WORKDAY, Integer.toString(XCalendar.WEDNESDAY));
      prefs.put(OpSettings.CALENDAR_DAY_WORK_TIME, new Double(5d));
      prefs.put(OpSettings.CALENDAR_WEEK_WORK_TIME, new Double(12d));
      prefs.put(OpSettings.CALENDAR_HOLIDAYS_LOCATION, "us");
      prefs.put(OpSettings.ALLOW_EMPTY_PASSWORD, Boolean.TRUE);
      prefs.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, "john.doe@unit.organization.domain");
      prefs.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, new Integer(3));
      prefs.put(OpSettings.RESOURCE_MAX_AVAILABYLITY, maxAvailability);
      prefs.put(OpSettings.MILESTONE_CONTROLLING_INTERVAL, new Integer(1));
      prefs.put(OpSettings.SHOW_RESOURCES_IN_HOURS, Boolean.TRUE);

      XMessage request = new XMessage();
      request.setArgument(OpSettingsService.NEW_SETTINGS, prefs);
      XMessage response = getSettingsService().saveSettings(session, request);
      assertNoError(response);

      getSettingsService().loadSettings(session, null);

      assertEquals("de", OpSettings.get(OpSettings.USER_LOCALE));
      assertEquals(Integer.toString(XCalendar.TUESDAY), OpSettings.get(OpSettings.CALENDAR_FIRST_WORKDAY));
      assertEquals(Integer.toString(XCalendar.WEDNESDAY), OpSettings.get(OpSettings.CALENDAR_LAST_WORKDAY));
      assertEquals("6.0", OpSettings.get(OpSettings.CALENDAR_DAY_WORK_TIME));
      assertEquals("12.0", OpSettings.get(OpSettings.CALENDAR_WEEK_WORK_TIME));
      assertEquals("us", OpSettings.get(OpSettings.CALENDAR_HOLIDAYS_LOCATION));
      assertEquals("true", OpSettings.get(OpSettings.ALLOW_EMPTY_PASSWORD));
      assertEquals("john.doe@unit.organization.domain", OpSettings.get(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS));
      assertEquals("3", OpSettings.get(OpSettings.REPORT_REMOVE_TIME_PERIOD));
      assertEquals(maxAvailability.toString(), OpSettings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
      assertEquals("1", OpSettings.get(OpSettings.MILESTONE_CONTROLLING_INTERVAL));
      assertEquals("true", OpSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS));
   }
}
