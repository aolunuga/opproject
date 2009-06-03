/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report;

import java.io.OutputStream;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRCsvExporterParameter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectSession;

import org.apache.log4j.Logger;

/**
 * !Document here!
 * 
 * @author cristian.godja
 */
public abstract class OpJasperReportExporter {

   /**
    * The Logger for this class...
    */
   private static final XLog logger = XLogFactory.getLogger(OpJasperReportExporter.class);

   /**
    * Jasper Exporter class for generating PDF
    */
   public static class OpJasperReportPDFExporter extends OpJasperReportExporter {

      public void exportReport(OpProjectSession session, JasperPrint jasperPrint, OutputStream os)
            throws OpReportException {
         // export report into pdf format.
         try {
            JasperExportManager.exportReportToPdfStream(jasperPrint, os);
         }
         catch (JRException e) {
            // todo: put error code for this in ReportingException, or add some IOException??
            e.printStackTrace();
            logger.error(e);
            throw new OpReportException(session.newError(OpReportService.ERROR_MAP,
                  OpReportError.JASPER_CAN_NOT_EXPORT_REPORT));
         }
      }
   }

   /**
    * Exports reports into a HTML format.
    */
   public static class OpJasperReportHTMLExporter extends OpJasperReportExporter {

      private static final Logger log = Logger.getLogger(OpJasperReportHTMLExporter.class);

      public void exportReport(OpProjectSession session, JasperPrint jasperPrint, OutputStream os)
            throws OpReportException {

         try {
            JRHtmlExporter exporter = new JRHtmlExporter();
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, os);
            exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, Boolean.FALSE);
            exporter.setParameter(JRHtmlExporterParameter.IS_OUTPUT_IMAGES_TO_DIR, Boolean.FALSE);
            exporter.exportReport();
         }
         catch (JRException e) {
            // todo: put error code for this in ReportingException, or add some IOException??
            throw new OpReportException(session.newError(OpReportService.ERROR_MAP,
                  OpReportError.JASPER_CAN_NOT_EXPORT_REPORT));
         }
      }
   }

   /**
    * Exports reports into a XML format.
    */
   public static class OpJasperReportXMLExporter extends OpJasperReportExporter {

      private static final Logger log = Logger.getLogger(OpJasperReportXMLExporter.class);

      public void exportReport(OpProjectSession session, JasperPrint jasperPrint, OutputStream os)
            throws OpReportException {

         try {
            JasperExportManager.exportReportToXmlStream(jasperPrint, os);
         }
         catch (JRException e) {
            // todo: put error code for this in ReportingException, or add some IOException??
            throw new OpReportException(session.newError(OpReportService.ERROR_MAP,
                  OpReportError.JASPER_CAN_NOT_EXPORT_REPORT));
         }
      }
   }

   /**
    * Exports reports into a XLS format.
    */
   public static class OpJasperReportXLSExporter extends OpJasperReportExporter {

      private static final Logger log = Logger.getLogger(OpJasperReportXLSExporter.class);

      public void exportReport(OpProjectSession session, JasperPrint jasperPrint, OutputStream os)
            throws OpReportException {

         try {
            JExcelApiExporter jExcelApiExporter = new JExcelApiExporter();
            jExcelApiExporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
//            jExcelApiExporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, jasperPrint.showType);
            jExcelApiExporter.setParameter(JRExporterParameter.OUTPUT_STREAM, os);
            jExcelApiExporter.exportReport();

//            JRXlsExporter exporter = new JRXlsExporter();
//            exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.TRUE);
//            exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
//            exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, os);
//            exporter.exportReport();
         }
         catch (JRException e) {
            // todo: put error code for this in ReportingException, or add some IOException??
            throw new OpReportException(session.newError(OpReportService.ERROR_MAP,
                  OpReportError.JASPER_CAN_NOT_EXPORT_REPORT));
         }
      }
   }

   /**
    * Exports reports into a CSV format.
    */
   public static class OpJasperReportCSVExporter extends OpJasperReportExporter {

      private static final Logger log = Logger.getLogger(OpJasperReportCSVExporter.class);
      /**
       * Separator used for exporting to CSV format
       */
      private static final String CSV_SEPARATOR = ",";

      public void exportReport(OpProjectSession session, JasperPrint jasperPrint, OutputStream os)
            throws OpReportException {

         try {
            JRCsvExporter exporter = new JRCsvExporter();
            exporter.setParameter(JRCsvExporterParameter.FIELD_DELIMITER, CSV_SEPARATOR);
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, os);
            exporter.exportReport();
         }
         catch (JRException e) {
            // todo: put error code for this in ReportingException, or add some IOException??
            throw new OpReportException(session.newError(OpReportService.ERROR_MAP,
                  OpReportError.JASPER_CAN_NOT_EXPORT_REPORT));
         }
      }
   }

   /**
    * Returns the appropriate exporter class for the specified type
    * 
    * @param type
    *           the exporter type
    * @return the exporter instance used for generating the result
    */
   public static OpJasperReportExporter getInstance(String type) {
      if (type.equals(OpReportService.REPORT_TYPE_PDF)) {
         return new OpJasperReportPDFExporter();
      }
      else if (type.equals(OpReportService.REPORT_TYPE_CSV)) {
         return new OpJasperReportCSVExporter();
      }
      else if (type.equals(OpReportService.REPORT_TYPE_HTML)) {
         return new OpJasperReportHTMLExporter();
      }
      else if (type.equals(OpReportService.REPORT_TYPE_XLS)) {
         return new OpJasperReportXLSExporter();
      }
      else if (type.equals(OpReportService.REPORT_TYPE_XML)) {
         return new OpJasperReportXMLExporter();
      }
      throw new RuntimeException("Unknow type for jasper exporter: " + type);
   }

   public abstract void exportReport(OpProjectSession session, JasperPrint jasperPrint, OutputStream os)
         throws OpReportException;

}
