/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.xml.XSchema;

/**
 * Schema class that handles backup files.
 */
public class OpBackupSchema extends XSchema {

   /**
    * Creates a new schema instance, registering the appropriate node handlers.
    */
   public OpBackupSchema() {
      registerNodeHandler(OpBackupManager.OPP_BACKUP, new OpBackupHandler());
      registerNodeHandler(OpBackupManager.PROTOTYPES, new OpPrototypesHandler());
      registerNodeHandler(OpBackupManager.PROTOTYPE, new OpPrototypeHandler());
      registerNodeHandler(OpBackupManager.FIELD, new OpFieldHandler());
      registerNodeHandler(OpBackupManager.RELATIONSHIP, new OpRelationshipHandler());
      registerNodeHandler(OpBackupManager.OBJECTS, new OpObjectsHandler());
      registerNodeHandler(OpBackupManager.O, new OpObjectHandler());
      registerNodeHandler(OpBackupManager.P, new OpPropertyHandler());
      registerNodeHandler(OpBackupManager.R, new OpReferenceHandler());
   }
}
