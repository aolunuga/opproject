/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_planning.components.test;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.XStyle;
import onepoint.express.server.XFormProvider;
import onepoint.project.team.modules.project_planning.components.OpChartComponent;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for the project components test class.
 *
 * @author mihai.costin
 */
public class OpAdvancedProjectComponentTestFormProvider implements XFormProvider {
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {

      //pipeline & chart
      OpChartComponent pipeline = (OpChartComponent) form.findComponent("pipeline");
      XExtendedComponent legend = (XExtendedComponent) form.findComponent("pipelineLegend");

      //color data set ref for pipeline and chart
      XComponent bubbleColors = (XComponent) form.findComponent("BubbleColors");
      XComponent dataCell;
      XComponent dataRow;
      //color 1, index 0
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("bubble color 1");
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(0));
      dataRow.addChild(dataCell);
      bubbleColors.addChild(dataRow);
      //color 2, index 1
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("bubble color 2");
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(1));
      dataRow.addChild(dataCell);
      bubbleColors.addChild(dataRow);
      //color 3, index 2
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("bubble color 3");
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(2));
      dataRow.addChild(dataCell);
      bubbleColors.addChild(dataRow);

      XComponent pipelineDataSet = (XComponent) form.findComponent("PipelineDataSet");
      XComponent values;
      XComponent bubble;
      XComponent bubbledataCell;

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("category 1 The first One"); //category name
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(3)); //category color
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      values = new XComponent(XComponent.DATA_SET);
      addBubble(values, 10, "First Bubble", 2);
      addBubble(values, 64, "SECOND Bubble That has long text caption", 1);
      dataCell.setValue(values);
      dataRow.addChild(dataCell);
      pipelineDataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("category 2"); //category name
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(4)); //category color
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      values = new XComponent(XComponent.DATA_SET);
      addBubble(values, 59, "Bubble 6", 1);
      addBubble(values, 78, "Bubble 7", 1);
      addBubble(values, 100, "Max size bubble", 0);
      addBubble(values, 15, "Bubble 9", 2);
      dataCell.setValue(values);
      dataRow.addChild(dataCell);
      pipelineDataSet.addChild(dataRow);


      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("category 3"); //category name
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(5)); //category color
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      values = new XComponent(XComponent.DATA_SET);
      addBubble(values, 55, "Bubble 3", 1);
      addBubble(values, 23, "Bubble 4", 2);
      addBubble(values, 89, "Bubble 5", 0);
      dataCell.setValue(values);
      dataRow.addChild(dataCell);
      pipelineDataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("category 4"); //category name
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(6)); //category color
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      values = new XComponent(XComponent.DATA_SET);
      addBubble(values, 0, "Min size bubble", 1);
      dataCell.setValue(values);
      dataRow.addChild(dataCell);
      pipelineDataSet.addChild(dataRow);
      
   }

   private void addBubble(XComponent dataSet, int bubbleSize, String bubbleName, int bubbleColorIndex) {
      XComponent bubble;
      XComponent bubbledataCell;
      bubble = new XComponent(XComponent.DATA_ROW);
      bubbledataCell = new XComponent(XComponent.DATA_CELL);
      bubbledataCell.setDoubleValue(bubbleSize);
      bubble.addChild(bubbledataCell);
      bubbledataCell = new XComponent(XComponent.DATA_CELL);
      bubbledataCell.setStringValue(bubbleName);
      bubble.addChild(bubbledataCell);
      bubbledataCell = new XComponent(XComponent.DATA_CELL);
      bubbledataCell.setIntValue(bubbleColorIndex);
      bubble.addChild(bubbledataCell);
      dataSet.addChild(bubble);
   }
}
