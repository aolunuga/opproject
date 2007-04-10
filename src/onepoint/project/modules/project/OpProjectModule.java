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

   public void start(OpProjectSession session) {

      //Register system objects with backup manager
      OpBackupManager.addSystemObjectIDQuery(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME, OpProjectNode.ROOT_PROJECT_PORTFOLIO_ID_QUERY);

      // Check if hard-wired portfolio object "Root Project Portfolio" exists (if not create it)
      OpBroker broker = session.newBroker();
      if (OpProjectAdministrationService.findRootPortfolio(broker) == null) {
         // check for old format
         OpProjectNode oldRoot = OpProjectAdministrationService.findProjectNode(broker, OLD_ROOT_PORTFOLIO_NAME, OpProjectNode.PORTFOLIO);
         if (oldRoot != null) {
            oldRoot.setName(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME);
            oldRoot.setDescription(OpProjectNode.ROOT_PROJECT_PORTFOLIO_DESCRIPTION);
            OpTransaction t = broker.newTransaction();
            broker.updateObject(oldRoot);
            t.commit();
         }
         else {
            OpProjectAdministrationService.createRootPortfolio(session, broker);
         }
      }

      // Patch for BETA-4: Check if all projects have associated project plans (if not: Create them)
      OpQuery query = broker.newQuery("select project, plan.ID from OpProjectNode as project left join project.Plan as plan where project.Type = ?");
      query.setByte(0, OpProjectNode.PROJECT);
      Iterator result = broker.iterate(query);
      Object[] record;
      OpProjectNode project;
      OpProjectPlan projectPlan;
      OpTransaction t = broker.newTransaction();
      while (result.hasNext()) {
         record = (Object[]) result.next();
         if (record[1] == null) {
            // Insert new project plan with default calculation and progress tracking settings
            project = (OpProjectNode) record[0];
            projectPlan = new OpProjectPlan();
            projectPlan.setStart(project.getStart());
            if (project.getFinish() != null) {
               projectPlan.setFinish(project.getFinish());
            }
            else {
               projectPlan.setFinish(projectPlan.getStart());
            }
            projectPlan.setProjectNode(project);
            projectPlan.setCalculationMode(OpProjectPlan.EFFORT_BASED);
            projectPlan.setProgressTracked(true);
            broker.makePersistent(projectPlan);
         }
      }
      t.commit();

      broker.close();
   }


}
