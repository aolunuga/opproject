/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.repository.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpInitializer;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for the confirmation message of the reset function.
 *
 * @author horia.chiorean
 */
public class OpResetConfirmationFormProvider implements XFormProvider {

   /**
    * Form component ids
    */
   private static final String SINGLE_USER_MESSAGE_LABEL_ID = "SingleUserConfirmationMessage";
   private static final String MULTI_USER_MESSAGE_LABEL_ID = "MultiUserConfirmationMessage";
   private static final String ADMIN_PASSWORD_FIELD_ID = "AdminPasswordField";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      boolean multiUser = OpInitializer.isMultiUser();
      if (multiUser) {
         form.findComponent(MULTI_USER_MESSAGE_LABEL_ID).setVisible(true);
         form.findComponent(ADMIN_PASSWORD_FIELD_ID).setVisible(true);
      }
      else {
         form.findComponent(SINGLE_USER_MESSAGE_LABEL_ID).setVisible(true);
      }
   }
}
