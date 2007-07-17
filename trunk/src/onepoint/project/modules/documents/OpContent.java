/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.documents;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpAttachmentVersion;
import onepoint.service.XSizeInputStream;

import java.util.HashSet;
import java.util.Set;
import java.io.*;

public class OpContent extends OpObject {

   public final static String CONTENT = "OpContent";
   public final static String MEDIA_TYPE = "MediaType";
   public final static String ATTACHMENTS = "Attachments";
   public final static String ATTACHMENT_VERSIONS = "AttachmentVersions";
   public final static String DOCUMENTS = "Documents";
   public final static String SIZE = "Size";
   public final static String STREAM = "Stream";

   private int refCount;
   private String mediaType; // MIME-type
   private long size;
   private XSizeInputStream stream;
   private Set<OpAttachment> attachments = new HashSet<OpAttachment>();
   private Set<OpAttachmentVersion> attachmentVersions = new HashSet<OpAttachmentVersion>();
   private Set<OpDocument> documents = new HashSet<OpDocument>();

   /**
    * Default constructor
    */
   @Deprecated
   OpContent() {
      // requiered by Hibernate
   }

   /**
    * Create a new instance of <code>OpContent</code> based on the specified stream
    *
    * @param stream an <code>XSizeInputStream</code> instance
    */
   public OpContent(XSizeInputStream stream) {
      this.stream = stream;
      this.size = stream == null ? 0 : stream.getSize();
   }

   /**
    * Create a new instance of <code>OpContent</code> based on the specified stream and the specified size
    *
    * @param stream   the <code>InputStream</code> instance that contains the data
    * @param size the number of bytes from the provided stream. [0..Long.MAX_VALUE]
    */
   public OpContent(InputStream stream, long size) {
      this.stream = new XSizeInputStream(stream, size);
      this.size = size;
   }

   public void setRefCount(int refCount) {
      this.refCount = refCount;
   }

   public int getRefCount() {
      return refCount;
   }

   public void setMediaType(String mediaType) {
      this.mediaType = mediaType;
   }

   public String getMediaType() {
      return mediaType;
   }

   /**
    * Sets the size of the Content. Only for internal use. (Hibernate)
    *
    * @param size the size of the Content data in bytes
    */
   public void setSize(long size) {
      this.size = size;
   }

   /**
    * Gets the size of the Content.
    *
    * @return the size of the Content data in bytes
    */
   public long getSize() {
      return size;
   }

   /**
    * Sets the input stream to be written into the database. This method will overwrite the value of the <code>size</code> code.
    *
    * @param stream an <code>XSizeInputStream</code> instance. Ignored if null.
    */
   public void setStream(XSizeInputStream stream) {
      if (stream != null) {
         if (stream.getSize() == XSizeInputStream.UNKNOW_STREAM_SIZE) {
            stream.setSize(this.size);  // if the stream doesn't have the sized set, use the persisted value
         }
         this.stream = stream;
      }
   }

   /**
    * Gets the input stream from the database. The size of the stream is defined by the <code>size</code> property.
    *
    * @return an <code>XSizeInputStream</code> instance.
    */
   public XSizeInputStream getStream() {
      if (stream != null && stream.getSize() == XSizeInputStream.UNKNOW_STREAM_SIZE) {
         stream.setSize(size); // if stream doesn't have the size set (retrieved from database), set it using the persisted size
      }
      return stream;
   }

   //
   public void setAttachments(Set<OpAttachment> attachments) {
      this.attachments = attachments;
   }

   public Set<OpAttachment> getAttachments() {
      return attachments;
   }

   public Set<OpDocument> getDocuments() {
      return documents;
   }

   public void setDocuments(Set<OpDocument> documents) {
      this.documents = documents;
   }

   public Set<OpAttachmentVersion> getAttachmentVersions() {
      return attachmentVersions;
   }

   public void setAttachmentVersions(Set<OpAttachmentVersion> attachmentVersions) {
      this.attachmentVersions = attachmentVersions;
   }
}
