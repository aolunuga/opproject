/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class contains helper methods for managing projects data
 *
 * @author lucian.furtos
 */
public class OpProjectTestDataFactory extends OpTestDataFactory {

   private final static String SELECT_PORTOFOLIO_ID_BY_NAME_QUERY = "select portofolio.ID from OpProjectNode as portofolio where portofolio.Name = ? and portofolio.Type = 1";
   private final static String SELECT_PROJECT_ID_BY_NAME_QUERY = "select project.ID from OpProjectNode as project where project.Name = ? and project.Type = 3";
   private final static String SELECT_PROJECT_PLAN_ID_BY_NAME_QUERY = "select plan.ID from OpProjectPlan as plan where plan.Name = ?";
   private final static String SELECT_ASSIGNMENT_ID_BY_ACTIVITY_NAME_QUERY = "select assignment.ID from OpAssignment as assignment where assignment.Resource.Name = ?";

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public OpProjectTestDataFactory(OpProjectSession session) {
      super(session);
   }

   /**
    * Get a portofolio by the name
    *
    * @param portofolioName the portofolio name
    * @return an instance of <code>OpProjectNode</code>
    */
   public OpProjectNode getPortofolioByName(String portofolioName) {
      String locator = getPortofolioId(portofolioName);
      if (locator != null) {
         return getPortofolioById(locator);
      }

      return null;
   }

   /**
    * Get a portofolio by the locator
    *
    * @param locator the uniq identifier (locator) of an entity
    * @return an instance of <code>OpProjectNode</code>
    */
   public OpProjectNode getPortofolioById(String locator) {
      return getProjectById(locator);
   }

   /**
    * Get the uniq identifier of a portofolio by name
    *
    * @param portofolioName the portofolio name
    * @return the uniq identifier (locator) of an entity
    */
   public String getPortofolioId(String portofolioName) {
      OpBroker broker = session.newBroker();
      try {
         Long projId = null;

         OpQuery query = broker.newQuery(SELECT_PORTOFOLIO_ID_BY_NAME_QUERY);
         query.setString(0, portofolioName);
         Iterator projectIt = broker.iterate(query);
         if (projectIt.hasNext()) {
            projId = (Long) projectIt.next();
         }

         if (projId != null) {
            return OpLocator.locatorString(OpProjectNode.PROJECT_NODE, projId);
         }
         return null;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get all the portofolios
    *
    * @param session a broker instance
    * @return a <code>List</code> of <code>OpProjectNode</code>
    */
   public List<OpProjectNode> getAllPortofolios(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("from OpProjectNode as portofolio where portofolio.Type = 1");
         return broker.list(query);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get a project by the name
    *
    * @param projectName the project name
    * @return an instance of <code>OpProjectNode</code>
    */
   public OpProjectNode getProjectByName(String projectName) {
      String locator = getProjectId(projectName);
      if (locator != null) {
         return getProjectById(locator);
      }

      return null;
   }

   /**
    * Get a project by the locator
    *
    * @param locator the uniq identifier (locator) of an entity
    * @return an instance of <code>OpProjectNode</code>
    */
   public OpProjectNode getProjectById(String locator) {
      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project = (OpProjectNode) broker.getObject(locator);
         OpTestDataFactory.initializeLazyRelationships(project);
         return project;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get a project by id
    *
    * @param id the uniq identifier of an entit
    * @return an instance of <code>OpProjectNode</code>
    */
   public OpProjectNode getProjectById(long id) {
      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project = (OpProjectNode) broker.getObject(OpProjectNode.class, id);
         OpTestDataFactory.initializeLazyRelationships(project);
         return project;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get the uniq identifier of a project by name
    *
    * @param projectName the project name
    * @return the uniq identifier (locator) of an entity
    */
   public String getProjectId(String projectName) {
      OpBroker broker = session.newBroker();
      try {
         Long projId = null;

         OpQuery query = broker.newQuery(SELECT_PROJECT_ID_BY_NAME_QUERY);
         query.setString(0, projectName);
         Iterator projectIt = broker.iterate(query);
         if (projectIt.hasNext()) {
            projId = (Long) projectIt.next();
         }

         if (projId != null) {
            return OpLocator.locatorString(OpProjectNode.PROJECT_NODE, projId);
         }
         return null;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get an assignment by the locator
    *
    * @param locator the unique identifier (locator) of an entity
    * @return an instance of <code>OpAssignment</code>
    */
   public OpAssignment getAssignmentById(String locator) {
      OpBroker broker = session.newBroker();
      try {
         OpAssignment assignment =  (OpAssignment) broker.getObject(locator);
         OpTestDataFactory.initializeLazyRelationships(assignment);
         return assignment;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get the unique identifier of an activity assignment by name
    *
    * @param activityName the assignment's activity name
    * @return the unique identifier (locator) of an entity
    */
   public String getActivityAssignmentId(String activityName) {
      OpBroker broker = session.newBroker();
      try {
         Long assignmentId = null;

         OpQuery query = broker.newQuery(SELECT_ASSIGNMENT_ID_BY_ACTIVITY_NAME_QUERY);
         query.setString(0, activityName);
         Iterator assignmentIt = broker.iterate(query);
         if (assignmentIt.hasNext()) {
            assignmentId = (Long) assignmentIt.next();
         }
         if (assignmentId != null) {
            return OpLocator.locatorString(OpAssignment.ASSIGNMENT, assignmentId);
         }
      }
      finally {
         broker.close();
      }
      return null;
   }

   /**
    * Get all the projects
    *
    * @param session
    * @return a <code>List</code> of <code>OpProjectNode</code>
    */
   public List<OpProjectNode> getAllProjects(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("from OpProjectNode as project where project.Type = 3");
         List<OpProjectNode> result = broker.list(query);
         return result;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get a project plan by the name
    *
    * @param projectPlanName the project plan name
    * @return an instance of <code>OpProjectPlan</code>
    */
   public OpProjectPlan getProjectPlanByName(String projectPlanName) {
      String locator = getProjectPlanId(projectPlanName);
      if (locator != null) {
         return getProjectPlanById(locator);
      }

      return null;
   }

   /**
    * Get a project plan by the locator
    *
    * @param locator the uniq identifier (locator) of an entity
    * @return an instance of <code>OpProjectPlan</code>
    */
   public OpProjectPlan getProjectPlanById(String locator) {
      OpBroker broker = session.newBroker();
      try {
         OpProjectPlan projectPlan = (OpProjectPlan) broker.getObject(locator);
         OpTestDataFactory.initializeLazyRelationships(projectPlan);
         return projectPlan;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get the uniq identifier of a project plan by name
    *
    * @param projectPlanName the project plan name
    * @return the uniq identifier (locator) of an entity
    */
   public String getProjectPlanId(String projectPlanName) {
      OpBroker broker = session.newBroker();
      try {
         Long projId = null;

         OpQuery query = broker.newQuery(SELECT_PROJECT_PLAN_ID_BY_NAME_QUERY);
         query.setString(0, projectPlanName);
         Iterator projectIt = broker.iterate(query);
         if (projectIt.hasNext()) {
            projId = (Long) projectIt.next();
         }
         if (projId != null) {
            return OpLocator.locatorString(OpProjectPlan.PROJECT_PLAN, projId);
         }
         return null;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get a project plan version by it's locator.
    *
    * @param locator the unique identifier (locator) of the <code>OpProjectPlanVersion</code> entity.
    * @return an instance of <code>OpProjectPlanVersion</code>.
    */
   public OpProjectPlanVersion getProjectPlanVersionById(String locator) {
      OpBroker broker = session.newBroker();
      try {
         OpProjectPlanVersion projectPlanVersion = (OpProjectPlanVersion) broker.getObject(locator);
         OpTestDataFactory.initializeLazyRelationships(projectPlanVersion);
         return projectPlanVersion;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get all the projects
    *
    * @return a <code>List</code> of <code>OpProjectNode</code>
    */
   public List getAllProjectPlans() {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("from OpProjectNode as project where project.Type = 3");
         return broker.list(query);
      }
      finally {
         broker.close();
      }
   }

   public static XMessage createPortofolioMsg(String name, String description, String parent, XComponent set) {
      if (set == null) {
         set = new XComponent(XComponent.DATA_SET);
      }
      HashMap args = new HashMap();
      args.put(OpProjectNode.TYPE, OpProjectNode.PORTFOLIO);
      args.put(OpProjectNode.NAME, name);
      args.put(OpProjectNode.DESCRIPTION, description);
      args.put("SuperPortfolioID", parent);
      args.put(OpPermissionDataSetFactory.PERMISSION_SET, set);

      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_DATA, args);
      return request;
   }

   public static XMessage updatePortofolioMsg(String id, String name, String description, XComponent set) {
      if (set == null) {
         set = new XComponent(XComponent.DATA_SET);
      }
      HashMap args = new HashMap();
      args.put(OpProjectNode.TYPE, OpProjectNode.PORTFOLIO);
      args.put(OpProjectNode.NAME, name);
      args.put(OpProjectNode.DESCRIPTION, description);
      args.put(OpPermissionDataSetFactory.PERMISSION_SET, set);

      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_ID, id);
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_DATA, args);
      return request;
   }

   public static XMessage deletePortofolioMsg(List ids) {
      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_IDS, ids);
      return request;
   }

   public static XMessage createProjectMsg(String name, Date date, double budget, String status, String portfolio) {
      if (portfolio == null) {
         portfolio = OpLocator.locatorString(OpProjectNode.PROJECT_NODE, 0);
      }
      return createProjectMsg(name, date, budget, status, portfolio, null, null, null, null);
   }

   public static XMessage createProjectMsg(String name, Date date, Date finishDate, double budget, String status, String portfolio) {
      if (date == null) {
         date = new Date(System.currentTimeMillis());
      }
      if (portfolio == null) {
         portfolio = OpLocator.locatorString(OpProjectNode.PROJECT_NODE, 0);
      }
      return createProjectMsg(name, date, finishDate, budget, status, portfolio, null, null, null, null, new XComponent(XComponent.DATA_SET));
   }

   public static XMessage createProjectMsg(String name, Date date, double budget, String status, String portfolio,
        Boolean calcMode, Boolean prgTrk, XComponent resources, Object[][] goals) {
      return createProjectMsg(name, date, null, budget, status, portfolio, calcMode, prgTrk, resources, goals, new XComponent(XComponent.DATA_SET));
   }

   public static XMessage createProjectMsg(String name, Date date, Date finishDate, double budget, String status, String portfolio,
        Boolean calcMode, Boolean prgTrk, XComponent resources, Object[][] goals, XComponent dataSet) {
      HashMap args = new HashMap();
      args.put(OpProjectNode.TYPE, OpProjectNode.PROJECT);
      args.put(OpProjectNode.NAME, name);
      args.put(OpProjectNode.START, date);
      args.put(OpProjectNode.FINISH, finishDate);
      args.put(OpProjectNode.BUDGET, budget);
      args.put(OpProjectNode.STATUS, status);
      args.put(OpProjectNode.PRIORITY, OpProjectNode.DEFAULT_PRIORITY);
      args.put(OpProjectNode.PROBABILITY, OpProjectNode.DEFAULT_PROBABILITY);
      args.put(OpProjectNode.ARCHIVED, OpProjectNode.DEFAULT_ARCHIVED);
      args.put("PortfolioID", portfolio);
      args.put(OpProjectPlan.CALCULATION_MODE, calcMode);
      args.put(OpProjectPlan.PROGRESS_TRACKED, prgTrk);
      args.put(OpPermissionDataSetFactory.PERMISSION_SET, dataSet);

      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, args);
      request.setArgument(OpProjectAdministrationService.GOALS_SET, createDataSet(goals));
      request.setArgument(OpProjectAdministrationService.RESOURCE_SET, resources);
      return request;
   }

   public static XMessage updateProjectMsg(String id, String name, Date date, double budget, String status, String portfolio,
        Boolean calcMode, Boolean prgTrk, XComponent resouces, Object[][] goals, boolean modifiedRates) {
      return updateProjectMsg(id, name, date, null, budget, status, portfolio, calcMode, prgTrk, resouces, createDataSet(goals), modifiedRates);
   }

   public static XMessage updateProjectMsg(String id, String name, Date startDate, Date finishDate, double budget, String status, String portfolio,
        Boolean calcMode, Boolean prgTrk, XComponent resources, XComponent goals, boolean modifiedRates) {
      HashMap args = new HashMap();
      args.put(OpProjectNode.TYPE, OpProjectNode.PROJECT);
      args.put(OpProjectNode.NAME, name);
      args.put(OpProjectNode.START, startDate);
      args.put(OpProjectNode.FINISH, finishDate);
      args.put(OpProjectNode.BUDGET, budget);
      args.put(OpProjectNode.STATUS, status);
      args.put(OpProjectNode.PRIORITY, OpProjectNode.DEFAULT_PRIORITY);
      args.put(OpProjectNode.PROBABILITY, OpProjectNode.DEFAULT_PROBABILITY);
      args.put(OpProjectNode.ARCHIVED, OpProjectNode.DEFAULT_ARCHIVED);
      args.put("PortfolioID", portfolio);
      args.put(OpProjectPlan.CALCULATION_MODE, calcMode);
      args.put(OpProjectPlan.PROGRESS_TRACKED, prgTrk);
      args.put(OpPermissionDataSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));

      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PROJECT_ID, id);
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, args);
      request.setArgument(OpProjectAdministrationService.GOALS_SET, goals);
      request.setArgument("resource_set", resources);
      request.setArgument("versions_set", new XComponent(XComponent.DATA_SET));
      request.setArgument("modified_rates", modifiedRates);
      return request;
   }

   public static XMessage deleteProjectMsg(List ids) {
      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PROJECT_IDS, ids);
      return request;
   }

   public static XMessage moveProjectsMsg(String id, List ids) {
      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_ID, id);
      request.setArgument(OpProjectAdministrationService.PROJECT_IDS, ids);
      return request;
   }

   /**
    * Creates a Data set from a table
    *
    * @param table a bi-dimentional array of Objects
    * @return an DATA_SET XComponent
    */
   public static XComponent createDataSet(Object[][] table) {
      return createDataSet(null, table);
   }

   /**
    * Creates a Data set from a table
    *
    * @param ids   the id of each row
    * @param table a bi-dimentional array of Objects
    * @return an DATA_SET XComponent
    */
   public static XComponent createDataSet(List ids, Object[][] table) {
      XComponent data_set = new XComponent(XComponent.DATA_SET);
      if (table != null) {
         for (int i = 0; i < table.length; i++) {
            Object[] objects = table[i];
            XComponent row = new XComponent(XComponent.DATA_ROW);
            String id = ids != null && ids.size() > i ? (String) ids.get(i) : null;
            row.setValue(id);
            data_set.addChild(row);
            for (Object o : objects) {
               XComponent cell = new XComponent(XComponent.DATA_CELL);
               row.addChild(cell);
               cell.setValue(o);
            }
         }
      }
      return data_set;
   }

   /**
    * Add the rate value to the current total for each working day in the interval calendarStart - calendarEnd.
    *
    * @param startDate    - the start of the interval.
    * @param startDate    - the end of the interval.
    * @param xCalendar    - the calendar needed for determining the working days.
    * @param currentTotal - the variabile to which the rate is added.
    * @param rateToAdd    - the rate for each day in the interval calendarStart - calendarEnd.
    * @return - a <code>double</code> value representing current total after the rate was added for each working day
    *         in the interval calendarStart - calendarEnd.
    */
   public static double addRateForDays(Date startDate, Date endDate, XCalendar xCalendar, double currentTotal, double rateToAdd) {
      Calendar calendarStartCopy = Calendar.getInstance();
      calendarStartCopy.setTime(startDate);
      while (!calendarStartCopy.getTime().after(endDate)) {
         if (xCalendar.isWorkDay(new Date(calendarStartCopy.getTimeInMillis()))) {
            currentTotal += rateToAdd * xCalendar.getWorkHoursPerDay();
         }
         calendarStartCopy.add(Calendar.DATE, 1);
      }
      return currentTotal;
   }
}
