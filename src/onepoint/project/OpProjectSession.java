/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import onepoint.error.XErrorMap;
import onepoint.express.server.XExpressSession;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpPersistenceManager;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpSource;
import onepoint.persistence.OpSourceManager;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpLock;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XResourceBrokerIfc;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.XSizeInputStream;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

public class OpProjectSession extends XExpressSession {

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpProjectSession.class);

   private static final long NO_ID = -1;

   protected long userId = NO_ID;
   protected long administratorId = NO_ID; // Site administrator
   private long everyoneId = NO_ID; // Everyone inside the site
   private List<Long> subjectIds = new ArrayList<Long>();
   private List<OpBroker> brokerList = new ArrayList<OpBroker>();
   protected String sourceName;
   private XResourceBrokerIfc resource_broker;

   private boolean restoreState = false;

   /**
    * Creates a new instance, but in order to REALLY use it you must call init method (with appropriate source
    * name to use).
    */
   public OpProjectSession() {
      //TODO - calin.pavel - this line should be changed when multiple databases will be supported.
      //for the configuration wizard, there are no sources initially
      if (OpSourceManager.containsSource(OpSource.DEFAULT_SOURCE_NAME)) {
         this.init(OpSource.DEFAULT_SOURCE_NAME);
      }
      else {
         super.setLocale(XLocaleManager.getDefaultLocale());
         super.setLocalizerParameters(OpSettingsService.getI18NParametersMap(this));
      }
   }

   /**
    * Creates a new instance, but in order to REALLY use it you must call init method (with appropriate source
    * name to use).
    *
    * @param sourceName source name
    */
   public OpProjectSession(String sourceName) {
      this.init(sourceName);
   }
   
   /**
    * Defines the name of the source that will be used to create brokers.
    * Attention: This method must be called imediatelly after intialization/constructor.
    *
    * @param sourceName source name
    */
   public void init(String sourceName) {
      this.sourceName = sourceName;

      OpBroker broker = newBroker();
      try {
         if (broker.getConnection() != null && broker.getConnection().isValid()) {
            resetLocaleToSystemDefault();

            lookUpAdministratorID(broker);
            lookUpEveryoneID(broker);
         }
         else {
            super.setLocale(XLocaleManager.getDefaultLocale());
         }
         super.setLocalizerParameters(OpSettingsService.getI18NParametersMap(this));
      }
      finally {
         broker.close();
      }
   }

   /**
    * Resets the session locale to the system default locale.
    */
   public void resetLocaleToSystemDefault() {
      XLocale default_locale = XLocaleManager.findLocale(OpSettingsService.getService().getStringValue(this, OpSettings.USER_LOCALE_ID));
      super.setLocale(default_locale);
   }

   public void authenticateUser(OpBroker broker, OpUser user) {
      // Set user ID and
      if (user == null) {
         userId = NO_ID;
      }
      else {
         userId = user.getId();
      }

      loadSubjectIds(broker);
      lookUpAdministratorID(broker);
      lookUpEveryoneID(broker);

      //mark the session as valid
      validate();
   }

   /**
    * The name of the data source.
    *
    * @return name of the data source.
    */
   public String getSourceName() {
      return sourceName;
   }

   public long getUserID() {
      return userId;
   }

   public final List getSubjectIds() {
      return subjectIds;
   }

   public OpUser user(OpBroker broker) {
      return (OpUser) broker.getObject(OpUser.class, userId);
   }

   protected void lookUpAdministratorID(OpBroker broker) {
      OpQuery query = broker.newQuery(OpUser.ADMINISTRATOR_ID_QUERY);
      Iterator result = broker.iterate(query);
      if (result.hasNext()) {
         administratorId = (Long) result.next();
      }
      else {
         administratorId = NO_ID;
      }
   }

   public long getAdministratorID() {
      return administratorId;
   }

   public OpUser administrator(OpBroker broker) {
      if (administratorId == NO_ID) {
         lookUpAdministratorID(broker);
      }
      return (OpUser) broker.getObject(OpUser.class, administratorId);
   }

   protected void lookUpEveryoneID(OpBroker broker) {
      OpQuery query = broker.newQuery(OpGroup.EVERYONE_ID_QUERY);
      Iterator result = broker.iterate(query);
      if (result.hasNext()) {
         everyoneId = (Long) result.next();
      }
      else {
         everyoneId = NO_ID;
      }
   }

   public long getEveryoneID() {
      return everyoneId;
   }

   public OpGroup everyone(OpBroker broker) {
      if (everyoneId == NO_ID) {
         lookUpEveryoneID(broker);
      }
      return (OpGroup) broker.getObject(OpGroup.class, everyoneId);
   }

   public void loadSubjectIds(OpBroker broker) {
      // TODO: Get all subject-Ids of the current user
      // Clear subject ID list and insert user as first entry
      subjectIds.clear();
      subjectIds.add(userId);
      // Use HQL, because we only need the IDs
      OpQuery query = broker
           .newQuery("select assignment.Group.id from OpUserAssignment as assignment where assignment.User.id = ?");
      query.setLong(0, userId);
      Iterator i = broker.iterate(query);
      List<Long> groups = new ArrayList<Long>();
      Long subjectId = null;
      while (i.hasNext()) {
         subjectId = (Long) i.next();
         subjectIds.add(subjectId);
         groups.add(subjectId);
      }
      // Iteratively add super groups of groups to subject ID list
      query = broker
           .newQuery("select assignment.SuperGroup.id from OpGroupAssignment as assignment where assignment.SubGroup.id in (:subjectIds)");
      while (groups.size() > 0) {
         query.setCollection("subjectIds", groups);
         i = broker.iterate(query);
         groups.clear();
         while (i.hasNext()) {
            subjectId = (Long) i.next();
            subjectIds.add(subjectId);
            groups.add(subjectId);
         }
      }
   }

   /**
    * Creates a new broker
    *
    * @return a new <code>Broker</code> instance.
    */
   public OpBroker newBroker() {
      if (sourceName == null) {
         throw new NullPointerException("Count not instantiate broker without source name.");
      }

      OpBroker broker = OpPersistenceManager.newBroker(sourceName);
      brokerList.add(broker);
      return broker;
   }

   public XError newError(XErrorMap errorMap, int errorCode) {
      return errorMap.newError(errorCode, getLocale());
   }

   public XError newError(XErrorMap errorMap, int errorCode, List args) {
      return errorMap.newError(errorCode, getLocale(), args);
   }

   public XError newError(XErrorMap errorMap, int errorCode, Map<String, Object> args) {
      return errorMap.newError(errorCode, getLocale(), args);
   }

   public byte effectiveAccessLevel(OpBroker broker, long objectId) {
      return effectiveAccessLevel(broker, objectId, getUserID());
   }

   public byte effectiveAccessLevel(OpBroker broker, OpPermissionable object) {
      return effectiveAccessLevel(broker, object.getId());
   }

   public byte effectiveAccessLevel(OpBroker broker, OpPermissionable object, OpUser user) {
      return effectiveAccessLevel(broker, object.getId(), user.getId());
   }
   
   public byte effectiveAccessLevel(OpBroker broker, long objectId, long userId) {
      Byte userAccessLevel = effectivePermissions(broker, objectId);
      
      // Check for locks and respective lock owners
      OpQuery query = broker.newQuery("select lock from OpLock as lock where lock.Target.id = ? and lock.Owner.id != ?");
      query.setLong(0, objectId);
      query.setLong(1, userId);
      Iterator result = broker.iterate(query);
      // If someone beside the current user has a lock: Downgrade the user access level accordingly
      OpLock lock;
      boolean otherLockersExist = false;
      while ((result.hasNext() && (!otherLockersExist))) {
         lock = (OpLock) result.next();
         otherLockersExist = !lock.lockedByMe(this, broker);
      }

      if (otherLockersExist && (userAccessLevel > OpPermission.CONTRIBUTOR)) {
         userAccessLevel = OpPermission.CONTRIBUTOR;
      }
      return userAccessLevel;
   }
   
   public byte effectivePermissions(OpBroker broker, long objectId) {
      // Site administrator has always administrative access
      if (userId == administratorId) {
         return OpPermission.ADMINISTRATOR;
      }
      // Invoke max-query on the object's permissions
      // TODO: Cache queries?
      OpQuery query = broker
           .newQuery("select max(permission.AccessLevel) from OpPermission as permission where permission.Object.id = (:objectId) and permission.Subject.id in (:subjectIds)");
      query.setLong("objectId", objectId);
      query.setCollection("subjectIds", getSubjectIds());
      Iterator result = broker.iterate(query);
      // No permissions mean access level zero (i.e., no access at all)
      if (result == null) {
         return 0;
      }
      Byte userAccessLevel = (Byte) result.next();
      if (userAccessLevel == null) {
         return 0;
      }
      return userAccessLevel;
   }

   public boolean checkAccessLevel(OpBroker broker, OpPermissionable object, OpUser user, byte accessLevel) {
      return checkAccessLevel(broker, object.getId(), user.getId(), accessLevel);
   }

   public boolean checkAccessLevel(OpBroker broker, OpPermissionable object, byte accessLevel) {
      return checkAccessLevel(broker, object.getId(), getUserID(), accessLevel);
   }

   public boolean checkAccessLevel(OpBroker broker, long objectId, byte accessLevel) {
      return checkAccessLevel(broker, objectId, getUserID(), accessLevel);
   }

   public boolean checkAccessLevel(OpBroker broker, long objectId, long userId, byte accessLevel) {
      return effectiveAccessLevel(broker, objectId, userId) >= accessLevel;
   }

   public boolean checkPermissions(OpBroker broker, long objectId, byte accessLevel) {
      return effectivePermissions(broker, objectId) >= accessLevel;
   }

   public Map effectiveAccessLevels(OpBroker broker, List objectIds) {
      // TODO: Copy code from above and strip object part
      // (Note: We take a Map because it is not efficiently possible to return the same order)
      return null;
   }

   public Set accessibleIds(OpBroker broker, Collection objectIds, byte accessLevel) {
      // Returns list of accessible object IDs (w/specified minimum access level)
      // Checks whether objects are accessible (i.e., observer access level) while retrieving them
      if (objectIds.size() == 0) {
         return new HashSet();
      }
      // Administrator user has always administrative access level
      Set<Object> accessibleIds = new HashSet<Object>();
      if (userId == administratorId) {
         for (Object objectId : objectIds) {
            accessibleIds.add(objectId);
         }
      }
      else {
         final String qString = "" +
         		"select " +
         		"  p.Object.id " +
         		"from " +
         		"  OpPermission as p " +
         		"where " +
         		"  p.Object.id in (:objectIds) and" +
         		"  p.Subject.id in (:subjectIds) and" +
         		"  p.AccessLevel >= :accessLevel";
         
         OpQuery query = broker.newQuery(qString);
         query.setCollection("objectIds", objectIds);
         query.setCollection("subjectIds", getSubjectIds());
         query.setByte("accessLevel", accessLevel);
         Iterator result = broker.iterate(query);
         // TODO: Probably return a special iterator (XAccessibleObjectsIterator) -- for scalability
         // (Note: Can be used to implement paging together w/a count argument)
         // ArrayList accessibleIds = new ArrayList();
         accessibleIds = new HashSet<Object>();
         while (result.hasNext()) {
            accessibleIds.add(result.next());
         }
      }
      if (accessibleIds.size() == 0) {
         return accessibleIds;
      }
      // Shortcurt if desired access level is contributor or observer (write-locks not relevant)
      if (accessLevel <= OpPermission.CONTRIBUTOR) {
         return accessibleIds;
      }
      // Correct effective access levels by retrieving not-owned lock count
      OpQuery query = broker
           .newQuery("select lock from " +
           		"OpLock as lock " +
           		"  where " +
           		"lock.Target.id in (:accessibleIds) and " +
           		"lock.Owner.id != :userId");
      
      query.setCollection("accessibleIds", accessibleIds);
      query.setLong("userId", getUserID());
      Iterator result = broker.iterate(query);
      while (result.hasNext()) {
         OpLock lock = (OpLock) result.next();
         accessibleIds.remove(new Long(lock.getTarget().getId()));
      }
      return accessibleIds;
   }

   /**
    * retrives all object af a certain type where I have at least accessLevel to
    * if objectIds is null, all objects of that type are looked at otherwise 
    * objectIds are the starting point
    * @param broker
    * @param objectIds the set of Ids to start with
    * @param accessLevel the level I want to have
    * @param order an order is abused here to specify type and - guess what - an order ;-)
    * @return
    */
   public Iterator accessibleObjects(OpBroker broker, Collection objectIds, byte accessLevel, OpObjectOrderCriteria order) {
      // Checks whether objects are accessible (i.e., observer access level) while retrieving them
      if (objectIds != null && objectIds.size() == 0) {
         return objectIds.iterator();
      }
      String entityName = order.getObjectName();
      String sortQuery = order.toHibernateQueryString("accessibleObject");

      // Administrator user has always administrative access level
      Map<Long, OpObject> accessibleObjectMap = new HashMap<Long, OpObject>();
      List<OpObject> accessibleObjects = new ArrayList<OpObject>(); // Presume order from order-by statement
      if (userId == administratorId) {
         StringBuffer queryString = new StringBuffer();
         queryString.append("select accessibleObject from ");
         queryString.append(entityName);
         queryString.append(" as accessibleObject ");
         if (objectIds != null) {
            queryString.append(" where accessibleObject.id in (:objectIds)");
         }
         queryString.append(sortQuery);
         OpQuery query = broker.newQuery(queryString.toString());
         if (objectIds != null) {
            query.setCollection("objectIds", objectIds);
         }
         Iterator result = broker.iterate(query);
         OpObject object;
         while (result.hasNext()) {
            object = (OpObject) result.next();
            accessibleObjectMap.put(object.getId(), object);
            accessibleObjects.add(object);
         }
      }
      else {
         String groupByString = order.toHibernateGroupByQuery("accessibleObject");
         if (groupByString.length() > 0) {
            groupByString = "," + groupByString;
         }
         StringBuffer queryBuffer = new StringBuffer("select accessibleObject");
         queryBuffer.append(", max(permission.AccessLevel)");
         queryBuffer.append(" from ");
         queryBuffer.append(entityName);
         queryBuffer.append(" as accessibleObject inner join accessibleObject.Permissions as permission");
         queryBuffer.append(" where ");
         if (objectIds != null) {
            queryBuffer.append("  accessibleObject.id in (:objectIds) and ");
         }
         queryBuffer.append("  permission.Subject.id in (:subjectIds) ");
         queryBuffer.append(" group by accessibleObject.id ");
         queryBuffer.append(groupByString);
         queryBuffer.append(" having max(permission.AccessLevel) >= :accessLevel");
         queryBuffer.append(sortQuery);
         OpQuery query = broker.newQuery(queryBuffer.toString());
         if (objectIds != null) {
            query.setCollection("objectIds", objectIds);
         }
         query.setCollection("subjectIds", getSubjectIds());
         query.setByte("accessLevel", accessLevel);
         Iterator result = broker.iterate(query);
         Object[] record = null;
         // TODO: Probably return a special iterator (XAccessibleObjectsIterator) -- for scalability
         // (Note: Can be used to implement paging together w/a count argument)
         OpObject object = null;
         while (result.hasNext()) {
            record = (Object[]) result.next();
            object = (OpObject) record[0];
            accessibleObjectMap.put(object.getId(), object);
            accessibleObjects.add(object);
         }
      }
      return accessibleObjects.iterator();
   }

   public final boolean userIsAdministrator() {
      return (userId != NO_ID) && (userId == administratorId);
   }

   public final boolean userIsAdministrator(OpUser user) {
      return (user.getId() != NO_ID) && (user.getId() == administratorId);
   }

   public final boolean userMemberOfGroup(long groupId) {
      return subjectIds.contains(new Long(groupId));
   }

   /**
    * Clears all the data on the project session.
    */
   public void clearSession() {
      super.clearSession();

      userId = NO_ID;
      administratorId = NO_ID;
      everyoneId = NO_ID;
      subjectIds.clear();
      this.clearVariables();
   }

   /**
    * Checks if the session is empty.
    *
    * @return a <code>true</code> if the session is empty (i.e there is no current user logged in).
    */
   public boolean isEmpty() {
      return userId == NO_ID;
   }

   /**
    * Counts the number of entities of the given type in the db.
    *
    * @param name   a <code>String</code> representing the name of the entity to count for.
    * @param broker a <code>OpBroker</code> used for performing business operations.
    * @return a <code>int</code> representing the number of the entities of the given type.
    */
   public int countEntity(String name, OpBroker broker) {
      StringBuffer queryStringBuffer = new StringBuffer("select count(entity) from ");
      queryStringBuffer.append(name);
      queryStringBuffer.append(" as entity");
      OpQuery query = broker.newQuery(queryStringBuffer.toString());
      Iterator it = broker.iterate(query);
      Number result = 0;
      while (it.hasNext()) {
         result = (Number) it.next();
      }
      return result.intValue();
   }

   /**
    * @param clearCache
    * @see onepoint.service.server.XSession#cleanupSession(boolean)
    */
   public void cleanupSession(boolean clearCache) {
      super.cleanupSession(clearCache);
      for (Iterator it = brokerList.iterator(); it.hasNext();) {
         OpBroker opBroker = (OpBroker) it.next();
         if (opBroker.isOpen()) {
            if (clearCache) {
               opBroker.clear();
            }
            opBroker.close();
         }
         it.remove();
      }
   }

   /**
    * Cleans the brokers which were opened by this session, with the exception of the brokers
    * found in the given list.
    *
    * @param exceptBrokers Brokers to be excluded from close/cleanup
    * @param clearCache
    */
   public void cleanupSession(List<OpBroker> exceptBrokers, boolean clearCache) {
      super.cleanupSession(clearCache);
      for (Iterator it = brokerList.iterator(); it.hasNext();) {
         OpBroker opBroker = (OpBroker) it.next();
         if (exceptBrokers.contains(opBroker)) {
            continue;
         }
         if (opBroker.isOpen()) {
            opBroker.close();
         }
         it.remove();
      }
   }

   /**
    * Returns the list of brokers opened by the session.
    *
    * @return a <code>List(OpBroker)</code>.
    */
   public List<OpBroker> getBrokerList() {
      return brokerList;
   }

   public boolean isUser(OpUser user) {
      if (user == null) {
         return (userId == NO_ID);
      }
      return (isUser(user.getId()));
   }

   public boolean isUser(long user_id) {
      return (userId == user_id);
   }

   public boolean isLoggedOn() {
      return (getUserID() != OpProjectSession.NO_ID);
   }

   /**
    * Gets the session variable which holds the client-timezone.
    *
    * @return a <code>TimeZone</code> object, representing the time zone of this client's session.
    */
   public TimeZone getClientTimeZone() {
      return (TimeZone) this.getVariable(OpProjectConstants.CLIENT_TIMEZONE);
   }

   /**
    * @see onepoint.service.server.XSession#invalidate()
    */
   @Override
   public void invalidate() {
      super.invalidate();
      this.resetLocaleToSystemDefault();
   }

   /**
    * Process the client request to persist the uploaded files. The request will be updated with the persisted files id
    *
    * @param message a <code>XMessage</code> instance
    * @throws java.io.IOException if the size of any of the files is larger than the configured max size
    */
   @Override
   public void processFiles(XMessage message)
        throws IOException {
      if (message != null) {

         Map<String, File> files = message.extractObjectsFromArguments(File.class);
         if (!files.isEmpty()) { //only do it if we need...
            Map<String, String> contents = new HashMap<String, String>();
            Map<File, String> processed = new HashMap<File, String>();
            long maxFileSize = OpInitializerFactory.getInstance().getInitializer().getMaxAttachmentSizeBytes();
            OpBroker broker = newBroker();
            try {
               for (Map.Entry<String, File> entry : files.entrySet()) {
                  String id = entry.getKey();
                  File file = entry.getValue();
                  if (file.length() > maxFileSize) {
                     String error = "The File " + entry.getKey() + " is larger than the configured size of " + maxFileSize + ". Aborting transaction";
                     logger.error(error);
                     throw new IOException(error);
                  }
                  if (processed.keySet().contains(file)) {
                     // this file was already processed, reuse the content
                     contents.put(id, processed.get(file));
                  }
                  else {
                     // process the file for the first time
                     try {
                        XSizeInputStream stream = new XSizeInputStream(new FileInputStream(file), file.length());
                        String mimeType = OpContentManager.getFileMimeType(file.getName());
                        OpContent content = OpContentManager.newContent(stream, mimeType, 0);

                        OpTransaction t = broker.newTransaction();
                        broker.makePersistent(content);
                        t.commit();

                        String contentId = content.locator();
                        contents.put(id, contentId);
                        processed.put(file, contentId);
                     }
                     catch (FileNotFoundException e) {
                        logger.error("The file: " + file.getAbsolutePath() + " could not be found to be persisted.");
                        contents.put(id, null);
                     }
                  }
               }
            }
            finally {
               broker.close();
            }
            message.insertObjectsIntoArguments(contents);
         }
      }
   }

   /**
    * Loads the application settings in this session.
 * @param startServices 
    */
   public void loadSettings(boolean startServices) {
      OpSettingsService.getService().loadSettings(this, startServices);
      OpSettingsService.getService().configureServerCalendar(this);
   }

   /**
    * The list of all the project sessions belonging to the same site and to the same server as the current session
    * (INCLUDING the id of the current session).
    *
    * @return the list of all the project sessions belonging to the same site and to the same server as the current
    *         session (INCLUDING the id of the current session).
    */
   public List<Integer> getIdsOfSessionsWithSameSource() {
      List<Integer> idsList = new ArrayList<Integer>();
      for (XSession session : getServer().getAllSessions()) {
         if (session instanceof OpProjectSession && ((OpProjectSession) session).getSourceName().equals(getSourceName())) {
            idsList.add(session.getID());
         }
      }
      return idsList;
   }

   /**
    * Checks if this session is the default session - meaning its source is the default source.
    *
    * @return a <code>true</code> if the project session is initialized with the default source.
    */
   public boolean isDefaultSession() {
      return OpSource.DEFAULT_SOURCE_NAME.equals(this.sourceName);
   }
   
   public OpProjectCalendar getCalendar() {
      return (OpProjectCalendar)super.getCalendar();
   }
   
   protected XCalendar getSessionDefaultCalendar() {
      return OpProjectCalendar.getDefaultCalendar();
   }

   public void setRestoreState(boolean state) {
      restoreState  = state;
   }

   public boolean isInRestoreState() {
      return restoreState;
   }

   public Set<Long> getManagedResources(OpBroker broker) {
      Set<Long> managedResources = new LinkedHashSet<Long>();
      Iterator resourceIterator = accessibleObjects(broker, null,
            OpPermission.MANAGER, new OpObjectOrderCriteria(OpResource.class,
                  OpResource.NAME, OpObjectOrderCriteria.ASCENDING));
      
      while (resourceIterator.hasNext()) {
         OpObjectIfc res = (OpObjectIfc) resourceIterator.next();
         managedResources.add(new Long(res.getId()));
      }
      return managedResources;
   }
}
