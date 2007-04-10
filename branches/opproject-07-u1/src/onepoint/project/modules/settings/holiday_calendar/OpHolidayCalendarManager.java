/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.settings.holiday_calendar;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for the holiday calendar dates
 *
 * @author ovidiu.lupas
 */
public class OpHolidayCalendarManager {

   /**
    * Map of holidays. key is locale of form {$au} and value is a OpHolidayCalendarManager manager
    */
   private static Map holidayMap;

   public static void addHolidayCalendar(OpHolidayCalendar holidayCalendar) {
      if (holidayMap == null) {
         holidayMap = new HashMap();
      }
      holidayMap.put(holidayCalendar.getLocation(), holidayCalendar);
   }

   /**
    * Gets the holiday map.
    * key is locale of form {$au} and value is a OpHolidayCalendar.
    *
    * @return holiday map
    */
   public static Map getHolidayCalendarsMap() {
      return holidayMap;
   }

   public static void clearHolidayCalendarsMap() {
      if (holidayMap != null) {
         holidayMap.clear();
      }
   }
}
