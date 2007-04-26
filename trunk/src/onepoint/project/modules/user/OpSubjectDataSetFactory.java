/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocalizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class used for creating client-side representations of subject hierarchies.
 *
 * @author horia.chiorean
 */
public final class OpSubjectDataSetFactory {

   /**
    * Subject specific constants
    */
   public final static int GROUP_ICON_INDEX = 0;
   public final static int USER_ICON_INDEX = 1;

   private final static String GROUP_DESCRIPTOR = "g";
   private final static String USER_DESCRIPTOR = "u";

   private final static String USER_ASSIGNMENT_QUERY = "select distinct ua.Group from OpUserAssignment as ua where ua.User.ID in (:userIds)";
   private final static String GROUP_ASSIGNMENT_QUERY = "select distinct ga.SuperGroup from OpGroupAssignment as ga where ga.SubGroup.ID in (:groupIds)";

   /**
    * This is a utility class.
    */
   private OpSubjectDataSetFactory() {
   }

   /**
    * Retrives a hierarchy of subjects, either in simple of complex structure.
    *
    * @param session            a <code>OpProjectSession</code> used for performing business operations.
    * @param dataSet            a <code>XComponent(DATA_SET)</code> that will contain the client side structure.
    * @param filteredSubjectIds List of ids to be removed from the final result.
    * @param groupId
    * @param outlineLevel
    * @param simpleStructure    a <code>boolean</code> indicating whether the retrived structure should be simple or complex.
    */
   public static void retrieveSubjectHierarchy(OpProjectSession session, XComponent dataSet,
        List filteredSubjectIds, long groupId, int outlineLevel, boolean simpleStructure) {

      // We are using display name here in order to localize name of group "Everyone"
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));

      OpBroker broker = session.newBroker();
      addSubGroupRows(broker, dataSet, localizer, groupId, outlineLevel, simpleStructure, filteredSubjectIds);
      addUserRows(broker, dataSet, localizer, groupId, outlineLevel, simpleStructure, filteredSubjectIds);
      broker.close();
   }

   /**
    * Adds (recursivelly) sub-group rows to the given data-set parameter.
    *
    * @param broker
    * @param dataSet
    * @param localizer
    * @param groupId
    * @param outlineLevel
    * @param simpleStructure
    * @param filteredSubjectIds
    */
   private static void addSubGroupRows(OpBroker broker, XComponent dataSet, XLocalizer localizer, long groupId, int outlineLevel,
        boolean simpleStructure, List filteredSubjectIds) {
      // configure group sort order
      OpObjectOrderCriteria groupOrderCriteria = new OpObjectOrderCriteria(OpGroup.GROUP, OpGroup.NAME, OpObjectOrderCriteria.ASCENDING);
      OpQuery query = null;
      if (groupId == -1) {
         String queryString = "select gr.ID, count(subGroupAssignment.ID)+count(userAssignment.ID) from OpGroup gr " +
              "left join gr.SubGroupAssignments subGroupAssignment " +
              "left join gr.UserAssignments userAssignment " +
              "group by gr.ID, gr.Name ";
         groupOrderCriteria.toHibernateQueryString("gr");
         query = broker.newQuery(queryString);
      }
      else {
         String queryString =
              "select subGroup.ID, count(subGroupAssignment.ID)+count(userAssignment.ID) " +
                   "from OpGroupAssignment assignment " +
                   "inner join assignment.SubGroup subGroup " +
                   "left join subGroup.SubGroupAssignments subGroupAssignment " +
                   "left join subGroup.UserAssignments userAssignment " +
                   "where assignment.SuperGroup.ID = ?" +
                   "group by subGroup.ID, subGroup.Name " +
                   groupOrderCriteria.toHibernateQueryString("subGroup");
         query = broker.newQuery(queryString);
         query.setLong(0, groupId);
      }

      Iterator results = broker.list(query).iterator();
      OpGroup subGroup;
      while (results.hasNext()) {
         Object[] record = (Object[]) results.next();
         Long subGroupID = (Long) record[0];
         subGroup = (OpGroup) broker.getObject(OpGroup.class, subGroupID.longValue());
         if (filteredSubjectIds != null && filteredSubjectIds.contains(subGroup.locator())) {
            continue;
         }
         Integer subRowsNr = (Integer) record[1];
         XComponent row = retrieveSubjectRow(subGroup, outlineLevel, localizer, simpleStructure);
         dataSet.addChild(row);
         if (subRowsNr.intValue() != 0 && !allChildrenFiltered(subGroup, filteredSubjectIds)) {
            row.setExpanded(false);
            //add dummy row
            XComponent dummyRow = new XComponent(XComponent.DATA_ROW);
            dummyRow.setStringValue(OpProjectConstants.DUMMY_ROW_ID);
            dummyRow.setOutlineLevel(outlineLevel + 1);
            dummyRow.setFiltered(true);
            dummyRow.setVisible(false);
            dummyRow.setSelectable(false);
            dataSet.addChild(dummyRow);
         }
      }
   }

   private static boolean allChildrenFiltered(OpGroup group, List filteredLocators) {

      if (filteredLocators == null || filteredLocators.isEmpty()) {
         return false;
      }

      Set groupAssignments = group.getSubGroupAssignments();
      for (Iterator iterator = groupAssignments.iterator(); iterator.hasNext();) {
         OpGroupAssignment assignment = (OpGroupAssignment) iterator.next();
         if (!filteredLocators.contains(assignment.getSubGroup().locator())) {
            return false;
         }
      }

      Set userAssignments = group.getUserAssignments();
      for (Iterator iterator = userAssignments.iterator(); iterator.hasNext();) {
         OpUserAssignment assignment = (OpUserAssignment) iterator.next();
         if (!filteredLocators.contains(assignment.getUser().locator())) {
            return false;
         }
      }

      return true;
   }

   /**
    * Adds user rows to the given data-set.
    */
   private static void addUserRows(OpBroker broker, XComponent dataSet, XLocalizer localizer, long groupId, int outlineLevel,
        boolean simpleStructure, List filteredSubjectIds) {

      // configure user sort order
      OpObjectOrderCriteria userOrderCriteria = new OpObjectOrderCriteria(OpUser.USER, OpUser.NAME, OpObjectOrderCriteria.ASCENDING);
      OpQuery query;
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
      OpUser user;
      while (users.hasNext()) {
         user = (OpUser) users.next();
         if (filteredSubjectIds != null && filteredSubjectIds.contains(user.locator())) {
            continue;
         }
         XComponent row = retrieveSubjectRow(user, outlineLevel, localizer, simpleStructure);
         dataSet.addChild(row);
      }
   }

   /**
    * Retrieves a data row containing client-side information for a subject.
    *
    * @param subject         a <code>OpSubject</code> representing the subject whose data will be retrieved.
    * @param outlineLevel    an <code>int</code> representing the outline level of the data row to be added.
    * @param localizer       a <code>XLocalizer</code> used for i18n-ing the display name of a subject.
    * @param simpleStructure a <code>boolean</code> which indicates the type of structure the data-row will contain.
    * @return Data row with the subject information.
    */
   private static XComponent retrieveSubjectRow(OpSubject subject, int outlineLevel, XLocalizer localizer, boolean simpleStructure) {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setOutlineLevel(outlineLevel);
      String subjectChoice = XValidator.choice(subject.locator(), localizer.localize(subject.getDisplayName()),
           getSubjectIconIndex(subject));

      if (simpleStructure) {
         dataRow.setStringValue(subjectChoice);
      }
      else {
         dataRow.setStringValue(subject.locator());

         // Data row w/index '0' is indicator row ('u' or 'g' for user/group)
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(getSubjectDescriptor(subject));
         dataRow.addChild(dataCell);

         // Display name and icon
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(subjectChoice);
         dataRow.addChild(dataCell);

         // Description
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(localizer.localize(subject.getDescription()));
         dataRow.addChild(dataCell);
      }
      return dataRow;
   }

   /**
    * Returns the descriptor string for a given subject.
    *
    * @param subject a <code>OpSubject</code> instance representing either a user or a group.
    * @return a <code>String</code> representing the descriptor of the subject or null if none can be found.
    */
   private static String getSubjectDescriptor(OpSubject subject) {
      if (subject.getPrototype().getInstanceClass().equals(OpUser.class)) {
         return USER_DESCRIPTOR;
      }
      if (subject.getPrototype().getInstanceClass().equals(OpGroup.class)) {
         return GROUP_DESCRIPTOR;
      }
      return null;
   }

   /**
    * Returns the icon index for a given subject.
    *
    * @param subject a <code>OpSubject</code> instance representing either a user or a group.
    * @return a <code>String</code> representing the icon index of the subject, or -1 if none can be found.
    */
   private static int getSubjectIconIndex(OpSubject subject) {
      if (subject.getPrototype().getInstanceClass().equals(OpUser.class)) {
         return USER_ICON_INDEX;
      }
      if (subject.getPrototype().getInstanceClass().equals(OpGroup.class)) {
         return GROUP_ICON_INDEX;
      }
      return -1;
   }


   /**
    * Traverses the subject hierarchy, enabling or disabling different types of subjects.
    *
    * @param subjectDataSet a <code>XComponent(DATA_SET)</code> representing the subject data-set.
    * @param enableUsers    a <code>boolean</code> indicating whether users should be enabled in the hierarchy.
    * @param enableGroups   a <code>boolean</code> indicating whether group should be enabled in the hierarchy.
    */
   public static void enableSubjectHierarchy(XComponent subjectDataSet, boolean enableUsers, boolean enableGroups) {
      for (int i = 0; i < subjectDataSet.getChildCount(); i++) {
         XComponent subjectRow = (XComponent) subjectDataSet.getChild(i);

         String choice = subjectRow.getStringValue();
         if (!OpProjectConstants.DUMMY_ROW_ID.equals(choice)) {
            String choiceId = XValidator.choiceID(choice);
            OpLocator locator = OpLocator.parseLocator(choiceId);
            if (locator.getPrototype().getInstanceClass().equals(OpUser.class) && !enableUsers) {
               subjectRow.setSelectable(false);
            }
            else if (locator.getPrototype().getInstanceClass().equals(OpGroup.class) && !enableGroups) {
               subjectRow.setSelectable(false);
            }
         }
      }
   }

   /**
    * @param session            OpProjectSession to be used for querying the db.
    * @param filteredSubjectIds List of already filtered ids.
    * @return A list containing the given filteredSubjectIds and the user's/group's super groups.
    */
   public static List getAlreadyAssignedGroups(OpProjectSession session, List filteredSubjectIds) {
      List result = new ArrayList();

      List groupIds = new ArrayList();
      List userIds = new ArrayList();
      for (int i = 0; i < filteredSubjectIds.size(); i++) {
         String choiceId = (String) filteredSubjectIds.get(i);
         OpLocator locator = OpLocator.parseLocator(choiceId);
         Long locatorId = new Long(locator.getID());
         if (locator.getPrototype().getInstanceClass() == OpGroup.class) {
            //can not assign a group to itself
            result.add(choiceId);
            groupIds.add(locatorId);
         }
         if (locator.getPrototype().getInstanceClass() == OpUser.class) {
            result.add(choiceId);
            userIds.add(locatorId);
         }
      }

      OpBroker broker = session.newBroker();
      //add group's already assigned supergroups to result
      if (groupIds.size() > 0) {
         OpQuery query = broker.newQuery(GROUP_ASSIGNMENT_QUERY);
         query.setCollection("groupIds", groupIds);
         Iterator assignedSuperGroups = broker.iterate(query);
         while (assignedSuperGroups.hasNext()) {
            OpGroup group = (OpGroup) assignedSuperGroups.next();
            result.add(OpLocator.locatorString(group));
         }
      }

      //add user's already assigned groups to result
      if (userIds.size() > 0) {
         OpQuery query = broker.newQuery(USER_ASSIGNMENT_QUERY);
         query.setCollection("userIds", userIds);
         Iterator assignedGroups = broker.iterate(query);
         while (assignedGroups.hasNext()) {
            OpGroup group = (OpGroup) assignedGroups.next();
            result.add(OpLocator.locatorString(group));
         }
      }
      broker.close();

      return result;
   }


}
