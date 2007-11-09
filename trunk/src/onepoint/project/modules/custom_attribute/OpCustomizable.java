/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.Date;

/**
 * Interface to access custom attributes.
 * All OpObjects that can store custom attributes implement this interface.
 *
 * @author dfreis
 */

public interface OpCustomizable {

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
   public abstract Long getNumber(String name)
   throws IllegalArgumentException;

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
   public abstract Date getDate(String name)
   throws IllegalArgumentException;

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
   public abstract String getText(String name)
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

}