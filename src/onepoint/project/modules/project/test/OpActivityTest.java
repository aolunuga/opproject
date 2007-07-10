/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.test.OpTestCase;

import java.sql.Date;
import java.util.List;

/**
 * Test class for OpActivity objects
 *
 * @author florin.haizea
 */
public class OpActivityTest extends OpTestCase {

   public void testGetStartEndDates()
        throws Exception {

      //create the project plan with start - end dates : 5/1/2007 - 5/30/2007
      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 1).getTimeInMillis()));
      projectPlan.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 30).getTimeInMillis()));

       //create the project node with start - end dates : 5/3/2007 - 5/28/2007
      OpProjectNode projectNode = new OpProjectNode();
      projectNode.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 3).getTimeInMillis()));
      projectNode.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 30).getTimeInMillis()));
      projectPlan.setProjectNode(projectNode);

      //a standard activity with start - end dates : 5/10/2007 - 5/15/2007
      OpActivity standardActivity = new OpActivity();
      standardActivity.setType(OpActivity.STANDARD);
      standardActivity.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 10).getTimeInMillis()));
      standardActivity.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 15).getTimeInMillis()));
      standardActivity.setProjectPlan(projectPlan);

      //a task activity with start - end dates : 5/15/2007 - 5/20/2007
      OpActivity taskActivity = new OpActivity();
      taskActivity.setType(OpActivity.TASK);
      taskActivity.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 15).getTimeInMillis()));
      taskActivity.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 20).getTimeInMillis()));
      taskActivity.setProjectPlan(projectPlan);

       //an ad hoc task activity with start - end dates : 5/13/2007 - 5/15/2007
      OpActivity adHocActivity = new OpActivity();
      adHocActivity.setType(OpActivity.ADHOC_TASK);
      adHocActivity.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 13).getTimeInMillis()));
      adHocActivity.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 15).getTimeInMillis()));
      adHocActivity.setProjectPlan(projectPlan);

      //a milestone activity with start - end dates : 5/17/2007 - 5/18/2007
      OpActivity milestoneActivity = new OpActivity();
      milestoneActivity.setType(OpActivity.MILESTONE);
      milestoneActivity.setStart(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 17).getTimeInMillis()));
      milestoneActivity.setFinish(new Date(OpTestCase.getCalendarWithExactDaySet(2007, 5, 18).getTimeInMillis()));
      milestoneActivity.setProjectPlan(projectPlan);

      List<Date> startEndList = standardActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(standardActivity.getStart(), startEndList.get(OpActivity.START_DATE_LIST_INDEX));
      assertEquals(standardActivity.getFinish(), startEndList.get(OpActivity.END_DATE_LIST_INDEX));

      startEndList = null;
      startEndList = taskActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(taskActivity.getStart(), startEndList.get(OpActivity.START_DATE_LIST_INDEX));
      assertEquals(taskActivity.getFinish(), startEndList.get(OpActivity.END_DATE_LIST_INDEX));

      //the end date of the task activity is null
      taskActivity.setFinish(null);
      startEndList = null;
      startEndList = taskActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(taskActivity.getStart(), startEndList.get(OpActivity.START_DATE_LIST_INDEX));
      assertEquals(projectNode.getFinish(), startEndList.get(OpActivity.END_DATE_LIST_INDEX));

      startEndList = null;
      startEndList = adHocActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(adHocActivity.getStart(), startEndList.get(OpActivity.START_DATE_LIST_INDEX));
      assertEquals(adHocActivity.getFinish(), startEndList.get(OpActivity.END_DATE_LIST_INDEX));

      //the end date of the ad hoc activity is null
      adHocActivity.setFinish(null);
      startEndList = null;
      startEndList = adHocActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(adHocActivity.getStart(), startEndList.get(OpActivity.START_DATE_LIST_INDEX));
      assertEquals(projectNode.getFinish(), startEndList.get(OpActivity.END_DATE_LIST_INDEX));

      //the start date of the ad hoc activity is null
      adHocActivity.setStart(null);
      startEndList = null;
      startEndList = adHocActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(projectNode.getStart(), startEndList.get(OpActivity.START_DATE_LIST_INDEX));
      assertEquals(projectNode.getFinish(), startEndList.get(OpActivity.END_DATE_LIST_INDEX));

      startEndList = null;
      startEndList = milestoneActivity.getStartEndDateByType();
      assertNull(startEndList);

      //the project's node end date is null
      projectNode.setFinish(null);

      startEndList = null;
      startEndList = taskActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(taskActivity.getStart(), startEndList.get(OpActivity.START_DATE_LIST_INDEX));
      assertEquals(projectPlan.getFinish(), startEndList.get(OpActivity.END_DATE_LIST_INDEX));

      startEndList = null;
      startEndList = adHocActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(projectNode.getStart(), startEndList.get(OpActivity.START_DATE_LIST_INDEX));
      assertEquals(projectPlan.getFinish(), startEndList.get(OpActivity.END_DATE_LIST_INDEX));
   }

   public void testCalculateActualCost()
        throws Exception {

      OpActivity activity = new OpActivity();
      activity.setActualPersonnelCosts(15.4d);
      activity.setActualTravelCosts(10d);
      activity.setActualMaterialCosts(20d);
      activity.setActualExternalCosts(3.8d);
      activity.setActualMiscellaneousCosts(2d);

      assertEquals(51.2d, activity.calculateActualCost(), 0.01);
   }

    public void testCalculateBaseCost()
        throws Exception {

      OpActivity activity = new OpActivity();
      activity.setBasePersonnelCosts(15.4d);
      activity.setBaseTravelCosts(10d);
      activity.setBaseMaterialCosts(20d);
      activity.setBaseExternalCosts(3.8d);
      activity.setBaseMiscellaneousCosts(2d);

      assertEquals(51.2d, activity.calculateBaseCost(), 0.01);
   }
}
