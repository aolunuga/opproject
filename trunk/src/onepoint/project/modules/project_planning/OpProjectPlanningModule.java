/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import onepoint.express.server.XFormLoader;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.module.OpModuleChecker;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpActivityVersionDataSetFactory;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanValidator;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkRecord;

/**
 * Project planning module class.
 *
 * @author : mihai.costin
 */
public class OpProjectPlanningModule extends OpModule {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpProjectPlanningModule.class);


   /**
    * @see onepoint.project.module.OpModule#start(onepoint.project.OpProjectSession)
    */
   @Override
   public void start(OpProjectSession session) {
      // Register project components
      OpProjectComponentHandler project_handler = new OpProjectComponentHandler();
      XFormLoader.registerComponent(OpProjectComponentHandler.GANTT_BOX, project_handler);
      XFormLoader.registerComponent(OpProjectComponentHandler.PROJECT_GANTT_BOX, project_handler);
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
      try {
         OpQuery query = broker.newQuery("select project from OpProjectNode as project where project.Type = ?");
         query.setByte(0, OpProjectNode.PROJECT);

         Iterator result = broker.iterate(query);
         List<Long> projectPlans = new ArrayList<Long>();
         //validate the tasks & collection tasks.
         while (result.hasNext()) {
            OpProjectNode projectNode = (OpProjectNode) result.next();
            OpProjectPlan projectPlan = projectNode.getPlan();

            logger.info("Upgrade tasks and scheduled tasks for " + projectNode.getName());
            updateTasks(projectPlan.getId(), projectNode, session);
            updateActivitiesAndChildren(projectPlan.getId(), session);

            //same update for all the activity versions from all the project versions.
            Set allVersions = projectPlan.getVersions();
            for (Object allVersion : allVersions) {
               OpProjectPlanVersion planVersion = (OpProjectPlanVersion) allVersion;

               updateTasksVersions(planVersion.getId(), projectNode, session);
               updateActivityVersionsAndChildren(planVersion.getId(), session);
            }
            projectPlans.add(projectPlan.getId());
         }
         //recalculate the project plans
         revalidateProjectPlans(session, projectPlans);
      }
      finally {
         broker.closeAndEvict();         
      }
   }

   /**
    * Upgrades this module to version #5 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion5(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select projectPlan from OpProjectNode project inner join project.Plan projectPlan where project.Type = ?");
         query.setByte(0, OpProjectNode.PROJECT);
         Iterator iterator = broker.iterate(query);
         while (iterator.hasNext()) {
            OpProjectPlan projectPlan = (OpProjectPlan) iterator.next();
            updateProceeds(projectPlan, broker);
         }
      }
      finally {
         broker.closeAndEvict();         
      }
   }

   /**
    * Upgrades this module to version #26 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion26(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
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
            for (OpProjectPlanVersion planVersion : projectPlan.getVersions()) {
               // planVersion.setHolidayCalendar(holidayCalendarId);
               broker.updateObject(planVersion);
            }
            logger.info("Upgrade holiday calendar id to: " + holidayCalendarId);
            broker.updateObject(projectPlan);
         }
         tx.commit();
      }
      finally {
         broker.closeAndEvict();
      }
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
      try {
         Iterator result = broker.iterate(query);

         while (result.hasNext()) {
            OpProjectPlan projectPlan = (OpProjectPlan) result.next();
            OpProjectPlanValidator.getInstance().validateProjectPlanWorkingVersion(session, broker, projectPlan, null, true);
         }
      }
      finally {
         broker.closeAndEvict();         
      }
   }

   /**
    * Added 2 new fields on OpActivity: remainingPersonnelCosts & remainingProceeds.
    *
    * @param session project session
    */
   public void upgradeToVersion33(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction tx = broker.newTransaction();
         List<OpActivity> collectionList = new ArrayList<OpActivity>();

         //set the collection's remaining personnel costs and remaining proceeds to 0
         OpQuery query = broker.newQuery("from OpActivity activity where not exists (select assignment.id from OpAssignment assignment where assignment.Activity.id = activity.id)");
         Iterator<OpActivity> result = broker.iterate(query);

         while (result.hasNext()) {
            OpActivity activity = result.next();
            activity.setRemainingPersonnelCosts(0d);
            activity.setRemainingProceeds(0d);
            collectionList.add(activity);
         }

         query = broker.newQuery("from OpActivity activity where exists (select assignment.id from OpAssignment assignment where assignment.Activity.id = activity.id)");
         result = broker.iterate(query);

         while (result.hasNext()) {
            OpActivity activity = result.next();
            activity.setRemainingPersonnelCosts(0d);
            activity.setRemainingProceeds(0d);
            double totalRemainingPersonnelCosts = 0;
            double totalRemainingProceeds = 0;

            //update the remaining personnel costs and remaining proceeds on the activity and its parents
            for (OpAssignment assignment : activity.getAssignments()) {
               totalRemainingPersonnelCosts -= assignment.getRemainingPersonnelCosts();
               totalRemainingProceeds -= assignment.getRemainingProceeds();
            }
            activity.updateRemainingPersonnelCosts(totalRemainingPersonnelCosts);
            activity.updateRemainingProceeds(totalRemainingProceeds);
            broker.updateObject(activity);
         }

         for (OpActivity activity : collectionList) {
            broker.updateObject(activity);
         }

         tx.commit();
      }
      finally {
         broker.closeAndEvict();         
      }
   }

   /**
    * Added priorities for activities (except collection, collection tasks and scheduled tasks). All other activities
    *    which had no priority will now have a default one.
    *
    * @param session - the project session.
    */
   public void upgradeToVersion48(OpProjectSession session) {
      List<Byte> acceptedTypes = new ArrayList<Byte>();
      acceptedTypes.add(new Byte(OpActivity.STANDARD));
      acceptedTypes.add(new Byte(OpActivity.MILESTONE));
      acceptedTypes.add(new Byte(OpActivity.TASK));

      OpBroker broker = session.newBroker();
      try {
         OpTransaction tx = broker.newTransaction();

         //set the priority default value on all activities which are not collections and for which this field is null
         OpQuery query = broker.newQuery("from OpActivity activity where activity.Type in (:types) and activity.Priority = (:noPriority)");
         query.setCollection("types", acceptedTypes);
         query.setInteger("noPriority", 0);
         Iterator<OpActivity> result = broker.iterate(query);
         while (result.hasNext()) {
            OpActivity activity = result.next();
            activity.setPriority(OpActivity.DEFAULT_PRIORITY);
            broker.updateObject(activity);
         }

         //get all activities versions belonging to working plan versions and update their priorities to the default value
         query = broker.newQuery("from OpActivityVersion activityVersion where activityVersion.PlanVersion.VersionNumber = (:workingVersionNumber) and activityVersion.Type in (:types) and activityVersion.Priority = (:noPriority)");
         query.setInteger("workingVersionNumber", OpProjectPlan.WORKING_VERSION_NUMBER);
         query.setCollection("types", acceptedTypes);
         query.setInteger("noPriority", 0);
         Iterator<OpActivityVersion> resultVersion = broker.iterate(query);
         while (resultVersion.hasNext()) {
            OpActivityVersion activityVersion = resultVersion.next();
            activityVersion.setPriority(OpActivity.DEFAULT_PRIORITY);
            broker.updateObject(activityVersion);
         }

         tx.commit();
      }
      finally {
         broker.closeAndEvict();         
      }
   }
   /**
    * Added priorities for activities (except collection, collection tasks and scheduled tasks). All other activities
    *    which had no priority will now have a default one.
    *
    * @param session - the project session.
    */
   public void upgradeToVersion59(OpProjectSession session) {
      logger.info("Removing ResponsibleResources from Collection Activity");
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();
         
         // remove ResponsibleResource from collection type activities and templates 
         OpQuery query = broker.newQuery("update OpActivity as act set act.ResponsibleResource = null"+
               " where (act.Type = "+OpActivity.COLLECTION+
               " or act.Type = "+OpActivity.COLLECTION_TASK+
               " or act.Type = "+OpActivity.SCHEDULED_TASK+
               " or act.Template = true )"+
               " and act.ResponsibleResource is not null");
         broker.execute(query);
         t.commit();
      }
      catch (RuntimeException exc) {
         exc.printStackTrace();
         throw exc;
      }
      finally {
         broker.close();
      }
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
         try {
            OpProjectPlan projectPlan = (OpProjectPlan) broker.getObject(OpProjectPlan.class, projectPlanId);
            OpProjectPlanValidator.getInstance().validateProjectPlan(session, broker, projectPlan, null, OpUser.SYSTEM_USER_NAME);
         }
         finally {
            broker.closeAndEvict();            
         }
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
      try {
         String queryString = "select activity from OpProjectPlan projectPlan inner join projectPlan.Activities activity where projectPlan.id=? and activity.Type=? and activity.Deleted=false";
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
      }
      finally {
         broker.closeAndEvict();         
      }
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
      try {
         String queryString = "select activityVersion from OpProjectPlanVersion projectPlanVersion inner join projectPlanVersion.ActivityVersions activityVersion where projectPlanVersion.id=? and activityVersion.Type=? and activityVersion.Activity.Deleted=false";
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
      }
      finally {
         broker.closeAndEvict();         
      }
   }

   /**
    * For a given project plan, updates the tasks and scheduled tasks.
    *
    * @param projectPlanId a <code>long</code> representing the id of a project plan.
    * @param session       a <code>OpProjectSession</code> representing the session which started the upgrade procedure.
    */
   private void updateActivitiesAndChildren(long projectPlanId, OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         // FIXME(dfreis Jun 10, 2008 10:17:07 AM) reverse query select activity.SuperActivity.Id, ....
         StringBuffer queryBuffer = new StringBuffer("select activity.id, count(subActivity) from OpProjectPlan projectPlan ");
         queryBuffer.append(" inner join projectPlan.Activities activity inner join activity.SubActivities subActivity ");
         queryBuffer.append(" where projectPlan.id=? and activity.Deleted=false and subActivity.Deleted=false and (subActivity.Type=? or subActivity.Type=?)");
         queryBuffer.append(" group by activity.id");
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
            int totalChildCount = OpActivityDataSetFactory.getSubactivitiesCount(broker, activity);
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
               queryBuffer.append(" where activity.id=? and subActivity.Deleted=false and (subActivity.Type=? or subActivity.Type=?) ");
               query = broker.newQuery(queryBuffer.toString());
               query.setLong(0, activity.getId());
               query.setByte(1, OpActivity.TASK);
               query.setByte(2, OpActivity.COLLECTION_TASK);
               for (Object o : broker.list(query)) {
                  OpActivity subTask = (OpActivity) o;
                  subTask.setStart(activity.getStart());
                  subTask.setFinish(activity.getFinish());
                  subTask.setDuration(activity.getDuration());
                  if (OpActivityDataSetFactory.getSubactivitiesCount(broker, subTask) == 0) {
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
      }
      finally {
         broker.closeAndEvict();
      }
   }

   /**
    * For a given project plan, updates the tasks and scheduled tasks versions.
    *
    * @param projectPlanVersionId a <code>long</code> representing the id of a project plan version.
    * @param session              a <code>OpProjectSession</code> representing the session which started the upgrade procedure.
    */
   private void updateActivityVersionsAndChildren(long projectPlanVersionId, OpProjectSession session) {
      OpBroker broker = session.newBroker();
      StringBuffer queryBuffer = new StringBuffer("select activityVersion.id, count(subActivityVersion) from OpProjectPlanVersion projectPlanVersion ");
      queryBuffer.append(" inner join projectPlanVersion.ActivityVersions activityVersion inner join activityVersion.SubActivityVersions subActivityVersion ");
      queryBuffer.append(" where projectPlanVersion.id=? and activityVersion.Activity.Deleted=false and subActivityVersion.Activity.Deleted=false and (subActivityVersion.Type=? or subActivityVersion.Type=?)");
      queryBuffer.append(" group by activityVersion.id");
      queryBuffer.append(" having count(subActivityVersion) > 0");
      OpQuery query = broker.newQuery(queryBuffer.toString());
      try {
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
            int totalChildCount = OpActivityVersionDataSetFactory.getSubactivityVersionsCount(broker, activityVersion);
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
               queryBuffer.append(" where activityVersion.id=? and subActivityVersion.Activity.Deleted=false and (subActivityVersion.Type=? or subActivityVersion.Type=?) ");
               query = broker.newQuery(queryBuffer.toString());
               query.setLong(0, activityVersion.getId());
               query.setByte(1, OpActivity.TASK);
               query.setByte(2, OpActivity.COLLECTION_TASK);
               for (Object o : broker.list(query)) {
                  OpActivityVersion subTaskVersion = (OpActivityVersion) o;
                  subTaskVersion.setStart(activityVersion.getStart());
                  subTaskVersion.setFinish(activityVersion.getFinish());
                  subTaskVersion.setDuration(activityVersion.getDuration());
                  if (OpActivityVersionDataSetFactory.getSubactivityVersionsCount(broker, subTaskVersion) == 0) {
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
      }
      finally {
         broker.closeAndEvict();
      }
   }


   public List<OpModuleChecker> getCheckerList() {
      List<OpModuleChecker> checkers = new ArrayList<OpModuleChecker>();
      checkers.add(new OpProjectPlanningModuleChecker());
      return checkers;
   }

}
