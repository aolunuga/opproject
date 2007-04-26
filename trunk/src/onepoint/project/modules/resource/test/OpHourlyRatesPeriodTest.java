/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource.test;

import onepoint.project.modules.resource.OpHourlyRatesPeriod;
import onepoint.project.modules.resource.OpResourceError;
import onepoint.project.test.OpBaseTestCase;

import java.sql.Date;
import java.util.Calendar;

/**
 * This class tests OpHourlyRatesPeriod isValid() method.
 *
 * @author florin.haizea
 */
public class OpHourlyRatesPeriodTest extends OpBaseTestCase {

   /**
    * Test happy-flow creation of OpHourlyRatesPeriod
    *
    * @throws Exception if the test fails
    */
   public void testValidOpHourlyRatesPeriod()
        throws Exception {

      OpHourlyRatesPeriod period = new OpHourlyRatesPeriod();

      Calendar calendar = Calendar.getInstance();
      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period.setFinish(new Date(calendar.getTimeInMillis()));

      period.setInternalRate(4d);
      period.setInternalRate(6d);
      assertEquals("OpHourlyRatesPeriod.isValid() failed", 0, period.isValid());

      period.setInternalRate(0d);
      period.setExternalRate(0d);
      assertEquals("OpHourlyRatesPeriod.isValid() failed", 0, period.isValid());

      calendar.set(2006, 4, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period.setStart(new Date(calendar.getTimeInMillis()));
      assertEquals("OpHourlyRatesPeriod.isValid() failed", 0, period.isValid());

      calendar.set(2006, 4, 22, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period.setFinish(new Date(calendar.getTimeInMillis()));
      assertEquals("OpHourlyRatesPeriod.isValid() failed", 0, period.isValid());
   }

   /**
    * Test creation of OpHourlyRatesPeriod with non valid fields
    *
    * @throws Exception if the test fails
    */
   public void testInvalidOpHourlyRatesPeriod()
        throws Exception {

      OpHourlyRatesPeriod period = new OpHourlyRatesPeriod();

      Calendar calendar = Calendar.getInstance();
      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period.setFinish(new Date(calendar.getTimeInMillis()));

      period.setInternalRate(-4d);
      period.setExternalRate(6d);
      assertEquals("OpHourlyRatesPeriod.isValid() failed", OpResourceError.HOURLY_RATE_NOT_VALID, period.isValid());

      period.setInternalRate(4d);
      period.setExternalRate(-7d);
      assertEquals("OpHourlyRatesPeriod.isValid() failed", OpResourceError.EXTERNAL_RATE_NOT_VALID, period.isValid());

      period.setExternalRate(7d);
      period.setStart(null);
      assertEquals("OpHourlyRatesPeriod.isValid() failed", OpResourceError.PERIOD_START_DATE_NOT_VALID, period.isValid());

      calendar.set(2006, 4, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period.setStart(new Date(calendar.getTimeInMillis()));
      period.setFinish(null);
      assertEquals("OpHourlyRatesPeriod.isValid() failed", OpResourceError.PERIOD_END_DATE_NOT_VALID, period.isValid());

      calendar.set(2006, 4, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period.setFinish(new Date(calendar.getTimeInMillis()));
      assertEquals("OpHourlyRatesPeriod.isValid() failed", OpResourceError.PERIOD_INTERVAL_NOT_VALID, period.isValid());

      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period.setFinish(new Date(calendar.getTimeInMillis()));
      assertEquals("OpHourlyRatesPeriod.isValid() failed", OpResourceError.PERIOD_INTERVAL_NOT_VALID, period.isValid());
   }
}
