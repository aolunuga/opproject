/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.documents;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
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
   private static final XLog logger = XLogFactory.getLogger(OpContentManager.class);

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
    * @param documentNode     the <code>OpDocumentNode</code> which refers this content.
    */
   public static void updateContent(OpContent content, OpBroker broker, boolean addReference, OpDocumentNode documentNode) {
      if (content != null) {
         if (addReference) {
            content.getDocumentNodes().add(documentNode);
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
    * @param document the <code>OpDocument</code> which refers this content.
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
         if (!content.exists()) {
        	 broker.makePersistent(content);
         }
         else {
        	 broker.updateObject(content);
         }
      }
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
