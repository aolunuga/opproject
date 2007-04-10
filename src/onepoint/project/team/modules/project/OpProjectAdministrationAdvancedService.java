/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.service.XError;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.*;

/**
 * Service class for the non-public project administration module.
 *
 * @author horia.chiorean
 */
public class OpProjectAdministrationAdvancedService extends OpProjectAdministrationService {

   /**
    * Parameter constants.
    */
   public final static String TEMPLATE_ID = "template_id";

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpProjectAdministrationAdvancedService.class, true);

   private final static String TEMPLATE_DATA = "template_data";
   private final static String TEMPLATE_IDS = "template_ids";

   // This represents the initial date.
   private static final String INITIAL_DATE = "0001-01-01";


   /**
    * @see onepoint.project.modules.project.OpProjectAdministrationService#applyTemplate(onepoint.persistence.OpBroker, java.util.HashMap, onepoint.project.modules.project.OpProjectNode, onepoint.project.modules.project.OpProjectPlan)
    */
   protected void applyTemplate(OpBroker broker, HashMap project_data, OpProjectNode project, OpProjectPlan projectPlan) {
      String templateNodeLocator = (String) project_data.get(OpProjectNode.TEMPLATE_NODE);
      if (templateNodeLocator != null) {
         templateNodeLocator = XValidator.choiceID(templateNodeLocator);
      }
      if ((templateNodeLocator != null) && (!templateNodeLocator.equals("null"))) {
         OpProjectNode templateNode = (OpProjectNode) broker.getObject(templateNodeLocator);
         if (templateNode != null) {
            // QUESTION: Copy also template node assignments, or does this undermine the security system?
            project.setTemplateNode(templateNode);
            broker.updateObject(project);
            copyProjectPlan(broker, templateNode.getPlan(), projectPlan);
         }
      }
   }

   public XMessage insertTemplate(OpProjectSession session, XMessage request) {
      logger.debug("OpProjectAdministrationService.insertTemplate()");
      HashMap template_data = (HashMap) (request.getArgument(TEMPLATE_DATA));

      XMessage reply = new XMessage();
      XError error;

      OpProjectNode template = new OpProjectNode();
      template.setType(OpProjectNode.TEMPLATE);
      template.setName((String) (template_data.get(OpProjectNode.NAME)));

      // check mandatory input fields
      if (template.getName() == null || template.getName().length() == 0) {
         error = session.newError(ERROR_MAP, OpProjectError.TEMPLATE_NAME_MISSING);
         reply.setError(error);
         return reply;
      }

      template.setDescription((String) (template_data.get(OpProjectNode.DESCRIPTION)));

      // Templates do not have Start or Finish dates set

      OpBroker broker = session.newBroker();

      // Check if template name is already used
      OpQuery projectNameQuery = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
      projectNameQuery.setString(0, template.getName());
      Iterator projects = broker.iterate(projectNameQuery);
      if (projects.hasNext()) {
         error = session.newError(ERROR_MAP, OpProjectError.TEMPLATE_NAME_ALREADY_USED);
         reply.setError(error);
         broker.close();
         return reply;
      }

      String portfolioLocator = (String) template_data.get("PortfolioID");
      logger.debug("PortfolioID='" + portfolioLocator + "'");
      if (portfolioLocator != null) {
         OpProjectNode portfolio = (OpProjectNode) broker.getObject(portfolioLocator);
         if (portfolio != null) {
            template.setSuperNode(portfolio);
         }
         else {
            template.setSuperNode(OpProjectAdministrationService.findRootPortfolio(broker));
         }
      }
      else {
         template.setSuperNode(OpProjectAdministrationService.findRootPortfolio(broker));
      }      

      // Check manager access for portfolio
      if (!session.checkAccessLevel(broker, template.getSuperNode().getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Insert access to portfolio denied; ID = " + template.getSuperNode().getID());
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      broker.makePersistent(template);

      // Insert project plan including settings
      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setStart(Date.valueOf(INITIAL_DATE));
      projectPlan.setFinish(Date.valueOf(INITIAL_DATE));
      projectPlan.setProjectNode(template);
      projectPlan.setTemplate(true);

      // CalculationMode
      Boolean calculationMode = (Boolean) template_data.get(OpProjectPlan.CALCULATION_MODE);
      if (calculationMode != null && !calculationMode.booleanValue()) {
         projectPlan.setCalculationMode(OpProjectPlan.INDEPENDENT);
      }
      else {
         projectPlan.setCalculationMode(OpProjectPlan.EFFORT_BASED);
      }

      Boolean progressTracked = (Boolean) template_data.get(OpProjectPlan.PROGRESS_TRACKED);
      if (progressTracked != null) {
         projectPlan.setProgressTracked(progressTracked.booleanValue());
      }
      else {
         projectPlan.setProgressTracked(true);
      }

      broker.makePersistent(projectPlan);

      XComponent permission_set = (XComponent) template_data.get(OpPermissionSetFactory.PERMISSION_SET);
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, template, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }

      t.commit();
      broker.close();

      logger.debug("/OpProjectAdministrationService.insertTemplate()");
      return reply;
   }

   public XMessage saveAsTemplate(OpProjectSession session, XMessage request) {
      XMessage reply = new XMessage();
      XError error;

      logger.debug("OpProjectAdministrationService.saveAsTemplate()");
      String projectLocator = (String) request.getArgument(PROJECT_ID);
      if (projectLocator == null) {
         error = session.newError(ERROR_MAP, OpProjectError.PROJECT_NOT_SPECIFIED);
         reply.setError(error);
         return reply;
      }

      HashMap template_data = (HashMap) request.getArgument(TEMPLATE_DATA);

      OpProjectNode template = new OpProjectNode();
      template.setType(OpProjectNode.TEMPLATE);
      template.setName((String) (template_data.get(OpProjectNode.NAME)));

      // check mandatory input fields
      if (template.getName() == null || template.getName().length() == 0) {
         error = session.newError(ERROR_MAP, OpProjectError.TEMPLATE_NAME_MISSING);
         reply.setError(error);
         return reply;
      }

      // Templates do not have Start or Finish dates set

      OpBroker broker = session.newBroker();

      // Check if template name is already used
      OpQuery projectNameQuery = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
      projectNameQuery.setString(0, template.getName());
      Iterator projects = broker.iterate(projectNameQuery);
      if (projects.hasNext()) {
         error = session.newError(ERROR_MAP, OpProjectError.TEMPLATE_NAME_ALREADY_USED);
         reply.setError(error);
         broker.close();
         return reply;
      }

      OpProjectNode project = (OpProjectNode) broker.getObject(projectLocator);
      if (project == null) {
         error = session.newError(ERROR_MAP, OpProjectError.PROJECT_NOT_FOUND);
         reply.setError(error);
         broker.close();
         return reply;
      }

      String portfolioLocator = (String) template_data.get("PortfolioID");
      logger.debug("PortfolioID='" + portfolioLocator + "'");
      if (portfolioLocator != null) {
         OpProjectNode portfolio = (OpProjectNode) broker.getObject(portfolioLocator);
         if (portfolio != null) {
            template.setSuperNode(portfolio);
         }
         else {
            template.setSuperNode(OpProjectAdministrationService.findRootPortfolio(broker));
         }
      }
      else {
         //target portfolio not selected on the ui
         logger.warn("Target portfolio not selected for template");
         error = session.newError(ERROR_MAP, OpProjectError.TARGET_PORTFOLIO_NOT_SELECTED);
         reply.setError(error);
         broker.close();
         return reply;
      }

      // Check manager access for portfolio
      if (!session.checkAccessLevel(broker, template.getSuperNode().getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Insert access to portfolio denied; ID = " + template.getSuperNode().getID());
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      broker.makePersistent(template);

      // Insert project plan including settings
      OpProjectPlan templatePlan = new OpProjectPlan();
      templatePlan.setStart(Date.valueOf(INITIAL_DATE));
      templatePlan.setFinish(Date.valueOf(INITIAL_DATE));
      templatePlan.setProjectNode(template);
      templatePlan.setTemplate(true);

      // Copy settings from project plan
      OpProjectPlan projectPlan = project.getPlan();
      if (projectPlan != null) {
         templatePlan.setCalculationMode(projectPlan.getCalculationMode());
         templatePlan.setProgressTracked(projectPlan.getProgressTracked());
      }

      broker.makePersistent(templatePlan);

      // Derive permissions from portfolio
      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      OpPermissionSetFactory.retrievePermissionSet(session, broker, template.getSuperNode().getPermissions(),
           permissionSet, OpProjectModule.TEMPLATE_ACCESS_LEVELS, session.getLocale());
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, template, permissionSet);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }

      // Copy template plan from project plan
      copyProjectPlan(broker, projectPlan, templatePlan);

      t.commit();
      broker.close();

      logger.debug("/OpProjectAdministrationService.insertTemplate()");
      return reply;
   }

   public XMessage updateTemplate(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(TEMPLATE_ID));
      logger.debug("OpProjectAdministrationService.updateTemplate(): id = " + id_string);
      HashMap template_data = (HashMap) (request.getArgument(TEMPLATE_DATA));

      XMessage reply = new XMessage();
      XError error;

      OpBroker broker = session.newBroker();

      String templateName = (String) (template_data.get(OpProjectNode.NAME));

      // Check mandatory input fields
      if (templateName == null || templateName.length() == 0) {
         error = session.newError(ERROR_MAP, OpProjectError.TEMPLATE_NAME_MISSING);
         reply.setError(error);
         broker.close();
         return reply;
      }

      OpProjectNode template = (OpProjectNode) (broker.getObject(id_string));

      if (template == null) {
         logger.warn("ERROR: Could not find object with ID " + id_string);
         broker.close();
         return null;
      }

      // Check manager access
      if (!session.checkAccessLevel(broker, template.getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Udpate access to template denied; ID = " + id_string);
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      // check if project name is already used
      OpQuery projectNameQuery = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
      projectNameQuery.setString(0, templateName);
      Iterator templates = broker.iterate(projectNameQuery);
      while (templates.hasNext()) {
         OpProjectNode other = (OpProjectNode) templates.next();
         if (other.getID() != template.getID()) {
            error = session.newError(ERROR_MAP, OpProjectError.TEMPLATE_NAME_ALREADY_USED);
            reply.setError(error);
            broker.close();
            return reply;
         }
      }

      template.setName(templateName);
      template.setDescription((String) (template_data.get(OpProjectNode.DESCRIPTION)));

      OpProjectPlan projectPlan = template.getPlan();
      Boolean calculationMode = (Boolean) template_data.get(OpProjectPlan.CALCULATION_MODE);
      if (calculationMode != null && !calculationMode.booleanValue()) {
         projectPlan.setCalculationMode(OpProjectPlan.INDEPENDENT);
      }
      else {
         projectPlan.setCalculationMode(OpProjectPlan.EFFORT_BASED);
      }

      Boolean progressTracked = (Boolean) template_data.get(OpProjectPlan.PROGRESS_TRACKED);
      if (progressTracked != null) {
         projectPlan.setProgressTracked(progressTracked.booleanValue());
      }
      else {
         projectPlan.setProgressTracked(true);
      }

      OpTransaction t = broker.newTransaction();

      broker.updateObject(projectPlan);
      broker.updateObject(template);

      XComponent permission_set = (XComponent) template_data.get(OpPermissionSetFactory.PERMISSION_SET);
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, template, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }

      t.commit();
      broker.close();

      logger.debug("/OpProjectAdministrationService.updateTemplate()");
      return null;
   }

   public XMessage deleteTemplates(OpProjectSession session, XMessage request) {
      ArrayList id_strings = (ArrayList) (request.getArgument(TEMPLATE_IDS));
      logger.debug("OpProjectAdministrationService.deleteTemplates(): template_ids = " + id_strings);

      OpBroker broker = session.newBroker();
      XMessage reply = new XMessage();

      List templateIds = new ArrayList();
      for (int i = 0; i < id_strings.size(); i++) {
         templateIds.add(new Long(OpLocator.parseLocator((String) id_strings.get(i)).getID()));
      }
      OpQuery query = broker.newQuery("select template.SuperNode.ID from OpProjectNode as template where template.ID in (:templateIds) and template.Type = (:projectType)");
      query.setCollection("templateIds", templateIds);
      query.setByte("projectType", OpProjectNode.TEMPLATE);
      List portfolioIds = broker.list(query);

      Set accessiblePortfolioIds = session.accessibleIds(broker, portfolioIds, OpPermission.MANAGER);
      if (accessiblePortfolioIds.size() == 0) {
         logger.warn("Manager access to portfolio " + portfolioIds + " denied");
         reply.setError(session.newError(ERROR_MAP, OpProjectError.MANAGER_ACCESS_DENIED));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();
      query = broker
           .newQuery("select template from OpProjectNode as template where template.ID in (:templateIds) and template.SuperNode.ID in (:accessiblePortfolioIds)");
      query.setCollection("templateIds", templateIds);
      query.setCollection("accessiblePortfolioIds", accessiblePortfolioIds);
      Iterator result = broker.iterate(query);
      while (result.hasNext()) {
         OpProjectNode projectNode = (OpProjectNode) result.next();
         clearActiveProjectNodeSelection(projectNode, session);
         broker.deleteObject(projectNode);
      }
      t.commit();

      //if (accessiblePortfolioIds.size() < portfolioIds.size())
      //TODO: Return ("informative") error if notAllAccessible

      broker.close();
      logger.debug("/OpProjectAdministrationService.deleteTemplates()");
      return null;
   }
}
