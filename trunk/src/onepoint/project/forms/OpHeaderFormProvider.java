/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpHeaderFormProvider implements XFormProvider {

   private final static String USER_DISPLAY_NAME = "UserDisplayName";
   private final static String SIGNOFF_BUTTON_ID = "signOff_button";
   private final static String QUIT_BUTTON_ID = "quit_button";
   private final static String BANDWIDTH_INFO = "BandwidthInfo";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      OpUser user = session.user(broker);
      // Localizer is used to localize administrator user display name
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));
      if (OpEnvironmentManager.isMultiUser()) {
         form.findComponent(USER_DISPLAY_NAME).setText(localizer.localize(user.getDisplayName()));
         form.findComponent(BANDWIDTH_INFO).setVisible(true);
      }
      else {
         form.findComponent(USER_DISPLAY_NAME).setVisible(false);
         form.findComponent(BANDWIDTH_INFO).setVisible(false);
      }
      form.findComponent(SIGNOFF_BUTTON_ID).setVisible(OpEnvironmentManager.isMultiUser());
      form.findComponent(QUIT_BUTTON_ID).setVisible(!OpEnvironmentManager.isMultiUser());
      broker.close();
   }

}
