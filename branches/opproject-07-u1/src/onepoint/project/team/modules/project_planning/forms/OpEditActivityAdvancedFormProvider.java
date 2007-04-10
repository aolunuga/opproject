package onepoint.project.team.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.project.OpActivityCategory;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.forms.OpEditActivityFormProvider;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.List;

public class OpEditActivityAdvancedFormProvider extends OpEditActivityFormProvider {

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      super.prepareForm(s, form, parameters);
   }


   protected void addCategories(OpBroker broker, XComponent categoryChooser, XComponent dataSet, XLanguageResourceMap resourceMap) {
      // TODO: Probably cache these globally (should be the same of all sessions)
      //add the "null" category
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      String noCategory = resourceMap.getResource("NoCategory").getText();
      dataRow.setStringValue(XValidator.choice(OpGanttValidator.NO_CATEGORY_ID, noCategory));
      dataSet.addChild(dataRow);

      OpQuery query = broker.newQuery("select category from OpActivityCategory as category");
      List categories = broker.list(query);
      OpActivityCategory category = null;
      for (int i = 0; i < categories.size(); i++) {
         category = (OpActivityCategory) categories.get(i);
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(category.locator(), category.getName()));
         if (!category.getActive()) {
            dataRow.setEnabled(false);
         }
         dataSet.addChild(dataRow);
      }

      categoryChooser.setEnabled(true);
   }
}
