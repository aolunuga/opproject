/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.persistence;

import onepoint.project.modules.custom_attribute.OpCustomType;

/**
 * @author dfreis
 *
 */
public interface OpCustomSubTypable {
   
   public final static String CUSTOM_TYPE_NAME = "CustomTypeName";
   
   public OpCustomType getCustomType();

   public void setCustomType(OpCustomType customType);
}
