package onepoint.project;

import onepoint.error.XErrorMap;
import onepoint.express.server.XExpressSession;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.service.XError;

import java.util.*;

public class OpProjectSession extends XExpressSession {

   private static final long NO_ID = -1;
   private long userId = NO_ID;
   private long administratorId = NO_ID ; // Site administrator
   private long everyoneId = NO_ID; // Everyone inside the site
   private ArrayList subjectIds = new ArrayList();
   private static final XLog logger = XLogFactory.getLogger(OpProjectSession.class, true);
   public OpProjectSession() {
      OpBroker broker = newBroker();
      if (broker.getConnection() != null && broker.getConnection().isValid()) {
         XLocale default_locale = XLocaleManager.findLocale(OpSettings.get(OpSettings.USER_LOCALE));
         super.setLocale(default_locale);

         lookUpAdministratorID(broker);
         lookUpEveryoneID(broker);
         broker.close();
      }
      else {
         super.setLocale(XLocaleManager.getDefaultLocale());
      }
   }

   public void authenticateUser(OpBroker broker, OpUser user) {
      // Set user ID and
      userId = user.getID();

      loadSubjectIds(broker);
      lookUpAdministratorID(broker);
      lookUpEveryoneID(broker);

      //mark the session as valid
      validate();
   }

   public long getUserID() {
      return userId;
   }

   public final ArrayList getSubjectIds() {
      return subjectIds;
   }

   public OpUser user(OpBroker broker) {
      return (OpUser) broker.getObject(OpUser.class, userId);
   }

   protected void lookUpAdministratorID(OpBroker broker) {
      OpQuery query = broker.newQuery("select user.ID from OpUser as user where user.Name = ?");
      query.setString(0, OpUser.ADMINISTRATOR_NAME);
      Iterator result = broker.iterate(query);
      if (result.hasNext()) {
         administratorId = ((Long) result.next()).longValue();
      }
      else {
         administratorId = NO_ID;
      }
   }

   public long getAdministratorID() {
      return administratorId;
   }

   public OpUser administrator(OpBroker broker) {
      return (OpUser) broker.getObject(OpUser.class, administratorId);
   }

   protected void lookUpEveryoneID(OpBroker broker) {
      OpQuery query = broker.newQuery("select group.ID from OpGroup as group where group.Name = ?");
      query.setString(0, OpGroup.EVERYONE_NAME);
      Iterator result = broker.iterate(query);
      if (result.hasNext()) {
         everyoneId = ((Long) result.next()).longValue();
      }
      else {
         everyoneId = NO_ID;
      }
   }

   public long getEveryoneID() {
      return everyoneId;
   }

   public OpGroup everyone(OpBroker broker) {
      return (OpGroup) broker.getObject(OpGroup.class, everyoneId);
   }

   public void loadSubjectIds(OpBroker broker) {
      // TODO: Get all subject-Ids of the current user
      // Clear subject ID list and insert user as first entry
      subjectIds.clear();
      subjectIds.add(new Long(userId));
      // Use HQL, because we only need the IDs
      OpQuery query = broker
            .newQuery("select assignment.Group.ID from OpUserAssignment as assignment where assignment.User.ID = ?");
      query.setLong(0, userId);
      Iterator i = broker.iterate(query);
      ArrayList groups = new ArrayList();
      Long subjectId = null;
      while (i.hasNext()) {
         subjectId = (Long) i.next();
         subjectIds.add(subjectId);
         groups.add(subjectId);
      }
      // Iteratively add super groups of groups to subject ID list
      query = broker
            .newQuery("select assignment.SuperGroup.ID from OpGroupAssignment as assignment where assignment.SubGroup.ID in (:subjectIds)");
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

   public OpBroker newBroker() {
      return OpPersistenceManager.newBroker();
   }


   public XError newError(XErrorMap errorMap, int errorCode) {
      return errorMap.newError(errorCode, getLocale());
   }

   public byte effectiveAccessLevel(OpBroker broker, long objectId) {
      // Site administrator has always administrative access
      if (userId == administratorId) {
         return OpPermission.ADMINISTRATOR;
      }
      // Invoke max-query on the object's permissions
      // TODO: Cache queries?
      OpQuery query = broker
            .newQuery("select max(permission.AccessLevel) from OpPermission as permission where permission.Object.ID = (:objectId) and permission.Subject.ID in (:subjectIds)");
      query.setLong("objectId", objectId);
      query.setCollection("subjectIds", getSubjectIds());
      Iterator result = broker.iterate(query);
      // No permissions mean access level zero (i.e., no access at all)
      Byte userAccessLevel = (Byte) result.next();
      if (userAccessLevel == null) {
         return 0;
      }
      // Check for locks and respective lock owners
      query = broker
            .newQuery("select count(lock.ID) from OpLock as lock where lock.Target.ID = ? and lock.Owner.ID != ?");
      query.setLong(0, objectId);
      query.setLong(1, getUserID());
      result = broker.iterate(query);
      // If someone beside the current user has a lock: Downgrade the user access level accordingly
      if ((((Integer) result.next()).intValue() > 0) && (userAccessLevel.byteValue() > OpPermission.CONTRIBUTOR)) {
         userAccessLevel = new Byte(OpPermission.CONTRIBUTOR);
      }
      return userAccessLevel.byteValue();
   }

   public boolean checkAccessLevel(OpBroker broker, long objectId, byte accessLevel) {
      return effectiveAccessLevel(broker, objectId) >= accessLevel;
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
      Set accessibleIds = new HashSet();
      if (userId == administratorId) {
         Iterator i = objectIds.iterator();
         while (i.hasNext()) {
            accessibleIds.add(i.next());
         }
      }
      else {
         StringBuffer queryBuffer = new StringBuffer("select accessibleObject.ID");
         queryBuffer.append(" from OpObject as accessibleObject inner join accessibleObject.Permissions as permission");
         queryBuffer.append(" where accessibleObject.ID in (:objectIds) and permission.Subject.ID in (:subjectIds)");
         queryBuffer.append(" group by accessibleObject.ID having max(permission.AccessLevel) >= :accessLevel");
         OpQuery query = broker.newQuery(queryBuffer.toString());
         query.setCollection("objectIds", objectIds);
         query.setCollection("subjectIds", getSubjectIds());
         query.setByte("accessLevel", accessLevel);
         Iterator result = broker.iterate(query);
         // TODO: Probably return a special iterator (XAccessibleObjectsIterator) -- for scalability
         // (Note: Can be used to implement paging together w/a count argument)
         // ArrayList accessibleIds = new ArrayList();
         accessibleIds = new HashSet();
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
            .newQuery("select accessibleObject.ID, count(lock.ID) from OpObject as accessibleObject inner join accessibleObject.Locks as lock where accessibleObject.ID in (:accessibleIds) and lock.Owner.ID = :userId group by accessibleObject.ID");
      query.setCollection("accessibleIds", accessibleIds);
      query.setLong("userId", getUserID());
      Iterator result = broker.iterate(query);
      Object[] record = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         if (((Integer) record[1]).intValue() > 0) {
            accessibleIds.remove(record[0]);
         }
      }
      return accessibleIds;
   }

   public Iterator accessibleObjects(OpBroker broker, Collection objectIds, byte accessLevel, OpObjectOrderCriteria order) {
      // Checks whether objects are accessible (i.e., observer access level) while retrieving them
      if (objectIds.size() == 0) {
         return objectIds.iterator();
      }
      String entityName = order.getObjectName();
      String sortQuery = order.toHibernateQueryString("accessibleObject");

      // Administrator user has always administrative access level
      HashMap accessibleObjectMap = new HashMap();
      ArrayList accessibleObjects = new ArrayList(); // Presume order from order-by statement
      if (userId == administratorId) {
         StringBuffer queryString = new StringBuffer();
         queryString.append("select accessibleObject from ");
         queryString.append(entityName);
         queryString.append(" as accessibleObject where accessibleObject.ID in (:objectIds)");
         queryString.append(sortQuery);
         OpQuery query = broker.newQuery(queryString.toString());
         query.setCollection("objectIds", objectIds);
         Iterator result = broker.iterate(query);
         OpObject object = null;
         while (result.hasNext()) {
            object = (OpObject) result.next();
            object.setEffectiveAccessLevel(OpPermission.ADMINISTRATOR);
            accessibleObjectMap.put(new Long(object.getID()), object);
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
         queryBuffer.append(" where accessibleObject.ID in (:objectIds) and permission.Subject.ID in (:subjectIds)");
         queryBuffer.append(" group by accessibleObject.ID");
         queryBuffer.append(groupByString);
         queryBuffer.append(" having max(permission.AccessLevel) >= :accessLevel");
         queryBuffer.append(sortQuery);
         OpQuery query = broker.newQuery(queryBuffer.toString());
         query.setCollection("objectIds", objectIds);
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
            object.setEffectiveAccessLevel(((Byte) record[1]).byteValue());
            accessibleObjectMap.put(new Long(object.getID()), object);
            accessibleObjects.add(object);
         }
      }
      if (accessibleObjects.size() == 0) {
         return accessibleObjects.iterator();
      }
      // Correct effective access levels by retrieving not-owned locks
      OpQuery query = broker
           .newQuery("select accessibleObject.ID, count(lock.ID) from OpObject as accessibleObject inner join accessibleObject.Locks as lock where accessibleObject.ID in (:accessibleIds) and lock.Owner.ID = :userId group by accessibleObject.ID");
      query.setCollection("accessibleIds", accessibleObjectMap.keySet());
      query.setLong("userId", userId);
      Iterator result = broker.iterate(query);
      Object[] record = null;
      OpObject object = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         object = (OpObject) accessibleObjectMap.get(record[0]);
         if ((((Integer) record[1]).intValue() > 0) && (object.getEffectiveAccessLevel() > OpPermission.CONTRIBUTOR)) {
            object.setEffectiveAccessLevel(OpPermission.CONTRIBUTOR);
         }
      }
      return accessibleObjects.iterator();
   }

   public final boolean userIsAdministrator() {
      return (userId != NO_ID) && (userId == administratorId);
   }

   public final boolean userMemberOfGroup(long groupId) {
      return subjectIds.contains(new Long(groupId));
   }

   public void clearVariables() {
      super.clearVariables();
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
    * @return a <code>true</code> if the session is empty (i.e there is no current user logged in).
    */
   public boolean isEmpty() {
      return userId == NO_ID;
   }

   /**
    * Counts the number of entities of the given type in the db.
    * @param name a <code>String</code> representing the name of the entity to count for.
    * @param broker a <code>OpBroker</code> used for performing business operations.
    * @return a <code>int</code> representing the number of the entities of the given type.
    */
   public int countEntity(String name, OpBroker broker) {
      StringBuffer queryStringBuffer = new StringBuffer("select count(entity) from ");
      queryStringBuffer.append(name);
      queryStringBuffer.append(" as entity");
      OpQuery query = broker.newQuery(queryStringBuffer.toString());
      Iterator it = broker.list(query).iterator();
      Number result = new Integer(0);
      while (it.hasNext()) {
         result = (Number) it.next();
      }
      return result.intValue();
   }
}
