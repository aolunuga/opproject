/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization;

import onepoint.express.XComponent;
import onepoint.persistence.OpLocator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service class for resource utilization module.
 *
 * @author mihai.costin
 */
public class OpResourceUtilizationService extends onepoint.project.OpProjectService {

   public final static String EDIT_MODE = "edit_mode";

   public final static String RESOURCE_DATA = "resource_data";
   public final static String RESOURCE_ID = "resource_id";
   public final static String RESOURCE_IDS = "resource_ids";
   public final static String PROJECTS = "Projects";

   public final static String POOL_DATA = "pool_data";
   public final static String POOL_ID = "pool_id";
   public final static String SUPER_POOL_ID = "super_pool_id";
   public final static String POOL_IDS = "pool_ids";

   public final static String PROJECT_IDS = "project_ids";


   public final static String HAS_ASSIGNMENTS = "Assignments";

   private final static String OUTLINE_LEVEL = "outlineLevel";
   private final static String POOL_LOCATOR = "source_pool_locator";
   private final static String POOL_SELECTOR = "poolColumnsSelector";
   private final static String RESOURCE_SELECTOR = "resourceColumnsSelector";


   public XMessage expandResourcePool(OpProjectSession session, XMessage request) {

      XMessage reply = new XMessage();

      String targetPoolLocator = (String) request.getArgument(POOL_LOCATOR);
      Integer outline = (Integer) (request.getArgument(OUTLINE_LEVEL));
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      Map poolSelector = (Map) request.getArgument(POOL_SELECTOR);
      Map resourceSelector = (Map) request.getArgument(RESOURCE_SELECTOR);

      List resultList;

      if (targetPoolLocator != null && outline != null) {

         OpLocator locator = OpLocator.parseLocator(targetPoolLocator);
         OpResourceDataSetFactory.retrieveResourceDataSet(session, dataSet, poolSelector, resourceSelector, locator.getID(), outline.intValue() + 1, null);
         OpResourceUtilizationDataSetFactory.fillUtilizationValues(session, dataSet, targetPoolLocator);

         resultList = new ArrayList();
         for (int i = 0; i < dataSet.getChildCount(); i++) {
            resultList.add(dataSet.getChild(i));
         }
         reply.setArgument(OpProjectConstants.CHILDREN, resultList);
      }

      return reply;
   }


}
