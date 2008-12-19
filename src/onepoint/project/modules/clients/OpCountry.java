package onepoint.project.modules.clients;

import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;

public class OpCountry extends OpObject {
   
   private String code;
   private String name;
   private String locale;
   
   private Set<OpClient> clients;

   public String getCode() {
      return code;
   }

   public void setCode(String code) {
      this.code = code;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getLocale() {
      return locale;
   }

   public void setLocale(String locale) {
      this.locale = locale;
   }

   public Set<OpClient> getClients() {
      return clients;
   }

   private void setClients(Set<OpClient> clients) {
      this.clients = clients;
   }
   
   public boolean addClient(OpClient client) {
      if (getClients() == null) {
         clients = new HashSet<OpClient>();
      }
      if (clients.add(client)) {
         client.setCountry(this);
         return true;
      }
      return false;
   }
   
   public boolean removeClient(OpClient client) {
      if (getClients() == null) {
         return false;
      }
      if (getClients().remove(client)) {
         client.setCountry(null);
         return true;
      }
      return false;
   }
   
}
