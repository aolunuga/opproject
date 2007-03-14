/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.report.test;

import onepoint.project.modules.report.OpReportManager;
import onepoint.project.modules.report.OpReportService;
import onepoint.project.test.OpBaseTestCase;
import onepoint.service.XMessage;
import onepoint.persistence.OpLocator;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * This class test report manager methods.
 *
 * @author calin.pavel
 */
public class OpReportManagerTest extends OpBaseTestCase {
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
   }

   /**
    * Here we test retrieval of a resource content through ReportManager.
    *
    * @throws Exception If something goes wrong.
    */
   public void testResourceRetrieval()
        throws Exception {
      XMessage request = OpReportTestDataFactory.buildDefaultRequest(OpReportService.REPORT_TYPE_PDF, OpLocator.parseLocator(adminId).getID());
      XMessage response = getReportService().createReport(session, request);
      assertNotNull(response);
      assertNoError(response);

      String reportPath = (String) response.getArgument(OpReportService.GENERATED_REPORT_PATH);
      assertNotNull(reportPath);

      // Now we'll try to transform that path into a resource path (to be loaded as a resource from classpath).
      String className = OpReportManager.class.getName();
      String rootPackage = className.substring(0, className.indexOf('.'));

      URL reportFileUrl = new URL(reportPath);
      reportPath = reportFileUrl.getFile();
      reportPath = reportPath.substring(reportPath.lastIndexOf(rootPackage));
      File reportFile = new File(reportFileUrl.getFile());

      byte[] content = reportManager.getResource(reportPath, session);
      assertNotNull(content);
      assertTrue(content.length > 0);
      assertEquals(reportFile.length(), content.length);
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
