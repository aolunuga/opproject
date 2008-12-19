package onepoint.project.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

public abstract class OpCollectionCopyHelper<TARGET, SOURCE> {
   
   private static XLog logger = XLogFactory.getLogger(OpCollectionCopyHelper.class);

   protected abstract TARGET newInstance(SOURCE src);
   protected abstract void deleteInstance(TARGET del);
   
   public void copy(Collection<TARGET> tgtColl, Collection<SOURCE> srcColl) {
      if (tgtColl != null) {
         Iterator<TARGET> tit = tgtColl.iterator();
         Set<TARGET> tmp = new HashSet<TARGET>(tgtColl);
         while (tit.hasNext()) {
            TARGET del = tit.next();
            tit.remove();
            deleteInstance(del);
         }
      }
      if (srcColl != null) {
         Iterator<SOURCE> sit = srcColl.iterator();
         while (sit.hasNext()) {
            SOURCE src = sit.next();
            TARGET np = newInstance(src);
            if (tgtColl != null) {
               tgtColl.add(np);
            }
         }
      }
   }
   
}
