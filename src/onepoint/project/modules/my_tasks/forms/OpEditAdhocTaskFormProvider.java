/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project_planning.forms.OpEditActivityFormProvider;
import onepoint.project.modules.resource.OpResource;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

import java.util.*;

/**
 * Form provider for edit adhoc task action.
 *
 * @author mihai.costin
 */
public class OpEditAdhocTaskFormProvider implements XFormProvider {

   private static final String RESOURCE_SET = "ResourceSet";
   private static final String PROJECT_SET = "ProjectSet";
   private static final String PROJECT_TO_RESOURCE_MAP = "ProjectToResourceMap";
   private static final String TASK_NAME = "Name";
   private static final String TASK_DESCRIPTION = "Description";
   private static final String TASK_PRIORITY = "Priority";
   private static final String TASK_DUE_DATE = "DueDate";
   private static final String TASK_PROJECT = "ProjectChooser";
   private static final String TASK_RESOURCE = "ResourceChooser";
   private static final String ACTIVITY_LOCATOR_FIELD = "ActivityLocator";

   private static final String RESOURCE_MAP = "my_tasks.adhoc_tasks";
   private static final String INFO_TITLE = "InfoAdhocTitle";
   private static final String OK_BUTTON = "okButton";
   private static final String ADD_DOC_BUTTON = "AddDocumentButton";
   private static final String ADD_URL_BUTTON = "AddURLButton";
   private static final String REMOVE_BUTTON = "RemoveAttachmentButton";

   //parameters
   private static final String SELECTED_ROW = "selectedRow";
   private static final String EDIT_MODE = "EditMode";
   private static final String ATTACHMENT_SET = "AttachmentSet";
   private static final String ACTIVITY_ROW_INDEX = "ActivityRowIndex";
   private static final String AD_HOC_RESOURCE_MAP = "my_tasks.adhoc_tasks";

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      XComponent resourceDataSet = form.findComponent(RESOURCE_SET);
      XComponent projectToResource = form.findComponent(PROJECT_TO_RESOURCE_MAP);
      XComponent projectDataSet = form.findComponent(PROJECT_SET);
      XComponent adhocRow = (XComponent) parameters.get(SELECTED_ROW);
      int activityIndex = adhocRow.getIndex();
      form.findComponent(ACTIVITY_ROW_INDEX).setIntValue(activityIndex);
      Boolean editMode = (Boolean) parameters.get(EDIT_MODE);
      OpProjectSession session = (OpProjectSession) s;

      Map projectsMap = OpProjectDataSetFactory.getProjectToResourceMap(session);
      projectToResource.setValue(projectsMap);

      String activityLocator = adhocRow.getStringValue();
      XComponent locatorField = form.findComponent(ACTIVITY_LOCATOR_FIELD);
      locatorField.setStringValue(activityLocator);

      OpBroker broker = session.newBroker();
      OpActivity task = (OpActivity) broker.getObject(activityLocator);
      XComponent nameField = form.findComponent(TASK_NAME);
      nameField.setStringValue(task.getName());
      XComponent descriptionField = form.findComponent(TASK_DESCRIPTION);
      descriptionField.setStringValue(task.getDescription());
      XComponent priorityField = form.findComponent(TASK_PRIORITY);
      priorityField.setIntValue(task.getPriority());
      XComponent dueField = form.findComponent(TASK_DUE_DATE);
      dueField.setDateValue(task.getFinish());

      //fill project data set
      int index = 0;
      int selectedIndex = 0;
      List resources = new ArrayList();
      for (Iterator iterator = projectsMap.keySet().iterator(); iterator.hasNext();) {
         String choice = (String) iterator.next();
         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(choice);
         projectDataSet.addChild(row);
         if (XValidator.choiceID(choice).equals(task.getProjectPlan().getProjectNode().locator())) {
            selectedIndex = index;
            resources = (List) projectsMap.get(choice);
         }
         index++;
      }
      XComponent projectChooser = form.findComponent(TASK_PROJECT);
      projectChooser.setSelectedIndex(new Integer(selectedIndex));

      //fill resource data set
      XComponent resourcetChooser = form.findComponent(TASK_RESOURCE);
      OpResource resource = null;
      Set assignments = task.getAssignments();
      for (Iterator iterator = assignments.iterator(); iterator.hasNext();) {
         OpAssignment assignment = (OpAssignment) iterator.next();
         resource = assignment.getResource();
      }
      index = 0;
      selectedIndex = 0;
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         String choice = (String) iterator.next();
         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(choice);
         resourceDataSet.addChild(row);
         if (resource != null && XValidator.choiceID(choice).equals(resource.locator())) {
            selectedIndex = index;
         }
      }
      resourcetChooser.setSelectedIndex(new Integer(selectedIndex));

      //fill attachement tab
      Set attachments = task.getAttachments();
      addAttachments(form, attachments);
      XLanguageResourceMap resourceMap = session.getLocale().getResourceMap(AD_HOC_RESOURCE_MAP);
      OpEditActivityFormProvider.showComments(form, task, session, broker, resourceMap, true);

      broker.close();

      if (editMode != null && !editMode.booleanValue()) {
         //disable all fields
         String title = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(INFO_TITLE).getText();
         form.setText(title);
         form.findComponent(OK_BUTTON).setVisible(false);
         form.findComponent(ADD_DOC_BUTTON).setEnabled(false);
         form.findComponent(ADD_URL_BUTTON).setEnabled(false);
         form.findComponent(REMOVE_BUTTON).setEnabled(false);
         nameField.setEnabled(false);
         descriptionField.setEnabled(false);
         priorityField.setEnabled(false);
         dueField.setEnabled(false);
         projectChooser.setEnabled(false);
         resourcetChooser.setEnabled(false);
      }
   }


   private void addAttachments(XComponent form, Set attachments) {
      //attachments tab

      // Fill attachments tab
      XComponent attachmentSet = form.findComponent(ATTACHMENT_SET);
      Iterator it = attachments.iterator();
      while (it.hasNext()) {
         OpAttachment attachment = (OpAttachment) it.next();
         XComponent attachmentRow = new XComponent(XComponent.DATA_ROW);

         //0 - type "u" or "d" identifier
         XComponent cell = new XComponent(XComponent.DATA_CELL);
         String type;
         int idx;
         if (attachment.getLinked()) {
            type = OpActivityDataSetFactory.LINKED_ATTACHMENT_DESCRIPTOR;
            idx = 0;
         }
         else {
            type = OpActivityDataSetFactory.DOCUMENT_ATTACHMENT_DESCRIPTOR;
            idx = 1;
         }
         cell.setStringValue(type);
         attachmentRow.addChild(cell);

         //1 - choice [name, locator]
         cell = XComponent.newDataCell();
         cell.setStringValue(XValidator.choice(String.valueOf(attachment.locator()), attachment.getName(), idx));
         attachmentRow.addChild(cell);

         //2 - location
         cell = XComponent.newDataCell();
         cell.setStringValue(attachment.getLocation());
         attachmentRow.addChild(cell);

         //for documents only
         if (!attachment.getLinked()) {
            //3 - contentId
            cell = XComponent.newDataCell();
            String contentId = OpLocator.locatorString(attachment.getContent());
            cell.setStringValue(contentId);
            attachmentRow.addChild(cell);
         }

         attachmentSet.addDataRow(attachmentRow);
      }
   }

}
