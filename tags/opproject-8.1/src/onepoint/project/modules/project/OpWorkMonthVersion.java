package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

/**
 * Work Month version entity.
 *
 * @author mihai.costin
 */
public class OpWorkMonthVersion extends OpObject {

   private int year;
   private byte month;

   /**
    * Assignment Version of this work month period.
    */
   private OpAssignmentVersion assignmentVersion;

   /**
    * % assigned of the resource.
    */
   private double baseAssigned;

   /**
    * Baseline effort of the assignmentVersion version.
    */
   private double baseEffort;

   /**
    * Baseline personnel costs of the assignmentVersion version.
    */
   private double basePersonnelCosts;

   /**
    * Baseline proceeds of the assignmentVersion version.
    */
   private double baseProceeds;

   public int getYear() {
      return year;
   }

   public void setYear(int year) {
      this.year = year;
   }

   public byte getMonth() {
      return month;
   }

   public void setMonth(byte month) {
      this.month = month;
   }

   public OpAssignmentVersion getAssignmentVersion() {
      return assignmentVersion;
   }

   public void setAssignmentVersion(OpAssignmentVersion assignmentVersion) {
      this.assignmentVersion = assignmentVersion;
   }

   public double getBaseAssigned() {
      return baseAssigned;
   }

   public void setBaseAssigned(double baseAssigned) {
      this.baseAssigned = baseAssigned;
   }

   public double getBaseEffort() {
      return baseEffort;
   }

   public void setBaseEffort(double baseEffort) {
      this.baseEffort = baseEffort;
   }

   public double getBasePersonnelCosts() {
      return basePersonnelCosts;
   }

   public void setBasePersonnelCosts(double basePersonnelCosts) {
      this.basePersonnelCosts = basePersonnelCosts;
   }

   public double getBaseProceeds() {
      return baseProceeds;
   }

   public void setBaseProceeds(double baseProceeds) {
      this.baseProceeds = baseProceeds;
   }
}
