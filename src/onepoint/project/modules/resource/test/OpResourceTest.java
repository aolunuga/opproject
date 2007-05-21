/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource.test;

import onepoint.project.modules.resource.OpHourlyRatesPeriod;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.test.OpBaseTestCase;

import java.sql.Date;
import java.util.*;

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
      Calendar calendar = Calendar.getInstance();

      calendar.set(2007, 6, 8, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      daysList.add(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 10, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      daysList.add(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      daysList.add(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 17, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      daysList.add(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      daysList.add(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 22, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      daysList.add(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      daysList.add(new Date(calendar.getTimeInMillis()));

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

      Calendar calendarStart = Calendar.getInstance();
      calendarStart.set(2007, 6, 10, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      Calendar calendarEnd = Calendar.getInstance();
      calendarEnd.set(2007, 6, 15, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);

      OpHourlyRatesPeriod hourlyRatesPeriod1 = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod1);
      hourlyRatesPeriod1.setStart(new Date(calendarStart.getTimeInMillis()));
      hourlyRatesPeriod1.setFinish(new Date(calendarEnd.getTimeInMillis()));
      hourlyRatesPeriod1.setInternalRate(3d);
      hourlyRatesPeriod1.setExternalRate(1d);

      calendarStart.set(2007, 6, 20, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      calendarEnd.set(2007, 6, 22, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);

      OpHourlyRatesPeriod hourlyRatesPeriod2 = new OpHourlyRatesPeriod();
      periodsSet.add(hourlyRatesPeriod2);
      hourlyRatesPeriod2.setStart(new Date(calendarStart.getTimeInMillis()));
      hourlyRatesPeriod2.setFinish(new Date(calendarEnd.getTimeInMillis()));
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