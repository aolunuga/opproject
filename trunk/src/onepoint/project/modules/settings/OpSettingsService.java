/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.error.XErrorMap;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendarLoader;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendarManager;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceManager;
import onepoint.util.XCalendar;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class OpSettingsService extends OpProjectService {


   private static String SETTINGS_SERVICE_NAME = "SettingsService";

   private static final XLog logger = XLogFactory.getServerLogger(OpSettingsService.class);

   // Form parameters
   public static final String NEW_SETTINGS = "new_settings";

   // Error map
   public final static XErrorMap ERROR_MAP = new OpSettingsErrorMap();

   // email pattern ex : eXpress@onepoint.at
   public static final String EMAIL_REGEX = "^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]";
   /**
    * Holiday calendars settings.
    */
   public final static String CALENDARS_DIR = "calendars";
   public final static String CALENDAR_RESOURCE_MAP_ID = "settings.calendar";

   /**
    * Gets the registered instance of this service.
    *
    * @return The registered instance of this service.
    */
   public static OpSettingsService getService() {
      return (OpSettingsService) XServiceManager.getService(SETTINGS_SERVICE_NAME);
   }

   // Defines a map of settings (one instance of OpSettings for each source defined)
   private Map<String, OpSettings> settingsMap;

   public OpSettingsService() {
      settingsMap = new HashMap<String, OpSettings>();
   }


   public XMessage saveSettings(OpProjectSession session, XMessage request) {
      logger.debug("OpSettingsService.saveSettings()");

      XMessage reply = new XMessage();

      Map<String, Object> settings = (Map) request.getArgument(NEW_SETTINGS);

      Map<String, String> newSettings = validateSettings(settings, session);

      //save the settings in the db
      saveSettings(session, newSettings);

      // Apply new settings
      boolean refresh = applySettings(session);

      configureServerCalendar(session);
      reply.setVariable(OpProjectConstants.CALENDAR, session.getCalendar());
      if (refresh) {
         reply.setArgument(OpProjectConstants.REFRESH_PARAM, Boolean.TRUE);
      }
      return reply;
   }

   protected Map<String, String> validateSettings(Map<String, Object> settings, OpProjectSession session) {
      Map<String, String> newSettings = new HashMap<String, String>();

      //user locale
      String userLocaleId = (String) settings.get(OpSettings.USER_LOCALE_ID);
      newSettings.put(OpSettings.USER_LOCALE_ID, userLocaleId);

      //first/last working day validation
      int firstWorkDay;
      try {
         firstWorkDay = Integer.parseInt((String) settings.get(OpSettings.CALENDAR_FIRST_WORKDAY));
         newSettings.put(OpSettings.CALENDAR_FIRST_WORKDAY, String.valueOf(firstWorkDay));
      }
      catch (NumberFormatException e) {
         throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.FIRST_WORK_DAY_INCORRECT));
      }

      int lastWorkDay;
      try {
         lastWorkDay = Integer.parseInt((String) settings.get(OpSettings.CALENDAR_LAST_WORKDAY));
         newSettings.put(OpSettings.CALENDAR_LAST_WORKDAY, String.valueOf(lastWorkDay));
      }
      catch (NumberFormatException e) {
         throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.LAST_WORK_DAY_INCORRECT));
      }

      if (firstWorkDay > lastWorkDay) {
         throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.LAST_WORK_DAY_INCORRECT));
      }

      //working hours per day validation
      Double dayWorkTime = (Double) settings.get(OpSettings.CALENDAR_DAY_WORK_TIME);
      if (dayWorkTime == null) {
         throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.DAY_WORK_TIME_INCORRECT));
      }
      if (dayWorkTime <= 0 || dayWorkTime > 24) {
         throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.DAY_WORK_TIME_INCORRECT));
      }
      newSettings.put(OpSettings.CALENDAR_DAY_WORK_TIME, dayWorkTime.toString());

      //week work time validation
      boolean weekWorkChanged = false;
      int workingDaysPerWeek = session.getCalendar().countWeekdays(firstWorkDay, lastWorkDay);
      Double weekWorkTime = (Double) settings.get(OpSettings.CALENDAR_WEEK_WORK_TIME);
      if (weekWorkTime == null) {
         throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.WEEK_WORK_TIME_INCORRECT));
      }

      String oldWeekWorkTime = get(session, OpSettings.CALENDAR_WEEK_WORK_TIME);
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
            throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.WEEK_WORK_TIME_INCORRECT));
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
         throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.EMAIL_INCORRECT));
      }
      newSettings.put(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, email);

      //resource max availability validation [0...Byte.MAX_VALUE]
      Double resourceMaxAvailabilityValue = ((Double) settings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
      if (resourceMaxAvailabilityValue == null || resourceMaxAvailabilityValue.doubleValue() <= 0) {
         throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.RESOURCE_MAX_AVAILABILITY_INCORRECT));
      }
      newSettings.put(OpSettings.RESOURCE_MAX_AVAILABYLITY, resourceMaxAvailabilityValue.toString());

      //holiday location
      String value = (String) settings.get(OpSettings.CALENDAR_HOLIDAYS_LOCATION);
      String location;
      if (value != null) {
         location = XValidator.choiceID(value);
         newSettings.put(OpSettings.CALENDAR_HOLIDAYS_LOCATION, location);
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
            throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.INVALID_PULSE_VALUE));
         }
         newSettings.put(OpSettings.PULSING, pulsing.toString());
      }

      //Enable time tracking
      Boolean enableTimeTracking = (Boolean) settings.get(OpSettings.ENABLE_TIME_TRACKING);
      if (enableTimeTracking != null) {
         newSettings.put(OpSettings.ENABLE_TIME_TRACKING, enableTimeTracking.toString());
      }

      //Hide manager features
      Boolean hideManagerFeatures = (Boolean) settings.get(OpSettings.HIDE_MANAGER_FEATURES);
      if (hideManagerFeatures != null) {
         newSettings.put(OpSettings.HIDE_MANAGER_FEATURES, hideManagerFeatures.toString());
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
      return newSettings;
   }

   public XMessage loadSettings(OpProjectSession session, XMessage request) {
      loadSettings(session);
      return null;
   }

   public void loadSettings(OpProjectSession session) {
      //load the holiday calendars
      loadHolidayCalendars(session);

      // Clear cached settings, load from database and apply
      OpSettings settings = getSettings(session);
      settings.clear();
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select setting from OpSetting as setting");
         Iterator result = broker.iterate(query);
         OpSetting setting = null;
         while (result.hasNext()) {
            setting = (OpSetting) result.next();
            settings.put(setting.getName(), setting.getValue());
         }
      }
      finally {
         broker.close();
      }
      // Apply loaded settings
      applySettings(session);
   }

   /**
    * Loads the holiday calendars into the <code>HolidayCalendarManager</code>.
    */
   private void loadHolidayCalendars(OpProjectSession session) {
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
      getSettings(session).setHolidayCalendars(OpHolidayCalendarManager.getHolidayCalendarsMap());
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

   protected boolean applySettings(OpProjectSession session) {
      boolean refresh = false;
      OpSettings settings = getSettings(session);

      // Apply settings to current environment
      settings.fillPlanningSettings();

      //update the i18n placeholders
      Map<String, String> oldLocalizerParameters = session.getLocalizerParameters();
      Map<String, String> newLocalizerParameters = settings.getI18NParameters();
      if (!oldLocalizerParameters.equals(newLocalizerParameters)) {
         session.setLocalizerParameters(newLocalizerParameters);
         refresh = true;
      }

      XLocale newLocale = XLocaleManager.findLocale(get(session, OpSettings.USER_LOCALE_ID));
      if (newLocale != null) {
         if (session.getLocale() == null) {
            session.setLocale(newLocale);
         }
         else {
            boolean changedLanguage = !newLocale.getID().equals(session.getLocale().getID());
            if (!OpEnvironmentManager.isMultiUser() && changedLanguage) {
               session.setLocale(newLocale);
               refresh = true;
            }
         }
      }
      return refresh;
   }

   /**
    * Saves the given settings in the database.
    *
    * @param session     a <code>OpProjectSession</code> represneting the server session.
    * @param newSettings a <code>Map(String, String)</code> representing the new settings
    *                    as modified from the outside.
    */
   private void saveSettings(OpProjectSession session, Map<String, String> newSettings) {
      OpSettings settings = getSettings(session);

      //update the settings map with the new settings
      settings.updateSettings(newSettings);

      // Copy settings and compare with stored settings
      Map<String, String> settingsClone = (Map<String, String>) ((HashMap) settings.getSettingsMap()).clone();
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();
         OpQuery query = broker.newQuery("select setting from OpSetting as setting");
         Iterator result = broker.iterate(query);
         while (result.hasNext()) {
            OpSetting setting = (OpSetting) result.next();
            String value = settingsClone.remove(setting.getName());
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
            OpSetting setting = new OpSetting(newName, newValue);
            broker.makePersistent(setting);
         }
         t.commit();
      }
      finally {
         broker.close();
      }
   }

   /**
    * Returns value for a given key
    *
    * @param broker broker to use.
    * @param name   name of the key
    * @return value for the given key
    */
   public String get(OpBroker broker, String name) {
      return getSettings(broker.getSource().getName()).get(name);
   }

   /**
    * Returns value for a given key
    *
    * @param session session to use
    * @param name    name of the key
    * @return value for the given key
    */
   public String get(OpProjectSession session, String name) {
      return getSettings(session).get(name);
   }

   public void configureServerCalendar(OpProjectSession session) {
      logger.info("Calendar is configured using locale : " + session.getID());
      XLocale locale = session.getLocale();
      TimeZone clientTimezone = session.getClientTimeZone();

      //initialize the calendar instance which will be on the server and also sent to client
      XCalendar calendar = new XCalendar();
      XLanguageResourceMap calendarI18nMap = XLocaleManager.findResourceMap(locale.getID(), CALENDAR_RESOURCE_MAP_ID);
      XLocalizer localizer = XLocalizer.getLocalizer(calendarI18nMap);
      calendar.configure(getSettings(session).getPlanningSettings(), locale, localizer, clientTimezone);
      session.setCalendar(calendar);
   }

   private Map<String, String> getI18NParameters(OpProjectSession session) {
      return getSettings(session).getI18NParameters();
   }

   public static Map<String, String> getI18NParametersMap(OpProjectSession session) {
      OpSettingsService settingsService = getService();
      Map<String, String> localizerParameters;

      if (settingsService == null) {
         OpSettings defaultSettings = new OpSettings();
         localizerParameters = defaultSettings.getI18NParameters();
      }
      else {
         localizerParameters = settingsService.getI18NParameters(session);
      }
      return localizerParameters;
   }

   /**
    * Returns settings for a given source (this was introduced especially for multi-site case).
    *
    * @param session session from where to get source name.
    * @return an instance of <code>OpSettings</code>
    */
   private OpSettings getSettings(OpProjectSession session) {
      return getSettings(session.getSourceName());
   }

   /**
    * Returns settings for a given source (this was introduced especially for multi-site case).
    *
    * @param sourceName source name for which to get settings
    * @return an instance of <code>OpSettings</code>
    */
   protected OpSettings getSettings(String sourceName) {
      OpSettings settings = settingsMap.get(sourceName);
      if (settings == null) {
         settings = createNewSettingInstance();
         settingsMap.put(sourceName, settings);
      }

      return settings;
   }

   /**
    * Creates a new instance of settings that will be added into global map.
    *
    * @return a new instance.
    */
   protected OpSettings createNewSettingInstance() {
      return new OpSettings();
   }
}
