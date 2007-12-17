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
import onepoint.service.XSizeInputStream;
import onepoint.util.XIOHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * This class test document classes.
 *
 * @author lucian.furtos
 */
//<FIXME author="Mihai Costin" description="Test methods that cause out of memory have been removed. Should be added back when the issue will be addressed">
public class OpDocumentsTest extends OpBaseOpenTestCase {
//</FIXME>

   private static final long ONE_MB = 1024L * 1024L; // 1 MB
   private static final String BINARY_MIME_TYPE = "binary/octet-stream";

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      //set the stream member of the OpContent class to lazy=false so that it is loaded
      OpContent.setStreamLazy(false);
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      //reset the lazy loading of the stream member from OpContent
      OpContent.setStreamLazy(true);

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

      String mimeType = BINARY_MIME_TYPE;

      // save content
      createContent(generateInputStream(ONE_MB), mimeType, ONE_MB);

      // load content
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from " + OpContent.CONTENT);
      List list = broker.list(query);

      assertNotNull(list);
      assertEquals(1, list.size());
      OpContent actual = (OpContent) list.get(0);
      assertEquals(mimeType, actual.getMediaType());
      assertEquals(ONE_MB, actual.getSize());
      assertEquals(1, actual.getRefCount());
      XSizeInputStream actualStream = actual.getStream();
      assertNotNull(actualStream);
      assertEquals(ONE_MB, actualStream.getSize());
      assertNotNull(actualStream.getInputStream());

      long count = XIOHelper.copy(actualStream, generateFakeOutputStream());
      assertEquals(ONE_MB, count);
      broker.close();
   }

   /**
    * Tests the creation of OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void testCreateLazyLoadedContent()
        throws Exception {
      //set the stream member of the OpContent class to lazy=true so that it is NOT loaded
      OpContent.setStreamLazy(true);

      String mimeType = BINARY_MIME_TYPE;

      // save content
      createContent(generateInputStream(ONE_MB), mimeType, ONE_MB);

      // load content
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from " + OpContent.CONTENT);
      List list = broker.list(query);

      assertNotNull(list);
      assertEquals(1, list.size());
      OpContent actual = (OpContent) list.get(0);
      assertEquals(mimeType, actual.getMediaType());
      assertEquals(ONE_MB, actual.getSize());
      assertEquals(1, actual.getRefCount());
      XSizeInputStream actualStream = actual.getStream();
      assertNotNull(actualStream);
      assertEquals(ONE_MB, actualStream.getSize());
      assertNotNull(actualStream.getInputStream());

      long count = XIOHelper.copy(actualStream, generateFakeOutputStream());
      //the stream is empty
      assertEquals(0, count);
      broker.close();

      //set the stream member of the OpContent class to lazy=false so that it is loaded
      OpContent.setStreamLazy(false);
   }

   /**
    * Tests the update of OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateContent()
        throws Exception {

      String mimeType = BINARY_MIME_TYPE;

      // save content
      createContent(generateInputStream(ONE_MB), mimeType, ONE_MB);

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
      assertEquals(ONE_MB, actual.getSize());
      assertEquals(2, actual.getRefCount());
      XSizeInputStream actualStream = actual.getStream();
      assertNotNull(actualStream);
      assertEquals(ONE_MB, actualStream.getSize());
      assertNotNull(actualStream.getInputStream());

      long count = XIOHelper.copy(actualStream, generateFakeOutputStream());
      assertEquals(ONE_MB, count);
      broker.close();
   }

   /**
    * Tests the deletion of OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void testDeleteContent()
        throws Exception {

      String mimeType = BINARY_MIME_TYPE;

      // save content
      createContent(generateInputStream(ONE_MB), mimeType, ONE_MB);

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

   /**
    * Tests the creation of a 5 MB OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void removedtestCreate5MBContent()
        throws Exception {
      contentSizeTesting(5 * ONE_MB);
   }

   /**
    * Tests the creation of a 10 MB OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void removedtestCreate10MBContent()
        throws Exception {
      contentSizeTesting(10 * ONE_MB);
   }

   /**
    * Tests the creation of a 20 MB OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void removedtestCreate20MBContent()
        throws Exception {
      contentSizeTesting(20 * ONE_MB);
   }

   /**
    * Tests the creation of a 50 MB OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void removedtestCreate50MBContent()
        throws Exception {
      contentSizeTesting(50 * ONE_MB);
   }

   /**
    * Tests the creation of a 100 MB OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void removedtestCreate100MBContent()
        throws Exception {
      contentSizeTesting(100 * ONE_MB);
   }

   // ******** Helper Methods *********

   /**
    * Test the creation of a content with a specified size
    *
    * @param size the size of the content to create
    */
   private void contentSizeTesting(long size)
        throws Exception {
      InputStream is = generateInputStream(size);

      // save content
      createContent(is, BINARY_MIME_TYPE, size);

      // load content
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from " + OpContent.CONTENT);
      List list = broker.list(query);

      assertNotNull(list);
      assertEquals(1, list.size());
      OpContent actual = (OpContent) list.get(0);
      assertEquals(BINARY_MIME_TYPE, actual.getMediaType());
      assertEquals(size, actual.getSize());
      assertEquals(1, actual.getRefCount());
      XSizeInputStream actualStream = actual.getStream();
      assertNotNull(actualStream);
      assertEquals(size, actualStream.getSize());
      assertNotNull(actualStream.getInputStream());

      long count = XIOHelper.copy(actualStream, generateFakeOutputStream());
      assertEquals(size, count);
      broker.close();
   }

   /**
    * Create and persist an OpConttent
    *
    * @param is       Input stream
    * @param mimeType the type of the file
    * @param size     the size of the content
    * @throws FileNotFoundException if the file cannot be found when datastream is read.
    */
   private void createContent(InputStream is, String mimeType, long size)
        throws FileNotFoundException {
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      XSizeInputStream stream = new XSizeInputStream(is, size);
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
      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();
      deleteAllObjects(broker, OpContent.CONTENT);
      transaction.commit();
      broker.close();
   }

   /**
    * Generate an <code>InputStream</code> with a given size.
    *
    * @param streamSize the size of the generated stream
    * @return an instance of <code>InputStream</code>
    */
   private InputStream generateInputStream(final long streamSize) {
      return new InputStream() {
         private long counter = 0;
         private long size = streamSize;

         /**
          * Reads the next byte of data from the input stream. The value byte is
          * returned as an <code>int</code> in the range <code>0</code> to
          * <code>255</code>. If no byte is available because the end of the stream
          * has been reached, the value <code>-1</code> is returned. This method
          * blocks until input data is available, the end of the stream is detected,
          * or an exception is thrown.
          * <p/>
          * <p> A subclass must provide an implementation of this method.
          *
          * @return the next byte of data, or <code>-1</code> if the end of the
          *         stream is reached.
          * @throws java.io.IOException if an I/O error occurs.
          */
         public int read()
              throws IOException {
            if (counter < size) {
               return (int) (counter++ % 256);
            }
            return -1;
         }
      };
   }

   /**
    * Generates a fake output stream.
    *
    * @return an instance of <code>OutputStream</code>
    */
   private OutputStream generateFakeOutputStream() {
      return new OutputStream() {
         public void write(int b)
              throws IOException {
            //do nothing
         }
      };
   }
}
