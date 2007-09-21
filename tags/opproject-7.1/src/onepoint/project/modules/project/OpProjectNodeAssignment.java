/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.resource.OpHourlyRatesPeriod;
import onepoint.project.modules.resource.OpResource;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class OpProjectNodeAssignment extends OpObject {

   public final static String PROJECT_NODE_ASSIGNMENT = "OpProjectNodeAssignment";

   public final static String RESOURCE = "Resource";
   public final static String PROJECT_NODE = "ProjectNode";

   public final static int INTERNAL_RATE_INDEX = 0;
   public final static int EXTERNAL_RATE_INDEX = 1;
   public final static int INTERNAL_RATE_LIST_INDEX = 0;
   public final static int EXTERNAL_RATE_LIST_INDEX = 1;


   private Double hourlyRate;
   private Double externalRate;
   private OpResource resource;
   private OpProjectNode projectNode;
   private Set<OpHourlyRatesPeriod> hourlyRatesPeriods;

   public void setResource(OpResource resource) {
      this.resource = resource;
   }

   public OpResource getResource() {
      return resource;
   }

   public void setProjectNode(OpProjectNode projectNode) {
      this.projectNode = projectNode;
   }

   public OpProjectNode getProjectNode() {
      return projectNode;
   }

   public Double getExternalRate() {
      return externalRate;
   }

   public void setExternalRate(Double externalRate) {
      this.externalRate = externalRate;
   }

   public Double getHourlyRate() {
      return hourlyRate;
   }

   public void setHourlyRate(Double hourlyRate) {
      this.hourlyRate = hourlyRate;
   }

   public Set<OpHourlyRatesPeriod> getHourlyRatesPeriods() {
      return hourlyRatesPeriods;
   }

   public void setHourlyRatesPeriods(Set<OpHourlyRatesPeriod> hourlyRatesPeriods) {
      this.hourlyRatesPeriods = hourlyRatesPeriods;
   }

   /**
    * Returns a List containing the internal and external rates of a resource for a given day.
    * When looking for the internal rate the priority is:
    * -  the assignments rates periods set (if the searchPeriodsSet parameter is true)
    * -  the assignment's hourly/external rate
    * - the assignment's resource rates periods set
    * - the assignment's resource hourly/external rate
    *
    * @param aDay - the day for which the internal rate will be returned
    * @return - the <code>List</code> containing the internal and external rates of a project assignment for the given day.
    *         Use <code>INTERNAL_RATE_INDEX</code> and <code>EXTERNAL_RATE_INDEX</code> to retrieve the values.
    */
   public List<Double> getRatesForDay(Date aDay, boolean searchPeriodsSet) {
      List<Double> result = new ArrayList<Double>();
      Double internalRate = null;
      Double externalRate = null;
      Calendar calendar = XCalendar.setCalendarTimeToZero(aDay);
      aDay.setTime(calendar.getTimeInMillis());

      //first look in the set of OpHourlyRatesPeriod if the search parameter allows it
      if (!hourlyRatesPeriods.isEmpty() && searchPeriodsSet) {
         for (OpHourlyRatesPeriod hourlyRatePeriod : hourlyRatesPeriods) {
            if (!hourlyRatePeriod.getStart().after(aDay) && !hourlyRatePeriod.getFinish().before(aDay)) {
               internalRate = hourlyRatePeriod.getInternalRate();
               externalRate = hourlyRatePeriod.getExternalRate();
            }
         }
      }

      // if the assignment has no OpHourlyRatesPeriods defined or
      // if the day is not in one of the OpHourlyRatesPeriods time intervals
      // we return the assignmet's hourly rate & external rate
      if (hourlyRatesPeriods.isEmpty() || (!hourlyRatesPeriods.isEmpty() && internalRate == null)) {
         internalRate = hourlyRate;
         //if the internal rate is null return the assignment's resource rate
         if (internalRate == null) {
            internalRate = resource.getRatesForDay(aDay).get(OpResource.INTERNAL_RATE_INDEX);
         }
      }
      if (hourlyRatesPeriods.isEmpty() || (!hourlyRatesPeriods.isEmpty() && externalRate == null)) {
         externalRate = this.externalRate;
         //if the external rate is null return the assignment's resource rate
         if (externalRate == null) {
            externalRate = resource.getRatesForDay(aDay).get(OpResource.EXTERNAL_RATE_INDEX);
         }
      }

      result.add(INTERNAL_RATE_INDEX, internalRate);
      result.add(EXTERNAL_RATE_INDEX, externalRate);
      return result;
   }

   /**
    * Returns a <code>List</code> that contains a list of internal rates and a list of external rates
    * for a project assignment for a given interval. When looking for the internal rate the
    * priority is:
    * -  the assignments rates periods set (if the searchPeriodsSet parameter is true)
    * -  the assignment's hourly/external rate
    * - the assignment's resource rates periods set
    * - the assignment's resource hourly/external rate
    *
    * @param start            - the begining of the interval
    * @param end              - the end of the interval
    * @param searchPeriodsSet - parameter which indicates if the search for the rates should look in the
    *                         assignment's hourly rates periods set
    * @return - the <code>List</code> with an internal rates list and an external rates list for a resource
    *         for the given interval
    */
   public List<List> getRatesForInterval(Date start, Date end, boolean searchPeriodsSet) {
      List<List> result = new ArrayList<List>();
      List<Double> internalRates = new ArrayList<Double>();
      List<Double> externalRates = new ArrayList<Double>();
      Calendar calendar = XCalendar.setCalendarTimeToZero(start);
      Date dateStart = new Date(calendar.getTimeInMillis());
      calendar = XCalendar.setCalendarTimeToZero(end);
      Date dateEnd = new Date(calendar.getTimeInMillis());
      Double internalDayRate;
      Double externalDayRate;

      while (!dateStart.after(dateEnd)) {
         internalDayRate = getRatesForDay(new Date(dateStart.getTime()), searchPeriodsSet).get(INTERNAL_RATE_INDEX);
         internalRates.add(internalDayRate);
         externalDayRate = getRatesForDay(new Date(dateStart.getTime()), searchPeriodsSet).get(EXTERNAL_RATE_INDEX);
         externalRates.add(externalDayRate);
         calendar = XCalendar.setCalendarTimeToZero(dateStart);
         calendar.add(Calendar.DATE, 1);
         dateStart = new Date(calendar.getTimeInMillis());
      }

      result.add(INTERNAL_RATE_LIST_INDEX, internalRates);
      result.add(EXTERNAL_RATE_LIST_INDEX, externalRates);
      return result;
   }

   /**
    * Returns a <code>List</code> that contains a list of internal rates and a list of external rates
    * for a resource assigned on a project for a given list of days. When looking for the internal/external
    * rate the priority is:
    * -  the assignments rates periods set (if the searchPeriodsSet parameter is true)
    * -  the assignment's hourly/external rate
    * - the assignment's resource rates periods set
    * - the assignment's resource hourly/external rate
    *
    * @param daysList - the list of days for which the rates will be returned
    * @return - the <code>List</code> with an internal rates list and an external rates list for a resource
    *         assigned on a project for the given interval
    */
   public List<List<Double>> getRatesForListOfDays(List<Date> daysList) {
      List<List<Double>> result = new ArrayList<List<Double>>();
      List<Double> internalRates = new ArrayList<Double>();
      List<Double> externalRates = new ArrayList<Double>();
      Double internalDayRate;
      Double externalDayRate;

      for (Date day : daysList) {
         internalDayRate = getRatesForDay(day, true).get(INTERNAL_RATE_INDEX);
         internalRates.add(internalDayRate);
         externalDayRate = getRatesForDay(day, true).get(EXTERNAL_RATE_INDEX);
         externalRates.add(externalDayRate);
      }

      result.add(INTERNAL_RATE_LIST_INDEX, internalRates);
      result.add(EXTERNAL_RATE_LIST_INDEX, externalRates);
      return result;
   }

   /**
    * Checks if at least two period intervals, from the set of OpHourlyRatesPeriod that belongs to a project
    * node assignment, overlap
    *
    * @return <code>true</code> if all period intervals are distinct
    *         <code>false</code> if at least two period intervals ovelap
    */
   public boolean checkPeriodDoNotOverlap() {

      ArrayList<OpHourlyRatesPeriod> periodList = new ArrayList<OpHourlyRatesPeriod>();
      for (OpHourlyRatesPeriod opHourlyRatesPeriod : getHourlyRatesPeriods()) {
         periodList.add(opHourlyRatesPeriod);
      }

      for (int i = 0; i < periodList.size(); i++) {
         OpHourlyRatesPeriod currentPeriod = periodList.get(i);
         java.util.Date currentStart = currentPeriod.getStart();
         java.util.Date currentEnd = currentPeriod.getFinish();

         for (int j = i + 1; j < periodList.size(); j++) {
            OpHourlyRatesPeriod secondPeriod = periodList.get(j);
            java.util.Date secondStart = secondPeriod.getStart();
            java.util.Date secondEnd = secondPeriod.getFinish();
            if (!((currentEnd.before(secondStart)) || (secondEnd.before(currentStart)))) {
               return false;
            }
         }
      }
      return true;
   }
}
