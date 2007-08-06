/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.modules.documents.OpContentManager;

import java.util.List;
import java.util.Set;

/**
 * Helper class needed to perform OpAttachment related operations
 *
 * @author florin.haizea
 */
public class OpAttachmentDataSetFactory {

   private static final String ATTACHMENT_DOCUMENT = "d";

   //indexes
   private static final int ATTACHMENT_TYPE_INDEX = 0;
   private static final int ATTACHMENT_LOCATOR_INDEX = 1;
   private static final int ATTACHMENT_NAME_INDEX = 2;
   private static final int ATTACHMENT_LOCATION_INDEX = 3;
   private static final int ATTACHMENT_CONTENT_ID_INDEX = 4;

   /**
    * Fills the <code>XComponent</code> data set passed as parameter with attachment info from the
    *    <code>List</code> passed as paramenter.
    *
    * @param attachmentList - the <code>List</code> from where the attachment information is taken
    * @param attachmentsDataSet - the <code>XComponent</code> data set that is being filled.
    */
   public static void fillAttachmentsDataSet(List<List> attachmentList, XComponent attachmentsDataSet) {
      if (attachmentList != null) {
         for (List attachmentInfo : attachmentList) {
            XComponent newAttachmentRow = attachmentsDataSet.newDataRow();

            //0 - attachment type
            XComponent dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setStringValue((String) attachmentInfo.get(ATTACHMENT_TYPE_INDEX));
            newAttachmentRow.addChild(dataCell);

            //1 - attachment choice
            int iconIndex = 0;
            if (attachmentInfo.get(ATTACHMENT_TYPE_INDEX).equals(ATTACHMENT_DOCUMENT)) {
               iconIndex = 1;
            }
            String choice = (String) attachmentInfo.get(ATTACHMENT_LOCATOR_INDEX);
            String choiceId = XValidator.choiceID(choice);
            String name = (String) attachmentInfo.get(ATTACHMENT_NAME_INDEX);

            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setStringValue(XValidator.choice(choiceId, name, iconIndex));
            newAttachmentRow.addChild(dataCell);

            //2 - location
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setStringValue((String) attachmentInfo.get(ATTACHMENT_LOCATION_INDEX));
            newAttachmentRow.addChild(dataCell);

            //3 - for documents only
            if (attachmentInfo.get(ATTACHMENT_TYPE_INDEX).equals(ATTACHMENT_DOCUMENT)) {
               //if this is a new attachment - set the entire file
               //else set the content id of the existing attachment
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setValue(attachmentInfo.get(ATTACHMENT_CONTENT_ID_INDEX));
               newAttachmentRow.addChild(dataCell);
            }
            attachmentsDataSet.addChild(newAttachmentRow);
         }
      }
   }

   /**
    * Decrements the reference count of each content of each attachment from the <code>Set</code> passed
    *    as parameter. If the reference count reaches 0 then the <code>OpContent</code> entity will be deleted.
    *
    * @param broker - the <code>OpBroker</code> used for performing business operations.
    * @param attachmentSet - the <code>Set</code> of <code>OpAttachment</code> entities for which the
    *    content reference count will be decremented.
    */
   public static void removeContents(OpBroker broker, Set<OpAttachment> attachmentSet) {
      for (OpAttachment attachment : attachmentSet) {
         if (!attachment.getLinked()) {
            OpContentManager.updateContent(attachment.getContent(), broker, false, true);
            attachment.setContent(null);
         }
      }
   }
}
