/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.neckermann.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivityCategory;
import onepoint.project.team.modules.project_planning.components.OpChartComponent;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

/**
 * Form provider for category peaks (Neckermann-Demo)
 * @author gmesaric
 */
public class OpCategoryPeaksFormProvider implements XFormProvider {
   
   public final static String CATEGORY_PEAKS_SET = "CategoryPeaksSet";
   public final static String CATEGORY_PEAKS_BOX = "CategoryPeaksBox";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      OpProjectSession session = (OpProjectSession)s;

      OpBroker broker = session.newBroker();
      
      // Get all *active* categories
      OpQuery query = broker.newQuery("select category from OpActivityCategory as category where category.Active = true");
      Iterator result = broker.iterate(query);
      HashMap categoryMap = new HashMap();
      OpActivityCategory category = null;
      int categoryCount = 0;
      while (result.hasNext()) {
         category = (OpActivityCategory) result.next();
         categoryMap.put(new Long(category.getID()), category);
         categoryCount++;
      }

      if (categoryCount == 0)
         return; // Nothing to be shown
      
      // Get minimum start and maximum finish dates
      query = broker.newQuery("select min(activity.Start), max(activity.Finish) from OpActivity as activity where activity.Template = false and activity.Deleted = false order by activity.Start");
      result = broker.iterate(query);
      if (!result.hasNext())
         return; // Nothing to be shown
      Object[] record = (Object[]) result.next();
      if ((record[0] == null) || (record[1] == null))
         return; // Nothing to be shown
      
      // Number of time-units is always rounded "up"
      XCalendar calendar = XCalendar.getDefaultCalendar();
      Date minStart = calendar.workWeekStart((Date) record[0]);
      Date maxFinish = (Date) record[1];
      int numTimeUnits = (int) ((maxFinish.getTime() + XCalendar.MILLIS_PER_WEEK - XCalendar.MILLIS_PER_DAY - minStart.getTime()) / XCalendar.MILLIS_PER_WEEK);

      // Pre-init data-set (category * num-time-units "matrix")
      XComponent dataSet = form.findComponent(CATEGORY_PEAKS_SET);
      HashMap categoryRowMap = new HashMap();
      Iterator categories = categoryMap.values().iterator();
      int j = 0;
      XComponent dataRow = null;
      XComponent dataCell = null;
      for (int i = 0; i < categoryCount; i++) {
         category = (OpActivityCategory) categories.next();
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataSet.addChild(dataRow);
         categoryRowMap.put(new Long(category.getID()), dataRow);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(category.getName());
         dataRow.addChild(dataCell);
         for (j = 0; j < numTimeUnits; j++) {
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setIntValue(0);
            dataRow.addChild(dataCell);
         }
      }
      
      // TODO: Create time-unit-captions
      // TODO: Should be an array-list
      List timeUnitCaptions = new ArrayList();
      // *** Get week number and year from calendar
      // ==> Get for each caption separately (different years might have different exact week counts)
      Date weekDate = null;
      long timeOffset = minStart.getTime();
      int week = 0;
      int year = 0;
      StringBuffer captionBuffer = null;
      Calendar jCalendar = Calendar.getInstance();
      for (j = 0; j < numTimeUnits; j++) {
         weekDate = new Date(timeOffset);
         captionBuffer = new StringBuffer();
         jCalendar.setTime(weekDate);
         year = jCalendar.get(Calendar.YEAR) % 100;
         if (year < 10)
            captionBuffer.append('0');
         captionBuffer.append(year);
         week = jCalendar.get(Calendar.WEEK_OF_YEAR);
         if (week < 10)
            captionBuffer.append(" KW 0");
         else
            captionBuffer.append(" KW ");
         captionBuffer.append(week);
         timeUnitCaptions.add(captionBuffer.toString());
         timeOffset += XCalendar.MILLIS_PER_WEEK;
      }

      // Iterate activity start, finish dates and category IDs
      query = broker.newQuery("select activity.Start, activity.Finish, activity.Category.ID from OpActivity as activity where activity.Template = false and activity.Deleted = false order by activity.Start");
      result = broker.iterate(query);
      Date start = null;
      Date finish = null;
      Long categoryId = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         start = (Date) record[0];
         finish = (Date) record[1];
         categoryId = (Long) record[2];
         
         if (categoryId != null) {
            
            dataRow = (XComponent) categoryRowMap.get(categoryId);
            if (dataRow != null) {
         
               int firstUnit = (int) ((start.getTime() - minStart.getTime()) / XCalendar.MILLIS_PER_WEEK);
               int lastUnit = (int) ((finish.getTime() - minStart.getTime()) / XCalendar.MILLIS_PER_WEEK);
               for (j = firstUnit; j <= lastUnit; j++) {
                  dataCell = (XComponent) dataRow.getChild(j);
                  dataCell.setIntValue(dataCell.getIntValue() + 1);
               }
            
            }

         }

      }
      
      OpChartComponent categoryPeaksBox = (OpChartComponent) (form.findComponent(CATEGORY_PEAKS_BOX));
      categoryPeaksBox.setDataColumnCount(numTimeUnits);
      categoryPeaksBox.setValueCaptions(timeUnitCaptions);

      broker.close();
      
   }

}
