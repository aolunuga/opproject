/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

public class OpToolGroup {

   private String name;
   private int sequence;
   private String caption;
   private OpModule module;
   private boolean administratorOnly = false;
   private Byte level;

   final public void setName(String name) {
      this.name = name;
   }

   public final String getName() {
      return name;
   }

   public final void setSequence(int sequence) {
      this.sequence = sequence;
   }

   public final int getSequence() {
      return sequence;
   }

   final public void setCaption(String caption) {
      this.caption = caption;
   }

   public final String getCaption() {
      return caption;
   }

   final void setModule(OpModule module) {
      this.module = module;
   }

   public final OpModule getModule() {
      return module;
   }

   public boolean isAdministratorOnly() {
      return administratorOnly;
   }

   public void setAdministratorOnly(boolean administratorOnly) {
      this.administratorOnly = administratorOnly;
   }

   final public void setLevel(Byte level) {
      this.level = level;
   }

   public final Byte getLevel() {
      return level;
   }
}
