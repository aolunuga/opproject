/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.license;

import onepoint.license.OpLicense;
import onepoint.license.OpLicenseCustomer;
import onepoint.license.OpLicensePartner;
import onepoint.license.OpLicenseProduct;
import onepoint.xml.XSchema;

import java.util.Iterator;
import java.util.List;

/**
 * @author : mihai.costin
 */
public class OpLicenseSchema extends XSchema {

   public OpLicenseSchema(){
      //register the node handlers
      OpLicenseHandler node_handler = new OpLicenseHandler();
      
      registerTags(OpLicense.getTags(), node_handler);
      registerTags(OpLicenseProduct.getTags(), node_handler);
      registerTags(OpLicenseCustomer.getTags(), node_handler);
      registerTags(OpLicensePartner.getTags(), node_handler);
   }

   private void registerTags(List tags, OpLicenseHandler node_handler) {
      for (Iterator iterator = tags.iterator(); iterator.hasNext();) {
         String tag = (String) iterator.next();
         registerNodeHandler(tag, node_handler);
      }
   }
}
