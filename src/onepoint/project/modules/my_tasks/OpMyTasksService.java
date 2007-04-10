/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpPermission;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.*;

/**
 * Service class for my tasks module.
 *
 * @author mihai.costin
 */
public class OpMyTasksService extends OpProjectService {

   private static final String ADHOC_DATA = "adhocData";
   private static final String MAX_ACTIVITY_SEQUENCE = "select max(activity.Sequence) from OpActivity activity";
   private static final OpMyTasksErrorMap ERROR_MAP = new OpMyTasksErrorMap();

   //arguments names
   private final String ACTIVITY = "activityLocator";
   private final String NAME = "name";
   private final String DESCRIPTION = "description";
   private final String PRIORITY = "priority";
   private final String DUEDATE = "dueDate";
   private final String PROJECT = "projectChoice";
   private final String RESOURCE = "resourceChoice";
   private final String ATTACHMENT_SET = "attachmentSet";

   /**
    * Adds a new ad-hoc tasks based on the given information.
    *
    * @param session an <code>OpProjectSession</code> object - the current session.
    * @param request a <code>XMessage</code> - the request (contains the task data).
    * @return a response in the form of a <code>XMessage</code> object.
    */
   public XMessage addAdhocTask(OpProjectSession session, XMessage request) {

      HashMap arguments = (HashMap) request.getArgument(ADHOC_DATA);
      Map values = new HashMap();

      XMessage reply = checkAdHocValues(session, arguments, values);
      if (reply.getError() != null) {
         return reply;
      }

      OpBroker broker = session.newBroker();
      OpProjectNode project = (OpProjectNode) broker.getObject((String) values.get(PROJECT));
      OpResource resource = (OpResource) broker.getObject((String) values.get(RESOURCE));

      //get activity sequence
      int sequence = 0;
      OpQuery query = broker.newQuery(MAX_ACTIVITY_SEQUENCE);
      Iterator it = broker.list(query).iterator();
      if (it.hasNext()) {
         Integer maxSeq = (Integer) it.next();
         if (maxSeq != null) {
            sequence = maxSeq.intValue() + 1;
         }
      }

      OpTransaction transaction = broker.newTransaction();
      OpActivity adhocTaks = new OpActivity();
      adhocTaks.setType(OpActivity.ADHOC_TASK);
      adhocTaks.setName((String) values.get(NAME));
      adhocTaks.setSequence(sequence);
      adhocTaks.setDescription((String) values.get(DESCRIPTION));
      adhocTaks.setFinish((Date) values.get(DUEDATE));
      adhocTaks.setProjectPlan(project.getPlan());
      adhocTaks.setPriority(((Integer) values.get(PRIORITY)).byteValue());
      broker.makePersistent(adhocTaks);

      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(adhocTaks);
      assignment.setResource(resource);
      assignment.setProjectPlan(adhocTaks.getProjectPlan());
      broker.makePersistent(assignment);

      XComponent attachmentSet = (XComponent) arguments.get(ATTACHMENT_SET);
      adhocTaks.setAttachments(new HashSet());
      updateAttachments(broker, adhocTaks, attachmentSet);

      transaction.commit();

      broker.close();
      return reply;
   }

   private XMessage checkAdHocValues(OpProjectSession session, Map arguments, Map values) {

      XMessage reply = new XMessage();
      String name = (String) arguments.get(NAME);
      String description = (String) arguments.get(DESCRIPTION);
      Integer priority = (Integer) arguments.get(PRIORITY);
      Date dueDate = (Date) arguments.get(DUEDATE);
      String projectChoice = (String) arguments.get(PROJECT);
      String resourceChoice = (String) arguments.get(RESOURCE);

      //task name - mandatory
      if (name == null) {
         reply.setError(session.newError(ERROR_MAP, OpMyTasksError.EMPTY_NAME_ERROR_CODE));
         return reply;
      }
      values.put(NAME, name);

      values.put(DUEDATE, dueDate);

      //project & resource
      if (projectChoice == null) {
         reply.setError(session.newError(ERROR_MAP, OpMyTasksError.NO_PROJECT_ERROR_CODE));
         return reply;
      }
      String projectLocator = XValidator.choiceID(projectChoice);
      values.put(PROJECT, projectLocator);

      if (resourceChoice == null) {
         reply.setError(session.newError(ERROR_MAP, OpMyTasksError.NO_RESOURCE_ERROR_CODE));
         return reply;
      }
      String resourceLocator = XValidator.choiceID(resourceChoice);
      values.put(RESOURCE, resourceLocator);

      //priority (between 0 and 9)
      if (priority == null || priority.intValue() < 1 || priority.intValue() > 9) {
         reply.setError(session.newError(ERROR_MAP, OpMyTasksError.INVALID_PRIORITY_ERROR_CODE));
         return reply;
      }
      values.put(PRIORITY, priority);

      values.put(DESCRIPTION, description);

      return reply;
   }

   /**
    * Updates the given ad-hoc tasks with the given information.
    *
    * @param session an <code>OpProjectSession</code> object - the current session.
    * @param request a <code>XMessage</code> - the request (contains the task data).
    * @return a response in the form of a <code>XMessage</code> object.
    */
   public XMessage updateAdhocTask(OpProjectSession session, XMessage request) {

      HashMap arguments = (HashMap) request.getArgument(ADHOC_DATA);
      Map values = new HashMap();

      XMessage reply = checkAdHocValues(session, arguments, values);
      if (reply.getError() != null) {
         return reply;
      }

      String locator = (String) arguments.get(ACTIVITY);
      OpBroker broker = session.newBroker();
      OpActivity activity = (OpActivity) broker.getObject(locator);

      boolean update = false;
      OpTransaction transaction = broker.newTransaction();

      String name = (String) values.get(NAME);
      if (!name.equals(activity.getName())) {
         activity.setName(name);
         update = true;
      }

      String description = (String) values.get(DESCRIPTION);
      if ((description == null && activity.getDescription() != null) ||
           (description != null && !description.equals(activity.getDescription()))) {
         activity.setDescription(description);
         update = true;
      }

      Integer priority = (Integer) values.get(PRIORITY);
      if (priority.byteValue() != activity.getPriority()) {
         activity.setPriority(priority.byteValue());
         update = true;
      }

      Date dueDate = (Date) values.get(DUEDATE);
      if ((dueDate == null && activity.getFinish() != null) ||
           (dueDate != null && (activity.getFinish() == null || !dueDate.equals(activity.getFinish())))) {
         activity.setFinish(dueDate);
         update = true;
      }

      String projectLocator = (String) values.get(PROJECT);
      if (!activity.getProjectPlan().getProjectNode().locator().equals(projectLocator)) {
         OpProjectNode project = (OpProjectNode) broker.getObject(projectLocator);
         activity.setProjectPlan(project.getPlan());
         update = true;
      }

      String resourceLocator = (String) values.get(RESOURCE);
      Iterator assignments = activity.getAssignments().iterator();
      OpResource resource;
      OpAssignment assignment;
      if (assignments.hasNext()) {
         assignment = (OpAssignment) assignments.next();
         resource = assignment.getResource();
         if (!resourceLocator.equals(resource.locator())) {
            resource = (OpResource) broker.getObject(resourceLocator);
            assignment.setResource(resource);
            broker.updateObject(assignment);
         }
      }

      //update attachments
      XComponent attachmentSet = (XComponent) arguments.get(ATTACHMENT_SET);
      if (updateAttachments(broker, activity, attachmentSet)) {
         update = true;
      }

      if (update) {
         broker.updateObject(activity);
      }

      transaction.commit();
      broker.close();

      return reply;
   }

   private boolean updateAttachments(OpBroker broker, OpActivity activity, XComponent attachmentSet) {

      boolean updated = false;
      Set existingSet = activity.getAttachments();

      //remove deleted attachments
      Map attachmentsRowMap = new HashMap();
      for (int i = 0; i < attachmentSet.getChildCount(); i++) {
         XComponent row = (XComponent) attachmentSet.getChild(i);
         String choice = ((XComponent) row.getChild(1)).getStringValue();
         String locator = XValidator.choiceID(choice);
         if (locator.equals(OpActivityDataSetFactory.NO_CONTENT_ID)) {
            attachmentsRowMap.put(locator + i, row);
         }
         else {
            attachmentsRowMap.put(locator, row);
         }
      }

      for (Iterator iterator = existingSet.iterator(); iterator.hasNext();) {
         OpAttachment attachment = (OpAttachment) iterator.next();
         if (!attachmentsRowMap.keySet().contains(attachment.locator())) {
            broker.deleteObject(attachment);
            updated = true;
         }
         else {
            attachmentsRowMap.remove(attachment.locator());
         }
      }

      for (Iterator iterator = attachmentsRowMap.keySet().iterator(); iterator.hasNext();) {
         String rowKey = (String) iterator.next();
         XComponent row = (XComponent) attachmentsRowMap.get(rowKey);
         ArrayList attachmentElement = new ArrayList();
         String descriptor = ((XComponent) row.getChild(0)).getStringValue();
         attachmentElement.add(descriptor);
         String choice = ((XComponent) row.getChild(1)).getStringValue();
         attachmentElement.add(choice);
         attachmentElement.add(XValidator.choiceCaption(choice)); //name
         attachmentElement.add(((XComponent) row.getChild(2)).getStringValue());
         if (row.getChildCount() > 3) {
            attachmentElement.add(((XComponent) row.getChild(3)).getStringValue()); //content id
         }
         if (row.getChildCount() > 4) {
            attachmentElement.add(((XComponent) row.getChild(4)).getValue()); //content
         }
         OpActivityDataSetFactory.createAttachment(broker, activity, activity.getProjectPlan(), attachmentElement, null);
         updated = true;
      }

      return updated;
   }

   /**
    * Removes the ad-hoc tasks given in the adhocData List.
    *
    * @param session an <code>OpProjectSession</code> object - the current session.
    * @param request a <code>XMessage</code> - the request (contains the task data).
    * @return a response in the form of a <code>XMessage</code> object.
    */
   public XMessage deleteAdhocTask(OpProjectSession session, XMessage request) {
      XMessage reply = new XMessage();
      List selectedRows = (List) request.getArgument(ADHOC_DATA);
      if (selectedRows != null) {
         OpBroker broker = session.newBroker();
         OpTransaction transaction = broker.newTransaction();
         for (int i = 0; i < selectedRows.size(); i++) {
            XComponent row = (XComponent) selectedRows.get(i);
            String locator = row.getStringValue();
            OpActivity activity = (OpActivity) broker.getObject(locator);
            if (activity.getType() == OpActivity.ADHOC_TASK) {
               //check access level
               if (session.effectiveAccessLevel(broker, activity.getProjectPlan().getID()) >= OpPermission.MANAGER) {
                  boolean hasWorkSlips = false;
                  for (Iterator iterator = activity.getAssignments().iterator(); iterator.hasNext();) {
                     OpAssignment assignment = (OpAssignment) iterator.next();
                     if (!assignment.getWorkRecords().isEmpty()) {
                        hasWorkSlips = true;
                     }
                  }
                  if (!hasWorkSlips) {
                     broker.deleteObject(activity);
                  }
                  else {
                     reply.setError(session.newError(ERROR_MAP, OpMyTasksError.EXISTING_WORKSLIP_ERROR_CODE));
                  }
               }
               else {
                  reply.setError(session.newError(ERROR_MAP, OpMyTasksError.INSUFICIENT_PERMISSIONS_ERROR_CODE));
               }
            }
         }
         transaction.commit();
         broker.close();
      }
      return reply;
   }


}
