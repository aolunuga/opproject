/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project.components;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.XValidationException;
import onepoint.express.XValidator;
import onepoint.express.XView;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.util.OpGraph;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpGraph.Entry;
import onepoint.project.validators.OpProjectValidator;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

public class OpGanttValidator extends OpProjectValidator {

   // TODO: Efforts of collection activities are to be handled differently
   // (Check for type COLLECTION in update finish, effort und duration)
   // *** Note: Also simplification possible for milestones
   // *** Note that we assume a certain number and order of columns

   // Component IDs of additional data holders
   public final static String ASSIGNMENT_SET = "AssignmentSet";
   public final static String PROJECT_START = "ProjectStartField";
   public final static String PROJECT_SETTINGS_DATA_SET = "ProjectSettingsDataSet";

   public final static String PROJECTCALENDAR_ID = "ProjectCalendar";
   public final static String RESOURCECALENDARS_ID = "ResourceCalendars";
   public final static String ABSENCES_ID = "Absences";

   private final static String PROJECT_FINISH = "ProjectFinishField";
   private final static String SHOW_RESOURCE_HOURS = "ShowResourceHours";
   private final static String RESOURCES_HOURLY_RATES_DATA_SET = "ResourcesHourlyRates";

   private static final XLog logger = XLogFactory.getLogger(OpGanttValidator.class);

   // Activity set column indexes (main data set)
   public final static int NAME_COLUMN_INDEX = 0;
   public final static int TYPE_COLUMN_INDEX = 1;
   public final static int CATEGORY_COLUMN_INDEX = 2;
   public final static int COMPLETE_COLUMN_INDEX = 3;
   public final static int START_COLUMN_INDEX = 4;
   public final static int FINISH_COLUMN_INDEX = 5;
   public final static int DURATION_COLUMN_INDEX = 6;
   public final static int BASE_EFFORT_COLUMN_INDEX = 7;
   public final static int PREDECESSORS_COLUMN_INDEX = 8;
   public final static int SUCCESSORS_COLUMN_INDEX = 9;
   public final static int RESOURCES_COLUMN_INDEX = 10;
   public final static int BASE_PERSONNEL_COSTS_COLUMN_INDEX = 11;
   public final static int BASE_TRAVEL_COSTS_COLUMN_INDEX = 12;
   public final static int BASE_MATERIAL_COSTS_COLUMN_INDEX = 13;
   public final static int BASE_EXTERNAL_COSTS_COLUMN_INDEX = 14;
   public final static int BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX = 15;
   public final static int DESCRIPTION_COLUMN_INDEX = 16;
   public final static int ATTACHMENTS_COLUMN_INDEX = 17;
   public final static int MODE_COLUMN_INDEX = 18;
   public final static int WORK_PHASES_COLUMN_INDEX = 19;
   public final static int REMAINING_EFFORT_COLUMN_INDEX = 20;
   public final static int WBS_CODE_COLUMN_INDEX = 21;
   public final static int RESOURCE_BASE_EFFORTS_COLUMN_INDEX = 22;
   public final static int PRIORITY_COLUMN_INDEX = 23;
   public final static int WORKRECORDS_COLUMN_INDEX = 24;
   public final static int ACTUAL_EFFORT_COLUMN_INDEX = 25;
   public final static int VISUAL_RESOURCES_COLUMN_INDEX = 26;
   public final static int RESPONSIBLE_RESOURCE_COLUMN_INDEX = 27;
   public final static int PAYMENT_COLUMN_INDEX = 29;
   public final static int BASE_PROCEEDS_COLUMN_INDEX = 30;
   public final static int BASE_BILLABLE_COLUMN_INDEX = 31;
   public final static int CUSTOM_ATTRIBUTES_COLUMN_INDEX = 32;
   public final static int OWNED_RESOURCES_COLUMN_INDEX = 33;
   public final static int LEAD_TIME_COLUMN_INDEX = 34;
   public final static int FOLLOW_UP_TIME_COLUMN_INDEX = 35;
   public final static int SUCCESSORS_VISUALIZATION_COLUMN_INDEX = 36;
   public final static int PREDECESSORS_VISUALIZATION_COLUMN_INDEX = 37;
   public final static int WORK_BREAKS_COLUMN_INDEX = 38;
   public final static int ACTIONS_COLUMN_INDEX = 40;
   public final static int MASTER_ACTIVITY_COLUMN_INDEX = 41;
   public final static int SUB_PROJECT_INDEX = 42;
   public final static int STATUS_COLUMN_INDEX = 43;
   public final static int ACTIONS_STATUS = 44;
   public final static int ERROR_COLUMN_INDEX = 45;
   
   private static final int NUMBER_OF_COLUMNS = 45;
   
   // error codes for use in columns...   
   public final static int NO_ERROR = 0;
   public final static int NO_WORKDAYS_IN_CALENDARS = 1;
   public final static int NO_WORKDAYS_IN_DURATION = 2;
   public final static int DURATION_EXCEEDED = 4;
   public final static int START_FIXED = 8;
   public final static int FINISH_FIXED = 16;
   
   // Actrion column index
   public static final int ACTION_STATUS = 2;

   // Assignment set column indexes
   private final static int AVAILABLE_COLUMN_INDEX = 0;

   //Hourly rates indexes
   public final static int INTERNAL_HOURLY_RATE_INDEX = 0;
   public final static int EXTERNAL_HOURLY_RATE_INDEX = 1;

   // Activity types
   public final static byte STANDARD = 0;
   public final static byte COLLECTION = 1;
   public final static byte MILESTONE = 2;
   public final static byte TASK = 3;
   public final static byte COLLECTION_TASK = 4;
   public final static byte SCHEDULED_TASK = 5;
   public final static byte ADHOC_TASK = 6;

   // Action types (bits/flags)
   public static final byte NO_ACTIONS = 0;
   public static final byte NOT_STARTED = 1;
   public static final byte STARTED = 2;
   public static final byte DONE = 4;

   // Dependency types and such stuff...
   public static final int DEP_END_START = 0;
   public static final int DEP_START_END = 1;
   public static final int DEP_START_START = 2;
   public static final int DEP_END_END = 3;
   public static final int DEP_COLLECTION_START = 4;
   public static final int DEP_COLLECTION_END = 5;
   
   public final static String DEP_TYPE = "type";
   public final static String DEP_OK = "ok";
   public final static String DEP_CRITICAL = "critical";
   
   public static final int DEP_DEFAULT = DEP_END_START;
   
   public static final Integer EDGE_CLASS_HIERACHY = new Integer(99);
   public static final Integer EDGE_CLASS_DEP_END_START = new Integer(DEP_END_START);
   public static final Integer EDGE_CLASS_DEP_START_END = new Integer(DEP_START_END);
   public static final Integer EDGE_CLASS_DEP_START_START = new Integer(DEP_START_START);
   public static final Integer EDGE_CLASS_DEP_END_END = new Integer(DEP_END_END);
   
   public final static String PREDECESSOR_TABLE_DELIMITER = ";";

   private final static Pattern PREDECESSOR_PATTERN = Pattern.compile("\\s*([0-9]+)(.{0,1})\\s*");
   private static final int PREDECESSOR_PATTERN_INDEX_GROUP = 1;
   private final static String[] DEP_TYPE_MAP = {">"};

   //Default Priority Value
   public final static byte DEFAULT_PRIORITY = 5;

   // Calculation modes
   public static final String CALCULATION_MODE = "CalculationMode";
   public static final byte EFFORT_BASED = 1;
   public static final byte INDEPENDENT = 2;

   //progress tracking
   public final static String PROGRESS_TRACKED = "ProgressTracked";

   //project type
   public final static String PROJECT_TEMPLATE = "Template";

   // Activity attributes (bits/flags)
   public final static int MANDATORY = 1;
   public final static int LINKED = 2;
   public final static int HAS_ATTACHMENTS = 4;
   public final static int HAS_COMMENTS = 8;
   public final static int START_IS_FIXED = 16;
   public final static int FINISH_IS_FIXED = 32;
   public final static int EXPORTED_TO_SUPERPROJECT = 64;
   public final static int IMPORTED_FROM_SUBPROJECT = 128;
   public final static int VALIDATION_ERROR = 256;
   public final static int INCOMPLETE_ASSIGNMENT = 512;

   public final static double ACTIVITY_MAX_DURATION = 20800d; // hours?!?
   public final static double ACTIVITY_MAX_EFFORT = 2080000d; // hours?!?

   //Activity status values
   public final static int NOT_OVERDUE_ACTIVITY = 1;
   public final static int OVERDUE_ACTIVITY = 2;

   //The id of the no category
   public static final String NO_CATEGORY_ID = "-1";

   // Date sentinel (indicates the date was "loaded", but value was null)
   private final static Date DATE_SENTINEL = new Date(0);

   private OpProjectCalendar projectCalendar = null;
   private Map resourceCalendarMap = null;
   
   // New two fields should never be accessed directly
   private Map resourceAbsences = null; // Map<String, Set<Date>> ... maps resource-locators to sets of dates...
   private XComponent hourlyRatesDataSet = null;
   private Date projectStart;
   private Date projectWorkingStart;
   private Date projectFinish;
   private Date projectWorkingFinish;
   private Date projectPlanFinish;
   private Boolean hourBasedResources;
   private Byte calculationMode;
   private Boolean progressTracked;
   private XComponent assignmentSet;
   private Boolean projectTemplate;
   private Double projectCost;
   private Double projectProceeds;
   private Double projectEffort;

   public final static String LOOP_EXCEPTION = "LoopException";
   public final static String ASSIGNMENT_EXCEPTION = "AssignmentException";
   public final static String RESOURCE_NAME_EXCEPTION = "ResourceNameException";
   public final static String EFFORTS_NOT_EQUAL_EXCEPTION = "NotEqualEffortsException";
   public final static String RANGE_EXCEPTION = "RangeException";
   public final static String MANDATORY_EXCEPTION = "MandatoryException";
   public final static String INVALID_COST_EXCEPTION = "InvalidCostException";
   public final static String MILESTONE_COLLECTION_EXCEPTION = "MilestoneCollectionException";
   public final static String SCHEDULED_MIXED_EXCEPTION = "ScheduledMixedException";
   public final static String WORKRECORDS_EXIST_EXCEPTION = "WorkRecordsExistException";
   public final static String TASK_EXTRA_RESOURCE_EXCEPTION = "TaskExtraResourceException";
   public final static String INVALID_PRIORITY_EXCEPTION = "InvalidPriorityException";
   public final static String BASE_EFFORT_BELOW_ACTUAL_EXCEPTION = "BaseEffortBelowActualException";
   public final static String CANNOT_MOVE_ROOT_ACTIVITY_EXCEPTION = "CannotMoveRootActivityException";
   public final static String OUTLINE_LEVEL_INVALID_EXCEPTION = "OutlineLevelInvalidException";
   public final static String INVALID_PAYMENT_EXCEPTION = "InvalidPaymentException";
   public final static String DEPENDENCY_FORMAT_EXCEPTION = "DependencyFormatException";
   public final static String PROGRAM_ELEMENT_MOVE_EXCEPTION = "ProgramElementMoveException";
   public final static String PROGRAM_ELEMENT_DELETE_EXCEPTION = "ProgramElementDeleteException";
   public final static String FIXED_START_EXCEPTION = "FixedStartException";
   public final static String FIXED_FINISH_EXCEPTION = "FixedFinishException";
   public final static String WORK_DAY_BEFORE_START_EXCEPTION = "WorkDayBeforeStartException";
   public final static String ITERATION_EXCEPTION = "IterationException";
   
   public final static double INVALID_ASSIGNMENT = -1;

   public final static String NO_RESOURCE_ID = "-1";
   public final static double NO_RESOURCE_AVAILABILITY = Double.MAX_VALUE;
   public final static String NO_RESOURCE_NAME = "?";

   private final static char HOURS_SYMBOL = 'h';
   private final static char PERCENT_SYMBOL = '%';
   
   public static final String WORK_BREAK_LOCATOR = "locator";
   public static final String WORK_BREAK_START = "start";
   public static final String WORK_BREAK_DURATION = "duration";
   
   public static final String WORK_PHASE_START_KEY = "start";
   public static final String WORK_PHASE_FINISH_KEY = "finish";
   public static final String WORK_PHASE_EFFORT_KEY = "effort";

   public final static String[][] WORK_BREAK_DATASET_ROW_DESCRIPTION = {
      {"locator", "-1"},
      {"start", "0", null, "out"},
      {"duration", "1", null, "out"},
      };
   
   public static final byte[] COLLECTION_TYPES = {COLLECTION, COLLECTION_TASK, SCHEDULED_TASK};
   public static final byte[] EFFORT_TYPES = {TASK, STANDARD};
   public static final byte[] NO_EFFORT_WITH_ASSIGNMENT_TYPES = {MILESTONE};
   public static final byte[] HAS_START_FINISH_TYPES = {STANDARD, MILESTONE, SCHEDULED_TASK, COLLECTION};
   public static final byte[] START_FINISH_MUST_BE_WORKDAY_TYPES = {STANDARD, MILESTONE, SCHEDULED_TASK};
   public static final byte[] DEFINES_START_FINISH_TYPES = {STANDARD, MILESTONE, SCHEDULED_TASK};
   public static final byte[] APPLY_DURATION_TYPES = {MILESTONE, STANDARD, SCHEDULED_TASK};
   public static final byte[] TASK_TYPES = {TASK};
   public static final byte[] HAS_RESPONSIBLE_RESOURCE = {TASK, STANDARD};
   public static final byte[] MILESTONE_TYPE = {MILESTONE};
   public static final byte[] ASSIGNMENT_HAS_PERCENTAGE = {STANDARD};


   /**
    * An error margin used in the calculations of the validator.
    */
   public final static double ERROR_MARGIN = 0.05d;

   private boolean cleanClipboard;
   private List undo;
   private boolean continuousAction;
   private List redo;
   protected boolean disableValidation = false;
   private Set validationAnchors;
   public static final int MAX_UNDO = 10;

   // used as start points for date-iterations:
   // move to OpGanttValidator eventually...
   public static Date END_OF_TIME = null; 
   public static Date BEGINNING_OF_TIME = null;
   private static Set emptySet = new HashSet();
   static {
      SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
      try {
         END_OF_TIME = new Date(DATE_FORMAT.parse("3000-12-31").getTime());
         BEGINNING_OF_TIME = new Date(DATE_FORMAT.parse("1900-01-01").getTime());
      } catch (ParseException e) {
         e.printStackTrace();
      }
   }
   
   public OpGanttValidator() {
   }

   public OpGanttValidator(OpProjectCalendar projectCalendar, Map resourceCalendarMap) {
      this();
      this.projectCalendar = projectCalendar;
      this.resourceCalendarMap = resourceCalendarMap;
   }

   public OpProjectCalendar getProjectCalendar() {
      return projectCalendar;
   }
   
   public Map getResourceCalendarMap() {
      return resourceCalendarMap;
   }
   
   private static XComponent getChild(XComponent dataRow, int column) {
      if (column < dataRow.getChildCount()) {
         return ((XComponent) (dataRow.getChild(column)));
      }
      return null;
   }

   private static XComponent ensureChildCreated(XComponent dataRow, int column) {
      int childCount = dataRow.getChildCount();
      for (; column >= childCount; childCount++) {
         dataRow.addChild(new XComponent(XComponent.DATA_CELL));
      }
      return ((XComponent)dataRow.getChild(column));
   }

   // Helpers for easier accessing data-set values
   private static void setSomething(XComponent dataRow, int column, Object value) {
      ensureChildCreated(dataRow, column).setValue(value);
   }
   
   private static Object getSomething(XComponent dataRow, int column) {
      XComponent cell = getChild(dataRow, column);
      if (cell != null) {
         return cell.getValue();
      }
      return null;
   }
   
   private static boolean columnHoldsValue(XComponent dataRow, Class c,
         int column) {
      if (column < dataRow.getChildCount()) {
         return ((XComponent) (dataRow.getChild(column))).hasValue(c);
      }
      return false;
   }
   
   public static void setName(XComponent dataRow, String name) {
      setSomething(dataRow, NAME_COLUMN_INDEX, name);
   }

   public static String getName(XComponent dataRow) {
      return columnHoldsValue(dataRow, String.class, NAME_COLUMN_INDEX) ? (String) getSomething(dataRow, NAME_COLUMN_INDEX) : null;
   }

   public static void setDescription(XComponent dataRow, String desc) {
      setSomething(dataRow, DESCRIPTION_COLUMN_INDEX, desc);
   }

   public static String getDescription(XComponent dataRow) {
      return columnHoldsValue(dataRow, String.class, DESCRIPTION_COLUMN_INDEX) ? (String) getSomething(dataRow, DESCRIPTION_COLUMN_INDEX) : null;
   }

   public static void setType(XComponent dataRow, byte type) {
      setSomething(dataRow, TYPE_COLUMN_INDEX, new Byte(type));
   }

   public static byte getType(XComponent dataRow) {
      return columnHoldsValue(dataRow, Byte.class, TYPE_COLUMN_INDEX) ? ((Byte) getSomething(
            dataRow, TYPE_COLUMN_INDEX)).byteValue()
            : STANDARD;
   }

   public static void setCategory(XComponent dataRow, String category) {
      setSomething(dataRow, CATEGORY_COLUMN_INDEX, category);
   }

   public static String getCategory(XComponent dataRow) {
      return columnHoldsValue(dataRow, String.class, CATEGORY_COLUMN_INDEX) ? (String) getSomething(
            dataRow, CATEGORY_COLUMN_INDEX)
            : null;
   }


   public static void setComplete(XComponent dataRow, double complete) {
      setSomething(dataRow, COMPLETE_COLUMN_INDEX, new Double(complete));
   }

   public static double getComplete(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, COMPLETE_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, COMPLETE_COLUMN_INDEX)).doubleValue()
            : 0d;
   }


   public static void setPriority(XComponent dataRow, Byte priority) {
      setSomething(dataRow, PRIORITY_COLUMN_INDEX, priority);
   }

   public static Byte getPriority(XComponent dataRow) {
      return columnHoldsValue(dataRow, Byte.class, PRIORITY_COLUMN_INDEX) ? (Byte) getSomething(
            dataRow, PRIORITY_COLUMN_INDEX)
            : null;
   }


   public static void setWorkRecords(XComponent dataRow, Map workRecords) {
      setSomething(dataRow, WORKRECORDS_COLUMN_INDEX, workRecords);
   }

   public static Map getWorkRecords(XComponent dataRow) {
      return columnHoldsValue(dataRow, Map.class, WORKRECORDS_COLUMN_INDEX) ? (Map) getSomething(
            dataRow, WORKRECORDS_COLUMN_INDEX)
            : null;
   }

   public static void setResponsibleResource(XComponent dataRow, String newValue) {
      setSomething(dataRow, RESPONSIBLE_RESOURCE_COLUMN_INDEX, newValue);
   }

   public static String getResponsibleResource(XComponent dataRow) {
      return columnHoldsValue(dataRow, String.class, RESPONSIBLE_RESOURCE_COLUMN_INDEX) ? (String) getSomething(
            dataRow, RESPONSIBLE_RESOURCE_COLUMN_INDEX)
            : null;
   }

   public static void setWBSCode(XComponent dataRow, String newValue) {
      setSomething(dataRow, WBS_CODE_COLUMN_INDEX, newValue);
   }

   public static String getWBSCode(XComponent dataRow) {
      return columnHoldsValue(dataRow, String.class, WBS_CODE_COLUMN_INDEX) ? (String) getSomething(
            dataRow, WBS_CODE_COLUMN_INDEX)
            : null;
   }

   public static void setPayment(XComponent dataRow, double payment) {
      setSomething(dataRow, PAYMENT_COLUMN_INDEX, new Double(payment));
   }

   public static double getPayment(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, PAYMENT_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, PAYMENT_COLUMN_INDEX)).doubleValue()
            : 0d;
   }

   public static void setBaseProceeds(XComponent dataRow, double proceeds) {
      setSomething(dataRow, BASE_PROCEEDS_COLUMN_INDEX, new Double(proceeds));
   }

   public static double getBaseProceeds(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, BASE_PROCEEDS_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, BASE_PROCEEDS_COLUMN_INDEX)).doubleValue()
            : 0d;
   }


   public static void setEffortBillable(XComponent dataRow, double billable) {
      setSomething(dataRow, BASE_BILLABLE_COLUMN_INDEX, new Double(billable));
   }

   public static double getEffortBillable(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, BASE_BILLABLE_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, BASE_BILLABLE_COLUMN_INDEX)).doubleValue()
            : 100d;
   }

   public static void clearEffortBillable(XComponent dataRow) {
      setSomething(dataRow, BASE_BILLABLE_COLUMN_INDEX, null);
   }


   public static void setStart(XComponent dataRow, Date start) {
      setSomething(dataRow, START_COLUMN_INDEX, start);
   }

   public static Date getStart(XComponent dataRow) {
      return columnHoldsValue(dataRow, Date.class, START_COLUMN_INDEX) ? (Date) getSomething(
            dataRow, START_COLUMN_INDEX)
            : null;
   }


   public static void setEnd(XComponent dataRow, Date finish) {
      setSomething(dataRow, FINISH_COLUMN_INDEX, finish);
   }

   public static Date getEnd(XComponent dataRow) {
      return columnHoldsValue(dataRow, Date.class, FINISH_COLUMN_INDEX) ? (Date) getSomething(
            dataRow, FINISH_COLUMN_INDEX)
            : null;
   }


   public static void setLeadTime(XComponent dataRow, double leadTime) {
      setSomething(dataRow, LEAD_TIME_COLUMN_INDEX, new Double(leadTime));
   }

   public static double getLeadTime(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, LEAD_TIME_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, LEAD_TIME_COLUMN_INDEX)).doubleValue()
            : 0d;
   }

   public static void setFollowUpTime(XComponent dataRow, double followUpTime) {
      setSomething(dataRow, FOLLOW_UP_TIME_COLUMN_INDEX, new Double(followUpTime));
   }

   public static double getFollowUpTime(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, FOLLOW_UP_TIME_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, FOLLOW_UP_TIME_COLUMN_INDEX)).doubleValue()
            : 0d;
   }

   public static void setDuration(XComponent dataRow, double duration) {
      setSomething(dataRow, DURATION_COLUMN_INDEX, new Double(duration));
   }

   public static double getDuration(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, DURATION_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, DURATION_COLUMN_INDEX)).doubleValue()
            : 0d;
   }

   public double getDurationDays(XComponent dataRow) {
      return getDurationDays(getDuration(dataRow), getCalendar());
   }

   public static double getDurationDays(double durationHours, OpProjectCalendar calendar) {
      // TODO: CHECK!
      return Math.ceil(durationHours / calendar.getWorkHoursPerDay());
   }
   
   public static void setBaseEffort(XComponent dataRow, double baseEffort) {
      setSomething(dataRow, BASE_EFFORT_COLUMN_INDEX, new Double(baseEffort));
   }

   public static double getBaseEffort(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, BASE_EFFORT_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, BASE_EFFORT_COLUMN_INDEX)).doubleValue()
            : 0d;
   }


   public static void setActualEffort(XComponent dataRow, double actualEffort) {
      setSomething(dataRow, ACTUAL_EFFORT_COLUMN_INDEX, new Double(actualEffort));
   }

   public static double getActualEffort(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, ACTUAL_EFFORT_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, ACTUAL_EFFORT_COLUMN_INDEX)).doubleValue()
            : 0d;
   }

   public static void setRemainigEffort(XComponent dataRow, double actualEffort) {
      setSomething(dataRow, REMAINING_EFFORT_COLUMN_INDEX, new Double(actualEffort));
   }

   public static double getRemainingEffort(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, REMAINING_EFFORT_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, REMAINING_EFFORT_COLUMN_INDEX)).doubleValue()
            : 0d;
   }


   public static void setPredecessors(XComponent dataRow, SortedMap predecessors) {
      setSomething(dataRow, PREDECESSORS_COLUMN_INDEX, predecessors);
      // build the visualization:
      String s = createPredecessorVisualization(predecessors);
      setPredecessorVisualization(dataRow, s);
   }

   public static SortedMap getPredecessors(XComponent dataRow) {
      if (!columnHoldsValue(dataRow, SortedMap.class, PREDECESSORS_COLUMN_INDEX)) {
         setPredecessors(dataRow, new TreeMap());
      }
      return (SortedMap) getSomething(dataRow, PREDECESSORS_COLUMN_INDEX);
   }

   public static void setPredecessorVisualization(XComponent dataRow, String visualization) {
      setSomething(dataRow, PREDECESSORS_VISUALIZATION_COLUMN_INDEX, visualization);
   }

   
   public static void setSuccessors(XComponent dataRow, SortedMap successors) {
      setSomething(dataRow, SUCCESSORS_COLUMN_INDEX, successors);
   }

   public static SortedMap getSuccessors(XComponent dataRow) {
      if (!columnHoldsValue(dataRow, SortedMap.class, SUCCESSORS_COLUMN_INDEX)) {
         setSuccessors(dataRow, new TreeMap());
      }
      return (SortedMap) getSomething(dataRow, SUCCESSORS_COLUMN_INDEX);
   }


   public static void setOwnedResources(XComponent dataRow, List resources) {
      setSomething(dataRow, OWNED_RESOURCES_COLUMN_INDEX, resources);
   }

   public static List getOwnedResources(XComponent dataRow) {
      return columnHoldsValue(dataRow, List.class, OWNED_RESOURCES_COLUMN_INDEX) ? (List) getSomething(
            dataRow, OWNED_RESOURCES_COLUMN_INDEX)
            : null;
   }


   public static void setResources(XComponent dataRow, List resources) {
      setSomething(dataRow, RESOURCES_COLUMN_INDEX, resources);
   }

   public static List getResources(XComponent dataRow) {
      return columnHoldsValue(dataRow, List.class, RESOURCES_COLUMN_INDEX) ? (List) getSomething(
            dataRow, RESOURCES_COLUMN_INDEX)
            : null;
   }


   public static void setVisualResources(XComponent dataRow, List formattedResources) {
      setSomething(dataRow, VISUAL_RESOURCES_COLUMN_INDEX, formattedResources);
   }

   public static List getVisualResources(XComponent dataRow) {
      return columnHoldsValue(dataRow, List.class, VISUAL_RESOURCES_COLUMN_INDEX) ? (List) getSomething(
            dataRow, VISUAL_RESOURCES_COLUMN_INDEX)
            : null;
   }


   public static void setAttachments(XComponent dataRow, List attachments) {
      setSomething(dataRow, ATTACHMENTS_COLUMN_INDEX, attachments);
   }

   public static List getAttachments(XComponent dataRow) {
      return columnHoldsValue(dataRow, List.class, ATTACHMENTS_COLUMN_INDEX) ? (List) getSomething(
            dataRow, ATTACHMENTS_COLUMN_INDEX)
            : null;
   }

   public static void setWorkBreaks(XComponent dataRow, SortedMap workBreaks) {
      setSomething(dataRow, WORK_BREAKS_COLUMN_INDEX, workBreaks);
   }

   public static SortedMap getWorkBreaks(XComponent dataRow) {
      if (!columnHoldsValue(dataRow, SortedMap.class, WORK_BREAKS_COLUMN_INDEX)) {
         setWorkBreaks(dataRow, new TreeMap());
      }
      return (SortedMap) getSomething(dataRow, WORK_BREAKS_COLUMN_INDEX);
   }

   public static void setError(XComponent dataRow, int code) {
      setSomething(dataRow, ERROR_COLUMN_INDEX, new Integer(code));
      setAttribute(dataRow, VALIDATION_ERROR, code != NO_ERROR);
   }

   public static int getError(XComponent dataRow) {
      return columnHoldsValue(dataRow, Integer.class, ERROR_COLUMN_INDEX) ? ((Integer) getSomething(
            dataRow, ERROR_COLUMN_INDEX)).intValue()
            : NO_ERROR;
   }

   public static void addSuccessor(XComponent dataRow, int index, final int type) {
      addSuccessor(dataRow, index, type, false);
   }

   public static void addSuccessor(XComponent dataRow, int index, final int type, boolean critical) {
      SortedMap successors = getSuccessors(dataRow);
      successors = addDependency(index, type, critical, successors);
      setSuccessors(dataRow, successors);
   }

   public static SortedMap addDependency(int index, final int type, boolean critical,
         SortedMap links) {
      if (links == null) {
         links = new TreeMap();
      }
      HashMap deps = new HashMap();
      deps.put(DEP_TYPE, new Integer(type));
      if (critical) {
         deps.put(DEP_CRITICAL, new Boolean(critical));
      }
      links.put(new Integer(index), deps);
      return links;
   }

   public static void removeSuccessor(XComponent dataRow, int index) {
      SortedMap successors = getSuccessors(dataRow);
      if (successors != null) {
         successors.remove(new Integer(index));
      }
   }

   public void removeLink(int sourceIndex, int targetIndex) {
      XComponent dataSet = getDataSet();
      XComponent source = (XComponent) dataSet.getChild(sourceIndex);
      XComponent target = (XComponent) dataSet.getChild(targetIndex);
      addValidationStartPoint(target);
      removeSuccessor(source, targetIndex);
      removePredecessor(target, sourceIndex);
   }

   /**
    * Add a predecessor for an activity data row
    *
    * @param dataRow          the data row
    * @param predecessor_index the predecesor index
    */
   public static void addPredecessor(XComponent dataRow, int index, final int type) {
      addPredecessor(dataRow, index, type, false);
   }
   
   public static void addPredecessor(XComponent dataRow, int index, final int type, boolean critical) {
      SortedMap predecessors = getPredecessors(dataRow);
      predecessors = addDependency(index, type, critical, predecessors);
      setPredecessors(dataRow, predecessors);
   }

   /**
    * Remove a predecessor for an activity data row
    *
    * @param dataRow          the data row
    * @param predecessor_index the succesor index
    */
   public static void removePredecessor(XComponent dataRow, int index) {
      SortedMap predecessors = getPredecessors(dataRow);
      if (predecessors != null) {
         predecessors.remove(new Integer(index));
         setPredecessorVisualization(dataRow, createPredecessorVisualization(getPredecessors(dataRow)));
      }
   }

   public static class ResourceChoiceComparator extends XComponent.ChoiceComparator {
      public int compare(Object o1, Object o2) {
         String n1 = OpGanttValidator.getResourceName(XValidator.choiceCaption((String)o1));
         String n2 = OpGanttValidator.getResourceName(XValidator.choiceCaption((String)o2));
         if (OpGanttValidator.NO_RESOURCE_NAME.equals(n1)) {
            return OpGanttValidator.NO_RESOURCE_NAME.equals(n2) ? 0 : 1;
         }
         return super.compare(o1, o2);
      }      
   }
   
   /**
    * Add a resource for an activity data row
    *
    * @param dataRow the data row
    * @param resource the resource that should be added
    * @see OpGanttValidator#setResources(onepoint.express.XComponent,List)
    */
   public static void addResource(XComponent dataRow, String resource) {
      SortedSet tmpRes = new TreeSet(new ResourceChoiceComparator());

      tmpRes.addAll(getResources(dataRow));
      tmpRes.add(resource);
      
      setResources(dataRow, new ArrayList(tmpRes));
   }

   public static void setBasePersonnelCosts(XComponent dataRow, double base_personnel_costs) {
      setSomething(dataRow, BASE_PERSONNEL_COSTS_COLUMN_INDEX, new Double(base_personnel_costs));
   }

   public static double getBasePersonnelCosts(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, BASE_PERSONNEL_COSTS_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, BASE_PERSONNEL_COSTS_COLUMN_INDEX)).doubleValue()
            : 0d;
   }


   public static void setBaseTravelCosts(XComponent dataRow, double base_travel_costs) {
      setSomething(dataRow, BASE_TRAVEL_COSTS_COLUMN_INDEX, new Double(base_travel_costs));
   }

   public static double getBaseTravelCosts(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, BASE_TRAVEL_COSTS_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, BASE_TRAVEL_COSTS_COLUMN_INDEX)).doubleValue()
            : 0d;
   }


   public static void setBaseMaterialCosts(XComponent dataRow, double base_material_costs) {
      setSomething(dataRow, BASE_MATERIAL_COSTS_COLUMN_INDEX, new Double(base_material_costs));
   }

   public static double getBaseMaterialCosts(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, BASE_MATERIAL_COSTS_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, BASE_MATERIAL_COSTS_COLUMN_INDEX)).doubleValue()
            : 0d;
   }


   public static void setBaseExternalCosts(XComponent dataRow, double base_external_costs) {
      setSomething(dataRow, BASE_EXTERNAL_COSTS_COLUMN_INDEX, new Double(base_external_costs));
   }

   public static double getBaseExternalCosts(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, BASE_EXTERNAL_COSTS_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, BASE_EXTERNAL_COSTS_COLUMN_INDEX)).doubleValue()
            : 0d;
   }


   public static void setBaseMiscellaneousCosts(XComponent dataRow, double base_miscellaneous_costs) {
      setSomething(dataRow, BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX, new Double(base_miscellaneous_costs));
   }

   public static double getBaseMiscellaneousCosts(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX)).doubleValue()
            : 0d;
   }


   public static void setAttributes(XComponent dataRow, int attributes) {
      setSomething(dataRow, MODE_COLUMN_INDEX, new Integer(attributes));
   }

   public static int getAttributes(XComponent dataRow) {
      return columnHoldsValue(dataRow, Integer.class, MODE_COLUMN_INDEX) ? ((Integer) getSomething(
            dataRow, MODE_COLUMN_INDEX)).intValue()
            : 0;
   }

   public static void setAttribute(XComponent dataRow, int attribute, boolean value) {
      int attributes = getAttributes(dataRow);
      if (value) {
         attributes |= attribute;
      }
      else {
         attributes -= (attributes & attribute);
      }
      setAttributes(dataRow, attributes);
   }

   public static boolean getAttribute(XComponent dataRow, int attribute) {
      return (getAttributes(dataRow) & attribute) == attribute;
   }

   public static void setWorkPhases(XComponent dataRow, SortedMap workPhases) {
      setSomething(dataRow, WORK_PHASES_COLUMN_INDEX, workPhases);
   }

   public static SortedMap getWorkPhases(XComponent dataRow) {
      return columnHoldsValue(dataRow, SortedMap.class, WORK_PHASES_COLUMN_INDEX) ? (SortedMap) getSomething(
            dataRow, WORK_PHASES_COLUMN_INDEX)
            : null;
   }

   public static void resetWorkPhases(XComponent dataRow) {
      setWorkPhases(dataRow, new TreeMap());
   }
   
   public static void addWorkPhase(XComponent dataRow, Map workPhase) {
      if (getWorkPhases(dataRow) == null) {
         setWorkPhases(dataRow, new TreeMap());
      }
      getWorkPhases(dataRow).put(workPhase.get(WORK_PHASE_START_KEY), workPhase);
   }

   public static void removeWorkPhase(XComponent dataRow, Map workPhase) {
      if (getWorkPhases(dataRow) == null) {
         return;
      }
      getWorkPhases(dataRow).remove(workPhase.get(WORK_PHASE_START_KEY));
   }

   /**
    * Set the resources base efforts for the given activity. Must be in the same order as the resources assigned to this
    * activity
    *
    * @param dataRow         activity to add the efforts to
    * @param resourcesEfforts XArray with values of the added efforts
    */
   public static void setResourceBaseEfforts(XComponent dataRow, Map resourcesEfforts) {
      setSomething(dataRow, RESOURCE_BASE_EFFORTS_COLUMN_INDEX, resourcesEfforts);
   }

   public static Map getResourceBaseEfforts(XComponent dataRow) {
      return columnHoldsValue(dataRow, Map.class, RESOURCE_BASE_EFFORTS_COLUMN_INDEX) ? (Map) getSomething(
            dataRow, RESOURCE_BASE_EFFORTS_COLUMN_INDEX)
            : null;
   }

   /**
    * Adds a new resource based effort for a given dataRow (activity). Must be in the same order as the resources
    * assigned to this activity
    *
    * @param dataRow        activity to add the effort to
    * @param ressourceEffort value of the added effort
    */
   public static void setResourceBaseEffort(XComponent dataRow, String resource, double ressourceEffort) {
      if (importedActivity(dataRow)) {
         return;
      }
      if (getResourceBaseEfforts(dataRow) == null) {
         setResourceBaseEfforts(dataRow, new HashMap());
      }
      getResourceBaseEfforts(dataRow).put(resource, new Double(ressourceEffort));
   }

   // Accessors for assignment set
   // TODO: Maybe change naming, e.g. activityName() and assignmentAvailable()

   public static double getAvailable(XComponent dataRow) {
      return columnHoldsValue(dataRow, Double.class, AVAILABLE_COLUMN_INDEX) ? ((Double) getSomething(
            dataRow, AVAILABLE_COLUMN_INDEX)).doubleValue()
            : 0d;
   }

   /**
    * @param dataRow
    * @return
    */
   public static Map getCustomAttributes(XComponent dataRow) {
      Map value = (Map) ((XComponent) dataRow.getChild(CUSTOM_ATTRIBUTES_COLUMN_INDEX)).getValue();
      if (value == null) {
         updateCustomAttributes(dataRow);
      }
      value = (Map) ((XComponent) dataRow.getChild(CUSTOM_ATTRIBUTES_COLUMN_INDEX)).getValue();
      return value;
   }
   
   public static void updateCustomAttributes(XComponent dataRow) {
      if (importedActivity(dataRow)) {
         return;
      }
      XComponent parent = (XComponent) dataRow.getChild(CUSTOM_ATTRIBUTES_COLUMN_INDEX);
      if (parent.getValue() != null) {
         return;
      }
      String objectId = dataRow.getStringValue();
      if (objectId == null) {
         return;
      }

      XMessage request = new XMessage();
      request.setAction("CustomAttributeService.getCustomValues");
      HashMap parameters = new HashMap();
      parameters.put("Prototype", "onepoint.project.modules.project.OpActivity");
      parameters.put("Subtype", null);
      parameters.put("Name", null);
      parameters.put("ObjectLocator", objectId);
      request.setArgument("parameters", parameters);

      XMessage response = XDisplay.getClient().invokeMethod(request);

      Map attributeList = (Map) response.getArgument("custom_values");
      parent.setValue(attributeList);
   }    

   public static void updateActions(XComponent dataRow, boolean editable, boolean statusChangeable) {
      if (importedActivity(dataRow)) {
         return;
      }
      XComponent parent = ensureChildCreated(dataRow, ACTIONS_COLUMN_INDEX);
      
      if (parent.getValue() != null) {
         return;
      }
      String objectId = dataRow.getStringValue();

      XMessage request = new XMessage();
      request.setAction("PlanningService.getActions");
      HashMap parameters = new HashMap();
      parameters.put("ObjectLocator", objectId);
      XComponent projectIdField = dataRow.getForm().findComponent("ProjectIDField");
      parameters.put("Project", projectIdField == null ? null : projectIdField.getStringValue());
      parameters.put("Editable", new Boolean(editable));
      parameters.put("StatusChangeable", new Boolean(statusChangeable));
      request.setArgument("parameters", parameters);

      XMessage response = XDisplay.getClient().invokeMethod(request);

      List attributeList = (List) response.getArgument("actions");
      parent.setValue(attributeList);
   }    

   
   /**
    * @param customAttributes 
    * @param dataRow
    * @return
    */
   public static void setCustomAttributes(XComponent dataRow, Map customAttributes) {
      setSomething(dataRow, CUSTOM_ATTRIBUTES_COLUMN_INDEX, customAttributes);
   }

   /**
    * Sets up the actions of an activity data row
    *
    * @param dataRow     the data row
    * @param predecessors an <code>XArray <Integer> </code> of predecessors
    */
   public static void setActions(XComponent dataRow, List actions) {
      setSomething(dataRow, ACTIONS_COLUMN_INDEX, actions);
      updateActionsStatus(dataRow);
   }

   /**
    * 
    * @param dataRow 
    * @pre
    * @post
    */
   public static void updateActionsStatus(XView dataRow) {
      int status = getActionsStatus((XComponent) dataRow, true, true);
      setActionsStatus((XComponent) dataRow, status);
   }

   /**
    * @param dataRow
    * @param status
    * @pre
    * @post
    */
   private static void setActionsStatus(XComponent dataRow, int status) {
      setSomething(dataRow, ACTIONS_STATUS, new Integer(status));
   }
   
   /**
    * @param activity
    * @pre
    * @post
    */
   private static int getActionStatus(XComponent action) {
      return columnHoldsValue(action,String.class, ACTION_STATUS) ? XValidator.choiceIconIndex(((String) getSomething(
            action, ACTION_STATUS)))
            : 0;
   }
   
   public static boolean isEnabledForActions(XComponent dataRow) {
      int type = getType(dataRow);
      return type == STANDARD ||
                  type == MILESTONE || 
                  type == TASK;
   }

   /**
    * @param activity
    * @pre
    * @post
    */
   private static int getActionsStatus(XComponent dataRow, boolean editable, boolean statusChangeable) {
      // no activity started : NOT_STARTED
      // at least one started or done : STARTED
      // all done : DONE
      List/*<XComponent>*/ actions = getActions(dataRow, editable, statusChangeable);
      int maxActionStatus = OpGanttValidator.NO_ACTIONS;
      int minActionStatus = OpGanttValidator.NO_ACTIONS;
      if (actions != null && actions.size() > 0) {
         minActionStatus = OpGanttValidator.DONE;
         Iterator iter = actions.iterator();
         while (iter.hasNext()) {
            XComponent action = (XComponent)iter.next();
            
            switch (getActionStatus(action)) {
               case 0: //OpActionIfc.NOT_STARTED:
                  if (maxActionStatus < OpGanttValidator.NOT_STARTED) {
                     maxActionStatus = OpGanttValidator.NOT_STARTED;
                  }
                  minActionStatus = OpGanttValidator.NOT_STARTED;
                  break;
               case 1: //OpActionIfc.STARTED:
                  if (maxActionStatus < OpGanttValidator.STARTED) {
                     maxActionStatus = OpGanttValidator.STARTED;
                  }
                  if (minActionStatus > OpGanttValidator.STARTED) {
                     minActionStatus = OpGanttValidator.STARTED;
                  }
                  break;
               case 2: //OpActionIfc.DONE:
                  maxActionStatus = OpGanttValidator.DONE;
                  if (minActionStatus > OpGanttValidator.DONE) {
                     minActionStatus = OpGanttValidator.DONE;
                  }
                  break;
            }
            if (maxActionStatus == OpGanttValidator.DONE && minActionStatus == OpGanttValidator.NOT_STARTED) {
               break;
            }
         }
      }
      if (maxActionStatus == OpGanttValidator.NO_ACTIONS) {
         return OpGanttValidator.NO_ACTIONS;
      }
      if (maxActionStatus == OpGanttValidator.NOT_STARTED) {
         return OpGanttValidator.NOT_STARTED;
      }
      if (minActionStatus == OpGanttValidator.DONE) {
         return OpGanttValidator.DONE;
      }
      return OpGanttValidator.STARTED;
   }


   /**
    * Returns the actions of an activity data row
    *
    * @param dataRow the data row
    * @return an <code>XArray <Integer> </code> of predecessors
    */
   public static List/*<XComponent>*/ getActions(XComponent dataRow, boolean editable, boolean statusChangeable) {
      if (!columnHoldsValue(dataRow, List.class, ACTIONS_COLUMN_INDEX)) {
         updateActions(dataRow, editable, statusChangeable);
      }
      return getActionsFromDataRow(dataRow);
   }
   
   /**
    * @param row
    * @param project
    * @return
    * @pre
    * @post
    */
   public static List/*<XComponent>*/ getActionsFromDataRow(XComponent dataRow) {
      return (columnHoldsValue(dataRow, List.class, ACTIONS_COLUMN_INDEX) ? 
         ((List) getSomething(dataRow, ACTIONS_COLUMN_INDEX)) :
            null);
   }

   
   public static void setMasterActivity(XComponent dataRow, String masterActivity) {
      setSomething(dataRow, MASTER_ACTIVITY_COLUMN_INDEX, masterActivity);
   }

   public static String getMasterActivity(XComponent dataRow) {
      return columnHoldsValue(dataRow, String.class, MASTER_ACTIVITY_COLUMN_INDEX) ? (String) getSomething(dataRow, MASTER_ACTIVITY_COLUMN_INDEX) : null;
   }

   public static void setSubProject(XComponent dataRow, String subProject) {
      setSomething(dataRow, SUB_PROJECT_INDEX, subProject);
   }

   public static String getSubProject(XComponent dataRow) {
      return columnHoldsValue(dataRow, String.class, SUB_PROJECT_INDEX) ? (String) getSomething(dataRow, SUB_PROJECT_INDEX) : null;
   }

   /**
    * Sets the status of the given activity row.
    *
    * @param dataRow activity row
    * @param status   possible values include {@link OVERDUE_ACTIVITY}, {@link NOT_OVERDUE_ACTIVITY}
    */
   public static void setStatus(XComponent dataRow, int status) {
      setSomething(dataRow, STATUS_COLUMN_INDEX, new Integer(status));
   }

   /**
    * @param dataRow activity row
    * @return the status of value of the given activity row.
    */
   public static int getStatus(XComponent dataRow) {
      return columnHoldsValue(dataRow, String.class, STATUS_COLUMN_INDEX) ?
           ((Integer) getSomething(dataRow, STATUS_COLUMN_INDEX)).intValue() : 0;
   }

   public Date dateFieldValue(String fieldId) {
      // Validator must also work stand-alone (outside a form)
      XComponent form = data_set.getForm();
      if (form == null) {
         return null;
      }
      XComponent field = form.findComponent(fieldId);
      if (field == null) {
         return null;
      }
      return field.getDateValue();
   }

   /**
    * Set date value to midnight (00:00:00.000)
    * @param d
    * @return
    */
   public static Date normalizeDate(Date d) {
      if (d == null) {
         return null;
      }
      long fixed = d.getTime();
      return new Date(fixed - fixed % XCalendar.MILLIS_PER_DAY);
   }

   /**
    * @return Project start date if it was set on the form, null otherwise
    */
   public Date getProjectStart() {
      if (projectStart == null) {
         projectStart = dateFieldValue(PROJECT_START);
         if (projectStart == null) {
            projectStart = DATE_SENTINEL;
         }
      }
      if (projectStart == DATE_SENTINEL) {
         return null;
      }
      return projectStart;
   }

   public void setProjectStart(Date start) {
      projectStart = new Date(start.getTime());
      projectWorkingStart = null;
   }

   /**
    * Returns the first working day after the project start if project start != null, null otherwise
    *
    * @return first working day after the project start (null if project has no start date).
    */
   public Date getWorkingProjectStart() {

      if (projectWorkingStart == null) {

         Date projectStart = getProjectStart();
         if (projectStart == null) {
            projectWorkingStart = DATE_SENTINEL;
         }
         else {
            if (!getCalendar().isWorkDay(projectStart)) {
               projectWorkingStart = getCalendar().nextWorkDay(projectStart);
            }
            else {
               projectWorkingStart = projectStart;
            }
         }
      }
      if (projectWorkingStart == DATE_SENTINEL) {
         return null;
      }
      else {
         return projectWorkingStart;
      }
   }

   public Date getWorkingProjectFinish() {

      if (projectWorkingFinish == null) {

         Date projectFinish = getProjectFinish();
         if (projectFinish == null) {
            projectWorkingFinish = DATE_SENTINEL;
         }
         else {
            if (!getCalendar().isWorkDay(projectFinish)) {
               projectWorkingFinish = getCalendar().nextWorkDay(projectFinish, -1);
            }
            else {
               projectWorkingFinish = projectFinish;
            }
         }
      }
      if (projectWorkingFinish == DATE_SENTINEL) {
         return null;
      }
      else {
         return projectWorkingFinish;
      }
   }


   /**
    * @return Project finish date if it was set on the form (or through the setter), null otherwise
    */
   public Date getProjectFinish() {
      if (projectFinish == null) {
         projectFinish = dateFieldValue(PROJECT_FINISH);
         if (projectFinish == null) {
            projectFinish = DATE_SENTINEL;
         }
      }
      if (projectFinish == DATE_SENTINEL) {
         return null;
      }
      return projectFinish;
   }

   /**
    * Sets the project end date.
    *
    * @param finish End Date.
    */
   public void setProjectFinish(Date finish) {
      projectFinish = finish;
      projectWorkingFinish = null;
   }

   /**
    * Sets the project plan end date.
    *
    * @param finish End Date.
    */
   public void setProjectPlanFinish(Date finish) {
      projectPlanFinish = finish;
   }

   public Date getProjectPlanFinish() {
      if (projectPlanFinish == null) {
         //project plan finish as max of the activities ends
         for (int i = 0; i < data_set.getChildCount(); i++) {
            XComponent dataRow = (XComponent) data_set.getChild(i);
            Date end = getEnd(dataRow);
            if (end != null) {
               if (projectPlanFinish == null) {
                  projectPlanFinish = new Date(end.getTime());
               }
               else {
                  if (projectPlanFinish.before(end)) {
                     projectPlanFinish = new Date(end.getTime());
                  }
               }
            }
         }
         if (projectPlanFinish == null) {
            //if project plan still null, use the project end
            projectPlanFinish = getProjectFinish();
         }
         if (projectPlanFinish == null) {
            //if project end is null, use project start
            projectPlanFinish = getProjectStart();
         }
      }
      return projectPlanFinish;
   }

   /**
    * Updates the project plan finish date and triggers the rest of the updates
    * (for the values constrained by the project plan end).
    */
   protected void updateProjectPlanFinish() {
      Date oldProjectPlan = null;
      if (projectPlanFinish != null) {
         oldProjectPlan = new Date(projectPlanFinish.getTime());
      }
      projectPlanFinish = null;

      //get project plan finish will update the value
      Date newProjectPlanFinish = getProjectPlanFinish();

      //if the project finish is null or the new date != old date, update the task costs
      if (getProjectFinish() == null || (oldProjectPlan == null || !newProjectPlanFinish.equals(oldProjectPlan))) {
         for (int i = 0; i < data_set.getChildCount(); i++) {
            XComponent activityRow = (XComponent) data_set.getChild(i);
            if (getType(activityRow) == TASK) {
               adjustTaskActivity(activityRow);
            }
         }
      }
   }


   /**
    * @param property
    * @return Project start date if it was set on the form, null otherwise
    */
   public Object projectSetting(String property) {
      if (data_set.getForm() != null) {
         XComponent projectSettings = data_set.getForm().findComponent(PROJECT_SETTINGS_DATA_SET);
         if (projectSettings != null) {
            for (int i = 0; i < projectSettings.getChildCount(); i++) {
               XComponent row = (XComponent) projectSettings.getChild(i);
               String name = ((XComponent) row.getChild(0)).getStringValue();
               if (property.equals(name)) {
                  Object value = ((XComponent) row.getChild(1)).getValue();
                  return value;
               }
            }
         }
      }
      return null;
   }

   public void setHourBasedResourceView(boolean hourBasedView) {
      this.hourBasedResources = Boolean.valueOf(hourBasedView);
   }

   public boolean isHourBasedResourceView() {
      if (hourBasedResources == null) {
         if (data_set.getForm() != null) {
            XComponent hourField = data_set.getForm().findComponent(SHOW_RESOURCE_HOURS);
            if (hourField != null) {
               hourBasedResources = Boolean.valueOf(hourField.getBooleanValue());
            }
            else {
               hourBasedResources = Boolean.FALSE;
            }
         }
         else {
            hourBasedResources = Boolean.FALSE;
         }
      }
      return hourBasedResources.booleanValue();
   }

   public final void setCalculationMode(Byte calculationMode) {
      this.calculationMode = calculationMode;
   }

   public final Byte getCalculationMode() {
      if (calculationMode == null) {
         calculationMode = (Byte) projectSetting(CALCULATION_MODE);
      }
      return calculationMode;
   }

   public final void setProjectTemplate(Boolean template) {
      this.projectTemplate = template;
   }

   /**
    * @return true if the project is a template and false otherwise.
    */
   public final Boolean getProjectTemplate() {
      if (projectTemplate == null) {
         Boolean template = (Boolean) projectSetting(PROJECT_TEMPLATE);
         if (template != null) {
            projectTemplate = template;
         }
         else {
            projectTemplate = Boolean.FALSE;
         }
      }
      return projectTemplate;
   }
   
   public boolean isTemplate() {
      return getProjectTemplate().booleanValue();
   }

   public final void setProgressTracked(Boolean progressTracked) {
      this.progressTracked = progressTracked;
   }

   public final Boolean getProgressTracked() {
      if (progressTracked == null) {
         progressTracked = (Boolean) projectSetting(PROGRESS_TRACKED);
      }
      return progressTracked;
   }

   public boolean isProgressTracked() {
      Boolean pt = getProgressTracked();
      return pt != null ? pt.booleanValue() : true;
   }
   
   /**
    * @return The set of absences used by the validator in the update process
    */
   public Map getAbsences() {
      Map absences = null;
      XComponent form = data_set.getForm();
      if (form != null) {
         XComponent absencesField = form.findComponent(ABSENCES_ID);
         if (absencesField != null) {
            absences = (Map) absencesField.getValue();
         }
      }
      return absences == null ?  new HashMap():  absences;
   }

   // Helpers for validation code

   /**
    * Get the super activity (collection activity in fact) for a custom activity
    *
    * @param activity the activity
    * @return the super activity or null if the activity is outline level 0
    */
   public XComponent superActivity(XComponent activity) {
      // *** Performance: Maybe cache super-activity somehow?
      int index = activity.getIndex() - 1;
      int outline_level = activity.getOutlineLevel();
      XComponent previous = null;
      while (index >= 0) {
         previous = (XComponent) (data_set.getChild(index));
         if (previous.getOutlineLevel() < outline_level) {
            return previous;
         }
         index--;
      }
      return null;
   }


   protected List getSubElements(XComponent activity, java.util.Set activityTypes, boolean childrenOnly) {
      List sub_activities = new ArrayList();
      int sub_outline_level = activity.getOutlineLevel() + 1;
      int index = activity.getIndex() + 1;
      XComponent next = null;
      while (index < data_set.getChildCount()) {
         next = (XComponent) (data_set.getChild(index));
         if (next.getOutlineLevel() < sub_outline_level) {
            return sub_activities;
         }
         if (next.getOutlineLevel() == sub_outline_level || !childrenOnly) {
            if (activityTypes == null || activityTypes.contains(new Byte(getType(next)))) {
               sub_activities.add(next);
            }
         }
         index++;
      }
      return sub_activities;
   }
   
   /**
    * Returns the subactivities (with outline level greater with 1) of a collection activity.
    *
    * @param activity the collection activity
    * @return an XArray<Integer> of subactivities
    */
   public List subActivities(XComponent activity) {
      java.util.Set actTypes = new HashSet();
      actTypes.add(new Byte(STANDARD));
      actTypes.add(new Byte(MILESTONE));
      actTypes.add(new Byte(COLLECTION));
      return getSubElements(activity, actTypes, true);
   }

   public List subTasks(XComponent activity) {
      java.util.Set actTypes = new HashSet();
      actTypes.add(new Byte(TASK));
      actTypes.add(new Byte(COLLECTION_TASK));
      actTypes.add(new Byte(ADHOC_TASK));
      actTypes.add(new Byte(SCHEDULED_TASK));
      return getSubElements(activity, actTypes, true);
   }

   /**
    * Check if a activity is a collection activity (it has activities with outline level greater)
    *
    * @param activity the activity
    * @return boolean true if the activity is a collection ,false otherwise
    */
   protected boolean isCollectionActivity(XComponent activity) {
      int index = activity.getIndex() + 1;
      if (index < data_set.getChildCount()) {
         XComponent next = (XComponent) (data_set.getChild(index));
         return next.getOutlineLevel() > activity.getOutlineLevel();
      }
      return false;
   }

   /**
    * Method checks if a activity is a an independent one. If the super activity is independent and it is a collection
    * the method returns true;
    *
    * @param activity the activity
    * @return boolean true if the activity is independent ,false otherwise
    */
   public boolean isIndependentActivity(XComponent activity) {
      // Returns true if there are no direct or indirec predecessors
      SortedMap predecessors = getPredecessors(activity);
      if ((predecessors == null) || (predecessors.size() == 0)) {
         XComponent super_activity = superActivity(activity);
         if (super_activity == null) {
            return true;
         }
         else {
            return isIndependentActivity(super_activity);
         }
      }
      else {
         return false;
      }
   }

   /**
    * Updates the type of all the activities from the data set and returns if necessary the fixed activities from the
    * dataset.
    *
    * @param find_fixed_activities - if true, fixed activities will be returned
    * @return fixed activities if find_fixed_activities == true
    */
   protected List updateActivityTypes(boolean find_fixed_activities) {
      // Find fixed activities (no predecessors) *** recalculate end-times
      List fixed_activities = null;
      if (find_fixed_activities) {
         fixed_activities = new ArrayList();
      }

      XComponent activity;
      boolean isCollection;
      boolean isTask;

      for (int index = 0; index < data_set.getChildCount(); index++) {
         activity = (XComponent) data_set._getChild(index);
         // Only normal activities that have no predecessors are fixed
         adjustTypeForActivity(activity);
      }

      for (int index = 0; index < data_set.getChildCount(); index++) {
         activity = (XComponent) data_set._getChild(index);
         isTask = isTaskType(activity);
         isCollection = (getType(activity) == COLLECTION);

         if (find_fixed_activities) {
            if (!isCollection && isIndependentActivity(activity) && !isTask) {
               fixed_activities.add(activity);
            }
            else {
               if (!isCollectionTask(activity)) {
                  setStart(activity, null);
                  setEnd(activity, null);
               }
            }
         }
      }
      if (find_fixed_activities) {
         logger.debug("Fixed activities # " + fixed_activities.size());
      }
      return fixed_activities;
   }

   /**
    * Updates the type for a given activity.
    *
    * @param activity the activity for which the type update is required
    * @return the new type
    */
   protected byte adjustTypeForActivity(XComponent activity) {
      if (importedActivity(activity)) {
         return getType(activity);
      }
      boolean isCollection = isCollectionActivity(activity);
      boolean isTask = (getStart(activity) == null);
      boolean isTaskCollection = isCollectionTask(activity);
      boolean isScheduledTask = isScheduledTask(activity);
      boolean isMilestone = isMilestone(activity);

      if (isTask && isTaskCollection) {
         updateType(activity, COLLECTION_TASK);
         return COLLECTION_TASK;
      }
      else if (isScheduledTask) {
         updateType(activity, SCHEDULED_TASK);
         return SCHEDULED_TASK;
      }
      else if (isCollection && !isTaskCollection) {
         updateType(activity, COLLECTION);
         return COLLECTION;
      }
      else if (isTask) {
         updateType(activity, TASK);
         return TASK;
      }
      else if (isMilestone) {
         updateType(activity, MILESTONE);
         return MILESTONE;
      }
      else {
         updateType(activity, STANDARD);
         return STANDARD;
      }
   }

   private boolean isScheduledTask(XComponent activity) {
      List subTasks = subTasks(activity);
      return subTasks.size() != 0 && subActivities(activity).size() == 0;
   }

   private boolean isMilestone(XComponent activity) {
      return getDuration(activity) == 0.0;
   }

   /**
    * Tells if a given activity is a task or collection task.
    *
    * @param activity Activity to be tested
    * @return true if the activity is a task or a collection task
    */
   public static boolean isTaskType(XComponent activity) {
      return getType(activity) == TASK || getType(activity) == COLLECTION_TASK || getType(activity) == ADHOC_TASK;
   }

   /**
    * Tests if a given activity is a task collection type activity based on the start date and children
    *
    * @param activity Activity to be tested
    * @return true if the start date is null
    */
   private boolean isCollectionTask(XComponent activity) {
      int index = activity.getIndex() + 1;
      if (index < data_set.getChildCount()) {
         //next activity
         XComponent next = (XComponent) (data_set.getChild(index));
         if (next.getOutlineLevel() <= activity.getOutlineLevel()) {
            return false;
         }
         //all the children must be tasks
         while (index < data_set.getChildCount() && next.getOutlineLevel() > activity.getOutlineLevel()) {
            if (getStart(next) != null) {
               return false;
            }
            index++;
            if (index < data_set.getChildCount()) {
               next = (XComponent) (data_set.getChild(index));
            }
         }
         return true;
      }
      return false;
   }

   public void updateType(XComponent activity, byte type) {
      if (importedActivity(activity)) {
         return;
      }
      // Update type and disable non-editable cells for collections and milestones
      byte oldType = getType(activity);
      if (type != oldType) {
         setType(activity, type);
      }
      boolean enableComplete = false;
      if (!isProgressTracked()) {
         enableComplete = true;
      }
      activity.getChild(NAME_COLUMN_INDEX).setEnabled(true);
      double oldBase = getBaseEffort(activity);
      switch (type) {
         case COLLECTION:
            activity.getChild(START_COLUMN_INDEX).setEnabled(false);
            activity.getChild(COMPLETE_COLUMN_INDEX).setEnabled(false);
            activity.getChild(FINISH_COLUMN_INDEX).setEnabled(false);
            activity.getChild(VISUAL_RESOURCES_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_EFFORT_COLUMN_INDEX).setEnabled(false);
            activity.getChild(DURATION_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_TRAVEL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_MATERIAL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_EXTERNAL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(PRIORITY_COLUMN_INDEX).setEnabled(false);
            activity.getChild(PAYMENT_COLUMN_INDEX).setEnabled(false);
            // a "new collection" will be expanded.
            if (oldType != COLLECTION) {
               activity.expanded(true, false);
            }
            setPriority(activity, null);
            setResources(activity, new ArrayList());
            setVisualResources(activity, new ArrayList());
            setWorkBreaks(activity, new TreeMap());
            setAttribute(activity, INCOMPLETE_ASSIGNMENT, false);
            break;
         case MILESTONE:
            activity.getChild(START_COLUMN_INDEX).setEnabled(true);
            activity.getChild(FINISH_COLUMN_INDEX).setEnabled(true);
            activity.getChild(COMPLETE_COLUMN_INDEX).setEnabled(enableComplete);
            activity.getChild(VISUAL_RESOURCES_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_EFFORT_COLUMN_INDEX).setEnabled(true);
            activity.getChild(DURATION_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_TRAVEL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_MATERIAL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_EXTERNAL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(PAYMENT_COLUMN_INDEX).setEnabled(true);
            // Clear not-relevant values (or do this during or after validation?)
            setBaseEffort(activity, 0.0d);
            setDuration(activity, 0.0d);
            setWorkPhases(activity, new TreeMap());
            //no costs for milestones
            setBasePersonnelCosts(activity, 0.0d);
            setBaseProceeds(activity, 0.0d);
            setBaseExternalCosts(activity, 0.0d);
            setBaseMaterialCosts(activity, 0.0d);
            setBaseMiscellaneousCosts(activity, 0.0d);
            setBaseTravelCosts(activity, 0.0d);
            List newResources = setupResources(0d, 0d, getResources(activity),
               getResourceAssignmentRule(activity), false);
            setResources(activity, newResources);
            if (getComplete(activity) == 100) {
               setComplete(activity, 100);
            }
            else {
               setComplete(activity, 0);
            }
            activity.getChild(PRIORITY_COLUMN_INDEX).setEnabled(false);
            setPriority(activity, null);
            setResourceBaseEfforts(activity, new HashMap());
            setWorkBreaks(activity, new TreeMap());
            break;
         case STANDARD:
            activity.getChild(START_COLUMN_INDEX).setEnabled(true);
            activity.getChild(FINISH_COLUMN_INDEX).setEnabled(true);
            activity.getChild(VISUAL_RESOURCES_COLUMN_INDEX).setEnabled(true);
            activity.getChild(COMPLETE_COLUMN_INDEX).setEnabled(enableComplete);
            activity.getChild(BASE_EFFORT_COLUMN_INDEX).setEnabled(true);
            activity.getChild(DURATION_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_TRAVEL_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_MATERIAL_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_EXTERNAL_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(PAYMENT_COLUMN_INDEX).setEnabled(false);
            if (getPriority(activity) == null || !activity.getChild(PRIORITY_COLUMN_INDEX).getEnabled()) {
               activity.getChild(PRIORITY_COLUMN_INDEX).setEnabled(true);
               setPriority(activity, new Byte(DEFAULT_PRIORITY));
            }
            if (activity.expandable()) {
               activity.expanded(true, false);
            }
            resetComplete(activity);
            break;
         case SCHEDULED_TASK:
            activity.getChild(START_COLUMN_INDEX).setEnabled(true);
            activity.getChild(FINISH_COLUMN_INDEX).setEnabled(true);
            activity.getChild(VISUAL_RESOURCES_COLUMN_INDEX).setEnabled(false);
            activity.getChild(COMPLETE_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_EFFORT_COLUMN_INDEX).setEnabled(false);
            activity.getChild(DURATION_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_TRAVEL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_MATERIAL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_EXTERNAL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(PRIORITY_COLUMN_INDEX).setEnabled(false);
            activity.getChild(PAYMENT_COLUMN_INDEX).setEnabled(false);
            setPriority(activity, null);
            setWorkPhases(activity, new TreeMap());
            setResources(activity, new ArrayList());
            setVisualResources(activity, new ArrayList());
            if (activity.expandable()) {
               activity.expanded(true, false);
            }
            setLeadTime(activity, 0d);
            setFollowUpTime(activity, 0d);
            setWorkBreaks(activity, new TreeMap());
            setAttribute(activity, INCOMPLETE_ASSIGNMENT, false);
            break;
         case COLLECTION_TASK:
            activity.getChild(START_COLUMN_INDEX).setEnabled(false);
            activity.getChild(FINISH_COLUMN_INDEX).setEnabled(false);
            activity.getChild(COMPLETE_COLUMN_INDEX).setEnabled(false);
            activity.getChild(VISUAL_RESOURCES_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_EFFORT_COLUMN_INDEX).setEnabled(false);
            activity.getChild(DURATION_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_TRAVEL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_MATERIAL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_EXTERNAL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(PRIORITY_COLUMN_INDEX).setEnabled(false);
            activity.getChild(PAYMENT_COLUMN_INDEX).setEnabled(false);

            setDuration(activity, 0.0d);
            setStart(activity, null);
            setEnd(activity, null);
            setResourceBaseEfforts(activity, new HashMap());
            setWorkPhases(activity, new TreeMap());
            setResources(activity, new ArrayList());
            setVisualResources(activity, new ArrayList());
            breakAllLinks(activity);
            setPriority(activity, null);
            if (oldType != COLLECTION_TASK) {
               activity.expanded(true, false);
            }
            setLeadTime(activity, 0d);
            setFollowUpTime(activity, 0d);
            setWorkBreaks(activity, new TreeMap());
            setAttribute(activity, INCOMPLETE_ASSIGNMENT, false);
            break;
         case TASK:
            activity.getChild(START_COLUMN_INDEX).setEnabled(true);
            activity.getChild(FINISH_COLUMN_INDEX).setEnabled(true);
            activity.getChild(VISUAL_RESOURCES_COLUMN_INDEX).setEnabled(true);
            activity.getChild(COMPLETE_COLUMN_INDEX).setEnabled(enableComplete);
            activity.getChild(BASE_EFFORT_COLUMN_INDEX).setEnabled(true);
            activity.getChild(DURATION_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_TRAVEL_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_MATERIAL_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_EXTERNAL_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(PRIORITY_COLUMN_INDEX).setEnabled(true);
            activity.getChild(PAYMENT_COLUMN_INDEX).setEnabled(false);
            // Clear values

            setDuration(activity, 0.0d);
            setStart(activity, null);
            setEnd(activity, null);
            updateProjectPlanFinish();
            newResources = new ArrayList();
            setResourceBaseEfforts(activity, new HashMap());
            if (getResources(activity) != null && getResources(activity).size() > 0) {
               newResources.add(getResources(activity).get(0));
               setResourceBaseEffort(activity, XValidator.choiceID((String) getResources(activity).get(0)), getBaseEffort(activity));
            }
            setResourcesForActivity(activity, newResources);

            //update the base personnel costs
            adjustTaskActivity(activity);
            //break all the links
            breakAllLinks(activity);

            resetComplete(activity);

            if (getPriority(activity) == null || !activity.getChild(PRIORITY_COLUMN_INDEX).getEnabled()) {
               activity.getChild(PRIORITY_COLUMN_INDEX).setEnabled(true);
               setPriority(activity, new Byte(DEFAULT_PRIORITY));
            }
            setLeadTime(activity, 0d);
            setFollowUpTime(activity, 0d);
            setWorkBreaks(activity, new TreeMap());
            break;
      }
      adjustRemainingEffort(activity, getBaseEffort(activity) - oldBase, isProgressTracked());
   }

   private void resetComplete(XComponent activity) {
      if (isProgressTracked()) {
         if (getResources(activity).isEmpty() && getComplete(activity) != 0) {
            setComplete(activity, 0);
         }
      }
   }

   /**
    * Breaks all the links from and to this activity (successors and predecessors)
    *
    * @param activity activity that will have its links removed
    */
   protected void breakAllLinks(XComponent activity) {
      SortedMap successors = getSuccessors(activity);
      for (Iterator iterator = successors.keySet().iterator(); iterator.hasNext();) {
         int index = ((Integer) iterator.next()).intValue();
         XComponent otherActivity = (XComponent) data_set.getChild(index);
         SortedMap preds = getPredecessors(otherActivity);
         preds.remove(new Integer(activity.getIndex()));
      }
      setSuccessors(activity, new TreeMap());

      SortedMap predecessors = getPredecessors(activity);
      for (Iterator iterator = predecessors.keySet().iterator(); iterator.hasNext();) {
         int i = ((Integer) iterator.next()).intValue();
         XComponent otherActivity = (XComponent) data_set.getChild(i);
         SortedMap succs = getSuccessors(otherActivity);
         succs.remove(new Integer(activity.getIndex()));
      }
      setPredecessors(activity, new TreeMap());
   }

   /**
    * Will update the effort, cost and %completed for the given collection based on the children activities.
    *
    * @param collection - collection activity to be updated. Can be either a normal collection or a collection of tasks.
    */
   //<FIXME> author="Mihai Costin" description="Should be split-up for scheduled activities (have subtasks) and collections (have sub activities)"
   protected void updateCollectionValues(XComponent collection) {
      //<FIXME>
      if (importedActivity(collection)) {
         return;
      }
      double actualSum = 0;
      double remainingSum = 0;
      double baseSum = 0;

      double tasksCompleteSum = 0;
      int tasksChildCount = 0;

      double complete = 0;
      double perCost = 0;
      double proceeds = 0;
      double matCost = 0;
      double travCost = 0;
      double extCost = 0;
      double miscCost = 0;

      //get all the children (including tasks and collection tasks) directly below
      List subActivities = subActivities(collection);
      subActivities.addAll(subTasks(collection));
      for (int i = 0; i < subActivities.size(); i++) {
         XComponent activity = (XComponent) subActivities.get(i);
         byte type = OpGanttValidator.getType(activity);
         //decision 25.04.06 - exclude milestones from %Complete calculations
         if (type == OpGanttValidator.MILESTONE) {
            continue;
         }
         double baseEffort = getBaseEffort(activity);
         double completeValue = getComplete(activity);
         // FIXME: Hack!
         double remainingEffort = 0d;
         double actualEffort = getActualEffort(activity);
         if (isProgressTracked()) {
            remainingEffort = getRemainingEffort(activity);
         }
         else {
            remainingEffort = calculateRemainingEffort(baseEffort,
                  actualEffort, completeValue);
         }

         actualSum += actualEffort;
         remainingSum += remainingEffort;
         baseSum += baseEffort;

         perCost += getBasePersonnelCosts(activity);
         proceeds += getBaseProceeds(activity);
         matCost += getBaseMaterialCosts(activity);
         travCost += getBaseTravelCosts(activity);
         extCost += getBaseExternalCosts(activity);
         miscCost += getBaseMiscellaneousCosts(activity);
      }

      //base effort
      setBaseEffort(collection, baseSum);
      setActualEffort(collection, actualSum);
      setRemainigEffort(collection, remainingSum);
      
      // set the costs
      setBasePersonnelCosts(collection, perCost);
      setBaseProceeds(collection, proceeds);
      setBaseMaterialCosts(collection, matCost);
      setBaseTravelCosts(collection, travCost);
      setBaseExternalCosts(collection, extCost);
      setBaseMiscellaneousCosts(collection, miscCost);

      ProgressTrackableEntityIfc te = new TrackedGanttActivity(collection, this);
      setComplete(collection, getCompleteFromTracking(te, isProgressTracked()));
      
      // collections should have no resources assigned
      setResources(collection, new ArrayList());
   }

   public static interface ProgressTrackableEntityIfc {
      public double getBaseEffort();
      public double getActualEffort();
      public double getOpenEffort();
      
      public double getComplete();
      public void setComplete(double complete);
      
      public boolean isTrackingLeaf();
      public boolean isIndivisible();
      
      Set getTrackedSubElements();
   }
   
   private static class TrackedGanttActivity implements ProgressTrackableEntityIfc {

      private XComponent dataRow = null;
      private OpGanttValidator validator = null;

      public TrackedGanttActivity(XComponent dataRow, OpGanttValidator validator) {
         this.dataRow = dataRow;
         this.validator = validator;
      }
      
      public double getActualEffort() {
         return OpGanttValidator.getActualEffort(dataRow);
      }

      public double getBaseEffort() {
         return OpGanttValidator.getBaseEffort(dataRow);
      }

      public double getOpenEffort() {
         return OpGanttValidator.getRemainingEffort(dataRow);
      }

      public Set getTrackedSubElements() {
         Set subEs = new HashSet();
         Iterator sit = validator.getSubElements(dataRow, null, true).iterator();
         while (sit.hasNext()) {
            XComponent subRow = (XComponent) sit.next();
            subEs.add(new TrackedGanttActivity(subRow, validator));
         }
         return subEs;
      }

      public boolean isTrackingLeaf() {
         return validator.getSubElements(dataRow, null, true).isEmpty();
      }

      public double getComplete() {
         return OpGanttValidator.getComplete(dataRow);
      }

      public void setComplete(double complete) {
         OpGanttValidator.setComplete(dataRow, complete);
      }

      public boolean isIndivisible() {
         return false;
      }
      
   }
   
   public static double getCompleteFromTracking(ProgressTrackableEntityIfc element, boolean progressTracked) {
      // check, whether use 
      if (element.isTrackingLeaf() && !progressTracked) {
         return element.getComplete();
      }
      if (isIndivisibleElemen(element)) {
         double completedSubElements = 0;
         Set tse = element.getTrackedSubElements();
         if (tse == null || tse.isEmpty()) {
            return progressTracked ? 0d : element.getComplete();
         }
         Iterator sit = tse.iterator();
         while (sit.hasNext()) {
            ProgressTrackableEntityIfc se = (ProgressTrackableEntityIfc) sit.next();
            double complete = se.getComplete();
            completedSubElements += OpGanttValidator
                  .isZeroWithTolerance(complete - 100d,
                        element.getBaseEffort()) ? 1 : 0;
         }
         if (element.isTrackingLeaf()) {
            return completedSubElements == tse.size() ? 100d : 0d;
         }
         else {
            return 100d * completedSubElements / tse.size();
         }
      }
      return OpGanttValidator.calculateCompleteValue(element.getActualEffort(),
            element.getBaseEffort(), element.getOpenEffort());
   }
   
   public static boolean isIndivisibleElemen(ProgressTrackableEntityIfc element) {
      return (OpGanttValidator.isZeroWithTolerance(element.getBaseEffort(),
            element.getBaseEffort()) || OpGanttValidator.isZeroWithTolerance(
            element.getActualEffort(), element.getBaseEffort()))
            && OpGanttValidator.isZeroWithTolerance(element.getOpenEffort(),
                  element.getBaseEffort());
   }
   
   /**
    * Will call <code>updateCollectionValues</code> for every collection
    * activity of the collection tree.
    * 
    * @param activity -
    *           the activity from the tree who's parents will be updated
    * @see OpGanttValidator#updateCollectionValues(onepoint.express.XComponent)
    */
   protected void updateCollectionTreeValues(XComponent activity) {
      if (importedActivity(activity)) {
         return;
      }
      if (isCollectionType(activity)) {
         updateCollectionValues(activity);
      }
      XComponent superActivity = superActivity(activity);
      while (superActivity != null) {
         if (isCollectionType(superActivity)) {
            updateCollectionValues(superActivity);
         }
         superActivity = superActivity(superActivity);
      }
   }

   public static boolean isOfType(XComponent dataRow, byte[] types) {
      byte rowType = getType(dataRow);
      return isOfType(rowType, types);
   }

   public static boolean isOfType(byte rowType, byte[] types) {
      if (types == null) {
         return false;
      }
      for (int i = 0; i < types.length; i++) {
         if (rowType == types[i]) {
            return true;
         }
      }
      return false;
   }
   
   public static boolean isCollectionType(XComponent activity) {
      return isOfType(activity, COLLECTION_TYPES);
   }

   public static boolean hasEffort(XComponent activity) {
      return isOfType(activity, EFFORT_TYPES);
   }

   public boolean validateDataSet() {
      if (disableValidation) {
         return true;
      }
      
      if (getValidationStartPoints().isEmpty()) {
         addValidationStartPoints(getIndependentActivities());
      }
      validateStartPoints(false);
      return true;
   }

   public boolean validateEntireDataSet() {
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent activityRow = (XComponent) data_set.getChild(i);
         addValidationStartPoint(activityRow);
      }
      validateStartPoints(true);
      return true;
   }

   /*
    * public boolean validateDataRow(XComponent dataRow) { return false; }
    */

   private static final int[] ENABLED_DATA_CELLS = { NAME_COLUMN_INDEX,
         START_COLUMN_INDEX, FINISH_COLUMN_INDEX, DURATION_COLUMN_INDEX,
         BASE_EFFORT_COLUMN_INDEX, VISUAL_RESOURCES_COLUMN_INDEX,
         PRIORITY_COLUMN_INDEX, PREDECESSORS_VISUALIZATION_COLUMN_INDEX,
         BASE_EXTERNAL_COSTS_COLUMN_INDEX, BASE_MATERIAL_COSTS_COLUMN_INDEX,
         BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX, BASE_TRAVEL_COSTS_COLUMN_INDEX };
   
   /**
    * @see onepoint.express.XValidator#newDataRow()
    */
   public XComponent newDataRow() {
      logger.debug("OpGanttValidator.newDataRow()");
      // *** TODO: Can we leave some values "blank"/null?
      // ==> At least not null [currently]: Problem w/exceptions in editors
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      setName(dataRow, null);
      setType(dataRow, STANDARD);
      setCategory(dataRow, null);
      setComplete(dataRow, 0d);
      // set a reasonable start date:
      Date start = isTemplate() ? getDefaultTemplateStart() : OpProjectCalendar.today();
      if (!getCalendar().isWorkDay(start)) {
         start = getCalendar().nextWorkDay(start);
      }
      setStart(dataRow, start);
      setEnd(dataRow, new Date(start.getTime()
            + (long) (((getCalendar().getWorkHoursPerWeek() / getCalendar()
                  .getWorkHoursPerDay()) - 1d) * XCalendar.MILLIS_PER_DAY)));
      setBaseEffort(dataRow, isEffortBasedProject() ? getCalendar().getWorkHoursPerWeek() : 0d);
      setDuration(dataRow, getCalendar().getWorkHoursPerWeek());
      setPredecessors(dataRow, new TreeMap());
      setPredecessorVisualization(dataRow, "");
      setSuccessors(dataRow, new TreeMap());
      setBasePersonnelCosts(dataRow, 0d);
      setBaseProceeds(dataRow, 0d);
      setBaseExternalCosts(dataRow, 0d);
      setBaseMaterialCosts(dataRow, 0d);
      setBaseMiscellaneousCosts(dataRow, 0d);
      setBaseTravelCosts(dataRow, 0d);
      setDescription(dataRow, null);
      setAttachments(dataRow, new ArrayList());
      setAttributes(dataRow, 0);
      setWorkPhases(dataRow, new TreeMap());
      setResources(dataRow, new ArrayList());
      setResourceBaseEfforts(dataRow, new HashMap());
      setPriority(dataRow, new Byte(DEFAULT_PRIORITY));
      setWorkRecords(dataRow, new HashMap());
      setActualEffort(dataRow, 0d);
      setVisualResources(dataRow, new ArrayList());
      setResponsibleResource(dataRow, null);
      setPayment(dataRow, 0d);
      setEffortBillable(dataRow, 100);
      setCustomAttributes(dataRow, null);
      
      adjustRemainingEffort(dataRow, getBaseEffort(dataRow), isProgressTracked());
      
      adjustActivityToResourcesAndDuration(dataRow, true, false);
      
      // enable cells:
      for (int i = 0; i < ENABLED_DATA_CELLS.length; i++) {
         dataRow.getDataCell(ENABLED_DATA_CELLS[i]).setEnabled(true);
      }
      // there are always some exceptions...
      dataRow.getDataCell(COMPLETE_COLUMN_INDEX).setEnabled(!isProgressTracked());

      ensureChildCreated(dataRow, NUMBER_OF_COLUMNS);
      return dataRow;
   }

   /**
    * @return the default start date for a template project (01.01.2001)
    */
   public static Date getDefaultTemplateStart() {
      Date start;
      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.YEAR, 2001);
      calendar.set(Calendar.MONTH, 1);
      calendar.set(Calendar.DAY_OF_YEAR, 1);
      start = new Date(calendar.getTime().getTime());
      return start;
   }

   /**
    * @see XValidator#addDataRow(onepoint.express.XComponent)
    */
   public void addDataRow(XComponent dataRow) {
      addValidationStartPoint(dataRow);
      addDataRow(data_set.getChildCount(), dataRow);
   }

   /**
    * @see XValidator#addDataRow(int,onepoint.express.XComponent)
    */
   public void addDataRow(int index, XComponent dataRow)
        throws XValidationException {
      
      if (index < data_set.getChildCount()) {
         XComponent previousRow = (XComponent) data_set.getChild(index);
         addValidationStartPoint(previousRow);
         XComponent parent = superActivity(previousRow);
         if (parent != null) {
            addValidationStartPoint(parent);
         }
      }
      addValidationStartPoint(dataRow);

      if (importedActivity(dataRow)) {
         return;
      }
      addToUndo();
      // Insert data-row at index position
      // *** TODO -- important: Do index link management
      // ==> Update all references to all rows "behind" this new row
      // ==> Check if predecessor or successor contains i > index: Increment
      if (index > data_set.getChildCount()) {
         index = data_set.getChildCount();
      }

      
      //If the row that will be added is the first child of it's predecessor
      //check if the predecessor activity has any workslips
      if (index > 0) {
         // adjsut ouline level:
         XComponent parent = (XComponent) data_set.getChild(index - 1);
         while (parent != null && importedActivity(parent)) {
            index = parent.getIndex() + 1;
            parent = superActivity(parent);
         }
         if (parent != null) {
            if (parent.getOutlineLevel() < dataRow.getOutlineLevel()) {
               dataRow.setOutlineLevel(parent.getOutlineLevel() + 1);
               checkDeletedAssignmentsForWorkslips(parent, new ArrayList());
            }
         }
         else {
            dataRow.setOutlineLevel(0);
            index--;
         }
      }

      XComponent updated_dataRow;
      for (int i = 0; i < data_set.getChildCount(); i++) {
         updated_dataRow = (XComponent) (data_set._getChild(i));
         setSuccessors(updated_dataRow, updateIndexListAfterAdd(getSuccessors(updated_dataRow), index));
         setPredecessors(updated_dataRow, updateIndexListAfterAdd(getPredecessors(updated_dataRow), index));
      }
      if (index < data_set.getChildCount()) {
         XComponent previousChild = (XComponent) data_set.getChild(index);
         if (index > 0) {
            previousChild = (XComponent) data_set.getChild(index - 1);
         }

         if (getType(previousChild) == TASK || getType(previousChild) == COLLECTION_TASK) {
            updateType(dataRow, TASK);
         }

         if (isCollectionType(previousChild)) {
            clearCollectionActivity(previousChild);
         }

         setCategory(dataRow, getCategory(previousChild));
      }
      data_set.addChild(index, dataRow);
      validateDataSet();
   }

   private void clearCollectionActivity(XComponent previousChild) {
      setComplete(previousChild, 0);
      setBaseExternalCosts(previousChild, 0);
      setBaseMaterialCosts(previousChild, 0);
      setBaseMiscellaneousCosts(previousChild, 0);
      setBasePersonnelCosts(previousChild, 0);
      setBaseTravelCosts(previousChild, 0);
      clearEffortBillable(previousChild);
   }

   /**
    * @throws XValidationException if all the removed rows are mandatory
    * @see XValidator#removeDataRows(java.util.List)
    */
   public boolean removeDataRows(List dataRows) {
      preCheckRemoveDataRows(dataRows);
      addToUndo();

      //start points = all direct successors and owning collection for each removed data row
      for (int i = 0; i < dataRows.size(); i++) {
         XComponent row = (XComponent) dataRows.get(i);
         SortedMap successorsIndexes = OpGanttValidator.getSuccessors(row);
         List successors = new ArrayList();
         for (Iterator iterator = successorsIndexes.keySet().iterator(); iterator.hasNext();) {
            Integer index = (Integer) iterator.next();
            XComponent successor = (XComponent) data_set.getChild(index.intValue());
            successors.add(successor);
         }
         addValidationStartPoints(successors);
         XComponent collection = superActivity(row);
         if (collection != null) {
            addValidationStartPoint(collection);
         }
         _removeDataRow(row);
      }
      removeValidationStartPoints(dataRows);

      validateDataSet();
      return true;
   }

   /**
    * Performes a suite of checks that must be done before the actual remove action
    *
    * @param dataRows data rows that will be removed.
    * @throws XValidationException MANDATORY_EXCEPTION if all "to be removed" activities are mandatory
    */
   protected void preCheckRemoveDataRows(List dataRows) {
      boolean allMandatory = true;
      boolean isProgramMember = false;
      for (int i = 0; i < dataRows.size(); i++) {
         XComponent dataRow = (XComponent) (dataRows.get(i));
         if (!isProjectMandatory(dataRow)) {
            allMandatory = false;
            break;
         }
      }
      
      List rootRows = findRootRows(dataRows);
      Iterator rit = rootRows.iterator();
      while (rit.hasNext()) {
         XComponent dataRow = (XComponent) rit.next();
         if (importedActivity(dataRow) && !importedHeadRow(dataRow)) {
            throw new XValidationException(PROGRAM_ELEMENT_DELETE_EXCEPTION);
         }
      }

      if (mandatoryCollectionCheck(dataRows)) {
         throw new XValidationException(MANDATORY_EXCEPTION);
      }

      if (allMandatory) {
         throw new XValidationException(MANDATORY_EXCEPTION);
      }

      //we cannot remove activities if we have assignments with workrecords.
      for (int i = 0; i < dataRows.size(); i++) {
         XComponent dataRow = (XComponent) dataRows.get(i);
         checkDeletedAssignmentsForWorkslips(dataRow, new ArrayList());
      }
   }

   /**
    * Removes the <code>dataRow</code> from the data set.
    *
    * @param dataRow the <code>DATA_ROW</code> which will be removed
    */
   protected void _removeDataRow(XComponent dataRow) {
      // *** Maybe just use index as argument (in this case also for setValue)
      // *** First, link-management: Keep predecessors/successors consistent
      // *** Then, remove & validate; note: Incremental validation possible
      int index = dataRow.getIndex();
      //check for mandatory activities
      XComponent row = (XComponent) data_set.getChild(index);
      if (!isProjectMandatory(row)) {
         //mandatory activities can't be removed
         data_set.removeChild(index);
         // update index list for the data set
         XComponent updated_dataRow = null;
         for (int i = 0; i < data_set.getChildCount(); i++) {
            updated_dataRow = (XComponent) (data_set._getChild(i));
            setSuccessors(updated_dataRow, updateIndexListAfterRemove(getSuccessors(updated_dataRow), index));
            setPredecessors(updated_dataRow, updateIndexListAfterRemove(getPredecessors(updated_dataRow), index));
         }
      }
   }

   static final public int[] MODIFIABLE_FIELDS_IN_IMPORTED_ACTIVITIES = {
         PREDECESSORS_COLUMN_INDEX, SUCCESSORS_COLUMN_INDEX,
         PREDECESSORS_VISUALIZATION_COLUMN_INDEX,
         SUCCESSORS_VISUALIZATION_COLUMN_INDEX };
   
   /**
    * @see XValidator#setDataCellValue(onepoint.express.XComponent,int,Object)
    */
   public void setDataCellValue(XComponent dataRow, int column_index, Object value) {

      if (importedActivity(dataRow)) {
         int i = 0;
         for (i = 0; i < MODIFIABLE_FIELDS_IN_IMPORTED_ACTIVITIES.length; i++) {
            if (column_index == MODIFIABLE_FIELDS_IN_IMPORTED_ACTIVITIES[i]) {
               break;
            }
         }
         if (i == MODIFIABLE_FIELDS_IN_IMPORTED_ACTIVITIES.length) {
            return;
         }
      }

      switch (column_index) {
         case START_COLUMN_INDEX:
            preCheckSetStartValue(dataRow, value);

            if (value == null) {
               //activity becomes a task
               checkDeletedAssignmentsForWorkslips(dataRow, new ArrayList());
               // TODO: maybe clear before...
               // addValidationStartPoints(getAllValidationSuccessors(dataRow));
               addToUndo();
               setStart(dataRow, null);
               // updateCollectionAfterActivityTypeChange(dataRow);
               // adjustTaskActivity(dataRow);
            }
            else {
               Date start = (Date) value;
               addToUndo();
               setStart(dataRow, start);
               if (getEnd(dataRow) == null) {
                  setEnd(dataRow, start);
               }
               // adjustActivityToResourcesAndDuration(dataRow, true, false);
               addValidationStartPoint(dataRow);
            }

            validateDataSet();
            break;

         case FINISH_COLUMN_INDEX:
            preCheckSetEndValue(dataRow, value);

            addToUndo();

            if (value == null) {
               //activity becomes a task
               // addValidationStartPoints(getAllValidationSuccessors(dataRow));
               setStart(dataRow, null);
               // updateCollectionAfterActivityTypeChange(dataRow);
               adjustTaskActivity(dataRow);
            }
            else {
               // Update duration
               Date end = (Date) value;
               // If end is not a workday then go to previous workday
               if (getStart(dataRow) == null) {
                  //if it was a task, set also the start date
                  setStart(dataRow, (Date) value);
               }
               Date start = getStart(dataRow);
               if (end.before(start)) {
                  end = start;
               }
               updateTreeType(dataRow);
               updateFinish(dataRow, end);
               addValidationStartPoint(dataRow);
            }

            validateDataSet();
            break;

         case DURATION_COLUMN_INDEX:
            double duration = preCheckSetDurationValue(dataRow, ((Double) value).doubleValue());

            addToUndo();
            updateDuration(dataRow, duration, true);
            addValidationStartPoint(dataRow);
            validateDataSet();
            break;

         case BASE_EFFORT_COLUMN_INDEX:
            double base_effort = preCheckSetEffortValue(dataRow, ((Double) value).doubleValue(), isEffortBasedProject());

            addToUndo();
            updateBaseEffort(dataRow, base_effort);
            addValidationStartPoint(dataRow);
            validateDataSet();
            break;

         case PREDECESSORS_COLUMN_INDEX:
            addToUndo();
            SortedMap oldPredecessors = getPredecessors(dataRow);
            try {
               setPredecessorsValue((SortedMap) value, dataRow);
               addValidationStartPoint(dataRow);
               validateDataSet();
            }
            catch (XValidationException vx) {
               setPredecessorsValue(oldPredecessors, dataRow);
               addValidationStartPoint(dataRow);
               validateDataSet();
               throw vx;
            }
            break;

         case SUCCESSORS_COLUMN_INDEX:
            addToUndo();
            // Update predecessors of successors
            addValidationStartPoint(dataRow);

            SortedMap removed_successors = setSuccessorsValue((SortedMap) value, dataRow);
            Iterator mit = removed_successors.keySet().iterator();
            while (mit.hasNext()) {
               Integer key = (Integer) mit.next();
               XComponent successor = (XComponent) (data_set._getChild(key.intValue()));
               addValidationStartPoint(successor);
            }
            validateDataSet();
            break;

         case VISUAL_RESOURCES_COLUMN_INDEX:

            List resources = (ArrayList) value;
            adjustResourcesVisualization(resources);
            
            addToUndo();

            //make sure from here onwards, we have all the assignment values in independent format.
            // resources = deLocalizeVisualResources(resources);
            boolean wasEmpty = getResources(dataRow) == null || getResources(dataRow).isEmpty();
            
            //tasks, milestones keep only the resource name
            boolean taskWarning = false;
            if (getType(dataRow) == TASK || getType(dataRow) == MILESTONE) {
               //for tasks only one resource
               ArrayList resourcesValue = ((ArrayList) value);
               if (getType(dataRow) == TASK && resourcesValue.size() > 1) {
                  resources = new ArrayList();
                  resources.add(resourcesValue.get(0));
                  taskWarning = true;
               }
            }
            // assignment has just been set, will no get adjusted (to duration/effort)
            duration = getDuration(dataRow);
            resources = setupResources(duration, getBaseEffort(dataRow),
               resources,  getResourceAssignmentRule(dataRow),
               !isEffortBasedProject(), true);
            checkDeletedAssignmentsForWorkslips(dataRow, resources);
            setResources(dataRow, resources);
            if (isEffortBasedProject()) {
               // adjust duration to effort:
               double assignedDailyEffort = getDailyEffortForAssignments(resources, true);
               double newDuration = Math.ceil(getBaseEffort(dataRow) / assignedDailyEffort) * getCalendar().getWorkHoursPerDay();
               setDuration(dataRow, newDuration);
            }
            adjustActivityToResourcesAndDuration(dataRow, true, false);
            updateResourceVisualization(dataRow);
            updateResponsibleResource(dataRow);

            if (getType(dataRow) == TASK) {
               //effort stays the same.
               updateBaseEffort(dataRow, getBaseEffort(dataRow));
            }

            addValidationStartPoint(dataRow);
            validateDataSet();

            if (taskWarning) {
               throw new XValidationException(TASK_EXTRA_RESOURCE_EXCEPTION);
            }
            break;

         case NAME_COLUMN_INDEX:
            if (isProjectMandatory(dataRow)) {
               if ((getName(dataRow) == null && value != null) || (!getName(dataRow).equals(value))) {
                  throw new XValidationException(MANDATORY_EXCEPTION);
               }
            }
            addToUndo();
            setName(dataRow, (String) value);
            break;
         case DESCRIPTION_COLUMN_INDEX:
            addToUndo();
            setDescription(dataRow, (String) value);
            break;
         case TYPE_COLUMN_INDEX:
            addToUndo();
            setType(dataRow, ((Byte) value).byteValue());
            break;
         case CATEGORY_COLUMN_INDEX:
            if (isProjectMandatory(dataRow)) {
               String newCategoryId = (value != null) ? choiceID((String) value) : NO_CATEGORY_ID;
               String existentCategory = getCategory(dataRow);
               if ((existentCategory == null && !NO_CATEGORY_ID.equalsIgnoreCase(newCategoryId))
                    || (existentCategory != null && !existentCategory.equalsIgnoreCase((String) value))) {
                  throw new XValidationException(MANDATORY_EXCEPTION);
               }
            }
            String categoryChoice = (String) value;
            String categoryId = choiceID(categoryChoice);
            if (categoryId.equals(NO_CATEGORY_ID)) {
               categoryChoice = null;
            }
            addToUndo();
            setCategory(dataRow, categoryChoice);
            break;
         case RESPONSIBLE_RESOURCE_COLUMN_INDEX:
            String resourceChoice = (String) value;
            String resourceId = choiceID(resourceChoice);
            if (resourceId.equals(NO_RESOURCE_ID)) {
               resourceChoice = null;
            }
            addToUndo();
            setResponsibleResource(dataRow, resourceChoice);
            break;
         case LEAD_TIME_COLUMN_INDEX:
            setLeadTime(dataRow, ((Double)value).doubleValue());
            validateDataSet();
            break;
         case FOLLOW_UP_TIME_COLUMN_INDEX:
            setFollowUpTime(dataRow, ((Double)value).doubleValue());
            validateDataSet();
            break;

         case COMPLETE_COLUMN_INDEX:
            // Change percentage range (0-100)
            if (value != null) {
               double complete = ((Double) value).doubleValue();
               byte type = OpGanttValidator.getType(dataRow);

               addToUndo();

               if ((type == OpGanttValidator.MILESTONE) && complete < 100) {
                  setComplete(dataRow, 0);
               }
               else if ((complete >= 0) && (complete <= 100)) {
                  setComplete(dataRow, complete);
               }
               updateCollectionTreeValues(dataRow);
            }
            break;

         case BASE_BILLABLE_COLUMN_INDEX:
            // Change percentage range (0-100)
            if (value != null) {
               double billable = ((Double) value).doubleValue();
               byte type = OpGanttValidator.getType(dataRow);

               addToUndo();

               if ((type == OpGanttValidator.TASK || type == OpGanttValidator.MILESTONE) && billable < 100) {
                  setEffortBillable(dataRow, 0);
               }
               else if ((billable >= 0) && (billable <= 100)) {
                  setEffortBillable(dataRow, billable);
               }
            }
            break;

         case BASE_TRAVEL_COSTS_COLUMN_INDEX:
            double costValue = ((Double) value).doubleValue();
            if (costValue < 0) {
               throw new XValidationException(INVALID_COST_EXCEPTION);
            }
            addToUndo();
            setBaseTravelCosts(dataRow, costValue);
            updateCollectionTreeValues(dataRow);
            break;

         case BASE_MATERIAL_COSTS_COLUMN_INDEX:
            costValue = ((Double) value).doubleValue();
            if (costValue < 0) {
               throw new XValidationException(INVALID_COST_EXCEPTION);
            }
            addToUndo();
            setBaseMaterialCosts(dataRow, costValue);
            updateCollectionTreeValues(dataRow);
            break;

         case BASE_EXTERNAL_COSTS_COLUMN_INDEX:
            costValue = ((Double) value).doubleValue();
            if (costValue < 0) {
               throw new XValidationException(INVALID_COST_EXCEPTION);
            }
            addToUndo();
            setBaseExternalCosts(dataRow, costValue);
            updateCollectionTreeValues(dataRow);
            break;

         case BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX:
            costValue = ((Double) value).doubleValue();
            if (costValue < 0) {
               throw new XValidationException(INVALID_COST_EXCEPTION);
            }
            addToUndo();
            setBaseMiscellaneousCosts(dataRow, costValue);
            updateCollectionTreeValues(dataRow);
            break;

         case ATTACHMENTS_COLUMN_INDEX:
            List attachments = (ArrayList) value;
            setAttachments(dataRow, attachments);
            updateAttachmentAttribute(dataRow);
            break;
         case PRIORITY_COLUMN_INDEX:
            int priority = ((Integer) value).intValue();
            if (priority < 1 || priority > 9) {
               throw new XValidationException(INVALID_PRIORITY_EXCEPTION);
            }
            addToUndo();
            setPriority(dataRow, new Byte((byte) priority));
            break;
         case MODE_COLUMN_INDEX:
            if (value != null) {
               int attrs = ((Integer) value).intValue();
               addToUndo();
               setAttributes(dataRow, attrs);
            }
            break;
         case PAYMENT_COLUMN_INDEX:
            if (value != null) {
               double paymentValue = ((Double) value).doubleValue();
               if (paymentValue < 0) {
                  throw new XValidationException(INVALID_PAYMENT_EXCEPTION);
               }
               setPayment(dataRow, ((Double) value).doubleValue());
            }
            break;
         case PREDECESSORS_VISUALIZATION_COLUMN_INDEX:
            SortedMap predecessors = parsePredecessorVisualization((String) value);
            setDataCellValue(dataRow, PREDECESSORS_COLUMN_INDEX,  predecessors);
            break;
         case WORK_BREAKS_COLUMN_INDEX:
            setWorkBreaks(dataRow, (SortedMap) value);
            adjustActivityToResourcesAndDuration(dataRow, true, false);
            validateDataSet();
            break;
         default: {
            logger.warn("unknown column: "+column_index);
            break;
         }
      }
   }

   protected SortedMap parsePredecessorVisualization(String visualization) {
      SortedMap result = new TreeMap();
      if (visualization == null) {
         return result;
      }
      StringTokenizer t = new StringTokenizer(visualization.trim(), PREDECESSOR_TABLE_DELIMITER);
      while (t.hasMoreTokens()) {
         String elem = t.nextToken();
         Matcher m = PREDECESSOR_PATTERN.matcher(elem);
         if (!m.matches()) {
            throw new XValidationException(DEPENDENCY_FORMAT_EXCEPTION);
         }
         int index = -1;
         try {
            index = Integer.parseInt(m.group(PREDECESSOR_PATTERN_INDEX_GROUP));
         }
         catch (NumberFormatException e) {
            throw new XValidationException(DEPENDENCY_FORMAT_EXCEPTION);
         }
         Map values = new HashMap();
         values.put(DEP_TYPE, new Integer(DEP_DEFAULT));
         result.put(new Integer(index - 1),values);
      }
      return result;
   }

   protected void updateResponsibleResource(XComponent dataRow) {
      if (importedActivity(dataRow)) {
         return;
      }
      if (getResponsibleResource(dataRow) == null) {
         List resources = getResources(dataRow);
         if (resources.size() > 0) {
            String resource = (String) resources.get(0);
            String caption = XValidator.choiceCaption(resource);
            String resName = getResourceName(caption, null);
            String id = XValidator.choiceID(resource);
            setResponsibleResource(dataRow, XValidator.choice(id, resName));
         }
      }
   }

   /**
    * Updates the has attachemt attribute on a data row based on the attachment list.
    *
    * @param dataRow row on which the update is made.
    */
   public static void updateAttachmentAttribute(XComponent dataRow) {
      List attachments = getAttachments(dataRow);
      if (attachments != null) {
         if (attachments.size() != 0) {
            setAttribute(dataRow, HAS_ATTACHMENTS, true);
         }
         else {
            setAttribute(dataRow, HAS_ATTACHMENTS, false);
         }
      }
   }


   protected Map getAvailabilityMap() {
      Map resourceAvailability = new HashMap();
      XComponent assignmentSet = getAssignmentSet();
      for (int j = 0; j < assignmentSet.getChildCount(); j++) {
         XComponent row = (XComponent) assignmentSet.getChild(j);
         String assignmentResource = XValidator.choiceID(row.getStringValue());
         Double available = new Double(getAvailable(row));
         resourceAvailability.put(assignmentResource, available);
      }
      return resourceAvailability;
   }

   private boolean isEffortBasedProject() {
      byte effortBasedByte = EFFORT_BASED;
      Byte calculationSetting = getCalculationMode();
      if (calculationSetting != null) {
         effortBasedByte = calculationSetting.byteValue();
      }
      return (effortBasedByte == EFFORT_BASED);
   }


   protected void preCheckSetStartValue(XComponent dataRow, Object value) {
      if (isProjectMandatory(dataRow) && ((getStart(dataRow) != null && value == null) ||
           (getStart(dataRow) == null && value != null))) {
         throw new XValidationException(MANDATORY_EXCEPTION);
      }
      if ((getStart(dataRow) != null && value == null) || (getStart(dataRow) == null && value != null)) {
         //to be changed into task/from task
         if (dataRow.getOutlineLevel() != 0) {
            XComponent parent = superActivity(dataRow);
            if (getChildren(parent).size() != 1) {
               throw new XValidationException(SCHEDULED_MIXED_EXCEPTION);
            }
         }
      }
   }

   protected double preCheckSetEffortValue(XComponent dataRow, double base_effort, boolean effortBased) {
      if (effortBased) {
         if (isProjectMandatory(dataRow)) {
            if ((OpGanttValidator.getType(dataRow) == MILESTONE && base_effort > 0) ||
                 (OpGanttValidator.getType(dataRow) != MILESTONE && base_effort <= 0)) {
               throw new XValidationException(MANDATORY_EXCEPTION);
            }
         }
   
         if ((OpGanttValidator.getType(dataRow) != MILESTONE && base_effort <= 0)) {
            //activity that will become milestone
            if (subTasks(dataRow).size() != 0) {
               throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
            }
            checkDeletedAssignmentsForWorkslips(dataRow, new ArrayList());
         }
         else if ((OpGanttValidator.getType(dataRow) == MILESTONE && base_effort > 0)) {
            //activity that will become milestone
            if (subTasks(dataRow).size() != 0) {
               throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
            }
            checkDeletedAssignmentsForWorkslips(dataRow, new ArrayList());
         }
      }
      return base_effort > ACTIVITY_MAX_EFFORT ? ACTIVITY_MAX_EFFORT: base_effort;
   }

   protected double preCheckSetDurationValue(XComponent dataRow, double duration) {
      if (isProjectMandatory(dataRow) &&
           ((OpGanttValidator.getType(dataRow) == MILESTONE && duration > 0) ||
                (OpGanttValidator.getType(dataRow) != MILESTONE && duration <= 0))) {
         throw new XValidationException(MANDATORY_EXCEPTION);
      }
      if ((OpGanttValidator.getType(dataRow) != MILESTONE && duration <= 0)) {
         //activity that will become milestone
         if (subTasks(dataRow).size() != 0) {
            throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
         }
         checkDeletedAssignmentsForWorkslips(dataRow, new ArrayList());
      }
      else if ((OpGanttValidator.getType(dataRow) == MILESTONE && duration > 0)) {
         //activity that will become milestone
         if (subTasks(dataRow).size() != 0) {
            throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
         }
         checkDeletedAssignmentsForWorkslips(dataRow, new ArrayList());
      }
      return duration > ACTIVITY_MAX_DURATION ? ACTIVITY_MAX_DURATION : duration;
   }

   protected void preCheckSetEndValue(XComponent dataRow, Object value) {

      if (isProjectMandatory(dataRow)) {
         if ((getEnd(dataRow) != null && value == null) ||
              (getEnd(dataRow) == null && value != null)) {
            throw new XValidationException(MANDATORY_EXCEPTION);
         }
         if (value != null) {
            if ((OpGanttValidator.getType(dataRow) == MILESTONE) &&
                 (((Date) value).getTime() > getStart(dataRow).getTime())) {
               throw new XValidationException(MANDATORY_EXCEPTION);
            }
            if ((OpGanttValidator.getType(dataRow) != MILESTONE) &&
                 (((Date) value).getTime() < getStart(dataRow).getTime())) {
               throw new XValidationException(MANDATORY_EXCEPTION);
            }
         }
      }

      if (getStart(dataRow) != null && (OpGanttValidator.getType(dataRow) != MILESTONE)) {
         if (value == null || (((Date) value).getTime() < getStart(dataRow).getTime())) {
            checkDeletedAssignmentsForWorkslips(dataRow, new ArrayList());
         }
      }

      if ((OpGanttValidator.getType(dataRow) != MILESTONE && (value != null
            && getStart(dataRow) != null && ((Date) value).getTime() < getStart(
            dataRow).getTime()))) {
         //activity that will become milestone
         if (subTasks(dataRow).size() != 0) {
            throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
         }
      }
      else if ((OpGanttValidator.getType(dataRow) == MILESTONE && (value != null
            && getStart(dataRow) != null && ((Date) value).getTime() >= getStart(
            dataRow).getTime()))) {
         //milestone that will become activity  
         if (subTasks(dataRow).size() != 0) {
            throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
         }
         checkDeletedAssignmentsForWorkslips(dataRow, new ArrayList());
      }

      if ((getEnd(dataRow) != null && value == null) || (getEnd(dataRow) == null && value != null)) {
         //to be changed into task/from task
         if (dataRow.getOutlineLevel() != 0) {
            XComponent parent = superActivity(dataRow);
            if (getChildren(parent).size() != 1) {
               throw new XValidationException(SCHEDULED_MIXED_EXCEPTION);
            }
         }
      }

   }

   private List setupResources(double duration, double effort,
         List resources, int assignmentAdjustmentRule, boolean addAnonymousResource) {
      OpProjectCalendar pCal = getCalendar();
      double durationDays = getDurationDays(duration, pCal);
      return setupResources(durationDays, effort, resources, assignmentAdjustmentRule, addAnonymousResource, false, pCal, getResourceCalendarMap(), getAssignmentSet());
   }
   /**
    * adjust assignments according to parameters given
    * Several different cases to handle:
    * - boundary conditions: effort or duration are zero
    * - assignments become to large (more than resource available)
    * - 
    * 
    * @param durationDays
    * @param effort
    * @param resources
    * @param adjustAssignments
    * @param addAnonymousResource
    * @return
    */
   public List setupResources(double duration, double effort,
         List resources, int assignmentAdjustmentRule, boolean addAnonymousResource, boolean userInput) {
      OpProjectCalendar pCal = getCalendar();
      double durationDays = getDurationDays(duration, pCal);
      return setupResources(durationDays, effort, resources, assignmentAdjustmentRule,
            addAnonymousResource, userInput, pCal,
            getResourceCalendarMap(), getAssignmentSet());
   }
   
   public final static int ASSIGNMENT_ADJUSTMENT_LOWER_EFFORT_ONLY = 1;
   public final static int ASSIGNMENT_ADJUSTMENT_IF_ALL_ASSIGNED = 2;
   public final static int ASSIGNMENT_ADJUSTMENT_MAX_FILL = 3;
   public final static int ASSIGNMENT_ADJUSTMENT_LOWER_AVAILABLE_ONLY = 4;

   public int getResourceAssignmentRule(XComponent dataRow) {
      return getResourceAssignmentRule(getType(dataRow), isEffortBasedProject());
   }
   
   public static int getResourceAssignmentRule(byte rowType, boolean effortBased) {
      if (isOfType(rowType, TASK_TYPES)) {
         return ASSIGNMENT_ADJUSTMENT_LOWER_EFFORT_ONLY;
      }
      return effortBased ? ASSIGNMENT_ADJUSTMENT_LOWER_AVAILABLE_ONLY
            : ASSIGNMENT_ADJUSTMENT_LOWER_EFFORT_ONLY;
   }
   
   public static List setupResources(double durationDays, double effort,
         List resources, int assignmentAdjustmentRule,
         boolean addAnonymousResource, boolean userInput,
         OpProjectCalendar pCal, Map resCals, XComponent assignmentSet) {
      if (resources == null) {
         resources = new ArrayList();
      }

      Map assignmentMap = new HashMap();
      Map hoursAvailMap = new HashMap();
      Map nameMap = new HashMap();
      
      double assignedEffort = 0d;
      double assignableEffortPerDay = 0d;
      double unassignedEffort = 0d;
      Iterator rit = resources.iterator();
      while (rit.hasNext()) {
         String assignment = (String) rit.next();
         String resourceId = choiceID(assignment);
         String resourceName = getResourceName(choiceCaption(assignment), null);
         
         OpProjectCalendar resCal = getCalendar(resourceId, OpProjectCalendar.getDefaultProjectCalendar(), pCal, resCals);
         double percentAvailable = getResourceAvailability(resourceId, assignmentSet) / 100;
         double hoursAvailable = resCal.getWorkHoursPerDay() * percentAvailable;
         
         hoursAvailMap.put(resourceId, new Double(hoursAvailable));

         double hoursAssigned = userInput ? localizedHoursAssigned(assignment, pCal) : hoursAssigned(assignment);
         double percentAssigned = 0d;
         if (hoursAssigned == INVALID_ASSIGNMENT) {
            percentAssigned = userInput ? localizedPercentageAssigned(assignment, pCal) : percentageAssigned(assignment);
            if (percentAssigned == INVALID_ASSIGNMENT) {
               hoursAssigned = hoursAvailable;
            }
            else {
               percentAssigned = percentAssigned / 100;
               hoursAssigned = resCal.getWorkHoursPerDay() * percentAssigned;
            }
            hoursAssigned = userInput ? hoursAssigned * durationDays : hoursAssigned;
         }
         double dailyHoursAssigned = userInput ? hoursAssigned / durationDays : hoursAssigned;
         // adjust to availability:
         dailyHoursAssigned = dailyHoursAssigned > hoursAvailable ? hoursAvailable : dailyHoursAssigned;

         // adjust overall hours assigned for the duration:
         hoursAssigned = dailyHoursAssigned * durationDays;
         Double existingAssigned = (Double) assignmentMap.put(resourceId, new Double(dailyHoursAssigned));
         if (existingAssigned != null) {
            // use the sum, if already existing...
            assignmentMap.put(resourceId, new Double(dailyHoursAssigned + existingAssigned.doubleValue()));
         }
         nameMap.put(resourceId, resourceName);
         
         if (!NO_RESOURCE_ID.equals(resourceId)) {
            assignedEffort += hoursAssigned;
            assignableEffortPerDay += hoursAvailable;
         }
         else {
            unassignedEffort += hoursAssigned;
         }
      }
      
      boolean hadUnassignedEffortBefore = greaterThanZeroWithTolerance(unassignedEffort, effort);
      // adjust to actual value:
      unassignedEffort = effort - assignedEffort;
      
      double factor = 1d; // do nothing...
      double base = 0d;
      boolean adjust = false;
      switch (assignmentAdjustmentRule) {
      case ASSIGNMENT_ADJUSTMENT_IF_ALL_ASSIGNED:
         adjust = !hadUnassignedEffortBefore;
         base = effort;
         factor = INVALID_ASSIGNMENT;
         break;
      case ASSIGNMENT_ADJUSTMENT_LOWER_EFFORT_ONLY:
         adjust = false;
         base = effort;
         break;
      case ASSIGNMENT_ADJUSTMENT_MAX_FILL:
         adjust = true;
         base = effort;
         factor = INVALID_ASSIGNMENT;
         break;
      case ASSIGNMENT_ADJUSTMENT_LOWER_AVAILABLE_ONLY:
         adjust = true;
         base = assignedEffort;
         factor = INVALID_ASSIGNMENT;
         break;
      }
      // always adjust, if lower...
      adjust = adjust || lessThanZeroWithTolerance(unassignedEffort, effort);

      if (!isZeroWithTolerance(assignedEffort, effort)) {
         factor = adjust ? (base / assignedEffort) : 1d;
      }
      // build new assignments:
      assignedEffort = 0d;

      // prepare sorted resource set:
      SortedMap orderedResources = new TreeMap(new Comparator() {
         public int compare(Object o1, Object o2) {
            String n1 = (String)o1;
            String n2 = (String)o2;
            int cmp = n1.compareToIgnoreCase(n2);
            if (cmp == 0) {
               cmp = n1.compareTo(n2);
            }
            return cmp;
         }});
      Iterator rmit = assignmentMap.keySet().iterator();
      while (rmit.hasNext()) {
         String resourceId = (String) rmit.next();
         String resourceName = (String) nameMap.get(resourceId);
         
         if (NO_RESOURCE_ID.equals(resourceId)) {
            // this will be re-added later
            continue;
         }
         
         if (durationDays == 0d) {
            orderedResources.put(resourceName, createDailyHoursAssignment(resourceId, resourceName, INVALID_ASSIGNMENT));
         }
         else {
            OpProjectCalendar resCal = getCalendar(resourceId, OpProjectCalendar.getDefaultProjectCalendar(), pCal, resCals);
            double dailyHoursAvailable = ((Double)hoursAvailMap.get(resourceId)).doubleValue();
            double dailyHoursAssigned = ((Double)assignmentMap.get(resourceId)).doubleValue();
            if (factor == INVALID_ASSIGNMENT) {
               double x = effort / assignableEffortPerDay / durationDays;
               dailyHoursAssigned = dailyHoursAvailable * x;
            }
            else {
               dailyHoursAssigned = dailyHoursAssigned * factor;
            }
            dailyHoursAssigned = (dailyHoursAssigned > dailyHoursAvailable) ? dailyHoursAvailable : dailyHoursAssigned;
            double hoursAssigned = durationDays * dailyHoursAssigned;
            assignedEffort += hoursAssigned;

            orderedResources.put(resourceName, createDailyHoursAssignment(resourceId, resourceName, dailyHoursAssigned));
         }
      }
      // copy into result list:
      List newRes = new ArrayList();
      Iterator orit = orderedResources.values().iterator();
      while (orit.hasNext()) {
         newRes.add(orit.next());
      }
      
      // potentially add unassigned stuff...
      unassignedEffort = effort - assignedEffort;
      if (unassignedEffort > ERROR_MARGIN && addAnonymousResource && !newRes.isEmpty()) {
         double dailyHoursUnassigned = unassignedEffort / durationDays;
         newRes.add(createDailyHoursAssignment(NO_RESOURCE_ID, NO_RESOURCE_NAME, dailyHoursUnassigned));
      }
      return newRes;
   }

   private static String createDailyHoursAssignment(String resourceId,
         String resourceName, double dailyHoursAssigned) {
      String resCaption = resourceName + ((dailyHoursAssigned != INVALID_ASSIGNMENT) ? (" " + String.valueOf(new Double(dailyHoursAssigned)) + "h") : "");
           return choice(resourceId, resCaption);
   }
   
   // onyl used to check which way to go. Do not round of anything...
   public final static double TOLERANCE_FACTOR = 1e-10d;
   
   public static boolean isZeroWithTolerance(double zero, double scale) {
      return Math.abs(zero) < ((Math.abs(scale) + 1) * TOLERANCE_FACTOR);
   }
   
   public static boolean lessThanZeroWithTolerance(double value, double scale) {
      return value < -((Math.abs(scale) + 1) * TOLERANCE_FACTOR);
   }
   
   public static boolean greaterThanZeroWithTolerance(double value, double scale) {
      return value > ((Math.abs(scale) + 1) * TOLERANCE_FACTOR);
   }
   
   /**
    * Sets the value of the predecessors field. Also checks for aditional constraints on the predecessors.
    *
    * @param value    list of predecessors (indexes)
    * @param dataRow row to set the predecessors on
    */
   protected void setPredecessorsValue(SortedMap predecessors, XComponent dataRow) {

      // Range check
      checkIndexList(predecessors);
      predecessors = removeTaskIndexes(predecessors);

      // loop detection
      linksLoop(predecessors, new Integer(dataRow.getIndex()), PREDECESSORS_COLUMN_INDEX);

      SortedMap current_predecessors = getPredecessors(dataRow);
      SortedMap added_predecessors = new TreeMap();
      SortedMap removed_predecessors = new TreeMap();
      diffIndexMaps(current_predecessors, predecessors, added_predecessors, removed_predecessors);

      XComponent predecessor;
      SortedMap successors;

      Iterator rit = removed_predecessors.keySet().iterator();
      while (rit.hasNext()) {
         Integer key = (Integer) rit.next();
         predecessor = (XComponent) (data_set
              ._getChild(key.intValue()));
         successors = getSuccessors(predecessor);
         successors.remove(new Integer(dataRow.getIndex()));
      }
      Iterator ait = added_predecessors.keySet().iterator();
      while (ait.hasNext()) {
         Integer key = (Integer) ait.next();
         Object v = added_predecessors.get(key);
         predecessor = (XComponent) (data_set._getChild(key.intValue()));
         successors = getSuccessors(predecessor);
         successors.put(new Integer(dataRow.getIndex()), v);
      }
      setPredecessors(dataRow, predecessors);
   }

   private SortedMap removeTaskIndexes(SortedMap predecessors) {
      for (Iterator iterator = predecessors.keySet().iterator(); iterator.hasNext();) {
         Integer index = (Integer) iterator.next();
         byte type = getType((XComponent) data_set.getChild(index.intValue()));
         if (type == TASK || type == COLLECTION_TASK) {
            iterator.remove();
         }
      }
      return predecessors;
   }

   /**
    * Sets the value of the successors field. Also checks for aditional constraints on the successors.
    *
    * @param value    list of successors (index)
    * @param dataRow row to have the successors value set
    * @return newly added succesors (the diff to old ones)  - index
    */
   protected SortedMap setSuccessorsValue(SortedMap successors, XComponent dataRow) {
      SortedMap predecessors;
      // Range check
      checkIndexList(successors);

      successors = removeTaskIndexes(successors);

      // loop detection
      linksLoop(successors, new Integer(dataRow.getIndex()), SUCCESSORS_COLUMN_INDEX);


      SortedMap current_successors = getSuccessors(dataRow);
      SortedMap added_successors = new TreeMap();
      SortedMap removed_successors = new TreeMap();
      diffIndexMaps(current_successors, successors, added_successors, removed_successors);

      XComponent successor;

      Iterator rit = removed_successors.keySet().iterator();
      while (rit.hasNext()) {
         Integer key = (Integer) rit.next();
         successor = (XComponent) (data_set
              ._getChild(key.intValue()));
         predecessors = getPredecessors(successor);
         predecessors.remove(new Integer(dataRow.getIndex()));
      }
      Iterator ait = added_successors.keySet().iterator();
      while (ait.hasNext()) {
         Integer key = (Integer) ait.next();
         Object v = added_successors.get(key);
         successor = (XComponent) (data_set._getChild(key.intValue()));
         predecessors = getPredecessors(successor);
         predecessors.put(new Integer(dataRow.getIndex()), v);
      }

      setSuccessors(dataRow, successors);
      return removed_successors;
   }

   /**
    * Removes the dupicates from the given list
    *
    * @param map list to be processed
    * @return list without the duplicates
    */
   /**
    * Checks whether any assignments have been removed for resources that have workslips.
    *
    * @param dataRow      a <code>XComponent(DATA_ROW)</code> representing the current activity.
    * @param newResources a <code>List</code> of <code>String</code> representing choice's of resources.
    * @throws XValidationException if any assignments have been removed for resources that have workslips.
    */
   protected void checkDeletedAssignmentsForWorkslips(XComponent dataRow, List newResources)
        throws XValidationException {
      List oldResources = getResources(dataRow);
      List oldResourcesIds = new ArrayList(oldResources.size());
      for (int i = 0; i < oldResources.size(); i++) {
         oldResourcesIds.add(choiceID((String) oldResources.get(i)));
      }

      List newResourcesIds = new ArrayList(newResources.size());
      for (int i = 0; i < newResources.size(); i++) {
         newResourcesIds.add(choiceID((String) newResources.get(i)));
      }

      for (int i = 0; i < newResourcesIds.size(); i++) {
         String newResourceId = (String) newResourcesIds.get(i);
         oldResourcesIds.remove(newResourceId);
      }

      for (int i = 0; i < oldResourcesIds.size(); i++) {
         String oldResourceId = (String) oldResourcesIds.get(i);
         boolean hasWorkRecords = hasWorkRecords(dataRow, oldResourceId);
         if (hasWorkRecords) {
            throw new XValidationException(WORKRECORDS_EXIST_EXCEPTION);
         }
      }
   }

   /**
    * Checks whether there are any work records for the given activity and resource id.
    *
    * @param dataRow    a <code>XComponent(DATA_ROW)</code> representing an activity.
    * @param resourceId a <code>String</code> representing the id of a resource.
    * @return <code>true</code> if there are any workrecords for the given resource and the the given activity.
    */
   private static boolean hasWorkRecords(XComponent dataRow, String resourceId) {
      Map workRecords = getWorkRecords(dataRow);
      Boolean hasWorkRecords = (Boolean) workRecords.get(resourceId);
      if (hasWorkRecords == null) {
         return false;
      }
      else {
         return hasWorkRecords.booleanValue();
      }
   }

   /**
    * Checks an activity for mandatory attribute if the activity is part of a project (not in a template)
    *
    * @param dataRow activity to be checked
    * @return true if activity is mandatory and is part of a project (not a template)
    */
   protected boolean isProjectMandatory(XComponent dataRow) {
      return !getProjectTemplate().booleanValue() && getAttribute(dataRow, MANDATORY);
   }

   /**
    * Returns the <code>resourceId</code> availability from the <code>ASSIGNMENT_SET</code>.
    *
    * @param resourceId <code>String</code> representing the resource id.
    * @return <code>byte</code> availability
    */
   public double getResourceAvailability(String resourceId) {
      XComponent assignmentSet = getAssignmentSet();
      return getResourceAvailability(resourceId, assignmentSet);
   }
   
   public static double getResourceAvailability(String resourceId, XComponent assignmentSet) {
      if (NO_RESOURCE_ID.equals(resourceId)) {
         // the unnamed resource has a default availability of workHoursPerDay
         return NO_RESOURCE_AVAILABILITY;
      }
      XComponent assignmentRow = getAssignmentRow(resourceId, assignmentSet);
      double available = (assignmentRow == null) ? 0 : getAvailable(assignmentRow);
      return available;
   }

   /**
    * Gets a row from the assginment set for the given resource id.
    *
    * @param resourceId a <code>String</code> representing the id of a resource.
    * @return a <code>XComponent(DATA_ROW)</code>.
    */
   public XComponent getAssignmentRow(String resourceId) {
      XComponent assignmentSet = getAssignmentSet();
      return getAssignmentRow(resourceId, assignmentSet);
   }
   
   public static XComponent getAssignmentRow(String resourceId, XComponent assignmentSet) {
      for (int i = 0; i < assignmentSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) assignmentSet.getChild(i);
         if (choiceID(dataRow.getStringValue()).equals(resourceId)) {
            return dataRow;
         }
      }
      return null;
   }

   /**
    * @return The set of assignments used by the validator in the validation process
    */
   public XComponent getAssignmentSet() {
      if (assignmentSet == null) {
         assignmentSet = new XComponent(XComponent.DATA_SET);
         XComponent form = data_set.getForm();
         if (form != null) {
            assignmentSet = createAssignmentSet(form.findComponent(ASSIGNMENT_SET));
         }
      }
      return assignmentSet;
   }

   /**
    * set the assignments to be used by the validator in the validation process
    *
    * @param assignmentSet
    */
   public void setAssignmentSet(XComponent assignmentSet) {
      assignmentSet.addChild(createAnonymousResourceAssignmentDataRow());
      this.assignmentSet = assignmentSet;
   }

   /**
    * Checks for a loop in an activity's links (Predecessors or Successors) for a given startIndex and a set of links.
    * This method gives the possibility to check for a possible set of links without being forced to change the data
    * set.
    *
    * @param links      the array of links that is investigated.
    * @param startIndex the startIndex it is searched for in the links list.
    * @param linkType   the type of link it is checked (PREDECESSORS_COLUMN_INDEX / SUCCESSORS_COLUMN_INDEX)
    * @throws XValidationException if a loop was detected
    */
   public void linksLoop(SortedMap links, Integer startIndex, int linkType) {
      XComponent dataSet = getDataSet();
      XComponent dataRow = (XComponent) (dataSet.getChild(startIndex.intValue()));
      SortedMap linksNow = (SortedMap) ((XComponent) (dataRow.getChild(linkType))).getValue();
      SortedMap linksBefore = new TreeMap();
      mergeMaps(linksBefore, linksNow);

      // set the "to check" links
      ((XComponent) (dataRow.getChild(linkType))).setValue(links);

      // test the data set
      boolean cycles;

      // detect cycles
      cycles = detectLoops();

      // restore initial links
      ((XComponent) (dataRow.getChild(linkType))).setValue(linksBefore);
      if (cycles) {
         //throw validation exception
         throw new OpActivityLoopException(LOOP_EXCEPTION);
      }
   }

   /**
    * Detects loops in a data set.
    *
    * @return true if a loop is found. Returns at first found loop.
    */
   public boolean detectLoops() {
      try {
         OpGraph g = setupValidationGraph();
         getTopologicallyOrderedActivities(g);
         return false;
      }
      catch (IllegalStateException e) {
         // TODO: only illegal State???
         return true;
      }
   }

   
   protected OpGraph setupValidationGraph() {
      //transform data set into graph
      OpGraph graph = new OpGraph();
      //create nodes
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent dataRow = (XComponent) data_set.getChild(i);
         graph.addNode(dataRow);
      }

      //create links
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent dataRow = (XComponent) data_set.getChild(i);
         Entry node = graph.getNode(dataRow);
         //predecessors given by OpGanttValidator is an array of dataRow indexes
         addLinkedActivities(dataRow, graph, true);
      }
      return graph;
   }
   
   protected OpGraph setupHierarchyGraph() {
      OpGraph graph = new OpGraph();
      //create nodes
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent dataRow = (XComponent) data_set.getChild(i);
         graph.addNode(dataRow);
      }

      //create links
      Stack superActivties = new Stack();
      for (int i = 0; i < data_set.getChildCount(); i++) {
         maintainActivityStack(data_set.getDataRow(i), superActivties, graph);
         superActivties.push(data_set.getDataRow(i));
      }
      maintainActivityStack(null, superActivties, graph);
      return graph;
   }
   
   private void maintainActivityStack(XComponent x, Stack superActivityStack, OpGraph g) {
      int outlineLevel = x != null ? x.getOutlineLevel() : 0;
      while (!superActivityStack.isEmpty() && outlineLevel <= ((XComponent)superActivityStack.peek()).getOutlineLevel()) {
         Object sa = superActivityStack.pop();
         if (!superActivityStack.isEmpty()) {
            g.addEdge(g.getNode(superActivityStack.peek()), g.getNode(sa), EDGE_CLASS_HIERACHY);
         }
      }
   }

   protected List getTopologicallyOrderedActivities(OpGraph graph) {
      List ordered = graph.getTopologicOrder();
      return ordered;
   }

   protected void addLinkedActivities(XComponent dataRow, OpGraph g, boolean predecessors) {
      //successors given by OpGanttValidator is an array of dataRow indexes
      // linkedRows.addAll(getSubElements(dataRow, null, false));
      boolean sourceIsImported = importedActivity(dataRow);
      Iterator pit = predecessors ? OpGanttValidator.getPredecessors(dataRow)
            .entrySet().iterator() : OpGanttValidator.getSuccessors(dataRow)
            .entrySet().iterator();
      addDirectlyLinkedActivities(g, dataRow, dataRow, pit);
      // for validation-predecessors, 
      if (predecessors) {
         XComponent superRow = superActivity(dataRow);
         while (superRow != null) {
            Iterator sit = OpGanttValidator.getPredecessors(superRow).entrySet().iterator();
            addDirectlyLinkedActivities(g, superRow, dataRow, sit);
            superRow = superActivity(superRow);
         }
         Iterator cit = getSubElements(dataRow, null, true).iterator();
         Entry dre = g.getNode(dataRow);
         while (cit.hasNext()) {
            Entry le = g.getNode(cit.next());
            g.addEdge(le, dre, EDGE_CLASS_HIERACHY);
         }
      }
      else {
         Entry dre = g.getNode(dataRow);
         Iterator sit = OpGanttValidator.getSuccessors(dataRow).entrySet().iterator();
         while (sit.hasNext()) {
            Map.Entry lre = (Map.Entry) sit.next();
            Integer successorIndex = (Integer) lre.getKey();
            Map desc = (Map) lre.getValue();
            XComponent successor = data_set.getDataRow(successorIndex.intValue());
            if (!importedActivity(successor) || !sourceIsImported) {
               Entry le = g.getNode(successor);
               g.addEdge(le, dre, desc.get(DEP_TYPE));
               List children = getSubElements(successor, null, false);
               Iterator childIterator = children.iterator();
               while (childIterator.hasNext()) {
                  Entry lce = g.getNode(childIterator.next());
                  g.addEdge(lce, dre, desc.get(DEP_TYPE));
               }
            }
         }
      }
   }

   private void addDirectlyLinkedActivities(OpGraph g, XComponent linkSucc, XComponent anchor,
         Iterator linkedIterator) {
      Integer linkSuccKey = new Integer(linkSucc.getIndex());
      Entry anchorEntry = g.getNode(anchor);
      boolean anchorIsImported = getAttribute(anchor, IMPORTED_FROM_SUBPROJECT);
      while (linkedIterator.hasNext()) {
         Map.Entry lre = (Map.Entry) linkedIterator.next();
         Integer linkedRowIndex = (Integer) lre.getKey();
         Map predDesc = (Map) lre.getValue();
         predDesc.remove(DEP_OK);
         predDesc.remove(DEP_CRITICAL);
         XComponent linkedRow = data_set.getDataRow(linkedRowIndex.intValue());
         Map succDesc = (Map) getSuccessors(linkedRow).get(linkSuccKey);
         if (succDesc != null) {
            succDesc.remove(DEP_OK);
            succDesc.remove(DEP_CRITICAL);
         }
         if (!importedActivity(linkedRow) || !anchorIsImported) {
            Entry le = g.getNode(linkedRow);
            g.addEdge(le, anchorEntry, predDesc.get(DEP_TYPE));
         }
      }
   }
   /**
    * Creates a <code>List</code> from the given XArray. The copy is shallow.
    *
    * @param xArray
    * @return a <code>List<code> view.
    */
   public static List toList(List xArray) {
      List result = new ArrayList();
      for (int i = 0; i < xArray.size(); i++) {
         result.add(xArray.get(i));
      }
      return result;
   }

   // *** TODO: addDataRow(), addDataRow(int index,...), removeDataRow(int
   // index)
   // ==> Update predecessor and successor index-references

   /**
    * Updates the <code>DATA_SET</code> component with a new one. It changes only the index of the data rows and the
    * successors/predecessors lists!
    *
    * @param newDataSet an array of <code>XComponent</code> objects.
    */
   protected void updateDataSet(XComponent[] newDataSet) {
      /* data set size */
      int childCount = newDataSet.length;

      Hashtable index_map = new Hashtable();

      for (int index = 0; index < childCount; index++) {
         if (newDataSet[index].getIndex() != index) {
            index_map.put(new Integer(newDataSet[index].getIndex()), new Integer(index));
         }
      }
      /* remove all data_set childrens */
      data_set.removeAllChildren();
      /* update the data set */
      data_set.addAllChildren(newDataSet);

      /* update succesor and predecesors */
      for (int i = 0; i < childCount; i++) {
         setSuccessors(newDataSet[i], updateIndexList(getSuccessors(newDataSet[i]), index_map));
         setPredecessors(newDataSet[i], updateIndexList(getPredecessors(newDataSet[i]), index_map));
      }

   }

   /**
    * Returns the inner activities with the outline level greater with 1 of a <code>COLLECTION_ACTIVITY</code>
    * component.
    *
    * @param collectionActivity the collection activity
    * @return an <code>XArray <XComponent> </code> representing the children of the collection.
    */

   protected List getInnerActivitiesOfCollection(XComponent collectionActivity) {
      List innerActivities = new ArrayList();
      int fromPosition = collectionActivity.getIndex();
      int collectionOulineLevel = collectionActivity.getOutlineLevel();
      // skip next
      fromPosition++;
      for (int index = fromPosition; index < data_set.getChildCount(); index++) {
         XComponent row = (XComponent) data_set.getChild(index);
         if (row.getOutlineLevel() == collectionOulineLevel) {
            break;
         }
         if (row.getOutlineLevel() == collectionOulineLevel + 1) {
            innerActivities.add(row);
         }
      }
      return innerActivities;
   }

   /**
    * Returns for a <code>currentRow</code> the first <code>MILESTONE</code> activity taking into account the
    * <code>direction</code> of navigation.
    *
    * @param currentRow the <code>XComponent</code>
    * @param direction  direction of navigation
    * @return <code>XComponent</code> the first <code>MILESTONE</code> activity <code>null</code> in case that a
    *         <code>MILESTONE</code> activity doesn't exists
    */
   public XComponent findFirstMilestone(XComponent currentRow, int direction) {
      int currentRowIndex = currentRow.getIndex();
      int currentOutlineLevel = currentRow.getOutlineLevel();
      /* navigate upwards */
      if (direction < 0) {
         // skip to previous row
         currentRowIndex--;
         /* navigate upwards in the data set */
         for (int index = currentRowIndex; index >= 0; index--) {
            XComponent row = (XComponent) data_set.getChild(index);
            if ((OpGanttValidator.getType(row) == MILESTONE) && (row.getOutlineLevel() <= currentOutlineLevel)) {
               return row;
            }
         }
      }
      /* navigate downwords */
      if (direction > 0) {
         // skip to next row
         currentRowIndex++;
         /* navigate dowwards in the data set */
         for (int index = currentRowIndex; index < data_set.getChildCount(); index++) {
            XComponent row = (XComponent) data_set.getChild(index);
            if ((OpGanttValidator.getType(row) == MILESTONE) && (row.getOutlineLevel() <= currentOutlineLevel)) {
               return row;
            }
         }
      }
      // in case that a MILESTONE activity doesn't exists
      return null;
   }

   /**
    * Returns for a <code>currentRow</code> the first <code>XComponent</code> activity that has the same
    * <code>outline level</code> with the given activity. The first activity is returned taking into account the
    * <code>direction</code> of navigation.
    *
    * @param startRow     start data row
    * @param outlineLevel outline level to search for
    * @param direction    direction of navigation
    * @return <code>XComponent</code> the first activity with the outline level equal to the given
    *         <code>currentRow</code>
    *         <code>null</code> in case that the currentActivity is first or last activity
    *         or the currentActivity is the only activity with that outline level in a coolection
    */
   public XComponent findFirstActivityWithOutlineLevelEqual(XComponent startRow, int outlineLevel, int direction) {

      int currentRowIndex = startRow.getIndex();
      /* navigate upwards */
      if (direction < 0) {
         // skip to previous row
         currentRowIndex--;
         /* navigate upwards in the data set */
         for (int index = currentRowIndex; index >= 0; index--) {
            XComponent row = (XComponent) data_set.getChild(index);

            if (row.getOutlineLevel() == outlineLevel) {
               return row;
            }
            // if the row has the outline level lower than current activity
            if (row.getOutlineLevel() < outlineLevel) {
               return null;
            }
         }
      }
      /* navigate downwords */
      if (direction > 0) {
         // skip to next row
         currentRowIndex++;
         /* navigate downwards in the data set */
         for (int index = currentRowIndex; index < data_set.getChildCount(); index++) {
            XComponent row = (XComponent) data_set.getChild(index);
            if (row.getOutlineLevel() == outlineLevel) {
               return row;
            }
            // if the row has the outline level lower than current activity
            if (row.getOutlineLevel() < outlineLevel) {
               return null;
            }
         }
      }
      // in case that the currentRow is the first activity of the first collection activity of the data set
      // or if it is the first or the last activity of the data set
      return null;
   }

   /**
    * Returns for a <code>currentRow</code> the first <code>XComponent</code> activity that has the same (or lower)
    * <code>outline level</code> as the given activity. The first activity is returned taking into account the
    * <code>direction</code> of navigation.
    *
    * @param currentRow the <code>XComponent</code>
    * @param direction  direction of navigation
    * @return <code>XComponent</code> the first activity with the outline level equal to the given
    *         <code>currentRow</code>
    *         <code>null</code> in case that the currentActivity is first or last or the
    *         currentActivity is the first activity of the first collection
    */
   public XComponent findFirstActivityWithOutlineLevelEqualOrLower(XComponent currentRow, int direction) {

      int currentRowIndex = currentRow.getIndex();
      int currentRowOulineLevel = currentRow.getOutlineLevel();
      /* navigate upwards */
      if (direction < 0) {
         // skip to previous row
         currentRowIndex--;
         /* navigate upwards in the data set */
         for (int index = currentRowIndex; index >= 0; index--) {
            XComponent row = (XComponent) data_set.getChild(index);
            if (row.getOutlineLevel() <= currentRowOulineLevel) {
               return row;
            }
         }
      }
      /* navigate downwords */
      if (direction > 0) {
         // skip to next row
         currentRowIndex++;
         /* navigate upwards in the data set */
         for (int index = currentRowIndex; index < data_set.getChildCount(); index++) {
            XComponent row = (XComponent) data_set.getChild(index);
            if (row.getOutlineLevel() <= currentRowOulineLevel) {
               return row;
            }
         }
      }
      // in case that the currentRow is the first activity of the first collection activity of the data set
      // or if it is the first or the last activity of the data set
      return null;
   }

   /**
    * Returns the offset used in <code>OpGanttValidator#moveDataRows</code>.
    *
    * @param selectionStart
    * @param selectionEnd
    * @param direction      the direction of moving ( positive downwords,negative upwards)
    * @return the offset
    */
   public int getMovingOffset(XComponent selectionStart, XComponent selectionEnd, int direction) {

      int offset = 0;
      int activityIndex = selectionStart.getIndex();
      XComponent row;
      if (direction < 0) {
         row = findFirstActivityWithOutlineLevelEqual(selectionStart, selectionStart.getOutlineLevel(), direction);
      }
      else {
         row = findFirstActivityWithOutlineLevelEqual(selectionEnd, selectionStart.getOutlineLevel(), direction);
      }
      // the dataRow becomes the child of a new or existing collection
      if (row == null) {
         // if it is the first dataRow in the first collection or last dataRow of the last collection
         if (((activityIndex == 1) || (activityIndex == 0)) && (direction < 0)) {
            // suspend moving
            return offset;
         }
         // if it is the last dataRow of the last collection
         if ((activityIndex == getDataSet().getChildCount() - 1) && (direction > 0)) {
            // suspend moving
            return offset;
         }
         // move one position
         offset = 1;
         /* suppose that the dataRow is not a COLLECTION */
         int childrenSize = 0;
         // it the dataRow is a last collection in the data set
         if (direction > 0 && (OpGanttValidator.getType(selectionStart) == COLLECTION ||
              OpGanttValidator.getType(selectionStart) == OpGanttValidator.SCHEDULED_TASK ||
              OpGanttValidator.getType(selectionStart) == OpGanttValidator.COLLECTION_TASK)) {
            /* calculate it's number of children */
            childrenSize = getChildren(selectionStart).size();
            if (getDataSet().getChildCount() - childrenSize - 1 == activityIndex) {
               offset = 0;
               return offset;
            }
         }
         /* simutate moving and check for valid outline levels . */
         if (direction < 0) {//upwards
            if (selectionStart.getOutlineLevel() - ((XComponent) getDataSet().getChild(activityIndex + direction - 1)).getOutlineLevel() > 1) {
               // suspend moving
               offset = 0;
               return offset;
            }
         }
         else { //downwords
            if (selectionStart.getOutlineLevel() - ((XComponent) getDataSet().getChild(activityIndex + childrenSize + 1)).getOutlineLevel() > 1) {
               // suspend moving
               offset = 0;
               return offset;
            }
         }
      }
      else {
         if (getChildren(row).size() != 0) {
            List children = getChildren(row);
            offset = children.size() + 1;
         }
         else {
            offset = 1;
         }
      }
      // return the offset according to direction
      if (direction > 0) {
         return offset; //down
      }
      else {
         return -offset; //up
      }
   }

   /**
    * Calculates the offset that has to be passed to moveRows for 2 given indexes from the data set. The desired effect
    * is to move source BEFORE target.
    *
    * @param sourceDataRow
    * @param targetDataRow
    * @param moveAfterTarget
    * @return moving offset in order to move source before target.
    */
   public int getMovingOffsetForComponents(XComponent sourceDataRow, XComponent targetDataRow, boolean moveAfterTarget) {
      int offset = 0;
      // source "before" target -> move "down".
      int sourceIndex = sourceDataRow.getIndex();
      int targetIndex = targetDataRow.getIndex();
      if (sourceIndex < targetIndex) {
         XComponent nextRow = findFirstActivityWithOutlineLevelEqualOrLower((XComponent) data_set
              .getChild(sourceIndex), 1);
         if (nextRow == null) {
            offset = 1;
         }
         else {
            int nextIndex = nextRow.getIndex();
            while (nextIndex < targetIndex) {
               offset++;
               nextIndex++;
            }
         }
      }
      // source "after" target -> move "up".
      else {
         offset = targetIndex - sourceIndex;
      }

      if (sourceDataRow.getOutlineLevel() == targetDataRow.getOutlineLevel()) {
         // the source must be moved after the target
         if (moveAfterTarget) {
            List targetChildren = getChildren(targetDataRow);
            offset = offset + 1 + targetChildren.size();
         }
      }
      else {
         offset++;
      }

      return offset;
   }

   /**
    * Returns a <code>XArray</code> of components representing ALL the children of a <code>COLLECTION</code> activity
    *
    * @param collectionActivity the <code>COLLECTION_ACTIVITY</code> component
    * @return a <code>XArray</code> of children
    */
   public List getChildren(XComponent collectionActivity) {
      // children array
      List children = new ArrayList();
      int rowIndex = collectionActivity.getIndex();
      // navigate from rowIndex + 1
      for (int index = rowIndex + 1; index < data_set.getChildCount(); index++) {
         XComponent row = (XComponent) data_set.getChild(index);

         if (row.getOutlineLevel() > collectionActivity.getOutlineLevel()) {
            children.add(row);
         }
         // if the activity has the same outline level don't navigate anymore
         if (row.getOutlineLevel() == collectionActivity.getOutlineLevel()) {
            break;
         }
      }
      return children;
   }

   /**
    * Returns a <code>XArray</code> of <code>COLLECTION</code> activities for the <code>activity</code>.
    * --Collection 1 ------Collection 2 ----------Activity 1 ------Collection 3 ----------Activity 2 ----------Activity
    * 4. If <code>activity<code> is Activity 4 the array containts Collection 3 and Collection 1.
    *
    * @param activity a <code> XComponent </code> representing the activity for which the collection array is returned
    * @return a <code>XArray</code> of <code>COLLECTION</code> activities
    */
   public List getCollectionsForActivity(XComponent activity) {
      List collectionArray = new ArrayList();
      int activityIndex = activity.getIndex();
      XComponent row;
      List rowChildren;
      // the data set
      XComponent dataSet = getDataSet();

      // navigate upwards;
      activityIndex--;
      for (int index = activityIndex; index >= 0; index--) {
         row = (XComponent) dataSet.getChild(index);
         rowChildren = getChildren(row);
         // if the activity is a inner activity of the collection (rowChildren.size() != 0)
         if (rowChildren.contains(activity)) {
            collectionArray.add(row);
         }

      }
      return collectionArray;
   }

   /**
    * Expands after moving each <code>STANDARD</code> activity that turns into a <code>COLLECTION</code> activity or
    * an existing <code>COLLECTION</code> activity.
    *
    * @param movedDataRows a <code>XArray</code> of moved data rows.
    */
   public void expandCollectionsAfterMove(List movedDataRows) {
      /* navigate the moved data rows array */
      for (int index = 0; index < movedDataRows.size(); index++) {
         // get a row
         XComponent row = (XComponent) movedDataRows.get(index);
         // find it's activity parents
         List collectionParents = getCollectionsForActivity(row);
         for (int i = 0; i < collectionParents.size(); i++) {
            XComponent collectionActivity = (XComponent) collectionParents.get(i);
            collectionActivity.expanded(true, false);
         }
      }
   }

   /**
    * @throws XValidationException
    * @see XValidator#moveDataRows(java.util.List,int)
    */
   public void moveDataRows(List dataRows, int offset)
        throws XValidationException {

      //NOTE: author="Mihai Costin" description="aditional performance gain possible by selecting only start points that changed collection"
      for (int i = 0; i < dataRows.size(); i++) {
         XComponent row = (XComponent) dataRows.get(i);
         addValidationStartPoint(row);
         XComponent collection = superActivity(row);
         if (collection != null) {
            addValidationStartPoint(collection);
         }
      }

      moveRows(dataRows, offset);
      updateActivityTypes(false);
      validateDataSet();
   }

   
   /**
    * find hierarchical root elements in a given list of datarows
    * @param dataRows
    * @return
    */
   private static List findRootRows(List dataRows) {
      int currentLevel = -1;
      int initialLevel = -1;
      int rowIndex = -1;
      
      List initialRows = new ArrayList();
      Iterator it = dataRows.iterator();
      while (it.hasNext()) {
         XComponent row = (XComponent) it.next();
         boolean isInitial = false;
         int idx = row.getIndex();
         if (idx != rowIndex) {
            isInitial = true;
         }
         else {
            currentLevel = row.getOutlineLevel();
            if (currentLevel <= initialLevel) {
               isInitial = true;
            }
         }
         if (isInitial) {
            initialRows.add(row);
            initialLevel = row.getOutlineLevel();
            rowIndex = idx;
         }
         rowIndex++;
      }
      return initialRows;
   }
   
   protected void moveRows(List dataRows, int offset) {
      int dataRowsSize = dataRows.size() - 1;

      int endRowIndex = ((XComponent) (dataRows.get(dataRowsSize))).getIndex();
      int startRowIndex = endRowIndex;
      List initalValues = new ArrayList();

      // startRowIndex should be the min of the indexes from dataRows (= the first selected row)
      // endRowIndex -maximum (the last selected row)
      int rowIndex;
      for (int i = 0; i < dataRows.size(); i++) {
         rowIndex = ((XComponent) (dataRows.get(i))).getIndex();
         if (startRowIndex > rowIndex) {
            startRowIndex = rowIndex;
         }
         if (endRowIndex < rowIndex) {
            endRowIndex = rowIndex;
         }
      }

      if (mandatoryCollectionCheck(dataRows)) {
         throw new XValidationException(MANDATORY_EXCEPTION);
      }

      // Check boundaries and offset
      int child_count = data_set.getChildCount();
      if ((offset > 0) && (endRowIndex + offset >= child_count)) {
         offset -= (child_count + 1 - endRowIndex - offset);
      }
      if ((offset < 0) && (startRowIndex + offset < 0)) {
         offset -= startRowIndex + offset;
      }
      if (offset == 0) {
         return;
      }

      XComponent[] children = new XComponent[child_count];
      for (int i = 0; i < child_count; i++) {
         XComponent child = (XComponent) (data_set.getChild(i));
         children[i] = child;
         // save the initial position, succ and pred list for each row
         List savedValues = new ArrayList();
         // ref to the data row
         savedValues.add(child);
         // successors
         SortedMap successors = new TreeMap();
         mergeMaps (successors, getSuccessors(child));
         savedValues.add(successors);

         // predecessors
         SortedMap predecessors = new TreeMap();
         mergeMaps(predecessors, getPredecessors(child));
         savedValues.add(predecessors);
         // save values
         initalValues.add(savedValues);
      }

      if (!preCheckProgramMoveConstraints(dataRows, offset)) {
         rollbackMove(initalValues);
         throw new XValidationException(PROGRAM_ELEMENT_MOVE_EXCEPTION);
      }
      changeSubProjectOutlineForMove(dataRows, offset);
      
      // Perform consecutive move - iterate as many times as offset is
      for (int k = 0; k < Math.abs(offset); k++) {
         // for each data row make a step
         children = moveRowsByOne(offset, children, dataRows);
         updateDataSet(children);
      }

      //check if the move was ok  (mandatory task with children != tasks/collection tasks)
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent row = (XComponent) data_set.getChild(i);
         List subActivities = subActivities(row);
         List subTasks = subTasks(row);
         if ((getType(row) == TASK || getType(row) == COLLECTION_TASK) &&
              isProjectMandatory(row) && subActivities.size() != 0) {
            rollbackMove(initalValues);
            throw new OpActivityLoopException(MANDATORY_EXCEPTION);
         }
         if ((getType(row) == MILESTONE) && (subActivities.size() != 0 || subTasks.size() != 0)) {
            rollbackMove(initalValues);
            throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
         }
         if ((getType(row) == SCHEDULED_TASK) && (subActivities.size() != 0)) {
            rollbackMove(initalValues);
            throw new XValidationException(SCHEDULED_MIXED_EXCEPTION);
         }
         if (subActivities.size() != 0 && subTasks.size() != 0) {
            rollbackMove(initalValues);
            throw new XValidationException(SCHEDULED_MIXED_EXCEPTION);
         }
         //collections with work records (collection tasks/ scheduled activities/ standard collections)
         if (subActivities.size() != 0 || subTasks.size() != 0) {
            try {
               checkDeletedAssignmentsForWorkslips(row, new ArrayList());
            }
            catch (XValidationException e) {
               rollbackMove(initalValues);
               throw e;
            }
         }
      }
      // check for loops and if everything ok "commit"
      if (detectLoops()) {
         // rollback
         rollbackMove(initalValues);
         throw new OpActivityLoopException(LOOP_EXCEPTION);
      }
      else {
         expandCollectionsAfterMove(dataRows);
      }
   }

   private boolean preCheckProgramMoveConstraints(List dataRows, int offset) {
      List rootRows = findRootRows(dataRows);
      Iterator rit = rootRows.iterator();
      
      while (rit.hasNext()) {
         XComponent root = (XComponent) rit.next();
         int previousIndex = root.getIndex() + offset - (offset < 0 ? 1 : 0);
         int nextIndex = previousIndex + 1;
         XComponent previous = null;
         XComponent next = null;
         if (previousIndex >= 0) {
            if (previousIndex >= data_set.getChildCount() && data_set.getChildCount() > 0) {
               previousIndex = data_set.getChildCount() - 1;
            }
            previous = getDataSet().getDataRow(previousIndex);
         }
         if (nextIndex < data_set.getChildCount()) {
            next = getDataSet().getDataRow(nextIndex);
         }
         // only the root of an imported hierarchy can be moved:
         if (importedActivity(root) && !importedHeadRow(root)) {
            return false;
         }
         if (previous != null && importedActivity(previous)) {
            if (next != null && importedActivity(next) && !importedHeadRow(next)) {
               return false;
            }
         }
      }
      return true;
   }

   private void changeSubProjectOutlineForMove(List dataRows, int offset) {
      List rootRows = findRootRows(dataRows);
      Iterator rit = rootRows.iterator();
      
      while (rit.hasNext()) {
         XComponent root = (XComponent) rit.next();
         int previousIndex = root.getIndex() + offset - (offset < 0 ? 1 : 0);
         int nextIndex = previousIndex + 1;
         XComponent previous = null;
         XComponent next = null;
         if (previousIndex >= 0) {
            if (previousIndex >= data_set.getChildCount() && data_set.getChildCount() > 0) {
               previousIndex = data_set.getChildCount() - 1;
            }
            previous = getDataSet().getDataRow(previousIndex);
         }
         if (nextIndex < data_set.getChildCount()) {
            next = getDataSet().getDataRow(nextIndex);
         }
         if (previous != null) {
            int outlineLevelOffset =  (next != null ? next.getOutlineLevel() : 0) - root.getOutlineLevel();
            changeOutlineOfSubHierarchy(root, outlineLevelOffset);
         }
      }
   }
   
   private void changeOutlineOfSubHierarchy(XComponent root, int offset) {
      List subElements = getSubElements(root, null, false);
      root.setOutlineLevel(root.getOutlineLevel() + offset);
      Iterator i = subElements.iterator();
      while (i.hasNext()) {
         XComponent row = (XComponent) i.next();
         row.setOutlineLevel(row.getOutlineLevel() + offset);
      }
   }
   
   
   public static SortedMap mergeMaps(SortedMap mergeResult, SortedMap source) {
      if (source != null) {
         Iterator mit = source.keySet().iterator();
         while (mit.hasNext()) {
            Object key = mit.next();
            mergeResult.put(key, source.get(key));
         }
      }
      return mergeResult;
   }
   
   /**
    * Checks if any of the selected rows is a milestone inside a mandatory collection and is the only child
    *
    * @param dataRows rows to be checked.
    * @return true if such a milestone is found
    */
   private boolean mandatoryCollectionCheck(List dataRows) {
      for (int i = 0; i < dataRows.size(); i++) {
         XComponent row = (XComponent) dataRows.get(i);
         XComponent prevRow = null;
         XComponent nextRow = null;
         if (row.getIndex() > 0) {
            prevRow = (XComponent) data_set.getChild(row.getIndex() - 1);
         }
         if (row.getIndex() < data_set.getChildCount() - 1) {
            nextRow = (XComponent) data_set.getChild(row.getIndex() + 1);
         }

         if (getType(row) == MILESTONE && prevRow != null && prevRow.getOutlineLevel() == row.getOutlineLevel() - 1
              && isProjectMandatory(prevRow) &&
              (nextRow == null || nextRow.getOutlineLevel() != row.getOutlineLevel())) {
            return true;
         }
      }
      return false;
   }

   /**
    * Restores the initial state of the data set given the array of values prior to the move action
    *
    * @param initalValues
    */
   private void rollbackMove(List initalValues) {
      // get the children from the initialvalues List
      data_set.removeAllChildren();
      for (int i = 0; i < initalValues.size(); i++) {
         List values = (List) initalValues.get(i);
         XComponent child = (XComponent) values.get(0);
         SortedMap successors = (SortedMap) values.get(1);
         SortedMap predecessors = (SortedMap) values.get(2);
         setSuccessors(child, successors);
         setPredecessors(child, predecessors);
         data_set.addChild(child);
      }
   }

   /**
    * Moves a set of given rows by one . Dirrectin is given by offset's sign. It will only change the possition of the
    * rows in the children array. No validation or succesors/predecessors lists will be made.
    *
    * @param offset    -
    *                  moving offset.
    * @param children  -
    *                  array of <code>XComponents</code> - all the data rows in the data set.
    * @param movedRows -
    *                  <code>XArray</code> of <code>XComponents</code> - the moved data rows.
    * @return a buffer with the new structure of the data set after move.
    */
   private XComponent[] moveRowsByOne(int offset, XComponent[] children, List movedRows) {
      // compute start point, end point, and increment for move
      int startIndex = 0, endIndex = 0, increment = 0, index = 0;
      if (offset < 0) {
         startIndex = 0;
         endIndex = movedRows.size();
         increment = 1;
      }
      else {
         startIndex = movedRows.size() - 1;
         endIndex = -1;
         increment = -1;
      }

      /* create a buffer */
      XComponent[] buffer = new XComponent[children.length];
      /* move all the children to the buffer */
      System.arraycopy(children, 0, buffer, 0, children.length);

      index = startIndex;
      while (index != endIndex) {

         XComponent row = (XComponent) movedRows.get(index);
         int rowIndex = row.getIndex();

         // not the first row up or the last one down.
         if (((offset < 0) && (rowIndex > 0)) || ((offset > 0) && (rowIndex < children.length - 1))) {

            // move inside a collection or natural
            if (offset < 0) {
               System.arraycopy(buffer, 0, buffer, 0, rowIndex - 1);
               buffer[rowIndex - 1] = row;
               buffer[rowIndex] = children[rowIndex - 1];
            }
            else {
               System.arraycopy(buffer, 0, buffer, 0, rowIndex);
               buffer[rowIndex] = children[rowIndex + 1];
               buffer[rowIndex + 1] = row;
            }
            System.arraycopy(buffer, rowIndex + 1, buffer, rowIndex + 1, children.length - (rowIndex + 1));

            // move all the buffer to children
            System.arraycopy(buffer, 0, children, 0, buffer.length);
         }

         index += increment;
      }

      return children;
   }


   /**
    * Checks if the next activity according to the <code>direction</code> of navigation is <code>MILESTONE</code>.
    *
    * @param activity  a <code>XComponent</code> representing a activity
    * @param direction a <code>int</code> representing direction [ -1 ]- upwords ; [ 1 ] downwords.
    * @return boolean
    */
   protected boolean isNextActivityMilestone(XComponent activity, int direction) {

      boolean isMilestone = false;
      // the index of the activity;
      int activityIndex = activity.getIndex();
      // the data set
      XComponent dataSet = getDataSet();
      // upwards
      if ((activityIndex > 0) && (direction < 0)) {
         activityIndex--;
         if (OpGanttValidator.getType((XComponent) dataSet.getChild(activityIndex)) == MILESTONE) {
            isMilestone = true;
         }
      }
      // downwords
      if ((activityIndex < dataSet.getChildCount() - 1) && (direction > 0)) {
         activityIndex++;
         if (OpGanttValidator.getType((XComponent) dataSet.getChild(activityIndex)) == MILESTONE) {
            isMilestone = true;
         }
      }

      return isMilestone;
   }

   /**
    * Changes the outline level for an array of data rows, creating (or destroying) in effect a child-parent
    * relationship. Change the outline Level for each activity of the array.
    *
    * @param dataRows an <code>XArray</code> of activities whose outline levels must be changed
    * @param offset    the offset (positive or negative value)
    * @throws XValidationException if a cycle was detected and the outline level can't be changed.
    */
   public void changeOutlineLevels(List dataRows, int offset)
        throws XValidationException {

      for (int i = 0; i < dataRows.size(); i++) {
         XComponent row = (XComponent) dataRows.get(i);
         addValidationStartPoint(row);
         XComponent collection = superActivity(row);
         if (collection != null) {
            addValidationStartPoint(collection);
         }
      }
      
      if (offset == 0) {
         return;
      }
      if (dataRows.size() == 0) {
         return;
      }

      XComponent row;
      int outline_level;

      List initialOutlineLevels = new ArrayList();
      addToUndo();
      try {
         for (int i = 0; i < dataRows.size(); i++) {
            row = (XComponent) (dataRows.get(i));
            initialOutlineLevels.add(new Integer(row.getOutlineLevel()));
   
            // IF the outline change is possible...
            canChangeOutline(dataRows, i, offset);
   
            // update the outline level value for the current row
            outline_level = row.getOutlineLevel() + offset;
   
            // set up the row outline Level
            row.setOutlineLevel(outline_level);
   
         }
   
         // loop detection
         if (detectLoops()) {
            throw new OpActivityLoopException(LOOP_EXCEPTION);
         }
   
         //<FIXME> author="Mihai Costin" description="Performance for this check could be improoved"
         for (int i = 0; i < dataRows.size(); i++) {
            row = (XComponent) (dataRows.get(i));
            //scheduled tasks can have only sub tasks
            XComponent parent = superActivity(row);
            if (parent != null) {
               List parentTasks = subTasks(parent);
               List parentActivities = subActivities(parent);
               if (parentTasks.size() != 0 && parentActivities.size() != 0) {
                  throw new OpActivityLoopException(SCHEDULED_MIXED_EXCEPTION);
               }
               if (parentTasks.size() != 0 || parentActivities.size() != 0) {
                  //if the previous activity has assignments
                  checkDeletedAssignmentsForWorkslips(parent, new ArrayList());
               }
            }

            List subRows = subTasks(row);
            List subActivities = subActivities(row);
            if (subRows.size() != 0 && subActivities.size() != 0) {
               throw new OpActivityLoopException(SCHEDULED_MIXED_EXCEPTION);
            }
            //collections with resources on them
            if (subRows.size() != 0 || subActivities.size() != 0) {
               //if the previous activity has assignments
               checkDeletedAssignmentsForWorkslips(row, new ArrayList());
            }

         }
         //<FIXME>
      }
      catch (XValidationException e) {
         for (int i = 0; i < initialOutlineLevels.size(); i++) {
            row = (XComponent) (dataRows.get(i));
            int initial = ((Integer) initialOutlineLevels.get(i)).intValue();
            row.setOutlineLevel(initial);
         }
         throw e;
      }
      validateDataSet();
   }

   /**
    * Determines if the outline level can be changed for a datarow. It is assumed that in a data set the outlinelevel is
    * changed in order for the selected group of data rows (from low index -> big index)
    *
    * @param dataRows        <code>XArray</code> of <code>XComponent</code>, array with the data rows that will have the outline
    *                        level changed at this step.
    * @param indexInSelected position for the data row that will be changed
    * @param offset          increment for the current offset
    * @return true if the outline level can be changed / false otherwise
    */
   private boolean canChangeOutline(List dataRows, int indexInSelected, int offset) {

      XComponent changedDataRow = (XComponent) dataRows.get(indexInSelected);
      int indexInDataSet = changedDataRow.getIndex();
      int outlineLevel = changedDataRow.getOutlineLevel();
      int newOutline = outlineLevel + offset;

      // outline level can't be < 0
      if (newOutline < 0) {
         throw new XValidationException(OUTLINE_LEVEL_INVALID_EXCEPTION);
      }

      // the first row can't have its outline level changed
      if (indexInDataSet == 0) {
         throw new XValidationException(CANNOT_MOVE_ROOT_ACTIVITY_EXCEPTION);
      }

      // get previous row
      XComponent previousRow = (XComponent) data_set.getChild(indexInDataSet - 1);
      int previousLevel = previousRow.getOutlineLevel();

      // if previous row is milestone, offset is +, and the milestone's outline level is != newOutline
      if ((getType(previousRow) == MILESTONE) && (offset > 0) && (previousLevel == newOutline - 1)) {
         throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
      }

      // if offset is + and diff from prev is > 1
      if ((offset > 0) && (newOutline - previousLevel > 1)) {
         throw new XValidationException(OUTLINE_LEVEL_INVALID_EXCEPTION);
      }

      // if changed row is a milestone, offset -, current outline level of milestone == outline level of next row
      // and the next row will not have it's outline level changed
      if (indexInDataSet + 1 != data_set.getChildCount()) {
         XComponent nextRow = (XComponent) data_set.getChild(indexInDataSet + 1);
         if ((outlineLevel == nextRow.getOutlineLevel()) && (getType(changedDataRow) == MILESTONE) && (offset < 0)
              && (!dataRows.contains(nextRow))) {
            throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
         }
      }

      //if the collection is mandatory, offset is +, activity is milestone, 1 activity in moved rows
      if (offset < 0 && previousRow.getOutlineLevel() == changedDataRow.getOutlineLevel() - 1) {
         if (isProjectMandatory(previousRow) && getType(changedDataRow) == MILESTONE) {
            throw new XValidationException(MANDATORY_EXCEPTION);
         }
      }
      
      // only move subprojects as a whole:
      XComponent previousInSelected = indexInSelected > 0 ? (XComponent) dataRows.get(indexInSelected - 1) : null;
      if (previousInSelected == null || previousInSelected.getIndex() != previousRow.getIndex()) {
         if ((importedActivity(changedDataRow) && !importedHeadRow(changedDataRow)) || importedActivity(previousRow)) {
            throw new XValidationException(PROGRAM_ELEMENT_MOVE_EXCEPTION);
         }
      }

      //not the first row
      if (offset > 0) {
         XComponent futureParent = null;
         int index = indexInDataSet;
         while (index > 0) {
            index--;
            XComponent row = (XComponent) data_set.getChild(index);
            if (row.getOutlineLevel() == changedDataRow.getOutlineLevel()) {
               futureParent = row;
               break;
            }
         }
         if (futureParent != null) {
            previousRow = futureParent;
            //if the previous is a mandatory task
            if (previousRow.getOutlineLevel() == changedDataRow.getOutlineLevel()) {
               if (isProjectMandatory(previousRow) && (getType(previousRow) == TASK || getType(previousRow) == COLLECTION_TASK) &&
                    (getType(changedDataRow) != TASK && getType(changedDataRow) != COLLECTION_TASK)) {
                  throw new XValidationException(MANDATORY_EXCEPTION);
               }
            }
         }
      }
      return true;
   }

   /**
    * Updates the outline level of the inner activities of a collection according to increment Method is used when a
    * collection activity becomes inner activity of another collection.
    *
    * @param collectionActivityRow the collection
    * @param increment             the increment (positive or negative value)
    */
   protected void updateInnerActivityOulineLevel(XComponent collectionActivityRow, int increment) {
      /* data set which contains all activities */
      XComponent dataSet = getDataSet();
      int collectionActivityOultineLevel = collectionActivityRow.getOutlineLevel();
      // for the inner activities update the outline level according to increment

      for (int rowIndex = collectionActivityRow.getIndex() + 1; rowIndex < dataSet.getChildCount(); rowIndex++) {
         XComponent innerActivityRow = (XComponent) dataSet.getChild(rowIndex);
         /* get the inner activity of the collection */
         int innerOutlineLevel = innerActivityRow.getOutlineLevel();
         /* check if they it is a inner activity of the collection */
         if ((collectionActivityOultineLevel <= innerOutlineLevel) && (innerOutlineLevel != 0)) {
            // update the inner activity outline
            innerActivityRow.setOutlineLevel(innerOutlineLevel + increment);
         }
      }
   }

   /**
    * Sets up the activity succesors of the current row. The array of succesors is searched inside the data set in this
    * way: If a row has a predessesor the current row index ,then the row index is added in the current row succesors
    * array.
    *
    * @param currentRow the <code>OpProjectComponent.STANDARD_ACTIVITY</code> activity row
    */
   protected void setActivityRowSuccesors(XComponent currentRow) {
      /* data set which contains all activities */
      XComponent dataSet = getDataSet();
      /* check if it is the last row */
      if (currentRow.getIndex() == dataSet.getChildCount()) {
         return;
      }
      /* current row succesors */
      SortedMap successors = new TreeMap();
      /* current row index */
      Integer currentRowIndex = new Integer(currentRow.getIndex());

      for (int index = currentRow.getIndex() + 1; index < dataSet.getChildCount(); index++) {
         XComponent row = (XComponent) dataSet.getChild(index);
         // predecesors of the index row
         SortedMap predecesors = getPredecessors(row);

         Object v = predecesors.get(currentRowIndex);
         if (v != null) {
            successors.put(new Integer(row.getIndex()), v);
         }
      }
      /* set up the succesors of the current row */
      setSuccessors(currentRow, successors);
   }

   /**
    * Returns the start date for a <code>COLLECTION_ACTIVITY</code> given by it's children
    *
    * @param children the children <code> XArray </code> of the collection
    * @return the minimum start date of the children
    */
   protected Date getStartDateForCollection(List children) {
      /* the first child start date */
      Date startDate = getStart((XComponent) children.get(0));

      for (int index = 1; index < children.size(); index++) {
         XComponent row = (XComponent) children.get(index);

         if (getStart(row).before(startDate)) {
            startDate = getStart(row);
         }
      }
      return startDate;
   }

   /**
    * Returns the end date for a <code>COLLECTION_ACTIVITY</code> given by it's children.
    *
    * @param children the children <code> XArray </code> of the collection
    * @return the minimum start date of the children
    */
   protected Date getEndDateForCollection(List children) {
      /* the first child end date */
      Date endDate = getEnd((XComponent) children.get(0));

      for (int index = 1; index < children.size(); index++) {
         XComponent row = (XComponent) children.get(index);

         if (getEnd(row).after(endDate)) {
            endDate = getEnd(row);
         }
      }
      return endDate;
   }

   /**
    * Sets up the activity <code>ENABLED</code> property for start Date, end Date duration and base effort.
    *
    * @param activityRow the activity row
    * @param editable    boolean value for the <code>ENABLED</code> property .
    */
   public void setActivityRowEditable(XComponent activityRow, boolean editable) {
      XView startDateCell = activityRow.getChild(START_COLUMN_INDEX);
      startDateCell.setEnabled(editable);

      XView endDateCell = activityRow.getChild(FINISH_COLUMN_INDEX);
      endDateCell.setEnabled(editable);

      XView durationCell = activityRow.getChild(DURATION_COLUMN_INDEX);
      durationCell.setEnabled(editable);

      XView baseEffortCell = activityRow.getChild(BASE_EFFORT_COLUMN_INDEX);
      baseEffortCell.setEnabled(editable);
   }

   /**
    * Copies a <code>XArray</code> of data rows to the clipboard of the XDisplay. The data rows are copied to
    * clipboard by value using <code>onepoint.express.XComponent#copyData()</code>. Method is used for both
    * <code>cut</code> and <code>copy</code> operations.
    *
    * @param dataRows  the <code>XArray </code> of <code>DATA_ROW</code>s
    * @param copy_rows the a flag indication if the operation is <code>cut</code> or <code>copy</code>
    * @see onepoint.express.XComponent#copyData()
    */
   protected final void _copyToClipboard(List dataRows, boolean copy_rows) {
      // Copy data-rows to static clipboard; adjust index references ("inside" only)
      // Create index-map and add data-rows to clipboard
      XComponent clipboard = XDisplay.getClipboard();
      clipboard.removeAllChildren();
      clipboard.setClipboardOriginalIndexes(null);

      XComponent row = null;
      Hashtable index_map = new Hashtable();
      Integer row_index = null;

      List clonedDataRows = copyRows(dataRows, copy_rows);

      /* array of original indexes of the data rows */
      List originalIndexes = new ArrayList();
      for (int i = 0; i < clonedDataRows.size(); i++) {
         row = ((XComponent) clonedDataRows.get(i));
         OpGanttValidator.setWorkRecords(row, new HashMap());
         /* put the corresponding rowIndex in the map */
         Integer originalRowIndex = new Integer(((XComponent) dataRows.get(i)).getIndex());
         index_map.put(originalRowIndex, new Integer(i));
         /* put the original index in the clipboard proeperty */
         originalIndexes.add(originalRowIndex);
         /* add the row to clipboard */
         clipboard.addChild(row);
      }
      // finally set up the clipboard property
      clipboard.setClipboardOriginalIndexes(originalIndexes);

      // Filter and adjust index-references
      for (int i = 0; i < clonedDataRows.size(); i++) {
         row = (XComponent) (clonedDataRows.get(i));
         filterIndexList(getPredecessors(row), index_map);
         filterIndexList(getSuccessors(row), index_map);
      }
   }

   private List copyRows(List dataRows, boolean resetRowValue) {
      XComponent row;
      XComponent origRow;
      /* make a copy of the dataRows and use the elements for clipboard placing */
      List clonedDataRows = new ArrayList();
      boolean gotRootOfSubproject = false;
      for (int index = 0; index < dataRows.size(); index++) {
         row = ((XComponent) dataRows.get(index)).copyData();
         if (importedActivity(row)) {
            continue;
         }
         //copy rows business case clears the activity row value
         if (resetRowValue) {
            row.setValue(null);
         }
         clonedDataRows.add(row);
      }
      /* array of succesors */
      SortedMap succesors;
      /* array of predecessors */
      SortedMap predecessors;
      /* array of resources */
      List resources;
      /* array of resources base efforts*/
      Map resourceEfforts;
      
      Map customAttributes;
      
      for (int i = 0; i < clonedDataRows.size(); i++) {
         row = ((XComponent) clonedDataRows.get(i));
         origRow = ((XComponent) dataRows.get(i));
         /* set up the succesors of the row */
         succesors = new TreeMap();
         mergeMaps(succesors, OpGanttValidator.getSuccessors(row));
         OpGanttValidator.setSuccessors(row, succesors);

         /* set up the predecessors of the row */
         predecessors = new TreeMap();
         mergeMaps(predecessors, OpGanttValidator.getPredecessors(row));
         OpGanttValidator.setPredecessors(row, predecessors);
         
         /* set up the resources of the row*/
         resources = new ArrayList();
         resources.addAll(OpGanttValidator.getResources(row));
         OpGanttValidator.setResources(row, resources);

         /* set up the resource efforts of the row*/
         resourceEfforts = new HashMap();
         Iterator reit = OpGanttValidator.getResourceBaseEfforts(row).entrySet().iterator();
         while (reit.hasNext()) {
            Map.Entry e = (java.util.Map.Entry) reit.next();
            resourceEfforts.put(e.getKey(), e.getValue());
         }
         OpGanttValidator.setResourceBaseEfforts(row, resourceEfforts);

         setUpCustomAttributes(row, origRow);
         setUpActions(row, origRow);
      }
      return clonedDataRows;
   }

   /**
    * @param row
    * @param origRow
    * @pre
    * @post
    */
   protected void setUpActions(XComponent row, XComponent origRow) {
   }

   /**
    * @param row
    * @param origRow
    * @pre
    * @post
    */
   protected void setUpCustomAttributes(XComponent row, XComponent origRow) {
   }

   /**
    * @see XValidator#copyToClipboard(List)
    */
   public void copyToClipboard(List selected_rows) {
      _copyToClipboard(selected_rows, true);
      cleanClipboard = false;
   }

   /**
    * @see XValidator#cutToClipboard(List)
    */
   public void cutToClipboard(List selected_rows) {
      // copy the selected rows to clipboard
      _copyToClipboard(selected_rows, false);
      // remove the selected rows from data set first
      try {
         removeDataRows(selected_rows);
      }
      catch (XValidationException e) {
         XComponent clipboard = XDisplay.getClipboard();
         clipboard.removeAllChildren();
         throw e;
      }
      cleanClipboard = true;
   }

   /**
    * Filters the <code>selectedRows</code> which represents the target of the paste operation. If an activity in the
    * <code>selectedRows</code> is a <code>COLLECTION</code> it's children are removed from the
    * <code>selectedRows</code>
    *
    * @param selectedRows a <code>XArray</code> representing the target of the paste operation.
    * @return a <code>XArray</code> after filtering the <code>selectedRows</code>
    */
   public List filterSelectedRowsForPasteOperation(List selectedRows) {
      List filterRows = new ArrayList();
      for (int index = 0; index < selectedRows.size(); index++) {
         // get a row
         XComponent row = (XComponent) selectedRows.get(index);
         // add it to the filtered array
         filterRows.add(row);
         if (OpGanttValidator.getType(row) == COLLECTION || OpGanttValidator.getType(row) == OpGanttValidator.SCHEDULED_TASK) {
            // skip it's children
            index = index + getChildren(row).size();
         }
      }
      return filterRows;
   }

   /**
    * Paste the clipboard rows in the data set.
    *
    * @param selected_rows a <code>XArray</code> of <code>DATA_ROW</code>s where the clipboard are pasted.
    * @param insert
    */
   public void pasteFromClipboard(List selected_rows, boolean insert) {
      addToUndo();

      XComponent displayClipboard = XDisplay.getClipboard();
      List allChildren = new ArrayList();
      
      for (int i = 0; i < displayClipboard.getChildCount(); i++) {
         XComponent child = (XComponent) displayClipboard.getChild(i);
         allChildren.add(child);
      }
      List clipboardRows = copyRows(allChildren, true);
      XComponent clipboard = new XComponent(XComponent.DATA_SET);
      for (int i = 0; i < clipboardRows.size(); i++) {
         XComponent row = (XComponent) clipboardRows.get(i);
         clipboard.addChild(row);
      }

      if (clipboard.getChildCount() == 0) {
         return;
      }

      int startIndex = ((XComponent) selected_rows.get(0)).getIndex();
      if (importedActivity((XComponent) selected_rows.get(0))) {
         throw new XValidationException(PROGRAM_ELEMENT_MOVE_EXCEPTION);
      }

      if (!insert) {
         removeDataRows(selected_rows);
      }

      continuousAction = true;

      // normalize clipboard outline level...
      int startOutlineLevel = ((XComponent) selected_rows.get(0)).getOutlineLevel();
      normalizeOutlineLevel(clipboard, startOutlineLevel);

      // update succ and pred index lists
      List origIndexes = displayClipboard.getClipboardOriginalIndexes();

      Hashtable newIndexes = new Hashtable();
      for (int i = 0; i < origIndexes.size(); i++) {
         newIndexes.put(origIndexes.get(i), new Integer(startIndex + i));
      }

      XComponent[] children = new XComponent[data_set.getChildCount() + clipboard.getChildCount()];

      // add the clipboard to the dataset
      for (int i = 0; i < startIndex; i++) {
         children[i] = (XComponent) data_set.getChild(i);
      }
      XComponent dummy = newDataRow();
      try {
         disableValidation  = true;
         for (int i = 0; i < clipboard.getChildCount(); i++) {
            XComponent row = (XComponent) clipboard.getChild(i);
            setSuccessors(row, updateIndexList(getSuccessors(row), newIndexes));
            setPredecessors(row, updateIndexList(getPredecessors(row), newIndexes));
            if (isProgressTracked()) {
               //remove complete
               OpGanttValidator.setComplete(row, 0);
            }
            addDataRow(startIndex, dummy);
            children[startIndex + i] = row;
         }
      } 
      finally {
         disableValidation = false;
      }
      validateDataSet();
      for (int i = startIndex + clipboard.getChildCount(); i < data_set.getChildCount(); i++) {
         children[i] = (XComponent) data_set.getChild(i);
      }

      // clean the clipboard
      if (cleanClipboard) {
         displayClipboard.removeAllChildren();
      }

      data_set.removeAllChildren();
      data_set.addAllChildren(children);

      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent dataRow = (XComponent) data_set.getChild(i);
         List resources = getResources(dataRow);
         for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
            String resource = (String) iterator.next();
            boolean found = false;
            XComponent assignmentSet = getAssignmentSet();
            for (int j = 0; j < assignmentSet.getChildCount(); j++) {
               XComponent row = (XComponent) assignmentSet.getChild(j);
               String assignmentResource = XValidator.choiceID(row.getStringValue());
               String resourceID = XValidator.choiceID(resource);
               if (assignmentResource.equals(resourceID)) {
                  found = true;
               }
            }
            if (!found) {
               iterator.remove();
            }
         }
      }

      // clean clipboard's rows original indexes
      if (cleanClipboard) {
         displayClipboard.setClipboardOriginalIndexes(null);
      }
      validateDataSet();

      continuousAction = false;
   }

   /**
    * Normalizes the outline level in the given data set. The first row in the data set will always have outline level
    * minLevel, and the next outline levels will be corrected as not to be greater that the previous one with more that
    * 1 unit.
    *
    * @param dataSet  the data set that has to be "normalized".
    * @param minLevel the minimum outline level for the normalized data set
    */
   private void normalizeOutlineLevel(XComponent dataSet, int minLevel) {

      //normalize outline levels in given data set
      int prevLevel;
      XComponent row = (XComponent) dataSet.getChild(0);
      prevLevel = row.getOutlineLevel();
      for (int i = 1; i < dataSet.getChildCount(); i++) {
         row = (XComponent) dataSet.getChild(i);
         int outlineLevel = row.getOutlineLevel();
         if (outlineLevel > prevLevel + 1) {
            outlineLevel = prevLevel + 1;
            row.setOutlineLevel(outlineLevel);
         }
         prevLevel = outlineLevel;
      }

      //align them to minlevel
      row = (XComponent) dataSet.getChild(0);
      int firstOutline = row.getOutlineLevel();
      int offset = minLevel - firstOutline;
      int outmostOffest = 0;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         row = (XComponent) dataSet.getChild(i);
         if (row.getOutlineLevel() + offset + outmostOffest < 0) {
            outmostOffest = -(row.getOutlineLevel() + offset + outmostOffest);
         }
         row.setOutlineLevel(row.getOutlineLevel() + offset + outmostOffest);
      }
   }

   /**
    * Checks that a list of indexes contains only valid entries with regards to the number of data rows.
    *
    * @param array a <code>XArray</code> of <code>Integer</code> representing indexes.
    * @return <code>true</code> if the index list is valid, <code>false</code> otherwise.
    */
   public boolean checkIndexList(SortedMap map) {
      // Check index list range
      // TODO: use sorted feature, check only last element here?!?
      if (map != null) {
         int max_value = data_set.getChildCount() - 1;
         Iterator mit = map.keySet().iterator();
         while (mit.hasNext()) {
            Integer i = (Integer) mit.next();
            if ((i.intValue() < 0) || (i.intValue() > max_value)) {
               throw new XValidationException(RANGE_EXCEPTION);
            }
         }
      }
      return true;
   }

   public static final void removeIndexListValue(SortedMap map, int value) {
      map.remove(new Integer(value));
   }

   public static final boolean containsIndexListValue(SortedMap map, int value) {
      return map.containsKey(new Integer(value));
   }

   public void diffIndexMaps(SortedMap map2, SortedMap map1, SortedMap added_elements,
         SortedMap removed_elements) {
      int value = 0;
      int index = 0;
      Iterator kit1 = map1.keySet().iterator();
      Set visited = new HashSet();
      while (kit1.hasNext()) {
         Object key1 = kit1.next();
         Object value1 = map1.get(key1);
         Object value2 = map2.get(key1);
         
         if (value1 != null) {
            if (value2 == null) {
               added_elements.put(key1, value1);
            }
            else if (!value2.equals(value1)) {
               added_elements.put(key1, value1);
               removed_elements.put(key1, value2);
            }
            visited.add(key1);
         }
      }
      
      Iterator kit2 = map2.keySet().iterator();
      while (kit2.hasNext()) {
         Object key2 = kit2.next();
         if (!visited.contains(key2)) {
            // two possible reasons: value 1 is null or keys does not exist in map1
            removed_elements.put(key2, map2.get(key2));
         }
      }
   }

   /**
    * Updates the given array (Successors or Predecessors) after an add was made in the data set. If one element from
    * the <code>array</code> is greater then <code>start</code> but lower than <code>end</code> the value is
    * incresead by <code>offset</code>
    *
    * @param array          <code>XArray</code> of <code>Integer</code> (succesors or predecesors)
    * @param elemToAddIndex a <code>int</code> the index where the new element will be added.
    */
   private static SortedMap updateIndexListAfterAdd(SortedMap indexMap, int elemToAddIndex) {
      return updateIndexListAfterAdd(indexMap, elemToAddIndex, 1);
   }

   public static SortedMap updateIndexListAfterAdd(SortedMap indexMap, int position, int number) {
      Iterator kit = indexMap.keySet().iterator();
      SortedMap newIndexMap = new TreeMap();
      while (kit.hasNext()) {
         Integer idx = (Integer) kit.next();
         if (idx.intValue() >= position) {
            newIndexMap.put(new Integer(idx.intValue() + number), indexMap.get(idx));
         }
         else {
            newIndexMap.put(idx, indexMap.get(idx));
         }
      }
      return newIndexMap;
   }

   /**
    * Updates the given array (Successors or Predecessors) after a remove was made in the data set. If one element from
    * the <code>array</code> is greater then <code>removedElementIndex</code> but lower than <code>end</code> the value is
    * incresead by <code>offset</code>. The removedElementIndex index will be removed from the given array.
    *
    * @param array               <code>XArray</code> of <code>Integer</code> (succesors or predecesors)
    * @param removedElementIndex the removedElementIndex index, index of the modified row.
    */
   private static SortedMap updateIndexListAfterRemove(SortedMap indexMap, int removedElementIndex) {
      Iterator kit = indexMap.keySet().iterator();
      SortedMap newIndexMap = new TreeMap();
      while (kit.hasNext()) {
         Integer idx = (Integer) kit.next();
         if (idx.intValue() > removedElementIndex) {
            newIndexMap.put(new Integer(idx.intValue() - 1), indexMap.get(idx));
         }
         else if (idx.intValue() < removedElementIndex) {
            newIndexMap.put(idx, indexMap.get(idx));
         }
         else {
            logger.debug("Removed link to " + removedElementIndex);
            // do nothing...
         }
      }
      return newIndexMap;
   }

   /**
    * Updates a <code>XArray[Integer]<code> with the values from the <code>index_map</code>.
    * An element of the <code>array</code> represents the <code>key</code> in the <code>index_map</code>.
    *
    * @param array     the <code>XArray</code>
    * @param index_map the Hashtable
    */
   public SortedMap updateIndexList(SortedMap map, Hashtable index_map) {
      SortedMap newMap = new TreeMap();
      Iterator mit = map.keySet().iterator();
      while (mit.hasNext()) {
         Object key = mit.next();
         Object newKey = index_map.get(key);
         if (newKey != null) {
            newMap.put(newKey, map.get(key));
         }
         else {
            newMap.put(key, map.get(key));
         }
      }
      return newMap;
   }

   /**
    * Updates a <code>XArray[Integer]<code> with the values from the <code>index_map</code>.
    * One element from the <code>array</code> represents the <code>key</code> in the <code>index_map</code>.
    *
    * @param array     a <code>XArray</code> if <code>Integer</code>s representing the list of successors or predecessors
    * @param index_map a <code>Hashtable</code> where
    *                  <code>key</code> represents the original index of the a copy/cut activity
    *                  <code>value</code> represents the updated index of the activity.
    */
   public void updateClipboardIndexList(List array, Hashtable index_map) {
      Integer new_index = null;
      for (int i = 0; i < array.size(); i++) {
         new_index = (Integer) (index_map.get(array.get(i)));
         if (new_index != null) {
            array.set(i, new_index);
         }
         else {
            array.remove(i);
         }
      }
   }

   /**
    * Filters a <code>XArray[Integer]</code>.If an element of the <code>array</code> has the value between
    * <code>start_index</code> and <code>end_index</code> it will be removed.
    *
    * @param array       the <code>XArray</code> of <code>Integer</code> values
    * @param start_index the start index value
    * @param end_index   the end index value
    */
   public void filterIndexList(List array, int start_index, int end_index) {

      for (int index = 0; index < array.size(); index++) {
         int element = ((Integer) array.get(index)).intValue();
         if ((element >= start_index) && (element <= end_index)) {
            array.remove(index);
         }
      }
   }

   /**
    * Filters a <code>XArray[Integer]</code>.An element of the <code>array</code> represents the <code>key</code>
    * in the <code>index_map</code>.If the element doesn't have a coresponding value it will be removed.
    *
    * @param map     the <code>XArray</code> of <code>Integer</code> values
    * @param index_map a <code>Hashtable</code> of indexes
    */
   public void filterIndexList(SortedMap map, Hashtable index_map) {

      Iterator mit = map.keySet().iterator();
      while (mit.hasNext()) {
         Object key = mit.next();
         if (!index_map.contains(key)) {
            mit.remove();
         }
      }
   }

   /**
    * Updates the given data row with the values given in the array
    *
    * @param dataRow row to be updated
    * @param array    the new values of the data row. The order is important:
    *                 0 - start
    *                 1 - end
    *                 2 - duration
    *                 3 - effort
    *                 4 - assignments
    */
   public void updateDataCells(XComponent dataRow, List array) {

      Date start, end;
      Double duration;
      Double effort;
      List assignments;

      start = (Date) array.get(0);
      end = (Date) array.get(1);

      //old start
      Date oldStart = getStart(dataRow);
      //old end
      Date oldEnd = getEnd(dataRow);
      //old base effort
      double oldBaseEffort = getBaseEffort(dataRow);

      if (start != null && end != null) {
         //duration
         duration = (Double) array.get(2);
         if (duration != null && duration.doubleValue() != getDuration(dataRow)) {
            setDataCellValue(dataRow, DURATION_COLUMN_INDEX, duration);
         }
         //effort
         effort = (Double) array.get(3);
         if (effort != null && effort.doubleValue() != oldBaseEffort) {
            setDataCellValue(dataRow, BASE_EFFORT_COLUMN_INDEX, effort);
         }
      }
      else {
         //start/end are null -> update effort
         effort = (Double) array.get(3);
         if (effort != null && effort.doubleValue() != oldBaseEffort) {
            setDataCellValue(dataRow, BASE_EFFORT_COLUMN_INDEX, effort);
         }
      }

      if ((start == null && oldStart != null) || (start != null && !start.equals(oldStart))) {
         setDataCellValue(dataRow, START_COLUMN_INDEX, start);
      }

      if ((end == null && oldEnd != null) || (end != null && !end.equals(oldEnd))) {
         setDataCellValue(dataRow, FINISH_COLUMN_INDEX, end);
      }

      //assignments
      boolean assignments_changed = false;
      assignments = (ArrayList) array.get(4);
      if (assignments != null) {
         List old_assignments = getResources(dataRow);
         if (assignments.size() != old_assignments.size()) {
            assignments_changed = true;
         }
         else {
            for (int j = 0; j < assignments.size(); j++) {
               if (!containsAssignment(dataRow, (String) assignments.get(j))) {
                  assignments_changed = true;
                  break;
               }
            }
         }
         if (assignments_changed) {
            setDataCellValue(dataRow, VISUAL_RESOURCES_COLUMN_INDEX, assignments);
         }
      }

   }

   private boolean containsAssignment(XComponent dataRow, String assignment) {
      List assignments = getResources(dataRow);
      for (int i = 0; i < assignments.size(); i++) {
         String s = (String) assignments.get(i);
         if (s.equals(assignment)) {
            return true;
         }
      }
      return false;
   }

   private static final Pattern HOURS_PATTERN = Pattern.compile("(.+) ([0-9,.+\\-eE]+)h");
   private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("(.+) ([0-9,.+\\-eE]+)%");
   
   private static final int NAME_GROUP = 1;
   private static final int NUMBER_GROUP = 2;
   /**
    * Returns a numerical value representing the percentage a resource is assigned, from a string.
    *
    * @param assignment a <code>String</code> containing the percentage of a assignment.
    * @return a <code>byte</code> representing the percentage a resource is assigned.
    */
   private static Double parseSomethingAssigned(String assignment, Pattern pattern, OpProjectCalendar calendar) {
      String caption = XValidator.choiceCaption(assignment);
      Matcher m = pattern.matcher(caption);
      if (m.matches()) {
         double assignmentValue = 0d;
         String captionNumber = m.group(NUMBER_GROUP);
         try {
            if (calendar != null) {
               assignmentValue = calendar.parseDouble(captionNumber);
            }
            else {
               assignmentValue = Double.parseDouble(captionNumber);
            }
         }
         catch (ParseException e) {
            logger.warn(captionNumber + " is not a valid number", e);
            return null;
         }
         assignmentValue = assignmentValue < 0d ? 0d : assignmentValue;
         return new Double(assignmentValue);
      }
      else {
         return null;
      }
   }

   public static String getResourceName(String caption) {
	   return getResourceName(caption, null);
   }
   
   public static String getResourceName(String caption, String expectedView) {
      if (expectedView == null || expectedView.equals("h")) {
         Matcher m = HOURS_PATTERN.matcher(caption);
         if (m.matches()) {
            return m.group(NAME_GROUP);
         }
      }
      if (expectedView == null || expectedView.equals("%")) {
         Matcher m = PERCENTAGE_PATTERN.matcher(caption);
         if (m.matches()) {
            return m.group(NAME_GROUP);
         }
      }
      return caption;
   }

   public double percentageAssignedOrAvailable(String assignment) {
      Double res = parseSomethingAssigned(assignment, PERCENTAGE_PATTERN, null);
      if (res != null) {
         return res.doubleValue();
      }
      return getResourceAvailability(XValidator.choiceID(assignment));
   }
   
   public double hoursAssignedOrAvailable(String assignment) {
      Double res = parseSomethingAssigned(assignment, HOURS_PATTERN, null);
      if (res != null) {
         return res.doubleValue();
      }
      return getHoursAvailable(assignment);
   }

   private double getHoursAvailable(String assignment) {
      String resourceId = XValidator.choiceID(assignment);
      return getResourceAvailability(resourceId) * getCalendar(resourceId).getWorkHoursPerDay() / 100;
   }
   
   public static double percentAssigned(String assignment) {
      return percentageAssigned(assignment);
   }
   public static double percentageAssigned(String assignment) {
      Double res = parseSomethingAssigned(assignment, PERCENTAGE_PATTERN, null);
      return res == null ? INVALID_ASSIGNMENT : res.doubleValue();
   }
   
   public static double localizedPercentageAssigned(String assignment, OpProjectCalendar calendar) {
      Double res = parseSomethingAssigned(assignment, PERCENTAGE_PATTERN, calendar);
      return res == null ? INVALID_ASSIGNMENT : res.doubleValue();
   }
   
   public static boolean isPositivePercentageAssigned(String assignment) {
      Double res = parseSomethingAssigned(assignment, PERCENTAGE_PATTERN, null);
      return res == null ? true : res.doubleValue() > 0;
   }
   
   public static double hoursAssigned(String assignment) {
      Double res = parseSomethingAssigned(assignment, HOURS_PATTERN, null);
      return res == null ? INVALID_ASSIGNMENT : res.doubleValue();
   }
   
   public static double localizedHoursAssigned(String assignment, OpProjectCalendar calendar) {
      Double res = parseSomethingAssigned(assignment, HOURS_PATTERN, calendar);
      return res == null ? INVALID_ASSIGNMENT : res.doubleValue();
   }
   
   public static boolean isPositiveHoursAssigned(String assignment) {
      Double res = parseSomethingAssigned(assignment, HOURS_PATTERN, null);
      return res == null ? true : res.doubleValue() > 0;
   }

   /**
    * Returns the assignment without the specified effort from the assignment's caption
    *
    * @param assignment a <code>String</code> containing the percentage of a assignment.
    * @return a <code>String</code> representing the assignment without the specified effort.
    */
   public static String deleteEffortAssignment(String assignment) {
      String caption = XValidator.choiceCaption(assignment);
      String result = assignment;
      Matcher hm = HOURS_PATTERN.matcher(assignment);
      Matcher pm = PERCENTAGE_PATTERN.matcher(assignment);
      if (hm.matches()) {
         result = hm.group(NAME_GROUP);
      }
      if (pm.matches()) {
         result = pm.group(NAME_GROUP);
      }
      return result;
   }

   public OpProjectCalendar getCalendar() {
      return getCalendar(null);
   }

   /**
    * get the right calendar for the resource...
    * @param resource
    * @return
    */
   public OpProjectCalendar getCalendar(String resource) {
      XComponent form = getDataSet() != null ? getDataSet().getForm() : null;
      if (projectCalendar == null) {
         projectCalendar = (form != null
               && form.findComponent(PROJECTCALENDAR_ID) != null) ? (OpProjectCalendar) form
               .findComponent(PROJECTCALENDAR_ID).getValue() : null;
      }
      if (resourceCalendarMap == null) {
         resourceCalendarMap = (form != null
               && form.findComponent(RESOURCECALENDARS_ID) != null && form
               .findComponent(RESOURCECALENDARS_ID).getValue() != null) ? (Map) form
               .findComponent(RESOURCECALENDARS_ID).getValue()
               : new HashMap();
      }
      return OpGanttValidator.getCalendar(resource, OpProjectCalendar.getDefaultProjectCalendar(), getProjectCalendar(), resourceCalendarMap);
   }
   
   public static OpProjectCalendar getCalendar(
         OpProjectCalendar defaultCalendar, OpProjectCalendar projectCalendar) {
      OpProjectCalendar c= null;
      if (projectCalendar != null) {
         return projectCalendar;
      }
      else {
         return defaultCalendar;
      }
   }

   public static OpProjectCalendar getCalendar(
         OpProjectCalendar defaultCalendar, OpProjectCalendar projectCalendar,
         OpProjectCalendar resourceCalendar) {
      OpProjectCalendar c= null;
      if (resourceCalendar != null) {
         return resourceCalendar;
      }
      else {
         return getCalendar(defaultCalendar, projectCalendar);
      }
   }

   public static OpProjectCalendar getCalendar(String resource, OpProjectCalendar defaultCalendar, OpProjectCalendar projectCalendar, Map resourceCalendarMap) {
      OpProjectCalendar resCal= null;
      if (resourceCalendarMap != null && resource != null) {
         resCal = (OpProjectCalendar) resourceCalendarMap.get(resource);
      }
      return getCalendar(defaultCalendar, projectCalendar, resCal);
   }
   /**
    * setup calendars for use of this validator on the server side.
    * @param projectCalendar
    * @param resourceCalendarMap
    */
   public void setupCalendars(OpProjectCalendar projectCalendar, Map resourceCalendarMap) {
      this.projectCalendar = projectCalendar;
      this.resourceCalendarMap = resourceCalendarMap;
   }
   
   /**
    * setup absences for use of this validator on the server side.
    * @param absences
    */
   public void setupAbsences(Map absences) {
      this.resourceAbsences = absences;
   }
   
   public boolean resourceIsAbsent(String resource, Date day) {
      XComponent form = getDataSet().getForm();
      if (resourceAbsences == null) {
         resourceAbsences = (form != null
               && form.findComponent(ABSENCES_ID) != null && form
               .findComponent(ABSENCES_ID).getValue() != null) ? (Map) form
               .findComponent(ABSENCES_ID).getValue() : new HashMap();
      }
      if (resourceAbsences.get(resource) == null) {
         return false;
      }
      return ((Set)resourceAbsences.get(resource)).contains(day);
   }
   
   /**
    * Tries to update the base effort for a given activity. The end date and duration are changed according to the new
    * effort. This kind of update is not possible for collections.
    *
    * @param dataRow the data for this activity
    * @param effort   -
    *                 the desired new value for the effort.
    * @return true if the effort can be updated / false otherwise (for activity != STANDARD) the effort can't be
    *         updated.
    */
   public boolean updateBaseEffort(XComponent dataRow, double effort) {
      if (importedActivity(dataRow)) {
         return false;
      }

      // effort must be >= 0
      effort = effort < 0d ? 0d : effort;

      if (OpGanttValidator.getType(dataRow) == COLLECTION
            || OpGanttValidator.getType(dataRow) == OpGanttValidator.SCHEDULED_TASK) {
         //effort can't be changed for collections
         return false;
      }
      if (OpGanttValidator.getType(dataRow) == MILESTONE && effort > 0) {
         if (isEffortBasedProject()) {
            updateType(dataRow, STANDARD);
         }
         else {
            return false;
         }
      }
      
      // for effort based stuff, we should first find out how to adjust duration:
      if (isEffortBasedProject()) {
         // get assigned effort ignoring eventually present ? resource
         double dailyEffortAssigned = 0d;
         if (!getResources(dataRow).isEmpty()) {
            dailyEffortAssigned = getDailyEffortForAssignments(getResources(dataRow), true);
            if (isZeroWithTolerance(dailyEffortAssigned, effort)) {
               dailyEffortAssigned = getMaxDailyEffortForAssignments(getResources(dataRow));
            }
         }
         else {
            dailyEffortAssigned = getCalendar().getWorkHoursPerDay();
         }
         double adjustedDuration = (Math.ceil(effort / dailyEffortAssigned) * getCalendar()
               .getWorkHoursPerDay());
         setDuration(dataRow, adjustedDuration);
      }
      
      adjustRemainingEffort(dataRow, effort - getBaseEffort(dataRow), isProgressTracked());
      setBaseEffort(dataRow, effort);

      //for tasks only set the base effort
      if (getType(dataRow) == TASK) {
         adjustTaskActivity(dataRow);
      }
      else {
         // *** Distribute effort to resources based on percentages
         // *** TODO: Highly optimizable by special XChoice data type (or use DATA_ROW instead)
   
         //update the % of the resources in the activity, if any
         try {
            // the assignments gets adjusted to fit the new effort (always, because for effort-based case, duration was already updated above)
            List resources = setupResources(getDuration(dataRow),
                     getBaseEffort(dataRow), getResources(dataRow),
                     getResourceAssignmentRule(dataRow), !isEffortBasedProject());
            setResources(dataRow, resources);
         }
         catch (XValidationException e) {
            logger.info("Error in resource assignment...");
         }
         
      }
      //update visual resources
      updateResourceVisualization(dataRow);
      return true;
   }

   private double getDailyEffortForAssignments(List assignments, boolean ignoreUnnamedResource) {
      double dailyEffort = 0d;
      if (assignments.isEmpty()) {
         dailyEffort = getCalendar().getWorkHoursPerDay();
      }
      else {
         Iterator rit = assignments.iterator();
         while (rit.hasNext()) {
            String res = (String) rit.next();
            String resourceId = choiceID(res);
            if (ignoreUnnamedResource || !NO_RESOURCE_ID.equals(resourceId)) {
               dailyEffort += getIndividualEffortPerDay(res);
            }
         }
      }
      return dailyEffort;
   }

   private double getMaxDailyEffortForAssignments(List assignments) {
      double dailyEffort = 0d;
      Iterator rit = assignments.iterator();
      while (rit.hasNext()) {
         String res = (String) rit.next();
         String resourceId = choiceID(res);
         if (!NO_RESOURCE_ID.equals(resourceId)) {
            dailyEffort += getHoursAvailable(res);
         }
      }
      return dailyEffort;
   }

   public static Map createWorkPhaseDesc(Date wphStart, Date wphEnd,
         double wphEffort) {
      Map wfDesc = new HashMap(3);
      wfDesc.put(WORK_PHASE_START_KEY, wphStart);
      wfDesc.put(WORK_PHASE_FINISH_KEY, wphEnd);
      wfDesc.put(WORK_PHASE_EFFORT_KEY, new Double(wphEffort));
      return wfDesc;
   }

   private static class ApplyDurationCallback implements ActivityIterationCallback {
      
      private boolean distributeHours = false;
      private String[] resourceIds = null;
      private double[] dailyEffortPerResource = null;
      private XComponent dataRow = null;
      private List assignments = null;
      private Date workPhaseStart = null;
      private double workPhaseEffort = 0d;
      
      private double personellCosts = 0d;
      private double proceeds = 0d;
      private int workDays = 0;
      
      private Date latestDayVisited = null;
      
      private Date firstActivityDayVisited = null;
      private Date lastActivityDayVisited = null;
      
      private double[] resourceEffortsAccumulated = null;
      private double effortAccumulated = 0d;
      private double assignedEffortAccumulated = 0d;
      private double activityWorkHoursPerDay = 0d; 
      
      // the somewhat sick thing here is, that milestones do not have a duration, but has
      // the same start and end dates as an activity with the duration of one day!
      private boolean isMilestone = false;
      private boolean hasEffort = false;
      
      public int getWorkDays() {
         return isMilestone ? 0 : workDays;
      }

      public double getPersonellCosts() {
         return personellCosts;
      }

      public double getProceeds() {
         return proceeds;
      }

      public Date getLatestDayVisited() {
         return latestDayVisited;
      }

      public Date getFirstActivityDayVisited() {
         return firstActivityDayVisited;
      }

      public Date getLastActivityDayVisited() {
         return lastActivityDayVisited;
      }

      public Map getResourceEffortsAccumulated() {
         Map result = new HashMap();
         for(int i = 0; i < resourceIds.length; i++) {
            result.put(resourceIds[i], new Double(resourceEffortsAccumulated[i]));
         }
         return result;
      }

      public double getEffortAccumulated() {
         return effortAccumulated;
      }

      public double getAssignedEffortAccumulated() {
         return assignedEffortAccumulated;
      }
      public ApplyDurationCallback(OpGanttValidator validator,
            XComponent dataRow) {
         this(validator, dataRow, OpGanttValidator.INVALID_ASSIGNMENT);
      }
      
      public ApplyDurationCallback(OpGanttValidator validator,
            XComponent dataRow, double durationDays) {
         this.distributeHours = validator.isHourBasedResourceView();
         this.dataRow = dataRow;
         
         OpGanttValidator.resetWorkPhases(this.dataRow);
         
         this.assignments = OpGanttValidator.getResources(dataRow);
         if (assignments == null) {
            assignments = new ArrayList();
         }
         
         this.dailyEffortPerResource = new double[assignments.size()];
         this.resourceEffortsAccumulated = new double[assignments.size()];
         this.resourceIds = new String[assignments.size()];
         int i = 0;
         Iterator rit = assignments.iterator();
         while (rit.hasNext()) {
            String resource = (String) rit.next();
            String resourceId = XValidator.choiceID(resource);
            double resEffort = validator.getIndividualEffortPerDay(resource);
            dailyEffortPerResource[i] = resEffort;
            resourceEffortsAccumulated[i] = 0;
            resourceIds[i] = resourceId;
            i++;
         }
         
         isMilestone = OpGanttValidator.getType(dataRow) == OpGanttValidator.MILESTONE;
         hasEffort = OpGanttValidator.hasEffort(dataRow);
         if (validator.isEffortBasedProject() && OpGanttValidator.isOfType(dataRow, OpGanttValidator.APPLY_DURATION_TYPES)) {
            activityWorkHoursPerDay = validator.getCalendar().getWorkHoursPerDay();
         }
         else {
            activityWorkHoursPerDay = getBaseEffort(dataRow)
                  / (durationDays == INVALID_ASSIGNMENT ? validator
                        .getDurationDays(dataRow) : durationDays);
         }
      }

      public void iteration(ActivityIterator iterator, Date date,
            boolean workDay, boolean breakDay, boolean forward) {
         
         latestDayVisited = date;
         if (workDay && workPhaseStart == null) {
            // a workPhase starts
            workPhaseStart = date;
            workPhaseEffort = 0d;
         }
         if (!workDay && workPhaseStart != null) {
            // workPhase has ended
            Map wfDesc = OpGanttValidator.createWorkPhaseDesc(
                  forward ? workPhaseStart : new Date(date.getTime() + XCalendar.MILLIS_PER_DAY), forward ? date
                        : new Date(workPhaseStart.getTime() + XCalendar.MILLIS_PER_DAY), workPhaseEffort);
            workPhaseStart = null;
            workPhaseEffort = 0d;
            OpGanttValidator.addWorkPhase(dataRow, wfDesc);
         }
         if (workDay || breakDay) {
            if (firstActivityDayVisited == null) {
               firstActivityDayVisited = date;
            }
            lastActivityDayVisited = date;
         }
         if (workDay) {
            if (!assignments.isEmpty()) {
               Iterator rit = assignments.iterator();
               for (int i = 0; i < resourceIds.length; i++) {
                  String resourceId = resourceIds[i];
                  double resEffortPerDay = dailyEffortPerResource[i];
                  
                  if (hasEffort) {
                     workPhaseEffort += resEffortPerDay;
                     effortAccumulated += resEffortPerDay;
                     resourceEffortsAccumulated[i] += resEffortPerDay;

                     personellCosts += iterator.getValidator().getRateForResource(resourceId, INTERNAL_HOURLY_RATE_INDEX, date) * resEffortPerDay;
                     proceeds += iterator.getValidator().getRateForResource(resourceId, EXTERNAL_HOURLY_RATE_INDEX, date) * resEffortPerDay;
                     if (!resourceId.equals(NO_RESOURCE_ID)) {
                        assignedEffortAccumulated += resEffortPerDay;
                     }
                  }
               }
            }
            else {
               if (hasEffort) {
                  effortAccumulated += activityWorkHoursPerDay;
               }
            }
         }
      }

      public boolean isFinished(ActivityIterator iterator, Date date, boolean workDay, boolean forward)  {
         workDays += workDay ? 1 : 0;
         if (iterator.getDurationDays() == ActivityIterator.INVALID_NUMBER) {
            return date.after(iterator.getEndDate());
         }
         if (workDays >= Math.round(iterator.getDurationDays() + (isMilestone ? 1d : 0d))) {
            return true;
         }
         return false;
      }
   }
   
   private static class MoveActivityDateCallback implements ActivityIterationCallback {
      int days = 0;
      private Date finishDate = null;
      public MoveActivityDateCallback(int days) {
         this.days = days;
      }
      
      public boolean isFinished(ActivityIterator iterator, Date date,
            boolean workDay, boolean forward) {
         if (days == 0) {
            finishDate = date;
            return true;
         }
         return false;
      }

      public void iteration(ActivityIterator iterator, Date date,
            boolean workDay, boolean breakDay, boolean forward) {
         days -= workDay ? 1 : 0;         
      }

      public Date getFinishDate() {
         return finishDate;
      }
      
   }
   
   
   private void applyApplyDurationResults(XComponent dataRow,
         ApplyDurationCallback callback) {
      // costs where calculated on the way...
      if (isOfType(dataRow, APPLY_DURATION_TYPES)) {
         setDuration(dataRow, callback.getWorkDays() * getCalendar().getWorkHoursPerDay());
      }
      double newEffort = callback.getEffortAccumulated();
      adjustRemainingEffort(dataRow, newEffort - getBaseEffort(dataRow), isProgressTracked());
      setBaseEffort(dataRow, newEffort);
      
      if (isTemplate()) {
         return;
      }

      setBasePersonnelCosts(dataRow, callback.getPersonellCosts());
      setBaseProceeds(dataRow, callback.getProceeds());
      
      setResourceBaseEfforts(dataRow, callback.getResourceEffortsAccumulated());
      
      if (isOfType(dataRow, EFFORT_TYPES)) {
         setAttribute(dataRow,INCOMPLETE_ASSIGNMENT, callback.getAssignedEffortAccumulated() != callback.getEffortAccumulated());
      }
      else if (isOfType(dataRow, NO_EFFORT_WITH_ASSIGNMENT_TYPES)) {
         List assignments = getResources(dataRow);
         setAttribute(dataRow,INCOMPLETE_ASSIGNMENT, assignments == null || assignments.isEmpty());
      }
   }

   public static void adjustRemainingEffort(XComponent dataRow) {
      setRemainigEffort(dataRow, getBaseEffort(dataRow));
   }

   public static void adjustRemainingEffort(XComponent dataRow, double baseDelta, boolean progressTracked) {
      if (progressTracked) {
         Iterator rit = getResources(dataRow).iterator();
         while (rit.hasNext() && baseDelta != 0d) {
            String r = (String) rit.next();
            if (hasWorkRecords(dataRow, XValidator.choiceID(r))) {
               baseDelta = 0d;
            }
         }
      }
      setRemainigEffort(dataRow, getRemainingEffort(dataRow) + baseDelta);
   }

   private ApplyDurationCallback executeApplyDuration(XComponent dataRow,
         boolean forward) {
      resetWorkPhases(dataRow);
      ApplyDurationCallback callback = new ApplyDurationCallback(this, dataRow);
      ActivityIterator it = createActivityIterator(dataRow, forward, callback);
      try {
         setError(dataRow, NO_ERROR);
         it.run();
      }
      catch (IterationException ix) {
         setError(dataRow, ix.getErrorCode());
         return null; // no callback -> no luck...
      }
      return callback;
   }
   
   /**
    * Adjust finsh or start date to reflect duration and assignments of activity.
    * Fixed dates may be ignored to enable project shifts or templates
    * @param dataRow the affected activity
    * @param forward direction
    * @param ignoreFixed do not honor fixed attributes
    * @return
    */
   public boolean adjustActivityToResourcesAndDuration(XComponent dataRow, boolean forward, boolean ignoreFixed) {
      if (importedActivity(dataRow) || !isOfType(dataRow, APPLY_DURATION_TYPES)) {
         return false;
      }

      if (!ignoreFixed) {
         // fix direction based on fixed start or finish dates
         // (fixed dates should be ignored if project plans are shifted)
         forward = getAttribute(dataRow, START_IS_FIXED) ? true : getAttribute(dataRow, FINISH_IS_FIXED) ? false : forward;
      }      

      ApplyDurationCallback callback = executeApplyDuration(dataRow, forward);
      if (callback == null) {
         return false;
      }
      
      // TODO: check
      // adjust latest day from iteration (NOTE: the day imidiately before or 
      // after the activity is always visited with 'workday' set to false to as a sentinel)
      Date newStartDate = null;
      Date newFinishDate = null;
      if (forward) {
         newStartDate = callback.getFirstActivityDayVisited();
         newFinishDate = callback.getLastActivityDayVisited();
      }
      else {
         newStartDate = callback.getLastActivityDayVisited();
         newFinishDate = callback.getFirstActivityDayVisited();
      }
      
      if (newStartDate == null || newFinishDate == null) {
         setError(dataRow, NO_WORKDAYS_IN_DURATION); // no workdays were hit during iteration...
         return false;
      }
      // Note: if the finsh date was changed using update finish, the duration should already represent
      // the correct duration for the activity, so this should not change anything. Otherwise this sets
      // the finish date to a value reflecting the duration of the activity
      if (!getEnd(dataRow).equals(newFinishDate)) {
         if (getAttribute(dataRow, FINISH_IS_FIXED) && !ignoreFixed) {
            // this might happen, if either the duration requires a change of the end date (forward)
            // or the finish date was not placed on a workday for the resources assigned (backward)
            setError(dataRow, DURATION_EXCEEDED);
            return false;
         }
         setEnd(dataRow, newFinishDate);
      }
      if (!getStart(dataRow).equals(newStartDate)) {
         if (getAttribute(dataRow, START_IS_FIXED) && !ignoreFixed) {
            setError(dataRow, DURATION_EXCEEDED);
            return false;
         }
         setStart(dataRow, newStartDate);
      }
      applyApplyDurationResults(dataRow, callback);
      return true;
   }

   /**
    * adjust the assignments and calculates the workphases for tasks:
    * @param task
    * @return
    */
   private boolean adjustTaskActivity(XComponent task) {
      if (importedActivity(task) || !isOfType(task, TASK_TYPES)) {
         return false;
      }
      setError(task, NO_ERROR); // reset...
      // first, find the number of working days:
      //get rate for each activity day and multiply it by the effortPerDay
      XComponent limitingActivity = getActivityDeterminingStartFinish(task);
      CountWorkDaysCallback countCB = new CountWorkDaysCallback();
      ActivityIterator wdIt = null;
      if (limitingActivity != null) {
         wdIt = createActivityIterator(OpGanttValidator
               .getStart(limitingActivity), OpGanttValidator
               .getEnd(limitingActivity), OpGanttValidator
               .getWorkBreaks(limitingActivity), null, 0d, OpGanttValidator
               .getResources(task), true, countCB);
      }
      else {
         wdIt = createActivityIterator(getProjectStart(), getProjectFinish(),
               null, null, 0d, OpGanttValidator.getResources(task), true, countCB);
      }
      try {
         wdIt.run();
      }
      catch (IterationException ix) {
         setError(task, ix.getErrorCode()); // reset...
         return false;
      }
      double durationDays = countCB.getWorkDays();

      if (durationDays == 0d) {
         setError(task, NO_WORKDAYS_IN_DURATION); // no workdays were hit during iteration...
         return false;
      }

      // the tricky part: fake those assignments:
      Iterator rit = OpGanttValidator.getResources(task).iterator();
      List fakeAssignments = setupResources(durationDays
            * getCalendar().getWorkHoursPerDay(), getBaseEffort(task),
            getResources(task), ASSIGNMENT_ADJUSTMENT_MAX_FILL, !isEffortBasedProject());
      setResources(task, fakeAssignments);

      ApplyDurationCallback durCB = new ApplyDurationCallback(this, task, durationDays);
      ActivityIterator durIt = null;
      if (limitingActivity != null) {
         durIt = createActivityIterator(OpGanttValidator
               .getStart(limitingActivity), OpGanttValidator
               .getEnd(limitingActivity), OpGanttValidator
               .getWorkBreaks(limitingActivity), null, durationDays, getResources(task),
               true, durCB);
      }
      else {
         durIt = createActivityIterator(getProjectStart(), getProjectFinish(),
               null, null, durationDays, fakeAssignments, true, durCB);
      }
      try {
         durIt.run();
      }
      catch (IterationException ix) {
         setError(task, ix.getErrorCode()); // reset...
         return false;
      }
      applyApplyDurationResults(task, durCB);
      return true;
   }
   
   /**
    * Tries to update the duration for a given activity. This kind of update is not possible for collections.
    *
    * @param dataRow -
    *                 the data representing the activity
    * @param duration -
    *                 the desired new duration
    * @return true if the duration can be updated / false otherwise
    */
   public boolean updateDuration(XComponent dataRow, double duration, boolean forward) {

      // *** TODO: Return false if it is not allowed to change the duration
      // ==> Check activity mode/flags (note: Needs another column in data-set)
      // *** Loop duration (count down) until "exceeded"; calculcate i-efforts
      // ==> Add efforts only if no resource is absent (if currently no work-break)
      if (importedActivity(dataRow)) {
         return false;
      }
      
      if (OpGanttValidator.getStart(dataRow) == null || OpGanttValidator.getEnd(dataRow) == null) {
         return false;
      }
      if (OpGanttValidator.getType(dataRow) == MILESTONE && duration > 0) {
         updateType(dataRow, STANDARD);
      }
      // duration can't be <0
      if (duration <= 0) {
         duration = 0;
         updateType(dataRow, MILESTONE);
      }

      // fix direction based on fixed start or finish dates:
      if (getAttribute(dataRow, START_IS_FIXED)) {
         forward = true;
      }
      else if (getAttribute(dataRow, FINISH_IS_FIXED)) {
         forward = false;
      }
      
      double previousDuration = getDuration(dataRow);
      setDuration(dataRow, duration);
      
      // if the duration is changed, the assignments are not adjusted
      List resources = getResources(dataRow);
      resources = setupResources(duration, getBaseEffort(dataRow),
            resources, getResourceAssignmentRule(dataRow), !isEffortBasedProject());
      setResources(dataRow, resources);
 
      // apply the calculated duration and adjust start or finish date
      adjustActivityToResourcesAndDuration(dataRow, forward, false);
      
      //update visual resources
      updateResourceVisualization(dataRow);
      return true;
   }

   private Map getNextWorkBreak(boolean forward, SortedMap workBreaks,
         Iterator workBreakIterator, double currentDayNumber, Map nextWorkBreak) {
      if (nextWorkBreak != null) {
         double wbStart = ((Double) nextWorkBreak.get(WORK_BREAK_START)).doubleValue();
         double wbDuration = ((Double) nextWorkBreak.get(WORK_BREAK_DURATION)).doubleValue();
         
         if (forward && currentDayNumber > wbStart + wbDuration || !forward && currentDayNumber < wbStart) {
            nextWorkBreak = null;
         }
      }
      else {
         while (workBreakIterator.hasNext() && nextWorkBreak == null) {
            Map wb = (Map) workBreaks.get(workBreakIterator.next());
            double wbStart = ((Double) wb.get(WORK_BREAK_START)).doubleValue();
            double wbDuration = ((Double) wb.get(WORK_BREAK_DURATION)).doubleValue();
            
            if (forward && currentDayNumber <= wbStart + wbDuration || !forward && currentDayNumber >= wbStart) {
               nextWorkBreak = wb;
            }
         }
      }
      return nextWorkBreak;
   }

   /**
    * @author peter
    * callback "interface" for iterating over activities
    */
   public interface ActivityIterationCallback {
      
      // called for each iteration step (NOTE: the day imidiately before and 
      // after the activity is always visited with 'workday' set to false as a sentinel.
      // this gives the derived class a chance to setup or close any iteration artefacts)
      public void iteration(ActivityIterator iterator, Date date, boolean workDay, boolean breakDay, boolean forward);
      
      public boolean isFinished(ActivityIterator iterator, Date date, boolean workDay, boolean forward);
   }
   
   public static class IterationException extends XValidationException {

      int errorCode = OpGanttValidator.NO_ERROR;
      
      public IterationException(int errorCode) {
         super(OpGanttValidator.ITERATION_EXCEPTION);
         this.errorCode = errorCode;
      }
      
      int getErrorCode() {
         return errorCode;
      }
      
   }
   
   /**
    * @author peter
    * little helper to ease the pain of iterating over activities...
    */
   public static class ActivityIterator {
      
      public static final double INVALID_NUMBER = -1d;
      
      protected static Calendar helper = Calendar.getInstance();
      
      protected OpGanttValidator validator = null;
      protected boolean forward = true;
      // activtiy:
      protected Date startDate = null;
      protected Date finishDate = null;
      protected double durationDays = 0d;
      
      // iteration:
      protected Date currentDate = null;
      protected Date endDate = null;
      protected double relativeWorkDay = 0d;
      protected ActivityIterationCallback executor = null;
      
      public OpGanttValidator getValidator() {
         return validator;
      }

      public void setCurrentDate(Date currentDate) {
         this.currentDate = currentDate;
      }

      public Date getEndDate() {
         return endDate;
      }

      public Date getStartDate() {
         return startDate;
      }

      public Date getFinishDate() {
         return finishDate;
      }

      public double getDurationDays() {
         return durationDays;
      }

      public double getRelativeWorkDay() {
         return relativeWorkDay;
      }

      public ActivityIterator(OpGanttValidator validator, XComponent dataRow, boolean forward, ActivityIterationCallback executor) {
         this(validator, OpGanttValidator.getStart(dataRow), OpGanttValidator
               .getEnd(dataRow), validator.getDurationDays(dataRow), forward,
               executor);
      }
      
      public ActivityIterator(OpGanttValidator validator, Date startDate,
            Date finishDate, double durationDays, boolean forward,
            ActivityIterationCallback executor) {
         this.validator = validator;
         this.forward = forward;
         // start date is moved to project-start if null:
         this.startDate = startDate != null ? startDate : validator.getProjectStart();
         // finish date moves to project end, or if null as well to start date, creating a one-day activity...
         this.finishDate = finishDate != null ? finishDate : validator.getProjectFinish();
         if (this.finishDate == null) {
            // still not set, use today + one year by default...
            Calendar c = validator.getCalendar().cloneCalendarInstance();
            c.setTimeInMillis(this.startDate.getTime());
            c.add(Calendar.MONTH, 1);
            this.finishDate = new Date(c.getTimeInMillis());
         }
         this.currentDate = forward ? this.startDate : this.finishDate;
         this.endDate = forward ? this.finishDate : this.startDate;
         this.durationDays = (startDate == null || finishDate == null) ? INVALID_NUMBER : durationDays;
         this.relativeWorkDay = forward ? 0d : durationDays;
         this.executor = executor;
      }
      
      public void run() {
         int workDays = 0;

         double breakDay = 0d; // used as counter for days belonging to a work break
         boolean workDay = false;
         
         currentDate = new Date(currentDate.getTime() - (forward ? XCalendar.MILLIS_PER_DAY : -XCalendar.MILLIS_PER_DAY));
         executor.iteration(this, currentDate, workDay, false, forward);
         
         currentDate = new Date(currentDate.getTime() + (forward ? XCalendar.MILLIS_PER_DAY : -XCalendar.MILLIS_PER_DAY));
         while (!executor.isFinished(this, currentDate, workDay, forward)) {
            workDay = validator.getCalendar().isWorkDay(currentDate);
            // call derived:
            executor.iteration(this, currentDate, workDay, breakDay > 0d, forward);
            
            // finished?
            if (workDay) {
               relativeWorkDay += forward ? 1 : -1;
            }
            currentDate = new Date(currentDate.getTime() + (forward ? XCalendar.MILLIS_PER_DAY : -XCalendar.MILLIS_PER_DAY));
         }
         // one none-working day at after the activity ends...
         workDay = false;
         executor.iteration(this, currentDate, workDay, breakDay > 0d, forward);
      }
   }
   
   private static class CountWorkDaysCallback implements ActivityIterationCallback {

      private double workDays = 0d;
      private Date firstWorkDay = null;
      private Date lastWorkDay = null;

      public double getWorkDays() {
         return workDays;
      }

      
      public Date getFirstWorkDay() {
         return firstWorkDay;
      }


      public Date getLastWorkDay() {
         return lastWorkDay;
      }


      public void iteration(ActivityIterator iterator, Date date,
            boolean workDay, boolean breakDay, boolean forward) {
         if (workDay) {
            workDays += 1d;
            firstWorkDay = firstWorkDay == null ? date : firstWorkDay;
            lastWorkDay = date;
         }
      }

      public boolean isFinished(ActivityIterator iterator, Date date,
            boolean workDay, boolean forward) {
         return iterator.getEndDate() == null || date.after(iterator.getEndDate());
      }
   }
   
   public ActivityIterator createActivityIterator(XComponent dataRow, boolean forward, ActivityIterationCallback exec) {
      return new ActivityIterator(this, dataRow, forward, exec);
   }
   
   public ActivityIterator createActivityIterator(Date startDate, Date finishDate, SortedMap workBreaks, SortedMap workPhases, double durationDays, List assignments, boolean forward, ActivityIterationCallback exec) {
      return new ActivityIterator(this, startDate, finishDate, durationDays, forward, exec);
   }
   
   public boolean shouldHaveStartFinishDate(XComponent dataRow) {
      return isOfType(dataRow, HAS_START_FINISH_TYPES);
   }
   /**
    * Updates the finish date for an activity. If the finish date is smaller that the start date the activity will have
    * the duration set to zero. This kind of update is not possible for collections.
    *
    * @param dataRow -
    *                 tha data representing the activity
    * @param finish   -
    *                 the desired new value of the finish date
    * @return true if the value can be updated/ false otherwise
    */
   public boolean updateFinish(XComponent dataRow, Date finish) {

      if (finish != null) {
         if (!shouldHaveStartFinishDate(dataRow)) {
            return false;
         }
         
         setEnd(dataRow, finish);
         
         CountWorkDaysCallback callback = executeCountWorkDays(dataRow);
         if (callback == null) {
            return false;
         }

         double duration = callback.getWorkDays() * getCalendar().getWorkHoursPerDay();
         setDuration(dataRow, duration);

         adjustTypeForActivity(dataRow);
         
         if (isOfType(dataRow, START_FINISH_MUST_BE_WORKDAY_TYPES)) {
            // we cannot tell when to start or end collections here, so do not change those dates...
            Date firstWorkDay = callback.getFirstWorkDay();
            Date lastWorkDay = callback.getLastWorkDay();
            setStart(dataRow, firstWorkDay);
            setEnd(dataRow, lastWorkDay);

            // TODO: leave it to validate???
            updateDuration(dataRow, duration, true);
         }
      }
      else {
         setEnd(dataRow, finish);
      }
      return true;
   }

   private CountWorkDaysCallback executeCountWorkDays(XComponent dataRow) {
      resetWorkPhases(dataRow);
      CountWorkDaysCallback callback = new CountWorkDaysCallback();
      ActivityIterator it = createActivityIterator(dataRow, true, callback);
      
      try {
         setError(dataRow, NO_ERROR);
         it.run();
      }
      catch (IterationException ix) {
         setError(dataRow, ix.getErrorCode());
         return null;
      }
      return callback;
   }

   /**
    * Determines the remaining effort for an activity or assignment based on the other efforts.
    *
    * @param baseEffort   a <code>double</code> representing the base effort.
    * @param actualEffort a <code>double</code> representing the actual effort.
    * @param complete     a <code>double</code> representing the % complete.
    * @return a <code>double</code> representing the remainig effort for the collection.
    */
   public static double calculateRemainingEffort(double baseEffort, double actualEffort, double complete) {
      if (complete == 0) {
         return baseEffort;
      }
      else if (actualEffort == 0) {
         return baseEffort - (baseEffort * complete) / 100;
      }
      else {
         return complete == 100 ? 0d : actualEffort * 100 / complete - actualEffort;
      }
   }
   
   public static double calculateActualEffort(double baseEffort, double remainingEffort, double complete) {
      double actual = 0;
      if (complete == 0) {
         actual = 0;
      }
      if (remainingEffort == 0) {
         actual = baseEffort * complete / 100;
      }
      else {
         // to avoid division by zero here ;-)
         actual = complete == 100 ? baseEffort : remainingEffort * 100 / (100 - complete) - remainingEffort; 
      }
      return actual;
   }
   
   /**
    * Computes the %complete value of an activity (standard or collection) based on the given parameters.
    *
    * @param actualSum    a <code>double</code> representing a sum of actual efforts.
    * @param baseSum      a <code>double</code> representing a sum of base efforts.
    * @param remainingSum a <code>double</code> representing a sum of remaining efforts.
    */
   public static double calculateCompleteValue(double actualSum,
         double baseSum, double remainingSum) {
      double result = 0;
      double predictedSum = actualSum + remainingSum;
      if (predictedSum == 0.0) {
         // TODO: check
         result = 0;
      }
      else {
         result = remainingSum == 0d ? 100 : actualSum / predictedSum * 100;
      }
      
      return result;
   }

   /**
    * Moves an activity in a target collection.
    *
    * @param rows               data rows to be moved
    * @param offset             offset for move action
    * @param targetDataRow      future parent activity
    * @param targetOutlineLevel collection outline level
    */
   public void moveInCollection(List rows, int offset, XComponent targetDataRow, int targetOutlineLevel) {
      if (importedActivity(targetDataRow)) {
         throw new XValidationException(PROGRAM_ELEMENT_MOVE_EXCEPTION);
      }

      addValidationStartPoint(targetDataRow);
      for (int i = 0; i < rows.size(); i++) {
         XComponent row = (XComponent) rows.get(i);
         addValidationStartPoint(row);
         XComponent collection = superActivity(row);
         if (collection != null) {
            addValidationStartPoint(collection);
         }
      }

      if (getType(targetDataRow) != MILESTONE) {
         XComponent sourceDataRow = (XComponent) rows.get(0);
         targetOutlineLevel++;
         int outline;
         outline = sourceDataRow.getOutlineLevel();
         int diff = targetOutlineLevel - outline;
         if (offset != 0) {
            for (int i = 0; i < rows.size(); i++) {
               XComponent activity = (XComponent) rows.get(i);
               activity.setOutlineLevel(activity.getOutlineLevel() + diff);
            }
            try {
               moveDataRows(rows, offset);
            }
            catch (XValidationException e) {
               for (int i = 0; i < rows.size(); i++) {
                  XComponent activity = (XComponent) rows.get(i);
                  activity.setOutlineLevel(activity.getOutlineLevel() - diff);
               }
               throw e;
            }
         }
         else {
            changeOutlineLevels(rows, targetOutlineLevel - outline);
         }
      }
      else {
         throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
      }
   }

   /**
    * Moves the given activities with the given offset. Will try to change also the outline level of the rows if move is
    * not possible with the actual outline level.
    *
    * @param rows               rows to be moved
    * @param offset             offset for move action
    * @param targetOutlineLevel outline level of the target position
    */
   public void moveOverActivities(List rows, int offset, int targetOutlineLevel) {

      if (offset == 0) {
         return;
      }

      for (int i = 0; i < rows.size(); i++) {
         XComponent row = (XComponent) rows.get(i);
         addValidationStartPoint(row);
         XComponent collection = superActivity(row);
         if (collection != null) {
            addValidationStartPoint(collection);
         }
      }

      XComponent sourceDataRow = (XComponent) rows.get(0);
      int outline;
      outline = sourceDataRow.getOutlineLevel();
      boolean changeOutline = false;

      int outlineLevel = sourceDataRow.getOutlineLevel();
      if (outlineLevel != 0 && sourceDataRow.getIndex() + offset == 0) {
         changeOutline = true;
      }
      if (sourceDataRow.getIndex() + offset != 0) {
         int previousIndex = sourceDataRow.getIndex() + offset - 1;
         if (previousIndex >= data_set.getChildCount()) {
            previousIndex = data_set.getChildCount() - 1;
         }
         XComponent previous = (XComponent) data_set.getChild(previousIndex);
         if (Math.abs(outlineLevel - previous.getOutlineLevel()) > 1) {
            changeOutline = true;
         }
      }

      //try to move the rows with the existing outline level
      if (!changeOutline) {
         try {
            moveDataRows(rows, offset);
         }
         catch (XValidationException e) {
            changeOutline = true;
         }
      }

      int diff = targetOutlineLevel - outline;
      if (changeOutline) {
         //it is not possible with the current outline level, change outline level
         for (int i = 0; i < rows.size(); i++) {
            XComponent activity = (XComponent) rows.get(i);
            activity.setOutlineLevel(activity.getOutlineLevel() + diff);
         }
         try {
            moveDataRows(rows, offset);
         }
         catch (XValidationException e) {
            //rollback
            for (int i = 0; i < rows.size(); i++) {
               XComponent activity = (XComponent) rows.get(i);
               activity.setOutlineLevel(activity.getOutlineLevel() - diff);
            }
            throw e;
         }
      }
   }

   /**
    * Sets the continuous action flag for undo recording. When set to true the curent state of the data will be added to
    * the undo stack and nothing will be added untill the flag has been set to false.
    *
    * @param continuousAction true - stop recording undo actions / false - enable recording again
    */
   public void setContinuousAction(boolean continuousAction) {
      if (continuousAction) {
         addToUndo();
      }
      this.continuousAction = continuousAction;
   }

   /**
    * Adds a new action to the undo stack
    */
   protected void addToUndo() {
      if (continuousAction) {
         return;
      }

      if (undo == null) {
         undo = new ArrayList();
      }

      addToStack(data_set, undo);
      enableUndo(true);
      if (redo == null) {
         redo = new ArrayList();
      }
      redo.clear();
      enableRedo(false);
   }

   /**
    * Undo action on data set
    */
   public void undo() {
      if (undo != null && undo.size() > 0) {
         
         // redo only, if undo tracking is enabled...
         // FIXME: this is for the OPP-456, OPP-459 workaround
         if (!continuousAction) {
            if (redo == null) {
               redo = new ArrayList();
            }
            addToStack(data_set, redo);
            enableRedo(true);
         }
         // /FIXME

         data_set.removeAllChildren();
         List dataList = (List) undo.remove(undo.size() - 1);
         XComponent[] children = new XComponent[0];
         children = (XComponent[]) dataList.toArray(children);
         data_set.addAllChildren(children);
         if (undo.size() == 0) {
            enableUndo(false);
         }
      }
      else {
         enableUndo(false);
      }
   }

   /**
    * Redo action on data set
    */
   public void redo() {
      if (redo != null && redo.size() > 0) {
         addToStack(data_set, undo);
         enableUndo(true);
         data_set.removeAllChildren();
         List dataList = (List) redo.remove(redo.size() - 1);
         XComponent[] children = new XComponent[0];
         children = (XComponent[]) dataList.toArray(children);
         data_set.addAllChildren(children);
         if (redo.size() == 0) {
            enableRedo(false);
         }
      }
      else {
         enableRedo(false);
      }
   }


   private void addToStack(XComponent data_set, List undo) {
      List rows = new ArrayList();
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent row = (XComponent) data_set.getChild(i);
         rows.add(row);
      }
      if (undo.size() >= MAX_UNDO) {
         undo.remove(0);
      }
      undo.add(copyRows(rows, false));
   }

   private void enableUndo(boolean enable) {
      data_set.sendUndoEnableEvent(enable);
   }

   private void enableRedo(boolean enable) {
      data_set.sendRedoEnableEvent(enable);
   }
   
   public void updateResourceVisualization(XComponent dataRow) {
      Map resCals = new HashMap();
      Iterator rit = getResources(dataRow).iterator();
      while (rit.hasNext()) {
         String resId = XValidator.choiceID((String) rit.next());
         resCals.put(resId, getCalendar(resId));
      }
      OpProjectCalendar pCal = getCalendar();
      OpGanttValidator.updateResourceVisualization(dataRow,
            isHourBasedResourceView(), getAvailabilityMap(), pCal, resCals);
   }
 
   /**
    * Updates the resource visualization based on the data row resources (converts the data resources into hour or percent view)
    *
    * @param dataRow
    * @param hourBasedView
    * @param resourceAvailability
    */
   public static void updateResourceVisualization(XComponent dataRow,
         boolean hourBasedView, Map resourceAvailability, OpProjectCalendar pCal, Map resCals) {
      List displayedResources;
      
      boolean hasPercentAssignments = isOfType(dataRow, ASSIGNMENT_HAS_PERCENTAGE);

      //resources given by getResources method will always be in % view.
      List resources = getResources(dataRow);

      if (hourBasedView) {
         List converted = new ArrayList();
         for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
            String resource = (String) iterator.next();
            String caption = XValidator.choiceCaption(resource);
            String id = XValidator.choiceID(resource);

            OpProjectCalendar rc = OpGanttValidator.getCalendar(id, OpProjectCalendar.getDefaultProjectCalendar(), pCal, resCals);
            double hoursAssigned = hoursAssigned(resource);
            double hoursAvailable = ((Double) resourceAvailability.get(id)).doubleValue() * rc.getWorkHoursPerDay();
            if (hoursAssigned == INVALID_ASSIGNMENT) {
               hoursAssigned = hoursAvailable;
            }
            double durationDays = OpGanttValidator.getDuration(dataRow)
                  / pCal.getWorkHoursPerDay();
            double hours = hoursAssigned * durationDays;
            String name = getResourceName(caption, "h");
            if (durationDays > 0) {
               String hoursString = pCal.localizedDoubleToString(hours);
               resource = XValidator.choice(id, name + " " + hoursString + "h");
            }
            else {
               resource = XValidator.choice(id, name);
            }
            converted.add(resource);
         }
         displayedResources = converted;
      }
      else {
         displayedResources = new ArrayList();
         for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
            String resource = (String) iterator.next();
            String id = XValidator.choiceID(resource);
            String caption = XValidator.choiceCaption(resource);
            String resourceName = getResourceName(caption, "h");

            OpProjectCalendar rc = OpGanttValidator.getCalendar(id, OpProjectCalendar.getDefaultProjectCalendar(), pCal, resCals);
            double hoursAssigned = hoursAssigned(resource);
            double percentAvailable = ((Double) resourceAvailability.get(id)).doubleValue() / 100d;
            double hoursAvailable = percentAvailable * rc.getWorkHoursPerDay();
            if (hoursAssigned == INVALID_ASSIGNMENT) {
               hoursAssigned = hoursAvailable;
            }
            double percentAssigned = hoursAssigned / rc.getWorkHoursPerDay();
            if (percentAssigned == percentAvailable) {
               displayedResources.add(XValidator.choice(id, resourceName));
            }
            else if (percentAssigned != INVALID_ASSIGNMENT && hasPercentAssignments) {
               String percentageString = pCal.localizedDoubleToString(percentAssigned * 100d);
               String resourceChoice = XValidator.choice(id, resourceName + " " + percentageString + "%");
               displayedResources.add(resourceChoice);
            }
            else {
               displayedResources.add(XValidator.choice(id, resourceName));
            }
         }
      }
      setVisualResources(dataRow, displayedResources);
   }

   protected void setResourcesForActivity(XComponent dataRow, List resources) {
      List addedResources = new ArrayList();
      for (int i = 0; i < resources.size(); i++) {
         String resource = (String) resources.get(i);
         String resourceId = choiceID(resource);
         if (addedResources.contains(resourceId)) {
            resources.set(i, null);
            continue;
         }
         addedResources.add(resourceId);
         String caption = XValidator.choiceCaption(resource);
         String resName = getResourceName(caption, null);
         if (resName.equals(NO_RESOURCE_NAME)) {
            resources.set(i, null);
         }
         else {
            resources.set(i, XValidator.choice(resourceId, resName));
         }
      }
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         String resource = (String) iterator.next();
         if (resource == null) {
            iterator.remove();
         }
      }
      setResources(dataRow, resources);
      setVisualResources(dataRow, resources);
   }

   private double getIndividualEffortPerDay(String resource) {
      return hoursAssignedOrAvailable(resource);
   }

   /**
    * Checks if the assignment set used by the validator, contains a resource with the given name.
    *
    * @param resourceName a <code>String</code> representing the name of a resource.
    * @return <code>true</code> if the assignment set contains a resource with the given
    *         name, false otherwise.
    */
   private boolean checkResourceAssignment(String resourceName) {
      if (resourceName == null) {
         return false;
      }
      XComponent assigmentSet = this.getAssignmentSet();
      for (int i = 0; i < assigmentSet.getChildCount(); i++) {
         XComponent assignmentRow = (XComponent) assigmentSet.getChild(i);
         String assignedResourceName = choiceCaption(assignmentRow.getStringValue());
         if (assignedResourceName.equalsIgnoreCase(resourceName)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Parses a list of resources coming from the "outside", removing the unplanned one and
    * making sure that the remaining ones are found amongst the assigned resources.
    *
    * @param visualResources a <code>List(String)</code> resource choices.
    * @throws XValidationException if a resource is not found in the assignment set.
    */
   protected void adjustResourcesVisualization(List visualResources) {
      for (Iterator it = visualResources.iterator(); it.hasNext();) {
         String resourceChoice = (String) it.next();
         if (resourceChoice == null) {
            throw new XValidationException(RESOURCE_NAME_EXCEPTION);
         }
         String resourceCaption = choiceCaption(resourceChoice);
         String resourceName = resourceCaption != null ? getResourceName(resourceCaption, null) : getResourceName(resourceChoice, null);
         //remove un-named resources
         if (resourceName.equalsIgnoreCase(NO_RESOURCE_NAME)) {
            it.remove();
            continue;
         }
         if (!checkResourceAssignment(resourceName)) {
            throw new XValidationException(RESOURCE_NAME_EXCEPTION);
         }
      }
   }

   /**
    * Converts the assignments (hours or %) from a localized form (entered by the user) to an internal form.
    *
    * @param visualResources a <code>List</code> of <code>String</code> representing a list of visual resource.
    * @return an <code>List</code> of the same resources, but with the assignments strings locale independent.
    */
   protected List deLocalizeVisualResources(List visualResources) {
      List result = new ArrayList();
      for (Iterator it = visualResources.iterator(); it.hasNext();) {
         String visualResource = (String) it.next();
         String id = choiceID(visualResource);
         String caption = choiceCaption(visualResource);
         String resourceName = getResourceName(caption, null);
         String assignmentSymbol = null;
         String hoursString = getResourceName(caption, "h");
         if (hoursString.length() == resourceName.length()) {
            assignmentSymbol = "h";
         }
         else {
            assignmentSymbol = "%";
         }

         double assignment = localizedHoursAssigned(visualResource, getCalendar());
         if (assignment == INVALID_ASSIGNMENT) {
            assignment = localizedPercentageAssigned(visualResource, getCalendar());
         }
         String assignmentString = (assignment != INVALID_ASSIGNMENT) ? String.valueOf(assignment) : null;
         String delocalizedResource = null;
         if (assignmentString != null) {
            delocalizedResource = XValidator.choice(id, resourceName + " " + assignmentString + assignmentSymbol);
         }
         else {
            delocalizedResource = XValidator.choice(id, resourceName);
         }
         result.add(delocalizedResource);
      }
      return result;
   }

   /**
    * Creates a special kind of assignment, used for independent planning to plan remaining effort.
    *
    * @return a <code>XComponent</code> similar in structure to the resource assignment set entries.
    */
   private static XComponent createAnonymousResourceAssignmentDataRow() {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(XValidator.choice(NO_RESOURCE_ID, NO_RESOURCE_NAME));
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(NO_RESOURCE_AVAILABILITY);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell);
      return dataRow;
   }

   /**
    * Creates a copy of the assignment set, which contains the "no resource" assignment.
    *
    * @param originalAssignmentSet a <code>XComponent(DATA_SET)</code> representing the original assignment set.
    * @return a <code>XComponent(DATA_SET)</code> that is a copy of the original, plus the "no resource" assignment.
    */
   private XComponent createAssignmentSet(XComponent originalAssignmentSet) {
      XComponent result = new XComponent(XComponent.DATA_SET);
      for (int i = 0; i < originalAssignmentSet.getChildCount(); i++) {
         XComponent assignmentCopy = ((XComponent) originalAssignmentSet.getChild(i)).copyData();
         result.addChild(assignmentCopy);
      }
      result.addChild(createAnonymousResourceAssignmentDataRow());
      return result;
   }

   /**
    * determines the activity responsible for start and finish of the
    * activity in question.
    * 
    * @param activityRow
    * @return the row determinig start and finish for the activity (task) in question
    */
   private XComponent getActivityDeterminingStartFinish(XComponent activityRow) {
      if (getStart(activityRow) != null && getEnd(activityRow) != null) {
         return activityRow;
      }
      List parents = activityRow.getSuperRows();
      for (int i = 0; i < parents.size(); i++) {
         XComponent row = (XComponent) parents.get(i);
         if (getStart(row) != null && getEnd(row) != null) {
            return row;
         }
      }
      return null;
   }

   public void setProjectCost(Double cost) {
      projectCost = cost;
   }

   public void setProjectEffort(Double effort) {
      projectEffort = effort;
   }

   /**
    * @return The base cost associated with a project (Sum of all the lvl 0 activity costs)
    */
   public double getProjectCost() {
      if (projectCost == null) {
         double costs = 0;
         //calculate project costs from data set
         for (int i = 0; i < data_set.getChildCount(); i++) {
            XComponent row = (XComponent) data_set.getChild(i);
            if (row.getOutlineLevel() == 0) {
               costs += getBaseExternalCosts(row);
               costs += getBaseMaterialCosts(row);
               costs += getBaseMiscellaneousCosts(row);
               costs += getBasePersonnelCosts(row);
               costs += getBaseTravelCosts(row);
            }
         }
         projectCost = new Double(costs);
      }
      return projectCost.doubleValue();
   }

   /**
    * Sets the project proceeds value
    *
    * @param proceeds Project proceeds
    */
   public void setProceeds(Double proceeds) {
      projectProceeds = proceeds;
   }

   /**
    * @return The base proceeds associated with a project (Sum of all the lvl 0 activity proceeds)
    */
   public double getProceeds() {
      if (projectProceeds == null) {
         double proceeds = 0;
         //calculate project proceeds from data set
         for (int i = 0; i < data_set.getChildCount(); i++) {
            XComponent row = (XComponent) data_set.getChild(i);
            if (row.getOutlineLevel() == 0) {
               proceeds += getBaseProceeds(row);
            }
         }
         projectProceeds = new Double(proceeds);
      }
      return projectProceeds.doubleValue();
   }

   /**
    * @return The base effort associated with a project (Sum of all the lvl 0 activity efforts)
    */
   public double getProjectEffort() {
      if (projectEffort == null) {
         double effort = 0;
         //calculate project effort from data set
         for (int i = 0; i < data_set.getChildCount(); i++) {
            XComponent row = (XComponent) data_set.getChild(i);
            if (row.getOutlineLevel() == 0) {
               effort += getBaseEffort(row);
            }
         }
         projectEffort = new Double(effort);
      }
      return projectEffort.doubleValue();
   }

   /**
    * Gets the rate for a given resource.
    *
    * @param resourceLocator Resource locator for the resource is questions
    * @param rateIndex       type of rate to be retrieved
    * @param day             date to get the rate for
    * @return value of the needed rate
    */
   public double getRateForResource(String resourceLocator, int rateIndex, Date day) {
      XComponent hourlyRatesDataSet = getHourlyRatesDataSet();
      for (int i = 0; i < hourlyRatesDataSet.getChildCount(); i++) {
         XComponent resourceRow = (XComponent) hourlyRatesDataSet.getChild(i);
         Date previousDate = null;
         Date key;
         if (resourceLocator.equals(resourceRow.getStringValue())) {
            //map is sorted
            Map intervals = (Map) ((XComponent) resourceRow.getChild(0)).getValue();
            Date startDate = null;
            for (Iterator it = intervals.keySet().iterator(); it.hasNext();) {
               startDate = (Date) it.next();
               if (day.before(startDate)) {
                  break;
               }
               previousDate = startDate;
            }
            if (previousDate == null) {
               key = startDate;
            }
            else {
               key = previousDate;
            }
            List rateValues = (List) intervals.get(key);
            return ((Double) rateValues.get(rateIndex)).doubleValue();
         }
      }
      return 0;
   }

   /**
    * Gets the hourly rates data set associated with the current project.
    *
    * @return hourly rates data set component
    */
   public XComponent getHourlyRatesDataSet() {
      if (hourlyRatesDataSet == null) {
         XComponent form = data_set.getForm();
         if (form != null) {
            hourlyRatesDataSet = form.findComponent(RESOURCES_HOURLY_RATES_DATA_SET);
         }
      }
      return hourlyRatesDataSet;
   }

   /**
    * Sets the validator's hourly rate data set. Each row has the resource locator as value and a data cell containing
    * a Map of sorted dates as keys and hourly rates as values.
    * (10.01.07 -> [20, 30] ; 20.02.07 -> [25; 60] means that the resource has the internal HR 20/external HR 30 from
    * 10.01.07 to 19.02.07 and internal rate 25/external rate 60 from 20.02.07 on)
    *
    * @param dataSet Data set containing the hourly rates
    */
   public void setHourlyRatesDataSet(XComponent dataSet) {
      hourlyRatesDataSet = dataSet;
   }

   public static Date getLeadStart(XComponent dataRow, OpProjectCalendar calendar) {
      Date startDate = isOfType(dataRow, MILESTONE_TYPE) ? new Date(getStart(
            dataRow).getTime()
            + XCalendar.MILLIS_PER_DAY) : getStart(dataRow);
      return getLeadStart(startDate, getLeadTime(dataRow), calendar);
   }
   
   public static Date getFollowUpFinish(XComponent dataRow, OpProjectCalendar calendar) {
      return getFollowUpFinish(getEnd(dataRow), getFollowUpTime(dataRow), calendar);
   }
   
   public static Date getLeadStart(Date start, double leadTime, OpProjectCalendar calendar) {
      return calendar.nextWorkDay(start, - leadTime);
   }
   
   public static Date getFollowUpFinish(Date finsh, double followUpTime, OpProjectCalendar calendar) {
      return calendar.nextWorkDay(finsh, followUpTime);
   }
   
   public static double getLeadTimeAdjustment(Date activityLeadStart, Date parentLeadStart, OpProjectCalendar calendar) {
      double diff = 0d;
      if (parentLeadStart.after(activityLeadStart)) {
         double daysDiff = calendar.countWorkDaysBetween(activityLeadStart, parentLeadStart);
         diff = daysDiff;
      }
      return diff;
   }

   public static double getFollowUpTimeAdjustment(Date activityFollowUpFinish, Date parentFollowUpFinish, OpProjectCalendar calendar) {
      double diff = 0d;
      if (activityFollowUpFinish.after(parentFollowUpFinish)) {
         double daysDiff = calendar.countWorkDaysBetween(new Date(
               parentFollowUpFinish.getTime() + XCalendar.MILLIS_PER_DAY),
               new Date(activityFollowUpFinish.getTime()
                     + XCalendar.MILLIS_PER_DAY));
         diff = daysDiff;
      }
      return diff;
   }
   
   public static Date getConnectedStartTime(Date predFollowupFinish, double leadTime, OpProjectCalendar calendar) {
      return calendar.nextWorkDay(predFollowupFinish, leadTime);
   }
   
   public static Date getConnectedFinishTime(Date predLeadStart, double followUpTime, OpProjectCalendar calendar) {
      return calendar.nextWorkDay(predLeadStart, -followUpTime);
   }
   
   public static String createPredecessorVisualization(SortedMap predecessors) {
      StringBuffer b = new StringBuffer();
      Iterator pit = predecessors.keySet().iterator();
      while (pit.hasNext()) {
         Integer key = (Integer) pit.next();
         Map values = (Map) predecessors.get(key);
         int type = ((Integer)values.get(DEP_TYPE)).intValue();
         b.append(key.intValue() + 1);
         if (type != DEP_DEFAULT) {
            b.append(DEP_TYPE_MAP[type]);
         }
         if (pit.hasNext()) {
            b.append(PREDECESSOR_TABLE_DELIMITER);
         }
      }
      String s = b.toString();
      return s;
   }

   public static SortedMap addWorkBreak(SortedMap wbs, Map wbInfo) {
      SortedMap newWBs = new TreeMap();
      Iterator wbIt = wbs.keySet().iterator();
      //      if (!wbIt.hasNext()) {
      //         return newWBs;
      //      }
      wbInfo = normalizeWorkBreak(wbInfo);
      
      while (wbIt.hasNext()) {
         Map wb = (Map) wbs.get((Double) wbIt.next());
         if (workBreaksOverlap(wbInfo, wb)) {
            wbInfo = mergeWorkBreaks(wb, wbInfo);
         }
         else {
            newWBs.put(wb.get(WORK_BREAK_START), wb);
         }
      }
      // wbInfo either contain the new values or everything merged in here...
      if (isValidWorkBreak(wbInfo)) {
         newWBs.put(wbInfo.get(WORK_BREAK_START), wbInfo);
      }
      
      return newWBs;
   }

   public static Map normalizeWorkBreak(Map wbInfo) {
      double wbStart = ((Double) wbInfo.get(WORK_BREAK_START)).doubleValue();
      double wbDuration = ((Double) wbInfo.get(WORK_BREAK_DURATION)).doubleValue();
      Map newWB = new HashMap();
      newWB.put(WORK_BREAK_START, wbStart > 0d ? new Double(Math.ceil(wbStart)) : new Double(0d));
      newWB.put(WORK_BREAK_DURATION, wbDuration > 0d ? new Double(Math.ceil(wbDuration)) : new Double(0d));
      return newWB;
   }
   
   public static boolean isValidWorkBreak(Map wbInfo) {
      double wbStart = ((Double) wbInfo.get(WORK_BREAK_START)).doubleValue();
      double wbDuration = ((Double) wbInfo.get(WORK_BREAK_DURATION)).doubleValue();
      return wbDuration > 0 && wbStart >= 0;
   }
   
   public static boolean workBreaksOverlap(Map wb1, Map wb2) {
      boolean overlap = false;
      
      double wb1Start = ((Double) wb1.get(WORK_BREAK_START)).doubleValue();
      double wb1Duration = ((Double) wb1.get(WORK_BREAK_DURATION)).doubleValue();

      double wb2Start = ((Double) wb2.get(WORK_BREAK_START)).doubleValue();
      double wb2Duration = ((Double) wb2.get(WORK_BREAK_DURATION)).doubleValue();

      // overlap is somewhat wrong here, because - by definition - there is no overlap between 
      // work breaks. Th start of a work break is specified as the relative work break which - again
      // by definition - cannot be inside of any other work break. So no overlap...
      overlap = overlap || wb1Start == wb2Start; 
      
      //      overlap = overlap || wb1Start <= wb2Start && wb1Start + wb1Duration >= wb2Start; 
      //      overlap = overlap || wb2Start <= wb1Start && wb2Start + wb2Duration >= wb1Start;
      return overlap;
   }
   
   public static Map mergeWorkBreaks(Map wb1, Map wb2) {
      double wb1Start = ((Double) wb1.get(WORK_BREAK_START)).doubleValue();
      double wb1Duration = ((Double) wb1.get(WORK_BREAK_DURATION)).doubleValue();

      double wb2Start = ((Double) wb2.get(WORK_BREAK_START)).doubleValue();
      double wb2Duration = ((Double) wb2.get(WORK_BREAK_DURATION)).doubleValue();
      
      Map merged = new HashMap();
      //      boolean startAt1 = Double.compare(wb1Start, wb2Start) < 0;
      //      boolean endAt1 = Double.compare(wb1Start + wb1Duration, wb2Start + wb2Duration) > 0;
      //      double mergedStart = startAt1 ? wb1Start : wb2Start;
      //      double mergedEnd = (endAt1 ? wb1Start + wb1Duration : wb2Start + wb2Duration); 
      
      // due to the fact that only workbreaks starting at the same day needs to be merged
      // wb1Start and wb2Start are equal here
      double mergedStart = wb1Start; 
      double mergedEnd =  wb1Start + wb1Duration + wb2Duration;

      merged.put(WORK_BREAK_START, new Double(mergedStart));
      merged.put(WORK_BREAK_DURATION, new Double(mergedEnd - mergedStart));
      merged.put(WORK_BREAK_LOCATOR, wb1.get(WORK_BREAK_LOCATOR) != null ? wb1.get(WORK_BREAK_LOCATOR) : wb2.get(WORK_BREAK_LOCATOR));
      
      return merged;
   }
   
   public static boolean importedActivity(XComponent dataRow) {
      return getAttribute(dataRow, IMPORTED_FROM_SUBPROJECT);
   }
   
   public static boolean exportedActivity(XComponent dataRow) {
      return getAttribute(dataRow, EXPORTED_TO_SUPERPROJECT);
   }
   
   public static boolean importedHeadRow(XComponent dataRow) {
      return importedActivity(dataRow) && getSubProject(dataRow) != null;
   }
   
   private void setupValidationStartPoints() {
      if (validationAnchors == null) {
         validationAnchors = new HashSet();
      }
   }

   protected void clearValidationStartPoints() {
      if (validationAnchors != null) {
         validationAnchors.clear();
      }
   }

   protected void addValidationStartPoints(Collection rowsToAdd) {
      setupValidationStartPoints();
      validationAnchors.addAll(rowsToAdd);
   }

   protected void addValidationStartPoint(XComponent rowToAdd) {
      setupValidationStartPoints();
      validationAnchors.add(rowToAdd);
   }

   protected void removeValidationStartPoints(Collection rowsToRemove) {
      if (validationAnchors != null) {
         validationAnchors.removeAll(rowsToRemove);
      }
   }

   protected Set getValidationStartPoints() {
      if (validationAnchors == null) {
         return emptySet;
      }
      return validationAnchors;
   }

   /**
    * Retrieves for a given data row all the successors as seen by the graph.
    *
    * @param dataRow data row that is being queried
    * @return a list of <code>XComponent</code> with all the successors from the validation point of view
    */
//   protected Set getAllValidationSuccessors(XComponent dataRow) {
//      Set dataRowSuccessors = new HashSet();
//      getLinkedActivities(dataRow, dataRowSuccessors, false);
//      return dataRowSuccessors;
//   }

   /**
    * Updates the type of the activities from the given activity upwards (parent relation)
    *
    * @param activity a <code>XComponent(DATA_ROW)</code> representing a client activity.
    */
   protected void updateTreeType(XComponent activity) {
      if (activity == null) {
         return;
      }
      if (getType(activity) != adjustTypeForActivity(activity) || getValidationStartPoints().contains(activity)) {
         XComponent up = superActivity(activity);
         updateTreeType(up);
      }
   }

   /**
    * Updates the start/end of the collections starting from an initial activity.
    * start = the min of children starts
    * end = the max of children ends
    *
    * @param collection a <code>XComponent(DATA_ROW)</code> a collection activity.
    */
   protected void updateCollectionDates(XComponent collection, Map dateDelimitersMap) {
      if (collection == null) {
         return; // ?!?!
      }
      if (importedActivity(collection)) {
         return;
      }
      List children = subActivities(collection);
   
      Date minStart = null;
      Date minLeadStart = null;
      Date maxEnd = null;
      Date maxFollowUpFinish = null;
      boolean changed = false;
      setStart(collection, null);
      setEnd(collection, null);
      for (Iterator iterator = children.iterator(); iterator.hasNext();) {
         XComponent activity = (XComponent) iterator.next();
         minStart = getMinDate(getStart(activity), minStart);
         maxEnd = getMaxDate(getEnd(activity), maxEnd);
         
         Date updatedLeadStart = updateDelimiter(collection, activity, DEP_COLLECTION_START, dateDelimitersMap);
         minLeadStart = updatedLeadStart != null ? updatedLeadStart : minLeadStart;
         Date updatedFollowUpFinish = updateDelimiter(collection, activity, DEP_COLLECTION_END, dateDelimitersMap);
         maxFollowUpFinish = updatedFollowUpFinish != null ? updatedFollowUpFinish : maxFollowUpFinish;
      }
   
      if (getStart(collection) == null || (minStart != null && !getStart(collection).equals(minStart))) {
         setStart(collection, minStart);
         changed = true;
      }
   
      if (getEnd(collection) == null || (maxEnd != null && !getEnd(collection).equals(maxEnd))) {
         setEnd(collection, maxEnd);
         changed = true;
      }
   
      if (minLeadStart != null && !minLeadStart.equals(getLeadStart(collection, getCalendar()))) {
         changed = true;
         setLeadTime(collection, getLeadTimeAdjustment(minLeadStart, getStart(collection), getCalendar()));
      }
      if (maxFollowUpFinish != null && !maxFollowUpFinish.equals(getFollowUpFinish(collection, getCalendar()))) {
         changed = true;
         setFollowUpTime(collection, getFollowUpTimeAdjustment(maxFollowUpFinish, getEnd(collection), getCalendar()));
      }
      
      if (changed) {
         //update the duration
         updateFinish(collection, getEnd(collection));
         //update also the upper collections
         updateCollectionDates(superActivity(collection), dateDelimitersMap);
      }
   }

   protected Date getMinDate(Date d1, Date d2) {
      return d1 != null ? (d2 != null ? (d1.before(d2) ? d1 : d2) : d1) : d2;  
   }

   protected Date getMaxDate(Date d1, Date d2) {
      return d1 != null ? (d2 != null ? (d1.after(d2) ? d1 : d2) : d1) : d2;  
   }
   
   protected static class ActivityDelimiters {
      private Date currentStart = null;
      private Set startDelimiters = null;
      private Date currentFinish = null;
      private Set finishDelimiters = null;
      private OpGanttValidator validator = null;
      private XComponent candidate = null;
      
      public ActivityDelimiters(OpGanttValidator validator, XComponent dataRow) {
         this.validator = validator;
         this.candidate = dataRow;
         startDelimiters = new HashSet(); 
         finishDelimiters = new HashSet();
      }
      
      public Set getStartDelimiters() {
         return startDelimiters;
      }

      public Set getFinishDelimiters() {
         return finishDelimiters;
      }
      
      public void setCurrentStart(Date currentStart) {
         this.currentStart = currentStart;
      }

      public void setCurrentFinish(Date currentFinish) {
         this.currentFinish = currentFinish;
      }

      public Date getCurrentStart() {
         return currentStart;
      }

      public Date getCurrentFinish() {
         return currentFinish;
      }

      public XComponent getCandidate() {
         return candidate;
      }
      
   }
   
   protected void validateStartPoints(boolean validateAllStartPoints) {
      
      // set the calendar for the dataset to the project calendar...
      long now = System.currentTimeMillis();
      data_set.setComponentCalendar(getCalendar());
      logger.debug("Validating StartPoints -----------");
      //update the types upwards from the start points
      for (Iterator iterator = getValidationStartPoints().iterator(); iterator.hasNext();) {
         XComponent activity = (XComponent) iterator.next();
         updateTreeType(activity);
         adjustActivityToResourcesAndDuration(activity, true, false);
         adjustTaskActivity(activity);
      }
      logger.debug("TIMING: validateStartPoints #01: " + (System.currentTimeMillis() - now));
      //update collection
      for (Iterator iterator = getValidationStartPoints().iterator(); iterator.hasNext();) {
         updateCollectionTreeValues((XComponent) iterator.next());
      }
      logger.debug("TIMING: validateStartPoints #02: " + (System.currentTimeMillis() - now));

      OpGraph g = setupValidationGraph();
      List ordered = getTopologicallyOrderedActivities(g);
      logger.debug("TIMING: validateStartPoints #03: " + (System.currentTimeMillis() - now));
      //print(ordered);
      int modifiedActivities = 0;
      Set pathRoots = new HashSet();
      Map dateDelimitersMap = new HashMap();
      Date projectFinish = null;
      // subPathMap.put(new Integer(-1), startPath);
      for (Iterator iterator = ordered.iterator(); iterator.hasNext();) {
         Entry e = (Entry) iterator.next();
         if (validateActivity(e, dateDelimitersMap)) {
            modifiedActivities++;
         }
         projectFinish = updateCriticalPathRoots(e, pathRoots, projectFinish);
      }
      logger.debug("TIMING: validateStartPoints #04: " + (System.currentTimeMillis() - now) + " mod:" + modifiedActivities);
      // find longest path:
      markCriticalPath(pathRoots, g, dateDelimitersMap);
      
      logger.debug("TIMING: validateStartPoints #05: " + (System.currentTimeMillis() - now));
      updateProjectPlanFinish();
      logger.debug("TIMING: validateStartPoints #06: " + (System.currentTimeMillis() - now));
      clearValidationStartPoints();
   }

   protected Date updateCriticalPathRoots(Entry e, Set pathRoots,
         Date projectFinish) {
      return null;
   }

   protected void markCriticalPath(Set pathRoots, OpGraph g, Map dateDelimitersMap) {
   }

   /**
    * Returns a set with the independent activities from the underlying data-set.
    *
    * @return a <code>Set(XComponent(DATA_ROW))</code> representing independent
    *         activities.
    */
   protected Set getIndependentActivities() {
      Set result = new HashSet();
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent activity = (XComponent) data_set.getChild(i);
         if (isIndependentActivity(activity)) {
            result.add(activity);
         }
      }
      return result;
   }

   /**
    * Set start as max of all its predecessors (next work day after it) -
    * duration stays the same. If old values are the same as the new ones ->
    * stop validation.
    * @param superActivityMap 
    * 
    * @param dataRow -
    *           activity to validate
    */
   protected boolean validateActivity(Entry entry, Map dateDelimitersMap) {
      boolean touched = false;
      XComponent dataRow = (XComponent) entry.getElem();
      Iterator predsIt = entry.backEdgeIterator();
      
      boolean validationCollection = OpGanttValidator.isCollectionType(dataRow)
            && getType(dataRow) != SCHEDULED_TASK
            && getType(dataRow) != COLLECTION_TASK;
   
      if (validationCollection) {
         updateCollectionDates(dataRow, dateDelimitersMap);
      }
      
      boolean isMileStone = getType(dataRow) == MILESTONE;

      // first find out limits given by validation-predecessors:
      Date startDateLimit = null;
      while (predsIt.hasNext()) {
         Entry pred = (Entry) predsIt.next();
         XComponent predDataRow = (XComponent) pred.getElem();
         Date predEnd = OpGanttValidator.getEnd(predDataRow);
         Date updatedLimit = updateDelimiter(dataRow, predDataRow, DEP_END_START, dateDelimitersMap);
         startDateLimit = updatedLimit != null ? updatedLimit : startDateLimit;
      }

      // correct new dependent start:
      startDateLimit = (startDateLimit == null || getProjectStart() == null) ? getProjectStart()
            : (startDateLimit.before(getProjectStart()) ? getProjectStart()
                  : startDateLimit);
      if (!validationCollection) {
         boolean startFixed = OpGanttValidator.getAttribute(dataRow,
               OpGanttValidator.START_IS_FIXED);
         boolean finishFixed = OpGanttValidator.getAttribute(dataRow,
               OpGanttValidator.FINISH_IS_FIXED);
         if (startDateLimit != null) {
            // find the dependency:
            Date oldStart = OpGanttValidator.getStart(dataRow);
            
            Date newStart = startDateLimit;
            if (oldStart != null && !oldStart.equals(newStart)) {
               // move the activity
               if (!startFixed && !finishFixed && !importedActivity(dataRow)) {
                  touched = true;
                  OpGanttValidator.setStart(dataRow, newStart);
               }
            }
         }
         
         if ((startDateLimit != null) && touched) {
            adjustActivityToResourcesAndDuration(dataRow, true, false);
            updateCollectionTreeValues(dataRow);
         }
      }
   
      return touched;
   }

   protected Date updateDelimiter(XComponent dataRow, XComponent delimiter, int depType, Map dateDelimitersMap) {
//      if (getAttribute(dataRow, START_IS_FIXED) || getAttribute(dataRow, FINISH_IS_FIXED)) {
//         return null;
//      }
      ActivityDelimiters del = (ActivityDelimiters) dateDelimitersMap.get(dataRow);
      if (del == null) {
         del = new ActivityDelimiters(this, dataRow);
         dateDelimitersMap.put(dataRow, del);
      }
      Date newDel = null;
      switch (depType) {
      case DEP_END_START:
      case DEP_START_START:
      case DEP_COLLECTION_START:
         newDel = updateDateDelimiter(dataRow, delimiter, del.getCurrentStart(), del.getStartDelimiters(), depType);
         if (newDel != null) {
            del.setCurrentStart(newDel);
         }
         break;
      case DEP_START_END:
      case DEP_END_END:
      case DEP_COLLECTION_END:
         newDel = updateDateDelimiter(dataRow, delimiter, del.getCurrentFinish(), del.getFinishDelimiters(), depType);
         if (newDel != null) {
            del.setCurrentFinish(newDel);
         }
         break;
      }
      return newDel;
   }

   protected Entry getSuperActivity(Entry entry) {
      Iterator parents = entry.getEdges(EDGE_CLASS_HIERACHY).iterator();
      if (parents.hasNext()) {
         return (OpGraph.Entry)parents.next();
      }
      return null;
   }

   public void mergeInSubset(int offset, List subset, Map subsetIndexMap, boolean replace) {
   }
   
   protected Date updateDateDelimiter(XComponent dataRow, XComponent delimiter, Date currDelimitingDate, Set dateDelimiters, int depType) {
      boolean moveDependendDateForward = false;
      boolean dependsOnStart = false;
      boolean addToDelimiters = false;
      double moveEndDateByWorkdays = 0d;
      double moveDateByWorkdaysOfDelimiter = 0d;
      switch (depType) {
      case DEP_END_START:
         moveDependendDateForward = true;
         addToDelimiters = true;
         moveEndDateByWorkdays = (dataRow != null && isMilestone(dataRow)) ? 0d : 1d;
         break;
      case DEP_START_END:
         moveDateByWorkdaysOfDelimiter = isMilestone(delimiter) ? 0d : -1d;
         addToDelimiters = true;
         dependsOnStart = true;
         break;
      case DEP_START_START:
         moveDependendDateForward = true;
         addToDelimiters = true;
         dependsOnStart = true;
         break;
      case DEP_END_END:
         // moveDependendDateForward = true;
         // addToDelimiters = true;
         break;
      case DEP_COLLECTION_START:
         dependsOnStart = true;
         break;
      case DEP_COLLECTION_END:
         moveDependendDateForward = true;
         addToDelimiters = true;
         break;
      }
      Date entryDate = null;
      if (dependsOnStart && getStart(delimiter) != null) {
         //Date start, double leadTime, OpProjectCalendar calendar
         entryDate = new Date(OpGanttValidator.getLeadStart(getStart(delimiter), getLeadTime(delimiter),
               getCalendar()).getTime());
      }
      else if (getEnd(delimiter) != null) {
         // somewhat starnage: we do not need to find the next workday, because ending between e.g. friday 
         // and saturday is perfectly ok. Only starting there will not work
         entryDate = OpGanttValidator.getFollowUpFinish(getEnd(delimiter), getFollowUpTime(delimiter) + moveEndDateByWorkdays,
               getCalendar());
      }
      // FIXME: OPP-1079... Base data defective???
      if (entryDate == null) {
         logger.error("Invalid Delimiter encountered: " + delimiter.getIndex()
               + ":" + delimiter.getStringValue() + "-" + getName(delimiter));
         return currDelimitingDate;
      }
      if (moveDateByWorkdaysOfDelimiter != 0d) {
         // milestones start date is NOT moved!
         entryDate = moveDateForActivity(entryDate, delimiter, moveDateByWorkdaysOfDelimiter);
      }
      boolean add = false;
      if (currDelimitingDate == null) {
         currDelimitingDate = entryDate;
         add = true;
      }
      else {
         if (moveDependendDateForward ? currDelimitingDate.before(entryDate) : currDelimitingDate.after(entryDate)) {
            currDelimitingDate = entryDate;
            dateDelimiters.clear();
            add = true;
         }
         else if (currDelimitingDate.equals(entryDate)) {
            currDelimitingDate = null; // do not return irrelevant date...
            add = true;
         }
         else {
            currDelimitingDate = null; // do not return irrelevant date...
         }
      }
      if (add && addToDelimiters) {
         dateDelimiters.add(delimiter);
      }
      return currDelimitingDate;
   }
   
   protected Date moveDateForActivity(Date startDateToMove, XComponent dataRow, double days) {
      if (days == 0d) {
         return startDateToMove;
      }
      resetWorkPhases(dataRow);
      boolean forward = days > 0d;
      MoveActivityDateCallback durCB = new MoveActivityDateCallback((int)(forward ? days : -days));
      // startDateToMove = new Date(startDateToMove.getTime() + (forward ? XCalendar.MILLIS_PER_DAY : - XCalendar.MILLIS_PER_DAY));
      ActivityIterator durIt = createActivityIterator(startDateToMove,
            startDateToMove, null, null, days, getResources(dataRow), forward,
            durCB);
      try {
         durIt.run();
      }
      catch (IterationException ix) {
         setError(dataRow, ix.getErrorCode()); // reset...
         return null;
      }
      return durCB.getFinishDate();
   }

   public static String getPredecessorTypeString(int type) {
      if (type < DEP_TYPE_MAP.length) {
         return DEP_TYPE_MAP[type];
      }
      return "";
   }
   
}