/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.my_projects.forms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.project.modules.project_costs.OpProjectCostsDataSetFactory;
import onepoint.project.modules.project_resources.OpProjectResourceDataSetFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpSubjectDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

/**
 * Form provider for my projects tool.
 *
 * @author mihai.costin
 */
public class OpMyProjectsFormProvider implements XFormProvider {

   protected final static String PROJECTS_DATA_SET = "ProjectsSet";
   protected final static String GANTT_PROJECTS_DATA_SET = "GanttProjectsSet";
   private final static String ROLE_CHOICE_ID = "role_choice_id";
   private final static String ROLE_CHOICE_FIELD = "RoleChooser";
   private final static String ROLE_PANEL = "RolePanel";
   private final static String PRINT_BUTTON = "PrintButton";
   private final static int DEFAULT_PROJECT_CHOICE_FIELD_INDEX = 0;

   private final static String COSTS_TAB = "ProjectCostsTab";
   private final static String COSTS_COLUMN = "CostsColumn";

   //project choice values
   private final static String OBSERVER = "observer";
   private final static String CONTRIB = "contrib";
   private final static String MANAGER = "manager";
   private final static String DEFAULT_VALUE = CONTRIB;

   protected final int PROJECT_NAME_INDEX = 0;
   protected final int BASE_EFFORT_INDEX = 3;
   protected final int BASE_COST_INDEX = 5;
   protected final int PREDICTED_COSTS_INDEX = 7;
   protected final int PREDICTED_EFFORT_INDEX = 10;
   protected final int COMPLETED_INDEX = 2;

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      XComponent projectsDataSet;
      XComponent ganttProjectsDataSet = form.findComponent(GANTT_PROJECTS_DATA_SET);
      projectsDataSet = form.findComponent(PROJECTS_DATA_SET);
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      try {
         // hide multi-user components
         if (!OpEnvironmentManager.isMultiUser()) {
            form.findComponent(ROLE_PANEL).setVisible(false);
         }

         String projectChoice = getProjectChoice(parameters, form, session);
         List<Byte> levels = getLevelsForChoice(projectChoice);
         Set<OpProjectNode> projectNodes = getProjects(session, broker, levels);

         //projectMap = new HashMap();
         for (OpProjectNode projectNode: projectNodes) {
            XComponent row = createProjectRow(session, projectNode, broker);
            projectsDataSet.addChild(row);

            XComponent ganntDataRow = OpProjectDataSetFactory.createProjectNodeAdvancedRow(session, broker, projectNode, new XLocalizer(), true, 0);
            ganttProjectsDataSet.addChild(ganntDataRow);
         }

         //sort by name
         projectsDataSet.sort(PROJECT_NAME_INDEX);

         //enable the print button if there are any projects
         if (projectsDataSet.getChildCount() > 0) {
            form.findComponent(PRINT_BUTTON).setEnabled(true);
         }

         //hide costs tab and costs column for users that have only the customer level or
         //if the app. is multiuser and hide manager features is set to true and the user is not manager
         OpUser user = session.user(broker);
         if (OpSubjectDataSetFactory.shouldHideFromUser(session, user) || user.getLevel() == OpUser.OBSERVER_CUSTOMER_USER_LEVEL) {
            form.findComponent(COSTS_TAB).setHidden(true);
            form.findComponent(COSTS_COLUMN).setHidden(true);

         }
      }
      finally {
         broker.close();
      }
   }
   
   protected Set<OpProjectNode> getProjects(OpProjectSession session, OpBroker broker, Collection<Byte> accessLevels) {
      return OpProjectDataSetFactory.getProjectsByPermissions(session, broker, accessLevels);
   }

   private List getLevelsForChoice(String permission) {
      List<Byte> levels = new ArrayList<Byte>();
      if (OBSERVER.equals(permission)) {
         //show all projects the user has at least read access to
         levels.add(OpPermission.OBSERVER);
         levels.add(OpPermission.CONTRIBUTOR);
         levels.add(OpPermission.MANAGER);
         levels.add(OpPermission.ADMINISTRATOR);
      }
      else if (CONTRIB.equals(permission)) {
         //show projects the user has at least contributor permissions to
         levels.add(OpPermission.CONTRIBUTOR);
         levels.add(OpPermission.MANAGER);
         levels.add(OpPermission.ADMINISTRATOR);
      }
      else if (MANAGER.equals(permission)) {
         //show projects the user has at least manager permissions to
         levels.add(OpPermission.MANAGER);
         levels.add(OpPermission.ADMINISTRATOR);
      }
      return levels;
   }

   /**
    * Gets the project permission choice from the PROJECT_CHOICE_FIELD component.
    *
    * @param parameters form parameters
    * @param form       the current form
    * @param session    the project session
    * @return project permission choice
    */
   private String getProjectChoice(HashMap parameters, XComponent form, OpProjectSession session) {
      String roleChoice = (String) parameters.get(ROLE_CHOICE_ID);
      if (roleChoice == null) {
         //set default value
         XComponent chooser = form.findComponent(ROLE_CHOICE_FIELD);
         Map stateMap = session.getComponentStateMap(form.getID());
         int selectedIndex;
         if (stateMap != null) {
            Integer state = (Integer) stateMap.get(ROLE_CHOICE_FIELD);
            if (state != null) {
               selectedIndex = state;
               String value = (String) ((XComponent) chooser.getDataSetComponent().getChild(selectedIndex)).getValue();
               roleChoice = XValidator.choiceID(value);
            }
            else {
               roleChoice = DEFAULT_VALUE;
               selectedIndex = DEFAULT_PROJECT_CHOICE_FIELD_INDEX;
            }
         }
         else {
            roleChoice = DEFAULT_VALUE;
            selectedIndex = DEFAULT_PROJECT_CHOICE_FIELD_INDEX;
         }
         chooser.setSelectedIndex(selectedIndex);
      }
      return roleChoice;
   }  

   /**
    * Creates a my-project dataRow for a given project node.
    *
    * @param projectNode Project Node to create the row for.
    * @param broker      broker use for db operations.
    * @return the resulting data row.
    */
   private XComponent createProjectRow(OpProjectSession session, OpProjectNode projectNode, OpBroker broker) {

      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell;

      ArrayList<Byte> activityTypes = new ArrayList<Byte>();
      activityTypes.add(OpActivity.STANDARD);
      activityTypes.add(OpActivity.COLLECTION);

      double complete = OpProjectDataSetFactory.getCompletedValue(broker, projectNode.getId(), activityTypes);

      XComponent costDataSet = new XComponent(XComponent.DATA_SET);
      OpProjectCostsDataSetFactory.fillCostsDataSet(session, broker, projectNode, 0, costDataSet, null);

      XComponent effortDataSet = new XComponent(XComponent.DATA_SET);
      OpProjectResourceDataSetFactory.fillEffortDataSet(session, broker, projectNode, 0, effortDataSet, false);

      //project locator
      dataRow.setStringValue(projectNode.locator());

      //project name  0
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(projectNode.getName());
      dataRow.addChild(dataCell);
      //project status  1
      dataCell = new XComponent(XComponent.DATA_CELL);
      OpProjectStatus projectStatus = projectNode.getStatus();
      if (projectStatus != null) {
         dataCell.setStringValue(projectStatus.getName());
      }
      else {
         dataCell.setStringValue(null);
      }
      dataRow.addChild(dataCell);
      //% complete   2
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(complete);
      dataRow.addChild(dataCell);
      //base effort  3
      dataCell = new XComponent(XComponent.DATA_CELL);
      double baseEffort = effortDataSet.calculateDoubleSum(OpProjectResourceDataSetFactory.BASE_COLUMN_INDEX, 0);
      dataCell.setDoubleValue(baseEffort);
      dataRow.addChild(dataCell);
      //actual effort 4
      dataCell = new XComponent(XComponent.DATA_CELL);
      double actualEffort = effortDataSet.calculateDoubleSum(OpProjectResourceDataSetFactory.ACTUAL_COLUMN_INDEX, 0);
      dataCell.setDoubleValue(actualEffort);
      dataRow.addChild(dataCell);
      //base costs  5
      dataCell = new XComponent(XComponent.DATA_CELL);
      double baseCost = costDataSet.calculateDoubleSum(OpProjectCostsDataSetFactory.BASE_COLUMN_INDEX, 0);
      dataCell.setDoubleValue(baseCost);
      dataRow.addChild(dataCell);
      //actual costs  6
      dataCell = new XComponent(XComponent.DATA_CELL);
      double actualCost = costDataSet.calculateDoubleSum(OpProjectCostsDataSetFactory.ACTUAL_COLUMN_INDEX, 0);
      dataCell.setDoubleValue(actualCost);
      dataRow.addChild(dataCell);

      //predicted costs 7
      dataCell = new XComponent(XComponent.DATA_CELL);
      double predictedCost = costDataSet.calculateDoubleSum(OpProjectCostsDataSetFactory.PREDICTED_COLUMN_INDEX, 0);
      dataCell.setDoubleValue(predictedCost);
      dataRow.addChild(dataCell);
      //costs deviation 8
      dataCell = new XComponent(XComponent.DATA_CELL);
      double deviationCost = costDataSet.calculateDoubleSum(OpProjectCostsDataSetFactory.DEVIATION_COLUMN_INDEX, 0);
      dataCell.setDoubleValue(deviationCost);
      dataRow.addChild(dataCell);
      //costs %deviation 9
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(OpActivityDataSetFactory.calculatePercentDeviation(baseCost, deviationCost));
      dataRow.addChild(dataCell);

      //predicted effort 10
      dataCell = new XComponent(XComponent.DATA_CELL);
      double predictedEffort = effortDataSet.calculateDoubleSum(OpProjectResourceDataSetFactory.PREDICTED_COLUMN_INDEX, 0);
      dataCell.setDoubleValue(predictedEffort);
      dataRow.addChild(dataCell);
      //effort deviation 11
      dataCell = new XComponent(XComponent.DATA_CELL);
      double deviationEffort = effortDataSet.calculateDoubleSum(OpProjectResourceDataSetFactory.DEVIATION_COLUMN_INDEX, 0);
      dataCell.setDoubleValue(deviationEffort);
      dataRow.addChild(dataCell);
      //effort %deviation 12
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(OpActivityDataSetFactory.calculatePercentDeviation(baseEffort, deviationEffort));
      dataRow.addChild(dataCell);

      //description 13
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(projectNode.getDescription());
      dataRow.addChild(dataCell);

      //start & finish  14 & 15
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDateValue(projectNode.getStart());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDateValue(projectNode.getFinish());
      dataRow.addChild(dataCell);

      double resources = OpProjectDataSetFactory.getResourcesValue(broker, projectNode.getId(), activityTypes);
      double costs = OpProjectDataSetFactory.getCostsValue(broker, projectNode.getId(), activityTypes);
      //resources  16
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(resources);
      dataRow.addChild(dataCell);
      //resources 17
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(costs);
      dataRow.addChild(dataCell);

      //remaining effort (base - actual)  18
      double remainingEffort = baseEffort - actualEffort;
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(remainingEffort);
      dataRow.addChild(dataCell);

      //remaining cost (base - actual)  19
      double remainingCost = baseCost - actualCost;
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(remainingCost);
      dataRow.addChild(dataCell);

      //priority 20
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(projectNode.getPriority());
      dataRow.addChild(dataCell);

      return dataRow;
   }
}
