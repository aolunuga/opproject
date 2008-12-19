/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;

public class OpAttachmentVersion extends OpObject implements OpPermissionable, OpAttachmentIfc {

   public final static String ATTACHMENT_VERSION = "OpAttachmentVersion";

   public final static String NAME = "Name";
   public final static String LINKED = "Linked";
   public final static String LOCATION = "Location";
   public final static String CONTENT = "Content";

   private String name;
   private boolean linked;
   private String location;
   private OpContent content;
   private OpActivityVersion activityVersion;

   private Set<OpPermission> permissions;

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#setName(java.lang.String)
    */
   public void setName(String name) {
      this.name = name;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#getName()
    */
   public String getName() {
      return name;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#setLinked(boolean)
    */
   public void setLinked(boolean linked) {
      this.linked = linked;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#getLinked()
    */
   public boolean getLinked() {
      return linked;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#setLocation(java.lang.String)
    */
   public void setLocation(String location) {
      this.location = location;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#getLocation()
    */
   public String getLocation() {
      return location;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#setContent(onepoint.project.modules.documents.OpContent)
    */
   public void setContent(OpContent content) {
      this.content = content;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#getContent()
    */
   public OpContent getContent() {
      return content;
   }

   public void setActivityVersion(OpActivityVersion activityVersion) {
      this.activityVersion = activityVersion;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#getActivityVersion()
    */
   // deprecated
   public OpActivityVersion getActivityVersion() {
      return activityVersion;
   }
   
   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#getPermissions()
    */
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#getPermissions()
    */
   public Set<OpPermission> getPermissions() {
      return permissions;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.user.OpPermissionable#setPermissions(java.util.Set)
    */
   public void setPermissions(Set<OpPermission> permissions) {
      this.permissions = permissions;
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#addPermission(onepoint.project.modules.user.OpPermission)
    */
   public void addPermission(OpPermission permission) {
      Set<OpPermission> perm = getPermissions();
      if (perm == null) {
         perm = new HashSet<OpPermission>();
         setPermissions(perm);
      }
      perm.add(permission);
      permission.setObject(this);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAttachmentIfc#removePermission(onepoint.project.modules.user.OpPermission)
    */
   public void removePermission(OpPermission opPermission) {
      Set<OpPermission> perm = getPermissions();
      if (perm != null) {
         perm.remove(opPermission);
      }
      opPermission.setObject(null);
   }

   public OpActivityIfc getActivityIfc() {
      return getActivityVersion();
   }

   public OpActivityVersion getActivity() {
      return activityVersion;
   }

}
