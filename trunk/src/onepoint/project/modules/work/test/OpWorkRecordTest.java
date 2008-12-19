/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.persistence.OpEntityException;
import onepoint.project.modules.work.OpTimeRecord;
import onepoint.project.modules.work.OpWorkError;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.test.OpTestCase;
import onepoint.project.util.OpProjectCalendar;

import java.util.HashSet;
import java.util.Set;

/**
 * This class tests OpWordRecord validate() method.
 *
 * @author florin.haizea
 */
public class OpWorkRecordTest extends OpTestCase {

   /**
    * Test happy-flow validation of an OpWordRecord entity
    *
    * @throws Exception if the test fails
    */
   public void testValidOpWorkRecord()
        throws Exception {

      OpWorkRecord workRecord = new OpWorkRecord();

      workRecord.setActualEffort(13d);
      workRecord.setMaterialCosts(12d);
      workRecord.setTravelCosts(11d);
      workRecord.setExternalCosts(10d);
      workRecord.setMiscellaneousCosts(9d);
      workRecord.validate();

      workRecord.setCompleted(true);
      workRecord.setActualEffort(0d);
      workRecord.setMaterialCosts(0d);
      workRecord.setTravelCosts(0d);
      workRecord.setExternalCosts(0d);
      workRecord.setMiscellaneousCosts(0d);
      workRecord.validate();

      Set<OpTimeRecord> timeRecords = new HashSet<OpTimeRecord>();
      OpTimeRecord timeRecord = new OpTimeRecord();

      //no time record is associated with the work record
      workRecord.addTimeRecords(timeRecords);
      workRecord.validate();

      //i)start - end: 22 - 29
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord, 22, 29, 7);
      timeRecords.add(timeRecord);
      //one time record is associated with the work record
      workRecord.validate();

      //ii)start - end: 0 - 22
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord, 0, 22, 22);
      timeRecords.add(timeRecord);
      //two time records are associated with the work record
      workRecord.validate();

      //iii)start - end: 67 - 90
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord, 67, 90, 23);
      timeRecords.add(timeRecord);
      //three time records are associated with the work record
      workRecord.validate();

      //iv)start - end: 90 - 24*60-1
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord, 90, OpProjectCalendar.MINUTES_PER_DAY - 1, OpProjectCalendar.MINUTES_PER_DAY - 91);
      timeRecords.add(timeRecord);
      //three time records are associated with the work record
      workRecord.validate();

      //v)start - end: 29 - 67
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord, 29, 67, 38);
      timeRecords.add(timeRecord);
      //four time records are associated with the work record
      workRecord.validate();
   }

    /**
    * Test validation of OpWorkRecord with invalid OpTimeRecords
    *
     * @throws Exception if the test fails
     */
    public void testInvalidOpWorkRecord()
         throws Exception {

       OpWorkRecord workRecord = new OpWorkRecord();

       workRecord.setActualEffort(-1d);
       workRecord.setMaterialCosts(-2d);
       workRecord.setTravelCosts(-3d);
       workRecord.setExternalCosts(-4d);
       workRecord.setMiscellaneousCosts(-5d);

       boolean exceptionThrown = false;
       try {
          workRecord.validate();
       }
       catch (OpEntityException e) {
          exceptionThrown = true;
          assertEquals("OpWorkRecord.validate() has thrown a wrong error code", OpWorkError.INCORRECT_ACTUAL_EFFORT, e.getErrorCode());
       }
       assertTrue("OpWorkRecord.validate() failed, exception should have been thrown", exceptionThrown);

       workRecord.setActualEffort(0d);
       workRecord.setCompleted(false);
       exceptionThrown = false;
       try {
          workRecord.validate();
       }
       catch (OpEntityException e) {
          exceptionThrown = true;
          assertEquals("OpWorkRecord.validate() has thrown a wrong error code", OpWorkError.INCORRECT_ACTUAL_EFFORT, e.getErrorCode());
       }
       assertTrue("OpWorkRecord.validate() failed, exception should have been thrown", exceptionThrown);

       workRecord.setActualEffort(12d);
       exceptionThrown = false;
       try {
          workRecord.validate();
       }
       catch (OpEntityException e) {
          exceptionThrown = true;
          assertEquals("OpWorkRecord.validate() has thrown a wrong error code", OpWorkError.INCORRECT_MATERIAL_COSTS, e.getErrorCode());
       }
       assertTrue("OpWorkRecord.validate() failed, exception should have been thrown", exceptionThrown);

       workRecord.setMaterialCosts(15d);
       exceptionThrown = false;
       try {
          workRecord.validate();
       }
       catch (OpEntityException e) {
          exceptionThrown = true;
          assertEquals("OpWorkRecord.validate() has thrown a wrong error code", OpWorkError.INCORRECT_TRAVEL_COSTS, e.getErrorCode());
       }
       assertTrue("OpWorkRecord.validate() failed, exception should have been thrown", exceptionThrown);

       workRecord.setTravelCosts(0d);
       exceptionThrown = false;
       try {
          workRecord.validate();
       }
       catch (OpEntityException e) {
          exceptionThrown = true;
          assertEquals("OpWorkRecord.validate() has thrown a wrong error code", OpWorkError.INCORRECT_EXTERNAL_COSTS, e.getErrorCode());
       }
       assertTrue("OpWorkRecord.validate() failed, exception should have been thrown", exceptionThrown);

       workRecord.setExternalCosts(10d);
       exceptionThrown = false;
       try {
          workRecord.validate();
       }
       catch (OpEntityException e) {
          exceptionThrown = true;
          assertEquals("OpWorkRecord.validate() has thrown a wrong error code", OpWorkError.INCORRECT_MISCELLANEOUS_COSTS, e.getErrorCode());
       }
       assertTrue("OpWorkRecord.validate() failed, exception should have been thrown", exceptionThrown);

       workRecord.setMiscellaneousCosts(0d);

       Set<OpTimeRecord> timeRecords = new HashSet<OpTimeRecord>();
       workRecord.addTimeRecords(timeRecords);
       OpTimeRecord timeRecord1 = new OpTimeRecord();
       OpTimeRecord timeRecord2 = new OpTimeRecord();

       //time record1: start - end: 5 - 20
       //time record2: start - end: 0 - 6
       exceptionThrown = false;
       OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord1, 5, 20, 15);
       OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord2, 0, 6, 6);
       timeRecords.add(timeRecord1);
       timeRecords.add(timeRecord2);
       try {
          workRecord.validate();
       }
       catch (OpEntityException e) {
          exceptionThrown = true;
          assertEquals("OpWorkRecord.validate() has thrown a wrong error code", OpWorkError.TIME_RECORDS_OVERLAP, e.getErrorCode());
       }
       assertTrue("OpWorkRecord.validate() failed, exception should have been thrown", exceptionThrown);

       //time record1: start - end: 5 - 20
       //time record2: start - end: 0 - 5
       //time record3: start - end: 5 - 20
       exceptionThrown = false;
       OpTimeRecord timeRecord3 = new OpTimeRecord();
       OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord2, 0, 5, 5);
       OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord3, 5, 20, 15);
       timeRecords.add(timeRecord3);
       try {
          workRecord.validate();
       }
       catch (OpEntityException e) {
          exceptionThrown = true;
          assertEquals("OpWorkRecord.validate() has thrown a wrong error code", OpWorkError.TIME_RECORDS_OVERLAP, e.getErrorCode());
       }
       assertTrue("OpWorkRecord.validate() failed, exception should have been thrown", exceptionThrown);

       //time record1: start - end: 5 - 20
       //time record2: start - end: 0 - 6
       //time record3: start - end: 56 - 60
       //time record4: start - end: 15 - 60
       exceptionThrown = false;
       OpTimeRecord timeRecord4 = new OpTimeRecord();
       OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord3, 56, 60, 4);
       OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord4, 15, 60, 45);
       timeRecords.add(timeRecord4);
       try {
          workRecord.validate();
       }
       catch (OpEntityException e) {
          exceptionThrown = true;
          assertEquals("OpWorkRecord.validate() has thrown a wrong error code", OpWorkError.TIME_RECORDS_OVERLAP, e.getErrorCode());
       }
       assertTrue("OpWorkRecord.validate() failed, exception should have been thrown", exceptionThrown);

       //time record1: start - end: 5 - 20
       //time record2: start - end: 0 - 6
       //time record3: start - end: 56 - 60
       //time record4: start - end: 90 - 100
       //time record5: start - end: 57 - 95
       exceptionThrown = false;
       OpTimeRecord timeRecord5 = new OpTimeRecord();
       OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord4, 90, 100, 10);
       OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord5, 57, 95, 38);
       timeRecords.add(timeRecord5);
       try {
          workRecord.validate();
       }
       catch (OpEntityException e) {
          exceptionThrown = true;
          assertEquals("OpWorkRecord.validate() has thrown a wrong error code", OpWorkError.TIME_RECORDS_OVERLAP, e.getErrorCode());
       }
       assertTrue("OpWorkRecord.validate() failed, exception should have been thrown", exceptionThrown);
    }
}
