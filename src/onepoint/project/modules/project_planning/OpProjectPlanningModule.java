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
import onepoint.project.module.OpModuleChecker;
import onepoint.project.modules.project.*;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.work.OpWorkRecord;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Project planning module class.
 *
 * @author : mihai.costin
 */
public class OpProjectPlanningModule extends OpModule {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpProjectPlanningModule.class);


   /**
    * @see onepoint.project.module.OpModule#start(onepoint.project.OpProjectSession)
    */
   @Override
   public void start(OpProjectSession session) {
      // Register project components
      OpProjectComponentHandler project_handler = new OpProjectComponentHandler();
      XFormLoader.registerComponent(OpProjectComponentHandler.GANTT_BOX, project_handler);
      XFormLoader.registerComponent(OpProjectComponentHandler.GANTT_MAP, project_handler);
      XFormLoader.registerComponent(OpProjectComponentHandler.UTILIZATION_BOX, project_handler);
   }


   /**
    * Upgrades this module to version #3 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion3(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("select project from OpProjectNode as project where project.Type = ?");
      query.setByte(0, OpProjectNode.PROJECT);

      Iterator result = broker.iterate(query);
      List<Long> projectPlans = new ArrayList<Long>();
      //validate the tasks & collection tasks.
      while (result.hasNext()) {
         OpProjectNode projectNode = (OpProjectNode) result.next();
         OpProjectPlan projectPlan = projectNode.getPlan();

         logger.info("Upgrade tasks and scheduled tasks for " + projectNode.getName());
         updateTasks(projectPlan.getID(), projectNode, session);
         updateActivitiesAndChildren(projectPlan.getID(), session);

         //same update for all the activity versions from all the project versions.
         Set allVersions = projectPlan.getVersions();
         for (Object allVersion : allVersions) {
            OpProjectPlanVersion planVersion = (OpProjectPlanVersion) allVersion;

            updateTasksVersions(planVersion.getID(), projectNode, session);
            updateActivityVersionsAndChildren(planVersion.getID(), session);
         }
         projectPlans.add(projectPlan.getID());
      }
      broker.close();

      //recalculate the project plans
      revalidateProjectPlans(session, projectPlans);
   }

   /**
    * Upgrades this module to version #5 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion5(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("select projectPlan from OpProjectNode project inner join project.Plan projectPlan where project.Type = ?");
      query.setByte(0, OpProjectNode.PROJECT);
      Iterator iterator = broker.iterate(query);
      while (iterator.hasNext()) {
         OpProjectPlan projectPlan = (OpProjectPlan) iterator.next();
         updateProceeds(projectPlan, broker);
      }
      broker.close();
   }

   /**
    * Upgrades this module to version #26 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion26(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("from OpProjectPlan");
      Iterator result = broker.iterate(query);

      query = broker.newQuery("select setting.Value from OpSetting as setting where setting.Name = '" + OpSettings.CALENDAR_HOLIDAYS_LOCATION + "'");
      Iterator calendarResult = broker.iterate(query);
      String holidayCalendarId = null;
      if (calendarResult.hasNext()) {
         holidayCalendarId = (String) calendarResult.next();
      }

      OpTransaction tx = broker.newTransaction();
      while (result.hasNext()) {
         OpProjectPlan projectPlan = (OpProjectPlan) result.next();
         projectPlan.setHolidayCalendar(holidayCalendarId);
         for (OpProjectPlanVersion planVersion : projectPlan.getVersions()) {
            planVersion.setHolidayCalendar(holidayCalendarId);
            broker.updateObject(planVersion);
         }
         logger.info("Upgrade holiday calendar id to: " + holidayCalendarId);
         broker.updateObject(projectPlan);
      }
      tx.commit();
      broker.close();
   }

   /**
    * Revalidate working version with the right calendar.
    *
    * @param session project session
    */
   public void upgradeToVersion29(OpProjectSession session) {
      logger.info("Revalidating working versions with calendar settings on");

      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("from OpProjectPlan");
      Iterator result = broker.iterate(query);

      while (result.hasNext()) {
         OpProjectPlan projectPlan = (OpProjectPlan) result.next();
         OpProjectPlanValidator validator = new OpProjectPlanValidator(projectPlan);
         validator.validateProjectPlanWorkingVersion(broker, null, true);
      }
      broker.close();
   }


   /**
    * Updates the actual proceeds of all activity, activity assignments and work records,
    * by using the same values as the already existent acutual personnel costs.
    * This update is caused by the fact that the external rate is the same as the internal one.
    *
    * @param projectPlan a <code>OpProjectPlan</code> object representing the project plan for
    *                    which to update.
    * @param broker      a <code>OpBroker</code> used for persistence operations.
    */
   private void updateProceeds(OpProjectPlan projectPlan, OpBroker broker) {
      OpTransaction tx = broker.newTransaction();
      for (OpActivity activity : projectPlan.getActivities()) {
         activity.setActualProceeds(activity.getActualPersonnelCosts());
         activity.setBaseProceeds(activity.getBasePersonnelCosts());
         broker.updateObject(activity);
         for (OpAssignment assignment : activity.getAssignments()) {
            assignment.setBaseProceeds(assignment.getBaseCosts());
            assignment.setActualProceeds(assignment.getActualCosts());
            broker.updateObject(assignment);
            for (OpWorkRecord workRecord : assignment.getWorkRecords()) {
               workRecord.setActualProceeds(workRecord.getPersonnelCosts());
               broker.updateObject(workRecord);
            }
         }
      }
      tx.commit();
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
    * Updates tasks of a given project plan, by updating the start and end date.
    *
    * @param projectPlanId a <code>long</code> representing the id of a project plan .
    * @param projectNode   a <code>OpProjectNode</code> representing the project to which the version belongs.
    * @param session       a <code>OpProjectSession</code> representing the session on which the upgrade is done.
    */
   private void updateTasks(long projectPlanId, OpProjectNode projectNode, OpProjectSession session) {
      OpBroker broker = session.newBroker();
      String queryString = "select activity from OpProjectPlan projectPlan inner join projectPlan.Activities activity where projectPlan.ID=? and activity.Type=? and activity.Deleted=false";
      OpQuery query = broker.newQuery(queryString);
      query.setLong(0, projectPlanId);
      query.setByte(1, OpActivity.TASK);
      Iterator tasks = broker.iterate(query);
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
    *
    * @param projectPlanVersionId a <code>long</code> representing the id of a project plan version.
    * @param projectNode          a <code>OpProjectNode</code> representing the project to which the version belongs.
    * @param session              a <code>OpProjectSession</code> representing the session on which the upgrade is done.
    */
   private void updateTasksVersions(long projectPlanVersionId, OpProjectNode projectNode, OpProjectSession session) {
      OpBroker broker = session.newBroker();
      String queryString = "select activityVersion from OpProjectPlanVersion projectPlanVersion inner join projectPlanVersion.ActivityVersions activityVersion where projectPlanVersion.ID=? and activityVersion.Type=? and activityVersion.Activity.Deleted=false";
      OpQuery query = broker.newQuery(queryString);
      query.setLong(0, projectPlanVersionId);
      query.setByte(1, OpActivity.TASK);
      Iterator tasksVersions = broker.iterate(query);
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
    *
    * @param projectPlanId a <code>long</code> representing the id of a project plan.
    * @param session       a <code>OpProjectSession</code> representing the session which started the upgrade procedure.
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
      Iterator it = broker.iterate(query);
      OpTransaction t = broker.newTransaction();
      while (it.hasNext()) {
         Object[] result = (Object[]) it.next();

         Long activityId = (Long) result[0];
         OpActivity activity = (OpActivity) broker.getObject(OpActivity.class, activityId);
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
            for (Object o : broker.list(query)) {
               OpActivity subTask = (OpActivity) o;
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
    *
    * @param projectPlanVersionId a <code>long</code> representing the id of a project plan version.
    * @param session              a <code>OpProjectSession</code> representing the session which started the upgrade procedure.
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
      Iterator it = broker.iterate(query);
      OpTransaction t = broker.newTransaction();
      while (it.hasNext()) {
         Object[] result = (Object[]) it.next();

         Long activityVersionId = (Long) result[0];
         OpActivityVersion activityVersion = (OpActivityVersion) broker.getObject(OpActivityVersion.class, activityVersionId);
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
            for (Object o : broker.list(query)) {
               OpActivityVersion subTaskVersion = (OpActivityVersion) o;
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


   public List<OpModuleChecker> getCheckerList() {
      List<OpModuleChecker> checkers = new ArrayList<OpModuleChecker>();
      checkers.add(new OpProjectPlanningModuleChecker());
      return checkers;
   }

}
