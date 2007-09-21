/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings.holiday_calendar;

/**
 * Instances of this class represent exceptions that can occur during holiday calendar parsing
 *
 * @author ovidiu.lupas
 */
public class OpHolidayCalendarException extends RuntimeException {

   public OpHolidayCalendarException(String message) {
      super(message);
   }
   public OpHolidayCalendarException(String message,Throwable cause) {
      super(message,cause);
   }
}
