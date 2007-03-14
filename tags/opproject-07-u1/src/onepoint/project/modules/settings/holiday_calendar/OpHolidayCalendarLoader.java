/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.settings.holiday_calendar;

import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;

import java.io.InputStream;

/**
 * Class that loads holiday dates
 *
 * @author ovidiu.lupas
 */
public class OpHolidayCalendarLoader extends XLoader {

   public final static XSchema HOLIDAY_CALENDAR_SCHEMA = new OpHolidayCalendarSchema();

   /**
    * Creates a new holiday calendar loader
    */
   public OpHolidayCalendarLoader() {
      super(new XDocumentHandler(HOLIDAY_CALENDAR_SCHEMA));
      setUseResourceLoader(false);
   }

   /**
    * Loads the holidays from the given <code>inputStream</code>
    *
    * @param inputStream <code>InputStream</code> from which the holidays are read
    * @return <code>OpHolidayCalendarManager</code>
    */
   public OpHolidayCalendar loadHolidays(InputStream inputStream) {
      return (OpHolidayCalendar) (loadObject(inputStream, null));
   }
}
