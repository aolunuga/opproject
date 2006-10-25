/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
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
   private static final XLog logger = XLogFactory.getLogger(OpBackupManager.class, true);

   /**
    * Date format used for importing/exporting date values.
    */
   final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'GMT'");
   final static SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'");

   /**
    * The map of prototypes (their load order is important).
    */
   private static Map prototypes = new LinkedHashMap();

   /**
    * A map of [systemObjectName, systemObjectQuery] used for retrieving system objects.
    */
   private static Map systemObjectIdQueries = new HashMap();

   /**
    * The class instance
    */
   private static OpBackupManager backupManager = null;

   /**
    * The path to the dir where binary files will be stored (is normally relative to the export path)
    */
   private static String binaryDirPath = "/binary-files";

   /**
    * There should be only 1 instance of this class.
    */
   private OpBackupManager() {
   }

   /**
    * Returns a backup manager instance.
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
      HashMap systemObjectIdMap = new HashMap();
      Iterator names = systemObjectIdQueries.keySet().iterator();
      while (names.hasNext()) {
         String name = (String) names.next();
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
      List allMembers = new ArrayList();

      writer.writeStartElement(PROTOTYPES, null, false);

      HashMap attributes = new HashMap();
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
               accessor = prototype.getInstanceClass().getMethod("get" + member.getName(), null);
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
    * @param member a <code>OpBackupMember</code> representing a field of an entity.
    * @param writer a <code>XDocumentWriter</code> that writes out the field with its value.
    * @param value a <code>Object</code> representing the value of the field.
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
    * @param value a <code>Object</code> representing the value of the relation ship.
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
    * @see OpBackupManager#exportObjects(onepoint.persistence.OpBroker, onepoint.xml.XDocumentWriter, String, OpBackupMember[], java.util.Map)
    *
    * @param orderedBy a <code>String</code> representing the name of the member field after which to order by.
    * @param recursiveBy a <code>String</code> representing the name of the member relationship which is recursive.
    * @param superIds a <code>List</code> of <code>Long</code> representing super ids in case of a recursive backup.
    */
   private void exportSubObjects(OpBroker broker, XDocumentWriter writer, String prototypeName,
        OpBackupMember[] members, String orderedBy, String recursiveBy, List superIds, Map systemIdMap)
        throws IOException {
      // Here we could do additional filtering for partial backups

      StringBuffer queryBuffer = new StringBuffer("select xobject from ");
      queryBuffer.append(prototypeName);
      queryBuffer.append(" as xobject");

      // Handle recursive export (query string)
      if (recursiveBy != null) {
         queryBuffer.append(" where xobject.");
         queryBuffer.append(recursiveBy);
         if (superIds == null) {
            // Retrieve root level
            queryBuffer.append(" is null");
         }
         else {
            // Retrieve sub-objects of specified super-IDs
            queryBuffer.append(".ID in (:superIds)");
         }
      }

      // Handle ordered export
      if (orderedBy != null) {
         queryBuffer.append(" order by xobject.");
         queryBuffer.append(orderedBy);
      }

      logger.debug("***QUERY: " + queryBuffer);
      OpQuery query = broker.newQuery(queryBuffer.toString());

      // Handle recursive export (query parameters)
      if ((recursiveBy != null) && (superIds != null)) {
         logger.debug("SUPER-IDS " + superIds);
         query.setCollection("superIds", superIds);
      }

      Iterator result = broker.iterate(query);

      List ids = (recursiveBy != null) ? new ArrayList() : null;
      try {
         HashMap attributes = new HashMap();
         int j = 0;
         while (result.hasNext()) {
            OpObject object = (OpObject) result.next();
            //object id
            Long id = new Long(object.getID());
            if (recursiveBy != null) {
               ids.add(id);
            }
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
            for (j = 0; j < members.length; j++) {
               // Null-members indicate a back-relationship (which are ignored) or a field with unknown type
               member = members[j];
               if (member == null) {
                  continue;
               }

               Object value = member.accessor.invoke(object, null);
               if (member.relationship) {
                  this.writeRelationshipMember(writer, value);
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

      // Check for next recursion
      if ((recursiveBy != null) && (ids.size() > 0)) {
         exportSubObjects(broker, writer, prototypeName, members, orderedBy, recursiveBy, ids, systemIdMap);
      }
   }

   /**
    * For the given proptotype and members of the prototype exports all current db values.
    *
    * @param broker a <code>OpBroker</code> used for performing db operations.
    * @param writer a <code>XDocumentWriter</code> that is used to output xml.
    * @param prototypeName a <code>String</code> representing the name of the prototype.
    * @param members a <code>OpBackupMember[]</code> representing the members to be exported for the given prototype.
    * @param systemIdMap a <code>Map</code> of <code>[String,Long]</code> representing the system objects.
    * @throws IOException if anything fails.
    */
   private void exportObjects(OpBroker broker, XDocumentWriter writer, String prototypeName,
        OpBackupMember[] members, Map systemIdMap)
        throws IOException {
      logger.info("Backing up " + prototypeName);

      HashMap attributes = new HashMap();
      attributes.put(TYPE, prototypeName);
      writer.writeStartElement(OBJECTS, attributes, false);
      attributes.clear();

      // Check for first occurence of "ordered" field
      String orderedBy = null;
      for (int i = 0; i < members.length; i++) {
         if (members[i] != null) {
            logger.info("Backing up member " + members[i].name);
            if (!members[i].relationship && members[i].ordered) {
               orderedBy = members[i].name;
               break;
            }
         }
      }

      // Check for first occurence of "recursive" field
      String recursiveBy = null;
      for (int i = 0; i < members.length; i++) {
         if ((members[i] != null) && members[i].relationship && members[i].recursive) {
            recursiveBy = members[i].name;
            break;
         }
      }

      exportSubObjects(broker, writer, prototypeName, members, orderedBy, recursiveBy, null, systemIdMap);
      writer.writeEndElement(OBJECTS);
   }


   /**
    * Backs up an existent repository under an xml file with the given path.
    * @param session an <code>OpProjectSession</code> representing an application session.
    * @param path a <code>String</code> representing the path to the file where the backup should be generated.
    *
    * @throws IOException if the operation fails and the backup cannot be written.
    * @throws IllegalArgumentException if the given path is invalid.
    */
   public final void backupRepository(OpProjectSession session, String path)
        throws IOException {
      this.initFilePath(path);
      this.backupRepository(session, new BufferedOutputStream(new FileOutputStream(path, false)));
   }

   /**
    * Initializes the path to the binary files directory, from the given backup path.
    * @param path a <code>String</code> representing the backup path.
    * @throws IllegalArgumentException if the given path is invalid.
    */
   private void initFilePath(String path) {
      String formattedPath = XEnvironmentManager.convertPathToSlash(path);
      File filePath = new File(formattedPath);
      if (filePath.exists() && filePath.isDirectory()) {
         logger.info("Given backup path is a directory");
         throw new IllegalArgumentException("The path for the backup should point to a file");
      }
      int lastSlashIndex = formattedPath.lastIndexOf("/");
      if (lastSlashIndex != -1 && !filePath.isDirectory()) {
         binaryDirPath = formattedPath.substring(0, lastSlashIndex) + binaryDirPath;
      }
      else if (filePath.isDirectory()) {
         binaryDirPath = formattedPath + binaryDirPath;
      }
      else {
         binaryDirPath = "." + binaryDirPath;
      }
   }

   /**
    * @see OpBackupManager#backupRepository(onepoint.project.OpProjectSession, String)
    */
   public void backupRepository(OpProjectSession session, OutputStream output)
        throws IOException {
      XDocumentWriter writer = new XDocumentWriter(output);

      // Write XML header
      writer.writeHeader1_0();
      HashMap attributes = new HashMap();

      // Write root elements (pilot release uses version="0")
      attributes.put(VERSION, new StringBuffer().append(CURRENT_VERSION_NUMBER).toString());
      writer.writeStartElement(OPP_BACKUP, attributes, false);
      attributes.clear();

      OpBroker broker = session.newBroker();

      // Export prototype order
      List allMembers = exportPrototypes(writer);

      // Query system object ID ids and names before exporting objects
      Map systemIdMap = querySystemObjectIdMap(broker);

      int memberIndex = 0;
      Iterator it = prototypes.values().iterator();
      while (it.hasNext()) {
         OpPrototype prototype = (OpPrototype) it.next();
         exportObjects(broker, writer, prototype.getName(), (OpBackupMember[]) allMembers.get(memberIndex), systemIdMap);
         memberIndex ++;
      }
      broker.close();

      writer.writeEndElement(OPP_BACKUP);

      // Important: Flushes and closes output stream
      writer.close();
   }

   /**
    * Restores a repository from the given path.
    *
    * @param session a <code>OpProjectSession</code> representing an application session.
    * @param path a <code>String</code> representing a path to the backup file.
    * @throws IOException if the repository cannot be restored.
    */
   public final void restoreRepository(OpProjectSession session, String path)
        throws IOException {
      restoreRepository(session, new BufferedInputStream(new FileInputStream(path)));
   }

   /**
    * @see OpBackupManager#restoreRepository(onepoint.project.OpProjectSession, String)
    */
   public void restoreRepository(OpProjectSession session, InputStream input)
        throws IOException {
      // Extra path is required for restoring binary content stored in separate files
      OpBroker broker = session.newBroker();
      OpBackupLoader backupLoader = new OpBackupLoader();
      backupLoader.loadBackup(broker, input);
      broker.close();
   }


   /**
    * Generates a string that represents a path to the file that will be written with the binary content.
    * @param id a <code>long</code> representing the id of the object that has the content.
    * @param backupMemberName a <code>String</code> representing the name of the object's property which has the content.
    * @return a <code>String</code> representing the name of the binary file.
    */
   private String binaryFilePath(long id, String backupMemberName) {
      StringBuffer pathBuffer = new StringBuffer(binaryDirPath);
      pathBuffer.append("/object-");
      pathBuffer.append(id);
      pathBuffer.append('-');
      pathBuffer.append(backupMemberName);
      pathBuffer.append(".bin");
      return pathBuffer.toString();
   }

   /**
    * Writes a binary file under the given path, in a special directory.
    * @param id a <code>long</code> representing the if of the object which contains the written content.
    * @param backupMemberName a <code>String</code> representing the name of the backup member which has the content.
    * @param content a <code>byte[]</code> representing the actual content.
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
         FileOutputStream fileOutput = new FileOutputStream(fileName);
         fileOutput.write(content);
         fileOutput.flush();
         fileOutput.close();
      }
      catch (IOException e) {
         logger.error("ERROR: An I/O exception occured when trying to write binary file: ", e);
      }
      return fileName;
   }

   /**
    * Reads the contents of a binary file from the given path.
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
