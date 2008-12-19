/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.custom_attribute.OpActionType;

/**
 * @author dfreis
 *
 */
public class OpActionVersion extends OpObject implements OpActionIfc {

   private OpActionType actionType;
   String name;
   String description;
   private OpActivityVersion activityVersion;
   private boolean deleted;
//   private int status = NOT_STARTED;
   private OpAction action;
      
   /**
    * for internal use only 
    */
   private OpActionVersion() {
   }

   /**
    *  Constructor for activity based ctions
    */
   public OpActionVersion(String name, String description, int status) {
      this();
      this.name = name;
      this.description = description;
//      this.status = status;
      //action = new OpAction()
      setAction(new OpAction(status));
   }

   /**
    * 
    */
   public OpActionVersion(OpActionType type) {
      this();
      this.actionType = type;
      setAction(new OpAction());
   }

   /**
    * @param action
    * @deprecated used for upgrade only required for amazone, once they run version 8.1 this method and all within the call stack can be removed
    */
   public OpActionVersion(OpAction action) {
      this();
      this.actionType = action.getActionType();
//      this.name = action instanceof OpAction ? ((OpAction)action).name : ((OpActionVersion)action).name;
//      this.description = action instanceof OpAction ? ((OpAction)action).description : ((OpActionVersion)action).description;
      this.name = action.getName();
      this.description = action.getDescription();
      this.deleted = action.isDeleted();
//      this.status = action.getStatus();
      action.addActionVersion(this);
   }

   /**
    * @param srcAction 
    * 
    */
   public OpActionVersion(OpActionVersion srcAction, boolean independentVersion) {
      this();
      update(srcAction);
      if (independentVersion) {
         action = new OpAction(srcAction.getAction().getStatus());
      }
      action.addActionVersion(this);
   }

   
   void setAction(OpAction action) {
      this.action = action;
   }
   
   public OpAction getAction() {
      return action;
   }

   public boolean isActionTypeBased() {
      return actionType != null;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpActionIfc#getActionType()
    */
   public OpActionType getActionType() {
      return actionType;
   }

   public void setActionType(OpActionType type) {
      this.actionType = type;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpActionIfc#getActivity()
    */
   public OpActivityVersion getActivity() {
      return activityVersion;
   }

   void setActivity(OpActivityVersion activityVersion) {
      this.activityVersion = activityVersion;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpActionIfc#getDescription()
    */
   public String getDescription() {
      if ((name == null || name.length() == 0) && actionType != null) {
         return actionType.getDescription();
      }
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpActionIfc#getName()
    */
   public String getName() {
      if ((name == null || name.length() == 0) && actionType != null) {
         return actionType.getName();
      }
      return name;
   }
   
   public void setName(String name) {
      this.name = name;
   }

   /**
    * @deprecated
    */
   public void setStatus(int status) {
//      this.status = status;
   }

   /**
    * @deprecated
    */
   public int getStatus() {
      if (action != null) {
         return action.getStatus();
      }
      return 0;
//      return status;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpActionIfc#isDeleted()
    */
   public boolean isDeleted() {
      return deleted;
   }
   
   public void setDeleted(boolean deleted) {
      this.deleted = deleted;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return this.getClass().getSimpleName()+":id: "+getId()+", n="+getName()+", d="+getDescription()+", del="+deleted+", status: "+getStatus();
   }

   /**
    * @param action2
    * @pre
    * @post
    */
   public void update(OpActionVersion srcAction) {
      this.actionType = srcAction.actionType;
      this.name = srcAction.name;
      this.description = srcAction.description;
      this.deleted = srcAction.deleted;
//      this.status = srcAction.status;
      this.action = srcAction.action;
   }

}
