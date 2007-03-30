/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.configuration_wizard;

import onepoint.project.module.OpModule;
import onepoint.project.module.OpModuleLoader;
import onepoint.resource.XLanguageKit;
import onepoint.resource.XLocaleManager;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceManager;

import java.util.Iterator;

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
      Iterator it = configModule.getServices();
      while (it.hasNext()) {
         XService service = (XService) it.next();
         XServiceManager.registerService(service);
      }

      it = configModule.getLanguageKits();
      while (it.hasNext()) {
         XLanguageKit kit = (XLanguageKit) it.next();
         XLocaleManager.registerLanguageKit(kit);
      }
   }
}
