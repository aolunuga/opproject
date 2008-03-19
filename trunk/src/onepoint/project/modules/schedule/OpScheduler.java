/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.schedule;

import java.sql.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import onepoint.express.XEventHandler;
import onepoint.express.XTimer;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.util.XCalendar;

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
   private static Map<String, XTimer> scheduleTimers = new HashMap<String, XTimer>();

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
               nextExecutionTime += XCalendar.MILLIS_PER_DAY * timeInterval;
            }
            return nextExecutionTime;

         case WEEKS:
            Calendar nextExecutionCalendar = XCalendar.getDefaultCalendar().getCalendar();
            nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
            if (lastExecutedDate != null) { //schedule has been executed once
               nextExecutionTime = lastExecutedDate.getTime() - (lastExecutedDate.getTime() % XCalendar.MILLIS_PER_DAY);
               nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
               int nextWeek = nextExecutionCalendar.get(Calendar.WEEK_OF_YEAR) + timeInterval;
               nextExecutionCalendar.set(Calendar.WEEK_OF_YEAR, nextWeek);
            }
            return nextExecutionCalendar.getTimeInMillis();

         case MONTHS:
            nextExecutionCalendar = XCalendar.getDefaultCalendar().getCalendar();
            nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
            if (lastExecutedDate != null) { //schedule has been executed once
               nextExecutionTime = lastExecutedDate.getTime() - (lastExecutedDate.getTime() % XCalendar.MILLIS_PER_DAY);
               nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
               int nextMonth = nextExecutionCalendar.get(Calendar.MONTH) + timeInterval;
               nextExecutionCalendar.set(Calendar.MONTH, nextMonth);
            }
            return nextExecutionCalendar.getTimeInMillis();

         case YEARS:
            nextExecutionCalendar = XCalendar.getDefaultCalendar().getCalendar();
            nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
            if (lastExecutedDate != null) { //schedule has been executed once
               nextExecutionTime = lastExecutedDate.getTime() - (lastExecutedDate.getTime() % XCalendar.MILLIS_PER_DAY);
               nextExecutionCalendar.setTimeInMillis(nextExecutionTime);
               int nextYear = nextExecutionCalendar.get(Calendar.YEAR) + timeInterval;
               nextExecutionCalendar.set(Calendar.YEAR, nextYear);
            }
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
      try {
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
         OpSchedule persistedSchedule;
         OpTransaction tx = broker.newTransaction();
         if (schedules.isEmpty()) {
            broker.makePersistent(schedule);
            persistedSchedule = schedule;
         }
         else {
            persistedSchedule = (OpSchedule) schedules.get(0);
            persistedSchedule.setName(schedule.getName());
            persistedSchedule.setDescription(schedule.getDescription());
            persistedSchedule.setStart(schedule.getStart());
            persistedSchedule.setUnit(schedule.getUnit());
            persistedSchedule.setInterval(schedule.getInterval());
            persistedSchedule.setMask(schedule.getMask());
            broker.updateObject(persistedSchedule);
         }
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
            
            // update schedlue to reflect execution:
            persistedSchedule.setLastExecuted(new java.sql.Date(System.currentTimeMillis()));
            broker.updateObject(schedule);
            //next date of execution
            nextExecutionTime += timerInterval;

            //the first delay
            timerFirstDelay = Math.abs(nextExecutionTime - now);
            //and the interval
            timerInterval = getTimerInterval(persistedSchedule);
         }

         tx.commit();
         scheduleTimer.setWatch(SCHEDULER, schedule.getHandler(), timerFirstDelay, timerInterval, eventMap);

         //finally register schedule timer 
         scheduleTimers.put(scheduleName, scheduleTimer);
      }
      finally {
         broker.closeAndEvict();
      }
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
      try {
         OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule where schedule.Name = ?");
         query.setString(0, scheduleName);
         List schedules = broker.list(query);

         if (schedules.isEmpty()) {
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
            if (scheduleTimer != null) {
               scheduleTimer.clearWatch(SCHEDULER);
               scheduleTimer.setWatch(SCHEDULER, schedule.getHandler(), timerFirstDelay, timerInterval, eventMap);
            }
         }
      }
      finally {
         broker.close();
      }
   }

   /**
    * Schedules an execution for the give <code>schedule</code>. If a schedule with the same is not found it is
    * persisted first and then the scheduling is performed.
    *
    * @param session  <code>OpProjectSession</code>
    * @param schedule <code>OpSchedule</code>
    */
   public static void startSchedule(OpProjectSession session, String scheduleName, XEventHandler handler) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule where schedule.Name = ?");
         query.setString(0, scheduleName);
         List<OpSchedule> schedules = broker.list(query);
   
         //timer event map
         HashMap<String, Comparable> eventMap = new HashMap<String, Comparable>();
         eventMap.put(SCHEDULE_NAME, scheduleName);
   
         XTimer scheduleTimer = new XTimer();
   
         OpSchedule persistedSchedule = schedules.get(0);
         //long timerFirstDelay = getTimerInterval(schedule);
         long timerInterval = getTimerInterval(persistedSchedule);
   
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
            handler.processEvent(eventMap);
            //next date of execution
            nextExecutionTime += timerInterval;
         }
   
         //the first delay
         long timerFirstDelay = Math.abs(nextExecutionTime - now);
         //and the interval
         timerInterval = getTimerInterval(persistedSchedule);
   
         scheduleTimer.setWatch(SCHEDULER, handler, timerFirstDelay, timerInterval, eventMap);
   
         //finally register schedule timer 
         scheduleTimers.put(scheduleName, scheduleTimer);
      }
      finally {
         broker.close();
      }
   }

   /**
    * @param session
    * @return
    * @pre
    * @post
    */
   public static List<OpSchedule> getSchedulers(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule");
         return broker.list(query, OpSchedule.class);
      }
      finally {
         broker.close();
      }
   }

   public static void unregisterSchedule(OpProjectSession session, String scheduleName) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule where schedule.Name = ?");
         query.setString(0, scheduleName);
         List schedules = broker.list(query);
   
         //persist schedule if not found
         if (schedules.isEmpty()) {
            return;
         }
         OpTransaction tx = broker.newTransaction();
         broker.deleteObject((OpSchedule)schedules.get(0));
         tx.commit();

         //finally unregister schedule timer 
         scheduleTimers.remove(scheduleName);
      }
      finally {
         broker.close();
      }
   }
}
