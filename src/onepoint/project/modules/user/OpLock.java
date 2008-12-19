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

   private static final XLog logger = XLogFactory.getLogger(OpLock.class);

   private OpUser owner;
   private OpLockable target;

   private Long lockerID;

   public void setOwner(OpUser owner) {
      this.owner = owner;
   }

   public OpUser getOwner() {
      return owner;
   }

   public void setTarget(OpLockable target) {
      this.target = target;
   }

   public OpLockable getTarget() {
      return target;
   }

   /**
    * Sets an identified for this lock.
    * identifiers can be uses to check if the lock was set within a specific session. 
    * @param id the id to set
    */
   public void setLockerID(Long id) {
      this.lockerID = id;
   }

   /**
    * Returns the identifier the lock was set.
    * @return the identifier the lock was set.
    */
   public Long getLockerID() {
      return lockerID;
   }

   /**
    * @param session
    * @param broker 
    * @return
    * @pre
    * @post
    */
   public boolean lockedByMe(OpProjectSession session, OpBroker broker) {
      OpUser user = session.user(broker);
      if (user == null) {
         return false;
      }
      if (owner == null) {
         return false;
      }
      if (owner.getId() == user.getId()) {
         Timestamp lockTS = getModified();
         if (lockTS == null) {
            lockTS = getCreated();
         }
         // check if XServer is older than lock timesatamp (restarted after lock)
         if (new Timestamp(session.getServer().getCreationTimeMillis()).before(lockTS)) {
            if (lockerID != null) {
               if (session.getID() != lockerID.intValue()) {
                  // check if lock is held by a still open session
                  OpProjectSession lockerSession = (OpProjectSession) session.getServer().getSession(lockerID.intValue());
                  // check if lockerSession is still valid and no logoff toke place
                  if ((lockerSession != null) && (lockerSession.isValid()) &&
                        (lockerSession.getUserID() == owner.getId())) {
                     return false;
                  }
               }
               // have to be done as atomic call
               OpBroker lockBroker = session.newBroker();
               try {
                  OpTransaction t = lockBroker.newTransaction();
                  OpLock lock = (OpLock)lockBroker.getObject(OpLock.class, getId());
                  if (lock != null) {
                     // FIXME: ugly, if the lock was created in a surrounding transaction, then it will not be found by this inner broker...
                     // steel lock
                     logger.info("steeling lock for object "+getTarget().getId()+
                           " from id "+getLockerID()+" to id "+session.getID());
                     lock.setLockerID(new Long(session.getID()));
                  }
                  t.commit();
               }
               finally {
                  lockBroker.close();
               }
//               broker.makePersistent(lock);
            }
         }
         else {
            // steel lock
            logger.info("steeling lock after restart for object "+getTarget().getId()+
                  " from id "+getLockerID()+" to id "+session.getID());
            // have to be done as atomic call
            OpBroker lockBroker = session.newBroker();
            OpTransaction t = lockBroker.newTransaction();
            OpLock lock = (OpLock)lockBroker.getObject(OpLock.class, getId());
            lock.setLockerID(new Long(session.getID()));
            t.commit();
            lockBroker.close();
         }
         return true;
      }
      return false;
   }
}