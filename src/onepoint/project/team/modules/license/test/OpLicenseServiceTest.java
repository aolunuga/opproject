/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.team.modules.license.test;

import onepoint.project.team.modules.license.OpLicenseModule;
import onepoint.project.team.modules.license.OpLicenseService;
import onepoint.project.team.test.OpBaseTeamTestCase;
import onepoint.service.XMessage;

/**
 * This class test license service methods.
 *
 * @author calin.pavel
 */
public class OpLicenseServiceTest extends OpBaseTeamTestCase {
   private OpLicenseService licenseService;

   static {
      OpLicenseModule.loadModule();
   }

   /**
    * Here we prepare data for these tests.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      licenseService = getLicenseService();
   }

   /**
    * Clean data after test execution.
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      super.tearDown();

      licenseService = null;
   }

   /**
    * Test license cheking.
    *
    * @throws Exception If something goes wrong.
    */
   public void testCheckLicense()
        throws Exception {
      XMessage response = licenseService.checkLicense(session, new XMessage());
      assertNoError(response);
   }

   /**
    * Test license cheking.
    *
    * @throws Exception If something goes wrong.
    */
   public void testLicenseExpired()
        throws Exception {
      boolean licenseExpired = licenseService.licenseExpired();
      assertFalse(licenseExpired);
   }
}
