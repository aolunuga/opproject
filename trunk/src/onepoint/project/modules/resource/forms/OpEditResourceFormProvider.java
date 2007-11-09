/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpEditResourceFormProvider implements XFormProvider {

   private final static String ASSIGNED_PROJECT_DATA_SET = "AssignedProjectDataSet";
   private final static String EDIT_MODE = "EditMode";
   private final static String RESOURCE_ID = "ResourceID";
   private final static String USER_NAME = "UserName";
   private final static String PERMISSION_SET = "PermissionSet";
   private final static String ORIGINAL_AVAILABLE = "OriginalAvailable";
   private final static String ORIGINAL_HOURLY_RATE = "OriginalHourlyRate";
   private final static String ORIGINAL_EXTERNAL_RATE = "OriginalExternalRate";
   private final static String ORIGINAL_INHERIT = "OriginalInherit";
   private final static String USER_LABEL = "ResponsibleUserLabel";
   private final static String PERMISSIONS_TAB = "PermissionsTab";
   private final static String HOURLY_RATE = "HourlyRate";
   private final static String HOURLY_RATE_LABEL = "HourlyRateLabel";
   private final static String EXTERNAL_RATE = "ExternalRate";
   private final static String EXTERNAL_RATE_LABEL = "ExternalRateLabel";
   private final static String INHERIT_POOL_RATE = "InheritPoolRate";
   private static final String PROJECT_TOOL_PANEL = "ProjectToolPanel";
   private static final String CANCEL = "Cancel";
   private final static String POOL_HOURLY_RATE_ID  = "PoolHourlyRate";
   private final static String POOL_EXTERNAL_RATE_ID  = "PoolExternalRate";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // Find user in database      
      String id_string = (String) (parameters.get(OpResourceService.RESOURCE_ID));
      Boolean editMode = (Boolean) parameters.get(OpResourceService.EDIT_MODE);

      OpResource resource = (OpResource) (broker.getObject(id_string));

      // Downgrade edit mode to view mode if no manager access
      byte accessLevel = session.effectiveAccessLevel(broker, resource.getID());
      if (editMode && (accessLevel < OpPermission.MANAGER)) {
         editMode = Boolean.FALSE;
      }

      if ((accessLevel < OpPermission.MANAGER)) {
         form.findComponent(HOURLY_RATE).setVisible(false);
         form.findComponent(HOURLY_RATE_LABEL).setVisible(false);
         form.findComponent(EXTERNAL_RATE).setVisible(false);
         form.findComponent(EXTERNAL_RATE_LABEL).setVisible(false);
         form.findComponent(INHERIT_POOL_RATE).setVisible(false);
      }

      // Fill edit-user form with user data
      // *** Use class-constants for text-field IDs?
      form.findComponent(RESOURCE_ID).setStringValue(id_string);
      form.findComponent(EDIT_MODE).setBooleanValue(editMode);
      XComponent name = form.findComponent(OpResource.NAME);
      name.setStringValue(resource.getName());
      XComponent desc = form.findComponent(OpResource.DESCRIPTION);
      desc.setStringValue(resource.getDescription());
      XComponent available = form.findComponent(OpResource.AVAILABLE);
      available.setDoubleValue(resource.getAvailable());
      XComponent hourlyRate = form.findComponent(HOURLY_RATE);
      hourlyRate.setDoubleValue(resource.getHourlyRate());
      XComponent externalRate = form.findComponent(EXTERNAL_RATE);
      externalRate.setDoubleValue(resource.getExternalRate());

      //save available and hourly rate for later confirm dialog
      XComponent poolHourlyRate = form.findComponent(POOL_HOURLY_RATE_ID);
      XComponent poolExternalRate = form.findComponent(POOL_EXTERNAL_RATE_ID);
      poolHourlyRate.setDoubleValue(resource.getPool().getHourlyRate());
      poolExternalRate.setDoubleValue(resource.getPool().getExternalRate());      
      XComponent originalAvailable = form.findComponent(ORIGINAL_AVAILABLE);
      originalAvailable.setDoubleValue(resource.getAvailable());
      XComponent originalHourlyRate = form.findComponent(ORIGINAL_HOURLY_RATE);
      originalHourlyRate.setDoubleValue(resource.getHourlyRate());
      XComponent originalExternalRate = form.findComponent(ORIGINAL_EXTERNAL_RATE);
      originalExternalRate.setDoubleValue(resource.getExternalRate());

      XComponent inherit_pool_rate = form.findComponent(INHERIT_POOL_RATE);
      inherit_pool_rate.setBooleanValue(!resource.getInheritPoolRate());
      XComponent originalInheritPoolRate = form.findComponent(ORIGINAL_INHERIT);
      originalInheritPoolRate.setBooleanValue(!resource.getInheritPoolRate());
      if (resource.getInheritPoolRate()) {
         hourlyRate.setEnabled(false);
         externalRate.setEnabled(false);
      }

      OpUser user = resource.getUser();
      if (user != null) {
         XLocalizer localizer = new XLocalizer();
         localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionDataSetFactory.USER_OBJECTS));

         XComponent userName = form.findComponent(USER_NAME);
         userName.setStringValue(XValidator.choice(user.locator(), localizer.localize(user.getDisplayName())));
      }

      XComponent assigned_project_data_set = form.findComponent(ASSIGNED_PROJECT_DATA_SET);

      // configure project assignment sort order
      OpObjectOrderCriteria projectOrderCriteria = new OpObjectOrderCriteria(OpProjectNode.PROJECT_NODE, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
      StringBuffer assignmentQuery = new StringBuffer("select assignment.ProjectNode from OpResource as resource inner join resource.ProjectNodeAssignments as assignment where assignment.Resource.ID = ?");
      assignmentQuery.append(projectOrderCriteria.toHibernateQueryString("assignment.ProjectNode"));

      OpQuery query = broker.newQuery(assignmentQuery.toString());
      query.setLong(0, resource.getID());
      Iterator i = broker.iterate(query);
      OpProjectNode project;
      XComponent data_row;
      while (i.hasNext()) {
         project = (OpProjectNode) (i.next());
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setStringValue(XValidator.choice(project.locator(), project.getName()));
         assigned_project_data_set.addChild(data_row);
      }

      if (!editMode) {
         name.setEnabled(false);
         desc.setEnabled(false);
         available.setEnabled(false);
         hourlyRate.setEnabled(false);
         externalRate.setEnabled(false);
         inherit_pool_rate.setEnabled(false);
         form.findComponent(USER_NAME).setEnabled(false);
         form.findComponent(PROJECT_TOOL_PANEL).setVisible(false);
         form.findComponent(CANCEL).setVisible(false);
         String title = session.getLocale().getResourceMap("resource.Info").getResource("InfoResource").getText();
         form.setText(title);
      }

      if (!OpEnvironmentManager.isMultiUser()) {
         form.findComponent(USER_NAME).setVisible(false);
         form.findComponent(USER_LABEL).setVisible(false);
         form.findComponent(PERMISSIONS_TAB).setHidden(true);
      }
      else {
         // Locate permission data set in form
         XComponent permissionSet = form.findComponent(PERMISSION_SET);
         OpPermissionDataSetFactory.retrievePermissionSet(session, broker, resource.getPermissions(), permissionSet, OpResourceModule.RESOURCE_ACCESS_LEVELS, session.getLocale());
         OpPermissionDataSetFactory.administratePermissionTab(form, editMode, accessLevel);
      }

      prepareTablesAdvancedFeature(form, editMode, resource);

      broker.close();
   }

   /**
    * This method allows the closed module to add advanced functionality for resource
    *
    * @param form
    * @param editMode
    * @param resource
    */
   protected void prepareTablesAdvancedFeature(XComponent form, Boolean editMode, OpResource resource) {
   }
}
