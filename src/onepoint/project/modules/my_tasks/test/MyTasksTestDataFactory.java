/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.my_tasks.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.test.TestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class contains helper methods for managing activitys data
 *
 * @author lucian.furtos
 */
public class MyTasksTestDataFactory extends TestDataFactory {

   private final static String SELECT_MYTASKS_ID_BY_NAME_QUERY = "select activity.ID from OpActivity as activity where activity.Name = ?";

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public MyTasksTestDataFactory(OpProjectSession session) {
      super(session);
   }

   /**
    * Get a activity by the name
    *
    * @param activityName the activity name
    * @return an instance of <code>OpActivity</code>
    */
   public OpActivity getActivityByName(String activityName) {
      String locator = getActivityId(activityName);
      if (locator != null) {
         return getActivityById(locator);
      }

      return null;
   }

   /**
    * Get a activity by the locator
    *
    * @param locator the uniq identifier (locator) of an entity
    * @return an instance of <code>OpActivity</code>
    */
   public OpActivity getActivityById(String locator) {
      OpBroker broker = session.newBroker();

      OpActivity activity = (OpActivity) broker.getObject(locator);
      // just to inialize the collection
      activity.getProjectPlan().locator();
      activity.getAssignments().size();
      activity.getDynamicResources().size();
      activity.getAttachments().size();
      activity.getLocks().size();
      activity.getPermissions().size();
      activity.getComments().size();
      activity.getPredecessorDependencies().size();
      activity.getSubActivities().size();
      activity.getSuccessorDependencies().size();
      activity.getVersions().size();
      activity.getWorkPeriods().size();
      broker.close();

      return activity;
   }

   /**
    * Get the uniq identifier of a activity by name
    *
    * @param activityName the activity name
    * @return the uniq identifier (locator) of an entity
    */
   public String getActivityId(String activityName) {
      OpBroker broker = session.newBroker();
      Long projId = null;

      OpQuery query = broker.newQuery(SELECT_MYTASKS_ID_BY_NAME_QUERY);
      query.setString(0, activityName);
      Iterator activityIt = broker.iterate(query);
      if (activityIt.hasNext()) {
         projId = (Long) activityIt.next();
      }

      broker.close();
      if (projId != null) {
         return OpLocator.locatorString(OpActivity.ACTIVITY, projId.longValue());
      }
      return null;
   }

   /**
    * Get all the activitys
    *
    * @return a <code>List</code> of <code>OpActivity</code>
    */
   public List getAllActivities() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from OpActivity");
      List result = broker.list(query);
      broker.close();

      return result;
   }

   /**
    * Get all the attachments
    *
    * @return a <code>List</code> of <code>OpAttachment</code>
    */
   public List getAllAttachments() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from OpAttachment as attachment order by attachment.Name");
      List result = broker.list(query);
      broker.close();

      return result;
   }

   public static XMessage addAdhocMsg(String name, String description, int priority, Date duedate, String projectChoice, String resourceChioce) {
      return addAdhocMsg(name, description, priority, duedate, projectChoice, resourceChioce, new XComponent(XComponent.DATA_SET));
   }

   public static XMessage addAdhocMsg(String name, String description, int priority, Date duedate, String projectChoice,
        String resourceChioce, XComponent dataSet) {
      HashMap args = new HashMap();
      args.put("name", name);
      args.put("description", description);
      args.put("priority", new Integer(priority));
      args.put("dueDate", duedate);
      args.put("projectChoice", projectChoice);
      args.put("resourceChoice", resourceChioce);
      args.put("attachmentSet", dataSet);

      XMessage request = new XMessage();
      request.setArgument("adhocData", args);
      return request;
   }

   public static XMessage updateAdhocMsg(String locator, String name, String description, int priority, Date duedate, String projectChoice, String resourceChioce) {
      return updateAdhocMsg(locator, name, description, priority, duedate, projectChoice, resourceChioce, new XComponent(XComponent.DATA_SET));
   }

   public static XMessage updateAdhocMsg(String locator, String name, String description, int priority, Date duedate,
        String projectChoice, String resourceChioce, XComponent dataSet) {
      HashMap args = new HashMap();
      args.put("activityLocator", locator);
      args.put("name", name);
      args.put("description", description);
      args.put("priority", new Integer(priority));
      args.put("dueDate", duedate);
      args.put("projectChoice", projectChoice);
      args.put("resourceChoice", resourceChioce);
      args.put("attachmentSet", dataSet);

      XMessage request = new XMessage();
      request.setArgument("adhocData", args);
      return request;
   }

   public static XMessage deleteAdhocMsg(List ids) {
      List args = new ArrayList();
      for (Iterator iterator = ids.iterator(); iterator.hasNext();) {
         String id = (String) iterator.next();
         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(id);
         args.add(row);
      }

      XMessage request = new XMessage();
      request.setArgument("adhocData", args);
      return request;
   }

   /**
    * Creates an attachmet data set from a list
    *
    * @param choices the choice ( id['name']) of each attachment
    * @return an DATA_SET XComponent
    */
   public static XComponent createDataSet(List choices) {
      XComponent data_set = new XComponent(XComponent.DATA_SET);
      if (choices != null) {
         for (int i = 0; i < choices.size(); i++) {
            XComponent row = new XComponent(XComponent.DATA_ROW);
            data_set.addChild(row);
            // 0. attch descriptor
            XComponent cell = new XComponent(XComponent.DATA_CELL);
            cell.setValue(OpActivityDataSetFactory.LINKED_ATTACHMENT_DESCRIPTOR);
            row.addChild(cell);
            // 1. choice
            cell = new XComponent(XComponent.DATA_CELL);
            cell.setValue(choices.get(i));
            row.addChild(cell);
            // 2. location
            cell = new XComponent(XComponent.DATA_CELL);
            cell.setValue("location");
            row.addChild(cell);
         }
      }
      return data_set;
   }

}
