/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.team.test;


import onepoint.project.team.modules.activity_category.OpActivityCategoryService;
import onepoint.project.team.modules.license.OpLicenseService;
import onepoint.project.team.modules.project.OpProjectAdministrationAdvancedService;
import onepoint.project.team.modules.report_archive.OpReportArchiveService;
import onepoint.project.test.OpBaseTestCase;
import onepoint.service.server.XServiceManager;

/**
 * This is the super class for all TEAM test classes used into OnePoint project.
 *
 * @author calin.pavel, lucian.furtos
 */
public class OpBaseTeamTestCase extends OpBaseTestCase {

   // Name of the license service.
   private static final String LICENSE_SERVICE_NAME = "LicenseService";

   // Name of the project service.
   private static final String PROJECT_SERVICE_NAME = "ProjectService";

   // Name of the activity category service.
   private static final String ACTIVITY_CATEGORY_SERVICE_NAME = "ActivityCategoryService";

   // Name of the report archive service.
   private static final String REPORT_ARCHIVE_SERVICE_NAME = "ReportArchiveService";

   /**
    * Creates a new instance of test case.
    */
   public OpBaseTeamTestCase() {
      super();
   }

   /**
    * Creates a new instance of test case.
    *
    * @param name test case name
    */
   public OpBaseTeamTestCase(String name) {
      super(name);
   }

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      super.tearDown();
   }

   // ----- Helper Methods ------

   /**
    * Return the Project Advanced Service instance
    *
    * @return an instance of <code>OpProjectAdministrationAdvancedService</code>
    */
   protected static OpProjectAdministrationAdvancedService getProjectAdvancedService() {
      return (OpProjectAdministrationAdvancedService) XServiceManager.getService(PROJECT_SERVICE_NAME);
   }

   /**
    * Return the Activity Category Service instance
    *
    * @return an instance of <code>OpProjectAdministrationService</code>
    */
   protected static OpActivityCategoryService getActivityCategoryService() {
      return (OpActivityCategoryService) XServiceManager.getService(ACTIVITY_CATEGORY_SERVICE_NAME);
   }

   /**
    * Return the Repository Archive Service instance
    *
    * @return an instance of <code>OpReportArchiveService</code>
    */
   protected static OpReportArchiveService getReportArchiveService() {
      return (OpReportArchiveService) XServiceManager.getService(REPORT_ARCHIVE_SERVICE_NAME);
   }

   /**
    * Return the Team License Service instance
    *
    * @return an instance of <code>OpLicenseService</code>
    */
   protected static OpLicenseService getLicenseService() {
      return (OpLicenseService) XServiceManager.getService(LICENSE_SERVICE_NAME);
   }
}
