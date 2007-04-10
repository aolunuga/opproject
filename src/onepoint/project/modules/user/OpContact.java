/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.persistence.OpObject;

public class OpContact extends OpObject {

   public final static String CONTACT = "OpContact";

   public final static String FIRST_NAME = "FirstName";
   public final static String LAST_NAME = "LastName";
   public final static String EMAIL = "EMail";
   public final static String PHONE = "Phone";
   public final static String MOBILE = "Mobile";
   public final static String FAX = "Fax";
   public final static String USER = "User";

   private String lastName;
   private String firstName;
   private String eMail;
   private String phone;
   private String mobile;
   private String fax;
   private OpUser user;
   
   public void setLastName(String lastName) {
      this.lastName = lastName;
   }
   
   public String getLastName() {
      return lastName;
   }

   public void setFirstName(String firstName) {
      this.firstName = firstName;
   }
   
   public String getFirstName() {
      return firstName;
   }
   
   public void setEMail(String eMail) {
      this.eMail = eMail;
   }
   
   public String getEMail() {
      return eMail;
   }

   public void setPhone(String phone) {
      this.phone = phone;
   }
   
   public String getPhone() {
      return phone;
   }

   public void setMobile(String mobile) {
      this.mobile = mobile;
   }
   
   public String getMobile() {
      return mobile;
   }

   public void setFax(String fax) {
      this.fax = fax;
   }
   
   public String getFax() {
      return fax;
   }
   
   public void setUser(OpUser user) {
      this.user = user;
   }
   
   public OpUser getUser() {
      return user;
   }

}
