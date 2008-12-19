/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report;

import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * Class loader that is nothing more than a simple URL class loader, that is being set by the report manager.
 * This class is needed by the Report Manager to make sure that a correct class loader has been set when trying
 * to retrieve report resources.
 *
 * @author horia.chiorean
 */
public class OpReportCustomClassLoader extends URLClassLoader {

   /**
    * @see URLClassLoader#URLClassLoader(java.net.URL[], ClassLoader)
    */
   public OpReportCustomClassLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
   }
}
