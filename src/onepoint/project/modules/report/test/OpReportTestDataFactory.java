/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.report.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.report.OpReportService;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class was introduce to produce data for all report related tests.
 *
 * @author calin.pavel
 */
public class OpReportTestDataFactory {
   // Name of the report used for testing.
   public static final String REPORT_NAME = "workreport.jar";

   // Query for retrieving all reports
   private final static String SELECT_REPORTS_QUERY = "select report from OpReport as report";

   // Query for retrieving all contents
   private final static String SELECT_CONTENTS_QUERY = "select content from OpContent as content";


   private OpProjectSession session;

   /**
    * Creates a new instance of data factory.
    *
    * @param session seesion to be used.
    */
   public OpReportTestDataFactory(OpProjectSession session) {
      this.session = session;
   }

   /**
    * Builds a new request with the following arguments
    * FIELDS
    * Map: "sum_material"=Integer(4), "sum_external"=Integer(5), "cat_name"=Integer(0), "sum_travel"=Integer(3),
    * "sum_actualeffort"=Integer(1), "cat_description"=Integer(7), "sum_misc"=Integer(6), "sum_personnel"=Integer(2)
    * <p/>
    * NAME
    * String: workreport.jar
    * <p/>
    * QUERY
    * Map:
    * - queryString=select category.Name, sum(activity.ActualEffort), sum(activity.ActualPersonnelCosts),
    * sum(activity.ActualTravelCosts), sum(activity.ActualMaterialCosts), sum(activity.ActualExternalCosts),
    * sum(activity.ActualMiscellaneousCosts), category.Description from OpActivityCategory category inner
    * join category.Activities activity  where activity.Start >= ? and activity.Finish <= ? and
    * activity.Deleted = false and activity.Template = false and category.Active = true group by
    * category.Name, category.Description order by category.Name
    * - queryParams=ArrayList of SQL dates[2007-01-01, 2007-12-31]
    * <p/>
    * PARAMETERS
    * Map:
    * - UserId = the ID of the user
    * - toDate = SQLDate (2007-12-31)
    * - fromDate = SQLDate (2007-01-01)
    * <p/>
    * FORMATS
    * ArrayList ["PDF"]
    *
    * @param format report format (PDF, XML, HTML, ...)
    */
   public static XMessage buildDefaultRequest(String format, long userId) {


      String startDate = "2007-01-01";
      String endDate = "2007-12-31";

      Map fieldsMap = new HashMap();
      fieldsMap.put("sum_material", new Integer(4));
      fieldsMap.put("sum_external", new Integer(5));
      fieldsMap.put("cat_name", new Integer(0));
      fieldsMap.put("sum_travel", new Integer(3));
      fieldsMap.put("sum_actualeffort", new Integer(1));
      fieldsMap.put("cat_description", new Integer(7));
      fieldsMap.put("sum_misc", new Integer(6));
      fieldsMap.put("sum_personnel", new Integer(2));

      Map queryMap = new HashMap();
      queryMap.put("queryString", "select category.Name, sum(activity.ActualEffort), sum(activity.ActualPersonnelCosts), " +
           "sum(activity.ActualTravelCosts), sum(activity.ActualMaterialCosts), sum(activity.ActualExternalCosts)," +
           " sum(activity.ActualMiscellaneousCosts), category.Description from OpActivityCategory category inner join " +
           "category.Activities activity  where activity.Start >= ? and activity.Finish <= ? and " +
           "activity.Deleted = false and activity.Template = false and category.Active = true group by " +
           "category.Name, category.Description order by category.Name");
      queryMap.put("queryParams", Arrays.asList(new Date[]{Date.valueOf(startDate), Date.valueOf(endDate)}));

      Map parametersMap = new HashMap();
      parametersMap.put("fromDate", Date.valueOf(startDate));
      parametersMap.put("toDate", Date.valueOf(endDate));
      parametersMap.put("UserId", new Long(userId));

      XMessage request = new XMessage();
      request.setArgument(OpReportService.FIELDS, fieldsMap);
      request.setArgument(OpReportService.NAME, REPORT_NAME);
      request.setArgument(OpReportService.QUERY_MAP, queryMap);
      request.setArgument(OpReportService.PARAMETERS, parametersMap);
      request.setArgument(OpReportService.FORMATS, Arrays.asList(new String[]{format}));

      return request;
   }

   /**
    * This method returns all reports available into system database.
    *
    * @return a list of <code>OpReport</code> instances.
    */
   public List getAllReports() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery(SELECT_REPORTS_QUERY);
      List groups = broker.list(query);
      broker.close();

      return groups;
   }

   /**
    * This method returns all contents available into system database.
    *
    * @return a list of <code>OpContent</code> instances.
    */
   public List getAllContents() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery(SELECT_CONTENTS_QUERY);
      List groups = broker.list(query);
      broker.close();

      return groups;
   }
}
