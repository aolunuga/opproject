/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpSubjectDataSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;
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

      //filter if necessary
      Boolean includeParentsInFilter = (Boolean) parameters.get(INCLUDE_PARENTS_IN_FILTER);
      List filteredSubjectIds = (List) parameters.get(FILTERED_SUBJECT_IDS);
      form.findComponent(INCLUDE_PARENTS_IN_FILTER).setValue(includeParentsInFilter);
      form.findComponent(FILTERED_SUBJECT_IDS).setValue(filteredSubjectIds);

      if (includeParentsInFilter != null && includeParentsInFilter.booleanValue()) {
         filteredSubjectIds = OpSubjectDataSetFactory.getAlreadyAssignedGroups(session, filteredSubjectIds);
      }

      //retrive subject data-set
      OpSubjectDataSetFactory.retrieveSubjectHierarchy(session, data_set, filteredSubjectIds, -1, 0, true);

      //enable/disable items in the hierarchy
      Boolean enableUsersValue = ((Boolean) parameters.get(ENABLE_USERS));
      Boolean enableGroupsValue = ((Boolean) parameters.get(ENABLE_GROUPS));
      boolean enableUsers = enableUsersValue.booleanValue();
      boolean enableGroups = enableGroupsValue.booleanValue();

      form.findComponent(ENABLE_USERS).setValue(enableUsersValue);
      form.findComponent(ENABLE_GROUPS).setValue(enableGroupsValue);

      OpSubjectDataSetFactory.enableSubjectHierarchy(data_set, enableUsers, enableGroups);
   }

}
