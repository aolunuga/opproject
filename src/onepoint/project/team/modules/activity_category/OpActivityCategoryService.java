/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.activity_category;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivityCategory;
import onepoint.service.XError;
import onepoint.service.XMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class OpActivityCategoryService extends OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpActivityCategoryService.class, true);

   public final static String EDIT_MODE = "edit_mode";

   public final static String CATEGORY_DATA = "category_data";
   public final static String CATEGORY_ID = "category_id";
   public final static String CATEGORY_IDS = "category_ids";

   public final static OpActivityCategoryErrorMap ERROR_MAP = new OpActivityCategoryErrorMap();

   public XMessage insertCategory(OpProjectSession session, XMessage request) {
      logger.debug("OpActivityCategoryService.insertCategory()");

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpActivityCategoryError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      HashMap category_data = (HashMap) (request.getArgument(CATEGORY_DATA));

      OpActivityCategory category = new OpActivityCategory();
      category.setName((String) (category_data.get(OpActivityCategory.NAME)));
      category.setDescription((String) (category_data.get(OpActivityCategory.DESCRIPTION)));
      category.setColor(((Integer) (category_data.get(OpActivityCategory.COLOR))).intValue());

      XMessage reply = new XMessage();

      // check mandatory input fields
      if (category.getName() == null || category.getName().length() == 0) {
         reply.setError(session.newError(ERROR_MAP, OpActivityCategoryError.CATEGORY_NAME_NOT_SPECIFIED));
         return reply;
      }

      OpBroker broker = session.newBroker();

      // check if category name is already used
      OpQuery query = broker
           .newQuery("select category.ID from OpActivityCategory as category where category.Name = :categoryName");
      query.setString("categoryName", category.getName());
      Iterator categoryIds = broker.iterate(query);
      if (categoryIds.hasNext()) {
         reply.setError(session.newError(ERROR_MAP, OpActivityCategoryError.CATEGORY_NAME_NOT_UNIQUE));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();
      broker.makePersistent(category);

      t.commit();
      logger.debug("/OpActivityCategoryService.insertCategory()");
      broker.close();
      return reply;
   }

   public XMessage updateCategory(OpProjectSession session, XMessage request) {
      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpActivityCategoryError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      String id_string = (String) (request.getArgument(CATEGORY_ID));
      logger.debug("OpActivityCategoryService.updateCategory(): id = " + id_string);
      HashMap category_data = (HashMap) (request.getArgument(CATEGORY_DATA));

      OpBroker broker = session.newBroker();
      XMessage reply = new XMessage();

      OpActivityCategory category = (OpActivityCategory) (broker.getObject(id_string));
      if (category == null) {
         logger.warn("ERROR: Could not find object with ID " + id_string);
         reply.setError(session.newError(ERROR_MAP, OpActivityCategoryError.CATEGORY_NOT_FOUND));
         broker.close();
         return reply;
      }

      String categoryName = (String) (category_data.get(OpActivityCategory.NAME));

      // check mandatory input fields
      if (categoryName == null || categoryName.length() == 0) {
         reply.setError(session.newError(ERROR_MAP, OpActivityCategoryError.CATEGORY_NAME_NOT_SPECIFIED));
         broker.close();
         return reply;
      }

      // check if category name is already used
      OpQuery query = broker
           .newQuery("select category from OpActivityCategory as category where category.Name = :categoryName");
      query.setString("categoryName", categoryName);
      Iterator categories = broker.iterate(query);
      while (categories.hasNext()) {
         OpActivityCategory other = (OpActivityCategory) categories.next();
         if (other.getID() != category.getID()) {
            reply.setError(session.newError(ERROR_MAP, OpActivityCategoryError.CATEGORY_NAME_NOT_UNIQUE));
            broker.close();
            return reply;
         }
      }

      category.setName(categoryName);
      category.setDescription((String) (category_data.get(OpActivityCategory.DESCRIPTION)));
      category.setColor(((Integer) (category_data.get(OpActivityCategory.COLOR))).intValue());

      OpTransaction t = broker.newTransaction();

      broker.updateObject(category);

      t.commit();
      logger.debug("/OpActivityCategoryService.updateCategory()");
      broker.close();
      return reply;
   }

   public XMessage deleteCategories(OpProjectSession session, XMessage request) {

      if (!session.userIsAdministrator()) {
         XMessage reply = new XMessage();
         XError error = session.newError(ERROR_MAP, OpActivityCategoryError.INSUFFICIENT_PRIVILEGES);
         reply.setError(error);
         return reply;
      }

      ArrayList id_strings = (ArrayList) (request.getArgument(CATEGORY_IDS));

      logger.debug("OpActivityCategoryService.deleteCategories(): category_ids = " + id_strings);

      OpBroker broker = ((OpProjectSession) session).newBroker();

      ArrayList categoryIds = new ArrayList();
      int i = 0;
      for (i = 0; i < id_strings.size(); i++) {
         categoryIds.add(new Long(OpLocator.parseLocator((String) id_strings.get(i)).getID()));
      }

      OpTransaction t = broker.newTransaction();

      /*
       * --- Not yet support by Hibernate (delete query against joined-subclass) query = broker.newQuery("delete from
       * OpActivityCategory where OpActivityCategory.ID in (:categoryIds)"; broker.execute(query);
       */
      OpQuery query = broker
           .newQuery("select category from OpActivityCategory as category where category.ID in (:categoryIds)");
      query.setCollection("categoryIds", categoryIds);
      Iterator result = broker.iterate(query);
      while (result.hasNext()) {
         OpActivityCategory category = (OpActivityCategory) result.next();
         if (category.getActivities().size() > 0 || category.getActivityVersions().size() > 0) {
            if (category.getActive()) {
               category.setActive(false);
               broker.updateObject(category);
            }
         }
         else {
            broker.deleteObject(category);
         }
      }

      t.commit();

      logger.debug("/OpActivityCategoryService.deleteCategories()");

      broker.close();

      return null;
   }

}
