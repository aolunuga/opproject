package onepoint.project.modules.work.components;

import onepoint.express.XComponent;
import onepoint.express.XExpressProxy;
import onepoint.project.modules.work.validators.OpWorkValidator;
import onepoint.script.interpreter.XInterpreterException;

/**
 *
 * @author mihai.costin
 */
public class OpWorkProxy extends XExpressProxy {

   private final static String FILTER_ACTIVITY_SET = "filterActivities";
   private final static String FILTER_RESOURCE_SET = "filterResources";

   public Object invokeMethod(Object object, String method_name, Object[] arguments)
        throws XInterpreterException {

      if (object instanceof XComponent) {
         if (FILTER_ACTIVITY_SET.equals(method_name)) {
            if (arguments.length == 1) {
               OpWorkValidator validator = (OpWorkValidator) ((XComponent)object).validator();
               XComponent row = (XComponent)arguments[0];
               if (row != null) {
                  validator.filterActivities(row, validator.getResource(row));
               }
               else {
                  validator.filterActivities(null, null);
               }
               return null;
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
            }
         }
         else if (FILTER_RESOURCE_SET.equals(method_name)) {
            if (arguments.length == 1) {
               OpWorkValidator validator = (OpWorkValidator) ((XComponent)object).validator();
               XComponent row = (XComponent)arguments[0];
               if (row != null) {
                  validator.filterResources(row, validator.getActivity(row));
               }
               else {
                  validator.filterResources(null, null);
               }
               return null;
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
            }
         }
      }

      return super.invokeMethod(object, method_name, arguments);
   }

}
