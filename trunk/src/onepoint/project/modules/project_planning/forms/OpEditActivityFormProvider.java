/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.forms;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityCategory;
import onepoint.project.modules.project.OpActivityComment;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityIfc;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpAssignmentIfc;
import onepoint.project.modules.project.OpAttachmentIfc;
import onepoint.project.modules.project.OpDependencyIfc;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpSubjectDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

public class OpEditActivityFormProvider implements XFormProvider {

   public final static String PROJECT_EDIT_ACTIVITY = "project_planning.EditActivity";

   protected static final XLog logger = XLogFactory.getLogger(OpActivitiesFormProvider.class);

   private final static String RESPONSIBLE_RESOURCE_SET = "ResponsibleResourceSet";
   private final static String COMMENTS_LABEL = "CommentsLabel";
   private final static String COMMENTS_PANEL = "CommentsPanel";
   private final static String ADD_COMMENT_BUTTON = "AddCommentButton";
   public final static String ACTIVITY_ID_FIELD = "ActivityIDField";
   public final static String PROGRESS_TRACKED_FIELD = "ProgressTrackedField";
   private static final String CALCULATION_MODE = "CalculationMode";
   private static final String SUBJECT_ID_FIELD = "SubjectIDField";
   private final static String HAS_COMMENTS_FIELD = "HasCommentsField";
   private final static String EXCLUDED_RESOURCES = "ExcludedResources";

   //action that should be performed when a comment is removed
   private static final String REMOVE_COMMENT_ACTION = "removeComment";
   //remove comment button icon
   private static final String REMOVE_COMMENT_ICON = "/icons/minus_s.png";
   private static final String REMOVE_COMMENT_BUTTON_STYLE_REF = "icon-button-default";
   private static final String COMMENT_SO_FAR = "CommentSoFar";
   private static final String COMMENTS_SO_FAR = "CommentsSoFar";
   private static final String NO_COMMENTS = "NoCommentsPossible";
   private static final String NO_RESOURCE_TEXT = "NoResource";
   private static final String COMPLETE = "Complete";
   private static final String NO_CATEGORY_RESOURCE = "NoCategory";
   private static final String COSTS_TAB = "CostsTab";
   private static final String RESOURCES_TAB = "ResourcesTab";
   private static final String ASSIGNMENT_TABLE = "AssignmentTable";

   private static final String FILL_DATA = "fillData";

   public static final String EDIT_MODE = "editMode";

   private static final String EDIT_ACTIONS_MODE = "editActionsMode";

   private static final String ACTIONS_EDITABLE = "ActionsEditable";

   protected int activityType = -1;

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      logger.info("OpEditActivityFormProvider.prepareForm()");

      OpProjectSession session = (OpProjectSession) s;

      OpBroker broker = session.newBroker();
      try {
         OpUser currentUser = session.user(broker);

         Object activityTypeObject = parameters.get(OpProjectPlanningService.ACTIVITY_TYPE);
         if (activityTypeObject != null) {
            activityType = (Byte) activityTypeObject;
         }

         // Check for activity ID (get activity if activity is a version)
         String activityLocator = (String) parameters.get(OpProjectPlanningService.ACTIVITY_ID);
         form.findComponent(SUBJECT_ID_FIELD).setStringValue(activityLocator);

         Boolean editActivityModeBoolean = (Boolean)parameters.get(EDIT_ACTIONS_MODE);
         boolean editActivityMode = false;
         if (editActivityModeBoolean != null) {
            editActivityMode = editActivityModeBoolean.booleanValue(); // false = called from my_tasks, true = called from activities
         }
         OpActivityVersion activity = null;
         if (activityLocator != null) {
            Object obj = broker.getObject(activityLocator);
            if (obj instanceof OpActivity) {
               activity = ((OpActivity)obj).getLatestVersion();
            }
            else {
               activity = (OpActivityVersion) obj;
            }
            if (activity != null) {
               form.findComponent(ACTIVITY_ID_FIELD).setStringValue(activity.locator());
//               form.findComponent(PROGRESS_TRACKED_FIELD).setBooleanValue(activity.getProjectPlan().getProgressTracked());
               boolean actionsEditable = !activity.getProjectPlan().getProgressTracked();
               if (editActivityMode) {
                  actionsEditable = !actionsEditable;
               }
               form.findComponent(ACTIONS_EDITABLE).setBooleanValue(actionsEditable);
            }
         }

         //enable the %complete field if tracking is off
         String currentProjectId = (String) session.getVariable(OpProjectConstants.PROJECT_ID);
         if (currentProjectId != null) {
            OpProjectNode project = (OpProjectNode) broker.getObject(currentProjectId);
            if (activity == null) {
               boolean actionsEditable = !project.getPlan().getProgressTracked();
               if (editActivityMode) {
                  actionsEditable = !actionsEditable;
               }
               form.findComponent(ACTIONS_EDITABLE).setBooleanValue(actionsEditable);
            }
            XComponent projectResources = new XComponent(XComponent.DATA_SET);
            XComponent excludedResources = new XComponent(XComponent.DATA_SET);
            List<Long> projectResourcesList = new ArrayList<Long>();

            OpActivityDataSetFactory.getInstance().retrieveResourceDataSet(broker, project, projectResources);

            for (int i = 0; i < projectResources.getChildCount(); i++) {
               XComponent d = (XComponent) projectResources.getChild(i);
               projectResourcesList.add(OpLocator.parseLocator(XValidator.choiceID(d.getStringValue())).getID());
            }

            OpQuery query = null;
            if (projectResourcesList.size() != 0) {
               query = broker.newQuery("from OpResource resource where resource.Archived=false and resource.id not in (:resourceIds)");
               query.setCollection("resourceIds", projectResourcesList);
            }
            else {
               query = broker.newQuery("from OpResource where Archived=false");
            }

            Iterator it = broker.iterate(query);

            while (it.hasNext()) {
               OpResource resource = (OpResource) it.next();
               XComponent dataRow = new XComponent(XComponent.DATA_ROW);
               dataRow.setStringValue(resource.locator()+"['"+resource.getName()+"']");
               excludedResources.addChild(dataRow);
            }

            form.findComponent(EXCLUDED_RESOURCES).setValue(excludedResources);

            if (!project.getPlan().getProgressTracked()) {
               form.findComponent(COMPLETE).setEnabled(true);
            }
            else {
               form.findComponent(COMPLETE).setEnabled(false);
            }

            if(project.getType() == OpProjectNode.TEMPLATE) {
               form.findComponent(RESOURCES_TAB).setHidden(true);
            }

            form.findComponent(ASSIGNMENT_TABLE).setSelectionModel(XComponent.CELL_SELECTION);
         }

         XLanguageResourceMap resourceMap = session.getLocale().getResourceMap(PROJECT_EDIT_ACTIVITY);

         //Resource set
         XComponent responsibleSet = form.findComponent(RESPONSIBLE_RESOURCE_SET);
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         String noResource = resourceMap.getResource(NO_RESOURCE_TEXT).getText();
         dataRow.setStringValue(XValidator.choice(OpGanttValidator.NO_RESOURCE_ID, noResource));
         responsibleSet.addChild(dataRow);

         //if the app. is multiuser and hide manager features is set to true and the user is not manager
         if (OpSubjectDataSetFactory.shouldHideFromUser(session, currentUser)) {
            //hide costs tab 
            form.findComponent(COSTS_TAB).setHidden(true);
         }

         if (activity != null) {
            if (Boolean.TRUE.equals((Boolean)parameters.get(FILL_DATA))) {
               fillData(session, broker, activity, form, parameters);
            }
            // TODO: see onepoint.project.modules.project_planning.OpProjectPlanningService.insertComment(OpProjectSession, XMessage)
            showComments(form, activity.getActivityForAdditionalObjects(), session, broker, resourceMap, true);

         }
         logger.info("/OpEditActivityFormProvider.prepareForm()");
      }
      finally {
         broker.close();
      }
   }

   /**
    * @param broker 
    * @param session 
    * @param form 
    * @param parameters 
    * @param object
    * @pre
    * @post
    */
   private void fillData(OpProjectSession session, OpBroker broker, OpActivityVersion activity, XComponent form, HashMap parameters) {
//      String title = getTitle(activity);
//      if (title != null) {
         Set<String> disabledFields = getDisabledFields(activity);
         Set<String> hiddenFields = getHiddenFields(activity);
         Set<String> hiddenTabs = getDisabledTabs(activity);
         addAlwaysDisableFields(disabledFields);
         Boolean editMode = (Boolean)parameters.get(EDIT_MODE);
         OpProjectPlan projectPlan = activity.getProjectPlan();

         baseEditActivity(session, broker, form, parameters, activity, 
               Boolean.TRUE == editMode, activity.getProjectPlan().getProgressTracked(), 
               disabledFields, hiddenFields, hiddenTabs, false);
      }
//   }

   private static void addAlwaysDisableFields( Set<String> disabledFieldsIds) {
      disabledFieldsIds.add("Category");
   }

   /**
    * @param activity
    * @return
    * @pre
    * @post
    */
   private String getTitle(OpActivityIfc activity) {
      switch (activity.getType()) {
      case OpActivity.STANDARD :
         return "StandardDialogTitle";
      case OpActivity.COLLECTION :
         return "CollectionDialogTitle";
      case OpActivity.MILESTONE :
         return "MilestoneDialogTitle";
      case OpActivity.TASK :
         return "TaskDialogTitle";
      case OpActivity.COLLECTION_TASK :
         return "CollectionTaskDialogTitle";
      case OpActivity.SCHEDULED_TASK :
         return "ScheduledTaskTitle";
      }
      return null;
   }

   /**
    * @param activity
    * @return
    * @pre
    * @post
    */
   private Set<String> getDisabledFields(OpActivityIfc activity) {
      HashSet<String> disabledFieldsIds = new HashSet<String>();
//      disabledFieldsIds.add("CostsSum");
      switch (activity.getType()) {
      case OpActivity.STANDARD :
         disabledFieldsIds.add("PersonnelCosts");
         disabledFieldsIds.add("ProceedsCosts");
         break;
      case OpActivity.COLLECTION :
         disabledFieldsIds.add("Complete");
         disabledFieldsIds.add("Start");
         disabledFieldsIds.add("End");
         disabledFieldsIds.add("Duration");
         disabledFieldsIds.add("BaseEffort");
         disabledFieldsIds.add("PersonnelCosts");
         disabledFieldsIds.add("TravelCosts");
         disabledFieldsIds.add("MaterialCosts");
         disabledFieldsIds.add("MiscellaneousCosts");
         disabledFieldsIds.add("ExternalCosts");
         disabledFieldsIds.add("ProceedsCosts");
         disabledFieldsIds.add("BaseEffortSum");
         // advanced planning:
         disabledFieldsIds.add("LeadTime");
         disabledFieldsIds.add("FollowUpTime");
         disabledFieldsIds.add("StartFixed");
         disabledFieldsIds.add("FinishFixed");
         break;
      case OpActivity.MILESTONE :
         break;
      case OpActivity.TASK :
         disabledFieldsIds.add("PersonnelCosts");
         disabledFieldsIds.add("ProceedsCosts");
         break;
      case OpActivity.COLLECTION_TASK :
         disabledFieldsIds.add("Complete");
         disabledFieldsIds.add("PersonnelCosts");
         disabledFieldsIds.add("TravelCosts");
         disabledFieldsIds.add("MaterialCosts");
         disabledFieldsIds.add("MiscellaneousCosts");
         disabledFieldsIds.add("ExternalCosts");
         disabledFieldsIds.add("BaseEffort");
         disabledFieldsIds.add("ProceedsCosts");
         disabledFieldsIds.add("BaseEffortSum");
         break;
      case OpActivity.SCHEDULED_TASK :
         disabledFieldsIds.add("BaseEffort");
         disabledFieldsIds.add("PersonnelCosts");
         disabledFieldsIds.add("TravelCosts");
         disabledFieldsIds.add("MaterialCosts");
         disabledFieldsIds.add("MiscellaneousCosts");
         disabledFieldsIds.add("ExternalCosts");
         disabledFieldsIds.add("ProceedsCosts");
         disabledFieldsIds.add("BaseEffortSum");
         break;
      default :
         return disabledFieldsIds;
      }
      return disabledFieldsIds;
   }

   /**
    * @param activity
    * @return
    * @pre
    * @post
    */
   private Set<String> getHiddenFields(OpActivityIfc activity) {
      HashSet<String> hiddenFieldsIds = new HashSet<String>();
      switch (activity.getType()) {
      case OpActivity.STANDARD :
         break;
      case OpActivity.COLLECTION :
         hiddenFieldsIds.add("Priority");
         hiddenFieldsIds.add("EffortBillable");
         break;
      case OpActivity.MILESTONE :
         hiddenFieldsIds.add("Priority");
         hiddenFieldsIds.add("Duration");
         hiddenFieldsIds.add("BaseEffort");
         hiddenFieldsIds.add("EffortBillable");
         hiddenFieldsIds.add("BaseEffortSum");
         // advanced planning:
         hiddenFieldsIds.add("LeadTime");
         hiddenFieldsIds.add("FollowUpTime");
         break;
      case OpActivity.TASK :
         hiddenFieldsIds.add("Start");
         hiddenFieldsIds.add("End");
         hiddenFieldsIds.add("Duration");
         hiddenFieldsIds.add("StartFixed");
         hiddenFieldsIds.add("FinishFixed");
         // hiddenFieldsIds.add("Exported");
         // advanced planning:
         hiddenFieldsIds.add("LeadTime");
         hiddenFieldsIds.add("FollowUpTime");
         break;
      case OpActivity.COLLECTION_TASK :
         hiddenFieldsIds.add("Start");
         hiddenFieldsIds.add("End");
         hiddenFieldsIds.add("StartFixed");
         hiddenFieldsIds.add("FinishFixed");
         hiddenFieldsIds.add("Duration");
         hiddenFieldsIds.add("Priority");
         // hiddenFieldsIds.add("Exported");
         // advanced planning:
         hiddenFieldsIds.add("LeadTime");
         hiddenFieldsIds.add("FollowUpTime");
         break;
      case OpActivity.SCHEDULED_TASK :
         hiddenFieldsIds.add("Priority");
         break;
      default :
         return null;
      }
      return hiddenFieldsIds;
   }

   /**
    * @param activity
    * @return
    * @pre
    * @post
    */
   private Set<String> getDisabledTabs(OpActivityIfc activity) {
      HashSet<String> hiddenTabsIds = new HashSet<String>();
      switch (activity.getType()) {
      case OpActivity.STANDARD :
         break;
      case OpActivity.COLLECTION :
         hiddenTabsIds.add("ResourcesTab");
         hiddenTabsIds.add("WorkBreaksTab");
         break;
      case OpActivity.MILESTONE :
         hiddenTabsIds.add("CostsTab");
         // advanced planning:
         hiddenTabsIds.add("WorkBreaksTab");
         break;
      case OpActivity.TASK :
         hiddenTabsIds.add("PredecessorTab");
         // advanced planning:
         hiddenTabsIds.add("WorkBreaksTab");
         break;
      case OpActivity.COLLECTION_TASK :
         hiddenTabsIds.add("PredecessorTab");
         hiddenTabsIds.add("ResourcesTab");
         // advanced planning:
         hiddenTabsIds.add("WorkBreaksTab");
         break;
      case OpActivity.SCHEDULED_TASK :
         hiddenTabsIds.add("ResourcesTab");
         // advanced planning:
         hiddenTabsIds.add("WorkBreaksTab");
         break;
      default :
         return null;
      }
      return hiddenTabsIds;
   }

   private void baseEditActivity(OpProjectSession session, OpBroker broker, XComponent form, HashMap parameters, 
                                                 OpActivityVersion activity, boolean edit_mode, //boolean progressTracked, 
                                                 boolean actionsEditable, Set<String> disabledFieldsIds, Set<String> hiddenFieldIds, Set<String> hiddenTabsIds, 
                                                 boolean checkAvailability) {
//    MANDATORY_MASK = 1;
      String titleComponentId = getTitle(activity);
      OpProjectPlan projectPlan = activity.getProjectPlan();
      boolean isTemplate = projectPlan.getTemplate();
      boolean effortBased= projectPlan.getCalculationMode() == OpProjectPlan.EFFORT_BASED;

      edit_mode = edit_mode && !activity.isImported();
      boolean edit_mode_programm = edit_mode;
      if (edit_mode) {
         titleComponentId = "Edit" + titleComponentId;
      }
      else {
         titleComponentId = "Info" + titleComponentId;
      }
      String title = "";
      XComponent titleComponent = form.findComponent(titleComponentId);
      if (titleComponent != null) {
         title = titleComponent.getText();
      }
      // FIXME(dfreis Sep 17, 2008 1:51:02 PM) may not work (used to set title on dialog!):
      form.setText(title);

      //effortBased?
      form.findComponent("EffortBasedPlanning").setBooleanValue(effortBased);

      //if availability should be checked for added resources
      form.findComponent("CheckAvailability").setBooleanValue(checkAvailability);

      //fill the project edit mode
      form.findComponent("EditModeField").setBooleanValue(edit_mode);

      //fill the project edit mode
      form.findComponent("ActionsEditable").setBooleanValue(actionsEditable);

      form.findComponent("CallingForm").setValue(parameters.get("callingForm"));
      form.findComponent("ActivityRowIndex").setIntValue(((Integer)parameters.get("activityRowIndex")).intValue());
      // FIXME(dfreis Sep 17, 2008 1:51:02 PM) may not work 
      // Fill ActivityRowIndex field
      //row_index = selected_indices[0];
      //form.findComponent("ActivityRowIndex").setValue(row_index);

      //name
      setField("Name", activity.getName(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
      //description
      setField("Description", activity.getDescription(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
      //completed
      XComponent completed_field = form.findComponent("Complete");
      setField("Complete", activity.getComplete(), form, hiddenFieldIds, disabledFieldsIds, edit_mode && completed_field.getEnabled());

      //start
      setField("Start", activity.getStart(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);

      //end
      setField("End", activity.getFinish(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);

      //leadTime
      setField("LeadTime", activity.getLeadTime(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);

      //followUpTime
      setField("FollowUpTime", activity.getFollowUpTime(),form, hiddenFieldIds, disabledFieldsIds, edit_mode);

      // start fixed
      setField("StartFixed", activity.hasAttribute(OpActivityIfc.START_IS_FIXED), form, hiddenFieldIds, disabledFieldsIds, edit_mode);

      // finish fixed
      setField("FinishFixed", activity.hasAttribute(OpActivityIfc.FINISH_IS_FIXED), form, hiddenFieldIds, disabledFieldsIds, edit_mode);

      // FIXME(dfreis Sep 17, 2008 3:17:12 PM) duration is wrong !!! (not converted via XCalendar.convertDurationFromUnit(..))
      //duration
      double duration = activity.getDuration();
      duration = convertDuration(form, duration);
      setField("Duration", duration, form, hiddenFieldIds, disabledFieldsIds, edit_mode);

      //effort
      setField("BaseEffort", activity.getBaseEffort(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
      setField("BaseEffortSum", activity.getBaseEffort(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);

      XComponent category_field = form.findComponent("Category");
      OpActivityCategory category = activity.getCategory();
      String categoryName;
      if (category != null) {
         category_field.setStringValue(category.getName());
         ((XComponent)category_field.getChild(0)).setStringValue(category.getName());
      }
      else {
         XComponent activityCategoryDataSet = (XComponent) form.findComponent("ActivityCategoryDataSet");
         if (activityCategoryDataSet.getChildCount() > 0) {
            String noCategory = ((XComponent) form.findComponent("ActivityCategoryDataSet").getChild(0)).getStringValue();
            category_field.setStringValue(noCategory);
            ((XComponent) category_field.getChild(0)).setStringValue(noCategory);
         }
      }
      setFieldEditMode(edit_mode, category_field, disabledFieldsIds);

      //priority
      int priority = activity.getPriority();
      setField("Priority", priority, form, hiddenFieldIds, disabledFieldsIds, edit_mode);
 
      //mandatory (from attributes)
      setField("Mandatory", activity.hasAttribute(OpGanttValidator.MANDATORY), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
      
      //payment
      setField("Payment", activity.getPayment(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
      
      // exported
      setField("Exported", activity.hasAttribute(activity.EXPORTED_TO_SUPERPROJECT), form, hiddenFieldIds, disabledFieldsIds, edit_mode);

      //Fill project assignment set & responsible resource
//      assignmentsSet = XDisplay.getActiveForm().findComponent("AssignmentSet");
      Set<? extends OpAssignmentIfc> assignmentsSet = activity.getAssignments();
      XComponent responsibleSet = form.findComponent("ResponsibleResourceSet");
      if (assignmentsSet != null) {
         for (OpAssignmentIfc assignment : assignmentsSet) {
            XComponent resource = OpActivityDataSetFactory.createResourceRow(assignment.getResource());
            responsibleSet.addChild(resource);
         }
      }

      XComponent resource_field = form.findComponent("ResponsibleResource");
      if (!shouldHideField(resource_field, hiddenFieldIds, form)) {
         OpResource resource = activity.getResponsibleResource();
         if (resource != null) {
            String resourceLocator = XValidator.choice(resource.locator(), resource.getName());
            resource_field.setStringValue(resourceLocator);
            ((XComponent)resource_field.getChild(0)).setStringValue(resourceLocator);
         }
         else {
            String noResponsible = ((XComponent)responsibleSet.getChild(0)).getStringValue();
            resource_field.setStringValue(noResponsible);
            ((XComponent)resource_field.getChild(0)).setStringValue(noResponsible);
         }
         boolean showField = (edit_mode && (responsibleSet.getChildCount() > 1));
         setFieldEditMode(showField, resource_field, disabledFieldsIds);
      }

      //resources tab
      XComponent resource_tab = form.findComponent("ResourcesTab");
      if (!shouldHideTab(resource_tab, hiddenTabsIds, form)) {
         // Fill resources tab: Reuse resource data-set from activities form
         XComponent resource_data_set = new XComponent(XComponent.DATA_SET);
         for (OpAssignmentIfc assignment : activity.getAssignments()) {
            XComponent assRow = OpActivityDataSetFactory.createResourceRow(assignment.getResource());
            resource_data_set.addChild(assRow);
         }
         form.findComponent("ResourceColumn").setDataSetComponent(resource_data_set);
         form.findComponent("AssignmentTable").setEditMode(edit_mode);
         form.findComponent("AssignmentTable").setEnabled(edit_mode);

         XComponent assignment_set = form.findComponent("AssignmentSet");
         XComponent assignment_cell;
         XComponent assignment_row;
         for (OpAssignmentIfc assignment : activity.getAssignments()) {
            OpResource resource = assignment.getResource();
            assignment_row = assignment_set.newDataRow();
            assignment_cell = XComponent.newDataCell();
            assignment_cell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
            assignment_row.addChild(assignment_cell);
            assignment_cell = XComponent.newDataCell();
            
            assignment_cell.setDoubleValue(assignment.getAssigned());
            assignment_cell.setEditMode(edit_mode);
            assignment_cell.setEnabled(edit_mode);
            assignment_row.addChild(assignment_cell);

            XComponent assignment_h_cell = XComponent.newDataCell();
            
//            Double assignmentH = null;
//            double effort = activity.getBaseEffort();
//            //if there is no duration (e.g tasks, use the effort)
//            if (duration == 0) {
//               if (effort != 0) {
//                  assignmentH = effort;
//               }
//            }
//            else {
//               if (effort == 0) {
//                  assignmentH = effort;
//               }
//               else {
//                  assignmentH = resource.getAvailable() * duration / 100.0; //assignment.getAssigned() * duration / 100.0;
//               }
//            }

//            assignment_h_cell.setDoubleValue(assignmentH);
            assignment_h_cell.setDoubleValue(assignment.getBaseEffort());
            assignment_h_cell.setEditMode(edit_mode);
            assignment_h_cell.setEnabled(edit_mode);
            assignment_row.addChild(assignment_h_cell);
            assignment_set.addDataRow(assignment_row);
         }
         
         // add/remove resource icon buttons visibility according to project edit mode
         XComponent add_icon_button = form.findComponent("ResourceAddIcon");
         boolean showButtons = (edit_mode && resource_data_set != null && (resource_data_set.getChildCount() > 0));
         setButtonVisibleMode(showButtons, add_icon_button);
         XComponent remove_icon_button = form.findComponent("ResourceRemoveIcon");
         setButtonVisibleMode(showButtons, remove_icon_button);
      }

      //fill the resource availability data set
      XComponent resourceAvailability = form.findComponent("ResourceAvailability");
      Map<String, Double> availabilityMap = OpResourceDataSetFactory.createResourceAvailabilityMap(broker); //one DB-query only!
      resourceAvailability.setValue(availabilityMap);

      //predecessor tab
      XComponent predecessor_tab = form.findComponent("PredecessorTab");
      if (!shouldHideTab(predecessor_tab, hiddenTabsIds, form)) {
         // Fill predecessors tab: Reuse activity data-set from activities form
         XComponent predecessor_name_column = form.findComponent("PredecessorNameColumn");
         XComponent activity_data_set = new XComponent(XComponent.DATA_SET);
         OpProjectPlanVersion workingPlanVersion = projectPlan.getWorkingVersion();  //one DB-query only
         getActivitiesFormProvider().fillActivityDataSet(form, activity_data_set, session, broker, projectPlan.getProjectNode(), 
               workingPlanVersion,  null, edit_mode);
         predecessor_name_column.setDataSetComponent(activity_data_set);

         // *** Create predecessor-set from choice-list-value (XArray of String/Choice)
         // ==> Should be quite similar to resources: Probably we can reuse much of the code
         XComponent predecessor_set = form.findComponent("PredecessorSet");
         for (OpDependencyIfc predecessorDependency : activity.getPredecessorDependencies()) {
            OpActivityIfc preAct = predecessorDependency.getPredecessorActivity();
            String predecessor_choice = XValidator.choice(Integer.toString(preAct.getSequence()), preAct.getName());
            XComponent predecessor_row = predecessor_set.newDataRow();
            XComponent predecessor_cell = XComponent.newDataCell();
            predecessor_cell.setStringValue(predecessor_choice);
            predecessor_row.addChild(predecessor_cell);

            predecessor_cell = XValidator.initChoiceDataCell(form.findComponent("LinkTypeSet"),
                  OpGanttValidator.getPredecessorTypeString(predecessorDependency.getDependencyType()));
            predecessor_cell.setEnabled(edit_mode_programm);
            predecessor_row.addChild(predecessor_cell);

            predecessor_set.addDataRow(predecessor_row);
         }
         // add/remove predecessor icon buttons visibility according to project edit mode
         XComponent add_icon_button = form.findComponent("PredecessorAddIcon");
         setButtonVisibleMode(edit_mode_programm, add_icon_button);
         XComponent remove_icon_button = form.findComponent("PredecessorRemoveIcon");
         setButtonVisibleMode(edit_mode_programm, remove_icon_button);
      }
      //workbreaks tab
      XComponent workBreakTabs = form.findComponent("WorkBreaksTab");
      if (!shouldHideTab(workBreakTabs, hiddenTabsIds, form)) {
         XComponent add_icon_button = form.findComponent("WorkBreakAddIcon");
         setButtonVisibleMode(edit_mode_programm, add_icon_button);
         XComponent remove_icon_button = form.findComponent("WorkBreakRemoveIcon");
         setButtonVisibleMode(edit_mode_programm, remove_icon_button);
      }

      //costs tab
      XComponent costsTab = form.findComponent("CostsTab");
      if (!shouldHideTab(costsTab, hiddenTabsIds, form)) {
         //personnel costs (edit mode false by default)
         setField("PersonnelCosts", activity.getBasePersonnelCosts(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
         setField("TravelCosts", activity.getBaseTravelCosts(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
         setField("MaterialCosts", activity.getBaseMaterialCosts(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
         setField("ExternalCosts", activity.getBaseExternalCosts(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
//         setField("CostsSum", activity.getBasePersonnelCosts()+activity.getBaseTravelCosts()+activity.getBaseMaterialCosts()+activity.getBaseExternalCosts(),form, hiddenFieldIds, disabledFieldsIds, edit_mode);
         setField("MiscellaneousCosts", activity.getBaseMiscellaneousCosts(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
         setField("ProceedsCosts", activity.getBaseProceeds(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
         setField("EffortBillable", activity.getEffortBillable(), form, hiddenFieldIds, disabledFieldsIds, edit_mode);
      }

      //attachments tab
      XComponent attachmentTab = form.findComponent("AttachmentTab");
      if (!shouldHideTab(attachmentTab, hiddenTabsIds, form)) {
         // Fill attachments tab
         XComponent attachment_set = form.findComponent("AttachmentSet");
         for (OpAttachmentIfc attachment : activity.getAttachments()) {
            XComponent attachmentDataRow = attachment_set.newDataRow();
            //0 - type "u" or "d" identifier
            XComponent dc = XComponent.newDataCell();
            dc.setStringValue(attachment.getLinked() ? "u" : "d");
            attachmentDataRow.addChild(dc);

            //1 - choice [name, id]
            dc = XComponent.newDataCell();
            dc.setStringValue(XValidator.choice(attachment.locator(), attachment.getName(), (attachment.getLinked() ? 0 : 1)));
            attachmentDataRow.addChild(dc);

            //2 - location
            dc = XComponent.newDataCell();
            dc.setStringValue(attachment.getLocation());
            attachmentDataRow.addChild(dc);

            if (!attachment.getLinked()) {
               //3 - contentId
               dc = XComponent.newDataCell();
               dc.setValue(attachment.getContent().locator());
               attachmentDataRow.addChild(dc);
            }

            attachment_set.addDataRow(attachmentDataRow);
            
         }

         // addURL/addDocument/remove/view attachements buttons visibility according to edit mode
         XComponent add_document_button = form.findComponent("AddDocumentButton");
         setButtonVisibleMode(edit_mode, add_document_button);
         XComponent add_url_button = form.findComponent("AddURLButton");
         setButtonVisibleMode(edit_mode, add_url_button);
         XComponent remove_button = form.findComponent("RemoveAttachmentButton");
         setButtonVisibleMode(edit_mode, remove_button);
         XComponent view_button = form.findComponent("ViewAttachmentButton");
         setButtonVisibleMode(edit_mode, view_button);
      }
//      boolean statusChangeable = form.findComponent(PROGRESS_TRACKED_FIELD).getBooleanValue(false);// projectPlan.getProgressTracked();  
      editActivityAdditional(session, broker, activity, form, parameters, edit_mode, actionsEditable, disabledFieldsIds);

      //cancel button visibility according to project edit mode
      XComponent cancel_button = form.findComponent("cancelButton");
      setButtonVisibleMode(edit_mode, cancel_button);
   }

   /**
    * @return
    * @pre
    * @post
    */
   protected OpActivitiesFormProvider getActivitiesFormProvider() {
      return new OpActivitiesFormProvider();
   }

   /**
    * @param form
    * @param duration
    * @return
    * @pre
    * @post
    */
   private double convertDuration(XComponent form, double duration) {
      //default HOUR
      byte durationUnit = 2; //Byte.parseByte("1");

//      if (XDisplay.getActiveForm().findComponent("ActivityTable") != null) {
//         //get the table header and from there get the duration column
//         durationColumn = XDisplay.getActiveForm().findComponent("ActivityTable").getChild(3).getChild(0).getChild(7);
//         if (durationColumn.getID() == "Duration") {
//            durationUnit = durationColumn.getDisplayUnit();
//         }
//         //if the parent form has an activity table but the table has no duration column: show the duration in days
//         else {
//            //DAYS
//            durationUnit = Byte.parseByte("2");
//         }
//      }
//      //if the parent form doesn't have the activity table always show the duration in days
//      else {
//         //DAYS
//         durationUnit = Byte.parseByte("2");
//      }

      return form.getComponentCalendar().convertDurationToUnit(duration, durationUnit);
   }

   /**
    * @param broker 
    * @param session 
    * @param activity
    * @param form
    * @param parameters
    * @param edit_mode2
    * @param disabledFieldsIds
    * @pre
    * @post
    */
   protected void editActivityAdditional(OpProjectSession session, OpBroker broker, OpActivityVersion activityVersion, XComponent form,
         HashMap parameters, boolean edit_mode, boolean statusChangeable, Set<String> disabledFieldsIds) {
      
   }

   /**
    * @param string
    * @param name
    * @param form
    * @param hiddenFieldIds
    * @param editMode 
    * @param disabledFieldsIds 
    * @pre
    * @post
    */
   private void setField(String fieldId, Object value, XComponent form,
         Set<String> hiddenFieldIds, Set<String> disabledFieldsIds, boolean editMode) {
      XComponent field = form.findComponent(fieldId);
      if (!shouldHideField(field, hiddenFieldIds, form)) {
         field.setValue(value);
         setFieldEditMode(editMode, field, disabledFieldsIds);
      }
   }

   /**
   * Makes a form field enabled or disabled, according to a flag and to a list of field ids which should be disabled.
   * @param editMode a boolean indicating whether the field should be enabled or not.
   * @param field a component of the edit activity form.
   * @param disabledFieldIds a list of component ids which should always be disabled.
   */
  protected static void setFieldEditMode(boolean editMode, XComponent field, Set<String> disabledFieldIds) {
     if (field == null) {
        return;
     }
     if (disabledFieldIds.contains(field.getID())) {
        field.setEnabled(false);
     }
     else {
        field.setEnabled(editMode);
     }
  }

   private static boolean shouldHideField(XComponent field, Set<String>hiddenFieldIds, XComponent form) {
      if (field == null) {
         logger.warn("Field not found!!");
         return true;
      }
      if (hiddenFieldIds.contains(field.getID())) {
         field.setVisible(false);
         String fieldLabelId = field.getID() + "Label";
         XComponent fieldLabel = form.findComponent(fieldLabelId);
         if (fieldLabel != null) {
            fieldLabel.setVisible(false);
         }
         return true;
      }
      return false;
   }

   /**
    * Checks for a given tab id, whether it should be visible or not.
    *
    * @param tabId a String representing the id of the tab (from activity.oxf.xml)
    * @param hiddenTabIds a List of tab ids which should be hidden.
    * @param form the edit activity form.
    * @retun true if the tab should be hidden, false otherwise.
    */
   private static boolean shouldHideTab(XComponent tab, Set<String>hiddenTabIds, XComponent form) {
      if (tab != null) {
         if (hiddenTabIds.contains(tab.getID())) {
            tab.setHidden(true);
            return true;
         }
      }
      return false;
   }



   /**
    * Adds to the curent form the comments panel information.
    *
    * @param form        Current form
    * @param activity    Activity to show the comments for
    * @param session     Current project session
    * @param broker      Broker instance for db access
    * @param resourceMap langauage resource map for comments
    * @param enabled     if true, panel is action enabled (remove/add comment)
    */
   public static void showComments(XComponent form, OpActivity activity, OpProjectSession session, OpBroker broker, XLanguageResourceMap resourceMap, boolean enabled) {
      // Show comments if activity is already persistent
      XComponent commentsLabel = form.findComponent(COMMENTS_LABEL);
      XComponent hasCommentsField = form.findComponent(HAS_COMMENTS_FIELD);

      // Ability to add and remove comments is only defined by access control list
      // (No edit-mode required in order to add comments -- reason: Contributor-access)
      byte accessLevel = OpPermission.OBSERVER;
      if (activity != null) {
         accessLevel = session.effectiveAccessLevel(broker, activity.getProjectPlan().getProjectNode().getId());
         XComponent commentsPanel = form.findComponent(COMMENTS_PANEL);
         boolean removeEnabled = (accessLevel >= OpPermission.ADMINISTRATOR) && enabled;
         int commentCount = addComments(session, broker, activity, commentsPanel, resourceMap, removeEnabled);
         hasCommentsField.setBooleanValue(commentCount > 0);
         StringBuffer commentsBuffer = new StringBuffer();
         commentsBuffer.append(commentCount);
         commentsBuffer.append(' ');
         if (commentCount == 1) {
            commentsBuffer.append(resourceMap.getResource(COMMENT_SO_FAR).getText());
         }
         else {
            commentsBuffer.append(resourceMap.getResource(COMMENTS_SO_FAR).getText());
         }
         commentsLabel.setText(commentsBuffer.toString());
      }
      else {
         commentsLabel.setText(resourceMap.getResource(NO_COMMENTS).getText());
         hasCommentsField.setBooleanValue(false);
      }

      XComponent addCommentButton = form.findComponent(ADD_COMMENT_BUTTON);
      // check for enable add comment button
      if (accessLevel >= OpPermission.CONTRIBUTOR && enabled) {
         addCommentButton.setEnabled(true);
      }
   }

   protected void addCategories(OpBroker broker, XComponent categoryChooser, XComponent dataSet, XLanguageResourceMap resourceMap) {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      String noCategory = resourceMap.getResource(NO_CATEGORY_RESOURCE).getText();
      dataRow.setStringValue(XValidator.choice(OpGanttValidator.NO_CATEGORY_ID, noCategory));
      dataSet.addChild(dataRow);
      categoryChooser.setEnabled(false);
   }

   private static int addComments(OpProjectSession session, OpBroker broker, OpActivity activity, XComponent commentsPanel, XLanguageResourceMap resourceMap, boolean enableCommentRemoving) {

      OpQuery query = broker.newQuery("select comment, creator.DisplayName from OpActivityComment as comment inner join comment.Creator as creator where comment.Activity.id = ? order by comment.Sequence");

      query.setLong(0, activity.getId());
      Iterator result = broker.iterate(query);
      Object[] record;
      int count = 0;
      OpActivityComment comment;
      XComponent commentPanel;

      //use localizer to localize name of administrator
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionDataSetFactory.USER_OBJECTS));

      while (result.hasNext()) {
         record = (Object[]) result.next();
         comment = (OpActivityComment) record[0];
         commentPanel = createPanel(comment, resourceMap, localizer, enableCommentRemoving, session.getCalendar());
         commentsPanel.addChild(commentPanel);
         count++;
      }
      return count;
   }

   /**
    * Creates a comment panel with the given information.
    *
    * @param comment               comment entity the panel is created for
    * @param resourceMap           language resource map
    * @param localizer             localizer used for the name of the comment creator
    * @param enableCommentRemoving enable/disable remove dialog button
    * @param calendar a <code>OpProjectCalendar</code> representing the client's calendar.
    * @return an <code>XComponent</code> representing the comment panel
    */
   public static XComponent createPanel(OpActivityComment comment, XLanguageResourceMap resourceMap, XLocalizer localizer, boolean enableCommentRemoving, OpProjectCalendar calendar) {
      XComponent commentPanel;
      StringBuffer subjectBuffer;
      XComponent subjectLabel;
      StringBuffer whoAndWhenBuffer;
      XComponent whoAndWhenLabel;
      XComponent textPanel;
      XComponent textBox;
      XComponent buttonPanel;
      XComponent removeButton;
      int count = comment.getSequence();
      String by = resourceMap.getResource("By").getText();
      String on = resourceMap.getResource("On").getText();
      String at = resourceMap.getResource("At").getText();
      commentPanel = new XComponent(XComponent.PANEL);
      commentPanel.setLayout("flow");
      commentPanel.setDirection(XComponent.SOUTH);
      commentPanel.setStyle(XComponent.DEFAULT_LAYOUT_PANEL_STYLE);

      // Subject in bold font
      subjectBuffer = new StringBuffer();
      subjectBuffer.append(comment.getSequence());
      subjectBuffer.append("   ");
      subjectBuffer.append(comment.getName());
      subjectLabel = new XComponent(XComponent.LABEL);
      subjectLabel.setStyle(XComponent.DEFAULT_LABEL_EMPHASIZED_LEFT_STYLE);
      subjectLabel.setText(subjectBuffer.toString());
      commentPanel.addChild(subjectLabel);
      // Creator display name, date and time
      whoAndWhenBuffer = new StringBuffer(by);
      whoAndWhenBuffer.append(' ');
      whoAndWhenBuffer.append(localizer.localize(comment.getCreator().getDisplayName()));
      whoAndWhenBuffer.append(' ');
      whoAndWhenBuffer.append(on);
      whoAndWhenBuffer.append(' ');
      whoAndWhenBuffer.append(calendar.localizedDateToString(new Date(comment.getCreated().getTime())));
      whoAndWhenBuffer.append(' ');
      whoAndWhenBuffer.append(at);
      whoAndWhenBuffer.append(' ');
      whoAndWhenBuffer.append(calendar.localizedTimeToString(new Date(comment.getCreated().getTime())));
      whoAndWhenLabel = new XComponent(XComponent.LABEL);
      whoAndWhenLabel.setStyle(XComponent.DEFAULT_LABEL_LEFT_STYLE);
      whoAndWhenLabel.setText(whoAndWhenBuffer.toString());
      commentPanel.addChild(whoAndWhenLabel);
      //text panel contains text box and remove button
      textPanel = new XComponent(XComponent.PANEL);
      textPanel.setDirection(XComponent.EAST);
      textPanel.setLayout("flow");
      textPanel.setStyle(XComponent.DEFAULT_LAYOUT_PANEL_STYLE);
      commentPanel.addChild(textPanel);
      // Actual comment (text)
      textBox = new XComponent(XComponent.TEXT_BOX);
      textBox.setEnabled(false);
      textBox.setRows(5);
      textBox.setColumns(50);
      textBox.setFlexible(true);
      textBox.setStringValue(comment.getText());
      textPanel.addChild(textBox);
      // Remove button panel
      buttonPanel = new XComponent(XComponent.PANEL);
      buttonPanel.setLayout("flow");
      buttonPanel.setDirection(XComponent.WEST);
      textPanel.addChild(buttonPanel);
      removeButton = new XComponent(XComponent.BUTTON);
      removeButton.setID("RemoveCommentButton_" + count);
      removeButton.setStringValue(OpLocator.locatorString(comment));
      removeButton.setIcon(REMOVE_COMMENT_ICON);
      removeButton.setStyle(XComponent.DEFAULT_ICON_BUTTON_STYLE);
      removeButton.setOnButtonPressed(REMOVE_COMMENT_ACTION);
      removeButton.setEnabled(enableCommentRemoving);
      removeButton.setStyle(REMOVE_COMMENT_BUTTON_STYLE_REF);
      buttonPanel.addChild(removeButton);
      return commentPanel;
   }

   protected static void setButtonVisibleMode(boolean visible, XComponent button) {
      if (button != null) {
         button.setVisible(visible);
      }
   }

}