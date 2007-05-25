/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.test.TestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class contains helper methods for managing projects data
 *
 * @author lucian.furtos
 */
public class ProjectTestDataFactory extends TestDataFactory {

   private final static String SELECT_PORTOFOLIO_ID_BY_NAME_QUERY = "select portofolio.ID from OpProjectNode as portofolio where portofolio.Name = ? and portofolio.Type = 1";
   private final static String SELECT_PROJECT_ID_BY_NAME_QUERY = "select project.ID from OpProjectNode as project where project.Name = ? and project.Type = 3";
   private final static String SELECT_PROJECT_PLAN_ID_BY_NAME_QUERY = "select plan.ID from OpProjectPlan as plan where plan.Name = ?";

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public ProjectTestDataFactory(OpProjectSession session) {
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
      Long projId = null;

      OpQuery query = broker.newQuery(SELECT_PORTOFOLIO_ID_BY_NAME_QUERY);
      query.setString(0, portofolioName);
      Iterator projectIt = broker.iterate(query);
      if (projectIt.hasNext()) {
         projId = (Long) projectIt.next();
      }

      broker.close();
      if (projId != null) {
         return OpLocator.locatorString(OpProjectNode.PROJECT_NODE, projId.longValue());
      }
      return null;
   }

   /**
    * Get all the portofolios
    *
    * @return a <code>List</code> of <code>OpProjectNode</code>
    */
   public List getAllPortofolios() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from OpProjectNode as portofolio where portofolio.Type = 1");
      List result = broker.list(query);
      broker.close();

      return result;
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

      OpProjectNode project = (OpProjectNode) broker.getObject(locator);
      // just to inialize the collection
      project.getAssignments().size();
      project.getDynamicResources().size();
      project.getGoals().size();
      project.getInstanceNodes().size();
      project.getLocks().size();
      project.getPermissions().size();
      project.getSubNodes().size();
      project.getToDos().size();
      broker.close();

      return project;
   }

   /**
    * Get the uniq identifier of a project by name
    *
    * @param projectName the project name
    * @return the uniq identifier (locator) of an entity
    */
   public String getProjectId(String projectName) {
      OpBroker broker = session.newBroker();
      Long projId = null;

      OpQuery query = broker.newQuery(SELECT_PROJECT_ID_BY_NAME_QUERY);
      query.setString(0, projectName);
      Iterator projectIt = broker.iterate(query);
      if (projectIt.hasNext()) {
         projId = (Long) projectIt.next();
      }

      broker.close();
      if (projId != null) {
         return OpLocator.locatorString(OpProjectNode.PROJECT_NODE, projId.longValue());
      }
      return null;
   }

   /**
    * Get all the projects
    *
    * @return a <code>List</code> of <code>OpProjectNode</code>
    */
   public List getAllProjects() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from OpProjectNode as project where project.Type = 3");
      List result = broker.list(query);
      broker.close();

      return result;
   }

   /**
    * Get a project plan by the name
    *
    * @param projectPlanName the project plan name
    * @return an instance of <code>OpProjectPlan</code>
    */
   public OpProjectPlan getProjectPlanByName(String projectPlanName) {
      String locator = getProjectId(projectPlanName);
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

      OpProjectPlan plan = (OpProjectPlan) broker.getObject(locator);
      // just to inialize the collection
      plan.getActivities().size();
      plan.getActivityAssignments().size();
      plan.getActivityAttachments().size();
      plan.getDependencies().size();
      plan.getDynamicResources().size();
      plan.getLocks().size();
      plan.getPermissions().size();
      plan.getVersions().size();
      plan.getWorkPeriods().size();
      broker.close();

      return plan;
   }

   /**
    * Get the uniq identifier of a project plan by name
    *
    * @param projectPlanName the project plan name
    * @return the uniq identifier (locator) of an entity
    */
   public String getProjectPlanId(String projectPlanName) {
      OpBroker broker = session.newBroker();
      Long projId = null;

      OpQuery query = broker.newQuery(SELECT_PROJECT_PLAN_ID_BY_NAME_QUERY);
      query.setString(0, projectPlanName);
      Iterator projectIt = broker.iterate(query);
      if (projectIt.hasNext()) {
         projId = (Long) projectIt.next();
      }

      broker.close();
      if (projId != null) {
         return OpLocator.locatorString(OpProjectPlan.PROJECT_PLAN, projId.longValue());
      }
      return null;
   }

   /**
    * Get all the projects
    *
    * @return a <code>List</code> of <code>OpProjectNode</code>
    */
   public List getAllProjectPlans() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from OpProjectNode as project where project.Type = 3");
      List result = broker.list(query);
      broker.close();

      return result;
   }

   public static XMessage createPortofolioMsg(String name, String description, String parent, XComponent set) {
      if (set == null) {
         set = new XComponent(XComponent.DATA_SET);
      }
      HashMap args = new HashMap();
      args.put(OpProjectNode.NAME, name);
      args.put(OpProjectNode.DESCRIPTION, description);
      args.put("SuperPortfolioID", parent);
      args.put(OpPermissionSetFactory.PERMISSION_SET, set);

      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PORTFOLIO_DATA, args);
      return request;
   }

   public static XMessage updatePortofolioMsg(String id, String name, String description, XComponent set) {
      if (set == null) {
         set = new XComponent(XComponent.DATA_SET);
      }
      HashMap args = new HashMap();
      args.put(OpProjectNode.NAME, name);
      args.put(OpProjectNode.DESCRIPTION, description);
      args.put(OpPermissionSetFactory.PERMISSION_SET, set);

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
      return createProjectMsg(name, date, budget, status, portfolio, null, null, null, null, null);
   }

   public static XMessage createProjectMsg(String name, Date date, Date finishDate, double budget, String status, String portfolio) {
      if (date == null) {
         date = new Date(System.currentTimeMillis());
      }
      if (portfolio == null) {
         portfolio = OpLocator.locatorString(OpProjectNode.PROJECT_NODE, 0);
      }
      return createProjectMsg(name, date, finishDate, budget, status, portfolio, null, null, null, null, null, new XComponent(XComponent.DATA_SET));
   }

   public static XMessage createProjectMsg(String name, Date date, double budget, String status, String portfolio,
        Boolean calcMode, Boolean prgTrk, ArrayList resouces, Object[][] goals, Object[][] todos) {
      return createProjectMsg(name, date, null, budget, status, portfolio, calcMode, prgTrk, resouces, goals, todos, new XComponent(XComponent.DATA_SET));
   }

   public static XMessage createProjectMsg(String name, Date date, Date finishDate, double budget, String status, String portfolio,
        Boolean calcMode, Boolean prgTrk, ArrayList resouces, Object[][] goals, Object[][] todos, XComponent dataSet) {
      HashMap args = new HashMap();
      args.put(OpProjectNode.NAME, name);
      args.put(OpProjectNode.START, date);
      args.put(OpProjectNode.FINISH, finishDate);
      args.put(OpProjectNode.BUDGET, new Double(budget));
      args.put(OpProjectNode.STATUS, status);
      args.put("PortfolioID", portfolio);
      args.put(OpProjectPlan.CALCULATION_MODE, calcMode);
      args.put(OpProjectPlan.PROGRESS_TRACKED, prgTrk);
      args.put(OpPermissionSetFactory.PERMISSION_SET, dataSet);

      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, args);
      request.setArgument(OpProjectAdministrationService.GOALS_SET, createDataSet(goals));
      request.setArgument(OpProjectAdministrationService.TO_DOS_SET, createDataSet(todos));
      request.setArgument("resource_list", resouces);
      return request;
   }

   public static XMessage updateProjectMsg(String id, String name, Date date, double budget, String status, String portfolio,
        Boolean calcMode, Boolean prgTrk, ArrayList resouces, Object[][] goals, Object[][] todos) {
      return updateProjectMsg(id, name, date, null, budget, status, portfolio, calcMode, prgTrk, resouces, createDataSet(goals), createDataSet(todos));
   }

   public static XMessage updateProjectMsg(String id, String name, Date startDate, Date finishDate, double budget, String status, String portfolio,
        Boolean calcMode, Boolean prgTrk, ArrayList resouces, XComponent goals, XComponent todos) {
      HashMap args = new HashMap();
      args.put(OpProjectNode.NAME, name);
      args.put(OpProjectNode.START, startDate);
      args.put(OpProjectNode.FINISH, finishDate);
      args.put(OpProjectNode.BUDGET, new Double(budget));
      args.put(OpProjectNode.STATUS, status);
      args.put("PortfolioID", portfolio);
      args.put(OpProjectPlan.CALCULATION_MODE, calcMode);
      args.put(OpProjectPlan.PROGRESS_TRACKED, prgTrk);
      args.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));

      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PROJECT_ID, id);
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, args);
      request.setArgument(OpProjectAdministrationService.GOALS_SET, goals);
      request.setArgument(OpProjectAdministrationService.TO_DOS_SET, todos);
      request.setArgument("versions_set", new XComponent(XComponent.DATA_SET));
      request.setArgument("resource_list", resouces);
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
            for (int j = 0; j < objects.length; j++) {
               Object o = objects[j];
               XComponent cell = new XComponent(XComponent.DATA_CELL);
               row.addChild(cell);
               cell.setValue(o);
            }
         }
      }
      return data_set;
   }

}
