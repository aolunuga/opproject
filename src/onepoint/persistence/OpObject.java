/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class OpObject {

   public final static String ID = "ID";

   private OpPrototype prototype;
   private Date created;
   private Date modified;
   // private OpObject _access_context;
   // private Set _guarded_objects;
   private Set permissions;
   private Set locks;
   private long id = 0;
   private Set dynamicResources = new HashSet();

   // The following field is intentionally transient and dynamic
   private byte effectiveAccessLevel;

   public OpObject() {
      prototype = OpTypeManager.getPrototypeByClassName(getClass().getName());
   }

   public OpPrototype getPrototype() {
      return prototype;
   }

   // TODO: Had to make this public for personal object store; find a solution
   // (Hibernate is able to call private methods; check its source code)

   // TODO: We probably do not need personal store anymore -- make setID() private again?

   // Attention: Hibernate needs non-final setters and getters for proxy handling

   public void setID(long id) {
      this.id = id;
   }

   public long getID() {
      return id;
   }

   public void setCreated(Date created) {
      this.created = created;
   }

   public Date getCreated() {
      return created;
   }

   public void setModified(Date modified) {
      this.modified = modified;
   }

   public Date getModified() {
      return modified;
   }

   /*
   public void setAccessContext(OpObject access_context) {
      _access_context = access_context;
   }
   
   public OpObject getAccessContext() {
      return _access_context;
   }

   public void setGuardedObjects(Set guarded_objects) {
      _guarded_objects = guarded_objects;
   }
   
   public Set getGuardedObjects() {
      return _guarded_objects;
   }
   */

   public void setPermissions(Set permissions) {
      this.permissions = permissions;
   }

   public Set getPermissions() {
      return permissions;
   }

   public void setLocks(Set locks) {
      this.locks = locks;
   }

   public Set getLocks() {
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

   // Note: Both methods should be overriden by business key implementations

   public boolean equals(Object object) {
      return (object.getClass() == getClass()) && (((OpObject) object).id == id) && (((OpObject) object).id != 0)
               && (id != 0);
   }

   public int hashCode() {
      if (id != 0) {
         return (int) (id ^ (id >>> 32));
      }
      else {
         return System.identityHashCode(this);
      }
   }

}