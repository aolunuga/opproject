/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.project.modules.user;

import java.util.Set;

import onepoint.persistence.OpObjectIfc;

/**
 * @author dfreis
 *
 */
public interface OpPermissionable extends OpObjectIfc {

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#setPermissions(java.util.Set)
    */
   public abstract void setPermissions(Set<OpPermission> permissions);

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#getPermissions()
    */
   public abstract Set<OpPermission> getPermissions();

   /**
    * @param permission
    */
   public abstract void addPermission(OpPermission permission);

   /**
    * @param permission
    */
   public abstract void removePermission(OpPermission permission);

}