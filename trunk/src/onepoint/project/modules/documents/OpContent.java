/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.documents;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpAttachmentVersion;

import java.util.HashSet;
import java.util.Set;

public class OpContent extends OpObject {
   
   public final static String CONTENT = "OpContent";
   public final static String MEDIA_TYPE = "MediaType";
   public final static String ATTACHMENTS = "Attachments";
   public final static String ATTACHMENT_VERSIONS = "AttachmentVersions";
   public final static String DOCUMENTS = "Documents";
   public final static String SIZE = "Size";
   public final static String BYTES = "Bytes";

   private int refCount;
   private String mediaType; // MIME-type
   private long size;
   private byte[] bytes; // TODO: Use OpBlobUserType; what about streaming?
   private Set<OpAttachment> attachments = new HashSet<OpAttachment>();
   private Set<OpAttachmentVersion> attachmentVersions = new HashSet<OpAttachmentVersion>();
   private Set<OpDocument> documents = new HashSet<OpDocument>();

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

   public void setSize(long size) {
      this.size = size;
   }

   public long getSize() {
      return size;
   }

   public void setBytes(byte[] bytes) {
      this.bytes = bytes;
   }

   public byte[] getBytes() {
      return bytes;
   }
   
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
