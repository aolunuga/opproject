/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.schedule.OpScheduler;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendar;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendarLoader;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendarManager;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.util.XCalendar;

import java.io.*;
import java.util.*;

public class OpSettings {

   private static XLog logger = XLogFactory.getLogger(OpSettings.class, true);
   // Available settings
   public static final String USER_LOCALE = "User_Locale";

   public static final String CALENDAR_FIRST_WORKDAY = "Calendar_FirstWorkday";
   public static final String CALENDAR_LAST_WORKDAY = "Calendar_LastWorkday";
   public static final String CALENDAR_DAY_WORK_TIME = "Calendar_DayWorkTime"; // Hours
   public static final String CALENDAR_WEEK_WORK_TIME = "Calendar_WeekWorkTime"; // Hours
   public static final String CALENDAR_HOLIDAYS_LOCATION = "Calendar_HolidaysLocation"; // holiday scheme
   public static final String ALLOW_EMPTY_PASSWORD = "Allow_EmptyPassword";
   public static final String SHOW_RESOURCES_IN_HOURS = "Show_ResourceHours";
   public static final String EMAIL_NOTIFICATION_FROM_ADDRESS = "EMail_FromAddress";
   public static final String REPORT_REMOVE_TIME_PERIOD = "Report_RemoveTimePeriod";
   public static final String MILESTONE_CONTROLLING_INTERVAL = "Milestone_ControllingInterval";
   public static final String RESOURCE_MAX_AVAILABYLITY = "Resource_MaxAvailability";
   /*schedule names */
   public static final String REPORT_ARCHIVE_SCHEDULE_NAME = "ReportArchive_ScheduleName";

   // Global defaults
   public static final String USER_LOCALE_DEFAULT = "de";

   public static final String CALENDAR_FIRST_WORKDAY_DEFAULT = new StringBuffer().append(XCalendar.MONDAY).toString();
   public static final String CALENDAR_LAST_WORKDAY_DEFAULT = new StringBuffer().append(XCalendar.FRIDAY).toString();
   public static final String CALENDAR_DAY_WORK_TIME_DEFAULT = "8";
   public static final String CALENDAR_WEEK_WORK_TIME_DEFAULT = "40";
   public static final String ALLOW_EMPTY_PASSWORD_DEFAULT = Boolean.toString(false);
   public static final String SHOW_RESOURCES_IN_HOURS_DEFAULT = Boolean.toString(false);
   public static final String EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT = "info@onepoint.at";
   public static final String REPORT_ARCHIVE_SCHEDULE_NAME_DEFAULT = "report-archive-schedule";
   public static final String REPORT_REMOVE_TIME_PERIOD_DEFAULT = Integer.toString(8);
   public static final String CALENDAR_HOLIDAYS_LOCATION_DEFAULT = "SelectCalendar";
   public static final String RESOURCE_MAX_AVAILABYLITY_DEFAULT = "100";
   public static final String MILESTONE_CONTROLLING_INTERVAL_DEFALUT = "2";

   /**
    * A query after which the report scheduler can be retrieved.
    */
   public final static String REPORT_ARCHIVE_SCHEDULE_QUERY = "select schedule.ID from OpSchedule as schedule where schedule.Name = '" + REPORT_ARCHIVE_SCHEDULE_NAME_DEFAULT + "'";

   /**
    * Holiday calendars settings.
    */
   private final static String CALENDARS_DIR = "calendars";

   private static HashMap defaults = new HashMap();
   private static HashMap settings = new HashMap();

   /**
    * The map of holiday calendars.
    */
   private static Map holidayCalendars = null;

   /**
    * Settings which are specific to the calendar.
    */
   private static HashMap calendarSettings = new HashMap();

   static {
      // Set defaults
      defaults.put(USER_LOCALE, USER_LOCALE_DEFAULT);

      defaults.put(CALENDAR_FIRST_WORKDAY, CALENDAR_FIRST_WORKDAY_DEFAULT);
      defaults.put(CALENDAR_LAST_WORKDAY, CALENDAR_LAST_WORKDAY_DEFAULT);
      defaults.put(CALENDAR_DAY_WORK_TIME, CALENDAR_DAY_WORK_TIME_DEFAULT);
      defaults.put(CALENDAR_WEEK_WORK_TIME, CALENDAR_WEEK_WORK_TIME_DEFAULT);
      defaults.put(ALLOW_EMPTY_PASSWORD, ALLOW_EMPTY_PASSWORD_DEFAULT);
      defaults.put(SHOW_RESOURCES_IN_HOURS, SHOW_RESOURCES_IN_HOURS_DEFAULT);
      defaults.put(EMAIL_NOTIFICATION_FROM_ADDRESS, EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);
      defaults.put(REPORT_REMOVE_TIME_PERIOD, REPORT_REMOVE_TIME_PERIOD_DEFAULT);
      defaults.put(REPORT_ARCHIVE_SCHEDULE_NAME, REPORT_ARCHIVE_SCHEDULE_NAME_DEFAULT);
      defaults.put(CALENDAR_HOLIDAYS_LOCATION, CALENDAR_HOLIDAYS_LOCATION_DEFAULT);
      defaults.put(RESOURCE_MAX_AVAILABYLITY, RESOURCE_MAX_AVAILABYLITY_DEFAULT);
      defaults.put(MILESTONE_CONTROLLING_INTERVAL, MILESTONE_CONTROLLING_INTERVAL_DEFALUT);
   }

   public static void applySettings(OpProjectSession session) {
      // Apply settings to current environment
      fillWithPlanningSettings(calendarSettings);

      String reportScheduleName =  get(OpSettings.REPORT_ARCHIVE_SCHEDULE_NAME);
      int reportRemoveInterval = Integer.parseInt(OpSettings.get(OpSettings.REPORT_REMOVE_TIME_PERIOD));
      OpScheduler.updateScheduleInterval(session,reportScheduleName,reportRemoveInterval);
   }

   /**
    * Apply all the global settings which are related to the application calendar.
    * The locale specific calendar settings are added when the user signs in.
    */
   public static Map fillWithPlanningSettings(Map calendarSettings) {
      Integer firstWorkday = Integer.valueOf(get(CALENDAR_FIRST_WORKDAY));
      calendarSettings.put(XCalendar.FIRST_WORKDAY_KEY, firstWorkday);

      Integer lastWorkday = Integer.valueOf(get(CALENDAR_LAST_WORKDAY));
      calendarSettings.put(XCalendar.LAST_WORKDAY_KEY, lastWorkday);

      Double dayWorkTime = new Double(get(CALENDAR_DAY_WORK_TIME));
      calendarSettings.put(XCalendar.WORKHOURS_PERDAY_KEY, dayWorkTime);

      Double weekWorkTime = new Double(get(CALENDAR_WEEK_WORK_TIME));
      calendarSettings.put(XCalendar.WORKHOURS_PERWEEK_KEY, weekWorkTime);

      String id = get(CALENDAR_HOLIDAYS_LOCATION);
      TreeSet holidaysDates = new TreeSet();
      if (holidayCalendars != null) {
         OpHolidayCalendar holidayManager = (OpHolidayCalendar) holidayCalendars.get(id);
         if (holidayManager != null) {
            holidaysDates = new TreeSet(holidayManager.getHolidayDates());
         }
      }
      calendarSettings.put(XCalendar.HOLIDAYS_KEY, holidaysDates);
      return calendarSettings;
   }

   public static void loadSettings(OpProjectSession session) {
      //load the holiday calendars
      loadHolidayCalendars();

      // Clear cached settings, load from database and apply
      settings.clear();
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("select setting from OpSetting as setting");
      Iterator result = broker.list(query).iterator();
      OpSetting setting = null;
      while (result.hasNext()) {
         setting = (OpSetting) result.next();
         settings.put(setting.getName(), setting.getValue());
      }
      broker.close();
      // Apply loaded settings
      applySettings(session);
   }

   /**
    * Loads the holiday calendars into the <code>HolidayCalendarManager</code>.
    */
   private static void loadHolidayCalendars() {
      OpHolidayCalendarManager.clearHolidayCalendarsMap();
      List files = getAllHolidayCalendarFiles();
      if (files != null) {
         OpHolidayCalendarLoader loader = new OpHolidayCalendarLoader();
         for (Iterator iterator = files.iterator(); iterator.hasNext();) {
            String file = (String) iterator.next();
            InputStream input = null;
            try {
               input = new FileInputStream(new File(file));
            }
            catch (FileNotFoundException e) {
               System.err.println(file + " not found");
            }
            loader.loadHolidays(input);
         }
      }
      holidayCalendars = OpHolidayCalendarManager.getHolidayCalendarsMap();
   }

   /**
    * Gets a list with all the holiday calendar files.
    * @return a <code>List</code> of <code>String</code> representing the file names of the holiday calendars.
    */
   private static List getAllHolidayCalendarFiles() {
      String path = OpEnvironmentManager.getEnvironmentVariable(onepoint.project.configuration.OpConfiguration.ONEPOINT_HOME);
      path += "/" + CALENDARS_DIR;
      logger.info("Loading calendars from " + path);
      File calendarDir = new File(path);
      String calendarDirPath = calendarDir.getPath();
      String[] files = calendarDir.list(new FilenameFilter() {
         public boolean accept(File dir, String name) {
            return name.indexOf(".ohc.xml") > 0;
         }
      });
      List filePaths = null;
      if (files != null) {

         filePaths = new ArrayList();
         for (int i = 0; i < files.length; i++) {
            String file = files[i];
            filePaths.add(calendarDirPath + "/" + file);
            logger.info("calendar file : " + calendarDirPath + "/" + file);
         }
      }
      return filePaths;
   }

   public static void saveSettings(OpProjectSession session) {
      // Copy settings and compare with stored settings
      HashMap newSettings = (HashMap) settings.clone();
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpQuery query = broker.newQuery("select setting from OpSetting as setting");
      Iterator result = broker.list(query).iterator();
      OpSetting setting = null;
      String newValue = null;
      while (result.hasNext()) {
         setting = (OpSetting) result.next();
         newValue = (String) newSettings.remove(setting.getName());
         if (newValue == null) {
            // Value has been removed: Delete setting from database
            broker.deleteObject(setting);
         }
         else if (!setting.getValue().equals(newValue)) {
            // Value has changed: Update in database
            setting.setValue(newValue);
            broker.updateObject(setting);
         }
      }
      // Insert remaining new settings
      Iterator newNames = newSettings.keySet().iterator();
      Iterator newValues = newSettings.values().iterator();
      while (newNames.hasNext()) {
         setting = new OpSetting();
         setting.setName((String) newNames.next());
         setting.setValue((String) newValues.next());
         broker.makePersistent(setting);
      }
      t.commit();
      broker.close();
   }

   public static void set(String name, String value) {
      logger.info("*** Setting: " + name + " -> " + value);
      if (value == null) {
         settings.remove(value);
      }
      else {
         // Only set if value is different from default
         String defaultValue = (String) defaults.get(name);
         if ((defaultValue != null) && value.trim().equals(defaultValue)) {
            settings.remove(name);
         }
         else {
            settings.put(name, value);
         }
      }
   }

   public static String get(String name) {
      logger.info("=== get-setting " + name);
      String value = (String) settings.get(name);
      logger.info("   value " + value);
      if (value == null) {
         value = (String) defaults.get(name);
      }
      logger.info("   value (default?) " + value);
      return value;
   }

   /**
    * Returns a clone of the calendar settings map.
    * @return a <code>Map</code> of settings related to the calendar.
    */
   public static Map getCalendarSettings() {
      return (Map) calendarSettings.clone();
   }
}
