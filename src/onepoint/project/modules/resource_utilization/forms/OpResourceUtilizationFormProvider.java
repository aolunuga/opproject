/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class OpResourceUtilizationFormProvider implements XFormProvider {

   // TODO: Check if it can be 100% correct w/start and end-based data, or if time-phased approach is necessary
   // TODO: Try to optimize via database queries and joins (assignments and activities)

   public final static String UTILIZATION_DATA_SET = "UtilizationDataSet";
   public final static String UTILIZATION_LEGEND_DATA_SET = "UtilizationResourceColorSet";

   public final static String ALL_POOLS = "from " + OpResourcePool.RESOURCE_POOL;
   public final static String RESOURCES_OUTSIDE_POOL = "select resource from OpResource as resource where resource.Pool.ID = null";
   public final static String ALL_ASSIGNMENTS = "from " + OpAssignment.ASSIGNMENT;

   public final static int POOL_ICON_INDEX = 0;
   public final static int RESOURCE_ICON_INDEX = 1;

   public final static String POOL_DESCRIPTOR = OpProjectComponent.UTILIZATION_POOL_DESCRIPTOR;
   public final static String RESOURCE_DESCRIPTOR = OpProjectComponent.UTILIZATION_RESOURCE_DESCRIPTOR;
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


      List poolColumnsSelector = new ArrayList();
      poolColumnsSelector.add(new Integer(OpResourceDataSetFactory.DESCRIPTOR));
      poolColumnsSelector.add(new Integer(OpResourceDataSetFactory.NAME));
      poolColumnsSelector.add(new Integer(OpResourceDataSetFactory.NULL));
      poolColumnsSelector.add(new Integer(OpResourceDataSetFactory.NULL));
      poolColumnsSelector.add(new Integer(OpResourceDataSetFactory.NULL));
      poolColumnsSelector.add(new Integer(OpResourceDataSetFactory.NULL));
      poolColumnsSelector.add(new Integer(OpResourceDataSetFactory.NULL));
      List resourceColumnsSelector = new ArrayList();
      resourceColumnsSelector.add(new Integer(OpResourceDataSetFactory.DESCRIPTOR));
      resourceColumnsSelector.add(new Integer(OpResourceDataSetFactory.NAME));
      resourceColumnsSelector.add(new Integer(OpResourceDataSetFactory.AVAILABLE));
      resourceColumnsSelector.add(new Integer(OpResourceDataSetFactory.NULL));
      resourceColumnsSelector.add(new Integer(OpResourceDataSetFactory.NULL));
      resourceColumnsSelector.add(new Integer(OpResourceDataSetFactory.NULL));
      resourceColumnsSelector.add(new Integer(OpResourceDataSetFactory.ID));
      OpResourceDataSetFactory.retrieveFirstLevelsResourceDataSet(session, dataSet, poolColumnsSelector, resourceColumnsSelector, null);

      form.findComponent(POOL_SELECTOR).setValue(poolColumnsSelector);
      form.findComponent(RESOURCE_SELECTOR).setValue(resourceColumnsSelector);

      OpResourceUtilizationDataSetFactory.calculateUtilizationValues(dataSet, session);

   }


}
