/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

public class OpGoal extends OpObject {
   
   public final static String GOAL = "OpGoal";

   public final static String NAME = "Name";
   public final static String PRIORITY = "Priority";
   public final static String COMPLETED = "Completed";
   public final static String PROJECT_NODE = "ProjectNode";
   // *** Maybe add Description

   private String name;
   private byte priority;
   private boolean completed;
   private OpProjectNode projectNode;

   public void setName(String name) {
      if (name == null) {
         name = "";
      }
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setPriority(byte priority) {
      this.priority = priority;
   }

   public byte getPriority() {
      return priority;
   }

   public void setCompleted(boolean completed) {
      this.completed = completed;
   }

   public boolean getCompleted() {
      return completed;
   }

   public void setProjectNode(OpProjectNode projectNode) {
      this.projectNode = projectNode;
   }

   public OpProjectNode getProjectNode() {
      return projectNode;
   }

}
