/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

public class OpWorkSlipsFormProvider implements XFormProvider {

   public final static String WORK_SLIP_SET = "WorkSlipSet";

   public final static int NUMBER_COLUMN_INDEX = 0;
   public final static int RESOURCE_COLUMN_INDEX = 1;
   public final static int DATE_COLUMN_INDEX = 2;

   public final static String WORK_SLIPS_QUERY = "select workSlip from OpWorkSlip as workSlip where workSlip.Creator.ID = ? order by workSlip.Date desc";

   /* form button ids */
   private final static String NEW_WORK_SLIP_BUTTON = "NewWorkSlip";
   private final static String EDIT_WORK_SLIP_BUTTON = "EditWorkSlip";
   private final static String INFO_WORK_SLIP_BUTTON = "InfoWorkSlip";
   private final static String DELETE_WORK_SLIP_BUTTON = "DeleteWorkSlip";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      //disable buttons that require selection
      XComponent editButton = form.findComponent(EDIT_WORK_SLIP_BUTTON);
      editButton.setEnabled(false);
      XComponent infoButton = form.findComponent(INFO_WORK_SLIP_BUTTON);
      infoButton.setEnabled(false);
      XComponent deleteButton = form.findComponent(DELETE_WORK_SLIP_BUTTON);
      deleteButton.setEnabled(false);

      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // Locate data set in form
      XComponent data_set = form.findComponent(WORK_SLIP_SET);
      XComponent data_row = null;
      XComponent data_cell = null;

      OpQuery query = broker.newQuery(WORK_SLIPS_QUERY);
      query.setLong(0, session.getUserID());
      Iterator work_slips = broker.iterate(query);
      OpWorkSlip work_slip = null;

      while (work_slips.hasNext()) {
         work_slip = (OpWorkSlip) (work_slips.next());
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setStringValue(work_slip.locator());
         data_set.addChild(data_row);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setIntValue(work_slip.getNumber());
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setValue(work_slip.getDate());
         data_row.addChild(data_cell);
      }

      //new work slip button enabled by default
      XComponent newWorkSlipButton = form.findComponent(NEW_WORK_SLIP_BUTTON);

      // find user asociated resources
      long sessionUserID = session.getUserID();
      OpUser user = (OpUser) (broker.getObject(OpUser.class, sessionUserID));
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


      //<FIXME author="Ovidiu Lupas" description="might not find assignments">
      int weeks = 8; // TODO: Make #weeks configurable
      Date start = new Date(System.currentTimeMillis() + XCalendar.MILLIS_PER_WEEK * weeks);
      //</FIXME>

      StringBuffer buffer = new StringBuffer();
      buffer.append("select activity from OpAssignment as assignment inner join assignment.Activity as activity ");
      buffer.append("where assignment.Resource.ID in (:resourceIds) and assignment.Complete < :complete and (activity.Start < :start or activity.Start is null) and activity.Type in (:type)");
      query = broker.newQuery(buffer.toString());
      query.setCollection("resourceIds", resourceIds);
      query.setByte("complete", (byte)100);
      query.setDate("start", start);

      List types = new ArrayList();
      types.add(new Byte(OpActivity.STANDARD));
      types.add(new Byte(OpActivity.TASK));
      types.add(new Byte(OpActivity.MILESTONE));
      query.setCollection("type", types);

      List results = broker.list(query);
      if (results.isEmpty()){
         broker.close();
         newWorkSlipButton.setEnabled(false);
         return;
      }
      broker.close();
   }

}
