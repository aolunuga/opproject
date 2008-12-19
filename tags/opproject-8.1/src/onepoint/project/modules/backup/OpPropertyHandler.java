/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpObject;
import onepoint.project.util.Triple;
import onepoint.xml.XContext;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XNodeHandler;

/**
 * Class that handles property nodes.
 */
public class OpPropertyHandler implements XNodeHandler {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpPropertyHandler.class);

   /**
    * 
    */
   public OpPropertyHandler() {
   }
   /**
    * @see XNodeHandler#newNode(onepoint.xml.XContext,String,java.util.HashMap)
    */
   public Object newNode(XContext context, String name, HashMap attributes) {
      return new LinkedList<Triple<Boolean, String, Boolean>>();
   }

   /**
    * @see XNodeHandler#addChildNode(onepoint.xml.XContext,Object,String,Object)
    */
   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   /**
    * @see XNodeHandler#addNodeContent(onepoint.xml.XContext,Object,String)
    */
   public void addNodeContent(XContext context, Object node, String content) {
      LinkedList<Triple<Boolean, String, Boolean>> list = (LinkedList<Triple<Boolean, String, Boolean>>) node;
      if (Boolean.TRUE.equals(context.getVariable(XDocumentHandler.CDATA))) {
         list.add(new Triple<Boolean, String, Boolean>(Boolean.FALSE, content, Boolean.FALSE));
      }
      else {
         int st = 0;
         int len = content.length();

         while ((st < len) && (content.charAt(st) <= ' ')) {
            st++;
         }
         while ((st < len) && (content.charAt(len - 1) <= ' ')) {
            len--;
         }
         if ((st > 0) || (len < content.length())) {
            list.add(new Triple<Boolean, String, Boolean>(
                  (st > 0 ? Boolean.TRUE : Boolean.FALSE), content.substring(st, len), (len < content.length() ? Boolean.TRUE : Boolean.FALSE)));
         }
         else {
            list.add(new Triple<Boolean, String, Boolean>(Boolean.FALSE, content, Boolean.FALSE));
         }
      }
   }

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext,String,Object,Object)
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      // Members are always written in the same order as defined in the backup file header
      OpObject object = (OpObject) parent;
      OpBackupMember backupMember = ((OpRestoreContext) context).nextBackupMember();
      LinkedList<Triple<Boolean, String, Boolean>> list = (LinkedList<Triple<Boolean, String, Boolean>>) node;
      StringBuffer buffer = new StringBuffer();
      int len;
      int st = 0;
      Boolean trunc = Boolean.FALSE;
      if (list != null) {
         for (Triple<Boolean, String, Boolean> val : list) {
            if (trunc || val.getFirst()) {
               if (buffer.length() > 0) {
                  buffer.append(" ");
               }
            }
            buffer.append(val.getSecond());
            trunc = val.getThird();
         }
      }
      String valueString = buffer.toString();
      Object value = null;
      list.clear();
      if (!valueString.equals(OpBackupManager.NULL)) {
         String workingDirectory = (String) context.getVariable(OpRestoreContext.WORKING_DIRECTORY);
         value = OpBackupTypeManager.convertParsedValue(backupMember.typeId, valueString, workingDirectory);
      }
      // Call accessor method in order to set value
      invokeMethod(object, backupMember, value);
   }
   /**
    * @param object
    * @param backupMember
    * @param value
    * @pre
    * @post
    */
   private void invokeMethod(OpObject object, OpBackupMember backupMember,
         Object value) {
      Object[] arguments = new Object[]{value};
      try {
         //we should be somewhat graceful. It may happen, that members vanish...
         if (backupMember.accessor != null && !backupMember.wasDeleted) {
            if (backupMember.convertMethod != null) {
               try {
                  Method method = object.getClass().getDeclaredMethod(backupMember.convertMethod, new Class[0]);
                  value = method.invoke(object, new Object[0]);
                  arguments = new Object[]{value};
               }
               catch (SecurityException exc) {
                  logger.error("could not convert values", exc);
               }
               catch (NoSuchMethodException exc) {
                  // try it with arg:
                  try {
                     Method method = object.getClass().getDeclaredMethod(backupMember.convertMethod, backupMember.accessor.getParameterTypes());
                     value = method.invoke(object, arguments);
                     arguments = new Object[]{value};
                  }
                  catch (SecurityException exc2) {
                     logger.error("could not convert values", exc2);
                  }
                  catch (NoSuchMethodException exc2) {
                     logger.error("could not convert values", exc2);
                  }
               }
            }
            backupMember.accessor.invoke(object, arguments);
         }
         else {
        	 if (!backupMember.wasDeleted) {
        		 logger.error("skipped execution of accessor for " + backupMember.name + " as it was null.");
        	 }
         }
      }
      catch (InvocationTargetException e) {
         logger.error("Could not restore property value", e);
      }
      catch (IllegalAccessException e) {
         logger.error("Could not restore property value", e);
      }
      arguments = null;
   }
}
