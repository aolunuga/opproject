/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.report_archive.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.List;

/**
 * Form provider for the delete confirmation dialog.
 *
 * @author horia.chiorean
 */
public class OpConfirmDeleteReportFormProvider implements XFormProvider {

   /**
    * Form component ids.
    */
   private static final String REPORT_IDS_FIELD = "ReportIds";

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      //comes from report_archive.jes
      List reportIds = (List) parameters.get("selectedIds");
      form.findComponent(REPORT_IDS_FIELD).setValue(reportIds);
   }
}
