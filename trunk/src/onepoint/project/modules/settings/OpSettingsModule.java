/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;

public class OpSettingsModule extends OpModule {

   private static final XLog logger = XLogFactory.getServerLogger(OpSettingsModule.class);

   public void start(OpProjectSession session) {
      // Load settings
      OpSettingsService.getService().loadSettings(session);
   }
}
