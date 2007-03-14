/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.Hashtable;

public class OpSourceManager {

	private static Hashtable _sources; // Mapping of source names to mounted object sources
	private static OpSource _default_source; // Default object source

	// In the future maybe JNDI-lookup instead of static?

	static {
		_sources = new Hashtable();
		_default_source = null;
	}

	public static void registerSource(OpSource source) {
		// To do: Check for uniqueness of name
		if (source.getName() != null)
			_sources.put(source.getName(), source);
		// Invoke on-register callback
		source.onRegister();
	}

	public static void setDefaultSource(OpSource source) {
		// *** Check if source is registered
		_default_source = source;
	}

	public static OpSource getDefaultSource() {
		return _default_source;
	}

	public static OpSource getSource(String name) {
		return (OpSource) (_sources.get(name));
	}

	// Where to mount data sources (in sessions or here, more globally)?
	// ==> Maybe both: Global for everyone and just have to be opened in a session?!
	// ==> Master/default-source should be defined here

	// ==> We could mimik Unix here: Only "root/system" is allowed to mount/unmount
	// (in a session, but it is "remembered" persistently in the broker)

}
