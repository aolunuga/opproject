/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.documents;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpAttachmentVersion;
import onepoint.service.XSizeInputStream;

import javax.activation.FileTypeMap;

/**
 * Utility class, responsible for performing content-related operations.
 *
 * @author horia.chiorean
 */
public final class OpContentManager {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpContentManager.class);
   private static final String DELETE_ZERO_REF_CONTENTS_QUERY_STRING = "delete from OpContent as content where content.RefCount=0";

   /**
    * This is private class.
    */
   private OpContentManager() {
   }

   /**
    * Creates a new content object, from the given stream.
    *
    * @param stream   a <code>XSizeInputStream</code> representing the actual data.
    * @param mimeType a <code>String</code> representing the mime-type of the content. Can be null.
    * @return a new <code>OpContent</code> entity.
    */
   public static OpContent newContent(XSizeInputStream stream, String mimeType) {
      OpContent result = new OpContent(stream);
      result.setMediaType(mimeType);
      result.setRefCount(1);
      return result;
   }

   /**
    * Creates a new content object, from the given stream.
    *
    * @param stream   a <code>XSizeInputStream</code> representing the actual data.
    * @param mimeType a <code>String</code> representing the mime-type of the content. Can be null.
    * @param refCount a <code>int</code> representing the number of references to this object
    * @return a new <code>OpContent</code> entity.
    */
   public static OpContent newContent(XSizeInputStream stream, String mimeType, int refCount) {
      OpContent result = new OpContent(stream);
      result.setMediaType(mimeType);
      result.setRefCount(refCount);
      return result;
   }

   /**
    * Creates a new content object, from the given content bytes.
    *
    * @param stream   a <code>XSizeInputStream</code> representing the actual data.
    * @param mimeType   a <code>String</code> representing the mime-type of the content. Can be null.
    * @param attachment the <code>OpAttachment</code> which refer this content.
    * @return a new <code>OpContent</code> entity.
    */
   public static OpContent newContent(XSizeInputStream stream, String mimeType, OpAttachment attachment) {
      OpContent result = newContent(stream, mimeType);
      result.getAttachments().add(attachment);
      return result;
   }

   /**
    * Creates a new content object, from the given content bytes.
    *
    * @param stream   a <code>XSizeInputStream</code> representing the actual data.
    * @param mimeType          a <code>String</code> representing the mime-type of the content. Can be null.
    * @param attachmentVersion the <code>OpAttachmentVersion</code> which refer this content.
    * @return a new <code>OpContent</code> entity.
    */
   public static OpContent newContent(XSizeInputStream stream, String mimeType, OpAttachmentVersion attachmentVersion) {
      OpContent result = newContent(stream, mimeType);
      result.getAttachmentVersions().add(attachmentVersion);
      return result;
   }

   /**
    * Creates a new content object, from the given content bytes.
    *
    * @param stream   a <code>XSizeInputStream</code> representing the actual data.
    * @param mimeType a <code>String</code> representing the mime-type of the content. Can be null.
    * @param document the <code>OpDocument</code> which refer this content.
    * @return a new <code>OpContent</code> entity.
    */
   public static OpContent newContent(XSizeInputStream stream, String mimeType, OpDocument document) {
      OpContent result = newContent(stream, mimeType);
      result.getDocuments().add(document);
      return result;
   }

   /**
    * Updates a content by either increasing of decreasing its references. If its references are decreased and reaches 0,
    * then it is marked for deletion.
    *
    * @param content      a <code>OpContent</code> entity representing the content to be updated.
    * @param broker       a <code>OpBroker</code> object used for performing business operations.
    * @param addReference a <code>boolean</code> indicating whether the reference to the content should be increased. If
    *                     false, it will be decreased.
    * @param attachment   the <code>OpAttachment</code> which refer this content.
    */
   public static void updateContent(OpContent content, OpBroker broker, boolean addReference, OpAttachment attachment) {
      if (content != null) {
         if (addReference) {
            content.getAttachments().add(attachment);
         }
         updateContent(content, broker, addReference);
      }
      else {
         logger.error("Trying to update inexistent content");
      }
   }

   /**
    * Updates a content by either increasing of decreasing its references. If its references are decreased and reaches 0,
    * then it is marked for deletion.
    *
    * @param content           a <code>OpContent</code> entity representing the content to be updated.
    * @param broker            a <code>OpBroker</code> object used for performing business operations.
    * @param addReference      a <code>boolean</code> indicating whether the reference to the content should be increased. If
    *                          false, it will be decreased.
    * @param attachmentVersion the <code>OpAttachmentVersion</code> which refer this content.
    */
   public static void updateContent(OpContent content, OpBroker broker, boolean addReference, OpAttachmentVersion attachmentVersion) {
      if (content != null) {
         if (addReference) {
            content.getAttachmentVersions().add(attachmentVersion);
         }
         updateContent(content, broker, addReference);
      }
      else {
         logger.error("Trying to update inexistent content");
      }
   }

   /**
    * Updates a content by either increasing of decreasing its references. If its references are decreased and reaches 0,
    * then it is marked for deletion.
    *
    * @param content      a <code>OpContent</code> entity representing the content to be updated.
    * @param broker       a <code>OpBroker</code> object used for performing business operations.
    * @param addReference a <code>boolean</code> indicating whether the reference to the content should be increased. If
    *                     false, it will be decreased.
    * @param document     the <code>OpDocument</code> which refer this content.
    */
   public static void updateContent(OpContent content, OpBroker broker, boolean addReference, OpDocument document) {
      if (content != null) {
         if (addReference) {
            content.getDocuments().add(document);
         }
         updateContent(content, broker, addReference);
      }
      else {
         logger.error("Trying to update inexistent content");
      }
   }

   /**
    * Updates a content by either increasing of decreasing its references. If its references are decreased and reaches 0,
    * then it is marked for deletion.
    *
    * @param content      a <code>OpContent</code> entity representing the content to be updated.
    * @param broker       a <code>OpBroker</code> object used for performing business operations.
    * @param addReference a <code>boolean</code> indicating whether the reference to the content should be increased. If
    *                     false, it will be decreased.
    */
   private static void updateContent(OpContent content, OpBroker broker, boolean addReference) {
      updateContent(content, broker, addReference, true);
   }

   /**
    * Updates a content by either increasing of decreasing its references.
    *
    * @param content      a <code>OpContent</code> entity representing the content to be updated.
    * @param broker       a <code>OpBroker</code> object used for performing business operations.
    * @param addReference a <code>boolean</code> indicating whether the reference to the content should be increased. If
    *                     false, it will be decreased.
    * @param enforceDeletion a <code>boolean</code> value indicating whether the <code>OpContent</code> entity should be marked
    *                      for deletion if it's reference count is 0.
    */
   public static void updateContent(OpContent content, OpBroker broker, boolean addReference, boolean enforceDeletion) {
       int refCount = content.getRefCount();
      refCount = addReference ? ++refCount : --refCount;
      if (refCount == 0 && enforceDeletion) {
         broker.deleteObject(content);
      }
      else {
         content.setRefCount(refCount);
         broker.updateObject(content);
      }
   }

   /**
    * Deletes all <code>OpContent</code> objects that have the reference count = 0.
    *
    * @param broker a <code>OpBroker</code> object used for performing business operations.
    */
   public static void deleteZeroRefContents(OpBroker broker) {
      OpQuery query = broker.newQuery(DELETE_ZERO_REF_CONTENTS_QUERY_STRING);
      broker.execute(query);
   }

   /**
    * Returns the mime type for the given file name.
    *
    * @param name file name to get the mime type for (based on extension)
    * @return the mime type for the given file name
    */
   public static String getFileMimeType(String name) {
      return FileTypeMap.getDefaultFileTypeMap().getContentType(name.toLowerCase());
   }
}
