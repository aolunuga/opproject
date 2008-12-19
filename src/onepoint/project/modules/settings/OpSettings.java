/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpNewConfigurationHandler;
import onepoint.project.configuration.generated.OpConfig;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendar;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.Pair;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;

public class OpSettings {

   private static final XLog logger = XLogFactory.getLogger(OpSettings.class);
   // Available settings
   public static final String USER_LOCALE_ID = "User_Locale";

   public static final String CALENDAR_FIRST_WORKDAY = "Calendar_FirstWorkday";
   public static final String CALENDAR_LAST_WORKDAY = "Calendar_LastWorkday";
   public static final String CALENDAR_DAY_WORK_TIME = "Calendar_DayWorkTime"; // Hours
   public static final String CALENDAR_WEEK_WORK_TIME = "Calendar_WeekWorkTime"; // Hours
   public static final String CALENDAR_HOLIDAYS_LOCATION = "Calendar_HolidaysLocation"; // holiday scheme
   public static final String ALLOW_EMPTY_PASSWORD = "Allow_EmptyPassword";
   public static final String SHOW_RESOURCES_IN_HOURS = "Show_ResourceHours";
   public static final String EMAIL_NOTIFICATION_FROM_ADDRESS = "EMail_FromAddress";
   public static final String RESOURCE_MAX_AVAILABYLITY = "Resource_MaxAvailability";
   public static final String ENABLE_PROGRESS_TRACKING = "EnabledProgressTracking";
   public static final String EFFORT_BASED_PLANNING = "EffortBasedPlanning";
   public static final String PULSING = "Pulsing";
   public static final String ENABLE_TIME_TRACKING = "EnableTimeTracking";
   public static final String SHOW_ONLY_MYWORK_FOR_CONTRIBUTOR_USERS = "ShowOnlyMyWorkForContributorUsers";
   public static final String HIDE_MANAGER_FEATURES = "HideManagerFeatures";
   public static final String CURRENCY_SYMBOL = "Currency_Symbol";
   public static final String TRAVEL_COST = "Travel";
   public static final String MATERIAL_COST = "Material";
   public static final String EXTERNAL_COST = "External";
   public static final String MISC_COST = "Miscellaneous";
   public static final String SHORT_TRAVEL_COST = "ShortTravel";
   public static final String SHORT_MATERIAL_COST = "ShortMaterial";
   public static final String SHORT_EXTERNAL_COST = "ShortExternal";
   public static final String SHORT_MISC_COST = "ShortMiscellaneous";
   public static final String CURRENCY_SHORT_NAME = "Currency_ShortName";
   public static final String HOLIDAYS_ARE_WORKDAYS = "HolidaysAreWorkdays";
   public static final String HIGHLY_UNDERUTILIZED = "HighlyUnderutilized";
   public static final String UNDERUTILIZED = "Underutilized";
   public static final String HIGHLY_OVERUTILIZED = "HighlyOverutilized";
   public static final String OVERUTILIZED = "Overutilized";

   public static final String CALENDAR_FIRST_WORKDAY_DEFAULT = new StringBuffer().append(Calendar.MONDAY).toString();
   public static final String CALENDAR_LAST_WORKDAY_DEFAULT = new StringBuffer().append(Calendar.FRIDAY).toString();
   public static final String CALENDAR_DAY_WORK_TIME_DEFAULT = "8";
   public static final String CALENDAR_WEEK_WORK_TIME_DEFAULT = "40";
   public static final String ALLOW_EMPTY_PASSWORD_DEFAULT = Boolean.toString(false);
   public static final String SHOW_RESOURCES_IN_HOURS_DEFAULT = Boolean.toString(false);
   public static final String EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT = "info@onepoint.at";
   public static final String CALENDAR_HOLIDAYS_LOCATION_DEFAULT = "select_calendar";
   public static final String ENABLE_PROGRESS_TRACKING_DEFAULT = "true";
   public static final String EFFORT_BASED_PLANNING_DEFAULT = "true";
   public static final String RESOURCE_MAX_AVAILABYLITY_DEFAULT = "100";
   public static final String ENABLE_TIME_TRACKING_DEFAULT = "false";
   public static final String HIDE_MANAGER_FEATURES_DEFAULT = "false";
   public static final String CURRENCY_SYMBOL_DEFAULT = "\u20AC";
   public static final String CURRENCY_SHORT_NAME_DEFAULT = "EUR";
   public static final String EMAIL_NOTIFICATION_TRIGGER_KEY = "email_notification_trigger_key";
   // should get workdays from settings
   public static final String EMAIL_NOTIFICATION_TRIGGER_DEFAULT = "0 0 8am ? * MON-FRI"; // every min: "0 * *  ? * MON-FRI";
   
   /**
    * Name of placeholders in i18n files which are taken from the settings
    */
   private final static String CURRENCY_SYMBOL_I18N_PARAMETER = "CurrencySymbol";

   public static final String SETTING = "OpSetting";

   protected Map<String, Pair<String, byte[]>> defaults = new HashMap<String, Pair<String, byte[]>>();
   public Map<String, Pair<String, byte[]>> settings = new HashMap<String, Pair<String, byte[]>>();

   /**
    * The map of holiday calendars.
    */
   private Map holidayCalendars = null;

   /**
    * Plannig settings which are specific to the calendar.
    */
   private OpProjectCalendar.PlanningSettings planningSettings = null;

   /**
    * Settings i18n resource map.
    */
   private static String SETTINGS_MAP_ID = "settings.settings";

   public OpSettings() {
      // Set defaults
      addDefault(USER_LOCALE_ID, XLocaleManager.getDefaultLocale().getID());
      addDefault(CALENDAR_FIRST_WORKDAY, CALENDAR_FIRST_WORKDAY_DEFAULT);
      addDefault(CALENDAR_LAST_WORKDAY, CALENDAR_LAST_WORKDAY_DEFAULT);
      addDefault(CALENDAR_DAY_WORK_TIME, CALENDAR_DAY_WORK_TIME_DEFAULT);
      addDefault(CALENDAR_WEEK_WORK_TIME, CALENDAR_WEEK_WORK_TIME_DEFAULT);
      addDefault(ALLOW_EMPTY_PASSWORD, ALLOW_EMPTY_PASSWORD_DEFAULT);
      addDefault(SHOW_RESOURCES_IN_HOURS, SHOW_RESOURCES_IN_HOURS_DEFAULT);
      addDefault(EMAIL_NOTIFICATION_FROM_ADDRESS, EMAIL_NOTIFICATION_FROM_ADDRESS_DEFAULT);
      addDefault(CALENDAR_HOLIDAYS_LOCATION, CALENDAR_HOLIDAYS_LOCATION_DEFAULT);
      addDefault(ENABLE_PROGRESS_TRACKING, ENABLE_PROGRESS_TRACKING_DEFAULT);
      addDefault(EFFORT_BASED_PLANNING, EFFORT_BASED_PLANNING_DEFAULT);
      addDefault(RESOURCE_MAX_AVAILABYLITY, RESOURCE_MAX_AVAILABYLITY_DEFAULT);
      addDefault(ENABLE_TIME_TRACKING, ENABLE_TIME_TRACKING_DEFAULT);
      addDefault(SHOW_ONLY_MYWORK_FOR_CONTRIBUTOR_USERS, "false");
      addDefault(HIDE_MANAGER_FEATURES, HIDE_MANAGER_FEATURES_DEFAULT);
      addDefault(CURRENCY_SYMBOL, CURRENCY_SYMBOL_DEFAULT);
      addDefault(CURRENCY_SHORT_NAME, CURRENCY_SHORT_NAME_DEFAULT);
      String trigger = EMAIL_NOTIFICATION_TRIGGER_DEFAULT;
      OpConfig opConfiguration = OpNewConfigurationHandler.getInstance().getOpConfiguration();
      if (opConfiguration != null) {
         String notificationTrigger = opConfiguration.getNotificationTrigger();
         if (notificationTrigger != null && notificationTrigger.length() > 0) {
            trigger = notificationTrigger;
         }
      }
      addDefault(EMAIL_NOTIFICATION_TRIGGER_KEY, trigger);
      addDefault(HIGHLY_UNDERUTILIZED, "0.5");
      addDefault(UNDERUTILIZED, "0.8");
      addDefault(OVERUTILIZED, "1");
      addDefault(HIGHLY_OVERUTILIZED, "1.2");
   }

   protected void addDefault(String key, String value) {
	   defaults.put(key, new Pair<String, byte[]>(value, null));
   }

   protected void addSetting(String key, Pair<String, byte[]> value) {
	   settings.put(key, value);
   }

/**
    * Updates the application settings from the given map of new settings.
    *
    * @param newSettings a <code>Map(String, String)</code> of new settings.
    */
   public void updateSettings(Map<String, Pair<String, byte[]>> newSettings) {
      //merge updated/deleted settings
      for (Iterator<String> it = settings.keySet().iterator(); it.hasNext();) {
         String oldName = it.next();
         Pair<String, byte[]> newValue = newSettings.get(oldName);
         addSetting(oldName, newValue);
      }

      //add new settings
      for (String newName : newSettings.keySet()) {
         if (settings.get(newName) == null) {
            addSetting(newName, newSettings.get(newName));
         }
      }
   }

   public String getStringValue(String name) {
      logger.debug("=== get-setting " + name);
      Pair<String, byte[]> value = settings.get(name);
      logger.debug("   value " + value);
      if (value == null) {
         value = defaults.get(name);
      }
      logger.debug("   value (default?) " + value);
      return value == null ? null : value.getFirst();
   }

   public byte[] getContent(String name) {
	      Pair<String, byte[]> value = settings.get(name);
	      if (value == null) {
	         value = defaults.get(name);
	      }
	      return value == null ? null : value.getSecond();
	   }

   public void putStringValue(String name, String value) {
	   put(name, value, null);
   }

   public void put(String name, String value, byte[] content) {
	   put(name, new Pair<String, byte[]>(value, content));
   }

   public void put(String name, Pair<String, byte[]> value) {
	   if (settings == null) {
		   settings = new HashMap<String, Pair<String,byte[]>>();
	   }
	   addSetting(name, value);
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
    * @param session Current session
    * @return a <code>Map(String, String)</code> representing placeholder name, placeholder value pairs.
    * @see onepoint.resource.XLocalizer#localize(String,java.util.Map)
    */
   public Map<String, String> getI18NParameters(OpProjectSession session) {
      Map<String, String> result = new HashMap<String, String>();
      result.put(CURRENCY_SYMBOL_I18N_PARAMETER, getStringValue(CURRENCY_SYMBOL));

      //add the cost types parameters (if no settings value is found, default to the i18n value)
      XLocalizer localizer = new XLocalizer();
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), SETTINGS_MAP_ID);
      localizer.setResourceMap(resourceMap);

      String travel = getStringValue(TRAVEL_COST);
      if (travel == null) {
         travel = localizer.localize("${" + TRAVEL_COST + "}");
      }
      result.put(TRAVEL_COST, travel);
      String shortTravel = getStringValue(SHORT_TRAVEL_COST);
      if (shortTravel == null) {
         shortTravel = localizer.localize("${" + SHORT_TRAVEL_COST + "}");
      }
      result.put(SHORT_TRAVEL_COST, shortTravel);

      String material = getStringValue(MATERIAL_COST);
      if (material == null) {
         material = localizer.localize("${" + MATERIAL_COST + "}");
      }
      result.put(MATERIAL_COST, material);
      String shortMaterial = getStringValue(SHORT_MATERIAL_COST);
      if (shortMaterial == null) {
         shortMaterial = localizer.localize("${" + SHORT_MATERIAL_COST + "}");
      }
      result.put(SHORT_MATERIAL_COST, shortMaterial);

      String external = getStringValue(EXTERNAL_COST);
      if (external == null) {
         external = localizer.localize("${" + EXTERNAL_COST + "}");
      }
      result.put(EXTERNAL_COST, external);
      String shortExternal = getStringValue(SHORT_EXTERNAL_COST);
      if (shortExternal == null) {
         shortExternal = localizer.localize("${" + SHORT_EXTERNAL_COST + "}");
      }
      result.put(SHORT_EXTERNAL_COST, shortExternal);

      String misc = getStringValue(MISC_COST);
      if (misc == null) {
         misc = localizer.localize("${" + MISC_COST + "}");
      }
      result.put(MISC_COST, misc);
      String shortMisc = getStringValue(SHORT_MISC_COST);
      if (shortMisc == null) {
         shortMisc = localizer.localize("${" + SHORT_MISC_COST + "}");
      }
      result.put(SHORT_MISC_COST, shortMisc);

      return result;
   }

   /**
    * Returns the id of the loaded holiday calendar.
    *
    * @return a <code>String</code> the id of the loaded holiday caledar, or null if there isn't any.
    */
   public String getHolidayCalendarId() {
      String id = getStringValue(CALENDAR_HOLIDAYS_LOCATION);
      return !id.equalsIgnoreCase(CALENDAR_HOLIDAYS_LOCATION_DEFAULT) ? id : null;
   }

   public Map getHolidayCalendars() {
      return holidayCalendars;
   }

   public void setHolidayCalendars(Map holidayCalendars) {
      this.holidayCalendars = holidayCalendars;
   }

   public Map<String, Pair<String, byte[]>> getSettingsMap() {
      return settings;
   }

   /**
    * Initializes the planning settings which are to be used by the calendar.
    */
   public boolean adjustPlanningSettings() {
      
      Integer firstWorkday = Integer.valueOf(getStringValue(CALENDAR_FIRST_WORKDAY));
      Integer lastWorkday = Integer.valueOf(getStringValue(CALENDAR_LAST_WORKDAY));
      Double dayWorkTime = new Double(getStringValue(CALENDAR_DAY_WORK_TIME));
      Double weekWorkTime = new Double(getStringValue(CALENDAR_WEEK_WORK_TIME));
      Boolean holidaysAreWorkdays = Boolean.valueOf(getStringValue(HOLIDAYS_ARE_WORKDAYS));

      String id = getStringValue(CALENDAR_HOLIDAYS_LOCATION);
      SortedMap holidaysDates = new TreeMap();
      if (getHolidayCalendars() != null) {
         OpHolidayCalendar holidayCalendar = (OpHolidayCalendar) getHolidayCalendars().get(id);
         if (holidayCalendar != null) {
            holidaysDates = OpProjectCalendar.createHolidayMap(holidayCalendar.getHolidayDates(), OpProjectCalendar.ABSENCE_PUBLIC_HOLIDAY);
         }
      }
            
      OpProjectCalendar.PlanningSettings tmp = new OpProjectCalendar.PlanningSettings(
            firstWorkday.intValue(), lastWorkday.intValue(), dayWorkTime
                  .doubleValue(), weekWorkTime.doubleValue(), holidaysDates,
            getHolidayCalendarId(), holidaysAreWorkdays == null ? false
                  : holidaysAreWorkdays.booleanValue());
      
      boolean changed = !tmp.equals(planningSettings);
      planningSettings = tmp;
      return changed;
   }

   public OpProjectCalendar.PlanningSettings getPlanningSettings() {
      return planningSettings;
   }

   public void setPlanningSettings(OpProjectCalendar.PlanningSettings planningSettings) {
      this.planningSettings = planningSettings;
   }
}
