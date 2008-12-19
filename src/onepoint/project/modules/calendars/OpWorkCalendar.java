package onepoint.project.modules.calendars;

import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;

public class OpWorkCalendar extends OpObject {

   private Set<OpResource> resources = null;
   private Set<OpResourcePool> resourcePools = null;
   private Set<OpProjectPlanVersion> projectPlanVersions = null;
   private Set<OpProjectPlan> projectPlans = null;
   
   private String name = null;
   
   private int firstWorkday = -1;
   private int lastWorkday = -1;
   
   private double workHoursPerDay = 0d;
   
   private String holidayCalendarId = null;
   boolean holidaysAreWorkdays = false;
   
   boolean defaultCalendar = false;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Set<OpResource> getResources() {
      return resources;
   }

   private void setResources(Set<OpResource> resources) {
      this.resources = resources;
   }

   public void addResource(OpResource r) {
      if (getResources() == null) {
         setResources(new HashSet<OpResource>());
      }
      if (getResources().add(r)) {
         r.setWorkCalendar(this);
      }
   }
      
   public void removeResource(OpResource r) {
      if (getResources() == null) {
         return;
      }
      if (getResources().remove(r)) {
         r.setWorkCalendar(null);
      }
   }
      
   public Set<OpResourcePool> getResourcePools() {
      return resourcePools;
   }

   private void setResourcePools(Set<OpResourcePool> resourcePools) {
      this.resourcePools = resourcePools;
   }

   public void addResourcePool(OpResourcePool r) {
      if (getResourcePools() == null) {
         setResourcePools(new HashSet<OpResourcePool>());
      }
      if (getResourcePools().add(r)) {
         r.setWorkCalendar(this);
      }
   }
      
   public void removeResourcePool(OpResourcePool r) {
      if (getResourcePools() == null) {
         return;
      }
      if (getResourcePools().remove(r)) {
         r.setWorkCalendar(null);
      }
   }
      
   public Set<OpProjectPlanVersion> getProjectPlanVersions() {
      return projectPlanVersions;
   }

   private void setProjectPlanVersions(Set<OpProjectPlanVersion> projectPlanVersions) {
      this.projectPlanVersions = projectPlanVersions;
   }
   
   public void addProjectPlanVersion(OpProjectPlanVersion pv) {
      if (getProjectPlanVersions() == null) {
         setProjectPlanVersions(new HashSet<OpProjectPlanVersion>());
      }
      if (getProjectPlanVersions().add(pv)) {
         pv.setWorkCalendar(this);
      }
   }

   public void removeProjectPlanVersion(OpProjectPlanVersion pv) {
      if (getProjectPlanVersions() == null) {
         return;
      }
      if (getProjectPlanVersions().remove(pv)) {
         pv.setWorkCalendar(null);
      }
   }

   public Set<OpProjectPlan> getProjectPlans() {
      return projectPlans;
   }

   private void setProjectPlans(Set<OpProjectPlan> projectPlans) {
      this.projectPlans = projectPlans;
   }

   public void addProjectPlan(OpProjectPlan pp) {
      if (getProjectPlans() == null) {
         setProjectPlans(new HashSet<OpProjectPlan>());
      }
      if (getProjectPlans().add(pp)) {
         pp.setWorkCalendar(this);
      }
   }

   public void removeProjectPlan(OpProjectPlan pp) {
      if (getProjectPlans() == null) {
         return;
      }
      if (getProjectPlans().remove(pp)) {
         pp.setWorkCalendar(null);
      }
   }

   public int getFirstWorkday() {
      return firstWorkday;
   }

   public void setFirstWorkday(String firstWorkday) {
      setFirstWorkday(Integer.parseInt(firstWorkday));
   }

   public void setFirstWorkday(int firstWorkday) {
      this.firstWorkday = firstWorkday;
   }

   public int getLastWorkday() {
      return lastWorkday;
   }

   public void setLastWorkday(String lastWorkday) {
      setLastWorkday(Integer.parseInt(lastWorkday));
   }

   public void setLastWorkday(int lastWorkday) {
      this.lastWorkday = lastWorkday;
   }

   public double getWorkHoursPerDay() {
      return workHoursPerDay;
   }

   public void setWorkHoursPerDay(double workHoursPerDay) {
      this.workHoursPerDay = workHoursPerDay;
   }

   public String getHolidayCalendarId() {
      return holidayCalendarId;
   }

   public void setHolidayCalendarId(String holidayCalendarId) {
      this.holidayCalendarId = holidayCalendarId;
   }

   public boolean isHolidaysAreWorkdays() {
      return holidaysAreWorkdays;
   }

   public void setHolidaysAreWorkdays(boolean holidaysAreWorkdays) {
      this.holidaysAreWorkdays = holidaysAreWorkdays;
   }
   
   public void setHolidaysAreWorkdays(Boolean holidaysAreWorkdays) {
      this.holidaysAreWorkdays = holidaysAreWorkdays != null ? holidaysAreWorkdays.booleanValue() : false;
   }
   
   public boolean isInUse() {
      return (getProjectPlans() != null && !getProjectPlans().isEmpty())
            || (getProjectPlanVersions() != null && !getProjectPlanVersions()
                  .isEmpty())
            || (getResources() != null && !getResources().isEmpty())
            || (getResourcePools() != null && !getResourcePools().isEmpty());
   }

   public boolean isDefaultCalendar() {
      return defaultCalendar;
   }

   public void setDefaultCalendar(boolean defaultCalendar) {
      this.defaultCalendar = defaultCalendar;
   }
   
   public void setDefaultCalendar(Boolean defaultCalendar) {
      this.defaultCalendar = defaultCalendar != null ? defaultCalendar.booleanValue() : false;
   }
   
   
}
