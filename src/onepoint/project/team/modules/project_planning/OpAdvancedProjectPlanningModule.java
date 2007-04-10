/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_planning;

import onepoint.express.server.XFormLoader;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project_planning.OpProjectPlanningModule;

/**
 * Advanced Project Planning module. Registers the chart components with the handler.
 *
 * @author mihai.costin
 */
public class OpAdvancedProjectPlanningModule extends OpProjectPlanningModule {

   public void start(OpProjectSession session) {
      super.start(session);
      OpAdvancedProjectComponentHandler project_handler = new OpAdvancedProjectComponentHandler();
      XFormLoader.registerComponent(OpAdvancedProjectComponentHandler.WBS_BOX, project_handler);
      // Register chart components
      OpChartComponentHandler chart_handler = new OpChartComponentHandler();
      XFormLoader.registerComponent(OpChartComponentHandler.LINE_CHART_BOX, chart_handler);
      XFormLoader.registerComponent(OpChartComponentHandler.BAR_CHART_BOX, chart_handler);
      XFormLoader.registerComponent(OpChartComponentHandler.PIPELINE_CHART_BOX, chart_handler);
   }
}
