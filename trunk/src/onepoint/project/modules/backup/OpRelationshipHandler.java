/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.util.HashMap;

import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpTypeManager;
import onepoint.xml.XContext;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XNodeHandler;

/**
 * Class that handles <relationship> elements.
 */
public class OpRelationshipHandler implements XNodeHandler {

   /**
    * @see XNodeHandler#newNode(onepoint.xml.XContext, String, java.util.HashMap)
    */
   public Object newNode(XContext context, String name, HashMap attributes) {
      OpBackupMember backupMember = new OpBackupMember();
      backupMember.relationship = true;
      // Fill out name and type; other parts of backup-member require the prototype
      backupMember.name = (String) attributes.get(OpBackupManager.NAME);
      String typeString = (String) attributes.get(OpBackupManager.TYPE);
      // Type string maps to a prototype name
      OpPrototype targetPrototype = OpTypeManager.getPrototype(typeString);
      OpRestoreContext restoreContext = (OpRestoreContext)context;
      XDocumentHandler handler = (restoreContext).getDocumentHandler();
      String nodeName = (String) handler.getContextNode();
      if (targetPrototype == null) {
         targetPrototype = restoreContext.getMappedRelation(nodeName, backupMember.name);
         if (targetPrototype == null) {
            throw new OpBackupException("Type with name:" + typeString + "  is not registered by the type manager");
         }
      }

      backupMember.typeId = targetPrototype.getID();
      backupMember.typeString = typeString;
      return backupMember;
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
   }

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext, String, Object, Object)  
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {
   }

}
