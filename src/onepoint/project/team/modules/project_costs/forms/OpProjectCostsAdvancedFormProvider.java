/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_costs.forms;

import onepoint.express.XComponent;
import onepoint.express.XStyle;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project_costs.forms.OpProjectCostsFormProvider;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Advanced form provider class for the close projec costs module.
 */
public class OpProjectCostsAdvancedFormProvider extends OpProjectCostsFormProvider {

   public final static String CHART_RESOURCE_SET = "ChartCostSet";
   public final static String CHART_COLORS_SET = "ColorsCostSet";
   public final static String OUTLINE_LEVEL_CHOOSER = "OutlineLevelChooser";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // Decide on project-ID and retrieve project
      String project_locator = (String) (parameters.get(PROJECT_ID));
      if (project_locator != null) {
         // Get open project-ID from parameters and set project-ID session variable
         session.setVariable(PROJECT_ID, project_locator);
      }
      else {
         project_locator = (String) (session.getVariable(PROJECT_ID));
      }

      if (project_locator != null) {
         super.prepareForm(s, form, parameters);
         form.findComponent(OUTLINE_LEVEL_CHOOSER).setEnabled(true);
         OpProjectNode project = (OpProjectNode) (broker.getObject(project_locator));
         XComponent chart_data_set = form.findComponent(CHART_RESOURCE_SET);
         //<FIXME> author="Mihai Costin" description="To be decided how should the chart show activities with outlinve lvl >= 1"
         createViewDataSet(session, broker, project, 0, chart_data_set);
         //<FIXME>
         XComponent color_data_set = form.findComponent(CHART_COLORS_SET);
         fillColorDataSet(session, color_data_set);
      }
      broker.close();
   }

   private void fillColorDataSet(OpProjectSession session, XComponent color_data_set) {

      // I18ned cost types
      XLocalizer localizer = new XLocalizer();
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), PROJECT_COSTS_PROJECT_COSTS);
      String personnel = PERSONNEL;
      String travel = TRAVEL;
      String material = MATERIAL;
      String external = EXTERNAL;
      String miscellaneous = MISCELLANEOUS;
      if (resourceMap != null) {
         localizer.setResourceMap(resourceMap);
         personnel = localizer.localize(personnel);
         travel = localizer.localize(travel);
         material = localizer.localize(material);
         external = localizer.localize(external);
         miscellaneous = localizer.localize(miscellaneous);
      }

      XComponent dataRow;
      XComponent dataCell;
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(personnel);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(0));
      dataRow.addChild(dataCell);
      color_data_set.addDataRow(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(travel);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(1));
      dataRow.addChild(dataCell);
      color_data_set.addDataRow(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(material);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(2));
      dataRow.addChild(dataCell);
      color_data_set.addDataRow(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(external);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(3));
      dataRow.addChild(dataCell);
      color_data_set.addDataRow(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(miscellaneous);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.colorSchema.get(4));
      dataRow.addChild(dataCell);
      color_data_set.addDataRow(dataRow);

   }

   protected int getMaxOutlineLevel(XComponent form, OpProjectSession session) {
      int max_outline_level;
      XComponent outlineLevel = form.findComponent(OUTLINE_LEVEL_CHOOSER);
      Integer index = null;
      HashMap componentStateMap = session.getComponentStateMap(form.getID());
      if (componentStateMap != null) {
         Integer state = (Integer) componentStateMap.get(OUTLINE_LEVEL_CHOOSER);
         if (state != null) {
            index = state;
         }
      }
      if (index == null) {
         index = outlineLevel.getSelectedIndex();
      }

      int indexVal = index.intValue();
      XComponent dataSet = outlineLevel.getDataSetComponent();
      XComponent selectedRow = (XComponent) dataSet.getChild(indexVal);
      String strValue = XValidator.choiceID(selectedRow.getStringValue());
      int value = Integer.parseInt(strValue);

      if (value == -1) {
         max_outline_level = Integer.MAX_VALUE;
      }
      else {
         max_outline_level = value;
      }
      return max_outline_level;
   }


}
