/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map.Entry;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpDurationFormater;
import onepoint.project.util.Triple;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;

/**
 * Class that restores a repository backup.
 */
public class OpBackupLoader extends XLoader {

   // Does not use resource loading, because file name and location specified by user

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpReferenceHandler.class);

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
      OpBroker broker = session.newBroker();
      try {
         long start = System.currentTimeMillis();
         OpRestoreContext context = new OpRestoreContext(session, broker, getDocumentHandler());
         context.setVariable(OpRestoreContext.WORKING_DIRECTORY, workingDirectory);
         context.init();
         loadObject(input_stream, context);
         // complete all missing relations
         context.finish();
         logger.info("Loading Backup file ("+context.getTotalInsertCount()+" entries) to Database lasted: "+new OpDurationFormater(System.currentTimeMillis()-start, true).toString());

//         context.commitRestoredObjects();
         Iterator<Triple<String, String, OpBackupMember>> iterator = context.relationDelayedIterator();
         int count = 0;
         OpTransaction t = broker.newTransaction();
         try {
            while (iterator.hasNext()) {
               Triple<String, String, OpBackupMember> node = iterator.next();
               try {
                  OpObjectIfc source = broker.getObject(node.getFirst());
                  OpObjectIfc destination = broker.getObject(node.getSecond());
                  if (destination == null) {
                	  logger.error("could not restore relationship from: "+node.getFirst()+" named: "+node.getThird().name);
                  }
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
            logger.info("Loading Backup file ("+context.getTotalInsertCount()+" entries) to and restoring Relations Database lasted: "+new OpDurationFormater(System.currentTimeMillis()-start, true).toString());

            Iterator<Entry<Triple<OpLocator, OpLocator, OpBackupMember>, OpObjectIfc>> iter = context.relationDelayedPerTransactionIterator();
            if (iter.hasNext()) {
            	StringBuffer buffer = new StringBuffer();
            	buffer.append("Could not restore the following relations:");
        		buffer.append('\n');
            	while (iter.hasNext()) {
            		Entry<Triple<OpLocator, OpLocator, OpBackupMember>, OpObjectIfc> entry = iter.next();
            		Triple<OpLocator, OpLocator, OpBackupMember> elem = entry.getKey();
            		buffer.append(elem.getSecond());
            		buffer.append('\n');
            	}
                logger.error(buffer);
            }
            else {
            	logger.info("All Relations restored!");
            }
         }
         catch (IllegalArgumentException e) {
            logger.error("Panic! database corrupted...", e);
         }
         finally {
            if (!t.wasCommited()) {
               t.rollback();
            }
         }
      }
      finally {
         broker.closeAndEvict();
      }
   }
}
