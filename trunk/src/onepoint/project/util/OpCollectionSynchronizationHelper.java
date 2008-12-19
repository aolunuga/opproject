package onepoint.project.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

public abstract class OpCollectionSynchronizationHelper<TARGET, SOURCE> {
   
   protected static final int AFTER = 1; // move source pointer (create new element for source)
   protected static final int EQUAL = 0;  // synchronize
   protected static final int BEFORE = -1; // move target pointer (delete target)
   
   // to enforce certain behavior for one or the other reason...
   // target not modified, so should be greater than zero:
   protected static final int DO_NOT_COPY = 3;
   protected static final int ADD = 2;
   
   protected static final int OK = 0;
   protected static final int ERROR = -2;
   
   private static XLog logger = XLogFactory.getLogger(OpCollectionSynchronizationHelper.class);

   protected abstract TARGET newInstance(SOURCE src);
   protected abstract void deleteInstance(TARGET del);
   protected abstract int cloneInstance(TARGET tgt, SOURCE src);
   
   protected abstract int corresponds(TARGET tgt, SOURCE src);
   
   protected abstract int targetOrder(TARGET cm1a, TARGET cm1b);
   protected abstract int sourceOrder(SOURCE cm2a, SOURCE cm2b);
   
   protected String debugTarget(TARGET tgt) {
      return null;
   }
   
   protected String debugSource(SOURCE src) {
      return null;
   }
   
   public void copy(Collection<TARGET> tgtColl, Collection<SOURCE> srcColl) {
      long now = System.currentTimeMillis();

      long[] mt = {0, 0, 0, 0, 0, 0, 0}; 
      int mti = 0;
      // setup ordered set input collections:
      // NOTE: the order should correspond as good as it gets:
      SortedSet<TARGET> tgtMembers = new TreeSet<TARGET>(new Comparator<TARGET>() {
         public int compare(TARGET o1, TARGET o2) {
            return targetOrder(o1, o2);
         }
      });
      if (tgtColl != null) {
         tgtMembers.addAll(tgtColl);
      }
      mt[mti++] = (tgtMembers.size());
      mt[mti++] = (System.currentTimeMillis() - now);
      
      SortedSet<SOURCE> srcMembers = new TreeSet<SOURCE>(new Comparator<SOURCE>() {
         public int compare(SOURCE o1, SOURCE o2) {
            return sourceOrder(o1, o2);
         }
      });
      if (srcColl != null) {
         srcMembers.addAll(srcColl);
      }
      mt[mti++] = (srcMembers.size());
      mt[mti++] = (System.currentTimeMillis() - now);
      
      // walk through src stuff and handle accordingly...
      Iterator<SOURCE> srcIt = srcMembers.iterator();
      Iterator<TARGET> tgtIt = tgtMembers.iterator();
      SOURCE srcM = null;
      TARGET tgtM = null;
      
      int reused = 0;
      while ((srcM != null || srcIt.hasNext()) && (tgtM != null || tgtIt.hasNext())) {

         srcM = (srcM == null && srcIt.hasNext()) ? srcIt.next() : srcM;
         tgtM = (tgtM == null && tgtIt.hasNext()) ? tgtIt.next() : tgtM;
         
         if (logger.isLoggable(XLog.DEBUG)) {
            String srcOut = debugSource(srcM);
            if (srcOut != null) {
               logger.debug("SRC-ELEMENT: " + srcOut);
            }
            String tgtOut = debugTarget(tgtM);
            if (srcOut != null) {
               logger.debug("TGT-ELEMENT: " + tgtOut);
            }
         }
         int corr = corresponds(tgtM, srcM);
         
         if (corr == EQUAL) {
            corr = cloneInstance(tgtM, srcM);
         }
         if (corr == OK) {
            tgtIt.remove();
            srcIt.remove();
            tgtM = null;
            srcM = null;
            reused++;
         }
         else if (corr == BEFORE) {
            // keep target for delete
            tgtM = null;
         }
         else if (corr == DO_NOT_COPY) {
            srcIt.remove(); // so that nobody tries to create it...
            srcM = null;
         }
         else if (corr == AFTER || corr == ADD) {
            // keep source for add
            srcM = null;
         }
         else if (corr == ERROR) {
            return;
         }
      }
      mt[mti++] = (System.currentTimeMillis() - now);
      // those left are either to be deleted or to be created:
      int deleted = 0;
      tgtIt = tgtMembers.iterator();
      while (tgtIt.hasNext()) {
         deleteInstance(tgtIt.next());
         deleted++;
      }
      
      mt[mti++] = (System.currentTimeMillis() - now);
      srcIt = srcMembers.iterator();
      int created = 0;
      int notCreated = 0;
      while (srcIt.hasNext()) {
         SOURCE nSrc = srcIt.next();
         TARGET nTgt = newInstance(nSrc);
         int result = cloneInstance(nTgt, nSrc);
         if (result == OK) {
            created++;
         }
         else if (result == DO_NOT_COPY) {
            deleteInstance(nTgt);
            notCreated++;
         }
      }
      mt[mti++] = (System.currentTimeMillis() - now);
      if(logger.isLoggable(XLog.INFO) && (System.currentTimeMillis() - now) > 1) {
         StringBuffer mtString = new StringBuffer();
         for (int xxx = 0; xxx < mt.length; xxx++) {
            mtString.append(" ");
            mtString.append(xxx);
            mtString.append(":");
            mtString.append(mt[xxx]);
         }
         logger.debug("TIMING-DETAILS: synchronize #03: " + mtString);
      }
      logger.debug("STATISTICS: synchronize R:" + reused + " C:" + created + " D:" + deleted + " nc:" + notCreated);
   }
   
   // simple, but useful helpers ;-)
   protected int cmpLong(long l1, long l2) {
      if (l2 == 0) {
         return DO_NOT_COPY;
      }
      return (l1 < l2 ? BEFORE : (l1 == l2 ? EQUAL : AFTER));
   }
   
   protected int cmpLongLong(long l1, long ll1, long l2, long ll2) {
      if (l2 == 0) {
         return DO_NOT_COPY;
      }
      int cmp = l1 < l2 ? BEFORE : l1 == l2 ? EQUAL : AFTER;
      if (cmp == EQUAL) {
         cmp = ll1 < ll2 ? BEFORE : ll1 == ll2 ? EQUAL : AFTER;
      }
      return cmp;
   }
}

