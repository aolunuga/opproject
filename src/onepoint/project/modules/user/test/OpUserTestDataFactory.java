/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.user.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.*;
import onepoint.project.test.OpTestDataFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class contains helper methods for building data structures needed into User module.
 *
 * @author calin.pavel
 */
public class OpUserTestDataFactory extends OpTestDataFactory {
   // Query for retrieving subjects by name
   private final static String SELECT_SUBJECT_ID_BY_NAME_QUERY = "select subject.ID from OpSubject as subject where subject.Name = ?";

   // Query for retrieving all users
   private final static String SELECT_USERS_QUERY = "select user from OpUser as user";

   // Query for retrieving all groups
   private final static String SELECT_GROUPS_QUERY = "select group from OpGroup as group";

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public OpUserTestDataFactory(OpProjectSession session) {
      super(session);
   }

   /**
    * Retrieve all users from database.
    *
    * @return a list of <code>OpUser</code> instances.
    * @param broker
    */
   public List<OpUser> getAllUsers(OpBroker broker) {
      OpQuery query = broker.newQuery(SELECT_USERS_QUERY);
      return (List<OpUser>) broker.list(query);
   }

   /**
    * Retrieve an user by his USER_NAME.
    *
    * @param userName user name to look for.
    * @return found user or null.
    */
   public OpUser getUserByName(OpBroker broker, String userName) {
      Long id = getSubjectId(userName);
      if (id != null) {
         String locator = OpLocator.locatorString(OpUser.USER, Long.parseLong(id.toString()));

         return getUserById(broker, locator);
      }

      return null;
   }

   /**
    * Retrieve all groups from database.
    *
    * @return a list of <code>OpGroup</code> instances.
    * @param broker
    */
   public List<OpGroup> getAllGroups(OpBroker broker) {
      OpQuery query = broker.newQuery(SELECT_GROUPS_QUERY);
      return (List<OpGroup>)broker.list(query);
   }


   /**
    * Retrieve an user by its ID.
    *
    * @param locator id/locator to look for.
    * @return found user or null.
    */
   public OpUser getUserById(OpBroker broker, String locator) {
      return (OpUser) getSubjectById(broker, locator);
   }

   /**
    * Retrieve an group by its GROUP_NAME.
    *
    * @param groupName group  name to look for.
    * @return found group or null.
    */
   public OpGroup getGroupByName(OpBroker broker, String groupName) {
      Long id = getSubjectId(groupName);
      if (id != null) {
         String locator = OpLocator.locatorString(OpGroup.GROUP, Long.parseLong(id.toString()));

         return getGroupById(broker, locator);
      }

      return null;
   }

   /**
    * Retrieve an group by its locator.
    *
    * @param locator id/locator to look for.
    * @return found group or null.
    */
   public OpGroup getGroupById(OpBroker broker, String locator) {
      return (OpGroup) getSubjectById(broker, locator);
   }

   /**
    * Retrieve a given subject by its LOCATOR.
    *
    * @param locator subject locator.
    * @return found subject (instace of <code>OpUser</code> or <code>OpGroup</code>) or null.
    */
   private OpSubject getSubjectById(OpBroker broker, String locator) {
      OpSubject subject = (OpSubject) broker.getObject(locator);
      if (subject instanceof OpUser) {
         OpUser user = (OpUser) subject;
         // we have to do this to initialize lazy collection
         user.getPreferenceValue(OpPreference.LOCALE);
         user.getAssignments().size();
      }
      else if (subject instanceof OpGroup) {
         // initialize lazy collections.
         OpGroup group = (OpGroup) subject;
         group.getSubGroupAssignments().size();
         group.getSuperGroupAssignments().size();
         group.getUserAssignments().size();
      }

      return subject;
   }

   /**
    * This method serach for a user with a given username and returns an instance of <code>OpUser</code>.
    *
    * @param userName user name to look for.
    * @return found subject (instace of <code>OpUser</code> or <code>OpGroup</code>) or null.
    */
   private Long getSubjectId(String userName) {
      OpBroker broker = session.newBroker();
      Long subjectId = null;

      OpQuery query = broker.newQuery(SELECT_SUBJECT_ID_BY_NAME_QUERY);
      query.setString(0, userName);
      Iterator subjectsIt = broker.iterate(query);
      if (subjectsIt.hasNext()) {
         subjectId = (Long) subjectsIt.next();
      }

      broker.close();
      return subjectId;
   }


   /**
    * Creates the map of data necessary for user creation/update.
    *
    * @param userName    user name
    * @param password    user password
    * @param description user description
    * @param level       user level
    * @param firstName   user firstName
    * @param lastName    user lastName
    * @param language    user language
    * @param email       user email
    * @param fax         user fax
    * @param mobile      user mobile
    * @param phone       user phone
    * @param groups      user groups (should be a list containing groups ids, instances of <code>String</code>).
    * @return map of data
    */
   public static Map createUserData(String userName, String password, String description, byte level, String firstName,
        String lastName, String language, String email, String fax, String mobile, String phone, List groups) {

      Map userData = createUserData(userName, password, level);

      userData.put(OpUser.DESCRIPTION, description);
      userData.put(OpUserService.LANGUAGE, language);

      userData.put(OpContact.FIRST_NAME, firstName);
      userData.put(OpContact.LAST_NAME, lastName);
      userData.put(OpContact.EMAIL, email);
      userData.put(OpContact.FAX, fax);
      userData.put(OpContact.MOBILE, mobile);
      userData.put(OpContact.PHONE, phone);

      userData.put(OpUserService.ASSIGNED_GROUPS, groups);

      return userData;
   }

   /**
    * Creates the map of data necessary for user creation/update.
    *
    * @param userName user name
    * @param password user password
    * @param level    user level
    * @return map of data
    */
   public static Map createUserData(String userName, String password, byte level) {
      Map userData = new HashMap();

      userData.put(OpUser.NAME, userName);
      userData.put(OpUser.PASSWORD, password);
      userData.put(OpUserService.PASSWORD_RETYPED, password);

      userData.put(OpUserService.USER_LEVEL, Byte.toString(level));

      return userData;
   }

   /**
    * Creates the map of data necessary for group creation/update.
    *
    * @param groupName   name of the group
    * @param description group description
    * @param groups      parent groups ids
    * @return group information.
    */
   public static Map createGroupData(String groupName, String description, List groups) {
      Map groupData = new HashMap();

      groupData.put(OpGroup.NAME, groupName);
      groupData.put(OpGroup.DESCRIPTION, description);
      groupData.put(OpUserService.ASSIGNED_GROUPS, groups);

      return groupData;
   }
}
