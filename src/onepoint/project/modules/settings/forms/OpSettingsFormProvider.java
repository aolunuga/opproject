/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings.forms;

import java.util.Calendar;
import java.util.HashMap;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsDataSetFactory;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

/**
 * <FIXME author="Horia Chiorean" description="Refactor the form provider into smaller methods for each setting !">
 */
public class OpSettingsFormProvider extends OpProjectFormProvider {

   private static XLog logger = XLogFactory.getLogger(OpSettingsFormProvider.class);

   private static final String USER_LOCALE_DATA_SET = "UserLocaleDataSet";
   private static final String FIRST_WORKDAY_DATA_SET = "FirstWorkdayDataSet";
   private static final String LAST_WORKDAY_DATA_SET = "LastWorkdayDataSet";
   private static final String HOLIDAYS_DATA_SET = "HolidaysDataSet";

   private static final String USER_LOCALE = "UserLocale";
   private static final String FIRST_WORK_DAY = "FirstWorkday";
   private static final String LAST_WORK_DAY = "LastWorkday";
   private static final String HOLYDAYS = "HolidaysChoice";
   private static final String DAY_WORK_TIME = "DayWorkTime";
   private static final String WEEK_WORK_TIME = "WeekWorkTime";
   private static final String EMAIL_NOTIFICATION_FROM_ADDRESS = "EMailNotificationFromAddress";
   private static final String EMAIL_NOTIFICATION_FROM_ADDRESS_LABEL = "EMailNotificationFromAddressLabel";
   private static final String ENABLE_PROGRESS_TRACKING = "EnabledProgressTracking";
   private static final String EFFORT_BASED_PLANNING = "EffortBasedPlanning";
   private static final String RESOURCE_MAX_AVAILABILITY = "ResourceMaxAvailability";
   private static final String ALLOW_EMPTY_PASSWORD = "AllowEmptyPassword";
   private static final String SHOW_RESOURCES_IN_HOURS = "ShowResourceHours";
   private static final String ENABLE_TIME_TRACKING = "EnableTimeTracking";
   private static final String PULSING = "Pulsing";
   private static final String ENABLE_PULSING = "EnablePulsing";
   private static final String SHOW_ONLY_MYWORK_FOR_CONTRIBUTOR_USERS = "ShowOnlyMyWorkForContributorUsers";
   private static final String HIDE_MANAGER_FEATURES = "HideManagerFeatures";
   private static final String HOLIDAYS_ARE_WORKDAYS = "HolidaysAreWorkdays";

   private static final String SAVE_BUTTON = "Save";
   private static final String CURRENCY_CHOICE_ID = "CurrencyChoice";
   private static final String CURRENCY_DATASET_ID = "CurrencyDataSet";
   private static final String CURRENCY_NAME_SYMBOL_SEPARATOR_ID = "CurrencyNameSymbolSeparator";
   private static final String ORIGINAL_HOLIDAY_CALENDAR = "OriginalHolidayCalendar";

   // Resource map names
   public static final String SETTINGS_SETTINGS = "settings.settings";
   public static final String SETTINGS_WEEKDAYS = "settings.weekdays";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      logger.info("PREPARE_SETTINGS_FORM");

      XLocalizer localizer = new XLocalizer();
      OpProjectSession session = (OpProjectSession) s;

      XComponent fw_data_set = form.findComponent(FIRST_WORKDAY_DATA_SET);
      XComponent lw_data_set = form.findComponent(LAST_WORKDAY_DATA_SET);
      XComponent holidaysDataSet = form.findComponent(HOLIDAYS_DATA_SET);
      XComponent fw = form.findComponent(FIRST_WORK_DAY);
      XComponent lw = form.findComponent(LAST_WORK_DAY);
      XComponent holidays = form.findComponent(HOLYDAYS);
      String firstWorkday = OpSettingsService.getService().getStringValue(session, OpSettings.CALENDAR_FIRST_WORKDAY);
      String lastWorkday = OpSettingsService.getService().getStringValue(session, OpSettings.CALENDAR_LAST_WORKDAY);
      String lastLocation = OpSettingsService.getService().getStringValue(session, OpSettings.CALENDAR_HOLIDAYS_LOCATION);
      form.findComponent(ORIGINAL_HOLIDAY_CALENDAR).setStringValue(lastLocation);

      //fill weekdays data set
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), SETTINGS_WEEKDAYS);
      localizer.setResourceMap(resourceMap);

      fillWeekDataSet(fw_data_set, fw, firstWorkday, localizer);
      fillWeekDataSet(lw_data_set, lw, lastWorkday, localizer);


      resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), SETTINGS_SETTINGS);
      localizer.setResourceMap(resourceMap);
      setupHolidayCalendarChooser(session, holidays, lastLocation);

      fillWorkTime(form, session);

      String resourceMaxAvailability = OpSettingsService.getService().getStringValue(session, OpSettings.RESOURCE_MAX_AVAILABYLITY);
      XComponent resourceMaxAvailabilityTextField = form.findComponent(RESOURCE_MAX_AVAILABILITY);
      double available = 0;
      try {
         available = Double.valueOf(resourceMaxAvailability);
      }
      catch (NumberFormatException e) {
         logger.warn("Error in parsing double number " + resourceMaxAvailability);
      }
      resourceMaxAvailabilityTextField.setDoubleValue(available);

      fillMilestoneSettings(session, form);

      String emptyPasswordValue = OpSettingsService.getService().getStringValue(session, OpSettings.ALLOW_EMPTY_PASSWORD);
      XComponent emptyPasswordCheckBox = form.findComponent(ALLOW_EMPTY_PASSWORD);
      emptyPasswordCheckBox.setBooleanValue(Boolean.valueOf(emptyPasswordValue));

      String showHours = OpSettingsService.getService().getStringValue(session, OpSettings.SHOW_RESOURCES_IN_HOURS);
      XComponent showHoursCheckBox = form.findComponent(SHOW_RESOURCES_IN_HOURS);
      showHoursCheckBox.setBooleanValue(Boolean.valueOf(showHours));

      String emailFromAddress = OpSettingsService.getService().getStringValue(session, OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS);
      XComponent mailMessageTextField = form.findComponent(EMAIL_NOTIFICATION_FROM_ADDRESS);
      mailMessageTextField.setStringValue(emailFromAddress);

      // Fill user locale data set
      XComponent dataSet = form.findComponent(USER_LOCALE_DATA_SET);
      XComponent userLocaleChoiceField = form.findComponent(USER_LOCALE);
      String userXLocaleId = OpSettingsService.getService().getStringValue(session, OpSettings.USER_LOCALE_ID);
      OpSettingsDataSetFactory.fillLanguageDataSet(dataSet, userLocaleChoiceField, userXLocaleId);
      logger.info("***CURRRENT LOCALE '" + userXLocaleId + "'");

      //enable progress tracking
      XComponent enableProgressTracking = form.findComponent(ENABLE_PROGRESS_TRACKING);
      enableProgressTracking.setBooleanValue(Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.ENABLE_PROGRESS_TRACKING)));

      //effort based planning
      XComponent effortBasedPlanning = form.findComponent(EFFORT_BASED_PLANNING);
      effortBasedPlanning.setBooleanValue(Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.EFFORT_BASED_PLANNING)));

      //enable time tracking
      XComponent enableTimeTrackingField = form.findComponent(ENABLE_TIME_TRACKING);
      enableTimeTrackingField.setBooleanValue(Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.ENABLE_TIME_TRACKING)));

      //pulsing
      XComponent enablePulsingField = form.findComponent(ENABLE_PULSING);
      XComponent pulsingField = form.findComponent(PULSING);
      String pulsingValue = OpSettingsService.getService().getStringValue(session, OpSettings.PULSING);
      if (pulsingValue == null) {
         enablePulsingField.setBooleanValue(false);
         pulsingField.setEnabled(false);
      }
      else {
         enablePulsingField.setBooleanValue(true);
         pulsingField.setIntValue(Integer.valueOf(pulsingValue));
      }

      // show only MyWork for contributor users
      XComponent showOnlyMyWorkForContributorUsers = form.findComponent(SHOW_ONLY_MYWORK_FOR_CONTRIBUTOR_USERS);
      if (!OpEnvironmentManager.isMultiUser()) {
         showOnlyMyWorkForContributorUsers.setVisible(false);
      }
      showOnlyMyWorkForContributorUsers.setBooleanValue(Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.SHOW_ONLY_MYWORK_FOR_CONTRIBUTOR_USERS)));
      //hide non manager features
      XComponent hideManagerfeaturesField = form.findComponent(HIDE_MANAGER_FEATURES);
      if (!OpEnvironmentManager.isMultiUser()) {
         hideManagerfeaturesField.setVisible(false);
      }
      else {
         hideManagerfeaturesField.setBooleanValue(Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.HIDE_MANAGER_FEATURES)));
      }

      //currency
      fillCurency(form, session);

      Boolean holidaysAreWorkDays = Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.HOLIDAYS_ARE_WORKDAYS));
      form.findComponent(HOLIDAYS_ARE_WORKDAYS).setBooleanValue(holidaysAreWorkDays == null ? false : holidaysAreWorkDays.booleanValue());

      // hide multi-user fields
      if (!OpEnvironmentManager.isMultiUser()) {
        mailMessageTextField.setVisible(false);
         form.findComponent(EMAIL_NOTIFICATION_FROM_ADDRESS_LABEL).setVisible(false);
         emptyPasswordCheckBox.setVisible(false);
      }

      // only administrator can modifiy the settings
      if (!session.userIsAdministrator()) {
         disableFields(form);
      }
      
   }

   protected void fillMilestoneSettings(OpProjectSession session, XComponent form) {
   }

   private void fillWorkTime(XComponent form, OpProjectSession session) {
      XComponent dayWorkTimeField = form.findComponent(DAY_WORK_TIME);
      String dayWorkTimeString = OpSettingsService.getService().getStringValue(session, OpSettings.CALENDAR_DAY_WORK_TIME);
      double dayWorkTime;
      try {
         dayWorkTime = Double.valueOf(dayWorkTimeString);
      }
      catch (NumberFormatException e) {
         dayWorkTime = 0;
      }
      dayWorkTimeField.setDoubleValue(dayWorkTime);
      logger.debug("   dwt " + dayWorkTimeString);

      XComponent weekWorkTimeField = form.findComponent(WEEK_WORK_TIME);
      String weekWorkTimeString = OpSettingsService.getService().getStringValue(session, OpSettings.CALENDAR_WEEK_WORK_TIME);
      double weekWorkTime;
      try {
         weekWorkTime = Double.valueOf(weekWorkTimeString);
      }
      catch (NumberFormatException e) {
         weekWorkTime = 0;
      }
      weekWorkTimeField.setDoubleValue(weekWorkTime);
   }

   private void fillCurency(XComponent form, OpProjectSession session) {
      String currencyShortName = OpSettingsService.getService().getStringValue(session, OpSettings.CURRENCY_SHORT_NAME);
      XComponent currencyChoice = form.findComponent(CURRENCY_CHOICE_ID);
      String currencySeparator = form.findComponent(CURRENCY_NAME_SYMBOL_SEPARATOR_ID).getStringValue();
      XComponent currencyDataSet = form.findComponent(CURRENCY_DATASET_ID);
      for (int i = 0; i < currencyDataSet.getChildCount(); i++) {
         XComponent currencyRow = (XComponent) currencyDataSet.getChild(i);
         String currencyChoiceId = XValidator.choiceID(currencyRow.getStringValue());
         int symbolShortNameIndex = currencyChoiceId.indexOf(currencySeparator);
         String rowCurrencyShortName = currencyChoiceId.substring(0, symbolShortNameIndex);
         if (currencyShortName.equalsIgnoreCase(rowCurrencyShortName)) {
            currencyRow.setSelected(true);
            currencyChoice.setSelectedIndex(i);
            break;
         }
      }
   }


   protected void disableFields(XComponent form) {
      XComponent saveButton = form.findComponent(SAVE_BUTTON);
      saveButton.setEnabled(false);

      XComponent userLocaleChoiceField = form.findComponent(USER_LOCALE);
      userLocaleChoiceField.setEnabled(false);

      XComponent fw = form.findComponent(FIRST_WORK_DAY);
      fw.setEnabled(false);

      XComponent lw = form.findComponent(LAST_WORK_DAY);
      lw.setEnabled(false);

      XComponent dayWorkTimeField = form.findComponent(DAY_WORK_TIME);
      dayWorkTimeField.setEnabled(false);

      XComponent weekWorkTimeField = form.findComponent(WEEK_WORK_TIME);
      weekWorkTimeField.setEnabled(false);

      XComponent mailMessageTextField = form.findComponent(EMAIL_NOTIFICATION_FROM_ADDRESS);
      mailMessageTextField.setEnabled(false);

      XComponent emptyPasswordCheckBox = form.findComponent(ALLOW_EMPTY_PASSWORD);
      emptyPasswordCheckBox.setEnabled(false);

      XComponent resourceMaxAvailabilityTextField = form.findComponent(RESOURCE_MAX_AVAILABILITY);
      resourceMaxAvailabilityTextField.setEnabled(false);

      XComponent showHoursCheckBox = form.findComponent(SHOW_RESOURCES_IN_HOURS);
      showHoursCheckBox.setEnabled(false);

      XComponent holidays = form.findComponent(HOLYDAYS);
      holidays.setEnabled(false);


      XComponent enableTimeTrackingField = form.findComponent(ENABLE_TIME_TRACKING);
      enableTimeTrackingField.setEnabled(false);

      XComponent enablePulsingField = form.findComponent(ENABLE_PULSING);
      enablePulsingField.setEnabled(false);

      XComponent pulsingField = form.findComponent(PULSING);
      pulsingField.setEnabled(false);

      XComponent currencyChoice = form.findComponent(CURRENCY_CHOICE_ID);
      currencyChoice.setEnabled(false);

      form.findComponent(HOLIDAYS_ARE_WORKDAYS).setEnabled(false);
   }

   private void fillWeekDataSet(XComponent data_set, XComponent choice, String orig_value, XLocalizer weekdaysLocalizer) {
      XComponent data_row;
      String val;
      int selectedIndex = -1;
      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + Calendar.SUNDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("${Sunday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 0;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + Calendar.MONDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("${Monday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 1;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + Calendar.TUESDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("${Tuesday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 2;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + Calendar.WEDNESDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("${Wednesday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 3;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + Calendar.THURSDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("${Thursday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 4;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + Calendar.FRIDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("${Friday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 5;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + Calendar.SATURDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("${Saturday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 6;
      }
      data_set.addChild(data_row);

      choice.setSelectedIndex(selectedIndex);
   }
}
