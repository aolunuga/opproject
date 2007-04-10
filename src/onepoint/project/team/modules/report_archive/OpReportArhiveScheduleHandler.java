/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.report_archive;

import onepoint.express.XEventHandler;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpPersistenceManager;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.schedule.OpSchedule;
import onepoint.project.modules.schedule.OpScheduler;
import onepoint.util.XCalendar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Scheduler handler class for reports archive
 * @author ovidiu.lupas
 */
public class OpReportArhiveScheduleHandler implements XEventHandler {

   private static final XLog logger = XLogFactory.getLogger(OpReportArhiveScheduleHandler.class, true);

   /**
    * Performs report archive scheduler processing
    */
   public void processEvent(HashMap event) {
      logger.info("Process report archive schedule");

      String scheduleName = (String) event.get(OpScheduler.SCHEDULE_NAME);
      OpBroker broker = OpPersistenceManager.newBroker();
      // Get schedule from database
      OpQuery query = broker.newQuery("select schedule from OpSchedule as schedule where schedule.Name = ?");
      query.setString(0, scheduleName);
      List schedules = broker.list(query);

      if (schedules.isEmpty()) {
         broker.close();
         return;//should not happen under normal circumstances
      }
      OpSchedule reportSchedule = (OpSchedule) schedules.get(0);

      query = broker.newQuery("select report from OpReport as report");
      Iterator reportsIterator = broker.iterate(query);

      //..and delete them if created date is before execution date
      java.util.Date lastExecuted = (java.util.Date) event.get(OpScheduler.LAST_EXECUTED_DATE);
      if (lastExecuted == null) { //timer event
         lastExecuted = new java.util.Date(System.currentTimeMillis());
      }

      OpTransaction tx = broker.newTransaction();
      while (reportsIterator.hasNext()) {
         OpReport report = (OpReport) reportsIterator.next();
         if (report.getCreated().before(lastExecuted)) {
            broker.deleteObject(report);
         }
      }
      reportSchedule.setLastExecuted(new java.sql.Date(lastExecuted.getTime()));
      broker.updateObject(reportSchedule);
      tx.commit();
      broker.close();

   }
}
