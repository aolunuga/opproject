/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.util.XCalendar;

import java.io.*;
import java.util.*;

public class OpSettings {

   private static final XLog logger = XLogFactory.getServerLogger(OpSettings.class);
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
   public static final String PULSING = "Pulsing";
   public static final String ENABLE_TIME_TRACKING = "EnableTimeTracking";
   //schedule names
   public static final String REPORT_ARCHIVE_SCHEDULE_NAME = "ReportArchive_ScheduleName";
   public static final String CURRENCY_SYMBOL = "Currency_Symbol";
   public static final String CURRENCY_SHORT_NAME = "Currency_ShortName";

   public static final String CALENDAR_FIRST_WORKDAY_DEFAULT = new StringBuffer().append(Calendar.MONDAY).toString();
   public static final String CALENDAR_LAST_WORKDAY_DEFAULT = new StringBuffer().append(Calendar.FRIDAY).toString();
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
   public static final String ENABLE_TIME_TRACKING_DEFAULT = "false";
   public static final String CURRENCY_SYMBOL_DEFAULT = "€";
   public static final String CURRENCY_SHORT_NAME_DEFAULT = "EUR";

   /**
    * A query after which the report scheduler can be retrieved.
    */
   public final static String REPORT_ARCHIVE_SCHEDULE_QUERY = "select schedule.ID from OpSchedule as schedule where schedule.Name = '" + REPORT_ARCHIVE_SCHEDULE_NAME_DEFAULT + "'";

   /**
    * Holiday calendars settings.
    */
   private final static String CALENDARS_DIR = "calendars";

   private final static String CALENDAR_RESOURCE_MAP_ID = "settings.calendar";

   /**
    * Name of placeholders in i18n files which are taken from the settings
    */
   private final static String CURRENCY_SYMBOL_I18N_PARAMETER = "CurrencySymbol";

   private static Map<String, String> defaults = new HashMap<String, String>();
   private static Map<String, String> settings = new HashMap<String, String>();

   /**
    * The map of holiday calendars.
    */
   private static Map holidayCalendars = null;

   /**
    * Plannig settings which are specific to the calendar.
    */
   private static XCalendar.PlanningSettings planningSettings = null;

   static {
      // Set defaults
      defaults.put(USER_LOCALE, XLocaleManager.DEFAULT_LOCALE.getLanguage());
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
      defaults.put(ENABLE_TIME_TRACKING, ENABLE_TIME_TRACKING_DEFAULT);
      defaults.put(CURRENCY_SYMBOL, CURRENCY_SYMBOL_DEFAULT);
      defaults.put(CURRENCY_SHORT_NAME, CURRENCY_SHORT_NAME_DEFAULT);
   }

   public static boolean applySettings(OpProjectSession session) {
      boolean refresh = false;

      // Apply settings to current environment
      fillPlanningSettings();

      //update the report schedule name
      String reportScheduleName = get(OpSettings.REPORT_ARCHIVE_SCHEDULE_NAME);
      int reportRemoveInterval = Integer.parseInt(OpSettings.get(OpSettings.REPORT_REMOVE_TIME_PERIOD));
      OpScheduler.updateScheduleInterval(session, reportScheduleName, reportRemoveInterval);

      //update the i18n placeholders
      Map<String, String> oldLocalizerParameters = session.getLocalizerParameters();
      Map<String, String> newLocalizerParameters = getI18NParameters();
      if (!oldLocalizerParameters.equals(newLocalizerParameters)) {
         session.setLocalizerParameters(getI18NParameters());
         refresh = true;
      }

      XLocale newLocale = XLocaleManager.findLocale(get(OpSettings.USER_LOCALE));
      boolean changedLanguage = !newLocale.getID().equals(session.getLocale().getID());
      if (!OpEnvironmentManager.isMultiUser() && changedLanguage) {
         session.setLocale(newLocale);
         refresh = true;
      }
      return refresh;
   }

   /**
    * Initializes the planning settings which are to be used by the calendar.
    *
    */
   private static void fillPlanningSettings() {
      Integer firstWorkday = Integer.valueOf(get(CALENDAR_FIRST_WORKDAY));
      Integer lastWorkday = Integer.valueOf(get(CALENDAR_LAST_WORKDAY));
      Double dayWorkTime = new Double(get(CALENDAR_DAY_WORK_TIME));
      Double weekWorkTime = new Double(get(CALENDAR_WEEK_WORK_TIME));

      String id = get(CALENDAR_HOLIDAYS_LOCATION);
      TreeSet holidaysDates = new TreeSet();
      if (holidayCalendars != null) {
         OpHolidayCalendar holidayManager = (OpHolidayCalendar) holidayCalendars.get(id);
         if (holidayManager != null) {
            holidaysDates = new TreeSet(holidayManager.getHolidayDates());
         }
      }
      planningSettings = new XCalendar.PlanningSettings(firstWorkday.intValue(), lastWorkday.intValue(), dayWorkTime.doubleValue(),
           weekWorkTime.doubleValue(), holidaysDates);
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
               logger.error("Could not find file: " + file);
            }
            loader.loadHolidays(input);
         }
      }
      holidayCalendars = OpHolidayCalendarManager.getHolidayCalendarsMap();
   }

   /**
    * Gets a list with all the holiday calendar files.
    *
    * @return a <code>List</code> of <code>String</code> representing the file names of the holiday calendars.
    */
   private static List getAllHolidayCalendarFiles() {
      String path = OpEnvironmentManager.getOnePointHome();
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

   /**
    * Saves the given settings in the database.
    * @param session a <code>OpProjectSession</code> represneting the server session.
    * @param newSettings a <code>Map(String, String)</code> representing the new settings
    * as modified from the outside.
    */
   public static void saveSettings(OpProjectSession session, Map<String, String> newSettings) {
      //update the settings map with the new settings
      updateSettings(newSettings);

      // Copy settings and compare with stored settings
      Map<String, String> settingsClone = (Map<String, String>) ((HashMap) settings).clone();
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpQuery query = broker.newQuery("select setting from OpSetting as setting");
      Iterator result = broker.list(query).iterator();
      while (result.hasNext()) {
         OpSetting setting = (OpSetting) result.next();
         String value = (String) settingsClone.remove(setting.getName());
         if (value == null) {
            // Value has been removed: Delete setting from database
            broker.deleteObject(setting);
         }
         else if (!setting.getValue().equals(value)) {
            // Value has changed: Update in database
            setting.setValue(value);
            broker.updateObject(setting);
         }
      }

      //persist the new settings
      for (String newName : settingsClone.keySet()) {
         String newValue = settingsClone.get(newName);
         OpSetting setting = new OpSetting();
         setting.setName(newName);
         setting.setValue(newValue);
         broker.makePersistent(setting);
      }
      t.commit();
      broker.close();
   }

   /**
    * Updates the application settings from the given map of new settings.
    * @param newSettings a <code>Map(String, String)</code> of new settings.
    */
   private static void updateSettings(Map<String, String> newSettings) {
      //merge updated/deleted settings
      for (Iterator<String> it = settings.keySet().iterator(); it.hasNext();) {
         String oldName = it.next();
         String newValue = newSettings.get(oldName);
         if (newValue == null) {
            it.remove();
         }
         else {
            settings.put(oldName, newValue);
         }
      }

      //add new settings
      for (String newName : newSettings.keySet()) {
         if (settings.get(newName) == null) {
            settings.put(newName, newSettings.get(newName));
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

   public static void configureServerCalendar(OpProjectSession session) {
      logger.info("Calendar is configured using locale : " + session.getID());
      XLocale locale = session.getLocale();
      TimeZone clientTimezone = session.getClientTimeZone();

      //initialize the calendar instance which will be on the server and also sent to client
      XCalendar calendar = new XCalendar();
      XLanguageResourceMap calendarI18nMap = XLocaleManager.findResourceMap(locale.getID(), CALENDAR_RESOURCE_MAP_ID);
      XLocalizer localizer = XLocalizer.getLocalizer(calendarI18nMap);
      calendar.configure(planningSettings, locale, localizer, clientTimezone);
      session.setCalendar(calendar);
   }

   /**
    * Returns a map with the placeholders and actual values of settings used during the
    * i18n process. This map will be passed to the localizer when parsing i18n resources.
    * @return a <code>Map(String, String)</code> representing placeholder name, placeholder
    * value pairs.
    *
    * @see onepoint.resource.XLocalizer#localize(String, java.util.Map)
    */
   public static Map<String, String> getI18NParameters() {
      Map<String, String> result = new HashMap<String, String>();
      result.put(CURRENCY_SYMBOL_I18N_PARAMETER, get(CURRENCY_SYMBOL));
      return result;
   }
}
