/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.report;

import onepoint.service.XError;
import onepoint.service.server.XServiceException;

public class OpReportException extends XServiceException {
   
   public OpReportException(XError error) {
      super(error);
   }

}
