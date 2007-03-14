/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.util.ArrayList;

public class OpActivityFilter {

   private boolean templates = false;
   private boolean dependencies = false;
   private boolean workPhases = false;
   private ArrayList projectNodeIds = new ArrayList();
   private ArrayList resourceIds = new ArrayList();
   private ArrayList types = new ArrayList();
   private Date startFrom = null;
   private Date startTo = null;
   private Boolean completed = null;
   private Boolean assignmentCompleted = null;

   public final void setTemplates(boolean templates) {
      this.templates = templates;
   }

   public final boolean getTemplates() {
      return templates;
   }

   public final void setDependencies(boolean dependencies) {
      this.dependencies = dependencies;
   }

   public final boolean getDependencies() {
      return dependencies;
   }

   public final void setWorkPhases(boolean workPhases) {
      this.workPhases = workPhases;
   }

   public final boolean getWorkPhases() {
      return workPhases;
   }

   public final void addProjectNodeID(long projectNodeId) {
      projectNodeIds.add(new Long(projectNodeId));
   }

   public final ArrayList getProjectNodeIds() {
      return projectNodeIds;
   }

   public final void addResourceID(long resourceId) {
      resourceIds.add(new Long(resourceId));
   }

   public final ArrayList getResourceIds() {
      return resourceIds;
   }

   public final void addType(byte type) {
      types.add(new Byte(type));
   }

   public final ArrayList getTypes() {
      return types;
   }
   
   public final void setStartFrom(Date startFrom) {
      this.startFrom = startFrom;
   }
   
   public final Date getStartFrom() {
      return startFrom;
   }

   public final void setStartTo(Date startTo) {
      this.startTo = startTo;
   }
   
   public final Date getStartTo() {
      return startTo;
   }

   public final void setCompleted(Boolean completed) {
      this.completed = completed;
   }
   
   public final Boolean getCompleted() {
      return completed;
   }

   public void setAssignmentCompleted(Boolean assignmentComplete) {
      assignmentCompleted = assignmentComplete;
   }

   public Boolean getAssignmentComplete() {
      return assignmentCompleted;
   }
}
