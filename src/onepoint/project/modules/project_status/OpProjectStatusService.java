/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_status;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.service.XMessage;

import java.util.*;

/**
 * Project Status service class. Covers basic operations on project status items.
 *
 * @author mihai.costin
 */
public class OpProjectStatusService extends OpProjectService {

   private static final XLog logger = XLogFactory.getServerLogger(OpProjectStatusService.class);

   public final static String PROJECT_STATUS_ID = "project_status_id";
   private final static String PROJECT_STATUS_IDS = "project_status_ids";
   private final static String PROJECT_STATUS_DATA = "project_status_data";
   private final static String PROJECT_STATUS_LOCATORS = "project_locators";
   private final static String MOVE_DIRECTION = "direction";
   private final static int MOVE_UP = -1;
   private final static int MOVE_DOWN = 1;

   public final static OpProjectStatusErrorMap ERROR_MAP = new OpProjectStatusErrorMap();

   public XMessage insertProjectStatus(OpProjectSession session, XMessage request) {
      logger.debug("OpProjectStatusService.insertProjectStatus()");

      HashMap project_status_data = (HashMap) (request.getArgument(PROJECT_STATUS_DATA));

      OpProjectStatus projectStatus = new OpProjectStatus();
      projectStatus.setName((String) (project_status_data.get(OpProjectStatus.NAME)));
      projectStatus.setDescription((String) (project_status_data.get(OpProjectStatus.DESCRIPTION)));
      projectStatus.setColor(((Integer) (project_status_data.get(OpProjectStatus.COLOR))).intValue());

      XMessage reply = new XMessage();

      // check mandatory input fields
      if (projectStatus.getName() == null || projectStatus.getName().length() == 0) {
         reply.setError(session.newError(ERROR_MAP, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_SPECIFIED));
         return reply;
      }

      OpBroker broker = session.newBroker();
      try {
         //get the max sequence from db
         OpQuery query = broker.newQuery("select max(status.Sequence) from OpProjectStatus as status");
         Iterator result = broker.iterate(query);
         int sequence = 0;
         if (result.hasNext()) {
            Integer maxSequence = (Integer) result.next();
            if (maxSequence != null) {
               sequence = maxSequence.intValue() + 1;
            }
         }
         projectStatus.setSequence(sequence);

         // check if projectStatus name is already used
         query = broker
         .newQuery("from OpProjectStatus as status where status.Name = :statusName");
         query.setString("statusName", projectStatus.getName());
         Iterator categoryIds = broker.iterate(query);
         if (categoryIds.hasNext()) {
            OpProjectStatus existingProjectStatus = (OpProjectStatus) categoryIds.next();
            reply = reactivateProjectStatus(session, existingProjectStatus, projectStatus);
            if (reply.getError() != null) {
               return reply;
            }
            else {
               OpTransaction t = broker.newTransaction();
               try {
                  broker.updateObject(existingProjectStatus);
                  t.commit();
               }
               finally {
                  if (!t.wasCommited()) {
                     t.rollback();
                  }
               }
               return reply;
            }
         }

         OpTransaction t = broker.newTransaction();
         try {
            broker.makePersistent(projectStatus);
            t.commit();
         }
         finally {
            if (!t.wasCommited()) {
               t.rollback();
            }
         }

         logger.debug("/OpProjectStatusService.insertProjectStatus()");
         return reply;
      }
      finally {
         broker.close();
      }
   }

   public XMessage updateProjectStatus(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(PROJECT_STATUS_ID));
      logger.debug("OpProjectStatusService.updateProjectStatus(): id = " + id_string);
      HashMap project_status_data = (HashMap) (request.getArgument(PROJECT_STATUS_DATA));

      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();
      try {
         OpProjectStatus projectStatus = (OpProjectStatus) (broker.getObject(id_string));
         if (projectStatus == null) {
            logger.warn("ERROR: Could not find object with ID " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpProjectStatusError.PROJECT_STATUS_NOT_FOUND));
            return reply;
         }

         String projectStatusName = (String) (project_status_data.get(OpProjectStatus.NAME));

         // check mandatory input fields
         if (projectStatusName == null || projectStatusName.length() == 0) {
            reply.setError(session.newError(ERROR_MAP, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_SPECIFIED));
            return reply;
         }

         // check if projectStatus name is already used
         OpQuery query = broker
         .newQuery("select status from OpProjectStatus as status where status.Name = :statusName");
         query.setString("statusName", projectStatusName);
         Iterator projectStatusItr = broker.iterate(query);
         while (projectStatusItr.hasNext()) {
            OpProjectStatus other = (OpProjectStatus) projectStatusItr.next();
            if (other.getID() != projectStatus.getID()) {
               reply = reactivateProjectStatus(session, other, projectStatus);
               if (reply.getError() != null) {
                  return reply;
               }
               else {
                  OpTransaction t = broker.newTransaction();
                  try {
                     //find all projects that have the project status as their status and transfer their status to the
                     //reactivated status
                     for(OpProjectNode project : projectStatus.getProjects()) {
                        project.setStatus(other);
                     }

                     //delete the project status
                     broker.deleteObject(projectStatus);

                     broker.updateObject(other);
                     t.commit();
                  }
                  finally {
                     if (!t.wasCommited()) {
                        t.rollback();
                     }
                  }
                  return reply;
               }
            }
         }

         projectStatus.setName(projectStatusName);
         projectStatus.setDescription((String) (project_status_data.get(OpProjectStatus.DESCRIPTION)));
         projectStatus.setColor(((Integer) (project_status_data.get(OpProjectStatus.COLOR))).intValue());

         OpTransaction t = broker.newTransaction();
         try {
            broker.updateObject(projectStatus);
            t.commit();
         }
         finally {
            if (!t.wasCommited()) {
               t.rollback();
            }
         }
         logger.debug("/OpProjectStatusService.updateProjectStatus()");
         return reply;
      }
      finally {
         broker.close();
      }
   }

   public XMessage move(OpProjectSession session, XMessage request) {
      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();
      try {
         List locators = (List) request.getArgument(PROJECT_STATUS_LOCATORS);
         int direction = ((Integer) request.getArgument(MOVE_DIRECTION)).intValue();

         Map sortOrder = new HashMap(1);
         List projectStatusList = new ArrayList();
         String queryString = "select status from OpProjectStatus as status where status.Active=true ";

         int increment = 0;
         OpProjectStatus previous = null;
         if (direction == MOVE_UP) {
            increment = -1;
            sortOrder.put(OpProjectStatus.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
            OpObjectOrderCriteria categoryOrderCriteria = new OpObjectOrderCriteria(OpProjectStatus.PROJECT_STATUS, sortOrder);
            OpQuery query = broker.newQuery(queryString + categoryOrderCriteria.toHibernateQueryString("status"));
            projectStatusList = broker.list(query);

         }
         else if (direction == MOVE_DOWN) {
            increment = 1;
            sortOrder.put(OpProjectStatus.SEQUENCE, OpObjectOrderCriteria.DESCENDING);
            OpObjectOrderCriteria categoryOrderCriteria = new OpObjectOrderCriteria(OpProjectStatus.PROJECT_STATUS, sortOrder);
            OpQuery query = broker.newQuery(queryString + categoryOrderCriteria.toHibernateQueryString("status"));
            projectStatusList = broker.list(query);
         }

         List changedObjects = new ArrayList();

         for (int i=0; i<projectStatusList.size(); i++) {
            OpProjectStatus status = (OpProjectStatus) projectStatusList.get(i);
            if (locators.contains(status.locator())) {

               //must be moved
               status.setSequence(status.getSequence() + increment);
               changedObjects.add(status);

               //update alo the previous element
               if (previous != null) {
                  previous.setSequence(previous.getSequence() - increment);
                  if (!changedObjects.contains(previous)) {
                     changedObjects.add(previous);
                  }
               }
            }
            else {
               previous = status;
            }
         }

         OpTransaction transaction = broker.newTransaction();
         for (int i=0; i<changedObjects.size(); i++) {
            OpProjectStatus status = (OpProjectStatus) changedObjects.get(i);
            broker.updateObject(status);
         }
         transaction.commit();

         return reply;
      }
      finally {
         broker.close();
      }
   }


   public XMessage deleteProjectStatus(OpProjectSession session, XMessage request) {
      List id_strings = (List) (request.getArgument(PROJECT_STATUS_IDS));

      logger.debug("OpProjectStatusService.deleteProjectStatus(): project_status_ids = " + id_strings);

      OpBroker broker = ((OpProjectSession) session).newBroker();
      try {
         ArrayList projectStatusItems = new ArrayList();
         int i = 0;
         for (i = 0; i < id_strings.size(); i++) {
            projectStatusItems.add(new Long(OpLocator.parseLocator((String) id_strings.get(i)).getID()));
         }

         OpQuery query;
         OpTransaction t = broker.newTransaction();

         query = broker.newQuery("select min(status.Sequence) from OpProjectStatus as status");
         Iterator minResult = broker.iterate(query);
         int minSequence = 0;
         if (minResult.hasNext()) {
            Integer minValue = (Integer) minResult.next();
            if (minValue != null) {
               minSequence = minValue.intValue();
            }
         }

         query = broker.newQuery("select status from OpProjectStatus as status where status.ID in (:projectStatusIds)");
         query.setCollection("projectStatusIds", projectStatusItems);
         Iterator results = broker.iterate(query);
         while (results.hasNext()) {
            OpProjectStatus status = (OpProjectStatus) results.next();
            if (OpProjectDataSetFactory.getProjectsCount(broker, status) > 0) {
               if (status.getActive()) {
                  status.setActive(false);
                  minSequence--;
                  status.setSequence(minSequence);
                  broker.updateObject(status);
               }
            }
            else {
               broker.deleteObject(status);
            }
         }

         //update sequence for the active status items
         results = OpProjectDataSetFactory.getProjectStatusIterator(broker);
         int sequence = 0;
         while (results.hasNext()) {
            OpProjectStatus status = (OpProjectStatus) results.next();

            if (!status.getActive()) {
               continue;
            }
            status.setSequence(sequence);
            broker.updateObject(status);
            sequence++;
         }

         t.commit();
         logger.debug("/OpProjectStatusService.deleteProjectStatus()");

      }
      finally {
         broker.close();
      }
      return null;
   }

   /**
    * Reactivates an existing <code>OpProjectStatus</code> object which is inactive and sets it's field values from
    *    the other <code>OpProjectStatus</code> object passed as parameter.
    *
    * @param session - the <code>OpProjectSession</code> object.
    * @param inactiveProjectStatus - the <code>OpProjectStatus</code> object that is going to be reactivated.
    * @param newProjectStatus - the <code>OpProjectStatus</code> object which supplies the values for the reactivated
    *    project status.
    * @return an <code>XMessage</code> object containing an error code if the inactive project status is active.
    */
   private XMessage reactivateProjectStatus(OpProjectSession session, OpProjectStatus inactiveProjectStatus,
        OpProjectStatus newProjectStatus) {
      XMessage reply = new XMessage();
      if (inactiveProjectStatus.getActive()) {
         reply.setError(session.newError(ERROR_MAP, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_UNIQUE));
         return reply;
      }
      else {
         inactiveProjectStatus.setActive(true);
         inactiveProjectStatus.setDescription(newProjectStatus.getDescription());
         inactiveProjectStatus.setColor(newProjectStatus.getColor());
         inactiveProjectStatus.setSequence(newProjectStatus.getSequence());
      }
      return reply;
   }
}