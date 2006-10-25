/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpPermission;
import onepoint.resource.XLocalizer;

import java.util.Iterator;

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

   public static void retrieveResourceDataSet(OpProjectSession session, OpBroker broker, XComponent dataSet) {
      retrieveResourceDataSet(session, broker, dataSet, null, null);
   }

   public static void retrieveResourceDataSet(OpProjectSession session, OpBroker broker, XComponent dataSet,
        int[] poolColumnsSelector, int[] resourceColumnsSelector) {
      // Localizer is used in order to localize name and description of root resource pool
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(RESOURCE_OBJECTS));
      addSubPoolRows(session, broker, dataSet, localizer, -1, 0, poolColumnsSelector, resourceColumnsSelector);
   }

   private static void addSubPoolRows(OpProjectSession session, OpBroker broker, XComponent dataSet,
        XLocalizer localizer, long poolId, int outlineLevel, int[] poolColumnsSelector, int[] resourceColumnsSelector) {

      OpQuery query = null;
      if (poolId == -1) {
         query = broker.newQuery("select pool.ID from OpResourcePool as pool where pool.SuperPool.ID is null");
      }
      else {
         query = broker.newQuery("select pool.ID from OpResourcePool as pool where pool.SuperPool.ID = ?");
         query.setLong(0, poolId);
      }

      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpResourcePool.RESOURCE_POOL, OpResourcePool.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator subPools = session.accessibleObjects(broker, broker.list(query), OpPermission.OBSERVER, order);
      OpResourcePool subPool = null;
      XComponent dataRow = null;
      XComponent dataCell = null;
      int i = 0;
      while (subPools.hasNext()) {
         subPool = (OpResourcePool) (subPools.next());
         dataRow = new XComponent(XComponent.DATA_ROW);
         // Expand first level automatically (root resource pool)
         dataRow.setOutlineLevel(outlineLevel);
         if (outlineLevel == 0) {
            dataRow.setExpanded(true);
         }
         if (poolColumnsSelector != null) {
            dataRow.setStringValue(subPool.locator());
            // Add data cells
            for (i = 0; i < poolColumnsSelector.length; i++) {
               dataCell = new XComponent(XComponent.DATA_CELL);
               switch (poolColumnsSelector[i]) {
                  case NULL: {
                     break;
                  }
                  case ID: {
                     dataCell.setLongValue(subPool.getID());
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
                     dataCell.setByteValue(session.effectiveAccessLevel(broker, subPool.getID()));
                     break;
                  }
               }
               dataRow.addChild(dataCell);
            }
         }
         else {
            dataRow.setStringValue(XValidator.choice(subPool.locator(), localizer.localize(subPool.getName()),
                 POOL_ICON_INDEX));
         }
         dataSet.addChild(dataRow);
         // Add sub-pools of this pool
         addSubPoolRows(session, broker, dataSet, localizer, subPool.getID(), outlineLevel + 1, poolColumnsSelector,
              resourceColumnsSelector);
         // Add resources of this pool
         addResourceRows(session, broker, dataSet, localizer, subPool.getID(), outlineLevel + 1,
              resourceColumnsSelector);
      }
   }

   private static void addResourceRows(OpProjectSession session, OpBroker broker, XComponent data_set,
        XLocalizer localizer, long poolId, int outline_level, int[] columnsSelector) {

      OpQuery query = null;
      if (poolId == -1) {
         query = broker.newQuery("select resource.ID from OpResource as resource where resource.Pool.ID is null");
      }
      else {
         query = broker.newQuery("select resource.ID from OpResource as resource where resource.Pool.ID = ?");
         query.setLong(0, poolId);
      }

      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpResource.RESOURCE, OpResource.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator resources = session.accessibleObjects(broker, broker.list(query), OpPermission.OBSERVER, order);
      OpResource resource = null;
      XComponent dataRow = null;
      XComponent dataCell = null;
      int i = 0;
      while (resources.hasNext()) {
         resource = (OpResource) (resources.next());
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setOutlineLevel(outline_level);
         if (columnsSelector != null) {
            dataRow.setStringValue(resource.locator());
            // Add data cells
            for (i = 0; i < columnsSelector.length; i++) {
               dataCell = new XComponent(XComponent.DATA_CELL);
               switch (columnsSelector[i]) {
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
               dataRow.addChild(dataCell);
            }
         }
         else {
            dataRow.setStringValue(XValidator.choice(resource.locator(), localizer.localize(resource.getName()),
                 RESOURCE_ICON_INDEX));
         }
         data_set.addChild(dataRow);
      }
   }
}
