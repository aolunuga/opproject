/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource.test;

import onepoint.project.modules.resource.OpHourlyRatesPeriod;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.test.OpBaseTestCase;

import java.sql.Date;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class tests OpResource's getInternalRateForDay()& getExternalRateForDay() methods.
 *
 * @author florin.haizea
 */
public class OpResourceTest extends OpBaseTestCase {

   /**
    * Test extraction of the internal rate for a given day
    *
    * @throws Exception if the test fails
    */
   public void testGetInternalRateForDay()
        throws Exception {

      Double result;
      Set<OpHourlyRatesPeriod> periodsSet = new HashSet<OpHourlyRatesPeriod>();
      Calendar calendar = Calendar.getInstance();
      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);

      OpResource resource = new OpResource();
      resource.setHourlyRatesPeriods(periodsSet);
      resource.setHourlyRate(0d);
      List<Double> ratesList = resource.getRatesForDay(new Date(calendar.getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_INDEX);
      assertEquals("OpResource.getInternalRateForDay failed", 0d, result.doubleValue(), 0d);

      OpHourlyRatesPeriod hourlyRatesPeriod = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(3d);
      hourlyRatesPeriod.setExternalRate(4d);

      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setFinish(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ratesList = resource.getRatesForDay(new Date(calendar.getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_INDEX);
      assertEquals("OpResource.getInternalRateForDay failed", 3d, result.doubleValue(), 0d);

      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setStart(new Date(calendar.getTimeInMillis()));
      assertEquals("OpResource.getInternalRateForDay failed", 3d, result.doubleValue(), 0d);

      calendar.set(2006, 4, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ratesList = resource.getRatesForDay(new Date(calendar.getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_INDEX);
      assertEquals("OpResource.getInternalRateForDay failed", 3d, result.doubleValue(), 0d);

      calendar.set(2006, 4, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ratesList = resource.getRatesForDay(new Date(calendar.getTimeInMillis()));
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
      Calendar calendarStart = Calendar.getInstance();
      calendarStart.set(2006, 4, 20, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      Calendar calendarEnd = Calendar.getInstance();
      calendarEnd.set(2006, 4, 23, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);
      Calendar calendar = Calendar.getInstance();

      OpResource resource = new OpResource();
      resource.setHourlyRatesPeriods(periodsSet);
      resource.setHourlyRate(0d);
      List<List> ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(0), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(1), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(2), 0d);

      OpHourlyRatesPeriod hourlyRatesPeriod = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(3d);
      hourlyRatesPeriod.setExternalRate(4d);

      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setFinish(new Date(calendar.getTimeInMillis()));
      calendarStart.set(2006, 4, 24, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      calendarEnd.set(2006, 4, 26, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);
      ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 3d, result.get(0), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 3d, result.get(1), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(2), 0d);

      calendar.set(2006, 4, 24, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setFinish(new Date(calendar.getTimeInMillis()));
      ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 3d, result.get(0), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(1), 0d);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(2), 0d);

      calendarStart.set(2006, 4, 19, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      calendarEnd.set(2006, 4, 19, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);
      ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 3d, result.get(0), 0d);

      calendar.set(2006, 4, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setStart(new Date(calendar.getTimeInMillis()));
      ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
      result = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      assertEquals("OpResource.getInternalRateForInterval failed", 0d, result.get(0), 0d);

      calendarStart.set(2006, 4, 19, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      calendarEnd.set(2006, 4, 16, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);
      ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
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
      Calendar calendar = Calendar.getInstance();
      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);

      OpResource resource = new OpResource();
      resource.setHourlyRatesPeriods(periodsSet);
      resource.setExternalRate(0d);
      List<Double> ratesList = resource.getRatesForDay(new Date(calendar.getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_INDEX);
      assertEquals("OpResource.getExternalRateForDay failed", 0d, result.doubleValue(), 0d);

      OpHourlyRatesPeriod hourlyRatesPeriod = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(5d);
      hourlyRatesPeriod.setExternalRate(10d);

      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setFinish(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ratesList = resource.getRatesForDay(new Date(calendar.getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_INDEX);
      assertEquals("OpResource.getExternalRateForDay failed", 10d, result.doubleValue(), 0d);

      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setStart(new Date(calendar.getTimeInMillis()));
      assertEquals("OpResource.getExternalRateForDay failed", 10d, result.doubleValue(), 0d);

      calendar.set(2006, 4, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ratesList = resource.getRatesForDay(new Date(calendar.getTimeInMillis()));
      result = ratesList.get(OpResource.EXTERNAL_RATE_INDEX);
      assertEquals("OpResource.getExternalRateForDay failed", 10d, result.doubleValue(), 0d);

      calendar.set(2006, 4, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 20, 0, 0, 0);
      ratesList = resource.getRatesForDay(new Date(calendar.getTimeInMillis()));
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
         Calendar calendarStart = Calendar.getInstance();
         calendarStart.set(2006, 4, 20, 0, 0, 0);
         calendarStart.set(Calendar.MILLISECOND, 0);
         Calendar calendarEnd = Calendar.getInstance();
         calendarEnd.set(2006, 4, 23, 0, 0, 0);
         calendarEnd.set(Calendar.MILLISECOND, 0);
         Calendar calendar = Calendar.getInstance();

         OpResource resource = new OpResource();
         resource.setHourlyRatesPeriods(periodsSet);
         resource.setHourlyRate(0d);
         List<List> ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
         result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
         assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(0), 0d);
         assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(1), 0d);
         assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(2), 0d);

         OpHourlyRatesPeriod hourlyRatesPeriod = new OpHourlyRatesPeriod();
         periodsSet.add(hourlyRatesPeriod);
         hourlyRatesPeriod.setInternalRate(3d);
         hourlyRatesPeriod.setExternalRate(4d);

         calendar.set(2006, 4, 19, 0, 0, 0);
         calendar.set(Calendar.MILLISECOND, 0);
         hourlyRatesPeriod.setStart(new Date(calendar.getTimeInMillis()));
         calendar.set(2006, 4, 25, 0, 0, 0);
         calendar.set(Calendar.MILLISECOND, 0);
         hourlyRatesPeriod.setFinish(new Date(calendar.getTimeInMillis()));
         calendarStart.set(2006, 4, 24, 0, 0, 0);
         calendarStart.set(Calendar.MILLISECOND, 0);
         calendarEnd.set(2006, 4, 26, 0, 0, 0);
         calendarEnd.set(Calendar.MILLISECOND, 0);
         ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
         result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
         assertEquals("OpResource.getExternalRateForInterval failed", 4d, result.get(0), 0d);
         assertEquals("OpResource.getExternalRateForInterval failed", 4d, result.get(1), 0d);
         assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(2), 0d);

         calendar.set(2006, 4, 24, 0, 0, 0);
         calendar.set(Calendar.MILLISECOND, 0);
         hourlyRatesPeriod.setFinish(new Date(calendar.getTimeInMillis()));
         ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
         result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
         assertEquals("OpResource.getExternalRateForInterval failed", 4d, result.get(0), 0d);
         assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(1), 0d);
         assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(2), 0d);

         calendarStart.set(2006, 4, 19, 0, 0, 0);
         calendarStart.set(Calendar.MILLISECOND, 0);
         calendarEnd.set(2006, 4, 19, 0, 0, 0);
         calendarEnd.set(Calendar.MILLISECOND, 0);
         ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
         result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
         assertEquals("OpResource.getExternalRateForInterval failed", 4d, result.get(0), 0d);

         calendar.set(2006, 4, 21, 0, 0, 0);
         calendar.set(Calendar.MILLISECOND, 0);
         hourlyRatesPeriod.setStart(new Date(calendar.getTimeInMillis()));
         ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
         result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
         assertEquals("OpResource.getExternalRateForInterval failed", 0d, result.get(0), 0d);

         calendarStart.set(2006, 4, 19, 0, 0, 0);
         calendarStart.set(Calendar.MILLISECOND, 0);
         calendarEnd.set(2006, 4, 16, 0, 0, 0);
         calendarEnd.set(Calendar.MILLISECOND, 0);
         ratesList = resource.getRatesForInterval(new Date(calendarStart.getTimeInMillis()), new Date(calendarEnd.getTimeInMillis()));
         result = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
         assertEquals("OpResource.getExternalRateForInterval failed", 0, result.size());
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
      Calendar calendar = Calendar.getInstance();

      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period1.setFinish(new Date(calendar.getTimeInMillis()));

      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 16, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period2.setFinish(new Date(calendar.getTimeInMillis()));

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
      Calendar calendar = Calendar.getInstance();

      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period1.setFinish(new Date(calendar.getTimeInMillis()));

      calendar.set(2006, 4, 22, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period2.setFinish(new Date(calendar.getTimeInMillis()));

      hourlyPeriods.add(period1);
      hourlyPeriods.add(period2);
      OpResource resource = new OpResource();
      resource.setHourlyRatesPeriods(hourlyPeriods);

      assertFalse(resource.checkPeriodDoNotOverlap());

      calendar.set(2006, 4, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period2.setFinish(new Date(calendar.getTimeInMillis()));
      assertFalse(resource.checkPeriodDoNotOverlap());

      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period2.setFinish(new Date(calendar.getTimeInMillis()));
      assertFalse(resource.checkPeriodDoNotOverlap());

      calendar.set(2006, 4, 16, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      period2.setFinish(new Date(calendar.getTimeInMillis()));
      assertFalse(resource.checkPeriodDoNotOverlap());
   }
}