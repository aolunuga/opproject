/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpObject;
import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * Class that handles property nodes.
 */
public class OpPropertyHandler implements XNodeHandler {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpPropertyHandler.class);

   /**
    * @see XNodeHandler#newNode(onepoint.xml.XContext,String,java.util.HashMap)
    */
   public Object newNode(XContext context, String name, HashMap attributes) {
      return new StringBuffer();
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
      ((StringBuffer) node).append(content);
   }

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext,String,Object,Object)
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      // Members are always written in the same order as defined in the backup file header
      OpObject object = (OpObject) parent;
      OpBackupMember backupMember = ((OpRestoreContext) context).nextBackupMember();
      String valueString = node.toString().trim();
      Object value = null;
      if (!valueString.equals(OpBackupManager.NULL)) {
         String workingDirectory = (String) context.getVariable(OpRestoreContext.WORKING_DIRECTORY);
         value = OpBackupTypeManager.convertParsedValue(backupMember.typeId, valueString, workingDirectory);
      }
      // Call accessor method in order to set value
      Object[] arguments = new Object[]{value};
      try {
         //we should be somewhat graceful. It may happen, that members vanish...
         if (backupMember.accessor != null) {
            backupMember.accessor.invoke(object, arguments);
         }
         else {
            logger.error("skipped execution of accessor for " + backupMember.name + " as it was null.");
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
