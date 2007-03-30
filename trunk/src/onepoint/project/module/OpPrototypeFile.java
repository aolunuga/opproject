/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpPrototypeLoader;
import onepoint.project.util.OpProjectConstants;

public class OpPrototypeFile {

   private String fileName;
   private OpPrototype prototype;

   public final void setFileName(String file_name) {
      fileName = file_name;
   }

   public final OpPrototype loadPrototype() {
      String prototype_file_name = OpProjectConstants.PROJECT_PACKAGE + fileName;
      return new OpPrototypeLoader().loadPrototype(prototype_file_name);
   }

}