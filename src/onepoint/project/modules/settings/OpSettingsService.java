/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import onepoint.error.XErrorMap;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendarLoader;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendarManager;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.project.util.Pair;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceManager;

public class OpSettingsService extends OpProjectService {


	private static String SETTINGS_SERVICE_NAME = "SettingsService";

	private static final XLog logger = XLogFactory.getLogger(OpSettingsService.class);

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

	private final static String COST_NAMES = "cost_names";

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

		Map<String, Pair<String, byte[]>> newSettings = validateSettings(settings, session);

		//save the settings in the db
		saveSettings(session, newSettings);

		// Apply new settings
		boolean refresh = applySettings(session, true);
		// TODO: set "baseDataChanged" property for all projects and resources using default calendars...
		reply.setVariable(OpProjectConstants.CALENDAR, session.getCalendar());
		if (refresh) {
         touchAllDefaultObjects(session);
			reply.setArgument(OpProjectConstants.REFRESH_PARAM, Boolean.TRUE);
		}
		return reply;
	}

	public void defaultCalendarChanged() {

	}


	protected Map<String, Pair<String, byte[]>> validateSettings(Map<String, Object> settings, OpProjectSession session) {
		Map<String, Pair<String, byte[]>> newSettings = new HashMap<String, Pair<String, byte[]>>();

		//user locale
		String userLocaleId = (String) settings.get(OpSettings.USER_LOCALE_ID);
		add(newSettings, OpSettings.USER_LOCALE_ID, userLocaleId);

		//first/last working day validation
		int firstWorkDay;
		try {
			firstWorkDay = Integer.parseInt((String) settings.get(OpSettings.CALENDAR_FIRST_WORKDAY));
			add(newSettings, OpSettings.CALENDAR_FIRST_WORKDAY, String.valueOf(firstWorkDay));
		}
		catch (NumberFormatException e) {
			throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.FIRST_WORK_DAY_INCORRECT));
		}

		int lastWorkDay;
		try {
			lastWorkDay = Integer.parseInt((String) settings.get(OpSettings.CALENDAR_LAST_WORKDAY));
			add(newSettings, OpSettings.CALENDAR_LAST_WORKDAY, String.valueOf(lastWorkDay));
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
		add(newSettings, OpSettings.CALENDAR_DAY_WORK_TIME, dayWorkTime.toString());

		//week work time validation
		boolean weekWorkChanged = false;
		int workingDaysPerWeek = session.getCalendar().countWeekdays(firstWorkDay, lastWorkDay);
		Double weekWorkTime = (Double) settings.get(OpSettings.CALENDAR_WEEK_WORK_TIME);
		if (weekWorkTime == null) {
			throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.WEEK_WORK_TIME_INCORRECT));
		}

		String oldWeekWorkTime = getStringValue(session, OpSettings.CALENDAR_WEEK_WORK_TIME);
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
			add(newSettings, OpSettings.CALENDAR_DAY_WORK_TIME, Double.toString(newDayWorkTime));
			dayWorkTime = newDayWorkTime;
			if (dayWorkTime > 24 || dayWorkTime <= 0) {
				throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.WEEK_WORK_TIME_INCORRECT));
			}
		}
		else {
			add(newSettings, OpSettings.CALENDAR_DAY_WORK_TIME, Double.toString(dayWorkTime));
		}
		//change week work time accordingly to day work time
		double newWeekWorkTime = workingDaysPerWeek * dayWorkTime;
		add(newSettings, OpSettings.CALENDAR_WEEK_WORK_TIME, Double.toString(newWeekWorkTime));

		//email from address validation
		String email = (String) settings.get(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS);
		if (email == null || !Pattern.matches(EMAIL_REGEX, email)) {
			throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.EMAIL_INCORRECT));
		}
		add(newSettings, OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS, email);

		//resource max availability validation [0...Byte.MAX_VALUE]
		Double resourceMaxAvailabilityValue = ((Double) settings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
		if (resourceMaxAvailabilityValue == null || resourceMaxAvailabilityValue.doubleValue() <= 0) {
			throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.RESOURCE_MAX_AVAILABILITY_INCORRECT));
		}
		add(newSettings, OpSettings.RESOURCE_MAX_AVAILABYLITY, resourceMaxAvailabilityValue.toString());

		//holiday location
		String value = (String) settings.get(OpSettings.CALENDAR_HOLIDAYS_LOCATION);
		String location;
		if (value != null) {
			location = XValidator.choiceID(value);
			add(newSettings, OpSettings.CALENDAR_HOLIDAYS_LOCATION, location);
		}

		//Show_ResourceHours
		Boolean showResourceHoursValue = (Boolean) settings.get(OpSettings.SHOW_RESOURCES_IN_HOURS);
		if (showResourceHoursValue != null) {
			add(newSettings, OpSettings.SHOW_RESOURCES_IN_HOURS, showResourceHoursValue.toString());
		}

		//Allow_EmptyPassword
		Boolean allowEmptyPassword = (Boolean) settings.get(OpSettings.ALLOW_EMPTY_PASSWORD);
		if (allowEmptyPassword != null) {
			add(newSettings, OpSettings.ALLOW_EMPTY_PASSWORD, allowEmptyPassword.toString());
		}

		//Allow_EmptyPassword
		Boolean holidaysAreWorkdays = (Boolean) settings.get(OpSettings.HOLIDAYS_ARE_WORKDAYS);
		if (holidaysAreWorkdays != null) {
			add(newSettings, OpSettings.HOLIDAYS_ARE_WORKDAYS, holidaysAreWorkdays.toString());
		}

		//Pulsing
		Integer pulsing = (Integer) settings.get(OpSettings.PULSING);
		if (pulsing != null) {
			if (pulsing < 0) {
				throw new OpSettingsException(session.newError(ERROR_MAP, OpSettingsError.INVALID_PULSE_VALUE));
			}
			add(newSettings, OpSettings.PULSING, pulsing.toString());
		}

		//Enable progress tracking
		Boolean enableProgressTracking = (Boolean) settings.get(OpSettings.ENABLE_PROGRESS_TRACKING);
		if (enableProgressTracking != null) {
			add(newSettings, OpSettings.ENABLE_PROGRESS_TRACKING, enableProgressTracking.toString());
		}

		//Effort based planning
		Boolean effortBasedPlanning = (Boolean) settings.get(OpSettings.EFFORT_BASED_PLANNING);
		if (effortBasedPlanning != null) {
			add(newSettings, OpSettings.EFFORT_BASED_PLANNING, effortBasedPlanning.toString());
		}

		//Enable time tracking
		Boolean enableTimeTracking = (Boolean) settings.get(OpSettings.ENABLE_TIME_TRACKING);
		if (enableTimeTracking != null) {
			add(newSettings, OpSettings.ENABLE_TIME_TRACKING, enableTimeTracking.toString());
		}

		//show only myWork of contributors
		Boolean showOnlyMyWorkForContributors = (Boolean) settings.get(OpSettings.SHOW_ONLY_MYWORK_FOR_CONTRIBUTOR_USERS);
		if (showOnlyMyWorkForContributors != null) {
			add(newSettings, OpSettings.SHOW_ONLY_MYWORK_FOR_CONTRIBUTOR_USERS, showOnlyMyWorkForContributors.toString());
		}

		//Hide manager features
		Boolean hideManagerFeatures = (Boolean) settings.get(OpSettings.HIDE_MANAGER_FEATURES);
		if (hideManagerFeatures != null) {
			add(newSettings, OpSettings.HIDE_MANAGER_FEATURES, hideManagerFeatures.toString());
		}

		//currency symbol
		String currencySymbol = (String) settings.get(OpSettings.CURRENCY_SYMBOL);
		if (currencySymbol != null) {
			add(newSettings, OpSettings.CURRENCY_SYMBOL, currencySymbol);
		}

		//currency shorname
		String currencyShortName = (String) settings.get(OpSettings.CURRENCY_SHORT_NAME);
		if (currencyShortName != null) {
			add(newSettings, OpSettings.CURRENCY_SHORT_NAME, currencyShortName);
		}

		Double highlyUnderutilized = (Double) settings.get(OpSettings.HIGHLY_UNDERUTILIZED);
		if (highlyUnderutilized != null) {
			add(newSettings, OpSettings.HIGHLY_UNDERUTILIZED, highlyUnderutilized.toString());
		}

		Double underutilized = (Double) settings.get(OpSettings.UNDERUTILIZED);
		if (underutilized != null) {
			add(newSettings, OpSettings.UNDERUTILIZED, underutilized.toString());
		}

		Double overutilized = (Double) settings.get(OpSettings.OVERUTILIZED);
		if (overutilized != null) {
			add(newSettings, OpSettings.OVERUTILIZED, overutilized.toString());
		}

		Double highlyOverutilized = (Double) settings.get(OpSettings.HIGHLY_OVERUTILIZED);
		if (highlyOverutilized != null) {
			add(newSettings, OpSettings.HIGHLY_OVERUTILIZED, highlyOverutilized.toString());
		}

		return newSettings;
	}

	protected static void add(Map<String, Pair<String, byte[]>> newSettings,
			String key, String value) {
		add(newSettings, key, value, null);
	}

	protected static void add(Map<String, Pair<String, byte[]>> newSettings,
			String key, String value, byte[] content) {
		newSettings.put(key, new Pair<String, byte[]>(value, content));
	}

	public XMessage loadSettings(OpProjectSession session, XMessage request) {
		loadSettings(session, true);
		return null;
	}

	/**
	 * Returns a map with the cost types names.
	 *
	 * @param session current session
	 * @param request request object from client
	 * @return XMessage containing map with all the cost types names.
	 */
	public XMessage getCostNames(OpProjectSession session, XMessage request) {
		Map<String, String> i18nParams = getI18NParameters(session);

		Map<String, String> result = new HashMap<String, String>();
		result.put(OpSettings.TRAVEL_COST, i18nParams.get(OpSettings.TRAVEL_COST));
		result.put(OpSettings.SHORT_TRAVEL_COST, i18nParams.get(OpSettings.SHORT_TRAVEL_COST));
		result.put(OpSettings.EXTERNAL_COST, i18nParams.get(OpSettings.EXTERNAL_COST));
		result.put(OpSettings.SHORT_EXTERNAL_COST, i18nParams.get(OpSettings.SHORT_EXTERNAL_COST));
		result.put(OpSettings.MATERIAL_COST, i18nParams.get(OpSettings.MATERIAL_COST));
		result.put(OpSettings.SHORT_MATERIAL_COST, i18nParams.get(OpSettings.SHORT_MATERIAL_COST));
		result.put(OpSettings.MISC_COST, i18nParams.get(OpSettings.MISC_COST));
		result.put(OpSettings.SHORT_MISC_COST, i18nParams.get(OpSettings.SHORT_MISC_COST));

		XMessage reply = new XMessage();
		reply.setArgument(COST_NAMES, result);
		return reply;
	}

	public void loadSettings(OpProjectSession session, boolean startServices) {
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
				Pair<String, OpContent> value = setting.get();
				byte[] content = null;
				if (value.getSecond() != null) {
					content = value.getSecond().toByteArray();
				}
				settings.put(setting.getName(), value.getFirst(), content);
			}
		}
		finally {
			broker.close();
		}
		// Apply loaded settings
		applySettings(session, startServices);
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

	protected boolean applySettings(OpProjectSession session, boolean startSerrvices) {
		boolean refresh = false;
		OpSettings settings = getSettings(session);

		// Apply settings to current environment
		if (settings.adjustPlanningSettings()) {
			refresh = true;
		}

		//update the i18n placeholders
		if (updateLocalizerParameters(session, settings)) {
			refresh = true;
		}

		XLocale newLocale = XLocaleManager.findLocale(getStringValue(session, OpSettings.USER_LOCALE_ID));
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

		configureServerCalendar(session);
      OpProjectCalendarFactory.getInstance().resetCalendars(session);

		return refresh;
	}

	private void touchAllDefaultObjects(OpProjectSession session) {
		OpBroker broker = session.newBroker();
		try {
			OpTransaction tx = broker.newTransaction();
			OpQuery rq = broker.newQuery("select res from OpResource as res where res.WorkCalendar is null");
			Iterator<OpResource> rit = broker.iterate(rq);
			while (rit.hasNext()) {
				rit.next().touch();
			}

			OpQuery pq = broker.newQuery("select p from OpProjectPlan as p where p.WorkCalendar is null");
			Iterator<OpProjectPlan> pit = broker.iterate(pq);
			while (pit.hasNext()) {
				pit.next().touch();
			}
			OpProjectCalendarFactory.getInstance().resetCalendars(session);
			tx.commit();
		}
		finally {
			broker.closeAndEvict();
		}
	}


	/**
	 * Updates the i18n parameters with the new settings values.
	 *
	 * @param session  current session.
	 * @param settings new settings object.
	 * @return true if refresh is required (if the new i18n parameters are different from the old ones)
	 */
	protected boolean updateLocalizerParameters(OpProjectSession session, OpSettings settings) {
		boolean refresh = false;
		Map<String, String> oldLocalizerParameters = session.getLocalizerParameters();
		Map<String, String> newLocalizerParameters = settings.getI18NParameters(session);
		if (!oldLocalizerParameters.equals(newLocalizerParameters)) {
			session.setLocalizerParameters(newLocalizerParameters);
			refresh = true;
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
	protected void saveSettings(OpProjectSession session, Map<String, Pair<String, byte[]>> newSettings) {
		OpSettings settings = getSettings(session);

		//update the settings map with the new settings
		settings.updateSettings(newSettings);

		//update the database with the new settings
		updateDBSettings(session, settings);
	}

	/**
	 * Updates the database settings with values from the newSettings map.
	 *
	 * @param session     current session
	 * @param newSettings new settings object
	 */
	protected void updateDBSettings(OpProjectSession session, OpSettings newSettings) {
		// Copy settings and compare with stored settings
		Map<String, Pair<String, byte[]>> newSettingsMap = newSettings.getSettingsMap();
		Map<String, Pair<String, byte[]>> settingsClone = (Map<String, Pair<String, byte[]>>) ((HashMap) newSettingsMap).clone();
		OpBroker broker = session.newBroker();
		try {
			OpTransaction t = broker.newTransaction();
			OpQuery query = broker.newQuery("select setting from OpSetting as setting");
			Iterator result = broker.iterate(query);
			while (result.hasNext()) {
				OpSetting dbSetting = (OpSetting) result.next();
				Pair<String, byte[]> value = settingsClone.remove(dbSetting.getName());
				// removes notifications !@#$%
				if (value == null) {
					//               //If the db setting name exists in the new map, but has null value => delete from db.
					//               if (newSettingsMap.containsKey(dbSetting.getName())) {
					//                  broker.deleteObject(dbSetting);
					//               }
				} else {
					Pair<String, OpContent> dbSettingValue = dbSetting.get();
					OpContent content = dbSettingValue.getSecond();
					Pair<String, byte[]> oldValue = new Pair<String, byte[]>(dbSettingValue.getFirst(), content == null ? null : content.toByteArray());
					if (!value.equals(oldValue)) {
						// Value has changed: Update in database
						if (content != null) {
							dbSetting.setContent(null); // requered to not get foreign key constraint violation
							OpContentManager.updateContent(content, broker, false, true);
							content = null;
						}
						if (value.getSecond() != null) {
							content = new OpContent(value.getSecond());
							OpContentManager.updateContent(content, broker, true, true);
						}
						dbSetting.setValue(value.getFirst(), content);
					}
				}
			}

			//persist the new settings
			for (String newName : settingsClone.keySet()) {
				Pair<String, byte[]> newValue = settingsClone.get(newName);
				OpContent content = null;
				if (newValue.getSecond() != null) {
					content = new OpContent(newValue.getSecond());
					OpContentManager.updateContent(content, broker, true, true);
				}
				OpSetting setting = new OpSetting(newName, newValue.getFirst(), content);
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
	public String getStringValue(OpBroker broker, String name) {
		return getSettings(broker.getSource().getName()).getStringValue(name);
	}

	/**
	 * Returns value for a given key
	 *
	 * @param session session to use
	 * @param name    name of the key
	 * @return value for the given key
	 */
	public String getStringValue(OpProjectSession session, String name) {
		return getSettings(session).getStringValue(name);
	}

	public byte[] getContent(OpProjectSession session, String name) {
		return getSettings(session).getContent(name);
	}

	public boolean configureServerCalendar(OpProjectSession session) {
		logger.info("Calendar is configured using locale : " + session.getID());
		XLocale locale = session.getLocale();
		TimeZone clientTimezone = session.getClientTimeZone();

		//initialize the calendar instance which will be on the server and also sent to client
		OpProjectCalendar calendar = new OpProjectCalendar();
		XLanguageResourceMap calendarI18nMap = XLocaleManager.findResourceMap(locale.getID(), CALENDAR_RESOURCE_MAP_ID);
		XLocalizer localizer = XLocalizer.getLocalizer(calendarI18nMap);
		calendar.configure(getSettings(session).getPlanningSettings(), locale, localizer, clientTimezone);
		session.setCalendar(calendar);

		return false;
	}

	private Map<String, String> getI18NParameters(OpProjectSession session) {
		return getSettings(session).getI18NParameters(session);
	}

	public static Map<String, String> getI18NParametersMap(OpProjectSession session) {
		OpSettingsService settingsService = getService();
		Map<String, String> localizerParameters;

		if (settingsService == null) {
			OpSettings defaultSettings = new OpSettings();
			localizerParameters = defaultSettings.getI18NParameters(session);
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
	protected OpSettings getSettings(OpProjectSession session) {
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
