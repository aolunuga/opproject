/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpUser;

import java.util.Iterator;

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
   @Override
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
    * Upgrades this module to version #5 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion5(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      updateRootPortfolioName(broker);
      broker.close();
   }

   /**
    * Upgrades this module to version #11 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion11(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpQuery allProjectsQuery = broker.newQuery("from OpProjectNode projectNode where projectNode.Type = :type");
      allProjectsQuery.setParameter("type", OpProjectNode.PROJECT);
      OpTransaction tx = broker.newTransaction();
      Iterator<OpProjectNode> projectsIt = broker.iterate(allProjectsQuery);
      while (projectsIt.hasNext()) {
         OpProjectNode project = projectsIt.next();
         project.setArchived(OpProjectNode.DEFAULT_ARCHIVED);
         project.setPriority(OpProjectNode.DEFAULT_PRIORITY);
         project.setProbability(OpProjectNode.DEFAULT_PROBABILITY);
         broker.updateObject(project);
      }
      tx.commit();
      broker.close();
   }

   /**
    * Upgrades this module to version #21 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion21(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("from OpProjectPlanVersion planVersion");
      Iterator it = broker.iterate(query);
      OpTransaction transaction = broker.newTransaction();
      while (it.hasNext()) {
         OpProjectPlanVersion planVersion = (OpProjectPlanVersion) it.next();
         if (planVersion.isBaseline() == null) {
            planVersion.setBaseline(Boolean.FALSE);
            broker.updateObject(planVersion);
         }
      }
      transaction.commit();
      broker.close();
   }


   public void upgradeToVersion24(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpQuery allProjectsQuery = broker.newQuery("from OpProjectNode projectNode where projectNode.Type = :type");
      allProjectsQuery.setParameter("type", OpProjectNode.PROJECT);
      OpTransaction tx = broker.newTransaction();
      Iterator<OpProjectNode> projectsIt = broker.iterate(allProjectsQuery);
      while (projectsIt.hasNext()) {
         OpProjectNode project = projectsIt.next();
         for (OpAssignment assignment : project.getPlan().getActivityAssignments()) {
            OpActivityDataSetFactory.updateWorkMonths(broker, assignment, session.getCalendar());
         }
      }
      tx.commit();
      broker.close();
   }

   /**
    * Upgrades this module to version #25 (via reflection - must be public).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion25(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpUser administrator = session.administrator(broker);
      OpQuery allPlanVersions = broker.newQuery("from OpProjectPlanVersion projectVersion");
      OpTransaction tx = broker.newTransaction();
      Iterator<OpProjectPlanVersion> projectsIt = broker.iterate(allPlanVersions);
      while (projectsIt.hasNext()) {
         OpProjectPlanVersion planVersion = projectsIt.next();
         OpUser user = (OpUser) broker.getObject(OpUser.class, new Long(planVersion.getCreator()));
         String displayName = (user != null) ? user.getDisplayName() : administrator.getDisplayName();
         planVersion.setCreator(displayName);
         broker.updateObject(planVersion);
      }
      tx.commit();
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
