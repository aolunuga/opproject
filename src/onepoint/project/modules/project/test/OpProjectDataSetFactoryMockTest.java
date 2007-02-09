/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.util.XCalendar;
import org.jmock.core.Constraint;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;

import java.util.ArrayList;

/**
 * Test case class for OpProjectDataSetFactory. Will test the functionality of the helper class using mock objects
 *
 * @author ovidiu.lupas
 */
public class OpProjectDataSetFactoryMockTest extends onepoint.project.test.OpBaseMockTestCase {
   /*root portfolio of project */
   private OpProjectNode rootPortfolio;
   /* subportfolio of the root portfolio*/
   private OpProjectNode portfolio;
   /*inner project of the portfolio */
   private OpProjectNode project1;
   /*inner project of the root portfolio */
   private OpProjectNode project2;

   /* ids of the entities*/
   private long ROOT_PORTFOLIO_ID = 1;
   private long PORTFOLIO_ID = 10;
   private long PROJECT1_ID = 11;
   private long PROJECT2_ID = 12;

   /*HQLs */
   private static final String SELECT_PORTFOLIO_ID = "select portfolio.ID from OpProjectNode as portfolio " +
        "where portfolio.SuperNode.ID is null and portfolio.Type = ?";
   private static final String SELECT_PORTFOLIO_ID_BY_SUPERPORTFOLIO_ID = "select portfolio.ID from OpProjectNode as portfolio " +
        "where portfolio.SuperNode.ID = ? and portfolio.Type = ?";
   private static final String SELECT_PROJECT_TEMPLATE_BY_PORTFOLIO_ID = "select template.ID from OpProjectNode as template " +
        "where template.SuperNode.ID = ? and template.Type = ?";
   private static final String SELECT_PROJECT_BY_PORTFOLIO_ID = "select project.ID from OpProjectNode as project " +
        "where project.SuperNode.ID = ? and project.Type = ?";

   /**
    * @see onepoint.project.test.OpBaseMockTestCase#invocationMatch(org.jmock.core.Invocation)
    */
   public Object invocationMatch(Invocation invocation) throws IllegalArgumentException {
      String methodName = invocation.invokedMethod.getName();

      if (methodName.equals(ACCESSIBLE_OBJECTS_METHOD)) {
         return queryResults.iterator();
      }
      if (methodName.equals(EFFECTIVE_ACCESS_LEVEL_METHOD)) {
         return new Byte((byte) 16);
      }
      //no such method was found
      throw new IllegalArgumentException("Invalid method name:" + methodName + " for this stub");
   }

   public void setUp() {

      super.setUp();
      //crete the root portfolio
      rootPortfolio = new OpProjectNode();
      rootPortfolio.setType(OpProjectNode.PORTFOLIO);
      rootPortfolio.setID(ROOT_PORTFOLIO_ID);
      rootPortfolio.setName("RootPortfolio");
      rootPortfolio.setDescription("RootPortfolioDescription");

      //create one subportfolio
      portfolio = new OpProjectNode();
      portfolio.setType(OpProjectNode.PORTFOLIO);
      portfolio.setID(PORTFOLIO_ID);
      portfolio.setName("PortfolioName");
      portfolio.setDescription("PortfolioDescription");

      //create first project
      project1 = new OpProjectNode();
      project1.setType(OpProjectNode.PROJECT);
      project1.setID(PROJECT1_ID);
      project1.setName("Project1");
      project1.setDescription("Project1_Description");
      project1.setStart(XCalendar.today());
      project1.setFinish(XCalendar.today());

      //create second project
      project2 = new OpProjectNode();
      project2.setType(OpProjectNode.PROJECT);
      project2.setID(PROJECT2_ID);
      project2.setName("Project2");
      project2.setDescription("Project2_Description");
      project2.setStart(XCalendar.today());
      project2.setFinish(XCalendar.today());

      //query results
      queryResults = new ArrayList();

   }

   /**
    * Tests the behaviour of <code>OpProjectDataSetFactory#retriveProjectDataSet</code>.
    * The expected project data set is presented below.
    * ---->RootPortfolio
    * ------->Portfolio1
    * ----------->Project1
    * ------->Project2
    */
   public void testRetrieveProjectDataSet() {

      //get locale method
      mockSession.expects(once()).method(GET_LOCALE_METHOD).will(methodStub);

      //accessible object will return a queryResults iterator
      mockSession.expects(atLeastOnce()).method(ACCESSIBLE_OBJECTS_METHOD).will(methodStub);

      //list result sets
      mockBroker.expects(atLeastOnce()).method(LIST_METHOD).with(same(query)).will(methodStub);

      //the root portfolio is searched for
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PORTFOLIO_ID)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //add root portfolio
            queryResults.clear();
            queryResults.add(rootPortfolio);
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_PORTFOLIO_ID);
         }
      });

      //the portfolio is searched for
      mockBroker.expects(atLeastOnce()).method(NEW_QUERY_METHOD).with(eq(SELECT_PORTFOLIO_ID_BY_SUPERPORTFOLIO_ID)).
           will(methodStub).id(SELECT_PORTFOLIO_ID_BY_SUPERPORTFOLIO_ID);

      //the portfolio templates is searched for
      mockBroker.expects(atLeastOnce()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_TEMPLATE_BY_PORTFOLIO_ID)).
           will(methodStub).id(SELECT_PROJECT_TEMPLATE_BY_PORTFOLIO_ID);

      //the portfolio projects are searched for
      mockBroker.expects(atLeastOnce()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_BY_PORTFOLIO_ID)).
           will(methodStub).id(SELECT_PROJECT_BY_PORTFOLIO_ID);

      //search for the portfolios of root portfolio
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(ROOT_PORTFOLIO_ID)}).
           after(mockBroker, SELECT_PORTFOLIO_ID_BY_SUPERPORTFOLIO_ID).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //add portfolio
            queryResults.clear();
            queryResults.add(portfolio);
            return null;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SET_LONG_METHOD);
         }
      });

      //search for the portfolios of subportfolio
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(PORTFOLIO_ID)}).
           after(mockBroker, SELECT_PORTFOLIO_ID_BY_SUPERPORTFOLIO_ID).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //clear results
            queryResults.clear();
            return null;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SET_LONG_METHOD);
         }
      });
      //search for the projects of rootportfolio
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(ROOT_PORTFOLIO_ID)}).
           after(mockBroker, SELECT_PROJECT_BY_PORTFOLIO_ID).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //add project2
            queryResults.clear();
            queryResults.add(project2);
            return null;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SET_LONG_METHOD);
         }
      });

      //search for the projects of subportfolio
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(PORTFOLIO_ID)}).
           after(mockBroker, SELECT_PROJECT_BY_PORTFOLIO_ID).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //add project1
            queryResults.clear();
            queryResults.add(project1);
            return null;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SET_LONG_METHOD);
         }
      });

      //search for the template of rootportfolio
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(ROOT_PORTFOLIO_ID)}).
           after(mockBroker, SELECT_PROJECT_TEMPLATE_BY_PORTFOLIO_ID).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //clear results
            queryResults.clear();
            return null;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SET_LONG_METHOD);
         }
      });

      //search for the template of subportfolio
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(PORTFOLIO_ID)}).
           after(mockBroker, SELECT_PROJECT_TEMPLATE_BY_PORTFOLIO_ID).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            //clear results
            queryResults.clear();
            return null;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SET_LONG_METHOD);
         }
      });


      mockSession.expects(atLeastOnce()).method(EFFECTIVE_ACCESS_LEVEL_METHOD).will(methodStub);
      //set project types PORTFOLIO,TEMPLATE,PROJECT
      mockQuery.expects(atLeastOnce()).method(SET_BYTE_METHOD);

      /*the expected data set */
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpProjectDataSetFactory.retrieveProjectDataSet(((OpProjectSession) mockSession.proxy()), ((OpBroker) mockBroker.proxy()), dataSet, OpProjectDataSetFactory.ALL_TYPES, true);
      assertEquals("Wrong number of children in the data set", 4, dataSet.getChildCount());
      //assert for root portfolio
      assertEqualProjectPortfolios(rootPortfolio, (XComponent) dataSet.getChild(0));
      //assert for inner root portfolio
      assertEqualProjectPortfolios(portfolio, (XComponent) dataSet.getChild(1));

      //assert for project1
      assertEqualProjects(project1, (XComponent) dataSet.getChild(2));
      //assert for project2
      assertEqualProjects(project2, (XComponent) dataSet.getChild(3));

   }

   /**
    * Asserts that the given <code>portfolioDataRow</code> has the <code>expectedPortfolio<code> field values
    *
    * @param expectedPortfolio the expected project portfolio instance of <code>XProjectPortfolio</code>
    * @param portfolioDataRow  the tested portfolio <code>XComponent.DATA_ROW</code>
    */

   private void assertEqualProjectPortfolios(OpProjectNode expectedPortfolio, XComponent portfolioDataRow) {

      XComponent dataCell = (XComponent) portfolioDataRow.getChild(0);
      //portfolio descriptor
      assertEquals("Tested Project Portfolio descriptor do not match ", OpProjectDataSetFactory.PORTFOLIO_DESCRIPTOR, dataCell.getStringValue());
      //portfolio locator
      assertEquals("Project Portfolios locator do not match", expectedPortfolio.locator(), portfolioDataRow.getStringValue());
      //portfolio descriptor
      dataCell = (XComponent) portfolioDataRow.getChild(2);
      assertEquals("Project Portfolios description do not match", expectedPortfolio.getDescription(), dataCell.getStringValue());
      //portfolio start date
      dataCell = (XComponent) portfolioDataRow.getChild(3);
      assertNull("Tested Project Portfolio Start Date do not match", dataCell.getValue());
      //portfolio finish date
      dataCell = (XComponent) portfolioDataRow.getChild(4);
      assertNull("Tested Project Portfolio Finish Date do not match", dataCell.getValue());
   }


   /**
    * Asserts that the given <code>projectDataRow</code> has the <code>expectedProject<code> field values
    *
    * @param expectedProject the expected project instance of <code>XProject</code>
    * @param projectDataRow  the tested project <code>XComponent.DATA_ROW</code>
    */

   private void assertEqualProjects(OpProjectNode expectedProject, XComponent projectDataRow) {

      XComponent dataCell = (XComponent) projectDataRow.getChild(0);
      //project descriptor
      assertEquals("Tested Project descriptor do not match ", OpProjectDataSetFactory.PROJECT_DESCRIPTOR, dataCell.getStringValue());
      //project locator
      assertEquals("Projects locator do not match", expectedProject.locator(), projectDataRow.getStringValue());
      //project descriptor
      dataCell = (XComponent) projectDataRow.getChild(2);
      assertEquals("Projects description do not match", expectedProject.getDescription(), dataCell.getStringValue());
      //project start date
      dataCell = (XComponent) projectDataRow.getChild(3);
      assertEquals("Projects Start Date do not match", dataCell.getDateValue(), expectedProject.getStart());
      //project finish date
      dataCell = (XComponent) projectDataRow.getChild(4);
      assertEquals("Projects Finish Date do not match", dataCell.getDateValue(), expectedProject.getFinish());
   }
}
