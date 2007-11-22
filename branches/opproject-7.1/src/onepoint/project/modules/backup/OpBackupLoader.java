/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.project.OpProjectSession;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;

import java.io.InputStream;

/**
 * Class that restores a repository backup.
 */
public class OpBackupLoader extends XLoader {

   // Does not use resource loading, because file name and location specified by user

   /**
    * The schema that is used by the backup loader.
    */
   public final static XSchema BACKUP_SCHEMA = new OpBackupSchema();

   /**
    * Creates a new backup loader.
    */
   public OpBackupLoader() {
      super(new XDocumentHandler(BACKUP_SCHEMA));
      setUseResourceLoader(false);
   }

   /**
    * Loads a backup from the given input stream and using the given broker.
    * @param session a <code>OpProjectSession</code> the server session.
    * @param input_stream an <code>InputStream</code> from which the contents of the back-up are read.
    * @param workingDirectory a <code>String</code> representing the working directory of the file being restored.
    */
   public void loadBackup(OpProjectSession session, InputStream input_stream, String workingDirectory) {
      OpRestoreContext context = new OpRestoreContext(session);
      context.setVariable(OpRestoreContext.WORKING_DIRECTORY, workingDirectory);
      loadObject(input_stream, context);
   }
}
