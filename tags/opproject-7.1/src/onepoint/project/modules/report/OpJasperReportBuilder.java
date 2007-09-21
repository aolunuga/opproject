/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.report;

import net.sf.jasperreports.engine.*;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectSession;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;

/**
 * This class contains some methods for building the jasper report result (SQL reports or not).
 *
 * @author horia.chiorean
 */
public final class OpJasperReportBuilder {

   // Class logger.
   private static final XLog logger = XLogFactory.getServerLogger(OpJasperReportBuilder.class);

   /**
    * Utility class.
    */
   private OpJasperReportBuilder() {
   }

   /**
    * This method processes a given SQL jasper report with some parameters and creates a result as a
    * <code>JasperPrint</code> using a custom data source. It uses a custom class loader that retrieves all the resources necessary
    * for the master report ( images, subreports).
    *
    * @param jasperReport   the initial jasper report.
    * @param parameters     the report parameters.
    * @param session a <code>OpProjectSession</code> representing the server session.
    * @param pathUrl a <code>URL</code> to the location
    * @param ds a  <code>OpReportDataSource</code> representing the data source that will be used.
    *
    * @return a <code>JasperPrint</code> object representing the filled report.
    */
   public static JasperPrint buildDatasourceReport(JasperReport jasperReport, Map parameters, OpProjectSession session, URL pathUrl, OpReportDataSource ds) {
      // set up a new classloader
      ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
      URLClassLoader jcl = new URLClassLoader(new URL[]{pathUrl}, oldClassLoader);

      // replace the old classloader with the new one
      Thread currentThread = Thread.currentThread();
      JasperPrint jasperPrint = null;
      try {
         currentThread.setContextClassLoader(jcl);
         parameters.put(JRParameter.REPORT_LOCALE, new Locale(session.getLocale().getID()));
         jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, ds);
      }
      catch (JRException e) {
         logger.error("Cannot fill report", e);
      }
      finally {
         Thread.currentThread().setContextClassLoader(oldClassLoader);
      }
      return jasperPrint;
   }


   /**
    * Tries to compile the jasper report.
    * @param sourceFileName a <code>String</code> representing a source file.
    * @param destFileName a <code>String</code> representing a destination file.
    * @return true if the operation was successfull.
    */
   public static boolean compileReport(String sourceFileName, String destFileName) {
      try {
         JasperCompileManager.compileReportToFile(sourceFileName, destFileName);
         return true;
      }
      catch (JRException jre) {
         logger.error(jre);
         return false;
      }
   }
}
