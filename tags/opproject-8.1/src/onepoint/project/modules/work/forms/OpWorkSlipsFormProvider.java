/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.cfg.Settings;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkSlipDataSetFactory;
import onepoint.project.util.OpProjectCalendar;
import onepoint.service.server.XSession;

/**
 * Form provider for Work Slips.
 */
public class OpWorkSlipsFormProvider implements XFormProvider {

   public final static String WORK_SLIP_SET = "WorkSlipSet";
   public final static String WORK_SLIP_STATE_SET = "WorkSlipStateSet";

   public final static int NUMBER_COLUMN_INDEX = 0;
   public final static int RESOURCE_COLUMN_INDEX = 1;
   public final static int DATE_COLUMN_INDEX = 2;

   //form button ids 
   protected final static String NEW_WORK_SLIP_BUTTON = "NewWorkSlip";
   protected final static String INFO_WORK_SLIP_BUTTON = "InfoWorkSlip";
   protected final static String DELETE_WORK_SLIP_BUTTON = "DeleteWorkSlip";
   protected final static String LOCK_WORK_SLIP_BUTTON = "LockWorkSlip";
   protected final static String UNLOCK_WORK_SLIP_BUTTON = "UnlockWorkSlip";

   protected final static String PERIOD_CHOICE_ID = "period_choice_id";
   protected final static String PERIOD_CHOICE_FIELD = "PeriodChooser";
   protected final static String PERIOD_STARTING_WITH_CURRENT_WEEK = "currentWeek";
   protected final static String PERIOD_STARTING_WITH_PREVIOUS_WEEK = "previousWeek";
   protected final static String PERIOD_STARTING_WITH_CURRENT_MONTH = "current";
   protected final static String PERIOD_STARTING_WITH_PREVIOUS_MONTH = "previous";
   protected final static String PERIOD_STARTING_WITH_CURRENT_YEAR = "year";

   public final static String WORKSLIPSFORM_ID = "WorkSlipsForm";
   public final static String WORKSLIPSET_ID = "WorkSlipSet";
   public final static String WORKSLIPSPANEL_ID = "WorkSlipsPanel";
   public final static String PERIODSET_ID = "PeriodSet";
   public final static String LOCKSTATESET_ID = "LockStateSet";
   public final static String TOOLBARPANEL_ID = "ToolBarPanel";
   public final static String TOOLBAR_ID = "ToolBar";
   public final static String NEWWORKSLIP_ID = "NewWorkSlip";
   public final static String INFOWORKSLIP_ID = "InfoWorkSlip";
   public final static String DELETEWORKSLIP_ID = "DeleteWorkSlip";
   public final static String CHOOSERPANEL_ID = "ChooserPanel";
   public final static String PERIODCHOOSER_ID = "PeriodChooser";
   public final static String WEEKDAYSDATASET_ID = "WeekDaysDataSet";
   public final static String WORKSLIPSFOOTERDATASET_ID = "WorkSlipsFooterDataSet";
   public final static String WORKSLIPROWMAP_ID = "WorkslipRowMap";
   public final static String WORKSLIPTABLE_ID = "WorkSlipTable";
   
   public final static String WORK_SLIPS_QUERY = "select workSlip from OpWorkSlip as workSlip where " +
   "workSlip.Creator.id = :userId and workSlip.Date >= :startDate and workSlip.Date < :finishDate order by workSlip.Date desc";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      //disable buttons that require selection
      XComponent infoButton = form.findComponent(INFO_WORK_SLIP_BUTTON);
      infoButton.setEnabled(false);
      XComponent deleteButton = form.findComponent(DELETE_WORK_SLIP_BUTTON);
      deleteButton.setEnabled(false);

      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      try {
         // Locate data set in form
         XComponent data_set = form.findComponent(WORK_SLIP_SET);
         
         Iterator wsIterator = prepareWorkslipsDataSetIterator(broker, session, form, parameters);
         OpWorkSlipDataSetFactory.fillWorkSlipsDataSet(wsIterator, data_set,
               form.findComponent(WORKSLIPROWMAP_ID), form
                     .findComponent(WEEKDAYSDATASET_ID));
         
         XComponent lockWorkSlipButton = form.findComponent(LOCK_WORK_SLIP_BUTTON);
         if (lockWorkSlipButton != null) {
            lockWorkSlipButton.setEnabled(false);
         }
         XComponent unlockWorkSlipButton = form.findComponent(UNLOCK_WORK_SLIP_BUTTON);
         if (unlockWorkSlipButton != null) {
            unlockWorkSlipButton.setEnabled(false);
         }

         //new work slip button enabled by default
         XComponent newWorkSlipButton = form.findComponent(NEW_WORK_SLIP_BUTTON);

         // find user asociated resources
         long sessionUserID = session.getUserID();
         OpUser user = broker.getObject(OpUser.class, sessionUserID);
         Set userResources = user.getResources();

         if (userResources.isEmpty()) {
            broker.close();
            newWorkSlipButton.setEnabled(false);
            return;
         }

         Set resourceIds = new HashSet();
         OpResource resource;
         for (Iterator it = userResources.iterator(); it.hasNext();) {
            resource = (OpResource) it.next();
            resourceIds.add(new Long(resource.getId()));
         }

         List types = new ArrayList();
         types.add(new Byte(OpActivity.STANDARD));
         types.add(new Byte(OpActivity.TASK));
         types.add(new Byte(OpActivity.MILESTONE));
         types.add(new Byte(OpActivity.ADHOC_TASK));

         Iterator result = OpWorkSlipDataSetFactory.getAssignments(broker, resourceIds, types, null, null, OpWorkSlipDataSetFactory.ALL_PROJECTS_ID, false);
         if (!result.hasNext()) {
            broker.close();
            newWorkSlipButton.setEnabled(false);
            return;
         }
      }
      finally {
         broker.close();
      }
   }

   protected Iterator prepareWorkslipsDataSetIterator(OpBroker broker,
         OpProjectSession session, XComponent form, HashMap parameters) {

      String period = XValidator.getChoiceID(form, session.getComponentStateMap(form.getID()),
            PERIOD_CHOICE_FIELD, (String) parameters.get(PERIOD_CHOICE_ID),
            PERIOD_STARTING_WITH_CURRENT_MONTH);
      Date start = getStartTimeFromPeriodChooser(session, period);
      Date finish = getFinishTimeFromPeriodChooser(session, period);
      OpQuery query = broker.newQuery(WORK_SLIPS_QUERY);
      query.setLong("userId", session.getUserID());
      query.setDate("startDate", start);
      query.setDate("finishDate", finish);
      Iterator wsIterator = broker.iterate(query);
      return wsIterator;
   }

   protected Date getStartTimeFromPeriodChooser(OpProjectSession session,
         String period) {
      if (period == null) {
         period = PERIOD_STARTING_WITH_CURRENT_MONTH; // default value
      }
      Calendar calendar = session.getCalendar().cloneCalendarInstance();
      calendar.setTime(OpProjectCalendar.today());
      if (period.equals(PERIOD_STARTING_WITH_CURRENT_WEEK) || period.equals(PERIOD_STARTING_WITH_PREVIOUS_WEEK)) {
    	  OpBroker broker = session.newBroker();
    	  try {
    		  int firstWorkday = Integer.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.CALENDAR_FIRST_WORKDAY));
    		  calendar.set(Calendar.DAY_OF_WEEK, firstWorkday);
    		  if (period.equals(PERIOD_STARTING_WITH_PREVIOUS_WEEK)) {
    			  calendar.add(Calendar.WEEK_OF_MONTH, -1);
    		  }
    	  }
    	  finally {
    		  broker.close();
    	  }
       }
      else if (period.equals(PERIOD_STARTING_WITH_CURRENT_MONTH)) {
         calendar.set(Calendar.DAY_OF_MONTH, 1);
      }
      else if (period.equals(PERIOD_STARTING_WITH_PREVIOUS_MONTH)) {
         calendar.add(Calendar.MONTH, -1);
         calendar.set(Calendar.DAY_OF_MONTH, 1);

      }
      else if (period.equals(PERIOD_STARTING_WITH_CURRENT_YEAR)) {
         calendar.set(Calendar.DAY_OF_MONTH, 1);
         calendar.set(Calendar.MONTH, 0);
      }
      else {
         //all
         calendar.setTime(new Date(0));
      }
      return new Date(calendar.getTime().getTime());
   }

   protected Date getFinishTimeFromPeriodChooser(OpProjectSession session,
	         String period) {
	      if (period == null) {
	         period = PERIOD_STARTING_WITH_CURRENT_MONTH; // default value
	      }
	      Calendar calendar = session.getCalendar().cloneCalendarInstance();
	      calendar.setTime(getStartTimeFromPeriodChooser(session, period));
	      if (period.equals(PERIOD_STARTING_WITH_CURRENT_WEEK) || period.equals(PERIOD_STARTING_WITH_PREVIOUS_WEEK)) {
	    	  calendar.add(Calendar.WEEK_OF_MONTH, 1);
	       }
	      else if (period.equals(PERIOD_STARTING_WITH_CURRENT_MONTH) || period.equals(PERIOD_STARTING_WITH_PREVIOUS_MONTH)) {
	         calendar.add(Calendar.MONTH, 1);
	      }
	      else if (period.equals(PERIOD_STARTING_WITH_CURRENT_YEAR)) {
	    	  calendar.add(Calendar.YEAR, 1);
	      }
	      else {
	    	  calendar.setTime(new GregorianCalendar().getTime());
	    	  calendar.add(Calendar.YEAR, 100); // today + 100 years
	      }
	      return new Date(calendar.getTime().getTime());
	   }

}
