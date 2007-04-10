/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.team.modules.project.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.test.ProjectTestDataFactory;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.team.modules.project.OpProjectAdministrationAdvancedService;
import onepoint.service.XMessage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class contains helper methods for managing projects data
 *
 * @author lucian.furtos
 */
public class ProjectAdvancedTestDataFactory extends ProjectTestDataFactory {

   private final static String SELECT_TEMPLATE_ID_BY_NAME_QUERY = "select template.ID from OpProjectNode as template where template.Name = ? and template.Type = 4";

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public ProjectAdvancedTestDataFactory(OpProjectSession session) {
      super(session);
   }

   /**
    * Get a template by the name
    *
    * @param templateName the template name
    * @return an instance of <code>OpProjectNode</code>
    */
   public OpProjectNode getTemplateByName(String templateName) {
      String locator = getTemplateId(templateName);
      if (locator != null) {
         return getTemplateById(locator);
      }

      return null;
   }

   /**
    * Get a template by the locator
    *
    * @param locator the uniq identifier (locator) of an entity
    * @return an instance of <code>OpProjectNode</code>
    */
   public OpProjectNode getTemplateById(String locator) {
      OpBroker broker = session.newBroker();

      OpProjectNode template = (OpProjectNode) broker.getObject(locator);
      // just to inialize the collection
      template.getAssignments().size();
      template.getDynamicResources().size();
      template.getGoals().size();
      template.getInstanceNodes().size();
      template.getLocks().size();
      template.getPermissions().size();
      template.getSubNodes().size();
      template.getToDos().size();
      broker.close();

      return template;
   }

   /**
    * Get the uniq identifier of a template by name
    *
    * @param templateName the template name
    * @return the uniq identifier (locator) of an entity
    */
   public String getTemplateId(String templateName) {
      OpBroker broker = session.newBroker();
      Long projId = null;

      OpQuery query = broker.newQuery(SELECT_TEMPLATE_ID_BY_NAME_QUERY);
      query.setString(0, templateName);
      Iterator templateIt = broker.iterate(query);
      if (templateIt.hasNext()) {
         projId = (Long) templateIt.next();
      }

      broker.close();
      if (projId != null) {
         return OpLocator.locatorString(OpProjectNode.PROJECT_NODE, projId.longValue());
      }
      return null;
   }

   /**
    * Get all the templates
    *
    * @return a <code>List</code> of <code>OpProjectNode</code>
    */
   public List getAllTemplates() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from OpProjectNode as template where template.Type = 4");
      List result = broker.list(query);
      broker.close();

      return result;
   }

   public static XMessage createTemplateMsg(String name) {
      return createTemplateMsg(name, "Description of " + name, null, null, null);
   }

   public static XMessage createTemplateMsg(String name, String description, String portfolio, Boolean calcMode, Boolean prgTrk) {
      HashMap args = new HashMap();
      args.put(OpProjectNode.NAME, name);
      args.put(OpProjectNode.DESCRIPTION, description);
      args.put("PortfolioID", portfolio);
      args.put(OpProjectPlan.CALCULATION_MODE, calcMode);
      args.put(OpProjectPlan.PROGRESS_TRACKED, prgTrk);
      args.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));

      XMessage request = new XMessage();
      request.setArgument("template_data", args);
      return request;
   }

   public static XMessage saveAsTemplateMsg(String prjId, String name, String portfolio) {
      HashMap args = new HashMap();
      args.put(OpProjectNode.NAME, name);
      args.put("PortfolioID", portfolio);

      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PROJECT_ID, prjId);
      request.setArgument("template_data", args);
      return request;
   }

   public static XMessage updateTemplateMsg(String id, String name) {
      return updateTemplateMsg(id, name, "Description of " + name, null, null);
   }

   public static XMessage updateTemplateMsg(String id, String name, String description, Boolean calcMode, Boolean prgTrk) {
      HashMap args = new HashMap();
      args.put(OpProjectNode.NAME, name);
      args.put(OpProjectNode.DESCRIPTION, description);
      args.put(OpProjectPlan.CALCULATION_MODE, calcMode);
      args.put(OpProjectPlan.PROGRESS_TRACKED, prgTrk);
      args.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));

      XMessage request = new XMessage();
      request.setArgument(OpProjectAdministrationAdvancedService.TEMPLATE_ID, id);
      request.setArgument("template_data", args);
      return request;
   }
}
