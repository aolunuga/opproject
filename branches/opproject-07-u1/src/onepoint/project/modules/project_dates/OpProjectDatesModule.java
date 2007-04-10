/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_dates;

import onepoint.project.module.OpModule;


/**
 * Module class for the time-controlling (project dates feature).
 */
public class OpProjectDatesModule extends OpModule {

   public static final String MODULE_NAME = "project_dates";
   
   /**
    * Indicates whether the milestone controlling interval setting should be enabled or not.
    * @return <code>true</code> if milestone controlling interval setting should be enabled.
    */
   public boolean enableMilestoneControllingIntervalSetting() {
      return false;
   }
}
