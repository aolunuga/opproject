/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

import java.util.HashMap;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.module.OpTool;
import onepoint.project.module.OpToolManager;
import onepoint.service.server.XSession;

/**
 * Form provider class for the start form.
 *
 * @author horia.chiorean
 */
public class OpStartFormProvider implements XFormProvider {
   
   public final static String TOOL_PARAMETER = "tool";
   
   public final static String DOCK_PATH = "/forms/dock.oxf.xml";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      // If a dedicated "tool" parameter is not set provide the "my_tasks" tool as a default
      String activeToolName = (String) parameters.get(TOOL_PARAMETER);
      /*
      if (activeToolName == null) {
         activeToolName = "activity"; //OpMyTasksModule.TOOL_NAME;
         parameters.put(TOOL_PARAMETER, activeToolName);
      }
      */
      XComponent dockFrame = form.findComponent("DockFrame");
      if (activeToolName != null)
         dockFrame.setContent(getDockPath() + "?" + TOOL_PARAMETER + "=" + activeToolName);
      else
         dockFrame.setContent(getDockPath());
      OpTool activeTool = OpToolManager.getTool(activeToolName);
      if (activeTool != null) {
         // Set corresponding start form in main frame
         XComponent mainFrame = form.findComponent("MainFrame");
         // *** TODO: Add all parameters beside tool
         mainFrame.setContent(activeTool.getStartForm());
      }
   }
   
   protected String getDockPath() {
      return DOCK_PATH;
   }
}
