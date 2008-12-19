/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization.forms;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.express.XComponent;
import onepoint.express.XStyle;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.custom_attribute.OpCustomType;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationDataSetFactory;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationService;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.util.OpProjectCalendar;
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
   private final static String ABSENCE = "Absence";
   private final static String POOL_SELECTOR = "poolColumnsSelector";
   private final static String RESOURCE_SELECTOR = "resourceColumnsSelector";
   private final static Integer DEFAULT_PROBABILITY = 0;
   private final static String PROBABILITY_CHOOSER = "ProbabilityChooser";
   private final static String PROBABILITY_SET = "ProbabilitySet";
   private final static String PROBABILITY_FIELD = "projectProbability";
   private final static String PROJECTTYPESET_ID = "ProjectTypeSet";
   private static final String PROJECTTYPECHOOSER_ID = "ProjectTypeChooser";
   private static final String PROJECTTYPE_FIELD = "projectType";
   private final static String FILTERED_OUT_LOCATORS = "FilteredOutIds";
   
   private static final String UNTYPEDCAPTION_ID = "UntypedCaption";
   
   private static final String UNTYPED_ID = "UNTYPED";
   private static final String ALLTYPES_ID = "ALL";


   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      //prepare the utilization legend data set
      XLanguageResourceMap map = session.getLocale().getResourceMap(RESOURCE_MAP);
      XComponent legendDataSet = form.findComponent(UTILIZATION_LEGEND_DATA_SET);
      OpBroker broker = session.newBroker();
      try {
         this.fillLegendDataSet(session, broker, map, legendDataSet);
         int projectProbability = getProbability(form, parameters, session);

         Map<String, List<String>> projectsResourcesMap = OpProjectDataSetFactory.getProjectToResourceMap(session, OpPermission.OBSERVER);
         
         setupProjectTypeChooser(session, broker, form, projectsResourcesMap);
   
         //get probability filter value
         Set<String> projects = getProjectsForSelectedType(session, broker, form, parameters);
         Set<String> usedResources = new HashSet<String>();
         
         Set<Long> projectPlanVersionIds = OpResourceUtilizationService.getPlanVersionIDsForProjectsAndUser(session, broker, projects, session.user(broker)); 

         for (String pLoc : projects) {
            Map.Entry<String, List<String>> toRemove = null;
            for (Map.Entry<String, List<String>> entry : projectsResourcesMap.entrySet()) {
               if (pLoc.equals(XValidator.choiceID(entry.getKey()))) {
                  for (String value : entry.getValue()) {
                     usedResources.add(XValidator.choiceID(value));
                  }
               }
            }
         }
         Set<String> invertedResources = new HashSet<String>();
         OpQuery allResources = broker.newQuery("select resource from OpResource as resource");
         Iterator<OpResource> rit = broker.iterate(allResources);
         while (rit.hasNext()) {
            OpResource r = rit.next();
            String rLoc = r.locator();
            if (!usedResources.contains(rLoc)) {
               invertedResources.add(rLoc);
            }
         }
         
         form.findComponent(FILTERED_OUT_LOCATORS).setValue(invertedResources);
         //fill resources
         Map poolColumnsSelector = this.createPoolColumnsSelector();
         form.findComponent(POOL_SELECTOR).setValue(poolColumnsSelector);
         Map resourceColumnsSelector = this.createResourceColumsSelector();
         form.findComponent(RESOURCE_SELECTOR).setValue(resourceColumnsSelector);
         XComponent dataSet = form.findComponent(UTILIZATION_DATA_SET);
         OpResourceDataSetFactory.retrieveFirstLevelsResourceDataSet(session,
               dataSet, poolColumnsSelector, resourceColumnsSelector,
               invertedResources, null, false);
   
         //fill the actual utilization values
         int numUtilizationEntries = OpResourceUtilizationDataSetFactory.getInstance().fillUtilizationValues(session, dataSet, null, projectProbability, projectPlanVersionIds);
   
         //if we have at least 1 resource/pool, enable print button
         if (numUtilizationEntries > 0) {
            form.findComponent(PRINT_BUTTON).setEnabled(true);
         }
      } 
      finally {
       broker.close();
      }
   }

   protected void setupProjectTypeChooser(OpProjectSession session,
         OpBroker broker, XComponent form,
         Map<String, List<String>> projectsResourcesMap) {
   }

   /**
    * Returns the project probability filter value.
    *
    * @param form       current form
    * @param parameters form parameters
    * @param session    current session
    * @return int representing the project probability filter
    */
   protected Set<String> getProjectsForSelectedType(OpProjectSession session, OpBroker broker, XComponent form, HashMap parameters) {
//      long project = -1;
      Set<String> projectLocators = getProjectLocatorsForCustomType(broker,
            null);
      
      return projectLocators;
   }

   public static Set<String> getProjectLocatorsForCustomType(OpBroker broker,
         String projectType) {
      OpQuery projectsQuery = null;
      if (projectType != null && !UNTYPED_ID.equals(projectType) && !ALLTYPES_ID.equals(projectType)) {
         OpCustomType type = (OpCustomType) broker.getObject(projectType);
         projectsQuery = broker.newQuery("from OpProjectNode as pn where pn.CustomType.id = :typeId");
         projectsQuery.setLong("typeId", type.getId());
      }
      else if (projectType != null && UNTYPED_ID.equals(projectType)) {
         OpCustomType type = (OpCustomType) broker.getObject(projectType);
         projectsQuery = broker.newQuery("from OpProjectNode as pn where pn.CustomType is null");
      }
      else {
         projectsQuery = broker.newQuery("from OpProjectNode as pn");
      }
      Set<String> projectLocators = new HashSet<String>();
      Iterator<OpProjectNode> pit = broker.iterate(projectsQuery);
      while (pit.hasNext()) {
         OpProjectNode pn = pit.next();
         projectLocators.add(pn.locator());
      }
      return projectLocators;
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
      index = new Integer(OpProjectComponent.UTILIZATION_ABSENCES_COLUMN_INDEX);
      resourceColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.NULL));
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
      //needed because we want for the pools and resources to have the same nr. of cells
      index = new Integer(OpProjectComponent.UTILIZATION_ROW_ID);
      poolColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.NULL));
      index = new Integer(OpProjectComponent.UTILIZATION_ABSENCES_COLUMN_INDEX);
      poolColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.NULL));
      return poolColumnsSelector;
   }

   /**
    * Fills the languge data set of the resource utilization.
 * @param broker 
 * @param session 
    *
    * @param map           a <code>XLanguageResourceMap</code> used for i18n.
    * @param legendDataSet a <code>XComponent(DATA_SET)</code>  the utilization legend.
    */
   private void fillLegendDataSet(OpProjectSession session, OpBroker broker, XLanguageResourceMap map, XComponent legendDataSet) {
      //HIGHLY_UNDERUSED -> BACKGROUND
      OpProjectCalendar localizationCal = OpProjectCalendarFactory.getInstance().getDefaultCalendar(session);

      prepareLegendRow(localizationCal, legendDataSet, Double.valueOf(OpSettingsService
            .getService().getStringValue(broker, OpSettings.HIGHLY_UNDERUTILIZED)),
            null, XStyle.DEFAULT_GRAY2, map.getResource(HIGHLY_UNDERUSED).getText());

      //UNDERUSED -> BLUE
      prepareLegendRow(localizationCal, legendDataSet, Double.valueOf(OpSettingsService
            .getService().getStringValue(broker, OpSettings.UNDERUTILIZED)),
            null, XStyle.DEFAULT_BLUE, map.getResource(UNDERUSED).getText());

      //NORMALUSE -> GREEN
      prepareLegendRow(localizationCal, legendDataSet, Double.valueOf(OpSettingsService
            .getService().getStringValue(broker, OpSettings.UNDERUTILIZED)),
            Double.valueOf(OpSettingsService.getService().getStringValue(
                  broker, OpSettings.OVERUTILIZED)), XStyle.DEFAULT_GREEN, map
                  .getResource(NORMALUSE).getText());

      //OVERUSED -> ORANGE
      prepareLegendRow(localizationCal, legendDataSet, null,
            Double.valueOf(OpSettingsService.getService().getStringValue(
                  broker, OpSettings.OVERUTILIZED)), XStyle.DEFAULT_ORANGE, map
                  .getResource(OVERUSED).getText());

      //HIGHLY_OVERUSED -> RED
      prepareLegendRow(localizationCal, legendDataSet, null,
            Double.valueOf(OpSettingsService.getService().getStringValue(
                  broker, OpSettings.HIGHLY_OVERUTILIZED)), XStyle.DEFAULT_RED, map
                  .getResource(HIGHLY_OVERUSED).getText());

      prepareLegendRow(localizationCal, legendDataSet, null, null,
            XStyle.DEFAULT_WHITE, map.getResource(ABSENCE).getText());
   }

   private void prepareLegendRow(OpProjectCalendar localizationCal, XComponent legendDataSet, Double lower,
         Double upper, Color color, String legendText) {
      XComponent dataRow;
      XComponent dataCell;
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      String ls = lower != null ? localizationCal.localizedDoubleToString(lower.doubleValue()) : null;
      String us = upper != null ? localizationCal.localizedDoubleToString(upper.doubleValue()) : null;
      if (us != null || ls != null) {
         dataCell.setStringValue(legendText+" ("+ (ls != null ? (us != null ? ls + "-" + us : "<" + ls) : ">" + us) + ")");
      }
      else {
         dataCell.setStringValue(legendText);
      }
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(color);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(lower != null ? lower : upper);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);
   }
   
}
