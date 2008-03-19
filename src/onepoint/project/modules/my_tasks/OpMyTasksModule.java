/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks;

import onepoint.project.module.OpModule;

public class OpMyTasksModule extends OpModule {
	
	public final static String MODULE_NAME = "my_tasks";

	   /**
	    * Returns the name of the start form for this module.
	    *
	    * @return a <code>String</code> representing the path to the start form.
	    */
	   public String getStartFormPath() {
	      return "/modules/my_tasks/forms/my_tasks.oxf.xml";
	   }

}
