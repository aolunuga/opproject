/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpPermission;
import onepoint.resource.XLocalizer;

import java.util.Iterator;

public final class OpProjectDataSetFactory {

   public final static int DESCRIPTOR_COLUMN_INDEX = 0;

   public final static String PORTFOLIO_DESCRIPTOR = "f";
   public final static String TEMPLATE_DESCRIPTOR = "t";
   public final static String PROJECT_DESCRIPTOR = "p";

   public final static int PORTFOLIO_ICON_INDEX = 0;
   public final static int TEMPLATE_ICON_INDEX = 1;
   public final static int PROJECT_ICON_INDEX = 2;

   // Type filter
   public final static int PORTFOLIOS = 1;
   public final static int TEMPLATES = 2;
   public final static int PROJECTS = 4;
   private final static int PROGRAMS = 8;
   public final static int ALL_TYPES = PORTFOLIOS + TEMPLATES + PROJECTS + PROGRAMS;

   public final static String PROJECT_OBJECTS = "project.objects";

   /**
    * Utility class.
    */
   private OpProjectDataSetFactory() {
   }

   public static void retrieveProjectDataSet(OpProjectSession session, OpBroker broker, XComponent dataSet, int types,
        boolean tabular) {
      // Localizer is used in order to localize name and description of root project portfolio
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(PROJECT_OBJECTS));
      addSubPortfolioRows(session, broker, dataSet, localizer, -1, 0, types, tabular);
      dataSet.synchronizeExpanded();
   }

   public static void retrieveProjectDataSetFlatStructure(OpProjectSession session, OpBroker broker, XComponent dataSet, int types,
        boolean tabular) {
      // Localizer is used in order to localize name and description of root project portfolio
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(PROJECT_OBJECTS));
      OpQuery query;
      if ((types & PROJECTS) == PROJECTS) {
         query = broker.newQuery("select project.ID from OpProjectNode as project where project.Type = ?");
         query.setByte(0, OpProjectNode.PROJECT);
         populateProjectNodeDataSet(session, broker, query, localizer, tabular, 0, dataSet);
      }
      if ((types & TEMPLATES) == TEMPLATES) {
         query = broker.newQuery("select project.ID from OpProjectNode as project where project.Type = ?");
         query.setByte(0, OpProjectNode.TEMPLATE);
         populateProjectNodeDataSet(session, broker, query, localizer, tabular, 0, dataSet);
      }
   }

   private static void addSubPortfolioRows(OpProjectSession session, OpBroker broker, XComponent dataSet,
        XLocalizer localizer, long superNodeId, int outlineLevel, int types, boolean tabular) {

      OpQuery query = null;
      if (superNodeId == -1) {
         query = broker
              .newQuery("select portfolio.ID from OpProjectNode as portfolio where portfolio.SuperNode.ID is null and portfolio.Type = ?");
         query.setByte(0, OpProjectNode.PORTFOLIO);
      }
      else {
         query = broker
              .newQuery("select portfolio.ID from OpProjectNode as portfolio where portfolio.SuperNode.ID = ? and portfolio.Type = ?");
         query.setLong(0, superNodeId);
         query.setByte(1, OpProjectNode.PORTFOLIO);
      }

      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.PROJECT_NODE, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator subPortfolios = session.accessibleObjects(broker, broker.list(query), OpPermission.OBSERVER, order);
      OpProjectNode subPortfolio = null;
      while (subPortfolios.hasNext()) {
         subPortfolio = (OpProjectNode) (subPortfolios.next());
         XComponent dataRow = createProjectNodeRow(session, broker, subPortfolio, localizer, tabular, outlineLevel);
         dataSet.addChild(dataRow);
         // Add sub-portfolios of this portfolio
         if ((types & PORTFOLIOS) == PORTFOLIOS) {
            addSubPortfolioRows(session, broker, dataSet, localizer, subPortfolio.getID(), outlineLevel + 1, types,
                 tabular);
         }
         // Add templates of this portfolio
         if ((types & TEMPLATES) == TEMPLATES) {
            addTemplateRows(session, broker, dataSet, localizer, subPortfolio.getID(), outlineLevel + 1, tabular);
         }
         // Add projects of this portfolio
         if ((types & PROJECTS) == PROJECTS) {
            addProjectRows(session, broker, dataSet, localizer, subPortfolio.getID(), outlineLevel + 1, tabular);
         }
      }
   }

   private static void addTemplateRows(OpProjectSession session, OpBroker broker, XComponent data_set,
        XLocalizer localizer, long superNodeId, int outline_level, boolean tabular) {

      OpQuery query = null;
      if (superNodeId == -1) {
         query = broker
              .newQuery("select template.ID from OpProjectNode as template where template.SuperNode.ID is null and template.Type = ?");
         query.setByte(0, OpProjectNode.TEMPLATE);
      }
      else {
         query = broker
              .newQuery("select template.ID from OpProjectNode as template where template.SuperNode.ID = ? and template.Type = ?");
         query.setLong(0, superNodeId);
         query.setByte(1, OpProjectNode.TEMPLATE);
      }
      populateProjectNodeDataSet(session, broker, query, localizer, tabular, outline_level, data_set);
   }

   private static void addProjectRows(OpProjectSession session, OpBroker broker, XComponent data_set,
        XLocalizer localizer, long superNodeId, int outline_level, boolean tabular) {

      OpQuery query = null;
      if (superNodeId == -1) {
         query = broker
              .newQuery("select project.ID from OpProjectNode as project where project.SuperNode.ID is null and project.Type = ?");
         query.setByte(0, OpProjectNode.PROJECT);
      }
      else {
         query = broker
              .newQuery("select project.ID from OpProjectNode as project where project.SuperNode.ID = ? and project.Type = ?");
         query.setLong(0, superNodeId);
         query.setByte(1, OpProjectNode.PROJECT);
      }
      populateProjectNodeDataSet(session, broker, query, localizer, tabular, outline_level, data_set);
   }

   /**
    * Creates a data row containing information from a project node.
    *
    * @param session      a <code>OpProjectSession</code> representing an application session.
    * @param broker       a <code>OpBroker</code> used for db operations.
    * @param query        a <code>OpQuery</code> representing the query which will retrieve project nodes.
    * @param localizer    a <code>XLocalizer</code> used for i18n operations.
    * @param tabular      a <code>boolean</code> indicating whether to retrieve entire data from project node or just id.
    * @param outlineLevel a <code>int</code> representing the outline level of the data row.
    * @param dataSet      a <code>XComponent(DATA_SET)</code> that will contain the results.
    */
   private static void populateProjectNodeDataSet(OpProjectSession session, OpBroker broker, OpQuery query,
        XLocalizer localizer, boolean tabular, int outlineLevel, XComponent dataSet) {

      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.PROJECT_NODE, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator it = session.accessibleObjects(broker, broker.list(query), OpPermission.OBSERVER, order);

      while (it.hasNext()) {
         OpProjectNode projectNode = (OpProjectNode) it.next();
         XComponent dataRow = createProjectNodeRow(session, broker, projectNode, localizer, tabular, outlineLevel);
         dataSet.addChild(dataRow);
      }
   }

   /**
    * Creates a data row from the given project node.
    *
    * @return a <code>XComponent(DATA_ROW)</code>.
    * @see OpProjectDataSetFactory#populateProjectNodeDataSet(onepoint.project.OpProjectSession, onepoint.persistence.OpBroker,
    *      onepoint.persistence.OpQuery, onepoint.resource.XLocalizer, boolean, int, onepoint.express.XComponent)
    */
   private static XComponent createProjectNodeRow(OpProjectSession session, OpBroker broker, OpProjectNode projectNode,
        XLocalizer localizer, boolean tabular, int outlineLevel) {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setOutlineLevel(outlineLevel);
      if (outlineLevel == 0) {
         dataRow.setExpanded(true);
      }

      //determine specific values based on type
      int iconIndex = -1;
      String descriptor = null;
      switch (projectNode.getType()) {
         case OpProjectNode.PROJECT: {
            iconIndex = PROJECT_ICON_INDEX;
            descriptor = PROJECT_DESCRIPTOR;
            break;
         }
         case OpProjectNode.PORTFOLIO: {
            iconIndex = PORTFOLIO_ICON_INDEX;
            descriptor = PORTFOLIO_DESCRIPTOR;
            break;
         }
         case OpProjectNode.TEMPLATE: {
            iconIndex = TEMPLATE_ICON_INDEX;
            descriptor = TEMPLATE_DESCRIPTOR;
            break;
         }
      }

      if (tabular) {
         //data row value - template id
         dataRow.setStringValue(projectNode.locator());

         //0 - descriptor
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(descriptor);
         dataRow.addChild(dataCell);

         //1 - choice [name, id]
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setValue(XValidator.choice(projectNode.locator(), localizer.localize(projectNode.getName()),
              iconIndex));
         dataRow.addChild(dataCell);

         //2 - description
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setValue(localizer.localize(projectNode.getDescription()));
         dataRow.addChild(dataCell);

         //3 - start date
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDateValue(projectNode.getStart());
         dataRow.addChild(dataCell);

         //4 - end date
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDateValue(projectNode.getFinish());
         dataRow.addChild(dataCell);

         //5 - effective permissions
         dataCell = new XComponent(XComponent.DATA_CELL);
         byte effectivePermission = session.effectiveAccessLevel(broker, projectNode.getID());
         dataCell.setByteValue(effectivePermission);
         dataRow.addChild(dataCell);
      }
      else {
         dataRow.setStringValue(XValidator.choice(projectNode.locator(), localizer.localize(projectNode.getName()),
              iconIndex));
      }
      return dataRow;
   }
}
