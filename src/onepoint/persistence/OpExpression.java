/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

import java.util.Stack;

public class OpExpression extends OpAbstractExpression {

   private static XLog logger = XLogFactory.getLogger(OpExpression.class,true);

  // * Not 100% sure if precedence now works (see AND/OR re-ordering in XSQLConnection._convertExpression)
  //    - Possible simplification: Parsing order of BNF automatically resolves precedence?
  // * Equal-operator that takes prototypes is missing (for performing object-identity checks)

  private Stack _operand_stack;
  private Stack _operator_stack;
  private OpAbstractExpression _root_expression; // If set the expression is fully evaluated

  public OpExpression() {
    logger.debug("OpExpression()");
    _operand_stack = new Stack();
    _operator_stack = new Stack();
  }

  public final OpAbstractExpression getRootExpression() {
    return _root_expression;
  }

  // Navigation

  // public OpExpression child(String name) {}

  public OpExpression attribute(String name) {
    // Push variable-expression onto operand-stack
    logger.debug("OpExpression.attribute() : " + name);
    OpVariableExpression variable_expression = new OpVariableExpression(OpAxis.ATTRIBUTE);
    variable_expression.setVariable(name);
    _operand_stack.push(variable_expression);
    return this;
  }

  // Conditional operators

  protected void _addOperator(OpOperatorExpression operator, OpAbstractExpression right_operand) {
    // Compute priority of this operator
    boolean priority = _operator_stack.empty();
    if (!priority) {
      // Compare precedence to the operator on top of the stack
      OpOperatorExpression top_operator = (OpOperatorExpression) (_operator_stack.peek());
      if (operator.getPrecedence() < top_operator.getPrecedence())
        priority = true;
    }
    // Act based on calculcated priority
    if (!priority) {
      // Execute top-operator and push result onto operand-stack
      OpOperatorExpression top_operator = (OpOperatorExpression) (_operator_stack.pop());
      OpAbstractExpression top_right_operand = (OpAbstractExpression) (_operand_stack.pop());
      OpAbstractExpression top_left_operand = (OpAbstractExpression) (_operand_stack.pop());
      top_operator.setLeftOperand(top_left_operand);
      top_operator.setRightOperand(top_right_operand);
      _operand_stack.push(top_operator);
    }
    // Push new operator and operand onto stacks
    _operator_stack.push(operator);
    _operand_stack.push(right_operand);
  }

  // Package-local (called by OpPath)
  void evaluate() {
    // Complete expression evaluation: Evaluate operator stack

    while (!_operator_stack.empty()) {
      // Pop top operator and evaluate using top operands
      OpOperatorExpression operator = (OpOperatorExpression) (_operator_stack.pop());
      OpAbstractExpression right_operand = (OpAbstractExpression) (_operand_stack.pop());
      OpAbstractExpression left_operand = (OpAbstractExpression) (_operand_stack.pop());
      // Possible re-ordering of AND/OR terms according to operator precedence
      logger.debug("   EVAL: RE -- OP : " + operator.getOperator());
      boolean reorder = false;
      if (((operator.getOperator() == OpOperatorExpression.AND) || (operator.getOperator() == OpOperatorExpression.OR)) && (right_operand instanceof OpOperatorExpression)) {
        OpOperatorExpression right_operator = (OpOperatorExpression)right_operand;
        if (((right_operator.getOperator() == OpOperatorExpression.AND) || (right_operator.getOperator() == OpOperatorExpression.OR))
          && (right_operator.getPrecedence() < operator.getPrecedence())) {
          reorder = true;
        }
      }
      if (reorder) {
        logger.debug("   OP RIGHT -- OP : " + ((OpOperatorExpression)right_operand).getOperator());
        operator.setLeftOperand(right_operand);
        operator.setRightOperand(left_operand);
      }
      else {
        operator.setLeftOperand(left_operand);
        operator.setRightOperand(right_operand);
      }
      // Push result onto operand stack
      _operand_stack.push(operator);
    }
    // There should always only remain a single element on the operand stack
    _root_expression = (OpAbstractExpression) (_operand_stack.pop());
  }

  public OpExpression and(OpExpression operand) {
    // Add conditional 'and' expression (operand is evaluated)
    logger.debug("OpExpression.and()");
    operand.evaluate();
    OpOperatorExpression and_operator = new OpOperatorExpression(OpOperatorExpression.AND);
    _addOperator(and_operator, operand._root_expression);
    return this;
  }

  public OpExpression or(OpExpression operand) {
    // Add conditional 'or' expression (operand is evaluated)
    logger.debug("OpExpression.or()");
    operand.evaluate();
    OpOperatorExpression or_operator = new OpOperatorExpression(OpOperatorExpression.OR);
    _addOperator(or_operator, operand._root_expression);
    return this;
  }

  // Comparison operators

  protected OpExpression _equal(OpAbstractExpression expression) {
    // Add '=' comparison expression
    OpOperatorExpression equal_operator = new OpOperatorExpression(OpOperatorExpression.EQUAL);
    _addOperator(equal_operator, expression);
    return this;
  }

  public final OpExpression equal(OpExpression expression) {
    logger.debug("OpExpression.equal()");
    return _equal(expression);
  }

  public final OpExpression equal(boolean value) {
    logger.debug("OpExpression.equal() : value = " + value);
    return _equal(new OpValueExpression(value));
  }

  public final OpExpression equal(int value) {
    logger.debug("OpExpression.equal() : value = " + value);
    return _equal(new OpValueExpression(value));
  }

  public final OpExpression equal(String value) {
    logger.debug("OpExpression.equal() : value = " + value);
    return _equal(new OpValueExpression(value));
  }

  public final OpExpression equal(long value) {
    logger.debug("OpExpression.equal() : value = " + value);
    return _equal(new OpValueExpression(value));
  }

  protected OpExpression _notEqual(OpAbstractExpression expression) {
    // Add '!=' comparison expression
    OpOperatorExpression not_equal_operator = new OpOperatorExpression(OpOperatorExpression.NOT_EQUAL);
    _addOperator(not_equal_operator, expression);
    return this;
  }

  public final OpExpression notEqual(OpExpression expression) {
    logger.debug("OpExpression.notEqual()");
    return _notEqual(expression);
  }

  public final OpExpression notEqual(boolean value) {
    logger.debug("OpExpression.notEqual() : value = " + value);
    return _notEqual(new OpValueExpression(value));
  }

  public final OpExpression notEqual(int value) {
    logger.debug("OpExpression.notEqual() : value = " + value);
    return _notEqual(new OpValueExpression(value));
  }

  public final OpExpression notEqual(String value) {
    logger.debug("OpExpression.notEqual() : value = " + value);
    return _notEqual(new OpValueExpression(value));
  }

  public final OpExpression notEqual(long value) {
    logger.debug("OpExpression.notEqual() : value = " + value);
    return _notEqual(new OpValueExpression(value));
  }

  // Relational operators

  protected OpExpression _lessThan(OpAbstractExpression expression) {
    // Add 'GREATER_THAN' comparison expression
    OpOperatorExpression less_than_operator = new OpOperatorExpression(OpOperatorExpression.LESS_THAN);
    _addOperator(less_than_operator, expression);
    return this;
  }

  public final OpExpression lessThan(OpExpression expression) {
    logger.debug("OpExpression.lessThan()");
    return _lessThan(expression);
  }

  public final OpExpression lessThan(boolean value) {
    logger.debug("OpExpression.lessThan() : value = " + value);
    return _lessThan(new OpValueExpression(value));
  }

  public final OpExpression lessThan(int value) {
    logger.debug("OpExpression.lessThan() : value = " + value);
    return _lessThan(new OpValueExpression(value));
  }

  public final OpExpression lessThan(String value) {
    logger.debug("OpExpression.lessThan() : value = " + value);
    return _lessThan(new OpValueExpression(value));
  }

  protected OpExpression _lessThanOrEqual(OpAbstractExpression expression) {
    // Add 'GREATER_THANOREQUAL' comparison expression
    OpOperatorExpression less_equal_operator = new OpOperatorExpression(OpOperatorExpression.LESS_THAN_OR_EQUAL);
    _addOperator(less_equal_operator, expression);
    return this;
  }

  public final OpExpression lessThanOrEqual(OpExpression expression) {
    logger.debug("OpExpression.lessThanOrEqual()");
    return _lessThanOrEqual(expression);
  }

  public final OpExpression lessThanOrEqual(boolean value) {
    logger.debug("OpExpression.lessThanOrEqual() : value = " + value);
    return _lessThanOrEqual(new OpValueExpression(value));
  }

  public final OpExpression lessThanOrEqual(int value) {
    logger.debug("OpExpression.lessThanOrEqual() : value = " + value);
    return _lessThanOrEqual(new OpValueExpression(value));
  }

  public final OpExpression lessThanOrEqual(String value) {
    logger.debug("OpExpression.lessThanOrEqual() : value = " + value);
    return _lessThanOrEqual(new OpValueExpression(value));
  }

  protected OpExpression _greaterThan(OpAbstractExpression expression) {
    // Add 'GREATER_THAN' comparison expression
    OpOperatorExpression greater_than_operator = new OpOperatorExpression(OpOperatorExpression.GREATER_THAN);
    _addOperator(greater_than_operator, expression);
    return this;
  }

  public final OpExpression greaterThan(OpExpression expression) {
    logger.debug("OpExpression.greaterThan()");
    return _greaterThan(expression);
  }

  public final OpExpression greaterThan(boolean value) {
    logger.debug("OpExpression.greaterThan() : value = " + value);
    return _greaterThan(new OpValueExpression(value));
  }

  public final OpExpression greaterThan(int value) {
    logger.debug("OpExpression.greaterThan() : value = " + value);
    return _greaterThan(new OpValueExpression(value));
  }

  public final OpExpression greaterThan(String value) {
    logger.debug("OpExpression.greaterThan() : value = " + value);
    return _greaterThan(new OpValueExpression(value));
  }

  protected OpExpression _greaterThanOrEqual(OpAbstractExpression expression) {
    // Add 'GREATER_THAN' comparison expression
    OpOperatorExpression greater_equal_operator = new OpOperatorExpression(OpOperatorExpression.GREATER_THAN_OR_EQUAL);
    _addOperator(greater_equal_operator, expression);
    return this;
  }

  public final OpExpression greaterThanOrEqual(OpExpression expression) {
    logger.debug("OpExpression.greaterThanOrEqual()");
    return _greaterThanOrEqual(expression);
  }

  public final OpExpression greaterThanOrEqual(boolean value) {
    logger.debug("OpExpression.greaterThanOrEqual() : value = " + value);
    return _greaterThanOrEqual(new OpValueExpression(value));
  }

  public final OpExpression greaterThanOrEqual(int value) {
    logger.debug("OpExpression.greaterThanOrEqual() : value = " + value);
    return _greaterThanOrEqual(new OpValueExpression(value));
  }

  public final OpExpression greaterThanOrEqual(String value) {
    logger.debug("OpExpression.greaterThanOrEqual() : value = " + value);
    return _greaterThanOrEqual(new OpValueExpression(value));
  }

}
