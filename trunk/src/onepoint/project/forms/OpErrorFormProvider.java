/*
* Copyright(c) OnePoint Software GmbH 2005. All Rights Reserved.
*/
package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for error dialog
 *
 * @author mihai.costin
 */
public class OpErrorFormProvider implements XFormProvider {

   private static final String LABEL = "Message";
   private static final String RESOURCE_MAP_ID = "errorMap";
   private static final String ERROR_TEXT_ID = "errorID";
   private static final String TEXT_MESSAGE = "errorMessage";
   private static final String WARNING = "warning";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      String resourceMap = (String) (parameters.get(RESOURCE_MAP_ID));
      //default fallback
      if (resourceMap == null) {
         resourceMap = "main.error";
      }
      String errorIdText = (String) (parameters.get(ERROR_TEXT_ID));
      String errorText = (String) (parameters.get(TEXT_MESSAGE));
      boolean warning = (parameters.get(WARNING) != null) ? ((Boolean) parameters.get(WARNING)).booleanValue() : false;

      //Set Message
      XComponent label = form.findComponent(LABEL);
      if (warning) {
         label.setStyleAttributes(XComponent.DEFAULT_LABEL_STYLE_ATTRIBUTES);
         String warningText = session.getLocale().getResourceMap(resourceMap).getResource("Warning").getText();
         form.setText(warningText);
      }

      String text;
      if (errorIdText != null){
         text = session.getLocale().getResourceMap(resourceMap).getResource(errorIdText).getText();
         label.setText(text);
      }
      else if (errorText != null){
         label.setText(errorText);
      }
      else {
         //<FIXME author="Mihai Costin" description="should be from resource map. Aslo the form title could be a parameter"> 
         //no message or id was specified.
         label.setText("Error");
         //</FIXME>
      }
   }
}
