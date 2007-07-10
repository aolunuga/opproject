/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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
   protected static final String TEXT_MESSAGE = "errorMessage";
   private static final String WARNING = "warning";
   private static final String CALLBACK_FRAME = "FrameName";
   private static final String CALLBACK_FRAME_PARAM = "frameName";
   private static final String CALLBACK_NAME = "CallbackName";
   private static final String CALLBACK_NAME_PARAM = "callbackName";
   private static final String CALLBACK_PARAMETERS = "CallbackParameters";
   private static final String CALLBACK_PARAMETERS_PARAM = "parameters";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      String resourceMap = (String) (parameters.get(RESOURCE_MAP_ID));
      //default fallback
      if (resourceMap == null) {
         resourceMap = "main.error";
      }
      String errorIdText = (String) (parameters.get(ERROR_TEXT_ID));
      String errorText = (String) (parameters.get(TEXT_MESSAGE));
      boolean warning = (parameters.get(WARNING) != null) && ((Boolean) parameters.get(WARNING)).booleanValue();

      //Set Message
      XComponent label = form.findComponent(LABEL);
      if (warning) {
         label.setStyleAttributes(XComponent.DEFAULT_LABEL_STYLE_ATTRIBUTES);
         String warningText = session.getLocale().getResourceMap(resourceMap).getResource("Warning").getText();
         form.setText(warningText);
      }

      String text;
      if (errorIdText != null) {
         text = session.getLocale().getResourceMap(resourceMap).getResource(errorIdText).getText();
         label.setText(text);
      }
      else if (errorText != null) {
         label.setText(errorText);
      }
      else {
         //no message or id was specified.
         label.setText(form.getText());
      }

      //set the callback and frame name
      XComponent callBackFrame = form.findComponent(CALLBACK_FRAME);
      if (callBackFrame != null) {
         callBackFrame.setStringValue((String) parameters.get(CALLBACK_FRAME_PARAM));
      }

      XComponent callbackName = form.findComponent(CALLBACK_NAME);
      if (callbackName != null) {
         callbackName.setStringValue((String) parameters.get(CALLBACK_NAME_PARAM));
      }

      XComponent callbackParam = form.findComponent(CALLBACK_PARAMETERS);
      if (callbackParam != null) {
         callbackParam.setValue(parameters.get(CALLBACK_PARAMETERS_PARAM));
      }
   }
}
