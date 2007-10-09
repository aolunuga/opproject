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
 * Class that handles reference nodes.
 */
public class OpReferenceHandler implements XNodeHandler {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpReferenceHandler.class);

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
      String valueString = ((StringBuffer) node).toString().trim();
      OpRestoreContext restoreContext = (OpRestoreContext) context;

      if (!valueString.equals(OpBackupManager.NULL)) {
         Long objectId = new Long(valueString);
         OpObject relationshipEnd = restoreContext.getRelationshipOwner(objectId);
         Object value = null;
         if (relationshipEnd == null) {
            logger.error("Cannot restore relationship towards object with id:" + objectId.toString());
         }
         else {
            if (backupMember.relationship) {
               value = relationshipEnd;
            }
            else {
               //there was a type change
               String workingDirectory = (String) context.getVariable(OpRestoreContext.WORKING_DIRECTORY);
               //<FIXME author="Horia Chiorean" description="For relationships changed to attributes, this might not work correctly">
               value = OpBackupTypeManager.convertParsedValue(backupMember.typeId, String.valueOf(relationshipEnd.getID()), workingDirectory);
               //<FIXME>
            }
         }
         try {
            if (backupMember.accessor != null) {
              backupMember.accessor.invoke(object, value);
            }
         }
         catch (IllegalAccessException e) {
            logger.error("Cannot restore object relationship", e);
         }
         catch (InvocationTargetException e) {
            logger.error("Cannot restore object relationship", e);
         }
      }
   }
}
