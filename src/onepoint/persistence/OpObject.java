/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.project.modules.user.OpLock;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

public class OpObject {

   public final static String CREATED = "Created";
   public final static String MODIFIED = "Modified";
   public final static String ID = "ID";

   private OpPrototype prototype;
   private Timestamp created;
   private Timestamp modified;
   private Set permissions;
   private Set<OpLock> locks;
   private long id = 0;
   private Set dynamicResources = new HashSet();
   private String siteId;
   
   // The following field is intentionally transient and dynamic
   private byte effectiveAccessLevel;

   public OpObject() {
      prototype = OpTypeManager.getPrototypeByClassName(getClass().getName());
   }

   public OpPrototype getPrototype() {
      return prototype;
   }

   private void setID(long id) {
      this.id = id;
   }

   public long getID() {
      return id;
   }

   public String getSiteId() {
      return siteId;
   }

   public void setSiteId(String siteId) {
      this.siteId = siteId;
   }

   public void setCreated(Timestamp created) {
      this.created = created;
   }

   public Timestamp getCreated() {
      return created;
   }

   public void setModified(Timestamp modified) {
      this.modified = modified;
   }

   public Timestamp getModified() {
      return modified;
   }

   public void setPermissions(Set permissions) {
      this.permissions = permissions;
   }

   public Set getPermissions() {
      return permissions;
   }

   public void setLocks(Set<OpLock> locks) {
      this.locks = locks;
   }

   public Set<OpLock> getLocks() {
      return locks;
   }

   public void setEffectiveAccessLevel(byte effectiveAccessLevel) {
      this.effectiveAccessLevel = effectiveAccessLevel;
   }

   public byte getEffectiveAccessLevel() {
      return effectiveAccessLevel;
   }

   public String locator() {
      return OpLocator.locatorString(this);
   }

   public Set getDynamicResources() {
      return dynamicResources;
   }

   public void setDynamicResources(Set dynamicResources) {
      this.dynamicResources = dynamicResources;
   }

   @Override
   public boolean equals(Object object) {
      return (object.getClass() == getClass()) && (((OpObject) object).id == id) && (((OpObject) object).id != 0)
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

}