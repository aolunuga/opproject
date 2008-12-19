/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.project;

import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.custom_attribute.OpActionType;

/**
 * @author dfreis
 *
 */
public class OpAction extends OpObject implements OpActionIfc {

   private OpActionType actionType;
   String name;
   String description;
   private OpActivity activity;
   private boolean deleted;
   private int status = NOT_STARTED;
   private Set<OpActionVersion> actionVersions;

   /**
    * 
    */
   public OpAction() {
   }
   
//   /**
//    *  Constructor for activity based ctions
//    */
//   public OpAction(String name, String description) {
//      this(name, description, NOT_STARTED);
//   }
   

//   /**
//    *  Constructor for activity based ctions
//    */
//   public OpAction(String name, String description, int status) {
//      this();
//      this.name = name;
//      this.description = description;
//      this.status = status;
//   }
///**
//    * 
//    */
//   public OpAction(OpActionType type) {
//      this();
//      this.actionType = type;
//   }
   
   /**
    * This Constructor may only be called from the very first actionVersion (no other actionVersion for this activity exist)
    * 
    * @param version
    */
   public OpAction(OpActionVersion version) {
      this();
      this.actionType = version.getActionType();      this.name = version.name;
      this.description = version.description;
//      this.name = version instanceof OpAction ? ((OpAction)version).name : ((OpActionVersion)version).name;
//      this.description = version instanceof OpAction ? ((OpAction)version).description : ((OpActionVersion)version).description;
      this.deleted = version.isDeleted();
      this.status = version.getStatus();
      addActionVersion(version);
   }
   
   /**
    * @param status2
    */
   public OpAction(int status) {
      this();
      setStatus(status);
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

   public boolean isActionTypeBased() {
      return actionType != null;
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpActionIfc#getActivity()
    */
   public OpActivity getActivity() {
      return activity;
   }
   
   void setActivity(OpActivity activity) {
     this.activity = activity;
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
    * @see onepoint.project.modules.project.OpActionIfc#isDeleted()
    */
   public boolean isDeleted() {
      return deleted;
   }
   
   public void setDeleted(boolean deleted) {
      this.deleted = deleted;
   }
   
   public void setStatus(int status) {
      this.status = status;
   }
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpActionIfc#getStatus()
    */
   public int getStatus() {
      return status;
   }
   
   public Set<OpActionVersion> getActionVersions() {
      return actionVersions;
   }

   private void setActionVersions(Set<OpActionVersion> actionVersions) {
      this.actionVersions = actionVersions;
   }

   /**
    * @param opAction
    * @pre
    * @post
    */
   public void addActionVersion(OpActionVersion actionVersion) {
      if (getActionVersions() == null) {
         setActionVersions(new HashSet<OpActionVersion>());
      }
      getActionVersions().add(actionVersion);
      actionVersion.setAction(this);
   }
   
   public void removeActionVersion(OpActionVersion actionVersion) {
      if (getActionVersions() != null) {
         getActionVersions().remove(actionVersion);
      }
      actionVersion.setAction(null);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return this.getClass().getSimpleName()+":id: "+getId()+", n="+getName()+", d="+getDescription()+", del="+deleted+", status: "+status;
   }

   /**
    * 
    * @pre
    * @post
    */
   public void unlinkFromActionVersions() {
      Set<OpActionVersion> versions = getActionVersions();
      if (versions != null) {
         for (OpActionVersion version : versions) {
            removeActionVersion(version);
         }
      }
   }

   /**
    * @param toCopy
    * @pre
    * @post
    */
   public void update(OpActionVersion toCopy) {
      actionType = toCopy.getActionType();
      name = toCopy.getName();
      description = toCopy.getDescription();
      deleted = toCopy.isDeleted();
   }
   
}
