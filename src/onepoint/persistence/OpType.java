/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.lang.reflect.Constructor;

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

   private String name;
   private int id; // Type-id, set by type-manager
   private String className;
   private Class clazz; // Resolved on reading XML-file
   private boolean collectionType; // True if this is a collection-type

   private static final XLog logger = XLogFactory.getLogger(OpType.class);

   public OpType() {
   }

   public OpType(String name, String class_name) {
      this.name = name;
      this.className = class_name;
   }

   public OpType(String name, String class_name, boolean collection_type) {
      this.name = name;
      this.className = class_name;
      collectionType = collection_type;
   }

   public final void setName(String name) {
      this.name = name;
   }

   public final String getName() {
      return name;
   }

   final void setID(int id) {
      // Package-local method, only called by type-manager
      this.id = id;
   }

   public final int getID() {
      return id;
   }

   final void setClassName(String class_name) {
      this.className = class_name;
   }

   /*
   public final void setInstanceClass(Class instance_class) {
     clazz = instance_class;
   }
   */

   public final Class getInstanceClass() {
      if (clazz == null) {
         try {
            clazz = Class.forName(className); // To do: Exception/error handling
         }
         catch (Exception e) {
            logger.error("OpType.getInstanzeClass: ", e);
         }
      }
      return clazz;
   }

   public final boolean isCollectionType() {
      return collectionType;
   }

   public final Object newInstance() {
      Object object = null;
      try {
         Constructor constr = clazz.getDeclaredConstructor(new Class[0]);
         constr.setAccessible(true);
         object = constr.newInstance(new Object[0]);
      }
      catch (Exception e) {
         logger.error("OpType.newInstance: ", e);
      }
      return object;
   }

   // Callbacks invoked by type-manager

   public void onRegister() {
      try {
         clazz = Class.forName(className); // To do: Exception/error handling
      }
      catch (Exception e) {
         logger.error("OpType.onRegister: ", e);

      }
   }

   public void onDeregister() {
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      return getName().equals(((OpType)obj).getName());
   }
}
