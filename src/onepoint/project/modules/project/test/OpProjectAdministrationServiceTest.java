/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.test;

import onepoint.express.XComponent;
import onepoint.project.modules.project.*;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.test.OpServiceAbstractTest;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;
import org.jmock.core.Constraint;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;

import java.sql.Date;
import java.util.*;

/**
 * Test case class for the <code>OpProjectAdministrationService</code> administrationService.
 *
 * @author horia.chiorean
 * @author ovidiu.lupas
 */
//<FIXME author="Ovidiu Lupas" description="test persistence exceptions ">
// </FIXME>


public class OpProjectAdministrationServiceTest extends OpServiceAbstractTest {
   /**
    * Various predefined project id that will be used in the tests.
    */
   private static final String NONEXISTENT_PROJECT_ID = "OpProjectNode.1206.xid";
   private static final String NONEXISTENT_PORTFOLIO_ID = "OpProjectNode.1456.xid";


   /**
    * The project administrationService object that is being tested.
    */
   protected OpProjectAdministrationService administrationService = null;

   /**
    * A user that acts as the logged on user.
    */
   private OpUser sessionUser = null;

   /* Everyone persisted group*/
   private OpGroup everyone = null;

   /**
    * Project that is being tested.
    */
   private String projectName;
   private String projectDescription;
   private OpProjectNode project;


   /**
    * Project plan for project
    */
   protected OpProjectPlan projectPlan;
   /**
    * Portfolio that is being tested
    */
   private String portfolioName;
   private String portfolioDescription;
   private OpProjectNode portfolio;
   private OpProjectNode superPortfolio;

   /**
    * Portfolio/Project/Project Template String id
    */
   protected final String PORTFOLIO_ID = "OpProjectNode.1897.xid";
   protected final String PROJECT_ID = "OpProjectNode.1500.xid";


   protected final int SESSION_USER_ID_LONG = 666;
   protected final String SESSION_USED_ID = "OpUser."+SESSION_USER_ID_LONG+".xid";
   protected final int EVERYONE_ID_LONG = 222;
   protected final String EVERYONE_GROUP_ID = "OpGroup."+EVERYONE_ID_LONG+".xid";

   /**
    * HQL
    */
   protected static final String SELECT_PROJECT_BY_NAME = "select project from OpProjectNode project where project.Name = ?";
   protected static final String SELECT_PROJECT_FIRST_ACTIVITY_START = "select min(activity.Start) from OpActivity as activity where activity.ProjectPlan.ID = ?";

   private static final String SELECT_PROJECT_PORTFOLIO_BY_NAME = "select project from OpProjectNode project where project.Name = ?";

   private static final String SELECT_ROOT_PORTFOLIO_BY_NAME = "select portfolio from OpProjectNode as portfolio where portfolio.Name = ? and portfolio.Type = ?";

   private static final String SELECT_PORTFOLIO_ID_BY_PROJECT_ID = "select project.SuperNode.ID from OpProjectNode as project " +
        "where project.ID in (:projectIds) and project.Type = (:projectType)";

   private static final String SELECT_SUPER_PORTFOLIO_ID_BY_PORTFOLIO_ID = "select portfolio.SuperNode.ID from OpProjectNode as portfolio " +
        "where portfolio.ID in (:portfolioIds) and portfolio.Type = (:projectType)";


   private static final String SELECT_ACCESSIBLE_PROJECTS = "select project from OpProjectNode as project " +
        "where project.ID in (:projectIds) and project.SuperNode.ID in (:accessiblePortfolioIds)";

   private static final String SELECT_ACCESSIBLE_PORTFOLIOS = "select portfolio from OpProjectNode as portfolio " +
        "where portfolio.ID in (:portfolioIds) and portfolio.SuperNode.ID in (:accessibleSuperPortfolioIds)";

   private static final String SELECT_PROJECT_WORK_RECORDS = "select count(workrecord) from OpProjectPlan projectPlan join projectPlan.Activities activity join activity.Assignments assignment join assignment.WorkRecords workrecord where projectPlan.ID = ?";


   /**
    * @see onepoint.project.test.OpServiceAbstractTest#setUp()
    */
   protected void setUp() {
      super.setUp();
      //create the administrationService
      administrationService =  getTestedService();
      //the session user
      sessionUser = new OpUser();
      sessionUser.setID(SESSION_USER_ID_LONG);
      sessionUser.setLevel(new Byte(OpUser.MANAGER_USER_LEVEL));
      //the everyone group
      everyone = new OpGroup();
      everyone.setID(EVERYONE_ID_LONG);
      everyone.setUserAssignments(new HashSet());

      //project plan for project
      projectPlan = new OpProjectPlan();
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setActivities(new HashSet());
      projectPlan.setActivityAssignments(new HashSet());
      projectPlan.setActivityAttachments(new HashSet());
      projectPlan.setDependencies(new HashSet());
      projectPlan.setDependencies(new HashSet());
      projectPlan.setWorkPeriods(new HashSet());

      //persistet project
      projectName = "FirstProject";
      projectDescription = "FirstProjectDescription";
      //create a project
      project = new OpProjectNode();
      project.setType(OpProjectNode.PROJECT);
      project.setID(1);
      project.setName(projectName);
      project.setDescription(projectDescription);
      project.setStart(XCalendar.today());
      project.setFinish(XCalendar.today());
      project.setAssignments(new HashSet());
      project.setGoals(new HashSet());
      project.setToDos(new HashSet());
      project.setPlan(projectPlan);


      //create a super portfolio
      superPortfolio = new OpProjectNode();
      superPortfolio.setType(OpProjectNode.PORTFOLIO);
      superPortfolio.setName(portfolioName);
      superPortfolio.setDescription(portfolioDescription);
      superPortfolio.setID(1);

      //persisted portfolio
      portfolioName = "FirstPortfolio";
      portfolioDescription = "FirstPortfolioDescription";
      //create a portfolio
      portfolio = new OpProjectNode();
      portfolio.setType(OpProjectNode.PORTFOLIO);
      portfolio.setName(portfolioName);
      portfolio.setDescription(portfolioDescription);
      Set projectsSet = new HashSet();
      projectsSet.add(project);
      project.setSuperNode(portfolio);
      portfolio.setSubNodes(projectsSet);
      portfolio.setID(3);

      //empty list of queryResults
      queryResults = new ArrayList();
   }

   protected OpProjectAdministrationService getTestedService() {
     return new OpProjectAdministrationService();
   }


   /**
    * @see onepoint.project.test.OpServiceAbstractTest#invocationMatch(org.jmock.core.Invocation)
    */
   public Object invocationMatch(Invocation invocation)
        throws IllegalArgumentException {
      String methodName = invocation.invokedMethod.getName();

      //get Object
      if (methodName.equals(GET_OBJECT_METHOD)) {
         String entityId = (String) invocation.parameterValues.get(0);
         if (entityId.equals(PORTFOLIO_ID)) {
            return portfolio;
         }
         else if (entityId.equals(PROJECT_ID)) {
            return project;
         }
         else if (entityId.equals(SESSION_USED_ID)) {
            return sessionUser;
         }
         else if (entityId.equals(EVERYONE_GROUP_ID)) {
            return everyone;
         }
         else if (entityId.equals(NONEXISTENT_PROJECT_ID) || entityId.equals(NONEXISTENT_PORTFOLIO_ID)) {
            return null;
         }
      }

      //getUser
      else if (methodName.equals(USER_METHOD)) {
         return sessionUser;
      }
      //get everyone group
      else if (methodName.equals(EVERYONE_METHOD)) {
          return everyone;
      }
      //check acces level
      else if (methodName.equals(CHECK_ACCESS_LEVEL_METHOD)) {
         return Boolean.TRUE;
      }
      //accesible portfolio and super pool ids
      else if (methodName.equals(ACCESSIBLE_IDS_METHOD)) {
         Set accessiblePortfolioIds = new HashSet();
         accessiblePortfolioIds.add(new Long(portfolio.getID()));
         return accessiblePortfolioIds;
      }
      //no such method was found
      throw new IllegalArgumentException("Invalid method name:" + methodName + " for this stub");
   }


   /**
    * Tests that a new project will be corectly inserted provided that the project data is correct
    */
   public void testInsertNewProject() {
      //create the request
      XMessage request = new XMessage();
      Map projectData = creatProjectData("ProjectName", "ProjectDescription", XCalendar.today(), XCalendar.today(), 0);
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, projectData);

      //add a goal
      XComponent goalsDataSet = new XComponent(XComponent.DATA_SET);
      XComponent goal = createGoalData(false, "Goal One Subject", 123);
      goalsDataSet.addChild(goal);
      request.setArgument(OpProjectAdministrationService.GOALS_SET, goalsDataSet);

      //add a _todo
      XComponent toDoDataSet = new XComponent(XComponent.DATA_SET);
      XComponent todo = createTodoData(false, "ToDo One Subject", 123, new Date(10000));
      toDoDataSet.addChild(todo);
      request.setArgument(OpProjectAdministrationService.TO_DOS_SET, toDoDataSet);

      Constraint testProjectNode = createProjectNodeConstraint(projectData);

      //no projects should be found by find() on broker
      queryResults.clear();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the project is searched for (a project can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(projectData.get(OpProjectNode.NAME))});
      //iterate over results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(String.valueOf(PORTFOLIO_ID))).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the project node must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testProjectNode);
      //the project plan must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(isA(OpProjectPlan.class));

      //the goal must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createGoalConstraint(goalsDataSet));

      //the to_do must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createToDoConstraint(toDoDataSet));

      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(administrationService.insertProject((XSession) mockSession.proxy(), request));

   }




   /**
    * Creates a goal data component
    *
    * @param complete If the goal was completed
    * @param subject  The subject of this goal
    * @param priority The priority of the goal
    * @return <code>XComponent<code> representing a DATA_ROW with data for a goal.
    */
   private XComponent createGoalData(boolean complete, String subject, int priority) {
      XComponent dataCell;
      XComponent goal = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(complete); //done
      goal.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(subject); //Subject
      goal.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(priority); //Priority
      goal.addChild(dataCell);
      return goal;
   }

   /**
    * Creates a to do data component
    *
    * @param complete If the to do was completed
    * @param subject  The subject of this to do
    * @param priority The priority of the to do
    * @return <code>XComponent<code> representing a DATA_ROW with data for a to do.
    */
   private XComponent createTodoData(boolean complete, String subject, int priority, Date due) {
      XComponent dataCell;
      XComponent todo = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(complete); //done
      todo.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(subject); //Subject
      todo.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(priority); //Priority
      todo.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDateValue(due); //Due
      todo.addChild(dataCell);
      return todo;
   }

   /**
    * Tests the behavior of insertProject for an invalid project data.
    */
   public void testInsertProjectWrongData() {
      XComponent goals = new XComponent(XComponent.DATA_SET);
      XComponent toDos = new XComponent(XComponent.DATA_SET);

      checkInsertProjectWrongData("", "projectDescription", XCalendar.today(), XCalendar.today(), 0, goals, toDos);
      checkInsertProjectWrongData("projectName", "projectDescription", null, XCalendar.today(), 0, goals, toDos);
      checkInsertProjectWrongData("projectName", "projectDescription", XCalendar.today(),
                              new Date(XCalendar.today().getTime() - XCalendar.MILLIS_PER_WEEK), 0, goals, toDos);
      //start date before end date
      Date startDate = new Date(XCalendar.today().getTime() + XCalendar.MILLIS_PER_DAY);
      checkInsertProjectWrongData("projectName", "projectDescription", startDate, XCalendar.today(), 0, goals, toDos);
      checkInsertProjectWrongData("projectName", "projectDescription", XCalendar.today(), XCalendar.today(), -5, goals, toDos);

      //goals
      goals = new XComponent(XComponent.DATA_SET);
      toDos = new XComponent(XComponent.DATA_SET);
      goals.addChild(createGoalData(true, "GoalSubject", -10));
      checkInsertProjectToDosOrGoalsWrongData("projectName", "projectDescription", XCalendar.today(), XCalendar.today(), 0, goals, toDos);

      goals = new XComponent(XComponent.DATA_SET);
      toDos = new XComponent(XComponent.DATA_SET);
      toDos.addChild(createTodoData(true, "ToDoSubject", -1, XCalendar.today()));
      checkInsertProjectToDosOrGoalsWrongData("projectName", "projectDescription", XCalendar.today(), XCalendar.today(), 0, goals, toDos);


   }

   /**
    * Tests the behavior of insertProject for an invalid project data.
    */
   private void checkInsertProjectWrongData(String projectName, String projectDescription, Date startDate, Date endDate,
        double projectBudget, XComponent goals, XComponent todos) {
      //create the request
      XMessage request = new XMessage();
      Map projectData = creatProjectData(projectName, projectDescription, startDate, endDate, projectBudget);
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, projectData);

      request.setArgument(OpProjectAdministrationService.GOALS_SET, goals);
      request.setArgument(OpProjectAdministrationService.TO_DOS_SET, todos);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      // a new broker must  not be created
      mockSession.expects(never()).method(NEW_BROKER_METHOD);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the portfolio must not be persisted
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must not be closed
      mockBroker.expects(never()).method(CLOSE_METHOD);

      XMessage result = administrationService.insertProject((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of insertProject for an invalid project data todos and goals.
    */
   private void checkInsertProjectToDosOrGoalsWrongData(String projectName, String projectDescription, Date startDate, Date endDate,
        double projectBudget, XComponent goals, XComponent todos) {
      //create the request
      XMessage request = new XMessage();
      Map projectData = creatProjectData(projectName, projectDescription, startDate, endDate, projectBudget);
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, projectData);

      request.setArgument(OpProjectAdministrationService.GOALS_SET, goals);
      request.setArgument(OpProjectAdministrationService.TO_DOS_SET, todos);

      //no projects should be found by find() on broker
      queryResults.clear();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the project is searched for (a project can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(projectData.get(OpProjectNode.NAME))});
      //iterate over results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(String.valueOf(PORTFOLIO_ID))).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the project must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createProjectNodeConstraint(projectData));
       //the project plan must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(isA(OpProjectPlan.class));
      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      mockTransaction.expects(once()).method(ROLLBACK_METHOD);
      //broker must  be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      XMessage result = administrationService.insertProject((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of insertProject for an already existent project data.
    */
   public void testInsertAlreadyExistentProject() {
      //create the request
      XMessage request = new XMessage();
      Map projectData = creatProjectData(projectName, "NewProjectDescription", XCalendar.today(), XCalendar.today(), 0);
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, projectData);

      XComponent goalsDataSet = new XComponent(XComponent.DATA_SET);
      request.setArgument(OpProjectAdministrationService.GOALS_SET, goalsDataSet);
      XComponent toDoDataSet = new XComponent(XComponent.DATA_SET);
      request.setArgument(OpProjectAdministrationService.TO_DOS_SET, toDoDataSet);

      //one project should be found
      queryResults.clear();
      queryResults.add(project);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the project is searched for (a project can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(projectData.get(OpProjectNode.NAME))});
      //iterate over results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      ///a new transaction will never be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the portfolio must not be persisted
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = administrationService.insertProject((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of deleteProjects for an existing project.
    */
   public void testDeleteProject() {
      XMessage request = new XMessage();
      //the id of the project that will be deleted
      List projectIds = new ArrayList();
      projectIds.add(PROJECT_ID);
      //set up the request
      request.setArgument(OpProjectAdministrationService.PROJECT_IDS, projectIds);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the project's portfolio is searched for
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PORTFOLIO_ID_BY_PROJECT_ID)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //clear the list and add a portfolio id
            queryResults.clear();
            queryResults.add(new Long(portfolio.getID()));
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_PORTFOLIO_ID_BY_PROJECT_ID);
         }
      });
      mockQuery.expects(atLeastOnce()).method(SET_COLLECTION_METHOD);
      mockQuery.expects(atLeastOnce()).method(SET_BYTE_METHOD).with(new Constraint[]{eq("projectType"),eq(OpProjectNode.PROJECT)});
      //list  results
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(methodStub);
      //session accesible portfolio ids
      mockSession.expects(once()).method(ACCESSIBLE_IDS_METHOD).will(methodStub);

      //the accessible projects for delete operation
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ACCESSIBLE_PROJECTS)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //clear the list and project
            queryResults.clear();
            queryResults.add(project);
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_ACCESSIBLE_PROJECTS);
         }
      });
      //the project work records count
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_WORK_RECORDS)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //no work records for project inside portfolio
            queryResults.clear();
            queryResults.add(new Integer(0));
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_PROJECT_WORK_RECORDS);
         }
      });
      //set project id
      mockQuery.expects(once()).method(SET_LONG_METHOD);

      //iterate  results
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //delete object object method
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(same(project));

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = administrationService.deleteProjects((XSession) mockSession.proxy(), request);
      assertNull("No Error message should have been returned", result);
   }

   /**
    * Tests the behavior of deleteProjects for a non accesible project.
    */
   public void testDeleteNonAccesibleProject() {
      XMessage request = new XMessage();
      ArrayList projectIds = new ArrayList();
      projectIds.add(NONEXISTENT_PROJECT_ID);
      //set up the request
      request.setArgument(OpProjectAdministrationService.PROJECT_IDS, projectIds);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the project's portfolio is searched for
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PORTFOLIO_ID_BY_PROJECT_ID)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //clear the list and add a portfolio id
            queryResults.clear();
            queryResults.add(new Long(portfolio.getID()));
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_PORTFOLIO_ID_BY_PROJECT_ID);
         }
      });
      mockQuery.expects(atLeastOnce()).method(SET_COLLECTION_METHOD);
      mockQuery.expects(atLeastOnce()).method(SET_BYTE_METHOD).with(new Constraint[]{eq("projectType"),eq(OpProjectNode.PROJECT)});
      //list  results
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(methodStub);
      //session accesible portfolio ids
      mockSession.expects(once()).method(ACCESSIBLE_IDS_METHOD).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            return new HashSet();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_SUPER_PORTFOLIO_ID_BY_PORTFOLIO_ID);
         }
      });

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = administrationService.deleteProjects((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());

   }

   /**
    * Tests the behavior of updateProject for an existent project and accurate data.
    */
   public void testUpdateProject() {
      //create the request
      XMessage request = new XMessage();
      //set up the project id
      request.setArgument(OpProjectAdministrationService.PROJECT_ID, PROJECT_ID);
      //set up the updated project data
      Map projectData = creatProjectData("NewProjectName", "NewProjectDescription", XCalendar.today(), XCalendar.today(), 0);
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, projectData);

      XComponent goalsDataSet = new XComponent(XComponent.DATA_SET);
      request.setArgument(OpProjectAdministrationService.GOALS_SET, goalsDataSet);
      XComponent toDoDataSet = new XComponent(XComponent.DATA_SET);
      request.setArgument(OpProjectAdministrationService.TO_DOS_SET, toDoDataSet);


      Constraint testProject = createProjectNodeConstraint(projectData);

      //no projects should be found with the same name
      queryResults.clear();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(PROJECT_ID)).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //the project is searched for (a project can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(projectData.get(OpProjectNode.NAME))});
      //iterate over results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the project node / project plan must be updated
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(testProject);
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(isA(OpProjectPlan.class));

      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(administrationService.updateProject((XSession) mockSession.proxy(), request));

   }

   /**
    * Tests the behavior of updateProject for a non existent project name and accurata data.
    */
   public void testUpdateNonExistentProject() {
      //create the request
      XMessage request = new XMessage();
      //set up the portfolio id
      request.setArgument(OpProjectAdministrationService.PROJECT_ID, NONEXISTENT_PROJECT_ID);
      //set up the updated project data
      Map projectData = creatProjectData("NewProjectName", "NewProjectDescription", XCalendar.today(), XCalendar.today(), 0);
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, projectData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(NONEXISTENT_PROJECT_ID)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the portfolio must not be updated
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      //<FIXME author="Ovidiu Lupas" description="Fails becouse error message is missing in case that the project is unexistent">
      XMessage result = administrationService.updateProject((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result);
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
      //</FIXME>
   }

   /**
    * Tests the behavior of updateProject with an already existent project.
    */
   public void testUpdateProjectWithExistentData() {
      //create the request
      XMessage request = new XMessage();
      //set up the project id
      request.setArgument(OpProjectAdministrationService.PROJECT_ID, PROJECT_ID);
      //set up the updated project data
      Map projectData = creatProjectData(projectName, "NewProjectDescription", XCalendar.today(), XCalendar.today(), 0);
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, projectData);

      //the persisted project with the same name but different id
      OpProjectNode existentProject = new OpProjectNode();
      existentProject.setType(OpProjectNode.PROJECT);
      existentProject.setName(projectName);
      existentProject.setDescription(projectDescription);
      existentProject.setID(2);

      //one portfolio should be found by find() on broker
      queryResults.clear();
      queryResults.add(existentProject);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(PROJECT_ID)).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //the project is searched for (a project can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(projectData.get(OpProjectNode.NAME))});
      //iterate over results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will never be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the portfolio must not be updated
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = administrationService.updateProject((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of updateProject for an invalid project data.
    */
   public void testUpdateProjectWrongData() {
      checkUpdateProjectWrongData("", "projectDescription", XCalendar.today(), XCalendar.today(), 0);
      checkUpdateProjectWrongData("projectName", "projectDescription", null, XCalendar.today(), 0);
      //start date before end date
      Date startDate = new Date(XCalendar.today().getTime() + XCalendar.MILLIS_PER_DAY);
      checkUpdateProjectWrongData("projectName", "projectDescription", startDate, XCalendar.today(), 0);
      checkUpdateProjectWrongData("projectName", "projectDescription", XCalendar.today(), XCalendar.today(), -5);
   }

   /**
    * Tests the behavior of updateProject for an invalid project data.
    */
   private void checkUpdateProjectWrongData(String projectName, String projectDescription, Date startDate, Date endDate,
        double projectBudget) {
      //create the request
      XMessage request = new XMessage();
      //set up the project id
      request.setArgument(OpProjectAdministrationService.PROJECT_ID, PROJECT_ID);
      //set up the updated project data
      Map projectData = creatProjectData(projectName, projectDescription, startDate, endDate, projectBudget);
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, projectData);

      XComponent goalsDataSet = new XComponent(XComponent.DATA_SET);
      request.setArgument(OpProjectAdministrationService.GOALS_SET, goalsDataSet);
      XComponent toDoDataSet = new XComponent(XComponent.DATA_SET);
      request.setArgument(OpProjectAdministrationService.TO_DOS_SET, toDoDataSet);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //get object
      mockBroker.expects(never()).method(GET_OBJECT_METHOD).with(eq(PROJECT_ID)).will(methodStub);

      // the check acces level must not be performed
      mockSession.expects(never()).method(CHECK_ACCESS_LEVEL_METHOD);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will never be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the portfolio must not be persisted
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must not be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = administrationService.updateProject((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());

   }

   /**
    * Tests that a new portfolio will be corectly inserted provided that the portfolio data is correct
    */
   public void testInsertNewPortfolio() {
      //create the request
      XMessage request = new XMessage();
      HashMap portfolioData = creatPortfolioData("PortfolioName", "Portfolio Description");
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_DATA, portfolioData);

      Constraint testPortfolio = createPortfolioConstraint(portfolioData);

      //no portfolios should be found
      queryResults.clear();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the portfolio is searched for (a portfolio can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_PORTFOLIO_BY_NAME)).will(methodStub);
      //the root portfolio is searched for
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ROOT_PORTFOLIO_BY_NAME)).will(methodStub);

      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)});
      mockQuery.expects(once()).method(SET_BYTE_METHOD).with(new Constraint[]{eq(1), eq(OpProjectNode.PORTFOLIO)});
      //list results for super portfolio
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.clear();
            queryResults.add(superPortfolio);
            return queryResults;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_PROJECT_PORTFOLIO_BY_NAME);
         }
      });

      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(portfolioData.get(OpProjectNode.NAME))});

      //iterate over results for existent portfolio with the same name
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.clear();
            return queryResults.iterator();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_PROJECT_PORTFOLIO_BY_NAME);
         }
      });

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the portfolio must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testPortfolio);

      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(administrationService.insertPortfolio((XSession) mockSession.proxy(), request));

   }

   /**
    * Tests the behavior of insertPortfolio if the portfolio name is missing or is empty.
    */
   public void testInsertPortfolioWrongData() {
      //create the request
      XMessage request = new XMessage();
      HashMap portfolioData = creatPortfolioData(null, "Portfolio Description");
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_DATA, portfolioData);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      // a new broker must not be created
      mockSession.expects(never()).method(NEW_BROKER_METHOD).will(methodStub);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the portfolio must not be persisted
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker.close method must not be called
      mockBroker.expects(never()).method(CLOSE_METHOD);

      XMessage result = administrationService.insertPortfolio((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of insertPortfolio if the portfolio already exists.
    */
   public void testInsertAlreadyExistentPortfolio() {
      //create the request
      XMessage request = new XMessage();
      HashMap portfolioData = creatPortfolioData("PortfolioName", "Portfolio Description");
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_DATA, portfolioData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the portfolio is searched for (a portfolio can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_PORTFOLIO_BY_NAME)).will(methodStub);

      //the root portfolio is searched for
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ROOT_PORTFOLIO_BY_NAME)).will(methodStub);

      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)});
      mockQuery.expects(once()).method(SET_BYTE_METHOD).with(new Constraint[]{eq(1), eq(OpProjectNode.PORTFOLIO)});
      //list results for super portfolio
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.clear();
            queryResults.add(superPortfolio);
            return queryResults;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_PROJECT_PORTFOLIO_BY_NAME);
         }
      });

      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(portfolioData.get(OpProjectNode.NAME))});

      //iterate over results for existent portfolio with the same name
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.clear();
            queryResults.add(portfolio);
            return queryResults.iterator();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_PROJECT_PORTFOLIO_BY_NAME);
         }
      });

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the portfolio must not be persisted
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = administrationService.insertPortfolio((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of updatePortfolio for an existent portfolio and accurate data.
    */
   public void testUpdatePortfolio() {
      //create the request
      XMessage request = new XMessage();
      //set up the portfolio id
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_ID, PORTFOLIO_ID);
      //set up the updated portfolio data
      HashMap portfolioData = creatPortfolioData("PortfolionName", "Portfolio Description");
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_DATA, portfolioData);

      Constraint testPortfolio = createPortfolioConstraint(portfolioData);

      //no portfolios should be found by find() on broker
      queryResults.clear();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(PORTFOLIO_ID)).will(methodStub);

      //the portfolio is searched for (a portfolio can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_PORTFOLIO_BY_NAME)).will(methodStub);

      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(portfolioData.get(OpProjectNode.NAME))});

      //the root portfolio is searched for
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ROOT_PORTFOLIO_BY_NAME)).will(methodStub);

      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)});
      mockQuery.expects(once()).method(SET_BYTE_METHOD).with(new Constraint[]{eq(1), eq(OpProjectNode.PORTFOLIO)});
      //list results for super portfolio
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.clear();
            queryResults.add(superPortfolio);
            return queryResults;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_PROJECT_PORTFOLIO_BY_NAME);
         }
      });

      //iterate over results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the portfolio must be updated
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(testPortfolio);

      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(administrationService.updatePortfolio((XSession) mockSession.proxy(), request));

   }

   /**
    * Tests the behavior of updatePortfolio for a non existent portfolio name and accurata data.
    */
   public void testUpdateNonExistentPortfolio() {
      //create the request
      XMessage request = new XMessage();
      //set up the portfolio id
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_ID, NONEXISTENT_PORTFOLIO_ID);
      //set up the updated portfolio data
      HashMap portfolioData = creatPortfolioData("PortfolionName", "Portfolio Description");
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_DATA, portfolioData);
      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(NONEXISTENT_PORTFOLIO_ID)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the portfolio must not be updated
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = administrationService.updatePortfolio((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of updatePortfolio if the portfolio name is missing or is empty.
    */
   public void testUpdatePortfolioWrongData() {
      //create the request
      XMessage request = new XMessage();
      //set up the portfolio id
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_ID, PORTFOLIO_ID);
      //set up the updated portfolio data
      HashMap portfolioData = creatPortfolioData(null, "Portfolio Description");
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_DATA, portfolioData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //get portfolio object
      mockBroker.expects(never()).method(GET_OBJECT_METHOD);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the portfolio must not be updated
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = administrationService.updatePortfolio((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of updatePortfolio with an already existing portfolio.
    */
   public void testUpdatePortfolioWithExistentData() {
      //create the request
      XMessage request = new XMessage();
      //set up the portfolio id
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_ID, PORTFOLIO_ID);
      //set up the updated portfolio data
      HashMap portfolioData = creatPortfolioData("PortfolionName", "Portfolio Description");
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_DATA, portfolioData);

      //the persisted portfolio with the same name but different id
      OpProjectNode existentPortfolio = new OpProjectNode();
      existentPortfolio.setType(OpProjectNode.PORTFOLIO);
      existentPortfolio.setName(portfolioName);
      existentPortfolio.setDescription(portfolioDescription);
      existentPortfolio.setID(2);

      //one portfolio should be found
      queryResults.clear();
      queryResults.add(existentPortfolio);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(PORTFOLIO_ID)).will(methodStub);

      //the portfolio is searched for (a portfolio can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_PORTFOLIO_BY_NAME)).will(methodStub);

      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(portfolioData.get(OpProjectNode.NAME))});

      //iterate over results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the portfolio must not be updated
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = administrationService.updatePortfolio((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of deletePortfolios for an existing portfolio.
    */
   public void testDeletePortfolio() {
      XMessage request = new XMessage();
      ArrayList portfolioIds = new ArrayList();
      portfolioIds.add(PORTFOLIO_ID);
      //set up the request
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_IDS, portfolioIds);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the project's portfolio is searched for
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUPER_PORTFOLIO_ID_BY_PORTFOLIO_ID)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //clear the list and add the super portfolio id
            queryResults.clear();
            queryResults.add(new Long(superPortfolio.getID()));
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_SUPER_PORTFOLIO_ID_BY_PORTFOLIO_ID);
         }
      });
      mockQuery.expects(atLeastOnce()).method(SET_COLLECTION_METHOD);
      mockQuery.expects(atLeastOnce()).method(SET_BYTE_METHOD).with(new Constraint[]{eq("projectType"),eq(OpProjectNode.PORTFOLIO)});
      //list  results
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(methodStub);
      //session accesible super portfolio ids
      mockSession.expects(once()).method(ACCESSIBLE_IDS_METHOD).will(methodStub);

      //the accessible portfolios for delete operation
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ACCESSIBLE_PORTFOLIOS)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //clear the list and a portfolio
            queryResults.clear();
            queryResults.add(portfolio);
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_ACCESSIBLE_PORTFOLIOS);
         }
      });

      //the project work records count
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_WORK_RECORDS)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //no work records for project inside portfolio
            queryResults.clear();
            queryResults.add(new Integer(0));
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_PROJECT_WORK_RECORDS);
         }
      });
      //set project id
      mockQuery.expects(once()).method(SET_LONG_METHOD);

      //iterate  results
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //delete project inside portfolio method
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(isA(OpProjectNode.class));
      //delete portfolio method
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(same(portfolio));

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(administrationService.deletePortfolios((XSession) mockSession.proxy(), request));
   }

   /**
    * Tests the behavior of deletePortfolios for a non accesible portfolio.
    */
   public void testDeleteNonAccesiblePortfolio() {
      XMessage request = new XMessage();
      ArrayList portfolioIds = new ArrayList();
      portfolioIds.add(NONEXISTENT_PORTFOLIO_ID);
      //set up the request
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_IDS, portfolioIds);
      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the project's portfolio is searched for
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUPER_PORTFOLIO_ID_BY_PORTFOLIO_ID)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //clear the list and add the super portfolio id
            queryResults.clear();
            queryResults.add(new Long(superPortfolio.getID()));
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_SUPER_PORTFOLIO_ID_BY_PORTFOLIO_ID);
         }
      });
      mockQuery.expects(atLeastOnce()).method(SET_COLLECTION_METHOD);
      mockQuery.expects(atLeastOnce()).method(SET_BYTE_METHOD).with(new Constraint[]{eq("projectType"),eq(OpProjectNode.PORTFOLIO)});
      //list  results
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(methodStub);
      //session accesible super portfolio ids
      mockSession.expects(once()).method(ACCESSIBLE_IDS_METHOD).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            return new HashSet();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_SUPER_PORTFOLIO_ID_BY_PORTFOLIO_ID);
         }
      });
      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = administrationService.deletePortfolios((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Creates a portfolio data given the properties of the new portfolio
    *
    * @param portfolioName a <code>String </code> representing the name for the portfolio
    * @param description   a <code>String</code>representing the description of the portfolio
    * @return a new <code>XStruct</code> with the portfolio data
    */
   private HashMap creatPortfolioData(String portfolioName, String description) {
      Map portfolioValues = new HashMap();
      portfolioValues.put(OpProjectNode.NAME, portfolioName);
      portfolioValues.put(OpProjectNode.DESCRIPTION, description);
      portfolioValues.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));
      return transformToXStruct(portfolioValues);
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if a portfolio is the expected one.
    *
    * @return a new portfolio constraint
    */
   private Constraint createPortfolioConstraint(final HashMap portfolioData) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpProjectNode)) {
               return false;
            }
            OpProjectNode portfolio = (OpProjectNode) object;
            if (portfolio.getType() != OpProjectNode.PORTFOLIO)
               return false;
            if (!portfolioData.get(OpProjectNode.NAME).equals(portfolio.getName())) {
               return false;
            }
            return portfolioData.get(OpProjectNode.DESCRIPTION).equals(portfolio.getDescription());
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if a portfolio is the expected one");
         }
      };
   }

   /**
    * Creates a project data given the properties of the new project
    *
    * @param projectName   a <code>String</code> representing the project name
    * @param projectName   a <code>String</code> representing the project description
    * @param startDate     a <code>Date</code> representing the start date of the project
    * @param endDate       a <code>Date</code> representing the end date of the project
    * @param projectBudget a <code>double</code> representing the budget of the project
    * @return a new <code>XStruct</code> with the project data
    */
   protected Map creatProjectData(String projectName, String projectDescription, Date startDate, Date endDate, double projectBudget) {
      Map projectValues = new HashMap();
      projectValues.put(OpProjectNode.NAME, projectName);
      projectValues.put(OpProjectNode.DESCRIPTION, projectDescription);
      projectValues.put(OpProjectNode.START, startDate);
      projectValues.put(OpProjectNode.FINISH, endDate);
      projectValues.put(OpProjectNode.BUDGET, new Double(projectBudget));
      projectValues.put("PortfolioID", String.valueOf(PORTFOLIO_ID));
      projectValues.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));
      return projectValues;
      //return transformToXStruct(projectValues);
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if a project is the expected one.
    *
    * @return <code>Constraint</code> representing a project constraint
    */
   protected Constraint createProjectNodeConstraint(final Map projectData) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpProjectNode)) {
               return false;
            }
            OpProjectNode project = (OpProjectNode) object;
            if (project.getType() != OpProjectNode.PROJECT)
               return false;
            if (!projectData.get(OpProjectNode.NAME).equals(project.getName())) {
               return false;
            }
            if (!projectData.get(OpProjectNode.DESCRIPTION).equals(project.getDescription())) {
               return false;
            }
            if (!projectData.get(OpProjectNode.START).equals(project.getStart())) {
               return false;
            }
            if (!projectData.get(OpProjectNode.FINISH).equals(project.getFinish())) {
               return false;
            }
            return (((Double) projectData.get(OpProjectNode.BUDGET)).doubleValue() == project.getBudget());
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if a project is the expected one");
         }
      };
   }

   /**
    * Creates a constraint for a OpGoal instance .
    *
    * @param goalDataSet data set with the expected values to be found on the project
    * @return a new Constraint object that will check if a OpGoal instance is the expected one
    */
   private Constraint createGoalConstraint(final XComponent goalDataSet) {
      return new Constraint() {
         public boolean eval(Object object) {
            if (!(object instanceof OpGoal)) {
               return false;
            }
            OpGoal goal = (OpGoal) object;
            //assume that the data set has only one data row
            XComponent dataRow = (XComponent) goalDataSet.getChild(0);

            XComponent completedDataCell = (XComponent) dataRow.getChild(0);
            if (!(goal.getCompleted() == completedDataCell.getBooleanValue())) {
               return false;
            }

            XComponent subjectDataCell = (XComponent) dataRow.getChild(1);
            if (!(goal.getName().equals(subjectDataCell.getStringValue()))) {
               return false;
            }

            XComponent priorityDataCell = (XComponent) dataRow.getChild(2);
            return (goal.getPriority() == priorityDataCell.getIntValue());

         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Check if an toDo is the expected one");
         }
      };
   }

   /**
    * Creates a constraint for a OpToDo instance .
    *
    * @param toDoDataSet data set with the expected values to be found on the project
    * @return a new Constraint object that will check if a OpToDo instance is the expected one
    */
   private Constraint createToDoConstraint(final XComponent toDoDataSet) {
      return new Constraint() {
         public boolean eval(Object object) {
            if (!(object instanceof OpToDo)) {
               return false;
            }
            OpToDo toDo = (OpToDo) object;
            //assume that the data set has only one data row
            XComponent dataRow = (XComponent) toDoDataSet.getChild(0);

            XComponent completedDataCell = (XComponent) dataRow.getChild(0);
            if (!(toDo.getCompleted() == completedDataCell.getBooleanValue())) {
               return false;
            }

            XComponent subjectDataCell = (XComponent) dataRow.getChild(1);
            if (!(toDo.getName().equals(subjectDataCell.getStringValue()))) {
               return false;
            }
            XComponent priorityDataCell = (XComponent) dataRow.getChild(2);
            if (!(toDo.getPriority() == priorityDataCell.getIntValue())) {
               return false;
            }

            XComponent dueDataCell = (XComponent) dataRow.getChild(3);
            return toDo.getDue().equals(dueDataCell.getDateValue());
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Check if an toDo is the expected one");
         }
      };
   }

}
