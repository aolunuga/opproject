/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning;

import onepoint.express.XComponent;
import onepoint.express.server.XExpressSession;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.mail.OpMailMessage;
import onepoint.project.modules.mail.OpMailer;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.forms.OpEditActivityFormProvider;
import onepoint.project.modules.project_planning.msproject.OpMSProjectManager;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpLock;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocale;
import onepoint.resource.XLocalizer;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.util.XEnvironmentManager;
import onepoint.util.XEncodingHelper;

import javax.mail.internet.AddressException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author : mihai.costin
 */
public class OpProjectPlanningService extends OpProjectService {

   public final static String SERVICE_NAME = "PlanningService";

   public final static String PROJECT_ID = "project_id";
   public final static String ACTIVITY_ID = "activity_id";
   public final static String ACTIVITY_SET = "activity_set";
   public final static String WORKING_PLAN_VERSION_ID = "working_plan_version_id";

   private final static String EDIT_MODE = "edit_mode";
   private final static String BYTES_ARRAY_FIELD = "bytes_array";
   private final static String FILE_NAME_FIELD = "file_name";
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
   private final static OpProjectPlanningErrorMap PLANNING_ERROR_MAP = new OpProjectPlanningErrorMap();
   private final static OpProjectErrorMap PROJECT_ERROR_MAP = new OpProjectErrorMap();

   private static final XLog logger = XLogFactory.getLogger(OpProjectPlanningService.class, true);

   public XMessage importActivities(OpProjectSession session, XMessage request) {

      String projectId = (String) (request.getArgument(PROJECT_ID));
      boolean editMode = (Boolean) (request.getArgument(EDIT_MODE));
      byte[] file = (byte[]) (request.getArgument(BYTES_ARRAY_FIELD));

      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();
      OpProjectNode project = (OpProjectNode) (broker.getObject(projectId));
      OpProjectPlan projectPlan = project.getPlan();

      if (OpProjectAdministrationService.hasWorkRecords(project, broker)) {
         broker.close();
         throw new OpProjectPlanningException(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.IMPORT_ERROR_WORK_RECORDS_EXIST));
      }

      InputStream inFile = new ByteArrayInputStream(file);
      XComponent dataSet;
      try {
         dataSet = OpMSProjectManager.importActivities(inFile, projectPlan, session.getLocale());
      }
      catch (IOException e) {
         reply.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.MSPROJECT_FILE_READ_ERROR));
         return reply;
      }

      //edit if !edit_mode
      if (!editMode) {
         reply = internalEditActivities(session, request);
         if (reply.getError() != null) {
            return reply;
         }
      }

      //save
      request.setArgument(ACTIVITY_SET, dataSet);
      reply = saveActivities(session, request);
      if (reply != null && reply.getError() != null) {
         return reply;
      }

      //check in if !edit_mode
      if (!editMode) {
         reply = internalCheckInActivities(session, request);
         if (reply != null && reply.getError() != null) {
            return reply;
         }
      }

      return reply;
   }

   public XMessage exportActivities(OpProjectSession session, XMessage request) {

      XComponent activitySet = (XComponent) request.getArgument(ACTIVITY_SET);
      String fileName = (String) (request.getArgument(FILE_NAME_FIELD));
      XMessage response = new XMessage();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try {
         fileName = OpMSProjectManager.exportActivities(fileName, out, activitySet, session.getLocale());
      }
      catch (IOException e) {
         response.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.MSPROJECT_FILE_WRITE_ERROR));
         return null;
      }

      byte[] outArray = out.toByteArray();
      response.setArgument(BYTES_ARRAY_FIELD, outArray);
      response.setArgument(FILE_NAME_FIELD, fileName);
      return response;
   }

   private XMessage setEditLock(OpBroker broker, OpProjectSession session, OpProjectNode project) {
      // *** Set lock on project for current user (insert lock, update project)
      logger.debug("begin lock");
      OpTransaction t = broker.newTransaction();
      OpLock lock = new OpLock();
      lock.setOwner(session.user(broker));
      lock.setTarget(project);
      broker.makePersistent(lock);
      // *** TODO: Use session.getCalendar().now() for setCreated()
      logger.debug("locked");
      XMessage reply = checkExternalModifications(project, session, broker);
      t.commit();
      return reply;
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
   private XMessage internalEditActivities(OpProjectSession session, XMessage request) {
      OpBroker broker = null;
      try {
         // Set persistent lock for current user (working project plan version is created on first save)
         String project_id_string = (String) (request.getArgument(PROJECT_ID));
         broker = session.newBroker();
         OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string));

         // Check manager access
         if (!session.checkAccessLevel(broker, project.getID(), OpPermission.MANAGER)) {
            logger.warn("ERROR: Udpate access to project denied; ID = " + project_id_string);
            broker.close();
            throw new OpProjectPlanningException(session.newError(PROJECT_ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
         }

         // *** Check if lock is already set -- if yes throw exception
         if (project.getLocks().size() > 0) {
            logger.error("Project is already locked");
            broker.close();
            throw new OpProjectPlanningException(session.newError(PROJECT_ERROR_MAP, OpProjectError.PROJECT_LOCKED_ERROR));
         }
         XMessage reply = setEditLock(broker, session, project);

         broker.close();
         return reply;
      }
      finally {
         finalizeSession(null, broker);
      }
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
      boolean hasAvailabilityChanged = false;
      boolean haveRatesChanged = false;

      Set activities = project.getPlan().getActivities();
      if (activities != null) {
         Iterator activitiesIt = activities.iterator();
         while (activitiesIt.hasNext()) {
            OpActivity activity = (OpActivity) activitiesIt.next();
            if (!activity.getDeleted()) {
               hasAvailabilityChanged |= checkResourceAvailabilityModifications(activity, broker);
               haveRatesChanged |= checkHourlyRateModifications(activity, broker);
            }
         }

         if (hasAvailabilityChanged && haveRatesChanged) {
            result.setArgument(WARNING_ARGUMENT, Boolean.TRUE);
            result.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.AVAILIBILITY_AND_RATES_MODIFIED_WARNING));
            return result;
         }
         if (hasAvailabilityChanged) {
            result.setArgument(WARNING_ARGUMENT, Boolean.TRUE);
            result.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.AVAILIBILITY_MODIFIED_WARNING));
            return result;
         }
         if (haveRatesChanged) {
            result.setArgument(WARNING_ARGUMENT, Boolean.TRUE);
            result.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.HOURLY_RATES_MODIFIED_WARNING));
            return result;
         }
      }
      return result;
   }

   /**
    * Checks whether resource availibility has changed for an activity.
    *
    * @param activity a <code>OpActivity</code> representing an activity.
    * @param broker   a <code>OpBroker</code> used to perform business operations.
    * @return <code>true</code> if the availibility has changed for any of the resources of the activity.
    */
   private boolean checkResourceAvailabilityModifications(OpActivity activity, OpBroker broker) {
      if (activity.getAssignments().size() == 0) {
         return false;
      }
      boolean changed = false;
      Set assignments = activity.getAssignments();
      if (assignments != null) {
         Iterator it = assignments.iterator();
         while (it.hasNext()) {
            OpAssignment assignment = (OpAssignment) it.next();
            if (assignment.getAssigned() > assignment.getResource().getAvailable()) {
               changed = true;
               assignment.setAssigned(assignment.getResource().getAvailable());
               broker.updateObject(assignment);
            }
         }
      }
      return changed;
   }

   /**
    * Checks whether the hourly rate has changed for the given activity.
    *
    * @param activity a <code>OpActivity</code> representing an activity.
    * @return <code>true</code> if the hourly rate has changed for any of the resources of the activity.
    */
   private boolean checkHourlyRateModifications(OpActivity activity, OpBroker broker) {
      if (activity.getAssignments().size() == 0) {
         return false;
      }

      boolean changed = false;
      double currentPersonnelCosts = activity.getBasePersonnelCosts();

      double recalculatedPersonnelCosts = 0;
      Iterator it1 = activity.getAssignments().iterator();
      while (it1.hasNext()) {
         OpAssignment assignment = ((OpAssignment) it1.next());
         recalculatedPersonnelCosts += assignment.getBaseEffort() * assignment.getResource().getHourlyRate();
      }

      if (Math.abs(recalculatedPersonnelCosts - currentPersonnelCosts) >= OpGanttValidator.ERROR_MARGIN) {
         changed = true;
         activity.setBasePersonnelCosts(recalculatedPersonnelCosts);
         broker.updateObject(activity);

         //update all super activities
         while (activity.getSuperActivity() != null) {
            OpActivity superActivity = activity.getSuperActivity();
            double costsDifference = activity.getBasePersonnelCosts() - currentPersonnelCosts;
            currentPersonnelCosts = superActivity.getBasePersonnelCosts();
            superActivity.setBasePersonnelCosts(superActivity.getBasePersonnelCosts() + costsDifference);
            broker.updateObject(activity);
            activity = superActivity;
         }
         broker.updateObject(activity);
      }

      return changed;
   }

   /**
    * Saves the given activity set. Will serialize the activity set and set it as the plan for the working version.
    *
    * @param session The session used
    * @param request The request that contains the parameters required by the save process
    * @return A message that will contain error messages if something went wrong
    */
   public XMessage saveActivities(OpProjectSession session, XMessage request) {
      OpBroker broker = null;
      try {
         logger.debug("OpProjectAdministrationService.saveActivities");
         String project_id_string = (String) (request.getArgument(PROJECT_ID));
         if ((project_id_string == null) || (project_id_string.trim().length() == 0)) {
            return null;
         }
         String workingPlanVersionLocator = (String) request.getArgument(WORKING_PLAN_VERSION_ID);
         XComponent dataSet = (XComponent) (request.getArgument(ACTIVITY_SET));

         logger.debug("SAVE-ACTIVITIES " + dataSet.getChildCount());

         broker = session.newBroker();
         OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string));

         // *** Check if current user has lock on project
         if (project.getLocks().size() == 0) {
            logger.error("Project is currently not being edited");
            broker.close();
            throw new OpProjectPlanningException(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.PROJECT_CHECKED_IN_ERROR));
         }
         OpLock lock = (OpLock) project.getLocks().iterator().next();
         if (lock.getOwner().getID() != session.getUserID()) {
            logger.error("Project is locked by another user");
            broker.close();
            throw new OpProjectPlanningException(session.newError(PROJECT_ERROR_MAP, OpProjectError.PROJECT_LOCKED_ERROR));
         }


         OpTransaction t = broker.newTransaction();

         // Check if project plan already exists (create if not)
         OpProjectPlan projectPlan = project.getPlan();
         if (projectPlan == null) {
            projectPlan = new OpProjectPlan();
            projectPlan.setProjectNode(project);
            projectPlan.setStart(project.getStart());
            projectPlan.setFinish(project.getFinish());
            projectPlan.setTemplate(project.getType() == OpProjectNode.TEMPLATE);
            broker.makePersistent(projectPlan);
         }

         // Check if working plan version ID is correct (if it is set)
         boolean fromProjectPlan = true;
         OpProjectPlanVersion workingPlanVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker,
              projectPlan, OpProjectAdministrationService.WORKING_VERSION_NUMBER);
         if (workingPlanVersionLocator != null) {
            // It is important that fromProjectPlan is checked against workingPlanVersionLocator parameter
            // (And not against existing working plan version in database, because client view might not have been
            // reloaded)
            fromProjectPlan = false;
            if ((workingPlanVersion != null)
                 && (workingPlanVersion.getID() != OpLocator.parseLocator(workingPlanVersionLocator).getID())) {
               // TODO: Send INTERNAL_ERROR (should not happen during normal circumstances)?
               finalizeSession(t, broker);
               return null;
            }
         }
         // Create new working plan version object if it does not already exist
         if (workingPlanVersion == null) {
            workingPlanVersion = OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, projectPlan, session
                 .user(broker), OpProjectAdministrationService.WORKING_VERSION_NUMBER, false);
         }

         // Store working copy as project plan version
         HashMap resourceMap = OpActivityDataSetFactory.resourceMap(broker, project);
         OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, workingPlanVersion, resourceMap,
              fromProjectPlan);

         t.commit();
         broker.close();

         logger.debug("/OpProjectAdministrationService.saveActivities");
         return null;
      }
      finally {
         finalizeSession(null, broker);
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
      logger.debug("OpProjectAdministrationService.checkInActivities");
      return internalCheckInActivities(session, request);
   }

   /**
    * Check in activities used by the service internally.
    *
    * @see #checkInActivities(onepoint.project.OpProjectSession,onepoint.service.XMessage)
    */
   private XMessage internalCheckInActivities(OpProjectSession session, XMessage request) {
      OpBroker broker = null;
      try {
         String project_id_string = (String) (request.getArgument(PROJECT_ID));
         String workingPlanVersionLocator = (String) request.getArgument(WORKING_PLAN_VERSION_ID);
         XComponent dataSet = (XComponent) (request.getArgument(ACTIVITY_SET));
         broker = session.newBroker();
         OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string));

         if (project == null) {
            broker.close();
            throw new OpProjectPlanningException(session.newError(PROJECT_ERROR_MAP, OpProjectError.PROJECT_NOT_FOUND));
         }

         // Check if project is locked and current user owns the lock
         if (project.getLocks().size() == 0) {
            logger.error("Project is currently not being edited");
            broker.close();
            throw new OpProjectPlanningException(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.PROJECT_CHECKED_IN_ERROR));
         }
         OpLock lock = (OpLock) project.getLocks().iterator().next();
         if (lock.getOwner().getID() != session.getUserID()) {
            logger.error("Project is locked by another user");
            broker.close();
            throw new OpProjectPlanningException(session.newError(PROJECT_ERROR_MAP, OpProjectError.PROJECT_LOCKED_ERROR));
         }

         OpTransaction t = broker.newTransaction();

         // Check if project plan already exists (create if not)
         OpProjectPlan projectPlan = project.getPlan();

         // Archive current project plan to new project plan version
         OpQuery query = broker.newQuery("select max(planVersion.VersionNumber) from OpProjectPlanVersion as planVersion where planVersion.ProjectPlan.ProjectNode.ID = ?");
         query.setLong(0, project.getID());
         Integer maxVersionNumber = (Integer) broker.iterate(query).next();
         //first version for project plan
         int versionNumber = 1;
         // a version exists and it's not WORKING VERSION NUMBER
         if (maxVersionNumber != null &&
              maxVersionNumber.intValue() != OpProjectAdministrationService.WORKING_VERSION_NUMBER) {
            versionNumber = maxVersionNumber.intValue() + 1;
         }
         OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, projectPlan, session.user(broker), versionNumber, true);

         // Check if working plan version ID is correct (if it is set)
         OpProjectPlanVersion workingPlanVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, projectPlan, OpProjectAdministrationService.WORKING_VERSION_NUMBER);
         if (workingPlanVersionLocator != null) {
            if ((workingPlanVersion != null)
                 && (workingPlanVersion.getID() != OpLocator.parseLocator(workingPlanVersionLocator).getID())) {
               // TODO: Send INTERNAL_ERROR (should not happen during normal circumstances)?
               finalizeSession(t, broker);
               return null;
            }
         }

         // Update project plan from client data-set ONLY if working plan version LOCATOR is set
         // (Working plan version might exist, but client-side data-set might still be an activity set -- not reloaded)
         HashMap resourceMap = OpActivityDataSetFactory.resourceMap(broker, project);
         if (workingPlanVersionLocator != null) {
            OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, resourceMap, projectPlan, workingPlanVersion);
         }
         else {
            OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, resourceMap, projectPlan, null);
         }

         // Delete working version (note: There is not necessarily a working version)
         if (workingPlanVersion != null) {
            OpActivityVersionDataSetFactory.deleteProjectPlanVersion(broker, workingPlanVersion);
         }

         // 4. Finally, delete lock
         broker.deleteObject(lock);

         // Send email notification to all project participants on project plan check-in
         sendProjectNotification(session, project.getName(), resourceMap);

         t.commit();
         broker.close();
         return null;
      }
      finally {
         finalizeSession(null, broker);
      }
   }

   public XMessage revertActivities(OpProjectSession session, XMessage request) {
      logger.debug("OpProjectAdministrationService.revertActivities");
      OpBroker broker = null;
      try {
         String project_id_string = (String) (request.getArgument(PROJECT_ID));
         broker = session.newBroker();
         OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string));

         // Check if project is locked and current user owns the lock
         if (project.getLocks().size() == 0) {
            logger.error("Project is currently not being edited");
            broker.close();
            // TODO: Error handling
            return null;
         }
         OpLock lock = (OpLock) project.getLocks().iterator().next();
         if (lock.getOwner().getID() != session.getUserID()) {
            logger.error("Project is locked by another user");
            broker.close();
            // TODO: Error handling
            return null;
         }

         OpTransaction t = broker.newTransaction();

         // Check if project plan exists: If yes we have to delete a potential working version
         OpProjectPlan projectPlan = project.getPlan();
         if (projectPlan != null) {

            // Try to retrieve working plan version
            OpProjectPlanVersion workingPlanVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker,
                 projectPlan, OpProjectAdministrationService.WORKING_VERSION_NUMBER);

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
         finalizeSession(null, broker);
      }
      return null;
   }

   private void sendProjectNotification(OpProjectSession session, String projectName, Map resourceMap) {
      OpMailMessage message = new OpMailMessage();

      // Add users email as cc to mail message
      for (Iterator iterator = resourceMap.values().iterator(); iterator.hasNext();) {
         OpResource resource = (OpResource) iterator.next();
         OpUser user = resource.getUser();
         if (user != null) {
            String email = user.getContact().getEMail();
            if (email != null && email.length() > 0) {
               try {
                  message.addCC(email, user.getDisplayName());
               }
               catch (UnsupportedEncodingException e) {
                  logger.warn("Could not add email '" + email + "' for user '" + user.getDisplayName() + "'", e);
               }
            }
         }
      }
      /*no available users to send email */
      if (!message.getCCs().hasNext()) {
         return;
      }
      /*get configuration form address  */
      String fromEmailAddress = OpSettings.get(OpSettings.EMAIL_NOTIFICATION_FROM_ADDRESS);
      try {
         message.setFrom(fromEmailAddress);
      }
      catch (AddressException e) {
         logger.warn("Could not add from email " + fromEmailAddress, e);
      }

      /*default mail subject and body */
      String mail_subject = "Project update";
      String mail_body = "Project plan for project '$ProjectName$' has been changed!";

      // load language resources for body and subject of email
      XLocale locale = ((XExpressSession) session).getLocale();
      XLanguageResourceMap resource_map = locale.getResourceMap(TEMPLATE_MAP);
      if (resource_map != null) {
         mail_subject = resource_map.getResource("NotificationMailSubject").getText();
         mail_body = resource_map.getResource("NotificationMailBody").getText();
      }

      message.setSubject(mail_subject);
      projectName = projectName.replace('\\', '/');
      /*alternative in jdk5 replace(CharSequence target,CharSequence replacement) */
      mail_body = mail_body.replaceAll("\\$ProjectName\\$", projectName);
      message.addContent(mail_body);

      OpMailer.sendMessageAsynchronous(message);
   }

   public XMessage prepareAttachment(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(ATTACHMENT_ID));

      OpBroker broker = session.newBroker();

      OpObject object = broker.getObject(id_string);
      if (object == null) {
         logger.warn("Could not find attachment with ID " + id_string);
         broker.close();
         return null;
      }

      XMessage response = new XMessage();
      if (!session.checkAccessLevel(broker, object.getID(), OpPermission.OBSERVER)) {
         response.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.INSUFICIENT_ATTACHMENT_PERMISSIONS));
         return response;
      }

      try {

         // <FIXME author="Horia Chiorean" description="This code is here because we can have either OpAttachment or
         // OpAttachmentVersion">
         Method getContentMethod = object.getClass().getMethod("getContent", null);
         OpContent content = (OpContent) getContentMethod.invoke(object, null);
         Method getLocationMethod = object.getClass().getMethod("getLocation", null);
         String location = (String) getLocationMethod.invoke(object, null);
         // <FIXME>

         //multi-user means remote
         if (OpInitializer.isMultiUser()) {
            response.setArgument(ATTACHMENT_URL, location);
            response.setArgument(CONTENT_ID, OpLocator.locatorString(content));
         }
         else {
            String temporaryFileUrl = createTemporaryAttachment(location, content.getBytes());
            response.setArgument(ATTACHMENT_URL, temporaryFileUrl);
         }

         broker.close();
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
      byte[] content = (byte[]) parameters.get(CONTENT);
      String fileName = (String) parameters.get(FILE_NAME);

      if (content == null || fileName == null) {
         logger.error("Cannot create temporary file with name:" + fileName + " and content:" + content);
         return null;
      }

      String temporaryFileUrl = createTemporaryAttachment(fileName, content);
      XMessage response = new XMessage();
      response.setArgument(ATTACHMENT_URL, temporaryFileUrl);
      return response;
   }

   /**
    * Creates a temporary file with the content of an attachment.
    *
    * @param location a <code>String</code> representing the location of the real attachment object.
    * @param content  <code>byte[]</code> representing the content of the attachment.
    * @return a <code>String</code> representing an URL-like path to a temporary file that has the same content as the
    *         attachment.
    */
   private String createTemporaryAttachment(String location, byte[] content) {
      int extensionIndex = location.lastIndexOf(".");
      String prefix = location;
      String suffix = null;
      if (extensionIndex != -1) {
         prefix = location.substring(0, extensionIndex);
         suffix = location.substring(extensionIndex, location.length());
      }
      if (prefix.length() < 3) {
         prefix = "file" + prefix;
      }

      try {
         File temporaryFile = File.createTempFile(prefix, suffix, new File(XEnvironmentManager.TMP_DIR));
         FileOutputStream fos = new FileOutputStream(temporaryFile);
         fos.write(content);
         fos.flush();
         fos.close();
         return XEncodingHelper.encodeValue(temporaryFile.getName());
      }
      catch (IOException e) {
         logger.error("Cannot create temporary attachment file on server", e);
      }
      return null;
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

      String activityLocator = (String) comment_data.get(ACTIVITY_ID);
      OpActivity activity = (OpActivity) broker.getObject(activityLocator);
      if (activity == null) {
         broker.close();
         error = session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.COMMENT_NOT_FOUND);
         reply.setError(error);
         return reply;
      }

      // Check contributor access to activity
      if (!session.checkAccessLevel(broker, activity.getProjectPlan().getProjectNode().getID(), OpPermission.CONTRIBUTOR)) {
         logger.warn("ERROR: Insert access to activity denied; ID = " + activity.getID());
         broker.close();
         reply.setError(session.newError(PROJECT_ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      // Query max-sequence
      OpQuery query = broker.newQuery("select max(comment.Sequence) from OpActivityComment as comment where comment.Activity.ID = ?");
      query.setLong(0, activity.getID());
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
      comment.setActivity(activity);
      broker.makePersistent(comment);

      // Update activity.Attributes in order that activity has comments and all of its versions
      if ((activity.getAttributes() & OpActivity.HAS_COMMENTS) == 0) {
         activity.setAttributes(activity.getAttributes() + OpActivity.HAS_COMMENTS);
         broker.updateObject(activity);
         Iterator versions = activity.getVersions().iterator();
         OpActivityVersion version;
         while (versions.hasNext()) {
            version = (OpActivityVersion) versions.next();
            if ((version.getAttributes() & OpActivityVersion.HAS_COMMENTS) == 0) {
               version.setAttributes(version.getAttributes() + OpActivityVersion.HAS_COMMENTS);
               broker.updateObject(version);
            }
         }
      }

      t.commit();

      XLanguageResourceMap resourceMap = session.getLocale().getResourceMap(OpEditActivityFormProvider.PROJECT_EDIT_ACTIVITY);
      boolean canDelete = session.checkAccessLevel(broker, activity.getProjectPlan().getProjectNode().getID(), OpPermission.ADMINISTRATOR);
      reply = createActivityCommentPanel(session, comment, resourceMap, canDelete);

      broker.close();
      logger.debug("/OpProjectAdministrationService.insertComment()");
      return reply;
   }

   public XMessage deleteComment(OpProjectSession session, XMessage request) {
      String comment_id = (String) request.getArgument(COMMENT_ID);
      logger.debug("OpProjectAdministrationService.deleteComments(): comment_ids = " + comment_id);

      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();

      OpActivityComment comment = (OpActivityComment) broker.getObject(comment_id);

      if (comment == null) {
         broker.close();
         reply.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.COMMENT_NOT_FOUND));
         return reply;
      }

      OpActivity activity = comment.getActivity();
      // Check administrator access to activity
      if (!session.checkAccessLevel(broker, activity.getProjectPlan().getProjectNode().getID(), OpPermission.ADMINISTRATOR)) {
         logger.warn("ERROR: Administrator access to activity denied; ID = " + activity.getID());
         broker.close();
         reply.setError(session.newError(PLANNING_ERROR_MAP, OpProjectPlanningError.ADMINISTRATOR_ACCESS_DENIED));
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      OpQuery query = broker.newQuery("select comment from OpActivityComment as comment where comment.ID != (:commentId) and comment.Activity.ID = (:activityId) order by comment.Sequence");
      query.setLong("commentId", comment.getID());
      query.setLong("activityId", activity.getID());
      List comments = broker.list(query);
      //resource map
      XLanguageResourceMap resourceMap = session.getLocale().getResourceMap(OpEditActivityFormProvider.PROJECT_EDIT_ACTIVITY);
      //the new comments panel
      XComponent commentsPanel = new XComponent(XComponent.PANEL);

      Iterator it = comments.iterator();
      while (it.hasNext()) {
         OpActivityComment activityComment = (OpActivityComment) it.next();
         int sequence = activityComment.getSequence();
         if (sequence > comment.getSequence()) {
            activityComment.setSequence(--sequence);
         }
         reply = createActivityCommentPanel(session, activityComment, resourceMap, true);
         commentsPanel.addChild((XComponent) reply.getArgument(ACTIVITY_COMMENT_PANEL));
      }
      //set up reply needed args
      if (comments.isEmpty()) {

         //update all the activity versions
         Set activityVersions = activity.getVersions();
         if (activityVersions != null && activityVersions.size() > 0) {
            for (Iterator versionsIt = activityVersions.iterator(); versionsIt.hasNext();) {
               OpActivityVersion activityVersion = (OpActivityVersion) versionsIt.next();
               activityVersion.setAttributes(activityVersion.getAttributes() ^ OpActivity.HAS_COMMENTS);
               broker.updateObject(activityVersion);
            }
         }

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

      /*finnaly delete comment*/
      broker.deleteObject(comment);

      t.commit();
      broker.close();
      logger.debug("/OpProjectAdministrationService.deleteComments()");
      return reply;
   }

   /**
    * Moves the start date of a project plan, by creating a new version, revalidating it and checking it in.
    *
    * @param session a <code>OpProjectSession</code> representing a server session.
    * @param request a <code>XMessage</code> representing the request.
    * @return a <code>XMessage</code> representing a possible error or <code>null</code> if the operation was successfull.
    *         <FIXME author="Horia Chiorean" description="Possible problem: this method is not atomic">
    */
   public XMessage moveProjectPlanStartDate(OpProjectSession session, XMessage request) {

      OpProjectPlan projectPlan = (OpProjectPlan) request.getArgument("projectPlan");
      Date newDate = (Date) request.getArgument("newDate");
      //create and validate a working version

      String projectIdArg = "project_id";
      String activitySetArg = "activity_set";
      String workingPlanArg = "working_plan_version_id";

      //check out the current project plan
      String projectId = projectPlan.getProjectNode().locator();
      XMessage editActivitiesRequest = new XMessage();
      editActivitiesRequest.setArgument(projectIdArg, projectId);

      XMessage reply = this.internalEditActivities(session, editActivitiesRequest);
      if (reply != null && reply.getError() != null) {
         return reply;
      }

      OpBroker broker = session.newBroker();
      //attach the project plan with a new session
      projectPlan = (OpProjectPlan) broker.getObject(projectPlan.locator());
      OpTransaction tx = broker.newTransaction();

      OpProjectPlanVersion workingVersion = OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, projectPlan, session.user(broker),
           OpProjectAdministrationService.WORKING_VERSION_NUMBER, false);
      String workingPlanLocator = workingVersion.locator();

      XComponent newDataSet = shiftAndValidateWorkingVersion(projectPlan, broker, newDate, workingVersion);

      tx.commit();
      broker.close();

      //check-in the working version
      XMessage checkInRequest = new XMessage();
      checkInRequest.setArgument(activitySetArg, newDataSet);
      checkInRequest.setArgument(projectIdArg, projectId);
      checkInRequest.setArgument(workingPlanArg, workingPlanLocator);

      reply = this.internalCheckInActivities(session, checkInRequest);

      if (reply != null && reply.getError() != null) {
         return reply;
      }
      return null;
   }

   /**
    * Shifts the start dates and revalidates the working version of the given project plan.
    *
    * @param projectPlan    a <code>OpProjecPlan</code> representing a project plan.
    * @param broker         a <code>OpBroker</code> used for business operations.
    * @param newDate        a <code>Date</code> which will be the new start date of the project plan.
    * @param workingVersion a <code>OpProjectPlanVersion</code> representing the working version of the given project plan.
    * @return a <code>XComponent(DATA_SET)</code> reperesenting the client-representation of the new working plan.
    */
   private XComponent shiftAndValidateWorkingVersion(OpProjectPlan projectPlan, OpBroker broker, Date newDate, OpProjectPlanVersion workingVersion) {
      OpProjectNode projectNode = projectPlan.getProjectNode();
      logger.info("Revalidating working plan for " + projectNode.getName());

      //create the validator
      OpGanttValidator validator = new OpGanttValidator();
      validator.setProjectStart(projectPlan.getProjectNode().getStart());
      validator.setProgressTracked(Boolean.valueOf(projectPlan.getProgressTracked()));
      validator.setProjectTemplate(Boolean.valueOf(projectPlan.getTemplate()));
      validator.setCalculationMode(new Byte(projectPlan.getCalculationMode()));

      XComponent resourceDataSet = new XComponent();
      HashMap resources = OpActivityDataSetFactory.resourceMap(broker, projectNode);
      OpActivityDataSetFactory.retrieveResourceDataSet(resources, resourceDataSet);
      validator.setAssignmentSet(resourceDataSet);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, projectPlan, dataSet, false);
      validator.setDataSet(dataSet);
      long millisDifference = newDate.getTime() - projectPlan.getProjectNode().getStart().getTime();
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent activityRow = (XComponent) dataSet.getChild(i);

         //update the start
         java.sql.Date originalStart = OpGanttValidator.getStart(activityRow);
         //start can be null for certain activities
         if (originalStart != null) {
            long newStartTime = originalStart.getTime() + millisDifference;
            OpGanttValidator.setStart(activityRow, new java.sql.Date(newStartTime));
         }
         //set the end to null so that it's recalculated in the validation process
         OpGanttValidator.setEnd(activityRow, null);
      }

      validator.validateDataSet();
      OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, workingVersion, resources, false);
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
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));

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

}
