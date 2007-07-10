/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Class that contains helper methods for managing activity data
 *
 * @author florin.haizea
 */
public class OpActivityTestDataFactory extends OpTestDataFactory {

   private final static String SELECT_ACTIVITY_ID_BY_NAME_QUERY = "select activity.ID from OpActivity as activity where activity.Name = ?";
   private final static String SELECT_ACTIVITY_VERSION_ID_BY_NAME_QUERY = "select activityVersion.ID from OpActivityVersion as activityVersion where activityVersion.Name = ?";
   private final static String ACTIVITY_DATA = "activityData";

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public OpActivityTestDataFactory(OpProjectSession session) {
      super(session);
   }

   /**
    * Get an activity by name
    *
    * @param activityName the activty name
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
    * Get an activity by locator
    *
    * @param locator the unique identifier (locator) of an entity
    * @return an instance of <code>OpActivity</code>
    */
   public OpActivity getActivityById(String locator) {
      OpBroker broker = session.newBroker();

      OpActivity activity = (OpActivity) broker.getObject(locator);
      if (activity != null) {
         // just to initialize the collection
         activity.getAssignments().size();
         activity.getAttachments().size();
         activity.getComments().size();
         activity.getPredecessorDependencies();
         activity.getSubActivities().size();
         activity.getSuccessorDependencies().size();
         activity.getVersions().size();
         activity.getWorkPeriods().size();
      }
      broker.close();

      return activity;
   }

   /**
    * Get an activity version by locator
    *
    * @param locator the unique identifier (locator) of an entity
    * @return an instance of <code>OpActivityVersion</code>
    */
   public OpActivityVersion getActivityVersionById(String locator) {
      OpBroker broker = session.newBroker();

      OpActivityVersion activityVersion = (OpActivityVersion) broker.getObject(locator);
      if (activityVersion != null) {
         // just to initialize the collection
         activityVersion.getAssignmentVersions().size();
         activityVersion.getAttachmentVersions().size();
         activityVersion.getPredecessorVersions();
         activityVersion.getSubActivityVersions().size();
         activityVersion.getSuccessorVersions().size();
         activityVersion.getWorkPeriodVersions().size();
      }
      broker.close();

      return activityVersion;
   }

   /**
    * Get the DB identifier of an activity by name
    *
    * @param activityName the activity name
    * @return the unique identifier of an entity (the locator)
    */
   public String getActivityId(String activityName) {
      OpBroker broker = session.newBroker();
      Long activityId = null;

      OpQuery query = broker.newQuery(SELECT_ACTIVITY_ID_BY_NAME_QUERY);
      query.setString(0, activityName);
      Iterator activityIt = broker.iterate(query);
      if (activityIt.hasNext()) {
         activityId = (Long) activityIt.next();
      }
      broker.close();

      if (activityId != null) {
         return OpLocator.locatorString(OpActivity.ACTIVITY, Long.parseLong(activityId.toString()));
      }
      return null;
   }

   /**
    * Get the DB identifier of an activity version by name
    *
    * @param activityName the activity name
    * @return the unique identifier of an entity (the locator)
    */
   public String getActivityVersionId(String activityName) {
      OpBroker broker = session.newBroker();
      Long activityId = null;

      OpQuery query = broker.newQuery(SELECT_ACTIVITY_VERSION_ID_BY_NAME_QUERY);
      query.setString(0, activityName);
      Iterator activityIt = broker.iterate(query);
      if (activityIt.hasNext()) {
         activityId = (Long) activityIt.next();
      }
      broker.close();

      if (activityId != null) {
         return OpLocator.locatorString(OpActivityVersion.ACTIVITY_VERSION, Long.parseLong(activityId.toString()));
      }
      return null;
   }

   /**
    * A request to create an activity.
    *
    * @param name
    * @param description
    * @param start
    * @param finish
    * @param projectPlan
    * @return
    */
   public XMessage createActivityMsg(String name, String description, Date start, Date finish, OpProjectPlan projectPlan) {
      HashMap args = new HashMap();
      args.put(OpActivity.NAME, name);
      args.put(OpActivity.DESCRIPTION, description);
      args.put(OpActivity.START, start);
      args.put(OpActivity.FINISH, finish);
      args.put(OpActivity.PROJECT_PLAN, projectPlan);

      XMessage request = new XMessage();
      request.setArgument(ACTIVITY_DATA, args);
      return request;
   }

   /**
    * Inserts an activity with name, description, start/finish dates and a project assignment
    *
    * @param session
    * @param request
    * @return - an empty <code>XMessage</code> if the insert operation succeeded
    */
   public XMessage insertActivity(OpProjectSession session, XMessage request) {

      XMessage reply = new XMessage();

      String name = (String) (request.getArgument(OpActivity.NAME));
      String description = (String) (request.getArgument(OpActivity.DESCRIPTION));
      Date start = (Date) (request.getArgument(OpActivity.START));
      Date finish = (Date) (request.getArgument(OpActivity.FINISH));
      OpProjectPlan projectPlan = (OpProjectPlan) (request.getArgument(OpActivity.PROJECT_PLAN));

      OpActivity activity = new OpActivity();
      activity.setName(name);
      activity.setDescription(description);
      activity.setStart(start);
      activity.setFinish(finish);
      activity.setProjectPlan(projectPlan);

      OpBroker broker = session.newBroker();
      broker.makePersistent(activity);
      broker.close();

      return reply;
   }
}
