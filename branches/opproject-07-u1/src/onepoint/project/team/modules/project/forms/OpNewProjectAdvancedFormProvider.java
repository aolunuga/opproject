/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.forms.OpNewProjectFormProvider;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Form provider for the new project dialog, advanced version.
 *
 * @author horia.chiorean
 */
public class OpNewProjectAdvancedFormProvider extends OpNewProjectFormProvider {

   /**
    * Form component ids.
    */
   private final static String TEMPLATE_SET = "TemplateSet";
   private final static String NOT_SELECTED = "NotSelected";
   /**
    * @see onepoint.project.modules.project.forms.OpNewProjectFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      super.prepareForm(s, form, parameters);
      //enable template field
      form.findComponent(TEMPLATE_FIELD).setEnabled(true);

      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      // Locate and fill template data set
      fillTemplateSet(session, broker, form);
      broker.close();
   }

   /**
    * Fills the template set that is used to select templates for the project.
    * @param session an <code>OpProjectSession</code> representing the server session.
    * @param broker an <code>OpBroker</code> used for performing business operations.
    * @param form a <code>XComponent</code> representing the form that will be populated.
    */
   protected void fillTemplateSet(OpProjectSession session, OpBroker broker, XComponent form) {
      XComponent dataSet = form.findComponent(TEMPLATE_SET);
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      String nullChoice = XValidator.choice("null", session.getLocale().getResourceMap(PROJECT_NEW_PROJECT).getResource(NOT_SELECTED).getText());
      dataRow.setStringValue(nullChoice);
      dataSet.addChild(dataRow);
      XComponent templateField = form.findComponent(TEMPLATE_FIELD);
      templateField.setStringValue(nullChoice);
      ((XComponent) templateField._getChild(0)).setStringValue(nullChoice);
      OpQuery query = broker.newQuery("select template.ID, template.Name from OpProjectNode as template where template.Type = ? order by template.Name");
      query.setByte(0, OpProjectNode.TEMPLATE);
      Iterator result = broker.list(query).iterator();
      Object[] record = null;
      long templateId = 0;
      String templateName = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         templateId = ((Long) record[0]).longValue();
         templateName = (String) record[1];
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(OpLocator.locatorString(OpProjectNode.PROJECT_NODE, templateId), templateName));
         dataSet.addChild(dataRow);
      }
   }
}
