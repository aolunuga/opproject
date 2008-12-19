/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpNewConfigurationHandler;
import onepoint.project.configuration.generated.OpReportWorkflow;
import onepoint.project.modules.report.OpReportManager;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpUser;
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

   /**
    *  Session variable indicating the last report query type
    */
   private static final String REPORT_QUERY_TYPE = "reportQueryType";

   private static final String PREVIOUS_QUERY_FORM = "PreviousQueryForm";

   /**
    * Buttons ids
    */
   private static final String CREATE_REPORT_BUTTON = "CreateReportButton";
   private static final String RUN_QUERY_BUTTON = "RunQueryButton";
   private static final String SEND_REPORT_BUTTON = "SendReportButton";
   private static final String RESET_QUERY_BUTTON = "ResetQueryButton";
   private static final String PRINT_BUTTON = "PrintButton";
   private static final String CURRENCY_FIELD = "Currency";
   private static final String EDIT_MODE = "EditMode";


   public void prepareForm(XSession session, XComponent form, HashMap parameters) {

      //first initialize some stuff...
      OpProjectSession s = (OpProjectSession) session;
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(s.getLocale().getResourceMap(REPORT_RESOUCE));
      OpReportManager xrm = OpReportManager.getReportManager(s);
      // fill the data-set
      XComponent resultSet = form.findComponent(RESULT_SET);
      XComponent resultRow = null;
      //fill the currency field
      XComponent currencyField = form.findComponent(CURRENCY_FIELD);
      currencyField.setStringValue(OpSettingsService.getService().getStringValue(s, OpSettings.CURRENCY_SYMBOL));    

      //Now we deal with the jasper-stuff...
      String currLocale = s.getLocale().getID();
      HashMap jasperReports = xrm.getJasperDescriptions();
      Iterator reportEnum = jasperReports.keySet().iterator();
      String lastSelectedReport = (String) session.getVariable(REPORT_QUERY_TYPE);
      StringBuffer queryFormName = null;
      while (reportEnum.hasNext()) {
         resultRow = new XComponent(XComponent.DATA_ROW);
         String reportName = (String) reportEnum.next();
         String jesname = (String) xrm.getJesName(reportName);
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
      OpBroker broker = s.newBroker();
      OpUser user = s.user(broker);
      boolean editMode = true;
      if (user.getLevel() < OpUser.MANAGER_USER_LEVEL) {
         editMode = false;
      }
      broker.close();

      form.findComponent(EDIT_MODE).setBooleanValue(editMode);
      if (queryFormName != null) {
         prepareQueryFormPresent(form, editMode, queryFormName.toString());
      }
      else {
         prepareQueryFormAbsent(form, editMode);
      }
   }

   /**
    * Prepares the report list form, when a query form doesn't exist.
    * @param form a <code>XComponent(FORM)</code> representing the report list form.
    * @param editMode
    */
   protected void prepareQueryFormAbsent(XComponent form, boolean editMode) {
      form.findComponent(CREATE_REPORT_BUTTON).setEnabled(false);
      form.findComponent(RUN_QUERY_BUTTON).setEnabled(false);
      form.findComponent(RESET_QUERY_BUTTON).setEnabled(false);
      form.findComponent(PRINT_BUTTON).setEnabled(false);
      boolean sendReportEnabled = isSendReportEnabled();
      if (sendReportEnabled && editMode) {
         form.findComponent(SEND_REPORT_BUTTON).setVisible(true);
         form.findComponent(SEND_REPORT_BUTTON).setEnabled(false);
      } 
      else {
         form.findComponent(SEND_REPORT_BUTTON).setVisible(false);
      }
   }

   /**
    * Prepares the report list form, when a query form exists.
    * @param form a <code>XComponent(FORM)</code> representing the report list form.
    * @param editMode
    * @param queryFormName a <code>String</code> representing the name of the query form.
    */
   protected void prepareQueryFormPresent(XComponent form, boolean editMode, String queryFormName) {
      XComponent previousQueryFrameField = form.findComponent(PREVIOUS_QUERY_FORM);
      previousQueryFrameField.setStringValue(queryFormName);
      form.findComponent(CREATE_REPORT_BUTTON).setEnabled(editMode);
      form.findComponent(RUN_QUERY_BUTTON).setEnabled(editMode);
      form.findComponent(RESET_QUERY_BUTTON).setEnabled(editMode);
      boolean sendReportEnabled = isSendReportEnabled();
      if (sendReportEnabled && editMode) {
         form.findComponent(SEND_REPORT_BUTTON).setVisible(true);
         form.findComponent(SEND_REPORT_BUTTON).setEnabled(true);
      } 
      else {
         form.findComponent(SEND_REPORT_BUTTON).setVisible(false);
      }
   }
   
   /**
    * Checks if report workflow is configured within configuration file.
    * @return true if report workflow is enabled within configuration file, false otherwise.
    */
   private boolean isSendReportEnabled() {
      OpReportWorkflow reportWorkflow = OpNewConfigurationHandler.getInstance().getOpConfiguration().getReportWorkflow();
      return ((reportWorkflow != null) && (reportWorkflow.isEnabled()));
   }
}
