package onepoint.project;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendar;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendarManager;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpSubject;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

public abstract class OpProjectFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getLogger(OpProjectFormProvider.class);

   private static final String SELECT_CALENDAR = "${SelectCalendar}";
   
   private static final Map<String, Byte> ACCESSLEVEL_MAP = new HashMap<String, Byte>() {
      {
         put("administrators", new Byte(OpPermission.ADMINISTRATOR));
         put("managers", new Byte(OpPermission.MANAGER));
         put("observers", new Byte(OpPermission.OBSERVER));
      }
   };
   
   private final static String ADMINISTRATOR_STRING = "administrator";
   private final static String EVERYONE_STRING = "everyone";
   private final static String USER_STRING = "user";

   private static final String CALENDAR_CHOOSER_RESOURCES = "calendar.chooser";
   
   /**
    * setup a Set<OpPermission> described by the data set in descriptionMapDataSet
    * @param session
    * @param broker
    * @param descriptionMapDataSet
    * @param user
    * @return
    */
   public static Set<OpPermission> createDefaultPermissions(OpProjectSession session, OpBroker broker, XComponent descriptionMapDataSet, OpUser user) {
      Set<OpPermission> permissions = new HashSet<OpPermission>();
      Map<OpSubject, Byte> permissionsMap = new HashMap<OpSubject, Byte>();
      
      for (int i = 0; i < descriptionMapDataSet.getChildCount(); i++) {
         XComponent mappingsRow = ((XComponent)descriptionMapDataSet.getChild(i));
         Byte level = ACCESSLEVEL_MAP.get(mappingsRow.getStringValue());
         for (int j = 0; j < mappingsRow.getChildCount(); j++) {
            String userString = ((XComponent)mappingsRow.getChild(j)).getStringValue();
            OpSubject s = null;
            if (ADMINISTRATOR_STRING.equals(userString)) {
               s = session.administrator(broker);
            }
            else if (EVERYONE_STRING.equals(userString)) {
               s = session.everyone(broker);
            }
            else if (USER_STRING.equals(userString)) {
               s = user;
            }
            if (level != null && s != null) {
               Byte current = permissionsMap.get(s);
               if (current == null || level.compareTo(current) > 0) {
                  permissionsMap.remove(s);
                  permissionsMap.put(s, level);
               }
            }
         }
      }
      Iterator<OpSubject> sit = permissionsMap.keySet().iterator();
      while (sit.hasNext()) {
         OpSubject s = sit.next();
         OpPermission p = new OpPermission(null, s, permissionsMap.get(s).byteValue());
         permissions.add(p);
      }
      return permissions;
   }
   
  
   /**
    * setup calendar chooser
    * @param calendarChooser
    * @param selectedItem
    * @param localizer
    */
   public static void setupHolidayCalendarChooser(OpProjectSession session, XComponent calendarChooser,
         String selectedItem) {
      XLocalizer localizer = new XLocalizer();
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), CALENDAR_CHOOSER_RESOURCES);
      localizer.setResourceMap(resourceMap);

      XComponent holidaysDataSet = calendarChooser.getDataSetComponent();
      setupHolidayCalendarChooserDataSet(session, holidaysDataSet, null);
      if (holidaysDataSet.getChildCount() > 0) {
         XValidator.initChoiceField(calendarChooser, selectedItem);
      } else {
         calendarChooser.setEnabled(false);
      }
   }

   public static String getHolidayCalendarChoice(OpProjectSession session, XComponent holidaysDataSet,
         String selectedItem) {
      
      setupHolidayCalendarChooserDataSet(session, holidaysDataSet, null);
      return XValidator.initChoiceValue(holidaysDataSet, selectedItem);
   }

   public static void setupHolidayCalendarChooserDataSet(OpProjectSession session, XComponent holidaysDataSet, String defaultCalendarText) {
      XLocalizer localizer = new XLocalizer();
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), CALENDAR_CHOOSER_RESOURCES);
      localizer.setResourceMap(resourceMap);

      Map holidayMap = OpHolidayCalendarManager.getHolidayCalendarsMap();
      if (holidayMap != null && !holidayMap.isEmpty()) {
         Set keys = holidayMap.keySet();

         SortedMap<String, OpHolidayCalendar> calendarMap = new TreeMap<String, OpHolidayCalendar>();
         for (Object key : keys) {
            String id = (String) key;
            OpHolidayCalendar cal = (OpHolidayCalendar) holidayMap.get(id);
            calendarMap.put(cal.getLabel(), cal);
         }

         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(XValidator.choice(
               OpSettings.CALENDAR_HOLIDAYS_LOCATION_DEFAULT,
               defaultCalendarText != null ? defaultCalendarText : localizer
                     .localize(SELECT_CALENDAR)));
         holidaysDataSet.addChild(row);
         for (OpHolidayCalendar c : calendarMap.values()) {
            row = new XComponent(XComponent.DATA_ROW);
            row
                  .setStringValue(XValidator.choice(c.getLocation(), c
                        .getLabel()));
            holidaysDataSet.addChild(row);
         }
      }
   }


   public abstract void prepareForm(XSession session, XComponent form, HashMap parameters);

}
