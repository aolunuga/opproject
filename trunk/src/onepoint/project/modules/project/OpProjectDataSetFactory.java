/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocalizer;

import java.util.*;

public final class OpProjectDataSetFactory {

   public final static int DESCRIPTOR_COLUMN_INDEX = 0;

   public final static String PORTFOLIO_DESCRIPTOR = "f";
   public final static String TEMPLATE_DESCRIPTOR = "t";
   public final static String PROJECT_DESCRIPTOR = "p";

   public final static int PORTFOLIO_ICON_INDEX = 0;
   public final static int TEMPLATE_ICON_INDEX = 1;
   public final static int PROJECT_ICON_INDEX = 2;

   // Type filter
   // FIXME(dfreis Mar 22, 2007 3:04:24 PM)
   // should really be the same as in OpProjectNode!!!!
   
   public final static int PORTFOLIOS = 1;
   public final static int TEMPLATES = 2;
   public final static int PROJECTS = 4;
   public final static int PROGRAMS = 8;
   public final static int ALL_TYPES = PORTFOLIOS + TEMPLATES + PROJECTS + PROGRAMS;

   public final static int ALL_PROJECT_NODE_TYPES = OpProjectNode.PORTFOLIO + 
                                                    OpProjectNode.TEMPLATE + 
                                                    OpProjectNode.PROJECT + 
                                                    OpProjectNode.PROGRAM;
   
   public final static String PROJECT_OBJECTS = "project.objects";

   public final static String ENABLE_PROJECTS = "EnableProjects";
   public final static String ENABLE_PORTFOLIOS = "EnablePortfolios";
   public final static String ENABLE_TEMPLATES = "EnableTemplates";
   public final static String FILTERED_OUT_IDS = "FilteredOutIds";

   private final static String PROJECT_BY_PERMISSIONS_QUERY =
        "select project.ID from OpPermission as permission, OpProjectNode as project " +
             "where permission.Object.ID = project.ID " +
             "and permission.Subject.ID in (:subjectIds) " +
             "and project.Type in (:projectTypes)" +
             "and permission.AccessLevel in (:levels) " +
             "group by project.ID";


   /**
    * Utility class.
    */
   private OpProjectDataSetFactory() {
   }

   /**
    * Retrieves all the project nodes with the given type from the db.
    *
    * @param session a <code>OpProjectSession</code> representing the server session.
    * @param broker  an <code>OpBroker</code> used for performing business operations.
    * @param dataSet a <code>XComponent(DATA_SET)</code> representing the project data.
    * @param types   a <code>int</code> representing a filter that allows to select only certain types of projects.
    * @param tabular a <code>boolean</code> indicating whether the structure to retrieve should have tabular structure or not.
    */
   public static void retrieveProjectDataSet(OpProjectSession session, OpBroker broker, XComponent dataSet, int types,
        boolean tabular) {
      // Localizer is used in order to localize name and description of root project portfolio
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(PROJECT_OBJECTS));
      addSubPortfolioRows(session, broker, dataSet, localizer, -1, 0, types, tabular);
   }

   /**
    * Adds rows of sub-portfolios to the given data set.
    */
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
         XComponent dataRow = createProjectNodeAdvancedRow(session, broker, subPortfolio, localizer, tabular, outlineLevel);
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

   /**
    * Adds rows of templates to the given dataset.
    */
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

   /**
    * Adds rows of projects to the given dataset.
    */
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
         XComponent dataRow = createProjectNodeBasicRow(session, broker, projectNode, localizer, tabular, outlineLevel);
         dataSet.addChild(dataRow);
      }
   }

   /**
    * Creates a data row from the given project node.
    *
    * @return a <code>XComponent(DATA_ROW)</code>.
    * @see OpProjectDataSetFactory#populateProjectNodeDataSet(onepoint.project.OpProjectSession,onepoint.persistence.OpBroker,
    *onepoint.persistence.OpQuery,onepoint.resource.XLocalizer,boolean,int,onepoint.express.XComponent)
    */
   private static XComponent createProjectNodeBasicRow(OpProjectSession session, OpBroker broker, OpProjectNode projectNode,
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

   /**
    * Retrieves only the first 2 levels of project nodes from the db. This method is used primarily together with lazy loading.
    *
    * @param session     a <code>OpProjectSession</code> representing the server session.
    * @param dataSet     a <code>XComponent(DATA_SET)</code> representing the data-set which will be populated.
    * @param types       a <code>int</code> representing a filter of project types.
    * @param tabular     a <code>boolean</code> indicating whether the retrieved structure should be tabular (will contain more info).
    * @param idsToFilter a <code>List</code> of <code>String</code> representing locator strings for ids to filter out (children that should be retrieved).
    */
   public static void retrieveProjectDataSetRootHierarchy(OpProjectSession session, XComponent dataSet, int types,
        boolean tabular, List idsToFilter) {
      OpBroker broker = session.newBroker();

      // Localizer is used in order to localize name and description of root project portfolio
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(PROJECT_OBJECTS));

      Map projectNodes = getProjectNodes(types, broker, -1);
      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.PROJECT_NODE, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator it = session.accessibleObjects(broker, projectNodes.keySet(), OpPermission.OBSERVER, order);
      while (it.hasNext()) {
         OpProjectNode projectNode = (OpProjectNode) it.next();
         String locatorString = OpLocator.locatorString(projectNode);
         if (idsToFilter != null && idsToFilter.contains(locatorString)) {
            continue;
         }

         XComponent dataRow = createProjectNodeAdvancedRow(session, broker, projectNode, localizer, tabular, 0);
         dataSet.addChild(dataRow);

         long childCount = ((Number) projectNodes.get(new Long(projectNode.getID()))).longValue();
         boolean childrenAdded = false;
         if (childCount > 0) {
            List firstLevelChildren = retrieveProjectNodeChildren(session, dataRow, types, tabular, idsToFilter);
            for (int i = 0; i < firstLevelChildren.size(); i++) {
               XComponent child = (XComponent) firstLevelChildren.get(i);
               dataSet.addChild(child);
               childrenAdded = true;
            }
         }

         if (childrenAdded) {
            //mark the level 0 nodes as expanded (if they show any children)
            XComponent expandedDataCell = new XComponent(XComponent.DATA_CELL);
            expandedDataCell.setBooleanValue(true);
            dataRow.addChild(expandedDataCell);
         }
      }
      broker.close();
   }

   /**
    * Retrieves the direct descendants of the given parent project node. This is used with dynamic loading.
    *
    * @param session     a <code>OpProjectSession</code> representing the server session.
    * @param parentNode  a <code>XComponent(DATA_ROW)</code> representing the client-view of a project node.
    * @param types       a <code>int</code> used for filtering project nodes.
    * @param tabular     a <code>boolean</code> indicating whether the retrieved structure should be tabular (will contain more info).
    * @param idsToFilter a <code>List</code> of <code>String</code> representing locator strings for ids to filter out (children that should be retrieved).
    * @return a <code>List</code> of <code>XComponent(DATA_ROW)</code> representing the direct descendants of the given data-row.
    */
   public static List retrieveProjectNodeChildren(OpProjectSession session, XComponent parentNode, int types,
        boolean tabular, List idsToFilter) {
      List result = new ArrayList();
      if (types <= 0) {
         return result;
      }
      OpBroker broker = session.newBroker();

      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(PROJECT_OBJECTS));

      String parentLocator = tabular ? parentNode.getStringValue() : XValidator.choiceID(parentNode.getStringValue());
      long parentId = OpLocator.parseLocator(parentLocator).getID();

      Map projectNodes = getProjectNodes(types, broker, parentId);
      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.PROJECT_NODE, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator it = session.accessibleObjects(broker, projectNodes.keySet(), OpPermission.OBSERVER, order);

      int outlineLevel = parentNode.getOutlineLevel() + 1;
      while (it.hasNext()) {
         OpProjectNode projectNode = (OpProjectNode) it.next();
         String locatorString = OpLocator.locatorString(projectNode);
         if (idsToFilter != null && idsToFilter.contains(locatorString)) {
            continue;
         }

         XComponent dataRow = createProjectNodeAdvancedRow(session, broker, projectNode, localizer, tabular, outlineLevel);
         result.add(dataRow);
         int childCount = ((Number) projectNodes.get(new Long(projectNode.getID()))).intValue();
         if (projectNode.getType() == OpProjectNode.PORTFOLIO && childCount > 0 && hasNonFilteredChildren(projectNode, idsToFilter)) {
            XComponent dummyChild = createDummyChild(dataRow);
            result.add(dummyChild);
         }
      }
      broker.close();
      return result;
   }

   /**
    * Checks if the given portfolio has any children which aren't filtered.
    *
    * @param projectNode a <code>OpProjectNode</code> representing the portfolio.
    * @param idsToFilter a <code>List</code> of <code>String</code> representing project locators.
    * @return true if the portfolio has any children which shouldn't be filtered.
    */
   private static boolean hasNonFilteredChildren(OpProjectNode projectNode, List idsToFilter) {
      if (idsToFilter == null) {
         return true;
      }
      boolean hasNonFilteredChildren = false;
      Iterator it = projectNode.getSubNodes().iterator();
      while (it.hasNext()) {
         OpProjectNode child = (OpProjectNode) it.next();
         String locatorString = OpLocator.locatorString(child);
         hasNonFilteredChildren |= !idsToFilter.contains(locatorString);
      }
      return hasNonFilteredChildren;
   }

   /**
    * Returns a map with all the project nodes of a given type, and their nr. of children.
    *
    * @param types    a <code>int</code> representing the types of project nodes to retrieve.
    * @param broker   a <code>OpBroker</code> used for performing business operations.
    * @param parentId a <code>long</code> representing the id of parent node, or -1 if top-level projects should be retrieved.
    * @return a <code>Map</code> of <code>Long,Number</code> pairs representing [id,childCount] pairs.
    */
   private static Map getProjectNodes(int types, OpBroker broker, long parentId) {
      StringBuffer queryString = new StringBuffer("select projectNode.ID from OpProjectNode as projectNode where ");
      if (parentId != -1) {
         queryString.append("projectNode.SuperNode.ID = ?");
      }
      else {
         queryString.append("projectNode.SuperNode is null");
      }

      List typeParams = new ArrayList();
      queryString.append(" and ( ");
      boolean filterAdded = false;
      if ((types & PORTFOLIOS) == PORTFOLIOS) {
         queryString.append("projectNode.Type = ?");
         typeParams.add(new Byte(OpProjectNode.PORTFOLIO));
         filterAdded = true;
      }

      if ((types & PROJECTS) == PROJECTS) {
         if (filterAdded) {
            queryString.append(" or ");
         }
         queryString.append(" projectNode.Type = ?");
         typeParams.add(new Byte(OpProjectNode.PROJECT));
         filterAdded = true;
      }

      if ((types & TEMPLATES) == TEMPLATES) {
         if (filterAdded) {
            queryString.append(" or ");
         }
         queryString.append(" projectNode.Type = ?");
         typeParams.add(new Byte(OpProjectNode.TEMPLATE));
      }
      queryString.append(" )");
      OpQuery query = broker.newQuery(queryString.toString());

      //set the query parameters (based on the filtered types)
      int paramStartIndex = 1;
      if (parentId != -1) {
         query.setLong(0, parentId);
      }
      else {
         paramStartIndex = 0;
      }
      for (int i = 0; i < typeParams.size(); i++) {
         query.setByte(paramStartIndex++, ((Byte) typeParams.get(i)).byteValue());
      }

      Map result = new HashMap();
      Iterator queryResultIterator = broker.list(query).iterator();
      while (queryResultIterator.hasNext()) {
         Long id = (Long) queryResultIterator.next();

         StringBuffer childCountQuery = new StringBuffer("select count(subNode.ID) from OpProjectNode parentNode left join parentNode.SubNodes subNode");
         childCountQuery.append(" where parentNode.ID=? ");
         childCountQuery.append(" and (");
         filterAdded = false;
         if ((types & PORTFOLIOS) == PORTFOLIOS) {
            childCountQuery.append("subNode.Type = ?");
            filterAdded = true;
         }
         if ((types & PROJECTS) == PROJECTS) {
            if (filterAdded) {
               childCountQuery.append(" or ");
            }
            childCountQuery.append(" subNode.Type = ?");
            filterAdded = true;
         }
         if ((types & TEMPLATES) == TEMPLATES) {
            if (filterAdded) {
               childCountQuery.append(" or ");
            }
            childCountQuery.append(" subNode.Type = ?");
         }
         childCountQuery.append(")");
         childCountQuery.append(" group by parentNode.ID");

         OpQuery childQuery = broker.newQuery(childCountQuery.toString());
         childQuery.setLong(0, id.longValue());
         for (int i = 0; i < typeParams.size(); i++) {
            childQuery.setByte(i + 1, ((Byte) typeParams.get(i)).byteValue());
         }
         Long count = new Long(0);
         Iterator childCountIterator = broker.list(childQuery).iterator();
         if (childCountIterator.hasNext()) {
            count = new Long(((Number) childCountIterator.next()).longValue());
         }
         result.put(id, count);
      }
      return result;
   }

   /**
    * Creates a data row from the given project node.
    *
    * @return a <code>XComponent(DATA_ROW)</code>.
    * @see OpProjectDataSetFactory#populateProjectNodeDataSet(onepoint.project.OpProjectSession,onepoint.persistence.OpBroker,
    *onepoint.persistence.OpQuery,onepoint.resource.XLocalizer,boolean,int,onepoint.express.XComponent)
    */
   private static XComponent createProjectNodeAdvancedRow(OpProjectSession session, OpBroker broker, OpProjectNode projectNode,
        XLocalizer localizer, boolean tabular, int outlineLevel) {
      XComponent dataRow = createProjectNodeBasicRow(session, broker, projectNode, localizer, tabular, outlineLevel);

      //only for tabular structures we add extra data
      if (tabular) {
         ArrayList activityTypes = new ArrayList();
         activityTypes.add(new Byte(OpActivity.STANDARD));
         activityTypes.add(new Byte(OpActivity.COLLECTION));
         activityTypes.add(new Byte(OpActivity.SCHEDULED_TASK));
         activityTypes.add(new Byte(OpActivity.TASK));
         activityTypes.add(new Byte(OpActivity.COLLECTION_TASK));

         //6 - Completed
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         if (isOfType(dataRow, PROJECT_DESCRIPTOR)) {
            double value = getCompletedValue(broker, projectNode.getID(), activityTypes);
            dataCell.setDoubleValue(value);
         }
         dataRow.addChild(dataCell);

         //7 - Resources
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (isOfType(dataRow, PROJECT_DESCRIPTOR)) {
            double value = getResourcesValue(broker, projectNode.getID(), activityTypes);
            dataCell.setDoubleValue(value);
         }
         dataRow.addChild(dataCell);

         //8 - Costs
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (isOfType(dataRow, PROJECT_DESCRIPTOR)) {
            double value = getCostsValue(broker, projectNode.getID(), activityTypes);
            dataCell.setDoubleValue(value);
         }
         dataRow.addChild(dataCell);
      }
      return dataRow;
   }

   /**
    * Computes the overall complete value for a project, based on its activities.
    *
    * @param broker        a <code>OpBroker</code> used for perfroming business operations.
    * @param projectId     a <code>long</code> representing the id of the project for which the calculations are done.
    * @param activityTypes a <code>List</code> of <code>int</code> representing the types of activities to take into account.
    * @return a <code>double</code> value representing the completness of the project.
    */
   public static double getCompletedValue(OpBroker broker, long projectId, List activityTypes) {
      StringBuffer queryBuffer = new StringBuffer("select sum(activity.Complete * activity.Duration),  sum(activity.Duration)");
      queryBuffer.append(" from OpProjectNode as project inner join project.Plan as plan inner join plan.Activities as activity");
      queryBuffer.append(" where project.ID = :projectId and activity.OutlineLevel = 0 and activity.Type in (:activityTypes) and activity.Deleted = false group by project.ID");
      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setLong("projectId", projectId);
      query.setCollection("activityTypes", activityTypes);
      List completes = broker.list(query);
      Object[] record;
      for (int i = 0; i < completes.size(); i++) {
         record = (Object[]) completes.get(i);
         Double sum1 = (Double) record[0];
         Double sum2 = (Double) record[1];
         if (sum1 != null && sum2 != null) {
            double value = Double.MAX_VALUE;
            if (sum1.doubleValue() == 0 && sum2.doubleValue() == 0) {
               value = 0;
            }
            else if (sum2.doubleValue() != 0) {
               value = sum1.doubleValue() / sum2.doubleValue();
            }
            return value;
         }
      }
      return 0;
   }

   /**
    * Computes the value of the efort for the project, based on the effort of its resources.
    *
    * @param broker        a <code>OpBroker</code> used for perfroming business operations.
    * @param projectId     a <code>long</code> representing the id of the project for which the calculations are done.
    * @param activityTypes a <code>List</code> of <code>int</code> representing the types of activities to take into account.
    * @return a <code>double</code> representing the value of the efforts of the resources assigned on the project.
    */
   public static double getResourcesValue(OpBroker broker, long projectId, List activityTypes) {
      StringBuffer queryBuffer = new StringBuffer("select sum(activity.ActualEffort), sum(activity.BaseEffort)");
      queryBuffer.append(" from OpProjectNode as project inner join project.Plan as plan inner join plan.Activities as activity");
      queryBuffer.append(" where project.ID = :projectId  and activity.OutlineLevel = 0 and activity.Type in (:activityTypes) and activity.Deleted = false group by project.ID");
      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setLong("projectId", projectId);
      query.setCollection("activityTypes", activityTypes);
      List resources = broker.list(query);
      for (int i = 0; i < resources.size(); i++) {
         Object[] record = (Object[]) resources.get(i);
         Double sum1 = (Double) record[0];
         Double sum2 = (Double) record[1];
         if (sum1 != null && sum2 != null) {
            double value = Double.MAX_VALUE;
            if (sum1.doubleValue() == 0 && sum2.doubleValue() == 0) {
               value = 0;
            }
            else if (sum2.doubleValue() != 0) {
               value = sum1.doubleValue() / sum2.doubleValue() * 100;
            }
            return value;
         }
      }
      return 0;
   }

   /**
    * Computes the value of the efort for the project, based on the effort of its resources.
    *
    * @param broker        a <code>OpBroker</code> used for perfroming business operations.
    * @param projectId     a <code>long</code> representing the id of the project for which the calculations are done.
    * @param activityTypes a <code>List</code> of <code>int</code> representing the types of activities to take into account.
    * @return a <code>double</code> representing the value of the efforts of the resources assigned on the project.
    */
   public static double getCostsValue(OpBroker broker, long projectId, List activityTypes) {
      StringBuffer queryBuffer = new StringBuffer("select sum(activity.ActualPersonnelCosts + activity.ActualTravelCosts + activity.ActualMaterialCosts + activity.ActualExternalCosts + activity.ActualMiscellaneousCosts)");
      queryBuffer.append(" , sum(activity.BasePersonnelCosts + activity.BaseTravelCosts + activity.BaseMaterialCosts + activity.BaseExternalCosts + activity.BaseMiscellaneousCosts)");
      queryBuffer.append(" from OpProjectNode as project inner join project.Plan as plan inner join plan.Activities as activity");
      queryBuffer.append(" where project.ID  = :projectId and activity.OutlineLevel = 0 and activity.Type in (:activityTypes) group by project.ID");

      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setLong("projectId", projectId);
      query.setCollection("activityTypes", activityTypes);
      List costs = broker.list(query);
      for (int i = 0; i < costs.size(); i++) {
         Object[] record = (Object[]) costs.get(i);
         Double sum1 = (Double) record[0];
         Double sum2 = (Double) record[1];
         if (sum1 != null && sum2 != null) {
            double value = Double.MAX_VALUE;
            if (sum1.doubleValue() == 0 && sum2.doubleValue() == 0) {
               value = 0;
            }
            else if (sum2.doubleValue() != 0) {
               value = sum1.doubleValue() / sum2.doubleValue() * 100;
            }
            return value;
         }
      }
      return 0;
   }

   /**
    * Checks if a given data-row represents an entity with the required type.
    *
    * @param dataRow a <code>XComponent(DATA_ROW)</code> representing a portfolio data-row.
    * @param type    a <code>String</code> constant representing possible types of a data-row.
    * @return <code>true</code> if the dataRow represents a portfolio.
    */
   private static boolean isOfType(XComponent dataRow, String type) {
      String descriptor = ((XComponent) dataRow.getChild(0)).getStringValue();
      if (descriptor.equalsIgnoreCase(type)) {
         return true;
      }
      return false;
   }

   /**
    * Creates a dummy data-row for the given parent row.
    *
    * @param dataRow a <code>XComponent(DATA_ROW)</code> that represents a parent row.
    * @return a <code>XComponent(DATA_ROW)</code> representing a dummy child.
    */
   private static XComponent createDummyChild(XComponent dataRow) {
      XComponent dummyChild = new XComponent(XComponent.DATA_ROW);
      dummyChild.setOutlineLevel(dataRow.getOutlineLevel() + 1);
      dummyChild.setStringValue(OpProjectConstants.DUMMY_ROW_ID);
      dummyChild.setVisible(false);
      dummyChild.setEnabled(false);
      dummyChild.setSelectable(false);
      return dummyChild;
   }

   /**
    * Performs enabling or disabling (selection wise) of various project nodes, based on the request parameters.
    *
    * @param parameters a <code>Map</code> of String,Object pairs representing the request parameters.
    * @param dataSet    a <code>XComponent(DATA_SET)</code> representing the project node structure.
    */
   public static void enableNodes(Map parameters, XComponent dataSet) {
      List dataRows = new ArrayList(dataSet.getChildCount());
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRows.add(dataSet.getChild(i));
      }
      enableNodes(parameters, dataRows);
   }

   /**
    * Performs enabling or disabling (selection wise) of various project nodes, based on the request parameters.
    *
    * @param parameters a <code>Map</code> of String,Object pairs representing the request parameters.
    * @param dataRows   a <code>List</code> of data rows representing project nodes.
    */
   public static void enableNodes(Map parameters, List dataRows) {
      boolean enablePortfolios = ((Boolean) parameters.get(ENABLE_PORTFOLIOS)).booleanValue();
      boolean enableTemplates = ((Boolean) parameters.get(ENABLE_TEMPLATES)).booleanValue();
      boolean enableProjects = ((Boolean) parameters.get(ENABLE_PROJECTS)).booleanValue();
      for (Iterator it = dataRows.iterator(); it.hasNext();) {
         XComponent dataRow = (XComponent) it.next();
         String choice = dataRow.getStringValue();
         //<FIXME author="Horia Chiorean" description="Using the icon index as a denominator is not the best choice">
         int iconIndex = XValidator.choiceIconIndex(choice);
         //<FIXME>
         switch (iconIndex) {
            case OpProjectDataSetFactory.PROJECT_ICON_INDEX: {
               if (!enableProjects) {
                  dataRow.setSelectable(false);
               }
               break;
            }
            case OpProjectDataSetFactory.PORTFOLIO_ICON_INDEX: {
               if (!enablePortfolios) {
                  dataRow.setSelectable(false);
               }
               break;
            }
            case OpProjectDataSetFactory.TEMPLATE_ICON_INDEX: {
               if (!enableTemplates) {
                  dataRow.setSelectable(false);
               }
               break;
            }
         }
      }
   }

   public static Iterator getProjectStatusIterator(OpBroker broker) {
      //configure project status sort order
      Map sortOrder = new HashMap(1);
      sortOrder.put(OpProjectStatus.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
      OpObjectOrderCriteria categoryOrderCriteria = new OpObjectOrderCriteria(OpProjectStatus.PROJECT_STATUS, sortOrder);
      OpQuery query = broker.newQuery("select status from OpProjectStatus as status where status.Active=true " + categoryOrderCriteria.toHibernateQueryString("status"));
      Iterator projectStatusItr = broker.iterate(query);
      return projectStatusItr;
   }

   /**
    * Counts the number of project nodes of the given type.
    *
    * @param type   a <code>int</code> representing the project node type discriminator.
    * @param broker a <code>OpBroker</code> user for business operations.
    * @return a <code>int</code> representing the number of project nodes of the given type.
    */
   public static int countProjectNode(byte type, OpBroker broker) {
      String queryString = "select count(projectNode) from OpProjectNode projectNode where projectNode.Type=?";
      OpQuery query = broker.newQuery(queryString);
      query.setByte(0, type);
      Number result = new Integer(0);
      Iterator it = broker.list(query).iterator();
      while (it.hasNext()) {
         result = (Number) it.next();
      }
      return result.intValue();
   }


   /**
    * Gets the list of project ids for the given user and chosen permission role
    *
    * @param broker  Broker used for db access.
    * @param levels  List of permission levels (Byte) to include in the search
    * @param session current session
    * @return List of project ids.
    */
   public static List getProjectsByPermissions(OpProjectSession session, OpBroker broker, List levels) {
      OpQuery query = broker.newQuery(PROJECT_BY_PERMISSIONS_QUERY);
      List subjectIds = session.getSubjectIds();

      query.setCollection("subjectIds", subjectIds);
      List types = new ArrayList();
      types.add(new Byte(OpProjectNode.PROJECT));
      query.setCollection("projectTypes", types);
      query.setCollection("levels", levels);
      return broker.list(query);
   }

   /**
    * Creates a map of projects->resources for the current session user taking into account also his permissions over
    * the resources.
    * Gets a set containing all {@link OpProjectNode}s 
    * (Portfolios, Projects and/or Templates) representing 
    * direct children of the given parent. 
    * @param broker the broker to perform any operation.
    * @param types bitset, one of (
    * @param parentId the id of the parent to get child nodes.
    * @return a set containing all children nodes.
    * @pre none
    * @post none
    */
   
   public static Set<OpProjectNode> getProjectNodes(OpBroker broker, int types, long parentId) {
     StringBuffer queryString = new StringBuffer("select projectNode from OpProjectNode as projectNode where ");
     if (parentId != -1) {
       queryString.append("projectNode.SuperNode.ID = ?");
     }
     else {
       queryString.append("projectNode.SuperNode is null");
     }

     List<Byte> typeParams = new ArrayList<Byte>();
     boolean filterAdded = false;

     if ((types & ALL_PROJECT_NODE_TYPES) == ALL_PROJECT_NODE_TYPES) 
     {
     }
     else
     {
       queryString.append(" and (");
       if ((types & OpProjectNode.PORTFOLIO) == OpProjectNode.PORTFOLIO) {
         queryString.append(" projectNode.Type = ?");
         typeParams.add(new Byte(OpProjectNode.PORTFOLIO));
         filterAdded = true;
       }
       if ((types & OpProjectNode.PROJECT) == OpProjectNode.PROJECT) {
         if (filterAdded) {
           queryString.append(" or ");
         }
         queryString.append(" projectNode.Type = ?");
         typeParams.add(new Byte(OpProjectNode.PROJECT));
         filterAdded = true;
       }

       if ((types & OpProjectNode.TEMPLATE) == OpProjectNode.TEMPLATE) {
         if (filterAdded) {
           queryString.append(" or ");
         }
         queryString.append(" projectNode.Type = ?");
         typeParams.add(new Byte(OpProjectNode.TEMPLATE));
       }
       
       queryString.append(" )");
     }
     OpQuery query = broker.newQuery(queryString.toString());

     //set the query parameters (based on the filtered types)
     // set parent id
     int paramStartIndex = 0;
     if (parentId != -1) {
       query.setLong(0, parentId);
       ++paramStartIndex;
     }
     for (int i = 0; i < typeParams.size(); i++) {
       query.setByte(paramStartIndex++, ((Byte) typeParams.get(i)).byteValue());
     }
     
     List children = broker.list(query);
     Iterator childrenIterator = children.iterator();
     OpProjectNode node;
     HashSet<OpProjectNode> result = new HashSet<OpProjectNode>(children.size());
     while (childrenIterator.hasNext()) {
       node = (OpProjectNode)childrenIterator.next();
       result.add(node);
     }
     return result;
   }

   /**
    * Creates a map of projects->resources for the current session user taking into account his permissions over the projects.
    *
    * @param session Current project session (used for db access and current user)
    * @return Map of key: project_locator/project_name choice -> value: List of resource_locator/resource_name choices
    */
   public static Map getProjectToResourceMap(OpProjectSession session) {
      Map projectsMap = new HashMap();
      OpBroker broker = session.newBroker();
      long userId = session.getUserID();
      List types = new ArrayList();

      //add all the projects the user has access to (at least CONTRIBUTOR)
      types.add(new Byte(OpPermission.CONTRIBUTOR));
      types.add(new Byte(OpPermission.ADMINISTRATOR));
      types.add(new Byte(OpPermission.MANAGER));

      List projects = getProjectsByPermissions(session, broker, types);
      for (int i = 0; i < projects.size(); i++) {
         Long id = (Long) projects.get(i);
         OpProjectNode project = (OpProjectNode) broker.getObject(OpProjectNode.class, id.longValue());
         Set assignments = project.getAssignments();
         List resources = new ArrayList();
         for (Iterator iterator = assignments.iterator(); iterator.hasNext();) {
            OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) iterator.next();
            OpResource resource = assignment.getResource();
            if (resource.getUser() != null && resource.getUser().getID() == userId) {
               //responsible user for this resource
               resources.add(XValidator.choice(resource.locator(), resource.getName()));
            }
            else {
               if (session.effectiveAccessLevel(broker, resource.getID()) >= OpPermission.MANAGER) {
                  //the user is at least manager on the resource
                  resources.add(XValidator.choice(resource.locator(), resource.getName()));
               }
            }
         }
         if (!resources.isEmpty()) {
            projectsMap.put(XValidator.choice(project.locator(), project.getName()), resources);
         }
      }
      broker.close();
      return projectsMap;
   }
}
