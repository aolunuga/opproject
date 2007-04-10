/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_resources.forms;

import onepoint.express.XComponent;
import onepoint.express.XStyle;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project_resources.OpProjectResourceDataSetFactory;
import onepoint.project.modules.project_resources.forms.OpProjectResourcesFormProvider;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpProjectResourcesAdvancedFormProvider extends OpProjectResourcesFormProvider {

   public final static String CHART_RESOURCE_SET = "ChartResourceSet";
   public final static String RESOURCE_COLOR_SET = "ResourceColorSet";
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
         XComponent resource_color_data_set = form.findComponent(RESOURCE_COLOR_SET);
         //<FIXME> author="Mihai Costin" description="To be decided how should the chart show activities with outlinve lvl >= 1"
         OpProjectResourceDataSetFactory.fillEffortDataSet(broker, project, 0, chart_data_set);
         fillResourceColorDataSet(chart_data_set, resource_color_data_set);
         //<FIXME>
      }

      broker.close();

   }

   /**
    * Fills the color data set for chart component and color legend.
    *
    * @param chartDataSet         chart's main data set
    * @param resourceColorDataSet color data set
    */
   private void fillResourceColorDataSet(XComponent chartDataSet, XComponent resourceColorDataSet) {
      List addedResource = new ArrayList();
      int index = 0;
      for (int i = 0; i < chartDataSet.getChildCount(); i++) {
         XComponent row = (XComponent) chartDataSet.getChild(i);
         if (row.getOutlineLevel() == 1) {
            XComponent captionCell = (XComponent) row.getChild(0);
            String caption = captionCell.getStringValue();
            if (!addedResource.contains(caption)) {
               addedResource.add(caption);
               XComponent cell = new XComponent(XComponent.DATA_CELL);
               cell.setStringValue(caption);
               XComponent colorRow = new XComponent(XComponent.DATA_ROW);
               colorRow.addChild(cell);
               cell = new XComponent(XComponent.DATA_CELL);
               cell.setValue(XStyle.colorSchema.get(index % XStyle.colorSchema.size()));
               colorRow.addChild(cell);
               resourceColorDataSet.addChild(colorRow);
               index++;
            }
         }
      }
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
