/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.schedule;

import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;

public class OpScheduleModule extends OpModule {
	
	public void install(OpProjectSession session) {
		// *** Create prototypes (check for upgrade later on)
		// *** Add tools

		// *** Although advantages if specified in module.oxr
		// ==> Dependencies could be checked, pre-configured delivery of application is easier
		
		// *** Best: We leave the choice to the programmer
		// ==> Prototype and tool handling can be configured in module.oxm; implemented by base class OpModule
		
		// *** The only issue: We have to provide an intelligent "upgrade"-check (existing data)
		// ==> OpModule should provide a call-back upgrade()
	}

	public void remove(OpProjectSession session) {
		// *** Drop prototypes (what about potential data-loss)?
		// *** remove tools
	}
	
	public void start(OpProjectSession session) {
		OpScheduler.start();
	}
	
	public void stop() {
		OpScheduler.start();
	}

}

