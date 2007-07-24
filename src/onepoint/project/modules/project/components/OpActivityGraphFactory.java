/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.components;

import onepoint.express.XComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mihai.costin
 *         <p/>
 *         Transforms a given data set into an activity graph (OpGraph). The mapping from activities to nodes is 1-to-1.
 *         The links between the activity nodes are created as follows:
 *         for each activity, are added as successors all its direct successors and all the successors af its parent
 *         collections.
 *         When adding a collection as a successors, all the children af that collection are added as successors as well
 *         (expand the list of successors).
 *         The same process will take place also for the predecessors, and the successors will be added for the
 *         predecessors activities (if a is a predecessor for b, b is a successor for a).
 */
public class OpActivityGraphFactory {

   /**
    * Creates a graph <code>OpGraph</code> object out of a given data set.
    * This can be used to detect cycles
    *
    * @param dataSet data set to be used when creating the graph
    * @return graph obtained by transforming the data set
    */
   public static OpGraph createGraph(XComponent dataSet){
      OpGraph graph = createBaseGraph(dataSet);
      graph.addMatrixView();
      return graph;
   }

   /**
    * Creates a base graph. It lacks the matrix view.
    *
    * @param dataSet data set to be used when creating the graph
    * @return graph obtained by transforming the data set
    */
   public static OpGraph createBaseGraph(XComponent dataSet) {
      OpGraph graph = new OpGraph();
      List activityNodes = new ArrayList();
      Map mapping = new HashMap();
      OpGraphNode node;

      //create nodes
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         if (addToGraph(dataRow)) {
            node = new OpGraphNode();
            activityNodes.add(node);
            node.addComponent(dataRow);
            mapping.put(new Integer(dataRow.getIndex()), node);
         }
      }
      graph.setMapping(mapping);
      graph.setNodes(activityNodes);

      //create links
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         node = graph.getNodeForKey(dataRow.getIndex());
         if (node == null) {
            continue;
         }
         //successors given by OpGanttValidator is an array of dataRow indexes
         List successors = getAllSuccessors(dataSet, dataRow);
         //add all links given by the successors of this data row to the graph
         for (int j = 0; j < successors.size(); j++) {
            int succ = ((Integer) successors.get(j)).intValue();
            OpGraphNode succNode = graph.getNodeForKey(succ);
            if (succNode != null) {
               node.addSuccessor(succNode);
            }
         }
         //predecessors given by OpGanttValidator is an array of dataRow indexes
         List predecessors = getAllPredecessors(dataSet, dataRow);
         //add all links given by the predecessors of this data row to the graph
         //a predecessor x for y <=> a successor y for x
         for (int j = 0; j < predecessors.size(); j++) {
            int pred = ((Integer) predecessors.get(j)).intValue();
            OpGraphNode predNode = graph.getNodeForKey(pred);
            if (predNode != null) {
               predNode.addSuccessor(node);
            }
         }
      }
      return graph;
   }

   private static boolean addToGraph(XComponent dataRow) {
      if (OpGanttValidator.getType(dataRow) == OpGanttValidator.TASK) {
         return false;
      }
      return OpGanttValidator.getType(dataRow) != OpGanttValidator.COLLECTION_TASK;
   }

   /**
    * Gets all the direct successors of the activity and of the collections above starting from dataRow
    *
    * @param dataSet
    * @param dataRow
    * @return all the successors of the activity and of the collections above
    */
   private static List getAllSuccessors(XComponent dataSet, XComponent dataRow) {

      List succs;
      //add the direct successors to the list
      succs = new ArrayList(OpGanttValidator.getSuccessors(dataRow));

      XComponent parent = getParent(dataRow, dataSet);
      if (parent != null) {
         //add also the successors of the parent to the list
         succs.addAll(getAllSuccessors(dataSet, parent));
      }

      //expand the list
      List expanded = expand(dataSet, succs);
      List successors = new ArrayList();
      for (int i = 0; i < expanded.size(); i++) {
         Integer element = (Integer) expanded.get(i);
         if (!successors.contains(element)) {
            successors.add(element);
         }
      }
      return successors;
   }

   /**
    * Gets all the direct predecessors of the activity and of the collections above starting from dataRow
    *
    * @param dataSet
    * @param dataRow
    * @return all the successors of the activity and of the collections above
    */
   private static List getAllPredecessors(XComponent dataSet, XComponent dataRow) {
      List preds;
      //add the direct successors to the list
      preds = new ArrayList(OpGanttValidator.getPredecessors(dataRow));

      XComponent parent = getParent(dataRow, dataSet);
      if (parent != null) {
         //add also the successors of the parent to the list
         preds.addAll(getAllPredecessors(dataSet, parent));
      }

      //expand the list
      List expanded = expand(dataSet, preds);
      List predecessors = new ArrayList();
      for (int i = 0; i < expanded.size(); i++) {
         Integer element = (Integer) expanded.get(i);
         if (!predecessors.contains(element)) {
            predecessors.add(element);
         }
      }
      return predecessors;
   }

   /**
    * Returns the parent of the given dataRow from the given dataSet
    *
    * @param dataRow row for which to find the parent
    * @param dataSet contins all the rows from the data set
    * @return the parent of the given row
    */
   private static XComponent getParent(XComponent dataRow, XComponent dataSet) {
      XComponent parent = null;
      int index = dataRow.getIndex();
      for (int i = index; i >= dataSet.getChild(0).getIndex(); i--) {
         XComponent row = (XComponent) dataSet.getChild(i);
         //a super-activiy
         if (row.getOutlineLevel() < dataRow.getOutlineLevel()) {
            parent = row;
            break;
         }
      }
      return parent;
   }

   /**
    * Expands the dataIndexes in the given list. (Adds for a collection all its children)
    *
    * @param dataIndexes the list to be expanded
    * @param dataSet     the list with all the data
    * @return the list compose out of elements of the list plus the children of the
    *         collections in the list.
    */
   private static List expand(XComponent dataSet, List dataIndexes) {
      List expanded = new ArrayList(dataIndexes);

      for (int i = 0; i < dataIndexes.size(); i++) {

         Integer dataIndex = (Integer) dataIndexes.get(i);
         int index = dataIndex.intValue();
         XComponent dataRow = (XComponent) dataSet.getChild(index);

         for (int j = index + 1; j < dataSet.getChildCount(); j++) {
            XComponent nextDataRow = (XComponent) dataSet.getChild(j);
            if (dataRow.getOutlineLevel() < nextDataRow.getOutlineLevel()) {
               Integer newIndex = new Integer(nextDataRow.getIndex());
               if (!expanded.contains(newIndex)) {
                  expanded.add(newIndex);
               }
            }
            else {
               break;
            }
         }
      }
      return expanded;
   }
}
