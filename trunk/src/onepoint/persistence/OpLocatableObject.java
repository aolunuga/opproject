/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

public class OpLocatableObject implements OpLocatable {

   private static final XLog logger = XLogFactory.getLogger(OpLocatableObject.class);

   public final static String ID = "ID";
      
   private long id = 0;
   private String siteId;
   
   public OpLocatableObject() {
   }

   public void setId(long id) {
      this.id = id;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpObjectIfc#getID()
    */
   public long getId() {
      return id;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpObjectIfc#getSiteId()
    */
   public String getSiteId() {
      return siteId;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpObjectIfc#setSiteId(java.lang.String)
    */
   public void setSiteId(String siteId) {
      this.siteId = siteId;
   }
   
   public String locator() {
      return OpLocator.locatorString(this);
   }

   @Override
   public boolean equals(Object object) {
      if (object == null) {
         return false;
      }
      return (object.getClass() == getClass()) && 
             (((OpLocatableObject) object).id == id) && 
             (((OpLocatableObject) object).id != 0)
             && (id != 0);
   }

   @Override
   public int hashCode() {
      if (id != 0) {
         return (int) (id ^ (id >>> 32));
      }
      else {
         return System.identityHashCode(this);
      }
   }
   /**
    * @return
    * @pre
    * @post
    */
   public boolean exists() {
      return getId() != 0;
   }

   /* (non-Javadoc)
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   public int compareTo(OpLocatable o) {
      return ((int)(getId()-o.getId()));
   }

}