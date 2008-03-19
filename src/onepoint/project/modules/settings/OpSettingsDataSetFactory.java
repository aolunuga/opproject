/*
 * Copyright(c) Onepoint Software GmbH 2008. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocaleMap;

import java.util.*;

/**
 * Data set factory class for settings module.
 *
 * @author mihai.costin
 */
public class OpSettingsDataSetFactory {

   /**
    * Fills the given language dataset component with the available languages.
    *
    * @param languageDataSet a <code>XComponent(DATA_SET)</code> representing a dataset that will hold the user languages.
    * @param languageField   a <code>XComponent(CHOICE_FIELD)</code> representing the choice field where the user selected language
    *                        will be displayed. This may be <code>null</code>.
    * @param userXLocaleId   a <code>String</code> representing current user Xlocale id.
    */
   public static void fillLanguageDataSet(XComponent languageDataSet, XComponent languageField, String userXLocaleId) {

      // get all locales defined in the application
      XLocaleMap localeMap = XLocaleManager.getLocaleMap();
      Map<String, String> localeChoices = new HashMap<String, String>();
      for (Iterator localeIt = localeMap.keyIterator(); localeIt.hasNext();) {
         String localeId = (String) localeIt.next();
         XLocale xLocale = localeMap.getLocale(localeId);
         String caption;
         if (xLocale.getName() == null) {
            Locale locale = xLocale.getLocale();
            caption = locale.getDisplayLanguage(locale);
         }
         else {
            caption = xLocale.getName();
         }
         localeChoices.put(caption, XValidator.choice(localeId, caption));
      }

      // sort languages according to the current locale
      List<String> localeNames = new ArrayList<String>(localeChoices.keySet());
      Collections.sort(localeNames);

      int selectedIndex = 0;
      XComponent dataRow;
      // add all the languages to the data set and select the current locale
      for (int i = 0; i < localeNames.size(); i++) {
         String name = localeNames.get(i);
         String localeChoice = localeChoices.get(name);
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(localeChoice);
         if (userXLocaleId.equals(XValidator.choiceID(localeChoice))) {
            dataRow.setSelected(true);
            selectedIndex = i;
         }
         languageDataSet.addChild(dataRow);
      }

      //set up the selected index on choice field based on data row selection index
      languageField.setSelectedIndex(selectedIndex);
   }
}
