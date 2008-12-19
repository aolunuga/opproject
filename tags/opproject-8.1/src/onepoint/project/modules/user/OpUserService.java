/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.XView;
import onepoint.express.util.XConstants;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.util.OpCollectionCopyHelper;
import onepoint.project.util.OpHashProvider;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceException;
import onepoint.service.server.XServiceManager;

public class OpUserService extends OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpUserService.class);

   public final static String LOGIN = "login";
   public final static String PASSWORD = "password";
   public final static String PASSWORD_RETYPED = "PasswordRetyped";
   public final static String USER_LEVEL = "userLevel";
   public final static String USER_DATA = "user_data";
   public final static String LANGUAGE = "Language";
   public final static String USER_ID = "user_id";
   public final static String GROUP_DATA = "group_data";
   public final static String GROUP_ID = "group_id";
   public final static String SUBJECT_ID = "subject_id";
   public final static String SUBJECT_NAME = "subject_name";
   public final static String SUBJECT_IDS = "subject_ids";
   public final static String SUPER_SUBJECT_IDS = "super_ids";
   public final static String SUB_SUBJECT_IDS = "sub_ids";
   public final static String TARGET_GROUP_ID = "target_group_id";

   private final static String SOURCE_GROUP_LOCATOR = "source_group_locator";
   private final static String OUTLINE_LEVEL = "outlineLevel";
   private final static String ENABLE_USERS = "EnableUsers";
   private final static String ENABLE_GROUPS = "EnableGroups";
   private final static String FILTERED_SUBJECT_IDS = "FilteredSubjectIds";
   private final static String INCLUDE_PARENTS_IN_FILTER = "IncludeParentsInFilter";

   /**
    * the map containing all error types.
    */
   public static final OpUserErrorMap ERROR_MAP = new OpUserErrorMap();

   // User data
   public final static String ASSIGNED_GROUPS = "assigned_groups";

   public final static String PASSWORD_TOKEN = "@*1XW9F4";
   private final static String NULL_PASSWORD = null;

   private final static String WARNING = "warning";

   private final static String USERS_QUERY = "select count(user) from OpUser as user where user.Level=? and user.Name != '" + OpUser.ADMINISTRATOR_NAME + "'";

   private OpUserServiceImpl serviceIfcImpl = null;

   /**
    * Creates a new server instance.
    */
   public OpUserService() {
      serviceIfcImpl = createServiceImpl();
   }

   /**
    * Creates a new service implementation instance.
    * @return a <code>OpUserServiceImpl</code> instance.
    */
   protected OpUserServiceImpl createServiceImpl() {
      return new OpUserServiceImpl();
   }

   public XMessage getHashAlgorithm(OpProjectSession session, XMessage request) {
      logger.debug("OpUserService.getHashAlgorithm()");

      String login = (String) (request.getArgument(LOGIN));
      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();
      try {
         String algo = serviceIfcImpl.getHashAlgorithm(session, broker, login);
         reply.setVariable("algorithm", algo);
      }
      finally {
         broker.close();
      }
      return reply;
   }

   public XMessage signOn(OpProjectSession session, XMessage request) {
      logger.debug("OpUserService.signOn()");

      String login = (String) (request.getArgument(LOGIN));
      String password = (String) (request.getArgument(PASSWORD));

      XMessage reply = new XMessage();
      if (session.getServer().isSiteValid(session.getSourceName())) {
         OpBroker broker = session.newBroker();
         try {
            serviceIfcImpl.signOn(session,  login, password);

            //send the calendar to the client
            reply.setVariable(OpProjectConstants.CALENDAR, session.getCalendar());

            //send client-side error messages
            reply.setVariable(XConstants.ERROR_FILE_NOT_FOUND, session.newError(ERROR_MAP, OpUserError.FILE_NOT_FOUND));
            reply.setVariable(XConstants.ERROR_OUT_OF_MEMORY, session.newError(ERROR_MAP, OpUserError.OUT_OF_MEMORY));
         }
         catch (XServiceException exc) {
            exc.append(reply);
         }

         broker.close();
      }
      else {
         reply.setError(session.newError(ERROR_MAP, OpUserError.SITE_IS_INVALID));
      }

      return reply;
   }

   public XMessage insertUser(OpProjectSession session, XMessage request) {
      logger.debug("OpUserService.insertUser()");
      XMessage reply = new XMessage();
      HashMap user_data = (HashMap) (request.getArgument(USER_DATA));

      OpUser user = new OpUser();
      // FIXME(dfreis Mar 2, 2007 9:45:37 AM)
      // check if this is ok, hibernate!!!
      // use user.createContact() -> OpContact
      OpContact contact = user.createContact(); // must not be null!

      contact.setFirstName((String) (user_data.get(OpContact.FIRST_NAME)));
      contact.setLastName((String) (user_data.get(OpContact.LAST_NAME)));
      contact.setEMail((String) (user_data.get(OpContact.EMAIL)));
      contact.setPhone((String) (user_data.get(OpContact.PHONE)));
      contact.setMobile((String) (user_data.get(OpContact.MOBILE)));
      contact.setFax((String) (user_data.get(OpContact.FAX)));

      user.setName((String) (user_data.get(OpUser.NAME)));
      user.setPassword((String) (user_data.get(OpUser.PASSWORD)));
      user.setDescription((String) (user_data.get(OpUser.DESCRIPTION)));

      // Create display name (note: This could be made configurable in the future)
      String displayName = contact.calculateDisplayName(user.getName());
      user.setDisplayName(displayName);

      //check for password mismatch
      String retypedPassword = (String) user_data.get(PASSWORD_RETYPED);
      if (!user.validatePassword(retypedPassword)) {
         reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.PASSWORD_MISMATCH));
         return reply;
      }
      //get user level
      String userLevel = (String) user_data.get(USER_LEVEL);
      Byte userLevelId;
      try {
         userLevelId = Byte.parseByte(userLevel);
      }
      catch (NumberFormatException e) {
         reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.INVALID_USER_LEVEL));
         return reply;
      }
      user.doSetLevel(userLevelId);

      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      try {
         reply = aditionalCheck(session, broker, user);
         if (reply.getError() != null) {
            broker.close();
            return reply;
         }

         // NOTE: do not remove local broker reference!! (ThreadLocal)
         t = broker.newTransaction();

         serviceIfcImpl.insertUser(session, broker, user);

         // set language preference
         String xLocaleId = (String) user_data.get(LANGUAGE);
         OpUserLanguageManager.updateUserLanguagePreference(broker, user, xLocaleId);

         // set assignments
         List assigned_groups = (List) (user_data.get(ASSIGNED_GROUPS));
         if ((assigned_groups != null) && (assigned_groups.size() > 0)) {
            String choice;
            OpGroup group;
            for (Object assigned_group : assigned_groups) {
               choice = (String) assigned_group;
               group = (OpGroup) (broker.getObject(XValidator.choiceID(choice)));
               serviceIfcImpl.assign(session, broker, user, group);
            }
         }

         // FIXME(dfreis Feb 28, 2007 3:11:28 PM)
         // do this using OpPreferencesAPI

         //create a preference regarding the show hours option, using the default value from the system settings
         String showHours = OpSettingsService.getService().getStringValue(session, OpSettings.SHOW_RESOURCES_IN_HOURS);
         OpPreference pref = new OpPreference();
         pref.setUser(user);
         pref.setName(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
         pref.setValue(showHours);
         broker.makePersistent(pref);
         t.commit();
         logger.debug("make-persistent");
      }
      catch (XServiceException exc) {
         if (t != null) {
            t.rollback();
         }
         reply.setError(exc.getError());
      }
      finally {
         broker.close();
      }
      return reply;
   }

   /**
    * Template method that allows aditional user check for overriding class.
    *
    * @param session
    * @param broker
    * @param user    User that needs to be checked
    * @return error reply if the user failed the check
    */
   protected XMessage aditionalCheck(OpProjectSession session, OpBroker broker, OpUser user) {
      return new XMessage();
   }

   public XMessage insertGroup(OpProjectSession session, XMessage request) {
      logger.debug("OpUserService.insertGroup()");
      HashMap group_data = (HashMap) (request.getArgument(GROUP_DATA));
      XMessage reply = new XMessage();
      OpGroup group = new OpGroup();
      group.setName((String) (group_data.get(OpSubject.NAME)));
      group.setDisplayName(group.getName());
      group.setDescription((String) (group_data.get(OpSubject.DESCRIPTION)));

      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      try {
         List assigned_group_id_strings = (List) group_data.get(ASSIGNED_GROUPS);
         Vector<OpGroup> super_groups = new Vector<OpGroup>();
         if (assigned_group_id_strings != null) {
            String choice;
            Iterator iter = assigned_group_id_strings.iterator();
            OpGroup assigned_group;
            while (iter.hasNext()) {
               choice = (String) iter.next();
               assigned_group = (OpGroup) (broker.getObject(XValidator.choiceID(choice)));
               if (assigned_group == null) {
                  // super group not found
                  reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
                  return reply;
               }
               super_groups.add(assigned_group);
            }
         }
         // super_groups now contains all super groups!
         if (!serviceIfcImpl.isAssignable(session, broker, group, super_groups.iterator())) {
            reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.LOOP_ASSIGNMENT));
            return reply;
         }

         // validation successfully completed
         t = broker.newTransaction();
         serviceIfcImpl.insertGroup(session, broker, group);
         serviceIfcImpl.assign(session, broker, group, super_groups.iterator());
         t.commit();
      }
      catch (XServiceException exc) {
         if (t != null) {
            t.rollback();
         }
         reply.setError(exc.getError());
         return reply;
      }
      finally {
         broker.close();
      }
      return reply;
   }

   public XMessage updateUser(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(USER_ID));
      logger.debug("OpUserService.updateUser(): id = " + id_string);
      HashMap user_data = (HashMap) (request.getArgument(USER_DATA));
      XMessage reply = new XMessage();

      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      try {
         OpUser user = serviceIfcImpl.getUserByIdString(session, broker, id_string);
         if (user == null) {
            logger.warn("ERROR: Could not find object with ID " + id_string);
            reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.USER_NOT_FOUND));
            return reply;
         }

         Byte originalLevel = user.getLevel();
         user.setName((String) (user_data.get(OpUser.NAME)));

         OpContact contact = user.getContact();
         contact.setFirstName((String) (user_data.get(OpContact.FIRST_NAME)));
         contact.setLastName((String) (user_data.get(OpContact.LAST_NAME)));
         contact.setEMail((String) (user_data.get(OpContact.EMAIL)));
         contact.setPhone((String) (user_data.get(OpContact.PHONE)));
         contact.setMobile((String) (user_data.get(OpContact.MOBILE)));
         contact.setFax((String) (user_data.get(OpContact.FAX)));

         //set user level
         try {
            user.doSetLevel(Byte.parseByte((String) user_data.get(USER_LEVEL)));
         }
         catch (NumberFormatException e) {
            reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.INVALID_USER_LEVEL));
            return reply;
         }
         catch (IllegalArgumentException e) {
            reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.DEMOTE_USER_ERROR));
            return reply;
         }

         user.setDescription((String) (user_data.get(OpUser.DESCRIPTION)));

         //user password validation
         String password = (String) (user_data.get(OpUser.PASSWORD));
         String retypedPassword = (String) user_data.get(PASSWORD_RETYPED);
         String token = new OpHashProvider().calculateHash(PASSWORD_TOKEN);

         // note PASSWORD_TOKEN is the default value of the password field, all other fields will display the real values of the user!
         if (token.equals(password == null ? OpUser.BLANK_PASSWORD : password)) {
            //check actual user password match only if user enters something in retype password field
            if ((retypedPassword != NULL_PASSWORD) && user.validatePassword(retypedPassword)) {
               reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.PASSWORD_MISMATCH));
               return reply;
            }
         }
         else {
            //password changed on UI
            //check for password mismatch
            if ((password != retypedPassword) && (!password.equals(retypedPassword))) {
               reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.PASSWORD_MISMATCH));
               return reply;
            }

            //and finally.....
            user.setPassword(password);
         }

         // Create display name (note: This could be made configurable in the future)
         user.setDisplayName(contact.calculateDisplayName(user.getName()));

         if (originalLevel.byteValue() != user.getLevel().byteValue()) {
            reply = aditionalCheck(session, broker, user);
            if (reply.getError() != null) {
               return reply;
            }
         }

         // validation successfully completed
         t = broker.newTransaction();
         serviceIfcImpl.updateUser(session, broker, user);

         //set the language
         String xLocaleId = (String) user_data.get(LANGUAGE);
         boolean languageUpdated = OpUserLanguageManager.updateUserLanguagePreference(broker, user, xLocaleId);
         if (languageUpdated && user.getId() == session.getUserID()) {
            //refresh forms
            XLocale newLocale = XLocaleManager.findLocale(xLocaleId);
            session.setLocale(newLocale);
            reply.setArgument(OpProjectConstants.REFRESH_PARAM, Boolean.TRUE);
         }

         // Compare and update assignments
         List updatedGroupIds = (List) (user_data.get(ASSIGNED_GROUPS));
         Set<Long> storedGroupIds = new HashSet<Long>();
         for (OpUserAssignment assignment : user.getAssignments()) {
            storedGroupIds.add(new Long(assignment.getGroup().getId()));
         }

         if (updatedGroupIds != null) {
            Long groupId = null;
            OpGroup group;
            for (Object updatedGroupId : updatedGroupIds) {
               groupId = OpLocator.parseLocator((String) updatedGroupId).getID();
               if (!storedGroupIds.remove(groupId)) {
                  group = serviceIfcImpl.getGroupById(session, broker, groupId);
                  if (group == null) {
                     reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.USER_NOT_FOUND));
                     reply.setArgument(WARNING, Boolean.TRUE);
                  }
                  else {
                     // Assignment not yet persistent: Create new user assignment
                     serviceIfcImpl.assign(session, broker, user, group);
                  }
               }
            }
         }

         // remove the stored assignments that are not in the request
         if (storedGroupIds.size() > 0) {
            /*
            * --- Not yet supported in Hibernate (delete on joined sub-classes) OpQuery query = broker.newQuery("delete
            * OpUserAssignment where User.id = :userId and Group.id in (:groupIds)"); query.setLong("userId",
            * user.getID()); query.setCollection("groupIds", storedGroupIds);
            */
            OpQuery query = broker.newQuery(
                 "select assignment from OpUserAssignment as assignment where assignment.User.id = :userId and assignment.Group.id in (:groupIds)");
            query.setLong("userId", user.getId());
            query.setCollection("groupIds", storedGroupIds);
            Iterator result = broker.iterate(query);
            while (result.hasNext()) {
               OpUserAssignment assignment = (OpUserAssignment) result.next();
               serviceIfcImpl.deleteUserAssignment(session, broker, assignment);
            }
         }
         t.commit();
      }
      catch (XServiceException exc) {
         if (t != null) {
            t.rollback();
         }
         reply.setError(exc.getError());
      }
      finally {
         broker.close();
      }
      return reply;
   }

   /**
    * Gets all the owned permissions for the given group and recursivly for its supergoups.
    *
    * @param group group that is being queried.
    * @return Set with the collected permissions.
    */
   private Collection getAllOwnedPermissions(OpGroup group) {
      Set ownedPermissions = new HashSet(group.getOwnedPermissions());
      for (Iterator iterator = group.getSuperGroupAssignments().iterator(); iterator.hasNext();) {
         OpGroupAssignment assignment = (OpGroupAssignment) iterator.next();
         ownedPermissions.addAll(getAllOwnedPermissions(assignment.getSuperGroup()));
      }
      return ownedPermissions;
   }

   public XMessage updateGroup(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(GROUP_ID));
      logger.debug("OpUserService.updateGroup(): id = " + id_string);
      HashMap group_data = (HashMap) (request.getArgument(GROUP_DATA));

      XMessage reply = new XMessage();

      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      try {
         OpGroup group = serviceIfcImpl.getGroupByIdString(session, broker, id_string);
         if (group == null) {
            logger.warn("ERROR: Could not find object with ID " + id_string);
            reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.GROUP_NOT_FOUND));
            return reply;
         }

         group.setName((String) (group_data.get(OpSubject.NAME)));
         group.setDisplayName(group.getName());
         group.setDescription((String) (group_data.get(OpSubject.DESCRIPTION)));

         List assigned_groups = (List) (group_data.get(ASSIGNED_GROUPS));
         Vector<OpGroup> super_groups = new Vector<OpGroup>();
         if (assigned_groups != null) {
            String choice = null;
            Iterator iter = assigned_groups.iterator();
            OpGroup assigned_group;
            while (iter.hasNext()) {
               choice = (String) iter.next();
               assigned_group = serviceIfcImpl.getGroupByIdString(session, broker, XValidator.choiceID(choice));
               if (assigned_group == null) // super group not found
               {
                  reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
                  broker.close();
                  return reply;
               }
               super_groups.add(assigned_group);
            }
         }
         // super_groups now contains all super groups!
         if (!serviceIfcImpl.isAssignable(session, broker, group, super_groups.iterator())) {
            reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.LOOP_ASSIGNMENT));
            broker.close();
            return reply;
         }

         // validation successfully completed
         t = broker.newTransaction();
         serviceIfcImpl.updateGroup(session, broker, group);

         // Compare and update assignments
         Iterator storedSuperGroupAssignments = group.getSuperGroupAssignments().iterator();
         OpGroupAssignment assignment = null;
         Set<Long> storedSuperGroupIds = new HashSet<Long>();
         while (storedSuperGroupAssignments.hasNext()) {
            assignment = (OpGroupAssignment) storedSuperGroupAssignments.next();
            storedSuperGroupIds.add(new Long(assignment.getSuperGroup().getId()));
         }

         if (assigned_groups != null) {
            Long groupId = null;
            OpGroup superGroup = null;
            for (int i = 0; i < assigned_groups.size(); i++) {
               groupId = new Long(OpLocator.parseLocator((String) (assigned_groups.get(i))).getID());
               if (!storedSuperGroupIds.remove(groupId)) {
                  superGroup = serviceIfcImpl.getGroupById(session, broker, groupId.longValue());
                  // Assignment not yet persistent: Create new user assignment
                  if (superGroup == null) {
                     reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
                  }
                  else {
                     serviceIfcImpl.assign(session, broker, group, superGroup);
                  }
               }
            }
         }

         if (storedSuperGroupIds.size() > 0) {
            /*
            * --- Not yet supported by Hibernate (delete on joined sub-classes) OpQuery query = broker .newQuery("delete
            * OpGroupAssignment where SubGroup.id = :subGroupId and SuperGroup.id in (:superGroupIds)");
            * query.setLong("subGroupId", group.getID()); query.setCollection("superGroupIds", storedSuperGroupIds);
            * broker.execute(query);
            */

            OpQuery query = broker.newQuery(
                 "select assignment from OpGroupAssignment as assignment where assignment.SubGroup.id = :subGroupId and assignment.SuperGroup.id in (:superGroupIds)");
            query.setLong("subGroupId", group.getId());
            query.setCollection("superGroupIds", storedSuperGroupIds);
            Iterator result = broker.iterate(query);
            while (result.hasNext()) {
               assignment = (OpGroupAssignment) result.next();
               serviceIfcImpl.deleteGroupAssignment(session, broker, assignment);
            }
         }

         t.commit();
      }
      catch (XServiceException exc) {
         if (t != null) {
            t.rollback();
         }
         reply.setError(exc.getError());
      }
      finally {
         broker.close();
      }
      return reply;
   }

   /**
    * Removes the assignments (user to group, group to group) between the given subjects (users/groups).
    *
    * @param session the session
    * @param request map containing all the param required for the method
    * @return an error/success message
    */
   public XMessage deleteAssignments(OpProjectSession session, XMessage request) {
      List superLocators = (List) (request.getArgument(SUPER_SUBJECT_IDS));
      List subLocators = (List) (request.getArgument(SUB_SUBJECT_IDS));
      // assuming that both lists are of same size!

      Iterator super_iter = superLocators.iterator();
      Iterator sub_iter = subLocators.iterator();
      if (super_iter.hasNext()) {
         OpBroker broker = session.newBroker();
         OpTransaction t = broker.newTransaction();
         try {
            while (super_iter.hasNext() && sub_iter.hasNext()) {
               OpLocator superLocator = OpLocator.parseLocator((String) (super_iter.next()));
               OpLocator subLocator = OpLocator.parseLocator((String) (sub_iter.next()));
               OpGroup super_group = serviceIfcImpl.getGroupById(session, broker, superLocator.getID());
               if (subLocator.getPrototype().getInstanceClass() == OpUser.class) {
                  // user to group
                  OpUser user = serviceIfcImpl.getUserById(session, broker, subLocator.getID());
                  serviceIfcImpl.removeUserFromGroup(session, broker, user, super_group);
               }
               else {
                  // group to group
                  OpGroup group = serviceIfcImpl.getGroupById(session, broker, subLocator.getID());
                  serviceIfcImpl.removeGroupFromGroup(session, broker, group, super_group);
               }
            }
            t.commit();
         }
         catch (XServiceException exc) {
            t.rollback();
            XMessage reply = new XMessage();
            reply.setError(exc.getError());
            return (reply);
         }
         finally {
            broker.close();
         }
      }
      return null;
   }

   public XMessage deleteSubjects(OpProjectSession session, XMessage request) {

      List subjectLocators = (List) (request.getArgument(SUBJECT_IDS));
      logger.debug("OpUserService.deleteSubjects(): subject_ids = " + subjectLocators);

      if ((subjectLocators == null) || (subjectLocators.size() == 0)) {
         return null;
      }


      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      try {
         checkSubjects(session, broker, subjectLocators);
         
         List<Long> subjectIds = new ArrayList<Long>();
         for (int i = 0; i < subjectLocators.size(); i++) {
            subjectIds.add(new Long(OpLocator.parseLocator((String) (subjectLocators.get(i))).getID()));
         }
         OpQuery query = broker.newQuery("select subject from OpUser as subject where subject.id in (:subjectIds)");
         query.setCollection("subjectIds", subjectIds);
         Iterator result = broker.iterate(query);
         OpUser user;
         while (result.hasNext()) {
            user = (OpUser) result.next();
            serviceIfcImpl.deleteUser(session, broker, user);
         }

         query = broker.newQuery("select subject from OpGroup as subject where subject.id in (:subjectIds)");
         query.setCollection("subjectIds", subjectIds);
         result = broker.iterate(query);
         OpGroup group;
         while (result.hasNext()) {
            group = (OpGroup) result.next();
            serviceIfcImpl.deleteGroup(session, broker, group);
         }
         t.commit();
      }
      catch (XServiceException exc) {
         t.rollback();
         XMessage reply = new XMessage();
         reply.setError(exc.getError());
         return (reply);
      }
      finally {
         broker.close();
      }
      return null;
   }

   public XMessage assignToGroup(OpProjectSession session, XMessage request) {
      XMessage reply = new XMessage();

      logger.debug("OpUserService.assignToGroup()");

      List subjectLocators = (List) (request.getArgument(SUBJECT_IDS));
      if (subjectLocators == null) {
         // FIXME(dfreis Apr 5, 2007 3:13:03 PM)
         // should return exception here!
         reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
         return (reply);
      }

      String targetGroupLocator = (String) (request.getArgument(TARGET_GROUP_ID));
      if (targetGroupLocator == null) {
         reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
         return (reply);
      }

      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      try {
         // *** Retrieve target group
         OpGroup targetGroup = serviceIfcImpl.getGroupByIdString(session, broker, targetGroupLocator);
         if (targetGroup == null) {
            logger.warn("ERROR: Could not find object with ID " + targetGroupLocator);
            reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
            broker.close();
            return reply;
         }

         t = broker.newTransaction();

         OpQuery userAssignmentQuery = broker
              .newQuery("select assignment.id from OpUserAssignment as assignment where assignment.User.id = ? and assignment.Group.id = ?");
         OpQuery groupAssignmentQuery = broker
              .newQuery("select assignment.id from OpGroupAssignment as assignment where assignment.SubGroup.id = ? and assignment.SuperGroup.id = ?");

         OpLocator subjectLocator = null;
         Iterator result = null;
         OpUser user = null;
         OpGroup group = null;
         for (int i = 0; i < subjectLocators.size(); i++) {
            subjectLocator = OpLocator.parseLocator((String) (subjectLocators.get(i)));
            if (subjectLocator.getPrototype().getInstanceClass() == OpUser.class) {
               // Assign user to target group
               userAssignmentQuery.setLong(0, subjectLocator.getID());
               userAssignmentQuery.setLong(1, targetGroup.getId());
               result = broker.iterate(userAssignmentQuery);
               if (!result.hasNext()) {
                  user = (OpUser) broker.getObject(OpUser.class, subjectLocator.getID());
                  if (user == null) {
                     reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.USER_NOT_FOUND));
                     reply.setArgument(WARNING, Boolean.TRUE);
                  }
                  else {
                     serviceIfcImpl.assign(session, broker, user, targetGroup);
                  }

               }
            }
            else {
               // Assign group to target (super) group
               groupAssignmentQuery.setLong(0, subjectLocator.getID());
               groupAssignmentQuery.setLong(1, targetGroup.getId());
               result = broker.iterate(groupAssignmentQuery);
               if (!result.hasNext()) {
                  group = (OpGroup) broker.getObject(OpGroup.class, subjectLocator.getID());
                  if (group == null) {
                     reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.GROUP_NOT_FOUND));
                     reply.setArgument(WARNING, Boolean.TRUE);
                  }
                  else {
                     //loop check
                     if (!serviceIfcImpl.isAssignable(session, broker, group, targetGroup)) {
                        reply = new XMessage();
                        XError error = session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.LOOP_ASSIGNMENT);
                        reply.setError(error);
                        reply.setArgument(WARNING, Boolean.TRUE);
                     }
                     else {
                        serviceIfcImpl.assign(session, broker, group, targetGroup);
                     }
                  }
               }
            }
         }

         t.commit();
      }
      catch (XServiceException exc) {
         if (t != null) {
            t.rollback();
         }
         reply.setError(exc.getError());
         return (reply);
      }
      finally {
         broker.close();
      }
      return reply;
   }

   /**
    * Loads the children of the given group based on the filter and enable rules given as parameters.
    * Used for lazy loading, simple structure & filtering.
    *
    * @param session session to use
    * @param request request data
    * @return processing results.
    */
   public XMessage expandFilteredGroup(OpProjectSession session, XMessage request) {

      XMessage reply = new XMessage();

      //filter for groups/users
      Boolean includeParentsInFilter = (Boolean) request.getArgument(INCLUDE_PARENTS_IN_FILTER);
      List filteredSubjectIds = (List) request.getArgument(FILTERED_SUBJECT_IDS);
      if (includeParentsInFilter != null && includeParentsInFilter.booleanValue()) {
         filteredSubjectIds = OpSubjectDataSetFactory.getAlreadyAssignedGroups(session, filteredSubjectIds);
      }
//      replace with api call!
      XComponent resultSet = expandGroupStructure(session, request, true, filteredSubjectIds);

      if (resultSet != null) {

         boolean enableUsers = ((Boolean) request.getArgument(ENABLE_USERS)).booleanValue();
         boolean enableGroups = ((Boolean) request.getArgument(ENABLE_GROUPS)).booleanValue();
         OpSubjectDataSetFactory.enableSubjectHierarchy(resultSet, enableUsers, enableGroups);

         List<XView> resultList = new ArrayList<XView>();
         for (int i = 0; i < resultSet.getChildCount(); i++) {
            resultList.add(resultSet.getChild(i));
         }
         reply.setArgument(OpProjectConstants.CHILDREN, resultList);
      }

      return reply;

   }

   /**
    * Loads the children of the given group (used for lazy loading/ complex structure)
    *
    * @param session session to use
    * @param request request data
    * @return processing results.
    */
   public XMessage expandGroup(OpProjectSession session, XMessage request) {
      XMessage reply = new XMessage();
      // replace with api call
      XComponent resultSet = expandGroupStructure(session, request, false, null);
      if (resultSet != null) {
         List<XView> resultList = new ArrayList<XView>();
         for (int i = 0; i < resultSet.getChildCount(); i++) {
            resultList.add(resultSet.getChild(i));
         }
         reply.setArgument(OpProjectConstants.CHILDREN, resultList);
      }
      return reply;
   }

   private XComponent expandGroupStructure(OpProjectSession session, XMessage request, boolean simpleStructure, List filteredSubjectIds) {
      OpBroker broker = session.newBroker();
      try {
         XComponent resultSet = new XComponent(XComponent.DATA_SET);
         String targetGroupLocator = (String) (request.getArgument(SOURCE_GROUP_LOCATOR));
         Integer outline = (Integer) (request.getArgument(OUTLINE_LEVEL));
         if (targetGroupLocator != null && outline != null) {
            OpLocator locator = OpLocator.parseLocator(targetGroupLocator);
            OpSubjectDataSetFactory.retrieveSubjectHierarchy(session, resultSet, filteredSubjectIds, locator.getID(), outline.intValue() + 1, simpleStructure);
         }
         return resultSet;
      }
      finally {
         broker.close();
      }
   }

   public static void createAdministrator(OpBroker broker) {
      // first check if an Administrator already exists
      OpQuery query = broker.newQuery(OpUser.ADMINISTRATOR_ID_QUERY);
      Iterator result = broker.iterate(query);
      if (!result.hasNext()) {
         OpTransaction t = broker.newTransaction();
         OpUser administrator = new OpUser();
         administrator.setName(OpUser.ADMINISTRATOR_NAME);
         administrator.setDisplayName(OpUser.ADMINISTRATOR_DISPLAY_NAME);
         administrator.setDescription(OpUser.ADMINISTRATOR_DESCRIPTION);
         administrator.setPassword(OpUser.BLANK_PASSWORD);
         administrator.doSetLevel(new Byte(OpUser.MANAGER_USER_LEVEL));
         broker.makePersistent(administrator);
         OpContact contact = new OpContact();
         contact.setUser(administrator);
         broker.makePersistent(contact);
         t.commit();
      }
   }

   public static void createEveryone(OpBroker broker) {
      // first check if Everybody group already exists
      OpQuery query = broker.newQuery(OpGroup.EVERYONE_ID_QUERY);
      Iterator result = broker.iterate(query);
      if (!result.hasNext()) {
         OpTransaction t = broker.newTransaction();
         OpGroup everyone = new OpGroup();
         everyone.setName(OpGroup.EVERYONE_NAME);
         everyone.setDisplayName(OpGroup.EVERYONE_DISPLAY_NAME);
         everyone.setDescription(OpGroup.EVERYONE_DESCRIPTION);
         broker.makePersistent(everyone);
         t.commit();
      }
   }

   /**
    * Checks if the logged in user or everyone group is the only subject in the array. If so, an error message is returned.
    *
    * @param session Session
    * @param request the request containing all the required parameters
    * @return a message containing an error if the logged in user was found amont the subjects
    */
   public XMessage checkSubjects(OpProjectSession session, XMessage request) {
      List subjectLocators = (List) (request.getArgument(SUBJECT_IDS));
      //get Everyone Group
      OpBroker broker = session.newBroker();
      try {
         checkSubjects(session, broker, subjectLocators);
      }
      catch (XServiceException exc) {
         XMessage reply = new XMessage();
         reply.setError(exc.getError());
         return (reply);
      }
      finally {
         broker.close();
      }
      return null;
   }

   private void checkSubjects(OpProjectSession session, OpBroker broker,
         List subjectLocators) throws XServiceException {
      //check if one of the selected subjects is the session user or is Everyone Group
      for (int i = 0; i < subjectLocators.size(); i++) {
         String locator = (String) subjectLocators.get(i);
         OpSubject s = (OpSubject) broker.getObject(locator);
         if (s == null) {
            continue; // some else was faster ?!?
         }
         if (s instanceof OpUser) {
            OpUser u = (OpUser) s;
            serviceIfcImpl.checkUser(session, broker, u);
         }
         else if (s instanceof OpGroup) {
            OpGroup g = (OpGroup) s;
            serviceIfcImpl.checkGroup(session, broker, g);
         }
      }
   }

   /**
    * Performs the necessary operation to sign-off a user.
    *
    * @param session a <code>XSession</code> representing the application server session.
    * @param request a <code>XMessage</code> representing the client request.
    * @return an <code>XMessage</code> representing the response.
    */
   public XMessage signOff(OpProjectSession session, XMessage request) {
      serviceIfcImpl.signOff(session);
      return null;
   }

   /* (non-Javadoc)
   * @see onepoint.project.OpProjectService#getServiceImpl()
   */
   @Override
   public Object getServiceImpl() {
      return serviceIfcImpl;
   }


   public static int getUsersOfLevel(OpBroker broker, byte level) {
      OpQuery query = broker.newQuery(USERS_QUERY);
      query.setByte(0, level);
      Iterator result = broker.iterate(query);
      int users = 0;
      if (result.hasNext()) {
         users = ((Number) result.next()).intValue();
      }
      return users;
   }

   private static class PermissionCopyHelper extends OpCollectionCopyHelper<OpPermission, OpPermission> {

      private OpBroker broker = null;
      private OpPermissionable tgt = null;
      
      public PermissionCopyHelper(OpBroker broker, OpPermissionable tgt) {
         this.broker = broker;
         this.tgt = tgt;
      }
      
      @Override
      protected void deleteInstance(OpPermission del) {
         del.setObject(null); // ATTN: already removed from the set, so just set to null here (would not happen with remove!)
         del.getSubject().removeOwnedPermission(del);
         broker.deleteObject(del);
      }

      @Override
      protected OpPermission newInstance(OpPermission src) {
         OpPermission np = new OpPermission();
         broker.makePersistent(np);

         src.getSubject().addOwnedPermission(np);
         tgt.addPermission(np);

         np.setAccessLevel(src.getAccessLevel());
         np.setSystemManaged(src.getSystemManaged());
         return np;
      }

   }
   
   public void copyPermissions(OpProjectSession session, OpBroker broker, OpPermissionable tgt, OpPermissionable src) {
      PermissionCopyHelper pch = new PermissionCopyHelper(broker, tgt);
      pch.copy(tgt.getPermissions(), src.getPermissions());
   }
   
   /**
    * Returns the instance of the user service which was registered with the service manager.
    * @return a <code>OpUserService</code> if the service was registered with the
    * service manager, <code>null</code> otherwise.
    */
   public static OpUserService getService() {
      return (OpUserService) XServiceManager.getService(OpUserServiceImpl.SERVICE_NAME);
   }
}
