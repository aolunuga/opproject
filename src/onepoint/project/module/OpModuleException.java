/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

/**
 * Exception class that is thrown by the modules of the onepoint application.
 *
 * @author : mihai.costin
 */
public class OpModuleException extends Exception {

   /**
    * The id of a resource map.
    */
   private String resourceMapId = null;

   /**
    * The id of a resource from the given resource map.
    */
   private String resourceId = null;

   /**
    * Creates a new exception with the given error message.
    * @param message a <code>String</code> representing the error message.
    */
   public OpModuleException(String message) {
      super(message);
   }

   public OpModuleException(String resourceMapId, String resourceId) {
      this.resourceMapId = resourceMapId;
      this.resourceId = "{$" + resourceId + "}";
   }

   /**
    * Gets the id of the resource map where a resource can be localized.
    * @return a <code>String</code> representing the id of resource map.
    */
   public String getResourceMapId() {
      return resourceMapId;
   }

   /**
    * Gets the id of a language resource.
    * @return a <code>String</code> representing the id of a language resource.
    */
   public String getResourceId() {
      return resourceId;
   }

}
