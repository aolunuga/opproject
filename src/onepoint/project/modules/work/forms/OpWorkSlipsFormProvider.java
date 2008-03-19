/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkSlipDataSetFactory;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

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
   protected final static String PERIOD_STARTING_WITH_CURRENT_MONTH = "current";
   protected final static String PERIOD_STARTING_WITH_PREVIOUS_MONTH = "previous";
   protected final static String PERIOD_STARTING_WITH_CURRENT_YEAR = "year";
   
   public final static String WORK_SLIPS_QUERY = "select workSlip from OpWorkSlip as workSlip where " +
   "workSlip.Creator.ID = :userId and workSlip.Date >= :date order by workSlip.Date desc";

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
         OpWorkSlipDataSetFactory.fillWorkSlipsDataSet(wsIterator, data_set);
         
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

         List resourceIds = new ArrayList();
         OpResource resource;
         for (Iterator it = userResources.iterator(); it.hasNext();) {
            resource = (OpResource) it.next();
            resourceIds.add(new Long(resource.getID()));
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
      Calendar calendar = getTimeFromPeriodChooser(session, period);

      OpQuery query = broker.newQuery(WORK_SLIPS_QUERY);
      query.setLong("usedId", session.getUserID());
      query.setDate("date", new Date(calendar.getTime().getTime()));
      Iterator wsIterator = broker.iterate(query);
      return wsIterator;
   }

   protected Calendar getTimeFromPeriodChooser(OpProjectSession session,
         String period) {
      if (period == null) {
         period = PERIOD_STARTING_WITH_CURRENT_MONTH; // default value
      }
      Calendar calendar = session.getCalendar().getCalendar();
      calendar.setTime(XCalendar.today());
      if (period.equals(PERIOD_STARTING_WITH_CURRENT_MONTH)) {
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
      return calendar;
   }
}
