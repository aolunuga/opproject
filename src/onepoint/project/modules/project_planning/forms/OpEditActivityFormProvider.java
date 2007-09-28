/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityComment;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpSubjectDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;

public class OpEditActivityFormProvider implements XFormProvider {

   public final static String PROJECT_EDIT_ACTIVITY = "project_planning.EditActivity";

   private static final XLog logger = XLogFactory.getServerLogger(OpActivitiesFormProvider.class);

   private final static String RESPONSIBLE_RESOURCE_SET = "ResponsibleResourceSet";
   private final static String ACTIVITY_CATEGORY_DATA_SET = "ActivityCategoryDataSet";
   private final static String ACTIVITY_CATEGORY_CHOOSER = "Category";
   private final static String COMMENTS_LABEL = "CommentsLabel";
   private final static String COMMENTS_PANEL = "CommentsPanel";
   private final static String ADD_COMMENT_BUTTON = "AddCommentButton";
   private final static String ACTIVITY_ID_FIELD = "ActivityIDField";
   private final static String HAS_COMMENTS_FIELD = "HasCommentsField";

   //action that should be performed when a comment is removed
   private static final String REMOVE_COMMENT_ACTION = "removeComment";
   //remove comment button icon
   private static final String REMOVE_COMMENT_ICON = "/icons/minus_s.png";
   private static final String REMOVE_COMMENT_BUTTON_STYLE_REF = "icon-button-default";
   private static final String COMMENT_SO_FAR = "CommentSoFar";
   private static final String COMMENTS_SO_FAR = "CommentsSoFar";
   private static final String NO_COMMENTS = "NoCommentsPossible";
   private static final String NO_RESOURCE_TEXT = "NoResource";
   private static final String COMPLETE = "Complete";
   private static final String NO_CATEGORY_RESOURCE = "NoCategory";
   private static final String COSTS_TAB = "CostsTab";

   protected int activityType = -1;

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      logger.info("OpEditActivityFormProvider.prepareForm()");

      OpProjectSession session = (OpProjectSession) s;

      OpBroker broker = session.newBroker();
      OpUser currentUser = session.user(broker);

      // Check for activity ID (get activity if activity is a version)
      String activityLocator = (String) parameters.get(OpProjectPlanningService.ACTIVITY_ID);
      OpActivity activity = null;
      if (activityLocator != null) {
         OpObject object = broker.getObject(activityLocator);
         if (object instanceof OpActivity) {
            activity = (OpActivity) object;
            activityType = activity.getType();
         }
         else if (object instanceof OpActivityVersion) {
            OpActivityVersion activityVersion = (OpActivityVersion) object;
            activity = activityVersion.getActivity();
            activityType = (activity != null) ? activity.getType() : activityVersion.getType();
         }
         // Store resolved activity locator in data-field
         if (activity != null) {
            form.findComponent(ACTIVITY_ID_FIELD).setStringValue(activity.locator());
         }
      }

      //enable the %complete field if tracking is off
      String currentProjectId = (String) session.getVariable(OpProjectConstants.PROJECT_ID);
      if (currentProjectId != null) {
         OpProjectNode project = (OpProjectNode) broker.getObject(currentProjectId);
         if (!project.getPlan().getProgressTracked()) {
            form.findComponent(COMPLETE).setEnabled(true);
         }
         else {
            form.findComponent(COMPLETE).setEnabled(false);
         }
      }

      XLanguageResourceMap resourceMap = session.getLocale().getResourceMap(PROJECT_EDIT_ACTIVITY);

      // Activity categories
      XComponent categoryDataSet = form.findComponent(ACTIVITY_CATEGORY_DATA_SET);
      XComponent categoryChooser = form.findComponent(ACTIVITY_CATEGORY_CHOOSER);
      addCategories(broker, categoryChooser, categoryDataSet, resourceMap);

      //Resource set
      XComponent responsibleSet = form.findComponent(RESPONSIBLE_RESOURCE_SET);
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      String noResource = resourceMap.getResource(NO_RESOURCE_TEXT).getText();
      dataRow.setStringValue(XValidator.choice(OpGanttValidator.NO_RESOURCE_ID, noResource));
      responsibleSet.addChild(dataRow);

      showComments(form, activity, session, broker, resourceMap, true);

      //if the app. is multiuser and hide manager features is set to true and the user is not manager
      if (OpSubjectDataSetFactory.shouldHideFromUser(currentUser)) {
         //hide costs tab 
         form.findComponent(COSTS_TAB).setHidden(true);
      }

      logger.info("/OpEditActivityFormProvider.prepareForm()");
      broker.close();
   }

   /**
    * Adds to the curent form the comments panel information.
    *
    * @param form        Current form
    * @param activity    Activity to show the comments for
    * @param session     Current project session
    * @param broker      Broker instance for db access
    * @param resourceMap langauage resource map for comments
    * @param enabled     if true, panel is action enabled (remove/add comment)
    */
   public static void showComments(XComponent form, OpActivity activity, OpProjectSession session, OpBroker broker, XLanguageResourceMap resourceMap, boolean enabled) {
      // Show comments if activity is already persistent
      XComponent commentsLabel = form.findComponent(COMMENTS_LABEL);
      XComponent hasCommentsField = form.findComponent(HAS_COMMENTS_FIELD);

      // Ability to add and remove comments is only defined by access control list
      // (No edit-mode required in order to add comments -- reason: Contributor-access)
      byte accessLevel = OpPermission.OBSERVER;
      if (activity != null) {
         accessLevel = session.effectiveAccessLevel(broker, activity.getProjectPlan().getProjectNode().getID());
         XComponent commentsPanel = form.findComponent(COMMENTS_PANEL);
         boolean removeEnabled = (accessLevel >= OpPermission.ADMINISTRATOR) && enabled;
         int commentCount = addComments(session, broker, activity, commentsPanel, resourceMap, removeEnabled);
         hasCommentsField.setBooleanValue(commentCount > 0);
         StringBuffer commentsBuffer = new StringBuffer();
         commentsBuffer.append(commentCount);
         commentsBuffer.append(' ');
         if (commentCount == 1) {
            commentsBuffer.append(resourceMap.getResource(COMMENT_SO_FAR).getText());
         }
         else {
            commentsBuffer.append(resourceMap.getResource(COMMENTS_SO_FAR).getText());
         }
         commentsLabel.setText(commentsBuffer.toString());
      }
      else {
         commentsLabel.setText(resourceMap.getResource(NO_COMMENTS).getText());
         hasCommentsField.setBooleanValue(false);
      }

      XComponent addCommentButton = form.findComponent(ADD_COMMENT_BUTTON);
      // check for enable add comment button
      if (accessLevel >= OpPermission.CONTRIBUTOR && enabled) {
         addCommentButton.setEnabled(true);
      }
   }

   protected void addCategories(OpBroker broker, XComponent categoryChooser, XComponent dataSet, XLanguageResourceMap resourceMap) {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      String noCategory = resourceMap.getResource(NO_CATEGORY_RESOURCE).getText();
      dataRow.setStringValue(XValidator.choice(OpGanttValidator.NO_CATEGORY_ID, noCategory));
      dataSet.addChild(dataRow);
      categoryChooser.setEnabled(false);
   }

   private static int addComments(OpProjectSession session, OpBroker broker, OpActivity activity, XComponent commentsPanel, XLanguageResourceMap resourceMap, boolean enableCommentRemoving) {

      OpQuery query = broker.newQuery("select comment, creator.DisplayName from OpActivityComment as comment inner join comment.Creator as creator where comment.Activity.ID = ? order by comment.Sequence");

      query.setLong(0, activity.getID());
      Iterator result = broker.iterate(query);
      Object[] record;
      int count = 0;
      OpActivityComment comment;
      XComponent commentPanel;

      //use localizer to localize name of administrator
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionDataSetFactory.USER_OBJECTS));

      while (result.hasNext()) {
         record = (Object[]) result.next();
         comment = (OpActivityComment) record[0];
         commentPanel = createPanel(comment, resourceMap, localizer, enableCommentRemoving, session.getCalendar());
         commentsPanel.addChild(commentPanel);
         count++;
      }
      return count;
   }

   /**
    * Creates a comment panel with the given information.
    *
    * @param comment               comment entity the panel is created for
    * @param resourceMap           language resource map
    * @param localizer             localizer used for the name of the comment creator
    * @param enableCommentRemoving enable/disable remove dialog button
    * @param calendar a <code>XCalendar</code> representing the client's calendar.
    * @return an <code>XComponent</code> representing the comment panel
    */
   public static XComponent createPanel(OpActivityComment comment, XLanguageResourceMap resourceMap, XLocalizer localizer, boolean enableCommentRemoving, XCalendar calendar) {
      XComponent commentPanel;
      StringBuffer subjectBuffer;
      XComponent subjectLabel;
      StringBuffer whoAndWhenBuffer;
      XComponent whoAndWhenLabel;
      XComponent textPanel;
      XComponent textBox;
      XComponent buttonPanel;
      XComponent removeButton;
      int count = comment.getSequence();
      String by = resourceMap.getResource("By").getText();
      String on = resourceMap.getResource("On").getText();
      String at = resourceMap.getResource("At").getText();
      commentPanel = new XComponent(XComponent.PANEL);
      commentPanel.setLayout("flow");
      commentPanel.setDirection(XComponent.SOUTH);
      commentPanel.setStyle(XComponent.DEFAULT_LAYOUT_PANEL_STYLE);

      // Subject in bold font
      subjectBuffer = new StringBuffer();
      subjectBuffer.append(comment.getSequence());
      subjectBuffer.append("   ");
      subjectBuffer.append(comment.getName());
      subjectLabel = new XComponent(XComponent.LABEL);
      subjectLabel.setStyle(XComponent.DEFAULT_LABEL_EMPHASIZED_LEFT_STYLE);
      subjectLabel.setText(subjectBuffer.toString());
      commentPanel.addChild(subjectLabel);
      // Creator display name, date and time
      whoAndWhenBuffer = new StringBuffer(by);
      whoAndWhenBuffer.append(' ');
      whoAndWhenBuffer.append(localizer.localize(comment.getCreator().getDisplayName()));
      whoAndWhenBuffer.append(' ');
      whoAndWhenBuffer.append(on);
      whoAndWhenBuffer.append(' ');
      whoAndWhenBuffer.append(calendar.localizedDateToString(new Date(comment.getCreated().getTime())));
      whoAndWhenBuffer.append(' ');
      whoAndWhenBuffer.append(at);
      whoAndWhenBuffer.append(' ');
      whoAndWhenBuffer.append(calendar.localizedTimeToString(new Date(comment.getCreated().getTime())));
      whoAndWhenLabel = new XComponent(XComponent.LABEL);
      whoAndWhenLabel.setStyle(XComponent.DEFAULT_LABEL_LEFT_STYLE);
      whoAndWhenLabel.setText(whoAndWhenBuffer.toString());
      commentPanel.addChild(whoAndWhenLabel);
      //text panel contains text box and remove button
      textPanel = new XComponent(XComponent.PANEL);
      textPanel.setDirection(XComponent.EAST);
      textPanel.setLayout("flow");
      textPanel.setStyle(XComponent.DEFAULT_LAYOUT_PANEL_STYLE);
      commentPanel.addChild(textPanel);
      // Actual comment (text)
      textBox = new XComponent(XComponent.TEXT_BOX);
      textBox.setEnabled(false);
      textBox.setRows(5);
      textBox.setColumns(50);
      textBox.setFlexible(true);
      textBox.setStringValue(comment.getText());
      textPanel.addChild(textBox);
      // Remove button panel
      buttonPanel = new XComponent(XComponent.PANEL);
      buttonPanel.setLayout("flow");
      buttonPanel.setDirection(XComponent.WEST);
      textPanel.addChild(buttonPanel);
      removeButton = new XComponent(XComponent.BUTTON);
      removeButton.setID("RemoveCommentButton_" + count);
      removeButton.setStringValue(OpLocator.locatorString(comment));
      removeButton.setIcon(REMOVE_COMMENT_ICON);
      removeButton.setStyle(XComponent.DEFAULT_ICON_BUTTON_STYLE);
      removeButton.setOnButtonPressed(REMOVE_COMMENT_ACTION);
      removeButton.setEnabled(enableCommentRemoving);
      removeButton.setStyle(REMOVE_COMMENT_BUTTON_STYLE_REF);
      buttonPanel.addChild(removeButton);
      return commentPanel;
   }

}