/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization.forms;

import onepoint.express.XComponent;
import onepoint.express.XStyle;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationDataSetFactory;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Map;


public class OpResourceUtilizationFormProvider implements XFormProvider {

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


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      XComponent dataSet = form.findComponent(UTILIZATION_DATA_SET);
      XComponent dataRow;
      XLanguageResourceMap map = session.getLocale().getResourceMap(RESOURCE_MAP);

      //prepare the utilization legend data set
      XComponent legendDataSet = form.findComponent(UTILIZATION_LEGEND_DATA_SET);
      XComponent dataCell;

      //HIGHLY_UNDERUSED -> BACKGROUND
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
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

      Integer index;
      Map poolColumnsSelector = new HashMap();

      index = new Integer(OpProjectComponent.UTILIZATION_DESCRIPTOR_COLUMN_INDEX);
      poolColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.DESCRIPTOR));
      index = new Integer(OpProjectComponent.UTILIZATION_NAME_COLUMN_INDEX);
      poolColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.NAME));
      index = new Integer(OpProjectComponent.UTILIZATION_ROW_ID);
      //needed because we want for the pools and resources to have the same nr. of cells
      poolColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.NULL));

      Map resourceColumnsSelector = new HashMap();
      index = new Integer(OpProjectComponent.UTILIZATION_DESCRIPTOR_COLUMN_INDEX);
      resourceColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.DESCRIPTOR));
      index = new Integer(OpProjectComponent.UTILIZATION_NAME_COLUMN_INDEX);
      resourceColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.NAME));
      index = new Integer(OpProjectComponent.UTILIZATION_AVAILABLE_COLUMN_INDEX);
      resourceColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.AVAILABLE));
      index = new Integer(OpProjectComponent.UTILIZATION_ROW_ID);
      resourceColumnsSelector.put(index, new Integer(OpResourceDataSetFactory.ID));

      OpResourceDataSetFactory.retrieveFirstLevelsResourceDataSet(session, dataSet, poolColumnsSelector, resourceColumnsSelector, null);

      form.findComponent(POOL_SELECTOR).setValue(poolColumnsSelector);
      form.findComponent(RESOURCE_SELECTOR).setValue(resourceColumnsSelector);

      OpResourceUtilizationDataSetFactory.fillUtilizationValues(session, dataSet, null);

      //if we have at least 1 resource/pool, enable print button
      if (OpResourceUtilizationDataSetFactory.getUtilizationMap(session, dataSet).size() > 0) {
         form.findComponent(PRINT_BUTTON).setEnabled(true);
      }
   }


}
