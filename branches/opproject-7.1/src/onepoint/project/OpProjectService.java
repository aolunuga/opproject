/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project;

import onepoint.error.XLocalizableException;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XService;
import onepoint.service.server.XSession;

import java.lang.reflect.Method;

public class OpProjectService extends XService {

   private static final XLog logger = XLogFactory.getServerLogger(OpProjectService.class);

   /**
    * Rollbacks the current<code>transaction</code> and releases the <code>broker</code>. This should be extracted
    * in a helper class.
    *
    * @param broker      a <code>OpBroker</code> representing the broker
    * @param transaction a <code>OpTransaction</code> representing the current transaction
    */
   protected void finalizeSession(OpTransaction transaction, OpBroker broker) {
      logger.info("Finalizing session...");
      if (transaction != null) {
         transaction.rollbackIfNecessary();
      }
      if (broker != null && broker.isOpen()) {
         broker.close();
      }
   }


   /**
    * @see onepoint.service.server.XService#findInstanceMethod(String)
    */
   protected Method findInstanceMethod(String methodName)
        throws NoSuchMethodException {
      try {
         Class clazz = this.getClass();
         return clazz.getMethod(methodName, new Class[]{OpProjectSession.class, XMessage.class});
      }
      catch (NoSuchMethodException e) {
         return super.findInstanceMethod(methodName);
      }
   }


   protected XMessage callJavaMethod(XSession session, XMessage request, Method javaMethod)
        throws Throwable {
      try {
         return super.callJavaMethod(session, request, javaMethod);
      }
      catch (XLocalizableException e) {
         if (e.getErrorMap() != null) {
            XMessage response = new XMessage();
            XError error = ((OpProjectSession) session).newError(e.getErrorMap(), e.getCode());
            response.setError(error);
            return response;
         }
         throw e;
      }
   }
}
