/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpPrototypeLoader;
import onepoint.project.util.OpProjectConstants;

public class OpPrototypeFile {

   private String _file_name;
   private OpPrototype _prototype;

   public final void setFileName(String file_name) {
      _file_name = file_name;
   }

   public final OpPrototype loadPrototype() {
      String prototype_file_name = OpProjectConstants.PROJECT_PACKAGE + _file_name;
      return new OpPrototypeLoader().loadPrototype(prototype_file_name);
   }

}