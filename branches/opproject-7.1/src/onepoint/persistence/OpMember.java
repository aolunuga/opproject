/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

public abstract class OpMember {

	private String name;
	private int id; // Member-ID that is system-wide unique
	private String typeName;
	private int typeId; // Resolved on-register (OpPrototype)
	private String collectionTypeName;
	private int collectionTypeId; // Resolved on-register (OpPrototype)

	public OpMember() {}

	public final void setName(String name) {
		this.name = name;
	}

	public final String getName() {
		return name;
	}

	final void setID(int id) {
		this.id = id;
	}

	public final int getID() {
		return id;
	}

	public final void setTypeName(String type_name) {
		this.typeName = type_name;
	}

	public final String getTypeName() {
		return typeName;
	}

	public final void setTypeID(int type_id) {
		// Called by OpPrototype
		typeId = type_id;
	}

	public final int getTypeID() {
		return typeId;
	}

	final void setCollectionTypeName(String collection_type_name) {
		collectionTypeName = collection_type_name;
	}

	final String getCollectionTypeName() {
		return collectionTypeName;
	}

	final void setCollectionTypeID(int collection_type_id) {
		// Called by OpPrototype
		collectionTypeId = collection_type_id;
	}

	public final int getCollectionTypeID() {
		return collectionTypeId;
	}

}
