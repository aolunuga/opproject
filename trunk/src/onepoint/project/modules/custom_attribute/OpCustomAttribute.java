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
   private String description;
   private String label;
   private int type;
   private boolean unique = false;
   private boolean mandatory = false;
   private int position = -1;
   private int sequence;
   private OpCustomType customType;
   private boolean deleted = false;
   private Byte minRole = null;
   
   /** constants for forms */
   public static final String NAME = "Name";
   public static final String DESCRIPTION = "Description";
   public static final String LABEL = "Label";
   public static final String TYPE = "Type";
   public static final String ROLE = "Role";
   public static final String MANDATORY = "Mandatory";
   public static final String UNIQUE = "Unique";
   public final static String SEQUENCE = "Sequence";

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
   public static int getTypeFromClass(Class classType) {
      for (int count = 0; count < CLASS_TYPES.length; count++) {
         if (CLASS_TYPES[count] == classType) {
            return count;
         }
      }
      throw new IllegalArgumentException("unsupported class type '"+classType.getName()+"'");
   }

   public void setTypeFromClass(Class classType) {
      this.type = getTypeFromClass(classType);
   }

   
   public int getPosition() {
      return position;
   }
   
   public void setPosition(int position) {
      if (position < 0) {
         throw new IllegalArgumentException("position must be >= 0");
      }
      // FIXME(dfreis Oct 8, 2007 7:28:32 PM) only until we implement multiple pages
      if (position > 9) {
         throw new IllegalArgumentException("position must be <= 9");
      }
      this.position  = position;
   }

   public void setSequence(int sequence) {
      if (sequence < 0) {
         throw new IllegalArgumentException("sequence must be >= 0, sequence was: "+sequence);
      }
      this.sequence = sequence;
   }

   public int getSequence() {
      return sequence;
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

   public Byte getMinRole() {
      return minRole;
   }

   public void setMinRole(Byte minRole) {
      this.minRole = minRole;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getLabel() {
      return label;
   }

   public void setLabel(String label) {
      this.label = label;
   }
}
