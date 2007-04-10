/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.activity_category.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivityCategory;
import onepoint.project.team.modules.activity_category.OpActivityCategoryService;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpEditCategoryFormProvider implements XFormProvider {

   public final static String CATEGORY_ID = "CategoryID";
   public final static String EDIT_MODE = "EditMode";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Locate category in database
      String id_string = (String) (parameters.get(OpActivityCategoryService.CATEGORY_ID));
      Boolean edit_mode = (Boolean) parameters.get(OpActivityCategoryService.EDIT_MODE);

      OpBroker broker = ((OpProjectSession) session).newBroker();

      OpActivityCategory category = (OpActivityCategory) (broker.getObject(id_string));

      // Downgrade edit mode to view mode if user is not the administrator
      if (edit_mode.booleanValue() && !session.userIsAdministrator())
         edit_mode = Boolean.FALSE;

      // Fill form with data
      form.findComponent(CATEGORY_ID).setStringValue(id_string);
      form.findComponent(EDIT_MODE).setBooleanValue(edit_mode.booleanValue());

      XComponent name = form.findComponent(OpActivityCategory.NAME);
      name.setStringValue(category.getName());
      XComponent description = form.findComponent(OpActivityCategory.DESCRIPTION);
      description.setStringValue(category.getDescription());
      XComponent color = form.findComponent(OpActivityCategory.COLOR);
      color.setIntValue(category.getColor());

      if (!edit_mode.booleanValue()) {
         name.setEnabled(false);
         description.setEnabled(false);
         color.setEnabled(false);
         form.findComponent("Cancel").setVisible(false);
         String title = session.getLocale().getResourceMap("activity_category.Info").getResource("InfoCategory")
               .getText();
         form.setText(title);
      }

      broker.close();
   }

}
