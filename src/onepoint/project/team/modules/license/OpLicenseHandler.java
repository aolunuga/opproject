/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.license;

import onepoint.license.*;
import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;
import java.util.Iterator;

/**
 * @author : mihai.costin
 */
public class OpLicenseHandler implements XNodeHandler {


   public Object newNode(XContext context, String name, HashMap attributes) {

      OpLicenseEntity entity;
      if (name.equals(OpLicense.LICENSE_TAG)) {
         entity = new OpLicense();
      }
      else if (name.equals(OpLicenseProduct.PRODUCT_TAG)) {
         entity = new OpLicenseProduct();
      }
      else if (name.equals(OpLicensePartner.PARTNER_TAG)) {
         entity = new OpLicensePartner();
      }
      else if (name.equals(OpLicenseCustomer.CUSTOMER_TAG)) {
         entity = new OpLicenseCustomer();
      }
      else {
         entity = new OpLicenseEntity(name);
      }

      //add attributes
      attributes.keySet();
      for (Iterator iterator = attributes.keySet().iterator(); iterator.hasNext();) {
         String attributeName = (String) iterator.next();
         String attributeValue = (String) attributes.get(attributeName);
         entity.addValue(attributeName, attributeValue);
      }

      return entity;
   }

   public void addNodeContent(XContext context, Object node, String content) {
      String nodeName = ((OpLicenseEntity) node).getName();
      if (nodeName != null){
         ((OpLicenseEntity) node).addValue(nodeName, content);
      }
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      if (parent != null) {
         ((OpLicenseEntity) parent).addValue(name, node);
      }
   }
}
