/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.persistence.OpObject;

public class OpPreference extends OpObject {
   
   public final static String PREFERENCE = "OpPreference";
   
   // Preference names
   public final static String LOCALE = "Locale";
   public static final String SHOW_ASSIGNMENT_IN_HOURS = "ShowHours";

   public final static String NAME = "Name";
   public final static String VALUE = "Value";
   public final static String USER = "User"; 
   
   private String name;
   private String value;
   private OpUser user;
   
   public void setName(String name) {
      this.name = name;
   }
   
   public String getName() {
      return name;
   }
   
   public void setValue(String value) {
      this.value = value;
   }
   
   public String getValue() {
      return value;
   }
   
   public void setUser(OpUser user) {
      this.user = user;
   }
   
   public OpUser getUser() {
      return user;
   }
   
}
