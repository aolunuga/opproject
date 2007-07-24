/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.configuration;

/**
 * Singleton instance of this class holds all configuration parameters for the the client launcher.
 *
 * @author ovidiu.lupas
 */
public class OpClientLauncherConfiguration {

   /*singleton instance of this class */
   private final static OpClientLauncherConfiguration instance = new OpClientLauncherConfiguration();

   /*the name of the remote configuration file */
   public final static String CONFIGURATION_FILE = "remote-configuration.oxc.xml";

   /*instance fields */
   private String host;
   private int port;
   private String path;
   private boolean secure;

   /**
    * Private constructor to prevent instantiation by outside callers
    */
   private OpClientLauncherConfiguration() {
   }

   /**
    * Returns the singleton instance of this class
    *
    * @return <code>OpClientLauncherConfiguration</code>
    */
   public static OpClientLauncherConfiguration getInstance() {
      return instance;
   }

   /**
    * Returns the host for this configuration
    *
    * @return <code>String</code> the host
    */
   public String getHost() {
      return host;
   }

   /**
    * Sets up the host for this configuration
    *
    * @param host <code>String</code> the host
    */
   public void setHost(String host) {
      this.host = host;
   }

   /**
    * Returns the port for this configuration
    *
    * @return <code>int</code> port
    */
   public int getPort() {
      return port;
   }

   /**
    * Sets up the port for this configuration
    *
    * @param port <code>int</code> the port
    */
   public void setPort(int port) {
      this.port = port;
   }

   /**
    * Returns the path for this configuration
    *
    * @return <code>String</code> path
    */
   public String getPath() {
      return path;
   }

   /**
    * Sets up the path for this configuration
    *
    * @param path <code>String</code> the path
    */
   public void setPath(String path) {
      this.path = path;
   }

   /**
    * Returns a flag indicating that a secure protocol is used or not
    *
    * @return <code>boolean</code> the secure protocol flag
    */
   public boolean getSecure() {
      return secure;
   }

   /**
    * Sets up the secure protocol boolean flag
    *
    * @param secure <code>boolean</code>
    */
   public void setSecure(boolean secure) {
      this.secure = secure;
   }

}
