/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.test;

import junit.framework.TestCase;
import onepoint.project.util.OpProjectCalendar;

import java.util.Calendar;

/**
 * Base class for tests that do not need DB access.
 *
 * @author florin.haizea
 */
public class OpTestCase extends TestCase {

   public final double DOUBLE_ERROR_MARGIN = Math.pow(10, -4);

   /**
    * Creates a new instance of test case.
    */
   public OpTestCase() {
   }

   /**
    * Creates a new instance of test case.
    *
    * @param name test case name
    */
   public OpTestCase(String name) {
      super(name);
   }

   /**
    * Returns the GMT calendar set to the year, month and day passed as parameters and hours, minutes, seconds, milliseconds set to 0.
    *
    * @param year  - the year to be set on the calendar
    * @param month - the month to be set on the calendar
    * @param day   - the day to be set on the calendar
    * @return - the GMT calendar set to the year, month and day passed as parameters and hours, minutes, seconds, milliseconds set to 0.
    */
   public static Calendar getCalendarWithExactDaySet(int year, int month, int day) {
      Calendar calendar = OpProjectCalendar.getDefaultProjectCalendar().cloneCalendarInstance();
      calendar.set(year, month, day, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      return calendar;
   }
}
