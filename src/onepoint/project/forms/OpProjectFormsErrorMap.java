/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.error.XErrorMap;

public class OpProjectFormsErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "dialog.error";

   public OpProjectFormsErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpProjectFormsError.MANDATORY_VALUE, OpProjectFormsError.MANDATORY_VALUE_NAME);
      registerErrorCode(OpProjectFormsError.INVALID_FORMAT, OpProjectFormsError.INVALID_FORMAT_NAME);
   }

}
