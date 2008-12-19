/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.util.XCalendar;

/**
 * A calendar wrapper class used by the application.
 */
public final class OpProjectCalendar extends XCalendar implements Serializable {

   public static final XLog logger = XLogFactory.getLogger(OpProjectCalendar.class);

   public final static String ABSENCE_PUBLIC_HOLIDAY = "public_holiday";
   public final static String ABSENCE_PERSONAL_HOLIDAY = "personal_holiday";
   public final static String ABSENCE_VACATION = "vacation";
   public final static String ABSENCE_SICK_LEAVE = "sick";
   public final static String ABSENCE_OTHER = "other";
   
   static {
      logger.debug("{...}");
      if (getInstance() == null || !(getInstance() instanceof OpProjectCalendar)) {
         register(new OpProjectCalendar());
      }
   }
   
   /*
    * Things treated as absence. Don't forget to add your reason!
    */
   public static String[] ABSENCE_REASONS = { ABSENCE_PUBLIC_HOLIDAY,
      ABSENCE_PERSONAL_HOLIDAY, ABSENCE_VACATION, ABSENCE_SICK_LEAVE,
      ABSENCE_OTHER };

   public static String[] ABSENCE_REASONS_HOLIDAY = { ABSENCE_PUBLIC_HOLIDAY,
      ABSENCE_PERSONAL_HOLIDAY };

   public static SortedMap createHolidayMap(List dates, String reason) {
      SortedMap holidays = new TreeMap();
      Iterator hit = dates.iterator();
      while (hit.hasNext()) {
         Date h = (Date) hit.next();
         holidays.put(h, reason);
      }
      return holidays;   
   }
   
   private static Set absenceReasons = null;
   private static Set holidayReasons = null;
   
   private static boolean isAbsenceReason(String reason) {
      if (absenceReasons == null) {
         absenceReasons = new HashSet();
         for (int i = 0; i < ABSENCE_REASONS.length; i++) {
            absenceReasons.add(ABSENCE_REASONS[i]);
         }
      }
      return reason == null ? false : absenceReasons.contains(reason);
   }
   
   private static boolean isHolidayReason(String reason) {
      if (holidayReasons == null) {
         holidayReasons = new HashSet();
         for (int i = 0; i < ABSENCE_REASONS_HOLIDAY.length; i++) {
            holidayReasons.add(ABSENCE_REASONS_HOLIDAY[i]);
         }
      }
      return reason == null ? false : holidayReasons.contains(reason);
   }
   
   /**
    * Serial version UID
    */
   private static final long serialVersionUID = -1740946410331019950L;

   /**
    * The objects that incapsulates the planning settings for this calendar.
    */
   private PlanningSettings planningSettings = null;

   /**
    * Instances of OpProjectCalendar cannot be created from the outside.
    */
   public OpProjectCalendar() {
      configure(new PlanningSettings(), XLocaleManager.getDefaultLocale(), null, GMT_TIMEZONE);
   }

   /**
    * Configures the calendar from the map of settings.
    *
    * @param planningSettings a <code>PlanningSettings</code> object, representing this calendar's planning settings.
    *                         If <code>null</code> the calendar will be intialized with the default planning settings.
    * @param locale           XLocale to which the calendar is binded
    * @param localizer        XLocalizer used to localize the calendar related language resources
    * @param timezone         a <code>TimeZone</code> object used for configuring the calendar. If <code>null</code>,
    *                         then the GMT timezone is used.
    */
   public void configure(PlanningSettings planningSettings, XLocale locale, XLocalizer localizer, TimeZone timezone) {

      if (planningSettings == null && locale == null) {
         logger.info("Trying to configure OpProjectCalendar with empty settings....");
         return;
      }

      //planning settings
      setPlanningSettings(planningSettings);
      super.configure(locale, localizer, timezone);
   }

   /**
    * Returns the <code>PlanningSettings</code> of this <code>OpProjectCalendar</code> object.
    *
    * @return the <code>PlanningSettings</code> of this <code>OpProjectCalendar</code> object.
    */
   public PlanningSettings getPlanningSettings() {
      if (planningSettings == null) {
         return new PlanningSettings();
      }
      return planningSettings;
   }

   /**
    * Sets this calendar's planning settings to the <code>PlanningSettings</code> object passed as parameter.
    *
    * @param planningSettings a <code>PlanningSettings</code> object representing the planning settings. If <code>null</code>,
    *                         then the default planning settings are used.
    */
   public void setPlanningSettings(PlanningSettings planningSettings) {
      //planning settings
      if (planningSettings == null) {
         planningSettings = new PlanningSettings();
      }
      this.planningSettings = planningSettings;
      initWorkDays();
   }

   /**
    * Gets the first working day from the current calendar.
    *
    * @return a <code>int</code> representing a day.
    */
   public int getFirstWorkday() {
      return planningSettings.firstWorkday;
   }

   /**
    * Gets the last working day from the current calendar.
    *
    * @return a <code>int</code> representing a day.
    */
   public int getLastWorkday() {
      return planningSettings.lastWorkday;
   }

   /**
    * Gets the number of working hours per day.
    *
    * @return a <code>double</code> representing the number of working hours per day.
    */
   public double getWorkHoursPerDay() {
      return planningSettings.workHoursPerDay;
   }

   /**
    * Gets the number of working hours per week.
    *
    * @return a <code>double</code> representing the number of working hours per week.
    */
   public double getWorkHoursPerWeek() {
      return planningSettings.workHoursPerWeek;
   }

   /**
    * Gets the holidays this calendar has.
    *
    * @return a <code>SortedSet</code> representing this calendar's holidays or an empty <code>Set</code> if there are no
    *         holidays.
    */
   public SortedMap getHolidays() {
      return planningSettings.holidays;
   }

   /**
    * Returns the id of the holiday calendar used.
    *
    * @return a <code>String</code> the id of the holiday calendar or <code>null</code>
    *         if there isn't one.
    */
   public String getHolidayCalendarId() {
      return planningSettings.holidayCalendarId;
   }

   /**
    * Checks is the given date is a working day
    *
    * @param date Date to be tested
    * @return true if date is working day
    */
   public boolean isWorkDay(Date date) {
      String reason = (String) this.getHolidays().get(date);
      if (isAbsenceReason(reason)) {
         if (planningSettings.noHolidays && isHolidayReason(reason)) {
            return true;
         }
         return false;
      }
      calendarGMT.setTime(date);
      return planningSettings.noHolidays || isWorkDay(calendarGMT.get(Calendar.DAY_OF_WEEK));
   }

   public boolean isHoliday(Date date) {
      if (isHolidayReason((String) this.getHolidays().get(date))) {
         return true;
      }
      return false;
   }

   public boolean isHolidaysAreWorkDay() {
      return planningSettings.noHolidays;
   }
   /**
    * Class encapsulating planning specific settings, used by the calendar.
    */
   public static class PlanningSettings implements Serializable {

      private static final long serialVersionUID = 8681681536459096123L;

      /**
       * The first working day of the week).
       */
      private int firstWorkday = Calendar.MONDAY;

      /**
       * The last working day (of the week).
       */
      private int lastWorkday = Calendar.FRIDAY;

      /**
       * The number of working hours per day.
       */
      private double workHoursPerDay = 8;

      /**
       * The number of working hours per week.
       */
      private double workHoursPerWeek = 40;

      /**
       * The id of the holiday calendar
       */
      private String holidayCalendarId = null;

      /**
       * The holidays registered with this calendar.
       */
      private SortedMap holidays = new TreeMap(); // All holiday calendar dates, sorted by start date

      private boolean noHolidays = false;      

      /**
       * Creates a new planning settings object, overridding the default values.
       *
       * @param firstWorkday      a <code>int</code> representing the first day of the week.
       * @param lastWorkday       a <code>int</code> representing the last day of the week.
       * @param workHoursPerDay   a <code>double</code> representing the number of working hours per day.
       * @param workHoursPerWeek  a <code>double</code> representing the number of working hours per week.
       * @param holidays          a <code>SortedSet</code> representing the set of planning holidays.
       * @param holidayCalendarId a <code>String</code> the id of the holiday calendar used for planning, or <code>null</code> if there isn't any.
       */
      public PlanningSettings(int firstWorkday, int lastWorkday, double workHoursPerDay,
           double workHoursPerWeek, SortedMap holidays, String holidayCalendarId, boolean noHolidays) {
         this.firstWorkday = firstWorkday;
         this.lastWorkday = lastWorkday;
         this.workHoursPerDay = workHoursPerDay;
         this.workHoursPerWeek = workHoursPerWeek;
         this.holidays = (holidays != null) ? holidays : new TreeMap();
         this.holidayCalendarId = holidayCalendarId;
         this.noHolidays = noHolidays;
      }

      /**
       * No-arg constructor, which uses the default values.
       */
      private PlanningSettings() {
      }
      
      public boolean equals(Object o) {
         if (o instanceof PlanningSettings) {
            PlanningSettings ps = (PlanningSettings) o;
            boolean changed = false;
            changed = changed || (ps.firstWorkday != firstWorkday);
            changed = changed || (ps.lastWorkday != lastWorkday);
            changed = changed || (ps.workHoursPerDay != workHoursPerDay);
            changed = changed || (ps.noHolidays != noHolidays);
            if (ps.holidayCalendarId != null) {
               changed = changed || (holidayCalendarId == null);
               changed = changed || !ps.holidayCalendarId.equals(holidayCalendarId);
            }
            else {
               changed = changed || holidayCalendarId != null;
            }
            return !changed;
         }
         else {
            return false;
         }
      }
      
   }

   /**
    * Returns a <code>List</code> containing the working days in an interval.
    *
    * @param start - the start date of the interval
    * @param end   - the end date of the interval
    * @return - a <code>List</code> containing the working days in an interval.
    */
   public List getWorkingDaysFromInterval(Date start, Date end) {
      List workingDays = new ArrayList();
      Date nextWorkingDay = null;

      if (isWorkDay(start)) {
         nextWorkingDay = start;
      }
      else {
         nextWorkingDay = nextWorkDay(start);
      }

      while (!nextWorkingDay.after(end)) {
         workingDays.add(nextWorkingDay);
         nextWorkingDay = nextWorkDay(nextWorkingDay);
      }
      return workingDays;
   }
   
   public int countWorkDaysBetween(Date start, Date end) {
      boolean forward = true;
      if (start.after(end)) {
         Date tmp = end;
         end = start;
         start = tmp;
         forward = false;
      }
      int count = 0;
      while (start.before(end)) {
         if (isWorkDay(start)) {
            count++;
         }
         start = new Date(start.getTime() + MILLIS_PER_DAY);
      }
      return forward ? count : -count;
   }

   /**
    * Returns the GMT calendar set to the date passed as parameter and hours, minutes, seconds, milliseconds set to 0.
    *
    * @param date - the date to be set on the calendar
    * @return - the GMT calendar set to the date passed as parameter and hours, minutes, seconds, milliseconds set to 0.
    */
   public static Calendar setCalendarTimeToZero() {
      return setCalendarTimeToZero(null);
   }
   public static Calendar setCalendarTimeToZero(java.util.Date date) {
      Calendar calendar = OpProjectCalendar.getDefaultProjectCalendar().calendarGMT;
      if (date != null) {
         calendar.setTime(date);
      }
      calendar.set(Calendar.HOUR, 0);
      calendar.set(Calendar.MINUTE, 0);
      calendar.set(Calendar.SECOND, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      return calendar;
   }

   /**
    * Creates a new calendar instance, based on the current instance, using serialization/deserialization
    * mechanism.
    * @return a  <code>OpProjectCalendar</code> instance, different from "this", but with the same
    * information.
    */
   public XCalendar copyInstance() {
      try {
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         ObjectOutputStream os = new ObjectOutputStream(byteArrayOutputStream);
         os.writeObject(this);
         os.flush();
         os.close();
         ByteArrayInputStream in = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
         ObjectInputStream ois = new ObjectInputStream(in);
         OpProjectCalendar result = (OpProjectCalendar) ois.readObject();
         ois.close();
         return result;
      }
      catch (Exception e) {
         logger.error("Cannot copy  calendar instance, returning identical instance", e);
         return this;
      }
   }
   
   public static OpProjectCalendar getDefaultProjectCalendar() {
      return (OpProjectCalendar)XCalendar.getDefaultCalendar();
   }

   protected  XCalendar newCalendarInstance() {
      return new OpProjectCalendar();
   }

}
