/*
 * Copyright(c) OnePoint Software GmbH 2005. All Rights Reserved.
 */
package onepoint.project.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.util.XCalendar;
import org.jmock.core.Constraint;
import org.jmock.core.Invocation;

import java.sql.Date;
import java.util.*;

/**
 * Test suite for XActivitySetFactory class.
 *
 * @author mihai.costin
 * @author ovidiu.lupas Date: Dec 12, 2005
 */
public class OpActivityDataSetFactoryTest extends OpServiceAbstractTest {

   private OpProjectPlan opProjectPlan;
   private OpActivity opFirstActivity;
   private OpActivity opSecondActivity;
   private OpActivity opThirdActivity;
   private OpResource opFirstResource;
   private OpResource opSecondResource;
   private OpWorkPeriod opFirstWorkPeriod;
   private OpProjectNode opProject;
   private OpBroker broker;

   private static final String SELECT_ACTIVITY = "select activity from OpActivity as activity where activity.ProjectPlan.ID = ? and activity.Deleted = false order by activity.Sequence";

   private static final String SELECT_RESOURCES_FOR_PROJECT = "select assignment.Resource from OpProjectNodeAssignment as assignment where assignment.ProjectNode.ID = ? order by assignment.Resource.Name asc";

   private static final int FIRST_ACTIVITY_ID = 1999;
   private static final int SECOND_ACTIVITY_ID = 2000;
   private static final int THIRD_ACTIVITY_ID = 2001;
   private static final int FIRST_RESOURCE_ID = 2002;
   private static final int SECOND_RESOURCE_ID = 2004;
   private static final int FIRST_WORK_PHASE_ID = 2003;
   private static final String FIRST_RESOURCE_NAME = "Resource One";
   private static final String SECOND_RESOURCE_NAME = "Resource Two";
   private static final String FIRST_RESOURCE_LOCATOR = "OpResource." + FIRST_RESOURCE_ID + ".xid";
   private static final String SECOND_RESOURCE_LOCATOR = "OpResource." + SECOND_RESOURCE_ID + ".xid";
   private static final String FIRST_ACTIVITY_LOCATOR = "OpActivity." + FIRST_ACTIVITY_ID + ".xid";
   private static final String SECOND_ACTIVITY_LOCATOR = "OpActivity." + SECOND_ACTIVITY_ID + ".xid";
   private static final String THIRD_ACTIVITY_LOCATOR = "OpActivity." + THIRD_ACTIVITY_ID + ".xid";

   /**
    * @see junit.framework.TestCase#setUp()
    */
   public void setUp() {
      super.setUp();

      broker = (OpBroker) mockBroker.proxy();

      opProjectPlan = new OpProjectPlan();
      opProjectPlan.setStart(XCalendar.today());
      opProjectPlan.setFinish(XCalendar.today());
      opProjectPlan.setActivityAssignments(new HashSet());
      opProjectPlan.setActivityAttachments(new HashSet());
      opProjectPlan.setDependencies(new HashSet());
      opProjectPlan.setWorkPeriods(new HashSet());
      opProjectPlan.setProgressTracked(true);

      opFirstActivity = new OpActivity();
      opFirstActivity.setSequence(0);
      opFirstActivity.setID(FIRST_ACTIVITY_ID);
      opFirstActivity.setStart(XCalendar.today());
      opFirstActivity.setFinish(XCalendar.today());
      opFirstActivity.setAttachments(new HashSet());
      opFirstActivity.setAssignments(new HashSet());
      opFirstActivity.setWorkPeriods(new HashSet());
      opFirstActivity.setProjectPlan(opProjectPlan);

      opSecondActivity = new OpActivity();
      opSecondActivity.setSequence(1);
      opSecondActivity.setID(SECOND_ACTIVITY_ID);
      opSecondActivity.setStart(XCalendar.today());
      opSecondActivity.setFinish(XCalendar.today());
      opSecondActivity.setAttachments(new HashSet());
      opSecondActivity.setAssignments(new HashSet());
      opSecondActivity.setWorkPeriods(new HashSet());
      opSecondActivity.setProjectPlan(opProjectPlan);

      opThirdActivity = new OpActivity();
      opThirdActivity.setSequence(2);
      opThirdActivity.setID(THIRD_ACTIVITY_ID);
      opThirdActivity.setStart(XCalendar.today());
      opThirdActivity.setFinish(XCalendar.today());
      opThirdActivity.setAttachments(new HashSet());
      opThirdActivity.setAssignments(new HashSet());
      opThirdActivity.setWorkPeriods(new HashSet());
      opThirdActivity.setProjectPlan(opProjectPlan);

      HashSet activities = new HashSet();
      activities.add(opFirstActivity);
      opProjectPlan.setActivities(activities);

      opFirstResource = new OpResource();
      opFirstResource.setID(FIRST_RESOURCE_ID);
      opFirstResource.setAbsences(new HashSet());
      opFirstResource.setAvailable((byte) 100);
      opFirstResource.setActivityAssignments(new HashSet());
      opFirstResource.setDescription("First Resource Description");
      opFirstResource.setHourlyRate(8);
      opFirstResource.setName(FIRST_RESOURCE_NAME);

      opSecondResource = new OpResource();
      opSecondResource.setID(SECOND_RESOURCE_ID);
      opSecondResource.setAbsences(new HashSet());
      opSecondResource.setAvailable((byte) 100);
      opSecondResource.setActivityAssignments(new HashSet());
      opSecondResource.setDescription("Second Resource Description");
      opSecondResource.setHourlyRate(8);
      opSecondResource.setName(SECOND_RESOURCE_NAME);

      opFirstWorkPeriod = new OpWorkPeriod();
      opFirstWorkPeriod.setID(FIRST_WORK_PHASE_ID);

      opProject = new OpProjectNode();
      opProject.setType(OpProjectNode.PROJECT);
      opProject.setID(5);
      opProject.setName("Project");
      opProject.setDescription("ProjectDescription");
      opProject.setCreated(XCalendar.today());
      opProject.setPlan(opProjectPlan);

      queryResults = new ArrayList();
   }

   /**
    * Specifies the behaviour of the mocked methods.
    *
    * @param invocation contains the object and the invoked method
    * @return depends on the invoked moethod
    * @throws IllegalArgumentException if no such method was defined in this mapping
    */
   public Object invocationMatch(Invocation invocation) throws IllegalArgumentException {
      String methodName = invocation.invokedMethod.getName();

      if (methodName.equals(LIST_METHOD)) {
         return queryResults;
      }
      // no such method was found
      throw new IllegalArgumentException("Invalid method name:" + methodName + " for this stub");

   }

   /**
    * @throws Exception
    * @see junit.framework.TestCase#tearDown()
    */
   public void tearDown() throws Exception {
      super.tearDown();
   }

   /**
    * Tests if the work phases are retrieved corectly.
    */
   public void testRetrieveWorkPhases() {

      boolean editable = true;
      // one activity for this project plan
      queryResults.clear();
      Calendar calendar = XCalendar.getDefaultCalendar().getCalendar();
      calendar.set(2006, 8, 25); //25 sept 2006
      Date activityStart = new Date(calendar.getTime().getTime());
      opFirstActivity.setStart(activityStart);
      calendar.set(2006, 9, 3); //3 oct 2006
      Date activityFinish = new Date(calendar.getTime().getTime());
      opFirstActivity.setFinish(activityFinish);
      opFirstActivity.setBaseEffort(56);
      queryResults.add(opFirstActivity);

      // prepare workPeriod
      Set workPeriods = new HashSet();
      workPeriods.add(opFirstWorkPeriod);
      opFirstWorkPeriod.setActivity(opFirstActivity);
      int baseEffort = 8;
      opFirstWorkPeriod.setBaseEffort(baseEffort);
      opFirstWorkPeriod.setStart(activityStart);
      opFirstWorkPeriod.setWorkingDays(415);
      opProjectPlan.setWorkPeriods(workPeriods);

      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ACTIVITY)).will(methodStub);
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(opProjectPlan.getID())});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      XComponent activitiesDataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, opProjectPlan, activitiesDataSet, editable);

      //test if the work phases on data set activity are corectly transformed from work periods
      int index = opFirstWorkPeriod.getActivity().getSequence();
      XComponent activity = (XComponent) activitiesDataSet.getChild(index);
      List workPhaseStarts = OpGanttValidator.getWorkPhaseStarts(activity);
      List workPhaseFinishes = OpGanttValidator.getWorkPhaseFinishes(activity);
      List workPhaseEfforts = OpGanttValidator.getWorkPhaseBaseEfforts(activity);

      assertEquals("Work Phase start not transformed corectly", 2, workPhaseStarts.size());
      assertEquals("Work Phase start not transformed corectly", activityStart, workPhaseStarts.get(0));
      assertEquals("Work Phase start not transformed corectly", new Date(activityStart.getTime() + 7 * XCalendar.MILLIS_PER_DAY), workPhaseStarts.get(1));

      assertEquals("Work Phase finish not transformed corectly", 2, workPhaseFinishes.size());
      assertEquals("Work Phase finish not transformed corectly", new Date(activityStart.getTime() + 4 * XCalendar.MILLIS_PER_DAY), workPhaseFinishes.get(0));
      assertEquals("Work Phase finish not transformed corectly", new Date(activityStart.getTime() + 8 * XCalendar.MILLIS_PER_DAY), workPhaseFinishes.get(1));

      assertEquals("Work Phase effort not transformed corectly", 2, workPhaseEfforts.size());
      assertEquals("Work Phase effort not transformed corectly", new Double(baseEffort * 5), workPhaseEfforts.get(0));
      assertEquals("Work Phase effort not transformed corectly", new Double(baseEffort * 2), workPhaseEfforts.get(1));

   }

   public void testGetWorkPeriods() {
      queryResults.clear();
      queryResults.add(opFirstActivity);
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ACTIVITY)).will(methodStub);
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(opProjectPlan.getID())});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);
      XComponent activitiesDataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, opProjectPlan, activitiesDataSet, true);
      XComponent dataRow = (XComponent) activitiesDataSet.getChild(0);

      Calendar calendar = XCalendar.getDefaultCalendar().getCalendar();
      calendar.set(2006, 8, 4); //4 sept 2006
      Date activityStart = new Date(calendar.getTime().getTime());
      calendar.set(2006, 8, 12); //12 sept 2006
      Date activityFinish = new Date(calendar.getTime().getTime());

      OpGanttValidator.setStart(dataRow, activityStart);
      OpGanttValidator.setEnd(dataRow, activityFinish);
      OpGanttValidator.setDuration(dataRow, 8 * 7);
      OpGanttValidator.setBaseEffort(dataRow, 8 * 7);

      ArrayList workPhaseStarts = new ArrayList();
      workPhaseStarts.add(activityStart);
      workPhaseStarts.add(new Date(activityStart.getTime() + 7 * XCalendar.MILLIS_PER_DAY));
      ArrayList workPhaseFinishes = new ArrayList();
      workPhaseFinishes.add(new Date(activityStart.getTime() + 4 * XCalendar.MILLIS_PER_DAY));
      workPhaseFinishes.add(activityFinish);
      ArrayList workPhaseEfforts = new ArrayList();
      workPhaseEfforts.add(new Double(40));
      workPhaseEfforts.add(new Double(16));
      OpGanttValidator.setWorkPhaseStarts(dataRow, workPhaseStarts);
      OpGanttValidator.setWorkPhaseFinishes(dataRow, workPhaseFinishes);
      OpGanttValidator.setWorkPhaseBaseEfforts(dataRow, workPhaseEfforts);

      Map periods = OpActivityDataSetFactory.getWorkPeriods(dataRow);

      Set startDates = periods.keySet();
      assertEquals("More that one period for activity", 1, startDates.size());

      long periodStartTime = activityStart.getTime() - (activityStart.getTime() % (OpWorkPeriod.PERIOD_LENGTH * XCalendar.MILLIS_PER_DAY));
      Date periodStart = (Date) startDates.iterator().next();
      assertEquals("Wrong period start", new Date(periodStartTime), periodStart);

      List periodValues = (List) periods.get(periodStart); //work day | base effort
      assertEquals("Wrong base effort/day ", new Double(8), periodValues.get(1));

      long workingDays = ((Long) periodValues.get(0)).longValue();
      long expectedWorkingDays = 0;
      int daysFromPeriodStart = (int) ((activityStart.getTime() - periodStartTime) / XCalendar.MILLIS_PER_DAY);
      for (long i = 0; i < 5; i++) {
         expectedWorkingDays |= (1L << (daysFromPeriodStart + i));
      }
      expectedWorkingDays |= (1L << (daysFromPeriodStart + 7));
      expectedWorkingDays |= (1L << (daysFromPeriodStart + 8));

      assertTrue("Working Days were not set correct", expectedWorkingDays - workingDays == 0);
   }

   /**
    * Tests the retrieval of assignments for an activity = resources and resource base efforts columns.
    */
   public void testRetrieveAssignments() {
      boolean editable = true;
      double baseEff = 3452;

      // one activity for this project plan
      queryResults.clear();
      queryResults.add(opFirstActivity);

      // prepare the assignments to be found for this activity
      OpAssignment assignmet = new OpAssignment();

      // res, activity, base eff for an assignment
      assignmet.setResource(opFirstResource);
      assignmet.setActivity(opFirstActivity);
      assignmet.setBaseEffort(baseEff);
      Set assignments = new HashSet();
      assignments.add(assignmet);
      opProjectPlan.setActivityAssignments(assignments);

      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ACTIVITY)).will(methodStub);
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(opProjectPlan.getID())});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      XComponent activitiesDataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, opProjectPlan, activitiesDataSet, editable);

      assertEqualAssignments(assignments, activitiesDataSet, editable);
   }

   /**
    * Tests the retrieval of dependencies between activities from a project. three links for activities : activity 1 ->
    * activity 2 activity 1 -> activity 3 activity 2 -> activity 3
    */
   public void testRetrieveDependencies() {

      boolean editable = true;

      // 3 activities will be found for this project plan
      queryResults.clear();
      queryResults.add(opFirstActivity);
      queryResults.add(opSecondActivity);
      queryResults.add(opThirdActivity);

      // create 2 dependencies and set the links
      Set dependencies = new HashSet();

      // link from activity one -> activity two
      OpDependency dependency = new OpDependency();
      dependency.setPredecessorActivity(opFirstActivity);
      dependency.setSuccessorActivity(opSecondActivity);
      dependencies.add(dependency);
      // link from activity one -> activity three
      dependency = new OpDependency();
      dependency.setPredecessorActivity(opFirstActivity);
      dependency.setSuccessorActivity(opThirdActivity);
      dependencies.add(dependency);
      // link from activity two -> activity three
      dependency = new OpDependency();
      dependency.setPredecessorActivity(opSecondActivity);
      dependency.setSuccessorActivity(opThirdActivity);
      dependencies.add(dependency);

      opProjectPlan.setDependencies(dependencies);

      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ACTIVITY)).will(methodStub);
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(opProjectPlan.getID())});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      XComponent activitiesDataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, opProjectPlan, activitiesDataSet, editable);

      assertEqualLinks(dependencies, activitiesDataSet, editable);
   }

   /**
    * Tests the behaviour of retrieveActivityDataSet for one activity in the data set
    */
   public void testRetrieveActivityDataSet() {

      boolean editable = true;
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ACTIVITY)).will(methodStub);
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(opProjectPlan.getID())});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      queryResults.clear();
      queryResults.add(opFirstActivity);

      XComponent activitiesDataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, opProjectPlan, activitiesDataSet, editable);

      assertEquals("Not the same size for xactivities and data set", queryResults.size(), activitiesDataSet
           .getChildCount());
      for (int i = 0; i < queryResults.size(); i++) {
         assertEqualActivities((OpActivity) queryResults.get(i), (XComponent) activitiesDataSet.getChild(i), editable);
      }

   }

   /**
    * Tests the behaviour of activities
    */
   public void testRetriveActivitiesForProjectPlan() {
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      activities.add(opSecondActivity);
      activities.add(opThirdActivity);

      // add activities set to project plan
      opProjectPlan.setActivities(activities);
      Map activitiesMap = OpActivityDataSetFactory.activities(opProjectPlan);
      assertEquals("The number of activities for the project plan is not correct ", activities.size(), activitiesMap
           .size());

      Object[] activitiesArray = activities.toArray();
      Object[] expectedActivitiesArray = activities.toArray();

      for (int index = 0; index < activitiesArray.length; index++) {
         assertEqualActivities((OpActivity) activitiesArray[index], (OpActivity) expectedActivitiesArray[index]);
      }
   }

   /**
    * Tests the behaviour of activities
    */
   public void testRetriveEmptyActivitiesMapForProjectPlan() {
      // add activities set to project plan
      opProjectPlan.setActivities(new HashSet());
      Map activitiesMap = OpActivityDataSetFactory.activities(opProjectPlan);
      assertEquals("The number of activities for the project plan is not correct ", 0, activitiesMap.size());
   }


   /**
    * Tests the behaviour of storeActivityDataSet for a newly created project plan with a containing <code>OpActivity.COLLECTION</code> activity.
    * An assignment for a standard activity is also added
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testStoreActivityDataSet1() {

      double baseEffort = 40.0;//base effort

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setProjectNode(opProject);
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setProgressTracked(true);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION, 0, baseEffort);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort);

      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow2, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      OpGanttValidator.setResourceBaseEfforts(dataRow2, resouceBaseEfforts);
      //add activities to data set
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      /*expectations*/
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(0, baseEffort, 0, baseEffort));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(0, baseEffort));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, projectPlan, null);

   }

   /**
    * Tests the behaviour of storeActivityDataSet for a newly created project plan with a containing <code>OpActivity.COLLECTION</code> activity.
    * <p/>
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testStoreActivityDataSet2() {

      double baseEffort = 40.0;//base effort

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setProjectNode(opProject);
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setProgressTracked(true);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION, 0, baseEffort);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort / 2);
      XComponent dataRow3 = newActivity(THIRD_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort / 2);
      //add activities to data set
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      dataSet.addChild(dataRow3);

      /*expectations*/
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(0, baseEffort, 0, baseEffort));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(0, baseEffort / 2, 0, baseEffort / 2));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), projectPlan, null);

   }

   /**
    * Tests the behaviour of storeActivityDataSet for a newly created project plan with a containing <code>OpActivity.STANDARD</code> activity.
    * The activity contains also a new assignment
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testStoreActivityDataSet3() {

      double baseEffort = 40.0;//base effort

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setProjectNode(opProject);
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setProgressTracked(true);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort);
      //new activity assignment
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);
      //add activity to data set
      dataSet.addChild(dataRow);

      /*expectations*/
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(0, baseEffort, 0, baseEffort));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(0, baseEffort));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, projectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet for an existent project plan with a containing <code>OpActivity.COLLECTION_TASK</code> activity.
    * The task activity contains also a new assignment.
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testStoreActivityDataSet4() {

      double baseEffort = 0.0;//base effort for task and collection task

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setProjectNode(opProject);
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setProgressTracked(true);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION_TASK, 0, baseEffort);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.TASK, 0, baseEffort);
      //new task assignment
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow2, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      OpGanttValidator.setResourceBaseEfforts(dataRow2, resouceBaseEfforts);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      /*expectations*/
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(0, baseEffort, 0, baseEffort));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(0, baseEffort));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, opProjectPlan, null);

   }

   /**
    * Tests the behaviour of storeActivityDataSet for a newly created project plan without activities
    */
   public void testStoreActivityDataSet5() {

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setProjectNode(opProject);
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(projectPlan));
      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), projectPlan, null);

   }

   /**
    * Tests the behaviour of storeActivityDataSet for a newly created project plan with a containing <code>OpActivity.STANDARD</code> activity.
    * The activity contains also a 2 new assignments
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testStoreActivityDataSet6() {

      double baseEffort = 40.0;//base effort

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setProjectNode(opProject);
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setProgressTracked(true);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);
      projectPlanResources.put(new Long(opSecondResource.getID()), opSecondResource);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort);
      //new activity assignment
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      resources.add(SECOND_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      resouceBaseEfforts.add(new Double(baseEffort / 2));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);
      //add activity to data set
      dataSet.addChild(dataRow);

      /*expectations*/
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(0, baseEffort, 0, baseEffort));
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(0, baseEffort));
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(0, baseEffort / 2));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, projectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet for a newly created project plan with a containing <code>OpActivity.STANDARD</code> activity.
    * The activity contains also a new assignment and is completed 50%.
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testStoreActivityDataSet7() {

      double baseEffort = 40.0;//base effort
      double complete = 50.0;
      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setProjectNode(opProject);
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setProgressTracked(false);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, complete, baseEffort);
      //new activity assignment
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);
      //add activity to data set
      dataSet.addChild(dataRow);

      /*expectations*/
      double expectedRemaining = baseEffort - (baseEffort * complete) / 100;
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(complete, baseEffort, 0, expectedRemaining));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(complete, expectedRemaining));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, projectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet for a newly created project plan with a containing <code>OpActivity.COLLECTION</code> activity completed 75%.
    * An assignment for a standard activity is also added
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testStoreActivityDataSet8() {

      double baseEffort = 40.0;//base effort
      double complete = 75.0; // %completed

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setProjectNode(opProject);
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setProgressTracked(false);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION, complete, baseEffort);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.STANDARD, complete, baseEffort);

      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow2, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      OpGanttValidator.setResourceBaseEfforts(dataRow2, resouceBaseEfforts);
      //add activities to data set
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      /*expectations*/
      double expectedRemaining = baseEffort - (baseEffort * complete) / 100;
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(complete, baseEffort, 0, expectedRemaining));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(complete, expectedRemaining));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, projectPlan, null);

   }

   /**
    * Tests the behaviour of storeActivityDataSet for an existent project plan with a containing <code>OpActivity.COLLECTION_TASK</code> activity.
    * The task activity contains also a new assignment.
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testStoreActivityDataSet9() {

      double baseEffort = 0.0;//base effort for task and collection task
      double complete1 = 100;
      double complete2 = 0;

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setProjectNode(opProject);
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setProgressTracked(false);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION_TASK, 50, baseEffort);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.TASK, complete1, baseEffort);
      XComponent dataRow3 = newActivity(THIRD_ACTIVITY_LOCATOR, OpActivity.TASK, complete2, baseEffort);
      //new task assignment
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow2, resources);
      OpGanttValidator.setResources(dataRow3, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      OpGanttValidator.setResourceBaseEfforts(dataRow2, resouceBaseEfforts);
      OpGanttValidator.setResourceBaseEfforts(dataRow3, resouceBaseEfforts);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      dataSet.addChild(dataRow3);

      /*expectations*/

      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(50, baseEffort, 0, baseEffort));
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(complete1, baseEffort, 0, baseEffort));
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(complete2, baseEffort, 0, baseEffort));
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(complete1, baseEffort));
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(complete2, baseEffort));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, projectPlan, null);

   }

   /**
    * Tests the behaviour of storeActivityDataSet for a newly created project plan with a containing <code>OpActivity.COLLECTION</code> activity.
    * Two assignments for the standard activity are also added
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testStoreActivityDataSet10() {

      double baseEffort = 40.0;//base effort
      double complete = 0; // %completed

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setProjectNode(opProject);
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setProgressTracked(false);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);
      projectPlanResources.put(new Long(opSecondResource.getID()), opSecondResource);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION, complete, baseEffort);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.STANDARD, complete, baseEffort);

      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      resources.add(SECOND_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow2, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort / 2));
      resouceBaseEfforts.add(new Double(baseEffort / 2));
      OpGanttValidator.setResourceBaseEfforts(dataRow2, resouceBaseEfforts);
      //add activities to data set
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      /*expectations*/
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityConstraint(complete, baseEffort, 0, baseEffort));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(complete, baseEffort / 2));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, projectPlan, null);

   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the base effort for a <code>OpActivity.STANDARD</code>.
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testUpdateActivityDataSet1() {
      double baseEffort = 40.0; //persisted base effort for the activity
      opFirstActivity.setBaseEffort(baseEffort);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      opProjectPlan.setActivities(activities);
      opProjectPlan.setProgressTracked(true);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort / 2);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);
      dataSet.addChild(dataRow);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(0, baseEffort / 2, 0, baseEffort / 2));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the base effort for a <code>OpActivity.STANDARD</code>.
    * An assignment is also added for the activity.
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testUpdateActivityDataSet2() {
      double baseEffort = 40.0; //persisted base effort for the activity
      opFirstActivity.setBaseEffort(baseEffort);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      opProjectPlan.setActivities(activities);
      opProjectPlan.setProgressTracked(true);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort / 2);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);

      //new activity assignment
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort / 2));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);

      dataSet.addChild(dataRow);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(0, baseEffort / 2, 0, baseEffort / 2));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentConstraint(0, baseEffort / 2));
      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the base effort for a <code>OpActivity.STANDARD</code>.
    * The activity's assignment is also updated for the activity.
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testUpdateActivityDataSet3() {
      double baseEffort = 40.0; //persisted base effort for the activity
      opFirstActivity.setBaseEffort(baseEffort);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      opProjectPlan.setActivities(activities);
      opProjectPlan.setProgressTracked(true);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);
      //project plan assignments
      opProjectPlan.getActivityAssignments().add(createActivityAssignment(opFirstActivity, opFirstResource, baseEffort, 0));

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort / 2);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);

      //update activity assignment
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort / 2));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);

      dataSet.addChild(dataRow);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(0, baseEffort / 2, 0, baseEffort / 2));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createAssignmentConstraint(0, baseEffort / 2));
      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the base effort for a <code>OpActivity.STANDARD</code> activity
    * within collection. The activity's assignment is also modified
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testUpdateActivityDataSet4() {
      double baseEffort = 40.0; //persisted base effort for the activity
      //persisted project plan activities
      opFirstActivity.setType(OpActivity.COLLECTION);
      opFirstActivity.setBaseEffort(baseEffort);
      opSecondActivity.setType(OpActivity.STANDARD);
      opSecondActivity.setBaseEffort(baseEffort);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      activities.add(opSecondActivity);
      opProjectPlan.setActivities(activities);
      opProjectPlan.setProgressTracked(true);

      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);
      //project plan assignments
      opProjectPlan.getActivityAssignments().add(createActivityAssignment(opSecondActivity, opFirstResource, baseEffort, 0));

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION, 0, baseEffort / 2);
      dataRow1.setStringValue(FIRST_ACTIVITY_LOCATOR);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort / 2);
      dataRow2.setStringValue(SECOND_ACTIVITY_LOCATOR);

      //update activity assignment
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow2, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort / 2));
      OpGanttValidator.setResourceBaseEfforts(dataRow2, resouceBaseEfforts);

      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(0, baseEffort / 2, 0, baseEffort / 2));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createAssignmentConstraint(0, baseEffort / 2));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the base effort for a <code>OpActivity.STANDARD</code> activity
    * within collection .
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testUpdateActivityDataSet5() {
      double baseEffort = 40.0; //persisted base effort for the activity
      //persisted project plan activities
      opFirstActivity.setType(OpActivity.COLLECTION);
      opFirstActivity.setBaseEffort(baseEffort);
      opSecondActivity.setType(OpActivity.STANDARD);
      opSecondActivity.setBaseEffort(baseEffort);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      activities.add(opSecondActivity);
      opProjectPlan.setActivities(activities);
      //progress tracking is on
      opProjectPlan.setProgressTracked(true);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION, 0, baseEffort / 2);
      dataRow1.setStringValue(FIRST_ACTIVITY_LOCATOR);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort / 2);
      dataRow2.setStringValue(SECOND_ACTIVITY_LOCATOR);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(0, baseEffort / 2, 0, baseEffort / 2));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when nothing is updated for a <code>OpActivity.TASK</code> activity
    * within collection .
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testUpdateActivityDataSet6() {
      double baseEffort = 0.0; //persisted base effort for the activity
      //persisted project plan activities
      opFirstActivity.setType(OpActivity.COLLECTION_TASK);
      opFirstActivity.setBaseEffort(baseEffort);
      opSecondActivity.setType(OpActivity.TASK);
      opSecondActivity.setBaseEffort(baseEffort);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      activities.add(opSecondActivity);
      opProjectPlan.setActivities(activities);
      //progress tracking is on
      opProjectPlan.setProgressTracked(true);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION_TASK, 0, baseEffort);
      dataRow1.setStringValue(FIRST_ACTIVITY_LOCATOR);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.TASK, 0, baseEffort);
      dataRow2.setStringValue(SECOND_ACTIVITY_LOCATOR);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(0, baseEffort, 0, baseEffort));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the base effort for a <code>OpActivity.STANDARD</code>.
    * The activity's assignments are also updated for the activity.
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testUpdateActivityDataSet7() {
      double baseEffort = 40.0; //persisted base effort for the activity
      opFirstActivity.setBaseEffort(baseEffort);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      opProjectPlan.setActivities(activities);
      opProjectPlan.setProgressTracked(true);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);
      projectPlanResources.put(new Long(opSecondResource.getID()), opSecondResource);
      //project plan assignments
      opProjectPlan.getActivityAssignments().add(createActivityAssignment(opFirstActivity, opFirstResource, baseEffort, 0));
      opProjectPlan.getActivityAssignments().add(createActivityAssignment(opFirstActivity, opSecondResource, baseEffort, 0));

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort / 2);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);

      //update activity assignment
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      resources.add(SECOND_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort / 2));
      resouceBaseEfforts.add(new Double(baseEffort / 3));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);

      dataSet.addChild(dataRow);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(0, baseEffort / 2, 0, baseEffort / 2));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createAssignmentConstraint(0, baseEffort / 2));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createAssignmentConstraint(0, baseEffort / 3));
      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the base effort for a <code>OpActivity.STANDARD</code>.
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testUpdateActivityDataSet8() {
      double baseEffort = 40.0; //persisted base effort for the activity
      opFirstActivity.setBaseEffort(baseEffort);
      opFirstActivity.setComplete(0);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      opProjectPlan.setActivities(activities);
      opProjectPlan.setProgressTracked(false);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort / 2);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);
      dataSet.addChild(dataRow);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(0, baseEffort / 2, 0, baseEffort / 2));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the %completed for a <code>OpActivity.STANDARD</code>.
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testUpdateActivityDataSet9() {
      double baseEffort = 40.0; //persisted base effort for the activity
      opFirstActivity.setBaseEffort(baseEffort);
      opFirstActivity.setComplete(75);

      Set activities = new HashSet();
      activities.add(opFirstActivity);
      opProjectPlan.setActivities(activities);
      opProjectPlan.setProgressTracked(false);

      double newComplete = 25; //new %completed from the UI
      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, newComplete, baseEffort);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);
      dataSet.addChild(dataRow);
      /*expectations*/
      double expectedRemaining = baseEffort - (baseEffort * newComplete) / 100;
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(newComplete, baseEffort, 0, expectedRemaining));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the base effort for a <code>OpActivity.STANDARD</code> activity
    * within collection .
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testUpdateActivityDataSet10() {
      double baseEffort = 40.0; //persisted base effort for the activity
      //persisted project plan activities
      opFirstActivity.setType(OpActivity.COLLECTION);
      opFirstActivity.setBaseEffort(baseEffort);
      opSecondActivity.setType(OpActivity.STANDARD);
      opSecondActivity.setBaseEffort(baseEffort);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      activities.add(opSecondActivity);
      opProjectPlan.setActivities(activities);
      //progress tracking is on
      opProjectPlan.setProgressTracked(false);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION, 0, baseEffort / 2);
      dataRow1.setStringValue(FIRST_ACTIVITY_LOCATOR);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort / 2);
      dataRow2.setStringValue(SECOND_ACTIVITY_LOCATOR);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      /*%complete is 0 so remainingEffort is base effort */
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(0, baseEffort / 2, 0, baseEffort / 2));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the %completed for a <code>OpActivity.STANDARD</code> activity
    * within collection .
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testUpdateActivityDataSet11() {
      double baseEffort = 40.0; //persisted base effort for the activity
      //persisted project plan activities
      opFirstActivity.setType(OpActivity.COLLECTION);
      opFirstActivity.setBaseEffort(baseEffort);
      opFirstActivity.setComplete(25);
      opSecondActivity.setType(OpActivity.STANDARD);
      opSecondActivity.setBaseEffort(baseEffort);
      opSecondActivity.setComplete(25);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      activities.add(opSecondActivity);
      opProjectPlan.setActivities(activities);
      //progress tracking is on
      opProjectPlan.setProgressTracked(false);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION, 0, baseEffort);
      dataRow1.setStringValue(FIRST_ACTIVITY_LOCATOR);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, baseEffort);
      dataRow2.setStringValue(SECOND_ACTIVITY_LOCATOR);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      /*%complete becomed 0 so remainingEffort is base effort */
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(0, baseEffort, 0, baseEffort));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the base effort for a <code>OpActivity.STANDARD</code>.
    * The activity's assignments base efforts are also updated for the activity.
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testUpdateActivityDataSet12() {
      double baseEffort = 40.0; //persisted base effort for the activity
      double complete = 50; //persisted %completed
      opFirstActivity.setBaseEffort(baseEffort);
      opFirstActivity.setComplete(complete);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      opProjectPlan.setActivities(activities);
      opProjectPlan.setProgressTracked(false);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(opFirstResource.getID()), opFirstResource);
      projectPlanResources.put(new Long(opSecondResource.getID()), opSecondResource);
      //project plan assignments
      opProjectPlan.getActivityAssignments().add(createActivityAssignment(opFirstActivity, opFirstResource, baseEffort, complete));
      opProjectPlan.getActivityAssignments().add(createActivityAssignment(opFirstActivity, opSecondResource, baseEffort, complete));

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, complete, baseEffort / 2);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);

      //update activity assignment
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      resources.add(SECOND_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort / 2));
      resouceBaseEfforts.add(new Double(baseEffort / 3));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);

      dataSet.addChild(dataRow);
      /*expectations*/
      double expectedActivityRemaining = (baseEffort / 2) - ((baseEffort / 2) * complete) / 100;
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(complete, baseEffort / 2, 0, expectedActivityRemaining));
      double expectedAssignment1Remaining = (baseEffort / 2) - ((baseEffort / 2) * complete) / 100;
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createAssignmentConstraint(complete, expectedAssignment1Remaining));
      double expectedAssignment2Remaining = (baseEffort / 3) - ((baseEffort / 3) * complete) / 100;
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createAssignmentConstraint(complete, expectedAssignment2Remaining));
      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, projectPlanResources, opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when updating the complete for a <code>OpActivity.MILESTONE</code>.
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testUpdateActivityDataSet13() {
      double baseEffort = 0.0; //persisted base effort for the activity
      opFirstActivity.setBaseEffort(baseEffort);
      opFirstActivity.setComplete(0);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      opProjectPlan.setActivities(activities);
      opProjectPlan.setProgressTracked(false);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 100, baseEffort);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);
      dataSet.addChild(dataRow);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(100, baseEffort, 0, 0));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), opProjectPlan, null);
   }

   /**
    * Tests the behaviour of storeActivityDataSet when %completed is updated for a <code>OpActivity.TASK</code> activity
    * within collection .
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testUpdateActivityDataSet14() {
      double baseEffort = 0.0; //persisted base effort for the activity
      //persisted project plan activities
      opFirstActivity.setType(OpActivity.COLLECTION_TASK);
      opFirstActivity.setBaseEffort(baseEffort);
      opSecondActivity.setType(OpActivity.TASK);
      opSecondActivity.setBaseEffort(baseEffort);
      Set activities = new HashSet();
      activities.add(opFirstActivity);
      activities.add(opSecondActivity);
      opProjectPlan.setActivities(activities);
      //progress tracking is off
      opProjectPlan.setProgressTracked(false);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION_TASK, 100, baseEffort);
      dataRow1.setStringValue(FIRST_ACTIVITY_LOCATOR);
      XComponent dataRow2 = newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.TASK, 100, baseEffort);
      dataRow2.setStringValue(SECOND_ACTIVITY_LOCATOR);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanConstraint(opProjectPlan));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityConstraint(100, baseEffort, 0, 0));

      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), opProjectPlan, null);
   }

   /**
    * Tests the behaviour of retrieveResourceDataSet for one resource in the data set
    */
   public void testRetrieveResourceDataSet() {

      HashMap resourcesMap = new HashMap();

      resourcesMap.put(new Long(FIRST_RESOURCE_ID), opFirstResource);
      XComponent resourcesDataSet = new XComponent(XComponent.DATA_SET);

      OpActivityDataSetFactory.retrieveResourceDataSet(resourcesMap, resourcesDataSet);

      assertEquals("One resource should be found in the data set", 1, resourcesDataSet.getChildCount());
      XComponent resource = (XComponent) resourcesDataSet.getChild(0);
      assertEquals("Available values are not the same ", opFirstResource.getAvailable(), OpGanttValidator
           .getAvailable(resource), 0);
      assertEquals("Hourly rates values are not the same ", opFirstResource.getHourlyRate(), OpGanttValidator
           .getHourlyRate(resource), 0);

      // todo OpResource has more fields that are not "translated" into component data row.
   }

   /**
    * Tests the behaviour of retrieveResourceDataSet for one existent project
    */
   public void testRetrieveResourceDataSetForProject() {

      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_RESOURCES_FOR_PROJECT)).will(methodStub);
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(opProject.getID())});
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(methodStub);

      // iterate should return resurces
      queryResults.clear();
      queryResults.add(opFirstResource);

      XComponent resourcesDataSet = new XComponent(XComponent.DATA_SET);

      OpActivityDataSetFactory.retrieveResourceDataSet(broker, opProject, resourcesDataSet);
      assertEquals("One resource should be found in the data set", 1, resourcesDataSet.getChildCount());
      for (int i = 0; i < resourcesDataSet.getChildCount(); i++) {
         assertEqualResources((XComponent) resourcesDataSet.getChild(i), opFirstResource);
      }

   }

   /**
    * Asserts that the given data set has the expected workPhases
    *
    * @param resource         a <code>XComponent.DATA_ROW<code> representing the resource being checked
    * @param expectedResource a <code>OpResource</code>  representing the expected resource
    */
   private static void assertEqualResources(XComponent resource, OpResource expectedResource) {

      assertEquals("The 2 resources don't have the same name ", XValidator.choice(expectedResource.locator(),
           expectedResource.getName()), resource.getStringValue());
      assertEquals("Available values are not the same ", expectedResource.getAvailable(), OpGanttValidator
           .getAvailable(resource), 0);
      assertEquals("Hourly rates values are not the same ", expectedResource.getHourlyRate(), OpGanttValidator
           .getHourlyRate(resource), 0);

   }

   /**
    * Asserts that the given data set has the expected assignments.
    *
    * @param assignments       <code>Set</code> of expected <code>XAssignments</code>
    * @param activitiesDataSet the tested activity data set
    * @param editable          true if editable mode is on
    */
   private static void assertEqualAssignments(Set assignments, XComponent activitiesDataSet, boolean editable) {
      for (Iterator iterator = assignments.iterator(); iterator.hasNext();) {

         OpAssignment opAssignment = (OpAssignment) iterator.next();
         int indexActivity = opAssignment.getActivity().getSequence();
         XComponent testedActivity = (XComponent) activitiesDataSet.getChild(indexActivity);
         assertEqualActivities(opAssignment.getActivity(), testedActivity, editable);

         // test resource locators and resource base efforts for this activity
         ArrayList testedResources = OpGanttValidator.getResources(testedActivity);
         OpResource expectedResource = opAssignment.getResource();
         ArrayList testedResourceBaseEfforts = OpGanttValidator.getResourceBaseEfforts(testedActivity);
         double expectedBaseEffort = opAssignment.getBaseEffort();
         boolean foundRess = false;
         boolean foundEffort = false;
         for (int i = 0; i < testedResources.size(); i++) {
            String testedResource = OpGanttValidator.getResourceName((String) testedResources.get(i),"%");
            if (XValidator.choice(expectedResource.locator(), expectedResource.getName()).equals(testedResource)) {
               foundRess = true;
               if (((Double) testedResourceBaseEfforts.get(i)).doubleValue() == expectedBaseEffort) {
                  foundEffort = true;
               }
               break;
            }
         }

         assertTrue("The expected resource was not found on the activity ", foundRess);
         assertTrue("The expected resource base effort was not found on the activity ", foundEffort);
      }

      // test for extra assignmets
      for (int i = 0; i < activitiesDataSet.getChildCount(); i++) {
         XComponent testedActivity = (XComponent) activitiesDataSet.getChild(i);
         ArrayList res = OpGanttValidator.getResources(testedActivity);
         for (int j = 0; j < res.size(); j++) {
            String testedResource = OpGanttValidator.getResourceName((String) res.get(j),"%");
            // see if it's in any of the assignments
            boolean foundRess = false;
            for (Iterator iterator = assignments.iterator(); iterator.hasNext();) {
               OpAssignment opAssignment = (OpAssignment) iterator.next();
               OpResource expectedResource = opAssignment.getResource();
               if (XValidator.choice(expectedResource.locator(), expectedResource.getName()).equals(testedResource)) {
                  foundRess = true;
               }
            }
            assertTrue(" There are extra resources in the data set ", foundRess);
         }
      }
   }

   /**
    * Asserts that the given data set has the expected dependencies.
    *
    * @param dependencies <code>Set</code> of expected <code>XDependencies</code>
    * @param dataSet      <code>XComponent</code> with the tested data
    * @param editable     edit mode
    */
   private static void assertEqualLinks(Set dependencies, XComponent dataSet, boolean editable) {
      boolean found;
      int activityIndex;
      for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
         OpDependency dep = (OpDependency) iterator.next();
         OpActivity opSuccessor = dep.getSuccessorActivity();
         OpActivity opPredecessor = dep.getPredecessorActivity();
         int indexSucc = opSuccessor.getSequence();
         int indexPred = opPredecessor.getSequence();

         // opPredecessor has opSuccessor as successor
         XComponent componentPred = (XComponent) dataSet.getChild(indexPred);
         ArrayList successorsOfPred = OpGanttValidator.getSuccessors(componentPred);
         found = false;
         XComponent activity = null;
         for (int i = 0; i < successorsOfPred.size(); i++) {
            activityIndex = ((Integer) successorsOfPred.get(i)).intValue();
            if (activityIndex == indexSucc) {
               found = true;
               activity = (XComponent) dataSet.getChild(activityIndex);
               break;
            }
         }
         assertTrue("The successor hasn't been found for activity " + indexPred, found);
         assertEqualActivities(opSuccessor, activity, editable);

         // and opSuccessor has opPredecessor as predecessor
         XComponent componentSucc = (XComponent) dataSet.getChild(indexSucc);
         ArrayList predecessorsOfSucc = OpGanttValidator.getPredecessors(componentSucc);
         found = false;
         activity = null;
         for (int i = 0; i < predecessorsOfSucc.size(); i++) {
            activityIndex = ((Integer) predecessorsOfSucc.get(i)).intValue();
            if (activityIndex == indexPred) {
               found = true;
               activity = (XComponent) dataSet.getChild(activityIndex);
               break;
            }
         }
         assertTrue("The predecessor hasn't been found for activity " + indexSucc, found);
         assertEqualActivities(opPredecessor, activity, editable);
      }

      // todo: test for "extra" links (like for assignments)
   }

   /**
    * Asserts that the given activity has the expected field values
    *
    * @param activity         the activity being tested instance of <code>OpActivity</code>
    * @param expectedActivity the expected activity instance of <code>OpActivity</code>
    */
   private static void assertEqualActivities(OpActivity activity, OpActivity expectedActivity) {

      assertEquals("Activities version locator does not match", activity.locator(), expectedActivity.locator());
      assertEquals("Activities outline level does not match ", activity.getOutlineLevel(), expectedActivity
           .getOutlineLevel());

      // check name
      assertEquals("Activities Name does not match", activity.getName(), expectedActivity.getName());

      // description
      assertEquals("Activities Description does not match ", activity.getDescription(), expectedActivity
           .getDescription());

      // type
      assertEquals("Activities Type does not match ", activity.getType(), expectedActivity.getType());

      // complete
      assertEquals("Activities Complete does not match", activity.getComplete(), expectedActivity.getComplete(), 0.0);

      // start date
      assertEquals("Activities Start does not match", activity.getStart(), expectedActivity.getStart());

      // finish date
      assertEquals("Activities Finish does not match", activity.getFinish(), expectedActivity.getFinish());

      // duration
      assertEquals("Activities Duration does not match ", activity.getDuration(), expectedActivity.getDuration(), 0);

      // base effort
      assertEquals("Activities Base Effort does not match ", activity.getBaseEffort(),
           expectedActivity.getBaseEffort(), 0);

      // costs

      assertEquals("Activities Base Personnel Costs does not match ", activity.getBasePersonnelCosts(),
           expectedActivity.getBasePersonnelCosts(), 0);

      assertEquals("Activities Base Travel Costs does not match ", activity.getBaseTravelCosts(), expectedActivity
           .getBaseTravelCosts(), 0);

      assertEquals("Activities Base Material Costs  does not match", activity.getBaseMaterialCosts(), expectedActivity
           .getBaseMaterialCosts(), 0);

      assertEquals("Activities Base External Costs  ", activity.getBaseExternalCosts(), expectedActivity
           .getBaseExternalCosts(), 0);

      assertEquals("Activities Base Miscellaneous Costs does not match ", activity.getBaseMiscellaneousCosts(),
           expectedActivity.getBaseMiscellaneousCosts(), 0);

      // is not done yet in the service as well

   }

   /**
    * Asserts that the given activity has the expected field values
    *
    * @param activity The expected activity <code>OpActivity</code>
    * @param dataRow  The tested activity <code>XComponent</code>
    * @param editable true if editable mode is on
    */
   private static void assertEqualActivities(OpActivity activity, XComponent dataRow, boolean editable) {

      assertEquals("Value for data row != activity locator", activity.locator(), dataRow.getStringValue());
      assertEquals("Value for data row outline level != activity", activity.getOutlineLevel(), dataRow
           .getOutlineLevel());
      assertEquals("Only top-level activities are expanded by default ", activity.getOutlineLevel() == 0, dataRow
           .getVisible());

      // name
      assertEquals("Name ", activity.getName(), OpGanttValidator.getName(dataRow));
      assertEquals("Editable for Name", editable, dataRow.getChild(OpGanttValidator.NAME_COLUMN_INDEX).getEnabled());

      // type
      assertEquals("Type  ", activity.getType(), OpGanttValidator.getType(dataRow));
      assertEquals("Editable for Type", editable, dataRow.getChild(OpGanttValidator.TYPE_COLUMN_INDEX).getEnabled());

      // category
      OpActivityCategory category = activity.getCategory();
      if (category != null) {
         XValidator.choice(category.locator(), category.getName());
         assertEquals("Category  ", XValidator.choice(category.locator(), category.getName()), OpGanttValidator
              .getCategory(dataRow));
      }
      else {
         assertNull("Category  ", ((XComponent) dataRow.getChild(OpGanttValidator.CATEGORY_COLUMN_INDEX)).getValue());
      }
      assertEquals("Editable for Category", editable, dataRow.getChild(OpGanttValidator.CATEGORY_COLUMN_INDEX)
           .getEnabled());

      // complete
      assertEquals("Complete  ", activity.getComplete(), OpGanttValidator.getComplete(dataRow), 0);
      assertEquals("Editable for Complete", false, dataRow.getChild(OpGanttValidator.COMPLETE_COLUMN_INDEX).getEnabled());

      // start date
      assertEquals("Start  ", activity.getStart(), OpGanttValidator.getStart(dataRow));
      assertEquals("Editable for Start", editable, dataRow.getChild(OpGanttValidator.START_COLUMN_INDEX).getEnabled());

      // end date
      assertEquals("Finish  ", activity.getFinish(), OpGanttValidator.getEnd(dataRow));
      assertEquals("Editable for End", editable, dataRow.getChild(OpGanttValidator.END_COLUMN_INDEX).getEnabled());

      // duration
      assertEquals("Duration  ", activity.getDuration(), OpGanttValidator.getDuration(dataRow), 0);
      assertEquals("Editable for Duration", editable, dataRow.getChild(OpGanttValidator.DURATION_COLUMN_INDEX)
           .getEnabled());

      // base effort
      assertEquals("Base Effort  ", activity.getBaseEffort(), OpGanttValidator.getBaseEffort(dataRow), 0);
      assertEquals("Editable for Base Effort", editable, dataRow.getChild(OpGanttValidator.BASE_EFFORT_COLUMN_INDEX)
           .getEnabled());

      // costs
      assertEquals("Base Personnel Costs  ", activity.getBasePersonnelCosts(), OpGanttValidator
           .getBasePersonnelCosts(dataRow), 0);
      assertEquals("Editable for Base Personnel Costs", false, dataRow.getChild(
           OpGanttValidator.BASE_PERSONNEL_COSTS_COLUMN_INDEX).getEnabled());
      assertEquals("Base Travel Costs  ", activity.getBaseTravelCosts(), OpGanttValidator.getBaseTravelCosts(dataRow), 0);
      assertEquals("Editable for Base Travel Costs", editable, dataRow.getChild(
           OpGanttValidator.BASE_TRAVEL_COSTS_COLUMN_INDEX).getEnabled());
      assertEquals("Base Material Costs  ", activity.getBaseMaterialCosts(), OpGanttValidator
           .getBaseMaterialCosts(dataRow), 0);
      assertEquals("Editable for Base Material Costs", editable && (activity.getType() == OpActivity.STANDARD), dataRow
           .getChild(OpGanttValidator.BASE_MATERIAL_COSTS_COLUMN_INDEX).getEnabled());
      assertEquals("Base External Costs  ", activity.getBaseExternalCosts(), OpGanttValidator
           .getBaseExternalCosts(dataRow), 0);
      assertEquals("Editable for Base External Costs", editable && (activity.getType() == OpActivity.STANDARD), dataRow
           .getChild(OpGanttValidator.BASE_MATERIAL_COSTS_COLUMN_INDEX).getEnabled());
      assertEquals("Base Miscellaneous Costs  ", activity.getBaseMiscellaneousCosts(), OpGanttValidator
           .getBaseMiscellaneousCosts(dataRow), 0);
      assertEquals("Editable for Base Miscellaneous Costs", editable && (activity.getType() == OpActivity.STANDARD),
           dataRow.getChild(OpGanttValidator.BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX).getEnabled());
      assertEquals("Description  ", activity.getDescription(), OpGanttValidator.getDescription(dataRow));
      assertEquals("Editable for Description  ", false, dataRow.getChild(OpGanttValidator.DESCRIPTION_COLUMN_INDEX)
           .getEnabled());
      assertEquals("Mode  ", activity.getAttributes(), OpGanttValidator.getAttributes(dataRow));
      assertEquals("Editable for Mode  ", false, dataRow.getChild(OpGanttValidator.MODE_COLUMN_INDEX).getEnabled());

      // attachments
      if (activity.getAttachments() != null) {
         assertEquals("Not the same attachments ammount ", activity.getAttachments().size(), OpGanttValidator
              .getAttachments(dataRow).size());
         ArrayList dataRowAtt = OpGanttValidator.getAttachments(dataRow);
         int i = 0;
         for (Iterator iterator = activity.getAttachments().iterator(); iterator.hasNext();) {
            OpAttachment opAtt = (OpAttachment) iterator.next();
            ArrayList dAtt = (ArrayList) dataRowAtt.get(i);

            if (opAtt.getLinked()) {
               assertEquals("Wrong identifier ", "u", dAtt.get(0));
            }
            else {
               assertEquals("Wrong identifier ", "d", dAtt.get(0));
            }
            assertEquals("Attachment ID ", XValidator.choice(opAtt.locator(), opAtt.getName()), dAtt.get(1));
            assertEquals("Attachment Name", opAtt.getName(), dAtt.get(2));
            assertEquals("Attachment Location", opAtt.getLocation(), dAtt.get(3));

         }
      }
      assertEquals("Editable for Attachments  ", false, dataRow.getChild(OpGanttValidator.ATTACHMENTS_COLUMN_INDEX)
           .getEnabled());

      // todo also check for activities types / editable relation (ex: duration for collections can't be modified) -
      // this
      // is not done yet in the service as well

   }

   /**
    * Creates a new activity
    *
    * @return <code>XComponent</code> - an activity
    */
   public static XComponent newActivity(String name, byte type, double complete, double baseEffort) {

      XComponent data_row = new XComponent(XComponent.DATA_ROW);
      // 0 Name
      XComponent data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setStringValue(name);
      data_row.addChild(data_cell);
      // 1 Type
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setByteValue((byte) type);
      data_row.addChild(data_cell);
      // 2 Default category - default null
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_row.addChild(data_cell);
      // 3 Complete
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(complete);
      data_row.addChild(data_cell);
      // 4 Start
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      Date start = XCalendar.today();
      data_cell.setDateValue(start);
      data_row.addChild(data_cell);
      // 5 End
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDateValue(start);
      data_row.addChild(data_cell);
      // 6 Duration
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);
      // 7 Effort
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(baseEffort);
      data_row.addChild(data_cell);
      // 8 Predecessors
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);
      // 9 Successors
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);
      // 10 Resources
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);
      // 11 Costs
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);

      // 16 Description
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setStringValue("Activity Description");
      data_row.addChild(data_cell);
      // 17 Attachments
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);
      // 18 Mode
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setIntValue(0);
      data_row.addChild(data_cell);
      // 19 Work Phase Begin
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);
      // 20 Work Phase End
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);

      // 21 work phase base
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);

      // 22 Resource Base Efforts
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);

      // 23 Priority
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setByteValue((byte) 1);
      data_row.addChild(data_cell);

      // 24 Workrecords a map of [resourceLocator, hasWorkRecords]
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setValue(new HashMap());
      data_row.addChild(data_cell);

      // 25 Actual effort - needed for %complete
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);

      return data_row;
   }

   /**
    * Returns a new created <code>OpAssignment</code> entity.
    *
    * @param activity   <code>OpActivity</code> the activity for which the assignment is created.
    * @param resource   <code>OpResource</coe> the assignment resource
    * @param baseEffort <code>double</code> base effort for the assignment
    * @param complete   <code>double</code> complete for the assignment
    * @return a new <code>OpAssignment</code> entity
    */
   private OpAssignment createActivityAssignment(OpActivity activity, OpResource resource, double baseEffort, double complete) {
      //create the activity asignment
      OpAssignment assignment = new OpAssignment();
      OpProjectPlan projectPlan = activity.getProjectPlan();
      assignment.setActivity(activity);
      assignment.setResource(resource);
      assignment.setProjectPlan(projectPlan);
      //add the created assignment to activity assignments set
      activity.getAssignments().add(assignment);
      assignment.setBaseEffort(baseEffort);

      if (activity.getProjectPlan().getProgressTracked()) {
         assignment.setActualEffort(0);
         assignment.setComplete(0);
         assignment.setRemainingEffort(baseEffort);
         assignment.setActualCosts(0);
      }
      else {
         assignment.setActualEffort(0);
         assignment.setComplete(complete);
         if (complete == 0) {
            assignment.setRemainingEffort(baseEffort);
         }
         if (complete != 0) { //actual Effort is 0
            assignment.setRemainingEffort(baseEffort - (baseEffort * complete) / 100);
         }
      }
      return assignment;
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if a project plan is the expected one.
    *
    * @return a new project plan constraint
    */
   private Constraint createProjectPlanConstraint(final OpProjectPlan projectPlan) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpProjectPlan)) {
               return false;
            }
            OpProjectPlan projectP = (OpProjectPlan) object;

            // TODO: check the activities set
            if (!projectPlan.getStart().equals(projectP.getStart())) {
               return false;
            }
            return (projectPlan.getFinish().equals(projectP.getFinish()));
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if a project plan is the expected one");
         }
      };
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if an assignment is the expected one.
    *
    * @param complete        <code>double</code> the expected %completed for the assignment
    * @param remainingEffort <code>double</code> the exprected remaining effort for the assignment
    * @return a new assignment constraint
    */
   private Constraint createAssignmentConstraint(final double complete, final double remainingEffort) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpAssignment)) {
               return false;
            }
            OpAssignment assignment = (OpAssignment) object;
            if (assignment.getRemainingEffort() != remainingEffort) {
               return false;
            }
            return assignment.getComplete() == complete;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if an assignment is the expected one");
         }
      };
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if an activity is the expected one.
    *
    * @param complete        <code>double</code> the expected %completed for the activity
    * @param baseEffort      <code>double</code> the expected baseEffort for the activity
    * @param actualEffort    <code>double</code> the expected actuall effort for the activity
    * @param remainingEffort <code>double</code> the expected remaining effort for the activity
    * @return a new activity constraint
    */
   private Constraint createActivityConstraint(final double complete, final double baseEffort, final double actualEffort, final double remainingEffort) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpActivity)) {
               return false;
            }
            OpActivity activity = (OpActivity) object;
            if (activity.getBaseEffort() != baseEffort) {
               return false;
            }
            if (activity.getActualEffort() != actualEffort) {
               return false;
            }
            if (activity.getRemainingEffort() != remainingEffort) {
               return false;
            }
            return activity.getComplete() == complete;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if an activity is the expected one");
         }
      };
   }

}
