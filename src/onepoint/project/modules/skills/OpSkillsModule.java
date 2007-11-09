/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.skills;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.service.server.XServiceManager;

public class OpSkillsModule extends OpModule {

   /**
    * @see onepoint.project.module.OpModule#start(onepoint.project.OpProjectSession)
    */
   @Override
   public void start(OpProjectSession session) {
      //register system objects with backup manager
      // TODO(dfreis Oct 8, 2007 5:54:08 AM) add to backup manager
      //OpBackupManager.addSystemObjectIDQuery(OpSkillCategory.ROOT_SKILL_CATEGORY_NAME, OpSkillCategory.ROOT_SKILL_CATEGORY_ID_QUERY);

      // Check if hard-wired pool object "Root Skill Category" exists (if not create it)
   }
}
