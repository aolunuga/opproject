/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.external_applications.MindMeister;

import onepoint.error.XErrorMap;

public class OpMindMeisterErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "mindmeister.error";

   OpMindMeisterErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpMindMeisterError.OTHER_ERROR, OpMindMeisterError.OTHER_ERROR_NAME);
      registerErrorCode(OpMindMeisterError.LOGIN_ERROR, OpMindMeisterError.LOGIN_ERROR_NAME);
      registerErrorCode(OpMindMeisterError.GET_MAP_ERROR, OpMindMeisterError.GET_MAP_ERROR_NAME);
   }

}
