/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.persistence.OpEntityException;
import onepoint.project.modules.work.OpCostRecord;
import onepoint.project.test.OpTestCase;

/**
 * This class tests OpCostRecordTest validate() method.
 *
 * @author florin.haizea
 */
public class OpCostRecordTest extends OpTestCase {

   /**
    * Test happy-flow creation of OpCostRecords
    *
    * @throws Exception if the test fails
    */
   public void testValidOpCostRecords()
        throws Exception {

      OpCostRecord costRecord = new OpCostRecord();

      costRecord.setActualCosts(15d);
      costRecord.setRemainingCosts(20d);
      costRecord.setType(OpCostRecord.TRAVEL_COST);
      costRecord.validate();

      costRecord.setActualCosts(0d);
      costRecord.setRemainingCosts(0d);
      costRecord.setType(OpCostRecord.MISCELLANEOUS_COST);
      costRecord.validate();

      costRecord.setActualCosts(100d);
      costRecord.setRemainingCosts(10d);
      costRecord.setType(OpCostRecord.EXTERNAL_COST);
      costRecord.validate();

      costRecord.setActualCosts(10d);
      costRecord.setRemainingCosts(100d);
      costRecord.setType(OpCostRecord.MATERIAL_COST);
      costRecord.validate();
   }

   /**
    * Test creation of OpCostRecords with non valid fields
    *
    * @throws Exception if the test fails
    */
   public void testInvalidOpTimeRecords()
        throws Exception {

      OpCostRecord costRecord = new OpCostRecord();
      boolean exceptionThrown = false;

      //actual costs < 0
      costRecord.setActualCosts(-100d);
      costRecord.setRemainingCosts(10d);
      costRecord.setType(OpCostRecord.EXTERNAL_COST);
      try {
         costRecord.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpCostRecord.validate() failed", OpCostRecord.ACTUAL_COSTS_NOT_VALID, e.getErrorCode());
      }
      assertTrue("OpCostRecord.validate() failed, exception should have been thrown", exceptionThrown);

      //remaining costs < 0
      exceptionThrown = false;
      costRecord.setActualCosts(100d);
      costRecord.setRemainingCosts(-10d);
      costRecord.setType(OpCostRecord.MATERIAL_COST);
      try {
         costRecord.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpCostRecord.validate() failed", OpCostRecord.REMAINING_COSTS_NOT_VALID, e.getErrorCode());
      }
      assertTrue("OpCostRecord.validate() failed, exception should have been thrown", exceptionThrown);

      //type not set
      exceptionThrown = false;
      costRecord.setActualCosts(100d);
      costRecord.setRemainingCosts(10d);
      costRecord.setType(null);
      try {
         costRecord.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpCostRecord.validate() failed", OpCostRecord.COST_TYPE_NOT_VALID, e.getErrorCode());
      }
      assertTrue("OpCostRecord.validate() failed, exception should have been thrown", exceptionThrown);

      //actual costs < 0, remaining costs < 0, type not set
      exceptionThrown = false;
      costRecord.setActualCosts(-90d);
      costRecord.setRemainingCosts(-10d);
      costRecord.setType(null);
      try {
         costRecord.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpCostRecord.validate() failed", OpCostRecord.ACTUAL_COSTS_NOT_VALID, e.getErrorCode());
      }
      assertTrue("OpCostRecord.validate() failed, exception should have been thrown", exceptionThrown);
   }
}
