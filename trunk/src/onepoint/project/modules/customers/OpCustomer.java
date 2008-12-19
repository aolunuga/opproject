package onepoint.project.modules.customers;

import java.util.HashSet;
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
}