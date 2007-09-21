/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpFilter;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.OpService;

import java.util.Iterator;
import java.util.Set;

/**
 * Service Implementation for Project Administration.
 * This class is capable of traversing Projects and Portfolios
 * and requesting the Root Portfolio.
 *
 * @author dfreis
 */

public class OpProjectAdministrationServiceImpl implements OpService {

   /**
    * the portfolio type.
    */
   public static final int TYPE_PORTFOLIO = OpProjectNode.PORTFOLIO;

   /**
    * the project type.
    */
   public static final int TYPE_PROJECT = OpProjectNode.PROJECT;

   /**
    * type for portfolio or project.
    */
   public static final int TYPE_ALL = TYPE_PORTFOLIO + TYPE_PROJECT;

   /**
    * Returns the project plan identified by the given id.
    * @param session the session within any operation will be performed.
    * @param broker the broker to perform any operation.
    * @param id the id of the project plan to get.
    * @return the requested project plan or <code>null</code> if no 
    * project was found for the given id.
    */
   public final OpProjectPlan getProjectPlanMyId(
         final OpProjectSession session, final OpBroker broker, final long id) {
      return (OpProjectPlan) broker.getObject(OpProjectPlan.class, id);
   }
   
   /**
    * Returns the top level portfolio or null if the user does not
    * have sufficient rights.
    * @param session the session within any operation will be performed.
    * @param broker the broker to perform any operation.
    * @return the top level portfolio.
    */
   public final OpProjectNode getRootPortfolio(
         final OpProjectSession session, final OpBroker broker) {
      if (!session.isLoggedOn()) {
         return null;
      }
      final OpQuery query = broker.newQuery(
      "select portfolio from OpProjectNode as portfolio where portfolio.Name = ? and portfolio.Type = ?");
      query.setString(0, OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME);
      query.setByte(1, OpProjectNode.PORTFOLIO);
      final Iterator result = broker.iterate(query);
      if (result.hasNext()) {
         return (OpProjectNode) result.next();
      }
      return null;
   }

   /**
    * Returns the direct children portfolios and/or projects of
    * the portfolio identified by <code>portfolio</code>.
    * @param session the session within any operation will be performed.
    * @param broker the broker to perform any operation.
    * @param type the type of projects or portfolios to be returned.
    * Must be one of <code>TYPE_PORTFOLIO</code>, <code>TYPE_PROJECT</code>
    * or <code>TYPE_ALL</code>.
    * @param portfolio the parent portfolio.
    * @return an iterator containing all children of the portfolio
    * identified by <code>portfolio</code> matching the given type.
    */
   public final Iterator<OpProjectNode> getChildren(
         final OpProjectSession session, final OpBroker broker,
         final OpProjectNode portfolio, final int type) {
      return getChildren(session, broker, portfolio, type, null);
   }

   /**
    * Returns the direct children portfolios and/or projects of
    * the portfolio identified by <code>portfolio</code> that match the
    * given type and filter creteria.
    * @param session the session within any operation will be performed.
    * @param broker the broker to perform any operation.
    * @param type the type of projects or portfolios to be returned.
    * Must be one of <code>TYPE_PORTFOLIO</code>, <code>TYPE_PROJECT</code>
    * or <code>TYPE_ALL</code>.
    * @param portfolio the parent portfolio.
    * @param filter a filter to filter out results, or null if nothing should be filtered out.
    * @return an iterator containing all children of the portfolio
    * identified by <code>portfolio</code> matching the given type and filter criteria.
    */
   public final Iterator<OpProjectNode> getChildren(
         final OpProjectSession session, final OpBroker broker,
         final OpProjectNode portfolio, final int type, final OpFilter filter) {
      Set<OpProjectNode> children = OpProjectDataSetFactory.getProjectNodes(broker, type & TYPE_ALL, portfolio.getID());
      if (filter == null) {
         return children.iterator();
      }
      Iterator<OpProjectNode> iter = children.iterator();
      OpProjectNode node;
      while (iter.hasNext()) {
         node = iter.next();
         if (!filter.accept(node)) {
            iter.remove();
         }
      }
      return children.iterator();
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.OpService#getName()
    */
   public String getName() {
      return OpProjectAdministrationService.SERVICE_NAME;
   }
   
}
