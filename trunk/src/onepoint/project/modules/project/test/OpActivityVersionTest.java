/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.test.OpBaseTestCase;

import java.sql.Date;
import java.util.Calendar;
import java.util.List;

/**
 * Test class for OpActivityVersion objects
 *
 * @author florin.haizea
 */
public class OpActivityVersionTest extends OpBaseTestCase {

   public void testGetStartEndDates()
        throws Exception {

      Calendar calendar = Calendar.getInstance();
      calendar.set(2007,5,1,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);

      //create the project plan version with start - end dates : 5/1/2007 - 5/30/2007
      OpProjectPlanVersion planVersion = new OpProjectPlanVersion();
      planVersion.setStart(new Date(calendar.getTimeInMillis()));

      //create the project plan with start - end dates : 5/1/2007 - 5/30/2007
      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setStart(new Date(calendar.getTimeInMillis()));

      calendar.set(2007, 5, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      projectPlan.setFinish(new Date(calendar.getTimeInMillis()));
      planVersion.setFinish(new Date(calendar.getTimeInMillis()));

      planVersion.setProjectPlan(projectPlan);

       //create the project node with start - end dates : 5/3/2007 - 5/28/2007
      OpProjectNode projectNode = new OpProjectNode();
      calendar.set(2007,5,3,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);

      projectNode.setStart(new Date(calendar.getTimeInMillis()));

      calendar.set(2007, 28, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      projectNode.setFinish(new Date(calendar.getTimeInMillis()));
      projectPlan.setProjectNode(projectNode);

      calendar.set(2007,5,10,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);

      //a standard activity with start - end dates : 5/10/2007 - 5/15/2007
      OpActivityVersion standardActivity = new OpActivityVersion();
      standardActivity.setType(OpActivityVersion.STANDARD);
      standardActivity.setStart(new Date(calendar.getTimeInMillis()));

      calendar.set(2007,5,15,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      standardActivity.setFinish(new Date(calendar.getTimeInMillis()));
      standardActivity.setPlanVersion(planVersion);

      calendar.set(2007,5,15,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);

      //a task activity with start - end dates : 5/15/2007 - 5/20/2007
      OpActivityVersion taskActivity = new OpActivityVersion();
      taskActivity.setType(OpActivityVersion.TASK);
      taskActivity.setStart(new Date(calendar.getTimeInMillis()));

      calendar.set(2007,5,20,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      taskActivity.setFinish(new Date(calendar.getTimeInMillis()));
      taskActivity.setPlanVersion(planVersion);

      calendar.set(2007,5,13,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);

      //a milestone activity with start - end dates : 5/17/2007 - 5/18/2007
      OpActivityVersion milestoneActivity = new OpActivityVersion();
      milestoneActivity.setType(OpActivityVersion.MILESTONE);
      milestoneActivity.setStart(new Date(calendar.getTimeInMillis()));

      calendar.set(2007,5,18,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      milestoneActivity.setFinish(new Date(calendar.getTimeInMillis()));
      milestoneActivity.setPlanVersion(planVersion);

      List<Date> startEndList = standardActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(standardActivity.getStart(), startEndList.get(OpActivityVersion.START_DATE_LIST_INDEX));
      assertEquals(standardActivity.getFinish(), startEndList.get(OpActivityVersion.END_DATE_LIST_INDEX));

      startEndList = null;
      startEndList = taskActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(taskActivity.getStart(), startEndList.get(OpActivityVersion.START_DATE_LIST_INDEX));
      assertEquals(taskActivity.getFinish(), startEndList.get(OpActivityVersion.END_DATE_LIST_INDEX));

      //the end date of the task activity is null
      taskActivity.setFinish(null);
      startEndList = null;
      startEndList = taskActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(taskActivity.getStart(), startEndList.get(OpActivityVersion.START_DATE_LIST_INDEX));
      assertEquals(projectNode.getFinish(), startEndList.get(OpActivityVersion.END_DATE_LIST_INDEX));

      startEndList = null;
      startEndList = milestoneActivity.getStartEndDateByType();
      assertNull(startEndList);

      //the project's node end date is null
      projectNode.setFinish(null);

      startEndList = null;
      startEndList = taskActivity.getStartEndDateByType();
      assertNotNull(startEndList);
      assertEquals(taskActivity.getStart(), startEndList.get(OpActivityVersion.START_DATE_LIST_INDEX));
      assertEquals(projectPlan.getFinish(), startEndList.get(OpActivityVersion.END_DATE_LIST_INDEX));
   }
}
