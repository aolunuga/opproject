/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.persistence.OpCustomSubTypable;
import onepoint.persistence.hibernate.OpPropertyAccessor;
import onepoint.project.modules.calendars.OpHasWorkCalendar;
import onepoint.project.modules.calendars.OpWorkCalendar;
import onepoint.project.modules.custom_attribute.OpCustomType;
import onepoint.project.modules.custom_attribute.OpCustomizableObject;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;

import java.util.HashSet;
import java.util.Set;

public class OpResourcePool extends OpCustomizableObject implements OpPermissionable, OpCustomSubTypable, OpHasWorkCalendar {

   public final static String RESOURCE_POOL = "OpResourcePool";

   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";
   public final static String HOURLY_RATE = "HourlyRate";
   public final static String EXTERNAL_RATE = "ExternalRate";
   public final static String SUPER_POOL = "SuperPool";
   public final static String SUB_POOLS = "SubPools";
   public final static String RESOURCES = "Resources";

   // Root resource pool
   public final static String ROOT_RESOURCE_POOL_NAME = "${RootResourcePoolName}";
   public final static String ROOT_RESOURCE_POOL_DESCRIPTION = "${RootResourcePoolDescription}";
   public final static String ROOT_RESOURCE_POOL_ID_QUERY = "select pool.id from OpResourcePool as pool where pool.Name = '" + ROOT_RESOURCE_POOL_NAME + "'";

   private String name;
   private String description;
   private double hourlyRate;
   private double externalRate;
   private String calendar;
   private OpResourcePool superPool;
   private Set<OpResourcePool> subPools;
   private Set<OpResource> resources;

   private OpCustomType customType;

   private Set<OpPermission> permissions;

   private OpWorkCalendar workCalendar = null;
   
   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getDescription() {
      return description;
   }

   public void setHourlyRate(double hourlyRate) {
      this.hourlyRate = hourlyRate;
   }

   public double getHourlyRate() {
      return hourlyRate;
   }
   
   public void setSuperPool(OpResourcePool superPool) {
      this.superPool = superPool;
   }
   
   public OpResourcePool getSuperPool() {
      return superPool;
   }

   public void setSubPools(Set<OpResourcePool> subPools) {
      this.subPools = subPools;
   }

   public Set<OpResourcePool> getSubPools() {
      return subPools;
   }

   public void setResources(Set<OpResource> resources) {
      this.resources = resources;
   }

   public Set<OpResource> getResources() {
      return this.resources;
   }

   public double getExternalRate() {
      return externalRate;
   }

   public void setExternalRate(Double externalRate) {
      setExternalRateInternal(externalRate);
   }

   /**
    * called internally by hibernate
    * @see OpPropertyAccessor
    */
   private void setExternalRateInternal(Double externalRate) {
      this.externalRate = (externalRate != null) ? externalRate : 0;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpCustomSubTypable#getCustomType()
    */
   public OpCustomType getCustomType() {
      return customType;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpCustomSubTypable#setCustomType(onepoint.project.modules.custom_attribute.OpCustomType)
    */
   public void setCustomType(OpCustomType customType) {
      this.customType = customType;
   }
   
   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#getPermissions()
    */
   public Set<OpPermission> getPermissions() {
      return permissions;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#setPermissions(java.util.Set)
    */
   public void setPermissions(Set<OpPermission> permissions) {
      this.permissions = permissions;
   }

   public void addPermission(OpPermission permission) {
      Set<OpPermission> perm = getPermissions();
      if (perm == null) {
         perm = new HashSet<OpPermission>();
         setPermissions(perm);
      }
      perm.add(permission);
      permission.setObject(this);
   }

   /**
    * @param opPermission
    * @pre
    * @post
    */
   public void removePermission(OpPermission opPermission) {
      Set<OpPermission> perm = getPermissions();
      if (perm != null) {
         perm.remove(opPermission);
      }
      opPermission.setObject(null);
   }

   private String getCalendar() {
      return calendar;
   }

   private void setCalendar(String calendar) {
      this.calendar = calendar;
   }

   public OpWorkCalendar getWorkCalendar() {
      return workCalendar;
   }

   public void setWorkCalendar(OpWorkCalendar workCalendar) {
      this.workCalendar = workCalendar;
   }

}