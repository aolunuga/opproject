/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.schedule;

import onepoint.express.XTimer;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

public class OpScheduler {

   // *** All schedules are stored in GMT/UST (use TimeZont "GMT"?)

   // Constants for scheduling units
   public final static int DAYS = 1;
   public final static int WEEKS = 2;
   public final static int MONTHS = 3;
   public final static int YEARS = 4;

   // String constant for scheduler watch (of timer)
   public final static String SCHEDULER = "scheduler";

   // schedule timers map
   private static Map scheduleTimers = new HashMap();

   // event map params
   public static final String LAST_EXECUTED_DATE = "last-executed-date";
   public static final String SCHEDULE_NAME = "schedule-name";

   private OpScheduler() {
   }

   /**
    * Performs initialization
    */
   public static void start() {

   }

   /**
    * Performs schedule timers clear
    */
   public static void stop() {
      Iterator timers = scheduleTimers.values().iterator();
      while (timers.hasNext()) {
         XTimer timer = (XTimer) timers.next();
         timer.clearWatch(SCHEDULER);
         timer.stopTimer();

      }
      scheduleTimers.clear();
      scheduleTimers = null;
   }


   /**
    * Returns the next execution date for the given <code>schedule</code> based on time unit and interval
    *
    * @param schedule <code>OpSchedule</code> for which is calculated the next execution time
    * @return <code>long</code> representing the next execution time
    */
   private static long getNextExecutionTime(OpSchedule schedule) {
      Date lastExecutedDate = schedule.getLastExecuted();
      long nextExecutionTime = schedule.getStart().getTime();
      int timeInterval = schedule.getInterval();

      switch (schedule.getUnit()) {
         case DAYS:
            // take the last execution at 00:00 hours then, we add the number of days and finally we add the time again
            if (lastExecutedDate != null) { //schedule has been executed once
               nextExecutionTime = lastExecutedDate.getTime() - (lastExecutedDate.getTime() % XCalendar.MILLIS_PER_DAY);
            }
            nextExecutionTime += XCalendar.MILLIS_PER_DAY * timeInterval;
            return nextExecutionTime;

         case WEEKS:
            Calendar nextExecutionCalendar = XCalendar.getDefaultCalendar().getCalendar();
            nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
            if (lastExecutedDate != null) { //schedule has been executed once
               nextExecutionTime = lastExecutedDate.getTime() - (lastExecutedDate.getTime() % XCalendar.MILLIS_PER_DAY);
               nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
            }
            int nextWeek = nextExecutionCalendar.get(Calendar.WEEK_OF_YEAR) + timeInterval;
            nextExecutionCalendar.set(Calendar.WEEK_OF_YEAR, nextWeek);
            return nextExecutionCalendar.getTimeInMillis();

         case MONTHS:
            nextExecutionCalendar = XCalendar.getDefaultCalendar().getCalendar();
            nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
            if (lastExecutedDate != null) { //schedule has been executed once
               nextExecutionTime = lastExecutedDate.getTime() - (lastExecutedDate.getTime() % XCalendar.MILLIS_PER_DAY);
               nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
            }
            int nextMonth = nextExecutionCalendar.get(Calendar.MONTH) + timeInterval;
            nextExecutionCalendar.set(Calendar.MONTH, nextMonth);
            return nextExecutionCalendar.getTimeInMillis();

         case YEARS:
            nextExecutionCalendar = XCalendar.getDefaultCalendar().getCalendar();
            nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
            if (lastExecutedDate != null) { //schedule has been executed once
               nextExecutionTime = lastExecutedDate.getTime() - (lastExecutedDate.getTime() % XCalendar.MILLIS_PER_DAY);
               nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
            }
            int nextYear = nextExecutionCalendar.get(Calendar.YEAR) + timeInterval;
            nextExecutionCalendar.set(Calendar.YEAR, nextYear);
            return nextExecutionCalendar.getTimeInMillis();
         default:
            throw new UnsupportedOperationException("Schedule unit time not supported");
      }
   }


   /**
    * Schedules an execution for the give <code>schedule</code>. If a schedule with the same is not found it is
    * persisted first and then the scheduling is performed.
    *
    * @param session  <code>OpProjectSession</code>
    * @param schedule <code>OpSchedule</code>
    */
   public static void registerSchedule(OpProjectSession session, OpSchedule schedule) {

      String scheduleName = schedule.getName();
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule where schedule.Name = ?");
      query.setString(0, scheduleName);
      List schedules = broker.list(query);

      //timer event map
      HashMap eventMap = new HashMap();
      eventMap.put(SCHEDULE_NAME, scheduleName);

      XTimer scheduleTimer = new XTimer();
      long timerFirstDelay = getTimerInterval(schedule);
      long timerInterval = getTimerInterval(schedule);

      //persist schedule if not found
      if (schedules.isEmpty()) {
         OpTransaction tx = broker.newTransaction();
         broker.makePersistent(schedule);
         tx.commit();
      }
      else {
         OpSchedule persistedSchedule = (OpSchedule) schedules.get(0);

         Date lastExecutedDate;
         long now = System.currentTimeMillis();
         long nextExecutionTime = getNextExecutionTime(persistedSchedule);

         //find the first "next" execution time for the schedule that is before now and try to execute it
         if (now >= nextExecutionTime) {
            if (now - nextExecutionTime > timerInterval) {
               int intervalNr = (int) ((now - nextExecutionTime) / timerInterval);
               //the last execution date before now
               lastExecutedDate = new Date(nextExecutionTime + (intervalNr * timerInterval));
            }
            else {
               lastExecutedDate = new Date(nextExecutionTime);
            }

            //send the event for processing
            eventMap.put(LAST_EXECUTED_DATE, lastExecutedDate);
            schedule.getHandler().processEvent(eventMap);
            //next date of execution
            nextExecutionTime += timerInterval;
         }

         //the first delay
         timerFirstDelay = Math.abs(nextExecutionTime - now);
         //and the interval
         timerInterval = getTimerInterval(persistedSchedule);
      }

      scheduleTimer.setWatch(SCHEDULER, schedule.getHandler(), timerFirstDelay, timerInterval, eventMap);

      //finally register schedule timer 
      scheduleTimers.put(scheduleName, scheduleTimer);
      broker.close();
   }

   /**
    * Returns the timer interval according to given <code>schedule</code> time unit and interval
    *
    * @param schedule <code>OpSchedule</code>
    * @return <code>long</code> the timer interval
    */
   private static long getTimerInterval(OpSchedule schedule) {
      int timeInterval = schedule.getInterval();
      switch (schedule.getUnit()) {
         case DAYS:
            return XCalendar.MILLIS_PER_DAY * timeInterval;
         case WEEKS:
            return 7 * XCalendar.MILLIS_PER_DAY * timeInterval;
         case MONTHS:
            //<FIXME author="Ovidiu Lupas" description="hard coded 30 days per month">
            return 30 * XCalendar.MILLIS_PER_DAY * timeInterval;
            //</FIXME>
         case YEARS:
            //<FIXME author="Ovidiu Lupas" description="hard coded 365 days per year">
            return 365 * XCalendar.MILLIS_PER_DAY * timeInterval;
            //</FIXME>
         default:
            throw new UnsupportedOperationException("Schedule unit time not supported");
      }
   }

   /**
    * Sets up the schedule timer interval for the given <code>scheduleName</code>
    *
    * @param session      <code>OpProjectSession</code>
    * @param scheduleName <code>String</code> representing the schedule name
    * @param newInterval  <code>long</code> value representing the new schedule interval
    */
   public static void updateScheduleInterval(OpProjectSession session, String scheduleName, int newInterval) {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule where schedule.Name = ?");
      query.setString(0, scheduleName);
      List schedules = broker.list(query);

      if (schedules.isEmpty()) {
         broker.close();
         return; //nothing to update
      }
      //perform update only if necesary
      OpSchedule schedule = (OpSchedule) schedules.get(0);

      if (schedule.getInterval() != newInterval) {

         OpTransaction tx = broker.newTransaction();
         schedule.setInterval(newInterval);
         broker.updateObject(schedule);
         tx.commit();

         XTimer scheduleTimer = (XTimer) scheduleTimers.get(scheduleName);
         /*timer event map */
         HashMap eventMap = new HashMap();
         eventMap.put(SCHEDULE_NAME, scheduleName);
         long timerFirstDelay = getTimerInterval(schedule);
         long timerInterval = getTimerInterval(schedule);
         /*clear timer and set a watch again */
         scheduleTimer.clearWatch(SCHEDULER);
         scheduleTimer.setWatch(SCHEDULER, schedule.getHandler(), timerFirstDelay, timerInterval, eventMap);
      }
      broker.close();

   }
}
