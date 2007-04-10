/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.license;

import onepoint.license.OpLicense;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.module.*;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpRSASecurity;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLanguageKit;
import onepoint.resource.XLocaleManager;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;


/**
 * Module class for the application license.
 *
 * @author : mihai.costin
 */
public class OpLicenseModule extends OpModule {

   /**
    * A public key used for signing the license.
    */
   private final static String PUBLIC_KEY;

   /**
    * This class's logger.
    */
   private final static XLog logger = XLogFactory.getLogger(OpLicenseModule.class);

   /**
    * The name of the license file.
    */
   private final static String LICENSE_FILE_NAME;

   static {
      LICENSE_FILE_NAME = "license.oxl.xml";
      // PUBLIC_KEY = "156140408475981700707138258989239719240218227499325703262251665529236540751839185603896969356599438172969317959657579330639706610858288870805071277339067867415198557570840819724279145551669363356208874792199270557843167493152491810926043935617947682755236167785355521037548371125901507556248605855069444697489#65537";
      PUBLIC_KEY = "89999919517580494847010192256905261407258706997277117258095451625805793080465932535349930573819560611210164515918554873274998843273920178645319911650664864964579938289129069924018687122426843624536871460113326633181593026322946186202282147009893459538914569684984915648383008688576982656082239123917118165057#65537";
   }

   /**
    * Loads and Checks the license from the license file.
    *
    * @param path Path to the license file
    * @param productCode a <code>String</code> representing the product code of the application invoking this method.
    * 
    * @throws FileNotFoundException if the license file os not found
    * @throws OpLicenseException if the license file is not valid.
    */
   public static void checkLicense(String path, String productCode)
        throws FileNotFoundException, OpLicenseException {

      //check for license
      OpLicense license = loadLicense(path);

      //test the signature of the license
      boolean signatureOk = OpRSASecurity.verify(PUBLIC_KEY, license.getContent(), license.getSignature());
      if (!signatureOk) {
         throw new OpLicenseException("The signature of the license file is invalid !");
      }

      String licenseProductCode = license.getProduct().getCode();
      String currentCode = productCode + OpProjectConstants.CODE_VERSION_NUMBER;
      if (!licenseProductCode.equals(currentCode)) {
         throw new OpLicenseException("The license product code does not match the current application's product code !");
      }
   }

   private static OpLicense loadLicense(String path)
        throws FileNotFoundException {
      OpLicense license;
      String licenseFile = path + "/" + LICENSE_FILE_NAME;

      InputStream inputLicense;
      inputLicense = new FileInputStream(licenseFile);

      OpLicenseLoader loader = new OpLicenseLoader();
      license = (OpLicense) loader.loadObject(inputLicense, null);
      return license;
   }

   public static OpModule loadModule() {
      OpModuleLoader moduleLoader = new OpModuleLoader();
      OpModule module = moduleLoader.loadModule(OpLicenseModule.class.getResourceAsStream("module.oxm.xml"));
      //this must be done first, so that any interceptor sees this
      try {
         OpLicense license = loadLicense(OpEnvironmentManager.getOnePointHome());
         logger.info(license);
         OpLicenseService licenseService = (OpLicenseService) module.getServices().next();
         licenseService.setLicense(license);
      }
      catch (FileNotFoundException e) {
         //<FIXME author="Horia Chiorean" description="Decide whether this should be terminal or not">
         logger.error("No license file could be found.");
         //<FIXME>
      }

      Iterator it = module.getServices();
      while (it.hasNext()) {
         XService service = (XService) it.next();
         XServiceManager.registerService(service);
      }

      it = module.getLanguageKits();
      while (it.hasNext()) {
         XLanguageKit kit = (XLanguageKit) it.next();
         XLocaleManager.registerLanguageKit(kit);
      }

      // Add tool-groups
      Iterator groups = module.getGroups();
      while (groups.hasNext()) {
         OpToolGroup group = (OpToolGroup) (groups.next());
         OpToolManager.registerGroup(group);
      }

      // Add tools
      Iterator tools = module.getTools();
      while (tools.hasNext()) {
         OpTool tool = (OpTool) (tools.next());
         // Resolve group-ref
         if (tool.getGroupRef() != null) {
            tool.setGroup(OpToolManager.getGroup(tool.getGroupRef()));
         }
         OpToolManager.registerTool(tool);
      }
      return module;
   }

}
