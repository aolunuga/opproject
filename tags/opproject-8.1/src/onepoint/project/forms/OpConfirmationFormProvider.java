/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.forms;

import java.util.HashMap;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

/**
 * Form provider for error dialog
 *
 * @author mihai.costin
 */
public class OpConfirmationFormProvider implements XFormProvider {

   private static final String LABEL = "Message";
   private static final String RESOURCE_MAP_ID = "resourceMap";
   private static final String ERROR_TEXT_ID = "errorID";
   protected static final String TEXT_MESSAGE = "message";
   private static final String WARNING = "warning";
   private static final String CALLBACK_FRAME = "FrameName";
   private static final String CALLBACK_FRAME_PARAM = "frame";
   private static final String CALLBACK_NAME = "CallbackName";
   private static final String CALLBACK_NAME_PARAM = "callback";
   private static final String CALLBACK_PARAMETERS = "CallbackParameters";
   private static final String CALLBACK_PARAMETERS_PARAM = "parameters";
   private static final String ACTION = "ActionName";
   private static final String ACTION_PARAM = "action";
   private static final String OPTION = "option";
   private static final String TYPE = "type";
   private static final String BUTTONS_RESOURCE_MAP = "main.confirmation";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      XLanguageResourceMap resourceMap = session.getLocale().getResourceMap((String) (parameters.get(RESOURCE_MAP_ID)));
      //default fallback
//      String errorIdText = (String) (parameters.get(ERROR_TEXT_ID));
      String errorText = (String) (parameters.get(TEXT_MESSAGE));
//      boolean warning = (parameters.get(WARNING) != null) && ((Boolean) parameters.get(WARNING)).booleanValue();
      int option = XDisplay.OK_CANCEL_OPTION;
      Integer optionInt = (Integer) (parameters.get(OPTION));
      if (optionInt != null) {
         option = optionInt.intValue();
      }
      int pos = 1;
      pos = showOrHideButton(form, option, "OKButton", XDisplay.OK_OPTION, pos);
      pos = showOrHideButton(form, option, "YesButton", XDisplay.YES_OPTION, pos);
      pos = showOrHideButton(form, option, "NoButton", XDisplay.NO_OPTION, pos);
      pos = showOrHideButton(form, option, "CancelButton", XDisplay.CANCEL_OPTION, pos);
      form.findComponent("buttonPanel").setWidth(pos+1);
      
      // NOTE: type is for future use, to display an info, error, warning, question icon
      int type = XDisplay.INFORMATION_MESSAGE;
      Integer typeInt = (Integer) (parameters.get(TYPE));
      if (typeInt != null) {
         type = typeInt.byteValue();
      }
      
      //Set Message
      XComponent label = form.findComponent(LABEL);
//      if (warning) {
//         label.setStyleAttributes(XComponent.DEFAULT_LABEL_STYLE_ATTRIBUTES);
//         String warningText = session.getLocale().getResourceMap(resourceMap).getResource("Warning").getText();
//         form.setText(warningText);
//      }

      String text;
      if (errorText != null) {
         text = getResourceText(session, resourceMap, errorText);
         label.setText(text);
      }

      //set the callback and frame name
      XComponent callBackFrame = form.findComponent(CALLBACK_FRAME);
      callBackFrame.setStringValue((String) parameters.get(CALLBACK_FRAME_PARAM));

      XComponent callbackName = form.findComponent(CALLBACK_NAME);
      callbackName.setStringValue((String) parameters.get(CALLBACK_NAME_PARAM));

      XComponent callbackParam = form.findComponent(CALLBACK_PARAMETERS);
      callbackParam.setValue(parameters.get(CALLBACK_PARAMETERS_PARAM));

      XComponent actionParam = form.findComponent(ACTION);
      actionParam.setValue(parameters.get(ACTION_PARAM));

   }

   private int showOrHideButton(XComponent form, int option, String button,
         int buttonOption, int pos) {
      XComponent comp = form.findComponent(button);
      boolean visible = (option & buttonOption) != 0;
      comp.setVisible(visible);
      comp.setX(pos);
      if (visible) {
         pos++;
      }
      return pos;
   }

   private String getResourceText(OpProjectSession session, XLanguageResourceMap resourceMap, String key) {
      if (resourceMap != null) {
         XLocalizer localizer = XLocalizer.getLocalizer(resourceMap);
         String value = localizer.localize(key);
         if (value !=null) {
            return value;
         }
      }
      return key;
   }
}
