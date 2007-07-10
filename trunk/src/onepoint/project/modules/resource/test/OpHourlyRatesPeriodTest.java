/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource.test;

import onepoint.persistence.OpEntityException;
import onepoint.project.modules.resource.OpHourlyRatesPeriod;
import onepoint.project.test.OpTestCase;

import java.sql.Date;

/**
 * This class tests OpHourlyRatesPeriod validate() method.
 *
 * @author florin.haizea
 */
public class OpHourlyRatesPeriodTest extends OpTestCase {

   /**
    * Test happy-flow creation of OpHourlyRatesPeriod
    *
    * @throws Exception if the test fails
    */
   public void testValidOpHourlyRatesPeriod()
        throws Exception {

      OpHourlyRatesPeriod period = new OpHourlyRatesPeriod();

      period.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 20).getTimeInMillis()));
      period.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 27).getTimeInMillis()));

      //start - end: 4/20/2006 - 4/27/2006
      period.setInternalRate(4d);
      period.setInternalRate(6d);
      period.validate();

      //start - end: 4/20/2006 - 4/27/2006
      period.setInternalRate(0d);
      period.setExternalRate(0d);
      period.validate();

      //start - end: 4/20/2006 - 4/20/2006
      period.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 20).getTimeInMillis()));
      period.validate();

      //start - end: 4/21/2006 - 4/22/2006
      period.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 21).getTimeInMillis()));
      period.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 22).getTimeInMillis()));
      period.validate();

      //start - end: 4/21/2006 - 4/25/2006
      period.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 25).getTimeInMillis()));
      period.validate();
   }

   /**
    * Test creation of OpHourlyRatesPeriod with non valid fields
    *
    * @throws Exception if the test fails
    */
   public void testInvalidOpHourlyRatesPeriod()
        throws Exception {

      OpHourlyRatesPeriod period = new OpHourlyRatesPeriod();
      boolean exceptionThrown = false;

      period.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 20).getTimeInMillis()));
      period.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 27).getTimeInMillis()));

      period.setInternalRate(-4d);
      period.setExternalRate(6d);
      try {
         period.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpHourlyRatesPeriod.validate() failed", OpHourlyRatesPeriod.INTERNAL_RATE_NOT_VALID, e.getErrorCode());
      }
      assertTrue("OpHourlyRatesPeriod.validate() failed, exception should have been thrown", exceptionThrown);

      exceptionThrown = false;
      period.setInternalRate(4d);
      period.setExternalRate(-7d);
      try {
         period.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpHourlyRatesPeriod.validate() failed", OpHourlyRatesPeriod.EXTERNAL_RATE_NOT_VALID, e.getErrorCode());
      }
      assertTrue("OpHourlyRatesPeriod.validate() failed, exception should have been thrown", exceptionThrown);

      exceptionThrown = false;
      period.setExternalRate(7d);
      period.setStart(null);
      try {
         period.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpHourlyRatesPeriod.validate() failed", OpHourlyRatesPeriod.PERIOD_START_DATE_NOT_VALID, e.getErrorCode());
      }
      assertTrue("OpHourlyRatesPeriod.validate() failed, exception should have been thrown", exceptionThrown);

      exceptionThrown = false;
      period.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 21).getTimeInMillis()));
      period.setFinish(null);
      try {
         period.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpHourlyRatesPeriod.validate() failed", OpHourlyRatesPeriod.PERIOD_END_DATE_NOT_VALID, e.getErrorCode());
      }
      assertTrue("OpHourlyRatesPeriod.validate() failed, exception should have been thrown", exceptionThrown);

      //start - end: 4/21/2006 - 4/19/2006
      exceptionThrown = false;
      period.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 19).getTimeInMillis()));
      try {
         period.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpHourlyRatesPeriod.validate() failed", OpHourlyRatesPeriod.PERIOD_INTERVAL_NOT_VALID, e.getErrorCode());
      }
      assertTrue("OpHourlyRatesPeriod.validate() failed, exception should have been thrown", exceptionThrown);
   }
}
