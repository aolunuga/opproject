/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.documents.test;

import onepoint.project.test.OpTestDataFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.project.OpActivityComment;
import onepoint.project.modules.documents.OpContent;
import onepoint.service.XMessage;
import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;

import java.util.HashMap;

/**
 * This class contains helper methods for managing documents data
 *
 * @author lucian.furtos
 */
public class OpDocumentsTestDataFactory extends OpTestDataFactory {

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public OpDocumentsTestDataFactory(OpProjectSession session) {
      super(session);
   }
}
