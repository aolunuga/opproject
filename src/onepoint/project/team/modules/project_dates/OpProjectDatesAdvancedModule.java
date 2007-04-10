/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_dates;

import onepoint.project.modules.project_dates.OpProjectDatesModule;

/**
 * Module class for the closed part of the project dates (time controlling) functionality.
 *
 * @author horia.chiorean
 */
public class OpProjectDatesAdvancedModule extends OpProjectDatesModule {

   public boolean enableMilestoneControllingIntervalSetting() {
      return true;
   }
}
