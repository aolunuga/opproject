/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocalizer;

import java.util.*;

public final class OpResourceDataSetFactory {

   public final static String POOL_DESCRIPTOR = "p";
   public final static String RESOURCE_DESCRIPTOR = "r";

   public final static int POOL_ICON_INDEX = 0;
   public final static int RESOURCE_ICON_INDEX = 1;

   // Constants for column selector
   public final static int NULL = 0;
   public final static int ID = 1;
   public final static int DESCRIPTOR = 2;
   public final static int NAME = 3;
   public final static int DESCRIPTION = 4;
   public final static int AVAILABLE = 5;
   public final static int INHERIT_POOL_RATE = 6;
   public final static int HOURLY_RATE = 7;
   public final static int EFFECTIVE_PERMISSIONS = 8;

   // I18n map for hard-wired resource objects (root resource pool)
   public final static String RESOURCE_OBJECTS = "resource.objects";

   /**
    * Utility class.
    */
   private OpResourceDataSetFactory() {
   }

   /**
    * Retrieves the sub-resources/pools for the given pool id and sets them as rows in the given data set with the
    * specifird outline level
    *
    * @param session
    * @param dataSet
    * @param poolColumnsSelector
    * @param resourceColumnsSelector
    * @param poolId                  Parent pool
    * @param childrenOutlineLevel    Outline level of the sub-rows
    * @param filteredLocators        List of locators to be removed from the final result
    */
   public static void retrieveResourceDataSet(OpProjectSession session, XComponent dataSet, Map poolColumnsSelector, Map resourceColumnsSelector, long poolId, int childrenOutlineLevel, List filteredLocators) {
      // Localizer is used in order to localize name and description of root resource pool
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(RESOURCE_OBJECTS));
      addSubPoolRows(session, dataSet, localizer, poolId, childrenOutlineLevel, poolColumnsSelector, resourceColumnsSelector, filteredLocators);
   }

   /**
    * Retrieves the first two levels of pools and resources.
    *
    * @param session
    * @param dataSet
    * @param poolColumnsSelector
    * @param resourceColumnsSelector
    * @param filteredLocators
    */
   public static void retrieveFirstLevelsResourceDataSet(OpProjectSession session, XComponent dataSet, Map poolColumnsSelector, Map resourceColumnsSelector, List filteredLocators) {
      // Localizer is used in order to localize name and description of root resource pool
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(RESOURCE_OBJECTS));

      //retrieve first level root pool
      addSubPoolRows(session, dataSet, localizer, -1, 0, poolColumnsSelector, resourceColumnsSelector, filteredLocators);

      XComponent resultDataSet = new XComponent(XComponent.DATA_SET);
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent row = (XComponent) dataSet.getChild(i);
         if (row.getOutlineLevel() == 0) {
            int size = row.getSubRows().size();
            resultDataSet.addChild(row);
            if (size > 0) {
               row.setExpanded(true);
               XComponent dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setBooleanValue(true);
               row.addChild(dataCell);
               String locatorString = row.getStringValue();
               OpLocator locator = OpLocator.parseLocator((String) (locatorString));
               //add level 1 rows
               addSubPoolRows(session, resultDataSet, localizer, locator.getID(), 1, poolColumnsSelector, resourceColumnsSelector, filteredLocators);
            }
         }
      }
      //set the result
      dataSet.removeAllChildren();
      for (int i = 0; i < resultDataSet.getChildCount(); i++) {
         dataSet.addChild(resultDataSet.getChild(i));
      }
   }


   /**
    * @param session
    * @param dataSet
    * @param localizer
    * @param poolId
    * @param childrenOutlineLevel    Outline level of the sub-rows.
    * @param poolColumnsSelector
    * @param resourceColumnsSelector
    * @param filteredLocators        List of locators to be filtered out by the factory method
    */
   private static void addSubPoolRows(OpProjectSession session, XComponent dataSet, XLocalizer localizer, long poolId,
        int childrenOutlineLevel, Map poolColumnsSelector, Map resourceColumnsSelector, List filteredLocators) {

      OpBroker broker = session.newBroker();
      OpQuery query = null;
      if (poolId == -1) {
         String queryString = "select pool.ID, count(pools.ID)+count(resources.ID) from OpResourcePool as pool " +
              "left join pool.SubPools pools " +
              "left join pool.Resources resources " +
              "where pool.SuperPool.ID is null " +
              "group by pool.ID";
         query = broker.newQuery(queryString);
      }
      else {
         String queryString = "select pool.ID, count(pools.ID)+count(resources.ID) from OpResourcePool as pool " +
              "left join pool.SubPools pools " +
              "left join pool.Resources resources " +
              "where pool.SuperPool.ID = ?" +
              "group by pool.ID";
         query = broker.newQuery(queryString);
         query.setLong(0, poolId);
      }

      Map subEntityMap = new HashMap();
      List poolIds = new ArrayList();
      List results = broker.list(query);
      for (int i = 0; i < results.size(); i++) {
         Object result[] = (Object[]) results.get(i);
         Long id = (Long) result[0];
         poolIds.add(id);
         Integer subEntities = (Integer) result[1];
         subEntityMap.put(id, subEntities);
      }

      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpResourcePool.RESOURCE_POOL, OpResourcePool.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator subPools = session.accessibleObjects(broker, poolIds, OpPermission.OBSERVER, order);
      OpResourcePool subPool = null;
      XComponent dataRow = null;
      XComponent dataCell = null;

      while (subPools.hasNext()) {
         subPool = (OpResourcePool) (subPools.next());
         long id = subPool.getID();
         String locator = subPool.locator();
         if (filteredLocators != null && filteredLocators.contains(locator)) {
            continue;
         }
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setOutlineLevel(childrenOutlineLevel);
         if (poolColumnsSelector != null) {
            dataRow.setStringValue(subPool.locator());
            // Add data cells

            Set keySet = poolColumnsSelector.keySet();
            Integer max = (Integer) Collections.max(keySet);
            for (int i = 0; i <= max.intValue(); i++) {
               dataRow.addChild(new XComponent(XComponent.DATA_CELL));
            }

            for (Iterator iterator = keySet.iterator(); iterator.hasNext();) {
               Integer index = (Integer) iterator.next();
               dataCell = (XComponent) dataRow.getChild(index.intValue());
               int selector = ((Integer) poolColumnsSelector.get(index)).intValue();
               switch (selector) {
                  case NULL: {
                     break;
                  }
                  case ID: {
                     dataCell.setLongValue(id);
                     break;
                  }
                  case DESCRIPTOR: {
                     dataCell.setStringValue(POOL_DESCRIPTOR);
                     break;
                  }
                  case NAME: {
                     dataCell.setStringValue(XValidator.choice(subPool.locator(),
                          localizer.localize(subPool.getName()), POOL_ICON_INDEX));
                     break;
                  }
                  case DESCRIPTION: {
                     dataCell.setStringValue(localizer.localize(subPool.getDescription()));
                     break;
                  }
                  case HOURLY_RATE: {
                     dataCell.setDoubleValue(subPool.getHourlyRate());
                     break;
                  }
                  case EFFECTIVE_PERMISSIONS: {
                     dataCell.setByteValue(session.effectiveAccessLevel(broker, id));
                     break;
                  }
               }
            }
         }
         else {
            dataRow.setStringValue(XValidator.choice(subPool.locator(),
                 localizer.localize(subPool.getName()), POOL_ICON_INDEX));
         }
         dataSet.addChild(dataRow);

         Integer subCount = ((Integer) subEntityMap.get(new Long(id)));
         if (subCount != null && subCount.intValue() > 0 && !allChildrenFiltered(subPool, filteredLocators)) {
            //add dummy child
            XComponent dummyRow = new XComponent(XComponent.DATA_SET);
            dummyRow.setStringValue(OpProjectConstants.DUMMY_ROW_ID);
            dummyRow.setOutlineLevel(childrenOutlineLevel + 1);
            dummyRow.setVisible(false);
            dummyRow.setFiltered(true);
            dataSet.addChild(dummyRow);
            dataRow.setExpanded(false);
            dummyRow.setSelectable(false);
         }
      }

      // Add resources of this pool
      addResourceRows(session, broker, dataSet, localizer, poolId, childrenOutlineLevel, resourceColumnsSelector, filteredLocators);

      broker.close();
   }

   private static boolean allChildrenFiltered(OpResourcePool subPool, List filteredLocators) {

      if (filteredLocators == null || filteredLocators.isEmpty()) {
         return false;
      }

      Set pools = subPool.getSubPools();
      for (Iterator iterator = pools.iterator(); iterator.hasNext();) {
         OpResourcePool pool = (OpResourcePool) iterator.next();
         if (!filteredLocators.contains(pool.locator())) {
            return false;
         }
      }

      Set resources = subPool.getResources();
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         OpResource resource = (OpResource) iterator.next();
         if (!filteredLocators.contains(resource.locator())) {
            return false;
         }
      }

      return true;
   }

   private static void addResourceRows(OpProjectSession session, OpBroker broker, XComponent data_set,
        XLocalizer localizer, long poolId, int outline_level, Map columnsSelector, List filteredLocators) {

      OpQuery query;
      if (poolId == -1) {
         query = broker.newQuery("select resource.ID from OpResource as resource where resource.Pool.ID is null");
      }
      else {
         query = broker.newQuery("select resource.ID from OpResource as resource where resource.Pool.ID = ?");
         query.setLong(0, poolId);
      }

      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpResource.RESOURCE, OpResource.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator resources = session.accessibleObjects(broker, broker.list(query), OpPermission.OBSERVER, order);
      OpResource resource;
      XComponent dataRow;
      XComponent dataCell;
      while (resources.hasNext()) {
         resource = (OpResource) (resources.next());
         if (filteredLocators != null && filteredLocators.contains(resource.locator())) {
            continue;
         }
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setOutlineLevel(outline_level);
         if (columnsSelector != null) {
            dataRow.setStringValue(resource.locator());

            Set keySet = columnsSelector.keySet();
            Integer max = (Integer) Collections.max(keySet);
            for (int i = 0; i <= max.intValue(); i++) {
               dataRow.addChild(new XComponent(XComponent.DATA_CELL));
            }

            for (Iterator iterator = keySet.iterator(); iterator.hasNext();) {
               Integer index = (Integer) iterator.next();
               dataCell = (XComponent) dataRow.getChild(index.intValue());
               int selector = ((Integer) columnsSelector.get(index)).intValue();
               switch (selector) {
                  case NULL: {
                     break;
                  }
                  case ID: {
                     dataCell.setLongValue(resource.getID());
                     break;
                  }
                  case DESCRIPTOR: {
                     dataCell.setStringValue(RESOURCE_DESCRIPTOR);
                     break;
                  }
                  case NAME: {
                     dataCell.setStringValue(XValidator.choice(resource.locator(), localizer.localize(resource
                          .getName()), RESOURCE_ICON_INDEX));
                     break;
                  }
                  case DESCRIPTION: {
                     dataCell.setStringValue(localizer.localize(resource.getDescription()));
                     break;
                  }
                  case AVAILABLE: {
                     dataCell.setDoubleValue(resource.getAvailable());
                     break;
                  }
                  case INHERIT_POOL_RATE: {
                     dataCell.setBooleanValue(resource.getInheritPoolRate());
                     break;
                  }
                  case HOURLY_RATE: {
                     dataCell.setDoubleValue(resource.getHourlyRate());
                     break;
                  }
                  case EFFECTIVE_PERMISSIONS: {
                     dataCell.setByteValue(session.effectiveAccessLevel(broker, resource.getID()));
                     break;
                  }
               }
            }
         }
         else {
            dataRow.setStringValue(XValidator.choice(resource.locator(), localizer.localize(resource.getName()),
                 RESOURCE_ICON_INDEX));
         }
         data_set.addChild(dataRow);
      }
   }


   /**
    * Makes item selectable in the resources data set, based on the value of the given parameters.
    *
    * @param dataSet       a <code>XComponent(DATA_SET)</code> representing the resources set.
    * @param showResources a <code>boolean</code> indicating whether to make resources selectable or not.
    * @param showPools     a <code>boolean</code> indicating whether to make pools selectable or not.
    */
   public static void enableResourcesSet(XComponent dataSet, boolean showResources, boolean showPools) {
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         if (!OpProjectConstants.DUMMY_ROW_ID.equals(dataRow.getStringValue())) {
            String id = XValidator.choiceID(dataRow.getStringValue());
            OpLocator locator = OpLocator.parseLocator(id);
            Class prototypeClass = locator.getPrototype().getInstanceClass();
            if (prototypeClass.equals(OpResource.class)) {
               dataRow.setSelectable(showResources);
            }
            else if (prototypeClass.equals(OpResourcePool.class)) {
               dataRow.setSelectable(showPools);
            }
         }
      }
   }

   /**
    * Fills the given data set with "read only resources" (access level < MANAGER).
    * Each row has a string value set on it - choice(resource.locator, name)
    *
    * @param broker            Broker to use for db access.
    * @param session           Current session.
    * @param readOnlyResources DataSet to fill up.
    */
   public static void fillReadOnlyResources(OpBroker broker, OpProjectSession session, XComponent readOnlyResources) {
      OpQuery query = broker.newQuery("select resource.ID from OpResource as resource ");
      List resourceIds = broker.list(query);
      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpResource.RESOURCE, OpResource.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator managerResourcesIt = session.accessibleObjects(broker, resourceIds, OpPermission.MANAGER, order);
      List managerResources = new ArrayList();
      while (managerResourcesIt.hasNext()) {
         OpResource resource = (OpResource) managerResourcesIt.next();
         managerResources.add(resource);
      }
      Iterator allResources = session.accessibleObjects(broker, resourceIds, OpPermission.OBSERVER, order);
      while (allResources.hasNext()) {
         OpResource resource = (OpResource) allResources.next();
         if (!managerResources.contains(resource)) {
            XComponent dataRow = new XComponent(XComponent.DATA_ROW);
            dataRow.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
            readOnlyResources.addChild(dataRow);
         }
      }
   }
}
