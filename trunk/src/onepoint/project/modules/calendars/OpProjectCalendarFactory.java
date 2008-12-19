package onepoint.project.modules.calendars;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter.DEFAULT;

import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpProjectCalendar;

public class OpProjectCalendarFactory {
   
   private static OpProjectCalendarFactory instance = null;
   
   private static final Object mutex = new Object();
   private static final Object DEFAULT_CALENDAR = new Object();
   
   // Maps any Locator to OpProkjectCalendar:
   private OpProjectCalendar defaultCalendar = null;
   
   public OpProjectCalendarFactory() {
   }
   
   static public OpProjectCalendarFactory getInstance() {
      synchronized (mutex) {
         return instance;
      }
   }
   
   static public void register(OpProjectCalendarFactory instance) {
      synchronized (mutex) {
         OpProjectCalendarFactory.instance = instance;
      }
   }
   
   protected void resetDefaultCalendar(OpProjectSession session) {
      defaultCalendar = null;
   }
   
   public void resetCalendars(OpProjectSession session) {
      resetDefaultCalendar(session);
   }
   
   public void resetCalendar(String locator) {
   }

   public OpProjectCalendar getDefaultCalendar(OpProjectSession session) {
      synchronized (DEFAULT_CALENDAR) {
      if (defaultCalendar == null) {
         defaultCalendar = OpProjectCalendar.getDefaultProjectCalendar();
         defaultCalendar.configure(session.getCalendar());
      }
      return defaultCalendar;
      }
   }
   
   public OpProjectCalendar getCalendar(OpProjectSession session, OpBroker broker, OpHasWorkCalendar priority1) {
      return getCalendar(session, broker, priority1, null);
   }
   
   public OpProjectCalendar getCalendar(OpProjectSession session, OpBroker broker, String priority1) {
      return getCalendar(session, broker, priority1, null);
   }
   
   public OpProjectCalendar getCalendar(OpProjectSession session, OpBroker broker, OpHasWorkCalendar priority1, OpHasWorkCalendar priority2) {
      return getDefaultCalendar(session);
   }

   public OpProjectCalendar getCalendar(OpProjectSession session,
         OpBroker broker, String priority1, String priority2) {
      return getDefaultCalendar(session);
   }
   
}
