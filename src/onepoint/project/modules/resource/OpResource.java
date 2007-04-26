/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.persistence.OpObject;
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

   private String name;
   private String description;
   private double available = 100; // Default: 100%
   private boolean inheritPoolRate;
   private double hourlyRate;
   private double externalRate;
   private OpResourcePool pool;
   private OpUser user;
   private Set projectNodeAssignments;
   private Set activityAssignments;
   private Set assignmentVersions;
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

   public void setActivityAssignments(Set activityAssignments) {
      this.activityAssignments = activityAssignments;
   }

   public Set getActivityAssignments() {
      return activityAssignments;
   }

   public void setAssignmentVersions(Set assignmentVersions) {
      this.assignmentVersions = assignmentVersions;
   }

   public Set getAssignmentVersions() {
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
    * Returns the internal rate of a resource for a given day. When looking for the internal rate the most
    * prioritary is the OpHourlyRatesPeriod set and then the resource's hourly rate
    * @param aDay - the day for which the internal rate will be returned
    * @return - the internal rate of a resource for the given day
    */
   public Double getInternalRateForDay(Date aDay){
      Double internalRate = null;

      //first look in the set of OpHourlyRatesPeriod
      if(hourlyRatesPeriods.size() > 0){
         OpHourlyRatesPeriod hourlyRatePeriod;
         Iterator<OpHourlyRatesPeriod> iterator = hourlyRatesPeriods.iterator();
         while (iterator.hasNext()){
            hourlyRatePeriod = iterator.next();
            if(hourlyRatePeriod.getStart().getTime() <= aDay.getTime()
                 && hourlyRatePeriod.getFinish().getTime() >= aDay.getTime()){
               internalRate = hourlyRatePeriod.getInternalRate();
            }
         }
      }

      // if the resource has no OpHourlyRatesPeriods defined or
      // if the day is not in one of the OpHourlyRatesPeriods time intervals
      // we return the resource's hourly rate
      if(hourlyRatesPeriods.size() == 0 || (hourlyRatesPeriods.size() > 0 && internalRate == null)){
         internalRate = hourlyRate;
      }

      return internalRate;
   }

   /**
    * Returns a <code>List</code> of internal rates of a resource for a given interval. When looking for the internal rate the most
    * prioritary is the OpHourlyRatesPeriod set and then the resource's hourly rate
    * @param start - the begining of the interval
    * @param end - the end of the interval
    * @return - the <code>List</code> of internal rates of a resource for the given interval
    */
   public List getInternalRateForInterval(Date start, Date end){
      List<Double> internalRates = new ArrayList<Double>();
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

      while(!calendarStart.after(calendarEnd)){
         internalDayRate = getInternalRateForDay(new Date(calendarStart.getTimeInMillis()));
         internalRates.add(internalDayRate);
         calendarStart.add(Calendar.DATE,1);
      }

      return internalRates;
   }

   /**
    * Returns the external rate of a resource for a given day. When looking for the external rate the most
    * prioritary is the OpHourlyRatesPeriod set and then the resource's external rate
    * @param aDay - the day for which the external rate will be returned
    * @return - the external rate of a resource for the given day
    */
   public Double getExternalRateForDay(Date aDay){
      Double externalRate = null;

      //first look in the set of OpHourlyRatesPeriod
      if(hourlyRatesPeriods.size() > 0){
         OpHourlyRatesPeriod hourlyRatePeriod;
         Iterator<OpHourlyRatesPeriod> iterator = hourlyRatesPeriods.iterator();
         while (iterator.hasNext()){
            hourlyRatePeriod = iterator.next();
            if(hourlyRatePeriod.getStart().getTime() <= aDay.getTime()
                 && hourlyRatePeriod.getFinish().getTime() >= aDay.getTime()){
               externalRate = hourlyRatePeriod.getExternalRate();
            }
         }
      }

      // if the resource has no OpHourlyRatesPeriods defined or
      // if the day is not in one of the OpHourlyRatesPeriods time intervals
      // we return the resource's external rate
      if(hourlyRatesPeriods.size() == 0 || (hourlyRatesPeriods.size() > 0 && externalRate == null)){
         externalRate = this.externalRate;
      }

      return externalRate;
   }

   /**
    * Returns a <code>List</code> of external rates of a resource for a given interval. When looking for the external rate the most
    * prioritary is the OpHourlyRatesPeriod set and then the resource's external rate
    * @param start - the begining of the interval
    * @param end - the end of the interval
    * @return - the <code>List</code> of external rates of a resource for the given interval
    */
   public List getExternalRateForInterval(Date start, Date end){
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
      Double externalDayRate;

      while(!calendarStart.after(calendarEnd)){
         externalDayRate = getExternalRateForDay(new Date(calendarStart.getTimeInMillis()));
         externalRates.add(externalDayRate);
         calendarStart.add(Calendar.DATE,1);
      }

      return externalRates;
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
            //     currentStart___________currentEnd
            //          secondStart__________secondEnd
            //   secondStart_________secondEnd
            //         secondStart______secondEnd
            if(!((currentEnd.getTime() < secondStart.getTime()) || (secondEnd.getTime() < currentStart.getTime()))){
               return false;
            }
         }
      }
      return true;
   }
}
