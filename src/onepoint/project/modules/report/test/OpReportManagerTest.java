/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.report.test;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpLocator;
import onepoint.project.modules.report.OpReportManager;
import onepoint.project.modules.report.OpReportService;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;

import java.util.Map;

/**
 * This class test report manager methods.
 *
 * @author calin.pavel
 */
public class OpReportManagerTest extends OpBaseOpenTestCase {

   /**
    * This class' logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpReportManagerTest.class);

   /**
    * The report manager used by the tests.
    */
   private OpReportManager reportManager;

   /**
    * Here we prepare data for these tests.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();
      reportManager = OpReportManager.getReportManager(session);
      //load the report used for the tests  (this automatically clears the old entry)
      reportManager.updateReportCacheEntry(OpReportTestDataFactory.REPORT_NAME);
   }

   /**
    * Here we test retrieval of a resource content through ReportManager.
    *
    * @throws Exception If something goes wrong.
    */
   public void testResourceRetrieval()
        throws Exception {
      XMessage request = OpReportTestDataFactory.buildDefaultRequest(OpReportService.REPORT_TYPE_PDF, OpLocator.parseLocator(adminId).getID());
      XMessage response = OpTestDataFactory.getReportService().createReport(session, request);
      assertNotNull(response);
      assertNoError(response);

      byte[] content = reportManager.getResource("/work_report.jes", session); // from path cache
      assertNotNull(content);
      assertTrue(content.length > 0);
      content = reportManager.getResource("jasper/logo.png", session); // from classpath
      assertNotNull(content);
      assertTrue(content.length > 0);
   }

   /**
    * Here we test retrieval of paths.
    *
    * @throws Exception If something goes wrong.
    */
   public void testPathRetrieval()
        throws Exception {
      String path = reportManager.getI18nFileName(OpReportTestDataFactory.REPORT_NAME);
      assertNotNull(path);

      path = reportManager.getJasperDirName(OpReportTestDataFactory.REPORT_NAME);
      assertNotNull(path);

      path = reportManager.getJasperFileName(OpReportTestDataFactory.REPORT_NAME);
      assertNotNull(path);

      path = reportManager.getJesName(OpReportTestDataFactory.REPORT_NAME);
      assertNotNull(path);

      path = reportManager.getLocalizedJasperFileName(OpReportTestDataFactory.REPORT_NAME, "en");
      assertNotNull(path);

      path = reportManager.getLocalizedJasperFileName(OpReportTestDataFactory.REPORT_NAME);
      assertNotNull(path);

      path = reportManager.getReportJarExpressFilesPath(OpReportTestDataFactory.REPORT_NAME);
      assertNotNull(path);

      path = reportManager.getReportJarJasperFilesPath(OpReportTestDataFactory.REPORT_NAME);
      assertNotNull(path);
   }

   /**
    * Here we test retrieval of descriptions for all reports.
    *
    * @throws Exception If something goes wrong.
    */
   public void testReportsDescriptionRetrieval()
        throws Exception {
      Map descriptions = reportManager.getJasperDescriptions();
      assertNotNull(descriptions);
      assertTrue(descriptions.size() > 0);

      Map reportDescriptions = (Map) descriptions.get(OpReportTestDataFactory.REPORT_NAME);
      assertNotNull(reportDescriptions);
      assertTrue(reportDescriptions.size() > 0);
   }
}
