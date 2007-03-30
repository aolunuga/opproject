/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.preferences;

import onepoint.error.XErrorMap;

/**
 * Error map class used for the preferences module.
 *
 * @author horia.chiorean
 */
public class OpPreferencesErrorMap extends XErrorMap {


   /**
    * @see XErrorMap#XErrorMap(String)
    */
   public OpPreferencesErrorMap() {
      super("preferences.error");
      registerErrorCode(OpPreferencesError.EMPTY_PASSWORD, OpPreferencesError.EMPTY_PASSWORD_NAME);
      registerErrorCode(OpPreferencesError.PASSWORD_MISSMATCH, OpPreferencesError.PASSWORD_MISSMATCH_NAME);
   }
}
