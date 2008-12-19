/**
 * Copyright(c) Onepoint Software GmbH 2008. All Rights Reserved.
 */
package onepoint.project.forms;

import java.util.HashMap;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.service.server.XSession;

/**
 * Form provider for the document_chooser form.
 *
 * @author mihai.costin
 */
public class OpAttachmentChooserFormProvider implements XFormProvider {

   // Form parameters and component IDs
   private final static String CALLING_WINDOW = "CallingWindow";
   private final static String ACTION_HANDLER = "ActionHandler";
   private static final String MAX_ATTACHMENT_SIZE_ID = "MaxAttachmentSize";
   private static final String PARAMETERS = "Parameters";

   
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpInitializer initializer=  OpInitializerFactory.getInstance().getInitializer();
      form.findComponent(MAX_ATTACHMENT_SIZE_ID).setLongValue(initializer.getMaxAttachmentSizeBytes());
      String actionHandler = (String) parameters.remove(ACTION_HANDLER);
      form.findComponent(ACTION_HANDLER).setStringValue(actionHandler);
      form.findComponent(PARAMETERS).setValue(parameters);
   }
}