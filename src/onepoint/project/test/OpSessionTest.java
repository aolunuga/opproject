/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.service.XMessage;
import onepoint.util.XEnvironmentManager;
import onepoint.util.XIOHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test the OpProjectSession class
 *
 * @author lucian.furtos
 */
public class OpSessionTest extends OpBaseOpenTestCase {

   private static final String FILE_PREFIX = "testfile";
   private static final int FILE_SIZE = 1024;

   private File imgFile;
   private File pdfFile;
   private File mp3File;
   private File parent;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   @Override
   protected void setUp()
        throws Exception {
      super.setUp();
      parent = new File(XEnvironmentManager.TMP_DIR);

      imgFile = createFile("jpg");
      pdfFile = createFile("pdf");
      mp3File = createFile("mp3");
   }


   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   @Override
   protected void tearDown()
        throws Exception {
      clean();
      super.tearDown();
   }

   /**
    * Test a single file upload
    */
   public void testUploadFile()
        throws Exception {
      Map<String, File> files = new HashMap<String, File>();
      files.put("file", imgFile);
      XMessage message = new XMessage("Service.Target");
      message.setArgument("args", files);
      session.processFiles(message);

      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("from OpContent");
         List<OpContent> list = broker.list(query);
         assertNotNull(list);
         assertEquals(1, list.size());
         OpContent content = list.get(0);
         assertEquals(0, content.getRefCount());
         assertEquals(FILE_SIZE, content.getSize());
         assertEquals(OpContentManager.getFileMimeType(imgFile.getName()), content.getMediaType());
         String contentId = content.locator();
         Map newFileMap = (Map) message.getArgument("args");
         assertEquals(contentId, newFileMap.get("file"));
      }
      finally {
         broker.close();
      }
   }

   /**
    * Test multiple references file upload
    */
   public void testUploadMultipleFiles()
        throws Exception {
      Map<String, File> files = new HashMap<String, File>();
      files.put("file1", imgFile);
      files.put("file2", mp3File);
      files.put("file3", imgFile);
      files.put("file4", pdfFile);
      files.put("file5", mp3File);
      XMessage message = new XMessage("Service.Target");
      message.setArgument("args", files);
      session.processFiles(message);

      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("from OpContent");
         List<OpContent> list = broker.list(query);
         assertNotNull(list);
         assertEquals(3, list.size());
         Map newFileMap = (Map) message.getArgument("args");
         assertEquals(newFileMap.get("file1"), newFileMap.get("file3"));
         assertEquals(newFileMap.get("file2"), newFileMap.get("file5"));
      }
      finally {
         broker.close();
      }
   }

   /**
    * Test upload invalid files
    */
   public void testUploadWrongFiles()
        throws Exception {
      Map<String, File> files = new HashMap<String, File>();
      files.put("file1", imgFile);
      files.put("file2", parent); // invalid file - folder
      XMessage message = new XMessage("Service.Target");
      message.setArgument("args", files);
      session.processFiles(message);

      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("from OpContent");
         List<OpContent> list = broker.list(query);
         assertNotNull(list);
         assertEquals(1, list.size());
         Map newFileMap = (Map) message.getArgument("args");
         assertNull(newFileMap.get("file2"));
      }
      finally {
         broker.close();
      }
   }

   // ***** Helper Methods *****

   /**
    * Clean database and file system for tests
    */
   protected void clean()
        throws Exception {
      //delete temporary files
      File[] files = parent.listFiles(new FilenameFilter() {
         public boolean accept(File dir, String name) {
            return name.startsWith(FILE_PREFIX);
         }
      });
      for (File file : files) {
         file.delete();
      }

      super.clean();
   }

   /**
    * Create a temporary file
    *
    * @param type the file extension
    * @return the File object for the newly created file
    */
   private File createFile(String type)
        throws Exception {
      File file = File.createTempFile(FILE_PREFIX, type, parent);
      byte[] bytes = new byte[FILE_SIZE];
      for (int i = 0; i < bytes.length; i++) {
         bytes[i] = (byte) (i % 256);
      }
      FileOutputStream out = new FileOutputStream(file);
      XIOHelper.copy(new ByteArrayInputStream(bytes), out);
      out.flush();
      out.close();

      return file;
   }
}
