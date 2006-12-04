/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpLocator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class for resource utilization module.
 *
 * @author mihai.costin
 */
public class OpResourceUtilizationService extends onepoint.project.OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpResourceUtilizationService.class, true);

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


   public XMessage expandResourcePool(XSession s, XMessage request) {

      XMessage reply = new XMessage();
      OpProjectSession session = (OpProjectSession) s;

      String targetPoolLocator = (String) request.getArgument(POOL_LOCATOR);
      Integer outline = (Integer) (request.getArgument(OUTLINE_LEVEL));
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      List poolSelector = (List) request.getArgument(POOL_SELECTOR);
      List resourceSelector = (List) request.getArgument(RESOURCE_SELECTOR);

      List resultList = null;

      if (targetPoolLocator != null && outline != null) {

         OpLocator locator = OpLocator.parseLocator((String) (targetPoolLocator));
         OpResourceDataSetFactory.retrieveResourceDataSet(session, dataSet, poolSelector, resourceSelector, locator.getID(), outline.intValue() + 1, null);

         OpResourceUtilizationDataSetFactory.calculateUtilizationValues(dataSet, session);

         resultList = new ArrayList();
         for (int i = 0; i < dataSet.getChildCount(); i++) {
            resultList.add(dataSet.getChild(i));
         }
         reply.setArgument(OpProjectConstants.CHILDREN, resultList);
      }

      return reply;
   }


}
