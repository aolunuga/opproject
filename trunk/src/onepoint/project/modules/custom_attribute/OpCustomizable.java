/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.Date;
import java.util.Set;

import onepoint.persistence.OpObjectIfc;
import onepoint.project.modules.project.OpAttachment;

/**
 * @author dfreis
 *
 */
public interface OpCustomizable extends OpObjectIfc {

   public static final int CUSTOM_VALUES_SIZE = 10;

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getBoolean(java.lang.String)
    */
   public abstract Object getObject(String name)
         throws IllegalArgumentException;

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getBoolean(java.lang.String)
    */
   public abstract void setObject(String name, Object value)
         throws IllegalArgumentException;

   /**
    * Gets the boolean value for the custom attribute <code>name</code>.
    * @param name the name of the custom attribute.
    * @param value the value to set. the custom value.
    * @throws IllegalArgumentException if <code>name</code> is not a known custom attribute
    */
   public abstract Boolean getBoolean(String name)
         throws IllegalArgumentException;

   /**
    * Sets the boolean <code>value</code> for the custom attribute <code>name</code>.
    * Unset the value by passing <code>null</code> as <code>value</code>.
    * @param name the name of the custom attribute. the name of the custom attribute.
    * @param value the value to set.
    * @throws IllegalArgumentException if <code>name</code> is not a known custom attribute
    */
   public abstract void setBoolean(String name, Boolean value)
         throws IllegalArgumentException;

   /**
    * Gets the number value for the custom attribute <code>name</code>.
    * @param name the name of the custom attribute.
    * @return the custom value.
    * @throws IllegalArgumentException if <code>name</code> is not a known custom attribute
    */
   public abstract Long getNumber(String name) throws IllegalArgumentException;

   /**
    * Sets the number <code>value</code> for the custom attribute <code>name</code>.
    * Unset the value by passing <code>null</code> as <code>value</code>.
    * @param name the name of the custom attribute.
    * @param value the value to set.
    * @throws IllegalArgumentException if <code>name</code> is not a known custom attribute
    */
   public abstract void setNumber(String name, Long value)
         throws IllegalArgumentException;

   /**
    * Gets the decimal value for the custom attribute <code>name</code>.
    * @param name the name of the custom attribute.
    * @return the custom value.
    * @throws IllegalArgumentException if <code>name</code> is not a known custom attribute
    */
   public abstract Double getDecimal(String name)
         throws IllegalArgumentException;

   /**
    * Sets the decimal <code>value</code> for the custom attribute <code>name</code>.
    * Unset the value by passing <code>null</code> as <code>value</code>.
    * @param name the name of the custom attribute.
    * @param value the value to set.
    * @throws IllegalArgumentException if <code>name</code> is not a known custom attribute
    */
   public abstract void setDecimal(String name, Double value)
         throws IllegalArgumentException;

   /**
    * Gets the date value for the custom attribute <code>name</code>.
    * @param name the name of the custom attribute.
    * @param value the value to set.
    * @throws IllegalArgumentException if <code>name</code> is not a known custom attribute
    */
   public abstract Date getDate(String name) throws IllegalArgumentException;

   /**
    * Sets the date <code>value</code> for the custom attribute <code>name</code>.
    * Unset the value by passing <code>null</code> as <code>value</code>.
    * @param name the name of the custom attribute.
    * @param value the value to set.
    * @throws IllegalArgumentException if <code>name</code> is not a known custom attribute
    */
   public abstract void setDate(String name, Date value)
         throws IllegalArgumentException;

   /**
    * Gets the text value for the custom attribute <code>name</code>.
    * @param name the name of the custom attribute.
    * @return the custom value.
    * @throws IllegalArgumentException if <code>name</code> is not a known custom attribute
    */
   public abstract String getText(String name) throws IllegalArgumentException;

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getText(java.lang.String)
    */
   public abstract OpCustomTextValue getMemo(String name)
         throws IllegalArgumentException;

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getDocument(java.lang.String)
    */
   public abstract OpAttachment getAttachment(String name)
         throws IllegalArgumentException;

   /**
    * Sets the text <code>value</code> for the custom attribute <code>name</code>.
    * Unset the value by passing <code>null</code> as <code>value</code>.
    * @param name the name of the custom attribute.
    * @param value the value to set.
    * @throws IllegalArgumentException if <code>name</code> is not a known custom attribute
    */
   public abstract void setText(String name, String value)
         throws IllegalArgumentException;

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setText(java.lang.String, java.lang.String)
    */
   public abstract void setMemo(String name, OpCustomTextValue value)
         throws IllegalArgumentException;

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setDocument(java.lang.String, onepoint.project.modules.attachments.OpDocument)
    */
   public abstract void setAttachment(String name, OpAttachment value)
         throws IllegalArgumentException;

   public abstract Set<OpCustomValuePage> getCustomValuePages();

   public abstract void setCustomValuePages(Set<OpCustomValuePage> pages);

   public abstract OpCustomValuePage getCustomValuePage();

   public abstract void setCustomValuePage(OpCustomValuePage customValuePage);

}