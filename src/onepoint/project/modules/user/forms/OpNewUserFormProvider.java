/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserLanguageManager;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider class for new user dialog.
 *
 * @author ovidiu.lupas
 */
public class OpNewUserFormProvider implements XFormProvider {

   /*form components */
   public final static String EVERYONE_GROUP_FIELD = "EveryoneGroupField";
   public final static String ASSIGNMENT_GROUP_DATA_SET = "AssignedGroupDataSet";
   public final static String USER_LANGUAGE_DATA_SET = "UserLanguageDataSet";
   public final static String USER_LANGUAGE_FIELD = "UserLanguage";
   public final static String USER_LEVEL_DATA_SET = "UserLevelDataSet";
   public final static String USER_LEVEL_FIELD = "UserLevel";
   public final static String MANAGER_LEVEL_CAPTION = "{$ManagerLevel}";
   public final static String STANDARD_LEVEL_CAPTION = "{$StandardLevel}";
   public final static String RESOURCE_MAP = "user.new_user";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      //set up the everyone group field
      XComponent everyoneField = form.findComponent(EVERYONE_GROUP_FIELD);
      //assume that everyone group always exists (becouse is a system -managed group that can't be deleted)
      XLocalizer userObjectsLocalizer = XLocaleManager.createLocalizer(session.getLocale().getID(), OpPermissionSetFactory.USER_OBJECTS);
      OpGroup everyone = session.everyone(broker);
      everyoneField.setStringValue(XValidator.choice(everyone.locator(), userObjectsLocalizer.localize(everyone.getDisplayName())));

      //make everyone the default user assignment group
      XComponent assignmentDataSet = form.findComponent(ASSIGNMENT_GROUP_DATA_SET);
      XComponent dataRow = assignmentDataSet.newDataRow();
      dataRow.setStringValue(everyoneField.getStringValue());
      assignmentDataSet.addDataRow(dataRow);

      //fill the language dataset
      XComponent languageDataSet = form.findComponent(USER_LANGUAGE_DATA_SET);
      XComponent languageDataField = form.findComponent(USER_LANGUAGE_FIELD);
      OpUser currentUser = session.user(broker);
      OpUserLanguageManager.fillLanguageDataSet(languageDataSet, languageDataField, currentUser);

      //fill the user level dataset
      XLocalizer localizer = new XLocalizer();
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), RESOURCE_MAP);
      localizer.setResourceMap(resourceMap);
      XComponent levelDataSet = form.findComponent(USER_LEVEL_DATA_SET);
      XComponent levelField = form.findComponent(USER_LEVEL_FIELD);
      dataRow = new XComponent(XComponent.DATA_ROW);
      String caption = localizer.localize(MANAGER_LEVEL_CAPTION);
      dataRow.setValue(XValidator.choice(String.valueOf(OpUser.MANAGER_USER_LEVEL), caption));
      levelDataSet.addChild(dataRow);
      dataRow = new XComponent(XComponent.DATA_ROW);
      caption = localizer.localize(STANDARD_LEVEL_CAPTION);
      dataRow.setValue(XValidator.choice(String.valueOf(OpUser.STANDARD_USER_LEVEL), caption));
      levelDataSet.addChild(dataRow);
      levelField.setSelectedIndex(new Integer(0));

      broker.close();
   }
}
