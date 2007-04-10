/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */
package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for change resource confirm dialog.
 *
 * @author mihai.costin
 */
public class OpConfirmChangePoolFormProvider implements XFormProvider {

   private final static String POOL_ID = "pool_id";
   private final static String POOL_DATA = "pool_data";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      // Retrieve the pool
      String id= (String) (parameters.get(POOL_ID));
      form.findComponent(POOL_ID).setValue(id);

      HashMap data = (HashMap) (parameters.get(POOL_DATA));
      form.findComponent(POOL_DATA).setValue(data);
   }
}
