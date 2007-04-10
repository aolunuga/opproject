/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_planning.components;

import onepoint.express.*;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.components.OpProjectComponent;

import java.awt.*;
import java.awt.geom.Line2D;
import java.sql.Date;
import java.util.*;
import java.util.List;

/**
 * Advanced version of the project component.
 *
 * @author mihai.costin
 */
public class OpAdvancedProjectComponent extends OpProjectComponent {

   private static final XLog logger = XLogFactory.getLogger(OpAdvancedProjectComponent.class);

   public final static byte WBS_BOX = 20;
   public final static byte WBS_CHART = 21;
   public final static byte WBS_ACTIVITY = 22;
   public final static byte WBS_GLYPH = 23;
   public final static byte WBS_ROOT = 24;

   // WBS chart
   private final static int WBS_CHART_MAX_GLYPS = 5;
   private final static int WBS_CHART_MAX_TEXT_GLYPS = 2;

   //outline levels in wbs chart
   private final static int PARENT_WBS_OUTLINE = 0;
   private final static int CHILD_WBS_OUTLINE = 1;

   //Effort and Cost i18n text for WBS Activities
   public final static Integer EFFORT_TEXT = new Integer(286);
   public final static Integer COSTS_TEXT = new Integer(287);
   //Project name property for the WBS box
   private final static Integer PROJECT_NAME = new Integer(303);

   public final static String DEFAULT_WBS_CHART_STYLE = "wbs-chart-default";
   public final static String DEFAULT_WBS_ACTIVITY_STYLE = "wbs-activity-default";
   public final static String DEFAULT_WBS_ROOT_STYLE = "wbs-root-default";

   public final static XStyle DEFAULT_WBS_CHART_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES;


   private final static String ROW_SEPARATOR_ELEMENT = "-";
   private final static String COLON = ": ";

   static {
      // Default WBS chart style
      DEFAULT_WBS_CHART_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_WBS_CHART_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_WBS_CHART_STYLE_ATTRIBUTES.alternate_background = XStyle.DEFAULT_ICON_BUTTON;
      DEFAULT_WBS_CHART_STYLE_ATTRIBUTES.border_dark = XStyle.DEFAULT_SHADOW_ALTERNATE;
      DEFAULT_WBS_CHART_STYLE_ATTRIBUTES.border_light = XStyle.DEFAULT_SHADOW;
      DEFAULT_WBS_CHART_STYLE_ATTRIBUTES.left = 12;
      DEFAULT_WBS_CHART_STYLE_ATTRIBUTES.right = 12;
      DEFAULT_WBS_CHART_STYLE_ATTRIBUTES.top = 12;
      DEFAULT_WBS_CHART_STYLE_ATTRIBUTES.bottom = 12;
      DEFAULT_WBS_CHART_STYLE_ATTRIBUTES.gap = 12;
      addDefaultStyle(DEFAULT_WBS_CHART_STYLE, DEFAULT_WBS_CHART_STYLE_ATTRIBUTES);

      //WBS activity style
      DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES.alternate_background = XStyle.DEFAULT_BUTTON_ALTERNATE;
      DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES.border_light = XStyle.DEFAULT_SHADOW;
      DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES.border_dark = XStyle.DEFAULT_SHADOW_ALTERNATE;
      DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES.top = 2;
      DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES.bottom = 2;
      DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES.left = 2;
      DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES.right = 2;
      DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES.gap = 4;
      addDefaultStyle(DEFAULT_WBS_ACTIVITY_STYLE, DEFAULT_WBS_ACTIVITY_STYLE_ATTRIBUTES);

      //WBS root style
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES.foreground = XStyle.DEFAULT_FIELD;
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES.alternate_background = XStyle.DEFAULT_ICON_BUTTON;
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES.border_light = XStyle.DEFAULT_SHADOW;
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES.border_dark = XStyle.DEFAULT_SHADOW_ALTERNATE;
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES.top = 2;
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES.bottom = 2;
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES.left = 2;
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES.right = 2;
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES.gap = 4;
      DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES.setFontStyle(Font.BOLD);
      addDefaultStyle(DEFAULT_WBS_ROOT_STYLE, DEFAULT_WBS_ROOT_STYLE_ATTRIBUTES);
   }

   public OpAdvancedProjectComponent(byte type) {
      super(type);
   }

   protected boolean isChartActivity() {
      return super.isChartActivity() || (pc_type == WBS_ACTIVITY);
   }

   protected boolean isChartComponent() {
      return super.isChartComponent() || (pc_type == WBS_CHART);
   }

   protected boolean isActivityBox(OpProjectComponent component) {
      return super.isActivityBox(component) || (component.getComponentType() == WBS_BOX);
   }


   /**
    * Sets up the <code>WBS_BOX</code> component's <code>PROJECT_NAME</code> property, to the specified
    * <code>projectName</code> value. By default, this property is set blank in the component constructor.
    * Must be called on the box component.
    *
    * @param projectName <code>String<code> representing the project name
    */
   public final void setProjectName(String projectName) {
      setProperty(PROJECT_NAME, projectName);
   }

   /**
    * Returns the existent value of the <code>PROJECT_NAME</code> property for the calling <code>WBS_BOX</code> component.
    * Must be called on the box component.
    *
    * @return <code>String</code> representing the project name
    */
   public final String getProjectName() {
      return (String) getProperty(PROJECT_NAME);
   }

   public void setEffortText(String value) {
      setProperty(EFFORT_TEXT, value);
   }

   public String getEffortText() {
      return (String) getProperty(EFFORT_TEXT);
   }

   public void setCostsText(String value) {
      setProperty(COSTS_TEXT, value);
   }

   public String getCostsText() {
      return (String) getProperty(COSTS_TEXT);
   }


   /**
    * @see onepoint.express.XView#isEnterLeaveEventSource()
    */
   public boolean isEnterLeaveEventSource() {
      if (pc_type == WBS_GLYPH) {
         return false;
      }
      return super.isEnterLeaveEventSource();
   }


   protected void initializeProjectComponent(byte type) {
      setFocusable(true);
      pc_type = type;

      switch (pc_type) {
         case WBS_ACTIVITY:
            setStyle(DEFAULT_WBS_ACTIVITY_STYLE);
            break;
         case WBS_ROOT:
            setStyle(DEFAULT_WBS_ROOT_STYLE);
            break;
         case WBS_CHART:
            setStyle(DEFAULT_WBS_CHART_STYLE);
            setFocusable(false); // *** really?
            setGridX(120);
            setGridY(60);
            break;
         case WBS_BOX:
            setStyle(DEFAULT_GANTT_BOX_STYLE);
            XView view_port = initializeScrollBox();
            OpAdvancedProjectComponent wbs_chart = new OpAdvancedProjectComponent(WBS_CHART);
            view_port.addChild(wbs_chart);
            setProjectName("");
            setEditMode(false);
            break;
         case WBS_GLYPH:
            setStyle(DEFAULT_GANTT_ACTIVITY_STYLE);
            break;
         default:
            super.initializeProjectComponent(type);
      }
   }


   public XComponent getContext() {
      switch (pc_type) {
         case WBS_ROOT:
         case WBS_GLYPH:
         case WBS_ACTIVITY:
         case WBS_CHART:
            // Context is always the WBS BOX
            XComponent component = (XComponent) getParent();
            while (component != null) {
               if ((component instanceof OpAdvancedProjectComponent)
                    && (component.getComponentType() == WBS_BOX)) {
                  return component;
               }
               component = (XComponent) (component.getParent());
            }
            break;
         default:
            return super.getContext();
      }
      return null;
   }


   public Dimension getPreferredSize() {
      switch (pc_type) {
         case WBS_CHART:
            return preferredWBSChartSize();
         case WBS_GLYPH:
            return new Dimension(getWidth(), getHeight());
         default:
            return super.getPreferredSize();
      }
   }

   protected Dimension preferredWBSChartSize() {

      XComponent box = getContext();
      XComponent data_set = box.getDataSetComponent();
      XStyle style = getStyleAttributes();
      // get font metrics according to WBSChartStyle
      FontMetrics metrics = getFontMetrics(style.font());

      // default WBS activity height
      int wbsActivityHeight = metrics.getAscent() + metrics.getDescent();

      int grid_x = getGridX();
      int grid_y = WBS_CHART_MAX_GLYPS * wbsActivityHeight + style.top + style.bottom;
      int width = 0;
      int height = 0;
      int max_height = 0;
      XComponent activity;
      for (int i = 0; i < data_set.getChildCount(); i++) {
         activity = (XComponent) (data_set.getChild(i));
         if (!activity.getFiltered()) {
            if (activity.getOutlineLevel() == 0) {
               // Horizontal size
               if (width > 0) {
                  width += style.gap;
               }
               width += grid_x;
               if (height > max_height) {
                  max_height = height;
               }
               height = 0;
            }
            else if (activity.getOutlineLevel() == 1) {
               // Vertical size
               if (height > 0) {
                  height += style.gap;
               }
               height += grid_y;
            }
         }
      }
      width += style.gap + style.right;
      if (height > max_height) {
         max_height = height;
      }
      // Add grid lines for root node (project) and first outline-level activities
      max_height += style.top + style.bottom + grid_y + style.gap + grid_y + style.gap;
      return new Dimension(width, max_height);
   }

   public void doLayout() {
      switch (pc_type) {
         case WBS_BOX:
            scrollSlidersToZero();
            doLayoutScrollBox();
            scrollSlidersToLastValue();
            syncBoxUISelection();
            break;
         case WBS_CHART:
            doLayoutWBSChart();
            break;
         case WBS_ROOT:
            doLayoutWBSGlyphs();
         case WBS_ACTIVITY:
            doLayoutWBSGlyphs();
            break;
         case WBS_GLYPH:
            break;
         default:
            super.doLayout();
      }
   }


   public void paint(Graphics g, Rectangle clip_area) {
      Rectangle bounds = getBounds();
      XStyle style = getStyleAttributes();
      switch (pc_type) {
         case WBS_ACTIVITY:
            paintWBSActivity(g, clip_area);
            paintChildren(g, clip_area);
            break;
         case WBS_ROOT:
            paintWBSRoot(g);
            paintChildren(g, clip_area);
            break;
         case WBS_CHART:
            paintWBSChart(g, clip_area);
            break;
         case WBS_GLYPH:
            g.setColor(style.foreground);
            g.setFont(style.font());
            String s = getText();
            if (s != null) {
               g.drawString(s, 0, g.getFontMetrics().getAscent());
            }
            break;
         case WBS_BOX:
            drawBox(g, style, 0, 0, bounds.width, bounds.height);
            super.paint(g, clip_area);
            break;
         default:
            super.paint(g, clip_area);
      }
   }

   private String constructProjectCaption(String project, XStyle style, int width) {
      StringBuffer buff = new StringBuffer(project);
      buff.append(LINE_SEPARATOR);

      OpAdvancedProjectComponent box = (OpAdvancedProjectComponent) getContext();
      OpGanttValidator validator = (OpGanttValidator) box.getDataSetComponent().validator();

      String rowSeparator = getRowSeparator(style, width);
      buff.append(rowSeparator);
      buff.append(LINE_SEPARATOR);

      buff.append(box.getEffortText());
      buff.append(COLON);
      double effort = validator.getProjectEffort();
      buff.append(XDisplay.getCalendar().localizedDoubleToString(effort));
      buff.append(LINE_SEPARATOR);

      buff.append(box.getCostsText());
      buff.append(COLON);
      double cost = validator.getProjectCost();
      buff.append(XDisplay.getCalendar().localizedDoubleToString(cost, 2));
      buff.append(LINE_SEPARATOR);

      return buff.toString();
   }


   private void paintWBSChart(Graphics g, Rectangle clip_area) {

      XStyle style = getStyleAttributes();
      OpProjectComponent draggedWbs = getDraggedComponent();
      Rectangle bounds = getBounds();
      g.setColor(style.background);
      g.fillRect(0, 0, bounds.width, bounds.height);

      Point[] path = getPath();
      if (path.length == 0) {
         return;
      }

      // *** Paint other activities (children *generated* by layouting process)
      paintChildren(g, clip_area);
      // *** Paint connectors by using path generated by layouting process
      int[] x_points = new int[4];
      int[] y_points = new int[4];
      g.setColor(style.border_light);
      for (int i = 0; i < path.length;) {
         x_points[0] = path[i].x;
         y_points[0] = path[i].y;
         i++;
         x_points[1] = path[i].x;
         y_points[1] = path[i].y;
         i++;
         x_points[2] = path[i].x;
         y_points[2] = path[i].y;
         i++;
         x_points[3] = path[i].x;
         y_points[3] = path[i].y;
         i++;
         g.drawPolyline(x_points, y_points, 4);
      }

      // see if we have some dragging - has to be drawn on top
      if (draggedWbs != null) {
         ArrayList dummy = new ArrayList();
         dummy.add(draggedWbs);
         paintChildren(g, clip_area, dummy);

         //also for dragging case - paint also the position/insert line line
         //draw the insert line
         Line2D insertLine = getInsertLine();
         if (insertLine != null) {
            g.setColor(style.border_dark);
            g.drawLine((int) insertLine.getX1(), (int) insertLine.getY1(),
                 (int) insertLine.getX2(), (int) insertLine.getY2());
         }

      }
   }

   private void paintWBSActivity(Graphics g, Rectangle clip_area) {
      XStyle style = getStyleAttributes();
      Rectangle bounds = getBounds();
      switch (getActivityType()) {
         case SCHEDULED_TASK_ACTIVITY:
         case STANDARD_ACTIVITY:
         case COLLECTION_ACTIVITY:
            // Draw shadow *before* the rest
            Graphics2D g2D = (Graphics2D) g;
            Stroke previousStroke = g2D.getStroke();
            g.setColor(style.border_dark);
            g.fillRect(2, 2, bounds.width - 1, bounds.height - 1);
            if (getFocused() || getDrawActivityBorder()) {
               g.setColor(style.selection_background);
            }
            else {
               // Check for category color
               Integer colorIndex = getColorIndex();
               if ((colorIndex != null) && (colorIndex.intValue() < XStyle.colorSchema.size())) {
                  g.setColor((Color) XStyle.colorSchema.get(colorIndex.intValue()));
               }
               else {
                  g.setColor(style.alternate_background);
               }
            }
            g.fillRect(0, 0, bounds.width - 1, bounds.height - 1);
            g.setColor(style.border_light);
            if (getDrawActivityBorder()) {
               g2D.setStroke(new BasicStroke(2));
            }
            g.drawRect(0, 0, bounds.width - 2, bounds.height - 2);
            if (getDrawActivityBorder()) {
               g2D.setStroke(previousStroke);
            }
            // Draw diagonal lines if 50% and 100% completed respectively
            XComponent activity = getDataRow();
            double complete = OpGanttValidator.getComplete(activity);
            g.setColor(style.border_light);
            if (complete > 0) {
               g.drawLine(0, 0, bounds.width - 2, bounds.height - 2);
            }
            if (complete == 100) {
               g.drawLine(bounds.width - 2, 0, 0, bounds.height - 2);
            }
            paintChildren(g, clip_area);
            break;
         case TASK_ACTIVITY:
         case COLLECTION_TASK_ACTIVITY: {

            g.setColor(style.border_dark);
            g.fillRoundRect(2, 2, bounds.width, bounds.height, XStyle.ARC_DIAMETER, XStyle.ARC_DIAMETER);

            if (getFocused() || getDrawActivityBorder()) {
               g.setColor(style.selection_background);
            }
            else {
               // Check for category color
               Integer colorIndex = getColorIndex();
               if ((colorIndex != null) && (colorIndex.intValue() < XStyle.colorSchema.size())) {
                  g.setColor((Color) XStyle.colorSchema.get(colorIndex.intValue()));
               }
               else {
                  g.setColor(style.alternate_background);
               }
            }

            // g.drawRect(0, 0, bounds.width - 2, bounds.height - 2);
            g2D = (Graphics2D) g;
            previousStroke = g2D.getStroke();
            g2D.fillRoundRect(0, 0, bounds.width - 1, bounds.height - 1, XStyle.ARC_DIAMETER, XStyle.ARC_DIAMETER);
            g.setColor(style.border_light);
            if (getDrawActivityBorder()) {
               g2D.setStroke(new BasicStroke(2f));
            }
            else {
               g2D.setStroke(new BasicStroke(1f));
            }
            g2D.drawRoundRect(0, 0, bounds.width - 1, bounds.height - 1, XStyle.ARC_DIAMETER, XStyle.ARC_DIAMETER);
            g2D.setStroke(previousStroke);
            // Draw diagonal lines if 50% and 100% completed respectively
            activity = getDataRow();
            complete = OpGanttValidator.getComplete(activity);
            g.setColor(style.border_light);
            if (complete == 100) {
               g.drawLine(2, 2, bounds.width - 2, bounds.height - 2);
               g.drawLine(bounds.width - 2, 2, 2, bounds.height - 2);
            }
            paintChildren(g, clip_area);
         }
         break;
         case MILESTONE_ACTIVITY:

            // Draw shadow
            int[] xp = new int[8];
            int[] yp = new int[8];
            xp[0] = 13;
            yp[0] = 3;
            xp[1] = bounds.width - 9;
            yp[1] = 3;
            xp[2] = bounds.width + 1;
            yp[2] = 13;
            xp[3] = bounds.width + 1;
            yp[3] = bounds.height - 9;
            xp[4] = bounds.width - 9;
            yp[4] = bounds.height + 1;
            xp[5] = 13;
            yp[5] = bounds.height + 1;
            xp[6] = 3;
            yp[6] = bounds.height - 9;
            xp[7] = 3;
            yp[7] = 13;
            g.setColor(style.border_dark);
            g.fillPolygon(xp, yp, 8);

            if (getFocused() || getDrawActivityBorder()) {
               g.setColor(style.selection_background);
            }
            else {
               // Check for category color
               Integer colorIndex = getColorIndex();
               if ((colorIndex != null) && (colorIndex.intValue() < XStyle.colorSchema.size())) {
                  g.setColor((Color) XStyle.colorSchema.get(colorIndex.intValue()));
               }
               else {
                  g.setColor(style.alternate_background);
               }
            }

            xp[0] = 10;
            yp[0] = 0;
            xp[1] = bounds.width - 2 - 10;
            yp[1] = 0;
            xp[2] = bounds.width - 2;
            yp[2] = 10;
            xp[3] = bounds.width - 2;
            yp[3] = bounds.height - 2 - 10;
            xp[4] = bounds.width - 2 - 10;
            yp[4] = bounds.height - 2;
            xp[5] = 10;
            yp[5] = bounds.height - 2;
            xp[6] = 0;
            yp[6] = bounds.height - 2 - 10;
            xp[7] = 0;
            yp[7] = 10;
            g.fillPolygon(xp, yp, 8);
            g.setColor(style.border_light);

            g2D = (Graphics2D) g;
            previousStroke = g2D.getStroke();
            if (getDrawActivityBorder()) {
               g2D.setStroke(new BasicStroke(2));
            }
            g.drawPolygon(xp, yp, 8);
            if (getDrawActivityBorder()) {
               g2D.setStroke(previousStroke);
            }

            g.setColor(style.border_dark);
            g.drawLine(11, bounds.height - 1, bounds.width - 12, bounds.height - 1);
            g.drawLine(bounds.width - 1, 11, bounds.width - 1, bounds.height - 12);
            g.drawLine(bounds.width - 12, bounds.height - 1, bounds.width - 1, bounds.height - 12);
            // Draw diagonal lines if 50% and 100% completed respectively
            activity = getDataRow();
            complete = OpGanttValidator.getComplete(activity);
            g.setColor(style.border_light);
            if (complete == 100) {
               g.drawLine(5, 5, bounds.width - 7, bounds.height - 7);
               g.drawLine(bounds.width - 7, 5, 5, bounds.height - 7);
            }
            paintChildren(g, clip_area);
            break;
      }
   }

   private void paintWBSRoot(Graphics g) {
      XStyle style = getStyleAttributes();
      Rectangle bounds = getBounds();
      XStyle activityStyle = getParent().getChild(0).getStyleAttributes();
      g.setColor(style.border_dark);
      int x = 0;
      int width = bounds.width;
      int height = bounds.height;
      g.fillRect(x + 2, activityStyle.top + 2, width - 1, height - 1);
      g.setColor(style.alternate_background);
      g.fillRect(x, activityStyle.top, width - 1, height - 1);
      g.setColor(style.border_light);
      g.drawRect(x, activityStyle.top, width - 2, height - 2);
   }

   /**
    * Does layouting for <code>WBS_CHART</code> components
    */
   private void doLayoutWBSChart() {

      // Note: Centered read-only root node (project) is directly painted w/o layouting
      XStyle wbsChartStyle = getStyleAttributes();

      // get font metrics according to wbsChartStyle
      FontMetrics metrics = getFontMetrics(wbsChartStyle.font());

      // default WBS activity height
      int wbsActivityHeight = metrics.getAscent() + metrics.getDescent();

      OpAdvancedProjectComponent box = (OpAdvancedProjectComponent) getContext();
      XComponent data_set = box.getDataSetComponent();
      Map categoryColorMap = box.categoryColorMap();

      int grid_x = getGridX();
      int grid_y;

      //grid_y represent the height of the WBS activity
      grid_y = WBS_CHART_MAX_GLYPS * wbsActivityHeight + wbsChartStyle.top + wbsChartStyle.bottom;
      List points = new ArrayList();

      // Starting point of zero outline level connectors is always the same
      Point start0 = new Point(0, wbsChartStyle.top + grid_y);
      Point turn0 = new Point(0, wbsChartStyle.gap / 2 + start0.y);
      Point start1 = null;
      Point turn1 = null;

      int vy = wbsChartStyle.top + grid_y + wbsChartStyle.gap;
      int x = wbsChartStyle.left;
      int y = wbsChartStyle.top;

      int width = 0;

      XComponent activity;
      OpProjectComponent visual;

      int number0 = 0;
      int number1 = 0;
      StringBuffer outline_number;

      String category;
      int shownDataIndex = 0;
      String rowSeparator = getRowSeparator(wbsChartStyle, grid_x);

      if (getChildCount() == 0 && data_set.getChildCount() > 0) {
         createWBSRoot(start0.x, wbsChartStyle.top, grid_x, grid_y);
      }

      for (int i = 0; i < data_set.getChildCount(); i++) {
         activity = (XComponent) (data_set.getChild(i));
         if (activity.getOutlineLevel() > 1) {
            continue;
         }

         if (!activity.getFiltered()) {
            if (this.getWBSActivityChildCount() > shownDataIndex) {
               visual = getWBSActivityChild(shownDataIndex);
            }
            else {
               visual = new OpAdvancedProjectComponent(WBS_ACTIVITY);
               _addChild(visual);
            }
            visual.setDataRow(activity);
            visual.setActivityType(OpGanttValidator.getType(activity));
            // Map category to color index
            if (categoryColorMap != null) {
               category = OpGanttValidator.getCategory(activity);
               if (category != null) {
                  // Map category choice ID to color index
                  String categoryLocator = XValidator.choiceID(category);
                  Integer colorIndex = (Integer) categoryColorMap.get(categoryLocator);
                  visual.setColorIndex(colorIndex);
               }
               else {
                  visual.setColorIndex(null);
               }
            }
            String caption = constructWBSActivityCaption(box, activity, rowSeparator);
            visual.setText(caption);


            if (activity.getOutlineLevel() == 0) {
               // Horizontal orientation
               if (shownDataIndex != 0) {
                  x += grid_x + wbsChartStyle.gap;
               }
               visual.setBounds(new Rectangle(x, vy, grid_x, grid_y));
               // Add four connector points to path
               points.add(start0);
               points.add(turn0);
               points.add(new Point(grid_x / 2 + x, -wbsChartStyle.gap / 2 + vy));
               points.add(new Point(grid_x / 2 + x, vy));
               // Init outline level 1
               start1 = new Point(x, grid_y / 2 + vy);
               turn1 = new Point(-wbsChartStyle.gap / 2 + x, grid_y / 2 + vy + (wbsChartStyle.gap / 2));
               y = vy + grid_y + wbsChartStyle.gap;
               if (width < x) {
                  width = x;
               }
               // Set outline number
               number0++;
               visual.setOutlineNumber(Integer.toString(number0));
               number1 = 1;
            }
            else if (activity.getOutlineLevel() == 1) {
               // Vertical orientation
               visual.setBounds(new Rectangle(x, y, grid_x, grid_y));
               // Add four connector points to path
               points.add(start1);
               points.add(turn1);
               points.add(new Point(-wbsChartStyle.gap / 2 + x, grid_y / 2 + y));
               points.add(new Point(x, grid_y / 2 + y));
               y += grid_y + wbsChartStyle.gap;
               // Set outline number
               outline_number = new StringBuffer();
               outline_number.append(number0);
               outline_number.append('.');
               outline_number.append(number1);
               visual.setOutlineNumber(outline_number.toString());
               number1++;
            }

            shownDataIndex++;
         }
      }
      // Set x-coordinates of start0 and turn0 points (based on width)
      width += grid_x + wbsChartStyle.right;
      start0.x = width / 2;
      turn0.x = start0.x;
      // Remove unused visuals
      if (getWBSActivityChildCount() > shownDataIndex) {
         for (int i = getWBSActivityChildCount() - 1; i >= shownDataIndex; i--) {
            XView toBeRemoved = getWBSActivityChild(i);
            XView focusedView = getDisplay().getDisplayFocusedView();
            if (focusedView == null || focusedView.equals(toBeRemoved)) {
               getDisplay().setDisplayFocusedView(getContext());
            }
            removeChild(toBeRemoved);
         }
      }
      Point[] path = new Point[points.size()];
      for (int i = 0; i < path.length; i++) {
         path[i] = (Point) (points.get(i));
      }
      setPath(path);

      if (path.length > 0) {
         updateRootPosition(path[0].x, wbsChartStyle, grid_x);
      }

      ((OpGanttValidator) box.getDataSetComponent().validator()).setProjectCost(null);
      ((OpGanttValidator) box.getDataSetComponent().validator()).setProjectEffort(null);
   }

   private void updateRootPosition(int x, XStyle style, int grid_x) {
      if (this.getChildCount() > 0) {
         XComponent root = (XComponent) this.getChild(0);
         String project_name = ((OpAdvancedProjectComponent) getContext()).getProjectName();
         root.setTooltip(project_name);
         project_name = constructProjectCaption(project_name, style, grid_x);
         root.setText(project_name);
         Rectangle bounds = new Rectangle(root.getBounds());
         bounds.x = x - grid_x / 2;
         root.setBounds(bounds);
      }
   }

   private void createWBSRoot(int startX, int y, int grid_x, int grid_y) {
      OpProjectComponent visual;//add root portfolio
      visual = new OpAdvancedProjectComponent(WBS_ROOT);
      _addChild(visual);
      XStyle style = visual.getStyleAttributes();
      String project_name = ((OpAdvancedProjectComponent) getContext()).getProjectName();
      visual.setTooltip(project_name);
      project_name = constructProjectCaption(project_name, style, grid_x);
      visual.setText(project_name);
      visual.setBounds(new Rectangle(startX - grid_x / 2, y, grid_x, grid_y));
      visual.setEnabled(false);
      //set on glyphs as well
      for (int i = 0; i < visual.getChildCount(); i++) {
         XComponent glyph = (XComponent) visual.getChild(i);
         glyph.setStyleAttributes(style);
      }
   }

   /**
    * Gets the WBS Activity child at given index. It assumes that the first child is the root project component.
    *
    * @param index
    * @return
    */
   private OpAdvancedProjectComponent getWBSActivityChild(int index) {
      return (OpAdvancedProjectComponent) getChild(index + 1);
   }

   /**
    * Returns the nr of WBS activities on this chart. (The first child is the root project)
    *
    * @return
    */
   private int getWBSActivityChildCount() {
      int children = getChildCount();
      if (children > 0) {
         children--;
      }
      return children;
   }

   /**
    * Return a line separator according to <code>Xstyle </code> and <code>width</code> of the compoent
    *
    * @param style the component style property <code> XStyle </code> instance
    * @param width the component <code>width</code>
    * @return String representing line separator
    */
   private String getRowSeparator(XStyle style, int width) {
      // get font metrics according to style
      FontMetrics metrics = getFontMetrics(style.font());
      StringBuffer lineSeparator = new StringBuffer();
      while (metrics.stringWidth(lineSeparator.toString()) + style.left + style.right < width) {
         lineSeparator.append(ROW_SEPARATOR_ELEMENT);
      }
      return lineSeparator.toString();
   }

   /**
    * Constructs the WBS activity caption for the given activity.
    *
    * @param box          WBS box
    * @param dataRow      data row of the activity
    * @param rowSeparator string to be used as row separator between caption and activity info
    * @return activity caption (lines will be delimited by LINE_SEPARATOR)
    */
   private String constructWBSActivityCaption(OpAdvancedProjectComponent box, XComponent dataRow, String rowSeparator) {
      String i18nEffortText = box.getEffortText();
      String i18nCostsText = box.getCostsText();
      String activityName = OpGanttValidator.getName(dataRow);
      StringBuffer caption = new StringBuffer(activityName == null ? "" : activityName);
      caption.append(LINE_SEPARATOR);

      caption.append(rowSeparator).append(LINE_SEPARATOR);

      double baseEffort = OpGanttValidator.getBaseEffort(dataRow);
      caption.append(i18nEffortText).append(COLON).append(XDisplay.getCalendar().localizedDoubleToString(baseEffort));
      caption.append(LINE_SEPARATOR);

      double totalCost = OpGanttValidator.getBaseExternalCosts(dataRow)
           + OpGanttValidator.getBaseMaterialCosts(dataRow)
           + OpGanttValidator.getBaseMiscellaneousCosts(dataRow)
           + OpGanttValidator.getBasePersonnelCosts(dataRow) + OpGanttValidator.getBaseTravelCosts(dataRow);
      caption.append(i18nCostsText).append(COLON).append(XDisplay.getCalendar().localizedDoubleToString(totalCost, 2));
      return caption.toString();
   }


   private int addLineTextOnGlyphs(String text, int statingGlyph, int maxNrGlyphs) {

      XStyle style = getStyleAttributes();
      FontMetrics metrics = getFontMetrics(style.font());
      Rectangle bounds = getBounds();
      int maxWidth = bounds.width - style.right - style.left;
      String space = " ";

      //split line in words
      StringTokenizer spaceTokenizer = new StringTokenizer(text, space);

      StringBuffer line = new StringBuffer();
      String glyphText;
      int line_width = 0;
      int glyphNr = 0;
      int spaceWidth = metrics.stringWidth(space);

      while (spaceTokenizer.hasMoreTokens() && glyphNr < maxNrGlyphs) {

         String word = spaceTokenizer.nextToken();
         int word_width = metrics.stringWidth(word);
         boolean widthExceeded = false;
         boolean currentWordAdded = false;
         if (line_width + word_width + spaceWidth < maxWidth) {
            line.append(word).append(space);
            line_width += word_width + spaceWidth;
            currentWordAdded = true;
         }
         else {
            widthExceeded = true;
         }

         boolean lastWord = !spaceTokenizer.hasMoreTokens();
         boolean lastGlyph = (glyphNr == maxNrGlyphs - 1);
         boolean addLastWordOnNewGlyph = false;

         if (widthExceeded || lastWord) {
            //add text on glyphs

            if (line.length() == 0) {
               //empty line, current word exceeded width
               glyphText = cutTextToWidth(style, word, maxWidth);
               currentWordAdded = true;
            }
            else {
               if (widthExceeded && lastGlyph) {
                  //try to sqeeze in some more chars from this last visible word
                  line.append(word).append(space);
                  glyphText = cutTextToWidth(style, line.toString(), maxWidth);
                  currentWordAdded = true;
               }
               else {
                  if (lastWord && widthExceeded) {
                     addLastWordOnNewGlyph = true;
                  }
                  glyphText = line.toString();
               }
            }

            //set the text on a new glyph
            glyphNr = addGlyph(statingGlyph, glyphNr, glyphText, metrics);

            //when the last word in the text won't fit in the current glyph and we still have available glyphs
            if (addLastWordOnNewGlyph) {
               if (word_width > maxWidth) {
                  glyphText = cutTextToWidth(style, word, maxWidth);
               }
               else {
                  glyphText = word;
               }
               currentWordAdded = true;
               glyphNr = addGlyph(statingGlyph, glyphNr, glyphText, metrics);
            }

            if (currentWordAdded) {
               line = new StringBuffer();
               line_width = 0;
            }
            else {
               line = new StringBuffer(word).append(space);
               line_width = word_width + spaceWidth;
            }
         }
      }

      return glyphNr;
   }

   private int addGlyph(int statingGlyph, int glyphNr, String glyphText, FontMetrics metrics) {
      OpAdvancedProjectComponent glyph;
      if (statingGlyph + glyphNr < getChildCount()) {
         glyph = (OpAdvancedProjectComponent) (_getChild(statingGlyph + glyphNr));
      }
      else {
         glyph = new OpAdvancedProjectComponent(WBS_GLYPH);
         _addChild(glyph);
      }
      glyph.setText(glyphText);
      int textWidth = metrics.stringWidth(glyphText);
      glyph.setWidth(textWidth);
      glyph.setHeight(metrics.getMaxAscent() + metrics.getLeading() + metrics.getMaxDescent());
      glyphNr++;
      return glyphNr;
   }


   /**
    * Does layouting for <code>WBS_GLYPH</code> components
    */
   private void doLayoutWBSGlyphs() {

      // First we have to wrap the text (regardless of v/h-alignment)
      Rectangle bounds = getBounds();
      String wbsActivityText = getText();
      String lineText;

      int glyph_count = 0;
      OpProjectComponent glyph;

      StringTokenizer linesTokenizer = new StringTokenizer(wbsActivityText, LINE_SEPARATOR);

      //add activity name (first line)
      String activityName = linesTokenizer.nextToken();
      int glyphNr = addLineTextOnGlyphs(activityName, 0, WBS_CHART_MAX_TEXT_GLYPS);
      glyph_count += glyphNr;

      //add activity info (max one glyph/line )
      while (linesTokenizer.hasMoreTokens()) {
         lineText = linesTokenizer.nextToken();
         glyphNr = addLineTextOnGlyphs(lineText, glyph_count, 1);
         glyph_count += glyphNr;
      }

      // Align the glyphs
      // TODO: Currently only center/center is implemented (WBS default style)
      XStyle style = getStyleAttributes();
      int height = 0;
      int i;
      for (i = 0; i < glyph_count; i++) {
         glyph = (OpProjectComponent) _getChild(i);
         height += glyph.getHeight();
      }

      // *** Center w/h inside bounds
      int x = 0;
      int y = (bounds.height - height) / 2;
      for (i = 0; i < glyph_count; i++) {
         // Center each line horizontally
         glyph = (OpProjectComponent) _getChild(i);
         x = style.left + (bounds.width - style.left - style.right - glyph.getWidth()) / 2;
         glyph.setBounds(new Rectangle(x, y, glyph.getWidth(), glyph.getHeight()));
         y += glyph.getHeight();
      }

      // Finally, remove unused components
      while (glyph_count < getChildCount()) {
         removeChild(getChildCount() - 1);
      }

   }

   /**
    * Will proces a pointer event over a WBS CHART component. The event is described by the received arguments.
    *
    * @param event
    * @param action
    * @param x
    * @param y
    * @param modifiers
    * @see XView#processPointerEvent(HashMap,int,int,int,int)
    */
   protected void processWBSPointerEvent(HashMap event, int action, int x, int y, int modifiers) {
      // get the curent drawing tool
      String toolID = DEFAULT_CURSOR;
      if (getDrawingToolId() != null) {
         toolID = getDrawingToolId();
      }
      switch (action) {
         case POINTER_DOWN: {
            if (toolID.equals(MILESTONE_DRAW_ITEM)) {
               addWBSActivity(MILESTONE_ACTIVITY, x, y);
            }
            if (toolID.equals(ACTIVITY_DRAW_ITEM)) {
               addWBSActivity(STANDARD_ACTIVITY, x, y);
            }
            if (toolID.equals(TASK_DRAW_ITEM)) {
               addWBSActivity(TASK_ACTIVITY, x, y);
            }
         }
         break;
      }
   }

   /**
    * Add an activity on a WBS chart.
    *
    * @param type
    * @param x
    * @param y
    */
   private void addWBSActivity(byte type, int x, int y) {
      int gap = getStyleAttributes().gap;
      OpProjectComponent box = (OpProjectComponent) getContext();
      XComponent dataSet = box.getDataSetComponent();
      OpGanttValidator validator = (OpGanttValidator) dataSet.validator();
      OpProjectComponent targetActivity = null;
      boolean after = false;
      int outlineLevel = 0;
      int index = 0;

      if (dataSet.getChildCount() == 0) {
         //empty data set
         index = 0;
      }
      else {

         //find the "square" in which this new activity should be placed
         Rectangle square;
         int minX = this.getBounds().x + this.getBounds().width + 10;
         int maxX = 0;
         int minY = this.getBounds().y + this.getBounds().height + 10;
         OpProjectComponent minActivity = null, maxActivity = null, lastActivity, upperActivity = null;
         for (int i = 0; i < getWBSActivityChildCount(); i++) {
            OpProjectComponent activity = getWBSActivityChild(i);
            Rectangle bounds = activity.getBounds();
            XComponent data = activity.getDataRow();
            square = new Rectangle(bounds.x - gap / 2, bounds.y - gap / 2, bounds.width + gap, bounds.height + gap);
            if (square.contains(x, y)) {
               targetActivity = activity;
               outlineLevel = data.getOutlineLevel();
               canAddAsSibling(targetActivity, type, outlineLevel, validator);
               break;
            }
            //find min, max & top activity from parent level in wbs chart
            if (data != null && data.getOutlineLevel() == PARENT_WBS_OUTLINE) {
               if (bounds.x < minX) {
                  minX = bounds.x;
                  minActivity = activity;
               }
               if (bounds.x > maxX) {
                  maxX = bounds.x + bounds.width;
                  maxActivity = activity;
               }
               if (x > bounds.x - gap / 2 && x < bounds.x + bounds.width + gap / 2 &&
                    bounds.y < minY) {
                  minY = bounds.y;
                  upperActivity = activity;
               }
            }
         }

         //if target is still null try to add it up, down, left or right
         if (targetActivity == null) {
            if (x > maxX) {
               //right
               targetActivity = maxActivity;
               after = true;
               index += targetActivity.getDataRow().getSubRows().size();
               outlineLevel = targetActivity.getDataRow().getOutlineLevel();
            }
            else if (x < minX) {
               //left
               targetActivity = minActivity;
               outlineLevel = targetActivity.getDataRow().getOutlineLevel();
            }
            else if (upperActivity != null && y < minY) {
               //up
               targetActivity = upperActivity;
               outlineLevel = targetActivity.getDataRow().getOutlineLevel();
            }
            else {
               //down
               lastActivity = getLastActivityOnColumn(this, x, dataSet, null);
               if (y > lastActivity.getBounds().y) {
                  targetActivity = lastActivity;
                  XComponent targetData = targetActivity.getDataRow();
                  if (targetData.getOutlineLevel() == PARENT_WBS_OUTLINE && OpGanttValidator.getType(targetData) == OpGanttValidator.MILESTONE) {
                     throw new XValidationException(OpGanttValidator.MILESTONE_COLLECTION_EXCEPTION);
                  }
                  //only tasks can be "newly" added under another task activity
                  if (targetData.getOutlineLevel() == PARENT_WBS_OUTLINE && OpGanttValidator.getType(targetData) == OpGanttValidator.TASK
                       && type != OpGanttValidator.TASK) {
                     throw new XValidationException(OpGanttValidator.SCHEDULED_MIXED_EXCEPTION);
                  }
                  after = true;
                  outlineLevel = CHILD_WBS_OUTLINE;
               }
            }
         }
         XComponent targetData = targetActivity.getDataRow();
         index += targetData.getIndex();
      }

      if (after) {
         index++;
         canAddAsSibling(targetActivity, type, outlineLevel, validator);
      }

      XComponent row = dataSet.newDataRow();
      Date start = OpGanttValidator.getStart(row);
      Date end = OpGanttValidator.getEnd(row);
      row.setOutlineLevel(outlineLevel);

      if (type == MILESTONE_ACTIVITY) {
         //milestone
         OpGanttValidator.setStart(row, start);
         validator.updateType(row, OpGanttValidator.MILESTONE);
      }
      else if (type == TASK_ACTIVITY) {
         //task
         validator.updateType(row, OpGanttValidator.TASK);
      }
      else if (type == STANDARD_ACTIVITY) {
         OpGanttValidator.setStart(row, start);
         OpGanttValidator.setEnd(row, end);
         validator.updateType(row, OpGanttValidator.STANDARD);
         validator.updateFinish(row, end);
      }

      //add data row & validate
      dataSet.addDataRow(index, row);

      //update UI
      getContext().doLayout();
      OpAdvancedProjectComponent visual = (OpAdvancedProjectComponent) getVisualForData(row);
      visual.selectWBSActivity();
      getContext().repaint();
   }

   /**
    * Checks if the given activity (type/outlinelevel) can be added after the target activity.
    *
    * @param targetActivity Target activity - the new activity will be added after this activity.
    * @param type           Type of the new activity.
    * @param outlineLevel   Outline level of the new activity.
    * @param validator      Validator instance used in the add precess.
    */
   private void canAddAsSibling(OpProjectComponent targetActivity, byte type, int outlineLevel, OpGanttValidator validator) {
      if (outlineLevel != PARENT_WBS_OUTLINE) {
         XComponent targetActivityRow = targetActivity.getDataRow();
         XComponent superActivity = validator.superActivity(targetActivityRow);
         if (superActivity != null) {
            byte superActivityType = OpGanttValidator.getType(superActivity);
            if (superActivityType == OpGanttValidator.SCHEDULED_TASK
                 && type != OpGanttValidator.TASK && type != OpGanttValidator.COLLECTION_TASK) {
               throw new XValidationException(OpGanttValidator.SCHEDULED_MIXED_EXCEPTION);
            }
            List subActivities = validator.subActivities(superActivity);
            if (type == OpGanttValidator.TASK && subActivities.size() != 0) {
               throw new XValidationException(OpGanttValidator.SCHEDULED_MIXED_EXCEPTION);
            }
            List subTasks = validator.subTasks(superActivity);
            if (type != OpGanttValidator.TASK && subTasks.size() != 0) {
               throw new XValidationException(OpGanttValidator.SCHEDULED_MIXED_EXCEPTION);
            }
         }
      }
   }


   /**
    * Moves the WBS activity (context) at the given (x,y) position.
    */
   private void moveActivityOnWBS() {

      OpProjectComponent box = (OpProjectComponent) getContext();
      OpGanttValidator validator = (OpGanttValidator) (box.getDataSetComponent().validator());

      int middleX = getBounds().x + getBounds().width / 2;
      int middleY = getBounds().y + getBounds().height / 2;

      List result = determineWBSPosition(middleX, middleY);
      OpProjectComponent destination = (OpProjectComponent) result.get(0);
      boolean moveAfterTarget = ((Boolean) result.get(1)).booleanValue();

      // do the moving
      if (destination != null && validator != null) {
         XComponent sourceDataRow = this.getDataRow();
         XComponent targetDataRow = destination.getDataRow();
         if (sourceDataRow.getOutlineLevel() >= targetDataRow.getOutlineLevel()) {
            ArrayList rows = new ArrayList();
            rows.add(sourceDataRow);

            // add all the children for the sourceDataRow
            ArrayList children = validator.getChildren(sourceDataRow);
            rows.addAll(children);
            int offset = validator.getMovingOffsetForComponents(sourceDataRow, targetDataRow, moveAfterTarget);

            // move the rows
            XComponent dataSet = (XComponent) sourceDataRow.getParent();
            dataSet.moveDataRows(rows, offset);
         }
      }
   }

   /**
    * Performs <code>WBS_ACTIVITY</code> selection and data synchronizing.
    * Must be called on a <code>WBS_ACTIVITY</code> component.
    *
    * @throws UnsupportedOperationException if method isn't called on a <code>WBS_ACTIVITY</code> component
    */
   private void selectWBSActivity() {
      if (pc_type == WBS_ACTIVITY) {
         requestFocus();
         getDisplay().setDragSource(this);
         // set the dragged prop on the wbs chart.
         ((OpProjectComponent) getParent()).setDraggedComponent(this);
         // get the data row
         XComponent dataRow = this.getDataRow();
         XComponent dataSet = (XComponent) dataRow.getParent();
         dataSet.clearDataSelection();//for multiple selection (based on modifiers) this must not be called
         dataRow.setSelected(true);
      }
      else {
         throw new UnsupportedOperationException("The method selectWBSActivity cannot be called for this component type");
      }

   }


   /**
    * Computes the new position of a WBS activity component in a WBS chart based on the given coordinates.
    *
    * @param middleX middle x position of the WBS activity component
    * @param middleY middle y position of the WBS activity component
    * @return List that contains on position 0 a <code>OpProjectComponent</code> the destination component and on
    *         position 1 a <code>Boolean</code> that is true if the new position is after the given destination.
    */
   private List determineWBSPosition(int middleX, int middleY) {

      // the dragged activity can be dropped after or before the target
      boolean moveAfterTarget = false;

      XComponent dataSet = (XComponent) this.getDataRow().getParent();
      OpAdvancedProjectComponent box = (OpAdvancedProjectComponent) getContext();

      OpAdvancedProjectComponent chart = (OpAdvancedProjectComponent) box.getBoxContent();
      OpProjectComponent destination = null;
      List result = new ArrayList();
      List components = chart.findAll(middleX, middleY, this);
      for (int i = 0; i < components.size(); i++) {
         XView xView = (XView) components.get(i);
         if (xView instanceof OpAdvancedProjectComponent && ((XComponent) xView).getComponentType() == WBS_ACTIVITY) {
            destination = (OpProjectComponent) xView;
            if (destination.getDataRow().getOutlineLevel() == PARENT_WBS_OUTLINE &&
                 this.getDataRow().getOutlineLevel() == CHILD_WBS_OUTLINE) {
               moveAfterTarget = true;
            }
            break;
         }
      }

      // determines if the activity must be dropped at the "left" or "right" of the chart
      // compute min and max X for the activities in the WBS chart
      if (destination == null) {
         int minX = chart.getBounds().x + chart.getBounds().width + 10;
         int maxX = 0;
         OpAdvancedProjectComponent minActivity = null, maxActivity = null;
         for (int index = 0; index < chart.getWBSActivityChildCount(); index++) {
            OpAdvancedProjectComponent activity = chart.getWBSActivityChild(index);
            // other activities than the dragged one
            if (activity != this && activity.getComponentType() == WBS_ACTIVITY) {
               XComponent data = activity.getDataRow();
               if (data != null && data.getOutlineLevel() == PARENT_WBS_OUTLINE) {
                  if (activity.getBounds().x < minX) {
                     minX = activity.getBounds().x;
                     minActivity = activity;
                  }
                  if (activity.getBounds().x > maxX) {
                     maxX = activity.getBounds().x;
                     maxActivity = activity;
                  }
               }
            }
         }
         if (middleX < minX) {
            destination = minActivity;
         }
         if (middleX > maxX) {
            destination = maxActivity;
            moveAfterTarget = true;
         }
      }

      // determine if we have to move the activity at the bottom
      if (destination == null && this.getDataRow().getOutlineLevel() != PARENT_WBS_OUTLINE) {
         int maxY = 0;
         OpProjectComponent afterActivity = null;
         OpProjectComponent activity = getLastActivityOnColumn(chart, middleX, dataSet, this);

         if (activity != null) {
            maxY = activity.getBounds().y + activity.getBounds().height;
            //if the dragged activity is bellow max y - add it after "activity"
            if (middleY > maxY) {
               afterActivity = activity;
            }
         }

         destination = afterActivity;
         moveAfterTarget = true;
      }


      result.add(destination);
      result.add(Boolean.valueOf(moveAfterTarget));
      return result;
   }

   /**
    * @param chart    wbs chart that contains the activities
    * @param middleX
    * @param dataSet
    * @param excluded
    * @return Last activity on the column gevin by middleX in a WBS chart
    */
   private OpProjectComponent getLastActivityOnColumn(OpAdvancedProjectComponent chart, int middleX, XComponent dataSet, OpProjectComponent excluded) {
      OpProjectComponent activity = null;
      XComponent dataRow;

      //search for the component after wich this must be added
      for (int index = 0; index < chart.getWBSActivityChildCount(); index++) {
         OpProjectComponent child = chart.getWBSActivityChild(index);
         if (child == excluded) {
            continue;
         }
         activity = child;
         if (activity.getDataRow().getOutlineLevel() == PARENT_WBS_OUTLINE) {
            //if dragged activity (this) on this column
            if (middleX >= activity.getBounds().x && middleX < activity.getBounds().x + activity.getBounds().width) {
               //determine last y on this column (last children of this component)
               XComponent targetDataRow = activity.getDataRow();
               int dataIndex = targetDataRow.getIndex() + 1;
               while (dataIndex < dataSet.getChildCount()) {
                  //get current data row
                  dataRow = (XComponent) dataSet.getChild(dataIndex);
                  if (dataRow.getOutlineLevel() == PARENT_WBS_OUTLINE) {
                     break;
                  }
                  if (dataRow.getOutlineLevel() == CHILD_WBS_OUTLINE) {
                     targetDataRow = dataRow;
                  }
                  dataIndex++;
               }
               if (targetDataRow != activity.getDataRow()) {
                  activity = chart.getVisualForData(targetDataRow);
               }
               break;
            }
         }
      }
      return activity;
   }


   public void processPointerEvent(HashMap event, int action, int x, int y, int modifiers) {
      logger.debug("OpAdvancedProjectComponent.processPointerEvent");
      switch (pc_type) {
         case WBS_ACTIVITY:
            processWBSActivityPointerEvent(action, x, y);
            break;
         case WBS_CHART:
            super.processPointerEvent(event, action, x, y, modifiers);
            try {
               processWBSPointerEvent(event, action, x, y, modifiers);
            }
            catch (XValidationException e) {
               getContext().requestFocus();
               XComponent form = getContext().getForm();
               form.showValidationException(e);
            }
            break;
         case WBS_BOX: {
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
    * Process a pointer event on a WBS_ACTIVITY. Here it is handled the focus and draging of a WBS_ACTIVITY.
    *
    * @param action type of action (POINTER_DOWN, POINTER_DRAG, etc.)
    * @param x      x location
    * @param y      y location
    */
   private void processWBSActivityPointerEvent(int action, int x, int y) {
      OpAdvancedProjectComponent chart = ((OpAdvancedProjectComponent) getParent());
      switch (action) {
         // for a wbs activity show details on double click
         case POINTER_DOUBLE_TAP:
            OpProjectComponent wbs_box = (OpProjectComponent) getContext();
            String handler = wbs_box.getOnActivityDetails();
            if (handler != null) {
               // select data row
               XComponent dataRow = getDataRow();
               XComponent dataSet = (XComponent) dataRow.getParent();
               dataSet.clearDataSelection();
               dataRow.setSelected(true);
               // activity loses focus
               setFocused(false);
               repaint();
               // invoke handler
               wbs_box.getForm().invokeFunction(handler, null);
            }
            break;
         case POINTER_DOWN:
            selectWBSActivity();
            break;
         case POINTER_UP:
            getDisplay().setDragSource(null);
            // set the dragged prop on the wbs chart.
            ((OpProjectComponent) getParent()).setDraggedComponent(null);
            break;
         case POINTER_DRAG:
            Point drag_position = getDragPosition();
            if (drag_position != null) {
               Rectangle moved_bounds = getBounds();
               moved_bounds.x += x - drag_position.x;
               moved_bounds.y += y - drag_position.y;
               x -= x - drag_position.x;
               y -= y - drag_position.y;
               setBounds(moved_bounds);
               chart.computeInsertLine();
               getParent().repaint();
            }
            setDragPosition(new Point(x, y));
            _scrollToPosition(x, y);
            break;
         case POINTER_DRAG_END:

            if (getDragPosition() != null) {
               ((OpProjectComponent) getParent()).setDraggedComponent(null);
               setDragPosition(null);
               chart.setInsertLine(null);
               //all activities get the drawborder null
               for (int i = 0; i < chart.getWBSActivityChildCount(); i++) {
                  OpAdvancedProjectComponent activity = chart.getWBSActivityChild(i);
                  activity.setDrawActivityBorder(false);
               }
               OpProjectComponent box = (OpProjectComponent) getContext();
               this.moveActivityOnWBS();
               box.doLayout();
               box.repaint();
               box.requestFocus();
            }
            break;
      }
   }

   /**
    * computes and sets the insert line for the chart component.
    */
   private void computeInsertLine() {

      XComponent dataSet;
      setInsertLine(null);
      XStyle style = getStyleAttributes();
      int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
      OpAdvancedProjectComponent draggedWbs = (OpAdvancedProjectComponent) getDraggedComponent();
      if (draggedWbs != null) {
         XComponent draggedDataRow = draggedWbs.getDataRow();

         Point point = draggedWbs.getDragPosition();
         if (point != null) {
            int middleX = draggedWbs.getBounds().x + draggedWbs.getBounds().width / 2;
            int middleY = draggedWbs.getBounds().y + draggedWbs.getBounds().height / 2;
            List result = draggedWbs.determineWBSPosition(middleX, middleY);
            OpProjectComponent target = (OpProjectComponent) result.get(0);
            boolean afterTargetVal = ((Boolean) result.get(1)).booleanValue();

            if (target != null) {
               Rectangle targetBounds = target.getBounds();
               XComponent targetDataRow = target.getDataRow();
               if ((afterTargetVal && targetDataRow.getIndex() + 1 == draggedDataRow.getIndex())
                    || (!afterTargetVal && targetDataRow.getIndex() - 1 == draggedDataRow.getIndex())) {
                  setInsertLine(null);
                  return;
               }
               dataSet = (XComponent) targetDataRow.getParent();

               //1.dragged and target have both outline level 0
               if (targetDataRow.getOutlineLevel() == 0 && draggedDataRow.getOutlineLevel() == 0) {
                  if (afterTargetVal) {
                     x1 = targetBounds.x + targetBounds.width + style.gap / 2;
                     x2 = x1;
                     y1 = 0;
                     y2 = getBounds().height;
                  }
                  else {
                     x1 = targetBounds.x - style.gap / 2;
                     x2 = x1;
                     y1 = 0;
                     y2 = getBounds().height;
                  }
               }

               //2.dragged with outline level 1, target with outline level 0 or 1 (only 0 or 1 allowed now in WBS charts)
               if (draggedDataRow.getOutlineLevel() == 1) {
                  OpGanttValidator validator = (OpGanttValidator) (getContext().getDataSetComponent().validator());
                  if (afterTargetVal) {
                     x1 = 0;
                     x2 = getBounds().width;
                     y1 = targetBounds.y + targetBounds.height + style.gap / 2;
                     y2 = y1;
                  }
                  else {
                     x1 = 0;
                     x2 = getBounds().width;
                     y1 = targetBounds.y - style.gap / 2;
                     y2 = y1;
                     if (targetDataRow.getIndex() - 1 >= 0) {
                        targetDataRow = (XComponent) dataSet.getChild(targetDataRow.getIndex() - 1);
                     }
                  }
                  XComponent collectionData;
                  if (targetDataRow.getOutlineLevel() != 0) {
                     collectionData = validator.superActivity(targetDataRow);
                  }
                  else {
                     collectionData = targetDataRow;
                  }
                  //find component that has this data attached
                  for (int i = 0; i < getWBSActivityChildCount(); i++) {
                     OpAdvancedProjectComponent component = getWBSActivityChild(i);
                     component.setDrawActivityBorder(false);
                     if (component.getDataRow() == collectionData) {
                        component.setDrawActivityBorder(true);
                     }
                  }
               }
               setInsertLine(new Line2D.Double(x1, y1, x2, y2));
            }
         }
      }
   }


   public void processKeyboardEvent(HashMap event, int action, int key_code, char key_char, int modifiers) {
      logger.debug("OpAdvancedProjectComponent.processKeyboardEvent");
      switch (pc_type) {
         case WBS_BOX:
            processUndoRedoKeyboardEvent(action, key_code, modifiers);
            break;
         case WBS_ACTIVITY:
            processWBSActivityKeyboardEvent(event, action, key_code, key_char, modifiers);
            break;
         default:
            super.processKeyboardEvent(event, action, key_code, key_char, modifiers);
      }
   }

   private OpAdvancedProjectComponent nextWBSActivitySibling() {
      OpProjectComponent sibling = (OpProjectComponent) this.nextSibling();
      while (sibling != null && sibling.getComponentType() != WBS_ACTIVITY) {
         sibling = (OpProjectComponent) sibling.nextSibling();
      }
      return (OpAdvancedProjectComponent) sibling;
   }

   private OpAdvancedProjectComponent previousWBSActivitySibling() {
      OpProjectComponent sibling = (OpProjectComponent) this.previousSibling();
      while (sibling != null && sibling.getComponentType() != WBS_ACTIVITY) {
         sibling = (OpProjectComponent) sibling.previousSibling();
      }
      return (OpAdvancedProjectComponent) sibling;
   }


   private void processWBSActivityKeyboardEvent(HashMap event, int action, int key_code, char key_char, int modifiers) {
      if (action == KEY_DOWN) {
         OpProjectComponent box = (OpAdvancedProjectComponent) getContext();
         switch (key_code) {
            case DELETE_KEY:
               logger.debug("WBS-DEL");
               // Remove activity from chart
               OpAdvancedProjectComponent chart = (OpAdvancedProjectComponent) box.getBoxContent();
               chart.removeActivity(this);
               box.doLayout();
               box.repaint();
               break;
               // TODO: Tab focus management (in activity outline-number order)
            case CURSOR_RIGHT_KEY:
               if (getDataRow().getOutlineLevel() == 0) {
                  // Go to next data-row with PARENT outline-level
                  OpAdvancedProjectComponent next_visual = nextWBSActivitySibling();
                  while ((next_visual != null) && next_visual.getDataRow() != null &&
                       (next_visual.getDataRow().getOutlineLevel() != PARENT_WBS_OUTLINE)) {
                     next_visual = next_visual.nextWBSActivitySibling();
                  }
                  if (next_visual != null) {
                     next_visual.requestFocus();
                  }
               }
               else {
                  // Count back until data-row's outline-level is PARENT; find next PARENT-level
                  int count = 1;
                  OpAdvancedProjectComponent previous_visual = previousWBSActivitySibling();
                  while (previous_visual.getDataRow().getOutlineLevel() == CHILD_WBS_OUTLINE) {
                     count++;
                     previous_visual = previous_visual.previousWBSActivitySibling();
                  }
                  // Find next data-row with outline-level 0
                  OpAdvancedProjectComponent next_visual = previous_visual.nextWBSActivitySibling();
                  while ((next_visual != null) && (next_visual.getDataRow().getOutlineLevel() != PARENT_WBS_OUTLINE)) {
                     next_visual = next_visual.nextWBSActivitySibling();
                  }
                  // Try to count forward same number of outline-level 1 as up before
                  if (next_visual != null) {
                     int i = 0;
                     previous_visual = next_visual;
                     next_visual = next_visual.nextWBSActivitySibling();
                     while ((next_visual != null) && (next_visual.getDataRow().getOutlineLevel() == CHILD_WBS_OUTLINE)) {
                        i++;
                        if (i == count) {
                           break;
                        }
                        previous_visual = next_visual;
                        next_visual = next_visual.nextWBSActivitySibling();
                     }
                     if ((next_visual != null) && (next_visual.getDataRow().getOutlineLevel() == CHILD_WBS_OUTLINE)) {
                        next_visual.requestFocus();
                     }
                     else {
                        previous_visual.requestFocus();
                     }
                  }
               }
               break;
            case CURSOR_LEFT_KEY:
               if (getDataRow().getOutlineLevel() == 0) {
                  // Go to previous data-row w/outline-level of 0
                  OpAdvancedProjectComponent previous_visual = previousWBSActivitySibling();
                  while ((previous_visual != null) && previous_visual.getDataRow() != null &&
                       (previous_visual.getDataRow().getOutlineLevel() != PARENT_WBS_OUTLINE)) {
                     previous_visual = previous_visual.previousWBSActivitySibling();
                  }
                  if (previous_visual != null) {
                     previous_visual.requestFocus();
                  }
               }
               else {
                  // Count back until data-row's outline-level is 0; find
                  // previous 0-level
                  int count = 1;
                  OpAdvancedProjectComponent previous_visual = previousWBSActivitySibling();
                  while (previous_visual.getDataRow().getOutlineLevel() == 1) {
                     count++;
                     previous_visual = previous_visual.previousWBSActivitySibling();
                  }
                  // Find previous data-row with outline-level 0
                  previous_visual = previous_visual.previousWBSActivitySibling();
                  while ((previous_visual != null) && (previous_visual.getDataRow().getOutlineLevel() != PARENT_WBS_OUTLINE)) {
                     previous_visual = previous_visual.previousWBSActivitySibling();
                  }
                  // Try to count forward same number of outline-level 1 as up
                  // before
                  if (previous_visual != null) {
                     int i = 0;
                     OpAdvancedProjectComponent next_visual = previous_visual.nextWBSActivitySibling();
                     previous_visual = next_visual;
                     while ((next_visual != null) && (next_visual.getDataRow().getOutlineLevel() == CHILD_WBS_OUTLINE)) {
                        i++;
                        if (i == count) {
                           break;
                        }
                        previous_visual = next_visual;
                        next_visual = next_visual.nextWBSActivitySibling();
                     }
                     if ((next_visual != null) && (next_visual.getDataRow().getOutlineLevel() == CHILD_WBS_OUTLINE)) {
                        next_visual.requestFocus();
                     }
                     else {
                        previous_visual.requestFocus();
                     }
                  }
               }
               break;
            case CURSOR_DOWN_KEY:
               // Go down unless next data-row's outline-level is not 1
               OpAdvancedProjectComponent next_visual = nextWBSActivitySibling();
               if ((next_visual != null) && (next_visual.getDataRow() != null) &&
                    (next_visual.getDataRow().getOutlineLevel() == CHILD_WBS_OUTLINE)) {
                  next_visual.requestFocus();
               }
               break;
            case CURSOR_UP_KEY:
               // Go up unless this data-row's outline-level is not 1
               if (getDataRow().getOutlineLevel() == CHILD_WBS_OUTLINE) {
                  previousSibling().requestFocus();
               }
               break;
            case C_KEY:
               if ((modifiers & CTRL_KEY_DOWN) == CTRL_KEY_DOWN) {
                  String scriptAction = box.getOnCopy();
                  this.getForm().invokeFunction(scriptAction, null);
               }
               break;
            case X_KEY:
               if ((modifiers & CTRL_KEY_DOWN) == CTRL_KEY_DOWN) {
                  String scriptAction = box.getOnCut();
                  this.getForm().invokeFunction(scriptAction, null);
               }
               break;
            case V_KEY:
               if ((modifiers & CTRL_KEY_DOWN) == CTRL_KEY_DOWN) {
                  String scriptAction = box.getOnPaste();
                  this.getForm().invokeFunction(scriptAction, null);
               }
               break;
               // TODO: Maybe support ctrl-1 to ctrl-0 for setting complete
         }
      }
      else if (action == KEY_TYPED) {
         if ((key_char != (char) 127) && (key_char != (char) 9) && (int) key_char != ENTER_KEY &&
              (modifiers & CTRL_KEY_DOWN) != CTRL_KEY_DOWN) {
            // Open caption-editor
            _openCaptionEditor(OpGanttValidator.getName(getDataRow()));
            // get the caption editor
            XComponent captionEditor = _getCaptionEditor();
            // process the caption editor keyboard event
            captionEditor.processKeyboardEvent(event, action, key_code, key_char, modifiers);
         }
      }
   }


   public void processFocusEvent(HashMap event, int action) {
      logger.debug("OpAdvancedProjectComponent.processFocusEvent");
      switch (pc_type) {
         case WBS_ACTIVITY:
            if (action == FOCUS_GAINED) {
               scrollToComponent();
            }
            break;
         default:
            super.processFocusEvent(event, action);
      }
   }


   public void processComponentEvent(HashMap event, int action) { // Process
      logger.debug("OpAdvancedProjectComponent.processComponentEvent");
      switch (pc_type) {
         case WBS_BOX:
            if (action == STATUS_CHANGED) {
               //clear data selection
               XComponent dataSet = this.getDataSetComponent();
               if (dataSet != null) {
                  dataSet.clearDataSelection();
               }
            }
            processScrollBoxComponentEvent(event, action);
            super.processComponentEvent(event, action);
            return;
         case WBS_ACTIVITY:
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
               OpAdvancedProjectComponent chart = (OpAdvancedProjectComponent) getParent();
               if (chart != null) {
                  chart.resetCached();
               }
               //close caption editor and caption owner(this) becomes the focused component
               _closeCaptionEditor();
               requestFocus();
               getContext().doLayout();
               getContext().repaint();
            }
            return;
         default:
            super.processComponentEvent(event, action);
      }
   }

   protected void openTooltipPopup(HashMap event) {
      if (getContext().getDetailsFormRef() != null) {
         int mouseX = ((Integer) event.get(X)).intValue();
         int mouseY = ((Integer) event.get(Y)).intValue();
         switch (pc_type) {
            case WBS_ROOT:
               super.openTooltipPopup(event);
               break;
            case WBS_ACTIVITY:
               openActivityTooltip(mouseX, mouseY);
               break;
            default:
               super.openTooltipPopup(event);
         }
      }
      else {
         super.openTooltipPopup(event);
      }
   }

   /**
    * Set the cursor for this component. Extended behavior for project components.
    *
    * @param cursor The new cursor for this component.
    */
   protected void setMouseCursor(Cursor cursor) {
      super.setMouseCursor(cursor);
      switch (pc_type) {
         case WBS_ACTIVITY:
         case WBS_CHART:
            //set also for all children
            for (int i = 0; i < getChildCount(); i++) {
               OpAdvancedProjectComponent child = (OpAdvancedProjectComponent) getChild(i);
               child.setMouseCursor(cursor);
            }
            break;
      }
   }
}
