/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import onepoint.persistence.OpCustomSubTypable;
import onepoint.persistence.OpEntityException;
import onepoint.project.modules.calendars.OpHasWorkCalendar;
import onepoint.project.modules.calendars.OpWorkCalendar;
import onepoint.project.modules.custom_attribute.OpCustomType;
import onepoint.project.modules.custom_attribute.OpCustomizableObject;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAssignmentVersion;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.skills.OpSkillRating;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpProjectCalendar;

public class OpResource extends OpCustomizableObject implements OpPermissionable, OpCustomSubTypable, OpHasWorkCalendar {

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
   public final static String SKILL_RATINGS = "SkillRatings";
   public static final String ARCHIVED = "Archived";
   
   public final static int INTERNAL_RATE_INDEX = 0;
   public final static int EXTERNAL_RATE_INDEX = 1;
   public final static int INTERNAL_RATE_LIST_INDEX = 0;
   public final static int EXTERNAL_RATE_LIST_INDEX = 1;

   public static int INVALID_USER_LEVEL = 0;

   private String name;
   private String description;
   private double available = 100; // Default: 100%
   private boolean inheritPoolRate;
   private double hourlyRate;
   private double externalRate;
   private boolean archived = false;
   private Timestamp baseDataChanged = null;
   private OpResourcePool pool;
   private OpUser user;
   private Set<OpProjectNodeAssignment> projectNodeAssignments;
   private Set<OpAssignment> activityAssignments;
   private Set<OpAssignmentVersion> assignmentVersions;
   private Set responsibleActivities;
   private Set responsibleActivityVersions;
   private Set<OpHourlyRatesPeriod> hourlyRatesPeriods;
   private Set<OpSkillRating> skillRatings;
   private OpCustomType customType;

   private Set<OpPermission> permissions;
   
   private OpWorkCalendar workCalendar = null;
   private boolean inheritPoolWorkCalendar = true;
   private Set<OpAbsence> absences = null;

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

   private void setAvailable(double available) {
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

   private void setHourlyRate(double hourlyRate) {
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

   public void setProjectNodeAssignments(Set<OpProjectNodeAssignment> projectNodeAssignments) {
      this.projectNodeAssignments = projectNodeAssignments;
   }

   public Set<OpProjectNodeAssignment> getProjectNodeAssignments() {
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

   private void setExternalRate(Double externalRate) {
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
    *
    * @param aDay - the day for which the internal rate will be returned
    * @return - the <code>List</code> containing the internal and external rates of a resource for the given day
    */
   public List<Double> getRatesForDay(Date aDay) {
      List<Double> result = new ArrayList<Double>();
      Double internalRate = null;
      Double externalRate = null;
      Calendar calendar = OpProjectCalendar.setCalendarTimeToZero(aDay);
      aDay.setTime(calendar.getTimeInMillis());

      //first look in the set of OpHourlyRatesPeriod
      if (!hourlyRatesPeriods.isEmpty()) {
         for (OpHourlyRatesPeriod hourlyRatePeriod : hourlyRatesPeriods) {
            if (!hourlyRatePeriod.getStart().after(aDay) && !hourlyRatePeriod.getFinish().before(aDay)) {
               internalRate = hourlyRatePeriod.getInternalRate();
               externalRate = hourlyRatePeriod.getExternalRate();
            }
         }
      }

      // if the resource has no OpHourlyRatesPeriods defined or
      // if the day is not in one of the OpHourlyRatesPeriods time intervals
      // we return the resource's hourly rate & external rate
      //<FIXME author="Haizea Florin" description="data loading problem: the hourlyRatesPeriods.isEmpty() statement will
      //  load all the hourly rates periods of this resource even if in some cases this is not necessary">
      if (hourlyRatesPeriods.isEmpty() || (!hourlyRatesPeriods.isEmpty() && internalRate == null)) {
      //<FIXME>
         internalRate = hourlyRate;
      }
      if (hourlyRatesPeriods.isEmpty() || (!hourlyRatesPeriods.isEmpty() && externalRate == null)) {
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
    *
    * @param start - the begining of the interval
    * @param end   - the end of the interval
    * @return - the <code>List</code> with an internal rates list and an external rates list for a resource
    *         for the given interval
    */
   public List<List> getRatesForInterval(Date start, Date end) {
      List<List> result = new ArrayList<List>();
      List<Double> internalRates = new ArrayList<Double>();
      List<Double> externalRates = new ArrayList<Double>();
      Calendar calendar = OpProjectCalendar.setCalendarTimeToZero(start);
      Date startDate = new Date(calendar.getTimeInMillis());
      calendar = OpProjectCalendar.setCalendarTimeToZero(end);
      Date endDate = new Date(calendar.getTimeInMillis());
      Double internalDayRate;
      Double externalDayRate;

      while (!startDate.after(endDate)) {
         internalDayRate = getRatesForDay(new Date(startDate.getTime())).get(INTERNAL_RATE_INDEX);
         internalRates.add(internalDayRate);
         externalDayRate = getRatesForDay(new Date(startDate.getTime())).get(EXTERNAL_RATE_INDEX);
         externalRates.add(externalDayRate);
         calendar = OpProjectCalendar.setCalendarTimeToZero(startDate);
         calendar.add(Calendar.DATE, 1);
         startDate = new Date(calendar.getTimeInMillis());
      }

      result.add(INTERNAL_RATE_LIST_INDEX, internalRates);
      result.add(EXTERNAL_RATE_LIST_INDEX, externalRates);
      return result;
   }

   /**
    * Returns a <code>List</code> that contains a list of internal rates and a list of external rates
    * for a resource for a given list of days. When looking for the internal rate the most
    * prioritary is the OpHourlyRatesPeriod set and then the resource's hourly rate
    *
    * @param daysList - the list of days for which the rates will be returned
    * @return - the <code>List</code> with an internal rates list and an external rates list for a resource
    *         for the given interval
    */
   public List<List> getRatesForListOfDays(List<Date> daysList) {
      List<List> result = new ArrayList<List>();
      List<Double> internalRates = new ArrayList<Double>();
      List<Double> externalRates = new ArrayList<Double>();
      Double internalDayRate;
      Double externalDayRate;

      for (Date day : daysList) {
         internalDayRate = getRatesForDay(day).get(INTERNAL_RATE_INDEX);
         internalRates.add(internalDayRate);
         externalDayRate = getRatesForDay(day).get(EXTERNAL_RATE_INDEX);
         externalRates.add(externalDayRate);
      }

      result.add(INTERNAL_RATE_LIST_INDEX, internalRates);
      result.add(EXTERNAL_RATE_LIST_INDEX, externalRates);
      return result;
   }

   /**
    * Checks if at least two period intervals, from the set of OpHourlyRatesPeriod that belongs to a resource, overlap
    *
    * @return <code>true</code> if all period intervals are distinct
    *         <code>false</code> if at least two period intervals ovelap
    */
   public boolean checkPeriodDoNotOverlap() {

      ArrayList<OpHourlyRatesPeriod> periodList = new ArrayList<OpHourlyRatesPeriod>();
      Iterator<OpHourlyRatesPeriod> iterator = getHourlyRatesPeriods().iterator();
      while (iterator.hasNext()) {
         periodList.add(iterator.next());
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

   public Set<OpSkillRating> getSkillRatings() {
      return skillRatings;
   }

   public void addSkillRating(OpSkillRating skillRating) {
      if (this.skillRatings == null) {
         this.skillRatings = new HashSet<OpSkillRating>();
      }
      this.skillRatings.add(skillRating);
   }

   public void setSkillRatings(Set skillRatings) {
      this.skillRatings = skillRatings;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpCustomSubTypable#getCustomType()
    */
   public OpCustomType getCustomType() {
      return customType;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpCustomSubTypable#setCustomType(onepoint.project.modules.custom_attribute.OpCustomType)
    */
   public void setCustomType(OpCustomType customType) {
      this.customType = customType;
   }

   //<FIXME author="Mihai Costin" description="Move validations from service into this method">
   public void validate()
        throws OpEntityException {
      if (this.getUser() != null) {
         if (this.getUser().getLevel() < OpUser.CONTRIBUTOR_USER_LEVEL) {
            throw new OpEntityException(INVALID_USER_LEVEL);
         }
      }
   }
   //</FIXME>

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#getPermissions()
    */
   public Set<OpPermission> getPermissions() {
      return permissions;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#setPermissions(java.util.Set)
    */
   public void setPermissions(Set<OpPermission> permissions) {
      this.permissions = permissions;
   }

   public void addPermission(OpPermission permission) {
      Set<OpPermission> perm = getPermissions();
      if (perm == null) {
         perm = new HashSet<OpPermission>();
         setPermissions(perm);
      }
      perm.add(permission);
      permission.setObject(this);
   }

   /**
    * @param opPermission
    * @pre
    * @post
    */
   public void removePermission(OpPermission opPermission) {
      Set<OpPermission> perm = getPermissions();
      if (perm != null) {
         perm.remove(opPermission);
      }
      opPermission.setObject(null);
   }

   public Timestamp getBaseDataChanged() {
      return baseDataChanged;
   }

   private void setBaseDataChanged(Timestamp touched) {
      this.baseDataChanged = touched;
   }

   public Boolean isArchived() {
	   return archived;
   }

   /**
    * @deprecated
    */
   public Boolean getArchived() {
	   return isArchived();
   }

   public void setArchived(Boolean archived) {
	   this.archived = (archived == null ? false : archived);
   }

   public void updateInternalHourlyRate(double newRate) {
      if (newRate == getHourlyRate()) {
         return;
      }
      touch();
      setHourlyRate(newRate);
   }

   public void updateExternalHourlyRate(double newRate) {
      if (newRate == getExternalRate()) {
         return;
      }
      touch();
      setExternalRate(newRate);
   }
   
   public void updateAvailable(double available) {
      if (getAvailable() == available) {
         return;
      }
      touch();
      setAvailable(available);
   }
   
   public void touch() {
      setBaseDataChanged(new Timestamp(System.currentTimeMillis()));
   }

   public void addActivity(OpActivity x) {
      if (getResponsibleActivities() == null) {
         setResponsibleActivities(new HashSet());
      }
      if (getResponsibleActivities().add(x)) {
         x.setResponsibleResource(this);
      }
   }
   
   public void removeActivity(OpActivity x) {
      if (getResponsibleActivities() == null) {
         return;
      }
      if (getResponsibleActivities().remove(x)) {
         x.setResponsibleResource(null);
      }
   }
   
   public void addActivityVersion(OpActivityVersion x) {
      if (getResponsibleActivityVersions() == null) {
         setResponsibleActivityVersions(new HashSet());
      }
      if (getResponsibleActivityVersions().add(x)) {
         x.setResponsibleResource(this);
      }
   }
   
   public void removeActivityVersion(OpActivityVersion x) {
      if (getResponsibleActivityVersions() == null) {
         return;
      }
      if (getResponsibleActivityVersions().remove(x)) {
         x.setResponsibleResource(null);
      }
   }
   
   public void addActivityAssignment(OpAssignment a) {
      if (getActivityAssignments() == null) {
         setActivityAssignments(new HashSet<OpAssignment>());
      }
      if (getActivityAssignments().add(a)) {
         a.setResource(this);
      }
   }

   public void removeActivityAssignment(OpAssignment a) {
      if (getActivityAssignments() == null) {
         return;
      }
      if (getActivityAssignments().remove(a)) {
         a.setResource(null);
      }
   }

   public void addActivityVersionAssignment(OpAssignmentVersion a) {
      if (getAssignmentVersions() == null) {
         setAssignmentVersions(new HashSet<OpAssignmentVersion>());
      }
      if (getAssignmentVersions().add(a)) {
         a.setResource(this);
      }
   }

   public void removeActivityVersionAssignment(OpAssignmentVersion a) {
      if (getAssignmentVersions() == null) {
         return;
      }
      if (getAssignmentVersions().remove(a)) {
         a.setResource(null);
      }
   }

   public OpWorkCalendar getWorkCalendar() {
      return workCalendar;
   }

   public void setWorkCalendar(OpWorkCalendar workCalendar) {
      this.workCalendar = workCalendar;
   }

   public boolean getInheritPoolWorkCalendar() {
      return inheritPoolWorkCalendar;
   }

   public void setInheritPoolWorkCalendar(boolean inheritPoolWorkCalendar) {
      this.inheritPoolWorkCalendar = inheritPoolWorkCalendar;
   }

   public void setInheritPoolWorkCalendar(Boolean inheritPoolWorkCalendar) {
      this.inheritPoolWorkCalendar = inheritPoolWorkCalendar != null ? inheritPoolWorkCalendar.booleanValue() : true;
   }

   public Set<OpAbsence> getAbsences() {
      return absences;
   }

   public void setAbsences(Set<OpAbsence> absences) {
      this.absences = absences;
   }

   public void addAbsence(OpAbsence absence) {
      if (getAbsences() == null) {
         setAbsences(new HashSet<OpAbsence>());
      }
      if (getAbsences().add(absence)) {
         absence.setResource(this);
      }
   }
   
   public void removeAbsence(OpAbsence absence) {
      if (getAbsences() == null) {
         return;
      }
      if (getAbsences().remove(absence)) {
         absence.setResource(null);
      }
   }
   
   public boolean updateWorkCalendar(OpWorkCalendar newWC) {
      boolean changed = (newWC != null ? newWC.getId() : 0l) != (getWorkCalendar() != null ? getWorkCalendar().getId()
            : 0l);
      if (changed) {
         if (getWorkCalendar() != null) {
            getWorkCalendar().removeResource(this);
         }
         if (newWC != null) {
            newWC.addResource(this);
         }
         touch();
      }
      return changed;
   }

   public void addProjectNodeAssignment(OpProjectNodeAssignment pna) {
      if (getProjectNodeAssignments() == null) {
         setProjectNodeAssignments(new HashSet<OpProjectNodeAssignment>());
      }
      if (getProjectNodeAssignments().add(pna)) {
         pna.setResource(this);
      }
   }
   
   @Override
	public String toString() {
	   StringBuffer b = new StringBuffer();
	   b.append("OpResource: {");
	   b.append("N: "+name);
	   b.append("D: "+description);
	   b.append("A: "+available);
	   b.append("}");
	   return b.toString();
	}

   public void removeProjectNodeAssignment(OpProjectNodeAssignment del) {
      if (getProjectNodeAssignments() == null) {
         return;
      }
      if (getProjectNodeAssignments().remove(del)) {
         del.setResource(null);
      }
   }
}
