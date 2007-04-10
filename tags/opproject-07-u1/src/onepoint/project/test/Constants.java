/**
 * Copyright(c) OnePoint Software GmbH 2005. All Rights Reserved.
 */
package onepoint.project.test;

import java.io.File;

/**
 * Here we define common constatnt used through testing process.
 *
 * @author calin.pavel
 */
public interface Constants {
   // Defines the name of the registry file used through tests.
   public static final String REGISTRY_FILE = "registry_test.oxr.xml";

   // OnePoint Home path used into tests.
   public static final String ONEPOINT_HOME = new File("").getAbsolutePath() + "/build/classes/onepoint/project/test";

   // Testing resource path
   public static final String RESOURCE_PATH = "onepoint/project";

   // Locales file.
   public static final String LOCALES_OLM_XML = "/locales.olm.xml";
}