/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.documents.test;

import onepoint.project.OpProjectSession;
import onepoint.project.test.OpTestDataFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class contains helper methods for managing documents data
 *
 * @author lucian.furtos
 */
public class OpDocumentsTestDataFactory extends OpTestDataFactory {

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public OpDocumentsTestDataFactory(OpProjectSession session) {
      super(session);
   }

   /**
    * Generate an <code>InputStream</code> with a given size.
    *
    * @param streamSize the size of the generated stream
    * @return an instance of <code>InputStream</code>
    */
   public InputStream generateInputStream(final long streamSize) {
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
}
