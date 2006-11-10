/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.report;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpDynamicResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocaleMap;
import onepoint.resource.XLocalizer;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;
import onepoint.util.XEnvironment;

import java.io.*;
import java.net.FileNameMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;

public class OpReportService extends OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpReportService.class, true);

   public final static String JASPER_REPORTS_PATH = "/modules/report/jasperreports/";

   public final static String SAVED_REPORTS_PATH = "/reports/";

   public final static OpReportErrorMap ERROR_MAP = new OpReportErrorMap();

   /**
    * Defines the supported formats for reports.
    */
   public static final String REPORT_TYPE_PDF = "PDF";
   public static final String REPORT_TYPE_HTML = "HTML";
   public static final String REPORT_TYPE_XML = "XML";
   public static final String REPORT_TYPE_XLS = "XLS";
   public static final String REPORT_TYPE_CSV = "CSV";

   public final static String NAME = "name";
   public final static String JARFILE = "jarfile";
   public final static String PARAMETERS = "parameters";
   public final static String FORMATS = "formats";
   public final static String QUERY_MAP = "query";
   public final static String QUERY_STRING = "queryString";
   public final static String QUERY_PARAMS = "queryParams";
   public final static String FIELDS = "fields";
   public final static String RESOURCE_MAP_ID = "resourceMapId";
   public final static String SUBREPORT_DATA = "subReportData";

   // Special parameter names
   public final static String DAY_WORK_TIME = "DayWorkTime";
   public static final String GENERATED_REPORT_PATH = "GeneratedReportPath";
   public static final String GENERATED_REPORT_CONTENT = "GeneratedReportContent";

   public static final String CONTENT_ID = "ContentId";
   public static final String REPORT_TYPE_ID = "ReportTypeId";
   public static final String REPORT_NAME = "ReportName";
   public static final String REPORT_QUERY_TYPE = "reportQueryType";

   public XMessage createReport(XSession s, XMessage request) {
      logger.debug("OpReportService.createReport()");

      String name = (String) (request.getArgument(NAME));
      ArrayList formats = (ArrayList) (request.getArgument(FORMATS));

      OpProjectSession session = (OpProjectSession) s;
      StringBuffer pathBuffer = new StringBuffer();
      OpReportManager xrm = OpReportManager.getReportManager(s);

      try {
         JasperPrint compiledReport = createJasperPrint(session, request);

         // TODO: Format ending and location is "too hard-coded"
         pathBuffer = new StringBuffer(XEnvironment.getVariable(onepoint.project.configuration.OpConfiguration.ONEPOINT_HOME));
         pathBuffer.append(SAVED_REPORTS_PATH);
         //create the saved reports directory if not exists
         File saveReportsDirectory = new File(pathBuffer.toString());
         if (!saveReportsDirectory.exists()) {
            saveReportsDirectory.mkdir();
         }
         pathBuffer.append(xrm.getLocalizedJasperFileName(name));
         pathBuffer.append('-');
         SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh_mm_ss");
         pathBuffer.append(dateFormat.format(new Date()));
         pathBuffer.append(".pdf");


         String path = pathBuffer.toString();

         FileOutputStream resultStream = new FileOutputStream(path);
         //<FIXME author="Horia Chiorean" description="Use the formats here">
         exportReport(session, compiledReport, "PDF", resultStream);
         //<FIXME>
         resultStream.flush();
         resultStream.close();

         XMessage response = new XMessage();
         response.setArgument(GENERATED_REPORT_PATH, new File(path).toURL().toExternalForm());
         return response;
      }
      catch (Exception e) {
         // Whatever happened, we cannot handle it sensefull here...
         logger.error("An Exception occured during execution of Method 'writeReportWorker'", e);
      }
      logger.debug("/OpReportService.createReport()");
      return null;
   }

   /**
    * Saves the report content the database.
    * @param s a <code>XSession</code> representing the current server session.
    * @param request a <code>XMessage</code> representing the current request.
    * @return a <code>XMessage</code> representing the response.
    */
   public XMessage saveReport(XSession s, XMessage request) {
      String name = (String) (request.getArgument(NAME));
      String reportName = name.substring(0, name.lastIndexOf('.'));
      ArrayList formats = (ArrayList) (request.getArgument(FORMATS));
      String format = (String) formats.get(0);

      OpProjectSession session = (OpProjectSession) s;
      OpReportManager xrm = OpReportManager.getReportManager(s);

      OpBroker broker = session.newBroker();
      OpTransaction tx = broker.newTransaction();

      try {
         //create the content
         JasperPrint compiledReport = createJasperPrint(session, request);
         ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
         exportReport(session, compiledReport, format, byteOut);
         OpContent reportContent = createReportContent(broker, byteOut.toByteArray(), format);

         //see if we have a new report type or an existent one
         OpReportType reportType = null;

         OpQuery query = broker.newQuery("select report from OpReportType as report where report.Name=?");
         query.setString(0, reportName);
         Iterator it = broker.iterate(query);
         if (it.hasNext()) {
             reportType = (OpReportType) it.next();
         }
         else {
           reportType = createNewReportType(xrm, broker, name);
         }
         tx.commit();

         //generate the response
         String contentLocator = OpLocator.locatorString(reportContent);
         String reportTypeLocator = OpLocator.locatorString(reportType);

         XMessage response = new XMessage();
         response.setArgument(CONTENT_ID, contentLocator);
         response.setArgument(REPORT_TYPE_ID, reportTypeLocator);
         response.setArgument(REPORT_NAME, name);
         return response;
      }
      catch (OpReportException e) {
         logger.error("Cannot save report into db", e);
      }
      finally {
         broker.close();
      }
      return null;
   }

   /**
    * Creates a new report type.
    *
    * @param reportManager a <code>OpReportManager</code> object used for working with reports.
    * @param broker a <code>OpBroker</code> used to perform the business operations.
    * @param name a <code>String</code> representing the name of the report file, including the .jar extension.
    * @return a <code>OpReportType</code> representing the newly created report type.
    */
   private OpReportType createNewReportType(OpReportManager reportManager, OpBroker broker, String name) {
      String reportName = name.substring(0, name.lastIndexOf('.'));

      //create the report type
      OpReportType reportType = new OpReportType();
      reportType.setName(reportName);

      //get the report type name for all the locales
      XLocaleMap localeMap = XLocaleManager.getLocaleMap();
      Iterator it = localeMap.keyIterator();
      while (it.hasNext()) {
         String localeId = (String) it.next();
         String reportTypeName = reportManager.getLocalizedJasperFileName(name, localeId);
         //create for each language a dynamic resource
         OpDynamicResource dynamicResource = new OpDynamicResource();
         dynamicResource.setLocale(localeId);
         dynamicResource.setValue(reportTypeName);
         dynamicResource.setObject(reportType);
         broker.makePersistent(dynamicResource);
      }
      broker.makePersistent(reportType);
      return reportType;
   }

   /**
    * Creates the actual compiled (and filled) jasper report, which will then be exported to a certain format.
    * @param session a <code>OpProjectSession</code> representing the current server session.
    * @param request a <code>XMessage</code> representing the request parameter.
    * @return a <code>JasperPrint</code> object representing the compiled jasper report.
    */
   private JasperPrint createJasperPrint(OpProjectSession session, XMessage request) {
      String name = (String) (request.getArgument(NAME));
      HashMap parameters = (HashMap) (request.getArgument(PARAMETERS));

      //create the report query
      OpBroker broker = session.newBroker();

      if (parameters != null) {
         Map subReportData = (Map) request.getArgument(SUBREPORT_DATA);
         if (subReportData != null) {
            putSubreportParameters(subReportData, parameters, broker, session);
         }
         //make sure the day work time is taken from the system settings
         String dayWorkTime = (String) parameters.remove(DAY_WORK_TIME);
         if (dayWorkTime != null) {
            parameters.put(DAY_WORK_TIME, new Double(OpSettings.get(OpSettings.CALENDAR_DAY_WORK_TIME)));
         }
      }


      Map queryMap = (Map) request.getArgument(QUERY_MAP);
      if (queryMap == null || queryMap.size() == 0) {
         logger.error("Cannot get main report query map from request");
         return null;
      }
      OpQuery reportQuery = createReportQuery(broker, queryMap);
      if (reportQuery == null) {
         logger.error("Cannot create the report query");
         return null;
      }

      //get the report fields
      Map reportFields = (Map) request.getArgument(FIELDS);

      //get the resource map for the report
      String resourceMapId = (String) request.getArgument(RESOURCE_MAP_ID);
      XLocalizer localizer = null;
      if (resourceMapId != null) {
         localizer = new XLocalizer();
         localizer.setResourceMap(session.getLocale().getResourceMap(resourceMapId));
      }

      //create the report data-source
      OpReportDataSource ds = new OpReportDataSource(reportFields, broker.list(reportQuery).iterator(), localizer);

      JasperReport jasperReport = null;
      OpReportManager xrm = OpReportManager.getReportManager(session);
      String jasperFileName = xrm.getJasperFileName(name);
      if (jasperFileName == null) {
         //something badly went wrong. At least it means, that our reportsCache is corrupt.
         logger.info("got problems with report '" + name + "'. Will try to straighten Cache (fileName).");
         xrm.updateReportCacheEntry(name);
         jasperFileName = xrm.getJasperFileName(name);
         if (jasperFileName == null) {
            //now we give up!!!
            logger.error("could not fix problems with report '" + name + "'. Deleted from Cache (fileName)..");
            xrm.removeReportFromCache(name);
         }
      }

      try {
         InputStream is = new FileInputStream(jasperFileName);
         ObjectInputStream ois = new ObjectInputStream(is);
         jasperReport = (JasperReport) ois.readObject();
      }
      catch (Exception e) {
         //at least we will give it another try. Most probably, the Jasper-file somehow is corrupt (e.g. wrong jasper-version)
         logger.info("got problems with report '" + name + "'. Will try to straighten Cache (jasperReport).");
         xrm.updateReportCacheEntry(name);
         jasperFileName = xrm.getJasperFileName(name);
         try {
            InputStream is = new FileInputStream(jasperFileName);
            ObjectInputStream ois = new ObjectInputStream(is);
            jasperReport = (JasperReport) ois.readObject();
         }
         catch (Exception ee) {
            //nothing to do.
            logger.fatal("could not get valid Jasper-file for '" + name + "'. Please inform the Administrator");
         }
      }
      finally {
         if (jasperReport == null) {
            //somehow we got here without catching an exception earlier ???
            //Nevertheless, we are broken now and do not go on.
            logger.fatal("could not get valid Jasper-file for '" + name + "'. Please inform the Administrator (finally)");
            broker.close();
            return null;
         }
      }

      // Now rewrite the Parameter-Map. The potentially thrown IllegalArgumentException it passed through
      JRParameter[] defParams = jasperReport.getParameters();
      if (parameters == null) {
         parameters = new HashMap();
      }
      Map cleanedReportParameters = null;

      try {
         cleanedReportParameters = updateParameterValues(session, parameters, defParams);
         // some initializing stuff
         URL reportLocation = new File(xrm.getJasperDirName(name)).toURL();
         JasperPrint jasperPrint = OpJasperReportBuilder.buildDatasourceReport(jasperReport, cleanedReportParameters, session, reportLocation, ds);
         return jasperPrint;
      }
      catch (OpReportException e) {
         logger.error("Cannot generate report", e);
      }
      catch (MalformedURLException e) {
         logger.error("Cannot generate report", e);
      }
      finally {
         broker.close();
      }
      return null;
   }

   /**
    * Creates a query that will be used to populate the report.
    * @param broker a <code>OpBroker</code> used for performing business operations.
    * @param queryMap a <code>Map</code> of (String, Object) representing information necessary to create a report query.
    * @return a <code>OpQuery</code> object or null if some of the request parameters are invalid.
    */
   private OpQuery createReportQuery(OpBroker broker, Map queryMap) {
      String queryString = (String) queryMap.get(QUERY_STRING);
      if (queryString == null) {
         logger.error("Query string not found");
         return null;
      }
      List queryParams = (List) queryMap.get(QUERY_PARAMS);
      if (queryParams == null) {
         queryParams = new ArrayList();
      }
      OpQuery query = broker.newQuery(queryString);
      int index = 0;
      for (Iterator it = queryParams.iterator(); it.hasNext();) {
         Object paramValue = it.next();
         //try to parse locator
         if (paramValue instanceof String) {
            OpLocator locator = OpLocator.parseLocator((String) paramValue);
            if (locator != null) {
               query.setID(index, locator.getID());
            }
            else {
               query.setParameter(index, paramValue);
            }
         }
         else {
            query.setParameter(index, paramValue);
         }
         index++;
      }
      return query;
   }

   /**
    * Parses the given request for sub-report related information, and transforms that information creating the necessary
    * sub-report datasources.
    * @param subReportData a <code>Map</code> of (String, Map(fields, queryMap, resourceMapId)) representing subreport data.
    * @param parametersMap a <code>Map</code> of (String, Object) pairs.
    * @param broker a <code>OpBroker</code> used for performing business operations.
    * @param session a <code>OpProjectSession</code> representing the application session.
    */
   private void putSubreportParameters(Map subReportData, Map parametersMap, OpBroker broker, OpProjectSession session) {
      Iterator it = subReportData.keySet().iterator();
      while (it.hasNext()) {
         String subReportDatasourceName = (String) it.next();
         Map subReportMap = (Map) subReportData.get(subReportDatasourceName);

         //subreport fields
         Map subReportFields = (Map) subReportMap.get(FIELDS);

         //subreport query map
         Map subReportQueryMap = (Map) subReportMap.get(QUERY_MAP);
         if (subReportQueryMap == null) {
            logger.error("Cannot create data source for sub report parameter " + subReportDatasourceName);
            continue;
         }
         OpQuery subReportQuery = createReportQuery(broker, subReportQueryMap);

         //subreport language resources
         String resourceMapId = (String) subReportMap.get(RESOURCE_MAP_ID);
         XLocalizer localizer = null;
         if (resourceMapId != null) {
            localizer = new XLocalizer();
            localizer.setResourceMap(session.getLocale().getResourceMap(resourceMapId));
         }

         OpReportDataSource subReportDs = new OpReportDataSource(subReportFields, broker.list(subReportQuery).iterator(),
              localizer);
         parametersMap.put(subReportDatasourceName, subReportDs);
      }
   }

   /**
    * Saves the report into the database.
    *
    * @param broker a <code>OpBroker</code> object used for data persistence.
    * @param content an array of bytes representing the report content.
    * @param contentType a <code>String</code> representing the extension type (pdf, xls etc).
    * @return a <code>OpContent</code> object representing the newly created content.
    */
   private OpContent createReportContent(OpBroker broker, byte[] content, String contentType) {
      OpContent reportContent = new OpContent();
      reportContent.setBytes(content);
      reportContent.setSize(content.length);

      FileNameMap fileMap = URLConnection.getFileNameMap();
      String mimeType = fileMap.getContentTypeFor("." + contentType);
      reportContent.setMediaType(mimeType);

      broker.makePersistent(reportContent);
      return reportContent;
   }

   /**
    * This method exports the results of the report execution in different formats.
    *
    * @param jasperPrint The jasper report execution result.
    */
   private void exportReport(OpProjectSession session, JasperPrint jasperPrint, String type, OutputStream outputStream)
        throws OpReportException {
      OpJasperReportExporter exporter = OpJasperReportExporter.getInstance(type);
      exporter.exportReport(session, jasperPrint, outputStream);
   }

   /**
    * Replace the constants for the Parameter-Map... Right now quite ugly, as not extensible and not typesafe, as we
    * take some String and give back a differently typed Object. In case of ${curDate} the value is the current date and
    * for ${curUser} the value is the ID of the user.
    *
    * @param params
    * @throws OpReportException If the values can not be updated.
    */
   private Map updateParameterValues(OpProjectSession session, HashMap params, JRParameter[] defParams)
        throws OpReportException {
      //go through the defParams Array and convertParameterValue the things we get...
      JRParameter currParam;
      Class typeClass;
      for (int i = 0; i < defParams.length; i++) {
         currParam = defParams[i];

         if (currParam.isSystemDefined() || !currParam.isForPrompting()) {
            // nothing to do about these...
            continue;
         }

         typeClass = currParam.getValueClass();
         // first check, if we have this parameter...
         if (!params.containsKey(currParam.getName())) {
            continue;
         }

         try {
            // now try to "cast".
            Object valObj = params.get(currParam.getName());
            if (valObj == null) {
               params.put(currParam.getName(), typeClass.newInstance());
            }
            else {
               params.put(currParam.getName(), valObj);
            }
         }
         catch (InstantiationException e) {
            throw new OpReportException(session.newError(ERROR_MAP, OpReportError.VALUE_CONVERSION_EXCEPTION));
         }
         catch (IllegalAccessException e) {
            throw new OpReportException(session.newError(ERROR_MAP, OpReportError.VALUE_CONVERSION_EXCEPTION));
         }
      }

      return params;
   }

   /**
    * Sets up the current selected report query type as a session variable.
    * @param s <code>String</code> the session
    * @param request <code>XMessage</code> the request containing as argument the current query type
    */
   public void setSessionReportQueryType(XSession s, XMessage request){
      String queryName = (String)request.getArgument(REPORT_QUERY_TYPE);
      s.setVariable(REPORT_QUERY_TYPE,queryName);

   }

   /**
    * Removes all the files in the reports directory - Used by the client.
    * @param s
    * @param request
    */
   public void reportsCleanUp(XSession s, XMessage request) {
      removeReportFiles();
      OpReportManager manager = OpReportManager.getReportManager(s);
      s.removeResourceInterceptor(manager);
   }

   /**
    * Removes all the files in the reports directory
    */
   public static void removeReportFiles() {
      StringBuffer pathBuffer = new StringBuffer(XEnvironment.getVariable(onepoint.project.configuration.OpConfiguration.ONEPOINT_HOME));
      pathBuffer.append(SAVED_REPORTS_PATH);
      File saveReportsDirectory = new File(pathBuffer.toString());
      if (saveReportsDirectory.exists()) {
         File[] files = saveReportsDirectory.listFiles();
         for (int i = 0; i < files.length; i++) {
            File file = files[i];
            file.delete();
         }
      }
   }

}
