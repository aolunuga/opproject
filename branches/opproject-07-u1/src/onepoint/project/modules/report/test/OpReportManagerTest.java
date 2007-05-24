/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.report.test;

import onepoint.project.modules.report.OpReportManager;
import onepoint.project.test.OpBaseTestCase;

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
    * <FIXME author="Horia Chiorean" description="Write a proper test for this">
    */
   public void testResourceRetrieval()
        throws Exception {
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
