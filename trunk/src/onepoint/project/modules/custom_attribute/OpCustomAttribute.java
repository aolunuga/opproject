/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.Date;

import com.sun.msv.grammar.util.PossibleNamesCollector;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAttachment;

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
   private String possibleStringValues = null;
   /** constants for forms */
   public static final String NAME = "Name";
   public static final String DESCRIPTION = "Description";
   public static final String LABEL = "Label";
   public static final String TYPE = "Type";
   public static final String ROLE = "Role";
   public static final String MANDATORY = "Mandatory";
   public static final String UNIQUE = "Unique";
   public final static String SEQUENCE = "Sequence";
   public static final String TAB_LABEL = "TabLabel";

//   public static final Class[] CLASS_TYPES = 
//   { Boolean.class, Long.class, Double.class, Date.class, String.class, OpCustomTextValue.class, OpAttachment.class};

//   public static final String[] CLASS_TYPE_NAMES = 
//   { "Booleans", "Long", "Double", "Date", "String",  "OpCustomTextValue", "OpAttachment", "Choice"};

   private static final String[] DB_TYPES = 
   { "Booleans", "Number", "Decimal", "Date", "Text",  "Memo", "Attachment", "Choice"};

   public static final String[] DB_TYPES_NAMES = 
   { "Boolean", "Number", "Decimal", "Date", "Text",  "Memo", "Attachment", "Choice"};


   public static final int BOOLEAN = 0;
   public static final int NUMBER = 1;
   public static final int DECIMAL = 2;
   public static final int DATE = 3;
   public static final int TEXT = 4;
   public static final int MEMO = 5;
   public static final int ATTACHMENT = 6;
   public static final int CHOICE = 7;
   
   /**
    * 
    */
   public OpCustomAttribute() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
//      if (name == null) {
//         throw new IllegalArgumentException("name must not be null");
//      }
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
      if ((type < 0) || (type >= DB_TYPES.length)) {
         throw new IllegalArgumentException("type must be within [0.."+DB_TYPES.length+"]");
      }
      this.type = type;
   }

   public static int getTypeFromName(String typeName) {
	   for (int count = 0; count < DB_TYPES_NAMES.length; count++) {
		   if (DB_TYPES_NAMES[count].equals(typeName)) {
			   return count;
		   }
	   }
	   throw new IllegalArgumentException("unsupported class type '"+typeName+"'");
   }

   public static String getTypeName(int type) {
	   return DB_TYPES_NAMES[type];
   }
//   public void setTypeFromClass(Class classType) {
//      this.type = getTypeFromClass(classType);
//   }

   
   public int getPosition() {
      return position;
   }
   public void resetPosition() {
	   position = -1;
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
   
   public String getPossibleStringValues() {
	   return possibleStringValues;
   }

   public String[] getPossibleStringValuesAsArray() {
	   return possibleStringValues == null ? null : possibleStringValues.split(";");
   }

   public void setPossibleStringValues(String[] values) {
	   StringBuffer buffer = new StringBuffer();
	   for (int pos = 0; pos < values.length; pos++) {
		   if (pos > 0) {
			   buffer.append(";");
		   }
		   buffer.append(values[pos].trim());
	   }
	   setPossibleStringValues(buffer.toString());
   }

   public void setPossibleStringValues(String possibleStringValues) {
	   this.possibleStringValues = possibleStringValues;
   }

@Override
   public String toString() {
      return "{OpCustomAttribute: N:"+name+" D:"+description+" L:"+label+" T:"+type+
      " M:"+mandatory+" P:"+position+" S:"+sequence+" d: "+deleted+" m:"+minRole+"}";
   }
}

