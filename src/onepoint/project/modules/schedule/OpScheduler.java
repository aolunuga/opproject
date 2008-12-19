/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.schedule;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import onepoint.express.XEventHandler;
import onepoint.express.XTimer;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpProjectCalendar;

public class OpScheduler implements XEventHandler {

   private static final XLog logger = XLogFactory.getLogger(OpScheduler.class);
   

   // Constants for scheduling units
   public final static int DAYS = 1;
   public final static int WEEKS = 2;
   public final static int MONTHS = 3;
   public final static int YEARS = 4;

   // String constant for scheduler watch (of timer)
   public final static String SCHEDULER = "OpScheduler";

   private SortedMap<Long, Map<String, Set<OpSchedule>>> jobQueue = new TreeMap<Long, Map<String, Set<OpSchedule>>>(new Comparator<Long>() {
      public int compare(Long o1, Long o2) {
         return o1.compareTo(o2);
      }});
   
   private Object queueMutex = new Object();
   private XTimer scheduleTimer = new XTimer(SCHEDULER);

   private static OpScheduler instance = null;
   
   
   // event map params
   public static final String LAST_EXECUTED_DATE = "last-executed-date";
   public static final String SCHEDULE_NAME = "schedule-name";

   private OpScheduler() {
   }

   public static OpScheduler getInstance() {
      if (instance == null) {
         instance = new OpScheduler();
      }
      return instance;
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
      getInstance().scheduleTimer.clearWatch(SCHEDULER);
      getInstance().scheduleTimer.stopTimer();
   }

   /**
    * Returns the next execution date for the given <code>schedule</code> based on time unit and interval
    *
    * @param schedule <code>OpSchedule</code> for which is calculated the next execution time
    * @return <code>long</code> representing the next execution time
    */
   private static long getNextExecutionTime(OpSchedule schedule) {

      long nextExecutionMillis = schedule.getStart().getTime();
      long lastExecutionMillis = nextExecutionMillis;

      Calendar nextCalendar = OpProjectCalendar.getDefaultProjectCalendar().cloneCalendarInstance();
      nextCalendar.setTimeInMillis(nextExecutionMillis);

      Date lastExecutedDate = schedule.getLastExecuted();
      if (lastExecutedDate != null) {
         int timeInterval = schedule.getInterval();
         lastExecutionMillis = lastExecutedDate.getTime();
         
         Calendar tmpCalendar = OpProjectCalendar.getDefaultProjectCalendar().cloneCalendarInstance();
         tmpCalendar.setTimeInMillis(lastExecutionMillis);

         switch (schedule.getUnit()) {
            case DAYS:
               // take the last execution at 00:00 hours then, we add the number of days and finally we add the time again
               long diffDaysFromStart = (lastExecutionMillis - nextExecutionMillis) / OpProjectCalendar.MILLIS_PER_DAY;
               diffDaysFromStart = diffDaysFromStart - diffDaysFromStart % timeInterval + (diffDaysFromStart == 0 ? timeInterval : 0);
               nextCalendar.add(Calendar.DAY_OF_MONTH, (int) diffDaysFromStart);
               break;
   
            case WEEKS:
               long diffWeeksFromStart = (lastExecutionMillis - nextExecutionMillis) / OpProjectCalendar.MILLIS_PER_WEEK;
               diffWeeksFromStart = diffWeeksFromStart - diffWeeksFromStart % timeInterval + (diffWeeksFromStart == 0 ? timeInterval : 0);
               nextCalendar.add(Calendar.WEEK_OF_YEAR, (int) diffWeeksFromStart);
               break;
   
            case YEARS:
               timeInterval = timeInterval * 12;
            case MONTHS:
               int absoluteMonth = tmpCalendar.get(Calendar.YEAR) * 12;
               absoluteMonth += tmpCalendar.get(Calendar.MONTH);
               int startAbsoluteMonth = nextCalendar.get(Calendar.YEAR) * 12;
               startAbsoluteMonth += nextCalendar.get(Calendar.MONTH);
               
               int diffMonth = (absoluteMonth - startAbsoluteMonth);
               diffMonth = diffMonth - diffMonth % timeInterval + (diffMonth == 0 ? timeInterval : 0);
               nextCalendar.add(Calendar.MONTH, diffMonth);
               break;
   
            default:
               throw new UnsupportedOperationException("Schedule unit time not supported");
         }
      }
      nextExecutionMillis = nextCalendar.getTimeInMillis();
      return nextExecutionMillis;
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
         OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule where schedule.Name = :name");
         query.setString("name", scheduleName);
         List schedules = broker.list(query);

         //persist schedule if not found
         OpSchedule persistedSchedule;
         OpTransaction tx = broker.newTransaction();
         if (schedules.isEmpty()) {
            broker.makePersistent(schedule);
            schedule.setCreated(new Timestamp(System.currentTimeMillis()));
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
            persistedSchedule.setHandler(schedule.getHandler());
            broker.updateObject(persistedSchedule);
         }
         tx.commit();
         getInstance().addJobToQueue(session.getSourceName(), persistedSchedule);
         
      }
      finally {
         broker.closeAndEvict();
      }
   }

   private void addJobToQueue(String sessionName, OpSchedule schedule) {
      logger.debug("addJobToQueue - session: " + sessionName + " schedule: " + schedule.getName());
      synchronized(queueMutex) {
         long when = getNextExecutionTime(schedule);
         insertJobIntoQueue(sessionName, when, schedule);
      }
      processJobQueue();
   }

   private void insertJobIntoQueue(String sessionName, long when, OpSchedule schedule) {
      if (when == -1) {
         return;
      }
      Long wHen = new Long(when);
      Map<String, Set<OpSchedule>> sessionMap = jobQueue.get(wHen);
      if (sessionMap == null) {
         sessionMap = new HashMap<String, Set<OpSchedule>>();
         jobQueue.put(when, sessionMap);
      }
      Set<OpSchedule> schedulesForSession = sessionMap.get(sessionName);
      if (schedulesForSession == null) {
         schedulesForSession = new HashSet<OpSchedule>();
         sessionMap.put(sessionName, schedulesForSession);
      }
      schedulesForSession.add(schedule);
   }
   
   private void removeJobFromQueue(String sessionName, OpSchedule schedule) {
      synchronized(queueMutex) {
         Long wHen = new Long(getNextExecutionTime(schedule));
         Map<String, Set<OpSchedule>> sessionMap = jobQueue.get(wHen);
         if (sessionMap != null) {
            Set<OpSchedule> schedulesForSession = sessionMap.get(sessionName);
            if (schedulesForSession != null) {
               schedulesForSession.remove(schedule);
            }
         }
      }
   }
   
   private void processJobQueue() {
      Long nextTime = null;
      long now = System.currentTimeMillis();
      synchronized(queueMutex) {
         while (true) {
            Iterator<Long> it = jobQueue.keySet().iterator();
            if (!it.hasNext()) {
               break;
            }
            nextTime = it.next();
            if (nextTime.longValue() <= now) {
               // do something:
               Map<String, Set<OpSchedule>> sessionScheduleMap = jobQueue.get(nextTime);
               Iterator<String> sessionNameIterator = sessionScheduleMap.keySet().iterator();
               while (sessionNameIterator.hasNext()) {
                  String sessionName = sessionNameIterator.next();
                  Set<OpSchedule> schedules = sessionScheduleMap.get(sessionName);
                  if (schedules != null && !schedules.isEmpty()) {
                     OpProjectSession schedulerSession = new OpProjectSession(sessionName);
                     OpBroker broker = schedulerSession.newBroker();
                     try {
                        OpTransaction tx = broker.newTransaction();
                        Iterator<OpSchedule> scheduleIterator = schedules.iterator();
                        while (scheduleIterator.hasNext()) {
                           OpSchedule currentSchedule = scheduleIterator.next();
                           long nextExecutionTime = executeSchedule(schedulerSession, broker, currentSchedule);
                           if (nextExecutionTime != -1) {
                              insertJobIntoQueue(sessionName, nextExecutionTime, currentSchedule);
                           }
                        }
                        tx.commit();
                     }
                     finally {
                        broker.closeAndEvict();
                        schedulerSession.close();
                     }
                  }
               }
               // remove from queue...
               jobQueue.remove(nextTime);
            }
            else {
               break;
            }
         }
      }
      if (nextTime != null) {
         long delay = nextTime.longValue() - now;
         if (delay < 0) {
            delay = 1;
         }
         logger.debug("reschedule after (ms): " + delay);
         HashMap eventMap = new HashMap();
         scheduleTimer.clearWatch(SCHEDULER);
         scheduleTimer.setWatch(SCHEDULER, this, delay, 0, eventMap);
      }
   }
   
   private long executeSchedule(OpProjectSession session, OpBroker broker, OpSchedule schedule) {
      logger.debug("executeSchedule: " + schedule.getName());

      HashMap eventMap = new HashMap();
      eventMap.put(SCHEDULE_NAME, schedule.getName());
      eventMap.put(LAST_EXECUTED_DATE, schedule.getLastExecuted());
      schedule.getHandler().processEvent(eventMap);

      long nextExecutionTime  = -1;
      // update schedlue to reflect execution:
      OpSchedule persistetSchedule = (OpSchedule) broker.getObject(schedule.locator());
      if (persistetSchedule != null) {
         nextExecutionTime = getNextExecutionTime(schedule);
         persistetSchedule.setLastExecuted(new java.sql.Date(System.currentTimeMillis()));
         broker.updateObject(persistetSchedule);
         schedule.getHandler().processEvent(eventMap);
      }
      return nextExecutionTime;
   }
   
   /**
    * Sets up the schedule timer interval for the given <code>scheduleName</code>
    *
    * @param session      <code>OpProjectSession</code>
    * @param scheduleName <code>String</code> representing the schedule name
    * @param newInterval  <code>long</code> value representing the new schedule interval
    */
   public static void updateScheduleInterval(OpProjectSession session, String scheduleName, int newInterval) {
      logger.debug("updateScheduleInterval: " + scheduleName);
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
            getInstance().removeJobFromQueue(session.getSourceName(), schedule);
            
            OpTransaction tx = broker.newTransaction();
            schedule.setInterval(newInterval);
            broker.updateObject(schedule);
            tx.commit();

            getInstance().addJobToQueue(session.getSourceName(), schedule);
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
      logger.debug("startSchedule: " + scheduleName);
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule where schedule.Name = :name");
         query.setString("name", scheduleName);
         List<OpSchedule> schedules = broker.list(query);
   
         XTimer scheduleTimer = new XTimer(SCHEDULER + ":" + scheduleName);
   
         OpSchedule persistedSchedule = schedules.get(0);
         persistedSchedule.setHandler(handler);
         getInstance().addJobToQueue(session.getSourceName(), persistedSchedule);
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
      logger.debug("unregisterSchedule: " + scheduleName);
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule where schedule.Name = ?");
         query.setString(0, scheduleName);
         List schedules = broker.list(query);
   
         //persist schedule if not found
         if (schedules.isEmpty()) {
            return;
         }
         OpSchedule schedule = (OpSchedule)schedules.get(0);
         getInstance().removeJobFromQueue(session.getSourceName(), schedule);

         OpTransaction tx = broker.newTransaction();
         broker.deleteObject(schedule);
         tx.commit();
      }
      finally {
         broker.close();
      }
   }

   public void processEvent(HashMap event) {
      logger.debug("processEvent");
      processJobQueue();
   }
}
