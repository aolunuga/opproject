/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceInterceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
    * List of brokers which should be excluded from the cleanup mechanism.
    */
   private List<OpBroker> exceptBrokers = null;


   /**
    * @see onepoint.service.server.XServiceInterceptor#beforeAdvice(onepoint.service.server.XService,java.lang.reflect.Method,Object[])
    */
   public void beforeAdvice(XService service, Method method, Object[] arguments) {
      logger.debug("Applying before advice...");
      OpProjectSession session = this.getProjectSesssion(arguments);
      exceptBrokers = new ArrayList<OpBroker>(session.getBrokerList());
   }

   /**
    * @see onepoint.service.server.XServiceInterceptor#afterAdvice(onepoint.service.server.XService,java.lang.reflect.Method,Object[])
    */
   public void afterAdvice(XService service, Method method, Object[] arguments) {
      logger.debug("Applying after advice...");
      OpProjectSession session = this.getProjectSesssion(arguments);
      if (session != null) {
         session.cleanupSession(exceptBrokers, false);
      }
      exceptBrokers.clear();
   }

   /**
    * Searches the given object array for a project session.
    *
    * @param arguments a <code>Object[]</code>.
    * @return a <code>OpProjectSession</code> instance representing the session with which the service method was called,
    *         or <code>null</code> if there isn't any.
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
