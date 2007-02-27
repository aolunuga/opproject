
/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_resources;


/**
 * @author gerald
 */
public class OpProjectResourceSummary {
   
   private long _resource_id;
   private String _resource_name;
   private double _base_effort;
   private double _actual_effort;
   private double _effort_to_complete;
   private double _predicted_effort;

   public OpProjectResourceSummary(long resource_id, String resource_name) {
      _resource_id = resource_id;
      _resource_name = resource_name;
   }
   
   public final long getResourceID() {
      return _resource_id;
   }
   
   public final String getResourceName() {
      return _resource_name;
   }

   public final void addBaseEffort(double base_effort) {
      _base_effort += base_effort;
   }
   
   public final double getBaseEffort() {
      return _base_effort;
   }

   public final void addActualEffort(double actual_effort) {
      _actual_effort += actual_effort;
   }
   
   public final double getActualEffort() {
      return _actual_effort;
   }

   public final void addEffortToComplete(double effort_to_complete) {
      _effort_to_complete += effort_to_complete;
   }

   public final double getEffortToComplete() {
      return _effort_to_complete;
   }
   
   public final void addPredictedEffort(double predicted_effort) {
      _predicted_effort += predicted_effort;
   }
   
   public final double getPredictedEffort() {
      return _predicted_effort;
   }

}
