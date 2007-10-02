/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.XView;
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
import onepoint.project.util.OpHashProvider;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceException;

import java.util.*;

public class OpUserService extends OpProjectService {

   private static final XLog logger = XLogFactory.getServerLogger(OpUserService.class);

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


   // User data
   public final static String ASSIGNED_GROUPS = "assigned_groups";

   // *** Where do we provide the XML-code to register the service?
   // ==> Maybe the most consistent way it to include it in the module

   public final static String PASSWORD_TOKEN = "@*1XW9F4";
   private final static String NULL_PASSWORD = null;

   private final static String WARNING = "warning";

   private final static String USERS_QUERY = "select count(user) from OpUser as user where user.Level=? and user.Name != '" + OpUser.ADMINISTRATOR_NAME + "'";

   // FIXME(dfreis Mar 5, 2007 11:16:13 AM)
   // should be set within constructor!
   private OpUserServiceImpl serviceIfcImpl_ = new OpUserServiceImpl();

   public XMessage getHashAlgorithm(OpProjectSession session, XMessage request) {
      logger.debug("OpUserService.getHashAlgorithm()");

      String login = (String) (request.getArgument(LOGIN));
      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();
      String algo = serviceIfcImpl_.getHashAlgorithm(session, broker, login);
      reply.setVariable("algorithm", algo);
      broker.close();
      return reply;
   }

   public XMessage signOn(OpProjectSession session, XMessage request) {
      logger.debug("OpUserService.signOn()");

      String login = (String) (request.getArgument(LOGIN));
      String password = (String) (request.getArgument(PASSWORD));

      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();
      try {
         // note: transaction is required here for ldap identification, 
         //       because ldap identification may create new user and/or group objects
         OpTransaction t = broker.newTransaction();
         serviceIfcImpl_.signOn(session, broker, login, password);

         //initialize the calendar settings
         OpSettingsService.getService().configureServerCalendar(session);

         //send the calendar to the client
         reply.setVariable(OpProjectConstants.CALENDAR, session.getCalendar());
         t.commit();
      }
      catch (XServiceException exc) {
         exc.append(reply);
      }

      broker.close();
      return reply;
   }

   public XMessage insertUser(OpProjectSession session, XMessage request) {
      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
         return (reply);
      }
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
      Byte userLevelId = OpUser.DEFAULT_USER_LEVEL;
      try {
         userLevelId = Byte.parseByte(userLevel);
      }
      catch (NumberFormatException e) {
         reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.INVALID_USER_LEVEL));
         return reply;
      }
      user.setLevel(userLevelId);

      OpBroker broker = session.newBroker();

      reply = aditionalCheck(session, broker, user);
      if (reply.getError() != null) {
         broker.close();
         return reply;
      }

      // NOTE: do not remove local broker reference!! (ThreadLocal)
      OpTransaction t = broker.newTransaction();

      try {
         serviceIfcImpl_.insertUser(session, broker, user);

         // set language preference
         String language = (String) user_data.get(LANGUAGE);
         OpUserLanguageManager.updateUserLanguagePreference(broker, user, language);

         // set assignments
         List assigned_groups = (List) (user_data.get(ASSIGNED_GROUPS));
         if ((assigned_groups != null) && (assigned_groups.size() > 0)) {
            String choice = null;
            OpGroup group = null;
            for (int i = 0; i < assigned_groups.size(); i++) {
               choice = (String) (assigned_groups.get(i));
               group = (OpGroup) (broker.getObject(XValidator.choiceID(choice)));

               try {
                  serviceIfcImpl_.assign(session, broker, user, group);
               }
               catch (XServiceException exc) {
                  // only warning!
                  reply.setError(exc.getError());
                  reply.setArgument(WARNING, Boolean.TRUE);
               }
            }
         }

         // FIXME(dfreis Feb 28, 2007 3:11:28 PM)
         // do this using OpPreferencesAPI

         //create a preference regarding the show hours option, using the default value from the system settings
         String showHours = OpSettingsService.getService().get(OpSettings.SHOW_RESOURCES_IN_HOURS);
         OpPreference pref = new OpPreference();
         pref.setUser(user);
         pref.setName(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
         pref.setValue(showHours);
         broker.makePersistent(pref);
         t.commit();
         logger.debug("   make-persistent");
      }
      catch (XServiceException exc) {
         t.rollback();
         reply.setError(exc.getError());
      }
      broker.close();
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
      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }


      HashMap group_data = (HashMap) (request.getArgument(GROUP_DATA));

      XMessage reply = new XMessage();
//      XError error = null;

      OpGroup group = new OpGroup();
      group.setName((String) (group_data.get(OpSubject.NAME)));
      group.setDisplayName(group.getName());
      group.setDescription((String) (group_data.get(OpSubject.DESCRIPTION)));

      OpBroker broker = session.newBroker();
      List assigned_group_id_strings = (List) group_data.get(ASSIGNED_GROUPS);
      Vector<OpGroup> super_groups = new Vector<OpGroup>();
      if (assigned_group_id_strings != null) {
         String choice = null;
         Iterator iter = assigned_group_id_strings.iterator();
         OpGroup assigned_group;
         while (iter.hasNext()) {
            choice = (String) iter.next();
            assigned_group = (OpGroup) (broker.getObject(XValidator.choiceID(choice)));
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
      if (!serviceIfcImpl_.isAssignable(session, broker, group, super_groups.iterator())) {
         reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.LOOP_ASSIGNMENT));
         broker.close();
         return reply;
      }

      // validation successfully completed
      OpTransaction t = broker.newTransaction();
      try {
         serviceIfcImpl_.insertGroup(session, broker, group);
         try {
            serviceIfcImpl_.assign(session, broker, group, super_groups.iterator());
         }
         catch (XServiceException exc) {
            // only warning!
            reply.setError(exc.getError());
            reply.setArgument(WARNING, Boolean.TRUE);
         }

//        // assign groups
//        if ((assigned_group_id_strings != null) && (assigned_group_id_strings.size() > 0)) {
//          Iterator iter = assigned_group_id_strings.iterator();
//          while (iter.hasNext())
//          {
//            try
//            {
//              xxx
//              serviceIfcImpl_.assign(group, iter);
//            } catch (XServiceException exc)
//            {
//              // only warning!
//              reply.setError(exc.getError());
//              reply.setArgument(WARNING, Boolean.TRUE);
//            }
//          }
//        }

         t.commit();
      }
      catch (XServiceException exc) {
         t.rollback();
         reply.setError(exc.getError());//session.newError(OpOpUserServiceImplImpl.ERROR_MAP, OpUserError.LOOP_ASSIGNMENT));
         return reply;
      }
      finally {
         broker.close();
      }
      return (reply);
      //      broker.makePersistent(group);
   }

   public XMessage updateUser(OpProjectSession session, XMessage request) {
      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
         return (reply);
      }

      String id_string = (String) (request.getArgument(USER_ID));
      logger.debug("OpUserService.updateUser(): id = " + id_string);
      HashMap user_data = (HashMap) (request.getArgument(USER_DATA));

      XMessage reply = new XMessage();
//      XError error = null;

      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      try {
         OpUser user = serviceIfcImpl_.getUserByIdString(session, broker, id_string);

         // *** We could check if the fields have been modified (does this help or
         // not)?
         if (user == null) {
            logger.warn("ERROR: Could not find object with ID " + id_string);
            reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.USER_NOT_FOUND));
            return reply;
         }

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
            user.setLevel(Byte.parseByte((String) user_data.get(USER_LEVEL)));
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

         reply = aditionalCheck(session, broker, user);
         if (reply.getError() != null) {
            return reply;
         }

         // validation successfully completed
         t = broker.newTransaction();
         serviceIfcImpl_.updateUser(session, broker, user);

         //set the language
         String language = (String) user_data.get(LANGUAGE);
         boolean languageUpdated = OpUserLanguageManager.updateUserLanguagePreference(broker, user, language);
         if (languageUpdated && session.userIsAdministrator() && user.getID() == session.getUserID()) {
            //refresh forms
            XLocale newLocale = XLocaleManager.findLocale(language);
            session.setLocale(newLocale);
            reply.setArgument(OpProjectConstants.REFRESH_PARAM, Boolean.TRUE);
         }

         // Compare and update assignments
         List updatedGroupIds = (List) (user_data.get(ASSIGNED_GROUPS));
         Set<Long> storedGroupIds = new HashSet<Long>();
         for (OpUserAssignment assignment : user.getAssignments()) {
            storedGroupIds.add(new Long(assignment.getGroup().getID()));
         }

         if (updatedGroupIds != null) {
            Long groupId = null;
            OpGroup group;
            for (int i = 0; i < updatedGroupIds.size(); i++) {
               groupId = new Long(OpLocator.parseLocator((String) (updatedGroupIds.get(i))).getID());
               if (!storedGroupIds.remove(groupId)) {
                  group = serviceIfcImpl_.getGroupById(session, broker, groupId.longValue());
                  if (group == null) {
                     reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.USER_NOT_FOUND));
                     reply.setArgument(WARNING, Boolean.TRUE);
                  }
                  else {
                     // Assignment not yet persistent: Create new user assignment
                     try {
                        serviceIfcImpl_.assign(session, broker, user, group);
                     }
                     catch (XServiceException exc) {
                        // only warning!
                        reply.setError(exc.getError());
                        reply.setArgument(WARNING, Boolean.TRUE);
                     }
                  }
               }
            }
         }

         // remove the stored assignments that are not in the request
         if (storedGroupIds.size() > 0) {
            /*
            * --- Not yet supported in Hibernate (delete on joined sub-classes) OpQuery query = broker.newQuery("delete
            * OpUserAssignment where User.ID = :userId and Group.ID in (:groupIds)"); query.setLong("userId",
            * user.getID()); query.setCollection("groupIds", storedGroupIds);
            */
            OpQuery query = broker.newQuery(
                 "select assignment from OpUserAssignment as assignment where assignment.User.ID = :userId and assignment.Group.ID in (:groupIds)");
            query.setLong("userId", user.getID());
            query.setCollection("groupIds", storedGroupIds);
            Iterator result = broker.iterate(query);
            while (result.hasNext()) {
               OpUserAssignment assignment = (OpUserAssignment) result.next();
               serviceIfcImpl_.deleteUserAssignment(session, broker, assignment);
            }
         }

         t.commit();
      }
      catch (XServiceException exc) {
//        exc.printStackTrace();
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
    * Gets all the permissions for the given user. Goes recursively upwards collecting permissions from groups as well.
    *
    * @param user user object to gather the permissions for.
    * @return Set of permissions.
    */
   private Set getAllOwnedPermissions(OpUser user) {
      Set ownedPermissions = new HashSet(user.getOwnedPermissions());
      for (Iterator iterator = user.getAssignments().iterator(); iterator.hasNext();) {
         OpUserAssignment assignment = (OpUserAssignment) iterator.next();
         ownedPermissions.addAll(getAllOwnedPermissions(assignment.getGroup()));
      }
      return ownedPermissions;
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
      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
         return (reply);
      }
      String id_string = (String) (request.getArgument(GROUP_ID));
      logger.debug("OpUserService.updateGroup(): id = " + id_string);
      HashMap group_data = (HashMap) (request.getArgument(GROUP_DATA));

      XMessage reply = new XMessage();

      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      try {
         OpGroup group = serviceIfcImpl_.getGroupByIdString(session, broker, id_string);
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
               assigned_group = serviceIfcImpl_.getGroupByIdString(session, broker, XValidator.choiceID(choice));
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
         if (!serviceIfcImpl_.isAssignable(session, broker, group, super_groups.iterator())) {
            reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.LOOP_ASSIGNMENT));
            broker.close();
            return reply;
         }

         // validation successfully completed
         t = broker.newTransaction();
         serviceIfcImpl_.updateGroup(session, broker, group);

         // Compare and update assignments
         Iterator storedSuperGroupAssignments = group.getSuperGroupAssignments().iterator();
         OpGroupAssignment assignment = null;
         Set<Long> storedSuperGroupIds = new HashSet<Long>();
         while (storedSuperGroupAssignments.hasNext()) {
            assignment = (OpGroupAssignment) storedSuperGroupAssignments.next();
            storedSuperGroupIds.add(new Long(assignment.getSuperGroup().getID()));
         }

         if (assigned_groups != null) {
            Long groupId = null;
            OpGroup superGroup = null;
            for (int i = 0; i < assigned_groups.size(); i++) {
               groupId = new Long(OpLocator.parseLocator((String) (assigned_groups.get(i))).getID());
               if (!storedSuperGroupIds.remove(groupId)) {
                  superGroup = serviceIfcImpl_.getGroupById(session, broker, groupId.longValue());
                  // Assignment not yet persistent: Create new user assignment
                  if (superGroup == null) {
                     reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
                  }
                  else {
                     try {
                        serviceIfcImpl_.assign(session, broker, group, superGroup);
                     }
                     catch (XServiceException exc) {
                        // only warning!
                        reply.setError(exc.getError());
                     }
                  }
               }
            }
         }

         if (storedSuperGroupIds.size() > 0) {
            /*
            * --- Not yet supported by Hibernate (delete on joined sub-classes) OpQuery query = broker .newQuery("delete
            * OpGroupAssignment where SubGroup.ID = :subGroupId and SuperGroup.ID in (:superGroupIds)");
            * query.setLong("subGroupId", group.getID()); query.setCollection("superGroupIds", storedSuperGroupIds);
            * broker.execute(query);
            */

            OpQuery query = broker.newQuery(
                 "select assignment from OpGroupAssignment as assignment where assignment.SubGroup.ID = :subGroupId and assignment.SuperGroup.ID in (:superGroupIds)");
            query.setLong("subGroupId", group.getID());
            query.setCollection("superGroupIds", storedSuperGroupIds);
            Iterator result = broker.iterate(query);
            while (result.hasNext()) {
               assignment = (OpGroupAssignment) result.next();
               serviceIfcImpl_.deleteGroupAssignment(session, broker, assignment);
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
      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }


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
               OpGroup super_group = serviceIfcImpl_.getGroupById(session, broker, superLocator.getID());
               if (subLocator.getPrototype().getInstanceClass() == OpUser.class) {
                  // user to group
                  OpUser user = serviceIfcImpl_.getUserById(session, broker, subLocator.getID());
                  serviceIfcImpl_.removeUserFromGroup(session, broker, user, super_group);
               }
               else {
                  // group to group
                  OpGroup group = serviceIfcImpl_.getGroupById(session, broker, subLocator.getID());
                  serviceIfcImpl_.removeGroupFromGroup(session, broker, group, super_group);
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
      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      XMessage checkUser = checkSubjects(session, request);
      if (checkUser.getError() != null) {
         return checkUser;
      }

      List subjectLocators = (List) (request.getArgument(SUBJECT_IDS));
      logger.debug("OpUserService.deleteSubjects(): subject_ids = " + subjectLocators);

      if ((subjectLocators == null) || (subjectLocators.size() == 0)) {
         return null;
      }

      OpBroker broker = session.newBroker();

      List<Long> subjectIds = new ArrayList<Long>();
      for (int i = 0; i < subjectLocators.size(); i++) {

         Long uId = new Long(OpLocator.parseLocator((String) (subjectLocators.get(i))).getID());
         //the administrator can't be erased (the user can't delete itself)
         if (uId.longValue() != session.getAdministratorID()) {
            subjectIds.add(uId);
         }
         // TODO: In case of group -- check if group is empty/not-empty?
         // (Probably better: Do it in advance -- in the confirm dialog -- ConfirmDeleteNotEmptyGroups)
         // *** Note: This can be checked in the ConfirmDelete form provider (no script code necessary)!
      }

      OpTransaction t = broker.newTransaction();
      try {
         /*
         * --- Not yet supported in Hibernate (delete against joined sub-classes) OpQuery query = broker.newQuery("delete
         * OpSubject where ID in (:subjectIds)"); query.setCollection("subjectIds", subjectIds); broker.execute(query);
         */
         OpQuery query = broker.newQuery("select subject from OpUser as subject where subject.ID in (:subjectIds)");
         query.setCollection("subjectIds", subjectIds);
         Iterator result = broker.iterate(query);
         OpUser user;
         while (result.hasNext()) {
            user = (OpUser) result.next();
            serviceIfcImpl_.deleteUser(session, broker, user);
         }

         query = broker.newQuery("select subject from OpGroup as subject where subject.ID in (:subjectIds)");
         query.setCollection("subjectIds", subjectIds);
         result = broker.iterate(query);
         OpGroup group;
         while (result.hasNext()) {
            group = (OpGroup) result.next();
            serviceIfcImpl_.deleteGroup(session, broker, group);
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

      //only the administrator has access right
//      if (!session.userIsAdministrator()) {
//         XError error = session.newError(OpUserAPIImpl.ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
//         reply.setError(error);
//         return reply;
//      }

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
         OpGroup targetGroup = serviceIfcImpl_.getGroupByIdString(session, broker, targetGroupLocator);
         if (targetGroup == null) {
            logger.warn("ERROR: Could not find object with ID " + targetGroupLocator);
            reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
            broker.close();
            return reply;
         }

         t = broker.newTransaction();

         OpQuery userAssignmentQuery = broker
              .newQuery("select assignment.ID from OpUserAssignment as assignment where assignment.User.ID = ? and assignment.Group.ID = ?");
         OpQuery groupAssignmentQuery = broker
              .newQuery("select assignment.ID from OpGroupAssignment as assignment where assignment.SubGroup.ID = ? and assignment.SuperGroup.ID = ?");

         OpLocator subjectLocator = null;
         Iterator result = null;
         OpUser user = null;
         OpGroup group = null;
         for (int i = 0; i < subjectLocators.size(); i++) {
            subjectLocator = OpLocator.parseLocator((String) (subjectLocators.get(i)));
            if (subjectLocator.getPrototype().getInstanceClass() == OpUser.class) {
               // Assign user to target group
               userAssignmentQuery.setLong(0, subjectLocator.getID());
               userAssignmentQuery.setLong(1, targetGroup.getID());
               result = broker.iterate(userAssignmentQuery);
               if (!result.hasNext()) {
                  user = (OpUser) broker.getObject(OpUser.class, subjectLocator.getID());
                  if (user == null) {
                     reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.USER_NOT_FOUND));
                     reply.setArgument(WARNING, Boolean.TRUE);
                  }
                  else {
                     serviceIfcImpl_.assign(session, broker, user, targetGroup);
                  }

               }
            }
            else {
               // Assign group to target (super) group
               groupAssignmentQuery.setLong(0, subjectLocator.getID());
               groupAssignmentQuery.setLong(1, targetGroup.getID());
               result = broker.iterate(groupAssignmentQuery);
               if (!result.hasNext()) {
                  group = (OpGroup) broker.getObject(OpGroup.class, subjectLocator.getID());
                  if (group == null) {
                     reply.setError(session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.GROUP_NOT_FOUND));
                     reply.setArgument(WARNING, Boolean.TRUE);
                  }
                  else {
                     //loop check
                     if (!serviceIfcImpl_.isAssignable(session, broker, group, targetGroup)) {
                        reply = new XMessage();
                        XError error = session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.LOOP_ASSIGNMENT);
                        reply.setError(error);
                        reply.setArgument(WARNING, Boolean.TRUE);
                     }
                     else {
                        serviceIfcImpl_.assign(session, broker, group, targetGroup);
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
      XComponent resultSet = new XComponent(XComponent.DATA_SET);
      String targetGroupLocator = (String) (request.getArgument(SOURCE_GROUP_LOCATOR));
      Integer outline = (Integer) (request.getArgument(OUTLINE_LEVEL));
      if (targetGroupLocator != null && outline != null) {
         OpLocator locator = OpLocator.parseLocator(targetGroupLocator);
         OpSubjectDataSetFactory.retrieveSubjectHierarchy(session, resultSet, filteredSubjectIds, locator.getID(), outline.intValue() + 1, simpleStructure);
      }
      broker.close();
      return resultSet;
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
         administrator.setLevel(new Byte(OpUser.MANAGER_USER_LEVEL));
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
      XMessage reply = new XMessage();
      List subjectLocators = (List) (request.getArgument(SUBJECT_IDS));
      //get Everyone Group
      OpGroup everyone = session.everyone(session.newBroker());
      //everyone group should always exists
      long everyoneID = -1;
      if (everyone != null) {
         everyoneID = everyone.getID();
      }

      //check if one of the selected subjects is the session user or is Everyone Group
      for (int i = 0; i < subjectLocators.size(); i++) {
         long subjectId = OpLocator.parseLocator((String) (subjectLocators.get(i))).getID();
         if (subjectId == session.getUserID()) {
            XError error = session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.SESSION_USER);
            reply.setError(error);
            break;
         }
         if (subjectId == everyoneID) {
            XError error = session.newError(OpUserServiceImpl.ERROR_MAP, OpUserError.EVERYONE_GROUP);
            reply.setError(error);
            break;
         }
      }
      return reply;
   }

   /**
    * Performs the necessary operation to sign-off a user.
    *
    * @param session a <code>XSession</code> representing the application server session.
    * @param request a <code>XMessage</code> representing the client request.
    * @return an <code>XMessage</code> representing the response.
    */
   public XMessage signOff(OpProjectSession session, XMessage request) {
      OpBroker broker = session.newBroker();
      serviceIfcImpl_.signOff(session, broker);
      broker.close();
      return null;
   }

   /* (non-Javadoc)
   * @see onepoint.project.OpProjectService#getServiceImpl()
   */
   @Override
   public Object getServiceImpl() {
      return serviceIfcImpl_;
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


}