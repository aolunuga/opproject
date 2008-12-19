/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.xml_rpc.test;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.sql.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.my_tasks.OpMyTasksService;
import onepoint.project.modules.my_tasks.test.OpMyTasksTestDataFactory;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.project_planning.test.OpProjectPlanningTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.xml_rpc.OpOpenXMLRPCServlet;
import onepoint.project.xml_rpc.OpXMLRPCUtil;
import onepoint.service.XMessage;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.server.XmlRpcStreamServer;
import org.apache.xmlrpc.util.ThreadPool.InterruptableTask;
import org.apache.xmlrpc.util.ThreadPool.Task;
import org.apache.xmlrpc.webserver.HttpServletRequestImpl;
import org.apache.xmlrpc.webserver.HttpServletResponseImpl;
import org.apache.xmlrpc.webserver.ServletWebServer;
import org.apache.xmlrpc.webserver.WebServer;

/**
 * currently supported services:
 * 'UserService.signOn',
 * 'UserService.signOff',
 * 'UserService.getSignedOnUserData',
 * 'MyTasksService.getRootTasks',
 * 'MyTasksService.getParentTask',
 * 'MyTasksService.getChildTasks',
 * 'MyTasksService.getMyTasks'
 */

public class OpXMLRPCServiceTest extends OpBaseOpenTestCase {

   private static final String DEFAULT_USER = "tester";
   private static final String DEFAULT_PASSWORD = "pass";

   private static final String RESOURCE_NAME = "resource";
   private static final String PROJECT_NAME = "project";
   private static final String ACTIVITY_NAME = "activity";
// private static final String ATTACH_NAME = "Attachment";

// // Password used for tests.
// private static final String TEST_PASS = new OpHashProvider().calculateHash("password");
// // User data used through tests.
// private static final String TEST_USER_NAME = "tester";
// private static final String TEST_EMAIL = "tester@onepoint.at";
// private static final String TEST_LANGUAGE = "en";
// // Group data
////private static final String TEST_GROUP_NAME = "group1";
// // Dummy string
// private static final String DUMMY_STRING = "tester";


   private OpMyTasksService myTaskService;

   // private OpUserService userService;
   private ServletWebServer server = null;
   private XmlRpcClient client;
   //   private OpWorkService service;
   private OpProjectPlanningService planningService;
   private OpProjectPlanningTestDataFactory planningDataFactory;
   private OpProjectTestDataFactory projectDataFactory;
   private OpResourceTestDataFactory resourceDataFactory;

   private String resId;
   private String projId;
   private String planId;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */

   /**
    *
    */
   public OpXMLRPCServiceTest() {
      // start up mini http xml-rpc server
      final OpOpenXMLRPCServlet servlet = new OpOpenXMLRPCServlet();
      try {
         server = new ServletWebServer(servlet, 8888) {
            /* (non-Javadoc)
             * @see org.apache.xmlrpc.webserver.ServletWebServer#newTask(org.apache.xmlrpc.webserver.WebServer, org.apache.xmlrpc.server.XmlRpcStreamServer, java.net.Socket)
             */
            @Override
            protected Task newTask(WebServer pWebServer, XmlRpcStreamServer pXmlRpcServer, Socket pSocket)
                 throws IOException {
               return new OpServletConnection(servlet, pSocket);
            }
         };
         server.start();
      }
      catch (Exception exc) {
         exc.printStackTrace();
      }
   }

   /* (non-Javadoc)
    * @see java.lang.Object#finalize()
    */
   @Override
   protected void finalize()
        throws Throwable {
      super.finalize();
      if (server != null) {
         server.shutdown();
         server = null;
      }
   }

   protected void setUp()
        throws Exception {
      super.setUp();

      //      service = OpTestDataFactory.getWorkService();
      myTaskService = OpTestDataFactory.getMyTasksService();
      planningService = OpTestDataFactory.getProjectPlanningService();
      planningDataFactory = new OpProjectPlanningTestDataFactory(session);
      projectDataFactory = new OpProjectTestDataFactory(session);
      resourceDataFactory = new OpResourceTestDataFactory(session);
      //userService = OpTestDataFactory.getUserService();
      OpUserTestDataFactory userDataFactory = new OpUserTestDataFactory(session);
      //onepoint = new OpXmlRpc("http://localhost:8080/opproject/xml-rpc");

      clean();

      Map userData = OpUserTestDataFactory.createUserData(DEFAULT_USER, DEFAULT_PASSWORD, null, OpUser.CONTRIBUTOR_USER_LEVEL,
           "John", "Doe", "en", "user@email.com", null, null, null, null);
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = OpTestDataFactory.getUserService().insertUser(session, request);
      assertNoError(response);

      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      request = resourceDataFactory.createResourceMsg("resource", "description", 50d, 2d, 1d, false, poolid);
      response = OpTestDataFactory.getResourceService().insertResource(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      try {
         resId = resourceDataFactory.getResourceByName("resource").locator();
         OpTransaction t = broker.newTransaction();
         OpResource res = (OpResource) broker.getObject(resId);
         OpUser user = userDataFactory.getUserByName(DEFAULT_USER);
         res.setUser(user);
         broker.updateObject(res);
         t.commit();

//       OpBroker broker2 = session.newBroker();
//       assertEquals(userDataFactory.getUserByName(DEFAULT_USER).getResources().size(), 1);

//       String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
//       XMessage request = resourceDataFactory.createResourceMsg(RESOURCE_NAME, "description", 50d, 2d, 1d, false, poolid);
//       XMessage response = OpTestDataFactory.getResourceService().insertResource(session, request);
//       assertNoError(response);
//       resId = resourceDataFactory.getResourceByName(RESOURCE_NAME).locator();
//       request = resourceDataFactory.createResourceMsg(RESOURCE_NAME + 2, "description", 10d, 9d, 1d, false, poolid);
//       response = OpTestDataFactory.getResourceService().insertResource(session, request);
//       assertNoError(response);
////     res2Id = resourceDataFactory.getResourceByName(RESOURCE_NAME + 2).locator();

         request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME, new Date(1), 1d, null, null);
         response = OpTestDataFactory.getProjectService().insertProject(session, request);
         assertNoError(response);
         projId = projectDataFactory.getProjectId(PROJECT_NAME);
//       request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME + 2, new Date(1000), 6d, null, null);
//       response = OpTestDataFactory.getProjectService().insertProject(session, request);
//       assertNoError(response);
////     proj2Id = projectDataFactory.getProjectId(PROJECT_NAME + 2);

         planId = projectDataFactory.getProjectById(projId).getPlan().locator();
////     plan2Id = projectDataFactory.getProjectById(proj2Id).getPlan().locator();

         // setup xml client
         XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
         config.setServerURL(new URL("http://localhost:8888/opproject/xml-rpc"));
         config.setEnabledForExceptions(false); // no serialisation
         client = new XmlRpcClient();
         client.setConfig(config);

         Object[] params = new Object[]{DEFAULT_USER, DEFAULT_PASSWORD};
         Boolean result = (Boolean) client.execute("UserService.signOn", params);
         assertTrue(result);
      }
      finally {
         broker.close();
      }
//    request = new XMessage();
//    request.setArgument(OpUserService.LOGIN, DEFAULT_USER);
//    request.setArgument(OpUserService.PASSWORD, DEFAULT_PASSWORD);
//    response = OpTestDataFactory.getUserService().signOn(session, request);
//    assertNoError(response);
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      clean();

      Object[] params = new Object[]{};
      Boolean result = (Boolean) client.execute("UserService.signOff", params);
      assertTrue(result);

      super.tearDown();
   }
   
   public void testSignOn()
   throws Exception {
      Object[] params = new Object[]{DEFAULT_USER, DEFAULT_PASSWORD};
      Boolean result = (Boolean) client.execute("UserService.signOn", params);
      assertTrue(result);
   }
   public void testGetMyTasks()
        throws Exception {
      insertMyTask();

      Object[] params = new Object[]{};
      //List<Map<String, Object>> result = (List<Map<String, Object>>) 
//    Object obj = client.execute("MyTasksService.getMyTasks", params);
      Object[] result = (Object[]) client.execute("MyTasksService.getMyTasks", params);

      assertEquals(result.length, 1); // only one task
      Map args = (Map) result[0];
      assertEquals(args.get("Name"), ACTIVITY_NAME);
      assertEquals(args.get("Description"), "descr");
      assertEquals(args.get("Priority"), new Integer(5));
      assertEquals(args.get("Finish"), Date.valueOf("2007-05-24"));

      assertEquals(new Integer(OpActivity.ADHOC_TASK), args.get("Type"));

      assertEquals(1, ((Object[]) args.get((String) args.get("Assignments"))).length);
      assertEquals(planId, ((Map) args.get((String) args.get("ProjectPlan"))).get(OpXMLRPCUtil.LOCATOR_KEY));

   }

   public void testGetUserData()
   throws Exception {
      Object[] params = new Object[]{DEFAULT_USER, DEFAULT_PASSWORD};
      Boolean result = (Boolean) client.execute("UserService.signOn", params);
      assertTrue(result);

      Map<String, Object> userData = (Map<String, Object>) client.execute("UserService.getSignedOnUserData", new Object[] {});
      assertEquals(userData.get("DisplayName"), "John Doe");
      assertEquals(userData.get("EMail"), "user@email.com");
      assertEquals(userData.get("level"), 1);
      assertEquals(userData.get("DisplayName"), "John Doe");
      assertEquals(userData.get("FirstName"), "John");
      assertEquals(userData.get("Name"), "tester");
      assertEquals(userData.get("LastName"), "Doe");
   }


   private void insertMyTask() {
      Date duedate = Date.valueOf("2007-05-24");
      String prjChoice = XValidator.choice(projId, PROJECT_NAME);
      String resChoice = XValidator.choice(resId, RESOURCE_NAME);
      XMessage request = OpMyTasksTestDataFactory.addAdhocMsg(ACTIVITY_NAME, "descr", 5, duedate, prjChoice, resChoice);
      XMessage response = myTaskService.addAdhocTask(session, request);
      assertNoError(response);
   }
   
  public static void main(String[] args) {
     OpXMLRPCServiceTest test = new OpXMLRPCServiceTest();
  }
}

class OpServletConnection implements InterruptableTask {
   private final HttpServlet servlet;
   private final Socket socket;
   private final HttpServletRequest request;
   private final HttpServletResponse response;
   private boolean shuttingDown;

   /**
    * Creates a new instance.
    *
    * @param pServlet The servlet, which ought to handle the request.
    * @param pSocket  The socket, to which the client is connected.
    * @throws IOException
    */
   public OpServletConnection(HttpServlet pServlet, Socket pSocket)
        throws IOException {
      servlet = pServlet;
      socket = pSocket;
      request = new HttpServletRequestImpl(socket) {
         /* (non-Javadoc)
          * @see org.apache.xmlrpc.webserver.HttpServletRequestImpl#getSession()
          */
         @Override
         public HttpSession getSession() {
            return MySession.getSession(true);
         }

         /* (non-Javadoc)
         * @see org.apache.xmlrpc.webserver.HttpServletRequestImpl#getSession(boolean)
         */
         @Override
         public HttpSession getSession(boolean pCreate) {
            return MySession.getSession(pCreate);
         }
      };
      response = new HttpServletResponseImpl(socket);
   }

   public void run()
        throws Throwable {
      try {
         servlet.service(request, response);
      }
      catch (Throwable t) {
         if (!shuttingDown) {
            throw t;
         }
      }
   }

   public void shutdown()
        throws Throwable {
      shuttingDown = true;
      socket.close();
   }
}

class MySession implements HttpSession {
   static MySession session;

   static MySession getSession(boolean create) {
      if ((session == null) && (create)) {
         session = new MySession();
      }
      return session;
   }

   Hashtable attributeValues = new Hashtable();

   public MySession() {
   }

   public long getCreationTime() {
      throw new java.lang.UnsupportedOperationException("Method getCreationTime() not yet implemented.");
   }

   public String getId() {
      throw new java.lang.UnsupportedOperationException("Method getId() not yet implemented.");
   }

   public long getLastAccessedTime() {
      throw new java.lang.UnsupportedOperationException("Method getLastAccessedTime() not yet implemented.");
   }

   public ServletContext getServletContext() {
      /**@todo: Implement this javax.servlet.http.HttpSession method*/
      throw new java.lang.UnsupportedOperationException("Method getServletContext() not yet implemented.");
   }

   public void setMaxInactiveInterval(int parm1) {
      /**@todo: Implement this javax.servlet.http.HttpSession method*/
      throw new java.lang.UnsupportedOperationException("Method setMaxInactiveInterval() not yet implemented.");
   }

   public int getMaxInactiveInterval() {
      /**@todo: Implement this javax.servlet.http.HttpSession method*/
      throw new java.lang.UnsupportedOperationException("Method getMaxInactiveInterval() not yet implemented.");
   }

   public HttpSessionContext getSessionContext() {
      /**@todo: Implement this javax.servlet.http.HttpSession method*/
      throw new java.lang.UnsupportedOperationException("Method getSessionContext() not yet implemented.");
   }

   public Object getAttribute(String parm1) {
      return attributeValues.get(parm1);
   }

   public Object getValue(String parm1) {
      throw new java.lang.UnsupportedOperationException("Method not yet implemented.");
   }

   public Enumeration getAttributeNames() {
      return attributeValues.keys();
   }

   public String[] getValueNames() {
      /**@todo: Implement this javax.servlet.http.HttpSession method*/
      throw new java.lang.UnsupportedOperationException("Method getValueNames() not yet implemented.");
   }

   public void setAttribute(String parm1, Object parm2) {
      attributeValues.put(parm1, parm2);
   }

   public void putValue(String parm1, Object parm2) {
      /**@todo: Implement this javax.servlet.http.HttpSession method*/
      throw new java.lang.UnsupportedOperationException("Method putValue() not yet implemented.");
   }

   public void removeAttribute(String parm1) {
      attributeValues.remove(parm1);
   }

   public void removeValue(String parm1) {
      /**@todo: Implement this javax.servlet.http.HttpSession method*/
      throw new java.lang.UnsupportedOperationException("Method removeValue() not yet implemented.");
   }

   public void invalidate() {
      /**@todo: Implement this javax.servlet.http.HttpSession method*/
      throw new java.lang.UnsupportedOperationException("Method invalidate() not yet implemented.");
   }

   public boolean isNew() {
      /**@todo: Implement this javax.servlet.http.HttpSession method*/
      throw new java.lang.UnsupportedOperationException("Method isNew() not yet implemented.");
   }
}


