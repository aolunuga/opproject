package onepoint.project.modules.external_applications;

public class OpExternalApplicationParameter extends OpParameter {

   private OpExternalApplication application;
   
   public OpExternalApplicationParameter() {
      super(null, null);
      application = null;
   }
   
   public OpExternalApplicationParameter(String name, String value) {
      super(name, value);
   }

   public OpExternalApplication getApplication() {
      return application;
   }

   public void setApplication(OpExternalApplication application) {
      this.application = application;
   }

}
