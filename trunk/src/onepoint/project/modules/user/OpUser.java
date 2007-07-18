/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.persistence.OpFilter;
import onepoint.project.modules.project.OpActivityComment;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.util.OpHashProvider;
import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
//   public final static String AUTHENTICATION = "AuthenticationType";
   

   // Administrator user (per site)
   public final static String ADMINISTRATOR_NAME = "Administrator";
   public final static String ADMINISTRATOR_NAME_ALIAS1 = "administrator";
   public final static String ADMINISTRATOR_NAME_ALIAS2 = "ADMINISTRATOR";
   public final static String ADMINISTRATOR_DISPLAY_NAME = "${AdministratorDisplayName}";
   public final static String ADMINISTRATOR_DESCRIPTION = "${AdministratorDescription}";
   public final static String ADMINISTRATOR_ID_QUERY = "select user.ID from OpUser as user where user.Name = '" + OpUser.ADMINISTRATOR_NAME + "'";

   // *** Maybe extra property DISPLAY_NAME (configurable)?


   public final static String BLANK_PASSWORD = new OpHashProvider().calculateHash("");

   private String password = BLANK_PASSWORD;
   private Byte level = MANAGER_USER_LEVEL;
   private OpContact contact;
   private Set<OpUserAssignment> assignments;
   private Set<OpLock> ownedLocks;
   private Set<OpResource> resources;
   // a set containing OpPreference's only!
   private Set<OpPreference> preferences;
   private Set<OpActivityComment> activityComments;
   private Set<OpWorkSlip> workSlips;

   public OpUser() {
      super();
   }

   public void setPassword(String password) {
      this.password = password == null ? BLANK_PASSWORD : password;
   }

   public String getPassword() {
      return password;
   }

   public OpContact createContact() {
      contact = new OpContact();
      contact.setUser(this);
      return (contact);
   }

   public void setContact(OpContact contact) {
      this.contact = contact;
   }

   public OpContact getContact() {
      return contact;
   }

   public void setAssignments(Set<OpUserAssignment> assignments) {
      this.assignments = assignments;
   }

   public Set<OpUserAssignment> getAssignments() {
      return assignments;
   }

   public void setOwnedLocks(Set<OpLock> ownedLocks) {
      this.ownedLocks = ownedLocks;
   }

   public Set<OpLock> getOwnedLocks() {
      return ownedLocks;
   }

   public void setResources(Set<OpResource> resources) {
      this.resources = resources;
   }

   public Set<OpResource> getResources() {
      return resources;
   }

   public void setPreferences(Set<OpPreference> preferences) {
      this.preferences = preferences;
   }

   public Set<OpPreference> getPreferences() {
      return preferences;
   }

   public void setActivityComments(Set<OpActivityComment> activityComments) {
      this.activityComments = activityComments;
   }

   public Set<OpActivityComment> getActivityComments() {
      return activityComments;
   }

   public void setWorkSlips(Set<OpWorkSlip> workSlips) {
      this.workSlips = workSlips;
   }

   /**
    * get all existing work slips of the current user.
    *
    * @return all the users work slips.
    * @pre none
    * @post none
    */

   public Set<OpWorkSlip> getWorkSlips() {
      return workSlips;
   }

   /**
    * get all existing work slips of the current user that match the given filter argument.
    *
    * @param filter filter to filter out results.
    * @return all the users work slips matching the given filter criteria.
    * @pre none
    * @post none
    */
   public Set<OpWorkSlip> getWorkSlips(OpFilter filter) {
      if (filter == null) {
         return (getWorkSlips());
      }
      Set<OpWorkSlip> ret = new HashSet<OpWorkSlip>();
      Iterator<OpWorkSlip> iter = workSlips.iterator();
      OpWorkSlip current;
      while (iter.hasNext()) {
         current = iter.next();
         if (filter.accept(current)) {
            ret.add(current);
         }
      }
      return ret;
   }


   /**
    * Returns a preference for the current user, or <code>null</code> if there is no such preference store for the user.
    *
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
    *
    * @param preferenceName a <code>String</code> representing the name of the preference to retrieve.
    * @return a <code>OpPreference</code> representing a user preference.
    */
   public OpPreference getPreference(String preferenceName) {
      if (this.getPreferences() == null) {
         return null;
      }
      for (OpPreference opPreference : this.getPreferences()) {
         if (opPreference.getName().equals(preferenceName)) {
            return opPreference;
         }
      }
      return null;
   }

   public Byte getLevel() {
      return level;
   }

   public void setLevel(Byte level) {
      if (this.level.byteValue() > level.byteValue()) {
         Set<OpPermission> permissions = getOwnedPermissions();
         if (permissions != null) {
            for (OpPermission permission : permissions) {
               if (permission.getAccessLevel() >= OpPermission.MANAGER) {
                  throw new IllegalArgumentException("demote of user level not allowed");
                  //XException(session.newError(UserServiceIfc.ERROR_MAP, OpUserError.DEMOTE_USER_ERROR));
               }
            }
         }
      }
      this.level = level;
   }

   /**
    * @return
    * @pre
    * @post
    */
   public boolean isLevelValid() {
      if (level == null) {
         return (false);
      }
      return (level >= OpUser.STANDARD_USER_LEVEL &&
           level <= OpUser.MANAGER_USER_LEVEL);
   }

   /**
    * Performs equality checking for the given passwords.
    *
    * @return boolean flag indicating passwords equality
    */
   public boolean validatePassword(String toCheck) {
      if (toCheck == null) {
         return(password.equalsIgnoreCase(BLANK_PASSWORD));
      }
//      String pwd = new String(getByteArrayValue(sr.getAttributes(), configUserMapping, OPUSER_PASSWORD));
      String[] split = splitAlgorithmAndPassword(password);
      String algorithm = split[0];
      String pwd = split[1];
      if (algorithm != null) { // pwd is base 64 encoded
         try {
            pwd = new String(new BASE64Decoder().decodeBuffer(pwd));
            return pwd.equals(OpHashProvider.fromHashString(toCheck));
         } catch (IOException exc) {
         }
         return false;
      }
      return password.equalsIgnoreCase(toCheck); 
   }

   /**
    * splits the given password into an {@link String} array of length 2.
    * the second value will always contain the password (without the algorithm) 
    * wheras the firts value will contain the algorithm used to encode this password. 
    * If the pwd does not have an algorithm associated with it, the first value will be <code>null</code>. 
    * @param pwd the password like {SHA}pwd or pwd_in_hex.
    * @return an array of size two, containing the algorithm and the password.
    */
   public static String[] splitAlgorithmAndPassword(String pwd) {
      Pattern p = Pattern.compile("^\\{([^\\}]*)\\}(.*)$");
      Matcher m = p.matcher(pwd);
      String[] ret = new String[2];
      
      if (m.matches()) {
         ret[0] = m.group(1);
         ret[1] = m.group(2);
      }
      else {
         ret[0] = null;
         ret[1]= pwd;
      }
      return ret;
   }

   /**
    * @return
    * @pre
    * @post
    */
   public boolean passwordIsEmpty() {
      return (validatePassword(null));
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("<OpUser>");
//      buffer.append('\n');
      buffer.append("<id>"+getID()+"</id>");
//      buffer.append('\n');
      buffer.append("<name>"+super.getName()+"</name>");
//      buffer.append('\n');
      buffer.append("<displayname>"+super.getDisplayName()+"</displayname>");
//      buffer.append('\n');
      buffer.append("<password>HIDDEN</password>");
//      buffer.append('\n');
      buffer.append("<source>"+ getSource()+"</source>");
//      buffer.append('\n');
      buffer.append("<level>"+level+"</level>");
//      buffer.append('\n');
      buffer.append(contact);
//      buffer.append('\n');
      buffer.append("</OpUser>");
      
      return buffer.toString();
   }

}
