/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 *
 */

package onepoint.project.modules.user;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleChecker;

/**
 * Checker class for User module.
 *
 * @author mihai.costin
 */
public class OpUserModuleChecker implements OpModuleChecker {

   /**
    * This class's logger
    */
   private static final XLog logger = XLogFactory.getLogger(OpUserModuleChecker.class);

   /**
    * @see onepoint.project.module.OpModuleChecker#check(onepoint.project.OpProjectSession)
    */
   public void check(OpProjectSession session) {
      assignAdministratorPermission(session);
   }

   /**
    * Add the administrator permission on all the db opobjects if missing.
    *
    * @param session project session
    */
   private void assignAdministratorPermission(OpProjectSession session) {
      List objectWithPermissions = null;
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select permission from OpPermission permission where permission.Subject.Name='" + OpUser.ADMINISTRATOR_NAME + "'");
         objectWithPermissions = broker.list(query);
         if (objectWithPermissions != null) {
            addPermissionsForPrototype(session, objectWithPermissions, "OpResource");
            addPermissionsForPrototype(session, objectWithPermissions, "OpResourcePool");
            addPermissionsForPrototype(session, objectWithPermissions, "OpProjectNode");
            addPermissionsForPrototype(session, objectWithPermissions, "OpProjectPlan");
            addPermissionsForPrototype(session, objectWithPermissions, "OpProjectPlanVersion");
            addPermissionsForPrototype(session, objectWithPermissions, "OpAttachment");
         }
      }
      finally {
         broker.closeAndEvict();
      }
   }

   /**
    * Adds administrator permissions for the given prototype on all the objects thate are not in the given list.
    *
    * @param session               project session
    * @param objectWithPermissions 
    * @param prototype             name of the prototype included in the permission upgrade
    */
   private void addPermissionsForPrototype(OpProjectSession session, List objectWithPermissions, String prototype) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction transaction = broker.newTransaction();
         int opCount = 0;
         OpUser user = session.administrator(broker);
         Iterator iterator = objectWithPermissions.iterator();
         if (iterator.hasNext()) {
            while (iterator.hasNext()) {
               OpPermissionable object = ((OpPermission) iterator.next()).getObject();
               if (object != null) {
                  // check if object has admin perms
                  Set<OpPermission> perms = object.getPermissions();
                  boolean found = false;
                  if (perms != null) {
                     for (OpPermission perm : perms) {
                        if ((perm.getSubject().getId() == user.getId()) && (perm.getAccessLevel() == OpPermission.ADMINISTRATOR)) {
                           found = true;
                           break;
                        }
                     }
                  }
                  if (!found) {
                     OpPermission permission = new OpPermission(object, user, OpPermission.ADMINISTRATOR);
                     broker.makePersistent(permission);
                     logger.info("Adding administrator permission for object " + prototype + " with ID " + object.getId());
                  }
               }
            }
         }
         else {
            String queryStr = "select obj from " + prototype + " obj";
            OpQuery query = broker.newQuery(queryStr);
            iterator = broker.iterate(query);
            while (iterator.hasNext()) {
               OpPermissionable object = (OpPermissionable) iterator.next();
               OpPermission permission = new OpPermission(object, user, OpPermission.ADMINISTRATOR);
               broker.makePersistent(permission);
               logger.info("Adding new administrator permission for object " + prototype + " with ID " + object.getId());
            }
         }
         transaction.commit();
      }
      finally {
         broker.closeAndEvict();
         session.cleanupSession(false);
      }
   }

}
