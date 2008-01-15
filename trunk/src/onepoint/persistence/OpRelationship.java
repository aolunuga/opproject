/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

public class OpRelationship extends OpMember {

   // Maybe put these constants into a seperate class (or inner public class)?
   public final static int ASSOCIATION = 0;
   public final static int AGGREGATION = 1;
   public final static int COMPOSITION = 2;

   private int relationshipType;
   private String backRelationshipName;
   private OpRelationship backRelationship; // Resolved on registration
   private boolean inverse = false;
   private boolean recursive = false; // Used for export and (logical) backup

   /**
    * Indicates whether cascadeMode mode should be used for this relationship.
    * Can be one of the following: "save-update" or "delete".
    */
   private String cascadeMode = null;

   /**
    * Indicates whether fetch mode should be used for this relationship.
    * Can be one of the following: "subselect" or "join".
    */
   private String fetch = null;

   /**
    * sort order for collection, syntax is <code>column_name asc|desc</code>.
    */
   private String orderBy;

   public OpRelationship() {
      // Default relationship-type is 'association'
      relationshipType = ASSOCIATION;
   }

   public final void setRelationshipType(int relationship_type) {
      relationshipType = relationship_type;
   }

   public final int getRelationshipType() {
      return relationshipType;
   }

   final void setBackRelationshipName(String back_relationship_name) {
      backRelationshipName = back_relationship_name;
   }

   String getBackRelationshipName() {
      // Called by OpTypeManager
      return backRelationshipName;
   }

   final void setBackRelationship(OpRelationship back_relationship) {
      // Called by OpPrototype
      backRelationship = back_relationship;
   }

   public final OpRelationship getBackRelationship() {
      return backRelationship;
   }

   final void setInverse(boolean inverse) {
      this.inverse = inverse;
   }

   public final boolean getInverse() {
      return inverse;
   }

   final void setRecursive(boolean recursive) {
      this.recursive = recursive;
   }

   public final boolean getRecursive() {
      return recursive;
   }

   /**
    * Gets the value of the cascade mode.
    * @return a <code>String</code> representing the value of the cascade mode.
    */
   public String getCascadeMode() {
      return cascadeMode;
   }

   /**
    * Gets the value of the fetch mode.
    * @return a <code>String</code> representing the value of the fetch mode. May return <code>null</code>!
    */
   public String getFetch() {
      return fetch;
   }

   /**
    * Sets the value of the cascade mode.
    * @param cascadeMode a <code>String</code> representing the value of the cascade mode.
    */
   public void setCascadeMode(String cascadeMode) {
      this.cascadeMode = cascadeMode;
   }

   /**
    * Sets the value of the fetch mode.
    * @param fetch a <code>String</code> representing the value of the fetch mode.
    */
   public void setFetch(String fetch) {
      if(fetch.equalsIgnoreCase("subselect") || fetch.equalsIgnoreCase("join"))
         this.fetch = fetch.toLowerCase();
   }
   /**
    * @return
    * @pre
    * @post
    */
   public String getOrderBy() {
      return orderBy;
   }

   /**
    * @return
    * @pre
    * @post
    */
   public void setOrderBy(String orderBy) {
      this.orderBy = orderBy;
   }

}
