/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.error.XErrorMap;

public class OpWorkErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "work.error";

   OpWorkErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpWorkError.DATE_MISSING, OpWorkError.DATE_MISSING_NAME);
      registerErrorCode(OpWorkError.DUPLICATE_DATE, OpWorkError.DUPLICATE_DATE_NAME);
      registerErrorCode(OpWorkError.INCORRECT_ACTUAL_EFFORT, OpWorkError.INCORRECT_ACTUAL_EFFORT_NAME);
      registerErrorCode(OpWorkError.INCORRECT_EXTERNAL_COSTS, OpWorkError.INCORRECT_EXTERNAL_COSTS_NAME);
      registerErrorCode(OpWorkError.INCORRECT_MATERIAL_COSTS, OpWorkError.INCORRECT_MATERIAL_COSTS_NAME);
      registerErrorCode(OpWorkError.INCORRECT_MISCELLANEOUS_COSTS, OpWorkError.INCORRECT_MISCELLANEOUS_COSTS_NAME);
      registerErrorCode(OpWorkError.INCORRECT_REMAINING_EFFORT, OpWorkError.INCORRECT_REMAINING_EFFORT_NAME);
      registerErrorCode(OpWorkError.INCORRECT_TRAVEL_COSTS, OpWorkError.INCORRECT_TRAVEL_COSTS_NAME);
   }

}
