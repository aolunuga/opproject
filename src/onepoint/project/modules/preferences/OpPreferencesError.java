/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.preferences;

/**
 * Enum class that defines all the error codes/constant used for the preferences module.
 *
 * @author horia.chiorean
 */
public interface OpPreferencesError {
   /**
    * Error constants.
    */
   static final int EMPTY_PASSWORD = 1;
   static final int PASSWORD_MISSMATCH = 2;

   /**
    * Error names
    */
   static final String EMPTY_PASSWORD_NAME = "EmptyPassword";
   static final String PASSWORD_MISSMATCH_NAME = "PasswordMissmatch";
}
