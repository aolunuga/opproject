/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.servlet;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.team.modules.license.OpLicenseException;
import onepoint.project.team.modules.license.OpLicenseModule;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;

import javax.servlet.ServletException;
import java.io.FileNotFoundException;

/**
 * Team edition servlet.
 *
 * @author mihai.costin
 */
public class OpTeamServlet extends OpOpenServlet {

   private static final XLog logger = XLogFactory.getLogger(OpTeamServlet.class, true);

   public void onInit()
        throws ServletException {
      //init the project home
      this.initProjectHome();
      try {
         OpLicenseModule.checkLicense(OpEnvironmentManager.getOnePointHome(), getProductCode());
      }
      catch (FileNotFoundException e) {
         String errorMessage = "Could not find license file";
         logger.fatal(errorMessage);
         throw new ServletException(errorMessage);
      }
      catch (OpLicenseException e) {
         logger.fatal(e.getMessage());
         throw new ServletException(e.getMessage());
      }

      super.onInit();

      OpLicenseModule.loadModule();
   }

   protected String getAppletClassName() {
     return "onepoint.project.applet.OpTeamApplet.class";
   }

   /**
    * @see OpOpenServlet#getProductCode()
    */
   protected String getProductCode() {
      return OpProjectConstants.TEAM_EDITION_CODE;
   }
}
