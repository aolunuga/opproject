/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.OpProjectSession;
import onepoint.util.XEnvironmentManager;
import onepoint.xml.XDocumentWriter;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class that backs-up and restores the application repository.
 *
 * @author gmesaric
 * @author horia.chiorean
 */
public class OpBackupManager {

   /**
    * The current project version number.
    */
   public final static int CURRENT_VERSION_NUMBER = 1; // OPP v06.0

   /**
    * Constants that represent node and attribute names that will be written to the xml backup.
    */
   public final static String OPP_BACKUP = "opp-backup";
   public final static String VERSION = "version";
   public final static String SCHEMA_VERSION = "schema-version";

   public final static String PROTOTYPES = "prototypes";
   public final static String PROTOTYPE = "prototype";
   public final static String NAME = "name";
   public final static String FIELD = "field";
   public final static String TYPE = "type";
   public final static String RELATIONSHIP = "relationship";

   public final static String OBJECTS = "objects";
   public final static String O = "object";
   public final static String ID = "id";
   public final static String SYSTEM = "system"; // Indicates a system object
   public final static String P = "property";
   public final static String R = "reference";
   public final static String NULL = "null";
   public final static String TRUE = "true";
   public final static String FALSE = "false";

   /**
    * This class's logger
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpBackupManager.class);

   /**
    * The size of a page, when performing backup.
    */
   private static final int BACKUP_PAGE_SIZE = 1000;

   /**
    * The name of the directory where binary files (contents) are stored
    */
   private static final String BINARY_DIR_NAME_SUFFIX = "-files";

   /**
    * Date format used for importing/exporting date values.
    */
   final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'GMT'");
   final static SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'");

   /**
    * The map of prototypes (their load order is important).
    */
   private static Map<String, OpPrototype> prototypes = new LinkedHashMap<String, OpPrototype>();

   /**
    * A map of [systemObjectName, systemObjectQuery] used for retrieving system objects.
    */
   private static Map<String, String> systemObjectIdQueries = new HashMap<String, String>();

   /**
    * The class instance
    */
   private static OpBackupManager backupManager = null;

   /**
    * "/" string
    */
   private static final String SLASH_STRING = "/";

   /**
    * The path to the dir where binary files will be stored
    */
   private String binaryDirPath = null;

   /**
    * The name of the dir where binary files will be stored
    */
   private String binaryDirName = null;

   /**
    * There should be only 1 instance of this class.
    */
   private OpBackupManager() {
   }

   /**
    * Returns a backup manager instance.
    *
    * @return a <code>OpBackupManager</code> instance.
    */
   public static OpBackupManager getBackupManager() {
      if (backupManager == null) {
         backupManager = new OpBackupManager();
      }
      return backupManager;
   }

   /**
    * Adds a prototype to the map of prototypes.
    *
    * @param prototype a <code>OpPrototype</code> object.
    */
   public static void addPrototype(OpPrototype prototype) {
      logger.info("Backup manager registering: " + prototype.getName());
      prototypes.put(prototype.getName(), prototype);
   }

   /**
    * Checks whether the manager has registed the given prototype or not.
    *
    * @param prototype a <code>OpPrototype</code> representing the prototype to register.
    * @return a <code>boolean</code> indicating whether the prototype has been registered or not.
    */
   public static boolean hasRegistered(OpPrototype prototype) {
      return prototypes.get(prototype.getName()) != null;
   }

   /**
    * Adds a system object name together with a query that can obtain the system object to the backup manager.
    *
    * @param name  a <code>String</code> representing the name of the system object.
    * @param query a <code>String</code> representing a query which together with the name obtains the sytem object.
    */
   public static void addSystemObjectIDQuery(String name, String query) {
      systemObjectIdQueries.put(name, query);
   }

   /**
    * Returns a query for the given system object name.
    *
    * @param name a <code>String</code> representing the name of the system object.
    * @return a <code>String</code> representing a query for the system object's name.
    */
   public static String getSystemObjectIDQuery(String name) {
      return (String) systemObjectIdQueries.get(name);
   }

   /**
    * Creates a map of [object_name, object_id] for all the system objects that were registered by the manager.
    *
    * @param broker a <code>OpBroker</code> that executes the query agains a db.
    * @return a <code>Map</code> of [<code>String</code>, <code>Long</code>] pairs representing [name, id] pairs.
    */
   public static Map querySystemObjectIdMap(OpBroker broker) {
      Map<Long, String> systemObjectIdMap = new HashMap<Long, String>();
      for (String name : systemObjectIdQueries.keySet()) {
         String queryString = (String) systemObjectIdQueries.get(name);
         logger.info("Query:" + queryString);

         OpQuery query = broker.newQuery(queryString);
         Iterator it = broker.iterate(query);
         if (!it.hasNext()) {
            logger.error("System object id not found after query:" + queryString);
            continue;
         }
         Long id = (Long) it.next();
         systemObjectIdMap.put(id, name);
      }
      return systemObjectIdMap;
   }

   /**
    * Return a backup member for the given field.
    *
    * @param field a <code>OpField</code> instance, representing a field.
    * @return a <code>XBackupMemeber</code> instance.
    */
   private OpBackupMember getMemberForField(OpField field) {
      OpBackupMember backupMember = new OpBackupMember();
      backupMember.name = field.getName();
      backupMember.typeId = field.getTypeID();
      backupMember.relationship = false;
      backupMember.ordered = field.getOrdered();
      backupMember.recursive = false;

      return backupMember;
   }

   /**
    * Return a backup member for the given field.
    *
    * @param relationship a <code>XRelationShip</code> instance, representing a relationship.
    * @return a <code>XBackupMemeber</code> instance.
    */
   private OpBackupMember getMemberForRelationShip(OpRelationship relationship) {
      OpBackupMember backupMember = new OpBackupMember();
      backupMember.name = relationship.getName();
      backupMember.typeId = relationship.getTypeID();
      backupMember.relationship = true;
      backupMember.ordered = false;
      backupMember.recursive = relationship.getRecursive();
      OpRelationship backRelationShip = relationship.getBackRelationship();
      backupMember.backRelationshipName = backRelationShip != null ? backRelationShip.getName() : null;

      return backupMember;
   }

   /**
    * Exports all the prototypes this manager has loaded on the given document writer.
    *
    * @param writer a <code>XDocumentWriter</code> that writes out xml files.
    * @return a <code>List</code> of <code>BackupMember[]</code> representing all the members which have been backed up
    *         for each prototype.
    * @throws IOException if any of the prototypes could not be exported.
    */
   private List exportPrototypes(XDocumentWriter writer)
        throws IOException {
      List<OpBackupMember[]> allMembers = new ArrayList<OpBackupMember[]>();

      writer.writeStartElement(PROTOTYPES, null, false);

      Map<String, String> attributes = new HashMap<String, String>();
      Iterator it = prototypes.values().iterator();
      while (it.hasNext()) {
         OpPrototype prototype = (OpPrototype) it.next();

         attributes.put(NAME, prototype.getName());
         writer.writeStartElement(PROTOTYPE, attributes, false);
         attributes.clear();

         // Create Java array for caching member information
         OpBackupMember[] backupMembers = new OpBackupMember[prototype.getSize()];

         Iterator members = prototype.getMembers();
         int i = 0;
         while (members.hasNext()) {
            OpMember member = (OpMember) members.next();

            // Cache accessor method (Note that we assume that persistent member names start with an upper-case letter)
            Method accessor = null;
            try {
               accessor = prototype.getInstanceClass().getMethod("get" + member.getName());
            }
            catch (NoSuchMethodException e) {
               logger.error("No accessor method for " + prototype.getName() + "." + member.getName());
               continue;
            }

            if (member instanceof OpField) {
               OpBackupMember backupMember = this.getMemberForField((OpField) member);
               String typeString = OpBackupTypeManager.getTypeString(member.getTypeID());
               if (typeString == null) {
                  backupMembers[i] = null;
               }
               else {
                  backupMember.accessor = accessor;
                  backupMembers[i] = backupMember;
               }

               attributes.put(NAME, member.getName());
               attributes.put(TYPE, typeString);
               writer.writeStartElement(FIELD, attributes, true);
               attributes.clear();
            }
            else if (member instanceof OpRelationship && !((OpRelationship) member).getInverse()) {
               // Attention: Only non-inverse relationships are backuped
               // Note: We have to check for *all* prototypes, because of abstract prototypes
               OpPrototype targetPrototype = OpTypeManager.getPrototypeByID(member.getTypeID());
               if (targetPrototype == null) {
                  logger.error("Unknown prototype with ID " + member.getTypeID());
                  backupMembers[i] = null;
                  continue;
               }
               attributes.put(NAME, member.getName());
               attributes.put(TYPE, targetPrototype.getName());
               writer.writeStartElement(RELATIONSHIP, attributes, true);
               attributes.clear();

               OpBackupMember backupMember = this.getMemberForRelationShip((OpRelationship) member);
               backupMember.accessor = accessor;
               backupMembers[i] = backupMember;
            }
            else {
               backupMembers[i] = null;
            }
            i++;
         }

         allMembers.add(backupMembers);
         writer.writeEndElement(PROTOTYPE);
      }
      writer.writeEndElement(PROTOTYPES);

      return allMembers;
   }

   /**
    * Writes the given field (backup member) to the given writer, including its value.
    *
    * @param member a <code>OpBackupMember</code> representing a field of an entity.
    * @param writer a <code>XDocumentWriter</code> that writes out the field with its value.
    * @param value  a <code>Object</code> representing the value of the field.
    * @throws IOException if the field cannot be written.
    */
   private void writeFieldMember(OpObject object, OpBackupMember member, XDocumentWriter writer, Object value)
        throws IOException {
      writer.writeStartElement(P, null, false);
      if (value == null) {
         writer.writeContent(NULL);
      }
      else {
         switch (member.typeId) {
            case OpType.BOOLEAN:
               if (((Boolean) value).booleanValue()) {
                  writer.writeContent(TRUE);
               }
               else {
                  writer.writeContent(FALSE);
               }
               break;
            case OpType.INTEGER:
               writer.writeContent(((Integer) value).toString());
               break;
            case OpType.LONG:
               writer.writeContent(((Long) value).toString());
               break;
            case OpType.STRING:
               writer.writeContent(new StringBuffer("<![CDATA[").append((String) value).append("]]>")
                    .toString());
               break;
            case OpType.TEXT:
               writer.writeContent(new StringBuffer("<![CDATA[").append((String) value).append("]]>")
                    .toString());
               break;
            case OpType.DATE:
               writer.writeContent(DATE_FORMAT.format((Date) value));
               break;
            case OpType.CONTENT:
               // Write binary content to external file and file name as element content
               if (value == null) {
                  writer.writeContent(NULL);
               }
               else {
                  writer.writeContent(writeBinaryFile(object.getID(), member.name, (byte[]) value));
               }
               break;
            case OpType.BYTE:
               writer.writeContent(((Byte) value).toString());
               break;
            case OpType.DOUBLE:
               writer.writeContent(((Double) value).toString());
               break;
            case OpType.TIMESTAMP:
               writer.writeContent(TIMESTAMP_FORMAT.format((Timestamp) value));
               break;
            default:
               logger.error("ERROR: Unsupported type ID " + member.typeId + " for " + object.getPrototype().getName()
                    + "." + member.name);
         }
      }
      writer.writeEndElement(P);
   }

   /**
    * Writes the value of a relationship member.
    *
    * @param writer a <code>XDocumentWriter</code> that will write the output.
    * @param value  a <code>Object</code> representing the value of the relation ship.
    */
   private void writeRelationshipMember(XDocumentWriter writer, Object value)
        throws IOException {
      writer.writeStartElement(R, null, false);

      if (value == null) {
         writer.writeContent(NULL);
      }
      else {
         writer.writeContent(String.valueOf(((OpObject) value).getID()));
      }
      writer.writeEndElement(R);
   }

   /**
    * @param orderedBy   a <code>String</code> representing the name of the member field after which to order by.
    * @param recursiveBy a <code>String</code> representing the name of the member relationship which is recursive.
    * @param objects     a <code>List</code> of <code>OpObject</code> representing child objects in case of a recursive backup.
    * @see OpBackupManager#exportObjects(onepoint.project.OpProjectSession,onepoint.xml.XDocumentWriter,String,OpBackupMember[],java.util.Map)
    */
   private void exportSubObjects(OpProjectSession session, XDocumentWriter writer, String prototypeName,
        OpBackupMember[] members, String orderedBy, String recursiveBy, List objects, Map systemIdMap)
        throws IOException {
      int pageSize = BACKUP_PAGE_SIZE;
      int startIndex = 0;

      OpBroker broker = session.newBroker();
      Iterator result = null;
      if (objects != null) {
         result = objects.iterator();
      }
      else {
         int count = getObjectsToBackupCount(prototypeName, recursiveBy, broker);
         logger.info("Backing up " + count + " " + prototypeName);
         if (count > pageSize) {
            while (startIndex < count) {
               logger.info("Backing up objects between " + startIndex + " and " + (startIndex + BACKUP_PAGE_SIZE));
               OpBroker pagingBroker = session.newBroker();
               result = getObjectsToBackup(prototypeName, recursiveBy, orderedBy, pagingBroker, startIndex, BACKUP_PAGE_SIZE);
               startIndex += pageSize;
               if (startIndex >= count) {
                  break;
               }
               if (startIndex + pageSize > count) {
                  pageSize = count - startIndex;
               }
               //export each object
               List childObjects = exportIteratedObjects(result, systemIdMap, writer, members, recursiveBy, broker, prototypeName);
               if ((recursiveBy != null) && (childObjects.size() > 0)) {
                  exportSubObjects(session, writer, prototypeName, members, orderedBy, recursiveBy, childObjects, systemIdMap);
               }
               pagingBroker.close();
            }
            return;
         }
         else {
            result = getObjectsToBackup(prototypeName, recursiveBy, orderedBy, broker, 0, count);
         }
      }

      //export each object
      List childObjects = exportIteratedObjects(result, systemIdMap, writer, members, recursiveBy, broker, prototypeName);

      // Check for next recursion
      if ((recursiveBy != null) && (childObjects.size() > 0)) {
         exportSubObjects(session, writer, prototypeName, members, orderedBy, recursiveBy, childObjects, systemIdMap);
      }
      broker.close();
   }

   private List exportIteratedObjects(Iterator result, Map systemIdMap, XDocumentWriter writer, OpBackupMember[] members,
        String recursiveBy, OpBroker broker, String prototypeName)
        throws IOException {
      List childObjects = (recursiveBy != null) ? new ArrayList() : null;
      try {
         Map<String, String> attributes = new HashMap<String, String>();
         while (result.hasNext()) {
            OpObject object = (OpObject) result.next();
            //object id
            Long id = new Long(object.getID());
            //check whether the object is a system object
            attributes.put(ID, String.valueOf(object.getID()));
            String systemObjectName = (String) systemIdMap.get(id);
            if (systemObjectName != null) {
               attributes.put(SYSTEM, systemObjectName);
            }
            writer.writeStartElement(O, attributes, false);
            attributes.clear();

            // Iterate and call accessors to write field and relationship values
            OpBackupMember member = null;
            for (int i = 0; i < members.length; i++) {
               // Null-members indicate a back-relationship (which are ignored) or a field with unknown type
               member = members[i];
               if (member == null) {
                  continue;
               }

               Object value = member.accessor.invoke(object);
               if (member.relationship) {
                  this.writeRelationshipMember(writer, value);
                  if (recursiveBy != null && recursiveBy.equals(member.name)) {
                     try {
                        Method m = object.getClass().getMethod("get" + member.backRelationshipName);
                        Object backReturnValue = m.invoke(object);
                        if (backReturnValue != null && (backReturnValue instanceof Collection)) {
                           childObjects.addAll((Collection) backReturnValue);
                        }
                     }
                     catch (NoSuchMethodException e) {
                        logger.error("Cannot invoke back-relationship method " + member.backRelationshipName);
                     }
                  }
               }
               else {
                  this.writeFieldMember(object, member, writer, value);
               }
            }
            writer.writeEndElement(O);
         }
      }
      catch (IllegalAccessException e) {
         logger.error(e);
      }
      catch (InvocationTargetException e) {
         logger.error(e);
      }
      return childObjects;
   }

   /**
    * Gets a list of objects to back-up.
    *
    * @param prototypeName a <code>String</code> representing the name of a prototype.
    * @param recursiveBy   a <code>String</code> indicating the name of the recursive relationship.
    * @param orderedBy     a <code>String</code> representing the name of the field after which to order the objects.
    * @param broker        a <code>OpBroker</code> used for performing business operations.
    * @return an <code>Iterator</code> over <code>OpObjects</code>.
    */
   private Iterator getObjectsToBackup(String prototypeName, String recursiveBy, String orderedBy, OpBroker broker,
        int startIndex, int count) {
      StringBuffer queryBuffer = new StringBuffer("select xobject from ");
      queryBuffer.append(prototypeName);
      queryBuffer.append(" as xobject");

      // Handle recursive export (query string)
      if (recursiveBy != null) {
         queryBuffer.append(" where xobject.");
         queryBuffer.append(recursiveBy);
         // Retrieve root level
         queryBuffer.append(" is null");
      }

      queryBuffer.append(" order by xobject.");
      // Handle ordered export
      if (orderedBy != null) {
         queryBuffer.append(orderedBy);
      }
      else {
         queryBuffer.append("ID");
      }

      logger.debug("***QUERY: " + queryBuffer);

      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setFirstResult(startIndex);
      query.setMaxResults(count);
      return broker.list(query).iterator();
   }

   /**
    * Gets the number of objects to back-up from the db.
    *
    * @param prototypeName a <code>String</code> representing the name of a prototype.
    * @param broker        a <code>OpBroker</code> used for performing business operations.
    * @return an <code>Iterator</code> over <code>OpObjects</code>.
    */
   private int getObjectsToBackupCount(String prototypeName, String recursiveBy, OpBroker broker) {
      StringBuffer queryBuffer = new StringBuffer("select count(xobject.ID) from ");
      queryBuffer.append(prototypeName);
      queryBuffer.append(" as xobject");

      // Handle recursive export (query string)
      if (recursiveBy != null) {
         queryBuffer.append(" where xobject.");
         queryBuffer.append(recursiveBy);
         // Retrieve root level
         queryBuffer.append(" is null");
      }

      logger.debug("***QUERY: " + queryBuffer);
      OpQuery query = broker.newQuery(queryBuffer.toString());

      return ((Number) broker.iterate(query).next()).intValue();
   }

   /**
    * For the given proptotype and members of the prototype exports all current db values.
    *
    * @param session       a <code>OpProjectSession</code> used for creating brokers that perform db operations.
    * @param writer        a <code>XDocumentWriter</code> that is used to output xml.
    * @param prototypeName a <code>String</code> representing the name of the prototype.
    * @param members       a <code>OpBackupMember[]</code> representing the members to be exported for the given prototype.
    * @param systemIdMap   a <code>Map</code> of <code>[String,Long]</code> representing the system objects.
    * @throws IOException if anything fails.
    */
   private void exportObjects(OpProjectSession session, XDocumentWriter writer, String prototypeName,
        OpBackupMember[] members, Map systemIdMap)
        throws IOException {
      logger.info("Backing up ******** " + prototypeName + " ********");
      Map<String, String> attributes = new HashMap<String, String>();
      attributes.put(TYPE, prototypeName);
      writer.writeStartElement(OBJECTS, attributes, false);
      attributes.clear();

      // Check for first occurence of "ordered" field
      String orderedBy = null;
      String recursiveBy = null;
      for (int i = 0; i < members.length; i++) {
         if (members[i] != null) {
            logger.info("Backing up member " + members[i].name);
            if (!members[i].relationship && members[i].ordered && orderedBy == null) {
               orderedBy = members[i].name;
            }
            else if (members[i].relationship && members[i].recursive && recursiveBy == null) {
               recursiveBy = members[i].name;
            }

            if (recursiveBy != null && orderedBy != null) {
               break;
            }
         }
      }

      exportSubObjects(session, writer, prototypeName, members, orderedBy, recursiveBy, null, systemIdMap);
      writer.writeEndElement(OBJECTS);
   }

   /**
    * Backs up an existent repository under an xml file with the given path.
    *
    * @param session an <code>OpProjectSession</code> representing an application session.
    * @param path    a <code>String</code> representing the path to the file where the backup should be generated.
    * @throws IOException              if the operation fails and the backup cannot be written.
    * @throws IllegalArgumentException if the given path is invalid.
    */
   public final void backupRepository(OpProjectSession session, String path)
        throws IOException {
      if (!isPathAFile(path, false)) {
         throw new IllegalArgumentException("The given path to backup the repository to is not a file");
      }
      String workingDir = getParentDirectory(path);
      String fileName = new File(path).getName();
      fileName = fileName.substring(0, fileName.indexOf('.'));
      this.binaryDirName = fileName + BINARY_DIR_NAME_SUFFIX;
      this.binaryDirPath = workingDir + SLASH_STRING + this.binaryDirName;
      this.backupRepository(session, new BufferedOutputStream(new FileOutputStream(path, false)));
   }

   /**
    * Checks whether the given path points to an existent file or not.
    *
    * @param path        a <code>String</code> representing a path.
    * @param shouldExist a <code>boolean</code> indicating whether an existence check should be done or not.
    * @return <code>true</code> if the given path points to an existent file.
    */
   private boolean isPathAFile(String path, boolean shouldExist) {
      String formattedPath = XEnvironmentManager.convertPathToSlash(path);
      File filePath = new File(formattedPath);
      if (shouldExist && !filePath.exists()) {
         logger.info("Given path should be an existing file, but isn't");
         return false;
      }
      if (filePath.isDirectory()) {
         logger.info("Given path is a directory");
         return false;
      }
      return true;
   }

   /**
    * From the given path to a file, extracts the parent directory of the file.
    *
    * @param filePath a <code>String</code> representing the path to an existent file.
    * @return a <code>String</code> representing the parent directory of the given file.
    * @throws IllegalArgumentException if the given string does not point to an exiting file.
    */
   private String getParentDirectory(String filePath) {
      filePath = XEnvironmentManager.convertPathToSlash(filePath);
      File file = new File(filePath);
      if (!file.exists() || file.isDirectory()) {
         throw new IllegalArgumentException("The given path argument does not point to an existent file");
      }
      return XEnvironmentManager.convertPathToSlash(file.getParent());
   }

   /**
    * @see OpBackupManager#backupRepository(onepoint.project.OpProjectSession,String)
    */
   private void backupRepository(OpProjectSession session, OutputStream output)
        throws IOException {
      long startTime = System.currentTimeMillis();
      logger.info("Starting repository backup....");
      XDocumentWriter writer = new XDocumentWriter(output);

      // Write XML header
      writer.writeHeader1_0();
      Map<String, String> attributes = new HashMap<String, String>();

      // Write root elements
      attributes.put(VERSION, String.valueOf(CURRENT_VERSION_NUMBER));
      attributes.put(SCHEMA_VERSION, String.valueOf(OpHibernateSource.SCHEMA_VERSION));
      writer.writeStartElement(OPP_BACKUP, attributes, false);
      attributes.clear();

      // Export prototype order
      List allMembers = exportPrototypes(writer);

      // Query system object ID ids and names before exporting objects
      OpBroker broker = session.newBroker();
      Map systemIdMap = querySystemObjectIdMap(broker);
      broker.close();

      int memberIndex = 0;
      for (OpPrototype prototype : prototypes.values()) {
         exportObjects(session, writer, prototype.getName(), (OpBackupMember[]) allMembers.get(memberIndex), systemIdMap);
         memberIndex++;
      }

      writer.writeEndElement(OPP_BACKUP);

      // Important: Flushes and closes output stream
      writer.close();
      long elapsedTimeSecs = (System.currentTimeMillis() - startTime) / 1000;
      logger.info("Repository backup completed in " + elapsedTimeSecs + " seconds");
   }

   /**
    * Restores a repository from the given path.
    *
    * @param session a <code>OpProjectSession</code> representing an application session.
    * @param path    a <code>String</code> representing a path to the backup file.
    * @throws IOException              if the repository cannot be restored.
    * @throws IllegalArgumentException if the given path does not point to an existing file.
    */
   public final void restoreRepository(OpProjectSession session, String path)
        throws IOException {
      if (!isPathAFile(path, true)) {
         throw new IllegalArgumentException("The given path does not point to an exiting file");
      }
      String parentDirectory = getParentDirectory(path);
      restoreRepository(session, new BufferedInputStream(new FileInputStream(path)), parentDirectory);
   }

   /**
    * @see OpBackupManager#restoreRepository(onepoint.project.OpProjectSession,String)
    */
   private void restoreRepository(OpProjectSession session, InputStream input, String workingDirectory) {
      long start = System.currentTimeMillis();
      logger.info("Restoring repository...");
      // Extra path is required for restoring binary content stored in separate files
      OpBroker broker = session.newBroker();
      OpBackupLoader backupLoader = new OpBackupLoader();
      backupLoader.loadBackup(broker, input, workingDirectory);
      broker.close();
      long elapsedTimeSecs = (System.currentTimeMillis() - start) / 1000;
      logger.info("Repository restore completed in " + elapsedTimeSecs + " seconds");
   }


   /**
    * Generates a string that represents a path to the file that will be written with the binary content.
    *
    * @param id               a <code>long</code> representing the id of the object that has the content.
    * @param backupMemberName a <code>String</code> representing the name of the object's property which has the content.
    * @return a <code>String</code> representing the name of the binary file.
    */
   private String binaryFilePath(long id, String backupMemberName) {
      StringBuffer pathBuffer = new StringBuffer("/object-");
      pathBuffer.append(id);
      pathBuffer.append('-');
      pathBuffer.append(System.currentTimeMillis());
      pathBuffer.append('-');
      pathBuffer.append(backupMemberName);
      pathBuffer.append(".bin");
      return pathBuffer.toString();
   }

   /**
    * Writes a binary file under the given path, in a special directory.
    *
    * @param id               a <code>long</code> representing the if of the object which contains the written content.
    * @param backupMemberName a <code>String</code> representing the name of the backup member which has the content.
    * @param content          a <code>byte[]</code> representing the actual content.
    * @return the path to the newly written binary file.
    */
   private String writeBinaryFile(long id, String backupMemberName, byte[] content) {
      File directory = new File(binaryDirPath);
      if (!directory.exists()) {
         directory.mkdir();
      }
      // Write binary data to file
      String fileName = binaryFilePath(id, backupMemberName);
      try {
         FileOutputStream fileOutput = new FileOutputStream(binaryDirPath + fileName);
         fileOutput.write(content);
         fileOutput.flush();
         fileOutput.close();
      }
      catch (IOException e) {
         logger.error("ERROR: An I/O exception occured when trying to write binary file: ", e);
      }
      return SLASH_STRING + this.binaryDirName + fileName;
   }

   /**
    * Reads the contents of a binary file from the given path.
    *
    * @param path a <code>String</code> representing the path to a binary file.
    * @return a <code>byte[]</code> with the file's content.
    */
   static byte[] readBinaryFile(String path) {
      try {
         FileInputStream fileInput = new FileInputStream(path);
         ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
         byte[] buffer = new byte[2048];
         int count = 0;
         while (count != -1) {
            count = fileInput.read(buffer);
            if (count > 0) {
               byteOutput.write(buffer, 0, count);
            }
         }
         fileInput.close();
         return byteOutput.toByteArray();
      }
      catch (IOException e) {
         logger.error("An I/O exception occured when trying to read binary file" + path, e);
         return null;
      }
   }
}
