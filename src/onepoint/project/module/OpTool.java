/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

public class OpTool {

   private String name;
   private int sequence;
   private String caption;
   private String icon;
   private String startForm;
   private String groupRef;
   private OpToolGroup group;
   private OpModule module;
   private Boolean multiUserOnly;
   private boolean selected;
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

   final public void setIcon(String icon) {
      this.icon = icon;
   }

   final public String getIcon() {
      return icon;
   }

   final public void setStartForm(String start_form) {
      startForm = start_form;
   }

   public final String getStartForm() {
      return startForm;
   }

   final public void setGroupRef(String group_ref) {
      groupRef = group_ref;
      group = null;
   }

   public final String getGroupRef() {
      return groupRef;
   }

   final public void setGroup(OpToolGroup group) {
      this.group = group;
   }

   public final OpToolGroup getGroup() {
      return group;
   }

   final void setModule(OpModule module) {
      this.module = module;
   }

   public final OpModule getModule() {
      return module;
   }

   public Boolean isMultiUserOnly() {
      return multiUserOnly;
   }

   public void setMultiUserOnly(Boolean multiUserOnly) {
      this.multiUserOnly = multiUserOnly;
   }

   public boolean isSelected() {
      return selected;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   final public void setLevel(Byte level) {
      this.level = level;
   }

   public final Byte getLevel() {
      return level;
   }
}
