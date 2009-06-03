/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 *
 */

package onepoint.project.modules.user;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.OpTypeManager;
import onepoint.persistence.hibernate.OpMappingsGenerator;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleChecker;

import org.hibernate.ObjectNotFoundException;

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
      fixPermissionsWithNonExistingObjects(session); 
      assignAdministratorPermission(session);
   }

   private void fixPermissionsWithNonExistingObjects(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {

         OpPrototype pt = OpTypeManager.getPrototypeByClass(OpPermissionable.class);
         Set<OpPrototype> subTypes = pt.getSubTypes();
         if (subTypes != null) {
            OpTransaction t = broker.newTransaction();
            for (OpPrototype p : subTypes) {
               String id = OpMappingsGenerator.getIdForImplementingType(p);
               String queryString = "select p from OpPermission p" +
               " where p.Object.class = :id" +
               " and not exists(select x from "+p.getInstanceClass().getName()+" x where p.Object.id = x.id)";
               OpQuery query = broker.newQuery(queryString);
               query.setString("id", id);
               Iterator iter = broker.iterate(query);
               while (iter.hasNext()) {
                  OpPermission perm = (OpPermission)iter.next();
                  perm.setSubject(null);
                  broker.deleteObject(perm);
               }
            }
            t.commit();
         }
      } finally {
         broker.close();
      }
      
   }

   /**
    * Add the administrator permission on all the db opobjects if missing.
    *
    * @param session project session
    */
   private void assignAdministratorPermission(OpProjectSession session) {
      List permissionsWithObjects = null;
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select permission from OpPermission permission where permission.Subject.Name='" + OpUser.ADMINISTRATOR_NAME + "'");
         permissionsWithObjects = broker.list(query);
         if (permissionsWithObjects != null) {
            addPermissionsForPrototype(session, permissionsWithObjects, "OpResource");
            addPermissionsForPrototype(session, permissionsWithObjects, "OpResourcePool");
            addPermissionsForPrototype(session, permissionsWithObjects, "OpProjectNode");
            addPermissionsForPrototype(session, permissionsWithObjects, "OpProjectPlan");
            addPermissionsForPrototype(session, permissionsWithObjects, "OpProjectPlanVersion");
            addPermissionsForPrototype(session, permissionsWithObjects, "OpAttachment");
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
    * @param permissionsWithObject 
    * @param prototype             name of the prototype included in the permission upgrade
    */
   private void addPermissionsForPrototype(OpProjectSession session, List permissionsWithObject, String prototype) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction transaction = broker.newTransaction();
         int opCount = 0;
         OpUser user = session.administrator(broker);
         Iterator iterator = permissionsWithObject.iterator();
         Set<OpPermission> toBeDeleted = new HashSet<OpPermission>();
         if (iterator.hasNext()) {
            while (iterator.hasNext()) {
               OpPermission p = ((OpPermission) iterator.next());
               OpPermissionable object = p.getObject();
               if (object != null) {
                  try {
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
                  catch (NullPointerException npx) {
                     logger.warn("NullPointerException.", npx);
//                     toBeDeleted.add(p);
                  }
                  catch (ObjectNotFoundException nex) {
                     try {
                        logger.warn("Permission referencing none-existent object " + nex.getIdentifier() + " of type " + prototype + ". Refering permission: " + p.locator());
                        // p.setObject(null);
                        toBeDeleted.add(p);
                     }
                     catch (Exception e) {
                        logger.error("Strange things happened: " , e);
                     }
                  }
               }
               else {
                  logger.warn("Permission refering null object of type " + prototype + ". Permission: " + p.locator());
                  toBeDeleted.add(p);
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
//         for (OpPermission del:toBeDeleted) {
//            // p.setObject(null);
//            transaction = broker.newTransaction();
//            OpPermission opPermission = (OpPermission)broker.getObject(del.locator());
//            opPermission.getSubject().removeOwnedPermission(opPermission);
////            opPermission.setSubject(null);
//            transaction.commit();
//            transaction = broker.newTransaction();
////            broker.getJDBCConnection().
//            broker.deleteObject(broker.getObject(del.locator()));
//            transaction.commit();
//         }
//         transaction.commit();
      }
      finally {
         broker.closeAndEvict();
         session.cleanupSession(false);
      }
   }

}
