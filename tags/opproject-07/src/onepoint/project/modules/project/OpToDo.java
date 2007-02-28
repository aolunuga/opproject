/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

import java.sql.Date;

public class OpToDo extends OpObject {
   
   public final static String TO_DO = "OpToDo";

	public final static String NAME = "Name";
	public final static String PRIORITY = "Priority";
	public final static String COMPLETED = "Completed";
	public final static String DUE = "Due";
	public final static String PROJECT = "Project";
	// *** Maybe add Description

	private String name;
	private byte priority;
	private boolean completed;
	private Date due;
	private OpProjectNode projectNode;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setPriority(byte priority) {
		this.priority = priority;
	}

	public byte getPriority() {
		return priority;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public boolean getCompleted() {
		return completed;
	}
	
	public void setDue(Date due) {
		this.due = due;
	}
	
	public Date getDue() {
		return due;
	}

	public void setProjectNode(OpProjectNode projectNode) {
		this.projectNode = projectNode;
	}

	public OpProjectNode getProjectNode() {
		return projectNode;
	}

}
