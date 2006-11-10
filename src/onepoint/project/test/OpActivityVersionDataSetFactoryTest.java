/*
 * Copyright(c) OnePoint Software GmbH 2005. All Rights Reserved.
 */

package onepoint.project.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.util.XCalendar;
import org.jmock.core.Constraint;
import org.jmock.core.Invocation;

import java.util.*;

/**
 * Test case class for <code>OpActivityVersionDataSetFactory</code> using JMock.
 *
 * @author ovidiu.lupas
 */
public class OpActivityVersionDataSetFactoryTest extends OpServiceAbstractTest {
   /*project plan */
   private OpProjectPlan projectPlan;
   /*project plan version */
   private OpProjectPlanVersion projectPlanVersion;
   /*activity */
   private OpActivity activity;
   /* activity version */
   private OpActivityVersion activityVersion;
   /*session broker */
   private OpBroker broker;
   /*assignment version */
   private OpAssignmentVersion assignmentVersion;
   /*work phase version */
   private OpWorkPeriodVersion workPeriodVersion;
   /*resources */
   private OpResource resource1;
   private OpResource resource2;

   private static final String SELECT_ACTIVITY_VERSION = "select activity from OpActivityVersion as activity " +
        "where activity.PlanVersion.ID = ? order by activity.Sequence";
   private static final String SELECT_PROJECT_PLAN_VERSION = "select planVersion from OpProjectPlanVersion as planVersion " +
        "where planVersion.ProjectPlan.ID = ? and planVersion.VersionNumber = ?";

   private static final int FIRST_ACTIVITY_ID = 1999;
   private static final int SECOND_ACTIVITY_ID = 2000;
   private static final int FIRST_RESOURCE_ID = 2002;
   private static final int SECOND_RESOURCE_ID = 2004;
   private static final String FIRST_RESOURCE_NAME = "Resource One";
   private static final String SECOND_RESOURCE_NAME = "Resource Two";
   private static final String FIRST_RESOURCE_LOCATOR = "OpResource." + FIRST_RESOURCE_ID + ".xid";
   private static final String SECOND_RESOURCE_LOCATOR = "OpResource." + SECOND_RESOURCE_ID + ".xid";
   private static final String FIRST_ACTIVITY_LOCATOR = "OpActivity." + FIRST_ACTIVITY_ID + ".xid";
   private static final String SECOND_ACTIVITY_LOCATOR = "OpActivity." + SECOND_ACTIVITY_ID + ".xid";


   private final int ACTIVITY_VERSION_ID = 10;
   private final int PROJECT_PLAN_ID = 1;
   private final int PROJECT_PLAN_VERSION_NUMBER = 2;
   private final String ACTIVITY_VERSION_LOCATOR = "OpActivityVersion." + ACTIVITY_VERSION_ID + ".xid";
   /*attachment content ref count */
   private int CONTENT_REF_COUNT = 1;

   public void setUp() {
      super.setUp();

      //create the project plan
      projectPlan = new OpProjectPlan();
      projectPlan.setID(PROJECT_PLAN_ID);
      projectPlan.setActivities(new HashSet());
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setActivityAssignments(new HashSet());
      projectPlan.setActivityAttachments(new HashSet());
      projectPlan.setDependencies(new HashSet());
      projectPlan.setWorkPeriods(new HashSet());

      resource1 = new OpResource();
      resource1.setID(FIRST_RESOURCE_ID);
      resource1.setName(FIRST_RESOURCE_NAME);
      resource1.setAbsences(new HashSet());
      resource1.setAvailable((byte) 100);
      resource1.setActivityAssignments(new HashSet());
      resource1.setDescription("First Resource Description");
      resource1.setHourlyRate(8);

      resource2 = new OpResource();
      resource2.setID(SECOND_RESOURCE_ID);
      resource2.setName(SECOND_RESOURCE_NAME);
      resource2.setAbsences(new HashSet());
      resource2.setAvailable((byte) 100);
      resource2.setActivityAssignments(new HashSet());
      resource2.setDescription("Second Resource Description");
      resource2.setHourlyRate(8);

      //project plan version
      projectPlanVersion = new OpProjectPlanVersion();
      projectPlanVersion.setComment("Draft Version");
      projectPlanVersion.setProjectPlan(projectPlan);
      projectPlanVersion.setStart(XCalendar.today());
      projectPlanVersion.setFinish(XCalendar.today());
      projectPlanVersion.setVersionNumber(PROJECT_PLAN_VERSION_NUMBER);
      projectPlanVersion.setActivityVersions(new HashSet());
      projectPlanVersion.setAssignmentVersions(new HashSet());
      projectPlanVersion.setDependencyVersions(new HashSet());
      projectPlanVersion.setAttachmentVersions(new HashSet());
      projectPlanVersion.setWorkPeriodVersions(new HashSet());

      //create the activity
      activity = new OpActivity();
      activity.setName("Activity1");
      activity.setDescription("Activity1_VersionDescription");
      activity.setOutlineLevel((byte) 0);
      activity.setSequence(0);
      activity.setType(OpActivityVersion.STANDARD);
      activity.setComplete(80.0);
      activity.setStart(XCalendar.today());
      activity.setFinish(XCalendar.today());
      activity.setDuration(activity.getFinish().getTime() - activity.getStart().getTime());
      activity.setBaseEffort(0);
      activity.setBasePersonnelCosts(0);
      activity.setBaseExternalCosts(0);
      activity.setBaseTravelCosts(0);
      activity.setBaseMaterialCosts(0);

      //create the activity version
      activityVersion = new OpActivityVersion();
      activityVersion.setName("Activity1");
      activityVersion.setDescription("Activity1_VersionDescription");
      activityVersion.setOutlineLevel((byte) 0);
      activityVersion.setSequence(0);
      activityVersion.setType(OpActivityVersion.STANDARD);
      activityVersion.setComplete(80.0);
      activityVersion.setStart(XCalendar.today());
      activityVersion.setFinish(XCalendar.today());
      activityVersion.setDuration(activityVersion.getFinish().getTime() - activityVersion.getStart().getTime());
      activityVersion.setBaseEffort(0);
      activityVersion.setBasePersonnelCosts(0);
      activityVersion.setBaseExternalCosts(0);
      activityVersion.setBaseTravelCosts(0);
      activityVersion.setBaseMaterialCosts(0);
      activityVersion.setAssignmentVersions(new HashSet());
      activityVersion.setAttachmentVersions(new HashSet());
      activityVersion.setPlanVersion(projectPlanVersion);

      //assignment version for project plan version
      assignmentVersion = new OpAssignmentVersion();
      assignmentVersion.setActivityVersion(activityVersion);
      assignmentVersion.setBaseCosts(0.0);
      assignmentVersion.setBaseEffort(0.0);
      assignmentVersion.setResource(resource1);

      //work phase version for project plan version
      workPeriodVersion = new OpWorkPeriodVersion();
      workPeriodVersion.setActivityVersion(activityVersion);
      workPeriodVersion.setStart(XCalendar.today());
      workPeriodVersion.setFinish(XCalendar.today());
      workPeriodVersion.setBaseEffort(0.0);

      //query results
      queryResults = new ArrayList();
      //broker
      broker = (OpBroker) mockBroker.proxy();
   }

   public Object invocationMatch(Invocation invocation) throws IllegalArgumentException {
      String methodName = invocation.invokedMethod.getName();
      //no such method was found
      throw new IllegalArgumentException("Invalid method name:" + methodName + " for this stub");

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#retrieveActivityVersionDataSet</code>
    */
   public void testRetriveActivityVersions() {
      boolean editable = true;
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ACTIVITY_VERSION)).will(methodStub);
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(projectPlanVersion.getID())});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      queryResults.clear();
      queryResults.add(activityVersion);

      projectPlanVersion.getAssignmentVersions().add(assignmentVersion);
      XComponent activitiesVersionDataSet = new XComponent(XComponent.DATA_SET);
      OpActivityVersionDataSetFactory.retrieveActivityVersionDataSet(broker, projectPlanVersion, activitiesVersionDataSet, editable);

      assertEquals("Not the same size for activities and data set", queryResults.size(), activitiesVersionDataSet.getChildCount());
      for (int i = 0; i < queryResults.size(); i++) {
         assertEqualActivityVersions((OpActivityVersion) queryResults.get(i), (XComponent) activitiesVersionDataSet.getChild(i), editable);
      }
   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#activityVersions</code>
    */
   public void testGetActivityVersionsForProjectPlanVersion() {
      //add an activity version to project plan
      Set activityVersions = new HashSet();
      activityVersions.add(activityVersion);
      projectPlanVersion.setActivityVersions(activityVersions);

      Collection activityVersionValues = OpActivityVersionDataSetFactory.activityVersions(projectPlanVersion).values();
      assertEquals("Not the same size for activities map ", projectPlanVersion.getActivityVersions().size(), activityVersionValues.size());

      for (Iterator iterator = activityVersionValues.iterator(); iterator.hasNext();) {
         assertEqualActivityVersions((OpActivityVersion) iterator.next(), activityVersion);
      }
   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#activityVersions
    */
   public void testGetEmptyActivityVersionsForProjectPlanVersion() {
      //project plan doesn't have any activity versions
      projectPlanVersion.setActivityVersions(new HashSet());

      Collection activityVersionValues = OpActivityVersionDataSetFactory.activityVersions(projectPlanVersion).values();
      assertEquals("Not the same size for version activities map ", projectPlanVersion.getActivityVersions().size(), activityVersionValues.size());
      assertEquals("Not empty map for project plan version", 0, activityVersionValues.size());

   }


   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#findProjectPlanVersion</code>
    */
   public void testFindExistentProjectPlanVersion() {

      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_PLAN_VERSION)).will(methodStub);
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(projectPlan.getID())});
      mockQuery.expects(once()).method(SET_INTEGER_METHOD).with(new Constraint[]{eq(1), eq(projectPlanVersion.getVersionNumber())});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);
      //add the project plan version to results
      queryResults.add(projectPlanVersion);

      OpProjectPlanVersion projectVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, projectPlan, projectPlanVersion.getVersionNumber());
      assertEqualProjectPlanVersions(projectVersion, projectPlanVersion);
   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#findProjectPlanVersion
    */
   public void testFindNonExistentProjectPlanVersion() {

      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_PLAN_VERSION)).will(methodStub);
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(projectPlan.getID())});
      mockQuery.expects(once()).method(SET_INTEGER_METHOD).with(new Constraint[]{eq(1), eq(projectPlanVersion.getVersionNumber())});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);
      //clear the project plan versions results
      queryResults.clear();

      OpProjectPlanVersion projectVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, projectPlan, projectPlanVersion.getVersionNumber());
      assertNull("The project plan version does not match the expected one ", projectVersion);
   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#deleteProjectPlanVersion</code>
    */
   public void testDeleteProjectPlanVersion() {
      //attachment version
      OpAttachmentVersion attachmentVersion = new OpAttachmentVersion();
      //create an attachment content
      OpContent attachmentContent = new OpContent();
      //set up the content ref count
      CONTENT_REF_COUNT = 1;
      attachmentContent.setRefCount(CONTENT_REF_COUNT);
      attachmentVersion.setContent(attachmentContent);
      attachmentVersion.setName("AttachmentVersion");
      attachmentVersion.setPlanVersion(projectPlanVersion);
      attachmentVersion.setActivityVersion(activityVersion);
      //the project plan attanchment versions
      Set attachementVersions = new HashSet();
      attachementVersions.add(attachmentVersion);
      //set the project plan version attachement
      projectPlanVersion.setAttachmentVersions(attachementVersions);

      //broker expectations
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(createContentConstraint(CONTENT_REF_COUNT));
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));

      OpActivityVersionDataSetFactory.deleteProjectPlanVersion(broker, projectPlanVersion);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#deleteProjectPlanVersion</code> where the attachment's
    * content has <code>refCount</code> greater than 1
    */
   public void testDeleteProjectPlanVersionWithMultipleContentRef() {
      //attachment version
      OpAttachmentVersion attachmentVersion = new OpAttachmentVersion();
      //create an attachment content
      OpContent attachmentContent = new OpContent();
      //set up the content ref count
      CONTENT_REF_COUNT = 2;
      attachmentContent.setRefCount(CONTENT_REF_COUNT);
      attachmentVersion.setContent(attachmentContent);
      attachmentVersion.setName("AttachmentVersion");
      attachmentVersion.setPlanVersion(projectPlanVersion);
      attachmentVersion.setActivityVersion(activityVersion);
      //the project plan attanchment versions
      Set attachementVersions = new HashSet();
      attachementVersions.add(attachmentVersion);
      //set the project plan version attachement
      projectPlanVersion.setAttachmentVersions(attachementVersions);

      //broker expectations
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createContentConstraint(CONTENT_REF_COUNT - 1));
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));

      OpActivityVersionDataSetFactory.deleteProjectPlanVersion(broker, projectPlanVersion);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#newProjectPlanVersion</code>
    * The <code>copyActivities<code> flag is <code>true</code>
    */
   public void testNewProjectPlanVersion1() {
      projectPlan.setProgressTracked(true);
      projectPlan.getActivities().add(activity);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityVersionConstraint(activity.getComplete(), activity.getBaseEffort()));

      //create a plan version from project plan
      OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, projectPlan, null, PROJECT_PLAN_VERSION_NUMBER, true);
   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#newProjectPlanVersion</code>
    * The <code>copyActivities<code> flag is <code>false</code>
    */
   public void testNewProjectPlanVersion2() {
      projectPlan.setProgressTracked(false);
      projectPlan.getActivities().add(activity);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));
      //create a plan version from project plan
      OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, projectPlan, null, PROJECT_PLAN_VERSION_NUMBER, false);
   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#storeActivityVersionDataSet</code> for a new
    * created project plan version. The <code>copyActivities<code> flag is <code>true</code>.
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testStoreActivityVersionDataSet1() {
      projectPlan.setProgressTracked(true); //doesn't matter
      projectPlan.getActivities().clear(); //clear the project plan activities

      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(isA(OpProjectPlanVersion.class));
      //create a plan version from project plan
      OpProjectPlanVersion ppversionn = OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, projectPlan, null, PROJECT_PLAN_VERSION_NUMBER, true);
      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow1 = OpActivityDataSetFactoryTest.newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.COLLECTION, 0, 40);
      XComponent dataRow2 = OpActivityDataSetFactoryTest.newActivity(SECOND_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, 40);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityVersionConstraint(0, 40));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanVersionConstraint(ppversionn));
      OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, ppversionn, new HashMap(), false);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#storeActivityVersionDataSet</code> for a already
    * created project plan version.An activity assignment version is also added
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testStoreActivityVersionDataSet2() {
      projectPlan.setProgressTracked(true); //doesn't matter
      projectPlan.getActivities().clear(); //clear the project plan activities
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(resource1.getID()), resource1);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = OpActivityDataSetFactoryTest.newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, 40);
      //add activities to data set
      dataSet.addChild(dataRow);

      double baseEffort = 40;
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);

      /*an instance of a project activity assignment used for assignment version constraint*/
      OpAssignment projectAssignment = new OpAssignment();
      projectAssignment.setComplete(0);
      projectAssignment.setBaseEffort(baseEffort);
      projectAssignment.setBaseCosts(baseEffort * resource1.getHourlyRate());

      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentVersionConstraint(projectAssignment));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityVersionConstraint(0, baseEffort));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));

      OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, projectPlanVersion, projectPlanResources, false);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#storeActivityVersionDataSet</code> for a already
    * created project plan version.
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testStoreActivityVersionDataSet3() {
      projectPlan.setProgressTracked(false); //doesn't matter
      projectPlan.getActivities().clear(); //clear the project plan activities

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = OpActivityDataSetFactoryTest.newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, 40);
      //add activities to data set
      dataSet.addChild(dataRow);
      /*expectations */
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityVersionConstraint(0, 40));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));

      OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, projectPlanVersion, new HashMap(), false);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#storeActivityVersionDataSet</code> for a already
    * created project plan version.Two assignment versions are also added
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testStoreActivityVersionDataSet4() {
      projectPlan.setProgressTracked(true); //doesn't matter
      projectPlan.getActivities().clear(); //clear the project plan activities
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(resource1.getID()), resource1);
      projectPlanResources.put(new Long(resource2.getID()), resource2);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = OpActivityDataSetFactoryTest.newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0, 40);
      //add activities to data set
      dataSet.addChild(dataRow);

      double baseEffort = 40;
      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      resources.add(SECOND_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      resouceBaseEfforts.add(new Double(baseEffort / 2));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);

      /*an instance of a project activity assignment used for assignment version constraint*/
      OpAssignment projectAssignment1 = new OpAssignment();
      projectAssignment1.setComplete(0);
      projectAssignment1.setBaseEffort(baseEffort);
      projectAssignment1.setBaseCosts(baseEffort * resource1.getHourlyRate());

      /*an instance of a project activity assignment used for assignment version constraint*/
      OpAssignment projectAssignment2 = new OpAssignment();
      projectAssignment2.setComplete(0);
      projectAssignment2.setBaseEffort(baseEffort / 2);
      projectAssignment2.setBaseCosts((baseEffort / 2) * resource1.getHourlyRate());

      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentVersionConstraint(projectAssignment1));
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentVersionConstraint(projectAssignment2));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createActivityVersionConstraint(0, baseEffort));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));

      OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, projectPlanVersion, projectPlanResources, false);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#storeActivityVersionDataSet</code> for a already
    * created project plan version. The activity version's base effort is updated
    * The project plan has <code>progressTracking</code> flag on
    */
   public void testUpdateActivityVersionDataSet1() {
      double baseEffort = 80;
      projectPlan.setProgressTracked(true);
      activityVersion.setID(FIRST_ACTIVITY_ID);
      activityVersion.setBaseEffort(baseEffort);
      activityVersion.setComplete(0);
      projectPlanVersion.getActivityVersions().add(activityVersion);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = OpActivityDataSetFactoryTest.newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 0,baseEffort / 2);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);
      //add activities to data set
      dataSet.addChild(dataRow);
      /*expectations */
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createActivityVersionConstraint(0, baseEffort / 2));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));

      OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, projectPlanVersion, new HashMap(), false);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#storeActivityVersionDataSet</code> for a already
    * created project plan version. The activity version's %completed is updated
    * The project plan has <code>progressTracking</code> flag OFF
    */
   public void testUpdateActivityVersionDataSet2() {
      double baseEffort = 80;
      projectPlan.setProgressTracked(false);
      activityVersion.setID(FIRST_ACTIVITY_ID);
      activityVersion.setBaseEffort(baseEffort);
      activityVersion.setComplete(50);
      projectPlanVersion.getActivityVersions().add(activityVersion);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = OpActivityDataSetFactoryTest.newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 25,baseEffort);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);
      //add activities to data set
      dataSet.addChild(dataRow);
      /*expectations */
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createActivityVersionConstraint(25, baseEffort));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));

      OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, projectPlanVersion, new HashMap(), false);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#storeActivityVersionDataSet</code> for a already
    * created project plan version.The activity version's base effort is updated and an assignment version is also added.
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testUpdateActivityVersionDataSet3() {
      projectPlan.setProgressTracked(false);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(resource1.getID()), resource1);

      double baseEffort = 40;
      activityVersion.setID(FIRST_ACTIVITY_ID);
      activityVersion.setBaseEffort(baseEffort);
      activityVersion.setComplete(50);
      projectPlanVersion.getActivityVersions().add(activityVersion);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = OpActivityDataSetFactoryTest.newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 50, baseEffort*2);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);
      //add activities to data set
      dataSet.addChild(dataRow);


      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);

      /*an instance of a project activity assignment used for assignment version constraint*/
      OpAssignment projectAssignment = new OpAssignment();
      projectAssignment.setComplete(0);
      projectAssignment.setBaseEffort(baseEffort);
      projectAssignment.setBaseCosts(baseEffort * resource1.getHourlyRate());

      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentVersionConstraint(projectAssignment));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityVersionConstraint(50, baseEffort * 2));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));

      OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, projectPlanVersion, projectPlanResources, false);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#storeActivityVersionDataSet</code> for a already
    * created project plan version.The activity version's base effort and also an assignment version are updated.
    * The project plan has <code>progressTracking</code> flag off
    */
   public void testUpdateActivityVersionDataSet4() {
      projectPlan.setProgressTracked(false);
      //project plan resources
      HashMap projectPlanResources = new HashMap();
      projectPlanResources.put(new Long(resource1.getID()), resource1);

      double baseEffort = 40;
      activityVersion.setID(FIRST_ACTIVITY_ID);
      activityVersion.setBaseEffort(baseEffort);
      activityVersion.setComplete(50);
      projectPlanVersion.getActivityVersions().add(activityVersion);
      projectPlanVersion.getAssignmentVersions().add(assignmentVersion);

      // create the data set
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = OpActivityDataSetFactoryTest.newActivity(FIRST_ACTIVITY_LOCATOR, OpActivity.STANDARD, 50, baseEffort*2);
      dataRow.setStringValue(FIRST_ACTIVITY_LOCATOR);
      //add activities to data set
      dataSet.addChild(dataRow);


      ArrayList resources = new ArrayList();
      resources.add(FIRST_RESOURCE_LOCATOR);
      OpGanttValidator.setResources(dataRow, resources);

      ArrayList resouceBaseEfforts = new ArrayList();
      resouceBaseEfforts.add(new Double(baseEffort));
      OpGanttValidator.setResourceBaseEfforts(dataRow, resouceBaseEfforts);

      /*an instance of a project activity assignment used for assignment version constraint*/
      OpAssignment projectAssignment = new OpAssignment();
      projectAssignment.setComplete(0);
      projectAssignment.setBaseEffort(baseEffort);
      projectAssignment.setBaseCosts(baseEffort * resource1.getHourlyRate());

      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createAssignmentVersionConstraint(projectAssignment));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createActivityVersionConstraint(50, baseEffort * 2));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));

      OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, projectPlanVersion, projectPlanResources, false);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#newProjectPlanVersion</code>
    */
   public void testNewProjectPlanVersionWithCopyActivitiesDataFromProjectPlan() {

      //project plan attachment
      OpAttachment attachment = new OpAttachment();
      //create an attachment content
      OpContent attachmentContent = new OpContent();
      CONTENT_REF_COUNT = 0;
      attachmentContent.setRefCount(CONTENT_REF_COUNT);
      attachment.setContent(attachmentContent);
      attachment.setName("Attachment");
      attachment.setLinked(true);
      attachment.setLocation("AttachmentLocation");
      attachment.setActivity(activity);

      //project plan assignment
      OpAssignment assignment = new OpAssignment();
      assignment.setResource(new OpResource());
      assignment.setComplete(80.0);
      assignment.setBaseCosts(0.0);
      assignment.setBaseEffort(0.0);
      assignment.setActivity(activity);

      //project plan work phase
      OpWorkPeriod workPeriod = new OpWorkPeriod();
      long periodStartTime = XCalendar.today().getTime() - (XCalendar.today().getTime() % (OpWorkPeriod.PERIOD_LENGTH * XCalendar.MILLIS_PER_DAY));
      workPeriod.setStart(new java.sql.Date(periodStartTime));
      workPeriod.setWorkingDays(15);
      workPeriod.setBaseEffort(8);
      workPeriod.setActivity(activity);

      //create the project plan
      projectPlan = createProjectPlan(activity, attachmentContent, attachment, assignment, workPeriod, new HashSet());

      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(isA(OpActivityVersion.class));
      mockBroker.expects(atLeastOnce()).method(UPDATE_OBJECT_METHOD).with(createContentConstraint(CONTENT_REF_COUNT + 1));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createAttachmentVersionContraint(attachment));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createAssignmentVersionConstraint(assignment));
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(createWorkPhaseVersionConstraint(workPeriod));

      OpProjectPlanVersion planVersion = OpActivityVersionDataSetFactory.
           newProjectPlanVersion(broker, projectPlan, null, PROJECT_PLAN_VERSION_NUMBER, true);
      assertEqualProjectPlanVersions(planVersion, projectPlanVersion);

   }

   /**
    * Tests the behaviour of <code>OpActivityVersionDataSetFactory#newProjectPlanVersion</code>
    */
   public void testNewProjectPlanVersionWithoutCopyActivitiesDataFromProjectPlan() {
      //persist only the newly created project plan version
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createProjectPlanVersionConstraint(projectPlanVersion));

      OpProjectPlanVersion planVersion = OpActivityVersionDataSetFactory.
           newProjectPlanVersion(broker, projectPlan, null, PROJECT_PLAN_VERSION_NUMBER, true);
      assertEqualProjectPlanVersions(planVersion, projectPlanVersion);

   }

   /**
    * Checks that two activity versions are equal or not
    *
    * @param activityVersion a <code>OpActivityVersion</code> representing the reference activity version
    * @param dataRow         a <code>XComponent.DATA_ROW</code>
    * @param editable        <code>boolean</code> editable flag for the activity version
    */
   public void assertEqualActivityVersions(OpActivityVersion activityVersion, XComponent dataRow,
        boolean editable) {

      assertEquals("Activities version locator does not match", activityVersion.locator(), dataRow.getStringValue());
      assertEquals("Activities outline level does not match ", activityVersion.getOutlineLevel(), dataRow.getOutlineLevel());
      assertEquals("Only top-level activities are expanded by default ", activityVersion.getOutlineLevel() == 0, dataRow.getVisible());

      //check name
      assertEquals("Activities Name does not match", activityVersion.getName(), OpGanttValidator.getName(dataRow));
      assertEquals("Activities Editable mode for Name does not match", editable, dataRow.getChild(OpGanttValidator.NAME_COLUMN_INDEX).getEnabled());

      //description
      assertEquals("Activities Description does not match ", activityVersion.getDescription(), OpGanttValidator.getDescription(dataRow));

      //type
      assertEquals("Activities Type does not match ", activityVersion.getType(), OpGanttValidator.getType(dataRow));
      assertEquals("Activities Editable mode for Type does not match", editable, dataRow.getChild(OpGanttValidator.TYPE_COLUMN_INDEX).getEnabled());

      //category
      OpActivityCategory category = activityVersion.getCategory();
      if (category != null) {
         XValidator.choice(category.locator(), category.getName());
         assertEquals("Activities Category  does not match ", XValidator.choice(category.locator(), category.getName()), OpGanttValidator.getCategory(dataRow));
         assertEquals("Activities Editable Mode for Category does not match", editable, dataRow.getChild(OpGanttValidator.CATEGORY_COLUMN_INDEX).getEnabled());
      }
      else {
         assertNull("Activities Category does not match", ((XComponent) dataRow.getChild(OpGanttValidator.CATEGORY_COLUMN_INDEX)).getValue());
      }

      //complete
      assertEquals("Activities Complete does not match", activityVersion.getComplete(), OpGanttValidator.getComplete(dataRow), 0.0);

      //start date
      assertEquals("Activities Start does not match", activityVersion.getStart().getTime(), OpGanttValidator.getStart(dataRow).getTime());
      assertEquals("Activities Editable mode for Start does not match", editable, dataRow.getChild(OpGanttValidator.START_COLUMN_INDEX).getEnabled());

      //finish date
      assertEquals("Activities Finish does not match", activityVersion.getFinish(), OpGanttValidator.getEnd(dataRow));
      assertEquals("Activities Editable mode for End does not match", editable, dataRow.getChild(OpGanttValidator.END_COLUMN_INDEX).getEnabled());

      //duration
      assertEquals("Activities Duration does not match ", activityVersion.getDuration(), OpGanttValidator.getDuration(dataRow), 0);
      assertEquals("Activities Editable mode for Duration does not match", editable, dataRow.getChild(OpGanttValidator.DURATION_COLUMN_INDEX).getEnabled());

      //base effort
      assertEquals("Activities Base Effort does not match ", activityVersion.getBaseEffort(), OpGanttValidator.getBaseEffort(dataRow), 0);
      assertEquals("Activities Editable mode for Base Effort does not match", editable, dataRow.getChild(OpGanttValidator.BASE_EFFORT_COLUMN_INDEX).getEnabled());

      //costs
      assertEquals("Activities Base Personnel Costs does not match ", activityVersion.getBasePersonnelCosts(), OpGanttValidator.getBasePersonnelCosts(dataRow), 0);
      assertEquals("Activities Editable for Base Personnel Costs does not match", false,
           dataRow.getChild(OpGanttValidator.BASE_PERSONNEL_COSTS_COLUMN_INDEX).getEnabled());

      assertEquals("Activities Base Travel Costs does not match ", activityVersion.getBaseTravelCosts(), OpGanttValidator.getBaseTravelCosts(dataRow), 0);
      assertEquals("Activities Editable mode for Base Travel Costs does not match", editable,
           dataRow.getChild(OpGanttValidator.BASE_TRAVEL_COSTS_COLUMN_INDEX).getEnabled());

      assertEquals("Activities Base Material Costs  does not match", activityVersion.getBaseMaterialCosts(), OpGanttValidator.getBaseMaterialCosts(dataRow), 0);
      assertEquals("Activities Editable mode for Base Material Costs does not match", editable && (activityVersion.getType() == OpActivity.STANDARD),
           dataRow.getChild(OpGanttValidator.BASE_MATERIAL_COSTS_COLUMN_INDEX).getEnabled());

      assertEquals("Activities Base External Costs  ", activityVersion.getBaseExternalCosts(), OpGanttValidator.getBaseExternalCosts(dataRow), 0);
      assertEquals("Activities Editable mode for Base External Costs", editable && (activityVersion.getType() == OpActivity.STANDARD),
           dataRow.getChild(OpGanttValidator.BASE_MATERIAL_COSTS_COLUMN_INDEX).getEnabled());

      assertEquals("Activities Base Miscellaneous Costs does not match ", activityVersion.getBaseMiscellaneousCosts(), OpGanttValidator.getBaseMiscellaneousCosts(dataRow), 0);
      assertEquals("Activities Editable mode for Base Miscellaneous Costs does not match", editable && (activityVersion.getType() == OpActivity.STANDARD),
           dataRow.getChild(OpGanttValidator.BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX).getEnabled());


   }

   /**
    * Checks that two activity versions are equal or not
    *
    * @param activityVersion         a <code>OpActivityVersion</code> representing the activity being checked
    * @param expectedActivityVersion a <code>OpActivityVersion</code> representing the reference activity version
    */
   public void assertEqualActivityVersions(OpActivityVersion activityVersion,
        OpActivityVersion expectedActivityVersion) {

      assertEquals("Activities version locator does not match", activityVersion.locator(), expectedActivityVersion.locator());
      assertEquals("Activities outline level does not match ", activityVersion.getOutlineLevel(), expectedActivityVersion.getOutlineLevel());

      //check name
      assertEquals("Activities Name does not match", activityVersion.getName(), expectedActivityVersion.getName());

      //description
      assertEquals("Activities Description does not match ", activityVersion.getDescription(), expectedActivityVersion.getDescription());

      //type
      assertEquals("Activities Type does not match ", activityVersion.getType(), expectedActivityVersion.getType());

      //complete
      assertEquals("Activities Complete does not match", activityVersion.getComplete(), expectedActivityVersion.getComplete(), 0.0);

      //start date
      assertEquals("Activities Start does not match", activityVersion.getStart().getTime(), expectedActivityVersion.getStart().getTime());

      //finish date
      assertEquals("Activities Finish does not match", activityVersion.getFinish(), expectedActivityVersion.getFinish());

      //duration
      assertEquals("Activities Duration does not match ", activityVersion.getDuration(), expectedActivityVersion.getDuration(), 0);

      //base effort
      assertEquals("Activities Base Effort does not match ", activityVersion.getBaseEffort(), expectedActivityVersion.getBaseEffort(), 0);

      //costs
      assertEquals("Activities Base Personnel Costs does not match ", activityVersion.getBasePersonnelCosts(), expectedActivityVersion.getBasePersonnelCosts(), 0);

      assertEquals("Activities Base Travel Costs does not match ", activityVersion.getBaseTravelCosts(), expectedActivityVersion.getBaseTravelCosts(), 0);

      assertEquals("Activities Base Material Costs  does not match", activityVersion.getBaseMaterialCosts(), expectedActivityVersion.getBaseMaterialCosts(), 0);

      assertEquals("Activities Base External Costs  ", activityVersion.getBaseExternalCosts(), expectedActivityVersion.getBaseExternalCosts(), 0);

      assertEquals("Activities Base Miscellaneous Costs does not match ", activityVersion.getBaseMiscellaneousCosts(), expectedActivityVersion.getBaseMiscellaneousCosts(), 0);


   }

   /**
    * Checks that two project plans are equal or not
    *
    * @param projectPlan         a <code>OpProjectPlanVersion</code> representing the project plan  being checked
    * @param expectedProjectPlan a <code>OpProjectPlanVersion</code> representing the reference project plan
    */
   public void assertEqualProjectPlan(OpProjectPlan projectPlan, OpProjectPlan expectedProjectPlan) {
      //id number
      assertEquals("The project plans Id does not match", projectPlan.getID(), expectedProjectPlan.getID());
      //start date
      assertEquals("The project plans Start date does not match", projectPlan.getStart(), expectedProjectPlan.getStart());
      //finish date
      assertEquals("The project plans Finish does not match", projectPlan.getFinish(), expectedProjectPlan.getFinish());
   }

   /**
    * Checks that two project plan versions are equal or not
    *
    * @param projectVersion         a <code>OpProjectPlanVersion</code> representing the plan version being checked
    * @param expectedProjectVersion a <code>OpProjectPlanVersion</code> representing the reference plan version
    */
   public void assertEqualProjectPlanVersions(OpProjectPlanVersion projectVersion, OpProjectPlanVersion expectedProjectVersion) {
      //version number
      assertEquals("The project versions VersionNumber does not match", projectVersion.getVersionNumber(), expectedProjectVersion.getVersionNumber());
      //comment
      if (projectVersion.getComment() != null) {
         assertEquals("The project versions Comment does not match ", projectVersion.getComment(), expectedProjectVersion.getComment());
      }
      //project plan
      assertEqualProjectPlan(projectVersion.getProjectPlan(), expectedProjectVersion.getProjectPlan());
      //start date
      assertEquals("The project versions Start date does not match", projectVersion.getStart(), expectedProjectVersion.getStart());
      //finish date
      assertEquals("The project versions Finish does not match", projectVersion.getFinish(), expectedProjectVersion.getFinish());

   }

   /**
    * Creates a <code>OpProjectPlan<code>
    *
    * @return a new created instance of <code>OpProjectPlan</code>
    */
   public OpProjectPlan createProjectPlan(OpActivity activity, OpContent attachmentCntent, OpAttachment attachment,
        OpAssignment assignment, OpWorkPeriod workPeriod, HashSet dependecies) {

      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      //set up the project plan's activities
      Set activitySet = new HashSet();
      activitySet.add(activity);
      projectPlan.setActivities(activitySet);

      //the project plan attanchment
      Set attachementSet = new HashSet();
      attachementSet.add(attachment);
      //set the project plan attachement
      projectPlan.setActivityAttachments(attachementSet);

      //the project plan's assignment set
      Set assignmentSet = new HashSet();
      assignmentSet.add(assignment);
      projectPlan.setActivityAssignments(assignmentSet);

      //the project plan's work phase set
      Set workPhasesSet = new HashSet();
      workPhasesSet.add(workPeriod);
      projectPlan.setWorkPeriods(workPhasesSet);

      //the project plan's dependecies
      projectPlan.setDependencies(dependecies);

      return projectPlan;
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if an activity version the expected one.
    *
    * @param complete   <code>double</code> the expected %completed for the activity version
    * @param baseEffort <code>double</code> the expected baseEffort for the activity version
    * @return a new activity version constraint
    */
   private Constraint createActivityVersionConstraint(final double complete, final double baseEffort) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpActivityVersion)) {
               return false;
            }
            OpActivityVersion version = (OpActivityVersion) object;
            if (version.getBaseEffort() != baseEffort) {
               return false;
            }
            return version.getComplete() == complete;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if an activity version is the expected one");
         }
      };
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if a project plan version is the expected one.
    *
    * @return a new project plan version constraint
    */
   private Constraint createProjectPlanVersionConstraint(final OpProjectPlanVersion projectPlanVersion) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpProjectPlanVersion)) {
               return false;
            }
            OpProjectPlanVersion projectPV = (OpProjectPlanVersion) object;

            if (projectPlanVersion.getVersionNumber() != projectPV.getVersionNumber()) {
               return false;
            }
            if (!projectPV.getProjectPlan().equals(projectPlan)) {
               return false;
            }
            if (!projectPlanVersion.getStart().equals(projectPV.getStart())) {
               return false;
            }
            return (projectPlanVersion.getFinish().equals(projectPV.getFinish()));
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if a project plan version is the expected one");
         }
      };
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if a attachment version is the expected one.
    *
    * @return a new attachment version constraint
    */
   private Constraint createAttachmentVersionContraint(final OpAttachment projectAttachment) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpAttachmentVersion)) {
               return false;
            }
            OpAttachmentVersion attachmentVersion = (OpAttachmentVersion) object;

            if (!projectAttachment.getName().equals(attachmentVersion.getName())) {
               return false;
            }
            if (projectAttachment.getLinked() != attachmentVersion.getLinked()) {
               return false;
            }
            return (projectAttachment.getLocation().equals(attachmentVersion.getLocation()));
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if an attachment version is the expected one");
         }
      };
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if a assignment version is the expected one.
    *
    * @return a new assignment version  constraint
    */
   private Constraint createAssignmentVersionConstraint(final OpAssignment projectAssignment) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpAssignmentVersion)) {
               return false;
            }
            OpAssignmentVersion assignmentVersion = (OpAssignmentVersion) object;

            if (projectAssignment.getComplete() != assignmentVersion.getComplete()) {
               return false;
            }

            if (projectAssignment.getBaseCosts() != assignmentVersion.getBaseCosts()) {
               return false;
            }


            return (projectAssignment.getBaseEffort() == assignmentVersion.getBaseEffort());
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if an attachment version is the expected one");
         }
      };
   }


   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if an work phase version is the expected one.
    *
    * @return a new work phase version  constraint
    */
   private Constraint createWorkPhaseVersionConstraint(final OpWorkPeriod projectWorkPeriod) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpWorkPeriodVersion)) {
               return false;
            }
            OpWorkPeriodVersion workPeriodVersion = (OpWorkPeriodVersion) object;
            if (!projectWorkPeriod.getStart().equals(workPeriodVersion.getStart())) {
               return false;
            }
            if (projectWorkPeriod.getWorkingDays() != workPeriodVersion.getWorkingDays()) {
               return false;
            }
            return (projectWorkPeriod.getBaseEffort() == workPeriodVersion.getBaseEffort());
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if an work phase version is the expected one");
         }
      };
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if a content is the expected one.
    *
    * @return a new content constraint
    */
   private Constraint createContentConstraint(final int refCount) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpContent)) {
               return false;
            }
            OpContent content = (OpContent) object;

            return (content.getRefCount() == refCount);
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if a content is the expected one");
         }
      };
   }
}
