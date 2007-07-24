/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
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

   public void install(OpProjectSession session) {
      // *** Create prototypes (check for upgrade later on)
      // *** Add tools

      // *** Although advantages if specified in module.oxr
      // ==> Dependencies could be checked, pre-configured delivery of application is easier

      // *** Best: We leave the choice to the programmer
      // ==> Prototype and tool handling can be configured in module.oxm; implemented by base class OpModule

      // *** The only issue: We have to provide an intelligent "upgrade"-check (existing data)
      // ==> OpModule should provide a call-back upgrade()
   }

   public void remove(OpProjectSession session) {
      // *** Drop prototypes (what about potential data-loss)?
      // *** remove tools
   }

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
    * Upgrades a module from a previous version to a new version.
    *
    * @param session   a <code>OpProjectSession</code> representing a server session.
    * @param dbVersion The current version of the DB.
    */
   public void upgrade(OpProjectSession session, int dbVersion) {
      if (dbVersion < 3) {
         OpBroker broker = session.newBroker();
         OpQuery query = broker.newQuery("select user from OpUser as user");
         Iterator result = broker.iterate(query);
         OpTransaction transaction  = broker.newTransaction();
         while (result.hasNext()) {
            OpUser user = (OpUser) result.next();
            user.setLevel(new Byte(OpUser.MANAGER_USER_LEVEL));
            broker.updateObject(user);
         }
         transaction.commit();
         broker.close();
      }
   }

}
