/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.modules.backup.OpBackupManager;

import java.util.Iterator;

public class OpUserModule extends OpModule {

   @Override
   public void start(OpProjectSession session) {
      // Check if hard-wired group object "Everyone" exists (if not create it)
      OpBroker broker = session.newBroker();
      if (session.administrator(broker) == null) {
         OpUserService.createAdministrator(broker);
      }
      if (session.everyone(broker) == null) {
         OpUserService.createEveryone(broker);
      }

      //register system objects with Backup manager
      OpBackupManager.addSystemObjectIDQuery(OpUser.ADMINISTRATOR_NAME, OpUser.ADMINISTRATOR_ID_QUERY);
      OpBackupManager.addSystemObjectIDQuery(OpGroup.EVERYONE_NAME, OpGroup.EVERYONE_ID_QUERY);

      broker.close();
   }

   /**
    * Upgrades this module to version #5 (via reflection - must be public).
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion12(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpTransaction t =  broker.newTransaction();
      updateSystemObjectsName(session, broker);
      updateSubjectsSourceFlag(broker);
      t.commit();
      broker.close();
   }

   /**
    * Updates the display name and description of the administrator and everyone group.
    * @param session a <code>OpProjectSession</code> the server session.
    * @param broker a <code>OpBroker</code> used for persistence operations.
    */
   private void updateSystemObjectsName(OpProjectSession session, OpBroker broker) {
      OpUser admin = session.administrator(broker);
      admin.setDisplayName(OpUser.ADMINISTRATOR_DISPLAY_NAME);
      admin.setDescription(OpUser.ADMINISTRATOR_DESCRIPTION);
      OpGroup every = session.everyone(broker);
      every.setDisplayName(OpGroup.EVERYONE_DISPLAY_NAME);
      every.setDescription(OpGroup.EVERYONE_DESCRIPTION);
      broker.updateObject(admin);
      broker.updateObject(every);
   }

   /**
    * Updates the creator flag for all the subjects in the system.
    * @param broker a <code>OpBroker</code> used for persistence operations.
    */
   private void updateSubjectsSourceFlag(OpBroker broker) {
      OpQuery allSubjectsQuery = broker.newQuery("from OpSubject");
      Iterator<OpSubject> subjectsIterator = broker.list(allSubjectsQuery).iterator();
      while (subjectsIterator.hasNext()) {
         OpSubject subject = subjectsIterator.next();
         subject.setSource(OpSubject.INTERNAL);
         broker.updateObject(subject);
      }
   }

   /**
    * Upgrades this module to version #3 (via reflection - must be public).
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion3(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("select user from OpUser as user");
      Iterator result = broker.iterate(query);
      OpTransaction transaction  = broker.newTransaction();
      while (result.hasNext()) {
         OpUser user = (OpUser) result.next();
         user.setLevel(OpUser.MANAGER_USER_LEVEL);
         broker.updateObject(user);
      }
      transaction.commit();
      broker.close();
   }
}
