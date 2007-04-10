/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.reports.category_report;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Form provider class for the category report.
 *
 * @author horia.chiorean
 */
public class OpCategoryReportFormProvider implements XFormProvider {

   /**
    * Form component ids.
    */
   public final static String RESULT_SET = "ResultSet";
   public final static String START_FIELD = "StartField";
   public final static String FINISH_FIELD = "FinishField";

   /**
    * Form parameters.
    */
   public final static String RUN_QUERY = "RunQuery";

   /**
    * This class's logger
    */
   private final static XLog logger = XLogFactory.getLogger(onepoint.project.team.reports.category_report.OpCategoryReportFormProvider.class);

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;


      // Execute query and fill result set if RunQuery is true
      if (parameters != null && parameters.size() > 0) {
         OpBroker broker = ((OpProjectSession) session).newBroker();
         Boolean runQuery = (Boolean) parameters.get(onepoint.project.team.reports.category_report.OpCategoryReportFormProvider.RUN_QUERY);
         if (runQuery != null && runQuery.booleanValue()) {
            //start date
            Date start = (Date) parameters.get("Start");
            if (start == null) {
               onepoint.project.team.reports.category_report.OpCategoryReportFormProvider.logger.error("XCategoryFormProvider: No start date given");
               broker.close();
               return;
            }
            //end date
            Date end = (Date) parameters.get("Finish");
            if (end == null) {
               onepoint.project.team.reports.category_report.OpCategoryReportFormProvider.logger.error("XCategoryFormProvider: No end date given");
               broker.close();
               return;
            }

            // Execute query
            StringBuffer queryBuffer = new StringBuffer("select category.Name, sum(activity.ActualEffort), sum(activity.ActualPersonnelCosts), sum(activity.ActualTravelCosts), sum(activity.ActualMaterialCosts), sum(activity.ActualExternalCosts), sum(activity.ActualMiscellaneousCosts)");
            queryBuffer.append(" from OpActivityCategory category inner join category.Activities activity ");
            queryBuffer.append(" where activity.Start >= ? and activity.Finish <= ? and activity.Deleted = false and activity.Template = false and category.Active = true");
            queryBuffer.append(" group by category.Name order by category.Name");

            OpQuery query = broker.newQuery(queryBuffer.toString());
            query.setDate(0, start);
            query.setDate(1, end);

            Iterator i = broker.iterate(query);
            Object[] record = null;
            XComponent resultSet = form.findComponent("ResultSet");
            XComponent resultRow = null;
            XComponent resultCell = null;
            while (i.hasNext()) {
               record = (Object[]) i.next();
               resultRow = new XComponent(XComponent.DATA_ROW);
               resultCell = new XComponent(XComponent.DATA_CELL);

               //category name - 0
               resultCell.setStringValue((String) record[0]);
               resultRow.addChild(resultCell);

               //sum(actual_effort) - 1
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[1]).doubleValue());
               resultRow.addChild(resultCell);

               //sum(personnel costs) - 2
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[2]).doubleValue());
               resultRow.addChild(resultCell);

               //sum(travel costs) - 3
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[3]).doubleValue());
               resultRow.addChild(resultCell);

               //sum(material costs) - 4
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[4]).doubleValue());
               resultRow.addChild(resultCell);

               //sum(external costs) - 5
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[5]).doubleValue());
               resultRow.addChild(resultCell);

               //sum(miscellaneous costs) - 6
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[6]).doubleValue());
               resultRow.addChild(resultCell);

               resultSet.addChild(resultRow);
            }
            form.findComponent(onepoint.project.team.reports.category_report.OpCategoryReportFormProvider.START_FIELD).setDateValue(start);
            form.findComponent(onepoint.project.team.reports.category_report.OpCategoryReportFormProvider.FINISH_FIELD).setDateValue(end);
         }
         broker.close();
      }
      else {
         form.findComponent(onepoint.project.team.reports.category_report.OpCategoryReportFormProvider.START_FIELD).setDateValue(XCalendar.getDefaultCalendar().getCurrentYearFirstDate());
         form.findComponent(onepoint.project.team.reports.category_report.OpCategoryReportFormProvider.FINISH_FIELD).setDateValue(XCalendar.getDefaultCalendar().getCurrentYearLastDate());
      }
   }
}
