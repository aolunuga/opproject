/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.documents.test;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.service.XSizeInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class test document classes.
 *
 * @author lucian.furtos
 */
public class OpDocumentsTest extends OpBaseOpenTestCase {

   /**
    * This class logger
    */
   private static final XLog logger = XLogFactory.getLogger(OpBaseOpenTestCase.class);

   private static final long ONE_MB = 1024L * 1024L; // 1 MB
   private static final String BINARY_MIME_TYPE = "binary/octet-stream";
   private OpDocumentsTestDataFactory dataFactory;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   @Override
   protected void setUp()
        throws Exception {
      super.setUp();
      dataFactory = new OpDocumentsTestDataFactory(session);
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   @Override
   protected void tearDown()
        throws Exception {
      super.deleteAllObjects(OpContent.CONTENT);
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
      long contentId = createContent(dataFactory.generateInputStream(ONE_MB), mimeType, ONE_MB);

      // load content
      OpContent actual = null;
      OpBroker broker = session.newBroker();
      try {
         actual = (OpContent) broker.getObject(OpContent.class, contentId);
      }
      finally {
         broker.close();
      }

      assertNotNull(actual);
      assertEquals(mimeType, actual.getMediaType());
      assertEquals(ONE_MB, actual.getSize());
      assertEquals(1, actual.getRefCount());
      XSizeInputStream actualStream = actual.getStream();
      assertNotNull(actualStream);
      assertEquals(ONE_MB, actualStream.getSize());
      assertNotNull(actualStream.getInputStream());
   }

   /**
    * Tests the creation of OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void testCreateLazyLoadedContent()
        throws Exception {
      String mimeType = BINARY_MIME_TYPE;

      // save content
      long contentId = createContent(dataFactory.generateInputStream(ONE_MB), mimeType, ONE_MB);

      // load content
      OpBroker broker = session.newBroker();
      OpContent actual = null;
      try {
         actual = broker.getObject(OpContent.class, contentId);
         assertNotNull(actual);
      }
      finally {
         broker.close();
      }

      assertEquals(mimeType, actual.getMediaType());
      assertEquals(ONE_MB, actual.getSize());
      assertEquals(1, actual.getRefCount());
      XSizeInputStream actualStream = actual.getStream();
      assertNotNull(actualStream);
      assertEquals(actual.getSize(), actualStream.getSize());
      assertEquals(0, actualStream.available());
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
      long contentId = createContent(dataFactory.generateInputStream(ONE_MB), mimeType, ONE_MB);

      // load content
      OpBroker broker = session.newBroker();
      try {
         OpContent content = broker.getObject(OpContent.class, contentId);
         assertNotNull(content);

         OpTransaction t = broker.newTransaction();
         OpContentManager.updateContent(content, broker, true, (OpAttachment) null);
         t.commit();
         broker.clear();

         OpContent actual = (OpContent) broker.getObject(OpContent.class, contentId);
         assertEquals(mimeType, actual.getMediaType());
         assertEquals(ONE_MB, actual.getSize());
         assertEquals(2, actual.getRefCount());
         XSizeInputStream actualStream = actual.getStream();
         assertNotNull(actualStream);
         assertEquals(ONE_MB, actualStream.getSize());
         assertNotNull(actualStream.getInputStream());
      }
      finally {
         broker.close();
      }
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
      long contentId = createContent(dataFactory.generateInputStream(ONE_MB), mimeType, ONE_MB);

      // load content
      OpBroker broker = session.newBroker();
      try {
         OpContent content = broker.getObject(OpContent.class, contentId);
         assertNotNull(content);

         OpTransaction t = broker.newTransaction();
         OpContentManager.updateContent(content, broker, false, (OpAttachment) null);
         t.commit();
         OpQuery query = broker.newQuery("select count(content.id) from " + OpContent.CONTENT + " content");
         Number count = (Number) broker.list(query).get(0);
         assertEquals("Contents are still present in the database ", 0, count.intValue());
      }
      finally {
         broker.close();
      }
   }

   /**
    * Tests the creation of a OpContent which has the size equal to the maximum application
    * allowed size.
    *
    * @throws Exception if anything fails.
    */
   public void testCreateDefaultMaxSizeContent()
        throws Exception {
      contentSizeTesting(OpConfiguration.DEFAULT_MAX_ATTACHMENT_SIZE * ONE_MB);
   }

   /**
    * Tests that the stream of contents is lazy loaded, when working with multiple contents
    * if the contents are loaded one by one, using a <coed>OpBroker</code>.
    *
    * @throws Exception if anything unexpected fails.
    */
   public void testMultipleContentsLazyStream()
        throws Exception {
      int contentsNumber = 10;
      Map<Long, Long> contentsSizesMap = createContents(contentsNumber);

      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select content.id  from " + OpContent.CONTENT + " content ");
         List list = broker.list(query);

         assertNotNull(list);
         assertEquals(contentsNumber, list.size());
         for (Iterator it = list.iterator(); it.hasNext();) {
            Long id = (Long) it.next();
            OpContent actualContent = broker.getObject(OpContent.class, id);
            long contentSize = contentsSizesMap.get(actualContent.getId()).longValue();
            this.assertLazyContent(actualContent, contentSize);
         }
      }
      finally {
         broker.close();
      }
   }

   /**
    * Tests that the stream of contents is lazy loaded,  when working with multiple contents
    * and retrieving them via a HQL query.
    *
    * @throws Exception if anything unexpected fails.
    */
   //<FIXME author="Calin Pavel" description="Test is commented out because on MySQL at the moment it blocks indifinetly when performing broker.list()">
   public void _testMultipleContentsLazyStreamQuery()
        throws Exception {

      int contentsNumber = 10;
      Map<Long, Long> contentsSizesMap = createContents(contentsNumber);

      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery(" from " + OpContent.CONTENT);
         List list = broker.list(query);

         assertNotNull(list);
         assertEquals(contentsNumber, list.size());
         for (Iterator it = list.iterator(); it.hasNext();) {
            OpContent actualContent = (OpContent) it.next();
            long contentSize = contentsSizesMap.get(actualContent.getId()).longValue();
            this.assertLazyContent(actualContent, contentSize);
         }
      }
      finally {
         broker.close();
      }
   }
   //<FIXME>

   /**
    * Asserts that the given content is valid and its stream is lazy loaded.
    *
    * @param actualContent an <code>OpContent</code> instance.
    * @param contentSize   a <code>long</code> the expected size of the content.
    * @throws IOException if the stream of the content cannot be queried.
    */
   private void assertLazyContent(OpContent actualContent, long contentSize)
        throws IOException {
      assertEquals(BINARY_MIME_TYPE, actualContent.getMediaType());
      assertEquals(contentSize, actualContent.getSize());
      assertEquals(1, actualContent.getRefCount());
      assertEquals(0, actualContent.getStream().available());
      assertEquals(actualContent.getSize(), actualContent.getStream().getSize());
   }

   /**
    * Creates multiple contents and persists them in the db.
    *
    * @param contentsNumber a <code>int</code> the number of contents to create.
    * @return a <code>Map(Long, Long)</code> of content ids and their sizes.
    */
   private Map<Long, Long> createContents(int contentsNumber) {
      Map<Long, Long> contentsSizesMap = new HashMap<Long, Long>();
      //create contents into DB
      for (int i = 0; i < contentsNumber; i++) {
         long contentSize = Math.round(Math.random() * OpConfiguration.DEFAULT_MAX_ATTACHMENT_SIZE);
         if (contentSize == 0) {
            contentSize++;
         }
         contentSize *= ONE_MB;

         InputStream is = dataFactory.generateInputStream(contentSize);
         logger.info("Creating content with of size:  " + contentSize / ONE_MB + " MB");
         long contentId = this.createContent(is, BINARY_MIME_TYPE, contentSize);
         assertTrue(" The content wasn't created ", contentId > 0);
         contentsSizesMap.put(contentId, contentSize);
      }
      return contentsSizesMap;
   }


   //<FIXME author="Calin Pavel" description="These following tests are removed from running because of issue OPP-147. They fail with OOM">
   /**
    * Tests the creation of a 10 MB OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void _testCreate10MBContent()
        throws Exception {
      contentSizeTesting(10 * ONE_MB);
   }

   /**
    * Tests the creation of a 20 MB OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void _testCreate20MBContent()
        throws Exception {
      contentSizeTesting(20 * ONE_MB);
   }

   /**
    * Tests the creation of a 50 MB OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void _testCreate50MBContent()
        throws Exception {
      contentSizeTesting(50 * ONE_MB);
   }

   /**
    * Tests the creation of a 100 MB OpContent.
    *
    * @throws Exception if anything fails.
    */
   public void _testCreate100MBContent()
        throws Exception {
      contentSizeTesting(100 * ONE_MB);
   }
   //<FIXME>

   // ******** Helper Methods *********

   /**
    * Test the creation of a content with a specified size
    *
    * @param size the size of the content to create
    * @throws IOException if streams cannot be copied
    */
   private void contentSizeTesting(long size)
        throws IOException {
      InputStream is = dataFactory.generateInputStream(size);

      // save content
      long contentId = createContent(is, BINARY_MIME_TYPE, size);
      is.close();
      is = null;
      
      // load content
      OpBroker broker = session.newBroker();
      OpContent actual = null;
      try {
         actual = (OpContent) broker.getObject(OpContent.class, contentId);
      }
      finally {
         broker.close();
      }

      assertNotNull(actual);
      assertEquals(BINARY_MIME_TYPE, actual.getMediaType());
      assertEquals(size, actual.getSize());
      assertEquals(1, actual.getRefCount());
      XSizeInputStream actualStream = actual.getStream();
      assertNotNull(actualStream);
      assertEquals(size, actualStream.getSize());
      assertNotNull(actualStream.getInputStream());
   }

   /**
    * Create and persist an OpConttent
    *
    * @param is       Input stream
    * @param mimeType the type of the file
    * @param size     the size of the content
    * @return a <code>long</code> the id of the newly created content
    */
   private long createContent(InputStream is, String mimeType, long size) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();

         XSizeInputStream stream = new XSizeInputStream(is, size);
         OpContent content = OpContentManager.newContent(stream, mimeType, 1);
         broker.makePersistent(content);

         t.commit();
         return content.getId();
      }
      finally {
         broker.close();
      }
   }
}
