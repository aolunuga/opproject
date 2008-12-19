/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.util.HashMap;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

/**
 * Class that handles the parsing of <field> tags.
 */
public class OpFieldHandler implements XNodeHandler {

   /**
    * @see XNodeHandler#newNode(onepoint.xml.XContext, String, java.util.HashMap)
    */
   public Object newNode(XContext context, String name, HashMap attributes) {
      OpBackupMember backupMember = new OpBackupMember();
      // Fill out name and type; other parts of backup-member require the prototype
      backupMember.name = (String) attributes.get(OpBackupManager.NAME);
      backupMember.relationship = false;
      String typeString = (String) attributes.get(OpBackupManager.TYPE);
      Integer typeId = OpBackupTypeManager.getType(typeString);
      if (typeId == null) {
         throw new OpBackupException("Type id not found for the given string:" + typeString);
      }
      backupMember.typeId = typeId.intValue();
      backupMember.typeString = typeString;
      return backupMember;
   }

   /**
    * @see XNodeHandler#addChildNode(onepoint.xml.XContext, Object, String, Object)
    */
   public void addChildNode(XContext context, Object node, String child_name, Object child) {}

   /**
    * @see XNodeHandler#addNodeContent(onepoint.xml.XContext, Object, String)
    */
   public void addNodeContent(XContext context, Object node, String content) {}

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext, String, Object, Object)
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {}
}
