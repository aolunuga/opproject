package onepoint.project.modules.calendars;

import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;

public class OpCalendarsModule extends OpModule {

   @Override
   public void start(OpProjectSession session) {
      // TODO Auto-generated method stub
      super.start(session);
      OpProjectCalendarFactory.getInstance().resetCalendars(session);
   }

}
