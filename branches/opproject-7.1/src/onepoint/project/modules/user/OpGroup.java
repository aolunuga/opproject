/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import java.util.Set;

public class OpGroup extends OpSubject {

   public final static String GROUP = "OpGroup";

   public final static String USER_ASSIGNMENTS = "UserAssignments";
   public final static String SUPER_GROUP_ASSIGNMENTS = "SuperGroupAssignments";
   public final static String SUB_GROUP_ASSIGNMENTS = "SubGroupAssignments";

   // Name and resource references of hard-wired group "everyone"
   public final static String EVERYONE_NAME = "Everyone";
   public final static String EVERYONE_DISPLAY_NAME = "${EveryoneDisplayName}";
   public final static String EVERYONE_DESCRIPTION = "${EveryoneDescription}";
   public final static String EVERYONE_ID_QUERY = "select group.ID from OpGroup as group where group.Name = '" + EVERYONE_NAME + "'";

   private Set userAssignments;
   private Set superGroupAssignments;
   private Set subGroupAssignments;

   public void setSuperGroupAssignments(Set superGroupAssignments) {
      this.superGroupAssignments = superGroupAssignments;
   }

   public Set getSuperGroupAssignments() {
      return superGroupAssignments;
   }

   public void setSubGroupAssignments(Set subGroupAssignments) {
      this.subGroupAssignments = subGroupAssignments;
   }

   public Set getSubGroupAssignments() {
      return subGroupAssignments;
   }

   public void setUserAssignments(Set userAssignments) {
      this.userAssignments = userAssignments;
   }

   public Set<OpUserAssignment> getUserAssignments() {
      return userAssignments;
   }

}
