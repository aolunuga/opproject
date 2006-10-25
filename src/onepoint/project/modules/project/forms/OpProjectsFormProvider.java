/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermission;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpProjectsFormProvider implements XFormProvider {

   /**
    * Form component ids.
    */
   public final static String PROJECT_DATA_SET = "ProjectDataSet";

   private static final String NEW_PROJECT_BUTTON = "NewProjectButton";
   private static final String NEW_PORTFOLIO_BUTTON = "NewPortfolioButton";
   private static final String EDIT_BUTTON = "EditButton";
   private static final String INFO_BUTTON = "InfoButton";
   private static final String MOVE_BUTTON = "MoveButton";
   private static final String DELETE_BUTTON = "DeleteButton";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = ((OpProjectSession) session).newBroker();

      //set the value of the manager permission
      form.findComponent("ManagerPermission").setByteValue(OpPermission.MANAGER);

      //set the permission for the root portfolio
      OpProjectNode rootPortfolio = OpProjectAdministrationService.findRootPortfolio(broker);
      byte rootPortfolioPermission = session.effectiveAccessLevel(broker, rootPortfolio.getID());
      form.findComponent("RootPortfolioPermission").setByteValue(rootPortfolioPermission);

      //see whether newXXX buttons should be enabled or disabled
      if (rootPortfolioPermission < OpPermission.MANAGER) {
         form.findComponent(NEW_PORTFOLIO_BUTTON).setEnabled(false);
         form.findComponent(NEW_PROJECT_BUTTON).setEnabled(false);
      }

      //disable buttons that required selection to work
      disableSelectionButtons(form);

      //retrieve project data set
      XComponent dataSet = form.findComponent(PROJECT_DATA_SET);
      OpProjectDataSetFactory.retrieveProjectDataSet(session, broker, dataSet, OpProjectDataSetFactory.ALL_TYPES, true);

      // Only projects and programs have project plans and thus make sense for progress bars
      ArrayList projectTypes = new ArrayList();
      projectTypes.add(new Byte(OpProjectNode.PROJECT));

      ArrayList activityTypes = new ArrayList();
      activityTypes.add(new Byte(OpActivity.STANDARD));
      activityTypes.add(new Byte(OpActivity.COLLECTION));

      // Gather accessible project IDs
      int i = 0;
      XComponent dataRow = null;
      XComponent dataCell = null;
      Long projectId = null;
      ArrayList projectIds = new ArrayList(dataSet.getChildCount());
      HashMap dataRowMap = new HashMap();
      int startIndex = -1;
      String descriptor = null;
      boolean isProject = false;
      for (i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         projectId = new Long(OpLocator.parseLocator(dataRow.getStringValue()).getID());
         projectIds.add(projectId);
         dataRowMap.put(projectId, dataRow);
         if (startIndex == -1)
            startIndex = dataRow.getChildCount();
         descriptor = ((XComponent) dataRow.getChild(OpProjectDataSetFactory.DESCRIPTOR_COLUMN_INDEX)).getStringValue();
         isProject = descriptor.equals(OpProjectDataSetFactory.PROJECT_DESCRIPTOR);
         //6 - Completed
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (isProject)
            dataCell.setDoubleValue(0);
         dataRow.addChild(dataCell);
         //7 - Resources
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (isProject)
            dataCell.setDoubleValue(0);
         dataRow.addChild(dataCell);
         //8 - Costs
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (isProject)
            dataCell.setDoubleValue(0);
         dataRow.addChild(dataCell);
      }

      // Add completed status
      // TODO: (Sum of individual complete * individual duration) / (number * sum of durations)
      StringBuffer queryBuffer = new StringBuffer(
            "select project.ID, sum(activity.Complete * activity.Duration),  sum(activity.Duration)");
      queryBuffer
            .append(" from OpProjectNode as project inner join project.Plan as plan inner join plan.Activities as activity");
      queryBuffer
            .append(" where project.ID in (:projectIds) and project.Type in (:projectTypes) and activity.OutlineLevel = 0 and activity.Type in (:activityTypes) group by project.ID");
      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setCollection("projectIds", projectIds);
      query.setCollection("projectTypes", projectTypes);
      query.setCollection("activityTypes", activityTypes);
      List completes = broker.list(query);
      Object[] record = null;
      for (i = 0; i < completes.size(); i++) {
         record = (Object[]) completes.get(i);
         Long projId = (Long) record[0];
         Double sum1 = (Double) record[1];
         Double sum2 = (Double) record[2];
         dataRow = (XComponent) dataRowMap.get(projId);
         if (sum1 != null && sum2 != null) {
            double value = Double.MAX_VALUE;
            if (sum1.doubleValue() == 0 && sum2.doubleValue() == 0) {
               value = 0;
            }
            else if (sum2.doubleValue() != 0) {
               value = sum1.doubleValue() / sum2.doubleValue();
            }
            ((XComponent) dataRow.getChild(startIndex)).setDoubleValue(value);
         }
      }

      // Add resource status
      queryBuffer = new StringBuffer("select project.ID, sum(activity.ActualEffort), sum(activity.BaseEffort)");
      queryBuffer
            .append(" from OpProjectNode as project inner join project.Plan as plan inner join plan.Activities as activity");
      queryBuffer
            .append(" where project.ID in (:projectIds) and project.Type in (:projectTypes) and activity.OutlineLevel = 0 and activity.Type in (:activityTypes) group by project.ID");
      query = broker.newQuery(queryBuffer.toString());
      query.setCollection("projectIds", projectIds);
      query.setCollection("projectTypes", projectTypes);
      query.setCollection("activityTypes", activityTypes);
      List resources = broker.list(query);
      record = null;
      for (i = 0; i < resources.size(); i++) {
         record = (Object[]) resources.get(i);
         dataRow = (XComponent) dataRowMap.get((Long) record[0]);
         Long projId = (Long) record[0];
         Double sum1 = (Double) record[1];
         Double sum2 = (Double) record[2];
         dataRow = (XComponent) dataRowMap.get(projId);
         if (sum1 != null && sum2 != null) {
            double value = Double.MAX_VALUE;
            if (sum1.doubleValue() == 0 && sum2.doubleValue() == 0) {
               value = 0;
            }
            else if (sum2.doubleValue() != 0) {
               value = sum1.doubleValue() / sum2.doubleValue() * 100;
            }
            ((XComponent) dataRow.getChild(startIndex + 1)).setDoubleValue(value);
         }
      }

      // Add costs status
      queryBuffer = new StringBuffer("select project.ID");
      queryBuffer
            .append(", sum(activity.ActualPersonnelCosts + activity.ActualTravelCosts + activity.ActualMaterialCosts + activity.ActualExternalCosts + activity.ActualMiscellaneousCosts)");
      queryBuffer
            .append(" , sum(activity.BasePersonnelCosts + activity.BaseTravelCosts + activity.BaseMaterialCosts + activity.BaseExternalCosts + activity.BaseMiscellaneousCosts)");
      queryBuffer
            .append(" from OpProjectNode as project inner join project.Plan as plan inner join plan.Activities as activity");
      queryBuffer
            .append(" where project.ID in (:projectIds) and project.Type in (:projectTypes) and activity.OutlineLevel = 0 and activity.Type in (:activityTypes) group by project.ID");
      query = broker.newQuery(queryBuffer.toString());
      query.setCollection("projectIds", projectIds);
      query.setCollection("activityTypes", activityTypes);
      query.setCollection("projectTypes", projectTypes);
      List costs = broker.list(query);
      record = null;
      for (i = 0; i < costs.size(); i++) {
         record = (Object[]) costs.get(i);
         dataRow = (XComponent) dataRowMap.get((Long) record[0]);
         Long projId = (Long) record[0];
         Double sum1 = (Double) record[1];
         Double sum2 = (Double) record[2];
         dataRow = (XComponent) dataRowMap.get(projId);
         if (sum1 != null && sum2 != null) {
            double value = Double.MAX_VALUE;
            if (sum1.doubleValue() == 0 && sum2.doubleValue() == 0) {
               value = 0;
            }
            else if (sum2.doubleValue() != 0) {
               value = sum1.doubleValue() / sum2.doubleValue() * 100;
            }
            ((XComponent) dataRow.getChild(startIndex + 2)).setDoubleValue(value);
         }
      }

      broker.close();
   }

   /**
    * Disables buttons that require a selection in order to be enabled.
    * @param form a <code>XComponent</code> representing the project form.
    */
   protected void disableSelectionButtons(XComponent form) {
      form.findComponent(INFO_BUTTON).setEnabled(false);
      form.findComponent(MOVE_BUTTON).setEnabled(false);
      form.findComponent(DELETE_BUTTON).setEnabled(false);
      form.findComponent(EDIT_BUTTON).setEnabled(false);
   }
}
