/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

public interface OpTransaction {

	public void commit();
	public void rollback();

}

