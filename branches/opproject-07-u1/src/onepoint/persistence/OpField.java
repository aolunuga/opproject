/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

public class OpField extends OpMember {

   private boolean autoIncrement;
   private boolean unique;
   private boolean mandatory; // Not-null
   private boolean ordered; // Used for export and (logical) backup
   private boolean indexed;

   public OpField() {}

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
}
