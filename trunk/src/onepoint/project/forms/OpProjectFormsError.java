/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

/**
 * Work slip error codes and names
 * @author ovidiu.lupas
 */
public abstract class OpProjectFormsError {

   // Error codes
   public final static int INTERNAL_ERROR = 1;
   public final static int MANDATORY_VALUE = 4;
   public final static int INVALID_FORMAT = 5;

   // Error names
   public final static String INTERNAL_ERROR_NAME = "InternalError";
   public final static String MANDATORY_VALUE_NAME = "MandatoryValue";
   public final static String INVALID_FORMAT_NAME = "IvnalidFormat";
}
