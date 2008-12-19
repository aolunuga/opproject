/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

public class OpObject extends OpLocatableObject implements OpObjectIfc {

   private static final XLog logger = XLogFactory.getLogger(OpObject.class);

   public final static String CREATED = "Created";
   public final static String MODIFIED = "Modified";
      
   private Timestamp created;
   private Timestamp modified;
   
   public OpObject() {
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpObjectIfc#setCreated(java.sql.Timestamp)
    */
   public void setCreated(Timestamp created) {
      this.created = created;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpObjectIfc#getCreated()
    */
   public Timestamp getCreated() {
      return created;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpObjectIfc#setModified(java.sql.Timestamp)
    */
   public void setModified(Timestamp modified) {
      this.modified = modified;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpObjectIfc#getModified()
    */
   public Timestamp getModified() {
      return modified;
   }
   
   /**
    * if somebody finds a better place for this one...
    * @param objects
    * @return
    */
   public static Collection<Long> getIdsFromObjects(Collection<? extends OpObject> objects) {
      Collection ids = new ArrayList<Long>(objects.size());
      for (OpObject o : objects) {
         ids.add(new Long(o.getId()));
      }
      return ids;
   }
}  