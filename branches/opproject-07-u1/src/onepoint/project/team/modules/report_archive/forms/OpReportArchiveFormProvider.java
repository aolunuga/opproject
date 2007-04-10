/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.report_archive.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpDynamicResource;
import onepoint.project.modules.documents.OpDynamicResourceManager;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.report.OpReportType;
import onepoint.resource.XLocale;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Form provider class for the report archive main form.
 *
 * @author horia.chiorean
 */
public class OpReportArchiveFormProvider implements XFormProvider {

   /**
    * The id of the report archive data set.
    */
   private static final String REPORT_DATASET_ID = "ReportArchiveDataSet";
   /**
    * The id of the report types tool bar.
    */
   private static final String REPORT_TYPES_TOOL_BAR = "ReportTypesToolBar";

   /*script action that should be performed when a report type button is pressed */
   private static final String ACTION = "filterReports";

   /*the report type used for filtering */
   private static final String REPORT_TYPE = "report_type_locator";
   /*report type tool button icon */
   private static final String ICON_PATH = "/modules/report_archive/icons/report_archive_tool.png";

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    * <FIXME author="Horia Chiorean" description="Maybe add permissions checks later on.">
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

//      String reportTypesQuery = "select report.Type from OpReport as report group by report.Type";
//      OpQuery query = broker.newQuery(reportTypesQuery);
//      Iterator it = broker.iterate(query);
//
//      XComponent reportTypesToolBar = form.findComponent(REPORT_TYPES_TOOL_BAR);
//      fillReportTypeToolBar(reportTypesToolBar,it,localeId);

      OpQuery query;
      String localeId = session.getLocale().getID();
      String reportArchiveQuery = "select report from OpReport as report";
      if (parameters.isEmpty()){
         query = broker.newQuery(reportArchiveQuery);
      }
      else {/*filter reports by type*/
         String reportTypeLocator = (String)parameters.get(REPORT_TYPE);
         long reportTypeID = OpLocator.parseLocator(reportTypeLocator).getID();
         reportArchiveQuery = "select report from OpReport as report where report.Type.ID = ?";
         query = broker.newQuery(reportArchiveQuery);
         query.setLong(0,reportTypeID);
      }
      Iterator it = broker.iterate(query);

      XComponent reportArchiveDataSet = form.findComponent(REPORT_DATASET_ID);
      fillReportArchiveDataSet(reportArchiveDataSet, it, session, broker);

      form.findComponent("ManagerPermission").setByteValue(OpPermission.MANAGER);

      form.findComponent("InfoTool").setEnabled(false);
      form.findComponent("DeleteTool").setEnabled(false);

      broker.close();
   }

   /**
    * Fills the report archive data set with the necessary data.
    * @param dataSet a <code>XComponent(DATA_SET)</code>.
    * @param it a <code>Iterator</code> over a set of <code>OpReport</code> entities.
    * @param session current session
    */
   private void fillReportArchiveDataSet(XComponent dataSet, Iterator it, OpProjectSession session, OpBroker broker) {

      XLocale locale = session.getLocale();
      Map sortedReports = new TreeMap();

      while (it.hasNext()) {
         OpReport report = (OpReport) it.next();
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);

         //id (locator) - 0
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(OpLocator.locatorString(report));
         dataRow.addChild(dataCell);

         //name - 1
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(report.getName());
         dataRow.addChild(dataCell);

         //choice - [reportTypeLocator, reportTypeName] - 2
         OpReportType type = report.getType();
         String localizedReportName = getI18nReportType(type, locale.getID());
         String reportTypeChoice = XValidator.choice(OpLocator.locatorString(type), localizedReportName);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(reportTypeChoice);
         dataRow.addChild(dataCell);

         //created by - 3
         dataCell = new XComponent(XComponent.DATA_CELL);
         XLocalizer userObjectsLocalizer = new XLocalizer();
         userObjectsLocalizer.setResourceMap(locale.getResourceMap(OpPermissionSetFactory.USER_OBJECTS));
         String displayName = userObjectsLocalizer.localize(report.getCreator().getDisplayName());
         dataCell.setStringValue(displayName);
         dataRow.addChild(dataCell);

         //created on - 4
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDateValue(report.getCreated());
         dataRow.addChild(dataCell);
         sortedReports.put(report.getCreated(), dataRow);

         //permissions - 5
         dataCell = new XComponent(XComponent.DATA_CELL);
         byte effectivePermission = session.effectiveAccessLevel(broker, report.getID());
         dataCell.setByteValue(effectivePermission);
         dataRow.addChild(dataCell);

      }

      //populate the data-set from the sorted map
      Iterator mapIt = sortedReports.keySet().iterator();
      while (mapIt.hasNext()) {
         XComponent row = (XComponent) sortedReports.get(mapIt.next());
         dataSet.addChild(row);
      }
   }

   /**
    * Fills the report type collapsing box with the persistent report types
    *
    * @param toolBar  <code>XComponent.TOOL_BAR</code> which will contain a <code>XComponent.TOOL_BUTTON</code> for
    *                 each persistent report type
    * @param it       <code>Iterator</code> over a set of <code>OpReportType</code> entities.
    * @param localeId <code>String</code> representing the id of the current locale.
    */
   private void fillReportTypeToolBar(XComponent toolBar, Iterator it, String localeId) {
      while (it.hasNext()) {
         XComponent reportTypeToolButton = new XComponent(XComponent.TOOL_BUTTON);
         OpReportType type = (OpReportType) it.next();
         String localizedReportName = getI18nReportType(type, localeId);
         /*set caption */
         reportTypeToolButton.setText(localizedReportName);
         /*set report type locator*/
         reportTypeToolButton.setStringValue(OpLocator.locatorString(type));
         /* set script action*/
         reportTypeToolButton.setOnButtonPressed(ACTION);
         /* set icon*/
         reportTypeToolButton.setIcon(ICON_PATH);
         toolBar.addChild(reportTypeToolButton);
      }
   }

   /**
    * Gets the name of the report type, based on the current locale.
    * @param reportType a <code>OpReportType</code> entity.
    * @param localeId a <code>String</code> representing the id of a locale.
    * @return a <code>String</code> representing the i18ned report type.
    */
   static String getI18nReportType(OpReportType reportType, String localeId) {
      OpDynamicResource dynamicResource = OpDynamicResourceManager.getDynamicResourceForLocale(localeId, reportType);
      String localizedReportName = reportType.getName();
      if (dynamicResource != null) {
          localizedReportName = dynamicResource.getValue();
      }
      return localizedReportName;
   }
}
