/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import java.sql.Timestamp;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;

public class OpLock extends OpObject {
   
   public final static String LOCK = "OpLock";

   public final static String OWNER = "Owner";
   public final static String TARGET = "Target";
   public final static String LOCKERID = "LockerId";

   private static final XLog logger = XLogFactory.getServerLogger(OpLock.class);

   private OpUser owner;
   private OpObject target;

   private Long lockerId;

   public void setOwner(OpUser owner) {
      this.owner = owner;
   }

   public OpUser getOwner() {
      return owner;
   }

   public void setTarget(OpObject target) {
      this.target = target;
   }

   public OpObject getTarget() {
      return target;
   }

   /**
    * Sets an identified for this lock.
    * identifiers can be uses to check if the lock was set within a specific session. 
    * @param id the id to set
    */
   public void setLockerID(Long id) {
      this.lockerId = id;
   }

   /**
    * Returns the identifier the lock was set.
    * @return the identifier the lock was set.
    */
   public Long getLockerID() {
      return lockerId;
   }

   /**
    * @param session
    * @param broker 
    * @return
    * @pre
    * @post
    */
   public boolean lockedByMe(OpProjectSession session, OpBroker broker) {
      if (owner.getID() == session.user(broker).getID()) {
         Timestamp lockTS = getModified();
         if (lockTS == null) {
            lockTS = getCreated();
         }
         // check if XServer is older than lock timesatamp (restarted after lock)
         if (new Timestamp(session.getServer().getCreationTimeMillis()).before(lockTS)) {
            if (lockerId != null) {
               if (session.getID() != lockerId.intValue()) {
                  // check if lock is held by a still open session
                  if (session.getServer().isOpen(lockerId.intValue())) {
                     return false;
                  }
               }
               // steel lock
               logger.info("steeling lock for object "+getTarget().getID()+
                     " from id "+getLockerID()+" to id "+session.getID());
               // have to be done as atomic call
               OpBroker lockBroker = session.newBroker();
               OpTransaction t = lockBroker.newTransaction();
               OpLock lock = (OpLock)lockBroker.getObject(OpLock.class, getID());
               lock.setLockerID(new Long(session.getID()));
               t.commit();
               lockBroker.close();
//               broker.makePersistent(lock);
            }
         }
         else {
            // steel lock
            logger.info("steeling lock after restart for object "+getTarget().getID()+
                  " from id "+getLockerID()+" to id "+session.getID());
            // have to be done as atomic call
            OpBroker lockBroker = session.newBroker();
            OpTransaction t = lockBroker.newTransaction();
            OpLock lock = (OpLock)lockBroker.getObject(OpLock.class, getID());
            lock.setLockerID(new Long(session.getID()));
            t.commit();
            lockBroker.close();
         }
         return true;
      }
      return false;
   }
}