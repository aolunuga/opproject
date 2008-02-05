/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.persistence.hibernate;

import java.util.Arrays;
import java.util.EventObject;
import java.util.LinkedList;

import org.hibernate.event.EventSource;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.project.OpProjectSession;

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

   public static final int UPDATE = 1;
   public static final int INSERT = 2;
   public static final int DELETE = 4;
   public static final int PRE_FLUSH = 8;
   public static final int POST_FLUSH = 16;   
   
   public OpEvent(OpBroker broker, OpObject source, int action,
         String[] propertyNames, Object[] oldState, Object[] newState) {
      super(source);
      this.broker = broker;
//      this.sourceType = sourceType;
      this.action = action;
      this.propertyNames = propertyNames;
      this.oldState = oldState;
      this.newState = newState;
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
            if (oldState == null) { // add all ne values
               return pos;
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
}
