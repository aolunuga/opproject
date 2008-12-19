/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.project.modules.project;

import onepoint.persistence.OpLocatable;
import onepoint.project.modules.custom_attribute.OpActionType;

/**
 * @author dfreis
 *
 */
public interface OpActionIfc extends OpLocatable {

   public final static int NOT_STARTED = 0;
   public final static int STARTED = 1;
   public final static int DONE = 2;

   /**
    * @return the name
    */
   public abstract String getName();

   public abstract void setName(String name);

   /**
    * @return the description
    */
   public abstract String getDescription();

   public abstract void setDescription(String description);

   /**
    * @return the activity
    */
   public abstract OpActivityIfc getActivity();

   /**
    * @return the type
    */
   public abstract OpActionType getActionType();

   public boolean isActionTypeBased();

   /**
    * @return the status
    */
   public abstract int getStatus();

   /**
    * @return
    * @pre
    * @post
    */
   public abstract boolean isDeleted();

   /**
    * @param status
    * @pre
    * @post
    */
   public abstract void setStatus(int status);

   /**
    * @param b
    * @pre
    * @post
    */
   public abstract void setDeleted(boolean b);

}