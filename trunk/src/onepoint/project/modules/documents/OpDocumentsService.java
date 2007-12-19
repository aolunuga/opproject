/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.documents;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.service.XMessage;

/**
 * Service class that handles all the document related operations
 *
 * @author lucian.furtos
 */
public class OpDocumentsService extends OpProjectService {

   /**
    * Deletes all <code>OpContent</code> objects that have no reference to them.
    *
    * @param session - the current <code>OpProjectSession</code>.
    * @param request a <code>XMessage</code> - the request.
    * @return a <code>XMessage</code> reply
    */
   public void deleteZeroRefContents(OpProjectSession session, XMessage request) {
      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();
      OpContentManager.deleteZeroRefContents(broker);
      transaction.commit();
      broker.close();
   }
}
