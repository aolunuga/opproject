/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.documents;

import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.service.XMessage;

/**
 * Service class that handles all the document related operations
 *
 * @author lucian.furtos
 */
public class OpDocumentsService extends OpProjectService {

   /**
    * This service's error map
    */
   private static final OpDocumentErrorMap ERROR_MAP = new OpDocumentErrorMap();

   /**
    * Returns to the client the message for exceeding the attachment limit.
    *
    * @param session a <code>OpProjectSession</code> object representing the current session.
    * @param request a <code>XMessage</code> representing the current request.
    * @return a response in the form of a <code>XMessage</code> object.
    */
   public XMessage displayOutOfMemory(OpProjectSession session, XMessage request) {
      XMessage reply = new XMessage();
      reply.setError(session.newError(ERROR_MAP, OpDocumentError.OUT_OF_MEMORY));
      return reply;
   }

   /**
    * Returns to the client the message attachment file not found.
    *
    * @param session a <code>OpProjectSession</code> object representing the current session.
    * @param request a <code>XMessage</code> representing the current request.
    * @return a response in the form of a <code>XMessage</code> object.
    */
   public XMessage displayFileNotFound(OpProjectSession session, XMessage request) {
      XMessage reply = new XMessage();
      reply.setError(session.newError(ERROR_MAP, OpDocumentError.FILE_NOT_FOUND));
      return reply;
   }
}
