/**
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.persistence.hibernate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpField;
import onepoint.persistence.OpMember;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpRelationship;
import onepoint.persistence.OpType;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.project.modules.customers.OpCustomer;
import onepoint.project.modules.documents.OpDocumentNode;
import onepoint.project.modules.documents.OpFolder;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpAttachmentVersion;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.report.OpReportType;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.work.OpCostRecord;
import onepoint.project.modules.work.OpTimeRecord;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XResourceBroker;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.BooleanType;

/**
 * This class is responsible for generation of Hibernate mapping file for all loaded prototypes.
 *
 * @author calin.pavel
 */
public class OpMappingsGenerator {
   /**
    * 
    */
   public static final String TYPE_SUFFIX = "_TYPE";
   public static final String ID_SUFFIX = "_ID";

   // The logger used in this class.
   private static final XLog logger = XLogFactory.getLogger(OpMappingsGenerator.class);

   private final static String INDEX_NAME_PREFIX = "op_";
   private final static String INDEX_NAME_POSTFIX = "_i";
   private final static String COLUMN_NAME_PREFIX = "op_";
   private final static String COLUMN_NAME_POSTFIX = "";
   private final static String TABLE_NAME_PREFIX = "op_";

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
   private static final boolean USE_DYNAMIC = false;

   // Used database type.
   private int databaseType;

   // Root prototypes for which we should generate mapping file.
   protected List<OpPrototype> rootTypes;

   // Hibernate filters
   protected Set<OpHibernateFilter> filters;


   private static OpMappingsGenerator generator;

   private static Map<Class, String> shortNameMap = new HashMap<Class, String>();
   
   static {
         shortNameMap.put(OpActivity.class, "AC");
         shortNameMap.put(OpActivityVersion.class, "ACV");
         shortNameMap.put(OpAttachment.class, "AT");
         shortNameMap.put(OpAttachmentVersion.class, "ATV");
         shortNameMap.put(OpCostRecord.class, "CR");
         shortNameMap.put(OpCustomer.class, "CU");
         shortNameMap.put(OpDocumentNode.class, "DN");
         shortNameMap.put(OpFolder.class, "FO");
         shortNameMap.put(OpProjectNode.class, "PN");
         shortNameMap.put(OpReport.class, "RP");
         shortNameMap.put(OpReportType.class, "RPT");
         shortNameMap.put(OpResource.class, "RE");
         shortNameMap.put(OpResourcePool.class, "REP");
         shortNameMap.put(OpTimeRecord.class, "TR");
         shortNameMap.put(OpProjectPlan.class, "PP");
         shortNameMap.put(OpProjectPlanVersion.class, "PPV");
   }

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
      buffer.append("<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n");
      // FIXME(dfreis Oct 11, 2007 6:35:57 AM) OPP 8.1: direct field access - will break backup/restore
 //     buffer.append("<hibernate-mapping default-access=\"onepoint.persistence.hibernate.OpPropertyAccessor\">").append(NEW_LINE);
      buffer.append("<hibernate-mapping>").append(NEW_LINE);

      //Process all root types
      for (OpPrototype prototype : rootTypes) {
         appendMapping(buffer, prototype, 0);
      }

      // Add filter definitions
      if (filters != null && filters.size() > 0) {
         for (OpHibernateFilter filter : filters) {
            addFilterDefinition(buffer, filter, 1);
         }
      }

      buffer.append("</hibernate-mapping>").append(NEW_LINE);

      String mappings = buffer.toString();
      if (logger.isLoggable(XLog.INFO)) {
         try {
            File fout = new File("HibernateMapping.hbm.xml");
            if (!fout.exists()) {
               fout.createNewFile();
            }
            logger.info("writing mapping to file: "+fout.getAbsolutePath());
            FileWriter writer = new FileWriter(fout);
            writer.write(mappings);
            writer.close();
         }
         catch (IOException exc) {
            // TODO Auto-generated catch block
            exc.printStackTrace();
         }
      }
      return mappings;
   }

   /**
    * @return
    * @pre
    * @post
    */
   private Map<OpPrototype, Set<OpPrototype>> getInterfaceMap() {
      // TODO Auto-generated method stub
      return null;
   }

   /**
    * @param buffer
    * @param prototype
    * @pre
    * @post
    */
   private void appendMapping(StringBuffer buffer, OpPrototype prototype,
         int level) {
      String[] implementingNames = prototype.getImplementingNames();
//      if (implementingNames != null && implementingNames.length > 0) {
//         for (int pos = 0; pos < implementingNames.length; pos++) {
//            
//         }
//      }
      if (prototype.getSuperType() == null) {
         generateRootClassMapping(buffer, prototype);
      }
      else {
         appendSubTypeMapping(buffer, prototype, level);
      }
      // TODO Auto-generated method stub
      
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
      if (rootType.isInterface()) {
         return;
      }
      int level = 1;
      buffer.append(generateIndent(level)).append("<class name=\"").
      append(rootType.getInstanceClass().getName());
      buffer.append("\" polymorphism=\"implicit\" ");
      if (USE_DYNAMIC) {
         buffer.append("dynamic-insert=\"true\" ");
         buffer.append("dynamic-update=\"true\" ");
      }
      buffer.append("table=\"").append(generateTableName(rootType.getName())).append("\">").append(NEW_LINE);
      appendLine(buffer, "<cache usage=\"read-write\"/>", level + 1);

      // Add hard-coded primary key property "ID"
      //if ()
      appendLine(buffer, "<id name=\"id\" column=\"op_id\" type=\"long\">", level + 1);
      appendLine(buffer, "<generator class=\"hilo\">", level + 2);
      appendLine(buffer, "<param name=\"table\">" + OpHibernateSource.HILO_GENERATOR_TABLE_NAME + "</param>", level + 3);
      appendLine(buffer, "<param name=\"column\">" + OpHibernateSource.HILO_GENERATOR_COLUMN_NAME + "</param>", level + 3);
      appendLine(buffer, "</generator>", level + 2);
      appendLine(buffer, "</id>", level + 1);
      // add hardcoded created and modified fields
//      appendLine(buffer, "<property name=\"Created\" type=\"timestamp\" not-null=\"true\"><column name=\"op_created\" not-null=\"true\"/></property>", level+1);
//      appendLine(buffer, "<property name=\"Modified\" type=\"timestamp\" not-null=\"true\"><column name=\"op_modified\" not-null=\"true\"/></property>", level+1);

      // Add interfaces
      addInterfaces(buffer, rootType, level);
      
      // Add members
      addMembers(buffer, rootType, level + 1);

      // Add subtypes
      Iterator subTypesIt = rootType.subTypes();
      while (subTypesIt.hasNext()) {
         OpPrototype subType = (OpPrototype) subTypesIt.next();
         appendMapping(buffer, subType, level + 1);
      }

      // add if necessary filter at class level
      addFiltersUsage(buffer, level + 1);

      buffer.append(generateIndent(level)).append("</class>").append(NEW_LINE);
   }

   /**
    * @param buffer
    * @param rootType
    * @param level
    * @pre
    * @post
    */
   private void addInterfaces(StringBuffer buffer, OpPrototype rootType,
         int level) {
      if (rootType.getImplementedTypes() != null) {
         for (OpPrototype type : rootType.getImplementedTypes()) {
            appendInterface(buffer, rootType, type, level+1);
         }         
      }
   }

   /**
    * @param buffer
    * @param type 
    * @param interfaceType
    * @param i
    * @pre
    * @post
    */
   private void appendInterface(StringBuffer buffer,
         OpPrototype type, OpPrototype interfaceType, int level) {
      Iterator<OpMember> iter = interfaceType.getMembers();
      while (iter.hasNext()) {
         OpMember member = iter.next();
         if (member instanceof OpField) {
            addFieldMember(buffer, type, (OpField)member, level);
         }
         else if (member instanceof OpRelationship) {
            addRelationMember(buffer, type, (OpRelationship)member, level, true);
         }
         else {
            logger.error("Invalid type for member: " + type.getName());
         }

//         if (member instanceof OpRelationship) {
////            if (!((OpRelationship) member).isTransient()) {
//            appendInterface(buffer, type, interfaceType, (OpRelationship) member, level);
////            }
//         }
      }
   }

   /**
    * @param buffer
    * @param type
    * @param interfaceType
    * @param member
    * @param level
    * @pre
    * @post
    */
   private void appendInterface(StringBuffer buffer, OpPrototype type,
         OpRelationship member, int level) {
//      if (true) return;
//      if (member.getInverse()) {
         buffer.append(generateIndent(level)).append("<set name=\"").
         append(member.getName()).append("\"");
         buffer.append(" table=\"").append(generateTableName(member.getTypeName())).append("\"");
         buffer.append(" where=\"").append(COLUMN_NAME_PREFIX).append(member.getBackRelationship().getName()).append(TYPE_SUFFIX).append(COLUMN_NAME_POSTFIX)
         .append("='").append(getIdForImplementingType(type)).append("'\"");
//       if (member.getInverse()) {
//       buffer.append(" inverse=\"true\"");
//       }
         String fetch = member.getFetch();
         String orderBy = member.getOrderBy();
         String cascadeMode = member.getCascadeMode();

//       if (orderBy != null) {
//       buffer.append(" order-by=\"").append(generateColumnName(orderBy)).append("\"");
//       }
//       buffer.append(" lazy=\"").append(member.getLazy()).append("\"");
//       if (cascadeMode != null) {
//       buffer.append(" cascade=\"").append(cascadeMode).append("\"");
//       }
//       if (fetch != null) {
//       buffer.append(" fetch=\"").append(fetch).append("\"");
//       }

         buffer.append(">").append(NEW_LINE);
         buffer.append(generateIndent(level + 1)).append("<key foreign-key=\"none\">").append(NEW_LINE);
         buffer.append(generateIndent(level + 2)).append("<column name=\"").
         append(COLUMN_NAME_PREFIX).append(member.getBackRelationship().getName()+ID_SUFFIX).append(COLUMN_NAME_POSTFIX).
         append("\"/>").append(NEW_LINE);
         buffer.append(generateIndent(level + 1)).append("</key>").append(NEW_LINE);

         buffer.append(generateIndent(level + 1)).append("<one-to-many class=\"").
         append(OpTypeManager.getPrototypeByID(member.getTypeID()).getInstanceClass().getName()).
         append("\"/>").append(NEW_LINE);

         // add if necessary filter for type
         addFiltersUsage(buffer, level + 1);
         buffer.append(generateIndent(level)).append("</set>").append(NEW_LINE);
//      }
//      else {
//         XXXXXXXX
//      }
   }

   /**
    * @param type
    * @return
    * @pre
    * @post
    */
   public static String getIdForImplementingType(OpPrototype type) {
      String shortName = shortNameMap.get(type.getInstanceClass());
      if (shortName == null) {
         shortName = type.getInstanceClass().getSimpleName()+":"+type.getInstanceClass().getPackage().getName();
      }
      return shortName;
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
            addRelationMember(buffer, prototype, (OpRelationship) member, level, false);
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

      if (field.getUnique() && field.getUniqueKey() == null) {
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

      if (field.getUnique() && field.getTypeID() != OpType.TEXT && field.getUniqueKey() == null) {
         buffer.append(" unique=\"true\"");
      }

      if (field.getUniqueKey() != null && field.getUniqueKey().trim().length() != 0) {
         buffer.append(" unique-key=\"").append(field.getUniqueKey()).append("\"");
      }

      String defaultValue = field.getDefaultValue();
      if (defaultValue != null && defaultValue.trim().length() != 0) {
         if (field.getTypeID() == OpType.BOOLEAN) {
            defaultValue = getDefautBooleanValue(defaultValue, databaseType);
         }
         if (field.getTypeID() == OpType.STRING || field.getTypeID() == OpType.TEXT) {
            defaultValue = "'" + defaultValue + "'";
         }
         buffer.append(" default=\"").append(defaultValue).append("\"");
//         buffer.append(" not-null=\"false\"");
      }

      if (field.getIndexed() && field.getTypeID() != OpType.TEXT) {
         if (databaseType != OpHibernateSource.ORACLE || !field.getUnique()) {
            buffer.append(" index=\"");
            buffer.append(generateIndexName(prototype.getName(), field.getName()));
            buffer.append('\"');
         }
      }

      if (field.getTypeID() == OpType.TEXT) {
         buffer.append(" length=\"").append(OpTypeManager.MAX_TEXT_LENGTH).append("\"");
      }

      buffer.append("/></property>").append(NEW_LINE);
   }

   public static String getDefautBooleanValue(String defaultValue, int databaseType) {
	   boolean value = false;
	   if ("true".equalsIgnoreCase(defaultValue)) {
		   value = true;
	   }	
	   else if ("1".equals(defaultValue)) {
		   value = true;		   
	   }
	  
	   final BooleanType type = new BooleanType();
       Dialect dialect = OpHibernateSource.getDialect(databaseType);
       try {
    	   String retval = type.objectToSQLString(value, dialect);
    	   return retval;
       } catch (Exception e) {
    	   logger.error(e);
    	   return defaultValue;
	}
}

/**
    * Add to mapping content prototype relations
    *
    * @param buffer       bufffer where to add mapping
    * @param prototype    prototype for which we add relatiojship
    * @param relationship relationship to map
    * @param level        indent level
    * @param isInterface 
    */
   private void addRelationMember(StringBuffer buffer, OpPrototype prototype, OpRelationship relationship, int level, boolean isInterface) {
      // Map relationship
      String cascadeMode = relationship.getCascadeMode();
      String fetch = relationship.getFetch();
      String lazy = relationship.getLazy();
      String orderBy = relationship.getOrderBy();
      String sort = relationship.getSort();
      OpRelationship back_relationship = relationship.getBackRelationship();
      OpPrototype target_prototype = OpTypeManager.getPrototypeByID(relationship.getTypeID());
      if (relationship.getCollectionTypeID() != OpType.SET) {
         // Map one-to-one or many-to-one relationship
         if (target_prototype.isInterface()) {
            String relName = relationship.getName();
//            relName = relName.substring(0, 1).toLowerCase()+relName.substring(1);
            buffer.append(generateIndent(level)).
            append("<any name=\"").append(relName).append("\"").
            append(" meta-type=\"string\"").
            append(" id-type=\"long\"").
            append(">").append(NEW_LINE);

            Iterator<OpPrototype> iter = target_prototype.subTypes();
            while (iter.hasNext()) {
               OpPrototype implementingType = iter.next();
               buffer.append(generateIndent(level+1)).
               append("<meta-value value=\"").append(getIdForImplementingType(implementingType)).
               append("\" class=\"").append(implementingType.getInstanceClass().getName()).append("\"/>").
               append(NEW_LINE);
            }
            buffer.append(generateIndent(level+1));
            buffer.append("<column name=\"").append(COLUMN_NAME_PREFIX).append(relationship.getName()).append(TYPE_SUFFIX).append(COLUMN_NAME_POSTFIX).append("\"/>").
            append(NEW_LINE);
            buffer.append(generateIndent(level+1)).
            append("<column name=\"").append(COLUMN_NAME_PREFIX).append(relationship.getName()).append(ID_SUFFIX).append(COLUMN_NAME_POSTFIX).append("\"/>").
            append(NEW_LINE);
            buffer.append(generateIndent(level)).
            append("</any>").
            append(NEW_LINE);
         }
         else if ((back_relationship != null) && (back_relationship.getCollectionTypeID() != OpType.SET)) {
            // Map one-to-one relationship
            if (relationship.getInverse()) {
               buffer.append(generateIndent(level)).append("<one-to-one name=\"");
               buffer.append(relationship.getName());
               buffer.append("\" property-ref=\"");
               buffer.append(back_relationship.getName());
               if (cascadeMode != null) {
                  buffer.append("\" cascade=\"").append(cascadeMode);
               }
               if (fetch != null) {
                  buffer.append("\" fetch=\"").append(fetch);
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
                  buffer.append("\" cascade=\"").append(cascadeMode);
               }
               if (fetch != null) {
                  buffer.append("\" fetch=\"").append(fetch);
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
//            if (relationship.getBackRelationship() == null) {
//               buffer.append("\" foreign-key=\"").append(prototype.getName()+"_"+relationship.getName());
//            }
            buffer.append("\" class=\"");
            buffer.append(target_prototype.getInstanceClass().getName());
            if (cascadeMode != null) {
               buffer.append("\" cascade=\"").append(cascadeMode);
            }
            if (fetch != null) {
               buffer.append("\" fetch=\"").append(fetch);
            }
            buffer.append("\"/>").append(NEW_LINE);
         }
      }
      else if (back_relationship != null) {
         // Map one-to-many or many-to-many relationship
         if (back_relationship.getCollectionTypeID() != OpType.SET) {
            // Map one-to-many relationship
            if (isInterface) {
               appendInterface(buffer, prototype, relationship, level);
            }
            else {
               buffer.append(generateIndent(level)).append("<set name=\"");
               buffer.append(relationship.getName());
               if (relationship.getInverse()) {
                  buffer.append("\" inverse=\"true");
               }
               if (orderBy != null) {
                  buffer.append("\" order-by=\"").append(generateColumnName(orderBy));
               }
               if (sort != null) {
                  buffer.append("\" sort=\"").append(sort);
               }
               buffer.append("\" lazy=\"").append(lazy);
               if (cascadeMode != null) {
                  buffer.append("\" cascade=\"").append(cascadeMode);
               }
               if (fetch != null) {
                  buffer.append("\" fetch=\"").append(fetch);
               }
               buffer.append("\">").append(NEW_LINE);
               buffer.append(generateIndent(level + 1)).append("<key column=\"");
               buffer.append(generateColumnName(back_relationship.getName()));
//               buffer.append("\" foreign-key=\"").append(prototype.getName()+"_"+relationship.getName());
               buffer.append("\"/>").append(NEW_LINE);
               buffer.append(generateIndent(level + 1)).append("<one-to-many class=\"");
               buffer.append(target_prototype.getInstanceClass().getName());
               buffer.append("\"/>").append(NEW_LINE);

               // add if necessary filter for relationship
               addFiltersUsage(buffer, level + 1);

               buffer.append(generateIndent(level)).append("</set>").append(NEW_LINE);
            }
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
//            if (prototype.isInterface()) {
//               buffer.append(" where=\"").append(COLUMN_NAME_PREFIX).append(back_relationship.getName()).append(TYPE_SUFFIX).append(COLUMN_NAME_POSTFIX)
//               .append("='").append(getIdForImplementingType(prototype)).append("'\"");
//            }
            if (orderBy != null) {
               buffer.append("\" order-by=\"").append(generateColumnName(orderBy));
            }
            buffer.append("\" lazy=\"").append(lazy);
            if (cascadeMode != null) {
               buffer.append("\" cascade=\"").append(cascadeMode);
            }
            if (fetch != null) {
               buffer.append("\" fetch=\"").append(fetch);
            }
            buffer.append("\">").append(NEW_LINE);
            buffer.append(generateIndent(level + 1)).append("<key column=\"").append(key_column_name);
//            buffer.append("\" foreign-key=\"").append(prototype.getName()+"_"+relationship.getName());
            buffer.append("\"/>").append(NEW_LINE);
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
      if (prototype.isInterface()) {
         return;
      }
      buffer.append(NEW_LINE);
      buffer.append(generateIndent(level)).append("<joined-subclass name=\"");
      buffer.append(prototype.getInstanceClass().getName());
      buffer.append("\" table=\"");
      String table_name = generateTableName(prototype.getName());
      buffer.append(table_name);
      if (USE_DYNAMIC) {
         buffer.append("\" dynamic-insert=\"true");
         buffer.append("\" dynamic-update=\"true");
      }

//      buffer.append("\" polymorphism=\"implicit");

      if (prototype.getBatchSize() != null) {
         buffer.append("\" batch-size=\"").append(prototype.getBatchSize());
      }
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
         appendMapping(buffer, subType, level + 1);
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
      StringBuffer buffer = new StringBuffer(getColumnNamePrefix());
      if (generateUpperCase()) {
         buffer.append(property_name.toUpperCase());
      }
      else {
         buffer.append(property_name.toLowerCase());
      }
      buffer.append(getColumnNamePostFix());
      return buffer.toString();
   }

   /**
    * Generates table name
    *
    * @param prototype_name name of the prototype for which we need to generate table name
    * @return table name
    */
   public static String generateTableName(String prototype_name) {
      StringBuffer buffer = new StringBuffer(getTableNamePrefix());
      String prototype = removePrototypeNamePrefixes(prototype_name);
      if (generateUpperCase()) {
         buffer.append(prototype.toUpperCase());
      }
      else {
         buffer.append(prototype.toLowerCase());
      }
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
      StringBuffer buffer = new StringBuffer(getTableNamePrefix());
      if (generateUpperCase()) {
         buffer.append(prototype_name1.toUpperCase());
      }
      else {
         buffer.append(prototype_name1.toLowerCase());
      }
      buffer.append(JOIN_NAME_SEPARATOR);
      if (generateUpperCase()) {
         buffer.append(prototype_name2.toUpperCase());
      }
      else {
         buffer.append(prototype_name2.toLowerCase());
      }
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
      StringBuffer buffer = new StringBuffer(getTableNamePrefix());
      if (generateUpperCase()) {
         buffer.append(prototype_name.toUpperCase());
      }
      else {
         buffer.append(prototype_name.toLowerCase());
      }
      buffer.append(JOIN_NAME_SEPARATOR);
      if (generateUpperCase()) {
         buffer.append(property_name.toUpperCase());
      }
      else {
         buffer.append(property_name.toLowerCase());
      }
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
      if (generateUpperCase()) {
         buffer.append(property_name.toUpperCase());
      }
      else {
         buffer.append(property_name.toLowerCase());
      }
      buffer.append(getIndexNamePostfix());

      if (databaseType == OpHibernateSource.IBM_DB2 && buffer.length() > IBM_DB2_INDEX_NAME_LENGTH) {
         String indexName = buffer.substring(getTableNamePrefix().length());
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

   public static void main(String[] args) {
      if (args.length < 1) {
         System.out.println("usage java -cp lib OpHibernateSource dbtype <outfile>");
         System.out.println("  dbtype is one of derby, mssql, mysql_innodb, mysql, postgres, oracle, hsql or db2");
         System.out.println("  outfile is the filename to write the mapping to, stdout is used if this argument is not given");
         System.exit(-1);
      }
      int dbType = -1;
      if (args[0].equalsIgnoreCase("derby")) {
         dbType = OpHibernateSource.DERBY;
      }
      else if (args[0].equalsIgnoreCase("mysql_innodb")) {
         dbType = OpHibernateSource.MYSQL_INNODB;
      }
      else if (args[0].equalsIgnoreCase("mysql")) {
         dbType = OpHibernateSource.IBM_DB2;
      }
      else if (args[0].equalsIgnoreCase("postgres")) {
         dbType = OpHibernateSource.POSTGRESQL;
      }
      else if (args[0].equalsIgnoreCase("oracle")) {
         dbType = OpHibernateSource.ORACLE;
      }
      else if (args[0].equalsIgnoreCase("hsql")) {
         dbType = OpHibernateSource.HSQLDB;
      }
      else if (args[0].equalsIgnoreCase("db2")) {
         dbType = OpHibernateSource.IBM_DB2;
      }
      else if (args[0].equalsIgnoreCase("mssql")) {
         dbType = OpHibernateSource.MSSQL;
      }
      else {
         System.err.println("ERROR unknown dbtype '" + args[0] + "'");
         System.exit(-1);
      }
      PrintStream out = System.out;
      if (args.length > 1) {
         try {
            File file = new File(args[1]);
            if (file.exists()) {
               file.delete();
            }
            file.createNewFile();
            out = new PrintStream(file);
         }
         catch (IOException exc) {
            System.err.println("ERROR could not create file '" + args[1] + "', error: " + exc.getLocalizedMessage());
            System.exit(-1);
         }
      }

      OpEnvironmentManager.setOnePointHome(System.getProperty("user.dir"));
      XResourceBroker.setResourcePath("onepoint/project");
      // initialize factory
      OpInitializerFactory factory = OpInitializerFactory.getInstance();
      OpInitializer initializer = factory.setInitializer(OpInitializer.class);
      initializer.init(OpProjectConstants.OPEN_EDITION_CODE);
      OpMappingsGenerator generator = new OpMappingsGenerator(dbType);
      generator.init(OpTypeManager.getPrototypes());
      String mapping = generator.generateMappings();
      out.print(mapping);
   }

   private int getDatabaseType() {
      return databaseType;
   }

   private static boolean generateUpperCase() {
      if (generator != null) {
         return generator.getDatabaseType() == OpHibernateSource.ORACLE;
      }
      else {
         return false;
      }
   }

   public static String getIndexNamePrefix() {
      if (generateUpperCase()) {
         return INDEX_NAME_PREFIX.toUpperCase();
      }
      return INDEX_NAME_PREFIX;
   }


   public static String getIndexNamePostfix() {
      if (generateUpperCase()) {
         return INDEX_NAME_POSTFIX.toUpperCase();
      }
      return INDEX_NAME_POSTFIX;
   }

   public static String getColumnNamePrefix() {
      if (generateUpperCase()) {
         return COLUMN_NAME_PREFIX.toUpperCase();
      }
      return COLUMN_NAME_PREFIX;
   }

   public static String getColumnNamePostFix() {
      if (generateUpperCase()) {
         return COLUMN_NAME_POSTFIX.toUpperCase();
      }
      return COLUMN_NAME_POSTFIX;
   }

   public static String getTableNamePrefix() {
      if (generateUpperCase()) {
         return TABLE_NAME_PREFIX.toUpperCase();
      }
      return TABLE_NAME_PREFIX;
   }

   public static OpMappingsGenerator getInstance(int databaseType) {
      if (generator == null) {
         generator = new OpMappingsGenerator(databaseType);
      }
      return generator;
   }
}
