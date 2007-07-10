/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

public class OpField extends OpMember {

   private boolean autoIncrement;
   private boolean unique;
   private String uniqueKey;
   private boolean mandatory; // Not-null
   private boolean ordered; // Used for export and (logical) backup
   private boolean indexed;
   private String column;
   private String defaultValue;
   private Boolean update; // specifies that the mapped columns should be included in SQL UPDATE statements
   private Boolean insert; // specifies that the mapped columns should be included in SQL INSERT statements

   public OpField() {
   }

   public final void setAutoIncrement(boolean auto_increment) {
      autoIncrement = auto_increment;
   }

   public final boolean getAutoIncrement() {
      return autoIncrement;
   }

   public final void setUnique(boolean unique) {
      this.unique = unique;
   }

   public final boolean getUnique() {
      return unique;
   }

   public final String getUniqueKey() {
      return uniqueKey;
   }

   public final void setUniqueKey(String uniqueKey) {
      this.uniqueKey = uniqueKey;
   }

   public final void setMandatory(boolean mandatory) {
      this.mandatory = mandatory;
   }

   public final boolean getMandatory() {
      return mandatory;
   }

   public final void setOrdered(boolean ordered) {
      this.ordered = ordered;
   }

   public final boolean getOrdered() {
      return ordered;
   }

   public final void setIndexed(boolean indexed) {
      this.indexed = indexed;
   }

   public final boolean getIndexed() {
      return indexed;
   }

   public final String getColumn() {
      return column;
   }

   public final void setColumn(String column) {
      this.column = column;
   }

   public final String getDefaultValue() {
      return defaultValue;
   }

   public final void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
   }

   public Boolean getUpdate() {
      return update;
   }

   public void setUpdate(Boolean update) {
      this.update = update;
   }

   public Boolean getInsert() {
      return insert;
   }

   public void setInsert(Boolean insert) {
      this.insert = insert;
   }
}
