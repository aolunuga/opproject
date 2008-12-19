/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.project.modules.project;

/**
 * @author dfreis
 *
 */
public interface OpDependencyIfc {

//   public abstract void setPredecessorActivity(OpActivity predecessorActivity);

   public abstract OpActivityIfc getPredecessorActivity();

//   public abstract void setSuccessorActivity(OpActivityIfc successorActivity);

   public abstract OpActivityIfc getSuccessorActivity();

   public abstract int getDependencyType();

   public abstract void setDependencyType(int type);

   public abstract boolean getAttribute(int key);

}