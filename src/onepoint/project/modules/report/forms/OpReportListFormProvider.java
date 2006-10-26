/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.report.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.report.OpReportManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpReportListFormProvider implements XFormProvider {

   /**
    * This is the string, which determines in what field of the form we put the report-list.
    * Has to match settings in the "report.oxf.xml"
    */
   private final static String RESULT_SET = "QuerySet";
   /**
    * determines the resourcemap to use. It is taken from "report.oxf.xml".
    */
   private final static String REPORT_RESOUCE = "report.report";

   /* session variable indicating the last report query type*/
   private static final String REPORT_QUERY_TYPE = "reportQueryType";

   private static final String PREVIOUS_QUERY_FORM = "PreviousQueryForm";

   /**
    * Buttons ids
    */
   private static final String CREATE_REPORT_BUTTON = "CreateReportButton";
   private static final String RUN_QUERY_BUTTON = "RunQueryButton";
   private static final String RESET_QUERY_BUTTON = "ResetQueryButton";
   private static final String PRINT_BUTTON = "PrintButton";


   public void prepareForm(XSession session, XComponent form,
        HashMap parameters) {
      //first initialize some stuff...
      OpProjectSession s = (OpProjectSession) session;
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(s.getLocale().getResourceMap(REPORT_RESOUCE));
      OpReportManager xrm = OpReportManager.getReportManager(s);
      // fill the data-set
      XComponent resultSet = form.findComponent(RESULT_SET);
      XComponent resultRow = null;

      //Now we deal with the jasper-stuff...
      String currLocale = s.getLocale().getID();
      HashMap jasperReports = xrm.getJasperDescriptions();
      Iterator reportEnum = jasperReports.keySet().iterator();
      String lastSelectedReport = (String) session.getVariable(REPORT_QUERY_TYPE);
      StringBuffer queryFormName = null;
      while (reportEnum.hasNext()) {
         resultRow = new XComponent(XComponent.DATA_ROW);
         String reportName = (String) reportEnum.next();
         String jesname = xrm.getJesName(reportName);
         String localizedReportName = xrm.getLocalizedJasperFileName(reportName, currLocale);
         resultRow.setStringValue(jesname.concat("['").concat(localizedReportName).concat("']"));
         XComponent sortValue = new XComponent(XComponent.DATA_CELL);
         sortValue.setStringValue(localizedReportName);
         resultRow.addChild(sortValue);
         resultSet.addChild(resultRow);
         //check for  previous query form existence
         if (lastSelectedReport != null && localizedReportName.equals(lastSelectedReport)) {
            resultRow.setSelected(true);
            queryFormName = new StringBuffer(jesname);
            queryFormName.append(".oxf.xml");
         }
      }
      resultSet.sort(0);
      //load query form
      if (queryFormName != null) {
         prepareQueryFormPresent(form, queryFormName.toString());
      }
      else {
         prepareQueryFormAbsent(form);
      }
   }

   /**
    * Prepares the report list form, when a query form doesn't exist.
    * @param form a <code>XComponent(FORM)</code> representing the report list form.
    */
   protected void prepareQueryFormAbsent(XComponent form) {
      form.findComponent(CREATE_REPORT_BUTTON).setEnabled(false);
      form.findComponent(RUN_QUERY_BUTTON).setEnabled(false);
      form.findComponent(RESET_QUERY_BUTTON).setEnabled(false);
      form.findComponent(PRINT_BUTTON).setEnabled(false);
   }

   /**
    * Prepares the report list form, when a query form exists.
    * @param form a <code>XComponent(FORM)</code> representing the report list form.
    * @param queryFormName a <code>String</code> representing the name of the query form.
    */
   protected void prepareQueryFormPresent(XComponent form, String queryFormName) {
      XComponent previousQueryFrameField = form.findComponent(PREVIOUS_QUERY_FORM);
      previousQueryFrameField.setStringValue(queryFormName);
      form.findComponent(CREATE_REPORT_BUTTON).setEnabled(true);
   }
}
