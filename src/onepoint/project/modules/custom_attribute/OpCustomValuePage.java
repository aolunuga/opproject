/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.Date;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAttachment;

/**
 * @author dfreis
 *
 */
public class OpCustomValuePage extends OpObject {

   private static final int BOOLEAN_VALUE_MASK = 0x1;
   private static final int BOOLEAN_SET_MASK = 0x10;
   private static final int BOOLEAN_TRUE = BOOLEAN_SET_MASK | BOOLEAN_VALUE_MASK;
   private static final int BOOLEAN_FALSE = BOOLEAN_SET_MASK;

   private int booleans = 0;
   private Long number0 = null;
   private Long number1 = null;
   private Long number2 = null;
   private Long number3 = null;
   private Long number4 = null;
   private Long number5 = null;
   private Long number6 = null;
   private Long number7 = null;
   private Long number8 = null;
   private Long number9 = null;
   
   private Double decimal0 = null;
   private Double decimal1 = null;
   private Double decimal2 = null;
   private Double decimal3 = null;
   private Double decimal4 = null;
   private Double decimal5 = null;
   private Double decimal6 = null;
   private Double decimal7 = null;
   private Double decimal8 = null;
   private Double decimal9 = null;

   private Long date0 = null;
   private Long date1 = null;
   private Long date2 = null;
   private Long date3 = null;
   private Long date4 = null;
   private Long date5 = null;
   private Long date6 = null;
   private Long date7 = null;
   private Long date8 = null;
   private Long date9 = null;

   private String text0 = null;
   private String text1 = null;
   private String text2 = null;
   private String text3 = null;
   private String text4 = null;
   private String text5 = null;
   private String text6 = null;
   private String text7 = null;
   private String text8 = null;
   private String text9 = null;

   private OpCustomTextValue memo0 = null;
   private OpCustomTextValue memo1 = null;
   private OpCustomTextValue memo2 = null;
   private OpCustomTextValue memo3 = null;
   private OpCustomTextValue memo4 = null;
   private OpCustomTextValue memo5 = null;
   private OpCustomTextValue memo6 = null;
   private OpCustomTextValue memo7 = null;
   private OpCustomTextValue memo8 = null;
   private OpCustomTextValue memo9 = null;

   private OpAttachment attachment0 = null;
   private OpAttachment attachment1 = null;
   private OpAttachment attachment2 = null;
   private OpAttachment attachment3 = null;
   private OpAttachment attachment4 = null;
   private OpAttachment attachment5 = null;
   private OpAttachment attachment6 = null;
   private OpAttachment attachment7 = null;
   private OpAttachment attachment8 = null;
   private OpAttachment attachment9 = null;

   private String choice0 = null;
   private String choice1 = null;
   private String choice2 = null;
   private String choice3 = null;
   private String choice4 = null;
   private String choice5 = null;
   private String choice6 = null;
   private String choice7 = null;
   private String choice8 = null;
   private String choice9 = null;

   @SuppressWarnings("unused")
   private OpCustomizable object;

   @SuppressWarnings("unused")
   private int sequence = 0;

   /**
    * 
    */
   public OpCustomValuePage() {
   }

//   /**
//    * 
//    */
//   public OpCustomValuePage(OpCustomizable obj, int sequence) {
//      this();
//      setObject(obj);
//      setSequence(sequence);
//   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getBoolean(int)
    */
   public Boolean getBoolean(int slot) {
      check(slot);
      int value = (booleans >>> (2*slot)) & 0x11;
      if ((value & BOOLEAN_SET_MASK) == BOOLEAN_SET_MASK) {
         return ((value & BOOLEAN_VALUE_MASK) == BOOLEAN_VALUE_MASK);
      }
      return null;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setBoolean(int, java.lang.Boolean)
    */
   public void setBoolean(int slot, Boolean value) {
      if (value == null) {
         unsetBoolean(slot);
         return;
      }
      if (value.booleanValue()) {
         setBooleanToTrue(slot);
         return;
      }
      setBooleanToFalse(slot);
   }
   
   public void setBooleanToTrue(int slot) {
      check(slot);
      booleans |= BOOLEAN_TRUE << (2*slot);
   }

   public void setBooleanToFalse(int slot) {
      check(slot);
      booleans |= BOOLEAN_FALSE << (2*slot); // set hi bit
      booleans &= ~BOOLEAN_VALUE_MASK; // unset lo bit
   }

   public void unsetBoolean(int slot) {
      check(slot);
      booleans &= ~(BOOLEAN_TRUE); // unset high and lo bit
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getNumber(int)
    */
   public Long getNumber(int slot) {
      check(slot);
      switch (slot) {
      case 0:
         return number0;
      case 1:
         return number1;
      case 2:
         return number2;
      case 3:
         return number3;
      case 4:
         return number4;
      case 5:
         return number5;
      case 6:
         return number6;
      case 7:
         return number7;
      case 8:
         return number8;
      case 9:
         return number9;
      }
      throw new ArrayIndexOutOfBoundsException("slot is > 9");
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setNumber(int, java.lang.Long)
    */
   public void setNumber(int slot, Long value) {
      check(slot);
      switch (slot) {
      case 0:
         number0 = value;
         break;
      case 1:
         number1 = value;
         break;
      case 2:
         number2 = value;
         break;
      case 3:
         number3 = value;
         break;
      case 4:
         number4 = value;
         break;
      case 5:
         number5 = value;
         break;
      case 6:
         number6 = value;
         break;
      case 7:
         number7 = value;
         break;
      case 8:
         number8 = value;
         break;
      case 9:
         number9 = value;
         break;
      default:
         throw new ArrayIndexOutOfBoundsException("slot is > 9");
      }
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getDecimal(int)
    */
   public Double getDecimal(int slot) {
      check(slot);
      switch (slot) {
      case 0:
         return decimal0;
      case 1:
         return decimal1;
      case 2:
         return decimal2;
      case 3:
         return decimal3;
      case 4:
         return decimal4;
      case 5:
         return decimal5;
      case 6:
         return decimal6;
      case 7:
         return decimal7;
      case 8:
         return decimal8;
      case 9:
         return decimal9;
      }
      throw new ArrayIndexOutOfBoundsException("slot is > 9");
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setDecimal(int, java.lang.Double)
    */
   public void setDecimal(int slot, Double value) {
      check(slot);

      switch (slot) {
      case 0:
         decimal0 = value;
         break;
      case 1:
         decimal1 = value;
         break;
      case 2:
         decimal2 = value;
         break;
      case 3:
         decimal3 = value;
         break;
      case 4:
         decimal4 = value;
         break;
      case 5:
         decimal5 = value;
         break;
      case 6:
         decimal6 = value;
         break;
      case 7:
         decimal7 = value;
         break;
      case 8:
         decimal8 = value;
         break;
      case 9:
         decimal9 = value;
         break;
      default:
         throw new ArrayIndexOutOfBoundsException("slot is > 9");
      }
   }

   public Date getDate(int slot) {
      check(slot);
      
      switch (slot) {
      case 0:
         return date0 == null ? null : new Date(date0);
      case 1:
         return date1 == null ? null : new Date(date1);
      case 2:
         return date2 == null ? null : new Date(date2);
      case 3:
         return date3 == null ? null : new Date(date3);
      case 4:
         return date4 == null ? null : new Date(date4);
      case 5:
         return date5 == null ? null : new Date(date5);
      case 6:
         return date6 == null ? null : new Date(date6);
      case 7:
         return date7 == null ? null : new Date(date7);
      case 8:
         return date8 == null ? null : new Date(date8);
      case 9:
         return date9 == null ? null : new Date(date9);
      }
      throw new ArrayIndexOutOfBoundsException("slot is > 9");
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setDate(int, java.util.Date)
    */
   public void setDate(int slot, Date value) {
      check(slot);

      switch (slot) {
      case 0:
         date0 = value == null ? null : value.getTime();
         break;
      case 1:
         date1 = value == null ? null : value.getTime();
         break;
      case 2:
         date2 = value == null ? null : value.getTime();
         break;
      case 3:
         date3 = value == null ? null : value.getTime();
         break;
      case 4:
         date4 = value == null ? null : value.getTime();
         break;
      case 5:
         date5 = value == null ? null : value.getTime();
         break;
      case 6:
         date6 = value == null ? null : value.getTime();
         break;
      case 7:
         date7 = value == null ? null : value.getTime();
         break;
      case 8:
         date8 = value == null ? null : value.getTime();
         break;
      case 9:
         date9 = value == null ? null : value.getTime();
         break;
      default:
         throw new ArrayIndexOutOfBoundsException("slot is > 9");
      }
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getText(int)
    */
   public String getText(int slot) {
      check(slot);

      switch (slot) {
      case 0:
         return text0;
      case 1:
         return text1;
      case 2:
         return text2;
      case 3:
         return text3;
      case 4:
         return text4;
      case 5:
         return text5;
      case 6:
         return text6;
      case 7:
         return text7;
      case 8:
         return text8;
      case 9:
         return text9;
      }
      throw new ArrayIndexOutOfBoundsException("slot is > 9");
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getText(int)
    */
   public OpCustomTextValue getMemo(int slot) {
      check(slot);

      switch (slot) {
      case 0:
         return memo0;
      case 1:
         return memo1;
      case 2:
         return memo2;
      case 3:
         return memo3;
      case 4:
         return memo4;
      case 5:
         return memo5;
      case 6:
         return memo6;
      case 7:
         return memo7;
      case 8:
         return memo8;
      case 9:
         return memo9;
      }
      throw new ArrayIndexOutOfBoundsException("slot is > 9");
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setText(int, java.lang.String)
    */
   public void setText(int slot, String value) {
      check(slot);

      switch (slot) {
      case 0:
         text0 = value;
         break;
      case 1:
         text1 = value;
         break;
      case 2:
         text2 = value;
         break;
      case 3:
         text3 = value;
         break;
      case 4:
         text4 = value;
         break;
      case 5:
         text5 = value;
         break;
      case 6:
         text6 = value;
         break;
      case 7:
         text7 = value;
         break;
      case 8:
         text8 = value;
         break;
      case 9:
         text9 = value;
         break;
      default:
         throw new ArrayIndexOutOfBoundsException("slot is > 9");
      }
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setText(int, java.lang.String)
    */
   public void setMemo(int slot, OpCustomTextValue value) {
      check(slot);

      switch (slot) {
      case 0:
         memo0 = value;
         break;
      case 1:
         memo1 = value;
         break;
      case 2:
         memo2 = value;
         break;
      case 3:
         memo3 = value;
         break;
      case 4:
         memo4 = value;
         break;
      case 5:
         memo5 = value;
         break;
      case 6:
         memo6 = value;
         break;
      case 7:
         memo7 = value;
         break;
      case 8:
         memo8 = value;
         break;
      case 9:
         memo9 = value;
         break;
      default:
         throw new ArrayIndexOutOfBoundsException("slot is > 9");
      }
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getDocument(int)
    */
   public OpAttachment getAttachment(int slot) {
      check(slot);

      switch (slot) {
      case 0:
         return attachment0;
      case 1:
         return attachment1;
      case 2:
         return attachment2;
      case 3:
         return attachment3;
      case 4:
         return attachment4;
      case 5:
         return attachment5;
      case 6:
         return attachment6;
      case 7:
         return attachment7;
      case 8:
         return attachment8;
      case 9:
         return attachment9;
      }
      throw new ArrayIndexOutOfBoundsException("slot is > 9");
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setDocument(int, onepoint.project.modules.attachments.OpAttachment)
    */
   public void setAttachment(int slot, OpAttachment value) {
      check(slot);

      switch (slot) {
      case 0:
         attachment0 = value;
         break;
      case 1:
         attachment1 = value;
         break;
      case 2:
         attachment2 = value;
         break;
      case 3:
         attachment3 = value;
         break;
      case 4:
         attachment4 = value;
         break;
      case 5:
         attachment5 = value;
         break;
      case 6:
         attachment6 = value;
         break;
      case 7:
         attachment7 = value;
         break;
      case 8:
         attachment8 = value;
         break;
      case 9:
         attachment9 = value;
         break;
      default:
         throw new ArrayIndexOutOfBoundsException("slot is > 9");
      }
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getDocument(int)
    */
   public String getChoice(int slot) {
      check(slot);

      switch (slot) {
      case 0:
         return choice0;
      case 1:
         return choice1;
      case 2:
         return choice2;
      case 3:
         return choice3;
      case 4:
         return choice4;
      case 5:
         return choice5;
      case 6:
         return choice6;
      case 7:
         return choice7;
      case 8:
         return choice8;
      case 9:
         return choice9;
      }
      throw new ArrayIndexOutOfBoundsException("slot is > 9");
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setDocument(int, onepoint.project.modules.attachments.OpChoice)
    */
   public void setChoice(int slot, String value) {
      check(slot);

      switch (slot) {
      case 0:
         choice0 = value;
         break;
      case 1:
         choice1 = value;
         break;
      case 2:
         choice2 = value;
         break;
      case 3:
         choice3 = value;
         break;
      case 4:
         choice4 = value;
         break;
      case 5:
         choice5 = value;
         break;
      case 6:
         choice6 = value;
         break;
      case 7:
         choice7 = value;
         break;
      case 8:
         choice8 = value;
         break;
      case 9:
         choice9 = value;
         break;
      default:
         throw new ArrayIndexOutOfBoundsException("slot is > 9");
      }
   }

   private void check(int slot) {
      if (slot < 0) {
         throw new IllegalArgumentException("slot must be >= 0");
      }
      if (slot > 9) {
         throw new IllegalArgumentException("slot must be <= 9");
      }
   }
   
   public void setBooleans(int value) {
      this.booleans = value;
   }
   
   public int getBooleans() {
      return booleans;
   }

   public void setNumber0(Long value) {
      this.number0 = value;
   }
   
   public Long getNumber0() {
      return number0;
   }

   public void setNumber1(Long value) {
      this.number1 = value;
   }
   
   public Long getNumber1() {
      return number1;
   }

   public void setNumber2(Long value) {
      this.number2 = value;
   }
   
   public Long getNumber2() {
      return number2;
   }

   public void setNumber3(Long value) {
      this.number3 = value;
   }
   
   public Long getNumber3() {
      return number3;
   }

   public void setNumber4(Long value) {
      this.number4 = value;
   }
   
   public Long getNumber4() {
      return number4;
   }

   public void setNumber5(Long value) {
      this.number5 = value;
   }
   
   public Long getNumber5() {
      return number5;
   }

   public void setNumber6(Long value) {
      this.number6 = value;
   }
   
   public Long getNumber6() {
      return number6;
   }

   public void setNumber7(Long value) {
      this.number7 = value;
   }
   
   public Long getNumber7() {
      return number7;
   }

   public void setNumber8(Long value) {
      this.number8 = value;
   }
   
   public Long getNumber8() {
      return number8;
   }

   public void setNumber9(Long value) {
      this.number9 = value;
   }
   
   public Long getNumber9() {
      return number9;
   }

   public void setDecimal0(Double value) {
      this.decimal0 = value;
   }
   
   public Double getDecimal0() {
      return decimal0;
   }

   public void setDecimal1(Double value) {
      this.decimal1 = value;
   }
   
   public Double getDecimal1() {
      return decimal1;
   }

   public void setDecimal2(Double value) {
      this.decimal2 = value;
   }
   
   public Double getDecimal2() {
      return decimal2;
   }

   public void setDecimal3(Double value) {
      this.decimal3 = value;
   }
   
   public Double getDecimal3() {
      return decimal3;
   }

   public void setDecimal4(Double value) {
      this.decimal4 = value;
   }
   
   public Double getDecimal4() {
      return decimal4;
   }

   public void setDecimal5(Double value) {
      this.decimal5 = value;
   }
   
   public Double getDecimal5() {
      return decimal5;
   }

   public void setDecimal6(Double value) {
      this.decimal6 = value;
   }
   
   public Double getDecimal6() {
      return decimal6;
   }

   public void setDecimal7(Double value) {
      this.decimal7 = value;
   }
   
   public Double getDecimal7() {
      return decimal7;
   }

   public void setDecimal8(Double value) {
      this.decimal8 = value;
   }
   
   public Double getDecimal8() {
      return decimal8;
   }

   public void setDecimal9(Double value) {
      this.decimal9 = value;
   }
   
   public Double getDecimal9() {
      return decimal9;
   }

   public Long getDate0() {
      return date0;
   }

   public void setDate0(Long value) {
      this.date0 = value;
   }
   
   public Long getDate1() {
      return date1;
   }

   public void setDate1(Long value) {
      this.date1 = value;
   }
   
   public Long getDate2() {
      return date2;
   }

   public void setDate2(Long value) {
      this.date2 = value;
   }
   
   public Long getDate3() {
      return date3;
   }

   public void setDate3(Long value) {
      this.date3 = value;
   }
   
   public Long getDate4() {
      return date4;
   }

   public void setDate4(Long value) {
      this.date4 = value;
   }
   
   public Long getDate5() {
      return date5;
   }

   public void setDate5(Long value) {
      this.date5 = value;
   }
   
   public Long getDate6() {
      return date6;
   }

   public void setDate6(Long value) {
      this.date6 = value;
   }
   
   public Long getDate7() {
      return date7;
   }

   public void setDate7(Long value) {
      this.date7 = value;
   }
   
   public Long getDate8() {
      return date8;
   }

   public void setDate8(Long value) {
      this.date8 = value;
   }
   
   public Long getDate9() {
      return date9;
   }

   public void setDate9(Long value) {
      this.date9 = value;
   }
   
   public String getText0() {
      return text0;
   }

   public void setText0(String value) {
      this.text0 = value;
   }
   
   public String getText1() {
      return text1;
   }

   public void setText1(String value) {
      this.text1 = value;
   }
   
   public String getText2() {
      return text2;
   }

   public void setText2(String value) {
      this.text2 = value;
   }
   
   public String getText3() {
      return text3;
   }

   public void setText3(String value) {
      this.text3 = value;
   }
   
   public String getText4() {
      return text4;
   }

   public void setText4(String value) {
      this.text4 = value;
   }
   
   public String getText5() {
      return text5;
   }

   public void setText5(String value) {
      this.text5 = value;
   }
   
   public String getText6() {
      return text6;
   }

   public void setText6(String value) {
      this.text6 = value;
   }
   
   public String getText7() {
      return text7;
   }

   public void setText7(String value) {
      this.text7 = value;
   }
   
   public String getText8() {
      return text8;
   }

   public void setText8(String value) {
      this.text8 = value;
   }
   
   public String getText9() {
      return text9;
   }

   public void setText9(String value) {
      this.text9 = value;
   }

   public OpCustomTextValue getMemo0() {
      return memo0;
   }

   public void setMemo0(OpCustomTextValue value) {
      this.memo0 = value;
   }
   
   public OpCustomTextValue getMemo1() {
      return memo1;
   }

   public void setMemo1(OpCustomTextValue value) {
      this.memo1 = value;
   }
   
   public OpCustomTextValue getMemo2() {
      return memo2;
   }

   public void setMemo2(OpCustomTextValue value) {
      this.memo2 = value;
   }
   
   public OpCustomTextValue getMemo3() {
      return memo3;
   }

   public void setMemo3(OpCustomTextValue value) {
      this.memo3 = value;
   }
   
   public OpCustomTextValue getMemo4() {
      return memo4;
   }

   public void setMemo4(OpCustomTextValue value) {
      this.memo4 = value;
   }
   
   public OpCustomTextValue getMemo5() {
      return memo5;
   }

   public void setMemo5(OpCustomTextValue value) {
      this.memo5 = value;
   }
   
   public OpCustomTextValue getMemo6() {
      return memo6;
   }

   public void setMemo6(OpCustomTextValue value) {
      this.memo6 = value;
   }
   
   public OpCustomTextValue getMemo7() {
      return memo7;
   }

   public void setMemo7(OpCustomTextValue value) {
      this.memo7 = value;
   }
   
   public OpCustomTextValue getMemo8() {
      return memo8;
   }

   public void setMemo8(OpCustomTextValue value) {
      this.memo8 = value;
   }
   
   public OpCustomTextValue getMemo9() {
      return memo9;
   }

   public void setMemo9(OpCustomTextValue value) {
      this.memo9 = value;
   }

   public OpCustomizable getObject() {
      return object;
   }

   public void setObject(OpCustomizable value) {
      this.object = value;
   }

   public int getSequence() {
      return sequence;
   }

   public void setSequence(int value) {
      this.sequence = value;
   }


   public OpAttachment getAttachment0() {
      return attachment0;
   }

   public void setAttachment0(OpAttachment attachment0) {
      this.attachment0 = attachment0;
   }

   public OpAttachment getAttachment1() {
      return attachment1;
   }

   public void setAttachment1(OpAttachment attachment1) {
      this.attachment1 = attachment1;
   }

   public OpAttachment getAttachment2() {
      return attachment2;
   }

   public void setAttachment2(OpAttachment attachment2) {
      this.attachment2 = attachment2;
   }

   public OpAttachment getAttachment3() {
      return attachment3;
   }

   public void setAttachment3(OpAttachment attachment3) {
      this.attachment3 = attachment3;
   }

   public OpAttachment getAttachment4() {
      return attachment4;
   }

   public void setAttachment4(OpAttachment attachment4) {
      this.attachment4 = attachment4;
   }

   public OpAttachment getAttachment5() {
      return attachment5;
   }

   public void setAttachment5(OpAttachment attachment5) {
      this.attachment5 = attachment5;
   }

   public OpAttachment getAttachment6() {
      return attachment6;
   }

   public void setAttachment6(OpAttachment attachment6) {
      this.attachment6 = attachment6;
   }

   public OpAttachment getAttachment7() {
      return attachment7;
   }

   public void setAttachment7(OpAttachment attachment7) {
      this.attachment7 = attachment7;
   }

   public OpAttachment getAttachment8() {
      return attachment8;
   }

   public void setAttachment8(OpAttachment attachment8) {
      this.attachment8 = attachment8;
   }

   public OpAttachment getAttachment9() {
      return attachment9;
   }

   public void setAttachment9(OpAttachment attachment9) {
      this.attachment9 = attachment9;
   }

   
   public String getChoice0() {
	   return choice0;
   }

   public void setChoice0(String choice0) {
	   this.choice0 = choice0;
   }

   public String getChoice1() {
	   return choice1;
   }

   public void setChoice1(String choice1) {
	   this.choice1 = choice1;
   }

   public String getChoice2() {
	   return choice2;
   }

   public void setChoice2(String choice2) {
	   this.choice2 = choice2;
   }

   public String getChoice3() {
	   return choice3;
   }

   public void setChoice3(String choice3) {
	   this.choice3 = choice3;
   }

   public String getChoice4() {
	   return choice4;
   }

   public void setChoice4(String choice4) {
	   this.choice4 = choice4;
   }

   public String getChoice5() {
	   return choice5;
   }

   public void setChoice5(String choice5) {
	   this.choice5 = choice5;
   }

   public String getChoice6() {
	   return choice6;
   }

   public void setChoice6(String choice6) {
	   this.choice6 = choice6;
   }

   public String getChoice7() {
	   return choice7;
   }

   public void setChoice7(String choice7) {
	   this.choice7 = choice7;
   }

   public String getChoice8() {
	   return choice8;
   }

   public void setChoice8(String choice8) {
	   this.choice8 = choice8;
   }

   public String getChoice9() {
	   return choice9;
   }

   public void setChoice9(String choice9) {
	   this.choice9 = choice9;
   }

/* (non-Javadoc)
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   public int compareTo(OpCustomValuePage o) {
      if (object.equals(o.object))
         return (sequence - o.sequence);
      return object.hashCode() - o.hashCode();
   }
}
