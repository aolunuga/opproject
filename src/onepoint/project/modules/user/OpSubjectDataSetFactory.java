/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.resource.XLocalizer;

import java.util.Iterator;

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


   /**
    * This is a utility class.
    */
   private OpSubjectDataSetFactory() {
   }

   /**
    * Retrieves a subject hierarchy having a simple structure.
    */
   public static void retrieveSimpleSubjectHierarchy(OpBroker broker, XComponent dataSet, XLocalizer localizer) {
      retrieveSubjectHierarchy(broker, dataSet, localizer, true);
   }

   /**
    * Retrieves a subject hierarchy having a complex structure.
    */
   public static void retrieveComplexSubjectHierarchy(OpBroker broker, XComponent dataSet, XLocalizer localizer) {
      retrieveSubjectHierarchy(broker, dataSet, localizer, false);
   }

   /**
    * Retrives a hierarchy of subjects, either in simple of complex structure.
    * @param broker a <code>OpBroker</code> used for performing business operations.
    * @param dataSet a <code>XComponent(DATA_SET)</code> that will contain the client side structure.
    * @param localizer a <code>XLocalizer</code> used for i18n.
    * @param simpleStructure a <code>boolean</code> indicating whether the retrived structure should be simple or complex.
    */
   private static void retrieveSubjectHierarchy(OpBroker broker, XComponent dataSet, XLocalizer localizer, boolean simpleStructure) {
      addSubGroupRows(broker, dataSet, localizer, -1, 0, simpleStructure);
      addUserRows(broker, dataSet, localizer, -1, 0, simpleStructure);      
   }

   /**
    * Adds (recursivelly) sub-group rows to the given data-set parameter.
    * @param broker
    * @param dataSet
    * @param localizer
    * @param groupId
    * @param outlineLevel
    * @param simpleStructure
    */
   private static void addSubGroupRows(OpBroker broker, XComponent dataSet, XLocalizer localizer, long groupId, int outlineLevel,
        boolean simpleStructure) {
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
      while (subGroups.hasNext()) {
         subGroup = (OpGroup) subGroups.next();
         retrieveSubjectRow(dataSet, subGroup, outlineLevel, localizer, simpleStructure);

         // Recursively add sub-groups and users
         addSubGroupRows(broker, dataSet, localizer, subGroup.getID(), outlineLevel + 1, simpleStructure);
         addUserRows(broker, dataSet, localizer, subGroup.getID(), outlineLevel + 1, simpleStructure);
      }
   }

   /**
    * Adds user rows to the given data-set.
    */
   private static void addUserRows(OpBroker broker, XComponent dataSet, XLocalizer localizer, long groupId, int outlineLevel,
        boolean simpleStructure) {
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
      while (users.hasNext()) {
         user = (OpUser) users.next();
         retrieveSubjectRow(dataSet, user, outlineLevel, localizer, simpleStructure);
      }
   }

   /**
    * Retrieves a data row containing client-side information for a subject.
    *
    * @param dataSet         a <code>XComponent(DATA_SET)</code> to which a data row will be added.
    * @param subject         a <code>OpSubject</code> representing the subject whose data will be retrieved.
    * @param outlineLevel    an <code>int</code> representing the outline level of the data row to be added.
    * @param localizer       a <code>XLocalizer</code> used for i18n-ing the display name of a subject.
    * @param simpleStructure a <code>boolean</code> which indicates the type of structure the data-row will contain.
    */
   private static void retrieveSubjectRow(XComponent dataSet, OpSubject subject, int outlineLevel, XLocalizer localizer, boolean simpleStructure) {
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
      dataSet.addChild(dataRow);
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
}
