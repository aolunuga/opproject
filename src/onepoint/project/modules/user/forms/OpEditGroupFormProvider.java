/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpGroupAssignment;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpEditGroupFormProvider implements XFormProvider {

   private static XLog logger = XLogFactory.getLogger(OpEditGroupFormProvider.class,true);

   public final static String ASSIGNED_GROUP_DATA_SET = "AssignedGroupDataSet";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      
      // Find group in database
      logger.debug("OpEditGroupFormProvider.prepareForm()");

      String id_string = (String) (parameters.get("group_id"));
      Boolean edit_mode = (Boolean) (parameters.get("edit_mode"));
      OpBroker broker = ((OpProjectSession) session).newBroker();

      OpGroup group = (OpGroup) (broker.getObject(id_string));
      // Fill edit-group form with group data
      // *** TODO: Use class-constants for text-field IDs
      form.findComponent("GroupID").setStringValue(id_string);
      form.findComponent("EditMode").setBooleanValue(edit_mode.booleanValue());
      // Attention: We are using display name here in order to localize name of group "Everyone"
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));
      XComponent name = form.findComponent("Name");
      name.setStringValue(localizer.localize(group.getDisplayName()));
      XComponent desc = form.findComponent("Description");
      desc.setStringValue(localizer.localize(group.getDescription()));

      if (!edit_mode.booleanValue()) {
         name.setEnabled(false);
         desc.setEnabled(false);
         form.findComponent("GroupToolPanel").setVisible(false);
         form.findComponent("Cancel").setVisible(false);
         String title = ((OpProjectSession) session).getLocale().getResourceMap("user.Info").getResource("InfoGroup")
               .getText();
         form.setText(title);
      }
      else if (group.getID() == session.getEveryoneID()) {
         // Name and description of group everyone cannot be changed
         name.setEnabled(false);
         desc.setEnabled(false);
      }

      XComponent assigned_group_data_set = form.findComponent(ASSIGNED_GROUP_DATA_SET);

      Iterator assignments = group.getSuperGroupAssignments().iterator();
      OpGroupAssignment assignment = null;
      OpGroup superGroup = null;
      XComponent dataRow = null;
      while (assignments.hasNext()) {
         assignment = (OpGroupAssignment) assignments.next();
         superGroup = assignment.getSuperGroup();
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(superGroup.locator(), superGroup.getName()));
         assigned_group_data_set.addChild(dataRow);
      }
      assigned_group_data_set.sort();
      broker.close();
   }

}
