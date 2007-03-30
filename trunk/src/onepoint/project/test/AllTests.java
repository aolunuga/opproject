/**
 * 
 */
package onepoint.project.test;

import java.net.URL;

import onepoint.log.XLogFactory;
import onepoint.log.server.XLog4JLog;

import junit.framework.Test;
import junit.framework.TestSuite;
import junitx.util.DirectorySuiteBuilder;
import junitx.util.SimpleTestFilter;
import junitx.util.TestFilter;

/**
 * @author dfreis
 *
 */
public class AllTests
{

  public static Test suite() {
    // disable logger
    DirectorySuiteBuilder builder = new DirectorySuiteBuilder();
    builder.setFilter(new SimpleTestFilter() {
         public boolean include(String classpath) {
           System.err.println("testing: "+classpath);

           boolean ret = (super.include(classpath) && 
                     !SimpleTestFilter.getClassName(classpath).contains("/install/") &&
                     !SimpleTestFilter.getClassName(classpath).contains("/hibernate/test/") &&
                     !SimpleTestFilter.getClassName(classpath).endsWith("/OpBaseTestCase.class"));
           System.err.println("end of testing");
           return(ret);
         }
    });

    Test suite;
    try
    {
      URL url = AllTests.class.getResource("../../..");
      System.err.println("url is: "+url.getFile());
//      suite = new TestSuite();
//      suite.addTestSuite(Class.forName("onepoint.project.modules.configuration_wizard.test.OpConfigurationWizardTest"));
      suite = builder.suite(url.getFile());
      System.err.println("suite is: "+suite.countTestCases());
      return suite;
    } catch (Exception e)
    {
      e.printStackTrace();
      return(null);
    }
  }
}
