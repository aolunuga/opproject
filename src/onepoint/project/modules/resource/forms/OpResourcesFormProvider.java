/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpPermission;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpResourcesFormProvider implements XFormProvider {

   /**
    * Form component ids.
    */
   public final static String RESOURCES_FORM = "ResourcesForm";
   public final static String RESOURCE_DATA_SET = "ResourceDataSet";

   private static final String NEW_POOL_BUTTON = "NewPoolButton";
   private static final String NEW_RESOURCE_BUTTON = "NewResourceButton";
   private static final String EDIT_BUTTON = "EditButton";
   private static final String PROPERTIES_BUTTON = "PropertiesButton";
   private static final String MOVE_BUTTON = "MoveButton";
   private static final String DELETE_BUTTON = "DeleteButton";
   private static final String ASSIGN_TO_PROJECT_BUTTON = "AssignToProjectButton";
   private static final String IMPORT_USER_BUTTON = "ImportUserButton";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // Fill resource data set with pool and resource data
      XComponent dataSet = form.findComponent(RESOURCE_DATA_SET);

      //set the manager permissions
      form.findComponent("ManagerPermission").setByteValue(OpPermission.MANAGER);

      //set the effective permissions of the root resource pool
      OpResourcePool rootResourcePool = OpResourceService.findRootPool(broker);
      byte rootPoolPermission = session.effectiveAccessLevel(broker, rootResourcePool.getID());
      form.findComponent("RootPoolPermission").setByteValue(rootPoolPermission);

      //disable the selection buttons
      disableSelectionButtons(form);

      //check button default button visibility
      if (rootPoolPermission < OpPermission.MANAGER) {
         form.findComponent(NEW_POOL_BUTTON).setEnabled(false);
         form.findComponent(NEW_RESOURCE_BUTTON).setEnabled(false);
         form.findComponent(IMPORT_USER_BUTTON).setEnabled(false);
      }

      int[] columnsSelector = new int[4];
      columnsSelector[0] = OpResourceDataSetFactory.DESCRIPTOR;
      columnsSelector[1] = OpResourceDataSetFactory.NAME;
      columnsSelector[2] = OpResourceDataSetFactory.DESCRIPTION;
      columnsSelector[3] = OpResourceDataSetFactory.EFFECTIVE_PERMISSIONS;

      OpResourceDataSetFactory.retrieveResourceDataSet(session, broker, dataSet, columnsSelector, columnsSelector);

      broker.close();
   }

   /**
    * Disables buttons that require a selection in order to be enabled.
    * @param form a <code>XComponent</code> representing the project form.
    */
   private void disableSelectionButtons(XComponent form) {
      form.findComponent(EDIT_BUTTON).setEnabled(false);
      form.findComponent(MOVE_BUTTON).setEnabled(false);
      form.findComponent(DELETE_BUTTON).setEnabled(false);
      form.findComponent(ASSIGN_TO_PROJECT_BUTTON).setEnabled(false);
      form.findComponent(PROPERTIES_BUTTON).setEnabled(false);
   }
}
