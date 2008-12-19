/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.resource.OpHourlyRatesPeriod;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.test.OpTestCase;

import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class tests OpProjetcNodeAssignment's getRatesForDay()& getRatesForInterval() &
 * checkPeriodDoNotOverlap methods.
 *
 * @author florin.haizea
 */
public class OpProjectNodeAssignmentTest extends OpTestCase {

   /**
    * Test extraction of the internal/external rate for a given day
    *
    * @throws Exception if the test fails
    */
   public void testGetRatesForDay()
        throws Exception {

      Double internalRate;
      Double externalRate;
      Set<OpHourlyRatesPeriod> assignmentPeriodsSet = new HashSet<OpHourlyRatesPeriod>();
      OpResource resource = new OpResource();
      resource.updateInternalHourlyRate(3d);
      resource.updateExternalHourlyRate(2d);
      Set<OpHourlyRatesPeriod> resourcePeriodsSet = new HashSet<OpHourlyRatesPeriod>();

      OpProjectNodeAssignment assignment = new OpProjectNodeAssignment();
      assignment.setHourlyRate(7d);
      assignment.setExternalRate(10d);
      assignment.updateHourlyRatesPeriods(assignmentPeriodsSet, null);
      assignment.setResource(resource);
      resource.setHourlyRatesPeriods(resourcePeriodsSet);

      List<Double> ratesList = assignment.getRatesForDay(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 10, 12).getTimeInMillis()), false);
      internalRate = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
      externalRate = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 7d, internalRate.doubleValue(), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 10d, externalRate.doubleValue(), 0d);

      ratesList = assignment.getRatesForDay(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 10, 12).getTimeInMillis()), true);
      internalRate = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
      externalRate = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 7d, internalRate.doubleValue(), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 10d, externalRate.doubleValue(), 0d);

      assignment.setHourlyRate(null);
      assignment.setExternalRate(null);
      ratesList = assignment.getRatesForDay(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 10, 12).getTimeInMillis()), true);
      internalRate = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
      externalRate = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 3d, internalRate.doubleValue(), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 2d, externalRate.doubleValue(), 0d);

      OpHourlyRatesPeriod hourlyRatesPeriod = new OpHourlyRatesPeriod();
      resourcePeriodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(9d);
      hourlyRatesPeriod.setExternalRate(9d);

      hourlyRatesPeriod.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 11).getTimeInMillis()));
      hourlyRatesPeriod.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 15).getTimeInMillis()));
      ratesList = assignment.getRatesForDay(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 14).getTimeInMillis()), true);
      internalRate = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
      externalRate = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 9d, internalRate.doubleValue(), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 9d, externalRate.doubleValue(), 0d);

      assignment.setHourlyRate(7d);
      assignment.setExternalRate(10d);
      ratesList = assignment.getRatesForDay(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 14).getTimeInMillis()), true);
      internalRate = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
      externalRate = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 7d, internalRate.doubleValue(), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 10d, externalRate.doubleValue(), 0d);

      hourlyRatesPeriod = new OpHourlyRatesPeriod();
      assignmentPeriodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(5d);
      hourlyRatesPeriod.setExternalRate(6d);

      hourlyRatesPeriod.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 10, 12).getTimeInMillis()));
      hourlyRatesPeriod.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 13).getTimeInMillis()));

      ratesList = assignment.getRatesForDay(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 12).getTimeInMillis()), true);
      internalRate = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
      externalRate = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 5d, internalRate.doubleValue(), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 6d, externalRate.doubleValue(), 0d);

      ratesList = assignment.getRatesForDay(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 14).getTimeInMillis()), true);
      internalRate = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
      externalRate = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 7d, internalRate.doubleValue(), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 10d, externalRate.doubleValue(), 0d);

      ratesList = assignment.getRatesForDay(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 14).getTimeInMillis()), false);
      internalRate = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
      externalRate = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 7d, internalRate.doubleValue(), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForDay failed", 10d, externalRate.doubleValue(), 0d);
   }

   /**
    * Test extraction of the internal/external rates for a given interval of time
    *
    * @throws Exception if the test fails
    */
   public void testGetRatesForInterval()
        throws Exception {

      List<Double> internalRateList;
      List<Double> externalRateList;
      Set<OpHourlyRatesPeriod> assignmentPeriodsSet = new HashSet<OpHourlyRatesPeriod>();
      OpResource resource = new OpResource();
      resource.updateInternalHourlyRate(3d);
      resource.updateExternalHourlyRate(2d);
      Set<OpHourlyRatesPeriod> resourcePeriodsSet = new HashSet<OpHourlyRatesPeriod>();
      Date dateStart = new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 12).getTimeInMillis());
      Date dateEnd = new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 14).getTimeInMillis());

      OpProjectNodeAssignment assignment = new OpProjectNodeAssignment();
      assignment.setHourlyRate(7d);
      assignment.setExternalRate(10d);
      assignment.updateHourlyRatesPeriods(assignmentPeriodsSet, null);
      assignment.setResource(resource);
      resource.setHourlyRatesPeriods(resourcePeriodsSet);

      List<List> ratesList = assignment.getRatesForInterval(dateStart, dateEnd, false);
      internalRateList = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
      externalRateList = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(2), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(2), 0d);

      ratesList = assignment.getRatesForInterval(dateStart, dateEnd, true);
      internalRateList = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
      externalRateList = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(2), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(2), 0d);

      assignment.setHourlyRate(null);
      assignment.setExternalRate(null);
      ratesList = assignment.getRatesForInterval(dateStart, dateEnd, true);
      internalRateList = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
      externalRateList = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 3d, internalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 3d, internalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 3d, internalRateList.get(2), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 2d, externalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 2d, externalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 2d, externalRateList.get(2), 0d);

      OpHourlyRatesPeriod hourlyRatesPeriod = new OpHourlyRatesPeriod();
      resourcePeriodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(9d);
      hourlyRatesPeriod.setExternalRate(9d);

      hourlyRatesPeriod.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 11).getTimeInMillis()));
      hourlyRatesPeriod.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 13).getTimeInMillis()));

      ratesList = assignment.getRatesForInterval(dateStart, dateEnd, true);
      internalRateList = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
      externalRateList = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 9d, internalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 9d, internalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 3d, internalRateList.get(2), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 9d, externalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 9d, externalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 2d, externalRateList.get(2), 0d);

      assignment.setHourlyRate(7d);
      assignment.setExternalRate(10d);
      ratesList = assignment.getRatesForInterval(dateStart, dateEnd, true);
      internalRateList = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
      externalRateList = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(2), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(2), 0d);

      hourlyRatesPeriod = new OpHourlyRatesPeriod();
      assignmentPeriodsSet.add(hourlyRatesPeriod);
      hourlyRatesPeriod.setInternalRate(5d);
      hourlyRatesPeriod.setExternalRate(6d);

      hourlyRatesPeriod.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 10, 12).getTimeInMillis()));
      hourlyRatesPeriod.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 11, 13).getTimeInMillis()));

      ratesList = assignment.getRatesForInterval(dateStart, dateEnd, true);
      internalRateList = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
      externalRateList = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 5d, internalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 5d, internalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(2), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 6d, externalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 6d, externalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(2), 0d);

      ratesList = assignment.getRatesForInterval(dateStart, dateEnd, false);
      internalRateList = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
      externalRateList = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 7d, internalRateList.get(2), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(0), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(1), 0d);
      assertEquals("OpProjectNodeAssignment.getRatesForInterval failed", 10d, externalRateList.get(2), 0d);
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

      period1.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 20).getTimeInMillis()));
      period1.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 27).getTimeInMillis()));

      period2.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 13).getTimeInMillis()));
      period2.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 16).getTimeInMillis()));

      hourlyPeriods.add(period1);
      hourlyPeriods.add(period2);
      OpProjectNodeAssignment assignment = new OpProjectNodeAssignment();
      assignment.updateHourlyRatesPeriods(hourlyPeriods, null);

      assertTrue(assignment.checkPeriodDoNotOverlap());
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

      period1.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 20).getTimeInMillis()));
      period1.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 27).getTimeInMillis()));

      period2.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 22).getTimeInMillis()));
      period2.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 25).getTimeInMillis()));

      hourlyPeriods.add(period1);
      hourlyPeriods.add(period2);
      OpProjectNodeAssignment assignment = new OpProjectNodeAssignment();
      assignment.updateHourlyRatesPeriods(hourlyPeriods, null);

      assertFalse(assignment.checkPeriodDoNotOverlap());

      period2.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 20).getTimeInMillis()));
      period2.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 27).getTimeInMillis()));
      assertFalse(assignment.checkPeriodDoNotOverlap());

      period2.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 13).getTimeInMillis()));
      period2.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 25).getTimeInMillis()));
      assertFalse(assignment.checkPeriodDoNotOverlap());

      period2.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 16).getTimeInMillis()));
      period2.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2006, 4, 27).getTimeInMillis()));
      assertFalse(assignment.checkPeriodDoNotOverlap());
   }
}
