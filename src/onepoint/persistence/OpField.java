/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

public class OpField extends OpMember {

   private boolean _auto_increment;
   private boolean _unique;
   private boolean _mandatory; // Not-null
   private boolean _ordered; // Used for export and (logical) backup
   private boolean _indexed;

   public OpField() {}

   public final void setAutoIncrement(boolean auto_increment) {
      _auto_increment = auto_increment;
   }

   public final boolean getAutoIncrement() {
      return _auto_increment;
   }

   public final void setUnique(boolean unique) {
      _unique = unique;
   }

   public final boolean getUnique() {
      return _unique;
   }

   public final void setMandatory(boolean mandatory) {
      _mandatory = mandatory;
   }

   public final boolean getMandatory() {
      return _mandatory;
   }

   public final void setOrdered(boolean ordered) {
      _ordered = ordered;
   }

   public final boolean getOrdered() {
      return _ordered;
   }

   public final void setIndexed(boolean indexed) {
      _indexed = indexed;
   }

   public final boolean getIndexed() {
      return _indexed;
   }
}
