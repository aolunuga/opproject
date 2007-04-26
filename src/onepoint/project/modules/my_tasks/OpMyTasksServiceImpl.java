/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpConnection;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpUser;
import onepoint.service.server.XServiceException;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Service Implementation for MyTasks.
 * This class is capable of inserting updating and deleting
 * AdHocTasks and AdHocTask attachments.
 * Traversing MyTasks by requesting root tasks and children tasks.
 * Requesting MyTasks by id (whether by String or long).
 *
 * @author dfreis
 */
public class OpMyTasksServiceImpl {
   private static final XLog LOGGER =
        XLogFactory.getServerLogger(OpMyTasksServiceImpl.class);
   private static final String MAX_ACTIVITY_SEQUENCE =
        "select max(activity.Sequence) from OpActivity activity";
   static final OpMyTasksErrorMap ERROR_MAP = new OpMyTasksErrorMap();

   // type filter text;
   /**
    * Standard type.
    */
   public static final byte TYPE_STANDARD = OpGanttValidator.STANDARD;

   /**
    * Milestone type.
    */
   public static final byte TYPE_MILESTONE = OpGanttValidator.MILESTONE;

   /**
    * Collection type.
    */
   public static final byte TYPE_COLLECTION = OpGanttValidator.COLLECTION;

   /**
    * Task task type.
    */
   public static final byte TYPE_TASK = OpGanttValidator.TASK;

   /**
    * Collection task type.
    */
   public static final byte TYPE_COLLECTION_TASK = OpGanttValidator.COLLECTION_TASK;

   /**
    * Scheduled task type.
    */
   public static final byte TYPE_SCHEDULED_TASK = OpGanttValidator.SCHEDULED_TASK;

   /**
    * AdHoc task type.
    */
   public static final byte TYPE_ADHOC_TASK = OpGanttValidator.ADHOC_TASK;

   private static final String ALL_ROOT_ACTIVITIES =
        "select activity from OpActivity as activity"
             + " inner join activity.Assignments as assignment"
             + " where assignment.Resource.ID in (:resourceIds)"
             + " and activity.SuperActivity = null" // no parent
//    + " and activity.ProjectPlan.ID :project" 
             + " and activity.Deleted = false"
             + " order by activity.Sequence";

   private static final String ALL_ROOT_ACTIVITIES_OF_GIVEN_TYPE =
        "select activity from OpActivity as activity"
             + " inner join activity.Assignments as assignment"
             + " where assignment.Resource.ID in (:resourceIds)"
             + " and activity.SuperActivity = null" // no parent
//    + " and activity.ProjectPlan.ID :project" 
             + " and activity.Type in (:types)" // = 6 == ADHOC_TASK
             + " and activity.Deleted = false"
             + " order by activity.Sequence";

// private static final Criteria ALL_ROOT_ACTIVITIES_OF_GIVEN_TYPE = 
// new Criteria(OpActivity.class)
// .createAlias("Assignments","assignments")
// .add(Expression.gt("assignments.Resource.ID", new Long(83)))
// .add(Expression.isNull("SuperActivity"))
// .add(Expression.eq("Deleted", new Boolean(false)))
// .add(Expression.in("Type", new Byte[] {0,1,2,3,4,5,6}))
// .addOrder( Order.asc("Sequence"))

   /**
    * Returns an iterator over all tasks that do not have a parent
    * task (= root) and the user is allowed to see.
    *
    * @param session the session of the user
    * @param broker  the broker to use.
    * @return an iterator over all tasks that do not have a parent.
    *         task and are allowed for the user to be seen.
    */

   public final Iterator<OpActivity> getRootTasks(
        OpProjectSession session, OpBroker broker) {
      // get myResourceIds
      OpUser user = session.user(broker);
      Set<OpResource> resources = user.getResources();

      // construct query
      OpQuery query = broker.newQuery(ALL_ROOT_ACTIVITIES);
      query.setCollection("resourceIds", resources);

      // type save required...
      final Iterator iter = broker.iterate(query);
      return new Iterator<OpActivity>() {
         public boolean hasNext() {
            return iter.hasNext();
         }

         public OpActivity next() {
            return (OpActivity) iter.next();
         }

         public void remove() {
            iter.remove();
         }
      };
   }

   /**
    * Returns an iterator over all tasks that do not have a parent
    * task (= root) matching the given types. The user must have
    * sufficient privileged to see these tasks.
    *
    * @param session the session of the user
    * @param broker  the broker to use.
    * @param types   the types that are to be filtered;
    * @return an iterator over all tasks that do not have a parent.
    *         task, match one of the given types and are allowed for the
    *         user to be seen.
    */

   public final Iterator<OpActivity> getRootTasks(
        OpProjectSession session, OpBroker broker, BitSet types) {
      // get myResourceIds
      OpUser user = session.user(broker);
      Set<OpResource> resources = user.getResources();
      // construct query
      OpQuery query = broker.newQuery(ALL_ROOT_ACTIVITIES_OF_GIVEN_TYPE);
      query.setCollection("resourceIds", resources);
      LinkedList<Integer> type_list = new LinkedList<Integer>();
      for (int bit = types.nextSetBit(0); bit >= 0; bit = types.nextSetBit(bit + 1)) {
         type_list.add(bit);
      }
      query.setCollection("types", type_list);
      // type save required...
      final Iterator iter = broker.iterate(query);
      return new Iterator<OpActivity>() {
         public boolean hasNext() {
            return iter.hasNext();
         }

         public OpActivity next() {
            return (OpActivity) iter.next();
         }

         public void remove() {
            iter.remove();
         }
      };
   }

   /**
    * Returns an iterator over all child tasks that have the given
    * common parent activity. The user must have sufficient
    * privileged to see these tasks.
    *
    * @param session  the session of the user
    * @param broker   the broker to use.
    * @param activity the parent task to get the children for.
    * @return an iterator over all tasks representing children of
    *         the given activity.
    */
   public final Iterator<OpActivity> getChildTasks(
        OpProjectSession session, OpBroker broker, OpActivity activity) {
      return getChildTasks(session, broker, activity, null);
   }

   /**
    * Returns an iterator over all child tasks that have the given
    * common parent activity and match one of the given types.
    * The user must have sufficient privileged to see these tasks.
    *
    * @param session  the session of the user
    * @param broker   the broker to use.
    * @param activity the parent task to get the children for.
    * @param types    the types that are to be filtered;
    * @return an iterator over all tasks representing children of the given activity.
    */
   public final Iterator<OpActivity> getChildTasks(
        OpProjectSession session, OpBroker broker, OpActivity activity, BitSet types) {
      LinkedList<OpActivity> ret = new LinkedList<OpActivity>();
      Iterator<OpActivity> iter = activity.getSubActivities().iterator();
      OpActivity to_add;
      while (iter.hasNext()) {
         to_add = iter.next();
         if (readGranted(session, to_add)) {
            if (types == null || types.get(to_add.getType())) {
               ret.add(to_add);
            }
         }
      }
      return ret.iterator();
   }

   /**
    * Puts the given <code>activity</code> to the users list of activities.
    *
    * @param activity the activity that is to be inserted.
    * @throws XServiceException {@link OpMyTasksError#EMPTY_NAME_ERROR_CODE}
    *                           if no name is set within the given activity.
    * @throws XServiceException {@link OpMyTasksError#NO_PROJECT_ERROR_CODE}
    *                           if no project plan is set within the given activity.
    * @throws XServiceException {@link OpMyTasksError#INVALID_PRIORITY_ERROR_CODE}
    *                           if the priority within the given activity is out of scope [1,9]
    * @throws XServiceException {@link OpMyTasksError#NO_RESOURCE_ERROR_CODE}
    *                           if no assignment to a resource was set within the given activity.
    * @throws XServiceException {@link OpMyTasksError#INVALID_TYPE_ERROR_CODE}
    *                           if the given activity is not of type {@link OpActivity#ADHOC_TASK}.
    * @throws XServiceException {@link OpMyTasksError#INSUFICIENT_PERMISSIONS_ERROR_CODE}
    *                           if the user does not have sufficient privileges to insert an AdHoc Task.
    */
   public final void insertAdhocTask(OpProjectSession session, OpBroker broker, OpActivity activity)
        throws XServiceException {
      //task name - mandatory
      if (activity.getName() == null) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.EMPTY_NAME_ERROR_CODE)));
      }

      //project & resource
      if (activity.getProjectPlan() == null) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_PROJECT_ERROR_CODE)));
      }

      // check priority
      if ((activity.getPriority() <= 0) || (activity.getPriority() > 9)) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.INVALID_PRIORITY_ERROR_CODE)));
      }
      if (activity.getAssignments() == null || activity.getAssignments().isEmpty()) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_RESOURCE_ERROR_CODE)));
      }
      // check for null resources
      for (OpAssignment assignment : activity.getAssignments()) {
         OpResource resource = assignment.getResource();
         if (resource == null) {
            throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_RESOURCE_ERROR_CODE)));
         }
      }

      // can only insert AdHoc tasks
      if (activity.getType() != OpActivity.ADHOC_TASK) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.INVALID_TYPE_ERROR_CODE)));
      }

      // check rights
      if (!createGranted(session, activity)) {
         throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.INSUFICIENT_PERMISSIONS_ERROR_CODE));
      }

      // get activity sequence
      int sequence = 0;
      // allow queries to return stale state
      int mode = broker.getConnection().getFlushMode();
      broker.getConnection().setFlushMode(OpConnection.FLUSH_MODE_COMMIT);

      OpQuery query = broker.newQuery(MAX_ACTIVITY_SEQUENCE);
      Iterator it = broker.list(query).iterator();
      if (it.hasNext()) {
         Integer maxSeq = (Integer) it.next();
         if (maxSeq != null) {
            sequence = maxSeq + 1;
         }
      }
      // set flush mode back again
      broker.getConnection().setFlushMode(mode);

      activity.setSequence(sequence);
      broker.makePersistent(activity);
      // store assignments
      for (OpAssignment assignment : activity.getAssignments()) {
         broker.makePersistent(assignment);
      }
      // store attachments
      Iterator<OpAttachment> attachmentIter = activity.getAttachments().iterator();
      OpAttachment attachment;
      OpContent content;
      while (attachmentIter.hasNext()) {
         attachment = attachmentIter.next();
         content = attachment.getContent();
         if (content != null) {
            broker.makePersistent(content);
         }
         broker.makePersistent(attachment);
      }

      // flush connection to ensure possible exceptions here!
      broker.getConnection().flush();
   }

   /**
    * Deletes the given AdHoc Task.
    *
    * @param session  the session of the user
    * @param broker   the broker to use.
    * @param activity the task that is to be deleted.
    * @throws XServiceException {@link OpMyTasksError#TASK_NOT_FOUND_ERROR_CODE}
    *                           if the given task is <code>null</code>.
    * @throws XServiceException {@link OpMyTasksError#INSUFICIENT_PERMISSIONS_ERROR_CODE}
    *                           if the user is not logged in, or does not have sufficient privileges
    *                           to delete the given task.
    * @throws XServiceException {@link OpMyTasksError#INVALID_TYPE_ERROR_CODE}
    *                           if the given activity is not of type {@link OpActivity#ADHOC_TASK}.
    * @throws XServiceException {@link OpMyTasksError#EXISTING_WORKSLIP_ERROR_CODE}
    *                           if there exist workslips belonging to the given activity.
    */
   public void deleteAdhocTask(
        OpProjectSession session, OpBroker broker, OpActivity activity)
        throws XServiceException {
      if (activity == null) {
         LOGGER.warn("ERROR: given task is <null>");
         throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.TASK_NOT_FOUND_ERROR_CODE));
      }
      // check rights
      if (!deleteGranted(session, activity)) {
         throw new XServiceException(session.newError(ERROR_MAP,
              OpMyTasksError.INSUFICIENT_PERMISSIONS_ERROR_CODE));
      }

      if ((activity.getType() & OpActivity.ADHOC_TASK) != OpActivity.ADHOC_TASK) {
         throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.INVALID_TYPE_ERROR_CODE));
      }

      // cannot delete if work records exist
      for (OpAssignment assignment : activity.getAssignments()) {
         if (!assignment.getWorkRecords().isEmpty()) {
            throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.EXISTING_WORKSLIP_ERROR_CODE));
         }
      }
      broker.deleteObject(activity);
   }

   /**
    * Updates the given AdHoc Task.
    *
    * @param session  the session of the user
    * @param broker   the broker to use.
    * @param activity the task that is to be updated within the database.
    * @throws XServiceException {@link OpMyTasksError#TASK_NOT_FOUND_ERROR_CODE}
    *                           if the given task is <code>null</code>.
    * @throws XServiceException {@link OpMyTasksError#INSUFICIENT_PERMISSIONS_ERROR_CODE}
    *                           if the user is not logged in, or does not have sufficient privileges to delete the given task.
    * @throws XServiceException {@link OpMyTasksError#INVALID_TYPE_ERROR_CODE}
    *                           if the given activity is not of type {@link OpActivity#ADHOC_TASK}.
    * @throws XServiceException {@link OpMyTasksError#EMPTY_NAME_ERROR_CODE}
    *                           if no name is set within the given activity.
    * @throws XServiceException {@link OpMyTasksError#NO_PROJECT_ERROR_CODE}
    *                           if no project plan is set within the given activity.
    * @throws XServiceException {@link OpMyTasksError#INVALID_PRIORITY_ERROR_CODE}
    *                           if the priority within the given activity is out of scope [1,9]
    * @throws XServiceException {@link OpMyTasksError#NO_RESOURCE_ERROR_CODE}
    *                           if no assignment to a resource was set within the given activity.
    * @throws XServiceException {@link OpMyTasksError#EXISTING_WORKSLIP_ERROR_CODE}
    *                           if there exist workslips belonging to the given activity.
    */
   public void updateAdhocTask(OpProjectSession session, OpBroker broker, OpActivity activity)
        throws XServiceException {
      if (activity == null) {
         throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.TASK_NOT_FOUND_ERROR_CODE));
      }
      // check rights
      if (!editGranted(session, activity)) {
         throw new XServiceException(session.newError(ERROR_MAP,
              OpMyTasksError.INSUFICIENT_PERMISSIONS_ERROR_CODE));
      }

      if ((activity.getType() & OpActivity.ADHOC_TASK) != OpActivity.ADHOC_TASK) {
         throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.INVALID_TYPE_ERROR_CODE));
      }

      //task name - mandatory
      if (activity.getName() == null) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.EMPTY_NAME_ERROR_CODE)));
      }

      //project & resource
      if (activity.getProjectPlan() == null) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_PROJECT_ERROR_CODE)));
      }

      // check priority
      if ((activity.getPriority() <= 0) || (activity.getPriority() > 9)) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.INVALID_PRIORITY_ERROR_CODE)));
      }

      if (activity.getAssignments() == null || activity.getAssignments().isEmpty()) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_RESOURCE_ERROR_CODE)));
      }

      // check for null resources
      for (OpAssignment assignment : activity.getAssignments()) {
         OpResource resource = assignment.getResource();
         if (resource == null) {
            throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_RESOURCE_ERROR_CODE)));
         }
      }

      broker.getConnection().flush();
   }

   /**
    * Returns the AdHoc task identified by the given id, or null if no such task was found.
    *
    * @param session the session of the user.
    * @param broker  the broker to use.
    * @param id      the id of the AdHoc task to retrieve.
    * @return the activity identified by id.
    */
   public OpActivity getTaskById(
        OpProjectSession session, OpBroker broker, long id) {
      OpActivity activity = (OpActivity) broker.getObject(OpActivity.class, id);
      if (!readGranted(session, activity)) {
         return null;
      }
      return activity;
   }

   /**
    * Returns the AdHoc task identified by the given idString, or null if no such task was found.
    *
    * @param session  the session of the user.
    * @param broker   the broker to use.
    * @param idString the string representing an identifier of the AdHoc task to retrieve.
    * @return the activity identified by idString.
    */
   public OpActivity getTaskByIdString(
        OpProjectSession session, OpBroker broker, String idString) {
      OpActivity activity = (OpActivity) broker.getObject(idString);
      if (!readGranted(session, activity)) {
         return null;
      }

      return activity;
   }

// public void insertMyAttachment(OpProjectSession session, OpBroker broker, OpAttachment attachment, OpActivity activity)
// throws XServiceException {
// OpActivityDataSetFactory.createAttachment(broker, activity, activity.getProjectPlan(), attachmentElement, null);
// }

   /**
    * Deletes the given attachment from its corresponding task.
    *
    * @param session    the session of the user.
    * @param broker     the broker to use.
    * @param attachment the attachment that is to be deleted.
    */
   // FIXME(dfreis Apr 4, 2007 8:01:56 PM)
   // should be private!!
   void deleteAttachment(OpProjectSession session, OpBroker broker, OpAttachment attachment) {
      broker.deleteObject(attachment);
   }

// public void updateMyAttachment(
//   OpProjectSession session, OpBroker broker, OpAttachment attachment)
// throws XServiceException {
// }

   /**
    * It check if the user is at least Observer on the project
    *
    * @param session  the project session
    * @param activity the activity to check
    * @return true if the user has rights to view adhoc task
    */
   public static boolean readGranted(OpProjectSession session, OpActivity activity) {
      // read rights include admin rights
      if (basicRightsCheck(session)) {
         return true;
      }
      // no riths below OBSERVER
      if (hasProjectRights(session, activity, OpPermission.OBSERVER)) {
         return true;
      }
      else {
         LOGGER.warn("Insufficient Adhoc task view permissions!");
         return false;
      }
   }

   /**
    * It check if the user is administrator, project manager or project contributor
    *
    * @param session  the project session
    * @param activity the activity to check
    * @return true if the user has rights to create adhoc task
    */
   public static boolean createGranted(OpProjectSession session, OpActivity activity) {
      // create rights include admin rights
      if (basicRightsCheck(session)) {
         return true;
      }
      // no riths below CONTRIBUTOR
      if (hasProjectRights(session, activity, OpPermission.CONTRIBUTOR)) {
         return true;
      }
      else {
         LOGGER.warn("Insufficient Adhoc task create permissions!");
         return false;
      }
   }

   /**
    * It check if the user administrator, project manager or responsible user for assingned resource
    *
    * @param session  the project session
    * @param activity the activity to check
    * @return true if the user has rights to edit adhoc tasks
    */
   public static boolean editGranted(OpProjectSession session, OpActivity activity) {
      // delete rights include admin rights
      if (basicRightsCheck(session)) {
         return true;
      }
      // no riths below MANAGER
      if (hasProjectRights(session, activity, OpPermission.MANAGER)) {
         return true;
      }
      // be a responsible user
      OpBroker broker = session.newBroker();
      try {
         for (OpAssignment assignment : activity.getAssignments()) {
            OpResource resource = assignment.getResource();
            if (resource == null) {
               throw (new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_RESOURCE_ERROR_CODE)));
            }
            // responsible user can create/update AdHoc Tasks
            if (resource.getUser().getID() == session.getUserID()) {
               return true;
            }
         }
      }
      finally {
         broker.close();
      }
      LOGGER.warn("Insufficient Adhoc task edit permissions!");
      return false;
   }

   /**
    * It check if the user is administrator or project manager
    *
    * @param session  the project session
    * @param activity the activity to check
    * @return true if the user has rights to delete adhoc tasks
    */
   public static boolean deleteGranted(OpProjectSession session, OpActivity activity) {
      // delete rights include admin rights
      if (basicRightsCheck(session)) {
         return true;
      }
      // no riths below MANAGER
      if (hasProjectRights(session, activity, OpPermission.MANAGER)) {
         return true;
      }
      else {
         LOGGER.warn("Insufficient Adhoc task delete permissions!");
         return false;
      }
   }

   /**
    * Check if the user is log-in and if it's Administrator.
    *
    * @param session the project session
    * @return true if Administrator rights, fals othetwise or if not logged-in
    */
   private static boolean basicRightsCheck(OpProjectSession session) {
      // check if logged in
      if (!session.isLoggedOn()) {
         LOGGER.info("not logged in!");
         return false;
      }
      // administrator has all the rights
      return session.getUserID() == session.getAdministratorID();
   }

   /**
    * Check if the current user has the required permissions on the parent project of the given activity.
    *
    * @param session  the project session
    * @param activity the activity to check
    * @param reqLevel the required permission level
    * @return true if has the required permissions
    */
   private static boolean hasProjectRights(OpProjectSession session, OpActivity activity, byte reqLevel) {
      OpBroker broker = session.newBroker();
      try {
         // Project associated with AdHoc Tasks
         OpProjectNode project = activity.getProjectPlan().getProjectNode();
         byte level = session.effectiveAccessLevel(broker, project.getID());
         if (level >= reqLevel) { // the required level
            return true;
         }
      }
      finally {
         broker.close();
      }
      return false;
   }
}
