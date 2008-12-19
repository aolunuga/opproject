/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.reports.work_report;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;

public class OpWorkReportFormProvider implements XFormProvider {

   private final static String USER_NAME_FIELD = "UserNameField";
   private final static String USER_NAME_LABEL = "UserLabel";
   private final static String START_FIELD = "StartField";
   private final static String FINISH_FIELD = "FinishField";
   private final static String TOTAL_HOURS_FIELD = "TotalHoursField";

   // Form parameters
   private final static String RUN_QUERY = "RunQuery";
   private final static String USER_LOCATOR = "UserLocator";
   private final static String START = "Start";
   private final static String FINISH = "Finish";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionDataSetFactory.USER_OBJECTS));
      String displayName;
      OpBroker broker = session.newBroker();
      try {
         // default start-finish dates range
         Date defaultStartDate = session.getCalendar().getCurrentYearFirstDate();
         Date defaultFinishDate = session.getCalendar().getCurrentYearLastDate();

         // Execute query and fill result set if RunQuery is true
         if (parameters != null) {
            Boolean runQuery = (Boolean) parameters.get(RUN_QUERY);
            if (runQuery != null && runQuery.booleanValue()) {

               String userLocator = (String) parameters.get(USER_LOCATOR);
               if (userLocator == null) {
                  broker.close();
                  return; // TODO: Throw exception
               }

               OpUser user = (OpUser) broker.getObject(userLocator);
               if (user == null) {
                  broker.close();
                  return; // TODO: Throw exception
               }
               displayName = localizer.localize(user.getDisplayName());
               form.findComponent(USER_NAME_FIELD).setStringValue(XValidator.choice(user.locator(), displayName));

               // TODO: Use calendar instance of session to set default
               // start/finish dates
               Date start = (Date) parameters.get(START);
               if (start == null) {
                  start = defaultStartDate;
               }
               form.findComponent(START_FIELD).setDateValue(start);

               Date finish = (Date) parameters.get(FINISH);
               if (finish == null) {
                  finish = defaultFinishDate;
               }
               form.findComponent(FINISH_FIELD).setDateValue(finish);


               // Execute query
               StringBuffer queryBuffer = new StringBuffer("select resource.Name, sum(workRecord.ActualEffort), sum(workRecord.PersonnelCosts)");
               queryBuffer.append(", sum(workRecord.TravelCosts), sum(workRecord.MaterialCosts), sum(workRecord.ExternalCosts), sum(workRecord.MiscellaneousCosts)");
               queryBuffer.append(" from OpWorkSlip workSlip inner join workSlip.Creator creator inner join creator.Resources res");
               queryBuffer.append(" inner join workSlip.Records workRecord inner join workRecord.Assignment assignment inner join assignment.Resource resource inner join assignment.Activity activity inner join activity.ProjectPlan projectPlan inner join projectPlan.ProjectNode projectNode ");
               queryBuffer.append(" where projectNode.Archived=false and res.id = resource.id and creator.id = ? and workSlip.Date >= ? and workSlip.Date <= ? group by resource.id, resource.Name order by resource.Name");

               OpQuery query = broker.newQuery(queryBuffer.toString());
               query.setLong(0, user.getId());
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

               form.findComponent(TOTAL_HOURS_FIELD).setDoubleValue(totalHours);

            }
            else {
               form.findComponent(START_FIELD).setDateValue(defaultStartDate);
               form.findComponent(FINISH_FIELD).setDateValue(defaultFinishDate);
               OpUser user = session.user(broker);
               displayName = localizer.localize(user.getDisplayName());
               form.findComponent(USER_NAME_FIELD).setStringValue(XValidator.choice(user.locator(), displayName));
            }
         }
         else {
            form.findComponent(START_FIELD).setDateValue(defaultStartDate);
            form.findComponent(FINISH_FIELD).setDateValue(defaultFinishDate);
            OpUser user = session.user(broker);
            displayName = localizer.localize(user.getDisplayName());
            form.findComponent(USER_NAME_FIELD).setStringValue(XValidator.choice(user.locator(), displayName));
         }


         if (OpEnvironmentManager.isMultiUser()) {
            //enable select user button if session user is admin
            if (!session.userIsAdministrator()){
               form.findComponent(USER_NAME_FIELD).setEnabled(false);
            }
         }
         else {
            form.findComponent(USER_NAME_FIELD).setVisible(false);
            form.findComponent(USER_NAME_LABEL).setVisible(false);
         }
      }
      finally {
         broker.close();
      }
   }
}
