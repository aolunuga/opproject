/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

import java.util.Set;

public class OpResource extends OpObject {

   public final static String RESOURCE = "OpResource";

   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";
   public final static String AVAILABLE = "Available";
   public final static String INHERIT_POOL_RATE = "InheritPoolRate";
   public final static String HOURLY_RATE = "HourlyRate";
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
   private OpResourcePool pool;
   private OpUser user;
   private Set projectNodeAssignments;
   private Set activityAssignments;
   private Set assignmentVersions;
   private Set absences;

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

}
