/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpUsersFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getLogger(OpUsersFormProvider.class,true);

   public final static String GROUP_DESCRIPTOR = "g";
   public final static String USER_DESCRIPTOR = "u";

   public final static int GROUP_ICON_INDEX = 0;
   public final static int USER_ICON_INDEX = 1;

   /*form's data fields*/
   private final static String USER_DATA_SET = "UserDataSet";
   private final static String IS_ADMIN_ROLE_DATA_FIELD = "AdminRoleDataField";

   /*form's components*/
   private final static String NEW_USER_BUTTON = "NewUser";
   private final static String NEW_GROUP_BUTTON = "NewGroup";
   private final static String INFO_BUTTON = "Info";
   private final static String EDIT_BUTTON = "Edit";
   private final static String DELETE_BUTTON = "Delete";
   private final static String ASSIGN_TO_GROUP_BUTTON = "AssignToGroup";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      XComponent dataSet = form.findComponent(USER_DATA_SET);

      // Attention: We are using display name here in order to localize name of group "Everyone"
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));
      addSubGroupRows(broker, dataSet, localizer, -1, 0);
      addUserRows(broker, dataSet, localizer, -1, 0);

      //disable buttons that require selection
      form.findComponent(INFO_BUTTON).setEnabled(false);
      form.findComponent(EDIT_BUTTON).setEnabled(false);
      form.findComponent(DELETE_BUTTON).setEnabled(false);
      form.findComponent(ASSIGN_TO_GROUP_BUTTON).setEnabled(false);

      boolean isAdminRole = session.userIsAdministrator();
      form.findComponent(NEW_USER_BUTTON).setEnabled(isAdminRole);
      form.findComponent(NEW_GROUP_BUTTON).setEnabled(isAdminRole);
      form.findComponent(IS_ADMIN_ROLE_DATA_FIELD).setBooleanValue(isAdminRole);

      broker.close();
   }

   private void addSubGroupRows(OpBroker broker, XComponent dataSet, XLocalizer localizer, long groupId, int outlineLevel) {
      // configure group sort order
      OpObjectOrderCriteria groupOrderCriteria = new OpObjectOrderCriteria(OpGroup.GROUP, OpGroup.NAME, OpObjectOrderCriteria.ASCENDING);
      OpQuery query = null;
      if (groupId == -1) {
         query = broker.newQuery("select group from OpGroup as group" + groupOrderCriteria.toHibernateQueryString("group"));
      }
      else {
         String queryString = "select subGroup from OpGroupAssignment as assignment inner join assignment.SubGroup as subGroup where assignment.SuperGroup.ID = ?";
         queryString += groupOrderCriteria.toHibernateQueryString("subGroup");
         query = broker.newQuery(queryString);
         query.setLong(0, groupId);
      }

      Iterator subGroups = broker.iterate(query);
      OpGroup subGroup = null;
      XComponent dataRow = null;
      XComponent dataCell = null;
      while (subGroups.hasNext()) {
         subGroup = (OpGroup) subGroups.next();
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setOutlineLevel(outlineLevel);
         dataRow.setStringValue(subGroup.locator());
         dataSet.addChild(dataRow);
         // Data row w/index '0' is indicator row ('u' or 'g' for user/group)
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(GROUP_DESCRIPTOR);
         dataRow.addChild(dataCell);
         // Display name and icon
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(XValidator.choice(subGroup.locator(), localizer.localize(subGroup.getDisplayName()), GROUP_ICON_INDEX));
         dataRow.addChild(dataCell);
         // Description
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(localizer.localize(subGroup.getDescription()));
         dataRow.addChild(dataCell);
         // Recursively add sub-groups and users
         addSubGroupRows(broker, dataSet, localizer, subGroup.getID(), outlineLevel + 1);
         addUserRows(broker, dataSet, localizer, subGroup.getID(), outlineLevel + 1);
      }

   }

   private void addUserRows(OpBroker broker, XComponent dataSet, XLocalizer localizer, long groupId, int outlineLevel) {
      // configure user sort order
      OpObjectOrderCriteria userOrderCriteria = new OpObjectOrderCriteria(OpUser.USER, OpUser.NAME, OpObjectOrderCriteria.ASCENDING);
      OpQuery query = null;
      if (groupId == -1) {
         query = broker.newQuery("select user from OpUser as user" + userOrderCriteria.toHibernateQueryString("user"));
      }
      else {
         StringBuffer queryBuffer = new StringBuffer("select user from OpUserAssignment as assignment inner join assignment.User as user where assignment.Group.ID = ?");
         queryBuffer.append(userOrderCriteria.toHibernateQueryString("user"));
         query = broker.newQuery(queryBuffer.toString());
         query.setLong(0, groupId);
      }

      Iterator users = broker.iterate(query);
      OpUser user = null;
      XComponent dataRow = null;
      XComponent dataCell = null;
      while (users.hasNext()) {
         user = (OpUser) users.next();
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setOutlineLevel(outlineLevel);
         dataRow.setStringValue(user.locator());
         dataSet.addChild(dataRow);
         // Data row w/index '0' is indicator row ('u' or 'g' for user/group)
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(USER_DESCRIPTOR);
         dataRow.addChild(dataCell);
         // Display name and icon
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(XValidator.choice(user.locator(), localizer.localize(user.getDisplayName()), USER_ICON_INDEX));
         dataRow.addChild(dataCell);
         // Description
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(localizer.localize(user.getDescription()));
         dataRow.addChild(dataCell);
      }

   }

}
