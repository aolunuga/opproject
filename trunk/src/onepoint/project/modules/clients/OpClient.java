package onepoint.project.modules.clients;

import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpProjectNode;

public class OpClient extends OpObject {
   
   private String name;
   private String description;
   private String webSite;
   private String address;
   private String zip;
   private String city;

   private String contactFirstName;
   private String contactLastName;
   private String contactEmail;
   private String contactPhone;
   private String contactFax;

   private OpCountry country;
   
   private Set<OpProjectNode> projects;
   
   
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getContactFirstName() {
      return contactFirstName;
   }
   public void setContactFirstName(String contactFirstName) {
      this.contactFirstName = contactFirstName;
   }
   public String getContactLastName() {
      return contactLastName;
   }
   public void setContactLastName(String contactLastName) {
      this.contactLastName = contactLastName;
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
   public OpCountry getCountry() {
      return country;
   }
   public void setCountry(OpCountry country) {
      this.country = country;
   }
   
   public Set<OpProjectNode> getProjects() {
      return projects;
   }
   public void setProjects(Set<OpProjectNode> projects) {
      this.projects = projects;
   }
   
}
