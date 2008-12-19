package onepoint.project.modules.external_applications;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import onepoint.project.modules.external_applications.generated.description.Application;

public class OpExternalApplicationDescription {

   private Application app = null;
   
   private OpExternalApplicationInstance instance = null;
   
   public OpExternalApplicationDescription(Application app, OpExternalApplicationInstance instance) {
      this.app = app;
      this.instance = instance;
   }

   public String getApplicationType() {
      return app.getType();
   }

   public String getImportProjectPlanForm() {
      return app.getProjectPlanImport().getForm();
   }

   public boolean isDefaultImport() {
      return app.getProjectPlanImport().isDefault() != null ? app.getProjectPlanImport().isDefault().booleanValue() : false;
   }

   public Map<String, String> getParameters() {
      Map<String, String> result = new HashMap<String, String>();
      Iterator<Application.Parameter> i = app.getParameter().iterator();
      while (i.hasNext()) {
         Application.Parameter p = i.next();
         result.put(p.getName(), p.getValue());
      }
      return result;
   }
   public String getParameter(String name) {
      return getParameters().get(name);
   }

   public OpExternalApplicationInstance getInstance() {
      return instance;
   }
}
