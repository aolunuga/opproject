/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_planning;

import onepoint.express.XComponent;
import onepoint.express.server.XFormLoader;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;

import java.util.*;

/**
 * @author : mihai.costin
 */
public class OpProjectPlanningModule extends OpModule {


   public void start(OpProjectSession session) {

      // Register project components
      OpProjectComponentHandler project_handler = new OpProjectComponentHandler();
      XFormLoader.registerComponent(OpProjectComponentHandler.GANTT_BOX, project_handler);
      XFormLoader.registerComponent(OpProjectComponentHandler.GANTT_MAP, project_handler);
      XFormLoader.registerComponent(OpProjectComponentHandler.UTILIZATION_BOX, project_handler);
   }

   public void upgrade(OpProjectSession session, int dbVersion) {

      //update the project plan with scheduled task activities [ scheduled tasks added in version 2 ]
      if (dbVersion < 3) {

         OpBroker broker = session.newBroker();

         OpQuery query = broker.newQuery("select project from OpProjectNode as project where project.Type = ?");
         query.setByte(0, OpProjectNode.PROJECT);
         Iterator result = broker.iterate(query);
         OpTransaction transaction  = broker.newTransaction();
         List projectPlans = new ArrayList();

         while (result.hasNext()) {
            OpProjectNode projectNode = (OpProjectNode) result.next();
            OpProjectPlan projectPlan = projectNode.getPlan();

            //update all the activities from the current plan
            Iterator activityIterator = projectPlan.getActivities().iterator();
            while (activityIterator.hasNext()) {
               OpActivity activity = (OpActivity) activityIterator.next();
               if (activity.getDeleted()) {
                  //no update for the removed activities.
                  continue;
               }

               //if activity is a task, update the start and end date to the start/end date of the parent (project, if outline 0).
               if (activity.getType() == OpActivity.TASK) {
                  OpActivity superActivity = activity.getSuperActivity();
                  if (superActivity == null) {
                     activity.setStart(projectNode.getStart());
                     activity.setFinish(projectNode.getFinish());
                  }
                  else {
                     activity.setStart(superActivity.getStart());
                     activity.setFinish(superActivity.getFinish());
                  }
                  broker.updateObject(activity);
               }

               //update all the sub activities
               Set subActivities = activity.getSubActivities();
               if (subActivities.size() == 0) {
                  continue;
               }

               boolean hasSubTasks = false;
               boolean hasSubActivities = false;
               List subTasks = new ArrayList();
               for (Iterator iterator = subActivities.iterator(); iterator.hasNext();) {
                  OpActivity subActivity = (OpActivity) iterator.next();
                  if (subActivity.getType() == OpActivity.TASK || subActivity.getType() == OpActivity.COLLECTION_TASK) {
                     hasSubTasks = true;
                     subTasks.add(subActivity);
                  }
                  else {
                     hasSubActivities = true;
                  }
               }

               if (hasSubTasks && !hasSubActivities) {
                  //activity has only subtasks
                  if (activity.getType() != OpActivity.COLLECTION_TASK && activity.getType() != OpActivity.SCHEDULED_TASK)
                  {
                     activity.setType(OpActivity.SCHEDULED_TASK);
                     broker.updateObject(activity);
                  }
               }
               else if (hasSubTasks && hasSubActivities) {
                  //has subtasks as well as subactivities => change all tasks into activity with start & end from collection
                  for (Iterator iterator = subTasks.iterator(); iterator.hasNext();) {
                     OpActivity subTask = (OpActivity) iterator.next();
                     subTask.setStart(activity.getStart());
                     subTask.setFinish(activity.getFinish());
                     subTask.setDuration(activity.getDuration());
                     if (subTask.getSubActivities().size() == 0) {
                        subTask.setType(OpActivity.STANDARD);
                     }
                     else {
                        subTask.setType(OpActivity.COLLECTION);
                     }
                     broker.updateObject(subTask);
                  }
               }
            }

            //same update for all the activity versions from all the project versions.
            Set allVersions = projectPlan.getVersions();
            for (Iterator planVersionsIterator = allVersions.iterator(); planVersionsIterator.hasNext();) {
               OpProjectPlanVersion planVersion = (OpProjectPlanVersion) planVersionsIterator.next();
               Set activities = planVersion.getActivityVersions();
               for (Iterator activityVersionIterator = activities.iterator(); activityVersionIterator.hasNext();) {
                  OpActivityVersion activityVersion = (OpActivityVersion) activityVersionIterator.next();

                  //if activity is a task, update the start and end date to the start/end date of the parent (project, if outline 0).
                  if (activityVersion.getType() == OpActivityVersion.TASK) {
                     OpActivityVersion superActivity = activityVersion.getSuperActivityVersion();
                     if (superActivity == null) {
                        activityVersion.setStart(projectNode.getStart());
                        activityVersion.setFinish(projectNode.getFinish());
                     }
                     else {
                        activityVersion.setStart(superActivity.getStart());
                        activityVersion.setFinish(superActivity.getFinish());
                     }
                     broker.updateObject(activityVersion);
                  }

                  Set subActivities = activityVersion.getSubActivityVersions();
                  if (subActivities.size() == 0) {
                     continue;
                  }

                  boolean hasSubTasks = false;
                  boolean hasSubActivities = false;
                  List subTasks = new ArrayList();
                  for (Iterator iterator = subActivities.iterator(); iterator.hasNext();) {
                     OpActivityVersion subActivity = (OpActivityVersion) iterator.next();
                     if (subActivity.getType() == OpActivityVersion.TASK || subActivity.getType() == OpActivityVersion.COLLECTION_TASK)
                     {
                        hasSubTasks = true;
                        subTasks.add(subActivity);
                     }
                     else {
                        hasSubActivities = true;
                     }
                  }

                  if (hasSubTasks && !hasSubActivities) {
                     //only sub tasks
                     if (activityVersion.getType() != OpActivityVersion.COLLECTION_TASK && activityVersion.getType() != OpActivityVersion.SCHEDULED_TASK)
                     {
                        activityVersion.setType(OpActivityVersion.SCHEDULED_TASK);
                        broker.updateObject(activityVersion);
                     }
                  }
                  else if (hasSubTasks && hasSubActivities) {
                     //has subtasks as well as subactivities => change all tasks to activity with start&end from collection
                     for (Iterator iterator = subTasks.iterator(); iterator.hasNext();) {
                        OpActivityVersion subTask = (OpActivityVersion) iterator.next();
                        subTask.setStart(activityVersion.getStart());
                        subTask.setFinish(activityVersion.getFinish());
                        subTask.setDuration(activityVersion.getDuration());
                        if (subTask.getSubActivityVersions().size() == 0) {
                           subTask.setType(OpActivityVersion.STANDARD);
                        }
                        else {
                           subTask.setType(OpActivityVersion.COLLECTION);
                        }
                        broker.updateObject(subTask);
                     }
                  }
               }
            }
            projectPlans.add(projectPlan);

         }
         transaction.commit();

         transaction = broker.newTransaction();
         //validate all the project plans (this includes also the work phase -> work period upgrade)
         for (Iterator iterator = projectPlans.iterator(); iterator.hasNext();) {
            OpProjectPlan projectPlan = (OpProjectPlan) iterator.next();
            OpProjectNode projectNode = projectPlan.getProjectNode();
            OpProjectPlanVersion workingPlan = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, projectPlan, OpProjectAdministrationService.WORKING_VERSION_NUMBER);

            OpGanttValidator validator = new OpGanttValidator();
            validator.setProjectStart(projectPlan.getProjectNode().getStart());
            validator.setProgressTracked(Boolean.valueOf(projectPlan.getProgressTracked()));
            validator.setProjectTemplate(Boolean.valueOf(projectPlan.getTemplate()));
            validator.setCalculationMode(new Byte(projectPlan.getCalculationMode()));

            HashMap resources = OpActivityDataSetFactory.resourceMap(broker, projectNode);
            XComponent resourceDataSet = new XComponent();
            OpActivityDataSetFactory.retrieveResourceDataSet(resources, resourceDataSet);
            validator.setAssignmentSet(resourceDataSet);

            if (workingPlan != null) {
               XComponent dataSet = new XComponent(XComponent.DATA_SET);
               OpActivityVersionDataSetFactory.retrieveActivityVersionDataSet(broker, workingPlan, dataSet, true);
               validator.setDataSet(dataSet);
               validator.validateDataSet();
               OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, workingPlan, resources, false);
            }
            if (workingPlan == null || projectPlan.getVersions().size() > 1){
               XComponent dataSet = new XComponent(XComponent.DATA_SET);
               OpActivityDataSetFactory.retrieveActivityDataSet(broker, projectPlan, dataSet, true);
               validator.setDataSet(dataSet);
               validator.setAssignmentSet(resourceDataSet);
               validator.validateDataSet();
               OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, resources, projectPlan, null);
            }
         }
         transaction.commit();
         broker.close();
      }
   }

}
