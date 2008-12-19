/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.service.server.XSession;

import java.util.HashMap;


public class OpAddCommentFormProvider implements XFormProvider {

   private final static String ACTIVITY_ID_FIELD = "ActivityIDField";

   private static final XLog logger = XLogFactory.getLogger(OpActivitiesFormProvider.class);

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      logger.info("OpAddCommentFormProvider.prepareForm()");

      String activityLocator = (String) parameters.get(OpProjectPlanningService.ACTIVITY_ID);
      form.findComponent(ACTIVITY_ID_FIELD).setStringValue(activityLocator);

      logger.info("/OpAddCommentFormProvider.prepareForm()");
   }

}
