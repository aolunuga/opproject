/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 *
 */

package onepoint.project.modules.project_planning;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleChecker;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanValidator;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author mihai.costin
 */
public class OpProjectPlanningModuleChecker implements OpModuleChecker {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpProjectPlanningModuleChecker.class);
   private String ALL_PROJECT_PLANS = "select projectPlan from OpProjectNode project inner join project.Plan projectPlan where project.Type = ?";


   public void check(OpProjectSession session) {
      logger.info("Checking module ProjectPlanning");
      fixProjectPlans(session);
      logger.info("Done");
   }


   /**
    * Upgrades this module to version #5 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void fixProjectPlans(OpProjectSession session) {
      //delete any work records that might be associated with deleted activities
      List<Long> projectPlanIds = new ArrayList<Long>();
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery(ALL_PROJECT_PLANS);
      query.setByte(0, OpProjectNode.PROJECT);
      Iterator iterator = broker.iterate(query);
      while (iterator.hasNext()) {
         OpProjectPlan projectPlan = (OpProjectPlan) iterator.next();
         //due to a previous bug, we must make sure the start/end of the project plans are ok - 1st thing that most be done !
         fixProjectPlanDates(projectPlan, broker);
         projectPlanIds.add(projectPlan.getID());
      }
      broker.close();
      revalidateProjectPlans(session, projectPlanIds);
   }

   /**
    * Performs a validation on the given list of project plan ids.
    *
    * @param session        a <code>OpProjectSession</code> representing a server session.
    * @param projectPlanIds a <code>List(Long)</code> representing a list of project plan ids.
    */
   private void revalidateProjectPlans(OpProjectSession session, List<Long> projectPlanIds) {
      logger.info("Revalidating project plans...");
      //validate all the project plans (this includes also the work phase -> work period upgrade)
      for (Long projectPlanId : projectPlanIds) {
         OpBroker broker = session.newBroker();
         OpProjectPlan projectPlan = (OpProjectPlan) broker.getObject(OpProjectPlan.class, projectPlanId);
         new OpProjectPlanValidator(projectPlan).validateProjectPlan(broker, null);
         broker.close();
      }
   }

   /**
    * Fixes the problem with invalid project plan dates (weren't correct).
    *
    * @param projectPlan a <code>OpProjectPlan</code> representing a project plan.
    * @param broker      a <code>OpBroker</code> used for db operations.
    */
   private void fixProjectPlanDates(OpProjectPlan projectPlan, OpBroker broker) {
      OpTransaction tx = broker.newTransaction();
      projectPlan.copyDatesFromProject();
      Date projectPlanFinish = projectPlan.getFinish();
      //there can't be activities before the project start, so we only need to update the end date
      for (OpActivity activity : projectPlan.getActivities()) {
         if (activity.getFinish() != null && activity.getFinish().after(projectPlanFinish)) {
            projectPlanFinish = activity.getFinish();
         }
      }
      projectPlan.setFinish(projectPlanFinish);
      broker.updateObject(projectPlan);
      tx.commit();
   }

}
