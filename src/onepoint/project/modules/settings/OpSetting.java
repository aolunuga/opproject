/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.persistence.OpObject;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.util.Pair;

public class OpSetting extends OpObject {

   public final static String SETTING = "OpSetting";

   public final static String NAME = "Name";
   public final static String VALUE = "Value";
   public final static String CONTENT = "Content";

   private String name;
   private String value;
   private OpContent content;

   /**
    * @deprecated for hibernate and backup only
    */
   public OpSetting() {
}
   /**
    * @param value 
    * @param name 
    * 
    */
   public OpSetting(String name, Pair<String, OpContent> value) {
	   this (name, value.getFirst(), value.getSecond());
   }
   
   /**
    * @param name
    * @param value2
    */
   public OpSetting(String name, String value) {
	   this(name, value, null);
   }

   public OpSetting(String name, String value, OpContent content) {
	   this.name = name;
	   this.value = value;
	   this.content = content;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setValue(String value, OpContent content) {
	   setValue(new Pair<String, OpContent>(value, content));
   }

   public void setValue(Pair<String, OpContent> value) {
	   setValue(value.getFirst());
	   setContent(value.getSecond());
   }

   public void setValue(String value) {
      this.value = value;
   }

   public Pair<String, OpContent> get() {
	   return new Pair<String, OpContent>(value, content);
   }

   public String getValue() {
      return value;
   }
   
   public OpContent getContent() {
	   return content;
   }
   
   public void setContent(OpContent content) {
	   this.content = content;
   }
}