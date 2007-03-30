/**
 * 
 */
package onepoint.project.modules.project;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpFilter;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpSubject;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserError;
import onepoint.project.modules.user.OpUserErrorMap;
import onepoint.project.modules.user.OpUserServiceImpl;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceException;

/**
 * @author dfreis
 *
 */
public class OpProjectAdministrationServiceImpl
{
  
  /**
   * the portfolio type
   */
  public final static int TYPE_PORTFOLIO = OpProjectNode.PORTFOLIO;
  /**
   * the project type
   */
  public final static int TYPE_PROJECT = OpProjectNode.PROJECT;
  /**
   * type for portfolio or project
   */
  public final static int TYPE_ALL = TYPE_PORTFOLIO + TYPE_PROJECT;

  /**
   * Returns the top level portfolio or null if the user does not 
   * have sufficient rights.
   * @param session the session within any operation will be performed.
   * @param broker the broker to perform any operation.
   * @return the top level portfolio.
   * @pre none
   * @post none
   */
  public OpProjectNode getRootPortfolio(OpProjectSession session, OpBroker broker) 
    throws XServiceException
  {
    // FIXME(dfreis Mar 22, 2007 1:31:35 PM)
    // should do that via OpProjectAdministrationServiceImpl

    if (!session.isLoggedOn())
    {
      return(null);
    }
    OpQuery query = broker.newQuery(
        "select portfolio from OpProjectNode as portfolio where portfolio.Name = ? and portfolio.Type = ?");
    query.setString(0, OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME);
    query.setByte(1, OpProjectNode.PORTFOLIO);
    Iterator result = broker.list(query).iterator();
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
   * @throws IllegalArgumentException if type is not valid or portfolio does 
   * not represent an existing or valid portfolio. 
   * @pre none.
   * @post none.
   */ 
  public Iterator<OpProjectNode> getChildren(OpProjectSession session, OpBroker broker, OpProjectNode portfolio, int type)
    throws XServiceException
  {
    return(getChildren(session, broker, portfolio, type, null));
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
   * @throws IllegalArgumentException if type is not valid or portfolio does 
   * not represent an existing or valid portfolio. 
   * @pre none.
   * @post none.
   */ 
  public Iterator<OpProjectNode> getChildren(OpProjectSession session, OpBroker broker, OpProjectNode portfolio, int type, OpFilter filter)
  throws XServiceException
  {
    Set<OpProjectNode> children = OpProjectDataSetFactory.getProjectNodes(broker, type & TYPE_ALL, portfolio.getID());
    if (filter == null) {
      return(children.iterator());
    }
    Iterator<OpProjectNode> iter = children.iterator();
    OpProjectNode node ;
    while (iter.hasNext())
    {
      node = iter.next();
      if (!filter.accept(node)) {
        children.remove(node);
      }
    }
    return(children.iterator());
  }
}
