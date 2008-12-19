/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for the add_document.oxf.xml form used for adding attachments.
 *
 * @author horia.chiorean
 */
public class OpAddDocumentFormProvider implements XFormProvider {

   /**
    * The id of the field that will hold the max size of the attachments
    */
   private static final String MAX_ATTACHMENT_SIZE_ID = "MaxAttachmentSize";

   /**
    *  @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      OpInitializer initializer=  OpInitializerFactory.getInstance().getInitializer();
      form.findComponent(MAX_ATTACHMENT_SIZE_ID).setLongValue(initializer.getMaxAttachmentSizeBytes());
   }
}
