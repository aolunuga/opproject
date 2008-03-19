/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.modules.user.OpPreference;
import onepoint.resource.XLocaleManager;

import java.util.Iterator;

public class OpSettingsModule extends OpModule {

   private static final XLog logger = XLogFactory.getServerLogger(OpSettingsModule.class);
   public static final String OLD_LOCALE_SETTING = "en";


   public void start(OpProjectSession session) {
      // Load settings
      OpSettingsService.getService().loadSettings(session);
   }

   /**
    * All the settings containing the "en" locale key will be changed to the new default locale .
    *
    * @param session Current session
    */
   public void upgradeToVersion55(OpProjectSession session) {

      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select setting from OpSetting as setting");
         Iterator result = broker.iterate(query);
         OpSetting setting;
         OpTransaction transaction = broker.newTransaction();
         String newLocaleId = XLocaleManager.getDefaultLocale().getID();
         while (result.hasNext()) {
            setting = (OpSetting) result.next();
            if (OpSettings.USER_LOCALE_ID.equals(setting.getName())) {
               if (OLD_LOCALE_SETTING.equals(setting.getValue())) {
                  logger.info("Changed setting value from " + OLD_LOCALE_SETTING + " to " + newLocaleId);
                  setting.setValue(newLocaleId);
                  broker.updateObject(setting);
               }
            }
         }

         //update also the preferences
         query = broker.newQuery("select preference from OpPreference as preference");
         result = broker.iterate(query);
         OpPreference preference;
         while (result.hasNext()) {
            preference = (OpPreference) result.next();
            if (OpPreference.LOCALE_ID.equals(preference.getName())) {
               logger.info("Changed preference value from " + OLD_LOCALE_SETTING + " to " + newLocaleId);
               preference.setValue(newLocaleId);
               broker.updateObject(preference);
            }
         }

         transaction.commit();

         OpSettings settings = OpSettingsService.getService().getSettings(session.getSourceName());
         if (settings != null) {
            String settingsLocale = settings.get(OpSettings.USER_LOCALE_ID);
            if (OLD_LOCALE_SETTING.equals(settingsLocale)) {
               settings.put(OpSettings.USER_LOCALE_ID, newLocaleId);
            }
         }

      }
      finally {
         broker.closeAndEvict();
      }
   }

}
