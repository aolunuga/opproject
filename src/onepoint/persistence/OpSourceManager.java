/**
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * This class allow you to register and retrieve later OpSource instances.
 */
public class OpSourceManager {

   /**
    * Defines the name used to register default source
    */
   private static final String DEFAULT_SOURCE_NAME = "default_source";

   /**
    * Mapping of source names to mounted object sources
    */
   private static Map<String, OpSource> sources = new HashMap<String, OpSource>();

   /**
    * Register sources using a given name.
    *
    * @param source     source to register
    * @param sourceName name registration name used for the provided source
    */
   private static void registerSource(OpSource source, String sourceName) {
      // To do: Check for uniqueness of name
      if (sourceName != null) {
         sources.put(sourceName, source);

         // Invoke on-register callback
         source.onRegister();
      }
      else {
         throw new NullPointerException("Could not register a source with a NULL source name.");
      }
   }

   /**
    * Register source using name defined into it (retrieved by getName() method).
    *
    * @param source source to be registered
    */
   public static void registerSource(OpSource source) {
      if (source != null) {
         registerSource(source, source.getName());
      }
      else {
         throw new NullPointerException("Could not register a NULL source.");
      }
   }

   /**
    * Register a default source.
    *
    * @param source default source to be registered.
    */
   public static void registerDefaultSource(OpSource source) {
      registerSource(source, DEFAULT_SOURCE_NAME);
   }

   /**
    * Retrieve registered default source
    *
    * @return default source
    */
   public static OpSource getDefaultSource() {
      return getSource(DEFAULT_SOURCE_NAME);
   }

   /**
    * Retrieve and return a source with a given name. If no source can be found with that name, return <code>Null</code>
    *
    * @param sourceName name of the source to be retrieved.
    * @return found source or <code>Null</code> if none was found.
    */
   public static OpSource getSource(String sourceName) {
      return (OpSource) (sources.get(sourceName));
   }

   /**
    * Closes the given Source and unregisters it.
    *
    * @param sourceName name of the source to be retrieved.
    */
   public static void closeSource(String sourceName) {
       OpSource currSource = (OpSource)sources.get(sourceName);
       if(currSource != null){
    	   currSource.close();
    	   sources.remove(sourceName);
       }
      
   }

   /**
    * Closes all registered Sources. Should be called before shutdown.
    *
    */
   public static void closeAllSources() {
	   for (String currKey : sources.keySet()) {
    	   closeSource(currKey);
       }
   }

   // Where to mount data sources (in sessions or here, more globally)?
   // ==> Maybe both: Global for everyone and just have to be opened in a session?!
   // ==> Master/default-source should be defined here

   // ==> We could mimik Unix here: Only "root/system" is allowed to mount/unmount
   // (in a session, but it is "remembered" persistently in the broker)

}
