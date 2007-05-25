/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.persistence.OpObject;

public class OpLock extends OpObject {

   public final static String LOCK = "OpLock";

   public final static String OWNER = "Owner";
   public final static String TARGET = "Target";

   private OpUser owner;
   private OpObject target;

   public void setOwner(OpUser owner) {
      this.owner = owner;
   }

   public OpUser getOwner() {
      return owner;
   }

   public void setTarget(OpObject target) {
      this.target = target;
   }

   public OpObject getTarget() {
      return target;
   }

}
