/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

public abstract class OpMember {

	private String _name;
	private int _id; // Member-ID that is system-wide unique
	private String _type_name;
	private int _type_id; // Resolved on-register (OpPrototype)
	private String _collection_type_name;
	private int _collection_type_id; // Resolved on-register (OpPrototype)

	public OpMember() {}

	public final void setName(String name) {
		_name = name;
	}

	public final String getName() {
		return _name;
	}

	final void setID(int id) {
		_id = id;
	}

	public final int getID() {
		return _id;
	}

	final void setTypeName(String type_name) {
		_type_name = type_name;
	}

	final String getTypeName() {
		return _type_name;
	}

	final void setTypeID(int type_id) {
		// Called by OpPrototype
		_type_id = type_id;
	}

	public final int getTypeID() {
		return _type_id;
	}

	final void setCollectionTypeName(String collection_type_name) {
		_collection_type_name = collection_type_name;
	}

	final String getCollectionTypeName() {
		return _collection_type_name;
	}

	final void setCollectionTypeID(int collection_type_id) {
		// Called by OpPrototype
		_collection_type_id = collection_type_id;
	}

	public final int getCollectionTypeID() {
		return _collection_type_id;
	}

}
