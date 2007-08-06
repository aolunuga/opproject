/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.validators.OpWorkCostValidator;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class needed to form OpCostRecord entities from data sets and vice versa
 *
 * @author florin.haizea
 */
public class OpCostRecordDataSetFactory {

   //map for the cost type names
   private static final String COST_TYPES_RESOURCE_MAP = "work.costTypes";
   private static final String TRAVEL_COST_RESOURCE = "TravelCost";
   private static final String MATERIAL_COST_RESOURCE = "MaterialCost";
   private static final String EXTERNAL_COST_RESOURCE = "ExternalCost";
   private static final String MISCELLANEOUS_COST_RESOURCE = "MiscellaneousCost";

   /**
    * Creates a <code>XComponent</code> data set from a <code>OpWorkRecord</code> entity. Each row in the
    * data set will represent an <code>OpCostRecord</code> entity from the work record's cost recods set.
    *
    * @param workRecord - the <code>OpWorkRecord</code> entity whose cost records will be in the data set
    * @param session    - the <code>OpProjectSession</code> needed to get the internationalized cost types
    * @return a <code>XComponent</code> data set created from the <code>OpWorkRecord</code> entity. Each row in the
    *         data set will represent an <code>OpCostRecord</code> entity from the work record's cost recods set.
    */
   public static XComponent getCostDataSetForWorkRecord(OpWorkRecord workRecord, OpProjectSession session) {
      XComponent dataSet = new XComponent(XComponent.DATA_SET);

      for (OpCostRecord costRecord : workRecord.getCostRecords()) {
         XComponent dataRow = createCostRow(costRecord, session);
         dataSet.addChild(dataRow);
      }

      return dataSet;
   }

   /**
    * Creates a <code>List</code> of <code>OpCostRecord</code> entities, each entity corresponding to a row in the
    * data set.
    *
    * @param costDataSet - the <code>XComponent</code> data set whose rows will form the list of cost periods
    * @param broker      - the <code>OpBroker</code> needed to persist the attachments and contents
    * @return a <code>List</code> of <code>OpCostRecord</code> entities, each entity corresponding to a row in the
    *         data set.
    */
   public static Set<OpCostRecord> createCostRecords(OpBroker broker, XComponent costDataSet) {
      Set<OpCostRecord> costRecords = new HashSet<OpCostRecord>();
      OpCostRecord costRecord;

      for (int i = 0; i < costDataSet.getChildCount(); i++) {
         costRecord = createCostEntity(broker, (XComponent) costDataSet.getChild(i));
         costRecords.add(costRecord);
      }

      return costRecords;
   }

   /**
    * Fills an <code>XComponent</code> data set with the internationalized costs type names.
    *
    * @param session          - the <code>OpProjectSession</code> needed to get the locale settings
    * @param costTypesDataSet - the <code>XComponent</code> data set that will be filled with the costs type names.
    */
   public static void fillCostTypesDataSet(OpProjectSession session, XComponent costTypesDataSet) {
      XLocalizer localizer = new XLocalizer();
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), COST_TYPES_RESOURCE_MAP);
      localizer.setResourceMap(resourceMap);

      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setValue(XValidator.choice(String.valueOf(OpCostRecord.TRAVEL_COST), localizer.localize("${" + TRAVEL_COST_RESOURCE + "}")));
      costTypesDataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setValue(XValidator.choice(String.valueOf(OpCostRecord.MATERIAL_COST), localizer.localize("${" + MATERIAL_COST_RESOURCE + "}")));
      costTypesDataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setValue(XValidator.choice(String.valueOf(OpCostRecord.EXTERNAL_COST), localizer.localize("${" + EXTERNAL_COST_RESOURCE + "}")));
      costTypesDataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setValue(XValidator.choice(String.valueOf(OpCostRecord.MISCELLANEOUS_COST), localizer.localize("${" + MISCELLANEOUS_COST_RESOURCE + "}")));
      costTypesDataSet.addChild(dataRow);
   }

   /**
    * Creates a <code>XComponent</code> data row with the cell's values set from the <code>OpCostRecord</code> entity
    *
    * @param costRecord - the <code>OpCostRecord</code> entity whose atributes will be set on the data row
    * @param session    - the <code>OpProjectSession</code> needed to get the internationalized cost types
    * @return a data row with the cell's values set from the <code>OpCostRecord</code> entity
    */
   private static XComponent createCostRow(OpCostRecord costRecord, OpProjectSession session) {
      OpWorkCostValidator costValidator = new OpWorkCostValidator();
      XComponent dataRow = costValidator.newDataRow();
      XComponent dataCell;

      OpActivity activity = costRecord.getActivity();
      OpProjectNode project = activity.getProjectPlan().getProjectNode();
      OpResource resource = costRecord.getWorkRecord().getAssignment().getResource();

      //0 - set the name of the project
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.PROJECT_NAME_INDEX);
      dataCell.setStringValue(XValidator.choice(project.locator(), project.getName()));

      //1 - set the name of the activity
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.ACTIVITY_NAME_INDEX);
      dataCell.setStringValue(XValidator.choice(activity.locator(), activity.getName()));

      //2 - set the name of the resource
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.RESOURCE_NAME_INDEX);
      dataCell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));

      //3 - set the indicator
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.INDICATOR_INDEX);
      if (!costRecord.getAttachments().isEmpty()) {
         costValidator.setAttribute(dataRow, OpWorkCostValidator.HAS_ATTACHMENTS, true);
      }
      else {
         costValidator.setAttribute(dataRow, OpWorkCostValidator.HAS_ATTACHMENTS, false);
      }

      //4 - set the cost type
      XLocalizer localizer = new XLocalizer();
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), COST_TYPES_RESOURCE_MAP);
      localizer.setResourceMap(resourceMap);
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.COST_TYPE_INDEX);
      String text = "";
      switch (costRecord.getType()) {
         case OpCostRecord.TRAVEL_COST: {
            text = localizer.localize("${" + TRAVEL_COST_RESOURCE + "}");
            break;
         }
         case OpCostRecord.MATERIAL_COST: {
            text = localizer.localize("${" + MATERIAL_COST_RESOURCE + "}");
            break;
         }
         case OpCostRecord.EXTERNAL_COST: {
            text = localizer.localize("${" + EXTERNAL_COST_RESOURCE + "}");
            break;
         }
         case OpCostRecord.MISCELLANEOUS_COST: {
            text = localizer.localize("${" + MISCELLANEOUS_COST_RESOURCE + "}");
            break;
         }
      }
      dataCell.setStringValue(XValidator.choice(String.valueOf(costRecord.getType()), text));

      //5 - set the base cost
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.BASE_COST_INDEX);
      if (activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setValue(null);
      }
      else {
         dataCell.setDoubleValue(costRecord.getBaseCost());
      }

      //6 - set the actual cost
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.ACTUAL_COST_INDEX);
      dataCell.setDoubleValue(costRecord.getActualCosts());

      //7 - set the remaining cost
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.REMAINING_COST_INDEX);
      if (activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setValue(null);
         dataCell.setEnabled(false);
      }
      else {
         dataCell.setDoubleValue(costRecord.getRemainingCosts());
      }

      //8 - set the comments
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.COMMENTS_COST_INDEX);
      dataCell.setStringValue(costRecord.getComment());

      //9 - set the attachments
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.ATTACHMENT_INDEX);
      ArrayList attachmentList = new ArrayList();
      dataCell.setListValue(attachmentList);
      OpActivityDataSetFactory.retrieveAttachments(costRecord.getAttachments(), attachmentList);

      //10 - set the original remaining cost
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.ORIGINAL_REMAINING_COST_INDEX);
      switch (costRecord.getType()) {
         case OpCostRecord.TRAVEL_COST:
            dataCell.setDoubleValue(activity.getRemainingTravelCosts());
            break;
         case OpCostRecord.MATERIAL_COST:
            dataCell.setDoubleValue(activity.getRemainingMaterialCosts());
            break;
         case OpCostRecord.EXTERNAL_COST:
            dataCell.setDoubleValue(activity.getRemainingExternalCosts());
            break;
         case OpCostRecord.MISCELLANEOUS_COST:
            dataCell.setDoubleValue(activity.getRemainingMiscellaneousCosts());
            break;
      }

      //11 - set the activity type
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.ACTIVITY_TYPE_INDEX);
      dataCell.setByteValue(activity.getType());

      //set the activity's assignment on the dataRow
      dataRow.setValue(costRecord.getWorkRecord().getAssignment().locator());

      return dataRow;
   }

   /**
    * Creates an <code>OpCostRecord</code> entity from the data row's cell's values
    *
    * @param dataRow - the <code>XComponent</code> data row whose cell's values will be set on the entity
    * @param broker  - the <code>OpBroker</code> needed to persist the attachments and contents
    * @return an <code>OpCostRecord</code> entity created from the data row's cell's values
    */
   private static OpCostRecord createCostEntity(OpBroker broker, XComponent dataRow) {
      OpCostRecord costRecord = new OpCostRecord();

      //the work record will not be set on the costRecord
      //set the cost type
      String costTypeCaption = ((XComponent) dataRow.getChild(OpWorkCostValidator.COST_TYPE_INDEX)).getStringValue();
      String type = XValidator.choiceID(costTypeCaption);
      costRecord.setType(Byte.valueOf(type));
      //base costs are not set at this level
      //set the actual costs
      costRecord.setActualCosts(((XComponent) dataRow.getChild(OpWorkCostValidator.ACTUAL_COST_INDEX)).getDoubleValue());

      //set the remaining cost
      double remainingCost;
      Double remainingValue = (Double) ((XComponent) dataRow.getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getValue();
      if (remainingValue == null) {
         remainingCost = 0;
      }
      else {
         remainingCost = remainingValue;
      }
      costRecord.setRemainingCosts(remainingCost);
      //set comments
      costRecord.setComment(((XComponent) dataRow.getChild(OpWorkCostValidator.COMMENTS_COST_INDEX)).getStringValue());
      //set the attachments
      List<List> attachmentList = ((XComponent) dataRow.getChild(OpWorkCostValidator.ATTACHMENT_INDEX)).getListValue();
      if (attachmentList != null && !attachmentList.isEmpty()) {
         Set<OpAttachment> attachments = new HashSet<OpAttachment>();
         for (List attachmentElement : attachmentList) {
            OpAttachment attachment = OpActivityDataSetFactory.createAttachment(broker, null, null, attachmentElement, null, null);
            if (attachment != null) {
               attachments.add(attachment);
            }
         }
         costRecord.setAttachments(attachments);
      }
      return costRecord;
   }

   /**
    * Fills a project choice set, an activity choice set and a resource choice set with information from the
    * assignmentList. Each row in the data sets will have it's value set to the choice of the entity.
    * The activity choice set rows will contain a data cell with the type of activity and a data cell with
    * the list of costs for that activity.
    * <p/>
    * Filter out activities that are milestones.
    *
    * @param choiceProjectSet  - the <code>XComponent</code> choice project set
    * @param choiceActivitySet - the <code>XComponent</code> choice activity set
    * @param choiceResourceSet - the <code>XComponent</code> choice resource set
    * @param assignmentList    - the <code>List</code> of assignments.
    */
   public static void fillChoiceDataSets(XComponent choiceProjectSet, XComponent choiceActivitySet, XComponent choiceResourceSet,
        List<OpAssignment> assignmentList) {
      OpActivity activity;

      for (OpAssignment assignment : assignmentList) {
         activity = assignment.getActivity();

         //filter out milestones
         if (activity.getType() == OpActivity.MILESTONE) {
            continue;
         }

         OpWorkSlipDataSetFactory.fillChoiceDataSetsFromSingleAssignment(assignment, choiceProjectSet,
              choiceActivitySet, choiceResourceSet);
      }
   }
}