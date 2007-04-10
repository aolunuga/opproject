package onepoint.project.team.modules.project_planning;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.project_planning.OpProjectComponentHandler;
import onepoint.project.team.modules.project_planning.components.OpAdvancedProjectComponent;
import onepoint.xml.XContext;

import java.util.HashMap;

/**
 * @author mihai.costin
 */
public class OpAdvancedProjectComponentHandler extends OpProjectComponentHandler {

   public final static String WBS_BOX = "wbs-box";

   private final static String EFFORT_TEXT = "effort_text";
   private final static String COSTS_TEXT = "costs_text";
   private final static String PROJECT_NAME = "project-name";   

   private static final XLog logger = XLogFactory.getLogger(OpAdvancedProjectComponentHandler.class, true);

   public Object newNode(XContext context, String name, HashMap attributes) {
      logger.debug("OpProjectComponentHandler.newNode() : name = " + name);
      OpAdvancedProjectComponent component;
      Object value;
      if (WBS_BOX.equals(name)) {
         component = new OpAdvancedProjectComponent(OpAdvancedProjectComponent.WBS_BOX);
         _parseScrollBoxAttributes(component, attributes);
         value = attributes.get(DATA_SET_REF);
         if ((value != null) && (value instanceof String)) {
            component.setDataSetRef((String) value);
         }
         value = attributes.get(EFFORT_TEXT);
         if ((value != null) && (value instanceof String)) {
            component.setEffortText((String) value);
         }
         value = attributes.get(COSTS_TEXT);
         if ((value != null) && (value instanceof String)) {
            component.setCostsText((String) value);
         }
         value = attributes.get(ON_ACTIVITY_DETAILS);
         if ((value != null) && (value instanceof String)) {
            component.setOnActivityDetails((String) value);
         }
         value = attributes.get(CATEGORY_COLOR_SET_REF);
         if ((value != null) && (value instanceof String)) {
            component.setCategoryColorSetRef((String) value);
         }
         value = attributes.get(PROJECT_NAME);
         if ((value != null) && (value instanceof String)) {
            component.setProjectName((String)value);
         }
      }
      else {
         return super.newNode(context, name, attributes);
      }

      parseCommonAttributes(component, attributes);
      return component;
   }
}
