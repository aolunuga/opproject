/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpType;
import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
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
    * @see XNodeHandler#newNode(onepoint.xml.XContext, String, java.util.HashMap)
    */
   public Object newNode(XContext context, String name, HashMap attributes) {
      return new StringBuffer();
   }

   /**
    * @see XNodeHandler#addChildNode(onepoint.xml.XContext, Object, String, Object)
    */
   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   /**
    * @see XNodeHandler#addNodeContent(onepoint.xml.XContext, Object, String)
    */
   public void addNodeContent(XContext context, Object node, String content) {
      ((StringBuffer) node).append(content);
   }

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext, String, Object, Object)
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      // Members are always written in the same order as defined in the backup file header
      OpObject object = (OpObject) parent;
      OpBackupMember backupMember = ((OpRestoreContext) context).nextBackupMember();
      String valueString = ((StringBuffer) node).toString().trim();
      Object value = null;
      if (!valueString.equals(OpBackupManager.NULL)) {
         switch (backupMember.typeId) {
            case OpType.BOOLEAN:
               value = Boolean.valueOf(valueString);
               break;
            case OpType.INTEGER:
               value = Integer.valueOf(valueString);
               break;
            case OpType.LONG:
               value = Long.valueOf(valueString);
               break;
            case OpType.STRING:
               value = valueString;
               break;
            case OpType.TEXT:
               value = valueString;
               break;
            case OpType.DATE:
               try {
                  value = new Date(OpBackupManager.DATE_FORMAT.parse(valueString).getTime());
               }
               catch (ParseException e) {
                  logger.error("Could not parse date:" + valueString);
               }
               break;
            case OpType.CONTENT:
               if (valueString != null) {
                  String workingDirectory = (String) context.getVariable(OpRestoreContext.WORKING_DIRECTORY);
                  String contentPath = workingDirectory + valueString;
                  value = OpBackupManager.readBinaryFile(contentPath);
               }
               break;
            case OpType.BYTE:
               value = Byte.valueOf(valueString);
               break;
            case OpType.DOUBLE:
               value = Double.valueOf(valueString);
               break;
            case OpType.TIMESTAMP:
               try {
                  value = new Timestamp(OpBackupManager.TIMESTAMP_FORMAT.parse(valueString).getTime());
               }
               catch (ParseException e) {
                  logger.error("Could not parse timestamp:" + valueString);
                }
               break;
            default:
               logger.error("Unsupported type ID " + backupMember.typeId);
         }
      }
      // Call accessor method in order to set value
      Object[] arguments = new Object[] {value};
      try {
         backupMember.accessor.invoke(object, arguments);
      }
      catch (InvocationTargetException e) {
         logger.error("Could not restore property value" , e);
      }
      catch (IllegalAccessException e) {
         logger.error("Could not restore property value" , e);
      }
   }
}
