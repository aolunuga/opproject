/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.components.test;

import java.io.InputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.XValidationException;
import onepoint.express.XView;
import onepoint.express.server.XFormSchema;
import onepoint.project.modules.project.components.OpActivityLoopException;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.test.OpTestCase;
import onepoint.project.util.OpProjectCalendar;
import onepoint.resource.XLocale;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;

/**
 * Test case for the XGantValidator class.
 *
 * @author horia.chiorean
 */
public class OpGanttValidatorTest extends OpTestCase {

   /**
    * The name of the xml file that contains the test data.
    */
   private static final String TEST_DATA_FILENAME = "gantValidatorTestData.xml";

   /**
    * The number of working days in a week.
    */
   private static final int WORKING_DAYS_IN_A_WEEK = 5;

   /**
    * The number of milliseconds in a day.
    */
   private static final long DAY_MILLIS = 24 * 60 * 60 * 1000;

   /**
    * The gant validator that is tested.
    */
   private OpGanttValidator validator = null;

   /**
    * Ids of special activities defined in the xml file.
    */
   private static final String COLLECTION_ID = "Collection";
   private static final String MILESTONE_ID = "Milestone";

   private String WORKER1_ID = "OpResource.16.xid";
   private final String WORKER1 = WORKER1_ID + "['Worker1']";
   private final String WORKER1_REGEXP = WORKER1_ID + "['Worker1 #']";
   private String WORKER2_ID = "OpResource.18.xid";
   private final String WORKER2 = WORKER2_ID + "['Worker2']";

   private final double DOUBLE_ERROR_MARGIN = Math.pow(10, -4);

   /**
    * @see junit.framework.TestCase#setUp()
    */
   public void setUp()
        throws Exception {
      super.setUp();

      //init the default calendar
      XDisplay display = new XDisplay(null);
      ((OpProjectCalendar)display.getCalendar()).configure(null, new XLocale(Locale.ENGLISH.getLanguage(), "de", ""), null, null);

      XComponent testDataSet = getTestDataSet(TEST_DATA_FILENAME);

      validator = getValidator();

      validator.setDataSet(testDataSet);
      XComponent assignmentSet = this.createAssignmentSet();
      validator.setAssignmentSet(assignmentSet);
      XComponent hourlyRates = this.createHourlyRates();
      validator.setHourlyRatesDataSet(hourlyRates);
   }

   /**
    * Loads a test data set from the given file.
    *
    * @param testDataFile a <code>xml</code> file containing a data set that will be used
    *                     for testing.
    * @return a <code>XComponent(DATA_SET)</code> containing test data for the validator.
    */
   public static XComponent getTestDataSet(String testDataFile) {
      InputStream testDataInputStream = OpGanttValidatorTest.class.getResourceAsStream(testDataFile);
      return getTestDataSet(testDataInputStream);
   }
   public static XComponent getTestDataSet(InputStream testDataInputStream) {
      XLoader xmlLoader = new XLoader(new XDocumentHandler(new XFormSchema()));
      XComponent testForm = (XComponent) xmlLoader.loadObject(testDataInputStream, null);
      //project start
      XComponent startField = new XComponent(XComponent.DATA_FIELD);
      startField.setID(OpGanttValidator.PROJECT_START);
      Calendar calendar = Calendar.getInstance(OpProjectCalendar.GMT_TIMEZONE);
      calendar.set(2000, 1, 1);
      startField.setDateValue(calendar.getTime());
      testForm.addChild(startField);
      XComponent projectSettings = createProjectSettings();
      testForm.addChild(projectSettings);

      //activity data set.
      XComponent testDataSet = (XComponent) testForm.getChild(0);
      //set Work records map
      for (int i = 0; i < testDataSet.getChildCount(); i++) {
         XComponent row = (XComponent) testDataSet.getChild(i);
         OpGanttValidator.setWorkRecords(row, new HashMap());
      }
      return testDataSet;
   }

   /**
    * Creates a data-set containing hourly rates values.
    *
    * @return a <code>XComponent(DATA_SET)</code>.
    */
   private XComponent createHourlyRates() {
      //hourly rates for WORKER1_ID and WORKER2_ID
      XComponent hourlyRates = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(WORKER1_ID);
      Map rates = new TreeMap();
      List<Double> ratesList = new ArrayList<Double>();
      ratesList.add(10.0);
      ratesList.add(20.0);
      rates.put(OpProjectCalendar.today(), ratesList);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(rates);
      dataRow.addChild(dataCell);
      hourlyRates.addChild(dataRow);
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(WORKER2_ID);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(rates);
      dataRow.addChild(dataCell);
      hourlyRates.addChild(dataRow);
      return hourlyRates;
   }

   /**
    * Creates a data-set simulating user assignements, to be used by the validator.
    *
    * @return a <code>XComponent(DATA_SET)</code> representing an assignment set.
    */
   private XComponent createAssignmentSet() {
      //assignment set
      XComponent assignmentSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(WORKER1);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(100);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(250);
      dataRow.addChild(dataCell);
      assignmentSet.addChild(dataRow);
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(WORKER2);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(100);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(250);
      dataRow.addChild(dataCell);
      assignmentSet.addChild(dataRow);
      return assignmentSet;
   }

   /**
    * Creates a client structure resembling project settings, that is to be used by the
    * validator.
    *
    * @return a <code>XComponent(DATA_SET)</code> containing the same data
    *         as the project settings.
    */
   private static XComponent createProjectSettings() {
      //project settings
      XComponent projectSettings = new XComponent(XComponent.DATA_SET);
      projectSettings.setID(OpGanttValidator.PROJECT_SETTINGS_DATA_SET);

      //calculation mode
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(OpGanttValidator.CALCULATION_MODE);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setByteValue((byte) 1);
      dataRow.addChild(dataCell);
      projectSettings.addChild(dataRow);
      //progress tracked
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(OpGanttValidator.PROGRESS_TRACKED);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(true);
      dataRow.addChild(dataCell);
      projectSettings.addChild(dataRow);

      //project template
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(OpGanttValidator.PROJECT_TEMPLATE);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(false);
      dataRow.addChild(dataCell);
      projectSettings.addChild(dataRow);

      return projectSettings;
   }

   protected OpGanttValidator getValidator() {
      return new OpGanttValidator();
   }

   private void setIndependentEffort() {
      validator.setCalculationMode(new Byte(OpGanttValidator.INDEPENDENT));
   }

   /**
    * Tests if the duration is updated corectly for a given activity.
    * The activity has no resources assigned to it.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateDuration()
        throws Exception {

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      double effort = OpGanttValidator.getBaseEffort(firstActivity);
      double duration = OpGanttValidator.getDuration(firstActivity);
      Date start = OpGanttValidator.getStart(firstActivity);
      Date end = OpGanttValidator.getEnd(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;
      Date newStart;
      long expectedTime;

      //change to the same duration => nothing should change
      validator.updateDuration(firstActivity, duration, true);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", duration, newDuration, 0);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The new start Date was not set correctly ", start, newStart);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", end, newEnd);

      //double duration, and no resource assigned  => effort==duration.
      validator.updateDuration(firstActivity, 2 * duration, true);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 2 * duration, newDuration, 0);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", newDuration, newEffort, 0);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The new start Date was not set correctly ", start, newStart);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

      //set duration to <0 => duration=0, effort=0, start=end
      validator.updateDuration(firstActivity, -100, true);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 0, newDuration, 0);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 0, newEffort, 0);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The new start Date was not set correctly ", start, newStart);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", start, newEnd);

   }


   /**
    * Tests if the duration is updated corectly for a given activity.
    * The activity has resources assigned to it.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateDurationWithResources()
        throws Exception {

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      double effort = OpGanttValidator.getBaseEffort(firstActivity);
      double duration = OpGanttValidator.getDuration(firstActivity);
      Date start = OpGanttValidator.getStart(firstActivity);
      Date end = OpGanttValidator.getEnd(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;
      Date newStart;
      long expectedTime;

      //no resources.
      //add a resource
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);
      validator.setDataCellValue(firstActivity, OpGanttValidator.RESOURCES_COLUMN_INDEX, resources);

      //change to the same duration => nothing should change
      validator.updateDuration(firstActivity, duration, true);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", duration, newDuration, 0);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The Start Date was modified ", start, newStart);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", end, newEnd);

      //double duration, and a resource assigned  => everything recalculated (effort also doubles)
      validator.updateDuration(firstActivity, 2 * duration, true);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The Start Date was modified ", start, newStart);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 2 * duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 2 * effort, newEffort, 0);

      //add a resource and double duration => effort should double 2 times
      resources.add(WORKER2);
      validator.setDataCellValue(firstActivity, OpGanttValidator.RESOURCES_COLUMN_INDEX, resources);

      validator.updateDuration(firstActivity, 4 * duration, true);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The Start Date was modified ", start, newStart);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 4 * duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 4 * effort, newEffort, 0);
   }

   /**
    * Tests if the duration is updated corectly for a given activity.
    * The activity has resources assigned to it.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateDurationWithResourcesIndependent()
        throws Exception {
      setIndependentEffort();

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      double effort = OpGanttValidator.getBaseEffort(firstActivity);
      double duration = OpGanttValidator.getDuration(firstActivity);
      Date start = OpGanttValidator.getStart(firstActivity);
      Date end = OpGanttValidator.getEnd(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;
      Date newStart;
      long expectedTime;

      //no resources.
      //add a resource
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);
      validator.setDataCellValue(firstActivity, OpGanttValidator.RESOURCES_COLUMN_INDEX, resources);

      //change to the same duration => nothing should change
      validator.updateDuration(firstActivity, duration, true);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The effort was changed ", effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", duration, newDuration, 0);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The Start Date was modified ", start, newStart);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", end, newEnd);

      //double duration, and a resource assigned  => everything recalculated (effort also doubles)
      validator.updateDuration(firstActivity, 2 * duration, true);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The Start Date was modified ", start, newStart);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 2 * duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The effort was changed ", effort, newEffort, 0);

      //add another resource
      resources.add(WORKER2);
      validator.setDataCellValue(firstActivity, OpGanttValidator.RESOURCES_COLUMN_INDEX, resources);

      validator.updateDuration(firstActivity, 4 * duration, true);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The Start Date was modified ", start, newStart);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 4 * duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The effort was changed ", effort, newEffort, 0);
   }


   /**
    * Tests if the effort is updated corectly for a given activity that has resources assigned to it.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateBaseEffortWithResources()
        throws Exception {

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      double effort = OpGanttValidator.getBaseEffort(firstActivity);
      double duration = OpGanttValidator.getDuration(firstActivity);
      Date start = OpGanttValidator.getStart(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;

      //no resources.
      //add a resource
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);
      validator.setDataCellValue(firstActivity, OpGanttValidator.RESOURCES_COLUMN_INDEX, resources);

      //same effort => the duration should stay the same
      validator.updateBaseEffort(firstActivity, effort);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      long expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

      //add another resource and double the effort => duration and end date the same
      resources.add(WORKER2);
      validator.setDataCellValue(firstActivity, OpGanttValidator.RESOURCES_COLUMN_INDEX, resources);

      validator.updateBaseEffort(firstActivity, 2 * effort);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 2 * effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was modified ", duration, 160, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

   }

   /**
    * Tests if the effort is updated corectly for a given activity that has resources assigned to it.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateBaseEffortWithResourcesIndependent()
        throws Exception {

      setIndependentEffort();

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      double effort = OpGanttValidator.getBaseEffort(firstActivity);
      double duration = OpGanttValidator.getDuration(firstActivity);
      Date start = OpGanttValidator.getStart(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;

      //no resources.
      //add a resource
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);
      validator.setDataCellValue(firstActivity, OpGanttValidator.RESOURCES_COLUMN_INDEX, resources);

      //same effort => the duration should stay the same
      validator.updateBaseEffort(firstActivity, effort);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      long expectedTime = getEndTimeForDuration(start, duration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

      //add another resource and double the effort => duration and end date the same
      resources.add(WORKER2);
      validator.setDataCellValue(firstActivity, OpGanttValidator.RESOURCES_COLUMN_INDEX, resources);

      validator.updateBaseEffort(firstActivity, 2 * effort);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 2 * effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The duration was changed ", duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      expectedTime = getEndTimeForDuration(start, duration);
      assertEquals("The end date was changed ", new Date(expectedTime), newEnd);
   }


   /**
    * Tests that the effort is updated corectly for a given activity.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateBaseEffort()
        throws Exception {

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      double effort = OpGanttValidator.getBaseEffort(firstActivity);
      double duration = OpGanttValidator.getDuration(firstActivity);
      Date start = OpGanttValidator.getStart(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;

      //change to the same effort. => nothing should change
      validator.updateBaseEffort(firstActivity, effort);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      long expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

      //double the effort. => the duration should double
      validator.updateBaseEffort(firstActivity, 2 * effort);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 2 * effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 2 * duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);
   }

   /**
    * Tests that the effort is updated corectly for a given activity.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateBaseEffortIndependent()
        throws Exception {

      setIndependentEffort();

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      double effort = OpGanttValidator.getBaseEffort(firstActivity);
      double duration = OpGanttValidator.getDuration(firstActivity);
      Date start = OpGanttValidator.getStart(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;

      //change to the same effort. => nothing should change
      validator.updateBaseEffort(firstActivity, effort);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      long expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

      //double the effort. => the duration stays the same
      validator.updateBaseEffort(firstActivity, 2 * effort);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 2 * effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      expectedTime = getEndTimeForDuration(start, duration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);
   }

   /**
    * Tests that the effort is updated corectly (at zero value) for a given activity.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateBaseEffortToZero()
        throws Exception {

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      Date start = OpGanttValidator.getStart(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;

      //set the effort to zero => duration will be zero, end=start
      validator.updateBaseEffort(firstActivity, 0);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 0, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 0, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", start, newEnd);

      //set the effort to < zero => duration will be zero, end=start
      validator.updateBaseEffort(firstActivity, -100);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 0, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 0, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", start, newEnd);

   }

   /**
    * Tests that the effort is updated corectly (at zero value) for a given activity.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateBaseEffortToZeroIndependent()
        throws Exception {

      setIndependentEffort();

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      Date end = OpGanttValidator.getEnd(firstActivity);
      double duration = OpGanttValidator.getDuration(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;

      //set the effort to zero => duration unchanged
      validator.updateBaseEffort(firstActivity, 0);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 0, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", end, newEnd);

      //set the effort to < zero => duration will be zero, end=start
      validator.updateBaseEffort(firstActivity, -100);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 0, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The duration was changed ", duration, newDuration, 0);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The end Date was modified ", end, newEnd);

   }

   /**
    * Tests that the end date is updated corectly for a given activity.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateFinish()
        throws Exception {

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      Date start = OpGanttValidator.getStart(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;

      //set the end date before start => end date = start date, effort=0, duration=0
      Date setEnd = new Date(start.getTime() - 1);
      validator.updateFinish(firstActivity, setEnd);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", start, newEnd);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 0, newDuration, 0);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", 0, newEffort, 0);

      //2 weeks
      setEnd = new Date(start.getTime() + 14 * OpProjectCalendar.MILLIS_PER_DAY);
      validator.updateFinish(firstActivity, setEnd);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", setEnd, newEnd);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      long expectedDuration = getDurationForPeriod(start, setEnd);
      assertEquals("The new duration was not set correctly ", expectedDuration, newDuration, 0);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", expectedDuration, newEffort, 0);

   }

   /**
    * Tests that the end date is updated corectly for a given activity.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateFinishIndependent()
        throws Exception {

      setIndependentEffort();

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      Date start = OpGanttValidator.getStart(firstActivity);
      double effort = OpGanttValidator.getBaseEffort(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;

      //set the end date before start => end date = start date, effort=0, duration=0
      Date setEnd = new Date(start.getTime() - 1);
      validator.updateFinish(firstActivity, setEnd);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", start, newEnd);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 0, newDuration, 0);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The effort was changed ", 0, newEffort, 0);

      //2 weeks
      setEnd = new Date(start.getTime() + 14 * OpProjectCalendar.MILLIS_PER_DAY);
      validator.updateFinish(firstActivity, setEnd);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", setEnd, newEnd);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      long expectedDuration = getDurationForPeriod(start, setEnd);
      assertEquals("The new duration was not set correctly ", expectedDuration, newDuration, 0);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The effort was changed ", 0, newEffort, 0);

   }


   /**
    * Tests that the end date is updated corectly for a given activity that has resources assigned to it.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateFinishWithResources()
        throws Exception {

      //second activity already has a resource assigned to it.
      XComponent secondActivity = (XComponent) validator.getDataSet().getChild(1);
      Date start = OpGanttValidator.getStart(secondActivity);
      double newEffort;
      double newDuration;
      Date newEnd;

      //set the end date before start => end date = start date, effort=0, duration=0
      Date setEnd = new Date(start.getTime() - 1);
      validator.updateFinish(secondActivity, setEnd);

      newEnd = OpGanttValidator.getEnd(secondActivity);
      assertEquals("The new end Date was not set correctly ", start, newEnd);

      newDuration = OpGanttValidator.getDuration(secondActivity);
      assertEquals("The new duration was not set correctly ", 0, newDuration, 0);

      newEffort = OpGanttValidator.getBaseEffort(secondActivity);
      assertEquals("The new effort was not set correctly ", 0, newEffort, 0);

      //start + 2 weeks
      setEnd = new Date(start.getTime() + 14 * OpProjectCalendar.MILLIS_PER_DAY);
      validator.updateFinish(secondActivity, setEnd);

      newEnd = OpGanttValidator.getEnd(secondActivity);
      assertEquals("The new end Date was not set correctly ", setEnd, newEnd);

      newDuration = OpGanttValidator.getDuration(secondActivity);
      long expectedDuration = getDurationForPeriod(start, setEnd);
      assertEquals("The new duration was not set correctly ", expectedDuration, newDuration, 0);

      newEffort = OpGanttValidator.getBaseEffort(secondActivity);
      assertEquals("The new effort was not set correctly ", expectedDuration, newEffort, 0);

   }

   /**
    * Tests that the end date is updated corectly for a given activity that has resources assigned to it.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateFinishWithResourcesIndependent()
        throws Exception {

      setIndependentEffort();

      //second activity already has a resource assigned to it.
      XComponent secondActivity = (XComponent) validator.getDataSet().getChild(1);
      Date start = OpGanttValidator.getStart(secondActivity);
      double effort = OpGanttValidator.getBaseEffort(secondActivity);
      double newDuration;
      Date newEnd;

      //set the end date before start => end date = start date, effort=unchanged, duration=0
      Date setEnd = new Date(start.getTime() - 1);
      validator.updateFinish(secondActivity, setEnd);

      newEnd = OpGanttValidator.getEnd(secondActivity);
      assertEquals("The new end Date was not set correctly ", start, newEnd);

      newDuration = OpGanttValidator.getDuration(secondActivity);
      assertEquals("The new duration was not set correctly ", 0, newDuration, 0);

      assertEquals("The effort was changed ", effort, 80, 0);

      //start + 2 weeks
      setEnd = new Date(start.getTime() + 14 * OpProjectCalendar.MILLIS_PER_DAY);
      validator.updateFinish(secondActivity, setEnd);

      newEnd = OpGanttValidator.getEnd(secondActivity);
      assertEquals("The new end Date was not set correctly ", setEnd, newEnd);

      newDuration = OpGanttValidator.getDuration(secondActivity);
      long expectedDuration = getDurationForPeriod(start, setEnd);
      assertEquals("The new duration was not set correctly ", expectedDuration, newDuration, 0);

      assertEquals("The effort was changed ", effort, 80, 0);
   }

   /**
    * Tests the updating of personnel cost when adding a resource to an activity
    *
    * @throws Exception
    */
   public void testUpdatePersonnelCostAddResource()
        throws Exception {
      OpProjectCalendar calendar = OpProjectCalendar.getDefaultProjectCalendar();
      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      double effort = OpGanttValidator.getBaseEffort(firstActivity);

      //do not modify anything
      validator.setDataCellValue(firstActivity, OpGanttValidator.BASE_EFFORT_COLUMN_INDEX, new Double(effort));
      assertEquals("The base personnel cost was not set correctly ", 0, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", 0, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);

      //add the first resource one to the first activity
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);
      validator.setDataCellValue(firstActivity, 26, resources);
      assertEquals("The base personnel cost was not set correctly ", 1600, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", 3200, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);

      //get the second activity and transform it into a task
      XComponent secondActivity = (XComponent) validator.getDataSet().getChild(1);

      validator.setDataCellValue(secondActivity, OpGanttValidator.START_COLUMN_INDEX, null);
      validator.setDataCellValue(secondActivity, OpGanttValidator.FINISH_COLUMN_INDEX, null);
      assertEquals("The base personnel cost was not set correctly ", 0, OpGanttValidator.getBasePersonnelCosts(secondActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", 0, OpGanttValidator.getBaseProceeds(secondActivity), DOUBLE_ERROR_MARGIN);

      //set the project's start to 6/10/2007, project's end to null, and the project plan's finish to 6/25/2007
      Date start = new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis());
      Date finish = new Date(getCalendarWithExactDaySet(2007, 6, 25).getTimeInMillis());

      validator.setProjectStart(start);
      validator.validateEntireDataSet();
      validator.setProjectFinish(null);
      validator.setProjectPlanFinish(finish);

      //set the resource of the second activity to WORKER1
      validator.setDataCellValue(secondActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", 800d, OpGanttValidator.getBasePersonnelCosts(secondActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", 1600d, OpGanttValidator.getBaseProceeds(secondActivity), DOUBLE_ERROR_MARGIN);

      //add an hourly rate period for resource 1 from 6/12/2007 - 6/16/2007
      addRatesToWorker1(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()), new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis()));

      int workDaysWithDefaultRate = calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 11).getTimeInMillis())).size();
      workDaysWithDefaultRate += calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 17).getTimeInMillis()),
           validator.getProjectPlanFinish()).size();
      int workDaysWithOtherRate = calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis())).size();
      double hoursPerDay = 80d / (double) (workDaysWithDefaultRate + workDaysWithOtherRate);

      //reassign the first resource to the task activity
      validator.setDataCellValue(secondActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", hoursPerDay * (workDaysWithDefaultRate * 10d + workDaysWithOtherRate * 3d),
           OpGanttValidator.getBasePersonnelCosts(secondActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", hoursPerDay * (workDaysWithDefaultRate * 20d + workDaysWithOtherRate * 4d),
           OpGanttValidator.getBaseProceeds(secondActivity), DOUBLE_ERROR_MARGIN);
   }

   /**
    * Tests the updating of personnel cost when adding two resources to an activity, both 100% and x% assigned
    *
    * @throws Exception
    */
   public void testUpdatePersonnelCostAddTwoResources()
        throws Exception {
      OpProjectCalendar calendar = OpProjectCalendar.getDefaultProjectCalendar();
      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);

      //set the activity's start to 6/10/2007 and it's finish to 6/25/2007
      Date start = new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis());
      Date finish = new Date(getCalendarWithExactDaySet(2007, 6, 25).getTimeInMillis());
      validator.setDataCellValue(firstActivity, OpGanttValidator.START_COLUMN_INDEX, start);
      validator.setDataCellValue(firstActivity, OpGanttValidator.FINISH_COLUMN_INDEX, finish);

      //add an hourly rate period for resource 1 from 6/12/2007 - 6/16/2007
      addRatesToWorker1(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()), new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis()));

      //add both resources to the first activity
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);
      resources.add(WORKER2);

      int workDaysWithDefaultRate = calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 11).getTimeInMillis())).size();
      workDaysWithDefaultRate += calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 17).getTimeInMillis()),
           validator.getProjectPlanFinish()).size();
      int workDaysWithOtherRate = calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis())).size();

      //set the effort of the activity to double the normal effort between 6/10/2007 and 6/20/2007 because we have two resources assigned 100% for this activity
      validator.setDataCellValue(firstActivity, OpGanttValidator.BASE_EFFORT_COLUMN_INDEX, (workDaysWithDefaultRate + workDaysWithOtherRate) * 2 * calendar.getWorkHoursPerDay());

      validator.setDataCellValue(firstActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", calendar.getWorkHoursPerDay() * (2 * workDaysWithDefaultRate + workDaysWithOtherRate) * 10d
           + calendar.getWorkHoursPerDay() * workDaysWithOtherRate * 3d, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", calendar.getWorkHoursPerDay() * (2 * workDaysWithDefaultRate + workDaysWithOtherRate) * 20d
           + calendar.getWorkHoursPerDay() * workDaysWithOtherRate * 4d, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);

      //set the effort of the activity to the normal effort between 6/10/2007 and 6/20/2007 because the resources are not 100% assigned
      validator.setDataCellValue(firstActivity, OpGanttValidator.BASE_EFFORT_COLUMN_INDEX, (workDaysWithDefaultRate + workDaysWithOtherRate) * calendar.getWorkHoursPerDay());
      //change the availability of the first resource to 40% and the availability of the second resource to 60%
      changeAvailabilityForResources(40d, 60d);

      double baseCostForWorker1 = calendar.getWorkHoursPerDay() * 40 / 100 * (workDaysWithDefaultRate * 10 + workDaysWithOtherRate * 3);
      double proceedCostForWorker1 = calendar.getWorkHoursPerDay() * 40 / 100 * (workDaysWithDefaultRate * 20 + workDaysWithOtherRate * 4);
      double baseCostForWorker2 = calendar.getWorkHoursPerDay() * 60 / 100 * 10 * (workDaysWithDefaultRate + workDaysWithOtherRate);
      double proceedCostForWorker2 = calendar.getWorkHoursPerDay() * 60 / 100 * 20 * (workDaysWithDefaultRate + workDaysWithOtherRate);

      validator.setDataCellValue(firstActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", baseCostForWorker1 + baseCostForWorker2, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", proceedCostForWorker1 + proceedCostForWorker2, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);
   }

   /**
    * Tests the updating of personnel cost when deleting one or more resources from an activity
    *
    * @throws Exception
    */
   public void testUpdatePersonnelCostRemoveResource()
        throws Exception {
      OpProjectCalendar calendar = OpProjectCalendar.getDefaultProjectCalendar();
      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);

      //add the first resource one to the first activity
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);
      validator.setDataCellValue(firstActivity, 26, resources);
      assertEquals("The base personnel cost was not set correctly ", 1600, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", 3200, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);

      //remove the resource from the activity
      resources.remove(WORKER1);
      validator.setDataCellValue(firstActivity, 26, resources);
      assertEquals("The base personnel cost was not set correctly ", 0, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", 0, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);

      //set the activity's start to 6/10/2007 and it's finish to 6/25/2007
      Date start = new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis());
      Date finish = new Date(getCalendarWithExactDaySet(2007, 6, 25).getTimeInMillis());
      validator.setDataCellValue(firstActivity, OpGanttValidator.START_COLUMN_INDEX, start);
      validator.setDataCellValue(firstActivity, OpGanttValidator.FINISH_COLUMN_INDEX, finish);

      //add an hourly rate period for resource 1 from 6/12/2007 - 6/16/2007
      addRatesToWorker1(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()), new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis()));

      //add both resources to the first activity
      resources = new ArrayList();
      resources.add(WORKER1);
      resources.add(WORKER2);

      int workDaysWithDefaultRate = calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 11).getTimeInMillis())).size();
      workDaysWithDefaultRate += calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 17).getTimeInMillis()),
           validator.getProjectPlanFinish()).size();
      int workDaysWithOtherRate = calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis())).size();

      //set the effort of the activity to the normal effort between 6/10/2007 and 6/20/2007 because the resources are not 100% assigned
      validator.setDataCellValue(firstActivity, OpGanttValidator.BASE_EFFORT_COLUMN_INDEX, (workDaysWithDefaultRate + workDaysWithOtherRate) * calendar.getWorkHoursPerDay());
      //change the availability of the first resource to 40% and the availability of the second resource to 60%
      changeAvailabilityForResources(40d, 60d);

      double baseCostForWorker1 = calendar.getWorkHoursPerDay() * 40 / 100 * (workDaysWithDefaultRate * 10 + workDaysWithOtherRate * 3);
      double proceedCostForWorker1 = calendar.getWorkHoursPerDay() * 40 / 100 * (workDaysWithDefaultRate * 20 + workDaysWithOtherRate * 4);
      double baseCostForWorker2 = calendar.getWorkHoursPerDay() * 60 / 100 * 10 * (workDaysWithDefaultRate + workDaysWithOtherRate);
      double proceedCostForWorker2 = calendar.getWorkHoursPerDay() * 60 / 100 * 20 * (workDaysWithDefaultRate + workDaysWithOtherRate);

      validator.setDataCellValue(firstActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", baseCostForWorker1 + baseCostForWorker2, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", proceedCostForWorker1 + proceedCostForWorker2, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);

      //remove the second resource from the activity
      resources = new ArrayList();
      resources.add(WORKER1);

      double hoursPerDay = calendar.getWorkHoursPerDay() * 40 / 100;
      double newNumberOfDays = Math.ceil(OpGanttValidator.getBaseEffort(firstActivity) / hoursPerDay);

      baseCostForWorker1 = hoursPerDay * ((newNumberOfDays - workDaysWithOtherRate) * 10 + workDaysWithOtherRate * 3);
      proceedCostForWorker1 = hoursPerDay * ((newNumberOfDays - workDaysWithOtherRate) * 20 + workDaysWithOtherRate * 4);

      validator.setDataCellValue(firstActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", baseCostForWorker1, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", proceedCostForWorker1, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);

      //remove the first resource from the activity
      resources.remove(WORKER1);
      validator.setDataCellValue(firstActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", 0d, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", 0d, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);
   }

   /**
    * Tests the updating of personnel cost when modifying one or more resources belonging to an activity
    *
    * @throws Exception
    */
   public void testUpdatePersonnelCostModifyResource()
        throws Exception {
      OpProjectCalendar calendar = OpProjectCalendar.getDefaultProjectCalendar();
      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      //remove the link between the first activity and the second activity
      XComponent secondActivity = (XComponent) validator.getDataSet().getChild(1);
      validator.setDataCellValue(firstActivity, OpGanttValidator.SUCCESSORS_COLUMN_INDEX, new ArrayList());
      validator.setDataCellValue(secondActivity, OpGanttValidator.PREDECESSORS_COLUMN_INDEX, new ArrayList());

      //set the activity's start to 6/10/2007 and it's finish to 6/25/2007
      Date start = new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis());
      Date finish = new Date(getCalendarWithExactDaySet(2007, 6, 25).getTimeInMillis());
      validator.setDataCellValue(firstActivity, OpGanttValidator.START_COLUMN_INDEX, start);
      validator.setDataCellValue(firstActivity, OpGanttValidator.FINISH_COLUMN_INDEX, finish);

      //add an hourly rate period for resource 1 from 6/12/2007 - 6/16/2007
      addRatesToWorker1(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()), new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis()));

      //change the availability of the first resource to 40% and the availability of the second resource to 60%
      changeAvailabilityForResources(40d, 60d);

      //add the first resource to the first activity
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);

      int workDaysWithDefaultRate = calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 11).getTimeInMillis())).size();
      workDaysWithDefaultRate += calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 17).getTimeInMillis()),
           validator.getProjectPlanFinish()).size();
      int workDaysWithOtherRate = calendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis())).size();

      //set the effort of the activity to the normal effort between 6/10/2007 and 6/20/2007 because the resources are not 100% assigned
      double standardEffort = (workDaysWithDefaultRate + workDaysWithOtherRate) * calendar.getWorkHoursPerDay();
      validator.setDataCellValue(firstActivity, OpGanttValidator.BASE_EFFORT_COLUMN_INDEX, standardEffort);

      double hoursPerDay = calendar.getWorkHoursPerDay() * 40 / 100;
      double newNumberOfDays = Math.ceil(OpGanttValidator.getBaseEffort(firstActivity) / hoursPerDay);

      double baseCostForWorker1 = hoursPerDay * ((newNumberOfDays - workDaysWithOtherRate) * 10 + workDaysWithOtherRate * 3);
      double proceedCostForWorker1 = hoursPerDay * ((newNumberOfDays - workDaysWithOtherRate) * 20 + workDaysWithOtherRate * 4);

      validator.setDataCellValue(firstActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", baseCostForWorker1, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", proceedCostForWorker1, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);

      //reassign the first resource to 100%
      //change the availability of the first resource to 100% and the availability of the second resource to 100%
      changeAvailabilityForResources(100d, 100d);
      //set the effort of the activity to the normal effort between 6/10/2007 and 6/20/2007 because the resource is 100% assigned
      validator.setDataCellValue(firstActivity, OpGanttValidator.BASE_EFFORT_COLUMN_INDEX, standardEffort);

      baseCostForWorker1 = calendar.getWorkHoursPerDay() * (workDaysWithDefaultRate * 10 + workDaysWithOtherRate * 3);
      proceedCostForWorker1 = calendar.getWorkHoursPerDay() * (workDaysWithDefaultRate * 20 + workDaysWithOtherRate * 4);

      validator.setDataCellValue(firstActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", baseCostForWorker1, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", proceedCostForWorker1, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);
   }

   /**
    * Tests the update of base costs for a collection
    *
    * @throws Exception
    */
   public void testUpdatePersonnelCostModifyCollection()
        throws Exception {

      OpProjectCalendar xCalendar = OpProjectCalendar.getDefaultProjectCalendar();
      //the second activity is the collection
      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(1);
      XComponent secondActivity = (XComponent) validator.getDataSet().getChild(2);
      XComponent thirdActivity = (XComponent) validator.getDataSet().getChild(3);

      validator.setDataCellValue(firstActivity, OpGanttValidator.SUCCESSORS_COLUMN_INDEX, new ArrayList());
      validator.setDataCellValue(secondActivity, OpGanttValidator.PREDECESSORS_COLUMN_INDEX, new ArrayList());
      validator.setDataCellValue(thirdActivity, OpGanttValidator.SUCCESSORS_COLUMN_INDEX, new ArrayList());

      validator.setProjectStart(new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
      validator.setProjectFinish(null);
      validator.setProjectPlanFinish(new Date(getCalendarWithExactDaySet(2007, 6, 30).getTimeInMillis()));

      //set the activity's start to 6/10/2007 and it's finish to 6/25/2007
      Date start = new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis());
      Date finish = new Date(getCalendarWithExactDaySet(2007, 6, 25).getTimeInMillis());
      validator.setDataCellValue(thirdActivity, OpGanttValidator.START_COLUMN_INDEX, start);
      validator.setDataCellValue(thirdActivity, OpGanttValidator.FINISH_COLUMN_INDEX, finish);

      //add two hourly rate periods for resource 1 from 6/12/2007 - 6/16/2007 and from 6/18/2007 - 6/22/2007
      XComponent hourlyRates = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(WORKER1_ID);
      Map rates = new TreeMap();
      List ratesList = new ArrayList();
      ratesList.add(new Double(10));
      ratesList.add(new Double(20));
      rates.put(new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()), ratesList);
      ratesList = new ArrayList();
      ratesList.add(new Double(3));
      ratesList.add(new Double(4));
      rates.put(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()), ratesList);
      ratesList = new ArrayList();
      ratesList.add(new Double(10));
      ratesList.add(new Double(20));
      rates.put(new Date(getCalendarWithExactDaySet(2007, 6, 17).getTimeInMillis()), ratesList);
      ratesList = new ArrayList();
      ratesList.add(new Double(5));
      ratesList.add(new Double(2));
      rates.put(new Date(getCalendarWithExactDaySet(2007, 6, 18).getTimeInMillis()), ratesList);
      ratesList = new ArrayList();
      ratesList.add(new Double(10));
      ratesList.add(new Double(20));
      rates.put(new Date(getCalendarWithExactDaySet(2007, 6, 23).getTimeInMillis()), ratesList);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(rates);
      dataRow.addChild(dataCell);
      hourlyRates.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(WORKER2_ID);
      rates = new TreeMap();
      ratesList = new ArrayList();
      ratesList.add(new Double(10));
      ratesList.add(new Double(20));
      rates.put(new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()), ratesList);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(rates);
      dataRow.addChild(dataCell);
      hourlyRates.addChild(dataRow);
      validator.setHourlyRatesDataSet(hourlyRates);

      //change the effort of activity 3 to 88h
      validator.setDataCellValue(thirdActivity, OpGanttValidator.BASE_EFFORT_COLUMN_INDEX, 88d);
      Date minDate = validator.getProjectPlanFinish();
      if (OpGanttValidator.getEnd(thirdActivity).before(minDate)) {
         minDate = OpGanttValidator.getEnd(thirdActivity);
      }

      int workDaysWithDefaultRate = xCalendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 11).getTimeInMillis())).size();
      workDaysWithDefaultRate += xCalendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 17).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 17).getTimeInMillis())).size();
      workDaysWithDefaultRate += xCalendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 23).getTimeInMillis()),
           minDate).size();
      int workDaysWithFirstRate = xCalendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis())).size();
      int workDaysWithSecondRate = xCalendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 18).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 22).getTimeInMillis())).size();

      double baseCostForWorker1 = xCalendar.getWorkHoursPerDay() * (workDaysWithDefaultRate * 10 + workDaysWithFirstRate * 3 + workDaysWithSecondRate * 5);
      double proceedCostForWorker1 = xCalendar.getWorkHoursPerDay() * (workDaysWithDefaultRate * 20 + workDaysWithFirstRate * 4 + workDaysWithSecondRate * 2);

      //add the first resource to the third activity
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);

      validator.setDataCellValue(thirdActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", baseCostForWorker1, OpGanttValidator.getBasePersonnelCosts(thirdActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", proceedCostForWorker1, OpGanttValidator.getBaseProceeds(thirdActivity), DOUBLE_ERROR_MARGIN);

      //the base costs were modified for the collection also
      validator.setDataCellValue(thirdActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", baseCostForWorker1, OpGanttValidator.getBasePersonnelCosts(secondActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", proceedCostForWorker1, OpGanttValidator.getBaseProceeds(secondActivity), DOUBLE_ERROR_MARGIN);
   }

   /**
    * Tests the update of base costs when modifying different fields of a milestone activity.
    * Normally this shouldn't modify the costs of the milestone.
    *
    * @throws Exception
    */
   public void testUpdatePersonnelCostModifyMilestone()
        throws Exception {

      XComponent milestone = (XComponent) validator.getDataSet().getChild(4);

      validator.setProjectStart(new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
      validator.validateDataSet();
      validator.setProjectFinish(null);
      validator.setProjectPlanFinish(new Date(getCalendarWithExactDaySet(2007, 6, 30).getTimeInMillis()));

      //set the milestone's date to 6/10/2007
      Date date = new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis());
      validator.setDataCellValue(milestone, OpGanttValidator.START_COLUMN_INDEX, date);
      validator.setDataCellValue(milestone, OpGanttValidator.FINISH_COLUMN_INDEX, date);

      //add an hourly rate period for resource 1 from 6/12/2007 - 6/16/2007
      addRatesToWorker1(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()), new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis()));

      //add the first resource to the milestone
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);

      validator.setDataCellValue(milestone, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", 0d, OpGanttValidator.getBasePersonnelCosts(milestone), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", 0d, OpGanttValidator.getBaseProceeds(milestone), DOUBLE_ERROR_MARGIN);

      //change the date of the milestone to 6/12/2007
      date = new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis());
      validator.setDataCellValue(milestone, OpGanttValidator.START_COLUMN_INDEX, date);
      validator.setDataCellValue(milestone, OpGanttValidator.FINISH_COLUMN_INDEX, date);

      validator.setDataCellValue(milestone, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", 0d, OpGanttValidator.getBasePersonnelCosts(milestone), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", 0d, OpGanttValidator.getBaseProceeds(milestone), DOUBLE_ERROR_MARGIN);
   }

   /**
    * Tests the update of base costs when modifying the duration of the activity
    *
    * @throws Exception
    */
   public void testUpdatePersonnelCostModifyDuration()
        throws Exception {

      OpProjectCalendar xCalendar = OpProjectCalendar.getDefaultProjectCalendar();
      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);

      validator.setDataCellValue(firstActivity, OpGanttValidator.SUCCESSORS_COLUMN_INDEX, new ArrayList());

      validator.setProjectStart(new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()));
      validator.setProjectFinish(null);
      validator.setProjectPlanFinish(new Date(getCalendarWithExactDaySet(2007, 6, 30).getTimeInMillis()));

      //set the activity's start to 6/10/2007 and it's finish to 6/25/2007
      Date start = new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis());
      Date finish = new Date(getCalendarWithExactDaySet(2007, 6, 25).getTimeInMillis());
      validator.setDataCellValue(firstActivity, OpGanttValidator.START_COLUMN_INDEX, start);
      validator.setDataCellValue(firstActivity, OpGanttValidator.FINISH_COLUMN_INDEX, finish);

      //add an hourly rate period for resource 1 from 6/12/2007 - 6/16/2007
      addRatesToWorker1(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()), new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis()));

      //change the duration of the activity by adding one day
      validator.setDataCellValue(firstActivity, OpGanttValidator.DURATION_COLUMN_INDEX, OpGanttValidator.getDuration(firstActivity) + 1);
      Date minDate = validator.getProjectPlanFinish();
      if (OpGanttValidator.getEnd(firstActivity).before(minDate)) {
         minDate = OpGanttValidator.getEnd(firstActivity);
      }

      int workDaysWithDefaultRate = xCalendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 11).getTimeInMillis())).size();
      workDaysWithDefaultRate += xCalendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 17).getTimeInMillis()),
           minDate).size();
      int workDaysWithFirstRate = xCalendar.getWorkingDaysFromInterval(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()),
           new Date(getCalendarWithExactDaySet(2007, 6, 16).getTimeInMillis())).size();

      double baseCostForWorker1 = xCalendar.getWorkHoursPerDay() * (workDaysWithDefaultRate * 10 + workDaysWithFirstRate * 3);
      double proceedCostForWorker1 = xCalendar.getWorkHoursPerDay() * (workDaysWithDefaultRate * 20 + workDaysWithFirstRate * 4);

      //add the first resource to the third activity
      ArrayList resources = new ArrayList();
      resources.add(WORKER1);

      validator.setDataCellValue(firstActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);
      assertEquals("The base personnel cost was not set correctly ", baseCostForWorker1, OpGanttValidator.getBasePersonnelCosts(firstActivity), DOUBLE_ERROR_MARGIN);
      assertEquals("The base proceed cost was not set correctly ", proceedCostForWorker1, OpGanttValidator.getBaseProceeds(firstActivity), DOUBLE_ERROR_MARGIN);
   }

   /**
    * Computes the expected end date for a given start date and a duration taking into account
    * only the working days.
    *
    * @param start    start date for the analyzed activity
    * @param duration duration of the activity
    * @return the expected end date
    */
   private long getEndTimeForDuration(Date start, double duration) {
      OpProjectCalendar calendar = validator.getCalendar();
      long time = start.getTime();
      Date currentDate = new Date(time);
      while (duration > 0) {
         if (calendar.isWorkDay(currentDate)) {
            duration -= calendar.getWorkHoursPerDay();
         }
         time += OpProjectCalendar.MILLIS_PER_DAY;
         currentDate = new Date(time);
      }
      if (time != start.getTime()) {
         time -= OpProjectCalendar.MILLIS_PER_DAY;
      }
      return time;
   }

   /**
    * Computes the expected duration for a given time interval taking into account only
    * the working days.
    *
    * @param start - start date of the interval
    * @param end   - end date of the interval
    * @return the expected duration for the interval.
    */
   private long getDurationForPeriod(Date start, Date end) {
      OpProjectCalendar calendar = validator.getCalendar();
      long time = start.getTime();
      Date currentDate = new Date(time);
      long duration = 0;
      while (time <= end.getTime()) {
         if (calendar.isWorkDay(currentDate)) {
            duration += calendar.getWorkHoursPerDay();
         }
         time += OpProjectCalendar.MILLIS_PER_DAY;
         currentDate = new Date(time);
      }
      return duration;
   }


   /**
    * Tests that a new data row is added by the validator to the already existent data.
    *
    * @throws Exception if anything fails.
    */
   public void testNewDataRow()
        throws Exception {
      double duration = WORKING_DAYS_IN_A_WEEK * validator.getCalendar().getWorkHoursPerDay();

      XComponent dataRow = validator.newDataRow();
      assertNotNull("No data row created ", dataRow);
      //Name
      assertNull("The name is not null ", OpGanttValidator.getName(dataRow));
      assertEquals("The type is not correct ", OpGanttValidator.STANDARD, OpGanttValidator.getType(dataRow));
      assertEquals("The % complete is not correct ", 0.0d, OpGanttValidator.getComplete(dataRow), 0);
      // different for weekdays and wekends
      if (!OpProjectCalendar.getDefaultProjectCalendar().isWorkDay(OpProjectCalendar.today())) {
         assertEquals("The start date is not correct ", OpProjectCalendar.getDefaultProjectCalendar().nextWorkDay(OpProjectCalendar.today()), OpGanttValidator.getStart(dataRow));
      }
      else {
         assertEquals("The start date is not correct ", OpProjectCalendar.today(), OpGanttValidator.getStart(dataRow));
      }

      long endTime = OpProjectCalendar.today().getTime();
      Date expectedEndDate = new Date(endTime);
      int index = 0;
      while (index < WORKING_DAYS_IN_A_WEEK) {
         if (validator.getCalendar().isWorkDay(expectedEndDate)) {
            index++;
         }
         if (index < WORKING_DAYS_IN_A_WEEK) {
            expectedEndDate = new Date(expectedEndDate.getTime() + DAY_MILLIS);
         }
      }
      assertEquals("The end date is not correct ", expectedEndDate, OpGanttValidator.getEnd(dataRow));

      assertEquals("The durration is not correct ", duration, OpGanttValidator.getDuration(dataRow), 0);
      assertEquals("The effort is not correct ", duration, OpGanttValidator.getBaseEffort(dataRow), 0);
      assertEquals("The predecessors are not correct ", 0, OpGanttValidator.getPredecessors(dataRow).size());
      assertEquals("The succesors are not correct ", 0, OpGanttValidator.getSuccessors(dataRow).size());
      assertEquals("The resources are not correct ", 0, OpGanttValidator.getResources(dataRow).size());
      assertEquals("The base personel costs are not correct ", 0.0d, OpGanttValidator.getBasePersonnelCosts(dataRow), 0);
      assertEquals("The base travel costs are not correct ", 0.0d, OpGanttValidator.getBaseTravelCosts(dataRow), 0);
      assertEquals("The base material costs are not correct ", 0.0d, OpGanttValidator.getBaseMaterialCosts(dataRow), 0);
      assertEquals("The base external costs are not correct ", 0.0d, OpGanttValidator.getBaseExternalCosts(dataRow), 0);
      assertEquals("The base miscellneous costs are not correct ", 0.0d, OpGanttValidator.getBaseMiscellaneousCosts(dataRow), 0);
      assertNull("The description is not null", OpGanttValidator.getDescription(dataRow));
      assertEquals("The attachments are not correct ", 0, OpGanttValidator.getAttachments(dataRow).size());
      assertEquals("The mode is not correct ", (byte) 0, OpGanttValidator.getAttributes(dataRow));
      assertNotNull("The work phase begin is not correct ", OpGanttValidator.getWorkPhases(dataRow));
      assertNotNull("The ressource based effort is not correct ", OpGanttValidator.getResourceBaseEfforts(dataRow));
      assertEquals("Priority is not correct ", OpGanttValidator.DEFAULT_PRIORITY, OpGanttValidator.getPriority(dataRow).byteValue());
      assertNotNull("Work records map is not correct ", OpGanttValidator.getWorkRecords(dataRow));
      assertEquals("Actual effort is not correct ", 0.0d, OpGanttValidator.getActualEffort(dataRow), 0);
   }

   /**
    * Tests the addition of data rows using the validator.
    *
    * @throws Exception if anything fails.
    */
   public void testAddDataRow()
        throws Exception {
      XComponent dataRow = createNewDataRow();
      int dataRowsNr = validator.getDataSet().getChildCount();
      validator.addDataRow(dataRow);
      assertEquals("There is an invalid number of data-rows ", dataRowsNr + 1, validator.getDataSet().getChildCount());
   }

   /**
    * Tests that when a data row is added to the begining of a validator's data-set, the successors and predecessors of
    * the dependent data rows are updated
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateIndexesAtBegining()
        throws Exception {
      int dataRowsNr = assertTestDataMinimumSize(2);

      XComponent dataRow1 = (XComponent) validator.getDataSet().getChild(0);
      final Integer successorIndex = new Integer(1);
      OpGanttValidator.setSuccessors(dataRow1, new TreeMap() {{ put(successorIndex, new Integer(OpGanttValidator.DEP_DEFAULT)); }});
      XComponent dataRow2 = (XComponent) validator.getDataSet().getChild(1);

      final Integer predecessorIndex = new Integer(0);
      OpGanttValidator.setPredecessors(dataRow2, new TreeMap() {{ put(predecessorIndex, new Integer(OpGanttValidator.DEP_DEFAULT)); }});


      XComponent newDataRow = createNewDataRow();
      validator.addDataRow(0, newDataRow);
      assertEquals("The data row was not added to the validator ", dataRowsNr + 1, validator.getDataSet().getChildCount());
      assertEquals("The data row was not added to the right position ", newDataRow, validator.getDataSet().getChild(0));

      dataRow1 = (XComponent) validator.getDataSet().getChild(1);
      Integer newSuccessorIndex = (Integer) OpGanttValidator.getSuccessors(dataRow1).get(0);
      assertEquals("The successor index was not updated ", new Integer(successorIndex.intValue() + 1), newSuccessorIndex);
      dataRow2 = (XComponent) validator.getDataSet().getChild(2);
      Integer newPredecessorIndex = (Integer) OpGanttValidator.getPredecessors(dataRow2).get(0);
      assertEquals("The predecessor index was not updated ", new Integer(predecessorIndex.intValue() + 1), newPredecessorIndex);
   }

   /**
    * Tests that when a data row is added to the middle of a validator's data-set, the successors and predecessors of
    * the dependent data rows are updated
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateIndexesInMiddle()
        throws Exception {
      int dataRowsNr = assertTestDataMinimumSize(2);

      XComponent dataRow1 = (XComponent) validator.getDataSet().getChild(0);
      final Integer successorIndex = new Integer(1);
      OpGanttValidator.setSuccessors(dataRow1, new TreeMap() {{ put(successorIndex, new Integer(OpGanttValidator.DEP_DEFAULT)); }});
      XComponent dataRow2 = (XComponent) validator.getDataSet().getChild(1);
      final Integer predecessorIndex = new Integer(0);
      OpGanttValidator.setPredecessors(dataRow2, new TreeMap() {{ put(predecessorIndex, new Integer(OpGanttValidator.DEP_DEFAULT)); }});

      XComponent newDataRow = createNewDataRow();
      validator.addDataRow(1, newDataRow);
      assertEquals("The data row was not added to the validator ", dataRowsNr + 1, validator.getDataSet().getChildCount());
      assertEquals("The data row was not added to the right position ", newDataRow, validator.getDataSet().getChild(1));

      dataRow1 = (XComponent) validator.getDataSet().getChild(0);
      Integer newSuccessorIndex = (Integer) OpGanttValidator.getSuccessors(dataRow1).get(0);
      assertEquals("The successor index was not updated ", new Integer(successorIndex.intValue() + 1), newSuccessorIndex);
      dataRow2 = (XComponent) validator.getDataSet().getChild(2);
      Integer newPredecessorIndex = (Integer) OpGanttValidator.getPredecessors(dataRow2).get(0);
      assertEquals("The predecessor index was not updated ", predecessorIndex, newPredecessorIndex);
   }

   /**
    * Tests that when a data row is added to the end of a validator's data-set, the successors and predecessors of
    * the other data rows are not updated
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateIndexesAtEnd()
        throws Exception {
      int dataRowsNr = assertTestDataMinimumSize(2);

      XComponent dataRow1 = (XComponent) validator.getDataSet().getChild(0);
      final Integer successorIndex = new Integer(1);
      OpGanttValidator.setSuccessors(dataRow1, new TreeMap() {{ put(successorIndex, new Integer(OpGanttValidator.DEP_DEFAULT)); }});
      XComponent dataRow2 = (XComponent) validator.getDataSet().getChild(1);
      final Integer predecessorIndex = new Integer(0);
      OpGanttValidator.setPredecessors(dataRow2, new TreeMap() {{ put(predecessorIndex, new Integer(OpGanttValidator.DEP_DEFAULT)); }});

      XComponent newDataRow = createNewDataRow();
      validator.addDataRow(2, newDataRow);
      assertEquals("The data row was not added to the validator ", dataRowsNr + 1, validator.getDataSet().getChildCount());
      assertEquals("The data row was not added to the right position ", newDataRow, validator.getDataSet().getChild(2));

      dataRow1 = (XComponent) validator.getDataSet().getChild(0);
      Integer newSuccessorIndex = (Integer) OpGanttValidator.getSuccessors(dataRow1).get(0);
      assertEquals("The successor index was not updated ", successorIndex, newSuccessorIndex);
      dataRow2 = (XComponent) validator.getDataSet().getChild(1);
      Integer newPredecessorIndex = (Integer) OpGanttValidator.getPredecessors(dataRow2).get(0);
      assertEquals("The predecessor index was not updated ", predecessorIndex, newPredecessorIndex);
   }

   /**
    * Checks that the validator's data set has a minimum number of data-rows.
    *
    * @param size a <code>int</code> representing the minimum number of data rows the data set should have.
    * @return an <code>int</code> representing the actual number of rows in the dataset.
    */
   private int assertTestDataMinimumSize(int size) {
      int dataRowsNr = validator.getDataSet().getChildCount();
      assertTrue("At least " + size + " data rows should be present ", size <= dataRowsNr);
      return dataRowsNr;
   }

   /**
    * Tests that an array of data-rows is properly removed from a data-set.
    *
    * @throws Exception if anything fails.
    */
   public void testRemoveDataRows()
        throws Exception {
      int datasetSize = this.assertTestDataMinimumSize(2);

      ArrayList rows = new ArrayList();
      rows.add(validator.getDataSet().getChild(0));
      rows.add(validator.getDataSet().getChild(1));

      validator.removeDataRows(rows);
      assertEquals("The data rows were not removed from the data set ", datasetSize - 2, validator.getDataSet().getChildCount());
   }

   /**
    * Tests that values for the data cells are correctly set by the <code>XGantValidator</code>.
    * <FIXME author="Horia Chiorean" description="There are some missing properties not checked by the XGantValidator">
    *
    * @throws Exception if anything fails.
    */
   public void testSetDataCellValue()
        throws Exception {
      this.assertTestDataMinimumSize(3);

      XComponent activity0 = (XComponent) validator.getDataSet().getChild(0);
      XComponent activity1 = (XComponent) validator.getDataSet().getChild(1);
      XComponent activity2 = (XComponent) validator.getDataSet().getChild(2);

      String newName = "newName";
      validator.setDataCellValue(activity0, OpGanttValidator.NAME_COLUMN_INDEX, newName);
      assertEquals("The name has not been changed ", newName, OpGanttValidator.getName(activity0));

      byte type = 2;
      validator.setDataCellValue(activity0, OpGanttValidator.TYPE_COLUMN_INDEX, new Byte(type));
      assertEquals("The type has not been changed ", type, OpGanttValidator.getType(activity0));

      String category = "someCategory";
      validator.setDataCellValue(activity0, OpGanttValidator.CATEGORY_COLUMN_INDEX, category);
      assertEquals("The category has not been changed ", category, OpGanttValidator.getCategory(activity0));

      //activity is milestone -> complete will be set to 0 unless 100 is given
      byte percentComplete = 75;
      validator.setDataCellValue(activity0, OpGanttValidator.COMPLETE_COLUMN_INDEX, new Double(percentComplete));
      assertEquals("The % complete has not been changed ", 0, OpGanttValidator.getComplete(activity0), 0);

      //change activity back to standart
      type = OpGanttValidator.STANDARD;
      validator.setDataCellValue(activity0, OpGanttValidator.TYPE_COLUMN_INDEX, new Byte(type));
      assertEquals("The type has not been changed ", type, OpGanttValidator.getType(activity0));

      percentComplete = 10;
      validator.setDataCellValue(activity0, OpGanttValidator.COMPLETE_COLUMN_INDEX, new Double(percentComplete));
      assertEquals("The % complete has not been changed ", percentComplete, OpGanttValidator.getComplete(activity0), 0);

      percentComplete = 100;
      validator.setDataCellValue(activity0, OpGanttValidator.COMPLETE_COLUMN_INDEX, new Double(percentComplete));
      assertEquals("The % complete has not been changed ", percentComplete, OpGanttValidator.getComplete(activity0), 0);

      byte bigPercentComplete = 101;
      validator.setDataCellValue(activity0, OpGanttValidator.COMPLETE_COLUMN_INDEX, new Double(bigPercentComplete));
      assertEquals("The % complete has been changed ", percentComplete, OpGanttValidator.getComplete(activity0), 0);

      byte smallPercentComplete = -1;
      validator.setDataCellValue(activity0, OpGanttValidator.COMPLETE_COLUMN_INDEX, new Double(smallPercentComplete));
      assertEquals("The % complete has been changed ", percentComplete, OpGanttValidator.getComplete(activity0), 0);

      Date startDate = new Date(System.currentTimeMillis());
      Calendar startDateCalendar = OpProjectCalendar.getDefaultProjectCalendar().getCalendar();
      startDateCalendar.setTimeInMillis(startDate.getTime());
      startDateCalendar.set(Calendar.HOUR_OF_DAY, 0);
      startDateCalendar.set(Calendar.MINUTE, 0);
      startDateCalendar.set(Calendar.SECOND, 0);
      startDateCalendar.set(Calendar.MILLISECOND, 0);
      startDate.setTime(startDateCalendar.getTimeInMillis() + DAY_MILLIS);

      validator.setDataCellValue(activity0, OpGanttValidator.START_COLUMN_INDEX, startDate);
      if (!validator.getCalendar().isWorkDay(startDate)) {
         startDate = validator.getCalendar().nextWorkDay(startDate);
      }
      assertEquals("The start date has not been changed ", startDate, OpGanttValidator.getStart(activity0));

      Date endDate = new Date(System.currentTimeMillis());
      Calendar endDateCalendar = OpProjectCalendar.getDefaultProjectCalendar().getCalendar();
      endDateCalendar.setTimeInMillis(endDate.getTime());

      /*reset the date hour-minute-second-millisecond*/
      endDateCalendar.set(Calendar.HOUR_OF_DAY, 0);
      endDateCalendar.set(Calendar.MINUTE, 0);
      endDateCalendar.set(Calendar.SECOND, 0);
      endDateCalendar.set(Calendar.MILLISECOND, 0);
      endDate.setTime(endDateCalendar.getTimeInMillis() + DAY_MILLIS);
      validator.setDataCellValue(activity0, OpGanttValidator.FINISH_COLUMN_INDEX, endDate);

      if (!validator.getCalendar().isWorkDay(endDate)) {
         endDate = validator.getCalendar().nextWorkDay(endDate);
      }
      assertEquals("The end date has not been changed ", endDate, OpGanttValidator.getEnd(activity0));

      double baseEffort = 40.5; //will be changed to 48 (roundup)
      OpGanttValidator.setType(activity0, OpGanttValidator.STANDARD);
      validator.setDataCellValue(activity0, OpGanttValidator.BASE_EFFORT_COLUMN_INDEX, new Double(baseEffort));
      assertEquals("The base effort has been changed ", 48, OpGanttValidator.getBaseEffort(activity0), 0);

      assertActivitiesDependency();

      try {
         validator.setDataCellValue(activity0, OpGanttValidator.PREDECESSORS_COLUMN_INDEX, new ArrayList(new ArrayList(Arrays.asList(
              new Integer[]{new Integer(validator.getDataSet().getChildCount())}))));
         fail("The list of predecessors shouldn't have changed ");
      }
      catch (XValidationException e) {
         //expected exception - index is outsite the bounds
         assertEquals("Exception should be range exception", OpGanttValidator.RANGE_EXCEPTION, e.getMessage());
      }

      //make activity0 a predecessor of activity2
      ArrayList newPredecessor = new ArrayList();
      newPredecessor.add(new Integer(0));
      validator.setDataCellValue(activity2, OpGanttValidator.PREDECESSORS_COLUMN_INDEX, newPredecessor);
      SortedMap activity1Successors = OpGanttValidator.getSuccessors(activity1);
      assertEquals("There should be no successors for activity 1", 0, activity1Successors.size());
      SortedMap activity0Successors = OpGanttValidator.getSuccessors(activity0);
      assertEquals("There should be 2 successors for activity 0", 2, activity0Successors.size());

      //set the successor list of activity1 only to activity2
      SortedMap originalSucessors = OpGanttValidator.getSuccessors(activity0);
      SortedMap successors = new TreeMap();
      successors.put(new Integer(validator.getDataSet().getChildCount()), OpGanttValidator.DEP_DEFAULT);
      try {
         validator.setDataCellValue(activity0, OpGanttValidator.SUCCESSORS_COLUMN_INDEX, successors);
         fail("The list of sucessors shouldn't have changed ");
      }
      catch (XValidationException e) {
         //out of range exception
         assertEquals("Expected RangeException", OpGanttValidator.RANGE_EXCEPTION, e.getMessage());
      }

      ArrayList newSucessor = new ArrayList();
      newSucessor.add(new Integer(1));
      validator.setDataCellValue(activity0, OpGanttValidator.SUCCESSORS_COLUMN_INDEX, newSucessor);


      try {
         validator.setDataCellValue(activity1, OpGanttValidator.SUCCESSORS_COLUMN_INDEX, newSucessor);
         fail("Loop should have been detected");
      }
      catch (OpActivityLoopException e) {
         //expected exception
      }

      validator.setDataCellValue(activity2, OpGanttValidator.PREDECESSORS_COLUMN_INDEX, newSucessor);

      ArrayList newResources = new ArrayList();
      newResources.add(WORKER1);
      OpGanttValidator.setResources(activity0, newResources);
      assertEquals("The resources have not changed ", newResources, OpGanttValidator.getResources(activity0));

      double travelCosts = 40.5;
      validator.setDataCellValue(activity0, OpGanttValidator.BASE_TRAVEL_COSTS_COLUMN_INDEX, new Double(travelCosts));
      assertEquals("The base travel costs have not been changed ", travelCosts, OpGanttValidator.getBaseTravelCosts(activity0), 0);
      double materialCosts = 100.5;
      validator.setDataCellValue(activity0, OpGanttValidator.BASE_MATERIAL_COSTS_COLUMN_INDEX, new Double(materialCosts));
      assertEquals("The base material costs have not been changed ", materialCosts, OpGanttValidator.getBaseMaterialCosts(activity0), 0);
      double externalCosts = 9900.5;
      validator.setDataCellValue(activity0, OpGanttValidator.BASE_EXTERNAL_COSTS_COLUMN_INDEX, new Double(externalCosts));
      assertEquals("The base external costs have not been changed ", externalCosts, OpGanttValidator.getBaseExternalCosts(activity0), 0);
      double miscCosts = 10.5;
      validator.setDataCellValue(activity0, OpGanttValidator.BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX, new Double(miscCosts));
      assertEquals("The base external costs have not been changed ", miscCosts, OpGanttValidator.getBaseMiscellaneousCosts(activity0), 0);

      ArrayList attachments = new ArrayList();
      attachments.add("No idea what should be in here");
      validator.setDataCellValue(activity0, OpGanttValidator.ATTACHMENTS_COLUMN_INDEX, attachments);
      assertEquals("The attachments should've changed ", attachments, OpGanttValidator.getAttachments(activity0));
   }

   /**
    * Checks that the data set used by this test's validator has the first 3 activities with the
    * following dependency: Activity1 --> Activity2 --> Activity3
    */
   private void assertActivitiesDependency() {
      XComponent activity1 = (XComponent) validator.getDataSet().getChild(0);
      SortedMap activity1Successors = OpGanttValidator.getSuccessors(activity1);
      assertEquals("Activity1 should have 1 sucessor ", 1, activity1Successors.size());
      assertEquals("Activity1 has the wrong sucessor ", 1, ((Integer) activity1Successors.get(0)).intValue());

      XComponent activity2 = (XComponent) validator.getDataSet().getChild(1);
      SortedMap activity2Predecessors = OpGanttValidator.getPredecessors(activity2);
      assertEquals("The second activity should have 1 predecessor ", 1, activity2Predecessors.size());
      assertEquals("The second activity has the wrong predecessor ", 0, ((Integer) activity2Predecessors.get(0)).intValue());
      SortedMap activity2Successors = OpGanttValidator.getSuccessors(activity2);
      assertEquals("The second activity should have 1 sucessor ", 1, activity2Successors.size());
      assertEquals("The second activity has the wrong sucessor ", 2, ((Integer) activity2Successors.get(0)).intValue());

      XComponent activity3 = (XComponent) validator.getDataSet().getChild(2);
      SortedMap activity3Predecessors = OpGanttValidator.getPredecessors(activity3);
      assertEquals("Activity3 should have 1 predecessor ", 1, activity3Predecessors.size());
      assertEquals("Activity3 has the wrong predecessor ", 1, ((Integer) activity3Predecessors.get(0)).intValue());
   }

   /**
    * Tests that the validator correctly moves a collection of  data rows upwards.
    *
    * @throws Exception if anything fails.
    */
   public void testMoveCollectionRowsUp()
        throws Exception {
      this.assertTestDataMinimumSize(3);

      Map originalIndexMap = this.getIndexMap();

      ArrayList rowsToMove = new ArrayList();
      //add the first 2 rows and test that nothing happens
      rowsToMove.add(validator.getDataSet().getChild(0));
      rowsToMove.add(validator.getDataSet().getChild(1));
      validator.moveDataRows(rowsToMove, -1);

      Map movedIndexMap = this.getIndexMap();
      assertEquals("The rows new index shouldn't have moved ", originalIndexMap.keySet(), movedIndexMap.keySet());
      assertEquals("The rows original index shouldn't have moved ", new ArrayList(originalIndexMap.values()),
           new ArrayList(movedIndexMap.values()));
      //clear the rows
      rowsToMove.clear();

      int firstRowIndex = 2;
      XView firstRow = validator.getDataSet().getChild(firstRowIndex);
      rowsToMove.add(firstRow);
      int secondRowIndex = 3;
      XView secondRow = validator.getDataSet().getChild(secondRowIndex);
      rowsToMove.add(secondRow);

      validator.moveDataRows(rowsToMove, -1);
      assertEquals("The first row has not been moved up ", firstRow, validator.getDataSet().getChild(1));
      assertEquals("The second row has not been moved up ", secondRow, validator.getDataSet().getChild(2));
      SortedMap successors = OpGanttValidator.getSuccessors((XComponent) secondRow);
      assertTrue("The list of successors has not been updated ", successors.keySet().contains(new Integer(4)));
      SortedMap predecessors = OpGanttValidator.getPredecessors((XComponent) firstRow);
      assertTrue("The list of predecessors has not been updated ", predecessors.keySet().contains(new Integer(3)));

   }

   /**
    * Tests that the validator correctly moves a collection of data rows upwards another collection.
    *
    * @throws Exception if anything fails.
    */
   public void testMoveCollectionRowsUpOverAnotherCollectionRows()
        throws Exception {
      int dataSetSize = this.assertTestDataMinimumSize(4);


      ArrayList rowsToMove = new ArrayList();

      int firstRowIndex = dataSetSize - 2;
      XView firstRow = validator.getDataSet().getChild(firstRowIndex);
      rowsToMove.add(firstRow);
      int secondRowIndex = dataSetSize - 1;
      XView secondRow = validator.getDataSet().getChild(secondRowIndex);
      rowsToMove.add(secondRow);

      validator.moveDataRows(rowsToMove, validator.getMovingOffset((XComponent) firstRow, (XComponent) secondRow, -1));
      assertEquals("The first row has not been moved up ", firstRow, validator.getDataSet().getChild(5));
      assertEquals("The second row has not been moved up ", secondRow, validator.getDataSet().getChild(6));
      SortedMap successors = OpGanttValidator.getSuccessors((XComponent) firstRow);
      assertEquals("The list of successors has not been updated ", successors, new ArrayList());
      SortedMap predecessors = OpGanttValidator.getPredecessors((XComponent) firstRow);
      assertTrue("The list of predecessors has not been updated ", predecessors.keySet().contains(new Integer(8)));
      predecessors = OpGanttValidator.getPredecessors((XComponent) secondRow);
      assertTrue("The list of predecessors has been modified", predecessors.keySet().contains(new Integer(3)));

   }

   /**
    * Tests that the validator correctly moves rows  of collection data rows upwards a milestone activity.
    *
    * @throws Exception if anything fails.
    */
   public void testMoveCollectionRowsUpOverMilestoneActivity()
        throws Exception {
      this.assertTestDataMinimumSize(3);
      /*move a children of a collection upwards a MILESTONE activity =>should not move becouse a MILESTONE cannot become
     a collection*/
      ArrayList rowsToMove = new ArrayList();
      int firstRowIndex = 6;
      XView firstRow = validator.getDataSet().getChild(firstRowIndex);
      rowsToMove.add(firstRow);
      try {
         validator.moveDataRows(rowsToMove, validator.getMovingOffset((XComponent) firstRow, (XComponent) firstRow, -1));
         fail("Activity has been moved!");
      }
      catch (XValidationException e) {
         //activity can't be moved as a child of the milestone
      }

      /*move a entire collection upwards a MILESTONE activity*/
      rowsToMove.clear();
      //add a collection
      firstRowIndex = 5;
      firstRow = validator.getDataSet().getChild(firstRowIndex);
      rowsToMove.add(firstRow);

      int secondRowIndex = 6;
      XView secondRow = validator.getDataSet().getChild(secondRowIndex);
      rowsToMove.add(secondRow);

      validator.moveDataRows(rowsToMove, validator.getMovingOffset((XComponent) firstRow, (XComponent) secondRow, -1));
      assertEquals("The first row has not been moved up ", firstRow, validator.getDataSet().getChild(4));
      assertEquals("The second row has not been moved up ", secondRow, validator.getDataSet().getChild(5));
      SortedMap successors = OpGanttValidator.getSuccessors((XComponent) firstRow);
      assertEquals("The list of successors has not been updated ", successors, new ArrayList());
      SortedMap predecessors = OpGanttValidator.getPredecessors((XComponent) firstRow);
      assertTrue("The list of predecessors has not been updated ", predecessors.keySet().contains(new Integer(6)));
   }

   /**
    * Tests that the validator correctly moves data rows upwards.
    *
    * @throws Exception if anything fails.
    */
   public void testMoveRowsUp()
        throws Exception {
      this.assertTestDataMinimumSize(5);
      Map originalIndexMap = this.getIndexMap();

      ArrayList rowsToMove = new ArrayList();
      //add the first 2 rows and test that nothing happens
      rowsToMove.add(validator.getDataSet().getChild(0));
      rowsToMove.add(validator.getDataSet().getChild(1));
      validator.moveDataRows(rowsToMove, -1);
      Map movedIndexMap = this.getIndexMap();
      assertEquals("The rows new index shouldn't have moved ", originalIndexMap.keySet(), movedIndexMap.keySet());
      assertEquals("The rows original index shouldn't have moved ", new ArrayList(originalIndexMap.values()),
           new ArrayList(movedIndexMap.values()));

      //add the last 2 rows
      rowsToMove.clear();
      int firstRowIndex = 2;
      XView firstRow = validator.getDataSet().getChild(firstRowIndex);
      rowsToMove.add(firstRow);
      rowsToMove.add(validator.getDataSet().getChild(3));

      int secondRowIndex = 4;
      XView secondRow = validator.getDataSet().getChild(secondRowIndex);
      rowsToMove.add(secondRow);

      validator.moveDataRows(rowsToMove, -1);
      assertEquals("The first row has not been moved up ", firstRow, validator.getDataSet().getChild(1));
      assertEquals("The second row has not been moved up ", secondRow, validator.getDataSet().getChild(3));
      SortedMap successors = OpGanttValidator.getSuccessors((XComponent) secondRow);
      assertTrue("The list of successors has not been updated ", successors.keySet().contains(new Integer(5)));
      SortedMap predecessors = OpGanttValidator.getPredecessors((XComponent) secondRow);
      assertTrue("The list of predecessors has not been updated ", predecessors.keySet().contains(new Integer(2)));
   }

   /**
    * Tests that the validator correctly moves a collection of  data rows downwords.
    *
    * @throws Exception if anything fails.
    */
   public void testMoveCollectionRowsDown()
        throws Exception {
      assertTestDataMinimumSize(5);
      ArrayList rowsToMove = new ArrayList();

      //add a collection
      int firstRowIndex = 2;
      XView firstRow = validator.getDataSet().getChild(firstRowIndex);
      rowsToMove.add(firstRow);
      int secondRowIndex = 3;
      XView secondRow = validator.getDataSet().getChild(secondRowIndex);
      rowsToMove.add(secondRow);

      validator.moveDataRows(rowsToMove, validator.getMovingOffset((XComponent) firstRow, (XComponent) secondRow, 1));
      assertEquals("The first row has not been moved up ", firstRow, validator.getDataSet().getChild(3));
      assertEquals("The second row has not been moved up ", secondRow, validator.getDataSet().getChild(4));
      SortedMap successors = OpGanttValidator.getSuccessors((XComponent) secondRow);
      assertTrue("The list of successors has not been updated ", successors.keySet().contains(new Integer(2)));
      SortedMap predecessors = OpGanttValidator.getPredecessors((XComponent) firstRow);
      assertTrue("The list of predecessors has not been updated ", predecessors.keySet().contains(new Integer(1)));

   }

   /**
    * Tests that the validator correctly moves a collection of data rows downwords another collection.
    *
    * @throws Exception if anything fails.
    */
   public void testMoveCollectionRowsDownOverAnotherCollectionRows()
        throws Exception {
      int dataSetSize = this.assertTestDataMinimumSize(4);

      ArrayList rowsToMove = new ArrayList();
      //add a collection
      int firstRowIndex = dataSetSize - 4;
      XView firstRow = validator.getDataSet().getChild(firstRowIndex);
      rowsToMove.add(firstRow);

      int secondRowIndex = dataSetSize - 3;
      XView secondRow = validator.getDataSet().getChild(secondRowIndex);
      rowsToMove.add(secondRow);

      validator.moveDataRows(rowsToMove, validator.getMovingOffset((XComponent) firstRow, (XComponent) secondRow, 1));
      assertEquals("The first row has not been moved up ", firstRow, validator.getDataSet().getChild(7));
      assertEquals("The second row has not been moved up ", secondRow, validator.getDataSet().getChild(8));
      SortedMap successors = OpGanttValidator.getSuccessors((XComponent) firstRow);
      assertEquals("The list of successors has not been updated ", successors, new ArrayList());
      SortedMap predecessors = OpGanttValidator.getPredecessors((XComponent) firstRow);
      assertTrue("The list of predecessors has not been updated ", predecessors.keySet().contains(new Integer(4)));

   }

   /**
    * Tests that the validator correctly moves data rows downwards.
    *
    * @throws Exception if anything fails.
    */
   public void testMoveRowsDown()
        throws Exception {
      int dataSetSize = this.assertTestDataMinimumSize(3);
      Map originalIndexMap = this.getIndexMap();

      ArrayList rowsToMove = new ArrayList();
      //add the last 2 rows and test that nothing happens
      rowsToMove.add(validator.getDataSet().getChild(dataSetSize - 2));
      rowsToMove.add(validator.getDataSet().getChild(dataSetSize - 1));
      validator.moveDataRows(rowsToMove, 1);
      Map movedIndexMap = this.getIndexMap();
      assertEquals("The rows new index shouldn't have moved ", originalIndexMap.keySet(), movedIndexMap.keySet());
      assertEquals("The rows original index shouldn't have moved ", new ArrayList(originalIndexMap.values()),
           new ArrayList(movedIndexMap.values()));

      //add the first row
      rowsToMove.clear();
      int firstRowIndex = 0;
      XView firstRow = validator.getDataSet().getChild(firstRowIndex);
      rowsToMove.add(firstRow);

      validator.moveDataRows(rowsToMove, 1);
      int newFirstRowIndex = firstRowIndex + 1;
      assertEquals("The first row has not been moved down ", firstRow,
           validator.getDataSet().getChild(newFirstRowIndex));
      SortedMap successors = OpGanttValidator.getSuccessors((XComponent) firstRow);
      assertTrue("The list of successors has not been updated ", successors.keySet().contains(new Integer(firstRowIndex)));
      SortedMap predecessors = OpGanttValidator.getPredecessors((XComponent) firstRow);
      assertTrue("The list of predecessors has been modified", predecessors.isEmpty());
   }


   /**
    * Tests that the validator correctly moves data rows downwards.
    *
    * @throws Exception if anything fails.
    */
   public void testMoveRowsDownOverCollection()
        throws Exception {
      int dataSetSize = this.assertTestDataMinimumSize(3);
      Map originalIndexMap = this.getIndexMap();

      ArrayList rowsToMove = new ArrayList();
      //add the last 2 rows and test that nothing happens
      rowsToMove.add(validator.getDataSet().getChild(dataSetSize - 2));
      rowsToMove.add(validator.getDataSet().getChild(dataSetSize - 1));
      validator.moveDataRows(rowsToMove, 1);
      Map movedIndexMap = this.getIndexMap();
      assertEquals("The rows new index shouldn't have moved ", originalIndexMap.keySet(), movedIndexMap.keySet());
      assertEquals("The rows original index shouldn't have moved ", new ArrayList(originalIndexMap.values()),
           new ArrayList(movedIndexMap.values()));

      //add the first row
      rowsToMove.clear();
      int firstRowIndex = 0;
      XView firstRow = validator.getDataSet().getChild(firstRowIndex);
      rowsToMove.add(firstRow);
      int secondRowIndex = 1;
      XView secondRow = validator.getDataSet().getChild(secondRowIndex);
      rowsToMove.add(secondRow);

      //move the activities over the bellow collection
      int offset = validator.getMovingOffset((XComponent) firstRow, (XComponent) secondRow, 1);
      validator.moveDataRows(rowsToMove, offset);
      int newFirstRowIndex = firstRowIndex + offset;
      assertEquals("The first row has not been moved down ", firstRow,
           validator.getDataSet().getChild(newFirstRowIndex));
      int newSecondRowIndex = secondRowIndex + offset;
      assertEquals("The second row has not been moved up ", secondRow,
           validator.getDataSet().getChild(newSecondRowIndex));
      SortedMap successors = OpGanttValidator.getSuccessors((XComponent) firstRow);
      assertTrue("The list of successors has not been updated ", successors.keySet().contains(new Integer(newSecondRowIndex)));
      SortedMap predecessors = OpGanttValidator.getPredecessors((XComponent) secondRow);
      assertTrue("The list of predecessors has not been updated ", predecessors.keySet().contains(new Integer(newFirstRowIndex)));
   }

   /**
    * Checks if the <code>activity</code> is a <code>COLLECTION</code> activity
    *
    * @param activity an <code>XComponent</code> repsesenting the activity that should be a <code>COLLECTION</code>
    * @return String representing the name of the collection activity
    */
   private String assertTestActivityIsCollection(XComponent activity) {
      String activityName = OpGanttValidator.getName(activity);
      assertEquals("The activity " + activityName + " is not a collection", OpGanttValidator.getType(activity), OpGanttValidator.COLLECTION);
      return activityName;
   }

   /**
    * Tests if the validator returns the accurate children of a <code>COLLECTION</code> activity.
    *
    * @throws Exception if anything fails.
    */
   public void testCollectionActivityChildren()
        throws Exception {
      int dataSetSize = this.assertTestDataMinimumSize(3);

      int rowIndex = dataSetSize - 2;
      XComponent activity = (XComponent) validator.getDataSet().getChild(rowIndex);
      /*checks if the activity is a collection*/
      String activityName = assertTestActivityIsCollection(activity);
      /*get the activity children*/
      List childrenList = validator.getChildren(activity);
      /*the expected children List*/
      List expectedList = new ArrayList();
      expectedList.add(validator.getDataSet().getChild(dataSetSize - 1));

      assertEquals("The collection activity " + activityName + " has wrong number of children ", childrenList.size(),
           expectedList.size());
      assertEquals("The collection activity " + activityName + " has wrong children ", childrenList, expectedList);
   }

   /**
    * Tests for an activity that becomes a <code>COLLECTION</code> that you can't modify it's fallowing properies:
    * <code>START_COLUMN_INDEX</code>,<code>END_COLUMN_INDEX</code>, <code>DURATION_COLUMN_INDEX</code> and
    * <code>BASE_EFFORT_COLUMN_INDEX</code>
    *
    * @throws Exception if anything fails.
    */
   public void testCollectionActivityEditableMode()
        throws Exception {
      assertTestDataMinimumSize(4);

      //break link between 0 and 1 (in order to be able to make 1 a child of 0)
      validator.removeLink(0, 1);
      int rowIndex = 0;
      XComponent activity = (XComponent) validator.getDataSet().getChild(rowIndex);
      /*the array of componets that will become children of the activity*/
      ArrayList children = new ArrayList();
      children.add(validator.getDataSet().getChild(1));
      /*change the outline of the children*/
      validator.changeOutlineLevels(children, 1);
      /*checks if the activity is a collection*/
      String activityName = assertTestActivityIsCollection(activity);

      assertFalse("The collection activity " + activityName + " Start Date is editable",
           activity.getChild(OpGanttValidator.START_COLUMN_INDEX).getEnabled());
      assertFalse("The collection activity " + activityName + " End Date is editable",
           activity.getChild(OpGanttValidator.FINISH_COLUMN_INDEX).getEnabled());
      assertFalse("The collection activity " + activityName + " Duration is editable",
           activity.getChild(OpGanttValidator.DURATION_COLUMN_INDEX).getEnabled());
      assertFalse("The collection activity " + activityName + " Base Effort is editable",
           activity.getChild(OpGanttValidator.BASE_EFFORT_COLUMN_INDEX).getEnabled());
   }

   /**
    * Returns a map with the position of each data row in the validator, and its current order.
    *
    * @return a <code>Map</code> of <code>[Integer, Integer]</code> pairs representing [current_dataset_index, original_index]
    *         pairs.
    */
   private Map getIndexMap() {
      Map result = new HashMap(validator.getDataSet().getChildCount());
      for (int i = 0; i < validator.getDataSet().getChildCount(); i++) {
         XComponent dataRow = (XComponent) validator.getDataSet().getChild(i);
         result.put(new Integer(i), new Integer(dataRow.getIndex()));
      }
      return result;
   }

   /**
    * Tests that you cannot increase or decrease the outline level of the first activity in the  data set.
    * Tests that you cannot decrease the outline level of the last activity below 0.
    *
    * @throws Exception if anything fails.
    */
   public void testChangeOutlineLevelNoop()
        throws Exception {
      int dataSetSize = this.assertTestDataMinimumSize(3);

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      int originalOutlineLevel = firstActivity.getOutlineLevel();
      ArrayList toChangeLevel = new ArrayList();
      toChangeLevel.add(firstActivity);
      validator.changeOutlineLevels(toChangeLevel, 1);
      assertEquals("The outline level of the first activity shouldn't have changed", originalOutlineLevel, firstActivity.getOutlineLevel());
      validator.changeOutlineLevels(toChangeLevel, 1);
      assertEquals("The outline level of the first activity shouldn't have changed", originalOutlineLevel, firstActivity.getOutlineLevel());
      XComponent lastActivity = (XComponent) validator.getDataSet().getChild(dataSetSize - 1);
      toChangeLevel.clear();
      toChangeLevel.add(lastActivity);
      validator.changeOutlineLevels(toChangeLevel, -1);
      assertEquals("The outline level of the last activity shouldn't have changed", originalOutlineLevel, firstActivity.getOutlineLevel());
   }

   /**
    * Tests that when the outline level of a collection increases, that collection becomes a child of its original parent
    * which also becomes a collection.
    *
    * @throws Exception if anything fails.
    */
   public void testChangeOutlineLevelCollectionInCollection()
        throws Exception {
      this.assertTestDataMinimumSize(3);

      //break link 1->2 and 1->3 (in order to be able to make 2 a child of 1)
      validator.removeLink(1, 2);
      validator.removeLink(1, 3);

      XComponent collectionActivity = (XComponent) validator.getDataSet().getChild(2);
      int collectionOutlineLevel = collectionActivity.getOutlineLevel();
      XComponent nonCollectionActivity = (XComponent) validator.getDataSet().getChild(collectionActivity.getIndex() - 1);
      int originalOutlineLevel = nonCollectionActivity.getOutlineLevel();
      assertEquals("The activity should not be part of the collection", collectionActivity.getOutlineLevel(),
           nonCollectionActivity.getOutlineLevel());
      ArrayList toChangeLevel = new ArrayList();
      toChangeLevel.add(collectionActivity);
      XComponent child = (XComponent) validator.getDataSet().getChild(collectionActivity.getIndex() + 1);
      toChangeLevel.add(child);
      validator.changeOutlineLevels(toChangeLevel, 1);
      assertEquals("The collection activity should not have changed its type", OpGanttValidator.COLLECTION,
           OpGanttValidator.getType(collectionActivity));
      assertEquals("The collection activity should have changed its outline level", collectionOutlineLevel + 1,
           collectionActivity.getOutlineLevel());
      assertEquals("The non-collection activity should have changed its type", OpGanttValidator.COLLECTION,
           OpGanttValidator.getType(nonCollectionActivity));
      assertEquals("The non-collection activity should not have changed its outline level", originalOutlineLevel,
           nonCollectionActivity.getOutlineLevel());
   }

   /**
    * Tests that when increasing the outline level of a standard activity will cause the previous activity
    * in the data set to become a collection activity.
    *
    * @throws Exception if anything fails.
    */
   public void testChangeOutlineLevelStandardBecomesCollection()
        throws Exception {
      this.assertTestDataMinimumSize(3);

      //break link between 0 and 1 (in order to be able to make 1 child of 0)
      validator.removeLink(0, 1);
      XComponent collectionActivity = (XComponent) validator.getDataSet().getChild(0);
      assertEquals("The type of the activity should be standard", OpGanttValidator.STANDARD,
           OpGanttValidator.getType(collectionActivity));
      int collectionOutlineLevel = collectionActivity.getOutlineLevel();
      XComponent nonCollectionActivity = (XComponent) validator.getDataSet().getChild(1);
      int originalOutlineLevel = nonCollectionActivity.getOutlineLevel();
      assertEquals("The outline level of the to-be collection is wrong", 0, collectionOutlineLevel);
      assertEquals("The activity should not be part of the collection", collectionActivity.getOutlineLevel(),
           nonCollectionActivity.getOutlineLevel());
      ArrayList toChangeLevel = new ArrayList();
      toChangeLevel.add(nonCollectionActivity);
      validator.changeOutlineLevels(toChangeLevel, 1);
      assertEquals("The collection activity should have changed its type", OpGanttValidator.COLLECTION,
           OpGanttValidator.getType(collectionActivity));
      assertEquals("The collection activity should not have changed its outline level", collectionOutlineLevel,
           collectionActivity.getOutlineLevel());
      assertEquals("The non-collection activity should not have changed its type", OpGanttValidator.STANDARD,
           OpGanttValidator.getType(nonCollectionActivity));
      assertEquals("The non-collection activity should have changed its outline level", originalOutlineLevel + 1,
           nonCollectionActivity.getOutlineLevel());
   }

   /**
    * Test that increasing the outline level of a milestone will make the milestone a child of its
    * original parent which was a collection.
    *
    * @throws Exception if anything fails.
    */
   public void testChangeOutlineLevelMilestoneInCollection()
        throws Exception {
      this.assertTestDataMinimumSize(3);

      ArrayList toChangeLevel = new ArrayList();
      XComponent collectionActivity = validator.getDataSet().getForm().findComponent(COLLECTION_ID);
      int collectionOutlineLevel = collectionActivity.getOutlineLevel();
      XComponent nonCollectionActivity = (XComponent) validator.getDataSet().getChild(collectionActivity.getIndex() + 2);
      int originalOutlineLevel = nonCollectionActivity.getOutlineLevel();
      assertEquals("The activity should not be part of the collection", collectionActivity.getOutlineLevel(),
           nonCollectionActivity.getOutlineLevel());
      toChangeLevel.add(nonCollectionActivity);
      validator.changeOutlineLevels(toChangeLevel, 1);
      assertEquals("The collection activity should not have changed its type", OpGanttValidator.COLLECTION,
           OpGanttValidator.getType(collectionActivity));
      assertEquals("The collection activity should not have changed its outline level", collectionOutlineLevel,
           collectionActivity.getOutlineLevel());
      assertEquals("The non-collection activity should not have changed its type", OpGanttValidator.MILESTONE,
           OpGanttValidator.getType(nonCollectionActivity));
      assertEquals("The non-collection activity should have changed its outline level", originalOutlineLevel + 1,
           nonCollectionActivity.getOutlineLevel());
   }

   /**
    * Tests that when decreasing the outline level of the only activity within a collection, the parent which used to be
    * a collection is transformed into a standard activity.
    *
    * @throws Exception if anything fails.
    */
   public void testChangeOutlineLevelPullbackCollection()
        throws Exception {
      this.assertTestDataMinimumSize(3);

      XComponent mileStone = validator.getDataSet().getForm().findComponent(MILESTONE_ID);
      mileStone.setOutlineLevel(mileStone.getOutlineLevel() + 1);

      XComponent collectionActivity = validator.getDataSet().getForm().findComponent(COLLECTION_ID);
      assertTrue("The milestone should be a child of the collection ", collectionActivity.getOutlineLevel() == mileStone.getOutlineLevel() - 1);

      XComponent standardActivity = (XComponent) validator.getDataSet().getChild(collectionActivity.getIndex() + 1);
      assertEquals("The standard activity should have the same outline level as the milestone", mileStone.getOutlineLevel(),
           standardActivity.getOutlineLevel());

      ArrayList toChange = new ArrayList();
      toChange.add(mileStone);
      validator.changeOutlineLevels(toChange, -1);
      assertEquals("The milestone and the collection activity should have the same outline level ",
           collectionActivity.getOutlineLevel(), mileStone.getOutlineLevel());
      assertEquals("The type of the collection activity shouldn't have changed", OpGanttValidator.COLLECTION,
           OpGanttValidator.getType(collectionActivity));
      toChange.clear();
      toChange.add(standardActivity);
      validator.changeOutlineLevels(toChange, -1);
      assertEquals("The standard and the collection activity should have the same outline level ",
           collectionActivity.getOutlineLevel(), standardActivity.getOutlineLevel());
      assertEquals("The type of the collection activity should have changed to standard", OpGanttValidator.STANDARD,
           OpGanttValidator.getType(collectionActivity));
   }

   /**
    * Tests that when increasing the outline level of an activity below a milestone, the milestone cannot become a collection.
    *
    * @throws Exception if anything fails.
    */
   public void testChangeOutlineLevelMilestoneCannotBecomeCollection()
        throws Exception {
      this.assertTestDataMinimumSize(3);

      XComponent mileStone = validator.getDataSet().getForm().findComponent(MILESTONE_ID);
      XComponent standardActivity = (XComponent) validator.getDataSet().getChild(mileStone.getIndex() + 1);
      int originalOutlineLevel = standardActivity.getOutlineLevel();
      assertEquals("The milestone and the standard activity should be on the same level", mileStone.getOutlineLevel(),
           standardActivity.getOutlineLevel());
      ArrayList toChange = new ArrayList();
      toChange.add(standardActivity);
      try {
         validator.changeOutlineLevels(toChange, 1);
         fail("Milestone activity became a collection");
      }
      catch (XValidationException e) {
         //milestones can't become collections
      }
      assertEquals("The standard activity changed its outline level", originalOutlineLevel, standardActivity.getOutlineLevel());
      assertEquals("The milestone changed its type", OpGanttValidator.MILESTONE, OpGanttValidator.getType(mileStone));
   }

   /**
    * Tests that a list of activites is correctly copied to the clipboard.
    *
    * @throws Exception if anything fails.
    */
   public void testCopyToClipboard()
        throws Exception {
      this.assertTestDataMinimumSize(2);
      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      XComponent secondActivity = (XComponent) validator.getDataSet().getChild(1);
      firstActivity.setStringValue("OpActivity.1.xid");
      secondActivity.setStringValue("OpActivity.2.xid");
      ArrayList toCopy = new ArrayList();
      toCopy.add(firstActivity);
      toCopy.add(secondActivity);
      validator.copyToClipboard(toCopy);

      XComponent clipBoard = XDisplay.getClipboard();
      assertEquals("The clipboard should have 2 items", 2, clipBoard.getChildCount());
      //check for null value assigned to activity data row
      for (int index = 0; index < clipBoard.getChildCount(); index++) {
         Object activityValue = ((XComponent) clipBoard.getChild(index)).getValue();
         assertNull("The activity" + index + " value is not wrong", activityValue);
      }

      List expectedSuccessors;
      List expectedPredecessors;
      //expected successors and predecessors for first activity in clipboard
      expectedSuccessors = new ArrayList();
      expectedSuccessors.add(new Integer(1));

      expectedPredecessors = new ArrayList();
      assertActivitiesEqualAfterCutCopyPaste(firstActivity, (XComponent) clipBoard.getChild(0), expectedSuccessors, expectedPredecessors);

      expectedSuccessors.clear();
      expectedPredecessors.clear();
      //expected successors and predecessors for second activity in clipboard
      expectedSuccessors = new ArrayList();
      expectedPredecessors = new ArrayList();

      expectedPredecessors.add(new Integer(0));
      assertActivitiesEqualAfterCutCopyPaste(secondActivity, (XComponent) clipBoard.getChild(1), expectedSuccessors, expectedPredecessors);

   }

   /**
    * Compares 2 activities to see if they are equal, based on: number of children for each (data-cells), list of successors,
    * list of predecessors, value type and value of each children.
    *
    * @param originalActivity a <code>XComponent</code> representing the reference activity.
    * @param modifiedActivity a <code>XComponent</code> representing the activity being checked.
    */
   private void assertActivitiesEqual(XComponent originalActivity, XComponent modifiedActivity) {
      assertEquals("The 2 activites don't have the same nr of children", originalActivity.getChildCount(),
           modifiedActivity.getChildCount());
      assertEquals("The 2 activities don't have the same successors", OpGanttValidator.getSuccessors(originalActivity),
           OpGanttValidator.getSuccessors(modifiedActivity));
      assertEquals("The 2 activities don't have the same predecessors", OpGanttValidator.getPredecessors(originalActivity),
           OpGanttValidator.getPredecessors(modifiedActivity));
      for (int i = 0; i < originalActivity.getChildCount(); i++) {
         XComponent originalDataCell = (XComponent) originalActivity.getChild(i);
         XComponent modifiedDataCell = (XComponent) modifiedActivity.getChild(i);
         assertEquals("The value type of the data cells doesn't match", originalDataCell.getValueType(), modifiedDataCell.getValueType());
         assertEquals("The value of the data cells doesn't match", originalDataCell.getValue(), modifiedDataCell.getValue());
      }
   }

   /**
    * Tests that an array of data-rows is transfered to the clipboard.
    *
    * @throws Exception if anything fails.
    */
   public void testCutToClipboard()
        throws Exception {
      int size = this.assertTestDataMinimumSize(2);

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      XComponent secondActivity = (XComponent) validator.getDataSet().getChild(1);
      firstActivity.setStringValue("OpActivity.1.xid");
      secondActivity.setStringValue("OpActivity.2.xid");

      ArrayList toCut = new ArrayList();
      toCut.add(firstActivity);
      toCut.add(secondActivity);

      validator.cutToClipboard(toCut);
      String firstActivityName = OpGanttValidator.getName(firstActivity);
      String secondActivityName = OpGanttValidator.getName(secondActivity);

      assertEquals("The data was not removed from the underlying dataset", size - 2, validator.getDataSet().getChildCount());
      XComponent clipboard = XDisplay.getClipboard();
      assertEquals("The clipboard doesn't contain the cut data", 2, clipboard.getChildCount());

      //check for not null value assigned to activity data row
      for (int index = 0; index < clipboard.getChildCount(); index++) {
         Object activityValue = ((XComponent) clipboard.getChild(index)).getValue();
         assertNotNull("The activity " + index + "'s value is not wrong", activityValue);
      }

      assertEquals("The first activity from the clipboard is not correct", firstActivityName,
           OpGanttValidator.getName((XComponent) clipboard.getChild(0)));
      assertEquals("The second activity from the clipboard is not correct", secondActivityName,
           OpGanttValidator.getName((XComponent) clipboard.getChild(1)));
   }

   /**
    * Compares 2 activities to see if they are equal, based on: number of children for each (data-cells),value type and
    * value of each children (except successsors and predecessors list).One of the two activities represents the checked
    * activity. Becouse, during the cut/copy/paste operation the successors and predecessors of the <code>modifiedActivity<code>
    * are changed the comparison of these is made separately.
    *
    * @param originalActivity    a <code>XComponent</code> representing the reference activity.
    * @param modifiedActivity    a <code>XComponent</code> representing the checked activity.
    * @param expectedSuccesors   a <code>List<code> of expected successors of the <code>modifiedActivity</code>
    * @param expectedPredecessor a <code>List<code> of expected predecessors of the <code>modifiedActivity</code>
    */
   private void assertActivitiesEqualAfterCutCopyPaste(XComponent originalActivity, XComponent modifiedActivity,
        List expectedSuccesors, List expectedPredecessor) {

      assertEquals("The 2 activites don't have the same nr of children", originalActivity.getChildCount(),
           modifiedActivity.getChildCount());
      for (int i = 0; i < originalActivity.getChildCount(); i++) {
         XComponent originalDataCell = (XComponent) originalActivity.getChild(i);
         XComponent modifiedDataCell = (XComponent) modifiedActivity.getChild(i);
         assertEquals("The value type of the data cells doesn't match", originalDataCell.getValueType(), modifiedDataCell.getValueType());
         if ((modifiedDataCell.getValue() instanceof ArrayList) && (i == OpGanttValidator.SUCCESSORS_COLUMN_INDEX)) {
            assertEquals("The list of succesors doesn't match", expectedSuccesors, modifiedDataCell.getValue());
         }
         else {
            if ((modifiedDataCell.getValue() instanceof ArrayList) && (i == OpGanttValidator.PREDECESSORS_COLUMN_INDEX)) {
               assertEquals("The list of predecessors doesn't match", expectedPredecessor, modifiedDataCell.getValue());
            }
            else {
               Object origValue = originalDataCell.getValue();
               Object modifValue = modifiedDataCell.getValue();
               if (origValue instanceof ArrayList) {
                  assertEquals("The list value of the data cells doesn't match", origValue, modifValue);
               }
               else {
                  assertEquals("The value of the data cells doesn't match", origValue, modifValue);
               }
            }
         }
      }

   }

   /**
    * Tests that pasting data rows from the clipboard works.
    *
    * @throws Exception if anything fails.
    */
   public void testPasteFromClipboard()
        throws Exception {
      int size = this.assertTestDataMinimumSize(2);
      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      //update with its own duration in order to have the right work phases
      validator.updateDuration(firstActivity, OpGanttValidator.getDuration(firstActivity), true);
      XComponent secondActivity = (XComponent) validator.getDataSet().getChild(1);

      ArrayList selectedRows = new ArrayList();
      selectedRows.add(firstActivity);
      validator.cutToClipboard(selectedRows);
      selectedRows.clear();
      //where to paste
      selectedRows.add(secondActivity);
      validator.pasteFromClipboard(selectedRows, false);

      assertEquals("The validator has the wrong number of activities", size - 1, validator.getDataSet().getChildCount());
      XComponent retrievedActivity = (XComponent) validator.getDataSet().getChild(0);
      List expectedSuccessors = new ArrayList();
      List expectedPredecessors = new ArrayList();

      if (validator.isProgressTracked()) {
         //complete will be set to 0 by the paste operation
         assertEquals("Complete was not set to 0", OpGanttValidator.getComplete(retrievedActivity), 0, 0);
         OpGanttValidator.setComplete(retrievedActivity, OpGanttValidator.getComplete(firstActivity));
      }
      assertActivitiesEqualAfterCutCopyPaste(firstActivity, retrievedActivity, expectedSuccessors, expectedPredecessors);

      XView nameCell = retrievedActivity.getChild(OpGanttValidator.NAME_COLUMN_INDEX);
      XView startDateCell = retrievedActivity.getChild(OpGanttValidator.START_COLUMN_INDEX);
      XView endDateCell = retrievedActivity.getChild(OpGanttValidator.FINISH_COLUMN_INDEX);
      XView durationCell = retrievedActivity.getChild(OpGanttValidator.DURATION_COLUMN_INDEX);
      XView baseEffortCell = retrievedActivity.getChild(OpGanttValidator.BASE_EFFORT_COLUMN_INDEX);

      assertTrue("The activity's Name Cell is not editable", nameCell.getEnabled());
      assertTrue("The activity's Start Date cell is not editable", startDateCell.getEnabled());
      assertTrue("The activity's End Date cell is not editable", endDateCell.getEnabled());
      assertTrue("The activity's Duration cell is not editable", durationCell.getEnabled());
      assertTrue("The activity's Base Effort cell is not editable", baseEffortCell.getEnabled());
   }

   /**
    * Tests that pasting data rows  from the clipboard works.
    *
    * @throws Exception if anything fails.
    */
   public void testPasteCollectionFromClipboard()
        throws Exception {
      int size = this.assertTestDataMinimumSize(3);

      //initial validation of data set
      validator.validateDataSet();

      XComponent whereToPasteActivity = (XComponent) validator.getDataSet().getChild(0);
      //the activity which becomes a collection
      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(2);
      //the child of the collection
      XComponent secondActivity = (XComponent) validator.getDataSet().getChild(3);
      ArrayList selectedRows = new ArrayList();
      selectedRows.add(secondActivity);
      validator.changeOutlineLevels(selectedRows, 1);
      selectedRows.clear();

      //selected rows for copy
      selectedRows.add(firstActivity);
      selectedRows.add(secondActivity);
      validator.copyToClipboard(selectedRows);
      selectedRows.clear();

      //where to paste
      selectedRows.add(whereToPasteActivity);
      validator.pasteFromClipboard(selectedRows, false);

      assertEquals("The validator has the wrong number of activities", size + 1, validator.getDataSet().getChildCount());
      XComponent retrievedActivity = (XComponent) validator.getDataSet().getChild(1);
      List expectedSuccessors = new ArrayList();
      List expectedPredecessors = new ArrayList();
      //set up the new index after paste operation
      if (validator.isProgressTracked()) {
         //complete will be set to 0 by the paste operation
         assertEquals("Complete was not set to 0", OpGanttValidator.getComplete(retrievedActivity), 0, 0);
         OpGanttValidator.setComplete(retrievedActivity, OpGanttValidator.getComplete(secondActivity));
      }
      assertActivitiesEqualAfterCutCopyPaste(secondActivity, retrievedActivity, expectedSuccessors, expectedPredecessors);
   }

   /**
    * Tests the if the workphase are computed (when an update method is called) corectly.
    * Should be the same mechanism behind updateBaseEffort, updateDuration and updateFinish.
    */
   public void testWorkPhases() {
      XComponent testedActivity = (XComponent) validator.getDataSet().getChild(0);
      //start 04.10.2004
      //end 29.10.2004
      //working hours/day -> 8
      validator.updateBaseEffort(testedActivity, 20 * 8);

      //4 work phases
      ArrayList expectedStarts = new ArrayList();
      Calendar defCalendar = OpProjectCalendar.getDefaultProjectCalendar().getCalendar();
      defCalendar.set(2004, Calendar.OCTOBER, 4);
      expectedStarts.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 11);
      expectedStarts.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 18);
      expectedStarts.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 25);
      expectedStarts.add(defCalendar.getTime());

      ArrayList expectedEnds = new ArrayList();
      defCalendar = OpProjectCalendar.getDefaultProjectCalendar().getCalendar();
      defCalendar.set(2004, Calendar.OCTOBER, 8);
      expectedEnds.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 15);
      expectedEnds.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 22);
      expectedEnds.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 29);
      expectedEnds.add(defCalendar.getTime());

      ArrayList expectedEfforts = new ArrayList();
      expectedEfforts.add(new Double(40));
      expectedEfforts.add(new Double(40));
      expectedEfforts.add(new Double(40));
      expectedEfforts.add(new Double(40));

   }

   /**
    * Tests the if the workphase are computed (when an update method is called) corectly.
    * Should be the same mechanism behind updateBaseEffort, updateDuration and updateFinish.
    */
   public void testWorkPhasesIndependent() {

      setIndependentEffort();

      XComponent testedActivity = (XComponent) validator.getDataSet().getChild(0);
      //start 04.10.2004
      //end 29.10.2004
      //working hours/day -> 8
      validator.updateDuration(testedActivity, 20 * 8, true);
      validator.updateBaseEffort(testedActivity, 20 * 8);

      //4 work phases
      ArrayList expectedStarts = new ArrayList();
      Calendar defCalendar = OpProjectCalendar.getDefaultProjectCalendar().getCalendar();
      defCalendar.set(2004, Calendar.OCTOBER, 4);
      expectedStarts.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 11);
      expectedStarts.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 18);
      expectedStarts.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 25);
      expectedStarts.add(defCalendar.getTime());

      ArrayList expectedEnds = new ArrayList();
      defCalendar = OpProjectCalendar.getDefaultProjectCalendar().getCalendar();
      defCalendar.set(2004, Calendar.OCTOBER, 8);
      expectedEnds.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 15);
      expectedEnds.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 22);
      expectedEnds.add(defCalendar.getTime());
      defCalendar.set(2004, Calendar.OCTOBER, 29);
      expectedEnds.add(defCalendar.getTime());

      ArrayList expectedEfforts = new ArrayList();
      expectedEfforts.add(new Double(40));
      expectedEfforts.add(new Double(40));
      expectedEfforts.add(new Double(40));
      expectedEfforts.add(new Double(40));

      //update the duration - double -> eff / workphase should be 1/2
      double duration = OpGanttValidator.getDuration(testedActivity);
      validator.updateDuration(testedActivity, 2 * duration, true);
      expectedEfforts = new ArrayList();
      expectedEfforts.add(new Double(20));
      expectedEfforts.add(new Double(20));
      expectedEfforts.add(new Double(20));
      expectedEfforts.add(new Double(20));
   }

   /**
    * Tests if the resource based efforts are computed corectly for an activity (when updating the effort)
    */
   public void testResourceBasedEffort() {
      XComponent testedActivity = (XComponent) validator.getDataSet().getChild(0);

      ArrayList resources = new ArrayList();
      resources.add(WORKER2);
      resources.add(WORKER1_REGEXP.replaceAll("#", "50%"));
      validator.setDataCellValue(testedActivity, OpGanttValidator.VISUAL_RESOURCES_COLUMN_INDEX, resources);

      //a week (5 days) (res1 - eff 40), (res2 - eff 20)
      validator.setDataCellValue(testedActivity, OpGanttValidator.BASE_EFFORT_COLUMN_INDEX, new Double(60.0));

      Map efforts = OpGanttValidator.getResourceBaseEfforts(testedActivity);
      assertEquals("Effort for resource 1 is not correct", 40,
           ((Double) efforts.get(WORKER1_ID)).doubleValue(), DOUBLE_ERROR_MARGIN);
      assertEquals("Effort for resource 2 is not correct", 20,
           ((Double) efforts.get(WORKER2_ID)).doubleValue(), DOUBLE_ERROR_MARGIN);
   }


   /**
    *
    */
   public void testUpdateDurationIndependent() {

      setIndependentEffort();

      XComponent firstActivity = (XComponent) validator.getDataSet().getChild(0);
      double effort = OpGanttValidator.getBaseEffort(firstActivity);
      double duration = OpGanttValidator.getDuration(firstActivity);
      Date start = OpGanttValidator.getStart(firstActivity);
      Date end = OpGanttValidator.getEnd(firstActivity);
      double newEffort;
      double newDuration;
      Date newEnd;
      Date newStart;
      long expectedTime;

      //change to the same duration => nothing should change
      validator.updateDuration(firstActivity, duration, true);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", effort, newEffort, 0);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", duration, newDuration, 0);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The new start Date was not set correctly ", start, newStart);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", end, newEnd);

      //double duration, and no resource assigned  => effort won't change
      validator.updateDuration(firstActivity, 2 * duration, true);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 2 * duration, newDuration, 0);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", effort, newEffort, 0);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The new start Date was not set correctly ", start, newStart);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      expectedTime = getEndTimeForDuration(start, newDuration);
      assertEquals("The new end Date was not set correctly ", new Date(expectedTime), newEnd);

      //set duration to <0 => duration=0, effort won't change
      validator.updateDuration(firstActivity, -100, true);

      newDuration = OpGanttValidator.getDuration(firstActivity);
      assertEquals("The new duration was not set correctly ", 0, newDuration, 0);

      newEffort = OpGanttValidator.getBaseEffort(firstActivity);
      assertEquals("The new effort was not set correctly ", effort, 160, 0);

      newStart = OpGanttValidator.getStart(firstActivity);
      assertEquals("The new start Date was not set correctly ", start, newStart);

      newEnd = OpGanttValidator.getEnd(firstActivity);
      assertEquals("The new end Date was not set correctly ", start, newEnd);

   }

   /**
    * Add an hourly rate period for resource 1 from start to end
    *
    * @param start  - the start of the hourly rates period
    * @param finish - the end of hourly rates period
    */
   private void addRatesToWorker1(Date start, Date finish) {
      XComponent hourlyRates = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(WORKER1_ID);
      Map rates = new TreeMap();
      List ratesList = new ArrayList();
      ratesList.add(new Double(10));
      ratesList.add(new Double(20));
      rates.put(new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()), ratesList);
      ratesList = new ArrayList();
      ratesList.add(new Double(3));
      ratesList.add(new Double(4));
      rates.put(start, ratesList);
      ratesList = new ArrayList();
      ratesList.add(new Double(10));
      ratesList.add(new Double(20));
      Calendar calendar = OpProjectCalendar.setCalendarTimeToZero(finish);
      calendar.add(Calendar.DATE, 1);
      rates.put(new Date(calendar.getTimeInMillis()), ratesList);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(rates);
      dataRow.addChild(dataCell);
      hourlyRates.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(WORKER2_ID);
      rates = new TreeMap();
      ratesList = new ArrayList();
      ratesList.add(new Double(10));
      ratesList.add(new Double(20));
      rates.put(new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis()), ratesList);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(rates);
      dataRow.addChild(dataCell);
      hourlyRates.addChild(dataRow);
      validator.setHourlyRatesDataSet(hourlyRates);
   }

   /**
    * Change the availability of the two resources to the values specified by the parameters
    *
    * @param availabilityWorker1 - the new availability for WORKER1 (in %)
    * @param availabilityWorker2 - the new availability for WORKER2 (in %)
    */
   private void changeAvailabilityForResources(Double availabilityWorker1, Double availabilityWorker2) {
      XComponent assignmentSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(WORKER1);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(availabilityWorker1);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(250);
      dataRow.addChild(dataCell);
      assignmentSet.addChild(dataRow);
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(WORKER2);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(availabilityWorker2);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(250);
      dataRow.addChild(dataCell);
      assignmentSet.addChild(dataRow);
      validator.setAssignmentSet(assignmentSet);
   }

   /**
    * Creates a new empty <code>XComponent.DATA_ROW</code> component
    *
    * @return the created data row
    */
   private XComponent createNewDataRow() {
      return validator.newDataRow();
   }

   /**
    * Validation test created from issue OPP-18 (http://issues.onepoint.at/jira/browse/OPP-18).
    *
    * @throws Exception if anything unexpected fails.
    */
   public void testValidation1()
        throws Exception {
      XComponent validationDataSet = getTestDataSet("validation1TestData.xml");
      validator.setDataSet(validationDataSet);
      XComponent newActivity = this.createNewDataRow();
      //add a new data row (performs validation)
      validator.addDataRow(newActivity);
      assertEquals("Invalid number of activities:", 5, validationDataSet.getChildCount());

      XComponent activity = (XComponent) validationDataSet.getChild(0);
      String name = OpGanttValidator.getName(activity);
      assertEquals("The activity " + name + " has the wrong type:", OpGanttValidator.COLLECTION,
           OpGanttValidator.getType(activity));
      assertEquals("The activity " + name + " has the wrong number of children:",
           2, activity.getSubRows().size());
      assertEquals("The activity " + name + " has the wrong start date:",
           this.getGMTDate(11, 6, 2007), OpGanttValidator.getStart(activity));
      assertEquals("The activity " + name + " has the wrong end date:",
           this.getGMTDate(22, 6, 2007), OpGanttValidator.getEnd(activity));
      assertEquals("The activity " + name + " has the wrong duration:",
           80.0, OpGanttValidator.getDuration(activity));
      assertEquals("The activity " + name + " has the wrong effort:",
           80.0, OpGanttValidator.getBaseEffort(activity));
      assertEquals("The activity " + name + " has the wrong successors list:", 1,
           OpGanttValidator.getSuccessors(activity).size());
      assertEquals("The activity " + name + " has the wrong successor:",
           3, ((Integer) OpGanttValidator.getSuccessors(activity).get(0)).intValue());

      activity = (XComponent) validationDataSet.getChild(1);
      name = OpGanttValidator.getName(activity);
      assertEquals("The activity " + name + " has the wrong type:", OpGanttValidator.STANDARD,
           OpGanttValidator.getType(activity));
      assertEquals("The activity " + name + " has the wrong number of children:",
           0, activity.getSubRows().size());
      assertEquals("The activity " + name + " has the wrong start date:",
           this.getGMTDate(11, 6, 2007), OpGanttValidator.getStart(activity));
      assertEquals("The activity " + name + " has the wrong end date:",
           this.getGMTDate(15, 6, 2007), OpGanttValidator.getEnd(activity));
      assertEquals("The activity " + name + " has the wrong duration:",
           40.0, OpGanttValidator.getDuration(activity));
      assertEquals("The activity " + name + " has the wrong effort:",
           40.0, OpGanttValidator.getBaseEffort(activity));
      assertEquals("The activity " + name + " has the wrong successors list:", 1, OpGanttValidator.getSuccessors(activity).size());
      assertEquals("The activity " + name + " has the wrong successor:",
           2, ((Integer) OpGanttValidator.getSuccessors(activity).get(0)).intValue());

      activity = (XComponent) validationDataSet.getChild(2);
      name = OpGanttValidator.getName(activity);
      assertEquals("The activity " + name + " has the wrong type:", OpGanttValidator.STANDARD,
           OpGanttValidator.getType(activity));
      assertEquals("The activity " + name + " has the wrong number of children:",
           0, activity.getSubRows().size());
      assertEquals("The activity " + name + " has the wrong start date:",
           this.getGMTDate(18, 6, 2007), OpGanttValidator.getStart(activity));
      assertEquals("The activity " + name + " has the wrong end date:",
           this.getGMTDate(22, 6, 2007), OpGanttValidator.getEnd(activity));
      assertEquals("The activity " + name + " has the wrong duration:",
           40.0, OpGanttValidator.getDuration(activity));
      assertEquals("The activity " + name + " has the wrong effort:",
           40.0, OpGanttValidator.getBaseEffort(activity));
      assertEquals("The activity " + name + " has the wrong predecessor list:", 1,
           OpGanttValidator.getPredecessors(activity).size());
      assertEquals("The activity " + name + " has the wrong predecessor:",
           1, ((Integer) OpGanttValidator.getPredecessors(activity).get(0)).intValue());

      activity = (XComponent) validationDataSet.getChild(3);
      name = OpGanttValidator.getName(activity);
      assertEquals("The activity " + name + " has the wrong type:", OpGanttValidator.MILESTONE,
           OpGanttValidator.getType(activity));
      assertEquals("The activity " + name + " has the wrong number of children:",
           0, activity.getSubRows().size());
      assertEquals("The activity " + name + " has the wrong start date:",
           this.getGMTDate(22, 6, 2007), OpGanttValidator.getStart(activity));
      assertEquals("The activity " + name + " has the wrong end date:",
           this.getGMTDate(22, 6, 2007), OpGanttValidator.getEnd(activity));
      assertEquals("The activity " + name + " has the wrong duration:",
           0.0, OpGanttValidator.getDuration(activity));
      assertEquals("The activity " + name + " has the wrong effort:",
           0.0, OpGanttValidator.getBaseEffort(activity));
      assertEquals("The activity " + name + " has the wrong predecessor list:", 1,
           OpGanttValidator.getPredecessors(activity).size());
      assertEquals("The activity " + name + " has the wrong predecessor:",
           0, ((Integer) OpGanttValidator.getPredecessors(activity).get(0)).intValue());

   }

   /**
    * Returns a GMT date from the given values for day, month and year.
    *
    * @param day   a <code>byte</code> representing the day of a month.
    * @param month a <code>byte</code> representing the month of a year.
    * @param year  a <code>byte</code> representing the year.
    * @return a <code>Date</code> value.
    */
   private Date getGMTDate(int day, int month, int year) {
      Calendar gmtCalendar = Calendar.getInstance(OpProjectCalendar.GMT_TIMEZONE);
      gmtCalendar.set(Calendar.MILLISECOND, 0);
      gmtCalendar.set(Calendar.SECOND, 0);
      gmtCalendar.set(Calendar.MINUTE, 0);
      gmtCalendar.set(Calendar.HOUR_OF_DAY, 0);
      gmtCalendar.set(Calendar.DAY_OF_MONTH, day);
      gmtCalendar.set(Calendar.MONTH, month - 1);
      gmtCalendar.set(Calendar.YEAR, year);
      return new Date(gmtCalendar.getTime().getTime());
   }

   /**
    * @see junit.framework.TestCase#tearDown()
    */
   public void tearDown()
        throws Exception {
      super.tearDown();
   }
}
