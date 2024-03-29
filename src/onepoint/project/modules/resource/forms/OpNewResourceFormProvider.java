/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import java.util.HashMap;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceModule;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

public class OpNewResourceFormProvider implements XFormProvider {

   private final static String POOL_ID = "PoolID";
   private final static String POOL_INDEX_FIELD = "PoolIndexField";
   private final static String HOURLY_RATE = "HourlyRate";
   private final static String EXTERNAL_RATE = "ExternalRate";
   private final static String PERMISSION_SET = "PermissionSet";
   private final static String POOL_INDEX = "pool_index";
   private final static String USER_NAME = "UserName";
   private final static String USER_LABEL = "ResponsibleUserLabel";
   private final static String PERMISSIONS_TAB = "PermissionsTab";
   private final static String POOL_HOURLY_RATE_ID  = "PoolHourlyRate";
   private final static String POOL_EXTERNAL_RATE_ID  = "PoolExternalRate";

   public final static String SUB_TYPE_FIELD = "SubTypeField";
   public final static String SUB_TYPE_LABEL = "SubTypeLabel";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      String poolLocator = (String) parameters.get(OpResourceService.POOL_ID);

      OpBroker broker = session.newBroker();
      try {
         OpResourcePool pool = null;
         if (poolLocator != null) {
            pool = (OpResourcePool) broker.getObject(poolLocator);
         }
         else {
            pool = OpResourceService.findRootPool(broker);
         }

         if (pool == null) {
            // TODO: Error -- pool not found (although should normally not happen)
            return;
         }

         form.findComponent(POOL_ID).setStringValue(pool.locator());
         Integer poolIndex = (Integer) parameters.get(POOL_INDEX);
         if (poolIndex != null) {
            form.findComponent(POOL_INDEX_FIELD).setIntValue(poolIndex.intValue());
         }

         // Initialize hourly rate to pool rate and set override to false per default
         XComponent poolHourlyRate = form.findComponent(POOL_HOURLY_RATE_ID);
         XComponent poolExternalRate = form.findComponent(POOL_EXTERNAL_RATE_ID);
         poolHourlyRate.setDoubleValue(pool.getHourlyRate());
         poolExternalRate.setDoubleValue(pool.getExternalRate());
         XComponent hourlyRateField = form.findComponent(HOURLY_RATE);
         hourlyRateField.setDoubleValue(pool.getHourlyRate());
         XComponent externalRateField = form.findComponent(EXTERNAL_RATE);
         externalRateField.setDoubleValue(pool.getExternalRate());
         hourlyRateField.setEnabled(false);
         externalRateField.setEnabled(false);

         byte poolAccesssLevel = session.effectiveAccessLevel(broker, pool.getId());
         form.findComponent(SUB_TYPE_FIELD).setEnabled(false);

         if (OpEnvironmentManager.isMultiUser()) {
            // Locate permission data set in form
            XComponent permissionSet = form.findComponent(PERMISSION_SET);
            // Retrieve permission set of pool -- inheritance of permissions
            OpPermissionDataSetFactory.retrievePermissionSet(session, broker, pool.getPermissions(), permissionSet,
                  OpResourceModule.RESOURCE_ACCESS_LEVELS, session.getLocale());
            OpPermissionDataSetFactory.administratePermissionTab(form, true, poolAccesssLevel);
            
            OpUser user = session.user(broker);
            XLocalizer localizer = new XLocalizer();
            localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionDataSetFactory.USER_OBJECTS));
            form.findComponent(USER_NAME).setStringValue(XValidator.choice(user.locator(), localizer.localize(user.getDisplayName())));
         }
         else {
            form.findComponent(PERMISSIONS_TAB).setHidden(true);
            form.findComponent(USER_LABEL).setVisible(false);
            form.findComponent(USER_NAME).setVisible(false);
         }
         
         prepareAdvancedFeatures(session, broker, form, pool);
      }
      finally {
         broker.close();
      }
   }
   
   protected void prepareAdvancedFeatures(OpProjectSession session, OpBroker broker, XComponent form, OpResourcePool parent) {
   }
}
