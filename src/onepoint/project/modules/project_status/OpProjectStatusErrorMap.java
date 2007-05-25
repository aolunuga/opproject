/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_status;

import onepoint.error.XErrorMap;

/**
 * Project status error map. These errors can be returned in a reply from the project status service.
 *
 * @author mihai.costin
 */
public class OpProjectStatusErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "project_status.error";

   OpProjectStatusErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpProjectStatusError.PROJECT_STATUS_NAME_NOT_SPECIFIED, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_SPECIFIED_NAME);
      registerErrorCode(OpProjectStatusError.PROJECT_STATUS_NAME_NOT_UNIQUE, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_UNIQUE_NAME);
      registerErrorCode(OpProjectStatusError.PROJECT_STATUS_NOT_FOUND, OpProjectStatusError.PROJECT_STATUS_NOT_FOUND_NAME);
      registerErrorCode(OpProjectStatusError.INSUFFICIENT_PRIVILEGES, OpProjectStatusError.INSUFFICIENT_PRIVILEGES_NAME);
   }
}
