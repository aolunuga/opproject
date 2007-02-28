/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.error.XException;
import onepoint.service.XError;

public class OpResourceException extends XException {
   
   public OpResourceException(XError error) {
      super(error);
   }

}
