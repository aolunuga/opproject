/**
 * Copyright(c) OnePoint Software GmbH 2008. All Rights Reserved.
 */
package onepoint.project.modules.documents;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.sf.cglib.proxy.MethodProxy;
import onepoint.express.XComponent;
import onepoint.express.util.XConstants;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.OpServiceInterceptor;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.service.server.XMethod;
import onepoint.service.server.XService;

/**
 * Service interceptor for all services that handle OpContent objects.
 *
 * @author mihai.costin
 */
public class OpContentCheckServiceInterceptor extends OpServiceInterceptor {
   
   private static final XLog logger = XLogFactory.getLogger(OpContentCheckServiceInterceptor.class);


   /**
    * @see onepoint.service.server.XServiceInterceptor#intercept(Object,java.lang.reflect.Method,Object[],net.sf.cglib.proxy.MethodProxy)
    */
   @Override
   public Object intercept(Object object, Method method, Object[] objects, MethodProxy methodProxy)
        throws Throwable {
      Object result = null;
      XComponent attachmentSet = null;
      XMessage request = getRequest(objects);
      XService service = (XService) object;
      XMethod xMethod = (XMethod) service.getMethods().get(method.getName());

      try {
         result = methodProxy.invokeSuper(object, objects);
      }
      catch (Exception e) {
         //in the case of an exception on an operation that is "content aware", delete all previously managed contents
         if (xMethod.isContentCheck() && request != null) {
            attachmentSet = request.extractAttachmentSetFromArguments(OpContent.CONTENT);
            if (attachmentSet != null) {
               List<String> contentLocators = attachmentSet.extractLocators(OpProjectConstants.CONTENT_LOCATOR_REG_EX);
               List<Long> contentIds = new ArrayList<Long>();
               for (String locator : contentLocators) {
                  contentIds.add(OpLocator.parseLocator(locator).getID());
               }

               OpProjectSession session = this.getProjectSesssion(objects);
               OpBroker broker = session.newBroker();
               OpTransaction transaction = broker.newTransaction();
               OpContentService.getService().deleteZeroRefContentsWithIds(broker, contentIds);
               transaction.commit();
               broker.close();
            }
         } 
         throw e;
      }
      finally {
         //in the case of an error
         if (result != null && result instanceof XMessage && ((XMessage) result).getError() != null && xMethod.isContentCheck()) {
            //extract the attachment set from the request and set it on the response
            if (attachmentSet == null) {
               attachmentSet = request.extractAttachmentSetFromArguments(OpContent.CONTENT);
            }
            ((XMessage) result).setArgument(XConstants.ATTACHMENT_SET, attachmentSet);
         }
      }
      return result;
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
