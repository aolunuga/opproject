package onepoint.project;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpTransaction;
import onepoint.service.server.XService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class OpProjectService extends XService {

   private static XLog logger = XLogFactory.getLogger(OpProjectService.class,true);

   /**
    * Inidicates whether this service is remote or not.
    * <FIXME author="Horia Chiorean" description="Hack. Should not remain here.">
    */
   private static boolean isRemote = false;

   public void compareObjectSetToIDs(Set set, long[] assigned_group_ids, ArrayList to_add, ArrayList to_remove) {
      OpObject object = null;
      Iterator objects = set.iterator();
      int i = 0;
      boolean found = false;
      long id = 0;
      ArrayList to_compare = new ArrayList();
      while (objects.hasNext()) {
         object = (OpObject) (objects.next());
         id = object.getID();
         logger.debug("   ID " + id);
         found = false;
         for (i = 0; i < assigned_group_ids.length; i++) {
            if (id == assigned_group_ids[i]) {
               found = true;
               break;
            }
         }
         if (!found)
            to_remove.add(new Long(id));
         else
            to_compare.add(new Long(id));
      }
      int j = 0;
      for (i = 0; i < assigned_group_ids.length; i++) {
         found = false;
         for (j = 0; j < to_compare.size(); j++) {
            id = ((Long) (to_compare.get(j))).longValue();
            if (id == assigned_group_ids[i]) {
               found = true;
               break;
            }
         }
         if (!found)
            to_add.add(new Long(assigned_group_ids[i]));
      }
   }

   public static boolean isRemote() {
      return isRemote;
   }

   public static void setRemote(boolean remote) {
      isRemote = remote;
   }

   /**
    * Rollbacks the current<code>transaction</code> and releases the <code>broker</code>. This should be extracted
    * in a helper class.
    *
    * @param broker
    *           a <code>OpBroker</code> representing the broker
    * @param transaction
    *           a <code>OpTransaction</code> representing the current transaction
    */
   protected void finalizeSession(OpTransaction transaction, OpBroker broker) {
      transaction.rollback();
      broker.close();
   }
}
