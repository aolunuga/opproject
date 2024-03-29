/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.OpTransactionLock;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.preferences.OpPreferencesService;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityComment;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityIfc;
import onepoint.project.modules.project.OpActivityVersionDataSetFactory;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectError;
import onepoint.project.modules.project.OpProjectErrorMap;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanValidator;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.project.components.OpActivityLoopException;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.forms.OpEditActivityFormProvider;
import onepoint.project.modules.project_planning.msproject.OpMSProjectManager;
import onepoint.project.modules.user.OpLock;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocale;
import onepoint.resource.XLocalizer;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceManager;

/**
 * @author : mihai.costin
 */
public class OpProjectPlanningService extends OpProjectService {

   public final static String SERVICE_NAME = "PlanningService";

   public final static String PROJECT_ID = "project_id";
   public final static String ACTIVITY_ID = "activity_id";
   public final static String ACTIVITY_TYPE = "activity_type";
   public final static String ACTIVITY_SET = "activity_set";
   public final static String SOURCE_PLAN_VERSION_ID = "source_plan_version_id";
   public final static String EDIT_MODE = "edit_mode";
   public final static String BYTES_ARRAY_FIELD = "bytes_array";
   public final static String FILE_NAME_FIELD = "file_name";
   public final static String ROW_OFFSET = "row_offset";

   private static final String TEMPLATE_MAP = "template";
   private final static String ACTIVITY_COMMENTS_PANEL = "ActivityCommentsPanel";
   private final static String ACTIVITY_COMMENT_PANEL = "ActivityCommentPanel";
   private final static String COMMENTS_LABEL_TEXT = "CommentsLabelText";
   private final static String WARNING_ARGUMENT = "warning";
   private final static String ATTACHMENT_ID = "attachment_id";
   private final static String ATTACHMENT_URL = "attachmentUrl";
   private final static String CONTENT_ID = "contentId";
   private final static String CONTENT = "content";
   private final static String FILE_NAME = "fileName";
   private final static String COMMENT_DATA = "comment_data";
   private final static String COMMENT_ID = "comment_id";

   public final static OpProjectPlanningErrorMap PLANNING_ERROR_MAP = new OpProjectPlanningErrorMap();
   public final static OpProjectErrorMap PROJECT_ERROR_MAP = new OpProjectErrorMap();

   private static final XLog logger = XLogFactory.getLogger(OpProjectPlanningService.class);

   public OpProjectPlanningService() {
   }

   public static OpProjectPlanningService getService() {
      return (OpProjectPlanningService) XServiceManager.getService(SERVICE_NAME);
   }

   public XMessage importActivities(OpProjectSession session, XMessage request) {

      String projectId = (String) (request.getArgument(PROJECT_ID));
      boolean editMode = (Boolean) (request.getArgument(EDIT_MODE));
      byte[] file = (byte[]) (request.getArgument(BYTES_ARRAY_FIELD));
      String fileName = (String) (request.getArgument(FILE_NAME_FIELD));

      XMessage reply = new XMessage();

      XComponent dataSet = null;
      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project = (OpProjectNode) (broker.getObject(projectId));
         if (project.getType() != OpProjectNode.PROJECT) {
            reply.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_IMPORT));
            return reply;
         }
         OpProjectPlan projectPlan = project.getPlan();

         if (OpProjectAdministrationService.hasWorkRecords(project, broker)) {
            throw new OpProjectPlanningException(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.IMPORT_ERROR_WORK_RECORDS_EXIST));
         }

         InputStream inFile = new ByteArrayInputStream(file);
         
         try {
            dataSet = doImportActivities(session, fileName, broker, project,
                  projectPlan, inFile);
         }
         catch (OpActivityLoopException loopExc) {
            reply.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.MSPROJECT_FILE_READ_ERROR));
            return reply;
         }
         catch (IOException e) {
            reply.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.MSPROJECT_FILE_READ_ERROR));
            return reply;
         }
         finally {
            try {
               inFile.close();
            }
            catch (IOException exc) {
            }
         }
      }
      finally {
         broker.close();
      }

      //edit if !edit_mode
      if (!editMode) {
         XMessage editRequest = new XMessage();
         editRequest.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         reply = internalEditActivities(session, editRequest);
         if ((reply.getError() != null) && (Boolean.TRUE != reply.getArgument(WARNING_ARGUMENT))) {
            return reply;
         }
      }

      //save
      XMessage saveRequest = new XMessage();
      saveRequest.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      saveRequest.setArgument(OpProjectPlanningService.ACTIVITY_SET, dataSet);
      saveRequest.setArgument(OpProjectPlanningService.SOURCE_PLAN_VERSION_ID, null);
      reply = saveActivities(session, saveRequest);
      if (reply != null && reply.getError() != null) {
         return reply;
      }
//      //check in if !edit_mode
//      if (!editMode) {
//         XMessage checkInRequest = new XMessage();
//         checkInRequest.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
//         checkInRequest.setArgument(OpProjectPlanningService.ACTIVITY_SET, dataSet);
//         reply = internalCheckInActivities(session, checkInRequest);
//         if (reply != null && reply.getError() != null) {
//            return reply;
//         }
//      }
      return reply;
//      catch (Throwable t) {t.printStackTrace(); return null;}
   }

   protected XComponent doImportActivities(OpProjectSession session,
         String fileName, OpBroker broker, OpProjectNode project,
         OpProjectPlan projectPlan, InputStream inFile) throws IOException {
      XComponent dataSet = OpMSProjectManager.importActivities(session, broker, fileName, inFile, projectPlan, getImportExportLocale(session));
      return dataSet;
   }

   public XMessage exportActivities(OpProjectSession session, XMessage request) {
      String projectId = (String) request.getArgument(PROJECT_ID);
      OpBroker broker = session.newBroker();
      try {
         XMessage response = new XMessage();
         OpProjectNode project = null;
         if (projectId != null) {
            project = (OpProjectNode) broker.getObject(projectId);
            if (project.getType() != OpProjectNode.PROJECT) {
               response.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_EXPORT));
               return response;
            }
         }
         XComponent activitySet = (XComponent) request.getArgument(ACTIVITY_SET);
         String fileName = (String) (request.getArgument(FILE_NAME_FIELD));

         ByteArrayOutputStream out = new ByteArrayOutputStream();
         try {
            fileName = export(session, project, activitySet, fileName, out);
         }
         catch (IOException e) {
            response.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.MSPROJECT_FILE_WRITE_ERROR));
            return response;
         }

         byte[] outArray = out.toByteArray();
         response.setArgument(BYTES_ARRAY_FIELD, outArray);
         response.setArgument(FILE_NAME_FIELD, fileName);
         return response;
      }
      finally {
         broker.close();
      }
   }

   /**
    * @param session
    * @param project
    * @param activitySet
    * @param fileName
    * @param out
    * @return
    * @throws IOException
    * @pre
    * @post
    */
   protected String export(OpProjectSession session, OpProjectNode project,
        XComponent activitySet, String fileName, ByteArrayOutputStream out)
        throws IOException {
      return OpMSProjectManager.exportActivities(session, fileName, out, activitySet, getImportExportLocale(session), project);
   }

   public void setEditLock(OpBroker broker, OpProjectSession session, OpProjectNode project) {
      // *** Set lock on project for current user (insert lock, update project)
      logger.debug("begin lock");
      OpLock lock = new OpLock();
      lock.setOwner(session.user(broker));
      lock.setLockerID(new Long(session.getID()));
      broker.makePersistent(lock);
      project.addLock(lock);
      logger.debug("locked");
   }

   /**
    * Performs the project plan check out operation.
    *
    * @param session Current project session.
    * @param request Request containing all the required information at edit time.
    * @return an XMessage reply containing eventual errors.
    */
   public XMessage editActivities(OpProjectSession session, XMessage request) {
      return internalEditActivities(session, request);
   }

   /**
    * Edit method used internally by the service.
    *
    * @see #editActivities(onepoint.project.OpProjectSession,onepoint.service.XMessage)
    */
   protected XMessage internalEditActivities(OpProjectSession session, XMessage request) {
      OpBroker broker = null;
      try {
         broker = session.newBroker();
         OpTransaction tx = broker.newTransaction();
         XMessage reply = internalEditActivities(session, broker, request);
         tx.commit();
         return reply;
      }
      finally {
         finalizeSession(null, broker);
      }
   }

   protected XMessage internalEditActivities(OpProjectSession session, OpBroker broker, XMessage request) {
      // Set persistent lock for current user (working project plan version is created on first save)
      String project_id_string = (String) (request.getArgument(PROJECT_ID));
      OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string));
      
      // Check manager access
      if (!session.checkAccessLevel(broker, project.getId(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Udpate access to project denied; ID = " + project_id_string);
         broker.close();
         throw new OpProjectPlanningException(session.newError(PROJECT_ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
      }

      // *** Check if lock is already set -- if yes throw exception
      if (OpProjectDataSetFactory.hasLocks(broker, project)) {
         logger.error("Project is already locked");
         broker.close();
         throw new OpProjectPlanningException(session.newError(PROJECT_ERROR_MAP, OpProjectError.PROJECT_LOCKED_ERROR));
      }
      setEditLock(broker, session, project);
      XMessage reply = checkExternalModifications(project, session, broker);

      return reply;
   }

   /**
    * Checks for external project modifications in the resource hourly rates and availibility.
    *
    * @param project a <code>OpProjectNode</code> representing the current project.
    * @param session a <code>OpProjectSession</code> representing the server session.
    * @param broker  a <code>OpBroker</code> used for performing business operations.
    * @return a <code>XMessage</code> containing information about the resource changes.
    */
   private XMessage checkExternalModifications(OpProjectNode project, OpProjectSession session, OpBroker broker) {
      XMessage result = new XMessage();

      boolean projectModified = checkProjectModifications(project, session, broker);
      boolean resourcesChanged = checkProjectResourceModifications(project, session, broker);

      if (!projectModified && !resourcesChanged) {
         return result;
      }

      Set<OpActivity> activities = project.getPlan().getActivities();

      if (activities != null && !activities.isEmpty()) {
         // TODO: check, if warning should be 
         if (projectModified && resourcesChanged) {
            result.setArgument(WARNING_ARGUMENT, Boolean.TRUE);
            result.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.PROJECT_AND_RESOURCES_MODIFIED_WARNING));
            return result;
         }
         if (resourcesChanged) {
            result.setArgument(WARNING_ARGUMENT, Boolean.TRUE);
            result.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.RESOURCES_MODIFIED_WARNING));
            return result;
         }
         if (projectModified) {
            result.setArgument(WARNING_ARGUMENT, Boolean.TRUE);
            result.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.PROJECT_MODIFIED_WARNING));
            return result;
         }
      }
      return result;
   }

   /**
    * Checks if any system settings have changed, which cause the need for a revalidation.
    *
    * @param projectNode a <code>OpProjectNode</code>.
    * @param session     a <code>OpProjectSession</code> the server session
    * @return a <code>boolean</code> whether the settings have been modified or not.
    */
   private static boolean checkProjectModifications(OpProjectNode projectNode, OpProjectSession session, OpBroker broker) {
      OpProjectPlanVersion wv = projectNode.getPlan().getWorkingVersion();
      Timestamp recalculated = wv != null ? wv.getRecalculated() : projectNode.getPlan().getRecalculated();
      return checkProjectModifications(projectNode, recalculated, session, broker);
   }

   private static boolean checkProjectModifications(OpProjectNode projectNode, Timestamp planRecalculated, OpProjectSession session, OpBroker broker) {
      Timestamp projectTouched = projectNode.getPlan().getBaseDataChanged();
      return projectTouched != null && (planRecalculated == null || planRecalculated.before(projectTouched));
   }

   private static boolean checkProjectResourceModifications(OpProjectNode projectNode, OpProjectSession session, OpBroker broker) {
      OpProjectPlanVersion wv = projectNode.getPlan().getWorkingVersion();
      Timestamp recalculated = wv != null ? wv.getRecalculated() : projectNode.getPlan().getRecalculated();
      return checkProjectResourceModifications(projectNode, recalculated, session, broker);
   }

   private static boolean checkProjectResourceModifications(OpProjectNode projectNode, Timestamp planRecalculated, OpProjectSession session, OpBroker broker) {
      Iterator<OpProjectNodeAssignment> rait = projectNode.getAssignments().iterator();
      while (rait.hasNext()) {
         OpProjectNodeAssignment a = rait.next();
         if (a.getResource().getBaseDataChanged() != null && (planRecalculated == null || planRecalculated.before(a.getResource().getBaseDataChanged()))) {
            return true;
         }
      }
      return false;
   }


   /**
    * Checks whether resource availibility has changed for an activity.
    *
    * @param activity a <code>OpActivity</code> representing an activity.
    * @param broker   a <code>OpBroker</code> used to perform business operations.
    * @return <code>true</code> if the availibility has changed for any of the resources of the activity.
    */
   private void updateResourceAvailability(OpActivity activity, OpBroker broker) {
      if (activity.getAssignments().size() == 0) {
         return;
      }
      // FIXME: this changes the CURRENTLY CHECKED IN PROJECT PLAN (which should NEVER be done!!!)
      Set<OpAssignment> assignments = activity.getAssignments();
      if (assignments != null) {
         for (OpAssignment assignment : assignments) {
            if (assignment.getAssigned() > assignment.getResource().getAvailable()) {
               assignment.setAssigned(assignment.getResource().getAvailable());
               broker.updateObject(assignment);
            }
         }
      }
   }

   /**
    * Saves the given activity set. Will serialize the activity set and set it as the plan for the working version.
    *
    * @param session The session used
    * @param request The request that contains the parameters required by the save process
    * @return A message that will contain error messages if something went wrong
    */
   public XMessage saveActivities(OpProjectSession session, XMessage request) {
      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      String project_id_string = null;

      try {
         XMessage response = new XMessage();
         logger.debug("OpProjectAdministrationService.saveActivities");
         project_id_string = (String) (request.getArgument(PROJECT_ID));
         if ((project_id_string == null) || (project_id_string.trim().length() == 0)) {
            return response;
         }
         String planVersionLocator = (String) request.getArgument(SOURCE_PLAN_VERSION_ID);
         XComponent dataSet = (XComponent) (request.getArgument(ACTIVITY_SET));

         logger.debug("SAVE-ACTIVITIES " + dataSet.getChildCount());

         OpTransactionLock.getInstance().writeLock(project_id_string);

         OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string));

         // *** Check if current user has lock on project
         if (!OpProjectDataSetFactory.hasLocks(broker, project)) {
            logger.error("Project is currently not being edited");
            throw new OpProjectPlanningException(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.PROJECT_CHECKED_IN_ERROR));
         }
         OpLock lock = project.getLocks().iterator().next();
         checkLock(session, broker, lock);
         t = broker.newTransaction();

         OpProjectPlan projectPlan = saveProjectPlanVersion(session, broker,
               request, project, dataSet, planVersionLocator);

         //update the working version calendar
         response.setArgument(SOURCE_PLAN_VERSION_ID, projectPlan.getWorkingVersion() != null ? projectPlan.getWorkingVersion().locator() : null);

         t.commit();
         logger.debug("/OpProjectAdministrationService.saveActivities");
         return response;
      }
      finally {
         if (project_id_string != null) {
            OpTransactionLock.getInstance().unlock(project_id_string);
         }
         finalizeSession(t, broker);
      }
   }

   private OpProjectPlan saveProjectPlanVersion(OpProjectSession session,
         OpBroker broker, XMessage request, OpProjectNode project,
         XComponent dataSet, String sourcePlanVersionLocator) {
      // Check if project plan already exists (create if not)
      OpProjectPlan projectPlan = project.getPlan();
      if (projectPlan == null) {
         projectPlan = new OpProjectPlan();
         projectPlan.setProjectNode(project);
         projectPlan.copyDatesFromProject();
         projectPlan.setTemplate(project.getType() == OpProjectNode.TEMPLATE);
         broker.makePersistent(projectPlan);
      }
      // create a new project plan version (= working version) if no working version exists 
      OpProjectPlanVersion previousVersion = projectPlan.getWorkingVersion();
      if (previousVersion == null) {
         projectPlan.setWorkingVersion(OpActivityVersionDataSetFactory
               .getInstance().createProjectPlanVersionObject(session,
                     broker, projectPlan, session.user(broker),
                     OpProjectPlan.WORKING_VERSION_NUMBER));
      }

      // retrieve activities to "old" or Source project plan version to match them with edited ones:
      OpProjectPlanVersion sourcePlanVersion = null;
      if (sourcePlanVersionLocator != null && !projectPlan.getWorkingVersion().locator().equals(sourcePlanVersionLocator)) {
         sourcePlanVersion = (OpProjectPlanVersion) broker.getObject(sourcePlanVersionLocator);
      }      
      // Store working copy as project plan version
      HashMap resourceMap = OpActivityDataSetFactory.resourceMap(broker, project);
      OpActivityVersionDataSetFactory.getInstance()
            .storeActivityVersionDataSet(session, broker, dataSet,
                  projectPlan.getWorkingVersion(), resourceMap,
                  sourcePlanVersion, false);

      Timestamp now = new Timestamp(System.currentTimeMillis());
      projectPlan.getWorkingVersion().setRecalculated(now);
      
      // update custom attributes
//      saveActivitiesAdditional(session, broker, request, dataSet, project);
      return projectPlan;
   }

   /**
    * @param session
    * @param broker
    * @param request
    * @param dataSet
    * @param project
    * @pre
    * @post
    */
   protected void saveActivitiesAdditional(OpProjectSession session,
        OpBroker broker, XMessage request, XComponent dataSet,
        OpProjectNode project) {
   }

   /**
    * @param session
    * @param broker
    * @param lock
    * @pre
    * @post
    */
   private void checkLock(OpProjectSession session, OpBroker broker, OpLock lock)
        throws OpProjectPlanningException {
      if (!lock.lockedByMe(session, broker)) {
         logger.error("Project is locked by another user");
         throw new OpProjectPlanningException(session.newError(PROJECT_ERROR_MAP, OpProjectError.PROJECT_LOCKED_ERROR));
      }
   }

   /**
    * Checks in the project plan
    *
    * @param session Current project session
    * @param request Request containing the information needed at check in time.
    * @return an XMessage reply containing eventual errors.
    */
   public XMessage checkInActivities(OpProjectSession session, XMessage request) {
      long start = System.currentTimeMillis();
      logger.debug("OpProjectAdministrationService.checkInActivities");
      String project_id_string = (String) (request.getArgument(PROJECT_ID));
      OpTransactionLock.getInstance().writeLock(project_id_string);
      try {
         return internalCheckInActivities(session, request);
      }
      finally {
         OpTransactionLock.getInstance().unlock(project_id_string);
         logger.debug("OpProjectAdministrationService.checkInActivities lasted: " + (System.currentTimeMillis() - start));
      }
   }

   /**
    * Check in activities used by the service internally.
    *
    * @param session
    * @param request
    * @return
    * @see #checkInActivities(onepoint.project.OpProjectSession,onepoint.service.XMessage)
    */
   protected XMessage internalCheckInActivities(OpProjectSession session, XMessage request) {
      OpBroker broker = null;
      OpTransaction t = null;

      try {
         broker = session.newBroker();
         t = broker.newTransaction();

         XMessage reply = internalCheckInActivities(session, broker, request);

         t.commit();
         broker.close();
         return reply;
      }
      finally {
         finalizeSession(t, broker);
      }
   }

   public XMessage internalCheckInActivities(OpProjectSession session, OpBroker broker,
        XMessage request) {
      String project_id_string = (String) (request.getArgument(PROJECT_ID));
      String sourcePlanVersionLocator = (String) request.getArgument(SOURCE_PLAN_VERSION_ID);
      XComponent dataSet = (XComponent) (request.getArgument(ACTIVITY_SET));
      
      OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string));
      if (project == null) {
         throw new OpProjectPlanningException(session.newError(PROJECT_ERROR_MAP, OpProjectError.PROJECT_NOT_FOUND));
      }

      // Check if project is locked and current user owns the lock
      if (!OpProjectDataSetFactory.hasLocks(broker, project)) {
         logger.error("Project is currently not being edited");
         throw new OpProjectPlanningException(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.PROJECT_CHECKED_IN_ERROR));
      }
      OpLock lock = project.getLocks().iterator().next();
      checkLock(session, broker, lock);

      // through with the checks...
      // 1. save the dataset to the working version (which now always exists)
      OpProjectPlan projectPlan = saveProjectPlanVersion(session, broker, request, project, dataSet, sourcePlanVersionLocator);
      if (projectPlan == null) {
         // TODO: set ERROR?
         return null;
      }
      OpProjectPlanVersion workingPlanVersion = projectPlan.getWorkingVersion();

      // 2. make the working version the latestVersion.
      workingPlanVersion.setVersionNumber(projectPlan.incrementVersionNumber());
      workingPlanVersion.setCheckInTime(new Timestamp(System.currentTimeMillis()));
      
      projectPlan.setWorkingVersion(null);
      projectPlan.setLatestVersion(workingPlanVersion);
      
      if (projectPlan.isImplicitBaseline()) {
         projectPlan.setBaseVersion(workingPlanVersion);
         // todo: fix the activities ?!?
      }
      
      projectPlan.setCreator(session.user(broker).getDisplayName());
      broker.updateObject(projectPlan);

      OpActivityDataSetFactory.getInstance().checkInProjectPlan(session, broker, workingPlanVersion);

      // 4. Finally, delete lock
      project.removeLock(lock);
      broker.deleteObject(lock);
      return null;
   }

   public XMessage revertActivities(OpProjectSession session, XMessage request) {
      logger.debug("OpProjectAdministrationService.revertActivities");
      OpBroker broker = null;
      String project_id_string = (String) (request.getArgument(PROJECT_ID));
      OpTransactionLock.getInstance().writeLock(project_id_string);
      try {
         broker = session.newBroker();
         OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string));

         // Check if project is locked and current user owns the lock
         if (!OpProjectDataSetFactory.hasLocks(broker, project)) {
            logger.error("Project is currently not being edited");
            // TODO: Error handling
            return null;
         }
         OpLock lock = project.getLocks().iterator().next();
         try {
            checkLock(session, broker, lock);
         }
         catch (OpProjectPlanningException exc) {
            if (!session.userIsAdministrator()) {
               return null;
            }
         }

         OpTransaction t = broker.newTransaction();

         // Check if project plan exists: If yes we have to delete a potential working version
         OpProjectPlan projectPlan = project.getPlan();
         if (projectPlan != null) {

            // Try to retrieve working plan version
            OpProjectPlanVersion workingPlanVersion = projectPlan.getWorkingVersion();

            // Delete working version (note: There is not necessarily a working version)
            if (workingPlanVersion != null) {
               OpActivityVersionDataSetFactory.deleteProjectPlanVersion(broker, workingPlanVersion);
            }
         }

         // 4. Finally, delete lock
         broker.deleteObject(lock);

         t.commit();
         broker.close();
      }
      finally {
         OpTransactionLock.getInstance().unlock(project_id_string);
         finalizeSession(null, broker);
      }
      return null;
   }

   public XMessage prepareAttachment(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(ATTACHMENT_ID));

      OpBroker broker = session.newBroker();
      try {
         OpObjectIfc object = broker.getObject(id_string);
         if (object == null) {
            logger.warn("Could not find attachment with ID " + id_string);
            return null;
         }

         XMessage response = new XMessage();
         if (!session.checkAccessLevel(broker, object.getId(), OpPermission.OBSERVER)) {
            response.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.INSUFICIENT_ATTACHMENT_PERMISSIONS));
            return response;
         }

         try {
            // <FIXME author="Horia Chiorean" description="This code is here because we can have either OpAttachment or
            // OpAttachmentVersion">
            Method getContentMethod = object.getClass().getMethod("getContent");
            OpContent content = (OpContent) getContentMethod.invoke(object);
            Method getLocationMethod = object.getClass().getMethod("getLocation");
            String location = (String) getLocationMethod.invoke(object);
            // <FIXME>

            //multi-user means remote
            if (OpEnvironmentManager.isMultiUser()) {
               response.setArgument(ATTACHMENT_URL, location);
               response.setArgument(CONTENT_ID, OpLocator.locatorString(content));
            }
            else {
               String temporaryFileUrl = OpProjectDataSetFactory.createTemporaryAttachment(location, content.getStream(), logger);
               response.setArgument(ATTACHMENT_URL, temporaryFileUrl);
            }

            return response;
         }
         catch (NoSuchMethodException e) {
            logger.error("Cannot access attachment", e);
         }
         catch (IllegalAccessException e) {
            logger.error("Cannot access attachment", e);
         }
         catch (InvocationTargetException e) {
            logger.error("Cannot access attachment", e);
         }

         return null;
      }
      finally {
         broker.close();
      }
   }

   /**
    * From the given content (on the client-side) creates a temporary file on the server.
    * This method is used to open document attachments for the remote case.
    *
    * @param s       a <code>OpProjectSession</code> representing the application session.
    * @param request a <code>XMessage</code> representing the client request.
    * @return an <code>XMessage</code> representing the client response.
    */
   public XMessage createTemporaryFile(OpProjectSession s, XMessage request) {
      Map parameters = (Map) request.getArgument("parameters");
      String contentId = (String) parameters.get(CONTENT);
      String fileName = (String) parameters.get(FILE_NAME);

      if (!OpLocator.validate(contentId) || fileName == null) {
         logger.error("Cannot create temporary file with name:" + fileName + " and content:" + contentId);
         return null;
      }

      XMessage response = new XMessage();
      //multi-user means remote
      if (OpEnvironmentManager.isMultiUser()) {
         response.setArgument(ATTACHMENT_URL, fileName);
      }
      else {
         OpBroker broker = s.newBroker();
         try {
            OpContent content = (OpContent) broker.getObject(contentId);
            String temporaryFileUrl = OpProjectDataSetFactory.createTemporaryAttachment(fileName, content.getStream(), logger);
            response.setArgument(ATTACHMENT_URL, temporaryFileUrl);
         }
         finally {
            broker.close();
         }
      }
      response.setArgument(CONTENT_ID, contentId);

      return response;
   }

   public XMessage insertComment(OpProjectSession session, XMessage request) {
      logger.debug("OpProjectAdministrationService.insertComment()");
      HashMap comment_data = (HashMap) (request.getArgument(COMMENT_DATA));

      XMessage reply = new XMessage();
      XError error;

      // Check mandatory input fields
      String commentName = (String) comment_data.get(OpActivityComment.NAME);
      if (commentName == null || commentName.length() == 0) {
         error = session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.COMMENT_NAME_MISSING);
         reply.setError(error);
         return reply;
      }

      OpBroker broker = session.newBroker();
      try {
         String activityLocator = (String) comment_data.get(ACTIVITY_ID);
         OpActivityIfc av = (OpActivityIfc) broker.getObject(activityLocator);
         // TODO: check, which activity to comment ;-)
         
         if (av == null || av.getElementForActualValues() == null) {
            error = session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.COMMENT_NOT_FOUND);
            reply.setError(error);
            return reply;
         }

         OpActivity act = av.getActivityForAdditionalObjects();
         // Check contributor access to activity
         if (!session.checkAccessLevel(broker, act.getProjectPlan().getProjectNode().getId(), OpPermission.CONTRIBUTOR)) {
            logger.warn("ERROR: Insert access to activity denied; ID = " + act.getId());
            reply.setError(session.newError(PROJECT_ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
            return reply;
         }

         OpTransaction t = broker.newTransaction();

         // Query max-sequence
         OpQuery query = broker.newQuery("select max(comment.Sequence) from OpActivityComment as comment where comment.Activity.id = ?");
         query.setLong(0, act.getId());
         Iterator result = broker.iterate(query);
         int sequence = 1;
         if (result.hasNext()) {
            Integer maxSequence = (Integer) result.next();
            if (maxSequence != null) {
               sequence = maxSequence.intValue() + 1;
            }
         }

         OpActivityComment comment = new OpActivityComment();
         comment.setName(commentName);
         comment.setSequence(sequence);
         comment.setText((String) comment_data.get(OpActivityComment.TEXT));
         comment.setCreator(session.user(broker));
         comment.setActivity(act);
         broker.makePersistent(comment);

         // Update activity.Attributes in order that activity has comments and all of its versions
         if ((act.getAttributes() & OpActivity.HAS_COMMENTS) == 0) {
            act.setAttributes(act.getAttributes() + OpActivity.HAS_COMMENTS);
            broker.updateObject(act);
         }

         t.commit();

         XLanguageResourceMap resourceMap = session.getLocale().getResourceMap(OpEditActivityFormProvider.PROJECT_EDIT_ACTIVITY);
         boolean canDelete = session.checkAccessLevel(broker, av.getProjectPlan().getProjectNode().getId(), OpPermission.ADMINISTRATOR);
         reply = createActivityCommentPanel(session, comment, resourceMap, canDelete);

         logger.debug("/OpProjectAdministrationService.insertComment()");
         return reply;
      }
      finally {
         broker.close();
      }
   }

   public XMessage deleteComment(OpProjectSession session, XMessage request) {
      String comment_id = (String) request.getArgument(COMMENT_ID);
      logger.debug("OpProjectAdministrationService.deleteComments(): comment_ids = " + comment_id);

      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();
      try {
         OpActivityComment comment = (OpActivityComment) broker.getObject(comment_id);

         if (comment == null) {
            reply.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.COMMENT_NOT_FOUND));
            return reply;
         }

         OpActivity activity = comment.getActivity();
         // Check administrator access to activity
         if (!session.checkAccessLevel(broker, activity.getProjectPlan().getProjectNode().getId(), OpPermission.ADMINISTRATOR)) {
            logger.warn("ERROR: Administrator access to activity denied; ID = " + activity.getId());
            reply.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.ADMINISTRATOR_ACCESS_DENIED));
            return reply;
         }

         OpTransaction t = broker.newTransaction();

         OpQuery query = broker.newQuery("select comment from OpActivityComment as comment where comment.id != (:commentId) and comment.Activity.id = (:activityId) order by comment.Sequence");
         query.setLong("commentId", comment.getId());
         query.setLong("activityId", activity.getId());
         List comments = broker.list(query);
         //resource map
         XLanguageResourceMap resourceMap = session.getLocale().getResourceMap(OpEditActivityFormProvider.PROJECT_EDIT_ACTIVITY);
         //the new comments panel
         XComponent commentsPanel = new XComponent(XComponent.PANEL);

         for (Object comment1 : comments) {
            OpActivityComment activityComment = (OpActivityComment) comment1;
            int sequence = activityComment.getSequence();
            if (sequence > comment.getSequence()) {
               activityComment.setSequence(--sequence);
            }
            reply = createActivityCommentPanel(session, activityComment, resourceMap, true);
            commentsPanel.addChild((XComponent) reply.getArgument(ACTIVITY_COMMENT_PANEL));
         }
         //set up reply needed args
         if (comments.isEmpty()) {
            //mark activity flag becouse the last comment was deleted
            int activityAtributes = activity.getAttributes();
            activity.setAttributes(activityAtributes ^ OpActivity.HAS_COMMENTS);
            broker.updateObject(activity);

            //update all the versions of the activity
            StringBuffer commentsBuffer = new StringBuffer();
            commentsBuffer.append(comments.size());
            commentsBuffer.append(' ');
            commentsBuffer.append(resourceMap.getResource("CommentsSoFar").getText());
            reply.setArgument(COMMENTS_LABEL_TEXT, commentsBuffer.toString());
         }
         reply.setArgument(ACTIVITY_COMMENTS_PANEL, commentsPanel);

         //finnaly delete comment
         broker.deleteObject(comment);

         t.commit();
      }
      finally {
         broker.close();
      }
      logger.debug("/OpProjectAdministrationService.deleteComments()");
      return reply;
   }

   public XMessage moveProjectPlanStartDate(OpProjectSession session,
        OpBroker broker, OpProjectPlan projectPlan, Date newDate, Date oldDate) {
      XMessage reply = new XMessage();
      long newDateMillis = newDate.getTime();

      //check out the current project plan
      String projectId = projectPlan.getProjectNode().locator();
      XMessage editActivitiesRequest = new XMessage();
      editActivitiesRequest.setArgument(PROJECT_ID, projectId);

      OpProjectPlanVersion wv = projectPlan.getWorkingVersion();
      OpProjectPlanVersion pv = null;
      if (wv == null) {
         reply = this.internalEditActivities(session, broker, editActivitiesRequest);
         if (reply != null && reply.getError() != null) {
            //we don't want to stop when warnings are issued
            if (reply.getArgument(WARNING_ARGUMENT) != null) {
               logger.warn(reply.getError().getMessage());
               reply.setError(null);
            }
            else {
               return reply;
            }
         }
         pv = projectPlan.getLatestVersion();
         wv = OpActivityVersionDataSetFactory.getInstance()
               .createProjectPlanVersionObject(session, broker, projectPlan,
                     session.user(broker), OpProjectPlan.WORKING_VERSION_NUMBER);
         projectPlan.setWorkingVersion(wv);
      }
      else {
         pv = wv;
      }

      if (pv == null) {
         return reply;
      }
      
      //attach the project plan with a new session
      projectPlan = (OpProjectPlan) broker.getObject(projectPlan.locator());
      if (oldDate == null) {
         oldDate = projectPlan.getProjectNode().getStart();
      }
      long oldDateMillis = oldDate.getTime();
      XComponent newDataSet = createdShiftedActivityDataSetFromPlanVersion(session, broker, pv, newDateMillis - oldDateMillis);

      // Store working copy as project plan version
      HashMap resourceMap = OpActivityDataSetFactory.resourceMap(broker, projectPlan.getProjectNode());
      OpActivityVersionDataSetFactory.getInstance()
            .storeActivityVersionDataSet(session, broker, newDataSet,
                  projectPlan.getWorkingVersion(), resourceMap,
                  pv, false);

      Timestamp now = new Timestamp(System.currentTimeMillis());
      projectPlan.getWorkingVersion().setRecalculated(now);
      
      return reply;
   }

   /**
    * Revalidates all the working project plan versions.
    *
    * @param session a <code>OpProjectSession</code> the server session.
    * @param request a <code>XMessage</code> the client request.
    * @return a <code>XMessage</code> the response.
    */
   public XMessage revalidateWorkingVersions(OpProjectSession session, XMessage request) {
      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      try {
         t = broker.newTransaction();
         OpQuery query = broker.newQuery("from OpProjectPlan  projectPlan");
         Iterator it = broker.iterate(query);
         while (it.hasNext()) {
            OpProjectPlan projectPlan = (OpProjectPlan) it.next();
            OpProjectPlanValidator.getInstance().validateProjectPlanWorkingVersion(session, broker, projectPlan, null, false);
         }
         t.commit();
      }
      finally {
         if (t != null) {
            t.rollbackIfNecessary();
         }
         broker.close();
      }
      return null;
   }

   /**
    * Shifts the start dates and revalidates the working version of the given project plan.
    *
    * @param projectPlan      a <code>OpProjecPlan</code> representing a project plan.
    * @param broker           a <code>OpBroker</code> used for business operations.
    * @param millisDifference a <code>long</code> which will be the difference in milliseconds between the new and the old start date.
    * @return a <code>XComponent(DATA_SET)</code> reperesenting the client-representation of the new working plan.
    */
   private XComponent createAndValidateShiftedVersion(OpProjectPlan projectPlan, OpProjectSession session, OpBroker broker, long millisDifference) {
      OpProjectNode projectNode = projectPlan.getProjectNode();
      logger.info("Revalidating working plan for " + projectNode.getName());
      OpProjectPlanVersion planVersion = projectPlan.getLatestVersion();
      
      XComponent shiftedDataSet = createdShiftedActivityDataSetFromPlanVersion(
            session, broker, planVersion, millisDifference);
      return shiftedDataSet;
   }

   private XComponent createdShiftedActivityDataSetFromPlanVersion(
         OpProjectSession session, OpBroker broker,
         OpProjectPlanVersion planVersion, long moveMilliSeconds) {
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpActivityVersionDataSetFactory.getInstance().retrieveActivityVersionDataSet(session, broker, planVersion, dataSet, false);
      OpGanttValidator validator = OpProjectPlanValidator.getInstance().createValidator(session, broker, planVersion.getProjectPlan());
      validator.setDataSet(dataSet);
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent activityRow = (XComponent) dataSet.getChild(i);
         if (OpGanttValidator.importedActivity(activityRow)) {
            continue;
         }
         //update the start
         java.sql.Date originalStart = OpGanttValidator.getStart(activityRow);
         //start can be null for certain activities
         if (originalStart != null) {
            long newStartTime = originalStart.getTime() + moveMilliSeconds;
            OpGanttValidator.setStart(activityRow, new java.sql.Date(newStartTime));
         }
         validator.updateDuration(activityRow, OpGanttValidator.getDuration(activityRow), true);
      }
      validator.validateEntireDataSet();
      return validator.getDataSet();
   }

   /**
    * Returns an <code>XMessage</code> containing as an argument a <code>XComponent.PANEL</code> with the created comment
    *
    * @param session               <code>OpProjectSession</code>
    * @param comment               <code>OpActivityComment</code> entity which is persisted
    * @param resourceMap           <code>XLanguageResourceMap</code> representing the edit activity language resource map
    * @param enableCommentRemoving <code>boolean</code> flag indicating that the comment remove button will be enabled or not
    * @return a <code>XMessage</code>
    */
   private XMessage createActivityCommentPanel(OpProjectSession session, OpActivityComment comment, XLanguageResourceMap resourceMap, boolean enableCommentRemoving) {

      XMessage reply = new XMessage();

      //use localizer to localize name of administrator
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionDataSetFactory.USER_OBJECTS));

      //number of comments for activity
      int commentsCount = comment.getSequence();

      //create the comment panel
      XComponent commentPanel = OpEditActivityFormProvider.createPanel(comment, resourceMap, localizer, enableCommentRemoving, session.getCalendar());

      StringBuffer commentsBuffer = new StringBuffer();
      commentsBuffer.append(commentsCount);
      commentsBuffer.append(' ');
      if (commentsCount == 1) {
         commentsBuffer.append(resourceMap.getResource("CommentSoFar").getText());
      }
      else {
         commentsBuffer.append(resourceMap.getResource("CommentsSoFar").getText());
      }

      //set up reply args
      reply.setArgument(COMMENTS_LABEL_TEXT, commentsBuffer.toString());
      reply.setArgument(ACTIVITY_COMMENT_PANEL, commentPanel);

      return reply;
   }

   /**
    * Filters an activity based on a list of excluded activity types.
    *
    * @param checkedActivity - the <code>OpActivity</code> whose type is checked
    * @param excludedTypes   -  the <code>List</code> of exluded activity types
    * @return - <code>true</code> if the type of the activity passed as parameter does not belong to the list of excluded
    *         types and false otherwise.
    */
   private boolean isActivityExcluded(OpActivityIfc checkedActivity, List<Byte> excludedTypes) {
      for (Byte type : excludedTypes) {
         if (checkedActivity.getType() == type) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns a <code>XLocale</code> based on the import/export language user preference or the default session locale
    * if the preference is not set.
    *
    * @param session the <code>OpProjectSession</code> object
    * @return a <code>XLocale</code> based on the import/export language user preference or the default session locale
    *         if the preference is not set.
    */
   private XLocale getImportExportLocale(OpProjectSession session) {
      XLocale locale = session.getLocale();
      OpPreferencesService preferencesService = OpPreferencesService.getService();
      String exportPreference = preferencesService.getPreference(session, OpPreference.IMPORT_EXPORT_LANGUAGE);
      if (exportPreference != null) {
         if (exportPreference.contains("_")) {
            String language = exportPreference.substring(0, exportPreference.indexOf("_"));
            String country = exportPreference.substring(exportPreference.indexOf("_") + 1, exportPreference.length());
            locale = new XLocale(language, country, null);
         }
         else {
            locale = new XLocale(exportPreference, null, null);
         }
      }
      return locale;
   }
}
