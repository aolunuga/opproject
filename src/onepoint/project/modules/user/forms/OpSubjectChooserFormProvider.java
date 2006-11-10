/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpSubjectDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class OpSubjectChooserFormProvider implements XFormProvider {

   // Form parameters and component IDs
   private final static String CALLING_FRAME_ID = "CallingFrameID";
   private final static String ACTION_HANDLER = "ActionHandler";
   private final static String SUBJECT_SET = "SubjectSet";
   private final static String ENABLE_USERS = "EnableUsers";
   private final static String ENABLE_GROUPS = "EnableGroups";
   private final static String FILTERED_SUBJECT_IDS = "FilteredSubjectIds";
   private final static String MULTIPLE_SELECTION = "MultipleSelection";
   private final static String INCLUDE_PARENTS_IN_FILTER = "IncludeParentsInFilter";
   private final static String LIST_BOX_ID = "SubjectList";

   private final static String USER_ASSIGNMENT_QUERY = "select distinct ua.Group from OpUserAssignment as ua where ua.User.ID in (:userIds)";
   private final static String GROUP_ASSIGNMENT_QUERY = "select distinct ga.SuperGroup from OpGroupAssignment as ga where ga.SubGroup.ID in (:groupIds)";

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

      XComponent data_set = form.findComponent(SUBJECT_SET);
      OpBroker broker = ((OpProjectSession) session).newBroker();
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));

      //retrive subject data-set
      OpSubjectDataSetFactory.retrieveSimpleSubjectHierarchy(broker, data_set, localizer);

      //filter if necessary
      Boolean includeParentsInFilter = (Boolean) parameters.get(INCLUDE_PARENTS_IN_FILTER);
      List filteredSubjectIds = (List) parameters.get(FILTERED_SUBJECT_IDS);
      if (filteredSubjectIds != null && filteredSubjectIds.size() > 0) {
         if (includeParentsInFilter != null && includeParentsInFilter.booleanValue()) {
            filteredSubjectIds = getAlreadyAssignedGroups(filteredSubjectIds, broker);
         }
         this.filterSubjectSet(data_set, filteredSubjectIds);
      }

      broker.close();

      //enable/disable items in the hierarchy
      boolean enableUsers = ((Boolean) parameters.get(ENABLE_USERS)).booleanValue();
      boolean enableGroups = ((Boolean) parameters.get(ENABLE_GROUPS)).booleanValue();
      this.enableSubjectHierarchy(data_set, enableUsers, enableGroups);

      //synchronize expansion state
      data_set.synchronizeExpanded();
   }

   /**
    * From the given data set (which represents a subject set), removes the entries which have the value among the filtered ids.
    *
    * @param subjectSet  a <code>XComponent(DATA_SET)</code> representing a subject set.
    * @param filteredIds a <code>List</code> of <code>String</code> representing the ids which are to be filtered.
    */
   private void filterSubjectSet(XComponent subjectSet, List filteredIds) {
      List childrenToRemove = new ArrayList();
      //first collect the children
      for (int i = 0; i < subjectSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) subjectSet.getChild(i);

         String locator = XValidator.choiceID(dataRow.getStringValue());
         if (filteredIds.contains(locator)) {
            //add all the children of the data row
            List children = dataRow.getSubRows();
            for (Iterator it = children.iterator(); it.hasNext();) {
               XComponent childRow = (XComponent) it.next();
               if (!childrenToRemove.contains(dataRow)) {
                  childrenToRemove.add(childRow);
               }
            }

            if (!childrenToRemove.contains(dataRow)) {
               childrenToRemove.add(dataRow);
            }
         }
      }

      for (Iterator it = childrenToRemove.iterator(); it.hasNext(); ) {
         XComponent childRow = (XComponent) it.next();
         subjectSet.removeChild(childRow);
      }
   }

   /**
    * Traverses the subject hierarchy, enabling or disabling different types of subjects.
    *
    * @param subjectDataSet a <code>XComponent(DATA_SET)</code> representing the subject data-set.
    * @param enableUsers    a <code>boolean</code> indicating whether users should be enabled in the hierarchy.
    * @param enableGroups   a <code>boolean</code> indicating whether group should be enabled in the hierarchy.
    */
   private void enableSubjectHierarchy(XComponent subjectDataSet, boolean enableUsers, boolean enableGroups) {
      for (int i = 0; i < subjectDataSet.getChildCount(); i++) {
         XComponent subjectRow = (XComponent) subjectDataSet.getChild(i);

         String choice = subjectRow.getStringValue();
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


   private List getAlreadyAssignedGroups(List filteredSubjectIds, OpBroker broker) {
      List result = new ArrayList();

      List groupIds = new ArrayList();
      List userIds = new ArrayList();
      for (int i = 0; i < filteredSubjectIds.size(); i++) {
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         String choiceId = (String) filteredSubjectIds.get(i);
         dataRow.setStringValue(choiceId);
         OpLocator locator = OpLocator.parseLocator(dataRow.getStringValue());
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
      return result;
   }
}
