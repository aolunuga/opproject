/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.report;

import onepoint.error.XErrorMap;

public class OpReportErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "report.error";

   OpReportErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpReportError.COULD_NOT_EXECUTE_REPORT_SQL, OpReportError.COULD_NOT_EXECUTE_REPORT_SQL_NAME);
      registerErrorCode(OpReportError.VALUE_CONVERSION_EXCEPTION, OpReportError.VALUE_CONVERSION_EXCEPTION_NAME);
      registerErrorCode(OpReportError.JASPER_CAN_NOT_BUILD_REPORT, OpReportError.JASPER_CAN_NOT_BUILD_REPORT_NAME);
      registerErrorCode(OpReportError.JASPER_CAN_NOT_EXPORT_REPORT, OpReportError.JASPER_CAN_NOT_EXPORT_REPORT_NAME);
      registerErrorCode(OpReportError.INVALID_REPORT_FORMAT, OpReportError.INVALID_REPORT_FORMAT_NAME);
      registerErrorCode(OpReportError.CREATE_REPORT_EXCEPTION, OpReportError.CREATE_REPORT_EXCEPTION_NAME);
      registerErrorCode(OpReportError.SAVE_REPORT_EXCEPTION, OpReportError.SAVE_REPORT_EXCEPTION_NAME);
   }

}
