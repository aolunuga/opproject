/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.reports.delivery_forecast;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.util.HashMap;

public class OpDeliveryForecastFormProvider implements XFormProvider {

   private final static String USER_LOCATOR_FIELD = "UserLocatorField";
   private final static String USER_NAME_FIELD = "UserNameField";
   private final static String USER_NAME_LABEL = "UserLabel";
   private final static String SELECT_USER_BUTTON = "SelectUserButton";
   private final static String START_FIELD = "StartField";
   private final static String FINISH_FIELD = "FinishField";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      OpBroker broker = session.newBroker();
      OpUser user = session.user(broker);
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));
      String displayName = localizer.localize(user.getDisplayName());
      
      // Execute query and fill result set if RunQuery is true
      // set start date the first day of the current year
      form.findComponent(START_FIELD).setDateValue(XCalendar.getDefaultCalendar().getCurrentYearFirstDate());

      // set finish date the last day of the current year
      form.findComponent(FINISH_FIELD).setDateValue(XCalendar.getDefaultCalendar().getCurrentYearLastDate());

      form.findComponent(USER_LOCATOR_FIELD).setStringValue(XValidator.choice(user.locator(), displayName));

      if (OpInitializer.isMultiUser()) {
         form.findComponent(USER_NAME_FIELD).setStringValue(displayName);
         //enable select user button if session user is admin
         if (!session.userIsAdministrator()) {
            form.findComponent(SELECT_USER_BUTTON).setEnabled(false);
         }
      }
      else {
         form.findComponent(USER_NAME_FIELD).setVisible(false);
         form.findComponent(USER_NAME_LABEL).setVisible(false);
         form.findComponent(SELECT_USER_BUTTON).setVisible(false);
      }

      broker.close();

   }
}
