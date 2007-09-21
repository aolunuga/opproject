/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.project.OpAttachmentDataSetFactory;
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

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      logger.info("OpManageAttachmentsFormProvider.prepareForm()");

      // Check for cost record ID
      List<List> attachmentList = (List) parameters.get(OpWorkService.ATTACHMENT_LIST);

      XComponent attachmentSet = form.findComponent(ATTACHMENT_SET);
      XComponent costRowIndexField = form.findComponent(COST_ROW_INDEX_FIELD);
      costRowIndexField.setValue((Integer) parameters.get(OpWorkService.COST_ROW_INDEX));

      OpAttachmentDataSetFactory.fillAttachmentsDataSet(attachmentList, attachmentSet);

      logger.info("/OpManageAttachmentsFormProvider.prepareForm()");
   }
}