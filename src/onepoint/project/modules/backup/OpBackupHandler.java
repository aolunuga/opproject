/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

/**
 * Class that handles a backup node.
 */
public class OpBackupHandler implements XNodeHandler {

   /**
    *   Constant used when the schema version is missing when reading the backup-file (4 - represents ver 07).
    */
   private String MISSING_SCHEMA_VERSION ="4";

   /**
    * @see XNodeHandler#newNode(onepoint.xml.XContext,String,java.util.HashMap)
    */
   public Object newNode(XContext context, String name, HashMap attributes) {
      String versionString = (String) attributes.get(OpBackupManager.VERSION);
      if (versionString == null || Integer.valueOf(versionString).intValue() != OpBackupManager.CURRENT_VERSION_NUMBER) {
         throw new OpBackupException("The version number " + versionString + " found in the backup file does not match the expected one " + OpBackupManager.CURRENT_VERSION_NUMBER);
      }
      String schemaVersion = (String) attributes.get(OpBackupManager.SCHEMA_VERSION);
      //if no schema version present, set value to 0 to force update
      schemaVersion = (schemaVersion != null) ? schemaVersion : MISSING_SCHEMA_VERSION;
      ((OpRestoreContext) context).writeSchemaVersion(schemaVersion);
      return null;
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
   }

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext,String,Object,Object)
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {
   }
}
