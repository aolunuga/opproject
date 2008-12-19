/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.documents;

import onepoint.error.XErrorMap;

/**
 * Error map class used for the documents module.
 *
 * @author lucian.furtos
 */
public class OpDocumentsErrorMap extends XErrorMap {


   /**
    * @see onepoint.error.XErrorMap#XErrorMap(String)
    */
   public OpDocumentsErrorMap() {
      super("documents.error");
      registerErrorCode(OpDocumentsError.OUT_OF_MEMORY, OpDocumentsError.OUT_OF_MEMORY_NAME);
      registerErrorCode(OpDocumentsError.FILE_NOT_FOUND, OpDocumentsError.FILE_NOT_FOUND_NAME);
   }
}
