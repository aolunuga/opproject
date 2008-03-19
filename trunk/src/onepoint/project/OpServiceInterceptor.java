/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project;

import net.sf.cglib.proxy.MethodProxy;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
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
    * @see onepoint.service.server.XServiceInterceptor#intercept(Object,java.lang.reflect.Method,Object[],net.sf.cglib.proxy.MethodProxy)
    */
   @Override
   public Object intercept(Object object, Method method, Object[] objects, MethodProxy methodProxy)
        throws Throwable {
      List<OpBroker> brokersToExclude = new ArrayList<OpBroker>();
      OpProjectSession session = this.getProjectSesssion(objects);
      if (session != null) {
         logger.debug("Adding " + brokersToExclude.size() + " brokers to the exclude list ");
         brokersToExclude.addAll(session.getBrokerList());
      }

      Object result = methodProxy.invokeSuper(object, objects);

      if (session != null) {
         logger.debug("Clearing " + brokersToExclude.size() + " brokers");
         session.cleanupSession(brokersToExclude, false);
      }
      return result;
   }

   /**
    * Searches the given object array for a project session.
    *
    * @param arguments a <code>Object[]</code>.
    * @return a <code>OpProjectSession</code> instance representing the session with which the service method was called,
    *         or <code>null</code> if there isn't any.
    */
   protected OpProjectSession getProjectSesssion(Object[] arguments) {
      for (int i = 0; i < arguments.length; i++) {
         Object argument = arguments[i];
         if (argument instanceof OpProjectSession) {
            return (OpProjectSession) argument;
         }
      }
      return null;
   }
}
