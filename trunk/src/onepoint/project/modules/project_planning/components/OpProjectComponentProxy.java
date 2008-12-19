
/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.components;

import onepoint.express.XExpressProxy;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.util.OpHashProvider;
import onepoint.script.interpreter.XInterpreterException;

/**
 * @author gmesaric
 */
public class OpProjectComponentProxy extends XExpressProxy {
   public OpProjectComponentProxy() {}

   // Class names
   private final static String PROJECT_COMPONENT = OpProjectComponent.class.getName().intern();
   private final static String GANTT_VALIDATOR = OpGanttValidator.class.getName().intern();
   private final static String HASH_PROVIDER = OpHashProvider.class.getName().intern();

   private final static Class GANTT_VALIDATOR_CLASS = OpGanttValidator.class;
   private final static Class PROJECT_COMPONENT_CLASS = OpProjectComponent.class;
   private final static Class HASH_PROVIDER_CLASS = OpHashProvider.class;


   // Method names
   private final static String SET_TIME_UNIT = "setTimeUnit".intern();
   private final static String PERCENT_ASSIGNED = "percentAssigned".intern();
   private final static String GET_RESOURCE_NAME = "getResourceName".intern();
   private final static String CHANGE_TOOL = "changeTool".intern();
   private final static String RESET_CALENDAR = "resetCalendar".intern();
   private final static String SET_VIEW_TYPE = "setViewType".intern();

   // Method names: SHA1
   private final static String CALCULATE_HASH = "calculateHash";

   // Class name array
   private final static String[] _class_names = {PROJECT_COMPONENT, GANTT_VALIDATOR, HASH_PROVIDER};

   private final static Class[] _classes = {PROJECT_COMPONENT_CLASS, GANTT_VALIDATOR_CLASS, HASH_PROVIDER_CLASS};

   public Class[] getClasses() {
      return _classes;
   }

   public String[] getClassNames() {
      return _class_names;
   }

   public Object newInstance(String class_name) throws XInterpreterException {
      throw new XInterpreterException("No class name " + class_name + " defined in this proxy");
   }

   // TODO: We need static getters in order to access constants
   // ==> Provide static access to XDuration.DAYS, WEEKS and MONTHS in XDefaultProxy

   public Object invokeMethod(String class_name, String method_name, Object[] arguments) throws XInterpreterException {

      if (class_name == GANTT_VALIDATOR) {
         if (method_name == PERCENT_ASSIGNED) {
            if (arguments.length == 1) {
               return new Double(OpGanttValidator.percentageAssigned((String) arguments[0]));
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + class_name + "." + method_name);
            }
         }
         else if (method_name == GET_RESOURCE_NAME) {
            if (arguments.length == 2) {
               return OpGanttValidator.getResourceName((String) arguments[0], (String) arguments[1]);
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + class_name + "." + method_name);
            }
         }
         else {
            // ERROR
            throw new XInterpreterException("Class OpGanttValidator does not define a method named " + method_name);
         }
      }
      else if (class_name == HASH_PROVIDER) {
         // Methods of class File
         if (method_name.equals(CALCULATE_HASH)) {
            if (arguments.length == 2) {
               return new OpHashProvider().calculateHash((String) arguments[0], (String) arguments[1]);
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + class_name + "." + method_name);
            }
         }
         else {
            // ERROR
            throw new XInterpreterException("Class Console does not define a method named " + method_name);
         }
      }
      else {
         return super.invokeMethod(class_name, method_name, arguments);    //To change body of overridden methods use File | Settings | File Templates.
      }
   }

   public Object invokeMethod(Object object, String method_name, Object[] arguments) throws XInterpreterException {
      if (object instanceof OpProjectComponent) {
         // TODO: Check if script-interpreter can handle byte-values
         if (method_name == SET_TIME_UNIT) {
            if (arguments.length == 1) {
               ((OpProjectComponent) object).setTimeUnit(((Integer) (arguments[0])).byteValue());
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
            }
            return null;
         }
         else if (method_name == CHANGE_TOOL) {
            if (arguments.length == 1) {
               ((OpProjectComponent) object).changeTool((String) (arguments[0]));
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
            }
            return null;
         }
         else if (method_name == RESET_CALENDAR) {
            if (arguments.length == 0) {
               ((OpProjectComponent) object).resetCalendar();
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
            }
            return null;
         }
         else if (method_name == SET_VIEW_TYPE) {
             if (arguments.length == 1) {
                 ((OpProjectComponent) object).setViewType(((Integer) (arguments[0])).intValue());
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
