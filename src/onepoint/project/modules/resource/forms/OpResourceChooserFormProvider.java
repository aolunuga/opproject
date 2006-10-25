/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpResourceChooserFormProvider implements XFormProvider {

   // Form parameters and component IDs
   public final static String CALLING_FRAME_ID = "CallingFrameID";
   public final static String RESOURCE_LOCATOR_FIELD_ID = "ResourceLocatorFieldID";
   public final static String RESOURCE_NAME_FIELD_ID = "ResourceNameFieldID";
   public final static String RESOURCE_SET = "ResourceSet";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Set calling frame and field IDs from parameters
      String callingFrameID = (String) parameters.get(CALLING_FRAME_ID);
      form.findComponent(CALLING_FRAME_ID).setStringValue(callingFrameID);
      String resourceLocatorFieldID = (String) parameters.get(RESOURCE_LOCATOR_FIELD_ID);
      form.findComponent(RESOURCE_LOCATOR_FIELD_ID).setStringValue(resourceLocatorFieldID);
      String resourceNameFieldID = (String) parameters.get(RESOURCE_NAME_FIELD_ID);
      form.findComponent(RESOURCE_NAME_FIELD_ID).setStringValue(resourceNameFieldID);

      // Put all resource names into project data-set (values are IDs)
      XComponent dataSet = form.findComponent(RESOURCE_SET);

      OpBroker broker = session.newBroker();
      OpResourceDataSetFactory.retrieveResourceDataSet(session, broker, dataSet);
      broker.close();
   }

}
