/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.work.OpCostRecord;

public class OpAttachment extends OpObject {
   
   public final static String ATTACHMENT = "OpAttachment";

   public final static String NAME = "Name";
   public final static String LINKED = "Linked";
   public final static String LOCATION = "Location";
   public final static String CONTENT = "Content";
   public final static String PROJECT_PLAN = "ProjectPlan";
   public final static String ACTIVITY = "Activity";

   private String name;
   private boolean linked;
   private String location;
   private OpContent content;
   private OpProjectPlan projectPlan;
   private OpActivity activity;
   private OpCostRecord costRecord;
   private OpProjectNode projectNode;

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
   
   public void setProjectPlan(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }
   
   public OpProjectPlan getProjectPlan() {
      return projectPlan;
   }

   public void setActivity(OpActivity activity) {
      this.activity = activity;
   }

   public OpActivity getActivity() {
      return activity;
   }

   public OpCostRecord getCostRecord() {
      return costRecord;
   }

   public void setCostRecord(OpCostRecord costRecord) {
      this.costRecord = costRecord;
   }

    public OpProjectNode getProjectNode() {
      return projectNode;
   }

   public void setProjectNode(OpProjectNode projectNode) {
      this.projectNode = projectNode;
   }
}
