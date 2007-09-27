/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendar;
import onepoint.resource.XLocaleManager;
import onepoint.util.XCalendar;

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
   public static final String RESOURCE_MAX_AVAILABYLITY = "Resource_MaxAvailability";
   public static final String PULSING = "Pulsing";
   public static final String ENABLE_TIME_TRACKING = "EnableTimeTracking";
   public static final String HIDE_MANAGER_FEATURES = "HideManagerFeatures";
   public static final String CURRENCY_SYMBOL = "Currency_Symbol";
   public static final String CURRENCY_SHORT_NAME = "Currency_ShortName";

   public static final String CALENDAR_FIRST_WORKDAY_DEFAULT = new StringBuffer().append(Calendar.MONDAY).toString();
   public static final String CALENDAR_LAST_WORKDAY_DEFAULT = new StringBuffer().append(Calendar.FRIDAY).toString();
   public static final String CALENDAR_DAY_WORK_TIME_DEFAULT = "8";
   public static final String CALENDAR_WEEK_WORK_TIME_DEFAULT = "40";
   public static final String ALLOW_EMPTY_PASSWORD_DEFAULT = Boolean.toString(false);
   public static final String SHOW_RESOURCES_IN_HOURS_DEFAULT = Boolean.toString(false);
   public static final String EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT = "info@onepoint.at";
   public static final String CALENDAR_HOLIDAYS_LOCATION_DEFAULT = "SelectCalendar";
   public static final String RESOURCE_MAX_AVAILABYLITY_DEFAULT = "100";
   public static final String ENABLE_TIME_TRACKING_DEFAULT = "false";
   public static final String HIDE_MANAGER_FEATURES_DEFAULT = "false";
   public static final String CURRENCY_SYMBOL_DEFAULT = "€";
   public static final String CURRENCY_SHORT_NAME_DEFAULT = "EUR";

   /**
    * Name of placeholders in i18n files which are taken from the settings
    */
   private final static String CURRENCY_SYMBOL_I18N_PARAMETER = "CurrencySymbol";

   protected Map<String, String> defaults = new HashMap<String, String>();
   public Map<String, String> settings = new HashMap<String, String>();

   /**
    * The map of holiday calendars.
    */
   private Map holidayCalendars = null;

   /**
    * Plannig settings which are specific to the calendar.
    */
   private XCalendar.PlanningSettings planningSettings = null;

   public OpSettings() {
      // Set defaults
      defaults.put(USER_LOCALE, XLocaleManager.DEFAULT_LOCALE.getLanguage());
      defaults.put(CALENDAR_FIRST_WORKDAY, CALENDAR_FIRST_WORKDAY_DEFAULT);
      defaults.put(CALENDAR_LAST_WORKDAY, CALENDAR_LAST_WORKDAY_DEFAULT);
      defaults.put(CALENDAR_DAY_WORK_TIME, CALENDAR_DAY_WORK_TIME_DEFAULT);
      defaults.put(CALENDAR_WEEK_WORK_TIME, CALENDAR_WEEK_WORK_TIME_DEFAULT);
      defaults.put(ALLOW_EMPTY_PASSWORD, ALLOW_EMPTY_PASSWORD_DEFAULT);
      defaults.put(SHOW_RESOURCES_IN_HOURS, SHOW_RESOURCES_IN_HOURS_DEFAULT);
      defaults.put(EMAIL_NOTIFICATION_FROM_ADDRESS, EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);
      defaults.put(CALENDAR_HOLIDAYS_LOCATION, CALENDAR_HOLIDAYS_LOCATION_DEFAULT);
      defaults.put(RESOURCE_MAX_AVAILABYLITY, RESOURCE_MAX_AVAILABYLITY_DEFAULT);
      defaults.put(ENABLE_TIME_TRACKING, ENABLE_TIME_TRACKING_DEFAULT);
      defaults.put(HIDE_MANAGER_FEATURES, HIDE_MANAGER_FEATURES_DEFAULT);
      defaults.put(CURRENCY_SYMBOL, CURRENCY_SYMBOL_DEFAULT);
      defaults.put(CURRENCY_SHORT_NAME, CURRENCY_SHORT_NAME_DEFAULT);
   }

   /**
    * Updates the application settings from the given map of new settings.
    *
    * @param newSettings a <code>Map(String, String)</code> of new settings.
    */
   public void updateSettings(Map<String, String> newSettings) {
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

   public String get(String name) {
      logger.debug("=== get-setting " + name);
      String value = settings.get(name);
      logger.debug("   value " + value);
      if (value == null) {
         value = defaults.get(name);
      }
      logger.debug("   value (default?) " + value);
      return value;
   }

   public void put(String name, String value) {
      if (settings == null) {
         settings = new HashMap<String, String>();
      }
      settings.put(name, value);
   }

   public void clear() {
      if (settings != null) {
         settings.clear();
      }
   }

   /**
    * Returns a map with the placeholders and actual values of settings used during the
    * i18n process. This map will be passed to the localizer when parsing i18n resources.
    *
    * @return a <code>Map(String, String)</code> representing placeholder name, placeholder
    *         value pairs.
    * @see onepoint.resource.XLocalizer#localize(String,java.util.Map)
    */
   public Map<String, String> getI18NParameters() {
      Map<String, String> result = new HashMap<String, String>();
      result.put(CURRENCY_SYMBOL_I18N_PARAMETER, get(CURRENCY_SYMBOL));
      return result;
   }

   /**
    * Returns the id of the loaded holiday calendar.
    *
    * @return a <code>String</code> the id of the loaded holiday caledar, or null if there isn't any.
    */
   public String getHolidayCalendarId() {
      String id = get(CALENDAR_HOLIDAYS_LOCATION);
      return !id.equalsIgnoreCase(CALENDAR_HOLIDAYS_LOCATION_DEFAULT) ? id : null;
   }

   public Map getHolidayCalendars() {
      return holidayCalendars;
   }

   public void setHolidayCalendars(Map holidayCalendars) {
      this.holidayCalendars = holidayCalendars;
   }

   public Map<String, String> getSettingsMap() {
      return settings;
   }

   /**
    * Initializes the planning settings which are to be used by the calendar.
    */
   public void fillPlanningSettings() {
      Integer firstWorkday = Integer.valueOf(get(CALENDAR_FIRST_WORKDAY));
      Integer lastWorkday = Integer.valueOf(get(CALENDAR_LAST_WORKDAY));
      Double dayWorkTime = new Double(get(CALENDAR_DAY_WORK_TIME));
      Double weekWorkTime = new Double(get(CALENDAR_WEEK_WORK_TIME));

      String id = get(CALENDAR_HOLIDAYS_LOCATION);
      TreeSet holidaysDates = new TreeSet();
      if (getHolidayCalendars() != null) {
         OpHolidayCalendar holidayManager = (OpHolidayCalendar) getHolidayCalendars().get(id);
         if (holidayManager != null) {
            holidaysDates = new TreeSet(holidayManager.getHolidayDates());
         }
      }

      planningSettings = new XCalendar.PlanningSettings(firstWorkday.intValue(), lastWorkday.intValue(), dayWorkTime.doubleValue(),
           weekWorkTime.doubleValue(), holidaysDates, getHolidayCalendarId());
   }

   public XCalendar.PlanningSettings getPlanningSettings() {
      return planningSettings;
   }

   public void setPlanningSettings(XCalendar.PlanningSettings planningSettings) {
      this.planningSettings = planningSettings;
   }
}
