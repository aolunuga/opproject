package onepoint.project.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XLocalServer;
import onepoint.service.server.XServer;

import java.util.TimeZone;

/**
 * @author mihai.costin
 */
public class OpBaseTestCase extends OpTestCase {

   // This is the session that must be used into all test methods.
   protected OpProjectSession session;

   protected XServer server;

   protected String adminId;

   protected String rootId;
   public static final int DUMMY_ERROR_CODE = -49765;


   public OpBaseTestCase() {
   }


   public OpBaseTestCase(String name) {
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

      //all tests must use GMT dates (same as the application)
      TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

      this.server = new XLocalServer();
      this.session = new OpProjectSession();
      this.session.setServer(this.server);

      // Authenticate user to be sure that he has access.
      logIn(OpUser.ADMINISTRATOR_NAME, OpUser.BLANK_PASSWORD);

      OpBroker broker = session.newBroker();
      // administrator
      adminId = session.administrator(broker).locator();
      // root pool
      rootId = OpResourceService.findRootPool(broker).locator();
      broker.close();
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      super.tearDown();

      // Log-off user.
      logOut();

      this.session.close();
   }

   /**
    * Log-in Administrator user.
    */
   protected void logIn() {
      logIn(OpUser.ADMINISTRATOR_NAME, OpUser.BLANK_PASSWORD);
   }

   /**
    * Tries to login user with the provided data, but only after a log-out.
    *
    * @param userName user name to be used for log-in.
    * @param password user password.
    */
   protected void logIn(String userName, String password) {
      // Authenticate user to be sure that he has access.
      XMessage request = new XMessage();
      request.setArgument(OpUserService.LOGIN, userName);
      request.setArgument(OpUserService.PASSWORD, password);
      XMessage response = OpTestDataFactory.getUserService().signOn(session, request);
      assertNoError(response);
   }

   /**
    * Log-out current user.
    */
   protected void logOut() {
      XMessage response = OpTestDataFactory.getUserService().signOff(session, new XMessage());
      assertNoError(response);
   }

   /**
    * Asserts that a XMessage reply from a service method contains not error msgs.
    *
    * @param message message to process.
    */
   public static void assertNoError(XMessage message) {
      if (message != null) {
         XError error = message.getError();
         String errorMessage = "No error message should have been returned.";
         if (error != null) {
            errorMessage += "Received error: " + error.getName();
         }
         assertNull(errorMessage, error);
      }
      else {
         //message is null <=> no error (success)
      }
   }

   /**
    * Asserts that a XMessage reply from a service method contains error msgs.
    *
    * @param message message to process.
    */
   public static void assertError(XMessage message) {
      assertError(message, DUMMY_ERROR_CODE);
   }

   /**
    * Asserts that a XMessage reply from a service method contains error msgs.
    *
    * @param message   message to process.
    * @param errorCode expected error code.
    */
   public static void assertError(XMessage message, int errorCode) {
      assertNotNull("XMessage is null, expected to contain an error!", message);
      XError error = message.getError();
      assertNotNull("Error message should have been returned", error);
      int foundErrorCode = error.getCode();
      // do not check error code in case DUMMY code was used.
      assertTrue("Invalid error code. (got: "+foundErrorCode+", but expected: "+errorCode+")", errorCode == DUMMY_ERROR_CODE || errorCode == foundErrorCode);
      assertNotNull("Error should contain an error name.", error.getName());
      assertNotNull("Error should contain an error message.", error.getName());
   }


   /**
    * Deletes all the objects of the given prototype. 
    *
    * @param broker broker instance
    * @param prototypeName prototype of the objects to be removed
    */
   protected void deleteAllObjects(OpBroker broker, String prototypeName) {
      OpQuery query = broker.newQuery("from " + prototypeName);
      for (Object o : broker.list(query)) {
         OpObject object = (OpObject) o;
         broker.deleteObject(object);
      }
   }


}
