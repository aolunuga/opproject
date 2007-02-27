/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user;

import java.util.Iterator;
import java.util.Set;

public class OpUser extends OpSubject {

   //user level types
   public static final byte STANDARD_USER_LEVEL = 1;
   public static final byte MANAGER_USER_LEVEL = 2;

   public final static String USER = "OpUser";

   public final static String PASSWORD = "Password";
   public final static String CONTACT = "Contact";
   public final static String ASSIGNMENTS = "Assignments";
   public final static String LOCKS = "Locks";
   public final static String RESOURCES = "Resources";
   public final static String PREFERENCES = "Preferences";
   public final static String ACTIVITY_COMMENTS = "ActivityComments";

   // Administrator user (per site)
   public final static String ADMINISTRATOR_NAME = "Administrator";
   public final static String ADMINISTRATOR_NAME_ALIAS1 = "administrator";
   public final static String ADMINISTRATOR_NAME_ALIAS2 = "ADMINISTRATOR";
   public final static String ADMINISTRATOR_DISPLAY_NAME = "{$AdministratorDisplayName}";
   public final static String ADMINISTRATOR_DESCRIPTION = "{$AdministratorDescription}";
   public final static String ADMINISTRATOR_ID_QUERY = "select user.ID from OpUser as user where user.Name = '" + OpUser.ADMINISTRATOR_NAME + "'";

   // *** Maybe extra property DISPLAY_NAME (configurable)?

   // Authentication types
   public final static byte INTERNAL = 0; // Default authentication type
   public final static byte LDAP = 1;

   private String password;
   private byte authenticationType = INTERNAL;
   private Byte level;// = MANAGER_USER_LEVEL;
   private OpContact contact;
   private Set assignments;
   private Set ownedLocks;
   private Set resources;
   private Set preferences;
   private Set activityComments;
   private Set workSlips;

   public void setPassword(String password) {
      this.password = password;
   }

   public String getPassword() {
      return password;
   }

   public void setAuthenticationType(byte authenticationType) {
      this.authenticationType = authenticationType;
   }

   public byte getAuthenticationType() {
      return authenticationType;
   }

   public void setContact(OpContact contact) {
      this.contact = contact;
   }

   public OpContact getContact() {
      return contact;
   }

   public void setAssignments(Set assignments) {
      this.assignments = assignments;
   }

   public Set getAssignments() {
      return assignments;
   }

   public void setOwnedLocks(Set ownedLocks) {
      this.ownedLocks = ownedLocks;
   }

   public Set getOwnedLocks() {
      return ownedLocks;
   }

   public void setResources(Set resources) {
      this.resources = resources;
   }

   public Set getResources() {
      return resources;
   }

   public void setPreferences(Set preferences) {
      this.preferences = preferences;
   }

   public Set getPreferences() {
      return preferences;
   }

   public void setActivityComments(Set activityComments) {
      this.activityComments = activityComments;
   }

   public Set getActivityComments() {
      return activityComments;
   }

   public void setWorkSlips(Set workSlips) {
      this.workSlips = workSlips;
   }

   public Set getWorkSlips() {
      return workSlips;
   }

   /**
    * Returns a preference for the current user, or <code>null</code> if there is no such preference store for the user.
    * @param preferenceName a <code>String</code> representing the name of the preference to retrieve.
    * @return a <code>String</code> representing the value of the preference or <code>null</code> if there is no such preference.
    */
   public String getPreferenceValue(String preferenceName) {
      OpPreference pref = this.getPreference(preferenceName);
      if (pref != null) {
         return pref.getValue();
}
      return null;
   }

   /**
    * Returns a preference for the current user, or <code>null</code> if there is no such preference store for the user.
    * @param preferenceName a <code>String</code> representing the name of the preference to retrieve.
    * @return a <code>OpPreference</code> representing a user preference.
    */
   public OpPreference getPreference(String preferenceName) {
      if (this.getPreferences() == null) {
         return null;
      }
      Iterator it = this.getPreferences().iterator();
      while (it.hasNext()) {
         OpPreference preference = (OpPreference) it.next();
         if (preference.getName().equals(preferenceName)) {
            return preference;
         }
      }
      return null;
   }

   public Byte getLevel() {
      return level;
   }

   public void setLevel(Byte level) {
      this.level = level;
   }
   
}
