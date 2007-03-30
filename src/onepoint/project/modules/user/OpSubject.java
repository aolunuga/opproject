/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.persistence.OpObject;

import java.util.Set;

public class OpSubject extends OpObject {

   public final static String NAME = "Name";
   public final static String DISPLAY_NAME = "DisplayName";
   public final static String DESCRIPTION = "Description";
   public final static String OWNED_PERMISSIONS = "OwnedPermissions";

   private String name;
   private String displayName;
   private String description;
   private Set ownedPermissions;

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public String getDisplayName() {
      return displayName;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getDescription() {
      return description;
   }

   public void setOwnedPermissions(Set ownedPermissions) {
      this.ownedPermissions = ownedPermissions;
   }

   public Set getOwnedPermissions() {
      return ownedPermissions;
   }

}
