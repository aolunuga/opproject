/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityIfc;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.work.validators.OpWorkCostValidator;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;

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

   private static final String GET_ATTACHMENT_COUNT_FOR_COST_RECORD =
        "select count(attachment.id) from OpAttachment attachment where attachment.CostRecord = (:costRecordId)";

   /**
    * Creates a <code>XComponent</code> data set from a <code>OpWorkRecord</code> entity. Each row in the
    * data set will represent an <code>OpCostRecord</code> entity from the work record's cost recods set.
    *
    * @param workRecord - the <code>OpWorkRecord</code> entity whose cost records will be in the data set
    * @param session    - the <code>OpProjectSession</code> needed to get the internationalized cost types
    * @param broker     - the <code>OpBroker</code> needed to perform the DB operations
    * @return a <code>XComponent</code> data set created from the <code>OpWorkRecord</code> entity. Each row in the
    *         data set will represent an <code>OpCostRecord</code> entity from the work record's cost recods set.
    */
   public static XComponent getCostDataSetForWorkRecord(OpWorkRecord workRecord, OpProjectSession session, OpBroker broker) {
      XComponent dataSet = new XComponent(XComponent.DATA_SET);

      for (OpCostRecord costRecord : workRecord.getCostRecords()) {
         XComponent dataRow = createCostRow(costRecord, session, broker);
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
   public static Set<OpCostRecord> createCostRecords(OpBroker broker, XComponent costDataSet,
         Map<XComponent, List<OpAttachment>> unmodifiedAttachmentsMap) {
      Set<OpCostRecord> costRecords = new HashSet<OpCostRecord>();
      OpCostRecord costRecord;

      for (int i = 0; i < costDataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) costDataSet.getChild(i);
         costRecord = createCostEntity(broker, dataRow, unmodifiedAttachmentsMap);
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
      dataRow.setValue(XValidator.choice(String.valueOf(OpCostRecord.TRAVEL_COST), localizer.localize("${" + TRAVEL_COST_RESOURCE + "}", session.getLocalizerParameters())));
      costTypesDataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setValue(XValidator.choice(String.valueOf(OpCostRecord.MATERIAL_COST), localizer.localize("${" + MATERIAL_COST_RESOURCE + "}", session.getLocalizerParameters())));
      costTypesDataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setValue(XValidator.choice(String.valueOf(OpCostRecord.EXTERNAL_COST), localizer.localize("${" + EXTERNAL_COST_RESOURCE + "}", session.getLocalizerParameters())));
      costTypesDataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setValue(XValidator.choice(String.valueOf(OpCostRecord.MISCELLANEOUS_COST), localizer.localize("${" + MISCELLANEOUS_COST_RESOURCE + "}", session.getLocalizerParameters())));
      costTypesDataSet.addChild(dataRow);
   }

   /**
    * Creates a <code>XComponent</code> data row with the cell's values set from the <code>OpCostRecord</code> entity
    *
    * @param costRecord - the <code>OpCostRecord</code> entity whose atributes will be set on the data row
    * @param session    - the <code>OpProjectSession</code> needed to get the internationalized cost types
    * @param broker     - the <code>OpBroker</code> needed to perform the DB operations
    * @return a data row with the cell's values set from the <code>OpCostRecord</code> entity
    */
   private static XComponent createCostRow(OpCostRecord costRecord, OpProjectSession session, OpBroker broker) {
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
      String activityName = OpWorkSlipDataSetFactory.generateActivityName(activity);
      dataCell.setStringValue(XValidator.choice(activity.locator(), activityName));

      //2 - set the name of the resource
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.RESOURCE_NAME_INDEX);
      dataCell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));

      //3 - set the indicator
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.INDICATOR_INDEX);
      if (hasAttachments(broker, costRecord)) {
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
            text = localizer.localize("${" + TRAVEL_COST_RESOURCE + "}", session.getLocalizerParameters());
            break;
         }
         case OpCostRecord.MATERIAL_COST: {
            text = localizer.localize("${" + MATERIAL_COST_RESOURCE + "}", session.getLocalizerParameters());
            break;
         }
         case OpCostRecord.EXTERNAL_COST: {
            text = localizer.localize("${" + EXTERNAL_COST_RESOURCE + "}", session.getLocalizerParameters());
            break;
         }
         case OpCostRecord.MISCELLANEOUS_COST: {
            text = localizer.localize("${" + MISCELLANEOUS_COST_RESOURCE + "}", session.getLocalizerParameters());
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

      //12 - set the actual + remaining sum
      dataCell = (XComponent) dataRow.getChild(OpWorkCostValidator.ACTUAL_REMAINING_SUM_INDEX);
      dataCell.setDoubleValue(costRecord.getActualCosts() + costRecord.getRemainingCosts());
      if (activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setValue(null);
         dataCell.setEnabled(false);
      }

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
   private static OpCostRecord createCostEntity(OpBroker broker, XComponent dataRow,
        Map<XComponent, List<OpAttachment>> unmodifiedAttachmentsMap) {
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
      //if the data row has unmodified attachments set the newly formed cost record on the existing attachment. This was
      // done so that the attachments don't lose their creation date.
      if(unmodifiedAttachmentsMap != null && unmodifiedAttachmentsMap.get(dataRow) != null) {
         for(OpAttachment attachment : unmodifiedAttachmentsMap.get(dataRow)) {
            attachment.setCostRecord(costRecord);
         }
      }

      //set the attachments
      List<List> attachmentList = ((XComponent) dataRow.getChild(OpWorkCostValidator.ATTACHMENT_INDEX)).getListValue();
      if (attachmentList != null && !attachmentList.isEmpty()) {
         for (List attachmentElement : attachmentList) {
            OpAttachment attachment = OpActivityDataSetFactory.createAttachment(broker, costRecord, attachmentElement, null);
            costRecord.addAttachment(attachment); 
//            OpPermissionDataSetFactory.updatePermissions(broker, costRecord.getActivity().getProjectPlan().getProjectNode(), attachment);
         }
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
      OpActivityIfc activity;

      for (OpAssignment assignment : assignmentList) {
         activity = assignment.getActivity();

         //filter out milestones
         if (activity.getType() == OpActivity.MILESTONE) {
            continue;
         }

         OpWorkSlipDataSetFactory.fillChoiceDataSetsFromSingleAssignment(assignment, choiceProjectSet,
              choiceActivitySet, choiceResourceSet);
      }
      //sort the project & resources data-sets ascending after name
      choiceProjectSet.sort();
      choiceResourceSet.sort();
   }

   /**
    * Returns <code>true</code> if the <code>OpCostRecord</code> specified as parameter has attachments or <code>false</code> otherwise.
    *
    * @param broker     - the <code>OpBroker</code> object needed to perform DB operations.
    * @param costRecord - the <code>OpCostRecord</code> object.
    * @return <code>true</code> if the <code>OpCostRecord</code> specified as parameter has attachments or <code>false</code> otherwise.
    */
   public static boolean hasAttachments(OpBroker broker, OpCostRecord costRecord) {
      if (costRecord.getAttachments() != null) {
         OpQuery query = broker.newQuery(GET_ATTACHMENT_COUNT_FOR_COST_RECORD);
         query.setLong("costRecordId", costRecord.getId());
         Number counter = (Number) broker.iterate(query).next();
         if (counter.intValue() > 0) {
            return true;
         }
      }
      return false;
   }
}