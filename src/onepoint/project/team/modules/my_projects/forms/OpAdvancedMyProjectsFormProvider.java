/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.my_projects.forms;

import onepoint.express.XComponent;
import onepoint.express.XStyle;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.my_projects.forms.OpMyProjectsFormProvider;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Form provider for my projects tool (advanced part).
 *
 * @author mihai.costin
 */
public class OpAdvancedMyProjectsFormProvider extends OpMyProjectsFormProvider {

   private static final String PIPELINE_DATA_SET = "PipelineDataSet";
   private static final String BUBBLES_COLOR_DATA_SET = "BubbleColors";
   private final static String RESOURCE_MAP = "my_projects.my_projects";
   private final static String UNDERUSED = "Underused";
   private final static String NORMALUSE = "Normalused";
   private final static String OVERUSED = "Overused";

   private static final double LOWER_LIMIT = 0.8;
   private static final double UPPER_LIMIT = 1.2;
   private int UNDERUSED_COLOR_INDEX = 0;
   private int USED_COLOR_INDEX = 1;
   private int OVERUSED_COLOR_INDEX = 2;


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      super.prepareForm(s, form, parameters);

      OpProjectSession session = (OpProjectSession) s;

      XComponent projectsDataSet = form.findComponent(PROJECTS_DATA_SET);
      Map projectMap = createProjectMap(projectsDataSet);

      //bubble color set
      XComponent colorDataSet = form.findComponent(BUBBLES_COLOR_DATA_SET);
      fillColorDataSet(session, colorDataSet);

      XComponent pipelineDataSet = form.findComponent(PIPELINE_DATA_SET);

      OpBroker broker = session.newBroker();

      //get active project status elements
      Map sortOrder = new HashMap(1);
      sortOrder.put(OpProjectStatus.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
      OpObjectOrderCriteria categoryOrderCriteria = new OpObjectOrderCriteria(OpProjectStatus.PROJECT_STATUS, sortOrder);
      OpQuery query = broker.newQuery("select status from OpProjectStatus as status where status.Active=true " + categoryOrderCriteria.toHibernateQueryString("status"));
      Iterator projectStatusItr = broker.iterate(query);

      while (projectStatusItr.hasNext()) {
         OpProjectStatus status = (OpProjectStatus) projectStatusItr.next();
         addStatusRow(status, pipelineDataSet, projectMap);
      }

      //add inactive status rows
      sortOrder = new HashMap(1);
      sortOrder.put(OpProjectStatus.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
      categoryOrderCriteria = new OpObjectOrderCriteria(OpProjectStatus.PROJECT_STATUS, sortOrder);
      query = broker.newQuery("select status from OpProjectStatus as status where status.Active=false " + categoryOrderCriteria.toHibernateQueryString("status"));
      projectStatusItr = broker.iterate(query);

      while (projectStatusItr.hasNext()) {
         OpProjectStatus status = (OpProjectStatus) projectStatusItr.next();
         addStatusRow(status, pipelineDataSet, projectMap);
      }

      broker.close();
   }

   private Map createProjectMap(XComponent projectsDataSet) {
      Map projectMap = new HashMap();
      for (int i = 0; i < projectsDataSet.getChildCount(); i++) {
         XComponent row = (XComponent) projectsDataSet.getChild(i);
         String locator = row.getStringValue();
         projectMap.put(locator, row);
      }
      return projectMap;
   }

   private void addStatusRow(OpProjectStatus status, XComponent pipelineDataSet, Map projectMap) {
      XComponent dataRow;
      XComponent dataCell;
      dataRow = new XComponent(XComponent.DATA_ROW);

      //status name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(status.getName());
      dataRow.addChild(dataCell);

      //status color
      dataCell = new XComponent(XComponent.DATA_CELL);
      if (status.getActive()) {
         dataCell.setValue(XStyle.colorSchema.get(status.getColor()));
      }
      else {
         dataCell.setValue(XStyle.DEFAULT_BACKGROUND);
      }
      dataRow.addChild(dataCell);

      //projects / bubbles
      dataCell = new XComponent(XComponent.DATA_CELL);
      XComponent bubblesDataSet = createProjectBubbles(status, projectMap);
      dataCell.setValue(bubblesDataSet);
      dataRow.addChild(dataCell);

      pipelineDataSet.addChild(dataRow);
   }

   private void fillColorDataSet(OpProjectSession session, XComponent colorDataSet) {
      XComponent dataRow;
      XComponent dataCell;

      XLanguageResourceMap map = session.getLocale().getResourceMap(RESOURCE_MAP);

      //green - UNDERUSED_COLOR_INDEX
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(UNDERUSED).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.DEFAULT_GREEN);
      dataRow.addChild(dataCell);
      colorDataSet.addChild(dataRow);

      //orange - USED_COLOR_INDEX
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(NORMALUSE).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.DEFAULT_BORDER);
      dataRow.addChild(dataCell);
      colorDataSet.addChild(dataRow);

      //red - OVERUSED_COLOR_INDEX
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(OVERUSED).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.DEFAULT_RED);
      dataRow.addChild(dataCell);
      colorDataSet.addChild(dataRow);
   }

   private XComponent createProjectBubbles(OpProjectStatus status, Map projectsMap) {
      XComponent projectsDataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow;
      XComponent dataCell;
      Set projects = status.getProjects();
      for (Iterator iterator = projects.iterator(); iterator.hasNext();) {
         OpProjectNode projectNode = (OpProjectNode) iterator.next();
         XComponent projectRow = (XComponent) projectsMap.get(projectNode.locator());
         if (projectRow != null) {

            double baseEffort = ((XComponent) projectRow.getChild(BASE_EFFORT_INDEX)).getDoubleValue();
            double baseCost = ((XComponent) projectRow.getChild(BASE_COST_INDEX)).getDoubleValue();
            double predictedEffort = ((XComponent) projectRow.getChild(PREDICTED_EFFORT_INDEX)).getDoubleValue();
            double predictedCost = ((XComponent) projectRow.getChild(PREDICTED_COSTS_INDEX)).getDoubleValue();

            dataRow = new XComponent(XComponent.DATA_ROW);

            //project size 0
            dataCell = new XComponent(XComponent.DATA_CELL);
            double size = baseEffort + baseCost;
            dataCell.setDoubleValue(size);
            dataRow.addChild(dataCell);

            //project name 1
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setStringValue(projectNode.getName());
            dataRow.addChild(dataCell);

            //project color index 2
            int colorIndex;
            if (predictedEffort < baseEffort * LOWER_LIMIT && predictedCost < baseCost * LOWER_LIMIT) {
               colorIndex = UNDERUSED_COLOR_INDEX;
            }
            else if (predictedCost > baseCost * UPPER_LIMIT || predictedEffort > baseEffort * UPPER_LIMIT) {
               colorIndex = OVERUSED_COLOR_INDEX;
            }
            else {
               colorIndex = USED_COLOR_INDEX;
            }

            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setIntValue(colorIndex);
            dataRow.addChild(dataCell);

            projectsDataSet.addChild(dataRow);
         }
      }

      //sort by name
      projectsDataSet.sort(1);

      return projectsDataSet;
   }
}
