/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.persistence.OpObject;

public class OpSetting extends OpObject {

   public final static String SETTING = "OpSetting";

   public final static String NAME = "Name";
   public final static String VALUE = "Value";

   private String _name;
   private String _value;

   public void setName(String name) {
      _name = name;
   }

   public String getName() {
      return _name;
   }

   public void setValue(String value) {
      _value = value;
   }

   public String getValue() {
      return _value;
   }
}