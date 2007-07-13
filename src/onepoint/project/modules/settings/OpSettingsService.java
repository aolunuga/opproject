/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.error.XErrorMap;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class OpSettingsService extends OpProjectService {

   private static final XLog logger = XLogFactory.getServerLogger(OpSettingsService.class);

   // Form parameters
   public static final String NEW_SETTINGS = "new_settings";

   // Error map
   public final static XErrorMap ERROR_MAP = new OpSettingsErrorMap();

   // email pattern ex : eXpress@onepoint.at
   public static final String EMAIL_REGEX = "^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]";

   public XMessage saveSettings(OpProjectSession session, XMessage request) {
      logger.debug("OpSettingsService.saveSettings()");

      XMessage reply = new XMessage();

      Map<String, Object> settings = (Map) request.getArgument(NEW_SETTINGS);
      Map<String, String> newSettings = new HashMap<String, String>();

      //user locale
      String userLocale = (String) settings.get(OpSettings.USER_LOCALE);
      newSettings.put(OpSettings.USER_LOCALE, userLocale);

      //first/last working day validation
      int firstWorkDay;
      try {
         firstWorkDay = Integer.parseInt((String) settings.get(OpSettings.CALENDAR_FIRST_WORKDAY));
         newSettings.put(OpSettings.CALENDAR_FIRST_WORKDAY, String.valueOf(firstWorkDay));
      }
      catch (NumberFormatException e) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.FIRST_WORK_DAY_INCORRECT));
         return reply;
      }

      int lastWorkDay;
      try {
         lastWorkDay = Integer.parseInt((String) settings.get(OpSettings.CALENDAR_LAST_WORKDAY));
         newSettings.put(OpSettings.CALENDAR_LAST_WORKDAY, String.valueOf(lastWorkDay));
      }
      catch (NumberFormatException e) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.LAST_WORK_DAY_INCORRECT));
         return reply;
      }

      if (firstWorkDay > lastWorkDay) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.LAST_WORK_DAY_INCORRECT));
         return reply;
      }

      //working hours per day validation
      Double dayWorkTime = (Double) settings.get(OpSettings.CALENDAR_DAY_WORK_TIME);
      if (dayWorkTime == null) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.DAY_WORK_TIME_INCORRECT));
         return reply;
      }
      if (dayWorkTime <= 0 || dayWorkTime > 24) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.DAY_WORK_TIME_INCORRECT));
         return reply;
      }
      newSettings.put(OpSettings.CALENDAR_DAY_WORK_TIME, dayWorkTime.toString());

      //week work time validation
      boolean weekWorkChanged = false;
      int workingDaysPerWeek = session.getCalendar().countWeekdays(firstWorkDay, lastWorkDay);
      Double weekWorkTime = (Double) settings.get(OpSettings.CALENDAR_WEEK_WORK_TIME);
      if (weekWorkTime == null) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.WEEK_WORK_TIME_INCORRECT));
         return reply;
      }
      String oldWeekWorkTime = OpSettings.get(OpSettings.CALENDAR_WEEK_WORK_TIME);
      if (oldWeekWorkTime != null) {
         double oldDWeekWorkTime = Double.valueOf(oldWeekWorkTime).doubleValue();
         if (oldDWeekWorkTime != weekWorkTime) {
            weekWorkChanged = true;
         }
      }
      else {
         weekWorkChanged = true;
      }
      if (weekWorkChanged) {
         //change day work time accordingly
         double newDayWorkTime = weekWorkTime / workingDaysPerWeek;
         newSettings.put(OpSettings.CALENDAR_DAY_WORK_TIME, Double.toString(newDayWorkTime));
         dayWorkTime = newDayWorkTime;
         if (dayWorkTime > 24 || dayWorkTime <= 0) {
            reply.setError(session.newError(ERROR_MAP, OpSettingsError.WEEK_WORK_TIME_INCORRECT));
            return reply;
         }
      }
      else {
         newSettings.put(OpSettings.CALENDAR_DAY_WORK_TIME, Double.toString(dayWorkTime));
      }
      //change week work time accordingly to day work time
      double newWeekWorkTime = workingDaysPerWeek * dayWorkTime;
      newSettings.put(OpSettings.CALENDAR_WEEK_WORK_TIME, Double.toString(newWeekWorkTime));

      //email from address validation
      String email = (String) settings.get(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS);
      if (email == null || !Pattern.matches(EMAIL_REGEX, email)) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.EMAIL_INCORRECT));
         return reply;
      }
      newSettings.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, email);

      //reports remove time period validation
      Integer removePeriodValue = (Integer) settings.get(OpSettings.REPORT_REMOVE_TIME_PERIOD);
      if (removePeriodValue == null || removePeriodValue.intValue() <= 0) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.REPORT_REMOVE_TIME_PERIOD_INCORRECT));
         return reply;
      }
      newSettings.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, removePeriodValue.toString());

      //resource max availability validation [0...Byte.MAX_VALUE]
      Double resourceMaxAvailabilityValue = ((Double) settings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
      if (resourceMaxAvailabilityValue == null || resourceMaxAvailabilityValue.doubleValue() <= 0) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.RESOURCE_MAX_AVAILABILITY_INCORRECT));
         return reply;
      }
      newSettings.put(OpSettings.RESOURCE_MAX_AVAILABYLITY, resourceMaxAvailabilityValue.toString());

      //holiday location
      String value = (String) settings.get(OpSettings.CALENDAR_HOLIDAYS_LOCATION);
      String location;
      if (value != null) {
         location = XValidator.choiceID(value);
         newSettings.put(OpSettings.CALENDAR_HOLIDAYS_LOCATION, location);
      }

      //milestone controlling interval
      Integer milestoneControllingValue = (Integer) settings.get(OpSettings.MILESTONE_CONTROLLING_INTERVAL);
      if (milestoneControllingValue == null || milestoneControllingValue.intValue() <= 0) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.MILESTONE_CONTROLING_INCORRECT));
         return reply;
      }
      else {
         newSettings.put(OpSettings.MILESTONE_CONTROLLING_INTERVAL, milestoneControllingValue.toString());
      }

      //Show_ResourceHours
      Boolean showResourceHoursValue = (Boolean) settings.get(OpSettings.SHOW_RESOURCES_IN_HOURS);
      if (showResourceHoursValue != null) {
         newSettings.put(OpSettings.SHOW_RESOURCES_IN_HOURS, showResourceHoursValue.toString());
      }

      //Allow_EmptyPassword
      Boolean allowEmptyPassword = (Boolean) settings.get(OpSettings.ALLOW_EMPTY_PASSWORD);
      if (allowEmptyPassword != null) {
         newSettings.put(OpSettings.ALLOW_EMPTY_PASSWORD, allowEmptyPassword.toString());
      }

      //Pulsing
      Integer pulsing = (Integer) settings.get(OpSettings.PULSING);
      if (pulsing != null) {
         if (pulsing < 0) {
            reply.setError(session.newError(ERROR_MAP, OpSettingsError.INVALID_PULSE_VALUE));
            return reply;
         }
         newSettings.put(OpSettings.PULSING, pulsing.toString());
      }

      //Enable time tracking
      Boolean enableTimeTracking = (Boolean) settings.get(OpSettings.ENABLE_TIME_TRACKING);
      if (enableTimeTracking != null) {
         newSettings.put(OpSettings.ENABLE_TIME_TRACKING, enableTimeTracking.toString());
      }

      //currency symbol
      String currencySymbol = (String) settings.get(OpSettings.CURRENCY_SYMBOL);
      if (currencySymbol != null) {
         newSettings.put(OpSettings.CURRENCY_SYMBOL, currencySymbol);
      }

      //currency shorname
      String currencyShortName = (String) settings.get(OpSettings.CURRENCY_SHORT_NAME);
      if (currencyShortName != null) {
         newSettings.put(OpSettings.CURRENCY_SHORT_NAME, currencyShortName);
      }

      //save the settings in the db
      OpSettings.saveSettings(session, newSettings);

      // Apply new settings
      boolean refresh = OpSettings.applySettings(session);

      OpSettings.configureServerCalendar(session);
      reply.setVariable(OpProjectConstants.CALENDAR, session.getCalendar());
      if (refresh) {
         reply.setArgument(OpProjectConstants.REFRESH_PARAM, Boolean.TRUE);
      }
      return reply;
   }

   public XMessage loadSettings(OpProjectSession session, XMessage request) {
      OpSettings.loadSettings(session);
      return null;
   }
}
