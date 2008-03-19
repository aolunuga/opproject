/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.documents;

/**
 * Enum class that defines all the error codes/constant used for the documents module.
 *
 * @author lucian.furtos
 */
public interface OpDocumentsError {
   /**
    * Error constants.
    */
   static final int OUT_OF_MEMORY = 1;
   static final int FILE_NOT_FOUND = 2;

   /**
    * Error names
    */
   static final String OUT_OF_MEMORY_NAME = "OutOfMemory";
   static final String FILE_NOT_FOUND_NAME = "FileNotFound";   
}
