/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user.test;


import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.*;
import onepoint.project.util.OpSHA1;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;
import org.jmock.core.Constraint;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;

import java.util.*;

/**
 * @author mihai.costin
 * @author ovidiu.lupas
 *         Date: Dec 1, 2005
 *         <p/>
 *         Test Case for class OpUserService. Will test the functionality of the user service using mock objects.
 *         The CRUD operations are performed by the administrator only.
 */
//<FIXME author="Mihai Costin" description="more tests needed">
//complete tests for "display name" for an user

//add tests to include also the permissions - now, for existing tests, user id on session = administrator id on session = 0
//for delete subjects - a test for a non-empty group
//</FIXME>

public class OpUserServiceTest extends onepoint.project.test.OpServiceAbstractTest {

   protected OpUserService userService;

   private String userPassword;
   private OpUser user;

   private OpGroup group;
   private OpGroup superGroup;

   private OpSHA1 sha = new OpSHA1();

   private static final long EVERYONE_GROUP_ID_LONG = 29090;
   private static final String EVERYONE_GROUP_LOCATOR = "OpGroup." + EVERYONE_GROUP_ID_LONG + ".xid";

   private static final String GROUP_NAME = "groupOne";
   private static final long GROUP_ID_LONG = 1989;
   private static final long NON_EXISTENT_GROUP_ID_LONG = 5000;
   private static final String GROUP_LOCATOR = "OpGroup." + GROUP_ID_LONG + ".xid";
   private static final String NON_EXISTENT_GROUP_LOCATOR = "OpGroup." + NON_EXISTENT_GROUP_ID_LONG + ".xid";

   private static final String SUPER_GROUP_NAME = "superGroup";
   private static final long SUPER_GROUP_ID_LONG = 1966;
   private static final long NON_EXISTENT_SUPER_GROUP_ID_LONG = 6000;
   private static final String SUPER_GROUP_LOCATOR = "OpGroup." + SUPER_GROUP_ID_LONG + ".xid";
   private static final String NON_EXISTENT_SUPER_GROUP_LOCATOR = "OpGroup." + NON_EXISTENT_SUPER_GROUP_ID_LONG + ".xid";

   private static final long ADMINISTRATOR_ID_LONG = 1;
   private static final String ADMINISTRATOR_LOCATOR = "OpUser." + ADMINISTRATOR_ID_LONG + ".xid";
   private static final long USER_ID_LONG = 1987;
   private static final String USER_LOCATOR = "OpUser." + USER_ID_LONG + ".xid";
   private static final long NON_EXISTENT_USER_ID_LONG = 2000;
   private static final String NON_EXISTENT_USER_LOCATOR = "OpUser." + NON_EXISTENT_USER_ID_LONG + ".xid";
   private static final String USER_LOGIN_NAME = "userOneLoginName";

   private static final long ASSIGNMENT_ID_LONG = 5;
   private static final String ASSIGNMENT_LOCATOR = "OpUserAssignment." + ASSIGNMENT_ID_LONG + ".xid";

   //queries
   private static final String SELECT_USER_BY_NAME = "select user from OpUser as user where user.Name = ?";
   private static final String SELECT_SUBJECT_ID_BY_NAME = "select subject.ID from OpSubject as subject where subject.Name = ?";

   private static final String SELECT_USER_BY_ID = "select subject from OpUser as subject where subject.ID in (:subjectIds)";
   private static final String SELECT_GROUP_BY_ID = "select subject from OpGroup as subject where subject.ID in (:subjectIds)";

   private static final String SELECT_USER_ASSIGNMENTS_BY_ID =
        "select assignment from OpUserAssignment as assignment where assignment.User.ID = :userId and assignment.Group.ID in (:groupIds)";

   private static final String SELECT_GROUP_ASSIGNMENTS_BY_ID =
        "select assignment from OpGroupAssignment as assignment where assignment.SubGroup.ID = :subGroupId and assignment.SuperGroup.ID in (:superGroupIds)";

   private static final String SELECT_USER_ASSIGNMENT =
        "select assignment from OpUserAssignment as assignment where assignment.User.ID = ? and assignment.Group.ID = ?";

   private static final String SELECT_GROUP_ASSIGNMENT =
        "select assignment from OpGroupAssignment as assignment where assignment.SubGroup.ID = ? and assignment.SuperGroup.ID = ?";

   private static final String SELECT_USER_ASSIGNMENT_ID =
        "select assignment.ID from OpUserAssignment as assignment where assignment.User.ID = ? and assignment.Group.ID = ?";

   private static final String SELECT_GROUP_ASSIGNMENT_ID =
        "select assignment.ID from OpGroupAssignment as assignment where assignment.SubGroup.ID = ? and assignment.SuperGroup.ID = ?";

   //query helpers
   private static final String SUBJECTS_COLLECTION_FOR_QUERY = "subjectIds";
   private static final String USER_ID_FOR_QUERY = "userId";
   private static final String GROUP_ID_FOR_QUERY = "groupIds";
   private static final String SUPER_GROUP_ID_FOR_QUERY = "superGroupIds";
   private static final String SUB_GROUP_ID_FOR_QUERY = "subGroupId";


   public void setUp() {

      super.setUp();

      userService = new OpUserService();
      queryResults = new ArrayList();

      //prepare a user from the "db"
      String userFirstName = "userOneFirstName";
      String userLastName = "userOneLastName";
      userPassword = sha.calculateHash("userOnePassword");
      String userLoginName = USER_LOGIN_NAME;

      user = new OpUser();
      OpContact firstUserContact = new OpContact();
      user.setID(USER_ID_LONG);
      Set prefs = new HashSet();
      OpPreference myPref = new OpPreference();
      myPref.setName(OpPreference.LOCALE);
      myPref.setValue("en");
      myPref.setUser(user);
      prefs.add(myPref);
      user.setPreferences(prefs);
      user.setPassword(userPassword);
      firstUserContact.setFirstName(userFirstName);
      firstUserContact.setLastName(userLastName);
      user.setName(userLoginName);
      user.setAssignments(new HashSet());
      user.setResources(new HashSet());
      user.setContact(firstUserContact);

      //prepare a group from the "db"
      String groupDescription = "groupOneDescription";

      group = new OpGroup();
      group.setName(GROUP_NAME);
      group.setID(GROUP_ID_LONG);
      group.setDescription(groupDescription);
      group.setSuperGroupAssignments(new HashSet());
      group.setSubGroupAssignments(new HashSet());
      group.setUserAssignments(new HashSet());

      superGroup = new OpGroup();
      superGroup.setName(SUPER_GROUP_NAME);
      superGroup.setID(SUPER_GROUP_ID_LONG);
      superGroup.setDescription("Super group description");
      superGroup.setSuperGroupAssignments(new HashSet());
      superGroup.setSubGroupAssignments(new HashSet());
      superGroup.setUserAssignments(new HashSet());
   }


   /**
    * Specifies the behaviour of the mocked methods.
    *
    * @param invocation contains the object and the invoked method
    * @return depends on the invoked moethod
    * @throws IllegalArgumentException if no such method was defined in this mapping
    */
   public Object invocationMatch(Invocation invocation)
        throws IllegalArgumentException {
      //the invoked method parameter values
      List parameterValues = invocation.parameterValues;

      if (invocation.invokedMethod.getName().equals(ITERATE_METHOD)) {
         return queryResults.iterator();
      }
      else if (invocation.invokedMethod.getName().equals(USER_METHOD)) {
         return user;
      }
      else if (invocation.invokedMethod.getName().equals(GET_OBJECT_METHOD)) {
         if (parameterValues.get(0).equals(GROUP_LOCATOR)) {
            return group;
         }
         if (parameterValues.get(0).equals(SUPER_GROUP_LOCATOR)) {
            return superGroup;
         }
         if (parameterValues.get(0).equals(USER_LOCATOR)) {
            return user;
         }
         //(OpGroup.class, groupId.longValue())
         if (parameterValues.get(0).equals(OpGroup.class) && parameterValues.get(1).equals(new Long(GROUP_ID_LONG))) {
            return group;
         }
         if (parameterValues.get(0).equals(OpGroup.class) && parameterValues.get(1).equals(new Long(SUPER_GROUP_ID_LONG)))
         {
            return superGroup;
         }
         if (parameterValues.get(0).equals(OpUser.class) && parameterValues.get(1).equals(new Long(USER_ID_LONG))) {
            return user;
         }
         //default
         return null;
      }
      else if (invocation.invokedMethod.getName().equals(GET_ADMINISTRATOR_ID_METHOD)) {
         return new Long(ADMINISTRATOR_ID_LONG);
      }
      else {
         throw new IllegalArgumentException("No such object.method defined in this stub: " + invocation.invokedMethod.getName());
      }
   }

   /**
    * Tests that a user with a corect user name and password will be corectly "signed on"
    */
   public void testSignOn() {

      applySettingsExpectation();

      XMessage request = new XMessage();
      request.setArgument(OpUserService.LOGIN, USER_LOGIN_NAME);
      request.setArgument(OpUserService.PASSWORD, userPassword);

      queryResults.add(user);

      //a broker must be created and then the user must be found
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_USER_BY_NAME)).will(methodStub);

      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(USER_LOGIN_NAME)});

      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      mockSession.expects(once()).method(AUTHENTICATE_USER_METHOD).with(new Constraint[]{same(mockBroker.proxy()), same(user)});

      mockSession.expects(once()).method(CLEAR_VARIABLES_METHOD);

      //also the locale for the user must be set if it's in the prefs.
      mockSession.expects(once()).method(SET_LOCALE_METHOD).with(eq(userLocale));

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.signOn((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests that a user with a wrong password will not be signed in and an error message will be returned
    */
   public void testSignOnBadPassword() {

      applySettingsExpectation();

      XMessage request = new XMessage();
      request.setArgument(OpUserService.LOGIN, USER_LOGIN_NAME);
      request.setArgument(OpUserService.PASSWORD, "notPass");

      queryResults.add(user);

      //a broker must be created and then the user must be found
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_USER_BY_NAME)).will(methodStub);

      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(USER_LOGIN_NAME)});

      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.signOn((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been found ", result.getError());
   }

   /**
    * Tests that a user with a wrong user name will not be signed in and an error message will be returned
    */
   public void testSignOnNoUser() {

      XMessage request = new XMessage();
      String badUser = "userNoOne";
      request.setArgument(OpUserService.LOGIN, badUser);
      request.setArgument(OpUserService.PASSWORD, "notPass");

      queryResults.add(user);

      //a broker must be created and then the user must be found
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(once()).method(NEW_QUERY_METHOD).
           with(eq(SELECT_USER_BY_NAME)).
           will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(badUser)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.signOn((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been found ", result.getError());
   }


   /**
    * Tests if a user is corectly inserted when all given values are correct.
    */
   public void testInsertUser() {

      XMessage request = new XMessage();
      String newUserLogin = "loginName";

      List groups = new ArrayList();
      String groupLocator = GROUP_LOCATOR;
      groups.add(groupLocator);

      Map userValues = createUserData("userFirstName", "userLastName", newUserLogin, "password", "password", "email@email.com",
           "456879", "4578900", "450897", groups);

      userValues.put(OpUserService.ASSIGNED_GROUPS, groups);

      Constraint testUser = createUserConstraint(userValues);
      Constraint testContact = createUserContactConstraint(userValues);

      request.setArgument(OpUserService.USER_DATA, userValues);
      Constraint testPreference = createLanguageConstraint(userValues);

      //no queryResults should be found by find() on broker
      queryResults = new ArrayList();
      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the user is searched for (a user can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(newUserLogin)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the groups of the user from the user data must be added to the created user
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(XValidator.choiceID(groupLocator))).will(methodStub);

      //the user must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testUser);

      //the user's contact must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testContact);

      Constraint testUsersGroups = createUserAssignmentConstraint(newUserLogin, GROUP_NAME);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testUsersGroups);

      //the user's preferences must be persisted
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(isA(OpPreference.class));

      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.insertUser((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests if a user is corectly inserted when configuration allows empty password fields.
    */
   public void testInsertUserWhenConfigurationAllowsEmptyPassswords() {

      XMessage request = new XMessage();

      String newUserLogin = "loginName";

      List groups = new ArrayList();
      String groupLocator = GROUP_LOCATOR;
      groups.add(groupLocator);

      Map userValues = createUserData("userFirstName", "userLastName", newUserLogin, null, null, "email@email.com", "456879",
           "4578900", "450897", groups);

      Constraint testUser = createUserConstraint(userValues);
      Constraint testContact = createUserContactConstraint(userValues);

      request.setArgument(OpUserService.USER_DATA, userValues);
      Constraint testPreference = createLanguageConstraint(userValues);

      //no queryResults should be found by find() on broker
      queryResults = new ArrayList();
      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the user is searched for (a user can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(newUserLogin)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the groups of the user from the user data must be added to the created user
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(XValidator.choiceID(groupLocator))).will(methodStub);

      //the user must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testUser);

      //the user's preferences must be persisted
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(isA(OpPreference.class));

      //the user's contact must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testContact);

      Constraint testUsersGroups = createUserAssignmentConstraint(newUserLogin, GROUP_NAME);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testUsersGroups);

      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);
      //set up the configuration to allow empty passwords fields
      OpSettings.set(OpSettings.ALLOW_EMPTY_PASSWORD, Boolean.toString(true));

      XMessage result = userService.insertUser((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests if a user is not persisted when configuration does not allow empty password fields.
    */
   public void testInsertUserWhenConfigurationNotAllowsEmptyPassswords() {

      XMessage request = new XMessage();
      Map userValues = createUserData("userFirstName", "userLastName", "loginName", null, null, "email@email.com", "456879", "4578900",
           "450897", null);
      request.setArgument(OpUserService.USER_DATA, userValues);

      //no queryResults should be found by find() on broker
      queryResults = new ArrayList();

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      // a new broker must be created
      mockSession.expects(never()).method(NEW_BROKER_METHOD);

      //the user's preferences must be persisted
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //set up the configuration not to allow empty passwords
      OpSettings.set(OpSettings.ALLOW_EMPTY_PASSWORD, Boolean.toString(false));

      XMessage result = userService.insertUser((XSession) mockSession.proxy(), request);
      assertNotNull("Error should have been found becouse the configuration doesn't allow empty password fields", result.getError());
   }


   /**
    * Tests the behavior of insertUser if the user already exists.
    */
   public void testInsertUserAlreadyExists() {

      XMessage request = new XMessage();

      Map userValues = createUserData("userFirstName","userLastName",USER_LOGIN_NAME,"password","password","email@email.com",
                                     "456879","4578900","450897",null);
      request.setArgument(OpUserService.USER_DATA, userValues);

      //one user should be found by find() on broker
      queryResults.add(user);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the user is searched for (a user can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(USER_LOGIN_NAME)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.insertUser((XSession) mockSession.proxy(), request);
      assertNotNull("Error should have been found ", result.getError());
   }

   /**
    * Tests the behavior of insertUser for a invalid user data.
    */
   public void testInsertUserWrongData() {

      insertUserWrongData("first", "last", "", "pass","pass", "email@email.com", "1234", "5678", "90", null);
      insertUserWrongData("first", "last", "login", "wrongpass","", "email@email.com", "1234", "5678", "90", null);
      insertUserWrongData("first", "last", "login", "pass","pass", "_+_+#$@%cddf", "1234", "5678", "90", null);
   }

   /**
    * Tests the behavior of insertUser with an assign to non existing group.
    */
   public void testInsertUserNonExistingSuper() {
      XMessage request = new XMessage();

      String newUserLogin = "loginName";
      List groups = new ArrayList();
      //nonexistent group
      String groupLocator = "OpGroup.54321.xid";
      groups.add(groupLocator);

      Map userValues = createUserData("userFirstName","userLastName",newUserLogin,"password","password","email@email.com",
                                     "456879","4578900","450897",groups);

      Constraint testUser = createUserConstraint(userValues);
      Constraint testContact = createUserContactConstraint(userValues);

      request.setArgument(OpUserService.USER_DATA, userValues);
      Constraint testPreference = createLanguageConstraint(userValues);

      //no queryResults should be found by find() on broker
      queryResults = new ArrayList();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the user is searched for (a user can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(newUserLogin)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the groups of the user from the user data must be added to the created user
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(XValidator.choiceID(groupLocator))).will(methodStub);

      //the user must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testUser);

      //the user's preferences must be persisted
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(isA(OpPreference.class));

      //the user's contact must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testContact);

      Constraint testUserAssignment = createUserAssignmentConstraint(newUserLogin, null);
      //assigned group not found


      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.insertUser((XSession) mockSession.proxy(), request);
      assertNotNull("Error should have been found. The super group doesn't exist", result.getError());

   }

   /**
    * Tests the behavior of updateUser if the user exists.
    */
   public void testUpdateUser() {
      //create the request
      XMessage request = new XMessage();

      //one assigned group for this user
      List userGroups = new ArrayList();
      userGroups.add(GROUP_LOCATOR);
      String newUserLogin = "NewLoginName";

      Map userValues = createUserData("userFirstName","userLastName",newUserLogin,"password","password","email@email.com",
                                     "456879","4578900","450897",userGroups);
      //set up the  OpUserService.USER_ID request parameter
      request.setArgument(OpUserService.USER_ID, USER_LOCATOR);

      //set up the OpUserService.USER_DATA request parameter
      request.setArgument(OpUserService.USER_DATA, userValues);

      Constraint testUser = createUserConstraint(userValues);

      //no queryResults should be found by broker
      queryResults = new ArrayList();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(eq(USER_LOCATOR)).will(methodStub);

      //the user login is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(newUserLogin)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(testUser);
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createUserContactConstraint(userValues));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createLanguageConstraint(userValues));
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(new Constraint[]{eq(OpGroup.class), eq(GROUP_ID_LONG)}).will(methodStub);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createUserAssignmentConstraint(newUserLogin, GROUP_NAME));

      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateUser((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests the behavior of updateUser if the user exists and the password did not change.
    */
   public void testUpdateUserWhenPasswordNotChanged() {
      //create the request
      XMessage request = new XMessage();

      String newUserLogin = "NewLoginName";
      //one assigned group for this user
      List userGroups = new ArrayList();
      userGroups.add(GROUP_LOCATOR);

      Map userValues = createUserData("userFirstName","userLastName",newUserLogin,OpUserService.PASSWORD_TOKEN,"",
            "email@email.com","456879","4578900","450897",userGroups);

      //set up the  OpUserService.USER_ID request parameter
      request.setArgument(OpUserService.USER_ID, USER_LOCATOR);
      //set up the OpUserService.USER_DATA request parameter
      request.setArgument(OpUserService.USER_DATA, userValues);

      //no queryResults should be found by broker
      queryResults = new ArrayList();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(eq(USER_LOCATOR)).will(methodStub);

      //the user login is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(newUserLogin)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(user));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createUserContactConstraint(userValues));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createLanguageConstraint(userValues));
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(new Constraint[]{eq(OpGroup.class), eq(GROUP_ID_LONG)}).will(methodStub);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createUserAssignmentConstraint(newUserLogin, GROUP_NAME));

      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateUser((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests the behavior of updateUser if the user exists and configuration allows empty password fields.
    */
   public void testUpdateUserWhenConfigurationAllowsEmptyPasswords() {
      //create the request
      XMessage request = new XMessage();

      String newUserLogin = "NewLoginName";
      //one assigned group for this user
      List userGroups = new ArrayList();
      userGroups.add(GROUP_LOCATOR);

      Map userValues = createUserData("userFirstName", "userLastName", newUserLogin, "", "", "email@email.com", "456879",
           "4578900", "450897", userGroups);

      //set up the  OpUserService.USER_ID request parameter
      request.setArgument(OpUserService.USER_ID, USER_LOCATOR);
      //set up the OpUserService.USER_DATA request parameter
      request.setArgument(OpUserService.USER_DATA, userValues);

      Constraint testUser = createUserConstraint(userValues);

      //no queryResults should be found by broker
      queryResults = new ArrayList();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(eq(USER_LOCATOR)).will(methodStub);

      //the user login is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(newUserLogin)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(testUser);
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createUserContactConstraint(userValues));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createLanguageConstraint(userValues));
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(new Constraint[]{eq(OpGroup.class), eq(GROUP_ID_LONG)}).will(methodStub);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createUserAssignmentConstraint(newUserLogin, GROUP_NAME));

      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      //set up the configuration to allow empty passwords fields
      OpSettings.set(OpSettings.ALLOW_EMPTY_PASSWORD, Boolean.toString(true));

      XMessage result = userService.updateUser((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests the behavior of updateUser if the user exists and configuration doesn't allow empty password fields.
    */
   public void testUpdateUserWhenConfigurationNotAllowsEmptyPassswords() {
      //create the request
      XMessage request = new XMessage();
      String newUserLogin = "NewLoginName";
      //one assigned group for this user
      List userGroups = new ArrayList();
      userGroups.add(GROUP_LOCATOR);

      Map userValues = createUserData("userFirstName", "userLastName", newUserLogin, null, "kool", "email@email.com", "456879",
           "4578900", "450897", userGroups);
      //set up the  OpUserService.USER_ID request parameter
      request.setArgument(OpUserService.USER_ID, USER_LOCATOR);
      //set up the OpUserService.USER_DATA request parameter
      request.setArgument(OpUserService.USER_DATA, userValues);

      Constraint testUser = createUserConstraint(userValues);

      //no queryResults should be found by broker
      queryResults = new ArrayList();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(eq(USER_LOCATOR)).will(methodStub);

      //the user login is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(newUserLogin)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will never be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD).with(testUser);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      //set up the configuration in order not to allow empty passwords
      OpSettings.set(OpSettings.ALLOW_EMPTY_PASSWORD, Boolean.toString(false));

      XMessage result = userService.updateUser((XSession) mockSession.proxy(), request);
      assertNotNull("Error should have been found becouse the configuration doesn't allow empty password fields", result.getError());
   }


   /**
    * Tests the behavior of updateUser if the super group is removed
    */
   public void testUpdateUserDeleteSuper() {
      //create the request
      XMessage request = new XMessage();

      //first user has as group group
      final OpUserAssignment userToGroup = new OpUserAssignment();
      userToGroup.setUser(user);
      userToGroup.setGroup(group);
      Set assign = new HashSet();
      assign.add(userToGroup);
      user.setAssignments(assign);

      String newUserLogin = "NewLoginName";
      Map userValues = createUserData("userFirstName", "userLastName", newUserLogin, "pass", "pass", "email@email.com", "456879",
           "4578900", "450897", new ArrayList());

      //set up the  OpUserService.USER_ID request parameter
      request.setArgument(OpUserService.USER_ID, USER_LOCATOR);
      //set up the OpUserService.USER_DATA request parameter
      request.setArgument(OpUserService.USER_DATA, userValues);

      Constraint testUser = createUserConstraint(userValues);

      //no queryResults should be found by broker
      queryResults = new ArrayList();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(eq(USER_LOCATOR)).will(methodStub);

      //the user login is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(newUserLogin)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(testUser);
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createLanguageConstraint(userValues));
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createUserContactConstraint(userValues));

      //new query for finding the removed groups
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).
           with(eq(SELECT_USER_ASSIGNMENTS_BY_ID)).
           will(new Stub() {
              public Object invoke(Invocation invocation)
                   throws Throwable {
                 queryResults.add(userToGroup);
                 return query;
              }

              public StringBuffer describeTo(StringBuffer stringBuffer) {
                 return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_USER_ASSIGNMENTS_BY_ID);
              }
           });
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(USER_ID_FOR_QUERY), eq(user.getID())});
      Set expectedGroups = new HashSet();
      expectedGroups.add(new Long(group.getID()));
      mockQuery.expects(once()).method(SET_COLLECTION_METHOD).with(new Constraint[]{eq(GROUP_ID_FOR_QUERY), eq(expectedGroups)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //delete the removed assignments
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(same(userToGroup));

      //transaction must be commited
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateUser((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests the behavior of updateUser if the user doesn't exist.
    */
   public void testUpdateNonExistentUser() {
      //create the request
      XMessage request = new XMessage();

      String newUserLogin = "NewLoginName";
      Map userValues = createUserData("userFirstName", "userLastName", newUserLogin, "pass", "pass", "email@email.com",
           "456879", "4578900", "450897", new ArrayList());
      //set up the  OpUserService.USER_ID request parameter
      String userId = "Non existent used ID";
      request.setArgument(OpUserService.USER_ID, userId);
      //set up the OpUserService.USER_DATA request parameter
      request.setArgument(OpUserService.USER_DATA, userValues);

      //no queryResults should be found by broker
      queryResults = new ArrayList();

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get object depending on OpUserService.USER_ID request parameter
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(userId)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateUser((XSession) mockSession.proxy(), request);
      assertNotNull("Error should have been returned ", result.getError());
   }

   /**
    * Tests the behavior of updateUser if the user login name already exist.
    */
   public void testUpdateUserWithExistentData() {
      //create the request
      XMessage request = new XMessage();

      Map userValues = createUserData("userFirstName", "userLastName",USER_LOGIN_NAME, "pass", "password", "email@email.com",
           "456879", "4578900", "450897", new ArrayList());

      //set up the  OpUserService.USER_ID request parameter
      request.setArgument(OpUserService.USER_ID, USER_LOCATOR);
      //set up the OpUserService.USER_DATA request parameter
      request.setArgument(OpUserService.USER_DATA, userValues);

      //the persisted user with the same login name but different id
      queryResults = new ArrayList();
      queryResults.add(new Long(2));

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get object depending on OpUserService.USER_ID request parameter
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(USER_LOCATOR)).will(methodStub);

      //the user login is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(USER_LOGIN_NAME)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateUser((XSession) mockSession.proxy(), request);
      assertNotNull("Error should have been returned ", result.getError());
   }

   /**
    * Tests the behavior of updateUser for a invalid user data.
    */
   public void testUpdateUserWrongData() {
      updateUserWrongData(USER_LOCATOR, "first", "last", "", "pass","pass","email@email.com", "1234", "5678", "90", null);
      updateUserWrongData(USER_LOCATOR, "first", "last", "login", "wrongpasswor","", "email@email.com", "1234", "5678", "90", null);
      updateUserWrongData(USER_LOCATOR, "first", "last", "login", "pass","pass","cucu_bau#$#$a143.ro", "1234", "5678", "90", null);
   }


   /**
    * Creates a user data test case based on the given user properties.
    *
    * @param first  first name of the user
    * @param last   last name of the user
    * @param login  login name of the user
    * @param pass   password for the user
    * @param retypedPass   retyped password for the user
    * @param email  email of the user
    * @param phone  phone number of the user
    * @param mobile mobile phone number
    * @param fax    fax number
    * @param groups assigned groups for the user <code>XArray</code>
    */
   private void insertUserWrongData(String first, String last, String login, String pass,String retypedPass, String email,
        String phone, String mobile, String fax, ArrayList groups) {

      XMessage request = new XMessage();

      Map userValues = createUserData(first, last, login, pass, retypedPass, email,phone,mobile,fax,groups);
      request.setArgument(OpUserService.USER_DATA, userValues);

      //no user should be found by find() on broker
      queryResults = new ArrayList();
      // a new broker will not be created
      mockSession.expects(never()).method(NEW_BROKER_METHOD);
      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      XMessage result = userService.insertUser((XSession) mockSession.proxy(), request);
      assertNotNull("Error should have been found ", result.getError());
   }

   /**
    * Creates a user data update test case based on the given user properties.
    *
    * @param first  first name of the user
    * @param last   last name of the user
    * @param login  login name of the user
    * @param pass   password for the user
    * @param retypedPass   retyped password for the user
    * @param email  email of the user
    * @param phone  phone number of the user
    * @param mobile mobile phone number
    * @param fax    fax number
    * @param groups assigned groups for the user <code>XArray</code>
    */
   private void updateUserWrongData(String userId, String first, String last, String login, String pass,String retypedPass,
        String email,String phone, String mobile, String fax, ArrayList groups) {

      XMessage request = new XMessage();

      Map userValues = createUserData(first, last, login, pass, retypedPass, email,phone,mobile,fax,groups);
      request.setArgument(OpUserService.USER_DATA, userValues);
       //set up the user id
      request.setArgument(OpUserService.USER_ID, userId);
      //set up updated user data
      request.setArgument(OpUserService.USER_DATA, userValues);

      //no user should be found by broker
      queryResults = new ArrayList();
      // a new broker will not be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //get the user
      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(eq(USER_LOCATOR)).will(methodStub);

      //find the user with the same name
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);

      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(login)});
      //iterate query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);
      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateUser((XSession) mockSession.proxy(), request);
      assertNotNull("Error should have been returned ", result.getError());
   }

   /**
    * Tests the behavior of deleteSubjects for existing subjects.
    */
   public void testDeleteSubjects() {
      XMessage request = new XMessage();
      ArrayList subjectIds = new ArrayList();
      subjectIds.add(USER_LOCATOR);
      subjectIds.add(GROUP_LOCATOR);

      //set up the request
      request.setArgument(OpUserService.SUBJECT_IDS, subjectIds);

      //clear the subject list
      queryResults.clear();

      //broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //session user should not be the admin (check subjects)
      mockSession.expects(atLeastOnce()).method(GET_USER_ID_METHOD);
      //administrator should not delete everyone group
      mockSession.expects(atLeastOnce()).method(EVERYONE_METHOD);

      //a new query must be created to look up the user
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_USER_BY_ID)).will(methodStub).id(SELECT_USER_BY_ID);

      //a new query must be created to look up  the group
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_GROUP_BY_ID)).will(methodStub).id(SELECT_GROUP_BY_ID);

      //set the params for query and search for the given users
      ArrayList expectedIds = new ArrayList();
      expectedIds.add(new Long(USER_ID_LONG));
      expectedIds.add(new Long(GROUP_ID_LONG));

      mockQuery.expects(atLeastOnce()).method(SET_COLLECTION_METHOD).with(new Constraint[]{eq(SUBJECTS_COLLECTION_FOR_QUERY), eq(expectedIds)});

      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).after(SELECT_USER_BY_ID).will(new Stub() {

         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.clear();
            queryResults.add(user);
            return queryResults.iterator();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of iterating the results for the query ").append(SELECT_USER_BY_ID);
         }
      });

      //delete user object
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(same(user));

      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).after(SELECT_GROUP_BY_ID).will(new Stub() {

         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.clear();
            queryResults.add(group);
            return queryResults.iterator();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of iterating the results for the query ").append(SELECT_GROUP_BY_ID);
         }
      });


      mockSession.expects(once()).method(GET_ADMINISTRATOR_ID_METHOD).will(methodStub);

      //delete group object
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(same(group));

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.deleteSubjects((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests the behavior of deleteAssignments for existing user - group assignment.
    */
   public void testDeleteUserAssignments() {

      XMessage request = new XMessage();
      List subSubjectsIds = new ArrayList();
      subSubjectsIds.add(USER_LOCATOR);

      List superSubjectsIds = new ArrayList();
      superSubjectsIds.add(GROUP_LOCATOR);

      //set up the request
      request.setArgument(OpUserService.SUB_SUBJECT_IDS, subSubjectsIds);
      request.setArgument(OpUserService.SUPER_SUBJECT_IDS, superSubjectsIds);

      //clear the subject list
      queryResults.clear();

      //broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //a new query must be created to look up the user - group assignment
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_USER_ASSIGNMENT)).will(methodStub);

      //a new query must be created to look up the group - supergroup assignment
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_GROUP_ASSIGNMENT)).will(methodStub);

      //set placeholders for user-group assignment query
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(USER_ID_LONG)});
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(1), eq(GROUP_ID_LONG)});

      //persistent user assignment
      final OpUserAssignment userAssignment = new OpUserAssignment();
      userAssignment.setUser(user);
      userAssignment.setGroup(group);

      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(new Stub() {

         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.add(userAssignment);
            return queryResults.iterator();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of iterating the results for the query ").append(SELECT_USER_ASSIGNMENT);
         }
      });

      //delete user -group assignemnt object
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(same(userAssignment));

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.deleteAssignments((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests the behavior of deleteAssignments for existing group - superGroup assignment.
    */
   public void testDeleteGroupAssignments() {

      XMessage request = new XMessage();
      List subSubjectsIds = new ArrayList();
      subSubjectsIds.add(GROUP_LOCATOR);

      List superSubjectsIds = new ArrayList();
      superSubjectsIds.add(SUPER_GROUP_LOCATOR);

      //set up the request
      request.setArgument(OpUserService.SUB_SUBJECT_IDS, subSubjectsIds);
      request.setArgument(OpUserService.SUPER_SUBJECT_IDS, superSubjectsIds);

      //clear the subject list
      queryResults.clear();

      //broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //a new query must be created to look up the user - group assignment
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_USER_ASSIGNMENT)).will(methodStub);

      //a new query must be created to look up the group - supergroup assignment
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_GROUP_ASSIGNMENT)).will(methodStub);

      //set placeholders for group-supergroup assignment query
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(GROUP_ID_LONG)});
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(1), eq(SUPER_GROUP_ID_LONG)});

      //persistent group assignment
      final OpGroupAssignment groupAssignment = new OpGroupAssignment();
      groupAssignment.setSubGroup(group);
      groupAssignment.setSuperGroup(superGroup);

      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(new Stub() {

         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.add(groupAssignment);
            return queryResults.iterator();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of iterating the results for the query ").append(SELECT_GROUP_ASSIGNMENT);
         }
      });

      //delete group - supergroup assignemnt object
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(same(groupAssignment));

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.deleteAssignments((XSession) mockSession.proxy(), request);
      assertNoError(result);


   }

   /**
    * Tests the behavior of deleteSubject for a non existing subject.
    */
   public void testDeleteNonExistentSubject() {

      XMessage request = new XMessage();
      ArrayList userIds = new ArrayList();
      long id = 10000;
      String nonExistingId = "OpUser." + id + ".xid";
      userIds.add(nonExistingId);

      //set up the request
      request.setArgument(OpUserService.SUBJECT_IDS, userIds);

      //clear the subject list
      queryResults.clear();

      //broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //session user should not be the admin (check subjects)
      mockSession.expects(once()).method(GET_USER_ID_METHOD);
       //administrator should not delete everyone group (check subjects)
      mockSession.expects(atLeastOnce()).method(EVERYONE_METHOD);

      //a new query must be created to look the user
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_USER_BY_ID)).will(methodStub);

      //a new query must be created to look the group
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_GROUP_BY_ID)).will(methodStub);

      //set the params for query and search for the given users
      ArrayList expectedIds = new ArrayList();
      expectedIds.add(new Long(id));

      mockQuery.expects(atLeastOnce()).method(SET_COLLECTION_METHOD).with(new Constraint[]{eq(SUBJECTS_COLLECTION_FOR_QUERY), eq(expectedIds)});

      //iterate twice (once for user and one for goup)
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      mockSession.expects(once()).method(GET_ADMINISTRATOR_ID_METHOD).will(methodStub);
      //no delete
      mockBroker.expects(never()).method(DELETE_OBJECT_METHOD);

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(userService.deleteSubjects((XSession) mockSession.proxy(), request));

   }

   /**
    * Tests the behavior of deleteSubject for a logged in user.
    */
   public void testDeleteLoggedInUser() {
      XMessage request = new XMessage();
      ArrayList userIds = new ArrayList();
      userIds.add(ADMINISTRATOR_LOCATOR);

      //set up the request
      request.setArgument(OpUserService.SUBJECT_IDS, userIds);
      //administrator should not delete everyone group
      mockSession.expects(atLeastOnce()).method(EVERYONE_METHOD);
      //session user should be the admin but he has no privileges to delete itself
      mockSession.expects(once()).method(GET_USER_ID_METHOD).will(new Stub() {

         public Object invoke(Invocation invocation) throws Throwable {
            return new Long(ADMINISTRATOR_ID_LONG);
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(GET_USER_ID_METHOD);
         }

      });

      XMessage result = userService.deleteSubjects((XSession) mockSession.proxy(), request);

      assertNotNull("Error message should have been returned.The Administrator has no privileges to delete itself", result.getError());

   }

   /**
    * Tests the behavior of deleteSubject for the everyone group.
    */
   public void testDeleteEveryoneGroup() {
      XMessage request = new XMessage();
      ArrayList groupIds = new ArrayList();
      groupIds.add(EVERYONE_GROUP_LOCATOR);

      //set up the request
      request.setArgument(OpUserService.SUBJECT_IDS, groupIds);
      //administrator tries to delete everyone group
      mockSession.expects(atLeastOnce()).method(EVERYONE_METHOD).will(new Stub() {

         public Object invoke(Invocation invocation) throws Throwable {
            OpGroup everyone = new OpGroup();
            everyone.setID(EVERYONE_GROUP_ID_LONG);
            return everyone;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(EVERYONE_METHOD);
         }

      });
      //session user should be the admin but he has no privileges to delete itself
      mockSession.expects(once()).method(GET_USER_ID_METHOD).will(new Stub() {

         public Object invoke(Invocation invocation) throws Throwable {
            return new Long(USER_ID_LONG);
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(GET_USER_ID_METHOD);
         }

      });


      XMessage result = userService.deleteSubjects((XSession) mockSession.proxy(), request);

      assertNotNull("Error message should have been returned.The Administrator has no privileges to delete itself", result.getError());

   }

   /**
    * Creates a constraint that will check if the user param is as expected.
    *
    * @param userData User data used to construct the constraint
    * @return a constraint object.
    */
   private Constraint createUserConstraint(final Map userData) {

      return new Constraint() {
         public boolean eval(Object
              object) {
            if (!(object instanceof OpUser)) {
               return false;
            }
            OpUser testedUser = (OpUser) object;
            return checksUserAgainstData(testedUser, userData);
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Checks if a user is the expected one");
         }
      };
   }

   /**
    * Creates a constraint that will check if the user group is as expected.
    *
    * @param userName expected user name
    * @param group    expected User groups
    * @return a constraint object.
    */
   private Constraint createUserAssignmentConstraint(final String userName, final String group) {

      return new Constraint() {
         public boolean eval(Object object) {
            if (!(object instanceof OpUserAssignment)) {
               return false;
            }
            OpUserAssignment testedAssignment = (OpUserAssignment) object;
            if (!testedAssignment.getUser().getName().equals(userName)) {
               return false;
            }
            return testedAssignment.getGroup().getName().equals(group);
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Checks if a user is the expected one");
         }
      };
   }

   /**
    * Creates a constraint that will check if the group-superGroup relation is as expected.
    *
    * @param groupName      Group data used to construct the constraint
    * @param superGroupName expected superGroup
    * @return a constraint object.
    */
   private Constraint createSuperGroupConstraint(final String groupName, final String superGroupName) {

      return new Constraint() {
         public boolean eval(Object object) {
            if (!(object instanceof OpGroupAssignment)) {
               return false;
            }
            OpGroupAssignment testedAssignment = (OpGroupAssignment) object;
            if (!testedAssignment.getSubGroup().getName().equals(groupName)) {
               return false;
            }
            return testedAssignment.getSuperGroup().getName().equals(superGroupName);
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Checks if a superGroup is the expected one");
         }
      };
   }

   /**
    * Creates a constraint that will check if the contact for an user is as expected.
    *
    * @param userData User data used to construct the constraint
    * @return a constraint object.
    */
   private Constraint createUserContactConstraint(final Map userData) {

      return new Constraint() {
         public boolean eval(Object
              object) {
            if (!(object instanceof OpContact)) {
               return false;
            }
            OpContact testedContact = (OpContact) object;
            if (!testedContact.getEMail().equals(userData.get(OpContact.EMAIL))) {
               return false;
            }
            if (!testedContact.getFax().equals(userData.get(OpContact.FAX))) {
               return false;
            }
            if (!testedContact.getFirstName().equals(userData.get(OpContact.FIRST_NAME))) {
               return false;
            }
            if (!testedContact.getLastName().equals(userData.get(OpContact.LAST_NAME))) {
               return false;
            }
            return testedContact.getMobile().equals(userData.get(OpContact.MOBILE));
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Checks if a user is the expected one");
         }
      };
   }

   /**
    * Creates a constraint object that will beused to test if the preferences are as expected.
    *
    * @return a constraint object
    */
   private Constraint createLanguageConstraint(final Map userData) {
      return new Constraint() {

         public boolean eval(Object object) {

            if (!(object instanceof OpPreference)) {
               return false;
            }
            OpPreference preference = (OpPreference) object;
            if (!preference.getName().equals(OpPreference.LOCALE)) {
               return false;
            }
            if (!(preference.getValue().equals(userData.get("Language")))) {
               return false;
            }
            return checksUserAgainstData(preference.getUser(), userData);
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Checks if the preferences are saved ");
         }
      };
   }

   /**
    * Compares a given user's properties with the values from a given data structure
    *
    * @param testedUser user to be tested
    * @param userData   expected values
    * @return true if the user matches the given values, false otherwise
    */
   private boolean checksUserAgainstData(OpUser testedUser, Map userData) {

      if (!testedUser.getName().equals(userData.get(OpUser.NAME))) {
         return false;
      }
      String userDataPassword = (String)userData.get(OpUser.PASSWORD);
      String testedUserPassword = testedUser.getPassword();
      String token = sha.calculateHash(OpUserService.PASSWORD_TOKEN);
      //password not changed or equal to TOKEN
      if (userDataPassword != null){
         return testedUserPassword.equals(userDataPassword) || userDataPassword.equals(token);
      }
      return true;
   }


   /**
    * Tests that a new group will be corectly inserted provided that the group data is correct
    */
   public void testInsertGroup() {

      XMessage request = new XMessage();
      String groupName = "groupName";
      String groupDescription = "group Description";
      ArrayList superGroup = new ArrayList();
      superGroup.add(SUPER_GROUP_LOCATOR);
      Map groupData = creatGroupData(groupName, groupDescription, superGroup);
      request.setArgument(OpUserService.GROUP_DATA, groupData);

      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //no groups to be found by broker = group name is not taken
      queryResults = new ArrayList();

      //the group name is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(groupName)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //new group must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createGroupConstraint(groupData));

      //relation between group and super must be persisted
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(SUPER_GROUP_LOCATOR)).will(methodStub);
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(new Constraint[]{eq(OpGroup.class), eq(SUPER_GROUP_ID_LONG)}).will(methodStub);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createSuperGroupConstraint(groupName, SUPER_GROUP_NAME));

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.insertGroup((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests the behavior of insertGroup if the group already exists.
    */
   public void testInsertGroupAlreadyExists() {
      XMessage request = new XMessage();
      String groupName = group.getName();
      Map groupData = creatGroupData(groupName, "group Description", null);
      request.setArgument(OpUserService.GROUP_DATA, groupData);

      //clear the group list
      queryResults.clear();
      //one group should be found using find() method on the broker
      queryResults.add(group);

      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the group name is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(groupName)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //error
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.insertGroup((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
   }

   /**
    * Tests the behavior of insertGroup if the group name is missing or is empty.
    */
   public void testInsertGroupWrongData() {
      XMessage request = new XMessage();

      //empty group name
      String groupName = "";
      Map groupData = creatGroupData(groupName, "group Description", null);
      request.setArgument(OpUserService.GROUP_DATA, groupData);

      //no groups to be found by broker
      queryResults.clear();

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      XMessage result = userService.insertGroup((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
   }

   /**
    * Tests the behavior of insertGroup if the super group doesn't exist
    */
   public void testInsertGroupNonExistingSuper() {

      XMessage request = new XMessage();
      String groupName = "groupName";
      String groupDescription = "group Description";
      ArrayList superGroup = new ArrayList();
      long nonExistingSuperId = 12345;
      String nonExistingSuperLocator = "OpGroup." + nonExistingSuperId + ".xid";
      superGroup.add(nonExistingSuperLocator);
      Map groupData = creatGroupData(groupName, groupDescription, superGroup);
      request.setArgument(OpUserService.GROUP_DATA, groupData);

      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //no groups to be found by broker = group name is not taken
      queryResults = new ArrayList();

      //the group name is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(groupName)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //new group must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createGroupConstraint(groupData));

      //relation between group and super must be persisted
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(nonExistingSuperLocator)).will(methodStub);
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(new Constraint[]{eq(OpGroup.class), eq(nonExistingSuperId)}).will(methodStub);

      //no super will be found!

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.insertGroup((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned. The only given super group is invalid. ", result.getError());
   }

   /**
    * Tests the behavior of updateGroup.
    */
   public void testUpdateGroup() {
      XMessage request = new XMessage();
      //set up the request
      request.setArgument(OpUserService.GROUP_ID, GROUP_LOCATOR);

      String groupName = "groupName";
      ArrayList superGroups = new ArrayList();
      superGroups.add(SUPER_GROUP_LOCATOR);
      Map groupData = creatGroupData(groupName, "group Description", superGroups);
      request.setArgument(OpUserService.GROUP_DATA, groupData);

      //no groups to be found by broker query
      queryResults.clear();

      //broker must  be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(GROUP_LOCATOR)).will(methodStub);

      //the group name is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(groupName)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //update group
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createGroupConstraint(groupData));

      //get super group
      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).
           with(new Constraint[]{eq(OpGroup.class), eq(SUPER_GROUP_ID_LONG)}).
           will(methodStub);

      //update supergroup - add a relation
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createSuperGroupConstraint(groupName, SUPER_GROUP_NAME));

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateGroup((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }


   /**
    * Tests the behavior of updateGroup for a group when the superGroup is deleted
    */
   public void testUpdateGroupRemoveSuper() {
      XMessage request = new XMessage();

      final OpGroupAssignment groupToSuper = new OpGroupAssignment();
      groupToSuper.setSubGroup(group);
      groupToSuper.setSuperGroup(superGroup);
      Set superGroups = new HashSet();
      superGroups.add(groupToSuper);
      Set subGroups = new HashSet();
      subGroups.add(groupToSuper);
      group.setSuperGroupAssignments(superGroups);
      superGroup.setSubGroupAssignments(subGroups);

      //set up the request
      request.setArgument(OpUserService.GROUP_ID, GROUP_LOCATOR);

      String groupName = "groupName";
      Map groupData = creatGroupData(groupName, "group Description", new ArrayList());
      request.setArgument(OpUserService.GROUP_DATA, groupData);

      //no groups to be found by broker query
      queryResults.clear();

      //broker must  be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(GROUP_LOCATOR)).will(methodStub);

      //the group name is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(groupName)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //update group
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createGroupConstraint(groupData));

      //find all remaining groups  (that are not in the request) in order to delete them
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).
           with(eq(SELECT_GROUP_ASSIGNMENTS_BY_ID)).
           will(new Stub() {
              public Object invoke(Invocation invocation)
                   throws Throwable {
                 queryResults.add(groupToSuper);
                 return query;
              }

              public StringBuffer describeTo(StringBuffer stringBuffer) {
                 return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_USER_ASSIGNMENTS_BY_ID);
              }
           });
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(SUB_GROUP_ID_FOR_QUERY), eq(group.getID())});
      Set expectedGroups = new HashSet();
      expectedGroups.add(new Long(superGroup.getID()));
      mockQuery.expects(once()).method(SET_COLLECTION_METHOD).with(new Constraint[]{eq(SUPER_GROUP_ID_FOR_QUERY), eq(expectedGroups)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //delete the removed assignments
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(same(groupToSuper));

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateGroup((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Tests the behavior of updateGroup for a group with a non existent super group.
    */
   public void testUpdateGroupNonExistingSuper() {
      XMessage request = new XMessage();
      //set up the request
      request.setArgument(OpUserService.GROUP_ID, GROUP_LOCATOR);

      String groupName = "groupName";
      ArrayList superGroups = new ArrayList();
      long nonExistentGroupId = 76543;
      String superGroupLocator = "OpGroup." + nonExistentGroupId + ".xid";
      superGroups.add(superGroupLocator);
      Map groupData = creatGroupData(groupName, "group Description", superGroups);
      request.setArgument(OpUserService.GROUP_DATA, groupData);

      //no groups to be found by broker query
      queryResults.clear();

      //broker must  be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(GROUP_LOCATOR)).will(methodStub);

      //the group name is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(groupName)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //update group
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(createGroupConstraint(groupData));

      //get super group
      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).
           with(new Constraint[]{eq(OpGroup.class), eq(nonExistentGroupId)}).
           will(methodStub);

      //no group with the given id will be found...

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateGroup((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned. Super group doesn't exist", result.getError());
   }

   /**
    * Tests the behavior of updateGroup for a non existent group.
    */
   public void testUpdateNonExistentGroup() {
      XMessage request = new XMessage();
      //set up the request
      String groupId = "nonexistent group id";
      request.setArgument(OpUserService.GROUP_ID, groupId);

      String groupName = "groupName";
      Map groupData = creatGroupData(groupName, "group Description", null);
      request.setArgument(OpUserService.GROUP_DATA, groupData);

      //no groups to be found by broker
      queryResults.clear();

      //broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //find the group to be edited
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(groupId)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateGroup((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
   }

   /**
    * Tests the behavior of updateGroup for a invalid group data.
    */
   public void testUpdateGroupWrongData() {
      XMessage request = new XMessage();
      //set up the request
      request.setArgument(OpUserService.GROUP_ID, GROUP_LOCATOR);

      String groupName = "";
      Map groupData = creatGroupData(groupName, "group Description", null);
      request.setArgument(OpUserService.GROUP_DATA, groupData);

      //no groups to be found by broker
      queryResults.clear();

      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(GROUP_LOCATOR)).will(methodStub);

      //the group name is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(groupName)});

      //iterate query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateGroup((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
   }

   /**
    * Tests the behavior of updateGroup for an already existing group.
    */
   public void testUpdateGroupWithExistentData() {

      XMessage request = new XMessage();
      //set up the request
      request.setArgument(OpUserService.GROUP_ID, GROUP_LOCATOR);
      String groupName = GROUP_NAME;
      Map groupData = creatGroupData(groupName, "groupNewDescription", null);
      request.setArgument(OpUserService.GROUP_DATA, groupData);

      //the persisted group with the same name but different id
      OpSubject existentGroup = new OpSubject();
      existentGroup.setName(GROUP_NAME);
      existentGroup.setID(2);

      //clear the Group list
      queryResults.clear();
      //one existentGroup should be found using find() method on the broker
      queryResults.add(new Long(existentGroup.getID()));

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(GROUP_LOCATOR)).will(methodStub);

      //the group name is searched for (if it's already taken, error is returned)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_SUBJECT_ID_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(groupName)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(eq(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.updateGroup((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
   }

   /**
    * Creates a group data given the properties of the new group
    *
    * @param groupName   name for the group
    * @param description description of the group
    * @param groups      <code>XArray</code> with groups this group belongs to
    * @return a new <code>Map</code> with the group data
    */
   private Map creatGroupData(String groupName, String description, ArrayList groups) {
      Map groupValues = new HashMap();
      groupValues.put(OpSubject.NAME, groupName);
      groupValues.put(OpSubject.DESCRIPTION, description);
      groupValues.put(OpUserService.ASSIGNED_GROUPS, groups);
      return groupValues;

   }

   /**
    * Creates a user data given the properties of the new user.
    *
    * @param firstName       <code>String</code> first name of the user
    * @param lastName        <code>String</code> last name of the user
    * @param loginName       <code>String</code> login name of the user
    * @param password        <code>String</code> password of the user
    * @param retypedPassword <code>String</code> retyped password of the user
    * @param email           <code>String</code> email of the user
    * @param phone           <code>String</code> phone numberof the user
    * @param mobile          <code>String</code> mobile phone number of the user
    * @param fax             <code>String</code> fax of the user
    * @return a new <code>Map</code> with the user data
    */
   private Map createUserData(String firstName, String lastName, String loginName, String password, String retypedPassword,
        String email, String phone, String mobile, String fax,List assignedGroups) {
      Map userValues = new HashMap();

      userValues.put(OpContact.FIRST_NAME, firstName);
      userValues.put(OpContact.LAST_NAME, lastName);
      userValues.put(OpUser.NAME, loginName);
      userValues.put(OpUser.PASSWORD, (password == null) ? null : sha.calculateHash(password));
      userValues.put(OpUserService.PASSWORD_RETYPED, (retypedPassword == null) ? null : sha.calculateHash(retypedPassword));
      userValues.put(OpContact.EMAIL, email);
      userValues.put(OpContact.PHONE, phone);
      userValues.put(OpContact.MOBILE, mobile);
      userValues.put(OpContact.FAX, fax);
      userValues.put(OpUserService.ASSIGNED_GROUPS, assignedGroups);
      userValues.put(OpUserService.USER_LEVEL, String.valueOf(OpUser.MANAGER_USER_LEVEL));
      userValues.put("Language", "en");
      return userValues;
   }

   /**
    * Creates a new constraint that can be used to check if a group is the expected one.
    *
    * @return a new group constraint
    */
   private Constraint createGroupConstraint(final Map groupData) {
      return new Constraint() {
         public boolean eval(Object object) {
            if (!(object instanceof OpSubject)) {
               return false;
            }
            OpSubject group = (OpSubject) object;
            if (!groupData.get(OpSubject.NAME).equals(group.getName())) {
               return false;
            }
            return groupData.get(OpSubject.DESCRIPTION).equals(group.getDescription());
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if a group is the expected one");
         }
      };
   }

   /**
    * Test assign user to group
    */
   public void testAssignUserToGroup() {

      XMessage request = new XMessage();
      //set up the request

      ArrayList users = new ArrayList();
      users.add(USER_LOCATOR);
      request.setArgument(OpUserService.SUBJECT_IDS, users);
      request.setArgument(OpUserService.TARGET_GROUP_ID, SUPER_GROUP_LOCATOR);

      //prepare result for iterate - no results since the assignement doesn't allready exist
      queryResults.clear();

      //create broker
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get target
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(SUPER_GROUP_LOCATOR)).will(methodStub);

      //expect 2 query creation (2 assigns)
      mockBroker.expects(atLeastOnce()).
           method(NEW_QUERY_METHOD).
           with(or(eq(SELECT_GROUP_ASSIGNMENT_ID), eq(SELECT_USER_ASSIGNMENT_ID))).
           will(methodStub);

      //set values on query and check if assignment doesn't already exist
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(USER_ID_LONG)});
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(1), eq(SUPER_GROUP_ID_LONG)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //create transaction
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //get the user
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).
           with(new Constraint[]{eq(OpUser.class), eq(USER_ID_LONG)}).will(methodStub);

      //broker.makePersistent(userAssignment);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).
           with(createUserAssignmentConstraint(USER_LOGIN_NAME, SUPER_GROUP_NAME));

      //commit try and close broker
      mockTransaction.expects(once()).method(COMMIT_METHOD);
      mockBroker.expects(once()).method(CLOSE_METHOD);


      XMessage result = userService.assignToGroup((XSession) mockSession.proxy(), request);
      assertNoError(result);

   }

   /**
    * Test assign user to an alreadz assigned group
    */
   public void testAssignUserToAlreadyAssignedGroup() {

      XMessage request = new XMessage();
      //set up the request

      ArrayList users = new ArrayList();
      users.add(USER_LOCATOR);
      request.setArgument(OpUserService.SUBJECT_IDS, users);
      request.setArgument(OpUserService.TARGET_GROUP_ID, SUPER_GROUP_LOCATOR);

      //prepare result for iterate - results found since the assignement does allready exist
      queryResults.add(ASSIGNMENT_LOCATOR);

      //create broker
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get target
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(SUPER_GROUP_LOCATOR)).will(methodStub);

      //expect 2 query creation (2 assigns)
      mockBroker.expects(atLeastOnce()).method(NEW_QUERY_METHOD).with(or(eq(SELECT_GROUP_ASSIGNMENT_ID), eq(SELECT_USER_ASSIGNMENT_ID))).
           will(methodStub);

      //set values on query and check if assignment doesn't already exist
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(USER_ID_LONG)});
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(1), eq(SUPER_GROUP_ID_LONG)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //create transaction
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //get the user
      mockBroker.expects(never()).method(GET_OBJECT_METHOD).with(new Constraint[]{eq(OpUser.class), eq(USER_ID_LONG)}).will(methodStub);

      //broker.makePersistent(userAssignment);
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD).with(createUserAssignmentConstraint(USER_LOGIN_NAME, SUPER_GROUP_NAME));

      //do commit and try and close broker
      mockTransaction.expects(once()).method(COMMIT_METHOD);
      mockBroker.expects(once()).method(CLOSE_METHOD);

      // becouse the user is already assigned to target group NOP and no error message is returned
      assertNoError(userService.assignToGroup((XSession) mockSession.proxy(), request));

   }

   /**
    * Test assign user to an non existent group
    */
   public void testAssignUserToNonExistentGroup() {

      XMessage request = new XMessage();
      //set up the request

      ArrayList users = new ArrayList();
      users.add(USER_LOCATOR);
      request.setArgument(OpUserService.SUBJECT_IDS, users);
      request.setArgument(OpUserService.TARGET_GROUP_ID, NON_EXISTENT_GROUP_LOCATOR);

      //prepare result for iterate - results found since the assignement does allready exist
      queryResults.add(ASSIGNMENT_LOCATOR);

      //create broker
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get target group
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(NON_EXISTENT_GROUP_LOCATOR)).will(methodStub);

      //don't create transaction
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //expect 2 query creation (2 assigns)
      mockBroker.expects(never()).method(NEW_QUERY_METHOD);

      //set values on query and check if assignment doesn't already exist
      mockQuery.expects(never()).method(SET_LONG_METHOD);

      //don't makePersistent(userAssignment);
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //do not commit and try and close broker
      mockTransaction.expects(never()).method(COMMIT_METHOD);
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.assignToGroup((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned becouse the target group does not exist", result);
      assertNotNull("Error message should have been returned becouse the target group does not exist", result.getError());

   }

   /**
    * Test assign non existent user to target group
    */
   public void testAssignNonExitentUserToGroup() {

      XMessage request = new XMessage();
      //set up the request

      ArrayList users = new ArrayList();
      users.add(NON_EXISTENT_USER_LOCATOR);
      request.setArgument(OpUserService.SUBJECT_IDS, users);
      request.setArgument(OpUserService.TARGET_GROUP_ID, GROUP_LOCATOR);

      //prepare result for iterate - no results since the assignement doesn't allready exist
      queryResults.clear();

      //create broker
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get target
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(GROUP_LOCATOR)).will(methodStub);

      //expect 2 query creation (2 assigns)
      mockBroker.expects(atLeastOnce()).method(NEW_QUERY_METHOD).with(or(eq(SELECT_GROUP_ASSIGNMENT_ID), eq(SELECT_USER_ASSIGNMENT_ID))).
           will(methodStub);

      //set values on query and check if assignment doesn't already exist
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(NON_EXISTENT_USER_ID_LONG)});
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(1), eq(GROUP_ID_LONG)});

      //iterate query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //create transaction
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //get the user
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(new Constraint[]{eq(OpUser.class), eq(NON_EXISTENT_USER_ID_LONG)}).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //don't broker.makePersistent(userAssignment) for unexistent user;
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      mockTransaction.expects(once()).method(COMMIT_METHOD);
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = userService.assignToGroup((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result);
      assertNotNull("Error message should have been returned", result.getError());
   }

   /**
    * Test assign group to group
    */
   public void testAssignGroupToGroup() {

      XMessage request = new XMessage();
      //set up the request

      ArrayList groups = new ArrayList();
      groups.add(GROUP_LOCATOR);
      request.setArgument(OpUserService.SUBJECT_IDS, groups);
      request.setArgument(OpUserService.TARGET_GROUP_ID, SUPER_GROUP_LOCATOR);

      //prepare result for iterate - no results since the assignement doesn't allready exist
      queryResults.clear();

      //create broker
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get target super group
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(SUPER_GROUP_LOCATOR)).will(methodStub);

      //expect 2 query creation (2 assigns)
      mockBroker.expects(atLeastOnce()).method(NEW_QUERY_METHOD).with(or(eq(SELECT_GROUP_ASSIGNMENT_ID), eq(SELECT_USER_ASSIGNMENT_ID))).
           will(methodStub);

      //set values on query and check if assignment doesn't already exist
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(GROUP_ID_LONG)});
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(1), eq(SUPER_GROUP_ID_LONG)});
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //create transaction
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //get the group
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(new Constraint[]{eq(OpGroup.class), eq(GROUP_ID_LONG)}).will(methodStub);
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(new Constraint[]{eq(OpGroup.class), eq(SUPER_GROUP_ID_LONG)}).will(methodStub);
      //broker.makePersistent(userAssignment);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createSuperGroupConstraint(GROUP_NAME, SUPER_GROUP_NAME));

      //commit try and close broker
      mockTransaction.expects(once()).method(COMMIT_METHOD);
      mockBroker.expects(once()).method(CLOSE_METHOD);


      XMessage result = userService.assignToGroup((XSession) mockSession.proxy(), request);
      assertNoError(result);

   }

   /**
    * Test assign group to already assigned super group
    */
   public void testAssignGroupToAlreadyAssignedGroup() {

      XMessage request = new XMessage();
      //set up the request

      ArrayList groups = new ArrayList();
      groups.add(GROUP_LOCATOR);
      request.setArgument(OpUserService.SUBJECT_IDS, groups);
      request.setArgument(OpUserService.TARGET_GROUP_ID, SUPER_GROUP_LOCATOR);

      //prepare result for iterate - results since the assignment doe allready exist
      queryResults.add(ASSIGNMENT_LOCATOR);

      //create broker
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get target super group
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(SUPER_GROUP_LOCATOR)).will(methodStub);

      //expect 2 query creation (2 assigns)
      mockBroker.expects(atLeastOnce()).method(NEW_QUERY_METHOD).with(or(eq(SELECT_GROUP_ASSIGNMENT_ID), eq(SELECT_USER_ASSIGNMENT_ID))).
           will(methodStub);

      //set values on query and check if assignment doesn't already exist
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(GROUP_ID_LONG)});
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(1), eq(SUPER_GROUP_ID_LONG)});

      //iterate query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //create transaction
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //get the group
      mockBroker.expects(never()).method(GET_OBJECT_METHOD).with(new Constraint[]{eq(OpGroup.class), eq(GROUP_ID_LONG)}).will(methodStub);

      //don't broker.makePersistent(userAssignment);
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD).with(createSuperGroupConstraint(GROUP_NAME, SUPER_GROUP_NAME));

      //do commit and try and close broker
      mockTransaction.expects(once()).method(COMMIT_METHOD);
      mockBroker.expects(once()).method(CLOSE_METHOD);

      // becouse the group is already assigned to target super group NOP and no error message is returned
      XMessage result = userService.assignToGroup((XSession) mockSession.proxy(), request);
      assertNoError(result);

   }

   /**
    * Test assign group to non existent super group
    */
   public void testAssignGroupToNonExistentSuperGroup() {

      XMessage request = new XMessage();
      //set up the request

      ArrayList groups = new ArrayList();
      groups.add(GROUP_LOCATOR);
      request.setArgument(OpUserService.SUBJECT_IDS, groups);
      request.setArgument(OpUserService.TARGET_GROUP_ID, NON_EXISTENT_SUPER_GROUP_LOCATOR);

      //prepare result for iterate
      queryResults.clear();

      //create broker
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get target super group
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(NON_EXISTENT_SUPER_GROUP_LOCATOR)).will(methodStub);

      //don't create transaction
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //do not expect 2 query creation (2 assigns)
      mockBroker.expects(never()).method(NEW_QUERY_METHOD);

      //don't set values on query and check if assignment doesn't already exist
      mockQuery.expects(never()).method(SET_LONG_METHOD);

      //don't makePersistent(groupAssignment);
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //do not commit and try and close broker
      mockTransaction.expects(never()).method(COMMIT_METHOD);
      mockBroker.expects(once()).method(CLOSE_METHOD);


      XMessage result = userService.assignToGroup((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned becouse the target super group does not exist", result);
      assertNotNull("Error message should have been returned becouse the target super group does not exist ", result.getError());

   }

   /**
    * Test assign group/user wrong data to group/superr group wrong data
    */
   public void testAssignWongData() {

      XMessage request = new XMessage();
      //set up the request

      request.setArgument(OpUserService.SUBJECT_IDS, null);
      request.setArgument(OpUserService.TARGET_GROUP_ID, null);

      //prepare result for iterate
      queryResults.clear();
      //error must be generated
      mockSession.expects(never()).method(NEW_ERROR_METHOD).will(methodStub);

      //create broker
      mockSession.expects(never()).method(NEW_BROKER_METHOD).will(methodStub);

      //dont' get target group/super group
      mockBroker.expects(never()).method(GET_OBJECT_METHOD);

      //don't create transaction
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //don't set values on query and check if assignment doesn't already exist
      mockQuery.expects(never()).method(SET_LONG_METHOD);

      //don't makePersistent(userAssignment);
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //do not commit and try and close broker
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      mockBroker.expects(never()).method(CLOSE_METHOD);


      assertNoError(userService.assignToGroup((XSession) mockSession.proxy(), request));

   }

   /**
    * Test the behaviour of createAdministrator
    */
   public void testCreateAdministrator() {
      //create the administrator
      OpUser administrator = new OpUser();
      administrator.setName(OpUser.ADMINISTRATOR_NAME);
      administrator.setDisplayName(OpUser.ADMINISTRATOR_DISPLAY_NAME);
      administrator.setDescription(OpUser.ADMINISTRATOR_DESCRIPTION);
      administrator.setPassword(sha.calculateHash(""));

      //create transaction
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);
      //broker.makePersistent(userAssignment);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createAdministratorConstraint(administrator));
      //broker.makePersistent(userContact);
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(isA(OpContact.class));
      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      assertUserEqual(OpUserService.createAdministrator((OpBroker) mockBroker.proxy()), administrator);

   }

   /**
    * Test the behaviour of createEveryone
    */
   public void testCreateEveryone() {
      //create the group
      OpGroup group = new OpGroup();
      group.setName(OpGroup.EVERYONE_NAME);
      group.setDisplayName(OpGroup.EVERYONE_DISPLAY_NAME);
      group.setDescription(OpGroup.EVERYONE_DESCRIPTION);

      //create transaction
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);
      //broker.makePersistent
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(createGroupConstraint(group));
      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      assertGroupEqual(OpUserService.createEveryone((OpBroker) mockBroker.proxy()), group);
   }

   /**
    * Compares 2 users to see if they are equal, based on: Name,DisplayName,Description and Password.
    *
    * @param user         a  <code>OpUser</code> representing the user being checked.
    * @param expectedUser a <code>OpUser</code> representing the reference user
    */
   protected void assertUserEqual(OpUser user, OpUser expectedUser) {
      assertEquals("The 2 users don't have the same Name", user.getName(), expectedUser.getName());
      assertEquals("The 2 users don't have the same DisplayName", user.getDisplayName(), expectedUser.getDisplayName());
      assertEquals("The 2 users don't have the same Description", user.getDescription(), expectedUser.getDescription());
      assertEquals("The 2 users don't have the same Password", user.getPassword(), expectedUser.getPassword());
   }

   /**
    * Compares 2 groups to see if they are equal, based on: Name,DisplayName and Description.
    *
    * @param group         a <code>OpGroup</code> representing the group being checked.
    * @param expectedGroup a <code>OpGroup</code> representing the reference group
    */
   protected void assertGroupEqual(OpGroup group, OpGroup expectedGroup) {
      assertEquals("The 2 groups don't have the same Name", group.getName(), expectedGroup.getName());
      assertEquals("The 2 groups don't have the same DisplayName", group.getDisplayName(), expectedGroup.getDisplayName());
      assertEquals("The 2 groups don't have the same Description", group.getDescription(), expectedGroup.getDescription());
   }

   /**
    * Creates a new constraint that can be used to check if an administrator user is the expected one.
    *
    * @return a new user constraint
    */
   private Constraint createAdministratorConstraint(final OpUser administrator) {
      return new Constraint() {
         public boolean eval(Object obj) {
            if (! (obj instanceof OpUser)) {
               return false;
            }
            OpUser admin = (OpUser) obj;
            if (!(admin.getName().equals(administrator.getName()))) {
               return false;
            }
            if (!(admin.getDisplayName().equals(administrator.getDisplayName()))) {
               return false;
            }
            if (!(admin.getDescription().equals(administrator.getDescription()))) {
               return false;
            }
            //administrator password
            String password = new OpSHA1().calculateHash("");
            return (password.equals(administrator.getPassword()));
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if an administrator is the expected user");
         }
      };
   }

   /**
    * Creates a new constraint that can be used to check if a group is the expected one.
    *
    * @return a new group constraint
    */
   private Constraint createGroupConstraint(final OpGroup group) {
      return new Constraint() {
         public boolean eval(Object obj) {
            if (! (obj instanceof OpGroup)) {
               return false;
            }
            OpGroup g = (OpGroup) obj;

            if (!(g.getName().equals(group.getName()))) {
               return false;
            }
            if (!(g.getDisplayName().equals(group.getDisplayName()))) {
               return false;
            }
            return (g.getDescription().equals(group.getDescription()));
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if an group is the expected one");
         }
      };
   }

   /**
    * Must be called in order to configure user calendar settings
    */
   private void applySettingsExpectation() {
      String scheduleQuery = "select schedule from OpSchedule as schedule where schedule.Name = ?";
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //new query
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(scheduleQuery)).will(methodStub);
      //set schedule name
      mockQuery.expects(once()).method(SET_STRING_METHOD);
      //list schedules
      mockBroker.expects(once()).method(LIST_METHOD).with(eq(query)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.clear();
            return queryResults;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of listing the schedules");
         }
      });
      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      OpSettings.applySettings((OpProjectSession) mockSession.proxy());
   }
}
