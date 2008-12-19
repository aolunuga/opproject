/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.configuration_wizard;

import onepoint.project.module.OpModule;
import onepoint.project.module.OpModuleLoader;

/**
 * Manager class that is responsible for loading the configuration wizard module.
 *
 * @author horia.chiorean
 */
public final class OpConfigurationWizardManager {

   /**
    * This is a manager class.
    */
   private OpConfigurationWizardManager() {
   }

   /**
    * Loads the configuration wizard module.
    */
   public static void loadConfigurationWizardModule() {
      OpModuleLoader moduleLoader = new OpModuleLoader();
      OpModule configModule = moduleLoader.loadModule(OpConfigurationWizardManager.class.getResourceAsStream("module.oxm.xml"));
      configModule.register();
   }
}
