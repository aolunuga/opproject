/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResource;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceException;

import java.sql.Date;
import java.util.*;

/**
 * Service class for my tasks module.
 *
 * @author mihai.costin
 */
public class OpMyTasksService extends OpProjectService {

   private static final String ADHOC_DATA = "adhocData";

   //arguments names
   private final String ACTIVITY = "activityLocator";
   private final String NAME = "name";
   private final String DESCRIPTION = "description";
   private final String PRIORITY = "priority";
   private final String DUEDATE = "dueDate";
   private final String PROJECT = "projectChoice";
   private final String RESOURCE = "resourceChoice";
   private final String ATTACHMENT_SET = "attachmentSet";

   // FIXME(dfreis Mar 5, 2007 11:16:13 AM)
   // should be set within constructor!
   private OpMyTasksServiceImpl serviceImpl =  new OpMyTasksServiceImpl();

   /**
    * Adds a new ad-hoc tasks based on the given information.
    *
    * @param session an <code>OpProjectSession</code> object - the current session.
    * @param request a <code>XMessage</code> - the request (contains the task data).
    * @return a response in the form of a <code>XMessage</code> object.
    */
   public XMessage addAdhocTask(OpProjectSession session, XMessage request) {

      HashMap arguments = (HashMap) request.getArgument(ADHOC_DATA);

      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();
      OpTransaction transaction = null;
      try
      {
        OpProjectNode project = null;
        String project_locator = (String) arguments.get(PROJECT);
        if (project_locator != null && project_locator.length() > 0) {
          project = (OpProjectNode) broker.getObject(XValidator.choiceID(project_locator));
        }
        OpResource resource = null;
        String resource_locator = (String) arguments.get(RESOURCE);
        if (resource_locator != null && resource_locator.length() > 0) {
          resource = (OpResource) broker.getObject(XValidator.choiceID(resource_locator));
        }

        transaction = broker.newTransaction();
        OpActivity adhocTaks = new OpActivity(OpActivity.ADHOC_TASK);
        adhocTaks.setName((String) arguments.get(NAME));
        adhocTaks.setDescription((String) arguments.get(DESCRIPTION));
        adhocTaks.setFinish((Date) arguments.get(DUEDATE));
        adhocTaks.setProjectPlan(project == null ? null : project.getPlan());
        try {
          adhocTaks.setPriority(((Integer) arguments.get(PRIORITY)).byteValue());
        } catch (IllegalArgumentException exc)
        {
          reply.setError(session.newError(OpMyTasksServiceImpl.ERROR_MAP, OpMyTasksError.INVALID_PRIORITY_ERROR_CODE));
          return(reply);
        }

        OpAssignment assignment = new OpAssignment();
        assignment.setActivity(adhocTaks);
        assignment.setResource(resource);
        assignment.setProjectPlan(adhocTaks.getProjectPlan());
        HashSet<OpAssignment> assignments = new HashSet<OpAssignment>();
        assignments.add(assignment);
        adhocTaks.setAssignments(assignments);
        
        XComponent attachmentSet = (XComponent) arguments.get(ATTACHMENT_SET);
        adhocTaks.setAttachments(new HashSet());
        updateAttachments(session, broker, adhocTaks, attachmentSet);

        serviceImpl.insertAdhocTask(session, broker, adhocTaks);
        transaction.commit();
      } catch (XServiceException exc) {
        exc.append(reply);
        if (transaction != null)
           transaction.rollback();
        return(reply);
      } finally {
        broker.close();
      }
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

      String locator = (String) arguments.get(ACTIVITY);
      OpBroker broker = session.newBroker();
      OpTransaction transaction = null;
      XMessage reply = new XMessage();
      try
      {
        OpActivity activity = serviceImpl.getTaskByIdString(session, broker, locator);
        transaction = broker.newTransaction();

        activity.setName((String) arguments.get(NAME));
        activity.setDescription((String) arguments.get(DESCRIPTION));
        activity.setPriority(((Integer)arguments.get(PRIORITY)).byteValue());
        activity.setFinish((Date) arguments.get(DUEDATE));

        String projectLocator = (String) arguments.get(PROJECT);
        OpProjectNode project = (OpProjectNode) broker.getObject(projectLocator);
        activity.setProjectPlan(project.getPlan());

        // set resource to first assignment ??!!
        String resourceLocator = (String) arguments.get(RESOURCE);
        Iterator assignments = activity.getAssignments().iterator();
        OpResource resource;
        OpAssignment assignment;
        if (assignments.hasNext()) {
          assignment = (OpAssignment) assignments.next();
          resource = (OpResource) broker.getObject(resourceLocator);
          assignment.setResource(resource);
//          broker.updateObject(assignment);
        }

        //update attachments
        XComponent attachmentSet = (XComponent) arguments.get(ATTACHMENT_SET);
        updateAttachments(session, broker, activity, attachmentSet);

        serviceImpl.updateAdhocTask(session, broker, activity);
        
        transaction.commit();
      } catch (XServiceException exc) {
        exc.append(reply);
        if (transaction != null) {
          transaction.rollback();
        }
      } finally {
        broker.close();
      }
      return reply;
   }

   private void updateAttachments(OpProjectSession session, OpBroker broker, OpActivity activity, XComponent attachmentSet) {

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

      // remove non existing attachments
      for (Iterator iterator = existingSet.iterator(); iterator.hasNext();) {
         OpAttachment attachment = (OpAttachment) iterator.next();
         if (!attachmentsRowMap.keySet().contains(attachment.locator())) {
           serviceImpl.deleteAttachment(session, broker, attachment);
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
      }
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
       try
       {
         for (int i = 0; i < selectedRows.size(); i++) {
           XComponent row = (XComponent) selectedRows.get(i);
           String locator = row.getStringValue();
           OpActivity activity = (OpActivity) broker.getObject(locator);
           serviceImpl.deleteAdhocTask(session, broker, activity);
         }
         transaction.commit();
       } catch (XServiceException exc) {
         exc.append(reply);
         transaction.rollback();
       } finally {
         broker.close();
       }
     }
     return reply;
   }
}
