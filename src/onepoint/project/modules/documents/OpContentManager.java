/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.documents;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;

/**
 * Utility class, responsible for performing content-related operations.
 *
 * @author horia.chiorean
 */
public final class OpContentManager {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpContentManager.class, true);

   /**
    * This is private class.
    */
   private OpContentManager() {
   }

   /**
    * Creates a new content object, from the given content bytes.
    *
    * @param content  a <code>byte[]</code> representing the actual data.
    * @param mimeType a <code>String</code> representing the mime-type of the content. Can be null.
    * @return a new <code>OpContent</code> entity.
    */
   public static OpContent newContent(byte[] content, String mimeType) {
      OpContent result = new OpContent();
      result.setMediaType(mimeType);
      result.setRefCount(1);
      result.setBytes(content);
      result.setSize(content.length);
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
    */
   public static void updateContent(OpContent content, OpBroker broker, boolean addReference) {
      if (content != null) {
         int refCount = content.getRefCount();
         refCount = addReference ? ++refCount : --refCount;
         if (refCount == 0) {
            broker.deleteObject(content);
         }
         else {
            content.setRefCount(refCount);
            broker.updateObject(content);
         }
      }
      else {
         logger.error("Trying to update inexistent content");
      }
   }
}
