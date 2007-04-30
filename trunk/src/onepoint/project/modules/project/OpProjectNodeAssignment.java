/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.resource.OpHourlyRatesPeriod;
import onepoint.project.modules.resource.OpResource;

import java.util.Set;

public class OpProjectNodeAssignment extends OpObject {

   public final static String PROJECT_NODE_ASSIGNMENT = "OpProjectNodeAssignment";

   public final static String RESOURCE = "Resource";
   public final static String PROJECT_NODE = "ProjectNode";

   private Double hourlyRate;
   private Double externalRate;
   private OpResource resource;
   private OpProjectNode projectNode;
   private Set<OpHourlyRatesPeriod> hourlyRatesPeriods;

   public void setResource(OpResource resource) {
      this.resource = resource;
   }

   public OpResource getResource() {
      return resource;
   }

   public void setProjectNode(OpProjectNode projectNode) {
      this.projectNode = projectNode;
   }

   public OpProjectNode getProjectNode() {
      return projectNode;
   }

   public Double getExternalRate() {
      return externalRate;
   }

   public void setExternalRate(Double externalRate) {
      this.externalRate = externalRate;
   }

   public Double getHourlyRate() {
      return hourlyRate;
   }

   public void setHourlyRate(Double hourlyRate) {
      this.hourlyRate = hourlyRate;
   }

   public Set<OpHourlyRatesPeriod> getHourlyRatesPeriods() {
      return hourlyRatesPeriods;
   }

   public void setHourlyRatesPeriods(Set<OpHourlyRatesPeriod> hourlyRatesPeriods) {
      this.hourlyRatesPeriods = hourlyRatesPeriods;
   }
}
