/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.custom_attribute;

public interface OpCustomAttributeError {

   // Error codes
   public static final int EMPTY_NAME_ERROR = 1;
   public static final int NAME_NOT_UNIQUE_ERROR = 2;
   public static final int INSUFICIENT_PERMISSIONS_ERROR = 3;
   public static final int INVALID_POSITION_ERROR = 4;
   public static final int MANDATORY_ERROR = 5;
   public static final int NOT_UNIQUE_ERROR = 6;

   // Error names
   public static final String EMPTY_NAME_ERROR_NAME = "EmptyName";
   public static final String NAME_NOT_UNIQUE_NAME = "NameNotUnique";
   public static final String INSUFICIENT_PERMISSIONS_ERROR_NAME = "InsuficientPermissions";
   public static final String INVALID_POSITION_ERROR_NAME = "InvalidPosition";
   public static final String MANDATORY_ERROR_NAME = "Mandatory";
   public static final String NOT_UNIQUE_NAME = "NotUnique";
}
