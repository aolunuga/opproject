/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.server.XFormSchema;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocale;
import onepoint.service.XMessage;
import onepoint.service.XSizeInputStream;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

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
    * The name of the xml file that contains the test data.
    */
   private static final String TEST_DATA_FILENAME = "activityDataSetTestData.xml";

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
    * Loads a test data set from the given file.
    *
    * @param testDataFile a <code>xml</code> file containing a data set that will be used
    *                     for testing.
    * @return a <code>XComponent(DATA_SET)</code> containing test data.
    */
   private XComponent getTestDataSet(String testDataFile) {
      XLoader xmlLoader = new XLoader(new XDocumentHandler(new XFormSchema()));
      InputStream testDataInputStream = this.getClass().getResourceAsStream(testDataFile);
      XComponent testForm = (XComponent) xmlLoader.loadObject(testDataInputStream, null);

      //activity data set.
      return (XComponent) testForm.getChild(0);
   }

   /**
    * Tests the rebuilding of successors and predecessors indexes on an activity data set.
    *
    * @throws Exception if an error occured.
    */
   public void testRebuildPredecessorsSuccessorsIndexes()
        throws Exception {

      //init the default calendar
      XDisplay display = new XDisplay(null);
      display.getCalendar().configure(null, new XLocale("de", ""), null, null);
      XComponent dataSet = getTestDataSet(TEST_DATA_FILENAME);
      assertEquals(9, dataSet.getChildCount());

      //the map of successors rows for each activity in the data set
      Map<String, List<XComponent>> mapSuccessors = createSuccessorsMap(dataSet);
      //the map of predecessors rows for each activity in the data set
      Map<String, List<XComponent>> mapPredecessors = createPredecessorsMap(dataSet);

      /* create the Map<Integer, String> which has indexes of rows as keys and the names of the activities from the
         rows as values
       */
      Map<Integer, String> indexNameMap = new HashMap<Integer, String>();
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         indexNameMap.put(dataRow.getIndex(), OpGanttValidator.getName(dataRow));
      }

      //sort the data set according to the activity type
      dataSet.sort(OpGanttValidator.TYPE_COLUMN_INDEX);

      //the map of successors rows for each activity in the data set after sorting
      Map<String, List<XComponent>> mapSuccessorsPostSort = createSuccessorsMap(dataSet);
      //the map of predecessors rows for each activity in the data set after sorting
      Map<String, List<XComponent>> mapPredecessorsPostSort = createPredecessorsMap(dataSet);

      /* create the Map<String, Integer> which has the names of the activities from the
         rows as keys and the indexes of the data rows as values
       */
      Map<String, Integer> nameIndexMap = new HashMap<String, Integer>();
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         nameIndexMap.put(OpGanttValidator.getName(dataRow), dataRow.getIndex());
      }

      //after the sort the successors indexes point to the wrong data rows
      assertFalse(doRowsValuesMatch(mapSuccessors, mapSuccessorsPostSort));
      //after the sort the predecessors indexes point to the wrong data rows
      assertFalse(doRowsValuesMatch(mapPredecessors, mapPredecessorsPostSort));

      //rebuild the original successors and predecessors
      OpActivityDataSetFactory.rebuildPredecessorsSuccessorsIndexes(dataSet, indexNameMap, nameIndexMap);

      //the map of successors rows for each activity in the data set after rebuilding
      Map<String, List<XComponent>> mapSuccessorsPostRebuild = createSuccessorsMap(dataSet);
      //the map of predecessors rows for each activity in the data set after rebuilding
      Map<String, List<XComponent>> mapPredecessorsPostRebuild = createPredecessorsMap(dataSet);

      //after the rebuild the successors indexes point to the right data rows
      assertTrue(doRowsValuesMatch(mapSuccessors, mapSuccessorsPostRebuild));
      //after the rebuild the predecessors indexes point to the right data rows
      assertTrue(doRowsValuesMatch(mapPredecessors, mapPredecessorsPostRebuild));
   }

   /**
    * Creates a <code>Map</code> with all the successors for all activities in the data set.
      The structure of the map : Key - activity name
                                  Value - of data rows representing the successors of the activity

    * @param dataSet - the <code>XComponent</code> data set from which the successors are taken.
    * @return a lists with all the successors for all activities in the data set.
    */
   private Map<String, List<XComponent>> createSuccessorsMap(XComponent dataSet) {
      Map<String, List<XComponent>> successorsMap = new HashMap<String, List<XComponent>>();
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         List<XComponent> successorRows = new ArrayList<XComponent>();
         List<Integer> successors = OpGanttValidator.getSuccessors((XComponent) dataSet.getChild(i));
         if (!successors.isEmpty()) {
            for (Integer successorIndex : successors) {
               XComponent newDataRow = ((XComponent) dataSet.getChild(successorIndex)).copyData();
               successorRows.add(newDataRow);
            }
         }
         successorsMap.put(OpGanttValidator.getName((XComponent) dataSet.getChild(i)), successorRows);
      }
      return successorsMap;
   }

   /**
    * Creates a <code>Map</code> with all the predecessors for all activities in the data set.
      The structure of the map : Key - activity name
                                 Value - of data rows representing the predecessors of the activity
    *
    * @param dataSet - the <code>XComponent</code> data set from which the predecessors are taken.
    * @return a lists with all the predecessors for all activities in the data set.
    */
   private Map<String, List<XComponent>> createPredecessorsMap(XComponent dataSet) {
      Map<String, List<XComponent>> predecessorsMap = new HashMap<String, List<XComponent>>();
      for (int i = 0; i < dataSet.getChildCount(); i++) {

         List<XComponent> predecessorRows = new ArrayList<XComponent>();
         List<Integer> predecessors = OpGanttValidator.getPredecessors((XComponent) dataSet.getChild(i));
         if (!predecessors.isEmpty()) {
            for (Integer predecessorIndex : predecessors) {
               XComponent newDataRow = ((XComponent) dataSet.getChild(predecessorIndex)).copyData();
               predecessorRows.add(newDataRow);
            }
         }
         predecessorsMap.put(OpGanttValidator.getName((XComponent) dataSet.getChild(i)), predecessorRows);
      }
      return predecessorsMap;
   }

   /**
    * Checks the name values on the data rows in the maps passed al parameters and returns <code>true</code>
    *    if all the corresponding rows in both maps have the same name value set on them and <code>false</code>
    *    if at leat one rows has a different name value than it's corresponding row in the other map.
    *
    * @param oldMap
    * @param newMap
    * @return <code>true</code>
    *    if all the corresponding rows in both maps have the same name value set on them and <code>false</code>
    *    if at leat one rows has a different name value than it's corresponding row in the other map.
    */
   private boolean doRowsValuesMatch(Map<String, List<XComponent>> oldMap, Map<String, List<XComponent>> newMap) {
      Iterator it = oldMap.keySet().iterator();
      while (it.hasNext()) {
         String activityName = (String) it.next();
         List<XComponent> oldSuccessors = oldMap.get(activityName);
         List<XComponent> newSuccessors = newMap.get(activityName);
         for (int i = 0; i < oldSuccessors.size(); i++) {
            XComponent oldDataRow = oldSuccessors.get(i);
            XComponent newDataRow = newSuccessors.get(i);
            if (!OpGanttValidator.getName(oldDataRow).equals(OpGanttValidator.getName(newDataRow))) {
               return false;
            }
         }
      }
      return true;
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
