/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.util.Triple;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

/**
 * Class that restores a repository backup.
 */
public class OpBackupLoader extends XLoader {

   // Does not use resource loading, because file name and location specified by user

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpReferenceHandler.class);

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
      // complete all missing relations
      Iterator<Triple<String, String, OpBackupMember>> iterator = context.relationDelayedIterator();
      int count = 0;
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      try {
         while (iterator.hasNext()) {
            Triple<String, String, OpBackupMember> node = iterator.next();
            try {
               OpObject source = broker.getObject(node.getFirst());
               OpObject destination = broker.getObject(node.getSecond());
               node.getThird().accessor.invoke(source, destination);
               count++;
               if (count % OpRestoreContext.MAX_INSERTS_PER_TRANSACTION == 0) {
                  t.commit();
                  t = broker.newTransaction();
               }
            }
            catch (IllegalAccessException e) {
               logger.error("Cannot restore object relationship", e);
            }
            catch (InvocationTargetException e) {
               logger.error("Cannot restore object relationship", e);
            }
         }
         t.commit();
      }
      finally {
         if (!t.wasCommited()) {
            t.rollback();
         }
         broker.close();
      }
   }
}
