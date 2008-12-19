/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 *
 */
package onepoint.project.modules.my_tasks.xmlrpc;

import java.sql.Date;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.my_tasks.OpMyTasksServiceImpl;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityCategory;
import onepoint.project.modules.project.OpActivityComment;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpDependency;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectAdministrationServiceImpl;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpWorkPeriod;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.xml_rpc.OpXMLRPCUtil;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceException;
import onepoint.service.server.XServiceManager;
import onepoint.service.server.XSession;

import org.apache.xmlrpc.XmlRpcException;

/**
 * Xml-Rpc service implementation corresponding to the OpMyTasksServiceSMLRPC
 *
 * @author dfreis
 */
public class OpMyTasksServiceXMLRPC {
   private final static String[] ALLOWERS = new String[]{
      "Name",
      "Description",
      "Type",
      "Attributes",
      "Category.Name",
      "Category.Description",
      "Category.Color",
      "Sequence",
      "OutlineLevel",
      "Start",
      "Finish",
      "Duration",
      "LeadTime",
      "FollowUpTime",
      "Complete",
      "Priority",
      "BaseEffort", // Person hours
      "BaseTravelCosts",
      "BasePersonnelCosts",
      "BaseMaterialCosts",
      "BaseExternalCosts",
      "BaseMiscellaneousCosts",
      "ActualEffort", // Person hours
      "ActualTravelCosts",
      "RemainingTravelCosts",
      "ActualPersonnelCosts",
      "RemainingPersonnelCosts",
      "ActualMaterialCosts",
      "RemainingMaterialCosts",
      "ActualExternalCosts",
      "RemainingExternalCosts",
      "ActualMiscellaneousCosts",
      "RemainingMiscellaneousCosts",
      "RemainingEffort",
      "BaseProceeds",
      "ActualProceeds",
      "RemainingProceeds",
      "Payment",
      "Deleted",
      "Expanded",
      "Template",
      "EffortBillable",
      "ProjectPlan.Start",
      "ProjectPlan.Finish",
      "ProjectPlan.CalculationMode",
      "ProjectPlan.ProgressTracked",
      "ProjectPlan.Template",
      "ProjectPlan.HolidayCalendar",
      "ProjectPlan.ProjectNode.Name",
      "ProjectPlan.ProjectNode.Type",
      "ProjectPlan.ProjectNode.Description",
      "ProjectPlan.ProjectNode.Start",
      "ProjectPlan.ProjectNode.Finish",
      "ProjectPlan.ProjectNode.Budget",
      "ProjectPlan.ProjectNode.Priority",
      "ProjectPlan.ProjectNode.Probability",
      "ProjectPlan.ProjectNode.Archived",
      "ProjectPlan.Creator",
      "ProjectPlan.VersionNumber",
      "Assignments.Assigned",
      "Assignments.Complete",
      "Assignments.BaseEffort",
      "Assignments.ActualEffort",
      "Assignments.RemainingEffort",
      "Assignments.Resource.Name",
      "Assignments.Resource.Description",
      "Assignments.Resource.Description",
      "Assignments.Resource.InheritPoolRate",
      "Assignments.Resource.HourlyRate",
      "Assignments.Resource.ExternalRate",
      "Assignments.BaseCosts",
      "Assignments.ActualCosts",
      "Assignments.BaseProceeds",
      "Assignments.ActualProceeds",
      "Assignments.RemainingProceeds",
      "Assignments.RemainingPersonnelCosts"};

//   private final static String[] IGNORES = new String[]{
//        "SubActivities",
//        "SuccessorDependencies",
//        "PredecessorDependencies",
//        "SuperActivity",
//        "ResponsibleResource",
//        "ProjectPlan.ProjectNode",
//        "ProjectPlan.Activities",
//        "ProjectPlan.ActivityAttachments",
//        "ProjectPlan.ActivityAssignments",
//        "ProjectPlan.WorkPeriods",
//        "ProjectPlan.Dependencies",
//        "ProjectPlan.Versions",
//        "Assignments.ProjectPlan",
//        "Assignments.Resource",
//        "Assignments.Activity",
//        "Assignments.WorkRecords",
//        "Assignments.WorkMonths",
//        "Attachments", 
//        "WorkPeriods",
//        "Versions",
//        "Comments",
//        "ControllingRecords"};
         
   /**
    * the underlaying OpMyTaskService
    */
   private OpMyTasksServiceImpl impl;

   /**
    * the underlaying OpProjectAdministrationServiceImpl
    */
   private OpProjectAdministrationServiceImpl projectAdminImpl;

   /**
    * Default constructor setting up the underlying OpMyTaskService.
    */
   public OpMyTasksServiceXMLRPC() {
      XService xservice = XServiceManager.getService(OpMyTasksServiceImpl.SERVICE_NAME);
      if (xservice == null) {
         throw new IllegalStateException("required service '" + OpMyTasksServiceImpl.SERVICE_NAME + "' not found");
      }
      impl = (OpMyTasksServiceImpl) xservice.getServiceImpl();
      if (impl == null) {
         throw new IllegalStateException("required service impl for 'UserService' not found");
      }
      xservice = XServiceManager.getService(OpProjectAdministrationService.SERVICE_NAME);
      if (xservice == null) {
         throw new IllegalStateException("required service '" + OpProjectAdministrationService.SERVICE_NAME + "' not found");
      }
      projectAdminImpl = (OpProjectAdministrationServiceImpl) xservice.getServiceImpl();
      if (projectAdminImpl == null) {
         throw new IllegalStateException("required service impl for '" +
              OpProjectAdministrationService.SERVICE_NAME + "' not found");
      }
   }

   /**
    * @return a list containing all root tasks that are visible for me
    * @throws XmlRpcException
    * @pre
    * @post
    */
   public List<Map<String, Object>> getRootTasks()
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         List<Map<String, Object>> ret = new LinkedList<Map<String, Object>>();
         Iterator<OpActivity> rootTaskIter = impl.getRootTasks(session, broker);
         Map<String, Object> map;
         while (rootTaskIter.hasNext()) {
            map = getActivityData(rootTaskIter.next());
            //OpXMLRPCUtil.deleteMapNullValues(map);
            ret.add(map);
         }
         return ret;
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }

   }

   public List<Map<String, Object>> getMyTasks()
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         List<Map<String, Object>> ret = new LinkedList<Map<String, Object>>();
         Iterator<OpActivity> rootTaskIter = impl.getMyTasks(session, broker);
         Map<String, Object> map;
         while (rootTaskIter.hasNext()) {
            map = getActivityData(rootTaskIter.next());
            //OpXMLRPCUtil.deleteMapNullValues(map);
            ret.add(map);
         }
         return ret;
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }

   }

   public List<Map<String, Object>> getMyTasks(BitSet types)
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         List<Map<String, Object>> ret = new LinkedList<Map<String, Object>>();
         Iterator<OpActivity> rootTaskIter = impl.getMyTasks(session, broker, types);
         Map<String, Object> map;
         while (rootTaskIter.hasNext()) {
            map = getActivityData(rootTaskIter.next());
            //OpXMLRPCUtil.deleteMapNullValues(map);
            ret.add(map);
         }
         return ret;
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }
   }

   public Map<String, Object> getParentTask(String childId)
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         OpActivity activity = impl.getTaskById(session, broker, Long.parseLong(childId));
         OpActivity parent = impl.getParentTask(session, broker, activity);
         if (parent == null) {
            return (new HashMap<String, Object>());
         }
         return (getActivityData(parent));
         //OpXMLRPCUtil.deleteMapNullValues(map);
         //return map;
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }
   }

   public List<Map<String, Object>> getChildTasks(String parentId)
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         List<Map<String, Object>> ret = new LinkedList<Map<String, Object>>();
         OpActivity activity = impl.getTaskById(session, broker, Long.parseLong(parentId));
         Iterator<OpActivity> childTaskIter = impl.getChildTasks(session, broker, activity);
         Map<String, Object> map;
         while (childTaskIter.hasNext()) {
            map = getActivityData(childTaskIter.next());
            //OpXMLRPCUtil.deleteMapNullValues(map);
            ret.add(map);
         }
         return ret;
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }
   }

   public List<Map<String, Object>> getChildTasks(String parentId, BitSet types)
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         List<Map<String, Object>> ret = new LinkedList<Map<String, Object>>();
         OpActivity activity = impl.getTaskById(session, broker, Long.parseLong(parentId));
         Iterator<OpActivity> childTaskIter = impl.getChildTasks(session, broker, activity, types);
         Map<String, Object> map;
         while (childTaskIter.hasNext()) {
            map = getActivityData(childTaskIter.next());
            //OpXMLRPCUtil.deleteMapNullValues(map);
            ret.add(map);
         }
         return ret;
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }
   }

   public Map<String, Object> getTaskByIs(String id)
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         OpActivity activity = impl.getTaskById(session, broker, Long.parseLong(id));
         return getActivityData(activity);
         //OpXMLRPCUtil.deleteMapNullValues(map);
         //return map;
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }
   }

   public Map<String, Object> insertAdhocTask(Map<String, Object> activityData)
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         OpActivity activity = getActivity(session, broker, activityData);
         impl.insertAdhocTask(session, broker, activity);
         return getActivityData(activity);
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }

   }

   public boolean deleteAdhocTask(Map<String, Object> activityData)
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         OpActivity activity = getActivity(session, broker, activityData);
         impl.deleteAdhocTask(session, broker, activity);
         return true;
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }
   }

   public Map<String, Object> updateAdhocTask(Map<String, Object> activityData)
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         OpActivity activity = getActivity(session, broker, activityData);
         impl.updateAdhocTask(session, broker, activity);
         return getActivityData(activity);
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Returns all information associated to the currently signed on user.
    *
    * @return a map containing all user information.
    */
   private static Map<String, Object> getActivityData(OpActivity activity) {
      return OpXMLRPCUtil.convertToXMLRPCMap(activity, ALLOWERS, false);
   }

   /**
    * Returns all information associated to the currently signed on user.
    *
    * @return a map containing all user information.
    */
   private OpActivity getActivity(OpProjectSession session, OpBroker broker,
        Map<String, Object> activity) {

      // FIXME(dfreis Sep 12, 2007 10:51:27 AM)
      // to be done


      OpActivity ret;
      String id = (String) activity.get(OpActivity.ID);
      if (id != null) {
         ret = impl.getTaskById(session, broker, Long.parseLong(id));
      }
      else {
         ret = new OpActivity();
      }
      Double doubleValue = (Double) activity.get(OpActivity.ACTUAL_EFFORT);
      if (doubleValue != null) {
         ret.setActualEffort(doubleValue.doubleValue());
      }
      doubleValue = (Double) activity.get(OpActivity.ACTUAL_EXTERNAL_COSTS);
      if (doubleValue != null) {
         ret.setActualExternalCosts(doubleValue.doubleValue());
      }
      doubleValue = (Double) activity.get(OpActivity.ACTUAL_MATERIAL_COSTS);
      if (doubleValue != null) {
         ret.setActualMaterialCosts(doubleValue.doubleValue());
      }
      doubleValue = (Double) activity.get(OpActivity.ACTUAL_MISCELLANEOUS_COSTS);
      if (doubleValue != null) {
         ret.setActualMiscellaneousCosts(doubleValue.doubleValue());
      }
      doubleValue = (Double) activity.get(OpActivity.ACTUAL_PERSONNEL_COSTS);
      if (doubleValue != null) {
         ret.setActualPersonnelCosts(doubleValue.doubleValue());
      }
      //ret.setActualProceeds(activity.get(OpActivity.));
      doubleValue = (Double) activity.get(OpActivity.ACTUAL_TRAVEL_COSTS);
      if (doubleValue != null) {
         ret.setActualTravelCosts(doubleValue.doubleValue());
      }

      LinkedList<Map<String, Object>> assignmentsData = (LinkedList<Map<String, Object>>) activity.get(OpActivity.ASSIGNMENTS);
      Set<OpAssignment> assignments = new HashSet<OpAssignment>();
      if (assignmentsData != null) {
         for (Map<String, Object> assignmentData : assignmentsData) {
            ret.addAssignment(getAssignment(assignmentData));
         }
      }

      LinkedList<Map<String, Object>> attachmentsData = (LinkedList<Map<String, Object>>) activity.get(OpActivity.ATTACHMENTS);
      Set<OpAttachment> attachments = new HashSet<OpAttachment>();
      if (attachmentsData != null) {
         for (Map<String, Object> attachmentData : attachmentsData) {
            attachments.add(getAttachment(attachmentData));
         }
      }
      ret.setAttachments(attachments);

//      ret.setAttributes(activity.get(OpActivity.ATTRIBUTES));
      doubleValue = (Double) activity.get(OpActivity.BASE_EFFORT);
      if (doubleValue != null) {
         ret.setBaseEffort(doubleValue.doubleValue());
      }
      doubleValue = (Double) activity.get(OpActivity.BASE_EXTERNAL_COSTS);
      if (doubleValue != null) {
         ret.setBaseExternalCosts(doubleValue.doubleValue());
      }
      doubleValue = (Double) activity.get(OpActivity.BASE_MATERIAL_COSTS);
      if (doubleValue != null) {
         ret.setBaseMaterialCosts(doubleValue.doubleValue());
      }
      doubleValue = (Double) activity.get(OpActivity.BASE_MISCELLANEOUS_COSTS);
      if (doubleValue != null) {
         ret.setBaseMiscellaneousCosts(doubleValue.doubleValue());
      }
      doubleValue = (Double) activity.get(OpActivity.BASE_PERSONNEL_COSTS);
      if (doubleValue != null) {
         ret.setBasePersonnelCosts(doubleValue.doubleValue());
         //ret.setBaseProceeds(activity.get(OpActivity.));
      }
      doubleValue = (Double) activity.get(OpActivity.BASE_TRAVEL_COSTS);
      if (doubleValue != null) {
         ret.setBaseTravelCosts(doubleValue.doubleValue());
      }

      OpActivityCategory category = getActivityCategory((Map<String, Object>) activity.get(OpActivity.CATEGORY));
      ret.setCategory(category);

      LinkedList<Map<String, Object>> commentsData = (LinkedList<Map<String, Object>>) activity.get(OpActivity.COMMENTS);
      Set<OpActivityComment> comments = new HashSet<OpActivityComment>();
      if (commentsData != null) {
         for (Map<String, Object> commentData : commentsData) {
            comments.add(getActivityComment(commentData));
         }
      }
      ret.setComments(comments);
      doubleValue = (Double) activity.get(OpActivity.COMPLETE);
      if (doubleValue != null) {
         ret.setComplete(doubleValue.doubleValue());
      }
      Boolean boolValue = (Boolean) activity.get(OpActivity.DELETED);
      if (boolValue != null) {
         ret.setDeleted(boolValue.booleanValue());
      }

      ret.setDescription((String) activity.get(OpActivity.DESCRIPTION));

      doubleValue = (Double) activity.get(OpActivity.DURATION);
      if (doubleValue != null) {
         ret.setDuration(doubleValue.doubleValue());
      }
      //ret.setDynamicResources(activity.get());
      //ret.setEffectiveAccessLevel(activity.get));
      boolValue = (Boolean) activity.get(OpActivity.EXPANDED);
      if (boolValue != null) {
         ret.setExpanded(boolValue.booleanValue());
      }
      ret.setFinish((Date) activity.get(OpActivity.FINISH));
      //ret.setLocks(activity.get(OpActivity.));
      //ret.setModified(activity.get());
      ret.setName((String) activity.get(OpActivity.NAME));
      Byte byteValue = (Byte) activity.get(OpActivity.OUTLINE_LEVEL);
      if (byteValue != null) {
         ret.setOutlineLevel(byteValue.byteValue());
      }
      //ret.setPermissions(activity.get(OpActivity.));

      LinkedList<Map<String, Object>> predecessorDependenciesData = (LinkedList<Map<String, Object>>) activity.get(OpActivity.PREDECESSOR_DEPENDENCIES);
      Set<OpDependency> predecessorDependencies = new HashSet<OpDependency>();
      if (predecessorDependenciesData != null) {
         for (Map<String, Object> predecessorDependencieData : predecessorDependenciesData) {
            predecessorDependencies.add(getDependency(predecessorDependencieData));
         }
      }
      ret.setPredecessorDependencies(predecessorDependencies);
      byteValue = (Byte) activity.get(OpActivity.PRIORITY);
      if (byteValue != null) {
         ret.setPriority(byteValue.byteValue());
      }


      String stringValue = (String) activity.get(OpActivity.PROJECT_PLAN + "_ID");
      OpProjectPlan projectPlan = null;
      if (stringValue != null) {
         projectPlan = projectAdminImpl.getProjectPlanById(session, broker,
              Long.parseLong(stringValue));
         ret.setProjectPlan(projectPlan);
      }

      doubleValue = (Double) activity.get(OpActivity.REMAINING_EFFORT);
      if (doubleValue != null) {
         ret.setRemainingEffort(doubleValue.doubleValue());
      }

      stringValue = (String) activity.get(OpActivity.RESPONSIBLE_RESOURCE + "_ID");
      if (stringValue != null) {
         ret.setResponsibleResource((OpResource) broker.getObject(OpResource.class,
              Long.parseLong(stringValue)));
      }
      Integer intValue = (Integer) activity.get(OpActivity.SEQUENCE);
      if (intValue != null) {
         ret.setSequence(intValue.intValue());
      }
      ret.setStart((Date) activity.get(OpActivity.START));
      //ret.setSubActivities(activity.get(OpActivity.SUB_ACTIVITIES));

      LinkedList<Map<String, Object>> successorDependenciesData = (LinkedList<Map<String, Object>>) activity.get(OpActivity.SUCCESSOR_DEPENDENCIES);
      Set<OpDependency> successorDependencies = new HashSet<OpDependency>();
      if (successorDependenciesData != null) {
         for (Map<String, Object> successorDependencieData : successorDependenciesData) {
            successorDependencies.add(getDependency(successorDependencieData));
         }
      }
      ret.setSuccessorDependencies(successorDependencies);
      String superId = (String) activity.get(OpActivity.SUPER_ACTIVITY + "_ID");
      if (superId != null) {
         ret.setSuperActivity(impl.getTaskById(session, broker, Long.parseLong(superId)));
      }
      boolValue = (Boolean) activity.get(OpActivity.TEMPALTE);
      if (boolValue != null) {
         ret.setTemplate(boolValue.booleanValue());
      }
      byteValue = (Byte) activity.get(OpActivity.TYPE);
      if (byteValue != null) {
         ret.setType(byteValue);
      }

      LinkedList<Map<String, Object>> versionsData = (LinkedList<Map<String, Object>>) activity.get(OpActivity.SUCCESSOR_DEPENDENCIES);
      Set<OpActivityVersion> versions = new HashSet<OpActivityVersion>();
      if (versionsData != null) {
         for (Map<String, Object> versionData : versionsData) {
            versions.add(getActivityVersion(versionData));
         }
         ret.setVersions(versions);
      }

      LinkedList<Map<String, Object>> workPeriodsData = (LinkedList<Map<String, Object>>) activity.get(OpActivity.SUCCESSOR_DEPENDENCIES);
      Set<OpWorkPeriod> workPeriods = new HashSet<OpWorkPeriod>();
      if (workPeriodsData != null) {
         for (Map<String, Object> workPeriodData : workPeriodsData) {
            workPeriods.add(getWorkPeriod(session, broker, workPeriodData, ret, projectPlan));
         }
         ret.setWorkPeriods(workPeriods);
      }
      return ret;
   }

   /**
    * @param workPeriodData
    * @param activity
    * @param projectPlan
    * @return
    * @pre
    * @post
    */
   private OpWorkPeriod getWorkPeriod(OpProjectSession session, OpBroker broker,
        Map<String, Object> workPeriodData,
        OpActivity activity, OpProjectPlan projectPlan) {
      OpWorkPeriod ret;
      String id = (String) workPeriodData.get(OpWorkPeriod.ID);
      if (id != null) {
         ret = (OpWorkPeriod) broker.getObject(OpWorkPeriod.class, Long.parseLong(id));
      }
      else {
         ret = new OpWorkPeriod();
      }

      ret.setActivity(activity);
      ret.setProjectPlan(projectPlan);

      Double doubleValue = (Double) workPeriodData.get(OpWorkPeriod.BASE_EFFORT);
      if (doubleValue != null) {
         ret.setBaseEffort(doubleValue.doubleValue());
      }
      Date dateValue = (Date) workPeriodData.get(OpWorkPeriod.START);
      if (dateValue != null) {
         ret.setStart(dateValue);
      }

      long workingDays = 0L;
      Integer intValue = (Integer) workPeriodData.get(OpWorkPeriod.WORKING_DAYS + "_HI");
      if (intValue != null) {
         workingDays = ((long) intValue.intValue()) << 32;
      }
      intValue = (Integer) workPeriodData.get(OpWorkPeriod.WORKING_DAYS + "_LO");
      if (intValue != null) {
         workingDays |= (long) intValue.intValue();
         ret.setWorkingDays(workingDays);
      }
      return ret;
   }

   /**
    * @param versionData
    * @return
    * @pre
    * @post
    */
   private OpActivityVersion getActivityVersion(Map<String, Object> versionData) {
      // TODO Auto-generated method stub
      return null;
   }

   /**
    * @param predecessorDependencieData
    * @return
    * @pre
    * @post
    */
   private OpDependency getDependency(Map<String, Object> predecessorDependencieData) {
      // TODO Auto-generated method stub
      return null;
   }

   /**
    * @param commentData
    * @return
    * @pre
    * @post
    */
   private OpActivityComment getActivityComment(Map<String, Object> commentData) {
      // TODO Auto-generated method stub
      return null;
   }

   /**
    * @param name
    * @return
    * @pre
    * @post
    */
   private OpActivityCategory getActivityCategory(Map<String, Object> name) {
      // TODO Auto-generated method stub
      return null;
   }

   /**
    * @param attachmentData
    * @return
    * @pre
    * @post
    */
   private OpAttachment getAttachment(Map<String, Object> attachmentData) {
      // TODO Auto-generated method stub
      return null;
   }

   /**
    * @param assignmentData
    * @return
    * @pre
    * @post
    */
   private OpAssignment getAssignment(Map<String, Object> assignmentData) {
      // TODO Auto-generated method stub
      return null;
   }

//   /**
//    * @param comment
//    * @return
//    * @pre
//    * @post
//    */
//   private static Map<String, Object> getCommentData(OpActivityComment comment) {
//      Map<String, Object> ret = new HashMap<String, Object>();
//      ret.put(OpActivityComment.id, Long.toString(comment.getID()));
//      if (comment.getName() != null) {
//         ret.put(OpActivityComment.NAME, comment.getName());
//      }
//      if (comment.getText() != null) {
//         ret.put(OpActivityComment.TEXT, comment.getText());
//      }
//      ret.put(OpActivityComment.CREATOR + "_ID", Long.toString(comment.getCreator().getID()));
//      return ret;
//   }
//
//   /**
//    * @param version
//    * @return
//    * @pre
//    * @post
//    */
//   private static Map<String, Object> getVersionData(OpActivityVersion version) {
//      // TODO
//      Map<String, Object> ret = new HashMap<String, Object>();
//      return ret;
//   }
//
//   /**
//    * @param attachment
//    * @return
//    * @pre
//    * @post
//    */
//   private static Map<String, Object> getAttachmentData(OpAttachment attachment) {
//      Map<String, Object> ret = new HashMap<String, Object>();
//      ret.put(OpAttachment.id, Long.toString(attachment.getID()));
//      if (attachment.getName() != null) {
//         ret.put(OpAttachment.NAME, attachment.getName());
//      }
//      ret.put(OpAttachment.CONTENT, getContentData(attachment.getContent()));
//      ret.put(OpAttachment.LINKED, attachment.getLinked());
//      if (attachment.getLocation() != null) {
//         ret.put(OpAttachment.LOCATION, attachment.getLocation());
//      }
//      return ret;
//   }
//
//   /**
//    * @param content
//    * @return
//    * @pre
//    * @post
//    */
//   private static Map<String, Object> getContentData(OpContent content) {
//      Map<String, Object> ret = new HashMap<String, Object>();
//      ret.put(OpContent.id, Long.toString(content.getID()));
//      if (content.getMediaType() != null) {
//         ret.put(OpContent.MEDIA_TYPE, content.getMediaType());
//      }
//      // Note: Xml-Rpc does not support long! 
//      ret.put(OpContent.SIZE + "_HI", (int) (content.getSize() >> 32));
//      ret.put(OpContent.SIZE + "_LO", (int) content.getSize());
//      // add documents
////      LinkedList<Map<String,Object>> documents = new LinkedList<Map<String,Object>>();  
////      for (OpDocument document : content.getDocuments()) {
////         documents.add(getDocumentData(document));
////      }      
////      ret.put(OpContent.DOCUMENTS, documents);
//      //TODO: <FIXME author="Lucian Furtos" description="Fix the streaming over XML-RPC, make sure the connection to DB is NOT closed (broker.close())">
//      if (content.getStream() != null) {
//         ret.put(OpContent.STREAM, content.getStream());
//      }
//      //</FIXME>
//      return ret;
//   }
//
////   /**
////    * @param document
////    * @return
////    * @pre
////    * @post
////    */
////   private static Map<String, Object> getDocumentData(OpDocument document) {
////      Map<String, Object> ret = new HashMap<String, Object>();
////      document.getID();
////      document.getName();
////      // TODO Auto-generated method stub
////      return null;
////   }
//
//   /**
//    * @param period
//    * @return
//    * @pre
//    * @post
//    */
//   private static Map<String, Object> getWorkPeriodData(OpWorkPeriod period) {
//      Map<String, Object> ret = new HashMap<String, Object>();
//      ret.put(OpWorkPeriod.id, Long.toString(period.getID()));
//      ret.put(OpWorkPeriod.BASE_EFFORT, period.getBaseEffort());
//      if (period.getStart() != null) {
//         ret.put(OpWorkPeriod.START, period.getStart());
//      }
//      ret.put(OpWorkPeriod.WORKING_DAYS + "_HI", (int) (period.getWorkingDays() >> 32));
//      ret.put(OpWorkPeriod.WORKING_DAYS + "_LO", (int) (period.getWorkingDays()));
//      return ret;
//   }
//
//   /**
//    * @param assignment
//    * @return
//    * @pre
//    * @post
//    */
//   private static Map<String, Object> getAssignmentData(OpAssignment assignment) {
//      Map<String, Object> ret = new HashMap<String, Object>();
//      ret.put(OpAssignment.id, Long.toString(assignment.getID())); // note: xml-rpc does not 
//      ret.put(OpAssignment.ACTUAL_COSTS, assignment.getActualCosts());
//      ret.put(OpAssignment.BASE_COSTS, assignment.getBaseCosts());
//      ret.put(OpAssignment.COMPLETE, assignment.getComplete());
//      ret.put(OpAssignment.ACTUAL_EFFORT, assignment.getActualEffort());
//      ret.put(OpAssignment.BASE_EFFORT, assignment.getBaseEffort());
//      ret.put(OpAssignment.REMAINING_EFFORT, assignment.getRemainingEffort());
//      ret.put(OpAssignment.ASSIGNED, assignment.getAssigned());
//      ret.put(OpAssignment.RESOURCE, getResourceData(assignment.getResource()));
//      // add work records
//      LinkedList<Map<String, Object>> workRecords = new LinkedList<Map<String, Object>>();
//      for (OpWorkRecord workRecord : assignment.getWorkRecords()) {
//         workRecords.add(getWorkRecordData(workRecord));
//      }
//      ret.put(OpAssignment.WORK_RECORDS, workRecords);
//      ret.put(OpAssignment.PROJECT_PLAN + "_ID", Long.toString(assignment.getProjectPlan().getID()));
//      return ret;
//   }

//   /**
//    * @param workRecord
//    * @return
//    * @pre
//    * @post
//    */
//   private static Map<String, Object> getWorkRecordData(OpWorkRecord workRecord) {
//      Map<String, Object> ret = new HashMap<String, Object>();
//      ret.put(OpWorkRecord.id, Long.toString(workRecord.getID())); // note: xml-rpc does not 
//      ret.put(OpWorkRecord.ACTUAL_EFFORT, workRecord.getActualEffort());
//      if (workRecord.getComment() != null) {
//         ret.put(OpWorkRecord.COMMENT, workRecord.getComment());
//      }
//      ret.put(OpWorkRecord.EXTERNAL_COSTS, workRecord.getExternalCosts());
//      ret.put(OpWorkRecord.MATERIAL_COSTS, workRecord.getMaterialCosts());
//      ret.put(OpWorkRecord.MISCELLANEOUS_COSTS, workRecord.getMiscellaneousCosts());
//      ret.put(OpWorkRecord.REMAINING_EFFORT, workRecord.getRemainingEffort());
//      ret.put(OpWorkRecord.TRAVEL_COSTS, workRecord.getTravelCosts());
//      ret.put(OpWorkRecord.WORK_SLIP, getWorkSlipData(workRecord.getWorkSlip()));
//      return ret;
//   }
//
//   /**
//    * @param workSlip
//    * @return
//    * @pre
//    * @post
//    */
//   private static Map<String, Object> getWorkSlipData(OpWorkSlip workSlip) {
//      Map<String, Object> ret = new HashMap<String, Object>();
//      ret.put(OpWorkSlip.id, Long.toString(workSlip.getID())); // note: xml-rpc does not 
//      if (workSlip.getDate() != null) {
//         ret.put(OpWorkSlip.DATE, workSlip.getDate());
//      }
//      ret.put(OpWorkSlip.NUMBER, workSlip.getNumber());
//      ret.put(OpWorkSlip.CREATOR + "_ID", Long.toString(workSlip.getCreator().getID()));
//      return ret;
//   }
//
//   /**
//    * @param resource
//    * @return
//    * @pre
//    * @post
//    */
//   private static Map<String, Object> getResourceData(OpResource resource) {
//      Map<String, Object> ret = new HashMap<String, Object>();
//      ret.put(OpResource.id, Long.toString(resource.getID())); // note: xml-rpc does not 
//      if (resource.getName() != null) {
//         ret.put(OpResource.NAME, resource.getName());
//      }
//      // add absences
////      LinkedList<Map<String,Object>> absences = new LinkedList<Map<String,Object>>();
////      for (Op workRecord : resource.getAbsences()) {
////         workRecords.add(getWorkRecordData(workRecord));
////      }
////      ret.put(OpResource.ABSENCES, resource.getAbsences()); 
////      ret.put(OpResource.ACTIVITY_ASSIGNMENTS, resource.getActivityAssignments()); 
//      ret.put(OpResource.AVAILABLE, resource.getAvailable());
//      if (resource.getDescription() != null) {
//         ret.put(OpResource.DESCRIPTION, resource.getDescription());
//      }
//      ret.put(OpResource.EXTERNAL_RATE, resource.getExternalRate());
//      ret.put(OpResource.HOURLY_RATE, resource.getHourlyRate());
//      ret.put(OpResource.INHERIT_POOL_RATE, resource.getInheritPoolRate());
//      return ret;
//   }
}
