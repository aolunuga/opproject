/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_planning;

import onepoint.error.XErrorMap;
import onepoint.project.modules.project.OpProjectError;

/**
 * @author : mihai.costin
 */
public class OpProjectPlanningErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "project_planning.error";

   OpProjectPlanningErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpProjectPlanningError.COMMENT_NAME_MISSING, OpProjectPlanningError.COMMENT_NAME_MISSING_NAME);
      registerErrorCode(OpProjectPlanningError.COMMENT_NOT_FOUND, OpProjectPlanningError.COMMENT_NOT_FOUND_NAME);
      registerErrorCode(OpProjectPlanningError.ADMINISTRATOR_ACCESS_DENIED, OpProjectPlanningError.ADMINISTRATOR_ACCESS_DENIED_NAME);
      registerErrorCode(OpProjectPlanningError.HOURLY_RATES_MODIFIED_WARNING, OpProjectPlanningError.HOURLY_RATES_MODIFIED_WARNING_NAME);
      registerErrorCode(OpProjectPlanningError.AVAILIBILITY_MODIFIED_WARNING, OpProjectPlanningError.AVAILIBILITY_MODIFIED_WARNING_NAME);
      registerErrorCode(OpProjectPlanningError.AVAILIBILITY_AND_RATES_MODIFIED_WARNING, OpProjectPlanningError.AVAILIBILITY_AND_RATES_MODIFIED_WARNING_NAME);
      registerErrorCode(OpProjectPlanningError.FILE_READ_ERROR, OpProjectPlanningError.FILE_READ_ERROR_NAME);
      registerErrorCode(OpProjectPlanningError.FILE_WRITE_ERROR, OpProjectPlanningError.FILE_WRITE_ERROR_NAME);
      registerErrorCode(OpProjectError.UPDATE_ACCESS_DENIED, OpProjectError.UPDATE_ACCESS_DENIED_NAME);
   }
}
