/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project.components;

import onepoint.express.XComponent;
import onepoint.express.XValidationException;

import java.sql.Date;
import java.util.*;

/**
 * @author mihai.costin
 */
public class OpIncrementalValidator extends OpGanttValidator {

   private Set startPoints;
   private OpGraph graph;

   /**
    * In adition, start points are computed for each different type of update and used then in the validation process.
    *
    * @see onepoint.express.XValidator#setDataCellValue(onepoint.express.XComponent,int,Object)
    */
   public void setDataCellValue(XComponent data_row, int column_index, Object value) {

      data_set.removeAllDummyRows();
      switch (column_index) {
         case START_COLUMN_INDEX:
            preCheckSetStartValue(data_row, value);

            startPoints = new HashSet();
            if (value == null) {
               //activity becomes a task
               checkDeletedAssignmentsForWorkslips(data_row, new ArrayList());
               startPoints = getAllValidationSuccessors(data_row);
               addToUndo();
               setStart(data_row, null);
               updateAfterDelete(data_row);
            }
            else {
               Date start = (Date) value;
               // If start is not a workday then go to next workday
               if (!calendar.isWorkDay(start)) {
                  start = calendar.nextWorkDay(start);
               }
               addToUndo();
               setStart(data_row, start);
               if (getEnd(data_row) == null) {
                  setEnd(data_row, start);
                  updateProjectPlanFinish();
               }
               updateDuration(data_row, getDuration(data_row));
               startPoints.add(data_row);
            }

            validateDataSet();
            break;

         case END_COLUMN_INDEX:
            preCheckSetEndValue(data_row, value);

            addToUndo();

            startPoints = new HashSet();
            if (value == null) {
               //activity becomes a task
               startPoints = getAllValidationSuccessors(data_row);
               setStart(data_row, null);
               updateAfterDelete(data_row);
            }
            else {
               // Update duration
               Date end = (Date) value;
               // If end is not a workday then go to previous workday
               if (!calendar.isWorkDay(end)) {
                  end = calendar.previousWorkDay(end);
               }
               if (getStart(data_row) == null) {
                  //if it was a task, set also the start date
                  setStart(data_row, (Date) value);
               }
               updateFinish(data_row, end);

               startPoints.add(data_row);
            }

            validateDataSet();
            break;

         case DURATION_COLUMN_INDEX:
            double duration = ((Double) value).doubleValue();
            preCheckSetDurationValue(data_row, duration);

            addToUndo();

            updateDuration(data_row, duration);
            startPoints = new HashSet();
            startPoints.add(data_row);

            validateDataSet();
            break;

         case BASE_EFFORT_COLUMN_INDEX:
            double base_effort = ((Double) value).doubleValue();

            if (getCalculationMode() != null && getCalculationMode().byteValue() == EFFORT_BASED) {
               //if the project is effort based, setting the effort will also affect the duration
               preCheckSetEffortValue(data_row, base_effort);
               addToUndo();
               updateBaseEffort(data_row, base_effort);
               startPoints = new HashSet();
               startPoints.add(data_row);
               validateDataSet();
            }
            else {
               addToUndo();
               updateBaseEffort(data_row, base_effort);
               updateCollectionTreeValues(data_row);
            }
            break;

         case PREDECESSORS_COLUMN_INDEX:
            addToUndo();
            setPredecessorsValue(value, data_row);
            startPoints = new HashSet();
            startPoints.add(data_row);
            validateDataSet();
            break;

         case SUCCESSORS_COLUMN_INDEX:
            addToUndo();
            // Update predecessors of successors
            startPoints = new HashSet();
            startPoints.add(data_row);

            List removed_successors = setSuccessorsValue(value, data_row);

            for (int index = 0; index < removed_successors.size(); index++) {
               XComponent successor = (XComponent) (data_set._getChild(((Integer) (removed_successors.get(index))).intValue()));
               startPoints.add(successor);
            }
            validateDataSet();
            break;

         case VISUAL_RESOURCES_COLUMN_INDEX:

            List resources = (ArrayList) value;
            super.parseVisualResources(resources);

            //tasks, milestones keep only the resource name
            if (getType(data_row) == TASK || getType(data_row) == MILESTONE) {
               addToUndo();

               //for tasks only one resource
               boolean taskWarning = false;
               ArrayList resourcesValue = ((ArrayList) value);
               if (getType(data_row) == TASK && resourcesValue.size() > 1) {
                  resources = new ArrayList();
                  resources.add(resourcesValue.get(0));
                  taskWarning = true;
               }
               checkDeletedAssignmentsForWorkslips(data_row, resources);
               convertResourcesToNameOnly(data_row, resources);
               ArrayList effList = new ArrayList();
               for (int i = 0; i < resources.size(); i++) {
                  if (getType(data_row) == TASK) {
                     //tasks should only have 1 resource
                     effList.add(new Double(getBaseEffort(data_row)));
                     break;
                  }
                  else {
                     effList.add(new Double(0));
                  }
               }
               setResourceBaseEfforts(data_row, effList);
               updateResponsibleResource(data_row);

               startPoints = new HashSet();
               startPoints.add(data_row);

               validateDataSet();

               if (taskWarning) {
                  throw new XValidationException(TASK_EXTRA_RESOURCE_EXCEPTION);
               }
               break;
            }
            else {
               //standard activity
               //make sure from here onwards, we have all the assignment values in independent format.
               resources = deLocalizeVisualResources(resources);

               //standard activity
               resources = prepareResources(data_row, resources);
               addToUndo();
               setResources(data_row, resources);

               //effort stays the same.
               updateBaseEffort(data_row, getBaseEffort(data_row));

               //construct the resource availability map
               updateVisualResources(data_row, isHourBasedResourceView(), getAvailabilityMap());
               updateResponsibleResource(data_row);

               startPoints = new HashSet();
               startPoints.add(data_row);
               validateDataSet();
            }

            break;

         default:
            super.setDataCellValue(data_row, column_index, value);
            break;
      }

   }

   /**
    * @throws onepoint.express.XValidationException
    *          if all the removed rows are mandatory
    * @see onepoint.express.XValidator#removeDataRows(java.util.List)
    */
   public boolean removeDataRows(List data_rows) {
      preCheckRemoveDataRows(data_rows);
      addToUndo();

      //start points = all direct successors and owning collection for each removed data row
      startPoints = new HashSet();
      for (int i = 0; i < data_rows.size(); i++) {
         XComponent row = (XComponent) data_rows.get(i);
         List successorsIndexes = OpGanttValidator.getSuccessors(row);
         List successors = new ArrayList();
         for (Iterator iterator = successorsIndexes.iterator(); iterator.hasNext();) {
            Integer index = (Integer) iterator.next();
            XComponent successor = (XComponent) data_set.getChild(index.intValue());
            successors.add(successor);
         }
         startPoints.addAll(successors);
         XComponent collection = superActivity(row);
         if (collection != null) {
            startPoints.add(collection);
         }
         _removeDataRow(row);
      }
      startPoints.removeAll(data_rows);

      validateDataSet();
      return true;
   }


   /**
    * @throws onepoint.express.XValidationException
    *
    * @see onepoint.express.XValidator#moveDataRows(java.util.List,int)
    */
   public void moveDataRows(List dataRows, int offset)
        throws XValidationException {

      initStartPoints();
      //NOTE: author="Mihai Costin" description="aditional performance gain possible by selecting only start points that changed collection"
      for (int i = 0; i < dataRows.size(); i++) {
         XComponent row = (XComponent) dataRows.get(i);
         startPoints.add(row);
         XComponent collection = superActivity(row);
         if (collection != null) {
            startPoints.add(collection);
         }
      }

      moveRows(dataRows, offset);
      validateDataSet();
   }

   /**
    * Moves an activity in a target collection.
    *
    * @param rows               data rows to be moved
    * @param offset             offset for move action
    * @param targetDataRow      future parent activity
    * @param targetOutlineLevel collection outline level
    */
   public void moveInCollection(ArrayList rows, int offset, XComponent targetDataRow, int targetOutlineLevel) {

      startPoints = new HashSet();
      for (int i = 0; i < rows.size(); i++) {
         XComponent row = (XComponent) rows.get(i);
         startPoints.add(row);
         XComponent collection = superActivity(row);
         if (collection != null) {
            startPoints.add(collection);
         }
      }

      super.moveInCollection(rows, offset, targetDataRow, targetOutlineLevel);
   }


   /**
    * Moves the given activities with the given offset. Will try to change also the outline level of the rows if move is
    * not possible with the actual outline level.
    *
    * @param rows               rows to be moved
    * @param offset             offset for move action
    * @param targetOutlineLevel outline level of the target position
    */
   public void moveOverActivities(ArrayList rows, int offset, int targetOutlineLevel) {

      initStartPoints();
      for (int i = 0; i < rows.size(); i++) {
         XComponent row = (XComponent) rows.get(i);
         startPoints.add(row);
         XComponent collection = superActivity(row);
         if (collection != null) {
            startPoints.add(collection);
         }
      }

      super.moveOverActivities(rows, offset, targetOutlineLevel);
   }

   private void initStartPoints() {
      if (startPoints == null) {
         startPoints = new HashSet();
      }
   }


   /**
    * Changes the outline level for an array of data rows, creating (or destroying) in effect a child-parent
    * relationship. Change the outline Level for each activity of the array.
    *
    * @param data_rows an <code>XArray</code> of activities whose outline levels must be changed
    * @param offset    the offset (positive or negative value)
    * @throws onepoint.express.XValidationException
    *          if a cycle was detected and the outline level can't be changed.
    */
   public void changeOutlineLevels(List data_rows, int offset)
        throws XValidationException {

      initStartPoints();

      for (int i = 0; i < data_rows.size(); i++) {
         XComponent row = (XComponent) data_rows.get(i);
         startPoints.add(row);
         XComponent collection = superActivity(row);
         if (collection != null) {
            startPoints.add(collection);
         }
      }
      super.changeOutlineLevels(data_rows, offset);

   }

   /**
    * Updates the collection values starting from the given collection after an activity was removed from this
    * collection.
    *
    * @param collection starting collection
    */
   private void updateAfterDelete(XComponent collection) {
      if (collection == null) {
         return;
      }
      updateTreeType(collection);
      if (getType(collection) == COLLECTION) {
         updateCollectionDates(collection);
      }
      updateCollectionTreeValues(collection);
   }

   public boolean validateDataSet() {
      if (startPoints == null) {
         startPoints = getIndependentActivities();
      }
      if (!startPoints.isEmpty()) {
         //update the types upwards from the start points
         for (Iterator iterator = startPoints.iterator(); iterator.hasNext();) {
            XComponent activity = (XComponent) iterator.next();
            updateTreeType(activity);
         }

         //update collection
         for (Iterator iterator = startPoints.iterator(); iterator.hasNext();) {
            updateCollectionTreeValues((XComponent) iterator.next());
         }

         validateGanttChart();
      }
      startPoints = null;
      return true;
   }

   /**
    * Validates the entire data-set, by setting the start-points to all the activities in the data-set.
    */
   public void validateEntireDataSet() {
      startPoints = new HashSet();
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent activityRow = (XComponent) data_set.getChild(i);
         startPoints.add(activityRow);
      }
      this.validateDataSet();
   }

   /**
    * Returns a set with the independent activities from the underlying data-set.
    *
    * @return a <code>Set(XComponent(DATA_ROW))</code> representing independent
    *         activities.
    */
   private Set getIndependentActivities() {
      Set result = new HashSet();
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent activity = (XComponent) data_set.getChild(i);
         if (isIndependentActivity(activity)) {
            result.add(activity);
         }
      }
      return result;
   }

   /**
    * Retrieves for a given data row all the successors as seen by the graph.
    *
    * @param data_row data row that is being queried
    * @return a list of <code>XComponent</code> with all the successors from the validation point of view
    */
   private Set getAllValidationSuccessors(XComponent data_row) {

      Set dataRowSuccessors = new HashSet();
      OpGraph graph = OpActivityGraphFactory.createBaseGraph(data_set);
      int index = data_row.getIndex();
      OpGraphNode nodeForKey = graph.getNodeForKey(index);
      if (nodeForKey != null) {
         List nodeSuccessors = nodeForKey.getSuccessors();
         for (int i = 0; i < nodeSuccessors.size(); i++) {
            OpGraphNode node = (OpGraphNode) nodeSuccessors.get(i);
            //only one component in the graph - the data row index
            XComponent data = (XComponent) node.getComponents().get(0);
            dataRowSuccessors.add(data);
         }
      }
      return dataRowSuccessors;
   }

   /**
    * Validates the activities using the start points - startPoints
    */
   protected void validateGanttChart() {
      if (startPoints == null) {
         super.validateGanttChart();
      }
      else {

         //expand start points (collection will be replaced by all its children)
         expandStartPoints();

         //transform data set into graph
         graph = OpActivityGraphFactory.createBaseGraph(data_set);

         //consider only start points that don't have predecessors in the start point array
         for (Iterator iterator = startPoints.iterator(); iterator.hasNext();) {
            XComponent dataRow = (XComponent) iterator.next();
            OpGraphNode node = graph.getNodeForKey(dataRow.getIndex());
            if (node != null) {
               List preds = node.getPredecessors();
               for (Iterator predIterator = preds.iterator(); predIterator.hasNext();) {
                  OpGraphNode pred = (OpGraphNode) predIterator.next();
                  XComponent predDataRow = (XComponent) pred.getComponents().get(0);
                  if (startPoints.contains(predDataRow)) {
                     iterator.remove();
                     break;
                  }
               }
            }
         }

         //start validation for all start points
         for (Iterator iterator = startPoints.iterator(); iterator.hasNext();) {
            XComponent activity = (XComponent) iterator.next();
            validateActivity(activity);
         }
      }
   }

   /**
    * Replaces all collection from the startPoint list with their children.
    * Will keep only standard and milestones in start points lists
    */
   private void expandStartPoints() {
      Set newStarts;
      boolean expand = true;
      while (expand) {
         expand = false;
         newStarts = new HashSet();
         for (Iterator iterator = startPoints.iterator(); iterator.hasNext();) {
            XComponent activity = (XComponent) iterator.next();
            List children = subActivities(activity);
            if (children.size() != 0) {
               newStarts.addAll(children);
               expand = true;
            }
            else {
               if (getType(activity) == STANDARD || getType(activity) == MILESTONE || getType(activity) == SCHEDULED_TASK) {
                  newStarts.add(activity);
               }
            }
         }
         startPoints = newStarts;
      }
   }

   /**
    * Updates the type of the activities from the given activity upwards (parent relation)
    *
    * @param activity a <code>XComponent(DATA_ROW)</code> representing a client activity.
    */
   private void updateTreeType(XComponent activity) {
      if (activity == null) {
         return;
      }
      if (getType(activity) != updateTypeForActivity(activity) || startPoints.contains(activity)) {
         XComponent up = superActivity(activity);
         updateTreeType(up);
      }
   }


   /**
    * Set start as max of all its predecessors (next work day after it) - duration stays the same.
    * If old values are the same as the new ones -> stop validation.
    *
    * @param dataRow - activity to validate
    */
   private void validateActivity(XComponent dataRow) {

      OpGraphNode node = graph.getNodeForKey(dataRow.getIndex());
      List preds = new ArrayList();
      Date end = null;
      boolean validateSuccessors = true;
      if (node != null) {
         preds = node.getPredecessors();
      }


      if (!isCollectionType(dataRow)) {
         //get the last end date from predecessors ( end = maxend(preds) )
         for (Iterator iterator = preds.iterator(); iterator.hasNext();) {
            OpGraphNode pred = (OpGraphNode) iterator.next();
            //just one component/node
            XComponent predDataRow = (XComponent) pred.getComponents().get(0);
            Date predEnd = OpGanttValidator.getEnd(predDataRow);
            if (end == null) {
               end = predEnd;
            }
            else {
               end = end.before(predEnd) ? predEnd : end;
            }
         }

         if (end != null) {
            Date start = (getType(dataRow) == MILESTONE) ? end : calendar.nextWorkDay(end);
            Date oldStart = OpGanttValidator.getStart(dataRow);
            if (!oldStart.equals(start)) {
               //move the activity
               OpGanttValidator.setStart(dataRow, start);
            }
            else {
               //no change in the start date - stop validation (unless it is a start point)
               if (!startPoints.contains(dataRow)) {
                  validateSuccessors = false;
               }
            }
         }

         //check for the project start
         Date start = OpGanttValidator.getStart(dataRow);
         if (start != null) {
            if (!calendar.isWorkDay(start)) {
               start = calendar.nextWorkDay(start);
               validateSuccessors = true;
               OpGanttValidator.setStart(dataRow, start);
            }
            if (getWorkingProjectStart() != null) {
               if (start.before(getWorkingProjectStart())) {
                  validateSuccessors = true;
                  OpGanttValidator.setStart(dataRow, getWorkingProjectStart());
               }
            }
            Date oldFinish = OpGanttValidator.getEnd(dataRow);
            updateDuration(dataRow, OpGanttValidator.getDuration(dataRow));
            if (oldFinish != null && !oldFinish.equals(OpGanttValidator.getEnd(dataRow))) {
               validateSuccessors = true;
            }
            //update collection
            updateCollectionTreeValues(dataRow);
         }
      }

      //try to change the start/end of parent with new values of start/end
      if (node != null) {
         updateCollectionDates(superActivity(dataRow));
      }

      //validate all successors
      if (validateSuccessors && node != null) {
         List successors = node.getSuccessors();
         for (Iterator iterator = successors.iterator(); iterator.hasNext();) {
            OpGraphNode succNode = (OpGraphNode) iterator.next();
            validateActivity((XComponent) succNode.getComponents().get(0));
         }
      }
   }

   /**
    * Updates the start/end of the collections starting from an initial activity.
    * start = the min of children starts
    * end = the max of children ends
    *
    * @param collection a <code>XComponent(DATA_ROW)</code> a collection activity.
    */
   private void updateCollectionDates(XComponent collection) {
      if (collection == null) {
         return;
      }
      List children = subActivities(collection);

      Date startCollection = getStart(collection);
      Date endCollection = getEnd(collection);
      Date minStart = null, maxEnd = null;
      boolean changed = false;
      for (Iterator iterator = children.iterator(); iterator.hasNext();) {
         XComponent activity = (XComponent) iterator.next();
         Date start = getStart(activity);
         if (minStart == null) {
            minStart = start;
         }
         else {
            if (start.before(minStart)) {
               minStart = start;
            }
         }

         Date end = getEnd(activity);
         if (maxEnd == null) {
            maxEnd = end;
         }
         else {
            if (end.after(maxEnd)) {
               maxEnd = end;
            }
         }
      }

      if (startCollection == null || (minStart != null && !startCollection.equals(minStart))) {
         setStart(collection, minStart);
         changed = true;
      }

      if (endCollection == null || (maxEnd != null && !endCollection.equals(maxEnd))) {
         setEnd(collection, maxEnd);
         updateProjectPlanFinish();
         changed = true;
      }

      if (changed) {
         //update the duration
         updateFinish(collection, getEnd(collection));
         //update also the upper collections
         updateCollectionDates(superActivity(collection));
      }
   }


   /**
    * @see onepoint.express.XValidator#addDataRow(onepoint.express.XComponent)
    */
   public void addDataRow(XComponent data_row) {
      initStartPoints();
      startPoints.add(data_row);
      super.addDataRow(data_row);
   }

   /**
    * @see onepoint.express.XValidator#addDataRow(int,onepoint.express.XComponent)
    */
   public void addDataRow(int index, XComponent data_row) {
      initStartPoints();
      if (index < data_set.getChildCount()) {
         XComponent previousRow = (XComponent) data_set.getChild(index);
         startPoints.add(previousRow);
         XComponent parent = superActivity(previousRow);
         if (parent != null) {
            startPoints.add(parent);
         }
      }
      startPoints.add(data_row);
      super.addDataRow(index, data_row);
   }


   /**
    * Remove the link between the two given activities. No validation will occur.
    *
    * @param sourceIndex The index of the predecessor activity of the link
    * @param targetIndex The index of the successor activity of the link.
    */
   public void removeLink(int sourceIndex, int targetIndex) {
      initStartPoints();
      XComponent target = (XComponent) data_set.getChild(targetIndex);
      startPoints.add(target);
      super.removeLink(sourceIndex, targetIndex);
   }
}
