/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceInterceptor;

import java.lang.reflect.Method;

/**
 * Service interceptor which performs cleanup for service method invocations.
 *
 * @author horia.chiorean
 */
public class OpServiceInterceptor extends XServiceInterceptor {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpServiceInterceptor.class);

   /**
    * @see onepoint.service.server.XServiceInterceptor#afterAdvice(onepoint.service.server.XService, java.lang.reflect.Method, Object[])
    */
   public void afterAdvice(XService service, Method method, Object[] arguments) {
      logger.info("Applying after advice...");
      OpProjectSession session = this.getProjectSesssion(arguments);
      if (session != null) {
         session.cleanupSession();
      }
   }

   /**
    * Searches the given object array for a project session.
    * @param arguments a <code>Object[]</code>.
    * @return a <code>OpProjectSession</code> instance representing the session with which the service method was called,
    * or <code>null</code> if there isn't any.
    */
   private OpProjectSession getProjectSesssion(Object[] arguments) {
      for (int i = 0; i < arguments.length; i++) {
         Object argument = arguments[i];
         if (argument instanceof OpProjectSession) {
            return (OpProjectSession) argument;
         }
      }
      return null;
   }
}
