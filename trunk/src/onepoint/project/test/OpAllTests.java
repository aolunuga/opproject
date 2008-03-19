/**
 *
 */
package onepoint.project.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import junitx.util.DirectorySuiteBuilder;
import junitx.util.SimpleTestFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

/**
 * @author dfreis
 */
public class OpAllTests {

   public static Test suite() {
      File baseDir = new File(OpAllTests.class.getResource("../../..").getFile());
      String destName = baseDir.getName();
      baseDir = baseDir.getParentFile();
      File buildFile = new File(baseDir, "opproject/build.xml");
      
      Project project = new Project();
      project.setBaseDir(new File(baseDir, "opproject"));
      project.setBasedir(new File(baseDir, "opproject").getAbsolutePath());
      project.setUserProperty("basedir", new File(baseDir, "opproject").getAbsolutePath());
      project.init();
      ProjectHelper helper = ProjectHelper.getProjectHelper();
      project.addReference("ant.projectHelper", helper);
      helper.parse(project, buildFile);
      Properties prop = new Properties();
      try {
         prop.load(new FileInputStream(new File(baseDir, "opproject/build.properties")));
      }
      catch (FileNotFoundException exc) {
         exc.printStackTrace();
      }
      catch (IOException exc1) {
         // TODO Auto-generated catch block
         exc1.printStackTrace();
      }
      // correct classes.dir if we compile eg to bin dir
      prop.setProperty("basedir", new File(baseDir, "opproject").getAbsolutePath());
      prop.setProperty("classes.dir", "../"+destName);

      boolean cont = true;
      while (cont) {
         cont = false;
         for(Enumeration en = prop.keys(); en.hasMoreElements(); ) {
            String key = (String) en.nextElement();
            String value = prop.getProperty(key);
            String newValue= replace(value, prop);
            if (!value.equals(newValue)) {
               cont = true;
               prop.setProperty(key, newValue);
            }
         }
      }
      for(Enumeration en = prop.keys(); en.hasMoreElements(); ) {
        String key = (String) en.nextElement();
        String value = prop.getProperty(key);
        project.setProperty(key, value);
      }
      DefaultLogger log = new DefaultLogger();
      log.setErrorPrintStream(System.err);
      log.setOutputPrintStream(System.out);
      log.setMessageOutputLevel(Project.MSG_INFO);
      project.addBuildListener(log);
      project.initProperties();
      
      project.executeTarget("copy.resources.no.express4j.jars");
      project.executeTarget("copy.test.resources");
      // copy all configs to opHome
      DirectorySuiteBuilder builder = new DirectorySuiteBuilder();
      builder.setFilter(new SimpleTestFilter() {
         public boolean include(String classpath) {
            boolean ret = (super.include(classpath) &&
                  classpath.contains("/onepoint/project/") &&
                  !classpath.contains("/onepoint/project/od/") &&
                  !classpath.contains("/onepoint/project/team/") &&
                  !SimpleTestFilter.getClassName(classpath).contains("/install/") &&
                  !SimpleTestFilter.getClassName(classpath).contains("/hibernate/test/") &&
                  !SimpleTestFilter.getClassName(classpath).endsWith("/OpBaseTestCase.class"));
//            boolean ret = (super.include(classpath) &&
//                  classpath.contains("/onepoint/project/team/") &&
//                 !classpath.contains("/install/") &&
//                 !classpath.contains("/hibernate/test/") &&
//                 !classpath.endsWith("/OpBaseTestCase.class"));
            if (ret) {
               System.err.println("testing: " + classpath);
            }
            return (ret);
         }
      });

      TestSuite suite;
      try {
//         URL url = OpAllTests.class.getResource("../..");
         URL url = OpAllTests.class.getResource("../../..");
         suite = new TestSuite();
         // add closed test cases
         suite.addTest(builder.suite(url.getFile()));
         return suite;
      }
      catch (Exception e) {
         e.printStackTrace();
         return (null);
      }
   }
   
   private static String replace(String match, Properties prop) {
      Pattern pat = Pattern.compile("\\$\\{([^\\}]+)\\}");//\\{(.*)\\}");
      Matcher mat = pat.matcher(match);
      StringBuffer replaced = new StringBuffer();
      while (mat.find()) {
         String rep = (String)prop.get(mat.group(1));
         if (rep != null) {
            String repNew = replace(rep, prop);
            if (!rep.equals(repNew)) {
               rep = repNew;
            }
         }
         if (rep == null) {
            // NOTE: had to add leading \\ otherwise java thinks its a group ?!
            rep = "\\${"+mat.group(1)+"}";
         }
         mat.appendReplacement(replaced, rep);
      }
      mat.appendTail(replaced);
      return replaced.toString();
   }
//
//   public static Test suite() {
//      // disable logger
//      DirectorySuiteBuilder builder = new DirectorySuiteBuilder();
//      builder.setFilter(new SimpleTestFilter() {
//         public boolean include(String classpath) {
//            System.err.println("testing base: " + classpath);
//
//            boolean ret = (super.include(classpath) &&
//                 !classpath.contains("/onepoint/project/od/") &&
//                 !SimpleTestFilter.getClassName(classpath).contains("/install/") &&
//                 !SimpleTestFilter.getClassName(classpath).contains("/hibernate/test/") &&
//                 !SimpleTestFilter.getClassName(classpath).endsWith("/OpBaseTestCase.class"));
//            return (ret);
//         }
//      });
//
//      Test suite;
//      try {
//         URL url = OpAllTests.class.getResource("../../..");
//         System.err.println("url is: " + url.getFile());
////      suite = new TestSuite();
////      suite.addTestSuite(Class.forName("onepoint.project.modules.configuration_wizard.test.OpConfigurationWizardTest"));
//         suite = builder.suite(url.getFile());
//         System.err.println("suite is: " + suite.countTestCases());
//         return suite;
//      }
//      catch (Exception e) {
//         e.printStackTrace();
//         return (null);
//      }
//   }
}
