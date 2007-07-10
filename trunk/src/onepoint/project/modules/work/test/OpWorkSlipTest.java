/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.persistence.OpEntityException;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkError;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.test.OpTestCase;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * This class tests OpWorkSlip entity methods.
 *
 * @author florin.haizea
 */
public class OpWorkSlipTest extends OpTestCase {

   /**
    * Test happy-flow validation of an OpWorkSlip entity
    *
    * @throws Exception if the test fails
    */
   public void testValidOpWorkSlip()
        throws Exception {

      OpWorkRecord workRecord = new OpWorkRecord();
      workRecord.setActualEffort(13d);
      workRecord.setMaterialCosts(12d);
      workRecord.setTravelCosts(11d);
      workRecord.setExternalCosts(10d);
      workRecord.setMiscellaneousCosts(9d);
      Set<OpWorkRecord> workRecords = new HashSet<OpWorkRecord>();
      workRecords.add(workRecord);

      OpWorkSlip workSlip = new OpWorkSlip();
      workSlip.setCreator(new OpUser());
      workSlip.setDate(new Date(System.currentTimeMillis()));
      workSlip.setRecords(workRecords);
      workSlip.validate();
   }

   /**
    * Test validation of OpWorkSlip with invalid fields
    *
    * @throws Exception if the test fails
    */
   public void testInvalidOpWorkRecord()
        throws Exception {

      OpWorkSlip workSlip = new OpWorkSlip();

      boolean exceptionThrown = false;
      try {
         workSlip.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpWorkSlip.validate() has thrown a wrong error code", OpWorkError.CREATOR_MISSING, e.getErrorCode());
      }
      assertTrue("OpWorkSlip.validate() failed, exception should have been thrown", exceptionThrown);

      workSlip.setCreator(new OpUser());
      exceptionThrown = false;
      try {
         workSlip.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpWorkSlip.validate() has thrown a wrong error code", OpWorkError.DATE_MISSING, e.getErrorCode());
      }
      assertTrue("OpWorkSlip.validate() failed, exception should have been thrown", exceptionThrown);

      workSlip.setDate(new Date(System.currentTimeMillis()));
      exceptionThrown = false;
      try {
         workSlip.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpWorkSlip.validate() has thrown a wrong error code", OpWorkError.WORK_RECORDS_MISSING, e.getErrorCode());
      }
      assertTrue("OpWorkSlip.validate() failed, exception should have been thrown", exceptionThrown);

      OpWorkRecord workRecord = new OpWorkRecord();
      workRecord.setActualEffort(13d);
      workRecord.setMaterialCosts(-3d);
      Set<OpWorkRecord> workRecords = new HashSet<OpWorkRecord>();
      workRecords.add(workRecord);

      workSlip.setRecords(workRecords);
      exceptionThrown = false;
      try {
         workSlip.validate();
      }
      catch (OpEntityException e) {
         exceptionThrown = true;
         assertEquals("OpWorkSlip.validate() has thrown a wrong error code", OpWorkError.INCORRECT_MATERIAL_COSTS, e.getErrorCode());
      }
      assertTrue("OpWorkSlip.validate() failed, exception should have been thrown", exceptionThrown);
   }

}
