/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.project.util.OpProjectConstants;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceLoader;

public class OpServiceFile {

   private String fileName;
   private XService service;

   public final void setFileName(String file_name) {
      fileName = file_name;
   }

   public final XService loadService() {
      String service_file_name = OpProjectConstants.PROJECT_PACKAGE + fileName;
      return new XServiceLoader().loadService(service_file_name);
   }

   public String getFileName() {
      return fileName;
   }
}