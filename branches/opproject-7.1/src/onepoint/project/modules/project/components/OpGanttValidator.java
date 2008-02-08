/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project.components;

import onepoint.express.*;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.util.XCalendar;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.ParseException;
import java.util.*;

public class OpGanttValidator extends XValidator {

   // TODO: Efforts of collection activities are to be handled differently
   // (Check for type COLLECTION in update finish, effort und duration)
   // *** Note: Also simplification possible for milestones
   // *** Note that we assume a certain number and order of columns

   // Component IDs of additional data holders
   public final static String ASSIGNMENT_SET = "AssignmentSet";
   public final static String PROJECT_START = "ProjectStartField";
   public final static String PROJECT_SETTINGS_DATA_SET = "ProjectSettingsDataSet";

   private final static String ABSENCES_START = "AbsencesStart";
   private final static String ABSENCES_SET = "AbsencesSet";
   private final static String PROJECT_FINISH = "ProjectFinishField";
   private final static String SHOW_RESOURCE_HOURS = "ShowResourceHours";
   private final static String RESOURCES_HOURLY_RATES_DATA_SET = "ResourcesHourlyRates";

   private static final XLog logger = XLogFactory.getClientLogger(OpGanttValidator.class);

   // Activity set column indexes (main data set)
   public final static int NAME_COLUMN_INDEX = 0;
   public final static int TYPE_COLUMN_INDEX = 1;
   public final static int CATEGORY_COLUMN_INDEX = 2;
   public final static int COMPLETE_COLUMN_INDEX = 3;
   public final static int START_COLUMN_INDEX = 4;
   public final static int END_COLUMN_INDEX = 5;
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
   private final static int WORK_PHASE_STARTS_COLUMN_INDEX = 19;
   private final static int WORK_PHASE_FINISHES_COLUMN_INDEX = 20;
   private final static int WORK_PHASE_BASE_EFFORTS_COLUMN_INDEX = 21;
   private final static int RESOURCE_BASE_EFFORTS_COLUMN_INDEX = 22;
   private final static int PRIORITY_COLUMN_INDEX = 23;
   private final static int WORKRECORDS_COLUMN_INDEX = 24;
   private final static int ACTUAL_EFFORT_COLUMN_INDEX = 25;
   public final static int VISUAL_RESOURCES_COLUMN_INDEX = 26;
   public final static int RESPONSIBLE_RESOURCE_COLUMN_INDEX = 27;
   public final static int PAYMENT_COLUMN_INDEX = 29;
   public final static int BASE_PROCEEDS_COLUMN_INDEX = 30;

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

   //The id of the no category
   public static final String NO_CATEGORY_ID = "-1";

   // Date sentinel (indicates the date was "loaded", but value was null)
   private final static Date DATE_SENTINEL = new Date(0);

   protected XCalendar calendar;
   // New two fields should never be accessed directly
   private HashMap absencesSet = null;
   private XComponent hourlyRatesDataSet = null;
   private Date projectStart;
   private Date projectWorkingStart;
   private Date projectFinish;
   private Date projectPlanFinish;
   private Boolean hourBasedResources;
   private Byte calculationMode;
   private Boolean progressTracked;
   private Date absencesStart;
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

   public final static double INVALID_ASSIGNMENT = -1;

   public final static String NO_RESOURCE_ID = "-1";
   public final static double NO_RESOURCE_AVAILABILITY = Double.MAX_VALUE;
   public final static String NO_RESOURCE_NAME = "?";

   private final static char HOURS_SYMBOL = 'h';
   private final static char PERCENT_SYMBOL = '%';

   /**
    * An error margin used in the calculations of the validator.
    */
   public final static double ERROR_MARGIN = 0.05d;

   private boolean cleanClipboard;
   private List undo;
   private boolean continuousAction;
   private List redo;
   public static final int MAX_UNDO = 5;

   public OpGanttValidator() {
      calendar = XCalendar.getDefaultCalendar();
   }

   public final XCalendar getCalendar() {
      return calendar;
   }

   // Helpers for easier accessing data-set values

   public static void setName(XComponent data_row, String name) {
      ((XComponent) (data_row.getChild(NAME_COLUMN_INDEX))).setStringValue(name);
   }

   public static String getName(XComponent data_row) {
      return ((XComponent) (data_row.getChild(NAME_COLUMN_INDEX))).getStringValue();
   }

   public static void setDescription(XComponent data_row, String desc) {
      ((XComponent) (data_row.getChild(DESCRIPTION_COLUMN_INDEX))).setStringValue(desc);
   }

   public static String getDescription(XComponent data_row) {
      return ((XComponent) (data_row.getChild(DESCRIPTION_COLUMN_INDEX))).getStringValue();
   }

   public static void setType(XComponent data_row, byte type) {
      ((XComponent) (data_row.getChild(TYPE_COLUMN_INDEX))).setByteValue(type);
   }

   public static byte getType(XComponent data_row) {
      return ((XComponent) (data_row.getChild(TYPE_COLUMN_INDEX))).getByteValue();
   }

   public static void setCategory(XComponent data_row, String category) {
      ((XComponent) (data_row.getChild(CATEGORY_COLUMN_INDEX))).setStringValue(category);
   }

   public static String getCategory(XComponent data_row) {
      return ((XComponent) (data_row.getChild(CATEGORY_COLUMN_INDEX))).getStringValue();
   }

   public static void setComplete(XComponent data_row, double complete) {
      ((XComponent) (data_row.getChild(COMPLETE_COLUMN_INDEX))).setDoubleValue(complete);
   }

   public static double getComplete(XComponent data_row) {
      return ((XComponent) (data_row.getChild(COMPLETE_COLUMN_INDEX))).getDoubleValue();
   }

   public static void setPriority(XComponent data_row, Byte priority) {
      ((XComponent) (data_row.getChild(PRIORITY_COLUMN_INDEX))).setValue(priority);
   }

   public static Byte getPriority(XComponent data_row) {
      return (Byte) ((XComponent) (data_row.getChild(PRIORITY_COLUMN_INDEX))).getValue();
   }

   public static Map getWorkRecords(XComponent data_row) {
      return (Map) ((XComponent) (data_row.getChild(WORKRECORDS_COLUMN_INDEX))).getValue();
   }

   public static void setWorkRecords(XComponent data_row, Map newValue) {
      ((XComponent) (data_row.getChild(WORKRECORDS_COLUMN_INDEX))).setValue(newValue);
   }

   public static String getResponsibleResource(XComponent data_row) {
      return (String) ((XComponent) (data_row.getChild(RESPONSIBLE_RESOURCE_COLUMN_INDEX))).getValue();
   }

   public static void setResponsibleResource(XComponent data_row, String newValue) {
      ((XComponent) (data_row.getChild(RESPONSIBLE_RESOURCE_COLUMN_INDEX))).setValue(newValue);
   }

   public static void setPayment(XComponent data_row, double payment) {
      ((XComponent) (data_row.getChild(PAYMENT_COLUMN_INDEX))).setDoubleValue(payment);
   }

   public static double getPayment(XComponent data_row) {
      return ((XComponent) (data_row.getChild(PAYMENT_COLUMN_INDEX))).getDoubleValue();
   }

   public static void setBaseProceeds(XComponent data_row, double proceeds) {
      ((XComponent) (data_row.getChild(BASE_PROCEEDS_COLUMN_INDEX))).setDoubleValue(proceeds);
   }

   public static double getBaseProceeds(XComponent data_row) {
      return ((XComponent) (data_row.getChild(BASE_PROCEEDS_COLUMN_INDEX))).getDoubleValue();
   }

   /**
    * Sets up the start date of an activity data row
    *
    * @param data_row the data row
    * @param start    the start date of the activity
    */
   public static void setStart(XComponent data_row, Date start) {
      ((XComponent) (data_row.getChild(START_COLUMN_INDEX))).setDateValue(start);
   }

   /**
    * Returns the start date of an activity data row
    *
    * @param data_row the data row
    * @return Date representing the start date of the activity
    */
   public static Date getStart(XComponent data_row) {
      return ((XComponent) (data_row.getChild(START_COLUMN_INDEX))).getDateValue();
   }

   /**
    * Sets up the end date of an activity data row
    *
    * @param data_row the data row
    * @param end      the end date of the activity
    */
   public static void setEnd(XComponent data_row, Date end) {
      ((XComponent) (data_row.getChild(END_COLUMN_INDEX))).setDateValue(end);
   }

   /**
    * Returns the end date of an activity data row
    *
    * @param data_row the data row
    * @return Date representing the end date of the activity
    */
   public static Date getEnd(XComponent data_row) {
      return ((XComponent) (data_row.getChild(END_COLUMN_INDEX))).getDateValue();
   }

   /**
    * Sets up the duration of an activity data row
    *
    * @param data_row the data row
    * @param duration the duration of the activity (working hours)
    */
   public static void setDuration(XComponent data_row, double duration) {
      ((XComponent) (data_row.getChild(DURATION_COLUMN_INDEX))).setDoubleValue(duration);
   }

   /**
    * Returns the duration of an activity data row
    *
    * @param data_row the data row
    * @return double representing the duration of the activity (in working hours)
    */
   public static double getDuration(XComponent data_row) {
      return ((XComponent) (data_row.getChild(DURATION_COLUMN_INDEX))).getDoubleValue();
   }

   /**
    * Sets up the base effort of an activity data row
    *
    * @param data_row   the data row
    * @param baseEffort the base effort of the activity
    */
   public static void setBaseEffort(XComponent data_row, double baseEffort) {
      ((XComponent) (data_row.getChild(BASE_EFFORT_COLUMN_INDEX))).setDoubleValue(baseEffort);
   }

   /**
    * Returns the base effort of an activity data row
    *
    * @param data_row the data row
    * @return double representing the base effort of the activity
    */
   public static double getBaseEffort(XComponent data_row) {
      return ((XComponent) (data_row.getChild(BASE_EFFORT_COLUMN_INDEX))).getDoubleValue();
   }

   /**
    * Returns the actual effort of an activity data row
    *
    * @param data_row the data row
    * @return double representing the actual effort of the activity
    */
   public static double getActualEffort(XComponent data_row) {
      return ((XComponent) (data_row.getChild(ACTUAL_EFFORT_COLUMN_INDEX))).getDoubleValue();
   }

   /**
    * Sets the actual effort of an activity data row
    *
    * @param data_row the data row
    * @return double representing the actual effort of the activity
    */
   public static void setActualEffort(XComponent data_row, double actualEffort) {
      ((XComponent) (data_row.getChild(ACTUAL_EFFORT_COLUMN_INDEX))).setDoubleValue(actualEffort);
   }

   /**
    * Sets up the predecessors of an activity data row
    *
    * @param data_row     the data row
    * @param predecessors an <code>XArray <Integer> </code> of predecessors
    */
   public static void setPredecessors(XComponent data_row, List predecessors) {
      ((XComponent) (data_row.getChild(PREDECESSORS_COLUMN_INDEX))).setListValue(predecessors);
   }

   /**
    * Returns the predecessors of an activity data row
    *
    * @param data_row the data row
    * @return an <code>XArray <Integer> </code> of predecessors
    */
   public static List getPredecessors(XComponent data_row) {
      return ((XComponent) (data_row.getChild(PREDECESSORS_COLUMN_INDEX))).getListValue();
   }

   /**
    * Sets up the succesors of an activity data row
    *
    * @param data_row   the data row
    * @param successors an <code>XArray <Integer> </code> of succesors
    */
   public static void setSuccessors(XComponent data_row, List successors) {
      ((XComponent) (data_row.getChild(SUCCESSORS_COLUMN_INDEX))).setListValue(successors);
   }

   /**
    * Returns the succesors of an activity data row (indexes)
    *
    * @param data_row the data row
    * @return a <code>List</code> of succesor indexes (<code>Integer</code>)from data set
    */
   public static List getSuccessors(XComponent data_row) {
      return ((XComponent) (data_row.getChild(SUCCESSORS_COLUMN_INDEX))).getListValue();
   }

   /**
    * Sets the resources for an activity data row
    *
    * @param data_row  the activity data row
    * @param resources an <code>XArray<String> </code>
    */
   public static void setResources(XComponent data_row, List resources) {
      ((XComponent) (data_row.getChild(RESOURCES_COLUMN_INDEX))).setListValue(resources);
   }

   /**
    * Sets an array of resource for an activity data row
    *
    * @param data_row the activity data row
    * @return an <code>XArray<String> </code> of resources
    */
   public static List getResources(XComponent data_row) {
      return ((XComponent) (data_row.getChild(RESOURCES_COLUMN_INDEX))).getListValue();
   }

   /**
    * Sets the visual resources for an activity data row [h or % view]
    *
    * @param data_row  the activity data row
    * @param resources an <code>XArray<String> </code>
    */
   public static void setVisualResources(XComponent data_row, List resources) {
      ((XComponent) (data_row.getChild(VISUAL_RESOURCES_COLUMN_INDEX))).setListValue(resources);
   }

   /**
    * Gets an array of visual resource - as seen by the user , h or % - for an activity data row
    *
    * @param data_row the activity data row
    * @return an <code>XArray<String> </code> of resources
    */
   public static List getVisualResources(XComponent data_row) {
      return ((XComponent) (data_row.getChild(VISUAL_RESOURCES_COLUMN_INDEX))).getListValue();
   }

   /**
    * Sets attachements for an activity data row
    *
    * @param data_row    the activity data row
    * @param attachments and <code> XArray </code>
    */
   public static void setAttachments(XComponent data_row, List attachments) {
      ((XComponent) (data_row.getChild(ATTACHMENTS_COLUMN_INDEX))).setListValue(attachments);
   }

   /**
    * Return an array of attachements for an activity data row
    *
    * @param data_row the activity data row
    * @return an <code> XArray </code> of attachements
    */
   public static List getAttachments(XComponent data_row) {
      return ((XComponent) (data_row.getChild(ATTACHMENTS_COLUMN_INDEX))).getListValue();
   }

   /**
    * Add a succesor for an activity data row
    *
    * @param data_row        the data row
    * @param successor_index the succesor index
    */
   public static void addSuccessor(XComponent data_row, int successor_index) {
      List successors = getSuccessors(data_row);
      if (successors == null) {
         successors = new ArrayList();
         setSuccessors(data_row, successors);
      }
      successors.add(new Integer(successor_index));
   }

   /**
    * Remove a succesor for an activity data row
    *
    * @param data_row        the data row
    * @param successor_index the succesor index
    */
   public static void removeSuccessor(XComponent data_row, int successor_index) {
      List successors = getSuccessors(data_row);
      if (successors != null) {
         for (int i = 0; i < successors.size(); i++) {
            if (((Integer) (successors.get(i))).intValue() == successor_index) {
               successors.remove(i);
               return;
            }
         }
      }
   }

   /**
    * Remove the link between the two given activities. No validation will occur.
    *
    * @param sourceIndex The index of the predecessor activity of the link
    * @param targetIndex The index of the successor activity of the link.
    */
   public void removeLink(int sourceIndex, int targetIndex) {
      XComponent dataSet = getDataSet();
      XComponent source = (XComponent) dataSet.getChild(sourceIndex);
      XComponent target = (XComponent) dataSet.getChild(targetIndex);
      removeSuccessor(source, targetIndex);
      removePredecessor(target, sourceIndex);
   }

   /**
    * Add a predecessor for an activity data row
    *
    * @param data_row          the data row
    * @param predecessor_index the predecesor index
    */
   public static void addPredecessor(XComponent data_row, int predecessor_index) {
      List predecessors = getPredecessors(data_row);
      if (predecessors == null) {
         predecessors = new ArrayList();
         setPredecessors(data_row, predecessors);
      }
      predecessors.add(new Integer(predecessor_index));
   }

   /**
    * Remove a predecessor for an activity data row
    *
    * @param data_row          the data row
    * @param predecessor_index the succesor index
    */
   public static void removePredecessor(XComponent data_row, int predecessor_index) {
      List predecessors = getPredecessors(data_row);
      if (predecessors != null) {
         for (int i = 0; i < predecessors.size(); i++) {
            if (((Integer) (predecessors.get(i))).intValue() == predecessor_index) {
               predecessors.remove(i);
               return;
            }
         }
      }
   }

   /**
    * Add a resource for an activity data row
    *
    * @param data_row the data row
    * @param resource the resource that should be added
    * @see OpGanttValidator#setResources(onepoint.express.XComponent,List)
    */
   public static void addResource(XComponent data_row, String resource) {
      List resources = getResources(data_row);
      if (resources == null) {
         resources = new ArrayList();
         setResources(data_row, resources);
      }
      resources.add(resource);
   }

   /**
    * Remove a resource for an activity data row
    *
    * @param data_row the data row
    * @param resource the resource that should be removed
    */
   public static void removeResource(XComponent data_row, String resource) {
      List predecessors = getPredecessors(data_row);
      if (predecessors != null) {
         for (int i = 0; i < predecessors.size(); i++) {
            if (((String) (predecessors.get(i))).equals(resource)) {
               predecessors.remove(i);
               return;
            }
         }
      }
   }

   public static void setBasePersonnelCosts(XComponent data_row, double base_personnel_costs) {
      ((XComponent) (data_row.getChild(BASE_PERSONNEL_COSTS_COLUMN_INDEX))).setDoubleValue(base_personnel_costs);
   }

   public static double getBasePersonnelCosts(XComponent data_row) {
      return ((XComponent) (data_row.getChild(BASE_PERSONNEL_COSTS_COLUMN_INDEX))).getDoubleValue();
   }

   public static void setBaseTravelCosts(XComponent data_row, double base_travel_costs) {
      ((XComponent) (data_row.getChild(BASE_TRAVEL_COSTS_COLUMN_INDEX))).setDoubleValue(base_travel_costs);
   }

   public static double getBaseTravelCosts(XComponent data_row) {
      return ((XComponent) (data_row.getChild(BASE_TRAVEL_COSTS_COLUMN_INDEX))).getDoubleValue();
   }

   public static void setBaseMaterialCosts(XComponent data_row, double base_material_costs) {
      ((XComponent) (data_row.getChild(BASE_MATERIAL_COSTS_COLUMN_INDEX))).setDoubleValue(base_material_costs);
   }

   public static double getBaseMaterialCosts(XComponent data_row) {
      return ((XComponent) (data_row.getChild(BASE_MATERIAL_COSTS_COLUMN_INDEX))).getDoubleValue();
   }

   public static void setBaseExternalCosts(XComponent data_row, double base_external_costs) {
      ((XComponent) (data_row.getChild(BASE_EXTERNAL_COSTS_COLUMN_INDEX))).setDoubleValue(base_external_costs);
   }

   public static double getBaseExternalCosts(XComponent data_row) {
      return ((XComponent) (data_row.getChild(BASE_EXTERNAL_COSTS_COLUMN_INDEX))).getDoubleValue();
   }

   public static void setBaseMiscellaneousCosts(XComponent data_row, double base_miscellaneous_costs) {
      ((XComponent) (data_row.getChild(BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX)))
           .setDoubleValue(base_miscellaneous_costs);
   }

   public static double getBaseMiscellaneousCosts(XComponent data_row) {
      return ((XComponent) (data_row.getChild(BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX))).getDoubleValue();
   }

   public static void setAttributes(XComponent data_row, int attributes) {
      ((XComponent) (data_row.getChild(MODE_COLUMN_INDEX))).setIntValue(attributes);
   }

   public static int getAttributes(XComponent data_row) {
      return ((XComponent) (data_row.getChild(MODE_COLUMN_INDEX))).getIntValue();
   }

   public static void setAttribute(XComponent data_row, int attribute, boolean value) {
      int attributes = getAttributes(data_row);
      if (value) {
         attributes |= attribute;
      }
      else {
         attributes -= (attributes & attribute);
      }
      setAttributes(data_row, attributes);
   }

   public static boolean getAttribute(XComponent data_row, int attribute) {
      return (getAttributes(data_row) & attribute) == attribute;
   }

   public static void setWorkPhaseStarts(XComponent data_row, List resources) {
      ((XComponent) (data_row.getChild(WORK_PHASE_STARTS_COLUMN_INDEX))).setListValue(resources);
   }

   public static List getWorkPhaseStarts(XComponent data_row) {
      return ((XComponent) (data_row.getChild(WORK_PHASE_STARTS_COLUMN_INDEX))).getListValue();
   }

   public static void setWorkPhaseFinishes(XComponent data_row, List resources) {
      ((XComponent) (data_row.getChild(WORK_PHASE_FINISHES_COLUMN_INDEX))).setListValue(resources);
   }

   public static List getWorkPhaseFinishes(XComponent data_row) {
      return ((XComponent) (data_row.getChild(WORK_PHASE_FINISHES_COLUMN_INDEX))).getListValue();
   }

   public static void setWorkPhaseBaseEfforts(XComponent data_row, List resources) {
      ((XComponent) (data_row.getChild(WORK_PHASE_BASE_EFFORTS_COLUMN_INDEX))).setListValue(resources);
   }

   public static List getWorkPhaseBaseEfforts(XComponent data_row) {
      return ((XComponent) (data_row.getChild(WORK_PHASE_BASE_EFFORTS_COLUMN_INDEX))).getListValue();
   }

   public static void addWorkPhaseStart(XComponent data_row, Date workPhaseStart) {
      List workPhaseStarts = getWorkPhaseStarts(data_row);
      if (workPhaseStarts == null) {
         workPhaseStarts = new ArrayList();
         setWorkPhaseStarts(data_row, workPhaseStarts);
      }
      workPhaseStarts.add(workPhaseStart);
   }

   public static void removeWorkPhaseStart(XComponent data_row, Date workPhaseStart) {
      List workPhaseStarts = getWorkPhaseStarts(data_row);
      if (workPhaseStarts != null) {
         for (int i = 0; i < workPhaseStarts.size(); i++) {
            if (workPhaseStarts.get(i).equals(workPhaseStart)) {
               workPhaseStarts.remove(i);
               return;
            }
         }
      }
   }

   public static void addWorkPhaseFinish(XComponent data_row, Date workBreakEnd) {
      List workPhaseFinishes = getWorkPhaseFinishes(data_row);
      if (workPhaseFinishes == null) {
         workPhaseFinishes = new ArrayList();
         setWorkPhaseFinishes(data_row, workPhaseFinishes);
      }
      workPhaseFinishes.add(workBreakEnd);
   }

   public static void removeWorkPhaseFinish(XComponent data_row, Date workBreakEnd) {
      List workPhaseFinishes = getWorkPhaseFinishes(data_row);
      if (workPhaseFinishes != null) {
         for (int i = 0; i < workPhaseFinishes.size(); i++) {
            if (workPhaseFinishes.get(i).equals(workBreakEnd)) {
               workPhaseFinishes.remove(i);
               return;
            }
         }
      }
   }

   public static void addWorkPhaseBaseEffort(XComponent data_row, double baseEffort) {
      List workPhaseBaseEfforts = getWorkPhaseBaseEfforts(data_row);
      if (workPhaseBaseEfforts == null) {
         workPhaseBaseEfforts = new ArrayList();
         setWorkPhaseBaseEfforts(data_row, workPhaseBaseEfforts);
      }
      workPhaseBaseEfforts.add(new Double(baseEffort));
   }

   public static void removeWorkPhaseBaseEffort(XComponent data_row, double baseEffort) {
      List workPhaseBaseEfforts = getWorkPhaseBaseEfforts(data_row);
      if (workPhaseBaseEfforts != null) {
         workPhaseBaseEfforts.remove(new Double(baseEffort));
      }
   }

   /**
    * Set the resources base efforts for the given activity. Must be in the same order as the resources assigned to this
    * activity
    *
    * @param data_row         activity to add the efforts to
    * @param resourcesEfforts XArray with values of the added efforts
    */
   public static void setResourceBaseEfforts(XComponent data_row, List resourcesEfforts) {
      ((XComponent) (data_row.getChild(RESOURCE_BASE_EFFORTS_COLUMN_INDEX))).setListValue(resourcesEfforts);
   }

   public static List getResourceBaseEfforts(XComponent data_row) {
      return ((XComponent) (data_row.getChild(RESOURCE_BASE_EFFORTS_COLUMN_INDEX))).getListValue();
   }

   /**
    * Adds a new resource based effort for a given data_row (activity). Must be in the same order as the resources
    * assigned to this activity
    *
    * @param data_row        activity to add the effort to
    * @param ressourceEffort value of the added effort
    */
   public static void addResourceBaseEffort(XComponent data_row, double ressourceEffort) {
      List resourceBaseEfforts;
      if (data_row.getChild(RESOURCE_BASE_EFFORTS_COLUMN_INDEX) == null) {
         resourceBaseEfforts = new ArrayList();
         setWorkPhaseBaseEfforts(data_row, resourceBaseEfforts);
      }
      else {
         resourceBaseEfforts = getResourceBaseEfforts(data_row);
      }
      resourceBaseEfforts.add(new Double(ressourceEffort));
   }

   // Accessors for assignment set
   // TODO: Maybe change naming, e.g. activityName() and assignmentAvailable()

   public static double getAvailable(XComponent data_row) {
      return ((XComponent) (data_row.getChild(AVAILABLE_COLUMN_INDEX))).getDoubleValue();
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
            if (!calendar.isWorkDay(projectStart)) {
               projectWorkingStart = calendar.nextWorkDay(projectStart);
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
            if (!activityRow.isDummyRow() && getType(activityRow) == TASK) {
               updateTaskCosts(activityRow);
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

   public final void setProgressTracked(Boolean progressTracked) {
      this.progressTracked = progressTracked;
   }

   public final Boolean getProgressTracked() {
      if (progressTracked == null) {
         progressTracked = (Boolean) projectSetting(PROGRESS_TRACKED);
      }
      return progressTracked;
   }

   /**
    * Set the set of absences to be used by the validator in the update process
    *
    * @param absencesSet
    */
   public void setAbsencesSet(HashMap absencesSet) {
      this.absencesSet = absencesSet;
   }

   /**
    * @return The set of absences used by the validator in the update process
    */
   public HashMap getAbsencesSet() {
      // TODO: Check for data-set "AbsenceSet" and read it
      // *** Columns: Resource (locator), Absences (XArray)
      // ==> Cache in HashMap absences
      if (absencesSet == null) {
         absencesSet = new HashMap();
         XComponent form = data_set.getForm();
         if (form != null) {
            XComponent absencesSet = form.findComponent(ABSENCES_SET);
            if (absencesSet != null) {
               XComponent dataRow = null;
               XComponent dataCell = null;
               String resourceLocator = null;
               List absentDays = null;
               for (int i = 0; i < absencesSet.getChildCount(); i++) {
                  dataRow = (XComponent) absencesSet.getChild(i);
                  // Column 0: Resource-locator
                  dataCell = (XComponent) dataRow.getChild(0);
                  resourceLocator = dataCell.getStringValue();
                  // Column 1: Absences
                  dataCell = (XComponent) dataRow.getChild(1);
                  absentDays = dataCell.getListValue();
                  this.absencesSet.put(resourceLocator, absentDays);
               }
            }
         }
      }
      return absencesSet;
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

   /**
    * Returns the subactivities (with outline level greater with 1) of a collection activity.
    *
    * @param activity the collection activity
    * @return an XArray<Integer> of subactivities
    */
   public List subActivities(XComponent activity) {
      List sub_activities = new ArrayList();
      int sub_outline_level = activity.getOutlineLevel() + 1;
      int index = activity.getIndex() + 1;
      XComponent next = null;
      while (index < data_set.getChildCount()) {
         next = (XComponent) (data_set.getChild(index));
         if (next.getOutlineLevel() == sub_outline_level) {
            if (getType(next) != TASK && getType(next) != COLLECTION_TASK) {
               sub_activities.add(next);
            }
         }
         if (next.getOutlineLevel() < sub_outline_level) {
            return sub_activities;
         }
         index++;
      }
      return sub_activities;
   }

   /**
    * Returns all the sub tasks of the given activity
    *
    * @param activity
    * @return the sub tasks of the given activity (outline level +1 )
    */
   public List subTasks(XComponent activity) {
      List sub_activities = new ArrayList();
      int sub_outline_level = activity.getOutlineLevel() + 1;
      int index = activity.getIndex() + 1;
      XComponent next = null;
      while (index < data_set.getChildCount()) {
         next = (XComponent) (data_set.getChild(index));
         if (next.getOutlineLevel() == sub_outline_level) {
            if (getType(next) == TASK || getType(next) == COLLECTION_TASK) {
               sub_activities.add(next);
            }
         }
         if (next.getOutlineLevel() < sub_outline_level) {
            return sub_activities;
         }
         index++;
      }
      return sub_activities;
   }

   /**
    * Check if a activity is a collection activity (it has activities with outline level greater)
    *
    * @param activity the activity
    * @return boolean true if the activity is a collection ,false otherwise
    */
   private boolean isCollectionActivity(XComponent activity) {
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
      List predecessors = getPredecessors(activity);
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
         updateTypeForActivity(activity);
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
                  updateProjectPlanFinish();
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
   protected byte updateTypeForActivity(XComponent activity) {
      boolean isCollection;
      boolean isTask;
      boolean isTaskCollection;
      isCollection = isCollectionActivity(activity);
      isTask = (getStart(activity) == null);
      isTaskCollection = isCollectionTask(activity);
      boolean isScheduledTask = isScheduledTask(activity);
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
      else if (isMilestone(activity)) {
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
      // Update type and disable non-editable cells for collections and milestones
      byte oldType = getType(activity);
      setType(activity, type);
      boolean enableComplete = false;
      Boolean trackingSetting = getProgressTracked();
      if (trackingSetting != null && !trackingSetting.booleanValue()) {
         enableComplete = true;
      }
      activity.getChild(NAME_COLUMN_INDEX).setEnabled(true);
      switch (type) {
         case COLLECTION:
            activity.getChild(START_COLUMN_INDEX).setEnabled(false);
            activity.getChild(COMPLETE_COLUMN_INDEX).setEnabled(false);
            activity.getChild(END_COLUMN_INDEX).setEnabled(false);
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
            break;
         case MILESTONE:
            activity.getChild(START_COLUMN_INDEX).setEnabled(true);
            activity.getChild(END_COLUMN_INDEX).setEnabled(true);
            activity.getChild(COMPLETE_COLUMN_INDEX).setEnabled(enableComplete);
            activity.getChild(VISUAL_RESOURCES_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_EFFORT_COLUMN_INDEX).setEnabled(true);
            activity.getChild(DURATION_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_TRAVEL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_MATERIAL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_EXTERNAL_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX).setEnabled(false);
            activity.getChild(PRIORITY_COLUMN_INDEX).setEnabled(false);
            activity.getChild(PAYMENT_COLUMN_INDEX).setEnabled(true);
            // Clear not-relevant values (or do this during or after validation?)
            setBaseEffort(activity, 0.0d);
            setDuration(activity, 0.0d);
            setPriority(activity, null);
            setWorkPhaseStarts(activity, new ArrayList());
            setWorkPhaseFinishes(activity, new ArrayList());
            setWorkPhaseBaseEfforts(activity, new ArrayList());
            //no costs for milestones
            setBasePersonnelCosts(activity, 0.0d);
            setBaseProceeds(activity, 0.0d);
            setBaseExternalCosts(activity, 0.0d);
            setBaseMaterialCosts(activity, 0.0d);
            setBaseMiscellaneousCosts(activity, 0.0d);
            setBaseTravelCosts(activity, 0.0d);
            convertResourcesToNameOnly(activity, getResources(activity));
            if (getComplete(activity) == 100) {
               setComplete(activity, 100);
            }
            else {
               setComplete(activity, 0);
            }
            List efforts = getResourceBaseEfforts(activity);
            for (int i = 0; i < efforts.size(); i++) {
               efforts.set(i, new Double(0));
            }
            break;
         case STANDARD:
            activity.getChild(START_COLUMN_INDEX).setEnabled(true);
            activity.getChild(END_COLUMN_INDEX).setEnabled(true);
            activity.getChild(VISUAL_RESOURCES_COLUMN_INDEX).setEnabled(true);
            activity.getChild(COMPLETE_COLUMN_INDEX).setEnabled(enableComplete);
            activity.getChild(BASE_EFFORT_COLUMN_INDEX).setEnabled(true);
            activity.getChild(DURATION_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_TRAVEL_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_MATERIAL_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_EXTERNAL_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX).setEnabled(true);
            activity.getChild(PRIORITY_COLUMN_INDEX).setEnabled(false);
            activity.getChild(PAYMENT_COLUMN_INDEX).setEnabled(false);
            setPriority(activity, null);
            if (activity.expandable()) {
               activity.expanded(true, false);
            }
            resetComplete(activity);
            break;
         case SCHEDULED_TASK:
            activity.getChild(START_COLUMN_INDEX).setEnabled(true);
            activity.getChild(END_COLUMN_INDEX).setEnabled(true);
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
            setWorkPhaseStarts(activity, new ArrayList());
            setWorkPhaseFinishes(activity, new ArrayList());
            setWorkPhaseBaseEfforts(activity, new ArrayList());
            setResources(activity, new ArrayList());
            setVisualResources(activity, new ArrayList());
            if (activity.expandable()) {
               activity.expanded(true, false);
            }
            break;
         case COLLECTION_TASK:
            activity.getChild(START_COLUMN_INDEX).setEnabled(false);
            activity.getChild(END_COLUMN_INDEX).setEnabled(false);
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
            updateProjectPlanFinish();
            setResourceBaseEfforts(activity, new ArrayList());
            setWorkPhaseStarts(activity, new ArrayList());
            setWorkPhaseFinishes(activity, new ArrayList());
            setWorkPhaseBaseEfforts(activity, new ArrayList());
            setResources(activity, new ArrayList());
            setVisualResources(activity, new ArrayList());
            breakAllLinks(activity);
            setPriority(activity, null);
            if (oldType != COLLECTION_TASK) {
               activity.expanded(true, false);
            }
            break;
         case TASK:
            activity.getChild(START_COLUMN_INDEX).setEnabled(true);
            activity.getChild(END_COLUMN_INDEX).setEnabled(true);
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
            setWorkPhaseStarts(activity, new ArrayList());
            setWorkPhaseFinishes(activity, new ArrayList());
            setWorkPhaseBaseEfforts(activity, new ArrayList());
            ArrayList newResources = new ArrayList();
            List resourcesEfforts = new ArrayList();
            if (getResources(activity) != null && getResources(activity).size() > 0) {
               newResources.add(getResources(activity).get(0));
               resourcesEfforts.add(new Double(getBaseEffort(activity)));
            }
            convertResourcesToNameOnly(activity, newResources);

            //update the base personnel costs
            updateTaskCosts(activity);

            setResourceBaseEfforts(activity, resourcesEfforts);
            //break all the links
            breakAllLinks(activity);

            resetComplete(activity);

            if (oldType != TASK) {
               setPriority(activity, new Byte((byte) 5));
            }
            if (getComplete(activity) == 100) {
               setComplete(activity, 100);
            }
            else {
               setComplete(activity, 0);
            }
            break;
      }
   }

   private void resetComplete(XComponent activity) {
      if (progressTracked != null && progressTracked.booleanValue()) {
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
   private void breakAllLinks(XComponent activity) {
      List successors = getSuccessors(activity);
      for (Iterator iterator = successors.iterator(); iterator.hasNext();) {
         int index = ((Integer) iterator.next()).intValue();
         XComponent otherActivity = (XComponent) data_set.getChild(index);
         List preds = getPredecessors(otherActivity);
         preds.remove(new Integer(activity.getIndex()));
      }
      setSuccessors(activity, new ArrayList());

      List predecessors = getPredecessors(activity);
      for (Iterator iterator = predecessors.iterator(); iterator.hasNext();) {
         int i = ((Integer) iterator.next()).intValue();
         XComponent otherActivity = (XComponent) data_set.getChild(i);
         List succs = getSuccessors(otherActivity);
         succs.remove(new Integer(activity.getIndex()));
      }
      setPredecessors(activity, new ArrayList());
   }

   /**
    * Will update the effort, cost and %completed for the given collection based on the children activities.
    *
    * @param collection - collection activity to be updated. Can be either a normal collection or a collection of tasks.
    */
   //<FIXME> author="Mihai Costin" description="Should be split-up for scheduled activities (have subtasks) and collections (have sub activities)"
   protected void updateCollectionValues(XComponent collection) {
      //<FIXME>
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
         else if (type == OpGanttValidator.TASK || type == OpGanttValidator.COLLECTION_TASK) {
            tasksCompleteSum += getComplete(activity);
            tasksChildCount++;
            baseSum += getBaseEffort(activity);
         }
         else {
            double baseEffort = getBaseEffort(activity);
            double actualEffort = getActualEffort(activity);
            double completeValue = getComplete(activity);
            double remainingEffort = calculateRemainingEffort(baseEffort, actualEffort, completeValue);

            actualSum += actualEffort;
            remainingSum += remainingEffort;
            baseSum += baseEffort;
         }

         perCost += getBasePersonnelCosts(activity);
         proceeds += getBaseProceeds(activity);
         matCost += getBaseMaterialCosts(activity);
         travCost += getBaseTravelCosts(activity);
         extCost += getBaseExternalCosts(activity);
         miscCost += getBaseMiscellaneousCosts(activity);
      }

      double standardAvg = 0;
      if (OpGanttValidator.getType(collection) != OpGanttValidator.COLLECTION_TASK) {
         standardAvg = calculateCompleteValue(actualSum, baseSum, remainingSum);
      }
      double tasksAvg = (tasksChildCount != 0) ? (tasksCompleteSum / tasksChildCount) : 0;

      byte collectionType = OpGanttValidator.getType(collection);
      if (collectionType == OpGanttValidator.COLLECTION) {
         complete = standardAvg;
      }
      else {
         complete = tasksAvg;
      }
      setComplete(collection, complete);

      //base effort
      setBaseEffort(collection, baseSum);

      // set the costs
      setBasePersonnelCosts(collection, perCost);
      setBaseProceeds(collection, proceeds);
      setBaseMaterialCosts(collection, matCost);
      setBaseTravelCosts(collection, travCost);
      setBaseExternalCosts(collection, extCost);
      setBaseMiscellaneousCosts(collection, miscCost);

      // collections should have no resources assigned
      setResources(collection, new ArrayList());
   }

   /**
    * Will call <code>updateCollectionValues</code> for every collection activity of the collection tree.
    *
    * @param activity - the activity from the tree who's parents will be updated
    * @see OpGanttValidator#updateCollectionValues(onepoint.express.XComponent)
    */
   protected void updateCollectionTreeValues(XComponent activity) {
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

   protected boolean isCollectionType(XComponent activity) {
      return getType(activity) == COLLECTION || getType(activity) == COLLECTION_TASK || getType(activity) == SCHEDULED_TASK;
   }

   private void _validateActivity(XComponent activity, XCalendar calendar, Date not_before, boolean child,
        boolean parent) {
      // Recursive validation method: Start time of successor is set to this end
      // if end is greater

      logger.debug("VALIDATE: " + getName(activity) + " not before " + not_before);

      // Check not-before condition
      Date start = getStart(activity);
      Date end = null;
      Date workingProjectStart = getWorkingProjectStart();
      if (not_before != null) {
         if ((start == null) || (start.getTime() < not_before.getTime())) {
            // *** TODO: Check duration management -- use real work time etc.
            double duration = getDuration(activity);
            logger.debug("   duration " + duration);
            if (duration > 0.0) {
               start = calendar.nextWorkDay(not_before);

               long amount = 0;
               if (workingProjectStart != null && start.before(workingProjectStart)) {
                  amount = workingProjectStart.getTime() - start.getTime();
                  start = new Date(start.getTime() + amount);
               }

               setStart(activity, start);

               if (getType(activity) == STANDARD ||
                    (subActivities(activity).size() == 0 && subTasks(activity).size() != 0)) {
                  updateBaseEffort(activity, getBaseEffort(activity));
                  updateDuration(activity, duration);
               }

               end = getEnd(activity);
               if (end != null) {
                  end = new Date(end.getTime() + amount);
               }
            }
            else {
               // Milestones
               start = not_before;
               if (workingProjectStart != null && start.before(workingProjectStart)) {
                  start = workingProjectStart;
               }
               setStart(activity, start);
               end = start;
            }
         }
         else {
            end = getEnd(activity);
         }
      }
      else {
         if (getType(activity) != COLLECTION) {
            updateDuration(activity, getDuration(activity));
         }
         end = getEnd(activity);
      }


      if (getType(activity) == MILESTONE) {
         end = start;
      }
      else {
         if (end == null) {
            double duration = getDuration(activity);
            updateDuration(activity, duration);
            end = getEnd(activity);
         }
      }

      logger.debug("   start " + start);
      logger.debug("   end " + end);

      // Validate sub-activities
      int index = 0;

      XComponent sub_activity = null;
      List sub_activities = subActivities(activity);

      // if activity is a collection
      if (sub_activities.size() > 0 && child) {
         for (index = 0; index < sub_activities.size(); index++) {
            sub_activity = (XComponent) (sub_activities.get(index));
            _validateActivity(sub_activity, calendar, not_before, true, false);
         }

         // Reset end date to maximum of child end dates
         end = null;
         Date child_end = null;
         for (index = 0; index < sub_activities.size(); index++) {
            sub_activity = (XComponent) (sub_activities.get(index));
            child_end = getEnd(sub_activity);
            if ((child_end != null) && ((end == null) || (end.getTime() < child_end.getTime()))) {
               end = child_end;
            }
         }
         // Reset start date to minimium of child end dates
         start = null;
         Date child_start = null;
         for (index = 0; index < sub_activities.size(); index++) {
            sub_activity = (XComponent) (sub_activities.get(index));
            child_start = getStart(sub_activity);
            if ((child_start != null) && ((start == null) || (start.getTime() > child_start.getTime()))) {
               start = child_start;
            }
         }
      }

      // Set start and end date
      if (start != null && workingProjectStart != null && start.before(workingProjectStart)) {
         long amount = 0;
         amount = workingProjectStart.getTime() - start.getTime();
         start = new Date(start.getTime() + amount);

         //set end based on duration
         setStart(activity, start);
         updateDuration(activity, getDuration(activity));
         end = getEnd(activity);
      }
      else {
         setStart(activity, start);
         setEnd(activity, end);
         updateProjectPlanFinish();
      }

      if (start != null && !calendar.isWorkDay(start)) {
         start = calendar.nextWorkDay(start);
         setStart(activity, start);
         updateDuration(activity, getDuration(activity));
         end = getEnd(activity);
      }

      // update the values of effort, costs etc for a collection
      if (OpGanttValidator.getType(activity) == OpGanttValidator.COLLECTION || OpGanttValidator.getType(activity) == OpGanttValidator.SCHEDULED_TASK) {
         //update duration for collection based on the new end
         updateFinish(activity, getEnd(activity));
         updateCollectionValues(activity);
      }

      // Validate successors
      // *** This is a hack (next day): Have to use standard work-time here
      Date successor_start = end; // calendar.nextWorkDay(end);
      List successors = getSuccessors(activity);
      if (successors != null) {
         XComponent successor = null;
         for (index = 0; index < successors.size(); index++) {
            successor = (XComponent) (data_set._getChild(((Integer) (successors.get(index))).intValue()));
            logger.debug("   suc " + getName(successor) + " not before " + successor_start);
            _validateActivity(successor, calendar, successor_start, true, true);
         }
      }

      // Validate super-activity (start and end-date)
      if ((start != null) && (end != null) && (parent)) {
         XComponent super_activity = superActivity(activity);
         if (super_activity != null) {
            logger.debug("Super-activity " + super_activity.getIndex());
            _validateSuperActivity(super_activity, calendar, start, end);
         }
      }
   }

   private void _validateSuperActivity(XComponent activity, XCalendar calendar, Date child_start, Date child_end) {
      Date start = getStart(activity);
      Date end = getEnd(activity);
      logger.debug("cs, ce " + child_start + "," + child_end);
      boolean do_validate = false;
      if ((start == null) || (start.getTime() > child_start.getTime())) {
         setStart(activity, child_start);
         do_validate = true;
      }
      if ((end == null) || (end.getTime() < child_end.getTime())) {
         setEnd(activity, child_end);
         updateProjectPlanFinish();
         do_validate = true;
      }
      if (do_validate) {
         _validateActivity(activity, calendar, null, false, true);
      }
      else {
         updateCollectionValues(activity);
      }
   }

   /**
    * Updated the values for an activity that can contain tasks (collection task or scheduled task).
    *
    * @param taskParent a <code>XComponent</code> that is either a collection task or a scheduled task.
    */
   protected void updateTaskParentValues(XComponent taskParent) {
      List subTasks = subTasks(taskParent);
      double complete = 0;
      double baseEffort = 0;

      //costs
      double basePersonnelCosts = 0;
      double baseProceeds = 0;
      double baseMaterialCosts = 0;
      double baseTravelCosts = 0;
      double baseMiscellaneousCosts = 0;
      double baseExternalCosts = 0;

      for (int i = 0; i < subTasks.size(); i++) {
         XComponent subTask = (XComponent) subTasks.get(i);
         if (getType(subTask) == COLLECTION_TASK) {
            updateTaskParentValues(subTask);
         }
         complete += getComplete(subTask);
         baseEffort += getBaseEffort(subTask);
         basePersonnelCosts += getBasePersonnelCosts(subTask);
         baseProceeds += getBaseProceeds(subTask);
         baseMaterialCosts += getBaseMaterialCosts(subTask);
         baseTravelCosts += getBaseTravelCosts(subTask);
         baseMiscellaneousCosts += getBaseMiscellaneousCosts(subTask);
         baseExternalCosts += getBaseExternalCosts(subTask);

      }
      if (subTasks.size() != 0) {
         complete = complete / subTasks.size();
      }
      setComplete(taskParent, complete);
      setBaseEffort(taskParent, baseEffort);

      setBaseExternalCosts(taskParent, baseExternalCosts);
      setBaseMaterialCosts(taskParent, baseMaterialCosts);
      setBaseMiscellaneousCosts(taskParent, baseMiscellaneousCosts);
      setBasePersonnelCosts(taskParent, basePersonnelCosts);
      setBaseProceeds(taskParent, baseProceeds);
      setBaseTravelCosts(taskParent, baseTravelCosts);
   }

   public boolean validateDataSet() {
      if (XDisplay.getDefaultDisplay() != null) {
         calendar = XDisplay.getDefaultDisplay().getCalendar();
      }
      List fixed_activities = updateActivityTypes(true);

      //get all the collection tasks and update the values of the collection tasks
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent task = (XComponent) data_set.getChild(i);
         if (getType(task) == COLLECTION_TASK) {
            setComplete(task, -1);
         }
      }
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent task = (XComponent) data_set.getChild(i);
         if (getType(task) == COLLECTION_TASK) {
            if (getComplete(task) == -1) {
               updateTaskParentValues(task);
            }
         }
      }

      XComponent activity;
      for (int index = 0; index < fixed_activities.size(); index++) {
         activity = (XComponent) (fixed_activities.get(index));
         _validateActivity(activity, calendar, null, false, true);
      }
      fixed_activities.clear();
      return true;
   }

   public boolean validateEntireDataSet() {
      return validateDataSet();
   }

   /*
    * public boolean validateDataRow(XComponent data_row) { return false; }
    */

   /**
    * @see onepoint.express.XValidator#newDataRow()
    */
   public XComponent newDataRow() {
      logger.debug("OpGanttValidator.newDataRow()");
      // *** TODO: Can we leave some values "blank"/null?
      // ==> At least not null [currently]: Problem w/exceptions in editors
      XComponent data_row = new XComponent(XComponent.DATA_ROW);
      XComponent data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setStringValue(null); // Name 0
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setByteValue(STANDARD); // Type 1
      data_row.addChild(data_cell);
      // Default category is null 2
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      //complete is enabled only if tracking is off
      Boolean trackingSetting = getProgressTracked();
      if (trackingSetting != null && !trackingSetting.booleanValue()) {
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
      }
      data_cell.setDoubleValue(0); // Complete 3
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);

      Date start;
      if (getProjectTemplate().booleanValue()) {
         start = getDefaultTemplateStart();
      }
      else {
         start = XCalendar.today();
         if (!calendar.isWorkDay(start)) {
            start = calendar.nextWorkDay(start);
         }
         if (getWorkingProjectStart() != null && start.before(getWorkingProjectStart())) {
            start = getWorkingProjectStart();
         }
      }

      data_cell.setDateValue(start); // Start 4
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDateValue(start); // End 5
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0); // Duration 6
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0); // Effort 7
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList()); // Predecessors 8
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList()); // Successors 9
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList()); // Resources 10
      data_row.addChild(data_cell);
      // Base personnel costs - 11 (not editable)
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);
      // base travel costs - 12
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);
      // base material costs - 13
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);
      //base external costs - 14
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);
      //base miscellaneous costs - 15
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);
      // Description 16
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setStringValue(null);
      data_row.addChild(data_cell);
      // Attachments 17
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);
      // Attributes 18
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setIntValue(0);
      data_row.addChild(data_cell);
      // Work phase begins and ends 19 , 20
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);
      // work phase base column index 21
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_row.addChild(data_cell);

      // Resource Base Efforts 22
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);

      // Priority (23)
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setValue(null);
      data_row.addChild(data_cell);

      // Work records map (24) - for a new activity is always empty
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setValue(new HashMap());
      data_row.addChild(data_cell);

      //actual effort (25)
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);

      // Visual Resources (26)
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_cell.setListValue(new ArrayList());
      data_row.addChild(data_cell);

      // Responsible Resource (27)
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setStringValue(null);
      data_row.addChild(data_cell);

      //project cell (28)
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setStringValue(null);
      data_row.addChild(data_cell);

      // Payment (29)
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);

      // Base Proceeds (30)
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setDoubleValue(0);
      data_row.addChild(data_cell);

      // Must be done at the end: Might potentially need full data-row
      // TODO: Duration of new activities should be configurable
      double duration = 5 * calendar.getWorkHoursPerDay();
      updateDuration(data_row, duration);

      return data_row;
   }

   /**
    * @return the default start date for a template project (01.01.2001)
    */
   public static Date getDefaultTemplateStart() {
      Date start;
      Calendar calendar = XCalendar.getDefaultCalendar().getCalendar();
      calendar.set(Calendar.YEAR, 2001);
      calendar.set(Calendar.MONTH, 1);
      calendar.set(Calendar.DAY_OF_YEAR, 1);
      start = new Date(calendar.getTime().getTime());
      return start;
   }

   /**
    * @see XValidator#addDataRow(onepoint.express.XComponent)
    */
   public void addDataRow(XComponent data_row) {
      addToUndo();
      if (data_set.getChildCount() > 0) {
         XComponent lastActivity = (XComponent) data_set.getChild(data_set.getChildCount() - 1);
         if (getType(lastActivity) == TASK) {
            updateType(data_row, TASK);
         }
         setCategory(data_row, getCategory(lastActivity));
      }
      data_set.addChild(data_row);
      validateDataSet();
   }

   /**
    * @see XValidator#addDataRow(int,onepoint.express.XComponent)
    */
   public void addDataRow(int index, XComponent data_row)
        throws XValidationException {
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
         XComponent parent = (XComponent) data_set.getChild(index - 1);
         if (parent.getOutlineLevel() + 1 == data_row.getOutlineLevel()) {
            checkDeletedAssignmentsForWorkslips(parent, new ArrayList());
         }
      }

      XComponent updated_data_row;
      for (int i = 0; i < data_set.getChildCount(); i++) {
         updated_data_row = (XComponent) (data_set._getChild(i));
         updateIndexListAfterAdd(getSuccessors(updated_data_row), index);
         updateIndexListAfterAdd(getPredecessors(updated_data_row), index);
      }
      if (index < data_set.getChildCount()) {
         XComponent previousChild = (XComponent) data_set.getChild(index);
         if (index > 0) {
            previousChild = (XComponent) data_set.getChild(index - 1);
         }

         if (getType(previousChild) == TASK || getType(previousChild) == COLLECTION_TASK) {
            updateType(data_row, TASK);
         }

         if (isCollectionType(previousChild)) {
            clearCollectionActivity(previousChild);
         }

         setCategory(data_row, getCategory(previousChild));
      }
      data_set.addChild(index, data_row);
      validateDataSet();
   }

   private void clearCollectionActivity(XComponent previousChild) {
      setComplete(previousChild, 0);
      setBaseExternalCosts(previousChild, 0);
      setBaseMaterialCosts(previousChild, 0);
      setBaseMiscellaneousCosts(previousChild, 0);
      setBasePersonnelCosts(previousChild, 0);
      setBaseTravelCosts(previousChild, 0);
   }

   /**
    * @throws XValidationException if all the removed rows are mandatory
    * @see XValidator#removeDataRows(java.util.List)
    */
   public boolean removeDataRows(List data_rows) {

      preCheckRemoveDataRows(data_rows);

      addToUndo();
      for (int i = 0; i < data_rows.size(); i++) {
         _removeDataRow((XComponent) (data_rows.get(i)));
      }
      // *** TODO: Always validate "outside" OR inside (currently: mixed)
      validateDataSet();
      return true;
   }

   /**
    * Performes a suite of checks that must be done before the actual remove action
    *
    * @param data_rows data rows that will be removed.
    * @throws XValidationException MANDATORY_EXCEPTION if all "to be removed" activities are mandatory
    */
   protected void preCheckRemoveDataRows(List data_rows) {
      boolean allMandatory = true;
      for (int i = 0; i < data_rows.size(); i++) {
         XComponent data_row = (XComponent) (data_rows.get(i));
         if (!isProjectMandatory(data_row)) {
            allMandatory = false;
            break;
         }
      }

      if (mandatoryCollectionCheck(data_rows)) {
         throw new XValidationException(MANDATORY_EXCEPTION);
      }

      if (allMandatory) {
         throw new XValidationException(MANDATORY_EXCEPTION);
      }

      //we cannot remove activities if we have assignments with workrecords.
      for (int i = 0; i < data_rows.size(); i++) {
         XComponent data_row = (XComponent) data_rows.get(i);
         checkDeletedAssignmentsForWorkslips(data_row, new ArrayList());
      }
   }

   /**
    * Removes the <code>data_row</code> from the data set.
    *
    * @param data_row the <code>DATA_ROW</code> which will be removed
    */
   protected void _removeDataRow(XComponent data_row) {
      // *** Maybe just use index as argument (in this case also for setValue)
      // *** First, link-management: Keep predecessors/successors consistent
      // *** Then, remove & validate; note: Incremental validation possible
      int index = data_row.getIndex();
      //check for mandatory activities
      XComponent row = (XComponent) data_set.getChild(index);
      if (!isProjectMandatory(row)) {
         //mandatory activities can't be removed
         data_set.removeChild(index);
         // update index list for the data set
         XComponent updated_data_row = null;
         for (int i = 0; i < data_set.getChildCount(); i++) {
            updated_data_row = (XComponent) (data_set._getChild(i));
            updateIndexListAfterRemove(getSuccessors(updated_data_row), index);
            updateIndexListAfterRemove(getPredecessors(updated_data_row), index);
         }
      }
   }


   /**
    * @see XValidator#setDataCellValue(onepoint.express.XComponent,int,Object)
    */
   public void setDataCellValue(XComponent data_row, int column_index, Object value) {
      // *** TODO: Currently only implemented for days -- use workDuration
      // ==> Here we could also check if the correct value-type is set
      // *** In addition, use incremental validation in the future
      // *** TODO: Introduce boolean 'modified'; only validate of value changed
      data_set.removeAllDummyRows();
      logger.debug("*** setDataCellValue: " + data_row.getIndex() + "," + column_index + " = " + value);
      switch (column_index) {
         case NAME_COLUMN_INDEX:
            if (isProjectMandatory(data_row)) {
               if ((getName(data_row) == null && value != null) || (!getName(data_row).equals(value))) {
                  throw new XValidationException(MANDATORY_EXCEPTION);
               }
            }
            addToUndo();
            setName(data_row, (String) value);
            break;
         case DESCRIPTION_COLUMN_INDEX:
            addToUndo();
            setDescription(data_row, (String) value);
            break;
         case TYPE_COLUMN_INDEX:
            addToUndo();
            setType(data_row, ((Byte) value).byteValue());
            break;
         case CATEGORY_COLUMN_INDEX:
            if (isProjectMandatory(data_row)) {
               String newCategoryId = (value != null) ? choiceID((String) value) : NO_CATEGORY_ID;
               String existentCategory = getCategory(data_row);
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
            setCategory(data_row, categoryChoice);
            break;
         case RESPONSIBLE_RESOURCE_COLUMN_INDEX:
            String resourceChoice = (String) value;
            String resourceId = choiceID(resourceChoice);
            if (resourceId.equals(NO_RESOURCE_ID)) {
               resourceChoice = null;
            }
            addToUndo();
            setResponsibleResource(data_row, resourceChoice);
            break;
         case START_COLUMN_INDEX:

            preCheckSetStartValue(data_row, value);

            if (value == null) {
               //activity becomes a task
               checkDeletedAssignmentsForWorkslips(data_row, new ArrayList());
               addToUndo();
               setStart(data_row, null);
               validateDataSet();
            }
            else {
               Date start = (Date) value;
               // If start is not a workday then go to next workday
               if (!calendar.isWorkDay(start)) {
                  start = calendar.nextWorkDay(start);
               }
               addToUndo();
               setStart(data_row, start);
               updateDuration(data_row, getDuration(data_row));
               validateDataSet();
            }
            break;
         case END_COLUMN_INDEX:

            preCheckSetEndValue(data_row, value);

            addToUndo();

            if (value == null) {
               //activity becomes a task
               setStart(data_row, null);
               validateDataSet();
               break;
            }
            // Update duration
            Date end = (Date) value;

            // If end is not a workday then go to previous workday
            if (!calendar.isWorkDay(end)) {
               end = calendar.previousWorkDay(end);
            }
            if (getStart(data_row) == null) {
               setStart(data_row, (Date) value);
            }
            updateFinish(data_row, end);

            // TODO: Incremental validation of successors (recursive)
            validateDataSet();
            break;

         case DURATION_COLUMN_INDEX:
            // Update end date
            double duration = ((Double) value).doubleValue();
            preCheckSetDurationValue(data_row, duration);

            addToUndo();

            updateDuration(data_row, duration);

            // TODO: Incremental validation of successors (recursive)
            validateDataSet();
            break;

         case BASE_EFFORT_COLUMN_INDEX:
            double base_effort = ((Double) value).doubleValue();
            
            //if the project is effort based, setting the effort will also affect the duration
            if (getCalculationMode() != null && getCalculationMode().byteValue() == EFFORT_BASED) {
               preCheckSetEffortValue(data_row, base_effort);
            }

            addToUndo();

            updateBaseEffort(data_row, base_effort);

            // TODO: Incremental validation of successors (recursive)
            validateDataSet();
            break;
         case COMPLETE_COLUMN_INDEX:
            // Change percentage range (0-100)
            if (value != null) {
               double complete = ((Double) value).doubleValue();
               byte type = OpGanttValidator.getType(data_row);

               addToUndo();

               if ((type == OpGanttValidator.TASK || type == OpGanttValidator.MILESTONE) && complete < 100) {
                  setComplete(data_row, 0);
               }
               else if ((complete >= 0) && (complete <= 100)) {
                  setComplete(data_row, complete);
               }
               updateCollectionTreeValues(data_row);
            }
            break;

         case PREDECESSORS_COLUMN_INDEX:
            addToUndo();
            setPredecessorsValue(value, data_row);
            validateDataSet();
            break;

         case SUCCESSORS_COLUMN_INDEX:
            addToUndo();
            setSuccessorsValue(value, data_row);
            validateDataSet();
            break;

         case VISUAL_RESOURCES_COLUMN_INDEX:
            List visualResources = (ArrayList) value;
            this.parseVisualResources(visualResources);

            //tasks, milestones keep only the resource name
            if (getType(data_row) == TASK || getType(data_row) == MILESTONE) {
               addToUndo();
               List resources = visualResources;

               boolean taskWarning = false;
               //for tasks only one resource
               if (getType(data_row) == TASK && visualResources.size() > 1) {
                  resources = new ArrayList();
                  resources.add(visualResources.get(0));
                  taskWarning = true;
               }
               checkDeletedAssignmentsForWorkslips(data_row, resources);
               convertResourcesToNameOnly(data_row, resources);
               List effList = new ArrayList();
               for (int i = 0; i < resources.size(); i++) {
                  if (getType(data_row) == TASK) {
                     //tasks should only have 1 resource
                     effList.add(new Double(getBaseEffort(data_row)));
                     break;
                  }
                  else {
                     effList.add(new Double(0));
                  }
               }
               setResourceBaseEfforts(data_row, effList);
               updateResponsibleResource(data_row);

               validateDataSet();
               if (taskWarning) {
                  throw new XValidationException(TASK_EXTRA_RESOURCE_EXCEPTION);
               }
               break;
            }
            else {
               //standard activity
               //make sure from here onwards, we have all the assignment values in independent format.
               List resources = deLocalizeVisualResources(visualResources);

               resources = prepareResources(data_row, resources);

               addToUndo();

               setResources(data_row, resources);

               //effort stays the same.
               updateBaseEffort(data_row, getDuration(data_row));

               //construct the resource availability map
               updateVisualResources(data_row, isHourBasedResourceView(), getAvailabilityMap());
               updateResponsibleResource(data_row);

               validateDataSet();
            }
            break;

         case BASE_TRAVEL_COSTS_COLUMN_INDEX:
            double costValue = ((Double) value).doubleValue();
            if (costValue < 0) {
               throw new XValidationException(INVALID_COST_EXCEPTION);
            }
            addToUndo();
            setBaseTravelCosts(data_row, costValue);
            updateCollectionTreeValues(data_row);
            break;

         case BASE_MATERIAL_COSTS_COLUMN_INDEX:
            costValue = ((Double) value).doubleValue();
            if (costValue < 0) {
               throw new XValidationException(INVALID_COST_EXCEPTION);
            }
            addToUndo();
            setBaseMaterialCosts(data_row, costValue);
            updateCollectionTreeValues(data_row);
            break;

         case BASE_EXTERNAL_COSTS_COLUMN_INDEX:
            costValue = ((Double) value).doubleValue();
            if (costValue < 0) {
               throw new XValidationException(INVALID_COST_EXCEPTION);
            }
            addToUndo();
            setBaseExternalCosts(data_row, costValue);
            updateCollectionTreeValues(data_row);
            break;

         case BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX:
            costValue = ((Double) value).doubleValue();
            if (costValue < 0) {
               throw new XValidationException(INVALID_COST_EXCEPTION);
            }
            addToUndo();
            setBaseMiscellaneousCosts(data_row, costValue);
            updateCollectionTreeValues(data_row);
            break;

         case ATTACHMENTS_COLUMN_INDEX:
            List attachments = (ArrayList) value;
            setAttachments(data_row, attachments);
            updateAttachmentAttribute(data_row);
            break;
         case PRIORITY_COLUMN_INDEX:
            int priority = ((Integer) value).intValue();
            if (priority < 1 || priority > 9) {
               throw new XValidationException(INVALID_PRIORITY_EXCEPTION);
            }
            addToUndo();
            setPriority(data_row, new Byte((byte) priority));
            break;
         case MODE_COLUMN_INDEX:
            if (value != null) {
               int attrs = ((Integer) value).intValue();
               addToUndo();
               setAttributes(data_row, attrs);
            }
            break;
         case PAYMENT_COLUMN_INDEX: {
            if (value != null) {
               setPayment(data_row, ((Double) value).doubleValue());
            }
         }
      }
   }

   protected void updateResponsibleResource(XComponent data_row) {
      if (getResponsibleResource(data_row) == null) {
         List resources = getResources(data_row);
         if (resources.size() > 0) {
            String resource = (String) resources.get(0);
            String caption = XValidator.choiceCaption(resource);
            String resName = getResourceName(caption, null);
            String id = XValidator.choiceID(resource);
            setResponsibleResource(data_row, XValidator.choice(id, resName));
         }
      }
   }

   /**
    * Updates the has attachemt attribute on a data row based on the attachment list.
    *
    * @param data_row row on which the update is made.
    */
   public static void updateAttachmentAttribute(XComponent data_row) {
      List attachments = getAttachments(data_row);
      if (attachments.size() != 0) {
         setAttribute(data_row, HAS_ATTACHMENTS, true);
      }
      else {
         setAttribute(data_row, HAS_ATTACHMENTS, false);
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


   protected void preCheckSetStartValue(XComponent data_row, Object value) {
      if (isProjectMandatory(data_row) && ((getStart(data_row) != null && value == null) ||
           (getStart(data_row) == null && value != null))) {
         throw new XValidationException(MANDATORY_EXCEPTION);
      }
      if ((getStart(data_row) != null && value == null) || (getStart(data_row) == null && value != null)) {
         //to be changed into task/from task
         if (data_row.getOutlineLevel() != 0) {
            XComponent parent = superActivity(data_row);
            if (getChildren(parent).size() != 1) {
               throw new XValidationException(SCHEDULED_MIXED_EXCEPTION);
            }
         }
      }
   }

   protected void preCheckSetEffortValue(XComponent data_row, double base_effort) {
      if (isProjectMandatory(data_row)) {
         if ((OpGanttValidator.getType(data_row) == MILESTONE && base_effort > 0) ||
              (OpGanttValidator.getType(data_row) != MILESTONE && base_effort <= 0)) {
            throw new XValidationException(MANDATORY_EXCEPTION);
         }
      }

      if ((OpGanttValidator.getType(data_row) != MILESTONE && base_effort <= 0)) {
         //activity that will become milestone
         if (subTasks(data_row).size() != 0) {
            throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
         }
         checkDeletedAssignmentsForWorkslips(data_row, new ArrayList());
      }
   }

   protected void preCheckSetDurationValue(XComponent data_row, double duration) {
      if (isProjectMandatory(data_row) &&
           ((OpGanttValidator.getType(data_row) == MILESTONE && duration > 0) ||
                (OpGanttValidator.getType(data_row) != MILESTONE && duration <= 0))) {
         throw new XValidationException(MANDATORY_EXCEPTION);
      }
      if ((OpGanttValidator.getType(data_row) != MILESTONE && duration <= 0)) {
         //activity that will become milestone
         if (subTasks(data_row).size() != 0) {
            throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
         }
         checkDeletedAssignmentsForWorkslips(data_row, new ArrayList());
      }
   }

   protected void preCheckSetEndValue(XComponent data_row, Object value) {

      if (isProjectMandatory(data_row)) {
         if ((getEnd(data_row) != null && value == null) ||
              (getEnd(data_row) == null && value != null)) {
            throw new XValidationException(MANDATORY_EXCEPTION);
         }
         if (value != null) {
            if ((OpGanttValidator.getType(data_row) == MILESTONE) &&
                 (((Date) value).getTime() > getStart(data_row).getTime())) {
               throw new XValidationException(MANDATORY_EXCEPTION);
            }
            if ((OpGanttValidator.getType(data_row) != MILESTONE) &&
                 (((Date) value).getTime() < getStart(data_row).getTime())) {
               throw new XValidationException(MANDATORY_EXCEPTION);
            }
         }
      }

      if (getStart(data_row) != null && (OpGanttValidator.getType(data_row) != MILESTONE)) {
         if (value == null || (((Date) value).getTime() < getStart(data_row).getTime())) {
            checkDeletedAssignmentsForWorkslips(data_row, new ArrayList());
         }
      }

      if ((OpGanttValidator.getType(data_row) != MILESTONE &&
           (value != null && getStart(data_row) != null && ((Date) value).getTime() < getStart(data_row).getTime()))) {
         //activity that will become milestone
         if (subTasks(data_row).size() != 0) {
            throw new XValidationException(MILESTONE_COLLECTION_EXCEPTION);
         }
      }

      if ((getEnd(data_row) != null && value == null) || (getEnd(data_row) == null && value != null)) {
         //to be changed into task/from task
         if (data_row.getOutlineLevel() != 0) {
            XComponent parent = superActivity(data_row);
            if (getChildren(parent).size() != 1) {
               throw new XValidationException(SCHEDULED_MIXED_EXCEPTION);
            }
         }
      }

   }

   /**
    * Check and process resources before setting them from the validation method.
    *
    * @param data_row
    * @param resources list of resources to be processed
    */
   protected List prepareResources(XComponent data_row, List resources) {
      double baseEffort = getBaseEffort(data_row);
      for (int i = 0; i < resources.size(); i++) {
         String resource = (String) resources.get(i);

         //check if the resource has a negative effort assignment
         if (!(isPositiveHoursAssigned(resource) && isPositivePercentageAssigned(resource)) && baseEffort > 0) {
            throw new XValidationException(ASSIGNMENT_EXCEPTION);
         }

         //if the project has a 0 base effort than delete resource positive effort assignment
         if (baseEffort == 0) {
            resources.set(i, deleteEffortAssignment(resource));
         }
      }
      if (!isEffortBasedProject()) {
         //distribute the "remaining" effort on the resources specified only by the name
         resources = distributeBaseEffort(data_row, resources);
      }
      //for standard activities with resources assigned
      resources = convertResourcesToPercent(data_row, resources);

      if (!isEffortBasedProject()) {
         //distribute the "remaining" effort on the resources specified only by the name
         resources = planUnNamedResource(data_row, resources);
      }

      //resource validation
      List addedResources = new ArrayList();
      for (int i = 0; i < resources.size(); i++) {
         String resource = (String) resources.get(i);
         //throw exception if resource name is invalid
         if (resource == null) {
            throw new XValidationException(RESOURCE_NAME_EXCEPTION);
         }
         String resourceId = choiceID(resource);
         if (addedResources.contains(resourceId)) {
            //this resource was already added
            resources.set(i, null);
            continue;
         }
         addedResources.add(resourceId);
         double assigned = 0;
         double available = getResourceAvailability(resourceId);
         //resource must have xx% after name (only name if <available>%)
         assigned = percentageAssigned(resource);
         if (assigned == INVALID_ASSIGNMENT) {
            assigned = available;
         }
         //check assignment range [0..availability]
         if ((assigned < 0 || assigned > available) && !resourceId.equals(NO_RESOURCE_ID)) {
            throw new XValidationException(ASSIGNMENT_EXCEPTION);
         }
         //if 0% remove
         if (baseEffort != 0 || isEffortBasedProject()) {
            if (assigned == 0) {
               resources.set(i, null);
            }
         }
      }
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         String s = (String) iterator.next();
         if (s == null) {
            iterator.remove();
         }
      }

      //check for individual sum == base effort in independent case.
      if (!isEffortBasedProject()) {
         double effortSum = getIndividualEffortsSum(data_row, resources);
         if (effortSum != 0 && Math.abs(baseEffort - effortSum) > ERROR_MARGIN) {
            throw new XValidationException(EFFORTS_NOT_EQUAL_EXCEPTION);
         }
      }

      //check for removal constraints
      checkDeletedAssignmentsForWorkslips(data_row, resources);

      return resources;
   }

   /**
    * Sets the value of the predecessors field. Also checks for aditional constraints on the predecessors.
    *
    * @param value    list of predecessors (indexes)
    * @param data_row row to set the predecessors on
    */
   protected void setPredecessorsValue(Object value, XComponent data_row) {
      List predecessors = (ArrayList) value;

      // Range check
      checkIndexList(predecessors);
      predecessors = (ArrayList) removeDuplicates(predecessors);
      predecessors = removeTaskIndexes(predecessors);

      // loop detection
      linksLoop(predecessors, new Integer(data_row.getIndex()), PREDECESSORS_COLUMN_INDEX);

      List current_predecessors = getPredecessors(data_row);
      List added_predecessors = new ArrayList();
      List removed_predecessors = new ArrayList();
      compareIndexLists(current_predecessors, predecessors, added_predecessors, removed_predecessors);

      XComponent predecessor;
      List successors;
      int index;

      for (index = 0; index < removed_predecessors.size(); index++) {
         predecessor = (XComponent) (data_set
              ._getChild(((Integer) (removed_predecessors.get(index))).intValue()));
         successors = getSuccessors(predecessor);
         successors.remove(new Integer(data_row.getIndex()));
      }
      for (index = 0; index < added_predecessors.size(); index++) {
         predecessor = (XComponent) (data_set._getChild(((Integer) (added_predecessors.get(index))).intValue()));
         successors = getSuccessors(predecessor);
         successors.add(new Integer(data_row.getIndex()));
      }
      setPredecessors(data_row, predecessors);
   }

   private List removeTaskIndexes(List predecessors) {
      for (Iterator iterator = predecessors.iterator(); iterator.hasNext();) {
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
    * @param data_row row to have the successors value set
    * @return newly added succesors (the diff to old ones)  - index
    */
   protected List setSuccessorsValue(Object value, XComponent data_row) {
      List successors;
      int index;
      List predecessors;
      successors = (ArrayList) value;
      // Range check
      checkIndexList(successors);

      successors = (ArrayList) removeDuplicates(successors);
      successors = removeTaskIndexes(successors);

      // loop detection
      linksLoop(successors, new Integer(data_row.getIndex()), SUCCESSORS_COLUMN_INDEX);


      List current_successors = getSuccessors(data_row);
      List added_successors = new ArrayList();
      List removed_successors = new ArrayList();
      compareIndexLists(current_successors, successors, added_successors, removed_successors);

      XComponent successor;

      for (index = 0; index < removed_successors.size(); index++) {
         successor = (XComponent) (data_set._getChild(((Integer) (removed_successors.get(index))).intValue()));
         predecessors = getPredecessors(successor);
         predecessors.remove(new Integer(data_row.getIndex()));
      }
      for (index = 0; index < added_successors.size(); index++) {
         successor = (XComponent) (data_set._getChild(((Integer) (added_successors.get(index))).intValue()));
         predecessors = getPredecessors(successor);
         predecessors.add(new Integer(data_row.getIndex()));
      }

      setSuccessors(data_row, successors);
      return removed_successors;
   }

   /**
    * Removes the dupicates from the given list
    *
    * @param list list to be processed
    * @return list without the duplicates
    */
   protected static List removeDuplicates(List list) {
      // check for duplicates and remove them
      List newList = new ArrayList();
      for (int i = 0; i < list.size(); i++) {
         Object element = list.get(i);
         if (!newList.contains(element)) {
            newList.add(element);
         }
      }
      list = newList;
      return list;
   }

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
   private boolean hasWorkRecords(XComponent dataRow, String resourceId) {
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
    * @param data_row activity to be checked
    * @return true if activity is mandatory and is part of a project (not a template)
    */
   protected boolean isProjectMandatory(XComponent data_row) {
      return !getProjectTemplate().booleanValue() && getAttribute(data_row, MANDATORY);
   }

   /**
    * Returns the <code>resourceId</code> availability from the <code>ASSIGNMENT_SET</code>.
    *
    * @param resourceId <code>String</code> representing the resource id.
    * @return <code>byte</code> availability
    */
   public double getResourceAvailability(String resourceId) {
      XComponent assignmentRow = getAssignmentRow(resourceId);
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
      assignmentSet.addChild(createNoResourceAssignment());
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
   public void linksLoop(List links, Integer startIndex, int linkType) {
      XComponent dataSet = getDataSet();
      XComponent dataRow = (XComponent) (dataSet.getChild(startIndex.intValue()));
      List linksNow = ((XComponent) (dataRow.getChild(linkType))).getListValue();
      List linksBefore = new ArrayList();
      linksBefore.addAll(linksNow);

      // set the "to check" links
      ((XComponent) (dataRow.getChild(linkType))).setListValue(links);

      // test the data set
      boolean cycles;

      // detect cycles
      cycles = detectLoops();

      // restore initial links
      ((XComponent) (dataRow.getChild(linkType))).setListValue(linksBefore);
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
      OpGraph graph = OpActivityGraphFactory.createGraph(data_set);
      return graph.hasCycles();
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
         updateIndexList(getSuccessors(newDataSet[i]), index_map);
         updateIndexList(getPredecessors(newDataSet[i]), index_map);
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
      addToUndo();
      moveRows(dataRows, offset);
      // validate final result
      validateDataSet();
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
         List successors = new ArrayList();
         successors.addAll(getSuccessors(child));
         savedValues.add(successors);
         // predecessors
         List predecessors = new ArrayList();
         predecessors.addAll(getPredecessors(child));
         savedValues.add(predecessors);
         // save values
         initalValues.add(savedValues);
      }

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
         List successors = (ArrayList) values.get(1);
         List predecessors = (ArrayList) values.get(2);
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
    * @param data_rows an <code>XArray</code> of activities whose outline levels must be changed
    * @param offset    the offset (positive or negative value)
    * @throws XValidationException if a cycle was detected and the outline level can't be changed.
    */
   public void changeOutlineLevels(List data_rows, int offset)
        throws XValidationException {

      if (offset == 0) {
         return;
      }
      if (data_rows.size() == 0) {
         return;
      }

      XComponent row;
      int outline_level;

      List initialOutlineLevels = new ArrayList();
      addToUndo();
      try {
         for (int i = 0; i < data_rows.size(); i++) {
            row = (XComponent) (data_rows.get(i));
            initialOutlineLevels.add(new Integer(row.getOutlineLevel()));
   
            // IF the outline change is possible...
            canChangeOutline(data_rows, i, offset);
   
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
         for (int i = 0; i < data_rows.size(); i++) {
            row = (XComponent) (data_rows.get(i));
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
            row = (XComponent) (data_rows.get(i));
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
      List successors = new ArrayList();
      /* current row index */
      Integer currentRowIndex = new Integer(currentRow.getIndex());

      for (int index = currentRow.getIndex() + 1; index < dataSet.getChildCount(); index++) {
         XComponent row = (XComponent) dataSet.getChild(index);
         // predecesors of the index row
         List predecesors = getPredecessors(row);

         if (predecesors.contains(currentRowIndex)) {
            successors.add(new Integer(row.getIndex()));
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

      XView endDateCell = activityRow.getChild(END_COLUMN_INDEX);
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
      /* make a copy of the dataRows and use the elements for clipboard placing */
      List clonedDataRows = new ArrayList();
      for (int index = 0; index < dataRows.size(); index++) {
         row = ((XComponent) dataRows.get(index)).copyData();
         //copy rows business case clears the activity row value
         if (resetRowValue) {
            row.setValue(null);
         }
         clonedDataRows.add(row);
      }
      /* array of succesors */
      List succesors;
      /* array of predecessors */
      List predecessors;
      /* array of resources */
      List resources;
      /* array of resources base efforts*/
      List resourceEfforts;
      for (int i = 0; i < clonedDataRows.size(); i++) {
         row = ((XComponent) clonedDataRows.get(i));
         /* set up the succesors of the row */
         succesors = new ArrayList();
         succesors.addAll(OpGanttValidator.getSuccessors(row));
         OpGanttValidator.setSuccessors(row, succesors);

         /* set up the predecessors of the row */
         predecessors = new ArrayList();
         predecessors.addAll(OpGanttValidator.getPredecessors(row));
         OpGanttValidator.setPredecessors(row, predecessors);

         /* set up the resources of the row*/
         resources = new ArrayList();
         resources.addAll(OpGanttValidator.getResources(row));
         OpGanttValidator.setResources(row, resources);

         /* set up the resource efforts of the row*/
         resourceEfforts = new ArrayList();
         resourceEfforts.addAll(OpGanttValidator.getResourceBaseEfforts(row));
         OpGanttValidator.setResourceBaseEfforts(row, resourceEfforts);
      }
      return clonedDataRows;
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
      int startOutlineLevel = ((XComponent) selected_rows.get(0)).getOutlineLevel();

      if (!insert) {
         removeDataRows(selected_rows);
      }

      continuousAction = true;

      // normalize clipboard outline level...
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
      for (int i = 0; i < clipboard.getChildCount(); i++) {
         XComponent row = (XComponent) clipboard.getChild(i);
         updateIndexList(getSuccessors(row), newIndexes);
         updateIndexList(getPredecessors(row), newIndexes);
         if (getProgressTracked().booleanValue()) {
            //remove complete
            OpGanttValidator.setComplete(row, 0);
         }
         addDataRow(startIndex, dummy);
         children[startIndex + i] = row;
      }
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
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         row = (XComponent) dataSet.getChild(i);
         row.setOutlineLevel(row.getOutlineLevel() + offset);
      }
   }

   /**
    * Checks that a list of indexes contains only valid entries with regards to the number of data rows.
    *
    * @param array a <code>XArray</code> of <code>Integer</code> representing indexes.
    * @return <code>true</code> if the index list is valid, <code>false</code> otherwise.
    */
   public boolean checkIndexList(List array) {
      // Check index list range
      int max_value = data_set.getChildCount() - 1;
      int value = 0;
      for (int index = 0; index < array.size(); index++) {
         value = ((Integer) (array.get(index))).intValue();
         if ((value < 0) || (value > max_value)) {
            throw new XValidationException(RANGE_EXCEPTION);
         }
      }
      return true;
   }

   public final void removeIndexListValue(List array, int value) {
      Integer element = null;
      for (int index = 0; index < array.size(); index++) {
         element = (Integer) (array.get(index));
         if (element.intValue() == value) {
            array.remove(index);
            return;
         }
      }
   }

   public final boolean containsIndexListValue(List array, int value) {
      for (int index = 0; index < array.size(); index++) {
         if (((Integer) (array.get(index))).intValue() == value) {
            return true;
         }
      }
      return false;
   }

   public void compareIndexLists(List array1, List array2, List added_elements,
        List removed_elements) {
      // *** Optimization idea: XArray could "track" changes (change-log
      // w/savepoints)
      int value = 0;
      int index = 0;
      for (index = 0; index < array1.size(); index++) {
         value = ((Integer) (array1.get(index))).intValue();
         if (!containsIndexListValue(array2, value)) {
            removed_elements.add(new Integer(value));
         }
      }
      for (index = 0; index < array2.size(); index++) {
         value = ((Integer) (array2.get(index))).intValue();
         if (!containsIndexListValue(array1, value)) {
            added_elements.add(new Integer(value));
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
   private static void updateIndexListAfterAdd(List array, int elemToAddIndex) {
      for (int i = 0; i < array.size(); i++) {
         int index = ((Integer) (array.get(i))).intValue();
         if (index >= elemToAddIndex) {
            array.set(i, new Integer(index + 1));
         }
      }
   }

   /**
    * Updates the given array (Successors or Predecessors) after a remove was made in the data set. If one element from
    * the <code>array</code> is greater then <code>removedElementIndex</code> but lower than <code>end</code> the value is
    * incresead by <code>offset</code>. The removedElementIndex index will be removed from the given array.
    *
    * @param array               <code>XArray</code> of <code>Integer</code> (succesors or predecesors)
    * @param removedElementIndex the removedElementIndex index, index of the modified row.
    */
   private static void updateIndexListAfterRemove(List array, int removedElementIndex) {
      for (Iterator it = array.iterator(); it.hasNext();) {
         Integer indexElement = ((Integer) it.next());
         int index = indexElement.intValue();
         if (index == removedElementIndex) {
            it.remove();
         }
         else if (index > removedElementIndex) {
            array.set(array.indexOf(indexElement), new Integer(index - 1));
         }
      }
   }

   /**
    * Updates a <code>XArray[Integer]<code> with the values from the <code>index_map</code>.
    * An element of the <code>array</code> represents the <code>key</code> in the <code>index_map</code>.
    *
    * @param array     the <code>XArray</code>
    * @param index_map the Hashtable
    */
   public void updateIndexList(List array, Hashtable index_map) {
      Integer new_index = null;
      for (int i = 0; i < array.size(); i++) {
         new_index = (Integer) (index_map.get(array.get(i)));
         if (new_index != null) {
            array.set(i, new_index);
         }
      }
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
    * @param array     the <code>XArray</code> of <code>Integer</code> values
    * @param index_map a <code>Hashtable</code> of indexes
    */
   public void filterIndexList(List array, Hashtable index_map) {

      List forRemove = new ArrayList();
      for (int index = 0; index < array.size(); index++) {
         Integer element = (Integer) array.get(index);
         if (!index_map.containsKey(element)) {
            forRemove.add(element);
         }
      }
      for (int i = 0; i < forRemove.size(); i++) {
         array.remove(forRemove.get(i));
      }

   }

   /**
    * Updates the given data row with the values given in the array
    *
    * @param data_row row to be updated
    * @param array    the new values of the data row. The order is important:
    *                 0 - start
    *                 1 - end
    *                 2 - duration
    *                 3 - effort
    *                 4 - assignments
    */
   public void updateDataCells(XComponent data_row, List array) {

      Date start, end;
      Double duration;
      Double effort;
      List assignments;

      start = (Date) array.get(0);
      end = (Date) array.get(1);

      //old start
      Date oldStart = getStart(data_row);
      //old end
      Date oldEnd = getEnd(data_row);
      //old base effort
      double oldBaseEffort = getBaseEffort(data_row);

      if (start != null && end != null) {
         //duration
         duration = (Double) array.get(2);
         if (duration != null && duration.doubleValue() != getDuration(data_row)) {
            setDataCellValue(data_row, DURATION_COLUMN_INDEX, duration);
         }
         //effort
         effort = (Double) array.get(3);
         if (effort != null && effort.doubleValue() != oldBaseEffort) {
            setDataCellValue(data_row, BASE_EFFORT_COLUMN_INDEX, effort);
         }
      }
      else {
         //start/end are null -> update effort
         effort = (Double) array.get(3);
         if (effort != null && effort.doubleValue() != oldBaseEffort) {
            setDataCellValue(data_row, BASE_EFFORT_COLUMN_INDEX, effort);
         }
      }

      if ((start == null && oldStart != null) || (start != null && !start.equals(oldStart))) {
         setDataCellValue(data_row, START_COLUMN_INDEX, start);
      }

      if ((end == null && oldEnd != null) || (end != null && !end.equals(oldEnd))) {
         setDataCellValue(data_row, END_COLUMN_INDEX, end);
      }

      //assignments
      boolean assignments_changed = false;
      assignments = (ArrayList) array.get(4);
      if (assignments != null) {
         List old_assignments = getResources(data_row);
         if (assignments.size() != old_assignments.size()) {
            assignments_changed = true;
         }
         else {
            for (int j = 0; j < assignments.size(); j++) {
               if (!containsAssignment(data_row, (String) assignments.get(j))) {
                  assignments_changed = true;
                  break;
               }
            }
         }
         if (assignments_changed) {
            setDataCellValue(data_row, VISUAL_RESOURCES_COLUMN_INDEX, assignments);
         }
      }

   }

   private boolean containsAssignment(XComponent data_row, String assignment) {
      List assignments = getResources(data_row);
      for (int i = 0; i < assignments.size(); i++) {
         String s = (String) assignments.get(i);
         if (s.equals(assignment)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns a numerical value representing the percentage a resource is assigned, from a string.
    *
    * @param assignment a <code>String</code> containing the percentage of a assignment.
    * @return a <code>byte</code> representing the percentage a resource is assigned.
    */
   public static double percentageAssigned(String assignment) {
      String caption = XValidator.choiceCaption(assignment);
      if (percentCaption(caption)) {
         double assignmentValue;
         String captionNumber = caption.substring(caption.lastIndexOf(' ') + 1, caption.length() - 1);
         try {
            assignmentValue = Double.parseDouble(captionNumber);
         }
         catch (NumberFormatException e) {
            logger.warn(captionNumber + " is not a valid nummber", e);
            return INVALID_ASSIGNMENT;
         }
         return assignmentValue;
      }
      else {
         return INVALID_ASSIGNMENT;
      }
   }

   /**
    * Checks if the value representing the percentage a resource is assigned is a positive number.
    *
    * @param assignment a <code>String</code> containing the percentage of a assignment.
    * @return <code>true</code> if the value representing the percentage is a positive value or if a percentage is not specified
    *         <code>false</code> if the value representing the percentage is a negative value or not a valid number.
    */
   public static boolean isPositivePercentageAssigned(String assignment) {
      boolean isPositive = false;
      String caption = XValidator.choiceCaption(assignment);
      if (percentCaption(caption)) {
         double assignmentValue;
         String captionNumber = caption.substring(caption.lastIndexOf(' ') + 1, caption.length() - 1);
         try {
            assignmentValue = Double.parseDouble(captionNumber);
            if (assignmentValue >= 0) {
               isPositive = true;
            }
         }
         catch (NumberFormatException e) {
            logger.warn(captionNumber + " is not a valid number", e);
         }
         return isPositive;
      }
      else {
         return true;
      }
   }

   /**
    * Returns a numerical value representing the percentage a resource is assigned, from an i18n string (given by the user).
    *
    * @see OpGanttValidator#percentageAssigned(String)
    */
   public static double localizedPercentageAssigned(String assignment) {
      String caption = XValidator.choiceCaption(assignment);
      if (percentCaption(caption)) {
         double assignmentValue;
         String captionNumber = caption.substring(caption.lastIndexOf(' ') + 1, caption.length() - 1);
         try {
            assignmentValue = XCalendar.getDefaultCalendar().parseDouble(captionNumber);
         }
         catch (ParseException e) {
            logger.warn(captionNumber + " is not a valid number", e);
            return INVALID_ASSIGNMENT;
         }
         return assignmentValue;
      }
      else {
         return INVALID_ASSIGNMENT;
      }
   }

   /**
    * Returns a numerical value representing the hours a resource is assigned, from a string.
    *
    * @param assignment a <code>String</code> containing the percentage of a assignment.
    * @return a <code>byte</code> representing the percentage a resource is assigned.
    */
   public static double localizedHoursAssigned(String assignment) {
      String caption = XValidator.choiceCaption(assignment);
      if (hoursCaption(caption)) {
         double hoursAssigned;
         String captionNumber = caption.substring(caption.lastIndexOf(' ') + 1, caption.length() - 1);
         try {
            hoursAssigned = XCalendar.getDefaultCalendar().parseDouble(captionNumber);
         }
         catch (ParseException e) {
            logger.warn(captionNumber + " is not a valid number", e);
            return INVALID_ASSIGNMENT;
         }
         return hoursAssigned;
      }
      else {
         return INVALID_ASSIGNMENT;
      }
   }

   /**
    * Returns a numerical value representing the hours a resource is assigned, from a string.
    *
    * @param assignment a <code>String</code> containing the percentage of a assignment.
    * @return a <code>byte</code> representing the percentage a resource is assigned.
    */
   public static double hoursAssigned(String assignment) {
      String caption = XValidator.choiceCaption(assignment);
      if (hoursCaption(caption)) {
         double hoursAssigned;
         String captionNumber = caption.substring(caption.lastIndexOf(' ') + 1, caption.length() - 1);
         try {
            hoursAssigned = Double.parseDouble(captionNumber);
         }
         catch (NumberFormatException e) {
            logger.warn(captionNumber + " is not a valid number", e);
            return INVALID_ASSIGNMENT;
         }
         return hoursAssigned;
      }
      else {
         return INVALID_ASSIGNMENT;
      }
   }

   /**
    * Checks if the caption is of form '[name] [nr]h'
    *
    * @param caption
    * @return true if it is an hours caption
    */
   private static boolean hoursCaption(String caption) {
      return (caption != null) && (caption.charAt(caption.length() - 1) == HOURS_SYMBOL && caption.indexOf(' ') != -1);
   }

   private static boolean percentCaption(String caption) {
      return (caption != null) && (caption.charAt(caption.length() - 1) == PERCENT_SYMBOL);
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
      if (hoursCaption(caption)) {
         String captionNumber = caption.substring(caption.lastIndexOf(' ') + 1, caption.length() - 1);
         try {
            double hoursAssigned = Double.parseDouble(captionNumber);
            String newCaption = caption.substring(0, caption.lastIndexOf(' '));
            result = assignment.replace(caption, newCaption);
         }
         catch (NumberFormatException e) {
            logger.warn(captionNumber + " is not a valid number", e);
            return result;
         }
      }
      if (percentCaption(caption)) {
         String captionNumber = caption.substring(caption.lastIndexOf(' ') + 1, caption.length() - 1);
         try {
            double assignmentValue = Double.parseDouble(captionNumber);
            String newCaption = caption.substring(0, caption.lastIndexOf(' '));
            result = assignment.replace(caption, newCaption);
         }
         catch (NumberFormatException e) {
            logger.warn(captionNumber + " is not a valid number", e);
            return result;
         }
      }
      return result;
   }

   /**
    * Checks if the value representing the hours a resource is assigned is a positive number.
    *
    * @param assignment a <code>String</code> containing the percentage of a assignment.
    * @return <code>true</code> if the value representing the hours is a positive value or if the hours are not specified
    *         <code>false</code> if the value representing the hours is a negative value or not a valid number.
    */
   public static boolean isPositiveHoursAssigned(String assignment) {
      boolean isPositive = false;
      String caption = XValidator.choiceCaption(assignment);
      if (hoursCaption(caption)) {
         double hoursAssigned;
         String captionNumber = caption.substring(caption.lastIndexOf(' ') + 1, caption.length() - 1);
         try {
            hoursAssigned = Double.parseDouble(captionNumber);
            if (hoursAssigned >= 0) {
               isPositive = true;
            }
         }
         catch (NumberFormatException e) {
            logger.warn(captionNumber + " is not a valid number", e);
         }
         return isPositive;
      }
      else {
         return true;
      }
   }

   /**
    * @return date - start of the absences used by the validator in the validation process
    */
   public Date getAbsencesStart() {
      if (absencesStart == null) {
         XComponent form = data_set.getForm();
         absencesStart = DATE_SENTINEL;
         if (form != null) {
            XComponent absencesStartField = form.findComponent(ABSENCES_START);
            if (absencesStartField != null) {
               absencesStart = absencesStartField.getDateValue();
            }
         }
      }
      return absencesStart;
   }

   /**
    * set the start of the absences used by the validator in the validation process
    *
    * @param absencesStart
    */
   public void setAbsencesStart(Date absencesStart) {
      this.absencesStart = absencesStart;
   }


   /**
    * Computes the effort and absences values and updates the resource base efforts for an activity.
    *
    * @param activityRow             the activity row that will have its resource base efforts updated.
    * @param calendar                the calendar to be used for computing values
    * @param assignments             assignments for the activity
    * @param absences                tha absences array for the resources assigned to the activity
    * @param effort                  total effort for this activity
    * @param individualEfforts       individual efforts array to be filled
    * @param individualEffortsPerDay individual efforts / day array to be filled
    * @param individualAbsences      individual absences for resources array to be filled
    * @return personnel Cost for this activity
    */
   public List individualValues(XComponent activityRow, XCalendar calendar, List assignments,
        HashMap absences, double effort, double[] individualEfforts,
        double[] individualEffortsPerDay, List[] individualAbsences) {

      // Calculates important individual values per resource and returns personnel costs in the process
      List costs = new ArrayList();
      costs.add(INTERNAL_HOURLY_RATE_INDEX, new Double(0));
      costs.add(EXTERNAL_HOURLY_RATE_INDEX, new Double(0));
      String assignment;
      double[] assigneds = new double[individualEfforts.length];
      double sumAssigned = 0;
      int i;
      String resource_locator;
      for (i = 0; i < individualEfforts.length; i++) {
         assignment = (String) assignments.get(i);
         double assigned = percentageAssigned(assignment);
         if (assigned == INVALID_ASSIGNMENT) {
            assigned = getResourceAvailability(choiceID(assignment));
         }
         assigneds[i] = assigned;
         sumAssigned += assigneds[i];
         individualEffortsPerDay[i] = assigneds[i] * calendar.getWorkHoursPerDay() / 100;
         resource_locator = XValidator.choiceID(assignment);
         individualAbsences[i] = (ArrayList) absences.get(resource_locator);
      }
      setResourceBaseEfforts(activityRow, new ArrayList());
      for (i = 0; i < individualEfforts.length; i++) {
         individualEfforts[i] = effort * assigneds[i] / sumAssigned;
         addResourceBaseEffort(activityRow, individualEfforts[i]);
         // Update personnel costs
         assignment = (String) assignments.get(i);
         resource_locator = XValidator.choiceID(assignment);

         //update base costs (internal and external)
         List rates = calculateCostsForResourceOnActivity(resource_locator, activityRow, individualEffortsPerDay[i]);
         double newCosts = ((Double) costs.get(INTERNAL_HOURLY_RATE_INDEX)).doubleValue() +
              ((Double) rates.get(INTERNAL_HOURLY_RATE_INDEX)).doubleValue();
         costs.set(INTERNAL_HOURLY_RATE_INDEX, new Double(newCosts));
         newCosts = ((Double) costs.get(EXTERNAL_HOURLY_RATE_INDEX)).doubleValue() +
              ((Double) rates.get(EXTERNAL_HOURLY_RATE_INDEX)).doubleValue();
         costs.set(EXTERNAL_HOURLY_RATE_INDEX, new Double(newCosts));
      }

      return costs;
   }

   /**
    * Calculates the cost (internal and external) for the given resource and activity.
    * It uses the provided effort/day in order to calculate them.
    *
    * @param resource_locator Locator of the resource on the activity.
    * @param activityRow      Activity the costs are calculated for.
    * @param effortPerDay     daily effort for the resource on this activity
    * @return List of calculated costs. At INTERNAL_HOURLY_RATE_INDEX index is the internal personnel cost and at
    *         EXTERNAL_HOURLY_RATE_INDEX index there is the external costs (proceeds) value.
    */
   private List calculateCostsForResourceOnActivity(String resource_locator, XComponent activityRow, double effortPerDay) {
      List costs = new ArrayList();

      //get rate for each activity day and multiply it by the effortPerDay
      Date start = getActivityMaxStart(activityRow);
      Date end = getActivityMinEnd(activityRow);

      double internalCost = 0;
      double externalCost = 0;
      Date date = new Date(start.getTime());
      if (!getCalendar().isWorkDay(date)) {
         date = calendar.nextWorkDay(date);
      }

      while (!date.after(end)) {
         //get rate and add it
         internalCost += getRateForResource(resource_locator, INTERNAL_HOURLY_RATE_INDEX, date) * effortPerDay;
         externalCost += getRateForResource(resource_locator, EXTERNAL_HOURLY_RATE_INDEX, date) * effortPerDay;
         date = calendar.nextWorkDay(date);
      }

      costs.add(INTERNAL_HOURLY_RATE_INDEX, new Double(internalCost));
      costs.add(EXTERNAL_HOURLY_RATE_INDEX, new Double(externalCost));
      return costs;
   }

   /**
    * Tries to update the base effort for a given activity. The end date and duration are changed according to the new
    * effort. This kind of update is not possible for collections.
    *
    * @param data_row the data for this activity
    * @param effort   -
    *                 the desired new value for the effort.
    * @return true if the effort can be updated / false otherwise (for activity != STANDARD) the effort can't be
    *         updated.
    */
   public boolean updateBaseEffort(XComponent data_row, double effort) {

      if (OpGanttValidator.getType(data_row) == COLLECTION || OpGanttValidator.getType(data_row) == OpGanttValidator.SCHEDULED_TASK) {
         //effort can't be changed for collections
         return false;
      }
      if (OpGanttValidator.getType(data_row) == MILESTONE && effort > 0) {
         if (isEffortBasedProject()) {
            updateType(data_row, STANDARD);
         }
         else {
            //for not effort based projects, effort can't be changed for milestones.
            return false;
         }
      }

      // effort must be >= 0
      if (effort < 0) {
         effort = 0;
      }

      double previousEffort = getBaseEffort(data_row);
      setBaseEffort(data_row, effort);

      //for tasks only set the base effort
      if (getType(data_row) == TASK) {
         //update the base effort for the resource (if any)
         if (getResources(data_row).size() > 0) {
            List resourceEffort = new ArrayList();
            resourceEffort.add(new Double(effort));
            setResourceBaseEfforts(data_row, resourceEffort);
         }
         updateTaskCosts(data_row);
         XComponent parent = superActivity(data_row);
         if (parent != null) {
            updateTaskParentValues(parent);
         }
         return true;
      }

      // Get absences-start
      int absencesStartDay = (int) (getAbsencesStart().getTime() / XCalendar.MILLIS_PER_DAY);
      // Start with start date; get (start-)weekday
      Date start = getStart(data_row);
      long time = start.getTime();
      int day = (int) (time / XCalendar.MILLIS_PER_DAY);
      calendar.getCalendar().setTimeInMillis(time);
      Date currentDate = new Date(time);
      HashMap absences = getAbsencesSet();
      Iterator relevantHolidays = initHolidays(start);
      Date nextHoliday = null;
      if (relevantHolidays.hasNext()) {
         nextHoliday = (Date) relevantHolidays.next();
      }

      // *** Distribute effort to resources based on percentages
      // *** TODO: Highly optimizable by special XChoice data type (or use DATA_ROW instead)

      //update the % of the resources in the activity, if any
      try {
         if (previousEffort != 0) {
            if (effort != 0) {
               updatePercentage(data_row, effort / previousEffort);
            }
            else if (!isEffortBasedProject()) {
               updatePercentage(data_row, 0);
            }
         }
         else {
            //if coming from a previous effort of 0  redistribute the resources from scratch
            List resources = prepareResources(data_row, getResources(data_row));
            setResources(data_row, resources);
         }
      }
      catch (XValidationException e) {
         logger.info("Resource % > resource available. Duration will be updated.");
      }

      if (!isEffortBasedProject()) {
         List resources = getResources(data_row);
         resources = planUnNamedResource(data_row, resources);
         setResources(data_row, resources);
      }

      List assignments = getResources(data_row);
      if (assignments == null) {
         assignments = new ArrayList();
      }

      double[] individualEfforts = new double[assignments.size()];
      double[] individualEffortsPerDay = new double[individualEfforts.length];
      List[] individualAbsences = new ArrayList[individualEfforts.length];

      List personnelCosts = individualValues(data_row, calendar, assignments, absences, effort,
           individualEfforts, individualEffortsPerDay, individualAbsences);
      int durationDays = 0;
      boolean resourceAbsent = false;
      boolean workPhase = false;
      boolean workingDay;
      double workPhaseEffort = 0;

      List workPhaseStarts = new ArrayList();
      List workPhaseFinishes = new ArrayList();
      List workPhaseEfforts = new ArrayList();
      int i = 0;
      boolean effortBased = isEffortBasedProject();
      //the calculations have to be done using BigDecimals to avoid rounding errors.
      BigDecimal effortDecimal = new BigDecimal(effort);

      // *** Loop until effort is distributed
      if (effortBased) {
         while (effortDecimal.doubleValue() > ERROR_MARGIN) {
            if (calendar.isWorkDay(currentDate)) {
               if ((nextHoliday != null) && (time == nextHoliday.getTime())) {
                  nextHoliday = (Date) relevantHolidays.next();
                  workingDay = false;
               }
               else if (assignments.size() == 0) {
                  effortDecimal = effortDecimal.subtract(new BigDecimal(String.valueOf(calendar.getWorkHoursPerDay())));
                  workingDay = true;
                  workPhaseEffort += calendar.getWorkHoursPerDay();
               }
               else {
                  // *** Inner loop: Check for absences
                  if (absences != null) {
                     resourceAbsent = false;
                     for (i = 0; i < individualEfforts.length; i++) {
                        if ((individualAbsences[i] != null)
                             && ((Boolean) (individualAbsences[i].get(day - absencesStartDay))).booleanValue()) {
                           resourceAbsent = true;
                        }
                     }
                     workingDay = !resourceAbsent;
                     if (!resourceAbsent) {
                        for (i = 0; i < individualEffortsPerDay.length; i++) {
                           effortDecimal = effortDecimal.subtract(new BigDecimal(String.valueOf(individualEffortsPerDay[i])));
                           workPhaseEffort += individualEffortsPerDay[i];
                        }
                     }

                  }
                  else {
                     workingDay = true;
                     // Simple case: No absences
                     for (i = 0; i < individualEfforts.length; i++) {
                        individualEfforts[i] -= individualEffortsPerDay[i];
                        effortDecimal = effortDecimal.subtract(new BigDecimal(String.valueOf(individualEffortsPerDay[i])));
                        workPhaseEffort += individualEffortsPerDay[i];
                     }
                  }
               }
               durationDays++;
            }
            else {
               workingDay = false;
            }

            boolean oldWorkPhase = workPhase;
            workPhase = updateWorkPhases(workPhase, workingDay, time, workPhaseStarts, workPhaseFinishes, data_row, workPhaseEffort, workPhaseEfforts);
            if (oldWorkPhase != workPhase && !workPhase) {
               // a work phase was just ended
               workPhaseEffort = 0;
            }
            day++;
            time += XCalendar.MILLIS_PER_DAY;
            currentDate.setTime(time);
         }

         // if the the loop was executed at least once, the below condition should be true
         if (start.getTime() <= time - XCalendar.MILLIS_PER_DAY) {
            Date finish = new Date(time - XCalendar.MILLIS_PER_DAY);
            setEnd(data_row, finish);
            updateProjectPlanFinish();
            workingDay = false;
            updateWorkPhases(workPhase, workingDay, time, workPhaseStarts, workPhaseFinishes, data_row, workPhaseEffort, workPhaseEfforts);
         }
         else {
            setEnd(data_row, start);
            updateProjectPlanFinish();
         }
         double duration = durationDays * calendar.getWorkHoursPerDay();
         setDuration(data_row, duration);
         setWorkPhaseStarts(data_row, workPhaseStarts);
         setWorkPhaseFinishes(data_row, workPhaseFinishes);
         setWorkPhaseBaseEfforts(data_row, workPhaseEfforts);
      }
      else {
         //recompute the workpase efforts, if any workphases
         List efforts = getWorkPhaseBaseEfforts(data_row);
         if (efforts != null) {
            for (int j = 0; j < efforts.size(); j++) {
               double workEffort = ((Double) efforts.get(j)).doubleValue();
               if (previousEffort != 0) {
                  workEffort = workEffort * effort / previousEffort;
               }
               else {
                  workEffort = effort / efforts.size();
               }
               efforts.set(j, new Double(workEffort));
            }
            setWorkPhaseBaseEfforts(data_row, efforts);
         }
      }

      //update visual resources
      updateVisualResources(data_row, isHourBasedResourceView(), getAvailabilityMap());
      setBasePersonnelCosts(data_row, ((Double) personnelCosts.get(INTERNAL_HOURLY_RATE_INDEX)).doubleValue());
      setBaseProceeds(data_row, ((Double) personnelCosts.get(EXTERNAL_HOURLY_RATE_INDEX)).doubleValue());

      return true;
   }

   /**
    * Tries to update the duration for a given activity. This kind of update is not possible for collections.
    *
    * @param data_row -
    *                 the data representing the activity
    * @param duration -
    *                 the desired new duration
    * @return true if the duration can be updated / false otherwise
    */
   public boolean updateDuration(XComponent data_row, double duration) {

      // *** TODO: Return false if it is not allowed to change the duration
      // ==> Check activity mode/flags (note: Needs another column in data-set)
      // *** Loop duration (count down) until "exceeded"; calculcate i-efforts
      // ==> Add efforts only if no resource is absent (if currently no work-break)

      if (OpGanttValidator.getType(data_row) == MILESTONE && duration > 0) {
         updateType(data_row, STANDARD);
      }

      // duration can't be <0
      if (duration <= 0) {
         duration = 0;
         updateType(data_row, MILESTONE);
      }

      double previousDuration = getDuration(data_row);
      //update the % of the resources in the activity, if any
      if (!isEffortBasedProject() && getType(data_row) == STANDARD) {
         try {
            updatePercentage(data_row, previousDuration / duration);
            List resources = getResources(data_row);
            resources = planUnNamedResource(data_row, resources);
            setResources(data_row, resources);
         }
         catch (XValidationException e) {
            setDuration(data_row, previousDuration);
            throw e;
         }
      }

      // Get absences-start
      int absencesStartDay = (int) (getAbsencesStart().getTime() / XCalendar.MILLIS_PER_DAY);
      // Start with start date; get (start-)weekday
      Date start = getStart(data_row);
      long time = start.getTime();
      int day = (int) (time / XCalendar.MILLIS_PER_DAY);
      calendar.getCalendar().setTimeInMillis(time);
      Date currentDay = new Date(time);
      HashMap absences = getAbsencesSet();
      Iterator relevantHolidays = initHolidays(start);
      Date nextHoliday = null;
      if (relevantHolidays.hasNext()) {
         nextHoliday = (Date) relevantHolidays.next();
      }

      List assignments = getResources(data_row);
      if (assignments == null) {
         assignments = new ArrayList();
      }
      double[] individualEffortsPerDay = new double[assignments.size()];
      List[] individualAbsences = new ArrayList[individualEffortsPerDay.length];
      double[] assigneds = new double[individualEffortsPerDay.length];

      BigDecimal effort = new BigDecimal(0.0);
      boolean resourceAbsent;
      boolean workPhase = false;
      double workPhaseEffort = 0;
      boolean workingDay;

      List workPhaseStarts = new ArrayList();
      List workPhaseFinishes = new ArrayList();
      List workPhaseEfforts = new ArrayList();
      int i;
      double sumAssigned;

      boolean effortBased = isEffortBasedProject();
      double previousEffort = getBaseEffort(data_row);

      int durationDays = 0;
      durationDays = (int) Math.ceil(duration / calendar.getWorkHoursPerDay());
      /*int duration according to duration days */
      duration = durationDays * calendar.getWorkHoursPerDay();

      byte activityType = OpGanttValidator.getType(data_row);
      if (activityType == STANDARD) {

         setDuration(data_row, duration);
         // Work with zero effort -- if no assignments are there yet but duration is set
         sumAssigned = initAssigned(assignments, individualEffortsPerDay, assigneds, absences, individualAbsences,
              calendar);
         // *** Loop until duration is zero
         while (durationDays > 0) {
            if (calendar.isWorkDay(currentDay)) {
               if ((nextHoliday != null) && (time == nextHoliday.getTime())) {
                  nextHoliday = (Date) relevantHolidays.next();
                  workingDay = false;
               }
               else if (assignments.size() == 0) {
                  effort = effort.add(new BigDecimal(calendar.getWorkHoursPerDay()));
                  workPhaseEffort += calendar.getWorkHoursPerDay();
                  workingDay = true;
               }
               else {
                  // *** Inner loop: Check for absences
                  if (absences != null) {
                     resourceAbsent = false;
                     for (i = 0; i < individualEffortsPerDay.length; i++) {
                        if ((individualAbsences[i] != null)
                             && ((Boolean) (individualAbsences[i].get(day - absencesStartDay))).booleanValue()) {
                           resourceAbsent = true;
                        }
                     }
                     workingDay = !resourceAbsent;
                     if (!resourceAbsent) {
                        for (i = 0; i < individualEffortsPerDay.length; i++) {
                           effort = effort.add(new BigDecimal(String.valueOf(individualEffortsPerDay[i])));
                           workPhaseEffort += individualEffortsPerDay[i];
                        }
                     }
                  }
                  else {
                     workingDay = true;
                     // Simple case: No absences
                     for (i = 0; i < individualEffortsPerDay.length; i++) {
                        effort = effort.add(new BigDecimal(String.valueOf(individualEffortsPerDay[i])));
                        workPhaseEffort += individualEffortsPerDay[i];
                     }
                  }
               }
               durationDays--;
            }
            else {
               workingDay = false;
            }
            boolean oldWorkPhase = workPhase;
            workPhase = updateWorkPhases(workPhase, workingDay, time, workPhaseStarts, workPhaseFinishes, data_row, workPhaseEffort, workPhaseEfforts);
            if (oldWorkPhase != workPhase && !workPhase) {
               // a work phase was just ended
               workPhaseEffort = 0;
            }
            day++;
            time += XCalendar.MILLIS_PER_DAY;
            currentDay = new Date(time);
         }

         // if the the loop was executed at least once, the below condition should be true
         if (start.getTime() <= time - XCalendar.MILLIS_PER_DAY) {
            Date finish = new Date(time - XCalendar.MILLIS_PER_DAY);
            setEnd(data_row, finish);
            updateProjectPlanFinish();
            workingDay = false;
            updateWorkPhases(workPhase, workingDay, time, workPhaseStarts, workPhaseFinishes, data_row, workPhaseEffort, workPhaseEfforts);
         }
         else {
            setEnd(data_row, start);
            updateProjectPlanFinish();
         }

         if (effortBased) {
            setBaseEffort(data_row, effort.doubleValue());
         }

         //update the costs
         List costs = calculateCosts(data_row, individualEffortsPerDay, getBaseEffort(data_row), assigneds, sumAssigned,
              assignments);
         setBasePersonnelCosts(data_row, ((Double) costs.get(INTERNAL_HOURLY_RATE_INDEX)).doubleValue());
         setBaseProceeds(data_row, ((Double) costs.get(EXTERNAL_HOURLY_RATE_INDEX)).doubleValue());

         //recompute the work phase efforts
         recomputeWorkPhaseEffort(workPhaseEfforts, getBaseEffort(data_row), previousEffort);

         //add workphase efforts
         setWorkPhaseBaseEfforts(data_row, workPhaseEfforts);
         setWorkPhaseStarts(data_row, workPhaseStarts);
         setWorkPhaseFinishes(data_row, workPhaseFinishes);

      }
      else if (activityType == COLLECTION || activityType == SCHEDULED_TASK) {

         setDuration(data_row, duration);
         while (durationDays > 0) {
            if (calendar.isWorkDay(currentDay)) {
               if ((nextHoliday != null) && (time == nextHoliday.getTime())) {
                  nextHoliday = (Date) relevantHolidays.next();
               }
               else {
                  durationDays--;
               }
            }
            day++;
            time += XCalendar.MILLIS_PER_DAY;
            currentDay = new Date(time);
         }
         // *** Go back one day for finish date?
         Date finish = new Date(time - XCalendar.MILLIS_PER_DAY);
         setEnd(data_row, finish);
         updateProjectPlanFinish();
      }
      else if (activityType == MILESTONE) {
         setEnd(data_row, getStart(data_row));
         updateProjectPlanFinish();
      }
      //update visual resources
      updateVisualResources(data_row, isHourBasedResourceView(), getAvailabilityMap());
      return true;
   }

   private void recomputeWorkPhaseEffort(List workPhaseEfforts, double effort, double previousEffort) {
      double workPhaseEffort;
      //if not effort linked - recompute worphase efforts - workPhaseEfforts
      for (int j = 0; j < workPhaseEfforts.size(); j++) {
         workPhaseEffort = ((Double) workPhaseEfforts.get(j)).doubleValue();
         if (effort != 0) {
            workPhaseEffort = workPhaseEffort * previousEffort / effort;
         }
         else {
            workPhaseEffort = 0;
         }
         workPhaseEfforts.set(j, new Double(workPhaseEffort));
      }
   }

   /**
    * Updates the finish date for an activity. If the finish date is smaller that the start date the activity will have
    * the duration set to zero. This kind of update is not possible for collections.
    *
    * @param data_row -
    *                 tha data representing the activity
    * @param finish   -
    *                 the desired new value of the finish date
    * @return true if the value can be updated/ false otherwise
    */
   public boolean updateFinish(XComponent data_row, Date finish) {

      double duration;

      Date start = getStart(data_row);
      long time = start.getTime();
      int durationDays = 0;
      calendar.getCalendar().setTimeInMillis(time);
      Date currentDate = new Date(time);
      Iterator relevantHolidays = initHolidays(start);
      Date nextHoliday = null;
      if (relevantHolidays.hasNext()) {
         nextHoliday = (Date) relevantHolidays.next();
      }

      if (finish != null) {
         while (time <= finish.getTime()) {
            if (calendar.isWorkDay(currentDate)) {
               if ((nextHoliday != null) && (time == nextHoliday.getTime())) {
                  nextHoliday = (Date) relevantHolidays.next();
               }
               else {
                  durationDays++;
               }
            }
            time += XCalendar.MILLIS_PER_DAY;
            currentDate.setTime(time);
         }
         duration = durationDays * calendar.getWorkHoursPerDay();

         //update duration
         updateDuration(data_row, duration);
      }
      else {
         setEnd(data_row, finish);
         updateProjectPlanFinish();
      }
      return true;
   }

   /**
    * Updates the work phase status : if it was a work phase (workPhase=true) and it is a non working day, the work
    * phase will end. If it wasn't a work phase (workPhase=false) and it is a working day, the work phase will begin.
    *
    * @param workPhase         the curent state of the workBreak
    * @param time              the current time
    * @param workPhaseStarts   <code>XArray</code> with the starting dates <code>Date</code> of the workPhases.
    * @param workPhaseFinishes <code>XArray</code> with the ending dates <code>Date</code> of the workPhases.
    * @param data_row
    * @param workPhaseEffort
    * @param workPhaseEfforts
    * @return the state of workPhase. (true/false)
    */
   private boolean updateWorkPhases(boolean workPhase, boolean workingDay, long time, List workPhaseStarts,
        List workPhaseFinishes, XComponent data_row, double workPhaseEffort, List workPhaseEfforts) {

      if (workingDay && !workPhase) {
         // start a new workPhase
         if (workPhaseStarts == null) {
            workPhaseStarts = new ArrayList();
         }
         workPhaseStarts.add(new Date(time));
         workPhase = true;
      }

      if (!workingDay && workPhase) {
         // end the previous workPhase and also set the effort
         if (workPhaseFinishes == null) {
            workPhaseFinishes = new ArrayList();
         }
         // set the end as the last day from the previous work phase
         workPhaseFinishes.add(new Date(time - XCalendar.MILLIS_PER_DAY));
         workPhase = false;
         workPhaseEfforts.add(new Double(workPhaseEffort));
      }
      return workPhase;
   }

   /**
    * Initializes the holidays.
    *
    * @param start -
    *              Start date of the period of time for holidays initialization
    * @return a result <code>List</code> that has relevantHolidays iterator on position 0 and nextHoliday on position
    *         1
    */
   private Iterator initHolidays(Date start) {
      SortedSet holidays = calendar.getHolidays();
      Iterator relevantHolidays = holidays.tailSet(start).iterator();
      return relevantHolidays;
   }

   /**
    * Initializes the individual efforts and the assigned sum.
    *
    * @param assignments             <code>XArray</code> containing <code>String</code> the assignments (resources).
    * @param individualEffortsPerDay <code>double[]</code> for individual efforst per day. Its content will be changed.
    * @param assigneds               <code>byte[]</code> for the assigneds. Its content will be changed.
    * @param absences                HashMap with the absences for each resource. resource -> XArray of dates. can be otained with
    *                                getAbsencesSet().
    * @param individualAbsences      <code>XArray[]</code> for individual absences. Its content will be changed.
    * @param calendar                the calendar used
    * @return sum of assigneds
    */
   private double initAssigned(List assignments, double[] individualEffortsPerDay, double[] assigneds,
        HashMap absences, List[] individualAbsences, XCalendar calendar) {
      int i;
      double sumAssigned = 0;
      if (assignments.size() > 0) {
         String assignment = null;
         for (i = 0; i < individualEffortsPerDay.length; i++) {
            assignment = (String) assignments.get(i);
            double assigned = percentageAssigned(assignment);
            if (assigned == INVALID_ASSIGNMENT) {
               assigned = getResourceAvailability(choiceID(assignment));
            }
            assigneds[i] = assigned;
            sumAssigned += assigneds[i];
            individualEffortsPerDay[i] = assigneds[i] * calendar.getWorkHoursPerDay() / 100.0;
            individualAbsences[i] = (ArrayList) absences.get(XValidator.choiceID(assignment));
         }
      }
      return sumAssigned;
   }

   /**
    * Calculates the personal costs (internal & external).
    *
    * @param activityRow             the activity data row for wich is computed the cost
    * @param individualEffortsPerDay <code>double[]</code> for individual efforst per day.
    * @param effort                  the total effort for the activity
    * @param assigneds               <code>byte[]</code> for the assigneds (can be obtained by using initAssigned method)
    * @param sumAssigned             the total sum of assigneds (can be obtained by using initAssigned method)
    * @param assignments             <code>XArray</code> containing <code>String</code> the assignments (resources).
    * @return personnel cost
    */
   private List calculateCosts(XComponent activityRow, double[] individualEffortsPerDay, double effort,
        double[] assigneds, double sumAssigned, List assignments) {
      int i;
      double individualEffort;
      String resource_locator;
      String assignment;
      List activityCosts = new ArrayList();
      activityCosts.add(INTERNAL_HOURLY_RATE_INDEX, new Double(0));
      activityCosts.add(EXTERNAL_HOURLY_RATE_INDEX, new Double(0));
      setResourceBaseEfforts(activityRow, new ArrayList());
      for (i = 0; i < individualEffortsPerDay.length; i++) {
         individualEffort = effort * assigneds[i] / sumAssigned;
         if (getBaseEffort(activityRow) == 0) {
            individualEffort = 0;
         }
         addResourceBaseEffort(activityRow, individualEffort);
         // Update personnel costs
         assignment = (String) assignments.get(i);
         resource_locator = XValidator.choiceID(assignment);
         List costs = calculateCostsForResourceOnActivity(resource_locator, activityRow, individualEffortsPerDay[i]);
         double internalCost = ((Double) costs.get(INTERNAL_HOURLY_RATE_INDEX)).doubleValue();
         double newCosts = ((Double) activityCosts.get(INTERNAL_HOURLY_RATE_INDEX)).doubleValue() + internalCost;
         activityCosts.set(INTERNAL_HOURLY_RATE_INDEX, new Double(newCosts));
         double externalCost = ((Double) costs.get(EXTERNAL_HOURLY_RATE_INDEX)).doubleValue();
         newCosts = ((Double) activityCosts.get(EXTERNAL_HOURLY_RATE_INDEX)).doubleValue() + externalCost;
         activityCosts.set(EXTERNAL_HOURLY_RATE_INDEX, new Double(newCosts));
      }
      return activityCosts;
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
         return actualEffort * 100 / complete - actualEffort;
      }
   }

   /**
    * Computes the %complete value of an activity (standard or collection) based on the given parameters.
    *
    * @param actualSum    a <code>double</code> representing a sum of actual efforts.
    * @param baseSum      a <code>double</code> representing a sum of base efforts.
    * @param remainingSum a <code>double</code> representing a sum of remaining efforts.
    */
   public static double calculateCompleteValue(double actualSum, double baseSum, double remainingSum) {
      double result = 0;
      double predictedSum = actualSum + remainingSum;
      if (predictedSum == 0.0) {
         // TODO: check
         result = (baseSum == 0) ? 1 : 0;
      }
      else {
         result = actualSum / predictedSum;
      }
      
      return result * 100;
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
         if (!row.isDummyRow()) {
            rows.add(row);
         }
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

   /**
    * Updates the visual resources based on the data resources (convers the data resources into hour or percent view)
    *
    * @param data_row
    * @param hourBasedView
    * @param resourceAvailability
    */
   public static void updateVisualResources(XComponent data_row, boolean hourBasedView, Map resourceAvailability) {
      List visualResources;

      //resources given by getResources method will always be in % view.
      List resources = getResources(data_row);

      if (hourBasedView) {
         List converted = new ArrayList();
         for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
            String resource = (String) iterator.next();
            String caption = XValidator.choiceCaption(resource);
            String id = XValidator.choiceID(resource);
            double assigned = percentageAssigned(resource);
            double available = ((Double) resourceAvailability.get(id)).doubleValue();
            if (assigned == INVALID_ASSIGNMENT) {
               assigned = available;
            }
            double duration = OpGanttValidator.getDuration(data_row);
            double hours = assigned * duration / 100.0;
            String name = getResourceName(caption, "%");
            if (hours != 0 && assigned != available) {
               String hoursString = XCalendar.getDefaultCalendar().localizedDoubleToString(hours);
               resource = XValidator.choice(id, name + " " + hoursString + "h");
            }
            else {
               resource = XValidator.choice(id, name);
            }
            converted.add(resource);
         }
         visualResources = converted;
      }
      else {
         visualResources = new ArrayList();
         for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
            String resource = (String) iterator.next();
            String id = XValidator.choiceID(resource);
            String caption = XValidator.choiceCaption(resource);
            String resourceName = getResourceName(caption, "%");
            Double available = (Double) resourceAvailability.get(id);
            double assigned = percentageAssigned(resource);
            if (assigned == available.doubleValue() || assigned == 0.0) {
               visualResources.add(XValidator.choice(id, resourceName));
            }
            else if (assigned != INVALID_ASSIGNMENT) {
               String visualPercentage = XCalendar.getDefaultCalendar().localizedDoubleToString(assigned);
               String resourceChoice = XValidator.choice(id, resourceName + " " + visualPercentage + "%");
               visualResources.add(resourceChoice);
            }
            else {
               visualResources.add(resource);
            }
         }
      }
      setVisualResources(data_row, visualResources);
   }

   protected void convertResourcesToNameOnly(XComponent data_row, List resources) {
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
      setResources(data_row, resources);
      setVisualResources(data_row, resources);
   }

   /**
    * @param resources Resources in hour view
    * @return Resources in % view
    */
   private List convertResourcesToPercent(XComponent dataRow, List resources) {
      List converted = new ArrayList();
      double percent;
      boolean onlyName = false;
      double baseEffort = getBaseEffort(dataRow);
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         String resource = (String) iterator.next();
         if (resource == null) {
            throw new XValidationException(RESOURCE_NAME_EXCEPTION);
         }

         //check if only the name of the resource was specified in the cell
         String caption = XValidator.choiceCaption(resource);
         String id = XValidator.choiceID(resource);
         String name = getResourceName(caption, null);
         if (name.length() == caption.length()) {
            percent = getResourceAvailability(id);
            onlyName = true;
         }
         else {
            //try % after name
            name = getResourceName(caption, "%");
            if (name.length() != caption.length()) {
               //resource already in % view,
               converted.add(resource);
               continue;
            }
            else {
               //try h after name
               name = getResourceName(caption, "h");
               if (name.length() != caption.length()) {
                  double hours = hoursAssigned(resource);
                  double duration = getDuration(dataRow);
                  percent = hours * 100.0 / duration;
               }
               else {
                  //wrong caption for resource - shold not happen in the normal case
                  throw new XValidationException(RESOURCE_NAME_EXCEPTION);
               }
            }
         }

         //do not add the resource availability to resources that have
         //only the name specified in the cell and the base effort is 0
         if (onlyName && baseEffort == 0) {
            String percentString = String.valueOf(0);
            resource = XValidator.choice(id, name + " " + percentString + "%");
         }
         else {
            String percentString = String.valueOf(percent);
            resource = XValidator.choice(id, name + " " + percentString + "%");
         }
         converted.add(resource);
      }
      return converted;
   }

   private double getIndividualEffortsSum(XComponent data_row, List resources) {
      double duration = getDuration(data_row);
      double effortSum = 0;
      boolean onlyName = false;
      double baseEffort = getBaseEffort(data_row);
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         String resource = (String) iterator.next();
         double assigned = percentageAssigned(resource);

         //check if the baseEffort is 0 and if the resource has only the
         //name specified in the cell
         if (baseEffort == 0) {
            String caption = XValidator.choiceCaption(resource);
            String name = getResourceName(caption, null);
            if (name.length() == caption.length()) {
               onlyName = true;
            }
         }
         //do not calculate the effortSum only for those resources that are assigned to 0 effortBase activities
         //and have only the name specified in the cell
         if (!onlyName) {
            if (assigned == INVALID_ASSIGNMENT) {
               assigned = getResourceAvailability(choiceID(resource));
            }
            effortSum += assigned * duration / 100.0;
         }
      }
      //remove rounding errors by using the same number format for parsing
      try {
         effortSum = calendar.parseDouble(calendar.localizedDoubleToString(effortSum));
      }
      catch (ParseException e) {
         logger.error("Cannot remove rounding errors for double value:" + effortSum);
      }
      return effortSum;
   }

   private void updatePercentage(XComponent data_row, double changeFactor) {
      List resources = getResources(data_row);

      for (int j = 0; j < resources.size(); j++) {
         String resource = (String) resources.get(j);
         String id = choiceID(resource);
         String caption = choiceCaption(resource);
         String resourceName = getResourceName(caption, "%");
         double assigned = percentageAssigned(resource);
         double resourceAvailability = getResourceAvailability(id);
         if (assigned == INVALID_ASSIGNMENT) {
            assigned = resourceAvailability;
         }
         double newAssigned = changeFactor * assigned;
         //for independent planning, we redistribute the effort with an unnamed resource
         if (newAssigned > resourceAvailability && !id.equals(NO_RESOURCE_ID) && isEffortBasedProject()) {
            throw new XValidationException(ASSIGNMENT_EXCEPTION);
         }
         if (newAssigned == 0 && ((getBaseEffort(data_row) != 0) || isEffortBasedProject())) {
            resources.set(j, null);
         }
         else {
            if (newAssigned != getResourceAvailability(id)) {
               String newAssignedString = String.valueOf(newAssigned);
               resources.set(j, XValidator.choice(id, resourceName + " " + newAssignedString + "%"));
            }
            else {
               resources.set(j, XValidator.choice(id, resourceName));
            }
         }
      }

      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         String s = (String) iterator.next();
         if (s == null) {
            iterator.remove();
         }
      }
      setResources(data_row, resources);
   }


   private List distributeBaseEffort(XComponent dataRow, List resources) {
      boolean hourBasedResourceView = this.isHourBasedResourceView();
      double baseEffort = getBaseEffort(dataRow);
      List distributionIndexes = new ArrayList();
      for (int i = 0; i < resources.size(); i++) {
         String resource = (String) resources.get(i);
         double resourceEffort = INVALID_ASSIGNMENT;
         if (getBaseEffort(dataRow) > 0) {
            resourceEffort = getIndividualEffort(getDuration(dataRow), resource);
         }
         if (resourceEffort == INVALID_ASSIGNMENT || (resourceEffort == 0 && !isEffortBasedProject())) {
            distributionIndexes.add(new Integer(i));
         }
         else {
            baseEffort -= resourceEffort;
         }
      }

      List resourcesToRemove = new ArrayList();
      //all the indexes point to resource that are "name only"
      if (!distributionIndexes.isEmpty()) {
         double hours = baseEffort / distributionIndexes.size();
         for (Iterator iterator = distributionIndexes.iterator(); iterator.hasNext();) {
            Integer index = (Integer) iterator.next();
            int i = index.intValue();
            String resource = (String) resources.get(i);

            //in case of baseEffort = 0 and no hours or procentage specified in the cell
            //do not remove the resource
            if (hourBasedResourceView && hours != 0) {
               String parsedHours = String.valueOf(hours);
               resource = choice(choiceID(resource), choiceCaption(resource) + " " + parsedHours + "h");
            }
            else if (hours != 0) {
               double duration = getDuration(dataRow);
               double percent = hours * 100d / duration;
               resource = choice(choiceID(resource), choiceCaption(resource) + " " + String.valueOf(percent) + "%");
            }

            if (resource == null) {
               resourcesToRemove.add(new Integer(i));
            }
            else {
               resources.set(i, resource);
            }
         }
      }

      //remove resources with 0 hours
      Iterator it = resources.iterator();
      Integer index = new Integer(0);
      while (it.hasNext() && resourcesToRemove.size() > 0) {
         it.next();
         if (resourcesToRemove.contains(index)) {
            resourcesToRemove.remove(index);
            it.remove();
         }
         index = new Integer(index.intValue() + 1);
      }
      return resources;
   }

   private double getIndividualEffort(double duration, String resource) {
      double hours;
      double assigned;
      hours = hoursAssigned(resource);
      if (hours == INVALID_ASSIGNMENT) {
         assigned = percentageAssigned(resource);
         if (assigned != INVALID_ASSIGNMENT) {
            hours = assigned * duration / 100.0;
            return hours;
         }
         else {
            return INVALID_ASSIGNMENT;
         }
      }
      else {
         return hours;
      }
   }

   public static String getResourceName(String caption, String expectedView) {
      if (expectedView == null) {
         return caption.replaceAll(" [0-9.,]+[%h]", "");
      }
      if (expectedView.equals("h")) {
         return caption.replaceAll(" [0-9.,]+h", "");
      }
      if (expectedView.equals("%")) {
         return caption.replaceAll(" [0-9.,]+%", "");
      }
      return caption;
   }

   /**
    * Checks if the assignment set used by the validator, contains a resource with the given name.
    *
    * @param resourceName a <code>String</code> representing the name of a resource.
    * @return <code>true</code> if the assignment set contains a resource with the given
    *         name, false otherwise.
    */
   private boolean containsResource(String resourceName) {
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
   protected void parseVisualResources(List visualResources) {
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
         if (!containsResource(resourceName)) {
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

         double assignment = localizedHoursAssigned(visualResource);
         if (assignment == INVALID_ASSIGNMENT) {
            assignment = localizedPercentageAssigned(visualResource);
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
   private static XComponent createNoResourceAssignment() {
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
      result.addChild(createNoResourceAssignment());
      return result;
   }

   /**
    * Determines whether for a given activity there is remaining effort that needs to the planned from its original list
    * of assignments.
    * Precondition: all the resource assignments need to be specified in %.
    *
    * @param dataRow   a <code>XComponent(DATA_ROW)</code> representing an activity.
    * @param resources an <code>List</code> of <code>String</code> representing resource assignments.
    * @return a <code>List</code> of <code>String</code> representing an updated list of assignemts (with or without the unnamed resource).
    */
   private List planUnNamedResource(XComponent dataRow, List resources) {
      //unnamed resource doesn't exist if there are no assignments
      if (resources.size() == 0) {
         return resources;
      }

      List result = new ArrayList();
      double duration = getDuration(dataRow);
      double baseEffort = getBaseEffort(dataRow);

      double remainingResourceEffort = 0;
      double existingResourceEffort = 0;

      for (int i = 0; i < resources.size(); i++) {
         double resourceEffort = 0;

         String resourceChoice = (String) resources.get(i);
         String resourceId = XValidator.choiceID(resourceChoice);
         String resourceCaption = XValidator.choiceCaption(resourceChoice);
         String resourceName = getResourceName(resourceCaption, "%");

         double availability = getResourceAvailability(resourceId);
         double percent = percentageAssigned(resourceChoice);
         if (percent == INVALID_ASSIGNMENT) {
            percent = availability;
         }
         double percentDif = percent - availability;

         //if there is an over-assigned resource, downgrade it
         if (percentDif > 0 && !resourceId.equals(NO_RESOURCE_ID)) {
            String newPercent = String.valueOf(availability);
            String newCaption = resourceName + " " + newPercent + "%";
            String newResourceChoice = XValidator.choice(resourceId, newCaption);
            result.add(newResourceChoice);
            resourceEffort = getIndividualEffort(duration, newResourceChoice);
         }
         else if (!resourceId.equals(NO_RESOURCE_ID)) {
            result.add(resourceChoice);

            resourceEffort = getIndividualEffort(duration, resourceChoice);
            if (resourceEffort == INVALID_ASSIGNMENT) {
               resourceEffort = availability * duration / 100;
            }
         }

         existingResourceEffort += resourceEffort;
      }
      remainingResourceEffort = baseEffort - existingResourceEffort;

      //see whether we need to plan remaining effort
      if (remainingResourceEffort > ERROR_MARGIN) {
         String noResourceChoice = (String) getAssignmentRow(NO_RESOURCE_ID).getValue();
         String unnamedResourcePercent = String.valueOf(remainingResourceEffort * 100 / duration);
         String resource = choice(choiceID(noResourceChoice), choiceCaption(noResourceChoice) + " " + unnamedResourcePercent + "%");
         result.add(resource);
      }

      return result;
   }

   /**
    * Updates the costs for the given task.
    *
    * @param task a <code>XComponent</code> representing an activity of type TASK.
    */
   private void updateTaskCosts(XComponent task) {
      if (getType(task) != TASK) {
         throw new IllegalArgumentException("Invalid activity type for the method updateTaskCosts");
      }
      List resources = getResources(task);
      if (resources.size() == 0) {
         setBasePersonnelCosts(task, 0);
      }
      else {
         String resourceLocator = XValidator.choiceID((String) resources.get(0));
         Date start = getActivityMaxStart(task);
         Date end = getActivityMinEnd(task);
         List workingDays = getCalendar().getWorkingDaysFromInterval(start, end);
         int workingDaysNr = workingDays.size();
         double effortPerDay;
         if (workingDaysNr == 0) {
            effortPerDay = 0;
         }
         else {
            effortPerDay = getBaseEffort(task) / workingDaysNr;
         }
         List costs = calculateCostsForResourceOnActivity(resourceLocator, task, effortPerDay);
         setBasePersonnelCosts(task, ((Double) costs.get(INTERNAL_HOURLY_RATE_INDEX)).doubleValue());
         setBaseProceeds(task, ((Double) costs.get(EXTERNAL_HOURLY_RATE_INDEX)).doubleValue());
      }
   }

   /**
    * Gets the "not-null" end date of the activity.
    *
    * @param activityRow activity row the end is required for
    * @return the most constraining end date for the activity
    *         (e.g activity end for standard activity and scheduled task end for a child task of that scheduled task.
    *         Project start is the limit.)
    */
   private Date getActivityMinEnd(XComponent activityRow) {
      if (getEnd(activityRow) != null) {
         return getEnd(activityRow);
      }
      List parents = activityRow.getSuperRows();
      for (int i = 0; i < parents.size(); i++) {
         XComponent row = (XComponent) parents.get(i);
         Date end = getEnd(row);
         if (end != null) {
            return end;
         }
      }
      if (getProjectFinish() != null) {
         return getProjectFinish();
      }
      return getProjectPlanFinish();
   }

   /**
    * Gets the "not-null" start date of the activity.
    *
    * @param activityRow activity row the start is required for
    * @return the most constraining start date for the activity
    *         (e.g activity start for standard activity and scheduled task start for a child task of that scheduled task.
    *         Project start is the limit.)
    */
   private Date getActivityMaxStart(XComponent activityRow) {
      if (getStart(activityRow) != null) {
         return getStart(activityRow);
      }
      List parents = activityRow.getSuperRows();
      for (int i = 0; i < parents.size(); i++) {
         XComponent row = (XComponent) parents.get(i);
         Date start = getStart(row);
         if (start != null) {
            return start;
         }
      }
      return getProjectStart();
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

   public void setCalendar(XCalendar calendar) {
      this.calendar = calendar;
   }
}
