/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.settings.holiday_calendar;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;

/**
 * Class that handles the parsing of <code>holiday</code> nodes
 *
 * @author ovidiu.lupas
 */
public class OpHolidayHandler implements XNodeHandler {

   public final static String NAME = "name";
   public final static String DATE = "date";

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpHolidayMember member = new OpHolidayMember();
      member.date = (String)attributes.get(DATE);
      member.name = (String)attributes.get(NAME);
      return member;
   }

   public void addNodeContent(XContext context, Object node, String content) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      OpHolidayCalendarMember holidayCalendar = (OpHolidayCalendarMember) parent;
      holidayCalendar.addHoliday(((OpHolidayMember)node).date);
   }
}
