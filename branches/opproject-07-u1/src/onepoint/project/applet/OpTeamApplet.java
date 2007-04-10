/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.applet;

import onepoint.express.XComponent;
import onepoint.project.team.modules.project_planning.components.OpChartComponentProxy;

/**
 * @author mihai.costin
 */
public class OpTeamApplet extends OpOpenApplet{

   static {
      XComponent.registerProxy(new OpChartComponentProxy());
   }

}
