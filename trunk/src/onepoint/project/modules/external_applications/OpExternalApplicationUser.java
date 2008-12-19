package onepoint.project.modules.external_applications;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

public class OpExternalApplicationUser extends OpObject {
   
   private OpExternalApplication application = null;
   private OpUser user = null;
   
   private Set<OpExternalApplicationUserParameter> parameters = null;

   public OpExternalApplication getApplication() {
      return application;
   }

   public void setApplication(OpExternalApplication application) {
      this.application = application;
   }

   public OpUser getUser() {
      return user;
   }

   public void setUser(OpUser user) {
      this.user = user;
   }

   public Set<OpExternalApplicationUserParameter> getParameters() {
      return parameters;
   }

   private void setParameters(Set<OpExternalApplicationUserParameter> parameters) {
      this.parameters = parameters;
   }

   public void addParameter(OpExternalApplicationUserParameter p) {
      if (getParameters() == null) {
         setParameters(new HashSet<OpExternalApplicationUserParameter>());
      }
      if (getParameters().add(p)) {
         p.setUser(this);
      }
   }

   public void removeParameter(OpExternalApplicationUserParameter p) {
      if (getParameters() == null) {
         return;
      }
      if (getParameters().remove(p)) {
         p.setUser(null);
      }
   }
   
   public OpExternalApplicationUserParameter getParameter(String name) {
      if (getParameters() == null) {
         return null;
      }
      Iterator<OpExternalApplicationUserParameter> pit = getParameters().iterator();
      while (pit.hasNext()) {
         OpExternalApplicationUserParameter p = pit.next();
         if (name.equals(p.getName())) {
            return p;
         }
      }
      return null;
   }
}
