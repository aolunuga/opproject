/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.components;

import java.util.List;
import java.util.Map;

/**
 * @author mihai.costin
 */
public class OpGraph {

   /**
    * List of <code>OpGraphNode</code>
    */
   private List nodes;

   /**
    * Map of key <code>Integer</code>, value <code>OpGraphNode</code> that will hold a mapping from a key
    * to the corresponding graph node.
    */
   private Map mapping;

   /**
    * Matrix view of the graph
    */
   int[][] matrixGraph;

   /**
    * @param key key that is linked to a node in the graph
    * @return an OpGraphNode for the given key
    */
   public OpGraphNode getNodeForKey(int key) {
      return (OpGraphNode) mapping.get(new Integer(key));
   }

   /**
    * @return List of nodes for this graph. Elements are OpGraphNode
    */
   public List getNodes() {
      return nodes;
   }

   /**
    * @return Map from the external id Integer to the node OpGraphNode in the graph
    */
   public Map getMapping() {
      return mapping;
   }

   /**
    * Set the mapping for this graph
    *
    * @param mapping Map from the external id Integer to the node OpGraphNode in the graph
    */
   public void setMapping(Map mapping) {
      this.mapping = mapping;
   }

   /**
    * Set the new list of nodes for this graph. The method will take care that the id of a ndoe is the same as the
    * possition in the list.
    *
    * @param nodes
    */
   public void setNodes(List nodes) {
      this.nodes = nodes;
      for (int i = 0; i < nodes.size(); i++) {
         OpGraphNode node = (OpGraphNode) nodes.get(i);
         //set the node id to be equal to the possition in the list
         node.setId(new Integer(i));
      }
   }


   /**
    * Transforms the graph from node,successors representation to a matrix representation
    *
    * @return matrix representing the graph
    */
   public int[][] graphToMatrix() {
      int size = nodes.size();
      int[][] matrix = new int[size + 1][size];

      for (int i = 0; i < size; i++) {
         matrix[size][i] = 0;
      }

      //get all the nodes and put the successors info into the matrix
      for (int i = 0; i < size; i++) {
         OpGraphNode node = (OpGraphNode) nodes.get(i);
         List successors = node.getSuccessors();
         for (int j = 0; j < successors.size(); j++) {
            OpGraphNode succ = ((OpGraphNode) successors.get(j));
            int succId = succ.getId().intValue();
            matrix[i][succId] = 1;
            matrix[size][succId]++;
         }
      }
      return matrix;
   }

   /**
    * Detects if the graph has any cycles
    *
    * @return true if one cycle is found
    */
   public boolean hasCycles() {

      if (matrixGraph == null) {
         throw new IllegalStateException("Matrix Graph is null. addMatrixView() was not called on this graph");
      }

      int size = nodes.size();

      int[][] graph = new int[size + 1][size];
      for (int i = 0; i < size + 1; i++) {
         System.arraycopy(matrixGraph[i], 0, graph[i], 0, size);
      }

      boolean found = true;
      while (found) {
         found = false;
         //search for a column with sum 0
         for (int i = 0; i < size; i++) {
            if (graph[size][i] == 0) {
               found = true;
               //set 0 on line i
               for (int j = 0; j < size; j++) {
                  if (graph[i][j] == 1) {
                     graph[size][j]--;
                  }
                  graph[i][j] = 0;
               }
               graph[size][i] = -1;
            }
         }
      }
      //ends when no more nodes have sum 0 (all added or we have cycle)
      //if we have cycle - we have some value in the sum vector !=0 and !=-1
      for (int i = 0; i < size; i++) {
         if (graph[size][i] > 0) {
            return true;
         }
      }
      return false;
   }

   /**
    * Retrieves the node OpGraphNode for a given id.
    *
    * @param nodeId id searched for
    * @return the node with the id nodeId
    */
   public OpGraphNode getNodeForId(int nodeId) {
      return (OpGraphNode) nodes.get(nodeId);
   }

   /**
    * Adds also a matrix view of the graph to this instance
    */
   public void addMatrixView() {
      matrixGraph = graphToMatrix();
   }

}
