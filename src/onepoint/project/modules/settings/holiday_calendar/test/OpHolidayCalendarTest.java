/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.settings.holiday_calendar.test;

import junit.framework.TestCase;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendar;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendarLoader;

import java.io.InputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Test case for holiday calendar manager
 *
 * @author ovidiu.lupas
 */
public class OpHolidayCalendarTest extends TestCase {
   /*holiday calendar */
   private OpHolidayCalendar holidayCalendar;
   /*holidays test file name*/
   private final String HOLIDAY_DATA_FILE = "holidays_ro_test.ohc.xml";

   public void setUp() throws Exception {
      super.setUp();
      OpHolidayCalendarLoader loader = new OpHolidayCalendarLoader();
      InputStream input = this.getClass().getResourceAsStream(HOLIDAY_DATA_FILE);
      holidayCalendar = loader.loadHolidays(input);
   }

   public void testHolidaysLoading() {
      assertEquals("The number of holidays doesn't match", holidayCalendar.getHolidayDates().size(), 17);
      assertEquals("The number of holidays in 2006 doesn't match", getHolidayDates(holidayCalendar.getHolidayDates(), 2006).size(), 8);
      assertEquals("The number of holidays in 2007 doesn't match", getHolidayDates(holidayCalendar.getHolidayDates(), 2007).size(), 9);
      assertEquals("The number of holidays in 2008 doesn't match", getHolidayDates(holidayCalendar.getHolidayDates(), 2008).size(), 0);
   }

   /**
    * Returns a list of holiday dates with the given <code>year</code>
    *
    * @param source <code>List</code> of holiday dates (java.sql.Date)
    * @param year   <code>int</code> the year
    * @return <code>List</code> of holidays
    */
   private List getHolidayDates(List source, int year) {
      List holidays = new ArrayList();
      Calendar holidayCalendar = Calendar.getInstance();
      for (Iterator it = source.iterator(); it.hasNext();) {
         Date holidayDate = (Date) it.next();
         holidayCalendar.setTimeInMillis(holidayDate.getTime());
         if (holidayCalendar.get(Calendar.YEAR) == year) {
            holidays.add(holidayDate);
         }
      }
      return holidays;
   }
}
