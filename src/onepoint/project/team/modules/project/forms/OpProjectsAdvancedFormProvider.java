/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.project.modules.project.forms.OpProjectsFormProvider;
import onepoint.project.modules.user.OpPermission;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider class for the advanced (closed) projects module
 *
 * @author horia.chiorean
 */
public class OpProjectsAdvancedFormProvider extends OpProjectsFormProvider {

   /**
    * Button ids.
    */
   private static final String NEW_TEMPLATE_BUTTON = "NewTemplateButton";
   private static final String SAVE_AS_TEMPLATE_BUTTON = "SaveAsTemplateButton";

   /**
    * @see onepoint.project.modules.project.forms.OpProjectsFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      super.prepareForm(s, form, parameters);
      byte rootPortfolioPermission = form.findComponent("RootPortfolioPermission").getByteValue();
      if (rootPortfolioPermission < OpPermission.MANAGER) {
         form.findComponent(NEW_TEMPLATE_BUTTON).setEnabled(false);
      }
   }


   /**
    * Disables buttons that require a selection in order to be enabled.
    *
    * @param form a <code>XComponent</code> representing the project form.
    */
   protected void disableSelectionButtons(XComponent form) {
      super.disableSelectionButtons(form);
      form.findComponent(SAVE_AS_TEMPLATE_BUTTON).setEnabled(false);
   }
}
