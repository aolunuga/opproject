/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.project.OpProjectSession;

public class OpPermission extends OpObject {
   
   public final static String PERMISSION = "OpPermission";
   
   // Access levels
   public final static byte ADMINISTRATOR = 64;
   public final static byte MANAGER = 16;
   public final static byte CONTRIBUTOR = 4;
   public final static byte OBSERVER = 2;
   public final static byte EXTERNAL = 1;
   public final static byte DEFAULT_ACCESS_LEVEL = CONTRIBUTOR;

   public final static byte[] PERMISSION_TYPE = { ADMINISTRATOR, MANAGER, CONTRIBUTOR, OBSERVER, EXTERNAL };
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

   /**
    * @param broker 
    * @param session 
    * @param permissions
    * @return
    * @pre
    * @post
    */
   public static Map<Byte, Set<OpUser>> expandPermissionsToUsers(
         OpProjectSession session, OpBroker broker, Set<OpPermission> permissions) {
      Map<Byte, Set<OpUser>> ret = new HashMap<Byte, Set<OpUser>>();
      ret.put(OpPermission.ADMINISTRATOR, new HashSet<OpUser>());
      ret.put(OpPermission.MANAGER, new HashSet<OpUser>());
      ret.put(OpPermission.CONTRIBUTOR, new HashSet<OpUser>());
      ret.put(OpPermission.OBSERVER, new HashSet<OpUser>());
      ret.put(OpPermission.EXTERNAL, new HashSet<OpUser>());
      if (permissions == null) {
         return ret;
      }
      for (OpPermission perm : permissions) {
         OpSubject subject = perm.getSubject();
         
         if ((!(subject instanceof OpGroup)) && (!(subject instanceof OpUser))) {
            // sometimes hibernate proxies wrap the wrong (in this case OpSubject) object
            if (subject instanceof HibernateProxy) {
               subject = (OpSubject)((HibernateProxy)subject).getHibernateLazyInitializer().getImplementation();
            }
         }

         Set<OpUser> permSet = ret.get(perm.getAccessLevel());
         if (subject instanceof OpGroup) {
            Set<OpUserAssignment> ass = ((OpGroup) subject).getUserAssignments();
            for (OpUserAssignment as : ass) {
               permSet.add(as.getUser());
            }
         }
         else {
            permSet.add((OpUser) subject);
         }
      }
      // remove users from eg contributors if they are also manager...
      for (int pos = 0; pos < OpPermission.PERMISSION_TYPE.length; pos++) {
         Set<OpUser> permSet = ret.get(OpPermission.EXTERNAL);
         for (int count = pos; count < OpPermission.PERMISSION_TYPE.length; count++) {
            Set<OpUser> removeSet = ret.get(OpPermission.PERMISSION_TYPE[count]);
            removeSet.removeAll(permSet);
         }
      }
      return ret;
   }
}
