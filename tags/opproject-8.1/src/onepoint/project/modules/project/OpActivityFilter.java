/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

public class OpActivityFilter {

   public static final int ALL = -1;

   private boolean templates = false;
   private boolean dependencies = false;
   private boolean workPhases = false;
   private boolean exportedOnly = false;
   private boolean doNotFlatten = false;
   private boolean withAssignments = false;
   private int maxOutlineLevel = ALL; // means ALL!
   private HashSet<Long> projectNodeIds = new HashSet<Long>();
   private HashSet<Long> resourceIds = new HashSet<Long>();
   private HashSet<Byte> types = new HashSet<Byte>();
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

   public boolean isExportedOnly() {
      return exportedOnly;
   }

   public void setExportedOnly(boolean exportedOnly) {
      this.exportedOnly = exportedOnly;
   }

   public boolean doNotFlatten() {
      return doNotFlatten;
   }

   public void setDoNotFlatten(boolean doNotFlatten) {
      this.doNotFlatten = doNotFlatten;
   }

   public boolean isWithAssignments() {
      return withAssignments;
   }

   public void setWithAssignments(boolean withAssignments) {
      this.withAssignments = withAssignments;
   }

   public int getMaxOutlineLevel() {
      return maxOutlineLevel;
   }

   public void setMaxOutlineLevel(int maxOutlineLevel) {
      this.maxOutlineLevel = maxOutlineLevel;
   }

   public final boolean getWorkPhases() {
      return workPhases;
   }

   public final void addProjectNodeID(long projectNodeId) {
      projectNodeIds.add(new Long(projectNodeId));
   }

   public final Set<Long> getProjectNodeIds() {
      return projectNodeIds;
   }

   public final void addResourceID(long resourceId) {
      resourceIds.add(new Long(resourceId));
   }

   public final Set<Long> getResourceIds() {
      return resourceIds;
   }

   public final void addType(byte type) {
      types.add(new Byte(type));
   }

   public final Set<Byte> getTypes() {
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

   /**
    * @param id
    * @return
    * @pre
    * @post
    */
   public boolean containsResourceID(long id) {
      return resourceIds.contains(id);
   }
}
