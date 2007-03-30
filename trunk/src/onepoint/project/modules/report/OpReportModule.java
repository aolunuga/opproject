/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report;

import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;

public class OpReportModule extends OpModule {

  public void install(OpProjectSession session) {}

  public void remove(OpProjectSession session) {}


   public void start(OpProjectSession session) {
      super.setup(session);
      OpReportService.removeReportFiles();
   }

   public void stop(OpProjectSession session) {
      super.stop(session);
      OpReportService.removeReportFiles();
   }
}
