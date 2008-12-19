package onepoint.project.modules.project;

import onepoint.service.XError;
import onepoint.service.server.XServiceException;

/**
 * Exception class for project administration service.
 *
 * @author mihai.costin
 */
public class OpProjectAdministrationException extends XServiceException {

   public OpProjectAdministrationException(XError error) {
      super(error);
   }

   public OpProjectAdministrationException(String message, Throwable cause) {
      super(message, cause);
   }
}

