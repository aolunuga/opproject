/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings.holiday_calendar;

import onepoint.xml.XSchema;

/**
 * Schema class that handles holiday calendar files.
 *
 * @author ovidiu.lupas
 */
public class OpHolidayCalendarSchema extends XSchema {

   public final static String HOLIDAY = "holiday";
   public final static String HOLIDAY_CALENDAR = "holiday-calendar";
   public final static String HOLIDAY_CALENDARS = "holiday-calendars";

   public OpHolidayCalendarSchema() {
      registerNodeHandler(HOLIDAY_CALENDARS, new OpHolidayCalendarsHandler());
      registerNodeHandler(HOLIDAY_CALENDAR, new OpHolidayCalendarHandler());
      registerNodeHandler(HOLIDAY, new OpHolidayHandler());
   }
}
