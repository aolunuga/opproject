/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectModule;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.forms.OpEditProjectFormProvider;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.team.modules.project.OpProjectAdministrationAdvancedService;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpEditTemplateFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getLogger(OpEditProjectFormProvider.class, true);

   private final static String TEMPLATE_ID = "TemplateID";
   private final static String EDIT_MODE = "EditMode";
   private final static String PERMISSION_SET = "PermissionSet";
   private final static String PROJECT_INFO = "project.Info";
   private final static String PERMISSIONS_TAB = "PermissionsTab";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Find template in database
      String id_string = (String) (parameters.get(OpProjectAdministrationAdvancedService.TEMPLATE_ID));
      Boolean edit_mode = (Boolean) parameters.get(OpProjectAdministrationService.EDIT_MODE);

      logger.debug("OpEditTemplateFormProvider.prepareForm(): " + id_string);

      OpBroker broker = session.newBroker();
      OpProjectNode project = (OpProjectNode) (broker.getObject(id_string));

      XComponent calculationModeComponent = form.findComponent(OpProjectPlan.CALCULATION_MODE);
      if (project.getPlan().getCalculationMode() == OpProjectPlan.EFFORT_BASED) {
         calculationModeComponent.setBooleanValue(true);
      }
      else {
         calculationModeComponent.setBooleanValue(false);
      }

      XComponent trackingComponent = form.findComponent(OpProjectPlan.PROGRESS_TRACKED);
      trackingComponent.setBooleanValue(project.getPlan().getProgressTracked());
      trackingComponent.setEnabled(edit_mode.booleanValue());

      // Downgrade edit mode to view mode if no manager access
      byte accessLevel = session.effectiveAccessLevel(broker, project.getID());
      if (edit_mode.booleanValue() && (accessLevel < OpPermission.MANAGER)) {
         edit_mode = Boolean.FALSE;
      }

      form.findComponent(TEMPLATE_ID).setStringValue(id_string);
      form.findComponent(EDIT_MODE).setBooleanValue(edit_mode.booleanValue());

      XComponent name = form.findComponent(OpProjectNode.NAME);
      name.setStringValue(project.getName());
      XComponent description = form.findComponent(OpProjectNode.DESCRIPTION);
      description.setStringValue(project.getDescription());

      if (!edit_mode.booleanValue()) {
         name.setEnabled(false);
         description.setEnabled(false);
         form.findComponent("PermissionToolPanel").setVisible(false);
         form.findComponent("Cancel").setVisible(false);
         calculationModeComponent.setEnabled(false);
         trackingComponent.setEnabled(false);
         String title = session.getLocale().getResourceMap(PROJECT_INFO).getResource("InfoTemplate").getText();
         form.setText(title);
      }

      if (OpInitializer.isMultiUser()) {
         // Locate permission data set in form
         XComponent permissionSet = form.findComponent(PERMISSION_SET);
         OpPermissionSetFactory.retrievePermissionSet(session, broker, project.getPermissions(), permissionSet,
              OpProjectModule.PROJECT_ACCESS_LEVELS, session.getLocale());
         OpPermissionSetFactory.administratePermissionTab(form, edit_mode.booleanValue(), accessLevel);
      }
      else {
         form.findComponent(PERMISSIONS_TAB).setHidden(true);
      }
      broker.close();
   }

}
