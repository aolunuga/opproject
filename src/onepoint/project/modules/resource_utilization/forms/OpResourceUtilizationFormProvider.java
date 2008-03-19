/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization.forms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.express.XComponent;
import onepoint.express.XStyle;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpLocator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationDataSetFactory;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationService;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

/**
 * Form provider class for the resource utilization diagram.
 */
public class OpResourceUtilizationFormProvider implements XFormProvider {

   /**
    * Form constants
    */
   private final static String UTILIZATION_DATA_SET = "UtilizationDataSet";
   private final static String UTILIZATION_LEGEND_DATA_SET = "UtilizationResourceColorSet";
   private final static String PRINT_BUTTON = "PrintButton";

   private final static String RESOURCE_MAP = "resource_utilization.overview";
   private final static String HIGHLY_UNDERUSED = "HighlyUnderused";
   private final static String UNDERUSED = "Underused";
   private final static String NORMALUSE = "Normalused";
   private final static String OVERUSED = "Overused";
   private final static String HIGHLY_OVERUSED = "HighlyOverused";
   private final static String POOL_SELECTOR = "poolColumnsSelector";
   private final static String RESOURCE_SELECTOR = "resourceColumnsSelector";
   private final static Integer DEFAULT_PROBABILITY = 0;
   private final static String PROBABILITY_CHOOSER = "ProbabilityChooser";
   private final static String PROBABILITY_SET = "ProbabilitySet";
   private final static String PROBABILITY_FIELD = "projectProbability";
   private final static String PROJECT_SET = "ProjectSet";
   private static final Object PROJECT_CHOOSER = "ProjectChooser";
   private static final String PROJECT_FIELD = "project";
   private final static String FILTERED_OUT_LOCATORS = "FilteredOutIds";


   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      //prepare the utilization legend data set
      XLanguageResourceMap map = session.getLocale().getResourceMap(RESOURCE_MAP);
      XComponent legendDataSet = form.findComponent(UTILIZATION_LEGEND_DATA_SET);
      this.fillLegendDataSet(map, legendDataSet);
      Map<String, List<String>> projectsResourcesMap = OpProjectDataSetFactory.getProjectToResourceMap(session);
      fillProjectFilter(form, projectsResourcesMap);

      //get probability filter value
      int projectProbability = getProbability(form, parameters, session);
      OpLocator project = getProject(form, parameters, session);
      long projectId = project == null ? -1 : project.getID();
      Set<String> resourcesFilter = null;
      if (project != null) {
         resourcesFilter = new HashSet<String>();
         Map.Entry<String, List<String>> toRemove = null;
         // add all resources
         for (Map.Entry<String, List<String>> entry : projectsResourcesMap.entrySet()) {
            if (project.equals(OpLocator.parseLocator(XValidator.choiceID(entry.getKey())))) {
               toRemove = entry;
            }
            else {
               for (String value : entry.getValue()) {
                  resourcesFilter.add(XValidator.choiceID(value));
               }
            }
         }
         // remove the once within the selected project
         if (toRemove != null) {
            for (String value : toRemove.getValue()) {
               resourcesFilter.remove(XValidator.choiceID(value));
            }
         }
      }
      form.findComponent(FILTERED_OUT_LOCATORS).setValue(resourcesFilter);
      //fill resources
      Map poolColumnsSelector = this.createPoolColumnsSelector();
      form.findComponent(POOL_SELECTOR).setValue(poolColumnsSelector);
      Map resourceColumnsSelector = this.createResourceColumsSelector();
      form.findComponent(RESOURCE_SELECTOR).setValue(resourceColumnsSelector);
      XComponent dataSet = form.findComponent(UTILIZATION_DATA_SET);
      OpResourceDataSetFactory.retrieveFirstLevelsResourceDataSet(session, dataSet, poolColumnsSelector, resourceColumnsSelector, resourcesFilter);

      //fill the actual utilization values
      OpResourceUtilizationDataSetFactory.fillUtilizationValues(session, dataSet, null, projectProbability, projectId);

      //if we have at least 1 resource/pool, enable print button
      if (OpResourceUtilizationDataSetFactory.getUtilizationMap(session, dataSet, projectProbability, projectId).size() > 0) {
         form.findComponent(PRINT_BUTTON).setEnabled(true);
      }
   }

   /**
    * Returns the project probability filter value.
    *
    * @param form       current form
    * @param parameters form parameters
    * @param session    current session
    * @return int representing the project probability filter
    */
   private OpLocator getProject(XComponent form, HashMap parameters, OpProjectSession session) {
//      long project = -1;
      String projectChoice = (String) parameters.get(OpResourceUtilizationService.PROJECT_CHOICE_ID);
      OpLocator project = null;
      if (projectChoice != null) {
         OpLocator locator = OpLocator.parseLocator(projectChoice);
         if (locator != null) { // might be null eg: All
            project = OpLocator.parseLocator(projectChoice);
         }
      }
      else {
         //if no param given to form check for state
         HashMap componentStateMap = session.getComponentStateMap(form.getID());
         if (componentStateMap != null) {
            Integer index = (Integer) componentStateMap.get(PROJECT_CHOOSER);
            if (index != null) {
               XComponent dataSet = form.findComponent(PROJECT_SET);
               XComponent row = (XComponent) dataSet.getChild(index);
               project = OpLocator.parseLocator(XValidator.choiceID(row.getStringValue()));
            }
         }
      }
      form.findComponent(PROJECT_FIELD).setStringValue(project == null ? null : project.toString());
      return project;
   }

   /**
    * Returns the project probability filter value.
    *
    * @param form       current form
    * @param parameters form parameters
    * @param session    current session
    * @return int representing the project probability filter
    */
   private int getProbability(XComponent form, HashMap parameters, OpProjectSession session) {
      int projectProbability = DEFAULT_PROBABILITY;
      String probabilityChoice = (String) parameters.get(OpResourceUtilizationService.PROBABILITY_CHOICE_ID);
      if (probabilityChoice != null) {
         projectProbability = Integer.valueOf(probabilityChoice);
      }
      else {
         //if no param given to form check for state
         HashMap componentStateMap = session.getComponentStateMap(form.getID());
         if (componentStateMap != null) {
            Integer index = (Integer) componentStateMap.get(PROBABILITY_CHOOSER);
            if (index != null) {
               XComponent dataSet = form.findComponent(PROBABILITY_SET);
               XComponent row = (XComponent) dataSet.getChild(index);
               String id = XValidator.choiceID(row.getStringValue());
               projectProbability = Integer.valueOf(id);
            }
         }
      }
      form.findComponent(PROBABILITY_FIELD).setStringValue(String.valueOf(projectProbability));
      return projectProbability;
   }

   /**
    * Creates a map used for retrieving resource columns.
    *
    * @return a <code>Map(Integer, Integer)</code> containing data-row and field indexes.
    */
   private Map<Integer, Integer> createResourceColumsSelector() {
      Map<Integer, Integer> resourceColumnsSelector = new HashMap<Integer, Integer>();
      Integer index = new Integer(OpProjectComponent.UTILIZATION_DESCRIPTOR_COLUMN_INDEX);
      resourceColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.DESCRIPTOR));
      index = new Integer(OpProjectComponent.UTILIZATION_NAME_COLUMN_INDEX);
      resourceColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.NAME));
      index = new Integer(OpProjectComponent.UTILIZATION_AVAILABLE_COLUMN_INDEX);
      resourceColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.AVAILABLE));
      index = new Integer(OpProjectComponent.UTILIZATION_ROW_ID);
      resourceColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.ID));
      return resourceColumnsSelector;
   }

   /**
    * Creates a map used for retrieving pool columns.
    *
    * @return a <code>Map(Integer, Integer)</code> containing data-row and field indexes.
    */
   private Map<Integer, Integer> createPoolColumnsSelector() {
      Map<Integer, Integer> poolColumnsSelector = new HashMap<Integer, Integer>();
      Integer index = new Integer(OpProjectComponent.UTILIZATION_DESCRIPTOR_COLUMN_INDEX);
      poolColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.DESCRIPTOR));
      index = new Integer(OpProjectComponent.UTILIZATION_NAME_COLUMN_INDEX);
      poolColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.NAME));
      index = new Integer(OpProjectComponent.UTILIZATION_ROW_ID);
      //needed because we want for the pools and resources to have the same nr. of cells
      poolColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.NULL));
      return poolColumnsSelector;
   }

   /**
    * Fills the languge data set of the resource utilization.
    *
    * @param map           a <code>XLanguageResourceMap</code> used for i18n.
    * @param legendDataSet a <code>XComponent(DATA_SET)</code>  the utilization legend.
    */
   private void fillLegendDataSet(XLanguageResourceMap map, XComponent legendDataSet) {
      //HIGHLY_UNDERUSED -> BACKGROUND
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(HIGHLY_UNDERUSED).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(OpProjectComponent.DEFAULT_UTILIZATION_ROW_STYLE_ATTRIBUTES.alternate_background);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);

      //UNDERUSED -> BLUE
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(UNDERUSED).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.DEFAULT_BLUE);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);

      //NORMALUSE -> GREEN
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(NORMALUSE).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.DEFAULT_GREEN);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);

      //OVERUSED -> ORANGE
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(OVERUSED).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.DEFAULT_ORANGE);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);

      //HIGHLY_OVERUSED -> RED
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(HIGHLY_OVERUSED).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.DEFAULT_RED);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);
   }
   
   /**
    * Fills up the project filter data set.
    *
    * @param projectsResourcesMap a <code>Map</code> containing all the project where the current user is at least observer and the list of resources on these projects.
    * @param form                 a <code>XComponent(FORM)</code> representing the current form.
    */
   private void fillProjectFilter(XComponent form, Map<String, List<String>> projectsResourcesMap) {
      XComponent projectDataSet = form.findComponent(PROJECT_SET);

      //add projects that are only for adhoc tasks
      for (String projectChoice : projectsResourcesMap.keySet()) {
         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(projectChoice);
         projectDataSet.addChild(row);
      }
   }

}
