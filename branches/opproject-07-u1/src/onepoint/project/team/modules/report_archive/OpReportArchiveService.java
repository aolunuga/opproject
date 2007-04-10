/*
 * Copyright(c) OnePoint Software GmbH 2005. All Rights Reserved.
 */

package onepoint.project.team.modules.report_archive;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.util.XEncodingHelper;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.report.OpReportManager;
import onepoint.project.modules.report.OpReportType;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Service class for handling all operations related to the report archive module.
 *
 * @author horia.chiorean
 */
public class OpReportArchiveService extends OpProjectService {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpReportArchiveService.class, true);

   /**
    * The name of the request parameter representing the id of the content.
    */
   public static final String CONTENT_ID = "contentId";
   public static final String REPORT_TYPE_ID = "reportTypeId";
   public static final String REPORT_IDS = "reportIds";

   /**
    * The name of the request parameter representing the report name.
    */
   public static final String REPORT_NAME = "reportName";

   /**
    * Name of the response parameter representing the urls to the reports.
    */
   public static final String REPORT_URLS = "reportUrls";
   public static final String CONTENT_IDS = "contentIds";

   /**
    * Character used in the generation of the report name.
    */
   private static final char REPORT_NAME_SEPARATOR_CHAR = ' ';

   /**
    * The error map used by the service.
    */
   private static final OpReportArchiveErrorMap ERROR_MAP = new OpReportArchiveErrorMap();

   /**
    * Inserts a new report archive in the database.
    *
    * @param session       a <code>XSession</code> object representing the current session.
    * @param request a <code>XMessage</code> representing the current request.
    * @return a <code>XMessage</code> representing the response.
    */
   public XMessage insertReportArchive(OpProjectSession session, XMessage request) {
      OpBroker broker = session.newBroker();

      String reportJesName = (String) request.getArgument(REPORT_NAME);
      String contentLocator = (String) request.getArgument(CONTENT_ID);
      String reportTypeLocator = (String) request.getArgument(REPORT_TYPE_ID);

      OpTransaction tx = broker.newTransaction();

      OpUser currentUser = session.user(broker);
      OpContent content = (OpContent) broker.getObject(contentLocator);
      OpReportType reportType = (OpReportType) broker.getObject(reportTypeLocator);

      OpReport reportArchive = new OpReport();
      //generate name
      OpReportManager xrm = OpReportManager.getReportManager(session);
      String reportArchiveName = generateReportName(reportJesName, xrm);

      reportArchive.setName(reportArchiveName);
      reportArchive.setCreator(currentUser);
      reportArchive.setType(reportType);
      reportArchive.setContent(content);

      broker.makePersistent(reportArchive);
      
      content.getDocuments().add(reportArchive);
      broker.updateObject(content);
      OpContentManager.updateContent(content, broker, true);

      //permissions
      addReportPermissions(session, broker, reportArchive);

      tx.commit();
      broker.close();
      return null;
   }

   /**
    * Adds report permissions for the given report object. Make sure the report has a creator before this method is called.
    *  
    * @param session a <code>OpProjectSession</code> representing a server session.
    * @param broker a <code>OpBroker</code> used for performing business operations.
    * @param report a <code>OpReport</code> representing the report for which to save the permissions.
    */
   static void addReportPermissions(OpProjectSession session, OpBroker broker, OpReport report) {
      OpPermissionSetFactory.addSystemObjectPermissions(session, broker, report);
      OpPermission permission = new OpPermission();
      permission.setObject(report);
      permission.setSubject(report.getCreator());
      permission.setAccessLevel(OpPermission.MANAGER);
      broker.makePersistent(permission);
   }

   /**
    * Generates the name of the generated report.
    *
    * @return a <code>String</code> reprensenting the name of the report.
    */
   private String generateReportName(String jasperFileName, OpReportManager reportManager) {
      StringBuffer result = new StringBuffer();
      result.append(reportManager.getLocalizedJasperFileName(jasperFileName));
      result.append(REPORT_NAME_SEPARATOR_CHAR);
      result.append(XCalendar.getDefaultCalendar().getCurrentTimeShortFormat());
      return result.toString();
   }

   /**
    * Prepares a report for opening by creating a temporary file with the report content.
    *
    * @param session       a <code>XSession</code> object representing the current session.
    * @param request a <code>XMessage</code> representing the current request.
    * @return a <code>XMessage</code> representing the response.
    */
   public XMessage prepareReportOpening(OpProjectSession session, XMessage request) {

      XMessage response = new XMessage();
      List reportIds = (List) request.getArgument(REPORT_IDS);

      OpBroker broker = session.newBroker();

      List reportUrls = getReportUrls(session, broker, reportIds);
      if (reportUrls.isEmpty()) {
         response.setError(session.newError(ERROR_MAP, OpReportArchiveError.INSUFICIENT_PRIVILEGES));
         return response;
      }
      response.setArgument(REPORT_URLS, reportUrls);

      if (OpInitializer.isMultiUser()) {
         List contentIds = getContentIds(session, broker, reportIds);
         if (contentIds.isEmpty()) {
            response.setError(session.newError(ERROR_MAP, OpReportArchiveError.INSUFICIENT_PRIVILEGES));
            return response;
         }
         response.setArgument(CONTENT_IDS, contentIds);
      }
      else {
         response.setArgument(CONTENT_IDS, null);
      }
      broker.close();
      return response;
   }

   /**
    * Gets a list of content ids, from a given list of report ids.
    *
    * @param broker    a <code>OpBroker</code> used to perform business operations.
    * @param reportIds a <code>List</code> of <code>String</code> representing report locators.
    * @return a <code>List</code> of <code>String</code> representing content locators.
    */
   private List getContentIds(OpProjectSession session, OpBroker broker, List reportIds) {
      List result = new ArrayList(reportIds.size());
      for (Iterator it = reportIds.iterator(); it.hasNext();) {
         String reportLocator = (String) it.next();
         OpReport report = (OpReport) broker.getObject(reportLocator);
         if (session.checkAccessLevel(broker, report.getID(), OpPermission.OBSERVER)) {
            result.add(OpLocator.locatorString(report.getContent()));
         }
      }
      return result;
   }

   /**
    * Gets a list of report urls, from a given list of report ids.
    *
    * @param broker    a <code>OpBroker</code> used to perform business operations.
    * @param reportIds a <code>List</code> of <code>String</code> representing report locators.
    * @return a <code>List</code> of <code>String</code> representing local report urls.
    */
   private List getReportUrls(OpProjectSession session, OpBroker broker, List reportIds) {
      List result = new ArrayList(reportIds.size());
      Iterator it = reportIds.iterator();
      while (it.hasNext()) {
         String reportId = (String) it.next();
         OpReport report = (OpReport) broker.getObject(reportId);
         if (!session.checkAccessLevel(broker, report.getID(), OpPermission.OBSERVER)) {
            continue;
         }
         byte[] content = report.getContent().getBytes();
         try {
            String name = report.getName();
            String fileName = name.substring(0, name.indexOf(REPORT_NAME_SEPARATOR_CHAR));
            fileName = fileName + String.valueOf(System.currentTimeMillis());

            //<FIXME author="Ovidiu Lupas" description="support only for application/pdf mime type">
            File temporaryReport = File.createTempFile(fileName, ".pdf");
            //</FIXME>
            
            // this file is needed only temporary, so we delete it when JVM ends.
            temporaryReport.deleteOnExit();

            FileOutputStream fos = new FileOutputStream(temporaryReport);
            fos.write(content);
            fos.flush();
            fos.close();
            result.add(XEncodingHelper.encodeValue(temporaryReport.getCanonicalFile().toURL().toExternalForm()));
         }
         catch (IOException e) {
            //this is needed to keep this list in sync with the ids list (coming from report_archive.jes)
            result.add(null);
            logger.error("Cannot create temporary report " + report.getName(), e);
         }
      }
      return result;
   }

   /**
    * Removes a report from the database.
    *
    * @param session       a <code>XSession</code> object representing the current session.
    * @param request a <code>XMessage</code> representing the current request.
    * @return a <code>XMessage</code> representing the response.
    */
   public XMessage deleteReports(OpProjectSession session, XMessage request) {
      List reportIds = (List) request.getArgument(REPORT_IDS);

      OpBroker broker = session.newBroker();
      OpTransaction tx = broker.newTransaction();

      Iterator it = reportIds.iterator();
      while (it.hasNext()) {
         OpReport report = (OpReport) broker.getObject((String) it.next());
         if (report != null) {
            OpContent content = report.getContent();
            //<FIXME author="Horia Chiorean" description="Remove this when using the content manager">
            int refCount = content.getRefCount();
            refCount--;
            if (refCount == 0) {
               broker.deleteObject(content);
            }
            //<FIXME>
            broker.deleteObject(report);
         }
      }

      tx.commit();
      broker.close();
      return null;
   }
}
