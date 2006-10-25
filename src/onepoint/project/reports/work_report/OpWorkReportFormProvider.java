/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.reports.work_report;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;

public class OpWorkReportFormProvider implements XFormProvider {

   public final static String USER_LOCATOR_FIELD = "UserLocatorField";

   public final static String USER_NAME_FIELD = "UserNameField";

   public final static String START_FIELD = "StartField";

   public final static String FINISH_FIELD = "FinishField";

   public final static String RESULT_SET = "ResultSet";

   public final static String TOTAL_HOURS_FIELD = "TotalHoursField";

   public final static String SELECT_USER_BUTTON = "SelectUserButton";

   // Form parameters
   public final static String RUN_QUERY = "RunQuery";

   public final static String USER_LOCATOR = "UserLocator";

   public final static String START = "Start";

   public final static String FINISH = "Finish";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));

      OpBroker broker = session.newBroker();
      // default start-finish dates range
      Date defaultStartDate = XCalendar.getDefaultCalendar().getCurrentYearFirstDate();
      Date defaultFinishDate = XCalendar.getDefaultCalendar().getCurrentYearLastDate();

      // Execute query and fill result set if RunQuery is true
      if (parameters != null) {
         Boolean runQuery = (Boolean) parameters.get(onepoint.project.reports.work_report.OpWorkReportFormProvider.RUN_QUERY);
         if (runQuery != null && runQuery.booleanValue()) {

            String userLocator = (String) parameters.get(onepoint.project.reports.work_report.OpWorkReportFormProvider.USER_LOCATOR);
            if (userLocator == null) {
               broker.close();
               return; // TODO: Throw exception
            }

            OpUser user = (OpUser) broker.getObject(userLocator);
            if (user == null) {
               broker.close();
               return; // TODO: Throw exception
            }
            String displayName = localizer.localize(user.getDisplayName());
            form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.USER_LOCATOR_FIELD).setStringValue(XValidator.choice(user.locator(), displayName));
            form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.USER_NAME_FIELD).setStringValue(displayName);

            // TODO: Use calendar instance of session to set default
            // start/finish dates
            Date start = (Date) parameters.get(onepoint.project.reports.work_report.OpWorkReportFormProvider.START);
            if (start == null) {
               start = defaultStartDate;
            }
            form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.START_FIELD).setDateValue(start);

            Date finish = (Date) parameters.get(onepoint.project.reports.work_report.OpWorkReportFormProvider.FINISH);
            if (finish == null) {
               finish = defaultFinishDate;
            }
            form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.FINISH_FIELD).setDateValue(finish);


            // Execute query
            StringBuffer queryBuffer = new StringBuffer("select resource.Name, sum(workRecord.ActualEffort), sum(workRecord.ActualEffort) * max(resource.HourlyRate)");
            queryBuffer.append(", sum(workRecord.TravelCosts), sum(workRecord.MaterialCosts), sum(workRecord.ExternalCosts), sum(workRecord.MiscellaneousCosts)");
            queryBuffer.append(" from OpWorkSlip workSlip inner join workSlip.Creator creator inner join creator.Resources res");
            queryBuffer.append(" inner join workSlip.Records workRecord inner join workRecord.Assignment assignment inner join assignment.Resource resource");
            queryBuffer.append(" where res.ID = resource.ID and creator.ID = ? and workSlip.Date >= ? and workSlip.Date <= ? group by resource.ID, resource.Name order by resource.Name");

            OpQuery query = broker.newQuery(queryBuffer.toString());
            query.setLong(0, user.getID());
            query.setDate(1, start);
            query.setDate(2, finish);
            Iterator i = broker.iterate(query);
            Object[] record = null;
            XComponent resultSet = form.findComponent("ResultSet");
            XComponent resultRow = null;
            XComponent resultCell = null;
            double totalHours = 0.0d;
            while (i.hasNext()) {
               record = (Object[]) i.next();
               resultRow = new XComponent(XComponent.DATA_ROW);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setStringValue((String) record[0]);
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[1]).doubleValue());
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[2]).doubleValue());
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[3]).doubleValue());
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[4]).doubleValue());
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[5]).doubleValue());
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[6]).doubleValue());
               resultRow.addChild(resultCell);
               resultSet.addChild(resultRow);
               totalHours += ((Double) record[1]).doubleValue();
            }

            form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.TOTAL_HOURS_FIELD).setDoubleValue(totalHours);

         }
         else {
            form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.START_FIELD).setDateValue(defaultStartDate);
            form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.FINISH_FIELD).setDateValue(defaultFinishDate);
            OpUser user = session.user(broker);
            String displayName = localizer.localize(user.getDisplayName());
            form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.USER_LOCATOR_FIELD).setStringValue(XValidator.choice(user.locator(), displayName));
            form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.USER_NAME_FIELD).setStringValue(displayName);
         }
      }
      else {
         form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.START_FIELD).setDateValue(defaultStartDate);
         form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.FINISH_FIELD).setDateValue(defaultFinishDate);
         OpUser user = session.user(broker);
         String displayName = localizer.localize(user.getDisplayName());
         form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.USER_LOCATOR_FIELD).setStringValue(XValidator.choice(user.locator(), displayName));
         form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.USER_NAME_FIELD).setStringValue(displayName);
      }

      //enable select user button if session user is admin
      if (!session.userIsAdministrator()){
         form.findComponent(onepoint.project.reports.work_report.OpWorkReportFormProvider.SELECT_USER_BUTTON).setEnabled(false);
      }

      broker.close();

   }
}
