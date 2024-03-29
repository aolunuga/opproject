/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

public class OpTypeManager {

   /**
    * The maximum number of characters for the Text type
    */
   public static final int MAX_TEXT_LENGTH = 2500;

   /**
    * The maximum number of characters for String type
    */
   public static final int MAX_STRING_LENGTH = 255;

   // Class logger.
   private static final XLog logger = XLogFactory.getLogger(OpTypeManager.class);

   private static final String CGLIB_ENHANCER_PREFIX = "$$";

   private static Map<String, OpType> types;
   private static Map<String, OpPrototype> prototypes;
   private static Map<Integer, OpPrototype> prototypeIds;
   private static Map<String, OpType> classNames;
   private static int minTypeId; // Used to register basic types
   private static int maxTypeId; // Used to register prototypes
   private static boolean locked; // If locked new types cannot be added

   static {
      // Initialize hashtable and type-ID counters
      types = new HashMap<String, OpType>();
      prototypes = new LinkedHashMap<String, OpPrototype>();
      prototypeIds = new HashMap<Integer, OpPrototype>();
      classNames = new HashMap<String, OpType>();
      minTypeId = 0;
      maxTypeId = 0;
      locked = false;
      // Register built-in types (in order of constant type-IDs)
      registerType(new OpType("Boolean", "java.lang.Boolean"));
      registerType(new OpType("Integer", "java.lang.Integer"));
      registerType(new OpType("Long", "java.lang.Long"));
      registerType(new OpType("String", "java.lang.String"));
      registerType(new OpType("Date", "java.sql.Date"));
      // *** Think about collection-types once more (not ideal yet)
      registerType(new OpType("Set", "java.util.Set", true));
      registerType(new OpType("Content", "onepoint.service.XContent"));
      registerType(new OpType("Byte", "java.lang.Byte"));
      registerType(new OpType("Double", "java.lang.Double"));
      registerType(new OpType("Timestamp", "java.util.Date"));
      registerType(new OpType("Text", "java.lang.String"));
      // Load and register built-in protoypes "OpObject"
//      OpPrototype object_prototype = new OpPrototypeLoader().loadPrototype("onepoint/persistence/object.opt.xml");
//      registerPrototype(object_prototype);
   }

   public static void registerType(OpType type) { // synchronized
      // *** Throw exception is type-manager is locked
      // To do: Check uniqueness of name and throw exception
      // To do: Check type
      if (type instanceof OpPrototype) {
         registerPrototype((OpPrototype) type);
      }
      else {
         // For all basic types, type-IDs are counted down
         minTypeId--;
         type.setID(minTypeId);
         // Invoke callback
         type.onRegister(); // To do: Error handling?
         // Add to type-registry
         types.put(type.getName(), type);
         classNames.put(type.getInstanceClass().getName(), type);
      }
   }

   public static void registerPrototype(OpPrototype prototype) { // synchronized
      // *** Throw exception is type-manager is locked
      // To do: Check name and type (instanceof)

      logger.debug("OpTypeManager.registerPrototype() : " + prototype.getName());

      // Call on-register callback
      prototype.onRegister();

      logger.debug("OpTypeManager.registerPrototype() : object-class : " + prototype.getInstanceClass().getName());

      // For prototypes, type-IDs are counted up
      maxTypeId++;
      prototype.setID(maxTypeId);
      types.put(prototype.getName(), prototype);
      classNames.put(prototype.getInstanceClass().getName(), prototype);
      prototypes.put(prototype.getName(), prototype);
      prototypeIds.put(new Integer(maxTypeId), prototype);
      // Assign unique IDs to members (inside inheritance-chain)
      // *** We must assume here that all super-types are already registered (check it)!
      int max_member_id = -1;
      if (prototype.getSuperType() != null) {
         max_member_id = prototype.getSuperType().getSize();
      }
      // Resolve type-IDs and set member-ID
      Iterator members = prototype.getDeclaredMembers();
      while (members.hasNext()) {
         OpMember member = (OpMember) (members.next());
         max_member_id++;
         member.setID(max_member_id);
         // Resolve type-ID for fields ONLY: Relationships are resolved at lock()
         if (member instanceof OpField) {
            OpType type = OpTypeManager.getType(member.getTypeName());
            if (type == null) {
               // *** Throw exception
               logger.warn("No such type: " + member.getTypeName() + " for field " + prototype.getName() + "." + member.getName());
            }
            // Collection-types are not allowed here
            if (type.isCollectionType()) {
               // *** Throw exception
               logger.warn("Collection-type specified as relationship or field type");
            }
            member.setTypeID(type.getID());
            // Resolve collection-type-ID
            if ((member.getCollectionTypeName() != null) && (!(member.getCollectionTypeName().equals("false")))) {
               OpType collection_type = OpTypeManager.getType(member.getCollectionTypeName());
               if (collection_type == null) {
                  // *** Throw exception
                  logger.warn("No such collection-type");
               }
               if (!(collection_type.isCollectionType())) {
                  // *** Throw exception
                  logger.warn("Basic type or prototype specified as collection-type");
               }
               member.setCollectionTypeID(collection_type.getID());
            }
         }
      }
   }

   public static void lock() {
      // *** Check if it is already locked?
      // Resolve relationships between all registered prototypes
      Collection<OpPrototype> prototypes = OpTypeManager.prototypes.values();
      for (OpPrototype prototype : prototypes) {
         Iterator members = prototype.getDeclaredMembers();
         while (members.hasNext()) {
            OpMember member = (OpMember) (members.next());
            // Resolve type-ID and collection-type-ID for relationships
            if (member instanceof OpRelationship) {
               OpType type = OpTypeManager.getType(member.getTypeName());
               if (type == null) {
                  // *** Throw exception
                  logger.warn("No such type: " + member.getTypeName() + " for relationship " + prototype.getName() + "." + member.getName());
               }
               // Collection-types are not allowed here
               if (type.isCollectionType()) {
                  // *** Throw exception
                  logger.warn("Collection-type specified as relationship or field type");
               }
               member.setTypeID(type.getID());
               // Resolve collection-type-ID
               if ((member.getCollectionTypeName() != null) && (!(member.getCollectionTypeName().equals("false")))) {
                  OpType collection_type = OpTypeManager.getType(member.getCollectionTypeName());
                  if (collection_type == null) {
                     // *** Throw exception
                     logger.warn("No such collection-type: " + member.getCollectionTypeName());
                  }
                  if (!(collection_type.isCollectionType())) {
                     // *** Throw exception
                     logger.warn("Basic type or prototype specified as collection-type: " + prototype.getName() + "." + member.getName() + " " + member.getCollectionTypeName());
                  }
                  member.setCollectionTypeID(collection_type.getID());
               }
               // Resolve inverse-relationships
               String inverse_name = ((OpRelationship) member).getBackRelationshipName();
               if (inverse_name != null) {
                  OpPrototype related_prototype = OpTypeManager.getPrototypeByID(type.getID());
                  OpMember inverse = related_prototype.getDeclaredMember(inverse_name);
                  if ((inverse == null) || (!(inverse instanceof OpRelationship))) {
                     // *** Throw exception
                     logger.warn("No inverse relationship " + related_prototype.getName() + "." + inverse_name);
                  }
                  ((OpRelationship) member).setBackRelationship((OpRelationship) inverse);
               }
            }
         }
      }
      // Lock the type-manager (no new types can be added)
      locked = true;
   }

   public static Iterator<OpPrototype> getPrototypes() {
      return prototypes.values().iterator();
   }

   public static OpType getType(String name) {
      return (OpType) (types.get(name));
   }

   public static OpPrototype getPrototype(String name) {
      return (OpPrototype) (prototypes.get(name));
   }

   public static OpPrototype getPrototypeByID(int id) {
      return (OpPrototype) (prototypeIds.get(new Integer(id)));
   }

   public static OpPrototype getPrototypeByClass(Class type) {
      return getPrototypeByClassName(type.getName());
   }

   public static OpPrototype getPrototypeByClassName(String className) {
      // check if we have an enhanced class (Proxy) or not.
      int index = className.indexOf(CGLIB_ENHANCER_PREFIX);
      if (index != -1) {
         className = className.substring(0, index);
      }

      return (OpPrototype) classNames.get(className);
   }

   public static OpPrototype getPrototypeForObject(Object object) {
      return getPrototypeByClassName(object.getClass().getName());
   }

}
