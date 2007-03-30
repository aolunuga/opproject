/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.schedule.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.schedule.OpSchedule;
import onepoint.project.modules.schedule.OpScheduler;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.test.OpBaseTestCase;

import java.sql.Date;
import java.util.Iterator;
import java.util.List;

/**
 * TODO: Document HERE!!!
 *
 * @author lucian.furtos
 */
public class OpSchedulerTest extends OpBaseTestCase {
   private static final String SCHEDULE_NAME = "S1";

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      cleanDB();
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      cleanDB();
      super.tearDown();
   }

   public void testRegisterSchedulerOne()
        throws Exception {
      OpSchedule schedule = new OpSchedule();
      schedule.setName(SCHEDULE_NAME);
      schedule.setStart(new Date(System.currentTimeMillis() - 10000));
      schedule.setInterval(1000);
      schedule.setUnit(OpScheduler.DAYS);
      schedule.setLastExecuted(new Date(System.currentTimeMillis()));
      OpScheduler.registerSchedule(session, schedule);
      OpSchedule actual = getScheduleByName(SCHEDULE_NAME);
      assertNotNull(actual);

      schedule.setUnit(OpScheduler.WEEKS);
      schedule.setStart(new Date(System.currentTimeMillis() - 5000));
      OpScheduler.registerSchedule(session, schedule);
   }

   public void testRegisterSchedulerTwo()
        throws Exception {
      OpSchedule schedule = new OpSchedule();
      schedule.setName(SCHEDULE_NAME);
      schedule.setStart(new Date(System.currentTimeMillis() + 1000));
      schedule.setInterval(3);
      schedule.setUnit(OpScheduler.WEEKS);
      schedule.setLastExecuted(new Date(System.currentTimeMillis()));
      OpScheduler.registerSchedule(session, schedule);
      OpSchedule actual = getScheduleByName(SCHEDULE_NAME);
      assertNotNull(actual);

      schedule.setUnit(OpScheduler.MONTHS);
      schedule.setStart(new Date(System.currentTimeMillis() - 10000));
      OpScheduler.registerSchedule(session, schedule);
   }

   public void testRegisterSchedulerThree()
        throws Exception {
      OpSchedule schedule = new OpSchedule();
      schedule.setName(SCHEDULE_NAME);
      schedule.setStart(new Date(System.currentTimeMillis() + 1000));
      schedule.setInterval(3);
      schedule.setUnit(OpScheduler.MONTHS);
      schedule.setLastExecuted(new Date(System.currentTimeMillis()));
      OpScheduler.registerSchedule(session, schedule);
      OpSchedule actual = getScheduleByName(SCHEDULE_NAME);
      assertNotNull(actual);

      schedule.setUnit(OpScheduler.YEARS);
      schedule.setStart(new Date(System.currentTimeMillis() - 10000));
      OpScheduler.registerSchedule(session, schedule);
   }

   public void testRegisterSchedulerFour()
        throws Exception {
      OpSchedule schedule = new OpSchedule();
      schedule.setName(SCHEDULE_NAME);
      schedule.setStart(new Date(System.currentTimeMillis() + 1000));
      schedule.setInterval(3);
      schedule.setUnit(OpScheduler.YEARS);
      schedule.setLastExecuted(new Date(System.currentTimeMillis()));
      OpScheduler.registerSchedule(session, schedule);
      OpSchedule actual = getScheduleByName(SCHEDULE_NAME);
      assertNotNull(actual);

      schedule.setUnit(OpScheduler.DAYS);
      schedule.setStart(new Date(System.currentTimeMillis() - 10000));
      OpScheduler.registerSchedule(session, schedule);
   }

   public void testUpdateScheduler()
        throws Exception {
      OpSchedule schedule = new OpSchedule();
      schedule.setName(SCHEDULE_NAME);
      schedule.setStart(new Date(System.currentTimeMillis()));
      schedule.setInterval(10);
      schedule.setUnit(OpScheduler.DAYS);
      schedule.setLastExecuted(new Date(System.currentTimeMillis()));
      OpScheduler.registerSchedule(session, schedule);
      OpSchedule actual = getScheduleByName(SCHEDULE_NAME);
      assertNotNull(actual);

      OpScheduler.updateScheduleInterval(session, SCHEDULE_NAME, 1000);
      actual = getScheduleByName(SCHEDULE_NAME);
      assertEquals(1000, actual.getInterval());

      OpScheduler.updateScheduleInterval(session, SCHEDULE_NAME, 1000);
   }

   //                     ******* Helper Methods *******

   private void cleanDB()
        throws Exception {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("from OpSchedule");
      List schedules = broker.list(query);
      OpTransaction tx = broker.newTransaction();
      String reportSchedulerName = OpSettings.get(OpSettings.REPORT_ARCHIVE_SCHEDULE_NAME);
      for (Iterator iterator = schedules.iterator(); iterator.hasNext();) {
         OpSchedule schedule = (OpSchedule) iterator.next();
         if (!reportSchedulerName.equals(schedule.getName())) {
            broker.deleteObject(schedule);
         }
      }
      tx.commit();
      broker.close();
   }

   private OpSchedule getScheduleByName(String name)
        throws Exception {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule where schedule.Name = ?");
      query.setString(0, name);
      query.setMaxResults(1);
      List schedules = broker.list(query);
      return (OpSchedule) schedules.get(0);
   }

}
