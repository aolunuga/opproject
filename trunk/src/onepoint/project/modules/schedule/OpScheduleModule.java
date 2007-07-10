/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.schedule;

import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;

public class OpScheduleModule extends OpModule {
	
	@Override
  public void start(OpProjectSession session) {
		OpScheduler.start();
	}
	
	public void stop() {
		OpScheduler.start();
	}

}

