/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

public class OpType { // Maybe an additional sub-class "XBasicType"?

   // *** Attention: Hardcoded type-IDs are basically not used anymore!

   public final static int UNDEFINED = 0;

   public final static int BOOLEAN = -1;
   public final static int INTEGER = -2;
   public final static int LONG = -3;
   public final static int STRING = -4;
   public final static int DATE = -5;
   public final static int SET = -6;
   public final static int CONTENT = -7;
   public final static int BYTE = -8;
   public final static int DOUBLE = -9;
   public final static int TIMESTAMP = -10;
   public final static int TEXT = -11;

   private String _name;
   private int _id; // Type-id, set by type-manager
   private String _class_name;
   private Class _class; // Resolved on reading XML-file
   private boolean _collection_type; // True if this is a collection-type

   private static final XLog logger = XLogFactory.getLogger(OpType.class);

   public OpType() {
   }

   public OpType(String name, String class_name) {
      _name = name;
      _class_name = class_name;
   }

   public OpType(String name, String class_name, boolean collection_type) {
      _name = name;
      _class_name = class_name;
      _collection_type = collection_type;
   }

   public final void setName(String name) {
      _name = name;
   }

   public final String getName() {
      return _name;
   }

   final void setID(int id) {
      // Package-local method, only called by type-manager
      _id = id;
   }

   public final int getID() {
      return _id;
   }

   final void setClassName(String class_name) {
      _class_name = class_name;
   }

   /*
   public final void setInstanceClass(Class instance_class) {
     _class = instance_class;
   }
   */

   public final Class getInstanceClass() {
      return _class;
   }

   public final boolean isCollectionType() {
      return _collection_type;
   }

   public final Object newInstance() {
      Object object = null;
      try {
         object = _class.newInstance();
      }
      catch (Exception e) {
         logger.error("OpType.newInstance: ", e);
      }
      return object;
   }

   // Callbacks invoked by type-manager

   public void onRegister() {
      try {
         _class = Class.forName(_class_name); // To do: Exception/error handling
      }
      catch (Exception e) {
         logger.error("OpType.onRegister: ", e);

      }
   }

   public void onDeregister() {
   }

}
