/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.ArrayList;
import java.util.Iterator;

public class OpStep {

	private String _node_test; // Node test
	private ArrayList _predicates; // Predicates of this step

	public OpStep() {
		_predicates = new ArrayList();
	}

	public final void setNodeTest(String node_test) {
		_node_test = node_test;
	}

	public final String getNodeTest() {
		return _node_test;
	}

	public final void addPredicate(OpExpression expression) {
		_predicates.add(expression);
	}

	public final Iterator predicates() {
		return _predicates.iterator();
	}

}
