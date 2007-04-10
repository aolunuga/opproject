/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.report_archive.forms;

import onepoint.project.modules.report.forms.OpReportListFormProvider;
import onepoint.express.XComponent;

/**
 * Advanced form provider for the report list.
 *
 * @author horia.chiorean
 */
public class OpReportListAdvancedFormProvider extends OpReportListFormProvider {

   private static final String SAVE_REPORT_BUTTON = "SaveReportButton";


   /**
    * @see onepoint.project.modules.report.forms.OpReportListFormProvider#prepareQueryFormAbsent(onepoint.express.XComponent)
    */
   protected void prepareQueryFormAbsent(XComponent form) {
      super.prepareQueryFormAbsent(form);
      form.findComponent(SAVE_REPORT_BUTTON).setEnabled(false);
   }

   /**
    * @see onepoint.project.modules.report.forms.OpReportListFormProvider#prepareQueryFormPresent(onepoint.express.XComponent, String)   
    */
   protected void prepareQueryFormPresent(XComponent form, String queryFormName) {
      super.prepareQueryFormPresent(form, queryFormName);
      form.findComponent(SAVE_REPORT_BUTTON).setEnabled(true);
   }
}
