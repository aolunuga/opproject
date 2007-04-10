/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.report_archive.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.team.modules.report_archive.OpReportArchiveModule;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider class for the edit report form.
 *
 * @author horia.chiorean
 */
public class OpEditReportFormProvider implements XFormProvider {
   private final String PERMISSION_SET = "PermissionSet";
   private final String PERMISSIONS_TAB = "PermissionsTab";

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      boolean isEditMode = ((Boolean) parameters.get("editMode")).booleanValue();
      String reportLocator = (String) parameters.get("reportId");

      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      OpReport report = (OpReport) broker.getObject(reportLocator);

      byte accessLevel = session.effectiveAccessLevel(broker, report.getID());
      if (isEditMode && (accessLevel < OpPermission.MANAGER)) {
         isEditMode = false;
      }

      //populate the form's regular data
      populateForm(form, report, isEditMode, session.getLocale().getID());

      if (OpInitializer.isMultiUser()) {
         //set the permissisons
         XComponent permissionSet = form.findComponent(PERMISSION_SET);
         OpPermissionSetFactory.retrievePermissionSet(session, broker, report.getPermissions(), permissionSet,
              OpReportArchiveModule.REPORT_ACCESS_LEVELS, session.getLocale());
         OpPermissionSetFactory.administratePermissionTab(form, isEditMode, accessLevel);
      }
      else {
          form.findComponent(PERMISSIONS_TAB).setHidden(true);
      }
   }

   /**
    * Populates the edit-form with data.
    *
    * @param form     a <code>XComponent(FORM)</code> representing the edit form.
    * @param report   a <code>OpReport</code> entity containing the data.
    * @param editMode a <code>boolean</code> indicating whether edit or view should be used.
    * @param localeId a <code>String</code> representing the id of the current locale.
    */
   private void populateForm(XComponent form, OpReport report, boolean editMode, String localeId) {
      //report name
      XComponent reportNameField = form.findComponent("ReportName");
      reportNameField.setStringValue(report.getName());
      reportNameField.setEnabled(editMode);

      //report type
      XComponent reportTypeField = form.findComponent("ReportType");
      String reportTypeName = OpReportArchiveFormProvider.getI18nReportType(report.getType(), localeId);
      reportTypeField.setStringValue(reportTypeName);
      reportTypeField.setEnabled(editMode);

      //created by
      XComponent createdByField = form.findComponent("CreatedBy");
      String displayName = report.getCreator().getDisplayName();
      //<FIXME author="Horia Chiorean" description="For administrator...">
      if (displayName.startsWith("{$")) {
         displayName = report.getCreator().getName();
      }
      //<FIXME>
      createdByField.setStringValue(displayName);
      createdByField.setEnabled(editMode);

      XComponent createdOnField = form.findComponent("CreatedOn");
      createdOnField.setDateValue(report.getCreated());
      createdOnField.setEnabled(editMode);

      if (!editMode) {
         form.findComponent("cancelButton").setVisible(false);
      }
   }
}
