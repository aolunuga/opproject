/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project_status.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.project.test.OpTestDataFactory;

import java.util.Iterator;
import java.util.List;

/**
 * This class contains helper methods for managing project status data
 *
 * @author lucian.furtos
 */
public class OpProjectStatusTestDataFactory extends OpTestDataFactory {

   private final static String SELECT_STATUS_ID_BY_NAME_QUERY = "select status.id from OpProjectStatus as status where status.Name = ?";

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public OpProjectStatusTestDataFactory(OpProjectSession session) {
      super(session);
   }

   /**
    * Get a project status by the name
    *
    * @param statusName the project status name
    * @return an instance of <code>OpProjectStatus</code>
    */
   public OpProjectStatus getProjectStatusByName(String statusName) {
      Long id = getProjectStatusId(statusName);
      if (id != null) {
         String locator = OpLocator.locatorString(OpProjectStatus.PROJECT_STATUS, Long.parseLong(id.toString()));

         return getProjectStatusById(locator);
      }

      return null;
   }

   /**
    * Get a project status by the locator
    *
    * @param locator the uniq identifier (locator) of an entity
    * @return an instance of <code>OpProjectStatus</code>
    */
   public OpProjectStatus getProjectStatusById(String locator) {
      OpBroker broker = session.newBroker();
      try {
         return (OpProjectStatus) broker.getObject(locator);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get the DB identifier of a project status by name
    *
    * @param statusName the project status name
    * @return the uniq identifier (locator) of an entity
    */
   public Long getProjectStatusId(String statusName) {
     OpBroker broker = session.newBroker();
      try {
         Long statusId = null;
         OpQuery query = broker.newQuery(SELECT_STATUS_ID_BY_NAME_QUERY);
         query.setString(0, statusName);
         Iterator statusIt = broker.iterate(query);
         if (statusIt.hasNext()) {
            statusId = (Long) statusIt.next();
         }
         return statusId;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get all the project statuses ordered by sequence
    *
    * @return a <code>List</code> of <code>OpProjectStatus</code>
    */
   public List getAllProjectsStatus() {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("from OpProjectStatus as status order by status.Sequence asc");
         List result = broker.list(query);
         return result;
      }
      finally {
         broker.close();
      }
   }
}
