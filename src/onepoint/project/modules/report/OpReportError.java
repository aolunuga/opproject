/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */
package onepoint.project.modules.report;

/**
 * Exception Class, used by the Reporting package. Designed as subclass of <code>Exception</code> mainly to
 * distinguish the special Exceptions in case of occurancies in the <code>ReportManager</code>
 * 
 * @author jmersmann
 * @version $0.0.1$
 * @see java.lang.Exception
 * @since 1.0
 */
public final class OpReportError {

   public static final int VALUE_CONVERSION_EXCEPTION = 1;
   public static final int COULD_NOT_EXECUTE_REPORT_SQL = 2;
   public static final int JASPER_CAN_NOT_BUILD_REPORT = 3;
   public static final int JASPER_CAN_NOT_EXPORT_REPORT = 4;

   public static final String VALUE_CONVERSION_EXCEPTION_NAME = "ValueConversionException";
   public static final String COULD_NOT_EXECUTE_REPORT_SQL_NAME = "CouldNotExecuteReportSQL";
   public static final String JASPER_CAN_NOT_BUILD_REPORT_NAME = "JasperCanNotBuildFilledReport";
   public static final String JASPER_CAN_NOT_EXPORT_REPORT_NAME = "JasperCanNotExportReport";

}