/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.custom_attribute;

import onepoint.error.XErrorMap;

public class OpCustomAttributeErrorMap extends XErrorMap {

   public static final String RESOURCE_MAP_ID = "custom_attributes.error";

   public static final OpCustomAttributeErrorMap ERROR_MAP = new OpCustomAttributeErrorMap();
   
   public OpCustomAttributeErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpCustomAttributeError.EMPTY_NAME_ERROR, OpCustomAttributeError.EMPTY_NAME_ERROR_NAME);
      registerErrorCode(OpCustomAttributeError.NAME_NOT_UNIQUE_ERROR, OpCustomAttributeError.NAME_NOT_UNIQUE_NAME);
      registerErrorCode(OpCustomAttributeError.INSUFICIENT_PERMISSIONS_ERROR, OpCustomAttributeError.INSUFICIENT_PERMISSIONS_ERROR_NAME);
      registerErrorCode(OpCustomAttributeError.INVALID_POSITION_ERROR, OpCustomAttributeError.INVALID_POSITION_ERROR_NAME);
      registerErrorCode(OpCustomAttributeError.MANDATORY_ERROR, OpCustomAttributeError.MANDATORY_ERROR_NAME);
      registerErrorCode(OpCustomAttributeError.NOT_UNIQUE_ERROR, OpCustomAttributeError.NOT_UNIQUE_NAME);
   }

}
