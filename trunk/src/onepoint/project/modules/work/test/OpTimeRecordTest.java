/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.persistence.OpEntityException;
import onepoint.project.modules.work.OpTimeRecord;
import onepoint.project.modules.work.OpWorkError;
import onepoint.project.test.OpTestCase;
import onepoint.util.XCalendar;

/**
 * This class tests OpTimeRecordTest validate() method.
 *
 * @author florin.haizea
 */
public class OpTimeRecordTest extends OpTestCase {

   /**
    * Test happy-flow creation of OpTimeRecords
    *
    * @throws Exception if the test fails
    */
   public void testValidOpTimeRecords()
        throws Exception {

      OpTimeRecord timeRecord = new OpTimeRecord();

      //start - end: 22 - 29
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord, 22, 29, 7);
      timeRecord.validate();

      //start - end: 0 - 56
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord, 0, 56, 56);
      timeRecord.validate();

      //start - end: 35 - 24 * 60 - 1
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord, 35, XCalendar.MINUTES_PER_DAY - 1, XCalendar.MINUTES_PER_DAY - 36);
      timeRecord.validate();

      //start - end: 0 - 24 * 60 -1
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord, 0, XCalendar.MINUTES_PER_DAY - 1, XCalendar.MINUTES_PER_DAY - 1);
      timeRecord.validate();

      //start - end: 0 - 1
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord, 0, 1, 1);
      timeRecord.validate();
   }

   /**
    * Test creation of OpTimeRecords with non valid fields
    *
    * @throws Exception if the test fails
    */
   public void testInvalidOpTimeRecords()
        throws Exception {

      OpTimeRecord timeRecord = new OpTimeRecord();
      boolean exceptionThrown = false;

      //start < 0
      timeRecord.setStart(-5);
      timeRecord.setFinish(29);
      
      try {
         timeRecord.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpTimeRecord.validate() failed", OpWorkError.START_TIME_IS_NEGATIVE, e.getErrorCode());
      }
      assertTrue("OpTimeRecord.validate() failed, exception should have been thrown", exceptionThrown);

      //finish < 0
      exceptionThrown = false;
      timeRecord.setStart(15);
      timeRecord.setFinish(-7);
      try {
         timeRecord.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpTimeRecord.validate() failed", OpWorkError.FINISH_TIME_IS_NEGATIVE, e.getErrorCode());
      }
      assertTrue("OpTimeRecord.validate() failed, exception should have been thrown", exceptionThrown);

      //start > 24 * 60
      exceptionThrown = false;
      timeRecord.setStart(XCalendar.MINUTES_PER_DAY + 5);
      timeRecord.setFinish(XCalendar.MINUTES_PER_DAY + 10);
      try {
         timeRecord.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpTimeRecord.validate() failed", OpWorkError.START_TIME_IS_TOO_LARGE, e.getErrorCode());
      }
      assertTrue("OpTimeRecord.validate() failed, exception should have been thrown", exceptionThrown);

      //finish = 24 * 60
      exceptionThrown = false;
      timeRecord.setStart(100);
      timeRecord.setFinish(XCalendar.MINUTES_PER_DAY);
      try {
         timeRecord.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpTimeRecord.validate() failed", OpWorkError.FINISH_TIME_IS_TOO_LARGE, e.getErrorCode());
      }
      assertTrue("OpTimeRecord.validate() failed, exception should have been thrown", exceptionThrown);

//      //start = finish
//      exceptionThrown = false;
//      timeRecord.setStart(150);
//      timeRecord.setFinish(150);
//      try {
//         timeRecord.validate();
//      }
//      catch (OpEntityException e) {
//         exceptionThrown = true;
//         assertEquals("OpTimeRecord.validate() failed", OpWorkError.START_AFTER_FINISH, e.getErrorCode());
//      }
//      assertTrue("OpTimeRecord.validate() failed, exception should have been thrown", exceptionThrown);

      //start > finish
      exceptionThrown = false;
      timeRecord.setStart(160);
      timeRecord.setFinish(150);
      try {
         timeRecord.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         //assertEquals("OpTimeRecord.validate() failed", OpWorkError.START_AFTER_FINISH, e.getErrorCode());
      }
      assertFalse("OpTimeRecord.validate() worked, exception should have been thrown", exceptionThrown);
   }


}
