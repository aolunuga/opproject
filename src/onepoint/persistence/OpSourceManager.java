/**
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class allow you to register and retrieve later OpSource instances.
 */
public class OpSourceManager {

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
   private static void registerSource(String sourceName, OpSource source) {
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
         registerSource(source.getName(), source);
      }
      else {
         throw new NullPointerException("Could not register a NULL source.");
      }
   }

   /**
    * Retrieve and return a source with a given name. If no source can be found with that name, return <code>Null</code>
    *
    * @param sourceName name of the source to be retrieved.
    * @return found source or <code>Null</code> if none was found.
    */
   public static OpSource getSource(String sourceName) {
      OpSource source = sources.get(sourceName);
      if (source == null) {
         // TODO - here we should throw another exception, an appropriate one.
         throw new NullPointerException("No source was registered with name: " + sourceName);
      }

      return source;
   }

   /**
    * Returns the default source
    *
    * @return an <code>OpSource</code> instance or <code>null</code> if the default
    * source hasn't been registered.
    */
   public static OpSource getDefaultSource() {
      return sources.get(OpSource.DEFAULT_SOURCE_NAME);
   }

   /**
    * Checks whether the source manager contains a source with the given name.
    * @param sourceName a <code>String</code> representing the name of a source.
    * @return <code>true</code> if this source manager has registered a source with
    * the given name, <code>false</code> otherwise.
    */
   public static boolean containsSource(String sourceName) {
      return sources.keySet().contains(sourceName);
   }

   /**
    * Closes the given Source and unregisters it.
    *
    * @param sourceName name of the source to be retrieved.
    */
   public static void closeSource(String sourceName) {
      OpSource currSource = (OpSource) sources.get(sourceName);
      if (currSource != null) {
         currSource.close();
         sources.remove(sourceName);
      }
   }

   /**
    * Closes all registered Sources. Should be called before shutdown.
    */
   public static void closeAllSources() {
      for (String currKey : sources.keySet()) {
         closeSource(currKey);
      }
   }

   /**
    * Clear all registered Sources.
    */
   public static void clearAllSources() {
      for (OpSource source : sources.values()) {
         source.clear();
      }
   }


   /**
    * Returns all registered <code>OpSource</code>s
    *
    * @return all registered sources
    */
   public static Collection<OpSource> getAllSources() {
      return sources.values();
   }

   // Where to mount data sources (in sessions or here, more globally)?
   // ==> Maybe both: Global for everyone and just have to be opened in a session?!
   // ==> Master/default-source should be defined here

   // ==> We could mimik Unix here: Only "root/system" is allowed to mount/unmount
   // (in a session, but it is "remembered" persistently in the broker)

}
