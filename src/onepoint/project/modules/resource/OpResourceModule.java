/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.user.OpPermission;

public class OpResourceModule extends OpModule {

   public final static byte POOL_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER + OpPermission.OBSERVER;
   public final static byte RESOURCE_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER
        + OpPermission.OBSERVER;

   private final static String OLD_ROOT_RESOURCE_POOL_NAME = "{$RootResourcePoolName}";

   /**
    * @see onepoint.project.module.OpModule#start(onepoint.project.OpProjectSession)
    */
   public void start(OpProjectSession session) {
      //register system objects with backup manager  (for backup backward compatibility, add the old names as well)
      OpBackupManager.addSystemObjectIDQuery(OLD_ROOT_RESOURCE_POOL_NAME, OpResourcePool.ROOT_RESOURCE_POOL_ID_QUERY);
      OpBackupManager.addSystemObjectIDQuery(OpResourcePool.ROOT_RESOURCE_POOL_NAME, OpResourcePool.ROOT_RESOURCE_POOL_ID_QUERY);

      // Check if hard-wired pool object "Root Resource Pool" exists (if not create it)
      OpBroker broker = session.newBroker();
      if (OpResourceService.findRootPool(broker) == null && !updateRootPoolName(broker)) {
         OpResourceService.createRootPool(session, broker);
      }
      broker.close();
   }

   /**
    * @see onepoint.project.module.OpModule#upgrade(onepoint.project.OpProjectSession,int)
    */
   public void upgrade(OpProjectSession session, int dbVersion) {
      OpBroker broker = session.newBroker();
      updateRootPoolName(broker);
      broker.close();
   }

   /**
    * Changes the name of the root resource pool from the old resource naming - starting with {$
    * to the new naming with ${ - only if the old naming exists.
    *
    * @param broker a <code>OpBroker</code> used for persistence operations.
    * @return a <code>true</code> if the update was successfully done.
    */
   private boolean updateRootPoolName(OpBroker broker) {
      OpResourcePool oldPool = OpResourceService.findPool(broker, OLD_ROOT_RESOURCE_POOL_NAME);
      // check if old root pool exists and rename it
      if (oldPool != null) {
         oldPool.setName(OpResourcePool.ROOT_RESOURCE_POOL_NAME);
         oldPool.setDescription(OpResourcePool.ROOT_RESOURCE_POOL_DESCRIPTION);
         OpTransaction t = broker.newTransaction();
         broker.updateObject(oldPool);
         t.commit();
         return true;
      }
      return false;
   }
}
