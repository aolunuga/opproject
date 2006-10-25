/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

public class OpVariableExpression extends OpAbstractExpression {

	int _axis; // Axis as defined in class OpAxis
	String _variable; // Variable name

	public OpVariableExpression(int axis) {
		_axis = axis;
	}

	public final int getAxis() {
		return _axis;
	}

	public final void setVariable(String variable) {
		_variable = variable;
	}

	public final String getVariable() {
		return _variable;
	}

	public String toString() {
		return new String("child::" + _variable);
	}

}
