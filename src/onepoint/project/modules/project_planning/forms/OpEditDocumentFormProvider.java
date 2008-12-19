package onepoint.project.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for the edit document attachment dialog
 *
 * @author florin.haizea
 */
public class OpEditDocumentFormProvider implements XFormProvider {

   /**
    * The id of the field that will hold the max size of the attachments
    */
   private static final String MAX_ATTACHMENT_SIZE_ID = "MaxAttachmentSize";

   private static final String ATTACHMENT_NAME = "name";
   private static final String ATTACHMENT_LOCATION = "location";
   private static final String NAME_FIELD_ID = "Name";
   private static final String LOCATION_FIELD_ID = "Location";
   private static final String PATH_FIELD_ID = "DocumentPathField";
   private static final String FILE_SELECTED_ID = "FileSelected";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      OpInitializer initializer =  OpInitializerFactory.getInstance().getInitializer();
      form.findComponent(MAX_ATTACHMENT_SIZE_ID).setLongValue(initializer.getMaxAttachmentSizeBytes());

      String attachmentName = (String) parameters.get(ATTACHMENT_NAME);
      String attachmentLocation = (String) parameters.get(ATTACHMENT_LOCATION);

      XComponent nameField = form.findComponent(NAME_FIELD_ID);
      nameField.setStringValue(attachmentName);

      XComponent locationField = form.findComponent(LOCATION_FIELD_ID);
      locationField.setStringValue(attachmentLocation);

      XComponent pathField = form.findComponent(PATH_FIELD_ID);
      pathField.setStringValue(attachmentLocation);

      form.findComponent(FILE_SELECTED_ID).setBooleanValue(false);
   }
}