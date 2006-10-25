/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.service.server.XService;
import onepoint.service.server.XServiceLoader;

public class OpServiceFile {

   private String _file_name;
   private XService _service;

   public final void setFileName(String file_name) {
      _file_name = file_name;
   }

   public final XService loadService() {
      String service_file_name = onepoint.project.configuration.OpConfiguration.PROJECT_PACKAGE + _file_name;
      return new XServiceLoader().loadService(service_file_name);
   }

}