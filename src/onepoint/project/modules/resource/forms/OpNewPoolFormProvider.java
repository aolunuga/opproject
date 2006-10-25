/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceModule;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpNewPoolFormProvider implements XFormProvider {

   public final static String SUPER_POOL_ID = "SuperPoolID";
   public final static String SUPER_POOL_INDEX_FIELD = "SuperPoolIndexField";
   public final static String PERMISSION_SET = "PermissionSet";
   
   public final static String SUPER_POOL_INDEX = "super_pool_index";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession)s;

      String superPoolLocator = (String) parameters.get(OpResourceService.SUPER_POOL_ID);

      OpBroker broker = session.newBroker();

      OpResourcePool superPool = null;
      if (superPoolLocator != null)
         superPool = (OpResourcePool) broker.getObject(superPoolLocator);
      else
         superPool = OpResourceService.findRootPool(broker);
      
      if (superPool == null) {
         // TODO: Error -- super pool not found (although should normally not happen)
         broker.close();
         return;
      }
      
      form.findComponent(SUPER_POOL_ID).setStringValue(superPool.locator());
      Integer superPoolIndex = (Integer) parameters.get(SUPER_POOL_INDEX);
      if (superPoolIndex != null)
         form.findComponent(SUPER_POOL_INDEX_FIELD).setIntValue(superPoolIndex.intValue());

      byte superPoolAccesssLevel = session.effectiveAccessLevel(broker, superPool.getID());

      // Locate permission data set in form
      XComponent permissionSet = form.findComponent(PERMISSION_SET);

      // Retrieve permission set of super pool -- inheritance of permissions
      OpPermissionSetFactory.retrievePermissionSet(session, broker, superPool.getPermissions(), permissionSet,
            OpResourceModule.POOL_ACCESS_LEVELS, session.getLocale());
      OpPermissionSetFactory.administratePermissionTab(form, true, superPoolAccesssLevel);
      
      broker.close();

   }

}
