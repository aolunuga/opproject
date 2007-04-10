/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.license.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.license.OpLicense;
import onepoint.license.OpLicenseProduct;
import onepoint.project.OpInitializer;
import onepoint.project.team.modules.license.OpLicenseService;
import onepoint.service.server.XServiceManager;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * @author : mihai.costin
 */
public class OpLicenseFormProvider implements XFormProvider {

   //form fields
   public static final String VERSION = "Version";
   public static final String TYPE = "Type";
   public static final String ISSUER = "Issuer";
   public static final String CUSTOMER_NAME = "CustomerName";
   public static final String CUSTOMER_NUMBER = "CustomerNumber";
   public static final String PARTNER_NAME = "PartnerName";
   public static final String PARTNER_NUMBER = "PartnerNumber";
   public static final String VALID_UNTIL = "ValidUntil";
   public static final String PRODUCT = "Product";
   public static final String PRODUCT_CODE = "ProductCode";
   public static final String PRODUCT_VERSION = "ProductVersion";
   public static final String STANDARD_USERS = "StandardUsers";
   public static final String MANAGER_USERS = "ManagerUsers";
   public static final String STANDARD_USERS_LABEL = "StandardUsersLabel";
   public static final String MANAGER_USERS_LABEL = "ManagerUsersLabel";

   public void prepareForm(XSession session, XComponent form, HashMap parameters) {

      OpLicense license = null;

      OpLicenseService service = (OpLicenseService) XServiceManager.getService("LicenseService");
      if (service != null) {
         license = service.getLicense();
      }

      //TODO: author="Mihai Costin" description="Check for license expiration / warning messages"
      if (license == null) {
         return;
      }

      //populate fields
      XComponent versionField = form.findComponent(VERSION);
      versionField.setValue(license.getVersion());
      versionField.setEnabled(false);
      XComponent typeField = form.findComponent(TYPE);
      typeField.setValue(license.getLicenseType());
      typeField.setEnabled(false);
      XComponent issuerField = form.findComponent(ISSUER);
      issuerField.setValue(license.getIssuer());
      issuerField.setEnabled(false);

      //customer
      XComponent customerNameField = form.findComponent(CUSTOMER_NAME);
      customerNameField.setValue(license.getCustomer().getName());
      customerNameField.setEnabled(false);
      XComponent customerNumberField = form.findComponent(CUSTOMER_NUMBER);
      customerNumberField.setValue(license.getCustomer().getNumber());
      customerNumberField.setEnabled(false);

      //partner
      XComponent partnerNumberField = form.findComponent(PARTNER_NUMBER);
      XComponent partnerNameField = form.findComponent(PARTNER_NAME);
      partnerNumberField.setEnabled(false);
      partnerNameField.setEnabled(false);
      if (license.getPartner() != null) {
         partnerNumberField.setValue(license.getPartner().getNumber());
         partnerNameField.setValue(license.getPartner().getName());
      }

      //expiration date
      XComponent validUntilField = form.findComponent(VALID_UNTIL);
      validUntilField.setValue(license.getValidUntil());
      validUntilField.setEnabled(false);

      //products
      XComponent productField = form.findComponent(PRODUCT);
      productField.setEnabled(false);
      XComponent productCodeField = form.findComponent(PRODUCT_CODE);
      productCodeField.setEnabled(false);
      XComponent productVersionField = form.findComponent(PRODUCT_VERSION);
      productVersionField.setEnabled(false);
      XComponent standardUserField = form.findComponent(STANDARD_USERS);
      XComponent managerUserField = form.findComponent(MANAGER_USERS);

      if (OpInitializer.isMultiUser()) {
         standardUserField.setEnabled(false);
         managerUserField.setEnabled(false);
      }
      else {
         standardUserField.setVisible(false);
         managerUserField.setVisible(false);
         XComponent standardUserLabel = form.findComponent(STANDARD_USERS_LABEL);
         XComponent managerUserLabel = form.findComponent(MANAGER_USERS_LABEL);
         standardUserLabel.setVisible(false);
         managerUserLabel.setVisible(false);         
      }

      OpLicenseProduct product = license.getProduct();
      if (product != null) {
         productField.setStringValue(product.getName());
         productCodeField.setStringValue(product.getCode());
         productVersionField.setStringValue(product.getVersion());

         if (OpInitializer.isMultiUser()) {
            String standardUsers = product.getStandardUsers();
            if (standardUsers.equals(String.valueOf(Integer.MAX_VALUE))) {
               standardUsers = OpLicenseProduct.UNLIMITED;
            }
            standardUserField.setStringValue(standardUsers);

            String managerUsers = product.getManagerUsers();
            if (managerUsers.equals(String.valueOf(Integer.MAX_VALUE))) {
               managerUsers = OpLicenseProduct.UNLIMITED;
            }
            managerUserField.setStringValue(managerUsers);
         }
      }

   }
}
