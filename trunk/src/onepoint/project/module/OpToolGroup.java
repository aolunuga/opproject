/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpToolGroup {

   private String name;
   private int sequence;
   private String caption;
   private OpModule module;
   private boolean administratorOnly = false;
   private String category = "default";
   private Byte level;
   private Set<Byte> hiddenLevel;

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

   public final String getCategory() {
      return category;
   }

   public final void setCategory(String category) {
      this.category = category;
   }

   final public void setMinLevel(Byte level) {
      this.level = level;
   }

   public final Byte getMinLevel() {
      return level;
   }

   public void addHiddenLevel(Byte level) {
	   if (hiddenLevel == null) {
		   hiddenLevel = new HashSet<Byte>();
	   }
	   hiddenLevel.add(level);
   }

   public boolean removeHiddenLevel(Byte level) {
	   if (hiddenLevel == null) {
		   return false;
	   }
	   return hiddenLevel.remove(level);
   }

   public Set<Byte> getHiddenLevels() {
	   return hiddenLevel;
   }
}
