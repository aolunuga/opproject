/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
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
import onepoint.util.XCalendar;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

public class OpSettingsService extends OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpSettingsService.class, true);

   // Form parameters
   public static final String NEW_SETTINGS = "new_settings";

   // Error map
   public final static XErrorMap ERROR_MAP = new OpSettingsErrorMap();

   // email pattern ex : eXpress@onepoint.at
   public final String emailRegex = "^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]";

   public XMessage saveSettings(OpProjectSession session, XMessage request) {
      logger.debug("OpSettingsService.saveSettings()");
      Map newSettings = (HashMap) request.getArgument(NEW_SETTINGS);

      XMessage reply = new XMessage();

      /*first/last working day validation*/
      int firstWorkDay = Integer.parseInt((String) newSettings.get(OpSettings.CALENDAR_FIRST_WORKDAY));
      int lastWorkDay = Integer.parseInt((String) newSettings.get(OpSettings.CALENDAR_LAST_WORKDAY));
      if (firstWorkDay > lastWorkDay) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.LAST_WORK_DAY_INCORRECT));
         return reply;
      }

      /*working hours per day validation */
      double dayWorkTime;
      String oldValue;
      double oldDayWorkTime;
      Double dayWorkTimeDouble = (Double) newSettings.get(OpSettings.CALENDAR_DAY_WORK_TIME);
      dayWorkTime = dayWorkTimeDouble.doubleValue();
      if (dayWorkTime <= 0 || dayWorkTime > 24) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.DAY_WORK_TIME_INCORRECT));
         return reply;
      }

      /*week work time validation*/
      boolean weekWorkChanged = false;
      int workingDaysPerWeek = XCalendar.countWeekdays(firstWorkDay, lastWorkDay);
      double weekWorkTime;
      try {
         Double weekWorkTimeDouble = (Double) newSettings.get(OpSettings.CALENDAR_WEEK_WORK_TIME);
         weekWorkTime = weekWorkTimeDouble.doubleValue();
         oldValue = OpSettings.get(OpSettings.CALENDAR_WEEK_WORK_TIME);
         if (oldValue != null) {
            oldDayWorkTime = XCalendar.getDefaultCalendar().parseDouble(oldValue);
            if (oldDayWorkTime != weekWorkTime) {
               weekWorkChanged = true;
            }
         }
         else {
            weekWorkChanged = true;
         }
      }
      catch (ParseException e) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.WEEK_WORK_TIME_INCORRECT));
         return reply;
      }

      if (weekWorkChanged) {
         //change day work time accordingly
         double newDayWorkTime = weekWorkTime/workingDaysPerWeek;
         newSettings.put(OpSettings.CALENDAR_DAY_WORK_TIME, Double.toString(newDayWorkTime));
         dayWorkTime = newDayWorkTime;
         if (dayWorkTime > 24 || dayWorkTime <= 0) {
            reply.setError(session.newError(ERROR_MAP, OpSettingsError.WEEK_WORK_TIME_INCORRECT));
            return reply;
         }
      }
      else {
         //set day work time in settings as string
         newSettings.put(OpSettings.CALENDAR_DAY_WORK_TIME,  Double.toString(dayWorkTime));
      }
      //change week work time accordingly to day work time
      double newWeekWorkTime = workingDaysPerWeek * dayWorkTime;
      newSettings.put(OpSettings.CALENDAR_WEEK_WORK_TIME, Double.toString(newWeekWorkTime));

      /*email from address validation */
      String email = (String)newSettings.get(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS);
      if (!Pattern.matches(emailRegex, email)) {
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.EMAIL_INCORRECT));
         return reply;
      }

      /*reports remove time period validation */
      try{
         int removePeriod = Integer.parseInt((String)newSettings.get(OpSettings.REPORT_REMOVE_TIME_PERIOD));
         if (removePeriod <= 0) {
            reply.setError(session.newError(ERROR_MAP, OpSettingsError.REPORT_REMOVE_TIME_PERIOD_INCORRECT));
            return reply;
         }
      }
      catch(NumberFormatException e){
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.REPORT_REMOVE_TIME_PERIOD_INCORRECT));
         return reply;
      }

      /*resource max availability validation [0...Byte.MAX_VALUE]*/
      try{
         double resourceMaxAvailability = ((Double)newSettings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY)).doubleValue();
         if (resourceMaxAvailability < 0) {
            reply.setError(session.newError(ERROR_MAP, OpSettingsError.RESOURCE_MAX_AVAILABILITY_INCORRECT));
            return reply;
         }
         newSettings.put(OpSettings.RESOURCE_MAX_AVAILABYLITY, Double.toString(resourceMaxAvailability));
      }
      catch(NumberFormatException e){
         reply.setError(session.newError(ERROR_MAP, OpSettingsError.RESOURCE_MAX_AVAILABILITY_INCORRECT));
         return reply;
      }

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


      Iterator iterator = newSettings.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry entry = (Map.Entry)iterator.next();
         OpSettings.set((String)entry.getKey(),(String)entry.getValue());
      }

      OpSettings.saveSettings(session);

      // Apply new settings
      boolean changedLanguage = OpSettings.applySettings(session);

      Map userCalendarSettings = new HashMap();
      OpSettings.fillWithPlanningSettings(userCalendarSettings);
      reply.setVariable(XCalendar.CALENDAR_SETTINGS, userCalendarSettings);

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
