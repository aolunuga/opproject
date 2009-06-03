/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.custom_attribute.OpCustomType;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpLock;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocalizer;
import onepoint.util.XEncodingHelper;
import onepoint.util.XEnvironmentManager;
import onepoint.util.XIOHelper;

public final class OpProjectDataSetFactory {

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
   public final static String NOT_SELECTABLE_IDS = "NotSelectableIds";

   public final static int ARCHIVED_INDEX = 23;

   public final static String PROJECT_DESCRIPTOR = "p";
   public final static String PORTFOLIO_DESCRIPTOR = "f";
   public final static String TEMPLATE_DESCRIPTOR = "t";

   private final static int PORTFOLIO_ICON_INDEX = 0;
   private final static int TEMPLATE_ICON_INDEX = 1;
   private final static int PROJECT_ICON_INDEX = 2;
   private final static int PROJECT_EDITED_ICON_INDEX = 3;
   private final static int PROJECT_LOCKED_ICON_INDEX = 4;
   private final static int TEMPLATE_EDITED_ICON_INDEX = 5;
   private final static int TEMPLATE_LOCKED_ICON_INDEX = 6;

   private final static int EXPANDED_FLAG_COLUMN_INDEX = 24;
   private static final int SIMPLE_EXPANDED_FLAG_COLUMN_INDEX = 1;

   private static final String GET_LOCK_COUNT_FOR_PROJECT_NODE =
        "select count(lock.id) from OpLock lock where lock.Target = (:project)";
   private static final String GET_ACTIVITY_COUNT_FOR_PROJECT_PLAN =
        "select count(activity.id) from OpActivity activity where activity.ProjectPlan = (:projectPlanId) and activity.Deleted = false";
   private static final String GET_PLAN_VERSION_COUNT_FOR_PROJECT_PLAN =
        "select count(planVersion.id) from OpProjectPlanVersion planVersion where planVersion.ProjectPlan = (:projectPlanId)";
   private static final String GET_PROJECT_COUNT_FOR_PROJECT_STATUS =
        "select count(project.id) from OpProjectNode project where project.Status = (:statusId)";

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
              .newQuery("select portfolio.id from OpProjectNode as portfolio where portfolio.SuperNode.id is null and portfolio.Type = ?");
         query.setByte(0, OpProjectNode.PORTFOLIO);
      }
      else {
         query = broker
              .newQuery("select portfolio.id from OpProjectNode as portfolio where portfolio.SuperNode.id = ? and portfolio.Type = ?");
         query.setLong(0, superNodeId);
         query.setByte(1, OpProjectNode.PORTFOLIO);
      }

      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.class, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator subPortfolios = session.accessibleObjects(broker, broker.list(query), OpPermission.OBSERVER, order);
      OpProjectNode subPortfolio = null;
      while (subPortfolios.hasNext()) {
         subPortfolio = (OpProjectNode) (subPortfolios.next());
         XComponent dataRow = createProjectNodeAdvancedRow(session, broker, subPortfolio, localizer, tabular, outlineLevel);
         dataSet.addChild(dataRow);
         // Add sub-portfolios of this portfolio
         if ((types & PORTFOLIOS) == PORTFOLIOS) {
            addSubPortfolioRows(session, broker, dataSet, localizer, subPortfolio.getId(), outlineLevel + 1, types,
                 tabular);
         }
         // Add templates of this portfolio
         if ((types & TEMPLATES) == TEMPLATES) {
            addTemplateRows(session, broker, dataSet, localizer, subPortfolio.getId(), outlineLevel + 1, tabular);
         }
         // Add projects of this portfolio
         if ((types & PROJECTS) == PROJECTS) {
            addProjectRows(session, broker, dataSet, localizer, subPortfolio.getId(), outlineLevel + 1, tabular);
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
              .newQuery("select template.id from OpProjectNode as template where template.SuperNode.id is null and template.Type = ?");
         query.setByte(0, OpProjectNode.TEMPLATE);
      }
      else {
         query = broker
              .newQuery("select template.id from OpProjectNode as template where template.SuperNode.id = ? and template.Type = ?");
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
              .newQuery("select project.id from OpProjectNode as project where project.SuperNode.id is null and project.Type = ?");
         query.setByte(0, OpProjectNode.PROJECT);
      }
      else {
         query = broker
              .newQuery("select project.id from OpProjectNode as project where project.SuperNode.id = ? and project.Type = ?");
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

      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.class, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
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
    *      onepoint.persistence.OpQuery,onepoint.resource.XLocalizer,boolean,int,onepoint.express.XComponent)
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
            if (projectNode.getLocks().size() > 0) {
               iconIndex = PROJECT_LOCKED_ICON_INDEX;
               for (OpLock lock : projectNode.getLocks()) {
                  if (lock.lockedByMe(session, broker)) {
                     iconIndex = PROJECT_EDITED_ICON_INDEX;
                     break;
                  }
               }
            }
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
            if (projectNode.getLocks().size() > 0) {
               iconIndex = TEMPLATE_LOCKED_ICON_INDEX;
               for (OpLock lock : projectNode.getLocks()) {
                  if (lock.lockedByMe(session, broker)) {
                     iconIndex = TEMPLATE_EDITED_ICON_INDEX;
                     break;
                  }
               }
            }
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
         byte effectivePermission = session.effectiveAccessLevel(broker, projectNode.getId());
         dataCell.setByteValue(effectivePermission);
         dataRow.addChild(dataCell);
      }
      else {
         dataRow.setStringValue(XValidator.choice(projectNode.locator(), localizer.localize(projectNode.getName()),
              iconIndex));

         //0 - descriptor
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(descriptor);
         dataRow.addChild(dataCell);
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
    * @param idsToFilter a <code>List</code> of <code>String</code> representing locator strings for ids to filter out (children that should not be retrieved).
    */
   public static void retrieveProjectDataSetRootHierarchy(OpProjectSession session, XComponent dataSet, int types,
        boolean tabular, Collection idsToFilter) {
      retrieveProjectDataSetRootHierarchy(session, dataSet, types, tabular, idsToFilter, null);
   }
   
      /**
    * Retrieves only the first 2 levels of project nodes from the db. This method is used primarily together with lazy loading.
    *
    * @param session     a <code>OpProjectSession</code> representing the server session.
    * @param dataSet     a <code>XComponent(DATA_SET)</code> representing the data-set which will be populated.
    * @param types       a <code>int</code> representing a filter of project types.
    * @param tabular     a <code>boolean</code> indicating whether the retrieved structure should be tabular (will contain more info).
    * @param idsToFilter a <code>List</code> of <code>String</code> representing locator strings for ids to filter out (children that should not be retrieved).
    */
   public static void retrieveProjectDataSetRootHierarchy(OpProjectSession session, XComponent dataSet, int types,
        boolean tabular, Collection idsToFilter, String selectedLocator) {
      OpBroker broker = session.newBroker();
      try {
         OpProjectNode[] selectedPath = null;
         int depth = 0;
         if (selectedLocator != null) {
            OpProjectNode node = (OpProjectNode) broker.getObject(selectedLocator);
            LinkedList<OpProjectNode> pathList = new LinkedList<OpProjectNode>();
            while (node != null) {
               pathList.addFirst(node);
               node = node.getSuperNode();
            }
            selectedPath = new OpProjectNode[pathList.size()];
            selectedPath = pathList.toArray(selectedPath);
         }
         // Localizer is used in order to localize name and description of root project portfolio
         XLocalizer localizer = new XLocalizer();
         localizer.setResourceMap(session.getLocale().getResourceMap(PROJECT_OBJECTS));

         Map projectNodes = getProjectNodes(types, broker, -1);
         OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.class, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
         Iterator it = session.accessibleObjects(broker, projectNodes.keySet(), OpPermission.OBSERVER, order);
         while (it.hasNext()) {
            OpProjectNode projectNode = (OpProjectNode) it.next();
            String locatorString = OpLocator.locatorString(projectNode);
            if (idsToFilter != null && idsToFilter.contains(locatorString)) {
               continue;
            }

            // OpProjectDataSetFactory.expandPath ..
            // OpProjectDataSetFactory.select ..
//            for (int pos = 0; pos < dataSet.getChildCount(); pos++) {
//               child = dataSet.getChild(pos);
//            }
            XComponent dataRow = createProjectNodeAdvancedRow(session, broker, projectNode, localizer, tabular, 0);
            dataSet.addChild(dataRow);
            long childCount = ((Number) projectNodes.get(new Long(projectNode.getId()))).longValue();
            if (childCount > 0 || (selectedPath != null && projectNode.equals(selectedPath[0]))) {
               expandNode(session, dataSet, types, tabular, idsToFilter, dataRow, selectedPath, 1);
            }
         }
      }
      finally {
         broker.close();
      }
   }

      private static void expandNode(OpProjectSession session,
            XComponent dataSet, int types, boolean tabular,
            Collection idsToFilter, XComponent dataRow, OpProjectNode[] selectedPath, int depth) {
         boolean childrenAdded = false;
         if (selectedPath != null && selectedPath.length == depth) {
            dataRow.setSelected(true);
            dataSet.setSelectedIndex(new Integer(dataRow.getIndex()));
         }
         List firstLevelChildren = retrieveProjectNodeChildren(session, dataRow, types, tabular, idsToFilter);
         for (int i = 0; i < firstLevelChildren.size(); i++) {
            XComponent child = (XComponent) firstLevelChildren.get(i);
            dataSet.addChild(child);
            childrenAdded = true;
            String childLocator = tabular ? child.getStringValue() : XValidator.choiceID(child.getStringValue());
            if (selectedPath != null && childLocator.equals(selectedPath[depth].locator())) {
               expandNode(session, dataSet, types, tabular, idsToFilter, child, selectedPath, depth+1);
               // remove dummy row
               if (i+1 < firstLevelChildren.size()) {
                  XComponent nextChild = (XComponent) firstLevelChildren.get(i+1);
                  if (OpProjectConstants.DUMMY_ROW_ID.equals(nextChild.getStringValue())) {
                     i++;
                  }
               }
            }
         }
         
         if (childrenAdded) {
            //mark the level 0 nodes as expanded (if they show any children)
            XComponent expandedDataCell = null;
            if (tabular) {
               expandedDataCell = (XComponent) dataRow.getChild(EXPANDED_FLAG_COLUMN_INDEX);
            }
            else {
               expandedDataCell = (XComponent) dataRow.getChild(SIMPLE_EXPANDED_FLAG_COLUMN_INDEX);
            }
            expandedDataCell.setBooleanValue(true);
            dataRow.setExpanded(true);
         }
      }

   /**
    * Returns all the projects located in the subtree (from the whole project hierarchy) for which the project (whose
    * id is passed as parameter) is the root.
    *
    * @param session      - the <code>OpProjectSession</code> object.
    * @param projectId    - the id of the project whose subprojects are loaded.
    * @param outlineLevel - the outline level of the project.
    * @return all the projects located in the subtree (from the whole project hierarchy) for which the project (whose
    *         id is passed as parameter) is the root.
    */
   public static List<XComponent> getAllSubprojects(OpProjectSession session, Long projectId, Integer outlineLevel) {
      // Localizer is used in order to localize name and description of root project portfolio
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(PROJECT_OBJECTS));

      List<XComponent> subProjectRows = new ArrayList<XComponent>();
      OpBroker broker = session.newBroker();
      Map projectNodes = getProjectNodes(ALL_TYPES, broker, projectId);
      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.class, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
      Iterator it = session.accessibleObjects(broker, projectNodes.keySet(), OpPermission.OBSERVER, order);
      while (it.hasNext()) {
         OpProjectNode projectNode = (OpProjectNode) it.next();
         XComponent dataRow = createProjectNodeAdvancedRow(session, broker, projectNode, localizer, true, outlineLevel + 1);
         subProjectRows.add(dataRow);

         long childCount = ((Number) projectNodes.get(new Long(projectNode.getId()))).longValue();
         if (childCount > 0) {
            List<XComponent> nodeChildren = getAllSubprojects(session, projectNode.getId(), outlineLevel + 1);
            subProjectRows.addAll(nodeChildren);

            //mark the data row as expanded (because it has children)
            XComponent expandedDataCell = new XComponent(XComponent.DATA_CELL);
            expandedDataCell.setBooleanValue(true);
            dataRow.addChild(expandedDataCell);
         }
      }
      return subProjectRows;
   }

   /**
    * Returns a list with the locators of all the project which are or aren't archived.
    *
    * @param session  a <code>OpProjectSession</code> a server session.
    * @param archived a <code>boolean</code> indicating whether to search for archived or non-archived projects.
    * @return a <code>List(String)</code> a list of project locators.
    */
   public static Collection<String> retrieveArchivedProjects(OpProjectSession session, boolean archived) {
      Collection<String> result = new HashSet<String>();
      OpBroker broker = session.newBroker();
      try {
         OpQuery projectsQuery = broker.newQuery("from OpProjectNode projectNode where projectNode.Type=:type and projectNode.Archived=:archived");
         projectsQuery.setParameter("type", OpProjectNode.PROJECT);
         projectsQuery.setParameter("archived", archived);
         for (Iterator it = broker.iterate(projectsQuery); it.hasNext();) {
            String locator = ((OpProjectNode) it.next()).locator();
            result.add(locator);
         }
      }
      finally {
         broker.close();
      }
      return result;
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
   public static List<XComponent> retrieveProjectNodeChildren(OpProjectSession session, XComponent parentNode, int types,
        boolean tabular, Collection idsToFilter) {
      List<XComponent> result = new ArrayList<XComponent>();
      if (types <= 0) {
         return result;
      }
      OpBroker broker = session.newBroker();
      try {
         XLocalizer localizer = new XLocalizer();
         localizer.setResourceMap(session.getLocale().getResourceMap(PROJECT_OBJECTS));

         String parentLocator = tabular ? parentNode.getStringValue() : XValidator.choiceID(parentNode.getStringValue());
         long parentId = OpLocator.parseLocator(parentLocator).getID();

         Map projectNodes = getProjectNodes(types, broker, parentId);
         OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.class, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
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
            int childCount = ((Number) projectNodes.get(new Long(projectNode.getId()))).intValue();
            if (projectNode.getType() == OpProjectNode.PORTFOLIO && childCount > 0 && hasNonFilteredChildren(projectNode, idsToFilter)) {
               XComponent dummyChild = createDummyChild(dataRow);
               result.add(dummyChild);
            }
         }
      }
      finally {
         broker.close();
      }
      return result;
   }

   /**
    * Checks if the given portfolio has any children which aren't filtered.
    *
    * @param projectNode a <code>OpProjectNode</code> representing the portfolio.
    * @param idsToFilter a <code>List</code> of <code>String</code> representing project locators.
    * @return true if the portfolio has any children which shouldn't be filtered.
    */
   private static boolean hasNonFilteredChildren(OpProjectNode projectNode, Collection idsToFilter) {
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
      StringBuffer queryString = new StringBuffer("select projectNode.id from OpProjectNode as projectNode where ");
      if (parentId != -1) {
         queryString.append("projectNode.SuperNode.id = ?");
      }
      else {
         queryString.append("projectNode.SuperNode is null");
      }

      List<Byte> typeParams = new ArrayList<Byte>();
      queryString.append(" and ( ");
      boolean filterAdded = false;
      if ((types & PORTFOLIOS) == PORTFOLIOS) {
         queryString.append("projectNode.Type = ?");
         typeParams.add(OpProjectNode.PORTFOLIO);
         filterAdded = true;
      }

      if ((types & PROJECTS) == PROJECTS) {
         if (filterAdded) {
            queryString.append(" or ");
         }
         queryString.append(" projectNode.Type = ?");
         typeParams.add(OpProjectNode.PROJECT);
         filterAdded = true;
      }

      if ((types & TEMPLATES) == TEMPLATES) {
         if (filterAdded) {
            queryString.append(" or ");
         }
         queryString.append(" projectNode.Type = ?");
         typeParams.add(OpProjectNode.TEMPLATE);
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
      for (Object typeParam : typeParams) {
         query.setByte(paramStartIndex++, (Byte) typeParam);
      }

      Map<Long, Number> result = new HashMap<Long, Number>();
      for (Object o : broker.list(query)) {
         Long id = (Long) o;

         StringBuffer childCountQuery = new StringBuffer("select count(subNode.id) from OpProjectNode parentNode left join parentNode.SubNodes subNode");
         childCountQuery.append(" where parentNode.id=? ");
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
         childCountQuery.append(" group by parentNode.id");

         OpQuery childQuery = broker.newQuery(childCountQuery.toString());
         childQuery.setLong(0, id);
         for (int i = 0; i < typeParams.size(); i++) {
            childQuery.setByte(i + 1, (Byte) typeParams.get(i));
         }
         Number count = 0;
         Iterator childCountIterator = broker.iterate(childQuery);
         if (childCountIterator.hasNext()) {
            count = (Number) childCountIterator.next();
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
    *      onepoint.persistence.OpQuery,onepoint.resource.XLocalizer,boolean,int,onepoint.express.XComponent)
    */
   public static XComponent createProjectNodeAdvancedRow(OpProjectSession session, OpBroker broker, OpProjectNode projectNode,
        XLocalizer localizer, boolean tabular, int outlineLevel) {
      XComponent dataRow = createProjectNodeBasicRow(session, broker, projectNode, localizer, tabular, outlineLevel);

      //only for tabular structures we add extra data
      XComponent dataCell = null;
      if (tabular) {
         ArrayList activityTypes = new ArrayList();
         activityTypes.add(new Byte(OpActivity.STANDARD));
         activityTypes.add(new Byte(OpActivity.COLLECTION));
         activityTypes.add(new Byte(OpActivity.SCHEDULED_TASK));
         activityTypes.add(new Byte(OpActivity.TASK));
         activityTypes.add(new Byte(OpActivity.COLLECTION_TASK));

         //6 - Completed
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (isOfType(dataRow, PROJECT_DESCRIPTOR)) {
            double value = getCompletedValue(broker, projectNode, activityTypes);
            dataCell.setDoubleValue(value);
         }
         dataRow.addChild(dataCell);

         //7 - Resources
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (isOfType(dataRow, PROJECT_DESCRIPTOR)) {
            double value = getResourcesValue(broker, projectNode, activityTypes);
            dataCell.setDoubleValue(value);
         }
         dataRow.addChild(dataCell);

         //8 - Costs
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (isOfType(dataRow, PROJECT_DESCRIPTOR)) {
            double value = getCostsValue(broker, projectNode, activityTypes);
            dataCell.setDoubleValue(value);
         }
         dataRow.addChild(dataCell);

         //9 - Status
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (isOfType(dataRow, PROJECT_DESCRIPTOR)) {
            String projectStatus = projectNode.getStatus() != null ? projectNode.getStatus().getName() :
                 null;
            dataCell.setStringValue(projectStatus);
         }
         dataRow.addChild(dataCell);

         //10 - Priority
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (isOfType(dataRow, PROJECT_DESCRIPTOR)) {
            dataCell.setIntValue(projectNode.getPriority());
         }
         dataRow.addChild(dataCell);

         //for portofolios add empty cells for effors and costs and cells with values set to 0 for deviations
         if (projectNode.getType() != OpProjectNode.PROJECT) {
            addPortofolioCostAndEffortCells(dataRow);
         }
         //for regular projects calculate the values of the cells
         else {
            addProjectCostAndEffortCells(dataRow, projectNode.getPlan().getBaseVersion(), null);
         }
         //23 - Archived
         Boolean archived = projectNode.getArchived();
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setBooleanValue(archived != null ? archived : Boolean.FALSE);
         dataRow.addChild(dataCell);

      }
      // #24 or #1: expanded:
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(false);
      dataRow.addChild(dataCell);
      
      // #25 or #2 controlling sheet icons
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataRow.addChild(dataCell);

      if (tabular) {
         // 26 - project plan end date
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (OpProjectDataSetFactory.PROJECT_DESCRIPTOR.equals(((XComponent) dataRow.getChild(0)).getStringValue())) {
            dataCell.setDateValue(projectNode.getPlan().getFinish());
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
   public static double getCompletedValue(OpBroker broker, OpProjectNode projectNode, List activityTypes) {
      return projectNode.getPlan().getComplete();
   }

   /**
    * Computes the value of the efort for the project, based on the effort of its resources.
    *
    * @param broker        a <code>OpBroker</code> used for perfroming business operations.
    * @param projectId     a <code>long</code> representing the id of the project for which the calculations are done.
    * @param activityTypes a <code>List</code> of <code>int</code> representing the types of activities to take into account.
    * @return a <code>double</code> representing the value of the efforts of the resources assigned on the project.
    */
   public static double getResourcesValue(OpBroker broker, OpProjectNode projectNode, List activityTypes) {
      return projectNode.getPlan().getBaseEffort() != 0d ? projectNode.getPlan().getActualEffort() / projectNode.getPlan().getBaseEffort() * 100 : 0d;
   }

   /**
    * Computes the value of the efort for the project, based on the effort of its resources.
    *
    * @param broker        a <code>OpBroker</code> used for perfroming business operations.
    * @param projectId     a <code>long</code> representing the id of the project for which the calculations are done.
    * @param activityTypes a <code>List</code> of <code>int</code> representing the types of activities to take into account.
    * @return a <code>double</code> representing the value of the efforts of the resources assigned on the project.
    */
   public static double getCostsValue(OpBroker broker, OpProjectNode projectNode, List activityTypes) {
      OpProjectPlan plan = projectNode.getPlan();
      double actualCostsSum = plan.getActualExternalCosts()
      + plan.getActualMaterialCosts()
      + plan.getActualMiscellaneousCosts()
      + plan.getActualPersonnelCosts() + plan.getActualTravelCosts();
      double baseCostsSum = plan.getBaseExternalCosts()
      + plan.getBaseMaterialCosts()
      + plan.getBaseMiscellaneousCosts()
      + plan.getBasePersonnelCosts() + plan.getBaseTravelCosts();
      return baseCostsSum != 0d ? actualCostsSum / baseCostsSum * 100d : 0d;
   }

   /**
    * Checks if a given data-row represents an entity with the required type.
    *
    * @param dataRow a <code>XComponent(DATA_ROW)</code> representing a portfolio data-row.
    * @param type    a <code>String</code> constant representing possible types of a data-row.
    * @return <code>true</code> if the dataRow represents a portfolio.
    */
   public static boolean isOfType(XComponent dataRow, String type) {
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
      boolean enablePortfolios = (Boolean) parameters.get(ENABLE_PORTFOLIOS);
      boolean enableTemplates = (Boolean) parameters.get(ENABLE_TEMPLATES);
      boolean enableProjects = (Boolean) parameters.get(ENABLE_PROJECTS);
      List notSelectableIds = (List) parameters.get(NOT_SELECTABLE_IDS);
      for (Iterator it = dataRows.iterator(); it.hasNext();) {
         XComponent dataRow = (XComponent) it.next();
         if (notSelectableIds != null && notSelectableIds.contains(XValidator.choiceID(dataRow.getStringValue()))) {
            dataRow.setSelectable(false);
            continue;
         }
         if (dataRow.getStringValue().equals(OpProjectConstants.DUMMY_ROW_ID)) {
            continue;
         }
         String descriptor = ((XComponent) dataRow.getChild(0)).getStringValue();
         if (descriptor.equals(PROJECT_DESCRIPTOR) && !enableProjects) {
            dataRow.setSelectable(false);
         }
         if (descriptor.equals(PORTFOLIO_DESCRIPTOR) && !enablePortfolios) {
            dataRow.setSelectable(false);
         }
         if (descriptor.equals(TEMPLATE_DESCRIPTOR) && !enableTemplates) {
            dataRow.setSelectable(false);
         }
      }
   }

   public static Iterator getProjectStatusIterator(OpBroker broker) {
      //configure project status sort order
      TreeMap sortOrder = new TreeMap();
      sortOrder.put(OpProjectStatus.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
      OpObjectOrderCriteria categoryOrderCriteria = new OpObjectOrderCriteria(OpProjectStatus.class, sortOrder);
      OpQuery query = broker.newQuery("select status from OpProjectStatus as status " + categoryOrderCriteria.toHibernateQueryString("status"));
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
      Number result = 0;
      for (Object o : broker.list(query)) {
         result = (Number) o;
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
   public static Set<OpProjectNode> getProjectsByPermissions(OpProjectSession session, OpBroker broker, Collection<Byte> levels) {
      StringBuffer qb = new StringBuffer("select project from OpProjectNode as project ");
      if (!session.userIsAdministrator()) {
         qb.append(" join project.Permissions as permission ");
      }
      qb.append("where project.Type in (:types) " +
         "and project.Archived = false ");
      
      if (!session.userIsAdministrator()) {
         qb.append("and permission.Object.id = project.id " +
               "and permission.Subject.id in (:subjectIds) " +
               "and permission.AccessLevel in (:levels) ");
      }
      OpQuery query = broker.newQuery(qb.toString());
      if (!session.userIsAdministrator()) {
         List subjectIds = session.getSubjectIds();
         query.setCollection("subjectIds", subjectIds);
         query.setCollection("levels", levels);
      }

      List<Byte> types = new ArrayList<Byte>();
      types.add(OpProjectNode.PROJECT);
      query.setCollection("types", types);
      return new HashSet<OpProjectNode>(broker.list(query));
   }

   public static Set<OpProjectNode> getProjectsByCriteria(
         OpProjectSession session, OpBroker broker, Collection<Byte> levels,
         Collection<OpProjectStatus> projectStates,
         Collection<OpCustomType> projectTypes) {
      if (projectStates != null && projectStates.isEmpty() || projectTypes != null && projectTypes.isEmpty()) {
         return new HashSet<OpProjectNode>();
      }
      StringBuffer qb = new StringBuffer("select project from OpProjectNode as project ");
      if (!session.userIsAdministrator()) {
         qb.append(" join project.Permissions as permission ");
      }
      qb.append("where project.Type in (:types) " +
         "and project.Archived = false ");
      if (projectStates != null) {
         qb.append("and project.Status in (:states) ");
      }
      if (projectTypes != null) { 
         qb.append("and project.CustomType in (:projectTypes) ");
      }
      if (!session.userIsAdministrator()) {
         qb.append("and permission.Object.id = project.id " +
               "and permission.Subject.id in (:subjectIds) " +
               "and permission.AccessLevel in (:levels) ");
      }
      OpQuery query = broker.newQuery(qb.toString());
      
      if (!session.userIsAdministrator()) {
         List subjectIds = session.getSubjectIds();
         query.setCollection("subjectIds", subjectIds);
         query.setCollection("levels", levels);
      }
      if (projectStates != null) {
         query.setCollection("states", projectStates);
      }
      if (projectTypes != null) { 
         query.setCollection("projectTypes", projectTypes);
      }
      List<Byte> types = new ArrayList<Byte>();
      types.add(OpProjectNode.PROJECT);
      query.setCollection("types", types);
      return new HashSet<OpProjectNode>(broker.list(query));
   }

   /**
    * Creates a map of projects->resources for the current session user taking into account also his permissions over
    * the resources.
    * Gets a set containing all {@link OpProjectNode}s
    * (Portfolios, Projects and/or Templates) representing
    * direct children of the given parent.
    *
    * @param broker   the broker to perform any operation.
    * @param types    bitset, one of (
    * @param parentId the id of the parent to get child nodes.
    * @return a set containing all children nodes.
    * @pre none
    * @post none
    */

   public static Set<OpProjectNode> getProjectNodes(OpBroker broker, int types, long parentId) {
      StringBuffer queryString = new StringBuffer("select projectNode from OpProjectNode as projectNode where ");
      if (parentId != -1) {
         queryString.append("projectNode.SuperNode.id = ?");
      }
      else {
         queryString.append("projectNode.SuperNode is null");
      }

      List<Byte> typeParams = new ArrayList<Byte>();
      boolean filterAdded = false;

      if ((types & ALL_PROJECT_NODE_TYPES) == ALL_PROJECT_NODE_TYPES) {
      }
      else {
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

      Iterator childrenIterator = broker.iterate(query);
      OpProjectNode node;
      HashSet<OpProjectNode> result = new HashSet<OpProjectNode>();
      while (childrenIterator.hasNext()) {
         node = (OpProjectNode) childrenIterator.next();
         result.add(node);
      }
      return result;
   }

   /**
    * Get the list of reources linked to a given project
    *
    * @param project     the project node
    * @param userId      the used id
    * @param responsible to enforce that the given user (userId) is responsible for the returned resources
    * @return a <code>List&lt;String&gt;</code> of resource choices - e.g. locator['label'].
    */
   public static List<String> getProjectResources(OpProjectNode project, long userId, boolean responsible) {
      List<String> resources = new ArrayList<String>();
      Set<OpProjectNodeAssignment> assignments = project.getAssignments();
      for (OpProjectNodeAssignment assignment : assignments) {
         OpResource resource = assignment.getResource();
         if (responsible && (resource.getUser() == null || resource.getUser().getId() != userId)) {
            continue;
         }
         resources.add(XValidator.choice(resource.locator(), resource.getName()));
      }
      return resources;
   }

   /**
    * Adds 8 cells with value set to null and 4 cells woth values set to 0 to a <code>XComponent</code> data row.
    *
    * @param dataRow - the <code>XComponent</code> data row to which the cells are added
    */
   private static void addPortofolioCostAndEffortCells(XComponent dataRow) {
      //for portofolios do not show any efforts or costs
      for (int i = 0; i < 8; i++) {
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataRow.addChild(dataCell);
      }

      //the deviation cells values are set to 0 so that they show a disabled line
      for (int i = 0; i < 4; i++) {
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(0d);
         dataRow.addChild(dataCell);
      }
   }
   
   public static XComponent setDoubleValueInRow(XComponent dataRow, int cellIdx,
         double d) {
      return setDataCellValueInRow(dataRow, cellIdx, new Double(d), true);
   }

      
   public static XComponent setDataCellValueInRow(XComponent dataRow, int cellIdx,
         Object value, boolean showField) {
      if (cellIdx >= dataRow.getChildCount()) {
         for (int k = dataRow.getChildCount() - 1; k < cellIdx; k++) {
            dataRow.addChild(new XComponent(XComponent.DATA_CELL));
         }
      }
      XComponent cell = null; 
      if (cellIdx < 0) {
         cell = new XComponent(XComponent.DATA_CELL);
      }
      else {
         cell = (XComponent) dataRow.getChild(cellIdx);
      }
      cell.setValue(showField ? value : null);
      return cell;
   }

   private static int getIdx(int[] indexMap, int idx) {
      if (indexMap == null || idx >= indexMap.length || idx < 0) {
         return idx;
      }
      return indexMap[idx];
   }

   private static void addProjectCostAndEffortCells(XComponent dataRow, OpProjectPlanVersion planV, int [] indexMap) {
      int offset = dataRow.getChildCount();
      if (planV == null) {
         for (int i = 0; i < 12; i++) {
            dataRow.addChild(createDoubleDataCell(0d));
         }
         return;
      }
      OpProjectPlan plan = planV.getProjectPlan();
      double baseCosts = planV.getBasePersonnelCosts()
            + planV.getBaseExternalCosts() + planV.getBaseMaterialCosts()
            + planV.getBaseMiscellaneousCosts() + planV.getBaseTravelCosts();
      double actualCosts = planV.getActualPersonnelCosts()
            + planV.getActualExternalCosts() + planV.getActualMaterialCosts()
            + planV.getActualMiscellaneousCosts()
            + planV.getActualTravelCosts();
      double remainingCosts = planV.getRemainingPersonnelCosts()
            + planV.getRemainingExternalCosts()
            + planV.getRemainingMaterialCosts()
            + planV.getRemainingMiscellaneousCosts()
            + planV.getRemainingTravelCosts();
      double costsDeviation = (actualCosts + remainingCosts) - baseCosts;
      double costsPredicted = actualCosts + remainingCosts;
      double effortDeviation = (plan.getActualEffort() + plan.getOpenEffort()) - planV.getBaseEffort();
      double effortPredicted = plan.getActualEffort() + plan.getOpenEffort();

      setDoubleValueInRow(dataRow, getIdx(indexMap, 0) + offset, planV.getBaseEffort());
      setDoubleValueInRow(dataRow, getIdx(indexMap, 1) + offset, plan.getActualEffort());
      setDoubleValueInRow(dataRow, getIdx(indexMap, 2) + offset, plan.getActualEffort() + plan.getOpenEffort());
      setDoubleValueInRow(dataRow, getIdx(indexMap, 3) + offset, planV.getBaseEffort() - plan.getActualEffort());
      setDoubleValueInRow(dataRow, getIdx(indexMap, 4) + offset, baseCosts);
      setDoubleValueInRow(dataRow, getIdx(indexMap, 5) + offset, actualCosts);
      setDoubleValueInRow(dataRow, getIdx(indexMap, 6) + offset, costsPredicted);
      setDoubleValueInRow(dataRow, getIdx(indexMap, 7) + offset, baseCosts - actualCosts);
      setDoubleValueInRow(dataRow, getIdx(indexMap, 8) + offset, effortDeviation);
      setDoubleValueInRow(dataRow, getIdx(indexMap, 9) + offset, OpActivityDataSetFactory.calculatePercentDeviation(planV.getBaseEffort(), effortDeviation));
      setDoubleValueInRow(dataRow, getIdx(indexMap,10) + offset, costsDeviation);
      setDoubleValueInRow(dataRow, getIdx(indexMap,11) + offset, OpActivityDataSetFactory.calculatePercentDeviation(baseCosts, costsDeviation));

   }
   
   private static XComponent createDoubleDataCell(double value) {
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(value);
      return dataCell;
   }

   /**
    * Returns a map with the project locators and the ids of the baselines for those projects
    * which have a baseline set.
    *
    * @param session a <code>OpProjectSession</code> a server session.
    * @return a <code>Map(String, Long)</code> with (projectLocator, baselineID) pairs.
    */
   public static Map<String, Long> getProjectsWithBaseline(OpProjectSession session) {
      Map<String, Long> result = new HashMap<String, Long>();
      OpBroker broker = session.newBroker();
      try {
         String projectsQueryString = "select planVersion from OpProjectNode project inner join  project.Plan plan inner join plan.Versions planVersion where planVersion.Baseline=true";
         OpQuery query = broker.newQuery(projectsQueryString);
         Iterator<OpProjectPlanVersion> it = broker.iterate(query);
         while (it.hasNext()) {
            OpProjectPlanVersion baseline = it.next();
            OpProjectNode project = baseline.getProjectPlan().getProjectNode();
            result.put(project.locator(), baseline.getId());
         }
      }
      finally {
         broker.close();
      }
      return result;
   }

   /**
    * Returns <code>true</code> if the <code>OpProjectNode</code> specified as parameter has locks or <code>false</code> otherwise.
    *
    * @param broker      - the <code>OpBroker</code> object needed to perform DB operations.
    * @param projectNode - the <code>OpProjectNode</code> object.
    * @return <code>true</code> if the <code>OpProjectNode</code> specified as parameter has locks or <code>false</code> otherwise.
    */
   public static boolean hasLocks(OpBroker broker, OpProjectNode projectNode) {
      return (projectNode.getLocks() != null && !projectNode.getLocks().isEmpty());
   }

   /**
    * Returns <code>true</code> if the <code>OpProjectPlan</code> specified as parameter has activities or <code>false</code> otherwise.
    *
    * @param broker      - the <code>OpBroker</code> object needed to perform DB operations.
    * @param projectPlan - the <code>OpProjectPlan</code> object.
    * @return <code>true</code> if the <code>OpProjectPlan</code> specified as parameter has activities or <code>false</code> otherwise.
    */
   public static boolean hasActivities(OpBroker broker, OpProjectPlan projectPlan) {
      if (projectPlan.getActivities() != null) {
         OpQuery query = broker.newQuery(GET_ACTIVITY_COUNT_FOR_PROJECT_PLAN);
         query.setLong("projectPlanId", projectPlan.getId());
         Number counter = (Number) broker.iterate(query).next();
         if (counter.intValue() > 0) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns the number of project plan versions for the <code>OpProjectPlan</code> specified as parameter.
    *
    * @param broker      - the <code>OpBroker</code> object needed to perform DB operations.
    * @param projectPlan - the <code>OpProjectPlan</code> object.
    * @return the number of project plan versions for the <code>OpProjectPlan</code> specified as parameter.
    */
   public static int getPlanVersionsCount(OpBroker broker, OpProjectPlan projectPlan) {
      if (projectPlan.getVersions() != null) {
         OpQuery query = broker.newQuery(GET_PLAN_VERSION_COUNT_FOR_PROJECT_PLAN);
         query.setLong("projectPlanId", projectPlan.getId());
         Number counter = (Number) broker.iterate(query).next();
         return counter.intValue();
      }
      return 0;
   }

   /**
    * Returns the number of project nodes for the <code>OpProjectStatus</code> specified as parameter.
    *
    * @param broker        - the <code>OpBroker</code> object needed to perform DB operations.
    * @param projectStatus - the <code>OpProjectStatus</code> object.
    * @return the number of project nodes for the <code>OpProjectStatus</code> specified as parameter.
    */
   public static int getProjectsCount(OpBroker broker, OpProjectStatus projectStatus) {
      if (projectStatus.getProjects() != null) {
         OpQuery query = broker.newQuery(GET_PROJECT_COUNT_FOR_PROJECT_STATUS);
         query.setLong("statusId", projectStatus.getId());
         Number counter = (Number) broker.iterate(query).next();
         return counter.intValue();
      }
      return 0;
   }

   /**
    * Creates a temporary file with the content of an attachment or a document.
    *
    * @param location a <code>String</code> representing the location of the real attachment/document object.
    * @param content  <code>InputStream</code> representing the content of the attachment/document.
    * @param logger   <code>XLog</code> needed to log the possible errors.
    * @return a <code>String</code> representing an URL-like path to a temporary file that has the same content as the
    *         attachment/document.
    */
   public static String createTemporaryAttachment(String location, InputStream content, XLog logger) {
      int extensionIndex = location.lastIndexOf(".");
      String prefix = location;
      String suffix = null;
      if (extensionIndex != -1) {
         prefix = location.substring(0, extensionIndex);
         suffix = location.substring(extensionIndex, location.length());
      }
      if (prefix.length() < 3) {
         prefix = "file" + prefix;
      }

      try {
         File temporaryFile = File.createTempFile(prefix, suffix, new File(XEnvironmentManager.TMP_DIR));
         temporaryFile.deleteOnExit();
         FileOutputStream fos = new FileOutputStream(temporaryFile);
         XIOHelper.copy(content, fos);
         fos.flush();
         fos.close();
         content.close();
//         return temporaryFile.getName();
         return XEncodingHelper.encodeValue(temporaryFile.getName());
      }
      catch (IOException e) {
         logger.error("Cannot create temporary attachment or document file on server", e);
      }
      return null;
   }

   public static Map<String, List<String>> getProjectToResourceMap(OpProjectSession session) {
      return getProjectToResourceMap(session, OpPermission.MANAGER);
   }
   
   private static boolean hasAccess(OpProjectSession session, OpBroker broker, Map<Long, Boolean> accessCache, byte minAccessLevel, long id) {
      Long lId = new Long(id);
      Boolean access = accessCache.get(lId);
      if (access != null) {
         return access.booleanValue();
      }
      access = session.checkAccessLevel(broker, id, minAccessLevel);
      accessCache.put(lId, access ? Boolean.TRUE : Boolean.FALSE);
      return access;
   }
   /**
    * Returns a map of projects and list of resources for each project, where the current user is
    * at least observer on the project. The resource will be the ones the user is responsible for or has at least
    * manager permissions on them.
    *
    * @param session Current project session (used for db access and current user)
    * @return Map of key: project_locator/project_name choice -> value: List of resource_locator/resource_name choices
    */
   public static Map<String, List<String>> getProjectToResourceMap(OpProjectSession session, byte minResourceAccessLevel) {
      Map<String, List<String>> projectsMap = new HashMap<String, List<String>>();
      OpBroker broker = session.newBroker();
      try {
         long userId = session.getUserID();
         
         Map<Long, Boolean> resourceAccessLevels = new HashMap<Long, Boolean>();

         // add all the resources for which is responsible from project where the user has contributer access
         List<Byte> levels = new ArrayList<Byte>();

         //add only the responsible resources (or at least manager permission) for the projects where the user is  OBSERVER, CONTRIBUTOR, ADMINISTRATOR or MANAGER
         levels.add(OpPermission.OBSERVER);
         levels.add(OpPermission.CONTRIBUTOR);
         levels.add(OpPermission.ADMINISTRATOR);
         levels.add(OpPermission.MANAGER);
         Set<OpProjectNode> projects = getProjectsByPermissions(session, broker, levels);
         for (OpProjectNode project : projects) {
            SortedSet<String> allResources = new TreeSet<String>(new XComponent.ChoiceComparator());
            for (OpProjectNodeAssignment assignment : project.getAssignments()) {
               OpResource resource = assignment.getResource();
               boolean isResponsible = resource.getUser() != null && resource.getUser().getId() == userId;
               if (isResponsible || hasAccess(session, broker, resourceAccessLevels, minResourceAccessLevel, resource.getId())) {
                  allResources.add(XValidator.choice(resource.locator(), resource.getName()));
               }
            }
            if (!allResources.isEmpty()) {
               List<String> rList = new ArrayList(allResources);
               projectsMap.put(XValidator.choice(project.locator(), project.getName()), rList);
            }
         }
      }
      finally {
         broker.close();
      }
      return projectsMap;
   }
}