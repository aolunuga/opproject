/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.external_applications.MindMeister;

/**
 * Exception Class, used by the Reporting package. Designed as subclass of <code>Exception</code> mainly to
 * distinguish the special Exceptions in case of occurancies in the <code>ReportManager</code>
 *
 * @author jmersmann
 * @version $0.0.1$
 * @see java.lang.Exception
 * @since 1.0
 */
public final class OpMindMeisterError {

   public static final int OTHER_ERROR = 1;
   public static final int LOGIN_ERROR = 2;
   public static final int GET_MAP_ERROR = 3;

   public static final String OTHER_ERROR_NAME = "OtherError";
   public static final String LOGIN_ERROR_NAME = "LoginError";
   public static final String GET_MAP_ERROR_NAME = "GetMapError";

}
