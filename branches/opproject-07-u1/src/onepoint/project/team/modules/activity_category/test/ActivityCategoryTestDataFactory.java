/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.team.modules.activity_category.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityCategory;
import onepoint.project.test.TestDataFactory;

import java.util.Iterator;
import java.util.List;

/**
 * This class contains helper methods for managing activity category data
 *
 * @author lucian.furtos
 */
public class ActivityCategoryTestDataFactory extends TestDataFactory {

   private final static String SELECT_CATEGORY_ID_BY_NAME_QUERY = "select category.ID from OpActivityCategory as category where category.Name = ?";

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public ActivityCategoryTestDataFactory(OpProjectSession session) {
      super(session);
   }

   /**
    * Get an activity category by the name
    *
    * @param categoryName the activity category name
    * @return an instance of <code>OpActivityCategory</code>
    */
   public OpActivityCategory getActivityCategoryByName(String categoryName) {
      String locator = getActivityCategoryId(categoryName);
      if (locator != null) {
         return getActivityCategoryById(locator);
      }

      return null;
   }

   /**
    * Get an activity category by the locator
    *
    * @param locator the uniq identifier (locator) of an entity
    * @return an instance of <code>OpActivityCategory</code>
    */
   public OpActivityCategory getActivityCategoryById(String locator) {
      OpBroker broker = session.newBroker();

      OpActivityCategory category = (OpActivityCategory) broker.getObject(locator);
      if (category != null) {
         // just to inialize the collection
         category.getActivities().size();
         category.getActivityVersions().size();
         category.getDynamicResources().size();
         category.getLocks().size();
         category.getPermissions().size();
      }
      broker.close();

      return category;
   }

   /**
    * Get the DB identifier of an activity category by name
    *
    * @param categoryName the activity category name
    * @return the uniq identifier of an entity (the locator)
    */
   public String getActivityCategoryId(String categoryName) {
      OpBroker broker = session.newBroker();
      Long categoryId = null;

      OpQuery query = broker.newQuery(SELECT_CATEGORY_ID_BY_NAME_QUERY);
      query.setString(0, categoryName);
      Iterator categoryIt = broker.iterate(query);
      if (categoryIt.hasNext()) {
         categoryId = (Long) categoryIt.next();
      }
      broker.close();

      if (categoryId != null) {
         return OpLocator.locatorString(OpActivityCategory.ACTIVITY_CATEGORY, Long.parseLong(categoryId.toString()));
      }

      return null;
   }

   /**
    * Get all the activity categories
    *
    * @return a <code>List</code> of <code>OpActivityCategory</code>
    */
   public List getAllActivityCategories() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from OpActivityCategory");
      List result = broker.list(query);
      broker.close();

      return result;
   }

//                     ***** Activities *****

   /**
    * Creates a new Activity and links it to am activity category.
    *
    * @param name            the name of the new activity
    * @param categoryLocator the locator of the activity category
    * @return the locator of the new activity or null if not created
    */
   public String createActivity(String name, String categoryLocator) {
      OpActivityCategory category = getActivityCategoryById(categoryLocator);

      OpActivity activity = new OpActivity();
      activity.setCategory(category);
      activity.setName(name);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      broker.makePersistent(activity);
      t.commit();
      OpQuery query = broker.newQuery("from OpActivity as activity where activity.Name = ?");
      query.setString(0, name);
      Iterator activityIt = broker.iterate(query);
      if (activityIt.hasNext()) {
         activity = (OpActivity) activityIt.next();
         return activity.locator();
      }
      broker.close();

      return null;
   }

   /**
    * Delete all the activities
    */
   public void deleteAllActivities() {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("from OpActivity");
      Iterator activityIt = broker.iterate(query);
      OpTransaction t = broker.newTransaction();
      while (activityIt.hasNext()) {
         OpActivity activity = (OpActivity) activityIt.next();
         broker.deleteObject(activity);
      }
      t.commit();
      broker.close();
   }

}
