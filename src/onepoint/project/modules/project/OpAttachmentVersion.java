/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.documents.OpContent;

public class OpAttachmentVersion extends OpObject {

   public final static String ATTACHMENT_VERSION = "OpAttachmentVersion";

   public final static String NAME = "Name";
   public final static String LINKED = "Linked";
   public final static String LOCATION = "Location";
   public final static String CONTENT = "Content";
   public final static String PLAN_VERSION = "PlanVersion";
   public final static String ACTIVITY_VERSION = "ActivityVersion";

   private String name;
   private boolean linked;
   private String location;
   private OpContent content;
   private OpProjectPlanVersion planVersion;
   private OpActivityVersion activityVersion;

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setLinked(boolean linked) {
      this.linked = linked;
   }

   public boolean getLinked() {
      return linked;
   }

   public void setLocation(String location) {
      this.location = location;
   }

   public String getLocation() {
      return location;
   }

   public void setContent(OpContent content) {
      this.content = content;
   }

   public OpContent getContent() {
      return content;
   }

   public void setPlanVersion(OpProjectPlanVersion planVersion) {
      this.planVersion = planVersion;
   }
   
   public OpProjectPlanVersion getPlanVersion() {
      return planVersion;
   }

   public void setActivityVersion(OpActivityVersion activityVersion) {
      this.activityVersion = activityVersion;
   }

   public OpActivityVersion getActivityVersion() {
      return activityVersion;
   }

}
