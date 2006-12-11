package onepoint.project;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.service.server.XService;

public class OpProjectService extends XService {

   private static final XLog logger = XLogFactory.getLogger(OpProjectService.class, true);

   /**
    * Rollbacks the current<code>transaction</code> and releases the <code>broker</code>. This should be extracted
    * in a helper class.
    *
    * @param broker      a <code>OpBroker</code> representing the broker
    * @param transaction a <code>OpTransaction</code> representing the current transaction
    */
   protected void finalizeSession(OpTransaction transaction, OpBroker broker) {
      logger.info("Finalizing session...");
      transaction.rollback();
      broker.close();
   }
}
