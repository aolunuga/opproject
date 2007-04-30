/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAssignmentVersion;
import onepoint.project.modules.user.OpUser;

import java.sql.Date;
import java.util.*;

public class OpResource extends OpObject {

   public final static String RESOURCE = "OpResource";

   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";
   public final static String AVAILABLE = "Available";
   public final static String INHERIT_POOL_RATE = "InheritPoolRate";
   public final static String HOURLY_RATE = "HourlyRate";
   public final static String EXTERNAL_RATE = "ExternalRate";
   public final static String POOL = "Pool";
   public final static String USER = "User";
   public final static String PROJECT_NODE_ASSIGNMENTS = "ProjectNodeAssignments";
   public final static String ACTIVITY_ASSIGNMENTS = "ActivityAssignments";
   public final static String WORK_SLIPS = "WorkSlips";
   public final static String ABSENCES = "Absences";

   public final static int INTERNAL_RATE_INDEX = 0;
   public final static int EXTERNAL_RATE_INDEX = 1;
   public final static int INTERNAL_RATE_LIST_INDEX = 0;
   public final static int EXTERNAL_RATE_LIST_INDEX = 1;

   private String name;
   private String description;
   private double available = 100; // Default: 100%
   private boolean inheritPoolRate;
   private double hourlyRate;
   private double externalRate;
   private OpResourcePool pool;
   private OpUser user;
   private Set projectNodeAssignments;
   private Set<OpAssignment> activityAssignments;
   private Set<OpAssignmentVersion> assignmentVersions;
   private Set absences;
   private Set responsibleActivities;
   private Set responsibleActivityVersions;
   private Set<OpHourlyRatesPeriod> hourlyRatesPeriods;

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getDescription() {
      return description;
   }

   public void setAvailable(double available) {
      this.available = available;
   }

   public double getAvailable() {
      return available;
   }

   public void setInheritPoolRate(boolean inheritPoolRate) {
      this.inheritPoolRate = inheritPoolRate;
   }

   public boolean getInheritPoolRate() {
      return inheritPoolRate;
   }

   public void setHourlyRate(double hourlyRate) {
      this.hourlyRate = hourlyRate;
   }

   public double getHourlyRate() {
      return hourlyRate;
   }

   public void setPool(OpResourcePool pool) {
      this.pool = pool;
   }

   public OpResourcePool getPool() {
      return pool;
   }

   public void setUser(OpUser user) {
      this.user = user;
   }

   public OpUser getUser() {
      return user;
   }

   public void setProjectNodeAssignments(Set projectNodeAssignments) {
      this.projectNodeAssignments = projectNodeAssignments;
   }

   public Set getProjectNodeAssignments() {
      return projectNodeAssignments;
   }

   public void setActivityAssignments(Set<OpAssignment> activityAssignments) {
      this.activityAssignments = activityAssignments;
   }

   public Set<OpAssignment> getActivityAssignments() {
      return activityAssignments;
   }

   public void setAssignmentVersions(Set<OpAssignmentVersion> assignmentVersions) {
      this.assignmentVersions = assignmentVersions;
   }

   public Set<OpAssignmentVersion> getAssignmentVersions() {
      return assignmentVersions;
   }

   public void setAbsences(Set absences) {
      this.absences = absences;
   }

   public Set getAbsences() {
      return absences;
   }

   public Set getResponsibleActivities() {
      return responsibleActivities;
   }

   public void setResponsibleActivities(Set responsibleActivities) {
      this.responsibleActivities = responsibleActivities;
   }

   public Set getResponsibleActivityVersions() {
      return responsibleActivityVersions;
   }

   public void setResponsibleActivityVersions(Set responsibleActivityVersions) {
      this.responsibleActivityVersions = responsibleActivityVersions;
   }

   public double getExternalRate() {
      return externalRate;
   }

   public void setExternalRate(Double externalRate) {
      this.externalRate = (externalRate != null) ? externalRate : 0;
   }

   public Set<OpHourlyRatesPeriod> getHourlyRatesPeriods() {
      return hourlyRatesPeriods;
   }

   public void setHourlyRatesPeriods(Set<OpHourlyRatesPeriod> hourlyRatesPeriods) {
      this.hourlyRatesPeriods = hourlyRatesPeriods;
   }

   /**
    * Returns a List containing the internal and external rates of a resource for a given day.
    * When looking for the internal rate the most prioritary is the OpHourlyRatesPeriod set
    * and then the resource's hourly rate
    * @param aDay - the day for which the internal rate will be returned
    * @return - the <code>List</code> containing the internal and external rates of a resource for the given day
    */
   public List<Double> getRatesForDay(Date aDay){
      List<Double> result = new ArrayList<Double>();
      Double internalRate = null;
      Double externalRate = null;
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(aDay);
      calendar.set(Calendar.HOUR,0);
      calendar.set(Calendar.MINUTE,0);
      calendar.set(Calendar.SECOND,0);
      calendar.set(Calendar.MILLISECOND,0);
      aDay.setTime(calendar.getTimeInMillis());

      //first look in the set of OpHourlyRatesPeriod
      if(!hourlyRatesPeriods.isEmpty()){
         for(OpHourlyRatesPeriod hourlyRatePeriod:hourlyRatesPeriods){
            if(!hourlyRatePeriod.getStart().after(aDay) && !hourlyRatePeriod.getFinish().before(aDay)){
               internalRate = hourlyRatePeriod.getInternalRate();
               externalRate = hourlyRatePeriod.getExternalRate();
            }
         }
      }

      // if the resource has no OpHourlyRatesPeriods defined or
      // if the day is not in one of the OpHourlyRatesPeriods time intervals
      // we return the resource's hourly rate & external rate
      if(hourlyRatesPeriods.isEmpty() || (!hourlyRatesPeriods.isEmpty() && internalRate == null)){
         internalRate = hourlyRate;
      }
      if(hourlyRatesPeriods.isEmpty() || (!hourlyRatesPeriods.isEmpty() && externalRate == null)){
         externalRate = this.externalRate;
      }

      result.add(INTERNAL_RATE_INDEX, internalRate);
      result.add(EXTERNAL_RATE_INDEX, externalRate);
      return result;
   }

   /**
    * Returns a <code>List</code> that contains a list of internal rates and a list of external rates
    * for a resource for a given interval. When looking for the internal rate the most
    * prioritary is the OpHourlyRatesPeriod set and then the resource's hourly rate
    * @param start - the begining of the interval
    * @param end - the end of the interval
    * @return - the <code>List</code> with an internal rates list and an external rates list for a resource
    *  for the given interval
    */
   public List<List> getRatesForInterval(Date start, Date end){
      List<List> result = new ArrayList<List>();
      List<Double> internalRates = new ArrayList<Double>();
      List<Double> externalRates = new ArrayList<Double>();
      Calendar calendarStart = Calendar.getInstance();
      calendarStart.setTimeInMillis(start.getTime());
      calendarStart.set(Calendar.HOUR,0);
      calendarStart.set(Calendar.MINUTE,0);
      calendarStart.set(Calendar.SECOND,0);
      calendarStart.set(Calendar.MILLISECOND,0);
      Calendar calendarEnd = Calendar.getInstance();
      calendarEnd.setTimeInMillis(end.getTime());
      calendarEnd.set(Calendar.HOUR,0);
      calendarEnd.set(Calendar.MINUTE,0);
      calendarEnd.set(Calendar.SECOND,0);
      calendarEnd.set(Calendar.MILLISECOND,0);
      Double internalDayRate;
      Double externalDayRate;

      while(!calendarStart.after(calendarEnd)){
         internalDayRate = getRatesForDay(new Date(calendarStart.getTimeInMillis())).get(INTERNAL_RATE_INDEX);
         internalRates.add(internalDayRate);
         externalDayRate = getRatesForDay(new Date(calendarStart.getTimeInMillis())).get(EXTERNAL_RATE_INDEX);
         externalRates.add(externalDayRate);
         calendarStart.add(Calendar.DATE,1);
      }

      result.add(INTERNAL_RATE_LIST_INDEX,internalRates);
      result.add(EXTERNAL_RATE_LIST_INDEX,externalRates);
      return result;
   }

   /**
    * Checks if at least two period intervals, from the set of OpHourlyRatesPeriod that belongs to a resource, overlap
    *
    * @return <code>true</code> if all period intervals are distinct
    *         <code>false</code> if at least two period intervals ovelap
    */
   public boolean checkPeriodDoNotOverlap(){

      ArrayList<OpHourlyRatesPeriod> periodList = new ArrayList<OpHourlyRatesPeriod>();
      Iterator<OpHourlyRatesPeriod> iterator = getHourlyRatesPeriods().iterator();
      while(iterator.hasNext()){
          periodList.add(iterator.next());
      }

      for(int i = 0; i < periodList.size(); i++){
         OpHourlyRatesPeriod currentPeriod = periodList.get(i);
         java.util.Date currentStart = currentPeriod.getStart();
         java.util.Date currentEnd = currentPeriod.getFinish();

         for(int j = i + 1; j < periodList.size(); j++){
            OpHourlyRatesPeriod secondPeriod = periodList.get(j);
            java.util.Date secondStart = secondPeriod.getStart();
            java.util.Date secondEnd = secondPeriod.getFinish();
            if(!((currentEnd.before(secondStart)) || (secondEnd.before(currentStart)))){
               return false;
            }
         }
      }
      return true;
   }
}
