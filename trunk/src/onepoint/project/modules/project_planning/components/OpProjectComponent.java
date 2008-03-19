/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.components;

import onepoint.express.*;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.util.OpProjectConstants;
import onepoint.util.XCalendar;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Date;
import java.util.*;
import java.util.List;

public class OpProjectComponent extends XComponent {

   /**
    *
    */
   private static final long serialVersionUID = 1L;

   private static final XLog logger = XLogFactory.getClientLogger(OpProjectComponent.class);

   public final static byte GANTT_ACTIVITY = 1;
   public final static byte GANTT_DEPENDENCY = 2; // *** "GANTT_CONNECTOR"?
   public final static byte GANTT_CHART = 3; // "GANTT_CONTENT"/"GANTT_CANVAS"?
   public final static byte GANTT_HEADER = 4;
   public final static byte GANTT_BOX = 5; // "GANTT_CHART"?
   public final static byte GANTT_MAP = 6;
   public final static byte CAPTION_EDITOR = 7;
   public final static byte UTILIZATION_BOX = 8;
   public final static byte UTILIZATION_CHART = 9;
   public final static byte UTILIZATION_ROW = 10;
   public final static byte UTILIZATION_HEADER = 11;

   public final static Integer TEXT_LEFT = new Integer(256); // GanttBox
   public final static Integer TEXT_RIGHT = new Integer(257); // GanttBox
   public final static Integer TIME_SCALE = new Integer(258); // GanttChart
   public final static Integer ACTIVITY_TYPE = new Integer(259); // Gantt
   public final static Integer DATA_ROW = new Integer(260); // Gantt
   public final static Integer SOURCE = new Integer(261); // GanttDependency
   public final static Integer TARGET = new Integer(262); // GanttDependency
   public final static Integer PATH = new Integer(263); // GanttDependency
   public final static Integer DRAG_MODE = new Integer(264); // (transient)
   public final static Integer GRID_X = new Integer(265); // GanttChart
   public final static Integer GRID_Y = new Integer(266); // GanttChart
   public final static Integer START = new Integer(267); // GanttChart
   public final static Integer END = new Integer(268); // GanttChart
   public final static Integer OPEN_DEPENDENCIES = new Integer(269); // GanttChart
   public final static Integer FIRST_WORK_WEEK_LENGTH = new Integer(270); // GanttChart
   public final static Integer FIRST_WEEKEND_LENGTH = new Integer(271); // GanttChart
   public final static Integer WORK_WEEK_LENGTH = new Integer(272); // GanttChart
   public final static Integer WEEK_COUNT = new Integer(273); // GanttChart
   public final static Integer GANTT_BOX_COMPONENT = new Integer(274); // GanttMap
   public final static Integer GANTT_BOX_REF = new Integer(275); // GanttMap
   public final static Integer HISTORY_REF = new Integer(276); // GanttBox
   public final static Integer HISTORY = new Integer(277); // GanttBox
   public final static Integer OUTLINE_NUMBER = new Integer(278);
   public final static Integer TIME_UNIT = new Integer(279); // GanttChart
   public final static Integer FIRST_MONTH_LENGTH = new Integer(280);
   public final static Integer DRAGGED_COMP = new Integer(281);
   /**
    * Property that holds the ID of the drawing tool for a component with drawing abilities (like for a Gannt Chart).
    */
   public final static Integer DRAWING_TOOL = new Integer(282);
   /**
    * Property that holds the drawing rectangle for the drawing tool in a component with drawing abilities (like for a
    * Gannt Chart).
    */
   public final static Integer DRAWING_RECTANGLE = new Integer(283);
   /**
    * Property that holds a drawing line for the drawing tool in a component with drawing abilities (like a Gannt
    * Chart).
    */
   public final static Integer DRAWING_LINE = new Integer(284);
   /**
    * Property for a chart that specifies if the bounds should be increased with a certain "one-time" amount
    */
   public final static Integer COMPUTE_BOUNDS = new Integer(285);

   // properties to hold values that don't have to be calculated each time when the size of the chart is read
   public final static Integer GANTT_MAX_LEFT_CAPTION = new Integer(291);
   public final static Integer GANTT_MAX_RIGHT_CAPTION = new Integer(292);
   public final static Integer GANTT_CHART_HEIGHT = new Integer(293);
   public final static Integer UTILIZATION_CHART_VALUES = new Integer(294);

   public final static Integer GANTT_CAPTION_LEFT = new Integer(295);
   public final static Integer GANTT_CAPTION_RIGHT = new Integer(296);
   public final static Integer ON_ACTIVITY_DETAILS = new Integer(297);

   public final static Integer COLOR_INDEX = new Integer(300);
   public final static Integer CATEGORY_COLOR_SET_REF = new Integer(301);
   public final static Integer CATEGORY_COLOR_MAP = new Integer(302);

   //utilization box header time dimension property
   public final static Integer UTILIZATION_HEADER_TIME_DIMENSION = new Integer(304);
   //utilization visible details form
   public final static Integer UTILIZATION_VISIBLE_DETAILS_INTERVAL = new Integer(305);
   public final static Integer RESOURCE_TABLE_ID = new Integer(306);
   public final static Integer RESOURCE_TABLE = new Integer(307);
   public static final Integer ALTERNATE_DETAILS_FORM_REF = new Integer(308);

   /**
    * Event handler for the activitty selection event
    */
   public static final Integer ON_ACTIVITY_SELECT = new Integer(309);

   /**
    * Show costs property
    */
   public final static Integer SHOW_COSTS = new Integer(310);

   // Currently only use in OpAdvancedProjectComponent, but could be useful here too in the future
   private final static Integer VIEW_TYPE = new Integer(311);

   /*start and finish index in the details time interval list */
   private final static int INTERVAL_START_INDEX = 0;
   private final static int INTERVAL_FINISH_INDEX = 1;

   // Activity types
   public final static byte STANDARD_ACTIVITY = OpGanttValidator.STANDARD;
   public final static byte COLLECTION_ACTIVITY = OpGanttValidator.COLLECTION;
   public final static byte MILESTONE_ACTIVITY = OpGanttValidator.MILESTONE;
   public final static byte TASK_ACTIVITY = OpGanttValidator.TASK;
   public final static byte COLLECTION_TASK_ACTIVITY = OpGanttValidator.COLLECTION_TASK;
   public final static byte SCHEDULED_TASK_ACTIVITY = OpGanttValidator.SCHEDULED_TASK;

   // Drag modes
   public final static int DRAG_MOVE = 1;
   public final static int DRAG_RESIZE = 2;

   // Category data set
   public final static int CATEGORY_LOCATOR_COLUMN_INDEX = 0;
   public final static int CATEGORY_COLOR_COLUMN_INDEX = 1;

   // History data set
   public final static int START_HISTORY_COLUMN_INDEX = 0;
   public final static int END_HISTORY_COLUMN_INDEX = 1;

   // Utilization data set
   public final static int UTILIZATION_DESCRIPTOR_COLUMN_INDEX = 0;
   public final static int UTILIZATION_NAME_COLUMN_INDEX = 1;
   public final static int UTILIZATION_AVAILABLE_COLUMN_INDEX = 2;
   public final static int UTILIZATION_START_COLUMN_INDEX = 3;
   public final static int UTILIZATION_END_COLUMN_INDEX = 4;
   public final static int UTILIZATION_VALUES_COLUMN_INDEX = 5;
   public final static int UTILIZATION_ROW_ID = 6;

   public final static String UTILIZATION_POOL_DESCRIPTOR = "p";
   public final static String UTILIZATION_RESOURCE_DESCRIPTOR = "r";

   // Caption editor instance
   public static XComponent captionEditor = null;
   public static XComponent captionEditorOwner = null;

   public final static String ACTIVITY_DRAW_ITEM = "ActivityDrawItem";
   public final static String MILESTONE_DRAW_ITEM = "MilestoneDrawItem";
   public final static String TASK_DRAW_ITEM = "TaskDrawItem";
   public final static String DEPENDENCY_DRAW_ITEM = "DependencyDrawItem";
   public final static String DEFAULT_CURSOR = "NormalCursor";
   private final static String SELECTED_ACTIVITY_ARGUMENT = "Activity";

   // Default styles
   public final static String DEFAULT_GANTT_CHART_STYLE = "gantt-chart-default";
   public final static String DEFAULT_GANTT_ACTIVITY_STYLE = "gantt-activity-default";
   public final static String DEFAULT_GANTT_DEPENDENCY_STYLE = "gantt-dependency-default";
   public final static String DEFAULT_GANTT_HEADER_STYLE = "gantt-header-default";
   public final static String DEFAULT_GANTT_BOX_STYLE = "gantt-box-default";
   public final static String DEFAULT_GANTT_MAP_STYLE = "gantt-map-default";

   public final static String DEFAULT_UTILIZATION_CHART_STYLE = "utilization-chart-default";
   public final static String DEFAULT_UTILIZATION_ROW_STYLE = "utilization-row-default";
   public final static String DEFAULT_UTILIZATION_HEADER_STYLE = "utilization-header-default";
   public final static String DEFAULT_UTILIZATION_BOX_STYLE = "utilization-box-default";

   public final static XStyle DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_GANTT_DEPENDENCY_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_GANTT_HEADER_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_GANTT_BOX_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_GANTT_MAP_STYLE_ATTRIBUTES;

   public final static XStyle DEFAULT_UTILIZATION_CHART_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_UTILIZATION_ROW_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_UTILIZATION_HEADER_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_UTILIZATION_BOX_STYLE_ATTRIBUTES;

   // detail form attributes, also used for captions for a gantt activity
   private static final String DETAILS_NAME = "ActivityName";
   private static final String DETAILS_START = "Start";
   private static final String DETAILS_FINISH = "Finish";
   private static final String DETAILS_DURATION = "Duration";
   private static final String DETAILS_COMPLETE = "Complete";
   private static final String DETAILS_BASE_EFFORT = "BaseEffort";
   private static final String DETAILS_COST = "BaseCosts";
   private static final String DETAILS_COST_LABEL = "BaseCostsLabel";
   private static final String DETAILS_RESOURCE_NAMES = "ResourceNames";
   private static final String DETAILS_CATEGORY = "Category";
   private static final String DETAILS_PROCEEDS = "ProceedsCosts";
   private static final String DETAILS_PROCEEDS_LABEL = "ProceedsCostsLabel";

   static {
      // Default Gantt chart style
      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES.border_light = XStyle.DEFAULT_BORDER;
      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES.border_dark = XStyle.DEFAULT_BORDER_ALTERNATE;
      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES.alternate_background = XStyle.DEFAULT_ROW_ALTERNATE;
      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES.selection_background = XStyle.DEFAULT_HIGHLIGHTING;
      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES.selection_alternate_background = XStyle.DEFAULT_HIGHLIGHTING_ALTERNATE;
      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES.selection_gradient = XDisplay.gradient(XStyle.LINEAR_GRADIENT, HORIZONTAL,
      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES.selection_background,
      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES.selection_alternate_background);

      DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES.gap = 8; // Captions
      addDefaultStyle(DEFAULT_GANTT_CHART_STYLE, DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES);
      // Default Gantt task style
      DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES.alternate_background = XStyle.DEFAULT_BUTTON_ALTERNATE;
      DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES.border_light = XStyle.DEFAULT_SHADOW;
      DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES.border_dark = XStyle.DEFAULT_SHADOW_ALTERNATE;
      // DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES.border_light = XStyle.DEFAULT_GRAY1;
      // DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES.border_dark = XStyle.DEFAULT_BLACK;
      // DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES.selection_foreground =
      // Color.ORANGE;
      DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES.top = 2;
      DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES.bottom = 2;
      DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES.gap = 4;
      addDefaultStyle(DEFAULT_GANTT_ACTIVITY_STYLE, DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES);

      // Default Gantt dependency style
      DEFAULT_GANTT_DEPENDENCY_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_GANTT_DEPENDENCY_STYLE_ATTRIBUTES.foreground = XStyle.DEFAULT_SHADOW;
      // DEFAULT_GANTT_DEPENDENCY_STYLE_ATTRIBUTES.selection_foreground =
      // XStyle.DEFAULT_LIGHT_CYAN_COLOR;
      addDefaultStyle(DEFAULT_GANTT_DEPENDENCY_STYLE, DEFAULT_GANTT_DEPENDENCY_STYLE_ATTRIBUTES);
      // Default Gantt header style
      DEFAULT_GANTT_HEADER_STYLE_ATTRIBUTES = new XStyle(XExtendedComponent.DEFAULT_TABLE_COLUMN_STYLE_ATTRIBUTES);
      DEFAULT_GANTT_HEADER_STYLE_ATTRIBUTES.left = 0;
      DEFAULT_GANTT_HEADER_STYLE_ATTRIBUTES.right = 0;
      DEFAULT_GANTT_HEADER_STYLE_ATTRIBUTES.gap = 1;
      // DEFAULT_GANTT_HEADER_STYLE_ATTRIBUTES.font = XStyle.DEFAULT_PLAIN_FONT;
      addDefaultStyle(DEFAULT_GANTT_HEADER_STYLE, DEFAULT_GANTT_HEADER_STYLE_ATTRIBUTES);
      // Default Gantt box style
      DEFAULT_GANTT_BOX_STYLE_ATTRIBUTES = DEFAULT_LIST_BOX_STYLE_ATTRIBUTES;
      DEFAULT_GANTT_BOX_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_HEADER;
      addDefaultStyle(DEFAULT_GANTT_BOX_STYLE, DEFAULT_GANTT_BOX_STYLE_ATTRIBUTES);
      // Default Gantt map style
      DEFAULT_GANTT_MAP_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_GANTT_MAP_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_GANTT_MAP_STYLE_ATTRIBUTES.foreground = XStyle.DEFAULT_TEXT;
      addDefaultStyle(DEFAULT_GANTT_MAP_STYLE, DEFAULT_GANTT_MAP_STYLE_ATTRIBUTES);

      // Default Utilization chart style
      DEFAULT_UTILIZATION_CHART_STYLE_ATTRIBUTES = new XStyle(DEFAULT_GANTT_CHART_STYLE_ATTRIBUTES);
      DEFAULT_UTILIZATION_CHART_STYLE_ATTRIBUTES.gap = 0;
      addDefaultStyle(DEFAULT_UTILIZATION_CHART_STYLE, DEFAULT_UTILIZATION_CHART_STYLE_ATTRIBUTES);

      // Default Utilization row style
      DEFAULT_UTILIZATION_ROW_STYLE_ATTRIBUTES = new XStyle(DEFAULT_GANTT_ACTIVITY_STYLE_ATTRIBUTES);
      DEFAULT_UTILIZATION_ROW_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_UTILIZATION_ROW_STYLE_ATTRIBUTES.top = 3;
      DEFAULT_UTILIZATION_ROW_STYLE_ATTRIBUTES.bottom = 2;
      DEFAULT_UTILIZATION_ROW_STYLE_ATTRIBUTES.gap = 0;
      addDefaultStyle(DEFAULT_UTILIZATION_ROW_STYLE, DEFAULT_UTILIZATION_ROW_STYLE_ATTRIBUTES);

      // Default Utilization header style
      DEFAULT_UTILIZATION_HEADER_STYLE_ATTRIBUTES = DEFAULT_GANTT_HEADER_STYLE_ATTRIBUTES;
      // DEFAULT_GANTT_HEADER_STYLE_ATTRIBUTES.font = XStyle.DEFAULT_PLAIN_FONT;
      addDefaultStyle(DEFAULT_UTILIZATION_HEADER_STYLE, DEFAULT_UTILIZATION_HEADER_STYLE_ATTRIBUTES);
      // Default Utilization box style
      DEFAULT_UTILIZATION_BOX_STYLE_ATTRIBUTES = DEFAULT_LIST_BOX_STYLE_ATTRIBUTES;
      addDefaultStyle(DEFAULT_UTILIZATION_BOX_STYLE, DEFAULT_UTILIZATION_BOX_STYLE_ATTRIBUTES);
   }

   /**
    * Type of the project component
    */
   protected byte pcType = 0;

   /**
    * Used by activity component (gantt and wsb) to know if an insert border should be drawn around the activity
    */
   protected transient boolean drawActivityBorder;

   /**
    * insert line on a chart component (when an activity is moved on the chart)
    */
   private transient Line2D insertLine;

   /**
    * The position indexes from the gantt chart mapped to the indexes in the data set. (used because the
    * tasks/collection task will not show up in the gantt)
    */
   private transient Map ganttIndexes;

   /**
    * Area for a project component [ Computed at layout time ]
    */
   private transient Polygon componentShape;

   /**
    * Gets the componentShape for a specific project component
    *
    * @return componentShape for this project component
    */
   private Polygon getComponentPolygon() {
      if (pcType != GANTT_ACTIVITY) {
         throw new UnsupportedOperationException("Area can anly be obtained on project components");
      }
      return componentShape;
   }

   private Map getGanttIndexes() {
      if (pcType != GANTT_CHART) {
         throw new UnsupportedOperationException("Gantt indexes can only be called on gantt chart component");
      }
      return ganttIndexes;
   }

   protected boolean isChartActivity() {
      return (pcType == GANTT_ACTIVITY);
   }

   protected boolean isChartComponent() {
      return (pcType == GANTT_CHART);
   }

   protected boolean getDrawActivityBorder() {
      if (!this.isChartActivity()) {
         throw new UnsupportedOperationException("getDrawActivityBorder can only be called on a chart activity component");
      }
      return drawActivityBorder;
   }

   protected Line2D getInsertLine() {
      if (!isChartComponent()) {
         throw new UnsupportedOperationException("getInsertLine can only be called on a chart component");
      }
      return insertLine;
   }

   protected void setDrawActivityBorder(boolean drawActivityBorder) {
      if (!this.isChartActivity()) {
         throw new UnsupportedOperationException("setDrawActivityBorder can only be called on a chart activity component");
      }
      this.drawActivityBorder = drawActivityBorder;
   }

   protected void setInsertLine(Line2D insertLine) {
      if (!this.isChartComponent()) {
         throw new UnsupportedOperationException("setInsertLine can only be called on a chart component");
      }
      this.insertLine = insertLine;
   }

   public OpProjectComponent() {
      super(XComponent.PANEL);
   }

   public OpProjectComponent(byte type) {
      super(XComponent.PANEL);
      initializeProjectComponent(type);
   }

   public final int getComponentType() {
      return pcType;
   }


   protected void initializeProjectComponent(byte type) {
      setFocusable(true);
      pcType = type;
      switch (pcType) {
         case GANTT_CHART:
            setStyle(DEFAULT_GANTT_CHART_STYLE);
            setFocusable(false); // *** really?
            setGridX(16);
            setGridY(16);
            setTimeUnit(XCalendar.DAYS);
            setOpenDependencies(new ArrayList());
            break;
         case GANTT_ACTIVITY:
            setStyle(DEFAULT_GANTT_ACTIVITY_STYLE);
            // *** Use associated data-row (even for EXPANDED-property)
            break;
         case GANTT_DEPENDENCY:
            setStyle(DEFAULT_GANTT_DEPENDENCY_STYLE);
            // setFocusable(true);
            // Use shape mode for overriding pointer-inside test
            setShape(true);
            break;
         case GANTT_HEADER:
            setStyle(DEFAULT_GANTT_HEADER_STYLE);
            setFocusable(false);
            break;
         case GANTT_BOX:
            setStyle(DEFAULT_GANTT_BOX_STYLE);
            setGridX(16);
            setGridY(16);
            setTimeUnit(XCalendar.DAYS);
            XComponent view_port = initializeScrollBox();
            OpProjectComponent gantt_chart = new OpProjectComponent(GANTT_CHART);
            // *** Should be configurable
            view_port.addChild(gantt_chart);
            XComponent scrolling_header = new XComponent(XComponent.SCROLLING_PANEL);
            scrolling_header.setX(0);
            scrolling_header.setY(-1);
            _addChild(scrolling_header);
            OpProjectComponent gantt_header = new OpProjectComponent(GANTT_HEADER);
            scrolling_header.addChild(gantt_header);
            setEditMode(false);
            break;
         case GANTT_MAP:
            setStyle(DEFAULT_GANTT_MAP_STYLE);
            break;
         case CAPTION_EDITOR:
            initialize(FORM);
            setFocusable(true);
            setLayout("border");
            // *** Use string constant or try to use protected setLayout()
            // ==> In addition: Rename setLayout() to _setLayoutManager()
            XComponent text_overlay = new XComponent(TEXT_OVERLAY);
            addChild(text_overlay);
            registerEventHandler(this, COMPONENT_EVENT);
            setVisible(false);
            break;
         case UTILIZATION_CHART:
            setStyle(DEFAULT_UTILIZATION_CHART_STYLE);
            setFocusable(false); // *** really?
            setGridX(16);
            setTimeUnit(XCalendar.DAYS);
            break;
         case UTILIZATION_ROW:
            setStyle(DEFAULT_GANTT_ACTIVITY_STYLE);
            setFocusable(false);
            break;
         case UTILIZATION_HEADER:
            setStyle(DEFAULT_UTILIZATION_HEADER_STYLE);
            setFocusable(false);
            break;
         case UTILIZATION_BOX:
            setStyle(DEFAULT_UTILIZATION_BOX_STYLE);
            setGridX(16);
            setGridY(13);
            setTimeUnit(XCalendar.DAYS);
            OpProjectComponent utilization_chart = new OpProjectComponent(UTILIZATION_CHART);
            // *** Should be configurable
            initializeScrollBox().addChild(utilization_chart);
            scrolling_header = new XComponent(XComponent.SCROLLING_PANEL);
            scrolling_header.setX(0);
            scrolling_header.setY(-1);
            _addChild(scrolling_header);
            OpProjectComponent utilization_header = new OpProjectComponent(UTILIZATION_HEADER);
            scrolling_header.addChild(utilization_header);
            setEditMode(false);
            break;
      }
   }

   public final void setTextLeft(String text_left) {
      setProperty(TEXT_LEFT, text_left);
   }

   public final String getTextLeft() {
      return (String) getProperty(TEXT_LEFT);
   }

   public final void setTextRight(String text_right) {
      setProperty(TEXT_RIGHT, text_right);
   }

   public final String getTextRight() {
      return (String) getProperty(TEXT_RIGHT);
   }

   public final void setResourceTableId(String resourceTableId) {
      setProperty(RESOURCE_TABLE_ID, resourceTableId);
   }

   public final String getResourceTableId() {
      return (String) getProperty(RESOURCE_TABLE_ID);
   }

   public final void setResourceTable(XComponent resourceTableId) {
      setProperty(RESOURCE_TABLE, resourceTableId);
   }

   public final XComponent getResourceTable() {
      return (XComponent) getProperty(RESOURCE_TABLE);
   }

   public final void setAlternateDetailsFormRef(String formRef) {
      setProperty(ALTERNATE_DETAILS_FORM_REF, formRef);
   }

   public final String getAlternateDetailsFormRef() {
      return (String) getProperty(ALTERNATE_DETAILS_FORM_REF);
   }

   public final void setDragMode(int drag_mode) {
      setProperty(DRAG_MODE, new Integer(drag_mode));
   }

   public final int getDragMode() {
      return ((Integer) getProperty(DRAG_MODE)).intValue();
   }

   public final void setSource(OpProjectComponent source) {
      setProperty(SOURCE, source);
   }

   public final OpProjectComponent getSource() {
      return (OpProjectComponent) getProperty(SOURCE);
   }

   public final void setTarget(OpProjectComponent target) {
      setProperty(TARGET, target);
   }

   public final OpProjectComponent getTarget() {
      return (OpProjectComponent) getProperty(TARGET);
   }

   public final void setPath(Point[] path) {
      setProperty(PATH, path);
   }

   public final Point[] getPath() {
      return (Point[]) getProperty(PATH);
   }

   public final void setDataRow(XComponent data_row) {
      setProperty(DATA_ROW, data_row);
   }

   public final XComponent getDataRow() {
      return (XComponent) getProperty(DATA_ROW);
   }

   public final void setActivityType(int activity_type) {
      setProperty(ACTIVITY_TYPE, new Integer(activity_type));
   }

   public final int getActivityType() {
      return ((Integer) getProperty(ACTIVITY_TYPE)).intValue();
   }

   public final void setGridX(int grid_x) {
      setProperty(GRID_X, new Integer(grid_x));
   }

   public final int getGridX() {
      return ((Integer) getProperty(GRID_X)).intValue();
   }

   public final void setGridY(int grid_y) {
      setProperty(GRID_Y, new Integer(grid_y));
   }

   public final int getGridY() {
      return ((Integer) getProperty(GRID_Y)).intValue();
   }

   public final void setStart(Date start) {
      setProperty(START, start);
   }

   public final Date getStart() {
      return (Date) getProperty(START);
   }

   public final void setEnd(Date end) {
      setProperty(END, end);
   }

   public final Date getEnd() {
      return (Date) getProperty(END);
   }

   public final void setOpenDependencies(ArrayList open_dependencies) {
      setProperty(OPEN_DEPENDENCIES, open_dependencies);
   }

   public final ArrayList getOpenDependencies() {
      return (ArrayList) getProperty(OPEN_DEPENDENCIES);
   }

   public final void setWorkWeekLength(int work_week_length) {
      setProperty(WORK_WEEK_LENGTH, new Integer(work_week_length));
   }

   public final int getWorkWeekLength() {
      return ((Integer) getProperty(WORK_WEEK_LENGTH)).intValue();
   }

   public final void setFirstWorkWeekLength(int first_work_week_length) {
      setProperty(FIRST_WORK_WEEK_LENGTH, new Integer(first_work_week_length));
   }

   public final int getFirstWorkWeekLength() {
      return ((Integer) getProperty(FIRST_WORK_WEEK_LENGTH)).intValue();
   }

   public final void setFirstWeekendLength(int first_weekend_length) {
      setProperty(FIRST_WEEKEND_LENGTH, new Integer(first_weekend_length));
   }

   public final int getFirstWeekendLength() {
      return ((Integer) getProperty(FIRST_WEEKEND_LENGTH)).intValue();
   }

   public final void setWeekCount(int week_count) {
      setProperty(WEEK_COUNT, new Integer(week_count));
   }

   public final int getWeekCount() {
      return ((Integer) getProperty(WEEK_COUNT)).intValue();
   }

   public final void setFirstMonthLength(int first_month_length) {
      setProperty(FIRST_MONTH_LENGTH, new Integer(first_month_length));
   }

   public final int getFirstMonthLength() {
      return ((Integer) getProperty(FIRST_MONTH_LENGTH)).intValue();
   }

   public final void setGanttBoxRef(String gantt_box_ref) {
      setProperty(GANTT_BOX_REF, gantt_box_ref);
      setProperty(GANTT_BOX_COMPONENT, null);
   }

   public final String getGanttBoxRef() {
      return (String) getProperty(GANTT_BOX_REF);
   }

   public final String getOnActivitySelect() {
      return (String) getProperty(ON_ACTIVITY_SELECT);
   }

   public final void setOnActivitySelect(String onActivitySelectHandler) {
      this.setProperty(ON_ACTIVITY_SELECT, onActivitySelectHandler);
   }

   public final OpProjectComponent getGanttBoxComponent() {
      // *** Helper method _resolveIDReference() -- maybe even w/class-argument
      // for instanceof-checking?
      OpProjectComponent gantt_box_component = (OpProjectComponent) getProperty(GANTT_BOX_COMPONENT);
      if (gantt_box_component == null) {
         XComponent form = getForm();
         String gantt_box_ref = getGanttBoxRef();
         if ((form != null) && (gantt_box_ref != null)) {
            // *** To do: Check instanceof OpProjectComponent?
            gantt_box_component = (OpProjectComponent) (form.findComponent(gantt_box_ref));
            setProperty(GANTT_BOX_COMPONENT, gantt_box_component);
         }
      }
      return gantt_box_component;
   }

   public final void setHistoryRef(String history_ref) {
      setProperty(HISTORY_REF, history_ref);
      setProperty(HISTORY, null);
   }

   public final String getHistoryRef() {
      return (String) getProperty(HISTORY_REF);
   }

   public final void setCategoryColorSetRef(String dataSetRef) {
      setProperty(CATEGORY_COLOR_SET_REF, dataSetRef);
      setCategoryColorMap(null);
   }

   public final String getCategoryColorSetRef() {
      return (String) getProperty(CATEGORY_COLOR_SET_REF);
   }

   public final void setCaptionLeft(String history_ref) {
      setProperty(GANTT_CAPTION_LEFT, history_ref);
   }

   public final String getCaptionLeft() {
      return (String) getProperty(GANTT_CAPTION_LEFT);
   }

   public final void setCaptionRight(String history_ref) {
      setProperty(GANTT_CAPTION_RIGHT, history_ref);
   }

   public final String getCaptionRight() {
      return (String) getProperty(GANTT_CAPTION_RIGHT);
   }

   public final XComponent getHistory() {
      // *** Helper method _resolveIDReference() -- maybe even w/class-argument
      // for instanceof-checking?
      XComponent history = (XComponent) getProperty(HISTORY);
      if (history == null) {
         XComponent form = getForm();
         String history_ref = getHistoryRef();
         if ((form != null) && (history_ref != null)) {
            // *** To do: Check instanceof OpProjectComponent?
            history = form.findComponent(history_ref);
            setProperty(HISTORY, history);
         }
      }
      return history;
   }

   public final void setOutlineNumber(String outline_number) {
      setProperty(OUTLINE_NUMBER, outline_number);
   }

   public final String getOutlineNumber() {
      return (String) getProperty(OUTLINE_NUMBER);
   }

   public final void setTimeUnit(byte time_unit) {
      setProperty(TIME_UNIT, new Byte(time_unit));
   }

   public final byte getTimeUnit() {
      return ((Byte) getProperty(TIME_UNIT)).byteValue();
   }

   public final void setOnActivityDetails(String handlerName) {
      setProperty(ON_ACTIVITY_DETAILS, handlerName);
   }

   public final String getOnActivityDetails() {
      return (String) getProperty(ON_ACTIVITY_DETAILS);
   }

   protected XComponent _getCaptionEditor() {
      // Create caption-editor from standard components on-demand
      if (captionEditor == null) {
         captionEditor = new OpProjectComponent(CAPTION_EDITOR);
      }
      return captionEditor;
   }

   public Integer getCachedHeight() {
      return (Integer) getProperty(GANTT_CHART_HEIGHT);
   }

   public void setCachedHeight(Integer value) {
      setProperty(GANTT_CHART_HEIGHT, value);
   }

   public Integer getCachedLeftCaptionLenght() {
      return (Integer) getProperty(GANTT_MAX_LEFT_CAPTION);
   }

   public void setCachedLeftCaptionLenght(Integer value) {
      setProperty(GANTT_MAX_LEFT_CAPTION, value);
   }

   public Integer getCachedRightCaptionLenght() {
      return (Integer) getProperty(GANTT_MAX_RIGHT_CAPTION);
   }

   public void setCachedRightCaptionLenght(Integer value) {
      setProperty(GANTT_MAX_RIGHT_CAPTION, value);
   }

   public Map getCachedUtilizationChartValues() {
      return (Map) getProperty(UTILIZATION_CHART_VALUES);
   }

   public void setCachedUtilizationChartValues(Map value) {
      setProperty(UTILIZATION_CHART_VALUES, value);
   }

   public void setDrawingRectangle(Rectangle value) {
      setProperty(DRAWING_RECTANGLE, value);
   }

   public Rectangle getDrawingRectangle() {
      return (Rectangle) getProperty(DRAWING_RECTANGLE);
   }

   public void setDrawingLine(Line2D value) {
      setProperty(DRAWING_LINE, value);
   }

   public Line2D getDrawingLine() {
      return (Line2D) getProperty(DRAWING_LINE);
   }

   public void setDraggedComponent(OpProjectComponent value) {
      setProperty(DRAGGED_COMP, value);
   }

   public OpProjectComponent getDraggedComponent() {
      return (OpProjectComponent) getProperty(DRAGGED_COMP);
   }

   public void setDrawingToolId(String value) {
      setProperty(DRAWING_TOOL, value);
   }

   public String getDrawingToolId() {
      return (String) getProperty(DRAWING_TOOL);
   }

   public final void setColorIndex(Integer colorIndex) {
      setProperty(COLOR_INDEX, colorIndex);
   }

   public final Integer getColorIndex() {
      return (Integer) getProperty(COLOR_INDEX);
   }

   protected Map categoryColorMap() {
      //GanttBox: Cache category-color-set-ref data-set in a hash map property
      Map categoryColorMap = getCategoryColorMap();
      if (categoryColorMap == null) {
         String categoryColorSetRef = getCategoryColorSetRef();
         if (categoryColorSetRef != null) {
            XComponent dataSet = getForm().findComponent(categoryColorSetRef);
            if (dataSet != null) {
               categoryColorMap = new HashMap();
               XComponent dataRow = null;
               String locator = null;
               Integer colorIndex = null;
               for (int i = 0; i < dataSet.getChildCount(); i++) {
                  dataRow = (XComponent) dataSet.getChild(i);
                  locator = ((XComponent) dataRow.getChild(CATEGORY_LOCATOR_COLUMN_INDEX)).getStringValue();
                  colorIndex = (Integer) ((XComponent) dataRow.getChild(CATEGORY_COLOR_COLUMN_INDEX)).getValue();
                  categoryColorMap.put(locator, colorIndex);
               }
               setCategoryColorMap(categoryColorMap);
            }
         }
      }
      return categoryColorMap;
   }

   public void setViewType(int viewType) {
      setProperty(VIEW_TYPE, new Integer(viewType));
   }

   public int getViewType() {
      return ((Integer) getProperty(VIEW_TYPE)).intValue();
   }

   /**
    * Will open a caption Editor for this component (e.g. activity). Must be called in component context.
    */
   protected void _openCaptionEditor(String text) {

      if ((text == null)) {
         text = "";
      }

      if (captionEditor == null) {
         _getCaptionEditor();
      }
      if (!captionEditor.getVisible()) {
         captionEditor.setVisible(true);
         XComponent line_editor = (XComponent) (captionEditor._getChild(0)._getChild(0));
         line_editor.setSelectionStart(0);
         line_editor.setSelectionEnd(text.length());
         line_editor.setStringValue(text);

         captionEditor.setSelectionStart(0);
         captionEditor.setSelectionEnd(text.length());

         captionEditor.registerEventHandler(this, COMPONENT_EVENT);
         captionEditorOwner = this;
         XComponent viewPort = getContext().getViewPort();
         XComponent verticalScrollBar = (XComponent) getContext().getChild(VERTICAL_SCROLL_BAR_INDEX);
         int scrollWidth = 0;
         if (verticalScrollBar.getBounds() != null) {
            scrollWidth = verticalScrollBar.getBounds().width;
         }
         // x coordinate of the caption editor
         int x = getBounds().x;

         int ganttChartPosition = Math.abs(getParent().getBounds().x);
         // x relative to ganttchart position
         if (x < ganttChartPosition) {
            x = ganttChartPosition;
         }
         // the open position of the caption + caption editor.width oversteps the viewport
         if (captionEditor.getBounds() != null) {
            int decrement = x + captionEditor.getBounds().width
                 - (ganttChartPosition + viewPort.getBounds().width - scrollWidth);
            if (decrement > 0) {
               x -= decrement;
            }
         }
         Point absolutePoint = getParent().absolutePosition(x, getBounds().y);
         // <FIXME author="Horia Chiorean" description="For some reason, only forms may have the property below set =>
         // we need to set it here.">
         captionEditorOwner.setFocused(false);
         captionEditor.setFocusedView(captionEditor);
         // <FIXME>
         getDisplay().openLayer(captionEditor, absolutePoint.x, absolutePoint.y, true);
         // captionEditor.requestFocus();
         captionEditor.repaint();
         // *** Maybe make this an event-handler for caption-editor
         // ==> We could then change our text when editor is closed
      }
   }

   protected void _closeCaptionEditor() {
      if (captionEditor.getVisible()) {
         getDisplay().closeLayer(captionEditor);
         captionEditor.unregisterEventHandler(captionEditorOwner);
         captionEditor.setVisible(false);
         captionEditorOwner = null;
      }
   }

   // *** Note that the following algorithm does not scale
   // ==> Maybe have polygons as shapes registered and x/y hashtables?

   public boolean insideShape(Rectangle bounds, int x, int y) {
      if (pcType == GANTT_DEPENDENCY) {
         Point[] path = getPath();
         Point p1 = null;
         Point p2 = null;
         for (int index = 0; index < path.length - 1; index++) {
            p1 = path[index];
            p2 = path[index + 1];
            if (p1.x == p2.x) {
               // Case 1: Vertical line
               if ((x > p1.x - 4) && (x < p1.x + 4)) {
                  if (p1.y < p2.y) {
                     if ((y >= p1.y) && (y <= p2.y)) {
                        return true;
                     }
                  }
                  else {
                     if ((y >= p2.y) && (y <= p1.y)) {
                        return true;
                     }
                  }
               }
            }
            else {
               // Case 2: Horizontal line
               if ((y > p1.y - 4) && (y < p1.y + 4)) {
                  if (p1.x < p2.x) {
                     if ((x >= p1.x) && (x <= p2.x)) {
                        return true;
                     }
                  }
                  else {
                     if ((x >= p2.x) && (x <= p1.x)) {
                        return true;
                     }
                  }
               }
            }
         }
         return false;
      }
      else {
         return super.insideShape(bounds, x, y);
      }
   }

   public XComponent getContext() {
      switch (pcType) {
         case GANTT_ACTIVITY:
         case GANTT_DEPENDENCY:
         case GANTT_CHART:
         case GANTT_HEADER:
            // Context is always the GanttBox
            XComponent component = (XComponent) getParent();
            while (component != null) {
               if ((component instanceof OpProjectComponent)
                    && (((OpProjectComponent) component).getComponentType() == GANTT_BOX)) {
                  return component;
               }
               component = (XComponent) (component.getParent());
            }
            break;
         case UTILIZATION_ROW:
         case UTILIZATION_CHART:
         case UTILIZATION_HEADER:
            // Context is always the UtilizationBox
            component = (XComponent) getParent();
            while (component != null) {
               if ((component instanceof OpProjectComponent)
                    && (((OpProjectComponent) component).getComponentType() == UTILIZATION_BOX)) {
                  return component;
               }
               component = (XComponent) (component.getParent());
            }
            break;
      }
      return null;
   }

   /**
    * Resets all the cached values on this chart component.
    */
   protected void resetCached() {
      setStart(null);
      setEnd(null);
      setCachedLeftCaptionLenght(null);
      setCachedRightCaptionLenght(null);
      setCachedHeight(null);
   }

   /**
    * Resets the calendar on a time chart (GANTT_CHART, UTILIZATION_CHART). Must be called on a box in order to make
    * it's containing chart to reset it's calendar
    */
   public void resetCalendar() {
      XComponent viewPort = (XComponent) getViewPort();
      OpProjectComponent chart = (OpProjectComponent) viewPort._getChild(0);
      chart.setStart(null);
      chart.setEnd(null);
   }

   /**
    * Initializes the start/end dates for charts (utilization/gantt) (only if the start is null!)
    *
    * @return true if  start/end was changed
    */
   private boolean initializeCalendar() {

      if ((pcType != GANTT_CHART) && (pcType != UTILIZATION_CHART)) {
         throw new UnsupportedOperationException("Initialize calendar can only be called on GANTT_CHART or UTILIZATION_CHART");
      }

      if (getStart() == null) {
         XCalendar calendar = XDisplay.getDefaultDisplay().getCalendar();

         // Start and end of chart is the minimum/maximum of activities start/end
         long chart_start_time = Long.MAX_VALUE;
         long chart_end_time = Long.MIN_VALUE;
         long start_time = chart_start_time;
         long end_time = chart_end_time;
         OpProjectComponent box = (OpProjectComponent) getContext();
         XComponent data_set = box.getDataSetComponent();
         OpGanttValidator validator = null;
         if (pcType == GANTT_CHART) {
            validator = (OpGanttValidator) (data_set.validator());
         }

         XComponent data_row = null;
         for (int index = 0; index < data_set.getChildCount(); index++) {
            data_row = (XComponent) (data_set._getChild(index));
            if (!OpProjectConstants.DUMMY_ROW_ID.equals(data_row.getStringValue())) {
               if (pcType == GANTT_CHART) {
                  if (OpGanttValidator.getStart(data_row) == null ||
                       OpGanttValidator.getEnd(data_row) == null) {
                     continue;
                  }
                  start_time = OpGanttValidator.getStart(data_row).getTime();
                  end_time = OpGanttValidator.getEnd(data_row).getTime();
               }
               else {

                  XComponent utilizationStart = ((XComponent) (data_row.getChild(UTILIZATION_START_COLUMN_INDEX)));
                  if (utilizationStart != null && utilizationStart.getDateValue() != null) {
                     start_time = utilizationStart.getDateValue().getTime();
                  }
                  else {
                     start_time = chart_start_time;
                  }
                  XComponent utilizationEnd = ((XComponent) (data_row.getChild(UTILIZATION_END_COLUMN_INDEX)));
                  if (utilizationEnd != null && utilizationEnd.getDateValue() != null) {
                     end_time = utilizationEnd.getDateValue().getTime();
                  }
                  else {
                     end_time = chart_end_time;
                  }
               }
            }

            if (start_time < chart_start_time) {
               chart_start_time = start_time;
            }
            if (end_time > chart_end_time) {
               chart_end_time = end_time;
            }
         }
         /* check if there are histories and modify the chart start and end */
         XComponent history = ((OpProjectComponent) getContext()).getHistory();

         if (history != null) {
            XComponent historyRow;
            List startHistoryArray;
            List endHistoryArray;
            Date historyStartDate;
            Date historyEndDate;

            for (int historyRowIndex = 0; historyRowIndex < history.getChildCount(); historyRowIndex++) {
               historyRow = (XComponent) (history._getChild(historyRowIndex));
               // start history array for this history row
               startHistoryArray = ((XComponent) (historyRow._getChild(START_HISTORY_COLUMN_INDEX))).getListValue();
               // end history array for this history row
               endHistoryArray = ((XComponent) (historyRow._getChild(END_HISTORY_COLUMN_INDEX))).getListValue();

               for (int index = 0; index < startHistoryArray.size(); index++) {
                  historyStartDate = (Date) (startHistoryArray.get(index));
                  historyEndDate = (Date) (endHistoryArray.get(index));
                  /* if the start date of the history is before the chart start */
                  if (historyStartDate.getTime() < chart_start_time) {
                     chart_start_time = historyStartDate.getTime();
                  }
                  /* if the start date of the history is after the chart end */
                  if (historyEndDate.getTime() > chart_end_time) {
                     chart_end_time = historyEndDate.getTime();
                  }
               }
            }
         }

         // Special case: Empty chart
         if (chart_start_time == Long.MAX_VALUE) {
            Date chart_start;
            if (validator != null) {
               if (validator.getProjectTemplate().booleanValue()) {
                  //if the project is a template set the start date to default template start
                  chart_start = OpGanttValidator.getDefaultTemplateStart();
               }
               else {
                  //if the project is not a template set the start date around today
                  chart_start = calendar.workWeekStart(XCalendar.today());
               }
            }
            else {
               chart_start = calendar.workWeekStart(XCalendar.today());
            }
            chart_start_time = chart_start.getTime();
            chart_end_time = chart_start_time + XCalendar.MILLIS_PER_WEEK * 2;
            // *** Should project end at end-work-time?
         }

         // "Pad" start and end times for captions
         int uiDaysToLeft;
         int uiDaysToRight;
         double unitRatio = getUnitRatio(box.getTimeUnit());

         int weekLength = 7;
         if (pcType == GANTT_CHART) {
            double dayWidth = box._dayWidth();
            int maxLeftCaption = box.getMaxLeftCaptionSize();
            int maxRightCaption = box.getMaxRightCaptionSize();
            if (maxLeftCaption != 0) {
               uiDaysToLeft = (int) (maxLeftCaption / dayWidth) + weekLength / 2;
            }
            else {
               uiDaysToLeft = weekLength;
            }
            if (maxRightCaption != 0) {
               uiDaysToRight = (int) (maxRightCaption / dayWidth) + weekLength / 2;
            }
            else {
               uiDaysToRight = weekLength;
            }
         }
         else {
            //UTILIZATION_CHART
            uiDaysToLeft = weekLength;
            uiDaysToRight = weekLength;
         }

         chart_start_time -= uiDaysToLeft * unitRatio * XCalendar.MILLIS_PER_DAY;
         chart_end_time += uiDaysToRight * unitRatio * XCalendar.MILLIS_PER_DAY;


         Date chart_start = new Date(chart_start_time);

         byte timeUnit = box.getTimeUnit();
         switch (timeUnit) {
            case XCalendar.WEEKS: {
               chart_start = XDisplay.getDefaultDisplay().getCalendar().workWeekStart(chart_start);
               break;
            }
            case XCalendar.MONTHS: {
               chart_start = XDisplay.getDefaultDisplay().getCalendar().getFirstWorkDayOfMonth(chart_start);
               break;
            }
         }

         Date chart_end = new Date(chart_end_time);
         calendar.getCalendar().setTimeInMillis(chart_start_time);
         int first_month_length = calendar.getCalendar().getActualMaximum(Calendar.DATE)
              - calendar.getCalendar().get(Calendar.DATE);
         setFirstMonthLength(first_month_length);

         int start_weekday = calendar.getCalendar().getFirstDayOfWeek();

         // *** start-week-day-offset = 0
         int first_workday = calendar.getFirstWorkday();
         int last_workday = calendar.getLastWorkday();
         int work_week_length = 1 + last_workday - first_workday;
         setWorkWeekLength(work_week_length);

         int first_work_week_length = XDisplay.getDefaultDisplay().getCalendar().countWeekdays(start_weekday, last_workday);

         int first_weekend_length = weekLength - work_week_length;
         if (first_work_week_length > work_week_length) {
            // Start weekday is part of first weekend
            first_work_week_length = work_week_length;
            first_weekend_length = XDisplay.getDefaultDisplay().getCalendar().countWeekdays(start_weekday, calendar.previousWeekday(first_workday));
         }

         setFirstWorkWeekLength(first_work_week_length);
         setFirstWeekendLength(first_weekend_length);
         int week_count = (int) ((chart_end_time - chart_start_time) / XCalendar.MILLIS_PER_WEEK);
         setWeekCount(week_count);

         setStart(chart_start);
         setEnd(chart_end);

         logger.debug("*** CHART_START/END: " + chart_start + "/" + chart_end);

         //update took place
         return true;
      }
      else {
         return false; //no change was made
      }
   }

   /**
    * Will return the graphical unit width for DAYS, WEEKS, and MONTHS by calculating the maximum of the displayed
    * header
    *
    * @return graphical unit width.
    */
   protected final double _dayWidth() {
      if ((pcType == GANTT_BOX) || (pcType == UTILIZATION_BOX)) {
         // get the GanttHeaderComponent
         XComponent ganttHeader = (XComponent) (_getChild(3)).getChild(0);
         // get the style of the Gantt Header
         XStyle ganttHeaderStyle = ganttHeader.getStyleAttributes();
         // calculate the widthIncrement
         int widthGapIncrement = ganttHeaderStyle.gap * 2;
         // get font metrics according to style
         FontMetrics metrics = getFontMetrics(ganttHeaderStyle.font());
         int dayWidth = metrics.charWidth('W');
         int weekWidth = dayWidth * 2;
         switch (getTimeUnit()) {
            case XCalendar.DAYS:
               return dayWidth + widthGapIncrement;
            case XCalendar.WEEKS:
               return weekWidth;
            case XCalendar.MONTHS:
               return dayWidth + widthGapIncrement;
            default:
               throw new IllegalArgumentException("Unknown time unit for determining the day width");
         }
      }
      throw new UnsupportedOperationException("The method _dayWidth() cannot be called for this component type");
   }

   /**
    * Will return the timeUnits ration for a type of timeUnits (like XCalendar.WEEKS) in order to be able to transform
    * from a timeUnit in day units.
    *
    * @param timeUnits type of timeUnits
    * @return untiRatio for a type of timeUnits.
    */
   public static double getUnitRatio(int timeUnits) {
      double unitRatio = 1;
      switch (timeUnits) {
         case XCalendar.WEEKS:
            unitRatio = 7;
            break;
         case XCalendar.MONTHS:
            unitRatio = 30.46;
            break;
      }
      return unitRatio;
   }


   /**
    * Return the height of an <code>GANTT_CHART</code> component Must be called on a chart component.
    *
    * @return the height of the <code>GANTT_CHART</code> component
    */
   public int getGanttChartHeight() {

      int maxHeight = 0;
      Object ganttSize = getCachedHeight();

      if (ganttSize == null) {
         int height = getGridY();
         XStyle chartStyle = getStyleAttributes();
         OpProjectComponent box = (OpProjectComponent) getContext();
         XComponent dataSet = box.getDataSetComponent();

         boolean hasActivities = false;
         for (int index = 0; index < dataSet.getChildCount(); index++) {
            XComponent activity = (XComponent) dataSet.getChild(index);
            if (activity.getVisible() && !activity.getFiltered() && !OpGanttValidator.isTaskType(activity)) {
               hasActivities = true;
               maxHeight += height;
            }
         }
         if (hasActivities) {
            maxHeight += chartStyle.gap;
            setCachedHeight(new Integer(maxHeight));
         }
      }
      else {
         maxHeight = ((Integer) ganttSize).intValue();
      }

      return maxHeight;
   }

   /**
    * Return the height of an <code>UTILIZATION_CHART</code> component
    *
    * @return the height of the <code>UTILIZATION_CHART</code> component
    */
   public int getUtilizationChartHeight() {
      XStyle utilizationStyle = getStyleAttributes();
      // height increment
      int maxHeight = utilizationStyle.top + utilizationStyle.bottom;
      OpProjectComponent box = (OpProjectComponent) getContext();
      XComponent dataSet = box.getDataSetComponent();
      for (int index = 0; index < dataSet.getChildCount(); index++) {
         XComponent data_row = (XComponent) dataSet.getChild(index);
         if (data_row.getVisible() && !data_row.getFiltered()) {
            maxHeight += getUtilizationRowPrefferedHeight() + utilizationStyle.gap;
         }
      }
      //the gap of the last visible row has to be removed
      if (maxHeight > 0) {
         maxHeight -= utilizationStyle.gap;
      }
      return maxHeight;
   }

   public int getUtilizationRowPrefferedHeight() {
      XStyle style = DEFAULT_UTILIZATION_ROW_STYLE_ATTRIBUTES;
      FontMetrics metrics = getFontMetrics(style.font());
      return style.top + metrics.getAscent() + metrics.getDescent() + style.bottom;
   }


   /**
    * Calculates the maximum size in pixels of all the left alligned captions from the current chart component. Must be
    * called on a box that contains the chart.
    *
    * @return the max size from the left activity captions (in pixels).
    */
   private int getMaxLeftCaptionSize() {

      int size = 0;
      Object captionCached;
      String captionValueType;
      OpProjectComponent gantt_chart = (OpProjectComponent) getBoxContent();
      if (getCaptionLeft() == null) {
         return 0;
      }
      captionValueType = getCaptionLeft();
      captionCached = gantt_chart.getCachedLeftCaptionLenght();

      if (captionCached == null) {
         size = maxActivityCaption(gantt_chart, captionValueType);
         if (size != -1) {
            gantt_chart.setCachedLeftCaptionLenght(new Integer(size));
         }
         else {
            size = 0;
         }
      }
      else {
         size = ((Integer) captionCached).intValue();
      }
      return size;
   }

   /**
    * Calculates the maximum size in pixels of all the right alligned captions from the current chart component. Must be
    * called on a box that contains the chart.
    *
    * @return the max size from the right activity captions (in pixels).
    */
   private int getMaxRightCaptionSize() {

      int size = 0;
      Object captionCached;
      String captionValueType;
      OpProjectComponent gantt_chart = (OpProjectComponent) getBoxContent();
      if (getCaptionRight() == null) {
         return 0;
      }
      captionValueType = getCaptionRight();
      captionCached = gantt_chart.getCachedRightCaptionLenght();

      if (captionCached == null) {
         size = maxActivityCaption(gantt_chart, captionValueType);
         if (size != -1) {
            gantt_chart.setCachedRightCaptionLenght(new Integer(size));
         }
         else {
            size = 0;
         }
      }
      else {
         size = ((Integer) captionCached).intValue();
      }
      return size;
   }

   /**
    * Calculates the maximum lenght in pixels of the given activity property type
    *
    * @param gantt_chart      Chart containing all the queried activities
    * @param captionValueType the type of property for the activity
    * @return the maximum length (for the string value) of the given property type or -1 if no activities have been
    *         found on the chart
    */
   private int maxActivityCaption(OpProjectComponent gantt_chart, String captionValueType) {
      int size = 0;
      FontMetrics metrics = getFontMetrics(getStyleAttributes().font());
      boolean hasActivities = false;
      for (int i = 0; i < gantt_chart.getContext().getDataSetComponent().getChildCount(); i++) {
         XComponent activityData = (XComponent) gantt_chart.getContext().getDataSetComponent().getChild(i);
         String caption = "";
         if (activityData != null) {
            hasActivities = true;
            Object property = getActivityProperty(activityData, captionValueType);
            if (property != null) {
               caption = property.toString();
            }
         }
         int stringSize = metrics.stringWidth(caption);
         if (stringSize > size) {
            size = stringSize;
         }
      }
      return hasActivities ? size : -1;
   }

   /**
    * Calculates the new dimmension of a chart and if COMPUTE_BOUNDS is set to true will add to the dimension a fixed
    * size representing the largest size of a name of an activity. Also this method will make sure that the scroller is
    * always on. Must be called on a chart component.
    *
    * @param box       the box that contains the chart.
    * @param days      the number of initial days in tha chart.
    * @param maxHeight the height of the chart.
    * @return the new dimension computed.
    */
   private Dimension computeNewDimension(OpProjectComponent box, int days, int maxHeight) {

      Dimension realDim;
      XComponent viewPort;
      int newUiDays;
      int uiDaysToRight = 0;
      int uiDaysToLeft = 0;

      double unitRatio = getUnitRatio(box.getTimeUnit());
      int uiDays = (int) Math.ceil(days / unitRatio);

      // default dimmension
      double dayWidth = box._dayWidth();
      realDim = new Dimension((int) (uiDays * dayWidth), maxHeight);

      // make sure that the size and end date are always > viewport size in order for the scroll to appear
      //(only if in edit mode!)
      viewPort = (XComponent) getParent();
      if (viewPort.getBounds() != null && box.getEditMode()) {
         int viewWidth = viewPort.getBounds().width;
         if (realDim.getWidth() <= viewWidth) {
            newUiDays = (int) (viewWidth / dayWidth) + 1;
            uiDaysToRight = Math.max(uiDaysToRight, newUiDays - (uiDays + uiDaysToLeft));
            realDim = new Dimension((int) (newUiDays * dayWidth), maxHeight);
            box.updateGantChartDate(EAST, Calendar.DAY_OF_MONTH, (int) (uiDaysToRight * unitRatio));
         }
      }

      return realDim;
   }

   public Dimension getPreferredSize() {
      XStyle style = getStyleAttributes();
      OpProjectComponent box = null;
      XComponent viewPort = null;
      Dimension realDim = null;
      int days;
      /* unit ratio */
      double unitRatio = 0;

      switch (pcType) {

         case GANTT_CHART: {
            // Force new initialization: in case of non existing start Value
            initializeCalendar();
            days = (int) ((getEnd().getTime() - getStart().getTime()) / XCalendar.MILLIS_PER_DAY) + 1;
            box = (OpProjectComponent) getContext();
            // get MaxHeight for Gantt Chart
            int maxHeight = getGanttChartHeight();
            // compute the new dimension
            realDim = computeNewDimension(box, days, maxHeight);
            return realDim;
         }
         case GANTT_HEADER: {
            // Take Gantt-values from chart
            box = (OpProjectComponent) getContext();
            OpProjectComponent gantt_chart = (OpProjectComponent) box.getBoxContent();
            gantt_chart.initializeCalendar();
            Date start = gantt_chart.getStart();
            Date end = gantt_chart.getEnd();
            days = (int) ((end.getTime() - start.getTime()) / XCalendar.MILLIS_PER_DAY) + 1;
            FontMetrics metrics = getFontMetrics(getStyleAttributes().font());
            int line_height = metrics.getAscent() + metrics.getDescent() + style.top + style.bottom;
            realDim = gantt_chart.computeNewDimension(box, days, 2 * line_height + style.gap);
            return realDim;
         }
         case UTILIZATION_CHART: {
            // Force new initialization: in case of non existing start Value
            initializeCalendar();
            days = (int) ((getEnd().getTime() - getStart().getTime()) / XCalendar.MILLIS_PER_DAY) + 1;
            box = (OpProjectComponent) getContext();
            unitRatio = getUnitRatio(box.getTimeUnit());
            int maxHeight = getUtilizationChartHeight();
            realDim = new Dimension((int) (days / unitRatio * box._dayWidth()), maxHeight);
            return realDim;
         }
         case UTILIZATION_HEADER: {
            // Take Gantt-values from chart
            box = (OpProjectComponent) getContext();
            OpProjectComponent chart = (OpProjectComponent) box.getBoxContent();
            chart.initializeCalendar();
            Date start = chart.getStart();
            Date end = chart.getEnd();
            days = (int) ((end.getTime() - start.getTime()) / XCalendar.MILLIS_PER_DAY) + 1;
            unitRatio = getUnitRatio(box.getTimeUnit());
            FontMetrics metrics = getFontMetrics(getStyleAttributes().font());
            int line_height = metrics.getAscent() + metrics.getDescent() + style.top + style.bottom;
            realDim = new Dimension((int) (days / unitRatio * box._dayWidth()), 2 * line_height + style.gap);
            return realDim;
         }
         default:
            return super.getPreferredSize();
      }
   }

   protected void _dynamicGanttDependency(int source_x, int source_y, int target_x, int target_y) {
      // TODO: Take all grid-x/y from context/box instead of chart
      OpProjectComponent box = (OpProjectComponent) getContext();
      OpProjectComponent chart = (OpProjectComponent) box.getBoxContent();
      double half_day_w = box._dayWidth() / 2;
      int half_grid_y = chart.getGridY() / 2;
      Point target_point = new Point(target_x, target_y);
      // Start routing at the right/middle boundary of the source shape
      List route = new ArrayList();
      double x = source_x;
      int y = source_y;
      route.add(new Point((int) x, y));
      // Always go one half-grid to to right
      x += half_day_w;
      route.add(new Point((int) x, y));
      // There are two possible (sub-)routes: Around the source component and
      // directly down
      if (x > target_point.x - half_day_w) {
         // We have to go "around" the source component: Up or down
         if (y < target_point.y) {
            y += half_grid_y;
         }
         else {
            y -= half_grid_y;
         }
         route.add(new Point((int) x, y));
         x = target_point.x - half_day_w;
         route.add(new Point((int) x, y));
         y = target_point.y;
         route.add(new Point((int) x, y));
      }
      else {
         // We can go "directly" up or down
         y = target_point.y;
         route.add(new Point((int) x, y));
         x = target_point.x - half_day_w;
         route.add(new Point((int) x, y));
      }
      route.add(target_point);
      // Calculate bounding box of route
      Rectangle chart_bounds = chart.getBounds();
      // Rectangle bounding_box = new Rectangle(bounds.width, bounds.height, 0,
      // 0);
      int x1 = chart_bounds.width;
      int y1 = chart_bounds.height;
      int x2 = 0;
      int y2 = 0;
      Point[] path = new Point[route.size()];
      Point point = null;
      int index = 0;
      for (index = 0; index < path.length; index++) {
         point = (Point) (route.get(index));
         path[index] = point;
         x = point.x;
         y = point.y;
         if (x < x1) {
            x1 = (int) x;
         }
         if (y < y1) {
            y1 = y;
         }
         if (x > x2) {
            x2 = (int) x;
         }
         if (y > y2) {
            y2 = y;
         }
      }
      // Decrease y-coordinate or increase height because of arrow-size
      // *** TO DO: Check whether target component is "above" source
      y2 += 5;
      // Finalize path by correcting coordinates using bounding box
      for (index = 0; index < path.length; index++) {
         path[index].x -= x1;
         path[index].y -= y1;
      }
      setPath(path);
      // Set bounds of dependency (bounding box)
      if (y1 == y2) {
         y1 -= 2;
         y2 += 2;
      }
      setBounds(new Rectangle(x1, y1, x2 - x1, y2 - y1));
   }

   protected void _routeGanttDependency(OpProjectComponent dependency, double day_width, int grid_y) {
      // *** TO DO: Make 'dependency' to *this* (object-orientation)
      // Route Gantt depedency arrow from source to target component (within
      // project)
      // *** Note that MS-Project offers two routing methods (this one and a
      // simple one just pointing "down" on the target
      // *** right, down, left if necessary, then down until reached, then right
      // until reached
      // *** TO DO: Deal with the possibility that target-shape is *above*
      // source shape
      OpProjectComponent source = dependency.getSource();
      if (source != null && source.getVisible()) {
         Rectangle source_bounds = source.getBounds();
         OpProjectComponent target = dependency.getTarget();
         if (target != null && target.getVisible()) {
            Rectangle target_bounds = target.getBounds();
            // Target point is the left/middle boundary of the target shape
            int target_x = target_bounds.x;
            int target_y = target_bounds.y + (target_bounds.height / 2);
            int source_x = source_bounds.x + source_bounds.width;
            int source_y = source_bounds.y + (source_bounds.height / 2);
            dependency._dynamicGanttDependency(source_x, source_y, target_x, target_y);
         }
      }
   }

   /**
    * Does layouting of the <code>GANTT_CHART</code> component
    *
    * @param chart_start start Date of the <code>GANTT_CHART</code> component
    * @param day_width   width according to <code> TIME_UNITS</code>
    * @param grid_y      height of an activity in <code>GANTT_CHART</code> component
    */
   private void doLayoutGanttChart(Date chart_start, double day_width, int grid_y) {
      // Layout Gantt tasks and milestones (recursion over sub-projects)
      OpProjectComponent box = (OpProjectComponent) getContext();
      XComponent data_set = box.getDataSetComponent();
      Map categoryColorMap = box.categoryColorMap();

      XComponent data_row;
      OpProjectComponent component;
      int x;
      int y = 0;
      int width;
      int height;
      String category;
      //for non expanded collection activities hide all data rows
      for (int index = 0; index < data_set.getChildCount(); index++) {
         data_row = (XComponent) (data_set._getChild(index));
         if (data_row.expandable() && !data_row.getExpanded()) {
            List hiddenRows = data_row.getSubRows();
            for (int i = 0; i < hiddenRows.size(); i++) {
               XComponent childRow = (XComponent) hiddenRows.get(i);
               childRow.setVisible(false);
            }
         }
      }
      ganttIndexes = new HashMap();
      int ganttPositionIndex = 0;
      for (int index = 0; index < data_set.getChildCount(); index++) {
         if (getChildCount() > index) {
            component = (OpProjectComponent) _getChild(index);
            component.initializeGanttComponent(GANTT_ACTIVITY);
         }
         else {
            // Extend proxy children as needed
            component = new OpProjectComponent(GANTT_ACTIVITY);
            _addChild(component);
         }
         data_row = (XComponent) (data_set._getChild(index));
         component.setDataRow(data_row);

         // Map category to color index
         if (categoryColorMap != null) {
            category = OpGanttValidator.getCategory(data_row);
            if (category != null) {
               // Map category choice ID to color index
               String categoryLocator = XValidator.choiceID(category);
               Integer colorIndex = (Integer) categoryColorMap.get(categoryLocator);
               component.setColorIndex(colorIndex);
            }
            else {
               component.setColorIndex(null);
            }
         }

         if (data_row.getVisible() && !data_row.getFiltered() && !OpGanttValidator.isTaskType(data_row)) {
            // sub_activity_components = component.getSubActivityComponents();
            component.setVisible(true);
            ganttIndexes.put(new Integer(ganttPositionIndex), new Integer(data_row.getIndex()));
            ganttPositionIndex++;
            // start in seconds, relatif to the chart start
            long startS = OpGanttValidator.getStart(data_row).getTime() - chart_start.getTime();
            long endS = OpGanttValidator.getEnd(data_row).getTime() - chart_start.getTime();

            // compute startX and Width based on time unit and day_width
            long startDays = startS / XCalendar.MILLIS_PER_DAY;
            long durationDays = (endS - startS) / XCalendar.MILLIS_PER_DAY;
            durationDays++;
            double unitRatio = getUnitRatio(box.getTimeUnit());

            x = (int) ((day_width * (double) startDays) / (double) unitRatio);
            width = (int) ((day_width * (double) durationDays) / (double) unitRatio);
            if (width == 0) {
               width++;
            }

            height = grid_y;

            if (OpGanttValidator.getType(data_row) == OpGanttValidator.COLLECTION) {
               component.setActivityType(COLLECTION_ACTIVITY);
               x -= grid_y / 2;
               width += grid_y;
            }
            else if (OpGanttValidator.getType(data_row) == OpGanttValidator.MILESTONE) {
               component.setActivityType(MILESTONE_ACTIVITY);
               width = grid_y;
               if (box.getTimeUnit() == XCalendar.DAYS) {
                  // Milestone activity: Align on grid-lines (for day view)
                  x += day_width / 2;
               }
               else {
                  //upper corner of the milestone must be on the same line as the start/end day
                  x = (int) ((day_width * (double) (startDays + 1)) / (double) unitRatio) - grid_y / 2;
               }
            }
            else if (OpGanttValidator.getType(data_row) == OpGanttValidator.SCHEDULED_TASK) {
               component.setActivityType(SCHEDULED_TASK_ACTIVITY);
            }
            else {
               // Standard activity
               component.setActivityType(STANDARD_ACTIVITY);
            }
            if (component != getDisplay().getDragSource()) {
               component.setBounds(new Rectangle(x, y, width, height));
               component.createComponentShape();
            }
            y += grid_y;
         }
         else {
            component.setVisible(false);
         }
      }
      int activity_count = data_set.getChildCount();
      OpProjectComponent connector_component = null;
      List successors = null;
      int j = 0;
      int successor_index = 0;
      OpProjectComponent successor_component = null;
      // Layout successor connectors
      // *** TODO: What about visible/invisible connectors (source-components)?
      int component_count = activity_count;
      for (int index = 0; index < activity_count; index++) {
         data_row = (XComponent) (data_set._getChild(index));
         component = (OpProjectComponent) _getChild(index);
         if (data_row.getVisible() && !data_row.getFiltered() && !OpGanttValidator.isTaskType(data_row)) {
            successors = OpGanttValidator.getSuccessors(data_row);
            for (j = 0; j < successors.size(); j++) {

               successor_index = ((Integer) (successors.get(j))).intValue();
               successor_component = (OpProjectComponent) _getChild(successor_index);
               if (data_set._getChild(successor_index).getVisible()) {
                  if (component_count < getChildCount()) {
                     connector_component = (OpProjectComponent) _getChild(component_count);
                     connector_component.initializeGanttComponent(GANTT_DEPENDENCY);
                  }
                  else {
                     connector_component = new OpProjectComponent(GANTT_DEPENDENCY);
                     _addChild(connector_component);
                  }
                  connector_component.setSource(component);
                  connector_component.setTarget(successor_component);
                  _routeGanttDependency(connector_component, day_width, grid_y);
                  component_count++;
               }
            }
         }
      }
      int i = 0;
      // Add copies of open dependencies
      ArrayList open_dependencies = getOpenDependencies();
      OpProjectComponent open_dependency = null;
      OpProjectComponent open_dependency_copy = null;
      for (i = 0; i < open_dependencies.size(); i++) {
         open_dependency = (OpProjectComponent) (open_dependencies.get(i));
         if (component_count < getChildCount()) {
            open_dependency_copy = (OpProjectComponent) _getChild(component_count);
            open_dependency_copy.initializeGanttComponent(GANTT_DEPENDENCY);
         }
         else {
            open_dependency_copy = new OpProjectComponent(GANTT_DEPENDENCY);
            _addChild(open_dependency_copy);
         }
         open_dependency_copy.setSource(open_dependency.getSource());
         _routeGanttDependency(open_dependency_copy, day_width, grid_y);
         component_count++;
      }
      // Check whether some child components can be removed
      for (i = getChildCount() - 1; i >= component_count; i--) {
         removeChild(i);
      }
   }

   /**
    * Initializes a gantt UI component to a specified type.
    *
    * @param type new type of this component
    */
   private void initializeGanttComponent(byte type) {
      this.pcType = type;
      switch (type) {
         case GANTT_ACTIVITY:
            this.setVisible(true);
            this.setShape(false);
            this.setStyle(DEFAULT_GANTT_ACTIVITY_STYLE);
            this.setPath(null);
            break;
         case GANTT_DEPENDENCY:
            this.setVisible(true);
            this.setShape(true);
            this.setStyle(DEFAULT_GANTT_DEPENDENCY_STYLE);
            this.setDataRow(null);
            break;
      }
   }

   /**
    * Synchronize selection between data and UI. This method uses the gantt box component to synchronize
    * the selection.
    */
   protected void syncBoxUISelection() {
      //data-selected component will request focus
      this.requestFocus();
      OpProjectComponent chart = (OpProjectComponent) this.getBoxContent();
      for (int i = 0; i < chart.getChildCount(); i++) {
         OpProjectComponent activity = (OpProjectComponent) chart.getChild(i);
         XComponent dataRow = activity.getDataRow();
         if (dataRow != null && dataRow.getSelected()) {
            activity.requestFocus();
         }
         else {
            if (dataRow != null && !dataRow.getSelected()) {
               activity.setSelected(false);
            }
         }
      }
   }

   /**
    * Synchronize selection between data and UI. This method uses the gantt activity component to synchronize
    * the selection.
    */
   protected void syncActivityUISelection() {
      OpProjectComponent box = (OpProjectComponent) this.getContext();
      OpProjectComponent chart = (OpProjectComponent) box.getBoxContent();
      for (int i = 0; i < chart.getChildCount(); i++) {
         OpProjectComponent activity = (OpProjectComponent) chart.getChild(i);
         XComponent dataRow = activity.getDataRow();
         if (dataRow != null && dataRow.getSelected()) {
            activity.setSelected(true);
         }
         else {
            if (dataRow != null && !dataRow.getSelected()) {
               activity.setSelected(false);
            }
         }
      }
   }

   protected void clearSelection() {
      getContext().getDataSetComponent().clearDataSelection();
      syncActivityUISelection();
   }


   /**
    * Does layouting of the <code>UTILIZATION_CHART</code> component
    */
   protected void _doLayoutUtilizationChart() {
      // TODO: Probably use double for x like for Gantt-chart (accuracy)
      XStyle style = getStyleAttributes();
      OpProjectComponent box = (OpProjectComponent) getContext();
      String resourceTableId = box.getResourceTableId();
      if (resourceTableId != null) {
         XComponent resourceTable = getForm().findComponent(resourceTableId);
         resourceTable.registerEventHandler(getContext(), COMPONENT_EVENT);
         box.setResourceTableId(null);
         box.setResourceTable(resourceTable);
      }

      double day_width = box._dayWidth();
      int height = getUtilizationRowPrefferedHeight();
      XComponent data_set = box.getDataSetComponent();
      XComponent data_row;
      OpProjectComponent visual;
      long start;
      long end;
      int x;
      int y = 0;
      int width;
      int visual_count = 0;

      for (int i = 0; i < data_set.getChildCount(); i++) {
         data_row = (XComponent) (data_set.getChild(i));
         if (data_row.getVisible() && !data_row.getFiltered()) {

            XComponent utilizationStart = ((XComponent) (data_row.getChild(UTILIZATION_START_COLUMN_INDEX)));
            XComponent utilizationEnd = ((XComponent) (data_row.getChild(UTILIZATION_END_COLUMN_INDEX)));

            // resource has start & end ( has work slips )
            if (utilizationStart.getDateValue() != null && utilizationEnd.getDateValue() != null) {

               if (getChildCount() > visual_count) {
                  visual = (OpProjectComponent) _getChild(visual_count);
               }
               else {
                  // Extend visuals as needed
                  visual = new OpProjectComponent(UTILIZATION_ROW);
                  _addChild(visual);
               }
               visual_count++;
               visual.setDataRow(data_row);

               start = utilizationStart.getDateValue().getTime();
               end = utilizationEnd.getDateValue().getTime();
               logger.debug("   dw " + day_width);
               logger.debug("   s, e: " + start + ", " + end);
               // TODO: Should we support style.gap in x-direction?

               long startS = start - getStart().getTime();
               long endS = end - getStart().getTime();
               long startDays = startS / XCalendar.MILLIS_PER_DAY;
               long durationDays = (endS - startS) / XCalendar.MILLIS_PER_DAY;
               durationDays++;
               double unitRatio = getUnitRatio(box.getTimeUnit());

               x = (int) Math.round((day_width * (double) startDays) / (double) unitRatio);
               width = (int) Math.round((day_width * (double) durationDays) / (double) unitRatio);

               //set bounds 1px inside the utilization line
               visual.setBounds(new Rectangle(x, y + 1, width, height - 3));
            }
            y += height;
            y += style.gap;
         }
      }

      // Check whether some child components can be removed
      for (int i = getChildCount() - 1; i >= visual_count; i--) {
         removeChild(i);
      }

      initializeUtilizationChartTimeDimension();
   }

   /**
    * Sets the <code>last_value</code> on this scroll on the position for Today date.
    * <code>last_value</code> is used by scrollSlidersToLastValue().
    */
   private void setScrollLastValueToToday() {
      XComponent horizontal_scroll_bar = (XComponent) (_getChild(HORIZONTAL_SCROLL_BAR_INDEX));
      int scrollMax = horizontal_scroll_bar.getMaximum();
      if (!Boolean.TRUE.equals(horizontal_scroll_bar.getSliderWasMoved())) {
         if (scrollMax != 0) {
            OpProjectComponent chart = (OpProjectComponent) getBoxContent();
            long startTime = chart.getStart().getTime();
            long endTime = chart.getEnd().getTime();
            long todayTime = XCalendar.today().getTime();
            if (startTime > todayTime) {
               horizontal_scroll_bar.setLastValue(0);
            }
            else {
               long today = (todayTime - startTime) / ((endTime - startTime) / scrollMax);
               horizontal_scroll_bar.setLastValue((double) today / (double) scrollMax);
            }
         }
      }
   }

   public void doLayout() {
      switch (pcType) {
         case GANTT_CHART:
            // First layout Gantt chart activities (creating cached dependencies)
            initializeCalendar();
            OpProjectComponent box = (OpProjectComponent) getContext();
            moveToWeekMonthBeginning(box.getTimeUnit());
            doLayoutGanttChart(getStart(), box._dayWidth(), getGridY());
            break;
         case GANTT_BOX:
            scrollSlidersToZero();
            doLayoutScrollBox();
            setScrollLastValueToToday();
            scrollSlidersToLastValue();
            this.syncBoxUISelection();
            break;
         case UTILIZATION_BOX:
            scrollSlidersToZero();
            doLayoutScrollBox();
            setScrollLastValueToToday();
            scrollSlidersToLastValue();
            break;
         case UTILIZATION_CHART:
            initializeCalendar();
            box = (OpProjectComponent) getContext();
            moveToWeekMonthBeginning(box.getTimeUnit());
            _doLayoutUtilizationChart();
            break;
         case CAPTION_EDITOR:
            // call super layout for size calculations
            super.doLayout();
            XComponent lineEditor = (XComponent) this._getChild(0)._getChild(0);
            Rectangle lineBounds = lineEditor.getBounds();

            //<FIXME author="Horia Chiorean" description="What's with the magic numbers ?!?!">
            // if the line editor's size is too small, increase it by 50
            if (lineBounds.width < 60) {
               lineBounds.width += 50;
               this._getChild(0).getBounds().width += 55;
               this.getBounds().width += 55;
            }
            //<FIXME>
            break;
         default:
            super.doLayout();
      }
   }

   /**
    * For a gantt or utilization chart, on week or month view, makes sure the start of the chart is at the begining of
    * that week or month.
    *
    * @param timeUnit a <code>byte</code> representing the gannt time unit (weeks or months).
    */
   private void moveToWeekMonthBeginning(byte timeUnit) {
      if (this.pcType == GANTT_CHART || this.pcType == UTILIZATION_CHART) {
         // set the start for chart to "round" values - begining of month/begining of week
         Date start = this.getStart();
         Calendar calendar = XDisplay.getDefaultDisplay().getCalendar().getCalendar();
         calendar.setTime(start);
         switch (timeUnit) {
            case XCalendar.WEEKS:
               int minimumDay = XDisplay.getDefaultDisplay().getCalendar().getCalendar().getFirstDayOfWeek();
               int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
               if (dayOfWeek > minimumDay) {
                  int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
                  currentDay = currentDay - (dayOfWeek - minimumDay);
                  calendar.set(Calendar.DAY_OF_MONTH, currentDay);
               }
               break;
            case XCalendar.MONTHS:
               minimumDay = calendar.getActualMinimum(Calendar.DAY_OF_MONTH);
               if (calendar.get(Calendar.DAY_OF_MONTH) > minimumDay) {
                  calendar.set(Calendar.DAY_OF_MONTH, minimumDay);
               }
               break;
         }
         this.setStart(new Date(calendar.getTime().getTime()));
      }
   }

   /**
    * Paint the captions of the <code>GANTT_CHART</code> according to start x and y position
    *
    * @param g       the graphics
    * @param start_x the start x coordinate
    * @param start_y the start y coordinate
    */
   protected void paintGanttChartCaptions(Graphics g, int start_x, int start_y) {
      // *** Do we still need start_x, start_y?
      // Paint left, center and right labels (in the future maybe also top and
      // bottom labels)
      XStyle style = getStyleAttributes();
      FontMetrics metrics = getFontMetrics(style.font());
      g.setColor(style.foreground);
      g.setFont(style.font());
      int ascent = metrics.getAscent();
      int center_y = 0;
      String text = null;
      int x = 0;
      int y = 0;
      Rectangle bounds = null;
      OpProjectComponent box = (OpProjectComponent) getContext();
      XComponent data_set = box.getDataSetComponent();
      OpGanttValidator validator = (OpGanttValidator) (data_set.validator());
      XComponent data_row = null;
      OpProjectComponent child = null;
      Object property;
      for (int index = 0; index < data_set.getChildCount(); index++) {
         child = (OpProjectComponent) _getChild(index);
         data_row = (XComponent) (data_set._getChild(index));
         // *** pc-type should here always be GANTT_ACTIVITY
         // ==> Therefore, we could skip this if-construct
         if ((child.pcType == GANTT_ACTIVITY) && child.getVisible()) {
            bounds = child.getBounds();

            center_y = (bounds.height / 2) + (ascent / 2);

            // caption right ->
            if (box.getCaptionRight() != null) {
               x = start_x + bounds.x + bounds.width + style.gap;
               y = start_y + bounds.y + center_y;
               property = getActivityProperty(data_row, box.getCaptionRight());
               if (property != null) {
                  text = property.toString();
                  g.drawString(text, x, y);
               }
            }

            // <- caption left
            if (box.getCaptionLeft() != null) {
               property = getActivityProperty(data_row, box.getCaptionLeft());
               if (property != null) {
                  text = property.toString();
                  x = bounds.x - metrics.stringWidth(text) - style.gap;
                  y = start_y + bounds.y + center_y;
                  g.drawString(text, x, y);
               }
            }

         }
      }
   }

   /**
    * Retrieves the property identified by the id for the given activity.
    *
    * @param dataRow data row for the queried activitry
    * @param id      the
    * @return a string with
    */
   private Object getActivityProperty(XComponent dataRow, String id) {

      if (id.equals(DETAILS_NAME)) {
         return OpGanttValidator.getName(dataRow);
      }
      else if (id.equals(DETAILS_START)) {
         return OpGanttValidator.getStart(dataRow);
      }
      else if (id.equals(DETAILS_CATEGORY)) {
         return OpGanttValidator.getCategory(dataRow);
      }
      else if (id.equals(DETAILS_FINISH)) {
         return OpGanttValidator.getEnd(dataRow);
      }
      else if (id.equals(DETAILS_DURATION)) {
         XCalendar calendar = XDisplay.getDefaultDisplay().getCalendar();
         return new Double(Math.ceil(OpGanttValidator.getDuration(dataRow) / calendar.getWorkHoursPerDay()));
      }
      else if (id.equals(DETAILS_COMPLETE)) {
         return new Double(OpGanttValidator.getComplete(dataRow));
      }
      else if (id.equals(DETAILS_BASE_EFFORT)) {
         return new Double(OpGanttValidator.getBaseEffort(dataRow));
      }
      else if (id.equals(DETAILS_COST)) {
         double baseCost = 0;
         baseCost += OpGanttValidator.getBaseExternalCosts(dataRow);
         baseCost += OpGanttValidator.getBaseMaterialCosts(dataRow);
         baseCost += OpGanttValidator.getBaseMiscellaneousCosts(dataRow);
         baseCost += OpGanttValidator.getBasePersonnelCosts(dataRow);
         baseCost += OpGanttValidator.getBaseTravelCosts(dataRow);
         return new Double(baseCost);
      }
      else if (id.equals(DETAILS_RESOURCE_NAMES)) {
         List resources = OpGanttValidator.getVisualResources(dataRow);
         String names = "";
         if (resources.size() != 0) {
            for (int i = 0; i < resources.size(); i++) {
               String resourceName = (String) resources.get(i);
               resourceName = OpGanttValidator.choiceCaption(resourceName);
               if (resourceName != null) {
                  names += resourceName;
                  if (i != resources.size() - 1) {
                     names += ",";
                  }
               }
            }
         }
         return names;
      }
      else if (id.equals(DETAILS_PROCEEDS)) {
         return new Double(OpGanttValidator.getBaseProceeds(dataRow));
      }
      return "";
   }

   /**
    * Paints dependecy between gantt activities
    *
    * @param g         graphics
    * @param clip_area graphics clip are
    */
   protected void _paintGanttDependency(Graphics g, Rectangle clip_area) {
      // Paint connector using PATH property
      XStyle style = getStyleAttributes();
      Point[] path = getPath();
      int[] points_x = new int[path.length];
      int[] points_y = new int[path.length];
      Point point = null;
      for (int index = 0; index < path.length; index++) {
         points_x[index] = path[index].x;
         points_y[index] = path[index].y;
      }
      if (getFocused()) {
         g.setColor(style.selection_background);
      }
      else {
         g.setColor(style.foreground);
      }
      g.drawPolyline(points_x, points_y, path.length);
      // Paint right-arrow to target point (last point in path)
      int x = points_x[path.length - 1];
      int y = points_y[path.length - 1];
      points_x = new int[3];
      points_y = new int[3];
      points_x[0] = x - 5;
      points_y[0] = y - 5;
      points_x[1] = x - 5;
      points_y[1] = y + 5;
      points_x[2] = x;
      points_y[2] = y;
      g.fillPolygon(points_x, points_y, 3);
      if (getFocused()) {
         g.setColor(style.selection_foreground);
         g.drawPolygon(points_x, points_y, 3);
      }
   }

   /**
    * Paints the gantt chart history start for <code>STANDARD_ACTIVITY</code> and <code>COLLECTION_ACTIVITY</code>
    * according to the <code>TIME_UNITS</code>. Method must be called on a <code>GANTT_CHART</code> component.
    *
    * @param g                the Graphics
    * @param y                the y coordinate where the start triangle is painted
    * @param chartStartDate   the <code>Date</code> representing <code>GANTT_CHART</code> start date
    * @param historyStartDate the <code>Date</code> representing history start Date
    * @param historyEndDate   the <code>Date</code> representing history end Date
    */
   private void paintGanttChartHistoryStart(Graphics g, int y, Date chartStartDate, Date historyStartDate,
        Date historyEndDate) {
      OpProjectComponent ganttBox = (OpProjectComponent) getContext();
      /* get the time unit for chart */
      double timeWidth = ganttBox._dayWidth();
      int grid_y = getGridY();
      /* points to draw */
      int[] pointsX = new int[3];
      int[] pointsY = new int[3];

      int x;
      double unitRatio = getUnitRatio(ganttBox.getTimeUnit());
      long chartStart = chartStartDate.getTime();
      int startX = (int) ((historyStartDate.getTime() - chartStart) * timeWidth / (XCalendar.MILLIS_PER_DAY * unitRatio));
      int endX = (int) ((historyEndDate.getTime() - chartStart + XCalendar.MILLIS_PER_DAY) * timeWidth / (XCalendar.MILLIS_PER_DAY * unitRatio));

      x = startX;
      pointsX[0] = x;
      pointsY[0] = y + grid_y / 4 + grid_y / 2;
      pointsX[1] = (x - grid_y / 2);
      pointsY[1] = y + grid_y / 4;
      pointsX[2] = (x + grid_y / 2);
      pointsY[2] = y + grid_y / 4;
      /* draws the start poligon */
      g.drawPolygon(pointsX, pointsY, 3);
      /* draws a line between start and end */
      g.drawLine(x, y + grid_y / 4, endX, y + grid_y / 4);
   }

   /**
    * Paints the gantt chart history end for <code>STANDARD_ACTIVITY</code>,<code>COLLECTION_ACTIVITY</code> and
    * <code>MILESTONE_ACTIVITY</code> according to the <code>TIME_UNITS</code>. Method must be called on a
    * <code>GANTT_CHART</code> component.
    *
    * @param g              the Graphics
    * @param y              the y coordinate where the start triangle is painted
    * @param chartStartDate the <code>Date</code> representing <code>GANTT_CHART</code> start date
    * @param historyEndDate the <code>Date</code> representing history end Date
    */
   private void paintGanttChartHistoryEnd(Graphics g, int y, Date chartStartDate, Date historyEndDate) {

      OpProjectComponent ganttBox = (OpProjectComponent) getContext();
      double timeWidth = ganttBox._dayWidth();
      int grid_y = getGridY();
      /* points to draw */
      int[] pointsX = new int[3];
      int[] pointsY = new int[3];

      int x;
      double unitRatio = getUnitRatio(ganttBox.getTimeUnit());
      long chartStart = chartStartDate.getTime();
      int endX = (int) ((historyEndDate.getTime() - chartStart + XCalendar.MILLIS_PER_DAY) * timeWidth / (XCalendar.MILLIS_PER_DAY * unitRatio));
      x = endX;

      pointsX[0] = x;
      pointsY[0] = y + grid_y / 4 + grid_y / 2;
      pointsX[1] = (x - grid_y / 2);
      pointsY[1] = y + grid_y / 4;
      pointsX[2] = (x + grid_y / 2);
      pointsY[2] = y + grid_y / 4;
      /* fills the end triangle poligon */
      g.fillPolygon(pointsX, pointsY, 3);
   }

   /**
    * Paints the <code>GANTT_CHART</code> history
    *
    * @param g       the Graphics
    * @param history a <code>DATA_SET</code> of histories
    */
   protected void _paintGanttChartHistory(Graphics g, XComponent history) {
      // Assumes that history and data-set have the same number/order of rows
      XStyle style = getStyleAttributes();
      // *** Maybe have special HISTORY_STYLE property?
      OpProjectComponent box = (OpProjectComponent) getContext();
      double timeWidth = box._dayWidth();
      int grid_y = getGridY();

      Date chartStartDate = getStart();
      Date chartEnd = getEnd();

      XComponent history_row = null;
      XComponent history_cell = null;
      OpProjectComponent component = null;
      List start_history = null;
      List end_history = null;
      Date historyStartDate = null;
      Date historyEndDate = null;

      int y = 0;
      g.setColor(style.foreground);

      for (int i = 0; i < history.getChildCount(); i++) {
         component = (OpProjectComponent) _getChild(i);
         if (component.getVisible()) {
            history_row = (XComponent) (history._getChild(i));
            // Empty rows (no cells) are used to ensure that history is aligned w/Gantt-chart
            if (history_row.getChildCount() > 0) {
               history_cell = (XComponent) (history_row._getChild(START_HISTORY_COLUMN_INDEX));
               start_history = history_cell.getListValue();
               history_cell = (XComponent) (history_row._getChild(END_HISTORY_COLUMN_INDEX));
               end_history = history_cell.getListValue();
               for (int j = 0; j < start_history.size(); j++) {
                  historyStartDate = (Date) (start_history.get(j));
                  historyEndDate = (Date) (end_history.get(j));
                  switch (component.getActivityType()) {
                     case COLLECTION_ACTIVITY:
                     case STANDARD_ACTIVITY:
                     case SCHEDULED_TASK_ACTIVITY:
                        paintGanttChartHistoryStart(g, y, chartStartDate, historyStartDate, historyEndDate);
                        paintGanttChartHistoryEnd(g, y, chartStartDate, historyEndDate);
                        break;

                     case MILESTONE_ACTIVITY:
                        paintGanttChartHistoryEnd(g, y, chartStartDate, historyEndDate);
                        break;
                  }
               }
            }
            y += grid_y;
         }
      }
   }

   /**
    * Paints the <code>UTILIZATION_ROW</code> component
    *
    * @param g         the Graphics
    * @param clip_area the rectangle clip area
    */
   protected void paintUtilizationRow(Graphics g, Rectangle clip_area) {
      XStyle style = getStyleAttributes();
      Rectangle bounds = getBounds();
      OpProjectComponent box = (OpProjectComponent) getContext();
      double day_width = box._dayWidth();

      XComponent data_row = getDataRow();
      List values = ((XComponent) (data_row.getChild(UTILIZATION_VALUES_COLUMN_INDEX))).getListValue();
      double value;
      Color previous_color = null;
      Color color = null;
      double x = 0;
      // "pixels" to be painted
      double burstWidth = 0;
      // pixelAmount regarding to the timeunit and daywidth
      double pixelAmount;
      double ratio = getUnitRatio(box.getTimeUnit());
      pixelAmount = day_width / ratio;

      double available = ((XComponent) getDataRow().getChild(UTILIZATION_AVAILABLE_COLUMN_INDEX)).getDoubleValue();

      XComponent colorDataSet = box.getColorDataSetComponent();

      for (int i = 0; i < values.size(); i++) {
         value = ((Double) (values.get(i))).doubleValue();
         color = getRowColor(colorDataSet, value, available);
         if (color == previous_color) {
            burstWidth += pixelAmount;
         }
         else {
            // draw prev burstWidth (color was changed)
            if (previous_color != null) {
               paintBurst(g, x, burstWidth, bounds.height, previous_color, color, style);
            }
            x += burstWidth;
            // another color is used - add one burst unit for the color that was just changed
            previous_color = color;
            burstWidth = pixelAmount;
         }
      }

      // Draw last burstWidth
      if (color != null && burstWidth >= 1) {
         paintBurst(g, x, burstWidth, bounds.height, color, null, style);
      }
   }

   /**
    * Gets the drawing color for a given utilization value.
    *
    * @param colorDataSet Data set containing the utilization colors.
    * @param value        Utilization values that has to be painted.
    * @param available    The available value (the utilization will be compared against this value in order to decide the color)
    * @return Color for the given utilization value and available.
    */
   private Color getRowColor(XComponent colorDataSet, double value, double available) {
      Color color;
      XComponent row;
      row = (XComponent) colorDataSet.getChild(0);
      Color highlyUnderUsedColor = (Color) ((XComponent) row.getChild(1)).getValue();

      row = (XComponent) colorDataSet.getChild(1);
      Color underUsedColor = (Color) ((XComponent) row.getChild(1)).getValue();

      row = (XComponent) colorDataSet.getChild(2);
      Color normalUsedColor = (Color) ((XComponent) row.getChild(1)).getValue();

      row = (XComponent) colorDataSet.getChild(3);
      Color overUsedColor = (Color) ((XComponent) row.getChild(1)).getValue();

      row = (XComponent) colorDataSet.getChild(4);
      Color higlyOverUsedColor = (Color) ((XComponent) row.getChild(1)).getValue();
      color = null;
      if (value > available * 1.2d) {
         color = higlyOverUsedColor;
      }
      else if (value > available) {
         color = overUsedColor;
      }
      else if (value >= available * 0.8d) {
         color = normalUsedColor;
      }
      else if (value >= available * 0.5d) {
         color = underUsedColor;
      }
      else if (value > 0) {
         color = highlyUnderUsedColor;
      }
      return color;
   }

   /**
    * Draws an utilization rectangle from an utilization row definde by a burst (burstWidth).
    *
    * @param g          Graphic context used for painting
    * @param x          X position for the drawn utilization row part.
    * @param width      Width of the drawn burst
    * @param height     Height of the drawn burst
    * @param burstColor Color the burst is drawn with
    * @param newColor   The new color (color of the following burst - null if no burst will follow).
    * @param style      Utilization row style.
    */
   private void paintBurst(Graphics g, double x, double width, int height, Color burstColor, Color newColor, XStyle style) {
      g.setColor(burstColor);
      Rectangle2D rec = new Rectangle();
      rec.setRect(x, 0, width, height);
      ((Graphics2D) g).fill(rec);
      g.setColor(style.border_light);
      //one px has to be substracted from the frame in order for the burst not to overlap with the weekend (on day view)
      if (newColor == null) {
         rec.setFrame(x, 0, width - 1, height);
      }
      else {
         rec.setFrame(x, 0, width, height);
      }
      ((Graphics2D) g).draw(rec);
   }

   protected void _paintUtilizationChart(Graphics g, Rectangle clip_area) {
      _paintTimeChartBackground(g, clip_area);
      // Paint today marker
      paintProjectStartFinishLines(g, clip_area, false);
      paintChildren(g, clip_area);
   }

   /**
    * Paints the start, finish and today markers for GANTT and utilization chart
    *
    * @param g         Graphic context used for painting.
    * @param clip_area Paint clip area.
    */
   protected void paintProjectStartFinishLines(Graphics g, Rectangle clip_area, boolean showStartFinish) {
      OpProjectComponent box = (OpProjectComponent) getContext();
      Rectangle bounds = getBounds();
      XComponent view_port = (XComponent) box.getViewPort();
      OpProjectComponent ganttChart = (OpProjectComponent) view_port.getChild(0);


      long chartStart = ganttChart.getStart().getTime();
      int x = 0;
      double dayWidth = box._dayWidth();
      double unitRatio = getUnitRatio(box.getTimeUnit());

      //current day
      int width = (int) (dayWidth / unitRatio);
      if (width == 0) {
         width++;
      }
      long today = XCalendar.today().getTime();
      x = (int) ((today - chartStart) * dayWidth / (XCalendar.MILLIS_PER_DAY * unitRatio));
      XStyle style = getStyleAttributes();
      g.setColor(style.selection_background);
      if (style.selection_gradient != null) {
         drawGradientImage(g, style.selection_gradient, x, 0, width, bounds.height);
         g.drawRect(x, 0, width, bounds.height);
      }
      else {
         g.fillRect(x, 0, width, bounds.height);
      }

      if (showStartFinish) {

         XComponent data_set = box.getDataSetComponent();
         OpGanttValidator validator = (OpGanttValidator) (data_set.validator());
         if (validator == null) {
            return;
         }

         if (validator.getProjectStart() == null) {
            return;
         }

         long projectStart = validator.getProjectStart().getTime();

         // start bar
         x = (int) ((projectStart - chartStart) * dayWidth / (XCalendar.MILLIS_PER_DAY * unitRatio));
         g.setColor(XStyle.DEFAULT_GREEN);
         g.fillRect(x - 1, 0, 2, bounds.height);

         // end bar
         if (validator.getProjectFinish() == null) {
            return;
         }
         long projectFinish = validator.getProjectFinish().getTime();
         projectFinish += XCalendar.MILLIS_PER_DAY; // add one day => draw the end line at the end of the previous day
         x = (int) ((projectFinish - chartStart) * dayWidth / (XCalendar.MILLIS_PER_DAY * unitRatio));
         g.setColor(XStyle.DEFAULT_RED);
         g.fillRect((int) x, 0, 2, bounds.height);

      }

   }

   /**
    * Paints the time background accordind to <code>TIME_UNIT<code> for <code>GANTT_CHART</code> and
    * <code>UTILIZATION_CHART<code> components
    *
    * @param g         graphics
    * @param clip_area graphics clip area
    */
   protected void _paintTimeChartBackground(Graphics g, Rectangle clip_area) {
      // *** Paint grid and "grey-out" holidays
      XStyle style = getStyleAttributes();
      Rectangle bounds = getBounds();
      g.setColor(style.background);
      g.fillRect(0, 0, bounds.width, bounds.height);
      g.setColor(style.alternate_background);
      OpProjectComponent box = (OpProjectComponent) getContext();
      XComponent ganttHeader = (XComponent) box.getChild(3).getChild(0);
      Rectangle ganttHeaderBounds = ganttHeader.getBounds();

      XComponent view_port = (XComponent) box.getViewPort();
      OpProjectComponent ganttChart = (OpProjectComponent) view_port.getChild(0);
      XCalendar xCalendar = XDisplay.getDefaultDisplay().getCalendar();
      Date currentDay = ganttChart.getStart();

      byte time_unit = box.getTimeUnit();
      if (time_unit == XCalendar.DAYS || time_unit == XCalendar.WEEKS) {
         double x = 0;
         double day_width;
         if (time_unit == XCalendar.DAYS) {
            day_width = box._dayWidth();
         }
         else {
            day_width = box._dayWidth() / 7.0;
         }
         int width = getBounds().width;
         while (x < width) {
            if (!xCalendar.isWorkDay(currentDay)) {
               g.fillRect((int) x, 0, (int) Math.ceil(day_width), bounds.height);
            }
            currentDay = new Date(currentDay.getTime() + XCalendar.MILLIS_PER_DAY);
            x += day_width;
         }
      }
      else {
         //month view - draw the weekends
         double widthIncrement = box._dayWidth(); //a month
         int x = 0;
         while (x < bounds.width) {
            g.drawLine(x, 0, x, bounds.height);
            x += widthIncrement;
         }
      }
   }

   public void paint(Graphics g, Rectangle clip_area) {
      // *** Attention with clipping (milestone) and transparency
      // (connector/dependency)
      Rectangle bounds = getBounds();
      XStyle style = getStyleAttributes();
      switch (pcType) {
         case GANTT_CHART:
            initializeCalendar();
            _paintTimeChartBackground(g, clip_area);
            paintProjectStartFinishLines(g, clip_area, true);

            // Paint activities and dependencies (in this order)
            insertLine = getGanttActivityLine();
            paintChildren(g, clip_area);

            // Paint history
            XComponent history = ((OpProjectComponent) getContext()).getHistory();
            if (history != null) {
               _paintGanttChartHistory(g, history);
            }

            //paint activity line
            if (insertLine != null) {
               Color oldColor = g.getColor();
               g.setColor(getStyleAttributes().border_light);
               g.drawLine((int) insertLine.getX1(), (int) insertLine.getY1(),
                    (int) insertLine.getX2(), (int) insertLine.getY2());
               g.setColor(oldColor);
            }

            // Paint captions
            paintGanttChartCaptions(g, 0, 0);

            // paint ghost rectangle
            Rectangle ghostShape = getDrawingRectangle();
            if (ghostShape != null) {
               g.drawRect(ghostShape.x, ghostShape.y, ghostShape.width, ghostShape.height);
            }

            // paint ghost line
            Line2D ghost = getDrawingLine();
            if (ghost != null) {
               g.drawLine((int) ghost.getX1(), (int) ghost.getY1(), (int) ghost.getX2(), (int) ghost.getY2());
            }

            break;
         case GANTT_ACTIVITY:
            // Paint (sub-)project bar
            // *** TODO: Think about column ActivityType in data-set
            // ==> Might even make sense in database/persistent model
            OpProjectComponent chart = (OpProjectComponent) getParent();
            Graphics2D g2 = ((Graphics2D) g);
            switch (getActivityType()) {
               case COLLECTION_ACTIVITY:
                  drawActivityBorder = intersectsDragSource();
                  if (drawActivityBorder) {
                     chart.setInsertLine(null);
                  }

                  // Paint collection activity
                  // Check for category color
                  Integer colorIndex = getColorIndex();
                  if (getFocused() || drawActivityBorder || getSelected()) {
                     g.setColor(style.selection_background);
                  }
                  else if ((colorIndex != null) && (colorIndex.intValue() < XStyle.colorSchema.size())) {
                     g.setColor((Color) XStyle.colorSchema.get(colorIndex.intValue()));
                  }
                  else {
                     g.setColor(style.foreground);
                  }

                  Polygon shape = getComponentPolygon();

                  //fill component
                  g.fillPolygon(shape);

                  //draw border
                  if (drawActivityBorder) {
                     Color oldColor = g.getColor();
                     g.setColor(style.border_light);
                     Stroke oldStroke = ((Graphics2D) g).getStroke();
                     g2.setStroke(new BasicStroke(2));
                     g.drawPolygon(shape);
                     g2.setStroke(oldStroke);
                     g.setColor(oldColor);
                  }
                  else {
                     if (getFocused() || colorIndex != null) {
                        Color oldColor = g.getColor();
                        g.setColor(style.foreground);
                        g.drawPolygon(shape);
                        g.setColor(oldColor);
                     }
                  }

                  break;
               case MILESTONE_ACTIVITY:
                  // Paint milestone activity
                  double value = 0;
                  XComponent data_row = getDataRow();
                  if (data_row != null) {
                     value = OpGanttValidator.getComplete(data_row);
                  }
                  if (value == 100) {
                     g.setColor(style.foreground);
                  }
                  else {
                     // Check for category color
                     colorIndex = getColorIndex();
                     if ((colorIndex != null) && (colorIndex.intValue() < XStyle.colorSchema.size())) {
                        g.setColor((Color) XStyle.colorSchema.get(colorIndex.intValue()));
                     }
                     else {
                        g.setColor(style.background);
                     }
                  }
                  if (getFocused() || getSelected()) {
                     g.setColor(style.selection_background);
                  }
                  g.fillPolygon(getComponentPolygon());
                  g.setColor(style.border_light);
                  g.drawPolygon(getComponentPolygon());
                  break;

               case SCHEDULED_TASK_ACTIVITY:
               case STANDARD_ACTIVITY:
                  // Paint standard activity
                  drawActivityBorder = intersectsDragSource();
                  if (drawActivityBorder) {
                     chart.setInsertLine(null);
                  }

                  if (getFocused() || drawActivityBorder || getSelected()) {
                     g.setColor(style.selection_background);
                  }
                  else {
                     // Check for category color
                     colorIndex = getColorIndex();
                     if ((colorIndex != null) && (colorIndex.intValue() < XStyle.colorSchema.size())) {
                        g.setColor((Color) XStyle.colorSchema.get(colorIndex.intValue()));
                     }
                     else {
                        g.setColor(style.alternate_background);
                     }
                  }

                  g.fillPolygon(getComponentPolygon());
                  g.setColor(style.border_light);
                  g.drawPolygon(getComponentPolygon());

                  data_row = getDataRow();
                  if (data_row != null) {
                     value = OpGanttValidator.getComplete(data_row);
                     if (value > 0) {
                        // (Partially) completed activity: Draw inner bar (use
                        // gap)
                        int max_size = bounds.width;
                        int value_size = (int) (max_size * value / 100);
                        g.setColor(style.foreground);
                        g.fillRect(0, style.top + style.gap, value_size, bounds.height - style.top - style.bottom
                             - style.gap - style.gap + 1);
                     }
                  }

                  // Provide resize-arrow
                  if (getFocused() || drawActivityBorder) {
                     int center_y = (bounds.height - style.top - style.bottom) / 2 + style.top;
                     int[] points_x = new int[3];
                     int[] points_y = new int[3];
                     points_x[0] = bounds.width - 8;
                     points_y[0] = center_y - 4;
                     points_x[1] = bounds.width - 8;
                     points_y[1] = center_y + 4;
                     points_x[2] = bounds.width;
                     points_y[2] = center_y;
                     g.setColor(style.selection_background);
                     g.fillPolygon(points_x, points_y, 3);
                     g.setColor(style.selection_foreground);
                     g.drawPolygon(points_x, points_y, 3);
                  }

                  if (drawActivityBorder) {
                     Color oldColor = g.getColor();
                     g.setColor(XStyle.DEFAULT_BLACK);
                     Stroke oldStroke = ((Graphics2D) g).getStroke();
                     g2.setStroke(new BasicStroke(2));
                     g2.drawPolygon(getComponentPolygon());
                     g2.setStroke(oldStroke);
                     g.setColor(oldColor);
                  }
            }
            // Mark if focused
            break;
         case GANTT_DEPENDENCY:
            _paintGanttDependency(g, clip_area);
            break;
         case GANTT_HEADER:
            paintGanttHeader(g, clip_area);
            break;
         case UTILIZATION_ROW:
            paintUtilizationRow(g, clip_area);
            break;
         case UTILIZATION_CHART:
            _paintUtilizationChart(g, clip_area);
            break;
         case UTILIZATION_HEADER:
            paintGanttHeader(g, clip_area);
            break;
         case GANTT_BOX:
         case UTILIZATION_BOX:
            drawBox(g, style, 0, 0, bounds.width, bounds.height);
            super.paint(g, clip_area);
            break;
         default:
            super.paint(g, clip_area);
      }
   }

   /**
    * Sets the componentShape for this project component
    */
   private void createComponentShape() {

      Rectangle bounds = getBounds();
      XStyle style = getStyleAttributes();
      int center_y = (bounds.height - style.top - style.bottom) / 2 + style.top;
      Area border = new Area();
      int[] points_x = new int[4];
      int[] points_y = new int[4];


      switch (getActivityType()) {
         case COLLECTION_ACTIVITY: {
            //left triangle
            points_x[0] = 0;
            points_y[0] = center_y;
            points_x[1] = center_y;
            points_y[1] = bounds.height - style.bottom;
            points_x[2] = center_y + center_y;
            points_y[2] = center_y;
            Polygon leftTriangle = new Polygon(points_x, points_y, 3);

            //right triangle
            points_x[0] = bounds.width - center_y - center_y;
            points_y[0] = center_y;
            points_x[1] = bounds.width - center_y;
            points_y[1] = bounds.height - style.bottom;
            points_x[2] = bounds.width;
            points_y[2] = center_y;
            Polygon rightTriangle = new Polygon(points_x, points_y, 3);

            //main rectangle
            points_x[0] = 0;
            points_y[0] = style.top;
            points_x[1] = 0;
            points_y[1] = center_y;
            points_x[2] = bounds.width;
            points_y[2] = center_y;
            points_x[3] = bounds.width;
            points_y[3] = style.top;

            Polygon mainRectangle = new Polygon(points_x, points_y, 4);

            border = new Area(mainRectangle);
            border.add(new Area(leftTriangle));
            border.add(new Area(rightTriangle));

            componentShape = getPolygonFromArea(border);
         }
         break;

         case MILESTONE_ACTIVITY: {
            int center_x = bounds.width / 2;
            center_y = (bounds.height - style.top - style.bottom) / 2 + style.top;
            points_x = new int[4];
            points_y = new int[4];
            points_x[0] = center_x;
            points_y[0] = style.top;
            points_x[1] = bounds.width - 1;
            points_y[1] = center_y;
            points_x[2] = center_x;
            points_y[2] = bounds.height - style.bottom;
            points_x[3] = 0;
            points_y[3] = center_y;
            componentShape = new Polygon(points_x, points_y, 4);
         }
         break;

         case SCHEDULED_TASK_ACTIVITY:
         case STANDARD_ACTIVITY: {

            points_x[0] = 0;
            points_y[0] = style.top;
            points_x[1] = 0;
            points_y[1] = bounds.height - style.bottom;
            points_x[2] = bounds.width;
            points_y[2] = bounds.height - style.bottom;
            points_x[3] = bounds.width;
            points_y[3] = style.top;
            for (int i = 1; i < 4; i++) {
               if (points_y[i] < 0) {
                  points_y[i] = 0;
               }
               if (points_x[i] < 0) {
                  points_x[i] = 0;
               }
            }
            componentShape = new Polygon(points_x, points_y, 4);
         }
         break;
      }
   }

   private Polygon getPolygonFromArea(Area border) {
      PathIterator iterator = border.getPathIterator(null);

      int shapePoints_x[] = new int[20];
      int shapePoints_y[] = new int[20];
      int index = 0;
      while (!iterator.isDone()) {
         double[] points = new double[6];
         int type = iterator.currentSegment(points);
         if (type != PathIterator.SEG_CLOSE) {
            shapePoints_x[index] = (int) points[0];
            shapePoints_y[index] = (int) points[1];
            index++;
         }
         else {
            shapePoints_x[index] = shapePoints_x[0];
            shapePoints_y[index] = shapePoints_y[0];
            index++;
         }
         iterator.next();
      }
      return new Polygon(shapePoints_x, shapePoints_y, index);
   }

   /**
    * Returns the activity insert line at move/drag time
    */
   private Line2D getGanttActivityLine() {
      XView dragSource = getDisplay().getDragSource();
      if (dragSource != null && dragSource instanceof OpProjectComponent) {
         OpProjectComponent activity = (OpProjectComponent) dragSource;
         if (activity.pcType == GANTT_ACTIVITY &&
              activity.getDragPosition() != null) {
            int gridY = ((OpProjectComponent) getContext()).getGridY();
            int ganttIndex = activity.getBounds().y / gridY;
            XComponent draggedDataRow = activity.getDataRow();
            int childCount = getGanttIndexes().size();
            if (ganttIndex >= childCount) {
               ganttIndex = childCount - 1;
            }
            Integer intVal = (Integer) getGanttIndexes().get(new Integer(ganttIndex));
            int dataIndex = -1;
            if (intVal != null) {
               dataIndex = intVal.intValue();
            }
            if (dataIndex != draggedDataRow.getIndex() && dataIndex + 1 != draggedDataRow.getIndex()) {
               return new Line2D.Double(0, (ganttIndex + 1) * gridY, getBounds().width, (ganttIndex + 1) * gridY);
            }
         }
      }
      return null;
   }

   /**
    * Tests if a component intersects with the drag source (if the drag source is a project component)
    *
    * @return true;
    */
   private boolean intersectsDragSource() {
      //if the current activity intersects with the dragged activity...
      XView dragSource = getDisplay().getDragSource();
      if (dragSource != null && dragSource instanceof OpProjectComponent) {
         OpProjectComponent activity = (OpProjectComponent) dragSource;

         if (activity.getDragPosition() != null && activity.pcType == GANTT_ACTIVITY && activity != this) {

            Area intersection = this.intersects(activity);
            if (intersection.isEmpty()) {
               return false;
            }

            int gridY = ((OpProjectComponent) getContext()).getGridY();
            int ganttIndex = activity.getBounds().y / gridY;

            Map ganttIndexes = ((OpProjectComponent) getParent()).getGanttIndexes();
            int childCount = ganttIndexes.size();
            if (ganttIndex >= childCount) {
               ganttIndex = childCount - 1;
            }
            Integer intVal = (Integer) ganttIndexes.get(new Integer(ganttIndex));
            int dataIndex = -1;
            if (intVal != null) {
               dataIndex = intVal.intValue();
            }

            if (dataIndex == this.getDataRow().getIndex()) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Computes the intersection area of this component with the given component
    *
    * @param activity component that will be intersected with this one
    * @return area of the intersection for the 2 components.
    */
   private Area intersects(OpProjectComponent activity) {

      Polygon componentPolygon = this.getComponentPolygon();
      componentPolygon.translate(getBounds().x, getBounds().y);
      Area intersection = new Area(componentPolygon);
      Polygon draggedComponentPolygon = activity.getComponentPolygon();
      draggedComponentPolygon.translate(activity.getBounds().x, activity.getBounds().y);
      intersection.intersect(new Area(draggedComponentPolygon));
      componentPolygon.translate(-getBounds().x, -getBounds().y);
      draggedComponentPolygon.translate(-activity.getBounds().x, -activity.getBounds().y);
      return intersection;
   }

   /**
    * Paints the <code>GANTT_CHART </code> or <code>UTILIZATION_CHART</code> header
    *
    * @param g         the Graphics
    * @param clip_area clip area
    */
   protected void paintGanttHeader(Graphics g, Rectangle clip_area) {
      // Paint upper and lower scales depending on time-unit
      OpProjectComponent box = (OpProjectComponent) getContext();
      switch (box.getTimeUnit()) {
         case XCalendar.DAYS:
            paintDaysGanttHeader(g, clip_area);
            break;
         case XCalendar.WEEKS:
            paintWeeksGanttHeader(g, clip_area);
            break;
         case XCalendar.MONTHS:
            paintMonthsGanttHeader(g, clip_area);
            break;
      }
   }

   protected void drawGanttHeaderBackground(Graphics g, Rectangle bounds, XStyle style, int line_height) {
      if (style.gradient != null) {
         drawGradientImage(g, style.gradient, 0, 0, bounds.width, bounds.height);
      }
      else {
         g.setColor(style.background);
         g.fillRect(0, 0, bounds.width, bounds.height);
      }
      g.setColor(style.border_light);
      g.drawLine(0, 0, bounds.width, 0);
      g.drawLine(0, line_height + 1, bounds.width, line_height + 1);
      g.setColor(style.border_dark);
      g.drawLine(0, line_height, bounds.width, line_height);
      g.drawLine(0, bounds.height - 1, bounds.width, bounds.height - 1);
   }

   protected void paintDaysGanttHeader(Graphics g, Rectangle clip_area) {
      // Upper scale: Weeks (date of first weekday)
      // Lower scale: Days (first initial of weekday)
      Rectangle bounds = getBounds();
      XStyle style = getStyleAttributes();
      FontMetrics metrics = getFontMetrics(style.font());
      int line_height = metrics.getAscent() + metrics.getDescent() + style.top + style.bottom;
      int ascent = metrics.getAscent();
      OpProjectComponent gantt_chart = (OpProjectComponent) (((XComponent) getParent().getParent()).getBoxContent());
      // int first_week_length = gantt_chart.getFirstWorkWeekLength() +
      // gantt_chart.getFirstWeekendLength();
      Date start = gantt_chart.getStart();
      long time = start.getTime();
      XCalendar calendar = XDisplay.getDefaultDisplay().getCalendar();
      char[] weekday_initials = calendar.getWeekdayInitials();
      calendar.getCalendar().setTime(start);
      int weekday = calendar.getCalendar().get(Calendar.DAY_OF_WEEK);

      int x = 0;
      // width increment for x axis (uses the XCalendar.WEEKDAY_INITIALS max width + style.gap*2)
      int widthIncrement = (int) ((OpProjectComponent) getContext())._dayWidth();
      Date date = new Date(0);

      // DateFormat date_format = DateFormat.getDateInstance();

      drawGanttHeaderBackground(g, bounds, style, line_height);

      g.setFont(style.font());
      while (x < bounds.width) {
         // *** First day of week should be configurable (USA: Sunday)
         if (weekday == Calendar.MONDAY) {
            // Draw upper scale (week separater and date of first weekday)
            g.setColor(style.border_light);
            g.drawLine(x, 1, x, line_height - 1);
            date.setTime(time);
            g.setColor(style.foreground);
            g.drawString(calendar.localizedDateToString(date), x + style.gap, style.top + ascent);
         }
         // Draw lower scale (day separator and weekday initials)
         // *** Should be centered (or configurable)
         g.setColor(style.border_light);
         g.drawLine(x, line_height + 1, x, line_height + line_height - 2);
         g.setColor(style.foreground);
         g.drawString("" + weekday_initials[weekday], x + style.gap, style.top + line_height + ascent);
         // Go to next day
         time += XCalendar.MILLIS_PER_DAY;
         weekday = calendar.nextWeekday(weekday);
         x += widthIncrement;

      }
   }

   protected void paintWeeksGanttHeader(Graphics g, Rectangle clip_area) {
      // Upper scale: Quarters; lower scale: Weeks
      Rectangle bounds = getBounds();
      XStyle style = getStyleAttributes();
      FontMetrics metrics = getFontMetrics(style.font());
      int line_height = metrics.getAscent() + metrics.getDescent() + style.top + style.bottom;
      int ascent = metrics.getAscent();
      OpProjectComponent gantt_chart = (OpProjectComponent) (((XComponent) getParent().getParent()).getBoxContent());
      Date start = gantt_chart.getStart();
      long start_time = start.getTime();

      XCalendar calendar = XDisplay.getDefaultDisplay().getCalendar();
      Calendar j_calendar = calendar.getCalendar();
      j_calendar.setTimeInMillis(start_time);

      drawGanttHeaderBackground(g, bounds, style, line_height);

      g.setFont(style.font());

      int x = 0;
      // width increment for x axis (uses the getMaxxDayWidthIncrement *2)
      int weekWidthIncrement = (int) ((OpProjectComponent) getContext())._dayWidth();
      // the week of the year coresponding to startDate
      int week = j_calendar.get(Calendar.WEEK_OF_YEAR);

      // Draw weeks scale
      while (x < bounds.width) {
         // *** Weeks: Simple, always the same length (WEEK_DURATION)
         g.setColor(style.border_light);
         g.drawLine(x, line_height + 1, x, line_height + line_height - 2);
         g.setColor(style.foreground);
         g.drawString(new StringBuffer().append(week).toString(), x + style.gap, style.top + line_height + ascent);
         week++;
         if (week > 52) {
            week = 1;
         }
         start_time += XCalendar.MILLIS_PER_WEEK;
         // increment the x coordinate
         x += weekWidthIncrement;
      }
      // Draw quarters scale (JANUARY = 0; Modulo 3 returns 0 for 1st
      // quarter-month)

      start_time = start.getTime();
      j_calendar.setTimeInMillis(start_time);

      // dx coordinate
      double dx = 0;
      // numeber of days in a month
      int days = 0;
      // number of weeks in a month
      int weeks = 0;

      // // *** While month != first-quater-month (modulo 3 returns zero)
      // // ==> Set month; go back actual-maximum of month-days * day-width
      int month = j_calendar.get(Calendar.MONTH) + 1;

      if (month % 3 != 0) {
         // skip the JANUARY THE FIRST WEEK
         if (!((month == 1) && (j_calendar.get(Calendar.WEEK_OF_YEAR) == 1))) {
            int weekNumber = j_calendar.get(Calendar.WEEK_OF_YEAR);
            while (!isQuarter(weekNumber)) {
               // decreasa a week
               j_calendar.set(Calendar.DAY_OF_MONTH, j_calendar.get(Calendar.DAY_OF_MONTH) - 7);
               // get the new week number
               weekNumber = j_calendar.get(Calendar.WEEK_OF_YEAR);
               dx -= weekWidthIncrement;
            }
         }
      }
      else {
         int weekNumber = j_calendar.get(Calendar.WEEK_OF_YEAR);
         while (!isQuarter(weekNumber)) {
            // decreasa a week
            j_calendar.set(Calendar.DAY_OF_MONTH, j_calendar.get(Calendar.DAY_OF_MONTH) - 7);
            // get the new week number
            weekNumber = j_calendar.get(Calendar.WEEK_OF_YEAR);
            dx -= weekWidthIncrement;
         }
      }
      int year = j_calendar.get(Calendar.YEAR);

      month = j_calendar.get(Calendar.MONTH) + 1;
      int quarter = (month / 3) + 1;
      if (quarter > 4) {
         quarter = 1;
         year = j_calendar.get(Calendar.YEAR) + 1;
      }

      // paint quorters
      while (dx < bounds.width) {
         // *** Draw quarter
         g.setColor(style.border_light);
         g.drawLine((int) dx, 1, (int) dx, line_height - 1);
         // j_calendar.setTimeInMillis(time);
         g.setColor(style.foreground);

         String yearS = String.valueOf((year % 100));
         if (year % 100 < 10) {
            yearS = "0" + yearS;
         }
         g.drawString(new StringBuffer().append('Q').append(quarter).append('/').append(yearS).toString(), (int) dx
              + style.gap, style.top + ascent);

         quarter++;
         if (quarter > 4) {
            quarter = 1;
            year++;
         }

         // *** Advance dx
         for (month = 0; month < 3; month++) {
            j_calendar.add(Calendar.MONTH, 1);
            days = j_calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            // number of weeks in the month
            weeks = days / 7;
            dx += weeks * weekWidthIncrement;
         }
         dx += weekWidthIncrement;

         if (j_calendar.get(Calendar.WEEK_OF_YEAR) > 52) {
            dx += weekWidthIncrement;
         }
      }
   }

   protected void paintMonthsGanttHeader(Graphics g, Rectangle clip_area) {
      // Upper scale: Years; lower scale: Months
      Rectangle bounds = getBounds();
      XStyle style = getStyleAttributes();
      FontMetrics metrics = getFontMetrics(style.font());
      int line_height = metrics.getAscent() + metrics.getDescent() + style.top + style.bottom;
      int ascent = metrics.getAscent();
      OpProjectComponent gantt_chart = (OpProjectComponent) (((XComponent) getParent().getParent()).getBoxContent());
      Date start = gantt_chart.getStart();
      XCalendar calendar = XDisplay.getDefaultDisplay().getCalendar();
      Calendar j_calendar = calendar.getCalendar();
      j_calendar.setTime(start);

      drawGanttHeaderBackground(g, bounds, style, line_height);

      g.setFont(style.font());

      // Go back to 1st of month
      OpProjectComponent box = (OpProjectComponent) getContext();
      // double day_width = box._dayWidth();
      // width increment for x axis (uses the XCalendar.MONTH_INITIALS max width + style.gap)
      int widthIncrement = (int) box._dayWidth();
      double dx = 0;

      // Draw months and year scale
      int month = j_calendar.get(Calendar.MONTH); // begin from the current month
      int year = j_calendar.get(Calendar.YEAR); // begin from th current year
      char[] month_initials = calendar.getMonthInitials();

      while (dx < bounds.width) {
         // *** Weeks: Simple, always the same length (WEEK_DURATION)
         g.setColor(style.border_light);
         g.drawLine((int) dx, line_height + 1, (int) dx, line_height + line_height - 2);
         g.setColor(style.foreground);
         g.drawString(new StringBuffer().append(month_initials[month]).toString(), (int) dx + style.gap, style.top + line_height
              + ascent);
         if (month == 0) {
            // Draw year
            g.setColor(style.border_light);
            g.drawLine((int) dx, 1, (int) dx, line_height - 1);
            g.setColor(style.foreground);
            String yearString = String.valueOf((year % 100));
            if (year % 100 < 10) {
               yearString = "0" + yearString;
            }
            g.drawString(new StringBuffer().append(yearString).toString(), (int) dx + style.gap, style.top + ascent);
         }
         month++;
         dx += widthIncrement;
         if (month > 11) {
            month = 0;
            year++;
         }
      }// .while
   }


   /**
    * Will add an activity to the data set as result of user interaction in "drawing mode". <p/> The method may be
    * called on any component inside a chart-box.
    *
    * @param x      -
    *               X position of the mouse on Display where the activity should be "drawn"
    * @param y
    * @param length -
    *               length of activity. (if work_days==0, activity is a milestone)
    * @return The newly created activity component, an <code>OpProjectComponent</code> or null if no activity was
    *         created.
    */
   protected OpProjectComponent addGanttActivity(int x, int y, int length) {
      // Creates a new activity (optionally as a sub-activity of this activity)
      OpProjectComponent box = null;
      OpProjectComponent chart = null;

      // get the context (chart box) and the chart.
      box = (OpProjectComponent) getContext();
      chart = (OpProjectComponent) (box.getBoxContent());

      XComponent data_set = box.getDataSetComponent();
      OpGanttValidator validator = (OpGanttValidator) (data_set.validator());
      double day_width = box._dayWidth();
      double unitRatio = getUnitRatio(box.getTimeUnit());
      Date start;
      XComponent new_data_row;

      long day_offset;
      long day_end_offset;

      // standard activity
      if (length != 0) {
         day_offset = (long) (Math.round(((double) x) / day_width) * unitRatio);
         day_end_offset = (long) (Math.round(((double) (x + length)) / day_width) * unitRatio - 1);
         if (day_end_offset < day_offset) {
            box.repaint();
            return null;
         }
      }
      else {
         // milestone
         if (box.getTimeUnit() == XCalendar.WEEKS) {
            day_offset = (long) (Math.floor(((double) x) / day_width) * unitRatio);
            day_offset += XDisplay.getDefaultDisplay().getCalendar().getWorkHoursPerWeek() / XDisplay.getDefaultDisplay().getCalendar().getWorkHoursPerDay() - 1;
         }
         else if (box.getTimeUnit() == XCalendar.MONTHS) {
            day_offset = (long) (Math.floor(((double) x) / day_width + 1) * unitRatio);
         }
         else {
            //days view
            day_offset = (long) (Math.floor(((double) x) / day_width) * unitRatio);
         }
         day_end_offset = day_offset;
      }

      int index = y / getGridY();

      // index max = data_set.getChildCount()
      if (index > data_set.getChildCount()) {
         index = data_set.getChildCount();
      }
      if (index < 0) {
         index = 0;
      }

      validator.setContinuousAction(true);

      new_data_row = validator.newDataRow();
      start = new Date(day_offset * XCalendar.MILLIS_PER_DAY + chart.getStart().getTime());
      start = roundStart(box, start);
      validator.setDataCellValue(new_data_row, OpGanttValidator.START_COLUMN_INDEX, start);
      Date end = new Date(day_end_offset * XCalendar.MILLIS_PER_DAY + chart.getStart().getTime());
      if (end.before(OpGanttValidator.getStart(new_data_row))) {
         end = OpGanttValidator.getStart(new_data_row);
      }
      else {
         if (length != 0) {
            end = roundEnd(box, end);
         }
      }
      validator.updateFinish(new_data_row, end);
      if (length != 0) {
         validator.updateDuration(new_data_row, OpGanttValidator.getDuration(new_data_row));
      }
      else {
         validator.updateDuration(new_data_row, 0);
      }

      // add the newly created data row to the data set.
      // will add it with the same outline lvl as the activity whose position is taken
      XComponent replacedOutline = null;
      XComponent replaced = null;
      int outline;
      if (data_set.getChildCount() != 0) {
         if (index != data_set.getChildCount()) {
            replacedOutline = (XComponent) data_set.getChild(index);
            while (index != 0 && OpGanttValidator.isTaskType(replacedOutline)) {
               index--;
               replacedOutline = (XComponent) data_set.getChild(index);
            }
         }
         else {
            replacedOutline = (XComponent) data_set.getChild(index - 1);
            while (index != 1 && OpGanttValidator.isTaskType(replacedOutline)) {
               index--;
               replacedOutline = (XComponent) data_set.getChild(index - 1);
            }
         }
         outline = replacedOutline.getOutlineLevel();
         if (index != data_set.getChildCount()) {
            replaced = (XComponent) data_set.getChild(index);
         }
      }
      else {
         outline = 0;
      }

      if (replaced == null || OpGanttValidator.isTaskType(replaced)) {
         outline = 0;
         index = data_set.getChildCount() + 1;
      }
      new_data_row.setOutlineLevel(outline);
      data_set.addDataRow(index, new_data_row);

      chart.resetCached();
      box.doLayout();
      box.repaint();

      // Find proxy-component that related to newly created activity and focus it
      // *** TODO: This is a little bit of a hack (also performance-wise)?
      OpProjectComponent component = null;
      XComponent data_row = null;
      for (int i = 0; i < chart.getChildCount(); i++) {
         component = (OpProjectComponent) (chart._getChild(i));
         // *** TODO: Reuse XComponent.set/getDataComponent() instead
         data_row = component.getDataRow();
         if ((data_row != null) && (data_row == new_data_row)) {
            component.requestFocus();
            XComponent dataSet = (XComponent) data_row.getParent();
            dataSet.clearDataSelection();
            data_row.setSelected(true);
            break;
         }
      }

      validator.setContinuousAction(false);

      return component;
   }

   /**
    * Creates a new "open" dependency: Source is set to this, target is null Must be called on a component that suports
    * links.
    *
    * @return the newly created dependency
    */
   protected OpProjectComponent createDependency() {
      logger.debug("   DROP DEPENDENCY");
      OpProjectComponent box = (OpProjectComponent) getContext();
      OpProjectComponent chart = (OpProjectComponent) (box.getBoxContent());
      // *** TODO: This is a problem; Data-set/model supports no open
      // dependencies
      // ==> Partially solved via OPEN_DEPENDENCIES; Maybe focus problem remains
      OpProjectComponent dependency = new OpProjectComponent(GANTT_DEPENDENCY);
      dependency.setSource(this);
      chart._addChild(dependency);
      ArrayList open_dependencies = chart.getOpenDependencies();
      open_dependencies.add(dependency);
      box.doLayout();
      // *** TODO: General problem w/focus-management and cached components
      // ==> If focused component is reused for another component: Looses focus
      // dependency.requestFocus();
      box.repaint();
      return dependency;
   }

   /**
    * Links the current component to the given dependency. The component on which this method is called will be set as
    * the target for this link. The link must already have a source (e.g. may be created with createDependency) Closes
    * open dependency, or "re-closes" it (if it was already closed).
    *
    * @param dependency Dependency to be processed.
    * @return true if the link can be closed (e.g no loops caused by this link) and there isn't already a dependency
    *         with the same source & target/ false otherwise
    */
   protected boolean linkToDependency(OpProjectComponent dependency) {

      // Check if dependency already has a target
      OpProjectComponent box = (OpProjectComponent) getContext();
      OpProjectComponent chart = (OpProjectComponent) (box.getBoxContent());
      OpProjectComponent source = dependency.getSource();
      OpProjectComponent target = dependency.getTarget();

      XComponent sourceRow = source.getDataRow();
      XComponent targetRow = getDataRow();
      XComponent dataSet = box.getDataSetComponent();
      OpGanttValidator validator = (OpGanttValidator) (dataSet.validator());

      // avoid creating multiple dependencies between the same activities
      boolean newDependency = true;
      if (OpGanttValidator.getSuccessors(sourceRow).contains(new Integer(targetRow.getIndex()))) {
         newDependency = false;
      }
      else {
         Integer sourceIndex = new Integer(sourceRow.getIndex());
         List oldPred = OpGanttValidator.getPredecessors(targetRow);
         List pred = new ArrayList();
         pred.add(sourceIndex);
         if (oldPred != null) {
            pred.addAll(oldPred);
         }

         // "Open" dependency first, i.e., remove existing target
         // Remove existing target row from existing source row successors and vice-versa from predecessors
         if (target != null) {
            XComponent oldtargetRow = target.getDataRow();
            validator.removeLink(sourceRow.getIndex(), oldtargetRow.getIndex());
         }
         else {
            chart.getOpenDependencies().remove(dependency);
         }

         // set the new target for dependency
         dependency.setTarget(this);

         try {
            validator.setDataCellValue(targetRow, OpGanttValidator.PREDECESSORS_COLUMN_INDEX, pred);
         }
         catch (XValidationException e) {
            // rollback
            if (target != null) {
               XComponent oldtargetRow = target.getDataRow();
               OpGanttValidator.addSuccessor(sourceRow, oldtargetRow.getIndex());
               OpGanttValidator.addPredecessor(oldtargetRow, sourceRow.getIndex());
            }
            else {
               chart.getOpenDependencies().add(dependency);
            }
            dependency.setTarget(target);

            XComponent form = getForm();
            form.showValidationException(e);
            return false;
         }
      }

      // layout
      box.resetCalendar();
      box.doLayout();
      box.repaint();
      return newDependency;
   }

   protected void removeActivity(OpProjectComponent activity) {
      // Remove all references to this activity and the activity itself
      if (this.isChartComponent()) {

         OpGanttValidator validator = (OpGanttValidator) getContext().getDataSetComponent().validator();

         XComponent dataRow = activity.getDataRow();
         XComponent dataSet = (XComponent) dataRow.getParent();
         dataSet.completeSelection();
         ArrayList data_rows = dataSet.selectedRows();
         try {
            validator.removeDataRows(data_rows);
         }
         catch (XValidationException e) {
            XComponent form = getForm();
            form.showValidationException(e);
            return;
         }

         //detach the deleted activities from their data rows
         for (int i = 0; i < getChildCount(); i++) {
            activity = (OpProjectComponent) getChild(i);
            dataRow = activity.getDataRow();
            if (dataRow != null && dataRow.getParent() == null) {
               activity.setDataRow(null);
            }
         }

         // reset cache
         resetCached();
      }
   }

   protected void removeDependency(OpProjectComponent dependency) {
      // Remove dependency from all references
      if (pcType == GANTT_CHART) {
         OpProjectComponent source = dependency.getSource();
         OpProjectComponent target = dependency.getTarget();
         OpGanttValidator validator = (OpGanttValidator) getContext().getDataSetComponent().validator();
         validator.removeLink(source.getIndex(), target.getIndex());
         validator.validateDataSet();
      }
   }

   protected void _setExpandedActivity(boolean expanded) {
      // Make this activity visible or hidden including its dependencies
      if (pcType == GANTT_ACTIVITY) {
         getDataRow().expanded(expanded, false);
      }
   }

   protected void _scrollToPosition(int x, int y) {
      // *** Could also most probably be a general helper method (XComponent)
      // *** Check if point (x, y) is outside of currently visible area
      XComponent box = getContext();
      Point position = absolutePosition(x, y);
      Rectangle bounds = box.getViewPort().absoluteBounds();
      if ((position.x < bounds.x) || (position.x > bounds.x + bounds.width)) {
         // Scroll horizontally
         XComponent horizontal_scroll_bar = (XComponent) (box._getChild(HORIZONTAL_SCROLL_BAR_INDEX));
         if (position.x < bounds.x) {
            horizontal_scroll_bar.moveSlider(WEST, bounds.x - position.x);
         }
         else {
            horizontal_scroll_bar.moveSlider(EAST, position.x - bounds.x - bounds.width);
         }
      }
      if ((position.y < bounds.y) || (position.y > bounds.y + bounds.height)) {
         // Scroll vertically
         XComponent vertical_scroll_bar = (XComponent) (box._getChild(VERTICAL_SCROLL_BAR_INDEX));
         if (position.y < bounds.y) {
            vertical_scroll_bar.moveSlider(NORTH, bounds.y - position.y);
         }
         else {
            vertical_scroll_bar.moveSlider(SOUTH, position.y - bounds.y - bounds.height);
         }
      }
   }

   /**
    * Increases the size of the gantt chart when scroller at min//max position.
    *
    * @param minX
    * @param maxX
    * @param x
    */
   public void autoExpandWidthOnScroll(int minX, int maxX, int x) {

      if (!(x <= minX + 1 || x >= maxX - 1)) {
         return;
      }

      XComponent box = getContext();
      XComponent horizontal_scroll_bar = (XComponent) (box._getChild(HORIZONTAL_SCROLL_BAR_INDEX));
      if (horizontal_scroll_bar.getVisible()) {
         if (horizontal_scroll_bar.getIntValue() == horizontal_scroll_bar.getMaximum()) {
            int oldValue = horizontal_scroll_bar.getMaximum();
            horizontal_scroll_bar.sendScrollAtEndEvent(EAST);
            int newValue = horizontal_scroll_bar.getMaximum();
            horizontal_scroll_bar.moveSlider(WEST, newValue - oldValue);
         }
         else {
            if (horizontal_scroll_bar.getIntValue() == horizontal_scroll_bar.getMinimum()) {
               int oldValue = horizontal_scroll_bar.getMaximum();
               horizontal_scroll_bar.sendScrollAtEndEvent(WEST);
               int newValue = horizontal_scroll_bar.getMaximum();
               horizontal_scroll_bar.moveSlider(EAST, newValue - oldValue);
            }
         }
      }
   }


   /**
    * Will proces a pointer event over a GANTT CHART component. The event is described by the received arguments.
    *
    * @param event
    * @param action
    * @param x
    * @param y
    * @param modifiers
    * @see XView#processPointerEvent(HashMap,int,int,int,int)
    */
   protected void processGanttPointerEvent(HashMap event, int action, int x, int y, int modifiers) {
      logger.debug("OpProjectComponent.processGanttPointerEvent");
      // get the curent drawing tool
      String toolID = DEFAULT_CURSOR;
      if (getDrawingToolId() != null) {
         toolID = getDrawingToolId();
      }

      // what action was performed ?
      switch (action) {

         case POINTER_DOWN: {
            if (toolID.equals(MILESTONE_DRAW_ITEM)) {
               // for milestone direct drop
               addGanttActivity(x, y, 0);
            }
            else if (toolID.equals(ACTIVITY_DRAW_ITEM)) {
               // for task activity, set start point (and dragSource as the chart)
               getDisplay().setDragSource(this);
               setDragPosition(new Point(x, y));
            }
         }
         break;

         case POINTER_DRAG: {
            // draw ghost shape
            // rectagle for task
            if (toolID.equals(ACTIVITY_DRAW_ITEM)) {
               Rectangle ghostActivity;
               if (getDrawingRectangle() == null) {
                  // for task activity, start drawing ghost shape
                  ghostActivity = new Rectangle(x, y, 1, 10);
               }
               else {
                  ghostActivity = getDrawingRectangle();
               }

               // see where the cursor is relative to the start position
               if (x - getDragPosition().x < 0) {
                  ghostActivity.width = getDragPosition().x - x;
                  ghostActivity.x = x;
               }
               else {
                  ghostActivity.width = x - getDragPosition().x;
                  ghostActivity.x = getDragPosition().x;
               }
               setDrawingRectangle(ghostActivity);
               repaint();
            }
            // line for link
            else if (toolID.equals(DEPENDENCY_DRAW_ITEM)) {
               if (getDrawingLine() != null) {
                  Line2D ghostLine = getDrawingLine();
                  ghostLine.setLine(getDragPosition().x, getDragPosition().y, x, y);
                  setDrawingLine(ghostLine);
                  repaint();
               }
            }
            _scrollToPosition(x, y);
            autoExpandWidthOnScroll(0, getBounds().width, x);
         }
         break;

         case POINTER_DRAG_END:
            // *** Find drop-position (sequence-number)
            // ==> Add new activity at sequence-number position
            // *** y / chart.getGridY() is position within sub-project
            XView drag_source = getDisplay().getDragSource();
            if ((drag_source != null) && (toolID.equals(ACTIVITY_DRAW_ITEM))) {
               if (getDrawingRectangle() != null) {
                  Rectangle ghostActivity = getDrawingRectangle();
                  setDrawingRectangle(null);
                  int minLen = 1;
                  if (ghostActivity.width < minLen) {
                     addGanttActivity(ghostActivity.x, y, minLen);
                  }
                  else {
                     addGanttActivity(ghostActivity.x, y, ghostActivity.width);
                  }
               }
            }
            if ((drag_source != null) && (toolID.equals(DEPENDENCY_DRAW_ITEM))) {
               setDrawingLine(null);
               repaint();
            }
            break;
      }
   }

   public void processGenericPointerEvent(HashMap event, int action, int x, int y, int modifiers) {
      // pointer DOWN or POINTER_DOUBLE_TAP is still processed even if the box is not enabled
      logger.debug("OpProjectComponent.processGenericPointerEvent");
      OpProjectComponent context = (OpProjectComponent) getContext();
      if ((context != null) && (!context.getEditMode())
           && isActivityBox(context)) {
         if (!((action == POINTER_DOWN) ||
              (action == POINTER_DOUBLE_TAP) ||
              (action == POINTER_ENTER) ||
              (action == POINTER_LEAVE) ||
              (action == POINTER_TAP))) {
            return;
         }
      }
      // hide ErrorLabel if it's visible for box
      if ((action == POINTER_DOWN) && (context != null) && isActivityBox(context)) {
         XComponent form = context.getForm();
         form.hideValidationErrorMessage();
      }

      super.processGenericPointerEvent(event, action, x, y, modifiers);
   }

   protected boolean isActivityBox(OpProjectComponent component) {
      return (component.getComponentType() == OpProjectComponent.GANTT_BOX);
   }


   public void processPointerEvent(HashMap event, int action, int x, int y, int modifiers) {
      logger.debug("OpProjectComponent.processPointerEvent");
      switch (pcType) {
         // *** Not easily possible now: We cannot determine, if someone clicks
         // "beside" a shape
         // ==> Not really true: Use the following code, but call
         // super.processPointerEvent() before break
         case GANTT_CHART:
            // for disabled gantt boxes (e.g my tasks) do nothing
            if (!getContext().getEditMode()) {
               boolean passEvent = false;
               XView component = find(x, y);
               if (component instanceof OpProjectComponent) {
                  OpProjectComponent activity = ((OpProjectComponent) component);
                  if (activity.pcType == GANTT_ACTIVITY
                       && ((action == POINTER_DOWN) || (action == POINTER_DOUBLE_TAP))
                       || action == POINTER_TAP) {
                     passEvent = true;
                  }
               }
               if (!passEvent) {
                  break;
               }
            }

            if (action == POINTER_DOWN) {
               clearSelection();
            }

            super.processPointerEvent(event, action, x, y, modifiers);

            // processEvent() could return true if the event was consumed
            processGanttPointerEvent(event, action, x, y, modifiers);
            break;

         case GANTT_ACTIVITY: {
            String toolID = DEFAULT_CURSOR;
            OpProjectComponent parent = (OpProjectComponent) getParent();
            if (parent.getDrawingToolId() != null) {
               toolID = parent.getDrawingToolId();
            }

            if (!toolID.equals(DEFAULT_CURSOR)) {
               if (toolID.equals(DEPENDENCY_DRAW_ITEM) && action == POINTER_DOWN) {
                  Line2D.Double trace = new Line2D.Double(x, y, x, y);
                  getDisplay().setDragSource((XComponent) getParent());
                  parent.setDragPosition(new Point(getBounds().x + x, getBounds().y + y));
                  parent.setDrawingLine(trace);
                  parent.setDraggedComponent(this);
               }
               if (toolID.equals(DEPENDENCY_DRAW_ITEM) && (action == POINTER_DRAG_END)) {
                  OpProjectComponent source = parent.getDraggedComponent();
                  if (source != null && source != this) {
                     OpProjectComponent box = (OpProjectComponent) getContext();
                     OpProjectComponent chart = (OpProjectComponent) (box.getBoxContent());
                     parent.setDrawingLine(null);
                     OpProjectComponent dep = source.createDependency();
                     boolean result = linkToDependency(dep);
                     if (!result) {
                        chart.getOpenDependencies().remove(dep);
                        chart.removeChild(dep);
                        box.resetCalendar();
                        box.doLayout();
                        box.repaint();
                     }
                  }
               }
            }
            else {

               switch (action) {
                  // for all types of gantt activity show details on double click
                  case POINTER_DOUBLE_TAP:
                     OpProjectComponent box = (OpProjectComponent) getContext();
                     String handler = box.getOnActivityDetails();
                     if (handler != null) {
                        // select data row
                        getDataRow().setSelected(true);
                        // activity loses focus
                        setFocused(false);
                        repaint();
                        // invoke handler
                        box.getForm().invokeFunction(handler, null);
                     }
                     break;
               }

               switch (getActivityType()) {

                  case COLLECTION_ACTIVITY: {
                     switch (action) {
                        case POINTER_DOWN:
                           requestFocus();
                           getDisplay().setDragSource(this);
                           // select the data row
                           XComponent dataRow = this.getDataRow();
                           XComponent dataSet = (XComponent) dataRow.getParent();
                           dataRow.setSelected(true);
                           boolean changeExpandedMode = (modifiers & CTRL_KEY_DOWN) == CTRL_KEY_DOWN;
                           // expand or collapse collection activity on CTRL+POINTER_DOWN
                           if (changeExpandedMode) {
                              XComponent data_row = getDataRow();
                              boolean expanded = !(data_row.getExpanded());
                              data_row.expanded(expanded, false);
                              OpProjectComponent box = (OpProjectComponent) getContext();
                              OpProjectComponent chart = (OpProjectComponent) box.getBoxContent();
                              chart.resetCached();
                              box.doLayout();
                              box.repaint();
                           }
                           break;
                        case POINTER_UP:
                           getDisplay().setDragSource(null);
                           break;
                        case POINTER_DRAG:
                           Point drag_position = getDragPosition();
                           if (drag_position != null) {
                              Rectangle moved_bounds = getBounds();
                              moved_bounds.x += x - drag_position.x;
                              moved_bounds.y += y - drag_position.y;
                              setBounds(moved_bounds);
                              x -= x - drag_position.x;
                              y -= y - drag_position.y;
                              // *** Optimization potential: Use bitblit to move
                              // shape and repaint only obscured area
                              getParent().repaint();
                           }
                           setDragPosition(new Point(x, y));
                           _scrollToPosition(x, y);
                           autoExpandWidthOnScroll(0, getContext().getBounds().width, getBounds().x + x);
                           break;
                        case POINTER_DRAG_END:
                           OpProjectComponent box = (OpProjectComponent) getContext();
                           //move the activity
                           try {
                              moveActivityOnGantt();
                           }
                           catch (XValidationException e) {
                              box.requestFocus();
                              XComponent form = box.getForm();
                              form.showValidationException(e);
                           }

                           getDisplay().setDragSource(null);
                           setDragPosition(null);

                           box.doLayout();
                           box.repaint();
                           break;
                        case POINTER_DROP:
                           logger.debug(" Drop on Collection ");
                           dropOnActivity((OpProjectComponent) getDisplay().getDragSource());
                           break;
                        case POINTER_TAP: {
                           logger.debug("Selected activity");
                           box = (OpProjectComponent) getContext();
                           box.sendActivitySelectEvent(this.getDataRow());
                           break;
                        }
                     }
                  }
                  break;

                  case MILESTONE_ACTIVITY: {
                     switch (action) {
                        case POINTER_DOWN:
                           requestFocus();
                           getDisplay().setDragSource(this);
                           // select the data row
                           XComponent dataRow = this.getDataRow();
                           XComponent dataSet = (XComponent) dataRow.getParent();
                           dataRow.setSelected(true);
                           break;
                        case POINTER_UP:
                           getDisplay().setDragSource(null);
                           break;
                        case POINTER_DRAG:
                           // *** Most probably: Helper methods for moving and
                           // resizes w/pointer-drag
                           Point drag_position = getDragPosition();
                           if (drag_position != null) {
                              Rectangle moved_bounds = getBounds();
                              moved_bounds.x += x - drag_position.x;
                              moved_bounds.y += y - drag_position.y;
                              setBounds(moved_bounds);
                              x -= x - drag_position.x;
                              y -= y - drag_position.y;
                              // *** Optimization potential: Use bitblit to move
                              // shape and repaint only obscured area
                              getParent().repaint();
                           }
                           setDragPosition(new Point(x, y));
                           _scrollToPosition(x, y);
                           autoExpandWidthOnScroll(0, getContext().getBounds().width, getBounds().x + x);
                           break;
                        case POINTER_DRAG_END:
                           if (getDragPosition() != null) {
                              // Clear drag position and align bounds to grid lines
                              setDragPosition(null);
                              Rectangle bounds = getBounds();
                              OpProjectComponent box = (OpProjectComponent) getContext();
                              OpProjectComponent chart = (OpProjectComponent) (box.getBoxContent());
                              double day_width = box._dayWidth();
                              long day_offset = (long) (bounds.x / day_width);

                              if (box.getTimeUnit() == XCalendar.WEEKS) {
                                 //milestones on last working day of week
                                 day_offset = (long) (day_offset * getUnitRatio(box.getTimeUnit()));
                                 day_offset += XDisplay.getDefaultDisplay().getCalendar().getWorkHoursPerWeek() / XDisplay.getDefaultDisplay().getCalendar().getWorkHoursPerDay() - 1;
                              }
                              else if (box.getTimeUnit() == XCalendar.MONTHS) {
                                 day_offset = (long) ((day_offset + 1) * getUnitRatio(box.getTimeUnit()));
                              }
                              else {
                                 //days view
                                 day_offset = (long) (day_offset * getUnitRatio(box.getTimeUnit()));
                              }

                              Date start = new Date(day_offset * XCalendar.MILLIS_PER_DAY + chart.getStart().getTime());
                              start = roundStart(box, start);
                              OpGanttValidator validator = (OpGanttValidator) (box.getDataSetComponent().validator());

                              try {
                                 //move the activity
                                 moveActivityOnGantt();
                                 validator.setDataCellValue(getDataRow(), OpGanttValidator.START_COLUMN_INDEX, start);
                              }
                              catch (XValidationException e) {
                                 box.requestFocus();
                                 XComponent form = box.getForm();
                                 form.showValidationException(e);
                              }

                              getDisplay().setDragSource(null);

                              box.resetCalendar();
                              box.doLayout();
                              box.repaint();
                           }
                           break;
                        case POINTER_DROP:
                           dropOnActivity((OpProjectComponent) getDisplay().getDragSource());
                           break;
                        case POINTER_TAP: {
                           logger.debug("Selected activity");
                           OpProjectComponent box = (OpProjectComponent) getContext();
                           box.sendActivitySelectEvent(this.getDataRow());
                           break;
                        }
                     }
                  }
                  break;

                  case SCHEDULED_TASK_ACTIVITY:
                  case STANDARD_ACTIVITY: {
                     switch (action) {
                        case POINTER_DOWN:
                           requestFocus();
                           getDisplay().setDragSource(this);
                           // select the data row
                           XComponent dataRow = this.getDataRow();
                           XComponent dataSet = (XComponent) dataRow.getParent();
                           dataRow.setSelected(true);
                           break;
                        case POINTER_UP:
                           getDisplay().setDragSource(null);
                           break;
                        case POINTER_DRAG:
                           // *** Most probably: Helper methods for moving and
                           // resizing w/pointer-drag
                           Point drag_position = getDragPosition();
                           if (drag_position != null) {
                              if (getDragMode() == DRAG_MOVE) {
                                 Rectangle moved_bounds = getBounds();
                                 moved_bounds.x += x - drag_position.x;
                                 moved_bounds.y += y - drag_position.y;
                                 setBounds(moved_bounds);
                                 getParent().repaint();
                                 x -= x - drag_position.x;
                                 y -= y - drag_position.y;
                              }
                              else {
                                 // Resize shape in drag-direction
                                 Rectangle resized_bounds = getBounds();
                                 resized_bounds.width += x - drag_position.x;
                                 setBounds(resized_bounds);
                                 createComponentShape();
                                 getParent().repaint();
                              }
                           }
                           else {
                              // Decide on move or resize action
                              if (x >= getBounds().width - 6) {
                                 setDragMode(DRAG_RESIZE);
                              }
                              else {
                                 setDragMode(DRAG_MOVE);
                              }
                           }
                           setDragPosition(new Point(x, y));
                           _scrollToPosition(x, y);
                           autoExpandWidthOnScroll(0, getContext().getBounds().width, getBounds().x + x);
                           break;
                        case POINTER_DRAG_END:
                           if (getDragPosition() != null) {
                              // We have to differentiate between move and resize
                              // Clear drag position and align bounds to grid lines
                              setDragPosition(null);
                              Rectangle bounds = getBounds();
                              OpProjectComponent box = (OpProjectComponent) getContext();
                              OpProjectComponent chart = (OpProjectComponent) (box.getBoxContent());
                              double day_width = box._dayWidth();
                              double ratio = getUnitRatio(box.getTimeUnit());
                              long start_day_units = (int) ((bounds.x + (day_width / 2)) / day_width);
                              Date start = new Date((long) (start_day_units * ratio * XCalendar.MILLIS_PER_DAY
                                   + chart.getStart().getTime()));
                              start = roundStart(box, start);

                              long end_day_units = (int) ((bounds.x + bounds.width + (day_width / 2)) / day_width);
                              if (box.getTimeUnit() != XCalendar.DAYS) {
                                 if (end_day_units == start_day_units) {
                                    end_day_units++;
                                 }
                              }

                              Date end = new Date((long) (end_day_units * ratio * XCalendar.MILLIS_PER_DAY
                                   + chart.getStart().getTime()));
                              end = new Date(end.getTime() - XCalendar.MILLIS_PER_DAY);
                              end = roundEnd(box, end);
                              OpGanttValidator validator = (OpGanttValidator) (box.getDataSetComponent().validator());
                              logger.debug("*** STANDARD DE");

                              if (getDragMode() == DRAG_MOVE) {
                                 try {
                                    //move the activity (up/down)
                                    moveActivityOnGantt();
                                    validator.setDataCellValue(getDataRow(), OpGanttValidator.START_COLUMN_INDEX, start);
                                 }
                                 catch (XValidationException e) {
                                    box.requestFocus();
                                    XComponent form = box.getForm();
                                    form.showValidationException(e);
                                 }
                              }
                              else {
                                 try {

                                    validator.setDataCellValue(getDataRow(), OpGanttValidator.END_COLUMN_INDEX, end);
                                 }
                                 catch (XValidationException e) {
                                    box.requestFocus();
                                    XComponent form = box.getForm();
                                    form.showValidationException(e);
                                 }
                              }

                              // *** TODO: Have to check return value of setDCValue and repaint only if true returned
                              getDisplay().setDragSource(null);
                              box.resetCalendar();
                              box.doLayout();
                              box.repaint();
                           }
                           break;
                        case POINTER_DROP:
                           logger.debug("DROP on standard activity");
                           dropOnActivity((OpProjectComponent) getDisplay().getDragSource());
                           break;
                        case POINTER_TAP: {
                           logger.debug("Selected activity");
                           OpProjectComponent box = (OpProjectComponent) getContext();
                           box.sendActivitySelectEvent(this.getDataRow());
                           break;
                        }
                     }
                     break;
                  }
               }
            }
         }
         break;

         case GANTT_DEPENDENCY: {
            String toolID = DEFAULT_CURSOR;
            OpProjectComponent parent = (OpProjectComponent) getParent();
            if (parent.getDrawingToolId() != null) {
               toolID = parent.getDrawingToolId();
            }
            if (toolID.equals(DEFAULT_CURSOR)) {
               switch (action) {
                  case POINTER_DOWN:
                     // Enable drag & drop, request focus
                     // *** target-point: RESIZE else MOVE
                     logger.debug("POINTER-DOWN-DEPENDENCY");
                     getDisplay().setDragSource(this, true);
                     setDragMode(DRAG_RESIZE);
                     this.getContext().getDataSetComponent().clearDataSelection();
                     requestFocus();
                     repaint();
                     break;
                  case POINTER_DRAG:
                     // *** Most probably: Helper methods for moving and resizes
                     // w/pointer-drag
                     Point drag_position = getDragPosition();
                     int dragX = x;
                     int dragY = y;
                     if (drag_position != null) {
                        if (getDragMode() == DRAG_RESIZE) {
                           // Resize-mode means move only target-point
                           Point[] path = getPath();
                           Point target_point = path[path.length - 1];
                           Rectangle bounds = getBounds();

                           dragX = Math.min(target_point.x, Math.abs(x));
                           dragY = Math.min(target_point.y, Math.abs(y));

                           target_point.x += bounds.x + x - drag_position.x;
                           target_point.y += bounds.y + y - drag_position.y;
                           _dynamicGanttDependency(path[0].x + bounds.x, path[0].y + bounds.y, target_point.x,
                                target_point.y);
                           getParent().repaint();
                        }
                     }
                     setDragPosition(new Point(dragX, dragY));
                     _scrollToPosition(x, y);
                     break;
                  case POINTER_DRAG_END:
                     setDragPosition(null);
                     break;
               }
            }
         }
         if (action == POINTER_DRAG_END) {
            ((OpProjectComponent) getParent()).setDrawingLine(null);
            repaint();
         }
         break;
         case UTILIZATION_ROW:
            if (action == POINTER_MOVE) {
               refreshUtilizationRowDetails(x, y);
               break;
            }
            if (action == POINTER_LEAVE) {
               //leavs the component and clears the details interval
               ((OpProjectComponent) getContext()).setUtilizationVisibleDetailsInterval(new ArrayList());
               break;
            }
         case GANTT_BOX: {
            if (action == POINTER_DOWN) {
               requestFocus();
            }
            super.processPointerEvent(event, action, x, y, modifiers);
            break;
         }
         case UTILIZATION_BOX: {
            if (action == POINTER_DOWN) {
               requestFocus();
            }
            super.processPointerEvent(event, action, x, y, modifiers);
            break;
         }
         default:
            super.processPointerEvent(event, action, x, y, modifiers);
      }
   }


   /**
    * Moves an activity on a gantt chart. If the activity is droped on another activity tries to make the dropped
    * activity a child of the previous activity.
    */
   private void moveActivityOnGantt() {

      XComponent dataSet;
      Rectangle bounds = getBounds();
      XComponent sourceDataRow = getDataRow();
      dataSet = (XComponent) sourceDataRow.getParent();
      OpGanttValidator validator = (OpGanttValidator) dataSet.validator();
      OpProjectComponent chart = (OpProjectComponent) getParent();
      int oldIndexInDataSet = sourceDataRow.getIndex();
      int newPositionInGantt = bounds.y / chart.getGridY();
      Integer intValue = (Integer) chart.getGanttIndexes().get(new Integer(newPositionInGantt));
      int newIndexInDataSet;
      int offset;

      if (intValue != null) {
         newIndexInDataSet = intValue.intValue();

      }
      else {
         if (newPositionInGantt < 0) {
            newIndexInDataSet = -1;
         }
         else {
            newIndexInDataSet = dataSet.getChildCount();
         }
      }

      offset = newIndexInDataSet - oldIndexInDataSet;
      if (offset != 0) {
         ArrayList rows = new ArrayList();
         rows.add(sourceDataRow);
         List children = validator.getChildren(sourceDataRow);
         if (children.size() > 0) {
            rows.addAll(children);
            if (newIndexInDataSet > oldIndexInDataSet) {
               //new position "bellow" the old position
               offset -= children.size();
            }
         }

         OpProjectComponent target = null;
         XComponent targetDataRow = null;
         int targetOutlineLevel = 0;

         if (newIndexInDataSet < dataSet.getChildCount() && newIndexInDataSet >= 0) {
            targetDataRow = (XComponent) dataSet.getChild(newIndexInDataSet);
            targetOutlineLevel = targetDataRow.getOutlineLevel();
            //find the component that has this data
            for (int i = 0; i < chart.getChildCount(); i++) {
               OpProjectComponent component = (OpProjectComponent) chart.getChild(i);
               if (component.getDataRow() == targetDataRow) {
                  target = component;
                  break;
               }
            }
            //if target is in the moved array - error
            if (rows.contains(targetDataRow)) {
               throw new XValidationException(OpGanttValidator.LOOP_EXCEPTION);
            }
         }
         //offset
         if (offset < 0) {
            offset++;
         }

         validator.setContinuousAction(true);
         //this activity was dropped over another, target, activity. (this activity becomes a sub-activity of target)
         if (target != null && !this.intersects(target).isEmpty()) {
            validator.moveInCollection(rows, offset, targetDataRow, targetOutlineLevel);
         }
         else {
            if (targetDataRow != null) {
               if (OpGanttValidator.getType(targetDataRow) == OpGanttValidator.SCHEDULED_TASK) {
                  List taskChildren = validator.getChildren(targetDataRow);
                  //drop activity bellow the scheduled task and all its task children
                  offset += taskChildren.size();
               }
            }
            validator.moveOverActivities(rows, offset, targetOutlineLevel);
         }
         validator.setContinuousAction(false);
      }
   }

   /**
    * Handles the action of dropping a component on an activity.
    *
    * @param activity component being dropped on the current activity
    */
   private void dropOnActivity(OpProjectComponent activity) {
      if (activity != null) {
         // Connect open activity with target activity
         if (activity.pcType == GANTT_DEPENDENCY) {
            this.linkToDependency(activity);
         }
      }
   }


   /**
    * Returns the visual associated with the data row from a given "holder" component. The visuals are the children of
    * the holder component
    * Must be called on the holder.
    *
    * @param dataRow data associated with the visual that is beign searched for
    * @return visual that has this data (if more than one, the first one will be returned) or null if no such visual
    *         was found
    */
   protected OpProjectComponent getVisualForData(XComponent dataRow) {
      for (int i = 0; i < this.getChildCount(); i++) {
         OpProjectComponent component = (OpProjectComponent) this.getChild(i);
         if (component.getDataRow() == dataRow) {
            return component;
         }
      }
      return null;
   }


   public void processGenericKeyboardEvent(HashMap event, int action, int key_code, char key_char, int modifiers) {
      // suppress keyboad events if box components are disabled
      logger.debug("OpProjectComponent.processGenericKeyboardEvent");
      OpProjectComponent context = (OpProjectComponent) getContext();
      if ((context != null)
           && (!context.getEditMode())
           && isActivityBox(context)) {
         return;
      }
      super.processGenericKeyboardEvent(event, action, key_code, key_char, modifiers);
   }


   public void processKeyboardEvent(HashMap event, int action, int key_code, char key_char, int modifiers) {
      logger.debug("OpProjectComponent.processKeyboardEvent");
      switch (pcType) {
         case GANTT_BOX:
            processUndoRedoKeyboardEvent(action, key_code, modifiers);
            if (action == KEY_DOWN) {
               switch (key_code) {

                  case DELETE_KEY: {
                     if (pcType == GANTT_BOX) {
                        // Remove the selected activity from chart
                        OpProjectComponent chart = (OpProjectComponent) (getBoxContent());
                        for (int i = 0; i < chart.getChildCount(); i++) {
                           OpProjectComponent activity = (OpProjectComponent) chart.getChild(i);
                           XComponent dataRow = activity.getDataRow();
                           if (dataRow != null && dataRow.getSelected() && getEditMode()) {
                              chart.removeActivity(activity);
                           }
                        }
                        getDataSetComponent().clearDataSelection();
                        doLayout();
                        repaint();
                     }
                     break;
                  }
               }
            }
            break;

         case GANTT_ACTIVITY:
            OpProjectComponent box = (OpProjectComponent) getContext();
            if (action == KEY_DOWN) {
               Date activityStartDate = null;
               switch (key_code) {

                  case CURSOR_LEFT_KEY:
                     // Move activity one day backward in time
                     box = (OpProjectComponent) getContext();
                     activityStartDate = OpGanttValidator.getStart(this.getDataRow());
                     XCalendar calendar = this.getDisplay().getCalendar();
                     Date newStart = calendar.previousWorkDay(activityStartDate);
                     box.getDataSetComponent().validator().setDataCellValue(getDataRow(),
                          OpGanttValidator.START_COLUMN_INDEX, newStart);
                     box.doLayout();
                     box.repaint();
                     break;
                  case CURSOR_RIGHT_KEY:
                     // Move activity one day forward in time
                     box = (OpProjectComponent) getContext();
                     activityStartDate = OpGanttValidator.getStart(this.getDataRow());
                     calendar = this.getDisplay().getCalendar();
                     newStart = calendar.nextWorkDay(activityStartDate);
                     box.getDataSetComponent().validator().setDataCellValue(getDataRow(),
                          OpGanttValidator.START_COLUMN_INDEX, newStart);
                     box.doLayout();
                     box.repaint();
                     break;
                     // *** CURSOR_UP/DOWN: Move activity up/down?
                  case BACK_SPACE_KEY:
                  case DELETE_KEY: {
                     if (pcType == GANTT_ACTIVITY) {
                        // Remove activity from chart
                        box = (OpProjectComponent) getContext();
                        OpProjectComponent chart = (OpProjectComponent) (box.getBoxContent());
                        chart.removeActivity(this);
                        box.getDataSetComponent().clearDataSelection();
                        box.doLayout();
                        box.repaint();
                     }
                     break;
                  }
                  case TAB_KEY:
                     if ((modifiers & SHIFT_KEY_DOWN) == SHIFT_KEY_DOWN) {
                        if (transferFocusSiblingBackward()) {
                           repaint();
                           getDisplay().getDisplayFocusedView().repaint();
                        }
                     }
                     else {
                        if (transferFocusSiblingForward()) {
                           repaint();
                           getDisplay().getDisplayFocusedView().repaint();
                        }
                     }
                     break;
                  case C_KEY:
                     if (XDisplay.areModifiersDown(modifiers, true, false, false)) {
                        String scriptAction = box.getOnCopy();
                        this.getForm().invokeFunction(scriptAction, null);
                     }
                     break;
                  case X_KEY:
                     if (XDisplay.areModifiersDown(modifiers, true, false, false) && box.getEditMode()) {
                        String scriptAction = box.getOnCut();
                        this.getForm().invokeFunction(scriptAction, null);
                     }
                     break;
                  case V_KEY:
                     if (XDisplay.areModifiersDown(modifiers, true, false, false) && box.getEditMode()) {
                        String scriptAction = box.getOnPaste();
                        this.getForm().invokeFunction(scriptAction, null);
                     }
                     break;
                  default:
                     // Number-keys with CTRL-pressed set percentage-complete
                     if ((modifiers & CTRL_KEY_DOWN) == CTRL_KEY_DOWN) {
                        if ((key_code >= (int) '1') && (key_code <= (int) '9')) {
                           setIntValue((key_code - (int) '0') * 10);
                           repaint();
                        }
                        else if (key_code == (int) '0') {
                           setIntValue(100);
                           repaint();
                        }
                     }
               }
            }
            else if (action == KEY_TYPED) {
               if ((modifiers & CTRL_KEY_DOWN) == CTRL_KEY_DOWN) {
                  // Set value *** should be "completed"
                  // *** Smaller code possible: Check unicode for 0-9
                  if (key_char == ' ') {
                     setIntValue(0);
                     repaint();
                  }
                  // *** OR use CTRL+DELETE_KEY?
               }
               else if ((key_char != (char) 127) && (key_char != (char) 9) && (int) key_char != ENTER_KEY) {
                  // Open caption-editor
                  // *** Should be: getName()
                  _openCaptionEditor(OpGanttValidator.getName(getDataRow()));
                  // get the caption editor
                  XComponent captionEditor = _getCaptionEditor();
                  // process the caption editor keyboard event
                  captionEditor.processKeyboardEvent(event, action, key_code, key_char, modifiers);
               }
            }
            break;
         case GANTT_DEPENDENCY:
            if (action == KEY_DOWN) {
               switch (key_code) {
                  case DELETE_KEY: {
                     removeChartDependency();
                     break;
                  }
                  case BACK_SPACE_KEY: {
                     removeChartDependency();
                     break;
                  }
                  case TAB_KEY:
                     if ((modifiers & SHIFT_KEY_DOWN) == SHIFT_KEY_DOWN) {
                        if (transferFocusSiblingBackward()) {
                           repaint();
                           getDisplay().getDisplayFocusedView().repaint();
                        }
                     }
                     else {
                        if (transferFocusSiblingForward()) {
                           repaint();
                           getDisplay().getDisplayFocusedView().repaint();
                        }
                     }
                     break;
               }
            }
            break;
         case CAPTION_EDITOR:
            if (action == KEY_UP) {
               if (key_code == ENTER_KEY) {
                  sendValueChangedEvent(modifiers);
               }
               else {
                  _getChild(0).processKeyboardEvent(event, action, key_code, key_char, modifiers);
               }
            }
            else {
               if (key_code == ESCAPE_KEY) {
                  _closeCaptionEditor();
               }
               else {
                  _getChild(0).processKeyboardEvent(event, action, key_code, key_char, modifiers);
               }
            }
            break;
      }
   }

   protected void processUndoRedoKeyboardEvent(int action, int key_code, int modifiers) {
      if (action == KEY_DOWN) {
         XComponent dataSetComponent = this.getDataSetComponent();
         if (dataSetComponent != null && this.getEditMode()) {
            switch (key_code) {
               case Z_KEY:
                  if (XDisplay.areModifiersDown(modifiers, true, false, false)) {
                     dataSetComponent.dataSetUndo();
                     this.resetCalendar();
                     this.doLayout();
                     this.update();
                  }
                  break;
               case Y_KEY:
                  if (XDisplay.areModifiersDown(modifiers, true, false, false)) {
                     dataSetComponent.dataSetRedo();
                     this.resetCalendar();
                     this.doLayout();
                     this.update();
                  }
                  break;
            }
         }
      }
   }

   /**
    * Removes a gannt dependency from the chart.
    */
   private void removeChartDependency() {
      if (this.pcType == GANTT_DEPENDENCY) {
         // Remove activity from chart
         OpProjectComponent box = (OpProjectComponent) getContext();
         OpProjectComponent chart = (OpProjectComponent) box.getBoxContent();
         chart.removeDependency(this);
         box.doLayout();
         box.repaint();
      }
   }

   public void processFocusEvent(HashMap event, int action) {
      logger.debug("OpProjectComponent.processFocusEvent");
      switch (pcType) {
         case GANTT_ACTIVITY:
            if (action == FOCUS_GAINED) {
               scrollToComponent();
            }
            break;

         case GANTT_DEPENDENCY:
            if (action == FOCUS_GAINED) {
               scrollToComponent();
            }
            break;

         case CAPTION_EDITOR:
            if (action == FOCUS_GAINED) { // Select all text in glyph and show
               // cursor
               XComponent line_editor = (XComponent) (_getChild(0)._getChild(0));
               String text = line_editor.getStringValue();
               line_editor.setCursorOffset(text.length());
               line_editor.updateCursor();
            }
            else {
               // Clear selection in glyph and hide cursor
               XComponent line_editor = (XComponent) (_getChild(0)._getChild(0));
               line_editor.setSelectionStart(-1);
               line_editor.setSelectionEnd(-1);
               line_editor.hideCursor();
               repaint();
               // close the editor (captionEditor should never be null here - paranoia)
               if (captionEditor != null && captionEditorOwner != null) {
                  // close the layer
                  _closeCaptionEditor();
               }
            }
            break;
      }
   }

   /**
    * Method updates the <code>calendarField</code> of the <code> GANTT_CHART </code> component with
    * <code>calendarField</code>+<code>value</code>
    *
    * @param direction     direction of scrolling
    * @param calendarField the calendarField
    * @param value         the value to modify the calendarField
    */
   public void updateGantChartDate(int direction, int calendarField, int value) {
      // update the start Date and the End Date according to the scrolling direction
      OpProjectComponent ganttChart = (OpProjectComponent) getBoxContent();
      // get the date from the ganttChart according to the direction
      Calendar dateCalendar = Calendar.getInstance();
      // inject dateCalendar depending on scrolling direction
      if (direction == WEST) {
         dateCalendar.setTimeInMillis(ganttChart.getStart().getTime());
      }
      if (direction == EAST) {
         dateCalendar.setTimeInMillis(ganttChart.getEnd().getTime());
      }

      // update calednarField according to the value
      dateCalendar.set(calendarField, dateCalendar.get(calendarField) + value);
      // update the gannt chart start/end time according to scrolling direction
      if (direction == WEST) {
         ganttChart.setStart(new Date(dateCalendar.getTimeInMillis()));
      }
      if (direction == EAST) {
         ganttChart.setEnd(new Date(dateCalendar.getTimeInMillis()));
      }
   }

   /**
    * This method updates the start / end date from the header according with the direction of scrolling.
    *
    * @param direction direction of scrolling <code>WEST</code> or <code>EAST</code>
    */
   private void updateGanttChartDate(int direction) {
      // view port
      XComponent viewPort = getViewPort();
      // gantt chart
      OpProjectComponent ganttChart = (OpProjectComponent) viewPort._getChild(0);
      // update the start Date and the End Date according to the scrolling direction
      switch (getTimeUnit()) {
         case XCalendar.DAYS:
            // if the direction is west decrement the start date
            if (direction == WEST) {
               updateGantChartDate(direction, Calendar.DAY_OF_MONTH, -1);
            }
            // if the direction is east increment the end date
            if (direction == EAST) {
               updateGantChartDate(direction, Calendar.DAY_OF_MONTH, +1);

            }
            break;
         case XCalendar.WEEKS:
            // if the direction is west decrement the start date
            if (direction == WEST) {
               updateGantChartDate(direction, Calendar.WEEK_OF_MONTH, -1);
            }
            if (direction == EAST) {
               updateGantChartDate(direction, Calendar.WEEK_OF_MONTH, +1);
            }
            break;
         case XCalendar.MONTHS:
            // if the direction is west decrement the start date
            if (direction == WEST) {
               updateGantChartDate(direction, Calendar.MONTH, -1);
            }
            // if the direction is east increment the end date
            if (direction == EAST) {
               updateGantChartDate(direction, Calendar.MONTH, +1);
            }
            break;

      }
      // force the layout
      doLayout();
      // scroll sliders to maximum if direction is EAST
      if (direction == EAST) {
         XComponent horizontal_scroll_bar = (XComponent) (_getChild(HORIZONTAL_SCROLL_BAR_INDEX));
         if (horizontal_scroll_bar.getVisible()) {
            int scrollValue = horizontal_scroll_bar.getIntValue();
            int scrollMax = horizontal_scroll_bar.getMaximum();
            if (scrollValue < scrollMax) {
               horizontal_scroll_bar.moveSlider(EAST, scrollMax - scrollValue);
            }
         }
      }
      repaint();
      // force repaint on ganttchart
      ganttChart.repaint();

   }

   public void processComponentEvent(HashMap event, int action) { // Process
      // component
      // events
      logger.debug("OpProjectComponent.processComponentEvent");
      switch (pcType) {
         case GANTT_BOX:
            // the Horizontal scroll Bar is at minumum or maximul position and the box is enabled
            if (action == SCROLL_AT_END && getEditMode()) {
               // take the direction from the event
               int direction = ((Integer) event.get(DIRECTION)).intValue();
               // update the gantt chart start /end date and repaint all
               if (direction == WEST) {
                  OpProjectComponent chart = ((OpProjectComponent) getBoxContent());
                  Line2D drawingLine = chart.getDrawingLine();
                  Rectangle drawingShape = chart.getDrawingRectangle();
                  if (drawingLine != null) {
                     drawingLine.setLine(drawingLine.getX1() + _dayWidth(), drawingLine.getY1(), drawingLine.getX2(), drawingLine.getY2());
                     if (chart.getDragPosition() != null) {
                        chart.getDragPosition().x += _dayWidth();
                     }
                  }
                  if (drawingShape != null) {
                     drawingShape.width += _dayWidth();
                     if (chart.getDragPosition() != null) {
                        chart.getDragPosition().x += _dayWidth();
                     }
                  }
               }
               XComponent dataSet = this.getDataSetComponent();
               if (dataSet != null) {
                  dataSet.clearDataSelection();
               }
               updateGanttChartDate(direction);
            }
            if (action == TAB_ACTIVATED) {
               OpProjectComponent chart = (OpProjectComponent) getBoxContent();
               if (chart != null) {
                  chart.resetCached();
               }
               //clear data selection
               XComponent dataSet = this.getDataSetComponent();
               if (dataSet != null) {
                  dataSet.clearDataSelection();
               }
               if (dataSet != null) {
                  dataSet.removeAllDummyRows();
               }

            }
            processScrollBoxComponentEvent(event, action);
            super.processComponentEvent(event, action);

            break;
         case UTILIZATION_BOX:
            //comes from the resource scroller
            XExtendedComponent resourceTable = (XExtendedComponent) this.getResourceTable();
            if (resourceTable != null && event.get(EVENT_SOURCE) == resourceTable && action == SCROLL) {
               int direction = ((Integer) (event.get(DIRECTION))).intValue();
               if (direction == NORTH || direction == SOUTH) {
                  int increment = ((Integer) (event.get(INCREMENT))).intValue();
                  XComponent scroller = (XComponent) this.getChild(VERTICAL_SCROLL_BAR_INDEX);
                  scroller.moveSliderWithoutEvent(direction, increment);
                  scrollScrollBoxWithoutEvent(direction, increment);
               }
            }

            //comes from box's scroller
            if (action == VALUE_CHANGED) {
               int direction = ((Integer) (event.get(DIRECTION))).intValue();
               if (direction == NORTH || direction == SOUTH) {
                  //send scroll event also to resource scroller
                  if (resourceTable != null) {
                     int increment = ((Integer) (event.get(INCREMENT))).intValue();
                     XComponent scroller = (XComponent) resourceTable.getChild(VERTICAL_SCROLL_BAR_INDEX);
                     scroller.moveSliderWithoutEvent(direction, increment);
                     resourceTable.scrollScrollBoxWithoutEvent(direction, increment);
                  }
               }
               processScrollBoxComponentEvent(event, action);
            }
            super.processComponentEvent(event, action);
            return;
         case GANTT_ACTIVITY:
            if (action == VALUE_CHANGED) {
               XComponent line_editor = (XComponent) (captionEditor._getChild(0)._getChild(0));
               OpGanttValidator validator = (OpGanttValidator) ((XComponent) getDataRow().getParent()).validator();
               try {
                  validator.setDataCellValue(getDataRow(), OpGanttValidator.NAME_COLUMN_INDEX, line_editor.getStringValue());
               }
               catch (XValidationException e) {
                  XComponent form = getContext().getForm();
                  form.showValidationException(e);
               }
               OpProjectComponent chart = (OpProjectComponent) getParent();
               if (chart != null) {
                  chart.resetCached();
               }
               /*close caption editor and caption owner(this) becomes the focused component*/
               _closeCaptionEditor();
               requestFocus();
               getContext().doLayout();
               getContext().repaint();
            }
            return;
      }
      super.processComponentEvent(event, action);
   }

   public void reset() {
      // *** Call super.reset() for correctly implementing derived components?
      switch (pcType) {
         case GANTT_BOX:
            // Clear all data
            XComponent data = (XComponent) getBoxContent();
            if (data != null) {
               data.removeAllChildren();
            }
            break;
      }
   }

   public void addChild(XView child) {
      switch (pcType) {
         case GANTT_BOX:
            getBoxContent().addChild(child);
            break;
         default:
            super.addChild(child);
      }
   }

   /**
    * Method check is a week number is at the beginning of a quarter
    *
    * @param weekNumber the week number
    * @return boolean depending if the week is at the beginning of a quarter
    */
   public boolean isQuarter(int weekNumber) {
      if (weekNumber == 14) {
         return true;
      }
      if (weekNumber == 27) {
         return true;
      }
      if (weekNumber == 40) {
         return true;
      }
      if (weekNumber == 1) {
         return true;
      }
      return false;
   }

   /**
    * Changes the working tool for this project component. (e.g. Drawing tool for gannt chart) <p/> Will be called
    * on a "box" (e.g Gantt Box)
    *
    * @param toolID The id of the new tool to be set.
    */
   public void changeTool(String toolID) {
      OpProjectComponent chart = (OpProjectComponent) getBoxContent();
      // expand all the activities at drawing time
      XComponent dataSet = getDataSetComponent();
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent data = (XComponent) dataSet.getChild(i);
         if (data.expandable()) {
            data.expanded(true, false);
         }
      }
      chart.resetCached();
      doLayout();
      repaint();
      if (!toolID.equalsIgnoreCase(DEFAULT_CURSOR)) {
         // <FIXME author="Mihai Costin" description="Change the cursor to the actual shape.">
         //XComponent tool = XDisplay.getActiveForm().findComponent(toolID);
         //String iconPath = tool.getIcon();
         chart.setMouseCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
         // </FIXME>
      }
      else {
         chart.setMouseCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
      chart.setDrawingToolId(toolID);

   }

   /**
    * Opens a pop-up for the current component, containing the tooltip text or the special detail form, if available
    *
    * @param event
    */
   protected void openTooltipPopup(HashMap event) {
      if (getContext().getDetailsFormRef() != null) {
         int mouseX = ((Integer) event.get(X)).intValue();
         int mouseY = ((Integer) event.get(Y)).intValue();
         // special tool tip
         switch (pcType) {
            case GANTT_ACTIVITY:
               openActivityTooltip(mouseX, mouseY);
               break;
            case UTILIZATION_ROW:
               //get time interval on tap x coordinate inside the chart
               List detailsTimeInterval = ((OpProjectComponent) getContext()).findTimeInterval(mouseX + getBounds().x);
               showUtilizationRowDetails(mouseX, mouseY, detailsTimeInterval);
               break;
            default:
               if (this.getTooltip() != null) {
                  super.openTooltipPopup(event);
               }
               break;
         }
      }
      else {
         if (this.getTooltip() != null) {
            // normal tool tip
            super.openTooltipPopup(event);
         }
      }
   }

   protected void openActivityTooltip(int mouseX, int mouseY) {
      XComponent form;
      if (getContext().getDetailsForm() == null) {
         // load it
         form = XDisplay.loadForm(getContext().getDetailsFormRef(), null, null, null);
         getContext().setDetailsForm(form);
      }
      else {
         form = getContext().getDetailsForm();
      }
      // fill up the form
      fillUpDetails(form);
      //show tool tip form
      if (form != null) {
         XView parent = form.getParent();
         if (parent != null) {
            parent.removeChild(form);
         }
      }
      showToolTipFormForComponent(mouseX, mouseY, form);
   }

   /**
    * Fills up the details in the given form. It assumes the presence of a number of "standard-named" fields.
    *
    * @param form
    */
   private void fillUpDetails(XComponent form) {
      boolean showCosts = ((OpProjectComponent) getContext()).showCosts() == null
           || ((OpProjectComponent) getContext()).showCosts().booleanValue();
      XComponent dataRow = getDataRow();
      XComponent field;

      field = form.findComponent(DETAILS_NAME);
      if (field != null) {
         field.setStringValue((String) getActivityProperty(dataRow, DETAILS_NAME));
      }

      field = form.findComponent(DETAILS_START);
      if (field != null) {
         field.setDateValue((Date) getActivityProperty(dataRow, DETAILS_START));
      }

      field = form.findComponent(DETAILS_FINISH);
      if (field != null) {
         field.setDateValue((Date) getActivityProperty(dataRow, DETAILS_FINISH));
      }

      field = form.findComponent(DETAILS_DURATION);
      if (field != null) {
         field.setDoubleValue(((Double) getActivityProperty(dataRow, DETAILS_DURATION)).doubleValue());
      }

      field = form.findComponent(DETAILS_COMPLETE);
      if (field != null) {
         field.setDoubleValue(((Double) getActivityProperty(dataRow, DETAILS_COMPLETE)).doubleValue());
      }

      field = form.findComponent(DETAILS_BASE_EFFORT);
      if (field != null) {
         field.setDoubleValue(((Double) getActivityProperty(dataRow, DETAILS_BASE_EFFORT)).doubleValue());
      }

      field = form.findComponent(DETAILS_COST);
      if (field != null) {
         field.setDoubleValue(((Double) getActivityProperty(dataRow, DETAILS_COST)).doubleValue());
         field.setVisible(showCosts);
         form.findComponent(DETAILS_COST_LABEL).setVisible(showCosts);
      }

      field = form.findComponent(DETAILS_RESOURCE_NAMES);
      if (field != null) {
         field.setStringValue((String) getActivityProperty(dataRow, DETAILS_RESOURCE_NAMES));
      }

      field = form.findComponent(DETAILS_CATEGORY);
      if (field != null) {
         if (field.getChildCount() > 0) {
            ((XComponent) field.getChild(0)).setStringValue((String) getActivityProperty(dataRow, DETAILS_CATEGORY));
         }
         else {
            field.setStringValue((String) getActivityProperty(dataRow, DETAILS_CATEGORY));
         }
      }

      field = form.findComponent(DETAILS_PROCEEDS);
      if (field != null) {
         field.setDoubleValue(((Double) getActivityProperty(dataRow, DETAILS_PROCEEDS)).doubleValue());
         field.setVisible(showCosts);
         form.findComponent(DETAILS_PROCEEDS_LABEL).setVisible(showCosts);
      }
   }


   public void setStateful(boolean stateful) {
      switch (pcType) {
         case GANTT_BOX:
            super.setStateful(stateful);
            if (stateful) {
               makeScrollBarsStateful();
            }
            break;
         default:
            super.setStateful(stateful);
            break;
      }
   }

   public Serializable state() {
      Serializable state = null;
      switch (pcType) {
         case GANTT_BOX:
            OpProjectComponent chart = (OpProjectComponent) getBoxContent();
            List ganttState = new ArrayList();
            ganttState.add(chart.getDrawingToolId());
            ganttState.add(chart.getMouseCursor());
            ganttState.add(new Byte(getTimeUnit()));
            state = (Serializable) ganttState;
            break;
         case UTILIZATION_BOX:
            List utilizationState = new ArrayList();
            utilizationState.add(new Byte(getTimeUnit()));
            state = (Serializable) utilizationState;
            break;
      }
      return state;
   }

   public void restoreState(Serializable state) {
      switch (pcType) {
         case GANTT_BOX:
            OpProjectComponent chart = (OpProjectComponent) getBoxContent();
            List ganttState = (List) state;
            chart.setDrawingToolId((String) ganttState.get(0));
            chart.setMouseCursor((Cursor) ganttState.get(1));
            setTimeUnit(((Byte) ganttState.get(2)).byteValue());
            break;
         case UTILIZATION_BOX:
            List utilizationState = (List) state;
            setTimeUnit(((Byte) utilizationState.get(0)).byteValue());
            break;
      }
   }

   /**
    * Finds the time interval depending on the <code>x</code> mouse coordinate in the chart.
    * Must be called on a <code>UTILIZATION_BOX</code> component.
    *
    * @param mouseX <code>int</code> representing the mouse x coordinate in the chart
    * @return a <code>List</code> containing start and finish date.
    */
   private List findTimeInterval(int mouseX) {
      List interval = new ArrayList();
      Map headerTimeDimensionMap = getUtilizationHeaderTimeDimension();
      Set entries = headerTimeDimensionMap.entrySet();
      Iterator iterator = entries.iterator();
      Map.Entry startEntry = null;
      Map.Entry endEntry = null;

      if (iterator.hasNext()) {
         startEntry = (Map.Entry) iterator.next();
      }
      while (iterator.hasNext()) {
         endEntry = (Map.Entry) iterator.next();
         int startKey = ((Integer) startEntry.getKey()).intValue();
         int endKey = ((Integer) endEntry.getKey()).intValue();
         if (mouseX >= startKey && mouseX <= endKey) { //we find a interval
            Date startDate = (Date) startEntry.getValue();
            Date endDate = (Date) endEntry.getValue();
            if (this.getTimeUnit() != XCalendar.DAYS) {
               //try to find a more precise interval
               double days = ((endDate.getTime() - startDate.getTime()) / XCalendar.MILLIS_PER_DAY);
               double pxPerDay = (endKey - startKey) / days;
               double daysDiff = (mouseX - startKey) / pxPerDay;
               startDate = new Date((long) (startDate.getTime() + daysDiff * XCalendar.MILLIS_PER_DAY));
               endDate = new Date(startDate.getTime() + XCalendar.MILLIS_PER_DAY);
            }
            interval.add(startDate);
            interval.add(endDate);
            return interval;
         }
         //next interval
         startEntry = endEntry;
      }
      //no interval found (should not happen)
      return interval;
   }

   /**
    * Shows up the utilization row details form for a selected time interval
    *
    * @param detailsTimeInterval <code>List</code> which holds the selected time interval
    * @param mouseX              <code>int</code> the mouse x coordinate relative to utilization row bounds
    * @param mouseY              <code>int</code> the mouse y coordinate relative to utilization row bounds
    */
   private void showUtilizationRowDetails(int mouseX, int mouseY, List detailsTimeInterval) {

      XComponent dataRow = getDataRow();
      //check if the selected interval has valid utilization values
      boolean validData = existsValidUtilizationData(detailsTimeInterval);
      if (validData) {
         XComponent.closeTooltips();
         XComponent form;
         String descriptor = (String) ((XComponent) dataRow.getChild(UTILIZATION_DESCRIPTOR_COLUMN_INDEX)).getValue();
         HashMap params = new HashMap();
         params.put("ResourceId", dataRow.getStringValue());
         //marked details as visible
         params.put("DetailsTimeInterval", detailsTimeInterval);

         OpProjectComponent utilizationBox = (OpProjectComponent) getContext();
         utilizationBox.setUtilizationVisibleDetailsInterval(detailsTimeInterval);
         String location;
         if (UTILIZATION_RESOURCE_DESCRIPTOR.equals(descriptor)) {
            location = utilizationBox.getDetailsFormRef();
         }
         else {
            location = utilizationBox.getAlternateDetailsFormRef();
         }
         form = XDisplay.loadForm(location, params, null, null);
         //show tool tip form
         showToolTipFormForComponent(mouseX, mouseY, form);
      }
   }

   /**
    * Refreshes the utilization row details form for a selected time interval according to the new mouse position in
    * utilization row.A new details form will be shown only if conditions are met: the selected interval is different
    * from previous one and there are valid utilization data for the resource in the specified interval.
    *
    * @param mouseX <code>int</code> the mouse x coordinate relative to utilization row bounds
    * @param mouseY <code>int</code> the mouse y coordinate relative to utilization row bounds
    */
   private void refreshUtilizationRowDetails(int mouseX, int mouseY) {
      OpProjectComponent utilizationBox = (OpProjectComponent) getContext();
      List previousTimeInterval = utilizationBox.getUtilizationVisibleDetailsInterval();

      if (previousTimeInterval.isEmpty()) {
         return; //no previous selected interval
      }
      //get current time interval based on x coordinate inside the chart
      List currentTimeInterval = utilizationBox.findTimeInterval(mouseX + getBounds().x);
      //current interval when mouse moving => must be different from previous and
      // must have valid data in order to refresh form
      if (!(previousTimeInterval.get(INTERVAL_START_INDEX).equals(currentTimeInterval.get(INTERVAL_START_INDEX)) &&
           previousTimeInterval.get(INTERVAL_FINISH_INDEX).equals(currentTimeInterval.get(INTERVAL_FINISH_INDEX)))) {

         if (existsValidUtilizationData(currentTimeInterval)) {
            logger.debug("refresh previous " + previousTimeInterval + " current " + currentTimeInterval);
            showUtilizationRowDetails(mouseX, mouseY, currentTimeInterval);
         }
         else {
            logger.debug("refresh no data in" + currentTimeInterval);
            //update the selected details time interval
            utilizationBox.setUtilizationVisibleDetailsInterval(currentTimeInterval);
            XComponent.closeTooltips();
         }

      }
   }

   /**
    * Shows up the tool tip form for the component.
    *
    * @param mouseX <code>int</code> the mouse x coordinate
    * @param mouseY <code>int</code> the mouse y coordinate
    * @param form   <code>XComponent.FORM</code> the form.
    */
   private void showToolTipFormForComponent(int mouseX, int mouseY, XComponent form) {
      // show the form
      Rectangle compBounds = new Rectangle();
      compBounds.width = 0;
      compBounds.height = 0;
      Point showAt = absolutePosition(mouseX, mouseY);
      compBounds.x = showAt.x;
      compBounds.y = showAt.y;
      XComponent tooltip = XComponent.createToolTip(form);
      openToolTipForComponent(tooltip, compBounds);
   }

   /**
    * Set up the <code>UTILIZATION_VISIBLE_DETAILS_INTERVAL</code> property for a <code>UTILIZATION_BOX</code>
    *
    * @param timeInterval <code>List</code> which holds the selected time interval
    */
   private void setUtilizationVisibleDetailsInterval(List timeInterval) {
      setTransientProperty(UTILIZATION_VISIBLE_DETAILS_INTERVAL, timeInterval);
   }

   /**
    * Return the value of <code>UTILIZATION_VISIBLE_DETAILS_INTERVAL</code> property for a <code>UTILIZATION_BOX</code>
    *
    * @return <code>List</code> which holds the previous selected time interval
    */
   private List getUtilizationVisibleDetailsInterval() {
      return (List) getTransientProperty(UTILIZATION_VISIBLE_DETAILS_INTERVAL);
   }


   /**
    * Sets up the value of <code>UTILIZATION_HEADER_TIME_DIMENSION</code> property of the utilization box
    *
    * @param timeDimension <code>TreeMap</code> of time dimension
    */
   private void setUtilizationHeaderTimeDimension(Map timeDimension) {
      setTransientProperty(UTILIZATION_HEADER_TIME_DIMENSION, timeDimension);
   }

   /**
    * Returns the value of <code>UTILIZATION_HEADER_TIME_DIMENSION</code> property of the utilization box
    *
    * @return Map representing time dimension
    */
   private Map getUtilizationHeaderTimeDimension() {
      return (Map) getTransientProperty(UTILIZATION_HEADER_TIME_DIMENSION);
   }

   /**
    * Checks if for the selected time interval, the <code>UTILIZATION_ROW</code> has valid utilization values .
    *
    * @param detailsTimeInterval <code>List</code> which holds the selected time interval
    * @return boolean <code>true</code> if there are any values greater than 0 ; <code>false</code> otherwise
    */
   public boolean existsValidUtilizationData(List detailsTimeInterval) {

      Date rowStartDate = ((XComponent) getDataRow().getChild(UTILIZATION_START_COLUMN_INDEX)).getDateValue();
      int valuesStartIndex = XDisplay.getDefaultDisplay().getCalendar().getDaysDifference(rowStartDate, (Date) detailsTimeInterval.get(INTERVAL_START_INDEX));
      int valuesEndIndex = XDisplay.getDefaultDisplay().getCalendar().getDaysDifference(rowStartDate, (Date) detailsTimeInterval.get(INTERVAL_FINISH_INDEX));
      //list of values
      List values = ((XComponent) (getDataRow().getChild(UTILIZATION_VALUES_COLUMN_INDEX))).getListValue();
      //suppose not existent valid data in selected interval
      boolean validData = false;
      for (int i = valuesStartIndex; i < valuesEndIndex; i++) {
         if (((Double) values.get(i)).doubleValue() > 0) {
            validData = true;
            break;
         }
      }
      return validData;
   }

   /**
    * Performs initialization of the <code>UTILIZATION_HEADER_TIME_DIMENSION </code> .
    * Must be called on a <code>UTILIZATION_CHART</code>.
    */
   private void initializeUtilizationChartTimeDimension() {
      OpProjectComponent box = (OpProjectComponent) getContext();
      //get the header details (a linked map with <key ->x coordinate ,value -->Date)  and perform lazy initialization
      Map headerTimeDimensionMap = box.getUtilizationHeaderTimeDimension();

      Rectangle headerBounds = box.getChild(HEADER_INDEX).getChild(0).getBounds();

      if (headerTimeDimensionMap == null) {
         headerTimeDimensionMap = new TreeMap();
         box.setUtilizationHeaderTimeDimension(headerTimeDimensionMap);
      }
      //clear details
      headerTimeDimensionMap.clear();
      //utilization visible interval not selected
      box.setUtilizationVisibleDetailsInterval(new ArrayList());

      byte timeUnit = box.getTimeUnit();
      // width increment for x axis
      int widthIncrement = (int) box._dayWidth();

      Calendar time = XDisplay.getDefaultDisplay().getCalendar().getCalendar();
      //start of the utilization start
      time.setTime(getStart());
      int x = 0;
      //special handling for months
      if (timeUnit == XCalendar.MONTHS) {
         time.set(Calendar.DAY_OF_MONTH, 1);
      }
      //get the header details (a map with <key ->x coordinate ,value -->Date)
      headerTimeDimensionMap.put(new Integer(x), new Date(time.getTimeInMillis()));

      while (x <= headerBounds.width) {
         x += widthIncrement;
         switch (timeUnit) {
            case XCalendar.DAYS:
               time.setTimeInMillis(time.getTimeInMillis() + XCalendar.MILLIS_PER_DAY);
               break;
            case XCalendar.WEEKS:
               time.setTimeInMillis(time.getTimeInMillis() + XCalendar.MILLIS_PER_WEEK);
               break;
            case XCalendar.MONTHS:
               time.set(Calendar.MONTH, time.get(Calendar.MONTH) + 1);
               break;
         }
         headerTimeDimensionMap.put(new Integer(x), new Date(time.getTimeInMillis()));
      }
   }


   /**
    * Set the cursor for this component. Extended behavior for project components.
    *
    * @param cursor The new cursor for this component.
    */
   protected void setMouseCursor(Cursor cursor) {
      super.setMouseCursor(cursor);
      switch (pcType) {
         case GANTT_CHART:
            //set also for all children
            for (int i = 0; i < getChildCount(); i++) {
               OpProjectComponent child = (OpProjectComponent) getChild(i);
               child.setMouseCursor(cursor);
            }
            break;
      }
   }


   public Dimension prepareImageableView() {
      Dimension size;
      switch (pcType) {
         case GANTT_BOX:
            //this is required in order to make the gantt scroller dissapear
            getBoxContent().setEditMode(getEditMode());
            setEditMode(false);
            size = super.prepareImageableView();
            break;
         case UTILIZATION_BOX:
            hideScrollBars();
            size = getBoxContent().getBounds().getSize();
            break;
         default:
            size = super.prepareImageableView();
            break;
      }
      return size;
   }

   public void restoreImageableView() {
      switch (pcType) {
         case GANTT_BOX:
            setEditMode(getBoxContent().getEditMode());
            getBoxContent().setEditMode(false);
            super.restoreImageableView();
            break;
         default:
            super.restoreImageableView();
            break;
      }
   }


   private Date roundEnd(OpProjectComponent box, Date end) {
      if (box.getTimeUnit() == XCalendar.MONTHS) {
         Calendar calendar = XDisplay.getDefaultDisplay().getCalendar().getCalendar();
         calendar.setTime(end);
         int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
         int month = calendar.get(Calendar.MONTH);
         if (dayOfMonth < calendar.getMaximum(Calendar.DAY_OF_MONTH) / 2) {
            month--;
            calendar.set(Calendar.MONTH, month);
         }
         int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
         calendar.set(Calendar.DAY_OF_MONTH, lastDay);
         end = new Date(calendar.getTime().getTime());
      }
      return end;
   }

   private Date roundStart(OpProjectComponent box, Date start) {
      if (box.getTimeUnit() == XCalendar.MONTHS) {
         //for months view, round to the first day of month
         Calendar calendar = XDisplay.getDefaultDisplay().getCalendar().getCalendar();
         calendar.setTime(start);
         int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
         int month = calendar.get(Calendar.MONTH);
         if (dayOfMonth >= calendar.getMaximum(Calendar.DAY_OF_MONTH) / 2) {
            month++;
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.MONTH, month);
            start = new Date(calendar.getTime().getTime());
         }
         else {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            start = new Date(calendar.getTime().getTime());
         }
      }
      return start;
   }

   public void setCategoryColorMap(Map colorMap) {
      setProperty(CATEGORY_COLOR_MAP, colorMap);
   }

   public Map getCategoryColorMap() {
      return (Map) getProperty(CATEGORY_COLOR_MAP);
   }

   /**
    * Sends an activity selected event, with the given activity.
    *
    * @param activity a <code>XComponent(DATA_ROW)</code> representing an activity.
    */
   private void sendActivitySelectEvent(XComponent activity) {
      HashMap event = new HashMap();
      event.put(SELECTED_ACTIVITY_ARGUMENT, activity);
      invokeActionHandler(getOnActivitySelect(), event);
   }

   public Boolean showCosts() {
      return (Boolean) getProperty(SHOW_COSTS);
   }

   public void setShowCosts(boolean showCosts) {
      this.setProperty(SHOW_COSTS, Boolean.valueOf(showCosts));
   }


   private void writeObject(java.io.ObjectOutputStream stream)
        throws IOException {
      stream.writeByte(pcType);
   }

   private void readObject(java.io.ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
      pcType = stream.readByte();
   }

}
