/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 *
 */

package onepoint.project.modules.project_planning;

import java.sql.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityIfc;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAssignmentIfc;
import onepoint.project.modules.project.OpAssignmentVersion;
import onepoint.project.modules.project.OpProjectModuleChecker;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanValidator;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkRecord;

/**
 * Checker class associated to the project planning module.
 *
 * @author mihai.costin
 * @author horia.chiorean
 */
public class OpProjectPlanningModuleChecker extends OpProjectModuleChecker {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpProjectPlanningModuleChecker.class);


   @Override
   public void check(OpProjectSession session) {
      logger.info("Checking module Project Planning...");
      List<Long> projectsList = super.getProjectsOfType(session, OpProjectNode.PROJECT);
      int pos = 0;
      for (Iterator it = projectsList.iterator(); it.hasNext(); ) {
         logger.info("Checking module Project Planning "+pos+"/"+projectsList.size());
         Long  projectId = (Long) it.next();
         fixProjectPlans(session, projectId);
         deleteWorkRecordsForDeletedActivities(session, projectId);
         deleteWorkRecordsForCollections(session, projectId);
         resetActivityValues(session, projectId);
         recalculateAssignmentsValues(session, projectId);
         revalidateProjectPlan(session, projectId);
         pos++;
      }
   }

   private void recalculateAssignmentsValues(OpProjectSession session, long projectId) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction transaction = broker.newTransaction();

         //<FIXME author="Mihai Costin" description="See Opp-404">
         if (((OpHibernateSource) broker.getConnection().getSource()).getDatabaseType() == OpHibernateSource.DERBY) {
            OpQuery query = broker.newQuery("select activity.id from OpActivity activity where activity.Assignments.size > 0 and activity.ProjectPlan.ProjectNode.id=?");
            query.setLong(0, projectId);
            Iterator iterator = broker.list(query).iterator();
            while (iterator.hasNext()) {
               Long id = (Long) iterator.next();
               OpActivity activity = (OpActivity) broker.getObject(OpActivity.class, id);

               //calculate the activity assignments base effort sum
               double effort = 0;
               for (OpAssignmentIfc assignment : activity.getAssignments()) {
                  effort += assignment.getBaseEffort();
               }

               //distribute effort
               if (effort > activity.getBaseEffort()) {
                  distributeAssignmentEffort(broker, activity);
               }
            }
         }
         //</FIXME>
         else {
            OpQuery query = broker.newQuery("select activity.id, activity.BaseEffort from OpActivity activity inner join activity.Assignments assignment inner join activity.ProjectPlan projectPlan inner join projectPlan.ProjectNode project group by activity.id, activity.BaseEffort having sum(assignment.BaseEffort)>activity.BaseEffort and max(project.id)=?");
            query.setLong(0, projectId);
            Iterator iterator = broker.list(query).iterator();
            while (iterator.hasNext()) {
               Object[] activityInfo = (Object[]) iterator.next();
               Long id = (Long) activityInfo[0];
               OpActivity activity = (OpActivity) broker.getObject(OpActivity.class, id);
               distributeAssignmentEffort(broker, activity);
            }
         }

         transaction.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }

   private void distributeAssignmentEffort(OpBroker broker, OpActivity activity) {
      double assignmentSum = 0;
      for (OpAssignmentIfc assignment : activity.getAssignments()) {
         double effort = assignment.getBaseEffort();
         assignmentSum += effort;
      }
      double diff = assignmentSum - activity.getBaseEffort();
      logger.info("Found an activity with faulty assignments: " + activity.getName() + " from project " +
           activity.getProjectPlan().getProjectNode().getName() + ". Effort differ by " + diff);

      //distribute the diff by modifing the %assigned
      double ratio = activity.getBaseEffort() / assignmentSum;
      for (OpAssignment assignment : activity.getAssignments()) {
         assignment.setAssigned(assignment.getAssigned() * ratio);
         assignment.setBaseEffort(assignment.getBaseEffort() * ratio);
         broker.updateObject(assignment);
      }
   }

   /**
    * Resets the "planning" values of the activities (actual values are checked by work checker).
    *
    * @param session a <code>OpProjectSession</code> a server session
    * @param projectId a <code>long</code> the id of a project.
    */
   private void resetActivityValues(OpProjectSession session, long projectId) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction transaction = broker.newTransaction();
         OpQuery query = broker.newQuery("select activity from OpActivity activity where activity.Assignments is empty and activity.ProjectPlan.ProjectNode.id=?");
         query.setLong(0, projectId);
         long start = System.currentTimeMillis();
         Iterator iterator = broker.iterate(query);//list(query).iterator();
         logger.info("deleteWorkRecordsForCollections query lasted: "+(System.currentTimeMillis()-start));
         while (iterator.hasNext()) {
            OpActivity activity = (OpActivity) iterator.next();
            if (activity.getProjectPlan().getProgressTracked()) {
               activity.setComplete(0);
               activity.setRemainingEffort(activity.getBaseEffort());
               activity.setActualEffort(0);
            }
            else {
               activity.setActualEffort(0);
               if (activity.getComplete() < 0) {
                  activity.setComplete(0);
               }
               if (activity.getComplete() > 100) {
                  activity.setComplete(100);
               }
               //update remaining based on complete for non tracked
               double remaining = OpGanttValidator.calculateRemainingEffort(activity.getBaseEffort(), 0, activity.getComplete());
               activity.setRemainingEffort(remaining);
            }
            broker.updateObject(activity);
         }
         transaction.commit();
      }
      finally {
         broker.closeAndEvict();
      }

      broker = session.newBroker();
      try {
         OpTransaction transaction = broker.newTransaction();
         OpQuery query = broker.newQuery("select activity from OpActivity activity where activity.Assignments.size > 0 and activity.ProjectPlan.ProjectNode.id=?");
         query.setLong(0, projectId);
         Iterator iterator = broker.list(query).iterator();
         while (iterator.hasNext()) {
            OpActivity activity = (OpActivity) iterator.next();
            if (activity.getComplete() < 0) {
               activity.setComplete(0);
            }
            if (activity.getComplete() > 100) {
               activity.setComplete(100);
            }
            broker.updateObject(activity);
         }
         transaction.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }

   private void deleteWorkRecordsForCollections(OpProjectSession session, long projectId) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();
         OpQuery query = broker.newQuery("select distinct activity.SuperActivity from OpActivity activity where activity.SuperActivity.ProjectPlan.ProjectNode.id=?");
         query.setLong(0, projectId);
         Iterator iterator = broker.iterate(query);//list(query).iterator();
         while (iterator.hasNext()) {
            OpActivity activity = (OpActivity) iterator.next();
            for (OpAssignment assignment : activity.getAssignments()) {
               Set<OpAssignmentVersion> assignments = assignment.getAssignmentVersions();
               if (assignments != null) {
                  for (OpAssignmentVersion version : assignments) {
                     broker.deleteObject(version);
                  }
               }
               for (OpWorkRecord workRecord : assignment.getWorkRecords()) {
                  broker.deleteObject(workRecord);
               }
               broker.deleteObject(assignment);
            }
         }
         t.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }

   /**
    * Deletes all the workrecords that are linked to deleted activities.
    *
    * @param session Project session
    * @param projectId  a <code>long</code> the id of a project.
    */
   private void deleteWorkRecordsForDeletedActivities(OpProjectSession session, long projectId) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();
         OpQuery query = broker.newQuery("select activity from OpActivity activity where activity.Deleted = true and activity.ProjectPlan.ProjectNode.id=?");
         query.setLong(0, projectId);
         Iterator iterator = broker.list(query).iterator();
         while (iterator.hasNext()) {
            OpActivity activity = (OpActivity) iterator.next();
            for (OpAssignment assignment : activity.getAssignments()) {
               for (OpWorkRecord workRecord : assignment.getWorkRecords()) {
                  broker.deleteObject(workRecord);
               }
               broker.deleteObject(assignment);
            }
         }
         t.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }

   /**
    * Fixes the start and end dates for the project plan of a given project
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    * @param projectId a <code>long</code> the id of a project.
    */
   private void fixProjectPlans(OpProjectSession session, long projectId) {
      //delete any work records that might be associated with deleted activities
      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project = broker.getObject(OpProjectNode.class, projectId);
         fixProjectPlanDates(project.getPlan(), broker);
      }
      finally {
         broker.closeAndEvict();
      }
   }


   /**
    * Revalidates the plan of a given project
    * @param session a <code>OpProjectSesssion</code> the server session.
    * @param projectId a <code>long</code> the id of a project.
    */
   private void revalidateProjectPlan(OpProjectSession session, long projectId) {
      OpBroker broker = session.newBroker();
      OpProjectNode project = broker.getObject(OpProjectNode.class, projectId);
      OpProjectPlan projectPlan = project.getPlan();
      try {
         OpProjectPlanValidator.getInstance().validateProjectPlan(session, broker, projectPlan, null, OpUser.SYSTEM_USER_NAME);
      }
      catch (RuntimeException e) {
         logger.error("Could not upgrade project: " + project.getName(), e);
      }
      broker.closeAndEvict();
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
      for (OpActivityIfc activity : projectPlan.getActivities()) {
         if (activity.getFinish() != null && activity.getFinish().after(projectPlanFinish)) {
            projectPlanFinish = activity.getFinish();
         }
      }
      projectPlan.setFinish(projectPlanFinish);
      broker.updateObject(projectPlan);
      tx.commit();
   }
}
