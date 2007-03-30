/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.user.OpPermission;

public class OpResourceModule extends OpModule {

   public final static byte POOL_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER + OpPermission.OBSERVER;
   public final static byte RESOURCE_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER
        + OpPermission.OBSERVER;

   public void start(OpProjectSession session) {
      // Check if hard-wired pool object "Root Resource Pool" exists (if not create it)
      OpBroker broker = session.newBroker();
      if (OpResourceService.findRootPool(broker) == null) {
         OpResourceService.createRootPool(session, broker);
      }
      //register system objects with backup manager
      OpBackupManager.addSystemObjectIDQuery(OpResourcePool.ROOT_RESOURCE_POOL_NAME, OpResourcePool.ROOT_RESOURCE_POOL_ID_QUERY);
      
      broker.close();
   }

}
