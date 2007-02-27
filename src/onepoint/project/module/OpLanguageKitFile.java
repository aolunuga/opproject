/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.resource.XLanguageKit;
import onepoint.resource.XLanguageKitLoader;

import java.io.InputStream;

public class OpLanguageKitFile {

   private String _file_name;

   public OpLanguageKitFile() {
   }

   public OpLanguageKitFile(String _file_name) {
      this._file_name = _file_name;
   }

   public final void setFileName(String file_name) {
      _file_name = file_name;
   }

   public final XLanguageKit loadLanguageKit() {
      return new XLanguageKitLoader().loadLanguageKit(_file_name);
   }

   public static final XLanguageKit loadLanguageKit(InputStream is) {
      return new XLanguageKitLoader().loadLanguageKit(is);
   }
}