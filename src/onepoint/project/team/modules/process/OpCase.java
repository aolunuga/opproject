package onepoint.project.team.modules.process;

import onepoint.persistence.OpObject;

public class OpCase extends OpObject {

	public final static String NAME = "Name";
	public final static String ACTIVE_TASK_NAME = "ActiveTaskName";
	public final static String PROCESS = "Process";

	private String name;
	private String activeTaskName;
	private OpProcess process;

	public final void setName(String name) {
		this.name = name;
	}

	public final String getName() {
		return name;
	}

	public final void setActiveTaskName(String active_task_name) {
		activeTaskName = active_task_name;
	}

	public final String getActiveTaskName() {
		return activeTaskName;
	}

	public final void setProcess(OpProcess process) {
		this.process = process;
	}

	public final OpProcess getProcess() {
		return process;
	}

}
