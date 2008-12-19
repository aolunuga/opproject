/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpSubjectDataSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpUsersFormProvider implements XFormProvider {

   /*form's data fields*/
   private final static String USER_DATA_SET = "UserDataSet";
   private final static String IS_ADMIN_ROLE_DATA_FIELD = "AdminRoleDataField";

   /*form's components*/
   private final static String NEW_USER_BUTTON = "NewUser";
   private final static String NEW_GROUP_BUTTON = "NewGroup";
   private final static String INFO_BUTTON = "Info";
   private final static String DELETE_BUTTON = "Delete";
   private final static String ASSIGN_TO_GROUP_BUTTON = "AssignToGroup";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      XComponent dataSet = form.findComponent(USER_DATA_SET);

      //retrieve the subject structure
      OpSubjectDataSetFactory.retrieveSubjectHierarchy(session, dataSet, null, -1, 0, false);

      //disable buttons that require selection
      form.findComponent(INFO_BUTTON).setEnabled(false);
      form.findComponent(DELETE_BUTTON).setEnabled(false);
      form.findComponent(ASSIGN_TO_GROUP_BUTTON).setEnabled(false);

      boolean isAdminRole = session.userIsAdministrator();
      form.findComponent(NEW_USER_BUTTON).setEnabled(isAdminRole);
      form.findComponent(NEW_GROUP_BUTTON).setEnabled(isAdminRole);
      form.findComponent(IS_ADMIN_ROLE_DATA_FIELD).setBooleanValue(isAdminRole);
   }
}
