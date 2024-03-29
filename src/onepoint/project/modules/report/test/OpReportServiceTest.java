/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.report.test;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.report.OpReportError;
import onepoint.project.modules.report.OpReportService;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;
import onepoint.util.XEncodingHelper;

import java.io.File;

/**
 * This class test report service methods and form providers.
 *
 * @author calin.pavel
 */
public class OpReportServiceTest extends OpBaseOpenTestCase {
   // class logger.
   private static final XLog logger = XLogFactory.getLogger(OpReportServiceTest.class);

   private OpReportService reportService;

   private static final String INVALID_FORMAT = "invalidFormat";
   private static final String UNKNOWN_REPORT_NAME = "unknown_Name";

   /**
    * Here we prepare data for these tests.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      reportService = OpTestDataFactory.getReportService();
   }

   /**
    * Clean data after test execution.
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      clean();
      super.tearDown();
   }

   /**
    * Delete reports that could be present into database or file system.
    */
   protected void clean()
        throws Exception {
      try {
         reportService.reportsCleanUp(session, new XMessage());
      }
      catch (Exception e) {
         logger.error("Could not delete reports.", e);
      }

      super.clean();
   }

   /**
    * Here we test scenario where user creates a Report on disk on CSV Format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testCreateReportCSV()
        throws Exception {
      checkReportCreationOnDisk(OpReportService.REPORT_TYPE_CSV);
   }

   /**
    * Here we test scenario where user creates a Report on disk on HTML Format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testCreateReportHTML()
        throws Exception {
      checkReportCreationOnDisk(OpReportService.REPORT_TYPE_HTML);
   }

   /**
    * Here we test scenario where user creates a Report on disk on PDF Format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testCreateReportPDF()
        throws Exception {
      checkReportCreationOnDisk(OpReportService.REPORT_TYPE_PDF);
   }

   /**
    * Here we test scenario where user creates a Report on disk on XLS Format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testCreateReportXLS()
        throws Exception {
      checkReportCreationOnDisk(OpReportService.REPORT_TYPE_XLS);
   }

   /**
    * Here we test scenario where user creates a Report on disk on XLS Format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testCreateReportXML()
        throws Exception {
      checkReportCreationOnDisk(OpReportService.REPORT_TYPE_XML);
   }

   /**
    * Here we test scenario where user creates a Report on disk using an invalid format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testCreateReportUnknownFormat()
        throws Exception {
      XMessage request = OpReportTestDataFactory.buildDefaultRequest(INVALID_FORMAT, 0);
      XMessage response = reportService.createReport(session, request);
      assertNotNull(response);
      assertError(response, OpReportError.INVALID_REPORT_FORMAT);
   }

   /**
    * Here we test scenario where user creates a Report on disk using an invalid name.
    *
    * @throws Exception If something goes wrong.
    */
   public void testCreateReportUnknownName()
        throws Exception {
      XMessage request = OpReportTestDataFactory.buildDefaultRequest(OpReportService.REPORT_TYPE_PDF, 0);
      request.setArgument(OpReportService.NAME, UNKNOWN_REPORT_NAME);
      XMessage response = reportService.createReport(session, request);
      assertNotNull(response);
      assertError(response, OpReportError.CREATE_REPORT_EXCEPTION);
   }

   /**
    * Here we test scenario where user creates a Report into database using  CSV Format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testSaveReportCSV()
        throws Exception {
      checkReportCreationIntoDatabase(OpReportService.REPORT_TYPE_CSV);
   }

   /**
    * Here we test scenario where user creates a Report into database using  HTML Format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testSaveReportHTML()
        throws Exception {
      checkReportCreationIntoDatabase(OpReportService.REPORT_TYPE_HTML);
   }

   /**
    * Here we test scenario where user creates a Report into database using  PDF Format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testSaveReportPDF()
        throws Exception {
      checkReportCreationIntoDatabase(OpReportService.REPORT_TYPE_PDF);
   }

   /**
    * Here we test scenario where user creates a Report into database using  XLS Format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testSaveReportXLS()
        throws Exception {
      checkReportCreationIntoDatabase(OpReportService.REPORT_TYPE_XLS);
   }

   /**
    * Here we test scenario where user creates a Report into database using  XLS Format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testSaveReportXML()
        throws Exception {
      checkReportCreationIntoDatabase(OpReportService.REPORT_TYPE_XML);
   }

   /**
    * Here we test scenario where user creates a Report into database using an invalid format.
    *
    * @throws Exception If something goes wrong.
    */
   public void testSaveReportUnknownFormat()
        throws Exception {
      XMessage request = OpReportTestDataFactory.buildDefaultRequest(INVALID_FORMAT, 0);
      XMessage response = reportService.saveReport(session, request);
      assertNotNull(response);
      assertError(response, OpReportError.INVALID_REPORT_FORMAT);
   }

   /**
    * Here we test scenario where user creates a Report on disk using an invalid name.
    *
    * @throws Exception If something goes wrong.
    */
   public void testSaveReportUnknownName()
        throws Exception {
      XMessage request = OpReportTestDataFactory.buildDefaultRequest(OpReportService.REPORT_TYPE_PDF, 0);
      request.setArgument(OpReportService.NAME, UNKNOWN_REPORT_NAME);
      XMessage response = reportService.saveReport(session, request);
      assertNotNull(response);
      assertError(response, OpReportError.SAVE_REPORT_EXCEPTION);
   }

   /**
    * Here we create a report on disk and check if it was really created.
    *
    * @param format report format to use.
    * @throws Exception If something goes wrong.
    */
   private void checkReportCreationOnDisk(String format)
        throws Exception {
      XMessage request = OpReportTestDataFactory.buildDefaultRequest(format, OpLocator.parseLocator(adminId).getID());
      XMessage response = reportService.createReport(session, request);
      assertNotNull(response);
      assertNoError(response);

      String reportPath = (String) response.getArgument(OpReportService.GENERATED_REPORT_PATH);
      assertNotNull(reportPath);
      assertTrue(reportPath.length() > 0);

      reportPath = XEncodingHelper.decodeValue(reportPath);

      // now check if file really exists.
      File reportFile = new File(OpReportService.SAVED_REPORTS_PATH + reportPath);
      assertTrue("Returned file should exist on disk.", reportFile.exists());
      assertTrue("Returned path must be a file", reportFile.isFile());
      assertTrue("Generated report must not be empty.", reportFile.length() > 0);
   }

   /**
    * Here we create a report into database and check if it was really created.
    *
    * @param format report format to use.
    * @throws Exception If something goes wrong.
    */
   private void checkReportCreationIntoDatabase(String format)
        throws Exception {
      XMessage request = OpReportTestDataFactory.buildDefaultRequest(format, OpLocator.parseLocator(adminId).getID());
      XMessage response = reportService.saveReport(session, request);
      assertNotNull(response);
      assertNoError(response);

      // check generated report.
      String reportName = (String) response.getArgument(OpReportService.REPORT_NAME);
      assertNotNull(reportName);
      String reportTypeId = (String) response.getArgument(OpReportService.REPORT_TYPE_ID);
      assertNotNull(reportTypeId);
      String contentId = (String) response.getArgument(OpReportService.CONTENT_ID);
      assertNotNull(contentId);

      OpBroker broker = session.newBroker();
      try {
         OpContent content = (OpContent) broker.getObject(contentId);
         assertNotNull(content);
         assertTrue(content.getSize() > 0);
      }
      finally {
         broker.close();
      }
   }

}
