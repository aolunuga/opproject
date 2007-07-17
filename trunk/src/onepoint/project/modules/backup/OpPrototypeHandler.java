/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpTypeManager;
import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;
import java.util.List;

/**
 * Class that handles the parsing of <prototype> entities.
 */
public class OpPrototypeHandler implements XNodeHandler {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getClientLogger(OpPrototypeHandler.class);

   /**
    * @see XNodeHandler#newNode(onepoint.xml.XContext, String, java.util.HashMap)
    */
   public Object newNode(XContext context, String name, HashMap attributes) {
      // Add empty backup member list with prototype name to restore context
      String prototypeName = (String) attributes.get(OpBackupManager.NAME);
      ((OpRestoreContext) context).registerPrototype(prototypeName);
      return prototypeName;
   }

   /**
    * @see XNodeHandler#addChildNode(onepoint.xml.XContext, Object, String, Object)
    */
   public void addChildNode(XContext context, Object node, String child_name, Object child) {
      // Add backup members generated from field and relationship handlers
      List backupMembers = ((OpRestoreContext) context).getBackupMembers((String) node);
      backupMembers.add((OpBackupMember) child);
   }

   /**
    * @see XNodeHandler#addNodeContent(onepoint.xml.XContext, Object, String)
    */
   public void addNodeContent(XContext context, Object node, String content) {
   }

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext, String, Object, Object)
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      // Iterate backup-members and set accessor methods
      String prototypeName = (String) node;
      List backupMembers = ((OpRestoreContext) context).getBackupMembers(prototypeName);
      OpPrototype prototype = OpTypeManager.getPrototype(prototypeName);
      if (prototype == null) {
    	  logger.error("No prototype named " + prototypeName + ". Will skip this prototype.");
      }
      Class accesorArgument = null;
      for (int i = 0; i < backupMembers.size(); i++) {
         OpBackupMember backupMember = (OpBackupMember) backupMembers.get(i);
         if (backupMember.relationship) {
            OpPrototype targetPrototype = OpTypeManager.getPrototypeByID(backupMember.typeId);
            if (targetPrototype != null) {
               accesorArgument = targetPrototype.getInstanceClass();
            }
            else {
               throw new OpBackupException("Unsupported prototype ID " + backupMember.typeId + " for " + prototypeName
                    + "." + backupMember.name);
            }
         }
         else {
            accesorArgument = OpBackupTypeManager.getJavaType(backupMember.typeId);
            if (accesorArgument == null) {
               throw new OpBackupException("Unsupported type ID " + backupMember.typeId + " for " + prototypeName
                    + "." + backupMember.name);
            }
         }
         // Cache accessor method
         // (Note that we assume that persistent member names start with an upper-case letter)
         try {
        	//we should be somewhat graceful. It may happen, that entities vanish...
        	if(prototype != null)
        	   backupMember.accessor = prototype.getInstanceClass().getMethod("set" + backupMember.name, new Class[]{accesorArgument});
        	else
        	   logger.error("cannot handle '" + prototypeName +"' as the corresponding prototype is missing in this version");
         }
         catch (NoSuchMethodException e) {
            //if the accesorArgument is a primitive...
            accesorArgument = OpBackupTypeManager.getJavaPrimitiveType(backupMember.typeId);
            if (accesorArgument != null) {
               try {
                  backupMember.accessor = prototype.getInstanceClass().getMethod("set" + backupMember.name, new Class[]{accesorArgument});
               }
               catch (NoSuchMethodException e1) {
                  logger.error("No accessor method for " + prototype.getName() + "." + backupMember.name);
               }
            }
            else {
               logger.error("No accessor method for " + prototype.getName() + "." + backupMember.name);
            }
            // Note: Fields which do not have an accessors are not written
         }
         //TODO: that is very rude. But at least it keeps us trying. After we changed the backup-logic to be more clever,
         //we should get rid of this "catch everything"...
         catch (Exception e) {
        	 logger.error("unexpected Exception occured:" + e.getMessage());
         }
      }
   }
}
