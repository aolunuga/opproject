/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 *
 */

package onepoint.project.modules.user;

import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleChecker;

/**
 * @author mihai.costin
 */
public class OpUserModuleChecker implements OpModuleChecker {
   public void check(OpProjectSession session) {
      //assignAdministratorPermission
   }




//   public void assignAdministratorPermission(OpProjectSession session) {
//      OpBroker broker = session.newBroker();
//      OpQuery query = broker.newQuery("select obj from OpObject obj  where (select count(permission) from OpPermission permission where permission.Object.ID = obj.ID AND permission.Subject.Name = '" + OpUser.ADMINISTRATOR_NAME + "')=0");
//      Iterator iterator = broker.iterate(query);
//      OpUser user = session.administrator(broker);
//      OpTransaction transaction = broker.newTransaction();
//      while(iterator.hasNext()) {
//         OpObject object = (OpObject) iterator.next();
//         OpPermission permission = new OpPermission();
//         permission.setObject(object);
//         permission.setSubject(user);
//         permission.setAccessLevel(OpPermission.ADMINISTRATOR);
//         broker.makePersistent(permission);
//         logger.info("Adding Administrator permission for object " + object.getID());
//      }
//      transaction.commit();
//      broker.close();
//   }

}
