package onepoint.project.test;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.*;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.report.OpReportType;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.modules.work.OpCostRecord;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XLocalServer;
import onepoint.service.server.XServer;
import onepoint.service.server.XSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * @author mihai.costin
 */
public class OpBaseTestCase extends OpTestCase {

   protected static final String PROPERTY_FILE = "tests.properties";
   protected static final String INITIALIZER_KEY = "initializer";

   // This is the session that must be used into all test methods.
   protected OpProjectSession session;

   protected XServer server;

   protected String adminId;

   protected String rootId;

   protected static OpTestInitializer testInitializer;

   private static final XLog logger = XLogFactory.getServerLogger(OpBaseTestCase.class);

   private static final int DUMMY_ERROR_CODE = -49765;

   protected static void initTests(Class baseTestClass) {
      try {
         URL propertyResource = baseTestClass.getResource(PROPERTY_FILE);
         File propertyFile = new File(propertyResource.toURI());
         File propertyFileParent = propertyFile.getParentFile();
         OpEnvironmentManager.setOnePointHome(propertyFileParent.getPath());

         Properties testProperties = new Properties();
         testProperties.load(new FileInputStream(propertyFile));
         String className = (String) testProperties.get(INITIALIZER_KEY);

         logger.info("Class that will be used for test initialization=" + className + ".");
         Class initializerClass = Thread.currentThread().getContextClassLoader().loadClass(className);

         testInitializer = (OpTestInitializer) initializerClass.newInstance();
         testInitializer.initialize(testProperties);
      }
      catch (ClassNotFoundException e) {
         logger.error("OpBaseTestCase.initTests", e);
      }
      catch (IllegalAccessException e) {
         logger.error("OpBaseTestCase.initTests", e);
      }
      catch (InstantiationException e) {
         logger.error("OpBaseTestCase.initTests", e);
      }
      catch (IOException e) {
         logger.error("OpBaseTestCase.initTests", e);
      }
      catch (URISyntaxException e) {
         logger.error("OpBaseTestCase.initTests", e);
      }

   }

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
      this.server.setSessionClass(OpProjectSession.class);
      if (testInitializer != null) {
         this.session = (OpProjectSession) server.newSession(new Object[]{testInitializer.getSourceName()});
      }
      else {
         this.session = (OpProjectSession) server.newSession();
      }
      XSession.setSession(session);

      this.session.setServer(this.server);

      // Authenticate user to be sure that he has access.
      logIn(OpUser.ADMINISTRATOR_NAME, OpUser.BLANK_PASSWORD);

      OpBroker broker = session.newBroker();
      try {
         // administrator
         adminId = session.administrator(broker).locator();
         // root pool
         rootId = OpResourceService.findRootPool(broker).locator();
      }
      finally {
         broker.close();
      }
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      super.tearDown();

      // check if the objects from database are still valid.
      testInitializer.checkObjects(session);

      // Log-off user.
      logOut();

      this.session.close();
      XSession.removeSession();
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
      assertError(error, errorCode);
   }

   /**
    * @param error
    * @param errorCode
    */
   public static void assertError(XError error, int errorCode) {
      assertNotNull("Error message should have been returned", error);
      int foundErrorCode = error.getCode();
      // do not check error code in case DUMMY code was used.
      assertTrue("Invalid error code. (got: " + foundErrorCode + ", but expected: " + errorCode + ")", errorCode == DUMMY_ERROR_CODE || errorCode == foundErrorCode);
      assertNotNull("Error should contain an error name.", error.getName());
      assertNotNull("Error should contain an error message.", error.getName());
   }

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   protected void clean()
        throws Exception {

      OpUserTestDataFactory userDataFactory = new OpUserTestDataFactory(session);
      OpProjectTestDataFactory projectDataFactory = new OpProjectTestDataFactory(session);
      OpResourceTestDataFactory resourceDataFactory = new OpResourceTestDataFactory(session);

      deleteAllObjects(OpSettings.SETTING);
      deleteAllObjects(OpDocument.DOCUMENT);
      deleteAllObjects(OpWorkPeriod.WORK_PERIOD);
      deleteAllObjects(OpWorkRecord.WORK_RECORD);
      deleteAllObjects(OpWorkSlip.WORK_SLIP);
      deleteAllObjects(OpActivityComment.ACTIVITY_COMMENT);
      deleteAllObjects(OpActivityVersion.ACTIVITY_VERSION);
      deleteAllObjects(OpActivity.ACTIVITY);
      deleteAllObjects(OpActivityCategory.ACTIVITY_CATEGORY);
      deleteAllObjects(OpProjectNodeAssignment.PROJECT_NODE_ASSIGNMENT);
      deleteAllObjects(OpAttachment.ATTACHMENT);
      deleteAllObjects(OpAttachmentVersion.ATTACHMENT_VERSION);
      deleteAllObjects(OpDocumentNode.DOCUMENT_NODE);
      deleteAllObjects(OpFolder.FOLDER);
      deleteAllObjects(OpAssignmentVersion.ASSIGNMENT_VERSION);
      deleteAllObjects(OpAssignment.ASSIGNMENT);
      deleteAllObjects(OpProjectPlanVersion.PROJECT_PLAN_VERSION);
      deleteAllObjects(OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(OpContent.CONTENT);
      deleteAllObjects(OpCostRecord.COST_RECORD);
      deleteAllObjects(OpReportType.REPORT_TYPE);
      deleteAllObjects(OpDynamicResource.DYNAMIC_RESOURCE);
      deleteAllObjects(projectDataFactory.getAllProjects(session));
      deleteAllObjects(projectDataFactory.getAllPortofolios(session), Arrays.asList(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME));
      deleteAllObjects(OpResource.RESOURCE);
      deleteAllObjects(resourceDataFactory.getAllResourcePools(session), Arrays.asList(OpResourcePool.ROOT_RESOURCE_POOL_NAME));
      deleteAllObjects(OpProjectStatus.PROJECT_STATUS);

      deleteAllObjects(userDataFactory.getAllUsers(session), Arrays.asList(OpUser.ADMINISTRATOR_NAME));
      deleteAllObjects(OpGroup.GROUP, Arrays.asList(OpGroup.EVERYONE_NAME));
//      deleteAllObjects("OpCustomAttribute");
//      deleteAllObjects("OpCustomType");
   }

   /**
    * Deletes all the objects of the given prototype.
    *
    * @param prototypeName prototype of the objects to be removed
    */
   protected void deleteAllObjects(String prototypeName) {
      deleteAllObjects(prototypeName, null);
   }

   /**
    * Deletes all the objects of the given prototype.
    *
    * @param prototypeName prototype of the objects to be removed
    * @param excludeNames  names of the objects to be excluded from deletion
    */
   protected void deleteAllObjects(String prototypeName, List<String> excludeNames) {
      OpBroker broker = session.newBroker();
      try {
         if (prototypeName.equalsIgnoreCase(OpContent.CONTENT)) {
            removeAllContents(broker);
         }
         else {
            OpQuery query = broker.newQuery("from " + prototypeName);
            Iterator iter = broker.iterate(query);
            deleteAllObjects(broker, iter, excludeNames);
         }
      }
      finally {
         broker.close();
      }
   }

   /**
    * Removes all the content objects from the db.
    *
    * @param broker a <code>OpBroker</code> used for persistence operations. The broker
    *               must not be closed by this method.
    */
   private void removeAllContents(OpBroker broker) {
      OpQuery query = broker.newQuery("select content.ID from " + OpContent.CONTENT + " content");
      List<Long> contentIds = broker.list(query);
      for (long contentID : contentIds) {
         OpTransaction t = broker.newTransaction();
         OpContent content = broker.getObject(OpContent.class, contentID);
         broker.deleteObject(content);
         t.commit();
      }
   }

   /**
    * Deletes all the objects of the given prototype.
    *
    * @param objects list of objects to be deleted.
    */
   protected void deleteAllObjects(List objects) {
      deleteAllObjects(objects, null);
   }

   /**
    * Deletes all the objects of the given prototype.
    *
    * @param objects      list of objects to be deleted.
    * @param excludeNames names of the objects to be excluded from deletion
    */
   protected void deleteAllObjects(List objects, List<String> excludeNames) {
      if (objects != null || objects.size() > 0) {
         OpBroker broker = session.newBroker();
         try {
            deleteAllObjects(broker, objects.iterator(), excludeNames);
         }
         finally {
            broker.close();
         }
      }
   }

   /**
    * Deletes all the objects from the given iterator.
    *
    * @param broker   broker instance
    * @param iterator iterator over the objects to be deleted.
    */
   protected void deleteAllObjects(OpBroker broker, Iterator iterator) {
      deleteAllObjects(broker, iterator, null);
   }

   /**
    * Deletes all the objects from the given iterator.
    *
    * @param broker   broker instance
    * @param iterator iterator over the objects to be deleted.
    */
   protected void deleteAllObjects(OpBroker broker, Iterator iterator, List<String> excludeNames) {
      OpTransaction transaction = broker.newTransaction();
      List<OpObject> objs = new ArrayList<OpObject>();
      while (iterator.hasNext()) {
         OpObject object = (OpObject) iterator.next();
         objs.add(broker.getObject(OpObject.class, object.getID()));
      }
      iterator = objs.iterator();
      while (iterator.hasNext()) {
         OpObject object = (OpObject) iterator.next();

         if (excludeNames != null && excludeNames.size() > 0) {
            if (checkObject(object, excludeNames)) {
               broker.deleteObject(object);
            }
         }
         else {
            broker.deleteObject(object);
         }
      }
      transaction.commit();
   }

   /**
    * Check if the given object has a method getName and if this returns a name contained into excludedNames list.
    *
    * @param object       object to be checked
    * @param excludeNames list of names to be excluded
    * @return true if the object name is not into excluded names list or it does not have getName method.
    */
   private boolean checkObject(OpObject object, List<String> excludeNames) {
      try {
         Method method = object.getClass().getMethod("getName");
         String name = (String) method.invoke(object);

         return !excludeNames.contains(name);
      }
      catch (IllegalAccessException e) {
         // means that the getName method can not be called so object should be deleted.
         return true;
      }
      catch (InvocationTargetException e) {
         // means that the getName method can not be called so object should be deleted.
         return true;
      }
      catch (NoSuchMethodException e) {
         // means that the getName method can not be called so object should be deleted.
         return true;
      }
   }

}
