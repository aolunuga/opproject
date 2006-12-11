/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hsqldb.DatabaseManager;
import org.hsqldb.HsqlException;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OpHibernateConnection extends OpConnection {

   private static final XLog logger = XLogFactory.getLogger(OpHibernateConnection.class,true);

   private Session _session;

   public OpHibernateConnection(OpSource source, Session session) {
      super(source);
      _session = session;
   }

   public boolean isValid() {
      if (_session == null) {
         return false;
      }
      try {
         _session.connection().getMetaData();
      }
      catch (Exception e) {
         return false;
      }
      return true;
   }

   public void executeDDLScript(String[] script) {
      // Execute schema creation script directly via JDBC connection
      Connection connection = null;
      Statement statement = null;
      OpTransaction t = newTransaction();
      try {
         connection = _session.connection();
         for (int i = 0; i < script.length; i++) {
            statement = connection.createStatement();
            statement.executeUpdate(script[i]);
            System.err.println("SQL: " + script[i]);
            statement.close();
         }
         t.commit();
      }
      catch (SQLException e) {
         t.rollback();
         logger.error("OpHibernateConnection.persistObject(): Could not execute DDL script: " + e);
         // *** TODO: Throw OpPersistenceException
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not get JDBC connection from session: " + e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   protected void softExecuteDDLScript(String[] script) {
      // Execute DDL script via JDBC connection ignoring execution-errors
      Connection connection = null;
      Statement statement = null;
      OpTransaction t = null;
      // Try each drop extra and issues only warnings if tables cannot be dropped
      // (Basically, they most probably simply do not exist yet)
      for (int i = 0; i < script.length; i++) {
         try {
            connection = _session.connection();
            statement = connection.createStatement();
            t = newTransaction();
            statement.executeUpdate(script[i]);
            t.commit();
         }
         catch (SQLException e) {
            logger.error("WARNING: Could not execute DDL statement", e);
            if (t != null) {
               t.rollback();
            }
         }
         catch (HibernateException e) {
            logger.error("ERROR: Could not get JDBC connection from Hibernate session: ", e);
            System.exit(1);
         }
         finally {
            if (statement != null) {
               tryToClose(statement);
            }
         }
      }
   }

   /**
    * Tries to close a sql statement.
    * 
    * @param st
    *           a <code>Statement</code> object representing an sql statement.
    */
   private static void tryToClose(Statement st) {
      try {
         st.close();
      }
      catch (SQLException e) {
         logger.error("Cannot close statement", e);
      }
   }

   public void createSchema() {
      // Create schema creation script
      OpHibernateSource source = ((OpHibernateSource) getSource());
      Configuration configuration = source.getConfiguration();
      String[] script = null;
      try {
         script = configuration.generateSchemaCreationScript(source.newHibernateDialect());
         logger.debug("--- script ---\n");
         for (int i = 0; i < script.length; i++) {
            logger.debug(script[i]);
         }
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not generate schema creation script: " + e);
         // *** TODO: Throw OpPersistenceException
      }
      // Execute schema creation script
      executeDDLScript(script);

      //for hypersonic, we need to switch of the default write delay, or things won't work.
      if (source.getDatabaseType() == OpHibernateSource.HSQLDB) {
         try {
            String url = source.getURL();
            String dbPath = url.replaceAll(OpHibernateSource.HSQLDB_JDBC_CONNECTION_PREFIX, "");
            org.hsqldb.Session hsqldbSession = DatabaseManager.newSession(OpHibernateSource.HSQLDB_TYPE, dbPath,
                 source.getLogin(), source.getPassword(), null);
            hsqldbSession.sqlExecuteDirectNoPreChecks("SET WRITE_DELAY FALSE");
            hsqldbSession.commit();
            hsqldbSession.close();
         }
         catch (HsqlException e) {
            logger.error("Cannot configure Hypersonic", e);
         }
      }
   }

   public void updateSchema() {
      OpHibernateSource source = ((OpHibernateSource) getSource());
      Configuration configuration = source.getConfiguration();
      String[] script = null;
      Connection connection = _session.connection();
      List customUpdateScripts = new ArrayList();

      try {
         Dialect dialect = source.newHibernateDialect();
         DatabaseMetadata meta = new DatabaseMetadata(connection, dialect);
         script = configuration.generateSchemaUpdateScript(dialect, meta);
         logger.debug("--- update  script ---\n");
         for (int i = 0; i < script.length; i++) {
            logger.debug(script[i]);
         }
         //Perform custom update
         customUpdateScripts = OpHibernateSchemaUpdater.getInstance(source).generateUpdateSchemaScripts(connection.getMetaData());
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not generate schema update script: ", e);
         // *** TODO: Throw OpPersistenceException
      }
      catch (SQLException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not generate schema update script: ", e);
      }
      // Execute schema creation script
      executeDDLScript(script);
      executeDDLScript((String[]) customUpdateScripts.toArray(new String[]{}));
   }

   public void dropSchema() {
      // Create drop schema script
      OpHibernateSource source = ((OpHibernateSource) getSource());
      Configuration configuration = source.getConfiguration();
      String[] script = null;
      try {
         script = configuration.generateDropSchemaScript(source.newHibernateDialect());
         logger.debug("--- script ---\n");
         for (int i = 0; i < script.length; i++) {
            logger.debug(script[i]);
         }
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not generate drop schema script: " + e);
         // *** TODO: Throw OpPersistenceException
      }
      // Execute drop schema script
      softExecuteDDLScript(script);
   }

   // *** alterSchema could use generateUpdateSchemaScript()

   public void persistObject(OpObject object) {
      try {
         logger.debug("before _session.save()");
         _session.save(object);
         logger.debug("after _session.save()");
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not save object: " + e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   public OpObject getObject(Class c, long id) {
      OpObject object = null;
      try {
         object = (OpObject) (_session.get(c, new Long(id)));
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not load object: " + e);
         // *** TODO: Throw OpPersistenceException
      }
      return object;
   }

   public void updateObject(OpObject object) {
      try {
         _session.update(object);
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not update object: " + e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   public void deleteObject(OpObject object) {
      try {
         _session.delete(object);
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not delete object: " + e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   public void delete(OpPath path) {
      // try {
      // TODO: Hibernate seems to have a bug -- returns list of size > 0 w/null's
      // _session.delete(_createSQLQuery(path));
      Iterator i = find(path);
      OpObject object = null;
      while (i.hasNext()) {
         object = (OpObject) (i.next());
         if (object != null) {
            deleteObject(object);
         }
      }
      /*
       * } catch (HibernateException e) { logger.error("OpHibernateConnection.persistObject(): Could not delete object: " +
       * e); // *** TODO: Throw OpPersistenceException }
       */
   }

   protected String _createSQLQuery(OpPath path) {
      Iterator steps = path.steps();
      // Criteria criteria = null;
      OpPrototype prototype = null;
      String alias = null;
      String from = null;
      String where = null;
      int alias_count = 0;
      OpStep step = null;
      Iterator predicates = null;
      OpExpression predicate = null;
      while (steps.hasNext()) {
         step = (OpStep) (steps.next());
         logger.debug("STEP " + step.getNodeTest());
         // Node test
         if (alias == null) {
            /*
             * OpPrototype p = OpTypeManager.getPrototype(step.getNodeTest()); criteria =
             * _session.createCriteria(p.getInstanceClass());
             */
            alias_count++;
            alias = "alias" + alias_count;
            from = " from " + step.getNodeTest() + " as " + alias;
            prototype = OpTypeManager.getPrototype(step.getNodeTest());
         }
         else {
            // criteria.createCriteria(step.getNodeTest());
            logger.debug("   alias existing");
            alias_count++;
            String new_alias = "alias" + alias_count;
            logger.debug("   before find-member: Prototype " + prototype);
            OpMember member = prototype.getMember(step.getNodeTest());
            logger.debug("   find-member " + member);
            // *** TODO: What if not relationship -- select members/values?
            // ==> And, check if member is null
            if (member instanceof OpRelationship) {
               OpRelationship relationship = (OpRelationship) member;
               if (relationship.getCollectionTypeID() != OpType.SET) {
                  from += " inner join " + alias + "." + step.getNodeTest() + " as " + new_alias;
               }
               else {
                  from += " left outer join " + alias + "." + step.getNodeTest() + " as " + new_alias;
               }
               prototype = OpTypeManager.getPrototypeByID(member.getTypeID());
            }
            alias = new_alias;
         }
         // Predicates
         String expression = null;
         predicates = step.predicates();
         while (predicates.hasNext()) {
            predicate = (OpExpression) (predicates.next());
            if (expression == null) {
               expression = _convertExpression(alias, predicate.getRootExpression());
            }
            else {
               expression += " AND " + _convertExpression(alias, predicate.getRootExpression());
            }
         }
         if (expression != null) {
            if (where == null) {
               where = " WHERE " + expression;
            }
            else {
               where += " AND " + expression;
            }
         }
      }
      String sql = "select " + alias + from;
      if (where != null) {
         sql += where;
      }
      logger.debug("*** QUERY: " + sql);
      return sql;
   }

   public Iterator find(OpPath path) {
      try {
         // Note: Query.iterate() would query each object/row subsequently (n fetches)
         Query query = _session.createQuery(_createSQLQuery(path));
         query.setCacheable(true);
         return query.list().iterator();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.find(): Could not execute query: " + e);
         // *** TODO: Throw OpPersistenceException
      }
      return null;
   }

   public List list(OpQuery query) {
      try {
         // Note: Query.iterate() would query each object/row subsequently (n fetches)
         return ((OpHibernateQuery) query).getQuery().list();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.find(): Could not execute query: " + e);
         e.printStackTrace(System.err);
         // *** TODO: Throw OpPersistenceException
      }
      return null;
   }

   public Iterator iterate(OpQuery query) {
      try {
         // Note: Query.iterate() would query each object/row subsequently (n fetches)
         return ((OpHibernateQuery) query).getQuery().iterate();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.find(): Could not execute query: " + e);
         e.printStackTrace(System.err);
         // *** TODO: Throw OpPersistenceException
      }
      return null;
   }

   public int execute(OpQuery query) {
      try {
         // Note: Query.iterate() would query each object/row subsequently (n fetches)
         return ((OpHibernateQuery) query).getQuery().executeUpdate();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.find(): Could not execute query: " + e);
         e.printStackTrace(System.err);
         // *** TODO: Throw OpPersistenceException
      }
      return 0;
   }

   public int count(OpPath query) {
      // *** Same as find() [helper method required], but use...
      // ==> "SELECT count(alias.ID)"; therefore, only last part is different
      return -1;
   }

   // public abstract OpTransaction newTransaction(); ==> maybe t-objects only for broker?!
   // or just: beginTransaction(), rollback/abortTransaction(), commitTransaction()

   public void close() {
      try {
         _session.close();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.close(): Could not close session: " + e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   public OpTransaction newTransaction() {
      OpTransaction transaction = null;
      try {
         transaction = new OpHibernateTransaction(_session.beginTransaction());
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.newTransaction(): Could not begin transaction: " + e);
         // *** TODO: Throw OpPersistenceException
      }
      return transaction;
   }

   public Blob newBlob(byte[] bytes) {
      return Hibernate.createBlob(bytes);
   }

   protected String _convertExpression(String alias, OpAbstractExpression expression) {
      // Perform an inorder tree walk through the expression tree
      String sql_where = "";
      logger.debug("OpHibernateConnection._convertExpression:" + expression.toString());
      if (expression instanceof OpOperatorExpression) {
         logger.debug("OPERATOR_EXP");
         // The expression represents an operator: Perform recursions
         OpOperatorExpression operator_expression = (OpOperatorExpression) expression;
         // Recursion for left operand
         String sql_left = _convertExpression(alias, operator_expression.getLeftOperand());
         // Recursion for right operand
         String sql_right = _convertExpression(alias, operator_expression.getRightOperand());
         // Convert operator itself
         int operator = operator_expression.getOperator();
         switch (operator) {
            // Conditional operators
            case OpOperatorExpression.AND: {
               sql_where += sql_left;
               sql_where += " AND ";
               sql_where += sql_right;
               break;
            }
            case OpOperatorExpression.OR: {
               sql_where += sql_left;
               sql_where += " OR ";
               sql_where += sql_right;
               break;
            }
            // Equality operators
            case OpOperatorExpression.EQUAL: {
               sql_where += sql_left;
               sql_where += " = ";
               sql_where += sql_right;
               break;
            }
            case OpOperatorExpression.NOT_EQUAL: {
               sql_where += sql_left;
               sql_where += " != ";
               sql_where += sql_right;
               break;
            }
            // Relational operators
            case OpOperatorExpression.LESS_THAN: {
               sql_where += sql_left;
               sql_where += " < ";
               sql_where += sql_right;
               break;
            }
            case OpOperatorExpression.LESS_THAN_OR_EQUAL: {
               sql_where += sql_left;
               sql_where += " <= ";
               sql_where += sql_right;
               break;
            }
            case OpOperatorExpression.GREATER_THAN: {
               sql_where += sql_left;
               sql_where += " > ";
               sql_where += sql_right;
               break;
            }
            case OpOperatorExpression.GREATER_THAN_OR_EQUAL: {
               sql_where += sql_left;
               sql_where += " >= ";
               sql_where += sql_right;
               break;
            }
         }
      }
      else {
         // Everything else other than an operator is a leaf
         if (expression instanceof OpVariableExpression) {
            logger.debug("VAR_EXP");
            // Resolve variable reference
            OpVariableExpression variable_expression = (OpVariableExpression) expression;
            // Only attribute-axis is implemented for variable references
            String variable = variable_expression.getVariable();
            // *** Special "hack" for auto-mapping ID
            // ==> And attention: Only works for single-column-PKs
            sql_where += alias + "." + variable;
         }
         else if (expression instanceof OpValueExpression) {
            logger.debug("VALUE_EXP");
            // Append constant value (QUESTION: Use type-IDs in OpValueExpression?)
            // *** TO DO: Use SQL-Adapter to make SQL-value conversion
            Object value = ((OpValueExpression) expression).getValue();
            if (value instanceof Boolean) {
               // Boolean is converted to BIT
               if (((Boolean) value).booleanValue()) {
                  sql_where += "'1'";
               }
               else {
                  sql_where += "'0'";
               }
            }
            else if (value instanceof Integer) {
               // Append Integer
               sql_where += ((Integer) value).toString();
            }
            else if (value instanceof Long) {
               // Append Integer
               sql_where += ((Long) value).toString();
            }
            else if (value instanceof String) {
               // Append String
               sql_where += "'";
               sql_where += ((String) value);
               sql_where += "'";
            }
         }
      }
      logger.debug("REC: " + sql_where);
      return sql_where;
   }

   public final OpQuery newQuery(String s) {
      return new OpHibernateQuery(_session.createQuery(s));
   }

   public final Connection getJDBCConnection() {
      return _session.connection();
   }

}
