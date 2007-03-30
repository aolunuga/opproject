/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

public class OpToolGroup {

   private String _name;
   private int _sequence;
   private String _caption;
   private OpModule _module;
   private boolean administratorOnly = false;

   final public void setName(String name) {
      _name = name;
   }

   public final String getName() {
      return _name;
   }

   public final void setSequence(int sequence) {
      _sequence = sequence;
   }

   public final int getSequence() {
      return _sequence;
   }

   final public void setCaption(String caption) {
      _caption = caption;
   }

   public final String getCaption() {
      return _caption;
   }

   final void setModule(OpModule module) {
      _module = module;
   }

   public final OpModule getModule() {
      return _module;
   }

   public boolean isAdministratorOnly() {
      return administratorOnly;
   }

   public void setAdministratorOnly(boolean administratorOnly) {
      this.administratorOnly = administratorOnly;
   }
}
