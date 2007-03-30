/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.persistence.OpObject;

public class OpSetting extends OpObject {

   public final static String SETTING = "OpSetting";

   public final static String NAME = "Name";
   public final static String VALUE = "Value";

   private String name;
   private String value;

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public String getValue() {
      return value;
   }
}