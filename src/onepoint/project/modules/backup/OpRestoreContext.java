/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.xml.XContext;

import java.util.*;

public class OpRestoreContext extends XContext {

   /**
    * Variable representing the working directory.
    */
   static final String WORKING_DIRECTORY = "WorkingDir";

   /**
    * The maximum number of operations done per a transaction.
    */
   private final static int MAX_INSERTS_PER_TRANSACTION = 1000;

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpRestoreContext.class, true);

   /**
    * The broker used for performing db operations.
    */
   private OpBroker broker = null;

   /**
    * Map that holds [String, List] representing pairs of [PrototypeName, List<BackupMember>].
    */
   private Map backupMembersMap = new HashMap();

   /**
    * A map of [Long, OpObject] containing the objects which have already been activated (and their ids from the backup file).
    */
   private Map persistedObjectsMap = new HashMap();

   /**
    * List of objects which will be added to the db.
    */
   private List objectsToAdd = new ArrayList();

   /**
    * The activated object's id.
    */
   private Long activeId = null;

   /**
    * The currently used prototype, in terms of the objects being parsed..
    */
   private OpPrototype activePrototype;

   /**
    * The currently used system object name.
    */
   private String activeSystem;

   /**
    * The currently used entity.
    */
   private OpObject activeObject;

   /**
    * The list of backup memeber for the prototype which objects are created.
    */
   private List activeBackupMembers;

   /**
    * The index of the current backup member.
    */
   private int activeMemberIndex = -1;

   /**
    * Numbers of objects which have been added.
    */
   private int insertCount = 0;

   /**
    * Creates a new restore context with the given broker.
    */
   OpRestoreContext(OpBroker broker) {
      this.broker = broker;
   }

   /**
    * Registers a new protoype with an empty list of members.
    *
    * @param prototypeName a <code>String</code> representing the name of a prototype.
    */
   void registerPrototype(String prototypeName) {
      backupMembersMap.put(prototypeName, new ArrayList());
   }

   /**
    * Gets the current list of members to backup for the given prototype.
    *
    * @param prototypeName a <code>String</code> representing the name of a prototype.
    * @return a <code>List</code> of <code>OpBackupMember</code>.
    */
   List getBackupMembers(String prototypeName) {
      return (List) backupMembersMap.get(prototypeName);
   }

   /**
    * Creates a new object instance.
    *
    * @param id     a <code>Long</code> representing the object id.
    * @param system a <code>String</code> representing the system name of the object (if it exists).
    * @return a <code>OpObject</code> instance.
    */
   OpObject activateObject(Long id, String system) {
      // Create new instance of the active prototype and reset its backup members
      activeId = id;
      activeSystem = system;
      activeObject = (OpObject) activePrototype.newInstance();
      activeMemberIndex = -1;
      return activeObject;
   }

   /**
    * Persists the active object.
    *
    * @throws OpBackupException if a system object cannot be found.
    */
   void persistActiveObject() {

      if (activeSystem != null) {
         // Map active ID to valid system object ID
         String queryString = OpBackupManager.getSystemObjectIDQuery(activeSystem);
         logger.debug("QUERY: " + queryString);
         OpQuery query = broker.newQuery(queryString);
         Iterator iterator = broker.iterate(query);
         if (iterator.hasNext()) {
            //if a system object already exists in the db, make sure you mark the active object as existent.
            long id = ((Long) iterator.next()).longValue();
            OpObject systemObject = broker.getObject(activePrototype.getInstanceClass(), id);

            //delete the already existent system object - otherwise inconsistencie might happen with system objects which are restored
            OpTransaction t = broker.newTransaction();
            logger.info("Deleting system object with prototype: " + activePrototype.getName() + " and id:" + systemObject.getID());
            broker.deleteObject(systemObject);
            t.commit();
         }
      }
      executeActiveObjectPersist();
   }

   /**
    * Effectively persists the active object.
    */
   private void executeActiveObjectPersist() {
      persistedObjectsMap.put(activeId, activeObject);
      logger.info("Persisting object with prototype:" + activePrototype.getName());
      insertCount++;
      objectsToAdd.add(activeObject);
      if (insertCount == MAX_INSERTS_PER_TRANSACTION) {
         commitRestoredObjects();
      }
   }

   /**
    * Activates a prototype with the given name.
    *
    * @param prototypeName
    * @throws OpBackupException if the given prototype name is invalid.
    */
   void activatePrototype(String prototypeName)
        throws OpBackupException {
      if (prototypeName == null) {
         activeBackupMembers = null;
         activePrototype = null;
         activeSystem = null;
         activeObject = null;
         activeId = null;
      }
      else {
         activePrototype = OpTypeManager.getPrototype(prototypeName);
         activeBackupMembers = (List) backupMembersMap.get(prototypeName);
         if (activePrototype == null || activeBackupMembers == null) {
            throw new OpBackupException("Cannot activate prototype with name:" + prototypeName);
         }
      }
   }

   /**
    * Returns the next backup member that will be restored.
    *
    * @return a <code>OpBackupMember</code> that will be restored.
    * @throws OpBackupException if the asking for a back-up member that wasn't backed up.
    */
   OpBackupMember nextBackupMember() {
      activeMemberIndex++;
      if (activeMemberIndex >= activeBackupMembers.size()) {
         throw new OpBackupException("There isn't any backup member with index:" + activeMemberIndex);
      }
      return (OpBackupMember) activeBackupMembers.get(activeMemberIndex);
   }

   /**
    * Gets an object which has already been activated (in order to establish a relationship to it).
    *
    * @param id a <code>Long</code> representing the id of the which to activate.
    * @return a <code>OpObject</code> instance or <code>null</code> if the object with the given id hasn't been activated
    *         yet.
    */
   OpObject getRelationshipOwner(Long id) {
      return (OpObject) persistedObjectsMap.get(id);
   }

   /**
    * Creates a transaction which commits all in-memory changes.
    */
   void commitRestoredObjects() {
      if (objectsToAdd.size() > 0) {
         logger.info("Inserting objects into db...");
         OpTransaction t = broker.newTransaction();
         Iterator it = objectsToAdd.iterator();
         while (it.hasNext()) {
            broker.makePersistent((OpObject) it.next());
         }
         t.commit();
         logger.info("Objects persisted");
         objectsToAdd.clear();
         insertCount = 0;
      }
   }
}
