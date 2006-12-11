/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

public class OpTool {

   private String _name;
   private int _sequence;
   private String _caption;
   private String _icon;
   private String _start_form;
   private String _group_ref;
   private OpToolGroup _group;
   private OpModule _module;
   private Boolean multiUserOnly;

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

   final public void setIcon(String icon) {
      _icon = icon;
   }

   final public String getIcon() {
      return _icon;
   }

   final public void setStartForm(String start_form) {
      _start_form = start_form;
   }

   public final String getStartForm() {
      return _start_form;
   }

   final public void setGroupRef(String group_ref) {
      _group_ref = group_ref;
      _group = null;
   }

   public final String getGroupRef() {
      return _group_ref;
   }

   final public void setGroup(OpToolGroup group) {
      _group = group;
   }

   public final OpToolGroup getGroup() {
      return _group;
   }

   final void setModule(OpModule module) {
      _module = module;
   }

   public final OpModule getModule() {
      return _module;
   }

   public Boolean isMultiUserOnly() {
      return multiUserOnly;
   }

   public void setMultiUserOnly(Boolean multiUserOnly) {
      this.multiUserOnly = multiUserOnly;
   }
}
