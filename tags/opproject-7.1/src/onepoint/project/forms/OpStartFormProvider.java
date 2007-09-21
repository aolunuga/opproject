/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.module.OpModuleManager;
import onepoint.project.modules.project.OpProjectModule;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider class for the start form.
 *
 * @author horia.chiorean
 */
public class OpStartFormProvider implements XFormProvider {

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      OpProjectModule projectModule = (OpProjectModule) OpModuleManager.getModuleRegistry().getModule(OpProjectModule.MODULE_NAME);
      if (projectModule != null) {
         XComponent mainFrame = form.findComponent("MainFrame");
         mainFrame.setContent(projectModule.getStartFormPath());
         mainFrame.doLayout();
         mainFrame.repaint();
      }
   }
}
