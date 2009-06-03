/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project_planning.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityComment;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpActivityVersionDataSetFactory;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAssignmentIfc;
import onepoint.project.modules.project.OpAssignmentVersion;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.project.OpWorkMonth;
import onepoint.project.modules.project.OpWorkMonthVersion;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project.test.OpActivityTestDataFactory;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningError;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.service.XSizeInputStream;
import onepoint.util.XEncodingHelper;
import onepoint.util.XEnvironmentManager;

/**
 * This class test project service methods.
 *
 * @author lucian.furtos
 */
public class OpProjectPlanningServiceTest extends OpBaseOpenTestCase {

   private static final String DEFAULT_USER = "tester";
   private static final String DEFAULT_PASSWORD = "pass";
   private final double DOUBLE_ERROR_MARGIN = Math.pow(10, -4);

   private OpProjectPlanningService service;
   private OpProjectTestDataFactory projectDataFactory;
   private OpResourceTestDataFactory resourceDataFactory;
   private OpActivityTestDataFactory activityFactory;

   private String resId;
   private String projId;
   private String planId;
   private static final String TMP_FILE = "file.tmp";

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   @Override
   protected void setUp()
        throws Exception {
      super.setUp();

      service = OpTestDataFactory.getProjectPlanningService();
      projectDataFactory = new OpProjectTestDataFactory(session);
      resourceDataFactory = new OpResourceTestDataFactory(session);
      OpUserTestDataFactory userDataFactory = new OpUserTestDataFactory(session);
      activityFactory = new OpActivityTestDataFactory(session);

      Map userData = OpUserTestDataFactory.createUserData(DEFAULT_USER, DEFAULT_PASSWORD, null, OpUser.CONTRIBUTOR_USER_LEVEL,
           "John", "Doe", "en", "user@email.com", null, null, null, null);
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = OpTestDataFactory.getUserService().insertUser(session, request);
      assertNoError(response);

      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      request = resourceDataFactory.createResourceMsg("resource", "description", 50d, 2d, 1d, false, poolid);
      response = OpTestDataFactory.getResourceService().insertResource(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      try {
         resId = resourceDataFactory.getResourceByName("resource").locator();

         OpTransaction t = broker.newTransaction();
         OpResource res = (OpResource) broker.getObject(resId);
         res.setUser(userDataFactory.getUserByName(DEFAULT_USER));
         broker.updateObject(res);
         t.commit();
      }
      finally {
         broker.close();
      }
      request = OpProjectTestDataFactory.createProjectMsg("project", new Date(1), 1d, null, null);
      response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);

      projId = projectDataFactory.getProjectId("project");
      planId = projectDataFactory.getProjectById(projId).getPlan().locator();
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   @Override
   protected void tearDown()
        throws Exception {
      clean();
      super.tearDown();
   }

   /**
    * Tests the import/export of project plans from/to MS project format.
    *
    * @throws Exception if anything fails.
    */
   public void testImportExportActivities()
        throws Exception {
      long date = System.currentTimeMillis();
      OpBroker broker = session.newBroker();
      try {

         OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

         OpTransaction t = broker.newTransaction();

         OpResource resource = resourceDataFactory.getResourceById(resId);
         OpProjectNode project = projectDataFactory.getProjectById(projId);

         OpActivity activity = new OpActivity(OpActivity.COLLECTION_TASK);
         activity.setProjectPlan(plan);
         activity.setStart(new Date(date + 1000));
         activity.setComplete(0d);
         activity.setTemplate(false);
         broker.makePersistent(activity);

         OpAssignment assignment = new OpAssignment();
         activity.addAssignment(assignment);
         assignment.setResource(resource);
         assignment.setProjectPlan(plan);
         broker.makePersistent(assignment);

         OpProjectNodeAssignment projectAssignment = new OpProjectNodeAssignment();
         projectAssignment.setResource(resource);
         projectAssignment.setProjectNode(project);
         broker.makePersistent(projectAssignment);

         activity = new OpActivity(OpActivity.SCHEDULED_TASK);
         activity.setProjectPlan(plan);
         activity.setStart(new Date(date + 5000));
         activity.setComplete(0d);
         activity.setTemplate(false);
         activity.setAssignments(new HashSet<OpAssignment>());
         broker.makePersistent(activity);

         assignment = new OpAssignment();
         activity.addAssignment(assignment);
         assignment.setResource(resource);
         assignment.setProjectPlan(plan);
         broker.makePersistent(assignment);
         t.commit();

         // FIXME(dfreis Jan 25, 2008 11:16:31 PM) should not do that
         broker.close();
         broker = session.newBroker();

         String planLocator = plan.locator();
         plan = (OpProjectPlan) broker.getObject(planLocator);

         XComponent dataSet = new XComponent(XComponent.DATA_SET);
         dataSet.setValidatorClass(OpGanttValidator.class.getName());
         OpActivityVersionDataSetFactory.getInstance().retrieveActivityVersionDataSet(session, broker, plan.getLatestVersion(), dataSet, false);

         assertEquals(2, dataSet.getChildCount());

         String fileName = "msproject.test";
         XMessage request = OpProjectPlanningTestDataFactory.exportActivitiesMsg(projId, dataSet, fileName);
         XMessage response = service.exportActivities(session, request);
         assertNoError(response);
         String actualFile = (String) response.getArgument("file_name");
         assertEquals("msproject.mpx", actualFile);
         byte[] bytes = (byte[]) response.getArgument("bytes_array");
         assertNotNull(bytes);

         broker.close();
         broker = session.newBroker();
         t = broker.newTransaction();
         deleteAllObjects(broker, OpAssignmentIfc.ASSIGNMENT);
         deleteAllObjects(broker, OpActivity.ACTIVITY);
         t.commit();

         // FIXME(dfreis Jan 25, 2008 11:16:31 PM) should not do that
         broker.close();
         broker = session.newBroker();

         request = OpProjectPlanningTestDataFactory.importActivitiesMsg(projId, actualFile, Boolean.FALSE, bytes);
         response = service.importActivities(session, request);
         assertNoError(response);

         dataSet = new XComponent(XComponent.DATA_SET);
         dataSet.setValidatorClass(OpGanttValidator.class.getName());

         // FIXME(dfreis Jan 25, 2008 11:16:31 PM) should not do that
         broker.close();
         broker = session.newBroker();
         plan = (OpProjectPlan) broker.getObject(planLocator);

         OpActivityVersionDataSetFactory.getInstance().retrieveActivityVersionDataSet(session, broker, plan.getLatestVersion(), dataSet, false);
         assertEquals(2, dataSet.getChildCount());
      }
      finally {
         broker.close();
      }
   }

   /**
    * Tests the import/export of project plans from/to MS project format into an invalid
    * project node type.
    *
    * @throws Exception if anything unexpected fails
    */
   public void testImportExportActivitiesIntoInvalidProjectNode()
        throws Exception {
      XComponent dataSet = new XComponent(XComponent.DATA_SET);

      OpBroker broker = session.newBroker();
      try {
         OpProjectNode rootPortfolio = OpProjectAdministrationService.findRootPortfolio(broker);
         String rootPortfolioId = OpLocator.locatorString(rootPortfolio);

         String fileName = "msproject.test";
         XMessage request = OpProjectPlanningTestDataFactory.exportActivitiesMsg(rootPortfolioId, dataSet, fileName);
         XMessage response = service.exportActivities(session, request);
         assertError(response, OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_EXPORT);

         byte[] bytes = new byte[]{};
         request = OpProjectPlanningTestDataFactory.importActivitiesMsg(rootPortfolioId, fileName, Boolean.FALSE, bytes);
         response = service.importActivities(session, request);
         assertError(response, OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_IMPORT);
      }
      finally {
         broker.close();
      }
   }

   public void testEditActivities()
        throws Exception {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();

         OpProjectPlan plan = (OpProjectPlan) broker.getObject(planId);

         OpActivity activity = new OpActivity();
         activity.setName("A1");
         activity.setProjectPlan(plan);
         broker.makePersistent(activity);

         OpActivity activity1 = new OpActivity();
         activity.setName("A2");
         activity1.setProjectPlan(plan);
         broker.makePersistent(activity1);
         t.commit();
      }
      finally {
         broker.close();
      }

      XMessage request = new XMessage();
      request.setArgument("project_id", projId);
      XMessage response = service.editActivities(session, request);
      assertNoError(response);
   }

   public void testUpdateEffortAtCheckIn()
        throws Exception {
      OpProjectCalendar calendar = OpProjectCalendar.getDefaultProjectCalendar();
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();

         OpProjectPlan plan = (OpProjectPlan) broker.getObject(planId);
         plan.setFinish(new Date(getCalendarWithExactDaySet(2007, 7, 26).getTimeInMillis()));
         OpProjectNode project = (OpProjectNode) broker.getObject(projId);
         project.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
         project.setFinish(new Date(getCalendarWithExactDaySet(2007, 7, 26).getTimeInMillis()));
         OpResource resource = (OpResource) broker.getObject(resId);

         broker.updateObject(project);
         broker.updateObject(plan);

         final String activityName = "Task_Activity";
         OpActivity activity = new OpActivity();
         activity.setName(activityName);
         activity.setType(OpActivity.TASK);
         activity.setStart(project.getStart());
         activity.setBaseEffort(80d);
         activity.setDuration(10d);
         activity.setComplete(50d);
         activity.setProjectPlan(plan);

         OpAssignment assignment1 = new OpAssignment();
         activity.addAssignment(assignment1);
         assignment1.setResource(resource);
         assignment1.setAssigned(50d);
         assignment1.setBaseEffort(80.0);
         assignment1.setActualEffort(40.0);
         assignment1.setRemainingEffort(40.0);
         assignment1.setComplete(50.0);
         assignment1.setProjectPlan(plan);

         //calculate the base personnel cost and base proceeds cost for the activity
         List workingDays = calendar.getWorkingDaysFromInterval(project.getStart(), project.getFinish());
         double workHoursPerDay = activity.getBaseEffort() / (double) workingDays.size();
         double internalSum = 0;
         double externalSum = 0;
         for (Object workingDay : workingDays) {
            internalSum += 2d * workHoursPerDay * 50d / 100;
            externalSum += 1d * workHoursPerDay * 50d / 100;
         }
         assignment1.setBaseCosts(internalSum);
         assignment1.setBaseProceeds(externalSum);
         activity.setBasePersonnelCosts(internalSum);
         activity.setBaseProceeds(externalSum);
         activity.setActualEffort(40d);
         activity.setActualPersonnelCosts(80d);
         activity.setActualProceeds(40d);

         broker.makePersistent(activity);
         broker.makePersistent(assignment1);

         OpProjectNodeAssignment projectNodeAssignment = new OpProjectNodeAssignment();
         projectNodeAssignment.setResource(resource);
         projectNodeAssignment.setProjectNode(project);
         broker.makePersistent(projectNodeAssignment);

         OpWorkRecord workRecord1 = new OpWorkRecord();
         workRecord1.setAssignment(assignment1);
         workRecord1.setActualEffort(40d);

         OpWorkSlip workSlip1 = new OpWorkSlip();
         workSlip1.setDate(new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis()));
         workRecord1.setWorkSlip(workSlip1);

         broker.makePersistent(workRecord1);
         broker.makePersistent(workSlip1);

         t.commit();

         String projectId = XValidator.choice(project.locator(), project.getName());

         XMessage request = new XMessage();
         request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         XMessage response = service.editActivities(session, request);
         assertNoError(response);

         String taskId = activityFactory.getActivityId(activityName);
         activity = activityFactory.getActivityById(taskId);

         XComponent activityDataSet = new XComponent(XComponent.DATA_SET);
         activityDataSet.setValidatorClass(OpGanttValidator.class.getName());
         OpGanttValidator validator = (OpGanttValidator) activityDataSet.validator();
         validator.setProgressTracked(true);
         validator.setProjectTemplate(false);
         validator.setProjectStart(project.getStart());
         validator.setCalculationMode(OpGanttValidator.EFFORT_BASED);
         XComponent dataRow = validator.newDataRow();
         activityDataSet.addChild(dataRow);

         dataRow.setStringValue(activity.locator());
         //0 - name
         OpGanttValidator.setName(dataRow, activityName);
         //1- type
         OpGanttValidator.setType(dataRow, OpActivity.TASK);
         //2 - category
         //3 - complete
         //OpGanttValidator.setComplete(dataRow, 50d);
         //4 - start
         //5 - end
         //6 - duration
         OpGanttValidator.setDuration(dataRow, 20d);
         //7 - base effort
         OpGanttValidator.setBaseEffort(dataRow, 160d);
         //8 - predecessors
         OpGanttValidator.setPredecessors(dataRow, new TreeMap());
         //9 - successors
         OpGanttValidator.setSuccessors(dataRow, new TreeMap());
         //10 - resource
         OpGanttValidator.setResources(dataRow, new ArrayList());
         OpGanttValidator.addResource(dataRow, XValidator.choice(resource.locator(), resource.getName()));
         //11 - base personnel costs
         OpGanttValidator.setBasePersonnelCosts(dataRow, 160d);
         //12 - base travel costs
         OpGanttValidator.setBaseTravelCosts(dataRow, 0d);
         //13 - base material costs
         OpGanttValidator.setBaseMaterialCosts(dataRow, 0d);
         //14 - base external costs
         OpGanttValidator.setBaseExternalCosts(dataRow, 0d);
         //15 - base misc costs
         OpGanttValidator.setBaseMiscellaneousCosts(dataRow, 0d);
         //16 - description
         //17 - attachments
         OpGanttValidator.setAttachments(dataRow, new ArrayList());
         //18 - mode
         OpGanttValidator.setAttributes(dataRow, 0);
         //19 - work phase begin
         OpGanttValidator.setWorkPhases(dataRow, new TreeMap());
         //23 - priority
         OpGanttValidator.setPriority(dataRow, (byte) 1);
         //24 - work records
         Map<String, Object> workRecords = new HashMap<String, Object>();
         workRecords.put(resource.locator(), true);
         OpGanttValidator.setWorkRecords(dataRow, workRecords);
         //25 - actual effort
         OpGanttValidator.setActualEffort(dataRow, 40d);
         //26 - visual resources
         List<String> visualResources = new ArrayList<String>();
         visualResources.add(XValidator.choice(resource.locator(), resource.getName()));
         OpGanttValidator.setVisualResources(dataRow, visualResources);
         //27 - responsible resource
         OpGanttValidator.setResponsibleResource(dataRow, XValidator.choice(resource.locator(), resource.getName()));
         //28 - project
         ((XComponent) dataRow.getChild(28)).setValue(XValidator.choice(project.locator(), project.getName()));
         //29 - payment
         OpGanttValidator.setPayment(dataRow, activity.getPayment());
         //30 - proceeds
         OpGanttValidator.setBaseProceeds(dataRow, 80d);

         request = new XMessage();
         request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         request.setArgument(OpProjectPlanningService.ACTIVITY_SET, activityDataSet);
         request.setArgument(OpProjectPlanningService.SOURCE_PLAN_VERSION_ID, null);
         response = service.checkInActivities(session, request);
         assertNull(response);

         taskId = activityFactory.getActivityId(activityName);
         activity = activityFactory.getActivityById(taskId);
         assertEquals("Activity completion was not correctly calculated", 50d, activity.getComplete(), DOUBLE_ERROR_MARGIN);
      }
      finally {
         broker.close();
      }
   }


   public void testCreateSimpleWorkMonths() {

      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();
         XMessage request = new XMessage();
         OpProjectNode project = (OpProjectNode) broker.getObject(projId);
         String projectId = XValidator.choice(project.locator(), project.getName());
         OpProjectNodeAssignment projectNodeAssignment = new OpProjectNodeAssignment();
         OpResource resource = (OpResource) broker.getObject(resId);
         projectNodeAssignment.setResource(resource);
         projectNodeAssignment.setProjectNode(project);
         broker.makePersistent(projectNodeAssignment);
         t.commit();

         request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         XMessage response = service.editActivities(session, request);
         assertNoError(response);

         project = (OpProjectNode) broker.getObject(projId);
         resource = (OpResource) broker.getObject(resId);

         XComponent dataRow;
         XComponent activityDataSet = new XComponent(XComponent.DATA_SET);
         activityDataSet.setValidatorClass(OpGanttValidator.class.getName());
         OpGanttValidator validator = (OpGanttValidator) activityDataSet.validator();

         XComponent hourlyRates = new XComponent(XComponent.DATA_SET);
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(resource.locator());
         Map<Date, List<Double>> rates = new TreeMap<Date, List<Double>>();
         List<Double> ratesList = new ArrayList<Double>();
         ratesList.add((double) 2);
         ratesList.add((double) 1);
         rates.put(new Date(getCalendarWithExactDaySet(2007, 4, 1).getTimeInMillis()), ratesList);
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setValue(rates);
         dataRow.addChild(dataCell);
         hourlyRates.addChild(dataRow);
         validator.setHourlyRatesDataSet(hourlyRates);
         validator.setProgressTracked(true);
         validator.setProjectTemplate(false);
         validator.setProjectStart(project.getStart());
         validator.setCalculationMode(OpGanttValidator.EFFORT_BASED);
         XComponent assignmentSet = new XComponent(XComponent.DATA_SET);
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(resource.locator());
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(50);
         dataRow.addChild(dataCell);
         assignmentSet.addChild(dataRow);
         validator.setAssignmentSet(assignmentSet);


         dataRow = validator.newDataRow();
         activityDataSet.addChild(dataRow);
         dataRow.setStringValue(null);
         String activityName = "Test Activity";
         OpGanttValidator.setName(dataRow, activityName);
         OpGanttValidator.setType(dataRow, OpActivity.STANDARD);
         OpGanttValidator.setResources(dataRow, new ArrayList());
         OpGanttValidator.addResource(dataRow, XValidator.choice(resource.locator(), resource.getName()));
         List<String> visualResources = new ArrayList<String>();
         visualResources.add(XValidator.choice(resource.locator(), resource.getName()));
         OpGanttValidator.setVisualResources(dataRow, visualResources);
         OpGanttValidator.setStart(dataRow, new Date(getCalendarWithExactDaySet(2007, 5, 1).getTimeInMillis()));
         validator.updateFinish(dataRow, new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
         OpGanttValidator.setPredecessors(dataRow, new TreeMap());
         OpGanttValidator.setSuccessors(dataRow, new TreeMap());
         ((XComponent) dataRow.getChild(28)).setValue(XValidator.choice(project.locator(), project.getName()));


         projectId = XValidator.choice(project.locator(), project.getName());
         request = new XMessage();
         request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         request.setArgument(OpProjectPlanningService.ACTIVITY_SET, activityDataSet);
         request.setArgument(OpProjectPlanningService.SOURCE_PLAN_VERSION_ID, null);
         response = service.checkInActivities(session, request);
         assertNull(response);


         OpActivity activity = activityFactory.getActivityByName(activityName);
         String locator = activity.locator();
         activity = (OpActivity) broker.getObject(locator);
         assertEquals("Wrong number of assignments ", 1, activity.getAssignments().size());
         for (OpAssignment assignment : activity.getAssignments()) {
            assertEquals("Wrong number of work month entities created", 1, assignment.getWorkMonths().size());
            for (OpWorkMonth month : assignment.getWorkMonths()) {
               assertEquals("Wrong Month ", 5, month.getMonth());
               assertEquals("Wrong Year", 2007, month.getYear());
               assertEquals(false, month.isBaselineOnly());

               assertEquals("Wrong Base assignment ", 50.0, month.getBaseAssigned());
               assertEquals("Wrong Base effort ", 84.0, month.getBaseEffort());
               assertEquals("Wrong Base proceeds ", 84.0, month.getBaseProceeds());
               assertEquals("Wrong Base personnel costs ", 2 * 84.0, month.getBasePersonnelCosts());

               assertEquals("Wrong Latest assignment ", 50.0, month.getLatestAssigned());
               assertEquals("Wrong Latest effort ", 84.0, month.getLatestEffort());
               assertEquals("Wrong Latest proceeds ", 84.0, month.getLatestProceeds());
               assertEquals("Wrong Latest personnel costs ", 2 * 84.0, month.getLatestPersonnelCosts());
            }
         }
      }
      finally {
         broker.close();
      }
   }


   public void testCreateSimpleWorkMonthVersions() {

      XComponent activityDataSet;
      activityDataSet = new XComponent(XComponent.DATA_SET);

      String activityName = doubleCheckIn(activityDataSet);
      OpActivity activity;
      OpBroker broker = session.newBroker();
      try {
         activity = activityFactory.getActivityByName(activityName);
         String locator = activity.locator();

         activity = (OpActivity) broker.getObject(locator);
         assertEquals("Wrong number of assignments ", 1, activity.getAssignments().size());
         Iterator<OpAssignment> assignmentIterator = activity.getAssignments().iterator();
         OpAssignment assignment = assignmentIterator.next();
         assertEquals("Wrong number of work month entities created", 1, assignment.getWorkMonths().size());

         Iterator<OpWorkMonth> iterator = assignment.getWorkMonths().iterator();
         OpWorkMonth month = iterator.next();
         assertEquals("Wrong Month ", 6, month.getMonth());
         assertEquals("Wrong Year", 2007, month.getYear());
         assertEquals(false, month.isBaselineOnly());

         assertEquals("Wrong Base assignment ", 50.0, month.getBaseAssigned());
         assertEquals("Wrong Base effort ", 88.0, month.getBaseEffort());
         assertEquals("Wrong Base proceeds ", 88.0, month.getBaseProceeds());
         assertEquals("Wrong Base personnel costs ", 2 * 88.0, month.getBasePersonnelCosts());

         assertEquals("Wrong Base assignment ", 50.0, month.getLatestAssigned());
         assertEquals("Wrong Base effort ", 88.0, month.getLatestEffort());
         assertEquals("Wrong Base proceeds ", 88.0, month.getLatestProceeds());
         assertEquals("Wrong Base personnel costs ", 2 * 88.0, month.getLatestPersonnelCosts());

         OpActivityVersion activityVersion = assignment.getActivity().getVersions().iterator().next();
         OpAssignmentVersion assignmentVersion = activityVersion.getAssignmentVersions().iterator().next();
         OpWorkMonthVersion workVersion = assignmentVersion.getWorkMonthVersions().iterator().next();

         assertEquals("Wrong Month ", 5, workVersion.getMonth());
         assertEquals("Wrong Year", 2007, workVersion.getYear());

         assertEquals("Wrong Base assignment ", 50.0, workVersion.getBaseAssigned());
         assertEquals("Wrong Base effort ", 84.0, workVersion.getBaseEffort());
         assertEquals("Wrong Base proceeds ", 84.0, workVersion.getBaseProceeds());
         assertEquals("Wrong Base personnel costs ", 2 * 84.0, workVersion.getBasePersonnelCosts());
      }
      finally {
         broker.close();
      }
   }

   private String doubleCheckIn(XComponent activityDataSet) {

      XMessage request = new XMessage();
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();
         OpProjectNode project = (OpProjectNode) broker.getObject(projId);
         String projectId = XValidator.choice(project.locator(), project.getName());
         OpProjectNodeAssignment projectNodeAssignment = new OpProjectNodeAssignment();
         OpResource resource = (OpResource) broker.getObject(resId);
         projectNodeAssignment.setResource(resource);
         projectNodeAssignment.setProjectNode(project);
         broker.makePersistent(projectNodeAssignment);
         t.commit();
         request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);

         XMessage response = service.editActivities(session, request);
         assertNoError(response);

         project = (OpProjectNode) broker.getObject(projId);
         resource = (OpResource) broker.getObject(resId);

         XComponent dataRow;
         activityDataSet.setValidatorClass(OpGanttValidator.class.getName());
         OpGanttValidator validator = (OpGanttValidator) activityDataSet.validator();

         XComponent hourlyRates = new XComponent(XComponent.DATA_SET);
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(resource.locator());
         Map<Date, List<Double>> rates = new TreeMap<Date, List<Double>>();
         List<Double> ratesList = new ArrayList<Double>();
         ratesList.add((double) 2);
         ratesList.add((double) 1);
         rates.put(new Date(getCalendarWithExactDaySet(2007, 4, 1).getTimeInMillis()), ratesList);
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setValue(rates);
         dataRow.addChild(dataCell);
         hourlyRates.addChild(dataRow);
         validator.setHourlyRatesDataSet(hourlyRates);
         validator.setProgressTracked(true);
         validator.setProjectTemplate(false);
         validator.setProjectStart(project.getStart());
         validator.setCalculationMode(OpGanttValidator.EFFORT_BASED);
         XComponent assignmentSet = new XComponent(XComponent.DATA_SET);
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(resource.locator());
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(50);
         dataRow.addChild(dataCell);
         assignmentSet.addChild(dataRow);
         validator.setAssignmentSet(assignmentSet);


         dataRow = validator.newDataRow();
         activityDataSet.addChild(dataRow);
         dataRow.setStringValue(null);
         String activityName = "Test Activity";
         OpGanttValidator.setName(dataRow, activityName);
         OpGanttValidator.setType(dataRow, OpActivity.STANDARD);
         OpGanttValidator.setResources(dataRow, new ArrayList());
         OpGanttValidator.addResource(dataRow, XValidator.choice(resource.locator(), resource.getName()));
         List<String> visualResources = new ArrayList<String>();
         visualResources.add(XValidator.choice(resource.locator(), resource.getName()));
         OpGanttValidator.setVisualResources(dataRow, visualResources);
         OpGanttValidator.setStart(dataRow, new Date(getCalendarWithExactDaySet(2007, 5, 1).getTimeInMillis()));
         validator.updateFinish(dataRow, new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
         OpGanttValidator.setPredecessors(dataRow, new TreeMap());
         OpGanttValidator.setSuccessors(dataRow, new TreeMap());
         ((XComponent) dataRow.getChild(28)).setValue(XValidator.choice(project.locator(), project.getName()));


         projectId = XValidator.choice(project.locator(), project.getName());
         request = new XMessage();
         request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         request.setArgument(OpProjectPlanningService.ACTIVITY_SET, activityDataSet);
         request.setArgument(OpProjectPlanningService.SOURCE_PLAN_VERSION_ID, null);
         response = service.checkInActivities(session, request);
         assertNull(response);

         //second check in
         OpActivity activity = activityFactory.getActivityByName(activityName);

         request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         response = service.editActivities(session, request);
         assertNoError(response);

         activityDataSet.removeAllChildren();

         dataRow = validator.newDataRow();
         activityDataSet.addChild(dataRow);
         dataRow.setStringValue(activity.locator());
         OpGanttValidator.setName(dataRow, activityName);
         OpGanttValidator.setType(dataRow, OpActivity.STANDARD);
         OpGanttValidator.setResources(dataRow, new ArrayList());
         OpGanttValidator.addResource(dataRow, XValidator.choice(resource.locator(), resource.getName()));
         visualResources = new ArrayList<String>();
         visualResources.add(XValidator.choice(resource.locator(), resource.getName()));
         OpGanttValidator.setVisualResources(dataRow, visualResources);
         OpGanttValidator.setStart(dataRow, new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
         validator.updateFinish(dataRow, new Date(getCalendarWithExactDaySet(2007, 6, 31).getTimeInMillis()));
         OpGanttValidator.setPredecessors(dataRow, new TreeMap());
         OpGanttValidator.setSuccessors(dataRow, new TreeMap());
         ((XComponent) dataRow.getChild(28)).setValue(XValidator.choice(project.locator(), project.getName()));

         projectId = XValidator.choice(project.locator(), project.getName());
         request = new XMessage();
         request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         request.setArgument(OpProjectPlanningService.ACTIVITY_SET, activityDataSet);
         request.setArgument(OpProjectPlanningService.SOURCE_PLAN_VERSION_ID, null);
         response = service.checkInActivities(session, request);
         assertNull(response);
         return activityName;
      }
      finally {
         broker.close();
      }
   }


   public void testCreateSimpleWorkMonthsWithBaseline() {
      XComponent activityDataSet;
      activityDataSet = new XComponent(XComponent.DATA_SET);
      String activityName = doubleCheckIn(activityDataSet);
      OpBroker broker = session.newBroker();
      try {
         OpTransaction transaction = broker.newTransaction();
         OpProjectNode project = (OpProjectNode) broker.getObject(projId);
         OpResource resource = (OpResource) broker.getObject(resId);
         int maxVersion = -1;
         OpProjectPlanVersion lastVersion = null;
         for (OpProjectPlanVersion planVersion : project.getPlan().getVersions()) {
            int version = planVersion.getVersionNumber();
            if (version > maxVersion) {
               lastVersion = planVersion;
               maxVersion = version;
            }
         }
         assertNotNull(lastVersion);
         lastVersion.setBaseline(true);
         broker.updateObject(lastVersion);
         transaction.commit();
//       broker.close();

         OpActivity activity = activityFactory.getActivityByName(activityName);

         XMessage request = new XMessage();
         String projectId = XValidator.choice(project.locator(), project.getName());
         request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         XMessage response = service.editActivities(session, request);
         assertNoError(response);

         //project has baseline version...make a new check in and check the workmonths
         OpGanttValidator validator = (OpGanttValidator) activityDataSet.validator();
         XComponent dataRow = validator.newDataRow();
         activityDataSet.removeAllChildren();
         activityDataSet.addChild(dataRow);
         dataRow.setStringValue(activity.locator());
         OpGanttValidator.setName(dataRow, activityName);
         OpGanttValidator.setType(dataRow, OpActivity.STANDARD);
         OpGanttValidator.setResources(dataRow, new ArrayList());
         OpGanttValidator.addResource(dataRow, XValidator.choice(resource.locator(), resource.getName()));
         List<String> visualResources = new ArrayList<String>();
         visualResources.add(XValidator.choice(resource.locator(), resource.getName()));
         OpGanttValidator.setVisualResources(dataRow, visualResources);
         OpGanttValidator.setStart(dataRow, new Date(getCalendarWithExactDaySet(2007, 8, 1).getTimeInMillis()));
         validator.updateFinish(dataRow, new Date(getCalendarWithExactDaySet(2007, 8, 30).getTimeInMillis()));
         OpGanttValidator.setPredecessors(dataRow, new TreeMap());
         OpGanttValidator.setSuccessors(dataRow, new TreeMap());
         ((XComponent) dataRow.getChild(28)).setValue(XValidator.choice(project.locator(), project.getName()));

         request = new XMessage();
         request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         request.setArgument(OpProjectPlanningService.ACTIVITY_SET, activityDataSet);
         request.setArgument(OpProjectPlanningService.SOURCE_PLAN_VERSION_ID, null);
         response = service.checkInActivities(session, request);
         assertNull(response);

         activity = (OpActivity) broker.getObject(activity.locator());
         assertEquals("Wrong number of assignments ", 1, activity.getAssignments().size());
         Iterator<OpAssignment> assignmentIterator = activity.getAssignments().iterator();
         OpAssignment assignment = assignmentIterator.next();
         assertEquals("Wrong number of work month entities created", 2, assignment.getWorkMonths().size());

         for (OpWorkMonth month : assignment.getWorkMonths()) {

            if (month.getMonth() == 5) {
               assertEquals("Wrong Year", 2007, month.getYear());
               assertEquals(true, month.isBaselineOnly());

               assertEquals("Wrong Base assignment ", 50.0, month.getBaseAssigned());
               assertEquals("Wrong Base effort ", 84.0, month.getBaseEffort());
               assertEquals("Wrong Base proceeds ", 84.0, month.getBaseProceeds());
               assertEquals("Wrong Base personnel costs ", 2 * 84.0, month.getBasePersonnelCosts());

               assertEquals("Wrong Latest assignment ", 50.0, month.getLatestAssigned());
               assertEquals("Wrong Latest effort ", 0.0, month.getLatestEffort());
               assertEquals("Wrong Latest proceeds ", 0.0, month.getLatestProceeds());
               assertEquals("Wrong Latest personnel costs ", 0.0, month.getLatestPersonnelCosts());
            }
            else if (month.getMonth() == 8) {
               assertEquals("Wrong Year", 2007, month.getYear());
               assertEquals(false, month.isBaselineOnly());

               assertEquals("Wrong Base assignment ", 50.0, month.getBaseAssigned());
               assertEquals("Wrong Base effort ", 0.0, month.getBaseEffort());
               assertEquals("Wrong Base proceeds ", 0.0, month.getBaseProceeds());
               assertEquals("Wrong Base personnel costs ", 0.0, month.getBasePersonnelCosts());

               assertEquals("Wrong Latest assignment ", 50.0, month.getLatestAssigned());
               assertEquals("Wrong Latest effort ", 80.0, month.getLatestEffort());
               assertEquals("Wrong Latest proceeds ", 80.0, month.getLatestProceeds());
               assertEquals("Wrong Latest personnel costs ", 2 * 80.0, month.getLatestPersonnelCosts());

            }
            else {
               fail("Invalid month in the work month set");
            }
         }

      }
      finally {
         broker.close();
      }
   }


   public void testRevertVersion()
        throws Exception {
      long date = System.currentTimeMillis();
      OpBroker broker = session.newBroker();
      try {
         OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

         OpTransaction t = broker.newTransaction();

         //add resource to project
         OpProjectNodeAssignment projectAssignment = new OpProjectNodeAssignment();
         projectAssignment.setProjectNode(plan.getProjectNode());
         projectAssignment.setResource(resourceDataFactory.getResourceById(resId));
         broker.makePersistent(projectAssignment);

         OpActivity activity = new OpActivity(OpActivity.STANDARD);
         activity.setProjectPlan(plan);
         activity.setStart(new Date(date + OpProjectCalendar.MILLIS_PER_DAY));
         activity.setFinish(new Date(date + OpProjectCalendar.MILLIS_PER_WEEK));
         activity.setComplete(0d);
         activity.setTemplate(false);
         broker.makePersistent(activity);

         OpAssignment assignment = new OpAssignment();
         activity.addAssignment(assignment);
         assignment.setResource(resourceDataFactory.getResourceById(resId));
         assignment.setProjectPlan(plan);
         assignment.setAssigned(50d);
         broker.makePersistent(assignment);

         activity = new OpActivity(OpActivity.STANDARD);
         activity.setProjectPlan(plan);
         activity.setStart(new Date(date + 3 * OpProjectCalendar.MILLIS_PER_DAY));
         activity.setFinish(new Date(date + 2 * OpProjectCalendar.MILLIS_PER_WEEK));
         activity.setComplete(0d);
         activity.setTemplate(false);
         activity.setAssignments(new HashSet<OpAssignment>());
         broker.makePersistent(activity);

         assignment = new OpAssignment();
         activity.addAssignment(assignment);
         assignment.setResource(resourceDataFactory.getResourceById(resId));
         assignment.setProjectPlan(plan);
         assignment.setAssigned(50d);
         broker.makePersistent(assignment);

         t.commit();
         broker.clear();

         // create a Project plan version
         plan = (OpProjectPlan) broker.getObject(plan.locator());
         t = broker.newTransaction();
         OpActivityVersionDataSetFactory.getInstance().newProjectPlanVersion(session, broker, plan, session.user(broker), OpProjectPlan.WORKING_VERSION_NUMBER, true);
         t.commit();
         OpProjectPlanVersion planVersion = plan.getWorkingVersion();
         assertNotNull(planVersion);

         XMessage request = OpProjectPlanningTestDataFactory.editActivitiesMsg(projId);
         XMessage response = service.editActivities(session, request);
         assertError(response, OpProjectPlanningError.PROJECT_MODIFIED_WARNING);
         request = OpProjectPlanningTestDataFactory.revertActivitiesMsg(projId);
         response = service.revertActivities(session, request);
         assertNoError(response);
         planVersion = plan.getWorkingVersion();
         assertNull(planVersion);
      }
      finally {
         broker.close();
      }

   }

   public void testRevertVersionError()
        throws Exception {
      long date = System.currentTimeMillis();
      OpBroker broker = session.newBroker();
      try {
         OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

         OpTransaction t = broker.newTransaction();

         //add resource to project
         OpProjectNodeAssignment projectAssignment = new OpProjectNodeAssignment();
         projectAssignment.setProjectNode(plan.getProjectNode());
         projectAssignment.setResource(resourceDataFactory.getResourceById(resId));
         broker.makePersistent(projectAssignment);

         OpActivity activity = new OpActivity(OpActivity.STANDARD);
         activity.setProjectPlan(plan);
         activity.setStart(new Date(date + OpProjectCalendar.MILLIS_PER_DAY));
         activity.setFinish(new Date(date + OpProjectCalendar.MILLIS_PER_WEEK));
         activity.setComplete(0d);
         activity.setTemplate(false);
         broker.makePersistent(activity);

         OpAssignment assignment = new OpAssignment();
         activity.addAssignment(assignment);
         assignment.setResource(resourceDataFactory.getResourceById(resId));
         assignment.setProjectPlan(plan);
         assignment.setAssigned(67d);
         assignment.setBaseEffort(25d);
         broker.makePersistent(assignment);

         activity = new OpActivity(OpActivity.STANDARD);
         activity.setProjectPlan(plan);
         activity.setStart(new Date(date + 3 * OpProjectCalendar.MILLIS_PER_DAY));
         activity.setFinish(new Date(date + 2 * OpProjectCalendar.MILLIS_PER_WEEK));
         activity.setComplete(0d);
         activity.setTemplate(false);
         activity.setAssignments(new HashSet<OpAssignment>());
         broker.makePersistent(activity);

         assignment = new OpAssignment();
         activity.addAssignment(assignment);
         assignment.setResource(resourceDataFactory.getResourceById(resId));
         assignment.setProjectPlan(plan);
         assignment.setAssigned(78d);
         assignment.setBaseEffort(65d);
         broker.makePersistent(assignment);

         t.commit();
         broker.clear();

         // create a Project plan version
         t = broker.newTransaction();
         plan = (OpProjectPlan) broker.getObject(plan.locator());
         OpActivityVersionDataSetFactory.getInstance().newProjectPlanVersion(session, broker, plan, session.user(broker), OpProjectPlan.WORKING_VERSION_NUMBER, true);
         t.commit();
         OpProjectPlanVersion planVersion = plan.getWorkingVersion();
         assertNotNull(planVersion);

         XMessage request = OpProjectPlanningTestDataFactory.editActivitiesMsg(projId);
         XMessage response = service.editActivities(session, request);
         assertError(response, OpProjectPlanningError.PROJECT_AND_RESOURCES_MODIFIED_WARNING);
      }
      finally {
         broker.close();
      }

   }

   public void testCreateTmpFileMultiUser()
        throws Exception {
      byte[] bytes = "The content of the file".getBytes();
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();
         OpContent content = OpContentManager.newContent(new XSizeInputStream(new ByteArrayInputStream(bytes), bytes.length), null, 0);
         broker.makePersistent(content);
         String contentId = content.locator();
         t.commit();
         Map<String, String> params = new HashMap<String, String>();
         params.put("content", contentId);
         params.put("fileName", TMP_FILE);
         XMessage request = new XMessage();
         request.setArgument("parameters", params);
         XMessage response = service.createTemporaryFile(session, request);
         assertNoError(request);
         String actualUrl = (String) response.getArgument("attachmentUrl");
         String actualId = (String) response.getArgument("contentId");

         assertEquals(TMP_FILE, actualUrl);
         assertEquals(contentId, actualId);
      }
      finally {
         broker.close();
      }

   }

   public void testCreateTmpFileSingleUser()
        throws Exception {
      // get initial state
      String initialCode = OpEnvironmentManager.getProductCode();
      OpEnvironmentManager.setProductCode(OpProjectConstants.BASIC_EDITION_CODE);

      byte[] bytes = new byte[1024];
      String mimeType = "binary/octet-stream";
      OpContent content = OpContentManager.newContent(new XSizeInputStream(new ByteArrayInputStream(bytes), bytes.length), mimeType, 1);
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();
         broker.makePersistent(content);
         t.commit();
      }
      finally {
         broker.close();
      }
      String contentId = content.locator();

      Map<String, String> params = new HashMap<String, String>();
      params.put("content", contentId);
      params.put("fileName", TMP_FILE);
      XMessage request = new XMessage();
      request.setArgument("parameters", params);
      XMessage response = service.createTemporaryFile(session, request);
      assertNoError(request);
      String actualUrl = (String) response.getArgument("attachmentUrl");
      String actualId = (String) response.getArgument("contentId");

      assertEquals(contentId, actualId);

      String url = XEncodingHelper.decodeValue(actualUrl);

      // now check if file really exists.
      String filePath = XEnvironmentManager.TMP_DIR + File.separator + url;

      FileInputStream bis = new FileInputStream(filePath);
      byte[] actual = new byte[bytes.length];
      assertEquals(bytes.length, bis.available());
      bis.read(actual);
      bis.close();
      assertTrue(Arrays.equals(bytes, actual));

      // restore state
      OpEnvironmentManager.setProductCode(initialCode);
   }

   public void testInsertComment()
        throws Exception {
      long date = System.currentTimeMillis();

      OpBroker broker = session.newBroker();
      try {
         OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);
         OpTransaction t = broker.newTransaction();

         OpActivity activity = new OpActivity(OpActivity.COLLECTION_TASK);
         activity.setProjectPlan(plan);
         activity.setStart(new Date(date + 1000));
         activity.setComplete(0d);
         activity.setTemplate(false);
         broker.makePersistent(activity);

         t.commit();
         broker.clear();

         plan = projectDataFactory.getProjectPlanById(planId);
         Set activities = plan.getActivities();
         assertEquals(1, activities.size());
         activity = (OpActivity) activities.toArray(new OpActivity[1])[0];
         String id = activity.locator();

         XMessage request = OpProjectPlanningTestDataFactory.insertCommentMsg(id, "C1", "The body of the comment");
         XMessage response = service.insertComment(session, request);
         assertNoError(response);

         activity = (OpActivity) broker.getObject(id);
         assertEquals(1, activity.getComments().size());
      }
      finally {
         broker.close();
      }
   }

   public void testDeleteComment()
        throws Exception {
      long date = System.currentTimeMillis();
      OpBroker broker = session.newBroker();
      try {
         OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

         OpTransaction t = broker.newTransaction();

         OpActivity activity = new OpActivity(OpActivity.COLLECTION_TASK);
         activity.setProjectPlan(plan);
         activity.setStart(new Date(date + 1000));
         activity.setComplete(0d);
         activity.setTemplate(false);
         broker.makePersistent(activity);

         t.commit();
         broker.clear();

         plan = projectDataFactory.getProjectPlanById(planId);
         Set activities = plan.getActivities();
         assertEquals(1, activities.size());
         activity = (OpActivity) activities.toArray(new OpActivity[1])[0];
         String id = activity.locator();

         XMessage request = OpProjectPlanningTestDataFactory.insertCommentMsg(id, "C1", "The body of the comment");
         XMessage response = service.insertComment(session, request);
         assertNoError(response);
         request = OpProjectPlanningTestDataFactory.insertCommentMsg(id, "C2", "The second body of the comment");
         response = service.insertComment(session, request);
         assertNoError(response);

         activity = (OpActivity) broker.getObject(id);
         Set comments = activity.getComments();
         assertEquals(2, comments.size());
         OpActivityComment comment = (OpActivityComment) comments.toArray(new OpActivityComment[2])[0];
         String commentId = comment.locator();

         request = OpProjectPlanningTestDataFactory.deleteCommentMsg(commentId);
         response = service.deleteComment(session, request);
         assertNoError(response);

         broker.clear();

         activity = (OpActivity) broker.getObject(id);
         comments = activity.getComments();
         assertEquals(1, comments.size());
         comment = (OpActivityComment) comments.toArray(new OpActivityComment[1])[0];

         request = OpProjectPlanningTestDataFactory.deleteCommentMsg(comment.locator());
         response = service.deleteComment(session, request);
         assertNoError(response);

         broker.clear();

         activity = (OpActivity) broker.getObject(id);
         assertEquals(0, activity.getComments().size());
      }
      finally {
         broker.close();
      }
   }

}
