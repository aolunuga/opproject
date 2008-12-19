package onepoint.project.modules.settings;

import onepoint.service.XError;
import onepoint.service.server.XServiceException;

/**
 * Settings Exception class.
 *
 * @author mihai.costin
 */
public class OpSettingsException extends XServiceException {

   public OpSettingsException(XError error) {
      super(error);
   }

   public OpSettingsException(String message, Throwable cause) {
      super(message, cause);
   }
}
