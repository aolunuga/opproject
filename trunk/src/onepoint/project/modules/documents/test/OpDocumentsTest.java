/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.documents.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XSizeInputStream;
import onepoint.util.XEnvironmentManager;

import java.io.*;
import java.util.List;

/**
 * This class test document classes.
 *
 * @author lucian.furtos
 */
public class OpDocumentsTest extends OpBaseOpenTestCase {

   private static final String EXPECTED_FILE = XEnvironmentManager.TMP_DIR + "expectedfile.tmp";
   private static final String ACTUAL_FILE = XEnvironmentManager.TMP_DIR + "actualfile.tmp";
   private static final long FILE_SIZE = 1024L * 1024L; // 1 MB

   private File expectedFile;
   private File actualFile;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();
      clean();

      generateTestFile();

      expectedFile = new File(EXPECTED_FILE);
      assertTrue(expectedFile.exists());
      assertEquals(FILE_SIZE, expectedFile.length());

      actualFile = new File(ACTUAL_FILE);
      assertFalse(actualFile.exists());
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      clean();
      super.tearDown();
   }

   /**
    * Tests the creation of OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void testCreateContent()
        throws Exception {

      String mimeType = OpContentManager.getFileMimeType(expectedFile.getName());

      // save content
      createContent(expectedFile, mimeType);

      // load content
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from " + OpContent.CONTENT);
      List list = broker.list(query);

      assertNotNull(list);
      assertEquals(1, list.size());
      OpContent actual = (OpContent) list.get(0);
      assertEquals(mimeType, actual.getMediaType());
      assertEquals(FILE_SIZE, actual.getSize());
      assertEquals(1, actual.getRefCount());
      XSizeInputStream actualStream = actual.getStream();
      assertNotNull(actualStream);
      assertEquals(FILE_SIZE, actualStream.getSize());
      assertNotNull(actualStream.getInputStream());

      FileOutputStream out = new FileOutputStream(actualFile);
      OpTestDataFactory.copy(actualStream, out);
      out.close();
      assertTrue(actualFile.exists());
      assertEquals(FILE_SIZE, actualFile.length());
      broker.close();
   }

   /**
    * Tests the update of OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateContent()
        throws Exception {

      String mimeType = OpContentManager.getFileMimeType(expectedFile.getName());

      // save content
      createContent(expectedFile, mimeType);

      // load content
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("from " + OpContent.CONTENT);
      List list = broker.list(query);
      OpContent content = (OpContent) list.get(0);
      String id = content.locator();
      OpTransaction t = broker.newTransaction();
      OpContentManager.updateContent(content, broker, true, (OpAttachment) null);
      t.commit();
      broker.close();

      broker = session.newBroker();
      OpContent actual = (OpContent) broker.getObject(id);

      assertEquals(mimeType, actual.getMediaType());
      assertEquals(FILE_SIZE, actual.getSize());
      assertEquals(2, actual.getRefCount());
      XSizeInputStream actualStream = actual.getStream();
      assertNotNull(actualStream);
      assertEquals(FILE_SIZE, actualStream.getSize());
      assertNotNull(actualStream.getInputStream());

      FileOutputStream out = new FileOutputStream(actualFile);
      OpTestDataFactory.copy(actualStream, out);
      out.close();
      assertTrue(actualFile.exists());
      assertEquals(FILE_SIZE, actualFile.length());
      broker.close();
   }

   /**
    * Tests the deletion of OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void testDeleteContent()
        throws Exception {

      String mimeType = OpContentManager.getFileMimeType(expectedFile.getName());

      // save content
      createContent(expectedFile, mimeType);

      // load content
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("from " + OpContent.CONTENT);
      List list = broker.list(query);
      OpContent content = (OpContent) list.get(0);
      OpTransaction t = broker.newTransaction();
      OpContentManager.updateContent(content, broker, false, (OpAttachment) null);
      t.commit();
      broker.close();

      broker = session.newBroker();
      query = broker.newQuery("from " + OpContent.CONTENT);
      list = broker.list(query);

      assertNotNull(list);
      assertTrue(list.isEmpty());

      broker.close();
   }

   // ******** Helper Methods *********

   /**
    * Create and persist an OpConttent
    *
    * @param file     the file to save in the database
    * @param mimeType the type of the file
    * @throws FileNotFoundException if the file cannot be found when datastream is read.
    */
   private void createContent(File file, String mimeType)
        throws FileNotFoundException {
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      XSizeInputStream stream = new XSizeInputStream(new FileInputStream(file), file.length());
      OpContent content = OpContentManager.newContent(stream, mimeType);
      broker.makePersistent(content);

      t.commit();
      broker.close();
   }

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {
      new File(EXPECTED_FILE).delete();
      new File(ACTUAL_FILE).delete();


      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();
      deleteAllObjects(broker, OpContent.CONTENT);
      transaction.commit();
      broker.close();

   }

   /**
    * Generates a binary test file
    *
    * @throws IOException
    */
   private static void generateTestFile()
        throws IOException {
      FileOutputStream out = new FileOutputStream(EXPECTED_FILE);
      for (long i = 0; i < FILE_SIZE; i++) {
         out.write((int) i % 256);
      }
      out.flush();
      out.close();
   }
}
