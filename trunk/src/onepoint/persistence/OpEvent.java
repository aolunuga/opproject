/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.persistence;

import java.util.EventObject;
import java.util.LinkedList;

/**
 * @author dfreis
 *
 */
public class OpEvent extends EventObject {
   //private OpProjectSession session;
   
   private int action;
   private Object[] oldState;
   private Object[] newState;
   private Class sourceType;
   private OpBroker broker;
   private String[] propertyNames;
   private Object[] initialState;
   private boolean last;

   public static final int UPDATE = 1;
   public static final int INSERT = 2;
   public static final int DELETE = 4;
   public static final int PRE_FLUSH = 8;
   public static final int POST_FLUSH = 16;
   public static final int[] ALL_EVENTS = { UPDATE, INSERT, DELETE, PRE_FLUSH, POST_FLUSH };   
   
   public OpEvent(OpBroker broker, OpObjectIfc obj, int action,
         String[] propertyNames, Object[] oldState, Object[] newState, Object[] initialState, boolean last) {
      super(obj);
      this.broker = broker;
      this.action = action;
      this.propertyNames = propertyNames;
      this.oldState = oldState;
      this.newState = newState;
      this.last = last;
      this.initialState = initialState;
   }

   public void setAction(int action) {
      this.action = action;
   }

   public Class getSourceType() {
      return source.getClass();
   }
   
   public OpBroker getBroker() {
      return broker;
   }
   
   public int getAction() {
      return action;
   }
   
   public Object[] getOldState() {
      return oldState;
   }
   
   public Object[] getNewState() {
      return newState;
   }

   public String[] getPropertyNames() {
      return propertyNames;
   }

   public Object[] getInitialState() {
      return initialState;
   }

   public boolean isLast() {
      return last;
   }

   /**
    * @return
    * @pre
    * @post
    */
   public int[] getChangedPropertyPos() {
      int max = 0;
      if (oldState != null) {
         max = oldState.length;
      }
      else if (newState != null) {
         max = newState.length;
      }
         
      LinkedList<Integer> values = new LinkedList<Integer>();
      for (int pos = 0; pos < max; pos++) {
         if (oldState == null) { // add all ne values
            values.add(pos);
         }
         if (oldState[pos] == null) {
            if (newState[pos] != null) {
               values.add(pos);               
            }
         }
         else if ((oldState[pos] != newState[pos]) && (!oldState[pos].equals(newState[pos]))) {
            values.add(pos);
         }
//         values.add(pos);
      }
      int[] ret = new int[values.size()];
      int pos = 0;
      for (Integer value : values) {
         ret[pos] = value.intValue();
         pos++;
      }
      return ret;
   }

   /**
    * @return
    * @pre
    * @post
    */
   public int[] getInitialChangedPropertyPos() {
      int max = 0;
      if (initialState != null) {
         max = initialState.length;
      }
      else if (newState != null) {
         max = newState.length;
      }
         
      LinkedList<Integer> values = new LinkedList<Integer>();
      for (int pos = 0; pos < max; pos++) {
         if (initialState == null) { // add all ne values
            values.add(pos);
         }
         if (initialState[pos] == null) {
            if (newState[pos] != null) {
               values.add(pos);               
            }
         }
         else if ((initialState[pos] != newState[pos]) && (!initialState[pos].equals(newState[pos]))) {
            values.add(pos);
         }
//         values.add(pos);
      }
      int[] ret = new int[values.size()];
      int pos = 0;
      for (Integer value : values) {
         ret[pos] = value.intValue();
         pos++;
      }
      return ret;
   }

   /**
    * @param version_number
    * @return
    * @pre
    * @post
    */
   public int getChangedPropertyPos(String name) {
      int max = 0;
      if (oldState != null) {
         max = oldState.length;
      }
      else if (newState != null) {
         max = newState.length;
      }
         
      for (int pos = 0; pos < max; pos++) {
         if (name.equals(propertyNames[pos])) {
            if (oldState == null) { // add all new values
               return newState[pos] == null ? -1 : pos;
            }
            if (newState == null) {
                return oldState[pos] == null ? -1 : pos;
             }
            if (oldState[pos] == null) {
               if (newState[pos] != null) {
                  return pos;
               }
            }
            else if ((oldState[pos] != newState[pos]) && (!oldState[pos].equals(newState[pos]))) {
               return pos;
            }
            return -1;
         }
      }
      return -1;
   }

   /**
    * @param version_number
    * @return
    * @pre
    * @post
    */
   public int getInitialChangedPropertyPos(String name) {
      int max = 0;
      if (initialState != null) {
         max = initialState.length;
      }
      else if (newState != null) {
         max = newState.length;
      }
         
      for (int pos = 0; pos < max; pos++) {
         if (name.equals(propertyNames[pos])) {
            if (initialState == null) { // add all new values
               return newState[pos] == null ? -1 : pos;
            }
            if (initialState[pos] == null) {
               if (newState[pos] != null) {
                  return pos;
               }
            }
            else if ((initialState[pos] != newState[pos]) && (!initialState[pos].equals(newState[pos]))) {
               return pos;
            }
            return -1;
         }
      }
      return -1;
   }

/* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      OpEvent other = (OpEvent)obj;
      return (other.action == action &&
                    other.sourceType == sourceType &&
                    other.last == last &&
                    other.getSource() == getSource());
   }
//   private int action;
//   private Object[] oldState;
//   private Object[] newState;
//   private Class sourceType;
//   private OpBroker broker;
//   private String[] propertyNames;
//   private Object[] initialState;
//   private boolean last;

}
