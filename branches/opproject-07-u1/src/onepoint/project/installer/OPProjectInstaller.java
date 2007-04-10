/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.installer;

import onepoint.install.simple.OPSimpleGUIInstaller;


/**
 * @author gerald
 */
public class OPProjectInstaller {

   public static void main(String[] arguments) {

      // Parse command-line paramters
      String target_path = null;
      if ((arguments.length == 2) && (arguments[0].equals("-target_path")))
         target_path = arguments[1];
      else {
         System.err.println("USAGE: OPProjectInstaller -target_path <target-installation-dir>");
         System.exit(1);
      }

      OPSimpleGUIInstaller installer = new OPSimpleGUIInstaller(target_path);
      // TODO: How to choose locale?
      // (Probably have to add another step for language selection; maybe call it step 0)
      // (Note: Step 0 would only have to be engaged if more than one locale -- XInstallFormProvider)
      installer.loadLocaleMap("onepoint/install/simple/test/locales.olm.xml");
      installer.loadLanguageKit("onepoint/install/simple/test/i18n/simple_installer_test_de.olk.xml");
      /*
      try {
         // TODO: Maybe license text is also part of language-kit
         // (Although would be good if the license text on disk would be identical)
         // (Possible solution: Hold a map of license texts; one per locale-ID)
         installer.loadLicenseText("onepoint/install/simple/test/license_de.txt");
      }
      catch (OPInstallationException e) {
         System.err.println("ERROR: Could not load file 'license.txt': " + e);
         System.exit(1);
      }
      */
      installer.addCreateDirectory("bin");
      installer.addCreateDirectory("lib");
      installer.addCreateDirectory("schemas");

      installer.addCopyFile("dist/readme.txt", "readme.txt");
      installer.addCopyFile("dist/registry.oxr.xml", "registry.oxr.xml");
      installer.addCopyFile("dist/schemas/hibernate-mapping-3.0.dtd", "schemas/hibernate-mapping-3.0.dtd");

      installer.addCopyFile("dist/bin/onepoint.jar", "bin/onepoint.jar");
      installer.addCopyFile("dist/lib/hibernate3.jar", "lib/hibernate3.jar");
      installer.addCopyFile("dist/lib/activation.jar", "lib/activation.jar");
      installer.addCopyFile("dist/lib/antlr-2.7.5H3.jar", "lib/antlr-2.7.5H3.jar");
      installer.addCopyFile("dist/lib/asm-attrs.jar", "lib/asm-attrs.jar");
      installer.addCopyFile("dist/lib/asm.jar", "lib/asm.jar");
      installer.addCopyFile("dist/lib/c3p0-0.8.5.2.jar", "lib/c3p0-0.8.5.2.jar");
      installer.addCopyFile("dist/lib/cglib-2.1.jar", "lib/cglib-2.1.jar");
      installer.addCopyFile("dist/lib/commons-collections-2.1.1.jar", "lib/commons-collections-2.1.1.jar");
      installer.addCopyFile("dist/lib/commons-logging-1.0.4.jar", "lib/commons-logging-1.0.4.jar");
      installer.addCopyFile("dist/lib/dom4j-1.6.jar", "lib/dom4j-1.6.jar");
      installer.addCopyFile("dist/lib/ehcache-1.1.jar", "lib/ehcache-1.1.jar");
      installer.addCopyFile("dist/lib/jaxen-1.1-beta-4.jar", "lib/jaxen-1.1-beta-4.jar");
      installer.addCopyFile("dist/lib/jdbc2_0-stdext.jar", "lib/jdbc2_0-stdext.jar");
      installer.addCopyFile("dist/lib/jta.jar", "lib/jta.jar");
      installer.addCopyFile("dist/lib/mail.jar", "lib/mail.jar");
      installer.addCopyFile("dist/lib/xerces-2.6.2.jar", "lib/xerces-2.6.2.jar");
      installer.addCopyFile("dist/lib/xml-apis.jar", "lib/xml-apis.jar");
      installer.addCopyFile("dist/lib/mysql-connector-java-3.1.8-bin.jar", "lib/mysql-connector-java-3.1.8-bin.jar");
      
      // TODO: Have another action "createFile" which lets us create script/startup files
      StringBuffer configuration = new StringBuffer();
      configuration.append("<configuration>\r\n");
      configuration.append("   <database-url>jdbc:mysql:///test</database-url>\r\n");
      configuration.append("   <database-login></database-login>\r\n");
      configuration.append("   <database-password></database-password>\r\n");
      configuration.append("</configuration>\r\n");
      installer.addCreateFile("configuration.oxc.xml", configuration.toString());

      StringBuffer class_path = new StringBuffer();
      class_path.append(target_path + "/bin/onepoint.jar:");
      class_path.append(target_path + "/lib/hibernate3.jar:");
      class_path.append(target_path + "/lib/activation.jar:");
      class_path.append(target_path + "/lib/antlr-2.7.5H3.jar:");
      class_path.append(target_path + "/lib/asm-attrs.jar:");
      class_path.append(target_path + "/lib/asm.jar:");
      class_path.append(target_path + "/lib/c3p0-0.8.5.2.jar:");
      class_path.append(target_path + "/lib/cglib-2.1.jar:");
      class_path.append(target_path + "/lib/commons-collections-2.1.1.jar:");
      class_path.append(target_path + "/lib/commons-logging-1.0.4.jar:");
      class_path.append(target_path + "/lib/dom4j-1.6.jar:");
      class_path.append(target_path + "/lib/ehcache-1.1.jar:");
      class_path.append(target_path + "/lib/jaxen-1.1-beta-4.jar:");
      class_path.append(target_path + "/lib/jdbc2_0-stdext.jar:");
      class_path.append(target_path + "/lib/jta.jar:");
      class_path.append(target_path + "/lib/mail.jar:");
      class_path.append(target_path + "/lib/xerces-2.6.2.jar:");
      class_path.append(target_path + "/lib/xml-apis.jar:");
      class_path.append(target_path + "/lib/mysql-connector-java-3.1.8-bin.jar");
      
      StringBuffer opproject_bat = new StringBuffer();
      // TODO: Onepoint should be first JAR, Hibernate second (class- and resource-loading performance)
      opproject_bat.append("java -classpath ");
      opproject_bat.append(class_path);
      opproject_bat.append(" onepoint.project.application.OpBasicApplication -onepoint_home ");
      opproject_bat.append(target_path);
      installer.addCreateFile("opproject.bat", opproject_bat.toString());

      StringBuffer opprojecttestdata_bat = new StringBuffer();
      opprojecttestdata_bat.append("java -classpath ");
      opprojecttestdata_bat.append(class_path);
      opprojecttestdata_bat.append(" onepoint.project.modules.project.test.OpProjectTestData -onepoint_home ");
      opprojecttestdata_bat.append(target_path);
      installer.addCreateFile("opprojecttestdata.bat", opprojecttestdata_bat.toString());

      installer.run();

   }

}
