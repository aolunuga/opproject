/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import org.hibernate.exception.ConstraintViolationException;

import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpFilter;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XResourceCache;
import onepoint.service.server.XServiceException;
import onepoint.log.XLog;

/**
 * Service Implementation for Users and Groups.
 * This class is capable of:
 * Inserting, updating and deleting users and groups.
 * Signing on and off a user.
 * Assigning users and groups to groups.
 * Removing users and groups from groups.
 * Requesting users and groubs by id.
 * Traversing users and groups via children relations.
 *
 * @author dfreis
 *
 */

public class OpUserServiceImpl{

  private static final XLog logger_ = XLogFactory.getLogger(OpUserServiceImpl.class, true);

  /**
   * the map containing all error types.
   */
  public static final OpUserErrorMap ERROR_MAP = new OpUserErrorMap();

  /**
   * the user subject type used for simple type based filtering.
   */
  public static final int TYPE_USER = 1;

  /**
   * the group subject type used for simple type based filtering.
   */
  public static final int TYPE_GROUP = 2;

  /**
   * the all subjects type used to retrieve users and groups.
   */
  public static final int TYPE_ALL = TYPE_USER + TYPE_GROUP;

  private static final String ALL_ROOT_GROUPS = 
     "select activity from OpActivity as activity"
     + " inner join activity.Assignments as assignment"
     + " where assignment.Resource.ID in (:resourceIds)"
     + " and activity.SuperActivity = null" // no parent
//   + " and activity.ProjectPlan.ID :project" 
     + " and activity.Deleted = false"
     + " order by activity.Sequence";

  /**
   * Assigns the given {@link OpUser user} to the given {@link OpGroup group}.
   *
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param user the user that is to be assigned to a group.
   * @param group the group to assign the user to.
   * @throws XServiceException {@link OpUserError#USER_NOT_FOUND}
   * if {@link OpUser user} is null.
   * @throws XServiceException {@link OpUserError#SUPER_GROUP_NOT_FOUND}
   * if {@link OpGroup group} is null.
   * @throws XServiceException {@link OpUserError#INSUFFICIENT_PRIVILEGES}
   * if user is not administrator.
   */

  public final void assign(final OpProjectSession session, final OpBroker broker,
        final OpUser user, final OpGroup group)
    throws XServiceException {
    if (user == null) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.USER_NOT_FOUND));
    }

    if (group == null) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
    }
    if (!session.userIsAdministrator()) {
       throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
    }

    OpUserAssignment assignment = new OpUserAssignment();
    assignment.setUser(user);
    assignment.setGroup(group);
    broker.makePersistent(assignment);
  }

  /**
   * Returns an iterator over all groups that do not have a parent
   * group (= root groups).
   *
   * @param session the session of the user
   * @param broker the broker to use.
   * @return an iterator over all root groups.
   */

  public final Iterator<OpGroup> getRootGroups(
       final OpProjectSession session, final OpBroker broker) {

     return null;
//     // get myResourceIds
//     OpUser user = session.user(broker);
//     Set<OpResource> resources = user.getResources();
//
//     // construct query
//     OpQuery query = broker.newQuery(ALL_ROOT_GROUPS);
//     query.setCollection("resourceIds", resources);
//
//     // type save required...
//     final Iterator iter = broker.iterate(query);
//     return new Iterator<OpActivity>() {
//        public boolean hasNext() {
//           return iter.hasNext();
//        }
//
//        public OpActivity next() {
//           return (OpActivity) iter.next();
//        }
//
//        public void remove() {
//           iter.remove();
//        }
//     };
  }

  /**
   * Get all direct children {@link OpGroup}s and/or {@link OpUser}s of the given parent {@link OpGroup group}.
   * 
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param group
   * @param type
   * @return
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public Iterator<OpSubject> getSubSubjects(OpProjectSession session, OpBroker broker, OpGroup group, int type)
      throws XServiceException {
    return(getSubSubjects(session, broker, group, type, null));
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param group
   * @param type
   * @param filter
   * @return
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public Iterator<OpSubject> getSubSubjects(OpProjectSession session, OpBroker broker, OpGroup group, int type, OpFilter filter)
      throws XServiceException {
    LinkedList<OpSubject> matches = new LinkedList<OpSubject>();
    OpQuery query;
    if ((TYPE_GROUP & type) == TYPE_GROUP)
    {
      query = broker.newQuery("select SubGroup from OpGroupAssignment where SuperGroup.ID = ?");      
      query.setLong(0, group.getID());
      Iterator iter = broker.iterate(query);
      OpGroup child_group;
      while (iter.hasNext())
      {
        child_group = (OpGroup) iter.next();
        if ((filter == null) || (filter.accept(child_group)))
          matches.add(child_group); // add a clone
      }
    }
    if ((TYPE_USER & type) == TYPE_USER)
    {
     query = broker.newQuery("select User.Name from OpUserAssignment where Group.ID = ?");
     query.setLong(0, group.getID());
     Iterator iter = broker.iterate(query);
     OpUser child_user;
     while (iter.hasNext())
     {
       child_user = (OpUser)iter.next();
       if ((filter == null) || (filter.accept(child_user)))
         matches.add(child_user); // add a clone
     }
    }
    return(matches.iterator());
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param user
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public void insertUser(OpProjectSession session, OpBroker broker, OpUser user) throws XServiceException 
  {
    if (!session.userIsAdministrator()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
    }

    // optional fields email, phone, mobile and fax
    if (user.getName() == null || user.getName().length() == 0) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.LOGIN_MISSING));
    }
    OpContact contact = user.getContact(); // must not be null!
    if (!contact.isEmailValid()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.EMAIL_INCORRECT));
    }

    //configuration doesn't allow empty password fields
    if ((!Boolean.valueOf(OpSettings.get(OpSettings.ALLOW_EMPTY_PASSWORD)).booleanValue()) &&
        (user.passwordIsEmpty()))
      {
        throw new XServiceException(session.newError(ERROR_MAP, OpUserError.PASSWORD_MISSING));
      }

    // check user level
    if (!user.isLevelValid()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INVALID_USER_LEVEL));
    }

    // do the work ..
    try
    {
      broker.makePersistent(user);
      contact.setUser(user);
      broker.makePersistent(contact);
      broker.getConnection().flush(); // required to ensure ConstraintViolationException!
    } catch (ConstraintViolationException exc) { // name not unique!
      // note: possible check for exc.getConstraintName() to return correct error here if w use more unique keys!
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.LOGIN_ALREADY_USED));
    }
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param group
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public void insertGroup(OpProjectSession session, OpBroker broker, OpGroup group) throws XServiceException {
    if (!session.userIsAdministrator()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
    }
    if (group.getName() == null || group.getName().length() == 0) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.GROUP_NAME_MISSING));
    }
    
    try
    {
      broker.makePersistent(group);
      broker.getConnection().flush(); // required to ensure ConstraintViolationException!     
    } catch (ConstraintViolationException exc) { // name not unique!
      // note: possible check for exc.getConstraintName() to return correct error here if w use more unique keys!
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.GROUP_NAME_ALREADY_USED));
    }
  }

  /**
   * identifies this session with the given username and password.
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param username the username
   * @param password the password in encrypted form.
   * @return true if signOn went ok, false otherwise.
   * @throws XServiceException if username or password is not valid.
   * @pre session and broker must be valid
   * @post session is identified with the given username and password if signOn went ok.
   */
  public OpUser signOn(OpProjectSession session, OpBroker broker, String username, String password)
    throws XServiceException 
    {

    //don't perform any query because the login name doesn't exist
    if (username == null) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.USER_UNKNOWN));
    }      

    if (username.equals(OpUser.ADMINISTRATOR_NAME_ALIAS1) || 
        username.equals(OpUser.ADMINISTRATOR_NAME_ALIAS2)) 
    {
       username = OpUser.ADMINISTRATOR_NAME;
    }
    // TODO: Use HQL because of inheritance
    OpQuery query = broker.newQuery("select user from OpUser as user where user.Name = ?");
    query.setString(0, username);
    logger_.debug("...before find: login = " + username + "; pwd " + password);
    Iterator users = broker.iterate(query);
    logger_.debug("...after find");
    if (users.hasNext()) {
      OpUser user = (OpUser) (users.next());
      logger_.debug("### Found user for signOn: " + user.getName() + " (" + user.getDisplayName() + ")");
      if (!user.validatePassword(password)) {
        logger_.debug("==> Passwords do not match: Access denied");
        throw new XServiceException(session.newError(ERROR_MAP, OpUserError.PASSWORD_MISMATCH));
      }

      session.authenticateUser(broker, user);
      
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
      
      return(user); // return clone
    }
    throw new XServiceException(session.newError(ERROR_MAP, OpUserError.USER_UNKNOWN));
  }

  /**
   * signs of the currently signed on user.
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @pre session and broker must be valid
   * @post none
   */  
  public void signOff(OpProjectSession session, OpBroker broker) {
    session.clearSession();
    XResourceCache.clearCache();
  }

  /**
   * Returns the username of the user that is currently signed on, or <code>null</code> if no user is currently signed on.
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @return the currently signed on username or <code>null</code> if no user is currently signed on.
   * @pre session and broker must be valid
   * @post none
   */
  public OpUser signedOnAs(OpProjectSession session, OpBroker broker) {
    return (OpUser) broker.getObject(OpUser.class, session.getUserID());
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param user
   * @param group
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public void removeUserFromGroup(OpProjectSession session, OpBroker broker, OpUser user, OpGroup group) 
    throws XServiceException
  {
    if (!session.userIsAdministrator()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
    }

    OpQuery userAssignmentQuery = broker.newQuery("select assignment from OpUserAssignment as assignment where assignment.User.ID = ? and assignment.Group.ID = ?");
    userAssignmentQuery.setLong(0, user.getID());
    userAssignmentQuery.setLong(1, group.getID());
    
    Iterator result = broker.iterate(userAssignmentQuery);
    while (result.hasNext()) {
      OpUserAssignment assignment = (OpUserAssignment) result.next();
      broker.deleteObject(assignment);
    }
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param group
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public void updateGroup(OpProjectSession session, OpBroker broker, OpGroup group) throws XServiceException {
    if (!session.userIsAdministrator()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
    }
    String groupName = (String) group.getName();

    // check mandatory input fields
    if (groupName == null || groupName.length() == 0) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.GROUP_NAME_MISSING));
    }

    try
    {
//      broker.updateObject(group);    
      broker.getConnection().flush();
    } catch (ConstraintViolationException exc) { // name not unique!
      // note: possible check for exc.getConstraintName() to return correct error here if w use more unique keys!
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.GROUP_NAME_ALREADY_USED));
    }

  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param user
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public void updateUser(OpProjectSession session, OpBroker broker, OpUser user) throws XServiceException {
    if (!session.userIsAdministrator()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
    }
    String userName = (String) user.getName();
    // check mandatory input fields
    if (userName == null || userName.length() == 0) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.LOGIN_MISSING));
    }
    OpContact contact = user.getContact();
    if (!contact.isEmailValid()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.EMAIL_INCORRECT));
    }
    
    //check if configuration allows empty password fields
    if (!Boolean.valueOf(OpSettings.get(OpSettings.ALLOW_EMPTY_PASSWORD)).booleanValue()) {
      if (user.validatePassword(null)) { 
        throw new XServiceException(session.newError(ERROR_MAP, OpUserError.PASSWORD_MISSING));
      }
    }
    // check user level
    if (!user.isLevelValid()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INVALID_USER_LEVEL));
    }
    try
    {
//      broker.updateObject(user);    
//      broker.updateObject(contact);
      broker.getConnection().flush();
    } catch (ConstraintViolationException exc) { // name not unique!
      // note: possible check for exc.getConstraintName() to return correct error here if w use more unique keys!
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.LOGIN_ALREADY_USED));
    }
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param id
   * @return
   * @pre session and broker must be valid
   * @post
   */
  public OpUser getUserById(OpProjectSession session, OpBroker broker, long id) {
    return((OpUser)broker.getObject(OpUser.class, id));
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param group
   * @param superGroup
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public void assign(OpProjectSession session, OpBroker broker, OpGroup group, OpGroup superGroup) throws XServiceException 
  {
    if (superGroup == null) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.SUPER_GROUP_NOT_FOUND));
    }

    if (!session.userIsAdministrator()) {
       throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
    }

    if (!isAssignable(session, broker, group, superGroup))
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.LOOP_ASSIGNMENT));
      
    OpGroupAssignment assignment = new OpGroupAssignment();
    assignment.setSubGroup(group);
    assignment.setSuperGroup(superGroup);
    broker.makePersistent(assignment);
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param group
   * @param super_group
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public void removeGroupFromGroup(OpProjectSession session, OpBroker broker, OpGroup group, OpGroup super_group) throws XServiceException 
  {
    if (!session.userIsAdministrator()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
    }
    OpQuery groupAssignmentQuery = broker.newQuery("select assignment from OpGroupAssignment as assignment where assignment.SubGroup.ID = ? and assignment.SuperGroup.ID = ?");
    groupAssignmentQuery.setLong(0, group.getID());
    groupAssignmentQuery.setLong(1, super_group.getID());
    
    Iterator result = broker.iterate(groupAssignmentQuery);
    while (result.hasNext()) {
      OpGroupAssignment assignment = (OpGroupAssignment) result.next();
      broker.deleteObject(assignment);
    }
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param group
   * @param super_groups
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public void assign(OpProjectSession session, OpBroker broker, OpGroup group, Iterator<OpGroup> super_groups) throws XServiceException {
    while (super_groups.hasNext())
    {
      assign(session, broker, group, super_groups.next());
    }
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param group
   * @param super_group
   * @return
   * @pre session and broker must be valid
   * @post
   */
  public boolean isAssignable(OpProjectSession session, OpBroker broker, OpGroup group, OpGroup super_group) {
    Vector<OpGroup> super_groups = new Vector<OpGroup>();
    super_groups.add(super_group);
    return(isAssignable(session, broker, group, super_groups.iterator()));
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param group
   * @param super_groups
   * @return
   * @pre session and broker must be valid
   * @post
   */
  public boolean isAssignable(OpProjectSession session, OpBroker broker, OpGroup group, Iterator<OpGroup> super_groups) {
    if ((group == null) || (super_groups == null))
      throw new IllegalArgumentException("params must not be null");
    OpGroup current_super_group;
    while (super_groups.hasNext())
    {
      current_super_group = super_groups.next();
      if (current_super_group == null) // super group unknown
        return(false);
      // primitive cycle
      if (group.equals(current_super_group))
        return(false);
      
      Iterator iter = current_super_group.getSuperGroupAssignments().iterator();
      OpGroupAssignment super_group_assignment;
      OpGroup super_super_group;
      Vector<OpGroup> super_super_groups = new Vector<OpGroup>();      
      while (iter.hasNext())
      {
        super_group_assignment = (OpGroupAssignment) iter.next();
        if (super_group_assignment == null)
          throw new IllegalArgumentException("super_group must not be null");
        super_super_group = super_group_assignment.getSuperGroup();
        super_super_groups.add(super_super_group);
      }
      // recursively check parent groups 
      if (!isAssignable(session, broker, group, super_super_groups.iterator())) {
        return(false);
      }
    }
    return(true); 
  }

//  public void removeGroupFromGroups(OpProjectSession session, OpBroker broker, OpGroup group, Iterator<OpGroup> super_groups) throws XServiceException {
//    // TODO Auto-generated method stub
//    
//  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param id_string
   * @return
   * @pre session and broker must be valid
   * @post
   */
  public OpUser getUserByIdString(OpProjectSession session, OpBroker broker, String id_string) {
    return((OpUser)broker.getObject(id_string));
  }
 
  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param assignment
   * @pre session and broker must be valid
   * @post
   */
  public void deleteUserAssignment(OpProjectSession session, OpBroker broker, OpUserAssignment assignment) {
     if (!session.userIsAdministrator()) {
        throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
     }
     broker.deleteObject(assignment);    
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param id_string
   * @return
   * @pre session and broker must be valid
   * @post
   */
  public OpGroup getGroupByIdString(OpProjectSession session, OpBroker broker, String id_string) {
    return((OpGroup)broker.getObject(id_string));
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param id
   * @return
   * @pre session and broker must be valid
   * @post
   */
  public OpGroup getGroupById(OpProjectSession session, OpBroker broker, long id) {
    return((OpGroup)broker.getObject(OpGroup.class, id));
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param assignment
   * @pre session and broker must be valid
   * @post
   */
  public void deleteGroupAssignment(OpProjectSession session, OpBroker broker, OpGroupAssignment assignment) {
     if (!session.userIsAdministrator()) {
        throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
     }
     broker.deleteObject(assignment);
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param user
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public void deleteUser(OpProjectSession session, OpBroker broker, OpUser user) throws XServiceException {
    if (!session.userIsAdministrator()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
    }
      
    Set res = user.getResources();
    for (Iterator iterator = res.iterator(); iterator.hasNext();) {
       OpResource resource = (OpResource) iterator.next();
       resource.setUser(null);
    }
    user.setResources(new HashSet());
    broker.deleteObject(user);
  }

  /**
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @param group
   * @throws XServiceException
   * @pre session and broker must be valid
   * @post
   */
  public void deleteGroup(OpProjectSession session, OpBroker broker, OpGroup group) throws XServiceException {
    if (!session.userIsAdministrator()) {
      throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
    }
    broker.deleteObject(group);
  }
}
