/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.persistence.OpObject;

public class OpPermission extends OpObject {
   
   public final static String PERMISSION = "OpPermission";
   
   // Access levels
   public final static byte ADMINISTRATOR = 64;
   public final static byte MANAGER = 16;
   public final static byte CONTRIBUTOR = 4;
   public final static byte OBSERVER = 2;
   public final static byte EXTERNAL = 1;
   public final static byte DEFAULT_ACCESS_LEVEL = CONTRIBUTOR;

   // Maybe add another hierarchy level XGuardedObject which knows about permissions?
   // (Major drawback: Hierarchy levels drain performance in relational environments)

   public final static String OBJECT = "Object";
   public final static String SUBJECT = "Subject";
   public final static String ACCESS_LEVEL = "AccessLevel";
   public final static String SYSTEM_MANAGED = "SystemManaged";

   private OpObject object;
   private OpSubject subject;
   private byte accessLevel = DEFAULT_ACCESS_LEVEL;
   private boolean systemManaged = false;

   public void setObject(OpObject object) {
      this.object = object;
   }

   public OpObject getObject() {
      return object;
   }

   public void setSubject(OpSubject subject) {
      this.subject = subject;
   }

   public OpSubject getSubject() {
      return subject;
   }

   public void setAccessLevel(byte accessLevel) {
      this.accessLevel = accessLevel;
   }

   public byte getAccessLevel() {
      return accessLevel;
   }
   
   public void setSystemManaged(boolean systemManaged) {
      this.systemManaged = systemManaged;
   }
   
   public boolean getSystemManaged() {
      return systemManaged;
   }

}
