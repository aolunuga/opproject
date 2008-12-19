/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAction;
import onepoint.project.modules.project.OpActionVersion;

/**
 * @author dfreis
 *
 */
public class OpActionType extends OpObject {

   private OpCustomType type;
   private String name;
   private String description;
   private boolean mandatory = true;
   private Set<OpAction> actions;
   private Set<OpActionVersion> actionVersions;
   
   /**
    * 
    */
   public OpActionType() {
   }
   /**
    * @param type
    * @param name
    * @param description
    * @param mandatory
    */
   public OpActionType(OpCustomType type, String name,
         String description, boolean mandatory) {
      this.type = type;
      this.name = name;
      this.description = description;
      this.mandatory = mandatory;
   }

   /**
    * @return the type
    */
   public OpCustomType getCustomType() {
      return type;
   }

    public void setCustomType(OpCustomType type) {
      this.type = type;
   }

   /**
    * @return the name
    */
   public String getName() {
      return name;
   }
   
   public void setName(String name) {
      this.name = name;
   }

   /**
    * @return the description
    */
   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   /**
    * @return the mandatory
    */
   public boolean isMandatory() {
      return mandatory;
   }

   public void setMandatory(boolean mandatory) {
      this.mandatory = mandatory;
   }
   public Set<OpAction> getActions() {
      return actions;
   }
   public void setActions(Set<OpAction> actions) {
      this.actions = actions;
   }
   public Set<OpActionVersion> getActionVersions() {
      return actionVersions;
   }
   public void setActionVersions(Set<OpActionVersion> actionVersions) {
      this.actionVersions = actionVersions;
   }
}
