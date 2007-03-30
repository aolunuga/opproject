/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

import java.util.Set;

public class OpActivityCategory extends OpObject {

   public final static String ACTIVITY_CATEGORY = "OpActivityCategory";

   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";
   public final static String COLOR = "Color";
   public final static String PROJECT_PLAN = "ProjectPlan";
   public final static String ACTIVITIES = "Activities";
   public final static String ACTIVITY_VERSIONS = "ActivityVersions";

   private String name;
   private String description;
   private int color;
   private Set activities;
   private Set activityVersions;

   /**
    * Flag indicating whether a category is active or not (i.e deleted but referenced)
    */
   private boolean active = true;

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

   public void setColor(int color) {
      this.color = color;
   }

   public int getColor() {
      return color;
   }

   public void setActivities(Set activities) {
      this.activities = activities;
   }

   public Set getActivities() {
      return activities;
   }

   public void setActivityVersions(Set activityVersions) {
      this.activityVersions = activityVersions;
   }

   public Set getActivityVersions() {
      return activityVersions;
   }

   /**
    * Gets the value of the active flag.
    * @return a <code>boolean</code> indicating whether the category is active or not.
    */
   public boolean getActive() {
      return active;
   }

   /**
    * Sets the value of the active flag.
    * @param active a <code>boolean</code> indicating whether the category is active or not.
    */
   public void setActive(boolean active) {
      this.active = active;
   }
}
