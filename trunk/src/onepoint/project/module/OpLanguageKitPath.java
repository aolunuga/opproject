/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.module;

import onepoint.resource.XLanguageKitLoader;

import java.util.List;

/**
 * This class holds the path to language resources
 *
 * @author lucian.furtos
 */
public class OpLanguageKitPath {

   private String path;

   /**
    * Default constructor
    */
   public OpLanguageKitPath() {
   }

   /**
    * Constructor that sets the value of the language resource path
    *
    * @param path the project relative resource path
    */
   public OpLanguageKitPath(String path) {
      this.path = path;
   }

   /**
    * Sets the project relative resource path
    *
    * @param path the project relative resource path
    */
   public final void setPath(String path) {
      this.path = path;
   }

   /**
    * Load all the language resources from the resource path represented by this class
    *
    * @return a <code>List</code> of <code>XLanguageKit</code>
    */
   public final List loadLanguageKits() {
      return new XLanguageKitLoader().loadLanguageKits(path);
   }

}
