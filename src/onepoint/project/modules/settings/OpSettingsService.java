/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.error.XErrorMap;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
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

      Map<Object, Object> newSettings = (Map) request.getArgument(NEW_SETTINGS);
      // create a copy of the settings to not affect REQUEST content.
      newSettings = newSettings != null ? new HashMap<Object, Object>(newSettings) : null;

      XMessage reply = new XMessage();

      //first/last working day validation
      int firstWorkDay;
      try {
         firstWorkDay = Integer.parseInt((String) newSettings.get(OpSettings.CALENDAR_FIRST_WORKDAY));
      }
      catch (NumberFormatException e) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.FIRST_WORK_DAY_INCORRECT));
         return reply;
      }

      int lastWorkDay;
      try {
         lastWorkDay = Integer.parseInt((String) newSettings.get(OpSettings.CALENDAR_LAST_WORKDAY));
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
      double dayWorkTime;
      String oldValue;
      Double dayWorkTimeDouble = (Double) newSettings.get(OpSettings.CALENDAR_DAY_WORK_TIME);
      if (dayWorkTimeDouble == null) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.DAY_WORK_TIME_INCORRECT));
         return reply;
      }
      dayWorkTime = dayWorkTimeDouble.doubleValue();
      if (dayWorkTime <= 0 || dayWorkTime > 24) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.DAY_WORK_TIME_INCORRECT));
         return reply;
      }

      //week work time validation
      boolean weekWorkChanged = false;
      int workingDaysPerWeek = session.getCalendar().countWeekdays(firstWorkDay, lastWorkDay);
      double weekWorkTime;
      Double weekWorkTimeDouble = (Double) newSettings.get(OpSettings.CALENDAR_WEEK_WORK_TIME);
      if (weekWorkTimeDouble == null) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.WEEK_WORK_TIME_INCORRECT));
         return reply;
      }
      weekWorkTime = weekWorkTimeDouble.doubleValue();
      oldValue = OpSettings.get(OpSettings.CALENDAR_WEEK_WORK_TIME);
      if (oldValue != null) {
         double oldDWeekWorkTime = Double.valueOf(oldValue).doubleValue();
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
      String email = (String) newSettings.get(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS);
      if (email == null || !Pattern.matches(EMAIL_REGEX, email)) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.EMAIL_INCORRECT));
         return reply;
      }

      //reports remove time period validation
      Integer removePeriodValue = (Integer) newSettings.get(OpSettings.REPORT_REMOVE_TIME_PERIOD);
      if (removePeriodValue == null || removePeriodValue.intValue() <= 0) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.REPORT_REMOVE_TIME_PERIOD_INCORRECT));
         return reply;
      }
      newSettings.put(OpSettings.REPORT_REMOVE_TIME_PERIOD, removePeriodValue.toString());

      //resource max availability validation [0...Byte.MAX_VALUE]
      Double resourceMaxAvailabilityValue = ((Double) newSettings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
      if (resourceMaxAvailabilityValue == null || resourceMaxAvailabilityValue.doubleValue() <= 0) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.RESOURCE_MAX_AVAILABILITY_INCORRECT));
         return reply;
      }
      newSettings.put(OpSettings.RESOURCE_MAX_AVAILABYLITY, resourceMaxAvailabilityValue.toString());

      //holiday location
      String value = (String) newSettings.get(OpSettings.CALENDAR_HOLIDAYS_LOCATION);
      String location;
      if (value != null) {
         location = XValidator.choiceID(value);
         newSettings.put(OpSettings.CALENDAR_HOLIDAYS_LOCATION, location);
      }
      else {
         newSettings.put(OpSettings.CALENDAR_HOLIDAYS_LOCATION, null);
      }

      //milestone controlling interval
      Integer milestoneControllingValue = (Integer) newSettings.get(OpSettings.MILESTONE_CONTROLLING_INTERVAL);
      if (milestoneControllingValue == null || milestoneControllingValue.intValue() <= 0) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.MILESTONE_CONTROLING_INCORRECT));
         return reply;
      }
      else {
         newSettings.put(OpSettings.MILESTONE_CONTROLLING_INTERVAL, milestoneControllingValue.toString());
      }

      //Show_ResourceHours
      Boolean showResourceHoursValue = (Boolean) newSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS);
      if (showResourceHoursValue != null) {
         newSettings.put(OpSettings.SHOW_RESOURCES_IN_HOURS, showResourceHoursValue.toString());
      }

      //Allow_EmptyPassword
      Boolean allowEmptyPassword = (Boolean) newSettings.get(OpSettings.ALLOW_EMPTY_PASSWORD);
      if (allowEmptyPassword != null) {
         newSettings.put(OpSettings.ALLOW_EMPTY_PASSWORD, allowEmptyPassword.toString());
      }

      for (Map.Entry<Object, Object> entry1 : newSettings.entrySet()) {
         Map.Entry entry = (Map.Entry) entry1;
         OpSettings.set((String) entry.getKey(), (String) entry.getValue());
      }

      OpSettings.saveSettings(session);

      // Apply new settings
      boolean changedLanguage = OpSettings.applySettings(session);

      OpSettings.configureServerCalendar(session);
      reply.setVariable(OpProjectConstants.CALENDAR, session.getCalendar());

      if (!OpInitializer.isMultiUser() && changedLanguage) {
         reply.setArgument(OpProjectConstants.REFRESH_PARAM, Boolean.TRUE);
      }
      return reply;
   }

   public XMessage loadSettings(OpProjectSession session, XMessage request) {
      OpSettings.loadSettings(session);
      return null;
   }
}
