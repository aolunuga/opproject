/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.components;

import onepoint.express.XValidationException;

/**
 * Exception thrown when a loop is detected in the data set.
 *
 * @author mihai.costin
 */
public class OpActivityLoopException extends XValidationException {
   public OpActivityLoopException(String message) {
      super(message);
   }
}
