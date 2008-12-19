/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings.holiday_calendar;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

/**
 * Class that handles <code>holiday-calendar</code> nodes parsing
 *
 * @author ovidiu.lupas
 */
public class OpHolidayCalendarHandler implements XNodeHandler {
   public final static String YEAR = "year";

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpHolidayCalendarMember member = new OpHolidayCalendarMember();
      try {
         String year = (String) attributes.get(YEAR);
         if (year == null) {
            throw new OpHolidayCalendarException("The year attribute is missing");
         }
         member.year = Integer.parseInt(year);
      }
      catch (NumberFormatException e) {
         new OpHolidayCalendarException("The year attribute is not accurate", e);
      }
      return member;
   }

   /**
    * @see XNodeHandler#addNodeContent(onepoint.xml.XContext, Object, String)
    */
   public void addNodeContent(XContext context, Object node, String content) {
   }

   /**
    * @see XNodeHandler#addChildNode(onepoint.xml.XContext, Object, String, Object)
    */
   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext, String, Object, Object)
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      /*add holiday dates for this node to the manager */
      OpHolidayCalendarMember member = (OpHolidayCalendarMember) node;
      OpHolidayCalendar holidayCalendar = (OpHolidayCalendar) parent;
      holidayCalendar.addHolidays(member.getHolidayDates());
   }
}
