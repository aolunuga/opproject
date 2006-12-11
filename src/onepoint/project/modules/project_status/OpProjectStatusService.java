/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_status;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;

import java.util.*;

/**
 * Project Status service class. Covers basic operations on project status items.
 *
 * @author mihai.costin
 */
public class OpProjectStatusService extends OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpProjectStatusService.class, true);

   public final static String PROJECT_STATUS_ID = "project_status_id";
   private final static String PROJECT_STATUS_IDS = "project_status_ids";
   private final static String PROJECT_STATUS_DATA = "project_status_data";
   private final static String PROJECT_STATUS_LOCATORS = "project_locators";
   private final static String MOVE_DIRECTION = "direction";
   private final static int MOVE_UP = -1;
   private final static int MOVE_DOWN = 1;

   public final static OpProjectStatusErrorMap ERROR_MAP = new OpProjectStatusErrorMap();

   public XMessage insertProjectStatus(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

      logger.debug("OpProjectStatusService.insertProjectStatus()");

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpProjectStatusError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

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

      // check if projectStatus name is already used
      OpQuery query = broker
           .newQuery("select status.ID from OpProjectStatus as status where status.Name = :statusName");
      query.setString("statusName", projectStatus.getName());
      Iterator categoryIds = broker.iterate(query);
      if (categoryIds.hasNext()) {
         reply.setError(session.newError(ERROR_MAP, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_UNIQUE));
         broker.close();
         return reply;
      }

      //get the max sequence from db
      query = broker.newQuery("select max(status.Sequence) from OpProjectStatus as status");
      Iterator result = broker.iterate(query);
      int sequence = 0;
      if (result.hasNext()) {
         Integer maxSequence = (Integer) result.next();
         if (maxSequence != null) {
            sequence = maxSequence.intValue() + 1;
         }
      }
      projectStatus.setSequence(sequence);

      OpTransaction t = broker.newTransaction();
      broker.makePersistent(projectStatus);
      t.commit();

      logger.debug("/OpProjectStatusService.insertProjectStatus()");
      broker.close();
      return reply;
   }

   public XMessage updateProjectStatus(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpProjectStatusError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      String id_string = (String) (request.getArgument(PROJECT_STATUS_ID));
      logger.debug("OpProjectStatusService.updateProjectStatus(): id = " + id_string);
      HashMap project_status_data = (HashMap) (request.getArgument(PROJECT_STATUS_DATA));

      OpBroker broker = session.newBroker();
      XMessage reply = new XMessage();

      OpProjectStatus projectStatus = (OpProjectStatus) (broker.getObject(id_string));
      if (projectStatus == null) {
         logger.warn("ERROR: Could not find object with ID " + id_string);
         reply.setError(session.newError(ERROR_MAP, OpProjectStatusError.PROJECT_STATUS_NOT_FOUND));
         broker.close();
         return reply;
      }

      String projectStatusName = (String) (project_status_data.get(OpProjectStatus.NAME));

      // check mandatory input fields
      if (projectStatusName == null || projectStatusName.length() == 0) {
         reply.setError(session.newError(ERROR_MAP, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_SPECIFIED));
         broker.close();
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
            reply.setError(session.newError(ERROR_MAP, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_UNIQUE));
            broker.close();
            return reply;
         }
      }

      projectStatus.setName(projectStatusName);
      projectStatus.setDescription((String) (project_status_data.get(OpProjectStatus.DESCRIPTION)));
      projectStatus.setColor(((Integer) (project_status_data.get(OpProjectStatus.COLOR))).intValue());

      OpTransaction t = broker.newTransaction();

      broker.updateObject(projectStatus);

      t.commit();
      logger.debug("/OpProjectStatusService.updateProjectStatus()");
      broker.close();
      return reply;
   }

   public XMessage move(XSession s, XMessage request) {
      XMessage reply = new XMessage();
      OpProjectSession session = (OpProjectSession) s;

      if (!session.userIsAdministrator()) {
         XError error = session.newError(ERROR_MAP, OpProjectStatusError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      OpBroker broker = ((OpProjectSession) session).newBroker();
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
      
      broker.close();
      return reply;
   }


   public XMessage deleteProjectStatus(XSession s, XMessage request) {

      OpProjectSession session = (OpProjectSession) s;

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpProjectStatusError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      ArrayList id_strings = (ArrayList) (request.getArgument(PROJECT_STATUS_IDS));

      logger.debug("OpProjectStatusService.deleteProjectStatus(): project_status_ids = " + id_strings);

      OpBroker broker = ((OpProjectSession) session).newBroker();

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
         if (status.getProjects().size() > 0) {
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
         status.setSequence(sequence);
         broker.updateObject(status); 
         sequence++;
      }

      t.commit();
      logger.debug("/OpProjectStatusService.deleteProjectStatus()");

      broker.close();

      return null;
   }
}
