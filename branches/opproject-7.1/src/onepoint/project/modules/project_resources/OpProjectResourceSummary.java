
/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_resources;


/**
 * @author gerald
 */
public class OpProjectResourceSummary {
   
   private long resourceId;
   private String resourceName;
   private double baseEffort;
   private double actualEffort;
   private double effortToComplete;
   private double predictedEffort;

   public OpProjectResourceSummary(long resource_id, String resource_name) {
      resourceId = resource_id;
      resourceName = resource_name;
   }
   
   public final long getResourceID() {
      return resourceId;
   }
   
   public final String getResourceName() {
      return resourceName;
   }

   public final void addBaseEffort(double base_effort) {
      baseEffort += base_effort;
   }
   
   public final double getBaseEffort() {
      return baseEffort;
   }

   public final void addActualEffort(double actual_effort) {
      actualEffort += actual_effort;
   }
   
   public final double getActualEffort() {
      return actualEffort;
   }

   public final void addEffortToComplete(double effort_to_complete) {
      effortToComplete += effort_to_complete;
   }

   public final double getEffortToComplete() {
      return effortToComplete;
   }
   
   public final void addPredictedEffort(double predicted_effort) {
      predictedEffort += predicted_effort;
   }
   
   public final double getPredictedEffort() {
      return predictedEffort;
   }

}
