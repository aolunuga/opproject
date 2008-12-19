/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user.forms;

import java.util.HashMap;
import java.util.Iterator;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpGroup;
import onepoint.service.server.XSession;

public class OpAddGroupFormProvider implements XFormProvider {
   /* logger for this class*/
   private static final XLog logger = XLogFactory.getLogger(OpAddGroupFormProvider.class);
   /* groups that can be assigned for subject*/
   public final static String ASSIGNABLE_GROUP_DATA_SET = "AssignableGroupDataSet";
   /* groups that are already assigned*/
   public final static String ASSIGNED_GROUP_DATA_SET = "AssignedGroupsDataSet";


   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      logger.debug("OpAddGroupFormProvider.prepareForm()");

      String id_string = null;

      if (parameters != null) {
         id_string = (String) (parameters.get("group_id"));
      }

      OpBroker broker = ((OpProjectSession) session).newBroker();
      try {
         OpGroup group = null;
         logger.debug("ADD-GROUP " + id_string);

         // Ignore if id-string it is not a group ID
         if ((id_string != null) && (id_string.length() > 0)) {
            OpLocator locator = OpLocator.parseLocator(id_string);
            if (locator.getPrototype().getInstanceClass() == OpGroup.class)
               group = (OpGroup) (broker.getObject(id_string));
         }

         // *** For now: Find all groups; later on (v2.0) -- hierarchical
         // *** Maybe grey-out (enabled=false) already assigned groups?
         // ==> Problem: Only really possible if single-selection in users.oxf
         XComponent assignable_group_data_set = form.findComponent(ASSIGNABLE_GROUP_DATA_SET);

         XComponent assignedGroupsDataSet = (XComponent)parameters.get(ASSIGNED_GROUP_DATA_SET);

         StringBuffer queryBuffer = new StringBuffer("select group from OpGroup as group");
         if (group != null) {
            queryBuffer.append(" where group.id != ?");
         }
         // configure group sort order
         OpObjectOrderCriteria groupOrderCriteria = new OpObjectOrderCriteria(OpGroup.class, OpGroup.NAME, OpObjectOrderCriteria.ASCENDING);
         queryBuffer.append(groupOrderCriteria.toHibernateQueryString("group"));

         OpQuery query = broker.newQuery(queryBuffer.toString());
         if (group != null) {
            query.setLong(0, group.getId());
         }

         Iterator groups = broker.iterate(query);
         group = null;
         XComponent data_row = null;
         while (groups.hasNext()) {
            group = (OpGroup) (groups.next());
            String choice = XValidator.choice(group.locator(), group.getName());
            if (!isAlreadyAssigned(choice,assignedGroupsDataSet)){
               data_row = new XComponent(XComponent.DATA_ROW);
               data_row.setStringValue(choice);
               assignable_group_data_set.addChild(data_row);
            }
         }

      }
      finally {
         broker.close();
      }
   }

   /**
    * Checks if the <code>subject</code> already exists in the <code>assignedGroupsDataSet</code>
    *
    * @param subject               <code>String</code> the subject choice
    * @param assignedGroupDataSet <code>XComponent.DATA_SET</code> of assignments for a subject
    * @return boolean
    */
   private boolean isAlreadyAssigned(String subject, XComponent assignedGroupDataSet) {
      XComponent row;
      for (int index = 0; index < assignedGroupDataSet.getChildCount(); index++) {
         row = (XComponent) assignedGroupDataSet._getChild(index);
         if (subject.equals(row.getStringValue())) {
            return true;
         }
      }
      return false;
   }

}
