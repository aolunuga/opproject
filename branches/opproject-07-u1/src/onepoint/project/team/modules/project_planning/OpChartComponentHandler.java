/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_planning;

// import onepoint.express.XComponent;

import onepoint.express.server.XDefaultComponentHandler;
import onepoint.project.team.modules.project_planning.components.OpChartComponent;
import onepoint.xml.XContext;

import java.util.HashMap;

/**
 * Handler for the xml chart components.
 */
public class OpChartComponentHandler extends XDefaultComponentHandler {

   // Element names
   public final static String LINE_CHART_BOX = "line-chart-box";
   public final static String BAR_CHART_BOX = "bar-chart-box";
   public final static String PIPELINE_CHART_BOX = "pipeline-chart-box";

   // Attribute names
   public final static String DATA_UNIT = "data-unit";
   public final static String DATA_COLUMN_COUNT = "data-column-count";
   public final static String GRID_LINES = "grid-lines";
   public final static String MARKERS = "markers";
   public final static String DATA_CAPTION_COLUMN_INDEX = "data-caption-column-index";

   public Object newNode(XContext context, String name, HashMap attributes) {
      OpChartComponent component = null;
      Object value;
      if (LINE_CHART_BOX.equals(name)) {
         component = new OpChartComponent(OpChartComponent.LINE_CHART_BOX);
         value = attributes.get(MARKERS);
         if ((value != null) && (value instanceof String)) {
            if (value.equals("true")) {
               component.setMarkers(true);
            }
            else if (value.equals("false")) {
               component.setMarkers(false);
            }
         }
         _parseChartBoxAttributes(component, attributes);
      }
      else if (BAR_CHART_BOX.equals(name)) {
         component = new OpChartComponent(OpChartComponent.BAR_CHART_BOX);
         _parseChartBoxAttributes(component, attributes);
         _parseOrientationAttribute(component, attributes);
      }
      else if (PIPELINE_CHART_BOX.equals(name)) {
         component = new OpChartComponent(OpChartComponent.PIPELINE_CHART_BOX);
         _parseChartBoxAttributes(component, attributes);         
      }
      parseCommonAttributes(component, attributes);
      return component;
   }

   protected void _parseChartBoxAttributes(OpChartComponent component, HashMap attributes) {
      Object value = attributes.get(DATA_SET_REF);
      if ((value != null) && (value instanceof String)) {
         component.setDataSetRef((String) value);
      }
      _parseValueTypeAttribute(component, attributes);
      value = attributes.get(DATA_UNIT);
      if ((value != null) && (value instanceof String)) {
         component.setDataUnit(Float.parseFloat((String) value));
      }
      value = attributes.get(DATA_COLUMN_INDEX);
      if ((value != null) && (value instanceof String)) {
         component.setDataColumnIndex(Integer.parseInt((String) value));
      }
      value = attributes.get(DATA_COLUMN_COUNT);
      if ((value != null) && (value instanceof String)) {
         component.setDataColumnCount(Integer.parseInt((String) value));
      }
      value = attributes.get(GRID_LINES);
      if ((value != null) && (value instanceof String)) {
         if (value.equals("none")) {
            component.setGridLines(OpChartComponent.NONE);
         }
         else if (value.equals("horizontal")) {
            component.setGridLines(OpChartComponent.HORIZONTAL);
         }
         else if (value.equals("vertical")) {
            component.setGridLines(OpChartComponent.VERTICAL);
         }
         else if (value.equals("all")) {
            component.setGridLines(OpChartComponent.ALL);
         }
      }
      value = attributes.get(DATA_CAPTION_COLUMN_INDEX);
      if ((value != null) && (value instanceof String)) {
         component.setDataCaptionColumnIndex(Integer.parseInt((String) value));
      }
      value = attributes.get(DECIMALS);
      if ((value != null) && (value instanceof String)) {
         component.setDecimals(Integer.valueOf((String) value));
      }
   }

}
