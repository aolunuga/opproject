/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.util;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.util.XEnvironmentManager;

import java.io.*;
import java.util.Properties;

/**
 * Environment manager class for the One Point applications.
 *
 * @author horia.chiorean
 */
public class OpEnvironmentManager {

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpEnvironmentManager.class);

   /**
    * The name of the OS.
    */
   private static final String OS_NAME = System.getProperty("os.name");

   /**
    * A command that will be executed to get the env. variables.
    */
   private static String COMMAND;

   /**
    * The environment properties.
    */
   private static Properties envProps = new Properties();

   static {
      if (OpEnvironmentManager.OS_NAME.equals("Windows NT") || OpEnvironmentManager.OS_NAME.equals("Windows 2000") || OpEnvironmentManager.OS_NAME.equals("Windows XP") || OpEnvironmentManager.OS_NAME.equals("Windows 2003"))
      {
         OpEnvironmentManager.COMMAND = "cmd.exe /C set";
      }
      else
      if (OpEnvironmentManager.OS_NAME.equals("Windows 95") || OpEnvironmentManager.OS_NAME.equals("Windows 98") || OpEnvironmentManager.OS_NAME.equals("Windows Me"))
      {
         OpEnvironmentManager.COMMAND = "command.com /C set";
      }
      else if (OpEnvironmentManager.OS_NAME.equals("Mac OS X") || OpEnvironmentManager.OS_NAME.equals("Linux")) {
         OpEnvironmentManager.COMMAND = "env";
      }
   }

   private OpEnvironmentManager() {
   }


   /**
    * Performs loading of the OS environment variables.
    */
   private synchronized static void loadEnvironmentProperties() {
      // get runtime and execute windows command
      Runtime rt = Runtime.getRuntime();
      try {
         Process process = rt.exec(COMMAND);
         InputStream stdin = process.getInputStream();
         new ProcessOutputStreamReader(stdin).start();
         //wait for the process to finish
         process.waitFor();
         //wait on class monitor until envProps is filled up
         OpEnvironmentManager.class.wait();
      }
      catch (Exception e) {
         logger.error("Error occured while executing command that retrives environment variables", e);
      }
   }

   /**
    * Returns the value of the specified OS environment variable from the property list or <code>null</code> if is not
    * found.
    *
    * @param name <code>String</code> representing the environment variable
    * @return the value of the specified environment variable
    */
   public static String getEnvironmentVariable(String name) {
      if (envProps.size() == 0) {
         loadEnvironmentProperties();
      }
      String property;
      if (name.equals(onepoint.project.configuration.OpConfiguration.ONEPOINT_HOME)) {
         File dummy = new File("");
         String path = dummy.getAbsolutePath();

         if (path != null) {
            File testRegistry = new File(path + "/" + "registry.oxr.xml");
            if (!testRegistry.exists()) {
               path = envProps.getProperty(name);
               if (path == null) {
                  logger.fatal("registry.oxr.xml file does not exist");
                  return null;
               }
            }
         }
         else {
            path = envProps.getProperty(name);
         }
         property = XEnvironmentManager.convertPathToSlash(path);
      }
      else {
         property = envProps.getProperty(name);
      }
      return property;
   }


   /**
    * Reads the process's output stream and performs initialization of the environment variables properties list
    */
   private static class ProcessOutputStreamReader extends Thread {
      private InputStream is;

      private ProcessOutputStreamReader(InputStream is) {
         this.is = is;
      }

      public void run() {
         try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
               int index = -1;
               if ((index = line.indexOf("=")) > -1) {
                  String key = line.substring(0, index).trim();
                  String value = line.substring(index + 1).trim();
                  envProps.setProperty(key, value);
               }
               else {
                  envProps.setProperty(line, "");
               }
            }
         }
         catch (IOException e) {
            logger.error("Error occured while reading environment variables", e);
         }
         finally {
            synchronized (OpEnvironmentManager.class) {
               OpEnvironmentManager.class.notify();
            }
         }
      }
   }
}

