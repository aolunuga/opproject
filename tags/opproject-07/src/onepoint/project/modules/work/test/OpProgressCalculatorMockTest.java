/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.work.test;

import onepoint.persistence.OpBroker;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.OpProgressCalculator;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.util.XCalendar;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test case class for OpProgressCalculator.
 *
 * @author ovidiu.lupas
 */
public class OpProgressCalculatorMockTest extends onepoint.project.test.OpBaseMockTestCase {
   /*the assignment project plan*/
   private OpProjectPlan projectPlan;
   /*the assignment resources */
   private OpResource resource1;
   private OpResource resource2;
   /*HSQLs*/
   private static final String SELECT_WORKING_VERSION_ASSIGNMENT = "select assignmentVer from OpAssignmentVersion assignmentVer  inner join assignmentVer.ActivityVersion actVersion inner join actVersion.PlanVersion planVer  where assignmentVer.Resource.ID = ? and actVersion.Activity.ID = ? and planVer.VersionNumber = ?";
   private static final String SELECT_WORKING_VERSION_ACTIVITY = "select actVersion from OpActivityVersion actVersion  inner join actVersion.PlanVersion planVer  where actVersion.Activity.ID = ? and planVer.VersionNumber = ?";

   public Object invocationMatch(Invocation invocation) throws IllegalArgumentException {
      return null;
   }

   /**
    * Set up the test case
    */
   public void setUp() {

      super.setUp();
      //an empty list of results
      queryResults = new ArrayList();

      //project plan with progressTracked true by default
      projectPlan = new OpProjectPlan();
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setActivityAssignments(new HashSet());
      projectPlan.setActivityAttachments(new HashSet());
      projectPlan.setDependencies(new HashSet());
      projectPlan.setWorkPeriods(new HashSet());

      //create the resource
      resource1 = new OpResource();
      resource1.setName("ResourceName");
      resource1.setDescription("ResourceDescription");
      resource1.setAvailable((byte) 100);
      resource1.setHourlyRate(0);
      resource1.setInheritPoolRate(true);
      resource1.setProjectNodeAssignments(new HashSet());

      //create the resource
      resource2 = new OpResource();
      resource2.setName("ResourceName");
      resource2.setDescription("ResourceDescription");
      resource2.setAvailable((byte) 100);
      resource2.setHourlyRate(0);
      resource2.setInheritPoolRate(true);
      resource2.setProjectNodeAssignments(new HashSet());
   }
   /******************************************************************************************************************/
   /*                                     PROGRESS TRACKING ON Tests                                                 */
   /******************************************************************************************************************/
   /**
    * Tests that a new work record (but not completed) will be correctly inserted.
    * An assignment exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> on .
    */
   public void testAddNewWorkRecord1() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort

      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);

      double actualEffort = 20;
      double remainingEffort = baseEffort - actualEffort;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*compute the expected assignment complete */
      double assignmentComplete = (actualEffort * 100) / (actualEffort + remainingEffort);
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);

      /*compute the expected activity complete*/
      double activityComplete = (actualEffort * 100) / (actualEffort + remainingEffort);
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a new completed work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> on .
    */
   public void testAddNewWorkRecord2() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort

      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);

      double actualEffort = 20 ; //but the complete check box is enabled
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0,true);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete */
      double assignmentComplete = 100;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);

      /*compute the expected activity complete*/
      double activityComplete = (actualEffort * 100) / (actualEffort + remainingEffort);
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a new work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.TASK</code>or <code>OpActivity.MILESTONE</code>
    * of a project plan with <code>progressTracked</code> on .
    */
   public void testAddNewWorkRecord3() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 0;

      OpActivity taskActivity = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(taskActivity, resource1, baseEffort, 0);

      double actualEffort = 20;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, 0, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(taskActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment %complete value */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);

      /*the expected activity %complete value */
      double activityComplete = 0;
      assertEquals("Task Activity wrong %completed value", activityComplete, taskActivity.getComplete(), 0);
      assertActivityCostsEquality(taskActivity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a new work record will be correctly inserted.The completed check box is selected
    * An assignment exists for a <code>OpActivity.TASK</code>or <code>OpActivity.MILESTONE</code>
    * of a project plan with <code>progressTracked</code> on .
    */
   public void testAddNewWorkRecord4() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 0;

      OpActivity milestoneActivity = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(milestoneActivity, resource1, baseEffort, 0);

      OpWorkRecord workRecord = createWorkRecord(assignment, 0, 0, 0, 0, 0, 0, true);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(milestoneActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment %complete value */
      double assignmentComplete = 100;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);

      /*the expected milestone activity %complete value */
      double activityComplete = 100;
      assertEquals("Task Activity wrong %completed value", activityComplete, milestoneActivity.getComplete(), 0);
      assertActivityCostsEquality(milestoneActivity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a new work record (but not completed) will be correctly inserted.
    * An assignment exists for a <code>OpActivity.STANDARD</code> from a collection of a project plan with <code>progressTracked</code> on .
    */
   public void testAddNewWorkRecord5() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      collectionActivity.setSubActivities(subActivities);

      activity.setSuperActivity(collectionActivity);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);

      double actualEffort = 20;
      double remainingEffort = baseEffort - actualEffort;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 1000, 1000, 1000, 500, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*compute the expected assignment complete */
      double assignmentComplete = (actualEffort * 100) / (actualEffort + remainingEffort);
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);

      /*compute the expected activity complete*/
      double activityComplete = (actualEffort * 100) / (actualEffort + remainingEffort);
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 1000, 1000, 1000, 500);
      /*compute the expected collection activity complete*/
      double collectionActivityComplete = (actualEffort * 100) / (actualEffort + remainingEffort);
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 1000, 1000, 1000, 500);
   }

    /**
    * Tests that a new completed work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.STANDARD</code> from a collection of a project plan with <code>progressTracked</code> on .
    */
   public void testAddNewWorkRecord6() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      collectionActivity.setSubActivities(subActivities);

      activity.setSuperActivity(collectionActivity);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);

      double actualEffort = 0;
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 1000, 1000, 1000, 500, true);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete */
      double assignmentComplete = 100;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);

      /*compute the expected activity complete*/
      double activityComplete = 100 * (baseEffort - remainingEffort) / baseEffort;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 1000, 1000, 1000, 500);
      /*compute the expected collection activity complete*/
      double collectionActivityComplete = 100 * (baseEffort - remainingEffort)/baseEffort;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 1000, 1000, 1000, 500);
   }

   /**
    * Tests that a new completed work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.STANDARD</code> from a collection of a project plan with <code>progressTracked</code> on .
    * The collection contains a second activity with %completed equal to 50
    */
   public void testAddNewWorkRecord7() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      /*the first activity */
      OpActivity activity1 = createActivity(projectPlan, OpActivity.STANDARD, baseEffort / 2, 0);
      /*the second activity */
      OpActivity activity2 = createActivity(projectPlan, OpActivity.STANDARD, baseEffort / 2, 0);
      activity2.setComplete(50);
      activity2.setActualEffort(10);
      activity2.setRemainingEffort(10);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity1);
      subActivities.add(activity2);
      collectionActivity.setSubActivities(subActivities);

      activity1.setSuperActivity(collectionActivity);
      activity2.setSuperActivity(collectionActivity);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity1, resource1, baseEffort / 2, 0);

      double actualEffort = 10;
      double remainingEffort = baseEffort / 2 - actualEffort;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 1000, 1000, 1000, 500, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity1));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete */
      double assignmentComplete = actualEffort * 100 / (actualEffort + remainingEffort);
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);

      /*compute the expected activity complete*/
      double activityComplete = actualEffort * 100/ (actualEffort + remainingEffort);
      assertEquals("Activity wrong %completed value", activityComplete, activity1.getComplete(), 0);
      assertActivityCostsEquality(activity1, 0, 1000, 1000, 1000, 500);
      /*compute the expected collection activity complete*/
      double collectionActivityComplete = (actualEffort * 2) * 100 / (actualEffort * 2 + remainingEffort * 2);
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 1000, 1000, 1000, 500);
   }
   /**
    * Tests that a new work record (but not completed) will be correctly inserted.
    * An assignment exists for a <code>OpActivity.TASK</code> from a collection tasks of a project plan with <code>progressTracked</code> on .
    */
   public void testAddNewWorkRecord8() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 0; //0 h base effort
      /*the first task activity */
      OpActivity taskActivity1 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*the second task activity */
      OpActivity taskActivity2 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*create the collection task activity */
      OpActivity collectionTaskActivity = createActivity(projectPlan, OpActivity.COLLECTION_TASK, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(taskActivity1);
      subActivities.add(taskActivity2);
      collectionTaskActivity.setSubActivities(subActivities);

      taskActivity1.setSuperActivity(collectionTaskActivity);
      taskActivity2.setSuperActivity(collectionTaskActivity);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(taskActivity1, resource1, baseEffort, 0);

      double actualEffort = 20;
      double remainingEffort = baseEffort - actualEffort;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(taskActivity1));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionTaskActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);

      /*the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, taskActivity1.getComplete(), 0);
      assertActivityCostsEquality(taskActivity1, 0, 0, 0, 0,0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 0;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionTaskActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionTaskActivity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a new completed work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.TASK</code> from a collection of tasks of a project plan with <code>progressTracked</code> on.
    * The collection contains a second task activity with completed equal to 100 %.
    */
   public void testAddNewWorkRecord9() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 0; //0 h base effort
      /*the first task activity */
      OpActivity taskActivity1 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*the second task activity */
      OpActivity taskActivity2 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      taskActivity2.setComplete(100);
      /*create the collection task activity */
      OpActivity collectionTaskActivity = createActivity(projectPlan, OpActivity.COLLECTION_TASK, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(taskActivity1);
      subActivities.add(taskActivity2);
      collectionTaskActivity.setSubActivities(subActivities);

      taskActivity1.setSuperActivity(collectionTaskActivity);
      taskActivity2.setSuperActivity(collectionTaskActivity);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(taskActivity1, resource1, baseEffort, 0);

      double actualEffort = 0;
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, true);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(taskActivity1));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionTaskActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete */
      double assignmentComplete = 100;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);

      /*the expected activity complete*/
      double taskActivity1Complete = 100;
      assertEquals("Activity wrong %completed value", taskActivity1Complete, taskActivity1.getComplete(), 0);
      assertActivityCostsEquality(taskActivity1, 0, 0, 0, 0,0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = (taskActivity1Complete + taskActivity2.getComplete()) / 2;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionTaskActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionTaskActivity, 0, 0, 0, 0, 0);
   }
   /**
    * Tests that a new work record (but not completed) will be correctly inserted.
    * Two assignments exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> on .
    */
   public void testAddNewWorkRecord10() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort

      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create first assignment */
      OpAssignment assignment1 = createActivityAssignment(activity, resource1, baseEffort /2, 0);
      /*create second assignment */
      OpAssignment assignment2 = createActivityAssignment(activity, resource2, baseEffort / 2, 0);

      double actualEffort = 10;
      double remainingEffort = baseEffort / 2 - actualEffort;
      OpWorkRecord workRecord = createWorkRecord(assignment1, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment1));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*compute the expected assignment complete */
      double assignmentComplete = (actualEffort * 100) / (actualEffort + remainingEffort);
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment1.getComplete(), 0);

      /*compute the expected activity complete*/
      double activityComplete = (actualEffort * 100) / (actualEffort + remainingEffort + assignment2.getRemainingEffort());
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a completed new work record will be correctly inserted.
    * Two assignments exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> on .
    */
   public void testAddNewWorkRecord11() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      collectionActivity.setSubActivities(subActivities);

      activity.setSuperActivity(collectionActivity);

      /*create assignments */
      OpAssignment assignment1 = createActivityAssignment(activity, resource1, baseEffort / 2, 0);
      OpAssignment assignment2 = createActivityAssignment(activity, resource2, baseEffort / 2, 0);

      double actualEffort = 0;
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment1, actualEffort, remainingEffort, 1000, 1000, 1000, 500, true);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment1));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete */
      double assignmentComplete = 100;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment1.getComplete(), 0);

      /*compute the expected activity complete*/
      double activityComplete = 100 * (baseEffort - (remainingEffort + assignment2.getRemainingEffort()))/baseEffort;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 1000, 1000, 1000, 500);
      /*compute the expected collection activity complete*/
      double collectionActivityComplete = 100 * (baseEffort - (remainingEffort + assignment2.getRemainingEffort()))/baseEffort;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 1000, 1000, 1000, 500);
   }

   /**
    * Tests that a new work record(not completed) will be correctly inserted.
    * Two assignments exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> on .
    */
   public void testAddNewWorkRecord12() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*the milestone activity */
      OpActivity milestone = createActivity(projectPlan, OpActivity.MILESTONE, 0 , 0);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      subActivities.add(milestone);
      collectionActivity.setSubActivities(subActivities);

      activity.setSuperActivity(collectionActivity);
      milestone.setSuperActivity(collectionActivity);

      /*create assignments */
      OpAssignment assignment1 = createActivityAssignment(activity, resource1, baseEffort / 2, 0);
      OpAssignment assignment2 = createActivityAssignment(activity, resource2, baseEffort / 2, 0);

      double actualEffort = 10;
      double remainingEffort = baseEffort / 2 - actualEffort;
      OpWorkRecord workRecord = createWorkRecord(assignment1, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment1));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete */
      double assignmentComplete = actualEffort * 100 / (actualEffort + remainingEffort);
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment1.getComplete(), 0);

      /*compute the expected activity complete*/
      double activityComplete = 25;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 25;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 0, 0, 0, 0);
   }
   /******************************************************************************************************************/
   /*                                     PROGRESS TRACKING OFF Tests                                                */
   /******************************************************************************************************************/

    /**
    * Tests that a new work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> off .
    */
   public void testAddNewWorkRecord14() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      double complete = 0;
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, complete);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, complete);

      double actualEffort = 20;
      double remainingEffort = 0; //remaining effort doesn't matter
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete has original complete*/
      assertEquals("Assignment wrong %completed value", complete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      /*the activity complete is not calculated for standard activities*/
      assertEquals("Activity wrong %completed value", complete, activity.getComplete(), 0);
      assertEquals("Activity wrong %remaining effort value", baseEffort, activity.getRemainingEffort(), 0);

      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }
   /**
    * Tests that a new work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> off .
    * The activity is completed 50 %.
    */
   public void testAddNewWorkRecord15() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      double complete = 50;
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, complete);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, complete);

      double actualEffort = 200;
      double remainingEffort = 0; //remaining effort doesn't matter
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 1000, 1000, 1000, 500, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete has original complete*/
      assertEquals("Assignment wrong %completed value", complete, assignment.getComplete(), 0);
      /*the expected assignment and activity remaining effort */
      double remaining = (actualEffort * 100) /complete - actualEffort;
      assertEquals("Assignment wrong %remaining effort value", remaining, assignment.getRemainingEffort(), 0);

      /*the activity complete is not calculated for standard activities (only for collections)*/
      assertEquals("Activity wrong %completed value", complete, activity.getComplete(), 0);
      assertEquals("Activity wrong %remaining effort value", remaining, activity.getRemainingEffort(), 0);
      assertActivityCostsEquality(activity, 0, 1000, 1000, 1000, 500);
   }
    /**
    * Tests that a new work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.TASK</code> or <code>OpActivity.MILESTONE</code> of a project plan
    * with <code>progressTracked</code> off .
    */
   public void testAddNewWorkRecord16() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      double complete = 0; //%completed for activity and assignment
      OpActivity activity = createActivity(projectPlan, OpActivity.TASK, baseEffort, complete);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, complete);

      double actualEffort = 20;
      double remainingEffort = 0; //remaining effort doesn't matter
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete has original complete*/
      assertEquals("Assignment wrong %completed value", complete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);


      assertEquals("Activity wrong %completed value", complete, activity.getComplete(), 0);
      assertEquals("Activity wrong %remaining effort value", baseEffort, activity.getRemainingEffort(), 0);

      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a new work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.STANDARD</code> from a collection of a project plan with <code>progressTracked</code> off .
    */
   public void testAddNewWorkRecord17() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      double complete = 0; //%completed for activity and assignment
      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      OpActivity milestone = createActivity(projectPlan, OpActivity.MILESTONE, baseEffort, 0);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      subActivities.add(milestone);
      collectionActivity.setSubActivities(subActivities);

      activity.setSuperActivity(collectionActivity);
      milestone.setSuperActivity(collectionActivity);

      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);

      double actualEffort = 20;
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 1000, 1000, 1000, 500, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);

      /*the expected assignment complete does not change*/
      assertEquals("Assignment wrong %completed value", complete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);
      /*milestone is skipped from any calculations */
      assertEquals("Assignment wrong %completed value", complete, milestone.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort, milestone.getRemainingEffort(), 0);

      /*the expected activity complete is not calculated*/
      assertEquals("Activity wrong %completed value", complete, activity.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);
      assertActivityCostsEquality(activity, 0, 1000, 1000, 1000, 500);
      /*compute the expected collection activity complete*/
      double collectionActivityComplete = (actualEffort * 100) / (actualEffort + baseEffort);
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 1000, 1000, 1000, 500);
   }
  /**
    * Tests that a new work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.STANDARD</code> from a collection of a project plan with <code>progressTracked</code> off.
    * The collection contains a second activity with %completed equal to 50
    */
  public void testAddNewWorkRecord18() {
     /*progressTracked is off */
     projectPlan.setProgressTracked(false);
     double baseEffort = 40; //40 h base effort

     /*the first activity */
     OpActivity activity1 = createActivity(projectPlan, OpActivity.STANDARD, baseEffort / 2, 0);
     /*the second activity */
     OpActivity activity2 = createActivity(projectPlan, OpActivity.STANDARD, baseEffort / 2, 50);
     /*create the collection activity */
     OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
     Set subActivities = new HashSet();
     subActivities.add(activity1);
     subActivities.add(activity2);
     collectionActivity.setSubActivities(subActivities);

     activity1.setSuperActivity(collectionActivity);
     activity2.setSuperActivity(collectionActivity);

     /*create an asignment */
     OpAssignment assignment = createActivityAssignment(activity1, resource1, baseEffort / 2, 0);

     double actualEffort = 10;
     double remainingEffort = 0;
     OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 1000, 1000, 1000, 500, false);
     /*expectations*/
     mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
     updateAssignmentForWorkingVersionExpectation();
     mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity1));
     updateActivityForWorkingVersionExpectation();
     mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
     updateActivityForWorkingVersionExpectation();

     /*invoke the method on progress calculator */
     OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
     /*the expected assignment complete is 0% (nothing has changed)*/
     assertEquals("Assignment wrong %completed value", 0, assignment.getComplete(), 0);
     assertEquals("Assignment wrong %remaining effort value", baseEffort / 2, assignment.getRemainingEffort(), 0);

     /*the expected activity complete remains unchanged*/
     assertEquals("Activity wrong %completed value", 0, activity1.getComplete(), 0);
     assertEquals("Assignment wrong %remaining effort value", baseEffort / 2, assignment.getRemainingEffort(), 0);
     assertActivityCostsEquality(activity1, 0, 1000, 1000, 1000, 500);

     /*compute the expected collection activity complete*/
     double collectionActivityComplete = actualEffort * 100 / (actualEffort + activity2.getRemainingEffort() + baseEffort / 2);
     assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
     assertActivityCostsEquality(collectionActivity, 0, 1000, 1000, 1000, 500);
  }

   /**
    * Tests that a new work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.TASK</code>or <code>OpActivity.MILESTONE</code>
    * of a project plan with <code>progressTracked</code> off .
    */
   public void testAddNewWorkRecord19() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 0;

      /*the task activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.TASK, baseEffort,0);
      /*create the collection task activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION_TASK, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      collectionActivity.setSubActivities(subActivities);
      activity.setSuperActivity(collectionActivity);

      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);

      double actualEffort = 10;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, 0, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      assertEquals("Assignment wrong %completed value", 0, assignment.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      assertEquals("Task Activity wrong %completed value", 0, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
      assertEquals("Collection Task Activity wrong %completed value", 0, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }
    /**
    * Tests that a new completed work record will be correctly inserted.
    * An assignment exists for a <code>OpActivity.TASK</code> from a collection of tasks of a project plan with <code>progressTracked</code> off.
    * The collection contains a second task activity with completed equal to 100 %.
    */
   public void testAddNewWorkRecord20() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 0; //0 h base effort
      /*the first task activity */
      OpActivity taskActivity1 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*the second task activity */
      OpActivity taskActivity2 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 100);
      /*create the collection task activity */
      OpActivity collectionTaskActivity = createActivity(projectPlan, OpActivity.COLLECTION_TASK, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(taskActivity1);
      subActivities.add(taskActivity2);
      collectionTaskActivity.setSubActivities(subActivities);

      taskActivity1.setSuperActivity(collectionTaskActivity);
      taskActivity2.setSuperActivity(collectionTaskActivity);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(taskActivity1, resource1, baseEffort, 0);

      double actualEffort = 15;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, 0, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(taskActivity1));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionTaskActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      assertEquals("Assignment wrong %completed value",0, assignment.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      /*the expected activity complete*/
      double taskActivity1Complete = 0;
      assertEquals("Activity wrong %completed value", taskActivity1Complete, taskActivity1.getComplete(), 0);
      assertActivityCostsEquality(taskActivity1, 0, 0, 0, 0,0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = (taskActivity1Complete + taskActivity2.getComplete()) / 2;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionTaskActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionTaskActivity, 0, 0, 0, 0, 0);
    }
    /**
    * Tests that a new work record (but not completed) will be correctly inserted.The activity is 50% completed.
    * Two assignments exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> off .
    */
   public void testAddNewWorkRecord21() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort

      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 50);
      /*create first asignment */
      OpAssignment assignment1 = createActivityAssignment(activity, resource1, baseEffort /2, 50);
      /*create second asignment */
      createActivityAssignment(activity, resource2, baseEffort / 2, 0);

      double actualEffort = 10;
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment1, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment1));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete */
      double assignmentComplete = 50;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment1.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort / 2 - actualEffort, assignment1.getRemainingEffort(), 0);
      /*the expected activity complete remains unchanged*/
      double activityComplete = 50;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a new work record will be correctly inserted.The activity is 50% completed within a collection.
    * Two assignments exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> off .
    */
   public void testAddNewWorkRecord22() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 50);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 50);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      collectionActivity.setSubActivities(subActivities);

      activity.setSuperActivity(collectionActivity);


      /*create an asignment */
      OpAssignment assignment1 = createActivityAssignment(activity, resource1, baseEffort / 2, 50);
      createActivityAssignment(activity, resource2, baseEffort / 2, 0);

      double actualEffort = 10;
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment1, actualEffort, remainingEffort, 1000, 1000, 1000, 500, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment1));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      /*the expected assignment complete */
      double assignmentComplete = 50;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment1.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort / 2 - actualEffort, assignment1.getRemainingEffort(), 0);
      /*expected activity complete doesn't changed*/
      double activityComplete = 50;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 1000, 1000, 1000, 500);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 50;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 1000, 1000, 1000, 500);
   }

   /* Tests that a new work record will be correctly inserted.
   * An assignment exists for a <code>OpActivity.TASK</code>or <code>OpActivity.MILESTONE</code> of a project plan
   * with <code>progressTracked</code> off.The task is part of a collection with another task that is completed 100 %.
   */
   public void testAddNewWorkRecord23() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 0;

      /*the task activities */
      OpActivity task1 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 100);
      OpActivity task2 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*create the collection task activity */
      OpActivity taskCollection = createActivity(projectPlan, OpActivity.COLLECTION_TASK, baseEffort, 50);
      Set subActivities = new HashSet();
      subActivities.add(task1);
      subActivities.add(task2);
      taskCollection.setSubActivities(subActivities);

      task1.setSuperActivity(taskCollection);
      task2.setSuperActivity(taskCollection);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(task2, resource1, baseEffort, 0);

      double actualEffort = 10;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, 0, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(task2));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(taskCollection));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      OpProgressCalculator.addWorkRecord((OpBroker) mockBroker.proxy(), workRecord);
      assertEquals("Assignment wrong %completed value", 0, assignment.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      assertEquals("Task Activity wrong %completed value", 0, task2.getComplete(), 0);
      assertActivityCostsEquality(task1, 0, 0, 0, 0, 0);
      assertEquals("Collection Task Activity wrong %completed value", 50, taskCollection.getComplete(), 0);
      assertActivityCostsEquality(task1, 0, 0, 0, 0, 0);
   }
   /******************************************************************************************************************/
   /*                                     PROGRESS TRACKING ON Tests                                                 */
   /******************************************************************************************************************/

   /**
    * Tests that a work record will be correctly updated.
    * A work record exists for a<code>OpActivity.STANDARD</code> assignment of a project plan with <code>progressTracked</code> on .
    */
   public void testRemoveWorkRecord1() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      double actualEffort = 20;
      double remainingEffort = baseEffort - actualEffort;

      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);
      /*the work record that will be deleted*/
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*fill assignment */
      assignment.setActualEffort(actualEffort);
      assignment.setRemainingEffort(remainingEffort);
      assignment.setComplete((actualEffort * 100) / (actualEffort + remainingEffort));
      /*fill activity */
      activity.setComplete((actualEffort * 100) / (actualEffort + remainingEffort));

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());

      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      /*the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a compLeted work record will be correctly deleted.
    * The work record exists for a<code>OpActivity.STANDARD</code> assignment of a project plan with <code>progressTracked</code> on .
    */
   public void testRemoveWorkRecord2() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      double actualEffort = 20; // doesn't matter becouse work record is completed
      double remainingEffort = 0;

      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);
      /*the work record that will be updated*/
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, true);

      /*fill assignment */
      assignment.setComplete(100);
      assignment.setActualEffort(actualEffort);
      assignment.setRemainingEffort(remainingEffort);

      /*fill activity */
      activity.setComplete((actualEffort * 100) / (actualEffort + remainingEffort));

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      /*the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a work record will be correctly removed.
    * The work record exists for a<code>OpActivity.TASK</code> or <code>OpActivity.MILESTONE</code> assignment
    * of a project plan with <code>progressTracked</code> on .
    */
   public void testRemoveWorkRecord3() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 0;

      OpActivity taskActivity = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(taskActivity, resource1, baseEffort, 0);

      double actualEffort = 20;
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort , 0, 0, 0, 0, false);
      /*fill assignment */
      assignment.setComplete(0); //task or milestone
      assignment.setActualEffort(actualEffort);
      assignment.setRemainingEffort(0);

      /*fill activity */
      taskActivity.setComplete(0); //task or milestone

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(taskActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment %complete value */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      /*the expected activity %complete value */
      double activityComplete = 0;
      assertEquals("Task Activity wrong %completed value", activityComplete, taskActivity.getComplete(), 0);
   }

   /**
    * Tests that a work record will be correctly removed.
    * The work record exists for a<code>OpActivity.TASK</code> or <code>OpActivity.MILESTONE</code> assignment
    * of a project plan with <code>progressTracked</code> on .
    */
   public void testRemoveWorkRecord4() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 0;

      OpActivity milestoneActivity = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(milestoneActivity, resource1, baseEffort, 0);

      OpWorkRecord workRecord = createWorkRecord(assignment, 0, 0, 0, 0, 0, 0, true);
      /*fill assignment */
      assignment.setComplete(100); //for completed work record
      assignment.setActualEffort(0);
      assignment.setRemainingEffort(0);

      /*fill milestone activity */
      milestoneActivity.setComplete(100); //for completed work record

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(milestoneActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment %complete value */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);
      /*the expected milestone activity %complete value */
      double activityComplete = 0;
      assertEquals("Task Activity wrong %completed value", activityComplete, milestoneActivity.getComplete(), 0);

   }

   /**
    * Tests that a work record will be correctly removed. The work record exists for a<code>OpActivity.STANDARD</code> assignment
    * of a project plan with <code>progressTracked</code> on. The activity is part of a collection
    */
   public void testRemoveWorkRecord5() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      double actualEffort = 20;
      double remainingEffort = baseEffort - actualEffort;

      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*milestone */
      OpActivity milestone = createActivity(projectPlan, OpActivity.MILESTONE, baseEffort, 0);

      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      subActivities.add(milestone);
      collectionActivity.setSubActivities(subActivities);
      /*fill complete for collection */
      collectionActivity.setComplete((actualEffort * 100) / (actualEffort + remainingEffort));
      activity.setSuperActivity(collectionActivity);
      milestone.setSuperActivity(collectionActivity);
      /*fill complete for activity */
      activity.setComplete((actualEffort * 100) / (actualEffort + remainingEffort));
      activity.setActualEffort(actualEffort);
      activity.setRemainingEffort(remainingEffort);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*fill assignment */
      assignment.setComplete((actualEffort * 100) / (actualEffort + remainingEffort));
      assignment.setActualEffort(actualEffort);
      assignment.setRemainingEffort(remainingEffort);

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      /*the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 0;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a completed work record will be correctly removed. The work record exists for a<code>OpActivity.STANDARD</code> assignment
    * of a project plan with <code>progressTracked</code> on. The activity is part of a collection
    */
   public void testRemoveWorkRecord6() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      double actualEffort = baseEffort;
      double remainingEffort = 0;
      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      collectionActivity.setSubActivities(subActivities);
      collectionActivity.setComplete(100 * (baseEffort - remainingEffort) / baseEffort);

      activity.setSuperActivity(collectionActivity);
      activity.setComplete(100 * (baseEffort - remainingEffort) / baseEffort);
      activity.setActualEffort(actualEffort);
      activity.setRemainingEffort(remainingEffort);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, true);
      assignment.setComplete(100);
      assignment.setActualEffort(actualEffort);//actualEffort == baseEffort
      assignment.setRemainingEffort(remainingEffort);

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);
      /*the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 0;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a completed work record will be correctly removed.
    * The work record exists for a<code>OpActivity.TASK</code> assignment of a project plan with <code>progressTracked</code> on.
    * The task activity is part of a collection which contains another task that is not completed.
    */
   public void testRemoveWorkRecord7() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 0; //0 h base effort
      double actualEffort = 0; //but the work record is marked as completed
      double remainingEffort = 0;

      /*the first task activity */
      OpActivity taskActivity1 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*the second task activity */
      OpActivity taskActivity2 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*create the collection task activity */
      OpActivity collectionTaskActivity = createActivity(projectPlan, OpActivity.COLLECTION_TASK, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(taskActivity1);
      subActivities.add(taskActivity2);
      collectionTaskActivity.setSubActivities(subActivities);
      collectionTaskActivity.setComplete(50);

      taskActivity1.setSuperActivity(collectionTaskActivity);
      taskActivity1.setComplete(100);
      taskActivity2.setActualEffort(0);
      taskActivity2.setSuperActivity(collectionTaskActivity);
      taskActivity2.setComplete(0);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(taskActivity1, resource1, baseEffort, 0);
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, true);
      /*fill assignment */
      assignment.setComplete(100); //task or milestone
      assignment.setActualEffort(actualEffort);
      assignment.setRemainingEffort(remainingEffort);


      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(taskActivity1));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionTaskActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());

      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);
      /*the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, taskActivity1.getComplete(), 0);
      assertActivityCostsEquality(taskActivity1, 0, 0, 0, 0,0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 0;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionTaskActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionTaskActivity, 0, 0, 0, 0, 0);
   }
   /**
    * Tests that a completed work record will be correctly removed.
    *
    * The work record exists for a<code>OpActivity.STANDARD</code> assignemnt of a project plan with <code>progressTracked</code> on.
    * The activity is part of a collection which contains a second activity with %completed equal to 50 but with a different assignment
    */
   public void testRemoveWorkRecord8() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      double actualEffort = 10;
      double remainingEffort = baseEffort / 2 - actualEffort;
      /*the first activity */
      OpActivity activity1 = createActivity(projectPlan, OpActivity.STANDARD, baseEffort / 2, 0);
      activity1.setComplete(actualEffort * 100/ (actualEffort + remainingEffort));
      activity1.setActualEffort(actualEffort);
      activity1.setRemainingEffort(remainingEffort);
      /*the second activity */
      OpActivity activity2 = createActivity(projectPlan, OpActivity.STANDARD, baseEffort / 2, 0);
      activity2.setComplete(50);
      activity2.setActualEffort(actualEffort);
      activity2.setRemainingEffort(remainingEffort);
      /* a milestone that is not taken into consideration*/
      OpActivity milestone = createActivity(projectPlan, OpActivity.MILESTONE, baseEffort, 0);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity1);
      subActivities.add(activity2);
      subActivities.add(milestone);
      collectionActivity.setSubActivities(subActivities);
      /* %completed for collection*/
      collectionActivity.setComplete(50);

      activity1.setSuperActivity(collectionActivity);
      activity2.setSuperActivity(collectionActivity);
      milestone.setSuperActivity(collectionActivity);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity1, resource1, baseEffort / 2, 0);

      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*fill assignment */
      assignment.setComplete(actualEffort * 100 / (actualEffort + remainingEffort));
      assignment.setActualEffort(actualEffort);
      assignment.setRemainingEffort(remainingEffort);

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity1));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());

      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort / 2, assignment.getRemainingEffort(), 0);
      /*compute the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, activity1.getComplete(), 0);
      assertActivityCostsEquality(activity1, 0, 0, 0, 0, 0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 25;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a work record will be correctly removed.
    * A work record exists for a<code>OpActivity.STANDARD</code> assignment of a project plan with <code>progressTracked</code> on .
    */
   public void testRemoveWorkRecord9() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      double actualEffort = 10;
      double remainingEffort = baseEffort / 2 - actualEffort;

      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create assignments */
      OpAssignment assignment1 = createActivityAssignment(activity, resource1, baseEffort / 2 , 0);
      OpAssignment assignment2 = createActivityAssignment(activity, resource2, baseEffort / 2 , 0);
      /*the work record that will be deleted*/
      OpWorkRecord workRecord = createWorkRecord(assignment1, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*fill assignment */
      assignment1.setActualEffort(actualEffort);
      assignment1.setRemainingEffort(remainingEffort);
      assignment1.setComplete((actualEffort * 100) / (actualEffort + remainingEffort));
      /*fill activity */
      activity.setComplete((actualEffort * 100) / (actualEffort + remainingEffort + assignment2.getRemainingEffort()));

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment1));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());

      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment1.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort / 2, assignment1.getRemainingEffort(), 0);

      /*the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a completed work record will be correctly .
    * Two assignments exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> on .
    * The activity is completed 50% becouse of it's assignments and is part of a collection
    */
   public void testRemoveWorkRecord11() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      collectionActivity.setSubActivities(subActivities);

      activity.setSuperActivity(collectionActivity);

      /*create an assignment */
      OpAssignment assignment1 = createActivityAssignment(activity, resource1, baseEffort / 2, 0);
      createActivityAssignment(activity, resource2, baseEffort / 2, 0);

      double actualEffort = 0;
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment1, actualEffort, remainingEffort, 0, 0, 0, 0, true);
      /*fill assignment */
      assignment1.setActualEffort(actualEffort);
      assignment1.setRemainingEffort(remainingEffort);
      assignment1.setComplete(100);
      /*fill activity */
      activity.setComplete(50);
      activity.setActualEffort(actualEffort);
      activity.setRemainingEffort(baseEffort/2);
      /* %completed for collection*/
      collectionActivity.setComplete(50);
      collectionActivity.setActualEffort(actualEffort);
      collectionActivity.setRemainingEffort(baseEffort/2);


      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment1));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

     /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());

      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment1.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort / 2, assignment1.getRemainingEffort(), 0);
      /*the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 0;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 0, 0, 0, 0);
   }

    /**
    * Tests that a work record will be correctly .
    * Two assignments exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> on .
    * The activity is completed 25% becouse and is part of a collection
    */
   public void testRemoveWorkRecord12() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(true);
      double baseEffort = 40; //40 h base effort
      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*the milestone*/
      OpActivity milestone = createActivity(projectPlan, OpActivity.MILESTONE, 0, 0);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      subActivities.add(milestone);
      collectionActivity.setSubActivities(subActivities);

      activity.setSuperActivity(collectionActivity);
      milestone.setSuperActivity(milestone);
      /*create assignments */
      OpAssignment assignment1 = createActivityAssignment(activity, resource1, baseEffort / 2, 0);
      createActivityAssignment(activity, resource2, baseEffort / 2, 0);

      double actualEffort = 10;
      double remainingEffort = (baseEffort / 2) - actualEffort;
      OpWorkRecord workRecord = createWorkRecord(assignment1, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*fill assignment */
      assignment1.setActualEffort(actualEffort);
      assignment1.setRemainingEffort(remainingEffort);
      assignment1.setComplete(actualEffort * 100 / (actualEffort + remainingEffort));
      /*fill activity */
      activity.setComplete(25);
      activity.setActualEffort(actualEffort);
      activity.setRemainingEffort(baseEffort - actualEffort);
      /* %completed for collection*/
      collectionActivity.setComplete(25);
      collectionActivity.setActualEffort(actualEffort);
      collectionActivity.setRemainingEffort(baseEffort - actualEffort);


      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment1));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

     /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());

      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment1.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort / 2, assignment1.getRemainingEffort(), 0);
      /*the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertEquals("Activity wrong %remaining value", baseEffort, activity.getRemainingEffort(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 0;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 0, 0, 0, 0);
   }

   /******************************************************************************************************************/
   /*                                     PROGRESS TRACKING OFF Tests                                                */
   /******************************************************************************************************************/

   /**
    * Tests that a work record will be correctly removed.
    * A work record exists for a<code>OpActivity.STANDARD</code> assignment of a project plan with <code>progressTracked</code> off .
    */
   public void testRemoveWorkRecord14() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      double actualEffort = 20;

      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);
      /*the work record that will be deleted*/
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, baseEffort, 0, 0, 0, 0, false);
      /*fill assignment */
      assignment.setActualEffort(actualEffort);

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());


      assertEquals("Assignment wrong %completed value", 0, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      assertEquals("Activity wrong %completed value", 0, activity.getComplete(), 0);
      assertEquals("Activity wrong remaining effort value", baseEffort, activity.getRemainingEffort(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a work record will be correctly removed.
    * A work record exists for a<code>OpActivity.STANDARD</code> assignment of a project plan with <code>progressTracked</code> off .
    * The activity has completed value 50%
    */
   public void testRemoveWorkRecord15() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      double actualEffort = 20;
      double complete = 50;

      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, complete);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, complete);
      assignment.setActualEffort(actualEffort);
      /*the work record that will be deleted*/
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, baseEffort, 0, 0, 0, 0, false);

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());

      assertEquals("Assignment wrong %completed value", complete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort / 2, assignment.getRemainingEffort(), 0);

      assertEquals("Activity wrong %completed value", complete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a work record will be correctly removed.
    * The work record exists for a<code>OpActivity.TASK</code> or <code>OpActivity.MILESTONE</code> assignment
    * of a project plan with <code>progressTracked</code> off .
    */
   public void testRemoveWorkRecord16() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 0;

      OpActivity taskActivity = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(taskActivity, resource1, baseEffort, 0);

      double actualEffort = 20;
      double remainingEffort = 0;//not editable
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort , 0, 0, 0, 0, false);
      /*fill assignment */
      assignment.setActualEffort(actualEffort);
      /*fill activity */
      taskActivity.setActualEffort(actualEffort);

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(taskActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());

      assertEquals("Assignment wrong %completed value", 0, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      assertEquals("Task Activity wrong %completed value", 0, taskActivity.getComplete(), 0);
      assertEquals("Activity wrong remaining effort value", baseEffort, taskActivity.getRemainingEffort(), 0);
   }

   /**
    * Tests that a work record will be correctly removed. The work record exists for a<code>OpActivity.STANDARD</code> assignment
    * of a project plan with <code>progressTracked</code> off. The activity is part of a collection
    */
   public void testRemoveWorkRecord17() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      double actualEffort = 20;
      double remainingEffort = 0; //not editable from UI

      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 0);
      /*milestone */
      OpActivity milestone = createActivity(projectPlan, OpActivity.MILESTONE, baseEffort, 0);

      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      subActivities.add(milestone);
      collectionActivity.setSubActivities(subActivities);
      /*fill complete for collection */
      collectionActivity.setComplete((actualEffort * 100) / (actualEffort + baseEffort));
      activity.setSuperActivity(collectionActivity);
      milestone.setSuperActivity(collectionActivity);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 0);

      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*fill assignment */
      assignment.setActualEffort(actualEffort);
       /*fill activity */
      activity.setActualEffort(actualEffort);

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment complete */
      double assignmentComplete = 0;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);

      /*the expected activity complete*/
      double activityComplete = 0;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertEquals("Activity wrong remaining effort value", baseEffort, activity.getRemainingEffort(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 0;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 0, 0, 0, 0);
   }
   /**
    * Tests that a work record will be correctly removed. The work record exists for a<code>OpActivity.STANDARD</code> assignment
    * of a project plan with <code>progressTracked</code> off. The activity is part of a collection and is 100% completed
    */
   public void testRemoveWorkRecord18() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      double actualEffort = 20;
      double remainingEffort = 0; //not editable from UI

      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 100);
      /*milestone */
      OpActivity milestone = createActivity(projectPlan, OpActivity.MILESTONE, baseEffort, 100);

      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      subActivities.add(milestone);
      collectionActivity.setSubActivities(subActivities);
      /*fill complete for collection */
      collectionActivity.setComplete((actualEffort * 100) / (actualEffort + baseEffort));
      collectionActivity.setActualEffort(actualEffort);
      activity.setSuperActivity(collectionActivity);
      milestone.setSuperActivity(collectionActivity);


      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity, resource1, baseEffort, 100);
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*fill assignment */
      assignment.setActualEffort(actualEffort);
      /*fill activity */
      activity.setActualEffort(actualEffort);

      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment complete */
      double assignmentComplete = 100;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", 0, assignment.getRemainingEffort(), 0);

      /*the expected activity complete*/
      double activityComplete = 100;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertEquals("Activity wrong remaining effort value", 0, activity.getRemainingEffort(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 100;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a work record will be correctly removed.
    * The work record exists for a<code>OpActivity.TASK</code> or <code>OpActivity.MILESTONE</code> assignment
    * of a project plan with <code>progressTracked</code> off .
    */
   public void testRemoveWorkRecord19() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 0;

      OpActivity milestoneActivity = createActivity(projectPlan, OpActivity.TASK, baseEffort, 100);
      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(milestoneActivity, resource1, baseEffort, 100);

      OpWorkRecord workRecord = createWorkRecord(assignment, 0, 0, 0, 0, 0, 0, true);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(milestoneActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment %complete value */
      double assignmentComplete = 100;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);
      /*the expected milestone activity %complete value */
      double activityComplete = 100;
      assertEquals("Task Activity wrong %completed value", activityComplete, milestoneActivity.getComplete(), 0);
      assertEquals("Activity wrong remaining effort value", baseEffort, milestoneActivity.getRemainingEffort(), 0);
   }

   /**
    * Tests that a work record will be correctly deleted.
    * An assignment exists for a <code>OpActivity.STANDARD</code> from a collection of a project plan with <code>progressTracked</code> off.
    * The collection contains a second activity with %completed equal to 50
    */
   public void testRemoveWorkRecord20() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      double actualEffort = 10;
      double remainingEffort = 0; //not editable

      /*the first activity */
      OpActivity activity1 = createActivity(projectPlan, OpActivity.STANDARD, baseEffort / 2, 0);
      /*the second activity */
      OpActivity activity2 = createActivity(projectPlan, OpActivity.STANDARD, baseEffort / 2, 50);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(activity1);
      subActivities.add(activity2);
      collectionActivity.setSubActivities(subActivities);
      /*fill collection complete */
      collectionActivity.setComplete(actualEffort * 100 / (actualEffort + activity2.getRemainingEffort() + baseEffort / 2));
      activity1.setSuperActivity(collectionActivity);
      activity2.setSuperActivity(collectionActivity);

      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(activity1, resource1, baseEffort / 2, 0);
      assignment.setActualEffort(actualEffort);

      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity1));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment complete is 0% (nothing has changed)*/
      assertEquals("Assignment wrong %completed value", 0, assignment.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort / 2, assignment.getRemainingEffort(), 0);

      /*the expected activity complete remains unchanged*/
      assertEquals("Activity wrong %completed value", 0, activity1.getComplete(), 0);
      assertEquals("Activity wrong %remaining effort value", baseEffort / 2, assignment.getRemainingEffort(), 0);
      assertActivityCostsEquality(activity1, 0, 0, 0, 0, 0);

      /*compute the expected collection activity complete*/
      double collectionActivityComplete = actualEffort * 100 / (actualEffort + activity2.getRemainingEffort() + baseEffort / 2);
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 0, 0, 0, 0);
   }

     /**
    * Tests that a work record  will be correctly removed.
    * The work record exists for a<code>OpActivity.TASK</code> assignment of a project plan with <code>progressTracked</code> off.
    * The task activity is part of a collection which contains another task that is not completed.
    */
   public void testRemoveWorkRecord21() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 0; //0 h base effort
      double actualEffort = 100;
      double remainingEffort = 0; //not editable

      /*the first task activity */
      OpActivity taskActivity1 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 100);
      /*the second task activity */
      OpActivity taskActivity2 = createActivity(projectPlan, OpActivity.TASK, baseEffort, 0);
      /*create the collection task activity */
      OpActivity collectionTaskActivity = createActivity(projectPlan, OpActivity.COLLECTION_TASK, baseEffort, 0);
      Set subActivities = new HashSet();
      subActivities.add(taskActivity1);
      subActivities.add(taskActivity2);
      collectionTaskActivity.setSubActivities(subActivities);
      collectionTaskActivity.setComplete(50);

      taskActivity1.setSuperActivity(collectionTaskActivity);
      taskActivity2.setSuperActivity(collectionTaskActivity);



      /*create an asignment */
      OpAssignment assignment = createActivityAssignment(taskActivity1, resource1, baseEffort, 100);
      OpWorkRecord workRecord = createWorkRecord(assignment, actualEffort, remainingEffort, 0, 0, 0, 0, true);
      /*fill assignment */
      assignment.setActualEffort(actualEffort);


      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(taskActivity1));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionTaskActivity));
      updateActivityForWorkingVersionExpectation();

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());

      /*the expected assignment complete */
      double assignmentComplete = 100;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment.getComplete(), 0);
      assertEquals("Assignment wrong remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);
      /*the expected activity complete*/
      double activityComplete = 100;
      assertEquals("Activity wrong %completed value", activityComplete, taskActivity1.getComplete(), 0);
      assertEquals("Activity wrong %remaining effort value", baseEffort, assignment.getRemainingEffort(), 0);
      assertActivityCostsEquality(taskActivity1, 0, 0, 0, 0,0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 50;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionTaskActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionTaskActivity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a work record will be correctly removed.The activity is 50% completed.
    * Two assignments exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> off .
    */
   public void testRemoveWorkRecord22() {
      /*progressTracked is on */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort

      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 50);
      /*create first asignment */
      OpAssignment assignment1 = createActivityAssignment(activity, resource1, baseEffort /2, 50);
      /*create second asignment */
      createActivityAssignment(activity, resource2, baseEffort / 2, 0);

      double actualEffort = 10;
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment1, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment1));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      /*fill assignment */
      assignment1.setActualEffort(actualEffort);
      /*fill activity */
      activity.setComplete(50);
      activity.setActualEffort(actualEffort);
      activity.setRemainingEffort(baseEffort - actualEffort);

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment complete */
      double assignmentComplete = 50;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment1.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort / 2 - actualEffort, assignment1.getRemainingEffort(), 0);
      /*the activity complete remains unchanged*/
      double activityComplete = 50;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
   }

   /**
    * Tests that a work record will be correctly inserted.The activity is 50% completed within a collection.
    * Two assignments exists for a <code>OpActivity.STANDARD</code> of a project plan with <code>progressTracked</code> off .
    */
   public void testRemoveWorkRecord23() {
      /*progressTracked is off */
      projectPlan.setProgressTracked(false);
      double baseEffort = 40; //40 h base effort
      /*the activity */
      OpActivity activity = createActivity(projectPlan, OpActivity.STANDARD, baseEffort, 50);
      /*create the collection activity */
      OpActivity collectionActivity = createActivity(projectPlan, OpActivity.COLLECTION, baseEffort, 50);
      Set subActivities = new HashSet();
      subActivities.add(activity);
      collectionActivity.setSubActivities(subActivities);

      activity.setSuperActivity(collectionActivity);

      /*create assignments */
      OpAssignment assignment1 = createActivityAssignment(activity, resource1, baseEffort / 2, 50);
      createActivityAssignment(activity, resource2, baseEffort / 2, 0);

      double actualEffort = 10;
      double remainingEffort = 0;
      OpWorkRecord workRecord = createWorkRecord(assignment1, actualEffort, remainingEffort, 0, 0, 0, 0, false);
      /*expectations*/
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(assignment1));
      updateAssignmentForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(activity));
      updateActivityForWorkingVersionExpectation();
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(collectionActivity));
      updateActivityForWorkingVersionExpectation();
      /*fill assignment */
      assignment1.setActualEffort(actualEffort);
      /*fill activity */
      activity.setComplete(50);
      activity.setActualEffort(actualEffort);
      activity.setRemainingEffort(baseEffort - actualEffort);

      /*invoke the method on progress calculator */
      List workRecords = new ArrayList();
      workRecords.add(workRecord);
      OpProgressCalculator.removeWorkRecords((OpBroker) mockBroker.proxy(), workRecords.iterator());
      /*the expected assignment complete */
      double assignmentComplete = 50;
      assertEquals("Assignment wrong %completed value", assignmentComplete, assignment1.getComplete(), 0);
      assertEquals("Assignment wrong %remaining effort value", baseEffort / 2 - actualEffort, assignment1.getRemainingEffort(), 0);
      /*expected activity complete doesn't changed*/
      double activityComplete = 50;
      assertEquals("Activity wrong %completed value", activityComplete, activity.getComplete(), 0);
      assertActivityCostsEquality(activity, 0, 0, 0, 0, 0);
      /*the expected collection activity complete*/
      double collectionActivityComplete = 50;
      assertEquals("Collection activity wrong %completed value", collectionActivityComplete, collectionActivity.getComplete(), 0);
      assertActivityCostsEquality(collectionActivity, 0, 0, 0, 0, 0);
   }


   /**
    * Asserts that the given activity has the expected field values.
    * @param activity           a <code>OpActivity</code>
    * @param personnelCosts     a <code>double</code> representing the expected personnelCosts
    * @param materialCosts      a <code>double</code> representing the expected materialCosts
    * @param travelCosts        a <code>double</code> representing the expected travelCosts
    * @param externalCosts      a <code>double</code> representing the expected externalCosts
    * @param miscellaneousCosts a <code>double</code> representing the expected miscellaneousCosts
    */
   private static void assertActivityCostsEquality(OpActivity activity, double personnelCosts, double materialCosts,
                                                double travelCosts,double externalCosts, double miscellaneousCosts) {
      String activityType = "";
      switch (activity.getType()) {
         case OpActivity.STANDARD :
            activityType = "Standard";
            break;
         case OpActivity.COLLECTION :
            activityType = "Collection";
            break;
         case OpActivity.TASK :
            activityType = "Task";
            break;
         case OpActivity.MILESTONE :
            activityType = "Milestone";
            break;
         case OpActivity.COLLECTION_TASK :
            activityType = "Task collection";
            break;
         default:
            fail("Wrong activity type");
      }
      assertEquals(activityType + " activity wrong personal costs", personnelCosts, activity.getActualPersonnelCosts(), 0);
      assertEquals(activityType + " activity wrong material costs", materialCosts, activity.getActualMaterialCosts(), 0);
      assertEquals(activityType + " activity wrong travel costs", travelCosts, activity.getActualTravelCosts(), 0);
      assertEquals(activityType + " activity wrong external costs", externalCosts, activity.getActualExternalCosts(), 0);
      assertEquals(activityType + " activity wrong miscellaneous costs", miscellaneousCosts, activity.getActualMiscellaneousCosts(), 0);
   }

   /**
    * Creates and returns a new <code>OpWorkRecord</code> entity.
    *
    * @param actualEffort       a <code>double</code> representing the actualEffort
    * @param remainingEffort    a <code>double</code> representing the remainingEffort
    * @param materialCosts      a <code>double</code> representing the materialCosts
    * @param travelCosts        a <code>double</code> representing the travelCosts
    * @param externalCosts      a <code>double</code> representing the externalCosts
    * @param miscellaneousCosts a <code>double</code> representing the miscellaneousCosts
    * @param completed          a <code>boolean</code> representing completed check box
    * @return a new <code>OpWorkRecord</code> entity
    */
   private OpWorkRecord createWorkRecord(OpAssignment assignment, double actualEffort, double remainingEffort,
        double materialCosts, double travelCosts, double externalCosts, double miscellaneousCosts,
        boolean completed) {

      //create the work record
      OpWorkRecord workRecord = new OpWorkRecord();
      workRecord.setAssignment(assignment);
      workRecord.setActualEffort(actualEffort);
      workRecord.setRemainingEffort(remainingEffort);
      workRecord.setMaterialCosts(materialCosts);
      workRecord.setTravelCosts(travelCosts);
      workRecord.setExternalCosts(externalCosts);
      workRecord.setMiscellaneousCosts(miscellaneousCosts);
      workRecord.setCompleted(completed);
      workRecord.setRemainingEffortChange(remainingEffort - assignment.getRemainingEffort());

      return workRecord;
   }

   /**
    * Creates and returns a new <code>OpActivity/code> entity.
    *
    * @return a new <code>OpActivity</code> entity
    */
   public OpActivity createActivity(OpProjectPlan projecPlan, byte type, double baseEffort, double complete) {
      //create an activity
      OpActivity activity = new OpActivity();
      activity.setSequence(0);
      activity.setProjectPlan(projectPlan);
      activity.setType(type);
      activity.setStart(XCalendar.today());
      activity.setFinish(XCalendar.today());
      activity.setAttachments(new HashSet());
      activity.setAssignments(new HashSet());
      activity.setWorkPeriods(new HashSet());
      activity.setBaseEffort(baseEffort);

      if (projecPlan.getProgressTracked()) { //progressTracking on
         activity.setActualEffort(0);
         activity.setRemainingEffort(baseEffort);
         activity.setComplete(0);
      }
      else {
         activity.setActualEffort(0);
         activity.setComplete(complete);
         if (complete == 0) {
            activity.setRemainingEffort(baseEffort);
         }
         if (complete != 0) { //actual Effort is 0
            activity.setRemainingEffort(baseEffort - (baseEffort * complete) / 100);
         }
      }
      return activity;
   }

   /**
    * Returns a new created <code>OpAssignment</code> entity.
    *
    * @param activity        <code>OpActivity</code> the activity for which the assignment is created.
    * @param resource        <code>OpResource</coe> the assignment resource
    * @param baseEffort      <code>double</code> base effort for the assignment
    * @param complete        <code>double</code> complete for the assignment
    * @return a new <code>OpAssignment</code> entity
    */
   private OpAssignment createActivityAssignment(OpActivity activity, OpResource resource, double baseEffort, double complete) {
      //create the activity asignment
      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resource);
      assignment.setProjectPlan(activity.getProjectPlan());
      //add the created assignment to activity assignments set
      activity.getAssignments().add(assignment);
      assignment.setBaseEffort(baseEffort);

      if (activity.getProjectPlan().getProgressTracked()){
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
    * Expectation for OpProgressCalculator#updateAssignmentForWorkingVersion method.
    * We suppose that a working version for the project does not exist
    */
   private void updateAssignmentForWorkingVersionExpectation() {
      //new query on borker
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_WORKING_VERSION_ASSIGNMENT)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //will find no working version
            queryResults.clear();
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_WORKING_VERSION_ASSIGNMENT);
         }
      });
      //set resource id
      mockQuery.expects(once()).method(SET_LONG_METHOD);
      //set activity id
      mockQuery.expects(once()).method(SET_LONG_METHOD);
      //set project plan version
      mockQuery.expects(once()).method(SET_INTEGER_METHOD);
      //iterate over query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);


   }

   /**
    * Expectation for OpProgressCalculator#updateActivityForWorkingVersion method.
    * We suppose that a working version for the project plan does not exist.
    */
   private void updateActivityForWorkingVersionExpectation() {
      //new query on borker
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_WORKING_VERSION_ACTIVITY)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //will find no working version
            queryResults.clear();
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_WORKING_VERSION_ACTIVITY);
         }
      });
      //set activity id
      mockQuery.expects(once()).method(SET_LONG_METHOD);
      //set project plan version
      mockQuery.expects(once()).method(SET_INTEGER_METHOD);
      //iterate over query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);
   }

}
