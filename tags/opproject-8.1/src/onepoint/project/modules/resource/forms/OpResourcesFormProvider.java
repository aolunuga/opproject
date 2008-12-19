/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import java.util.HashMap;
import java.util.Map;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.service.server.XSession;

public class OpResourcesFormProvider implements XFormProvider {

   /**
    * Form component ids.
    */
   public final static String RESOURCES_FORM = "ResourcesForm";
   public final static String RESOURCE_DATA_SET = "ResourceDataSet";

   private static final String NEW_POOL_BUTTON = "NewPoolButton";
   private static final String NEW_RESOURCE_BUTTON = "NewResourceButton";
   private static final String PROPERTIES_BUTTON = "PropertiesButton";
   private static final String MOVE_BUTTON = "MoveButton";
   private static final String DELETE_BUTTON = "DeleteButton";
   private static final String ASSIGN_TO_PROJECT_BUTTON = "AssignToProjectButton";
   private static final String IMPORT_USER_BUTTON = "ImportUserButton";
   private final static String POOL_SELECTOR = "poolColumnsSelector";
   private final static String RESOURCE_SELECTOR = "resourceColumnsSelector";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      try {
         // Fill resource data set with pool and resource data
         XComponent dataSet = form.findComponent(RESOURCE_DATA_SET);

         //set the manager permissions
         form.findComponent("ManagerPermission").setByteValue(OpPermission.MANAGER);

         //set the effective permissions of the root resource pool
         OpResourcePool rootResourcePool = OpResourceService.findRootPool(broker);
         byte rootPoolPermission = session.effectiveAccessLevel(broker, rootResourcePool.getId());
         form.findComponent("RootPoolPermission").setByteValue(rootPoolPermission);

         //disable the selection buttons
         disableSelectionButtons(form);

         //check button default button visibility
         if (rootPoolPermission < OpPermission.MANAGER) {
            form.findComponent(NEW_POOL_BUTTON).setEnabled(false);
            form.findComponent(NEW_RESOURCE_BUTTON).setEnabled(false);
            if (OpEnvironmentManager.isMultiUser()) {
               form.findComponent(IMPORT_USER_BUTTON).setEnabled(false);
            }
         }

         Map<Integer, Integer> columnsSelector = new HashMap<Integer, Integer>();
         columnsSelector.put(0, OpResourceDataSetFactory.DESCRIPTOR);
         columnsSelector.put(1, OpResourceDataSetFactory.NAME);
         columnsSelector.put(2, OpResourceDataSetFactory.DESCRIPTION);
         columnsSelector.put(3, OpResourceDataSetFactory.EFFECTIVE_PERMISSIONS);

         form.findComponent(POOL_SELECTOR).setValue(columnsSelector);
         form.findComponent(RESOURCE_SELECTOR).setValue(columnsSelector);

         if (!OpEnvironmentManager.isMultiUser()) {
            form.findComponent(IMPORT_USER_BUTTON).setVisible(false);
         }

         OpResourceDataSetFactory.retrieveFirstLevelsResourceDataSet(session, dataSet, columnsSelector, columnsSelector, null, null, true);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Disables buttons that require a selection in order to be enabled.
    *
    * @param form a <code>XComponent</code> representing the project form.
    */
   private void disableSelectionButtons(XComponent form) {
      form.findComponent(MOVE_BUTTON).setEnabled(false);
      form.findComponent(DELETE_BUTTON).setEnabled(false);
      form.findComponent(ASSIGN_TO_PROJECT_BUTTON).setEnabled(false);
      form.findComponent(PROPERTIES_BUTTON).setEnabled(false);
   }
}
