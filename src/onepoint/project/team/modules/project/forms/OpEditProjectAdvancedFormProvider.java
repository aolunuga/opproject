/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.forms.OpEditProjectFormProvider;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Advanced form provider for the edit project functionality.
 *
 * @author horia.chiorean
 */
public class OpEditProjectAdvancedFormProvider extends OpEditProjectFormProvider {

   /**
    * @see onepoint.project.team.modules.project.forms.OpEditProjectAdvancedFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      super.prepareForm(s, form, parameters);

      // Find project in database
      String id_string = (String) (parameters.get(OpProjectAdministrationService.PROJECT_ID));

      OpBroker broker = ((OpProjectSession) s).newBroker();
      OpProjectNode project = (OpProjectNode) (broker.getObject(id_string));

      // Set not-editable template field
      OpProjectNode templateNode = project.getTemplateNode();
      if (templateNode != null) {
         XComponent templateField = form.findComponent(TEMPLATE_FIELD);
         String templateLocator = XValidator.choice(templateNode.locator(), templateNode.getName());
         templateField.setStringValue(templateLocator);
         ((XComponent) templateField.getChild(0)).setStringValue(templateLocator);
      }
   }
}
