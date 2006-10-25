/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.ArrayList;
import java.util.Iterator;

public class OpPath {

	private ArrayList _steps; // All location steps
	private OpStep _active_step; // Active location step

	public OpPath() {
		_steps = new ArrayList();
	}

	public OpPath child(String node_test) {
		// Add new location step in child-axis
		OpStep step = new OpStep();
		step.setNodeTest(node_test);
		_steps.add(step);
		_active_step = step;
		return this;
	}

	// public OpPath attribute(String name) {} -- selects an attribute-list

	public final OpPath predicate(OpExpression predicate) {
		// Add predicate to active location step
		predicate.evaluate();
		_active_step.addPredicate(predicate);
		return this;
	}

	public final Iterator steps() {
		return _steps.iterator();
	}
	
}
