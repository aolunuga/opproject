/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project;

import onepoint.project.modules.project.OpProjectModule;

/**
 * Advanced module for the project administration.
 *
 * @author horia.chiorean
 */
public class OpAdvancedProjectModule extends OpProjectModule {

   /**
    * Returns the name of the start form for this module.
    *
    * @return a <code>String</code> representing the path to the start form.
    */
   public String getStartFormPath() {
      return "/team/modules/project/forms/projects.oxf.xml"; 
   }
}
