/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;


public class OpOperatorExpression extends OpAbstractExpression {

    // Conditional operators
    public final static int AND = 1;
    public final static int OR = 2;
    // Equality operators
    public final static int EQUAL = 3;
    public final static int NOT_EQUAL = 4;
    // Relational operators
    public final static int LESS_THAN = 5;
    public final static int LESS_THAN_OR_EQUAL = 6;
    public final static int GREATER_THAN = 7;
    public final static int GREATER_THAN_OR_EQUAL = 8;
    // To add: Numerical operators (maybe on top -- precedence)

    private int _operator;
    private OpAbstractExpression _left_operand;
    private OpAbstractExpression _right_operand;

    public OpOperatorExpression(int operator) {
	_operator = operator;
	_left_operand = null;
	_right_operand = null;
    }

    public final int getOperator() {
	return _operator;
    }

    public final void setLeftOperand(OpAbstractExpression expression) {
	_left_operand = expression;
    }

    public final OpAbstractExpression getLeftOperand() {
	return _left_operand;
    }

    public final void setRightOperand(OpAbstractExpression expression) {
	_right_operand = expression;
    }

    public final OpAbstractExpression getRightOperand() {
	return _right_operand;
    }

    public int getPrecedence() {
	// Highest precedence first
	switch(_operator) {
	case LESS_THAN : {
	    return 1;
	}
	case LESS_THAN_OR_EQUAL : {
	    return 1;
	}
	case GREATER_THAN : {
	    return 1;
	}
	case GREATER_THAN_OR_EQUAL : {
	    return 1;
	}
	case EQUAL : {
	    return 2;
	}
	case NOT_EQUAL : {
	    return 2;
	}
	case AND : {
	    return 3;
	}
	case OR : {
	    return 4;
	}
	}
	return 0;
    }

    public String toString() {
	switch(_operator) {
	case AND : {
	    return "AND";
	}
	case OR : {
	    return "OR";
	}
	case EQUAL : {
	    return "=";
	}
	case NOT_EQUAL : {
	    return "!=";
	}
	case LESS_THAN : {
	    return "<";
	}
	case LESS_THAN_OR_EQUAL : {
	    return "<=";
	}
	case GREATER_THAN : {
	    return ">";
	}
	case GREATER_THAN_OR_EQUAL : {
	    return ">=";
	}
	}
	return "";
    }

}

