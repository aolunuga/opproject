/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.util;

import onepoint.express.util.XConstants;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.util.XEnvironmentManager;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Environment manager class for the One Point applications.
 *
 * @author horia.chiorean
 */
public final class OpEnvironmentManager {

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getClientLogger(OpEnvironmentManager.class);

   /**
    * The name of the OS.
    */
   private static final String OS_NAME = System.getProperty("os.name");

   /**
    * The simbolic name of the onepoint home environment variable
    */
   private static final String ONEPOINT_HOME = "ONEPOINT_HOME";

   /**
    * The path to the application data folder.
    */
   private static String DATA_FOLDER_PATH;

   /**
    * A command that will be executed to get the env. variables.
    */
   private static String COMMAND;

   /**
    * The environment properties.
    */
   private static Properties envProps = new Properties();

   /**
    * A map of [productCode, boolean] pairs, indicating which application is multi user and which is not.
    */
   private static final Map PRODUCT_CODES_MAP = new HashMap();

   /**
    * Initialize the product codes map
    */
   static {
      PRODUCT_CODES_MAP.put(OpProjectConstants.BASIC_EDITION_CODE, Boolean.FALSE);
      PRODUCT_CODES_MAP.put(OpProjectConstants.PROFESSIONAL_EDITION_CODE, Boolean.FALSE);
      PRODUCT_CODES_MAP.put(OpProjectConstants.OPEN_EDITION_CODE, Boolean.TRUE);
      PRODUCT_CODES_MAP.put(OpProjectConstants.TEAM_EDITION_CODE, Boolean.TRUE);
      PRODUCT_CODES_MAP.put(OpProjectConstants.ON_DEMAND_EDITION_CODE, Boolean.TRUE);
      PRODUCT_CODES_MAP.put(OpProjectConstants.NETWORK_EDITION_CODE, Boolean.TRUE);
   }

   /**
    * The product code used in the initialization process
    */
   private static String productCode = null;
   public static final String ONEPOINT_FOLDER = "Onepoint Project";

   /**
    * Initializes the command variable of the environment manager according to the type of OS.
    */
   static {
      if (OpEnvironmentManager.OS_NAME.equals("Windows NT") || OpEnvironmentManager.OS_NAME.equals("Windows 2000") || OpEnvironmentManager.OS_NAME.equals("Windows XP") || OpEnvironmentManager.OS_NAME.equals("Windows 2003")) {
         OpEnvironmentManager.COMMAND = "cmd.exe /C set";
      }
      else
      if (OpEnvironmentManager.OS_NAME.equals("Windows 95") || OpEnvironmentManager.OS_NAME.equals("Windows 98") || OpEnvironmentManager.OS_NAME.equals("Windows Me")) {
         OpEnvironmentManager.COMMAND = "command.com /C set";
      }
      else if (OpEnvironmentManager.OS_NAME.equals("Mac OS X") || OpEnvironmentManager.OS_NAME.equals("Linux")) {
         OpEnvironmentManager.COMMAND = "env";
      }
   }

   /**
    * Private constructor
    */
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
    * Returns the value of the specified Onepoint project environment variable from the property list or <code>null</code> if is not
    * found.
    *
    * @param name <code>String</code> representing the environment variable
    * @return the value of the specified environment variable or <code>null</code> if the variable can't be found
    */
   private static String getEnvironmentVariable(String name) {
      String property = envProps.getProperty(name);
      //if property not found, try to get it from the OS environment
      if (property == null) {
         loadEnvironmentProperties();
         property = envProps.getProperty(name);
         if (property != null && name.equals(ONEPOINT_HOME)) {
            File onepointHome = new File(property);
            if (onepointHome.exists() && onepointHome.isDirectory()) {
               envProps.put(ONEPOINT_HOME, XEnvironmentManager.convertPathToSlash(property));
            }
            else {
               return null;
            }
         }
      }
      return property;
   }

   /**
    * Gets the home directory path of the application.
    *
    * @return a <code>String</code> representing the home directory of the application.
    */
   public static String getOnePointHome() {
      return getEnvironmentVariable(ONEPOINT_HOME);
   }

   /**
    * Sets the home directory path of the application.
    *
    * @param onepointHome a <code>String</code> representing the path of the application home. Path uses "/" separator.
    */
   public static void setOnePointHome(String onepointHome) {
      envProps.setProperty(ONEPOINT_HOME, XEnvironmentManager.convertPathToSlash(onepointHome));
      System.setProperty(XConstants.LOGGER_HOME, envProps.getProperty(ONEPOINT_HOME)+XConstants.LOGGER_FOLDER);
   }

   /**
    * Sets the application data folder path of the application.
    *
    * @param dataFolder a <code>String</code> representing the path of the application data folder.
    */
   private static void setDataFolderPath(String dataFolder) {
      DATA_FOLDER_PATH = dataFolder;
      //also create the folder if it doesn't exist
      File folder = new File(DATA_FOLDER_PATH);
      if (!folder.exists() || !folder.isDirectory()) {
         folder.mkdir();
      }
   }

   /**
    * Sets the data folder path using a given db path.
    *
    * @param databaseURL data base folders path
    */
   public static void setDataFolderPathFromDbPath(String databaseURL) {
      File url = new File(databaseURL);
      File parentFile = url.getParentFile();

      File dataFolder = parentFile.getParentFile();
      if (dataFolder == null) {
         throw new IllegalArgumentException("Given path is not valid");
      }
      setDataFolderPath(dataFolder.getAbsolutePath());

   }


   /**
    * Returns the path to the onepoint data directory. This path is system dependent.
    *
    * @return Path of the onepoint data folder. Path uses "/" separator.
    */
   public static String getDataFolderPath() {
      if (isMultiUser()) {
         return getOnePointHome();
      }
      else {
         if (DATA_FOLDER_PATH == null) {
            String userHome = System.getProperty("user.home");
            String path;
            if (OpEnvironmentManager.OS_NAME.equals("Mac OS X")) {
               //$HOME/Library/Application Support/Onepoint Project
               path = userHome + File.separator + "Library" + File.separator + "Application Support" +
                    File.separator + ONEPOINT_FOLDER;
            }
            else {
               //$HOME/Onepoint Project
               path = userHome + File.separator + ONEPOINT_FOLDER;
            }
            path = XEnvironmentManager.convertPathToSlash(path);
            setDataFolderPath(path);
         }
      }
      return DATA_FOLDER_PATH;
   }


   /**
    * Returns the value of the multi-user flag, using the product code.
    *
    * @return true if the application is in multi-user mode
    */
   public static boolean isMultiUser() {
      Boolean isMultiUser = (Boolean) PRODUCT_CODES_MAP.get(productCode);
      if (isMultiUser == null) {
         throw new UnsupportedOperationException("Cannot determine whether application is multi user or not");
      }
      return isMultiUser.booleanValue();
   }


   public static boolean isNetworkEdition() {
      return productCode.equals(OpProjectConstants.NETWORK_EDITION_CODE);
   }


   /**
    * Returns the product code - one of the keys in PRODUCT_CODES_MAP
    *
    * @return product code.
    */
   public static String getProductCode() {
      return productCode;
   }

   /**
    * sets the product code
    *
    * @param productCode new product code
    */
   public static void setProductCode(String productCode) {
      OpEnvironmentManager.productCode = productCode;
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
            br.close();
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

