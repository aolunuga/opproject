/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource.test;

import junit.framework.TestCase;
import onepoint.project.modules.resource.OpHourlyRatesPeriod;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class tests OpResource's getInternalRateForDay()& getExternalRateForDay() methods.
 *
 * @author florin.haizea
 */
public class OpResourceTest extends TestCase {

   /**
    * Test extraction of the internal rate for a given day
    *
    * @throws Exception if the test fails
    */
   public void testGetInternalRateForDay()
        throws Exception {

      Double result;
      Set<OpHourlyRatesPeriod> periodsSet = new HashSet<OpHourlyRatesPeriod>();
      XCalendar calendar = XCalendar.getDefaultCalendar();

      OpResource resource = new OpResource();
      resource.setHourlyRatesPeriods(periodsSet);
      resource.setHourlyRate(0d);
      List<Double> ratesList = resource.getRatesForDay(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 20).getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_INDEX);
      assertEquals("OpResource.getInternalRateForDay failed", 0d, result.doubleValue(), 0d);

      OpHourlyRatesPeriod hourlyRatesPeriod = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(3d);
      hourlyRatesPeriod.setExternalRate(4d);

      hourlyRatesPeriod.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()));
      hourlyRatesPeriod.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 25).getTimeInMillis()));
      ratesList = resource.getRatesForDay(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 20).getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_INDEX);
      assertEquals("OpResource.getInternalRateForDay failed", 3d, result.doubleValue(), 0d);

      hourlyRatesPeriod.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 20).getTimeInMillis()));
      assertEquals("OpResource.getInternalRateForDay failed", 3d, result.doubleValue(), 0d);

      ratesList = resource.getRatesForDay(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 25).getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_INDEX);
      assertEquals("OpResource.getInternalRateForDay failed", 3d, result.doubleValue(), 0d);

      hourlyRatesPeriod.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 21).getTimeInMillis()));
      ratesList = resource.getRatesForDay(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 20).getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_INDEX);
      assertEquals("OpResource.getInternalRateForDay failed", 0d, result.doubleValue(), 0d);
   }

   /**
    * Test extraction of the internal rate for a given interval of time
    *
    * @throws Exception if the test fails
    */
   public void testGetInternalRateForInterval()
        throws Exception {

      List<Double> result;
      Set<OpHourlyRatesPeriod> periodsSet = new HashSet<OpHourlyRatesPeriod>();
      XCalendar calendar = XCalendar.getDefaultCalendar();

      OpResource resource = new OpResource();
      resource.setHourlyRatesPeriods(periodsSet);
      resource.setHourlyRate(0d);
      List<List> ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 20).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 23).getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(0), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(1), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(2), 0d);

      OpHourlyRatesPeriod hourlyRatesPeriod = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(3d);
      hourlyRatesPeriod.setExternalRate(4d);

      hourlyRatesPeriod.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()));
      hourlyRatesPeriod.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 25).getTimeInMillis()));
      ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 24).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 26).getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 3d, result.get(0), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 3d, result.get(1), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(2), 0d);

      hourlyRatesPeriod.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 24).getTimeInMillis()));
      ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 24).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 26).getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 3d, result.get(0), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(1), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(2), 0d);

      ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 3d, result.get(0), 0d);

      hourlyRatesPeriod.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 21).getTimeInMillis()));
      ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(0), 0d);

      ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 16).getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 0, result.size());
   }

   /**
    * Test extraction of the external rate for a day
    *
    * @throws Exception if the test fails
    */
   public void testGetExternalRateForDay()
        throws Exception {

      Double result;
      Set<OpHourlyRatesPeriod> periodsSet = new HashSet<OpHourlyRatesPeriod>();
      XCalendar calendar = XCalendar.getDefaultCalendar();

      OpResource resource = new OpResource();
      resource.setHourlyRatesPeriods(periodsSet);
      resource.setExternalRate(0d);
      List<Double> ratesList = resource.getRatesForDay(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 20).getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_INDEX);
      assertEquals("OpResource.getExternalRateForDay failed", 0d, result.doubleValue(), 0d);

      OpHourlyRatesPeriod hourlyRatesPeriod = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(5d);
      hourlyRatesPeriod.setExternalRate(10d);

      hourlyRatesPeriod.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()));
      hourlyRatesPeriod.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 25).getTimeInMillis()));
      ratesList = resource.getRatesForDay(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 20).getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_INDEX);
      assertEquals("OpResource.getExternalRateForDay failed", 10d, result.doubleValue(), 0d);

      hourlyRatesPeriod.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 20).getTimeInMillis()));
      assertEquals("OpResource.getExternalRateForDay failed", 10d, result.doubleValue(), 0d);

      ratesList = resource.getRatesForDay(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 25).getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_INDEX);
      assertEquals("OpResource.getExternalRateForDay failed", 10d, result.doubleValue(), 0d);

      hourlyRatesPeriod.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 21).getTimeInMillis()));
      ratesList = resource.getRatesForDay(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 20).getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_INDEX);
      assertEquals("OpResource.getExternalRateForDay failed", 0d, result.doubleValue(), 0d);
   }

   /**
    * Test extraction of the internal rate for a given interval of time
    *
    * @throws Exception if the test fails
    */
   public void testGetExternalRateForInterval()
        throws Exception {

      List<Double> result;
      Set<OpHourlyRatesPeriod> periodsSet = new HashSet<OpHourlyRatesPeriod>();
      XCalendar calendar = XCalendar.getDefaultCalendar();

      OpResource resource = new OpResource();
      resource.setHourlyRatesPeriods(periodsSet);
      resource.setHourlyRate(0d);
      List<List> ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 20).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 23).getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(0), 0d);
      assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(1), 0d);
      assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(2), 0d);

      OpHourlyRatesPeriod hourlyRatesPeriod = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(3d);
      hourlyRatesPeriod.setExternalRate(4d);

      hourlyRatesPeriod.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()));
      hourlyRatesPeriod.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 25).getTimeInMillis()));

      ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 24).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 26).getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getExternalRateForInterval failed", 4d, result.get(0), 0d);
      assertEquals("OpResource.getExternalRateForInterval failed", 4d, result.get(1), 0d);
      assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(2), 0d);

      hourlyRatesPeriod.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 24).getTimeInMillis()));
      ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 24).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 26).getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getExternalRateForInterval failed", 4d, result.get(0), 0d);
      assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(1), 0d);
      assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(2), 0d);

      ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getExternalRateForInterval failed", 4d, result.get(0), 0d);

      hourlyRatesPeriod.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 21).getTimeInMillis()));
      ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(0), 0d);

      ratesList = resource.getRatesForInterval(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 19).getTimeInMillis()),
           new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 4, 16).getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getExternalRateForInterval failed", 0, result.size());
   }

   /**
    * Tests the calculation of internal and external rates for a list of days
    * @throws Exception
    */
   public void testGetRatesForListOfDays()
        throws Exception {

      OpResource resource = new OpResource();
      resource.setHourlyRate(2d);
      resource.setExternalRate(4d);
      Set<OpHourlyRatesPeriod> periodsSet = new HashSet<OpHourlyRatesPeriod>();
      resource.setHourlyRatesPeriods(periodsSet);

      List<Date> daysList = new ArrayList<Date>();
      XCalendar calendar = XCalendar.getDefaultCalendar();

      daysList.add(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 8).getTimeInMillis()));
      daysList.add(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()));
      daysList.add(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 13).getTimeInMillis()));
      daysList.add(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 17).getTimeInMillis()));
      daysList.add(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 21).getTimeInMillis()));
      daysList.add(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 22).getTimeInMillis()));
      daysList.add(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 27).getTimeInMillis()));

      //no hourly rates periods were added - all the rates should be the default rates
      List<List> ratesList;
      ratesList = resource.getRatesForListOfDays(daysList);
      assertEquals(2d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(0), 0d);
      assertEquals(4d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(0), 0d);
      assertEquals(2d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(1), 0d);
      assertEquals(4d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(1), 0d);
      assertEquals(2d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(2), 0d);
      assertEquals(4d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(2), 0d);
      assertEquals(2d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(3), 0d);
      assertEquals(4d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(3), 0d);
      assertEquals(2d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(4), 0d);
      assertEquals(4d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(4), 0d);
      assertEquals(2d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(5), 0d);
      assertEquals(4d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(5), 0d);
      assertEquals(2d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(6), 0d);
      assertEquals(4d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(6), 0d);

      OpHourlyRatesPeriod hourlyRatesPeriod1 = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod1);
      hourlyRatesPeriod1.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()));
      hourlyRatesPeriod1.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 15).getTimeInMillis()));
      hourlyRatesPeriod1.setInternalRate(3d);
      hourlyRatesPeriod1.setExternalRate(1d);

      OpHourlyRatesPeriod hourlyRatesPeriod2 = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod2);
      hourlyRatesPeriod2.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 20).getTimeInMillis()));
      hourlyRatesPeriod2.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 22).getTimeInMillis()));
      hourlyRatesPeriod2.setInternalRate(6d);
      hourlyRatesPeriod2.setExternalRate(8d);

      //two hourly rates periods were added
      //6/10/2007 - 6/15/2007 with internal rate: 3 and external rate: 1
      //6/20/2007 - 6/22/2007 with internal rate: 6 and external rate: 8
      ratesList = resource.getRatesForListOfDays(daysList);
      assertEquals(2d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(0), 0d);
      assertEquals(4d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(0), 0d);
      assertEquals(3d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(1), 0d);
      assertEquals(1d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(1), 0d);
      assertEquals(3d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(2), 0d);
      assertEquals(1d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(2), 0d);
      assertEquals(2d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(3), 0d);
      assertEquals(4d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(3), 0d);
      assertEquals(6d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(4), 0d);
      assertEquals(8d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(4), 0d);
      assertEquals(6d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(5), 0d);
      assertEquals(8d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(5), 0d);
      assertEquals(2d, (Double)ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX).get(6), 0d);
      assertEquals(4d, (Double)ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX).get(6), 0d);
   }

   /**
    * Test the overlaping of periods in an OpHourlyRatesPeriod set
    *
    * @throws Exception if the test fails
    */
   public void testPeriodsDoNotOverlap()
        throws Exception {

      OpHourlyRatesPeriod period1 = new OpHourlyRatesPeriod();
      OpHourlyRatesPeriod period2 = new OpHourlyRatesPeriod();
      Set<OpHourlyRatesPeriod> hourlyPeriods = new HashSet<OpHourlyRatesPeriod>();
      XCalendar calendar = XCalendar.getDefaultCalendar();

      period1.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 20).getTimeInMillis()));
      period1.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 27).getTimeInMillis()));

      period2.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 13).getTimeInMillis()));
      period2.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis()));

      hourlyPeriods.add(period1);
      hourlyPeriods.add(period2);
      OpResource resource = new OpResource();
      resource.setHourlyRatesPeriods(hourlyPeriods);

      assertTrue(resource.checkPeriodDoNotOverlap());
   }

   /**
    * Test the overlaping of periods in an OpHourlyRatesPeriod set
    *
    * @throws Exception if the test fails
    */
   public void testPeriodsOverlap()
        throws Exception {

      OpHourlyRatesPeriod period1 = new OpHourlyRatesPeriod();
      OpHourlyRatesPeriod period2 = new OpHourlyRatesPeriod();
      Set<OpHourlyRatesPeriod> hourlyPeriods = new HashSet<OpHourlyRatesPeriod>();
      XCalendar calendar = XCalendar.getDefaultCalendar();

      period1.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 20).getTimeInMillis()));
      period1.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 27).getTimeInMillis()));

      period2.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 22).getTimeInMillis()));
      period2.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 25).getTimeInMillis()));

      hourlyPeriods.add(period1);
      hourlyPeriods.add(period2);
      OpResource resource = new OpResource();
      resource.setHourlyRatesPeriods(hourlyPeriods);

      assertFalse(resource.checkPeriodDoNotOverlap());

      period2.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 20).getTimeInMillis()));
      period2.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 27).getTimeInMillis()));
      assertFalse(resource.checkPeriodDoNotOverlap());

      period2.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 13).getTimeInMillis()));
      period2.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 25).getTimeInMillis()));
      assertFalse(resource.checkPeriodDoNotOverlap());

      period2.setStart(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis()));
      period2.setFinish(new Date(OpBaseOpenTestCase.getCalendarWithExactDaySet(2007, 6, 27).getTimeInMillis()));
      assertFalse(resource.checkPeriodDoNotOverlap());
   }
}