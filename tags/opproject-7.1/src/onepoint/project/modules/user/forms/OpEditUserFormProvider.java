/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.*;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpEditUserFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getServerLogger(OpEditUserFormProvider.class);

   public final static String ASSIGNED_GROUP_DATA_SET = "AssignedGroupDataSet";
   private final static String ADD_TO_GROUP = "AddToGroupButton";
   private final static String REMOVE_FROM_GROUP = "RemoveFromGroupButton";
   private final static String PASSWORD_RETYPED = "PasswordRetyped";
   public final static String USER_LEVEL_DATA_SET = "UserLevelDataSet";
   public final static String USER_LEVEL_FIELD = "UserLevel";
   public final static String MANAGER_LEVEL_CAPTION = "${ManagerLevel}";
   public final static String CONTRIBUTOR_LEVEL_CAPTION = "${ContributorLevel}";
   public final static String OBSERVER_CUSTOMER_LEVEL_CAPTION = "${ObserverCustomerLevel}";
   public final static String OBSERVER_LEVEL_CAPTION = "${ObserverLevel}";
   public final static String RESOURCE_MAP = "user.edit_user";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Find user in database
      logger.debug("OpEditUserFormProvider.prepareForm()");

      String id_string = (String) (parameters.get("user_id"));
      logger.debug("   uid  " + id_string);
      Boolean edit_mode = (Boolean) (parameters.get("edit_mode"));
      OpBroker broker = session.newBroker();

      OpUser user = (OpUser) (broker.getObject(id_string));
      OpContact contact = user.getContact();

      // Fill edit-user form with user data
      form.findComponent("UserID").setStringValue(id_string);
      form.findComponent("EditMode").setBooleanValue(edit_mode.booleanValue());
      XComponent firstName = form.findComponent(OpContact.FIRST_NAME);
      firstName.setStringValue(contact.getFirstName());
      XComponent lastName = form.findComponent(OpContact.LAST_NAME);
      lastName.setStringValue(contact.getLastName());
      XComponent login = form.findComponent(OpUser.NAME);
      login.setStringValue(user.getName());
      XComponent password = form.findComponent(OpUser.PASSWORD);
      //fill password field with a token
      password.setStringValue(OpUserService.PASSWORD_TOKEN);
      XComponent passwordConfirm = form.findComponent(PASSWORD_RETYPED);
      XComponent description = form.findComponent(OpUser.DESCRIPTION);
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));
      description.setStringValue(localizer.localize(user.getDescription()));

      // do not fill password field with data from db, because password is stored as the hash of the original password
      // so it would not make sense to display the hash of the password
      // password.setStringValue(user.getPassword());

      XComponent email = form.findComponent(OpContact.EMAIL);
      email.setStringValue(contact.getEMail());
      XComponent phone = form.findComponent(OpContact.PHONE);
      phone.setStringValue(contact.getPhone());
      XComponent mobile = form.findComponent(OpContact.MOBILE);
      mobile.setStringValue(contact.getMobile());
      XComponent fax = form.findComponent(OpContact.FAX);
      fax.setStringValue(contact.getFax());

      XComponent languageField = this.fillLanguageDataSet(form, session, broker, user);

      //fill the user level dataset
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), RESOURCE_MAP);
      localizer.setResourceMap(resourceMap);
      XComponent levelDataSet = form.findComponent(USER_LEVEL_DATA_SET);
      XComponent levelField = form.findComponent(USER_LEVEL_FIELD);
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      String caption = localizer.localize(MANAGER_LEVEL_CAPTION);
      dataRow.setValue(XValidator.choice(String.valueOf(OpUser.MANAGER_USER_LEVEL), caption));
      levelDataSet.addChild(dataRow);
      dataRow = new XComponent(XComponent.DATA_ROW);
      caption = localizer.localize(CONTRIBUTOR_LEVEL_CAPTION);
      dataRow.setValue(XValidator.choice(String.valueOf(OpUser.CONTRIBUTOR_USER_LEVEL), caption));
      levelDataSet.addChild(dataRow);
      dataRow = new XComponent(XComponent.DATA_ROW);
      caption = localizer.localize(OBSERVER_LEVEL_CAPTION);
      dataRow.setValue(XValidator.choice(String.valueOf(OpUser.OBSERVER_USER_LEVEL), caption));
      levelDataSet.addChild(dataRow);
      dataRow = new XComponent(XComponent.DATA_ROW);
      caption = localizer.localize(OBSERVER_CUSTOMER_LEVEL_CAPTION);
      dataRow.setValue(XValidator.choice(String.valueOf(OpUser.OBSERVER_CUSTOMER_USER_LEVEL), caption));
      levelDataSet.addChild(dataRow);
      byte level = user.getLevel().byteValue();
      switch (level) {
         case OpUser.MANAGER_USER_LEVEL:
            levelField.setSelectedIndex(new Integer(0));
            break;
         case OpUser.CONTRIBUTOR_USER_LEVEL:
            levelField.setSelectedIndex(new Integer(1));
            break;
         case OpUser.OBSERVER_USER_LEVEL:
            levelField.setSelectedIndex(new Integer(2));
            break;
         case OpUser.OBSERVER_CUSTOMER_USER_LEVEL:
            levelField.setSelectedIndex(new Integer(3));
            break;
      }

      if (!edit_mode.booleanValue()) {
         firstName.setEnabled(false);
         lastName.setEnabled(false);
         login.setEnabled(false);
         password.setEnabled(false);
         passwordConfirm.setEnabled(false);
         description.setEnabled(false);
         email.setEnabled(false);
         phone.setEnabled(false);
         mobile.setEnabled(false);
         fax.setEnabled(false);
         languageField.setEnabled(false);
         levelField.setEnabled(false);
         form.findComponent("Cancel").setVisible(false);
         String title = ((OpProjectSession) session).getLocale().getResourceMap("user.Info").getResource("InfoUser")
              .getText();
         form.setText(title);
      }
      else if (user.getID() == session.getAdministratorID()) {
         // Names of user administrator cannot be changed
         firstName.setEnabled(false);
         lastName.setEnabled(false);
         login.setEnabled(false);
         levelField.setEnabled(false);
      }

      XComponent assigned_group_data_set = form.findComponent(ASSIGNED_GROUP_DATA_SET);
      XLocalizer userObjectsLocalizer = XLocaleManager.createLocalizer(session.getLocale().getID(), OpPermissionSetFactory.USER_OBJECTS);
      Iterator assignments = user.getAssignments().iterator();
      OpUserAssignment assignment = null;
      OpGroup group = null;
      dataRow = null;
      while (assignments.hasNext()) {
         assignment = (OpUserAssignment) assignments.next();
         group = assignment.getGroup();
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(group.locator(), userObjectsLocalizer.localize(group.getDisplayName())));
         assigned_group_data_set.addChild(dataRow);
      }
      assigned_group_data_set.sort();

      if (!edit_mode.booleanValue()) {
         form.findComponent(ADD_TO_GROUP).setVisible(false);
         form.findComponent(ADD_TO_GROUP).setEnabled(false);
         form.findComponent(REMOVE_FROM_GROUP).setEnabled(false);
         form.findComponent(REMOVE_FROM_GROUP).setVisible(false);
      }
      broker.close();

   }

   /**
    * Fills the language dataset, selecting the user language in the combo, but displaying in the
    * language of the currently logged user.
    * @param form a <code>XComponent(FORM)</code> representing the current edit/info form.
    * @param session a <code>OpProjectSession</code> representing the server session.
    * @param broker a <code>OpBroker</code> used for persistence operations.
    * @param user a <code>OpUser</code> representing the user being edited.
    * @return a <code>XComponent(CHOICE_FIELD)</code> representing the language
    * choice field.  
    */
   private XComponent fillLanguageDataSet(XComponent form, OpProjectSession session, OpBroker broker, OpUser user) {
      XComponent languageField = form.findComponent("UserLanguage");
      XComponent languageDataSet = form.findComponent("UserLanguageDataSet");
      OpUser currentUser = session.user(broker);
      OpUserLanguageManager.fillLanguageDataSet(languageDataSet, languageField, currentUser);

      //get the user preferences. If the user has no preference, get the preference from system setings.
      String preference;
      if(user.getPreference(OpPreference.LOCALE) != null){
         preference = user.getPreference(OpPreference.LOCALE).getValue();
      }
      else{
         preference = OpSettings.get(OpSettings.USER_LOCALE);
      }
      
      for (int i =0 ; i  < languageDataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) languageDataSet.getChild(i);
         String localeChoice = dataRow.getStringValue();
         if (XValidator.choiceID(localeChoice).equalsIgnoreCase(preference)) {
            dataRow.setSelected(true);
            languageField.setSelectedIndex(i);
         }
         else {
            dataRow.setSelected(false);
         }
      }
      return languageField;
   }
}
