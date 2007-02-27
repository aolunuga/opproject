/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.report;

import onepoint.error.XException;
import onepoint.service.XError;

public class OpReportException extends XException {
   
   public OpReportException(XError error) {
      super(error);
   }

}
