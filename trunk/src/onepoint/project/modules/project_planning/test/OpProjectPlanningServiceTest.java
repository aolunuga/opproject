/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project_planning.test;

import onepoint.express.XComponent;
import onepoint.persistence.*;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project.test.ProjectTestDataFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningError;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.ResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.UserTestDataFactory;
import onepoint.project.test.OpBaseTestCase;
import onepoint.service.XMessage;
import onepoint.util.XEncodingHelper;
import onepoint.util.XEnvironmentManager;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Date;
import java.util.*;

/**
 * This class test project service methods.
 *
 * @author lucian.furtos
 */
public class OpProjectPlanningServiceTest extends OpBaseTestCase {

   private static final String DEFAULT_USER = "tester";
   private static final String DEFAULT_PASSWORD = "pass";

   private OpProjectPlanningService service;
   private ProjectPlanningTestDataFactory dataFactory;
   private ProjectTestDataFactory projectDataFactory;
   private ResourceTestDataFactory resourceDataFactory;
   private UserTestDataFactory userDataFactory;

   private String resId;
   private String projId;
   private String planId;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      service = getProjectPlanningService();
      dataFactory = new ProjectPlanningTestDataFactory(session);
      projectDataFactory = new ProjectTestDataFactory(session);
      resourceDataFactory = new ResourceTestDataFactory(session);
      userDataFactory = new UserTestDataFactory(session);

      clean();

      Map userData = UserTestDataFactory.createUserData(DEFAULT_USER, DEFAULT_PASSWORD, null, OpUser.STANDARD_USER_LEVEL,
           "John", "Doe", "en", "user@email.com", null, null, null, null);
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = getUserService().insertUser(session, request);
      assertNoError(response);

      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      request = resourceDataFactory.createResourceMsg("resource", "description", 50d, 2d, false, poolid);
      response = getResourceService().insertResource(session, request);
      assertNoError(response);
      resId = resourceDataFactory.getResourceByName("resource").locator();

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpResource res = (OpResource) broker.getObject(resId);
      res.setUser(userDataFactory.getUserByName(DEFAULT_USER));
      broker.updateObject(res);
      t.commit();
      broker.close();

      request = ProjectTestDataFactory.createProjectMsg("project", new Date(1), 1d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);
      projId = projectDataFactory.getProjectId("project");

      planId = projectDataFactory.getProjectById(projId).getPlan().locator();

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

   public void testImportExportActivities()
        throws Exception {
      long date = System.currentTimeMillis();
      OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpResource resource = resourceDataFactory.getResourceById(resId);
      OpProjectNode project = projectDataFactory.getProjectById(projId);

      OpActivity activity = new OpActivity(OpActivity.COLLECTION_TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 1000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      broker.makePersistent(activity);

      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resource);
      assignment.setProjectPlan(plan);
      broker.makePersistent(assignment);

      OpProjectNodeAssignment projectAssignment = new OpProjectNodeAssignment();
      projectAssignment.setResource(resource);
      projectAssignment.setProjectNode(project);
      broker.makePersistent(projectAssignment);

      activity = new OpActivity(OpActivity.SCHEDULED_TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 5000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      activity.setAssignments(new HashSet());
      broker.makePersistent(activity);

      assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resource);
      assignment.setProjectPlan(plan);
      broker.makePersistent(assignment);

      t.commit();
      broker.close();

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.setValidatorClass(OpGanttValidator.class.getName());
      broker = session.newBroker();
      OpActivityDataSetFactory.retrieveActivityDataSet(session.newBroker(), plan, dataSet, false);
      broker.close();
      assertEquals(2, dataSet.getChildCount());

      String fileName = "msproject.test";
      XMessage request = ProjectPlanningTestDataFactory.exportActivitiesMsg(dataSet, fileName);
      XMessage response = service.exportActivities(session, request);
      assertNoError(response);
      String actualFile = (String) response.getArgument("file_name");
      assertEquals("msproject.mpx", actualFile);
      byte[] bytes = (byte[]) response.getArgument("bytes_array");
      assertNotNull(bytes);

      deleteAllObjects(OpAssignment.ASSIGNMENT);
      deleteAllObjects(OpActivity.ACTIVITY);

      request = ProjectPlanningTestDataFactory.importActivitiesMsg(projId, Boolean.FALSE, bytes);
      response = service.importActivities(session, request);
      assertNoError(response);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.setValidatorClass(OpGanttValidator.class.getName());
      broker = session.newBroker();
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, plan, dataSet, false);
      broker.close();
      assertEquals(2, dataSet.getChildCount());
   }

   public void testEditActivities()
        throws Exception {
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpProjectPlan plan = (OpProjectPlan) broker.getObject(planId);

      OpActivity activity = new OpActivity();
      activity.setName("A1");
      activity.setProjectPlan(plan);
      broker.makePersistent(activity);

      OpActivity activity1 = new OpActivity();
      activity.setName("A2");
      activity1.setProjectPlan(plan);
      broker.makePersistent(activity1);

      t.commit();
      broker.close();

      XMessage request = new XMessage();
      request.setArgument("project_id", projId);
      XMessage response = service.editActivities(session, request);
      assertNoError(response);
   }

   public void testRevertVersion()
        throws Exception {
      long date = System.currentTimeMillis();
      OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity = new OpActivity(OpActivity.COLLECTION_TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 1000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      broker.makePersistent(activity);

      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId));
      assignment.setProjectPlan(plan);
      assignment.setAssigned(50d);
      broker.makePersistent(assignment);

      activity = new OpActivity(OpActivity.SCHEDULED_TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 5000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      activity.setAssignments(new HashSet());
      broker.makePersistent(activity);

      assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId));
      assignment.setProjectPlan(plan);
      assignment.setAssigned(50d);
      broker.makePersistent(assignment);

      t.commit();

      OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, plan, session.user(broker), 1, true);
      OpProjectPlanVersion planVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, plan, 1);
      assertNotNull(planVersion);

      broker.close();

      XMessage request = ProjectPlanningTestDataFactory.editActivitiesMsg(projId);
      XMessage response = service.editActivities(session, request);
      assertNoError(response);
      request = ProjectPlanningTestDataFactory.revertActivitiesMsg(projId);
      response = service.revertActivities(session, request);
      assertNoError(response);
      broker = session.newBroker();
      planVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, plan, 1);
      broker.close();
      assertNull(planVersion);
   }

   public void testRevertVersionError()
        throws Exception {
      long date = System.currentTimeMillis();
      OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity = new OpActivity(OpActivity.COLLECTION_TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 1000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      broker.makePersistent(activity);

      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId));
      assignment.setProjectPlan(plan);
      assignment.setAssigned(67d);
      assignment.setBaseEffort(25d);
      broker.makePersistent(assignment);

      activity = new OpActivity(OpActivity.SCHEDULED_TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 5000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      activity.setAssignments(new HashSet());
      broker.makePersistent(activity);

      assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId));
      assignment.setProjectPlan(plan);
      assignment.setAssigned(78d);
      assignment.setBaseEffort(65d);
      broker.makePersistent(assignment);

      t.commit();

      OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, plan, session.user(broker), 1, true);
      OpProjectPlanVersion planVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, plan, 1);
      assertNotNull(planVersion);

      broker.close();

      XMessage request = ProjectPlanningTestDataFactory.editActivitiesMsg(projId);
      XMessage response = service.editActivities(session, request);
      assertError(response, OpProjectPlanningError.AVAILIBILITY_AND_RATES_MODIFIED_WARNING);
   }

   public void testCreateTmpFile()
        throws Exception {
      byte[] content = "The content of the file".getBytes();
      Map params = new HashMap();
      params.put("content", content);
      params.put("fileName", "file.tmp");
      XMessage request = new XMessage();
      request.setArgument("parameters", params);
      XMessage response = service.createTemporaryFile(session, request);
      assertNoError(request);
      String url = (String) response.getArgument("attachmentUrl");
      url = XEncodingHelper.decodeValue(url);

      // now check if file really exists.
      String filePath = XEnvironmentManager.TMP_DIR + File.separator + url;

      FileInputStream bis = (FileInputStream) new FileInputStream(filePath);
      byte[] bytes = new byte[content.length];
      assertEquals(content.length, bis.read(bytes));
      assertTrue(Arrays.equals(content, bytes));
   }

   public void testInsertComment()
        throws Exception {
      long date = System.currentTimeMillis();
      OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity = new OpActivity(OpActivity.COLLECTION_TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 1000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      broker.makePersistent(activity);

      t.commit();
      broker.close();

      plan = projectDataFactory.getProjectPlanById(planId);
      Set activities = plan.getActivities();
      assertEquals(1, activities.size());
      activity = (OpActivity) activities.toArray(new OpActivity[1])[0];
      String id = activity.locator();

      XMessage request = ProjectPlanningTestDataFactory.insertCommentMsg(id, "C1", "The body of the comment");
      XMessage response = service.insertComment(session, request);
      assertNoError(response);

      broker = session.newBroker();
      activity = (OpActivity) broker.getObject(id);
      assertEquals(1, activity.getComments().size());
   }

   public void testDeleteComment()
        throws Exception {
      long date = System.currentTimeMillis();
      OpProjectPlan plan = projectDataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity = new OpActivity(OpActivity.COLLECTION_TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(date + 1000));
      activity.setComplete(0d);
      activity.setTemplate(false);
      broker.makePersistent(activity);

      t.commit();
      broker.close();

      plan = projectDataFactory.getProjectPlanById(planId);
      Set activities = plan.getActivities();
      assertEquals(1, activities.size());
      activity = (OpActivity) activities.toArray(new OpActivity[1])[0];
      String id = activity.locator();

      XMessage request = ProjectPlanningTestDataFactory.insertCommentMsg(id, "C1", "The body of the comment");
      XMessage response = service.insertComment(session, request);
      assertNoError(response);
      request = ProjectPlanningTestDataFactory.insertCommentMsg(id, "C2", "The second body of the comment");
      response = service.insertComment(session, request);
      assertNoError(response);

      broker = session.newBroker();
      activity = (OpActivity) broker.getObject(id);
      Set comments = activity.getComments();
      assertEquals(2, comments.size());
      OpActivityComment comment = (OpActivityComment) comments.toArray(new OpActivityComment[2])[0];
      String commentId = comment.locator();
      broker.close();

      request = ProjectPlanningTestDataFactory.deleteCommentMsg(commentId);
      response = service.deleteComment(session, request);
      assertNoError(response);

      broker = session.newBroker();
      activity = (OpActivity) broker.getObject(id);
      comments = activity.getComments();
      assertEquals(1, comments.size());
      comment = (OpActivityComment) comments.toArray(new OpActivityComment[1])[0];
      broker.close();

      request = ProjectPlanningTestDataFactory.deleteCommentMsg(comment.locator());
      response = service.deleteComment(session, request);
      assertNoError(response);

      broker = session.newBroker();
      activity = (OpActivity) broker.getObject(id);
      assertEquals(0, activity.getComments().size());
      broker.close();
   }

   // ******** Helper Methods *********

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {
      UserTestDataFactory usrData = new UserTestDataFactory(session);
      ArrayList ids = new ArrayList();
      List users = usrData.getAllUsers();
      for (Iterator iterator = users.iterator(); iterator.hasNext();) {
         OpUser user = (OpUser) iterator.next();
         if (user.getName().equals(OpUser.ADMINISTRATOR_NAME)) {
            continue;
         }
         ids.add(user.locator());
      }
      XMessage request = new XMessage();
      request.setArgument(OpUserService.SUBJECT_IDS, ids);
      getUserService().deleteSubjects(session, request);

      deleteAllObjects(OpAssignment.ASSIGNMENT);
      deleteAllObjects(OpActivityComment.ACTIVITY_COMMENT);
      deleteAllObjects(OpActivity.ACTIVITY);
      deleteAllObjects(OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(OpAssignmentVersion.ASSIGNMENT_VERSION);
      deleteAllObjects(OpProjectPlanVersion.PROJECT_PLAN_VERSION);
      deleteAllObjects(OpActivityVersion.ACTIVITY_VERSION);

      List projectList = projectDataFactory.getAllProjects();
      for (Iterator iterator = projectList.iterator(); iterator.hasNext();) {
         OpProjectNode project = (OpProjectNode) iterator.next();
         dataFactory.deleteObject(project);
      }

      List resoucesList = resourceDataFactory.getAllResources();
      for (Iterator iterator = resoucesList.iterator(); iterator.hasNext();) {
         OpResource resource = (OpResource) iterator.next();
         resourceDataFactory.deleteObject(resource);
      }

      List poolList = resourceDataFactory.getAllResourcePools();
      for (Iterator iterator = poolList.iterator(); iterator.hasNext();) {
         OpResourcePool pool = (OpResourcePool) iterator.next();
         if (pool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            continue;
         }
         resourceDataFactory.deleteObject(pool);
      }
   }

   private void deleteAllObjects(String prototypeName) {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("from " + prototypeName);
      Iterator it = broker.list(query).iterator();
      broker.close();
      while (it.hasNext()) {
         OpObject object = (OpObject) it.next();
         dataFactory.deleteObject(object);
      }
   }
}
