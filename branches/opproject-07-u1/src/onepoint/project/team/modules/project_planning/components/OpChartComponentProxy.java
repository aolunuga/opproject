/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_planning.components;

import onepoint.express.XExpressProxy;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.script.interpreter.XInterpreterException;

/**
 * @author mihai.costin
 */
public class OpChartComponentProxy extends XExpressProxy {

   private static final XLog logger = XLogFactory.getLogger(OpChartComponentProxy.class);

   public final static Class CHART_CLASS = OpChartComponent.class;

   public final static String CHART = OpChartComponent.class.getName().intern();

   // Method names
   private final static String ZOOM = "zoom";

   // Class array
   private final static Class[] _classes = {CHART_CLASS};

   // Class name array
   private final static String[] _class_names = {CHART};

   public Class[] getClasses() {
      return _classes;
   }

   public String[] getClassNames() {
      return _class_names;
   }

   public Object newInstance(String class_name) throws XInterpreterException {
      throw new XInterpreterException("No class name " + class_name + " defined in this proxy");
   }

   public Object invokeMethod(Object object, String method_name, Object[] arguments) throws XInterpreterException {
      if (object instanceof OpChartComponent) {
         if (ZOOM.equals(method_name)) {
            if (arguments.length == 1) {
               ((OpChartComponent) object).zoom(((Integer) (arguments[0])).intValue());
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
            }
            return null;
         }
         else {
            return super.invokeMethod(object, method_name, arguments);
         }
      }
      else {
         return super.invokeMethod(object, method_name, arguments);
      }
   }

}
