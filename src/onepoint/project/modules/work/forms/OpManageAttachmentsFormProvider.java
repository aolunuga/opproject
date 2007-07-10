/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.project_planning.forms.OpActivitiesFormProvider;
import onepoint.project.modules.work.OpWorkService;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.List;

/**
 * Form provider for add/remove attachments dialog
 *
 * @author florin.haizea
 */
public class OpManageAttachmentsFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getServerLogger(OpActivitiesFormProvider.class);

   private static final String ATTACHMENT_SET = "AttachmentSet";
   private static final String COST_ROW_INDEX_FIELD = "CostRowIndex";

   private static final String ATTACHMENT_DOCUMENT = "d";

   private static final int ATTACHMENT_TYPE_INDEX = 0;
   private static final int ATTACHMENT_LOCATOR_INDEX = 1;
   private static final int ATTACHMENT_NAME_INDEX = 2;
   private static final int ATTACHMENT_LOCATION_INDEX = 3;
   private static final int ATTACHMENT_CONTENT_ID_INDEX = 4;

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      logger.info("OpManageAttachmentsFormProvider.prepareForm()");

      // Check for cost record ID
      List<List> attachmentList = (List) parameters.get(OpWorkService.ATTACHMENT_LIST);

      XComponent attachmentSet = form.findComponent(ATTACHMENT_SET);
      XComponent costRowIndexField = form.findComponent(COST_ROW_INDEX_FIELD);
      costRowIndexField.setValue((Integer) parameters.get(OpWorkService.COST_ROW_INDEX));

      if (attachmentList != null) {
         for (List attachmentInfo : attachmentList) {
            XComponent newAttachmentRow = attachmentSet.newDataRow();

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
            attachmentSet.addChild(newAttachmentRow);
         }
      }

      logger.info("/OpManageAttachmentsFormProvider.prepareForm()");
   }
}