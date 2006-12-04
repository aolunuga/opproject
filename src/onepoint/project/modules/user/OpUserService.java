/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.util.OpProjectConstants;
import onepoint.project.util.OpSHA1;
import onepoint.resource.*;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.util.*;
import java.util.regex.Pattern;

public class OpUserService extends OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpUserService.class, true);

   public final static String LOGIN = "login";
   public final static String PASSWORD = "password";
   public final static String PASSWORD_RETYPED = "PasswordRetyped";
   public final static String USER_LEVEL = "userLevel";
   public final static String USER_DATA = "user_data";
   public final static String USER_ID = "user_id";
   public final static String GROUP_DATA = "group_data";
   public final static String GROUP_ID = "group_id";
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
   // email pattern ex : eXpress@onepoint.at
   public final String emailRegex = "^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]";

   // *** Where do we provide the XML-code to register the service?
   // ==> Maybe the most consistent way it to include it in the module

   public final static OpUserErrorMap ERROR_MAP = new OpUserErrorMap();
   private final static String SELECT_SUBJECT_ID_BY_NAME_QUERY = "select subject.ID from OpSubject as subject where subject.Name = ?";

   public final static String PASSWORD_TOKEN = "@*1XW9F4";
   private final static String NULL_PASSWORD = null;
   private final static String BLANK_PASSWORD = new OpSHA1().calculateHash("");

   /**
    * Calendar related i18n settings
    */
   private final static String CALENDAR_RESOURCE_MAP_ID = "user.calendar";
   private final static String I18N_HOUR_INITIAL = "{$HourInitial}";
   private final static String I18N_DAY_INITIAL = "{$DayInitial}";
   private final static String I18N_WEEK_INITIAL = "{$WeekInitial}";
   private final static String[] I18N_MONTHS = new String[]{"{$January}", "{$February}", "{$March}", "{$April}", "{$May}", "{$June}", "{$July}",
        "{$August}", "{$September}", "{$October}", "{$November}", "{$December}"};
   private final static String[] I18N_WEEKDAYS = new String[]{"{$Monday}", "{$Tuesday}", "{$Wednesday}", "{$Thursday}", "{$Friday}",
        "{$Saturday}", "{$Sunday}"};
   private final static String WARNING = "warning";


   public XMessage signOn(XSession s, XMessage request) {
      logger.debug("OpUserService.signOn()");
      OpProjectSession session = (OpProjectSession) s;
      String login = (String) (request.getArgument(LOGIN));
      String password = (String) (request.getArgument(PASSWORD));

      XMessage reply = new XMessage();
      //don't perform any query because the login name doesn't exist
      if (login == null) {
         XError error = session.newError(ERROR_MAP, OpUserError.USER_UNKNOWN);
         reply.setError(error);
         return reply;
      }

      OpBroker broker = session.newBroker();
      if (login.equals(OpUser.ADMINISTRATOR_NAME_ALIAS1) || login.equals(OpUser.ADMINISTRATOR_NAME_ALIAS2)) {
         login = OpUser.ADMINISTRATOR_NAME;
      }
      // TODO: Use HQL because of inheritance
      OpQuery query = broker.newQuery("select user from OpUser as user where user.Name = ?");
      query.setString(0, login);
      logger.debug("...before find: login = " + login + "; pwd " + password);
      Iterator users = broker.iterate(query);
      logger.debug("...after find");
      if (users.hasNext()) {
         OpUser user = (OpUser) (users.next());
         logger.debug("### Found user for signOn: " + user.getName() + " (" + user.getDisplayName() + ")");
         if (!validatePasswords(user.getPassword(), password)) {
            logger.debug("==> Passwords do not match: Access denied");
            XError error = session.newError(ERROR_MAP, OpUserError.PASSWORD_MISMATCH);
            reply.setError(error);
            broker.close();
            return reply;
         }

         session.authenticateUser(broker, user);

         // TODO: Get user preferences and set, e.g., user locale
         // *** Insert a German and an English test user (by specifying user
         // preferences)

         XLocale user_locale = null;
         if (user.getPreferences() != null) {
            Iterator preferences = user.getPreferences().iterator();
            OpPreference preference = null;
            while (preferences.hasNext()) {
               preference = (OpPreference) preferences.next();
               if (preference.getName().equals(OpPreference.LOCALE)) {
                  // Set user locale
                  user_locale = XLocaleManager.findLocale(preference.getValue());
                  // TODO: Write warning into log file if user locale is not found
               }
            }
         }

         // Fallback: Global locale setting in the database
         if (user_locale == null) {
            user_locale = XLocaleManager.findLocale(OpSettings.get(OpSettings.USER_LOCALE));
         }
         session.setLocale(user_locale);

         //initialze the calendar settings
         Map userCalendarSettings = initializeUserCalendarSettings(user_locale);
         reply.setVariable(XCalendar.CALENDAR_SETTINGS, userCalendarSettings);

         //initialize the calendar instance which will be on the server
         //<FIXME author="Horia Chiorean" description="In the remote case, this means that always the last signed in user will have his settings...">
         XCalendar.getDefaultCalendar().configure(userCalendarSettings);
         //<FIXME>
      }
      else {
         XError error = session.newError(ERROR_MAP, OpUserError.USER_UNKNOWN);
         reply.setError(error);
      }

      // *** Throw exception/error if user does not exist/pwd does not match

      broker.close();
      return reply;
   }

   /**
    * Initializes the calendar settings for the logged in user.
    *
    * @param userLocale a <code>Locale</code> representing the user locale.
    * @return a <code>Map</code> with all the user settings.
    */
   private Map initializeUserCalendarSettings(XLocale userLocale) {
      Map calendarSettings = OpSettings.getCalendarSettings();

      XLanguageResourceMap userCalendarI18nMap = XLocaleManager.findResourceMap(userLocale.getID(), CALENDAR_RESOURCE_MAP_ID);
      XLocalizer userLocalizer = XLocalizer.getLocalizer(userCalendarI18nMap);

      String hourInitial = userLocalizer.localize(I18N_HOUR_INITIAL);
      calendarSettings.put(XCalendar.HOUR_INITIAL_KEY, hourInitial);
      String dayInitial = userLocalizer.localize(I18N_DAY_INITIAL);
      calendarSettings.put(XCalendar.DAY_INITIAL_KEY, dayInitial);
      String weekInitial = userLocalizer.localize(I18N_WEEK_INITIAL);
      calendarSettings.put(XCalendar.WEEK_INITIAL_KEY, weekInitial);

      String[] monthNames = new String[I18N_MONTHS.length];
      for (int i = 0; i < I18N_MONTHS.length; i++) {
         String i18nMonth = userLocalizer.localize(I18N_MONTHS[i]);
         monthNames[i] = i18nMonth;
      }
      calendarSettings.put(XCalendar.MONTH_NAMES_KEY, monthNames);

      char[] weekdayInitials = new char[I18N_WEEKDAYS.length + 1];
      //index 0 is empty
      weekdayInitials[0] = '\0';
      //monday
      char mondayInitial = userLocalizer.localize(I18N_WEEKDAYS[0]).charAt(0);
      weekdayInitials[Calendar.MONDAY] = mondayInitial;
      //tuesday
      char tuesdayInitial = userLocalizer.localize(I18N_WEEKDAYS[1]).charAt(0);
      weekdayInitials[Calendar.TUESDAY] = tuesdayInitial;
      //wednesday
      char wednesdayInitial = userLocalizer.localize(I18N_WEEKDAYS[2]).charAt(0);
      weekdayInitials[Calendar.WEDNESDAY] = wednesdayInitial;
      //thursday
      char thursdayInitial = userLocalizer.localize(I18N_WEEKDAYS[3]).charAt(0);
      weekdayInitials[Calendar.THURSDAY] = thursdayInitial;
      //friday
      char fridayInitial = userLocalizer.localize(I18N_WEEKDAYS[4]).charAt(0);
      weekdayInitials[Calendar.FRIDAY] = fridayInitial;
      //saturday
      char saturdayInitial = userLocalizer.localize(I18N_WEEKDAYS[5]).charAt(0);
      weekdayInitials[Calendar.SATURDAY] = saturdayInitial;
      //sunday
      char sundayInitial = userLocalizer.localize(I18N_WEEKDAYS[6]).charAt(0);
      weekdayInitials[Calendar.SUNDAY] = sundayInitial;

      calendarSettings.put(XCalendar.WEEKDAYS_INITIALS_KEY, weekdayInitials);

      calendarSettings.put(XCalendar.LOCALE_KEY, new Locale(userLocale.getID()));
      return calendarSettings;
   }

   public XMessage insertUser(XSession s, XMessage request) {
      logger.debug("OpUserService.insertUser()");
      OpProjectSession session = (OpProjectSession) s;

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      HashMap user_data = (HashMap) (request.getArgument(USER_DATA));

      XMessage reply = new XMessage();
      XError error = null;

      OpUser user = new OpUser();
      OpContact contact = new OpContact();

      contact.setFirstName((String) (user_data.get(OpContact.FIRST_NAME)));
      contact.setLastName((String) (user_data.get(OpContact.LAST_NAME)));
      contact.setEMail((String) (user_data.get(OpContact.EMAIL)));
      contact.setPhone((String) (user_data.get(OpContact.PHONE)));
      contact.setMobile((String) (user_data.get(OpContact.MOBILE)));
      contact.setFax((String) (user_data.get(OpContact.FAX)));

      user.setName((String) (user_data.get(OpUser.NAME)));
      user.setPassword((String) (user_data.get(OpUser.PASSWORD)));
      user.setDescription((String) (user_data.get(OpUser.DESCRIPTION)));

      // optional fields email, phone, mobile and fax
      if (user.getName() == null || user.getName().length() == 0) {
         error = session.newError(ERROR_MAP, OpUserError.LOGIN_MISSING);
         reply.setError(error);
         return reply;
      }
      else if (contact.getEMail() != null && (!contact.getEMail().equals(""))
           && (!Pattern.matches(emailRegex, contact.getEMail()))) {
         error = session.newError(ERROR_MAP, OpUserError.EMAIL_INCORRECT);
         reply.setError(error);
         return reply;
      }

      //configuration doesn't allow empty password fields
      if (!Boolean.valueOf(OpSettings.get(OpSettings.ALLOW_EMPTY_PASSWORD)).booleanValue()) {
         if (user.getPassword() == NULL_PASSWORD) {
            error = session.newError(ERROR_MAP, OpUserError.PASSWORD_MISSING);
            reply.setError(error);
            return reply;
         }
      }

      //check for password mismatch
      String retypedPassword = (String) user_data.get(PASSWORD_RETYPED);
      if (!validatePasswords(user.getPassword(), retypedPassword)) {
         error = session.newError(ERROR_MAP, OpUserError.PASSWORD_MISMATCH);
         reply.setError(error);
         return reply;
      }

      //get user level
      String userLevel = (String) user_data.get(USER_LEVEL);
      byte userLevelId = OpUser.STANDARD_USER_LEVEL;
      boolean invalidLevel = false;
      try {
         userLevelId = Byte.parseByte(userLevel);
         if (userLevelId != OpUser.MANAGER_USER_LEVEL && userLevelId != OpUser.STANDARD_USER_LEVEL) {
            invalidLevel = true;
         }
      }
      catch (NumberFormatException e) {
         invalidLevel = true;
      }
      if (invalidLevel) {
         error = session.newError(ERROR_MAP, OpUserError.INVALID_USER_LEVEL);
         reply.setError(error);
         return reply;
      }
      user.setLevel(new Byte(userLevelId));

      // Create display name (note: This could be made configurable in the future)
      String displayName = getDisplayName(contact, user.getName());
      user.setDisplayName(displayName);

      ArrayList assigned_groups = (ArrayList) (user_data.get(ASSIGNED_GROUPS));

      OpBroker broker = session.newBroker();

      // check if user login is already used
      OpQuery query = broker.newQuery(SELECT_SUBJECT_ID_BY_NAME_QUERY);
      query.setString(0, user.getName());
      Iterator userIds = broker.iterate(query);
      if (userIds.hasNext()) {
         error = session.newError(ERROR_MAP, OpUserError.LOGIN_ALREADY_USED);
         reply.setError(error);
         broker.close();
         return reply;
      }

      // everything is alright, now save the user
      OpTransaction t = broker.newTransaction();

      broker.makePersistent(user);
      contact.setUser(user);
      broker.makePersistent(contact);

      // Set language preference
      String language = (String) user_data.get("Language");
      OpUserLanguageManager.updateUserLanguagePreference(broker, user, language);

      if ((assigned_groups != null) && (assigned_groups.size() > 0)) {
         String choice = null;
         OpGroup group = null;
         OpUserAssignment assignment = null;
         for (int i = 0; i < assigned_groups.size(); i++) {
            choice = (String) (assigned_groups.get(i));
            group = (OpGroup) (broker.getObject(XValidator.choiceID(choice)));
            if (group == null) {
               error = session.newError(ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND);
               reply.setError(error);
               reply.setArgument(WARNING, Boolean.TRUE);
            }
            else {
               assignment = new OpUserAssignment();
               assignment.setUser(user);
               assignment.setGroup(group);
               broker.makePersistent(assignment);
            }
         }
      }

      //create a preference regarding the show hours option, using the default value from the system settings
      String showHours = OpSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS);
      OpPreference pref = new OpPreference();
      pref.setUser(user);
      pref.setName(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      pref.setValue(showHours);
      broker.makePersistent(pref);

      t.commit();
      logger.debug("   make-persistent");

      broker.close();

      return reply;

   }

   public XMessage insertGroup(XSession s, XMessage request) {
      logger.debug("OpUserService.insertGroup()");
      OpProjectSession session = (OpProjectSession) s;

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }


      HashMap group_data = (HashMap) (request.getArgument(GROUP_DATA));

      XMessage reply = new XMessage();
      XError error = null;

      OpGroup group = new OpGroup();
      group.setName((String) (group_data.get(OpSubject.NAME)));
      group.setDisplayName(group.getName());
      group.setDescription((String) (group_data.get(OpSubject.DESCRIPTION)));

      if (group.getName() == null || group.getName().length() == 0) {
         error = session.newError(ERROR_MAP, OpUserError.GROUP_NAME_MISSING);
         reply.setError(error);
         return reply;
      }

      OpBroker broker = session.newBroker();

      ArrayList assigned_groups = (ArrayList) (group_data.get(ASSIGNED_GROUPS));
      List superGroupsIds = new ArrayList();
      if (assigned_groups != null) {
         for (int i = 0; i < assigned_groups.size(); i++) {
            Long id = new Long(OpLocator.parseLocator((String) (assigned_groups.get(i))).getID());
            superGroupsIds.add(id);
         }
         if (checkGroupAssignmentsForLoops(broker, group, superGroupsIds)) {
            error = session.newError(ERROR_MAP, OpUserError.LOOP_ASSIGNMENT);
            reply.setError(error);
         }
      }
      if (error != null) {
         broker.close();
         return reply;
      }

      // check if group name is already used
      OpQuery query = broker.newQuery(SELECT_SUBJECT_ID_BY_NAME_QUERY);
      query.setString(0, group.getName());
      Iterator groupIds = broker.iterate(query);
      if (groupIds.hasNext()) {
         error = session.newError(ERROR_MAP, OpUserError.GROUP_NAME_ALREADY_USED);
         reply.setError(error);
         broker.close();
         return reply;
      }
      // validation successfully completed
      OpTransaction t = broker.newTransaction();
      broker.makePersistent(group);

      if ((assigned_groups != null) && (assigned_groups.size() > 0)) {
         String choice = null;
         OpGroup superGroup = null;
         OpGroupAssignment assignment = null;
         for (int i = 0; i < assigned_groups.size(); i++) {
            choice = (String) (assigned_groups.get(i));
            superGroup = (OpGroup) (broker.getObject(XValidator.choiceID(choice)));
            if (superGroup == null) {
               error = session.newError(ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND);
               reply.setError(error);
               reply.setArgument(WARNING, Boolean.TRUE);
            }
            else {
               assignment = new OpGroupAssignment();
               assignment.setSubGroup(group);
               assignment.setSuperGroup(superGroup);
               broker.makePersistent(assignment);
            }
         }
      }

      t.commit();
      broker.close();
      return reply;
   }

   public XMessage updateUser(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      String id_string = (String) (request.getArgument(USER_ID));
      logger.debug("OpUserService.updateUser(): id = " + id_string);
      HashMap user_data = (HashMap) (request.getArgument(USER_DATA));

      XMessage reply = new XMessage();
      XError error = null;

      OpBroker broker = session.newBroker();

      OpUser user = (OpUser) (broker.getObject(id_string));
      // *** We could check if the fields have been modified (does this help or
      // not)?
      if (user == null) {
         logger.warn("ERROR: Could not find object with ID " + id_string);
         error = session.newError(ERROR_MAP, OpUserError.USER_NOT_FOUND);
         reply.setError(error);
         broker.close();
         return reply;
      }

      String userName = (String) (user_data.get(OpUser.NAME));

      // check if user login is already used
      OpQuery query = broker.newQuery(SELECT_SUBJECT_ID_BY_NAME_QUERY);
      query.setString(0, userName);
      logger.debug("...before find: login = " + (String) (user_data.get(OpSubject.NAME)));

      Iterator userIds = broker.iterate(query);
      while (userIds.hasNext()) {
         Long userId = (Long) userIds.next();
         if (userId.longValue() != user.getID()) {
            error = session.newError(ERROR_MAP, OpUserError.LOGIN_ALREADY_USED);
            reply.setError(error);
            broker.close();
            return reply;
         }
      }

      OpContact contact = user.getContact();
      contact.setFirstName((String) (user_data.get(OpContact.FIRST_NAME)));
      contact.setLastName((String) (user_data.get(OpContact.LAST_NAME)));
      contact.setEMail((String) (user_data.get(OpContact.EMAIL)));
      contact.setPhone((String) (user_data.get(OpContact.PHONE)));
      contact.setMobile((String) (user_data.get(OpContact.MOBILE)));
      contact.setFax((String) (user_data.get(OpContact.FAX)));

      // check mandatory input fields
      if (userName == null || userName.length() == 0) {
         error = session.newError(ERROR_MAP, OpUserError.LOGIN_MISSING);
         reply.setError(error);
      }
      else if (contact.getEMail() != null && (!contact.getEMail().equals(""))
           && (!Pattern.matches(emailRegex, contact.getEMail()))) {
         error = session.newError(ERROR_MAP, OpUserError.EMAIL_INCORRECT);
         reply.setError(error);
      }

      // if error occured return the message
      if (error != null) {
         broker.close();
         return reply;
      }

      //get user level
      String userLevel = (String) user_data.get(USER_LEVEL);
      byte userLevelId = OpUser.STANDARD_USER_LEVEL;
      boolean invalidLevel = false;
      try {
         userLevelId = Byte.parseByte(userLevel);
         if (userLevelId != OpUser.MANAGER_USER_LEVEL && userLevelId != OpUser.STANDARD_USER_LEVEL) {
            invalidLevel = true;
         }
      }
      catch (NumberFormatException e) {
         invalidLevel = true;
      }
      if (invalidLevel) {
         error = session.newError(ERROR_MAP, OpUserError.INVALID_USER_LEVEL);
         reply.setError(error);
         return reply;
      }
      user.setLevel(new Byte(userLevelId));

      user.setName(userName);
      user.setDescription((String) (user_data.get(OpUser.DESCRIPTION)));
      //user password validation
      String password = (String) (user_data.get(OpUser.PASSWORD));
      String retypedPassword = (String) user_data.get(PASSWORD_RETYPED);

      String token = new OpSHA1().calculateHash(PASSWORD_TOKEN);

      if (validatePasswords(password, token)) {
         //check actual user password match only if user enters something in retype password field
         if (!validatePasswords(retypedPassword, NULL_PASSWORD) && !validatePasswords(user.getPassword(), retypedPassword)) {
            error = session.newError(ERROR_MAP, OpUserError.PASSWORD_MISMATCH);
            reply.setError(error);
            broker.close();
            return reply;
         }
      }
      else {//password changed on UI
         //check if configuration allows empty password fields
         if (!Boolean.valueOf(OpSettings.get(OpSettings.ALLOW_EMPTY_PASSWORD)).booleanValue()) {
            if (password == NULL_PASSWORD) {
               error = session.newError(ERROR_MAP, OpUserError.PASSWORD_MISSING);
               reply.setError(error);
               broker.close();
               return reply;
            }
         }
         //check for password mismatch
         if (!validatePasswords(password, retypedPassword)) {
            error = session.newError(ERROR_MAP, OpUserError.PASSWORD_MISMATCH);
            reply.setError(error);
            broker.close();
            return reply;
         }
         //and finally.....
         user.setPassword(password);
      }

      // Create display name (note: This could be made configurable in the future)
      String displayName = getDisplayName(contact, user.getName());
      user.setDisplayName(displayName);

      ArrayList updatedGroupIds = (ArrayList) (user_data.get(ASSIGNED_GROUPS));

      //set the language
      String language = (String) user_data.get("Language");
      OpUserLanguageManager.updateUserLanguagePreference(broker, user, language);

      // validation successfully completed
      OpTransaction t = broker.newTransaction();
      broker.updateObject(user);
      broker.updateObject(contact);

      // Compare and update assignments
      Iterator storedAssignments = user.getAssignments().iterator();
      OpUserAssignment assignment = null;
      HashSet storedGroupIds = new HashSet();
      while (storedAssignments.hasNext()) {
         assignment = (OpUserAssignment) storedAssignments.next();
         storedGroupIds.add(new Long(assignment.getGroup().getID()));
      }

      if (updatedGroupIds != null) {
         Long groupId = null;
         OpGroup group;
         for (int i = 0; i < updatedGroupIds.size(); i++) {
            groupId = new Long(OpLocator.parseLocator((String) (updatedGroupIds.get(i))).getID());
            if (!storedGroupIds.remove(groupId)) {
               group = (OpGroup) broker.getObject(OpGroup.class, groupId.longValue());
               if (group == null) {
                  reply.setError(session.newError(ERROR_MAP, OpUserError.USER_NOT_FOUND));
                  reply.setArgument(WARNING, Boolean.TRUE);
               }
               else {
                  // Assignment not yet persistent: Create new user assignment
                  assignment = new OpUserAssignment();
                  assignment.setGroup(group);
                  assignment.setUser(user);
                  broker.makePersistent(assignment);
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
         query = broker
              .newQuery("select assignment from OpUserAssignment as assignment where assignment.User.ID = :userId and assignment.Group.ID in (:groupIds)");
         query.setLong("userId", user.getID());
         query.setCollection("groupIds", storedGroupIds);
         Iterator result = broker.iterate(query);
         while (result.hasNext()) {
            assignment = (OpUserAssignment) result.next();
            broker.deleteObject(assignment);
         }
      }

      t.commit();
      broker.close();
      return reply;
   }

   public XMessage updateGroup(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }


      String id_string = (String) (request.getArgument(GROUP_ID));
      logger.debug("OpUserService.updateGroup(): id = " + id_string);
      HashMap group_data = (HashMap) (request.getArgument(GROUP_DATA));

      XMessage reply = new XMessage();
      XError error = null;

      OpBroker broker = session.newBroker();
      OpGroup group = (OpGroup) (broker.getObject(id_string));

      if (group == null) {
         logger.warn("ERROR: Could not find object with ID " + id_string);
         error = session.newError(ERROR_MAP, OpUserError.GROUP_NOT_FOUND);
         reply.setError(error);
         broker.close();
         return reply;
      }

      // check if group name is already used
      OpQuery query = broker.newQuery(SELECT_SUBJECT_ID_BY_NAME_QUERY);
      query.setString(0, (String) (group_data.get(OpSubject.NAME)));
      Iterator groupsIds = broker.iterate(query);

      while (groupsIds.hasNext()) {
         Long other = (Long) groupsIds.next();
         if (other.longValue() != group.getID()) {
            error = session.newError(ERROR_MAP, OpUserError.GROUP_NAME_ALREADY_USED);
            reply.setError(error);
            broker.close();
            return reply;
         }
      }

      group.setName((String) (group_data.get(OpSubject.NAME)));
      group.setDisplayName(group.getName());
      group.setDescription((String) (group_data.get(OpSubject.DESCRIPTION)));

      if (group.getName() == null || group.getName().length() == 0) {
         error = session.newError(ERROR_MAP, OpUserError.GROUP_NAME_MISSING);
         reply.setError(error);
      }


      ArrayList updatedSuperGroupIds = (ArrayList) (group_data.get(ASSIGNED_GROUPS));
      List superGroupsIds = new ArrayList();
      if (updatedSuperGroupIds != null) {
         for (int i = 0; i < updatedSuperGroupIds.size(); i++) {
            Long id = new Long(OpLocator.parseLocator((String) (updatedSuperGroupIds.get(i))).getID());
            superGroupsIds.add(id);
         }
         if (checkGroupAssignmentsForLoops(broker, group, superGroupsIds)) {
            error = session.newError(ERROR_MAP, OpUserError.LOOP_ASSIGNMENT);
            reply.setError(error);
         }
      }

      if (error != null) {
         broker.close();
         return reply;
      }

      // validation successfully completed
      OpTransaction t = broker.newTransaction();
      broker.updateObject(group);

      // Compare and update assignments
      Iterator storedSuperGroupAssignments = group.getSuperGroupAssignments().iterator();
      OpGroupAssignment assignment = null;
      HashSet storedSuperGroupIds = new HashSet();
      while (storedSuperGroupAssignments.hasNext()) {
         assignment = (OpGroupAssignment) storedSuperGroupAssignments.next();
         storedSuperGroupIds.add(new Long(assignment.getSuperGroup().getID()));
      }

      if (updatedSuperGroupIds != null) {
         Long groupId = null;
         OpGroup superGroup = null;
         for (int i = 0; i < updatedSuperGroupIds.size(); i++) {
            groupId = new Long(OpLocator.parseLocator((String) (updatedSuperGroupIds.get(i))).getID());
            if (!storedSuperGroupIds.remove(groupId)) {
               superGroup = (OpGroup) broker.getObject(OpGroup.class, groupId.longValue());
               // Assignment not yet persistent: Create new user assignment
               if (superGroup == null) {
                  error = session.newError(ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND);
                  reply.setError(error);
                  reply.setArgument(WARNING, Boolean.TRUE);
               }
               else {
                  assignment = new OpGroupAssignment();
                  assignment.setSuperGroup(superGroup);
                  assignment.setSubGroup(group);
                  broker.makePersistent(assignment);
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
         query = broker
              .newQuery("select assignment from OpGroupAssignment as assignment where assignment.SubGroup.ID = :subGroupId and assignment.SuperGroup.ID in (:superGroupIds)");
         query.setLong("subGroupId", group.getID());
         query.setCollection("superGroupIds", storedSuperGroupIds);
         Iterator result = broker.iterate(query);
         while (result.hasNext()) {
            assignment = (OpGroupAssignment) result.next();
            broker.deleteObject(assignment);
         }
      }

      t.commit();
      broker.close();
      return reply;
   }

   /**
    * Removes the assignments (user to group, group to group) between the given subjects (users/groups).
    *
    * @param s       the session
    * @param request map containg all the param required for the method
    * @return an error/success message
    */
   public XMessage deleteAssignments(XSession s, XMessage request) {

      OpProjectSession session = (OpProjectSession) s;

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }


      ArrayList superLocators = (ArrayList) (request.getArgument(SUPER_SUBJECT_IDS));
      ArrayList subLocators = (ArrayList) (request.getArgument(SUB_SUBJECT_IDS));

      OpBroker broker = session.newBroker();
      OpQuery userAssignmentQuery = broker
           .newQuery("select assignment from OpUserAssignment as assignment where assignment.User.ID = ? and assignment.Group.ID = ?");
      OpQuery groupAssignmentQuery = broker
           .newQuery("select assignment from OpGroupAssignment as assignment where assignment.SubGroup.ID = ? and assignment.SuperGroup.ID = ?");

      if (superLocators.size() > 0) {

         OpTransaction t = broker.newTransaction();
         Iterator result;
         for (int i = 0; i < superLocators.size(); i++) {
            Long superId = new Long(OpLocator.parseLocator((String) (superLocators.get(i))).getID());
            Long subId = new Long(OpLocator.parseLocator((String) (subLocators.get(i))).getID());

            OpLocator subLocator = OpLocator.parseLocator((String) (subLocators.get(i)));
            if (subLocator.getPrototype().getInstanceClass() == OpUser.class) {
               //user - to group
               userAssignmentQuery.setLong(0, subId.longValue());
               userAssignmentQuery.setLong(1, superId.longValue());
               result = broker.iterate(userAssignmentQuery);
               while (result.hasNext()) {
                  OpUserAssignment assignment = (OpUserAssignment) result.next();
                  broker.deleteObject(assignment);
               }
            }
            else {
               //group - to group
               groupAssignmentQuery.setLong(0, subId.longValue());
               groupAssignmentQuery.setLong(1, superId.longValue());
               result = broker.iterate(groupAssignmentQuery);
               while (result.hasNext()) {
                  OpGroupAssignment assignment = (OpGroupAssignment) result.next();
                  broker.deleteObject(assignment);
               }
            }
         }
         t.commit();
      }
      broker.close();

      return null;
   }


   public XMessage deleteSubjects(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      XMessage checkUser = checkSubjects(s, request);
      if (checkUser.getError() != null) {
         return checkUser;
      }

      ArrayList subjectLocators = (ArrayList) (request.getArgument(SUBJECT_IDS));
      logger.debug("OpUserService.deleteSubjects(): subject_ids = " + subjectLocators);

      if ((subjectLocators == null) || (subjectLocators.size() == 0)) {
         return null;
      }

      OpBroker broker = session.newBroker();

      ArrayList subjectIds = new ArrayList();
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

      /*
       * --- Not yet supported in Hibernate (delete against joined sub-classes) OpQuery query = broker.newQuery("delete
       * OpSubject where ID in (:subjectIds)"); query.setCollection("subjectIds", subjectIds); broker.execute(query);
       */
      OpQuery query = broker.newQuery("select subject from OpUser as subject where subject.ID in (:subjectIds)");
      query.setCollection("subjectIds", subjectIds);
      Iterator result = broker.iterate(query);
      OpUser subject;
      while (result.hasNext()) {
         subject = (OpUser) result.next();
         Set res = subject.getResources();
         for (Iterator iterator = res.iterator(); iterator.hasNext();) {
            OpResource resource = (OpResource) iterator.next();
            resource.setUser(null);
         }
         subject.setResources(new HashSet());
         broker.deleteObject(subject);
      }

      query = broker.newQuery("select subject from OpGroup as subject where subject.ID in (:subjectIds)");
      query.setCollection("subjectIds", subjectIds);
      result = broker.iterate(query);
      OpGroup group;
      while (result.hasNext()) {
         group = (OpGroup) result.next();
         broker.deleteObject(group);
      }

      t.commit();
      broker.close();
      return null;
   }

   public XMessage assignToGroup(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;
      XMessage reply = new XMessage();

      //only the administrator has access right
      if (!session.userIsAdministrator()) {
         XError error = session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      logger.debug("OpUserService.assignToGroup()");

      ArrayList subjectLocators = (ArrayList) (request.getArgument(SUBJECT_IDS));
      String targetGroupLocator = (String) (request.getArgument(TARGET_GROUP_ID));

      if ((subjectLocators == null) || (targetGroupLocator == null)) {
         return null;
      }

      OpBroker broker = session.newBroker();
      // *** Retrieve target group
      OpGroup targetGroup = (OpGroup) (broker.getObject(targetGroupLocator));
      if (targetGroup == null) {
         logger.warn("ERROR: Could not find object with ID " + targetGroupLocator);
         reply.setError(session.newError(ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      OpQuery userAssignmentQuery = broker
           .newQuery("select assignment.ID from OpUserAssignment as assignment where assignment.User.ID = ? and assignment.Group.ID = ?");
      OpQuery groupAssignmentQuery = broker
           .newQuery("select assignment.ID from OpGroupAssignment as assignment where assignment.SubGroup.ID = ? and assignment.SuperGroup.ID = ?");

      OpLocator subjectLocator = null;
      Iterator result = null;
      OpUserAssignment userAssignment = null;
      OpUser user = null;
      OpGroupAssignment groupAssignment = null;
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
                  reply.setError(session.newError(ERROR_MAP, OpUserError.USER_NOT_FOUND));
                  reply.setArgument(WARNING, Boolean.TRUE);
               }
               else {
                  userAssignment = new OpUserAssignment();
                  userAssignment.setUser(user);
                  userAssignment.setGroup(targetGroup);
                  broker.makePersistent(userAssignment);
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
                  reply.setError(session.newError(ERROR_MAP, OpUserError.GROUP_NOT_FOUND));
                  reply.setArgument(WARNING, Boolean.TRUE);
               }
               else {
                  //loop check
                  List superGroupsIds = new ArrayList();
                  superGroupsIds.add(new Long(targetGroup.getID()));
                  if (checkGroupAssignmentsForLoops(broker, group, superGroupsIds)) {
                     reply = new XMessage();
                     XError error = session.newError(ERROR_MAP, OpUserError.LOOP_ASSIGNMENT);
                     reply.setError(error);
                     reply.setArgument(WARNING, Boolean.TRUE);
                     continue;
                  }
                  groupAssignment = new OpGroupAssignment();
                  groupAssignment.setSubGroup(group);
                  groupAssignment.setSuperGroup(targetGroup);
                  broker.makePersistent(groupAssignment);
               }
            }
         }
      }

      t.commit();
      broker.close();
      return reply;
   }

   /**
    * Loads the children of the given group based on the filter and enable rules given as parameters.
    * Used for lazy loading, simple structure & filtering.
    *
    * @param s
    * @param request
    * @return
    */
   public XMessage expandFilteredGroup(XSession s, XMessage request) {

      XMessage reply = new XMessage();
      OpProjectSession session = (OpProjectSession) s;

      //filter for groups/users
      Boolean includeParentsInFilter = (Boolean) request.getArgument(INCLUDE_PARENTS_IN_FILTER);
      List filteredSubjectIds = (List) request.getArgument(FILTERED_SUBJECT_IDS);
      if (includeParentsInFilter != null && includeParentsInFilter.booleanValue()) {
         filteredSubjectIds = OpSubjectDataSetFactory.getAlreadyAssignedGroups(session, filteredSubjectIds);
      }
      XComponent resultSet = expandGroupStructure(session, request, true, filteredSubjectIds);

      if (resultSet != null) {

         boolean enableUsers = ((Boolean) request.getArgument(ENABLE_USERS)).booleanValue();
         boolean enableGroups = ((Boolean) request.getArgument(ENABLE_GROUPS)).booleanValue();
         OpSubjectDataSetFactory.enableSubjectHierarchy(resultSet, enableUsers, enableGroups);

         List resultList = new ArrayList();
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
    * @param s
    * @param request
    * @return
    */
   public XMessage expandGroup(XSession s, XMessage request) {
      XMessage reply = new XMessage();
      OpProjectSession session = (OpProjectSession) s;
      XComponent resultSet = expandGroupStructure(session, request, false, null);
      if (resultSet != null) {
         List resultList = new ArrayList();
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

   public static OpUser createAdministrator(OpBroker broker) {
      OpTransaction t = broker.newTransaction();
      OpUser administrator = new OpUser();
      administrator.setName(OpUser.ADMINISTRATOR_NAME);
      administrator.setDisplayName(OpUser.ADMINISTRATOR_DISPLAY_NAME);
      administrator.setDescription(OpUser.ADMINISTRATOR_DESCRIPTION);
      administrator.setPassword(BLANK_PASSWORD);
      broker.makePersistent(administrator);
      OpContact contact = new OpContact();
      contact.setUser(administrator);
      broker.makePersistent(contact);
      t.commit();
      return administrator;
   }

   public static OpGroup createEveryone(OpBroker broker) {
      OpTransaction t = broker.newTransaction();
      OpGroup everyone = new OpGroup();
      everyone.setName(OpGroup.EVERYONE_NAME);
      everyone.setDisplayName(OpGroup.EVERYONE_DISPLAY_NAME);
      everyone.setDescription(OpGroup.EVERYONE_DESCRIPTION);
      broker.makePersistent(everyone);
      t.commit();
      return everyone;
   }

   /**
    * Checks if the logged in user or everyone group is the only subject in the array. If so, an error message is returned.
    *
    * @param s       Session
    * @param request the request containing all the required parameters
    * @return a message containing an error if the logged in user was found amont the subjects
    */
   public XMessage checkSubjects(XSession s, XMessage request) {
      XMessage reply = new XMessage();
      OpProjectSession session = (OpProjectSession) s;
      ArrayList subjectLocators = (ArrayList) (request.getArgument(SUBJECT_IDS));
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
            XError error = session.newError(ERROR_MAP, OpUserError.SESSION_USER);
            reply.setError(error);
            break;
         }
         if (subjectId == everyoneID) {
            XError error = session.newError(ERROR_MAP, OpUserError.EVERYONE_GROUP);
            reply.setError(error);
            break;
         }
      }
      return reply;
   }

   /**
    * Performs the necessary operation to sign-off a user.
    * @param s a <code>XSession</code> representing the application server session.
    * @param request a <code>XMessage</code> representing the client request.
    * @return an <code>XMessage</code> representing the response.
    */
   public XMessage signOff(XSession s, XMessage request) {
      OpProjectSession projectSession = (OpProjectSession) s;
      projectSession.clearSession();
      XResourceCache.clearCache();
      return null;
   }

   /**
    * Gets the display name for a user.
    *
    * @param contact     a <code>OpContact</code> entity, holding a user's contact information.
    * @param defaultName a <code>String</code> representing a fallback name, if no display name is found in the contact.
    * @return a <code>String</code> representing the display name of a user.
    */
   private static String getDisplayName(OpContact contact, String defaultName) {
      StringBuffer result = new StringBuffer();
      if (contact.getFirstName() != null && contact.getFirstName().trim().length() > 0) {
         result.append(contact.getFirstName());
      }
      if (contact.getLastName() != null && contact.getLastName().trim().length() > 0) {
         if (result.length() > 0) {
            result.append(" ");
         }
         result.append(contact.getLastName());
      }

      if (result.length() == 0) {
         return defaultName;
      }
      else {
         return result.toString();
      }
   }

   /**
    * Checks the group assignments for loops including the newSuperGroups
    *
    * @param broker         the session <code>OpBroker</code>
    * @param group          the <code>OpGroup</code> for which the check is performed
    * @param superGroupsIds <code>List</code> containing the assigned super group ids for the <code>group</code>
    * @return true if a loop was found, false otherwise
    */
   private boolean checkGroupAssignmentsForLoops(OpBroker broker, OpGroup group, List superGroupsIds) {
      for (int i = 0; i < superGroupsIds.size(); i++) {
         long groupId = ((Long) superGroupsIds.get(i)).longValue();
         if (groupId == group.getID()) {
            return true;
         }
         OpGroup superGroup = (OpGroup) broker.getObject(OpGroup.class, groupId);
         if (superGroup != null) { //super group entity exists
            Set superAssignments = superGroup.getSuperGroupAssignments();
            List groups = new ArrayList();
            for (Iterator iterator = superAssignments.iterator(); iterator.hasNext();) {
               OpGroupAssignment groupAssignment = (OpGroupAssignment) iterator.next();
               OpGroup sGroup = groupAssignment.getSuperGroup();
               groups.add(new Long(sGroup.getID()));
            }
            if (checkGroupAssignmentsForLoops(broker, group, groups)) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Performs equality checking for the given passwords.
    *
    * @param password1 <code>String</code> first password
    * @param password2 <code>String</code> second password
    * @return boolean flag indicating passwords equality
    */
   private boolean validatePasswords(String password1, String password2) {
      if (password1 != null && password2 != null) {
         return password1.equals(password2);
      }
      if (password1 != null) {
         return password1.equals(password2) || password1.equals(BLANK_PASSWORD);//for backward compatibility
      }
      if (password2 != null) {
         return password2.equals(password1) || password2.equals(BLANK_PASSWORD);//for backward compatibility
      }
      return password1 == password2;
   }
}
