package onepoint.project.modules.external_applications;

public class OpExternalApplicationUserParameter extends OpParameter {

   private OpExternalApplicationUser user;
   
   public OpExternalApplicationUserParameter() {
      super(null, null);
      user = null;
   }
   
   public OpExternalApplicationUserParameter(String name, String value) {
      super(name, value);
      // TODO Auto-generated constructor stub
   }

   public OpExternalApplicationUser getUser() {
      return user;
   }

   public void setUser(OpExternalApplicationUser user) {
      this.user = user;
   }

}
