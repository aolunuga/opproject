/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpPermission;
import onepoint.service.server.XSession;

import java.util.*;

/**
 * Form provider for add project assignment.
 *
 * @author ovidiu.lupas
 */
public class OpAddProjectAssignmentFormProvider implements XFormProvider {
   /*logger for this class */
   private static final XLog logger = XLogFactory.getLogger(OpAddProjectAssignmentFormProvider.class, true);
   /*resource data set to fill */
   private final static String RESOURCE_DATA_SET = "ResourceDataSet";
   /* assigned resource ids param*/
   private final static String ASSIGNED_RESOURCE_IDS = "resource_ids";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      List assignedResourceLocators = (ArrayList) parameters.get(ASSIGNED_RESOURCE_IDS);
      logger.debug("Assigned resources: " + assignedResourceLocators);
      Set assignedResourceIds = new HashSet();
      if (assignedResourceLocators != null) { //we have project node assignments
         for (int i = 0; i < assignedResourceLocators.size(); i++) {
            assignedResourceIds.add(new Long(OpLocator.parseLocator((String) assignedResourceLocators.get(i)).getID()));
         }
      }
      /*find all resources except the assigned ones*/
      OpBroker broker = session.newBroker();
      List resourceIds ;

      if (!assignedResourceIds.isEmpty()) {
         OpQuery query = broker.newQuery("select resource.ID from OpResource as resource where resource.ID not in (:assignedResourceIds)");
         query.setCollection("assignedResourceIds", assignedResourceIds);
         resourceIds = broker.list(query);
      }
      else {
         //no resource assignments -> find all resources
         OpQuery query = broker.newQuery("select resource.ID from OpResource as resource ");
         resourceIds = broker.list(query);
      }

      /*check manager access to resources*/
      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpResource.RESOURCE, OpResource.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator accessibleResources = session.accessibleObjects(broker, resourceIds, OpPermission.MANAGER, order);

      /*construct the data set with the founded accesible resources entities */
      XComponent resourceDataSet = form.findComponent(RESOURCE_DATA_SET);
      XComponent dataRow;
      while (accessibleResources.hasNext()) {
         OpResource resource = (OpResource) accessibleResources.next();
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
         resourceDataSet.addChild(dataRow);
      }
      //sort resource data set
      resourceDataSet.sort();
      broker.close();
   }
}
