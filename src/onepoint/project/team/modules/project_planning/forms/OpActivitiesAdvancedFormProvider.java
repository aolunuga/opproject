package onepoint.project.team.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.project_planning.forms.OpActivitiesFormProvider;
import onepoint.project.team.modules.project_planning.components.OpAdvancedProjectComponent;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpActivitiesAdvancedFormProvider extends OpActivitiesFormProvider {

   private final static String ACTIVITY_WBS_CHART = "ActivityWBSChart";
   private String WBS_TOOL_BAR = "WBSToolBar";
   private final static String CATEGORY_COLOR_DATA_SET = "CategoryColorDataSet";
   private final static String WBS_REDO_BUTTON = "wbsRedoButton";
   private final static String WBS_UNDO_BUTTON = "wbsUndoButton";
   
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      super.prepareForm(s, form, parameters);
   }


   protected void enableComponentsForNoOpenProject(XComponent form) {
      super.enableComponentsForNoOpenProject(form);
      form.findComponent(ACTIVITY_WBS_CHART).setEditMode(false);
      form.findComponent(ACTIVITY_WBS_CHART).setPopUpMenuRef(null);
   }

   protected void enableComponentsForProjectLocked(XComponent form) {
      super.enableComponentsForProjectLocked(form);
      form.findComponent(ACTIVITY_WBS_CHART).setEditMode(false);
   }


   protected void enableComponentsWhenUserOwner(XComponent form) {
      form.findComponent(WBS_TOOL_BAR).setVisible(true);
      XComponent wbsChart = form.findComponent(ACTIVITY_WBS_CHART);
      wbsChart.setEditMode(true);
      super.enableComponentsWhenUserOwner(form);
   }

   protected void setProjectRelatedSettings(XComponent form, OpProjectNode project) {
      //set up the project name on the wbs chart
      ((OpAdvancedProjectComponent)form.findComponent(ACTIVITY_WBS_CHART)).setProjectName(project.getName());
      super.setProjectRelatedSettings(form, project);
   }

   protected void addCategories(XComponent form, OpBroker broker) {
      super.addCategories(form, broker);
      // Add category color map
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.setID(CATEGORY_COLOR_DATA_SET);
      OpActivityDataSetFactory.fillCategoryColorDataSet(broker, dataSet);
      OpProjectComponent ganttChart = (OpProjectComponent) form.findComponent(ACTIVITY_GANTT_CHART);
      ganttChart.setCategoryColorSetRef(CATEGORY_COLOR_DATA_SET);
      form.addChild(dataSet);
   }

   protected void registerEventHandlersForButtons(XComponent form, XComponent activityDataSet) {
      XComponent button;
      button = form.findComponent(WBS_UNDO_BUTTON);
      activityDataSet.registerEventHandler(button , XComponent.COMPONENT_EVENT);
      button = form.findComponent(WBS_REDO_BUTTON);
      activityDataSet.registerEventHandler(button , XComponent.COMPONENT_EVENT);
      super.registerEventHandlersForButtons(form, activityDataSet);
   }

}
