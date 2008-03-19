/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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
import java.util.Set;

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
   public final static String PROBABILITY_CHOICE_ID = "probability_choice_id";
   public static final String PROJECT_CHOICE_ID = "project_choice_id";
   private final static String FILTERED_OUT_IDS = "FilteredOutIds";

   public XMessage expandResourcePool(OpProjectSession session, XMessage request) {

      XMessage reply = new XMessage();

      String probabilityStr = (String) (request.getArgument(PROBABILITY_CHOICE_ID));
      Integer probability = 0;
      if (probabilityStr != null) {
         probability = Integer.valueOf(probabilityStr);
      }

      String targetPoolLocator = (String) request.getArgument(POOL_LOCATOR);
      Integer outline = (Integer) (request.getArgument(OUTLINE_LEVEL));
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      Map poolSelector = (Map) request.getArgument(POOL_SELECTOR);
      Map resourceSelector = (Map) request.getArgument(RESOURCE_SELECTOR);
      String projectLocator = (String) (request.getArgument(PROJECT_CHOICE_ID));
      Set<String> filterSet = (Set<String>) request.getArgument(FILTERED_OUT_IDS);
      long projectId = -1;
      if (projectLocator != null) {
         projectId = OpLocator.parseLocator(projectLocator).getID();
      }
      List resultList;

      if (targetPoolLocator != null && outline != null) {

         OpLocator locator = OpLocator.parseLocator(targetPoolLocator);
         OpResourceDataSetFactory.retrieveResourceDataSet(session, dataSet, poolSelector, resourceSelector, locator.getID(), outline + 1, filterSet);
         OpResourceUtilizationDataSetFactory.fillUtilizationValues(session, dataSet, targetPoolLocator, probability, projectId);

         resultList = new ArrayList();
         for (int i = 0; i < dataSet.getChildCount(); i++) {
            resultList.add(dataSet.getChild(i));
         }
         reply.setArgument(OpProjectConstants.CHILDREN, resultList);
      }

      return reply;
   }


}
