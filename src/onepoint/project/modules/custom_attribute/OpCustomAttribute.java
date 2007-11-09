/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.Date;

import onepoint.persistence.OpObject;

/**
 * @author dfreis
 *
 */
public class OpCustomAttribute extends OpObject {
 
   private String name;
   private int type;
   private boolean unique = false;
   private boolean mandatory = false;
   private int position = -1;
   private OpCustomType customType;
   private boolean deleted = false;
   
   public static final Class[] CLASS_TYPES = 
   { Boolean.class, Long.class, Double.class, Date.class, String.class };

   public static final String[] DB_TYPES = 
   { "Booleans", "Number", "Decimal", "Date", "Text" };

   public static final int BOOLEAN = 0;
   public static final int NUMBER = 1;
   public static final int DECIMAL = 2;
   public static final int DATE = 3;
   public static final int TEXT = 4;
   
   /**
    * 
    */
   public OpCustomAttribute() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      if (name == null) {
         throw new IllegalArgumentException("name must not be null");
      }
      this.name = name;
   }
   
   public int getType() {
      return type;
   }
   
   public String getDBType(int pos) {
      if (position < 0) {
         throw new IllegalArgumentException("position must be > 0");
      }
      // FIXME(dfreis Oct 8, 2007 7:28:32 PM) only until we implement multiple pages
      if (position > 9) {
         throw new IllegalArgumentException("position must be <= 9");
      }
      if (type == BOOLEAN) {
         return DB_TYPES[BOOLEAN];
      }
      return DB_TYPES[type]+pos;
   }

   public void setType(int type) {
      if ((type < 0) || (type >= CLASS_TYPES.length)) {
         throw new IllegalArgumentException("type must be within [0.."+CLASS_TYPES.length+"]");
      }
      this.type = type;
   }

   public void setTypeFromClass(Class classType) {
      int type = -1;
      for (int count = 0; count < CLASS_TYPES.length; count++) {
         if (CLASS_TYPES[count] == classType) {
            type = count;
            break;
         }
      }
      if (type < 0) {
         throw new IllegalArgumentException("unsupported class type '"+classType.getName()+"'");
      }
      this.type = type;
   }

   
   public int getPosition() {
      return position;
   }
   
   public void setPosition(int position) {
      if (position < 0) {
         throw new IllegalArgumentException("position must be > 0");
      }
      // FIXME(dfreis Oct 8, 2007 7:28:32 PM) only until we implement multiple pages
      if (position > 9) {
         throw new IllegalArgumentException("position must be <= 9");
      }
      this.position  = position;
   }
   public Class getTypeAsClass() {
      return CLASS_TYPES[type];
   }
   
   public void setUnique(boolean unique) {
      this.unique = unique;
   }
   
   public boolean isUnique() {
      return unique;
   }

   public void setMandatory(boolean mandatory) {
      this.mandatory = mandatory;
   }
   
   public boolean isMandatory() {
      return mandatory;
   }  

   public OpCustomType getCustomType() {
      return customType;
   }
   
   // FIXME(dfreis Oct 9, 2007 4:08:24 PM) should only be called by service!
   public void setCustomType(OpCustomType customType) {
      this.customType = customType;
   }

   public boolean isDeleted() {
      return deleted;
   }

   /**
    * @param b
    * @pre
    * @post
    */
   public void setDeleted(boolean deleted) {
      this.deleted = deleted; 
   }
}
