/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning;

import onepoint.express.server.XFormLoader;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.modules.project.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * <FIXME author="Horia Chiorean" description="Be so nice as to add comments to classes [-(">
 * @author : mihai.costin
 */
public class OpProjectPlanningModule extends OpModule {

   /**
    * This class's logger.
    */
   private XLog logger = XLogFactory.getServerLogger(OpProjectPlanningModule.class);


   /**
    * @see onepoint.project.module.OpModule#start(onepoint.project.OpProjectSession)
    */
   public void start(OpProjectSession session) {
      // Register project components
      OpProjectComponentHandler project_handler = new OpProjectComponentHandler();
      XFormLoader.registerComponent(OpProjectComponentHandler.GANTT_BOX, project_handler);
      XFormLoader.registerComponent(OpProjectComponentHandler.GANTT_MAP, project_handler);
      XFormLoader.registerComponent(OpProjectComponentHandler.UTILIZATION_BOX, project_handler);
   }

   /**
    * @see onepoint.project.module.OpModule#upgrade(onepoint.project.OpProjectSession, int)
    */
   public void upgrade(OpProjectSession session, int dbVersion) {

      //update the project plan with scheduled task activities [ scheduled tasks added in version 2 ]
      if (dbVersion < 3) {
         logger.info("Upgrading for version < 3...");
         OpBroker broker = session.newBroker();
         OpQuery query = broker.newQuery("select project from OpProjectNode as project where project.Type = ?");
         query.setByte(0, OpProjectNode.PROJECT);

         Iterator result = broker.iterate(query);
         List projectPlans = new ArrayList();
         //validate the tasks & collection tasks.
         while (result.hasNext()) {
            OpProjectNode projectNode = (OpProjectNode) result.next();
            OpProjectPlan projectPlan = projectNode.getPlan();

            logger.info("Upgrade tasks and scheduled tasks for " + projectNode.getName());
            updateTasks(projectPlan.getID(), projectNode, session);
            updateActivitiesAndChildren(projectPlan.getID(), session);

            //same update for all the activity versions from all the project versions.
            Set allVersions = projectPlan.getVersions();
            for (Iterator planVersionsIterator = allVersions.iterator(); planVersionsIterator.hasNext();) {
               OpProjectPlanVersion planVersion = (OpProjectPlanVersion) planVersionsIterator.next();

               updateTasksVersions(planVersion.getID(), projectNode, session);
               updateActivityVersionsAndChildren(planVersion.getID(), session);
            }
            projectPlans.add(new Long(projectPlan.getID()));
         }
         broker.close();

         //recalculate the project plans
         revalidateProjectPlans(session, projectPlans);
      }
   }

   private void revalidateProjectPlans(OpProjectSession session, List projectPlanIds) {
      logger.info("Revalidating project plans...");
      OpBroker broker = session.newBroker();

      //validate all the project plans (this includes also the work phase -> work period upgrade)
      for (Iterator iterator = projectPlanIds.iterator(); iterator.hasNext();) {
         Long projectPlanId = (Long) iterator.next();
         OpProjectPlan projectPlan = (OpProjectPlan) broker.getObject(OpProjectPlan.class, projectPlanId.longValue());
         new OpProjectPlanValidator(projectPlan).validateProjectPlan(broker, null);
      }
      broker.close();
   }

   /**
    * Updates tasks of a given project plan, by updating the start and end date.
    * @param projectPlanId a <code>long</code> representing the id of a project plan .
    * @param projectNode a <code>OpProjectNode</code> representing the project to which the version belongs.
    * @param session a <code>OpProjectSession</code> representing the session on which the upgrade is done.
    */
   private void updateTasks(long projectPlanId, OpProjectNode projectNode, OpProjectSession session) {
      OpBroker broker = session.newBroker();
      String queryString = "select activity from OpProjectPlan projectPlan inner join projectPlan.Activities activity where projectPlan.ID=? and activity.Type=? and activity.Deleted=false";
      OpQuery query = broker.newQuery(queryString);
      query.setLong(0, projectPlanId);
      query.setByte(1, OpActivity.TASK);
      Iterator tasks = broker.list(query).iterator();
      OpTransaction t = broker.newTransaction();
      while (tasks.hasNext()) {
         OpActivity task = (OpActivity) tasks.next();
         OpActivity superActivity = task.getSuperActivity();
         if (superActivity == null) {
            task.setStart(projectNode.getStart());
            task.setFinish(projectNode.getFinish());
         }
         else {
            task.setStart(superActivity.getStart());
            task.setFinish(superActivity.getFinish());
         }
         broker.updateObject(task);
      }
      t.commit();
      broker.close();
   }

   /**
    * Updates tasks versions of a given project plan, by updating the start and end date.
    * @param projectPlanVersionId a <code>long</code> representing the id of a project plan version.
    * @param projectNode a <code>OpProjectNode</code> representing the project to which the version belongs.
    * @param session a <code>OpProjectSession</code> representing the session on which the upgrade is done.
    */
   private void updateTasksVersions(long projectPlanVersionId, OpProjectNode projectNode, OpProjectSession session) {
      OpBroker broker = session.newBroker();
      String queryString = "select activityVersion from OpProjectPlanVersion projectPlanVersion inner join projectPlanVersion.ActivityVersions activityVersion where projectPlanVersion.ID=? and activityVersion.Type=? and activityVersion.Activity.Deleted=false";
      OpQuery query = broker.newQuery(queryString);
      query.setLong(0, projectPlanVersionId);
      query.setByte(1, OpActivity.TASK);
      Iterator tasksVersions = broker.list(query).iterator();
      OpTransaction t = broker.newTransaction();
      while (tasksVersions.hasNext()) {
         OpActivityVersion taskVersion = (OpActivityVersion) tasksVersions.next();
         OpActivityVersion superActivityVersion = taskVersion.getSuperActivityVersion();
         if (superActivityVersion == null) {
            taskVersion.setStart(projectNode.getStart());
            taskVersion.setFinish(projectNode.getFinish());
         }
         else {
            taskVersion.setStart(superActivityVersion.getStart());
            taskVersion.setFinish(superActivityVersion.getFinish());
         }
         broker.updateObject(taskVersion);
      }
      t.commit();
      broker.close();
   }

   /**
    * For a given project plan, updates the tasks and scheduled tasks.
    * @param projectPlanId a <code>long</code> representing the id of a project plan.
    * @param session a <code>OpProjectSession</code> representing the session which started the upgrade procedure.
    */
   private void updateActivitiesAndChildren(long projectPlanId, OpProjectSession session) {
      OpBroker broker = session.newBroker();
      StringBuffer queryBuffer = new StringBuffer("select activity.ID, count(subActivity) from OpProjectPlan projectPlan ");
      queryBuffer.append(" inner join projectPlan.Activities activity inner join activity.SubActivities subActivity ");
      queryBuffer.append(" where projectPlan.ID=? and activity.Deleted=false and subActivity.Deleted=false and (subActivity.Type=? or subActivity.Type=?)");
      queryBuffer.append(" group by activity.ID");
      queryBuffer.append(" having count(subActivity) > 0");
      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setLong(0, projectPlanId);
      query.setByte(1, OpActivity.TASK);
      query.setByte(2, OpActivity.COLLECTION_TASK);
      Iterator it = broker.list(query).iterator();
      OpTransaction t = broker.newTransaction();
      while (it.hasNext()) {
         Object[] result = (Object[]) it.next();


         Long activityId = (Long) result[0];
         OpActivity activity = (OpActivity) broker.getObject(OpActivity.class, activityId.longValue());
         int activityType = activity.getType();
         int totalChildCount = activity.getSubActivities().size();
         int subTasksCount = ((Number) result[1]).intValue();

         if (totalChildCount == subTasksCount && activityType != OpActivity.COLLECTION_TASK && activityType != OpActivity.SCHEDULED_TASK) {
            //<FIXME author="Horia Chiorean" description="Shouldn't we remove assignments and work-records ?">
            activity.setType(OpActivity.SCHEDULED_TASK);
            broker.updateObject(activity);
            //<FIXME>
         }
         else {
            queryBuffer = new StringBuffer("select subActivity ");
            queryBuffer.append(" from OpActivity activity inner join activity.SubActivities subActivity ");
            queryBuffer.append(" where activity.ID=? and subActivity.Deleted=false and (subActivity.Type=? or subActivity.Type=?) ");
            query = broker.newQuery(queryBuffer.toString());
            query.setLong(0, activity.getID());
            query.setByte(1, OpActivity.TASK);
            query.setByte(2, OpActivity.COLLECTION_TASK);
            Iterator subTasksIterator = broker.list(query).iterator();
            while (subTasksIterator.hasNext()) {
               OpActivity subTask = (OpActivity) subTasksIterator.next();
               subTask.setStart(activity.getStart());
               subTask.setFinish(activity.getFinish());
               subTask.setDuration(activity.getDuration());
               if (subTask.getSubActivities().size() == 0) {
                  subTask.setType(OpActivityVersion.STANDARD);
               }
               else {
                  subTask.setType(OpActivityVersion.COLLECTION);
               }
               broker.updateObject(subTask);
            }
         }
      }
      t.commit();
      broker.close();
   }

   /**
    * For a given project plan, updates the tasks and scheduled tasks versions.
    * @param projectPlanVersionId a <code>long</code> representing the id of a project plan version.
    * @param session a <code>OpProjectSession</code> representing the session which started the upgrade procedure.
    */
   private void updateActivityVersionsAndChildren(long projectPlanVersionId, OpProjectSession session) {
      OpBroker broker = session.newBroker();
      StringBuffer queryBuffer = new StringBuffer("select activityVersion.ID, count(subActivityVersion) from OpProjectPlanVersion projectPlanVersion ");
      queryBuffer.append(" inner join projectPlanVersion.ActivityVersions activityVersion inner join activityVersion.SubActivityVersions subActivityVersion ");
      queryBuffer.append(" where projectPlanVersion.ID=? and activityVersion.Activity.Deleted=false and subActivityVersion.Activity.Deleted=false and (subActivityVersion.Type=? or subActivityVersion.Type=?)");
      queryBuffer.append(" group by activityVersion.ID");
      queryBuffer.append(" having count(subActivityVersion) > 0");
      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setLong(0, projectPlanVersionId);
      query.setByte(1, OpActivity.TASK);
      query.setByte(2, OpActivity.COLLECTION_TASK);
      Iterator it = broker.list(query).iterator();
      OpTransaction t = broker.newTransaction();
      while (it.hasNext()) {
         Object[] result = (Object[]) it.next();

         Long activityVersionId = (Long) result[0];
         OpActivityVersion activityVersion = (OpActivityVersion) broker.getObject(OpActivityVersion.class, activityVersionId.longValue());
         int activityVersionType = activityVersion.getType();
         int totalChildCount = activityVersion.getSubActivityVersions().size();
         int subTaskVersionsCount = ((Number) result[1]).intValue();

         if (totalChildCount == subTaskVersionsCount && activityVersionType != OpActivity.COLLECTION_TASK && activityVersionType != OpActivity.SCHEDULED_TASK) {
            //<FIXME author="Horia Chiorean" description="Shouldn't we also remove assignments and work-records ?">
            activityVersion.setType(OpActivity.SCHEDULED_TASK);
            broker.updateObject(activityVersion);
            //<FIXME>
         }
         else {
            queryBuffer = new StringBuffer("select subActivityVersion ");
            queryBuffer.append(" from OpActivityVersion activityVersion inner join activityVersion.SubActivityVersions subActivityVersion ");
            queryBuffer.append(" where activityVersion.ID=? and subActivityVersion.Activity.Deleted=false and (subActivityVersion.Type=? or subActivityVersion.Type=?) ");
            query = broker.newQuery(queryBuffer.toString());
            query.setLong(0, activityVersion.getID());
            query.setByte(1, OpActivity.TASK);
            query.setByte(2, OpActivity.COLLECTION_TASK);
            Iterator subTaskVersionsIterator = broker.list(query).iterator();
            while (subTaskVersionsIterator.hasNext()) {
               OpActivityVersion subTaskVersion = (OpActivityVersion) subTaskVersionsIterator.next();
               subTaskVersion.setStart(activityVersion.getStart());
               subTaskVersion.setFinish(activityVersion.getFinish());
               subTaskVersion.setDuration(activityVersion.getDuration());
               if (subTaskVersion.getSubActivityVersions().size() == 0) {
                  subTaskVersion.setType(OpActivityVersion.STANDARD);
               }
               else {
                  subTaskVersion.setType(OpActivityVersion.COLLECTION);
               }
               broker.updateObject(subTaskVersion);
            }
         }
      }
      t.commit();
      broker.close();
   }
}
