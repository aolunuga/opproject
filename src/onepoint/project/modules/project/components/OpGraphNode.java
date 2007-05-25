/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */
package onepoint.project.modules.project.components;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mihai.costin
 *         Date: Nov 14, 2005
 *         <p/>
 *         Each activity node will represent a level 0 dataRow and the activities, if any, that
 *         are "children" of this level 0 dataRow.
 *         The node will hold the ids of the successor nodes in the graph.
 */
public class OpGraphNode {

   /**
    * Identity of this node in the Graph.
    */
   private Integer nodeId;

   /**
    * ArrayList<code>OpGraphNode</code> of successors in graph.
    */
   private List successors;

   /**
    * ArrayList <code>OpGraphNode/code> of predecessors in graph.
    */
   private List predecessors;

   /**
    * The components that are contained in this activity node. List of Objects.
    */
   private List components;


   /**
    * Created a new Activity Node.
    */
   public OpGraphNode() {
      components = new ArrayList();
      successors = new ArrayList();
      predecessors = new ArrayList();
   }

   /**
    * Adds a new successor to this activity node.
    *
    * @param node - The OpGraphNode the successor node.
    */
   public void addSuccessor(OpGraphNode node) {
      //only if it's not already in the list
      if (!successors.contains(node)) {
         successors.add(node);
         //add also the predecessor link
         node.getPredecessors().add(this);
      }
   }

   /**
    * Adds a new data row (index from dataSet) in this node.
    */
   public void addComponent(Object component) {
      components.add(component);
   }

   /**
    * Returns the id of this node.
    *
    * @return id of this node.
    */
   public Integer getId() {
      return nodeId;
   }

   /**
    * Set a new Id for this node
    *
    * @param nodeId the new id
    */
   public void setId(Integer nodeId) {
      this.nodeId = nodeId;
   }

   /**
    * Retrieves the array of successors for this node.
    *
    * @return List of <code>OpGraphNode</code> with the successors
    */
   public List getSuccessors() {
      return successors;
   }

   /**
    * Retrieves the array of predecessors for this node.
    *
    * @return List of <code>OpGraphNode</code> with the successors
    */
   public List getPredecessors() {
      return predecessors;
   }

   /**
    * Retrieves the components of this activity node, representing the
    * indexes of the activities in the initial data set.
    *
    * @return <code>List</code> of <code>Integer</code>
    */
   public List getComponents() {
      return components;
   }
}
