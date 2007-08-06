/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.service.XSizeInputStream;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class test project service methods.
 *
 * @author lucian.furtos
 */
public class OpActivityDataSetFactoryTest extends OpBaseOpenTestCase {

   private static final String PRJ_NAME = "prj_test";
   private static final int STREAM_SIZE = 2 * 1024 * 1024; // 2 MB

   private OpProjectAdministrationService service;
   private OpProjectTestDataFactory dataFactory;
   private OpResourceTestDataFactory resourceDataFactory;

   private OpProjectNode project;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      service = OpTestDataFactory.getProjectService();
      dataFactory = new OpProjectTestDataFactory(session);
      resourceDataFactory = new OpResourceTestDataFactory(session);

      clean();

      java.sql.Date date = java.sql.Date.valueOf("2007-06-06");
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, date, 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      project = dataFactory.getProjectByName(PRJ_NAME);

   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      clean();
      super.tearDown();
   }

   /**
    * Test failed create attachments
    *
    * @throws Exception if the test fails
    */
   public void testFailedCreateAttachments()
        throws Exception {
      OpProjectPlan plan = project.getPlan();

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity = createActivity(plan, broker);

      // use an invalid Content ID - null
      List attachmentElement = generateAttachElements(null);

      OpActivityDataSetFactory.createAttachment(broker, activity, activity.getProjectPlan(), attachmentElement, null, null);
      t.commit();
      broker.close();

      // load content
      broker = session.newBroker();

      OpQuery query = broker.newQuery("from " + OpAttachment.ATTACHMENT);
      List list = broker.list(query);
      assertNotNull(list);
      assertTrue(list.isEmpty());  // no attachemnt created because invalid content

      broker.close();
   }

   /**
    * Test happy flow for create attachments
    *
    * @throws Exception if the test fails
    */
   public void testCreateAttachments()
        throws Exception {
      // create the content
       byte[] bytes = "The content of the file".getBytes();
      String contentId = createContent(bytes);

      OpProjectPlan plan = project.getPlan();

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpActivity activity = createActivity(plan, broker);
      List attachmentElement = generateAttachElements(contentId);
      OpActivityDataSetFactory.createAttachment(broker, activity, activity.getProjectPlan(), attachmentElement, null, null);
      t.commit();
      broker.close();

      // load content
      broker = session.newBroker();

      OpQuery query = broker.newQuery("from " + OpAttachment.ATTACHMENT);
      List list = broker.list(query);
      assertNotNull(list);
      assertEquals(1, list.size());

      query = broker.newQuery("from " + OpContent.CONTENT);
      list = broker.list(query);
      assertNotNull(list);
      assertEquals(1, list.size());

      broker.close();
   }

   /**
    * Test happy flow for create attachments
    *
    * @throws Exception if the test fails
    */
   public void testUpdateAttachments()
        throws Exception {
      // create the content
      byte[] bytes = new byte[STREAM_SIZE];
      for (int i = 0; i < bytes.length; i++) {
         bytes[i] = (byte) (i % 256);
      }
      String contentId = createContent(bytes);

      OpProjectNode project = dataFactory.getProjectByName(PRJ_NAME);
      OpProjectPlan plan = project.getPlan();

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity = createActivity(plan, broker);

      List attachmentElement = generateAttachElements(contentId);

      OpActivityDataSetFactory.createAttachment(broker, activity, activity.getProjectPlan(), attachmentElement, null, null);
      t.commit();
      broker.close();

      // load content
      broker = session.newBroker();
      t = broker.newTransaction();

      OpQuery query = broker.newQuery("from " + OpAttachment.ATTACHMENT);
      List list = broker.list(query);
      assertEquals(1, list.size());
      OpAttachment attachment = (OpAttachment) list.get(0);

      OpContent content = (OpContent) broker.getObject(contentId);
      assertEquals(1, content.getRefCount());

      attachmentElement = generateAttachElements(contentId);
      List<OpAttachment> reuselist = new ArrayList<OpAttachment>();
      reuselist.add(attachment);
      OpActivityDataSetFactory.createAttachment(broker, activity, activity.getProjectPlan(), attachmentElement, reuselist, null);
      t.commit();
      broker.close();

      // load content
      broker = session.newBroker();

      OpContent actual = (OpContent) broker.getObject(contentId);
      assertEquals(2, actual.getRefCount());

      broker.close();
   }

   // ******** Helper Methods *********

   private List generateAttachElements(String contentId) {
      List attachmentElement = new ArrayList();
      attachmentElement.add(OpProjectConstants.DOCUMENT_ATTACHMENT_DESCRIPTOR);  // 0 - document type
      attachmentElement.add(null); // 1 - unused
      attachmentElement.add("attachment1"); // 2 - name
      attachmentElement.add("file.tmp"); // 3- location / filename
      attachmentElement.add(contentId); // 4 - content id
      return attachmentElement;
   }

   private OpActivity createActivity(OpProjectPlan plan, OpBroker broker) {
      OpActivity activity = new OpActivity(OpActivity.TASK);
      activity.setName("task1");
      activity.setProjectPlan(plan);
      activity.setStart(new java.sql.Date(System.currentTimeMillis() + 1000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      broker.makePersistent(activity);
      return activity;
   }

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpUserTestDataFactory usrData = new OpUserTestDataFactory(session);
      List<OpUser> users = usrData.getAllUsers(broker);
      for (OpUser user : users) {
         if (user.getName().equals(OpUser.ADMINISTRATOR_NAME)) {
            continue;
         }
         broker.deleteObject(user);
      }

      deleteAllObjects(broker, OpAttachment.ATTACHMENT);
      deleteAllObjects(broker, OpAssignmentVersion.ASSIGNMENT_VERSION);
      deleteAllObjects(broker, OpContent.CONTENT);
      deleteAllObjects(broker, OpAssignment.ASSIGNMENT);
      deleteAllObjects(broker, OpActivityVersion.ACTIVITY_VERSION);
      deleteAllObjects(broker, OpActivity.ACTIVITY);
      deleteAllObjects(broker, OpProjectPlanVersion.PROJECT_PLAN_VERSION);
      deleteAllObjects(broker, OpProjectPlan.PROJECT_PLAN);

      List<OpProjectNode> projectList = dataFactory.getAllProjects(broker);
      for (OpProjectNode project : projectList) {
        broker.deleteObject(project);
      }

      List<OpProjectNode> portofolioList = dataFactory.getAllPortofolios(broker);
      for (OpProjectNode portofolio : portofolioList) {
         if (portofolio.getName().equals(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)) {
            continue;
         }
         broker.deleteObject(portofolio);
      }

      List<OpResource> resoucesList = resourceDataFactory.getAllResources(broker);
      for (OpResource resource : resoucesList) {
         broker.deleteObject(resource);
      }

      List<OpResourcePool> poolList = resourceDataFactory.getAllResourcePools(broker);
      for (OpResourcePool pool : poolList) {
         if (pool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            continue;
         }
         broker.deleteObject(pool);
      }

      t.commit();
      broker.close();
   }

   /**
    * Create a new OpContent with the given data and returns the locator
    *
    * @param data the data to be inserted in the content
    * @return the locator of the new content
    */
   private String createContent(byte[] data) {
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpContent content = OpContentManager.newContent(new XSizeInputStream(new ByteArrayInputStream(data), data.length), null, 0);
      broker.makePersistent(content);
      String contentId = content.locator();
      t.commit();
      broker.close();
      return contentId;
   }

}
