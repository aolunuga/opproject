/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.persistence.OpCustomSubTypable;
import onepoint.persistence.OpObject;
import onepoint.project.modules.custom_attribute.OpCustomType;

import java.util.Set;

public class OpResourcePool extends OpObject implements OpCustomSubTypable {

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
   public final static String ROOT_RESOURCE_POOL_ID_QUERY = "select pool.ID from OpResourcePool as pool where pool.Name = '" + ROOT_RESOURCE_POOL_NAME + "'";

   private String name;
   private String description;
   private double hourlyRate;
   private double externalRate;
   private OpResourcePool superPool;
   private Set subPools;
   private Set resources;

   private OpCustomType customType;

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

   public void setSubPools(Set subPools) {
      this.subPools = subPools;
   }

   public Set getSubPools() {
      return subPools;
   }

   public void setResources(Set resources) {
      this.resources = resources;
   }

   public Set getResources() {
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
}