/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpSourceManager;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.OpTypeManager;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.custom_attribute.OpCustomValuePage;
import onepoint.project.modules.custom_attribute.OpCustomizable;
import onepoint.project.modules.customers.OpCustomer;
import onepoint.project.modules.documents.OpDynamicResource;
import onepoint.project.modules.documents.OpDynamicResourceable;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpAttachmentVersion;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.user.OpLock;
import onepoint.project.modules.user.OpLockable;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.util.Triple;
import onepoint.xml.XContext;
import onepoint.xml.XDocumentHandler;

public class OpRestoreContext extends XContext {

   /**
    * Variable representing the working directory.
    */
   static final String WORKING_DIRECTORY = "WorkingDir";

   /**
    * The maximum number of operations done per a transaction.
    */
   public final static int MAX_INSERTS_PER_TRANSACTION = 1000;

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpRestoreContext.class);

   private static final Map<String, String> PROTOTYPE_MAP = new HashMap<String, String>();

   private static final Map<String, String> RELATION_MAP = new HashMap<String, String>();

   /**
    * The servser session used for performing db operations.
    */
   private OpProjectSession session = null;

   private OpBroker broker;
   private OpTransaction transaction = null;

   /**
    * Map that holds [String, List] representing pairs of [PrototypeName, List<BackupMember>].
    */
   private Map<String, List> backupMembersMap = new HashMap<String, List>();

   /**
    * A map of [Long, OpObjectIfc] containing the objects which have already been activated (and their ids from the backup file).
    */
   private Map<Long, Map<OpPrototype, OpObjectIfc>> persistedObjectsMap = new HashMap<Long, Map<OpPrototype,OpObjectIfc>>();

   /**
    * A mapping of backupIds to db Locator strings , after objects have  been inserted in the db
    */
   private Map<OpLocator, OpLocator> backupLocatorToLocatorMap = new HashMap<OpLocator, OpLocator>();

   /**
    * List of objects which will be added to the db.
    */
   private List<OpObjectIfc> objectsToAdd = new ArrayList<OpObjectIfc>();

   /**
    * The activated object's id.
    */
   OpLocator activeLocator = null;

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
   private OpObjectIfc activeObject;

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

   private HashSet<Triple<String, String, OpBackupMember>> delayedRelations = new HashSet<Triple<String, String, OpBackupMember>>();
   private HashMap<Triple<OpLocator, OpLocator, OpBackupMember>, OpObjectIfc> delayedRelationsPerTransaction = new HashMap<Triple<OpLocator,OpLocator,OpBackupMember>, OpObjectIfc>();

   private XDocumentHandler documentHandler;

   private int totalInsertCount = 0;


   static {
      PROTOTYPE_MAP.put("OpExternalApplicationParameter", "OpExtAppParameter");
      PROTOTYPE_MAP.put("OpExternalApplicationUserParameter", "OpExtAppUserParam");
      PROTOTYPE_MAP.put("OpExternalApplicationUser", "OpExtAppUser");
      PROTOTYPE_MAP.put("OpExternalApplication", "OpExtApp");
      PROTOTYPE_MAP.put("OpExtAppUserParameter", "OpExtAppUserParam");
   
      RELATION_MAP.put("OpDynamicResource::Object", "OpDynamicResourceable");
      RELATION_MAP.put("OpPermission::Object", "OpPermissionable");
      RELATION_MAP.put("OpLock::Target", "OpLockable");
      RELATION_MAP.put("OpCustomValuePage::Object", "OpCustomValuePage");
      
      OpBackupTypeManager.addMappedMethod(OpCustomValuePage.class, "setObject", "OpObject", OpCustomValuePage.class, "setObject", OpCustomizable.class);
      OpBackupTypeManager.addMappedMethod(OpDynamicResource.class, "setObject", "OpObject", OpDynamicResource.class, "setObject", OpDynamicResourceable.class);
      OpBackupTypeManager.addMappedMethod(OpAttachment.class, "setProjectPlan", "OpProjectPlan", OpAttachment.class, "setProjectNode", OpProjectNode.class, "getProjectNode");
      OpBackupTypeManager.addMappedMethod(OpAttachmentVersion.class, "setPlanVersion", "OpProjectPlanVersion", null, null, null);
      OpBackupTypeManager.addMappedMethod(OpLock.class, "setTarget", "OpObject", OpLock.class, "setTarget", OpLockable.class);
      OpBackupTypeManager.addMappedMethod(OpWorkSlip.class, "setNumber", "Integer", null, null, null);
      OpBackupTypeManager.addMappedMethod(OpProjectPlan.class, "setHolidayCalendar", "String", null, null, null);
      OpBackupTypeManager.addMappedMethod(OpProjectPlanVersion.class, "setHolidayCalendar", "String", null, null, null);
      OpBackupTypeManager.addMappedMethod(OpCustomer.class, "setContactFirstName", "String", OpCustomer.class, "setContactName", String.class, "concatContactFirstName");      
      OpBackupTypeManager.addMappedMethod(OpCustomer.class, "setContactLastName", "String", OpCustomer.class, "setContactName", String.class, "concatContactLastName");      

   }
   
   
   /**
    * Creates a new restore context with the given broker.
    *
    * @param session a <code>OpProjectSession</code> the server session.
    * @param documentHandler 
    */
   OpRestoreContext(OpProjectSession session, OpBroker broker, XDocumentHandler documentHandler) {
      this.session = session;
      this.broker = broker;
      this.documentHandler = documentHandler;
   }

   void init() {
	   transaction = broker.newTransaction();
   }
   
   public int getTotalInsertCount() {
      return totalInsertCount;
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
      return backupMembersMap.get(prototypeName);
   }

   /**
    * Writes the schema version table with the value of the schema from the backup file.
    *
    * @param schemaVersionNr a <code>String</code> representing the value of the schema version.
    */
   void writeSchemaVersion(String schemaVersionNr) {
      //TODO - calin.pavel - this line should be changed when multiple databases will be supported.
      OpHibernateSource hibernateSource = (OpHibernateSource) OpSourceManager.getAllSources().iterator().next();
      hibernateSource.updateSchemaVersionNumber(Integer.valueOf(schemaVersionNr));
   }

   /**
    * Creates a new object instance.
    *
    * @param loc     a <code>Long</code> representing the object id.
    * @param system a <code>String</code> representing the system name of the object (if it exists).
    * @return a <code>OpObjectIfc</code> instance.
    */
   OpObjectIfc activateObject(long id, String system) {
      // Create new instance of the active prototype and reset its backup members
      
      OpPrototype t = activePrototype;
      activeLocator = new OpLocator(t, id);
//      activeLocators = new ArrayList<String>();
//      while (t != null) {
//         activeLocators.add(new OpLocator(t, id).toString());
//         t = t.getSuperType();
//      }
      activeSystem = system;
      activeObject = (OpObjectIfc) activePrototype.newInstance();
      activeMemberIndex = -1;
      return activeObject;
   }

   /**
    * @return the documentHandler
    */
   public XDocumentHandler getDocumentHandler() {
      return documentHandler;
   }

   /**
    * Persists the active object.
    *
    * @throws OpBackupException if a system object cannot be found.
    */
   void persistActiveObject() {
      executeActiveObjectPersist();
   }

   /**
    * Effectively persists the active object.
    */
   private void executeActiveObjectPersist() {
//      for (String loc: activeLocators) {
//         persistedObjectsMap.put(loc, activeObject);
//      }
	   broker.makePersistent(activeObject);

      Map<OpPrototype, OpObjectIfc> map = persistedObjectsMap.get(activeLocator.getID());
      if (map == null) {
         map = new HashMap<OpPrototype, OpObjectIfc>();
         persistedObjectsMap.put(activeLocator.getID(), map);
      }
      map.put(activeLocator.getPrototype(), activeObject);
//      persistedObjectsMap.put(activeLocator, activeObject);
//      logger.info("Persisting object with prototype:" + activePrototype.getName());
      insertCount++;
      objectsToAdd.add(activeObject);
      if (insertCount == MAX_INSERTS_PER_TRANSACTION) {
         commitRestoredObjects();
      }
   }

   /**
    * Activates a prototype with the given name.
    *
    * @param prototypeName a <code>String</code> the name of a prototype
    * @throws OpBackupException if the given prototype name is invalid.
    */
   void activatePrototype(String prototypeName)
        throws OpBackupException {
      if (prototypeName == null) {
         activeBackupMembers = null;
         activePrototype = null;
         activeSystem = null;
         activeObject = null;
         activeLocator = null;
      }
      else {
         activePrototype = OpTypeManager.getPrototype(prototypeName);
         activeBackupMembers = backupMembersMap.get(prototypeName);
         if (activeBackupMembers == null) {
            String newPrototypeName = PROTOTYPE_MAP.get(prototypeName);
            if (newPrototypeName != null) {
               activeBackupMembers = backupMembersMap.get(newPrototypeName);
            }
         }
         if (activePrototype == null || activeBackupMembers == null) {
            
            //we should be somewhat graceful. It may happen, that entities vanish...
            logger.error("Cannot activate prototype with name:" + prototypeName);
            //throw new OpBackupException("Cannot activate prototype with name:" + prototypeName);
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
    * @return a <code>OpObjectIfc</code> instance or <code>null</code> if the object with the given id hasn't been activated
    *         yet.
    */
   OpObjectIfc getRelationshipOwner(OpLocator locator) {
      OpObjectIfc opObject = getAssignableObject(locator);
      if (opObject == null) {
         opObject = this.getObjectFromDb(locator);
      }
      return opObject;
   }

   /**
    * @param locator
    * @pre
    * @post
    */
   private OpObjectIfc getAssignableObject(OpLocator locator) {
      Map<OpPrototype, OpObjectIfc> objs = persistedObjectsMap.get(locator.getID());
      if (objs != null) {
         Set<Entry<OpPrototype, OpObjectIfc>> set = objs.entrySet();
         for (Map.Entry<OpPrototype, OpObjectIfc> entry : set) {
            if (entry.getKey().isInstanceOf(locator.getPrototype())) {
               return entry.getValue();
            }
         }
      }
      return null;
   }

   /**
    * Creates a transaction which commits all in-memory changes.
    */
   void commitRestoredObjects() {
      if (objectsToAdd.size() > 0) {
         transaction.commit();
         long start = System.currentTimeMillis();
         logger.info("Inserting "+objectsToAdd.size()+" objects of type: "+objectsToAdd.get(0).getClass().getSimpleName()+" into db...");
         Iterator<Entry<Triple<OpLocator, OpLocator, OpBackupMember>, OpObjectIfc>> iter = delayedRelationsPerTransaction.entrySet().iterator();
         Entry<Triple<OpLocator, OpLocator, OpBackupMember>, OpObjectIfc> entry;
         while (iter.hasNext()) {
            entry = iter.next();
            Triple<OpLocator, OpLocator, OpBackupMember> triple = entry.getKey();
            OpObjectIfc destination = getRelationshipOwner(triple.getSecond());
            if (destination != null) {
            	logger.info("Will restore relationship from object "+triple.getFirst()+" towards object  "+triple.getSecond());
            	delayedRelations.add(new Triple<String, String, OpBackupMember>(entry.getValue().locator(), destination.locator(), triple.getThird()));
               iter.remove();
            }
         }
         logger.info("Objects persisted, lasted: "+((System.currentTimeMillis()-start)) +" millies, #Objects to fix: "+(delayedRelationsPerTransaction.size()+delayedRelations.size()));
         objectsToAdd.clear();
         totalInsertCount += insertCount;
         insertCount = 0;
         transaction = broker.newTransaction();
      }
      
      for (Map.Entry<Long, Map<OpPrototype, OpObjectIfc>> entry : persistedObjectsMap.entrySet()) {
         for (Map.Entry<OpPrototype, OpObjectIfc> map : entry.getValue().entrySet()) {
            backupLocatorToLocatorMap.put(new OpLocator(map.getKey(), entry.getKey()), new OpLocator(map.getValue()));
         }
      }

      persistedObjectsMap.clear();
      broker.clear();
   }
   
   /**
    * Retrieves an object from the database, using the back-up ID of the object before
    * it was inserted.
    *
    * @param activeId a <code>long</code> the back-up id of an object
    * @return an <code>OpObjectIfc</code> instance.
    */
   private OpObjectIfc getObjectFromDb(OpLocator loc) {
       OpPrototype pt = loc.getPrototype();
       OpLocator locator = backupLocatorToLocatorMap.get(loc);
       if (locator != null) {
    	   OpObjectIfc object = broker.getObject(locator);
    	   return object;
       }
       // try supertype
       OpPrototype superType = pt.getSuperType();
       while (superType != null) {
          OpLocator superLoc = backupLocatorToLocatorMap.get(new OpLocator(superType, loc.getID()));
          if (superLoc != null) {
             OpObjectIfc object = broker.getObject(superLoc);
             return object;
          }
          superType = superType.getSuperType();
       }

       // try subtypes
	   if (pt.getSubTypes() != null) {
		   for (OpPrototype pti : pt.getSubTypes()) {
			   OpObjectIfc ret = getObjectFromDbDown(new OpLocator(pti, loc.getID()));
			   if (ret != null) {
				   return ret;
			   }
		   }
	   }
       return null;
   }

   
   private OpObjectIfc getObjectFromDbDown(OpLocator loc) {
       OpPrototype pt = loc.getPrototype();
       OpLocator locator = backupLocatorToLocatorMap.get(loc);
       if (locator != null) {
    	   OpObjectIfc object = broker.getObject(locator);
    	   return object;
       }
	   if (pt.getSubTypes() != null) {
		   for (OpPrototype pti : pt.getSubTypes()) {
			   OpObjectIfc ret = getObjectFromDbDown(new OpLocator(pti, pt.getID()));
			   if (ret != null) {
				   return ret;
			   }
		   }
	   }
	   return null;
   }

   /**
    * @see onepoint.xml.XContext#reset()
    */
   @Override
   public void reset() {
      super.reset();

      this.commitRestoredObjects();

      backupMembersMap.clear();
      backupMembersMap = null;
      persistedObjectsMap.clear();
      persistedObjectsMap = null;
//      backupLocatorToLocatorMap.clear();
//      backupLocatorToLocatorMap = null;
      objectsToAdd.clear();
      objectsToAdd = null;
   }

   /**
    * @return
    * @pre
    * @post
    */
   OpProjectSession getSession() {
      return session;
   }

   /**
    * @param locator
    * @param id
    * @param backupMember 
    * @pre
    * @post
    */
   public void putRelationDelayed(OpObjectIfc object, OpLocator locator, OpBackupMember backupMember) {
      delayedRelationsPerTransaction.put(new Triple<OpLocator, OpLocator, OpBackupMember>(activeLocator, locator, backupMember), object);
   }

   public Iterator<Entry<Triple<OpLocator, OpLocator, OpBackupMember>, OpObjectIfc>> relationDelayedPerTransactionIterator() {
	   return delayedRelationsPerTransaction.entrySet().iterator();
   }
   
   public Iterator<Triple<String, String, OpBackupMember>> relationDelayedIterator() {
      return delayedRelations.iterator();
   }

   /**
    * @param nodeName
    * @param name
    * @return
    * @pre
    * @post
    */
   public OpPrototype getMappedRelation(String nodeName, String name) { 
      String value = RELATION_MAP.get(nodeName+"::"+name);
      if (value == null) {
         return null;
      }
//      String[] values = value.split("::");
//      if (values.length < 2) {
//         values = new String[] { nodeName, value };
//      }
      return OpTypeManager.getPrototype(value);
   }

   public void finish() {
   }
}

