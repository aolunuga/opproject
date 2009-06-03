package onepoint.project.modules.customers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;

public class OpCustomer extends OpObject implements OpPermissionable {
   
   private String number;
   private String name;
   private String description;
   private String webSite;
   private String address;
   private String zip;
   private String city;

   private String contactName;
   private String contactEmail;
   private String contactPhone;
   private String contactFax;

   private String country;
   
   private Set<OpProjectNode> projects;
   private Set<OpPermission> permissions;
   private Map<String, String> countryCodeToNameMap;
   
   public String getNumber() {
      return number;
   }
   public void setNumber(String number) {
      this.number = number;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getContactName() {
      return contactName;
   }
   public void setContactName(String contactName) {
      this.contactName = contactName;
   }
   public String getContactEmail() {
      return contactEmail;
   }
   public void setContactEmail(String contactEmail) {
      this.contactEmail = contactEmail;
   }
   public String getContactPhone() {
      return contactPhone;
   }
   public void setContactPhone(String contactPhone) {
      this.contactPhone = contactPhone;
   }
   public String getContactFax() {
      return contactFax;
   }
   public void setContactFax(String contactFax) {
      this.contactFax = contactFax;
   }
   public String getDescription() {
      return description;
   }
   public void setDescription(String description) {
      this.description = description;
   }
   public String getWebSite() {
      return webSite;
   }
   public void setWebSite(String webSite) {
      this.webSite = webSite;
   }
   public String getAddress() {
      return address;
   }
   public void setAddress(String address) {
      this.address = address;
   }
   public String getZip() {
      return zip;
   }
   public void setZip(String zip) {
      this.zip = zip;
   }
   public String getCity() {
      return city;
   }
   public void setCity(String city) {
      this.city = city;
   }
   public String getCountry() {
      return country;
   }
   public void setCountry(String country) {
      this.country = country;
   }
   
   public Set<OpProjectNode> getProjects() {
      return projects;
   }
   private void setProjects(Set<OpProjectNode> projects) {
      this.projects = projects;
   }
   
   public void addProject(OpProjectNode project) {
      if (getProjects() == null) {
         setProjects(new HashSet<OpProjectNode>());
      }
      if (getProjects().add(project)) {
         project.setCustomer(this);
      }
   }
   public void removeProject(OpProjectNode project) {
      if (getProjects() == null) {
         return;
      }
      if (getProjects().remove(project)) {
         project.setCustomer(null);
      }
   }
   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#getPermissions()
    */
   public Set<OpPermission> getPermissions() {
      return permissions;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#setPermissions(java.util.Set)
    */
   public void setPermissions(Set<OpPermission> permissions) {
      this.permissions = permissions;
   }
   
   public void addPermission(OpPermission permission) {
      Set<OpPermission> perm = getPermissions();
      if (perm == null) {
         perm = new HashSet<OpPermission>();
         setPermissions(perm);
      }
      perm.add(permission);
      permission.setObject(this);
   }

   /**
    * @param opPermission
    * @pre
    * @post
    */
   public void removePermission(OpPermission opPermission) {
      Set<OpPermission> perm = getPermissions();
      if (perm != null) {
         perm.remove(opPermission);
      }
      opPermission.setObject(null);
   }

   public String concatContactFirstName(String firstName) {
      String contact = getContactName();
      if (contact == null || contact.length() == 0) {
         return firstName;
      }
      return firstName+" "+contact;
   }

   public String concatContactLastName(String lastName) {
      String contact = getContactName();
      if (contact == null || contact.length() == 0) {
         return lastName;
      }
      return contact + " " + lastName;
   }
   public String getCountryName() {
      if (countryCodeToNameMap == null) {
         countryCodeToNameMap = mapCountryCodesToNames();
      }
      String ret = countryCodeToNameMap.get(country);
      if (ret != null) {
         return ret;
      }
      return country;
   }

   private Map<String, String> mapCountryCodesToNames() {
      Map<String, String> ret = new HashMap<String, String>();
      // fetch from http://www.iso.org/iso/list-en1-semic-3.txt
      BufferedReader res = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("list-en1-semic-3.txt")));
      // read header
      String line = "x";
      try {
         while (line != null && line.length() > 0) {
            line = res.readLine();
         }
         while (true) {
            line = res.readLine();
            if (line == null) {
               break;
            }
            String[] countryAndCode = line.split(";");
            if (countryAndCode.length == 2) {
               ret.put(countryAndCode[1], fixCase(countryAndCode[0]));
            }
         }
      } catch (IOException e) {
         //         e.printStackTrace();
      }
      return ret;
   }
   
   final static String[] LOWER_CASED = new String[] {"and"};
   private String fixCase(String value) {
      String[] names = value.split(" ");
      StringBuffer buffer = new StringBuffer();
      for (int pos = 0; pos < names.length; pos++) {
         String val = names[pos];
         if (val.length() == 0) {
            continue;
         }
         val = val.substring(0, 1).toUpperCase()+val.substring(1).toLowerCase();
         for (int pos2 = 0; pos2 < LOWER_CASED.length; pos2++) {
            if (val.equalsIgnoreCase(LOWER_CASED[pos2])) {
               val = LOWER_CASED[pos2];
               break;
            }
         }
         if (pos != 0) {
            buffer.append(" ");
         }
        buffer.append(val);
      }
      return buffer.toString();
   }
}