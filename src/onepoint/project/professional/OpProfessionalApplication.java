/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.professional;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.application.OpBasicApplication;
import onepoint.project.module.OpModule;
import onepoint.project.team.modules.license.OpLicenseModule;
import onepoint.project.team.modules.license.OpLicenseService;
import onepoint.project.team.modules.license.OpLicenseException;
import onepoint.project.team.modules.project_planning.components.OpChartComponentProxy;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;

import java.io.FileNotFoundException;


/**
 * @author mihai.costin
 */
public class OpProfessionalApplication extends OpBasicApplication {

   private final static XLog logger = XLogFactory.getLogger(OpProfessionalApplication.class);

   protected OpProfessionalApplication() {
      super("Onepoint Project Professional Edition");
   }

   protected final void start(String[] arguments) {
      //the home should've been initialized by the super class
      String home = OpEnvironmentManager.getOnePointHome();
      
      try {
         OpLicenseModule.checkLicense(home, getProductCode());
      }
      catch (FileNotFoundException e) {
         logger.fatal("License file could not be found");
         System.exit(1);
      }
      catch(OpLicenseException e) {
         logger.fatal(e.getMessage());
         System.exit(1);
      }

      XComponent.registerProxy(new OpChartComponentProxy());
      super.start(arguments);
   }

   protected void additionalInitialization() {
      OpModule module = OpLicenseModule.loadModule();
      OpLicenseService licenseService = (OpLicenseService) module.getServices().next();
      //check the license expiration date
      if (licenseService.licenseExpired()) {
         logger.error("License has expired");
      }
   }

   public static void main(String[] args) {
      OpProfessionalApplication application = new OpProfessionalApplication();
      application.start(args);
   }


   /**
    * @see onepoint.project.application.OpBasicApplication#getProductCode()
    */
   protected String getProductCode() {
      return OpProjectConstants.PROFESSIONAL_EDITION_CODE;
   }
}
