/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.setup;

import java.util.Map;

import onepoint.express.XDisplay;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpSource;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;

/**
 * License Service class.
 *
 * @author : mihai.costin
 */
public class OpSetupService extends OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpSetupService.class);

   /**
    * License service name
    */
   private static final String SETUP_SERVICE_NAME = "SetupService";

   /**
    * Initialize product and set initialization parameters into provided XMessage parameter.
    * Also register the data source with the session.
    *
    * @param session a <code>OpProjectSession</code> representing the current server session.
    * @return response message where to write initialization parameters.
    */
   private XMessage initializeProduct(OpProjectSession session) {
      //response message
      XMessage response = new XMessage();

      // initialize factory
      OpInitializer initializer = OpInitializerFactory.getInstance().getInitializer();
      Map<String, Object> initParams = initializer.init(OpEnvironmentManager.getProductCode());

      if (Byte.parseByte((String)initParams.get(OpProjectConstants.RUN_LEVEL)) == OpProjectConstants.SUCCESS_RUN_LEVEL) {
         //set the source for the current session
         session.init(OpSource.DEFAULT_SOURCE_NAME);
      }

      response.setArgument(OpProjectConstants.INIT_PARAMS, initParams);
      return response;
   }

   /**
    * Checks if the license has expired.
    *
    * @param s
    * @param request
    * @return - an <code>XMessage</code> object containing the error code in case of an error.
    */
   public XMessage lockFileExists(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;
      XMessage reply = new XMessage();
      int action = (Integer)request.getArgument("action");
      if (action == XDisplay.OK_OPTION) {
         OpInitializer initializer = OpInitializerFactory.getInstance().getInitializer();
         initializer.deleteLockFile();
         Map<String, Object> initParams = initializer.init(OpEnvironmentManager.getProductCode());

         if (Byte.parseByte((String)initParams.get(OpProjectConstants.RUN_LEVEL)) == OpProjectConstants.SUCCESS_RUN_LEVEL) {
            //set the source for the current session
            session.init(OpSource.DEFAULT_SOURCE_NAME);
         }

         reply.setArgument(OpProjectConstants.INIT_PARAMS, initParams);
         return reply;
      }
      return reply;
   }
}