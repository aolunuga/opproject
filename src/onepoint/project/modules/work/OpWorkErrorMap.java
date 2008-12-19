/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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
      registerErrorCode(OpWorkError.WORK_SLIP_NOT_FOUND, OpWorkError.WORK_SLIP_NOT_FOUND_NAME);
      registerErrorCode(OpWorkError.CREATOR_MISSING, OpWorkError.CREATOR_MISSING_NAME);
      registerErrorCode(OpWorkError.WORK_RECORDS_MISSING, OpWorkError.WORK_RECORDS_MISSING_NAME);
      registerErrorCode(OpWorkError.START_TIME_IS_NEGATIVE, OpWorkError.START_TIME_IS_NEGATIVE_NAME);
      registerErrorCode(OpWorkError.START_TIME_IS_TOO_LARGE, OpWorkError.START_TIME_IS_TOO_LARGE_NAME);
      registerErrorCode(OpWorkError.FINISH_TIME_IS_NEGATIVE, OpWorkError.FINISH_TIME_IS_NEGATIVE_NAME);
      registerErrorCode(OpWorkError.FINISH_TIME_IS_TOO_LARGE, OpWorkError.FINISH_TIME_IS_TOO_LARGE_NAME);
      registerErrorCode(OpWorkError.START_AFTER_FINISH, OpWorkError.START_AFTER_FINISH_NAME);
      registerErrorCode(OpWorkError.DURATION_NOT_VALID, OpWorkError.DURATION_NOT_VALID_NAME);
      registerErrorCode(OpWorkError.ACTUAL_COSTS_NOT_VALID, OpWorkError.ACTUAL_COSTS_NOT_VALID_NAME);
      registerErrorCode(OpWorkError.REMAINING_COSTS_NOT_VALID, OpWorkError.REMAINING_COSTS_NOT_VALID_NAME);
      registerErrorCode(OpWorkError.COST_TYPE_NOT_VALID, OpWorkError.COST_TYPE_NOT_VALID_NAME);
      registerErrorCode(OpWorkError.TIME_RECORDS_OVERLAP, OpWorkError.TIME_RECORDS_OVERLAP_NAME);
      registerErrorCode(OpWorkError.WORK_SLIP_NOT_EDITABLE, OpWorkError.WORK_SLIP_NOT_EDITABLE_NAME);
      registerErrorCode(OpWorkError.WORK_SLIP_IS_CONTROLLED, OpWorkError.WORK_SLIP_IS_CONTROLLED_NAME);
      registerErrorCode(OpWorkError.WORK_SLIP_PERMISSION_DENIED, OpWorkError.WORK_SLIP_PERMISSION_DENIED_NAME);
      registerErrorCode(OpWorkError.REMAINING_COSTS_OUTDATED, OpWorkError.REMAINING_COSTS_OUTDATED_NAME);
   }

}
