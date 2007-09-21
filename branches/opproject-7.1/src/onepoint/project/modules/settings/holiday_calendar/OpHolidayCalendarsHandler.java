/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings.holiday_calendar;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

/**
 * Class that handles the holiday calendars root node
 * @author ovidiu.lupas
 */
public class OpHolidayCalendarsHandler implements XNodeHandler {

   public final static String LABEL = "label";
   public final static String LOCATION = "location";

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpHolidayCalendar holidayCalendar = new OpHolidayCalendar();
      String location = (String) attributes.get(LOCATION);
      holidayCalendar.setLocation(location);
      String label = (String) attributes.get(LABEL);
      holidayCalendar.setLabel(label);
      OpHolidayCalendarManager.addHolidayCalendar(holidayCalendar);
      return holidayCalendar;
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

   }
}