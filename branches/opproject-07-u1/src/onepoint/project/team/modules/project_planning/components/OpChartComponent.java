/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_planning.components;

import onepoint.express.*;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.util.XCalendar;

import java.awt.*;
import java.sql.Date;
import java.util.*;
import java.util.List;

/**
 * Chart component class.
 *
 * @author gmesaric
 */
public class OpChartComponent extends XComponent {

   private static final XLog logger = XLogFactory.getLogger(OpChartComponent.class);

   // Shape types
   public final static byte LINE_CHART_HEADER = 1;
   public final static byte LINE_CHART_SIDEBAR = 2;
   public final static byte LINE_CHART = 3;
   public final static byte LINE_CHART_BOX = 4;
   public final static byte BAR = 5;
   public final static byte BAR_CHART_HEADER = 6;
   public final static byte BAR_CHART_SIDEBAR = 7;
   public final static byte BAR_CHART = 8;
   public final static byte BAR_CHART_BOX = 9;
   public final static byte PIPELINE_ELEMENT = 10;
   public final static byte PIPELINE_CHART_FOOTER = 11;
   public final static byte PIPELINE_CHART = 13;
   public final static byte PIPELINE_CHART_BOX = 14;
   // Properties
   public final static Integer GRID_X = new Integer(256);
   public final static Integer GRID_Y = new Integer(257);
   public final static Integer DATA_UNIT = new Integer(258); // (ValueUnit?)
   public final static Integer GRID_LINES = new Integer(259);
   public final static Integer MARKERS = new Integer(260); // LineChartBox

   public final static Integer DATA_CAPTION_COLUMN_INDEX = new Integer(263);
   public final static Integer PATHS_X = new Integer(264); // Transient; LineChart
   public final static Integer PATHS_Y = new Integer(265); // Transient; LineChart
   public final static Integer DISTRIBUTION = new Integer(266); // Transient; BarChart
   public final static Integer DATA_COLUMN_COUNT = new Integer(267);
   public final static Integer COLOR_CAPTIONS = new Integer(270); // Bar/LineChartBox
   public final static Integer OFFSET_X = new Integer(271); // Transient; Bar/LineChart
   public final static Integer MAX_VALUE = new Integer(272); // Transient; Bar/LineChart
   public final static Integer MIN_VALUE = new Integer(273); // Transient; Bar/LineChart
   public final static Integer VALUE_CAPTIONS = new Integer(274); // Bar/LineChart(Box)
   public final static Integer PIPELINE_ELEMENT_HEIGHT = new Integer(275); // PipelineChart
   public final static Integer PIPELINE_GAP = new Integer(278); // PipelineChart
   public final static Integer PIPELINE_ELEMENT_FONT = new Integer(279); // PipelineChart
   public final static Integer CHART_OVERVIEW_STATE = new Integer(280); // if TRUE, chart is in zoomOut mode, if false it is in default view mode

   // Grid line options
   public final static byte NONE = 0;
   public final static byte HORIZONTAL = 1;
   public final static byte VERTICAL = 2;
   public final static byte ALL = 3;

   //footer index
   public final static byte PIPELINE_FOOTER_INDEX = 3;

   // Default styles
   public final static String DEFAULT_LINE_CHART_HEADER_STYLE = "line-chart-header-default";
   public final static String DEFAULT_LINE_CHART_SIDEBAR_STYLE = "line-chart-sidebar-default";
   public final static String DEFAULT_LINE_CHART_STYLE = "line-chart-default";
   public final static String DEFAULT_LINE_CHART_BOX_STYLE = "line-chart-box-default";
   public final static String DEFAULT_BAR_STYLE = "bar-default";
   public final static String DEFAULT_BAR_CHART_HEADER_STYLE = "bar-chart-header-default";
   public final static String DEFAULT_BAR_CHART_SIDEBAR_STYLE = "bar-chart-sidebar-default";
   public final static String DEFAULT_BAR_CHART_STYLE = "bar-chart-default";
   public final static String DEFAULT_BAR_CHART_BOX_STYLE = "bar-chart-box-default";

   public final static String DEFAULT_PIPELINE_ELEMENT_STYLE = "pipeline-element-default";
   public final static String DEFAULT_PIPELINE_CHART_HEADER_STYLE = "pipeline-chart-header-default";
   public final static String DEFAULT_PIPELINE_CHART_STYLE = "pipeline-chart-default";
   public final static String DEFAULT_PIPELINE_CHART_BOX_STYLE = "bar-chart-box-default";

   public final static XStyle DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_LINE_CHART_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_BAR_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_BAR_CHART_HEADER_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_BAR_CHART_SIDEBAR_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_BAR_CHART_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_BAR_CHART_BOX_STYLE_ATTRIBUTES;

   public final static XStyle DEFAULT_PIPELINE_CHART_HEADER_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_PIPELINE_CHART_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_PIPELINE_CHART_BOX_STYLE_ATTRIBUTES;
   public final static XStyle DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES;

   static {
      // Default LineChartHeader style
      DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES.alignment_x = XStyle.LEFT;
      DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES.alignment_y = XStyle.TOP;
      DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES.left = 2;
      DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES.top = 2;
      DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES.right = 2;
      DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES.bottom = 2;
      DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES.gap = 2;
      addDefaultStyle(DEFAULT_LINE_CHART_HEADER_STYLE, DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES);
      // Default LineChartSidebar style
      DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES.alignment_x = XStyle.LEFT;
      DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES.alignment_y = XStyle.TOP;
      DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES.left = 0;
      DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES.top = 0;
      DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES.right = 2;
      DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES.bottom = 0;
      DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES.gap = 0;
      addDefaultStyle(DEFAULT_LINE_CHART_SIDEBAR_STYLE, DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES);
      // Default LineChart style
      DEFAULT_LINE_CHART_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_LINE_CHART_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_LINE_CHART_STYLE_ATTRIBUTES.left = 0;
      DEFAULT_LINE_CHART_STYLE_ATTRIBUTES.top = 0;
      DEFAULT_LINE_CHART_STYLE_ATTRIBUTES.right = 0;
      DEFAULT_LINE_CHART_STYLE_ATTRIBUTES.bottom = 0;
      DEFAULT_LINE_CHART_STYLE_ATTRIBUTES.gap = 0;
      addDefaultStyle(DEFAULT_LINE_CHART_STYLE, DEFAULT_LINE_CHART_STYLE_ATTRIBUTES);
      // Default LineChartBox style
      DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES.alignment_x = XStyle.FILL;
      DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES.alignment_y = XStyle.FILL;
      DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES.left = 1;
      DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES.top = 1;
      DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES.right = 1;
      DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES.bottom = 1;
      DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES.gap = 0;
      addDefaultStyle(DEFAULT_LINE_CHART_BOX_STYLE, DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES);

      // Default Bar style
      DEFAULT_BAR_STYLE_ATTRIBUTES = new XStyle();
      DEFAULT_BAR_STYLE_ATTRIBUTES.background = XStyle.DEFAULT_FIELD;
      DEFAULT_BAR_STYLE_ATTRIBUTES.left = 0;
      DEFAULT_BAR_STYLE_ATTRIBUTES.top = 0;
      DEFAULT_BAR_STYLE_ATTRIBUTES.right = 0;
      DEFAULT_BAR_STYLE_ATTRIBUTES.bottom = 0;
      DEFAULT_BAR_STYLE_ATTRIBUTES.gap = 0;
      addDefaultStyle(DEFAULT_BAR_STYLE, DEFAULT_BAR_STYLE_ATTRIBUTES);

      // Default BarChartHeader style
      DEFAULT_BAR_CHART_HEADER_STYLE_ATTRIBUTES = new XStyle(DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES);
      addDefaultStyle(DEFAULT_BAR_CHART_HEADER_STYLE, DEFAULT_BAR_CHART_HEADER_STYLE_ATTRIBUTES);
      // Default BarChartSidebar style
      DEFAULT_BAR_CHART_SIDEBAR_STYLE_ATTRIBUTES = new XStyle(DEFAULT_LINE_CHART_SIDEBAR_STYLE_ATTRIBUTES);
      addDefaultStyle(DEFAULT_BAR_CHART_SIDEBAR_STYLE, DEFAULT_BAR_CHART_SIDEBAR_STYLE_ATTRIBUTES);
      // Default BarChart style
      DEFAULT_BAR_CHART_STYLE_ATTRIBUTES = new XStyle(DEFAULT_LINE_CHART_STYLE_ATTRIBUTES);
      addDefaultStyle(DEFAULT_BAR_CHART_STYLE, DEFAULT_BAR_CHART_STYLE_ATTRIBUTES);
      // Default LineChartBox style
      DEFAULT_BAR_CHART_BOX_STYLE_ATTRIBUTES = new XStyle(DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES);
      addDefaultStyle(DEFAULT_BAR_CHART_BOX_STYLE, DEFAULT_BAR_CHART_BOX_STYLE_ATTRIBUTES);

      DEFAULT_PIPELINE_CHART_BOX_STYLE_ATTRIBUTES = new XStyle(DEFAULT_LINE_CHART_BOX_STYLE_ATTRIBUTES);
      addDefaultStyle(DEFAULT_PIPELINE_CHART_BOX_STYLE, DEFAULT_PIPELINE_CHART_BOX_STYLE_ATTRIBUTES);

      DEFAULT_PIPELINE_CHART_HEADER_STYLE_ATTRIBUTES = new XStyle(DEFAULT_LINE_CHART_HEADER_STYLE_ATTRIBUTES);
      addDefaultStyle(DEFAULT_PIPELINE_CHART_HEADER_STYLE, DEFAULT_PIPELINE_CHART_HEADER_STYLE_ATTRIBUTES);

      DEFAULT_PIPELINE_CHART_STYLE_ATTRIBUTES = new XStyle(DEFAULT_LINE_CHART_STYLE_ATTRIBUTES);
      DEFAULT_PIPELINE_CHART_STYLE_ATTRIBUTES.tab = 10; //distance between the backgroung stripes
      DEFAULT_PIPELINE_CHART_STYLE_ATTRIBUTES.gap = 8; //distance between pipeline elements
      DEFAULT_PIPELINE_CHART_STYLE_ATTRIBUTES.bottom = 4;
      addDefaultStyle(DEFAULT_PIPELINE_CHART_STYLE, DEFAULT_PIPELINE_CHART_STYLE_ATTRIBUTES);

      DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES = new XStyle(DEFAULT_BAR_STYLE_ATTRIBUTES);
      DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES.foreground = XStyle.DEFAULT_WHITE;
      DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES.border_dark = XStyle.DEFAULT_BLACK;
      DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES.left = 2;
      DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES.top = 1;
      DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES.bottom = 1;
      DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES.right = 2;
      addDefaultStyle(DEFAULT_PIPELINE_ELEMENT_STYLE, DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES);
   }

   private byte ccType;

   public OpChartComponent() {
      super(XComponent.PANEL);
   }

   public OpChartComponent(byte cc_type) {
      ccType = cc_type;
      switch (cc_type) {
         case LINE_CHART_HEADER:
            setStyle(DEFAULT_LINE_CHART_HEADER_STYLE);
            setLayout(ABSOLUTE_LAYOUT);
            setFocusable(false);
            break;
         case LINE_CHART_SIDEBAR:
            setStyle(DEFAULT_LINE_CHART_SIDEBAR_STYLE);
            setLayout(ABSOLUTE_LAYOUT);
            setFocusable(false);
            break;
         case LINE_CHART:
            setStyle(DEFAULT_LINE_CHART_STYLE);
            break;
         case LINE_CHART_BOX:
            setStyle(DEFAULT_LINE_CHART_BOX_STYLE);
            setGridLines(ALL);
            setMarkers(true);
            setValueType(XRenderer.INT);
            setDataColumnIndex(1);
            setDataColumnCount(1);
            setDataCaptionColumnIndex(-1);
            initializeScrollBox().addChild(new OpChartComponent(LINE_CHART));
            XComponent scrolling_header = new XComponent(XComponent.SCROLLING_HEADER);
            scrolling_header.setX(0);
            scrolling_header.setY(-1);
            _addChild(scrolling_header);
            XComponent data_header = new OpChartComponent(OpChartComponent.LINE_CHART_HEADER);
            scrolling_header.addChild(data_header);
            XComponent scrolling_sidebar = new XComponent(XComponent.SCROLLING_SIDEBAR);
            scrolling_sidebar.setX(-1);
            scrolling_sidebar.setY(0);
            _addChild(scrolling_sidebar);
            XComponent data_sidebar = new OpChartComponent(OpChartComponent.LINE_CHART_SIDEBAR);
            scrolling_sidebar.addChild(data_sidebar);
            setFocusable(true);
            setSidebarWidth(100);
            break;
         case BAR:
            setStyle(DEFAULT_BAR_STYLE);
            break;
         case BAR_CHART_HEADER:
            setStyle(DEFAULT_BAR_CHART_HEADER_STYLE);
            setLayout(ABSOLUTE_LAYOUT);
            setFocusable(false);
            break;
         case BAR_CHART_SIDEBAR:
            setStyle(DEFAULT_BAR_CHART_SIDEBAR_STYLE);
            setLayout(ABSOLUTE_LAYOUT);
            setFocusable(false);
            break;
         case BAR_CHART:
            setStyle(DEFAULT_BAR_CHART_STYLE);
            break;
         case BAR_CHART_BOX:
            setStyle(DEFAULT_BAR_CHART_BOX_STYLE);
            setGridLines(NONE);
            setValueType(XRenderer.INT);
            setDataColumnIndex(1);
            setDataColumnCount(1);
            setDataCaptionColumnIndex(-1);
            initializeScrollBox().addChild(new OpChartComponent(BAR_CHART));
            scrolling_header = new XComponent(XComponent.SCROLLING_HEADER);
            scrolling_header.setX(0);
            scrolling_header.setY(-1);
            _addChild(scrolling_header);
            data_header = new OpChartComponent(OpChartComponent.BAR_CHART_HEADER);
            scrolling_header.addChild(data_header);
            scrolling_sidebar = new XComponent(XComponent.SCROLLING_SIDEBAR);
            scrolling_sidebar.setX(-1);
            scrolling_sidebar.setY(0);
            _addChild(scrolling_sidebar);
            data_sidebar = new OpChartComponent(OpChartComponent.BAR_CHART_SIDEBAR);
            scrolling_sidebar.addChild(data_sidebar);
            setFocusable(true);
            setSidebarWidth(140);
            break;
         case PIPELINE_CHART_BOX: {
            setStyle(DEFAULT_PIPELINE_CHART_BOX_STYLE);
            setScrollBarMode(XExtendedComponent.ALWAYS_SHOW_VERTICAL_BAR);
            initializeScrollBox().addChild(new OpChartComponent(PIPELINE_CHART));
            setScrolling(XComponent.VERTICAL_SCROLLING);
            OpChartComponent footer = new OpChartComponent(PIPELINE_CHART_FOOTER);
            footer.setX(0);
            footer.setY(1);
            _addChild(footer);
            setFocusable(true);
         }
         break;
         case PIPELINE_CHART:
            setStyle(DEFAULT_PIPELINE_CHART_STYLE);
            setDefaultChartValues();
            this.setChartOverview(Boolean.FALSE);
            break;
         case PIPELINE_CHART_FOOTER:
            setStyle(DEFAULT_PIPELINE_CHART_HEADER_STYLE);
            break;
         case PIPELINE_ELEMENT:
            setStyle(DEFAULT_PIPELINE_ELEMENT_STYLE);
            break;
      }
   }

   private void setDefaultChartValues() {
      this.setElementFont(DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES.font());
      this.setPipelineGap(DEFAULT_PIPELINE_CHART_STYLE_ATTRIBUTES.gap);
      FontMetrics metrics = getFontMetrics(getElementFont());
      int elementHeight = metrics.getMaxAscent() + metrics.getMaxDescent() +
           DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES.top +
           DEFAULT_PIPELINE_ELEMENT_STYLE_ATTRIBUTES.bottom;
      this.setElementHeight(elementHeight);
   }

   public final byte getChartComponentType() {
      return ccType;
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

   public final void setDataUnit(double data_unit) {
      setProperty(DATA_UNIT, new Double(data_unit));
   }

   public final double getDataUnit() {
      return ((Double) getProperty(DATA_UNIT)).doubleValue();
   }

   public final void setGridLines(byte grid_lines) {
      setProperty(GRID_LINES, new Byte(grid_lines));
   }

   public final byte getGridLines() {
      return ((Byte) getProperty(GRID_LINES)).byteValue();
   }

   public final void setMarkers(boolean markers) {
      setProperty(MARKERS, Boolean.valueOf(markers));
   }

   public final boolean getMarkers() {
      return ((Boolean) getProperty(MARKERS)).booleanValue();
   }

   public final void setDataCaptionColumnIndex(int data_caption_column_index) {
      setProperty(DATA_CAPTION_COLUMN_INDEX, new Integer(data_caption_column_index));
   }

   public final int getDataCaptionColumnIndex() {
      return ((Integer) getProperty(DATA_CAPTION_COLUMN_INDEX)).intValue();
   }

   public final void setPathsX(List paths_x) {
      setTransientProperty(PATHS_X, paths_x);
   }

   public final List getPathsX() {
      return (List) getTransientProperty(PATHS_X);
   }

   public final void setPathsY(List paths_y) {
      setTransientProperty(PATHS_Y, paths_y);
   }

   public final List getPathsY() {
      return (List) getTransientProperty(PATHS_Y);
   }

   public final void setDistribution(int[] distribution) {
      setTransientProperty(DISTRIBUTION, distribution);
   }

   public final int[] getDistribution() {
      return (int[]) getTransientProperty(DISTRIBUTION);
   }

   public final void setDataColumnCount(int data_column_count) {
      setProperty(DATA_COLUMN_COUNT, new Integer(data_column_count));
   }

   public final int getDataColumnCount() {
      return ((Integer) getProperty(DATA_COLUMN_COUNT)).intValue();
   }

   protected final void setColorCaptions(HashMap color_captions) {
      setTransientProperty(COLOR_CAPTIONS, color_captions);
   }

   protected final HashMap getColorCaptions() {
      return (HashMap) getTransientProperty(COLOR_CAPTIONS);
   }

   private Integer getIndexForColorCaption(XComponent colorDataSet, String caption) {
      for (int i = 0; i < colorDataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) colorDataSet.getChild(i);
         XComponent captionCell = (XComponent) dataRow.getChild(0);
         if (captionCell.getStringValue().equals(caption)) {
            return new Integer(i);
         }
      }
      return new Integer(0);
   }


   /**
    * Creates color captions map for the calling <code>LINE_CHART_BOX</code> or <code>BAR_CHART_BOX</code>
    *
    * @return <code>Map</code> of color captions key: caption; value: color
    */
   protected HashMap createColorCaptions() {
      // LinkedHashMap to maintain the order in which keys are added
      HashMap color_captions = new LinkedHashMap();
      XComponent data_set = getDataSetComponent();
      int data_caption_column_index = getDataCaptionColumnIndex();
      XComponent data_row;
      int counter = 0;
      XComponent colorDataSet = getColorDataSetComponent();

      switch (ccType) {
         case LINE_CHART_BOX:
            for (int i = 0; i < data_set.getChildCount(); i++) {
               data_row = (XComponent) (data_set._getChild(i));
               if (data_row.getVisible() && (data_row.getOutlineLevel() == 0) && !data_row.getFiltered()) {
                  if (data_caption_column_index >= 0) {
                     color_captions.put(new Integer(i), new Integer(counter));
                     counter++;
                  }
               }
            }
            break;
         case BAR_CHART_BOX:
            int outline_level;
            String caption;
            XComponent data_caption_cell;
            for (int i = 0; i < data_set.getChildCount(); i++) {
               data_row = (XComponent) (data_set._getChild(i));
               outline_level = data_row.getOutlineLevel();
               if (data_row.getVisible() && (outline_level >= 0) && (outline_level <= 1) && !data_row.getFiltered()) {
                  if (outline_level == 1) {
                     //NOTE:  Hide/filter sub-values if this component is not visible -> Results also in slightly better performance)
                     data_caption_cell = (XComponent) (data_row._getChild(data_caption_column_index));
                     caption = data_caption_cell.getStringValue();
                     if (caption == null) {
                        caption = "?";
                     }
                     if (!color_captions.containsKey(caption)) {
                        color_captions.put(caption, getIndexForColorCaption(colorDataSet, caption));
                        counter++;
                     }
                  }
               }
            }
            break;
      }
      return color_captions;
   }

   public HashMap getChartColorCaptions() {
      HashMap color_captions = getColorCaptions();
      if (color_captions == null) {
         color_captions = createColorCaptions();
         setColorCaptions(color_captions);
      }
      return color_captions;
   }

   public final void setOffsetX(int offset_x) {
      setTransientProperty(OFFSET_X, new Integer(offset_x));
   }

   public final int getOffsetX() {
      return ((Integer) getTransientProperty(OFFSET_X)).intValue();
   }

   public final void setMaxValue(double max_value) {
      setTransientProperty(MAX_VALUE, new Double(max_value));
   }

   public final double getMaxValue() {
      return ((Double) getTransientProperty(MAX_VALUE)).doubleValue();
   }

   public final void setMinValue(double min_value) {
      setTransientProperty(MIN_VALUE, new Double(min_value));
   }

   public final double getMinValue() {
      return ((Double) getTransientProperty(MIN_VALUE)).doubleValue();
   }

   public final void setValueCaptions(List value_captions) {
      setProperty(VALUE_CAPTIONS, value_captions);
   }

   public final List getValueCaptions() {
      return (List) getProperty(VALUE_CAPTIONS);
   }

   public final void setElementHeight(int height) {
      setProperty(PIPELINE_ELEMENT_HEIGHT, new Integer(height));
   }

   public final int getElementHeight() {
      return ((Integer) getProperty(PIPELINE_ELEMENT_HEIGHT)).intValue();
   }

   public final void setPipelineGap(int gap) {
      setProperty(PIPELINE_GAP, new Integer(gap));
   }

   public final int getPipelineGap() {
      return ((Integer) getProperty(PIPELINE_GAP)).intValue();
   }

   public final void setElementFont(Font font) {
      setProperty(PIPELINE_ELEMENT_FONT, font);
   }

   public final Font getElementFont() {
      return ((Font) getProperty(PIPELINE_ELEMENT_FONT));
   }

   public final void setChartOverview(Boolean overview) {
      setProperty(CHART_OVERVIEW_STATE, overview);
   }

   public final Boolean getChartOverview() {
      return ((Boolean) getProperty(CHART_OVERVIEW_STATE));
   }

   public XComponent getContext() {
      switch (ccType) {
         case LINE_CHART_HEADER:
         case LINE_CHART_SIDEBAR:
         case LINE_CHART:
            XComponent component = (XComponent) getParent();
            while (component != null) {
               if ((component instanceof OpChartComponent) && (((OpChartComponent) component).ccType == LINE_CHART_BOX)) {
                  return component;
               }
               component = (XComponent) (component.getParent());
            }
            break;
         case BAR:
         case BAR_CHART_HEADER:
         case BAR_CHART_SIDEBAR:
         case BAR_CHART:
            component = (XComponent) getParent();
            while (component != null) {
               if ((component instanceof OpChartComponent) && (((OpChartComponent) component).ccType == BAR_CHART_BOX)) {
                  return component;
               }
               component = (XComponent) (component.getParent());
            }
            break;
         case PIPELINE_CHART_FOOTER:
         case PIPELINE_CHART:
         case PIPELINE_ELEMENT:
            component = (XComponent) getParent();
            while (component != null) {
               if ((component instanceof OpChartComponent) && (((OpChartComponent) component).ccType == PIPELINE_CHART_BOX)) {
                  return component;
               }
               component = (XComponent) (component.getParent());
            }
            break;
      }
      return null;
   }

   protected int maxValueWidth(XStyle style, byte data_type, double min_value, double max_value) {
      FontMetrics metrics = getFontMetrics(style.font());
      XRenderer renderer = new XRenderer();
      switch (data_type) {
         case XRenderer.INT:
            int min_value_width;
            int max_value_width;
            renderer.setValueType(data_type);
            min_value_width = metrics.stringWidth(renderer.valueToString(new Integer((int) min_value)));
            max_value_width = metrics.stringWidth(renderer.valueToString(new Integer((int) max_value)));
            return Math.max(min_value_width, max_value_width);
         case XRenderer.DOUBLE:
         case XRenderer.DURATION:
            renderer.setValueType(data_type);
            min_value_width = metrics.stringWidth(renderer.valueToString(new Double(min_value)));
            max_value_width = metrics.stringWidth(renderer.valueToString(new Double(max_value)));
            return Math.max(min_value_width, max_value_width);
         case XRenderer.DATE:
            XCalendar calendar = XCalendar.getDefaultCalendar();
            Calendar c = calendar.getCalendar();
            c.set(Calendar.DAY_OF_MONTH, 30);
            max_value_width = 0;
            int value_width;
            for (int month = Calendar.JANUARY; month <= Calendar.DECEMBER; month++) {
               c.set(Calendar.MONTH, month);
               value_width = metrics.stringWidth(calendar.localizedDateToString(new Date(c.getTimeInMillis())));
               if (max_value_width < value_width) {
                  max_value_width = value_width;
               }
            }
            return max_value_width;
         default:
            return -1;
      }
   }

   /**
    * Gets the preffered size of a line chart component. If the points that make up the line chart were already
    * determined, they are taken into acoount. Otherwise a generic size is computed based on the number of data rows
    * this chart has.
    *
    * @return a <code>Dimension</code> representing the preffered size of the line chart.
    */
   protected Dimension getPreferredLineChartSize() {
      XStyle style = getStyleAttributes();
      OpChartComponent box = (OpChartComponent) getContext();
      int data_column_count = box.getDataColumnCount();
      FontMetrics metrics = getFontMetrics(style.font());
      int line_height = metrics.getAscent() + metrics.getDescent();
      int height = 0;
      if (this.getPathsY() != null) {
         for (Iterator it = this.getPathsY().iterator(); it.hasNext();) {
            int[] points_y = (int[]) it.next();
            for (int i = 0; i < points_y.length; i++) {
               int point = points_y[i];
               if (height < point) {
                  height = point;
               }
            }
         }
      }

      if (height == 0) {
         height = (data_column_count - 1) * line_height + ((data_column_count - 2) * style.gap) + style.top
              + style.bottom;
      }
      else {
         height += line_height;
      }
      return new Dimension(0, height);
   }

   protected Dimension getPreferredBarChartSize() {
      XStyle style = getStyleAttributes();
      OpChartComponent box = (OpChartComponent) getContext();
      XComponent dataSet = box.getDataSetComponent();
      int data_column_count = 0;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) (dataSet._getChild(i));
         int outlineLevel = dataRow.getOutlineLevel();
         //Bar chart can represent only data on outline level 0 from data set
         if (outlineLevel == 0) {
            data_column_count++;
         }
      }
      FontMetrics metrics = getFontMetrics(style.font());
      int line_height = (metrics.getAscent() + metrics.getDescent()) * 2;
      int height = data_column_count * line_height + ((data_column_count - 2) * style.gap) + style.top + style.bottom;
      return new Dimension(0, height);
   }


   public Dimension getPreferredSize() {
      switch (ccType) {
         case PIPELINE_CHART_FOOTER:
         case LINE_CHART_HEADER:
         case BAR_CHART_HEADER:
            XStyle style = getStyleAttributes();
            FontMetrics metrics = getFontMetrics(style.font());
            return new Dimension(0, metrics.getAscent() + metrics.getDescent() + style.top + style.bottom);
         case LINE_CHART_SIDEBAR:
         case BAR_CHART_SIDEBAR: {
            OpChartComponent box = (OpChartComponent) getContext();
            return new Dimension(box.getSidebarWidth(), 0);
         }
         case LINE_CHART:
            return getPreferredLineChartSize();
         case BAR_CHART:
            return getPreferredBarChartSize();
         case BAR:
            return new Dimension(getWidth(), getHeight());
         case PIPELINE_CHART:
            return getPreferredPipelineChartSize();
         default:
            return super.getPreferredSize();
      }
   }

   private Dimension getPreferredPipelineChartSize() {
      //TODO Author="Mihai Costin" Description="Cache the pref size in width/height on the component. Reset it on box layout"
      if (getContext().getBounds() != null) {
         OpChartComponent box = (OpChartComponent) getContext();
         int width = box.getBounds().width;
         XComponent vScroll = (XComponent) box.getChild(VERTICAL_SCROLL_BAR_INDEX);
         if (vScroll.getBounds() != null) {
            width -= vScroll.getBounds().width;
         }
         XComponent dataSet = box.getDataSetComponent();

         //max nr of bubbles/header caption
         int maxElements = 0;
         for (int i = 0; i < dataSet.getChildCount(); i++) {
            XComponent row = (XComponent) dataSet.getChild(i);
            XComponent dataCell = (XComponent) row.getChild(2);
            XComponent elements = (XComponent) dataCell.getValue();
            int nrElements = elements.getChildCount();
            if (maxElements < nrElements) {
               maxElements = nrElements;
            }
         }
         int height = getStyleAttributes().bottom + maxElements * (getElementHeight() + getPipelineGap());
         return new Dimension(width, height);
      }
      else {
         return super.getPreferredSize();
      }
   }

   /**
    * Does layouting of the <code>LINE_CHART</code> component
    */
   protected void doLayoutLineChart() {

      logger.debug("DO_LAYOUT line-chart");

      Rectangle bounds = getBounds();
      XStyle style = getStyleAttributes();
      OpChartComponent box = (OpChartComponent) getContext();
      OpChartComponent header = (OpChartComponent) box.getChild(3).getChild(0);
      XStyle headerStyle = header.getStyleAttributes();

      XComponent data_set = box.getDataSetComponent();
      XComponent data_row;
      XComponent data_cell;
      int data_column_index = box.getDataColumnIndex();
      int data_column_count = box.getDataColumnCount();
      int data_type = box.getValueType();

      double value = 0;
      double max_value = 0;
      double min_value = Float.MAX_VALUE;
      int j;
      int i;
      for (i = 0; i < data_set.getChildCount(); i++) {
         data_row = (XComponent) (data_set._getChild(i));
         if (data_row.getVisible() && (data_row.getOutlineLevel() == 0) && !data_row.getFiltered()) {
            for (j = data_column_index; j < data_column_index + data_column_count; j++) {
               data_cell = (XComponent) (data_row._getChild(j));
               if (data_cell.getValue() == null) {
                  continue;
               }
               switch (data_type) {
                  case XRenderer.INT:
                     value = data_cell.getIntValue();
                     break;
                  case XRenderer.DOUBLE:
                  case XRenderer.DURATION:
                     value = data_cell.getDoubleValue();
                     break;
                  case XRenderer.DATE:
                     value = data_cell.getDateValue().getTime();
                     break;
               }
               if (max_value < value) {
                  max_value = value;
               }
               if (min_value > value) {
                  min_value = value;
               }
            }
         }
      }

      // We need to auto-calculate "value-units" for grid-X (for value captions and grid lines)
      // *** We need some lower bounds; this could be the max-value-width
      // *** Then we would have to determine the "real-world" value-width of the same "distance"
      // *** From there: Round "up" until a "natural", "nice" scale value
      // Will be different for numbers (1, 2, 2.5, 5) and dates (multiple of 7 days)
      // *** Date: Divide lower bound through 7 days and round up to next week
      // *** Number (abs > 1): Divide through 10 until 1 < n < 10; round to (2, 2.5, 5, or 10)
      // *** Number (abs < 1): Multiple by 10 until 1 < n < 10; round to (2, 2.5, 5, or 10)

      //if min and max are equal, adjust them
      if (min_value == max_value) {
         switch (data_type) {
            case XRenderer.DATE: {
               min_value = min_value - XCalendar.MILLIS_PER_WEEK;
               max_value = max_value + XCalendar.MILLIS_PER_WEEK;
               break;
            }
         }
      }

      double value_step = Math.abs(max_value - min_value) / (bounds.width - style.left - style.right);
      FontMetrics metrics = getFontMetrics(style.font());
      int grid_y = (metrics.getAscent() + metrics.getDescent()) * 2;
      int grid_x = 0;

      double data_unit = 0.0d;

      // increment of the maxValueWidth represent the % of decrement of x coordinate
      int max_value_width = maxValueWidth(style, (byte) data_type, min_value, max_value) + 20;
      switch (data_type) {
         case XRenderer.INT:
         case XRenderer.DOUBLE:
         case XRenderer.DURATION:

            double d = Math.abs(max_value_width * value_step);
            if (d >= 1) {

               int c = 0;
               while (d > 5) {
                  d /= 10;
                  c++;
               }

               // Try to round up to numbers 1, 2, 2.5 and 5 as "soon" as possible
               // (Until rounded scale "fills" available space to at least 75%
               double value_width75 = Math.abs(max_value - min_value) * 0.75d;

               double scale = 0;
               double r;
               while ((scale * bounds.width < value_width75) && (c > 0)) {
                  scale *= 10;
                  r = 1;
                  if (d > 2.5) {
                     r = 5;
                  }
                  else if (d > 2) {
                     r = 2.5;
                  }
                  else if (d > 1) {
                     r = 2;
                  }
                  if ((scale + r) * bounds.width < value_width75) {
                     r = (int) d;
                  }
                  scale += r;
                  c--;
               }

               // Pad w/zeros
               if (c > 0) {
                  scale *= c * 10;
               }

               logger.debug("   d, scale, value_step " + d + ", " + scale + ", " + value_step);

               grid_x = max_value_width; // (int)(scale * value_step);
               data_unit = scale; // value_step * grid_x;

            }
            else {
               // d < 1
               //Analogous to upper case when this one works
               logger.debug("   d < 1!");
               grid_x = (int) ((double) (bounds.width - style.left - style.right) * 0.75 / Math.abs(max_value - min_value));
               data_unit = 1;
            }

            break;
         case XRenderer.DATE:

            // The minimum "step" is a week -- XCalendar.MILLIS_PER_WEEK; use modulo operations
            d = (double) max_value_width * value_step;
            double scale = Math.ceil(d / XCalendar.MILLIS_PER_WEEK) * XCalendar.MILLIS_PER_WEEK;

            grid_x = max_value_width;
            data_unit = scale;

            logger.debug("   d, scale, value_step " + d + ", " + scale + ", " + value_step);
            break;
      }

      List paths_x = new ArrayList();
      List paths_y = new ArrayList();
      int[] path_x;
      int[] path_y;
      int x;
      int y;

      for (i = 0; i < data_set.getChildCount(); i++) {
         y = grid_y / 2 + style.top;
         data_row = (XComponent) (data_set._getChild(i));
         if (data_row.getVisible() && (data_row.getOutlineLevel() == 0) && !data_row.getFiltered()) {
            path_x = new int[data_column_count];
            path_y = new int[data_column_count];
            for (j = data_column_index; j < data_column_index + data_column_count; j++) {
               data_cell = (XComponent) (data_row._getChild(j));
               if (data_cell.getValue() == null) {
                  continue;
               }
               switch (data_type) {
                  case XRenderer.INT:
                     value = data_cell.getIntValue();
                     break;
                  case XRenderer.DOUBLE:
                  case XRenderer.DURATION:
                     value = data_cell.getDoubleValue();
                     break;
                  case XRenderer.DATE:
                     value = data_cell.getDateValue().getTime();
                     break;
               }
               x = (int) Math.ceil((value - min_value) * grid_x / data_unit) + headerStyle.left;

               path_x[j - data_column_index] = x;
               path_y[j - data_column_index] = y;
               y += grid_y + style.gap;
            }
            paths_x.add(path_x);
            paths_y.add(path_y);
         }
      }

      box.setDataUnit(data_unit);
      box.setGridX(grid_x);
      box.setGridY(grid_y);
      logger.debug("  gx, du " + grid_x + ", " + data_unit);
      box.setMaxValue(max_value);
      box.setMinValue(min_value);
      setPathsX(paths_x);
      setPathsY(paths_y);
   }

   /**
    * Does layouting of the <code>BAR_CHART</code> component
    */
   protected void doLayoutBarChart() {
      // Layout bars including 2nd-level value distribution (if applicable)
      Rectangle bounds = getBounds();
      XStyle style = getStyleAttributes();
      OpChartComponent box = (OpChartComponent) getContext();
      OpChartComponent header = (OpChartComponent) box.getChild(3).getChild(0);
      /* get the header style */
      XStyle headerStyle = header.getStyleAttributes();

      List value_captions = null;
      if (box.getValueCaptions() == null) {
         value_captions = new ArrayList();
      }

      HashMap color_captions = box.getChartColorCaptions();

      XComponent data_set = box.getDataSetComponent();
      XComponent data_row;
      XComponent data_cell;
      XComponent data_caption_cell;
      int data_column_index = box.getDataColumnIndex();
      int data_column_count = box.getDataColumnCount();
      int data_caption_column_index = box.getDataCaptionColumnIndex();
      int data_type = box.getValueType();

      double value = 0;
      double max_value = 0;
      int j;
      int i;
      int outline_level;
      for (i = 0; i < data_set.getChildCount(); i++) {
         data_row = (XComponent) (data_set._getChild(i));
         outline_level = data_row.getOutlineLevel();
         if (data_row.getVisible() && (outline_level >= 0) && (outline_level <= 1) && !data_row.getFiltered()) {
            for (j = data_column_index; j < data_column_index + data_column_count; j++) {
               data_cell = (XComponent) (data_row._getChild(j));
               switch (data_type) {
                  case XRenderer.INT:
                     value = data_cell.getIntValue();
                     break;
                  case XRenderer.DOUBLE:
                     value = data_cell.getDoubleValue();
                     break;
                  case XRenderer.DATE:
                     value = data_cell.getDateValue().getTime();
                     break;
                  case XRenderer.DURATION:
                     // All durations must have the same unit (comparability)
                     value = data_cell.getDoubleValue();
                     break;
               }
               if (max_value < value) {
                  max_value = value;
               }
            }
         }
      }

      FontMetrics metrics = getFontMetrics(style.font());
      int sub_grid_y = (metrics.getAscent() + metrics.getDescent()) * 2;
      int grid_y = sub_grid_y * data_column_count;
      double value_step = max_value / (bounds.width - style.left - style.right);
      int grid_x = 0;

      double data_unit = 0.0d;

      //<FIXME author="Mihai Costin" description="30 - Magic number ?"
      int max_value_width = maxValueWidth(style, (byte) data_type, 0, max_value) + 30;
      //</FIXME>

      switch (data_type) {
         case XRenderer.INT:
         case XRenderer.DOUBLE:
         case XRenderer.DURATION:
            double d = Math.abs(max_value / ((double) bounds.width / (double) max_value_width));
            double d75;

            // check if it is INFINIT
            if (d == Double.POSITIVE_INFINITY) {
               d75 = max_value;
            }
            else {
               d75 = d * 1.25d;
               // check if it is INFINIT
               if (d75 == Double.POSITIVE_INFINITY) {
                  d75 = max_value * 1.25d;
               }
            }

            double x = d75;
            int c = 0;
            while (x >= 10) {
               x /= 10;
               c++;
            }

            grid_x = max_value_width;

            // new algorithm of the data_unit value
            double scaleX = Math.rint(d75);
            // string representation of the string
            String scaleStringReprezentation = Integer.toString((int) scaleX);
            // from 2777.5 return 777.5
            String stringDecrement = scaleStringReprezentation.substring(scaleStringReprezentation.length() - c);
            double doubleDecrement = 0;
            // check if is not a empty string
            if (stringDecrement.length() != 0) {
               doubleDecrement = Double.valueOf(stringDecrement).doubleValue();
            }

            // if the decrement is grater than 500
            if (doubleDecrement > 500) {
               int round = (int) doubleDecrement / 500;
               // decrement the value
               doubleDecrement = doubleDecrement - round * 500;
            }
            if (doubleDecrement != 0) {
               scaleX = scaleX - doubleDecrement + Math.pow(10, c);
            }
            data_unit = scaleX; // value_step * grid_x;
            break;

         case XRenderer.DATE:
            // The minimum "step" is a week -- XCalendar.MILLIS_PER_WEEK; use modulo operations
            d = (double) max_value_width * value_step;
            double scale = Math.ceil(d / XCalendar.MILLIS_PER_WEEK) * XCalendar.MILLIS_PER_WEEK;
            grid_x = max_value_width; // grid_x = (int)(scale * value_step);
            data_unit = scale; // value_step * grid_x;
            logger.debug("   d, scale, value_step " + d + ", " + scale + ", " + value_step);
            break;
      }

      OpChartComponent visual = null;
      int count = 0;
      int caption_count = 0;
      int max_caption_count = 0;
      String caption;
      int bar_width;
      int y = style.top;
      int[] distribution = null;
      for (i = 0; i < data_set.getChildCount(); i++) {
         data_row = (XComponent) (data_set._getChild(i));
         outline_level = data_row.getOutlineLevel();
         if (data_row.getVisible() && (outline_level >= 0) && (outline_level <= 1) && !data_row.getFiltered()) {
            for (j = data_column_index; j < data_column_index + data_column_count; j++) {
               data_cell = (XComponent) (data_row._getChild(j));
               switch (data_type) {
                  case XRenderer.INT:
                     value = data_cell.getIntValue();
                     break;
                  case XRenderer.DOUBLE:
                     value = data_cell.getDoubleValue();
                     break;
                  case XRenderer.DATE:
                     value = data_cell.getDateValue().getTime();
                     break;
                  case XRenderer.DURATION:
                     // All durations must have the same unit (comparability)
                     value = data_cell.getDoubleValue();
                     break;
               }
               if (max_value < value) {
                  max_value = value;
               }
               // the bar width is calculated taking into consideration the header style left
               bar_width = (int) Math.round(value * (grid_x + headerStyle.left) / data_unit);
               if (outline_level == 0) {
                  // Set distribution of sub-values for previous master-value
                  // visual
                  if (visual != null) {
                     visual.setDistribution(distribution);
                  }
                  distribution = null;
                  // Master values layout bars
                  if (count < getChildCount()) {
                     visual = (OpChartComponent) _getChild(count);
                  }
                  else {
                     visual = new OpChartComponent(BAR);
                     _addChild(visual);
                  }
                  visual.setBounds(new Rectangle(style.left, y, bar_width, sub_grid_y + 1));
                  count++;
                  y += sub_grid_y; // *** Probably go grid_x instead of
                  // sub-grid-x
                  if (j == data_column_index) {
                     if (max_caption_count < caption_count) {
                        max_caption_count = caption_count;
                     }
                     caption_count = 0;
                  }
                  // Optional value captions
                  if ((value_captions != null) && (j == data_column_index)) {
                     data_caption_cell = (XComponent) (data_row._getChild(data_caption_column_index));
                     caption = data_caption_cell.getStringValue();
                     value_captions.add(caption);
                  }
                  logger.debug("   BAR_WIDTH " + bar_width);
               }
               else if (visual != null) {
                  // TODO: Hide/filter sub-values if this component is not visible Results also in slightly better performance)
                  if (distribution == null) {
                     distribution = new int[color_captions.size()];
                     for (int d = 0; d < distribution.length; d++) {
                        distribution[d] = -1;
                     }
                  }
                  if (j == data_column_index) {
                     data_caption_cell = (XComponent) (data_row._getChild(data_caption_column_index));
                     caption = data_caption_cell.getStringValue();
                     int color_index = ((Integer) (color_captions.get(caption))).intValue();
                     logger.debug("      BAR_WIDTH " + bar_width);
                     distribution[color_index] = bar_width;
                  }
               }
            }
            y += style.gap;
         }
      }
      // Set distribution of last visual
      if (visual != null) {
         visual.setDistribution(distribution);
      }
      // Remove unused visuals
      while (count < getChildCount()) {
         removeChild(getChildCount() - 1);
      }

      box.setDataUnit(data_unit);
      box.setGridX(grid_x);
      box.setGridY(grid_y);
      logger.debug("  gx, du " + grid_x + ", " + data_unit);
      box.setMaxValue(max_value);
      box.setMinValue(0.0f);
      if ((value_captions != null) && (value_captions.size() > 0)) {
         box.setValueCaptions(value_captions);
      }
      else {
         box.setValueCaptions(box.getValueCaptions());
      }
   }


   public void doLayout() {
      switch (ccType) {
         case LINE_CHART_HEADER:
         case LINE_CHART_SIDEBAR:
         case BAR_CHART_HEADER:
         case BAR_CHART_SIDEBAR:
            break;
         case LINE_CHART:
            doLayoutLineChart();
            break;
         case BAR:
            break;
         case BAR_CHART:
            doLayoutBarChart();
            break;
         case LINE_CHART_BOX:
         case BAR_CHART_BOX:
            scrollSlidersToZero();
            doLayoutScrollBox();
            scrollSlidersToLastValue();
            break;
         case PIPELINE_CHART_BOX:
            scrollSlidersToZero();
            OpChartComponent chart = (OpChartComponent) this.getBoxContent();
            if (chart.getChartOverview().booleanValue()) {
               zoomIn();
               zoomOut();
            }
            else {
               zoomIn();
            }
            doLayoutScrollBox();
            scrollSlidersToLastValue();
            break;
         case PIPELINE_CHART:
            doLayoutPipelineChart();
            break;
         default:
            super.doLayout();
      }
   }

   private void doLayoutPipelineChart() {

      OpChartComponent box = (OpChartComponent) getContext();
      XComponent dataSet = box.getDataSetComponent();

      //nr of rows in the data set = nr of captions
      int rows = dataSet.getChildCount();

      int gridX;
      Rectangle chartBounds = this.getBounds();
      if (rows != 0) {
         gridX = chartBounds.width / rows;
      }
      else {
         gridX = chartBounds.width;
      }
      int gridY = this.getElementHeight();
      box.setGridX(gridX);
      box.setGridY(gridY);
      double maxElementSize = gridX * 1.1;
      double minElementSize = gridX * 0.5;
      double middleElementSize = gridX * 0.85;
      double maxElementValue = getMaxElementValue(dataSet);
      double minElementValue = getMinElementValue(dataSet);
      double overlapSize = maxElementSize - gridX;

      int nrElements = 0;
      int elementX = 0;
      boolean allEquals = true;
      for (int i = 0; i < dataSet.getChildCount(); i++) {

         // one row / h_caption
         XComponent row = (XComponent) dataSet.getChild(i);
         XComponent valuesDataSet = (XComponent) ((XComponent) row.getChild(2)).getValue();

         //create/reuse chartBounds and set on them the elementValues as value
         OpChartComponent element;
         int elementY = chartBounds.height - gridY - getStyleAttributes().bottom;
         for (int j = valuesDataSet.getChildCount() - 1; j >= 0; j--) {
            XComponent elementRow = (XComponent) valuesDataSet.getChild(j);
            if (nrElements < this.getChildCount()) {
               element = (OpChartComponent) this.getChild(nrElements);
            }
            else {
               //create new element
               element = new OpChartComponent(OpChartComponent.PIPELINE_ELEMENT);
               this.addChild(element);
            }
            double value = ((XComponent) elementRow.getChild(0)).getDoubleValue();

            int width = (int) (((value - minElementValue) * (maxElementSize - minElementSize)) / (maxElementValue - minElementValue) + minElementSize);
            if (value != maxElementValue) {
               allEquals = false;
            }
            String text = ((XComponent) elementRow.getChild(1)).getStringValue();
            int colorIndex = ((XComponent) elementRow.getChild(2)).getIntValue();
            element.setValue(getColorAtIndex(colorIndex));

            element.setText(text);
            element.setTooltip(text);

            Rectangle elementBounds = new Rectangle((int) (elementX - overlapSize / 2), elementY, (int) maxElementSize, gridY);
            element.setBounds(elementBounds);
            element.setWidth(width);
            elementY -= (gridY + this.getPipelineGap());
            nrElements++;
         }
         elementX += gridX;
      }
      //clean-up unused chartBounds
      while (getChildCount() > nrElements) {
         removeChild(getChildCount() - 1);
      }

      int equalElementSize = (int) middleElementSize;
      //if all data rows have 0 value, the element size should be the minimum one
      if (maxElementValue == 0) {
         equalElementSize = (int) minElementSize;
      }
      if (allEquals) {
         for (int i = 0; i < getChildCount(); i++) {
            OpChartComponent element = (OpChartComponent) getChild(i);
            element.setWidth(equalElementSize);
         }
      }
   }

   private double getMaxElementValue(XComponent dataSet) {
      double maxValue = -1;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent row = (XComponent) dataSet.getChild(i);
         XComponent dataCell = (XComponent) row.getChild(2);
         XComponent elementDataSet = (XComponent) dataCell.getValue();
         for (int j = 0; j < elementDataSet.getChildCount(); j++) {
            XComponent element = (XComponent) elementDataSet.getChild(j);
            XComponent valueDataCell = (XComponent) element.getChild(0);
            double elementValue = valueDataCell.getDoubleValue();
            if (elementValue > maxValue) {
               maxValue = elementValue;
            }
         }
      }
      return maxValue;
   }

   private int getMaxPipelineBarLength(XComponent dataSet) {
      int maxValue = -1;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent row = (XComponent) dataSet.getChild(i);
         XComponent dataCell = (XComponent) row.getChild(2);
         XComponent elementDataSet = (XComponent) dataCell.getValue();
         int size = elementDataSet.getChildCount();
         if (size > maxValue) {
            maxValue = size;
         }
      }
      return maxValue;
   }

   private double getMinElementValue(XComponent dataSet) {
      double minValue = Double.MAX_VALUE;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent row = (XComponent) dataSet.getChild(i);
         XComponent dataCell = (XComponent) row.getChild(2);
         XComponent elementDataSet = (XComponent) dataCell.getValue();
         for (int j = 0; j < elementDataSet.getChildCount(); j++) {
            XComponent element = (XComponent) elementDataSet.getChild(j);
            XComponent valueDataCell = (XComponent) element.getChild(0);
            double elementValue = valueDataCell.getDoubleValue();
            if (elementValue < minValue) {
               minValue = elementValue;
            }
         }
      }
      return minValue;
   }

   /**
    * Checks if the chart component contains any information (that came from the data-set).
    *
    * @return <code>true</code> if there is any information in the data-set.
    */
   private boolean containsData() {
      return this.getMinValue() < this.getMaxValue();
   }

   /**
    * Paints the <code>HEADER</code> component of the <code>BAR_CHART</code> and <code>LINE_CHART</code>
    *
    * @param g         The graphic context
    * @param clip_area Clip area rectangle
    */
   protected void paintChartHeader(Graphics g, Rectangle clip_area) {
      logger.debug("PAINT_CHART_HEADER");
      Rectangle bounds = getBounds();
      XStyle style = getStyleAttributes();
      OpChartComponent box = (OpChartComponent) getContext();
      FontMetrics metrics = getFontMetrics(style.font());
      int grid_x = box.getGridX();

      // Paint x-axis
      g.setColor(style.border_dark);
      int y = bounds.height - 1 - style.bottom;
      /* draw a botom line */
      g.drawLine(0, y, bounds.width, y);
      int dxIncrement = style.left + grid_x;
      int x = 0;
      while (x < bounds.width - style.right && grid_x > 0) {
         g.drawLine(x, style.top, x, y);
         x += dxIncrement;
      }

      // if no data was found, do not draw the scale
      if (!box.containsData()) {
         return;
      }

      // Draw value scale
      g.setColor(style.foreground);
      int data_type = box.getValueType();
      XCalendar c = XCalendar.getDefaultCalendar();
      double value = box.getMinValue();
      double data_unit = box.getDataUnit();
      String s = null;
      x = 0;
      y = style.top + metrics.getAscent();
      while (x < bounds.width - style.right && grid_x > 0) {
         switch (data_type) {
            case XRenderer.INT:
               s = XDisplay.getCalendar().localizedDoubleToString(value, 0);
               break;
            case XRenderer.DOUBLE:
               int decimalsNr = box.getDecimals() != null ? box.getDecimals().intValue() : XCalendar.DEFAULT_DECIMAL_DIGITS;
               s = XDisplay.getCalendar().localizedDoubleToString(value, decimalsNr);
               break;
            case XRenderer.DATE:
               s = c.localizedDateToString(new Date((long) value));
               break;
            case XRenderer.DURATION:
               s = c.localizedDurationToString(value, XCalendar.HOURS);
               break;
         }
         if (s != null) {
            g.drawString(s, x + style.left, y);
         }
         value += data_unit;
         x += dxIncrement;
      }
   }

   protected void paintChartSidebar(Graphics g, Rectangle clip_area, boolean y_markers) {
      logger.debug("PAINT_CHART_SIDEBAR");
      Rectangle bounds = getBounds();
      XStyle style = getStyleAttributes();
      OpChartComponent box = (OpChartComponent) getContext();
      int vScroll = -((XComponent) box.getChild(VERTICAL_SCROLL_BAR_INDEX)).getIntValue();
      FontMetrics metrics = getFontMetrics(style.font());
      int grid_y = box.getGridY();

      // Paint y-axis
      List value_captions = box.getValueCaptions();
      g.setColor(style.border_dark);
      g.drawLine(bounds.width - 1, style.top, bounds.width - 1, bounds.height - 1 - style.bottom);
      int i = 0;
      int y = style.top + metrics.getAscent() + vScroll;
      int yc = grid_y / 2 + style.top + vScroll;
      if (y_markers || (value_captions != null)) {
         while (y < bounds.height - style.bottom && grid_y > 0) {
            if ((value_captions != null) && (i < value_captions.size())) {
               String s = (String) (value_captions.get(i));
               if (s == null) {
                  s = "";
               }
               int maxWidth = bounds.width - style.right - style.left;
               if (maxWidth < metrics.stringWidth(" " + s)) {
                  s = cutTextToWidth(style, " " + s, maxWidth);
               }
               g.setColor(style.foreground);
               g.drawString(s, maxWidth - metrics.stringWidth(s), y - 2 + (grid_y - metrics.getAscent()) / 2);
               i++;
            }
            if (y_markers) {
               g.setColor(style.border_dark);
               g.drawLine(bounds.width - 1, yc, bounds.width - 5, yc);
            }
            y += grid_y;
            yc += grid_y;
         }
      }
   }

   /**
    * Paint chart backgroud for <code>BAR_CHART</code> and <code>LINE_CHART</code> components.
    *
    * @param g     The graphics
    * @param style The <code>BAR_CHART_HEADER</code> or <code>LINE_CHART</code> <code>XStyle</code> attributes.
    */
   protected void paintChartBackground(Graphics g, XStyle style) {
      Rectangle bounds = getBounds();
      g.setFont(style.font());
      g.setColor(style.background);
      g.fillRect(0, 0, bounds.width, bounds.height);
      OpChartComponent box = (OpChartComponent) getContext();
      int grid_x = box.getGridX();
      int grid_y = box.getGridY();
      int bottom = bounds.height - style.bottom;
      int x;
      int y;
      // Paint optional grid-lines
      g.setColor(style.border_light);
      byte grid_lines = box.getGridLines();
      if ((grid_lines == HORIZONTAL) || (grid_lines == ALL)) {
         y = style.top + (grid_y / 2);
         while (y < bounds.height - style.bottom && grid_y > 0) {
            g.drawLine(style.left, y, bounds.width - style.right, y);
            y += grid_y;
         }
      }

      if ((grid_lines == VERTICAL) || (grid_lines == ALL)) {
         x = style.left + grid_x;
         while (x < bounds.width - style.right && grid_x > 0) {
            g.drawLine(x, style.top, x, bottom);
            x += grid_x + style.left;
         }
      }
   }

   /**
    * Paints the <code>LINE_CHART</code> component
    *
    * @param g         the Graphics
    * @param clip_area clip area rectangle
    */
   protected void paintLineChart(Graphics g, Rectangle clip_area) {
      logger.debug("PAINT_LINE_CHART");
      XStyle style = getStyleAttributes();
      paintChartBackground(g, style);

      // Paint paths w/optional markers
      List paths_x = getPathsX();
      List paths_y = getPathsY();
      int[] path_x;
      int[] path_y;
      OpChartComponent box = (OpChartComponent) getContext();

      // do not paint if no underlying data
      if (!box.containsData()) {
         return;
      }

      boolean markers = box.getMarkers();
      int j;
      for (int i = 0; i < paths_x.size(); i++) {
         g.setColor(getColorAtIndex(i));
         path_x = (int[]) (paths_x.get(i));
         path_y = (int[]) (paths_y.get(i));

         g.drawPolyline(path_x, path_y, path_x.length);
         if (markers) {
            for (j = 0; j < path_x.length; j++) {
               g.fillRect(path_x[j] - 3, path_y[j] - 3, 6, 6);
            }
         }
      }
   }

   /**
    * Paints a <code> BAR</code> component.
    *
    * @param g         the <code>Graphics</code>
    * @param clip_area a <code>Rectangle</code> representing the clip area.
    */
   protected void paintBar(Graphics g, Rectangle clip_area) {
      XStyle style = getStyleAttributes();
      Rectangle bounds = getBounds();
      g.setColor(style.border_dark);
      //get distibution
      int[] distribution = getDistribution();

      if (distribution != null) {
         boolean distributionAvailable = false;
         for (int index = 0; index < distribution.length; index++) {
            if (distribution[index] != 0) {
               distributionAvailable = true;
            }
         }
         if (!distributionAvailable) {
            return; //distibution array is empty
         }

         int x = style.left;
         int dw;
         //bar width equal with distribution sum
         int barWidth = 0;
         for (int i = 0; i < distribution.length; i++) {
            dw = distribution[i];
            if (dw != -1) {
               barWidth += dw;
               //get color from index i
               Color color = getColorAtIndex(i);
               g.setColor(color);
               g.fillRect(x, style.top, dw, bounds.height - style.top - style.bottom);
               x += dw;
            }
         }
         g.setColor(style.border_light);
         //finnaly draw bar rect
         g.drawRect(style.left, style.top, barWidth, bounds.height - 1 - style.top - style.bottom);
      }
      else {
         g.drawRect(style.left, style.top, bounds.width, bounds.height - 1 - style.top - style.bottom);
      }
   }

   private Color getColorAtIndex(int i) {
      XComponent colorDataSetComponent = getContext().getColorDataSetComponent();
      XView row = colorDataSetComponent.getChild(i);
      XComponent colorCell = ((XComponent) row.getChild(1));
      return (Color) colorCell.getValue();
   }

   /**
    * Paints a <code>BAR_CHART</code> component.
    *
    * @param g         the <code>Graphics</code>
    * @param clip_area a <code>Rectangle</code> representing the clip area.
    */
   protected void paintBarChart(Graphics g, Rectangle clip_area) {
      OpChartComponent box = (OpChartComponent) getContext();
      OpChartComponent header = (OpChartComponent) box.getChild(3).getChild(0);
      /* get the header style */
      XStyle headerStyle = header.getStyleAttributes();

      paintChartBackground(g, headerStyle);
      paintChildren(g, clip_area);
   }


   public void paint(Graphics g, Rectangle clip_area) {
      switch (ccType) {
         case LINE_CHART_HEADER:
         case BAR_CHART_HEADER:
            paintChartHeader(g, clip_area);
            break;
         case LINE_CHART_SIDEBAR:
            paintChartSidebar(g, clip_area, true);
            break;
         case LINE_CHART:
            paintLineChart(g, clip_area);
            break;
         case BAR:
            paintBar(g, clip_area);
            break;
         case BAR_CHART_SIDEBAR:
            paintChartSidebar(g, clip_area, false);
            break;
         case BAR_CHART:
            paintBarChart(g, clip_area);
            break;
         case PIPELINE_CHART_BOX:
         case LINE_CHART_BOX:
         case BAR_CHART_BOX:
            Rectangle bounds = getBounds();
            XStyle style = getStyleAttributes();
            drawBox(g, style, 0, 0, bounds.width, bounds.height);
            paintChildren(g, clip_area);
            break;
         case PIPELINE_CHART:
            paintPipelineChartBackground(g);
            super.paint(g, clip_area);
            break;
         case PIPELINE_CHART_FOOTER:
            paintPipelineChartHeader(g);
            break;
         case PIPELINE_ELEMENT:
            paintPipelineElement(g);
            break;
         default:
            super.paint(g, clip_area);
      }
   }

   private void paintPipelineElement(Graphics g) {
      XStyle style = getStyleAttributes();
      OpChartComponent chart = (OpChartComponent) getParent();
      Font elementFont = chart.getElementFont();
      FontMetrics metrics = getFontMetrics(elementFont);
      int textWidth = metrics.stringWidth(getText());
      int elementWidth = getWidth();
      int elementHeight = chart.getElementHeight();
      int x = (getBounds().width - elementWidth) / 2;
      int y = 0;
      g.setColor(style.border_dark);
      //draw shadow
      g.fillRoundRect(x + 2, y + 1, elementWidth, elementHeight, XStyle.ARC_DIAMETER, XStyle.ARC_DIAMETER);
      Color color = (Color) getValue();
      g.setColor(color);
      //draw full shape
      g.fillRoundRect(x, y, elementWidth, elementHeight, XStyle.ARC_DIAMETER, XStyle.ARC_DIAMETER);
      //draw darker border
      Color darkerColor = XStyle.getDarkerColor(color);
      if (darkerColor != null) {
         g.setColor(darkerColor);
         g.drawRoundRect(x, y, elementWidth - 1, elementHeight - 1, XStyle.ARC_DIAMETER, XStyle.ARC_DIAMETER);
      }
      g.setColor(style.foreground);
      String text = getText();
      int textX = x + (elementWidth - textWidth) / 2;
      int textY = y + (elementHeight + metrics.getMaxAscent() - metrics.getMaxDescent()) / 2;
      if (textX < x) {
         textX = x;
         text = cutTextToWidth(style, text, elementWidth);
      }
      g.setFont(elementFont);
      g.drawString(text, textX, textY);
   }

   private void paintPipelineChartBackground(Graphics g) {
      XComponent dataSet = getContext().getDataSetComponent();
      XStyle style = getStyleAttributes();

      int tab = style.tab;
      int gridX = ((OpChartComponent) getContext()).getGridX();
      int x = 0;
      int height = this.getBounds().height;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent row = (XComponent) dataSet.getChild(i);
         XComponent colorDataCell = (XComponent) row.getChild(1);
         Color color = (Color) colorDataCell.getValue();
         g.setColor(color);

         int[] xPoints = new int[10];
         int[] yPoints = new int[10];

         int deviation = gridX / 6;
         xPoints[0] = x;
         yPoints[0] = 0;
         xPoints[1] = x + gridX - tab;
         yPoints[1] = yPoints[0];
         xPoints[2] = xPoints[1] + deviation;
         yPoints[2] = height / 2;
         xPoints[3] = xPoints[1];
         yPoints[3] = height;
         xPoints[4] = xPoints[0];
         yPoints[4] = yPoints[3];
         xPoints[5] = xPoints[0] + deviation;
         yPoints[5] = yPoints[2];

         g.fillPolygon(xPoints, yPoints, 6);
         x += gridX;
      }
   }

   private void paintPipelineChartHeader(Graphics g) {
      XComponent dataSet = getContext().getDataSetComponent();
      XStyle style = getStyleAttributes();
      FontMetrics metrics = getFontMetrics(style.font());

      int gridX = ((OpChartComponent) getContext()).getGridX();
      int x = 0;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent row = (XComponent) dataSet.getChild(i);
         XComponent captionDataCell = (XComponent) row.getChild(0);
         String caption = captionDataCell.getStringValue();
         caption = cutTextToWidth(style, caption, gridX);
         int textWidth = metrics.stringWidth(caption);
         Color color = style.foreground;
         g.setFont(style.font());
         g.setColor(color);
         g.drawString(caption, x + (gridX - textWidth) / 2, getBounds().height - metrics.getMaxDescent());
         x += gridX;
      }
   }

   public void processPointerEvent(HashMap event, int action, int x, int y, int modifiers) {
      logger.debug("OpChartComponent.processPointerEvent");
      switch (ccType) {
         case LINE_CHART_BOX: {
            if (action == POINTER_DOWN) {
               requestFocus();
            }
            super.processPointerEvent(event, action, x, y, modifiers);
            break;
         }
         case BAR_CHART_BOX: {
            if (action == POINTER_DOWN) {
               requestFocus();
            }
            super.processPointerEvent(event, action, x, y, modifiers);
            break;
         }
         default: {
            super.processPointerEvent(event, action, x, y, modifiers);
         }
      }
   }

   public void processComponentEvent(HashMap event, int action) {
      logger.debug("OpChartComponent.processComponentEvent");
      switch (ccType) {
         case PIPELINE_CHART_BOX:
         case LINE_CHART_BOX:
         case BAR_CHART_BOX:
            //Virtual inheritance does not work (component-type is not correct)
            processScrollBoxComponentEvent(event, action);
            super.processComponentEvent(event, action);
            break;
      }
   }

   /**
    * @see onepoint.express.XView#prepareImageableView()
    */
   public Dimension prepareImageableView() {
      Dimension size;
      switch (ccType) {
         case BAR_CHART_BOX: {
            hideScrollBars();
            int height = this.getPreferredSize().height;
            int width = this.getPreferredSize().width;
            OpChartComponent barChart = (OpChartComponent) this.getChild(VIEW_PORT_INDEX).getChild(0);
            for (int i = 0; i < barChart.getChildCount(); i++) {
               OpChartComponent bar = (OpChartComponent) barChart.getChild(i);
               if (bar.getPreferredSize().width > width) {
                  width = bar.getPreferredSize().width;
               }
            }
            //we need this, because the bars aren't fix.
            if (width < this.getBounds().width) {
               width = this.getBounds().width;
            }
            size = new Dimension(width, height);
         }
         break;
         case LINE_CHART_BOX: {
            Rectangle bounds = this.getBounds();
            hideScrollBars();
            int height = this.getPreferredSize().height;
            int width = this.getPreferredSize().width;

            if (height < bounds.height) {
               height = bounds.height;
            }
            if (width < bounds.width) {
               width = bounds.width;
            }
            size = new Dimension(width, height);
         }
         break;
         default:
            size = super.prepareImageableView();
            break;
      }
      return size;
   }

   /**
    * Zooms in/out the chart contained in this box.
    * Defined only on PIPELINE_CHART_BOX.
    *
    * @param zoomAmount the amout to zoom with (if <0 will zoom out. otherwise it will zoom in)
    */
   public void zoom(int zoomAmount) {
      if (this.getChartComponentType() == PIPELINE_CHART_BOX) {
         OpChartComponent chart = (OpChartComponent) this.getBoxContent();
         if (zoomAmount < 0 && !chart.getChartOverview().booleanValue()) {
            //zoom out
            chart.setChartOverview(Boolean.TRUE);
            this.update();
         }
         else {
            //zoom in
            chart.setChartOverview(Boolean.FALSE);
            this.update();
         }
      }
      else {
         throw new IllegalArgumentException("Zoom is not defined for this type of component " + this.getChartComponentType());
      }
   }

   /**
    * Zooms in the chart. Sets the heights and gaps of the elements to the default initial values.
    * Must be called on a PIPELINE CHART BOX.
    */
   private void zoomIn() {
      OpChartComponent chart = (OpChartComponent) this.getBoxContent();
      chart.setDefaultChartValues();
   }

   /**
    * Zooms out the chart. Sets the heights and gaps of the elements so that the chart box won't require a scroll bar.
    * Must be called on a PIPELINE CHART BOX.
    *
    * @return True if the zoom took place.
    */
   private boolean zoomOut() {
      OpChartComponent chart = (OpChartComponent) this.getBoxContent();
      OpChartComponent footer = (OpChartComponent) this.getChild(PIPELINE_FOOTER_INDEX);
      int diff = (int) (chart.getPreferredSize().getHeight() - (this.getBounds().height - footer.getBounds().height));
      diff += +chart.getStyleAttributes().gap + chart.getStyleAttributes().bottom;
      int maxLength = getMaxPipelineBarLength(this.getDataSetComponent());
      if (diff <= 0 || maxLength == 0) {
         return false;
      }
      if (maxLength > 2) {
         maxLength--; //leave one height out for safe zoomOut
      }
      double amount = Math.ceil(diff / (double) maxLength);
      int gap = chart.getPipelineGap();
      gap -= Math.ceil(amount / 2);
      int height = chart.getElementHeight();
      height -= Math.ceil(amount / 2);
      if (gap < 0) {
         height += gap;
         gap = 0;
      }
      height = (height < 2) ? 2 : height;
      int originalHeight = chart.getElementHeight();
      chart.setElementHeight(height);
      chart.setPipelineGap(gap);
      Font currentFont = chart.getElementFont();
      int currentSize = currentFont.getSize();
      int newSize = currentSize * height / originalHeight;
      newSize = (newSize < 3) ? 3 : newSize;
      chart.setElementFont(new Font(currentFont.getName(), currentFont.getStyle(), newSize));
      return true;
   }

}
