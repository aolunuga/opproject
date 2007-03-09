/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_planning;

import onepoint.service.XError;
import onepoint.service.server.XServiceException;

/**
 * @author mihai.costin
 */
public class OpProjectPlanningException extends XServiceException {

   public OpProjectPlanningException(XError error) {
      super(error);
   }

   public OpProjectPlanningException(String message, Throwable cause) {
      super(message, cause);
   }
}
