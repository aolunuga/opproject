/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.persistence.OpObject;

public class OpUserAssignment extends OpObject {
   
   public final static String USER_ASSIGNMENT = "OpUserAssignment";

   private OpUser user;
   private OpGroup group;

   public void setUser(OpUser user) {
      this.user = user;
   }

   public OpUser getUser() {
      return user;
   }

   public void setGroup(OpGroup group) {
      this.group = group;
   }

   public OpGroup getGroup() {
      return group;
   }

}
