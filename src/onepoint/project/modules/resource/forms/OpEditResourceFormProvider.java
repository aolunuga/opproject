/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceModule;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpEditResourceFormProvider implements XFormProvider {

   public final static String ASSIGNED_PROJECT_DATA_SET = "AssignedProjectDataSet";
   public final static String EDIT_MODE = "EditMode";
   public final static String RESOURCE_ID = "ResourceID";
   public final static String USER_NAME = "UserName";
   public final static String PERMISSION_SET = "PermissionSet";
   public final static String ORIGINAL_AVAILABLE = "OriginalAvailable";
   public final static String ORIGINAL_HOURLY_RATE = "OriginalHourlyRate";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // Find user in database      
      String id_string = (String) (parameters.get(OpResourceService.RESOURCE_ID));
      Boolean edit_mode = (Boolean) parameters.get(OpResourceService.EDIT_MODE);

      OpResource resource = (OpResource) (broker.getObject(id_string));

      // Downgrade edit mode to view mode if no manager access
      byte accessLevel = session.effectiveAccessLevel(broker, resource.getID());
      if (edit_mode.booleanValue() && (accessLevel < OpPermission.MANAGER)) {
         edit_mode = Boolean.FALSE;
      }

      // Fill edit-user form with user data
      // *** Use class-constants for text-field IDs?
      form.findComponent(RESOURCE_ID).setStringValue(id_string);
      form.findComponent(EDIT_MODE).setBooleanValue(edit_mode.booleanValue());
      XComponent name = form.findComponent(OpResource.NAME);
      name.setStringValue(resource.getName());
      XComponent desc = form.findComponent(OpResource.DESCRIPTION);
      desc.setStringValue(resource.getDescription());
      XComponent available = form.findComponent(OpResource.AVAILABLE);
      available.setDoubleValue(resource.getAvailable());
      XComponent hourly_rate = form.findComponent(OpResource.HOURLY_RATE);
      hourly_rate.setDoubleValue(resource.getHourlyRate());

      //save available and hourly rate for later confirm dialog
      XComponent originalAvailable = form.findComponent(ORIGINAL_AVAILABLE);
      originalAvailable.setDoubleValue(resource.getAvailable());
      XComponent originalHourly_rate = form.findComponent(ORIGINAL_HOURLY_RATE);
      originalHourly_rate.setDoubleValue(resource.getHourlyRate());

      XComponent inherit_pool_rate = form.findComponent(OpResource.INHERIT_POOL_RATE);
      inherit_pool_rate.setBooleanValue(resource.getInheritPoolRate());
      if (resource.getInheritPoolRate()) {
         hourly_rate.setEnabled(false);
      }

      OpUser user = resource.getUser();
      if (user != null) {
         // *** TODO: Use real choice-field (opens associated chooser dialog)
         // *** Meanwhile: Use a work-around
         // ==> Disabled text-field showing user name
         // ==> Data-field containing user ID
         // ==> Normal button launching chooser
         XLocalizer localizer = new XLocalizer();
         localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));

         XComponent user_name_text_field = form.findComponent(USER_NAME);
         user_name_text_field.setStringValue(localizer.localize(user.getDisplayName()));
         XComponent user_id_data_field = form.findComponent("SelectedUserDataField");
         user_id_data_field.setStringValue(user.locator());
      }

      XComponent assigned_project_data_set = form.findComponent(ASSIGNED_PROJECT_DATA_SET);
      // configure project assignment sort order
      OpObjectOrderCriteria projectOrderCriteria = new OpObjectOrderCriteria(OpProjectNode.PROJECT_NODE, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
      StringBuffer assignmentQuery = new StringBuffer("select assignment.ProjectNode from OpResource as resource inner join resource.ProjectNodeAssignments as assignment where assignment.Resource.ID = ?");
      assignmentQuery.append(projectOrderCriteria.toHibernateQueryString("assignment.ProjectNode"));

      OpQuery query = broker.newQuery(assignmentQuery.toString());
      query.setLong(0, resource.getID());
      Iterator i = broker.iterate(query);
      OpProjectNode project = null;
      XComponent data_row = null;
      while (i.hasNext()) {
         project = (OpProjectNode) (i.next());
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setStringValue(XValidator.choice(project.locator(), project.getName()));
         assigned_project_data_set.addChild(data_row);
      }

      if (!edit_mode.booleanValue()) {
         name.setEnabled(false);
         desc.setEnabled(false);
         available.setEnabled(false);
         hourly_rate.setEnabled(false);
         inherit_pool_rate.setEnabled(false);
         form.findComponent(USER_NAME).setEnabled(false);
         form.findComponent("ProjectToolPanel").setVisible(false);
         form.findComponent("SelectUserButton").setVisible(false);
         form.findComponent("Cancel").setVisible(false);
         String title = session.getLocale().getResourceMap("resource.Info").getResource("InfoResource").getText();
         form.setText(title);
      }

      // Locate permission data set in form
      XComponent permissionSet = form.findComponent(PERMISSION_SET);

      OpPermissionSetFactory.retrievePermissionSet(session, broker, resource.getPermissions(), permissionSet, OpResourceModule.RESOURCE_ACCESS_LEVELS, session.getLocale());
      OpPermissionSetFactory.administratePermissionTab(form, edit_mode.booleanValue(), accessLevel);

      broker.close();
   }

}
