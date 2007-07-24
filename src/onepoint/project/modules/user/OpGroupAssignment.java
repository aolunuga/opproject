/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.persistence.OpObject;

public class OpGroupAssignment extends OpObject {
   
   public final static String GROUP_ASSIGNMENT = "OpGroupAssignment";

   private OpGroup superGroup;
   private OpGroup subGroup;

   public void setSuperGroup(OpGroup superGroup) {
      this.superGroup = superGroup;
   }

   public OpGroup getSuperGroup() {
      return superGroup;
   }

   public void setSubGroup(OpGroup subGroup) {
      this.subGroup = subGroup;
   }

   public OpGroup getSubGroup() {
      return subGroup;
   }

}
