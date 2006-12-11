/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.settings.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleManager;
import onepoint.project.modules.project_dates.OpProjectDatesModule;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendar;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendarManager;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class OpSettingsFormProvider implements XFormProvider {

   private static XLog logger = XLogFactory.getLogger(OpSettingsFormProvider.class, true);

   public static final String USER_LOCALE_DATA_SET = "UserLocaleDataSet";
   public static final String FIRST_WORKDAY_DATA_SET = "FirstWorkdayDataSet";
   public static final String LAST_WORKDAY_DATA_SET = "LastWorkdayDataSet";
   public static final String HOLIDAYS_DATA_SET = "HolidaysDataSet";

   public static final String USER_LOCALE = "UserLocale";
   public static final String FIRST_WORK_DAY = "FirstWorkday";
   public static final String LAST_WORK_DAY = "LastWorkday";
   public static final String HOLYDAYS = "HolidaysChoice";
   public static final String DAY_WORK_TIME = "DayWorkTime";
   public static final String WEEK_WORK_TIME = "WeekWorkTime";
   public static final String EMAIL_NOTIFICATION_FROM_ADDRESS = "EMailNotificationFromAddress";
   public static final String REPORT_REMOVE_TIME_PERIOD = "ReportsRemoveTimePeriod";
   public static final String RESOURCE_MAX_AVAILABILITY = "ResourceMaxAvailability";
   public static final String MILESTONE_CONTROLLING_INTERVAL = "MilestoneControllingInterval";
   public static final String ALLOW_EMPTY_PASSWORD = "AllowEmptyPassword";
   public static final String SHOW_RESOURCES_IN_HOURS = "ShowResourceHours";
   public static final String SAVE_BUTTON = "Save";
   public static final String SELECT_CALENDAR = "{$SelectCalendar}";

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
      String firstWorkday = OpSettings.get(OpSettings.CALENDAR_FIRST_WORKDAY);
      String lastWorkday = OpSettings.get(OpSettings.CALENDAR_LAST_WORKDAY);
      String lastLocation = OpSettings.get(OpSettings.CALENDAR_HOLIDAYS_LOCATION);

      //fill weekdays data set
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), SETTINGS_WEEKDAYS);
      localizer.setResourceMap(resourceMap);

      fillWeekDataSet(fw_data_set, fw, firstWorkday, localizer);
      fillWeekDataSet(lw_data_set, lw, lastWorkday, localizer);


      resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), SETTINGS_SETTINGS);
      localizer.setResourceMap(resourceMap);
      fillHolidaysDataSet(holidaysDataSet, holidays, lastLocation, localizer);

      XComponent dayWorkTimeField = form.findComponent(DAY_WORK_TIME);
      String dayWorkTimeString = OpSettings.get(OpSettings.CALENDAR_DAY_WORK_TIME);
      double dayWorkTime;
      try {
         dayWorkTime = Double.valueOf(dayWorkTimeString).doubleValue();
      }
      catch (NumberFormatException e) {
         dayWorkTime = 0;
      }
      dayWorkTimeField.setDoubleValue(dayWorkTime);
      logger.debug("   dwt " + dayWorkTimeString);

      XComponent weekWorkTimeField = form.findComponent(WEEK_WORK_TIME);
      String weekWorkTimeString = OpSettings.get(OpSettings.CALENDAR_WEEK_WORK_TIME);
      double weekWorkTime;
      try {
         weekWorkTime = Double.valueOf(weekWorkTimeString).doubleValue();
      }
      catch (NumberFormatException e) {
         weekWorkTime = 0;
      }
      weekWorkTimeField.setDoubleValue(weekWorkTime);

      String resourceMaxAvailability = OpSettings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY);
      XComponent resourceMaxAvailabilityTextField = form.findComponent(RESOURCE_MAX_AVAILABILITY);
      resourceMaxAvailabilityTextField.setStringValue(resourceMaxAvailability);

      String milestoneControllingInterval = OpSettings.get(OpSettings.MILESTONE_CONTROLLING_INTERVAL);
      XComponent milestoneControllingIntervalField = form.findComponent(MILESTONE_CONTROLLING_INTERVAL);
      milestoneControllingIntervalField.setIntValue(Integer.valueOf(milestoneControllingInterval).intValue());
      OpProjectDatesModule projectDatesModule = (OpProjectDatesModule) OpModuleManager.getModuleRegistry().getModule(OpProjectDatesModule.MODULE_NAME);
      if (!projectDatesModule.enableMilestoneControllingIntervalSetting()) {
         milestoneControllingIntervalField.setEnabled(false);
      }

      String emptyPasswordValue = OpSettings.get(OpSettings.ALLOW_EMPTY_PASSWORD);
      XComponent emptyPasswordCheckBox = form.findComponent(ALLOW_EMPTY_PASSWORD);
      emptyPasswordCheckBox.setBooleanValue(Boolean.valueOf(emptyPasswordValue).booleanValue());

      String showHours = OpSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS);
      XComponent showHoursCheckBox = form.findComponent(SHOW_RESOURCES_IN_HOURS);
      showHoursCheckBox.setBooleanValue(Boolean.valueOf(showHours).booleanValue());

      String reportsRemovePeriod = OpSettings.get(OpSettings.REPORT_REMOVE_TIME_PERIOD);
      XComponent reportRemovePeriodTextField = form.findComponent(REPORT_REMOVE_TIME_PERIOD);
      reportRemovePeriodTextField.setIntValue(Integer.valueOf(reportsRemovePeriod).intValue());

      String emailFromAddress = OpSettings.get(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS);
      XComponent mailMessageTextField = form.findComponent(EMAIL_NOTIFICATION_FROM_ADDRESS);
      mailMessageTextField.setStringValue(emailFromAddress);

      // Fill user locale data set
      resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), SETTINGS_SETTINGS);
      localizer.setResourceMap(resourceMap);
      // TODO: Do not hard-code list of available locales here
      // TODO: Maybe sort locales in list according to current language
      String userLocale = OpSettings.get(OpSettings.USER_LOCALE);
      logger.info("***CURRRENT LOCALE '" + userLocale + "'");
      String localeDe = XValidator.choice("de", localizer.localize("{$German}"));
      String localeEn = XValidator.choice("en", localizer.localize("{$English}"));
      XComponent dataSet = form.findComponent(USER_LOCALE_DATA_SET);

      int selectedIndex = -1;
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(localeDe);
      if (userLocale.equals("de")) {
         dataRow.setSelected(true);
         selectedIndex = 0;
      }
      dataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(localeEn);
      if (userLocale.equals("en")) {
         dataRow.setSelected(true);
         selectedIndex = 1;
      }
      dataSet.addChild(dataRow);

      //set up the selected index on choice field based on data row selection index
      XComponent userLocaleChoiceField = form.findComponent(USER_LOCALE);
      userLocaleChoiceField.setSelectedIndex(new Integer(selectedIndex));

      // only administrator can modifiy the settings
      if (!session.userIsAdministrator()) {
         XComponent saveButton = form.findComponent(SAVE_BUTTON);
         userLocaleChoiceField.setEnabled(false);
         fw.setEnabled(false);
         lw.setEnabled(false);
         dayWorkTimeField.setEnabled(false);
         weekWorkTimeField.setEnabled(false);
         mailMessageTextField.setEnabled(false);
         reportRemovePeriodTextField.setEnabled(false);
         emptyPasswordCheckBox.setEnabled(false);
         resourceMaxAvailabilityTextField.setEnabled(false);
         showHoursCheckBox.setEnabled(false);
         holidays.setEnabled(false);
         saveButton.setEnabled(false);
         milestoneControllingIntervalField.setEnabled(false);
      }
   }

   private void fillHolidaysDataSet(XComponent holidaysDataSet, XComponent holidays, String lastLocation, XLocalizer localizer) {
      Map holidayMap = OpHolidayCalendarManager.getHolidayCalendarsMap();
      if (holidayMap != null && !holidayMap.isEmpty()) {
         Set keys = holidayMap.keySet();

         //use an intermediate data set in order to sort
         XComponent sorterDataSet = new XComponent(XComponent.DATA_SET);
         XComponent dataRow;
         for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            String id = (String) iterator.next();
            OpHolidayCalendar manager = (OpHolidayCalendar) holidayMap.get(id);
            String label = manager.getLabel();
            if (label == null) {
               label = " ";
               logger.warn("No holidays label for " + id);
            }
            dataRow = new XComponent(XComponent.DATA_ROW);
            dataRow.setStringValue(XValidator.choice(id, label));
            //sort criteria
            XComponent dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setStringValue(label);
            dataRow.addChild(dataCell);
            sorterDataSet.addChild(dataRow);
         }
         //sort the data
         sorterDataSet.sort(0);

         int selectedIndex = 0;
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(OpSettings.CALENDAR_HOLIDAYS_LOCATION_DEFAULT, localizer.localize(SELECT_CALENDAR)));
         holidaysDataSet.addChild(dataRow);
         for (int i=0; i<sorterDataSet.getChildCount(); i++) {
            dataRow = (XComponent) sorterDataSet.getChild(i);
            dataRow.removeChild(0); //remove extra sorting-criteria
            holidaysDataSet.addChild(dataRow);
            String id = XValidator.choiceID(dataRow.getStringValue());
            if (id.equals(lastLocation)) {
               selectedIndex = i + 1;
            }
         }
         holidays.setSelectedIndex(new Integer(selectedIndex));
         ((XComponent)holidaysDataSet.getChild(selectedIndex)).setSelected(true);
      }
      else {
         holidays.setEnabled(false);
      }
   }

   private void fillWeekDataSet(XComponent data_set, XComponent choice, String orig_value, XLocalizer weekdaysLocalizer) {
      XComponent data_row;
      String val;
      int selectedIndex = -1;
      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + XCalendar.SUNDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("{$Sunday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 0;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + XCalendar.MONDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("{$Monday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 1;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + XCalendar.TUESDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("{$Tuesday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 2;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + XCalendar.WEDNESDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("{$Wednesday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 3;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + XCalendar.THURSDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("{$Thursday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 4;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + XCalendar.FRIDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("{$Friday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 5;
      }
      data_set.addChild(data_row);

      data_row = new XComponent(XComponent.DATA_ROW);
      val = "" + XCalendar.SATURDAY;
      data_row.setStringValue(XValidator.choice(val, weekdaysLocalizer.localize("{$Saturday}")));
      if (orig_value.equals(val)) {
         data_row.setSelected(true);
         selectedIndex = 6;
      }
      data_set.addChild(data_row);

      choice.setSelectedIndex(new Integer(selectedIndex));
   }
}
