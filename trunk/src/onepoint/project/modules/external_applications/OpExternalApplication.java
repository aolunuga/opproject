package onepoint.project.modules.external_applications;

import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;

public class OpExternalApplication extends OpObject {
   
   private String kind;
   private String description;
   
   private String instanceName;
   
   private Set<OpExternalApplicationUser> users = null;
   private Set<OpExternalApplicationParameter> parameters = null;
   
   public String getKind() {
      return kind;
   }
   public void setKind(String kind) {
      this.kind = kind;
   }
   public String getDescription() {
      return description;
   }
   public void setDescription(String description) {
      this.description = description;
   }
   
   public Set<OpExternalApplicationUser> getUsers() {
      return users;
   }
   private void setUsers(Set<OpExternalApplicationUser> user) {
      this.users = user;
   }
   
   public void addUser(OpExternalApplicationUser user) {
      if (getUsers() == null) {
         setUsers(new HashSet<OpExternalApplicationUser>());
      }
      if (getUsers().add(user)) {
         user.setApplication(this);
      }
   }
   public void removeUser(OpExternalApplicationUser user) {
      if (getUsers() == null) {
         return;
      }
      if (getUsers().remove(user)) {
         user.setApplication(null);
      }
   }
   
   public Set<OpExternalApplicationParameter> getParameters() {
      return parameters;
   }
   private void setParameters(Set<OpExternalApplicationParameter> parameters) {
      this.parameters = parameters;
   }
   
   public void addParameter(OpExternalApplicationParameter p) {
      if (getParameters() == null) {
         setParameters(new HashSet<OpExternalApplicationParameter>());
      }
      if (getParameters().add(p)) {
         p.setApplication(this);
      }
   }
   public void removeParameter(OpExternalApplicationParameter p) {
      if (getParameters() == null) {
         return;
      }
      if (getParameters().remove(p)) {
         p.setApplication(null);
      }
   }
   
   public String getInstanceName() {
      return instanceName;
   }
   public void setInstanceName(String address) {
      this.instanceName = address;
   }
}
