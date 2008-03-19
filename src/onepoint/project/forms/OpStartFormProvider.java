/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

import java.util.HashMap;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.module.OpModuleManager;
import onepoint.project.modules.my_tasks.OpMyTasksModule;
import onepoint.service.server.XSession;

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
      OpMyTasksModule myTasksModule = (OpMyTasksModule) OpModuleManager.getModuleRegistry().getModule(OpMyTasksModule.MODULE_NAME);
      if (myTasksModule != null) {
         XComponent mainFrame = form.findComponent("MainFrame");
         mainFrame.setContent(myTasksModule.getStartFormPath());
         mainFrame.doLayout();
         mainFrame.repaint();
      }
   }
}
