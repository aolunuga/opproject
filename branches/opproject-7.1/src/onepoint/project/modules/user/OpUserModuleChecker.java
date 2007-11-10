/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 *
 */

package onepoint.project.modules.user;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleChecker;

import java.util.Iterator;
import java.util.List;

/**
 * Checker class for User module.
 *
 * @author mihai.costin
 */
public class OpUserModuleChecker implements OpModuleChecker {

   private static final XLog logger = XLogFactory.getServerLogger(OpUserModuleChecker.class);

   public void check(OpProjectSession session) {
      assignAdministratorPermission(session);
   }

   /**
    * Add the administrator permission on all the db opobjects if missing.
    *
    * @param session project session
    */
   private void assignAdministratorPermission(OpProjectSession session) {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("select obj.ID from OpObject obj inner join obj.Permissions permission where permission.Subject.Name='" + OpUser.ADMINISTRATOR_NAME + "'");
      List objectWithPermissions = broker.list(query);
      broker.close();

      addPermissionsForPrototype(session, objectWithPermissions, "OpResource");
      addPermissionsForPrototype(session, objectWithPermissions, "OpResourcePool");
      addPermissionsForPrototype(session, objectWithPermissions, "OpProjectNode");
      addPermissionsForPrototype(session, objectWithPermissions, "OpProjectPlan");
      addPermissionsForPrototype(session, objectWithPermissions, "OpProjectPlanVersion");
      addPermissionsForPrototype(session, objectWithPermissions, "OpAttachment");
   }

   /**
    * Adds administrator permissions for the given prototype on all the objects thate are not in the given list.
    *
    * @param session               project session
    * @param objectWithPermissions objects to exlude from permission update
    * @param prototype             name of the prototype included in the permission upgrade
    */
   private void addPermissionsForPrototype(OpProjectSession session, List objectWithPermissions, String prototype) {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("select obj from " + prototype + " obj where obj.ID not in (:adminObjects)");
      query.setCollection("adminObjects", objectWithPermissions);
      Iterator iterator = broker.iterate(query);
      if (iterator.hasNext()) {
         OpUser user = session.administrator(broker);
         OpTransaction transaction = broker.newTransaction();
         while (iterator.hasNext()) {
            OpObject object = (OpObject) iterator.next();
            OpPermission permission = new OpPermission();
            permission.setObject(object);
            permission.setSubject(user);
            permission.setAccessLevel(OpPermission.ADMINISTRATOR);
            broker.makePersistent(permission);
            logger.info("Adding Administrator permission for object " + prototype + " with ID " + object.getID());
         }
         transaction.commit();
      }
      broker.closeAndEvict();
      session.cleanupSession(false);
   }

}
