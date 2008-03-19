/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.user;

import net.sf.cglib.proxy.MethodProxy;
import onepoint.project.OpProjectSession;
import onepoint.project.OpServiceInterceptor;
import onepoint.service.server.XMethod;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceException;

import java.lang.reflect.Method;

/**
 * Service interceptor for OpUserService.
 *
 * @author florin.haizea
 */
public class OpPermissionCheckServiceInterceptor extends OpServiceInterceptor {

   /**
    * the map containing all error types.
    */
   public static final OpUserErrorMap ERROR_MAP = new OpUserErrorMap();

   /**
    * @see onepoint.service.server.XServiceInterceptor#intercept(Object,java.lang.reflect.Method,Object[],net.sf.cglib.proxy.MethodProxy)
    */
   @Override
   public Object intercept(Object object, Method method, Object[] objects, MethodProxy methodProxy)
        throws Throwable {
      this.checkAdministratorPermissions(object, method, objects);
      return super.intercept(object, method, objects, methodProxy);
   }

   /**
    * Checks that the session user which invokes a service method has administrator permissions.
    *
    * @param object  a <code>XService</code> instance.
    * @param method  a <code>Method</code> instance the service method which will be invoked.
    * @param objects a <code>Object[]</code> the arguments of the method.
    * @throws XServiceException if the session user is not an administrator
    */
   private void checkAdministratorPermissions(Object object, Method method, Object[] objects) {
      XService service = (XService) object;
      XMethod xMethod = (XMethod) service.getMethods().get(method.getName());

      if (!(object instanceof XService)) {
         throw new IllegalArgumentException("Service interceptor created for an invalid object type. Expected XService but was " + object.getClass().getName());
      }
      OpProjectSession session = this.getProjectSesssion(objects);
      if (xMethod.isAdminCheck() && !session.userIsAdministrator()) {
         throw new XServiceException(session.newError(ERROR_MAP, OpUserError.INSUFFICIENT_PRIVILEGES));
      }
   }
}
