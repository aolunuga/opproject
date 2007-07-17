/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.my_projects.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project_costs.OpProjectCostsDataSetFactory;
import onepoint.project.modules.project_resources.OpProjectResourceDataSetFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Form provider for my projects tool.
 *
 * @author mihai.costin
 */
public class OpMyProjectsFormProvider implements XFormProvider {

   protected final static String PROJECTS_DATA_SET = "ProjectsSet";
   private final static String PROJECT_CHOICE_ID = "project_choice_id";
   private final static String PROJECT_CHOICE_FIELD = "ProjectChooser";
   private final static String ROLE_PANEL = "RolePanel";
   private final static String PRINT_BUTTON = "PrintButton";
   private final static int DEFAULT_PROJECT_CHOICE_FIELD_INDEX = 0;

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
      projectsDataSet = form.findComponent(PROJECTS_DATA_SET);
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // hide multi-user components
      if (!OpEnvironmentManager.isMultiUser()) {
         form.findComponent(ROLE_PANEL).setVisible(false);
      }

      String projectChoice = getProjectChoice(parameters, form, session);
      List levels = getLevelsForChoice(projectChoice);
      List projectNodeIDs = OpProjectDataSetFactory.getProjectsByPermissions(session, broker, levels);

      //projectMap = new HashMap();
      for (Object projectNodeID : projectNodeIDs) {
         Long id = (Long) projectNodeID;
         OpProjectNode projectNode = (OpProjectNode) broker.getObject(OpProjectNode.class, id);
         XComponent row = createProjectRow(projectNode, broker);
         projectsDataSet.addChild(row);
      }

      //sort by name
      projectsDataSet.sort(PROJECT_NAME_INDEX);

      //enable the print button if there are any projects
      if (projectsDataSet.getChildCount() > 0) {
         form.findComponent(PRINT_BUTTON).setEnabled(true);
      }

      broker.close();
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
      String projectChoice = (String) parameters.get(PROJECT_CHOICE_ID);
      if (projectChoice == null) {
         //set default value
         XComponent chooser = form.findComponent(PROJECT_CHOICE_FIELD);
         Map stateMap = session.getComponentStateMap(form.getID());
         int selectedIndex;
         if (stateMap != null) {
            Integer state = (Integer) stateMap.get(PROJECT_CHOICE_FIELD);
            if (state != null) {
               selectedIndex = state;
               String value = (String) ((XComponent) chooser.getDataSetComponent().getChild(selectedIndex)).getValue();
               projectChoice = XValidator.choiceID(value);
            }
            else {
               projectChoice = DEFAULT_VALUE;
               selectedIndex = DEFAULT_PROJECT_CHOICE_FIELD_INDEX;
            }
         }
         else {
            projectChoice = DEFAULT_VALUE;
            selectedIndex = DEFAULT_PROJECT_CHOICE_FIELD_INDEX;
         }
         chooser.setSelectedIndex(selectedIndex);
      }
      return projectChoice;
   }  

   /**
    * Creates a my-project dataRow for a given project node.
    *
    * @param projectNode Project Node to create the row for.
    * @param broker      broker use for db operations.
    * @return the resulting data row.
    */
   private XComponent createProjectRow(OpProjectNode projectNode, OpBroker broker) {

      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell;

      ArrayList<Byte> activityTypes = new ArrayList<Byte>();
      activityTypes.add(OpActivity.STANDARD);
      activityTypes.add(OpActivity.COLLECTION);

      double complete = OpProjectDataSetFactory.getCompletedValue(broker, projectNode.getID(), activityTypes);

      XComponent costDataSet = new XComponent(XComponent.DATA_SET);
      OpProjectCostsDataSetFactory.fillCostsDataSet(broker, projectNode, 0, costDataSet, null);

      XComponent effortDataSet = new XComponent(XComponent.DATA_SET);
      OpProjectResourceDataSetFactory.fillEffortDataSet(broker, projectNode, 0, effortDataSet);

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

      double resources = OpProjectDataSetFactory.getResourcesValue(broker, projectNode.getID(), activityTypes);
      double costs = OpProjectDataSetFactory.getCostsValue(broker, projectNode.getID(), activityTypes);
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
