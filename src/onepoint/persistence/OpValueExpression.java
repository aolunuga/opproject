/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;


public class OpValueExpression extends OpAbstractExpression {

	Object _value;

	public OpValueExpression(boolean b) {
		_value = new Boolean(b);
	}

	public OpValueExpression(int i) {
		_value = new Integer(i);
	}

	public OpValueExpression(String s) {
		_value = s;
	}
	
	public OpValueExpression(long l) {
		_value = new Long(l);
	}

	public final Object getValue() {
		return _value;
	}

	public String toString() {
		return _value.toString();
	}

}
