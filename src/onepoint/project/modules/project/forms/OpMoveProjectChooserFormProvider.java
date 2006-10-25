/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider used for moving projects
 * @author ovidiu.lupas
 */
public class OpMoveProjectChooserFormProvider implements XFormProvider {

   /* form parameters */
   public final static String PROJECT_DATA_SET = "ProjectDataSet";
   public final static String PROJECT_NODE_ID_FIELD = "ProjectNodeIdField";

   /* map params */
   public final static String PROJECT_NODE_ID = "project_node_id";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      String projectNodeId = (String) parameters.get(PROJECT_NODE_ID);
      OpBroker broker = session.newBroker();
      //load it
      OpProjectNode projectNode = (OpProjectNode) broker.getObject(projectNodeId);

      if (projectNode == null) {
         return;
      }
      /*fill the project node id locator*/
      form.findComponent(PROJECT_NODE_ID_FIELD).setStringValue(projectNode.locator());

      /*fill the data set with portfolios (if the selected node is not root*/
      XComponent dataSet = form.findComponent(PROJECT_DATA_SET);
      if (!projectNode.getName().equals(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)) {
         OpProjectDataSetFactory.retrieveProjectDataSet(session, broker, dataSet, OpProjectDataSetFactory.PORTFOLIOS, false);
      }

      /*filter the data set :
         1.remove the selected project node if it is a portfolio
         2.remove the direct super node of projectNode if
            2.1 superNode has no sub portfolios
      */
      OpProjectNode superNode = projectNode.getSuperNode();
      if (superNode != null) {
         OpQuery query = broker.newQuery("select count(subNode) from OpProjectNode portfolio inner join portfolio.SubNodes subNode " +
              "where portfolio.ID = :portfolioId and subNode.Type = :subNodeType and subNode.ID != :subNodeId");
         query.setLong("portfolioId", superNode.getID());
         query.setByte("subNodeType", (byte) OpProjectDataSetFactory.PORTFOLIOS);
         query.setLong("subNodeId", projectNode.getID());
         Integer counter = (Integer) broker.iterate(query).next();

         /*needed locators */
         String superNodeLocator = superNode.locator();
         String projectNodeLocator = projectNode.locator();

         XComponent dataRow;
         for (int i = dataSet.getChildCount() - 1; i >= 0; i--) {
            dataRow = (XComponent) dataSet.getChild(i);
            String locator = OpLocator.parseLocator(dataRow.getStringValue()).toString();
            if (locator.equals(projectNodeLocator)) { //selected project node is a portfolio
               dataSet.removeDataRows(dataRow.getSubRows());
               dataSet.removeChild(i);
            }
            else if (locator.equals(superNodeLocator) && counter.intValue() == 0) {
               dataSet.removeChild(i);
            }
         }
      }

      broker.close();

   }
}
