/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpImportUserFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getLogger(OpImportUserFormProvider.class, true);

   private final static String USER_DATA_SET = "UserDataSet";
   private final static String POOL_ID = "PoolID";
   private final static String POOL_DATA_SET = "PoolDataSet";
   private final static String POOL_LIST = "PoolList";

   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      logger.debug("OpImportUserFormProvider.prepareForm()");

      OpBroker broker = ((OpProjectSession) session).newBroker();
      //Localizer is used to localize name of root resource pool and administrator
      XLocalizer localizer = new XLocalizer();

      OpResourcePool selected_pool = null;
      if (parameters != null) {
         String poolId = (String) parameters.get(POOL_ID);
         if (poolId != null) {
            selected_pool = (OpResourcePool) broker.getObject(poolId);
         }
      }

      if (selected_pool == null) {
         selected_pool = OpResourceService.findRootPool(broker);
      }

      // fill PoolDataSet
      XComponent data_set = form.findComponent(POOL_DATA_SET);
      XComponent pool_list = form.findComponent(POOL_LIST);

      XComponent data_row = null;
      //set up resource map for pools
      localizer.setResourceMap(((OpProjectSession) session).getLocale().getResourceMap(OpResourceDataSetFactory.RESOURCE_OBJECTS));

      OpQuery query = broker.newQuery("select pool from OpResourcePool as pool");
      Iterator pools = broker.iterate(query);
      OpResourcePool pool = null;
      // add all available pools to the data set
      while (pools.hasNext()) {
         pool = (OpResourcePool) (pools.next());
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setStringValue(XValidator.choice(pool.locator(), localizer.localize(pool.getName())));

         if (selected_pool != null && pool.getID() == selected_pool.getID()) {
            data_row.setSelected(true);
            pool_list.setStringValue(data_row.getStringValue());
         }

         data_set.addChild(data_row);
      }
      data_set.sort();

      // FIXME: hack so that the CHOICE_FIELD initially has the correct value
      ((XComponent) pool_list._getChild(0)).setStringValue(pool_list.getStringValue());

      XComponent user_data_set = form.findComponent(USER_DATA_SET);

      //set up resource map for users
      localizer.setResourceMap(((OpProjectSession) session).getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));

      query = broker.newQuery("select user from OpUser as user");
      Iterator users = broker.iterate(query);
      OpUser user = null;
      while (users.hasNext()) {
         user = (OpUser) (users.next());
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setStringValue(XValidator.choice(user.locator(), localizer.localize(user.getDisplayName())));
         user_data_set.addChild(data_row);
      }
      user_data_set.sort();
      broker.close();
   }

}
