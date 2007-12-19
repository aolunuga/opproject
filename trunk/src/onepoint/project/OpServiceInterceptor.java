/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project;

import net.sf.cglib.proxy.MethodProxy;
import onepoint.express.XComponent;
import onepoint.express.util.XConstants;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.modules.documents.OpContent;
import onepoint.service.XMessage;
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
   @Override
   public void beforeAdvice(XService service, Method method, Object[] arguments) {
      logger.debug("Applying before advice...");
      OpProjectSession session = this.getProjectSesssion(arguments);
      if (session != null) {
         exceptBrokers = new ArrayList<OpBroker>(session.getBrokerList());
      }
   }

   /**
    * @see onepoint.service.server.XServiceInterceptor#afterAdvice(onepoint.service.server.XService,java.lang.reflect.Method,Object[])
    */
   @Override
   public void afterAdvice(XService service, Method method, Object[] arguments) {
      logger.debug("Applying after advice...");
      OpProjectSession session = this.getProjectSesssion(arguments);
      if (session != null) {
         session.cleanupSession(exceptBrokers, false);
         exceptBrokers.clear();
      }
   }

   /**
    * @see onepoint.service.server.XServiceInterceptor#intercept(Object,java.lang.reflect.Method,Object[],net.sf.cglib.proxy.MethodProxy)
    */
   @Override
   public Object intercept(Object object, Method method, Object[] objects, MethodProxy methodProxy)
        throws Throwable {
      beforeAdvice((XService) object, method, objects);
      Object result = methodProxy.invokeSuper(object, objects);
      afterAdvice((XService) object, method, objects);
      XMessage request = getRequest(objects);
      if (result != null && ((XMessage) result).getError() != null && request.getArgument(XConstants.CLEANUP_CONTENTS) != null
           && (Boolean) request.getArgument(XConstants.CLEANUP_CONTENTS)) {
         //extract the attachment set from the request and set it on the response
         XComponent attachmentSet = request.extractAttachmentSetFromArguments(OpContent.CONTENT);
         if (attachmentSet != null) {
            ((XMessage) result).setArgument(XConstants.ATTACHMENT_SET, attachmentSet);
         }
         else {
            throw new IllegalArgumentException("An AttachmentSet could not be sent back to the client");
         }
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

   protected XMessage getRequest(Object[] arguments) {
      for (int i = 0; i < arguments.length; i++) {
         Object argument = arguments[i];
         if (argument instanceof XMessage) {
            return (XMessage) argument;
         }
      }
      return null;
   }
}
