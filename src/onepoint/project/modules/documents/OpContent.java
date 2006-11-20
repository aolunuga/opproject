/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.documents;

import onepoint.persistence.OpObject;

import java.util.Set;

public class OpContent extends OpObject {
   
   public final static String CONTENT = "OpContent";

   private int refCount;
   private String mediaType; // MIME-type
   private long size;
   private byte[] bytes; // TODO: Use OpBlobUserType; what about streaming?
   private Set attachments;
   private Set attachmentVersions;

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
   
   public void setAttachments(Set attachments) {
      this.attachments = attachments;
   }
   
   public Set getAttachments() {
      return attachments;
   }

   public void setAttachmentVersions(Set attachmentVersions) {
      this.attachmentVersions = attachmentVersions;
   }
   
   public Set getAttachmentVersions() {
      return attachmentVersions;
   }

}