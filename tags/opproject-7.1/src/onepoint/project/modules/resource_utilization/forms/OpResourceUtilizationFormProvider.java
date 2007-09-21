/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization.forms;

import onepoint.express.XComponent;
import onepoint.express.XStyle;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationDataSetFactory;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Map;

/**
 * Form provider class for the resource utilization diagram.
 */
public class OpResourceUtilizationFormProvider implements XFormProvider {

   /**
    *  Form constants
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

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      //prepare the utilization legend data set
      XLanguageResourceMap map = session.getLocale().getResourceMap(RESOURCE_MAP);
      XComponent legendDataSet = form.findComponent(UTILIZATION_LEGEND_DATA_SET);
      this.fillLegendDataSet(map, legendDataSet);

      //fill resources
      Map poolColumnsSelector = this.createPoolColumnsSelector();
      form.findComponent(POOL_SELECTOR).setValue(poolColumnsSelector);
      Map resourceColumnsSelector = this.createResourceColumsSelector();
      form.findComponent(RESOURCE_SELECTOR).setValue(resourceColumnsSelector);
      XComponent dataSet = form.findComponent(UTILIZATION_DATA_SET);
      OpResourceDataSetFactory.retrieveFirstLevelsResourceDataSet(session, dataSet, poolColumnsSelector, resourceColumnsSelector, null);

      //fill the actual utilization values
      OpResourceUtilizationDataSetFactory.fillUtilizationValues(session, dataSet, null);

      //if we have at least 1 resource/pool, enable print button
      if (OpResourceUtilizationDataSetFactory.getUtilizationMap(session, dataSet).size() > 0) {
         form.findComponent(PRINT_BUTTON).setEnabled(true);
      }
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
}
