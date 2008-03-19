/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A General Purpose directed Graph implementation.
 * Supports topologic order.
 * 
 * NOTE: preserves natural order! this is important in order to work for remove all objects
 * 
 * @author dfreis
 */

public class OpGraph {
  private LinkedHashSet rootNodes;
  private HashMap mapping;
  
  /**
   * Default Constructor
   */
  public OpGraph() {
     rootNodes = new LinkedHashSet();
     mapping = new HashMap();
  }
  
  /**
   * Adds a node to the graph that wraps <code>toAdd</code>.
   * If there is already a wrapped node for <code>toAdd</code> this one is returned, 
   * otherwise a new created and added node is returned 
   * 
   * @param toAdd the object that should be added to the graph.
   * @return a new created and added node or an olready existing one.
   */
  public synchronized Entry addNode(Object toAdd) {
     Entry exists = (Entry) mapping.get(toAdd);
     if (exists != null) {
        return exists;
     }
     Entry node = new Entry(toAdd);
     mapping.put(toAdd, node);
     rootNodes.add(node);
     return node;
  }
  
  /**
   * returns the wrapped node for the given <code>lookup</code>.
   * @param lookup the object to look for a wrapped node.
   * @return the wrapped node or null if non was found.
   */
  public Entry getNode(Object lookup) {
     return (Entry)mapping.get(lookup);
  }
  
  /**
   * Inserts a new directed edge from <code>source</code> to <code>destination</code>.
   * @param source the starting point for the edge.
   * @param destination the endpoint for the edge.
   */
  public synchronized void addEdge(Entry source, Entry destination) {
     if (destination.backEdges.isEmpty()) {
        rootNodes.remove(destination);
     }
     source.edges.add(destination);
     destination.backEdges.add(source);
  }
  
  /**
   * Removed a directed edge from <code>source</code> to <code>destination</code>.
   * @param source the starting point for the edge.
   * @param destination the endpoint for the edge.
   */
  public synchronized void removeEdge(Entry source, Entry destination) {
     source.edges.remove(destination);
     destination.backEdges.remove(source);
     if (destination.backEdges.isEmpty()) {
        rootNodes.add(destination);
     }
  }
  
  /**
   * Removes the given node.
   * @param toRemove the node to remove.
   */
  public synchronized void removeEntry(Entry toRemove) {
     Iterator iter = toRemove.backEdges.iterator();
     while (iter.hasNext()) {
        Entry source = (Entry)iter.next();
        removeEdge(source, toRemove);
     }
     iter = toRemove.edges.iterator();
     while (iter.hasNext()) {
        Entry destination = (Entry)iter.next(); 
        removeEdge(toRemove, destination);
     }
  }

  /**
   * Gets all root nodes which are noded that do not have an edge to the node.
   * @return the root nodes.
   */
  public Set getRootNodes() {
     return Collections.unmodifiableSet(rootNodes);
  }

 /* (non-Javadoc)
 * @see java.lang.Object#toString()
 */
  public String toString() {
     StringBuffer ret = new StringBuffer();
     int depth = 0;
     Iterator iter = rootNodes.iterator();
     while (iter.hasNext()) {
        Entry entry = (Entry)iter.next();
        append(ret, entry, depth);
     }
     return ret.toString();
  }

  /**
   * @param ret
   * @param entry
   * @param depth
   * @pre
   * @post
   */
  private void append(StringBuffer ret, Entry entry, int depth) {
     for (int count = 0; count < depth; count++) {
        ret.append(" ");
     }
     ret.append(entry.getElem());
     ret.append('\n');
 
     Iterator iter = entry.edges.iterator();
     while (iter.hasNext()) {
        Entry child = (Entry)iter.next();
        append(ret, child, depth+1);
     }
  }

  /**
   * returns a topologic order of the graph.
   * @return the topologic order.
   */
  public synchronized List getTopologicOrder() {
     // L : Empty list where we put the sorted elements
     // Q : Set of all nodes with no incoming edges
     // while Q is non-empty do
     //   remove a node n from Q
     //   insert n into L
     //   for each node m with an edge e from n to m do
     //     remove edge e from the graph
     //     if m has no other incoming edges then
     //       insert m into Q
     // if graph has edges then
     //   output error message (graph has a cycle)
     // else 
     //   output message (proposed topologically sorted order: L)

     LinkedList removedEdges = new LinkedList(); 
     LinkedList ret = new LinkedList();
     LinkedHashSet queue = new LinkedHashSet(rootNodes);
     while (true) {
        Iterator iter = queue.iterator();
        if (!iter.hasNext()) {
           break;
        }
        Entry entry = (Entry)iter.next();
        iter.remove();
        //queue.remove(entry);
        ret.add(entry);
        
        while (true) {
           Iterator edgeIter = entry.edges.iterator();
           if (!edgeIter.hasNext()) {
              break;
           }
           Entry edge = (Entry)edgeIter.next();
           edgeIter.remove();

           removeEdge(entry, edge);
           removedEdges.add(entry);
           removedEdges.add(edge);
           
           if (edge.backEdges.isEmpty()) {
              queue.add(edge);
           }
        }
     }
     boolean cycle = false;
     
     Iterator iter = queue.iterator();
     while (iter.hasNext()) {
        Entry node = (Entry)iter.next();
        if (!node.edges.isEmpty()) {
           cycle = true;
           break;
        }
     }
     if (cycle) {
        throw new IllegalStateException("cycle");
     }
     
     iter = removedEdges.iterator();
     while (iter.hasNext()) {
        addEdge((Entry)iter.next(), (Entry)iter.next());
     }
     
     return ret;
  }

  /**
   * @author dfreis
   *
   */
  static public class Entry {

     private Object elem;
     private LinkedHashSet edges;
     private LinkedHashSet backEdges;

     /**
      * Constructor
      * @param elem the object to wrap
      */
     private Entry(Object elem) {
        this.elem = elem;
        edges = new LinkedHashSet();
        backEdges = new LinkedHashSet();
     }

     /**
      * returns the wrapped object.
      * @return the wrapped object.
      */
     public Object getElem() {
        return elem;
     }

     /**
      * Gets all edges that start at this node.
      * @return all edges starting at this node.
      */
     public Set getEdges() {
        return Collections.unmodifiableSet(edges);
     }

     /**
      * Gets all edges that end at this node.
      * @return all edges ending at this node.
      */
     public Set getBackEdges() {
        return Collections.unmodifiableSet(backEdges);
     }

      public String toString() {
         return elem.toString();
      }
     
  }
}