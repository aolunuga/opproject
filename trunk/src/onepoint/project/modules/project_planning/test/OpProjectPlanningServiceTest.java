/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project_planning.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.*;
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
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.service.XSizeInputStream;
import onepoint.util.XCalendar;
import onepoint.util.XEncodingHelper;
import onepoint.util.XEnvironmentManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Date;
import java.util.*;

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
   protected void setUp()
        throws Exception {
      super.setUp();

      service = OpTestDataFactory.getProjectPlanningService();
      projectDataFactory = new OpProjectTestDataFactory(session);
      resourceDataFactory = new OpResourceTestDataFactory(session);
      OpUserTestDataFactory userDataFactory = new OpUserTestDataFactory(session);
      activityFactory = new OpActivityTestDataFactory(session);

      clean();

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
      resId = resourceDataFactory.getResourceByName("resource").locator();

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpResource res = (OpResource) broker.getObject(resId);
      res.setUser(userDataFactory.getUserByName(DEFAULT_USER));
      broker.updateObject(res);
      t.commit();
      broker.close();

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
      OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
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
      assignment.setActivity(activity);
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
      assignment.setActivity(activity);
      assignment.setResource(resource);
      assignment.setProjectPlan(plan);
      broker.makePersistent(assignment);
      t.commit();
      broker.close();

      broker = session.newBroker();
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.setValidatorClass(OpGanttValidator.class.getName());
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, plan, dataSet, false);
      broker.close();

      assertEquals(2, dataSet.getChildCount());

      String fileName = "msproject.test";
      XMessage request = OpProjectPlanningTestDataFactory.exportActivitiesMsg(projId, dataSet, fileName);
      XMessage response = service.exportActivities(session, request);
      assertNoError(response);
      String actualFile = (String) response.getArgument("file_name");
      assertEquals("msproject.mpx", actualFile);
      byte[] bytes = (byte[]) response.getArgument("bytes_array");
      assertNotNull(bytes);

      broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();
      deleteAllObjects(broker, OpAssignment.ASSIGNMENT);
      deleteAllObjects(broker, OpActivity.ACTIVITY);
      transaction.commit();
      broker.close();

      request = OpProjectPlanningTestDataFactory.importActivitiesMsg(projId, Boolean.FALSE, bytes);
      response = service.importActivities(session, request);
      assertNoError(response);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.setValidatorClass(OpGanttValidator.class.getName());
      broker = session.newBroker();
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, plan, dataSet, false);
      broker.close();
      assertEquals(2, dataSet.getChildCount());
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
      OpProjectNode rootPortfolio = OpProjectAdministrationService.findRootPortfolio(broker);
      String rootPortfolioId = OpLocator.locatorString(rootPortfolio);
      broker.close();

      String fileName = "msproject.test";
      XMessage request = OpProjectPlanningTestDataFactory.exportActivitiesMsg(rootPortfolioId, dataSet, fileName);
      XMessage response = service.exportActivities(session, request);
      assertError(response, OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_EXPORT);

      byte[] bytes = new byte[]{};
      request = OpProjectPlanningTestDataFactory.importActivitiesMsg(rootPortfolioId, Boolean.FALSE, bytes);
      response = service.importActivities(session, request);
      assertError(response, OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_IMPORT);
   }

   public void testEditActivities()
        throws Exception {
      OpBroker broker = session.newBroker();
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
      broker.close();

      XMessage request = new XMessage();
      request.setArgument("project_id", projId);
      XMessage response = service.editActivities(session, request);
      assertNoError(response);
   }

   public void testUpdateEffortAtCheckIn()
        throws Exception {
      XCalendar calendar = XCalendar.getDefaultCalendar();
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpProjectPlan plan = (OpProjectPlan) broker.getObject(planId);
      plan.setFinish(new Date(getCalendarWithExactDaySet(2007, 7, 26).getTimeInMillis()));
      OpProjectNode project = (OpProjectNode) broker.getObject(projId);
      project.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
      project.setFinish(new Date(getCalendarWithExactDaySet(2007, 7, 26).getTimeInMillis()));
      OpResource resource = (OpResource) broker.getObject(resId);

      broker.updateObject(project);
      broker.updateObject(plan);

      OpActivity activity = new OpActivity();
      activity.setName("Task_Activity");
      activity.setType(OpActivity.TASK);
      activity.setStart(project.getStart());
      activity.setBaseEffort(80d);
      activity.setDuration(10d);
      activity.setComplete(50d);
      activity.setProjectPlan(plan);

      OpAssignment assignment1 = new OpAssignment();
      assignment1.setActivity(activity);
      assignment1.setResource(resource);
      assignment1.setAssigned(50d);

      //calculate the base personnel cost and base proceeds cost for the activity
      List workingDays = calendar.getWorkingDaysFromInterval(project.getStart(), project.getFinish());
      double workHoursPerDay = activity.getBaseEffort() / (double) workingDays.size();
      double internalSum = 0;
      double externalSum = 0;
      for (int i = 0; i < workingDays.size(); i++) {
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
      broker.close();

      String projectId = XValidator.choice(project.locator(), project.getName());

      XMessage request = new XMessage();
      request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      XMessage response = service.editActivities(session, request);
      assertNoError(response);

      String taskId = activityFactory.getActivityId("Task_Activity");
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
      OpGanttValidator.setName(dataRow, "Task_Activity");
      //1- type
      OpGanttValidator.setType(dataRow, OpActivity.TASK);
      //2 - category
      //3 - complete
      OpGanttValidator.setComplete(dataRow, 50d);
      //4 - start
      //5 - end
      //6 - duration
      OpGanttValidator.setDuration(dataRow, 20d);
      //7 - base effort
      OpGanttValidator.setBaseEffort(dataRow, 160d);
      //8 - predecessors
      OpGanttValidator.setPredecessors(dataRow, new ArrayList());
      //9 - successors
      OpGanttValidator.setSuccessors(dataRow, new ArrayList());
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
      OpGanttValidator.setWorkPhaseStarts(dataRow, new ArrayList());
      //20 - work phase end
      OpGanttValidator.setWorkPhaseFinishes(dataRow, new ArrayList());
      //21 - work phase base efforts
      OpGanttValidator.setWorkPhaseBaseEfforts(dataRow, new ArrayList());
      //22 - resource base efforts
      OpGanttValidator.setResourceBaseEfforts(dataRow, new ArrayList());
      //23 - priority
      OpGanttValidator.setPriority(dataRow, (byte) 1);
      //24 - work records
      Map workRecords = new HashMap();
      workRecords.put(resource.locator(), true);
      OpGanttValidator.setWorkRecords(dataRow, workRecords);
      //25 - actual effort
      OpGanttValidator.setActualEffort(dataRow, 40d);
      //26 - visual resources
      ArrayList visualResources = new ArrayList();
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
      request.setArgument(OpProjectPlanningService.WORKING_PLAN_VERSION_ID, null);
      response = service.checkInActivities(session, request);
      assertNull(response);

      taskId = activityFactory.getActivityId("Task_Activity");
      activity = activityFactory.getActivityById(taskId);
      assertEquals("Activity completion was not correctly calculated", 25d, activity.getComplete(), DOUBLE_ERROR_MARGIN);
   }


   public void testCreateSimpleWorkMonths() {

      OpBroker broker = session.newBroker();
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
      broker.close();

      request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      XMessage response = service.editActivities(session, request);
      assertNoError(response);

      broker = session.newBroker();

      project = (OpProjectNode) broker.getObject(projId);
      resource = (OpResource) broker.getObject(resId);

      XComponent dataRow;
      XComponent activityDataSet = new XComponent(XComponent.DATA_SET);
      activityDataSet.setValidatorClass(OpGanttValidator.class.getName());
      OpGanttValidator validator = (OpGanttValidator) activityDataSet.validator();

      XComponent hourlyRates = new XComponent(XComponent.DATA_SET);
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(resource.locator());
      Map rates = new TreeMap();
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
      ArrayList visualResources = new ArrayList();
      visualResources.add(XValidator.choice(resource.locator(), resource.getName()));
      OpGanttValidator.setVisualResources(dataRow, visualResources);
      OpGanttValidator.setStart(dataRow, new Date(getCalendarWithExactDaySet(2007, 5, 1).getTimeInMillis()));
      validator.updateFinish(dataRow, new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
      OpGanttValidator.setPredecessors(dataRow, new ArrayList());
      OpGanttValidator.setSuccessors(dataRow, new ArrayList());
      ((XComponent) dataRow.getChild(28)).setValue(XValidator.choice(project.locator(), project.getName()));


      projectId = XValidator.choice(project.locator(), project.getName());
      request = new XMessage();
      request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      request.setArgument(OpProjectPlanningService.ACTIVITY_SET, activityDataSet);
      request.setArgument(OpProjectPlanningService.WORKING_PLAN_VERSION_ID, null);
      response = service.checkInActivities(session, request);
      assertNull(response);


      OpActivity activity = activityFactory.getActivityByName(activityName);
      String locator = activity.locator();
      broker = session.newBroker();
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
      broker.close();
   }


   public void testCreateSimpleWorkMonthVersions() {

      XComponent activityDataSet;
      activityDataSet = new XComponent(XComponent.DATA_SET);

      String activityName = doubleCheckIn(activityDataSet);
      OpActivity activity;
      OpBroker broker;

      activity = activityFactory.getActivityByName(activityName);
      String locator = activity.locator();

      broker = session.newBroker();
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

      broker.close();
   }

   private String doubleCheckIn(XComponent activityDataSet) {

      OpBroker broker = session.newBroker();
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
      broker.close();

      request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      XMessage response = service.editActivities(session, request);
      assertNoError(response);

      broker = session.newBroker();

      project = (OpProjectNode) broker.getObject(projId);
      resource = (OpResource) broker.getObject(resId);

      XComponent dataRow;
      activityDataSet.setValidatorClass(OpGanttValidator.class.getName());
      OpGanttValidator validator = (OpGanttValidator) activityDataSet.validator();

      XComponent hourlyRates = new XComponent(XComponent.DATA_SET);
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(resource.locator());
      Map rates = new TreeMap();
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
      ArrayList visualResources = new ArrayList();
      visualResources.add(XValidator.choice(resource.locator(), resource.getName()));
      OpGanttValidator.setVisualResources(dataRow, visualResources);
      OpGanttValidator.setStart(dataRow, new Date(getCalendarWithExactDaySet(2007, 5, 1).getTimeInMillis()));
      validator.updateFinish(dataRow, new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
      OpGanttValidator.setPredecessors(dataRow, new ArrayList());
      OpGanttValidator.setSuccessors(dataRow, new ArrayList());
      ((XComponent) dataRow.getChild(28)).setValue(XValidator.choice(project.locator(), project.getName()));


      projectId = XValidator.choice(project.locator(), project.getName());
      request = new XMessage();
      request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      request.setArgument(OpProjectPlanningService.ACTIVITY_SET, activityDataSet);
      request.setArgument(OpProjectPlanningService.WORKING_PLAN_VERSION_ID, null);
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
      visualResources = new ArrayList();
      visualResources.add(XValidator.choice(resource.locator(), resource.getName()));
      OpGanttValidator.setVisualResources(dataRow, visualResources);
      OpGanttValidator.setStart(dataRow, new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
      validator.updateFinish(dataRow, new Date(getCalendarWithExactDaySet(2007, 6, 31).getTimeInMillis()));
      OpGanttValidator.setPredecessors(dataRow, new ArrayList());
      OpGanttValidator.setSuccessors(dataRow, new ArrayList());
      ((XComponent) dataRow.getChild(28)).setValue(XValidator.choice(project.locator(), project.getName()));

      projectId = XValidator.choice(project.locator(), project.getName());
      request = new XMessage();
      request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      request.setArgument(OpProjectPlanningService.ACTIVITY_SET, activityDataSet);
      request.setArgument(OpProjectPlanningService.WORKING_PLAN_VERSION_ID, null);
      response = service.checkInActivities(session, request);
      assertNull(response);
      return activityName;
   }


   public void testCreateSimpleWorkMonthsWithBaseline() {

      XComponent activityDataSet;
      activityDataSet = new XComponent(XComponent.DATA_SET);
      String activityName = doubleCheckIn(activityDataSet);
      OpBroker broker = session.newBroker();
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
      broker.close();

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
      List visualResources = new ArrayList();
      visualResources.add(XValidator.choice(resource.locator(), resource.getName()));
      OpGanttValidator.setVisualResources(dataRow, visualResources);
      OpGanttValidator.setStart(dataRow, new Date(getCalendarWithExactDaySet(2007, 8, 1).getTimeInMillis()));
      validator.updateFinish(dataRow, new Date(getCalendarWithExactDaySet(2007, 8, 30).getTimeInMillis()));
      OpGanttValidator.setPredecessors(dataRow, new ArrayList());
      OpGanttValidator.setSuccessors(dataRow, new ArrayList());
      ((XComponent) dataRow.getChild(28)).setValue(XValidator.choice(project.locator(), project.getName()));

      request = new XMessage();
      request.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      request.setArgument(OpProjectPlanningService.ACTIVITY_SET, activityDataSet);
      request.setArgument(OpProjectPlanningService.WORKING_PLAN_VERSION_ID, null);
      response = service.checkInActivities(session, request);
      assertNull(response);


      broker = session.newBroker();
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

      broker.close();
   }


   public void testRevertVersion()
        throws Exception {
      long date = System.currentTimeMillis();
      OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //add resource to project
      OpProjectNodeAssignment projectAssignment = new OpProjectNodeAssignment();
      projectAssignment.setProjectNode(plan.getProjectNode());
      projectAssignment.setResource(resourceDataFactory.getResourceById(resId));
      broker.makePersistent(projectAssignment);

      OpActivity activity = new OpActivity(OpActivity.STANDARD);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + XCalendar.MILLIS_PER_DAY));
      activity.setFinish(new Date(date + XCalendar.MILLIS_PER_WEEK));
      activity.setComplete(0d);
      activity.setTemplate(false);
      broker.makePersistent(activity);

      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId));
      assignment.setProjectPlan(plan);
      assignment.setAssigned(50d);
      broker.makePersistent(assignment);

      activity = new OpActivity(OpActivity.STANDARD);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 3 * XCalendar.MILLIS_PER_DAY));
      activity.setFinish(new Date(date + 2 * XCalendar.MILLIS_PER_WEEK));
      activity.setComplete(0d);
      activity.setTemplate(false);
      activity.setAssignments(new HashSet<OpAssignment>());
      broker.makePersistent(activity);

      assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId));
      assignment.setProjectPlan(plan);
      assignment.setAssigned(50d);
      broker.makePersistent(assignment);

      t.commit();

      // create a Project plan version
      t = broker.newTransaction();
      OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, plan, session.user(broker), OpProjectAdministrationService.WORKING_VERSION_NUMBER, true);
      t.commit();
      OpProjectPlanVersion planVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, plan, OpProjectAdministrationService.WORKING_VERSION_NUMBER);
      assertNotNull(planVersion);

      broker.close();

      XMessage request = OpProjectPlanningTestDataFactory.editActivitiesMsg(projId);
      XMessage response = service.editActivities(session, request);
      assertError(response, OpProjectPlanningError.HOURLY_RATES_MODIFIED_WARNING);
      request = OpProjectPlanningTestDataFactory.revertActivitiesMsg(projId);
      response = service.revertActivities(session, request);
      assertNoError(response);
      broker = session.newBroker();
      planVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, plan, OpProjectAdministrationService.WORKING_VERSION_NUMBER);
      broker.close();
      assertNull(planVersion);
   }

   public void testRevertVersionError()
        throws Exception {
      long date = System.currentTimeMillis();
      OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //add resource to project
      OpProjectNodeAssignment projectAssignment = new OpProjectNodeAssignment();
      projectAssignment.setProjectNode(plan.getProjectNode());
      projectAssignment.setResource(resourceDataFactory.getResourceById(resId));
      broker.makePersistent(projectAssignment);

      OpActivity activity = new OpActivity(OpActivity.STANDARD);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + XCalendar.MILLIS_PER_DAY));
      activity.setFinish(new Date(date + XCalendar.MILLIS_PER_WEEK));
      activity.setComplete(0d);
      activity.setTemplate(false);
      broker.makePersistent(activity);

      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId));
      assignment.setProjectPlan(plan);
      assignment.setAssigned(67d);
      assignment.setBaseEffort(25d);
      broker.makePersistent(assignment);

      activity = new OpActivity(OpActivity.STANDARD);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 3 * XCalendar.MILLIS_PER_DAY));
      activity.setFinish(new Date(date + 2 * XCalendar.MILLIS_PER_WEEK));
      activity.setComplete(0d);
      activity.setTemplate(false);
      activity.setAssignments(new HashSet<OpAssignment>());
      broker.makePersistent(activity);

      assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId));
      assignment.setProjectPlan(plan);
      assignment.setAssigned(78d);
      assignment.setBaseEffort(65d);
      broker.makePersistent(assignment);

      t.commit();

      // create a Project plan version
      t = broker.newTransaction();
      OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, plan, session.user(broker), OpProjectAdministrationService.WORKING_VERSION_NUMBER, true);
      t.commit();
      OpProjectPlanVersion planVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, plan, OpProjectAdministrationService.WORKING_VERSION_NUMBER);
      assertNotNull(planVersion);

      broker.close();

      XMessage request = OpProjectPlanningTestDataFactory.editActivitiesMsg(projId);
      XMessage response = service.editActivities(session, request);
      assertError(response, OpProjectPlanningError.AVAILIBILITY_AND_RATES_MODIFIED_WARNING);
   }

   public void testCreateTmpFileMultiUser()
        throws Exception {
      byte[] bytes = "The content of the file".getBytes();
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpContent content = OpContentManager.newContent(new XSizeInputStream(new ByteArrayInputStream(bytes), bytes.length), null, 0);
      broker.makePersistent(content);
      String contentId = content.locator();
      t.commit();
      broker.close();

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

   public void testCreateTmpFileSingleUser()
        throws Exception {
      // get initial state
      String initialCode = OpEnvironmentManager.getProductCode();
      OpEnvironmentManager.setProductCode(OpProjectConstants.BASIC_EDITION_CODE);

      byte[] bytes = "The content of the file".getBytes();
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpContent content = OpContentManager.newContent(new XSizeInputStream(new ByteArrayInputStream(bytes), bytes.length), null, 0);
      broker.makePersistent(content);
      String contentId = content.locator();
      t.commit();
      broker.close();

      Map<String, String> params = new HashMap<String, String>();
      params.put("content", contentId);
      params.put("fileName", TMP_FILE);
      XMessage request = new XMessage();
      request.setArgument("parameters", params);
      XMessage response = service.createTemporaryFile(session, request);
      assertNoError(request);
      String actualUrl = (String) response.getArgument("attachmentUrl");
      String actualId = (String) response.getArgument("contentId");

      assertNull(actualId);

      String url = XEncodingHelper.decodeValue(actualUrl);

      // now check if file really exists.
      String filePath = XEnvironmentManager.TMP_DIR + File.separator + url;

      FileInputStream bis = new FileInputStream(filePath);
      byte[] actual = new byte[bytes.length];
      assertEquals(bytes.length, bis.read(actual));
      assertTrue(Arrays.equals(bytes, actual));

      // restore state
      OpEnvironmentManager.setProductCode(initialCode);
   }

   public void testInsertComment()
        throws Exception {
      long date = System.currentTimeMillis();
      OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity = new OpActivity(OpActivity.COLLECTION_TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 1000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      broker.makePersistent(activity);

      t.commit();
      broker.close();

      plan = projectDataFactory.getProjectPlanById(planId);
      Set activities = plan.getActivities();
      assertEquals(1, activities.size());
      activity = (OpActivity) activities.toArray(new OpActivity[1])[0];
      String id = activity.locator();

      XMessage request = OpProjectPlanningTestDataFactory.insertCommentMsg(id, "C1", "The body of the comment");
      XMessage response = service.insertComment(session, request);
      assertNoError(response);

      broker = session.newBroker();
      activity = (OpActivity) broker.getObject(id);
      assertEquals(1, activity.getComments().size());
   }

   public void testDeleteComment()
        throws Exception {
      long date = System.currentTimeMillis();
      OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity = new OpActivity(OpActivity.COLLECTION_TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 1000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      broker.makePersistent(activity);

      t.commit();
      broker.close();

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

      broker = session.newBroker();
      activity = (OpActivity) broker.getObject(id);
      Set comments = activity.getComments();
      assertEquals(2, comments.size());
      OpActivityComment comment = (OpActivityComment) comments.toArray(new OpActivityComment[2])[0];
      String commentId = comment.locator();
      broker.close();

      request = OpProjectPlanningTestDataFactory.deleteCommentMsg(commentId);
      response = service.deleteComment(session, request);
      assertNoError(response);

      broker = session.newBroker();
      activity = (OpActivity) broker.getObject(id);
      comments = activity.getComments();
      assertEquals(1, comments.size());
      comment = (OpActivityComment) comments.toArray(new OpActivityComment[1])[0];
      broker.close();

      request = OpProjectPlanningTestDataFactory.deleteCommentMsg(comment.locator());
      response = service.deleteComment(session, request);
      assertNoError(response);

      broker = session.newBroker();
      activity = (OpActivity) broker.getObject(id);
      assertEquals(0, activity.getComments().size());
      broker.close();
   }

   // ******** Helper Methods *********

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {

      OpUserTestDataFactory usrData = new OpUserTestDataFactory(session);

      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();

      deleteAllObjects(broker, OpWorkRecord.WORK_RECORD);
      deleteAllObjects(broker, OpWorkSlip.WORK_SLIP);
      deleteAllObjects(broker, OpAssignment.ASSIGNMENT);
      deleteAllObjects(broker, OpActivityComment.ACTIVITY_COMMENT);
      deleteAllObjects(broker, OpProjectNodeAssignment.PROJECT_NODE_ASSIGNMENT);
      deleteAllObjects(broker, OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(broker, OpAssignmentVersion.ASSIGNMENT_VERSION);
      deleteAllObjects(broker, OpProjectPlanVersion.PROJECT_PLAN_VERSION);
      deleteAllObjects(broker, OpActivityVersion.ACTIVITY_VERSION);
      deleteAllObjects(broker, OpActivity.ACTIVITY);

      for (OpProjectNode project : projectDataFactory.getAllProjects(broker)) {
         broker.deleteObject(project);
      }

      for (OpResource resource : resourceDataFactory.getAllResources(broker)) {
         broker.deleteObject(resource);
      }

      for (OpResourcePool pool : resourceDataFactory.getAllResourcePools(broker)) {
         if (pool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            continue;
         }
         broker.deleteObject(pool);
      }

      for (OpUser user : usrData.getAllUsers(broker)) {
         if (user.getName().equals(OpUser.ADMINISTRATOR_NAME)) {
            continue;
         }
         broker.deleteObject(user);
      }

      transaction.commit();
      broker.close();
   }

}
