/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceModule;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpEditPoolFormProvider implements XFormProvider {

   private final static String POOL_ID = "PoolID";
   private final static String EDIT_MODE = "EditMode";
   private final static String PERMISSION_SET = "PermissionSet";
   private final static String ORIGINAL_HOURLY_RATE = "OriginalHourlyRate";
   private final static String ORIGINAL_EXTERNAL_RATE = "OriginalExternalRate";
   private final static String RESOURCE_OBJECTS = "resource.objects";
   private final static String PERMISSIONS_TAB = "PermissionsTab";

   public final static String SUB_TYPE_FIELD = "SubTypeField";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Locate pool in database
      String id_string = (String) (parameters.get(OpResourceService.POOL_ID));
      Boolean edit_mode = (Boolean) parameters.get(OpResourceService.EDIT_MODE);

      OpBroker broker = ((OpProjectSession) session).newBroker();
      try {
         OpResourcePool pool = (OpResourcePool) (broker.getObject(id_string));

         // Downgrade edit mode to view mode if no manager access
         byte accessLevel = session.effectiveAccessLevel(broker, pool.getID());
         form.findComponent(SUB_TYPE_FIELD).setEnabled(false);
         if (edit_mode.booleanValue() && (accessLevel < OpPermission.MANAGER)) {
            edit_mode = Boolean.FALSE;
         }

         // Fill form with data
         form.findComponent(POOL_ID).setStringValue(id_string);
         form.findComponent(EDIT_MODE).setBooleanValue(edit_mode.booleanValue());

         // Localizer is used to localize name and description of root resource pool
         XLocalizer localizer = new XLocalizer();
         localizer.setResourceMap(session.getLocale().getResourceMap(RESOURCE_OBJECTS));
         XComponent name = form.findComponent(OpResourcePool.NAME);
         name.setStringValue(localizer.localize(pool.getName()));
         XComponent desc = form.findComponent(OpResourcePool.DESCRIPTION);
         desc.setStringValue(localizer.localize(pool.getDescription()));
         if (desc.getStringValue() == null) {
            desc.setStringValue("");
         }
         // TODO: Should be of type double
         XComponent hourlyRate = form.findComponent(OpResourcePool.HOURLY_RATE);
         hourlyRate.setDoubleValue(pool.getHourlyRate());
         XComponent externalRate = form.findComponent(OpResourcePool.EXTERNAL_RATE);
         externalRate.setDoubleValue(pool.getExternalRate());
         XComponent originalHourlyRate = form.findComponent(ORIGINAL_HOURLY_RATE);
         originalHourlyRate.setDoubleValue(pool.getHourlyRate());
         XComponent originalExternalRate = form.findComponent(ORIGINAL_EXTERNAL_RATE);
         originalExternalRate.setDoubleValue(pool.getExternalRate());

         if (!edit_mode.booleanValue()) {
            name.setEnabled(false);
            desc.setEnabled(false);
            hourlyRate.setEnabled(false);
            externalRate.setEnabled(false);
            form.findComponent("Cancel").setVisible(false);
            String title = session.getLocale().getResourceMap("resource.Info").getResource("InfoPool").getText();
            form.setText(title);
         }
         else if (pool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            // Root resource pool name and description are not editable at all
            name.setEnabled(false);
            desc.setEnabled(false);
         }

         if (OpEnvironmentManager.isMultiUser()) {
            // Locate permission data set in form
            XComponent permissionSet = form.findComponent(PERMISSION_SET);
            OpPermissionDataSetFactory.retrievePermissionSet(session, broker, pool.getPermissions(), permissionSet,
                  OpResourceModule.POOL_ACCESS_LEVELS, session.getLocale());
            OpPermissionDataSetFactory.administratePermissionTab(form, edit_mode.booleanValue(), accessLevel);
         }
         else {
            form.findComponent(PERMISSIONS_TAB).setHidden(true);
         }
      }
      finally {
         broker.close();
      }
   }

}
