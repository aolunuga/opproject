/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import onepoint.persistence.OpObject;

/**
 * @author dfreis
 *
 */
public class OpCustomTextValue extends OpObject  implements OpCustomValue {
   private String memo;

   /**
    * 
    */
   public OpCustomTextValue() {
   }
   /**
    * @param memo2
    */
   public OpCustomTextValue(String memo) {
      this.memo = memo;
   }

   public String getMemo() {
      return memo;
   }

   public void setMemo(String memo) {
      this.memo = memo;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#clone()
    */
   @Override
   public Object clone() {
      return new OpCustomTextValue(getMemo());
   }
}
