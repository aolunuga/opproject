/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.license;

import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;

/**
 * @author : mihai.costin
 */
public class OpLicenseLoader  extends XLoader {

   public OpLicenseLoader() {
      super(new XDocumentHandler(new OpLicenseSchema()));
      setUseResourceLoader(false);
   }
}
