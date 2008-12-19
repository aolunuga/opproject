/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.resource.XLanguageKit;
import onepoint.resource.XLanguageKitLoader;

import java.io.InputStream;

public class OpLanguageKitFile {

   private String fileName;

   public OpLanguageKitFile() {
   }

   public OpLanguageKitFile(String _file_name) {
      this.fileName = _file_name;
   }

   public final void setFileName(String file_name) {
      fileName = file_name;
   }

   public final XLanguageKit loadLanguageKit() {
      return new XLanguageKitLoader().loadLanguageKit(fileName);
   }

   public static final XLanguageKit loadLanguageKit(InputStream is) {
      return new XLanguageKitLoader().loadLanguageKit(is);
   }
}