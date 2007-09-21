/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings.holiday_calendar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author : mihai.costin
 */
public class OpHolidayCalendar {

   //list of holiday dates
   private List holidayDates = new ArrayList();

   //location of this holiday set
   private String location;

   //the label for this holiday set
   private String label;

   /**
    * Returns the list of holiday dates
    *
    * @return <code>List[java.sql.Date]</code>
    */
   public List getHolidayDates() {
      return holidayDates;
   }

   public void setLocation(String location) {
      this.location = location;
   }

   public String getLocation() {
      return location;
   }

   public String getLabel() {
      return label;
   }

   public void setLabel(String label) {
      this.label = label;
   }

   /**
    * Adds a collection of holidays dates
    *
    * @param holidays <code>Collection[java.sql.Date]</code> repreenting the holiday dates
    */
   public void addHolidays(Collection holidays) {
      holidayDates.addAll(holidays);
   }

}
