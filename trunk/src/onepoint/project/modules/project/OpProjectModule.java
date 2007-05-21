/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.user.OpPermission;

public class OpProjectModule extends OpModule {

   public final static byte PORTFOLIO_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER + OpPermission.OBSERVER;
   public final static byte PROJECT_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER + OpPermission.CONTRIBUTOR + OpPermission.OBSERVER;
   public final static byte TEMPLATE_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER + OpPermission.OBSERVER;

   /**
    * The name of project module.
    */
   public static final String MODULE_NAME = "project";

   private final static String OLD_ROOT_PORTFOLIO_NAME = "{$RootProjectPortfolioName}";

   /**
    * Returns the name of the start form for this module.
    *
    * @return a <code>String</code> representing the path to the start form.
    */
   public String getStartFormPath() {
      return "/modules/project/forms/projects.oxf.xml";
   }

   /**
    * @see onepoint.project.module.OpModule#start(onepoint.project.OpProjectSession)
    */
   public void start(OpProjectSession session) {

      //Register system objects with backup manager (for backup backward compatibility, add the old names as well)
      OpBackupManager.addSystemObjectIDQuery(OLD_ROOT_PORTFOLIO_NAME, OpProjectNode.ROOT_PROJECT_PORTFOLIO_ID_QUERY);
      OpBackupManager.addSystemObjectIDQuery(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME, OpProjectNode.ROOT_PROJECT_PORTFOLIO_ID_QUERY);

      // Check if hard-wired portfolio object "Root Project Portfolio" exists (if not create it)
      OpBroker broker = session.newBroker();
      if (OpProjectAdministrationService.findRootPortfolio(broker) == null && !updateRootPortfolioName(broker)) {
         OpProjectAdministrationService.createRootPortfolio(session, broker);
      }
      broker.close();
   }

   /**
    * @see onepoint.project.module.OpModule#upgrade(onepoint.project.OpProjectSession, int)  
    */
   public void upgrade(OpProjectSession session, int dbVersion) {
      OpBroker broker = session.newBroker();
      updateRootPortfolioName(broker);
      broker.close();
   }

   /**
    * Changes the name of the root project portfolio from the old resource naming - starting with {$
    * to the new naming with ${ - only if the old naming exists.
    *
    * @param broker a <code>OpBroker</code> used for persistence operations.
    * @return a <code>true</code> if the update was successfully done.
    */
   private boolean updateRootPortfolioName(OpBroker broker) {
      OpProjectNode oldRoot = OpProjectAdministrationService.findProjectNode(broker, OLD_ROOT_PORTFOLIO_NAME, OpProjectNode.PORTFOLIO);
      if (oldRoot != null) {
         oldRoot.setName(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME);
         oldRoot.setDescription(OpProjectNode.ROOT_PROJECT_PORTFOLIO_DESCRIPTION);
         OpTransaction t = broker.newTransaction();
         broker.updateObject(oldRoot);
         t.commit();
         return true;
      }
      return false;
   }
}
