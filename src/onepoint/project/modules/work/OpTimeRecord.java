/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpActivity;
import onepoint.util.XCalendar;

/**
 * Entity representing a time record for the advanced time tracking.
 *
 * @author horia.chiorean
 */
public class OpTimeRecord extends OpObject {

   public final static String TIME_RECORD = "OpTimeRecord";

   /**
    * Start and finish fields, representing the number of minutes in the day (workslip date).
    */
   private Integer start = null;
   private Integer finish = null;

   /**
    * The duration of the time record.
    */
   private Integer duration = null;

   /**
    * The associated work record.
    */
   private OpWorkRecord workRecord = null;

   /**
    * Gets the start of the time record.
    * @return an <code>Integer</code> representing a number of minutes.
    */
   public Integer getStart() {
      return start;
   }

   /**
    * Sets the start of the time record.
    * @param start an <code>Integer</code> representing a number of minutes.
    */
   public void setStart(Integer start) {
      this.start = start;
   }

   /**
    * Gets the finish of the time record.
    * @return an <code>Integer</code> representing a number of minutes.
    */
   public Integer getFinish() {
      return finish;
   }

   /**
    * Sets the start of the time record.
    * @param finish an <code>Integer</code> representing a number of minutes.
    */
   public void setFinish(Integer finish) {
      this.finish = finish;
   }

   /**
    * Gets the duration of the time record.
    * @return an <code>Integer</code> representing a number of minutes.
    */
   public Integer getDuration() {
      return duration;
   }

   /**
    * Sets the duration of the time record.
    * @param duration an <code>Integer</code> representing a number of minutes.
    */
   public void setDuration(Integer duration) {
      this.duration = duration;
   }

   /**
    * Gets the workrecord for the time record.
    * @return a <code>OpWorkRecord</code> entity.
    */
   public OpWorkRecord getWorkRecord() {
      return workRecord;
   }

   /**
    * Sets the workrecord for the time record.
    * @param workRecord a  <code>OpWorkRecord</code> entity.
    */
   public void setWorkRecord(OpWorkRecord workRecord) {
      this.workRecord = workRecord;
   }

   /**
    * Returns the record's activity if it has one, <code>null</code> otherwise.
    *
    * @return a <code>OpActivity</code> instance of <code>null</code>.
    */
   public OpActivity getActivity() {
      if (this.getWorkRecord() == null) {
         return null;
      }
      return this.getWorkRecord().getAssignment().getActivity();
   }

   /**
    * Checks if the fields of the time record are valid
    *
    * @throws onepoint.persistence.OpEntityException
    *          if some validation constraints are broken
    */
   public void validate()
        throws OpEntityException {

      //start time must be positive
      if (start < 0) {
         throw new OpEntityException(OpWorkError.START_TIME_IS_NEGATIVE);
      }
      //finish time must be positive
      else if (finish < 0) {
         throw new OpEntityException(OpWorkError.FINISH_TIME_IS_NEGATIVE);
      }
      //start time must not exceed 24 * 60
      else if (start >= XCalendar.MINUTES_PER_DAY) {
         throw new OpEntityException(OpWorkError.START_TIME_IS_TOO_LARGE);
      }
      //finish time must not exceed 24 * 60
      else if (finish >= XCalendar.MINUTES_PER_DAY) {
         throw new OpEntityException(OpWorkError.FINISH_TIME_IS_TOO_LARGE);
      }
      //finish time must be greater than start time
      else if (start >= finish) {
         throw new OpEntityException(OpWorkError.START_AFTER_FINISH);
      }
      //duration must be finish time - start time
      else if (duration != finish - start) {
         throw new OpEntityException(OpWorkError.DURATION_NOT_VALID);
      }
   }
}
