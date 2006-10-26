/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.*;

public class OpSubjectChooserFormProvider implements XFormProvider {

   private final static int GROUP_ICON_INDEX = 0;
   private final static int USER_ICON_INDEX = 1;

   // Form parameters and component IDs
   private final static String CALLING_FRAME_ID = "CallingFrameID";
   private final static String ACTION_HANDLER = "ActionHandler";
   private final static String SUBJECT_SET = "SubjectSet";
   private final static String SHOW_USERS = "ShowUsers";
   private final static String SHOW_GROUPS = "ShowGroups";
   private final static String EXCLUDED = "Excluded";
   private final static String MULTIPLE_SELECTION = "MultipleSelection";
   private final static String LIST_BOX_ID = "SubjectList";

   private final static String USER_ASSIGNMENT_QUERY = "select distinct ua.Group.ID from OpUserAssignment as ua where ua.User.ID in (:userIds)";
   private final static String GROUP_ASSIGNMENT_QUERY = "select distinct ga.SuperGroup.ID from OpGroupAssignment as ga where ga.SubGroup.ID in (:groupIds)";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Set calling frame and field IDs from parameters
      String callingFrameID = (String) parameters.get(CALLING_FRAME_ID);
      form.findComponent(CALLING_FRAME_ID).setStringValue(callingFrameID);
      String actionHandler = (String) parameters.get(ACTION_HANDLER);
      form.findComponent(ACTION_HANDLER).setStringValue(actionHandler);
      Boolean multipleSelection = (Boolean) parameters.get(MULTIPLE_SELECTION);
      if (!multipleSelection.booleanValue()) {
         form.findComponent(LIST_BOX_ID).setSelectionModel(XComponent.SINGLE_ROW_SELECTION);
      }
      form.findComponent(MULTIPLE_SELECTION).setBooleanValue(multipleSelection.booleanValue());
            
      boolean showUsers = ((Boolean) parameters.get(SHOW_USERS)).booleanValue();
      boolean showGroups = ((Boolean) parameters.get(SHOW_GROUPS)).booleanValue();
      List excluded = ((List) parameters.get(EXCLUDED));

      // Put all subject names into project data-set (values are IDs)
      XComponent data_set = form.findComponent(SUBJECT_SET);
      OpBroker broker = session.newBroker();

      Collection excludedGroups = new HashSet();
      if (excluded != null) {
         excludedGroups = getAlreadyAssignedGroups(excluded, broker);
      }

      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));
      if (showGroups) {
         addSubGroupRows(broker, data_set, localizer, -1, 0, showUsers, excludedGroups);
      }
      if (showUsers) {
         addUserRows(broker, data_set, localizer, -1, 0);
      }

      XComponent subject_row;
      //collapse all collections
      for (int i = 0; i < data_set.getChildCount(); i++) {
         subject_row = (XComponent) data_set.getChild(i);
         if (subject_row.getOutlineLevel() > 0) {
            subject_row.setVisible(false);
         }
      }
      broker.close();
   }

   private HashSet getAlreadyAssignedGroups(List subjectLocators, OpBroker broker) {
      int i;
      XComponent dataRow;
      OpLocator locator = null;
      HashSet notAssignableGroupIds = new HashSet();

      HashSet groupIds = new HashSet();
      HashSet userIds = new HashSet();

      for (i = 0; i < subjectLocators.size(); i++) {
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue((String) (subjectLocators.get(i)));
         locator = OpLocator.parseLocator(dataRow.getStringValue());
         Long locatorId = new Long(locator.getID());
         if (locator.getPrototype().getInstanceClass() == OpGroup.class){
            //can not assign a group to itself
            notAssignableGroupIds.add(locatorId);
            groupIds.add(locatorId);
         }
         if (locator.getPrototype().getInstanceClass() == OpUser.class){
            userIds.add(locatorId);
         }
      }
      //add group's already assigned supergroups to notAssignableGroupIds
      if (groupIds.size() > 0){
         OpQuery query = broker.newQuery(GROUP_ASSIGNMENT_QUERY);
         query.setCollection("groupIds",groupIds);
         Iterator assignedSuperGroupsIds = broker.iterate(query);
         while (assignedSuperGroupsIds.hasNext()){
            notAssignableGroupIds.add((Long)assignedSuperGroupsIds.next());
         }
      }
      //add user's already assigned groups to notAssignableGroupIds
      if (userIds.size() > 0){
         OpQuery query = broker.newQuery(USER_ASSIGNMENT_QUERY);
         query.setCollection("userIds",userIds);
         Iterator assignedGroupsIds = broker.iterate(query);
         while (assignedGroupsIds.hasNext()){
            notAssignableGroupIds.add(assignedGroupsIds.next());
         }
      }
      return notAssignableGroupIds;
   }


   private void addSubGroupRows(OpBroker broker, XComponent dataSet, XLocalizer localizer, long groupId, int outlineLevel, boolean showUsers, Collection excludedGroups) {

      OpObjectOrderCriteria groupOrderCriteria = new OpObjectOrderCriteria(OpGroup.GROUP, OpGroup.NAME, OpObjectOrderCriteria.ASCENDING);
      OpQuery query;
      if (groupId == -1) {
         String queryString = "select group from OpGroup as group" + groupOrderCriteria.toHibernateQueryString("group");
         query = broker.newQuery(queryString);
      }
      else {
         String queryString = "select subGroup from OpGroupAssignment as assignment inner join assignment.SubGroup as subGroup where assignment.SuperGroup.ID = ?";
         queryString += groupOrderCriteria.toHibernateQueryString("subGroup");
         query = broker
               .newQuery(queryString);
         query.setLong(0, groupId);
      }

      Iterator subGroups = broker.iterate(query);
      OpGroup subGroup;
      XComponent dataRow;
      while (subGroups.hasNext()) {
         subGroup = (OpGroup) subGroups.next();
         int nextOutline;
         if (!excludedGroups.contains(new Long(subGroup.getID()))){
            dataRow = new XComponent(XComponent.DATA_ROW);
            dataRow.setOutlineLevel(outlineLevel);
            dataRow.setStringValue(XValidator.choice(subGroup.locator(), localizer.localize(subGroup.getDisplayName()), GROUP_ICON_INDEX));
            dataSet.addChild(dataRow);
            nextOutline = outlineLevel + 1;
            // Recursively add sub-groups and users
            addSubGroupRows(broker, dataSet, localizer, subGroup.getID(), nextOutline, showUsers, excludedGroups);
         }
         else{
            nextOutline = outlineLevel;
         }
         if (showUsers) {
            addUserRows(broker, dataSet, localizer, subGroup.getID(), nextOutline);
         }
      }
   }

   private void addUserRows(OpBroker broker, XComponent dataSet, XLocalizer localizer, long groupId, int outlineLevel) {

      OpQuery query = null;
      OpObjectOrderCriteria userOrderCriteria = new OpObjectOrderCriteria(OpUser.USER, OpUser.NAME, OpObjectOrderCriteria.ASCENDING);
      if (groupId == -1) {
         String userQueryString = "select user from OpUser as user" + userOrderCriteria.toHibernateQueryString("user");
         query = broker.newQuery(userQueryString);
      }
      else {
         String queryString = "select user from OpUserAssignment as assignment inner join assignment.User as user where assignment.Group.ID = ?";
         queryString += userOrderCriteria.toHibernateQueryString("user");
         query = broker
               .newQuery(queryString);
         query.setLong(0, groupId);
      }

      Iterator users = broker.iterate(query);
      OpUser user = null;
      XComponent dataRow = null;
      while (users.hasNext()) {
         user = (OpUser) users.next();
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setOutlineLevel(outlineLevel);
         dataRow.setStringValue(XValidator.choice(user.locator(), localizer.localize(user.getDisplayName()), USER_ICON_INDEX));
         dataSet.addChild(dataRow);
      }

   }

}
