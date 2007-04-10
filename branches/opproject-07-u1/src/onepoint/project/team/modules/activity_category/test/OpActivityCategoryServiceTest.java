/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.team.modules.activity_category.test;

import onepoint.persistence.OpLocator;
import onepoint.project.modules.project.OpActivityCategory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.UserTestDataFactory;
import onepoint.project.team.modules.activity_category.OpActivityCategoryError;
import onepoint.project.team.modules.activity_category.OpActivityCategoryService;
import onepoint.project.team.test.OpBaseTeamTestCase;
import onepoint.service.XMessage;

import java.util.*;

/**
 * This class test activity category service methods and form providers.
 *
 * @author lucian.furtos
 */
public class OpActivityCategoryServiceTest extends OpBaseTeamTestCase {

   private static final String NAME = "activity_category";
   private static final String NEW_NAME = "new_activity_category";
   private static final String DESCRIPTION = "A test Activity Category";
   private static final String NEW_DESCRIPTION = "The new test Activity Category";

   private static final String DEFAULT_USER = "tester";
   private static final String DEFAULT_PASSWORD = "pass";

   private OpActivityCategoryService service;
   private ActivityCategoryTestDataFactory dataFactory;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();
      service = getActivityCategoryService();
      dataFactory = new ActivityCategoryTestDataFactory(session);
      clean();
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
    * Test the create of an activity category
    *
    * @throws Exception if the tests fails
    */
   public void testCreateActivityCategory()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpActivityCategory.NAME, NAME);
      args.put(OpActivityCategory.DESCRIPTION, DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, args);
      XMessage response = service.insertCategory(session, request);

      assertNoError(response);

      OpActivityCategory category = dataFactory.getActivityCategoryByName(NAME);
      assertNotNull(category);
      assertEquals(DESCRIPTION, category.getDescription());
      assertEquals(7, category.getColor());
   }

   /**
    * Test the create of an activity category with null name
    *
    * @throws Exception if the tests fails
    */
   public void testCreateActivityCategoryNameNull()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpActivityCategory.NAME, null);
      args.put(OpActivityCategory.DESCRIPTION, DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      XMessage response = service.insertCategory(session, request);
      assertError(response, OpActivityCategoryError.CATEGORY_NAME_NOT_SPECIFIED);

      args.put(OpActivityCategory.NAME, "");

      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      response = service.insertCategory(session, request);
      assertError(response, OpActivityCategoryError.CATEGORY_NAME_NOT_SPECIFIED);

   }

   /**
    * Test the create of an activity category with duplicate name
    *
    * @throws Exception if the tests fails
    */
   public void testCreateActivityCategoryDuplicateName()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpActivityCategory.NAME, NAME);
      args.put(OpActivityCategory.DESCRIPTION, DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      XMessage response = service.insertCategory(session, request);
      assertNoError(response);

      args.put(OpActivityCategory.DESCRIPTION, NEW_DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(3));

      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      response = service.insertCategory(session, request);
      assertError(response, OpActivityCategoryError.CATEGORY_NAME_NOT_UNIQUE);
   }

   /**
    * Test the update of an activity category
    *
    * @throws Exception if the tests fails
    */
   public void testUpdateActivityCategory()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpActivityCategory.NAME, NAME);
      args.put(OpActivityCategory.DESCRIPTION, DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      XMessage response = service.insertCategory(session, request);
      assertNoError(response);
      String locator = dataFactory.getActivityCategoryId(NAME);
      assertNotNull(locator);

      args.put(OpActivityCategory.NAME, NEW_NAME);
      args.put(OpActivityCategory.DESCRIPTION, NEW_DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(3));

      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_ID, locator);
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      response = service.updateCategory(session, request);
      assertNoError(response);

      OpActivityCategory category = dataFactory.getActivityCategoryById(locator);
      assertEquals(NEW_NAME, category.getName());
      assertEquals(NEW_DESCRIPTION, category.getDescription());
      assertEquals(3, category.getColor());
   }

   /**
    * Test the update of an unknown activity category
    *
    * @throws Exception if the tests fails
    */
   public void testUpdateUnknownActivityCategory()
        throws Exception {
      XMessage request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_ID, OpLocator.locatorString(OpActivityCategory.ACTIVITY_CATEGORY, 0L));
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap());
      XMessage response = service.updateCategory(session, request);
      assertError(response, OpActivityCategoryError.CATEGORY_NOT_FOUND);
   }

   /**
    * Test the update of an activity category using a wrong name
    *
    * @throws Exception if the tests fails
    */
   public void testUpdateActivityCategoryWrongName()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpActivityCategory.NAME, NAME);
      args.put(OpActivityCategory.DESCRIPTION, DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      XMessage response = service.insertCategory(session, request);
      assertNoError(response);
      String locator = dataFactory.getActivityCategoryId(NAME);
      assertNotNull(locator);

      args.put(OpActivityCategory.NAME, null);
      args.put(OpActivityCategory.DESCRIPTION, NEW_DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(3));

      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_ID, locator);
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      response = service.updateCategory(session, request);
      assertError(response, OpActivityCategoryError.CATEGORY_NAME_NOT_SPECIFIED);

      args.put(OpActivityCategory.NAME, "");

      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_ID, locator);
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      response = service.updateCategory(session, request);
      assertError(response, OpActivityCategoryError.CATEGORY_NAME_NOT_SPECIFIED);
   }

   /**
    * Test the update of an activity category with a duplicate name
    *
    * @throws Exception if the tests fails
    */
   public void testUpdateActivityCategoryDuplicateName()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpActivityCategory.NAME, NAME);
      args.put(OpActivityCategory.DESCRIPTION, DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      XMessage response = service.insertCategory(session, request);
      assertNoError(response);
      String locator = dataFactory.getActivityCategoryId(NAME);
      assertNotNull(locator);

      args.put(OpActivityCategory.NAME, NEW_NAME);
      args.put(OpActivityCategory.DESCRIPTION, NEW_DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(3));

      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      response = service.insertCategory(session, request);
      assertNoError(response);

      args.put(OpActivityCategory.NAME, NEW_NAME);

      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_ID, locator);
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      response = service.updateCategory(session, request);
      assertError(response, OpActivityCategoryError.CATEGORY_NAME_NOT_UNIQUE);
   }

   /**
    * Test the delete of activity categories
    *
    * @throws Exception if the tests fails
    */
   public void testDeleteActivityCategory()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpActivityCategory.NAME, NAME);
      args.put(OpActivityCategory.DESCRIPTION, DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      XMessage response = service.insertCategory(session, request);
      assertNoError(response);
      String locator1 = dataFactory.getActivityCategoryId(NAME);
      assertNotNull(locator1);

      args.put(OpActivityCategory.NAME, NEW_NAME);
      args.put(OpActivityCategory.DESCRIPTION, NEW_DESCRIPTION);
      args.put(OpActivityCategory.COLOR, new Integer(3));

      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap(args));
      response = service.insertCategory(session, request);
      assertNoError(response);
      String locator2 = dataFactory.getActivityCategoryId(NEW_NAME);
      assertNotNull(locator2);

      dataFactory.createActivity("activity", locator1);

      ArrayList ids = new ArrayList();
      ids.add(locator1);
      ids.add(locator2);
      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_IDS, ids);
      service.deleteCategories(session, request);

      OpActivityCategory category = dataFactory.getActivityCategoryById(locator1);
      assertNotNull(category);
      assertEquals(false, category.getActive());

      category = dataFactory.getActivityCategoryById(locator2);
      assertNull(category);
   }

   /**
    * Test the activity categories service permissions
    *
    * @throws Exception if the tests fails
    */
   public void testActivityCategoryPermisions()
        throws Exception {
      Map userData = UserTestDataFactory.createUserData(DEFAULT_USER, DEFAULT_PASSWORD, OpUser.STANDARD_USER_LEVEL);
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = getUserService().insertUser(session, request);
      assertNoError(response);
      logIn(DEFAULT_USER, DEFAULT_PASSWORD);

      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap());
      response = service.insertCategory(session, request);
      assertError(response, OpActivityCategoryError.INSUFFICIENT_PRIVILEGES);

      request = new XMessage();
      request.setArgument(OpActivityCategoryService.CATEGORY_ID, "locator");
      request.setArgument(OpActivityCategoryService.CATEGORY_DATA, new HashMap());
      response = service.updateCategory(session, request);
      assertError(response, OpActivityCategoryError.INSUFFICIENT_PRIVILEGES);

      request = new XMessage();
      response.setArgument(OpActivityCategoryService.CATEGORY_IDS, new ArrayList());
      service.deleteCategories(session, request);
      assertError(response, OpActivityCategoryError.INSUFFICIENT_PRIVILEGES);
   }

//                             ***** Helper Methods *****

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {
      logOut();
      logIn();
      UserTestDataFactory usrData = new UserTestDataFactory(session);
      OpUser user = usrData.getUserByName(DEFAULT_USER);
      if (user != null) {
         List ids = new ArrayList();
         ids.add(user.locator());
         XMessage request = new XMessage();
         request.setArgument(OpUserService.SUBJECT_IDS, ids);
         getUserService().deleteSubjects(session, request);
      }

      dataFactory.deleteAllActivities();

      List categoryList = dataFactory.getAllActivityCategories();
      for (Iterator iterator = categoryList.iterator(); iterator.hasNext();) {
         OpActivityCategory category = (OpActivityCategory) iterator.next();
         dataFactory.deleteObject(category);
      }
   }
}
