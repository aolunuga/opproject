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
public class OpDocumentErrorMap extends XErrorMap {


   /**
    * @see onepoint.error.XErrorMap#XErrorMap(String)
    */
   public OpDocumentErrorMap() {
      super("documents.error");
      registerErrorCode(OpDocumentError.OUT_OF_MEMORY, OpDocumentError.OUT_OF_MEMORY_NAME);
      registerErrorCode(OpDocumentError.FILE_NOT_FOUND, OpDocumentError.FILE_NOT_FOUND_NAME);
   }
}
