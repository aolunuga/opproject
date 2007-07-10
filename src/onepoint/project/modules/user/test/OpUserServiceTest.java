/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.user.test;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.*;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.util.OpHashProvider;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocaleManager;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class test user service methods and form providers.
 *
 * @author calin.pavel
 */
public class OpUserServiceTest extends OpBaseOpenTestCase {
   // class logger.
   private static final XLog logger = XLogFactory.getServerLogger(OpUserServiceTest.class);

   // Password used for tests.
   private static final String TEST_PASS = new OpHashProvider().calculateHash("password");

   // User data used through tests.
   private static final String TEST_USER_NAME = "tester";
   private static final String TEST_EMAIL = "tester@onepoint.at";
   private static final String TEST_LANGUAGE = "en";


   // Group data
   private static final String TEST_GROUP_NAME = "group1";

   // Dummy string
   private static final String DUMMY_STRING = "tester";

   private OpUserService userService;
   private OpUserTestDataFactory dataFactory;

   /**
    * Here we prepare data for these tests.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      dataFactory = new OpUserTestDataFactory(session);
      userService = OpTestDataFactory.getUserService();
      cleanUp();
   }

   /**
    * Clean data after test execution.
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      logOut();
      logIn();
      cleanUp();
      super.tearDown();
   }

   /**
    * Delete users/groups that are used into out tests.
    */
   private void cleanUp() {

      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();

      // delete all users except Administrator
      for (OpUser user : dataFactory.getAllUsers(broker)) {
         if (OpUser.ADMINISTRATOR_NAME.equals(user.getName())) {
            continue;
         }
         broker.deleteObject(user);
      }

      // delete all groups except Everyone
      for (OpGroup group : dataFactory.getAllGroups(broker)) {
         if (OpGroup.EVERYONE_NAME.equals(group.getName())) {
            continue;
         }
         broker.deleteObject(group);
      }

      transaction.commit();
      broker.close();
   }

   /**
    * Here we test scenario where user tries to log in without user name.
    *
    * @throws Exception If authentication process fails.
    */
   public void testLoginWithoutUsername()
        throws Exception {
      XMessage request = new XMessage();
      request.setArgument(OpUserService.PASSWORD, TEST_PASS);
      XMessage response = userService.signOn(session, request);
      assertError(response, OpUserError.USER_UNKNOWN);
   }

   /**
    * Here we try to see if application allow login of Administrator with different alises
    * ("administrator" or "ADMINISTRATOR").
    *
    * @throws Exception If authentication process fails.
    */
   public void testLoginWithUserAliases()
        throws Exception {

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.LOGIN, OpUser.ADMINISTRATOR_NAME.toLowerCase());
      request.setArgument(OpUserService.PASSWORD, OpUser.BLANK_PASSWORD);
      XMessage response = userService.signOn(session, request);
      assertNoError(response);

      // now try with upper case for all letters
      request.setArgument(OpUserService.LOGIN, OpUser.ADMINISTRATOR_NAME.toUpperCase());
      response = userService.signOn(session, request);
      assertNoError(response);
   }

   /**
    * Test login process with an invalid password.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testInvalidPassword()
        throws Exception {
      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.LOGIN, OpUser.ADMINISTRATOR_NAME);
      request.setArgument(OpUserService.PASSWORD, TEST_PASS);
      XMessage response = userService.signOn(session, request);
      assertError(response, OpUserError.PASSWORD_MISMATCH);
   }

   /**
    * Test login process with an invalid username.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testInvalidUserName()
        throws Exception {
      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.LOGIN, TEST_USER_NAME);
      request.setArgument(OpUserService.PASSWORD, TEST_PASS);
      XMessage response = userService.signOn(session, request);
      assertError(response, OpUserError.USER_UNKNOWN);
   }

   /**
    * Test login process with an invalid username.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testValidLogin()
        throws Exception {
      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.LOGIN, OpUser.ADMINISTRATOR_NAME);
      request.setArgument(OpUserService.PASSWORD, OpUser.BLANK_PASSWORD);
      XMessage response = userService.signOn(session, request);
      assertNoError(response);

      // now check if the response contains Locale and other user specific settings.
      Map variables = response.getVariables();
      assertNotNull(variables);

      XCalendar calendar = (XCalendar) variables.get(OpProjectConstants.CALENDAR);
      assertNotNull(calendar);
   }

   // -------------------------- USER TESTS --------------------------------

   /**
    * This method test addition of a new user to the system.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddUser()
        throws Exception {
      createDefaultUser();

      // now retrieve user and check its data.
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      assertEquals(TEST_USER_NAME, user.getName());
      assertEquals(TEST_PASS, user.getPassword());
      assertEquals(new Byte(OpUser.MANAGER_USER_LEVEL), user.getLevel());
      assertEquals(DUMMY_STRING, user.getDescription());
      assertEquals(TEST_LANGUAGE, user.getPreferenceValue(OpPreference.LOCALE));

      OpContact contact = user.getContact();
      assertEquals(DUMMY_STRING, contact.getFirstName());
      assertEquals(DUMMY_STRING, contact.getLastName());
      assertEquals(TEST_EMAIL, contact.getEMail());
      assertEquals(DUMMY_STRING, contact.getFax());
      assertEquals(DUMMY_STRING, contact.getMobile());
      assertEquals(DUMMY_STRING, contact.getPhone());
   }

   /**
    * This method test scenario where an user is created without USER_NAME
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddUserWithoutUserName()
        throws Exception {
      Map userData = OpUserTestDataFactory.createUserData(null, TEST_PASS, OpUser.MANAGER_USER_LEVEL);

      XMessage request = new XMessage();

      // now try to create user
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      assertError(response, OpUserError.LOGIN_MISSING);
   }

   /**
    * This method test scenario where an user is created with an invalid email.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddUserWithInvalidEmail()
        throws Exception {
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING, null);

      XMessage request = new XMessage();

      // now try to create user
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      assertError(response, OpUserError.EMAIL_INCORRECT);
   }

   /**
    * This method test scenario where an user is created with a different retyped password.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddUserWrongRetypePass()
        throws Exception {
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING, null);

      // insert a wrong retype password
      userData.put(OpUserService.PASSWORD_RETYPED, DUMMY_STRING);

      XMessage request = new XMessage();

      // now try to create user
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      assertError(response, OpUserError.PASSWORD_MISMATCH);
   }

   /**
    * This method test scenario where an user is created with wrong level.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddUserWrongLevel()
        throws Exception {
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, OpUser.MANAGER_USER_LEVEL);

      // insert a wrong level (not INT)
      userData.put(OpUserService.USER_LEVEL, "not_int");

      XMessage request = new XMessage();

      // now try to create user
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      assertError(response, OpUserError.INVALID_USER_LEVEL);

      // now try with a level which is not supported.
      userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, (byte) 5);

      // now try to create user
      request.setArgument(OpUserService.USER_DATA, userData);
      response = userService.insertUser(session, request);
      assertError(response, OpUserError.INVALID_USER_LEVEL);
   }

   /**
    * This method test scenario where an user is created with an existing user name
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddUserWithExistingUserName()
        throws Exception {
      Map userData = OpUserTestDataFactory.createUserData(OpUser.ADMINISTRATOR_NAME, TEST_PASS, OpUser.MANAGER_USER_LEVEL);

      XMessage request = new XMessage();

      // now try to create user
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      assertError(response, OpUserError.LOGIN_ALREADY_USED);
   }

   /**
    * This method test scenario where an user is created with a super group.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddUserWithSuperGroup()
        throws Exception {
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING,
           Arrays.asList(new String[]{group.locator()}));

      XMessage request = new XMessage();

      //create user
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      assertNoError(response);

      // now retrieve user and check its data.
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      Set superGroups = user.getAssignments();
      assertNotNull(superGroups);
      assertEquals(1, superGroups.size());

      OpUserAssignment superGroupAssignement = (OpUserAssignment) superGroups.iterator().next();
      assertNotNull(superGroupAssignement);
      assertEquals(group.getID(), superGroupAssignement.getGroup().getID());
      assertEquals(user.getID(), superGroupAssignement.getUser().getID());
   }

   /**
    * This method test scenario where an user is created with an unexisting super group.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddUserWithWrongGroup()
        throws Exception {

      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING,
           Arrays.asList(new String[]{"OpGroup.9999.xid"}));

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      assertError(response, OpUserError.SUPER_GROUP_NOT_FOUND);
   }

   /**
    * This method tests update of an user (happy flow).
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateUser()
        throws Exception {
      createDefaultUser();

      // now retrieve user and check its data.
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      // now update user with Administrator username.
      String newValue = "newVaue";
      String newLanguage = "de";
      String newEmail = "aaa@bbb.de";
      Map userData = OpUserTestDataFactory.createUserData(newValue, newValue, newValue, OpUser.STANDARD_USER_LEVEL,
           newValue, newValue, newLanguage, newEmail, newValue, newValue, newValue, null);

      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      request.setArgument(OpUserService.USER_ID, user.locator());
      XMessage response = userService.updateUser(session, request);
      assertNoError(response);

      // now retrieve user and check its data.
      user = dataFactory.getUserByName(newValue);
      assertNotNull(user);

      assertEquals(newValue, user.getName());
      assertEquals(newValue, user.getPassword());
      assertEquals(new Byte(OpUser.STANDARD_USER_LEVEL), user.getLevel());
      assertEquals(newValue, user.getDescription());
      assertEquals(newLanguage, user.getPreferenceValue(OpPreference.LOCALE));

      OpContact contact = user.getContact();
      assertEquals(newValue, contact.getFirstName());
      assertEquals(newValue, contact.getLastName());
      assertEquals(newEmail, contact.getEMail());
      assertEquals(newValue, contact.getFax());
      assertEquals(newValue, contact.getMobile());
      assertEquals(newValue, contact.getPhone());
   }

   /**
    * This method tests update of an user with an invalid id.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateUserWithWrongId()
        throws Exception {
      XMessage request = new XMessage();

      // update used data
      request.setArgument(OpUserService.USER_ID, "OpUser.9999.xid");
      XMessage response = userService.updateUser(session, request);
      assertError(response, OpUserError.USER_NOT_FOUND);
   }

   /**
    * This method tests update of an user with an username already used.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateUserWithExistingName()
        throws Exception {
      createDefaultUser();

      // now retrieve user and check its data.
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      // now update user with Administrator username.
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING, null);
      userData.put(OpUser.NAME, OpUser.ADMINISTRATOR_NAME);

      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_ID, user.locator());
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.updateUser(session, request);
      assertError(response, OpUserError.LOGIN_ALREADY_USED);
   }

   /**
    * This method tests update of an user without ane.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateWithoutName()
        throws Exception {
      createDefaultUser();

      // now retrieve user and check its data.
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      // now update user with Administrator username.
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING, null);
      userData.put(OpUser.NAME, null);

      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_ID, user.locator());
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.updateUser(session, request);
      assertError(response, OpUserError.LOGIN_MISSING);
   }

   /**
    * This method tests update of an user with wrong email.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateWithWrongEmail()
        throws Exception {
      createDefaultUser();

      // now retrieve user and check its data.
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      // now update user with Administrator username.
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING, null);

      XMessage request = new XMessage();

      userData.put(OpContact.EMAIL, DUMMY_STRING);
      request.setArgument(OpUserService.USER_ID, user.locator());
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.updateUser(session, request);
      assertError(response, OpUserError.EMAIL_INCORRECT);
   }

   /**
    * This method tests update of an user with invalid level.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateWithInvalidLevel()
        throws Exception {
      createDefaultUser();

      // now retrieve user and check its data.
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      // now update user with Administrator username.
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING, null);

      // set wrong level (not number)
      userData.put(OpUserService.USER_LEVEL, "aaa");

      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_ID, user.locator());
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.updateUser(session, request);
      assertError(response, OpUserError.INVALID_USER_LEVEL);

      // use level not in range
      userData.put(OpUserService.USER_LEVEL, "999");
      response = userService.updateUser(session, request);
      assertError(response, OpUserError.INVALID_USER_LEVEL);
   }

   /**
    * This method tests update of an user with wrong retyped password.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateWithWrongRetypedPass()
        throws Exception {
      createDefaultUser();

      // now retrieve user and check its data.
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      // now update user with Administrator username.
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING, null);

      XMessage request = new XMessage();

      userData.put(OpUserService.PASSWORD_RETYPED, "adasdsad");
      request.setArgument(OpUserService.USER_ID, user.locator());
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.updateUser(session, request);
      assertError(response, OpUserError.PASSWORD_MISMATCH);
   }

   // -------------------------- GROUPS TESTS --------------------------------
   /**
    * Test adding of a group (happy flow).
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddGroup()
        throws Exception {
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      assertEquals(TEST_GROUP_NAME, group.getName());
      assertEquals(DUMMY_STRING, group.getDescription());
   }

   /**
    * Test adding of a group without name).
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddGroupWithoutName()
        throws Exception {
      Map groupData = OpUserTestDataFactory.createGroupData(null, DUMMY_STRING, null);

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertError(response, OpUserError.GROUP_NAME_MISSING);
   }

   /**
    * Test adding of a group with a name which is already into database.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddGroupWithExistingName()
        throws Exception {
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      Map groupData = OpUserTestDataFactory.createGroupData(TEST_GROUP_NAME, DUMMY_STRING, null);

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertError(response, OpUserError.GROUP_NAME_ALREADY_USED);
   }

   /**
    * Here we test add of a group into another group.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddGroupWithSuperGroups()
        throws Exception {
      String superGroupName = "superGroup";
      Map groupData = OpUserTestDataFactory.createGroupData(superGroupName, DUMMY_STRING, null);

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertNoError(response);

      // now retrieve group and check its data.
      OpGroup superGroup = dataFactory.getGroupByName(superGroupName);
      assertNotNull(superGroup);

      // now try to add a new group with super group.
      groupData = OpUserTestDataFactory.createGroupData(TEST_GROUP_NAME, DUMMY_STRING,
           Arrays.asList(new String[]{String.valueOf(superGroup.locator())}));

      request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      response = userService.insertGroup(session, request);
      assertNoError(response);

      // now retrieve group and check its data.
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      assertEquals(TEST_GROUP_NAME, group.getName());
      assertEquals(DUMMY_STRING, group.getDescription());

      Set superGroups = group.getSuperGroupAssignments();
      assertNotNull(superGroups);
      assertEquals(1, superGroups.size());

      OpGroupAssignment superGroupAssignement = (OpGroupAssignment) superGroups.iterator().next();
      assertNotNull(superGroupAssignement);
      assertEquals(superGroup.getID(), superGroupAssignement.getSuperGroup().getID());
      assertEquals(group.getID(), superGroupAssignement.getSubGroup().getID());
   }

   /**
    * Here we test add of a group with an invalid super group.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAddGroupWithUnknownSuperGroups()
        throws Exception {

      // now try to add a new group with super group.
      Map groupData = OpUserTestDataFactory.createGroupData(TEST_GROUP_NAME, DUMMY_STRING,
           Arrays.asList(new String[]{"OpGroup.9999.xid"}));

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertError(response, OpUserError.SUPER_GROUP_NOT_FOUND);
   }

   /**
    * Test update of a group (happy flow).
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateGroup()
        throws Exception {
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      String newValue = "newValue";
      Map groupData = OpUserTestDataFactory.createGroupData(newValue, newValue, null);

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_ID, group.locator());
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.updateGroup(session, request);
      assertNoError(response);

      group = dataFactory.getGroupByName(newValue);
      assertNotNull(group);
      assertEquals(newValue, group.getName());
      assertEquals(newValue, group.getDescription());
   }

   /**
    * Test update of a group with an unexisting id/locator.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateGroupWithoutId()
        throws Exception {
      Map groupData = OpUserTestDataFactory.createGroupData(TEST_GROUP_NAME, DUMMY_STRING, null);

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_ID, "OpGroup.9999.xid");
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.updateGroup(session, request);
      assertError(response, OpUserError.GROUP_NOT_FOUND);
   }

   /**
    * Test update of a group with a name that already in use.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateGroupWithExistingName()
        throws Exception {
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      Map groupData = OpUserTestDataFactory.createGroupData(OpGroup.EVERYONE_NAME, DUMMY_STRING, null);

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_ID, group.locator());
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.updateGroup(session, request);
      assertError(response, OpUserError.GROUP_NAME_ALREADY_USED);
   }

   /**
    * Test update of a group without name.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateGroupWithoutName()
        throws Exception {
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      Map groupData = OpUserTestDataFactory.createGroupData(null, DUMMY_STRING, null);

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_ID, group.locator());
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.updateGroup(session, request);
      assertError(response, OpUserError.GROUP_NAME_MISSING);
   }

   /**
    * Here we test update groups with super group.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateGroupWithSuperGroups()
        throws Exception {
      String superGroupName = "superGroup";
      Map groupData = OpUserTestDataFactory.createGroupData(superGroupName, DUMMY_STRING, null);

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertNoError(response);

      // now retrieve group and check its data.
      OpGroup superGroup = dataFactory.getGroupByName(superGroupName);
      assertNotNull(superGroup);

      // now create a group without super group
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      // now update group
      request = new XMessage();
      groupData = OpUserTestDataFactory.createGroupData(TEST_GROUP_NAME, DUMMY_STRING,
           Arrays.asList(new String[]{superGroup.locator()}));
      request.setArgument(OpUserService.GROUP_ID, group.locator());
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      response = userService.updateGroup(session, request);
      assertNoError(response);

      // now retrieve group and check its data.
      group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      Set superGroups = group.getSuperGroupAssignments();
      assertNotNull(superGroups);
      assertEquals(1, superGroups.size());

      OpGroupAssignment superGroupAssignement = (OpGroupAssignment) superGroups.iterator().next();
      assertNotNull(superGroupAssignement);
      assertEquals(superGroup.getID(), superGroupAssignement.getSuperGroup().getID());
      assertEquals(group.getID(), superGroupAssignement.getSubGroup().getID());
   }

   /**
    * Here we test update groups with invalid super group.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateGroupWithInvalidSuperGroups()
        throws Exception {
      // now create a group without super group
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      // now update group
      XMessage request = new XMessage();
      Map groupData = OpUserTestDataFactory.createGroupData(TEST_GROUP_NAME, DUMMY_STRING,
           Arrays.asList(new String[]{"OpGroup.9999.xid"}));
      request.setArgument(OpUserService.GROUP_ID, group.locator());
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.updateGroup(session, request);
      assertError(response, OpUserError.SUPER_GROUP_NOT_FOUND);
   }

   /**
    * Here we test update groups with super group(s) that creates a loop.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testUpdateGroupWithLoops()
        throws Exception {
      String firstGroupName = "Group_A";
      Map groupData = OpUserTestDataFactory.createGroupData(firstGroupName, DUMMY_STRING, null);

      // Create first group
      XMessage request = new XMessage();
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertNoError(response);
      OpGroup firstGroup = dataFactory.getGroupByName(firstGroupName);
      assertNotNull(firstGroup);

      // create second group
      String secondGroupName = "Group_B";
      groupData = OpUserTestDataFactory.createGroupData(secondGroupName, DUMMY_STRING,
           Arrays.asList(new String[]{firstGroup.locator()}));
      request = new XMessage();
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      response = userService.insertGroup(session, request);
      assertNoError(response);

      // now retrieve group and check its data.
      OpGroup secondGroup = dataFactory.getGroupByName(secondGroupName);
      assertNotNull(secondGroup);

      // create second group
      String thirdGroupName = "Group_C";
      groupData = OpUserTestDataFactory.createGroupData(thirdGroupName, DUMMY_STRING,
           Arrays.asList(new String[]{secondGroup.locator()}));
      request = new XMessage();
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      response = userService.insertGroup(session, request);
      assertNoError(response);

      // now retrieve group and check its data.
      OpGroup thirdGroup = dataFactory.getGroupByName(thirdGroupName);
      assertNotNull(thirdGroup);

      // now update group Group_A to have as a parent Group_C. In this case we would have a loop.
      groupData = OpUserTestDataFactory.createGroupData(firstGroupName, DUMMY_STRING,
           Arrays.asList(new String[]{thirdGroup.locator()}));
      request = new XMessage();
      request.setArgument(OpUserService.GROUP_ID, firstGroup.locator());
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      response = userService.updateGroup(session, request);
      assertError(response, OpUserError.LOOP_ASSIGNMENT);
   }

   /**
    * This method tests the assignment of an user or group to a parent group.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testAssignmentToGroup()
        throws Exception {
      createDefaultUser();
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      createDefaultGroup();
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      // create supergroup
      String superGroupName = "SuperGroup";
      Map groupData = OpUserTestDataFactory.createGroupData(superGroupName, DUMMY_STRING, null);
      XMessage req = new XMessage();
      req.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage res = userService.insertGroup(session, req);
      assertNoError(res);
      OpGroup superGroup = dataFactory.getGroupByName(superGroupName);
      assertNotNull(superGroup);

      // Now try to assign these subjects to an invalid group.
      req = new XMessage();
      req.setArgument(OpUserService.SUBJECT_IDS, null);
      req.setArgument(OpUserService.TARGET_GROUP_ID, "OpGroup.9999.xid");
      res = userService.assignToGroup(session, req);
      assertError(res, OpUserError.SUPER_GROUP_NOT_FOUND);

      // now create a new SuperGroup and assign user/group to it
      req = new XMessage();
      String[] subjectIds = new String[]{user.locator(), group.locator()};
      req.setArgument(OpUserService.SUBJECT_IDS, Arrays.asList(subjectIds));
      req.setArgument(OpUserService.TARGET_GROUP_ID, superGroup.locator());
      res = userService.assignToGroup(session, req);
      assertNoError(res);

      // check id user and group was linked to supergroup
      superGroup = dataFactory.getGroupByName(superGroupName);
      Set subGroupAssignments = superGroup.getSubGroupAssignments();
      assertNotNull(subGroupAssignments);
      assertEquals(1, subGroupAssignments.size());
      Set subUserAssignments = superGroup.getUserAssignments();
      assertNotNull(subUserAssignments);
      assertEquals(1, subUserAssignments.size());

      user = dataFactory.getUserByName(TEST_USER_NAME);
      Set userAssignments = user.getAssignments();
      assertNotNull(userAssignments);
      assertEquals(1, userAssignments.size());
      OpUserAssignment userAssignment = (OpUserAssignment) userAssignments.iterator().next();
      assertNotNull(userAssignment);
      assertEquals(superGroup.getID(), userAssignment.getGroup().getID());

      group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      Set groupAssignments = group.getSuperGroupAssignments();
      assertNotNull(groupAssignments);
      assertEquals(1, groupAssignments.size());
      OpGroupAssignment groupAssignment = (OpGroupAssignment) groupAssignments.iterator().next();
      assertNotNull(groupAssignment);
      assertEquals(superGroup.getID(), groupAssignment.getSuperGroup().getID());

   }

   /**
    * This metho tests deletion of a group to group assignment.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testDeleteGroupToGroupAssignment()
        throws Exception {
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup superGroup = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(superGroup);

      // now try to add a new group with super group.
      String subGroupName = "SubGroup";
      Map groupData = OpUserTestDataFactory.createGroupData(subGroupName, DUMMY_STRING,
           Arrays.asList(new String[]{String.valueOf(superGroup.locator())}));

      XMessage request = new XMessage();
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertNoError(response);

      // now retrieve group and check its data.
      OpGroup subGroup = dataFactory.getGroupByName(subGroupName);
      assertNotNull(subGroup);

      // Now try to delete group to group assigment.
      List superIds = Arrays.asList(new String[]{superGroup.locator()});
      List subIds = Arrays.asList(new String[]{subGroup.locator()});

      request = new XMessage();
      request.setArgument(OpUserService.SUPER_SUBJECT_IDS, superIds);
      request.setArgument(OpUserService.SUB_SUBJECT_IDS, subIds);
      response = userService.deleteAssignments(session, request);
      assertNoError(response);

      // now check if the relation ship was realy deleted
      superGroup = dataFactory.getGroupByName(TEST_GROUP_NAME);
      Set subGroupsAss = superGroup.getSubGroupAssignments();
      assertNotNull(subGroupsAss);
      assertEquals(0, subGroupsAss.size());

      subGroup = dataFactory.getGroupByName(subGroupName);
      Set superGroupsAss = subGroup.getSuperGroupAssignments();
      assertNotNull(superGroupsAss);
      assertEquals(0, superGroupsAss.size());
   }

   /**
    * This metho tests deletion of a user to group assignment.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testDeleteUserToGroupAssignment()
        throws Exception {
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup superGroup = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(superGroup);

      // now try to add a new user with super group.
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING,
           Arrays.asList(new String[]{superGroup.locator()}));
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      assertNoError(response);

      // now retrieve group and check its data.
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      // Now try to delete group to group assigment.
      List superIds = Arrays.asList(new String[]{superGroup.locator()});
      List subIds = Arrays.asList(new String[]{user.locator()});

      request = new XMessage();
      request.setArgument(OpUserService.SUPER_SUBJECT_IDS, superIds);
      request.setArgument(OpUserService.SUB_SUBJECT_IDS, subIds);
      response = userService.deleteAssignments(session, request);
      assertNoError(response);

      // now check if the relation ship was realy deleted
      superGroup = dataFactory.getGroupByName(TEST_GROUP_NAME);
      Set subGroupsAss = superGroup.getSubGroupAssignments();
      assertNotNull(subGroupsAss);
      assertEquals(0, subGroupsAss.size());

      user = dataFactory.getUserByName(TEST_USER_NAME);
      Set superGroupsAss = user.getAssignments();
      assertNotNull(superGroupsAss);
      assertEquals(0, superGroupsAss.size());
   }

   /**
    * This method tests deletion of an user.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testDeleteUser()
        throws Exception {

      // count existing users.
      OpBroker broker = session.newBroker();
      List allUsers = dataFactory.getAllUsers(broker);
      assertNotNull(allUsers);
      int usersListSize = allUsers.size();
      broker.close();

      createDefaultUser();
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      // Now delete user.
      XMessage req = new XMessage();
      req.setArgument(OpUserService.SUBJECT_IDS, Arrays.asList(new String[]{user.locator()}));
      XMessage res = userService.deleteSubjects(session, req);
      assertNoError(res);

      broker = session.newBroker();
      allUsers = dataFactory.getAllUsers(broker);
      assertNotNull(allUsers);
      assertEquals(usersListSize, allUsers.size());
      broker.close();
      
   }

   /**
    * This method tests deletion of aa group.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testDeleteGroup()
        throws Exception {

      // count existing groups.
      OpBroker broker = session.newBroker();
      List allGroups = dataFactory.getAllGroups(broker);
      assertNotNull(allGroups);
      int groupsListSize = allGroups.size();
      broker.close();

      createDefaultGroup();
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      // Now delete group.
      XMessage req = new XMessage();
      req.setArgument(OpUserService.SUBJECT_IDS, Arrays.asList(group.locator()));
      XMessage res = userService.deleteSubjects(session, req);
      assertNoError(res);

      broker = session.newBroker();
      allGroups = dataFactory.getAllGroups(broker);
      assertNotNull(allGroups);
      assertEquals(groupsListSize, allGroups.size());
      broker.close();
   }

   /**
    * This method tests deletion of current logged in user or Everyone group.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testWrongDeletions()
        throws Exception {
      OpGroup group = dataFactory.getGroupByName(OpGroup.EVERYONE_NAME);
      assertNotNull(group);

      // Now try to delete Everyone group
      XMessage req = new XMessage();
      req.setArgument(OpUserService.SUBJECT_IDS, Arrays.asList(new String[]{group.locator()}));
      XMessage res = userService.deleteSubjects(session, req);
      assertError(res, OpUserError.EVERYONE_GROUP);

      // now try to delete current user
      req = new XMessage();
      req.setArgument(OpUserService.SUBJECT_IDS,
           Arrays.asList(new String[]{OpLocator.locatorString(OpUser.USER, session.getUserID())}));
      res = userService.deleteSubjects(session, req);
      assertError(res, OpUserError.SESSION_USER);
   }

   /**
    * This method tests groups expand.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testExpandGroup()
        throws Exception {
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup superGroup = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(superGroup);

      // now try to add a new group with super group.
      String subGroupName = "SubGroup";
      Map groupData = OpUserTestDataFactory.createGroupData(subGroupName, DUMMY_STRING,
           Arrays.asList(new String[]{String.valueOf(superGroup.locator())}));

      XMessage request = new XMessage();
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertNoError(response);

      // now retrieve group and check its data.
      OpGroup subGroup = dataFactory.getGroupByName(subGroupName);
      assertNotNull(subGroup);

      // Now create also an user
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING,
           Arrays.asList(new String[]{String.valueOf(superGroup.locator())}));

      // create user
      request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      response = userService.insertUser(session, request);
      assertNoError(response);

      // now retrieve user and check its data.
      OpUser subUser = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(subUser);

      // Now test expand groups method
      request = new XMessage();

      request.setArgument("source_group_locator", superGroup.locator());
      request.setArgument("outlineLevel", new Integer(1));
      response = userService.expandGroup(session, request);
      assertNoError(response);

      List list = (List) response.getArgument(OpProjectConstants.CHILDREN);
      assertNotNull(list);
      assertEquals(2, list.size());

   }

   /**
    * This method tests groups expand with filters.
    *
    * @throws Exception If somethign goes wrong.
    */
   public void testExpandFilteredGroup()
        throws Exception {
      createDefaultGroup();

      // now retrieve group and check its data.
      OpGroup superGroup = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(superGroup);

      // now try to add a new group with super group.
      String subGroupName = "SubGroup";
      Map groupData = OpUserTestDataFactory.createGroupData(subGroupName, DUMMY_STRING,
           Arrays.asList(new String[]{String.valueOf(superGroup.locator())}));

      XMessage request = new XMessage();
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertNoError(response);

      // now retrieve group and check its data.
      OpGroup subGroup = dataFactory.getGroupByName(subGroupName);
      assertNotNull(subGroup);

      // Now create also an user
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING,
           Arrays.asList(new String[]{String.valueOf(superGroup.locator())}));

      // create user
      request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      response = userService.insertUser(session, request);
      assertNoError(response);

      // now retrieve user and check its data.
      OpUser subUser = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(subUser);

      // Now test expand groups method
      request = new XMessage();

      request.setArgument("source_group_locator", superGroup.locator());
      request.setArgument("outlineLevel", new Integer(1));
      request.setArgument("EnableUsers", Boolean.TRUE);
      request.setArgument("EnableGroups", Boolean.TRUE);
      request.setArgument("FilteredSubjectIds", Arrays.asList(new String[]{subUser.locator()}));
      request.setArgument("IncludeParentsInFilter", Boolean.TRUE);
      response = userService.expandFilteredGroup(session, request);
      assertNoError(response);

      List list = (List) response.getArgument(OpProjectConstants.CHILDREN);
      assertNotNull(list);
      assertEquals(1, list.size());

   }


   // -------------------------- METHODS SECURITY TESTS --------------------------------
   /**
    * Here we test scenarios where user which uses service method does not have enougn rights.
    * For that we create another user, log that user in and call secured methods with him.
    *
    * @throws Exception If somethign goes wrong.
    */
   //<FIXME author="Mihai Costin" description="request parameter should be updated for each method call!!">  
   public void testMethodsSecurity()
        throws Exception {

      createDefaultUser();
      OpUser user = dataFactory.getUserByName(TEST_USER_NAME);
      assertNotNull(user);

      createDefaultGroup();
      OpGroup group = dataFactory.getGroupByName(TEST_GROUP_NAME);
      assertNotNull(group);

      // create supergroup
      String superGroupName = "SuperGroup";
      Map groupData = OpUserTestDataFactory.createGroupData(superGroupName, DUMMY_STRING, null);
      XMessage request = new XMessage();
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertNoError(response);
      OpGroup superGroup = dataFactory.getGroupByName(superGroupName);
      assertNotNull(superGroup);

      //log-out Administrator
      logOut();
      logIn(TEST_USER_NAME, TEST_PASS);

      // now try to call secured methods.
      List subIds = Arrays.asList(new String[]{user.locator()});

      request = new XMessage();
      request.setArgument(OpUserService.TARGET_GROUP_ID, superGroup.locator());
      request.setArgument(OpUserService.SUBJECT_IDS, subIds);

      try {
         // check insertUser method security
         response = userService.insertUser(session, request);
         assertError(response, OpUserError.INSUFFICIENT_PRIVILEGES);

         // check insertUser method security
         response = userService.insertGroup(session, request);
         assertError(response, OpUserError.INSUFFICIENT_PRIVILEGES);

         // check insertUser method security
         response = userService.updateUser(session, request);
         assertError(response, OpUserError.INSUFFICIENT_PRIVILEGES);

         // check insertUser method security
         response = userService.updateGroup(session, request);
         assertError(response, OpUserError.INSUFFICIENT_PRIVILEGES);

         // check insertUser method security
         response = userService.deleteAssignments(session, request);
         assertError(response, OpUserError.INSUFFICIENT_PRIVILEGES);

         // check insertUser method security
         response = userService.deleteSubjects(session, request);
         assertError(response, OpUserError.INSUFFICIENT_PRIVILEGES);

         // check insertUser method security
         response = userService.assignToGroup(session, request);
         assertError(response, OpUserError.INSUFFICIENT_PRIVILEGES);
      }
      finally {
         // log out the other user and log-in Administrator.
         logOut();
         logIn();
      }
   }

   /**
    * Tests that after a user with a default language preference logs in and out, after logout,
    * the session's language is the same as the system language.
    *
    * @throws Exception if anything fails.
    */
   public void testSystemDefaultLanguage()
        throws Exception {
      String language = "de";
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_USER_NAME, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, language, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING, null);

      //create user
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      assertNoError(response);

      logIn(TEST_USER_NAME, TEST_USER_NAME);
      assertEquals("The locale of the logged in user is not correct ", language, session.getLocale().getID());
      logOut();
      String systemLocaleId = XLocaleManager.findLocale(OpSettings.get(OpSettings.USER_LOCALE)).getID();
      assertEquals("The locale of the session is not the system locale ", systemLocaleId, session.getLocale().getID());
   }

   // ------------- Helper methods ----------------------------
   /**
    * Creates default user with default data.
    *
    * @return service response
    */
   private XMessage createDefaultUser() {
      Map userData = OpUserTestDataFactory.createUserData(TEST_USER_NAME, TEST_PASS, DUMMY_STRING, OpUser.MANAGER_USER_LEVEL,
           DUMMY_STRING, DUMMY_STRING, TEST_LANGUAGE, TEST_EMAIL, DUMMY_STRING, DUMMY_STRING, DUMMY_STRING, null);

      XMessage request = new XMessage();

      //create user
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      assertNoError(response);

      return response;
   }

   /**
    * Creates default group with default data.
    *
    * @return service response
    */
   private XMessage createDefaultGroup() {
      Map groupData = OpUserTestDataFactory.createGroupData(TEST_GROUP_NAME, DUMMY_STRING, null);

      XMessage request = new XMessage();

      // first try with lower case (for all letters)
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      assertNoError(response);

      return response;
   }
}
