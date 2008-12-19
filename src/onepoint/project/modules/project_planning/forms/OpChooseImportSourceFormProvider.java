package onepoint.project.modules.project_planning.forms;

import java.util.HashMap;
import java.util.Iterator;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.project.module.OpModuleManager;
import onepoint.project.modules.external_applications.OpExternalApplicationDescription;
import onepoint.project.modules.external_applications.OpExternalApplicationsModule;
import onepoint.service.server.XSession;

public class OpChooseImportSourceFormProvider implements XFormProvider {

   public final static String PARAMETERS_ID = "parameters";
   public final static String IMPORTERCHOICESDATASET_ID = "ImporterChoicesDataSet";
   public final static String DIALOGMAP_ID = "DialogMap";
   public final static String IMPORTSOURCECHOOSERLABEL_ID = "ImportSourceChooserLabel";
   public final static String IMPORTSOURCECHOOSER_ID = "ImportSourceChooser";
   public final static String OKBUTTON_ID = "OkButton";
   public final static String CANCELBUTTON_ID = "CancelButton";


   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      OpExternalApplicationsModule extAppModule = ((OpExternalApplicationsModule) OpModuleManager.getModuleRegistry()
            .getModule(OpExternalApplicationsModule.MODULE_NAME));

      form.findComponent(PARAMETERS_ID).setValue(parameters);
      
      XComponent extAppsChoices = form.findComponent(IMPORTERCHOICESDATASET_ID);
      Iterator<OpExternalApplicationDescription> ait = extAppModule.getApplications();
      while (ait.hasNext()) {
         OpExternalApplicationDescription desc = ait.next();
         if (desc.getImportProjectPlanForm() != null) {
            XComponent choice = new XComponent(XComponent.DATA_ROW); 
            choice.setStringValue(XValidator.choice(desc.getImportProjectPlanForm(),
                  desc.getApplicationType()));
            extAppsChoices.addChild(choice);
            if (desc.isDefaultImport()) {
               XValidator.initChoiceField(form.findComponent(IMPORTSOURCECHOOSER_ID), desc.getImportProjectPlanForm());
            }
         }
      }
   }

}
