package onepoint.project.team.modules.process;

import onepoint.persistence.OpObject;

public class OpProcess extends OpObject {

	public final static String NAME = "Name";
	public final static String DESCRIPTION = "Description";
	public final static String DEFINITION = "Definition";

	private String name;
	private String description;
	private byte[] definition;

	public final void setName(String name) {
		this.name = name;
	}

	public final String getName() {
		return name;
	}

	public final void setDescription(String description) {
		this.description = description;
	}

	public final String getDescription() {
		return description;
	}

	public final void setDefinition(byte[] definition) {
		this.definition = definition;
	}

	public final byte[] getDefinition() {
		return definition;
	}

}
