/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.repository.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpInitializer;
import onepoint.project.modules.user.OpUser;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider class for the success confirmation form.
 *
 * @author horia.chiorean
 */
public class OpSuccessConfirmationFormProvider implements XFormProvider {

   private static final String SUCCESS_CONFIRMATION_ARGUMENT = "SuccessConfirmation";
   private static final String SUCCESS_CONFIRMATION_LABEL_ID = "SuccessConfirmationMessage";
   private static final String MULTI_USER_FIELD_ID = "MultiUser";
   private static final String DEFAULT_USER_ID = "DefaultUserName";
   private static final String DEFAULT_PASSWORD_ID = "DefaultPassword";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)  
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      String confirmationText = (String) parameters.get(SUCCESS_CONFIRMATION_ARGUMENT);
      form.findComponent(SUCCESS_CONFIRMATION_LABEL_ID).setText(confirmationText);
      form.findComponent(MULTI_USER_FIELD_ID).setBooleanValue(OpInitializer.isMultiUser());
      form.findComponent(DEFAULT_USER_ID).setStringValue(OpUser.ADMINISTRATOR_NAME);
      form.findComponent(DEFAULT_PASSWORD_ID).setStringValue("");
   }
}
