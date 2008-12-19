/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings.holiday_calendar;

import onepoint.project.util.OpProjectCalendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author ovidiu.lupas
 */
public class OpHolidayCalendarMember {

   static {
      TimeZone.setDefault(OpProjectCalendar.GMT_TIMEZONE);
   }   
   //date format for the holiday dates
   private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd");

   //year for bank holidays
   public int year;

   //list bank holidays dates in this year
   private List holidayDates = new ArrayList();


   public void addHoliday(String holiday) {
      try {
         Date date = DATE_FORMAT.parse(holiday);
         Calendar calendar = OpProjectCalendar.setCalendarTimeToZero(date);
         calendar.set(Calendar.YEAR, year);
         holidayDates.add(new java.sql.Date(calendar.getTimeInMillis()));
      }
      catch (ParseException e) {
         throw new OpHolidayCalendarException("The holiday date cannot be parsed.", e);
      }
   }

   /**
    * Returns the list of holidays for this year.
    *
    * @return <code>List[of java.sql.Date]</code>
    */
   public List getHolidayDates() {
      return holidayDates;
   }

}
