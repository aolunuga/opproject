package onepoint.project.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for the edit URL attachment dialog
 *
 * @author florin.haizea
 */
public class OpEditURLFormProvider implements XFormProvider {

   private static final String URL_NAME = "name";
   private static final String URL_LOCATION = "location";
   private static final String NAME_FIELD_ID = "Name";
   private static final String LOCATION_FIELD_ID = "Location";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      String attachmentName = (String) parameters.get(URL_NAME);
      String attachmentLocation = (String) parameters.get(URL_LOCATION);

      XComponent nameField = form.findComponent(NAME_FIELD_ID);
      nameField.setStringValue(attachmentName);

      XComponent locationField = form.findComponent(LOCATION_FIELD_ID);
      locationField.setStringValue(attachmentLocation);
   }
}