/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.report_archive;

import onepoint.error.XErrorMap;

/**
 * Error map class for the report archive module.
 *
 * @author horia.chiorean
 */
public class OpReportArchiveErrorMap extends XErrorMap {

   public OpReportArchiveErrorMap() {
      super("report_archive.error");
      registerErrorCode(OpReportArchiveError.INSUFICIENT_PRIVILEGES, OpReportArchiveError.INSUFICIENT_PRIVILEGES_NAME);
   }
}
