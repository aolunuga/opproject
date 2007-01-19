/*
* Copyright(c) OnePoint Software GmbH 2005. All Rights Reserved.
*/
package onepoint.project.test;

import onepoint.persistence.*;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleManager;
import onepoint.project.module.OpModuleRegistryLoader;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.*;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.util.XEnvironment;
import org.apache.log4j.Logger;
import org.jmock.Mock;
import org.jmock.cglib.CGLIBCoreMock;
import org.jmock.cglib.MockObjectTestCase;
import org.jmock.core.*;

import java.util.*;

/**
 * @author mihai.costin
 *         Date: Dec 5, 2005
 *         <p/>
 *         Abstract class that defines common functionality for all service test cases.
 */
public abstract class OpServiceAbstractTest extends MockObjectTestCase {

   private static final Logger log = Logger.getLogger(OpServiceAbstractTest.class);
   protected Mock mockSession;
   protected Mock mockBroker;
   protected Mock mockQuery;
   protected Mock mockTransaction;
   protected XLocale userLocale;
   protected Stub methodStub = new MethodStub();

   /**
    * Query used by broker.
    */
   protected OpQuery query;

   /**
    * Error used to return a message to the user
    */
   protected XError error;

   /**
    *id generator
    */
   private static long id = 1;

   /**
    * Query results. (returned by iterate method)
    */
   protected List queryResults;


   //method names
   protected static final String NEW_BROKER_METHOD = "newBroker";
   protected static final String FIND_METHOD = "find";
   protected static final String ITERATE_METHOD = "iterate";
   protected static final String NEW_QUERY_METHOD = "newQuery";
   protected static final String SET_LONG_METHOD = "setLong";
   protected static final String SET_DATE_METHOD = "setDate";
   protected static final String SET_INTEGER_METHOD = "setInteger";
   protected static final String SET_STRING_METHOD = "setString";
   protected static final String SET_BYTE_METHOD ="setByte";
   protected static final String ACCESSIBLE_IDS_METHOD ="accessibleIds";
   protected static final String SET_COLLECTION_METHOD = "setCollection";
   protected static final String LOAD_SUBJECTS_IDS_METHOD = "loadSubjectIds";
   protected static final String GET_LOCALE_METHOD = "getLocale";
   protected static final String SET_LOCALE_METHOD = "setLocale";
   protected static final String GET_CONNECTION = "getConnection";
   protected static final String CLOSE_METHOD = "close";
   protected static final String NEW_TRANSACTION_METHOD = "newTransaction";
   protected static final String COMMIT_METHOD = "commit";
   protected static final String ROLLBACK_METHOD = "rollback";
   protected static final String MAKE_PERSISTENT_METHOD = "makePersistent";
   protected static final String USER_METHOD = "user";
   protected static final String GET_USER_ID_METHOD = "getUserID";
   protected static final String GET_OBJECT_METHOD = "getObject";
   protected static final String UPDATE_OBJECT_METHOD = "updateObject";
   protected static final String DELETE_OBJECT_METHOD = "deleteObject";
   protected static final String NEW_ERROR_METHOD = "newError";
   protected static final String CHECK_ACCESS_LEVEL_METHOD ="checkAccessLevel";
   protected static final String LOOK_UP_ADMINISTRATOR_METHOD = "lookUpAdministratorID";
   protected static final String LOOK_UP_EVERYONE_METHOD = "lookUpEveryoneID";
   protected static final String AUTHENTICATE_USER_METHOD = "authenticateUser";
   protected static final String LIST_METHOD ="list";
   protected static final String ACCESSIBLE_OBJECTS_METHOD = "accessibleObjects";
   protected static final String GET_ADMINISTRATOR_ID_METHOD = "getAdministratorID";
   protected static final String CLEAR_VARIABLES_METHOD ="clearVariables";
   protected static final String EVERYONE_METHOD ="everyone";
   protected static final String EFFECTIVE_ACCESS_LEVEL_METHOD ="effectiveAccessLevel";


   static {

      //Setup environment for all the tests

      log.debug("Static set up block for all test cases");
      XEnvironment.setVariable(OpEnvironmentManager.ONEPOINT_HOME, "onepoint/project/test");
      OpModuleRegistryLoader registryLoader = new OpModuleRegistryLoader();
      registryLoader.setUseResourceLoader(true);
      OpModuleManager.setModuleRegistryLoader(registryLoader);
      XResourceBroker.setResourcePath("onepoint/project");
      OpModuleManager.load();
   }

   /**
    * Sets up some resources that are needed by all tests.
    */
   protected void setUp() {
      XLocaleMap resources = new XLocaleMap();
      userLocale = new XLocale("en", "engl");
      XLanguageResourceMap resourceMap = new XLanguageResourceMap("error");
      userLocale.registerResourceMap(resourceMap);
      resources.addLocale(userLocale);
      XLocaleManager.setLocaleMap(resources);

      mockBroker = super.mock(OpBroker.class, "MockedXBroker");
      mockTransaction = super.mock(OpTransaction.class,"MockedXTransaction");
      mockQuery = super.mock(OpQuery.class,"MockedXQuery");

      query = (OpQuery)mockQuery.proxy();
      mockBroker.expects(atLeastOnce()).method(GET_CONNECTION);
      mockSession = mock(OpProjectSession.class, "MockedXSession");

   }


   /**
    * Will mock the given class (special case for session)
    * @param c Class to be mocked
    * @param name mane of the new class
    * @return the mocked onbject
    */
   public Mock mock(Class c, String name){
      //((InvocationDispatcher) (new LIFOInvocationDispatcher()))
      InvocationDispatcher disp = new LIFOInvocationDispatcher();

      InvocationMocker invMock = new InvocationMocker();
      invMock.setStub(methodStub);
      disp.add(invMock);

      //new CoreMock(class1, s)
      Mock mock1 = new Mock(new CGLIBCoreMock(c, name, disp));
      registerToVerify(mock1);
      return mock1;
   }

   /**
    * Stub used to map the behavior of the mocked methods in a mocked object.
    */
   private class MethodStub implements Stub {

      public StringBuffer describeTo(StringBuffer buffer) {
         return buffer.append("Mocks the behavior of the method");
      }

      public Object invoke(Invocation invocation)
           throws Throwable {

         //in this way the mocked broker is linked to the mocked session
         if (invocation.invokedMethod.getName().equals(NEW_BROKER_METHOD)) {
            return mockBroker.proxy();
         }
         else if (invocation.invokedMethod.getName().equals(NEW_TRANSACTION_METHOD)) {
            return mockTransaction.proxy();
         }
         else if (invocation.invokedMethod.getName().equals(CLOSE_METHOD)) {
            return null;
         }
         else if (invocation.invokedMethod.getName().equals(NEW_ERROR_METHOD)) {
            error = new XError(10, "TestError", "Mock Test Error");
            return error;
         }
         else if (invocation.invokedMethod.getName().equals(GET_LOCALE_METHOD)) {
            return new XLocale("error", "error");
         }
         else if (invocation.invokedMethod.getName().equals(MAKE_PERSISTENT_METHOD)) {
            ((OpObject) invocation.parameterValues.get(0)).setID(id);
            id++;
            return null;
         }
         else if (invocation.invokedMethod.getName().equals(NEW_QUERY_METHOD)) {
            return query;
         }
         else if (invocation.invokedMethod.getName().equals(LOOK_UP_ADMINISTRATOR_METHOD)) {
            return null;
         }
         else if (invocation.invokedMethod.getName().equals(LOOK_UP_EVERYONE_METHOD)) {
            return null;
         }
         else if (invocation.invokedMethod.getName().equals(ITERATE_METHOD)) {
            return queryResults.iterator();
         }
         else if (invocation.invokedMethod.getName().equals(LIST_METHOD)) {
            return queryResults;
         }
         else {
            return invocationMatch(invocation);
         }
      }
   }

   /**
    * Specifies the behaviour of the mocked methods.
    *
    * @param invocation contains the object and the invoked method
    * @return depends on the invoked moethod
    * @throws IllegalArgumentException if no such method was defined in this mapping
    */
   public abstract Object invocationMatch(Invocation invocation)
        throws IllegalArgumentException;

   /**
    * Creates a XStruct that contains all the elements of the given map
    *
    * @param values <code>XArray</code>
    * @return a <code>XStruct</code> with the info from the <code>Map</code>
    */
   protected static HashMap transformToXStruct(Map values) {

      HashMap dataStruct = new HashMap();
      Set keys = values.keySet();
      for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
         Object name = iterator.next();
         dataStruct.put(name, values.get(name));
      }
      return dataStruct;
   }


   /**
    * Asserts that a XMessage reply from a service method contains not error msgs.
    *
    * @param message
    */
   public static void assertNoError(XMessage message){
      if (message != null){
         assertNull("No error message should have been returned", message.getError());
      }
      else {
         //message is null <=> no error (success)
      }
   }
}
