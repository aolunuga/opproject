/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpTypeManager;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;
import onepoint.project.modules.work.OpCostRecord;

public class OpAttachment extends OpObject implements OpPermissionable, OpAttachmentIfc {
   
   public final static String ATTACHMENT = "OpAttachment";

   public final static String NAME = "Name";
   public final static String LINKED = "Linked";
   public final static String LOCATION = "Location";
   public final static String CONTENT = "Content";

   private String name;
   private boolean linked;
   private String location;
   private OpContent content;
   private OpActivity activity;
   private OpCostRecord costRecord;
   private OpProjectNode projectNode;

   private Set<OpPermission> permissions;

   /**
    * 
    */
   public OpAttachment() {
   }

   public OpAttachment(String name, String location, OpContent content) {
      this.name = name;
      this.location= location;
      this.content = content;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#clone()
    */
   @Override
   public Object clone() {
      return new OpAttachment(getName(), getLocation(), getContent());
   }

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

   /**
    * Returns the <code>OpObject</code> (which is either an <code>OpActivity</code>, a <code>OpProjectNode</code> or
    *    an <code>OpCostRecord</code> object)to which the <code>OpAttachment</code> is associated.
    *
    * @return the <code>OpObject</code> (which is either an <code>OpActivity</code>, a <code>OpProjectNode</code> or
    *         an <code>OpCostRecord</code> object)to which the <code>OpAttachment</code> is associated.
    */

   public OpObjectIfc getObject() {
      if (getActivity() != null) {
         return getActivity();
      }
      return getCostRecord();
   }

   /**
    * Sets an <code>OpObject</code> (which is either an <code>OpActivity</code>, a <code>OpProjectNode</code> or
    * a <code>OpCostRecord</code>) on the attachment.
    *
    * @param object - the <code>OpActivity</code>, <code>OpProjectNode</code> or <code>OpCostRecord</code> object
    *               which will be set on the attachment.
    */
   public void setObject(OpObjectIfc object) {
      if (OpTypeManager.getPrototypeForObject(object).getName().equals(OpActivity.ACTIVITY)) {
         setActivity((OpActivity) object);
      }
      if (OpTypeManager.getPrototypeForObject(object).getName().equals(OpCostRecord.COST_RECORD)) {
         setCostRecord((OpCostRecord) object);
      }
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
   
   private void setProjectPlan(OpProjectPlan projectPlan) {
      this.projectNode = projectPlan.getProjectNode();
   }

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


   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "{name: "+name+", location: "+location+", linked: "+linked+"}";
   }

   public OpActivityIfc getActivityIfc() {
      return getActivity();
   }
}
