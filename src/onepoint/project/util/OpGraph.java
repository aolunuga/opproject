/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

/**
 * A General Purpose directed Graph implementation.
 * Supports topologic order.
 * 
 * NOTE: preserves natural order! this is important in order to work for remove all objects
 * 
 * @author dfreis
 */

public class OpGraph {
   private static final XLog logger = XLogFactory.getLogger(OpGraph.class);

  private LinkedHashSet rootNodes;
  private HashMap mapping;
  
  private final static Integer DEFAULT_EDGE_CLASS = new Integer(0);
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
     addEdge(source, destination, DEFAULT_EDGE_CLASS);
  }
  
  public synchronized void addEdge(Entry source, Entry destination, Object edgeClass) {
     if (!destination.hasBackEdges()) {
        rootNodes.remove(destination);
     }
     source.addEdge(edgeClass, destination);
     destination.addBackEdge(edgeClass, source);
  }
  
  /**
   * Removed a directed edge from <code>source</code> to <code>destination</code>.
   * @param source the starting point for the edge.
   * @param destination the endpoint for the edge.
   */
  public synchronized Set removeEdge(Entry source, Entry destination) {
     Set classesRemoved = source.removeEdgesToDestination(destination);
     destination.removeEdgesFromSource(source);
     if (!destination.hasBackEdges()) {
        rootNodes.add(destination);
     }
     return classesRemoved;
  }
  
  /**
   * Removes the given node.
   * @param toRemove the node to remove.
   */
  public synchronized void removeEntry(Entry toRemove) {
     Iterator iter = toRemove.backEdgeIterator();
     while (iter.hasNext()) {
        Entry source = (Entry)iter.next();
        removeEdge(source, toRemove);
     }
     iter = toRemove.edgeIterator();
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
 
     Iterator iter = entry.edgeIterator();
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
           Iterator edgeIter = entry.edgeIterator();
           if (!edgeIter.hasNext()) {
              break;
           }
           Entry edge = (Entry)edgeIter.next();
           // edgeIter.remove();

           Set cr = removeEdge(entry, edge);
           removedEdges.add(cr);
           removedEdges.add(entry);
           removedEdges.add(edge);
           
           if (!edge.hasBackEdges()) {
              queue.add(edge);
           }
        }
     }
     boolean cycle = false;
     
     Iterator iter = mapping.values().iterator();
     while (iter.hasNext()) {
        Entry node = (Entry)iter.next();
        if (node.hasEdges()) {
           StringBuffer buffer = new StringBuffer();
           Iterator nodeIter = node.edgeIterator();
           while (nodeIter.hasNext()) {
              Object obj = nodeIter.next();
              buffer.append(obj);
           }
           logger.warn("cycle: "+buffer.toString());
           cycle = true;
           break;
        }
     }
     if (cycle) {
        throw new IllegalStateException("cycle");
     }
     
     iter = removedEdges.iterator();
     while (iter.hasNext()) {
        Iterator cit = ((Set) iter.next()).iterator();
        Entry src = (Entry)iter.next();
        Entry dest = (Entry)iter.next();
        while (cit.hasNext()) {
           Object c = cit.next();
           addEdge(src, dest, c);
        }
     }
     
     return ret;
  }


  /**
   * @author dfreis
   *
   */
  static public class Entry {

     private Object elem;
     private Map edgeSets;
     private Map backEdgeSets;
     
     private static final Set EMPTY_SET = new HashSet();

     /**
      * Constructor
      * @param elem the object to wrap
      */
     private Entry(Object elem) {
        this.elem = elem;
        edgeSets = new HashMap();
        backEdgeSets = new HashMap();
     }

     /**
      * returns the wrapped object.
      * @return the wrapped object.
      */
     public Object getElem() {
        return elem;
     }

     public void addEdge(Object edgeClass, Entry destination) {
        Set edgesForClass = (Set) edgeSets.get(edgeClass);
        if (edgesForClass == null) {
           edgesForClass = new LinkedHashSet();
           edgeSets.put(edgeClass, edgesForClass);
        }
        edgesForClass.add(destination);
     }

     public Set removeEdgesToDestination(Object destination) {
        Map sets = edgeSets;
        return removeAllEdges(destination, sets);
     }

     public Set removeEdgesFromSource(Object source) {
        Map sets = backEdgeSets;
        return removeAllEdges(source, sets);
     }

      private Set removeAllEdges(Object elem, Map sets) {
         Iterator sit = sets.entrySet().iterator();
           Set edgeClassesRemoved = new HashSet();
           while (sit.hasNext()) {
              Map.Entry e = (Map.Entry) sit.next();
              if (((Set) e.getValue()).remove(elem)) {
                 edgeClassesRemoved.add(e.getKey());
              }
           }
           return edgeClassesRemoved;
      }
     
     private boolean removeEdgeForClass(Map edgeSets, Object edgeClass, Object elem) {
        Set es = (Set) edgeSets.get(edgeClass);
        if (es != null) {
           return es.remove(elem);
        }
        return false;        
     }
     
     public void addBackEdge(Object edgeClass, Entry source) {
        Set edgesForClass = (Set) backEdgeSets.get(edgeClass);
        if (edgesForClass == null) {
           edgesForClass = new LinkedHashSet();
           backEdgeSets.put(edgeClass, edgesForClass);
        }
        edgesForClass.add(source);
     }
     
     public boolean hasEdges() {
        return edgeSetNotEmpty(edgeSets);
     }

     public boolean hasBackEdges() {
        return edgeSetNotEmpty(backEdgeSets);
     }

      private boolean edgeSetNotEmpty(Map sets) {
         Iterator kit = sets.values().iterator();
         while (kit.hasNext()) {
            Set ec = (Set) kit.next();
            if (!ec.isEmpty()) {
               return true;
            }
         }
         return false;
      }
      
      public Iterator edgeIterator() {
         return new EdgeIterator(edgeSets);
      }
      
      public Iterator backEdgeIterator() {
         return new EdgeIterator(backEdgeSets);
      }

      public Set getEdgeCLasses() {
         return Collections.unmodifiableSet(edgeSets.keySet());
      }

      public Set getBackEdgeCLasses() {
         return Collections.unmodifiableSet(backEdgeSets.keySet());
      }
     /**
      * Gets all edges that start at this node.
      * @return all edges starting at this node.
      */
     public Set getEdges(Object edgeClass) {
        Set edges = (Set) edgeSets.get(edgeClass);
        return Collections.unmodifiableSet(edges != null ? edges : EMPTY_SET);
     }

     /**
      * Gets all edges that end at this node.
      * @return all edges ending at this node.
      */
     public Set getBackEdges(Object edgeClass) {
        Set edges = (Set) backEdgeSets.get(edgeClass);
        return Collections.unmodifiableSet(edges != null ? edges : EMPTY_SET);
     }

      public String toString() {
         return elem.toString();
      }
     
   }
  
   public static class EdgeIterator implements Iterator {

      Iterator classIterator = null;
      Iterator edgeIterator = null;
      
      Map edgeSet = null;
      
      public EdgeIterator(Map edgeSet) {
         this.edgeSet = edgeSet;   
         edgeIterator = nextEdgeIterator();
      }

      public boolean hasNext() {
         while (edgeIterator != null && !edgeIterator.hasNext()) {
            edgeIterator = nextEdgeIterator();
         }
         return edgeIterator != null;
      }

      public Object next() {
         if (!hasNext()) {
            throw new NoSuchElementException();
         }
         return edgeIterator.next();
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }

      private Iterator nextEdgeIterator() {
         if (classIterator == null) {
            classIterator = edgeSet.entrySet().iterator();
         }
         if (classIterator.hasNext()) {
            Set ec = (Set) ((Map.Entry)classIterator.next()).getValue();
            return ec.iterator();
         }
         return null;
      }
      
   }

}