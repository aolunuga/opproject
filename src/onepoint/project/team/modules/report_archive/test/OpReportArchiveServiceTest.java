/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.team.modules.report_archive.test;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.report.OpReportService;
import onepoint.project.modules.report.test.OpReportTestDataFactory;
import onepoint.project.modules.schedule.OpScheduler;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.team.modules.report_archive.OpReportArchiveError;
import onepoint.project.team.modules.report_archive.OpReportArchiveService;
import onepoint.project.team.modules.report_archive.OpReportArhiveScheduleHandler;
import onepoint.project.team.test.OpBaseTeamTestCase;
import onepoint.util.XEncodingHelper;
import onepoint.service.XMessage;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * This class test report archive service methods and form providers.
 *
 * @author calin.pavel
 */
public class OpReportArchiveServiceTest extends OpBaseTeamTestCase {
   // class logger.
   private static final XLog logger = XLogFactory.getLogger(OpReportArchiveServiceTest.class, true);

   private OpReportArchiveService reportArchiveService;
   private OpReportService reportService;
   private OpReportTestDataFactory dataFactory;

   /**
    * Here we prepare data for these tests.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      reportArchiveService = getReportArchiveService();
      reportService = getReportService();
      dataFactory = new OpReportTestDataFactory(session);

      cleanUp();
   }

   /**
    * Clean data after test execution.
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      cleanUp();
      super.tearDown();

      reportArchiveService = null;
      reportService = null;
      dataFactory = null;
   }

   /**
    * Delete reports that could be present into database or file system.
    */
   private void cleanUp() {
      // delete reports from file system
      try {
         reportService.reportsCleanUp(session, new XMessage());
      }
      catch (Exception e) {
         logger.error("Could not delete reports.", e);
      }

      // now delete reports from database.
      List reports = dataFactory.getAllReports();
      if (reports != null && reports.size() > 0) {
         Iterator it = reports.iterator();
         OpBroker broker = session.newBroker();
         OpTransaction tx = broker.newTransaction();
         while (it.hasNext()) {
            OpReport opReport = (OpReport) it.next();
            broker.deleteObject(opReport);
         }
         tx.commit();
         broker.close();
      }
   }

   /**
    * Test insert of a report archive into database.
    *
    * @throws Exception If something goes wrong.
    */
   public void testReportArchiveInsert()
        throws Exception {
      createReport();

      // now check if the report was really created.
      List reports = dataFactory.getAllReports();
      assertNotNull(reports);
      assertEquals("We should have only one report into database", 1, reports.size());

      OpReport report = (OpReport) reports.get(0);
      assertNotNull(report);
      assertNotNull(report.getName());

      OpContent reportContent = report.getContent();
      assertNotNull(reportContent);
   }

   /**
    * Here we test preparation for opening of a report.
    *
    * @throws Exception If something goes wrong.
    */
   public void testPrepareReportOpening()
        throws Exception {
      createReport();

      // now check if the report was really created.
      List reports = dataFactory.getAllReports();
      assertNotNull(reports);
      assertEquals("We should have only one report into database", 1, reports.size());

      OpReport report = (OpReport) reports.get(0);
      assertNotNull(report);


      XMessage request = new XMessage();
      request.setArgument(OpReportArchiveService.REPORT_IDS, new ArrayList());
      XMessage response = reportArchiveService.prepareReportOpening(session, request);
      assertError(response, OpReportArchiveError.INSUFICIENT_PRIVILEGES);

      List reportIds = Arrays.asList(new String[]{OpLocator.locatorString(OpReport.REPORT, report.getID())});
      request.setArgument(OpReportArchiveService.REPORT_IDS, reportIds);
      response = reportArchiveService.prepareReportOpening(session, request);
      assertNoError(response);

      List contentIds = (List) response.getArgument(OpReportArchiveService.CONTENT_IDS);
      assertNotNull(contentIds);
      assertEquals(1, contentIds.size());

      List reportURLs = (List) response.getArgument(OpReportArchiveService.REPORT_URLS);
      assertNotNull(reportURLs);
      assertEquals(1, reportURLs.size());

      // now check if file really exists.
      String reportPath = (String) reportURLs.get(0);
      reportPath = XEncodingHelper.decodeValue(reportPath);
      URL reportFileUrl = new URL(reportPath);
      File reportFile = new File(reportFileUrl.getFile());
      assertTrue("Returned file should exist on disk.", reportFile.exists());
      assertTrue("Returned path must be a file", reportFile.isFile());
      assertTrue("Generated report must not be empty.", reportFile.length() > 0);
   }

   /**
    * Here we test the deletion of a report.
    *
    * @throws Exception If something goes wrong.
    */
   public void testDeleteReport()
        throws Exception {
      createReport();

      // now check if the report was really created.
      List reports = dataFactory.getAllReports();
      assertNotNull(reports);
      assertEquals("We should have only one report into database", 1, reports.size());

      OpReport report = (OpReport) reports.get(0);
      assertNotNull(report);

      List reportIds = Arrays.asList(new String[]{OpLocator.locatorString(OpReport.REPORT, report.getID())});
      XMessage request = new XMessage();
      request.setArgument(OpReportArchiveService.REPORT_IDS, reportIds);
      XMessage response = reportArchiveService.deleteReports(session, request);
      assertNoError(response);

      // now check if there is any other reports.
      reports = dataFactory.getAllReports();
      assertNotNull(reports);
      assertEquals(0, reports.size());
   }

   /**
    * This scenario tests how archive report scheduler works.
    *
    * @throws Exception If something goes wrong.
    */
   public void testArchiveSchedulerHandler()
        throws Exception {
      createReport();

      // now check if the report was really created.
      List reports = dataFactory.getAllReports();
      assertNotNull(reports);
      assertEquals("We should have only one report into database", 1, reports.size());

      OpReport report = (OpReport) reports.get(0);
      assertNotNull(report);

      long reportId = report.getID();

      // now change create date of the report
      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();

      report = (OpReport) broker.getObject(OpReport.class, reportId);
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.YEAR, -10);
      report.setCreated(cal.getTime());
      broker.updateObject(report);

      transaction.commit();
      broker.close();

      // now test if scheduler deletes automatically our report.
      HashMap events = new HashMap();
      events.put(OpScheduler.SCHEDULE_NAME, OpSettings.get(OpSettings.REPORT_ARCHIVE_SCHEDULE_NAME));

      OpReportArhiveScheduleHandler handler = new OpReportArhiveScheduleHandler();
      handler.processEvent(events);

      // now check if there is any other reports.
      reports = dataFactory.getAllReports();
      assertNotNull(reports);
      assertEquals(0, reports.size());
   }

   /**
    * This method creates a PDF report into database.
    */
   private void createReport() {
      // first create a report
      XMessage request = OpReportTestDataFactory.buildDefaultRequest(OpReportService.REPORT_TYPE_PDF, OpLocator.parseLocator(adminId).getID());
      XMessage response = reportService.saveReport(session, request);
      assertNoError(response);

      // get report info
      String reportName = (String) response.getArgument(OpReportService.REPORT_NAME);
      assertNotNull(reportName);
      String reportTypeId = (String) response.getArgument(OpReportService.REPORT_TYPE_ID);
      assertNotNull(reportTypeId);
      String contentId = (String) response.getArgument(OpReportService.CONTENT_ID);
      assertNotNull(contentId);

      request = new XMessage();
      request.setArgument(OpReportArchiveService.REPORT_NAME, reportName);
      request.setArgument(OpReportArchiveService.CONTENT_ID, contentId);
      request.setArgument(OpReportArchiveService.REPORT_TYPE_ID, reportTypeId);

      response = reportArchiveService.insertReportArchive(session, request);
      assertNoError(response);
   }
}
