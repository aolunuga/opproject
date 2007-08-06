/**
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;

import java.util.*;

/**
 * This class is responsible for generation of Hibernate mapping file for all loaded prototypes.
 *
 * @author calin.pavel
 */
public class OpMappingsGenerator {
   // The logger used in this class.
   private static final XLog logger = XLogFactory.getServerLogger(OpMappingsGenerator.class);

   public final static String INDEX_NAME_PREFIX = "op_";
   public final static String INDEX_NAME_POSTFIX = "_i";
   public final static String COLUMN_NAME_PREFIX = "op_";
   public final static String COLUMN_NAME_POSTFIX = "";
   public final static String TABLE_NAME_PREFIX = "op_";
   private final static String TABLE_NAME_POSTFIX = "";
   private final static String JOIN_NAME_SEPARATOR = "_";

   // Strings representing prototype prefixes
   private static final List<String> PROTOTYPE_PREFIXES = Arrays.asList("X", "Op");

   // IBM DB2 index maximum length is 18 (SQLSTATE=42622)
   private static final int IBM_DB2_INDEX_NAME_LENGTH = 18;

   // Character for NEW LINE.
   private static final String NEW_LINE = "\n";

   // String used to indent mappings
   private static final char SPACE_CHAR = ' ';

   // Used database type.
   private int databaseType;

   // Root prototypes for which we should generate mapping file.
   protected List<OpPrototype> rootTypes;

   // Hibernate filters
   protected Set<OpHibernateFilter> filters;

   /**
    * Creates a new generator for the provided prototypes.
    *
    * @param databaseType data base type.
    */
   public OpMappingsGenerator(int databaseType) {
      this.databaseType = databaseType;
   }

   /**
    * Initialize mappings generator with the prototypes for which mappings should be generated.
    *
    * @param initialPrototypes initial prototypes
    */
   protected void init(Iterator initialPrototypes) {
      if (initialPrototypes == null || !initialPrototypes.hasNext()) {
         logger.info("No prototype received for mapping.");
         return;
      }

      this.rootTypes = new ArrayList<OpPrototype>();

      // now process all prototypes and find root prototypes
      while (initialPrototypes.hasNext()) {
         OpPrototype prototype = (OpPrototype) initialPrototypes.next();
         OpPrototype superType = prototype.getSuperType();

         if (superType == null) {
            this.rootTypes.add(prototype);
         }
      }

      logger.debug("End processing prototypes.");
   }

   /**
    * This method generates and returns Hibernate mappings XML content.
    */
   public String generateMappings() {
      StringBuffer buffer = new StringBuffer();

      buffer.append("<?xml version=\"1.0\"?>\n");
      buffer.append("<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">");
      buffer.append("<hibernate-mapping>").append(NEW_LINE);

      //Process all root types
      for (OpPrototype prototype : rootTypes) {
         generateRootClassMapping(buffer, prototype);
      }

      // Add filter definitions
      if (filters != null && filters.size() > 0) {
         for (OpHibernateFilter filter : filters) {
            addFilterDefinition(buffer, filter, 1);
         }
      }

      buffer.append("</hibernate-mapping>").append(NEW_LINE);

      String mappings = buffer.toString();
      logger.debug(mappings);

      return mappings;
   }

   /**
    * This method add to mapping content definition of a Hibernate filter.
    *
    * @param buffer mappings buffer
    * @param filter filter to add
    * @param level  indent level
    */
   private void addFilterDefinition(StringBuffer buffer, OpHibernateFilter filter, int level) {
      buffer.append(generateIndent(level)).append("<filter-def name=\"").append(filter.getName()).append("\">").append(NEW_LINE);

      // add filter parameters.
      if (filter.getParameters() != null) {
         for (Map.Entry entry : filter.getParameters().entrySet()) {
            String paramName = (String) entry.getKey();
            String paramType = (String) entry.getValue();

            buffer.append(generateIndent(level + 1)).append("<filter-param name=\"").append(paramName);
            buffer.append("\" type=\"").append(paramType).append("\"/>").append(NEW_LINE);
         }
      }

      buffer.append(generateIndent(level)).append("</filter-def>").append(NEW_LINE);
   }

   /**
    * This method should add if necessary tags for filters usage (class, relationship, ... level).
    *
    * @param buffer buffer where to add content
    * @param level  indent level.
    */
   private void addFiltersUsage(StringBuffer buffer, int level) {
      if (filters != null && filters.size() > 0) {
         for (OpHibernateFilter filter : filters) {
            buffer.append(generateIndent(level)).append("<filter name=\"").append(filter.getName());
            buffer.append("\" condition=\"").append(filter.getCondition()).append("\"/>").append(NEW_LINE);
         }
      }
   }

   /**
    * Generates mapping for root type.
    *
    * @param buffer   buffer where to write mapping content
    * @param rootType root prototype
    */
   private void generateRootClassMapping(StringBuffer buffer, OpPrototype rootType) {
      int level = 1;
      buffer.append(generateIndent(level)).append("<class name=\"").append(rootType.getInstanceClass().getName());
      buffer.append("\" table=\"").append(generateTableName(rootType.getName())).append("\">").append(NEW_LINE);
      appendLine(buffer, "<cache usage=\"read-write\"/>", level + 1);

      // Add hard-coded primary key property "ID"
      appendLine(buffer, "<id name=\"ID\" column=\"op_id\" type=\"long\">", level + 1);
      appendLine(buffer, "<generator class=\"hilo\"/>", level + 2);
      appendLine(buffer, "</id>", level + 1);

      // Add members
      addMembers(buffer, rootType, level + 1);

      // Add subtypes
      Iterator subTypesIt = rootType.subTypes();
      while (subTypesIt.hasNext()) {
         OpPrototype subType = (OpPrototype) subTypesIt.next();
         appendSubTypeMapping(buffer, subType, level + 1);
      }

      // add if necessary filter at class level
      addFiltersUsage(buffer, level + 1);

      buffer.append(generateIndent(level)).append("</class>").append(NEW_LINE);
   }

   /**
    * Add mappings for prototype members.
    *
    * @param buffer    buffer where to write content.
    * @param prototype prototype for which we'll add members mappings
    * @param level     indent level
    */
   private void addMembers(StringBuffer buffer, OpPrototype prototype, int level) {
      Iterator membersIt = prototype.getDeclaredMembers();

      while (membersIt.hasNext()) {
         OpMember member = (OpMember) membersIt.next();

         if (member instanceof OpField) {
            addFieldMember(buffer, prototype, (OpField) member, level);
         }
         else if (member instanceof OpRelationship) {
            addRelationMember(buffer, prototype, (OpRelationship) member, level);
         }
         else {
            logger.error("Invalid type for member: " + member.getName());
         }
      }
   }

   /**
    * Add to mapping content prototype fields
    *
    * @param buffer bufffer where to add mapping
    * @param field  field to map
    * @param level  indent level
    */
   private void addFieldMember(StringBuffer buffer, OpPrototype prototype, OpField field, int level) {
      // Map field
      buffer.append(generateIndent(level)).append("<property name=\"").append(field.getName()).append("\"");

      buffer.append(" type=\"").append(getHibernateTypeName(field.getTypeID())).append("\"");

      if (field.getMandatory()) {
         buffer.append(" not-null=\"true\"");
      }

      if (field.getUnique()) {
         buffer.append(" unique=\"true\"");
      }

      if (field.getUniqueKey() != null && field.getUniqueKey().trim().length() != 0) {
         buffer.append(" unique-key=\"").append(field.getUniqueKey()).append("\"");
      }

      if (field.getUpdate() != null) {
         buffer.append(" update=\"").append(field.getUpdate()).append("\"");
      }

      if (field.getInsert() != null) {
         buffer.append(" insert=\"").append(field.getInsert()).append("\"");
      }

      buffer.append("><column name=\"");
      String columnName = field.getColumn() != null ? field.getColumn() : generateColumnName(field.getName());
      buffer.append(columnName);
      buffer.append('"');

      // Exception for some databases when using a BLOB
      if ((field.getTypeID() == OpType.CONTENT)) {
         if (databaseType == OpHibernateSource.MYSQL_INNODB) {
            buffer.append(" sql-type=\"longblob\"");
         }
         if (databaseType == OpHibernateSource.POSTGRESQL) {
            buffer.append(" sql-type=\"bytea\"");       // default generated type is wrong
         }
         if ((databaseType == OpHibernateSource.IBM_DB2)) {
            buffer.append(" sql-type=\"blob(100M)\"");
         }
         if (databaseType == OpHibernateSource.DERBY) {
            buffer.append(" sql-type=\"blob\"");
         }
      }

      if (field.getMandatory()) {
         buffer.append(" not-null=\"true\"");
      }

      if (field.getUnique() && !(field.getTypeID() == OpType.TEXT)) {
         buffer.append(" unique=\"true\"");
      }

      if (field.getUniqueKey() != null && field.getUniqueKey().trim().length() != 0) {
         buffer.append(" unique-key=\"").append(field.getUniqueKey()).append("\"");
      }

      if (field.getDefaultValue() != null && field.getDefaultValue().trim().length() != 0) {
         buffer.append(" default=\"").append(field.getDefaultValue()).append("\"");
      }

      if (field.getIndexed() && !(field.getTypeID() == OpType.TEXT)) {
         buffer.append(" index=\"");
         buffer.append(generateIndexName(prototype.getName(), field.getName()));
         buffer.append('\"');
      }

      if (field.getTypeID() == OpType.TEXT) {
         int maxLen = OpTypeManager.getMaxLength(OpType.TEXT);
         buffer.append(" length=\"").append(maxLen).append("\"");
      }

      buffer.append("/></property>").append(NEW_LINE);
   }

   /**
    * Add to mapping content prototype relations
    *
    * @param buffer       bufffer where to add mapping
    * @param prototype    prototype for which we add relatiojship
    * @param relationship relationship to map
    * @param level        indent level
    */
   private void addRelationMember(StringBuffer buffer, OpPrototype prototype, OpRelationship relationship, int level) {
      // Map relationship
      String cascadeMode = relationship.getCascadeMode();
      OpRelationship back_relationship = relationship.getBackRelationship();
      OpPrototype target_prototype = OpTypeManager.getPrototypeByID(relationship.getTypeID());
      if (relationship.getCollectionTypeID() != OpType.SET) {
         // Map one-to-one or many-to-one relationship
         if ((back_relationship != null) && (back_relationship.getCollectionTypeID() != OpType.SET)) {
            // Map one-to-one relationship
            if (relationship.getInverse()) {
               buffer.append(generateIndent(level)).append("<one-to-one name=\"");
               buffer.append(relationship.getName());
               buffer.append("\" property-ref=\"");
               buffer.append(back_relationship.getName());
               if (cascadeMode != null) {
                  buffer.append("\" cascade=\"");
                  buffer.append(cascadeMode);
               }
               buffer.append("\"/>").append(NEW_LINE);
            }
            else {
               buffer.append(generateIndent(level)).append("<many-to-one name=\"");
               buffer.append(relationship.getName());
               buffer.append("\" column=\"");
               buffer.append(generateColumnName(relationship.getName()));
               buffer.append("\" class=\"");
               buffer.append(target_prototype.getInstanceClass().getName());
               buffer.append("\" unique=\"true\" not-null=\"true");
               if (cascadeMode != null) {
                  buffer.append("\" cascade=\"");
                  buffer.append(cascadeMode);
               }
               buffer.append("\"/>").append(NEW_LINE);
            }
         }
         else {
            // Map many-to-one relationship
            buffer.append(generateIndent(level)).append("<many-to-one name=\"");
            buffer.append(relationship.getName());
            buffer.append("\" column=\"");
            buffer.append(generateColumnName(relationship.getName()));
            buffer.append("\" class=\"");
            buffer.append(target_prototype.getInstanceClass().getName());
            if (cascadeMode != null) {
               buffer.append("\" cascade=\"");
               buffer.append(cascadeMode);
            }
            buffer.append("\"/>").append(NEW_LINE);
         }
      }
      else if (back_relationship != null) {
         // Map one-to-many or many-to-many relationship
         if (back_relationship.getCollectionTypeID() != OpType.SET) {
            // Map one-to-many relationship
            buffer.append(generateIndent(level)).append("<set name=\"");
            buffer.append(relationship.getName());
            if (relationship.getInverse()) {
               buffer.append("\" inverse=\"true");
            }
            buffer.append("\" lazy=\"true");
            if (cascadeMode != null) {
               buffer.append("\" cascade=\"");
               buffer.append(cascadeMode);
            }
            buffer.append("\">").append(NEW_LINE);
            buffer.append(generateIndent(level + 1)).append("<key column=\"");
            buffer.append(generateColumnName(back_relationship.getName()));
            buffer.append("\"/>").append(NEW_LINE);
            buffer.append(generateIndent(level + 1)).append("<one-to-many class=\"");
            buffer.append(target_prototype.getInstanceClass().getName());
            buffer.append("\"/>").append(NEW_LINE);

            // add if necessary filter for relationship
            addFiltersUsage(buffer, level + 1);

            buffer.append(generateIndent(level)).append("</set>").append(NEW_LINE);
         }
         else {
            // Map many-to-many relationship
            buffer.append(generateIndent(level)).append("<set name=\"");
            buffer.append(relationship.getName());
            String join_table_name;
            String key_column_name;
            String column_name;
            if (relationship.getInverse()) {
               buffer.append("\" inverse=\"true");
               join_table_name = generateJoinTableName(target_prototype.getName(), back_relationship.getName());
            }
            else {
               join_table_name = generateJoinTableName(prototype.getName(), relationship.getName());
            }
            key_column_name = generateJoinColumnName(prototype.getName(), relationship.getName());
            column_name = generateJoinColumnName(target_prototype.getName(), back_relationship.getName());
            buffer.append("\" table=\"");
            buffer.append(join_table_name);
            buffer.append("\" lazy=\"true");
            if (cascadeMode != null) {
               buffer.append("\" cascade=\"").append(cascadeMode);
            }
            buffer.append("\">").append(NEW_LINE);
            buffer.append(generateIndent(level + 1)).append("<key column=\"").append(key_column_name).append("\"/>").append(NEW_LINE);
            buffer.append(generateIndent(level + 1)).append("<many-to-many class=\"");
            buffer.append(target_prototype.getInstanceClass().getName()).append("\" column=\"").append(column_name).append("\">").append(NEW_LINE);
            // add if necessary filter for relationship
            addFiltersUsage(buffer, level + 2);
            buffer.append(generateIndent(level + 1)).append("</many-to-many>");

            // add if necessary filter for relationship
            addFiltersUsage(buffer, level + 1);

            buffer.append(generateIndent(level)).append("</set>").append(NEW_LINE);
         }
      }
      else {
         logger.warn("Warning: To-many relationships not supported for null back-relationship: " + prototype.getName() + "." + relationship.getName());
      }

   }

   /**
    * Add mapping for subtype.
    *
    * @param buffer    buffer where to add mapping
    * @param prototype sub prototype
    * @param level     indent level
    */
   private void appendSubTypeMapping(StringBuffer buffer, OpPrototype prototype, int level) {
      buffer.append(NEW_LINE);
      buffer.append(generateIndent(level)).append("<joined-subclass name=\"");
      buffer.append(prototype.getInstanceClass().getName());
      buffer.append("\" table=\"");
      String table_name = generateTableName(prototype.getName());
      buffer.append(table_name);
      buffer.append("\">").append(NEW_LINE);

      // Add hard-coded join-key for column of property "ID"
      buffer.append(generateIndent(level + 1)).append("<key column=\"op_id\"/>").append(NEW_LINE);

      // Add declared members (only these of this inheritance level)
      addMembers(buffer, prototype, level + 1);

      // Recursively map sub-types
      Iterator subTypes = prototype.subTypes();
      OpPrototype subType;
      while (subTypes.hasNext()) {
         subType = (OpPrototype) subTypes.next();
         appendSubTypeMapping(buffer, subType, level + 1);
      }

      buffer.append(generateIndent(level)).append("</joined-subclass>").append(NEW_LINE);
   }

   /**
    * ************** HELPER METHODS **********************
    */

   /**
    * Generates column name
    *
    * @param property_name property for which we need to generate column name
    * @return column name
    */
   public static String generateColumnName(String property_name) {
      StringBuffer buffer = new StringBuffer(COLUMN_NAME_PREFIX);
      buffer.append(property_name.toLowerCase());
      buffer.append(COLUMN_NAME_POSTFIX);
      return buffer.toString();
   }

   /**
    * Generates table name
    *
    * @param prototype_name name of the prototype for which we need to generate table name
    * @return table name
    */
   public static String generateTableName(String prototype_name) {
      StringBuffer buffer = new StringBuffer(TABLE_NAME_PREFIX);
      buffer.append(removePrototypeNamePrefixes(prototype_name).toLowerCase());
      buffer.append(TABLE_NAME_POSTFIX);
      return buffer.toString();
   }

   /**
    * Removes prefixes from the name of a prototype, each prefix at most once.
    *
    * @param prototypeName a <code>String</code> representing the name of a prototype.
    * @return a <code>String</code> representing the name of the prototype without the prefixes.
    */
   private static String removePrototypeNamePrefixes(String prototypeName) {
      for (String prefix : PROTOTYPE_PREFIXES) {
         if (prototypeName.startsWith(prefix)) {
            prototypeName = prototypeName.replaceAll(prefix, "");
         }
      }
      return prototypeName;
   }

   /**
    * Generates JOIN table name
    *
    * @param prototype_name1 name of the prototype for which we need to generate table name
    * @param prototype_name2 name of the prototype for which we need to generate table name
    * @return table name
    */
   private static String generateJoinTableName(String prototype_name1, String prototype_name2) {
      StringBuffer buffer = new StringBuffer(TABLE_NAME_PREFIX);
      buffer.append(prototype_name1.toLowerCase());
      buffer.append(JOIN_NAME_SEPARATOR);
      buffer.append(prototype_name2.toLowerCase());
      buffer.append(TABLE_NAME_POSTFIX);

      return buffer.toString();
   }

   /**
    * GEnerates column name for a join.
    *
    * @param prototype_name prototype name
    * @param property_name  join property name
    * @return join column name
    */
   private static String generateJoinColumnName(String prototype_name, String property_name) {
      StringBuffer buffer = new StringBuffer(TABLE_NAME_PREFIX);
      buffer.append(prototype_name.toLowerCase());
      buffer.append(JOIN_NAME_SEPARATOR);
      buffer.append(property_name.toLowerCase());
      buffer.append(TABLE_NAME_POSTFIX);
      return buffer.toString();
   }

   /**
    * Generates index name
    *
    * @param prototype_name prototype for which we need to generate index.
    * @param property_name  property for which index must be generated
    * @return index name.
    */
   private String generateIndexName(String prototype_name, String property_name) {
      StringBuffer buffer = new StringBuffer(generateTableName(prototype_name));
      buffer.append(JOIN_NAME_SEPARATOR);
      buffer.append(property_name.toLowerCase());
      buffer.append(INDEX_NAME_POSTFIX);

      if (databaseType == OpHibernateSource.IBM_DB2 && buffer.length() > IBM_DB2_INDEX_NAME_LENGTH) {
         String indexName = buffer.substring(TABLE_NAME_PREFIX.length());
         int start = 0;
         if (indexName.length() > IBM_DB2_INDEX_NAME_LENGTH) {
            start = indexName.length() - IBM_DB2_INDEX_NAME_LENGTH;
         }

         return indexName.substring(start, indexName.length());
      }

      return buffer.toString();
   }

   /**
    * Returns the name of the hibernate type associated with the given id of an <code>OpType</code>.
    *
    * @param xTypeId a <code>int</code> representing the id of an OpType.
    * @return a <code>String</code> representing the name of the equivalent hibernate type, or the name of a custom type.
    */
   public static String getHibernateTypeName(int xTypeId) {
      String typeName = null;
      switch (xTypeId) {
         case OpType.BOOLEAN: {
            typeName = "boolean";
            break;
         }
         case OpType.INTEGER: {
            typeName = "integer";
            break;
         }
         case OpType.LONG: {
            typeName = "long";
            break;
         }
         case OpType.STRING: {
            typeName = "string";
            break;
         }
         case OpType.TEXT: {
            typeName = "string";
            break;
         }
         case OpType.DATE: {
            typeName = "java.sql.Date";
            break;
         }
         case OpType.CONTENT: {
            typeName = "onepoint.persistence.hibernate.OpBlobUserType";
            break;
         }
         case OpType.BYTE: {
            typeName = "byte";
            break;
         }
         case OpType.DOUBLE: {
            typeName = "double";
            break;
         }
         case OpType.TIMESTAMP: {
            typeName = "timestamp";
            break;
         }
      }
      return typeName;
   }

   /**
    * Add provided content into the buffer with appropriate indent and NEW line char at the end.
    *
    * @param buffer  buffer where to write content/
    * @param content content to write
    * @param level   indent level
    */
   private void appendLine(StringBuffer buffer, String content, int level) {
      buffer.append(generateIndent(level)).append(content).append(NEW_LINE);
   }

   /**
    * Generates indent string.
    *
    * @param level level for which we generate indent.
    * @return indent string
    */
   private static String generateIndent(int level) {
      // each line will be indented with 3 indent chars.
      char[] array = new char[level * 3];
      Arrays.fill(array, SPACE_CHAR);

      return new String(array);
   }
}
