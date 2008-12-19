/*
 * Copyright(c) Onepoint Software GmbH 2008. All Rights Reserved.
 */
package onepoint.persistence.hibernate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpField;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContent;
import onepoint.service.server.XSession;

/**
 * This implementation of <code>InputStream</code> was introduced to simulate lazy loading for OpContent stream.
 *
 * @author calin.pavel
 */
public class OpBlobUserTypeStream extends InputStream {
   /**
    * The logger used in this class.
    */
   private static final XLog logger = XLogFactory.getLogger(OpBlobUserTypeStream.class);


   private long contentId;
   private boolean initialized;
   private OpBroker broker;
   private InputStream databaseStream;

   private static String query;

   /**
    * Creates a new instance of stream for the given content identifier.
    * This was introduced because is not necessary to load OpContent data everytime an instance is accessed
    * (simulate data lazy loading).
    *
    * @param contentId identifier of the <code>OpContent</code> instance.
    */
   public OpBlobUserTypeStream(long contentId) {
      this.contentId = contentId;

      // build SQL query to be executed for content/stream retrieval
      if (query == null) {
         OpPrototype contentPrototype = OpTypeManager.getPrototypeByClassName(OpContent.class.getName());
         String tableName = OpMappingsGenerator.generateTableName(contentPrototype.getName());
         OpField streamField = (OpField) contentPrototype.getMember("Stream");
         String streamColumnName = streamField.getColumn();

         query = "select " + streamColumnName + " from " + tableName + " where op_id=?";
      }
   }

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
   @Override
   public int read()
        throws IOException {
      if (!initialized) {
         OpProjectSession session = (OpProjectSession) XSession.getSession();
         broker = session.newBroker();

         try {
            Connection connection = broker.getJDBCConnection();
            ResultSet rs = executeBlobQuery(connection);
            if (rs.next()) {
               databaseStream = rs.getBinaryStream(1);
               // FIXME: maybe fix derby for this one:
               // TODO: this is a Derby workaround...
               if (databaseStream instanceof org.apache.derby.iapi.services.io.NewByteArrayInputStream) {
                  byte[] b = ((org.apache.derby.iapi.services.io.NewByteArrayInputStream)databaseStream).getData();
                  databaseStream = new ByteArrayInputStream(b);
               }
            }
         }
         catch (SQLException e) {
            logger.error("Could not execute query for OpContent data retrieval", e);
            throw new IOException(e.getMessage());
         }

         initialized = true;
      }

      int read = -1;
      if (databaseStream != null) {
         read = databaseStream.read();
      }
      if (read == -1) {
         closeBroker();
      }
      return read;
   }

   private ResultSet executeBlobQuery(Connection connection)
         throws SQLException {
      PreparedStatement ps = connection.prepareStatement(query);
      ps.setLong(1, contentId);
      ResultSet rs = ps.executeQuery();
      return rs;
   }

   /**
    * Closes this input stream and releases any system resources associated
    * with the stream.
    * <p/>
    * <p> The <code>close</code> method of <code>InputStream</code> does
    * nothing.
    *
    * @throws java.io.IOException if an I/O error occurs.
    */
   @Override
   public void close()
        throws IOException {
      super.close();

      initialized = false;
      if (databaseStream != null) {
         databaseStream.close();
      }
      closeBroker();
   }

   private void closeBroker() {
      if (broker != null) {
         broker.close();
         broker = null;
      }
   }
}
