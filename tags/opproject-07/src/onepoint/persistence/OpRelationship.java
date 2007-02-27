/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

public class OpRelationship extends OpMember {

   // Maybe put these constants into a seperate class (or inner public class)?
   public final static int ASSOCIATION = 0;
   public final static int AGGREGATION = 1;
   public final static int COMPOSITION = 2;

   /**
    * Cascade mode possible values
    */
   public final static String CASCADE_DELETE = "delete";
   public final static String CASCADE_SAVEUPDATE = "save-update";

   private int _relationship_type;
   private String _back_relationship_name;
   private OpRelationship _back_relationship; // Resolved on registration
   private boolean _inverse = false;
   private boolean _recursive = false; // Used for export and (logical) backup

   /**
    * Indicates whether cascadeMode mode should be used for this relationship.
    * Can be one of the following: "save-update" or "delete".
    */
   private String cascadeMode = null;

   public OpRelationship() {
      // Default relationship-type is 'association'
      _relationship_type = ASSOCIATION;
   }

   public final void setRelationshipType(int relationship_type) {
      _relationship_type = relationship_type;
   }

   public final int getRelationshipType() {
      return _relationship_type;
   }

   final void setBackRelationshipName(String back_relationship_name) {
      _back_relationship_name = back_relationship_name;
   }

   String getBackRelationshipName() {
      // Called by OpTypeManager
      return _back_relationship_name;
   }

   final void setBackRelationship(OpRelationship back_relationship) {
      // Called by OpPrototype
      _back_relationship = back_relationship;
   }

   public final OpRelationship getBackRelationship() {
      return _back_relationship;
   }

   final void setInverse(boolean inverse) {
      _inverse = inverse;
   }

   public final boolean getInverse() {
      return _inverse;
   }

   final void setRecursive(boolean recursive) {
      _recursive = recursive;
   }

   public final boolean getRecursive() {
      return _recursive;
   }

   /**
    * Gets the value of the cascade mode.
    * @return a <code>String</code> representing the value of the cascade mode.
    */
   public String getCascadeMode() {
      return cascadeMode;
   }

   /**
    * Sets the value of the cascade mode.
    * @param cascadeMode a <code>String</code> representing the value of the cascade mode.
    */
   public void setCascadeMode(String cascadeMode) {
      this.cascadeMode = cascadeMode;
   }

}
