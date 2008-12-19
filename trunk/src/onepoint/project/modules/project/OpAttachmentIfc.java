package onepoint.project.modules.project;

import java.util.Set;

import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.user.OpPermission;

public interface OpAttachmentIfc {

   public abstract void setName(String name);

   public abstract String getName();

   public abstract void setLinked(boolean linked);

   public abstract boolean getLinked();

   public abstract void setLocation(String location);

   public abstract String getLocation();

   public abstract void setContent(OpContent content);

   public abstract OpContent getContent();

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#getPermissions()
    */
   public abstract Set<OpPermission> getPermissions();

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#setPermissions(java.util.Set)
    */
   public abstract void setPermissions(Set<OpPermission> permissions);

   public abstract void addPermission(OpPermission permission);

   /**
    * @param opPermission
    * @pre
    * @post
    */
   public abstract void removePermission(OpPermission opPermission);
   
   public OpActivityIfc getActivityIfc();

   public abstract String locator();

}