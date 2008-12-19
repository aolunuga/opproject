/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.setup;

import java.io.FileNotFoundException;

import onepoint.project.module.OpModule;
import onepoint.project.module.OpModuleLoader;


/**
 * Module class used within setup process.
 *
 * @author : dfreismuth
 */
public class OpSetupModule extends OpModule {

   /**
    * Flag indicating whether the module has been loaded or not.
    */
   private static boolean loaded = false;

   /**
    * Loads the license module.
    *
    * @throws java.io.FileNotFoundException If the license file wasn't found
    * @throws onepoint.license.OpInvalidLicenseException
    *                                       if the license is invalid
    */
   public static void loadModule()
        throws FileNotFoundException {

      if (!loaded) {
         OpModuleLoader moduleLoader = new OpModuleLoader();
         OpModule module = moduleLoader.loadModule(OpSetupModule.class.getResourceAsStream("module.oxm.xml"));
         module.register();
         loaded = true;
      }
   }
}
