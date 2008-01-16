/**
 * Copyright(c) OnePoint Software GmbH 2008. All Rights Reserved.
 */
package onepoint.project.modules.project_checklist;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpToDo;
import onepoint.service.XMessage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Service class for the project checklist module.
 *
 * @author mihai.costin
 */
public class OpProjectChecklistService extends OpProjectService {

   private final static String PROJECT_ID = "project_id";
   private final static String TO_DOS_SET = "to_dos_set";

   public final static OpProjectChecklistErrorMap ERROR_MAP = new OpProjectChecklistErrorMap();

   public XMessage updateProjectToDos(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(PROJECT_ID));
      XComponent toDosDataSet = (XComponent) (request.getArgument(TO_DOS_SET));
      XMessage reply = null;

      if (id_string != null && toDosDataSet != null) {
         OpBroker broker = session.newBroker();
         OpProjectNode project = (OpProjectNode) (broker.getObject(id_string));
         OpTransaction t = broker.newTransaction();
         reply = updateToDos(session, broker, project, toDosDataSet);
         if(reply.getError() != null) {
            t.rollback();
         }
         else {
            t.commit();
         }
         broker.close();
      }

      return reply;
   }

   private XMessage updateToDos(OpProjectSession session, OpBroker broker,
        OpProjectNode project, XComponent toDosDataSet) {
      //the reply message
      XMessage reply = new XMessage();

      Map<String, OpToDo> to_do_map = new HashMap<String, OpToDo>();
      Iterator to_dos = project.getToDos().iterator();
      OpToDo to_do;
      while (to_dos.hasNext()) {
         to_do = (OpToDo) (to_dos.next());
         to_do_map.put(to_do.locator(), to_do);
      }

      XComponent data_row;
      XComponent data_cell;
      boolean updated;

      for (int i = 0; i < toDosDataSet.getChildCount(); i++) {
         data_row = (XComponent) (toDosDataSet.getChild(i));
         to_do = to_do_map.remove(data_row.getStringValue());
         if (to_do != null) {
            // Compare values and update to do if values have changed
            updated = false;
            // completed data cell
            data_cell = (XComponent) (data_row.getChild(0));
            if (to_do.getCompleted() != data_cell.getBooleanValue()) {
               to_do.setCompleted(data_cell.getBooleanValue());
               updated = true;
            }
            data_cell = (XComponent) (data_row.getChild(1));
            if ((to_do.getName() != null && !to_do.getName().equals(data_cell.getStringValue())) ||
                 (to_do.getName() == null && data_cell.getStringValue() != null)) {
               to_do.setName(data_cell.getStringValue());
               updated = true;
            }
            data_cell = (XComponent) (data_row.getChild(2));
            if (to_do.getPriority() != data_cell.getIntValue()) {
               if (data_cell.getIntValue() < 1 || data_cell.getIntValue() > 9) {
                  reply.setError(session.newError(ERROR_MAP, OpProjectChecklistError.TODO_PRIORITY_ERROR));
                  return reply;
               }
               else {
                  to_do.setPriority((byte) data_cell.getIntValue());
                  updated = true;
               }
            }
            data_cell = (XComponent) (data_row.getChild(3));
            if (to_do.getDue() != data_cell.getDateValue()) {
               if ((to_do.getDue() == null) || (data_cell.getDateValue() == null)
                    || (!to_do.getDue().equals(data_cell.getDateValue()))) {
                  to_do.setDue(data_cell.getDateValue());
                  updated = true;
               }
            }
            if (updated) {
               broker.updateObject(to_do);
            }
         }
         else {
            // Insert new to do
            to_do = new OpToDo();
            to_do.setProjectNode(project);
            data_cell = (XComponent) (data_row.getChild(0));
            to_do.setCompleted(data_cell.getBooleanValue());
            data_cell = (XComponent) (data_row.getChild(1));
            to_do.setName(data_cell.getStringValue());
            data_cell = (XComponent) (data_row.getChild(2));
            if (data_cell.getIntValue() < 1 || data_cell.getIntValue() > 9) {
               reply.setError(session.newError(ERROR_MAP, OpProjectChecklistError.TODO_PRIORITY_ERROR));
               return reply;
            }
            else {
               to_do.setPriority((byte) data_cell.getIntValue());

            }
            data_cell = (XComponent) (data_row.getChild(3));
            to_do.setDue(data_cell.getDateValue());
            broker.makePersistent(to_do);
         }
      }
      // Remove outdated to dos from set and delete them
      Iterator locators = to_do_map.keySet().iterator();
      Set to_do_set = project.getToDos();
      while (locators.hasNext()) {
         to_do = (to_do_map.get((String) (locators.next())));
         to_do_set.remove(to_do);
         broker.deleteObject(to_do);
      }
      return reply;
   }
}
