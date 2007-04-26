/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_resources.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project_resources.OpProjectResourceDataSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpProjectResourcesFormProvider implements XFormProvider {

   private final static String RESOURCE_SET = "ResourceSet";

   protected final static String PROJECT_ID = "project_id";

   private final static String PRINT_TITLE = "PrintTitle";
   private final static String PRINT_BUTTON = "PrintButton";
   private final static String RESOURCE_FOOTER_DATA_SET = "ResourceFooterSet";
   private final static String TOTAL_CAPTION = "Total";
   // NL: Remaining is estimation of resource (effortToComplete); predicted = actual + remaining
   // (Therefore, deviation = predicted - base)

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

         OpProjectNode project = (OpProjectNode) (broker.getObject(project_locator));
         //print title
         form.findComponent(PRINT_TITLE).setStringValue(project.getName());
         form.findComponent(PRINT_BUTTON).setEnabled(true);

         // Locate data set in form
         XComponent data_set = form.findComponent(RESOURCE_SET);
         int max_outline_level = getMaxOutlineLevel(form, session);
         // Create dynamic resource summaries for collection-activities
         // (Note: Value of collection-activities have been set on check-in/work-calculator)
         OpProjectResourceDataSetFactory.fillEffortDataSet(broker, project, max_outline_level, data_set);
      }
      broker.close();
      updateFooterData(form);
   }

   private void updateFooterData(XComponent form) {
      XComponent dataSet = form.findComponent(RESOURCE_SET);
      XComponent footerDataSet = form.findComponent(RESOURCE_FOOTER_DATA_SET);

      XComponent row = new XComponent(XComponent.DATA_ROW);
      double sum;
      XComponent cell;

      //total 0
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setStringValue(form.findComponent(TOTAL_CAPTION).getText());
      row.addChild(cell);

      //base 1
      cell = new XComponent(XComponent.DATA_CELL);
      sum = dataSet.calculateDoubleSum(1, 0);
      cell.setDoubleValue(sum);
      row.addChild(cell);

      //actual 2
      cell = new XComponent(XComponent.DATA_CELL);
      sum = dataSet.calculateDoubleSum(2, 0);
      cell.setDoubleValue(sum);
      row.addChild(cell);

      //remaining 3
      cell = new XComponent(XComponent.DATA_CELL);
      sum = dataSet.calculateDoubleSum(3, 0);
      cell.setDoubleValue(sum);
      row.addChild(cell);

      //expected 4
      cell = new XComponent(XComponent.DATA_CELL);
      sum = dataSet.calculateDoubleSum(4, 0);
      cell.setDoubleValue(sum);
      row.addChild(cell);

      //5
      cell = new XComponent(XComponent.DATA_CELL);
      row.addChild(cell);

      //6
      cell = new XComponent(XComponent.DATA_CELL);
      row.addChild(cell);

      footerDataSet.addChild(row);
   }

   protected int getMaxOutlineLevel(XComponent form, OpProjectSession session) {
      return 0;
   }

}