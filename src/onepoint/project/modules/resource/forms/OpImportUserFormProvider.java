/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.*;

public class OpImportUserFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getServerLogger(OpImportUserFormProvider.class);

   private final static String USER_DATA_SET = "UserDataSet";
   private final static String POOL_ID = "pool_id";
   private final static String POOL_DATA_SET = "PoolDataSet";
   private final static String POOL_LIST = "PoolList";
   private final static String HOURLY_RATE = "HourlyRate";
   private final static String EXTERNAL_RATE = "ExternalRate";
   private final static String POOLS_RATES  = "PoolsRates";
   private final static int HOURLY_RATE_LIST_INDEX = 0;
   private final static int EXTERNAL_RATE_LIST_INDEX = 1;

   /**
    *  @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      logger.debug("OpImportUserFormProvider.prepareForm()");

      OpBroker broker = ((OpProjectSession) session).newBroker();

      //Localizer is used to localize name of root resource pool and administrator
      XLocalizer localizer = new XLocalizer();

      //fill resources
      localizer.setResourceMap(((OpProjectSession) session).getLocale().getResourceMap(OpResourceDataSetFactory.RESOURCE_OBJECTS));
      OpResourcePool selectedPool = getSelectedPool(parameters, broker);
      this.fillPoolDataSet(form, broker, localizer, selectedPool.getID());

      //fill users
      localizer.setResourceMap(((OpProjectSession) session).getLocale().getResourceMap(OpPermissionDataSetFactory.USER_OBJECTS));
      this.fillUsersDataSet(form, broker, localizer);

      this.prepareHourlyRateFields(form, selectedPool, broker);
      broker.close();
   }

   /**
    * Prepares the fields which handle the hourly rates.
    * @param form a <code>XComponent(FORM)</code> representing the form being prepared.
    * @param pool the <code>OpResourcePool</code> representing the resource's pool.
    * @param broker
    */
   private void prepareHourlyRateFields(XComponent form, OpResourcePool pool, OpBroker broker) {
      //form the map that has as keys the pools locators and as values lists with the pools rates
      XComponent poolsRates = form.findComponent(POOLS_RATES);
      Map<String, List> poolsRatesMap = new HashMap<String, List>();
      XComponent dataRow;
      List<Double> ratesList;
      XComponent dataSet = form.findComponent(POOL_DATA_SET);
      for(int i = 0; i < dataSet.getChildCount(); i++){
         dataRow = (XComponent)dataSet.getChild(i);
         String locator = XValidator.choiceID(dataRow.getStringValue());
         OpResourcePool currentPool = (OpResourcePool)broker.getObject(locator);
         ratesList = new ArrayList<Double>();
         ratesList.add(HOURLY_RATE_LIST_INDEX, currentPool.getHourlyRate());
         ratesList.add(EXTERNAL_RATE_LIST_INDEX, currentPool.getExternalRate());
         poolsRatesMap.put(dataRow.getStringValue(), ratesList);
      }

      poolsRates.setValue(poolsRatesMap);
      XComponent hourlyRateField = form.findComponent(HOURLY_RATE);
      hourlyRateField.setDoubleValue(pool.getHourlyRate());
      XComponent externalRateField = form.findComponent(EXTERNAL_RATE);
      externalRateField.setDoubleValue(pool.getExternalRate());
      hourlyRateField.setEnabled(false);
      externalRateField.setEnabled(false);
   }

   /**
    * Fills the user data-set with the available users from the system.
    * @param form a <code>XComponent(FORM)</code> representing the form being loaded.
    * @param broker a <code>OpBroker</code> used for persistence operations.
    * @param localizer a <code>XLocalizer</code> user for i18n operations.
    */
   private void fillUsersDataSet(XComponent form, OpBroker broker, XLocalizer localizer) {
      XComponent userDataSet = form.findComponent(USER_DATA_SET);
      OpQuery query = broker.newQuery("select user from OpUser as user");
      Iterator users = broker.iterate(query);
      while (users.hasNext()) {
         OpUser user = (OpUser) (users.next());
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(user.locator(), localizer.localize(user.getDisplayName())));
         userDataSet.addChild(dataRow);
      }
      userDataSet.sort();
   }

   /**
    * Gets the pool which was selected on the UI.
    *
    * @param parameters a <code>Map(String, Object)</code> representing the request
    *                   parameters.
    * @param broker     a <code>OpBroker</code> used for persistence operations.
    * @return a <code>OpResourcePool</code> representing the pool which was selected.
    */
   private OpResourcePool getSelectedPool(Map parameters, OpBroker broker) {
      if (parameters != null) {
         String poolId = (String) parameters.get(POOL_ID);
         if (poolId != null) {
            return (OpResourcePool) broker.getObject(poolId);
         }
      }
      return OpResourceService.findRootPool(broker);
      }

   /**
    * Fills the pools data-set.
    *
    * @param form           a <code>XComponent(FORM)</code> representing the import user form.
    * @param broker         a <code>OpBroker</code> used for persistence operations.
    * @param localizer      a <code>XLocalizer</code> used for i18n operations.
    * @param selectedPoolId a <code>long</code> representing the id of the pool which was
    *                       selected.
    */
   private void fillPoolDataSet(XComponent form, OpBroker broker, XLocalizer localizer, long selectedPoolId) {
      XComponent dataSet = form.findComponent(POOL_DATA_SET);
      OpQuery query = broker.newQuery("select pool from OpResourcePool as pool");
      Iterator pools = broker.iterate(query);
      XComponent selectedRow = null;
      while (pools.hasNext()) {
         OpResourcePool pool = (OpResourcePool) (pools.next());
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(pool.locator(), localizer.localize(pool.getName())));
         dataSet.addChild(dataRow);
         if (pool.getID() == selectedPoolId) {
            selectedRow = dataRow;
         }
      }
      dataSet.sort();

      XComponent poolList = form.findComponent(POOL_LIST);
      poolList.setSelectedIndex(new Integer(selectedRow.getIndex()));
      }
   }
