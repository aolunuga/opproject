/*
 * Copyright(c) OnePoint Software GmbH 2005. All Rights Reserved.
 */

package onepoint.project.team.modules.report_archive;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.module.OpTool;
import onepoint.project.module.OpToolManager;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.schedule.OpSchedule;
import onepoint.project.modules.schedule.OpScheduler;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpPermission;

import java.sql.Date;
import java.util.Iterator;

/**
 * Module class for the report archive.
 *
 * @author horia.chiorean
 */
public class OpReportArchiveModule extends OpModule {

   /**
    * Access level for reports.
    */
   public static final byte REPORT_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER + OpPermission.CONTRIBUTOR
        + OpPermission.OBSERVER;

   /**
    * Module constants
    */
   private static final String REPORT_LIST_TOOLNAME = "report";
   private static final String REPORT_LIST_START_FORM = "/team/modules/report_archive/forms/report.oxf.xml";

   public void start(OpProjectSession session) {
      //over-write the report list form
      OpTool reportListTool = OpToolManager.getTool(REPORT_LIST_TOOLNAME);
      reportListTool.setStartForm(REPORT_LIST_START_FORM);
      
      //create a report archive schedule and it's handler
      OpSchedule schedule = new OpSchedule();
      schedule.setName(OpSettings.get(OpSettings.REPORT_ARCHIVE_SCHEDULE_NAME));
      schedule.setStart(new Date(System.currentTimeMillis()));
      schedule.setUnit(OpScheduler.WEEKS);
      schedule.setInterval(Integer.parseInt(OpSettings.get(OpSettings.REPORT_REMOVE_TIME_PERIOD)));
      schedule.setHandler(new OpReportArhiveScheduleHandler());
      OpScheduler.registerSchedule(session, schedule);

   }

   /**
    * @see OpModule#upgrade(onepoint.project.OpProjectSession,int)
    */
   public void upgrade(OpProjectSession session, int dbVersion) {

      if (dbVersion < 3) {
         OpBroker broker = session.newBroker();
         OpQuery query = broker.newQuery("from OpReport");
         Iterator it = broker.iterate(query);
         if (it.hasNext()) {
            OpTransaction tx = broker.newTransaction();
            while (it.hasNext()) {
               OpReport report = (OpReport) it.next();
               if (report.getPermissions().size() == 0) {
                  OpReportArchiveService.addReportPermissions(session, broker, report);
               }
            }
            tx.commit();
         }
         broker.close();
      }
   }
}
