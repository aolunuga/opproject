/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpProjectNodeAssignment;

import java.sql.Date;

/**
 * Entity representing periods of external and internal hourly rates.
 *
 * @author horia.chiorean
 */
public class OpHourlyRatesPeriod extends OpObject {

   /**
    * The resource for which the hourly rates are recorded.
    */
   private OpResource resource;

   /**
    * The project node assignment for which the hourly rates are recorded.
    */
   private OpProjectNodeAssignment projectNodeAssignment;


   /**
    * The start of the hourly rates period.
    */
   private Date start;

   /**
    * The end of the hourly rates period.
    */
   private Date finish;

   /**
    * The resource internal hourly rate.
    */
   private double internalRate;

   /**
    * The resource external hourly rate.
    */
   private double externalRate;

   public static int PERIOD_START_DATE_NOT_VALID = 1;
   public static int PERIOD_END_DATE_NOT_VALID = 2;
   public static int INTERNAL_RATE_NOT_VALID = 3;
   public static int EXTERNAL_RATE_NOT_VALID = 4;
   public static int PERIOD_INTERVAL_NOT_VALID = 5;

   /**
    * Gets the resource of the period.
    *
    * @return a <code>OpResource</code> entity.
    */
   public OpResource getResource() {
      return resource;
   }

   /**
    * Sets the resource of the period.
    *
    * @param resource a <code>OpResource</code> entity.
    */
   public void setResource(OpResource resource) {
      this.resource = resource;
   }

   /**
    * Gets the project node assignment of the period.
    *
    * @return a <code>OpProjectNodeAssignment</code> entity.
    */
   public OpProjectNodeAssignment getProjectNodeAssignment() {
      return projectNodeAssignment;
   }

   /**
    * Sets the project node assignment of the period.
    *
    * @param projectNodeAssignment a <code>OpProjectNodeAssignment</code> entity.
    */
   public void setProjectNodeAssignment(OpProjectNodeAssignment projectNodeAssignment) {
      this.projectNodeAssignment = projectNodeAssignment;
   }

   /**
    * Gets the start date of the period.
    *
    * @return a <code>Date</code> object.
    */
   public Date getStart() {
      return start;
   }

   /**
    * Sets the start date of the period.
    *
    * @param start a <code>Date</code> object.
    */
   public void setStart(Date start) {
      this.start = start;
   }

   /**
    * Gets the finish date of the period.
    *
    * @return a <code>Date</code> object.
    */
   public Date getFinish() {
      return finish;
   }

   /**
    * Sets the finish date of the period.
    *
    * @param finish a <code>Date</code> object.
    */
   public void setFinish(Date finish) {
      this.finish = finish;
   }

   /**
    * Gets the internal hourly rate of this period.
    *
    * @return a <code>double</code> object.
    */
   public double getInternalRate() {
      return internalRate;
   }

   /**
    * Sets the internal hourly rate of this period.
    *
    * @param internalRate a <code>double</code> object.
    */
   public void setInternalRate(double internalRate) {
      this.internalRate = internalRate;
   }

   /**
    * Gets the external hourly rate of this period.
    *
    * @return a <code>double</code> object.
    */
   public double getExternalRate() {
      return externalRate;
   }

   /**
    * Sets the external hourly rate of this period.
    *
    * @param externalRate a <code>double</code> object.
    */
   public void setExternalRate(double externalRate) {
      this.externalRate = externalRate;
   }

   /**
    * Checks if the fields of the period are valid
    *
    * @return  <code>0</code> id all fields have valid values
    *          the error code corresponding to the error caused by the inccorect field
    */
   public int isValid() {

      if (start == null) {
         return PERIOD_START_DATE_NOT_VALID;
      }
      else if (finish == null) {
         return PERIOD_END_DATE_NOT_VALID;
      }
      else if (internalRate < 0) {
         return INTERNAL_RATE_NOT_VALID;
      }
      else if (externalRate < 0) {
         return EXTERNAL_RATE_NOT_VALID;
      }
      else if (finish.before(start)){
         return PERIOD_INTERVAL_NOT_VALID;
      }
      else {
         return 0;
      }
   }   
}
