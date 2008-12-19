/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpField;
import onepoint.persistence.OpMember;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpPersistenceManager;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpRelationship;
import onepoint.persistence.OpSource;
import onepoint.persistence.OpSourceManager;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.OpType;
import onepoint.persistence.OpTypeManager;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.custom_attribute.OpCustomValuePage;
import onepoint.project.modules.custom_attribute.OpCustomizable;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpGraph;
import onepoint.project.util.OpProjectCalendar;
import onepoint.service.XSizeInputStream;
import onepoint.service.server.XServiceException;
import onepoint.util.XEnvironmentManager;
import onepoint.util.XIOHelper;
import onepoint.xml.XDocumentWriter;

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
	private static final XLog logger = XLogFactory.getLogger(OpBackupManager.class);

	/**
	 * The size of a page, when performing backup.
	 */
	private static final int BACKUP_PAGE_SIZE = 1000;

	/**
	 * The maximum number of operations done per a transaction.
	 */
	public final static int MAX_DELETES_PER_TRANSACTION = 300;

	/**
	 * The name of the directory where binary files (contents) are stored
	 */
	private static final String BINARY_DIR_NAME_SUFFIX = "-files";

	/**
	 * Date format used for importing/exporting date values.
	 */
	static SimpleDateFormat DATE_FORMAT = null;
	static SimpleDateFormat TIMESTAMP_FORMAT = null;

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
	
	// maybe define dependency types???
	private final static Integer DEFAULT_EDGE_CLASS = new Integer(0);

	private static final String WHERE_STR = " where ";
	private static final String AND_STR = " and ";

	/**
	 * There should be only 1 instance of this class.
	 */
	private OpBackupManager() {
		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
		DATE_FORMAT.setTimeZone(OpProjectCalendar.GMT_TIMEZONE);
		TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		TIMESTAMP_FORMAT.setTimeZone(OpProjectCalendar.GMT_TIMEZONE);
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
	 * Initializes the backup manager by registering all the prototypes in correct order.
	 */
	public void initializeBackupManager() {
		OpGraph graph = new OpGraph();
		//      OpGraph.Entry objectNode = graph.addNode(OpTypeManager.getPrototype("OpObject"));
		//      Iterator it = OpTypeManager.getPrototypes(); // unordered!
		//      while (it.hasNext()) {
		//         OpPrototype prototype = (OpPrototype) it.next();
		//         OpGraph.Entry node = graph.addNode(prototype);
		//         if (prototype.getInstanceClass() != OpObject.class) { // add dependency from whatever -> OpObject
		//            graph.addEdge(node, objectNode);
		//         }
		//      }
		Iterator it = OpTypeManager.getPrototypes();
		while (it.hasNext()) {
			OpPrototype prototype = (OpPrototype) it.next();
			graph.addNode(prototype);
			OpGraph.Entry prototypeNode = graph.getNode(prototype);
			List dependencies = prototype.getSubsequentBackupDependencies();
			Iterator iter = dependencies.iterator();
			while (iter.hasNext()) {
				OpPrototype dependency = (OpPrototype) iter.next();
				OpGraph.Entry dependencyNode = (OpGraph.Entry) graph.getNode(dependency);
				if (dependencyNode == null) {
					dependencyNode = graph.addNode(dependency);
				}
				if (((OpPrototype) dependencyNode.getElem()).getInstanceClass() == OpObject.class) {
					graph.removeEdge(prototypeNode, dependencyNode); // remove cyclic dependencies
				}
				graph.addEdge(dependencyNode, prototypeNode, DEFAULT_EDGE_CLASS);
			}
		}

		Iterator iter = graph.getTopologicOrder().iterator();
		while (iter.hasNext()) {
			OpGraph.Entry node = (OpGraph.Entry) iter.next();
			if (!((OpPrototype)node.getElem()).subTypes().hasNext()) {
				addPrototype((OpPrototype)node.getElem());            
			}
		}
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
	public static Map<Long, String> querySystemObjectIdMap(OpBroker broker) {
		Map<Long, String> systemObjectIdMap = new HashMap<Long, String>();
		for (String name : systemObjectIdQueries.keySet()) {
			String queryString = (String) systemObjectIdQueries.get(name);
			logger.info("Query:" + queryString);

			OpQuery query = broker.newQuery(queryString);
			Iterator it = broker.forceIterate(query);
			if (!it.hasNext()) {
				logger.warn("System object id not found after query:" + queryString);
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
		for (OpPrototype prototype : prototypes.values()) {
			attributes.put(NAME, prototype.getName());
			writer.writeStartElement(PROTOTYPE, attributes, false);
			attributes.clear();

			// Create Java array for caching member information
			OpBackupMember[] backupMembers = new OpBackupMember[prototype.getMemberSize()];

			Iterator members = prototype.getMembers();
			int i = 0;
			while (members.hasNext()) {
				OpMember member = (OpMember) members.next();

				Method accessor = this.getAccessorForMember(prototype, member);
				if (accessor == null) {
					logger.error("No accessor found for member: " + member.getName() + "  of prototype:" + prototype.getName());
					continue;
				}
				accessor.setAccessible(true);

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
				else if (member instanceof OpRelationship && !((OpRelationship) member).getInverse() && 
						!((OpRelationship) member).isTransient()) {
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
	private void writeFieldMember(OpObjectIfc object, OpBackupMember member, XDocumentWriter writer, Object value)
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
					if (value instanceof XSizeInputStream) {
						writer.writeContent(writeBinaryFile(object.getId(), member.name, (XSizeInputStream) value));
					}
					else {
						writer.writeContent(writeBinaryFile(object.getId(), member.name, (byte[]) value));
					}
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
				logger.error("ERROR: Unsupported type ID " + member.typeId + " for " +
						OpTypeManager.getPrototypeForObject(object).getName() + "." + member.name);
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
			writer.writeContent(String.valueOf(((OpObjectIfc) value).getId()));
		}
		writer.writeEndElement(R);
	}

	/**
	 * @param orderedBy   a <code>String</code> representing the name of the member field after which to order by.
	 * @param recursiveBy a <code>String</code> representing the name of the member relationship which is recursive.
	 * @param objects     a <code>List</code> of <code>OpObject</code> representing child objects in case of a recursive backup.
	 * @see OpBackupManager#exportObjects(onepoint.project.OpProjectSession,onepoint.xml.XDocumentWriter,String,OpBackupMember[],java.util.Map)
	 */
	private void exportSubObjects(OpProjectSession session, OpBroker broker, XDocumentWriter writer, OpPrototype prototype,
			OpBackupMember[] members, String orderedBy, String recursiveBy, List objects, Map systemIdMap)
	throws IOException {
		int pageSize = BACKUP_PAGE_SIZE;
		int startIndex = 0;

		Iterator result = null;
		if (objects != null) {
			result = objects.iterator();
		}
		else {
			int count = getObjectsToBackupCount(prototype, recursiveBy, broker);
			logger.info("Backing up " + count + " " + prototype.getName());
			if (count > pageSize) {
				while (startIndex < count) {
					logger.info("Backing up objects between " + startIndex + AND_STR + (startIndex + pageSize));
					OpBroker pagingBroker = session.newBroker();
					try {
						result = getObjectsToBackup(prototype, recursiveBy, orderedBy, pagingBroker, startIndex, pageSize);
						startIndex += pageSize;
						pageSize = (startIndex + pageSize) < count ? pageSize : count - startIndex;
						//export each object
						List childObjects = exportIteratedObjects(result, systemIdMap, writer, members, recursiveBy);
						if ((recursiveBy != null) && (childObjects.size() > 0)) {
							exportSubObjects(session, pagingBroker, writer, prototype, members, orderedBy, recursiveBy, childObjects, systemIdMap);
						}
					}
					finally {
						pagingBroker.closeAndEvict();
					}
				}
				return;
			}
			else {
				result = getObjectsToBackup(prototype, recursiveBy, orderedBy, broker, 0, count);
			}
		}

		//export each object
		List childObjects = exportIteratedObjects(result, systemIdMap, writer, members, recursiveBy);

		// Check for next recursion
		if ((recursiveBy != null) && (childObjects.size() > 0)) {
			exportSubObjects(session, broker, writer, prototype, members, orderedBy, recursiveBy, childObjects, systemIdMap);
		}
	}

	private List exportIteratedObjects(Iterator result, Map systemIdMap, XDocumentWriter writer, OpBackupMember[] members,
			String recursiveBy)
	throws IOException {
		List childObjects = (recursiveBy != null) ? new ArrayList() : null;
		try {
			Map<String, String> attributes = new HashMap<String, String>();
			while (result.hasNext()) {
				OpObject object = (OpObject) result.next();
				//object id
				Long id = new Long(object.getId());
				//check whether the object is a system object
				attributes.put(ID, String.valueOf(object.getId()));
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

					//logger.info("value accessor is " + member.accessor.getName() + ", on type " + object.getClass().getName());
					Object value = member.accessor.invoke(object);
					if (member.relationship) {
						this.writeRelationshipMember(writer, value);
						if (member.backRelationshipName != null && recursiveBy != null && recursiveBy.equals(member.name)) {
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
	 * @param prototype a <code>String</code> representing the name of a prototype.
	 * @param recursiveBy   a <code>String</code> indicating the name of the recursive relationship.
	 * @param orderedBy     a <code>String</code> representing the name of the field after which to order the objects.
	 * @param broker        a <code>OpBroker</code> used for performing business operations.
	 * @return an <code>Iterator</code> over <code>OpObjects</code>.
	 */
	private Iterator getObjectsToBackup(OpPrototype prototype, String recursiveBy, String orderedBy, OpBroker broker,
			int startIndex, int count) {
		StringBuffer queryBuffer = new StringBuffer("select xobject from ");
		queryBuffer.append(prototype.getInstanceClass().getSimpleName());
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
         queryBuffer.append(", xobject.");
		}
		queryBuffer.append("id");

		logger.debug("***QUERY: " + queryBuffer);

		OpQuery query = broker.newQuery(queryBuffer.toString());
		query.setFirstResult(startIndex);
		query.setMaxResults(count);
		query.setFetchSize(count);
		return broker.forceIterate(query);
	}

	/**
	 * Gets the number of objects to back-up from the db.
	 *
	 * @param prototype a <code>String</code> representing the name of a prototype.
	 * @param broker        a <code>OpBroker</code> used for performing business operations.
	 * @return an <code>Iterator</code> over <code>OpObjects</code>.
	 */
	private int getObjectsToBackupCount(OpPrototype prototype, String recursiveBy, OpBroker broker) {
		StringBuffer queryBuffer = new StringBuffer("select count(xobject.id) from ");
		queryBuffer.append(prototype.getInstanceClass().getSimpleName());
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

		return ((Number) broker.forceIterate(query).next()).intValue();
	}

	/**
	 * For the given proptotype and members of the prototype exports all current db values.
	 *
	 * @param session       a <code>OpProjectSession</code> used for creating brokers that perform db operations.
	 * @param writer        a <code>XDocumentWriter</code> that is used to output xml.
	 * @param prototype a <code>String</code> representing the name of the prototype.
	 * @param members       a <code>OpBackupMember[]</code> representing the members to be exported for the given prototype.
	 * @param systemIdMap   a <code>Map</code> of <code>[String,Long]</code> representing the system objects.
	 * @throws IOException if anything fails.
	 */
	private void exportObjects(OpProjectSession session, XDocumentWriter writer, OpPrototype prototype,
			OpBackupMember[] members, Map systemIdMap)
	throws IOException {
		logger.info("Backing up ******** " + prototype.getName() + " ********");
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(TYPE, prototype.getName());
		writer.writeStartElement(OBJECTS, attributes, false);
		attributes.clear();

		// Check for first occurence of "ordered" field
		String orderedBy = null;
		//String recursiveBy = null;
		for (int i = 0; i < members.length; i++) {
			if (members[i] != null) {
				logger.info("Backing up member " + members[i].name);
				if (!members[i].relationship && members[i].ordered && orderedBy == null) {
					orderedBy = members[i].name;
				}
//				else if (members[i].relationship && members[i].recursive && recursiveBy == null) {
//					recursiveBy = members[i].name;
//				}

//				if (recursiveBy != null && orderedBy != null) {
				if (orderedBy != null) {
					break;
				}
			}
		}
		OpBroker broker = session.newBroker();
		try {
//			exportSubObjects(session, broker, writer, prototype, members, orderedBy, recursiveBy, null, systemIdMap);
			exportSubObjects(session, broker, writer, prototype, members, orderedBy, null, null, systemIdMap);
		}
		finally {
			broker.closeAndEvict();
		}
		writer.writeEndElement(OBJECTS);
		session.cleanupSession(true);
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
		logger.info("backing up to " + path);
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
		Map systemIdMap = null;
		OpBroker broker = session.newBroker();
		try {
			systemIdMap = querySystemObjectIdMap(broker);
		}
		finally {
			broker.closeAndEvict();
		}

		int memberIndex = 0;
		if (systemIdMap != null) {
			for (OpPrototype prototype : prototypes.values()) {
				exportObjects(session, writer, prototype, (OpBackupMember[]) allMembers.get(memberIndex), systemIdMap);
				memberIndex++;
			}
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
	 * @throws SQLException 
	 * @throws IllegalArgumentException if the given path does not point to an existing file.
	 */
	public final void restoreRepository(OpProjectSession session, String path)
	throws IOException, SQLException {
		if (!isPathAFile(path, true)) {
			throw new IllegalArgumentException("The given path does not point to an exiting file");
		}
		this.removeAllObjects(session);
		String parentDirectory = getParentDirectory(path);
		restoreRepository(session, new BufferedInputStream(new FileInputStream(path)), parentDirectory);
		postProcessRepository(session);
	}

	/**
	 * do whatever is required to finalize the import (correct whatever might be wrong with our data)
	 * @param session
	 */
	public void postProcessRepository(OpProjectSession session) {
		// first use of this one:
		// correct permissions on object that do not have any:
		// FIXME: how to find out which objects are affected??? -> this will resolve itself as soon as we have the "OpObjectWithPermissions"
		// FIXME: for now, maintain a String[] ;-)
		OpBroker broker = session.newBroker();
		try {
			// ensure that adminUser and everyone group are created
			OpUserService.createAdministrator(broker);
			OpUserService.createEveryone(broker);
			session.clearSession();

			String[] persistentClassesWithPermissions = { "OpResource",
					"OpResourcePool", "OpProjectNode", "OpProjectPlan",
					"OpProjectPlanVersion", "OpCustomer", "OpDocumentNode", "OpFolder",
					"OpReport", "OpAttachment", "OpAttachmentVersion" };

			for (int i = 0; i < persistentClassesWithPermissions.length; i++) {
				fixPermissionsAfterImport(session, broker, persistentClassesWithPermissions[i]);
			}
		}
		finally {
			broker.closeAndEvict();
		}


	}

	private void fixPermissionsAfterImport(OpProjectSession session, OpBroker broker,
			String className) {
		OpTransaction t = null;
		OpUser admin = session.administrator(broker);
		OpGroup everyone = session.everyone(broker);

		if (admin == null) {
			logger.warn("No Administrator user defined, no permissions created...");
		}
		if (everyone == null) {
			logger.warn("No group Everyone defined, no permissions created...");
		}

		try {
			t = broker.newTransaction();

			String qString = "" +
			"select x from " + className + " as x where not exists (select p from OpPermission as p where p.Object.id = x.id )";
			OpQuery q = broker.newQuery(qString);

			Iterator<OpObject> oit = broker.iterate(q);
			while (oit.hasNext()) {
				OpPermissionable o = (OpPermissionable) oit.next();
				if (o.getPermissions() == null || o.getPermissions().isEmpty()) {
					logger.info("fixing object without permissions: " + o.locator());
					if (admin != null) {
						broker.makePersistent(new OpPermission(o, admin, OpPermission.ADMINISTRATOR));
					}
					if (everyone != null) {
						broker.makePersistent(new OpPermission(o, everyone, OpPermission.OBSERVER));
					}
				}
			}

			t.commit();
		}
		finally {
			if (t != null) {
				t.rollbackIfNecessary();
			}
		}
	}

	/**
	 * Resets the db schema by dropping the existent one and creating a new one.  It's important here to not loose consistency
	 * in the hibernate hi-lo generator.
	 * <FIXME author="Horia Chiorean" description="Currently this method is used only from tests">
	 *
	 * @throws SQLException if the db schema cannot be droped or created.
	 */
	public void recreateDbSchema()
	throws SQLException {
		logger.info("Dropping schema...");
		OpPersistenceManager.dropSchema();
		logger.info("Creating schema...");
		OpPersistenceManager.createSchema();
	}

	/**
	 * Removes all the objects from the db.
	 *
	 * @param session a <code>OpProjectSession</code> the server session.
	 * @throws SQLException 
	 */
	public void removeAllObjects(OpProjectSession session) throws SQLException {
		logger.info("Removing all objects from the db ");
		// if we are not within SaaS solution and site id == 0 -> drop and recreate schema!
		// NOTE: dropping and recreating schema will be much faster than removing all objects!
		if (!OpEnvironmentManager.isOnDemand() && session.getSourceName() == OpSource.DEFAULT_SOURCE_NAME) {
			recreateDbSchema();
			for (OpSource source : OpSourceManager.getAllSources()) {
				((OpHibernateSource)source).updateSchemaVersionNumber(OpHibernateSource.SCHEMA_VERSION);
			}
			return;
		}

		// Steps done here:
		// 1. get prototypes in correct order (dependency graph)
		// 2. unlink all relations (except one-to-one, they have not null constraints)
		// 3. delete all prototypes 
		
		// create the dependency graph
		OpGraph graph = new OpGraph();

		// complete all prototypes with missing super types
		LinkedHashSet<OpPrototype> allPrototypes = new LinkedHashSet<OpPrototype>();
		for (OpPrototype prototype : prototypes.values()) {
			allPrototypes.add(prototype);
			OpPrototype superType = prototype.getSuperType();
			while (superType != null) {
				if (!allPrototypes.add(superType)) {
					break;
				}
				superType = superType.getSuperType();
			}
		}
		for (OpPrototype prototype : allPrototypes) {
			OpGraph.Entry node = graph.addNode(prototype);
			// add super nodes
			OpPrototype superType = prototype.getSuperType();
			if (superType != null) {
				OpGraph.Entry superNode = graph.addNode(superType);
				graph.addEdge(node, superNode, DEFAULT_EDGE_CLASS); // super type dependencies
			}

			for (OpPrototype dependent : prototype.getDeleteDependencies()) {
				if (dependent != node.getElem()) { // no cycles
					OpGraph.Entry dependentNode = graph.addNode(dependent);
					graph.addEdge(dependentNode, node, DEFAULT_EDGE_CLASS);
				}
			}
			for (OpPrototype dependent : prototype.getNonInverseNonRecursiveDependencies()) {
				if (dependent != node.getElem()) { // no cycles
					OpGraph.Entry dependentNode = graph.addNode(dependent);
					graph.addEdge(node, dependentNode, DEFAULT_EDGE_CLASS);
				}
			}

		}

		List dependencies = graph.getTopologicOrder();

		OpBroker broker = session.newBroker();
		try {
			long start = System.currentTimeMillis();
			logger.info("Clearing db");
			OpTransaction transaction = broker.newTransaction();
         Iterator i = dependencies.iterator();
         while (i.hasNext()) {
            OpGraph.Entry delete = (OpGraph.Entry) i.next();
            logger.info("unlinking objects of type: " + ((OpPrototype) delete.getElem()).getName());
            unlinkObjectsWithPrototype((OpPrototype) delete.getElem(), session, broker, transaction);
         }
			i = dependencies.iterator();
			while (i.hasNext()) {
				OpGraph.Entry delete = (OpGraph.Entry) i.next();
				logger.info("deleting objects of type: " + ((OpPrototype) delete.getElem()).getName());
				removeObjectsWithPrototype((OpPrototype) delete.getElem(), session, broker, transaction);
			}
			transaction.commit();
			logger.info("Clearing db lasted: " + ((System.currentTimeMillis()-start)/1000)+" seconds");
		}
		finally {
			broker.closeAndEvict();
		}
	}


	private void unlinkObjectsWithPrototype(OpPrototype prototype,
         OpProjectSession session, OpBroker broker, OpTransaction transaction) {
	   List<OpRelationship> relations = prototype.getRelationships();
//	   List<String> backRelationshipNames = new LinkedList<String>();
	   for (OpRelationship relationship : relations) {
	      if (!relationship.getInverse() && (!OpTypeManager.getPrototypeByID(relationship.getTypeID()).isInterface())) {
	         OpRelationship backRelationship = relationship.getBackRelationship();
	         if (!((backRelationship != null) && (backRelationship.getCollectionTypeID() != OpType.SET))) {
	            // FIXME (dfreis, Dec 11, 2008) : site filter is somehow not added here that's the reason why the where SiteId stuff is here
	            String query = "update " + prototype.getInstanceClass().getName()+" obj set obj."+relationship.getName()+" = null where SiteId = '"+session.getSourceName()+"'";// + " as o where o.id in (:objectIDs)";// + whereClause;
//	            logger.info("QUERY: "+query);
	            OpQuery objectsQuery = broker.newQuery(query);
	            broker.execute(objectsQuery);
//	            broker.getConnection().flush();
//	            broker.getConnection().clear();
	         }
	      }
	   }
   }

   /**
	 * Removes all the objects with the given prototype name from the db.
	 * This takes into account the recursive relationship order. So it is guaranteed that
	 * if object A references B (e.g. as its parent) A is deleted prior to B.
	 *
	 * @param prototype prototype to delete.
	 * @param session   a <code>OpProjectSession</code> the server session.
	 * @param transaction 
	 */
	private void removeObjectsWithPrototype(OpPrototype prototype, OpProjectSession session, OpBroker broker, OpTransaction transaction) {
	   if (prototype.isInterface()) {
	      return;
	   }
//		logger.info("Remove objects with prototype: " + prototype.getName());
//      OpTransaction transaction = broker.newTransaction();
      // FIXME (dfreis, Dec 11, 2008) : site filter is somehow not added here that's the reason why the where SiteId stuff is here
      String query = "delete " + prototype.getInstanceClass().getName()+" where SiteId = '"+session.getSourceName()+"'";// + " as o where o.id in (:objectIDs)";// + whereClause;
      OpQuery objectsQuery = broker.newQuery(query);
      broker.execute(objectsQuery);
//      broker.getConnection().flush();
//      broker.getConnection().clear();
//      transaction.commit();
	}

	private final static String DELETE_CUSTOM_ATTRIBUTES_FOR_PROTOTYPE = "delete OpCustomValuePage as custom where custom.Object.id in (:objectIDs)";
	/**
	 * @param session
	 * @param broker
	 * @param prototype2
	 * @param recursiveRelationshipCondition
	 * @param toDelete
	 * @pre
	 * @post
	 */
//	private void removeObjectsWithPrototype(OpProjectSession session,
//			OpBroker broker, OpTransaction transaction, OpPrototype prototype, ArrayList<Long> toDelete) {
//		// workaround for custom properties...
//
//		// find prototype superclss:
//		OpPrototype topLevelType = prototype;
//		while (topLevelType.getSuperType() != null) {
//			topLevelType = topLevelType.getSuperType();
//		}
//		if (topLevelType.getInstanceClass() == OpObject.class) { // no ext aps,...
//			logger.debug("deleting  CustomValuePages for " + prototype.getInstanceClass().getName());
////			OpTransaction t = broker.newTransaction();
//			if (prototype.getInstanceClass() == OpCustomValuePage.class) {
//				// query = "update OpObject as obj set obj.CustomValuePage = null where obj.CustomValuePage is not null and obj.CustomValuePage.id in (:objectIDs)";
//				OpQuery caQuery = broker.newQuery("select cvp.Object from OpCustomValuePage as cvp where cvp.id in (:objectIDs)");
//				caQuery.setCollection("objectIDs", toDelete);
//				Iterator iter = broker.iterate(caQuery);
//				while (iter.hasNext()) {
//					OpCustomizable obj = (OpCustomizable) iter.next();
//					obj.setCustomValuePage(null);
//				}
//			}
//			else {
//				String query = "update "+prototype.getInstanceClass().getName()+" as obj set obj.CustomValuePage = null where obj.id in (:objectIDs) and obj.CustomValuePage is not null";
//				OpQuery caQuery = broker.newQuery(query);
//				caQuery.setCollection("objectIDs", toDelete);
//				broker.execute(caQuery);
//			}
////			t.commit();
//			broker.getConnection().flush();
//		}
//		String query = "delete " + prototype.getInstanceClass().getName() + " as o where o.id in (:objectIDs)";// + whereClause;
//
//		OpQuery objectsQuery = broker.newQuery(query);
//		objectsQuery.setCollection("objectIDs", toDelete);
//
////		OpTransaction tx = broker.newTransaction();
//		long start = System.currentTimeMillis();
//		logger.info("Remove " + toDelete.size() + " objects query ("
//				+ prototype.getName() + "): " + query);
//		broker.execute(objectsQuery);
//      transaction.flush();
////		tx.commit();
//		logger.info("transaction lasted: " + (System.currentTimeMillis() - start));
//	}

	/**
	 * Get the identifiers of the objects for a given prototype.
	 *
	 * @param session       session to use
	 * @param prototypeName prototype for which to get the ids
	 * @param recursiveRelationshipCondition 
	 * @return a <code>List<Long></code> of object identifiers
	 */
	private List<Long> getObjectsWithPrototype(OpProjectSession session, OpBroker broker, OpPrototype prototype, String recursiveRelationshipCondition, Byte outlineLevel) {
		//      String whereClause = recursiveRelationshipCondition == null || recursiveRelationshipCondition.length() == 0 ?
		//            WHERE_STR : recursiveRelationshipCondition + AND_STR;
		String whereClause = recursiveRelationshipCondition == null || recursiveRelationshipCondition.length() == 0 ?
				"" : recursiveRelationshipCondition;
		String queryString = "select obj.id from " + prototype.getInstanceClass().getName() + " obj"+whereClause;
		OpQuery query = broker.newQuery(queryString);
		if (outlineLevel != null) {
			query.setByte("outlineLevel", outlineLevel);
			//         whereClause += (whereClause.length() <= 0) ? WHERE_STR : AND_STR;
			//         whereClause += "obj.OutlineLevel = "+outlineLevel;
		}
		logger.info("before getObjectsWithPrototype() query: " + queryString+(outlineLevel == null ? "" : ", outlineLevel : "+outlineLevel));
		long start = System.currentTimeMillis();
		List<Long> oids = broker.list(query, Long.class);
		logger.info("after getObjectsWithPrototype() query: " + queryString+(outlineLevel == null ? "" : ", outlineLevel : "+outlineLevel)+", size: "+oids.size()+", lasted: "+(System.currentTimeMillis()-start));
		return oids;

	}

	/**
	 * @see OpBackupManager#restoreRepository(onepoint.project.OpProjectSession,String)
	 */
	private void restoreRepository(OpProjectSession session, InputStream input, String workingDirectory) {
		long start = System.currentTimeMillis();
		logger.info("Restoring repository...");
		// Extra path is required for restoring binary content stored in separate files
		OpBackupLoader backupLoader = new OpBackupLoader();
		backupLoader.loadBackup(session, input, workingDirectory);
		long elapsedTimeSecs = (System.currentTimeMillis() - start) / 1000;

		// set all custom value pages - this is required in order to avoid cyclic constraints
		// note the problem is that OpObject references first custom value page, which should not be, but was the 
		// only way to add fetch="join"
		OpBroker broker = session.newBroker();
		try {
			OpTransaction t = broker.newTransaction();
			// FIXME(dfreis Feb 26, 2008 8:30:06 AM) {
			// could be done as batch!!!
			OpQuery query = broker.newQuery("select cvp, cvp.Object from OpCustomValuePage as cvp where cvp.Sequence = 0");
			Iterator iter = broker.iterate(query);
			while (iter.hasNext()) {
				Object[] objs = (Object[]) iter.next();
				OpCustomValuePage page = (OpCustomValuePage) objs[0];
				OpCustomizable obj = (OpCustomizable) objs[1];
				if (obj != null && obj.getId() != 0) {
					obj.setCustomValuePage(page);
				}
			}
			// }
		t.commit();
		}
		finally {
			broker.close();
		}

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
	 * Writes a binary file under the given path, in a special directory.
	 *
	 * @param id               a <code>long</code> representing the if of the object which contains the written content.
	 * @param backupMemberName a <code>String</code> representing the name of the backup member which has the content.
	 * @param content          a <code>XSizeInputStream</code> representing the actual content.
	 * @return the path to the newly written binary file.
	 */
	private String writeBinaryFile(long id, String backupMemberName, XSizeInputStream content) {
		File directory = new File(binaryDirPath);
		if (!directory.exists()) {
			directory.mkdir();
		}
		// Write binary data to file
		String fileName = binaryFilePath(id, backupMemberName);
		try {
			FileOutputStream fileOutput = new FileOutputStream(binaryDirPath + fileName);
			XIOHelper.copy(content, fileOutput);
			fileOutput.flush();
			fileOutput.close();
			content.close();
		}
		catch (IOException e) {
			logger.error("ERROR: An I/O exception occured when trying to write binary file: ", e);
		}
		return SLASH_STRING + this.binaryDirName + fileName;
	}

	/**
	 * Returns the accessor method for a member of a prototype.
	 *
	 * @param prototype a <code>OpPrototype</code> the prototype to be exported.
	 * @param member    a <code>OpMember</code> the field or relationship of the prototype to export.
	 * @return a <code>Method</code> to access the member or <code>null</code> if there is no such method.
	 */
	private Method getAccessorForMember(OpPrototype prototype, OpMember member) {
		final String[] PREFIXES = {"get", "is", "has"};
		for (String prefix : Arrays.asList(PREFIXES)) {
			try {
				return prototype.getInstanceClass().getMethod(prefix + member.getName());
			}
			catch (NoSuchMethodException e) {
				logger.info(e.getMessage());
			}
		}
		for (String prefix : Arrays.asList(PREFIXES)) {
			try {
				Method method = prototype.getInstanceClass().getDeclaredMethod(prefix + member.getName());
				method.setAccessible(true);
				return method;
			}
			catch (NoSuchMethodException e) {
				logger.info(e.getMessage());
			}
		}
		logger.error("no accessor method found for field " + member.getName() + ", within class " + prototype.getInstanceClass().getName());
		return null;
	}

	/**
	 * Reads the contents of a binary file from the given path.
	 *
	 * @param path a <code>String</code> representing the path to a binary file.
	 * @return a <code>XSizeInputStream</code> with the file's content.
	 */
	static XSizeInputStream readBinaryFile(String path) {
		try {
			File file = new File(path);
			return new XSizeInputStream(new FileInputStream(file), file.length());
		}
		catch (FileNotFoundException e) {
			logger.error("Missing file to restore: " + path, e);
			throw new XServiceException("Missing file to restore: " + path, e);
		}
		catch (IOException e) {
		   logger.error("An I/O exception occured when trying to read binary file" + path, e);
		   throw new XServiceException("An I/O exception occured when trying to read binary file" + path, e);
		}
	}

	/**
	 * Registers a prototype with the backup manager, taking into account the prototype's dependencies.
	 *
	 * @param superPrototype           a <code>OpPrototype</code> representing OpObject's prototype.
	 * @param startPoint               a <code>OpPrototype</code> representing a start point in the back-up registration process.
	 * @param lastPrototypesToRegister a <code>List</code> which acts as an acumulator and will contain at the end a list
	 *                                 of prototypes which will be registered at the end of all the others.
	 */
	private void registerPrototypeForBackup(OpPrototype superPrototype, OpPrototype startPoint, List<OpPrototype> lastPrototypesToRegister) {
		// FIXME(dfreis Oct 23, 2007 9:44:40 AM) wont work see also my comment on OPP-80
		// note lastPrototypesToRegister is a hack only!
		List dependencies = startPoint.getSubsequentBackupDependencies();
		logger.debug("start point is: " + startPoint.getName());
		for (Object dependency1 : dependencies) {
			OpPrototype dependency = (OpPrototype) dependency1;
			if (dependency.getID() == superPrototype.getID()) {
				if (!lastPrototypesToRegister.contains(startPoint)) {
					lastPrototypesToRegister.add(startPoint);
				}
			}
			else if (!OpBackupManager.hasRegistered(dependency)) {
				logger.debug("dependency is: " + dependency.getName());
				registerPrototypeForBackup(superPrototype, dependency, lastPrototypesToRegister);
			}
		}
		if (!startPoint.subTypes().hasNext() && !OpBackupManager.hasRegistered(startPoint)) {
			if (!lastPrototypesToRegister.contains(startPoint)) {
				addPrototype(startPoint);
			}
		}
	}
}
