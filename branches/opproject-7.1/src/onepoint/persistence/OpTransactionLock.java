package onepoint.persistence;

import java.util.HashMap;

/**
 * @author peter
 * 
 * We use the locator String to refer to the lock-object.
 * All locking is done manually, with full user responsibility.
 * It is recommended to use 
 * <code>lock; try { <critical-section/> } finally { unlock; }</code> pattern. 
 * 
 */
public class OpTransactionLock {
   
   // ok, there is some sort of Lock Object out there, but we don't want reentrant (to identify problems more early;-)
   private static class Lock {
      
      int locks = 0;
      int waitingReaders = 0;
      int waitingWriters = 0;
      private Object mutex = null;
      
      public Lock() {
         mutex = new Object();
      }
      
      boolean lockUsed() {
         synchronized(mutex) {
            return locks != 0 || waitingReaders != 0 || waitingWriters != 0;
         }
      }
      
      public void readLock() {
         synchronized(mutex) {
            waitingReaders++;
            while (locks < 0) {
               try {
                  mutex.wait();
               } catch (InterruptedException e) {
               }
            }
            locks++;
            waitingReaders--;
         }
      }
      
      public void writeLock() {
         synchronized(mutex) {
            waitingWriters++;
            while (locks != 0) {
               try {
                  mutex.wait();
               } catch (InterruptedException e) {
               }
            }
            locks = -1;
            waitingWriters--;
         }
      }

      public void unlock() {
         synchronized(mutex) {
            if (locks == 0) {
               return; // TODO: check for error?!?
            }
            if (locks > 0) {
               locks--;
            }
            else {
               locks = 0;
            }
            // wake them up:
            mutex.notifyAll();
         }
      }
   }
   
   private static OpTransactionLock instance = null;

   public static OpTransactionLock getInstance() {
      if (instance == null) {
         instance = new OpTransactionLock();
      }
      return instance;
   }

   private HashMap<String, Lock> locks = null;
   private Object mutex;
   
   private OpTransactionLock() {
      mutex = new Object();
      locks = new HashMap<String, Lock>();
   }

   public void readLock(String loc) {
      synchronized (mutex) {
         Lock l = locks.get(loc);
         if (l == null) {
            l = new Lock();
            locks.put(loc, l);
         }
         l.readLock();
      }
   }
   
   public void writeLock(String loc) {
      synchronized (mutex) {
         Lock l = locks.get(loc);
         if (l == null) {
            l = new Lock();
            locks.put(loc, l);
         }
         l.writeLock();
      }
   }
   
   public void unlock(String loc) {
      synchronized (mutex) {
         Lock l = locks.get(loc);
         if (l != null) {
            l.unlock();
         }
      }
   }
}
