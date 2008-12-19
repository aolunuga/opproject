/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import java.util.regex.Pattern;

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

   // email pattern ex : eXpress@onepoint.at
   public final String EMAIL_REG_EXP = "^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]";

   private String lastName;
   private String firstName;
   private String eMail;
   private String phone;
   private String mobile;
   private String fax;
   private OpUser user;

   public OpContact() {
      super();
   }

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

   /**
    * @return
    * @pre
    * @post
    */
   public boolean isEmailValid() {
      if ((eMail == null) || (eMail.length() == 0)) {
         return (true);
      }
      return (Pattern.matches(EMAIL_REG_EXP, eMail));
   }

   /**
    * Gets the display name for a user.
    *
    * @param defaultName a <code>String</code> representing a fallback name, if no display name is found in the contact.
    * @return a <code>String</code> representing the display name of a user.
    */
   public String calculateDisplayName(String defaultName) {
      StringBuffer result = new StringBuffer();
      if (firstName != null && firstName.trim().length() > 0) {
         result.append(firstName);
      }
      if (lastName != null && lastName.trim().length() > 0) {
         if (result.length() > 0) {
            result.append(" ");
         }
         result.append(lastName);
      }

      if (result.length() == 0) {
         return defaultName;
      }
      return result.toString();
   }
}
